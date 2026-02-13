package net.orbis.zakum.api.util;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class UuidBytes {

  private UuidBytes() {}

  public static byte[] toBytes(UUID uuid) {
    ByteBuffer bb = ByteBuffer.allocate(16);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());
    return bb.array();
  }

  public static UUID fromBytes(byte[] bytes) {
    if (bytes == null || bytes.length != 16) throw new IllegalArgumentException("uuid bytes");
    ByteBuffer bb = ByteBuffer.wrap(bytes);
    long msb = bb.getLong();
    long lsb = bb.getLong();
    return new UUID(msb, lsb);
  }
}
