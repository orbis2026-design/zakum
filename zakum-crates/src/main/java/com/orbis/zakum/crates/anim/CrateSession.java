package com.orbis.zakum.crates.anim;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import java.util.UUID;

public class CrateSession {
    private final UUID playerUuid;
    private final String crateId;
    private final Inventory gui;
    private int currentTick;
    private final int maxTicks;
    private boolean isFinished;

    public CrateSession(Player player, String crateId, Inventory gui, int maxTicks) {
        this.playerUuid = player.getUniqueId();
        this.crateId = crateId;
        this.gui = gui;
        this.maxTicks = maxTicks;
        this.currentTick = 0;
        this.isFinished = false;
    }

    public void tick() {
        this.currentTick++;
        if (this.currentTick >= maxTicks) {
            this.isFinished = true;
        }
    }

    public boolean isFinished() { return isFinished; }
    public UUID getPlayerUuid() { return playerUuid; }
    public String getCrateId() { return crateId; }
    public Inventory getGui() { return gui; }
}
