package net.orbis.zakum.pets.util;

import org.bukkit.ChatColor;

public final class Colors {
  private Colors() {}
  public static String color(String s) {
    if (s == null) return "";
    return ChatColor.translateAlternateColorCodes('&', s);
  }
}
