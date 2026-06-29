package com.github.jewishbanana.deadlydisasters.utils;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.DyeColor;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.DeadlyDisasters;

public class Utils {
	
	private static final JavaPlugin plugin;
	private static final RandomGenerator random;
	
	public static final String prefix;
	public static final String symbolLine;
	
	private static final boolean sendErrors;
	private static final DecimalFormat decimalFormat;
	private static final Pattern hexPattern;
	private static final Map<DyeColor, ChatColor> dyeChatMap;
	static {
		plugin = DeadlyDisasters.getInstance();
		random = RandomGenerator.of("SplittableRandom");
		hexPattern = Pattern.compile("\\(hex:#[a-fA-F0-9]{6}\\)");
		prefix = convertString("&a[DeadlyDisasters]: ");
		sendErrors = DataUtils.getMainConfigBoolean("general.debug_messages");
		decimalFormat = new DecimalFormat("0.0");
		
		StringBuilder builder = new StringBuilder();
		for (int i=0; i < 17; i++)
			builder.append('=');
		symbolLine = builder.toString();
		
		dyeChatMap = new HashMap<>();
		dyeChatMap.put(DyeColor.BLACK, ChatColor.BLACK);
		dyeChatMap.put(DyeColor.BLUE, ChatColor.DARK_BLUE);
		dyeChatMap.put(DyeColor.BROWN, ChatColor.GOLD);
		dyeChatMap.put(DyeColor.CYAN, ChatColor.AQUA);
		dyeChatMap.put(DyeColor.GRAY, ChatColor.DARK_GRAY);
		dyeChatMap.put(DyeColor.GREEN, ChatColor.DARK_GREEN);
		dyeChatMap.put(DyeColor.LIGHT_BLUE, ChatColor.BLUE);
		dyeChatMap.put(DyeColor.LIGHT_GRAY, ChatColor.GRAY);
		dyeChatMap.put(DyeColor.LIME, ChatColor.GREEN);
		dyeChatMap.put(DyeColor.MAGENTA, ChatColor.LIGHT_PURPLE);
		dyeChatMap.put(DyeColor.ORANGE, ChatColor.GOLD);
		dyeChatMap.put(DyeColor.PINK, ChatColor.LIGHT_PURPLE);
		dyeChatMap.put(DyeColor.PURPLE, ChatColor.DARK_PURPLE);
		dyeChatMap.put(DyeColor.RED, ChatColor.DARK_RED);
		dyeChatMap.put(DyeColor.WHITE, ChatColor.WHITE);
		dyeChatMap.put(DyeColor.YELLOW, ChatColor.YELLOW);
	}
	
	public static String convertString(String text) {
		if (text == null)
			return null;
		String s = text;
		Matcher match = hexPattern.matcher(s);
		if (DependencyUtils.isSpigotServer()) {
		    while (match.find()) {
		        String color = s.substring(match.start(), match.end());
		        s = s.replace(color, net.md_5.bungee.api.ChatColor.of(color.substring(5, color.length()-1))+"");
		        match = hexPattern.matcher(s);
		    }
		    return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', s);
		}
	    while (match.find()) {
	        String color = s.substring(match.start(), match.end());
	        Color col = Color.decode(color);
	        s = s.replace(color, dyeChatMap.getOrDefault(DyeColor.getByColor(org.bukkit.Color.fromRGB(col.getRed(), col.getGreen(), col.getBlue())), ChatColor.WHITE)+"");
	        match = hexPattern.matcher(s);
	    }
	    return ChatColor.translateAlternateColorCodes('&', s);
	}
	public static Vector getVectorTowards(Location initial, Location towards) {
	    final float dx = (float)(towards.getX() - initial.getX());
	    final float dy = (float)(towards.getY() - initial.getY());
	    final float dz = (float)(towards.getZ() - initial.getZ());
	    final float lengthSquared = dx * dx + dy * dy + dz * dz;
	    if (lengthSquared == 0.0f)
	        return new Vector(0, 0, 0);
	    final float invLength = fastInverseSqrt(lengthSquared);
	    return new Vector(dx * invLength, dy * invLength, dz * invLength);
	}
	public static float fastInverseSqrt(float x) {
	    final float halfX = 0.5f * x;
	    int i = Float.floatToRawIntBits(x);
	    i = 0x5f3759df - (i >> 1);
	    float y = Float.intBitsToFloat(i);
	    y = y * (1.5f - halfX * y * y);
	    return y;
	}
	public static Vector getRandomizedVector(float xWeight, float yWeight, float zWeight) {
	    final float x = xWeight == 0 ? 0.0f : random.nextFloat(-xWeight, xWeight);
	    final float y = yWeight == 0 ? 0.0f : random.nextFloat(-yWeight, yWeight);
	    final float z = zWeight == 0 ? 0.0f : random.nextFloat(-zWeight, zWeight);
	    final float lengthSquared = x * x + y * y + z * z;
	    if (lengthSquared == 0.0f)
	        return new Vector(0, 0, 0);
	    final float invLength = fastInverseSqrt(lengthSquared);
	    return new Vector(x * invLength, y * invLength, z * invLength);
	}
	public static Vector getRandomizedVector() {
	    final float x = random.nextFloat(-1.0f, 1.0f);
	    final float y = random.nextFloat(-1.0f, 1.0f);
	    final float z = random.nextFloat(-1.0f, 1.0f);
	    final float lengthSquared = x * x + y * y + z * z;
	    if (lengthSquared == 0.0f)
	        return new Vector(0, 0, 0);
	    final float invLength = fastInverseSqrt(lengthSquared);
	    return new Vector(x * invLength, y * invLength, z * invLength);
	}
	public static void copyUrlToFile(URL url, File destination) throws IOException {
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:10.0.2) Gecko/20100101 Firefox/10.0.2");
		connection.connect();
		FileUtils.copyInputStreamToFile(connection.getInputStream(), destination);
	}
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue());
        Map<K, V> result = new LinkedHashMap<>();
        for (Entry<K, V> entry : list)
            result.put(entry.getKey(), entry.getValue());
        return result;
    }
	public static <K, V> Map<K, V> reverseMap(Map<K, V> map) {
		List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
		Collections.reverse(list);
		Map<K, V> newMap = new LinkedHashMap<>();
		for (Entry<K, V> entry : list)
			newMap.put(entry.getKey(), entry.getValue());
		return newMap;
	}
	public static void runConsoleCommand(String command, World world) {
		Entity entity = world.spawn(new Location(world, 0, 0, 0), CommandMinecart.class);
		World tempWorld = Bukkit.getWorlds().get(0);
		boolean gameRule = tempWorld.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK);
		tempWorld.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
		plugin.getServer().dispatchCommand(entity, command);
		tempWorld.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, gameRule);
		entity.remove();
	}
	public static void mergeEntityData(Entity entity, String data) {
		Location entityLoc = entity.getLocation();
		runConsoleCommand("data merge entity @e[x="+entityLoc.getX()+",y="+entityLoc.getY()+",z="+entityLoc.getZ()+",distance=..0.1,limit=1] "+data, entity.getWorld());
	}
	public static void damageItem(ItemStack toDamage, int damage) {
		ItemMeta meta = toDamage.getItemMeta();
		if (!(meta instanceof Damageable damageable))
			return;
		damageable.setDamage(damageable.getDamage() + damage);
		if (damageable.getDamage() >= toDamage.getType().getMaxDurability()) {
			toDamage.setAmount(0);
			return;
		}
		toDamage.setItemMeta(meta);
	}
	public static void repairItem(ItemStack toRepair, int health) {
		ItemMeta meta = toRepair.getItemMeta();
		if (!(meta instanceof Damageable damageable))
			return;
		damageable.setDamage(Math.max(damageable.getDamage() - health, 0));
		toRepair.setItemMeta(meta);
	}
	public static Location findRandomSpotInRadius(Location initial, float minDist, float maxDist, int height, int attempts, Supplier<Vector> vector, Predicate<Location> conditions) {
		final double squaredMin = minDist * minDist;
		final World world = initial.getWorld();
		final double initialX = initial.getX();
		final double initialY = initial.getY();
	    final double initialZ = initial.getZ();
	    final int verticalRange = (int) maxDist;
	    for (int i = 0; i < attempts; i++) {
	        final float distance = random.nextFloat(minDist, maxDist);
	        final Vector dir = vector.get();
	        final double offsetX = dir.getX() * distance;
	        final double offsetZ = dir.getZ() * distance;
	        final Location searchLoc = new Location(world, initialX + offsetX, initialY, initialZ + offsetZ);
	        final Location temp = SpawnUtils.findSmartYSpawn(initial, searchLoc, height, verticalRange);
	        if (temp != null) {
	            final double dx = temp.getX() - initialX;
	            final double dz = temp.getZ() - initialZ;
	            final double distSquared = dx * dx + dz * dz;
	            if (distSquared >= squaredMin && conditions.test(temp))
	                return temp;
	        }
	    }
	    return null;
	}
	public static Location findRandomSpotInRadius(Location initial, float minDist, float maxDist, int height, int attempts, Supplier<Vector> vector) {
		return findRandomSpotInRadius(initial, minDist, maxDist, height, attempts, vector, test -> true);
	}
	public static Location findRandomSpotInRadius(Location initial, float minDist, float maxDist, int height, int attempts) {
		return findRandomSpotInRadius(initial, minDist, maxDist, height, attempts, () -> getRandomizedVector());
	}
	public static Location findRandomSpotInCircle(Location initial, float minDist, float maxDist, int attempts, Predicate<Location> conditions) {
	    final World world = initial.getWorld();
	    final double initialX = initial.getX();
	    final double initialY = initial.getY();
	    final double initialZ = initial.getZ();
	    for (int i = 0; i < attempts; i++) {
	        final float distance = random.nextFloat(minDist, maxDist);
	        final Vector dir = getRandomizedVector(1f, 0f, 1f);
	        final double x = initialX + dir.getX() * distance;
	        final double y = initialY + dir.getY() * distance;
	        final double z = initialZ + dir.getZ() * distance;
	        final Location temp = new Location(world, x, y, z);
	        if (conditions.test(temp))
	            return temp;
	    }
	    return null;
	}
	public static Location findRandomSpotInCircle(Location initial, float minDist, float maxDist) {
		return initial.clone().add(getRandomizedVector(1f, 0f, 1f).multiply(random.nextFloat(minDist, maxDist)));
	}
	public static Set<Chunk> getChunksInRadius(Location location, float radius) {
		final float radiusSquared = radius * radius;
		final int chunkX = location.getChunk().getX();
		final int chunkZ = location.getChunk().getZ();
		final int chunkRadius = (int) Math.ceil(radius / 16.0);
		final Set<Chunk> chunks = new HashSet<>();
		for (int x = -chunkRadius; x <= chunkRadius; x++)
			for (int z = -chunkRadius; z <= chunkRadius; z++) {
				int currentX = chunkX + x;
				int currentZ = chunkZ + z;
				float deltaX = location.getBlockX() - (Utils.clamp(location.getBlockX(), currentX * 16, (currentX + 1) * 16 - 1));
				float deltaZ = location.getBlockZ() - (Utils.clamp(location.getBlockZ(), currentZ * 16, (currentZ + 1) * 16 - 1));
				if (deltaX * deltaX + deltaZ * deltaZ <= radiusSquared)
					chunks.add(location.getWorld().getChunkAt(currentX, currentZ));
			}
		return chunks;
	}
	public static Set<Block> getRandomSurfaceBlocksInArea(Location location, int amount, int range) {
		Set<Block> set = new HashSet<>();
		for (int i=0; i < amount; i++)
			set.add(location.getWorld().getHighestBlockAt(location.clone().add(random.nextInt(-range, range), 0, random.nextInt(-range, range))));
		return set;
	}
	public static boolean isAreaFlatGrounded(Location location) {
		int count = 0;
		for (int i=0; i < 15; i++) {
			Location temp = location.clone().add(getRandomizedVector(1, 0, 1).multiply(random.nextFloat(1, 12)));
			if (BlockUtils.getHighestExposedBlock(temp.getBlock(), 12) != null)
				if (++count == 12)
					return true;
		}
		return count >= 12;
	}
	public static enum AreaClearing {
		
		CUBE_3X3_FROM_CENTER(block -> {
			final World world = block.getWorld();
			for (int x = block.getX() - 1; x <= block.getX() + 1; x++)
				for (int y = block.getY() - 1; y <= block.getY() + 1; y++)
					for (int z = block.getZ() - 1; z <= block.getZ() + 1; z++)
						if (!world.getBlockAt(x, y, z).isPassable())
							return false;
			return true;
		}),
		CUBE_3X3_FROM_CENTER_BOTTOM(block -> {
			final World world = block.getWorld();
			for (int x = block.getX() - 1; x <= block.getX() + 1; x++)
				for (int y = block.getY(); y <= block.getY() + 2; y++)
					for (int z = block.getZ() - 1; z <= block.getZ() + 1; z++)
						if (!world.getBlockAt(x, y, z).isPassable())
							return false;
			return true;
		}),
		CUBE_3X3_FROM_CENTER_TOP(block -> {
			final World world = block.getWorld();
			for (int x = block.getX() - 1; x <= block.getX() + 1; x++)
				for (int y = block.getY() - 2; y <= block.getY(); y++)
					for (int z = block.getZ() - 1; z <= block.getZ() + 1; z++)
						if (!world.getBlockAt(x, y, z).isPassable())
							return false;
			return true;
		}),
		PLUS_SIGN_3D_FROM_CENTER(block -> {
			for (BlockFace face : Set.of(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST))
				if (!block.getRelative(face).isPassable())
					return false;
			return true;
		});
		
		private Function<Block, Boolean> function;
		
		private AreaClearing(Function<Block, Boolean> function) {
			this.function = function;
		}
	}
	public static boolean isAreaClear(Block block, AreaClearing clearing) {
		return clearing.function.apply(block);
	}
	public static boolean isAreaClear(Location location, float radius) {
		for (Block b : BlockUtils.getBlocksInSphereRadius(location, radius))
			if (!b.isPassable())
				return false;
		return true;
	}
	public static boolean isAreaClear(Location location, float radius, float height) {
		for (Block b : BlockUtils.getBlocksInCylinderRadius(location, radius, height))
			if (!b.isPassable())
				return false;
		return true;
	}
	public static boolean isLocationExposedToOutdoors(Location location, float testRange, int horizontalCasts) {
	    final World world = location.getWorld();
	    final float x = (float) location.getX();
	    final float y = (float) location.getY();
	    final float z = (float) location.getZ();
	    final float angleStep = (float) (2 * Math.PI / horizontalCasts); // Distribute evenly around 360°
	    final float invStepSize = 1f / 0.9f;
	    final int maxSteps = (int) (testRange * invStepSize);
	    
	    // Scale accuracy threshold based on number of casts
	    // Original: 12 horizontal × 3 vertical = 36 total, threshold 78
	    // Formula: (horizontalCasts * 3) * (78.0 / 36.0) ≈ horizontalCasts * 6.5
	    final int accuracyThreshold = (int) (horizontalCasts * 3 * 2.17);
	    
	    int accuracy = 0;
	    for (int i = 0; i < horizontalCasts; i++) {
	        final float radians = i * angleStep;
	        final float cosR = (float) Math.cos(radians);
	        final float sinR = (float) Math.sin(radians);
	        for (int j = 0; j < 3; j++) {
	            final float angleY = -0.5f + (j * 0.5f);
	            final float dx = (cosR + angleY) * 0.9f;
	            final float dy = angleY * 0.9f;
	            final float dz = (sinR + angleY) * 0.9f;
	            float locX = x + dx;
	            float locY = y + dy;
	            float locZ = z + dz;
	            boolean flag = false;
	            for (int step = 0; step < maxSteps; step++) {
	                final int blockX = (int) Math.floor(locX);
	                final int blockY = (int) Math.floor(locY);
	                final int blockZ = (int) Math.floor(locZ);
	                if (!world.getBlockAt(blockX, blockY, blockZ).isPassable()) {
	                    if (world.getHighestBlockYAt(blockX, blockZ) != blockY)
	                        accuracy++;
	                    else
	                        flag = true;
	                    break;
	                }
	                locX += dx;
	                locY += dy;
	                locZ += dz;
	            }
	            if (!flag) {
	                final int finalBlockX = (int) Math.floor(locX);
	                final int finalBlockZ = (int) Math.floor(locZ);
	                final int finalBlockY = (int) Math.floor(locY);
	                if (world.getHighestBlockYAt(finalBlockX, finalBlockZ) > finalBlockY)
	                    accuracy += 2;
	            }
	            if (accuracy >= accuracyThreshold)
	                return false;
	        }
	    }
	    return true;
	}
	public static boolean isLocationExposedToOutdoors(Location location, float testRange) {
	    return isLocationExposedToOutdoors(location, testRange, 12);
	}
	public static boolean isLocationExposedToOutdoors(Location location) {
	    return isLocationExposedToOutdoors(location, 12f, 12);
	}
	private static final Map<Long, Boolean> passableCache = new ConcurrentHashMap<>();
	private static final Map<Long, Integer> highestBlockCache = new ConcurrentHashMap<>();
	static {
		new BukkitRunnable() {
			@Override
			public void run() {
				clearCaches();
			}
		}.runTaskTimerAsynchronously(plugin, 0, 60);
	}
	public static boolean isLocationExposedToOutdoorsOptimized(Location location, float testRange, int horizontalCasts) {
	    final World world = location.getWorld();
	    final float x = (float) location.getX();
	    final float y = (float) location.getY();
	    final float z = (float) location.getZ();
	    final float angleStep = (float) (2 * Math.PI / horizontalCasts);
	    final float invStepSize = 1.111f;
	    final int maxSteps = (int) (testRange * invStepSize);
	    final int accuracyThreshold = (int) (horizontalCasts * 3 * 2.17);
	    int accuracy = 0;
	    for (int i = 0; i < horizontalCasts; i++) {
	        final float radians = i * angleStep;
	        final float cosR = (float) Math.cos(radians);
	        final float sinR = (float) Math.sin(radians);
	        for (int j = 0; j < 3; j++) {
	            final float angleY = -0.5f + (j * 0.5f);
	            final float dx = (cosR + angleY) * 0.9f;
	            final float dy = angleY * 0.9f;
	            final float dz = (sinR + angleY) * 0.9f;
	            float locX = x + dx;
	            float locY = y + dy;
	            float locZ = z + dz;
	            boolean flag = false;
	            int lastBlockX = Integer.MIN_VALUE;
	            int lastBlockY = Integer.MIN_VALUE;
	            int lastBlockZ = Integer.MIN_VALUE;
	            for (int step = 0; step < maxSteps; step++) {
	                final int blockX = (int) Math.floor(locX);
	                final int blockY = (int) Math.floor(locY);
	                final int blockZ = (int) Math.floor(locZ);
	                if (blockX == lastBlockX && blockY == lastBlockY && blockZ == lastBlockZ) {
	                    locX += dx;
	                    locY += dy;
	                    locZ += dz;
	                    continue;
	                }
	                lastBlockX = blockX;
	                lastBlockY = blockY;
	                lastBlockZ = blockZ;
	                final long blockKey = blockKey(blockX, blockY, blockZ);
	                final boolean passable = passableCache.computeIfAbsent(blockKey, 
	                    k -> world.getBlockAt(blockX, blockY, blockZ).isPassable());
	                if (!passable) {
	                    long xzKey = xzKey(blockX, blockZ);
	                    int highestY = highestBlockCache.computeIfAbsent(xzKey, 
	                        k -> world.getHighestBlockYAt(blockX, blockZ));
	                    if (highestY != blockY)
	                        accuracy++;
	                    else
	                        flag = true;
	                    break;
	                }
	                locX += dx;
	                locY += dy;
	                locZ += dz;
	            }
	            if (!flag) {
	                final int finalBlockX = (int) Math.floor(locX);
	                final int finalBlockZ = (int) Math.floor(locZ);
	                final int finalBlockY = (int) Math.floor(locY);
	                final long xzKey = xzKey(finalBlockX, finalBlockZ);
	                final int highestY = highestBlockCache.computeIfAbsent(xzKey, 
	                    k -> world.getHighestBlockYAt(finalBlockX, finalBlockZ));
	                if (highestY > finalBlockY)
	                    accuracy += 2;
	            }
	            if (accuracy >= accuracyThreshold)
	                return false;
	        }
	    }
	    return true;
	}
	/**
	 * Graded variant of {@link #isLocationExposedToOutdoorsOptimized}: instead of a boolean it returns how exposed a
	 * location is, from {@code 0.0} (fully enclosed) to {@code 1.0} (fully open). Always runs every cast so callers can
	 * scale effects by partial cover.
	 */
	public static float getOutdoorExposureScore(Location location, float testRange, int horizontalCasts) {
	    final World world = location.getWorld();
	    final float x = (float) location.getX();
	    final float y = (float) location.getY();
	    final float z = (float) location.getZ();
	    final float angleStep = (float) (2 * Math.PI / horizontalCasts);
	    final float invStepSize = 1.111f;
	    final int maxSteps = (int) (testRange * invStepSize);
	    final int accuracyThreshold = (int) (horizontalCasts * 3 * 2.17);
	    int accuracy = 0;
	    for (int i = 0; i < horizontalCasts; i++) {
	        final float radians = i * angleStep;
	        final float cosR = (float) Math.cos(radians);
	        final float sinR = (float) Math.sin(radians);
	        for (int j = 0; j < 3; j++) {
	            final float angleY = -0.5f + (j * 0.5f);
	            final float dx = (cosR + angleY) * 0.9f;
	            final float dy = angleY * 0.9f;
	            final float dz = (sinR + angleY) * 0.9f;
	            float locX = x + dx;
	            float locY = y + dy;
	            float locZ = z + dz;
	            boolean flag = false;
	            int lastBlockX = Integer.MIN_VALUE;
	            int lastBlockY = Integer.MIN_VALUE;
	            int lastBlockZ = Integer.MIN_VALUE;
	            for (int step = 0; step < maxSteps; step++) {
	                final int blockX = (int) Math.floor(locX);
	                final int blockY = (int) Math.floor(locY);
	                final int blockZ = (int) Math.floor(locZ);
	                if (blockX == lastBlockX && blockY == lastBlockY && blockZ == lastBlockZ) {
	                    locX += dx;
	                    locY += dy;
	                    locZ += dz;
	                    continue;
	                }
	                lastBlockX = blockX;
	                lastBlockY = blockY;
	                lastBlockZ = blockZ;
	                final long blockKey = blockKey(blockX, blockY, blockZ);
	                final boolean passable = passableCache.computeIfAbsent(blockKey,
	                    k -> world.getBlockAt(blockX, blockY, blockZ).isPassable());
	                if (!passable) {
	                    long xzKey = xzKey(blockX, blockZ);
	                    int highestY = highestBlockCache.computeIfAbsent(xzKey,
	                        k -> world.getHighestBlockYAt(blockX, blockZ));
	                    if (highestY != blockY)
	                        accuracy++;
	                    else
	                        flag = true;
	                    break;
	                }
	                locX += dx;
	                locY += dy;
	                locZ += dz;
	            }
	            if (!flag) {
	                final int finalBlockX = (int) Math.floor(locX);
	                final int finalBlockZ = (int) Math.floor(locZ);
	                final int finalBlockY = (int) Math.floor(locY);
	                final long xzKey = xzKey(finalBlockX, finalBlockZ);
	                final int highestY = highestBlockCache.computeIfAbsent(xzKey,
	                    k -> world.getHighestBlockYAt(finalBlockX, finalBlockZ));
	                if (highestY > finalBlockY)
	                    accuracy += 2;
	            }
	        }
	    }
	    return Math.max(0f, 1f - (accuracy / (float) accuracyThreshold));
	}
	public static float getOutdoorExposureScore(Location location, float testRange) {
	    return getOutdoorExposureScore(location, testRange, 12);
	}
	public static float getOutdoorExposureScore(Location location) {
	    return getOutdoorExposureScore(location, 12f, 12);
	}
	private static long blockKey(int x, int y, int z) {
	    return ((long) x & 0x7FFFFFF) | (((long) z & 0x7FFFFFF) << 27) | (((long) y & 0xFFF) << 54);
	}
	private static long xzKey(int x, int z) {
	    return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
	}
	public static void clearCaches() {
	    passableCache.clear();
	    highestBlockCache.clear();
	}
	public static int isLocationExposedToOutdoorsDebug(Location location, float testRange) {
	    final World world = location.getWorld();
	    final float x = (float) location.getX();
	    final float y = (float) location.getY();
	    final float z = (float) location.getZ();
	    final float angleStep = (float) Math.toRadians(30.0);
	    final float invStepSize = 1f / 0.9f;
	    final int maxSteps = (int) (testRange * invStepSize);
	    int accuracy = 0;
	    for (int i = 0; i < 12; i++) {
	        final float radians = i * angleStep;
	        final float cosR = (float) Math.cos(radians);
	        final float sinR = (float) Math.sin(radians);
	        for (int j = 0; j < 3; j++) {
	            final float angleY = -0.5f + (j * 0.5f);
	            final float dx = (cosR + angleY) * 0.9f;
	            final float dy = angleY * 0.9f;
	            final float dz = (sinR + angleY) * 0.9f;
	            float locX = x + dx;
	            float locY = y + dy;
	            float locZ = z + dz;
	            boolean flag = false;
	            for (int step = 0; step < maxSteps; step++) {
	                final int blockX = (int) Math.floor(locX);
	                final int blockY = (int) Math.floor(locY);
	                final int blockZ = (int) Math.floor(locZ);
	                if (!world.getBlockAt(blockX, blockY, blockZ).isPassable()) {
	                    if (world.getHighestBlockYAt(blockX, blockZ) != blockY)
	                        accuracy++;
	                    else
	                        flag = true;
	                    break;
	                }
	                locX += dx;
	                locY += dy;
	                locZ += dz;
	            }
	            if (!flag) {
	                final int finalBlockX = (int) Math.floor(locX);
	                final int finalBlockZ = (int) Math.floor(locZ);
	                final int finalBlockY = (int) Math.floor(locY);
	                if (world.getHighestBlockYAt(finalBlockX, finalBlockZ) > finalBlockY)
	                    accuracy += 2;
	            }
//	            if (accuracy >= 80)
//	                return accuracy;
	        }
	    }
	    return accuracy; // accuracy < 80
	}
	public static int isLocationExposedToOutdoorsOptimizedDebug(Location location, float testRange, int horizontalCasts) {
	    final World world = location.getWorld();
	    final float x = (float) location.getX();
	    final float y = (float) location.getY();
	    final float z = (float) location.getZ();
	    final float angleStep = (float) (2 * Math.PI / horizontalCasts);
	    final float invStepSize = 1.111f;
	    final int maxSteps = (int) (testRange * invStepSize);
	    final int accuracyThreshold = (int) (horizontalCasts * 3 * 2.17);
	    int accuracy = 0;
	    for (int i = 0; i < horizontalCasts; i++) {
	        final float radians = i * angleStep;
	        final float cosR = (float) Math.cos(radians);
	        final float sinR = (float) Math.sin(radians);
	        for (int j = 0; j < 3; j++) {
	            final float angleY = -0.5f + (j * 0.5f);
	            final float dx = (cosR + angleY) * 0.9f;
	            final float dy = angleY * 0.9f;
	            final float dz = (sinR + angleY) * 0.9f;
	            float locX = x + dx;
	            float locY = y + dy;
	            float locZ = z + dz;
	            boolean flag = false;
	            int lastBlockX = Integer.MIN_VALUE;
	            int lastBlockY = Integer.MIN_VALUE;
	            int lastBlockZ = Integer.MIN_VALUE;
	            for (int step = 0; step < maxSteps; step++) {
	                final int blockX = (int) Math.floor(locX);
	                final int blockY = (int) Math.floor(locY);
	                final int blockZ = (int) Math.floor(locZ);
	                if (blockX == lastBlockX && blockY == lastBlockY && blockZ == lastBlockZ) {
	                    locX += dx;
	                    locY += dy;
	                    locZ += dz;
	                    continue;
	                }
	                lastBlockX = blockX;
	                lastBlockY = blockY;
	                lastBlockZ = blockZ;
	                final long blockKey = blockKey(blockX, blockY, blockZ);
	                final boolean passable = passableCache.computeIfAbsent(blockKey, 
	                    k -> world.getBlockAt(blockX, blockY, blockZ).isPassable());
	                if (!passable) {
	                    long xzKey = xzKey(blockX, blockZ);
	                    int highestY = highestBlockCache.computeIfAbsent(xzKey, 
	                        k -> world.getHighestBlockYAt(blockX, blockZ));
	                    if (highestY != blockY)
	                        accuracy++;
	                    else
	                        flag = true;
	                    break;
	                }
	                locX += dx;
	                locY += dy;
	                locZ += dz;
	            }
	            if (!flag) {
	                final int finalBlockX = (int) Math.floor(locX);
	                final int finalBlockZ = (int) Math.floor(locZ);
	                final int finalBlockY = (int) Math.floor(locY);
	                final long xzKey = xzKey(finalBlockX, finalBlockZ);
	                final int highestY = highestBlockCache.computeIfAbsent(xzKey, 
	                    k -> world.getHighestBlockYAt(finalBlockX, finalBlockZ));
	                if (highestY > finalBlockY)
	                    accuracy += 2;
	            }
	            if (accuracy >= accuracyThreshold)
	                return accuracy;
	        }
	    }
	    return accuracy;
	}
	public static boolean isLocationsWithinDistance(Location loc1, Location loc2, double distanceSquared) {
		return loc1 != null && loc2 != null && loc1.getWorld().equals(loc2.getWorld()) && loc1.distanceSquared(loc2) <= distanceSquared;
	}
	public static final double map(double value, double istart, double istop, double ostart, double ostop) {
		return ostart + (ostop - ostart) * ((value - istart) / (istop - istart));
	}
	public static boolean isEnvironment(World world, Environment environment) {
		return world.getEnvironment() == environment || world.getEnvironment() == Environment.CUSTOM;
	}
	public static <T> String getDecimalFormatted(T num) {
		return decimalFormat.format(num);
	}
	public static int clamp(int value, int min, int max) {
		return value < min ? min : value > max ? max : value;
	}
	public static double clamp(double value, double min, double max) {
		return value < min ? min : value > max ? max : value;
	}
	public static float clamp(float value, float min, float max) {
		return value < min ? min : value > max ? max : value;
	}
	public static <T> boolean isNotNullAndCondition(T object, Predicate<T> condition) {
		return object != null && condition.test(object);
	}
	public static <T, K> K getIfObjectNotNull(T object, Function<T, K> getter) {
		return object == null ? null : getter.apply(object);
	}
	public static void sendExceptionLog(Exception exception) {
		if (!sendErrors)
			return;
		exception.printStackTrace();
		DeadlyDisasters.consoleSender.sendMessage(prefix + Utils.convertString("&cAn error has occurred above this message. Please report the full error to the discord: &dhttps://discord.gg/MhXFj72VeN"));
	}
	public static void sendConsoleMessage(String message) {
		DeadlyDisasters.consoleSender.sendMessage(Utils.prefix + convertString(message));
	}
	public static RandomGenerator getRandomGenerator() {
		return random;
	}
}