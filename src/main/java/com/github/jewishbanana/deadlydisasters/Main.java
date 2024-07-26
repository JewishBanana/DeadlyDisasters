package com.github.jewishbanana.deadlydisasters;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.jewishbanana.deadlydisasters.commands.Disasters;
import com.github.jewishbanana.deadlydisasters.commands.TownyDisasters;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.CustomHead;
import com.github.jewishbanana.deadlydisasters.entities.EntityHandler;
import com.github.jewishbanana.deadlydisasters.events.Disaster;
import com.github.jewishbanana.deadlydisasters.events.disasters.BlackPlague;
import com.github.jewishbanana.deadlydisasters.events.disasters.Blizzard;
import com.github.jewishbanana.deadlydisasters.events.disasters.SandStorm;
import com.github.jewishbanana.deadlydisasters.handlers.Catalog;
import com.github.jewishbanana.deadlydisasters.handlers.ConfigSwapper;
import com.github.jewishbanana.deadlydisasters.handlers.ItemsHandler;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.handlers.SeasonsHandler;
import com.github.jewishbanana.deadlydisasters.handlers.TimerCheck;
import com.github.jewishbanana.deadlydisasters.handlers.WorldObject;
import com.github.jewishbanana.deadlydisasters.handlers.specialevents.SpecialEvent;
import com.github.jewishbanana.deadlydisasters.listeners.ArmorListener;
import com.github.jewishbanana.deadlydisasters.listeners.CoreListener;
import com.github.jewishbanana.deadlydisasters.listeners.CraftingListener;
import com.github.jewishbanana.deadlydisasters.listeners.CustomEnchantHandler;
import com.github.jewishbanana.deadlydisasters.listeners.CustomEntitiesListener;
import com.github.jewishbanana.deadlydisasters.listeners.DeathMessages;
import com.github.jewishbanana.deadlydisasters.listeners.TownyListener;
import com.github.jewishbanana.deadlydisasters.listeners.spawners.GlobalSpawner;
import com.github.jewishbanana.deadlydisasters.listeners.unloaders.Loader_ver_14;
import com.github.jewishbanana.deadlydisasters.listeners.unloaders.Loader_ver_17;
import com.github.jewishbanana.deadlydisasters.utils.ConfigUpdater;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.Metrics;
import com.github.jewishbanana.deadlydisasters.utils.NBSongs;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class Main extends JavaPlugin {
	
	public boolean RegionProtection;
	public boolean CProtect;
	
	public boolean updateNotify,firstSetup,firstAfterUpdate,debug,customNameSupport,isPro;
	private TimerCheck tc;
	private File dataf;
	public FileConfiguration dataFile;
	public String latestVersion;
	public static ConsoleCommandSender consoleSender;
	public double mcVersion,lastVersion;
	public SeasonsHandler seasonsHandler;
	public CustomEnchantHandler enchantHandler;
	public FixedMetadataValue fixedData;
	public ConfigSwapper cfgSwapper;
	public SpecialEvent eventHandler;
	public boolean noteBlockAPIEnabled;
	
	public Random random = new Random();
	public int maxDepth;
	
	private static Main instance;
	
	public static String pluginSpigotPage = "https://www.spigotmc.org/resources/deadly-disasters.90806/";
	
	public void onEnable() {
		instance = this;
		consoleSender = this.getServer().getConsoleSender();
		this.mcVersion = Double.parseDouble(Bukkit.getBukkitVersion().substring(0,4));
		if (mcVersion < 1.17)
			this.maxDepth = 0;
		else
			this.maxDepth = -64;
		fixedData = new FixedMetadataValue(this, "protected");
		
		if (!(new File(getDataFolder().getAbsolutePath(), "config.yml").exists()))
			firstSetup = true;
		getConfig().options().copyDefaults(true);
		saveDefaultConfig(); 
		if (firstSetup)
			try {
				ConfigUpdater.update(this, getResource("config.yml"), new File(getDataFolder().getAbsolutePath(), "config.yml"), Arrays.asList(""));
				this.reloadConfig();
			} catch (IOException e) {
				e.printStackTrace();
				consoleSender.sendMessage(Utils.convertString(Languages.prefix+"&cUnable to initialize config! Please report the full error above to the discord."));
			}
		
		dataf = new File(getDataFolder().getAbsolutePath(), "pluginData/data.yml");
		if (!dataf.exists()) {
			getLogger().info("Could not find data file in plugin directory! Creating new data file...");
			dataf.getParentFile().mkdirs();
			try {
				FileUtils.copyInputStreamToFile(getResource("files/data.yml"), dataf);
			} catch (IOException e) {
				e.printStackTrace();
				Utils.sendDebugMessage();
			}
			dataFile = YamlConfiguration.loadConfiguration(dataf);
			dataFile.set("data.version", getDescription().getVersion());
			saveDataFile();
		} else {
			dataFile = YamlConfiguration.loadConfiguration(dataf);
		}
		if (!dataFile.contains("data.lang")) {
			dataFile.set("data.lang", 0);
			saveDataFile();
		}
		
		CustomHead.init(this);
		
		Languages.defaultLang = YamlConfiguration.loadConfiguration(new InputStreamReader(getResource("lang/langEnglish.yml")));
		Languages.updateLang(dataFile.getInt("data.lang"), this, null);
		cfgSwapper = new ConfigSwapper(this);
		
		checkWorldsYaml();
		
		seasonsHandler = new SeasonsHandler(this);
		
		lastVersion = dataFile.getDouble("data.version");
		if (getConfig().contains("general.version") || dataFile.getDouble("data.version") < Double.parseDouble(this.getDescription().getVersion())) {
			updateConfig();
			updateWorldsYaml();
			if (seasonsHandler.isActive)
				seasonsHandler.updateSeasonsFile();
			dataFile.set("data.version", Double.parseDouble(this.getDescription().getVersion()));
			saveDataFile();
			firstAfterUpdate = true;
			consoleSender.sendMessage(Languages.prefix+Languages.joinAfterUpdate);
		}
		this.debug = getConfig().getBoolean("general.debug_messages");
		this.customNameSupport = getConfig().getBoolean("general.custom_name_support");
		
		for (World w : this.getServer().getWorlds())
			WorldObject.worlds.add(new WorldObject(w, this));
		
		PluginManager pm = getServer().getPluginManager();
		if (pm.isPluginEnabled("WorldGuard")) {
			Utils.WGuardB = true;
			getLogger().info("Successfully hooked into World Guard");
		}
		if (getCoreProtect() != null) {
			CProtect = true;
			getLogger().info("Successfully hooked into CoreProtect");
		}
		if (pm.isPluginEnabled("Towny")) {
			Utils.TownyB = true;
			new TownyListener(this);
			this.getCommand("towndisasters").setTabCompleter(new TownyDisasters(this));
			getLogger().info("Successfully hooked into Towny");
		}
		if (pm.isPluginEnabled("GriefPrevention")) {
			Utils.GriefB = true;
			getLogger().info("Successfully hooked into GriefPrevention");
		}
		if (pm.isPluginEnabled("Lands")) {
			Utils.LandsB = true;
			getLogger().info("Successfully hooked into Lands");
		}
		if (pm.isPluginEnabled("Kingdoms")) {
			Utils.KingsB = true;
			getLogger().info("Successfully hooked into KingdomsX");
		}
		if (Utils.WGuardB || Utils.TownyB || Utils.GriefB || Utils.LandsB || Utils.KingsB)
			RegionProtection = true;
		if (pm.isPluginEnabled("NoteBlockAPI")) {
			this.noteBlockAPIEnabled = true;
			NBSongs.init(this);
			getLogger().info("Successfully hooked into NoteBlockAPI");
		}
		new DependencyUtils(this);
		
		CustomEntityType.reload(this);

		new ArmorListener(this);
		tc = new TimerCheck(this, dataFile, random);
		CustomEntity.handler = new EntityHandler(this);
		if (mcVersion >= 1.17)
			new Loader_ver_17(this, CustomEntity.handler);
		else
			new Loader_ver_14(this, CustomEntity.handler);
		new CoreListener(this, tc, dataFile, random);
		enchantHandler = new CustomEnchantHandler(this);
		new CustomEntitiesListener(this);
		new CraftingListener(this);
		this.getCommand("disasters").setTabCompleter(new Disasters(this, tc, CustomEntity.handler, random, new Catalog(this)));
		new DeathMessages(this);
		new Utils(this);
		new GlobalSpawner(this);
		eventHandler = SpecialEvent.checkForEvent(this);
		
		Utils.easterEgg();
		checkForUpdates();
		
		ItemsHandler.createRecipes(this);
		
		SandStorm.sandStormBiomes.addAll(Arrays.asList(Biome.DESERT, Biome.BADLANDS, Biome.ERODED_BADLANDS));
		if (mcVersion < 1.18)
			SandStorm.sandStormBiomes.addAll(Arrays.asList(Biome.valueOf("DESERT_HILLS"), Biome.valueOf("DESERT_LAKES"), Biome.valueOf("MODIFIED_WOODED_BADLANDS_PLATEAU")));
		
		Disaster.reload(this);
		Disaster.GEYSER.setMetricsLabel("Water Geyser / Lava Geyser");
		
		File doomsday = new File(getDataFolder().getAbsolutePath(), "custom disasters/doomsday.yml");
		if (!doomsday.exists()) {
			try {
				doomsday.getParentFile().mkdirs();
				FileUtils.copyInputStreamToFile(getResource("files/doomsday.yml"), doomsday);
				consoleSender.sendMessage(Languages.prefix+Utils.convertString("&bInstalled &e'doomsday' &bsuccessfully!"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
//		consoleSender.sendMessage(Languages.prefix+Utils.convertString("&aEnjoying the plugin? Try the pro version which has many new features such as new &dnew disasters, new custom mobs, new custom items, regenerating worlds, and much more &aupgrade now to pro here https://www.spigotmc.org/resources/deadlydisasters-pro.100918/"));
		Blizzard.refreshFrozen(this);
		
		Metrics.configureMetrics(this);
	}
	public void onDisable() {
		try {
			removeCustomEntities();
			DeathMessages.purges.forEach(e -> e.clearBar());
		} catch (NoClassDefFoundError e) {}
		tc.saveTimerValues();
		CustomEntity.handler.cleanEntities();
		try {
			BlackPlague.time.forEach((k,v) -> {
				if (Bukkit.getEntity(k) != null)
					Bukkit.getEntity(k).removeMetadata("dd-plague", this);
			});
		} catch (NoClassDefFoundError e) {}
		long count = 0;
		if (dataFile.contains("timers"))
			count = dataFile.getConfigurationSection("timers").getKeys(false).stream().count();
		dataFile.set("data.entries", count);
		dataFile.set("data.firstStart", false);
		Catalog.saveTimer(this);
		if (eventHandler.isEnabled)
			eventHandler.saveData();
		Metrics.saveMetricsData(this);
		saveDataFile();
	}
	public void removeCustomEntities() {
		DeathMessages.endstorms.forEach(e -> e.clearEntities());
		DeathMessages.blizzards.forEach(e -> e.clearEntities());
		DeathMessages.sandstorms.forEach(e -> e.clearEntities());
		DeathMessages.soulstorms.forEach(e -> e.clearEntities());
		DeathMessages.solarstorms.forEach(e -> e.clearEntities());
		DeathMessages.purges.forEach(e -> e.clearEntities());
		DeathMessages.acidstorms.forEach(e -> e.clearEntities());
		DeathMessages.supernovas.forEach(e -> e.removeCrystal());
	}
	public void checkForUpdates() {
		getLogger().info("Checking for update...");
		CompletableFuture.runAsync(new Runnable() {
			@Override
			public void run() {
				URL checkURL;
				URLConnection con;
				try {
					checkURL = new URL("https://api.spigotmc.org/legacy/update.php?resource=90806");
					con = checkURL.openConnection();
					latestVersion = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();
				} catch (MalformedURLException e) {
					return;
				} catch (IOException e) {
					return;
				}
				if (!getDescription().getVersion().equals(latestVersion)) {
					String msg = Languages.langFile.getString("internal.consoleUpdate").replace("${plugin.page}", Main.pluginSpigotPage);
					consoleSender.sendMessage(Utils.convertString(Languages.prefix+"&a"+msg.substring(0, msg.indexOf('^'))+latestVersion+msg.substring(msg.indexOf('^')+1)));
					if (getConfig().getBoolean("general.update_notify"))
						updateNotify = true;
				}
			}
		});
	}
	private void updateConfig() {
		try {
			if (lastVersion < 10.5) {
				if (getConfig().getInt("purge.spawn_distance") >= 45)
					getConfig().set("purge.spawn_distance", 25);
				if (getConfig().getInt("tornado.max_entities.level 1") == 300)
					getConfig().set("tornado.max_entities.level 1", 200);
				if (getConfig().getInt("tornado.max_entities.level 2") == 600)
					getConfig().set("tornado.max_entities.level 2", 300);
				if (getConfig().getInt("tornado.max_entities.level 3") == 800)
					getConfig().set("tornado.max_entities.level 3", 500);
				if (getConfig().getInt("tornado.max_entities.level 4") == 1200)
					getConfig().set("tornado.max_entities.level 4", 800);
				if (getConfig().getInt("tornado.max_entities.level 5") == 1800)
					getConfig().set("tornado.max_entities.level 5", 1000);
				if (getConfig().getInt("tornado.max_entities.level 6") == 3000)
					getConfig().set("tornado.max_entities.level 6", 2000);
				saveConfig();
			}
			ConfigUpdater.update(this, Languages.fetchNewConfig(this, null), new File(getDataFolder().getAbsolutePath(), "config.yml"), Arrays.asList(""));
			reloadConfig();
			cfgSwapper.updateConfigFolder();
			
			if (lastVersion < 11.0) {
				for (String world : WorldObject.yamlFile.getKeys(false))
					WorldObject.yamlFile.set(world+".general.pet_warning_time", 0);
				WorldObject.saveYamlFile(this);
			}
			
			consoleSender.sendMessage(Utils.convertString(Languages.prefix+"&a"+Languages.langFile.getString("internal.cfgUpdate")+" "+getDescription().getVersion()));
		} catch (IOException e) {
			e.printStackTrace();
			consoleSender.sendMessage(Utils.convertString(Languages.prefix+"&cUnable to update config! Please report the full error above to the discord."));
		}
	}
	public void checkWorldsYaml() {
		File worldsf = new File(getDataFolder().getAbsolutePath(), "worlds.yml");
		FileConfiguration worldFile;
		if (!worldsf.exists()) {
			getLogger().info("Could not find worlds file in plugin directory! Creating new worlds file...");
			worldFile = YamlConfiguration.loadConfiguration(worldsf);
			for (World w : this.getServer().getWorlds())
				createWorldSection(w.getName(), worldFile);
			try {
				worldFile.save(worldsf);
			} catch (IOException e) {
				consoleSender.sendMessage(Utils.convertString(Languages.prefix+"&cCould not save worlds file in first init!"));
			}
		} else {
			worldFile = YamlConfiguration.loadConfiguration(worldsf);
			Set<String> worldsList = worldFile.getKeys(false);
			for (World w : this.getServer().getWorlds())
				if (!worldsList.contains(w.getName()))
					createWorldSection(w.getName(), worldFile);
			try {
				worldFile.save(worldsf);
			} catch (IOException e) {
				consoleSender.sendMessage(Utils.convertString(Languages.prefix+"&cCould not save worlds file in init!"));
			}
		}
		WorldObject.yamlFile = worldFile;
	}
	@SuppressWarnings("serial")
	public void createWorldSection(String name, FileConfiguration configuration) {
		configuration.createSection(name);
		Environment worldType = Bukkit.getWorld(name).getEnvironment();
		
		Map<String, Object> values = new LinkedHashMap<String, Object>() {{
			put("natural_disasters", getConfig().getBoolean("general.auto_enable_on_generation"));
	        put("min_timer", 90);
	        put("level_six", true);
	        put("event_broadcast", true);
	        put("disaster_offset", 10);
	        put("admin_override", true);
	        put("pet_warning_time", 30);
	        put("difficulty", "NORMAL");
	        put("minDistanceRadius", 50);
	        put("custom_mob_spawning", getConfig().getBoolean("general.auto_enable_natural_spawning"));
	        put("config", "DEFAULT");
	    }};
	    configuration.createSection(name+".general", values);
		values = new LinkedHashMap<String, Object>();
		for (Disaster type : Disaster.values()) {
			if (worldType == Environment.NETHER && type == Disaster.TORNADO)
				values.put(type.name(), false);
			else
				values.put(type.name(), true);
		}
		configuration.createSection(name+".disasters", values);
		configuration.createSection(name+".external");
		values = new LinkedHashMap<String, Object>() {{
			put("region_protection", true);
	        put("ignore_weather_effects_in_regions", true);
	        put("cure_plague_in_regions", true);
	    }};
	    configuration.createSection(name+".external.region_plugins", values);
	    values = new LinkedHashMap<String, Object>() {{
			put("level_1", 30);
			put("level_2", 25);
			put("level_3", 20);
			put("level_4", 15);
			put("level_5", 9);
			put("level_6", 1);
	    }};
	    configuration.createSection(name+".custom_table", values);
	    configuration.set(name+".whitelist", new ArrayList<String>());
	}
	@SuppressWarnings("serial")
	public void updateWorldsYaml() {
		Map<String, Object> generalValues = new LinkedHashMap<String, Object>() {{
			put("natural_disasters", getConfig().getBoolean("general.auto_enable_on_generation"));
	        put("min_timer", 90);
	        put("level_six", true);
	        put("event_broadcast", true);
	        put("disaster_offset", 10);
	        put("admin_override", true);
	        put("pet_warning_time", 30);
	        put("difficulty", "NORMAL");
	        put("minDistanceRadius", 50);
	        put("custom_mob_spawning", getConfig().getBoolean("general.auto_enable_natural_spawning"));
	        put("config", "DEFAULT");
		}};
	    Map<String, Object> custom_table = new LinkedHashMap<String, Object>() {{
			put("level_1", 30);
			put("level_2", 25);
			put("level_3", 20);
			put("level_4", 15);
			put("level_5", 9);
			put("level_6", 1);
	    }};
	    FileConfiguration worldFile = WorldObject.yamlFile;
	    for (String s : worldFile.getKeys(false)) {
	    	for (Map.Entry<String, Object> entries : generalValues.entrySet())
	    		if (!worldFile.contains(s+".general."+entries.getKey()))
	    			worldFile.set(s+".general."+entries.getKey(), entries.getValue());
	    	for (Disaster type : Disaster.values())
	    		if (!worldFile.contains(s+".disasters."+type.name()))
	    			worldFile.set(s+".disasters."+type.name(), true);
	    	for (Map.Entry<String, Object> entries : custom_table.entrySet())
	    		if (!worldFile.contains(s+".custom_table."+entries.getKey()))
	    			worldFile.set(s+".custom_table."+entries.getKey(), entries.getValue());
	    	if (!worldFile.contains(s+".whitelist"))
		    	worldFile.set(s+".whitelist", new ArrayList<String>());
	    }
	    WorldObject.saveYamlFile(this);
	}
	private net.coreprotect.CoreProtectAPI getCoreProtect() {
		Plugin plugin = getServer().getPluginManager().getPlugin("CoreProtect");
		if (plugin == null || !(plugin instanceof net.coreprotect.CoreProtect))
			return null;
		net.coreprotect.CoreProtectAPI CoreProtect = ((net.coreprotect.CoreProtect) plugin).getAPI();
		if (CoreProtect.isEnabled() == false)
			return null;
		if (CoreProtect.APIVersion() < 6)
			return null;
		return CoreProtect;
	}
	public void saveDataFile() {
		try {
			dataFile.save(dataf);
		} catch (IOException e) {
			consoleSender.sendMessage(Utils.convertString(Languages.prefix+"&cError #00 Unable to save data file!"));
		}
	}
	public static Main getInstance() {
		return instance;
	}
}