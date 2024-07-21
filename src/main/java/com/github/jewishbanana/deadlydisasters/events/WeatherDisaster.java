package com.github.jewishbanana.deadlydisasters.events;

import java.util.ArrayDeque;
import java.util.Queue;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.events.disasters.ExtremeWinds;
import com.github.jewishbanana.deadlydisasters.handlers.WorldObject;
import com.github.jewishbanana.deadlydisasters.utils.Metrics;
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
		this.configFile = plugin.getConfig();
	}
	public WeatherDisaster(int level, World world) {
		this.plugin = Main.getInstance();
		this.level = level;
		if (this.level > 5 && !(this instanceof ExtremeWinds))
			this.level = 5;
		this.configFile = WorldObject.findWorldObject(world).configFile;
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
