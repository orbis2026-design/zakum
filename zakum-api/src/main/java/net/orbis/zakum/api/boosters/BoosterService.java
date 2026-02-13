package net.orbis.zakum.api.boosters;

import net.orbis.zakum.api.entitlements.EntitlementScope;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Timed booster service.
 *
 * Implementation MUST be:
 * - main-thread safe (multiplier lookups are in-memory)
 * - async for DB writes
 */
public interface BoosterService {

  /**
   * Returns the effective multiplier for this player.
   * Includes:
   * - server-scope "ALL"
   * - server-scope per-player
   * - network-scope "ALL"
   * - network-scope per-player
   *
   * If none: 1.0
   */
  double multiplier(UUID playerId, EntitlementScope scope, String serverId, BoosterKind kind);

  CompletableFuture<Void> grantToAll(EntitlementScope scope, String serverId, BoosterKind kind, double multiplier, long durationSeconds);

  CompletableFuture<Void> grantToPlayer(UUID playerId, EntitlementScope scope, String serverId, BoosterKind kind, double multiplier, long durationSeconds);
}
