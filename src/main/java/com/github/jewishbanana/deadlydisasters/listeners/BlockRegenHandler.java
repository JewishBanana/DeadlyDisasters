package com.github.jewishbanana.deadlydisasters.listeners;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

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
import com.github.jewishbanana.deadlydisasters.utils.RegenerationDataUtil;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.ultimatecontent.utils.EntityUtils;
import com.mojang.datafixers.util.Pair;

public class BlockRegenHandler implements Listener {
	
	private static final Map<Block, BlockState> damagedBlocks;
	private static final Map<Block, Material> placedBlocks;
	private static final Map<Block, Disaster> damageTracker;
	private static final Map<Block, Block> blockToBlock;
	private static final Map<Block, Set<BlockState>> physicBlocks;
	private static final Map<UUID, Pair<Block, Disaster>> fallingBlocks;
	static {
		damagedBlocks = new HashMap<>();
		placedBlocks = new HashMap<>();
		damageTracker = new HashMap<>();
		blockToBlock = new HashMap<>();
		physicBlocks = new HashMap<>();
		fallingBlocks = new HashMap<>();
	}
	
	public BlockRegenHandler(Main plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	private static BlockState removeBlock(Block block, BlockState state, Disaster disaster, boolean withPhysics) {
		Block other = blockToBlock.remove(block);
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
		Set<BlockState> capturedPhyicBlocks = null;
		if (damagedBlocks.putIfAbsent(block, state) == null) {
			damageTracker.put(block, disaster);
			if (withPhysics)
				capturedPhyicBlocks = capturePhysicBlocks(block);
		}
		DependencyUtils.logCoreProtectRemoval(state);
		block.setType(Material.AIR, withPhysics);
		if (capturedPhyicBlocks != null) {
			capturedPhyicBlocks.removeIf(temp -> temp.getBlock().getType() == temp.getType());
			if (!capturedPhyicBlocks.isEmpty())
				physicBlocks.putIfAbsent(block, capturedPhyicBlocks);
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
		Set<BlockState> physicStates = physicBlocks.get(to);
		if (physicStates != null)
			physicStates.removeIf(temp -> temp.getBlock().equals(from));
		Block other = blockToBlock.remove(from);
		removeBlock(from, fromState, disaster, withPhysics);
		if (other != null)
			blockToBlock.put(to, other);
		else
			blockToBlock.putIfAbsent(to, from);
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
	public static void restoreBlock(Block block, boolean withPhysics) {
		BlockState state = damagedBlocks.remove(block);
		Material placed = placedBlocks.remove(block);
		damageTracker.remove(block);
		Set<BlockState> affectedBlocks = physicBlocks.remove(block);
		if (state == null)
			return;
		Block from = blockToBlock.remove(block);
		if (from != null) {
			if (block.getType() == Material.AIR
					|| (block.isLiquid() && !isLiquidSourceBlock(block))) {
				damagedBlocks.remove(from);
				state.update(true);
				DependencyUtils.logCoreProtectPlacement(state);
				updatePhysicBlocks(affectedBlocks, withPhysics);
				return;
			}
			if (from.getType() != Material.AIR && !from.isLiquid()) {
				damagedBlocks.remove(from);
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
				updatePhysicBlocks(affectedBlocks, withPhysics);
				return;
			}
			restoreBlock(from, withPhysics);
			if (from.getState() instanceof InventoryHolder holder && block.getState() instanceof InventoryHolder current)
				holder.getInventory().setContents(current.getInventory().getContents());
		}
		WorldWrapper link = WorldWrapper.getWorldWrapper(block.getWorld());
		if (link.blackListedBlocks == null || !link.blackListedBlocks.contains(state.getType())) {
			if (from != null || block.getType() == placed || block.getType() == Material.AIR || (block.isLiquid() && !isLiquidSourceBlock(block))) {
				state.update(true, withPhysics);
				DependencyUtils.logCoreProtectPlacement(state);
			} else {
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
			return;
		}
		block.setType(Material.AIR);
	}
	private static void updatePhysicBlocks(Set<BlockState> states, boolean withPhysics) {
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
	private static final Map<Material, Set<Material>> plantFixes;
	private static final BlockFace[] adjacentFaces;
	static {
		plantFixes = new HashMap<>();
		plantFixes.put(Material.BIG_DRIPLEAF_STEM, Set.of(Material.BIG_DRIPLEAF_STEM, Material.BIG_DRIPLEAF));
		plantFixes.put(Material.TWISTING_VINES_PLANT, Set.of(Material.TWISTING_VINES_PLANT, Material.TWISTING_VINES));
		plantFixes.put(Material.WEEPING_VINES_PLANT, Set.of(Material.WEEPING_VINES_PLANT, Material.WEEPING_VINES));
		plantFixes.put(Material.KELP_PLANT, Set.of(Material.KELP_PLANT, Material.KELP));
		
		adjacentFaces = new BlockFace[] { BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
	}
	private static Set<BlockState> capturePhysicBlocks(Block block) {
		Set<BlockState> captured = new HashSet<>();
		for (BlockFace face : adjacentFaces) {
			Block other = block.getRelative(face);
			if (other == null)
				continue;
			Material material = other.getType();
			if (material == Material.AIR)
				continue;
			captured.add(other.getState());
			switch (face) {
			case UP:
				BlockData data = other.getBlockData();
				if (data instanceof Bisected bisected) {
					Block otherHalf = other.getRelative(bisected.getHalf() == Half.TOP ? BlockFace.DOWN : BlockFace.UP);
					if (otherHalf != null && otherHalf.getType() == other.getType()) {
						captured.add(otherHalf.getState());
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
						captured.add(otherHalf.getState());
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
				if (material == Material.POINTED_DRIPSTONE
						|| material == Material.SNOW
						|| material == Material.BIG_DRIPLEAF_STEM
						|| material == Material.TWISTING_VINES_PLANT
						|| material == Material.CACTUS
						|| material == Material.SUGAR_CANE
						|| material == Material.SCAFFOLDING
						|| material == Material.BAMBOO
						|| material == Material.KELP_PLANT) {
					Block next = other.getRelative(face);
					Set<Material> types = plantFixes.getOrDefault(material, Set.of(material));
					do {
						if (next == null || !types.contains(next.getType()))
							break;
						captured.add(next.getState());
						next = next.getRelative(face);
					} while (true);
				}
				break;
			case DOWN:
				if (Tag.CAVE_VINES.isTagged(material)
						|| material == Material.POINTED_DRIPSTONE
						|| material == Material.WEEPING_VINES_PLANT) {
					Block next = other.getRelative(face);
					Set<Material> types = plantFixes.getOrDefault(material, Set.of(material));
					do {
						if (next == null || !types.contains(next.getType()))
							break;
						captured.add(next.getState());
						next = next.getRelative(face);
					} while (true);
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
				captured.add(otherHalf.getState());
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
	private static boolean isLiquidSourceBlock(Block block) {
		return block.getBlockData() instanceof Levelled levelled && levelled.getLevel() == 0;
	}
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
    	Block other = blockToBlock.remove(event.getBlock());
    	if (other != null)
    		damagedBlocks.remove(other);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
    	placedBlocks.remove(event.getBlock());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
    	Block other = blockToBlock.remove(event.getBlock());
    	if (other != null)
    		damagedBlocks.remove(other);
    }
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFallingBlockForm(EntityChangeBlockEvent event) {
    	Pair<Block, Disaster> pair = fallingBlocks.remove(event.getEntity().getUniqueId());
    	if (pair == null)
    		return;
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
    	if (!pair.getSecond().getModifiedBlocks().contains(pair.getFirst())) {
    		event.getEntity().remove();
    		return;
    	}
    	pair.getSecond().getModifiedBlocks().add(block);
    	new BukkitRunnable() {
			@Override
			public void run() {
				placeBlock(block, event.getBlockData(), pair.getSecond(), true);
				blockToBlock.put(block, pair.getFirst());
				BlockState fromState = damagedBlocks.get(pair.getFirst());
				if (fromState != null && fromState instanceof InventoryHolder fromHolder) {
					BlockState toState = block.getState();
					if (toState instanceof InventoryHolder toHolder)
						toHolder.getInventory().setContents(fromHolder.getInventory().getContents());
				}
			}
		}.runTask(Main.getInstance());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
    	event.blockList().forEach(block -> {
    		Block other = blockToBlock.remove(block);
        	if (other != null)
        		damagedBlocks.remove(other);
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
			    physicBlocks
			);
    }
}
