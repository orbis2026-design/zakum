package net.orbis.zakum.pets;

public final class LevelCurve {

  private final int maxLevel;
  private final long base;
  private final double growth;

  public LevelCurve(int maxLevel, long base, double growth) {
    this.maxLevel = Math.max(1, maxLevel);
    this.base = Math.max(1, base);
    this.growth = Math.max(1.0, growth);
  }

  public int maxLevel() { return maxLevel; }

  public long xpRequiredForNext(int currentLevel) {
    if (currentLevel >= maxLevel) return Long.MAX_VALUE;
    double v = base * Math.pow(growth, Math.max(0, currentLevel - 1));
    long out = (long) Math.ceil(v);
    return Math.max(1, out);
  }
}
