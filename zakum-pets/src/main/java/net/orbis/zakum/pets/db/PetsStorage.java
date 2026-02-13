package net.orbis.zakum.pets.db;

import net.orbis.zakum.api.ZakumApi;
import net.orbis.zakum.api.db.DatabaseState;
import net.orbis.zakum.api.util.UuidBytes;
import net.orbis.zakum.pets.state.PetPlayerState;

import java.util.UUID;

public final class PetsStorage {

  private PetsStorage() {}

  public static PetPlayerState load(ZakumApi zakum, String serverId, UUID uuid) {
    PetPlayerState st = new PetPlayerState();

    if (zakum.database().state() != DatabaseState.ONLINE) return st;

    var rows = zakum.database().jdbc().query(
      "SELECT pet_id, lvl, xp FROM zakum_pets_player WHERE server_id=? AND uuid=?",
      rs -> new Row(rs.getString(1), rs.getInt(2), rs.getLong(3)),
      serverId,
      UuidBytes.toBytes(uuid)
    );

    if (!rows.isEmpty()) {
      Row r = rows.get(0);
      st.selectedPetId = r.petId == null ? "" : r.petId;
      st.level = Math.max(1, r.lvl);
      st.xp = Math.max(0, r.xp);
    }

    return st;
  }

  public static void save(ZakumApi zakum, String serverId, UUID uuid, PetPlayerState st) {
    if (zakum.database().state() != DatabaseState.ONLINE) return;

    zakum.database().jdbc().update(
      "INSERT INTO zakum_pets_player (server_id, uuid, pet_id, lvl, xp) VALUES (?,?,?,?,?) " +
        "ON DUPLICATE KEY UPDATE pet_id=VALUES(pet_id), lvl=VALUES(lvl), xp=VALUES(xp)",
      serverId,
      UuidBytes.toBytes(uuid),
      st.selectedPetId,
      st.level,
      st.xp
    );
  }

  private record Row(String petId, int lvl, long xp) {}
}
