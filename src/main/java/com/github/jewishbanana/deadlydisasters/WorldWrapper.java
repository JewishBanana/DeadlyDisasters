package com.github.jewishbanana.deadlydisasters;

import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator;

import org.apache.commons.io.FileUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.github.jewishbanana.deadlydisasters.disasters.DisasterRegistry;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class WorldWrapper {

	private static final DeadlyDisasters plugin;
	private static final Map<World, WorldWrapper> worldLinks;
	private static final float[] DEFAULT_PROBABILITY_TABLE = new float[] { 30f, 25f, 20f, 15f, 9f, 1f };
	public static final Map<String, Set<DisasterRegistry>> disasterCategories;
	public static final Set<String> PRESET_NAMES = Set.of("EASY", "NORMAL", "HARD", "EXTREME");
	static {
		plugin = DeadlyDisasters.getInstance();
		worldLinks = new ConcurrentHashMap<>();
		
		Map<String, Set<DisasterRegistry>> disasterCategory = new HashMap<>();
		disasterCategory.put("ALL", Set.copyOf(DisasterRegistry.getRegisteredDisasters()));
		disasterCategory.put("DESTRUCTIVE_DISASTERS", Set.of(
			    DisasterRegistry.getRegistry("sinkhole"),
			    DisasterRegistry.getRegistry("earthquake"),
			    DisasterRegistry.getRegistry("tornado"),
			    DisasterRegistry.getRegistry("cavein"),
			    DisasterRegistry.getRegistry("water_geyser"),
			    DisasterRegistry.getRegistry("lava_geyser"),
			    DisasterRegistry.getRegistry("supernova"),
			    DisasterRegistry.getRegistry("tsunami")
			));
		disasterCategory.put("WEATHER_DISASTERS", Set.of(
				DisasterRegistry.getRegistry("acid_storm"),
				DisasterRegistry.getRegistry("sandstorm"),
				DisasterRegistry.getRegistry("blizzard"),
				DisasterRegistry.getRegistry("extreme_winds"),
				DisasterRegistry.getRegistry("soul_storm"),
				DisasterRegistry.getRegistry("meteor_shower"),
				DisasterRegistry.getRegistry("end_storm"),
				DisasterRegistry.getRegistry("solar_storm"),
				DisasterRegistry.getRegistry("hurricane")
				));
		disasterCategory.put("MOB_DISASTERS", Set.of(
				DisasterRegistry.getRegistry("black_plague"),
				DisasterRegistry.getRegistry("purge")
				));
		disasterCategories = Map.copyOf(disasterCategory);
	}
	
	private World world;
	private File configFile;
	private String configName;
	private FileConfiguration config;
	private String preset;
	private FileConfiguration presetConfig;
	private String presetDisplayName;
	private Sound startSound;
	private float startVolume;
	private float startPitch;
	private String soundTarget;
	
	public int targetingMode;
	public Set<DisasterRegistry> disabledDisasters;
	public int minimumTime;
	public int maximumTime;
	public float disasterOffset;
	public float[] probabilityTable = DEFAULT_PROBABILITY_TABLE.clone();
	public float sharedDisasterRadius;
	public Set<UUID> blacklistedPlayers;
	
	public WorldWrapper(World world) {
		this.world = world;
	}
	public static void init() {
		plugin.getServer().getWorlds().forEach(world -> initWorld(world));
	}
	public static void initWorld(World world) {
		try {
			WorldWrapper link = new WorldWrapper(world);
			ConfigurationSection section = DataUtils.getDataFile().getConfigurationSection("worlds."+world.getUID().toString());
			if (section == null)
				section = DataUtils.getDataFile().createSection("worlds."+world.getUID().toString());
			if (section.contains("config")) {
				String name = section.getString("config");
				File file = new File(plugin.getDataFolder().getAbsolutePath(), "worldConfigs/"+name+".yml");
				if (file.exists()) {
					link.configFile = file;
					link.config = YamlConfiguration.loadConfiguration(file);
					link.configName = name;
				} else {
					file = new File(plugin.getDataFolder().getAbsolutePath(), "worldConfigs/default.yml");
					if (file.exists()) {
						link.configFile = file;
						link.config = YamlConfiguration.loadConfiguration(file);
					} else {
						DeadlyDisasters.consoleSender.sendMessage(Utils.convertString(Utils.prefix+"&cError while trying to find world config file &d'"+name+"' &cin the worldConfigs folder! Please create this config. Reverting to default world config for world &a'"+world.getName()+"'&c!"));
						file.getParentFile().mkdirs();
						file.createNewFile();
						FileUtils.copyInputStreamToFile(plugin.getResource("files/worldConfigs/default.yml"), file);
						link.configFile = file;
						link.config = YamlConfiguration.loadConfiguration(file);
					}
					link.configName = "default";
				}
			} else {
				section.set("config", "default");
				DataUtils.saveDataFile();
				File file = new File(plugin.getDataFolder().getAbsolutePath(), "worldConfigs/default.yml");
				if (file.exists()) {
					link.configFile = file;
					link.config = YamlConfiguration.loadConfiguration(file);
				} else {
					file.getParentFile().mkdirs();
					file.createNewFile();
					FileUtils.copyInputStreamToFile(plugin.getResource("files/worldConfigs/default.yml"), file);
					link.configFile = file;
					link.config = YamlConfiguration.loadConfiguration(file);
				}
				link.configName = "default";
			}
			worldLinks.put(world, link);
			reload(link);
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
		}
	}
	public static void reload(WorldWrapper wrapper) {
		if (wrapper.configFile == null || !wrapper.configFile.exists()) {
			initWorld(wrapper.world);
			return;
		}
		wrapper.config = YamlConfiguration.loadConfiguration(wrapper.configFile);

		String presetName = DataUtils.getDataFileString("worlds." + wrapper.world.getUID().toString() + ".preset", null);
		wrapper.preset = presetName;
		wrapper.presetConfig = null;
		if (presetName != null && PRESET_NAMES.contains(presetName)) {
			try {
				wrapper.presetConfig = YamlConfiguration.loadConfiguration(
						new InputStreamReader(plugin.getResource("files/presets/" + presetName + ".yml")));
			} catch (Exception e) {
				Utils.sendExceptionLog(e);
			}
		}
		// The colored difficulty name used in world messages: pulled from the preset file, falling back to CUSTOM (no preset).
		wrapper.presetDisplayName = wrapper.presetConfig != null ? wrapper.presetConfig.getString("display_name", "&f&l" + presetName) : "&f&lCUSTOM";

		switch (DataUtils.getConfigString(wrapper.config, wrapper.configName, "world.targeting", "")) {
		default:
		case "DISABLED":
			wrapper.targetingMode = 0;
			break;
		case "INDIVIDUAL":
			wrapper.targetingMode = 1;
			break;
		case "GLOBAL":
			wrapper.targetingMode = 2;
			break;
		}
		Set<DisasterRegistry> disasters = new HashSet<>();
		List<String> disabledList = DataUtils.getConfigStringList(wrapper.config, wrapper.configName, "world.disabled_disasters");
		disabledList.forEach(disaster -> {
			switch (disaster.toUpperCase()) {
			case "NONE":
				break;
			case "ALL":
				disasters.addAll(DisasterRegistry.getRegisteredDisasters());
				break;
			default:
				Set<DisasterRegistry> category = disasterCategories.get(disaster.toUpperCase());
				if (category != null) {
					disasters.addAll(category);
					break;
				}
				DisasterRegistry registry = DisasterRegistry.getRegistry(disaster);
				if (registry == null) {
					Utils.sendConsoleMessage("&cERROR while trying to disable disaster in world &a'"+wrapper.world.getName()+"'&c for disaster &e'"+disaster+"'&c! This disaster will not be disabled! Check this value in the &e'"+wrapper.configName+"' &cconfig file.");
					break;
				}
				disasters.add(registry);
				break;
			}
		});
		wrapper.disabledDisasters = !disasters.isEmpty() ? Set.copyOf(disasters) : Set.of();
		wrapper.minimumTime = DataUtils.getConfigInt(wrapper.config, wrapper.configName, "world.minimum_time", 120);
		wrapper.maximumTime = DataUtils.getConfigInt(wrapper.config, wrapper.configName, "world.maximum_time", 180);
		if (wrapper.presetConfig != null) {
			if (wrapper.presetConfig.contains("world.minimum_time"))
				wrapper.minimumTime = wrapper.presetConfig.getInt("world.minimum_time");
			if (wrapper.presetConfig.contains("world.maximum_time"))
				wrapper.maximumTime = wrapper.presetConfig.getInt("world.maximum_time");
		}
		if (wrapper.maximumTime < wrapper.minimumTime) {
			wrapper.maximumTime = wrapper.minimumTime;
			Utils.sendConsoleMessage("&cERROR the maximum time must be greater than the minimum time! Something won't work until you fix this value in the world config &b'"+wrapper.configName+"'&c!");
		}
//		if (DataUtils.getDataFile().contains("worlds."+wrapper.world.getUID().toString()+".persisted_min") && DataUtils.getDataFile().contains("worlds."+wrapper.world.getUID().toString()+".persisted_max")) {
//			if (DataUtils.getDataFileInt("worlds."+wrapper.world.getUID().toString()+".persisted_min") != wrapper.minimumTime
//					|| DataUtils.getDataFileInt("worlds."+wrapper.world.getUID().toString()+".persisted_max") != wrapper.maximumTime) {
//				DataUtils.writeToDataFile(file -> {
//					file.set("worlds."+wrapper.world.getUID().toString()+".persisted_min", wrapper.minimumTime);
//					file.set("worlds."+wrapper.world.getUID().toString()+".persisted_max", wrapper.maximumTime);
//				});
//				plugin.selector.refreshWorldTimers(wrapper.world);
//			}
//		} else
//			DataUtils.writeToDataFile(file -> {
//				file.set("worlds."+wrapper.world.getUID().toString()+".persisted_min", wrapper.minimumTime);
//				file.set("worlds."+wrapper.world.getUID().toString()+".persisted_max", wrapper.maximumTime);
//			});
		wrapper.disasterOffset = (float) DataUtils.getConfigDouble(wrapper.config, wrapper.configName, "world.disaster_offset", 15.0);
		wrapper.sharedDisasterRadius = (float) DataUtils.getConfigDouble(wrapper.config, wrapper.configName, "world.shared_disaster_radius", 50.0);
		ConfigurationSection probabilityTable = DataUtils.getConfigSection(wrapper.config, wrapper.configName, "world.level_probabilities");
		if (probabilityTable != null) {
			wrapper.probabilityTable = new float[6];
			for (int i=1; i <= 6; i++)
				if (wrapper.config.contains(probabilityTable.getCurrentPath()+".level_"+i, true))
					wrapper.probabilityTable[i-1] = DataUtils.getConfigInt(wrapper.config, wrapper.configName, probabilityTable.getCurrentPath()+".level_"+i, 0);
		} else
			wrapper.probabilityTable = DEFAULT_PROBABILITY_TABLE.clone();
		// A preset overrides these difficulty values on top of the world config (just like it does for the timer values above).
		if (wrapper.presetConfig != null) {
			if (wrapper.presetConfig.contains("world.disaster_offset"))
				wrapper.disasterOffset = (float) wrapper.presetConfig.getDouble("world.disaster_offset");
			if (wrapper.presetConfig.contains("world.shared_disaster_radius"))
				wrapper.sharedDisasterRadius = (float) wrapper.presetConfig.getDouble("world.shared_disaster_radius");
			ConfigurationSection presetProbabilities = wrapper.presetConfig.getConfigurationSection("world.level_probabilities");
			if (presetProbabilities != null) {
				wrapper.probabilityTable = new float[6];
				for (int i=1; i <= 6; i++)
					if (presetProbabilities.contains("level_"+i))
						wrapper.probabilityTable[i-1] = (float) presetProbabilities.getDouble("level_"+i);
			}
		}

		if (DataUtils.getDataFile().contains("worlds."+wrapper.world.getUID().toString()+".player_blacklist")) {
			List<String> list = DataUtils.getDataFileStringList("worlds."+wrapper.world.getUID().toString()+".player_blacklist");
			Set<UUID> uuids = new HashSet<>();
			list.forEach(uuid -> {
				try {
					UUID temp = UUID.fromString(uuid);
					uuids.add(temp);
				} catch (IllegalArgumentException e) {
					Utils.sendConsoleMessage("&cERROR while trying to parse a players UUID from the player blacklist for world &a'"+wrapper.world.getName()+"'&c for UUID: &e'"+uuid+"'&c! This value will be omitted and the player will be effected by disasters.");
				}
			});
			wrapper.blacklistedPlayers = uuids.isEmpty() ? null : new HashSet<>(uuids);
		}

		String soundString = DataUtils.getConfigString(wrapper.config, wrapper.configName, "world.start_sound.sound", null);
		if (soundString != null && !soundString.equalsIgnoreCase("NONE"))
			try {
				wrapper.startSound = VersionUtils.getSound(soundString);
				if (wrapper.startSound == null)
					throw new IllegalArgumentException();
				wrapper.startVolume = (float) DataUtils.getConfigDouble(wrapper.config, wrapper.configName, "world.start_sound.volume", 1.0);
				wrapper.startPitch = (float) DataUtils.getConfigDouble(wrapper.config, wrapper.configName, "world.start_sound.pitch", 1.0);
				wrapper.soundTarget = DataUtils.getConfigString(wrapper.config, wrapper.configName, "world.start_sound.target", "ALL").toUpperCase();
			} catch (Exception exception) {
				Utils.sendConsoleMessage("&cERROR reading disaster start sound from world config &d'"+wrapper.configName+"' &cfix this section!");
			}
	}
	public static void reload() {
		worldLinks.values().forEach(wrapper -> reload(wrapper));
	}
	public static WorldWrapper getWorldWrapper(World world) {
		return worldLinks.get(world);
	}
	public static void removeWorld(World world) {
		worldLinks.remove(world);
	}
	public FileConfiguration getConfig() {
		return config;
	}
	public String getConfigName() {
		return configName;
	}
	public String getPreset() {
		return preset != null ? preset : "CUSTOM";
	}
	/** The colored difficulty display name (e.g. {@code &a&lEASY}) shown in world messages, configured per preset file. */
	public String getPresetDisplayName() {
		return presetDisplayName != null ? presetDisplayName : "&f&lCUSTOM";
	}
	public void saveAndReload() {
		try {
			config.save(configFile);
			reload(this);
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
		}
	}
	public int getConfigInt(String path) {
		return DataUtils.getConfigInt(config, configName, path);
	}
	public double getConfigDouble(String path) {
		return DataUtils.getConfigDouble(config, configName, path);
	}
	public boolean getConfigBoolean(String path) {
		return DataUtils.getConfigBoolean(config, configName, path);
	}
	public String getConfigString(String path) {
		return DataUtils.getConfigString(config, configName, path);
	}
	public List<String> getConfigStringList(String path) {
		return DataUtils.getConfigStringList(config, configName, path);
	}
	public List<Map<?, ?>> getConfigMapList(String path) {
		return DataUtils.getConfigMapList(config, configName, path);
	}
	public ConfigurationSection getConfigSection(String path) {
		return DataUtils.getConfigSection(config, configName, path);
	}
	public int rollLevel(RandomGenerator random) {
		if (probabilityTable == null || probabilityTable.length < 6)
			probabilityTable = DEFAULT_PROBABILITY_TABLE.clone();
		float roll = random.nextFloat(100f);
		float cumulative = 0;
		for (int i=0; i < 6; i++) {
			cumulative += probabilityTable[i];
			if (roll < cumulative)
				return i+1;
		}
		return 1;
	}
	public void playDisasterStartSound(Location location, Player target, double range) {
		if (startSound == null)
			return;
		switch (soundTarget) {
		default:
		case "ALL":
			location.getWorld().getPlayers().forEach(p -> p.playSound(p.getLocation(), startSound, startVolume, startPitch));
			break;
		case "NEARBY":
			double distance = range * range;
			location.getWorld().getPlayers().stream().filter(p -> p.getLocation().distanceSquared(location) <= distance).forEach(p -> p.playSound(p.getLocation(), startSound, startVolume, startPitch));
			break;
		case "TARGET":
			if (target != null)
				target.playSound(target.getLocation(), startSound, startVolume, startPitch);
			break;
		}
	}
}
