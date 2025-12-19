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
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.Main;

public class Utils {
	
	private static final JavaPlugin plugin;
	private static final RandomGenerator random;
	
	public static final String prefix;
	private static final boolean sendErrors;
	private static final DecimalFormat decimalFormat;
	private static boolean usingSpigot;
	private static final Pattern hexPattern;
	private static final Map<DyeColor, ChatColor> dyeChatMap;
	static {
		plugin = Main.getInstance();
		random = RandomGenerator.of("SplittableRandom");
		hexPattern = Pattern.compile("\\(hex:#[a-fA-F0-9]{6}\\)");
		prefix = convertString("&a[DeadlyDisasters]: ");
		sendErrors = DataUtils.getMainConfigBoolean("general.debug_messages");
		decimalFormat = new DecimalFormat("0.0");
		
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
		
		try {
	        Class.forName("org.bukkit.entity.Player$Spigot");
	        usingSpigot = true;
	    } catch (Throwable tr) {
	    	usingSpigot = false;
	    }
	}
	
	public static String convertString(String text) {
		if (text == null)
			return null;
		String s = text;
		Matcher match = hexPattern.matcher(s);
		if (usingSpigot) {
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
		return new Vector(towards.getX() - initial.getX(), towards.getY() - initial.getY(), towards.getZ() - initial.getZ()).normalize();
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
	public static Location findRandomSpotInRadius(Location initial, double minDist, double maxDist, int height, int attempts, Supplier<Vector> vector, Predicate<Location> conditions) {
		double squaredMin = minDist * minDist;
//		double squaredMax = maxDist * maxDist;
		for (int i=0; i < attempts; i++) {
			double distance = random.nextDouble(minDist, maxDist);
			Location temp = SpawnUtils.findSmartYSpawn(initial, initial.clone().add(vector.get().multiply(distance)), height, (int) (maxDist - distance)); //(int) Math.floor(Math.sqrt(squaredMax - (distance * distance)))
			if (temp != null && temp.distanceSquared(initial) >= squaredMin && conditions.test(temp.clone()))
				return temp;
		}
		return null;
	}
	public static Location findRandomSpotInRadius(Location initial, double minDist, double maxDist, int height, int attempts, Supplier<Vector> vector) {
		return findRandomSpotInRadius(initial, minDist, maxDist, height, attempts, vector, test -> true);
	}
	public static Location findRandomSpotInRadius(Location initial, double minDist, double maxDist, int height, int attempts) {
		return findRandomSpotInRadius(initial, minDist, maxDist, height, attempts, () -> getRandomizedVector());
	}
	public static Location findRandomSpotInCircle(Location initial, double minDist, double maxDist, int attempts, Predicate<Location> conditions) {
		for (int i=0; i < attempts; i++) {
			double distance = random.nextDouble(minDist, maxDist);
			Location temp = initial.clone().add(getRandomizedVector(1.0, 0.0, 1.0).multiply(distance));
			if (conditions.test(temp.clone()))
				return temp;
		}
		return null;
	}
	public static Location findRandomSpotInCircle(Location initial, double minDist, double maxDist) {
		return initial.clone().add(getRandomizedVector(1.0, 0.0, 1.0).multiply(random.nextDouble(minDist, maxDist)));
	}
	public static Set<Chunk> getChunksInRadius(Location location, double radius) {
		final double radiusSquared = radius * radius;
		final int chunkX = location.getChunk().getX();
		final int chunkZ = location.getChunk().getZ();
		final int chunkRadius = (int) Math.ceil(radius / 16.0);
		final Set<Chunk> chunks = new HashSet<>();
		for (int x = -chunkRadius; x <= chunkRadius; x++)
			for (int z = -chunkRadius; z <= chunkRadius; z++) {
				int currentX = chunkX + x;
				int currentZ = chunkZ + z;
				double deltaX = location.getBlockX() - (Utils.clamp(location.getBlockX(), currentX * 16, (currentX + 1) * 16 - 1));
				double deltaZ = location.getBlockZ() - (Utils.clamp(location.getBlockZ(), currentZ * 16, (currentZ + 1) * 16 - 1));
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
			Location temp = location.clone().add(getRandomizedVector(1, 0, 1).multiply(random.nextDouble(1, 12)));
			if (BlockUtils.getHighestExposedBlock(temp.getBlock(), 12) != null)
				if (++count == 12)
					return true;
		}
		return count >= 12;
	}
	public static enum AreaClearing {
		
		CUBE_3X3_FROM_CENTER(block -> {
			for (int x = block.getX() - 1; x <= block.getX() + 1; x++)
				for (int y = block.getY() - 1; y <= block.getY() + 1; y++)
					for (int z = block.getZ() - 1; z <= block.getZ() + 1; z++)
						if (!block.getWorld().getBlockAt(x, y, z).isPassable())
							return false;
			return true;
		}),
		CUBE_3X3_FROM_CENTER_BOTTOM(block -> {
			for (int x = block.getX() - 1; x <= block.getX() + 1; x++)
				for (int y = block.getY(); y <= block.getY() + 2; y++)
					for (int z = block.getZ() - 1; z <= block.getZ() + 1; z++)
						if (!block.getWorld().getBlockAt(x, y, z).isPassable())
							return false;
			return true;
		}),
		CUBE_3X3_FROM_CENTER_TOP(block -> {
			for (int x = block.getX() - 1; x <= block.getX() + 1; x++)
				for (int y = block.getY() - 2; y <= block.getY(); y++)
					for (int z = block.getZ() - 1; z <= block.getZ() + 1; z++)
						if (!block.getWorld().getBlockAt(x, y, z).isPassable())
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
	public static boolean isAreaClear(Location location, double radius) {
		for (Block b : BlockUtils.getBlocksInSphereRadius(location, radius))
			if (!b.isPassable())
				return false;
		return true;
	}
	public static boolean isAreaClear(Location location, double radius, double height) {
		for (Block b : BlockUtils.getBlocksInCylinderRadius(location, radius, height))
			if (!b.isPassable())
				return false;
		return true;
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
	public static Vector getRandomizedVector(double xWeight, double yWeight, double zWeight) {
		return new Vector(xWeight == 0 ? 0.0 : random.nextDouble(-xWeight, xWeight), yWeight == 0 ? 0.0 : random.nextDouble(-yWeight, yWeight), zWeight == 0 ? 0.0 : random.nextDouble(-zWeight, zWeight)).normalize();
	}
	public static Vector getRandomizedVector() {
		return new Vector(random.nextDouble(-1, 1), random.nextDouble(-1, 1), random.nextDouble(-1, 1)).normalize();
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
		Main.consoleSender.sendMessage(prefix + Utils.convertString("&cAn error has occurred above this message. Please report the full error to the discord: &dhttps://discord.gg/MhXFj72VeN"));
	}
	public static void sendConsoleMessage(String message) {
		Main.consoleSender.sendMessage(Utils.prefix + convertString(message));
	}
	public static boolean isSpigot() {
		return usingSpigot;
	}
	public static RandomGenerator getRandomGenerator() {
		return random;
	}
}