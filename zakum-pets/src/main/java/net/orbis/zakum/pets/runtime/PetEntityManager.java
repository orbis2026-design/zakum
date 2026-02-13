package net.orbis.zakum.pets.runtime;

import net.orbis.zakum.pets.model.FollowMode;
import net.orbis.zakum.pets.model.PetDef;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pet entity lifecycle.
 *
 * - AI mode: rely on vanilla follow for tameables.
 * - TELEPORT mode: periodic repositioning behind the owner (single task).
 */
public final class PetEntityManager {

  private final Plugin plugin;
  private final NamespacedKey keyOwner;
  private final NamespacedKey keyPetId;

  // owner -> (entity uuid + follow mode)
  private final ConcurrentHashMap<UUID, Active> active = new ConcurrentHashMap<>();

  public PetEntityManager(Plugin plugin) {
    this.plugin = plugin;
    this.keyOwner = new NamespacedKey(plugin, "owner");
    this.keyPetId = new NamespacedKey(plugin, "pet_id");
  }

  public void startFollowerTask() {
    Bukkit.getScheduler().runTaskTimer(plugin, this::tickTeleportFollowers, 10L, 10L);
  }

  public void despawn(UUID owner) {
    Active a = active.remove(owner);
    if (a == null) return;

    Entity e = Bukkit.getEntity(a.entityId);
    if (e != null) e.remove();
  }

  public void summon(Player owner, PetDef def) {
    despawn(owner.getUniqueId());

    Location at = owner.getLocation().clone();
    Entity e = owner.getWorld().spawnEntity(at, def.entityType());

    e.setInvulnerable(true);
    e.setSilent(true);
    e.setPersistent(true);
    e.setCustomName(def.name());
    e.setCustomNameVisible(false);

    e.getPersistentDataContainer().set(keyOwner, PersistentDataType.STRING, owner.getUniqueId().toString());
    e.getPersistentDataContainer().set(keyPetId, PersistentDataType.STRING, def.id());

    if (def.followMode() == FollowMode.AI && e instanceof Tameable t) {
      t.setOwner(owner);
      t.setTamed(true);
    }

    active.put(owner.getUniqueId(), new Active(e.getUniqueId(), def.followMode()));
  }

  public UUID activeEntity(UUID owner) {
    Active a = active.get(owner);
    return a == null ? null : a.entityId;
  }

  private void tickTeleportFollowers() {
    for (Map.Entry<UUID, Active> en : active.entrySet()) {
      Active a = en.getValue();
      if (a.followMode != FollowMode.TELEPORT) continue;

      Player p = Bukkit.getPlayer(en.getKey());
      if (p == null || !p.isOnline()) continue;

      Entity e = Bukkit.getEntity(a.entityId);
      if (e == null || e.isDead()) continue;

      if (!e.getWorld().equals(p.getWorld())) {
        e.teleport(p.getLocation());
        continue;
      }

      Location pl = p.getLocation();
      Location el = e.getLocation();

      double d2 = pl.distanceSquared(el);
      if (d2 < 9.0) continue; // <=3 blocks

      Vector back = pl.getDirection().normalize().multiply(-1.5);
      Location target = pl.clone().add(back).add(0, 0.1, 0);

      e.teleport(target);
    }
  }

  private record Active(UUID entityId, FollowMode followMode) {}
}
