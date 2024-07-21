package com.github.jewishbanana.deadlydisasters.events;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class DisasterEvent {
	
	public Disaster type;
	public int level;
	public World world;
	public FileConfiguration configFile;
	
	public static Queue<DisasterEvent> ongoingDisasters = new ArrayDeque<>();
	public static Map<UUID,Map<DisasterEvent,Integer>> countdownMap = new HashMap<>();
	
	public void inputPlayerToMap(int seconds, Player p) {
		if (!countdownMap.containsKey(p.getUniqueId()))
			countdownMap.put(p.getUniqueId(), new HashMap<>());
		countdownMap.get(p.getUniqueId()).put(this, seconds);
	}
}
