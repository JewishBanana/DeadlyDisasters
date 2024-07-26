package com.github.jewishbanana.deadlydisasters.events;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.events.disasters.ExtremeWinds;
import com.github.jewishbanana.deadlydisasters.handlers.DifficultyLevel;
import com.github.jewishbanana.deadlydisasters.handlers.WorldObject;
import com.github.jewishbanana.deadlydisasters.listeners.BlockRegenHandler;
import com.github.jewishbanana.deadlydisasters.utils.ChannelDataHolder;
import com.github.jewishbanana.deadlydisasters.utils.Metrics;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public abstract class WeatherDisaster extends DisasterEvent {
	
	public static Queue<World> currentWorlds = new ArrayDeque<>();
	
	protected int delay,time;
	public Main plugin;
	public double volume;
	public boolean RegionWeather;
	
	public WeatherDisaster(int level) {
		this.plugin = Main.getInstance();
		this.level = level;
		if (this.level > 5 && !(this instanceof ExtremeWinds))
			this.level = 5;
		this.worldObject = WorldObject.findWorldObject(Bukkit.getWorlds().get(0));
		this.configFile = worldObject.configFile;
		this.dropItems = configFile.getBoolean("regeneration.drop_container_items");
	}
	public WeatherDisaster(int level, World world) {
		this.plugin = Main.getInstance();
		this.level = level;
		if (this.level > 5 && !(this instanceof ExtremeWinds))
			this.level = 5;
		this.worldObject = WorldObject.findWorldObject(world);
		this.configFile = worldObject.configFile;
		this.dropItems = configFile.getBoolean("regeneration.drop_container_items");
	}
	public void triggerRegen(boolean reverse) {
		if (WorldObject.findWorldObject(world).difficulty != DifficultyLevel.CUSTOM || (int) WorldObject.findWorldObject(world).settings.get("regenDelay") < 0)
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
		task[0] = new RepeatingTask(plugin, (int) WorldObject.findWorldObject(world).settings.get("regenDelay") * 20, 1) {
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
			plugin.achievementsHandler.awardToWeatherSurvivalChannel(holder, value, achievement);
	}
	public void awardPlayersInSet(Set<ChannelDataHolder> set, int value, String achievement, int tierIndex) {
		if (!plugin.achievementsHandler.isEnabled)
			return;
		for (ChannelDataHolder holder : set)
			plugin.achievementsHandler.awardToWeatherSurvivalChannel(holder, value, achievement, tierIndex);
	}
	public void addPlayersToWeatherSurvivalChannel(World world, Set<ChannelDataHolder> set) {
		if (!plugin.achievementsHandler.isEnabled)
			return;
		for (Player player : world.getPlayers()) {
			if (Utils.isPlayerImmune(player))
				continue;
			ChannelDataHolder holder = new ChannelDataHolder(player);
			holder.setWorld(world);
			set.add(holder);
			plugin.achievementsHandler.addToWeatherSurvivalChannel(holder);
		}
	}
	public void removePlayersFromWeatherSurvivalChannel(Set<ChannelDataHolder> set) {
		for (ChannelDataHolder holder : set)
			plugin.achievementsHandler.removeFromWeatherSurvivalChannel(holder);
	}
	public Disaster getType() {
		return type;
	}
	public World getWorld() {
		return world;
	}
	public int getTime() {
		return time;
	}
	public void setTime(int ticks) {
		this.time = ticks;
	}
	public int getDelay() {
		return delay;
	}
	public void setDelay(int ticks) {
		this.delay = ticks;
	}
	public boolean isRegionWeatherEffects() {
		return RegionWeather;
	}
	public void setRegionWeatherEffects(boolean value) {
		RegionWeather = value;
	}
	public double getVolume() {
		return volume;
	}
	public void setVolume(double volume) {
		this.volume = volume;
	}
	public abstract void clear();
	public abstract void start(World world, Player p, boolean broadcastAllowed);
	
	public void updateWeatherSettings() {
		RegionWeather = (boolean) WorldObject.findWorldObject(world).settings.get("ignore_weather_effects_in_regions");
	}
	public void createTimedStart(int delaySeconds, World world, Player p) {
		this.world = world;
		currentWorlds.add(world);
		inputPlayerToMap(delaySeconds, p);
		plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
			@Override
			public void run() {
				currentWorlds.remove(world);
				WorldObject wo = WorldObject.findWorldObject(world);
				if (Utils.isPlayerImmune(p) || !wo.naturalAllowed ||!wo.allowed.contains(type))
					return;
				start(world, p, true);
				Metrics.incrementValue(Metrics.disasterOccurredMap, type.getMetricsLabel());
			}
		}, (delaySeconds * 20) - delay);
	}
}
