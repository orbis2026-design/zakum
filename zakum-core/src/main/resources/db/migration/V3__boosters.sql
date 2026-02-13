CREATE TABLE IF NOT EXISTS zakum_boosters (
  id BIGINT NOT NULL AUTO_INCREMENT,

  scope ENUM('SERVER','NETWORK') NOT NULL,
  server_id VARCHAR(64) NULL,

  target ENUM('ALL','PLAYER') NOT NULL,
  uuid BINARY(16) NULL,

  kind VARCHAR(64) NOT NULL,
  multiplier DOUBLE NOT NULL,
  expires_at BIGINT NOT NULL,

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  KEY idx_expires (expires_at),
  KEY idx_kind_scope (kind, scope, server_id),
  KEY idx_uuid (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
