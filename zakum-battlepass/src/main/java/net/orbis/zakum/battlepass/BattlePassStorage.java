package net.orbis.zakum.battlepass;

import net.orbis.zakum.api.ZakumApi;
import net.orbis.zakum.api.db.Jdbc;
import net.orbis.zakum.battlepass.model.QuestDef;
import net.orbis.zakum.battlepass.state.PlayerBpState;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

public final class BattlePassStorage {

  private BattlePassStorage() {}

  public static PlayerBpState loadPlayer(ZakumApi zakum, String serverId, int season, Map<String, QuestDef> quests, UUID uuid) {
    Jdbc jdbc = zakum.database().jdbc();
    PlayerBpState st = new PlayerBpState();

    var rows = jdbc.query(
      "SELECT tier, points FROM battlepass_progress WHERE server_id=? AND season=? AND uuid=? LIMIT 1",
      rs -> new Row(rs.getInt(1), rs.getLong(2)),
      serverId, season, uuidBytes(uuid)
    );
    if (!rows.isEmpty()) {
      st.tier = rows.get(0).tier;
      st.points = rows.get(0).points;
    }

    var steps = jdbc.query(
      "SELECT quest_id, step_idx, progress FROM battlepass_step_progress WHERE server_id=? AND season=? AND uuid=?",
      rs -> new StepRow(rs.getString(1), rs.getInt(2), rs.getLong(3)),
      serverId, season, uuidBytes(uuid)
    );

    for (var r : steps) {
      var ss = st.step(r.questId);
      ss.stepIdx = r.stepIdx;
      ss.progress = r.progress;
    }

    for (String qid : quests.keySet()) {
      st.step(qid);
    }

    return st;
  }

  public static void flushPlayer(ZakumApi zakum, String serverId, int season, UUID uuid, PlayerBpState st) {
    Jdbc jdbc = zakum.database().jdbc();

    jdbc.update(
      "INSERT INTO battlepass_progress (server_id, season, uuid, tier, points) VALUES (?,?,?,?,?) " +
        "ON DUPLICATE KEY UPDATE tier=VALUES(tier), points=VALUES(points)",
      serverId, season, uuidBytes(uuid), st.tier, st.points
    );

    for (var e : st.all().entrySet()) {
      String questId = e.getKey();
      var ss = e.getValue();

      jdbc.update(
        "INSERT INTO battlepass_step_progress (server_id, season, uuid, quest_id, step_idx, progress) VALUES (?,?,?,?,?,?) " +
          "ON DUPLICATE KEY UPDATE step_idx=VALUES(step_idx), progress=VALUES(progress)",
        serverId, season, uuidBytes(uuid), questId, ss.stepIdx, ss.progress
      );
    }
  }

  private static byte[] uuidBytes(UUID uuid) {
    ByteBuffer bb = ByteBuffer.allocate(16);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());
    return bb.array();
  }

  private record Row(int tier, long points) {}
  private record StepRow(String questId, int stepIdx, long progress) {}
}
