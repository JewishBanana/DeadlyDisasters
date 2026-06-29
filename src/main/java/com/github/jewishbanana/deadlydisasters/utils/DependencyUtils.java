package com.github.jewishbanana.deadlydisasters.utils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import com.github.jewishbanana.deadlydisasters.DeadlyDisasters;
import com.github.jewishbanana.deadlydisasters.commands.TownyDisasters;
import com.github.jewishbanana.deadlydisasters.disasters.Disaster;
import com.github.jewishbanana.deadlydisasters.disasters.DisasterRegistry;
import com.github.jewishbanana.deadlydisasters.items.BasicCoatingBook;
import com.github.jewishbanana.deadlydisasters.items.PlagueCure;
import com.github.jewishbanana.deadlydisasters.items.SplashPlagueCure;
import com.github.jewishbanana.deadlydisasters.items.enchants.BasicCoating;
import com.github.jewishbanana.deadlydisasters.listeners.LootGenerateListener;
import com.github.jewishbanana.deadlydisasters.listeners.TownyListener;

public class DependencyUtils {

	private static final boolean isSpigot;
	private static final boolean isPaper;
	
	private static boolean uif;
	private static UIFHook uifHook;
	private static UCHook ucHook;
	private static boolean ultimateContent;
	
	private static Predicate<Location> regionCheck;
	private static Predicate<Location> disasterStartCheck;
	
	private static com.palmergames.bukkit.towny.TownyAPI townyHook;
	private static net.coreprotect.CoreProtectAPI cpHook;
	private static final String cpUser = "Deadly-Disasters";
	private static SeasonsHook seasonsHook;
	
	private static final String UIFrameworkVersion = "3.1.4";
	private static final String UltimateContentVersion = "2.3.0";
	private static final String UIFrameworkLink = "https://www.spigotmc.org/resources/uiframework.110768/";
	private static final String UltimateContentLink = "https://www.spigotmc.org/resources/ultimatecontent.118256/";
	static {
		boolean spigotCheck = false;
		try {
	        Class.forName("org.bukkit.entity.Player$Spigot");
	        spigotCheck = true;
	    } catch (Throwable tr) {}
		isSpigot = spigotCheck;
		boolean paperCheck = false;
		try {
	        Class.forName("com.destroystokyo.paper.entity.ai.Goal");
	        paperCheck = true;
	    } catch (ClassNotFoundException e) {}
		isPaper = paperCheck;
	}
	
	public static void init(DeadlyDisasters plugin) {
		PluginManager pm = plugin.getServer().getPluginManager();
		if (pm.isPluginEnabled("UltimateContent")) {
			if (!isVersionOrAbove(plugin.getServer().getPluginManager().getPlugin("UltimateContent"), UltimateContentVersion))
				Utils.sendConsoleMessage(formatDependencyMessage("messages.internal.ultimatecontent_outdated", UltimateContentVersion, plugin.getServer().getPluginManager().getPlugin("UltimateContent").getDescription().getVersion(), UltimateContentLink));
			else
				ultimateContent = true;
		}
		if (pm.isPluginEnabled("UIFramework")) {
			if (!com.github.jewishbanana.uiframework.UIFramework.isVersionOrAbove(UIFrameworkVersion))
				Utils.sendConsoleMessage(formatDependencyMessage("messages.internal.uiframework_outdated", UIFrameworkVersion, plugin.getServer().getPluginManager().getPlugin("UIFramework").getDescription().getVersion(), UIFrameworkLink));
			else {
				uif = true;
				BasicCoating.register();
				uifHook = new UIFHook();
				if (ultimateContent)
					ucHook = new UCHook();
				else
					Utils.sendConsoleMessage(formatDependencyMessage("messages.internal.ultimatecontent_optional", UltimateContentLink));
				
				BasicCoatingBook.register();
				PlagueCure.register();
				SplashPlagueCure.register();
				
				new LootGenerateListener(plugin);
			}
		} else
			Utils.sendConsoleMessage(formatDependencyMessage("messages.internal.uiframework_optional", UIFrameworkLink));
		
		Predicate<Location> damageCheck = null;
		Predicate<Location> startCheck = null;
		try {
			if (pm.getPlugin("WorldGuard") != null) {
				if (DataUtils.getMainConfigBoolean("external.region_protection_plugins.world_guard")) {
					registerWorldGuardFlags(plugin);
					WorldGuardHook.initQueryCache();
					damageCheck = appendRegionPredicate(damageCheck, WorldGuardHook::isDamageProtected);
					startCheck = appendRegionPredicate(startCheck, WorldGuardHook::isDisasterStartBlocked);
					plugin.getLogger().info("Successfully hooked into World Guard");
				} else
					plugin.getLogger().info("World Guard was detected, but region protection for this plugin is disabled in the main config.yml file. World Guard regions will NOT be protected!");
			}
		} catch (Throwable e) {
			if (e instanceof Exception exception)
				Utils.sendExceptionLog(exception);
			else
				e.printStackTrace();
			Utils.sendConsoleMessage("&cAn error has occurred while trying to hook into &eWorld Guard &cregions from this plugin will NOT be protected!");
		}
		try {
			if (pm.isPluginEnabled("Towny")) {
				if (DataUtils.getMainConfigBoolean("external.region_protection_plugins.towny")) {
					townyHook = com.palmergames.bukkit.towny.TownyAPI.getInstance();
					TownyListener.registerTowns();
					new TownyListener(plugin);
					plugin.getCommand("towndisasters").setTabCompleter(new TownyDisasters(plugin));
				    Predicate<Location> predicate = loc -> townyHook.getTownBlock(loc) != null &&
						townyHook.getTownBlock(loc).getTownOrNull().getMetadata("DeadlyDisasters").getValue().equals(true);
				    damageCheck = appendRegionPredicate(damageCheck, predicate);
				    startCheck = appendRegionPredicate(startCheck, predicate);
					plugin.getLogger().info("Successfully hooked into Towny");
				} else
					plugin.getLogger().info("Towny was detected, but region protection for this plugin is disabled in the main config.yml file. Towny regions will NOT be protected!");
			}
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cAn error has occurred while trying to hook into &eTowny &cregions from this plugin will NOT be protected!");
		}
		try {
			if (pm.isPluginEnabled("GriefPrevention")) {
				if (DataUtils.getMainConfigBoolean("external.region_protection_plugins.grief_prevention")) {
					me.ryanhamshire.GriefPrevention.DataStore api = me.ryanhamshire.GriefPrevention.GriefPrevention.instance.dataStore;
					Predicate<Location> predicate = loc -> api.getClaimAt(loc, true, null) != null;
					damageCheck = appendRegionPredicate(damageCheck, predicate);
					startCheck = appendRegionPredicate(startCheck, predicate);
					plugin.getLogger().info("Successfully hooked into Grief Prevention");
				} else
					plugin.getLogger().info("Grief Prevention was detected, but region protection for this plugin is disabled in the main config.yml file. Grief Prevention regions will NOT be protected!");
			}
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cAn error has occurred while trying to hook into &eGrief Prevention &cregions from this plugin will NOT be protected!");
		}
		try {
			if (pm.isPluginEnabled("Lands")) {
				if (DataUtils.getMainConfigBoolean("external.region_protection_plugins.lands")) {
					me.angeschossen.lands.api.LandsIntegration api = me.angeschossen.lands.api.LandsIntegration.of(plugin);
				    Predicate<Location> predicate = loc -> api.getArea(loc) != null;
				    damageCheck = appendRegionPredicate(damageCheck, predicate);
				    startCheck = appendRegionPredicate(startCheck, predicate);
					plugin.getLogger().info("Successfully hooked into Lands");
				} else
					plugin.getLogger().info("Lands was detected, but region protection for this plugin is disabled in the main config.yml file. Lands regions will NOT be protected!");
			}
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cAn error has occurred while trying to hook into &eLands &cregions from this plugin will NOT be protected!");
		}
		try {
			if (pm.isPluginEnabled("Kingdoms")) {
				if (DataUtils.getMainConfigBoolean("external.region_protection_plugins.kingdoms")) {
					Predicate<Location> predicate = loc -> org.kingdoms.constants.land.Land.getLand(loc) != null;
					damageCheck = appendRegionPredicate(damageCheck, predicate);
					startCheck = appendRegionPredicate(startCheck, predicate);
					plugin.getLogger().info("Successfully hooked into Kingdoms");
				} else
					plugin.getLogger().info("Kingdoms was detected, but region protection for this plugin is disabled in the main config.yml file. Kingdoms regions will NOT be protected!");
			}
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cAn error has occurred while trying to hook into &eKingdoms &cregions from this plugin will NOT be protected!");
		}
		try {
			if (pm.isPluginEnabled("FieldZone")) {
				if (DataUtils.getMainConfigBoolean("external.region_protection_plugins.field_zone")) {
					kr.rtustudio.fieldzone.region.RegionFlag flag = kr.rtustudio.fieldzone.region.RegionFlag.create(plugin, "disasters");
					kr.rtustudio.fieldzone.FieldZoneAPI.registerFlag(flag);
					Predicate<Location> predicate = loc -> kr.rtustudio.fieldzone.FieldZoneAPI.hasFlag(loc, flag) == kr.rtustudio.fieldzone.region.FlagState.FALSE;
					damageCheck = appendRegionPredicate(damageCheck, predicate);
					startCheck = appendRegionPredicate(startCheck, predicate);
					plugin.getLogger().info("Successfully hooked into FieldZone");
				} else
					plugin.getLogger().info("FieldZone was detected, but region protection for this plugin is disabled in the main config.yml file. FieldZone regions will NOT be protected!");
			}
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cAn error has occurred while trying to hook into &eFieldZone &cregions from this plugin will NOT be protected!");
		}
		try {
			if (pm.isPluginEnabled("PlotSquared")) {
				if (DataUtils.getMainConfigBoolean("external.region_protection_plugins.plot_squared")) {
					Predicate<Location> predicate = loc -> com.plotsquared.core.plot.Plot.getPlot(com.plotsquared.bukkit.util.BukkitUtil.adapt(loc)) != null;
					damageCheck = appendRegionPredicate(damageCheck, predicate);
					startCheck = appendRegionPredicate(startCheck, predicate);
					plugin.getLogger().info("Successfully hooked into PlotSquared");
				} else
					plugin.getLogger().info("PlotSquared was detected, but region protection for this plugin is disabled in the main config.yml file. PlotSquared regions will NOT be protected!");
			}
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cAn error has occurred while trying to hook into &ePlotSquared &cregions from this plugin will NOT be protected!");
		}
		try {
			if (pm.isPluginEnabled("UltimateClans")) {
				if (DataUtils.getMainConfigBoolean("external.region_protection_plugins.ultimate_clans")) {
					me.ulrich.clans.interfaces.UClans api = (me.ulrich.clans.interfaces.UClans) Bukkit.getPluginManager().getPlugin("UltimateClans");
					Optional<me.ulrich.clans.interfaces.ClaimImplement> impl = api.getClaimAPI().getPreferentialOrFirstImplement();
					if(impl.isPresent()) {
						me.ulrich.clans.interfaces.ClaimImplement claimImpl = impl.get();
						Predicate<Location> predicate = loc -> claimImpl.hasClaimLocation(loc);
						damageCheck = appendRegionPredicate(damageCheck, predicate);
						startCheck = appendRegionPredicate(startCheck, predicate);
						plugin.getLogger().info("Successfully hooked into UltimateClans");
					} else
						plugin.getLogger().info("UltimateClans was detected, but an implementation could not be found. UltimateClans regions will NOT be protected!");
				} else
					plugin.getLogger().info("UltimateClans was detected, but region protection for this plugin is disabled in the main config.yml file. UltimateClans regions will NOT be protected!");
			}
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cAn error has occurred while trying to hook into &eUltimateClans &cregions from this plugin will NOT be protected!");
		}
		try {
			if (pm.isPluginEnabled("Factions")) {
				if (DataUtils.getMainConfigBoolean("external.region_protection_plugins.factions_uuid")) {
					dev.kitteh.factions.Factions factions = dev.kitteh.factions.Factions.factions();
					Predicate<Location> predicate = loc -> !factions.getAt(loc).isWilderness();
					damageCheck = appendRegionPredicate(damageCheck, predicate);
					startCheck = appendRegionPredicate(startCheck, predicate);
					plugin.getLogger().info("Successfully hooked into FactionsUUID");
				} else
					plugin.getLogger().info("FactionsUUID was detected, but region protection for this plugin is disabled in the main config.yml file. FactionsUUID regions will NOT be protected!");
			}
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cAn error has occurred while trying to hook into &eFactionsUUID &cregions from this plugin will NOT be protected!");
		}
		
		regionCheck = (damageCheck == null) ? loc -> false : damageCheck;
		disasterStartCheck = (startCheck == null) ? loc -> false : startCheck;
		
		try {
			Object coreProtect = getCoreProtect(plugin);
			if (coreProtect != null) {
				if (DataUtils.getMainConfigBoolean("external.core_protect_damage_logging")) {
					cpHook = (net.coreprotect.CoreProtectAPI) coreProtect;
					plugin.getLogger().info("Successfully hooked into Core Protect");
				} else
					plugin.getLogger().info("Core Protect was detected, but damage logging is disabled in the main config.yml file! Rollbacks will not be available.");
			}
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cAn error has occurred while trying to hook into &eCore Protect &cdestruction from disasters will NOT be logged to Core Protect!");
		}
	}
	public static void reload(DeadlyDisasters plugin) {
//		PluginManager pm = plugin.getServer().getPluginManager();
//		try {
//			if (pm.isPluginEnabled("RealisticSeasons")) {
//				FileConfiguration seasonsConfig = SeasonsHook.getSeasonsFile(plugin);
//				if (!DataUtils.getConfigBoolean(seasonsConfig, "seasons.yml", "general.enabled", false))
//					Utils.sendConsoleMessage("&eWARNING RealisticSeasons has been detected but &cdisabled &ein the &bseasons.yml &econfig file!");
//				else {
//					seasonsHook = new SeasonsHook(seasonsConfig);
//					plugin.getLogger().info("Successfully hooked into RealisticSeasons");
//				}
//			}
//		} catch (Exception e) {
//			Utils.sendExceptionLog(e);
//			Utils.sendConsoleMessage("&cAn error has occurred while trying to hook into &eRealistic Seasons &cseasonal disaster settings will NOT take affect!");
//		}
	}
	private static String formatDependencyMessage(String path, String link) {
		String message = DataUtils.getLanguageString(path);
		return message == null ? "" : message.replace("%link%", link);
	}
	private static String formatDependencyMessage(String path, String requiredVersion, String currentVersion, String link) {
		return formatDependencyMessage(path, link)
				.replace("%version%", requiredVersion)
				.replace("%currentversion%", currentVersion);
	}
	public static boolean isVersionOrAbove(Plugin plugin, String toCheckFor) {
		try {
			String[] current = plugin.getDescription().getVersion().split("\\.");
			String[] test = toCheckFor.split("\\.");
			for (int i = 0; i < test.length; i++) {
	            if (i >= current.length)
	                return false;
	            int currentSegment = Integer.parseInt(current[i]);
	            int testSegment = Integer.parseInt(test[i]);
	            if (currentSegment > testSegment)
	                return true;
	            else if (currentSegment < testSegment)
	                return false;
	        }
	        return true;
		} catch (NumberFormatException ex) {
			throw new NumberFormatException("The version string you supplied '"+toCheckFor+"' is not a valid version string! Format must be as follows: '1.2.3' or '1.2' or '1'!");
		}
	}
	public static boolean isSpigotServer() {
		return isSpigot;
	}
	public static boolean isPaperServer() {
		return isPaper;
	}
	public static boolean isUIFrameworkEnabled() {
		return uif;
	}
	public static boolean isUltimateContentEnabled() {
		return ultimateContent;
	}
	public static void registerWorldGuardFlags(Plugin plugin) {
		if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null)
			return;
		try {
			WorldGuardHook.registerFlags();
		} catch (Throwable e) {
			if (plugin != null)
				plugin.getLogger().warning("Failed to register WorldGuard disaster flags ("+e.getClass().getSimpleName()+": "+e.getMessage()+"). WorldGuard regions will fall back to protecting disasters by default.");
		}
	}
	public static void manipulateDrownedGoals(Drowned entity, boolean aggressive) {
		if (ultimateContent)
			com.github.jewishbanana.ultimatecontent.utils.EntityUtils.manipulateDrownedGoals(entity, aggressive);
	}
	public static boolean doesItemExist(String item) {
		return uif && com.github.jewishbanana.uiframework.items.UIItemType.getItemType(item) != null;
	}
	public static com.github.jewishbanana.uiframework.items.UIItemType getItemType(String item) {
		return com.github.jewishbanana.uiframework.items.UIItemType.getItemType(item);
	}
	/**
	 * Resolves a UIFramework item id (e.g. an achievement reward such as {@code uc:voids_edge} or {@code dd:void_tear}) to an
	 * ItemStack. Returns null when UIFramework is not installed or the id is not a registered item, so callers can fall back.
	 */
	public static ItemStack resolveUIItem(String id) {
		if (!uif)
			return null;
		com.github.jewishbanana.uiframework.items.UIItemType type = com.github.jewishbanana.uiframework.items.UIItemType.getItemType(id);
		return type != null ? type.getItem() : null;
	}
	public static int getBasicCoatingLevel(ItemStack item) {
		return uifHook == null ? 0 : uifHook.basicCoating.getEnchantLevel(item);
	}
	public static int getYetisBlessingLevel(ItemStack item) {
		return ucHook == null ? 0 : ucHook.yetisblessing.getEnchantLevel(item);
	}
	private static Predicate<Location> appendRegionPredicate(Predicate<Location> current, Predicate<Location> addition) {
		return current == null ? addition : current.or(addition);
	}
	public static boolean isRegionProtected(Location location) {
		return regionCheck != null && regionCheck.test(location);
	}
	public static boolean isDisasterStartBlocked(Location location) {
		return disasterStartCheck != null && disasterStartCheck.test(location);
	}
	private static final class WorldGuardHook {
		private static ThreadLocal<com.sk89q.worldguard.protection.regions.RegionQuery> query;
		private static com.sk89q.worldguard.protection.flags.StateFlag allowDisastersFlag;
		private static com.sk89q.worldguard.protection.flags.StateFlag allowDisasterDamageFlag;

		private static void registerFlags() {
			com.sk89q.worldguard.protection.flags.registry.FlagRegistry registry = com.sk89q.worldguard.WorldGuard.getInstance().getFlagRegistry();
			allowDisastersFlag = registerStateFlag(registry, "allow-disasters");
			allowDisasterDamageFlag = registerStateFlag(registry, "allow-disaster-damage");
		}
		private static com.sk89q.worldguard.protection.flags.StateFlag registerStateFlag(com.sk89q.worldguard.protection.flags.registry.FlagRegistry registry, String name) {
			try {
				com.sk89q.worldguard.protection.flags.StateFlag flag = new com.sk89q.worldguard.protection.flags.StateFlag(name, false);
				registry.register(flag);
				return flag;
			} catch (com.sk89q.worldguard.protection.flags.registry.FlagConflictException e) {
				com.sk89q.worldguard.protection.flags.Flag<?> existing = registry.get(name);
				return (existing instanceof com.sk89q.worldguard.protection.flags.StateFlag flag) ? flag : null;
			}
		}
		private static void initQueryCache() {
			query = ThreadLocal.withInitial(() -> com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery());
		}
		private static com.sk89q.worldguard.protection.ApplicableRegionSet getRegions(Location location) {
			if (location == null || location.getWorld() == null)
				return null;
			if (query == null)
				initQueryCache();
			return query.get().getApplicableRegions(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location));
		}
		private static boolean isDamageProtected(Location location) {
			com.sk89q.worldguard.protection.ApplicableRegionSet set = getRegions(location);
			if (set == null)
				return false;
			if (allowDisasterDamageFlag == null)
				return true;
			com.sk89q.worldguard.protection.flags.StateFlag.State state = set.queryState(null, allowDisasterDamageFlag);
			return state == com.sk89q.worldguard.protection.flags.StateFlag.State.DENY || (state == null && set.size() != 0);
		}
		private static boolean isDisasterStartBlocked(Location location) {
			com.sk89q.worldguard.protection.ApplicableRegionSet set = getRegions(location);
			if (set == null)
				return false;
			if (allowDisastersFlag == null)
				return true;
			com.sk89q.worldguard.protection.flags.StateFlag.State state = set.queryState(null, allowDisastersFlag);
			return state == com.sk89q.worldguard.protection.flags.StateFlag.State.DENY || (state == null && set.size() != 0);
		}
	}
	public static com.palmergames.bukkit.towny.TownyAPI getTownyAPI() {
		return townyHook;
	}
	public static boolean isCoreProtectEnabled() {
		return cpHook != null;
	}
	public static void logCoreProtectRemoval(BlockState state) {
		if (cpHook == null)
			return;
		cpHook.logRemoval(cpUser, state);
	}
	public static void logCoreProtectPlacement(BlockState state) {
		if (cpHook == null)
			return;
		cpHook.logPlacement(cpUser, state);
	}
	public static void logCoreProtectPlacement(Block block, Material material, BlockData data) {
		if (cpHook == null)
			return;
		cpHook.logPlacement(cpUser, block.getLocation(), material, data);
	}
	public static boolean isEntityProtected(Entity entity) {
		return isRegionProtected(entity.getLocation());
	}
	private static net.coreprotect.CoreProtectAPI getCoreProtect(DeadlyDisasters instance) {
		Plugin plugin = instance.getServer().getPluginManager().getPlugin("CoreProtect");
		if (plugin == null || !plugin.isEnabled() || !(plugin instanceof net.coreprotect.CoreProtect))
			return null;
		net.coreprotect.CoreProtectAPI CoreProtect = ((net.coreprotect.CoreProtect) plugin).getAPI();
		if (CoreProtect.isEnabled() == false)
			return null;
		if (CoreProtect.APIVersion() < 6)
			return null;
		return CoreProtect;
	}
	public static boolean isRealisticSeasonsEnabled() {
		return seasonsHook != null;
	}
	public static Object getRealisticSeasonsRawAPI() {
		return seasonsHook == null ? null : seasonsHook.seasonsAPI;
	}
	public static boolean isDisasterInSeason(Class<? extends Disaster> disasterClass, World world) {
		Set<String> allowed = seasonsHook.seasonMap.get(disasterClass);
		if (allowed == null || allowed.isEmpty())
			return false;
		try {
			Method method = seasonsHook.seasonsAPI.getClass().getMethod("getSeason", World.class);
			Object season = method.invoke(seasonsHook.seasonsAPI, world);
			return allowed.contains(((Enum<?>) season).toString());
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
		}
		return true;
	}
	public static boolean isTemperatureUnderOrAtBlizzardThreshold(Player player) {
		try {
			return seasonsHook.getPlayerTemperature(player) <= seasonsHook.blizzardTemperatureThreshold;
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
		}
		return true;
	}
	public static boolean isTemperatureUnderOrAtBlizzardThreshold(Location location) {
		try {
			return seasonsHook.getLocationAirTemperature(location) <= seasonsHook.blizzardTemperatureThreshold;
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
		}
		return true;
	}
	
	private static class UIFHook {
		private com.github.jewishbanana.uiframework.items.UIEnchantment basicCoating;
		
		public UIFHook() {
			this.basicCoating = com.github.jewishbanana.uiframework.items.UIEnchantment.getEnchant(BasicCoating.REGISTERED_KEY);
		}
	}
	private static class UCHook {
		private com.github.jewishbanana.uiframework.items.UIEnchantment yetisblessing;
		
		public UCHook() {
			this.yetisblessing = com.github.jewishbanana.uiframework.items.UIEnchantment.getEnchant("ui:yetis_blessing");
		}
	}
	private static class SeasonsHook {
		private final Object seasonsAPI;
		private final Map<Class<? extends Disaster>, Set<String>> seasonMap = new HashMap<>();
		private final int blizzardTemperatureThreshold;
		private final Method getPlayerTemperatureMethod;
		private final Method getAirTemperatureMethod;
		
		@SuppressWarnings("unused")
		public SeasonsHook(FileConfiguration config) throws Exception {
			Class<?> apiClass = Class.forName("me.casperge.realisticseasons.api.SeasonsAPI");
            Method getInstance = apiClass.getMethod("getInstance");
            seasonsAPI = getInstance.invoke(null);
			this.blizzardTemperatureThreshold = DataUtils.getConfigInt(config, "seasons.yml", "general.blizzard_temperature_threshold");
			this.getPlayerTemperatureMethod = apiClass.getMethod("getTemperature", Player.class);
			this.getAirTemperatureMethod = apiClass.getMethod("getAirTemperature", Location.class);
			ConfigurationSection section = DataUtils.getConfigSection(config, "seasons.yml", "disasters");
			if (section != null)
				section.getKeys(false).forEach(disaster -> {
					DisasterRegistry registry = DisasterRegistry.getRegistry(disaster);
					if (registry == null) {
						Utils.sendConsoleMessage("&eWARNING there is no such disaster &d'"+disaster+"' &ein the &b'seasons.yml' &efile!");
						return;
					}
					Set<String> seasons = new HashSet<>();
					List<String> list = DataUtils.getConfigStringList(config, "seasons.yml", section.getCurrentPath()+'.'+disaster);
					if (list == null)
						return;
					for (String season : list) {
						if (season.equalsIgnoreCase("all")) {
							seasons.addAll(Arrays.asList("SPRING", "SUMMER", "FALL", "WINTER"));
							break;
						}
						boolean valid = false;
		                try {
		                    Class<?> seasonEnumClass = Class.forName("me.casperge.realisticseasons.season.Season");
		                    @SuppressWarnings({ "rawtypes", "unchecked" })
		                    Enum<?> enumConst = Enum.valueOf((Class) seasonEnumClass, season.toUpperCase());
		                    if (enumConst != null)
		                    	valid = true;
		                } catch (ClassNotFoundException cnf) {
		                    valid = true;
		                } catch (IllegalArgumentException iae) {
		                    valid = false;
		                } catch (Exception ex) {
		                    valid = false;
		                }
		                if (valid)
		                	seasons.add(season.toUpperCase());
		                else
		                	Utils.sendConsoleMessage("&cERROR there is no such season &d'"+season+"' &cin the &b'seasons.yml' &cfile at disaster seasons section &edisasters."+disaster+"&c!");
					}
					seasonMap.computeIfAbsent(registry.getRegisteredClass(), set -> new HashSet<>()).addAll(seasons);
				});
		}
		private int getPlayerTemperature(Player player) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			return (int) getPlayerTemperatureMethod.invoke(seasonsAPI, player);
		}
		private int getLocationAirTemperature(Location location) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			return (int) getAirTemperatureMethod.invoke(seasonsAPI, location);
		}
		@SuppressWarnings("unused")
		private static FileConfiguration getSeasonsFile(DeadlyDisasters plugin) throws Exception {
			File file = new File(plugin.getDataFolder().getAbsolutePath(), "seasons.yml");
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				FileUtils.copyInputStreamToFile(plugin.getResource("files/seasons.yml"), file);
			}
			return YamlConfiguration.loadConfiguration(file);
		}
	}
}
