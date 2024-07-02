package com.github.jewishbanana.deadlydisasters.listeners;

import java.util.Random;

import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.handlers.ItemsHandler;

public class LootGenerateListener implements Listener {
	
	private Random rand = new Random();
	
	public LootGenerateListener(Main plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler
	public void onLootGen(LootGenerateEvent e) {
		if (e.getInventoryHolder() instanceof Chest) {
			if (rand.nextDouble()*100 < ItemsHandler.basicBookSpawnrate) {
				e.getLoot().add(ItemsHandler.basicBook);
			}
		}
	}
}
