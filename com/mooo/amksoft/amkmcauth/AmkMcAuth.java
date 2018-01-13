package com.mooo.amksoft.amkmcauth;

import com.google.common.base.Charsets;
import com.google.common.io.PatternFilenameFilter;
import com.mooo.amksoft.amkmcauth.commands.CmdAmkAuth;
import com.mooo.amksoft.amkmcauth.commands.CmdChangePassword;
import com.mooo.amksoft.amkmcauth.commands.CmdLogin;
import com.mooo.amksoft.amkmcauth.commands.CmdLogout;
import com.mooo.amksoft.amkmcauth.commands.CmdRegister;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class AmkMcAuth
  extends JavaPlugin
{
  public static File dataFolder;
  public Config c;
  public Logger log;
  private BukkitTask reminderTask = null;
  
  private BukkitTask getCurrentReminderTask()
  {
    return this.reminderTask;
  }

  private static AmkMcAuth instance; 
  public AmkMcAuth() {
  	instance = this;
  }
  public static Plugin getInstance() {
	// TODO Auto-generated method stub
	return instance;
  }
	
  private BukkitTask createSaveTimer(Plugin p)
  {
    this.reminderTask = AmkAUtils.createSaveTimer(p);
    return getCurrentReminderTask();
  }
  
  private void registerCommand(CommandExecutor ce, String command, JavaPlugin jp)
  {
    try
    {
      jp.getCommand(command).setExecutor(ce);
    }
    catch (NullPointerException e)
    {
      jp.getLogger().warning(String.format(Language.COULD_NOT_REGISTER_COMMAND.toString(), new Object[] { command, e.getMessage() }));
    }
  }
  
  private void update()
  {
    File userdataFolder = new File(dataFolder, "userdata");
    if ((!userdataFolder.exists()) || (!userdataFolder.isDirectory())) {
      return;
    }
    String[] arrayOfString;
    int j = (arrayOfString = userdataFolder.list(new PatternFilenameFilter("(?i)^.+\\.yml$"))).length;
    for (int i = 0; i < j; i++)
    {
      String fileName = arrayOfString[i];
      String playerName = fileName.substring(0, fileName.length() - 4);
      try
      {
        UUID.fromString(playerName);
      }
      catch (IllegalArgumentException localIllegalArgumentException)
      {
        boolean Online = true;
        UUID u;
        if (Bukkit.getOnlineMode() != Online)
        {
          u = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(Charsets.UTF_8));
        }
        else
        {
          try
          {
            u = AmkAUtils.getUUID(playerName);
          }
          catch (Exception ex)
          {
            u = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(Charsets.UTF_8));
          }
          if (u == null)
          {
            getLogger().warning(Language.ERROR.toString());
            continue;
          }
        }
        File origFile = new File(userdataFolder.toString() + File.separator + fileName);
        File destFile = new File(userdataFolder.toString() + File.separator + u + ".yml");
        if (!origFile.exists()) {
          getLogger().info("Debug: Orig-File " + origFile.toString() + " does NOT exist??");
        }
        if (destFile.exists()) {
          getLogger().info("Debug: Dest-File " + destFile.toString() + " allready exists??");
        }
        if (origFile.renameTo(destFile)) {
          getLogger().info(String.format(Language.CONVERTED_USERDATA.toString(), new Object[] { fileName, u + ".yml" }));
        } else {
          getLogger().warning(String.format(Language.COULD_NOT_CONVERT_USERDATA.toString(), new Object[] { fileName, u + ".yml" }));
        }
      }
    }
  }
  
  private void saveLangFile(String name)
  {
    if (!new File(getDataFolder() + File.separator + "lang" + File.separator + name + ".properties").exists()) {
      saveResource("lang" + File.separator + name + ".properties", false);
    }
  }
  
  public void onDisable()
  {
    getServer().getScheduler().cancelTasks(this);
    for (Player p : getServer().getOnlinePlayers())
    {
      AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
      if (ap.isLoggedIn()) {
        ap.logout(this, false);
      }
    }
    PConfManager.saveAllManagers();
    PConfManager.purge();
  }
  
  public void onEnable()
  {
    dataFolder = getDataFolder();
    if (!new File(getDataFolder(), "config.yml").exists()) {
      saveDefaultConfig();
    }
    this.c = new Config(this);
    this.log = getLogger();
    
    saveLangFile("en_us");
    try
    {
      new Language.LanguageHelper(new File(getDataFolder(), getConfig().getString("general.language_file", "lang/en_us.properties")));
    }
    catch (IOException e)
    {
      this.log.severe("Could not load language file: " + e.getMessage());
      this.log.severe("Disabling plugin.");
      setEnabled(false);
      return;
    }
    if (Config.checkOldUserdata) {
      update();
    }
    PluginManager pm = getServer().getPluginManager();
    pm.registerEvents(new AuthListener(this), this);
    
    registerCommand(new CmdAmkAuth(this), "amkmcauth", this);
    registerCommand(new CmdLogin(this), "login", this);
    registerCommand(new CmdLogout(this), "logout", this);
    registerCommand(new CmdRegister(this), "register", this);
    registerCommand(new CmdChangePassword(), "changepassword", this);
    for (Player p : getServer().getOnlinePlayers())
    {
      AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
      if (!ap.isLoggedIn()) {
        if (ap.isRegistered()) {
          ap.createLoginReminder(this);
        } else {
          ap.createRegisterReminder(this);
        }
      }
    }
    if (createSaveTimer(this) == null) {
      getLogger().info(ChatColor.RED + "AutoSave Task not created!");
    }
    getLogger().info("Counting PlayerBase, IP-Adresses and nlp-players.");
    PConfManager.countPlayersFromIpAndGetVipPlayers();
    getLogger().info("Counting done, PlayerBaseCount: " + String.valueOf(PConfManager.getPlayerCount()) + 
      ", Ip-AddressCount: " + String.valueOf(PConfManager.getIpaddressCount()) + 
      ", nlp-PlayerCount: " + String.valueOf(PConfManager.getVipPlayerCount()) + 
      ".");

	CmdAmkAuth.CheckDevMessage(Bukkit.getConsoleSender());
	
    if (getConfig().getBoolean("general.metrics_enabled"))
    {
		Metrics metrics = new Metrics(this);
        this.getLogger().info(Language.METRICS_ENABLED.toString());
        // Optional: Add custom charts
        metrics.addCustomChart(new Metrics.SimplePie("Registered_PlayerCount") {
        	//@Override
        	public String getValue() {
            	int PlayCnt=(PConfManager.getPlayerCount()/100);
        		return String.valueOf(PlayCnt*100) + "-" + String.valueOf(((PlayCnt+1)*100)-1) ;
        		//return "My value";              
        	}
        });
        metrics.addCustomChart(new Metrics.SimplePie("Appl_Usage") {
        	//@Override
        	public String getValue() {
            	String Usage="se+SaveFileonly+RegPwd";
				return Usage;
        		//return "My value";              
        	}
        });
    }
    else
    {
      getLogger().info("Metrics on plugin disabled.");
    }
    this.log.info(getDescription().getName() + " v" + getDescription().getVersion() + " " + Language.ENABLED + ".");
  }
  
  public boolean isRegistered(String player)
  {
    AuthPlayer ap = AuthPlayer.getAuthPlayer(player);
    if (ap == null)
    {
      getLogger().info(ChatColor.RED + Language.ERROR_OCCURRED.toString());
      return false;
    }
    return ap.isRegistered();
  }
  
  public void forceRegister(String player, String Password)
  {
    AuthPlayer ap = AuthPlayer.getAuthPlayer(player);
    if (ap == null)
    {
      getLogger().info(ChatColor.RED + Language.ERROR_OCCURRED.toString());
      return;
    }
    if (ap.isRegistered())
    {
      getLogger().info(ChatColor.RED + Language.PLAYER_ALREADY_REGISTERED.toString());
      return;
    }
    for (String disallowed : Config.disallowedPasswords) {
      if (Password.equalsIgnoreCase(disallowed))
      {
        getLogger().info(ChatColor.RED + Language.DISALLOWED_PASSWORD.toString());
        return;
      }
    }
    String name = AmkAUtils.forceGetName(ap.getUniqueId());
    if (ap.setPassword(Password, Config.passwordHashType))
    {
      if (name != player) {
        ap.setUserName(player);
      }
      getLogger().info(ChatColor.BLUE + String.format(Language.REGISTERED_SUCCESSFULLY.toString(), new Object[] { ChatColor.GRAY + player + ChatColor.BLUE }));
    }
    else
    {
      getLogger().info(ChatColor.RED + String.format(Language.COULD_NOT_REGISTER.toString(), new Object[] { ChatColor.GRAY + player + ChatColor.RED }));
    }
  }
  
  public void forceLogin(String player)
  {
    AuthPlayer ap = AuthPlayer.getAuthPlayer(player);
    if (ap == null)
    {
      getLogger().info(ChatColor.RED + Language.ERROR_OCCURRED.toString());
      return;
    }
    Player p = ap.getPlayer();
    if (p == null)
    {
      getLogger().info(ChatColor.RED + Language.PLAYER_NOT_ONLINE.toString());
      return;
    }
    ap.login();
    getLogger().info(p.getName() + " " + Language.HAS_LOGGED_IN);
  }
}
