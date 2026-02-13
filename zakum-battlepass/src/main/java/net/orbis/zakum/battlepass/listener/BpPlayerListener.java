package net.orbis.zakum.battlepass.listener;

import net.orbis.zakum.battlepass.BattlePassRuntime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class BpPlayerListener implements Listener {

  private final BattlePassRuntime runtime;

  public BpPlayerListener(BattlePassRuntime runtime) {
    this.runtime = runtime;
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    runtime.onJoin(e.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    runtime.onQuit(e.getPlayer().getUniqueId());
  }
}
