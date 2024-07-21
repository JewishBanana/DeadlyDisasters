package com.github.jewishbanana.deadlydisasters.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.palmergames.bukkit.towny.event.NewTownEvent;
import com.palmergames.bukkit.towny.object.metadata.BooleanDataField;

public class TownyListener implements Listener {
	
	public TownyListener(Main plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler
	public void onTownCreate(NewTownEvent event){
		event.getTown().addMetaData(new BooleanDataField("DeadlyDisasters", true));
	}
	public static void registerTowns() {
		for (com.palmergames.bukkit.towny.object.Town town: Utils.getTownyAPI().getTowns())
			if (!((com.palmergames.bukkit.towny.object.Town) town).hasMeta("DeadlyDisasters"))
				((com.palmergames.bukkit.towny.object.Town) town).addMetaData(new com.palmergames.bukkit.towny.object.metadata.BooleanDataField("DeadlyDisasters", true));
	}
}