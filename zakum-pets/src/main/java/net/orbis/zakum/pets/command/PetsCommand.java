package net.orbis.zakum.pets.command;

import net.orbis.zakum.pets.runtime.PetsRuntime;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PetsCommand implements CommandExecutor {

  private final PetsRuntime rt;

  public PetsCommand(PetsRuntime rt) {
    this.rt = rt;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player p)) {
      sender.sendMessage("§cPlayers only.");
      return true;
    }

    if (args.length == 0) {
      sender.sendMessage("§b/pets§7 list | summon <id> | dismiss | info");
      return true;
    }

    String sub = args[0].toLowerCase();

    if (sub.equals("list")) {
      sender.sendMessage("§dPets§7: " + String.join(", ", rt.defs().keySet()));
      return true;
    }

    if (sub.equals("summon")) {
      if (args.length < 2) {
        sender.sendMessage("§cUsage: /pets summon <id>");
        return true;
      }
      rt.summon(p, args[1]);
      return true;
    }

    if (sub.equals("dismiss")) {
      rt.dismiss(p);
      return true;
    }

    if (sub.equals("info")) {
      var st = rt.get(p.getUniqueId());
      if (st == null) {
        sender.sendMessage("§7Loading...");
        return true;
      }
      sender.sendMessage("§dPet§7: id=" + st.selectedPetId + " lvl=" + st.level + " xp=" + st.xp);
      return true;
    }

    sender.sendMessage("§cUnknown subcommand.");
    return true;
  }
}
