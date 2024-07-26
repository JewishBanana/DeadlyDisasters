package com.github.jewishbanana.deadlydisasters.handlers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.events.Disaster;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class WorldObject {
	
	public static Queue<WorldObject> worlds = new ArrayDeque<>();
	
	public static FileConfiguration yamlFile;
	
	private World world;
	public Set<Disaster> allowed = new HashSet<>();
	public Map<String, Object> settings;
	public int timer;
	public int offset;
	public int[] table = new int[6];
	public boolean naturalAllowed;
	public DifficultyLevel difficulty;
	public int maxRadius;
	public boolean protectRegions;
	public Set<UUID> whitelist = new HashSet<>();
	public boolean curePlagueInRegions;
	public FileConfiguration configFile;
	public Set<EntityType> blacklistedEntities = new HashSet<>();
	
	public WorldObject(World world, Main plugin) {
		this.world = world;
		reload(plugin);
	}
	public void reload(Main plugin) {
		allowed.clear();
		settings = yamlFile.getConfigurationSection(world.getName()+".general").getValues(false);
		configFile = plugin.cfgSwapper.getConfiguration((String) settings.get("config"));
		if (configFile == null) {
			Main.consoleSender.sendMessage(Utils.convertString("&e[DeadlyDisasters]: Could not set config for world &d'"+world.getName()+"' &e as the config &c'"+(String) settings.get("config")+"' &edoes not exist in the config folder!"));
			configFile = plugin.getConfig();
		}
		settings.put("region_protection", yamlFile.get(world.getName()+".external.region_plugins.region_protection"));
		settings.put("ignore_weather_effects_in_regions", yamlFile.get(world.getName()+".external.region_plugins.ignore_weather_effects_in_regions"));
		curePlagueInRegions = (boolean) yamlFile.get(world.getName()+".external.region_plugins.cure_plague_in_regions");
		protectRegions = (boolean) yamlFile.get(world.getName()+".external.region_plugins.region_protection");
		
		DifficultyLevel diff = DifficultyLevel.NORMAL;
		if (DifficultyLevel.forName(((String) yamlFile.get(world.getName()+".general.difficulty")).toUpperCase()) == null) {
			Main.consoleSender.sendMessage(Utils.convertString(Languages.prefix+"&c'"+(String) yamlFile.get(world.getName()+".general.difficulty")+"' is not a real difficulty level! &eSetting disaster difficulty for '"
					+ world.getName()+"' to NORMAL until you use '/disasters difficulty "+world.getName()+" <difficulty>' or fix this line in worlds.yml:&c\n"
					+ world.getName()+"\n  -> general\n    -> difficulty: "+(String) yamlFile.get(world.getName()+".general.difficulty")));
		} else
			diff = DifficultyLevel.valueOf(((String) yamlFile.get(world.getName()+".general.difficulty")).toUpperCase());
		settings.put("difficulty", diff);
		if (diff == DifficultyLevel.CUSTOM) {
			table[0] = (int) yamlFile.get(world.getName()+".custom_table.level_1");
			table[1] = (int) yamlFile.get(world.getName()+".custom_table.level_2");
			table[2] = (int) yamlFile.get(world.getName()+".custom_table.level_3");
			table[3] = (int) yamlFile.get(world.getName()+".custom_table.level_4");
			table[4] = (int) yamlFile.get(world.getName()+".custom_table.level_5");
			table[5] = (int) yamlFile.get(world.getName()+".custom_table.level_6");
			this.timer = (int) settings.get("min_timer");
			this.offset = (int) settings.get("disaster_offset");
		} else {
			table = diff.getTable();
			this.timer = diff.getTimer();
			this.offset = diff.getOffset();
		}
		Map<String, Object> map = yamlFile.getConfigurationSection(world.getName()+".disasters").getValues(false);
		for (Map.Entry<String, Object> values : map.entrySet()) {
			if (Boolean.valueOf((boolean) values.getValue()) == null || Disaster.forName(values.getKey()) == null) {
				Main.consoleSender.sendMessage(Utils.convertString(Languages.prefix+"&cWhy did you change this? Only change the true/false field! Something won't work right until you fix this line in worlds.yml:\n"
						+ world.getName()+"\n  -> disasters\n    -> "+values.getKey()+": "+values.getValue()));
				continue;
			}
			if ((boolean) values.getValue())
				allowed.add(Disaster.valueOf(values.getKey()));
		}
		whitelist.clear();
		for (String uuid : yamlFile.getStringList(world.getName()+".whitelist"))
			whitelist.add(UUID.fromString(uuid));
		
		this.naturalAllowed = (boolean) settings.get("natural_disasters");
		this.difficulty = diff;
		this.maxRadius = (int) settings.get("minDistanceRadius");
		
		blacklistedEntities.clear();
		configFile.getStringList("protection_settings.blacklisted_mob_types").forEach(e -> {
			try {
				blacklistedEntities.add(EntityType.valueOf(e.toUpperCase()));
			} catch (Exception ex) {
				Main.consoleSender.sendMessage(Languages.prefix+Utils.convertString("&cIncorrect mob type for blacklist &d'"+e+"' &cno such mob type exists! Ignoring this entry."));
			}
		});
	}
	public World getWorld() {
		return world;
	}
	public static void reloadWorlds(Main plugin) {
		yamlFile = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder().getAbsolutePath(), "worlds.yml"));
		for (WorldObject temp : worlds)
			temp.reload(plugin);
	}
	public static void saveYamlFile(Main plugin) {
		try {
			yamlFile.save(new File(plugin.getDataFolder().getAbsolutePath(), "worlds.yml"));
		} catch (IOException e) {
			Main.consoleSender.sendMessage(Utils.convertString("&c[DeadlyDisasters]: Error #00 Unable to save worlds file!"));
		}
	}
	public static WorldObject findWorldObject(World w) {
		for (WorldObject obj : worlds)
			if (obj.getWorld().equals(w))
				return obj;
		return null;
	}
	public static void changeAllField(String field, Object value, Main plugin) {
		for (WorldObject obj : worlds) {
			obj.settings.replace(field, value);
			yamlFile.set(obj.getWorld().getName()+".general."+field, value);
			if (field.equals("natural_disasters"))
				obj.naturalAllowed = (boolean) value;
		}
		saveYamlFile(plugin);
	}
	public static void changeDifficulty(WorldObject obj, DifficultyLevel diff) {
		if (diff == DifficultyLevel.CUSTOM) {
			String w = obj.getWorld().getName();
			int[] newTable = {(int) yamlFile.get(w+".custom_table.level_1"),
					(int) yamlFile.get(w+".custom_table.level_2"),
					(int) yamlFile.get(w+".custom_table.level_3"),
					(int) yamlFile.get(w+".custom_table.level_4"),
					(int) yamlFile.get(w+".custom_table.level_5"),
					(int) yamlFile.get(w+".custom_table.level_6")};
			obj.table = newTable;
			obj.timer = (int) obj.settings.get("min_timer");
			obj.offset = (int) obj.settings.get("disaster_offset");
		} else {
			obj.table = diff.getTable();
			obj.timer = diff.getTimer();
			obj.offset = diff.getOffset();
		}
		obj.difficulty = diff;
		yamlFile.set(obj.getWorld().getName()+".general.difficulty", diff.name());
	}
	public static void updateGlobalDisaster(Disaster disaster, boolean value, Main plugin) {
		for (WorldObject obj : worlds) {
			if (value) {
				if (!obj.allowed.contains(disaster))
					obj.allowed.add(disaster);
			} else
				if (obj.allowed.contains(disaster))
					obj.allowed.remove(disaster);
			yamlFile.set(obj.getWorld().getName()+".disasters."+disaster.name(), value);
		}
		saveYamlFile(plugin);
	}
	public int simulateLevel(Random rand) {
		int tempLevel = 0;
		int levelRatio = rand.nextInt(IntStream.of(table).sum())+1;
		int temp = table[0];
		for (int i = 0; i < 6; i++) {
			if (levelRatio <= temp) {
				tempLevel = i+1;
				break;
			}
			if (i > 5) {
				tempLevel = 1;
				break;
			}
			temp += table[i+1];
		}
		return tempLevel;
	}
	public int generateTimerValue(Random rand) {
		return rand.nextInt(timer/2)+timer;
	}
}