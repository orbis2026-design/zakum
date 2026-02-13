package net.orbis.zakum.battlepass;

import net.orbis.zakum.api.ZakumApi;
import net.orbis.zakum.api.actions.ActionEvent;
import net.orbis.zakum.api.actions.ActionSubscription;
import net.orbis.zakum.api.boosters.BoosterKind;
import net.orbis.zakum.api.entitlements.EntitlementScope;
import net.orbis.zakum.battlepass.index.QuestIndex;
import net.orbis.zakum.battlepass.model.QuestDef;
import net.orbis.zakum.battlepass.model.QuestStep;
import net.orbis.zakum.battlepass.premium.PremiumResolver;
import net.orbis.zakum.battlepass.state.PlayerBpState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BattlePassRuntime {

  private final Plugin plugin;
  private final ZakumApi zakum;

  private final String serverId;
  private final int season;

  private final Map<String, QuestDef> quests;
  private final QuestIndex index;

  private final PremiumResolver premium;

  private final ConcurrentHashMap<UUID, PlayerBpState> states = new ConcurrentHashMap<>();
  private volatile ActionSubscription sub;

  public BattlePassRuntime(Plugin plugin, ZakumApi zakum) {
    this.plugin = plugin;
    this.zakum = zakum;

    this.serverId = resolveProgressServerId(plugin, zakum);
    this.season = plugin.getConfig().getInt("battlepass.seasons.current", 1);

    this.quests = QuestLoader.loadFromJar(plugin);
    this.index = new QuestIndex(quests.values());

    String premiumScope = plugin.getConfig().getString("battlepass.premiumScope", "SERVER");
    String entKey = plugin.getConfig().getString("battlepass.premiumEntitlementKey", "battlepass_premium");
    this.premium = new PremiumResolver(zakum, premiumScope, entKey, serverId);
  }

  public void start() {
    this.sub = zakum.actions().subscribe(this::onAction);

    for (Player p : Bukkit.getOnlinePlayers()) {
      loadPlayerAsync(p.getUniqueId());
    }
  }

  public void stop() {
    if (sub != null) sub.close();
    sub = null;
    flushAllAsync();
  }

  public void onJoin(UUID uuid) {
    loadPlayerAsync(uuid);
  }

  public void onQuit(UUID uuid) {
    flushPlayerAsync(uuid);
    states.remove(uuid);
  }

  public void refreshPremiumAllAsync() {
    for (UUID uuid : states.keySet()) refreshPremiumAsync(uuid);
  }

  private void refreshPremiumAsync(UUID uuid) {
    PlayerBpState st = states.get(uuid);
    if (st == null) return;

    premium.isPremium(uuid).whenComplete((ok, err) -> {
      if (err != null) return;
      st.premium = Boolean.TRUE.equals(ok);
    });
  }

  private void onAction(ActionEvent e) {
    PlayerBpState st = states.get(e.playerId());
    if (st == null) return;

    // Apply progress booster (server-scope is most common).
    long amt = e.amount();
    double progMult = zakum.boosters().multiplier(e.playerId(), EntitlementScope.SERVER, serverId, BoosterKind.BATTLEPASS_PROGRESS);
    long boosted = (long) Math.max(1, Math.floor(amt * progMult));

    ActionEvent boostedEvent = (boosted == amt) ? e : new ActionEvent(e.type(), e.playerId(), boosted, e.key(), e.value());

    for (QuestDef q : index.candidates(boostedEvent)) {
      if (q.premiumOnly() && !st.premium) continue;
      applyQuest(boostedEvent, q, st);
    }
  }

  private void applyQuest(ActionEvent e, QuestDef q, PlayerBpState st) {
    var ss = st.step(q.id());
    if (ss.stepIdx >= q.steps().size()) return;

    QuestStep step = q.steps().get(ss.stepIdx);

    if (!step.type().equalsIgnoreCase(e.type())) return;
    if (!step.key().isBlank() && !step.key().equalsIgnoreCase(e.key())) return;
    if (!step.value().isBlank() && !step.value().equalsIgnoreCase(e.value())) return;

    long next = ss.progress + e.amount();
    if (next < step.required()) {
      ss.progress = next;
      return;
    }

    ss.stepIdx++;
    ss.progress = 0;

    if (ss.stepIdx >= q.steps().size()) {
      awardPoints(e.playerId(), q, st);
    }
  }

  private void awardPoints(UUID playerId, QuestDef q, PlayerBpState st) {
    long base = q.points();

    if (st.premium && q.premiumBonusPoints() > 0) {
      base += q.premiumBonusPoints();
    }

    double pointsMult = zakum.boosters().multiplier(playerId, EntitlementScope.SERVER, serverId, BoosterKind.BATTLEPASS_POINTS);
    long finalPoints = (long) Math.max(1, Math.floor(base * pointsMult));

    st.points += finalPoints;
  }

  public void flushAllAsync() {
    for (UUID uuid : states.keySet()) flushPlayerAsync(uuid);
  }

  public void flushPlayerAsync(UUID uuid) {
    if (zakum.database().state() != net.orbis.zakum.api.db.DatabaseState.ONLINE) return;

    PlayerBpState st = states.get(uuid);
    if (st == null) return;

    zakum.async().execute(() -> BattlePassStorage.flushPlayer(zakum, serverId, season, uuid, st));
  }

  private void loadPlayerAsync(UUID uuid) {
    if (zakum.database().state() != net.orbis.zakum.api.db.DatabaseState.ONLINE) {
      states.putIfAbsent(uuid, new PlayerBpState());
      refreshPremiumAsync(uuid);
      return;
    }

    zakum.async().execute(() -> {
      PlayerBpState st = BattlePassStorage.loadPlayer(zakum, serverId, season, quests, uuid);
      states.put(uuid, st);
      refreshPremiumAsync(uuid);
    });
  }

  private static String resolveProgressServerId(Plugin plugin, ZakumApi zakum) {
    String override = plugin.getConfig().getString("battlepass.progressServerIdOverride", "").trim();
    return override.isBlank() ? zakum.server().serverId() : override;
  }
}
