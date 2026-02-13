package net.orbis.zakum.minipets;

import net.orbis.zakum.minipets.model.FollowMode;
import net.orbis.zakum.minipets.model.MiniPetDef;
import net.orbis.zakum.minipets.util.Colors;
import net.orbis.zakum.minipets.util.ItemBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class MiniPetLoader {

  private MiniPetLoader() {}

  public static Map<String, MiniPetDef> load(Plugin plugin) {
    ConfigurationSection root = plugin.getConfig().getConfigurationSection("pets");
    if (root == null) return Map.of();

    Map<String, MiniPetDef> out = new LinkedHashMap<>();

    for (String id : root.getKeys(false)) {
      ConfigurationSection s = root.getConfigurationSection(id);
      if (s == null) continue;

      String name = Colors.color(s.getString("name", id));
      EntityType type = EntityType.valueOf(s.getString("entity", "RABBIT").toUpperCase(Locale.ROOT));

      FollowMode mode;
      try {
        mode = FollowMode.valueOf(s.getString("followMode", "TELEPORT").toUpperCase(Locale.ROOT));
      } catch (Exception e) {
        mode = FollowMode.TELEPORT;
      }

      ItemStack hat = null;
      var hatSec = s.getConfigurationSection("hat");
      if (hatSec != null) hat = ItemBuilder.fromMap(hatSec.getValues(false));

      EntityType ride = null;
      String rideRaw = s.getString("rideEntity", "");
      if (rideRaw != null && !rideRaw.isBlank()) {
        try { ride = EntityType.valueOf(rideRaw.toUpperCase(Locale.ROOT)); } catch (Exception ignored) {}
      }

      out.put(id, new MiniPetDef(id, name, type, mode, hat, ride));
    }

    return Map.copyOf(out);
  }
}
