package deadlydisasters.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import deadlydisasters.disasters.Disaster;
import deadlydisasters.general.Main;
import deadlydisasters.general.WorldObject;

public class Metrics {

	private final Plugin plugin;

	private final MetricsBase metricsBase;
	
	public static Map<String, Integer> disasterOccurredMap = new LinkedHashMap<>();
	public static Map<String, Integer> disasterSpawnedMap = new LinkedHashMap<>();
	public static Map<String, Integer> disasterDestroyedMap = new LinkedHashMap<>();
	public static Map<String, Integer> disasterKillMap = new LinkedHashMap<>();
	
	public static void configureMetrics(Main plugin) {
		Metrics metrics = new Metrics(plugin, 16366);
		
		List<Integer> disasterOccurredList = new ArrayList<>();
		if (!plugin.dataFile.contains("metrics.occurred")) {
			for (int i=0; i < 18; i++)
				disasterOccurredList.add(0);
			plugin.dataFile.set("metrics.occurred", disasterOccurredList);
			plugin.saveDataFile();
		} else {
			List<Integer> tempList = plugin.dataFile.getIntegerList("metrics.occurred");
			for (int i=0; i < 18; i++)
				if (i < tempList.size())
					disasterOccurredList.add(tempList.get(i));
				else
					disasterOccurredList.add(0);
		}
		disasterOccurredMap.put(Disaster.CUSTOM.getMetricsLabel(), disasterOccurredList.get(0));
		disasterOccurredMap.put(Disaster.SINKHOLE.getMetricsLabel(), disasterOccurredList.get(1));
		disasterOccurredMap.put(Disaster.CAVEIN.getMetricsLabel(), disasterOccurredList.get(2));
		disasterOccurredMap.put(Disaster.TORNADO.getMetricsLabel(), disasterOccurredList.get(3));
		disasterOccurredMap.put(Disaster.GEYSER.getMetricsLabel(), disasterOccurredList.get(4));
		disasterOccurredMap.put(Disaster.PLAGUE.getMetricsLabel(), disasterOccurredList.get(5));
		disasterOccurredMap.put(Disaster.ACIDSTORM.getMetricsLabel(), disasterOccurredList.get(6));
		disasterOccurredMap.put(Disaster.EXTREMEWINDS.getMetricsLabel(), disasterOccurredList.get(7));
		disasterOccurredMap.put(Disaster.SOULSTORM.getMetricsLabel(), disasterOccurredList.get(8));
		disasterOccurredMap.put(Disaster.BLIZZARD.getMetricsLabel(), disasterOccurredList.get(9));
		disasterOccurredMap.put(Disaster.SANDSTORM.getMetricsLabel(), disasterOccurredList.get(10));
		disasterOccurredMap.put(Disaster.EARTHQUAKE.getMetricsLabel(), disasterOccurredList.get(11));
		disasterOccurredMap.put(Disaster.TSUNAMI.getMetricsLabel(), disasterOccurredList.get(12));
		disasterOccurredMap.put(Disaster.METEORSHOWERS.getMetricsLabel(), disasterOccurredList.get(13));
		disasterOccurredMap.put(Disaster.ENDSTORM.getMetricsLabel(), disasterOccurredList.get(14));
		disasterOccurredMap.put(Disaster.SUPERNOVA.getMetricsLabel(), disasterOccurredList.get(15));
		disasterOccurredMap.put(Disaster.HURRICANE.getMetricsLabel(), disasterOccurredList.get(16));
		disasterOccurredMap.put(Disaster.PURGE.getMetricsLabel(), disasterOccurredList.get(17));
		
		metrics.addCustomChart(new Metrics.AdvancedPie("disasters_occurred", new Callable<Map<String, Integer>>() {
	        @Override
	        public Map<String, Integer> call() throws Exception {
	        	HashMap<String, Integer> map = new HashMap<>();
	        	for (Map.Entry<String, Integer> entry : disasterOccurredMap.entrySet())
	        		if (entry.getValue() > 0)
	        			map.put(entry.getKey(), entry.getValue());
	            return map;
	        }
	    }));
		
		List<Integer> disasterSpawnedList = new ArrayList<>();
		if (!plugin.dataFile.contains("metrics.spawned")) {
			for (int i=0; i < 18; i++)
				disasterSpawnedList.add(0);
			plugin.dataFile.set("metrics.spawned", disasterSpawnedList);
			plugin.saveDataFile();
		} else {
			List<Integer> tempList = plugin.dataFile.getIntegerList("metrics.spawned");
			for (int i=0; i < 18; i++)
				if (i < tempList.size())
					disasterSpawnedList.add(tempList.get(i));
				else
					disasterSpawnedList.add(0);
		}
		disasterSpawnedMap.put(Disaster.CUSTOM.getMetricsLabel(), disasterSpawnedList.get(0));
		disasterSpawnedMap.put(Disaster.SINKHOLE.getMetricsLabel(), disasterSpawnedList.get(1));
		disasterSpawnedMap.put(Disaster.CAVEIN.getMetricsLabel(), disasterSpawnedList.get(2));
		disasterSpawnedMap.put(Disaster.TORNADO.getMetricsLabel(), disasterSpawnedList.get(3));
		disasterSpawnedMap.put(Disaster.GEYSER.getMetricsLabel(), disasterSpawnedList.get(4));
		disasterSpawnedMap.put(Disaster.PLAGUE.getMetricsLabel(), disasterSpawnedList.get(5));
		disasterSpawnedMap.put(Disaster.ACIDSTORM.getMetricsLabel(), disasterSpawnedList.get(6));
		disasterSpawnedMap.put(Disaster.EXTREMEWINDS.getMetricsLabel(), disasterSpawnedList.get(7));
		disasterSpawnedMap.put(Disaster.SOULSTORM.getMetricsLabel(), disasterSpawnedList.get(8));
		disasterSpawnedMap.put(Disaster.BLIZZARD.getMetricsLabel(), disasterSpawnedList.get(9));
		disasterSpawnedMap.put(Disaster.SANDSTORM.getMetricsLabel(), disasterSpawnedList.get(10));
		disasterSpawnedMap.put(Disaster.EARTHQUAKE.getMetricsLabel(), disasterSpawnedList.get(11));
		disasterSpawnedMap.put(Disaster.TSUNAMI.getMetricsLabel(), disasterSpawnedList.get(12));
		disasterSpawnedMap.put(Disaster.METEORSHOWERS.getMetricsLabel(), disasterSpawnedList.get(13));
		disasterSpawnedMap.put(Disaster.ENDSTORM.getMetricsLabel(), disasterSpawnedList.get(14));
		disasterSpawnedMap.put(Disaster.SUPERNOVA.getMetricsLabel(), disasterSpawnedList.get(15));
		disasterSpawnedMap.put(Disaster.HURRICANE.getMetricsLabel(), disasterSpawnedList.get(16));
		disasterSpawnedMap.put(Disaster.PURGE.getMetricsLabel(), disasterSpawnedList.get(17));
		
		metrics.addCustomChart(new Metrics.AdvancedPie("disasters_spawned", new Callable<Map<String, Integer>>() {
	        @Override
	        public Map<String, Integer> call() throws Exception {
	        	HashMap<String, Integer> map = new HashMap<>();
	        	for (Map.Entry<String, Integer> entry : disasterSpawnedMap.entrySet())
	        		if (entry.getValue() > 0)
	        			map.put(entry.getKey(), entry.getValue());
	            return map;
	        }
	    }));
		
		metrics.addCustomChart(new Metrics.AdvancedPie("favored_disaster", new Callable<Map<String, Integer>>() {
	        @Override
	        public Map<String, Integer> call() throws Exception {
	        	Map<String, Integer> map = new HashMap<>();
	        	String favored = "Not Submitted";
	        	if (plugin.dataFile.contains("data.favored") && !plugin.dataFile.getString("data.favored").equals("null"))
	        		favored = plugin.dataFile.getString("data.favored");
	        	map.put(favored, 1);
	            return map;
	        }
	    }));
		
		metrics.addCustomChart(new Metrics.AdvancedPie("disliked_disaster", new Callable<Map<String, Integer>>() {
	        @Override
	        public Map<String, Integer> call() throws Exception {
	        	Map<String, Integer> map = new HashMap<>();
	        	String disliked = "Not Submitted";
	        	if (plugin.dataFile.contains("data.disliked") && !plugin.dataFile.getString("data.disliked").equals("null"))
	        		disliked = plugin.dataFile.getString("data.disliked");
	        	map.put(disliked, 1);
	            return map;
	        }
	    }));
		
		metrics.addCustomChart(new Metrics.AdvancedPie("disaster_difficulty_level", new Callable<Map<String, Integer>>() {
	        @Override
	        public Map<String, Integer> call() throws Exception {
	        	Map<String, Integer> map = new HashMap<>();
	        	map.put(WorldObject.findWorldObject(Bukkit.getWorld("world")).difficulty.toString(), 1);
	            return map;
	        }
	    }));
		
		metrics.addCustomChart(new Metrics.AdvancedPie("custom_disasters_installed", new Callable<Map<String, Integer>>() {
	        @Override
	        public Map<String, Integer> call() throws Exception {
	        	Map<String, Integer> map = new HashMap<>();
	        	int amount = new File(plugin.getDataFolder().getAbsolutePath(), "custom disasters").listFiles().length;
	        	if (amount <= 1)
	        		map.put("1", 1);
	        	else if (amount == 2)
	        		map.put("2", 1);
	        	else if (amount == 3)
	        		map.put("3", 1);
	        	else if (amount == 4)
	        		map.put("4", 1);
	        	else if (amount == 5)
	        		map.put("5", 1);
	        	else if (amount > 5)
	        		map.put("More Than 5", 1);
	        	else if (amount > 10)
	        		map.put("More Than 10", 1);
	            return map;
	        }
	    }));
		
		List<Integer> disasterDestroyedList = new ArrayList<>();
		if (!plugin.dataFile.contains("metrics.destroyed")) {
			for (int i=0; i < 11; i++)
				disasterDestroyedList.add(0);
			plugin.dataFile.set("metrics.destroyed", disasterDestroyedList);
			plugin.saveDataFile();
		} else {
			List<Integer> tempList = plugin.dataFile.getIntegerList("metrics.destroyed");
			for (int i=0; i < 11; i++)
				if (i < tempList.size())
					disasterDestroyedList.add(tempList.get(i));
				else
					disasterDestroyedList.add(0);
		}
		disasterDestroyedMap.put(Disaster.SINKHOLE.getMetricsLabel(), disasterDestroyedList.get(0));
		disasterDestroyedMap.put(Disaster.CAVEIN.getMetricsLabel(), disasterDestroyedList.get(1));
		disasterDestroyedMap.put(Disaster.TORNADO.getMetricsLabel(), disasterDestroyedList.get(2));
		disasterDestroyedMap.put(Disaster.GEYSER.getMetricsLabel(), disasterDestroyedList.get(3));
		disasterDestroyedMap.put(Disaster.ACIDSTORM.getMetricsLabel(), disasterDestroyedList.get(4));
		disasterDestroyedMap.put(Disaster.EXTREMEWINDS.getMetricsLabel(), disasterDestroyedList.get(5));
		disasterDestroyedMap.put(Disaster.EARTHQUAKE.getMetricsLabel(), disasterDestroyedList.get(6));
		disasterDestroyedMap.put(Disaster.TSUNAMI.getMetricsLabel(), disasterDestroyedList.get(7));
		disasterDestroyedMap.put(Disaster.METEORSHOWERS.getMetricsLabel(), disasterDestroyedList.get(8));
		disasterDestroyedMap.put(Disaster.SUPERNOVA.getMetricsLabel(), disasterDestroyedList.get(9));
		disasterDestroyedMap.put(Disaster.HURRICANE.getMetricsLabel(), disasterDestroyedList.get(10));
		
		metrics.addCustomChart(new Metrics.AdvancedPie("blocks_destroyed", new Callable<Map<String, Integer>>() {
	        @Override
	        public Map<String, Integer> call() throws Exception {
	        	HashMap<String, Integer> map = new HashMap<>();
	        	for (Map.Entry<String, Integer> entry : disasterDestroyedMap.entrySet())
	        		if (entry.getValue() > 0)
	        			map.put(entry.getKey(), entry.getValue());
	            return map;
	        }
	    }));
		
		List<Integer> disasterKillList = new ArrayList<>();
		if (!plugin.dataFile.contains("metrics.killed")) {
			for (int i=0; i < 17; i++)
				disasterKillList.add(0);
			plugin.dataFile.set("metrics.killed", disasterKillList);
			plugin.saveDataFile();
		} else {
			List<Integer> tempList = plugin.dataFile.getIntegerList("metrics.killed");
			for (int i=0; i < 17; i++)
				if (i < tempList.size())
					disasterKillList.add(tempList.get(i));
				else
					disasterKillList.add(0);
		}
		disasterKillMap.put(Disaster.SINKHOLE.getMetricsLabel(), disasterKillList.get(0));
		disasterKillMap.put(Disaster.CAVEIN.getMetricsLabel(), disasterKillList.get(1));
		disasterKillMap.put(Disaster.TORNADO.getMetricsLabel(), disasterKillList.get(2));
		disasterKillMap.put(Disaster.GEYSER.getMetricsLabel(), disasterKillList.get(3));
		disasterKillMap.put(Disaster.PLAGUE.getMetricsLabel(), disasterKillList.get(4));
		disasterKillMap.put(Disaster.ACIDSTORM.getMetricsLabel(), disasterKillList.get(5));
		disasterKillMap.put(Disaster.EXTREMEWINDS.getMetricsLabel(), disasterKillList.get(6));
		disasterKillMap.put(Disaster.SOULSTORM.getMetricsLabel(), disasterKillList.get(7));
		disasterKillMap.put(Disaster.BLIZZARD.getMetricsLabel(), disasterKillList.get(8));
		disasterKillMap.put(Disaster.SANDSTORM.getMetricsLabel(), disasterKillList.get(9));
		disasterKillMap.put(Disaster.EARTHQUAKE.getMetricsLabel(), disasterKillList.get(10));
		disasterKillMap.put(Disaster.TSUNAMI.getMetricsLabel(), disasterKillList.get(11));
		disasterKillMap.put(Disaster.METEORSHOWERS.getMetricsLabel(), disasterKillList.get(12));
		disasterKillMap.put(Disaster.ENDSTORM.getMetricsLabel(), disasterKillList.get(13));
		disasterKillMap.put(Disaster.SUPERNOVA.getMetricsLabel(), disasterKillList.get(14));
		disasterKillMap.put(Disaster.HURRICANE.getMetricsLabel(), disasterKillList.get(15));
		disasterKillMap.put(Disaster.PURGE.getMetricsLabel(), disasterKillList.get(16));
		
		metrics.addCustomChart(new Metrics.AdvancedPie("players_killed", new Callable<Map<String, Integer>>() {
	        @Override
	        public Map<String, Integer> call() throws Exception {
	        	HashMap<String, Integer> map = new HashMap<>();
	        	for (Map.Entry<String, Integer> entry : disasterKillMap.entrySet())
	        		if (entry.getValue() > 0)
	        			map.put(entry.getKey(), entry.getValue());
	            return map;
	        }
	    }));
	}
	
	public static void saveMetricsData(Main plugin) {
		List<Integer> intList = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : disasterOccurredMap.entrySet())
			intList.add(entry.getValue());
		plugin.dataFile.set("metrics.occurred", intList);
		intList.clear();
		for (Map.Entry<String, Integer> entry : disasterSpawnedMap.entrySet())
			intList.add(entry.getValue());
		plugin.dataFile.set("metrics.spawned", intList);
		intList.clear();
		for (Map.Entry<String, Integer> entry : disasterDestroyedMap.entrySet())
			intList.add(entry.getValue());
		plugin.dataFile.set("metrics.destroyed", intList);
		intList.clear();
		for (Map.Entry<String, Integer> entry : disasterKillMap.entrySet())
			intList.add(entry.getValue());
		plugin.dataFile.set("metrics.killed", intList);
	}
	
	public static void incrementValue(Map<String, Integer> map, String value) {
		map.replace(value, map.get(value) + 1);
	}
	
	public static void incrementValue(Map<String, Integer> map, String value, int increment) {
		map.replace(value, map.get(value) + increment);
	}

	/**
	 * Creates a new Metrics instance.
	 *
	 * @param plugin    Your plugin instance.
	 * @param serviceId The id of the service. It can be found at
	 *                  <a href="https://bstats.org/what-is-my-plugin-id">What is my
	 *                  plugin id?</a>
	 */
	@SuppressWarnings("deprecation")
	public Metrics(JavaPlugin plugin, int serviceId) {
		this.plugin = plugin;
		// Get the config file
		File bStatsFolder = new File(plugin.getDataFolder().getParentFile(), "bStats");
		File configFile = new File(bStatsFolder, "config.yml");
		YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		if (!config.isSet("serverUuid")) {
			config.addDefault("enabled", true);
			config.addDefault("serverUuid", UUID.randomUUID().toString());
			config.addDefault("logFailedRequests", false);
			config.addDefault("logSentData", false);
			config.addDefault("logResponseStatusText", false);
			// Inform the server owners about bStats
			config.options()
					.header("bStats (https://bStats.org) collects some basic information for plugin authors, like how\n"
							+ "many people use their plugin and their total player count. It's recommended to keep bStats\n"
							+ "enabled, but if you're not comfortable with this, you can turn this setting off. There is no\n"
							+ "performance penalty associated with having metrics enabled, and data sent to bStats is fully\n"
							+ "anonymous.")
					.copyDefaults(true);
			try {
				config.save(configFile);
			} catch (IOException ignored) {
			}
		}
		// Load the data
		boolean enabled = config.getBoolean("enabled", true);
		String serverUUID = config.getString("serverUuid");
		boolean logErrors = config.getBoolean("logFailedRequests", false);
		boolean logSentData = config.getBoolean("logSentData", false);
		boolean logResponseStatusText = config.getBoolean("logResponseStatusText", false);
		metricsBase = new MetricsBase("bukkit", serverUUID, serviceId, enabled, this::appendPlatformData,
				this::appendServiceData, submitDataTask -> Bukkit.getScheduler().runTask(plugin, submitDataTask),
				plugin::isEnabled, (message, error) -> this.plugin.getLogger().log(Level.WARNING, message, error),
				(message) -> this.plugin.getLogger().log(Level.INFO, message), logErrors, logSentData,
				logResponseStatusText);
	}

	/**
	 * Adds a custom chart.
	 *
	 * @param chart The chart to add.
	 */
	public void addCustomChart(CustomChart chart) {
		metricsBase.addCustomChart(chart);
	}

	private void appendPlatformData(JsonObjectBuilder builder) {
		builder.appendField("playerAmount", getPlayerAmount());
		builder.appendField("onlineMode", Bukkit.getOnlineMode() ? 1 : 0);
		builder.appendField("bukkitVersion", Bukkit.getVersion());
		builder.appendField("bukkitName", Bukkit.getName());
		builder.appendField("javaVersion", System.getProperty("java.version"));
		builder.appendField("osName", System.getProperty("os.name"));
		builder.appendField("osArch", System.getProperty("os.arch"));
		builder.appendField("osVersion", System.getProperty("os.version"));
		builder.appendField("coreCount", Runtime.getRuntime().availableProcessors());
	}

	private void appendServiceData(JsonObjectBuilder builder) {
		builder.appendField("pluginVersion", plugin.getDescription().getVersion());
	}

	private int getPlayerAmount() {
		try {
			// Around MC 1.8 the return type was changed from an array to a collection,
			// This fixes java.lang.NoSuchMethodError:
			// org.bukkit.Bukkit.getOnlinePlayers()Ljava/util/Collection;
			Method onlinePlayersMethod = Class.forName("org.bukkit.Server").getMethod("getOnlinePlayers");
			return onlinePlayersMethod.getReturnType().equals(Collection.class)
					? ((Collection<?>) onlinePlayersMethod.invoke(Bukkit.getServer())).size()
					: ((Player[]) onlinePlayersMethod.invoke(Bukkit.getServer())).length;
		} catch (Exception e) {
			// Just use the new method if the reflection failed
			return Bukkit.getOnlinePlayers().size();
		}
	}

	public static class MetricsBase {

		/** The version of the Metrics class. */
		public static final String METRICS_VERSION = "3.0.0";

		private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1,
				task -> new Thread(task, "bStats-Metrics"));

		private static final String REPORT_URL = "https://bStats.org/api/v2/data/%s";

		private final String platform;

		private final String serverUuid;

		private final int serviceId;

		private final Consumer<JsonObjectBuilder> appendPlatformDataConsumer;

		private final Consumer<JsonObjectBuilder> appendServiceDataConsumer;

		private final Consumer<Runnable> submitTaskConsumer;

		private final Supplier<Boolean> checkServiceEnabledSupplier;

		private final BiConsumer<String, Throwable> errorLogger;

		private final Consumer<String> infoLogger;

		private final boolean logErrors;

		private final boolean logSentData;

		private final boolean logResponseStatusText;

		private final Set<CustomChart> customCharts = new HashSet<>();

		private final boolean enabled;

		/**
		 * Creates a new MetricsBase class instance.
		 *
		 * @param platform                    The platform of the service.
		 * @param serviceId                   The id of the service.
		 * @param serverUuid                  The server uuid.
		 * @param enabled                     Whether or not data sending is enabled.
		 * @param appendPlatformDataConsumer  A consumer that receives a
		 *                                    {@code JsonObjectBuilder} and appends all
		 *                                    platform-specific data.
		 * @param appendServiceDataConsumer   A consumer that receives a
		 *                                    {@code JsonObjectBuilder} and appends all
		 *                                    service-specific data.
		 * @param submitTaskConsumer          A consumer that takes a runnable with the
		 *                                    submit task. This can be used to delegate
		 *                                    the data collection to a another thread to
		 *                                    prevent errors caused by concurrency. Can
		 *                                    be {@code null}.
		 * @param checkServiceEnabledSupplier A supplier to check if the service is
		 *                                    still enabled.
		 * @param errorLogger                 A consumer that accepts log message and an
		 *                                    error.
		 * @param infoLogger                  A consumer that accepts info log messages.
		 * @param logErrors                   Whether or not errors should be logged.
		 * @param logSentData                 Whether or not the sent data should be
		 *                                    logged.
		 * @param logResponseStatusText       Whether or not the response status text
		 *                                    should be logged.
		 */
		public MetricsBase(String platform, String serverUuid, int serviceId, boolean enabled,
				Consumer<JsonObjectBuilder> appendPlatformDataConsumer,
				Consumer<JsonObjectBuilder> appendServiceDataConsumer, Consumer<Runnable> submitTaskConsumer,
				Supplier<Boolean> checkServiceEnabledSupplier, BiConsumer<String, Throwable> errorLogger,
				Consumer<String> infoLogger, boolean logErrors, boolean logSentData, boolean logResponseStatusText) {
			this.platform = platform;
			this.serverUuid = serverUuid;
			this.serviceId = serviceId;
			this.enabled = enabled;
			this.appendPlatformDataConsumer = appendPlatformDataConsumer;
			this.appendServiceDataConsumer = appendServiceDataConsumer;
			this.submitTaskConsumer = submitTaskConsumer;
			this.checkServiceEnabledSupplier = checkServiceEnabledSupplier;
			this.errorLogger = errorLogger;
			this.infoLogger = infoLogger;
			this.logErrors = logErrors;
			this.logSentData = logSentData;
			this.logResponseStatusText = logResponseStatusText;
			checkRelocation();
			if (enabled) {
				// WARNING: Removing the option to opt-out will get your plugin banned from
				// bStats
				startSubmitting();
			}
		}

		public void addCustomChart(CustomChart chart) {
			this.customCharts.add(chart);
		}

		private void startSubmitting() {
			final Runnable submitTask = () -> {
				if (!enabled || !checkServiceEnabledSupplier.get()) {
					// Submitting data or service is disabled
					scheduler.shutdown();
					return;
				}
				if (submitTaskConsumer != null) {
					submitTaskConsumer.accept(this::submitData);
				} else {
					this.submitData();
				}
			};
			// Many servers tend to restart at a fixed time at xx:00 which causes an uneven
			// distribution
			// of requests on the
			// bStats backend. To circumvent this problem, we introduce some randomness into
			// the initial
			// and second delay.
			// WARNING: You must not modify and part of this Metrics class, including the
			// submit delay or
			// frequency!
			// WARNING: Modifying this code will get your plugin banned on bStats. Just
			// don't do it!
			long initialDelay = (long) (1000 * 60 * (3 + Math.random() * 3));
			long secondDelay = (long) (1000 * 60 * (Math.random() * 30));
			scheduler.schedule(submitTask, initialDelay, TimeUnit.MILLISECONDS);
			scheduler.scheduleAtFixedRate(submitTask, initialDelay + secondDelay, 1000 * 60 * 30,
					TimeUnit.MILLISECONDS);
		}

		private void submitData() {
			final JsonObjectBuilder baseJsonBuilder = new JsonObjectBuilder();
			appendPlatformDataConsumer.accept(baseJsonBuilder);
			final JsonObjectBuilder serviceJsonBuilder = new JsonObjectBuilder();
			appendServiceDataConsumer.accept(serviceJsonBuilder);
			JsonObjectBuilder.JsonObject[] chartData = customCharts.stream()
					.map(customChart -> customChart.getRequestJsonObject(errorLogger, logErrors))
					.filter(Objects::nonNull).toArray(JsonObjectBuilder.JsonObject[]::new);
			serviceJsonBuilder.appendField("id", serviceId);
			serviceJsonBuilder.appendField("customCharts", chartData);
			baseJsonBuilder.appendField("service", serviceJsonBuilder.build());
			baseJsonBuilder.appendField("serverUUID", serverUuid);
			baseJsonBuilder.appendField("metricsVersion", METRICS_VERSION);
			JsonObjectBuilder.JsonObject data = baseJsonBuilder.build();
			scheduler.execute(() -> {
				try {
					// Send the data
					sendData(data);
				} catch (Exception e) {
					// Something went wrong! :(
					if (logErrors) {
						errorLogger.accept("Could not submit bStats metrics data", e);
					}
				}
			});
		}

		private void sendData(JsonObjectBuilder.JsonObject data) throws Exception {
			if (logSentData) {
				infoLogger.accept("Sent bStats metrics data: " + data.toString());
			}
			String url = String.format(REPORT_URL, platform);
			HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
			// Compress the data to save bandwidth
			byte[] compressedData = compress(data.toString());
			connection.setRequestMethod("POST");
			connection.addRequestProperty("Accept", "application/json");
			connection.addRequestProperty("Connection", "close");
			connection.addRequestProperty("Content-Encoding", "gzip");
			connection.addRequestProperty("Content-Length", String.valueOf(compressedData.length));
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("User-Agent", "Metrics-Service/1");
			connection.setDoOutput(true);
			try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
				outputStream.write(compressedData);
			}
			StringBuilder builder = new StringBuilder();
			try (BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(connection.getInputStream()))) {
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					builder.append(line);
				}
			}
			if (logResponseStatusText) {
				infoLogger.accept("Sent data to bStats and received response: " + builder);
			}
		}

		/** Checks that the class was properly relocated. */
		private void checkRelocation() {
			// You can use the property to disable the check in your test environment
			if (System.getProperty("bstats.relocatecheck") == null
					|| !System.getProperty("bstats.relocatecheck").equals("false")) {
				// Maven's Relocate is clever and changes strings, too. So we have to use this
				// little
				// "trick" ... :D
				final String defaultPackage = new String(
						new byte[] { 'o', 'r', 'g', '.', 'b', 's', 't', 'a', 't', 's' });
				final String examplePackage = new String(
						new byte[] { 'y', 'o', 'u', 'r', '.', 'p', 'a', 'c', 'k', 'a', 'g', 'e' });
				// We want to make sure no one just copy & pastes the example and uses the wrong
				// package
				// names
				if (MetricsBase.class.getPackage().getName().startsWith(defaultPackage)
						|| MetricsBase.class.getPackage().getName().startsWith(examplePackage)) {
					throw new IllegalStateException("bStats Metrics class has not been relocated correctly!");
				}
			}
		}

		/**
		 * Gzips the given string.
		 *
		 * @param str The string to gzip.
		 * @return The gzipped string.
		 */
		private static byte[] compress(final String str) throws IOException {
			if (str == null) {
				return null;
			}
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			try (GZIPOutputStream gzip = new GZIPOutputStream(outputStream)) {
				gzip.write(str.getBytes(StandardCharsets.UTF_8));
			}
			return outputStream.toByteArray();
		}
	}

	public static class DrilldownPie extends CustomChart {

		private final Callable<Map<String, Map<String, Integer>>> callable;

		/**
		 * Class constructor.
		 *
		 * @param chartId  The id of the chart.
		 * @param callable The callable which is used to request the chart data.
		 */
		public DrilldownPie(String chartId, Callable<Map<String, Map<String, Integer>>> callable) {
			super(chartId);
			this.callable = callable;
		}

		@Override
		public JsonObjectBuilder.JsonObject getChartData() throws Exception {
			JsonObjectBuilder valuesBuilder = new JsonObjectBuilder();
			Map<String, Map<String, Integer>> map = callable.call();
			if (map == null || map.isEmpty()) {
				// Null = skip the chart
				return null;
			}
			boolean reallyAllSkipped = true;
			for (Map.Entry<String, Map<String, Integer>> entryValues : map.entrySet()) {
				JsonObjectBuilder valueBuilder = new JsonObjectBuilder();
				boolean allSkipped = true;
				for (Map.Entry<String, Integer> valueEntry : map.get(entryValues.getKey()).entrySet()) {
					valueBuilder.appendField(valueEntry.getKey(), valueEntry.getValue());
					allSkipped = false;
				}
				if (!allSkipped) {
					reallyAllSkipped = false;
					valuesBuilder.appendField(entryValues.getKey(), valueBuilder.build());
				}
			}
			if (reallyAllSkipped) {
				// Null = skip the chart
				return null;
			}
			return new JsonObjectBuilder().appendField("values", valuesBuilder.build()).build();
		}
	}

	public static class AdvancedPie extends CustomChart {

		private final Callable<Map<String, Integer>> callable;

		/**
		 * Class constructor.
		 *
		 * @param chartId  The id of the chart.
		 * @param callable The callable which is used to request the chart data.
		 */
		public AdvancedPie(String chartId, Callable<Map<String, Integer>> callable) {
			super(chartId);
			this.callable = callable;
		}

		@Override
		protected JsonObjectBuilder.JsonObject getChartData() throws Exception {
			JsonObjectBuilder valuesBuilder = new JsonObjectBuilder();
			Map<String, Integer> map = callable.call();
			if (map == null || map.isEmpty()) {
				// Null = skip the chart
				return null;
			}
			boolean allSkipped = true;
			for (Map.Entry<String, Integer> entry : map.entrySet()) {
				if (entry.getValue() == 0) {
					// Skip this invalid
					continue;
				}
				allSkipped = false;
				valuesBuilder.appendField(entry.getKey(), entry.getValue());
			}
			if (allSkipped) {
				// Null = skip the chart
				return null;
			}
			return new JsonObjectBuilder().appendField("values", valuesBuilder.build()).build();
		}
	}

	public static class MultiLineChart extends CustomChart {

		private final Callable<Map<String, Integer>> callable;

		/**
		 * Class constructor.
		 *
		 * @param chartId  The id of the chart.
		 * @param callable The callable which is used to request the chart data.
		 */
		public MultiLineChart(String chartId, Callable<Map<String, Integer>> callable) {
			super(chartId);
			this.callable = callable;
		}

		@Override
		protected JsonObjectBuilder.JsonObject getChartData() throws Exception {
			JsonObjectBuilder valuesBuilder = new JsonObjectBuilder();
			Map<String, Integer> map = callable.call();
			if (map == null || map.isEmpty()) {
				// Null = skip the chart
				return null;
			}
			boolean allSkipped = true;
			for (Map.Entry<String, Integer> entry : map.entrySet()) {
				if (entry.getValue() == 0) {
					// Skip this invalid
					continue;
				}
				allSkipped = false;
				valuesBuilder.appendField(entry.getKey(), entry.getValue());
			}
			if (allSkipped) {
				// Null = skip the chart
				return null;
			}
			return new JsonObjectBuilder().appendField("values", valuesBuilder.build()).build();
		}
	}

	public static class SimpleBarChart extends CustomChart {

		private final Callable<Map<String, Integer>> callable;

		/**
		 * Class constructor.
		 *
		 * @param chartId  The id of the chart.
		 * @param callable The callable which is used to request the chart data.
		 */
		public SimpleBarChart(String chartId, Callable<Map<String, Integer>> callable) {
			super(chartId);
			this.callable = callable;
		}

		@Override
		protected JsonObjectBuilder.JsonObject getChartData() throws Exception {
			JsonObjectBuilder valuesBuilder = new JsonObjectBuilder();
			Map<String, Integer> map = callable.call();
			if (map == null || map.isEmpty()) {
				// Null = skip the chart
				return null;
			}
			for (Map.Entry<String, Integer> entry : map.entrySet()) {
				valuesBuilder.appendField(entry.getKey(), new int[] { entry.getValue() });
			}
			return new JsonObjectBuilder().appendField("values", valuesBuilder.build()).build();
		}
	}

	public abstract static class CustomChart {

		private final String chartId;

		protected CustomChart(String chartId) {
			if (chartId == null) {
				throw new IllegalArgumentException("chartId must not be null");
			}
			this.chartId = chartId;
		}

		public JsonObjectBuilder.JsonObject getRequestJsonObject(BiConsumer<String, Throwable> errorLogger,
				boolean logErrors) {
			JsonObjectBuilder builder = new JsonObjectBuilder();
			builder.appendField("chartId", chartId);
			try {
				JsonObjectBuilder.JsonObject data = getChartData();
				if (data == null) {
					// If the data is null we don't send the chart.
					return null;
				}
				builder.appendField("data", data);
			} catch (Throwable t) {
				if (logErrors) {
					errorLogger.accept("Failed to get data for custom chart with id " + chartId, t);
				}
				return null;
			}
			return builder.build();
		}

		protected abstract JsonObjectBuilder.JsonObject getChartData() throws Exception;
	}

	public static class SimplePie extends CustomChart {

		private final Callable<String> callable;

		/**
		 * Class constructor.
		 *
		 * @param chartId  The id of the chart.
		 * @param callable The callable which is used to request the chart data.
		 */
		public SimplePie(String chartId, Callable<String> callable) {
			super(chartId);
			this.callable = callable;
		}

		@Override
		protected JsonObjectBuilder.JsonObject getChartData() throws Exception {
			String value = callable.call();
			if (value == null || value.isEmpty()) {
				// Null = skip the chart
				return null;
			}
			return new JsonObjectBuilder().appendField("value", value).build();
		}
	}

	public static class AdvancedBarChart extends CustomChart {

		private final Callable<Map<String, int[]>> callable;

		/**
		 * Class constructor.
		 *
		 * @param chartId  The id of the chart.
		 * @param callable The callable which is used to request the chart data.
		 */
		public AdvancedBarChart(String chartId, Callable<Map<String, int[]>> callable) {
			super(chartId);
			this.callable = callable;
		}

		@Override
		protected JsonObjectBuilder.JsonObject getChartData() throws Exception {
			JsonObjectBuilder valuesBuilder = new JsonObjectBuilder();
			Map<String, int[]> map = callable.call();
			if (map == null || map.isEmpty()) {
				// Null = skip the chart
				return null;
			}
			boolean allSkipped = true;
			for (Map.Entry<String, int[]> entry : map.entrySet()) {
				if (entry.getValue().length == 0) {
					// Skip this invalid
					continue;
				}
				allSkipped = false;
				valuesBuilder.appendField(entry.getKey(), entry.getValue());
			}
			if (allSkipped) {
				// Null = skip the chart
				return null;
			}
			return new JsonObjectBuilder().appendField("values", valuesBuilder.build()).build();
		}
	}

	public static class SingleLineChart extends CustomChart {

		private final Callable<Integer> callable;

		/**
		 * Class constructor.
		 *
		 * @param chartId  The id of the chart.
		 * @param callable The callable which is used to request the chart data.
		 */
		public SingleLineChart(String chartId, Callable<Integer> callable) {
			super(chartId);
			this.callable = callable;
		}

		@Override
		protected JsonObjectBuilder.JsonObject getChartData() throws Exception {
			int value = callable.call();
			if (value == 0) {
				// Null = skip the chart
				return null;
			}
			return new JsonObjectBuilder().appendField("value", value).build();
		}
	}

	/**
	 * An extremely simple JSON builder.
	 *
	 * <p>
	 * While this class is neither feature-rich nor the most performant one, it's
	 * sufficient enough for its use-case.
	 */
	public static class JsonObjectBuilder {

		private StringBuilder builder = new StringBuilder();

		private boolean hasAtLeastOneField = false;

		public JsonObjectBuilder() {
			builder.append("{");
		}

		/**
		 * Appends a null field to the JSON.
		 *
		 * @param key The key of the field.
		 * @return A reference to this object.
		 */
		public JsonObjectBuilder appendNull(String key) {
			appendFieldUnescaped(key, "null");
			return this;
		}

		/**
		 * Appends a string field to the JSON.
		 *
		 * @param key   The key of the field.
		 * @param value The value of the field.
		 * @return A reference to this object.
		 */
		public JsonObjectBuilder appendField(String key, String value) {
			if (value == null) {
				throw new IllegalArgumentException("JSON value must not be null");
			}
			appendFieldUnescaped(key, "\"" + escape(value) + "\"");
			return this;
		}

		/**
		 * Appends an integer field to the JSON.
		 *
		 * @param key   The key of the field.
		 * @param value The value of the field.
		 * @return A reference to this object.
		 */
		public JsonObjectBuilder appendField(String key, int value) {
			appendFieldUnescaped(key, String.valueOf(value));
			return this;
		}

		/**
		 * Appends an object to the JSON.
		 *
		 * @param key    The key of the field.
		 * @param object The object.
		 * @return A reference to this object.
		 */
		public JsonObjectBuilder appendField(String key, JsonObject object) {
			if (object == null) {
				throw new IllegalArgumentException("JSON object must not be null");
			}
			appendFieldUnescaped(key, object.toString());
			return this;
		}

		/**
		 * Appends a string array to the JSON.
		 *
		 * @param key    The key of the field.
		 * @param values The string array.
		 * @return A reference to this object.
		 */
		public JsonObjectBuilder appendField(String key, String[] values) {
			if (values == null) {
				throw new IllegalArgumentException("JSON values must not be null");
			}
			String escapedValues = Arrays.stream(values).map(value -> "\"" + escape(value) + "\"")
					.collect(Collectors.joining(","));
			appendFieldUnescaped(key, "[" + escapedValues + "]");
			return this;
		}

		/**
		 * Appends an integer array to the JSON.
		 *
		 * @param key    The key of the field.
		 * @param values The integer array.
		 * @return A reference to this object.
		 */
		public JsonObjectBuilder appendField(String key, int[] values) {
			if (values == null) {
				throw new IllegalArgumentException("JSON values must not be null");
			}
			String escapedValues = Arrays.stream(values).mapToObj(String::valueOf).collect(Collectors.joining(","));
			appendFieldUnescaped(key, "[" + escapedValues + "]");
			return this;
		}

		/**
		 * Appends an object array to the JSON.
		 *
		 * @param key    The key of the field.
		 * @param values The integer array.
		 * @return A reference to this object.
		 */
		public JsonObjectBuilder appendField(String key, JsonObject[] values) {
			if (values == null) {
				throw new IllegalArgumentException("JSON values must not be null");
			}
			String escapedValues = Arrays.stream(values).map(JsonObject::toString).collect(Collectors.joining(","));
			appendFieldUnescaped(key, "[" + escapedValues + "]");
			return this;
		}

		/**
		 * Appends a field to the object.
		 *
		 * @param key          The key of the field.
		 * @param escapedValue The escaped value of the field.
		 */
		private void appendFieldUnescaped(String key, String escapedValue) {
			if (builder == null) {
				throw new IllegalStateException("JSON has already been built");
			}
			if (key == null) {
				throw new IllegalArgumentException("JSON key must not be null");
			}
			if (hasAtLeastOneField) {
				builder.append(",");
			}
			builder.append("\"").append(escape(key)).append("\":").append(escapedValue);
			hasAtLeastOneField = true;
		}

		/**
		 * Builds the JSON string and invalidates this builder.
		 *
		 * @return The built JSON string.
		 */
		public JsonObject build() {
			if (builder == null) {
				throw new IllegalStateException("JSON has already been built");
			}
			JsonObject object = new JsonObject(builder.append("}").toString());
			builder = null;
			return object;
		}

		/**
		 * Escapes the given string like stated in https://www.ietf.org/rfc/rfc4627.txt.
		 *
		 * <p>
		 * This method escapes only the necessary characters '"', '\'. and '\u0000' -
		 * '\u001F'. Compact escapes are not used (e.g., '\n' is escaped as "\u000a" and
		 * not as "\n").
		 *
		 * @param value The value to escape.
		 * @return The escaped value.
		 */
		private static String escape(String value) {
			final StringBuilder builder = new StringBuilder();
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				if (c == '"') {
					builder.append("\\\"");
				} else if (c == '\\') {
					builder.append("\\\\");
				} else if (c <= '\u000F') {
					builder.append("\\u000").append(Integer.toHexString(c));
				} else if (c <= '\u001F') {
					builder.append("\\u00").append(Integer.toHexString(c));
				} else {
					builder.append(c);
				}
			}
			return builder.toString();
		}

		/**
		 * A super simple representation of a JSON object.
		 *
		 * <p>
		 * This class only exists to make methods of the {@link JsonObjectBuilder}
		 * type-safe and not allow a raw string inputs for methods like
		 * {@link JsonObjectBuilder#appendField(String, JsonObject)}.
		 */
		public static class JsonObject {

			private final String value;

			private JsonObject(String value) {
				this.value = value;
			}

			@Override
			public String toString() {
				return value;
			}
		}
	}
}