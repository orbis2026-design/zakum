package net.orbis.zakum.battlepass;

import net.orbis.zakum.api.ZakumApi;
import net.orbis.zakum.api.db.DatabaseState;
import net.orbis.zakum.battlepass.db.BattlePassSchema;
import net.orbis.zakum.battlepass.listener.BpPlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class BattlePassPlugin extends JavaPlugin {

  private ZakumApi zakum;
  private BattlePassRuntime runtime;

  private int flushTaskId = -1;
  private int premiumTaskId = -1;

  @Override
  public void onEnable() {
    saveDefaultConfig();

    this.zakum = Bukkit.getServicesManager().load(ZakumApi.class);
    if (zakum == null) {
      getLogger().severe("ZakumApi not found. Did Zakum load? Disabling ZakumBattlePass.");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }

    if (zakum.database().state() == DatabaseState.ONLINE) {
      BattlePassSchema.ensureTables(zakum.database().jdbc());
    } else {
      getLogger().warning("Zakum DB is offline. BattlePass will be limited until DB is back.");
    }

    this.runtime = new BattlePassRuntime(this, zakum);
    this.runtime.start();

    getServer().getPluginManager().registerEvents(new BpPlayerListener(runtime), this);

    int flushSeconds = Math.max(2, getConfig().getInt("battlepass.flush.intervalSeconds", 5));
    long flushTicks = flushSeconds * 20L;

    this.flushTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
      if (runtime != null) runtime.flushAllAsync();
    }, flushTicks, flushTicks);

    int premiumSeconds = Math.max(30, getConfig().getInt("battlepass.premiumRefresh.intervalSeconds", 300));
    long premiumTicks = premiumSeconds * 20L;

    this.premiumTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
      if (runtime != null) runtime.refreshPremiumAllAsync();
    }, premiumTicks, premiumTicks);

    getLogger().info("ZakumBattlePass enabled.");
  }

  @Override
  public void onDisable() {
    if (flushTaskId != -1) {
      getServer().getScheduler().cancelTask(flushTaskId);
      flushTaskId = -1;
    }
    if (premiumTaskId != -1) {
      getServer().getScheduler().cancelTask(premiumTaskId);
      premiumTaskId = -1;
    }
    if (runtime != null) {
      runtime.stop();
      runtime = null;
    }
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!command.getName().equalsIgnoreCase("battlepass")) return false;

    sender.sendMessage("§dBattlePass§7: server=" + zakum.server().serverId()
      + " db=" + zakum.database().state()
    );
    return true;
  }
}
