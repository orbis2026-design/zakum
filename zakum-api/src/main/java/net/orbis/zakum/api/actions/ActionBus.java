package net.orbis.zakum.api.actions;

/**
 * In-process pub/sub for normalized gameplay actions.
 *
 * This is the shared "trigger surface" reused by BattlePass, Jobs, Skills, etc.
 */
public interface ActionBus {

  void publish(ActionEvent event);

  ActionSubscription subscribe(ActionHandler handler);
}
