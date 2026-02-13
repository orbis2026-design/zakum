package net.orbis.zakum.core.util;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * UUID <-> BINARY(16) helpers.
 */
public final class UuidBytes {

  private UuidBytes() {}

  public static byte[] toBytes(UUID uuid) {
    ByteBuffer bb = ByteBuffer.allocate(16);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());
    return bb.array();
  }

  public static UUID fromBytes(byte[] bytes) {
    if (bytes == null || bytes.length != 16) throw new IllegalArgumentException("Expected 16 bytes");
    ByteBuffer bb = ByteBuffer.wrap(bytes);
    return new UUID(bb.getLong(), bb.getLong());
  }
}
