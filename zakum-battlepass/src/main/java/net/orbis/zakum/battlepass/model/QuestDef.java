package net.orbis.zakum.battlepass.model;

import java.util.List;

public record QuestDef(
  String id,
  String name,
  long points,
  boolean premiumOnly,
  long premiumBonusPoints,
  List<QuestStep> steps
) {}
