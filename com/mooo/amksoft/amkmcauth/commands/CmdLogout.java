package com.mooo.amksoft.amkmcauth.commands;

import com.mooo.amksoft.amkmcauth.AmkAUtils;
import com.mooo.amksoft.amkmcauth.AmkMcAuth;
import com.mooo.amksoft.amkmcauth.AuthPlayer;
import com.mooo.amksoft.amkmcauth.Language;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdLogout
  implements CommandExecutor
{
  private final AmkMcAuth plugin;
  
  public CmdLogout(AmkMcAuth instance)
  {
    this.plugin = instance;
  }
  
  public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args)
  {
    if (cmd.getName().equalsIgnoreCase("logout"))
    {
      if (!cs.hasPermission("amkauth.logout"))
      {
        AmkAUtils.dispNoPerms(cs);
        return true;
      }
      if (!(cs instanceof Player))
      {
        cs.sendMessage(ChatColor.RED + Language.COMMAND_NO_CONSOLE.toString());
        return true;
      }
      Player p = (Player)cs;
      AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
      if (!ap.isLoggedIn())
      {
        cs.sendMessage(ChatColor.RED + Language.NOT_LOGGED_IN.toString());
        return true;
      }
      cs.sendMessage(ChatColor.BLUE + Language.LOGGED_OUT.toString());
      ap.setLastQuitTimestamp(System.currentTimeMillis());
      ap.setLastJoinTimestamp(System.currentTimeMillis());
      ap.logout(this.plugin);
      return true;
    }
    return false;
  }
}
