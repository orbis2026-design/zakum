package net.orbis.zakum.minipets.command;

import net.orbis.zakum.minipets.runtime.MiniPetsRuntime;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MiniPetsCommand implements CommandExecutor {

  private final MiniPetsRuntime rt;

  public MiniPetsCommand(MiniPetsRuntime rt) {
    this.rt = rt;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player p)) {
      sender.sendMessage("§cPlayers only.");
      return true;
    }

    if (args.length == 0) {
      sender.sendMessage("§b/mpets§7 list | summon <id> | dismiss | hat | ride | info");
      return true;
    }

    String sub = args[0].toLowerCase();

    if (sub.equals("list")) {
      sender.sendMessage("§dPets§7: " + String.join(", ", rt.defs().keySet()));
      return true;
    }

    if (sub.equals("summon")) {
      if (args.length < 2) {
        sender.sendMessage("§cUsage: /mpets summon <id>");
        return true;
      }
      rt.summon(p, args[1]);
      return true;
    }

    if (sub.equals("dismiss")) {
      rt.dismiss(p);
      return true;
    }

    if (sub.equals("hat")) {
      rt.toggleHat(p);
      return true;
    }

    if (sub.equals("ride")) {
      rt.toggleRide(p);
      return true;
    }

    if (sub.equals("info")) {
      var st = rt.get(p.getUniqueId());
      if (st == null) {
        sender.sendMessage("§7Loading...");
        return true;
      }
      sender.sendMessage("§dMiniPet§7: id=" + st.petId + " hat=" + st.hat + " ride=" + st.ride);
      return true;
    }

    sender.sendMessage("§cUnknown subcommand.");
    return true;
  }
}
