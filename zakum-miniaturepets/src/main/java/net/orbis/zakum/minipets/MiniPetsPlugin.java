package net.orbis.zakum.minipets;

import net.orbis.zakum.api.ZakumApi;
import net.orbis.zakum.api.db.DatabaseState;
import net.orbis.zakum.minipets.command.MiniPetsCommand;
import net.orbis.zakum.minipets.db.MiniPetsSchema;
import net.orbis.zakum.minipets.listener.MiniPetsPlayerListener;
import net.orbis.zakum.minipets.runtime.MiniPetsRuntime;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class MiniPetsPlugin extends JavaPlugin {

  private ZakumApi zakum;
  private MiniPetsRuntime rt;

  @Override
  public void onEnable() {
    saveDefaultConfig();

    this.zakum = Bukkit.getServicesManager().load(ZakumApi.class);
    if (zakum == null) {
      getLogger().severe("ZakumApi not found. Disabling ZakumMiniaturePets.");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }

    if (zakum.database().state() == DatabaseState.ONLINE) {
      MiniPetsSchema.ensure(zakum.database().jdbc());
    } else {
      getLogger().warning("Zakum DB is offline. Mini pet state won't persist.");
    }

    this.rt = new MiniPetsRuntime(this, zakum);
    this.rt.start();

    getServer().getPluginManager().registerEvents(new MiniPetsPlayerListener(rt), this);

    var cmd = getCommand("mpets");
    if (cmd != null) cmd.setExecutor(new MiniPetsCommand(rt));

    getLogger().info("ZakumMiniaturePets enabled.");
  }

  @Override
  public void onDisable() {
    if (rt != null) {
      rt.stop();
      rt = null;
    }
  }
}
