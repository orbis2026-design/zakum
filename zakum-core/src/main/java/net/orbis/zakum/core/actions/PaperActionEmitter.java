package net.orbis.zakum.core.actions;

import net.orbis.zakum.api.actions.ActionBus;
import net.orbis.zakum.api.actions.ActionEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Emits a minimal baseline of actions common to most gamemodes.
 */
public final class PaperActionEmitter implements Listener {

  private final ActionBus bus;

  public PaperActionEmitter(ActionBus bus) {
    this.bus = bus;
  }

  @EventHandler(ignoreCancelled = true)
  public void onBreak(BlockBreakEvent e) {
    Player p = e.getPlayer();
    Material m = e.getBlock().getType();

    bus.publish(new ActionEvent(
      "block_break",
      p.getUniqueId(),
      1,
      "material",
      m.name()
    ));
  }

  @EventHandler(ignoreCancelled = true)
  public void onEntityDeath(EntityDeathEvent e) {
    Player killer = e.getEntity().getKiller();
    if (killer == null) return;

    bus.publish(new ActionEvent(
      "mob_kill",
      killer.getUniqueId(),
      1,
      "entity",
      e.getEntityType().name()
    ));
  }
}
