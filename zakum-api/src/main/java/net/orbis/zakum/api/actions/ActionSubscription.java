package net.orbis.zakum.api.actions;

/**
 * Handle returned by ActionBus#subscribe.
 * Call close() to unsubscribe.
 */
public interface ActionSubscription extends AutoCloseable {
  @Override
  void close();
}
