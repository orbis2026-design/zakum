package net.orbis.zakum.crates.model;

import net.orbis.zakum.api.util.WeightedTable;
import org.bukkit.inventory.ItemStack;

public record CrateDef(
  String id,
  String name,
  boolean publicOpen,
  int publicRadius,
  ItemStack keyItem,
  WeightedTable<RewardDef> rewards
) {}
