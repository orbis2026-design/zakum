package net.orbis.zakum.pets.runtime;

import net.orbis.zakum.api.ZakumApi;
import net.orbis.zakum.api.actions.ActionEvent;
import net.orbis.zakum.api.actions.ActionSubscription;
import net.orbis.zakum.api.boosters.BoosterKind;
import net.orbis.zakum.api.entitlements.EntitlementScope;
import net.orbis.zakum.pets.LevelCurve;
import net.orbis.zakum.pets.PetLoader;
import net.orbis.zakum.pets.db.PetsStorage;
import net.orbis.zakum.pets.model.PetDef;
import net.orbis.zakum.pets.state.PetPlayerState;
import net.orbis.zakum.pets.util.Colors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PetsRuntime {

  private final Plugin plugin;
  private final ZakumApi zakum;

  private final String serverId;
  private final Map<String, PetDef> defs;
  private final LevelCurve curve;

  private final PetEntityManager entities;

  private final ConcurrentMap<UUID, PetPlayerState> state = new ConcurrentHashMap<>();
  private final ConcurrentHashMap.KeySetView<UUID, Boolean> dirty = ConcurrentHashMap.newKeySet();

  private volatile ActionSubscription sub;

  public PetsRuntime(Plugin plugin, ZakumApi zakum) {
    this.plugin = plugin;
    this.zakum = zakum;

    this.serverId = zakum.server().serverId();
    this.defs = PetLoader.load(plugin);

    int maxLvl = plugin.getConfig().getInt("levels.maxLevel", 50);
    long base = plugin.getConfig().getLong("levels.xpBase", 100);
    double growth = plugin.getConfig().getDouble("levels.xpGrowth", 1.15);

    this.curve = new LevelCurve(maxLvl, base, growth);
    this.entities = new PetEntityManager(plugin);
  }

  public void start() {
    entities.startFollowerTask();

    this.sub = zakum.actions().subscribe(this::onAction);

    for (Player p : Bukkit.getOnlinePlayers()) loadAsync(p.getUniqueId());

    int flushSeconds = Math.max(2, plugin.getConfig().getInt("flush.intervalSeconds", 5));
    Bukkit.getScheduler().runTaskTimer(plugin, this::flushDirtyAsync, flushSeconds * 20L, flushSeconds * 20L);
  }

  public void stop() {
    if (sub != null) sub.close();
    sub = null;

    flushAllAsync();
  }

  public Map<String, PetDef> defs() { return defs; }

  public PetPlayerState get(UUID uuid) { return state.get(uuid); }

  public void onJoin(UUID uuid) { loadAsync(uuid); }

  public void onQuit(UUID uuid) {
    entities.despawn(uuid);
    flushOneAsync(uuid);
    state.remove(uuid);
    dirty.remove(uuid);
  }

  public void summon(Player p, String petId) {
    PetDef def = defs.get(petId);
    if (def == null) {
      p.sendMessage("§cUnknown pet.");
      return;
    }

    PetPlayerState st = state.computeIfAbsent(p.getUniqueId(), __ -> new PetPlayerState());
    st.selectedPetId = petId;
    dirty.add(p.getUniqueId());

    entities.summon(p, def);

    p.sendMessage(Colors.color("&aSummoned pet: &f" + def.name()));
  }

  public void dismiss(Player p) {
    entities.despawn(p.getUniqueId());
    p.sendMessage("§7Pet dismissed.");
  }

  private void onAction(ActionEvent e) {
    if (!e.type().equalsIgnoreCase("mob_kill")) return;

    PetPlayerState st = state.get(e.playerId());
    if (st == null) return;

    if (st.selectedPetId == null || st.selectedPetId.isBlank()) return;

    PetDef def = defs.get(st.selectedPetId);
    if (def == null) return;

    long baseXp = def.xpPerMobKill();
    if (baseXp <= 0) return;

    double mult = zakum.boosters().multiplier(e.playerId(), EntitlementScope.SERVER, serverId, BoosterKind.PETS_XP);
    long xpGain = (long) Math.max(1, Math.floor(baseXp * mult));

    addXp(e.playerId(), st, xpGain);
  }

  private void addXp(UUID uuid, PetPlayerState st, long xpGain) {
    if (st.level >= curve.maxLevel()) return;

    st.xp += xpGain;
    dirty.add(uuid);

    // Level-up loop (safe guard)
    int guard = 0;
    while (st.level < curve.maxLevel() && guard++ < 50) {
      long need = curve.xpRequiredForNext(st.level);
      if (st.xp < need) break;

      st.xp -= need;
      st.level++;

      Player p = Bukkit.getPlayer(uuid);
      if (p != null && p.isOnline()) {
        p.sendMessage("§aYour pet reached level §f" + st.level + "§a!");
      }
    }
  }

  private void loadAsync(UUID uuid) {
    zakum.async().execute(() -> {
      PetPlayerState st = PetsStorage.load(zakum, serverId, uuid);
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
    PetPlayerState st = state.get(uuid);
    if (st == null) return;

    zakum.async().execute(() -> PetsStorage.save(zakum, serverId, uuid, st));
  }

  private void flushAllAsync() {
    for (UUID uuid : state.keySet()) flushOneAsync(uuid);
  }
}
