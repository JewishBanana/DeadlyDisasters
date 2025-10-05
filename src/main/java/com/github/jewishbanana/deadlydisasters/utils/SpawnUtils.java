package com.github.jewishbanana.deadlydisasters.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SpawnUtils {
	
	public static final double MIN_SPAWN_DISTANCE_FROM_PLAYERS = 24.0;
	public static final double MAX_SPAWN_DISTANCE_FROM_PLAYERS = 70.0;

	public static Location findSpawnLocation(Location area, int height, double minDistance, double maxDistance) {
		for (int i=0; i < 5; i++) {
			Location spawn = Utils.findRandomSpotInRadius(area, minDistance, maxDistance, height, 3, () -> Utils.getRandomizedVector(1.0, 0.25, 1.0));
			if (canMonsterSpawn(spawn, minDistance))
				return spawn;
		}
		return null;
	}
	public static Location findSpawnLocation(Location area, int height) {
		return findSpawnLocation(area, height, MIN_SPAWN_DISTANCE_FROM_PLAYERS, MAX_SPAWN_DISTANCE_FROM_PLAYERS);
	}
	public static Location findSpawnLocation(Location area) {
		return findSpawnLocation(area, 2, MIN_SPAWN_DISTANCE_FROM_PLAYERS, MAX_SPAWN_DISTANCE_FROM_PLAYERS);
	}
	public static Location findSpawnLocationNoCollision(Location area, int height, double minDistance, double maxDistance) {
		for (int i=0; i < 10; i++) {
			Location spawn = area.clone().add(Utils.getRandomizedVector(1.0, 0.25, 1.0).multiply(Utils.getRandomGenerator().nextDouble(minDistance, maxDistance)));
			if (canMonsterSpawn(spawn, minDistance))
				return spawn;
		}
		return null;
	}
	public static boolean canMonsterSpawn(Location location, double minDistance) {
		return location != null
				&& !location.getBlock().isLiquid()
				&& location.getBlock().getLightFromBlocks() == 0
				&& location.getWorld().getNearbyEntities(location, minDistance, minDistance, minDistance, e -> e instanceof Player).size() == 0;
	}
}
