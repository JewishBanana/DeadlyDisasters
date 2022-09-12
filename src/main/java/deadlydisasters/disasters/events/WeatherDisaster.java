package deadlydisasters.disasters.events;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import deadlydisasters.disasters.Disaster;
import deadlydisasters.disasters.ExtremeWinds;
import deadlydisasters.general.DifficultyLevel;
import deadlydisasters.general.Main;
import deadlydisasters.general.WorldObject;
import deadlydisasters.utils.Metrics;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public abstract class WeatherDisaster extends DisasterEvent {
	
	public static Queue<World> currentWorlds = new ArrayDeque<>();
	
	protected World world;
	protected int delay,time;
	public Main plugin;
	public double volume;
	public boolean RegionWeather;
	
	public WeatherDisaster(int level) {
		this.plugin = Main.getInstance();
		this.level = level;
		if (this.level > 5 && !(this instanceof ExtremeWinds))
			this.level = 5;
	}
	public void triggerRegen() {
		if (WorldObject.findWorldObject(world).difficulty != DifficultyLevel.CUSTOM || (int) WorldObject.findWorldObject(world).settings.get("regenDelay") < 0)
			return;
		DisasterEvent instance = this;
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				reverseList();
				final double regenRate = type.getRegenTickRate();
				double[] regenTicks = {0};
				RepeatingTask task = new RepeatingTask(plugin, (int) WorldObject.findWorldObject(world).settings.get("regenDelay") * 20, 1) {
					@Override
					public void run() {
						regenTicks[0] += regenRate;
						while (regenTicks[0] >= 1) {
							regenTicks[0] -= 1;
							if (damagedBlocks.isEmpty()) {
								cancel();
								regeneratingTasks.remove(this);
								return;
							}
							Entry<Block, Material[]> entry = damagedBlocks.entrySet().iterator().next();
							Block b = entry.getKey();
							Material material = entry.getValue()[1];
							if (b.getType() == material || ((b.getType() == Material.WATER || b.getType() == Material.LAVA) && ((Levelled) b.getBlockData()).getLevel() > 0)
									|| b.getType() == Material.SNOW || material == Material.FIRE) {
								b.setType(entry.getValue()[0]);
								if (b.getType() != Material.AIR)
									b.setBlockData(blocksData.get(b));
								blocksData.remove(b);
								if (!b.isPassable())
									for (Entity e : b.getWorld().getNearbyEntities(b.getLocation().clone().add(0.5, 0.5, 0.5), .5, .5, .5))
										if (e.getVelocity().getY() < 0.4)
											e.setVelocity(e.getVelocity().setY(0.4));
							}
							damagedBlocks.remove(entry.getKey());
						}
					}
				};
				Map<DisasterEvent, Map<Block,Material[]>> tempMap = new HashMap<>();
				tempMap.put(instance, damagedBlocks);
				regeneratingTasks.put(task, tempMap);
			}
		});
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
