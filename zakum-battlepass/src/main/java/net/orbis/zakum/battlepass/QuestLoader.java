package net.orbis.zakum.battlepass;

import net.orbis.zakum.battlepass.model.QuestDef;
import net.orbis.zakum.battlepass.model.QuestStep;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class QuestLoader {

  private QuestLoader() {}

  public static Map<String, QuestDef> loadFromJar(Plugin plugin) {
    try (var in = plugin.getResource("quests.yml")) {
      if (in == null) throw new IllegalStateException("quests.yml missing");

      var yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
      ConfigurationSection root = yaml.getConfigurationSection("quests");
      if (root == null) return Map.of();

      Map<String, QuestDef> out = new LinkedHashMap<>();

      for (String questId : root.getKeys(false)) {
        ConfigurationSection q = root.getConfigurationSection(questId);
        if (q == null) continue;

        String name = q.getString("name", questId);
        long points = q.getLong("points", 0);
        boolean premiumOnly = q.getBoolean("premiumOnly", false);
        long premiumBonusPoints = q.getLong("premiumBonusPoints", 0);

        List<QuestStep> steps = new ArrayList<>();
        for (var raw : q.getMapList("steps")) {
          String type = String.valueOf(raw.getOrDefault("type", "")).trim();
          String key = String.valueOf(raw.getOrDefault("key", "")).trim();
          String value = String.valueOf(raw.getOrDefault("value", "")).trim();
          long required = Long.parseLong(String.valueOf(raw.getOrDefault("required", "1")));

          if (type.isBlank() || required <= 0) continue;
          steps.add(new QuestStep(type, key, value, required));
        }

        if (steps.isEmpty()) continue;

        out.put(questId, new QuestDef(
          questId,
          name,
          points,
          premiumOnly,
          premiumBonusPoints,
          List.copyOf(steps)
        ));
      }

      return Map.copyOf(out);

    } catch (Exception e) {
      plugin.getLogger().severe("Failed to load quests.yml: " + e.getMessage());
      return Map.of();
    }
  }
}
