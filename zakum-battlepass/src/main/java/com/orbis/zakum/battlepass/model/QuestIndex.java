package com.orbis.zakum.battlepass.model;

import com.orbis.zakum.api.action.ActionEvent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuestIndex {

    // Map<"block_break", List<QuestId>>
    private final Map<String, List<String>> triggerMap = new ConcurrentHashMap<>();
    
    // Map<QuestId, QuestDefinition>
    private final Map<String, QuestDefinition> questCache = new ConcurrentHashMap<>();

    public void register(QuestDefinition quest) {
        questCache.put(quest.getId(), quest);
        triggerMap.computeIfAbsent(quest.getTriggerType(), k -> new ArrayList<>())
                  .add(quest.getId());
    }

    /**
     * O(1) Narrowing: Only returns quests that care about this specific action type.
     */
    public List<QuestDefinition> findCandidates(ActionEvent event) {
        List<String> ids = triggerMap.get(event.getType());
        
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<QuestDefinition> matches = new ArrayList<>();
        for (String id : ids) {
            QuestDefinition q = questCache.get(id);
            // Pre-Filter: If quest requires DIAMOND_ORE, ignore COAL_ORE events here.
            if (q.matchesKey(event.getKey())) {
                matches.add(q);
            }
        }
        return matches;
    }
    
    public QuestDefinition getQuest(String id) {
        return questCache.get(id);
    }
}
