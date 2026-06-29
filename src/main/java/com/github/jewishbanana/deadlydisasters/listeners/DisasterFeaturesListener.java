package com.github.jewishbanana.deadlydisasters.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;

import com.github.jewishbanana.deadlydisasters.DeadlyDisasters;
import com.github.jewishbanana.deadlydisasters.disasters.weather.AcidStorm;

public class DisasterFeaturesListener implements Listener {
	
	public DisasterFeaturesListener(DeadlyDisasters plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (AcidStorm.poisonedCrops.remove(event.getBlock()) != null)
			event.setDropItems(false);
	}
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBlockGrow(BlockGrowEvent event) {
		if (AcidStorm.poisonedCrops.containsKey(event.getBlock())) {
			event.setCancelled(true);
		}
	}
}
