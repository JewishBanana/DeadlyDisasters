package com.github.jewishbanana.deadlydisasters.listeners;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.events.DestructionDisaster;
import com.github.jewishbanana.deadlydisasters.events.Disaster;
import com.github.jewishbanana.deadlydisasters.events.DisasterEvent;
import com.github.jewishbanana.deadlydisasters.events.WeatherDisaster;
import com.github.jewishbanana.deadlydisasters.handlers.WorldObject;
import com.github.jewishbanana.deadlydisasters.utils.BlockStateParser;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.mojang.datafixers.util.Pair;

public class BlockRegenHandler implements Listener {
	
	public Map<UUID,Block> uuidToBlock = new HashMap<>();
	
	public static Set<Block> gravityHold = ConcurrentHashMap.newKeySet();
	
	private boolean fireSpread;
	
	public BlockRegenHandler(Main plugin) {
		reload(plugin);
		this.fireSpread = plugin.getConfig().getBoolean("regeneration.fire_spread");
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	public static void reload(Main plugin) {
	}
	@EventHandler(priority=EventPriority.HIGH)
	public void preBlockChange(EntityChangeBlockEvent e) {
		if (e.isCancelled() || !(e.getEntity() instanceof FallingBlock))
			return;
		if (gravityHold.contains(e.getBlock())) {
			e.setCancelled(true);
			return;
		}
	}
	@EventHandler(priority=EventPriority.MONITOR)
	public void onBlockChange(EntityChangeBlockEvent e) {
		if (e.isCancelled() || !(e.getEntity() instanceof FallingBlock))
			return;
		UUID uuid = e.getEntity().getUniqueId();
		Block b = e.getBlock();
		if (e.getTo() == Material.AIR) {
			Block below = b.getRelative(BlockFace.DOWN);
			for (DisasterEvent dis : DisasterEvent.ongoingDisasters) {
				if (dis.damagedBlocks.containsKey(b)) {
					if (dis.blockToBlock.containsValue(b)) {
						Block original = null;
						for (Map.Entry<Block, Block> entry : dis.blockToBlock.entrySet())
							if (entry.getValue().equals(b)) {
								original = entry.getKey();
								break;
							}
						dis.blockToBlock.remove(original);
						dis.UUIDToFalling.put(uuid, original);
						return;
					}
					dis.UUIDToFalling.put(uuid, b);
					return;
				}
				if (Utils.isMaterialGravity(b.getType()) && dis.damagedBlocks.containsKey(below) && !DisasterEvent.disasterBlocks.containsKey(b)) {
					dis.addBlockToList(b, e.getBlock().getState());
					dis.UUIDToFalling.put(uuid, b);
					return;
				}
			}
			for (DisasterEvent dis : DisasterEvent.regeneratingDisasters) {
				if (dis.damagedBlocks.containsKey(b)) {
					if (dis.blockToBlock.containsValue(b)) {
						Block original = null;
						for (Map.Entry<Block, Block> entry : dis.blockToBlock.entrySet())
							if (entry.getValue().equals(b)) {
								original = entry.getKey();
								break;
							}
						dis.blockToBlock.remove(original);
						dis.UUIDToFalling.put(uuid, original);
						return;
					}
					dis.UUIDToFalling.put(uuid, b);
					return;
				}
				if (Utils.isMaterialGravity(b.getType()) && dis.damagedBlocks.containsKey(below) && !DisasterEvent.disasterBlocks.containsKey(b)) {
					dis.addBlockToList(b, e.getBlock().getState());
					dis.UUIDToFalling.put(uuid, b);
					return;
				}
			}
		} else {
			for (DisasterEvent dis : DisasterEvent.ongoingDisasters) {
				if (dis.fallingUUID.remove(uuid)) {
					dis.addBlockWithTopToList(b, b.getState());
					return;
				}
				if (dis.UUIDToFalling.containsKey(uuid)) {
					dis.blockToBlock.put(dis.UUIDToFalling.get(uuid), e.getBlock());
					dis.UUIDToFalling.remove(uuid);
					return;
				}
			}
			for (DisasterEvent dis : DisasterEvent.regeneratingDisasters) {
				if (dis.fallingUUID.remove(uuid)) {
					dis.addBlockWithTopToList(b, b.getState());
					return;
				}
				if (dis.UUIDToFalling.containsKey(uuid)) {
					dis.blockToBlock.put(dis.UUIDToFalling.get(uuid), e.getBlock());
					dis.UUIDToFalling.remove(uuid);
					return;
				}
			}
		}
	}
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerPlace(BlockPlaceEvent e) {
		if (e.isCancelled())
			return;
		for (DisasterEvent dis : DisasterEvent.ongoingDisasters)
			dis.damagedBlocks.remove(e.getBlock());
		for (DisasterEvent dis : DisasterEvent.regeneratingDisasters) {
			dis.damagedBlocks.remove(e.getBlock());
		}
	}
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerBreak(BlockBreakEvent e) {
		if (e.isCancelled())
			return;
		for (DisasterEvent dis : DisasterEvent.ongoingDisasters) {
			if (dis.blockToBlock.containsValue(e.getBlock())) {
				Iterator<Entry<Block, Block>> it = dis.blockToBlock.entrySet().iterator();
				while (it.hasNext())
					if (it.next().getValue().equals(e.getBlock())) {
						it.remove();
						return;
					}
			}
		}
		for (DisasterEvent dis : DisasterEvent.regeneratingDisasters) {
			if (dis.blockToBlock.containsValue(e.getBlock())) {
				Iterator<Entry<Block, Block>> it = dis.blockToBlock.entrySet().iterator();
				while (it.hasNext())
					if (it.next().getValue().equals(e.getBlock())) {
						it.remove();
						return;
					}
			}
		}
	}
	@EventHandler(priority=EventPriority.HIGH)
	public void onBlockSpread(BlockSpreadEvent e) {
		if (e.isCancelled())
			return;
		for (DisasterEvent dis : DisasterEvent.ongoingDisasters)
			if (dis.damagedBlocks.containsKey(e.getSource())) {
				if (!fireSpread) {
					e.setCancelled(true);
					return;
				}
				dis.addBlockToList(e.getBlock(), e.getBlock().getState());
				return;
			}
		for (DisasterEvent dis : DisasterEvent.regeneratingDisasters)
			if (dis.damagedBlocks.containsKey(e.getSource())) {
				if (!fireSpread) {
					e.setCancelled(true);
					return;
				}
				dis.addBlockToList(e.getBlock(), e.getBlock().getState());
				return;
			}
	}
	@EventHandler(priority=EventPriority.MONITOR)
	public void onBlockBurn(BlockBurnEvent e) {
		if (e.isCancelled())
			return;
		for (DisasterEvent dis : DisasterEvent.ongoingDisasters)
			if (dis.damagedBlocks.containsKey(e.getIgnitingBlock())) {
				dis.addBlockToList(e.getBlock(), e.getBlock().getState());
				return;
			}
		for (DisasterEvent dis : DisasterEvent.regeneratingDisasters)
			if (dis.damagedBlocks.containsKey(e.getIgnitingBlock())) {
				dis.addBlockToList(e.getBlock(), e.getBlock().getState());
				return;
			}
	}
	@EventHandler(priority=EventPriority.HIGH)
	public void onBlockIgnite(BlockIgniteEvent e) {
		if (e.isCancelled())
			return;
		for (DisasterEvent dis : DisasterEvent.ongoingDisasters)
			if (dis.damagedBlocks.containsKey(e.getBlock())) {
				if (!fireSpread) {
					e.setCancelled(true);
					return;
				}
				dis.addBlockToList(e.getBlock(), e.getBlock().getState());
				return;
			}
		for (DisasterEvent dis : DisasterEvent.regeneratingDisasters)
			if (dis.damagedBlocks.containsKey(e.getBlock())) {
				if (!fireSpread) {
					e.setCancelled(true);
					return;
				}
				dis.addBlockToList(e.getBlock(), e.getBlock().getState());
				return;
			}
	}
	public void saveRegenBlocks(Main plugin) {
		File file = new File(plugin.getDataFolder().getAbsolutePath(), "pluginData/regenData.yml");
		file.getParentFile().mkdirs();
		if (file.exists())
			file.delete();
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		plugin.getLogger().info("Saving regen data DO NOT CLOSE! Or regen data will be lost!");
		for (Entry<RepeatingTask, DisasterEvent> entry : DisasterEvent.regeneratingTasks.entrySet())
			entry.getKey().cancel();
		Queue<DisasterEvent> pool = new ArrayDeque<>(DisasterEvent.ongoingDisasters);
		pool.forEach(d -> {
			d.reverseList();
			d.damagedBlocks.putAll(d.physicBlocks);
		});
		pool.addAll(DisasterEvent.regeneratingDisasters);
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
		int count = 1;
		for (DisasterEvent dis : pool) {
			String path = "d"+count;
			yaml.createSection(path);
			if (dis instanceof DestructionDisaster)
				yaml.set(path+".world", ((DestructionDisaster) dis).getLocation().getWorld().getUID().toString());
			else
				yaml.set(path+".world", ((WeatherDisaster) dis).getWorld().getUID().toString());
			yaml.set(path+".type", dis.type.toString());
			List<String> damageList = new ArrayList<>();
			for (Entry<Block, BlockState> entry : dis.damagedBlocks.entrySet()) {
				Location loc = entry.getKey().getLocation();
				damageList.add(loc.getBlockX()+"|"+loc.getBlockY()+"|"+loc.getBlockZ()+"|"+entry.getValue().getBlockData().getAsString(true)+(entry.getValue() instanceof TileState ? "|"+BlockStateParser.serialize((TileState) entry.getValue()) : ""));
			}
			yaml.set(path+".s", damageList);
			List<String> blockToBlockList = new ArrayList<>();
			for (Entry<Block, Block> entry : dis.blockToBlock.entrySet()) {
				Location loc = entry.getKey().getLocation();
				Location locTo = entry.getValue().getLocation();
				blockToBlockList.add(loc.getBlockX()+"|"+loc.getBlockY()+"|"+loc.getBlockZ()+"|"+locTo.getBlockX()+"|"+locTo.getBlockY()+"|"+locTo.getBlockZ());
			}
			yaml.set(path+".b", blockToBlockList);
			List<String> fallingList = new ArrayList<>();
			for (Entry<UUID, Block> entry : dis.UUIDToFalling.entrySet()) {
				Location block = dis.UUIDToFalling.get(entry.getKey()).getLocation();
				fallingList.add(entry.getKey().toString()+"|"+block.getBlockX()+"|"+block.getBlockY()+"|"+block.getBlockZ());
			}
			yaml.set(path+".e", fallingList);
			count++;
		}
		try {
			yaml.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		plugin.getLogger().info("Sucessfully finished and saved all regen data!");
	}
	public void readRegenData(Main plugin) {
		File file = new File(plugin.getDataFolder().getAbsolutePath(), "pluginData/regenData.yml");
		if (!file.exists())
			return;
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
		plugin.getLogger().info("Loading block regen data...");
		int[] count = {0};
		if (yaml.getKeys(false).isEmpty())
			plugin.getLogger().info("Successfully loaded all regen data.");
		for (String task : yaml.getKeys(false)) {
			DisasterEvent dis = new DisasterEvent();
			DisasterEvent.regeneratingDisasters.add(dis);
			World world = Bukkit.getWorld(UUID.fromString(yaml.getString(task+".world")));
			dis.world = world;
			dis.configFile = WorldObject.findWorldObject(world).configFile;
			dis.type = Disaster.forName(yaml.getString(task+".type"));
			if (world == null || dis.type == null)
				continue;
			count[0]++;
			for (String data : yaml.getStringList(task+".e")) {
				String[] parse = data.split("\\|");
				UUID uuid = UUID.fromString(parse[0]);
				dis.UUIDToFalling.put(uuid, new Location(world, Integer.parseInt(parse[1]), Integer.parseInt(parse[2]), Integer.parseInt(parse[3])).getBlock());
			}
			plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
				Map<Location, Pair<BlockData, String>> blockMap = new LinkedHashMap<>();
				Map<Location, Location> b2bMap = new HashMap<>();
				for (String raw : yaml.getStringList(task+".s")) {
					try {
						String[] parse = raw.split("\\|");
						Location loc = new Location(world, Integer.parseInt(parse[0]), Integer.parseInt(parse[1]), Integer.parseInt(parse[2]));
						blockMap.put(loc, Pair.of(Bukkit.createBlockData(parse[3]), parse.length >= 5 ? parse[4] : null));
					} catch (Exception e) {
						e.printStackTrace();
						Utils.sendDebugMessage();
					}
				}
				for (String raw : yaml.getStringList(task+".b")) {
					try {
						String[] parse = raw.split("\\|");
						b2bMap.put(new Location(world, Integer.parseInt(parse[0]), Integer.parseInt(parse[1]), Integer.parseInt(parse[2])), new Location(world, Integer.parseInt(parse[3]), Integer.parseInt(parse[4]), Integer.parseInt(parse[5])));
					} catch (Exception e) {
						e.printStackTrace();
						Utils.sendDebugMessage();
					}
				}
				plugin.getServer().getScheduler().runTask(plugin, () -> {
					blockMap.forEach((k, v) -> {
						Block block = k.getBlock();
						dis.threadSafeRegen.put(block, v);
						DisasterEvent.disasterBlocks.put(block, dis);
					});
					b2bMap.forEach((k, v) -> dis.blockToBlock.put(k.getBlock(), v.getBlock()));
					dis.continueRegen(plugin);
					count[0]--;
					if (count[0] <= 0)
						plugin.getLogger().info("Successfully loaded all regen data.");
				});
			});
		}
	}
}
