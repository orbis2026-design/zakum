package net.orbis.zakum.battlepass.premium;

import net.orbis.zakum.api.ZakumApi;
import net.orbis.zakum.api.entitlements.EntitlementScope;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PremiumResolver {

  private final ZakumApi zakum;
  private final EntitlementScope scope;
  private final String entitlementKey;
  private final String serverId;

  public PremiumResolver(ZakumApi zakum, String premiumScope, String entitlementKey, String serverId) {
    this.zakum = zakum;

    this.scope = parseScope(premiumScope);
    this.entitlementKey = entitlementKey == null ? "battlepass_premium" : entitlementKey.trim();
    this.serverId = serverId;
  }

  public CompletableFuture<Boolean> isPremium(UUID uuid) {
    if (scope == EntitlementScope.NETWORK) {
      return zakum.entitlements().has(uuid, EntitlementScope.NETWORK, null, entitlementKey);
    }
    return zakum.entitlements().has(uuid, EntitlementScope.SERVER, serverId, entitlementKey);
  }

  private static EntitlementScope parseScope(String s) {
    if (s == null) return EntitlementScope.SERVER;
    String x = s.trim().toUpperCase(Locale.ROOT);
    if (x.equals("GLOBAL") || x.equals("NETWORK")) return EntitlementScope.NETWORK;
    return EntitlementScope.SERVER;
  }

  public EntitlementScope scope() { return scope; }

  public String serverId() { return serverId; }

  public String entitlementKey() { return entitlementKey; }
}
