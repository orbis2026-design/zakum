package net.orbis.zakum.crates.listener;

import net.orbis.zakum.crates.CrateRegistry;
import net.orbis.zakum.crates.CrateService;
import net.orbis.zakum.crates.db.CrateBlockStore;
import net.orbis.zakum.crates.model.CrateDef;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class CrateInteractListener implements Listener {

  private final CrateRegistry registry;
  private final CrateBlockStore blocks;
  private final CrateService service;

  public CrateInteractListener(CrateRegistry registry, CrateBlockStore blocks, CrateService service) {
    this.registry = registry;
    this.blocks = blocks;
    this.service = service;
  }

  @EventHandler(ignoreCancelled = true)
  public void onInteract(PlayerInteractEvent e) {
    if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

    Block b = e.getClickedBlock();
    if (b == null) return;

    String crateId = blocks.get(b.getWorld(), b.getX(), b.getY(), b.getZ());
    if (crateId == null) return;

    CrateDef def = registry.get(crateId);
    if (def == null) return;

    e.setCancelled(true);
    service.open(e.getPlayer(), def);
  }
}
