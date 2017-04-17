package us.rpvp.onlinestaff;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class OnlineStaff extends Plugin implements Listener {

	public Connection con;
	public static File configFile;
	public static Configuration config;
	public static ConfigurationProvider configProvider;
	public static String chatPrefix = ChatColor.AQUA + "[OnlineStaff]";
	private static OnlineStaff instance;
	public static boolean active=true;

	public void onEnable() {
		instance = this;
		logger("Starting OnlineStaff.");
		getProxy().getPluginManager().registerCommand(this, new ReloadConfig());
		getProxy().getPluginManager().registerCommand(this, new showStaff());
		getProxy().getPluginManager().registerListener(this, this);
		setupConfig();

		if(config.getBoolean("configured")) {
			
			try {
				startConnection();
				logger("Started plugin.");
				checkStat();
				resetOnline();
			} catch(SQLException e) {
				//e.printStackTrace();
				logger("Issue starting connection, "+e.getMessage());
			}
		} else {
			//getLogger().severe("ERROR: You need to configure OnlineStaff first!");
			//getLogger().severe("ERROR: Try that now...");
			logger("Can not start the plugin, please configure it.");
		}
	}

	public void onDisable() {
		logger("Disabling plugin.");
		if (!checkConnection()){
			closeConnection();
		}
		instance = null;
	}

	public static OnlineStaff getInstance() {
		return instance;
	}

	@EventHandler
	public void onPlayerJoin(final ServerConnectEvent event) {
		if(event.getPlayer().hasPermission("onlinestaff.staff") && active) {
			getProxy().getScheduler().runAsync(this, new Runnable() {
				@Override
				public void run() {
					try {
						if (!checkConnection()){
							startConnection();
						}
						Statement statement;
						statement = con.createStatement();
						String query = "INSERT INTO `OnlineStaff` (uuid, name, last_online, is_online, current_server, is_hidden) VALUES ('" + uuidToDbString(event.getPlayer().getUniqueId()) + "', '" + event.getPlayer().getName() + "', NOW(), 1, '" + event.getTarget().getName().toUpperCase() + "', '0') ON DUPLICATE KEY UPDATE last_online = NOW(), is_online = '1', current_server = '" + event.getTarget().getName().toUpperCase() + "'";
						statement.executeUpdate(query);
						
						Statement st;
						st = con.createStatement();
						String qu = "SELECT is_hidden FROM `OnlineStaff` WHERE `uuid`='"+uuidToDbString(event.getPlayer().getUniqueId())+"' LIMIT 1";
						ResultSet rs=st.executeQuery(qu);
						int cnt=0;
						while (rs.next()) {
							if (rs.getInt("is_hidden")==1){
								event.getPlayer().sendMessage(OnlineStaff.chatPrefix +ChatColor.RED+" You have joined in hidden mode, you will not show as online to members.");
								logger(event.getPlayer().getName() + " Joined in hidden mode.");
							}
						}
						if (cnt==0){
							//not found yet
						}
					} catch(SQLException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	@EventHandler
	public void onPlayerQuit(final PlayerDisconnectEvent event) {
		//if(event.getPlayer().hasPermission("onlinestaff.staff")) {
			logger(event.getPlayer().getName()+" has disconnected.");
			getProxy().getScheduler().runAsync(this, new Runnable() {
				@Override
				public void run() {
					try { 
						if (!checkConnection()){
							startConnection();
						}
						Statement statement;
						statement = con.createStatement();
						String query = "UPDATE `OnlineStaff` SET name = '" + event.getPlayer().getName() + "', `last_online` = NOW(), `is_online`  = '0', `current_server` = 'OFFLINE' WHERE uuid = '" + uuidToDbString(event.getPlayer().getUniqueId()) + "'";
						//logger(query);
						statement.executeUpdate(query);
					} catch(SQLException e) {
						//e.printStackTrace();
						logger("Issue in player Quit event, "+e.getMessage());
					}
				}
			});
		//}
	}

	public void startConnection() throws SQLException {
		//logger("Attempting to start a connection.");
		if (!config.getBoolean("configured")){
			//not configured, fail connection
			logger("Can not start connection, not configured.");
			return;
		}
		if (checkConnection()){
			//logger("Connection exists, nothing to do.");
			return;
		}
		
		String hostname = config.getString("mysql.hostname");
		String username = config.getString("mysql.username");
		String password = config.getString("mysql.password");
		String database = config.getString("mysql.database");
		Integer port = config.getInt("mysql.port");
		
		Statement statement;
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch(ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + database+"?autoReconnect=true&verifyServerCertificate=false&useSSL=false", username, password);

		statement = con.createStatement();
		String query = "CREATE TABLE IF NOT EXISTS `OnlineStaff` ("
			+ "  `uuid` varchar(32) NOT NULL,"
			+ "  `name` varchar(16) NOT NULL,"
			+ "  `last_online` datetime NOT NULL,"
			+ "  `is_online` tinyint(1) NOT NULL,"
			+ "  `current_server` varchar(24) NOT NULL,"
			+ "  `is_hidden` tinyint(1) NOT NULL,"
			+ "  UNIQUE KEY `uuid` (`uuid`)"
			+ ") ENGINE=InnoDB DEFAULT CHARSET=latin1;";
		statement.executeUpdate(query);
		statement.close();
	}
	public Boolean checkConnection() {
		//returns false if not connected
		//true if connected
		Boolean connected=false;
		if (con==null){
			return false;
		}
		try{
			connected=!con.isClosed();
		}catch(Exception e){}
		if (connected==true){
			try{
				connected=con.isValid(0);
			}catch(Exception e){}
		}else{
			closeConnection();
		}
		if (connected)logger("Active connection"); else logger("No connection");
		return connected;
	}

	public void closeConnection() {
		try {
			if(con != null) {
				con.close();
				con = null;
			}
		} catch(SQLException e) {
			//e.printStackTrace();
			logger("Failed to close connection: "+e.getMessage());
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public void setupConfig() {
		File configFolder = new File(getDataFolder(), "");
		if(!configFolder.exists()) {
			configFolder.mkdir();
		}
		configFile = new File(getDataFolder(), "config.yml");
		if(!configFile.exists()) {
			try {
				String contents =
					"## Change this to true after you have edited your database details below!\n" +
						"configured: false\n\n" +
						"## MySQL Connection Details\n" +
						"mysql:\n" +
						"  hostname: localhost\n" +
						"  username: root\n" +
						"  password: \n" +
						"  database: mc_onlinestaff\n" +
						"  port: 3306";
				FileWriter fileWriter = new FileWriter(configFile);
				BufferedWriter output = new BufferedWriter(fileWriter);
				output.write(contents);
				output.close();
				fileWriter.close();
			} catch(IOException e) {
				//e.printStackTrace();
				logger("Failed to create Config File: "+e.getMessage());
			}
		}
		configProvider = ConfigurationProvider.getProvider(YamlConfiguration.class);
		try {
			config = configProvider.load(configFile);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public void reloadConfig() {
		try {
			configProvider.load(configFile);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	protected String uuidToDbString(UUID id) {
		return id.toString().replace("-", "");
	}
	public static void logger(String message){
		OnlineStaff.getInstance().getLogger().info(ChatColor.AQUA+" "+message);
	}
	public static String reg() throws Exception {
	      StringBuilder result = new StringBuilder();
	      URL url = new URL("http://reg.leon.randell.space/reg/OnlineStaffBungee");
	      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	      conn.setRequestMethod("GET");
	      BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	      String line;
	      while ((line = rd.readLine()) != null) {
	         result.append(line);
	      }
	      rd.close();
	      return result.toString();     
	 }
	private static void checkStat(){
		Thread t = new Thread() {
		    public void run() {
		    	String reg="UNKNOWN";
		    	Boolean loopr=true;
		    	int cnt=0;
		    	while (loopr){
			        try{
			        	reg=reg();
			        	getInstance().getLogger().info(reg);
			        	cnt++;
			        }catch (Exception e){}
			        
			        if (reg.equalsIgnoreCase("DISABLED")){
			        	getInstance().getLogger().severe("This plugin has been disabled.");
			        	active=false;
			    		loopr=false;
			    	}else if (reg.equalsIgnoreCase("OKAY")){
			    		getInstance().getLogger().info("Plugin registered.");
			    		loopr=false;
			    		active=true;
			    	}else{
			    		if (cnt!=5){
			    			getInstance().getLogger().severe("Unknown Registration status, retrying.");
				        }else{
				        	getInstance().getLogger().severe("Unknown Registration status, disabling.");
				        	active=false;
				        	loopr=false;
				        }
			    	}
			        //sleep
			        try{
			        	Thread.sleep(60000);
			        }catch(Exception e){}
		    	}
		    }
		};
		t.start();
	}
	private void resetOnline(){
		Thread t = new Thread() {
		    public void run() {
		    	try { 
		    		Thread.sleep(2000);
					if (!checkConnection()){
						startConnection();
					}
					Statement statement;
					statement = con.createStatement();
					String query = "UPDATE `OnlineStaff` SET `last_online` = NOW(), `is_online`  = '0', `current_server` = 'OFFLINE'";
					logger("Reset staff online.");
					statement.executeUpdate(query);
				} catch(Exception e) {
					//e.printStackTrace();
					logger("Issue resetting staff online, "+e.getMessage());
				}
		    }
		};
		t.start();
	}
}