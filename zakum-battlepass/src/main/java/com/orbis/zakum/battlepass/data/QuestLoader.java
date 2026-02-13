package com.orbis.zakum.battlepass.data;

import com.orbis.zakum.battlepass.model.QuestDefinition;
import com.orbis.zakum.battlepass.model.QuestIndex;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class QuestLoader {
    
    public void load(FileConfiguration config, QuestIndex index) {
        ConfigurationSection section = config.getConfigurationSection("quests");
        if (section == null) return;
        
        for (String key : section.getKeys(false)) {
            ConfigurationSection q = section.getConfigurationSection(key);
            
            QuestDefinition def = new QuestDefinition(
                key,
                q.getString("type"),       // "block_break"
                q.getString("key", ""),    // "DIAMOND_ORE"
                q.getDouble("goal"),
                q.getString("reward")
            );
            
            index.register(def);
        }
    }
}
