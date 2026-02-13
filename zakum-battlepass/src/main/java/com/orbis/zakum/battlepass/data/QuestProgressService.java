package com.orbis.zakum.battlepass.data;

import com.orbis.zakum.core.ZakumCore;
import com.orbis.zakum.battlepass.model.QuestDefinition;
import org.bukkit.Bukkit;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class QuestProgressService {

    private final ZakumCore core;
    // Cache: Map<UUID, Map<QuestId, CurrentProgress>>
    private final Map<UUID, Map<String, Double>> progressCache = new ConcurrentHashMap<>();
    // Cache: Map<UUID, Set<QuestId>> (Completed Quests)
    private final Map<UUID, java.util.Set<String>> completedCache = new ConcurrentHashMap<>();

    public QuestProgressService(ZakumCore core) {
        this.core = core;
    }
    
    // Called on Join (Async)
    public void loadProfile(UUID uuid) {
        core.getAsync().execute(() -> {
            if (!core.getCircuitBreaker().canExecute()) return;
            
            // 1. Load Progress
            // SELECT * FROM orbis_battlepass_progress WHERE uuid = ? ...
            // (Simplified for brevity: Populate maps)
            progressCache.put(uuid, new ConcurrentHashMap<>());
            completedCache.put(uuid, ConcurrentHashMap.newKeySet());
            
            core.getCircuitBreaker().recordSuccess();
        });
    }

    // Called on Quit
    public void unloadProfile(UUID uuid) {
        progressCache.remove(uuid);
        completedCache.remove(uuid);
    }

    public void processProgress(UUID uuid, QuestDefinition quest, double amount) {
        // 1. Check if already completed (Fast fail)
        if (completedCache.containsKey(uuid) && completedCache.get(uuid).contains(quest.getId())) {
            return;
        }

        // 2. Update Cache
        Map<String, Double> playerProgress = progressCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        double current = playerProgress.getOrDefault(quest.getId(), 0.0);
        double invalid = current + amount;
        
        playerProgress.put(quest.getId(), invalid);

        // 3. Check Completion
        if (invalid >= quest.getGoal()) {
            completeQuest(uuid, quest);
        } else {
            // 4. Save Partial Progress (Async & Throttled)
            saveProgressAsync(uuid, quest.getId(), invalid);
        }
    }

    private void completeQuest(UUID uuid, QuestDefinition quest) {
        completedCache.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(quest.getId());
        
        // Fire Reward (Sync)
        core.runSync(() -> {
             Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                 quest.getRewardCommand().replace("%player%", Bukkit.getPlayer(uuid).getName()));
        });
        
        // Save Completion to DB (Async)
        // saveCompletionAsync(uuid, quest.getId());
    }

    private void saveProgressAsync(UUID uuid, String questId, double progress) {
        core.getAsync().execute(() -> {
            if (core.getCircuitBreaker().canExecute()) {
                String sql = "INSERT INTO orbis_battlepass_progress (server_id, uuid, quest_id, progress) " +
                             "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE progress = VALUES(progress)";
                
                try (Connection conn = core.getDatabase().getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    
                    ps.setString(1, core.getConfig().getString("server.id"));
                    ps.setBytes(2, core.getDatabase().uuidToBytes(uuid));
                    ps.setString(3, questId);
                    ps.setDouble(4, progress);
                    
                    ps.executeUpdate();
                    core.getCircuitBreaker().recordSuccess();
                } catch (Exception e) {
                    core.getCircuitBreaker().recordFailure();
                    e.printStackTrace();
                }
            }
        });
    }
}
