package com.github.jewishbanana.deadlydisasters.events;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.handlers.DifficultyLevel;
import com.github.jewishbanana.deadlydisasters.handlers.WorldObject;
import com.github.jewishbanana.deadlydisasters.listeners.BlockRegenHandler;
import com.github.jewishbanana.deadlydisasters.utils.ChannelDataHolder;
import com.github.jewishbanana.deadlydisasters.utils.Metrics;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public abstract class DestructionDisaster extends DisasterEvent {
	
	protected Location loc;
	protected Player p;
	public Main plugin;
	public double volume;
	
	public static Map<World,Queue<Player>> currentLocations = new HashMap<>();
	
	public DestructionDisaster(int level, World world) {
		this.plugin = Main.getInstance();
		this.level = level;
		this.world = world;
		this.worldObject = WorldObject.findWorldObject(world);
		this.configFile = worldObject.configFile;
		this.dropItems = configFile.getBoolean("regeneration.drop_container_items");
	}
	public Disaster getType() {
		return type;
	}
	public abstract void start(Location loc, Player p);
	
	public void broadcastMessage(Location temp, Player p) {
		if ((boolean) WorldObject.findWorldObject(temp.getWorld()).settings.get("event_broadcast"))
			Utils.broadcastEvent(level, "destructive", type, temp, p);
	}
	public abstract void startAdjustment(Location loc, Player p);
	public abstract Location findApplicableLocation(Location temp, Player p);
	
	public void createTimedStart(int delaySeconds, Vector offset, Player p) {
		this.loc = p.getLocation();
		if (!currentLocations.containsKey(loc.getWorld()))
			currentLocations.put(loc.getWorld(), new ArrayDeque<>());
		currentLocations.get(loc.getWorld()).add(p);
		inputPlayerToMap(delaySeconds, p);
		plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
			public void run() {
				if (!currentLocations.containsKey(loc.getWorld()) || !currentLocations.get(loc.getWorld()).contains(p))
					return;
				currentLocations.get(loc.getWorld()).remove(p);
				if (currentLocations.get(loc.getWorld()).isEmpty())
					currentLocations.remove(loc.getWorld());
				if (!p.isOnline())
					return;
				WorldObject wo = WorldObject.findWorldObject(p.getWorld());
				if (Utils.isPlayerImmune(p) || !wo.naturalAllowed || !wo.allowed.contains(type))
					return;
				Location temp = findApplicableLocation(p.getLocation().clone().add(offset), p);
				if (temp == null)
					return;
				if ((boolean) wo.settings.get("event_broadcast") && type != Disaster.GEYSER && type != Disaster.PURGE && type != Disaster.INFESTEDCAVES)
					Utils.broadcastEvent(level, "destructive", type, temp, p);
				if (currentLocations.containsKey(p.getWorld())) {
					for (Entity e : p.getNearbyEntities(wo.maxRadius, wo.maxRadius, wo.maxRadius))
						if (e instanceof Player && currentLocations.get(p.getWorld()).contains(e))
							currentLocations.get(p.getWorld()).remove(e);
					if (currentLocations.get(p.getWorld()).isEmpty())
						currentLocations.remove(p.getWorld());
				}
				plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
					public void run() {
						start(temp, p);
						Metrics.incrementValue(Metrics.disasterOccurredMap, type.getMetricsLabel());
					}
				}, type.getDelayTicks());
			}
		}, delaySeconds * 20);
	}
	public void triggerRegen(boolean reverse) {
		if (WorldObject.findWorldObject(loc.getWorld()).difficulty != DifficultyLevel.CUSTOM || (int) WorldObject.findWorldObject(loc.getWorld()).settings.get("regenDelay") < 0)
			return;
		UUIDToFalling.forEach((k, v) -> {
			if (Bukkit.getEntity(k) != null)
				Bukkit.getEntity(k).remove();
		});
		regeneratingDisasters.add(this);
		DisasterEvent instance = this;
		if (reverse)
			reverseList();
		damagedBlocks.putAll(physicBlocks);
		final double regenRate = type.getDefaultRegenTickRate() * configFile.getDouble(type.name().toLowerCase()+".regen_rate");
		double[] regenTicks = {0};
		Map<Block, ItemStack[]> inventories = new HashMap<>();
		RepeatingTask[] task = new RepeatingTask[1];
		task[0] = new RepeatingTask(plugin, (int) WorldObject.findWorldObject(loc.getWorld()).settings.get("regenDelay") * 20, 1) {
			@Override
			public void run() {
				regenTicks[0] += regenRate;
				while (regenTicks[0] >= 1) {
					regenTicks[0] -= 1;
					if (damagedBlocks.isEmpty()) {
						cancel();
						regeneratingTasks.remove(task[0]);
						regeneratingDisasters.remove(instance);
						BlockRegenHandler.gravityHold.removeAll(gravityHoldBlocks);
						break;
					}
					try {
						Entry<Block, BlockState> entry = damagedBlocks.entrySet().iterator().next();
						Block b = entry.getKey();
						Iterator<Entry<Block, Block>> it = blockToBlock.entrySet().iterator();
						while (it.hasNext()) {
							Entry<Block, Block> btb = it.next();
							if (btb.getKey().equals(b)) {
								Block toBlock = btb.getValue();
								if (b.equals(toBlock) || worldObject.blacklistedRegenBlocks.contains(toBlock.getType())) {
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
						if (Utils.isMaterialGravity(entry.getValue().getType())) {
							gravityHoldBlocks.add(b);
							BlockRegenHandler.gravityHold.add(b);
						}
						if (!worldObject.blacklistedRegenBlocks.contains(entry.getValue().getType())) {
							entry.getValue().update(true);
							if (inventories.containsKey(b))
								plugin.getServer().getScheduler().runTaskLater(plugin, () -> ((InventoryHolder) b.getState()).getInventory().setContents(inventories.get(b)), 7);
						}
						damagedBlocks.remove(b);
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
	public void awardPlayersInSet(Set<ChannelDataHolder> set, int value, String achievement) {
		if (!plugin.achievementsHandler.isEnabled)
			return;
		for (ChannelDataHolder holder : set)
			plugin.achievementsHandler.awardToSurvivalChannel(holder, value, achievement);
	}
	public void awardPlayersInSet(Set<ChannelDataHolder> set, int value, String achievement, int tierIndex) {
		if (!plugin.achievementsHandler.isEnabled)
			return;
		for (ChannelDataHolder holder : set)
			plugin.achievementsHandler.awardToSurvivalChannel(holder, value, achievement, tierIndex);
	}
	public void addPlayersToSurvivalChannel(Location loc, double radius, Set<ChannelDataHolder> set) {
		if (!plugin.achievementsHandler.isEnabled)
			return;
		for (Entity player : loc.getWorld().getNearbyEntities(loc, radius, radius, radius, e -> e instanceof Player && !Utils.isPlayerImmune((Player) e))) {
			ChannelDataHolder holder = new ChannelDataHolder((Player) player);
			set.add(holder);
			plugin.achievementsHandler.addToSurvivalChannel(holder);
		}
	}
	public void removePlayersFromSurvivalChannel(Set<ChannelDataHolder> set) {
		for (ChannelDataHolder holder : set)
			plugin.achievementsHandler.removeFromSurvivalChannel(holder);
	}
	public double getVolume() {
		return volume;
	}
	public void setVolume(double volume) {
		this.volume = volume;
	}
	public Location getLocation() {
		return loc;
	}
	public void setLocation(Location loc) {
		this.loc = loc;
	}
	public Player getP() {
		return p;
	}
	public void setP(Player p) {
		this.p = p;
	}
}
