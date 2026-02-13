package net.orbis.zakum.minipets.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public final class ItemBuilder {

  private ItemBuilder() {}

  public static ItemStack fromMap(Map<String, Object> m) {
    String matName = String.valueOf(m.getOrDefault("material", "STONE"));
    Material mat = Material.matchMaterial(matName);
    if (mat == null) mat = Material.STONE;

    ItemStack it = new ItemStack(mat, 1);
    ItemMeta meta = it.getItemMeta();
    if (meta != null) {
      String name = String.valueOf(m.getOrDefault("name", ""));
      if (!name.isBlank()) meta.setDisplayName(Colors.color(name));

      Object md = m.get("modelData");
      if (md != null) {
        try {
          int v = Integer.parseInt(String.valueOf(md));
          if (v > 0) meta.setCustomModelData(v);
        } catch (Exception ignored) {}
      }

      it.setItemMeta(meta);
    }
    return it;
  }
}
