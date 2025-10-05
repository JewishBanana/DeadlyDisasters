package com.github.jewishbanana.deadlydisasters.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.Main;

public class BlockUtils {
	
	private static boolean disableResistances;
	private static final Map<Material, Float> resistances;
	private static final RandomGenerator random;
	private static final Map<String, Set<Material>> customCategories;
	static {
		resistances = new HashMap<>();
		random = Utils.getRandomGenerator();
		
		Map<String, Set<String>> createCategory = new HashMap<>();
		createCategory.put("STONE_STAIRS", Set.of(
			    "ANDESITE_STAIRS",
			    "BLACKSTONE_STAIRS",
			    "BRICK_STAIRS",
			    "COBBLED_DEEPSLATE_STAIRS",
			    "COBBLESTONE_STAIRS",
			    "CUT_COPPER_STAIRS",
			    "DARK_PRISMARINE_STAIRS",
			    "DEEPSLATE_BRICK_STAIRS",
			    "DEEPSLATE_TILE_STAIRS",
			    "DIORITE_STAIRS",
			    "END_STONE_BRICK_STAIRS",
			    "EXPOSED_CUT_COPPER_STAIRS",
			    "GRANITE_STAIRS",
			    "MOSSY_COBBLESTONE_STAIRS",
			    "MOSSY_STONE_BRICK_STAIRS",
			    "NETHER_BRICK_STAIRS",
			    "OXIDIZED_CUT_COPPER_STAIRS",
			    "POLISHED_ANDESITE_STAIRS",
			    "POLISHED_BLACKSTONE_BRICK_STAIRS",
			    "POLISHED_BLACKSTONE_STAIRS",
			    "POLISHED_DIORITE_STAIRS",
			    "POLISHED_GRANITE_STAIRS",
			    "PRISMARINE_BRICK_STAIRS",
			    "PRISMARINE_STAIRS",
			    "PURPUR_STAIRS",
			    "QUARTZ_STAIRS",
			    "RED_NETHER_BRICK_STAIRS",
			    "RED_SANDSTONE_STAIRS",
			    "SANDSTONE_STAIRS",
			    "SMOOTH_QUARTZ_STAIRS",
			    "SMOOTH_RED_SANDSTONE_STAIRS",
			    "SMOOTH_SANDSTONE_STAIRS",
			    "STONE_BRICK_STAIRS",
			    "STONE_STAIRS",
			    "WAXED_CUT_COPPER_STAIRS",
			    "WAXED_EXPOSED_CUT_COPPER_STAIRS",
			    "WAXED_OXIDIZED_CUT_COPPER_STAIRS",
			    "WAXED_WEATHERED_CUT_COPPER_STAIRS",
			    "WEATHERED_CUT_COPPER_STAIRS"
			));
		createCategory.put("COMMAND_BLOCKS", Set.of(
				"COMMAND_BLOCK",
				"CHAIN_COMMAND_BLOCK",
				"REPEATING_COMMAND_BLOCK"
				));
		
		Map<String, Set<Material>> allCategories = new HashMap<>();
		createCategory.forEach((k, v) -> {
			Set<Material> materials = new HashSet<>();
			v.forEach(material -> {
				try {
					materials.add(Material.valueOf(material));
				} catch (IllegalArgumentException e) {}
			});
			allCategories.put(k, Set.copyOf(materials));
		});
		customCategories = Map.copyOf(allCategories);
	}

	public static void reload(Main plugin) {
		try {
			File file = new File(plugin.getDataFolder().getAbsolutePath(), "blocks.yml");
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				try {
					FileUtils.copyInputStreamToFile(plugin.getResource("files/blocks.yml"), file);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				ConfigUpdater.update(plugin, "files/blocks.yml", file, null);
			} catch (IOException e) {
				Utils.sendExceptionLog(e);
			}
			FileConfiguration config = YamlConfiguration.loadConfiguration(file);
			resistances.clear();
			disableResistances = DataUtils.getConfigBoolean(config, "blocks.yml", "protect_all_blocks", false);
			if (disableResistances) {
				Utils.sendConsoleMessage("&eWARNING you have enabled &d'protect_all_blocks' &ein the &b'blocks.yml' &efile. This means that disasters cannot destroy or displace any blocks at all!");
				return;
			}
			Set<String> keys = config.getKeys(true).stream().filter(key -> !config.isConfigurationSection(key)).collect(Collectors.toSet());
			Map<Material, Float> overrides = new HashMap<>();
			for (String path : keys)
				switch (path.toUpperCase()) {
				case "PROTECT_ALL_BLOCKS":
					break;
				case "IMMUNE_TYPES":
					DataUtils.getConfigStringList(config, "blocks.yml", path).forEach(category -> {
						Set<Material> set = getMaterials(category);
						if (set == null) {
							Utils.sendConsoleMessage("&cERROR no such block type or category named &e'"+category+"' &cin the &d'blocks.yml' &cfile in the &b'immune_types' &clist! This value will be omitted and not protected.");
							return;
						}
						set.forEach(material -> resistances.put(material, 1f));
					});
					break;
				default:
					String[] pathSplit = path.split("\\.");
					String key = pathSplit[pathSplit.length-1];
					Set<Material> set = getMaterials(key);
					if (set == null) {
						Utils.sendConsoleMessage("&cERROR no such block type or category named &e'"+key+"' &cin the &d'blocks.yml' &cfile at &b'"+path+"' &c! This value will be omitted and not protected.");
						return;
					}
					if (set.size() == 1) {
						Material material = set.iterator().next();
						if (material.toString().equals(key.toUpperCase())) {
							double value = DataUtils.getConfigDouble(config, "blocks.yml", path, 0.0);
							if (value != 0.0)
								overrides.put(material, (float) value);
							break;
						}
					}
					set.forEach(material -> {
						double value = DataUtils.getConfigDouble(config, "blocks.yml", path, 0.0);
						if (value != 0.0)
							resistances.put(material, (float) value);
					});
					break;
				}
			overrides.forEach((k, v) -> resistances.put(k, v));
		} catch (Exception exception) {
			Utils.sendExceptionLog(exception);
			Utils.sendConsoleMessage("&cERROR could not read the &eblocks.yml &cfile! Block resistances will not function until this is resolved!");
		}
	}
	public static Set<Material> getMaterials(String type) {
		String reformat = type.toUpperCase();
		Tag<Material> tag = getMaterialTagByName(reformat);
		if (tag != null)
			return Set.copyOf(tag.getValues());
		Set<Material> custom = customCategories.get(reformat);
		if (custom != null)
			return Set.copyOf(custom);
		try {
			return Set.of(Material.valueOf(reformat));
		} catch (IllegalArgumentException e) {}
		return null;
	}
	private static Tag<Material> getMaterialTagByName(String name) {
	    try {
	        Field field = Tag.class.getField(name);
	        Object value = field.get(null);
	        if (value instanceof Tag<?>) {
	            @SuppressWarnings("unchecked")
	            Tag<Material> tag = (Tag<Material>) value;
	            return tag;
	        }
	    } catch (NoSuchFieldException | IllegalAccessException e) {}
	    return null;
	}
	public static boolean testBlockResistance(Block block) {
		if (disableResistances)
			return true;
		Float value = resistances.get(block.getType());
		return value != null && (value == 1 || value < random.nextFloat());
	}
	public static boolean isBlockImmune(Block block) {
		return disableResistances || resistances.getOrDefault(block.getType(), 0f) == 1f;
	}
	public static boolean rayTraceForSolidBlock(Location initial, Location target) {
		Vector vec = new Vector(target.getX() - initial.getX(), target.getY() - initial.getY(), target.getZ() - initial.getZ()).normalize();
		double distance = Math.ceil(initial.distance(target));
		for (int i=0; i < distance; i++)
			if (!initial.clone().add(vec.clone().multiply(i)).getBlock().isPassable())
				return true;
		return false;
	}
	public static Block rayTraceForBlock(Location location, Vector direction, double maxDistance, Predicate<Block> conditions) {
		Vector vec = direction.normalize().multiply(0.8);
		Location forward = location.clone().add(vec);
		for (double i=0.8; i < maxDistance; i += 0.8) {
			Block temp = forward.getBlock();
			if (temp != null && conditions.test(temp))
				return temp;
			forward.add(vec);
		}
		return null;
	}
	public static Block rayTraceForBlock(Location location, Vector direction, double maxDistance) {
		return rayTraceForBlock(location, direction, maxDistance, temp -> !temp.isPassable());
	}
	public static Block rayTraceForBlock(Location location, Location target, double maxDistance) {
		return rayTraceForBlock(location, Utils.getVectorTowards(location, target), maxDistance, temp -> !temp.isPassable());
	}
	public static BlockFace getBlockFace(Player player) {
	    List<Block> lastTwoTargetBlocks = player.getLastTwoTargetBlocks(null, 100);
	    if (lastTwoTargetBlocks.size() != 2 || !lastTwoTargetBlocks.get(1).getType().isOccluding())
	    	return null;
	    Block targetBlock = lastTwoTargetBlocks.get(1);
	    Block adjacentBlock = lastTwoTargetBlocks.get(0);
	    return targetBlock.getFace(adjacentBlock);
	}
	public static Block rayCastForBlock(Location location, int minRange, int maxRange, int maxAttempts, Set<Material> materialWhitelist) {
		for (int i=0; i < maxAttempts; i++) {
			Location tempLoc = location.clone();
			Vector tempVec = new Vector((random.nextDouble()*2)-1, (random.nextDouble()*2)-1, (random.nextDouble()*2)-1).normalize();
			for (int c=0; c < maxRange; c++) {
				tempLoc.add(tempVec);
				Block b = tempLoc.getBlock();
				if (!b.isPassable()) {
					if (c < minRange || (materialWhitelist != null && !materialWhitelist.contains(b.getType())))
						break;
					return b;
				}
			}
		}
		return null;
	}
	public static Block rayCastForBlock(Location location, int minRange, int maxRange, int maxAttempts, Set<Material> materialWhitelist, Set<Block> blockWhitelist) {
		for (int i=0; i < maxAttempts; i++) {
			Location tempLoc = location.clone();
			Vector tempVec = new Vector((random.nextDouble()*2)-1, (random.nextDouble()*2)-1, (random.nextDouble()*2)-1).normalize();
			for (int c=0; c < maxRange; c++) {
				tempLoc.add(tempVec);
				Block b = tempLoc.getBlock();
				if (!b.isPassable()) {
					if (c < minRange || !blockWhitelist.contains(b) || (materialWhitelist != null && !materialWhitelist.contains(b.getType())))
						break;
					return b;
				}
			}
		}
		return null;
	}
	public static boolean rayTraceForSolid(Location initial, Location target) {
		Vector vec = Utils.getVectorTowards(initial, target);
		try {
			vec.checkFinite();
		} catch (IllegalArgumentException err) {
			return false;
		}
		int distance = (int) initial.distance(target);
		if (!initial.getBlock().isPassable())
			return true;
		Location temp = initial.clone();
		for (int i=1; i < distance; i++)
			if (!temp.add(vec.clone().multiply(i)).getBlock().isPassable())
				return true;
		return false;
	}
	public static Queue<Block> getBlocksInCircleRadius(Location location, double radius) {
		Queue<Block> queue = new ArrayDeque<>();
		double radiusSquared = radius * radius;
		Vector block = new Vector(location.getX(), location.getY(), location.getZ());
		World world = location.getWorld();
		for (double x = -radius; x <= radius; x++)
			for (double z = -radius; z <= radius; z++) {
				Vector position = block.clone().add(new Vector(x, 0, z));
				if (block.distanceSquared(position) <= radiusSquared)
					queue.add(position.toLocation(world).getBlock());
			}
		return queue;
	}
	public static Queue<Block> getBlocksInCircleCircumference(Location location, double radius) {
		Queue<Block> queue = new ArrayDeque<>();
		double outerRadius = radius * radius;
		double innerRadius = (radius - 1) * (radius - 1);
		Vector block = new Vector(location.getX(), location.getY(), location.getZ());
		World world = location.getWorld();
		for (double x = -radius; x <= radius; x++)
			for (double z = -radius; z <= radius; z++) {
				Vector position = block.clone().add(new Vector(x, 0, z));
				double distance = block.distanceSquared(position);
				if (distance <= outerRadius && distance > innerRadius)
					queue.add(position.toLocation(world).getBlock());
			}
		return queue;
	}
	public static Queue<Block> getBlocksInSphereRadius(Location location, double radius) {
		Queue<Block> queue = new ArrayDeque<>();
		double radiusSquared = radius * radius;
		Vector block = new Vector(location.getX(), location.getY(), location.getZ());
		World world = location.getWorld();
		for (double x = -radius; x <= radius; x++)
			for (double y = -radius; y <= radius; y++)
				for (double z = -radius; z <= radius; z++) {
					Vector position = block.clone().add(new Vector(x, y, z));
					if (block.distanceSquared(position) <= radiusSquared)
						queue.add(position.toLocation(world).getBlock());
				}
		return queue;
	}
	public static Block getHighestExposedBlock(Block block, int maxDistance, Predicate<Block> filter) {
		if (block == null)
			return null;
		Block b = block;
		if (!filter.test(b))
			for (int i=0; i < maxDistance; i++) {
				b = b.getRelative(BlockFace.DOWN);
				if (filter.test(b))
					return b;
			}
		else
			for (int i=0; i < maxDistance; i++) {
				b = b.getRelative(BlockFace.UP);
				if (!filter.test(b))
					return b.getRelative(BlockFace.DOWN);
			}
		return null;
	}
	public static Block getHighestExposedBlock(Block block, int maxDistance) {
		return getHighestExposedBlock(block, maxDistance, temp -> !temp.isPassable());
	}
	public static Location getCenterOfBlock(Block block) {
		return block.getLocation().add(.5, .5, .5);
	}
}
