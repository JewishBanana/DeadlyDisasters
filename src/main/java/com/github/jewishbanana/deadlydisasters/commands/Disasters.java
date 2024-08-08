package com.github.jewishbanana.deadlydisasters.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Goat;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.EntityHandler;
import com.github.jewishbanana.deadlydisasters.entities.christmasentities.Elf;
import com.github.jewishbanana.deadlydisasters.entities.christmasentities.Frosty;
import com.github.jewishbanana.deadlydisasters.entities.christmasentities.Grinch;
import com.github.jewishbanana.deadlydisasters.entities.christmasentities.Santa;
import com.github.jewishbanana.deadlydisasters.entities.easterentities.EasterBunny;
import com.github.jewishbanana.deadlydisasters.entities.easterentities.KillerChicken;
import com.github.jewishbanana.deadlydisasters.entities.easterentities.RampagingGoat;
import com.github.jewishbanana.deadlydisasters.entities.endstormentities.BabyEndTotem;
import com.github.jewishbanana.deadlydisasters.entities.endstormentities.EndTotem;
import com.github.jewishbanana.deadlydisasters.entities.endstormentities.EndWorm;
import com.github.jewishbanana.deadlydisasters.entities.endstormentities.VoidArcher;
import com.github.jewishbanana.deadlydisasters.entities.endstormentities.VoidGuardian;
import com.github.jewishbanana.deadlydisasters.entities.endstormentities.VoidStalker;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.Ghoul;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.Psyco;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.PumpkinKing;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.Scarecrow;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.Vampire;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.DarkMage;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.PrimedCreeper;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.ShadowLeech;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.SkeletonKnight;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.SwampBeast;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.TunnellerZombie;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.ZombieKnight;
import com.github.jewishbanana.deadlydisasters.entities.sandstormentities.AncientMummy;
import com.github.jewishbanana.deadlydisasters.entities.sandstormentities.AncientSkeleton;
import com.github.jewishbanana.deadlydisasters.entities.solarstormentities.FirePhantom;
import com.github.jewishbanana.deadlydisasters.entities.soulstormentities.LostSoul;
import com.github.jewishbanana.deadlydisasters.entities.soulstormentities.SoulReaper;
import com.github.jewishbanana.deadlydisasters.events.Disaster;
import com.github.jewishbanana.deadlydisasters.events.DisasterEvent;
import com.github.jewishbanana.deadlydisasters.events.disasters.AcidStorm;
import com.github.jewishbanana.deadlydisasters.events.disasters.BlackPlague;
import com.github.jewishbanana.deadlydisasters.events.disasters.Blizzard;
import com.github.jewishbanana.deadlydisasters.events.disasters.CaveIn;
import com.github.jewishbanana.deadlydisasters.events.disasters.CustomDisaster;
import com.github.jewishbanana.deadlydisasters.events.disasters.Earthquake;
import com.github.jewishbanana.deadlydisasters.events.disasters.EndStorm;
import com.github.jewishbanana.deadlydisasters.events.disasters.ExtremeWinds;
import com.github.jewishbanana.deadlydisasters.events.disasters.Geyser;
import com.github.jewishbanana.deadlydisasters.events.disasters.Hurricane;
import com.github.jewishbanana.deadlydisasters.events.disasters.MeteorShower;
import com.github.jewishbanana.deadlydisasters.events.disasters.Purge;
import com.github.jewishbanana.deadlydisasters.events.disasters.SandStorm;
import com.github.jewishbanana.deadlydisasters.events.disasters.Sinkhole;
import com.github.jewishbanana.deadlydisasters.events.disasters.SolarStorm;
import com.github.jewishbanana.deadlydisasters.events.disasters.SoulStorm;
import com.github.jewishbanana.deadlydisasters.events.disasters.Supernova;
import com.github.jewishbanana.deadlydisasters.events.disasters.Tornado;
import com.github.jewishbanana.deadlydisasters.events.disasters.Tsunami;
import com.github.jewishbanana.deadlydisasters.handlers.Catalog;
import com.github.jewishbanana.deadlydisasters.handlers.DifficultyLevel;
import com.github.jewishbanana.deadlydisasters.handlers.ItemsHandler;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.handlers.TimerCheck;
import com.github.jewishbanana.deadlydisasters.handlers.WorldObject;
import com.github.jewishbanana.deadlydisasters.listeners.CoreListener;
import com.github.jewishbanana.deadlydisasters.utils.Metrics;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class Disasters implements CommandExecutor,TabCompleter {
	
	private Main plugin;
	private Random rand;
	private EntityHandler handler;
	private TimerCheck tc;
	private Catalog catalog;
	
	public static List<String> disasterNames = new ArrayList<>();
	
	private String globalUsage = Utils.convertString("&cUsage: /disasters <enable|disable|start|mintimer|reload|help|summon|give|difficulty|language|catalog|whitelist|listplayer|config|favor|dislike|event>...");

	public Disasters(Main plugin, TimerCheck tc, EntityHandler handler, Random rand, Catalog catalog) {
		this.plugin = plugin;
		this.tc = tc;
		this.handler = handler;
		this.rand = rand;
		this.catalog = catalog;
		for (Disaster temp : Disaster.values())
			if (temp != Disaster.CUSTOM)
				disasterNames.add(temp.name().toLowerCase());

		plugin.getCommand("disasters").setExecutor(this);
	}
	public static void addDisaster(String name) {
		disasterNames.add(name);
	}
	public static void removeDisaster(String name) {
		disasterNames.remove(name);
	}
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(globalUsage);
			return true;
		}
		if (!(sender instanceof Player)) {
			if (args[0].equalsIgnoreCase("start") && args.length != 4) {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters start <disaster> <level> <player>"));
				return true;
			}
			if (((args[0].equalsIgnoreCase("mintimer") || args[0].equalsIgnoreCase("difficulty")) && args[1].equalsIgnoreCase("this_world"))
					|| ((args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("disable")) && args[2].equalsIgnoreCase("this_world")) || args[0].equalsIgnoreCase("catalog")) {
				sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.console_error_message")));
				return true;
			}
		}
		if (args.length > 7 || args.length < 1) {
			sender.sendMessage(globalUsage);
			return true;
		}
		if (args[0].equalsIgnoreCase("enable")) {
			if (sender instanceof Player && !(((Player) sender).hasPermission("deadlydisasters.modify"))) {
				sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.permission_error")));
				return true;
			}
			if (args.length != 3) {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters enable <disaster|randomdisasters|maxlevels|eventmsg> <world>"));
				return true;
			}
			WorldObject worldObj = null;
			if (args[2].equalsIgnoreCase("this_world"))
				if (sender instanceof Player)
					worldObj = WorldObject.findWorldObject(((Player) sender).getWorld());
				else {
					sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.console_error_message")));
					return true;
				}
			else if (!args[2].equalsIgnoreCase("all_worlds")) {
				if (Bukkit.getWorld(args[2]) == null) {
					sender.sendMessage(Utils.convertString("&cCould not find world '"+args[2]+"'"));
					return true;
				}
				worldObj = WorldObject.findWorldObject(Bukkit.getWorld(args[2]));
			}
			String upper = args[1].toUpperCase();
			if (args[1].equalsIgnoreCase("randomdisasters")) {
				if (args[2].equalsIgnoreCase("all_worlds")) {
					WorldObject.changeAllField("natural_disasters", true, plugin);
					for (Player p : Bukkit.getOnlinePlayers())
						if (p.hasPermission("deadlydisasters.difficultyNotify"))
							p.sendMessage(Utils.convertString("&3Random occurring disasters are now &a&lENABLED &3on &dALL_WORLDS"));
					return true;
				}
				worldObj.settings.replace("natural_disasters", true);
				worldObj.naturalAllowed = true;
				WorldObject.yamlFile.set(worldObj.getWorld().getName()+".general.natural_disasters", true);
				WorldObject.saveYamlFile(plugin);
				for (Player p : worldObj.getWorld().getPlayers())
					if (p.hasPermission("deadlydisasters.difficultyNotify"))
						p.sendMessage(Utils.convertString("&3Random occurring disasters are now &a&lENABLED &3on &d'"+worldObj.getWorld().getName()+"'"));
				return true;
			} else if (args[1].equalsIgnoreCase("maxlevels")) {
				if (args[2].equalsIgnoreCase("all_worlds")) {
					WorldObject.changeAllField("level_six", true, plugin);
					sender.sendMessage(Utils.convertString("&aLevel 6 disasters are now &benabled &aon &dALL_WORLDS"));
					return true;
				}
				worldObj.settings.replace("level_six", true);
				WorldObject.yamlFile.set(worldObj.getWorld().getName()+".general.level_six", true);
				WorldObject.saveYamlFile(plugin);
				sender.sendMessage(Utils.convertString("&aLevel 6 disasters are now &benabled &aon &d'"+worldObj.getWorld().getName()+"'"));
				return true;
			} else if (args[1].equalsIgnoreCase("eventmsg")) {
				if (args[2].equalsIgnoreCase("all_worlds")) {
					WorldObject.changeAllField("event_broadcast", true, plugin);
					sender.sendMessage(Utils.convertString("&aEvent broadcasts are now &benabled &aon &dALL_WORLDS"));
					return true;
				}
				worldObj.settings.replace("event_broadcast", true);
				WorldObject.yamlFile.set(worldObj.getWorld().getName()+".general.event_broadcast", true);
				WorldObject.saveYamlFile(plugin);
				sender.sendMessage(Utils.convertString("&aEvent broadcasts are now &benabled &aon &d'"+worldObj.getWorld().getName()+"'"));
				return true;
			} else if (Disaster.forName(upper) != null) {
				Disaster disaster = Disaster.valueOf(upper);
				if (args[2].equalsIgnoreCase("all_worlds")) {
					WorldObject.updateGlobalDisaster(disaster, true, plugin);
					sender.sendMessage(Utils.convertString(disaster.getLabel()+" &aare now &benabled &aon &dALL_WORLDS"));
					return true;
				}
				if (!worldObj.allowed.contains(disaster)) worldObj.allowed.add(disaster);
				WorldObject.yamlFile.set(worldObj.getWorld().getName()+".disasters."+upper, true);
				WorldObject.saveYamlFile(plugin);
				sender.sendMessage(Utils.convertString(disaster.getLabel()+" &aare now &benabled &aon &d'"+worldObj.getWorld().getName()+"'"));
				return true;
			} else {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters enable <disaster|randomdisasters|maxlevels|eventmsg> <world>"));
				return true;
			}
		} else if (args[0].equalsIgnoreCase("disable")) {
			if (sender instanceof Player && !(((Player) sender).hasPermission("deadlydisasters.modify"))) {
				sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.permission_error")));
				return true;
			}
			if (args.length != 3) {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters disable <disaster|randomdisasters|maxlevels|eventmsg> <world>"));
				return true;
			}
			WorldObject worldObj = null;
			if (args[2].equalsIgnoreCase("this_world"))
				if (sender instanceof Player)
					worldObj = WorldObject.findWorldObject(((Player) sender).getWorld());
				else {
					sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.console_error_message")));
					return true;
				}
			else if (!args[2].equalsIgnoreCase("all_worlds")) {
				if (Bukkit.getWorld(args[2]) == null) {
					sender.sendMessage(Utils.convertString("&cCould not find world '"+args[2]+"'"));
					return true;
				}
				worldObj = WorldObject.findWorldObject(Bukkit.getWorld(args[2]));
			}
			String upper = args[1].toUpperCase();
			if (args[1].equalsIgnoreCase("randomdisasters")) {
				if (args[2].equalsIgnoreCase("all_worlds")) {
					WorldObject.changeAllField("natural_disasters", false, plugin);
					for (Player p : Bukkit.getOnlinePlayers())
						if (p.hasPermission("deadlydisasters.difficultyNotify"))
							p.sendMessage(Utils.convertString("&3Random occurring disasters are now &c&lDISABLED &3on &dALL_WORLDS"));
					return true;
				}
				worldObj.settings.replace("natural_disasters", false);
				worldObj.naturalAllowed = false;
				WorldObject.yamlFile.set(worldObj.getWorld().getName()+".general.natural_disasters", false);
				WorldObject.saveYamlFile(plugin);
				for (Player p : worldObj.getWorld().getPlayers())
					if (p.hasPermission("deadlydisasters.difficultyNotify"))
						p.sendMessage(Utils.convertString("&3Random occurring disasters are now &c&lDISABLED &3on &d'"+worldObj.getWorld().getName()+"'"));
				return true;
			} else if (args[1].equalsIgnoreCase("maxlevels")) {
				if (args[2].equalsIgnoreCase("all_worlds")) {
					WorldObject.changeAllField("level_six", false, plugin);
					sender.sendMessage(Utils.convertString("&aLevel 6 disasters are now &cdisabled &aon &dALL_WORLDS"));
					return true;
				}
				worldObj.settings.replace("level_six", false);
				WorldObject.yamlFile.set(worldObj.getWorld().getName()+".general.level_six", false);
				WorldObject.saveYamlFile(plugin);
				sender.sendMessage(Utils.convertString("&aLevel 6 disasters are now &cdisabled &aon &d'"+worldObj.getWorld().getName()+"'"));
				return true;
			} else if (args[1].equalsIgnoreCase("eventmsg")) {
				if (args[2].equalsIgnoreCase("all_worlds")) {
					WorldObject.changeAllField("event_broadcast", false, plugin);
					sender.sendMessage(Utils.convertString("&aEvent broadcasts are now &cdisabled &aon &dALL_WORLDS"));
					return true;
				}
				worldObj.settings.replace("event_broadcast", false);
				WorldObject.yamlFile.set(worldObj.getWorld().getName()+".general.event_broadcast", false);
				WorldObject.saveYamlFile(plugin);
				sender.sendMessage(Utils.convertString("&aEvent broadcasts are now &cdisabled &aon &d'"+worldObj.getWorld().getName()+"'"));
				return true;
			} else if (Disaster.forName(upper) != null) {
				Disaster disaster = Disaster.valueOf(upper);
				if (args[2].equalsIgnoreCase("all_worlds")) {
					WorldObject.updateGlobalDisaster(disaster, false, plugin);
					sender.sendMessage(Utils.convertString(disaster.getLabel()+" &aare now &cdisabled &aon &dALL_WORLDS"));
					return true;
				}
				if (worldObj.allowed.contains(disaster)) worldObj.allowed.remove(disaster);
				WorldObject.yamlFile.set(worldObj.getWorld().getName()+".disasters."+upper, false);
				WorldObject.saveYamlFile(plugin);
				sender.sendMessage(Utils.convertString(disaster.getLabel()+" &aare now &cdisabled &aon &d'"+worldObj.getWorld().getName()+"'"));
				return true;
			} else {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters disable <disaster|randomdisasters|maxlevels|eventmsg> <world>"));
				return true;
			}
		} else if (args[0].equalsIgnoreCase("start")) {
			if (sender instanceof Player && !(((Player) sender).hasPermission("deadlydisasters.start"))) {
				sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.permission_error")));
				return true;
			}
			int level = 1;
			Player p = null;
			boolean broadcast = true;
			if (args.length >= 3 && args.length <= 5) {
				try {
					level = Integer.parseInt(args[2]);
					if (level <= 0 || level > 6) {
						sender.sendMessage(Utils.convertString("&cError level must be #1-6"));
						return true;
					}
				} catch (NumberFormatException e) {
					sender.sendMessage(Utils.convertString("&cError level must be a valid integer #1-6"));
					return true;
				}
				if (args.length >= 4) {
					for (Player target : Bukkit.getServer().getOnlinePlayers())
						if (target.getName().equalsIgnoreCase(args[3]))
							p = target;
					if (p == null) {
						sender.sendMessage(Utils.convertString("&c"+args[3]+" is not online!"));
						return true;
					}
				} else p = (Player) sender;
				if (args.length == 5) {
					if (args[4].equalsIgnoreCase("false"))
						broadcast = false;
					else if (!args[4].equalsIgnoreCase("true")) {
						sender.sendMessage(Utils.convertString("&cError broadcast can only be a true/false value!"));
						return true;
					}
				}
			} else if (args.length == 2) {
				level = rand.nextInt(6)+1;
				p = (Player) sender;
			} else {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters start <disaster> [level] [player] [broadcast]"));
				return true;
			}
			WorldObject temp = WorldObject.findWorldObject(p.getWorld());
			
			if (!(boolean)temp.settings.get("admin_override") && !temp.allowed.contains(Disaster.valueOf(args[1].toUpperCase()))) {
				sender.sendMessage(Utils.convertString("&cThis disaster is not allowed in the targets world!"));
				return true;
			}
			if (args[1].equalsIgnoreCase("acidstorm")) {
				if (!Utils.isEnvironment(p.getWorld(), Environment.NORMAL)) {
					sender.sendMessage(Utils.convertString("&cMust be in an overworld environment to start an acidstorm"));
					return true;
				}
				AcidStorm storm = new AcidStorm(level, p.getWorld());
				storm.start(p.getWorld(), p, broadcast);
				Metrics.incrementValue(Metrics.disasterSpawnedMap, Disaster.ACIDSTORM.getMetricsLabel());
				return true;
			} else if (args[1].equalsIgnoreCase("blizzard")) {
				if (!Utils.isEnvironment(p.getWorld(), Environment.NORMAL)) {
					sender.sendMessage(Utils.convertString("&cMust be in an overworld environment to start a blizzard"));
					return true;
				}
				Blizzard blizz = new Blizzard(level, p.getWorld());
				blizz.start(p.getWorld(), p, broadcast);
				Metrics.incrementValue(Metrics.disasterSpawnedMap, Disaster.BLIZZARD.getMetricsLabel());
				return true;
			} else if (args[1].equalsIgnoreCase("cavein")) {
				Block b = p.getLocation().add(0,2,0).getBlock();
				for (int i=0; i < 20; i++) {
					if (!b.isPassable()) {
						CaveIn cavein = new CaveIn(level, p.getWorld());
						if (broadcast)
							cavein.broadcastMessage(b.getLocation(), p);
						cavein.start(b.getLocation(), p);
						Metrics.incrementValue(Metrics.disasterSpawnedMap, Disaster.CAVEIN.getMetricsLabel());
						return true;
					}
					b = b.getRelative(BlockFace.UP);
				}
				sender.sendMessage(Utils.convertString("&cMust have a roof over target player to start a cave-in!"));
				return true;
			} else if (args[1].equalsIgnoreCase("earthquake")) {
				Location loc = p.getLocation();
				for (int c=loc.getBlockY()-1; c > 0; c--) {
					loc.setY(c);
					if (loc.getBlock().getType().isSolid()) {
						Earthquake earthquake = new Earthquake(level, p.getWorld());
						if (broadcast)
							earthquake.broadcastMessage(loc, p);
						earthquake.start(loc, p);
						Metrics.incrementValue(Metrics.disasterSpawnedMap, Disaster.EARTHQUAKE.getMetricsLabel());
						return true;
					}
				}
				sender.sendMessage(Utils.convertString("&cMust be ground below target player to start an earthquake!"));
				return true;
			} else if (args[1].equalsIgnoreCase("extremewinds")) {
				ExtremeWinds winds = new ExtremeWinds(level, p.getWorld());
				winds.start(p.getWorld(), p, broadcast);
				Metrics.incrementValue(Metrics.disasterSpawnedMap, Disaster.EXTREMEWINDS.getMetricsLabel());
				return true;
			} else if (args[1].equalsIgnoreCase("geyser")) {
				if (p.getWorld().getEnvironment() == Environment.THE_END) {
					sender.sendMessage(Utils.convertString("&cMust be in an overworld or nether environment to start a geyser"));
					return true;
				}
				Geyser geyser = new Geyser(level, p.getWorld());
				if (broadcast)
					geyser.broadcastMessage(p.getLocation(), p);
				geyser.start(p.getLocation(), p);
				Metrics.incrementValue(Metrics.disasterSpawnedMap, Disaster.GEYSER.getMetricsLabel());
				return true;
			} else if (args[1].equalsIgnoreCase("sinkhole")) {
				Location loc = p.getLocation().clone();
				for (int c=loc.getBlockY()-1; c > 0; c--) {
					loc.setY(c);
					if (loc.getBlock().getType().isSolid()) {
						Sinkhole s = new Sinkhole(level, p.getWorld());
						if (broadcast)
							s.broadcastMessage(loc, p);
						s.start(loc, p);
						Metrics.incrementValue(Metrics.disasterSpawnedMap, Disaster.SINKHOLE.getMetricsLabel());
						return true;
					}
				}
				sender.sendMessage(Utils.convertString("&cMust be ground below target player to start a sinkhole!"));
				return true;
			} else if (args[1].equalsIgnoreCase("soulstorm")) {
				if (!Utils.isEnvironment(p.getWorld(), Environment.NETHER)) {
					sender.sendMessage(Utils.convertString("&cMust be in a nether environment to start a soulstorm"));
					return true;
				}
				SoulStorm storm = new SoulStorm(level, p.getWorld());
				storm.start(p.getWorld(), p, broadcast);
				Metrics.incrementValue(Metrics.disasterSpawnedMap, Disaster.SOULSTORM.getMetricsLabel());
				return true;
			} else if (args[1].equalsIgnoreCase("tornado")) {
				Location loc = p.getLocation().clone();
				for (int i=loc.getBlockY()-1; i > 0; i--) {
					loc.setY(i);
					if (loc.getBlock().getType().isSolid()) {
						Tornado tornado = new Tornado(level, p.getWorld());
						if (broadcast)
							tornado.broadcastMessage(loc.clone().add(0,1,0), p);
						tornado.start(loc.clone().add(0,1,0), p);
						Metrics.incrementValue(Metrics.disasterSpawnedMap, Disaster.TORNADO.getMetricsLabel());
						return true;
					}
				}
				sender.sendMessage(Utils.convertString("&cMust be ground below target player to start a tornado!"));
				return true;
			} else if (args[1].equalsIgnoreCase("sandstorm")) {
				if (!Utils.isEnvironment(p.getWorld(), Environment.NORMAL)) {
					sender.sendMessage(Utils.convertString("&cMust be in an overworld environment to start a sandstorm"));
					return true;
				}
				SandStorm storm = new SandStorm(level, p.getWorld());
				storm.start(p.getWorld(), p, broadcast);
				Metrics.incrementValue(Metrics.disasterSpawnedMap, Disaster.SANDSTORM.getMetricsLabel());
				return true;
			} else if (args[1].equalsIgnoreCase("plague")) {
				BlackPlague plague = new BlackPlague(level, p.getWorld());
				if (plague.isMobAvailable(p.getWorld())) {
					plague.start(p.getWorld(), p, broadcast);
					Metrics.incrementValue(Metrics.disasterSpawnedMap, Disaster.PLAGUE.getMetricsLabel());
					return true;
				}
				sender.sendMessage(Utils.convertString("&cCould not find available mob nearby!"));
				return true;
			} else if (args[1].equalsIgnoreCase("tsunami")) {
				Tsunami tsu = new Tsunami(level, p.getWorld());
				Location test = tsu.findAvailabePool(p.getLocation());
				if (test != null) {
					if (broadcast)
						tsu.broadcastMessage(test, p);
					tsu.start(test, p);
					Metrics.incrementValue(Metrics.disasterSpawnedMap, Disaster.TSUNAMI.getMetricsLabel());
					return true;
				}
				sender.sendMessage(Utils.convertString("&cCould not find pool nearby!"));
				return true;
			} else if (args[1].equalsIgnoreCase("meteorshowers")) {
				if (!Utils.isEnvironment(p.getWorld(), Environment.NORMAL)) {
					sender.sendMessage(Utils.convertString("&cMust be in an overworld environment to start a meteor shower"));
					return true;
				}
				MeteorShower shower = new MeteorShower(level, p.getWorld());
				shower.start(p.getWorld(), p, broadcast);
				Metrics.incrementValue(Metrics.disasterSpawnedMap, Disaster.METEORSHOWERS.getMetricsLabel());
				return true;
			} else if (args[1].equalsIgnoreCase("endstorm")) {
				if (plugin.mcVersion < 1.16) {
					sender.sendMessage(Utils.convertString("&cThis disaster is only available on version 1.16 or higher!"));
					return true;
				}
				if (!Utils.isEnvironment(p.getWorld(), Environment.THE_END)) {
					sender.sendMessage(Utils.convertString("&cMust be in an end environment to start an endstorm"));
					return true;
				}
				EndStorm storm = new EndStorm(level, p.getWorld());
				storm.start(p.getWorld(), p, broadcast);
				Metrics.incrementValue(Metrics.disasterSpawnedMap, Disaster.ENDSTORM.getMetricsLabel());
				return true;
			} else if (args[1].equalsIgnoreCase("supernova")) {
				Supernova nova = new Supernova(level, p.getWorld());
				if (broadcast)
					nova.broadcastMessage(p.getLocation().clone().add(0,10,0), p);
				nova.start(p.getLocation().clone().add(0,10,0), p);
				Metrics.incrementValue(Metrics.disasterSpawnedMap, Disaster.SUPERNOVA.getMetricsLabel());
				return true;
			} else if (args[1].equalsIgnoreCase("hurricane")) {
				Hurricane storm = new Hurricane(level, p.getWorld());
				if (broadcast)
					storm.broadcastMessage(p.getLocation().clone().add(0,7,0), p);
				storm.start(p.getLocation().clone().add(0,7,0), p);
				Metrics.incrementValue(Metrics.disasterSpawnedMap, Disaster.HURRICANE.getMetricsLabel());
				return true;
			} else if (args[1].equalsIgnoreCase("purge")) {
				if (Purge.targetedPlayers.contains(p.getUniqueId())) {
					sender.sendMessage(Utils.convertString("&cCannot start a purge because player is already being targeted"));
					return true;
				}
				Purge purge = new Purge(level, p.getWorld());
				if (broadcast)
					purge.broadcastMessage(p.getLocation(), p);
				purge.start(p.getLocation(), p);
				Metrics.incrementValue(Metrics.disasterSpawnedMap, Disaster.PURGE.getMetricsLabel());
				return true;
			} else if (args[1].equalsIgnoreCase("solarstorm")) {
				if (!Utils.isEnvironment(p.getWorld(), Environment.NORMAL)) {
					sender.sendMessage(Utils.convertString("&cMust be in an overworld environment to start a solar storm"));
					return true;
				}
				SolarStorm storm = new SolarStorm(level, p.getWorld());
				storm.start(p.getWorld(), p, broadcast);
				Metrics.incrementValue(Metrics.disasterSpawnedMap, Disaster.SOLARSTORM.getMetricsLabel());
				return true;
			} else if (disasterNames.contains(args[1])) {
				YamlConfiguration yaml = CustomDisaster.disasterFiles.get(args[1]);
				if (!yaml.getStringList("settings.environments").contains(p.getWorld().getEnvironment().toString().toLowerCase())) {
					sender.sendMessage(Utils.convertString("&c"+args[1]+" cannot be started in this environment!"));
					return true;
				}
				if (yaml.getInt("settings.min_height") > p.getLocation().getBlockY()) {
					sender.sendMessage(Utils.convertString("&c"+args[1]+" can only be started above y="+yaml.getInt("settings.min_height")));
					return true;
				}
				if (!yaml.contains("core.level "+level)) {
					sender.sendMessage(Utils.convertString("&c"+args[1]+" does not have a level "+level));
					return true;
				}
				CustomDisaster custom = new CustomDisaster(level, plugin, yaml);
				if (broadcast)
					custom.broadcastMessage(p.getLocation(), p);
				custom.start(p.getLocation(), p);
				Metrics.incrementValue(Metrics.disasterSpawnedMap, Disaster.CUSTOM.getMetricsLabel());
				return true;
			}
			sender.sendMessage(Utils.convertString("&cNo disaster '"+args[1]+"' exists!"));
			return true;
		} else if (args[0].equalsIgnoreCase("mintimer")) {
			if (sender instanceof Player && !(((Player) sender).hasPermission("deadlydisasters.modify"))) {
				sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.permission_error")));
				return true;
			}
			if (args.length != 3) {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters mintimer <world> <seconds>"));
				return true;
			}
			WorldObject worldObj = null;
			if (args[1].equalsIgnoreCase("this_world"))
				if (sender instanceof Player)
					worldObj = WorldObject.findWorldObject(((Player) sender).getWorld());
				else {
					sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.console_error_message")));
					return true;
				}
			else if (!args[1].equalsIgnoreCase("all_worlds")) {
				if (Bukkit.getWorld(args[1]) == null) {
					sender.sendMessage(Utils.convertString("&cCould not find world '"+args[1]+"'"));
					return true;
				}
				worldObj = WorldObject.findWorldObject(Bukkit.getWorld(args[1]));
			}
			int time = 0;
			try {
				time = Integer.parseInt(args[2]);
			} catch (NumberFormatException e) {
				sender.sendMessage(Utils.convertString("&cValue must be a valid integer!"));
				return true;
			}
			if (args[1].equalsIgnoreCase("all_worlds")) {
				for (WorldObject wo : WorldObject.worlds) {
					wo.settings.replace("min_timer", time);
					WorldObject.yamlFile.set(wo.getWorld().getName()+".general.min_timer", time);
					if (wo.difficulty == DifficultyLevel.CUSTOM) {
						wo.timer = time;
						tc.updateTimerList(wo.getWorld());
					}
				}
				WorldObject.saveYamlFile(plugin);
				sender.sendMessage(Utils.convertString("&aChanged the minimum time to &b"+time+" &aon &dALL_WORLDS"
						+ "\n&3&oKeep in mind that a worlds disaster difficulty level must be set to &f&lCUSTOM &3&ofor these changes to take effect."));
				return true;
			}
			sender.sendMessage(Utils.convertString("&aChanged the minimum time to &b"+time+" &aon &d'"+worldObj.getWorld().getName()+"'"));
			worldObj.settings.replace("min_timer", time);
			if (worldObj.difficulty == DifficultyLevel.CUSTOM) {
				worldObj.timer = time;
				tc.updateTimerList(worldObj.getWorld());
			} else
				sender.sendMessage(Utils.convertString("&3&oThe worlds disaster difficulty level is set to "+worldObj.difficulty.getLabel()+" &3&oyou must change the worlds difficulty to &f&lCUSTOM &3&ofor these changes to take effect."));
			WorldObject.yamlFile.set(worldObj.getWorld().getName()+".general.min_timer", time);
			WorldObject.saveYamlFile(plugin);
			return true;
		} else if (args[0].equalsIgnoreCase("reload")) {
			if (sender instanceof Player && !(((Player) sender).hasPermission("deadlydisasters.modify"))) {
				sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.permission_error")));
				return true;
			}
			if (args.length != 1) {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters reload"));
				return true;
			}
			plugin.reloadConfig();
			Utils.reloadPlugin(plugin);
			sender.sendMessage(Utils.convertString(Languages.prefix+"&aSuccessfully reloaded the config!\n&7&oAll disasters currently ongoing will not have their settings reloaded!"));
			return true;
		} else if (args[0].equalsIgnoreCase("help")) {
			if (sender instanceof Player && !(((Player) sender).hasPermission("deadlydisasters.help"))) {
				sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.permission_error")));
				return true;
			}
			if (args.length > 2) {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters help [disaster]"));
				return true;
			}
			if (args.length == 1) {
				sender.sendMessage(Utils.convertString("&3&l====================================\n&6&n&l"+Languages.langFile.getString("misc.prefix")+" Commands:"
						+ "\n&6/disasters start <disaster> [level] [player] &3- "+Languages.langFile.getString("helpCommand.start")
						+ "\n&6/disasters enable <disaster | randomdisasters | maxlevels | eventmsg> <world> &3- "+Languages.langFile.getString("helpCommand.enable")
						+ "\n&6/disasters disable <disaster | randomdisasters | maxlevels | eventmsg> <world> &3- "+Languages.langFile.getString("helpCommand.disable")
						+ "\n&6/disasters mintimer <world> <seconds> &3- "+Languages.langFile.getString("helpCommand.mintimer")
						+ "\n&6/disasters reload &3- "+Languages.langFile.getString("helpCommand.reload")
						+ "\n&6/disasters help [disaster] &3- "+Languages.langFile.getString("helpCommand.help")
						+ "\n&6/disasters give <item> &3- "+Languages.langFile.getString("helpCommand.give")
						+ "\n&6/disasters summon <entity> &3- "+Languages.langFile.getString("helpCommand.summon")
						+ "\n&6/disasters difficulty <world> <difficulty> &3- "+Languages.langFile.getString("helpCommand.difficulty")
						+ "\n&6/disasters language <language> &3- "+Languages.langFile.getString("helpCommand.language")
						+ "\n&3"+Languages.langFile.getString("helpCommand.weather")
						+ "\n&6/disasters catalog &3- "+Languages.langFile.getString("helpCommand.catalog")
						+ "\n&6/disasters whitelist <add|remove> <player> <world> &3- "+Languages.langFile.getString("helpCommand.whitelist")
						+ "\n&6/disasters listplayer <player> &3- "+Languages.langFile.getString("helpCommand.listplayer")
						+ "\n&6/disasters config <save|swap|delete> <template> &3- "+Languages.langFile.getString("helpCommand.config")));
				return true;
			}
			if (args[1].equalsIgnoreCase("sinkhole"))
				sender.sendMessage(Languages.prefix+Utils.convertString(Disaster.SINKHOLE.getLabel()+" &bcreate massive holes in the ground pulling all mobs down with them, pools of lava form near the bottom.\n&7Damage: &4SEVERE\n&7Performance: &cLOW"));
			else if (args[1].equalsIgnoreCase("earthquake"))
				sender.sendMessage(Languages.prefix+Utils.convertString(Disaster.EARTHQUAKE.getLabel()+" &bcreate massive cracks in the world launching all nearby mobs around and forming pools of lava at the bottom. High levels create tremors.\n&7Damage: &4SEVERE\n&7Performance: &cLOW"));
			else if (args[1].equalsIgnoreCase("tornado"))
				sender.sendMessage(Languages.prefix+Utils.convertString(Disaster.TORNADO.getLabel()+" &bform strong currents of wind that throw mobs and blocks all around as they are blown higher and higher. Tornados do not move very far.\n&7Damage: &cLARGE\n&7Performance: &cLOW"));
			else if (args[1].equalsIgnoreCase("geyser"))
				sender.sendMessage(Languages.prefix+Utils.convertString("&9Water Geysers &bform on the overworld environment. Boiling hot water spurts that burn to the touch, fire resistance will protect you. &cLava Geysers &bform in nether environments. Spurts of blazing hot lava from the deepest pits of the nether.\n&7Damage: &eMINIMAL\n&7Performance: &eMEDIUM"));
			else if (args[1].equalsIgnoreCase("cavein"))
				sender.sendMessage(Languages.prefix+Utils.convertString(Disaster.CAVEIN.getLabel()+" &bunstable cave roofs collapse on players burying them alive. Form below surface in caves only and do massive damage.\n&7Damage: &eMINIMAL\n&7Performance: &eMEDIUM"));
			else if (args[1].equalsIgnoreCase("tsunami"))
				sender.sendMessage(Languages.prefix+Utils.convertString(Disaster.TSUNAMI.getLabel()+" &bmassive currents from the ocean sending waves of water to topple buildings and drown mobs.\n&7Damage: &eMINIMAL\n&7Performance: &cLOW"));
			else if (args[1].equalsIgnoreCase("acidstorm"))
				sender.sendMessage(Languages.prefix+Utils.convertString(Disaster.ACIDSTORM.getLabel()+" &bacidic rain pours from skies melting all metal equipment and tools, rain does not melt blocks.\n&7Damage: &eMINIMAL\n&7Performance: &aHIGH"));
			else if (args[1].equalsIgnoreCase("plague"))
				sender.sendMessage(Languages.prefix+Utils.convertString(Disaster.PLAGUE.getLabel()+" &brandom mobs contract the black plague and will suffer severe symptoms leading to death, there is no cure and mobs can pass the plague to any others they come into contact with.\n&7Damage: &aMILD\n&7Performance: &aHIGH"));
			else if (args[1].equalsIgnoreCase("blizzard"))
				sender.sendMessage(Languages.prefix+Utils.convertString(Disaster.BLIZZARD.getLabel()+" &bonly occur in arctic environments and will freeze mobs solid, mobs can be thawed out of ice after weather clears up, full leather armor can protect you from the cold and strong fires can too.\n&7Damage: &aMILD\n&7Performance: &aHIGH"));
			else if (args[1].equalsIgnoreCase("extremewinds"))
				sender.sendMessage(Languages.prefix+Utils.convertString(Disaster.EXTREMEWINDS.getLabel()+" &bwildy strong winds that carry mobs and objects through the air, each levels force can be modified in the config.\n&7Damage: &aMILD\n&7Performance: &eMEDIUM"));
			else if (args[1].equalsIgnoreCase("meteorshowers"))
				sender.sendMessage(Languages.prefix+Utils.convertString(Disaster.METEORSHOWERS.getLabel()+" &bthe sky turns dark and massive meteors from space come collapsing down on players, there are 3 types of meteors and some can have valuable ores in them.\n&7Damage: &cLARGE\n&7Performance: &eMEDIUM"));
			else if (args[1].equalsIgnoreCase("sandstorm"))
				sender.sendMessage(Languages.prefix+Utils.convertString(Disaster.SANDSTORM.getLabel()+" &bonly occur in desert environments, mobs are buffeted by sand blinding and withering them, dangerous mobs also appear in this violent weather and skulls can be found on the ground after these storms.\n&7Damage: &aMILD\n&7Performance: &aHIGH"));
			else if (args[1].equalsIgnoreCase("soulstorm"))
				sender.sendMessage(Languages.prefix+Utils.convertString(Disaster.SOULSTORM.getLabel()+" &bonly occur in nether environments, storms made of lost souls of the dead crying for help, vex's can spawn and attack violently.\n&7Damage: &aMILD\n&7Performance: &aHIGH"));
			else if (args[1].equalsIgnoreCase("endstorm"))
				sender.sendMessage(Languages.prefix+Utils.convertString(Disaster.ENDSTORM.getLabel()+" &bonly occur in end environments, unstable rifts release dangerous creatures from the deepest depths of the void.\n&7Damage: &aMILD\n&7Performance: &aHIGH"));
			else if (args[1].equalsIgnoreCase("supernova"))
				sender.sendMessage(Languages.prefix+Utils.convertString(Disaster.SUPERNOVA.getLabel()+" &bis an exploding star that causes colossal damage to the environment obliterating everything to dust.\n&7Damage: &4SEVERE\n&7Performance: &cLOW"));
			else if (args[1].equalsIgnoreCase("hurricane"))
				sender.sendMessage(Languages.prefix+Utils.convertString(Disaster.HURRICANE.getLabel()+" &bis a violent wind storm that travels throughout the world wiping out all that go near them.\n&7Damage: &aMILD\n&7Performance: &eMEDIUM"));
			else if (args[1].equalsIgnoreCase("purge"))
				sender.sendMessage(Languages.prefix+Utils.convertString(Disaster.PURGE.getLabel()+" &bmeans that a player is being targeted by a horde of mobs and must die or kill a certain amount for the horde to dissipate.\n&7Damage: &aLOW\n&7Performance: &aHIGH"));
			else if (args[1].equalsIgnoreCase("solarstorm"))
				sender.sendMessage(Languages.prefix+Utils.convertString(Disaster.SOLARSTORM.getLabel()+" &bis a very dangerous storm that rains fire and fire phantoms, all burning entities will be on fire for twice as long during solarstorms.\n&7Damage: &eMINIMAL\n&7Performance: &aHIGH"));
			else
				sender.sendMessage(Utils.convertString("&cUsage: /disasters help [disaster]"));
			return true;
		} else if (args[0].equalsIgnoreCase("summon")) {
			if (sender instanceof Player && !(((Player) sender).hasPermission("deadlydisasters.summon"))) {
				sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.permission_error")));
				return true;
			}
			Location loc = null;
			List<String> otherTypes = new ArrayList<>(Arrays.asList("normalMeteor","explodingMeteor","splittingMeteor"));
			Object optionalData = null;
			if (sender instanceof Player) {
				if (otherTypes.contains(args[1])) {
					if (args.length != 2 && args.length != 3) {
						sender.sendMessage(Utils.convertString("&cUsage: /disasters summon "+args[1]+" [size]"));
						return true;
					}
					if (args.length == 3)
						try {
							optionalData = Integer.parseInt(args[2]);
						} catch (NumberFormatException e) {
							sender.sendMessage(Utils.convertString("&cSize must be a valid integer!"));
							return true;
						}
					else
						optionalData = 5;
				} else if (args.length != 2 && args.length != 5) {
					sender.sendMessage(Utils.convertString("&cUsage: /disasters summon <entity> [x y z]"));
					return true;
				}
				loc = ((Player) sender).getLocation();
			} else {
				if (args.length < 6 || args.length > 7) {
					sender.sendMessage(Utils.convertString("&cUsage: /disasters summon <entity> <x y z> <world> [data]"));
					return true;
				}
				World world = Bukkit.getWorld(args[5]);
				if (world == null) {
					sender.sendMessage(Utils.convertString("&cUsage: /disasters summon <entity> <x y z> <world> [data]"));
					return true;
				}
				loc = new Location(world, 0, 0, 0);
				if (otherTypes.contains(args[1])) {
					if (args.length == 7)
						try {
							optionalData = Integer.parseInt(args[6]);
						} catch (NumberFormatException e) {
							sender.sendMessage(Utils.convertString("&cSize must be a valid integer!"));
							return true;
						}
					else
						optionalData = 5;
				}
			}
			if (args.length >= 5)
				try {
					loc.setX(Double.valueOf(args[2])+0.5);
					loc.setY(Double.valueOf(args[3]));
					loc.setZ(Double.valueOf(args[4])+0.5);
				} catch (NumberFormatException e) {
					if (sender instanceof Player)
						sender.sendMessage(Utils.convertString("&cUsage: /disasters summon <entity> [x y z]"));
					else
						sender.sendMessage(Utils.convertString("&cUsage: /disasters summon <entity> <x y z> <world>"));
					return true;
				}
			if (args[1].equalsIgnoreCase("babyendtotem")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.WOLF);
				handler.addEntity(new BabyEndTotem(entity, plugin, rand));
				sender.sendMessage(Utils.convertString("&fSummoned &5End Totem"));
				return true;
			} else if (args[1].equalsIgnoreCase("endtotem")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.WITHER_SKELETON);
				entity.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
				handler.addEntity(new EndTotem(entity, plugin, rand));
				sender.sendMessage(Utils.convertString("&fSummoned &5End Totem"));
				return true;
			} else if (args[1].equalsIgnoreCase("endworm")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
				handler.addEntity(new EndWorm(entity, plugin, rand));
				sender.sendMessage(Utils.convertString("&fSummoned &5End Worm"));
				return true;
			} else if (args[1].equalsIgnoreCase("voidguardian")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
				handler.addEntity(new VoidGuardian(entity, plugin, rand));
				sender.sendMessage(Utils.convertString("&fSummoned &5Void Guardian"));
				return true;
			} else if (args[1].equalsIgnoreCase("voidarcher")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.SKELETON);
				handler.addEntity(new VoidArcher(entity, plugin, rand));
				sender.sendMessage(Utils.convertString("&fSummoned &5Void Archer"));
				return true;
			} else if (args[1].equalsIgnoreCase("voidstalker")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.PHANTOM);
				handler.addEntity(new VoidStalker(entity, plugin, rand));
				sender.sendMessage(Utils.convertString("&fSummoned &5Void Stalker"));
				return true;
			} else if (args[1].equalsIgnoreCase("lostsoul")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.VEX);
				handler.addEntity(new LostSoul(entity, plugin, rand));
				sender.sendMessage(Utils.convertString("&fSummoned &3Lost Soul"));
				return true;
			} else if (args[1].equalsIgnoreCase("ancientskeleton")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.SKELETON);
				handler.addEntity(new AncientSkeleton(entity, plugin, rand));
				sender.sendMessage(Utils.convertString("&fSummoned &6Ancient Skeleton"));
				return true;
			} else if (args[1].equalsIgnoreCase("ancientmummy")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.HUSK);
				handler.addEntity(new AncientMummy(entity, plugin, rand));
				sender.sendMessage(Utils.convertString("&fSummoned &6Ancient Mummy"));
				return true;
			} else if (args[1].equalsIgnoreCase("tunneller")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
				handler.addEntity(new TunnellerZombie((Zombie) entity, null, plugin));
				sender.sendMessage(Utils.convertString("&fSummoned &7Tunneller"));
				return true;
			} else if (args[1].equalsIgnoreCase("primedcreeper")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.CREEPER);
				handler.addEntity(new PrimedCreeper(entity, plugin));
				sender.sendMessage(Utils.convertString("&fSummoned &7PrimedCreeper"));
				return true;
			} else if (args[1].equalsIgnoreCase("skeletonknight")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.SKELETON);
				handler.addEntity(new SkeletonKnight((Skeleton) entity, plugin));
				sender.sendMessage(Utils.convertString("&fSummoned &7SkeletonKnight"));
				return true;
			} else if (args[1].equalsIgnoreCase("darkmage")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
				entity.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
				handler.addEntity(new DarkMage(entity, plugin));
				sender.sendMessage(Utils.convertString("&fSummoned &7Dark Mage"));
				return true;
			} else if (args[1].equalsIgnoreCase("soulreaper")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.SKELETON);
				handler.addEntity(new SoulReaper(entity, plugin, rand));
				sender.sendMessage(Utils.convertString("&fSummoned &3Soul Reaper"));
				return true;
			} else if (args[1].equalsIgnoreCase("normalMeteor")) {
				Location spawn = new Location(loc.getWorld(), loc.getBlockX()+(rand.nextInt(100)-50), Math.min(loc.getBlockY()+70, 320), loc.getBlockZ()+(rand.nextInt(100)-50));
				if (optionalData == null)
					optionalData = 5;
				MeteorShower.spawnMeteor(1, spawn, loc, plugin, (int) optionalData);
				sender.sendMessage(Utils.convertString("&fSummoned &7Normal Meteor"));
				return true;
			} else if (args[1].equalsIgnoreCase("explodingMeteor")) {
				Location spawn = new Location(loc.getWorld(), loc.getBlockX()+(rand.nextInt(100)-50), Math.min(loc.getBlockY()+70, 320), loc.getBlockZ()+(rand.nextInt(100)-50));
				if (optionalData == null)
					optionalData = 5;
				MeteorShower.spawnMeteor(2, spawn, loc, plugin, (int) optionalData);
				sender.sendMessage(Utils.convertString("&fSummoned &cExploding Meteor"));
				return true;
			} else if (args[1].equalsIgnoreCase("splittingMeteor")) {
				Location spawn = new Location(loc.getWorld(), loc.getBlockX()+(rand.nextInt(100)-50), Math.min(loc.getBlockY()+70, 320), loc.getBlockZ()+(rand.nextInt(100)-50));
				if (optionalData == null)
					optionalData = 5;
				MeteorShower.spawnMeteor(3, spawn, loc, plugin, (int) optionalData);
				sender.sendMessage(Utils.convertString("&fSummoned Splitting Meteor"));
				return true;
			} else if (args[1].equalsIgnoreCase("firephantom")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.PHANTOM);
				handler.addEntity(new FirePhantom(entity, plugin, rand));
				sender.sendMessage(Utils.convertString("&fSummoned &cFire Phantom"));
				return true;
			} else if (args[1].equalsIgnoreCase("swampbeast")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
				handler.addEntity(new SwampBeast(entity, plugin));
				sender.sendMessage(Utils.convertString("&fSummoned &7Swamp Beast"));
				return true;
			} else if (args[1].equalsIgnoreCase("zombieknight")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
				handler.addEntity(new ZombieKnight(entity, plugin));
				sender.sendMessage(Utils.convertString("&fSummoned &7Zombie Knight"));
				return true;
			} else if (args[1].equalsIgnoreCase("shadowleech")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
				handler.addEntity(new ShadowLeech((Zombie) entity, plugin, rand));
				sender.sendMessage(Utils.convertString("&fSummoned &7Shadow Leech"));
				return true;
			} else if (args[1].equalsIgnoreCase("elf")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
				handler.addEntity(new Elf((Zombie) entity, plugin, rand));
				sender.sendMessage(Utils.convertString("&fSummoned &cElf"));
				return true;
			} else if (args[1].equalsIgnoreCase("frosty")) {
				Mob entity = (Mob) loc.getWorld().spawn(loc, Snowman.class);
				handler.addEntity(new Frosty((Snowman) entity, plugin, rand));
				sender.sendMessage(Utils.convertString("&fSummoned &cFrosty"));
				return true;
			} else if (args[1].equalsIgnoreCase("grinch")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
				handler.addEntity(new Grinch(entity, plugin, rand));
				sender.sendMessage(Utils.convertString("&fSummoned &cGrinch"));
				return true;
			} else if (args[1].equalsIgnoreCase("santa")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
				handler.addEntity(new Santa((Zombie) entity, plugin, rand));
				sender.sendMessage(Utils.convertString("&fSummoned &cSanta"));
				return true;
			} else if (args[1].equalsIgnoreCase("rampaginggoat")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.GOAT);
				handler.addEntity(new RampagingGoat((Goat) entity, plugin));
				sender.sendMessage(Utils.convertString("&fSummoned &aRampaging Goat"));
				return true;
			} else if (args[1].equalsIgnoreCase("easterbunny")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.RABBIT);
				handler.addEntity(new EasterBunny((Rabbit) entity, plugin, rand));
				sender.sendMessage(Utils.convertString("&fSummoned &aEaster Bunny"));
				return true;
			} else if (args[1].equalsIgnoreCase("killerchicken")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
				handler.addEntity(new KillerChicken((Zombie) entity, plugin));
				sender.sendMessage(Utils.convertString("&fSummoned &aKiller Chicken"));
				return true;
			} else if (args[1].equalsIgnoreCase("scarecrow")) {
				Zombie entity = (Zombie) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE, false);
				handler.addEntity(new Scarecrow((Zombie) entity, plugin, rand));
				sender.sendMessage(Utils.convertString("&fSummoned &6Scarecrow"));
				return true;
			} else if (args[1].equalsIgnoreCase("ghoul")) {
				if (!(sender instanceof Player)) {
					sender.sendMessage("Cannot summon this mob from console!");
					return true;
				}
				Block block = ((Player) sender).getTargetBlockExact(5, FluidCollisionMode.NEVER);
				if (block == null || !block.getType().isSolid()) {
					sender.sendMessage(Utils.convertString("&cCan only summon on solid block! Please look at a solid block when running this command!"));
					return true;
				}
				Mob entity = (Mob) loc.getWorld().spawnEntity(block.getRelative(BlockFace.DOWN).getLocation(), EntityType.ZOMBIE, false);
				handler.addEntity(new Ghoul((Zombie) entity, block, plugin, true));
				sender.sendMessage(Utils.convertString("&fSummoned &6Ghoul"));
				return true;
			} else if (args[1].equalsIgnoreCase("vampire")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.EVOKER, false);
				handler.addEntity(new Vampire((Mob) entity, plugin));
				sender.sendMessage(Utils.convertString("&fSummoned &6Vampire"));
				return true;
			} else if (args[1].equalsIgnoreCase("psyco")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.SKELETON, false);
				handler.addEntity(new Psyco((Mob) entity, plugin, rand));
				sender.sendMessage(Utils.convertString("&fSummoned &6Psyco Killer"));
				return true;
			} else if (args[1].equalsIgnoreCase("pumpkinking")) {
				Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.WITHER_SKELETON, false);
				handler.addEntity(new PumpkinKing((Mob) entity, plugin, rand));
				sender.sendMessage(Utils.convertString("&fSummoned &6Pumpkin King"));
				return true;
			} else
				sender.sendMessage(Utils.convertString("&c'"+args[1]+"' is not a valid mob!"));
			return true;
		} else if (args[0].equalsIgnoreCase("give")) {
			if (sender instanceof Player && !(((Player) sender).hasPermission("deadlydisasters.give"))) {
				sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.permission_error")));
				return true;
			}
			if (args.length < 2 || args.length > 3) {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters give <item> [player]"));
				return true;
			}
			Player p = null;
			if (args.length == 2 && sender instanceof Player)
				p = (Player) sender;
			else if (args.length == 3) {
				p = Bukkit.getPlayer(args[2]);
				if (p == null) {
					sender.sendMessage(Utils.convertString("&cCould not find player '"+args[2]+"'"));
					return true;
				}
			} else {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters give <item> <player>"));
				return true;
			}
			if (p.getInventory().firstEmpty() == -1) {
				sender.sendMessage(Utils.convertString("&cTargets inventory is full!"));
				return true;
			}
			if (!ItemsHandler.allItems.keySet().contains(args[1].toLowerCase())) {
				sender.sendMessage(Utils.convertString("&c'"+args[1]+"' is not a valid item!"));
				return true;
			}
			ItemStack item = ItemsHandler.allItems.get(args[1].toLowerCase());
			p.getInventory().addItem(item);
			sender.sendMessage(Utils.convertString("&aGave 1 ["+item.getItemMeta().getDisplayName()+"&a] to "+p.getName()));
			p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
			return true;
		} else if (args[0].equalsIgnoreCase("difficulty")) {
			if (sender instanceof Player && !(((Player) sender).hasPermission("deadlydisasters.modify"))) {
				sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.permission_error")));
				return true;
			}
			if (args.length != 3) {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters difficulty <world> <difficulty>"));
				return true;
			}
			WorldObject worldObj = null;
			if (args[1].equalsIgnoreCase("this_world"))
				if (sender instanceof Player)
					worldObj = WorldObject.findWorldObject(((Player) sender).getWorld());
				else {
					sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.console_error_message")));
					return true;
				}
			else if (!args[1].equalsIgnoreCase("all_worlds")) {
				if (Bukkit.getWorld(args[1]) == null) {
					sender.sendMessage(Utils.convertString("&cCould not find world '"+args[1]+"'"));
					return true;
				}
				worldObj = WorldObject.findWorldObject(Bukkit.getWorld(args[1]));
			}
			DifficultyLevel diff = DifficultyLevel.forName(args[2].toUpperCase());
			if (diff == null) {
				sender.sendMessage(Utils.convertString("&cThe difficulty "+args[2]+" does not exist!"));
				return true;
			}
			String message = "";
			if (args[1].equalsIgnoreCase("all_worlds")) {
				for (WorldObject temp : WorldObject.worlds) {
					WorldObject.changeDifficulty(temp, diff);
					tc.updateTimerList(temp.getWorld());
				}
				WorldObject.saveYamlFile(plugin);
				if (diff != DifficultyLevel.CUSTOM) {
					message = Utils.convertString(CoreListener.worldMessage.replace("%difficulty%", diff.getLabel()).replace("%world%", "ALL_WORLDS")
							+ "\n&3- "+Languages.langFile.getString("internal.min_timer")+": &6"+(diff.getTimer())+" &7/ &3"+Languages.langFile.getString("internal.offset")+": &6"+(diff.getOffset())
							+ "\n&3- "+Languages.langFile.getString("internal.levelWord")+": &a"+(diff.getTable()[0])+"% &2"+(diff.getTable()[1])+"% &b"+(diff.getTable()[2])+"% &e"+(diff.getTable()[3])+"% &c"+(diff.getTable()[4])+"% &4"+(diff.getTable()[5])+"%");
					for (Player p : Bukkit.getOnlinePlayers())
						if (p.hasPermission("deadlydisasters.difficultyNotify"))
							p.sendMessage(message);
				} else {
					message = Utils.convertString(CoreListener.worldMessage.replace("%difficulty%", diff.getLabel()).replace("%world%", "ALL_WORLDS"));
					for (Player p : Bukkit.getOnlinePlayers())
						if (p.hasPermission("deadlydisasters.difficultyNotify")) {
							WorldObject obj = WorldObject.findWorldObject(p.getWorld());
							String tf = Utils.convertString("&c&l"+Languages.langFile.getString("internal.offWord"));
							if (obj.naturalAllowed)
								tf = Utils.convertString("&a&l"+Languages.langFile.getString("internal.onWord"));
							p.sendMessage(message+Utils.convertString("\n&3- "+Languages.langFile.getString("internal.random_disasters")+": "+tf
									+ "\n&3- "+Languages.langFile.getString("internal.min_timer")+": &6"+(obj.timer)+" &7 &3"+Languages.langFile.getString("internal.offset")+": &6"+(obj.offset)
									+ "\n&3- "+Languages.langFile.getString("internal.levelWord")+": &a"+(obj.table[0])+"% &2"+(obj.table[1])+"% &b"+(obj.table[2])+"% &e"+(obj.table[3])+"% &c"+(obj.table[4])+"% &4"+(obj.table[5])+"%"));
							p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.3F, 1);
						}
				}
				return true;
			}
			WorldObject.changeDifficulty(worldObj, diff);
			WorldObject.saveYamlFile(plugin);
			tc.updateTimerList(worldObj.getWorld());
			String tf = Utils.convertString("&c&l"+Languages.langFile.getString("internal.offWord"));
			if (worldObj.naturalAllowed)
				tf = Utils.convertString("&a&l"+Languages.langFile.getString("internal.onWord"));
			message = Utils.convertString(CoreListener.worldMessage.replace("%difficulty%", diff.getLabel()).replace("%world%", worldObj.getWorld().getName())
					+ "\n&3- "+Languages.langFile.getString("internal.random_disasters")+": "+tf
					+ "\n&3- "+Languages.langFile.getString("internal.min_timer")+": &6"+(worldObj.timer)+" &7/ &3"+Languages.langFile.getString("internal.offset")+": &6"+(worldObj.offset)
					+ "\n&3- "+Languages.langFile.getString("internal.levelWord")+": &a"+(worldObj.table[0])+"% &2"+(worldObj.table[1])+"% &b"+(worldObj.table[2])+"% &e"+(worldObj.table[3])+"% &c"+(worldObj.table[4])+"% &4"+(worldObj.table[5])+"%");
			for (Player p : worldObj.getWorld().getPlayers())
				if (p.hasPermission("deadlydisasters.difficultyNotify")) {
					p.sendMessage(message);
					p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.3F, 1);
				}
			return true;
		} else if (args[0].equalsIgnoreCase("language")) {
			if (sender instanceof Player && !(((Player) sender).hasPermission("deadlydisasters.modify"))) {
				sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.permission_error")));
				return true;
			}
			if (args.length != 2) {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters language <language>"));
				return true;
			}
			Player[] p = new Player[1];
			if (sender instanceof Player)
				p[0] = (Player) sender;
			if (args[1].equalsIgnoreCase("english")) {
				plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
					public void run() {
						Languages.updateLang(0, plugin, p[0]);
						Languages.changeConfigLang(plugin);
						plugin.dataFile.set("data.lang", 0);
						plugin.saveDataFile();
						sender.sendMessage(Languages.prefix+Utils.convertString("&bLanguage translations have been set to &eEnglish"));
						Main.consoleSender.sendMessage(Utils.convertString(Languages.prefix+"&bLanguage translations have been set to &eEnglish"));
					}
				});
			} else if (args[1].equalsIgnoreCase("中文")) {
				plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
					public void run() {
						Languages.updateLang(1, plugin, p[0]);
						Languages.changeConfigLang(plugin);
						plugin.dataFile.set("data.lang", 1);
						plugin.saveDataFile();
						sender.sendMessage(Languages.prefix+Utils.convertString("&b插件语言已设置为&e中文。 请记住，并非所有内容都会被翻译!"
								+ "\n&3- 翻译者 &dKPC123"
								+ "\n&3- https://www.mcbbs.net/thread-1288279-1-1.html"));
						Main.consoleSender.sendMessage(Languages.prefix+Utils.convertString("&b插件语言已设置为&e中文。 请记住，并非所有内容都会被翻译!&r"
								+ "\n&3- 翻译者 &dKPC123&r"
								+ "\n&3- https://www.mcbbs.net/thread-1288279-1-1.html"));
					}
				});
			} else if (args[1].equalsIgnoreCase("русский")) {
				plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
					public void run() {
						Languages.updateLang(2, plugin, p[0]);
						Languages.changeConfigLang(plugin);
						plugin.dataFile.set("data.lang", 2);
						plugin.saveDataFile();
						sender.sendMessage(Languages.prefix+Utils.convertString("&bЯзык был изменен на русский. Обратите внимание на то, что не всё будет переведено!"
								+ "\n&3- Перевод от: ZBLL/Roughly_"));
						Main.consoleSender.sendMessage(Languages.prefix+Utils.convertString("&bЯзык был изменен на русский. Обратите внимание на то, что не всё будет переведено!"
								+ "\n&3- Перевод от: ZBLL/Roughly_"));
					}
				});
			} else if (args[1].equalsIgnoreCase("češtiny")) {
				plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
					public void run() {
						Languages.updateLang(3, plugin, p[0]);
						Languages.changeConfigLang(plugin);
						plugin.dataFile.set("data.lang", 3);
						plugin.saveDataFile();
						sender.sendMessage(Languages.prefix+Utils.convertString("&bJazyk překladu byl nastaven do češtiny. Mějte na paměti, že ne všechno bude přeloženo!"
								+ "\n&3- Přeložil: &dFreddy1CZ1"));
						Main.consoleSender.sendMessage(Languages.prefix+Utils.convertString("&bJazyk překladu byl nastaven do češtiny. Mějte na paměti, že ne všechno bude přeloženo!"
								+ "\n&3- Přeložil: &dFreddy1CZ1"));
					}
				});
			} else if (args[1].equalsIgnoreCase("français")) {
				plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
					public void run() {
						Languages.updateLang(4, plugin, p[0]);
						Languages.changeConfigLang(plugin);
						plugin.dataFile.set("data.lang", 4);
						plugin.saveDataFile();
						sender.sendMessage(Languages.prefix+Utils.convertString("&bTranslations have been set to French. Keep in mind that not everything will be translated!"
								+ "\n&3- Translations by: &dArryl"));
						Main.consoleSender.sendMessage(Languages.prefix+Utils.convertString("&bTranslations have been set to French. Keep in mind that not everything will be translated!"
								+ "\n&3- Translations by: &dArryl"));
					}
				});
			} else {
				sender.sendMessage(Utils.convertString("&c'"+args[1]+"' is not an applicable language!"));
				return true;
			}
			if (plugin.dataFile.getBoolean("data.firstStart")) {
				sender.sendMessage(Languages.prefix+Languages.firstStart
						+ "\n"+Utils.convertString("&c"+Languages.langFile.getString("internal.allowFlight")));
			}
			return true;
		} else if (args[0].equalsIgnoreCase("catalog")) {
			if (sender instanceof Player && !(((Player) sender).hasPermission("deadlydisasters.modify"))) {
				sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.permission_error")));
				return true;
			}
			((Player) sender).openInventory(catalog.featuredPage);
			return true;
		} else if (args[0].equalsIgnoreCase("whitelist")) {
			if (sender instanceof Player && !(((Player) sender).hasPermission("deadlydisasters.whitelist"))) {
				sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.permission_error")));
				return true;
			}
			if (args.length != 4) {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters whitelist <add|remove> <player> <world>"));
				return true;
			}
			if (Bukkit.getPlayer(args[2]) == null) {
				sender.sendMessage(Utils.convertString("&cCould not find player '"+args[2]+"'"));
				return true;
			}
			WorldObject worldObj = null;
			if (args[3].equalsIgnoreCase("this_world"))
				if (sender instanceof Player)
					worldObj = WorldObject.findWorldObject(((Player) sender).getWorld());
				else {
					sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.console_error_message")));
					return true;
				}
			else if (!args[3].equalsIgnoreCase("all_worlds")) {
				if (Bukkit.getWorld(args[3]) == null) {
					sender.sendMessage(Utils.convertString("&cCould not find world '"+args[3]+"'"));
					return true;
				}
				worldObj = WorldObject.findWorldObject(Bukkit.getWorld(args[3]));
			}
			UUID uuid = Bukkit.getPlayer(args[2]).getUniqueId();
			if (args[1].equalsIgnoreCase("add")) {
				if (args[3].equalsIgnoreCase("all_worlds")) {
					for (WorldObject wo : WorldObject.worlds) {
						wo.whitelist.add(uuid);
						List<String> players = WorldObject.yamlFile.getStringList(wo.getWorld().getName()+".whitelist");
						players.add(uuid.toString());
						WorldObject.yamlFile.set(wo.getWorld().getName()+".whitelist", players);
					}
					WorldObject.saveYamlFile(plugin);
					sender.sendMessage(Utils.convertString("&aAdded &6'"+args[2]+"' &bto the whitelist for &dALL_WORLDS"));
					return true;
				}
				worldObj.whitelist.add(uuid);
				List<String> players = WorldObject.yamlFile.getStringList(worldObj.getWorld().getName()+".whitelist");
				players.add(uuid.toString());
				WorldObject.yamlFile.set(worldObj.getWorld().getName()+".whitelist", players);
				WorldObject.saveYamlFile(plugin);
				sender.sendMessage(Utils.convertString("&aAdded &6'"+args[2]+"' &bto the whitelist for &d"+worldObj.getWorld().getName()));
				return true;
			} else if (args[1].equalsIgnoreCase("remove")) {
				if (args[3].equalsIgnoreCase("all_worlds")) {
					for (WorldObject wo : WorldObject.worlds) {
						wo.whitelist.remove(uuid);
						List<String> players = WorldObject.yamlFile.getStringList(wo.getWorld().getName()+".whitelist");
						players.remove(uuid.toString());
						WorldObject.yamlFile.set(wo.getWorld().getName()+".whitelist", players);
					}
					WorldObject.saveYamlFile(plugin);
					sender.sendMessage(Utils.convertString("&cRemoved &6'"+args[2]+"' &bto the whitelist for &dALL_WORLDS"));
					return true;
				}
				worldObj.whitelist.remove(uuid);
				List<String> players = WorldObject.yamlFile.getStringList(worldObj.getWorld().getName()+".whitelist");
				players.remove(uuid.toString());
				WorldObject.yamlFile.set(worldObj.getWorld().getName()+".whitelist", players);
				WorldObject.saveYamlFile(plugin);
				sender.sendMessage(Utils.convertString("&cRemoved &6'"+args[2]+"' &bto the whitelist for &d"+worldObj.getWorld().getName()));
				return true;
			} else {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters whitelist <add|remove> <player> <world>"));
				return true;
			}
		} else if (args[0].equalsIgnoreCase("listplayer")) {
			if (sender instanceof Player && !(((Player) sender).hasPermission("deadlydisasters.listplayer"))) {
				sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.permission_error")));
				return true;
			}
			if (args.length != 2) {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters listplayer <player>"));
				return true;
			}
			plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
				@SuppressWarnings("deprecation")
				@Override
				public void run() {
					Player player = Bukkit.getPlayer(args[1]);
					UUID uuid = null;
					String name = null;
					if (player == null) {
						OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
						if (offlinePlayer == null) {
							sender.sendMessage(Utils.convertString("&cPlayer '"+args[1]+"' does not exist!"));
							return;
						}
						uuid = offlinePlayer.getUniqueId();
						name = offlinePlayer.getName();
					} else {
						uuid = player.getUniqueId();
						name = player.getName();
					}
					StringBuilder string = new StringBuilder(Languages.prefix+Utils.convertString("&bCurrent timer values for &e"+name));
					if (Bukkit.getPlayer(uuid) != null)
						for (UUID worldUUID : tc.timer.keySet())
							if (Bukkit.getWorld(worldUUID).equals(Bukkit.getPlayer(uuid).getWorld())) {
								string.append(Utils.convertString("\n&d"+Bukkit.getWorld(worldUUID).getName()+" &7: &c"+tc.timer.get(worldUUID).get(uuid)));
								if (DisasterEvent.countdownMap.containsKey(uuid))
									for (Entry<DisasterEvent, Integer> entry : DisasterEvent.countdownMap.get(uuid).entrySet())
										string.append(Utils.convertString("\n  &7- "+entry.getKey().type.getLabel()+" &7(&"+Utils.getLevelChar(entry.getKey().level)+entry.getKey().level+"&7) : &e"+entry.getValue()+"&9s"));
							} else
								string.append(Utils.convertString("\n&d"+Bukkit.getWorld(worldUUID).getName()+" &7: &a"+tc.timer.get(worldUUID).get(uuid)));
					else
						for (UUID worldUUID : tc.timer.keySet())
							if (plugin.dataFile.contains("timers."+worldUUID+"."+uuid))
								string.append(Utils.convertString("\n&d"+Bukkit.getWorld(worldUUID).getName()+" &7: &a"+plugin.dataFile.getInt("timers."+worldUUID+"."+uuid)));
							else
								string.append(Utils.convertString("\n&d"+Bukkit.getWorld(worldUUID).getName()+" &7: &a-"));
					sender.sendMessage(string.toString());
				}
			});
			return true;
		} else if (args[0].equalsIgnoreCase("config")) {
			if (sender instanceof Player && !(((Player) sender).hasPermission("deadlydisasters.modify"))) {
				sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.permission_error")));
				return true;
			}
			if (args.length != 3 && !args[1].equalsIgnoreCase("view")) {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters config <save|swap|delete|view> <template>"));
				return true;
			}
			if (args[1].equalsIgnoreCase("save")) {
				if (plugin.cfgSwapper.configTemplates.contains(args[2])) {
					sender.sendMessage(Utils.convertString("&cA config template with the name '"+args[2]+"' already exists!"));
					return true;
				}
				plugin.cfgSwapper.saveConfig(args[2]);
				sender.sendMessage(Utils.convertString("&aSaved new config template &b'"+args[2]+"' &asuccessfully!"));
				return true;
			} else if (args[1].equalsIgnoreCase("swap")) {
				if (!plugin.cfgSwapper.configTemplates.contains(args[2])) {
					sender.sendMessage(Utils.convertString("&cThere is no config template with the name '"+args[2]+"'!"));
					return true;
				}
				if (plugin.cfgSwapper.currentConfig.equals(args[2])) {
					sender.sendMessage(Utils.convertString("&cTemplate '"+args[2]+"' is already selected!"));
					return true;
				}
				if (plugin.cfgSwapper.swapConfigs(args[2]))
					sender.sendMessage(Utils.convertString("&aSwapped config to template &b'"+args[2]+"' &asuccessfully!"));
				else
					sender.sendMessage(Utils.convertString("&cCould not swap to template '"+args[2]+"'!"));
				return true;
			} else if (args[1].equalsIgnoreCase("delete")) {
				if (!plugin.cfgSwapper.configTemplates.contains(args[2])) {
					sender.sendMessage(Utils.convertString("&cThere is no config template with the name '"+args[2]+"'!"));
					return true;
				}
				if (plugin.cfgSwapper.deleteConfig(args[2]))
					sender.sendMessage(Utils.convertString("&aRemoved config template &b'"+args[2]+"' &asuccessfully!"));
				else
					sender.sendMessage(Utils.convertString("&cCould not delete template '"+args[2]+"'!"));
				return true;
			} else if (args[1].equalsIgnoreCase("view")) {
				StringBuilder builder = new StringBuilder(Languages.prefix+Utils.convertString("&bList of config templates:"));
				for (String s : plugin.cfgSwapper.configTemplates)
					if (s.equals(plugin.cfgSwapper.currentConfig))
						builder.append(Utils.convertString("\n&3- &a"+s+" &7&o(Current)"));
					else
						builder.append(Utils.convertString("\n&3- &e"+s));
				sender.sendMessage(builder.toString());
				return true;
			} else {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters config <save|swap|delete> <template>"));
				return true;
			}
		} else if (args[0].equalsIgnoreCase("favor")) {
			if (sender instanceof Player && !(((Player) sender).hasPermission("deadlydisasters.modify"))) {
				sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.permission_error")));
				return true;
			}
			if (args.length != 2 || Disaster.forName(args[1].toUpperCase()) == null) {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters favor <disaster>\nVote also cannot be a custom disaster!"));
				return true;
			}
			Disaster disaster = Disaster.valueOf(args[1].toUpperCase());
			plugin.dataFile.set("data.favored", disaster.getMetricsLabel());
			plugin.saveDataFile();
			sender.sendMessage(Languages.prefix+ChatColor.GREEN+Languages.langFile.getString("internal.voteSubmission"));
			return true;
		} else if (args[0].equalsIgnoreCase("dislike")) {
			if (sender instanceof Player && !(((Player) sender).hasPermission("deadlydisasters.modify"))) {
				sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.permission_error")));
				return true;
			}
			if (args.length != 2 || Disaster.forName(args[1].toUpperCase()) == null) {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters dislike <disaster>\nVote also cannot be a custom disaster!"));
				return true;
			}
			Disaster disaster = Disaster.valueOf(args[1].toUpperCase());
			plugin.dataFile.set("data.disliked", disaster.getMetricsLabel());
			plugin.saveDataFile();
			sender.sendMessage(Languages.prefix+ChatColor.GREEN+Languages.langFile.getString("internal.voteSubmission"));
			return true;
		} else if (args[0].equalsIgnoreCase("event")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.console_error_message")));
				return true;
			}
			if (!(((Player) sender).hasPermission("deadlydisasters.event"))) {
				sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.permission_error")));
				return true;
			}
			if (!plugin.eventHandler.isEnabled) {
				sender.sendMessage(Utils.convertString("&cThere is no ongoing event at this time!"));
				return true;
			}
			plugin.eventHandler.openGUI((Player) sender);
			return true;
		} else if (args[0].equalsIgnoreCase("entities")) {
			if (sender instanceof Player && !(((Player) sender).hasPermission("deadlydisasters.entities"))) {
				sender.sendMessage(Utils.convertString(plugin.getConfig().getString("messages.permission_error")));
				return true;
			}
			if (args.length < 2) {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters entities <list|kill> [type]"));
				return true;
			}
			if (args[1].equalsIgnoreCase("list")) {
				if (args.length >= 3) {
					CustomEntityType type = CustomEntityType.getCustomEntityType(args[2].toLowerCase());
					if (type == null) {
						sender.sendMessage(Utils.convertString("&cNo such species type '"+args[2]+"'"));
						return true;
					}
					sender.sendMessage(Languages.prefix+Utils.convertString("&aCurrently spawned &"+type.getColChar()+type.species));
					String species = type.species;
					for (CustomEntity e : CustomEntity.handler.getList()) {
						if (e.getEntity() == null)
							continue;
						if (e.getSpecies().equals(species)) {
							String healthString = '('+Utils.getDecimalFormatted(e.getEntity().getHealth())+'/'+Utils.getDecimalFormatted(type.getHealth())+')';
							if (e.getEntity().getHealth() < type.getHealth()/4.0)
								healthString = 'c'+healthString;
							else if (e.getEntity().getHealth() < type.getHealth()/2.0)
								healthString = 'e'+healthString;
							else
								healthString = 'a'+healthString;
							String name = type.getColChar()+type.species;
							if (e.getEntity().getCustomName() != null)
								name = type.getColChar()+e.getEntity().getCustomName();
							Location loc = e.getEntity().getLocation();
							if (Utils.isSpigot() && sender instanceof Player) {
								net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent(Utils.convertString("&3- &"+name+" &"+healthString+" &7- &6("+loc.getWorld().getName()+") &f"+loc.getBlockX()+' '+loc.getBlockY()+' '+loc.getBlockZ()));
								message.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.SUGGEST_COMMAND, "/tp @s "+loc.getBlockX()+' '+loc.getBlockY()+' '+loc.getBlockZ()));
								((Player) sender).spigot().sendMessage(message);
							} else
								sender.sendMessage(Utils.convertString("&3- &"+name+" &"+healthString+" &7- &6("+loc.getWorld().getName()+") &f"+loc.getBlockX()+' '+loc.getBlockY()+' '+loc.getBlockZ()));
						}
					}
					return true;
				}
				sender.sendMessage(Languages.prefix+Utils.convertString("&aCurrently spawned entities"));
				Map<CustomEntityType, Integer> tempMap = new HashMap<>();
				for (CustomEntity e : CustomEntity.handler.getList()) {
					if (tempMap.containsKey(e.getType()))
						tempMap.replace(e.getType(), tempMap.get(e.getType()) + 1);
					else
						tempMap.put(e.getType(), 1);
				}
				for (Map.Entry<CustomEntityType, Integer> entry : tempMap.entrySet())
					sender.sendMessage(Utils.convertString("&3- &"+entry.getKey().getColChar()+entry.getKey().species+" &7- &f"+entry.getValue()));
				return true;
			} else if (args[1].equalsIgnoreCase("kill")) {
				if (args.length >= 3) {
					CustomEntityType type = CustomEntityType.getCustomEntityType(args[2].toLowerCase());
					if (type == null) {
						sender.sendMessage(Utils.convertString("&cNo such species type '"+args[2]+"'"));
						return true;
					}
					String species = type.species;
					for (CustomEntity e : CustomEntity.handler.getList())
						if (e.getEntity() != null && e.getSpecies().equals(species))
							e.getEntity().remove();
					sender.sendMessage(Languages.prefix+Utils.convertString("&eKilled all &"+type.getColChar()+type.species)+"&e!");
					return true;
				}
				for (CustomEntity e : CustomEntity.handler.getList())
					if (e.getEntity() != null)
						e.getEntity().remove();
				sender.sendMessage(Languages.prefix+Utils.convertString("&eKilled all custom entities!"));
				return true;
			}
		} else
			sender.sendMessage(globalUsage);
		return true;
	}
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		final List<String> list = new ArrayList<>();
		if (args.length == 1) {
			final List<String> tempList = new ArrayList<>();
			if (sender.hasPermission("deadlydisasters.start"))
				tempList.add("start");
			if (sender.hasPermission("deadlydisasters.help"))
				tempList.add("help");
			if (sender.hasPermission("deadlydisasters.modify"))
				tempList.addAll(Arrays.asList("enable","disable","mintimer","reload","difficulty","language","catalog","config","favor","dislike"));
			if (sender.hasPermission("deadlydisasters.whitelist"))
				tempList.add("whitelist");
			if (sender.hasPermission("deadlydisasters.listplayer"))
				tempList.add("listplayer");
			if (sender.hasPermission("deadlydisasters.summon"))
				tempList.add("summon");
			if (sender.hasPermission("deadlydisasters.give"))
				tempList.add("give");
			if (sender.hasPermission("deadlydisasters.event"))
				tempList.add("event");
			if (sender.hasPermission("deadlydisasters.entities"))
				tempList.add("entities");
			StringUtil.copyPartialMatches(args[0], tempList, list);
			Collections.sort(list);
			return list;
		} else if (args.length == 2) {
			if (args[0].equalsIgnoreCase("reload"))
				return list;
			if ((args[0].equalsIgnoreCase("mintimer") || args[0].equalsIgnoreCase("difficulty")) && sender.hasPermission("deadlydisasters.modify")) {
				List<String> extraCompletions = Bukkit.getServer().getWorlds().stream().map(p -> p.getName()).collect(Collectors.toList());
				extraCompletions.addAll(Arrays.asList("THIS_WORLD","ALL_WORLDS"));
				StringUtil.copyPartialMatches(args[1], extraCompletions, list);
				Collections.sort(list);
			}
			if ((args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("disable")) && sender.hasPermission("deadlydisasters.modify")) {
				List<String> extraCompletions = disasterNames;
				extraCompletions.addAll(Arrays.asList("randomdisasters","maxlevels","eventmsg"));
				StringUtil.copyPartialMatches(args[1], extraCompletions, list);
				Collections.sort(list);
				return list;
			}
			if ((args[0].equalsIgnoreCase("start") && sender.hasPermission("deadlydisasters.start")) || (args[0].equalsIgnoreCase("help") && sender.hasPermission("deadlydisasters.help"))
					|| ((args[0].equalsIgnoreCase("favor") || args[0].equalsIgnoreCase("dislike")) && sender.hasPermission("deadlydisasters.modify"))) {
				StringUtil.copyPartialMatches(args[1], disasterNames, list);
				Collections.sort(list);
				return list;
			}
			if (args[0].equalsIgnoreCase("summon") && sender.hasPermission("deadlydisasters.summon")) {
				StringUtil.copyPartialMatches(args[1], Stream.of(CustomEntityType.values()).map(e -> e.species).collect(Collectors.toList()), list);
				Collections.sort(list);
				return list;
			}
			if (args[0].equalsIgnoreCase("give") && sender.hasPermission("deadlydisasters.give")) {
				StringUtil.copyPartialMatches(args[1], ItemsHandler.allItems.keySet(), list);
				Collections.sort(list);
				return list;
			}
			if (args[0].equalsIgnoreCase("language") && sender.hasPermission("deadlydisasters.modify")) {
				StringUtil.copyPartialMatches(args[1], Arrays.asList("english","中文","русский","češtiny","français"), list);
				Collections.sort(list);
				return list;
			}
			if (args[0].equalsIgnoreCase("whitelist") && sender.hasPermission("deadlydisasters.whitelist")) {
				StringUtil.copyPartialMatches(args[1], Arrays.asList("add","remove"), list);
				Collections.sort(list);
				return list;
			}
			if (args[0].equalsIgnoreCase("listplayer") && sender.hasPermission("deadlydisasters.listplayer")) {
				StringUtil.copyPartialMatches(args[1], Bukkit.getServer().getOnlinePlayers().stream().map(p -> p.getName()).collect(Collectors.toList()), list);
				Collections.sort(list);
				return list;
			}
			if (args[0].equalsIgnoreCase("config") && sender.hasPermission("deadlydisasters.modify")) {
				StringUtil.copyPartialMatches(args[1], Arrays.asList("save","swap","delete","view"), list);
				Collections.sort(list);
				return list;
			}
			if (args[0].equalsIgnoreCase("entities")) {
				StringUtil.copyPartialMatches(args[1], Arrays.asList("list","kill"), list);
				Collections.sort(list);
				return list;
			}
		} else if (args.length == 3) {
			if (args[0].equalsIgnoreCase("start") && sender.hasPermission("deadlydisasters.start")) {
				list.addAll(Arrays.asList("1","2","3","4","5","6"));
				return list;
			}
			if (args[0].equalsIgnoreCase("difficulty") && sender.hasPermission("deadlydisasters.modify")) {
				StringUtil.copyPartialMatches(args[2], Arrays.stream(DifficultyLevel.values()).map(e -> e.name()).collect(Collectors.toList()), list);
				Collections.sort(list);
				return list;
			}
			if ((args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("disable")) && sender.hasPermission("deadlydisasters.modify")) {
				List<String> extraCompletions = Bukkit.getServer().getWorlds().stream().map(p -> p.getName()).collect(Collectors.toList());
				extraCompletions.addAll(Arrays.asList("THIS_WORLD","ALL_WORLDS"));
				StringUtil.copyPartialMatches(args[2], extraCompletions, list);
				Collections.sort(list);
				return list;
			}
			if ((args[0].equalsIgnoreCase("give") && sender.hasPermission("deadlydisasters.give")) || (args[0].equalsIgnoreCase("whitelist") && sender.hasPermission("deadlydisasters.whitelist"))) {
				StringUtil.copyPartialMatches(args[2], Bukkit.getServer().getOnlinePlayers().stream().map(p -> p.getName()).collect(Collectors.toList()), list);
				Collections.sort(list);
				return list;
			}
			if (args[0].equalsIgnoreCase("config") && (args[1].equalsIgnoreCase("swap") || args[1].equalsIgnoreCase("delete")) && sender.hasPermission("deadlydisasters.modify")) {
				StringUtil.copyPartialMatches(args[2], plugin.cfgSwapper.configTemplates, list);
				Collections.sort(list);
				return list;
			}
			if (args[0].equalsIgnoreCase("entities")) {
				StringUtil.copyPartialMatches(args[2], CustomEntityType.speciesList, list);
				Collections.sort(list);
				return list;
			}
		} else if (args.length == 4) {
			if (args[0].equalsIgnoreCase("start") && sender.hasPermission("deadlydisasters.start")) {
				StringUtil.copyPartialMatches(args[3], Bukkit.getServer().getOnlinePlayers().stream().map(p -> p.getName()).collect(Collectors.toList()), list);
				Collections.sort(list);
				return list;
			}
			if (args[0].equalsIgnoreCase("whitelist") && sender.hasPermission("deadlydisasters.whitelist")) {
				StringUtil.copyPartialMatches(args[3], Bukkit.getServer().getWorlds().stream().map(p -> p.getName()).collect(Collectors.toList()), list);
				Collections.sort(list);
				return list;
			}
		} else if (args.length == 5) {
			if (args[0].equalsIgnoreCase("start") && sender.hasPermission("deadlydisasters.start")) {
				StringUtil.copyPartialMatches(args[4], Arrays.asList("true","false"), list);
				Collections.sort(list);
				return list;
			}
		} else if (args.length == 6) {
			if (args[0].equalsIgnoreCase("summon") && sender.hasPermission("deadlydisasters.summon")) {
				StringUtil.copyPartialMatches(args[5], Bukkit.getServer().getWorlds().stream().map(p -> p.getName()).collect(Collectors.toList()), list);
				Collections.sort(list);
				return list;
			}
		}
		return list;
	}
}
