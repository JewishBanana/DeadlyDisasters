package deadlydisasters.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.metadata.BooleanDataField;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

//Used for Towny Specific things
public class Towny implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Resident resident = TownyAPI.getInstance().getResident(sender.getName());
        if (resident.hasTown()) {
            if (args[0].equalsIgnoreCase("on") || args.length == 0) {
                resident.getTownOrNull().addMetaData(new BooleanDataField("DeadlyDisasters", true));
                TownyMessaging.sendPrefixedTownMessage(resident.getTownOrNull(), "Disaster Protection has been Enabled.");
            } else if (args[0].equalsIgnoreCase("off")) {
                resident.getTownOrNull().addMetaData(new BooleanDataField("DeadlyDisasters", false));
                TownyMessaging.sendPrefixedTownMessage(resident.getTownOrNull(), "Disaster Protection has been Disabled.");
            }
            return true;
        }
    return true;}
}
