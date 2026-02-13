package net.orbis.zakum.crates.db;

import net.orbis.zakum.api.db.Jdbc;

public final class CratesSchema {

  private CratesSchema() {}

  public static void ensure(Jdbc jdbc) {
    jdbc.update(
      "CREATE TABLE IF NOT EXISTS zakum_crate_blocks (" +
        "server_id VARCHAR(64) NOT NULL," +
        "world_uuid BINARY(16) NOT NULL," +
        "x INT NOT NULL," +
        "y INT NOT NULL," +
        "z INT NOT NULL," +
        "crate_id VARCHAR(64) NOT NULL," +
        "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
        "PRIMARY KEY (server_id, world_uuid, x, y, z)," +
        "KEY idx_crate (server_id, crate_id)" +
      ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
    );
  }
}
