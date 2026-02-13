package net.orbis.zakum.minipets.runtime;

import net.orbis.zakum.api.ZakumApi;
import net.orbis.zakum.minipets.MiniPetLoader;
import net.orbis.zakum.minipets.db.MiniPetsStorage;
import net.orbis.zakum.minipets.model.MiniPetDef;
import net.orbis.zakum.minipets.state.MiniPetPlayerState;
import net.orbis.zakum.minipets.util.Colors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class MiniPetsRuntime {

  private final Plugin plugin;
  private final ZakumApi zakum;
  private final String serverId;

  private final Map<String, MiniPetDef> defs;
  private final MiniPetEntityManager entities;

  private final ConcurrentMap<UUID, MiniPetPlayerState> state = new ConcurrentHashMap<>();
  private final ConcurrentHashMap.KeySetView<UUID, Boolean> dirty = ConcurrentHashMap.newKeySet();

  private final ConcurrentMap<UUID, ItemStack> savedHelmet = new ConcurrentHashMap<>();

  public MiniPetsRuntime(Plugin plugin, ZakumApi zakum) {
    this.plugin = plugin;
    this.zakum = zakum;
    this.serverId = zakum.server().serverId();

    this.defs = MiniPetLoader.load(plugin);
    this.entities = new MiniPetEntityManager(plugin);
  }

  public void start() {
    long interval = plugin.getConfig().getLong("follow.intervalTicks", 10L);
    entities.startFollowTask(interval);

    for (Player p : Bukkit.getOnlinePlayers()) loadAsync(p.getUniqueId());

    int flushSeconds = Math.max(3, plugin.getConfig().getInt("flush.intervalSeconds", 10));
    Bukkit.getScheduler().runTaskTimer(plugin, this::flushDirtyAsync, flushSeconds * 20L, flushSeconds * 20L);
  }

  public void stop() {
    flushAllAsync();
  }

  public Map<String, MiniPetDef> defs() { return defs; }

  public MiniPetPlayerState get(UUID uuid) { return state.get(uuid); }

  public void onJoin(UUID uuid) { loadAsync(uuid); }

  public void onQuit(UUID uuid) {
    removeHat(uuid);
    entities.despawn(uuid);
    flushOneAsync(uuid);
    state.remove(uuid);
    dirty.remove(uuid);
    savedHelmet.remove(uuid);
  }

  public void summon(Player p, String petId) {
    MiniPetDef def = defs.get(petId);
    if (def == null) {
      p.sendMessage("§cUnknown pet.");
      return;
    }

    MiniPetPlayerState st = state.computeIfAbsent(p.getUniqueId(), __ -> new MiniPetPlayerState());
    st.petId = petId;
    dirty.add(p.getUniqueId());

    entities.summon(p, def);

    if (st.hat) applyHat(p, def);

    p.sendMessage(Colors.color("&aSummoned: &f" + def.name()));
  }

  public void dismiss(Player p) {
    removeHat(p.getUniqueId());
    entities.despawn(p.getUniqueId());
    p.sendMessage("§7Pet dismissed.");
  }

  public void toggleHat(Player p) {
    MiniPetPlayerState st = state.computeIfAbsent(p.getUniqueId(), __ -> new MiniPetPlayerState());
    st.hat = !st.hat;
    dirty.add(p.getUniqueId());

    MiniPetDef def = defs.get(st.petId);
    if (def == null) {
      p.sendMessage("§7No pet selected.");
      return;
    }

    if (st.hat) applyHat(p, def);
    else removeHat(p.getUniqueId());

    p.sendMessage("§dHat§7: " + (st.hat ? "§aON" : "§cOFF"));
  }

  public void toggleRide(Player p) {
    MiniPetPlayerState st = state.computeIfAbsent(p.getUniqueId(), __ -> new MiniPetPlayerState());
    st.ride = !st.ride;
    dirty.add(p.getUniqueId());

    Entity pet = entities.entity(p.getUniqueId());
    if (pet == null) {
      p.sendMessage("§7No active pet to ride.");
      return;
    }

    if (st.ride) {
      pet.addPassenger(p);
    } else {
      pet.removePassenger(p);
    }

    p.sendMessage("§dRide§7: " + (st.ride ? "§aON" : "§cOFF"));
  }

  private void applyHat(Player p, MiniPetDef def) {
    if (def.hatItem() == null) return;

    savedHelmet.putIfAbsent(p.getUniqueId(), p.getInventory().getHelmet());

    ItemStack hat = def.hatItem().clone();
    p.getInventory().setHelmet(hat);
  }

  private void removeHat(UUID uuid) {
    Player p = Bukkit.getPlayer(uuid);
    if (p == null) return;

    ItemStack prev = savedHelmet.remove(uuid);
    if (prev != null) p.getInventory().setHelmet(prev);
  }

  private void loadAsync(UUID uuid) {
    zakum.async().execute(() -> {
      MiniPetPlayerState st = MiniPetsStorage.load(zakum, serverId, uuid);
      state.put(uuid, st);
    });
  }

  private void flushDirtyAsync() {
    if (dirty.isEmpty()) return;

    for (UUID uuid : dirty) {
      dirty.remove(uuid);
      flushOneAsync(uuid);
    }
  }

  private void flushOneAsync(UUID uuid) {
    MiniPetPlayerState st = state.get(uuid);
    if (st == null) return;

    zakum.async().execute(() -> MiniPetsStorage.save(zakum, serverId, uuid, st));
  }

  private void flushAllAsync() {
    for (UUID uuid : state.keySet()) flushOneAsync(uuid);
  }
}
