package net.orbis.zakum.core;

import net.orbis.zakum.api.ZakumApi;
import net.orbis.zakum.api.ServerIdentity;
import net.orbis.zakum.core.actions.PaperActionEmitter;
import net.orbis.zakum.core.actions.SimpleActionBus;
import net.orbis.zakum.core.boosters.SqlBoosterService;
import net.orbis.zakum.core.db.SqlManager;
import net.orbis.zakum.core.entitlements.SqlEntitlementService;
import net.orbis.zakum.core.net.HttpControlPlaneClient;
import net.orbis.zakum.core.util.Async;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ExecutorService;

public final class ZakumPlugin extends JavaPlugin {

  private ZakumApiImpl api;
  private SqlManager sql;
  private ExecutorService asyncPool;

  private SimpleActionBus actionBus;
  private SqlEntitlementService entitlements;
  private SqlBoosterService boosters;

  @Override
  public void onEnable() {
    saveDefaultConfig();

    var serverId = getConfig().getString("server.id", "").trim();
    if (serverId.isBlank()) {
      getLogger().severe("config.yml server.id is required. Disabling Zakum.");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }

    var clock = Clock.systemUTC();
    this.asyncPool = Async.newSharedPool(getLogger());
    var async = this.asyncPool;

    this.sql = new SqlManager(this, async, clock);
    this.sql.start();

    var controlPlane = HttpControlPlaneClient.fromConfig(this, async);

    this.actionBus = new SimpleActionBus();

    int entCacheMax = 50_000;
    int entTtlSeconds = 30;
    this.entitlements = new SqlEntitlementService(sql, async, entCacheMax, Duration.ofSeconds(entTtlSeconds));

    this.boosters = new SqlBoosterService(this, sql, async);
    this.boosters.start();

    this.api = new ZakumApiImpl(
      this,
      new ServerIdentity(serverId),
      clock,
      async,
      sql,
      controlPlane,
      actionBus,
      entitlements,
      boosters
    );

    Bukkit.getServicesManager().register(ZakumApi.class, api, this, ServicePriority.Highest);
    Bukkit.getServicesManager().register(net.orbis.zakum.api.actions.ActionBus.class, actionBus, this, ServicePriority.Highest);
    Bukkit.getServicesManager().register(net.orbis.zakum.api.entitlements.EntitlementService.class, entitlements, this, ServicePriority.Highest);
    Bukkit.getServicesManager().register(net.orbis.zakum.api.boosters.BoosterService.class, boosters, this, ServicePriority.Highest);

    getServer().getPluginManager().registerEvents(new PaperActionEmitter(actionBus), this);

    getLogger().info("Zakum enabled. server.id=" + serverId + " db=" + sql.state());
  }

  @Override
  public void onDisable() {
    var sm = Bukkit.getServicesManager();

    if (api != null) sm.unregister(ZakumApi.class, api);
    if (actionBus != null) sm.unregister(net.orbis.zakum.api.actions.ActionBus.class, actionBus);
    if (entitlements != null) sm.unregister(net.orbis.zakum.api.entitlements.EntitlementService.class, entitlements);
    if (boosters != null) sm.unregister(net.orbis.zakum.api.boosters.BoosterService.class, boosters);

    if (boosters != null) boosters.shutdown();

    if (sql != null) sql.shutdown();
    if (asyncPool != null) asyncPool.shutdownNow();

    api = null;
    sql = null;
    asyncPool = null;
    actionBus = null;
    entitlements = null;
    boosters = null;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!command.getName().equalsIgnoreCase("zakum")) return false;

    if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
      sender.sendMessage("§bZakum§7: server=" + api.server().serverId()
        + " db=" + sql.state()
        + " poolActive=" + sql.poolStats().active()
        + " poolIdle=" + sql.poolStats().idle()
      );
      return true;
    }

    if (args[0].equalsIgnoreCase("reconnect")) {
      sql.requestReconnectNow();
      sender.sendMessage("§eZakum§7: reconnect requested.");
      return true;
    }

    sender.sendMessage("§cUsage: /zakum status | /zakum reconnect");
    return true;
  }
}
