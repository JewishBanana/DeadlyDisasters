package com.github.jewishbanana.deadlydisasters.utils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.io.FileUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.commands.TownyDisasters;
import com.github.jewishbanana.deadlydisasters.disasters.DisasterRegistry;
import com.github.jewishbanana.deadlydisasters.items.BasicCoatingBook;
import com.github.jewishbanana.deadlydisasters.items.PlagueCure;
import com.github.jewishbanana.deadlydisasters.items.SplashPlagueCure;
import com.github.jewishbanana.deadlydisasters.items.enchants.BasicCoating;
import com.github.jewishbanana.deadlydisasters.listeners.LootGenerateListener;
import com.github.jewishbanana.deadlydisasters.listeners.TownyListener;
import com.github.jewishbanana.uiframework.UIFramework;

public class DependencyUtils {

	private static boolean uif;
	private static UIFHook uifHook;
	private static UCHook ucHook;
	private static boolean ultimateContent;
	
	private static Predicate<Location> regionCheck;
	
	private static com.palmergames.bukkit.towny.TownyAPI townyHook;
	private static net.coreprotect.CoreProtectAPI cpHook;
	private static final String cpUser = "Deadly-Disasters";
	private static SeasonsHook seasonsHook;
	
	private static final String UIFrameworkVersion = "3.0.0";
	
	public static void init(Main plugin) {
		ultimateContent = plugin.getServer().getPluginManager().isPluginEnabled("UltimateContent");
		if (plugin.getServer().getPluginManager().isPluginEnabled("UIFramework")) {
			if (!UIFramework.isVersionOrAbove(UIFrameworkVersion))
				Utils.sendConsoleMessage("&cERROR Cannot hook into UIFramework because UIFramework is out of date! Please update to at least &a"+UIFrameworkVersion+" &c(Current version installed is &b"+(plugin.getServer().getPluginManager().getPlugin("UIFramework").getDescription().getVersion())+"&c). The only effect this error will have is all custom items related to DeadlyDisasters will be disabled. You can update UIFramework here:&6 https://www.spigotmc.org/resources/uiframework.110768/");
			else {
				uif = true;
				BasicCoating.register();
				uifHook = new UIFHook();
				if (ultimateContent)
					ucHook = new UCHook();
				else
					Utils.sendConsoleMessage("&aThere is an optional dependency UltimateContent, that adds some really cool custom items to DeadlyDisasters such as custom swords, custom mob drops, custom enchants, and more! Get UltimateContent here:&6 https://www.spigotmc.org/resources/ultimatecontent.118256/");
				
				BasicCoatingBook.register();
				PlagueCure.register();
				SplashPlagueCure.register();
				
				new LootGenerateListener(plugin);
			}
		} else
			Utils.sendConsoleMessage("&bThere is an optional dependency UIFramework, that adds some custom items to DeadlyDisasters such as the plague cure potion, basic coating enchant, and more! Get UIFramework here:&6 https://www.spigotmc.org/resources/uiframework.110768/");
		
		Predicate<Location> check = null;
		PluginManager pm = plugin.getServer().getPluginManager();
		try {
			if (pm.isPluginEnabled("WorldGuard")) {
				check = (check == null) ? loc -> isWGRegion(loc) : check.and(loc -> isWGRegion(loc));
				plugin.getLogger().info("Successfully hooked into World Guard");
			}
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cAn error has occurred while trying to hook into &eWorld Guard &cregions from this plugin will NOT be protected!");
		}
		try {
			if (pm.isPluginEnabled("Towny")) {
				townyHook = com.palmergames.bukkit.towny.TownyAPI.getInstance();
				TownyListener.registerTowns();
				new TownyListener(plugin);
				plugin.getCommand("towndisasters").setTabCompleter(new TownyDisasters(plugin));
			    check = (check == null) ? 
			            loc -> townyHook.getTownBlock(loc) != null &&
			            		townyHook.getTownBlock(loc).getTownOrNull().getMetadata("DeadlyDisasters").getValue().equals(true) : 
			            check.and(loc -> townyHook.getTownBlock(loc) != null &&
			            		townyHook.getTownBlock(loc).getTownOrNull().getMetadata("DeadlyDisasters").getValue().equals(true));
				plugin.getLogger().info("Successfully hooked into Towny");
			}
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cAn error has occurred while trying to hook into &eTowny &cregions from this plugin will NOT be protected!");
		}
		try {
			if (pm.isPluginEnabled("GriefPrevention")) {
				me.ryanhamshire.GriefPrevention.DataStore api = me.ryanhamshire.GriefPrevention.GriefPrevention.instance.dataStore;
				check = (check == null) ? 
			            loc -> api.getClaimAt(loc, true, null) != null : 
			            check.and(loc -> api.getClaimAt(loc, true, null) != null);
				plugin.getLogger().info("Successfully hooked into Grief Prevention");
			}
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cAn error has occurred while trying to hook into &eGrief Prevention &cregions from this plugin will NOT be protected!");
		}
		try {
			if (pm.isPluginEnabled("Lands")) {
				me.angeschossen.lands.api.LandsIntegration api = me.angeschossen.lands.api.LandsIntegration.of(plugin);
			    check = (check == null) ? 
			            loc -> api.getArea(loc) != null : 
			            check.and(loc -> api.getArea(loc) != null);
				plugin.getLogger().info("Successfully hooked into Lands");
			}
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cAn error has occurred while trying to hook into &eLands &cregions from this plugin will NOT be protected!");
		}
		try {
			if (pm.isPluginEnabled("Kingdoms")) {
				check = (check == null) ? 
			            loc -> org.kingdoms.constants.land.Land.getLand(loc) != null : 
			            check.and(loc -> org.kingdoms.constants.land.Land.getLand(loc) != null);
				plugin.getLogger().info("Successfully hooked into Kingdoms");
			}
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cAn error has occurred while trying to hook into &eKingdoms &cregions from this plugin will NOT be protected!");
		}
		regionCheck = (check == null) ? loc -> true : check;
		
		try {
			Object coreProtect = getCoreProtect(plugin);
			if (coreProtect != null) {
				cpHook = ((net.coreprotect.CoreProtect) coreProtect).getAPI();
				plugin.getLogger().info("Successfully hooked into Core Protect");
			}
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cAn error has occurred while trying to hook into &eCore Protect &cdestruction from disasters will NOT be logged to Core Protect!");
		}
	}
	public static void reload(Main plugin) {
		PluginManager pm = plugin.getServer().getPluginManager();
		try {
			if (pm.isPluginEnabled("RealisticSeasons")) {
				FileConfiguration seasonsConfig = SeasonsHook.getSeasonsFile(plugin);
				if (!DataUtils.getConfigBoolean(seasonsConfig, "seasons.yml", "general.enabled", false))
					Utils.sendConsoleMessage("&eWARNING RealisticSeasons has been detected but &cdisabled &ein the &bseasons.yml &econfig file!");
				else {
					seasonsHook = new SeasonsHook(seasonsConfig);
					plugin.getLogger().info("Successfully hooked into RealisticSeasons");
				}
			}
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
			Utils.sendConsoleMessage("&cAn error has occurred while trying to hook into &eRealistic Seasons &cseasonal disaster settings will NOT take affect!");
		}
	}
	public static boolean isUIFrameworkEnabled() {
		return uif;
	}
	public static boolean isUltimateContentEnabled() {
		return ultimateContent;
	}
	public static boolean doesItemExist(String item) {
		return uif && com.github.jewishbanana.uiframework.items.UIItemType.getItemType(item) != null;
	}
	public static com.github.jewishbanana.uiframework.items.UIItemType getItemType(String item) {
		return com.github.jewishbanana.uiframework.items.UIItemType.getItemType(item);
	}
	public static int getBasicCoatingLevel(ItemStack item) {
		return uifHook == null ? 0 : uifHook.basicCoating.getEnchantLevel(item);
	}
	public static int getYetisBlessingLevel(ItemStack item) {
		return ucHook == null ? 0 : ucHook.yetisblessing.getEnchantLevel(item);
	}
	private static boolean isWGRegion(Location location) {
		com.sk89q.worldedit.util.Location loc = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location);
		com.sk89q.worldguard.protection.regions.RegionContainer container = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
		com.sk89q.worldguard.protection.regions.RegionQuery query = container.createQuery();
		com.sk89q.worldguard.protection.ApplicableRegionSet set = query.getApplicableRegions(loc);
		return set.size() != 0;
	}
	public static boolean isRegionProtected(Location location) {
		return regionCheck.test(location);
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
	private static net.coreprotect.CoreProtectAPI getCoreProtect(Main instance) {
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
	public static boolean isDisasterInSeason(DisasterRegistry registry, World world) {
		Set<String> allowed = seasonsHook.seasonMap.get(registry);
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
		private final Map<DisasterRegistry, Set<String>> seasonMap = new HashMap<>();
		private final int blizzardTemperatureThreshold;
		private final Method getPlayerTemperatureMethod;
		private final Method getAirTemperatureMethod;
		
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
					seasonMap.computeIfAbsent(registry, set -> new HashSet<>()).addAll(seasons);
				});
		}
		private int getPlayerTemperature(Player player) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			return (int) getPlayerTemperatureMethod.invoke(seasonsAPI, player);
		}
		private int getLocationAirTemperature(Location location) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			return (int) getAirTemperatureMethod.invoke(seasonsAPI, location);
		}
		private static FileConfiguration getSeasonsFile(Main plugin) throws Exception {
			File file = new File(plugin.getDataFolder().getAbsolutePath(), "seasons.yml");
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				FileUtils.copyInputStreamToFile(plugin.getResource("files/seasons.yml"), file);
			}
			return YamlConfiguration.loadConfiguration(file);
		}
	}
}
