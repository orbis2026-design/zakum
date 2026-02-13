package com.orbis.zakum.crates.data;

import java.util.Map;
import java.util.UUID;

public class KeyService {
    public void giveKeysBatch(Map<UUID, Integer> recipients, String crateId) {
        // Use JDBC Batching here:
        // "INSERT INTO orbis_crates_keys ... ON DUPLICATE KEY UPDATE ..."
    }
}
