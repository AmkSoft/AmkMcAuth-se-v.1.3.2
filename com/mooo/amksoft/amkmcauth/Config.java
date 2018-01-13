package com.mooo.amksoft.amkmcauth;

import java.io.File;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;

public class Config
{
  public static boolean disableIfOnlineMode;
  public static boolean requireLogin;
  public static boolean kickIfAlreadyOnline;
  public static boolean KickOnPasswordFail;
  public static boolean useLoginPermission;
  public static String loginPermission;
  public static boolean allowChat;
  public static String chatPrefix;
  public static boolean allowCommands;
  public static List<String> allowedCommands;
  public static boolean allowMovementWalk;
  public static boolean allowMovementLook;
  public static long allowMovementTime;
  public static boolean godMode;
  public static boolean godModeAfterLogin;
  public static long godModeLength;
  public static long removeAfterDays;
  public static boolean remindEnabled;
  public static long remindInterval;
  public static long saveUserdataInterval;
  public static boolean sessionsEnabled;
  public static boolean sessionsCheckIP;
  public static long sessionLength;
  public static String sessionType;
  public static List<String> disallowedPasswords;
  public static String passwordHashType;
  public static boolean validateUsernames;
  public static String usernameRegex;
  public static boolean adventureMode;
  public static boolean teleportToSpawn;
  public static boolean kickPlayers;
  public static long kickAfter;
  public static boolean checkOldUserdata;
  public static long maxUsersPerIpaddress;
  private final AmkMcAuth plugin;
  
  public Config(AmkMcAuth instance)
  {
    this.plugin = instance;
    File config = new File(this.plugin.getDataFolder(), "config.yml");
    if (!config.exists())
    {
      if (!config.getParentFile().mkdirs()) {
        this.plugin.getLogger().warning("Could not create config.yml directory.");
      }
      this.plugin.saveDefaultConfig();
    }
    reloadConfiguration();
  }
  
  public void reloadConfiguration()
  {
    this.plugin.reloadConfig();
    FileConfiguration c = this.plugin.getConfig();
    
    disableIfOnlineMode = c.getBoolean("login.disable_if_online_mode");
    requireLogin = c.getBoolean("login.require");
    kickIfAlreadyOnline = c.getBoolean("login.kick_if_already_online");
    if (c.isSet("login.kick_on_password_fail"))
    {
      KickOnPasswordFail = c.getBoolean("login.kick_on_password_fail");
    }
    else
    {
      c.set("login.kick_on_password_fail", Boolean.valueOf(false));
      this.plugin.saveConfig();
      KickOnPasswordFail = false;
    }
    useLoginPermission = c.getBoolean("login.permission.enabled");
    loginPermission = c.getString("login.permission.permission");
    
    allowChat = c.getBoolean("login.restrictions.chat.allowed");
    chatPrefix = c.getString("login.restrictions.chat.prefix");
    
    allowCommands = c.getBoolean("login.restrictions.commands.allowed");
    allowedCommands = c.getStringList("login.restrictions.commands.exempt");
    
    allowMovementWalk = c.getBoolean("login.restrictions.movement.walk");
    allowMovementLook = c.getBoolean("login.restrictions.movement.look_around");
    allowMovementTime = c.getLong("login.restrictions.movement.allowmovetime");
    
    godMode = c.getBoolean("login.godmode.enabled");
    godModeAfterLogin = c.getBoolean("login.godmode.after_login.enabled");
    godModeLength = c.getLong("login.godmode.after_login.length");
    
    remindEnabled = c.getBoolean("login.remind.enabled");
    remindInterval = c.getLong("login.remind.interval");
    
    saveUserdataInterval = c.getLong("saving.interval");
    
    sessionsEnabled = c.getBoolean("sessions.enabled");
    sessionsCheckIP = c.getBoolean("sessions.check_ip");
    sessionLength = c.getLong("sessions.length");
    sessionType = c.getString("sessions.LoginCommandsMessage");
    
    disallowedPasswords = c.getStringList("passwords.disallowed");
    passwordHashType = c.getString("passwords.hash_type");
    
    validateUsernames = c.getBoolean("usernames.verify");
    usernameRegex = c.getString("usernames.regex");
    
    adventureMode = c.getBoolean("login.adventure_mode");
    teleportToSpawn = c.getBoolean("login.teleport_to_spawn");
    
    kickPlayers = c.getBoolean("login.remind.kick.enabled");
    kickAfter = c.getLong("login.remind.kick.wait");
    
    checkOldUserdata = c.getBoolean("saving.check_old_userdata");
    
    maxUsersPerIpaddress = c.getLong("general.users_per_ipaddress");

    if(c.isSet("saving.remove_inactive_after")) {        	
        removeAfterDays = c.getInt("saving.remove_inactive_after");
	} else {
        c.set("saving.remove_inactive_after",99);
        this.plugin.saveConfig(); // Save new Configuration
        removeAfterDays=99L;
    }

    //-- Check for invalid inputs and set to default if invalid --//

    if (remindInterval < 1L)        remindInterval = 30L;
    if (saveUserdataInterval < 1L)  saveUserdataInterval = 5L;
    if (sessionLength < 1L)         sessionLength = 15L;
    if (kickAfter < 0L)             kickAfter = 0L;
    if (godModeLength <= 0L)        godModeLength = 10L;
    if (maxUsersPerIpaddress < 0L)  maxUsersPerIpaddress = 50L;

  }
}
