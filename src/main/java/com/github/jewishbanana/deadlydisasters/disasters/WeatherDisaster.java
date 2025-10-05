package com.github.jewishbanana.deadlydisasters.disasters;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import com.github.jewishbanana.deadlydisasters.listeners.WorldListener;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.mojang.datafixers.util.Pair;

import io.papermc.lib.PaperLib;

public abstract class WeatherDisaster extends Disaster {
	
	protected static final double smoothingRangeExcess = 16.0;
	
	protected Set<UUID> weatherPlayers = ConcurrentHashMap.newKeySet();
	
	protected int time;
	protected float scale;
	protected volatile double currentStrength;
	protected float particleMultiplier;
	protected float smoothingRange;
	protected float smoothingIntensity;
	protected float weatherEffectsRange;
	protected float particleRenderDistance;
	protected int soundTickRate;
	protected int soundTick;
	
	public final Set<Chunk> involvedChunks = ConcurrentHashMap.newKeySet();

	public WeatherDisaster(@NotNull Location location, Player player, int level) {
		super(location, player, level);
		
		this.smoothingRange = (float) (Math.max(30, level * 20));
		this.smoothingIntensity = (float) (0.15 * level);
	}
	public void init() {
		super.init();
		if (getConfigPath() != null) {
			this.time = (int) (getConfigOverrideDouble("time.level_"+level) * 20.0);
			this.disasterRange = getConfigOverrideDouble("range.level_"+level);
			this.scale = (float) getConfigOverrideDouble("scaling.level_"+level);
			this.particleMultiplier = (float) getConfigOverrideDouble("particle_multiplier");
			this.particleRenderDistance = (float) getConfigOverrideDouble("particle_render_distance");
		}
		this.weatherEffectsRange = (float) (disasterRange + (smoothingRange / 2.0));
	}
	public void start() {
		super.start();
		if (getConfigPath() != null)
			addDeathWatcher("deaths." + getConfigPath().split("\\.")[2]);
		scheduleTask(new BukkitRunnable() {
			@Override
			public void run() {
				if (time <= 200) {
					currentStrength = Utils.clamp(currentStrength - 0.005, 0.0, 1.0);
					return;
				}
				if (currentStrength != 1.0)
					currentStrength = Utils.clamp(currentStrength + 0.005, 0.0, 1.0);
			}
		}.runTaskTimer(plugin, 0, 1));
	}
	public void clean() {
		super.clean();
		time = 0;
		WorldListener.removeChunkListener(this);
		removeDeathWatcher(0);
		weatherPlayers.forEach(uuid -> {
			Player player = Bukkit.getPlayer(uuid);
			if (player == null || !player.isOnline())
				return;
			player.resetPlayerWeather();
		});
	}
	public void createParticleAsyncTask(Consumer<Player> inStorm, Consumer<Pair<Player, Double>> smoothingArea, Predicate<Location> conditions) {
		scheduleTask(new BukkitRunnable() {
			private final World world = location.getWorld();
			private final double trueRadiusSquared = (disasterRange + smoothingRange + smoothingRangeExcess) * (disasterRange + smoothingRange + smoothingRangeExcess);
			private final double rangeSquared = disasterRange * disasterRange;
			private final double smoothingAdjustment = smoothingIntensity / smoothingRange;
			private final double weatherRadiusSquared = weatherEffectsRange * weatherEffectsRange;
			
			@Override
			public void run() {
				Set<UUID> set = new HashSet<>();
				for (Player player : world.getPlayers()) {
					if (!player.isOnline())
						continue;
					Location loc = player.getLocation();
					loc.setY(location.getY());
					final double distance = loc.distanceSquared(location);
					if (!loc.getWorld().equals(world) || distance > trueRadiusSquared)
						continue;
					if (distance <= weatherRadiusSquared && conditions.test(loc)) {
						UUID uuid = player.getUniqueId();
						set.add(uuid);
						if (!weatherPlayers.contains(uuid))
							addPlayerToWeather(player);
						if (forceDownfallWeather() && player.getPlayerWeather() != WeatherType.DOWNFALL)
							player.setPlayerWeather(WeatherType.DOWNFALL);
					}
					if (distance > rangeSquared)
						smoothingArea.accept(Pair.of(player, Utils.clamp(smoothingIntensity - ((loc.distance(location) - disasterRange) * smoothingAdjustment), smoothingIntensity / 4.0, smoothingIntensity)));
					else
						inStorm.accept(player);
				}
				weatherPlayers.removeAll(set);
				weatherPlayers.forEach(uuid -> {
					Player player = Bukkit.getPlayer(uuid);
					if (player == null || !player.isOnline())
						return;
					removePlayerFromWeather(player);
				});
				weatherPlayers.clear();
				weatherPlayers.addAll(set);
				if (soundTick++ == soundTickRate)
					soundTick = 0;
			}
		}.runTaskTimerAsynchronously(plugin, 0, 1));
	}
	public void createParticleAsyncTask(Consumer<Player> inStorm, Consumer<Pair<Player, Double>> smoothingArea) {
		createParticleAsyncTask(inStorm, smoothingArea, loc -> true);
	}
	public void addPlayerToWeather(Player player) {
		weatherPlayers.add(player.getUniqueId());
		if (forceDownfallWeather())
			player.setPlayerWeather(WeatherType.DOWNFALL);
	}
	public void removePlayerFromWeather(Player player) {
		if (forceDownfallWeather())
			player.resetPlayerWeather();
	}
	public boolean forceDownfallWeather() {
		return true;
	}
	public void getChunksInvolvedSafelyAndThen(Runnable function, boolean generateChunks) {
		final double radiusSquared = disasterRange * disasterRange;
		final int chunkX = location.getChunk().getX();
		final int chunkZ = location.getChunk().getZ();
		final int chunkRadius = (int) Math.ceil(disasterRange / 16.0);
		final Set<Chunk> chunks = new HashSet<>();
		if (PaperLib.isPaper()) {
			final WeatherDisaster reference = this;
			new BukkitRunnable() {
				@Override
				public void run() {
					for (int x = -chunkRadius; x <= chunkRadius; x++)
						for (int z = -chunkRadius; z <= chunkRadius; z++) {
							int currentX = chunkX + x;
							int currentZ = chunkZ + z;
							double deltaX = location.getBlockX() - (Utils.clamp(location.getBlockX(), currentX * 16, (currentX + 1) * 16 - 1));
							double deltaZ = location.getBlockZ() - (Utils.clamp(location.getBlockZ(), currentZ * 16, (currentZ + 1) * 16 - 1));
							if (deltaX * deltaX + deltaZ * deltaZ <= radiusSquared)
								try {
									Chunk chunk = PaperLib.getChunkAtAsync(location.getWorld(), currentX, currentZ, generateChunks).get();
									if (chunk != null)
										chunks.add(chunk);
									else
										WorldListener.addChunkListener(reference, currentX, currentZ);
								} catch (InterruptedException | ExecutionException e) {
									Utils.sendExceptionLog(e);
								}
						}
					involvedChunks.addAll(chunks);
					function.run();
				}
			}.runTaskAsynchronously(plugin);
		} else {
			for (int x = -chunkRadius; x <= chunkRadius; x++)
				for (int z = -chunkRadius; z <= chunkRadius; z++) {
					int currentX = chunkX + x;
					int currentZ = chunkZ + z;
					double deltaX = location.getBlockX() - (Utils.clamp(location.getBlockX(), currentX * 16, (currentX + 1) * 16 - 1));
					double deltaZ = location.getBlockZ() - (Utils.clamp(location.getBlockZ(), currentZ * 16, (currentZ + 1) * 16 - 1));
					if (deltaX * deltaX + deltaZ * deltaZ <= radiusSquared) {
						if (generateChunks || location.getWorld().isChunkLoaded(currentX, currentZ))
							chunks.add(location.getWorld().getChunkAt(currentX, currentZ, generateChunks));
						else
							WorldListener.addChunkListener(this, currentX, currentZ);
					}
				}
			involvedChunks.addAll(chunks);
			function.run();
		}
	}
	public void getChunksInvolvedSafelyAndThen(Runnable function) {
		getChunksInvolvedSafelyAndThen(function, false);
	}
	protected int getConfigOverrideInt(String path) {
		if (getWorldLink().getConfig().contains(getConfigPath()+'.'+path, true))
			return getConfigInt(path);
		if (getWorldLink().getConfig().contains("disasters.weather.global_weather."+path, true))
			return getWorldLink().getConfigInt("disasters.weather.global_weather."+path);
		return getWorldLink().getConfigInt("disasters.global."+path);
	}
	protected double getConfigOverrideDouble(String path) {
		if (getWorldLink().getConfig().contains(getConfigPath()+'.'+path, true))
			return getConfigDouble(path);
		if (getWorldLink().getConfig().contains("disasters.weather.global_weather."+path, true))
			return getWorldLink().getConfigDouble("disasters.weather.global_weather."+path);
		return getWorldLink().getConfigDouble("disasters.global."+path);
	}
	protected List<String> getConfigOverrideStringList(String path) {
		if (getWorldLink().getConfig().contains(getConfigPath()+'.'+path, true))
			return getConfigStringList(path);
		if (getWorldLink().getConfig().contains("disasters.weather.global_weather."+path, true))
			return getWorldLink().getConfigStringList("disasters.weather.global_weather."+path);
		return getWorldLink().getConfigStringList("disasters.global."+path);
	}
	public Set<PotionEffect> buildPotionEffects(String path) {
		ConfigurationSection section = getConfigSection(path);
		if (section == null)
			return Set.of();
		Set<PotionEffect> set = new HashSet<>();
		for (String effect : section.getKeys(false)) {
			PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(effect));
			if (type == null) {
				Utils.sendConsoleMessage("&eWARNING the potion effect &d'"+effect+"' &edoes not exist in the world disaster config &b'"+getWorldLink().getConfigName()+"' &eat the section &c'"+getConfigPath()+'.'+path+"'&e!");
				continue;
			}
			ConfigurationSection effectSection = DataUtils.getConfigSection(getWorldLink().getConfig(), getWorldLink().getConfigName(), section.getCurrentPath()+'.'+effect);
			if (effectSection == null)
				continue;
			int ticks = DataUtils.getConfigInt(getWorldLink().getConfig(), getWorldLink().getConfigName(), effectSection.getCurrentPath()+".ticks", 0);
			if (ticks <= 0)
				continue;
			int level = effectSection.contains("level", true) ? DataUtils.getConfigInt(getWorldLink().getConfig(), getWorldLink().getConfigName(), effectSection.getCurrentPath()+".level", 0) : 1;
			if (level < 1)
				continue;
			boolean particles = effectSection.contains("particles", true) ? DataUtils.getConfigBoolean(getWorldLink().getConfig(), getWorldLink().getConfigName(), effectSection.getCurrentPath()+".particles", true) : true;
			boolean icon = effectSection.contains("icon", true) ? DataUtils.getConfigBoolean(getWorldLink().getConfig(), getWorldLink().getConfigName(), effectSection.getCurrentPath()+".icon", true) : true;
			set.add(new PotionEffect(type, ticks, level - 1, true, particles, icon));
		}
		return Set.copyOf(set);
	}
	public String getBroadcastMessageConfigPath() {
		return "messages.disaster_broadcasts.weather.level_"+level;
	}
	public String getDisplayName() {
		return Utils.convertString(DataUtils.getLanguageString(getConfigPath()));
	}
	public void placeDebugRings() {
		BlockUtils.getBlocksInCircleCircumference(new Location(location.getWorld(), location.getX(), 110, location.getZ()), disasterRange).forEach(b -> b.setType(Material.GREEN_WOOL));
		BlockUtils.getBlocksInCircleCircumference(new Location(location.getWorld(), location.getX(), 111, location.getZ()), disasterRange + smoothingRange).forEach(b -> b.setType(Material.YELLOW_WOOL));
		BlockUtils.getBlocksInCircleCircumference(new Location(location.getWorld(), location.getX(), 112, location.getZ()), disasterRange + smoothingRange + smoothingRangeExcess).forEach(b -> b.setType(Material.RED_WOOL));
		BlockUtils.getBlocksInCircleCircumference(new Location(location.getWorld(), location.getX(), 113, location.getZ()), weatherEffectsRange).forEach(b -> b.setType(Material.WHITE_WOOL));
		new BukkitRunnable() {
			@Override
			public void run() {
				BlockUtils.getBlocksInCircleCircumference(new Location(location.getWorld(), location.getX(), 110, location.getZ()), disasterRange).forEach(b -> b.setType(Material.AIR));
				BlockUtils.getBlocksInCircleCircumference(new Location(location.getWorld(), location.getX(), 111, location.getZ()), disasterRange + smoothingRange).forEach(b -> b.setType(Material.AIR));
				BlockUtils.getBlocksInCircleCircumference(new Location(location.getWorld(), location.getX(), 112, location.getZ()), disasterRange + smoothingRange + smoothingRangeExcess).forEach(b -> b.setType(Material.AIR));
				BlockUtils.getBlocksInCircleCircumference(new Location(location.getWorld(), location.getX(), 113, location.getZ()), weatherEffectsRange).forEach(b -> b.setType(Material.AIR));
			}
		}.runTaskLater(plugin, time);
	}
}
