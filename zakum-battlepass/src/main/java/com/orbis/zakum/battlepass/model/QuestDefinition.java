package com.orbis.zakum.battlepass.model;

public class QuestDefinition {
    private final String id;
    private final String triggerType; // e.g., "block_break"
    private final String key;         // e.g., "DIAMOND_ORE" (Optional)
    private final double goal;
    private final String rewardCommand;

    public QuestDefinition(String id, String triggerType, String key, double goal, String rewardCommand) {
        this.id = id;
        this.triggerType = triggerType;
        this.key = key;
        this.goal = goal;
        this.rewardCommand = rewardCommand;
    }

    public String getId() { return id; }
    public String getTriggerType() { return triggerType; }
    public String getKey() { return key; }
    public double getGoal() { return goal; }
    public String getRewardCommand() { return rewardCommand; }

    /**
     * Fast check: Does the event key match the quest requirement?
     * If quest key is null/empty, it accepts ANY key (Wildcard).
     */
    public boolean matchesKey(String eventKey) {
        if (this.key == null || this.key.isEmpty()) return true;
        return this.key.equalsIgnoreCase(eventKey);
    }
}
