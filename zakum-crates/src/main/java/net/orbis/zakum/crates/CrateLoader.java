package net.orbis.zakum.crates;

import net.orbis.zakum.api.util.WeightedTable;
import net.orbis.zakum.crates.model.CrateDef;
import net.orbis.zakum.crates.model.RewardDef;
import net.orbis.zakum.crates.util.ItemBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;

public final class CrateLoader {

  private CrateLoader() {}

  public static Map<String, CrateDef> load(Plugin plugin) {
    ConfigurationSection root = plugin.getConfig().getConfigurationSection("crates");
    if (root == null) return Map.of();

    Map<String, CrateDef> out = new LinkedHashMap<>();

    for (String id : root.getKeys(false)) {
      ConfigurationSection c = root.getConfigurationSection(id);
      if (c == null) continue;

      String name = c.getString("name", id);
      boolean publicOpen = c.getBoolean("publicOpen", false);
      int publicRadius = Math.max(0, c.getInt("publicRadius", 8));

      ItemStack key = null;
      var keySec = c.getConfigurationSection("key");
      if (keySec != null) key = ItemBuilder.fromMap(keySec.getValues(false));
      if (key == null) continue;

      var builder = WeightedTable.<RewardDef>builder();

      for (Map<?, ?> raw : c.getMapList("rewards")) {
        double w = doubleOf(raw.get("weight"), 1.0);

        List<String> msgs = listOfStrings(raw.get("messages"));
        List<String> cmds = listOfStrings(raw.get("commands"));

        List<ItemStack> items = new ArrayList<>();
        Object itemsObj = raw.get("items");
        if (itemsObj instanceof List<?> l) {
          for (Object o : l) {
            if (o instanceof Map<?, ?> im) {
              @SuppressWarnings("unchecked")
              Map<String, Object> mm = (Map<String, Object>) im;
              items.add(ItemBuilder.fromMap(mm));
            }
          }
        }

        RewardDef r = new RewardDef(w, List.copyOf(msgs), List.copyOf(cmds), List.copyOf(items));
        builder.add(r, w);
      }

      WeightedTable<RewardDef> rewards;
      try {
        rewards = builder.build();
      } catch (Exception ignored) {
        continue;
      }

      out.put(id, new CrateDef(id, name, publicOpen, publicRadius, key, rewards));
    }

    return Map.copyOf(out);
  }

  private static double doubleOf(Object o, double def) {
    try {
      if (o == null) return def;
      return Double.parseDouble(String.valueOf(o));
    } catch (Exception ignored) {
      return def;
    }
  }

  private static List<String> listOfStrings(Object o) {
    if (!(o instanceof List<?> l)) return List.of();
    List<String> out = new ArrayList<>();
    for (Object x : l) out.add(String.valueOf(x));
    return out;
  }
}
