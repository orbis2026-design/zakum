package net.orbis.zakum.crates;

import net.orbis.zakum.crates.model.CrateDef;

import java.util.Map;

/**
 * Volatile registry to support /reload without re-registering listeners.
 */
public final class CrateRegistry {

  private volatile Map<String, CrateDef> crates;

  public CrateRegistry(Map<String, CrateDef> crates) {
    this.crates = crates;
  }

  public CrateDef get(String id) {
    return crates.get(id);
  }

  public int size() { return crates.size(); }

  public Map<String, CrateDef> snapshot() { return crates; }

  public void set(Map<String, CrateDef> crates) {
    this.crates = crates;
  }
}
