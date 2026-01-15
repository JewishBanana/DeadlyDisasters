package com.github.jewishbanana.deadlydisasters.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.jewishbanana.deadlydisasters.Main;

public class DataUtils {
	
	private static final JavaPlugin plugin;
	private static final FileConfiguration defaultGeneralConfig;
	private static final FileConfiguration defaultDisasterConfig;
	private static FileConfiguration languageConfig;
	private static final File dataFile;
	private static final FileConfiguration dataYaml;
	static {
		plugin = Main.getInstance();
		defaultGeneralConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource("config.yml")));
		defaultDisasterConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource("files/worldConfigs/default.yml")));
		
		List<String> ignoredSections = Arrays.asList(
				// Acid Storm
				"disasters.weather.acid_storm.entity_effects",
				"disasters.weather.acid_storm.block_changes",
				"disasters.weather.acid_storm.blacklisted_mob_types",
				// Sandstorm
				"disasters.weather.sandstorm.entity_effects",
				"disasters.weather.sandstorm.blacklisted_mob_types",
				// Blizzard
				"disasters.weather.blizzard.entity_effects",
				"disasters.weather.blizzard.blacklisted_mob_types",
				// Monsoon
				"disasters.weather.monsoon.entity_effects",
				"disasters.weather.monsoon.block_changes",
				"disasters.weather.monsoon.blacklisted_mob_types",
				// Solar Storm
				"disasters.weather.solar_storm.entity_effects",
				"disasters.weather.solar_storm.block_changes",
				"disasters.weather.solar_storm.blacklisted_mob_types",
				// Soul Storm
				"disasters.weather.soul_storm.entity_effects",
				"disasters.weather.soul_storm.blacklisted_mob_types",
				// End Storm
				"disasters.weather.end_storm.entity_effects",
				"disasters.weather.end_storm.blacklisted_mob_types"
				);
		try {
			File folder = new File(plugin.getDataFolder().getAbsolutePath(), "worldConfigs");
			folder.mkdirs();
			for (File f : folder.listFiles())
				try {
					ConfigUpdater.update(plugin, "files/worldConfigs/default.yml", f, ignoredSections);
				} catch (IOException e) {
					Utils.sendExceptionLog(e);
				}
		} catch (Exception exception) {
			Utils.sendExceptionLog(exception);
			Utils.sendConsoleMessage("&cERROR could not update configs in folder!");
		}
		
		dataFile = new File(plugin.getDataFolder().getAbsolutePath(), "data/data.yml");
		if (!dataFile.exists()) {
			plugin.getLogger().info("Could not find data file in plugin directory! Creating new data file...");
			dataFile.getParentFile().mkdirs();
			try {
				dataFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		dataYaml = YamlConfiguration.loadConfiguration(dataFile);
		updateConfigChanges();
	}

	public static void reload() {
		File translationsFile = new File(plugin.getDataFolder().getAbsolutePath(), "language.yml");
		if (!translationsFile.exists()) {
			translationsFile.getParentFile().mkdirs();
			try {
				FileUtils.copyInputStreamToFile(plugin.getResource("files/language.yml"), translationsFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			ConfigUpdater.update(plugin, "files/language.yml", translationsFile, null);
		} catch (IOException e) {
			Utils.sendExceptionLog(e);
		}
		languageConfig = YamlConfiguration.loadConfiguration(translationsFile);
	}
	private static void updateConfigChanges() {
		String last = dataYaml.contains("stored_version") ? dataYaml.getString("stored_version") : null;
		if (last != null && last.equals(plugin.getDescription().getVersion()))
			return;
		File folder = new File(plugin.getDataFolder().getAbsolutePath(), "worldConfigs");
		folder.mkdirs();
		switch (last != null ? last : "default") {
		default:
		case "1.0.1-BETA":
			for (File f : folder.listFiles())
				try {
					FileConfiguration file = YamlConfiguration.loadConfiguration(f);
					List<String> list = file.getStringList("disasters.weather.end_storm.blacklisted_mob_types");
					if (!list.isEmpty()) {
						list.add("end_crystal");
						file.set("disasters.weather.end_storm.blacklisted_mob_types", list);
					}
					file.save(f);
				} catch (Exception e) {
					Utils.sendExceptionLog(e);
				}
		case "1.0.2-BETA":
			try {
				plugin.getConfig().set("regeneration.force_regen_blocks_per_tick", null);
				plugin.saveConfig();
				plugin.reloadConfig();
			} catch (Exception e) {
				Utils.sendExceptionLog(e);
			}
			for (File f : folder.listFiles())
				try {
					FileConfiguration file = YamlConfiguration.loadConfiguration(f);
					if (file.getInt("disasters.weather.acid_storm.entity_effects.poison.ticks") == 10)
						file.set("disasters.weather.acid_storm.entity_effects.poison.ticks", 11);
					file.save(f);
				} catch (Exception e) {
					Utils.sendExceptionLog(e);
				}
			try {
				File blocksFile = new File(plugin.getDataFolder().getAbsolutePath(), "blocks.yml");
				FileConfiguration blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);
				if (blocksConfig.getDouble("stones.polished_granite") == 0.3)
					blocksConfig.set("stones.polished_granite", 0.2);
				if (blocksConfig.getDouble("stones.polished_diorite") == 0.3)
					blocksConfig.set("stones.polished_diorite", 0.2);
				if (blocksConfig.getDouble("stones.polished_andesite") == 0.3)
					blocksConfig.set("stones.polished_andesite", 0.2);
				if (blocksConfig.getDouble("stones.dripstone_block") == 0.1)
					blocksConfig.set("stones.dripstone_block", 0.03);
				if (blocksConfig.getDouble("stones.stone_bricks") == 0.4)
					blocksConfig.set("stones.stone_bricks", 0.25);
				if (blocksConfig.getDouble("stones.mossy_stone_bricks") == 0.35)
					blocksConfig.set("stones.mossy_stone_bricks", 0.2);
				if (blocksConfig.getDouble("stones.cracked_stone_bricks") == 0.2)
					blocksConfig.set("stones.cracked_stone_bricks", 0.15);
				if (blocksConfig.getDouble("stones.chiseled_stone_bricks") == 0.2)
					blocksConfig.set("stones.chiseled_stone_bricks", 0.15);
				if (blocksConfig.getDouble("stones.deepslate_bricks") == 0.3)
					blocksConfig.set("stones.deepslate_bricks", 0.25);
				if (blocksConfig.getDouble("stones.deepslate_tiles") == 0.4)
					blocksConfig.set("stones.deepslate_tiles", 0.3);
				if (blocksConfig.getDouble("stones.cracked_deepslate_tiles") == 0.3)
					blocksConfig.set("stones.cracked_deepslate_tiles", 0.25);
				if (blocksConfig.getDouble("stones.sandstone") == 0.15)
					blocksConfig.set("stones.sandstone", 0.05);
				if (blocksConfig.getDouble("stones.red_sandstone") == 0.15)
					blocksConfig.set("stones.red_sandstone", 0.05);
				if (blocksConfig.getDouble("stones.bricks") == 0.45)
					blocksConfig.set("stones.bricks", 0.3);
				
				if (blocksConfig.getDouble("woods.logs") == 0.15)
					blocksConfig.set("woods.logs", 0.0);
				if (blocksConfig.getDouble("woods.planks") == 0.25)
					blocksConfig.set("woods.planks", 0.15);
				if (blocksConfig.getDouble("woods.wooden_slabs") == 0.25)
					blocksConfig.set("woods.wooden_slabs", 0.15);
				if (blocksConfig.getDouble("woods.wooden_stairs") == 0.25)
					blocksConfig.set("woods.wooden_stairs", 0.15);
				if (blocksConfig.getDouble("woods.wooden_trapdoors") == 0.25)
					blocksConfig.set("woods.wooden_trapdoors", 0.15);
				if (blocksConfig.getDouble("woods.fences") == 0.2)
					blocksConfig.set("woods.fences", 0.15);
				if (blocksConfig.getDouble("woods.fence_gates") == 0.2)
					blocksConfig.set("woods.fence_gates", 0.15);
				
				if (blocksConfig.getDouble("nether.basalt") == 0.15)
					blocksConfig.set("nether.basalt", 0.05);
				if (blocksConfig.getDouble("nether.polished_basalt") == 0.3)
					blocksConfig.set("nether.polished_basalt", 0.1);
				if (blocksConfig.getDouble("nether.smooth_basalt") == 0.3)
					blocksConfig.set("nether.smooth_basalt", 0.1);
				if (blocksConfig.getDouble("nether.nether_bricks") == 0.4)
					blocksConfig.set("nether.nether_bricks", 0.25);
				if (blocksConfig.getDouble("nether.cracked_nether_bricks") == 0.3)
					blocksConfig.set("nether.cracked_nether_bricks", 0.2);
				if (blocksConfig.getDouble("nether.chiseled_nether_bricks") == 0.3)
					blocksConfig.set("nether.chiseled_nether_bricks", 0.2);
				if (blocksConfig.getDouble("nether.red_nether_bricks") == 0.4)
					blocksConfig.set("nether.red_nether_bricks", 0.25);
				if (blocksConfig.getDouble("nether.magma_block") == 0.1)
					blocksConfig.set("nether.magma_block", 0.02);
				if (blocksConfig.getDouble("nether.blackstone") == 0.2)
					blocksConfig.set("nether.blackstone", 0.05);
				if (blocksConfig.getDouble("nether.gilded_blackstone") == 0.3)
					blocksConfig.set("nether.gilded_blackstone", 0.05);
				if (blocksConfig.getDouble("nether.polished_blackstone") == 0.45)
					blocksConfig.set("nether.polished_blackstone", 0.2);
				if (blocksConfig.getDouble("nether.chiseled_polished_blackstone") == 0.25)
					blocksConfig.set("nether.chiseled_polished_blackstone", 0.15);
				if (blocksConfig.getDouble("nether.polished_blackstone_bricks") == 0.45)
					blocksConfig.set("nether.polished_blackstone_bricks", 0.275);
				if (blocksConfig.getDouble("nether.cracked_polished_blackstone_bricks") == 0.25)
					blocksConfig.set("nether.cracked_polished_blackstone_bricks", 0.2);
				
				if (blocksConfig.getDouble("end.end_stone") == 0.2)
					blocksConfig.set("end.end_stone", 0.05);
				if (blocksConfig.getDouble("end.end_stone_bricks") == 0.4)
					blocksConfig.set("end.end_stone_bricks", 0.25);
				if (blocksConfig.getDouble("end.purpur_block") == 0.4)
					blocksConfig.set("end.purpur_block", 0.25);
				if (blocksConfig.getDouble("end.purpur_pillar") == 0.4)
					blocksConfig.set("end.purpur_pillar", 0.25);
				
				if (blocksConfig.getDouble("building_blocks.beds") == 0.25)
					blocksConfig.set("building_blocks.beds", 0.03);
				if (blocksConfig.getDouble("building_blocks.bone_block") == 0.3)
					blocksConfig.set("building_blocks.bone_block", 0.02);
				if (blocksConfig.getDouble("building_blocks.crafting_table") == 0.2)
					blocksConfig.set("building_blocks.crafting_table", 0.03);
				if (blocksConfig.getDouble("building_blocks.chest") == 0.2)
					blocksConfig.set("building_blocks.chest", 0.03);
				if (blocksConfig.getDouble("building_blocks.furnace") == 0.25)
					blocksConfig.set("building_blocks.furnace", 0.05);
				if (blocksConfig.getDouble("building_blocks.anvil") == 0.6)
					blocksConfig.set("building_blocks.anvil", 0.2);
				if (blocksConfig.getDouble("building_blocks.ender_chest") == 0.2)
					blocksConfig.set("building_blocks.ender_chest", 0.1);
				if (blocksConfig.getDouble("building_blocks.honeycomb_block") == 0.15)
					blocksConfig.set("building_blocks.honeycomb_block", 0);
				if (blocksConfig.getDouble("building_blocks.barrel") == 0.15)
					blocksConfig.set("building_blocks.barrel", 0.03);
				if (blocksConfig.getDouble("building_blocks.smoker") == 0.2)
					blocksConfig.set("building_blocks.smoker", 0.05);
				if (blocksConfig.getDouble("building_blocks.blast_furnace") == 0.3)
					blocksConfig.set("building_blocks.blast_furnace", 0.05);
				if (blocksConfig.getDouble("building_blocks.bookshelf") == 0.2)
					blocksConfig.set("building_blocks.bookshelf", 0.03);
				
				blocksConfig.save(blocksFile);
			} catch (Exception e) {
				Utils.sendExceptionLog(e);
			}
			break;
		}
		writeToDataFile(file -> file.set("stored_version", plugin.getDescription().getVersion()));
	}
	public static int getMainConfigInt(String path) {
		try {
			return plugin.getConfig().getInt(path);
		} catch (Exception e) {
			Utils.sendConsoleMessage("&eWARNING while reading &dinteger &evalue from config path '"+path+"' please fix this value!");
			return defaultGeneralConfig.getInt(path);
		}
	}
	public static int getConfigInt(FileConfiguration config, String configName, String path) {
		try {
			return config.getInt(path);
		} catch (Exception e) {
			Utils.sendConsoleMessage("&eWARNING while reading &dinteger &evalue from the config &6'"+configName+"' &eat &a'"+path+"' &eplease fix this value!");
			return defaultDisasterConfig.getInt(path);
		}
	}
	public static int getConfigInt(FileConfiguration config, String configName, String path, int defaultValue) {
		try {
			if (!config.contains(path, true))
				return defaultValue;
			return config.getInt(path);
		} catch (Exception e) {
			Utils.sendConsoleMessage("&eWARNING while reading &dinteger &evalue from the config &6'"+configName+"' &eat &a'"+path+"' &eplease fix this value!");
			return defaultValue;
		}
	}
	public static double getMainConfigDouble(String path) {
		try {
			return plugin.getConfig().getDouble(path);
		} catch (Exception e) {
			Utils.sendConsoleMessage("&eWARNING while reading &ddouble &evalue from config path '"+path+"' please fix this value!");
			return defaultGeneralConfig.getDouble(path);
		}
	}
	public static double getConfigDouble(FileConfiguration config, String configName, String path) {
		try {
			return config.getDouble(path);
		} catch (Exception e) {
			Utils.sendConsoleMessage("&eWARNING while reading &ddouble &evalue from the config &6'"+configName+"' &eat &a'"+path+"' &eplease fix this value!");
			return defaultDisasterConfig.getDouble(path);
		}
	}
	public static double getConfigDouble(FileConfiguration config, String configName, String path, double defaultValue) {
		try {
			if (!config.contains(path, true))
				return defaultValue;
			return config.getDouble(path);
		} catch (Exception e) {
			Utils.sendConsoleMessage("&eWARNING while reading &ddouble &evalue from the config &6'"+configName+"' &eat &a'"+path+"' &eplease fix this value!");
			return defaultValue;
		}
	}
	public static boolean getMainConfigBoolean(String path) {
		try {
			return plugin.getConfig().getBoolean(path);
		} catch (Exception e) {
			Utils.sendConsoleMessage("&eWARNING while reading &dboolean &evalue from config path '"+path+"' please fix this value!");
			return defaultGeneralConfig.getBoolean(path);
		}
	}
	public static boolean getConfigBoolean(FileConfiguration config, String configName, String path) {
		try {
			return config.getBoolean(path);
		} catch (Exception e) {
			Utils.sendConsoleMessage("&eWARNING while reading &dboolean &evalue from the config &6'"+configName+"' &eat &a'"+path+"' &eplease fix this value!");
			return defaultDisasterConfig.getBoolean(path);
		}
	}
	public static boolean getConfigBoolean(FileConfiguration config, String configName, String path, boolean defaultValue) {
		try {
			if (!config.contains(path, true))
				return defaultValue;
			return config.getBoolean(path);
		} catch (Exception e) {
			Utils.sendConsoleMessage("&eWARNING while reading &dboolean &evalue from the config &6'"+configName+"' &eat &a'"+path+"' &eplease fix this value!");
			return defaultDisasterConfig.getBoolean(path);
		}
	}
	public static String getMainConfigString(String path) {
		try {
			return plugin.getConfig().getString(path);
		} catch (Exception e) {
			Utils.sendConsoleMessage("&eWARNING while reading &dstring &evalue from config path '"+path+"' please fix this value!");
			return defaultGeneralConfig.getString(path);
		}
	}
	public static String getConfigString(FileConfiguration config, String configName, String path) {
		try {
			return config.getString(path);
		} catch (Exception e) {
			Utils.sendConsoleMessage("&eWARNING while reading &dstring &evalue from the config &6'"+configName+"' &eat &a'"+path+"' &eplease fix this value!");
			return defaultDisasterConfig.getString(path);
		}
	}
	public static List<String> getMainConfigStringList(String path) {
		try {
			return plugin.getConfig().getStringList(path);
		} catch (Exception e) {
			Utils.sendConsoleMessage("&eWARNING while reading &dstring list &evalue from config path '"+path+"' please fix this value!");
			return defaultGeneralConfig.getStringList(path);
		}
	}
	public static String getConfigString(FileConfiguration config, String configName, String path, String defaultValue) {
		try {
			if (!config.contains(path, true))
				return defaultValue;
			return config.getString(path);
		} catch (Exception e) {
			Utils.sendConsoleMessage("&eWARNING while reading &dstring &evalue from the config &6'"+configName+"' &eat &a'"+path+"' &eplease fix this value!");
			return defaultValue;
		}
	}
	public static List<String> getConfigStringList(FileConfiguration config, String configName, String path) {
		try {
			return config.getStringList(path);
		} catch (Exception e) {
			Utils.sendConsoleMessage("&eWARNING while reading &dstring list &evalue from the config &6'"+configName+"' &eat &a'"+path+"' &eplease fix this value!");
			return defaultDisasterConfig.getStringList(path);
		}
	}
	public static List<Map<?, ?>> getConfigMapList(FileConfiguration config, String configName, String path) {
		try {
			return config.getMapList(path);
		} catch (Exception e) {
			Utils.sendConsoleMessage("&eWARNING while reading &dmap list &evalue from the config &6'"+configName+"' &eat &a'"+path+"' &eplease fix this value!");
			return defaultDisasterConfig.getMapList(path);
		}
	}
	public static ConfigurationSection getConfigSection(FileConfiguration config, String configName, String path) {
		try {
			if (!config.contains(path, true))
				return null;
			return config.getConfigurationSection(path);
		} catch (Exception e) {
			Utils.sendConsoleMessage("&eWARNING while reading &dconfiguration section &evalue from the config &6'"+configName+"' &eat &a'"+path+"' &eplease fix this value!");
			return null;
		}
	}
	public static FileConfiguration getDefaultDisasterConfig() {
		return defaultDisasterConfig;
	}
	public static String getLanguageString(String path) {
		try {
			return languageConfig.getString(path);
		} catch (Exception e) {
			Utils.sendConsoleMessage("&cERROR while reading &dstring &cvalue from the &elanguage.yml &cfile path &b'"+path+"' &cplease fix this value!");
		}
		return null;
	}
	public static List<String> getLanguageStringList(String path) {
		try {
			return languageConfig.getStringList(path);
		} catch (Exception e) {
			Utils.sendConsoleMessage("&cERROR while reading &dstring list &cvalue from the &elanguage.yml &cfile path &b'"+path+"' &cplease fix this value!");
		}
		return null;
	}
	public static FileConfiguration getLanguageConfig() {
		return languageConfig;
	}
	public static int getDataFileInt(String path) {
		try {
			return dataYaml.getInt(path);
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cERROR while reading &dinteger &cvalue from data file path '"+path+"'!");
			return 0;
		}
	}
	public static int getDataFileInt(String path, int defaultValue) {
		try {
			return dataYaml.getInt(path);
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cERROR while reading &dinteger &cvalue from data file path '"+path+"'!");
			return defaultValue;
		}
	}
	public static double getDataFileDouble(String path) {
		try {
			return dataYaml.getDouble(path);
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cERROR while reading &ddouble &cvalue from data file path '"+path+"'!");
			return 0.0;
		}
	}
	public static double getDataFileDouble(String path, double defaultValue) {
		try {
			return dataYaml.getDouble(path);
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cERROR while reading &ddouble &cvalue from data file path '"+path+"'!");
			return defaultValue;
		}
	}
	public static boolean getDataFileBoolean(String path) {
		try {
			return dataYaml.getBoolean(path);
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cERROR while reading &dboolean &cvalue from data file path '"+path+"'!");
			return false;
		}
	}
	public static boolean getDataFileBoolean(String path, boolean defaultValue) {
		try {
			return dataYaml.getBoolean(path);
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cERROR while reading &dboolean &cvalue from data file path '"+path+"'!");
			return defaultValue;
		}
	}
	public static String getDataFileString(String path) {
		try {
			return dataYaml.getString(path);
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cERROR while reading &dstring &cvalue from data file path '"+path+"'!");
			return null;
		}
	}
	public static String getDataFileString(String path, String defaultValue) {
		try {
			return dataYaml.getString(path);
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cERROR while reading &dstring &cvalue from data file path '"+path+"'!");
			return defaultValue;
		}
	}
	public static List<String> getDataFileStringList(String path) {
		try {
			return dataYaml.getStringList(path);
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cERROR while reading &dstring list &cvalue from data file path '"+path+"'!");
			return null;
		}
	}
	public static void writeToDataFile(Consumer<FileConfiguration> writeable) {
		try {
			writeable.accept(dataYaml);
			dataYaml.save(dataFile);
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
		}
	}
	@SuppressWarnings("unchecked")
	public static <T> T computeSection(FileConfiguration config, String section, T toCompute) {
		try {
			Object value = config.get(section);
			if (value == null)
				return toCompute;
			if (toCompute instanceof Map && value instanceof org.bukkit.configuration.ConfigurationSection cs) {
	            return (T) cs.getValues(false);
	        }
	        if (!toCompute.getClass().isInstance(value)) {
	            return toCompute;
	        }
	        return (T) value;
		} catch (Exception e) {
			return toCompute;
		}
	}
	public static FileConfiguration getDataFile() {
		return dataYaml;
	}
	public static void saveDataFile() {
		try {
			dataYaml.save(dataFile);
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
		}
	}
}
