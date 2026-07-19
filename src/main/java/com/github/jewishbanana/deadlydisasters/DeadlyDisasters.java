package com.github.jewishbanana.deadlydisasters;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CompletableFuture;

import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.jewishbanana.deadlydisasters.commands.DisastersCommand;
import com.github.jewishbanana.deadlydisasters.disasters.Disaster;
import com.github.jewishbanana.deadlydisasters.disasters.DisasterRegistry;
import com.github.jewishbanana.deadlydisasters.disasters.DisasterSelector;
import com.github.jewishbanana.deadlydisasters.disasters.MobDisaster;
import com.github.jewishbanana.deadlydisasters.disasters.destructive.CaveIn;
import com.github.jewishbanana.deadlydisasters.disasters.destructive.Earthquake;
import com.github.jewishbanana.deadlydisasters.disasters.destructive.LavaGeyser;
import com.github.jewishbanana.deadlydisasters.disasters.destructive.Sinkhole;
import com.github.jewishbanana.deadlydisasters.disasters.destructive.Supernova;
import com.github.jewishbanana.deadlydisasters.disasters.destructive.Tornado;
import com.github.jewishbanana.deadlydisasters.disasters.destructive.Tsunami;
import com.github.jewishbanana.deadlydisasters.disasters.destructive.WaterGeyser;
import com.github.jewishbanana.deadlydisasters.disasters.mob.BlackPlague;
import com.github.jewishbanana.deadlydisasters.disasters.mob.Purge;
import com.github.jewishbanana.deadlydisasters.disasters.weather.AcidStorm;
import com.github.jewishbanana.deadlydisasters.disasters.weather.Blizzard;
import com.github.jewishbanana.deadlydisasters.disasters.weather.EndStorm;
import com.github.jewishbanana.deadlydisasters.disasters.weather.ExtremeWinds;
import com.github.jewishbanana.deadlydisasters.disasters.weather.MeteorShower;
import com.github.jewishbanana.deadlydisasters.disasters.weather.Sandstorm;
import com.github.jewishbanana.deadlydisasters.disasters.weather.Hurricane;
import com.github.jewishbanana.deadlydisasters.disasters.weather.SolarStorm;
import com.github.jewishbanana.deadlydisasters.disasters.weather.SoulStorm;
import com.github.jewishbanana.deadlydisasters.listeners.DeathMessageHandler;
import com.github.jewishbanana.deadlydisasters.listeners.DisasterFeaturesListener;
import com.github.jewishbanana.deadlydisasters.listeners.EntitiesListener;
import com.github.jewishbanana.deadlydisasters.listeners.LootGenerateListener;
import com.github.jewishbanana.deadlydisasters.listeners.PlayerListener;
import com.github.jewishbanana.deadlydisasters.listeners.WorldListener;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.ConfigUpdater;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.Metrics;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class DeadlyDisasters extends JavaPlugin {
	
	/*
	 * TODO:
	 * - Acid rain stop crop growth, melons and pumpkins, sugar cane, etc.
	 * - Force regen does not account for fire spread after starting
	 * - Regen bug, if block is broken and player places new block on spot, if that next block gets broken it does not regen as first block occupies map. Create second map to store excess blocks and drop them according after regen.
	 * - Regen bug, potential dupe block drops when regenerating. Actual block items dropping.
	 * Before Update:
	 * - Verify disaster categories in WorldWrapper.java
	 * - Verify config ignored sections in DataUtils.java
	 * - Verify config changes in DataUtils.java
	 */
	
	private static final String pluginSpigotPage = "https://www.spigotmc.org/resources/deadly-disasters.90806/";
	
	public static ConsoleCommandSender consoleSender;
	public static boolean isDisablingPlugin;
	
	private static DeadlyDisasters instance;
	private static FixedMetadataValue fixedData;
	
	public DisasterSelector selector;
	
	public void onLoad() {
		DependencyUtils.registerWorldGuardFlags(this);
	}
	
	public void onEnable() {
		instance = this;
		fixedData = new FixedMetadataValue(this, "protected");
		consoleSender = this.getServer().getConsoleSender();
		
		getConfig().options().copyDefaults(true);
		saveDefaultConfig();
		try {
			ConfigUpdater.update(this, "config.yml", new File(getDataFolder().getAbsolutePath(), "config.yml"), null);
			this.reloadConfig();
		} catch (IOException e) {
			Utils.sendExceptionLog(e);
		}
		
		registerDisasters();
		
		init();
		
		if (DataUtils.getMainConfigBoolean("general.check_for_update"))
			checkForUpdates();
	}
	public void registerDisasters() {
		// Destructive
		DisasterRegistry.registerDisaster("sinkhole", Sinkhole.class);
		DisasterRegistry.registerDisaster("earthquake", Earthquake.class);
		DisasterRegistry.registerDisaster("tornado", Tornado.class);
		DisasterRegistry.registerDisaster("cavein", CaveIn.class);
		DisasterRegistry.registerDisaster("water_geyser", WaterGeyser.class);
		DisasterRegistry.registerDisaster("lava_geyser", LavaGeyser.class);
		DisasterRegistry.registerDisaster("supernova", Supernova.class);
		DisasterRegistry.registerDisaster("tsunami", Tsunami.class);
		
		// Weather
		DisasterRegistry.registerDisaster("acid_storm", AcidStorm.class);
		DisasterRegistry.registerDisaster("sandstorm", Sandstorm.class);
		DisasterRegistry.registerDisaster("blizzard", Blizzard.class);
		DisasterRegistry.registerDisaster("extreme_winds", ExtremeWinds.class);
		DisasterRegistry.registerDisaster("soul_storm", SoulStorm.class);
		DisasterRegistry.registerDisaster("meteor_shower", MeteorShower.class);
		DisasterRegistry.registerDisaster("end_storm", EndStorm.class);
		DisasterRegistry.registerDisaster("solar_storm", SolarStorm.class);
		DisasterRegistry.registerDisaster("hurricane", Hurricane.class);

		// Mob
		DisasterRegistry.registerDisaster("black_plague", BlackPlague.class);
		DisasterRegistry.registerDisaster("purge", Purge.class);
	}
	public void init() {
		DataUtils.reload();
		Metrics.configureMetrics(this);
		
		WorldWrapper.init();
		DependencyUtils.init(this);
		selector = new DisasterSelector(this);
		
		reload();
		
		new DisastersCommand(this);
		new WorldListener(this);
		new EntitiesListener(this);
		new PlayerListener(this);
		new DeathMessageHandler(this);
		new DisasterFeaturesListener(this);
	}
	public void onDisable() {
		isDisablingPlugin = true;
		
		BlackPlague.saveAllInfections();
		MobDisaster.cleanAllEntities();
		Disaster.cleanUpDisastersEffects();
		
		selector.saveData();
		Disaster.flushMetricsData();
		Metrics.saveData();
	}
	public void reload() {
		DependencyUtils.reload(this);
		DataUtils.reload();
		BlockUtils.reload(this);
		WorldWrapper.reload();
		LootGenerateListener.reload();
	}
	public void checkForUpdates() {
		getLogger().info("Checking for update...");
		CompletableFuture.runAsync(new Runnable() {
			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				URL checkURL;
				URLConnection con;
				try {
					checkURL = new URL("https://api.spigotmc.org/legacy/update.php?resource=90806");
					con = checkURL.openConnection();
					BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
					String latestVersion = br.readLine();
					br.close();
					if (!getDescription().getVersion().equals(latestVersion)) {
						final String updateMessage = Utils.convertString(
								"&a" + Utils.symbolLine + Utils.symbolLine + Utils.symbolLine + "\n"
								+ Utils.prefix+DataUtils.getLanguageString("messages.internal.update_notify_console")
								.replaceAll("%version%", getDescription().getVersion())
								.replaceAll("%newversion%", latestVersion)
								.replaceAll("%webpage%", pluginSpigotPage)
								+ "\n&a" + Utils.symbolLine + Utils.symbolLine + Utils.symbolLine);
						consoleSender.sendMessage(Utils.convertString("\n&a" + Utils.symbolLine + Utils.symbolLine + updateMessage + Utils.symbolLine + Utils.symbolLine));
						
						if (DataUtils.getMainConfigBoolean("general.update_notify_admins"))
							PlayerListener.adminUpdateMessage = updateMessage;
					}
				} catch (Exception e) {
					Utils.sendConsoleMessage("&cERROR could not connect to spigot to check if a new plugin version is available!");
				}
			}
		});
	}
	public static DeadlyDisasters getInstance() {
		return instance;
	}
	/** Seconds until the next disaster for {@code player} in their world, or {@code -1} if none. Bridge for UltimateContent's baby end totem warning. */
	public static int getSecondsUntilDisaster(org.bukkit.entity.Player player) {
		return instance == null || instance.selector == null ? -1 : instance.selector.getSecondsUntilDisaster(player);
	}
	public FixedMetadataValue getFixedMetadata() {
		if (fixedData == null)
			fixedData = new FixedMetadataValue(getInstance(), "protected");
		return fixedData;	
	}
	public boolean isPluginPro() {
		return false;
	}
}
