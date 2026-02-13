package com.orbis.zakum.crates.anim;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CrateAnimator implements Runnable {
    private final Map<UUID, CrateSession> activeSessions = new ConcurrentHashMap<>();

    public void startSession(CrateSession session) {
        activeSessions.put(session.getPlayerUuid(), session);
    }

    public boolean isOpening(UUID uuid) {
        return activeSessions.containsKey(uuid);
    }

    @Override
    public void run() {
        if (activeSessions.isEmpty()) return;

        Iterator<CrateSession> it = activeSessions.values().iterator();
        while (it.hasNext()) {
            CrateSession session = it.next();
            Player player = Bukkit.getPlayer(session.getPlayerUuid());

            if (player == null || !player.isOnline()) {
                it.remove();
                continue;
            }

            session.tick();
            if (session.isFinished()) {
                it.remove();
                // Call RewardExecutor here
            }
        }
    }
}
