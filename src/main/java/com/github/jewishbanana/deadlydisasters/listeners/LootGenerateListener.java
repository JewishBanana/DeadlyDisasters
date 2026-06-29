package com.github.jewishbanana.deadlydisasters.listeners;

import java.util.Objects;
import java.util.random.RandomGenerator;

import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;

import com.github.jewishbanana.deadlydisasters.DeadlyDisasters;
import com.github.jewishbanana.deadlydisasters.items.BasicCoatingBook;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class LootGenerateListener implements Listener {
	
	private RandomGenerator rand;
	private static float basicBookLootTableChance;
	
	public LootGenerateListener(DeadlyDisasters plugin) {
		this.rand = Utils.getRandomGenerator();
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onLootGen(LootGenerateEvent event) {
		if (!(event.getInventoryHolder() instanceof Chest) || event.getLoot() == null)
			return;
		if (event.getLoot().stream().filter(Objects::nonNull).anyMatch(item -> item.getType() == Material.ENCHANTED_BOOK)
				&& rand.nextFloat() < basicBookLootTableChance) {
			com.github.jewishbanana.uiframework.items.UIItemType type = com.github.jewishbanana.uiframework.items.UIItemType.getItemType(BasicCoatingBook.REGISTERED_KEY);
			event.getLoot().add(type.createNewInstance(type.getItem()).getItem());
		}
	}
	public static void reload() {
		basicBookLootTableChance = (float) DataUtils.getMainConfigDouble("items.basic_coating_book.loot_table_chance");
	}
}
