package com.orbis.zakum.pets.listener;

import com.orbis.zakum.pets.data.PetService;
import com.orbis.zakum.pets.model.PetData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PetLifecycleListener implements Listener {
    private final PetService service;

    public PetLifecycleListener(PetService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        PetData activePet = service.loadActivePet(event.getUniqueId());
        if (activePet != null) {
            service.cachePet(event.getUniqueId(), activePet);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PetData pet = service.getCachedPet(event.getPlayer().getUniqueId());
        if (pet != null && pet.isActive()) {
            // Spawn Pet (Sync)
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        service.removeCache(event.getPlayer().getUniqueId());
    }
}
