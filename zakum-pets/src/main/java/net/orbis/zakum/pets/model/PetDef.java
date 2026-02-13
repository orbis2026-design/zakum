package net.orbis.zakum.pets.model;

import org.bukkit.entity.EntityType;

public record PetDef(
  String id,
  String name,
  EntityType entityType,
  FollowMode followMode,
  int xpPerMobKill
) {}
