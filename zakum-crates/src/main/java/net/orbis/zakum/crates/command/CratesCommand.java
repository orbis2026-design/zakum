package net.orbis.zakum.crates.command;

import net.orbis.zakum.crates.CrateLoader;
import net.orbis.zakum.crates.CrateRegistry;
import net.orbis.zakum.crates.CrateService;
import net.orbis.zakum.crates.db.CrateBlockStore;
import net.orbis.zakum.crates.model.CrateDef;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;


public final class CratesCommand implements CommandExecutor {

  private final Plugin plugin;
  private final CrateBlockStore store;
  private final CrateService service;

  private final CrateRegistry registry;

  public CratesCommand(Plugin plugin, CrateRegistry registry, CrateBlockStore store, CrateService service) {
    this.plugin = plugin;
    this.registry = registry;
    this.store = store;
    this.service = service;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) {
      sender.sendMessage("§b/zcrates§7 open <crateId> | setblock <crateId> | givekey <player> <crateId> [amt] | reload");
      return true;
    }

    String sub = args[0].toLowerCase();

    if (sub.equals("reload")) {
      if (!sender.hasPermission("zakum.crates.admin")) {
        sender.sendMessage("§cNo permission.");
        return true;
      }

      plugin.reloadConfig();
      registry.set(CrateLoader.load(plugin));
      store.loadAll();

      sender.sendMessage("§aReloaded crates. count=" + registry.size());
      return true;
    }

    if (sub.equals("open")) {
      if (!(sender instanceof Player p)) {
        sender.sendMessage("§cPlayers only.");
        return true;
      }
      if (args.length < 2) {
        sender.sendMessage("§cUsage: /zcrates open <crateId>");
        return true;
      }

      CrateDef def = registry.get(args[1]);
      if (def == null) {
        p.sendMessage("§cUnknown crate.");
        return true;
      }

      service.open(p, def);
      return true;
    }

    if (sub.equals("setblock")) {
      if (!(sender instanceof Player p)) {
        sender.sendMessage("§cPlayers only.");
        return true;
      }
      if (!sender.hasPermission("zakum.crates.admin")) {
        sender.sendMessage("§cNo permission.");
        return true;
      }
      if (args.length < 2) {
        p.sendMessage("§cUsage: /zcrates setblock <crateId>");
        return true;
      }

      String crateId = args[1];
      CrateDef def = registry.get(crateId);
      if (def == null) {
        p.sendMessage("§cUnknown crate.");
        return true;
      }

      var b = p.getTargetBlockExact(5);
      if (b == null) {
        p.sendMessage("§cLook at a block within 5 blocks.");
        return true;
      }

      store.set(b, crateId);
      p.sendMessage("§aSet crate block for §f" + crateId);
      return true;
    }

    if (sub.equals("givekey")) {
      if (!sender.hasPermission("zakum.crates.admin")) {
        sender.sendMessage("§cNo permission.");
        return true;
      }
      if (args.length < 3) {
        sender.sendMessage("§cUsage: /zcrates givekey <player> <crateId> [amt]");
        return true;
      }

      Player target = Bukkit.getPlayerExact(args[1]);
      if (target == null) {
        sender.sendMessage("§cPlayer not online.");
        return true;
      }

      CrateDef def = registry.get(args[2]);
      if (def == null) {
        sender.sendMessage("§cUnknown crate.");
        return true;
      }

      int amt = 1;
      if (args.length >= 4) {
        try { amt = Math.max(1, Integer.parseInt(args[3])); } catch (Exception ignored) {}
      }

      var key = def.keyItem().clone();
      key.setAmount(amt);

      var leftover = target.getInventory().addItem(key);
      if (!leftover.isEmpty()) {
        for (var it : leftover.values()) target.getWorld().dropItemNaturally(target.getLocation(), it);
      }

      sender.sendMessage("§aGave " + amt + " key(s) for " + def.id() + " to " + target.getName());
      return true;
    }

    sender.sendMessage("§cUnknown subcommand.");
    return true;
  }
}
