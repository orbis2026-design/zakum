package net.orbis.zakum.battlepass.state;

import java.util.HashMap;
import java.util.Map;

public final class PlayerBpState {

  public long points;
  public int tier;

  public volatile boolean premium = false;

  private final Map<String, StepState> quests = new HashMap<>();

  public StepState step(String questId) {
    return quests.computeIfAbsent(questId, k -> new StepState());
  }

  public Map<String, StepState> all() {
    return quests;
  }

  public static final class StepState {
    public int stepIdx = 0;
    public long progress = 0;
  }
}
