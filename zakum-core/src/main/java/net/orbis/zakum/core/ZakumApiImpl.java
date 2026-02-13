package net.orbis.zakum.core;

import net.orbis.zakum.api.ServerIdentity;
import net.orbis.zakum.api.ZakumApi;
import net.orbis.zakum.api.actions.ActionBus;
import net.orbis.zakum.api.boosters.BoosterService;
import net.orbis.zakum.api.db.ZakumDatabase;
import net.orbis.zakum.api.entitlements.EntitlementService;
import net.orbis.zakum.api.net.ControlPlaneClient;
import org.bukkit.plugin.Plugin;

import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.Executor;

final class ZakumApiImpl implements ZakumApi {

  private final Plugin plugin;
  private final ServerIdentity server;
  private final Clock clock;
  private final Executor async;
  private final ZakumDatabase database;
  private final Optional<ControlPlaneClient> controlPlane;
  private final ActionBus actions;
  private final EntitlementService entitlements;
  private final BoosterService boosters;

  ZakumApiImpl(
    Plugin plugin,
    ServerIdentity server,
    Clock clock,
    Executor async,
    ZakumDatabase database,
    Optional<ControlPlaneClient> controlPlane,
    ActionBus actions,
    EntitlementService entitlements,
    BoosterService boosters
  ) {
    this.plugin = plugin;
    this.server = server;
    this.clock = clock;
    this.async = async;
    this.database = database;
    this.controlPlane = controlPlane;
    this.actions = actions;
    this.entitlements = entitlements;
    this.boosters = boosters;
  }

  @Override public Plugin plugin() { return plugin; }

  @Override public ServerIdentity server() { return server; }

  @Override public Clock clock() { return clock; }

  @Override public Executor async() { return async; }

  @Override public ZakumDatabase database() { return database; }

  @Override public ActionBus actions() { return actions; }

  @Override public EntitlementService entitlements() { return entitlements; }

  @Override public BoosterService boosters() { return boosters; }

  @Override public Optional<ControlPlaneClient> controlPlane() { return controlPlane; }
}
