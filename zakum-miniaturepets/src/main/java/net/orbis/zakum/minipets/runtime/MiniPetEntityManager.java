package net.orbis.zakum.minipets.runtime;

import net.orbis.zakum.minipets.model.MiniPetDef;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MiniPetEntityManager {

  private final Plugin plugin;
  private final NamespacedKey keyOwner;
  private final NamespacedKey keyPetId;

  // owner -> entity
  private final ConcurrentHashMap<UUID, UUID> active = new ConcurrentHashMap<>();

  public MiniPetEntityManager(Plugin plugin) {
    this.plugin = plugin;
    this.keyOwner = new NamespacedKey(plugin, "owner");
    this.keyPetId = new NamespacedKey(plugin, "pet_id");
  }

  public void startFollowTask(long intervalTicks) {
    long t = Math.max(1L, intervalTicks);
    Bukkit.getScheduler().runTaskTimer(plugin, this::tick, t, t);
  }

  public void despawn(UUID owner) {
    UUID eId = active.remove(owner);
    if (eId == null) return;

    Entity e = Bukkit.getEntity(eId);
    if (e != null) e.remove();
  }

  public void summon(Player owner, MiniPetDef def) {
    despawn(owner.getUniqueId());

    Entity e = owner.getWorld().spawnEntity(owner.getLocation(), def.entityType());
    e.setInvulnerable(true);
    e.setSilent(true);
    e.setPersistent(true);
    e.setCustomName(def.name());
    e.setCustomNameVisible(false);

    e.getPersistentDataContainer().set(keyOwner, PersistentDataType.STRING, owner.getUniqueId().toString());
    e.getPersistentDataContainer().set(keyPetId, PersistentDataType.STRING, def.id());

    active.put(owner.getUniqueId(), e.getUniqueId());
  }

  public Entity entity(UUID owner) {
    UUID id = active.get(owner);
    if (id == null) return null;
    return Bukkit.getEntity(id);
  }

  private void tick() {
    for (Map.Entry<UUID, UUID> en : active.entrySet()) {
      Player p = Bukkit.getPlayer(en.getKey());
      if (p == null || !p.isOnline()) continue;

      Entity e = Bukkit.getEntity(en.getValue());
      if (e == null || e.isDead()) continue;

      if (!e.getWorld().equals(p.getWorld())) {
        e.teleport(p.getLocation());
        continue;
      }

      Location pl = p.getLocation();
      Location el = e.getLocation();

      double d2 = pl.distanceSquared(el);
      if (d2 < 9.0) continue;

      Vector back = pl.getDirection().normalize().multiply(-1.3);
      Location target = pl.clone().add(back).add(0, 0.1, 0);
      e.teleport(target);
    }
  }
}
