package net.orbis.zakum.crates;

import net.orbis.zakum.api.ZakumApi;
import net.orbis.zakum.api.db.DatabaseState;
import net.orbis.zakum.crates.command.CratesCommand;
import net.orbis.zakum.crates.db.CrateBlockStore;
import net.orbis.zakum.crates.db.CratesSchema;
import net.orbis.zakum.crates.listener.CrateInteractListener;
import net.orbis.zakum.crates.model.CrateDef;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class CratesPlugin extends JavaPlugin {

  private ZakumApi zakum;

  private CrateRegistry registry;
  private CrateBlockStore store;
  private CrateService service;

  @Override
  public void onEnable() {
    saveDefaultConfig();

    this.zakum = Bukkit.getServicesManager().load(ZakumApi.class);
    if (zakum == null) {
      getLogger().severe("ZakumApi not found. Disabling ZakumCrates.");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }

    if (zakum.database().state() == DatabaseState.ONLINE) {
      CratesSchema.ensure(zakum.database().jdbc());
    } else {
      getLogger().warning("Zakum DB is offline. Crate blocks will not persist.");
    }

    this.registry = new CrateRegistry(CrateLoader.load(this));
    this.store = new CrateBlockStore(zakum);
    this.service = new CrateService(this);

    store.loadAll();

    getServer().getPluginManager().registerEvents(new CrateInteractListener(registry, store, service), this);

    var cmd = new CratesCommand(this, registry, store, service);
    var c = getCommand("zcrates");
    if (c != null) c.setExecutor(cmd);

    getLogger().info("ZakumCrates enabled. crates=" + registry.size());
  }
}
