package net.orbis.zakum.api;

import net.orbis.zakum.api.actions.ActionBus;
import net.orbis.zakum.api.boosters.BoosterService;
import net.orbis.zakum.api.db.ZakumDatabase;
import net.orbis.zakum.api.entitlements.EntitlementService;
import net.orbis.zakum.api.net.ControlPlaneClient;
import org.bukkit.plugin.Plugin;

import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Stable API consumed by other plugins.
 *
 * Consumers resolve via Bukkit ServicesManager:
 *   ZakumApi api = Bukkit.getServicesManager().load(ZakumApi.class);
 */
public interface ZakumApi {

  Plugin plugin();

  ServerIdentity server();

  Clock clock();

  Executor async();

  ZakumDatabase database();

  ActionBus actions();

  EntitlementService entitlements();

  BoosterService boosters();

  /**
   * Optional: Orbis Cloud Bot / control-plane integration.
   * Keep this interface stable; implementation can evolve.
   */
  Optional<ControlPlaneClient> controlPlane();
}
