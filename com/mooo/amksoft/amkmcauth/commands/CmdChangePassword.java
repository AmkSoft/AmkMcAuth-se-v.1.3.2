package com.mooo.amksoft.amkmcauth.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mooo.amksoft.amkmcauth.AuthPlayer;
import com.mooo.amksoft.amkmcauth.Config;
import com.mooo.amksoft.amkmcauth.Hasher;
import com.mooo.amksoft.amkmcauth.Language;
import com.mooo.amksoft.amkmcauth.AmkAUtils;

import java.security.NoSuchAlgorithmException;

public class CmdChangePassword implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("changepassword")) {
            if (!cs.hasPermission("amkauth.changepassword")) {
                AmkAUtils.dispNoPerms(cs);
                return true;
            }
            if (!(cs instanceof Player)) {
                cs.sendMessage(ChatColor.RED + Language.COMMAND_NO_CONSOLE.toString());
                return true;
            }
            Player p = (Player) cs;
            AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
            if (!ap.isLoggedIn()) {
                cs.sendMessage(ChatColor.RED + Language.YOU_MUST_LOGIN.toString());
                return true;
            }
            if (args.length < 2) {
                cs.sendMessage(cmd.getDescription());
                return false;
            }
            String oldPassword;
            String newPassword = args[1];
            for (String disallowed : Config.disallowedPasswords) {
                if (!newPassword.equalsIgnoreCase(disallowed)) continue;
                cs.sendMessage(ChatColor.RED + Language.DISALLOWED_PASSWORD.toString());
                return true;
            }
            try {
                oldPassword = Hasher.encrypt(args[0], ap.getHashType());
                newPassword = Hasher.encrypt(newPassword, Config.passwordHashType);
            } catch (NoSuchAlgorithmException e) {
                cs.sendMessage(ChatColor.RED + Language.ADMIN_SET_UP_INCORRECTLY.toString());
                cs.sendMessage(ChatColor.RED + Language.YOUR_PASSWORD_COULD_NOT_BE_CHANGED.toString());
                return true;
            }
            if (!ap.getPasswordHash().equals(oldPassword)) {
                cs.sendMessage(ChatColor.RED + Language.OLD_PASSWORD_INCORRECT.toString());
                return true;
            }
            ap.setHashedPassword(newPassword, Config.passwordHashType);
            cs.sendMessage(ChatColor.BLUE + Language.YOUR_PASSWORD_CHANGED.toString());
            return true;
        }
        return false;
    }

}
