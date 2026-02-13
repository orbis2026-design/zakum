package net.orbis.zakum.minipets.listener;

import net.orbis.zakum.minipets.runtime.MiniPetsRuntime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class MiniPetsPlayerListener implements Listener {

  private final MiniPetsRuntime rt;

  public MiniPetsPlayerListener(MiniPetsRuntime rt) {
    this.rt = rt;
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent e) { rt.onJoin(e.getPlayer().getUniqueId()); }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) { rt.onQuit(e.getPlayer().getUniqueId()); }
}
