package net.orbis.zakum.pets;

import net.orbis.zakum.api.ZakumApi;
import net.orbis.zakum.api.db.DatabaseState;
import net.orbis.zakum.pets.command.PetsCommand;
import net.orbis.zakum.pets.db.PetsSchema;
import net.orbis.zakum.pets.listener.PetsPlayerListener;
import net.orbis.zakum.pets.runtime.PetsRuntime;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class PetsPlugin extends JavaPlugin {

  private ZakumApi zakum;
  private PetsRuntime rt;

  @Override
  public void onEnable() {
    saveDefaultConfig();

    this.zakum = Bukkit.getServicesManager().load(ZakumApi.class);
    if (zakum == null) {
      getLogger().severe("ZakumApi not found. Disabling ZakumPets.");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }

    if (zakum.database().state() == DatabaseState.ONLINE) {
      PetsSchema.ensure(zakum.database().jdbc());
    } else {
      getLogger().warning("Zakum DB is offline. Pet progress won't persist.");
    }

    this.rt = new PetsRuntime(this, zakum);
    this.rt.start();

    getServer().getPluginManager().registerEvents(new PetsPlayerListener(rt), this);

    var cmd = getCommand("pets");
    if (cmd != null) cmd.setExecutor(new PetsCommand(rt));

    getLogger().info("ZakumPets enabled.");
  }

  @Override
  public void onDisable() {
    if (rt != null) {
      rt.stop();
      rt = null;
    }
  }
}
