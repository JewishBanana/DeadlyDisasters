package com.github.jewishbanana.deadlydisasters.listeners;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Bed.Part;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.WorldWrapper;
import com.github.jewishbanana.deadlydisasters.disasters.Disaster;
import com.github.jewishbanana.deadlydisasters.events.DisasterStopEvent.DisasterStopReason;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.RegenerationDataUtil;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;
import com.mojang.datafixers.util.Pair;

@SuppressWarnings("deprecation")
public class BlockRegenHandler implements Listener {
	
	private static final Map<Block, BlockState> damagedBlocks;
	private static final Map<Block, Material> placedBlocks;
	private static final Map<Block, Disaster> damageTracker;
	private static final Map<Block, Block> blockToBlock;
	private static final Map<Block, Block> blockOrigin;
	private static final Map<Block, List<BlockState>> physicBlocks;
	private static final Map<UUID, Pair<Block, Disaster>> fallingBlocks;
	private static final Map<Block, Disaster> collateralBlocks;
	private static final Set<Block> regenGravityBlocks;
	private static boolean cancelItemSpawns;
	static {
		damagedBlocks = new HashMap<>();
		placedBlocks = new HashMap<>();
		damageTracker = new HashMap<>();
		blockToBlock = new HashMap<>();
		blockOrigin = new HashMap<>();
		physicBlocks = new HashMap<>();
		fallingBlocks = new HashMap<>();
		collateralBlocks = new HashMap<>();
		regenGravityBlocks = new HashSet<>();
	}
	
	public BlockRegenHandler(Main plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	private static BlockState removeBlock(Block block, BlockState state, Disaster disaster, boolean withPhysics) {
		Block other = blockToBlock.remove(block);
		if (other != null)
			blockOrigin.remove(other);
		if (state instanceof InventoryHolder holder) {
			if (disaster.getWorldLink().dropContainerItems) {
				Location loc = BlockUtils.getCenterOfBlock(block);
				for (ItemStack item : holder.getInventory().getContents())
					if (item != null)
						block.getWorld().dropItemNaturally(loc, item);
				holder.getInventory().setContents(new ItemStack[holder.getInventory().getSize()]);
			}
			if (damagedBlocks.get(other) instanceof InventoryHolder otherHolder)
				otherHolder.getInventory().setContents(holder.getInventory().getContents());
		}
		List<Pair<BlockState, BlockFace>> capturedPhyicBlocks = null;
		if (damagedBlocks.putIfAbsent(block, state) == null) {
			damageTracker.put(block, disaster);
			if (withPhysics) {
				capturedPhyicBlocks = capturePhysicBlocks(block, disaster);
				cancelItemSpawns = true;
			}
		}
		DependencyUtils.logCoreProtectRemoval(state);
		block.setType(Material.AIR, withPhysics);
		cancelItemSpawns = false;
		if (capturedPhyicBlocks != null) {
			prunePhysicsList(capturedPhyicBlocks);
			if (!capturedPhyicBlocks.isEmpty())
				physicBlocks.putIfAbsent(block, capturedPhyicBlocks.stream().map(p -> p.getFirst()).collect(Collectors.toList()));
		}
		return state;
	}
	public static BlockState removeBlock(Block block, Disaster disaster, boolean withPhysics) {
		if (block.getType() == Material.AIR)
			return null;
		return removeBlock(block, block.getState(), disaster, withPhysics);
	}
	public static BlockState placeBlock(Block block, BlockData data, Disaster disaster, boolean withPhysics) {
		BlockState priorState = removeBlock(block, disaster, withPhysics);
		if (priorState == null && damagedBlocks.putIfAbsent(block, block.getState()) == null)
			damageTracker.put(block, disaster);
		block.setBlockData(data, withPhysics);
		placedBlocks.put(block, data.getMaterial());
		if (DependencyUtils.isCoreProtectEnabled())
			DependencyUtils.logCoreProtectPlacement(block.getState());
		return priorState;
	}
	public static BlockState placeBlock(Block block, Material material, Disaster disaster, boolean withPhysics) {
		return placeBlock(block, material.createBlockData(), disaster, withPhysics);
	}
	public static void moveBlock(Block from, Block to, Disaster disaster, boolean withPhysics) {
		if (Tag.BEDS.isTagged(from.getType()))
			removeBlock(from, disaster, withPhysics);
		BlockState fromState = from.getState();
		placeBlock(to, fromState.getBlockData(), disaster, withPhysics);
		if (fromState instanceof InventoryHolder fromHolder)
			((InventoryHolder) to.getState()).getInventory().setContents(fromHolder.getInventory().getContents());
		List<BlockState> physicStates = physicBlocks.get(to);
		if (physicStates != null)
			physicStates.removeIf(temp -> temp.getBlock().equals(from));
		Block other = blockToBlock.remove(from);
		removeBlock(from, fromState, disaster, withPhysics);
		if (other != null) {
			blockToBlock.put(to, other);
			blockOrigin.put(other, to);
		} else {
			if (blockToBlock.putIfAbsent(to, from) == null)
				blockOrigin.put(from, to);
		}
	}
	public static FallingBlock convertBlockIntoFallingBlock(Block block, Disaster disaster) {
		BlockState state = removeBlock(block, disaster, true);
		if (state == null)
			return null;
		FallingBlock entity = block.getWorld().spawnFallingBlock(BlockUtils.getCenterOfBlock(block), state.getBlockData());
		EntityUtils.markFallingBlock(entity);
		fallingBlocks.put(entity.getUniqueId(), Pair.of(block, disaster));
		EntitiesListener.attachRemoveKey(entity);
		return entity;
	}
	public static void replaceFallingBlockWithNew(UUID oldEntity, UUID newEntity) {
		Pair<Block, Disaster> pair = fallingBlocks.remove(oldEntity);
		if (pair == null)
			return;
		fallingBlocks.put(newEntity, pair);
	}
	public static boolean restoreBlock(Block block, boolean withPhysics) {
		BlockState state = damagedBlocks.get(block);
		if (state == null) {
			purgeBlockFromRegenData(block);
			return false;
		}
		Material placed = placedBlocks.get(block);
		List<BlockState> affectedBlocks = physicBlocks.get(block);
		Block from = blockToBlock.remove(block);
		if (from != null) {
			blockOrigin.remove(from);
			if (block.getType() == Material.AIR || (block.isLiquid() && !isLiquidSourceBlock(block))) {
				damagedBlocks.remove(from);
				purgeBlockFromRegenData(block);
				state.update(true);
				DependencyUtils.logCoreProtectPlacement(state);
				updatePhysicBlocks(affectedBlocks, withPhysics);
				return true;
			}
			if (from.getType() != Material.AIR && !from.isLiquid()) {
				damagedBlocks.remove(from);
				if (!blockOrigin.containsKey(block)) {
					purgeBlockFromRegenData(block);
					BlockState current = block.getState();
					state.update(true, false);
					Location center = BlockUtils.getCenterOfBlock(block);
					for (ItemStack item : block.getDrops())
						center.getWorld().dropItemNaturally(center, item);
					current.update(true, false);
					if (state instanceof InventoryHolder holder)
						for (ItemStack temp : holder.getInventory().getContents())
							if (temp != null)
								center.getWorld().dropItemNaturally(center, temp);
				}
				updatePhysicBlocks(affectedBlocks, withPhysics);
				return false;
			}
			restoreBlock(from, withPhysics);
			if (from.getState() instanceof InventoryHolder holder && block.getState() instanceof InventoryHolder current)
				holder.getInventory().setContents(current.getInventory().getContents());
			if (blockOrigin.containsKey(block)) {
				block.setType(Material.AIR);
				return true;
			}
		}
		purgeBlockFromRegenData(block);
		Material type = block.getType();
		if (block.getType() == Material.AIR && state.getType() == Material.AIR) {
			updatePhysicBlocks(affectedBlocks, withPhysics);
			return false;
		}
		WorldWrapper link = WorldWrapper.getWorldWrapper(block.getWorld());
		if (link.blackListedBlocks == null || !link.blackListedBlocks.contains(state.getType())) {
			if (from != null || type == placed || type == Material.AIR || (block.isLiquid() && !isLiquidSourceBlock(block))) {
				state.update(true, regenGravityBlocks.remove(block) ? false : withPhysics);
				DependencyUtils.logCoreProtectPlacement(state);
			} else {
				if (state.getType() == Material.AIR) {
					updatePhysicBlocks(affectedBlocks, withPhysics);
					return false;
				}
				BlockState current = block.getState();
				state.update(true, false);
				Location center = BlockUtils.getCenterOfBlock(block);
				for (ItemStack item : block.getDrops())
					center.getWorld().dropItemNaturally(center, item);
				current.update(true, false);
				if (state instanceof InventoryHolder holder)
					for (ItemStack temp : holder.getInventory().getContents())
						if (temp != null)
							center.getWorld().dropItemNaturally(center, temp);
			}
			updatePhysicBlocks(affectedBlocks, withPhysics);
			return true;
		}
		block.setType(Material.AIR);
		return true;
	}
	private static void purgeBlockFromRegenData(Block block) {
		damagedBlocks.remove(block);
		placedBlocks.remove(block);
		damageTracker.remove(block);
		physicBlocks.remove(block);
		collateralBlocks.remove(block);
		regenGravityBlocks.remove(block);
	}
	public static void printMaps() {
		Bukkit.broadcastMessage("damagedBlocks size "+damagedBlocks.size());
		Bukkit.broadcastMessage("placedBlocks size "+placedBlocks.size());
		Bukkit.broadcastMessage("damageTracker size "+damageTracker.size());
		Bukkit.broadcastMessage("physicBlocks size "+physicBlocks.size());
		Bukkit.broadcastMessage("blockToBlock size "+blockToBlock.size());
		Bukkit.broadcastMessage("blockOrigin size "+blockOrigin.size());
		Bukkit.broadcastMessage("collateralBlocks size "+collateralBlocks.size());
		Bukkit.broadcastMessage("regenGravityBlocks size "+regenGravityBlocks.size());
	}
	private static void updatePhysicBlocks(List<BlockState> states, boolean withPhysics) {
		if (states == null)
			return;
		states.forEach(physicState -> {
			try {
				Block physicBlock = physicState.getBlock();
				if (physicBlock.getType() == Material.AIR || physicBlock.isLiquid())
					physicState.update(true, withPhysics);
			} catch (IllegalStateException e) {
				Utils.sendExceptionLog(e);
			}
		});
	}
	private static final Map<BlockFace, Set<Material>> physicBlockTypes;
	private static final Map<Material, Set<Material>> plantFixes;
	private static final Set<Material> gravityBlockTypes = EnumSet.of(Material.GRAVEL, Material.SAND, Material.RED_SAND, Material.WHITE_CONCRETE_POWDER, Material.LIGHT_GRAY_CONCRETE_POWDER, Material.GRAY_CONCRETE_POWDER, Material.BLACK_CONCRETE_POWDER, Material.BROWN_CONCRETE_POWDER, Material.RED_CONCRETE_POWDER, 
			Material.ORANGE_CONCRETE_POWDER, Material.YELLOW_CONCRETE_POWDER, Material.LIME_CONCRETE_POWDER, Material.GREEN_CONCRETE_POWDER, Material.CYAN_CONCRETE_POWDER, Material.LIGHT_BLUE_CONCRETE_POWDER, Material.BLUE_CONCRETE_POWDER, Material.PURPLE_CONCRETE_POWDER, Material.MAGENTA_CONCRETE_POWDER, Material.PINK_CONCRETE_POWDER);
	private static final BlockFace[] adjacentFaces;
	static {
		final Set<Material> upwardPhysicBlocks = EnumSet.of(Material.SMALL_AMETHYST_BUD, Material.MEDIUM_AMETHYST_BUD, Material.LARGE_AMETHYST_BUD, Material.AMETHYST_CLUSTER, Material.MOSS_CARPET, Material.POINTED_DRIPSTONE, Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.CRIMSON_FUNGUS, Material.WARPED_FUNGUS, 
				VersionUtils.getShortGrass(), Material.TALL_GRASS, Material.FERN, Material.LARGE_FERN, Material.DEAD_BUSH, Material.BAMBOO_SAPLING, Material.BAMBOO, Material.SUGAR_CANE, Material.CACTUS, Material.CRIMSON_ROOTS, Material.WARPED_ROOTS, Material.NETHER_SPROUTS, Material.TWISTING_VINES, Material.TWISTING_VINES_PLANT, 
				Material.SMALL_DRIPLEAF, Material.BIG_DRIPLEAF, Material.BIG_DRIPLEAF_STEM, Material.GLOW_LICHEN, Material.SWEET_BERRY_BUSH, Material.SEAGRASS, Material.SEA_PICKLE, Material.KELP, Material.KELP_PLANT, Material.TORCH, Material.SOUL_TORCH, Material.REDSTONE_TORCH, Material.LANTERN, Material.SOUL_LANTERN, 
				Material.GRAVEL, Material.SCAFFOLDING, Material.REDSTONE_WIRE, Material.REPEATER, Material.COMPARATOR, Material.LEVER, Material.BELL, Material.NETHER_WART);
		upwardPhysicBlocks.addAll(Tag.ANVIL.getValues());
		upwardPhysicBlocks.addAll(Tag.BANNERS.getValues());
		upwardPhysicBlocks.addAll(Tag.BEDS.getValues());
		upwardPhysicBlocks.addAll(Tag.BUTTONS.getValues());
		upwardPhysicBlocks.addAll(Tag.CANDLES.getValues());
		upwardPhysicBlocks.addAll(Tag.CANDLE_CAKES.getValues());
		upwardPhysicBlocks.addAll(VersionUtils.isMCVersionOrAbove("1.19") ? Tag.WOOL_CARPETS.getValues() : Tag.CARPETS.getValues());
		upwardPhysicBlocks.addAll(Tag.CORAL_PLANTS.getValues());
		upwardPhysicBlocks.addAll(Tag.CROPS.getValues());
		upwardPhysicBlocks.addAll(Tag.DOORS.getValues());
		upwardPhysicBlocks.addAll(Tag.FLOWERS.getValues());
		upwardPhysicBlocks.addAll(Tag.PRESSURE_PLATES.getValues());
		upwardPhysicBlocks.addAll(Tag.RAILS.getValues());
		upwardPhysicBlocks.addAll(Tag.SAND.getValues());
		upwardPhysicBlocks.addAll(Tag.SAPLINGS.getValues());
		upwardPhysicBlocks.addAll(Tag.SNOW.getValues());
		upwardPhysicBlocks.addAll(Tag.SMALL_FLOWERS.getValues());
		upwardPhysicBlocks.addAll(gravityBlockTypes);
		
		final Set<Material> downwardPhysicBlocks = EnumSet.of(Material.SPORE_BLOSSOM, Material.WEEPING_VINES, Material.WEEPING_VINES_PLANT, Material.VINE, Material.GLOW_LICHEN, Material.HANGING_ROOTS, Material.LANTERN, Material.SOUL_LANTERN, Material.LEVER);
		downwardPhysicBlocks.addAll(Tag.CAVE_VINES.getValues());
		downwardPhysicBlocks.addAll(Tag.BUTTONS.getValues());
		
		final Set<Material> adjacentPhysicsBlocks = EnumSet.of(Material.VINE, Material.LADDER, Material.GLOW_LICHEN, Material.WALL_TORCH, Material.SOUL_WALL_TORCH, Material.REDSTONE_WALL_TORCH, Material.LEVER, Material.TRIPWIRE_HOOK);
		adjacentPhysicsBlocks.addAll(Tag.BUTTONS.getValues());
		adjacentPhysicsBlocks.addAll(Tag.BANNERS.getValues());
		adjacentPhysicsBlocks.addAll(Tag.WALL_SIGNS.getValues());
		
		if (VersionUtils.isMCVersionOrAbove("1.19")) {
			upwardPhysicBlocks.add(Material.SCULK_VEIN);
			downwardPhysicBlocks.add(Material.SCULK_VEIN);
			adjacentPhysicsBlocks.add(Material.SCULK_VEIN);
		}
		if (VersionUtils.isMCVersionOrAbove("1.20")) {
			upwardPhysicBlocks.addAll(Tag.ALL_SIGNS.getValues());
			upwardPhysicBlocks.add(Material.SUSPICIOUS_GRAVEL);
			gravityBlockTypes.add(Material.SUSPICIOUS_SAND);
			gravityBlockTypes.add(Material.SUSPICIOUS_GRAVEL);
		} else {
			upwardPhysicBlocks.addAll(Tag.SIGNS.getValues());
		}
		if (!VersionUtils.isMCVersionOrAbove("1.21.4")) {
			upwardPhysicBlocks.addAll(Tag.TALL_FLOWERS.getValues());
		}
		
		physicBlockTypes = new EnumMap<>(BlockFace.class);
		physicBlockTypes.put(BlockFace.UP, upwardPhysicBlocks);
		physicBlockTypes.put(BlockFace.DOWN, downwardPhysicBlocks);
		physicBlockTypes.put(BlockFace.NORTH, adjacentPhysicsBlocks);
		physicBlockTypes.put(BlockFace.EAST, adjacentPhysicsBlocks);
		physicBlockTypes.put(BlockFace.SOUTH, adjacentPhysicsBlocks);
		physicBlockTypes.put(BlockFace.WEST, adjacentPhysicsBlocks);
		
		plantFixes = new EnumMap<>(Material.class);
		plantFixes.put(Material.CAVE_VINES_PLANT, EnumSet.of(Material.CAVE_VINES_PLANT, Material.CAVE_VINES));
		plantFixes.put(Material.BIG_DRIPLEAF_STEM, EnumSet.of(Material.BIG_DRIPLEAF_STEM, Material.BIG_DRIPLEAF));
		plantFixes.put(Material.TWISTING_VINES_PLANT, EnumSet.of(Material.TWISTING_VINES_PLANT, Material.TWISTING_VINES));
		plantFixes.put(Material.WEEPING_VINES_PLANT, EnumSet.of(Material.WEEPING_VINES_PLANT, Material.WEEPING_VINES));
		plantFixes.put(Material.KELP_PLANT, EnumSet.of(Material.KELP_PLANT, Material.KELP));
		
		gravityBlockTypes.addAll(Tag.ANVIL.getValues());
		
		adjacentFaces = new BlockFace[] { BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
	}
	private static List<Pair<BlockState, BlockFace>> capturePhysicBlocks(Block block, Disaster disaster) {
		final List<Pair<BlockState, BlockFace>> captured = new ArrayList<>();
		for (BlockFace face : adjacentFaces) {
			Block other = block.getRelative(face);
			if (other == null)
				continue;
			Material material = other.getType();
			if (material == Material.AIR || !physicBlockTypes.get(face).contains(material))
				continue;
			Pair<BlockState, BlockFace> pair = Pair.of(other.getState(), face);
			captured.add(pair);
			switch (face) {
			case UP:
				BlockData data = other.getBlockData();
				if (data instanceof Bisected bisected) {
					Block otherHalf = other.getRelative(bisected.getHalf() == Half.TOP ? BlockFace.DOWN : BlockFace.UP);
					if (otherHalf != null && otherHalf.getType() == other.getType()) {
						captured.add(Pair.of(otherHalf.getState(), face));
						break;
					}
				}
				if (data instanceof Bed bed) {
					BlockFace facing = bed.getFacing();
					Block otherHalf = bed.getPart() == Bed.Part.FOOT ? other.getRelative(facing) :
						facing == BlockFace.NORTH ? other.getRelative(BlockFace.SOUTH) :
							facing == BlockFace.EAST ? other.getRelative(BlockFace.WEST) :
								facing == BlockFace.SOUTH ? other.getRelative(BlockFace.NORTH) :
									other.getRelative(BlockFace.EAST);
					if (otherHalf != null && otherHalf.getType() == other.getType()) {
						captured.add(Pair.of(otherHalf.getState(), face));
						if (bed.getPart() == Part.FOOT) {
							otherHalf.setType(Material.AIR);
							other.setType(Material.AIR);
						} else {
							other.setType(Material.AIR);
							otherHalf.setType(Material.AIR);
						}
						break;
					}
				}
				switch (material) {
				case POINTED_DRIPSTONE:
				case SNOW:
				case BIG_DRIPLEAF:
				case BIG_DRIPLEAF_STEM:
				case TWISTING_VINES:
				case TWISTING_VINES_PLANT:
				case CACTUS:
				case SUGAR_CANE:
				case SCAFFOLDING:
				case BAMBOO:
				case KELP:
				case KELP_PLANT:
					Block next = other.getRelative(face);
					Set<Material> types = plantFixes.getOrDefault(material, Set.of(material));
					do {
						if (next == null || !types.contains(next.getType()))
							break;
						captured.add(Pair.of(next.getState(), face));
						next = next.getRelative(face);
					} while (true);
					break;
				default:
					break;
				}
				if (gravityBlockTypes.contains(material))
					collateralBlocks.put(other, disaster);
				break;
			case DOWN:
				switch (material) {
				case CAVE_VINES:
				case CAVE_VINES_PLANT:
				case POINTED_DRIPSTONE:
				case WEEPING_VINES:
				case WEEPING_VINES_PLANT:
					Block next = other.getRelative(face);
					Set<Material> types = plantFixes.getOrDefault(material, Set.of(material));
					do {
						if (next == null || !types.contains(next.getType()))
							break;
						captured.add(Pair.of(next.getState(), face));
						next = next.getRelative(face);
					} while (true);
					break;
				default:
					break;
				}
				break;
			default:
				break;
			}
		}
		if (Tag.BEDS.isTagged(block.getType()) && block.getBlockData() instanceof Bed bed) {
			BlockFace facing = bed.getFacing();
			Block otherHalf = bed.getPart() == Bed.Part.FOOT ? block.getRelative(facing) :
				facing == BlockFace.NORTH ? block.getRelative(BlockFace.SOUTH) :
					facing == BlockFace.EAST ? block.getRelative(BlockFace.WEST) :
						facing == BlockFace.SOUTH ? block.getRelative(BlockFace.NORTH) :
							block.getRelative(BlockFace.EAST);
			if (otherHalf != null && otherHalf.getType() == block.getType()) {
				captured.add(Pair.of(otherHalf.getState(), facing));
				if (bed.getPart() == Bed.Part.FOOT) {
					otherHalf.setType(Material.AIR);
					block.setType(Material.AIR);
				} else {
					block.setType(Material.AIR);
					otherHalf.setType(Material.AIR);
				}
			}
		}
		return captured;
	}
	private static void prunePhysicsList(List<Pair<BlockState, BlockFace>> captured) {
		final Iterator<Pair<BlockState, BlockFace>> iterator = captured.iterator();
		while (iterator.hasNext()) {
			final Pair<BlockState, BlockFace> pair = iterator.next();
			final BlockState state = pair.getFirst();
			if (state.getBlock().getType() != state.getType())
				continue;
			switch (pair.getSecond()) {
			case UP -> {
				Material type = state.getType();
				switch (type) {
				case KELP:
				case KELP_PLANT:
				case TWISTING_VINES:
				case TWISTING_VINES_PLANT:
				case CACTUS:
				case SUGAR_CANE:
				case SCAFFOLDING:
				case BAMBOO:
					Block block = state.getBlock();
					block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType());
					block.setType(Material.AIR);
					continue;
				default:
					break;
				}
			}
			case DOWN -> {
				switch (state.getType()) {
				case CAVE_VINES:
				case CAVE_VINES_PLANT:
				case WEEPING_VINES:
				case WEEPING_VINES_PLANT:
					Block block = state.getBlock();
					block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType());
					block.setType(Material.AIR);
					continue;
				default:
					break;
				}
			}
			default -> {}
			}
			iterator.remove();
		}
	}
	private static boolean isLiquidSourceBlock(Block block) {
		return block.getBlockData() instanceof Levelled levelled && levelled.getLevel() == 0;
	}
	
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
    	if (cancelItemSpawns)
    		event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
    	Block other = blockToBlock.remove(event.getBlock());
    	if (other != null) {
    		BlockState otherState = damagedBlocks.get(other);
    		if (otherState != null)
    			otherState.setType(Material.AIR);
    		blockOrigin.remove(other);
    	}
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
    	placedBlocks.remove(event.getBlock());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
    	Block other = blockToBlock.remove(event.getBlock());
    	if (other != null) {
    		BlockState otherState = damagedBlocks.get(other);
    		if (otherState != null)
    			otherState.setType(Material.AIR);
    		blockOrigin.remove(other);
    	}
    }
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFallingBlockForm(EntityChangeBlockEvent event) {
    	Pair<Block, Disaster> pair = fallingBlocks.remove(event.getEntity().getUniqueId());
    	if (pair != null) {
    		Block block = event.getBlock();
    		if (pair.getFirst().equals(block)) {
    			BlockState fromState = damagedBlocks.get(pair.getFirst());
    			if (fromState != null && fromState instanceof InventoryHolder fromHolder)
    				new BukkitRunnable() {
    				@Override
    				public void run() {
    					if (event.isCancelled())
    						return;
    					BlockState toState = block.getState();
    					if (toState instanceof InventoryHolder toHolder)
    						toHolder.getInventory().setContents(fromHolder.getInventory().getContents());
    				}
    			}.runTask(Main.getInstance());
    			return;
    		}
    		event.setCancelled(true);
    		pair.getSecond().getModifiedBlocks().add(block);
    		placeBlock(block, event.getBlockData(), pair.getSecond(), true);
    		blockToBlock.put(block, pair.getFirst());
        	blockOrigin.put(pair.getFirst(), block);
        	BlockState fromState = damagedBlocks.get(pair.getFirst());
        	if (fromState != null && fromState instanceof InventoryHolder fromHolder) {
        		BlockState toState = block.getState();
        		if (toState instanceof InventoryHolder toHolder)
        			toHolder.getInventory().setContents(fromHolder.getInventory().getContents());
        	}
        	return;
    	}
    	Disaster disaster = collateralBlocks.remove(event.getBlock());
    	if (disaster != null) {
    		event.setCancelled(true);
    		convertBlockIntoFallingBlock(event.getBlock(), disaster);
    		return;
    	}
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
    	event.blockList().forEach(block -> {
    		Block other = blockToBlock.remove(block);
    		if (other != null) {
        		BlockState otherState = damagedBlocks.get(other);
        		if (otherState != null)
        			otherState.setType(Material.AIR);
        		blockOrigin.remove(other);
        	}
    	});
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
    	if (event.getCause() != IgniteCause.LAVA && event.getCause() != IgniteCause.SPREAD)
    		return;
    	Disaster disaster = damageTracker.get(event.getIgnitingBlock());
    	if (disaster != null) {
    		Block block = event.getBlock();
    		damagedBlocks.putIfAbsent(block, block.getState());
    		if (damageTracker.putIfAbsent(block, disaster) == null)
    			disaster.getModifiedBlocks().add(block);
    	}
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
    	Disaster disaster = damageTracker.get(event.getSource());
    	if (disaster != null) {
    		Block block = event.getBlock();
    		damagedBlocks.putIfAbsent(block, block.getState());
    		if (damageTracker.putIfAbsent(block, disaster) == null)
    			disaster.getModifiedBlocks().add(block);
    	}
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
    	Disaster disaster = damageTracker.get(event.getIgnitingBlock());
    	if (disaster != null) {
    		Block block = event.getBlock();
    		damagedBlocks.putIfAbsent(block, block.getState());
    		if (damageTracker.putIfAbsent(block, disaster) == null)
    			disaster.getModifiedBlocks().add(block);
    	}
    }
    public static void saveAll(Main plugin) {
    	List<Disaster> disasters = new ArrayList<>();
    	Queue<Disaster> ongoing = new ArrayDeque<>(Disaster.onGoingDisasters);
    	ongoing.forEach(disaster -> {
    		try {
    			disaster.stop(DisasterStopReason.SERVER_CLOSING);
    			if (!disaster.getModifiedBlocks().isEmpty())
    				disasters.add(disaster);
    		} catch (Exception e) {
    			Utils.sendExceptionLog(e);
    		}
    	});
    	if (!DataUtils.getMainConfigBoolean("regeneration.save_regen_data")) {
    		plugin.getLogger().warning("The setting 'save_regen_data' in the main config file has been set to false. Disaster block damage will not be saved!");
    	}
    	Disaster.regeneratingDisasters.forEach((disaster, regenTask) -> {
    		regenTask.task.cancel();
    		Set<Block> orderedSet = new LinkedHashSet<>();
    		orderedSet.addAll(regenTask.blocks);
    		orderedSet.addAll(disaster.getModifiedBlocks());
    		disaster.getModifiedBlocks().clear();
    		disaster.getModifiedBlocks().addAll(orderedSet);
    		disasters.add(disaster);
    	});
    	if (!disasters.isEmpty()) {
    		plugin.getLogger().info("Saving regen data...");
	    	RegenerationDataUtil.saveAll(
	    			plugin,
				    damagedBlocks,
				    placedBlocks,
				    damageTracker,
				    blockToBlock,
				    blockOrigin,
				    physicBlocks,
				    disasters
				);
	    	plugin.getLogger().info("Successfully saved all regen data!");
    	}
    }
    public static void loadAll(Main plugin) {
    	plugin.getLogger().info("Loading regen data...");
    	RegenerationDataUtil.loadAllAsync(
    			plugin,
			    damagedBlocks,
			    placedBlocks,
			    damageTracker,
			    blockToBlock,
			    blockOrigin,
			    physicBlocks
			);
    }
}
