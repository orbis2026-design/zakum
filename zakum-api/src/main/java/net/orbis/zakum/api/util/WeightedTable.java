package net.orbis.zakum.api.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Small weighted selection utility.
 *
 * O(n) build, O(log n) pick.
 * Intended for: crate rewards, loot tables, etc.
 */
public final class WeightedTable<T> {

  private final List<T> items;
  private final double[] cumulative;
  private final double total;

  private WeightedTable(List<T> items, double[] cumulative, double total) {
    this.items = items;
    this.cumulative = cumulative;
    this.total = total;
  }

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  public T pick(Random random) {
    Objects.requireNonNull(random, "random");
    if (items.isEmpty()) throw new IllegalStateException("table is empty");

    double r = random.nextDouble() * total;

    int lo = 0;
    int hi = cumulative.length - 1;
    while (lo < hi) {
      int mid = (lo + hi) >>> 1;
      if (r <= cumulative[mid]) hi = mid;
      else lo = mid + 1;
    }
    return items.get(lo);
  }

  public int size() { return items.size(); }

  public static final class Builder<T> {

    private final List<T> items = new ArrayList<>();
    private final List<Double> weights = new ArrayList<>();

    public Builder<T> add(T item, double weight) {
      Objects.requireNonNull(item, "item");
      if (Double.isNaN(weight) || Double.isInfinite(weight) || weight <= 0.0) return this;

      items.add(item);
      weights.add(weight);
      return this;
    }

    public WeightedTable<T> build() {
      if (items.isEmpty()) throw new IllegalStateException("no items");

      double[] cum = new double[items.size()];
      double sum = 0.0;
      for (int i = 0; i < items.size(); i++) {
        sum += weights.get(i);
        cum[i] = sum;
      }
      return new WeightedTable<>(List.copyOf(items), cum, sum);
    }
  }
}
