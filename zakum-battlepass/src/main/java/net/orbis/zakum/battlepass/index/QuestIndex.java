package net.orbis.zakum.battlepass.index;

import net.orbis.zakum.api.actions.ActionEvent;
import net.orbis.zakum.battlepass.model.QuestDef;
import net.orbis.zakum.battlepass.model.QuestStep;

import java.util.*;

/**
 * Global quest index for faster lookup.
 *
 * Keying rules:
 * - step.key blank => indexed as (type,"","")
 * - step.key set, step.value blank => (type,key,"")
 * - both set => (type,key,value)
 *
 * On event:
 * - probe (type,event.key,event.value)
 * - probe (type,event.key,"")
 * - probe (type,"","")
 */
public final class QuestIndex {

  private final Map<Key, List<QuestDef>> index = new HashMap<>();
  private final List<QuestDef> all;

  public QuestIndex(Collection<QuestDef> quests) {
    this.all = List.copyOf(quests);
    for (QuestDef q : quests) {
      for (QuestStep step : q.steps()) {
        Key k = Key.from(step.type(), step.key(), step.value());
        index.computeIfAbsent(k, __ -> new ArrayList<>()).add(q);
      }
    }
  }

  public List<QuestDef> candidates(ActionEvent e) {
    Key k1 = Key.from(e.type(), e.key(), e.value());
    Key k2 = Key.from(e.type(), e.key(), "");
    Key k3 = Key.from(e.type(), "", "");

    // common fast-path: exact match only
    var a = index.get(k1);
    var b = index.get(k2);
    var c = index.get(k3);

    if (a == null && b == null && c == null) return List.of();
    if (b == null && c == null) return a;
    if (a == null && c == null) return b;
    if (a == null && b == null) return c;

    // rare path: merge/dedup (quests can appear in multiple lists due to multiple steps)
    HashSet<QuestDef> set = new HashSet<>();
    if (a != null) set.addAll(a);
    if (b != null) set.addAll(b);
    if (c != null) set.addAll(c);
    return new ArrayList<>(set);
  }

  public List<QuestDef> all() {
    return all;
  }

  public record Key(String type, String key, String value) {

    public static Key from(String type, String key, String value) {
      String t = norm(type);
      String k = norm(key);
      String v = norm(value);
      return new Key(t, k, v);
    }

    private static String norm(String s) {
      if (s == null) return "";
      return s.trim().toUpperCase(Locale.ROOT);
    }
  }
}
