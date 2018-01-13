package com.mooo.amksoft.amkmcauth;

import com.google.common.base.Charsets;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class AuthPlayer
{
  private final static Map<UUID, AuthPlayer> authPlayers = new HashMap<>();
 
  private final PConfManager pcm;
  private UUID playerUUID;
  private String lastIPAddress;
  private long lastJoinTimestamp = 0L;
  private long lastWalkTimestamp = 0L;
  private long lastLoginTimestamp = 0L;
  private long lastQuitTimestamp = 0L;
  private Location lastJoinLocation;
  private BukkitTask reminderTask = null;
  
  private AuthPlayer(UUID u)
  {
    this.playerUUID = u;
    this.pcm = PConfManager.getPConfManager(u);
    this.lastJoinTimestamp = this.pcm.getLong("timestamps.join", 0L);
    this.lastLoginTimestamp = this.pcm.getLong("timestamps.login", 0L);
    this.lastQuitTimestamp = this.pcm.getLong("timestamps.quit", 0L);
  }
  
  private AuthPlayer(Player p)
  {
    this(p.getUniqueId());
  }
  
  public static AuthPlayer getAuthPlayer(UUID u)
  {
    synchronized (authPlayers)
    {
      if (authPlayers.containsKey(u)) {
        return (AuthPlayer)authPlayers.get(u);
      }
      AuthPlayer ap = new AuthPlayer(u);
      authPlayers.put(u, ap);
      return ap;
    }
  }
  
  public static AuthPlayer getAuthPlayer(String s)
  {
    boolean Online = true;
    UUID u;
    if (Bukkit.getOnlineMode() != Online) {
      u = UUID.nameUUIDFromBytes(("OfflinePlayer:" + s).getBytes(Charsets.UTF_8));
    } else {
      try
      {
        u = AmkAUtils.getUUID(s);
      }
      catch (Exception ex)
      {
        return null;
      }
    }
    return getAuthPlayer(u);
  }
  
  public static AuthPlayer getAuthPlayer(Player p)
  {
    return getAuthPlayer(p.getUniqueId());
  }
  
  public boolean isRegistered()
  {
    return this.pcm.isSet("login.password");
  }
  
  public boolean isVIP()
  {
    return this.pcm.getBoolean("login.vip");
  }
  
  public boolean isLoggedIn()
  {
    return this.pcm.getBoolean("login.logged_in");
  }
  
  public void setLoggedIn(boolean loggedIn)
  {
    this.pcm.set("login.logged_in", Boolean.valueOf(loggedIn));
  }
  
  public void setVIP(boolean VIP)
  {
    this.pcm.set("login.vip", Boolean.valueOf(VIP));
    if (VIP) {
      PConfManager.addVipPlayer(getUserName());
    } else {
      PConfManager.removeVipPlayer(getUserName());
    }
  }
  
  public void setUserName(String UserName)
  {
    this.pcm.set("login.username", UserName);
  }
  
  public boolean isInSession()
  {
    if (!isLoggedIn()) {
      return false;
    }
    if ((this.lastLoginTimestamp <= 0L) || (this.lastQuitTimestamp <= 0L)) {
      return false;
    }
    if (!getCurrentIPAddress().equals(this.lastIPAddress)) {
      return false;
    }
    return true;
  }
  
  public boolean isWithinSession()
  {
    if (!Config.sessionsEnabled) {
      return false;
    }
    if ((Config.sessionsCheckIP) && (!isInSession())) {
      return false;
    }
    long validUntil = Config.sessionLength * 60000L + this.lastQuitTimestamp;
    return (validUntil > System.currentTimeMillis()) || (isVIP());
  }
  
  public boolean setHashedPassword(String hashedPassword, String oldHashedPassword, String hashType)
  {
    if (!getPasswordHash().equals(oldHashedPassword)) {
      return false;
    }
    this.pcm.set("login.password", hashedPassword);
    this.pcm.set("login.hash", hashType.toUpperCase());
    return true;
  }
  
  public boolean setPassword(String rawPassword, String rawOldPassword, String hashType)
  {
    String oldPasswordHash = !getHashType().equalsIgnoreCase(hashType) ? getHashType() : hashType;
    try
    {
      rawPassword = Hasher.encrypt(rawPassword, hashType);
      rawOldPassword = Hasher.encrypt(rawOldPassword, oldPasswordHash);
    }
    catch (NoSuchAlgorithmException e)
    {
      e.printStackTrace();
      return false;
    }
    return setHashedPassword(rawPassword, rawOldPassword, hashType);
  }
  
  public String getPasswordHash()
  {
    return this.pcm.getString("login.password");
  }
  
  public String getHashType()
  {
    return this.pcm.getString("login.hash", "AMKAUTH");
  }
  
  public PConfManager getConfiguration()
  {
    return this.pcm;
  }
  
  /**
   * Remove AmkMcAuth PlayerProfile from configuration by the UUID of a player. 
   *
   * @param u UUID of the AuthPlayer to remove
   */
  public void remAuthPlayer(UUID u){
      synchronized (AuthPlayer.authPlayers) {
          if (AuthPlayer.authPlayers.containsKey(u)) {
          	AuthPlayer.authPlayers.remove(u);
          }
      }
  }
  /**
   * Sets AmkMcAuth Player as not logged in and no Username and no password 
   *
   */
  public void removeThisPlayer()
  {
    setLoggedIn(false);
    this.pcm.set("login.password", null);
    this.pcm.set("login.username", null);
  }
  
  public void enableAfterLoginGodmode()
  {
    if (Config.godModeAfterLogin) {
      this.pcm.set("godmode_expires", Long.valueOf(System.currentTimeMillis() + Config.godModeLength * 1000L));
    }
  }
  
  public boolean isInAfterLoginGodmode()
  {
    if (!Config.godModeAfterLogin) {
      return false;
    }
    long expires = this.pcm.getLong("godmode_expires", 0L);
    return expires >= System.currentTimeMillis();
  }
  
  public void login()
  {
    Player p = getPlayer();
    if (p == null) {
      throw new IllegalArgumentException("That player is not online!");
    }
    setLoggedIn(true);
    setUserName(p.getName());
    setLastLoginTimestamp(System.currentTimeMillis());
    BukkitTask reminder = getCurrentReminderTask();
    if (reminder != null) {
      reminder.cancel();
    }
    PConfManager pcm = getConfiguration();
    if (Config.adventureMode)
    {
      if (pcm.isSet("login.gamemode")) {
        try
        {
          p.setGameMode(GameMode.valueOf(pcm.getString("login.gamemode", "SURVIVAL")));
        }
        catch (IllegalArgumentException e)
        {
          p.setGameMode(GameMode.SURVIVAL);
        }
      }
      pcm.set("login.gamemode", null);
    }
    if (Config.allowMovementTime > 0L) {
      p.teleport(getJoinLocation());
    }
    if (Config.teleportToSpawn)
    {
      if (pcm.isSet("login.lastlocation")) {
        p.teleport(pcm.getLocation("login.lastlocation"));
      }
      pcm.set("login.lastlocation", null);
    }
    enableAfterLoginGodmode();
    setLoggedIn(true);
    setUserName(p.getName());
  }
  
  public void logout(Plugin plugin)
  {
    logout(plugin, true);
  }
  
  public void logout(Plugin plugin, boolean createReminders)
  {
    Player p = getPlayer();
    if (p != null)
    {
      setLoggedIn(false);
      if (createReminders) {
        if (isRegistered()) {
          createLoginReminder(plugin);
        } else {
          createRegisterReminder(plugin);
        }
      }
      PConfManager pcm = getConfiguration();
      if (Config.adventureMode)
      {
        if (!pcm.isSet("login.gamemode")) {
          pcm.set("login.gamemode", p.getGameMode().name());
        }
        p.setGameMode(GameMode.ADVENTURE);
      }
      if (Config.teleportToSpawn)
      {
        if (!pcm.isSet("login.lastlocation")) {
          pcm.setLocation("login.lastlocation", p.getLocation());
        }
        p.teleport(p.getLocation().getWorld().getSpawnLocation());
      }
      setLoggedIn(false);
    }
  }
  
  public void setLastLoginTimestamp(long timestamp)
  {
    this.lastLoginTimestamp = timestamp;
    this.pcm.set("timestamps.login", Long.valueOf(timestamp));
  }
  
  public void setLastQuitTimestamp(long timestamp)
  {
    this.lastQuitTimestamp = timestamp;
    this.pcm.set("timestamps.quit", Long.valueOf(timestamp));
  }
  
  public boolean setHashedPassword(String newPasswordHash, String hashType)
  {
    this.pcm.set("login.password", newPasswordHash);
    this.pcm.set("login.hash", hashType);
    return true;
  }
  
  public boolean setPassword(String rawPassword, String hashType)
  {
    try
    {
      rawPassword = Hasher.encrypt(rawPassword, hashType);
    }
    catch (NoSuchAlgorithmException e)
    {
      e.printStackTrace();
      return false;
    }
    this.pcm.set("login.password", rawPassword);
    this.pcm.set("login.hash", hashType);
    return true;
  }
  
  public void setLastIPAddress(String ip)
  {
    this.lastIPAddress = ip.replace("/", "");
    
    this.pcm.set("login.ipaddress", this.lastIPAddress);
  }
  
  public void updateLastIPAddress()
  {
    String ip = getCurrentIPAddress();
    if (ip.isEmpty()) {
      return;
    }
    setLastIPAddress(ip);
  }
  
  public String getCurrentIPAddress()
  {
    Player p = getPlayer();
    if (p == null) {
      return "";
    }
    InetSocketAddress isa = p.getAddress();
    if (isa == null) {
      return "";
    }
    return isa.getAddress().toString().replace("/", "");
  }
  
  public BukkitTask getCurrentReminderTask()
  {
    return this.reminderTask;
  }
  
  public BukkitTask createLoginReminder(Plugin p)
  {
    this.reminderTask = AmkAUtils.createLoginReminder(getPlayer(), p);
    return getCurrentReminderTask();
  }
  
  public BukkitTask createRegisterReminder(Plugin p)
  {
    this.reminderTask = AmkAUtils.createRegisterReminder(getPlayer(), p);
    return getCurrentReminderTask();
  }
  
  public Player getPlayer()
  {
    return Bukkit.getPlayer(this.playerUUID);
  }
  
  public String getUserName()
  {
    return this.pcm.getString("login.username");
  }
  
  public UUID getUniqueId()
  {
    return this.playerUUID;
  }
  
  public long getLastJoinTimestamp()
  {
    return this.lastJoinTimestamp;
  }
  
  public long getLastWalkTimestamp()
  {
    return this.lastWalkTimestamp;
  }
  
  public void setLastJoinTimestamp(long timestamp)
  {
    this.lastJoinTimestamp = timestamp;
    this.lastWalkTimestamp = timestamp;
    this.pcm.set("timestamps.join", Long.valueOf(timestamp));
  }
  
  public void setLastWalkTimestamp(long timestamp)
  {
    this.lastWalkTimestamp = timestamp;
  }
  
  public Location getJoinLocation()
  {
    return this.lastJoinLocation;
  }
  
  public void setJoinLocation(Location l)
  {
    this.lastJoinLocation = l;
  }
}
