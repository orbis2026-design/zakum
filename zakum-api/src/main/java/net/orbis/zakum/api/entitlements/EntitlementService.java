package net.orbis.zakum.api.entitlements;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Entitlements are gameplay-authoritative.
 */
public interface EntitlementService {

  CompletableFuture<Boolean> has(UUID playerId, EntitlementScope scope, String serverId, String entitlementKey);

  CompletableFuture<Void> grant(UUID playerId, EntitlementScope scope, String serverId, String entitlementKey, Long expiresAtEpochSeconds);

  CompletableFuture<Void> revoke(UUID playerId, EntitlementScope scope, String serverId, String entitlementKey);

  void invalidate(UUID playerId);
}
