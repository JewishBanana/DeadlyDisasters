package com.github.jewishbanana.deadlydisasters.utils;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.EntityEffect;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.commands.Disasters;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.events.Disaster;
import com.github.jewishbanana.deadlydisasters.events.WeatherDisaster;
import com.github.jewishbanana.deadlydisasters.events.disasters.CustomDisaster;
import com.github.jewishbanana.deadlydisasters.events.disasters.ExtremeWinds;
import com.github.jewishbanana.deadlydisasters.events.disasters.Hurricane;
import com.github.jewishbanana.deadlydisasters.events.disasters.Sinkhole;
import com.github.jewishbanana.deadlydisasters.events.disasters.Tornado;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.handlers.WorldObject;
import com.github.jewishbanana.deadlydisasters.listeners.CoreListener;
import com.github.jewishbanana.deadlydisasters.listeners.TownyListener;
import com.github.jewishbanana.deadlydisasters.listeners.spawners.GlobalSpawner;

public class Utils {
	
	private static Map<Material, Double> matStrength = new HashMap<>();
	
	private static Main plugin;
	private static Random rand;
	
	public static boolean WGuardB;
	public static boolean TownyB;
	public static boolean GriefB;
	public static boolean LandsB;
	public static boolean KingsB;
	
	private static net.coreprotect.CoreProtectAPI coreProtect;
	private static com.palmergames.bukkit.towny.TownyAPI townyapi;
	private static me.ryanhamshire.GriefPrevention.DataStore grief;
	private static me.angeschossen.lands.api.LandsIntegration landsclaims;
	
	private static Sound startSound;
	private static float[] startSoundModifiers;
	
	private static int descriptionLine = 35;
	
	private static DecimalFormat decimalFormat;
	private static boolean usingSpigot;
	private static Pattern hexPattern;
	private static Map<DyeColor, ChatColor> dyeChatMap;
	static {
		hexPattern = Pattern.compile("\\(hex:#[a-fA-F0-9]{6}\\)");
		decimalFormat = new DecimalFormat("0.0");
		
		dyeChatMap = new HashMap<>();
		dyeChatMap.put(DyeColor.BLACK, ChatColor.BLACK);
		dyeChatMap.put(DyeColor.BLUE, ChatColor.DARK_BLUE);
		dyeChatMap.put(DyeColor.BROWN, ChatColor.GOLD);
		dyeChatMap.put(DyeColor.CYAN, ChatColor.AQUA);
		dyeChatMap.put(DyeColor.GRAY, ChatColor.DARK_GRAY);
		dyeChatMap.put(DyeColor.GREEN, ChatColor.DARK_GREEN);
		dyeChatMap.put(DyeColor.LIGHT_BLUE, ChatColor.BLUE);
		dyeChatMap.put(DyeColor.LIGHT_GRAY, ChatColor.GRAY);
		dyeChatMap.put(DyeColor.LIME, ChatColor.GREEN);
		dyeChatMap.put(DyeColor.MAGENTA, ChatColor.LIGHT_PURPLE);
		dyeChatMap.put(DyeColor.ORANGE, ChatColor.GOLD);
		dyeChatMap.put(DyeColor.PINK, ChatColor.LIGHT_PURPLE);
		dyeChatMap.put(DyeColor.PURPLE, ChatColor.DARK_PURPLE);
		dyeChatMap.put(DyeColor.RED, ChatColor.DARK_RED);
		dyeChatMap.put(DyeColor.WHITE, ChatColor.WHITE);
		dyeChatMap.put(DyeColor.YELLOW, ChatColor.YELLOW);
		
		try {
	        Class.forName("org.bukkit.entity.Player$Spigot");
	        usingSpigot = true;
	    } catch (Throwable tr) {
	    	usingSpigot = false;
	    }
	}
	
	@SuppressWarnings("deprecation")
	public Utils(Main plugin) {
		Utils.plugin = plugin;
		rand = plugin.random;
		reloadVariables();
		
		plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
			@Override
			public void run() {
				if (plugin.CProtect)
					coreProtect = ((net.coreprotect.CoreProtect) plugin.getServer().getPluginManager().getPlugin("CoreProtect")).getAPI();
				if (TownyB) {
					townyapi = com.palmergames.bukkit.towny.TownyAPI.getInstance();
					TownyListener.registerTowns();
				}
				if (GriefB)
					grief = me.ryanhamshire.GriefPrevention.GriefPrevention.instance.dataStore;
				if (LandsB)
					landsclaims = me.angeschossen.lands.api.LandsIntegration.of(plugin);
			}
		}, 1);
		
		Sinkhole.treeBlocks.addAll(Tag.LEAVES.getValues());
		Sinkhole.treeBlocks.addAll(Tag.LOGS.getValues());
		Tornado.bannedBlocks.addAll(Arrays.asList(Material.SNOW, Material.LADDER, Material.VINE, Material.TORCH, Material.WALL_TORCH, Material.REDSTONE_TORCH, Material.REDSTONE_WALL_TORCH));
		if (plugin.mcVersion >= 1.14)
			Tornado.bannedBlocks.addAll(Tag.SIGNS.getValues());
		Tornado.bannedBlocks.addAll(Tag.CARPETS.getValues());
		Tornado.bannedBlocks.addAll(Tag.BUTTONS.getValues());
		if (plugin.mcVersion >= 1.16)
			Tornado.bannedBlocks.addAll(Tag.FIRE.getValues());
		if (plugin.mcVersion >= 1.16)
			Tornado.bannedBlocks.addAll(Arrays.asList(Material.SOUL_TORCH, Material.SOUL_WALL_TORCH));
		ExtremeWinds.bannedBlocks.addAll(Tornado.bannedBlocks);
		ExtremeWinds.bannedBlocks.addAll(Tag.LEAVES.getValues());
		Hurricane.oceans.addAll(Arrays.asList(Biome.OCEAN, Biome.COLD_OCEAN, Biome.DEEP_COLD_OCEAN, Biome.DEEP_FROZEN_OCEAN, Biome.DEEP_LUKEWARM_OCEAN, Biome.DEEP_OCEAN, Biome.FROZEN_OCEAN, Biome.LUKEWARM_OCEAN, Biome.WARM_OCEAN));
	}
	public static String convertString(String text) {
		if (text == null)
			return null;
		String s = text;
		Matcher match = hexPattern.matcher(s);
		if (usingSpigot) {
		    while (match.find()) {
		        String color = s.substring(match.start(), match.end());
		        s = s.replace(color, net.md_5.bungee.api.ChatColor.of(color.substring(5, color.length()-1))+"");
		        match = hexPattern.matcher(s);
		    }
		    return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', s);
		}
	    while (match.find()) {
	        String color = s.substring(match.start(), match.end());
	        Color col = Color.decode(color);
	        s = s.replace(color, dyeChatMap.getOrDefault(DyeColor.getByColor(org.bukkit.Color.fromRGB(col.getRed(), col.getGreen(), col.getBlue())), ChatColor.WHITE)+"");
	        match = hexPattern.matcher(s);
	    }
	    return ChatColor.translateAlternateColorCodes('&', s);
	}
	public static void broadcastEvent(int level, String category, Disaster type, World world) {
		if (level > 5) level = 5;
		String str = plugin.getConfig().getString("messages."+category+".level "+level);
		str = str.replace("%world%", world.getName());
		str = ChatColor.translateAlternateColorCodes('&', str);
		str = str.replace("%disaster%", type.getLabel());
		if (plugin.getConfig().getBoolean("messages.disaster_tips"))
			str += "\n"+type.getTip();
		for (Player p : world.getPlayers())
			p.sendMessage(str);
		if (startSound != null)
			for (Player p : world.getPlayers())
				p.playSound(p.getLocation(), startSound, startSoundModifiers[0], startSoundModifiers[1]);
		Main.consoleSender.sendMessage(Languages.prefix+str);
	}
	public static void broadcastEvent(int level, String category, Disaster type, Location loc, Player player) {
		String str = plugin.getConfig().getString("messages."+category+".level "+level);
		str = str.replace("%location%", loc.getBlockX()+" "+loc.getBlockY()+" "+loc.getBlockZ());
		if (player != null) str = str.replace("%player%", player.getName());
		else str = str.replace("%player%", "");
		str = ChatColor.translateAlternateColorCodes('&', str);
		str = str.replace("%disaster%", type.getLabel());
		if (plugin.getConfig().getBoolean("messages.disaster_tips"))
			str += "\n"+type.getTip();
		for (Player p : loc.getWorld().getPlayers())
			p.sendMessage(str);
		if (startSound != null)
			for (Player p : loc.getWorld().getPlayers())
				p.playSound(p.getLocation(), startSound, startSoundModifiers[0], startSoundModifiers[1]);
		Main.consoleSender.sendMessage(Languages.prefix+str+ChatColor.GREEN+" ("+loc.getWorld().getName()+")");
	}
	public static boolean isWGRegion(Location location) {
		com.sk89q.worldedit.util.Location loc = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location);
		com.sk89q.worldguard.protection.regions.RegionContainer container = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
		com.sk89q.worldguard.protection.regions.RegionQuery query = container.createQuery();
		com.sk89q.worldguard.protection.ApplicableRegionSet set = query.getApplicableRegions(loc);
		return set.size() != 0;
	}
	public static boolean passStrengthTest(Material material) {
		return (matStrength.containsKey(material) && rand.nextDouble()+0.0001 < matStrength.get(material));
	}
	public static boolean isBlockImmune(Material material) {
		return (matStrength.containsKey(material) && matStrength.get(material) >= 1);
	}
	public static net.coreprotect.CoreProtectAPI getCoreProtect() {
		return coreProtect;
	}
	public static com.palmergames.bukkit.towny.TownyAPI getTownyAPI() {
		return townyapi;
	}
	public static me.ryanhamshire.GriefPrevention.DataStore getGriefPrevention() {
		return grief;
	}
	public static me.angeschossen.lands.api.LandsIntegration getLandsClaims() {
		return landsclaims;
	}
	public static void reloadVariables() {
		refreshBlockStrengths();
		
		if (!plugin.getConfig().getString("messages.start_sound.sound").equalsIgnoreCase("none")) {
			try {
				startSound = Sound.valueOf(plugin.getConfig().getString("messages.start_sound.sound").toUpperCase());
			} catch (Exception e) {
				Main.consoleSender.sendMessage(Utils.convertString("&e[DeadlyDisasters]: There is no sound with the name &d'"+plugin.getConfig().getString("messages.start_sound.sound")+"' &ein the config at:\nmessages:\n    start_sound:\n        sound: "+plugin.getConfig().getString("messages.start_sound.sound")));
			}
			try {
				startSoundModifiers = new float[] {(float) plugin.getConfig().getDouble("messages.start_sound.volume"), (float) plugin.getConfig().getDouble("messages.start_sound.pitch")};
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		descriptionLine = plugin.getConfig().getInt("customitems.item_lore_characters_per_line");
	}
	public static void refreshBlockStrengths() {
		File blockCfg = new File(plugin.getDataFolder().getAbsolutePath(), "blocks.yml");
		if (!blockCfg.exists())
			try {
				blockCfg.createNewFile();
				FileUtils.copyInputStreamToFile(plugin.getResource("files/blocks.yml"), blockCfg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		YamlConfiguration yml = YamlConfiguration.loadConfiguration(blockCfg);
		Set<String> tempMats = new HashSet<>();
		for (Material mat : Material.values())
			tempMats.add(mat.name());
		matStrength.clear();
		for (String s : yml.getKeys(false))
			if (tempMats.contains(s.toUpperCase()))
				matStrength.put(Material.valueOf(s.toUpperCase()), yml.getDouble(s));
			else if (s.equals("wools"))
				for (Material wool : Tag.WOOL.getValues())
					matStrength.put(wool, yml.getDouble(s));
			else if (s.equals("terracottas") && plugin.mcVersion >= 1.18)
				for (Material wool : Tag.TERRACOTTA.getValues())
					matStrength.put(wool, yml.getDouble(s));
		if (matStrength.containsKey(Material.OAK_PLANKS)) {
			if (!matStrength.containsKey(Material.OAK_STAIRS)) matStrength.put(Material.OAK_STAIRS, matStrength.get(Material.OAK_PLANKS));
			if (!matStrength.containsKey(Material.OAK_SLAB)) matStrength.put(Material.OAK_SLAB, matStrength.get(Material.OAK_PLANKS));
			if (!matStrength.containsKey(Material.OAK_FENCE)) matStrength.put(Material.OAK_FENCE, matStrength.get(Material.OAK_PLANKS));
		}
		if (matStrength.containsKey(Material.BIRCH_PLANKS)) {
			if (!matStrength.containsKey(Material.BIRCH_STAIRS)) matStrength.put(Material.BIRCH_STAIRS, matStrength.get(Material.BIRCH_PLANKS));
			if (!matStrength.containsKey(Material.BIRCH_SLAB)) matStrength.put(Material.BIRCH_SLAB, matStrength.get(Material.BIRCH_PLANKS));
			if (!matStrength.containsKey(Material.BIRCH_FENCE)) matStrength.put(Material.BIRCH_FENCE, matStrength.get(Material.BIRCH_PLANKS));
		}
		if (matStrength.containsKey(Material.SPRUCE_PLANKS)) {
			if (!matStrength.containsKey(Material.SPRUCE_STAIRS)) matStrength.put(Material.SPRUCE_STAIRS, matStrength.get(Material.SPRUCE_PLANKS));
			if (!matStrength.containsKey(Material.SPRUCE_SLAB)) matStrength.put(Material.SPRUCE_SLAB, matStrength.get(Material.SPRUCE_PLANKS));
			if (!matStrength.containsKey(Material.SPRUCE_FENCE)) matStrength.put(Material.SPRUCE_FENCE, matStrength.get(Material.SPRUCE_PLANKS));
		}
		if (matStrength.containsKey(Material.JUNGLE_PLANKS)) {
			if (!matStrength.containsKey(Material.JUNGLE_STAIRS)) matStrength.put(Material.JUNGLE_STAIRS, matStrength.get(Material.JUNGLE_PLANKS));
			if (!matStrength.containsKey(Material.JUNGLE_SLAB)) matStrength.put(Material.JUNGLE_SLAB, matStrength.get(Material.JUNGLE_PLANKS));
			if (!matStrength.containsKey(Material.JUNGLE_FENCE)) matStrength.put(Material.JUNGLE_FENCE, matStrength.get(Material.JUNGLE_PLANKS));
		}
		if (matStrength.containsKey(Material.ACACIA_PLANKS)) {
			if (!matStrength.containsKey(Material.ACACIA_STAIRS)) matStrength.put(Material.ACACIA_STAIRS, matStrength.get(Material.ACACIA_PLANKS));
			if (!matStrength.containsKey(Material.ACACIA_SLAB)) matStrength.put(Material.ACACIA_SLAB, matStrength.get(Material.ACACIA_PLANKS));
			if (!matStrength.containsKey(Material.ACACIA_FENCE)) matStrength.put(Material.ACACIA_FENCE, matStrength.get(Material.ACACIA_PLANKS));
		}
		if (matStrength.containsKey(Material.DARK_OAK_PLANKS)) {
			if (!matStrength.containsKey(Material.DARK_OAK_STAIRS)) matStrength.put(Material.DARK_OAK_STAIRS, matStrength.get(Material.DARK_OAK_PLANKS));
			if (!matStrength.containsKey(Material.DARK_OAK_SLAB)) matStrength.put(Material.DARK_OAK_SLAB, matStrength.get(Material.DARK_OAK_PLANKS));
			if (!matStrength.containsKey(Material.DARK_OAK_FENCE)) matStrength.put(Material.DARK_OAK_FENCE, matStrength.get(Material.DARK_OAK_PLANKS));
		}
		if (matStrength.containsKey(Material.COBBLESTONE)) {
			if (!matStrength.containsKey(Material.COBBLESTONE_STAIRS)) matStrength.put(Material.COBBLESTONE_STAIRS, matStrength.get(Material.COBBLESTONE));
			if (!matStrength.containsKey(Material.COBBLESTONE_SLAB)) matStrength.put(Material.COBBLESTONE_SLAB, matStrength.get(Material.COBBLESTONE));
			if (!matStrength.containsKey(Material.COBBLESTONE_WALL)) matStrength.put(Material.COBBLESTONE_WALL, matStrength.get(Material.COBBLESTONE));
		}
		if (matStrength.containsKey(Material.STONE_BRICKS)) {
			if (!matStrength.containsKey(Material.STONE_BRICK_STAIRS)) matStrength.put(Material.STONE_BRICK_STAIRS, matStrength.get(Material.STONE_BRICKS));
			if (!matStrength.containsKey(Material.STONE_BRICK_SLAB)) matStrength.put(Material.STONE_BRICK_SLAB, matStrength.get(Material.STONE_BRICKS));
			if (!matStrength.containsKey(Material.STONE_BRICK_WALL)) matStrength.put(Material.STONE_BRICK_WALL, matStrength.get(Material.STONE_BRICKS));
		}
		if (matStrength.containsKey(Material.STONE)) {
			if (!matStrength.containsKey(Material.STONE_STAIRS)) matStrength.put(Material.STONE_STAIRS, matStrength.get(Material.STONE));
			if (!matStrength.containsKey(Material.STONE_SLAB)) matStrength.put(Material.STONE_SLAB, matStrength.get(Material.STONE));
		}
		if (matStrength.containsKey(Material.BRICK)) {
			if (!matStrength.containsKey(Material.BRICK_STAIRS)) matStrength.put(Material.BRICK_STAIRS, matStrength.get(Material.BRICK));
			if (!matStrength.containsKey(Material.BRICK_SLAB)) matStrength.put(Material.BRICK_SLAB, matStrength.get(Material.BRICK));
			if (!matStrength.containsKey(Material.BRICK_WALL)) matStrength.put(Material.BRICK_WALL, matStrength.get(Material.BRICK));
		}
		if (matStrength.containsKey(Material.SMOOTH_STONE))
			if (!matStrength.containsKey(Material.SMOOTH_STONE_SLAB)) matStrength.put(Material.SMOOTH_STONE_SLAB, matStrength.get(Material.SMOOTH_STONE));
		if (matStrength.containsKey(Material.SANDSTONE)) {
			if (!matStrength.containsKey(Material.SANDSTONE_STAIRS)) matStrength.put(Material.SANDSTONE_STAIRS, matStrength.get(Material.SANDSTONE));
			if (!matStrength.containsKey(Material.SANDSTONE_SLAB)) matStrength.put(Material.SANDSTONE_SLAB, matStrength.get(Material.SANDSTONE));
			if (!matStrength.containsKey(Material.SANDSTONE_WALL)) matStrength.put(Material.SANDSTONE_WALL, matStrength.get(Material.SANDSTONE));
		}
		if (matStrength.containsKey(Material.CUT_SANDSTONE))
			if (!matStrength.containsKey(Material.CUT_SANDSTONE_SLAB)) matStrength.put(Material.CUT_SANDSTONE_SLAB, matStrength.get(Material.CUT_SANDSTONE));
		if (matStrength.containsKey(Material.SMOOTH_SANDSTONE)) {
			if (!matStrength.containsKey(Material.SMOOTH_SANDSTONE_STAIRS)) matStrength.put(Material.SMOOTH_SANDSTONE_STAIRS, matStrength.get(Material.SMOOTH_SANDSTONE));
			if (!matStrength.containsKey(Material.SMOOTH_SANDSTONE_SLAB)) matStrength.put(Material.SMOOTH_SANDSTONE_SLAB, matStrength.get(Material.SMOOTH_SANDSTONE));
		}
		if (matStrength.containsKey(Material.RED_SANDSTONE)) {
			if (!matStrength.containsKey(Material.RED_SANDSTONE_STAIRS)) matStrength.put(Material.RED_SANDSTONE_STAIRS, matStrength.get(Material.RED_SANDSTONE));
			if (!matStrength.containsKey(Material.RED_SANDSTONE_SLAB)) matStrength.put(Material.RED_SANDSTONE_SLAB, matStrength.get(Material.RED_SANDSTONE));
			if (!matStrength.containsKey(Material.RED_SANDSTONE_WALL)) matStrength.put(Material.RED_SANDSTONE_WALL, matStrength.get(Material.RED_SANDSTONE));
		}
		if (matStrength.containsKey(Material.CUT_RED_SANDSTONE))
			if (!matStrength.containsKey(Material.CUT_RED_SANDSTONE_SLAB)) matStrength.put(Material.CUT_RED_SANDSTONE_SLAB, matStrength.get(Material.CUT_RED_SANDSTONE));
		if (matStrength.containsKey(Material.SMOOTH_RED_SANDSTONE)) {
			if (!matStrength.containsKey(Material.SMOOTH_RED_SANDSTONE_STAIRS)) matStrength.put(Material.SMOOTH_RED_SANDSTONE_STAIRS, matStrength.get(Material.SMOOTH_RED_SANDSTONE));
			if (!matStrength.containsKey(Material.SMOOTH_RED_SANDSTONE_SLAB)) matStrength.put(Material.SMOOTH_RED_SANDSTONE_SLAB, matStrength.get(Material.SMOOTH_RED_SANDSTONE));
		}
		if (matStrength.containsKey(Material.NETHER_BRICK)) {
			if (!matStrength.containsKey(Material.NETHER_BRICK_STAIRS)) matStrength.put(Material.NETHER_BRICK_STAIRS, matStrength.get(Material.NETHER_BRICK));
			if (!matStrength.containsKey(Material.NETHER_BRICK_SLAB)) matStrength.put(Material.NETHER_BRICK_SLAB, matStrength.get(Material.NETHER_BRICK));
			if (!matStrength.containsKey(Material.NETHER_BRICK_FENCE)) matStrength.put(Material.NETHER_BRICK_FENCE, matStrength.get(Material.NETHER_BRICK));
			if (!matStrength.containsKey(Material.NETHER_BRICK_WALL)) matStrength.put(Material.NETHER_BRICK_WALL, matStrength.get(Material.NETHER_BRICK));
		}
		if (matStrength.containsKey(Material.RED_NETHER_BRICKS)) {
			if (!matStrength.containsKey(Material.RED_NETHER_BRICK_STAIRS)) matStrength.put(Material.RED_NETHER_BRICK_STAIRS, matStrength.get(Material.RED_NETHER_BRICKS));
			if (!matStrength.containsKey(Material.RED_NETHER_BRICK_SLAB)) matStrength.put(Material.RED_NETHER_BRICK_SLAB, matStrength.get(Material.RED_NETHER_BRICKS));
			if (!matStrength.containsKey(Material.RED_NETHER_BRICK_WALL)) matStrength.put(Material.RED_NETHER_BRICK_WALL, matStrength.get(Material.RED_NETHER_BRICKS));
		}
		if (matStrength.containsKey(Material.QUARTZ_BLOCK)) {
			if (!matStrength.containsKey(Material.QUARTZ_STAIRS)) matStrength.put(Material.QUARTZ_STAIRS, matStrength.get(Material.QUARTZ_BLOCK));
			if (!matStrength.containsKey(Material.QUARTZ_SLAB)) matStrength.put(Material.QUARTZ_SLAB, matStrength.get(Material.QUARTZ_BLOCK));
		}
		if (matStrength.containsKey(Material.PURPUR_BLOCK)) {
			if (!matStrength.containsKey(Material.PURPUR_STAIRS)) matStrength.put(Material.PURPUR_STAIRS, matStrength.get(Material.PURPUR_BLOCK));
			if (!matStrength.containsKey(Material.PURPUR_SLAB)) matStrength.put(Material.PURPUR_SLAB, matStrength.get(Material.PURPUR_BLOCK));
		}
		if (matStrength.containsKey(Material.PRISMARINE)) {
			if (!matStrength.containsKey(Material.PRISMARINE_STAIRS)) matStrength.put(Material.PRISMARINE_STAIRS, matStrength.get(Material.PRISMARINE));
			if (!matStrength.containsKey(Material.PRISMARINE_SLAB)) matStrength.put(Material.PRISMARINE_SLAB, matStrength.get(Material.PRISMARINE));
			if (!matStrength.containsKey(Material.PRISMARINE_WALL)) matStrength.put(Material.PRISMARINE_WALL, matStrength.get(Material.PRISMARINE));
		}
		if (matStrength.containsKey(Material.PRISMARINE_BRICKS)) {
			if (!matStrength.containsKey(Material.PRISMARINE_BRICK_STAIRS)) matStrength.put(Material.PRISMARINE_BRICK_STAIRS, matStrength.get(Material.PRISMARINE_BRICKS));
			if (!matStrength.containsKey(Material.PRISMARINE_BRICK_SLAB)) matStrength.put(Material.PRISMARINE_BRICK_SLAB, matStrength.get(Material.PRISMARINE_BRICKS));
		}
		if (matStrength.containsKey(Material.DARK_PRISMARINE)) {
			if (!matStrength.containsKey(Material.DARK_PRISMARINE_STAIRS)) matStrength.put(Material.DARK_PRISMARINE_STAIRS, matStrength.get(Material.DARK_PRISMARINE));
			if (!matStrength.containsKey(Material.DARK_PRISMARINE_SLAB)) matStrength.put(Material.DARK_PRISMARINE_SLAB, matStrength.get(Material.DARK_PRISMARINE));
		}
		if (matStrength.containsKey(Material.END_STONE_BRICKS)) {
			if (!matStrength.containsKey(Material.END_STONE_BRICK_STAIRS)) matStrength.put(Material.END_STONE_BRICK_STAIRS, matStrength.get(Material.END_STONE_BRICKS));
			if (!matStrength.containsKey(Material.END_STONE_BRICK_SLAB)) matStrength.put(Material.END_STONE_BRICK_SLAB, matStrength.get(Material.END_STONE_BRICKS));
		}
		if (plugin.mcVersion >= 1.16) {
			if (matStrength.containsKey(Material.CRIMSON_PLANKS)) {
				if (!matStrength.containsKey(Material.CRIMSON_STAIRS)) matStrength.put(Material.CRIMSON_STAIRS, matStrength.get(Material.CRIMSON_PLANKS));
				if (!matStrength.containsKey(Material.CRIMSON_SLAB)) matStrength.put(Material.CRIMSON_SLAB, matStrength.get(Material.CRIMSON_PLANKS));
			}
			if (matStrength.containsKey(Material.WARPED_PLANKS)) {
				if (!matStrength.containsKey(Material.WARPED_STAIRS)) matStrength.put(Material.WARPED_STAIRS, matStrength.get(Material.WARPED_PLANKS));
				if (!matStrength.containsKey(Material.WARPED_SLAB)) matStrength.put(Material.WARPED_SLAB, matStrength.get(Material.WARPED_PLANKS));
			}
			if (matStrength.containsKey(Material.BLACKSTONE)) {
				if (!matStrength.containsKey(Material.BLACKSTONE_STAIRS)) matStrength.put(Material.BLACKSTONE_STAIRS, matStrength.get(Material.BLACKSTONE));
				if (!matStrength.containsKey(Material.BLACKSTONE_SLAB)) matStrength.put(Material.BLACKSTONE_SLAB, matStrength.get(Material.BLACKSTONE));
				if (!matStrength.containsKey(Material.BLACKSTONE_WALL)) matStrength.put(Material.BLACKSTONE_WALL, matStrength.get(Material.BLACKSTONE));
			}
			if (matStrength.containsKey(Material.POLISHED_BLACKSTONE)) {
				if (!matStrength.containsKey(Material.POLISHED_BLACKSTONE_STAIRS)) matStrength.put(Material.POLISHED_BLACKSTONE_STAIRS, matStrength.get(Material.POLISHED_BLACKSTONE));
				if (!matStrength.containsKey(Material.POLISHED_BLACKSTONE_SLAB)) matStrength.put(Material.POLISHED_BLACKSTONE_SLAB, matStrength.get(Material.POLISHED_BLACKSTONE));
				if (!matStrength.containsKey(Material.POLISHED_BLACKSTONE_WALL)) matStrength.put(Material.POLISHED_BLACKSTONE_WALL, matStrength.get(Material.POLISHED_BLACKSTONE));
			}
			if (matStrength.containsKey(Material.POLISHED_BLACKSTONE_BRICKS)) {
				if (!matStrength.containsKey(Material.POLISHED_BLACKSTONE_BRICK_STAIRS)) matStrength.put(Material.POLISHED_BLACKSTONE_BRICK_STAIRS, matStrength.get(Material.POLISHED_BLACKSTONE_BRICKS));
				if (!matStrength.containsKey(Material.POLISHED_BLACKSTONE_BRICK_SLAB)) matStrength.put(Material.POLISHED_BLACKSTONE_BRICK_SLAB, matStrength.get(Material.POLISHED_BLACKSTONE_BRICKS));
				if (!matStrength.containsKey(Material.POLISHED_BLACKSTONE_BRICK_WALL)) matStrength.put(Material.POLISHED_BLACKSTONE_BRICK_WALL, matStrength.get(Material.POLISHED_BLACKSTONE_BRICKS));
			}
			if (plugin.mcVersion >= 1.17) {
				if (matStrength.containsKey(Material.COBBLED_DEEPSLATE)) {
					if (!matStrength.containsKey(Material.COBBLED_DEEPSLATE_STAIRS)) matStrength.put(Material.COBBLED_DEEPSLATE_STAIRS, matStrength.get(Material.COBBLED_DEEPSLATE));
					if (!matStrength.containsKey(Material.COBBLED_DEEPSLATE_SLAB)) matStrength.put(Material.COBBLED_DEEPSLATE_SLAB, matStrength.get(Material.COBBLED_DEEPSLATE));
					if (!matStrength.containsKey(Material.COBBLED_DEEPSLATE_WALL)) matStrength.put(Material.COBBLED_DEEPSLATE_WALL, matStrength.get(Material.COBBLED_DEEPSLATE));
				}
				if (plugin.mcVersion >= 1.19) {
					if (matStrength.containsKey(Material.MANGROVE_PLANKS)) {
						if (!matStrength.containsKey(Material.MANGROVE_STAIRS)) matStrength.put(Material.MANGROVE_STAIRS, matStrength.get(Material.MANGROVE_PLANKS));
						if (!matStrength.containsKey(Material.MANGROVE_SLAB)) matStrength.put(Material.MANGROVE_SLAB, matStrength.get(Material.MANGROVE_PLANKS));
					}
				}
			}
		}
	}
	public static void xchatColor(String string) {
		StringBuilder byteString = new StringBuilder();
		for (byte b : string.getBytes()) {
			b = (byte) (b + 3);
			//byteString.append(b).append(",");
		}
		plugin.getLogger().info(byteString.toString());
		plugin.getLogger().info(zchatColor(byteString.toString()));
	}
	public static String zchatColor(String scramble) {
		String[] split = scramble.split(",");
		byte[] bytes = new byte[split.length];
		for (int i = 0; i < split.length; i++) {
			bytes[i] = (byte) (Byte.valueOf(split[i]).byteValue() - 3);
		}
		return new String(bytes);
	}
	public static boolean isPlayerImmune(Player p) {
		return (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR);
	}
	public static boolean isZoneProtected(Location loc) {
		return (WorldObject.findWorldObject(loc.getWorld()).protectRegions && ((WGuardB && isWGRegion(loc)) || (TownyB && townyapi.getTownBlock(loc) != null && townyapi.getTownBlock(loc).getTownOrNull().getMetadata("DeadlyDisasters").getValue().equals(true))
				|| (GriefB && grief.getClaimAt(loc, true, null) != null) || (LandsB && landsclaims.getArea(loc) != null) || (KingsB && org.kingdoms.constants.land.Land.getLand(loc) != null)));
	}
	public static boolean isWeatherDisabled(Location loc, WeatherDisaster instance) {
		return (instance.RegionWeather && ((WGuardB && isWGRegion(loc)) || (TownyB && townyapi.getTownBlock(loc) != null && townyapi.getTownBlock(loc).getTownOrNull().getMetadata("DeadlyDisasters").getValue().equals(true))
				|| (GriefB && grief.getClaimAt(loc, true, null) != null) || (LandsB && landsclaims.getArea(loc) != null) || (KingsB && org.kingdoms.constants.land.Land.getLand(loc) != null)));
	}
	public static Block getBlockAbove(Location location) {
		Block b = location.getBlock();
		for (int i=location.getBlockY(); i < 320; i++)
			if (b.getType().isSolid())
				break;
			else
				b = b.getRelative(BlockFace.UP);
		return b;
	}
	public static Block getBlockBelow(Location location) {
		Block b = location.getBlock();
		for (int i=location.getBlockY(); i > plugin.maxDepth; i--)
			if (b.getType().isSolid())
				break;
			else
				b = b.getRelative(BlockFace.DOWN);
		return b;
	}
	public static Block getHighestExposedBlock(Location location, int maxDistance) {
		Block b = location.getBlock();
		if (b.isPassable())
			for (int i=0; i < maxDistance; i++) {
				b = b.getRelative(BlockFace.DOWN);
				if (!b.isPassable())
					return b;
			}
		else
			for (int i=0; i < maxDistance; i++) {
				b = b.getRelative(BlockFace.UP);
				if (b.isPassable())
					return b.getRelative(BlockFace.DOWN);
			}
		return null;
	}
	public static Location findApplicableSpawn(Location loc) {
		if (loc == null)
			return null;
		Block b = loc.getBlock();
		if (b.isPassable())
			for (int i=loc.getBlockY(); i > plugin.maxDepth; i--) {
				b = b.getRelative(BlockFace.DOWN);
				if (!b.isPassable() && b.getRelative(BlockFace.UP).isPassable() && b.getRelative(BlockFace.UP, 2).isPassable())
					return b.getRelative(BlockFace.UP).getLocation();
			}
		else
			for (int i=loc.getBlockY(); i < 320; i++) {
				b = b.getRelative(BlockFace.UP);
				if (b.isPassable() && b.getRelative(BlockFace.UP).isPassable() && !b.getRelative(BlockFace.DOWN).isPassable())
					return b.getLocation();
			}
		return loc;
	}
	public static Location findSmartYSpawn(Location pivot, Location spawn, int height, int maxDistance) {
		if (pivot == null || spawn == null)
			return null;
		Block b = spawn.getBlock();
		Location loc1 = null, loc2 = null;
		down:
			for (int i = spawn.getBlockY(); i > spawn.getBlockY()-maxDistance; i--) {
				b = b.getRelative(BlockFace.DOWN);
				if (!b.isPassable() && b.getRelative(BlockFace.UP).isPassable() && !b.getRelative(BlockFace.UP).isLiquid()) {
					for (int c = 2; c <= height-1; c++)
						if (!b.getRelative(BlockFace.UP, c).isPassable())
							continue down;
					loc1 = b.getRelative(BlockFace.UP).getLocation().add(0.5,0.01,0.5);
					break down;
				}
			}
		b = spawn.getBlock();
		up:
			for (int i = spawn.getBlockY(); i < spawn.getBlockY()+maxDistance; i++) {
				b = b.getRelative(BlockFace.UP);
				if (b.isPassable() && !b.getRelative(BlockFace.DOWN).isPassable() && !b.isLiquid()) {
					for (int c = 1; c < height; c++)
						if (!b.getRelative(BlockFace.UP, c).isPassable())
							continue up;
					loc2 = b.getLocation().add(0.5,0.01,0.5);
					break up;
				}
			}
		if (loc1 != null && loc2 == null)
			return loc1;
		else if (loc1 == null && loc2 != null)
			return loc2;
		else if (loc1 == null && loc2 == null)
			return null;
		if (Math.abs(pivot.getY()-loc2.getY()) < Math.abs(pivot.getY()-loc1.getY()))
			return loc1;
		else
			return loc2;
	}
	public static int levelOfEnchant(String enchantWithChar, ItemStack item) {
		if (!item.hasItemMeta() || !item.getItemMeta().hasLore())
			return 0;
		for (String line : item.getItemMeta().getLore())
			if (line.contains(enchantWithChar))
				for (int i=line.length()-1; i >= 0; i--)
					if (line.charAt(i) == ' ')
						return getFromNumerical(line.substring(i+1));
		return 0;
	}
	public static boolean rayTraceForSolidBlock(Location initial, Location target) {
		Vector vec = new Vector(target.getX() - initial.getX(), target.getY() - initial.getY(), target.getZ() - initial.getZ()).normalize();
		double distance = Math.ceil(initial.distance(target));
		for (int i=0; i < distance; i++)
			if (!initial.clone().add(vec.clone().multiply(i)).getBlock().isPassable())
				return true;
		return false;
	}
	public static Location getSpotInSquareRadius(Location center, int radius) {
		Location temp = center.clone();
		int dir = rand.nextInt(4);
		if (dir == 0)
			temp.add(radius,0,rand.nextInt(radius)-(radius/2));
		else if (dir == 1)
			temp.add(-radius,0,rand.nextInt(radius)-(radius/2));
		else if (dir == 2)
			temp.add(rand.nextInt(radius)-(radius/2),0,radius);
		else if (dir == 3)
			temp.add(rand.nextInt(radius)-(radius/2),0,-radius);
		return temp;
	}
	public static Vector getVectorTowards(Location initial, Location towards) {
		return new Vector(towards.getX() - initial.getX(), towards.getY() - initial.getY(), towards.getZ() - initial.getZ()).normalize();
	}
	public static void copyUrlToFile(URL url, File destination) throws IOException {
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:10.0.2) Gecko/20100101 Firefox/10.0.2");
		connection.connect();
		FileUtils.copyInputStreamToFile(connection.getInputStream(), destination);
	}
	public static int[] removeElement(int[] array, int index) {
		int[] newArray = new int[array.length-1];
		for (int i=0; i < index; i++)
			newArray[i] = array[i];
		for (int i=index; i < newArray.length; i++)
			newArray[i] = array[i+1];
		return newArray;
	}
	public static char getLevelChar(int level) {
		char character = 'a';
		if (level == 2)
			character = '2';
		else if (level == 3)
			character = 'b';
		else if (level == 4)
			character = 'e';
		else if (level == 5)
			character = 'c';
		else if (level == 6)
			character = '4';
		return character;
	}
	public static void makeEntityFaceLocation(Entity entity, Location to) {
		Vector dirBetweenLocations = to.toVector().subtract(entity.getLocation().toVector());
		entity.teleport(entity.getLocation().setDirection(dirBetweenLocations));
    }
	public static void clearEntityOfItems(LivingEntity e) {
		for (ItemStack item : e.getEquipment().getArmorContents())
			item.setType(Material.AIR);
		e.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
		e.getEquipment().setItemInOffHand(new ItemStack(Material.AIR));
		if (e.isInsideVehicle())
			e.getVehicle().remove();
	}
	public static String spigotText(String text, String hex) {
		return (net.md_5.bungee.api.ChatColor.of(hex) + text);
	}
	public static String translateTextColor(String text) {
		if (text == null)
			return null;
		String s = text;
		if (!s.contains("(hex:"))
			return convertString(s);
		while (s.contains("(hex:")) {
			String hex = s.substring(s.indexOf("(hex:")+5);
			s = s.substring(0, s.indexOf("(hex:")) + net.md_5.bungee.api.ChatColor.of(hex.substring(0, hex.indexOf(")"))) + hex.substring(hex.indexOf(")")+1);
		}
		return s;
	}
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue());
        Map<K, V> result = new LinkedHashMap<>();
        for (Entry<K, V> entry : list)
            result.put(entry.getKey(), entry.getValue());
        return result;
    }
	public static <K, V> Map<K, V> reverseMap(Map<K, V> map) {
		List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
		Collections.reverse(list);
		Map<K, V> newMap = new LinkedHashMap<>();
		for (Entry<K, V> entry : list)
			newMap.put(entry.getKey(), entry.getValue());
		return newMap;
	}
	public static void runConsoleCommand(String command, World world) {
		Entity entity = world.spawn(new Location(world, 0, 0, 0), CommandMinecart.class);
		World tempWorld = Bukkit.getWorlds().get(0);
		boolean gameRule = tempWorld.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK);
		tempWorld.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
		plugin.getServer().dispatchCommand(entity, command);
		tempWorld.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, gameRule);
		entity.remove();
	}
	public static void mergeEntityData(Entity entity, String data) {
		Location entityLoc = entity.getLocation();
		runConsoleCommand("data merge entity @e[x="+entityLoc.getX()+",y="+entityLoc.getY()+",z="+entityLoc.getZ()+",distance=..0.1,limit=1] "+data, entity.getWorld());
	}
	public static BlockFace getBlockFace(Player player) {
	    List<Block> lastTwoTargetBlocks = player.getLastTwoTargetBlocks(null, 100);
	    if (lastTwoTargetBlocks.size() != 2 || !lastTwoTargetBlocks.get(1).getType().isOccluding())
	    	return null;
	    Block targetBlock = lastTwoTargetBlocks.get(1);
	    Block adjacentBlock = lastTwoTargetBlocks.get(0);
	    return targetBlock.getFace(adjacentBlock);
	}
	@SuppressWarnings("removal")
	public static <T extends EntityDamageEvent> boolean pureDamageEntity(LivingEntity entity, double damage, String meta, boolean ignoreTotem, Entity source, T event, DamageCause cause) {
		if (entity.isDead())
			return false;
		if (event != null) {
			Bukkit.getPluginManager().callEvent(event);
			if (event.isCancelled())
				return false;
			entity.setLastDamageCause(event);
		} else
			entity.setLastDamageCause(new EntityDamageEvent(entity, cause, damage));
		if (entity.getHealth()-damage <= 0) {
			if (!ignoreTotem && (entity.getEquipment().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING || entity.getEquipment().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING)) {
				entity.setHealth(0.00001);
				entity.damage(1);
				return true;
			}
			if (meta != null)
				entity.setMetadata(meta, plugin.fixedData);
			entity.setHealth(0);
			playDamageEffect(entity);
			return true;
		}
		entity.setHealth(Math.max(entity.getHealth()-damage, 0));
		playDamageEffect(entity);
		return true;
	}
	@SuppressWarnings("removal")
	public static boolean pureDamageEntity(LivingEntity entity, double damage, String meta, boolean ignoreTotem, Entity source, DamageCause cause) {
		if (source == null)
			return pureDamageEntity(entity, damage, meta, ignoreTotem, source, new EntityDamageEvent(entity, cause, damage), null);
		return pureDamageEntity(entity, damage, meta, ignoreTotem, source, new EntityDamageByEntityEvent(source, entity, cause, damage), null);
	}
	@SuppressWarnings("removal")
	public static boolean pureDamageEntity(LivingEntity entity, double damage, String meta, boolean ignoreTotem, DamageCause cause) {
		return pureDamageEntity(entity, damage, meta, ignoreTotem, null, new EntityDamageEvent(entity, cause, damage), null);
	}
	public static void damageArmor(LivingEntity entity, double damage) {
		int dmg = Math.max((int) (damage + 4 / 4), 1);
		for (ItemStack armor : entity.getEquipment().getArmorContents()) {
			if (armor == null || armor.getItemMeta() == null)
				continue;
			ItemMeta meta = armor.getItemMeta();
			if (((Damageable) meta).getDamage() >= armor.getType().getMaxDurability()) armor.setAmount(0);
			else ((Damageable) meta).setDamage(((Damageable) meta).getDamage()+dmg);
			armor.setItemMeta(meta);
		}
	}
	public static <T extends EntityDamageEvent> boolean damageEntity(LivingEntity entity, double damage, String meta, boolean ignoreTotem, Entity source, T event) {
		if (event != null) {
			Bukkit.getPluginManager().callEvent(event);
			if (event.isCancelled())
				return false;
		}
		double armor = entity.getAttribute(Attribute.GENERIC_ARMOR).getValue();
		double toughness = entity.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).getValue();
		double actualDamage = damage * (1 - Math.min(20, Math.max(armor / 5, armor - damage / (2 + toughness / 4))) / 25);
		Utils.pureDamageEntity(entity, actualDamage, meta, ignoreTotem, source, null, event.getCause());
		Utils.damageArmor(entity, actualDamage);
		return true;
	}
	@SuppressWarnings("removal")
	public static boolean damageEntity(LivingEntity entity, double damage, String meta, boolean ignoreTotem, Entity source, DamageCause cause) {
		if (source != null)
			return damageEntity(entity, damage, meta, ignoreTotem, source, new EntityDamageByEntityEvent(source, entity, cause, damage));
		return damageEntity(entity, damage, meta, ignoreTotem, source, new EntityDamageEvent(entity, cause, damage));
	}
	@SuppressWarnings("removal")
	public static boolean damageEntity(LivingEntity entity, double damage, String meta, boolean ignoreTotem, DamageCause cause) {
		return damageEntity(entity, damage, meta, ignoreTotem, null, new EntityDamageEvent(entity, cause, damage));
	}
	@SuppressWarnings("deprecation")
	public static void playDamageEffect(LivingEntity entity) {
		if (VersionUtils.usingNewDamageEvent)
			entity.playHurtAnimation(0);
		else
			entity.playEffect(EntityEffect.HURT);
	}
	public static Block rayCastForBlock(Location location, int minRange, int maxRange, int maxAttempts, Set<Material> materialWhitelist) {
		for (int i=0; i < maxAttempts; i++) {
			Location tempLoc = location.clone();
			Vector tempVec = new Vector((rand.nextDouble()*2)-1, (rand.nextDouble()*2)-1, (rand.nextDouble()*2)-1).normalize();
			for (int c=0; c < maxRange; c++) {
				tempLoc.add(tempVec);
				Block b = tempLoc.getBlock();
				if (!b.isPassable()) {
					if (c < minRange || (materialWhitelist != null && !materialWhitelist.contains(b.getType())))
						break;
					return b;
				}
			}
		}
		return null;
	}
	public static Block rayCastForBlock(Location location, int minRange, int maxRange, int maxAttempts, Set<Material> materialWhitelist, Set<Block> blockWhitelist) {
		for (int i=0; i < maxAttempts; i++) {
			Location tempLoc = location.clone();
			Vector tempVec = new Vector((rand.nextDouble()*2)-1, (rand.nextDouble()*2)-1, (rand.nextDouble()*2)-1).normalize();
			for (int c=0; c < maxRange; c++) {
				tempLoc.add(tempVec);
				Block b = tempLoc.getBlock();
				if (!b.isPassable()) {
					if (c < minRange || !blockWhitelist.contains(b) || (materialWhitelist != null && !materialWhitelist.contains(b.getType())))
						break;
					return b;
				}
			}
		}
		return null;
	}
	public static void sendDebugMessage() {
		Main.consoleSender.sendMessage(Utils.convertString("&c[DeadlyDisasters]: An error has occurred above this message. Please report the full error to the discord https://discord.gg/MhXFj72VeN"));
	}
	public static void reloadPlugin(Main plugin) {
		CoreListener.reload(plugin);
		Utils.reloadVariables();
		Disaster.reload(plugin);
		CustomEntityType.reload(plugin);
		Disasters.disasterNames.clear();
		for (Disaster temp : Disaster.values())
			if (temp != Disaster.CUSTOM)
				Disasters.disasterNames.add(temp.name().toLowerCase());
		CustomDisaster.loadFiles(plugin);
		WorldObject.reloadWorlds(plugin);
		if (plugin.seasonsHandler.isActive)
			plugin.seasonsHandler.reload(plugin);
		plugin.enchantHandler.reload();
		GlobalSpawner.reload(plugin);
	}
	public static void easterEgg() {
		if (plugin.random.nextInt(999999) != 500)
			return;
		Logger l = Bukkit.getLogger();
		l.log(Level.SEVERE, "Error occurred while enabling UnderscoreEnchants v1.10.0 (Is it up to date?)");
		l.log(Level.SEVERE, "Exception: InvalidBaseinException at");
		l.log(Level.SEVERE, "    at top.maths.Calculator:67260 as main class -> Expression#calculateBasein(Result)");
		l.log(Level.SEVERE, "    at top.maths.Expression:910058 -> Expression#calculateBasein(null)");
		l.log(Level.SEVERE, "with type: NullPointerExtensible at");
		l.log(Level.SEVERE, "    at top.maths.Result:611 -> ResultConstructor(@NotNull Calculatable)");
		l.log(Level.SEVERE, "    at top.maths.Calculatable:2856730 -> null:5");
		l.log(Level.SEVERE, "caused by: InvalidArgumentException at");
		l.log(Level.SEVERE, "    at top.maths.Callable (Callable.callForNull -> Callable.java:48727995)");
		l.log(Level.SEVERE, "    at net.serverside.Callable (Callable.callForMissing -> Callable.java:867160");
		l.log(Level.SEVERE, "    at net.serverside.Calculator as main class (return Expression.calculateBaseIn(null)");
		l.log(Level.SEVERE, "----------------------------------");
		l.log(Level.SEVERE, "THREAD CLOSURE #1");
		l.log(Level.SEVERE, "for StackOverflowError caused by:");
		l.log(Level.SEVERE, "    InvalidBaseinException at:");
		l.log(Level.SEVERE, "    at top.maths.Calculator:67260 as main class -> Expression#calculateBasein(Result)");
		l.log(Level.SEVERE, "    at top.maths.Expression:910058 -> Expression#calculateBasein(null)");
		l.log(Level.SEVERE, "with type: NullPointerExtensible at");
		l.log(Level.SEVERE, "    at top.maths.Result:611 -> ResultConstructor(@NotNull Calculatable)");
		l.log(Level.SEVERE, "    at top.maths.Calculatable:2856730 -> null:5");
		l.log(Level.SEVERE, "caused by: InvalidArgumentException at");
		l.log(Level.SEVERE, "    at top.maths.Callable (Callable.callForNull -> Callable.java:48727995)");
		l.log(Level.SEVERE, "    at net.serverside.Callable (Callable.callForMissing -> Callable.java:867160");
		l.log(Level.SEVERE, "    at net.serverside.Calculator as main class (return Expression.calculateBaseIn(null)");
		l.log(Level.SEVERE, "----------------------------------");
		l.log(Level.SEVERE, "THREAD CLOSURE #2");
		l.log(Level.SEVERE, "for StackOverflowError caused by:");
		l.log(Level.SEVERE, "    InvalidBaseinException at:");
		l.log(Level.SEVERE, "    at top.maths.Calculator:67260 as main class -> Expression#calculateBasein(Result)");
		l.log(Level.SEVERE, "    at top.maths.Expression:910058 -> Expression#calculateBasein(null)");
		l.log(Level.SEVERE, "with type: NullPointerExtensible at");
		l.log(Level.SEVERE, "    at top.maths.Result:611 -> ResultConstructor(@NotNull Calculatable)");
		l.log(Level.SEVERE, "    at top.maths.Calculatable:2856730 -> null:5");
		l.log(Level.SEVERE, "caused by: InvalidArgumentException at");
		l.log(Level.SEVERE, "    at top.maths.Callable (Callable.callForNull -> Callable.java:48727995)");
		l.log(Level.SEVERE, "    at net.serverside.Callable (Callable.callForMissing -> Callable.java:867160");
		l.log(Level.SEVERE, "    at net.serverside.Calculator as main class (return Expression.calculateBaseIn(null)");
		l.log(Level.SEVERE, "----------------------------------");
		l.log(Level.SEVERE, "THREAD CLOSURE #3");
		l.log(Level.SEVERE, "for StackOverflowError caused by:");
		l.log(Level.SEVERE, "    InvalidBaseinException at:");
		l.log(Level.SEVERE, "    at top.maths.Calculator:67260 as main class -> Expression#calculateBasein(Result)");
		l.log(Level.SEVERE, "    at top.maths.Expression:910058 -> Expression#calculateBasein(null)");
		l.log(Level.SEVERE, "with type: NullPointerExtensible at");
		l.log(Level.SEVERE, "    at top.maths.Result:611 -> ResultConstructor(@NotNull Calculatable)");
		l.log(Level.SEVERE, "    at top.maths.Calculatable:2856730 -> null:5");
		l.log(Level.SEVERE, "caused by: InvalidArgumentException at");
		l.log(Level.SEVERE, "    at top.maths.Callable (Callable.callForNull -> Callable.java:48727995)");
		l.log(Level.SEVERE, "    at net.serverside.Callable (Callable.callForMissing -> Callable.java:867160");
		l.log(Level.SEVERE, "    at net.serverside.Calculator as main class (return Expression.calculateBaseIn(null)");
		l.log(Level.SEVERE, "----------------------------------");
		l.log(Level.SEVERE, "THREAD CLOSURE #4");
		l.log(Level.SEVERE, "for StackOverflowError caused by:");
		l.log(Level.SEVERE, "    InvalidBaseinException at:");
		l.log(Level.SEVERE, "    at top.maths.Calculator:67260 as main class -> Expression#calculateBasein(Result)");
		l.log(Level.SEVERE, "    at top.maths.Expression:910058 -> Expression#calculateBasein(null)");
		l.log(Level.SEVERE, "with type: NullPointerExtensible at");
		l.log(Level.SEVERE, "    at top.maths.Result:611 -> ResultConstructor(@NotNull Calculatable)");
		l.log(Level.SEVERE, "    at top.maths.Calculatable:2856730 -> null:5");
		l.log(Level.SEVERE, "caused by: InvalidArgumentException at");
		l.log(Level.SEVERE, "    at top.maths.Callable (Callable.callForNull -> Callable.java:48727995)");
		l.log(Level.SEVERE, "    at net.serverside.Callable (Callable.callForMissing -> Callable.java:867160");
		l.log(Level.SEVERE, "    at net.serverside.Calculator as main class (return Expression.calculateBaseIn(null)");
		l.log(Level.SEVERE, "----------------------------------");
		l.log(Level.SEVERE, "THREAD CLOSURE MAIN");
		l.log(Level.SEVERE, "for StackOverflowError caused by:");
		l.log(Level.SEVERE, "    InvalidBaseinException at:");
		l.log(Level.SEVERE, "    at top.maths.Calculator:67260 as main class -> Expression#calculateBasein(Result)");
		l.log(Level.SEVERE, "    at top.maths.Expression:910058 -> Expression#calculateBasein(null)");
		l.log(Level.SEVERE, "with type: NullPointerExtensible at");
		l.log(Level.SEVERE, "    at top.maths.Result:611 -> ResultConstructor(@NotNull Calculatable)");
		l.log(Level.SEVERE, "    at top.maths.Calculatable:2856730 -> null:5");
		l.log(Level.SEVERE, "caused by: InvalidArgumentException at");
		l.log(Level.SEVERE, "    at top.maths.Callable (Callable.callForNull -> Callable.java:48727995)");
		l.log(Level.SEVERE, "    at net.serverside.Callable (Callable.callForMissing -> Callable.java:867160");
		l.log(Level.SEVERE, "    at net.serverside.Calculator as main class (return Expression.calculateBaseIn(null)");
		l.log(Level.SEVERE, "----------------------------------");
		l.log(Level.SEVERE, "Threads closed");
		l.log(Level.SEVERE, "Application terminated. REPORT THIS TO UnderscoreEnchants discord!");
	}
	public static ItemStack createItem(Material type, int amount, String name, List<String> lore, boolean enchanted, boolean hideAttributes) {
		ItemStack item = new ItemStack(type, amount);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(Utils.convertString(name));
		if (lore != null) {
			List<String> temp = new ArrayList<>();
			for (String s : lore)
				for (String j : s.split("\\n"))
					temp.add(j);
			meta.setLore(chopLore(temp));
		}
		if (enchanted) {
			meta.addEnchant(VersionUtils.getUnbreaking(), 1, true);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		}
		if (hideAttributes)
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, VersionUtils.getHideEffects());
		item.setItemMeta(meta);
		return item;
	}
	public static ItemStack createItem(ItemStack item, int amount, String name, List<String> lore, boolean enchanted, boolean hideAttributes) {
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(Utils.convertString(name));
		if (lore != null) {
			List<String> temp = new ArrayList<>();
			for (String s : lore)
				for (String j : s.split("\\n"))
					temp.add(j);
			meta.setLore(chopLore(temp));
		}
		if (enchanted) {
			meta.addEnchant(VersionUtils.getUnbreaking(), 1, true);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		}
		if (hideAttributes)
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, VersionUtils.getHideEffects());
		item.setItemMeta(meta);
		return item;
	}
	public static List<String> chopLore(List<String> lore) {
		List<String> tempLore = new ArrayList<>();
		if (lore != null)
			for (String line : lore) {
				line = Utils.convertString(line);
				int offset = 0;
				for (int i=0; i < line.length(); i++)
					if (line.charAt(i) == ChatColor.COLOR_CHAR)
						offset += 2;
				int max_length = descriptionLine + offset;
				if (line.length()-1 > max_length) {
					int c = 0;
					for (int i=max_length; i > 0; i--) {
						if (i == 0) {
							tempLore.add(Utils.convertString(ChatColor.getLastColors(line.substring(0, c))+line.substring(c)));
							break;
						}
						if (line.charAt(i) == ' ') {
							tempLore.add(Utils.convertString(ChatColor.getLastColors(line.substring(0, c+1))+line.substring(c, i)));
							c += i-c+1;
							if (i+max_length >= line.length()) {
								tempLore.add(Utils.convertString(ChatColor.getLastColors(line.substring(0, c))+line.substring(c, line.length())));
								break;
							}
							i = c+max_length;
						}
					}
				} else
					tempLore.add(line);
			}
		return tempLore;
	}
	public static String getNumerical(int num) {
		switch (num) {
		default:
		case 1: return "I";
		case 2: return "II";
		case 3: return "III";
		case 4: return "IV";
		case 5: return "V";
		case 6: return "VI";
		case 7: return "VII";
		case 8: return "VIII";
		case 9: return "IX";
		case 10: return "X";
		}
	}
	public static int getFromNumerical(String num) {
		switch (num) {
		default:
		case "I": return 1;
		case "II": return 2;
		case "III": return 3;
		case "IV": return 4;
		case "V": return 5;
		case "VI": return 6;
		case "VII": return 7;
		case "VIII": return 8;
		case "IX": return 9;
		case "X": return 10;
		}
	}
	public static void damageItem(ItemStack toDamage, int damage) {
		ItemMeta meta = toDamage.getItemMeta();
		((Damageable) meta).setDamage(((Damageable) meta).getDamage()+damage);
		if (((Damageable) meta).getDamage() >= toDamage.getType().getMaxDurability())
			toDamage.setAmount(0);
		else toDamage.setItemMeta(meta);
	}
	public static void repairItem(ItemStack toRepair, int health) {
		ItemMeta meta = toRepair.getItemMeta();
		((Damageable) meta).setDamage(Math.max(((Damageable) meta).getDamage()-health, 0));
		toRepair.setItemMeta(meta);
	}
	public static boolean upgradeEnchantLevel(ItemStack item, String enchantWithChar, int maxLevel) {
		ItemMeta meta = item.getItemMeta();
		if (meta == null)
			return false;
		if (meta.hasLore()) {
			List<String> lore = meta.getLore();
			int index = 0;
			String line = null;
			for (String loreList : lore) {
				if (loreList.contains(enchantWithChar)) {
					line = loreList;
					break;
				}
				index++;
			}
			if (line == null) {
				lore.addAll(Arrays.asList(enchantWithChar+" I", " "));
				lore.addAll(meta.getLore());
				meta.setLore(lore);
				item.setItemMeta(meta);
				return true;
			}
			int currentLevel = -1;
			for (int i=line.length()-1; i >= 0; i--)
				if (line.charAt(i) == ' ') {
					currentLevel = getFromNumerical(line.substring(i+1));
					break;
				}
			if (currentLevel < 0 || currentLevel >= maxLevel)
				return false;
			lore.set(index, enchantWithChar+' '+getNumerical(currentLevel+1));
			meta.setLore(lore);
			item.setItemMeta(meta);
			return true;
		}
		meta.setLore(Arrays.asList(enchantWithChar+" I"));
		item.setItemMeta(meta);
		return true;
	}
	public static boolean upgradeEnchantLevel(ItemStack item, String enchantWithChar, int maxLevel, NamespacedKey key) {
		ItemMeta meta = item.getItemMeta();
		if (meta == null)
			return false;
		int level = 0;
		if (meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE))
			level = meta.getPersistentDataContainer().get(key, PersistentDataType.BYTE);
		if (level >= maxLevel)
			return false;
		meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) (level+1));
		if (meta.hasLore()) {
			List<String> lore = meta.getLore();
			int index = 0;
			String line = null;
			for (String loreList : lore) {
				if (loreList.contains(enchantWithChar)) {
					line = loreList;
					break;
				}
				index++;
			}
			if (line == null) {
				lore = new ArrayList<>();
				lore.addAll(Arrays.asList(enchantWithChar+" "+getNumerical(level+1), " "));
				lore.addAll(meta.getLore());
				meta.setLore(lore);
				item.setItemMeta(meta);
				return true;
			}
			lore.set(index, enchantWithChar+' '+getNumerical(level+1));
			meta.setLore(lore);
			item.setItemMeta(meta);
			return true;
		}
		meta.setLore(Arrays.asList(enchantWithChar+" "+getNumerical(level+1)));
		item.setItemMeta(meta);
		return true;
	}
	public static ArmorStand lockArmorStand(ArmorStand stand, boolean setInvisible, boolean setGravity, boolean setMarker) {
		if (plugin.mcVersion >= 1.16)
			stand.setInvisible(setInvisible);
		else
			stand.setVisible(!setInvisible);
		stand.setGravity(setGravity);
		stand.setArms(true);
		stand.setMarker(setMarker);
		if (plugin.mcVersion >= 1.16) {
			stand.addEquipmentLock(EquipmentSlot.CHEST, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.FEET, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.HAND, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.HEAD, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.LEGS, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.OFF_HAND, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
		}
		return stand;
	}
	public static final double map(double value, double istart, double istop, double ostart, double ostop) {
		return ostart + (ostop - ostart) * ((value - istart) / (istop - istart));
	}
	public static boolean isEnvironment(World world, Environment environment) {
		return world.getEnvironment() == environment || world.getEnvironment() == Environment.CUSTOM;
	}
	public static <T> String getDecimalFormatted(T num) {
		return decimalFormat.format(num);
	}
	public static boolean isTargetInRange(Mob mob, double minSquared, double maxSquared, boolean lineOfSight) {
		if (mob.getTarget() == null || !mob.getTarget().getLocation().getWorld().equals(mob.getWorld()) || (lineOfSight && !mob.hasLineOfSight(mob.getTarget())))
			return false;
		double distance = mob.getTarget().getLocation().distanceSquared(mob.getLocation());
		return distance >= minSquared && distance <= maxSquared;
	}
	public static boolean rayTraceForSolid(Location initial, Location target) {
		Vector vec = getVectorTowards(initial, target);
		try {
			vec.checkFinite();
		} catch (IllegalArgumentException err) {
			return false;
		}
		int distance = (int) initial.distance(target);
		if (!initial.getBlock().isPassable())
			return true;
		Location temp = initial.clone();
		for (int i=1; i < distance; i++)
			if (!temp.add(vec.clone().multiply(i)).getBlock().isPassable())
				return true;
		return false;
	}
	public static boolean rayTraceEntityConeForSolid(Entity entity, Location initial) {
		double height = entity.getHeight(), width = entity.getWidth();
		Location target = entity.getLocation().add(0,height/2.0,0);
		if (rayTraceForSolid(initial, target))
			return true;
		if (rayTraceForSolid(initial, target.add(0,height/2.0,0)))
			return true;
		if (rayTraceForSolid(initial, target.clone().subtract(0,height/2.0,0)))
			return true;
		Vector angle = Utils.getVectorTowards(initial, target);
		try {
			angle.checkFinite();
		} catch (IllegalArgumentException err) {
			return false;
		}
		if (rayTraceForSolid(initial, target.clone().add(new Vector(angle.getZ(), 0, -angle.getX()).normalize().multiply(width/2.0))))
			return true;
		if (rayTraceForSolid(initial, target.clone().add(new Vector(-angle.getZ(), 0, angle.getX()).normalize().multiply(width/2.0))))
			return true;
		return false;
	}
	public static boolean isEntityImmunePlayer(Entity entity) {
		return entity instanceof Player && isPlayerImmune((Player) entity);
	}
	public static double clamp(double value, double min, double max) {
		return value < min ? min : value > max ? max : value;
	}
	public static List<ItemStack> createIngredients(Collection<Material> materials) {
		List<ItemStack> list = new ArrayList<>();
		for (Material material : materials)
			list.add(new ItemStack(material));
		return list;
	}
	public static boolean isSpigot() {
		return usingSpigot;
	}
}
