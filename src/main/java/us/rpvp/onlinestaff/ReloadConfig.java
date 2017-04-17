package us.rpvp.onlinestaff;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

import java.sql.SQLException;

public class ReloadConfig extends Command {

	public ReloadConfig() {
		super("osreload", "onlinestaff.reload");
	} 

	@Override
	public void execute(CommandSender sender, String[] strings) {
		if (!OnlineStaff.active){
			sender.sendMessage(ChatColor.RED+"Plugin disabled.");
			return;
		}
		
		if(sender.hasPermission("onlinestaff.reload")) {
			if (OnlineStaff.getInstance().checkConnection()){
				OnlineStaff.getInstance().closeConnection();
			}
			OnlineStaff.getInstance().reloadConfig();
				
			if(OnlineStaff.config.getBoolean("configured")) {
				
				try {
					OnlineStaff.getInstance().startConnection();
				} catch(SQLException e) {
					//e.printStackTrace();
					OnlineStaff.logger("Failed to run reload command for "+sender.getName() +", "+e.getMessage());
				}
			}
			sender.sendMessage(OnlineStaff.chatPrefix + ChatColor.GOLD + " Configuration reloaded.");
		}
	}
}