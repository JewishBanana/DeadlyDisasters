package deadlydisasters.general;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import deadlydisasters.disasters.AcidStorm;
import deadlydisasters.disasters.BlackPlague;
import deadlydisasters.disasters.Blizzard;
import deadlydisasters.disasters.CaveIn;
import deadlydisasters.disasters.CustomDisaster;
import deadlydisasters.disasters.Disaster;
import deadlydisasters.disasters.Earthquake;
import deadlydisasters.disasters.EndStorm;
import deadlydisasters.disasters.ExtremeWinds;
import deadlydisasters.disasters.Geyser;
import deadlydisasters.disasters.Hurricane;
import deadlydisasters.disasters.MeteorShower;
import deadlydisasters.disasters.Purge;
import deadlydisasters.disasters.SandStorm;
import deadlydisasters.disasters.Sinkhole;
import deadlydisasters.disasters.SoulStorm;
import deadlydisasters.disasters.Supernova;
import deadlydisasters.disasters.Tornado;
import deadlydisasters.disasters.Tsunami;
import deadlydisasters.listeners.DeathMessages;
import deadlydisasters.utils.Utils;

public class TimerCheck {

	public Map<UUID,Map<UUID,Integer>> timer = new ConcurrentHashMap<UUID,Map<UUID,Integer>>();

	private FileConfiguration dataFile;
	private Main plugin;
	private Random rand;
	
	private Set<Biome> oceanBiomes = new HashSet<>();
	
	private SeasonsHandler seasonsHandler;
	private boolean seasonsActive;

	public TimerCheck(Main plugin, FileConfiguration data, Random rand) { //https://pokechu22.github.io/Burger/1.19.html#sounds
		this.dataFile = data;
		this.plugin = plugin;
		this.rand = rand;

		for (World world : Bukkit.getWorlds())
			if (!timer.containsKey(world.getUID()))
				timer.put(world.getUID(), new HashMap<UUID,Integer>());
		if (!dataFile.contains("timers"))
			dataFile.createSection("timers");
		refreshTimerList();
		
		oceanBiomes.addAll(Arrays.asList(Biome.OCEAN, Biome.COLD_OCEAN, Biome.DEEP_COLD_OCEAN, Biome.DEEP_FROZEN_OCEAN, Biome.DEEP_LUKEWARM_OCEAN, Biome.DEEP_OCEAN, Biome.FROZEN_OCEAN, Biome.LUKEWARM_OCEAN,
				Biome.WARM_OCEAN, Biome.RIVER, Biome.FROZEN_RIVER));

		startTimer(plugin);
		plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
			public void run() {
				seasonsHandler = plugin.seasonsHandler;
				seasonsActive = plugin.seasonsHandler.isActive;
			}
		}, 2);
	}
	public void startTimer(Main plugin) {
		Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				for (Player all : Bukkit.getServer().getOnlinePlayers()) {
					if (Utils.isPlayerImmune(all))
						continue;
					UUID uuid = all.getUniqueId();
					World world = all.getWorld();
					WorldObject worldObj = WorldObject.findWorldObject(world);
					if (worldObj.whitelist.contains(uuid) || !worldObj.naturalAllowed)
						continue;
					if (!timer.containsKey(world.getUID()) || !timer.get(world.getUID()).containsKey(uuid))
						continue;
					int value = timer.get(world.getUID()).get(uuid);
					if (value > 0) {
						timer.get(world.getUID()).replace(uuid, value - 1);
						continue;
					}

					final int MinTime = (int) worldObj.timer;
					int[] offset = {worldObj.offset};
					if (offset[0] <= 0)
						offset[0] = 1;
					Vector offVec = new Vector(rand.nextInt(offset[0]*2)-offset[0], 0, rand.nextInt(offset[0]*2)-offset[0]);
					
					int[] tempLevel = {worldObj.simulateLevel(rand)};
					if (tempLevel[0] == 6 && !(boolean)worldObj.settings.get("level_six"))
						tempLevel[0] = 5;

					if (worldObj.allowed.contains(Disaster.CUSTOM) && plugin.getConfig().getDouble("custom.frequency") > rand.nextDouble()) {
						List<YamlConfiguration> disasters = new ArrayList<>(CustomDisaster.disasterFiles.values());
						String environment = all.getWorld().getEnvironment().toString().toLowerCase();
						int height = all.getLocation().getBlockY();
						for (Iterator<YamlConfiguration> iterator = disasters.iterator(); iterator.hasNext();) {
							YamlConfiguration yaml = iterator.next();
							if (!yaml.getBoolean("settings.natural") || (yaml.contains("settings.frequency") && yaml.getDouble("settings.frequency") < rand.nextDouble())
									|| !yaml.getStringList("settings.environments").contains(environment) || (yaml.contains("settings.min_height") && yaml.getInt("settings.min_height") > height)
									|| (yaml.contains("settings.max_height") && yaml.getInt("settings.max_height") < height))
								iterator.remove();
						}
						if (!disasters.isEmpty()) {
							YamlConfiguration yaml = disasters.get(rand.nextInt(disasters.size()));
							boolean[] levels = {false, false, false, false, false, false};
							for (String section : yaml.getConfigurationSection("core").getKeys(false))
								levels[Integer.parseInt(section.substring(6))-1] = true;
							if (!levels[tempLevel[0]-1])
								for (int i=0; i < 6; i++)
									if (levels[i])
										tempLevel[0] = i+1;
							CustomDisaster custom = new CustomDisaster(tempLevel[0], plugin, yaml);
							custom.createTimedStart((int) worldObj.settings.get("pet_warning_time"), offVec, all);
							timer.get(world.getUID()).replace(uuid, rand.nextInt(MinTime/2)+MinTime);
							plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
								@Override
								public void run() {
									resetNearbyPlayers(all, worldObj.maxRadius, MinTime);
								}
							});
							continue;
						}
					}
					List<Disaster> options = new ArrayList<>(Arrays.asList(Disaster.values()));
					options.remove(Disaster.CUSTOM);
					if (seasonsActive) {
						me.casperge.realisticseasons.season.Season season = SeasonsHandler.getSeasonsAPI().getSeason(all.getWorld());
						for (Iterator<Disaster> iterator = options.iterator(); iterator.hasNext();) {
							Disaster disaster = (Disaster) iterator.next();
							if (seasonsHandler.seasonMap.containsKey(disaster) && !seasonsHandler.seasonMap.get(disaster).contains(season))
								iterator.remove();
						}
					}
					for (Iterator<Disaster> iterator = options.iterator(); iterator.hasNext();)
						if (iterator.next().getFrequency() < rand.nextDouble())
							iterator.remove();
					Collections.shuffle(options);
					Bukkit.getScheduler().runTask(plugin, new Runnable() {
						@Override
						public void run() {
							if (plugin.RegionProtection && (boolean) worldObj.settings.get("region_protection") && Utils.isZoneProtected(all.getLocation()))
								return;
							int level = tempLevel[0];
							Location loc = all.getLocation();
							World world = all.getWorld();
							Biome biome = loc.getBlock().getBiome();
							if (biome == null)
								return;
							breakthis:
								for (Disaster disaster : options) {
									switch (disaster) {
									case SINKHOLE:
										if (!worldObj.allowed.contains(Disaster.SINKHOLE) || loc.getBlockY() < Disaster.SINKHOLE.getMinHeight() || oceanBiomes.contains(biome)) continue;
										if (level > Disaster.SINKHOLE.getMaxLevel())
											level = Disaster.SINKHOLE.getMaxLevel();
										Sinkhole s = new Sinkhole(level);
										s.createTimedStart((int) worldObj.settings.get("pet_warning_time"), offVec, all);
										resetNearbyPlayers(all, worldObj.maxRadius, MinTime);
										break breakthis;
									case CAVEIN:
										if (!worldObj.allowed.contains(Disaster.CAVEIN) || (loc.getBlockY() > Disaster.CAVEIN.getMinHeight() && world.getEnvironment() != Environment.NETHER) || world.getHighestBlockYAt(loc) <= loc.getBlockY()+1) continue;
										if (level > Disaster.CAVEIN.getMaxLevel())
											level = Disaster.CAVEIN.getMaxLevel();
										CaveIn cavein = new CaveIn(level);
										cavein.createTimedStart((int) worldObj.settings.get("pet_warning_time"), offVec, all);
										resetNearbyPlayers(all, worldObj.maxRadius, MinTime);
										break breakthis;
									case TORNADO:
										if (!worldObj.allowed.contains(Disaster.TORNADO) || loc.getBlockY() < Disaster.TORNADO.getMinHeight() || oceanBiomes.contains(biome)) continue;
										if (level > Disaster.TORNADO.getMaxLevel())
											level = Disaster.TORNADO.getMaxLevel();
										Tornado tornado = new Tornado(level);
										tornado.createTimedStart((int) worldObj.settings.get("pet_warning_time"), offVec, all);
										resetNearbyPlayers(all, worldObj.maxRadius, MinTime);
										break breakthis;
									case GEYSER:
										if (!worldObj.allowed.contains(Disaster.GEYSER) || loc.getBlockY() < Disaster.GEYSER.getMinHeight() || world.getEnvironment() == Environment.THE_END) continue;
										if (level > Disaster.GEYSER.getMaxLevel())
											level = Disaster.GEYSER.getMaxLevel();
										Geyser geyser = new Geyser(level);
										geyser.createTimedStart((int) worldObj.settings.get("pet_warning_time"), offVec, all);
										resetNearbyPlayers(all, worldObj.maxRadius, MinTime);
										break breakthis;
									case PLAGUE:
										if (!worldObj.allowed.contains(Disaster.PLAGUE)) continue;
										if (level > Disaster.PLAGUE.getMaxLevel())
											level = Disaster.PLAGUE.getMaxLevel();
										BlackPlague plague = new BlackPlague(level);
										if (plague.isMobAvailable(all.getWorld())) {
											plague.createTimedStart((int) worldObj.settings.get("pet_warning_time"), all.getWorld(), all);
											resetNearbyPlayers(all, worldObj.maxRadius, MinTime);
											break breakthis;
										}
										break;
									case ACIDSTORM:
										if (!worldObj.allowed.contains(Disaster.ACIDSTORM) || loc.getBlockY() < Disaster.ACIDSTORM.getMinHeight() || world.getEnvironment() != Environment.NORMAL || world.hasStorm()
										|| loc.getBlock().getTemperature() <= 0.15 || loc.getBlock().getTemperature() > 0.95 || DeathMessages.acidstorms.stream().anyMatch(n -> n.getWorld() == world)) continue;
										if (level > Disaster.ACIDSTORM.getMaxLevel())
											level = Disaster.ACIDSTORM.getMaxLevel();
										AcidStorm acidstorm = new AcidStorm(level);
										acidstorm.createTimedStart((int) worldObj.settings.get("pet_warning_time"), all.getWorld(), all);
										resetNearbyPlayers(all, worldObj.maxRadius, MinTime);
										break breakthis;
									case EXTREMEWINDS:
										if (!worldObj.allowed.contains(Disaster.EXTREMEWINDS) || loc.getBlockY() < Disaster.EXTREMEWINDS.getMinHeight() || world.getEnvironment() != Environment.NORMAL
										|| DeathMessages.extremewinds.stream().anyMatch(n -> n.getWorld() == world)) continue;
										if (level > Disaster.EXTREMEWINDS.getMaxLevel())
											level = Disaster.EXTREMEWINDS.getMaxLevel();
										ExtremeWinds winds = new ExtremeWinds(level);
										winds.createTimedStart((int) worldObj.settings.get("pet_warning_time"), all.getWorld(), all);
										resetNearbyPlayers(all, worldObj.maxRadius, MinTime);
										break breakthis;
									case SOULSTORM:
										if (!worldObj.allowed.contains(Disaster.SOULSTORM) || loc.getBlockY() < Disaster.SOULSTORM.getMinHeight() || world.getEnvironment() != Environment.NETHER
										|| DeathMessages.soulstorms.stream().anyMatch(n -> n.getWorld() == world)) continue;
										if (level > Disaster.SOULSTORM.getMaxLevel())
											level = Disaster.SOULSTORM.getMaxLevel();
										SoulStorm soulstorm = new SoulStorm(level);
										soulstorm.createTimedStart((int) worldObj.settings.get("pet_warning_time"), all.getWorld(), all);
										resetNearbyPlayers(all, worldObj.maxRadius, MinTime);
										break breakthis;
									case BLIZZARD:
										if (!worldObj.allowed.contains(Disaster.BLIZZARD) || world.getEnvironment() != Environment.NORMAL || loc.getBlockY() < Disaster.BLIZZARD.getMinHeight()
										|| DeathMessages.blizzards.stream().anyMatch(n -> n.getWorld() == world) || (!seasonsActive && loc.getBlock().getTemperature() > 0.15)) continue;
										if (level > Disaster.BLIZZARD.getMaxLevel())
											level = Disaster.BLIZZARD.getMaxLevel();
										Blizzard blizz = new Blizzard(level);
										blizz.createTimedStart((int) worldObj.settings.get("pet_warning_time"), all.getWorld(), all);
										resetNearbyPlayers(all, worldObj.maxRadius, MinTime);
										break breakthis;
									case SANDSTORM:
										if (!worldObj.allowed.contains(Disaster.SANDSTORM) || world.getEnvironment() != Environment.NORMAL || !SandStorm.sandStormBiomes.contains(loc.getBlock().getBiome())
										|| loc.getBlockY() < Disaster.SANDSTORM.getMinHeight() || DeathMessages.sandstorms.stream().anyMatch(n -> n.getWorld() == world)) continue;
										if (level > Disaster.SANDSTORM.getMaxLevel())
											level = Disaster.SANDSTORM.getMaxLevel();
										SandStorm sand = new SandStorm(level);
										sand.createTimedStart((int) worldObj.settings.get("pet_warning_time"), all.getWorld(), all);
										resetNearbyPlayers(all, worldObj.maxRadius, MinTime);
										break breakthis;
									case EARTHQUAKE:
										if (!worldObj.allowed.contains(Disaster.EARTHQUAKE) || loc.getBlockY() < Disaster.EARTHQUAKE.getMinHeight() || oceanBiomes.contains(biome)) continue;
										if (level > Disaster.EARTHQUAKE.getMaxLevel())
											level = Disaster.EARTHQUAKE.getMaxLevel();
										Earthquake earthquake = new Earthquake(level);
										earthquake.createTimedStart((int) worldObj.settings.get("pet_warning_time"), offVec, all);
										resetNearbyPlayers(all, worldObj.maxRadius, MinTime);
										break breakthis;
									case TSUNAMI:
										if (!worldObj.allowed.contains(Disaster.TSUNAMI) || loc.getBlockY() < Disaster.TSUNAMI.getMinHeight()) continue;
										if (level > Disaster.TSUNAMI.getMaxLevel())
											level = Disaster.TSUNAMI.getMaxLevel();
										Tsunami tsu = new Tsunami(level);
										if (tsu.findAvailabePool(loc) == null) break;
										tsu.createTimedStart((int) worldObj.settings.get("pet_warning_time"), offVec, all);
										resetNearbyPlayers(all, worldObj.maxRadius, MinTime);
										break breakthis;
									case METEORSHOWERS:
										if (!worldObj.allowed.contains(Disaster.METEORSHOWERS) || loc.getBlockY() < Disaster.METEORSHOWERS.getMinHeight() || world.getEnvironment() != Environment.NORMAL
										|| DeathMessages.meteorshowers.stream().anyMatch(n -> n.getWorld() == world)) continue;
										if (level > Disaster.METEORSHOWERS.getMaxLevel())
											level = Disaster.METEORSHOWERS.getMaxLevel();
										MeteorShower storm = new MeteorShower(level);
										storm.createTimedStart((int) worldObj.settings.get("pet_warning_time"), all.getWorld(), all);
										resetNearbyPlayers(all, worldObj.maxRadius, MinTime);
										break breakthis;
									case ENDSTORM:
										if (!worldObj.allowed.contains(Disaster.ENDSTORM) || loc.getBlockY() < Disaster.ENDSTORM.getMinHeight() || world.getEnvironment() != Environment.THE_END || plugin.mcVersion < 1.16
										|| DeathMessages.endstorms.stream().anyMatch(n -> n.getWorld() == world)) continue;
										if (level > Disaster.ENDSTORM.getMaxLevel())
											level = Disaster.ENDSTORM.getMaxLevel();
										EndStorm endstorm = new EndStorm(level);
										endstorm.createTimedStart((int) worldObj.settings.get("pet_warning_time"), all.getWorld(), all);
										resetNearbyPlayers(all, worldObj.maxRadius, MinTime);
										break breakthis;
									case SUPERNOVA:
										if (!worldObj.allowed.contains(Disaster.SUPERNOVA) || world.getEnvironment() == Environment.NETHER || loc.getBlockY() < Disaster.SUPERNOVA.getMinHeight() || oceanBiomes.contains(biome)) continue;
										if (level > Disaster.SUPERNOVA.getMaxLevel())
											level = Disaster.SUPERNOVA.getMaxLevel();
										Supernova nova = new Supernova(level);
										nova.createTimedStart((int) worldObj.settings.get("pet_warning_time"), offVec, all);
										resetNearbyPlayers(all, worldObj.maxRadius, MinTime);
										break breakthis;
									case HURRICANE:
										if (!worldObj.allowed.contains(Disaster.HURRICANE) || world.getEnvironment() != Environment.NORMAL || loc.getBlockY() < Disaster.HURRICANE.getMinHeight()
										|| (!Hurricane.oceans.contains(biome) && (loc.getBlock().getTemperature() <= 0.85 || loc.getBlock().getTemperature() >= 1.0))) continue;
										if (level > Disaster.HURRICANE.getMaxLevel())
											level = Disaster.HURRICANE.getMaxLevel();
										Hurricane hurricane = new Hurricane(level);
										hurricane.createTimedStart((int) worldObj.settings.get("pet_warning_time"), offVec, all);
										resetNearbyPlayers(all, worldObj.maxRadius, MinTime);
										break breakthis;
									case PURGE:
										if (!worldObj.allowed.contains(Disaster.PURGE)) continue;
										if (level > Disaster.PURGE.getMaxLevel())
											level = Disaster.PURGE.getMaxLevel();
										Purge purge = new Purge(level);
										purge.createTimedStart((int) worldObj.settings.get("pet_warning_time"), offVec, all);
										resetNearbyPlayers(all, worldObj.maxRadius, MinTime);
										break breakthis;
									default:
										break;
									}
								}
						}
					});
					timer.get(world.getUID()).replace(uuid, rand.nextInt(MinTime/2)+MinTime);
				}
			}
		}, 0, 20);
	}
	public void refreshTimerList() {
		for (Player p : Bukkit.getOnlinePlayers())
			for (String section : dataFile.getConfigurationSection("timers").getKeys(false))
				if (timer.containsKey(UUID.fromString(section)))
					if (dataFile.contains("timers."+section+"."+p.getUniqueId().toString()))
						timer.get(UUID.fromString(section)).put(p.getUniqueId(), dataFile.getInt("timers."+section+"."+p.getUniqueId().toString()));
					else
						timer.get(UUID.fromString(section)).put(p.getUniqueId(), WorldObject.findWorldObject(Bukkit.getWorld(UUID.fromString(section))).generateTimerValue(rand));
	}
	public void updateTimerList(World world) {
		WorldObject worldObj = WorldObject.findWorldObject(world);
		for (Player p : world.getPlayers())
			timer.get(world.getUID()).put(p.getUniqueId(), rand.nextInt(worldObj.timer/2)+worldObj.timer);
		if (dataFile.contains("timers."+world.getUID()))
			for (String uuid : dataFile.getConfigurationSection("timers."+world.getUID()).getKeys(false))
				dataFile.set("timers."+world.getUID()+"."+uuid, rand.nextInt(worldObj.timer/2)+worldObj.timer);
		plugin.saveDataFile();
	}
	public void saveTimerValues() {
		for (Entry<UUID, Map<UUID, Integer>> entry : timer.entrySet())
			for (Entry<UUID, Integer> internal : entry.getValue().entrySet())
				dataFile.set("timers."+entry.getKey()+"."+internal.getKey(), internal.getValue());
	}
	public void resetNearbyPlayers(Player p, int range, int minTime) {
		for (Entity e : p.getNearbyEntities(range, range, range))
			if (e instanceof Player && !Utils.isPlayerImmune((Player) e))
				timer.get(e.getWorld().getUID()).replace(e.getUniqueId(), rand.nextInt(minTime/2)+minTime);
	}
}
