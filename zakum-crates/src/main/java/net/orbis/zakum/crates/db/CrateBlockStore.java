package net.orbis.zakum.crates.db;

import net.orbis.zakum.api.ZakumApi;
import net.orbis.zakum.api.db.DatabaseState;
import net.orbis.zakum.api.util.UuidBytes;
import net.orbis.zakum.crates.util.BlockKey;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory lookup map with DB persistence when available.
 */
public final class CrateBlockStore {

  private final ZakumApi zakum;
  private final String serverId;

  // worldUuid -> (packedXYZ -> crateId)
  private final Map<UUID, Map<Long, String>> blocks = new HashMap<>();

  public CrateBlockStore(ZakumApi zakum) {
    this.zakum = zakum;
    this.serverId = zakum.server().serverId();
  }

  public synchronized void loadAll() {
    blocks.clear();

    if (zakum.database().state() != DatabaseState.ONLINE) return;

    var rows = zakum.database().jdbc().query(
      "SELECT world_uuid, x, y, z, crate_id FROM zakum_crate_blocks WHERE server_id=?",
      rs -> new Row(rs.getBytes(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getString(5)),
      serverId
    );

    for (Row r : rows) {
      UUID world = UuidBytes.fromBytes(r.worldUuid);
      blocks.computeIfAbsent(world, __ -> new HashMap<>())
        .put(BlockKey.pack(r.x, r.y, r.z), r.crateId);
    }
  }

  public synchronized String get(World world, int x, int y, int z) {
    Map<Long, String> m = blocks.get(world.getUID());
    if (m == null) return null;
    return m.get(BlockKey.pack(x, y, z));
  }

  public void set(Block block, String crateId) {
    UUID world = block.getWorld().getUID();
    long k = BlockKey.pack(block.getX(), block.getY(), block.getZ());

    synchronized (this) {
      blocks.computeIfAbsent(world, __ -> new HashMap<>()).put(k, crateId);
    }

    if (zakum.database().state() != DatabaseState.ONLINE) return;

    zakum.async().execute(() -> zakum.database().jdbc().update(
      "INSERT INTO zakum_crate_blocks (server_id, world_uuid, x, y, z, crate_id) VALUES (?,?,?,?,?,?) " +
        "ON DUPLICATE KEY UPDATE crate_id=VALUES(crate_id)",
      serverId,
      UuidBytes.toBytes(world),
      block.getX(), block.getY(), block.getZ(),
      crateId
    ));
  }

  private record Row(byte[] worldUuid, int x, int y, int z, String crateId) {}
}
