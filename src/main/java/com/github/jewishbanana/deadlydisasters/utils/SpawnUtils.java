package com.github.jewishbanana.deadlydisasters.utils;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public class SpawnUtils {
	
	public static final double MIN_SPAWN_DISTANCE_FROM_PLAYERS = 24.0;
	public static final double MAX_SPAWN_DISTANCE_FROM_PLAYERS = 70.0;

	public static Location findMonsterSpawnLocation(Location area, int height, double minDistance, double maxDistance) {
		for (int i=0; i < 5; i++) {
			Location spawn = Utils.findRandomSpotInRadius(area, minDistance, maxDistance, height, 3, () -> Utils.getRandomizedVector(1.0, 0.25, 1.0));
			if (canMonsterSpawn(spawn, minDistance))
				return spawn;
		}
		return null;
	}
	public static Location findMonsterSpawnLocation(Location area, int height) {
		return findMonsterSpawnLocation(area, height, MIN_SPAWN_DISTANCE_FROM_PLAYERS, MAX_SPAWN_DISTANCE_FROM_PLAYERS);
	}
	public static Location findMonsterSpawnLocation(Location area) {
		return findMonsterSpawnLocation(area, 2, MIN_SPAWN_DISTANCE_FROM_PLAYERS, MAX_SPAWN_DISTANCE_FROM_PLAYERS);
	}
	public static Location findMonsterSpawnLocationNoCollision(Location area, int height, double minDistance, double maxDistance) {
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
	public static Location findSmartYSpawn(Location pivot, Location spawn, double height, int maxDistance) {
		if (pivot == null || spawn == null)
			return null;
		Block b = spawn.getBlock();
		Location loc1 = null, loc2 = null;
		down:
			for (int i = spawn.getBlockY(); i > spawn.getBlockY()-maxDistance; i--) {
				b = b.getRelative(BlockFace.DOWN);
				if (!b.isPassable() && b.getRelative(BlockFace.UP).isPassable() && !b.getRelative(BlockFace.UP).isLiquid()) {
					for (int c = 2; c <= height-1; c++)
						if (!b.getRelative(BlockFace.UP, c).isPassable())
							continue down;
					loc1 = b.getRelative(BlockFace.UP).getLocation().add(0.5,0.01,0.5);
					break down;
				}
			}
		b = spawn.getBlock();
		up:
			for (int i = spawn.getBlockY(); i < spawn.getBlockY()+maxDistance; i++) {
				b = b.getRelative(BlockFace.UP);
				if (b.isPassable() && !b.getRelative(BlockFace.DOWN).isPassable() && !b.isLiquid()) {
					for (int c = 1; c < height; c++)
						if (!b.getRelative(BlockFace.UP, c).isPassable())
							continue up;
					loc2 = b.getLocation().add(0.5,0.01,0.5);
					break up;
				}
			}
		if (loc1 != null && loc2 == null)
			return loc1;
		else if (loc1 == null && loc2 != null)
			return loc2;
		else if (loc1 == null && loc2 == null)
			return null;
		if (Math.abs(pivot.getY()-loc2.getY()) < Math.abs(pivot.getY()-loc1.getY()))
			return loc1;
		else
			return loc2;
	}
}
