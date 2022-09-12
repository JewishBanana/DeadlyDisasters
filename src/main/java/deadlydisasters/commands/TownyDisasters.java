package deadlydisasters.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.metadata.BooleanDataField;

import deadlydisasters.general.Main;
import deadlydisasters.utils.Utils;

public class TownyDisasters implements CommandExecutor,TabCompleter {
	
	private Main plugin;
	
	public TownyDisasters(Main plugin) {
		this.plugin = plugin;
		
		plugin.getCommand("towndisasters").setExecutor(this);
	}
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    	if (!Utils.TownyB) {
    		sender.sendMessage(Utils.chat("&cTowny has not been detected on the server!"));
    		return true;
    	}
    	if (args.length < 1 || args.length > 2) {
    		sender.sendMessage(Utils.chat("&c/towndisasters <on|off> [player]"));
    		return true;
    	}
    	if (!(sender instanceof Player) && args.length != 2) {
    		sender.sendMessage(Utils.chat("&c/towndisasters <on|off> <player>"));
    		return true;
    	} else if (!((Player) sender).hasPermission("deadlydisasters.towny")) {
    		sender.sendMessage(Utils.chat(plugin.getConfig().getString("messages.permission_error")));
			return true;
    	}
    	Resident resident = null;
    	if (args.length == 2) {
    		resident = Utils.getTownyAPI().getResident(args[1]);
    		if (resident == null) {
    			sender.sendMessage(Utils.chat("&cCould not find player '"+args[1]+"'"));
        		return true;
    		}
    	} else
    		resident = Utils.getTownyAPI().getResident((Player) sender);
    	if (resident.getTownOrNull() == null) {
    		sender.sendMessage(Utils.chat("&cPlayer '"+resident.getName()+"' does not have a town!"));
    		return true;
    	}
    	if (args[0].equalsIgnoreCase("on")) {
    		resident.getTownOrNull().addMetaData(new BooleanDataField("DeadlyDisasters", true));
            TownyMessaging.sendPrefixedTownMessage(resident.getTownOrNull(), "Disaster Protection has been Enabled.");
            return true;
    	} else if (args[0].equalsIgnoreCase("off")) {
    		resident.getTownOrNull().addMetaData(new BooleanDataField("DeadlyDisasters", false));
            TownyMessaging.sendPrefixedTownMessage(resident.getTownOrNull(), "Disaster Protection has been Disabled.");
            return true;
    	} else {
    		sender.sendMessage(Utils.chat("&c/towndisasters <on|off> [player]"));
			return true;
    	}
    }
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		final List<String> list = new ArrayList<>();
		if (!(sender.hasPermission("deadlydisasters.towny")))
			return list;
		if (args.length == 1) {
			StringUtil.copyPartialMatches(args[0], Arrays.asList("on","off"), list);
			Collections.sort(list);
			return list;
		} else if (args.length == 2) {
			StringUtil.copyPartialMatches(args[1], Bukkit.getServer().getOnlinePlayers().stream().map(p -> p.getName()).collect(Collectors.toList()), list);
			Collections.sort(list);
			return list;
		}
		return list;
	}
}