package net.orbis.zakum.api.actions;

import java.util.UUID;

/**
 * Normalized player action event emitted by Zakum.
 *
 * Performance posture:
 * - keep it small
 * - prefer a single key/value pair for common "variable" cases
 */
public record ActionEvent(
  String type,
  UUID playerId,
  long amount,
  String key,
  String value
) {
  public ActionEvent {
    if (type == null || type.isBlank()) throw new IllegalArgumentException("type");
    if (playerId == null) throw new IllegalArgumentException("playerId");
    if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
  }
}
