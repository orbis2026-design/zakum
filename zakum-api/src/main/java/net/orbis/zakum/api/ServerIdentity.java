package net.orbis.zakum.api;

/**
 * Single-region, single-identifier model.
 * Everything data-related should be scoped by serverId.
 */
public record ServerIdentity(String serverId) {

  public ServerIdentity {
    if (serverId == null || serverId.isBlank()) {
      throw new IllegalArgumentException("serverId must not be blank");
    }
  }
}
