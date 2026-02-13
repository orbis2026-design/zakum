package com.orbis.zakum.crates;

import com.orbis.zakum.crates.anim.CrateSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CrateRewardExecutor {
    public void finalizeSession(Player player, CrateSession session) {
        // 1. Sync Logic (Give Item)
        player.closeInventory();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "give " + player.getName() + " diamond 64");
        
        // 2. Async Logic (Log History)
        // core.getAsync().execute(() -> { ... });
    }
}
