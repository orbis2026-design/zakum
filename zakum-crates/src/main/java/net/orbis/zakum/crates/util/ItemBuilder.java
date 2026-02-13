package net.orbis.zakum.crates.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ItemBuilder {

  private ItemBuilder() {}

  @SuppressWarnings("unchecked")
  public static ItemStack fromMap(Map<String, Object> m) {
    String matName = String.valueOf(m.getOrDefault("material", "STONE"));
    Material mat = Material.matchMaterial(matName);
    if (mat == null) mat = Material.STONE;

    int amount = intOf(m.getOrDefault("amount", 1), 1);

    ItemStack it = new ItemStack(mat, Math.max(1, amount));
    ItemMeta meta = it.getItemMeta();
    if (meta != null) {
      String name = str(m.get("name"));
      if (!name.isBlank()) meta.setDisplayName(color(name));

      Object loreObj = m.get("lore");
      if (loreObj instanceof List<?> list) {
        List<String> lore = new ArrayList<>();
        for (Object o : list) lore.add(color(String.valueOf(o)));
        meta.setLore(lore);
      }

      int md = intOf(m.getOrDefault("modelData", 0), 0);
      if (md > 0) meta.setCustomModelData(md);

      it.setItemMeta(meta);
    }
    return it;
  }

  public static String color(String s) {
    return ChatColor.translateAlternateColorCodes('&', s);
  }

  private static String str(Object o) {
    if (o == null) return "";
    return String.valueOf(o).trim();
  }

  private static int intOf(Object o, int def) {
    try {
      if (o == null) return def;
      return Integer.parseInt(String.valueOf(o));
    } catch (Exception ignored) {
      return def;
    }
  }
}
