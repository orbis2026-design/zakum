package net.orbis.zakum.pets.db;

import net.orbis.zakum.api.db.Jdbc;

public final class PetsSchema {

  private PetsSchema() {}

  public static void ensure(Jdbc jdbc) {
    jdbc.update(
      "CREATE TABLE IF NOT EXISTS zakum_pets_player (" +
        "server_id VARCHAR(64) NOT NULL," +
        "uuid BINARY(16) NOT NULL," +
        "pet_id VARCHAR(64) NOT NULL," +
        "lvl INT NOT NULL," +
        "xp BIGINT NOT NULL," +
        "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
        "PRIMARY KEY (server_id, uuid)" +
      ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
    );
  }
}
