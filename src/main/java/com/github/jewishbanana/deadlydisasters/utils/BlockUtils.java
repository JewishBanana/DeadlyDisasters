package com.github.jewishbanana.deadlydisasters.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
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
		resistances = new EnumMap<>(Material.class);
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
			if (!materials.isEmpty())
				allCategories.put(k, EnumSet.copyOf(materials));
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
								overrides.put(material, Utils.clamp((float) value, 0f, 1f));
							break;
						}
					}
					set.forEach(material -> {
						double value = DataUtils.getConfigDouble(config, "blocks.yml", path, 0.0);
						if (value != 0.0)
							resistances.put(material, Utils.clamp((float) value, 0f, 1f));
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
			return tag.getValues();
		Set<Material> custom = customCategories.get(reformat);
		if (custom != null)
			return custom;
		try {
			return Set.of(Material.valueOf(reformat));
		} catch (IllegalArgumentException e) {}
		return null;
	}
	public static Tag<Material> getMaterialTagByName(String name) {
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
	public static boolean doesBlockResist(Block block, ThreadLocalRandom rng) {
	    if (disableResistances)
	        return true;
	    final Float value = resistances.get(block.getType());
	    return value != null && (value == 1f || rng.nextFloat() < value);
	}
	public static boolean doesBlockResist(Block block) {
	    return doesBlockResist(block, ThreadLocalRandom.current());
	}
	public static boolean isBlockImmune(Block block) {
		return disableResistances || resistances.getOrDefault(block.getType(), 0f) == 1f;
	}
	public static Block rayTraceForBlock(Location location, Vector direction, double maxDistance, Predicate<Block> conditions) {
	    final float dx = (float)direction.getX();
	    final float dy = (float)direction.getY();
	    final float dz = (float)direction.getZ();
	    final float lengthSquared = dx * dx + dy * dy + dz * dz;
	    if (lengthSquared == 0.0f)
	        return null;
	    final float invLength = Utils.fastInverseSqrt(lengthSquared);
	    final float dirX = dx * invLength * 0.8f;
	    final float dirY = dy * invLength * 0.8f;
	    final float dirZ = dz * invLength * 0.8f;
	    final World world = location.getWorld();
	    final int steps = (int)(maxDistance / 0.8);
	    float x = (float)location.getX() + dirX;
	    float y = (float)location.getY() + dirY;
	    float z = (float)location.getZ() + dirZ;
	    for (int i = 0; i < steps; i++) {
	        final Block temp = world.getBlockAt((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
	        if (temp != null && conditions.test(temp))
	            return temp;
	        x += dirX;
	        y += dirY;
	        z += dirZ;
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
	public static Block rayCastForBlock(Location location, int minRange, int maxRange, int maxAttempts, Set<Material> materialWhitelist, Set<Block> blockWhitelist) {
	    final World world = location.getWorld();
	    final float startX = (float)location.getX();
	    final float startY = (float)location.getY();
	    final float startZ = (float)location.getZ();
	    for (int i = 0; i < maxAttempts; i++) {
	        final float dx = random.nextFloat(-1.0f, 1.0f);
	        final float dy = random.nextFloat(-1.0f, 1.0f);
	        final float dz = random.nextFloat(-1.0f, 1.0f);
	        final float lengthSquared = dx * dx + dy * dy + dz * dz;
	        if (lengthSquared == 0.0f)
	            continue;
	        final float invLength = Utils.fastInverseSqrt(lengthSquared);
	        final float dirX = dx * invLength;
	        final float dirY = dy * invLength;
	        final float dirZ = dz * invLength;
	        float x = startX;
	        float y = startY;
	        float z = startZ;
	        for (int c = 0; c < maxRange; c++) {
	            x += dirX;
	            y += dirY;
	            z += dirZ;
	            final Block b = world.getBlockAt((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
	            if (!b.isPassable()) {
	                if (c < minRange)
	                    break;
	                if (blockWhitelist != null && !blockWhitelist.contains(b))
	                    break;
	                if (materialWhitelist != null && !materialWhitelist.contains(b.getType()))
	                    break;
	                return b;
	            }
	        }
	    }
	    return null;
	}
	public static Block rayCastForBlock(Location location, int minRange, int maxRange, int maxAttempts, Set<Material> materialWhitelist) {
	    return rayCastForBlock(location, minRange, maxRange, maxAttempts, materialWhitelist, null);
	}
	public static boolean rayTraceForSolid(Location initial, Location target) {
	    final float dx = (float) (target.getX() - initial.getX());
	    final float dy = (float) (target.getY() - initial.getY());
	    final float dz = (float) (target.getZ() - initial.getZ());
	    final float lengthSquared = dx * dx + dy * dy + dz * dz;
	    if (lengthSquared == 0.0f || !Float.isFinite(lengthSquared))
	        return !initial.getBlock().isPassable();
	    final float distance = (float) Math.sqrt(lengthSquared);
	    final float dirX = dx / distance;
	    final float dirY = dy / distance;
	    final float dirZ = dz / distance;
	    if (!Float.isFinite(dirX) || !Float.isFinite(dirY) || !Float.isFinite(dirZ))
	        return !initial.getBlock().isPassable();
	    final World world = initial.getWorld();
	    final int distanceInt = (int) distance;
	    if (!initial.getBlock().isPassable())
	        return true;
	    float x = (float) initial.getX();
	    float y = (float) initial.getY();
	    float z = (float) initial.getZ();
	    for (int i = 1; i < distanceInt; i++) {
	        x += dirX;
	        y += dirY;
	        z += dirZ;
	        if (!world.getBlockAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)).isPassable())
	            return true;
	    }
	    return false;
	}
	public static List<Block> getBlocksInCircleRadius(Location location, float radius) {
	    final List<Block> list = new ArrayList<>((int) (Math.PI * radius * radius) + 1);
	    final float radiusSquared = radius * radius;
	    final World world = location.getWorld();
	    final float centerX = (float) location.getX();
	    final float centerY = (float) location.getY();
	    final float centerZ = (float) location.getZ();
	    final int minX = (int) Math.floor(centerX - radius);
	    final int maxX = (int) Math.floor(centerX + radius);
	    final int minZ = (int) Math.floor(centerZ - radius);
	    final int maxZ = (int) Math.floor(centerZ + radius);
	    final int blockY = (int) Math.floor(centerY);
	    for (int x = minX; x <= maxX; x++) {
	        final float dx = x - centerX;
	        final float dxSquared = dx * dx;
	        for (int z = minZ; z <= maxZ; z++) {
	            final float dz = z - centerZ;
	            final float distanceSquared = dxSquared + dz * dz;
	            if (distanceSquared <= radiusSquared)
	                list.add(world.getBlockAt(x, blockY, z));
	        }
	    }
	    return list;
	}
	public static List<Block> getBlocksInCircleCircumference(Location location, float radius) {
	    final List<Block> list = new ArrayList<>((int) (2 * Math.PI * radius) + 1);
	    final float outerRadius = radius * radius;
	    final float innerRadius = (radius - 1) * (radius - 1);
	    final World world = location.getWorld();
	    final float centerX = (float) location.getX();
	    final float centerY = (float) location.getY();
	    final float centerZ = (float) location.getZ();
	    final int minX = (int) Math.floor(centerX - radius);
	    final int maxX = (int) Math.floor(centerX + radius);
	    final int minZ = (int) Math.floor(centerZ - radius);
	    final int maxZ = (int) Math.floor(centerZ + radius);
	    final int blockY = (int) Math.floor(centerY);
	    for (int x = minX; x <= maxX; x++) {
	        final float dx = x - centerX;
	        final float dxSquared = dx * dx;
	        for (int z = minZ; z <= maxZ; z++) {
	            final float dz = z - centerZ;
	            final float distanceSquared = dxSquared + dz * dz;
	            if (distanceSquared <= outerRadius && distanceSquared > innerRadius)
	                list.add(world.getBlockAt(x, blockY, z));
	        }
	    }
	    return list;
	}
	public static List<Block> getBlocksInSphereRadius(Location location, float radius) {
	    final List<Block> list = new ArrayList<>((int) (4.188790 * radius * radius * radius) + 1);
	    final float radiusSquared = radius * radius;
	    final World world = location.getWorld();
	    final float centerX = (float) location.getX();
	    final float centerY = (float) location.getY();
	    final float centerZ = (float) location.getZ();
	    final int minX = (int) Math.floor(centerX - radius);
	    final int maxX = (int) Math.floor(centerX + radius);
	    final int minY = (int) Math.floor(centerY - radius);
	    final int maxY = (int) Math.floor(centerY + radius);
	    final int minZ = (int) Math.floor(centerZ - radius);
	    final int maxZ = (int) Math.floor(centerZ + radius);
	    for (int x = minX; x <= maxX; x++) {
	        final float dx = x - centerX;
	        final float dxSquared = dx * dx;
	        for (int y = minY; y <= maxY; y++) {
	            final float dy = y - centerY;
	            final float dySquared = dy * dy;
	            final float dxdySquared = dxSquared + dySquared;
	            for (int z = minZ; z <= maxZ; z++) {
	                final float dz = z - centerZ;
	                final float distanceSquared = dxdySquared + dz * dz;
	                if (distanceSquared <= radiusSquared)
	                    list.add(world.getBlockAt(x, y, z));
	            }
	        }
	    }
	    return list;
	}
	public static List<Block> getBlocksInCylinderRadius(Location location, float radius, float height) {
		List<Block> list = new ArrayList<>();
	    double radiusSquared = radius * radius;
	    int blockX = location.getBlockX();
	    int blockY = location.getBlockY();
	    int blockZ = location.getBlockZ();
	    World world = location.getWorld();
	    int radiusCeil = (int) Math.ceil(radius);
	    for (int y = 0; y <= height; y++)
	        for (int x = -radiusCeil; x <= radiusCeil; x++)
	            for (int z = -radiusCeil; z <= radiusCeil; z++) {
	                double distSquared = x * x + z * z;
	                if (distSquared <= radiusSquared)
	                    list.add(world.getBlockAt(blockX + x, blockY + y, blockZ + z));
	            }
	    return list;
	}
	public static Block getHighestExposedBlock(Block block, int maxDistance, Predicate<Block> filter) {
	    if (block == null)
	        return null;
	    final World world = block.getWorld();
	    final int x = block.getX();
	    final int z = block.getZ();
	    int y = block.getY();
	    if (!filter.test(block)) {
	        for (int i = 0; i < maxDistance; i++) {
	            y--;
	            final Block b = world.getBlockAt(x, y, z);
	            if (filter.test(b))
	                return b;
	        }
	    } else {
	        for (int i = 0; i < maxDistance; i++) {
	            y++;
	            final Block b = world.getBlockAt(x, y, z);
	            if (!filter.test(b))
	                return world.getBlockAt(x, y - 1, z);
	        }
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
