package net.orbis.zakum.battlepass.model;

public record QuestStep(
  String type,
  String key,
  String value,
  long required
) {}
