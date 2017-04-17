package us.rpvp.onlinestaff;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class showStaff extends Command {

	public showStaff() {
		super("staff", "onlinestaff.show");
	}

	@Override
	public void execute(final CommandSender sender, String[] strings) {
		if (!OnlineStaff.active){
			sender.sendMessage(ChatColor.RED+"Plugin disabled.");
			return;
		}
		//check database
		if (!OnlineStaff.getInstance().checkConnection()) {
			sender.sendMessage(ChatColor.GOLD + "\tNo database connection.");
			OnlineStaff.logger(sender.getName()+" tried to run /staff but there was no database, connecting.");
			try {
				OnlineStaff.getInstance().startConnection();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				OnlineStaff.logger("Failed to start connection, "+e.getMessage());
				
			}
		}
		
		
		if (sender.hasPermission("onlinestaff.show") && strings.length == 0) {
			sender.sendMessage(OnlineStaff.chatPrefix + ChatColor.GOLD + " -----Staff Online-----");
			try {
				Statement statement;
				statement = OnlineStaff.getInstance().con.createStatement();

				String query = "SELECT `name`, `current_server`, `is_hidden` FROM OnlineStaff WHERE `is_online`='1' and `is_hidden`='0'";
				if (sender.hasPermission("onlinestaff.staff")) {
					query = "SELECT `name`, `current_server`, `is_hidden` FROM OnlineStaff WHERE `is_online`='1'";
				}
				ResultSet rs = statement.executeQuery(query);
				int cnt = 0;
				while (rs.next()) {
					if (rs.getInt("is_hidden") == 1 && sender.hasPermission("onlinestaff.staff")) {
						sender.sendMessage(ChatColor.GRAY + "        INVISIBLE- " + rs.getString("name") + ChatColor.GRAY
								+ " is on server " + rs.getString("current_server"));

					} else {
						sender.sendMessage(
								ChatColor.WHITE + "        " + ChatColor.GREEN + rs.getString("name") + ChatColor.WHITE
										+ " is on server " + ChatColor.GREEN + rs.getString("current_server"));
					}
					cnt++;
				}
				if (cnt == 0) {
					sender.sendMessage("        " + ChatColor.RED + "No staff online");
				} else {
					sender.sendMessage(ChatColor.AQUA + "    (" + cnt + ") Staff displayed");
				}
				statement.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			sender.sendMessage(OnlineStaff.chatPrefix + ChatColor.GOLD + " -------- END --------");
		} else if (sender.hasPermission("onlinestaff.show") && strings.length == 1) {
			if (strings[0].equalsIgnoreCase("hide")|| strings[0].equalsIgnoreCase("h")) {
				setHidden(sender);
			} else if (strings[0].equalsIgnoreCase("show")|| strings[0].equalsIgnoreCase("s")) {
				setShown(sender);
			} else if (strings[0].equalsIgnoreCase("v") || strings[0].equalsIgnoreCase("vanish")) {
				if (getPlayerStatus(sender)) {
					//player shown, hide player
					setHidden(sender);
				}else{
					//player hidden, show player
					setShown(sender);
				}
			} else {
				sender.sendMessage(OnlineStaff.chatPrefix + " Unknown argument, accepted is show/hide/v");
			}
		}

	}
	private static boolean getPlayerStatus(CommandSender sender){
		//if shown, return true else false
		try {
			Statement statement;
			statement = OnlineStaff.getInstance().con.createStatement();
			UUID UUID = ((ProxiedPlayer) sender).getUniqueId();
			String query = "SELECT `is_hidden` FROM OnlineStaff WHERE `uuid`='"+OnlineStaff.getInstance().uuidToDbString(UUID)+"' LIMIT 1";
			ResultSet rs = statement.executeQuery(query);
			while (rs.next()) {
				if (rs.getInt("is_hidden") == 1) {
					//player is hidden
					return true;

				} else {
					//player is shown
					return false;
				}
			}
			return false;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private static void setHidden(final CommandSender sender){
		OnlineStaff.getInstance().getProxy().getScheduler().runAsync(OnlineStaff.getInstance(), new Runnable() {
			@Override
			public void run() {
				try {
					Statement statement;
					statement = OnlineStaff.getInstance().con.createStatement();
					String query = "UPDATE `OnlineStaff` SET `is_hidden`='1' WHERE name = '" + sender.getName()
							+ "' LIMIT 1";
					statement.executeUpdate(query);
					sender.sendMessage(
							OnlineStaff.chatPrefix + ChatColor.RED + " You are now hidden to members.");
				} catch (SQLException e) {
					e.printStackTrace();
					sender.sendMessage(OnlineStaff.chatPrefix + ChatColor.RED
							+ " There was an error processing the command.");
				}
			}
		});
	} 
	private static void setShown(final CommandSender sender){
		OnlineStaff.getInstance().getProxy().getScheduler().runAsync(OnlineStaff.getInstance(), new Runnable() {
			@Override
			public void run() {
				try {
					Statement statement;
					statement = OnlineStaff.getInstance().con.createStatement();
					String query = "UPDATE `OnlineStaff` SET `is_hidden`='0' WHERE name = '" + sender.getName()
							+ "' LIMIT 1";
					statement.executeUpdate(query);
					sender.sendMessage(
							OnlineStaff.chatPrefix + ChatColor.RED + " You are now shown to members.");
				} catch (SQLException e) {
					e.printStackTrace();
					sender.sendMessage(OnlineStaff.chatPrefix + ChatColor.RED
							+ " There was an error processing the command.");
				}
			}
		});
	} 
}