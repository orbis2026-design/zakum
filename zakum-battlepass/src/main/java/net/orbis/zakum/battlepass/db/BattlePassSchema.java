package net.orbis.zakum.battlepass.db;

import net.orbis.zakum.api.db.Jdbc;

/**
 * v1 bootstrap: create BattlePass tables if they do not exist.
 *
 * Migration strategy:
 * - v1 uses CREATE IF NOT EXISTS so you can ship without manual DB steps.
 * - Later: move to Flyway with strict versioned migrations.
 */
public final class BattlePassSchema {

  private BattlePassSchema() {}

  public static void ensureTables(Jdbc jdbc) {
    jdbc.update("""
      CREATE TABLE IF NOT EXISTS battlepass_progress (
        server_id VARCHAR(64) NOT NULL,
        season INT NOT NULL,
        uuid BINARY(16) NOT NULL,
        tier INT NOT NULL DEFAULT 0,
        points BIGINT NOT NULL DEFAULT 0,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        PRIMARY KEY (server_id, season, uuid),
        KEY idx_season_points (server_id, season, points)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
      """);

    jdbc.update("""
      CREATE TABLE IF NOT EXISTS battlepass_step_progress (
        server_id VARCHAR(64) NOT NULL,
        season INT NOT NULL,
        uuid BINARY(16) NOT NULL,
        quest_id VARCHAR(64) NOT NULL,
        step_idx INT NOT NULL DEFAULT 0,
        progress BIGINT NOT NULL DEFAULT 0,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        PRIMARY KEY (server_id, season, uuid, quest_id),
        KEY idx_quest (server_id, season, quest_id)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
      """);

    jdbc.update("""
      CREATE TABLE IF NOT EXISTS battlepass_entitlements (
        -- If premiumScope=GLOBAL, use server_id='GLOBAL'
        server_id VARCHAR(64) NOT NULL,
        uuid BINARY(16) NOT NULL,
        premium BOOLEAN NOT NULL DEFAULT FALSE,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        PRIMARY KEY (server_id, uuid)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
      """);
  }
}
