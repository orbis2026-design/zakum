package net.orbis.zakum.crates;

import net.orbis.zakum.crates.model.CrateDef;
import net.orbis.zakum.crates.model.RewardDef;
import net.orbis.zakum.crates.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class CrateService {

  private final Plugin plugin;
  private final Random random = new Random();

  public CrateService(Plugin plugin) {
    this.plugin = plugin;
  }

  public void open(Player opener, CrateDef crate) {
    if (!consumeKey(opener, crate.keyItem())) {
      opener.sendMessage(ItemBuilder.color("&cYou need a key to open this crate."));
      return;
    }

    broadcastOpen(opener, crate);

    // Lightweight "animation": delayed reward.
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      grant(opener, crate);
    }, 40L);
  }

  private void broadcastOpen(Player opener, CrateDef crate) {
    opener.playSound(opener.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);

    if (!crate.publicOpen()) {
      opener.sendTitle(ItemBuilder.color("&bOpening..."), ItemBuilder.color(crate.name()), 0, 30, 10);
      return;
    }

    int r = crate.publicRadius();
    double r2 = r * r;

    for (Player p : opener.getWorld().getPlayers()) {
      if (p.getLocation().distanceSquared(opener.getLocation()) > r2) continue;
      p.sendTitle(ItemBuilder.color("&bOpening..."), ItemBuilder.color(crate.name()), 0, 30, 10);
    }
  }

  private void grant(Player p, CrateDef crate) {
    RewardDef reward = crate.rewards().pick(random);

    for (String msg : reward.messages()) {
      if (!msg.isBlank()) p.sendMessage(ItemBuilder.color(msg));
    }

    for (String cmd : reward.commands()) {
      String c = cmd.replace("{player}", p.getName());
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
    }

    giveItems(p, reward.items());
  }

  private void giveItems(Player p, List<ItemStack> items) {
    for (ItemStack it : items) {
      if (it == null || it.getType().isAir()) continue;
      var leftover = p.getInventory().addItem(it);
      if (!leftover.isEmpty()) {
        for (var e : leftover.values()) {
          p.getWorld().dropItemNaturally(p.getLocation(), e);
        }
      }
    }
  }

  private boolean consumeKey(Player p, ItemStack key) {
    ItemStack k = key.clone();
    k.setAmount(1);

    var inv = p.getInventory();
    for (int i = 0; i < inv.getSize(); i++) {
      ItemStack slot = inv.getItem(i);
      if (slot == null) continue;
      if (!slot.isSimilar(k)) continue;

      int amt = slot.getAmount();
      if (amt <= 1) inv.setItem(i, null);
      else slot.setAmount(amt - 1);
      return true;
    }
    return false;
  }
}
