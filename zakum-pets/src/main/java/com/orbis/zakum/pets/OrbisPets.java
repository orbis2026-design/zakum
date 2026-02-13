package com.orbis.zakum.pets;

import com.orbis.zakum.core.ZakumCore;
import com.orbis.zakum.pets.data.PetService;
import com.orbis.zakum.pets.listener.PetLifecycleListener;
import org.bukkit.plugin.java.JavaPlugin;

public class OrbisPets extends JavaPlugin {
    
    private ZakumCore core;
    private PetService petService;
    // private PetManager petManager; // Handles NMS/Entity Spawning

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("ZakumCore") == null) return;
        this.core = ZakumCore.getInstance();
        
        this.petService = new PetService(core);
        // this.petManager = new PetManager(); 
        
        // Register Lifecycle (Async Load -> Sync Spawn)
        getServer().getPluginManager().registerEvents(new PetLifecycleListener(petService), this);
        getLogger().info("OrbisPets Active. State Engine Ready.");
    }
    
    public PetService getPetService() { return petService; }
    // public PetManager getPetManager() { return petManager; } 
    // Stub for compiler until Manager is built
    public PetManagerStub getPetManager() { return new PetManagerStub(); } 
}

// Stub class to allow compilation of listener
class PetManagerStub {
    public void spawnPet(org.bukkit.entity.Player p, com.orbis.zakum.pets.model.PetData d) {}
    public void despawnPet(org.bukkit.entity.Player p) {}
}
