package us.rpvp.onlinestaff;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class showStaff extends Command {

	public showStaff() {
		super("staff", "onlinestaff.show");
	}

	@Override
	public void execute(final CommandSender sender, String[] strings) {
		if(sender.hasPermission("onlinestaff.show") && strings.length==0) {
			sender.sendMessage(OnlineStaff.chatPrefix + ChatColor.GOLD + " -----Staff Online-----");
			if (OnlineStaff.getInstance().checkConnection()){
				try {
					Statement statement;
					statement = OnlineStaff.getInstance().con.createStatement();
					
					String query = "SELECT `name`, `current_server`, `is_hidden` FROM OnlineStaff WHERE `is_online`='1' and `is_hidden`='0'";
					if (sender.hasPermission("onlinestaff.staff")){
						query = "SELECT `name`, `current_server`, `is_hidden` FROM OnlineStaff WHERE `is_online`='1'";
					}
					ResultSet rs=statement.executeQuery(query);
					int cnt=0;
					while (rs.next()) {
						if (rs.getInt("is_hidden")==1 && sender.hasPermission("onlinestaff.staff")){
					        sender.sendMessage(ChatColor.GRAY+"        "+rs.getString("name") +ChatColor.GRAY+" is on server "+rs.getString("current_server"));

						}else{
					        sender.sendMessage(ChatColor.WHITE+"        "+ChatColor.GREEN+rs.getString("name") +ChatColor.WHITE+" is on server "+ChatColor.GREEN+rs.getString("current_server"));
						}
				        cnt++;
				    }
					if (cnt==0){
						sender.sendMessage("        "+ChatColor.RED+"No staff online");
					}else{
						sender.sendMessage(ChatColor.AQUA +"    ("+cnt+") Staff displayed");
					}
					statement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}else{
				sender.sendMessage(ChatColor.GOLD + "\tNo database connection.");

			}
			sender.sendMessage(OnlineStaff.chatPrefix + ChatColor.GOLD + " -------- END --------");
		}else if (sender.hasPermission("onlinestaff.show") && strings.length==1){
			if (strings[0].equalsIgnoreCase("hide")){
				final CommandSender es=sender;
				OnlineStaff.getInstance().getProxy().getScheduler().runAsync(OnlineStaff.getInstance(), new Runnable() {
					@Override
					public void run() {
						try {
							Statement statement;
							statement = OnlineStaff.getInstance().con.createStatement();
							String query = "UPDATE `OnlineStaff` SET `is_hidden`='1' WHERE name = '" + es.getName() + "' LIMIT 1";
							statement.executeUpdate(query);
							sender.sendMessage(OnlineStaff.chatPrefix+ChatColor.RED+" You are now hidden to members.");
						} catch(SQLException e) {
							e.printStackTrace();
							sender.sendMessage(OnlineStaff.chatPrefix+ChatColor.RED+" There was an error processing the command.");
						}
					}
				});
			}else if (strings[0].equalsIgnoreCase("show")){
				final CommandSender es=sender;
				OnlineStaff.getInstance().getProxy().getScheduler().runAsync(OnlineStaff.getInstance(), new Runnable() {
					@Override
					public void run() {
						try {
							Statement statement;
							statement = OnlineStaff.getInstance().con.createStatement();
							String query = "UPDATE `OnlineStaff` SET `is_hidden`='0' WHERE name = '" + es.getName() + "' LIMIT 1";
							statement.executeUpdate(query);
							sender.sendMessage(OnlineStaff.chatPrefix+ChatColor.GREEN+" You are now shown to members.");
						} catch(SQLException e) { 
							e.printStackTrace();
							sender.sendMessage(OnlineStaff.chatPrefix+ChatColor.RED+" There was an error processing the command.");
						}
					}
				});
			}else{
				sender.sendMessage(OnlineStaff.chatPrefix+" Unknown argument, accepted is show/hide");
			}
		}
		
	}
}