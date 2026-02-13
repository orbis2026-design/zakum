package net.orbis.zakum.core.db;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.orbis.zakum.api.db.DatabaseState;
import net.orbis.zakum.api.db.Jdbc;
import net.orbis.zakum.api.db.ZakumDatabase;
import org.bukkit.plugin.Plugin;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * MySQL manager with:
 * - HikariCP pool
 * - Flyway migrations
 * - OFFLINE retry loop (async; no main-thread blocking)
 */
public final class SqlManager implements ZakumDatabase {

  private final Plugin plugin;
  private final Logger log;
  private final Executor async;
  @SuppressWarnings("unused")
  private final Clock clock;

  private final AtomicReference<DatabaseState> state = new AtomicReference<>(DatabaseState.OFFLINE);

  private volatile HikariDataSource ds;
  private volatile Flyway flyway;
  private volatile boolean shuttingDown = false;

  // Example cache (handy for simple KV reads); safe to remove.
  @SuppressWarnings("unused")
  private final Cache<String, String> kvCache = Caffeine.newBuilder()
    .maximumSize(10_000)
    .expireAfterWrite(Duration.ofMinutes(5))
    .build();

  private final Jdbc jdbc = new JdbcImpl(this);

  private final Object reconnectLock = new Object();
  private volatile long nextRetryNanos = 0;

  public SqlManager(Plugin plugin, Executor async, Clock clock) {
    this.plugin = plugin;
    this.log = plugin.getLogger();
    this.async = async;
    this.clock = clock;
  }

  public void start() {
    if (!plugin.getConfig().getBoolean("database.enabled", true)) {
      log.warning("database.enabled=false; Zakum DB services disabled.");
      state.set(DatabaseState.OFFLINE);
      return;
    }
    requestReconnectNow();
  }

  public void shutdown() {
    shuttingDown = true;

    var current = ds;
    ds = null;

    if (current != null) {
      try { current.close(); } catch (Throwable t) {
        log.warning("Error closing HikariDataSource: " + t.getMessage());
      }
    }
  }

  public void requestReconnectNow() {
    scheduleReconnect(0);
  }

  private void scheduleReconnect(long delaySeconds) {
    long when = System.nanoTime() + TimeUnit.SECONDS.toNanos(delaySeconds);
    nextRetryNanos = when;

    async.execute(() -> {
      synchronized (reconnectLock) {
        if (shuttingDown) return;
        if (System.nanoTime() < nextRetryNanos) return;
        connectOrRetry();
      }
    });
  }

  private void connectOrRetry() {
    try {
      connect();
      state.set(DatabaseState.ONLINE);
      log.info("Database ONLINE.");
    } catch (Throwable t) {
      state.set(DatabaseState.OFFLINE);

      long retrySeconds = plugin.getConfig().getLong("database.failover.retrySeconds", 30);
      log.warning("Database OFFLINE. Retry in " + retrySeconds + "s. Cause: " +
        t.getClass().getSimpleName() + ": " + t.getMessage());

      scheduleReconnect(retrySeconds);
    }
  }

  private void connect() {
    var cfg = plugin.getConfig();

    String host = cfg.getString("database.host");
    int port = cfg.getInt("database.port");
    String database = cfg.getString("database.database");
    String user = cfg.getString("database.user");
    String password = cfg.getString("database.password");
    String params = cfg.getString("database.params", "");

    Objects.requireNonNull(host, "database.host");
    Objects.requireNonNull(database, "database.database");
    Objects.requireNonNull(user, "database.user");
    Objects.requireNonNull(password, "database.password");

    var jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?" + params;

    var hc = new HikariConfig();
    hc.setJdbcUrl(jdbcUrl);
    hc.setUsername(user);
    hc.setPassword(password);

    hc.setMaximumPoolSize(cfg.getInt("database.pool.maxPoolSize", 10));
    hc.setMinimumIdle(cfg.getInt("database.pool.minIdle", 2));
    hc.setConnectionTimeout(cfg.getLong("database.pool.connectionTimeoutMs", 5000));
    hc.setValidationTimeout(cfg.getLong("database.pool.validationTimeoutMs", 2500));
    hc.setIdleTimeout(cfg.getLong("database.pool.idleTimeoutMs", 600000));
    hc.setMaxLifetime(cfg.getLong("database.pool.maxLifetimeMs", 1800000));

    long leakMs = cfg.getLong("database.pool.leakDetectionMs", 0);
    if (leakMs > 0) hc.setLeakDetectionThreshold(leakMs);

    // Production posture (MySQL driver hints).
    hc.addDataSourceProperty("cachePrepStmts", "true");
    hc.addDataSourceProperty("prepStmtCacheSize", "250");
    hc.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    hc.addDataSourceProperty("useServerPrepStmts", "true");

    var newDs = new HikariDataSource(hc);

    // Quick verification.
    try (var c = newDs.getConnection()) {
      c.isValid(2);
    }

    var newFlyway = Flyway.configure()
      .dataSource(newDs)
      .locations("classpath:db/migration")
      .baselineOnMigrate(true)
      .load();

    newFlyway.migrate();

    var old = ds;
    ds = newDs;
    flyway = newFlyway;

    if (old != null) old.close();
  }

  @Override
  public DatabaseState state() {
    return state.get();
  }

  @Override
  public DataSource dataSource() {
    var current = ds;
    if (current == null) throw new IllegalStateException("DB is offline");
    return current;
  }

  @Override
  public Jdbc jdbc() {
    return jdbc;
  }

  public PoolStats poolStats() {
    var current = ds;
    if (current == null) return new PoolStats(0, 0);

    var mx = current.getHikariPoolMXBean();
    if (mx == null) return new PoolStats(0, 0);

    return new PoolStats(mx.getActiveConnections(), mx.getIdleConnections());
  }

  public record PoolStats(int active, int idle) {}
}
