package net.orbis.zakum.pets;

import net.orbis.zakum.pets.model.FollowMode;
import net.orbis.zakum.pets.model.PetDef;
import net.orbis.zakum.pets.util.Colors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class PetLoader {

  private PetLoader() {}

  public static Map<String, PetDef> load(Plugin plugin) {
    ConfigurationSection root = plugin.getConfig().getConfigurationSection("pets");
    if (root == null) return Map.of();

    Map<String, PetDef> out = new LinkedHashMap<>();

    for (String id : root.getKeys(false)) {
      ConfigurationSection s = root.getConfigurationSection(id);
      if (s == null) continue;

      String name = Colors.color(s.getString("name", id));
      EntityType type = EntityType.valueOf(s.getString("entity", "WOLF").toUpperCase(Locale.ROOT));

      FollowMode mode;
      try {
        mode = FollowMode.valueOf(s.getString("followMode", "AI").toUpperCase(Locale.ROOT));
      } catch (Exception e) {
        mode = FollowMode.AI;
      }

      int xp = Math.max(0, s.getInt("xpPerMobKill", 10));

      out.put(id, new PetDef(id, name, type, mode, xp));
    }

    return Map.copyOf(out);
  }
}
