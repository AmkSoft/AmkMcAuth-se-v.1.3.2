package com.mooo.amksoft.amkmcauth;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.google.common.io.PatternFilenameFilter;

public class PConfManager extends YamlConfiguration {
  private static final Map<UUID, PConfManager> pcms = new HashMap<>();
  private final Object saveLock = new Object();
  private File pconfl = null;
  private static String VipPlayers = " ";
  private static int PlayerCount = 0;
  private static List<String> IpAdresses = new ArrayList<String>();
  private static List<Integer> IpAdressesCnt = new ArrayList<Integer>();
  private static String PlayerNameFile = AmkMcAuth.dataFolder + File.separator + "PlayerNames.txt";
  
  PConfManager(OfflinePlayer p)
  	{
    File dataFolder = AmkMcAuth.dataFolder;
    this.pconfl = new File(dataFolder + File.separator + "userdata" + File.separator + p.getUniqueId() + ".yml");
    try
    	{
    	load(this.pconfl);
    }
    catch (Exception localException) {}
  }
  
  PConfManager(UUID u)
  	{
    File dataFolder = AmkMcAuth.dataFolder;
    this.pconfl = new File(dataFolder + File.separator + "userdata" + File.separator + u + ".yml");
    try
    	{
    	load(this.pconfl);
    }
    catch (Exception localException) {}
  }
  
  PConfManager() {}
  
  public static PConfManager getPConfManager(Player p)
  	{
    return getPConfManager(p.getUniqueId());
  }
  
  public static PConfManager getPConfManager(UUID u)
  {
    synchronized (pcms)
    	{
    	if (pcms.containsKey(u)) {
    		return (PConfManager)pcms.get(u);
    	}
    	PConfManager pcm = new PConfManager(u);
    	pcms.put(u, pcm);
    	return pcm;
    }
  }
  
  public static void saveAllManagers()
  	{
  
    synchronized (pcms)
    	{    	
		if(Config.removeAfterDays>0) {
			
	    	Logger log = AmkMcAuth.getInstance().getLogger();
			
   			UUID Uid;
   	   		// 1000*60 sec. * 60 min. * 24 uur * Config.removeAfterDays dagen
   	  		long PlayerSleepTime = (1000*60 * 60 * 24 * Config.removeAfterDays );
			//for (PConfManager pcm : PConfManager.pcms.values()) {
			List<UUID> toRemove = new ArrayList<UUID>();  // Fixes ConcurrentModificationException ??
   			for (PConfManager pcm : PConfManager.pcms.values()) { // Gives ConcurrentModificationException on remove!!
   				// First: Remove Old Entries from pcms (Player has NOT logged in for a LONG time)
   				// This information is already saved on the previous saveAllManagers run, so no worry.
   				// We are ONLY freeing up server internal memory, not Profile Files/SQL-Data!!!.
   				// PlayerTimeoutAt is Join-time + Sleep-time. Is normaly higher!! then Current-Timestamp
   				// Invalid Logons will be removed asap at they do not have a true Join-timestamp!
   					
   				long PlayerTimeoutAt = pcm.getLong("timestamps.join",0L) + PlayerSleepTime;
   				//long PlayerTimeoutAt = ap.getLastJoinTimestamp() + PlayerSleepTime;
   				long PlayerTimeLeft=PlayerTimeoutAt - System.currentTimeMillis();
   				//if(PlayerTimeoutAt < System.currentTimeMillis()) {
   	   			if(PlayerTimeLeft<0) { // Out of Time, Remove PlayerData from memory
					String SoFar="Start try";
   					try {
						Uid = AmkAUtils.getUUID(pcm.getString("login.username"));
						AuthPlayer ap = AuthPlayer.getAuthPlayer(Uid);
						if (Uid!=null && pcm.getString("login.username")!=null) { 
							//if (PConfManager.pcms.containsKey(Uid) & !ap.isLoggedIn()) {
							if (PConfManager.pcms.containsKey(Uid)) {
								//if(DebugSave) log.info("Player: " + pcm.getString("login.username") + " not logged in for " + 
		             			//		Config.removeAfterDays + " days. Clearing Profile-data.");
								log.info("Player: " + pcm.getString("login.username") + " not Joined in for " + 
		             					Config.removeAfterDays + " days. Removing in-memory Profile-data.");
								SoFar="map.remove";
		   						pcm.map.remove(Uid); // remove this pcm map
								SoFar="pcms.remove";
								toRemove.add(Uid); // Fixes ConcurrentModificationException ??
		   						//PConfManager.pcms.remove(Uid); // remove this pcms from list
								SoFar="ap.remAuthPlayer";
								ap.remAuthPlayer(Uid); // Removes authPlayers HashMap from AuthPlayer
								SoFar="Yes!!, All done";
							}								
						}            
					} catch (Exception ex) {
	   					log.info(SoFar + ": failed to Clear: " + pcm.getString("login.username") + "'s Profile-data!");
						//ex.printStackTrace();
					}
   				}
   			}
   			for(UUID key: toRemove ) {  // Fixes ConcurrentModificationException ??
   				PConfManager.pcms.remove(key); // remove this pcms from list   					
   			}
   		}

    	for (PConfManager pcm : pcms.values()) {
    		if (pcm.isSet("login.password")) {
    			pcm.forceSave();
    		}
    	}
    }
  }
  
  public void forceSave()
  	{
    synchronized (this.saveLock)
    	{
    	try
    		{
    		save(this.pconfl);
    	}
    	catch (IOException localIOException) {}
    }
  }
  
  /** 
   * Get all PlayerProfile Files and parse info in it.
   * Remembers IP-Addresses and VIP players. Call from onEnabled.
   */
  public static void countPlayersFromIpAndGetVipPlayers()
	{
	String AllPlayers = " ";
	String PlayerFound;
	PlayerCount=0;

	boolean VipFound=false;
	boolean UserNameFound=false;
	final File userdataFolder = new File(AmkMcAuth.dataFolder, "userdata");
	if (!userdataFolder.exists() || !userdataFolder.isDirectory()) return;

	//File IpFile = new File(IpAddressesFile);
	//IpFile.delete(); // Remove the 'old' IpAddressFile

	for (String fileName : userdataFolder.list(new PatternFilenameFilter("(?i)^.+\\.yml$"))) {
		Scanner in;
		VipFound=false;
		UserNameFound=false;
		PlayerFound=" ";
		PlayerCount++;
		try {
			in = new Scanner(new File(userdataFolder + File.separator + fileName));
			//while (in.hasNextLine()) { // iterates each line in the file
			while (in.hasNext()) { // 1 more character?: iterates each line in the file
				String line = in.nextLine();
				if(line.contains("username:")) {
					PlayerFound = line.substring(line.lastIndexOf(" ")+1) + " ";
					// AllPlayers bugfix SamePlayer with different lower-/uppercase letters 
					AllPlayers = AllPlayers + PlayerFound;
					UserNameFound=true;
				}
				if(line.contains("vip: true")){
					VipFound=true;
				}
				if(line.contains("ipaddress:") && Config.maxUsersPerIpaddress>0 ){
					addPlayerToIp(line.substring(line.lastIndexOf(" ")+1));
				}
			}
			if(VipFound && UserNameFound){
				VipPlayers = VipPlayers + PlayerFound;
			}
			in.close(); // don't forget to close resource leaks
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	byte data[]=AllPlayers.getBytes();
	Path p = Paths.get(PlayerNameFile);
	try (OutputStream out = new BufferedOutputStream(
			Files.newOutputStream(p, CREATE, TRUNCATE_EXISTING))) {
		out.write(data,0,data.length);
	} catch (IOException e) {
		e.printStackTrace();        	
	}
  }
  
  public static int getPlayerCount()
  {
    return PlayerCount;
  }
  
  public static int getIpaddressCount()
  {
    return IpAdresses.size();
  }
  
  public static String listIpaddressesInfo(int i)
  {
    return (String)IpAdresses.get(i) + "  " + IpAdressesCnt.get(i);
  }
  
  public static int countPlayersFromIp(String IpAddress)
  {
    int PlIdx = IpAdresses.indexOf(IpAddress);
    if (PlIdx == -1) {
      return 0;
    }
    return ((Integer)IpAdressesCnt.get(PlIdx)).intValue();
  }
  
  public static void addPlayerToIp(String IpAddress)
  {
    if (Config.maxUsersPerIpaddress == 0L) {
      return;
    }
    int PlIdx = IpAdresses.indexOf(IpAddress);
    if (PlIdx == -1)
    {
      IpAdresses.add(IpAddress);
      IpAdressesCnt.add(Integer.valueOf(1));
    }
    else
    {
      IpAdressesCnt.set(PlIdx, Integer.valueOf(((Integer)IpAdressesCnt.get(PlIdx)).intValue() + 1));
    }
  }
  
  public static void removePlayerFromIp(String IpAddress)
  {
    if (Config.maxUsersPerIpaddress == 0L) {
      return;
    }
    int PlIdx = IpAdresses.indexOf(IpAddress);
    if (PlIdx >= 0)
    {
      int Cntr = ((Integer)IpAdressesCnt.get(PlIdx)).intValue();
      if (Cntr > 0) {
        IpAdressesCnt.set(PlIdx, Integer.valueOf(Cntr - 1));
      }
    }
  }
  
  public static String getVipPlayers()
  {
    return VipPlayers;
  }
  
  public static int getVipPlayerCount()
  {
    if (VipPlayers.isEmpty()) {
      return 0;
    }
    return VipPlayers.split("\\s+").length - 1;
  }
  
  public static void addVipPlayer(String NewPlayer)
  {
    if (!VipPlayers.contains(" " + NewPlayer + " ")) {
      VipPlayers = VipPlayers + NewPlayer + " ";
    }
  }
  
  public static void removeVipPlayer(String PlayerToRemove)
  {
    if (VipPlayers.contains(" " + PlayerToRemove + " ")) {
      VipPlayers = VipPlayers.replaceAll("(?i) " + PlayerToRemove.toLowerCase() + " ", " ");
    }
  }
  
  public static boolean doesPlayerExist(String PlayerToSearch)
  {
    String AllPlayers = "";
    Path p = Paths.get(PlayerNameFile, new String[0]);
	Logger log = AmkMcAuth.getInstance().getLogger();
    if(!Files.exists(p)) {
		log.info(PlayerNameFile + " not found");
    	return false;
    }
    
    try
    {
    	AllPlayers = new String(Files.readAllBytes(p));
    }
    catch (IOException e)
    {
    	e.printStackTrace();
    }
    AllPlayers = AllPlayers.toLowerCase();
    if (AllPlayers.isEmpty()) {
    	log.info(PlayerNameFile + " isEmpty");
    	return false;
    }
    return (AllPlayers.indexOf(" " + PlayerToSearch.toLowerCase() + " ") >= 0);
  }
  
  /** 
   * Add a Player to the ALL-Player list. 
   */
  public static void addAllPlayer(String NewPlayer)
  {
	//if(!AllPlayers.contains(" " + NewPlayer + " ")){
	//	AllPlayers = AllPlayers + NewPlayer + " ";
	NewPlayer = " " + NewPlayer + " ";
	
	byte data[]=NewPlayer.getBytes();
	Path p = Paths.get(PlayerNameFile);
	try (OutputStream out = new BufferedOutputStream(
			Files.newOutputStream(p, CREATE, APPEND))) {
		out.write(data,0,data.length);
	} catch (IOException e) {
		e.printStackTrace();        	
	}        
  }

  /** 
   * remove a Player From the ALL-Player list. 
   */
  public static void removeAllPlayer(String PlayerToRemove)
  {
	String AllPlayers = "";
	Path p = Paths.get(PlayerNameFile);

	try {
		AllPlayers = new String(Files.readAllBytes(p));
	} catch (IOException e) {
		e.printStackTrace();
	}

	AllPlayers = AllPlayers.replaceAll("(?i) " + PlayerToRemove.toLowerCase() + " ", " ");
  
	byte data[]=AllPlayers.getBytes();
	try (OutputStream out = new BufferedOutputStream(
			Files.newOutputStream(p, CREATE, TRUNCATE_EXISTING))) {
		out.write(data,0,data.length);
	} catch (IOException e) {
		e.printStackTrace();        	
	}        
  }
  
  public static void purge()
  {
    synchronized (pcms)
    {
      pcms.clear();
    }
  }
  
  public boolean exists()
  {
    return this.pconfl.exists();
  }
  
  public boolean createFile()
  {
    try
    {
      return this.pconfl.createNewFile();
    }
    catch (IOException ignored) {}
    return false;
  }
  
  public static void removePlayer(UUID u)
  {
    synchronized (pcms)
    {
      if (pcms.containsKey(u))
      {
        pcms.clear();
        File dataFolder = AmkMcAuth.dataFolder;
        File rfile = new File(dataFolder + File.separator + "userdata" + File.separator + u + ".yml");
        if (rfile.exists()) {
          rfile.delete();
        }
      }
    }
  }
  
  public Location getLocation(String path)
  {
    if (get(path) == null) {
      return null;
    }
    String world = getString(path + ".w");
    double x = getDouble(path + ".x");
    double y = getDouble(path + ".y");
    double z = getDouble(path + ".z");
    float pitch = getFloat(path + ".pitch");
    float yaw = getFloat(path + ".yaw");
    return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
  }
  
  public void setLocation(String path, Location value)
  {
    set(path + ".w", value.getWorld().getName());
    set(path + ".x", Double.valueOf(value.getX()));
    set(path + ".y", Double.valueOf(value.getY()));
    set(path + ".z", Double.valueOf(value.getZ()));
    set(path + ".pitch", Float.valueOf(value.getPitch()));
    set(path + ".yaw", Float.valueOf(value.getYaw()));
  }
  
  public float getFloat(String path)
  {
    return (float)getDouble(path);
  }
}
