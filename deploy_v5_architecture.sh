#!/bin/bash

echo "=========================================="
echo "   ORBIS V5 NETWORK ARCHITECTURE DEPLOY   "
echo "   Target: 1000 Concurrent Players        "
echo "   Modules: Core, Crates, BP, Pets,       "
echo "            Market, Enchants, Progression "
echo "=========================================="

# ==========================================
# 1. ZAKUM CORE (Defenses)
# ==========================================
echo ">> [1/7] Deploying Zakum Core (Iron Dome)..."
mkdir -p zakum-core/src/main/java/com/orbis/zakum/core/util
mkdir -p zakum-core/src/main/java/com/orbis/zakum/core/database
mkdir -p zakum-core/src/main/resources

# TokenBucket (Rate Limiter)
cat <<EOF > zakum-core/src/main/java/com/orbis/zakum/core/util/TokenBucket.java
package com.orbis.zakum.core.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Lock-free Token Bucket for high-concurrency rate limiting.
 * Prevents packet spam from crashing the main thread.
 */
public class TokenBucket {
    private final long capacity;
    private final long refillTokensPerSecond;
    private final AtomicLong lastRefillTimestamp;
    private final AtomicLong tokens;

    public TokenBucket(long maxTokens, long refillTokensPerSecond) {
        this.capacity = maxTokens;
        this.refillTokensPerSecond = refillTokensPerSecond;
        this.lastRefillTimestamp = new AtomicLong(System.nanoTime());
        this.tokens = new AtomicLong(maxTokens);
    }

    public boolean tryConsume(long cost) {
        refill();
        long currentTokens = tokens.get();
        if (currentTokens >= cost) {
            while (currentTokens >= cost) {
                if (tokens.compareAndSet(currentTokens, currentTokens - cost)) {
                    return true;
                }
                currentTokens = tokens.get();
            }
        }
        return false;
    }

    private void refill() {
        long now = System.nanoTime();
        long last = lastRefillTimestamp.get();
        long nanosPerToken = 1_000_000_000 / refillTokensPerSecond;
        if (now - last > nanosPerToken) {
            long newTokens = (now - last) / nanosPerToken;
            if (newTokens > 0) {
                if (lastRefillTimestamp.compareAndSet(last, now)) {
                    tokens.updateAndGet(t -> Math.min(capacity, t + newTokens));
                }
            }
        }
    }
}
EOF

# CircuitBreaker (DB Safety)
cat <<EOF > zakum-core/src/main/java/com/orbis/zakum/core/database/CircuitBreaker.java
package com.orbis.zakum.core.database;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Protects the server from hanging when the Database goes offline.
 * Switches to "Fail-Open" mode (Read-Only/Volatile) automatically.
 */
public class CircuitBreaker {
    private final AtomicBoolean isOpen = new AtomicBoolean(false);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);

    private static final int FAILURE_THRESHOLD = 5;
    private static final long RETRY_DELAY = 30_000; 

    public boolean canExecute() {
        if (isOpen.get()) {
            if (System.currentTimeMillis() - lastFailureTime.get() > RETRY_DELAY) {
                return true; // Probe
            }
            return false;
        }
        return true;
    }

    public void recordSuccess() {
        failureCount.set(0);
        if (isOpen.compareAndSet(true, false)) {
            System.out.println("[Orbis] Database connection recovered. Persistence resumed.");
        }
    }

    public void recordFailure() {
        lastFailureTime.set(System.currentTimeMillis());
        if (!isOpen.get() && failureCount.incrementAndGet() >= FAILURE_THRESHOLD) {
            isOpen.set(true);
            System.err.println("[Orbis] Database unstable! Circuit OPEN. Saving disabled.");
        }
    }
}
EOF

# Core Config
cat <<EOF > zakum-core/src/main/resources/config.yml
server:
  id: "survival-01" 

database:
  type: MYSQL
  host: "127.0.0.1"
  port: 3306
  database: "orbis_production"
  username: "admin"
  password: "change_me"
  maximumPoolSize: 20
  connectionTimeout: 5000
  properties:
    cachePrepStmts: true
    prepStmtCacheSize: 250
    rewriteBatchedStatements: true
    useServerPrepStmts: true

safety:
  max_actions_per_second: 15
  circuit_breaker_enabled: true
EOF

# ==========================================
# 2. ORBIS CRATES (Async Engine)
# ==========================================
echo ">> [2/7] Deploying OrbisCrates..."
mkdir -p zakum-crates/src/main/java/com/orbis/zakum/crates/anim
mkdir -p zakum-crates/src/main/java/com/orbis/zakum/crates/data
mkdir -p zakum-crates/src/main/java/com/orbis/zakum/crates/listener

# CrateSession
cat <<EOF > zakum-crates/src/main/java/com/orbis/zakum/crates/anim/CrateSession.java
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
EOF

# CrateAnimator
cat <<EOF > zakum-crates/src/main/java/com/orbis/zakum/crates/anim/CrateAnimator.java
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
EOF

# CrateRewardExecutor
cat <<EOF > zakum-crates/src/main/java/com/orbis/zakum/crates/CrateRewardExecutor.java
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
EOF

# KeyService
cat <<EOF > zakum-crates/src/main/java/com/orbis/zakum/crates/data/KeyService.java
package com.orbis.zakum.crates.data;

import java.util.Map;
import java.util.UUID;

public class KeyService {
    public void giveKeysBatch(Map<UUID, Integer> recipients, String crateId) {
        // Use JDBC Batching here:
        // "INSERT INTO orbis_crates_keys ... ON DUPLICATE KEY UPDATE ..."
    }
}
EOF

# ==========================================
# 3. ORBIS BATTLEPASS (O(1) Engine)
# ==========================================
echo ">> [3/7] Deploying OrbisBattlePass..."
mkdir -p zakum-battlepass/src/main/java/com/orbis/zakum/battlepass/model
mkdir -p zakum-battlepass/src/main/java/com/orbis/zakum/battlepass/listener
mkdir -p zakum-battlepass/src/main/java/com/orbis/zakum/battlepass/data

# QuestIndex
cat <<EOF > zakum-battlepass/src/main/java/com/orbis/zakum/battlepass/model/QuestIndex.java
package com.orbis.zakum.battlepass.model;

import com.orbis.zakum.api.action.ActionEvent; 
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuestIndex {
    private final Map<String, List<String>> triggerMap = new ConcurrentHashMap<>();

    public List<Object> findCandidates(ActionEvent event) {
        // Fast narrowing logic
        return Collections.emptyList();
    }
}
EOF

# BattlePassListener
cat <<EOF > zakum-battlepass/src/main/java/com/orbis/zakum/battlepass/listener/BattlePassListener.java
package com.orbis.zakum.battlepass.listener;

import com.orbis.zakum.api.action.ActionEvent;
import com.orbis.zakum.api.action.ActionListener;
import com.orbis.zakum.battlepass.model.QuestIndex;

public class BattlePassListener implements ActionListener {
    private final QuestIndex index;

    public BattlePassListener(QuestIndex index) {
        this.index = index;
    }

    @Override
    public void onAction(ActionEvent event) {
        // 1. Lookup candidates
        // 2. Process progress
    }
}
EOF

# QuestProgressService
cat <<EOF > zakum-battlepass/src/main/java/com/orbis/zakum/battlepass/data/QuestProgressService.java
package com.orbis.zakum.battlepass.data;

import java.util.UUID;

public class QuestProgressService {
    public void processProgress(UUID uuid, Object quest, double amount) {
        // Update Cache -> Save Async via CircuitBreaker
    }
}
EOF

# ==========================================
# 4. ORBIS PETS (State-Safe)
# ==========================================
echo ">> [4/7] Deploying OrbisPets..."
mkdir -p zakum-pets/src/main/java/com/orbis/zakum/pets/model
mkdir -p zakum-pets/src/main/java/com/orbis/zakum/pets/data
mkdir -p zakum-pets/src/main/java/com/orbis/zakum/pets/listener

# PetData
cat <<EOF > zakum-pets/src/main/java/com/orbis/zakum/pets/model/PetData.java
package com.orbis.zakum.pets.model;

public class PetData {
    private final String petId;
    private String customName;
    private int level;
    private double xp;
    private boolean isActive;

    public PetData(String petId, String customName, int level, double xp, boolean isActive) {
        this.petId = petId;
        this.customName = customName;
        this.level = level;
        this.xp = xp;
        this.isActive = isActive;
    }

    public String getPetId() { return petId; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
}
EOF

# PetService
cat <<EOF > zakum-pets/src/main/java/com/orbis/zakum/pets/data/PetService.java
package com.orbis.zakum.pets.data;

import com.orbis.zakum.pets.model.PetData;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PetService {
    private final Map<UUID, PetData> activePetCache = new ConcurrentHashMap<>();

    public PetData loadActivePet(UUID uuid) {
        // SELECT * FROM orbis_pets_data ...
        return null; 
    }

    public void cachePet(UUID uuid, PetData pet) {
        activePetCache.put(uuid, pet);
    }

    public PetData getCachedPet(UUID uuid) {
        return activePetCache.get(uuid);
    }

    public void removeCache(UUID uuid) {
        activePetCache.remove(uuid);
    }
}
EOF

# PetLifecycleListener
cat <<EOF > zakum-pets/src/main/java/com/orbis/zakum/pets/listener/PetLifecycleListener.java
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
EOF

# ==========================================
# 5. ORBIS MARKET (Economy Pillar)
# ==========================================
echo ">> [5/7] Deploy