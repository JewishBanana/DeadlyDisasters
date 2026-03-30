package com.github.jewishbanana.deadlydisasters;

import java.io.File;
import java.util.EnumSet;
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
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.github.jewishbanana.deadlydisasters.disasters.DisasterRegistry;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class WorldWrapper {

	private static final Main plugin;
	private static final Map<World, WorldWrapper> worldLinks;
	public static final Map<String, Set<DisasterRegistry>> disasterCategories;
	static {
		plugin = Main.getInstance();
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
			    DisasterRegistry.getRegistry("landslide")
			));
		disasterCategory.put("WEATHER_DISASTERS", Set.of(
				DisasterRegistry.getRegistry("acid_storm"),
				DisasterRegistry.getRegistry("sandstorm"),
				DisasterRegistry.getRegistry("blizzard"),
				DisasterRegistry.getRegistry("extreme_winds"),
				DisasterRegistry.getRegistry("soul_storm"),
				DisasterRegistry.getRegistry("monsoon"),
				DisasterRegistry.getRegistry("meteor_shower"),
				DisasterRegistry.getRegistry("end_storm"),
				DisasterRegistry.getRegistry("solar_storm")
				));
		disasterCategory.put("MOB_DISASTERS", Set.of(
				DisasterRegistry.getRegistry("purge")
				));
		disasterCategories = Map.copyOf(disasterCategory);
	}
	
	private World world;
	private File configFile;
	private String configName;
	private FileConfiguration config;
	private Sound startSound;
	private float startVolume;
	private float startPitch;
	private String soundTarget;
	
	public int targetingMode;
	public Set<DisasterRegistry> disabledDisasters;
	public int minimumTime;
	public int maximumTime;
	public float disasterOffset;
	public float[] probabilityTable;
	public float sharedDisasterRadius;
	public boolean dropContainerItems;
	public Set<Material> blackListedBlocks;
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
						Main.consoleSender.sendMessage(Utils.convertString(Utils.prefix+"&cError while trying to find world config file &d'"+name+"' &cin the worldConfigs folder! Please create this config. Reverting to default world config for world &a'"+world.getName()+"'&c!"));
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
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
		}
	}
	@SuppressWarnings("deprecation")
	public static void reload(WorldWrapper wrapper) {
		if (wrapper.configFile == null || !wrapper.configFile.exists())
			initWorld(wrapper.world);
		wrapper.config = YamlConfiguration.loadConfiguration(wrapper.configFile);
		
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
			wrapper.probabilityTable = new float[] { 30f, 25f, 20f, 15f, 9f, 1f };
		
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

		wrapper.dropContainerItems = DataUtils.getConfigBoolean(wrapper.config, wrapper.configName, "regeneration.drop_container_items", false);
		List<String> list = DataUtils.getConfigStringList(wrapper.config, wrapper.configName, "regeneration.block_blacklist");
		Set<Material> materials = new HashSet<>();
		list.forEach(material -> {
			Set<Material> set = BlockUtils.getMaterials(material);
			if (set == null) {
				Utils.sendConsoleMessage("&cERROR no such block type or category named &e'"+material+"' &cin the &d'"+wrapper.configName+"' &cworld config file at &b'b'regeneration.block_blacklist' &clist! This value will be omitted and regenerated as usual.");
				return;
			}
			materials.addAll(set);
		});
		wrapper.blackListedBlocks = materials.isEmpty() ? null : EnumSet.copyOf(materials);

		String soundString = DataUtils.getConfigString(wrapper.config, wrapper.configName, "world.start_sound.sound", null);
		if (soundString != null && !soundString.equalsIgnoreCase("NONE"))
			try {
				wrapper.startSound = Sound.valueOf(soundString.toUpperCase());
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
	public FileConfiguration getConfig() {
		return config;
	}
	public String getConfigName() {
		return configName;
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
