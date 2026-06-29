package com.github.jewishbanana.deadlydisasters.disasters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.DeadlyDisasters;
import com.github.jewishbanana.deadlydisasters.WorldWrapper;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class DisasterSelector {
	
	private final RandomGenerator random;
	
	public final Map<UUID, Map<UUID, Integer>> playerTimers = new ConcurrentHashMap<>();
	public final Map<UUID, Integer> worldTimers = new ConcurrentHashMap<>();
	
	public DisasterSelector(DeadlyDisasters plugin) {
		this.random = Utils.getRandomGenerator();
		FileConfiguration data = DataUtils.getDataFile();
		ConfigurationSection section = data.getConfigurationSection("timers");
		if (section == null)
			section = data.createSection("timers");
		ConfigurationSection playerSection = section.getConfigurationSection("players");
		if (playerSection == null)
			playerSection = section.createSection("players");
		for (String worldID : playerSection.getKeys(false))
			for (String playerID : playerSection.getConfigurationSection(worldID).getKeys(false))
				playerTimers.computeIfAbsent(UUID.fromString(worldID), m -> new ConcurrentHashMap<>()).put(UUID.fromString(playerID), DataUtils.getDataFileInt(playerSection.getCurrentPath()+'.'+worldID+'.'+playerID));
		ConfigurationSection worldsSection = section.getConfigurationSection("worlds");
		if (worldsSection == null)
			worldsSection = section.createSection("worlds");
		for (String worldID : worldsSection.getKeys(false))
			worldTimers.put(UUID.fromString(worldID), DataUtils.getDataFileInt(worldsSection.getCurrentPath()+'.'+worldID));
		
		new BukkitRunnable() {
			@Override
			public void run() {
				Bukkit.getWorlds().forEach(world -> {
					WorldWrapper wrapper = WorldWrapper.getWorldWrapper(world);
					if (wrapper == null)
						return;
					switch (wrapper.targetingMode) {
					// Disabled
					default:
					case 0:
						break;
					// Individual
					case 1:
						for (Player player : world.getPlayers()) {
							if (!isEligibleNaturalTarget(player, wrapper))
								continue;
							UUID uuid = player.getUniqueId();
							Map<UUID, Integer> worldMap = playerTimers.computeIfAbsent(world.getUID(), m -> new ConcurrentHashMap<>());
							worldMap.compute(uuid, (k, timer) -> {
								if (k == null || timer == null)
									return random.nextInt(wrapper.minimumTime, wrapper.maximumTime + 1);
								if (timer > 0)
									return timer - 1;
								if (timer == -1)
									return timer;
								plugin.getServer().getScheduler().runTask(plugin, () -> {
									if (isEligibleNaturalTarget(player, wrapper) && selectDisaster(player, wrapper)) {
										worldMap.replace(uuid, random.nextInt(wrapper.minimumTime, wrapper.maximumTime + 1));
										Location playerLoc = player.getLocation();
										final double distance = wrapper.sharedDisasterRadius * wrapper.sharedDisasterRadius;
										world.getPlayers().forEach(nearby -> {
											if (nearby.equals(player) || !nearby.isValid() || EntityUtils.isPlayerImmune(nearby) || (wrapper.blacklistedPlayers != null && wrapper.blacklistedPlayers.contains(nearby.getUniqueId()))
													|| !Utils.isLocationsWithinDistance(nearby.getLocation(), playerLoc, distance))
												return;
											worldMap.put(nearby.getUniqueId(), random.nextInt(wrapper.minimumTime, wrapper.maximumTime + 1));
										});
									} else
										worldMap.replace(uuid, 10);
								});
								return -1;
							});
						}
						break;
					// Global
					case 2:
						UUID worldID = world.getUID();
						int timer = worldTimers.computeIfAbsent(worldID, val -> random.nextInt(wrapper.minimumTime, wrapper.maximumTime + 1));
						if (timer > 0)
							worldTimers.replace(worldID, timer - 1);
						else if (timer == 0) {
							worldTimers.replace(worldID, -1);
							plugin.getServer().getScheduler().runTask(plugin, () -> {
								List<Player> players = world.getPlayers();
								Collections.shuffle(players);
								int disasterCount = wrapper.getConfigInt("world.global_disaster_count");
								for (Player player : players)
									if (isEligibleNaturalTarget(player, wrapper) && selectDisaster(player, wrapper)) {
										if (--disasterCount > 0)
											continue;
										worldTimers.replace(worldID, random.nextInt(wrapper.minimumTime, wrapper.maximumTime + 1));
										return;
									}
								worldTimers.replace(worldID, 10);
							});
						}
						break;
					}
				});
			}
		}.runTaskTimer(plugin, 0, 20);
	}
	private boolean isEligibleNaturalTarget(Player player, WorldWrapper wrapper) {
		if (EntityUtils.isPlayerImmune(player) || !player.isValid())
			return false;
		UUID uuid = player.getUniqueId();
		return (wrapper.blacklistedPlayers == null || !wrapper.blacklistedPlayers.contains(uuid)) && !DependencyUtils.isDisasterStartBlocked(player.getLocation());
	}
	public boolean selectDisaster(Player player, WorldWrapper wrapper) {
		try {
			Location loc = player.getLocation().add(new Vector(random.nextFloat(-wrapper.disasterOffset, wrapper.disasterOffset), 0, random.nextFloat(-wrapper.disasterOffset, wrapper.disasterOffset)));
			final int level = wrapper.rollLevel(random);
			final List<DisasterRegistry> registers = new ArrayList<>(DisasterRegistry.getRegisteredDisasters());
			Collections.shuffle(registers);
			for (DisasterRegistry registry : registers) {
				if (wrapper.disabledDisasters.contains(registry))
					continue;
				Disaster disaster = registry.createDisaster(loc, player, level, false);
				if (!disaster.softStart())
					continue;
				return true;
			}
			return false;
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			return false;
		}
	}
	/**
	 * Seconds until the next disaster is rolled for {@code player} in their current world, or {@code -1} if disasters are
	 * disabled there, no timer is running yet, or one is currently mid-selection ({@code -1} sentinel). Used by
	 * UltimateContent's tameable baby end totem to warn the player as a disaster approaches.
	 */
	public int getSecondsUntilDisaster(Player player) {
		World world = player.getWorld();
		WorldWrapper wrapper = WorldWrapper.getWorldWrapper(world);
		if (wrapper == null)
			return -1;
		switch (wrapper.targetingMode) {
		// Individual
		case 1: {
			Map<UUID, Integer> worldMap = playerTimers.get(world.getUID());
			if (worldMap == null)
				return -1;
			Integer timer = worldMap.get(player.getUniqueId());
			return timer == null || timer < 0 ? -1 : timer;
		}
		// Global
		case 2: {
			Integer timer = worldTimers.get(world.getUID());
			return timer == null || timer < 0 ? -1 : timer;
		}
		default:
			return -1;
		}
	}
	public void saveData() {
		DataUtils.writeToDataFile(file -> {
			playerTimers.forEach((worldID, map) -> map.forEach((playerID, timer) -> file.set("timers.players."+worldID.toString()+'.'+playerID.toString(), timer)));
			worldTimers.forEach((worldID, timer) -> file.set("timers.worlds."+worldID.toString(), timer));
		});
	}
	public void refreshWorldTimers(World world) {
		WorldWrapper wrapper = WorldWrapper.getWorldWrapper(world);
		if (wrapper == null)
			return;
		Map<UUID, Integer> players = playerTimers.get(world.getUID());
		if (players != null)
			players.forEach((player, timer) -> players.replace(player, random.nextInt(wrapper.minimumTime, wrapper.maximumTime + 1)));
		Integer worldTimer = worldTimers.get(world.getUID());
		if (worldTimer != null)
			worldTimers.replace(world.getUID(), random.nextInt(wrapper.minimumTime, wrapper.maximumTime + 1));
	}
}
