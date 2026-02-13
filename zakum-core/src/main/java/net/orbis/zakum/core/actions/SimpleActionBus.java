package net.orbis.zakum.core.actions;

import net.orbis.zakum.api.actions.ActionBus;
import net.orbis.zakum.api.actions.ActionEvent;
import net.orbis.zakum.api.actions.ActionHandler;
import net.orbis.zakum.api.actions.ActionSubscription;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Small in-process bus.
 *
 * CopyOnWriteArrayList works well because:
 * - subscribe/unsubscribe is rare
 * - publish is frequent
 */
public final class SimpleActionBus implements ActionBus {

  private final CopyOnWriteArrayList<ActionHandler> handlers = new CopyOnWriteArrayList<>();

  @Override
  public void publish(ActionEvent event) {
    Objects.requireNonNull(event, "event");
    for (var h : handlers) h.onAction(event);
  }

  @Override
  public ActionSubscription subscribe(ActionHandler handler) {
    Objects.requireNonNull(handler, "handler");
    handlers.add(handler);
    return () -> handlers.remove(handler);
  }
}
