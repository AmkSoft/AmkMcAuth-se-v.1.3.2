package com.mooo.amksoft.amkmcauth.commands;

import com.mooo.amksoft.amkmcauth.AmkAUtils;
import com.mooo.amksoft.amkmcauth.AmkMcAuth;
import com.mooo.amksoft.amkmcauth.AuthPlayer;
import com.mooo.amksoft.amkmcauth.Config;
import com.mooo.amksoft.amkmcauth.Language;
import com.mooo.amksoft.amkmcauth.PConfManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class CmdRegister
  implements CommandExecutor
{
  private final AmkMcAuth plugin;
  
  public CmdRegister(AmkMcAuth instance)
  {
    this.plugin = instance;
  }
  
  public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args)
  {
    if (cmd.getName().equalsIgnoreCase("register"))
    {
      if (!cs.hasPermission("amkauth.register"))
      {
        AmkAUtils.dispNoPerms(cs);
        return true;
      }
      if (args.length < 1)
      {
        cs.sendMessage(cmd.getDescription());
        return false;
      }
      if (!(cs instanceof Player))
      {
        cs.sendMessage(ChatColor.RED + Language.COMMAND_NO_CONSOLE.toString());
        return true;
      }
      Player p = (Player)cs;
      AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
      if ((ap.isLoggedIn()) || (ap.isRegistered()))
      {
        cs.sendMessage(ChatColor.RED + Language.ALREADY_REGISTERED.toString());
        return true;
      }
      int PlayerCount = PConfManager.countPlayersFromIp(ap.getCurrentIPAddress());
      if (Config.maxUsersPerIpaddress > 0L)
      {
        this.plugin.getLogger().info("Login Ip-Address " + ap.getCurrentIPAddress() + " used by " + PlayerCount + " player(s) ");
        this.plugin.getLogger().info("Configured maximum allowed players from one Ip-Address is: " + Config.maxUsersPerIpaddress);
      }
      else
      {
        this.plugin.getLogger().info("Maximum allowed registered player counting from one Ip-Address is disabled.");
      }
      if ((PlayerCount >= Config.maxUsersPerIpaddress) && (Config.maxUsersPerIpaddress > 0L))
      {
        cs.sendMessage(ChatColor.RED + Language.PLAYER_EXCEEDS_MAXREGS.toString());
        return true;
      }
      String rawPassword = args[0];
      for (String disallowed : Config.disallowedPasswords) {
        if (rawPassword.equalsIgnoreCase(disallowed))
        {
          cs.sendMessage(ChatColor.RED + Language.DISALLOWED_PASSWORD.toString());
          return true;
        }
      }
      if (ap.setPassword(rawPassword, Config.passwordHashType))
      {
        this.plugin.getLogger().info(p.getName() + " !!!! " + Language.HAS_REGISTERED);
        cs.sendMessage(ChatColor.BLUE + Language.PASSWORD_SET_AND_REGISTERED.toString());
        
        ap.setUserName(p.getName());
        PConfManager.addAllPlayer(p.getName());
        PConfManager.addPlayerToIp(ap.getCurrentIPAddress());
        
        BukkitTask reminder = ap.getCurrentReminderTask();
        if (reminder != null) {
          reminder.cancel();
        }
        ap.createLoginReminder(this.plugin);
      }
      else
      {
        cs.sendMessage(ChatColor.RED + Language.PASSWORD_COULD_NOT_BE_SET.toString());
      }
      return true;
    }
    return false;
  }
}
