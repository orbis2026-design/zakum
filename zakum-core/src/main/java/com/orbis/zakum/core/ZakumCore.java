package com.orbis.zakum.core;

import com.orbis.zakum.core.database.CircuitBreaker;
import com.orbis.zakum.core.database.DatabaseManager;
import com.orbis.zakum.core.util.TokenBucket;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ZakumCore extends JavaPlugin {

    private static ZakumCore instance;
    private DatabaseManager database;
    private CircuitBreaker circuitBreaker;
    private Executor asyncPool;
    
    // Player rate limiters
    private final Map<UUID, TokenBucket> playerBuckets = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // 1. Initialize Thread Pool (Async Spine)
        this.asyncPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        // 2. Initialize Safety Systems
        this.circuitBreaker = new CircuitBreaker();

        // 3. Initialize Database
        try {
            this.database = new DatabaseManager(getConfig());
            getLogger().info("Database connected successfully.");
        } catch (Exception e) {
            getLogger().severe("FAILED TO CONNECT TO DATABASE! Server running in DEGRADED mode.");
            circuitBreaker.recordFailure(); // Trip the breaker immediately
            e.printStackTrace();
        }
        
        // 4. Register ActionBus (Placeholder for API module)
        // ActionBus.init(this);
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
    }

    public static ZakumCore getInstance() { return instance; }
    public DatabaseManager getDatabase() { return database; }
    public CircuitBreaker getCircuitBreaker() { return circuitBreaker; }
    
    // Expose Async Executor for other modules
    public Executor getAsync() { return asyncPool; }
    
    // Rate Limiter Access
    public TokenBucket getRateLimiter(org.bukkit.entity.Player player) {
        // 15 actions/sec burst limit
        return playerBuckets.computeIfAbsent(player.getUniqueId(), k -> new TokenBucket(15, 5)); 
    }
    
    // Helper to run Sync tasks from Async threads
    public void runSync(Runnable r) {
        getServer().getScheduler().runTask(this, r);
    }
}
