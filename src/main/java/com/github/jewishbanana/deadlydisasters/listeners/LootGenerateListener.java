package com.github.jewishbanana.deadlydisasters.listeners;

import java.util.Random;

import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.items.BasicCoatingBook;

public class LootGenerateListener implements Listener {
	
	private Random rand = new Random();
	private static double basicBookLootChance;
	
	public LootGenerateListener(Main plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		reload(plugin);
	}
	@EventHandler
	public void onLootGen(LootGenerateEvent e) {
		if (e.getInventoryHolder() instanceof Chest) {
			if (rand.nextDouble()*100 < basicBookLootChance) {
				e.getLoot().add(com.github.jewishbanana.uiframework.items.ItemType.getItemType(BasicCoatingBook.REGISTERED_KEY).getBuilder().getItem());
			}
		}
	}
	public static void reload(Main plugin) {
		basicBookLootChance = plugin.getConfig().getDouble("customitems.items.basic_coating_book.chest_spawn_rate");
	}
}
