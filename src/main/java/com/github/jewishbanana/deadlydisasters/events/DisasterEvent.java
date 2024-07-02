package com.github.jewishbanana.deadlydisasters.events;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.listeners.BlockRegenHandler;
import com.github.jewishbanana.deadlydisasters.utils.BlockStateParser;
import com.github.jewishbanana.deadlydisasters.utils.ChannelDataHolder;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.mojang.datafixers.util.Pair;

public class DisasterEvent {
	
	public Disaster type;
	public int level;
	public World world;
	public FileConfiguration configFile;
	public boolean dropItems;
	
	public static Queue<DisasterEvent> ongoingDisasters = new ArrayDeque<>();
	public static Queue<DisasterEvent> regeneratingDisasters = new ArrayDeque<>();
	public static Map<UUID,Map<DisasterEvent,Integer>> countdownMap = new HashMap<>();
	
	public static Map<RepeatingTask, DisasterEvent> regeneratingTasks = new HashMap<>();
	
	public Map<Block,BlockState> damagedBlocks = new LinkedHashMap<>();
	public Map<Block,BlockState> physicBlocks = new LinkedHashMap<>();
	public Map<Block,Block> blockToBlock = new HashMap<>();
	public Set<Block> gravityHoldBlocks = new HashSet<>();
	public Map<UUID,Block> UUIDToFalling = new HashMap<>();
	public Set<UUID> fallingUUID = new HashSet<>();
	public Map<Block, Pair<BlockData, String>> threadSafeRegen = new LinkedHashMap<>();
	
	public static Map<Block, DisasterEvent> disasterBlocks = new ConcurrentHashMap<>();
	
	protected Set<ChannelDataHolder> survivingPlayers = new HashSet<>();
	
	public void addBlockToList(Block block, BlockState state) {
		if (disasterBlocks.containsKey(block) || damagedBlocks.containsKey(block))
			return;
		if (dropItems && state instanceof BlockInventoryHolder) {
			BlockInventoryHolder storage = (BlockInventoryHolder) state;
			Location loc = block.getLocation().add(.5,.5,.5);
			for (ItemStack item : storage.getInventory().getContents())
				if (item != null)
					block.getWorld().dropItemNaturally(loc, item.clone());
			storage.getInventory().setContents(new ItemStack[storage.getInventory().getSize()]);
		}
		damagedBlocks.put(block, state);
		disasterBlocks.put(block, this);
	}
	public void addBlockWithTopToList(Block block, BlockState state) {
		if (!disasterBlocks.containsKey(block) && !damagedBlocks.containsKey(block)) {
			if (dropItems && state instanceof BlockInventoryHolder) {
				BlockInventoryHolder storage = (BlockInventoryHolder) state;
				Location loc = block.getLocation().add(.5,.5,.5);
				for (ItemStack item : storage.getInventory().getContents())
					if (item != null)
						block.getWorld().dropItemNaturally(loc, item.clone());
				storage.getInventory().setContents(new ItemStack[storage.getInventory().getSize()]);
			}
			damagedBlocks.put(block, block.getState());
			disasterBlocks.put(block, this);
		}
		for (int i=0; i < 5; i++) {
			block = block.getRelative(BlockFace.UP);
			if (block.getType() == Material.AIR || block.isLiquid() || (!block.isPassable() && !Utils.isMaterialGravity(block.getType())))
				return;
			if (!disasterBlocks.containsKey(block) && !damagedBlocks.containsKey(block)) {
				physicBlocks.put(block, block.getState());
				disasterBlocks.put(block, this);
			}
		}
	}
	public void addBlockWithTopToListAsync(Block block, BlockState state) {
		if (disasterBlocks.containsKey(block) || !damagedBlocks.containsKey(block)) {
			if (dropItems && state instanceof BlockInventoryHolder) {
				BlockInventoryHolder storage = (BlockInventoryHolder) state;
				Location loc = block.getLocation().add(.5,.5,.5);
				for (ItemStack item : storage.getInventory().getContents())
					if (item != null)
						block.getWorld().dropItemNaturally(loc, item.clone());
				storage.getInventory().setContents(new ItemStack[storage.getInventory().getSize()]);
			}
			damagedBlocks.put(block, state);
			disasterBlocks.put(block, this);
		}
		Main.getInstance().getServer().getScheduler().runTask(Main.getInstance(), () -> {
			Block b = block;
			for (int i=0; i < 5; i++) {
				b = b.getRelative(BlockFace.UP);
				if (block.getType() == Material.AIR || block.isLiquid() || (!block.isPassable() && !Utils.isMaterialGravity(block.getType())))
					return;
				if (!disasterBlocks.containsKey(b) && !damagedBlocks.containsKey(b)) {
					physicBlocks.put(b, b.getState());
					disasterBlocks.put(b, this);
				}
			}
		});
	}
	public void reverseList() {
		if (damagedBlocks.size() <= 0)
			return;
		List<Block> blockList = new ArrayList<>(damagedBlocks.keySet());
		Collections.reverse(blockList);
		Map<Block,BlockState> blocks = new LinkedHashMap<>();
		for (Block b : blockList)
			blocks.put(b, damagedBlocks.get(b));
		damagedBlocks.clear();
		damagedBlocks.putAll(blocks);
	}
	public void inputPlayerToMap(int seconds, Player p) {
		if (!countdownMap.containsKey(p.getUniqueId()))
			countdownMap.put(p.getUniqueId(), new HashMap<>());
		countdownMap.get(p.getUniqueId()).put(this, seconds);
	}
	public void continueRegen(Main plugin) {
		dropItems = configFile.getBoolean("regeneration.drop_container_items");
		UUIDToFalling.forEach((k, v) -> {
			if (Bukkit.getEntity(k) != null)
				Bukkit.getEntity(k).remove();
		});
		DisasterEvent instance = this;
		final double regenRate = type.getDefaultRegenTickRate() * configFile.getDouble(type.name().toLowerCase()+".regen_rate");
		double[] regenTicks = {0};
		Map<Block, ItemStack[]> inventories = new HashMap<>();
		RepeatingTask[] task = new RepeatingTask[1];
		task[0] = new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				regenTicks[0] += regenRate;
				while (regenTicks[0] >= 1) {
					regenTicks[0] -= 1;
					if (threadSafeRegen.isEmpty()) {
						cancel();
						regeneratingTasks.remove(task[0]);
						regeneratingDisasters.remove(instance);
						BlockRegenHandler.gravityHold.removeAll(gravityHoldBlocks);
						break;
					}
					try {
						Entry<Block, Pair<BlockData, String>> entry = threadSafeRegen.entrySet().iterator().next();
						Block b = entry.getKey();
						Iterator<Entry<Block, Block>> it = blockToBlock.entrySet().iterator();
						while (it.hasNext()) {
							Entry<Block, Block> btb = it.next();
							if (btb.getKey().equals(b)) {
								Block toBlock = btb.getValue();
								if (b.equals(toBlock)) {
									it.remove();
									break;
								}
								if (toBlock.getState() instanceof InventoryHolder) {
									final ItemStack[] items = ((InventoryHolder) toBlock.getState()).getInventory().getContents();
									plugin.getServer().getScheduler().runTaskLater(plugin, () -> ((InventoryHolder) b.getState()).getInventory().setContents(items), 7);
								}
								toBlock.setType(Material.AIR);
								it.remove();
								while (it.hasNext()) {
									btb = it.next();
									if (btb.getValue().equals(b)) {
										if (b.getState() instanceof InventoryHolder)
											inventories.put(btb.getKey(), ((InventoryHolder) b.getState()).getInventory().getContents());
										it.remove();
									}
								}
								break;
							}
							if (btb.getValue().equals(b)) {
								if (b.getState() instanceof InventoryHolder)
									inventories.put(btb.getKey(), ((InventoryHolder) b.getState()).getInventory().getContents());
								it.remove();
							}
						}
						if (Utils.isMaterialGravity(entry.getValue().getFirst().getMaterial())) {
							gravityHoldBlocks.add(b);
							BlockRegenHandler.gravityHold.add(b);
						}
						b.setBlockData(entry.getValue().getFirst());
						if (entry.getValue().getSecond() != null)
							BlockStateParser.deserializeToBlock(entry.getValue().getSecond(), b);
						if (inventories.containsKey(b)) {
							plugin.getServer().getScheduler().runTaskLater(plugin, () -> ((InventoryHolder) b.getState()).getInventory().setContents(inventories.get(b)), 7);
						}
						threadSafeRegen.remove(b);
						disasterBlocks.remove(b);
					} catch (Exception e) {
						e.printStackTrace();
						Utils.sendDebugMessage();
					}
				}
			}
		};
		regeneratingTasks.put(task[0], instance);
	}
}
