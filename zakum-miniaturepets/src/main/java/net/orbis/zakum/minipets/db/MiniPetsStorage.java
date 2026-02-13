package net.orbis.zakum.minipets.db;

import net.orbis.zakum.api.ZakumApi;
import net.orbis.zakum.api.db.DatabaseState;
import net.orbis.zakum.api.util.UuidBytes;
import net.orbis.zakum.minipets.state.MiniPetPlayerState;

import java.util.UUID;

public final class MiniPetsStorage {

  private MiniPetsStorage() {}

  public static MiniPetPlayerState load(ZakumApi zakum, String serverId, UUID uuid) {
    MiniPetPlayerState st = new MiniPetPlayerState();
    if (zakum.database().state() != DatabaseState.ONLINE) return st;

    var rows = zakum.database().jdbc().query(
      "SELECT pet_id, hat, ride FROM zakum_minipets_player WHERE server_id=? AND uuid=?",
      rs -> new Row(rs.getString(1), rs.getBoolean(2), rs.getBoolean(3)),
      serverId,
      UuidBytes.toBytes(uuid)
    );

    if (!rows.isEmpty()) {
      Row r = rows.get(0);
      st.petId = r.petId == null ? "" : r.petId;
      st.hat = r.hat;
      st.ride = r.ride;
    }

    return st;
  }

  public static void save(ZakumApi zakum, String serverId, UUID uuid, MiniPetPlayerState st) {
    if (zakum.database().state() != DatabaseState.ONLINE) return;

    zakum.database().jdbc().update(
      "INSERT INTO zakum_minipets_player (server_id, uuid, pet_id, hat, ride) VALUES (?,?,?,?,?) " +
        "ON DUPLICATE KEY UPDATE pet_id=VALUES(pet_id), hat=VALUES(hat), ride=VALUES(ride)",
      serverId,
      UuidBytes.toBytes(uuid),
      st.petId,
      st.hat,
      st.ride
    );
  }

  private record Row(String petId, boolean hat, boolean ride) {}
}
