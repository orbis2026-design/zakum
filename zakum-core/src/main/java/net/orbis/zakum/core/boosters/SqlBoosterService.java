package net.orbis.zakum.core.boosters;

import net.orbis.zakum.api.boosters.BoosterKind;
import net.orbis.zakum.api.boosters.BoosterService;
import net.orbis.zakum.api.db.DatabaseState;
import net.orbis.zakum.api.db.ZakumDatabase;
import net.orbis.zakum.api.entitlements.EntitlementScope;
import net.orbis.zakum.core.util.UuidBytes;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Booster manager with:
 * - async DB writes
 * - in-memory multiplier cache for fast main-thread reads
 * - periodic purge + refresh
 */
public final class SqlBoosterService implements BoosterService {

  private static final double MAX_MULT = 100.0;

  private final Plugin plugin;
  private final ZakumDatabase db;
  private final Executor async;

  private final Map<Key, Double> allMult = new HashMap<>();
  private final Map<PlayerKey, Double> playerMult = new HashMap<>();

  private final AtomicInteger taskId = new AtomicInteger(-1);

  public SqlBoosterService(Plugin plugin, ZakumDatabase db, Executor async) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.db = Objects.requireNonNull(db, "db");
    this.async = Objects.requireNonNull(async, "async");
  }

  public void start() {
    // Load active boosters once (async) and then keep a lightweight periodic refresh.
    async.execute(this::refreshFromDb);

    int id = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::refreshFromDb, 20L * 60, 20L * 60).getTaskId();
    taskId.set(id);
  }

  public void shutdown() {
    int id = taskId.getAndSet(-1);
    if (id != -1) Bukkit.getScheduler().cancelTask(id);

    synchronized (this) {
      allMult.clear();
      playerMult.clear();
    }
  }

  @Override
  public double multiplier(UUID playerId, EntitlementScope scope, String serverId, BoosterKind kind) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(scope, "scope");
    Objects.requireNonNull(kind, "kind");

    String sId = (scope == EntitlementScope.NETWORK) ? null : Objects.requireNonNull(serverId, "serverId");

    synchronized (this) {
      double mult = 1.0;

      // ALL
      mult *= allMult.getOrDefault(new Key(scope, sId, kind.name()), 1.0);

      // PLAYER
      mult *= playerMult.getOrDefault(new PlayerKey(playerId, scope, sId, kind.name()), 1.0);

      if (mult < 0.0) mult = 1.0;
      if (mult > MAX_MULT) mult = MAX_MULT;
      return mult;
    }
  }

  @Override
  public CompletableFuture<Void> grantToAll(EntitlementScope scope, String serverId, BoosterKind kind, double multiplier, long durationSeconds) {
    Objects.requireNonNull(scope, "scope");
    Objects.requireNonNull(kind, "kind");

    String sId = (scope == EntitlementScope.NETWORK) ? null : Objects.requireNonNull(serverId, "serverId");

    return CompletableFuture.runAsync(() -> {
      ensureOnline();

      long expiresAt = Instant.now().getEpochSecond() + Math.max(1, durationSeconds);
      double mult = sanitize(multiplier);

      db.jdbc().update(
        "INSERT INTO zakum_boosters (scope, server_id, target, uuid, kind, multiplier, expires_at) VALUES (?,?,?,?,?,?,?)",
        scope.name(), sId, "ALL", null, kind.name(), mult, expiresAt
      );

      refreshFromDb();
    }, async);
  }

  @Override
  public CompletableFuture<Void> grantToPlayer(UUID playerId, EntitlementScope scope, String serverId, BoosterKind kind, double multiplier, long durationSeconds) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(scope, "scope");
    Objects.requireNonNull(kind, "kind");

    String sId = (scope == EntitlementScope.NETWORK) ? null : Objects.requireNonNull(serverId, "serverId");

    return CompletableFuture.runAsync(() -> {
      ensureOnline();

      long expiresAt = Instant.now().getEpochSecond() + Math.max(1, durationSeconds);
      double mult = sanitize(multiplier);

      db.jdbc().update(
        "INSERT INTO zakum_boosters (scope, server_id, target, uuid, kind, multiplier, expires_at) VALUES (?,?,?,?,?,?,?)",
        scope.name(), sId, "PLAYER", UuidBytes.toBytes(playerId), kind.name(), mult, expiresAt
      );

      refreshFromDb();
    }, async);
  }

  private void ensureOnline() {
    if (db.state() != DatabaseState.ONLINE) {
      throw new IllegalStateException("DB is offline");
    }
  }

  private static double sanitize(double multiplier) {
    if (Double.isNaN(multiplier) || Double.isInfinite(multiplier)) return 1.0;
    if (multiplier < 0.01) return 0.01;
    if (multiplier > MAX_MULT) return MAX_MULT;
    return multiplier;
  }

  private void refreshFromDb() {
    if (db.state() != DatabaseState.ONLINE) return;

    long now = Instant.now().getEpochSecond();

    var rows = db.jdbc().query(
      "SELECT scope, server_id, target, uuid, kind, multiplier FROM zakum_boosters WHERE expires_at > ?",
      rs -> new Row(
        rs.getString(1),
        rs.getString(2),
        rs.getString(3),
        rs.getBytes(4),
        rs.getString(5),
        rs.getDouble(6)
      ),
      now
    );

    Map<Key, Double> newAll = new HashMap<>();
    Map<PlayerKey, Double> newPlayer = new HashMap<>();

    for (Row r : rows) {
      EntitlementScope scope = EntitlementScope.valueOf(r.scope);
      String sId = r.serverId; // null ok
      String kind = r.kind;

      if ("ALL".equalsIgnoreCase(r.target)) {
        var k = new Key(scope, sId, kind);
        newAll.put(k, product(newAll.get(k), r.multiplier));
      } else {
        if (r.uuid == null || r.uuid.length != 16) continue;
        UUID uuid = UuidBytes.fromBytes(r.uuid);

        var pk = new PlayerKey(uuid, scope, sId, kind);
        newPlayer.put(pk, product(newPlayer.get(pk), r.multiplier));
      }
    }

    synchronized (this) {
      allMult.clear();
      allMult.putAll(newAll);

      playerMult.clear();
      playerMult.putAll(newPlayer);
    }
  }

  private static double product(Double a, double b) {
    double x = (a == null) ? 1.0 : a;
    x *= sanitize(b);
    if (x > MAX_MULT) x = MAX_MULT;
    return x;
  }

  private record Row(String scope, String serverId, String target, byte[] uuid, String kind, double multiplier) {}

  private record Key(EntitlementScope scope, String serverId, String kind) {}

  private record PlayerKey(UUID uuid, EntitlementScope scope, String serverId, String kind) {}
}
