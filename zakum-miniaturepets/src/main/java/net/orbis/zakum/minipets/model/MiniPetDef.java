package net.orbis.zakum.minipets.model;

import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

public record MiniPetDef(
  String id,
  String name,
  EntityType entityType,
  FollowMode followMode,
  ItemStack hatItem,
  EntityType rideEntityType
) {}
