package net.orbis.zakum.api.actions;

/**
 * Listener for normalized Zakum actions.
 *
 * NOTE: called on the Bukkit main thread.
 * Keep handlers fast; push I/O to async executors.
 */
@FunctionalInterface
public interface ActionHandler {
  void onAction(ActionEvent event);
}
