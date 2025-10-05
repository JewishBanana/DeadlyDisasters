package com.github.jewishbanana.deadlydisasters.listeners;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldInitEvent;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.WorldWrapper;
import com.github.jewishbanana.deadlydisasters.disasters.Disaster;
import com.github.jewishbanana.deadlydisasters.disasters.WeatherDisaster;
import com.mojang.datafixers.util.Pair;

public class WorldListener implements Listener {
	
	private static final Map<WeatherDisaster, Queue<Pair<Integer, Integer>>> chunkListeners;
	static {
		chunkListeners = new ConcurrentHashMap<>();
	}
	
	public WorldListener(Main plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler
	public void onWorldInit(WorldInitEvent event) {
		if (WorldWrapper.getWorldWrapper(event.getWorld()) == null)
			WorldWrapper.initWorld(event.getWorld());
	}
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		final int chunkX = event.getChunk().getX();
		final int chunkZ = event.getChunk().getZ();
		chunkListeners.forEach((disaster, queue) -> {
			Iterator<Pair<Integer, Integer>> it = queue.iterator();
			while (it.hasNext()) {
				Pair<Integer, Integer> pair = it.next();
				if (pair.getFirst() == chunkX && pair.getSecond() == chunkZ) {
					disaster.involvedChunks.add(event.getChunk());
					it.remove();
					break;
				}
			}
		});
	}
	public static void addChunkListener(WeatherDisaster disaster, int chunkX, int chunkZ) {
		chunkListeners.computeIfAbsent(disaster, k -> new ConcurrentLinkedQueue<>()).add(Pair.of(chunkX, chunkZ));
	}
	public static void removeChunkListener(Disaster disaster) {
		chunkListeners.remove(disaster);
	}
}
