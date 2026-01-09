package com.github.jewishbanana.deadlydisasters.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public class SpawnUtils {
	
	public static final float MIN_SPAWN_DISTANCE_FROM_PLAYERS = 24f;
	public static final float MAX_SPAWN_DISTANCE_FROM_PLAYERS = 70f;

	public static Location findMonsterSpawnLocation(Location area, int height, float minDistance, float maxDistance) {
		for (int i=0; i < 5; i++) {
			Location spawn = Utils.findRandomSpotInRadius(area, minDistance, maxDistance, height, 3, () -> Utils.getRandomizedVector(1f, 0.25f, 1f));
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
			Location spawn = area.clone().add(Utils.getRandomizedVector(1f, 0.25f, 1f).multiply(Utils.getRandomGenerator().nextDouble(minDistance, maxDistance)));
			if (canMonsterSpawn(spawn, minDistance))
				return spawn;
		}
		return null;
	}
	public static boolean canMonsterSpawn(Location location, double minDistance) {
		if (location == null)
			return false;
		final Block block = location.getBlock();
		if (block != null && (block.isLiquid() || block.getLightFromBlocks() != 0 || Utils.isNotNullAndCondition(block.getRelative(BlockFace.DOWN), t -> t.isPassable())))
			return false;
		return location.getWorld().getNearbyEntities(location, minDistance, minDistance, minDistance, e -> e instanceof Player).size() == 0;
	}
	public static Location findSmartYSpawn(Location pivot, Location spawn, double height, int maxDistance) {
	    if (pivot == null || spawn == null)
	        return null;
	    final World world = spawn.getWorld();
	    final int spawnX = spawn.getBlockX();
	    final int spawnZ = spawn.getBlockZ();
	    final int spawnY = spawn.getBlockY();
	    final int heightInt = (int)height;
	    final double pivotY = pivot.getY();
	    Location loc1 = null;
	    Location loc2 = null;
	    down:
	    for (int y = spawnY; y > spawnY - maxDistance; y--) {
	        final Block current = world.getBlockAt(spawnX, y, spawnZ);
	        if (!current.isPassable()) {
	            final Block above = world.getBlockAt(spawnX, y + 1, spawnZ);
	            if (above.isPassable() && !above.isLiquid()) {
	                for (int c = 2; c <= heightInt - 1; c++) {
	                    if (!world.getBlockAt(spawnX, y + c, spawnZ).isPassable())
	                        continue down;
	                }
	                loc1 = new Location(world, spawnX + 0.5, y + 1.01, spawnZ + 0.5);
	                break;
	            }
	        }
	    }
	    up:
	    for (int y = spawnY; y < spawnY + maxDistance; y++) {
	        final Block current = world.getBlockAt(spawnX, y, spawnZ);
	        if (current.isPassable() && !current.isLiquid()) {
	            final Block below = world.getBlockAt(spawnX, y - 1, spawnZ);
	            if (!below.isPassable()) {
	                for (int c = 1; c < heightInt; c++) {
	                    if (!world.getBlockAt(spawnX, y + c, spawnZ).isPassable())
	                        continue up;
	                }
	                loc2 = new Location(world, spawnX + 0.5, y + 0.01, spawnZ + 0.5);
	                break;
	            }
	        }
	    }
	    if (loc1 != null && loc2 == null)
	        return loc1;
	    if (loc1 == null)
	        return loc2;
	    final double dist1 = Math.abs(pivotY - loc1.getY());
	    final double dist2 = Math.abs(pivotY - loc2.getY());
	    return dist2 < dist1 ? loc2 : loc1;
	}
}
