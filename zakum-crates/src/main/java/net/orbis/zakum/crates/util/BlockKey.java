package net.orbis.zakum.crates.util;

/**
 * Packs XYZ into a long (26 X, 12 Y, 26 Z).
 */
public final class BlockKey {

  private BlockKey() {}

  public static long pack(int x, int y, int z) {
    return ((long)(x & 0x3FFFFFF) << 38) | ((long)(z & 0x3FFFFFF) << 12) | (long)(y & 0xFFF);
  }
}
