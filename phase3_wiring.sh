#!/bin/bash

echo ">> STARTING PHASE 3 MEGA-DUMP: Enchants, Progression, Pets, Crates..."

# ==========================================
# 1. ORBIS ENCHANTS: THE "PROC" ENGINE
# ==========================================
echo ">> [1/4] Wiring OrbisEnchants..."

# 1.1 Enchant Definition (Rule Set)
cat <<EOF > zakum-enchants/src/main/java/com/orbis/zakum/enchants/model/EnchantDefinition.java
package com.orbis.zakum.enchants.model;

public class EnchantDefinition {
    private final String id;
    private final String triggerType; // e.g., "block_break", "entity_damage"
    private final double chancePerLevel;
    private final String effectType;  // e.g., "EXPLOSION", "POTION", "LIGHTNING"

    public EnchantDefinition(String id, String triggerType, double chancePerLevel, String effectType) {
        this.id = id;
        this.triggerType = triggerType;
        this.chancePerLevel = chancePerLevel;
        this.effectType = effectType;
    }

    public String getId() { return id; }
    public String getTriggerType() { return triggerType; }
    public String getEffectType() { return effectType; }

    public boolean roll(int level) {
        return Math.random() < (chancePerLevel * level);
    }
}
EOF

# 1.2 Enchant Registry (The Router)
cat <<EOF > zakum-enchants/src/main/java/com/orbis/zakum/enchants/logic/EnchantRegistry.java
package com.orbis.zakum.enchants.logic;

import com.orbis.zakum.enchants.model.EnchantDefinition;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EnchantRegistry {
    private final Map<String, EnchantDefinition> enchants = new ConcurrentHashMap<>();

    public void register(EnchantDefinition def) {
        enchants.put(def.getId(), def);
    }

    public EnchantDefinition get(String id) {
        return enchants.get(id);
    }
}
EOF

# 1.3 Enchant Profile Manager (Inventory Caching)
cat <<EOF > zakum-enchants/src/main/java/com/orbis/zakum/enchants/data/EnchantProfileManager.java
package com.orbis.zakum.enchants.data;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class EnchantProfileManager {
    
    // Map<PlayerUUID, EnchantProfile>
    private final Map<UUID, EnchantProfile> profiles = new ConcurrentHashMap<>();

    public EnchantProfile get(UUID uuid) {
        return profiles.computeIfAbsent(uuid, k -> new EnchantProfile());
    }

    public void updateProfile(Player player) {
        // Run on Main Thread (safe inventory access)
        ItemStack hand = player.getInventory().getItemInMainHand();
        Map<String, Integer> active = new HashMap<>();
        
        // This is where you'd parse NBT. 
        // For V5, we assume a helper exists or we use standard Lore parsing for now.
        // Pseudo-code:
        // if (hand.hasItemMeta()) {
        //     for (String line : hand.getItemMeta().getLore()) {
        //         // Parse "Explosive III" -> put("explosive", 3)
        //     }
        // }
        
        get(player.getUniqueId()).update(active);
    }
    
    public void clear(UUID uuid) {
        profiles.remove(uuid);
    }
}
EOF

# 1.4 OrbisEnchants Main Class
cat <<EOF > zakum-enchants/src/main/java/com/orbis/zakum/enchants/OrbisEnchants.java
package com.orbis.zakum.enchants;

import com.orbis.zakum.core.ZakumCore;
import com.orbis.zakum.enchants.data.EnchantProfileManager;
import com.orbis.zakum.enchants.logic.EnchantRegistry;
import com.orbis.zakum.enchants.logic.EnchantRouter;
import org.bukkit.plugin.java.JavaPlugin;

public class OrbisEnchants extends JavaPlugin {
    
    private ZakumCore core;
    private EnchantProfileManager profileManager;
    private EnchantRegistry registry;
    private EnchantRouter router;

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("ZakumCore") == null) return;
        this.core = ZakumCore.getInstance();

        this.profileManager = new EnchantProfileManager();
        this.registry = new EnchantRegistry();
        this.router = new EnchantRouter(this);

        // Register Listeners for Cache Updates (Hotbar Switch)
        getServer().getPluginManager().registerEvents(new com.orbis.zakum.enchants.listener.InventoryListener(profileManager), this);
        
        getLogger().info("OrbisEnchants Active. ActionBus Router Ready.");
    }

    public EnchantProfileManager getProfileManager() { return profileManager; }
    public EnchantRegistry getRegistry() { return registry; }
}
EOF

# 1.5 Inventory Listener (Cache Updater)
cat <<EOF > zakum-enchants/src/main/java/com/orbis/zakum/enchants/listener/InventoryListener.java
package com.orbis.zakum.enchants.listener;

import com.orbis.zakum.enchants.data.EnchantProfileManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class InventoryListener implements Listener {
    private final EnchantProfileManager manager;

    public InventoryListener(EnchantProfileManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent event) {
        // Fast update of cached enchants when switching tools
        manager.updateProfile(event.getPlayer());
    }
    
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.clear(event.getPlayer().getUniqueId());
    }
}
EOF

# ==========================================
# 2. ORBIS PROGRESSION: THE "RETENTION" ENGINE
# ==========================================
echo ">> [2/4] Wiring OrbisProgression..."

# 2.1 Progression Service (Auto-Saver)
cat <<EOF > zakum-progression/src/main/java/com/orbis/zakum/progression/data/ProgressionService.java
package com.orbis.zakum.progression.data;

import com.orbis.zakum.core.ZakumCore;
import com.orbis.zakum.progression.model.ProgressionData;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProgressionService {
    
    private final ZakumCore core;
    private final Map<UUID, ProgressionData> cache = new ConcurrentHashMap<>();

    public ProgressionService(ZakumCore core) {
        this.core = core;
    }
    
    public ProgressionData getData(UUID uuid) {
        return cache.computeIfAbsent(uuid, k -> new ProgressionData());
    }

    public void unload(UUID uuid) {
        ProgressionData data = cache.remove(uuid);
        if (data != null && data.isDirty()) {
            saveAsync(uuid, data);
        }
    }

    // Called every 5 minutes
    public void runAutoSave() {
        core.getAsync().execute(() -> {
            if (!core.getCircuitBreaker().canExecute()) return;

            for (Map.Entry<UUID, ProgressionData> entry : cache.entrySet()) {
                if (entry.getValue().isDirty()) {
                    saveToDb(entry.getKey(), entry.getValue());
                    entry.getValue().markClean();
                }
            }
        });
    }

    private void saveAsync(UUID uuid, ProgressionData data) {
        core.getAsync().execute(() -> saveToDb(uuid, data));
    }

    private void saveToDb(UUID uuid, ProgressionData data) {
        // JDBC Batch Logic
        String sql = "INSERT INTO orbis_progression (uuid, type, xp) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE xp = VALUES(xp)";
        
        try (Connection conn = core.getDatabase().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            conn.setAutoCommit(false);
            for (Map.Entry<String, Double> entry : data.getAll().entrySet()) {
                ps.setBytes(1, core.getDatabase().uuidToBytes(uuid));
                ps.setString(2, entry.getKey());
                ps.setDouble(3, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
            core.getCircuitBreaker().recordSuccess();
            
        } catch (Exception e) {
            core.getCircuitBreaker().recordFailure();
            e.printStackTrace();
        }
    }
}
EOF

# 2.2 OrbisProgression Main Class
cat <<EOF > zakum-progression/src/main/java/com/orbis/zakum/progression/OrbisProgression.java
package com.orbis.zakum.progression;

import com.orbis.zakum.core.ZakumCore;
import com.orbis.zakum.progression.data.ProgressionService;
import org.bukkit.plugin.java.JavaPlugin;

public class OrbisProgression extends JavaPlugin {
    
    private ZakumCore core;
    private ProgressionService service;

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("ZakumCore") == null) return;
        this.core = ZakumCore.getInstance();
        this.service = new ProgressionService(core);
        
        // Start Auto-Save Task (Every 5 mins = 6000 ticks)
        getServer().getScheduler().runTaskTimerAsynchronously(this, 
            () -> service.runAutoSave(), 6000L, 6000L);
            
        getServer().getPluginManager().registerEvents(new com.orbis.zakum.progression.listener.ProgressionListener(service), this);
        getLogger().info("OrbisProgression Active. Auto-Save Armed.");
    }
    
    public ProgressionService getService() { return service; }
}
EOF

# 2.3 Progression Listener (Join/Quit)
cat <<EOF > zakum-progression/src/main/java/com/orbis/zakum/progression/listener/ProgressionListener.java
package com.orbis.zakum.progression.listener;

import com.orbis.zakum.progression.data.ProgressionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ProgressionListener implements Listener {
    private final ProgressionService service;

    public ProgressionListener(ProgressionService service) {
        this.service = service;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        // Trigger load (could actually load from DB here if needed)
        // For now, lazy loading in service.getData() is acceptable for XP
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        service.unload(event.getPlayer().getUniqueId());
    }
}
EOF

# ==========================================
# 3. ORBIS PETS: STATE MANAGEMENT
# ==========================================
echo ">> [3/4] Wiring OrbisPets..."

cat <<EOF > zakum-pets/src/main/java/com/orbis/zakum/pets/OrbisPets.java
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
EOF

# ==========================================
# 4. ORBIS CRATES: ANIMATION WIRING
# ==========================================
echo ">> [4/4] Wiring OrbisCrates..."

cat <<EOF > zakum-crates/src/main/java/com/orbis/zakum/crates/OrbisCrates.java
package com.orbis.zakum.crates;

import com.orbis.zakum.core.ZakumCore;
import com.orbis.zakum.crates.anim.CrateAnimator;
import com.orbis.zakum.crates.listener.CrateInteractionListener;
import org.bukkit.plugin.java.JavaPlugin;

public class OrbisCrates extends JavaPlugin {
    
    private ZakumCore core;
    private CrateAnimator animator;
    private CrateRewardExecutor executor;

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("ZakumCore") == null) return;
        this.core = ZakumCore.getInstance();
        
        this.animator = new CrateAnimator();
        this.executor = new CrateRewardExecutor();
        
        // Start the Single-Ticker (Runs every 1 tick)
        getServer().getScheduler().runTaskTimer(this, animator, 1L, 1L);
        
        // Register Interaction Listener
        getServer().getPluginManager().registerEvents(new CrateInteractionListener(animator), this);
        
        getLogger().info("OrbisCrates Active. Animator Started.");
    }
    
    public CrateRewardExecutor getRewardExecutor() { return executor; }
    // public RenderService getRenderService() { ... } 
}
EOF

echo ">> PHASE 3 MEGA-DUMP COMPLETE."
echo "   - OrbisEnchants: Active (Cache + Router)"
echo "   - OrbisProgression: Active (Auto-Saver)"
echo "   - OrbisPets: Active (Lifecycle)"
echo "   - OrbisCrates: Active (Animator)"
echo "   SYSTEM READY FOR ACTIONBUS INTEGRATION."