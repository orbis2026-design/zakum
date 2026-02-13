package net.orbis.zakum.pets.listener;

import net.orbis.zakum.pets.runtime.PetsRuntime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PetsPlayerListener implements Listener {

  private final PetsRuntime rt;

  public PetsPlayerListener(PetsRuntime rt) {
    this.rt = rt;
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    rt.onJoin(e.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    rt.onQuit(e.getPlayer().getUniqueId());
  }
}
