package net.orbis.zakum.core.entitlements;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.orbis.zakum.api.db.DatabaseState;
import net.orbis.zakum.api.db.ZakumDatabase;
import net.orbis.zakum.api.entitlements.EntitlementScope;
import net.orbis.zakum.api.entitlements.EntitlementService;
import net.orbis.zakum.core.util.UuidBytes;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Cached entitlement checks with async DB access.
 */
public final class SqlEntitlementService implements EntitlementService {

  private final ZakumDatabase db;
  private final Executor async;
  private final Cache<String, Boolean> cache;

  public SqlEntitlementService(ZakumDatabase db, Executor async, int maxSize, Duration ttl) {
    this.db = Objects.requireNonNull(db, "db");
    this.async = Objects.requireNonNull(async, "async");

    this.cache = Caffeine.newBuilder()
      .maximumSize(maxSize)
      .expireAfterWrite(ttl)
      .build();
  }

  @Override
  public CompletableFuture<Boolean> has(UUID playerId, EntitlementScope scope, String serverId, String entitlementKey) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(scope, "scope");
    Objects.requireNonNull(entitlementKey, "entitlementKey");

    String sId = (scope == EntitlementScope.NETWORK) ? null : Objects.requireNonNull(serverId, "serverId");
    String ck = cacheKey(playerId, scope, sId, entitlementKey);

    Boolean hit = cache.getIfPresent(ck);
    if (hit != null) return CompletableFuture.completedFuture(hit);

    return CompletableFuture.supplyAsync(() -> {
      if (db.state() != DatabaseState.ONLINE) return false;

      long now = Instant.now().getEpochSecond();

      boolean ok = !db.jdbc().query(
        "SELECT 1 FROM zakum_entitlements WHERE uuid=? AND scope=? AND (server_id <=> ?) AND entitlement_key=? AND (expires_at IS NULL OR expires_at > ?) LIMIT 1",
        rs -> 1,
        UuidBytes.toBytes(playerId),
        scope.name(),
        sId,
        entitlementKey,
        now
      ).isEmpty();

      cache.put(ck, ok);
      return ok;
    }, async);
  }

  @Override
  public CompletableFuture<Void> grant(UUID playerId, EntitlementScope scope, String serverId, String entitlementKey, Long expiresAtEpochSeconds) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(scope, "scope");
    Objects.requireNonNull(entitlementKey, "entitlementKey");

    String sId = (scope == EntitlementScope.NETWORK) ? null : Objects.requireNonNull(serverId, "serverId");

    return CompletableFuture.runAsync(() -> {
      if (db.state() != DatabaseState.ONLINE) return;

      db.jdbc().update(
        "INSERT INTO zakum_entitlements (uuid, scope, server_id, entitlement_key, expires_at) VALUES (?,?,?,?,?) " +
          "ON DUPLICATE KEY UPDATE expires_at=VALUES(expires_at)",
        UuidBytes.toBytes(playerId),
        scope.name(),
        sId,
        entitlementKey,
        expiresAtEpochSeconds
      );

      invalidate(playerId);
    }, async);
  }

  @Override
  public CompletableFuture<Void> revoke(UUID playerId, EntitlementScope scope, String serverId, String entitlementKey) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(scope, "scope");
    Objects.requireNonNull(entitlementKey, "entitlementKey");

    String sId = (scope == EntitlementScope.NETWORK) ? null : Objects.requireNonNull(serverId, "serverId");

    return CompletableFuture.runAsync(() -> {
      if (db.state() != DatabaseState.ONLINE) return;

      db.jdbc().update(
        "DELETE FROM zakum_entitlements WHERE uuid=? AND scope=? AND (server_id <=> ?) AND entitlement_key=?",
        UuidBytes.toBytes(playerId),
        scope.name(),
        sId,
        entitlementKey
      );

      invalidate(playerId);
    }, async);
  }

  @Override
  public void invalidate(UUID playerId) {
    String prefix = playerId.toString() + "|";
    cache.asMap().keySet().removeIf(k -> k.startsWith(prefix));
  }

  private static String cacheKey(UUID playerId, EntitlementScope scope, String serverId, String entitlementKey) {
    return playerId + "|" + scope + "|" + (serverId == null ? "" : serverId) + "|" + entitlementKey;
  }
}
