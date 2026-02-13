CREATE TABLE IF NOT EXISTS zakum_entitlements (
  uuid BINARY(16) NOT NULL,
  scope ENUM('SERVER','NETWORK') NOT NULL,
  server_id VARCHAR(64) NULL,
  entitlement_key VARCHAR(128) NOT NULL,
  expires_at BIGINT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (uuid, scope, server_id, entitlement_key),
  KEY idx_ent_key (entitlement_key),
  KEY idx_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
