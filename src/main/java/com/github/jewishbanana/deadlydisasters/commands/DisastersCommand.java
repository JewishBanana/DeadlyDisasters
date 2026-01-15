package com.github.jewishbanana.deadlydisasters.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.WorldWrapper;
import com.github.jewishbanana.deadlydisasters.disasters.Disaster;
import com.github.jewishbanana.deadlydisasters.disasters.Disaster.RegeneratingTask;
import com.github.jewishbanana.deadlydisasters.disasters.DisasterRegistry;
import com.github.jewishbanana.deadlydisasters.events.DisasterStartEvent;
import com.github.jewishbanana.deadlydisasters.events.DisasterStartEvent.DisasterStartReason;
import com.github.jewishbanana.deadlydisasters.events.DisasterStopEvent.DisasterStopReason;
import com.github.jewishbanana.deadlydisasters.listeners.BlockRegenHandler;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class DisastersCommand implements CommandExecutor, TabCompleter {
	
	private static int regenBlocksPerTick;
	private static int fastRegenBlocksPerTick;
	public static void reload() {
		regenBlocksPerTick = DataUtils.getMainConfigInt("regeneration.regen_blocks_per_tick");
		fastRegenBlocksPerTick = DataUtils.getMainConfigInt("regeneration.fast_regen_blocks_per_tick");
	}
	
	private final Main plugin;
	private final String usage = Utils.convertString("&cUsage: /disasters <help|start|stop|regenerate|fastRegenerate|config|blacklist|timers>...");
	private final Map<String, ConfigSettingOption> configSettings = Map.of(
			"targeting", 
			new ConfigSettingOption("world.", Set.of("DISABLED", "INDIVIDUAL", "GLOBAL"), container -> {
				switch (container.value.toUpperCase()) {
				case "DISABLED":
				case "INDIVIDUAL":
				case "GLOBAL":
					return true;
				}
				container.sender.sendMessage(Utils.convertString("&cThe targeting type '"+container.value+"' is not valid! You can only select one of the valid targeting options."));
				return false;
			}),
			"minimum_time", 
			new ConfigSettingOption("world.", null, container -> {
				try {
					int value = Integer.parseInt(container.value);
					if (container.wrapper.maximumTime < value)
						container.wrapper.getConfig().set("maximum_time", value);
					return true;
				} catch (NumberFormatException ex) {}
				container.sender.sendMessage(Utils.convertString("&cThe value '"+container.value+"' is not valid! Expected an integer value."));
				return false;
			}),
			"maximum_time", 
			new ConfigSettingOption("world.", null, container -> {
				try {
					int value = Integer.parseInt(container.value);
					if (container.wrapper.minimumTime > value)
						container.wrapper.getConfig().set("minimumTime", value);
					return true;
				} catch (NumberFormatException ex) {}
				container.sender.sendMessage(Utils.convertString("&cThe value '"+container.value+"' is not valid! Expected an integer value."));
				return false;
			}),
			"disaster_offset", 
			new ConfigSettingOption("world.", null, container -> {
				try {
					Float.parseFloat(container.value);
					return true;
				} catch (NumberFormatException ex) {}
				container.sender.sendMessage(Utils.convertString("&cThe value '"+container.value+"' is not valid! Expected a decimal value."));
				return false;
			}),
			"shared_disaster_radius", 
			new ConfigSettingOption("world.", null, container -> {
				try {
					Float.parseFloat(container.value);
					return true;
				} catch (NumberFormatException ex) {}
				container.sender.sendMessage(Utils.convertString("&cThe value '"+container.value+"' is not valid! Expected a decimal value."));
				return false;
			}),
			"broadcast_disasters", 
			new ConfigSettingOption("world.", Set.of("true", "false"), container -> {
				if (container.value.equals("true") || container.value.equals("false"))
					return true;
				container.sender.sendMessage(Utils.convertString("&cThe value '"+container.value+"' is not valid! Expected a true/false value."));
				return false;
			}),
			"disaster_tips", 
			new ConfigSettingOption("world.", Set.of("true", "false"), container -> {
				if (container.value.equals("true") || container.value.equals("false"))
					return true;
				container.sender.sendMessage(Utils.convertString("&cThe value '"+container.value+"' is not valid! Expected a true/false value."));
				return false;
			}));

	public DisastersCommand(Main plugin) {
		this.plugin = plugin;
		
		plugin.getCommand("disasters").setExecutor(this);
	}
	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(Utils.convertString("&cUsage: /disasters help"));
			return true;
		}
		String firstArg = args[0].toLowerCase();
		if (sender instanceof Player && !sender.hasPermission("deadlydisasters."+firstArg)) {
			sender.sendMessage(Utils.convertString(DataUtils.getLanguageString("messages.commands.permission_error")));
			return true;
		}
		switch (firstArg) {
		case "help" -> {
			if (args.length < 2) {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters help <command>"));
				return true;
			}
			switch (args[1].toLowerCase()) {
			case "start" -> sender.sendMessage(Utils.convertString(DataUtils.getLanguageString("messages.commands.help.start")));
			case "stop" -> sender.sendMessage(Utils.convertString(DataUtils.getLanguageString("messages.commands.help.stop")));
			case "regenerate" -> sender.sendMessage(Utils.convertString(DataUtils.getLanguageString("messages.commands.help.regenerate")));
			case "fastregenerate" -> sender.sendMessage(Utils.convertString(DataUtils.getLanguageString("messages.commands.help.fastRegenerate")));
			case "config" -> {
				if (args.length < 3) {
					sender.sendMessage(Utils.convertString(DataUtils.getLanguageString("messages.commands.help.config.config")));
					return true;
				}
				switch (args[2].toLowerCase()) {
				case "reload" -> sender.sendMessage(Utils.convertString(DataUtils.getLanguageString("messages.commands.help.config.reload")));
				case "set" -> sender.sendMessage(Utils.convertString(DataUtils.getLanguageString("messages.commands.help.config.set")));
				case "enable" -> sender.sendMessage(Utils.convertString(DataUtils.getLanguageString("messages.commands.help.config.enable")));
				case "disable" -> sender.sendMessage(Utils.convertString(DataUtils.getLanguageString("messages.commands.help.config.disable")));
				case "setting" -> sender.sendMessage(Utils.convertString(DataUtils.getLanguageString("messages.commands.help.config.setting")));
				case "list" -> sender.sendMessage(Utils.convertString(DataUtils.getLanguageString("messages.commands.help.config.list")));
				default -> sender.sendMessage(Utils.convertString(DataUtils.getLanguageString("messages.commands.help.unrecognized")+" '"+args[2]+"'!"));
				}
			}
			case "blacklist" -> sender.sendMessage(Utils.convertString(DataUtils.getLanguageString("messages.commands.help.blacklist")));
			case "timers" -> sender.sendMessage(Utils.convertString(DataUtils.getLanguageString("messages.commands.help.timers")));
			default -> sender.sendMessage(Utils.convertString(DataUtils.getLanguageString("messages.commands.help.unrecognized")+" '"+args[1]+"'!"));
			}
			return true;
		}
		case "start" -> {
			if (args.length < 2) {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters start <disaster> [level] [player|x y z] [world]"));
				return true;
			}
			DisasterRegistry register = DisasterRegistry.getRegistry(args[1]);
			if (register == null) {
				sender.sendMessage(Utils.convertString("&cThere is no such disaster with the name '"+args[1]+"'!"));
				return true;
			}
			Location loc = null;
			Player player = null;
			int level = 1;
			if (args.length > 2) {
				try {
					level = Integer.parseInt(args[2]);
				} catch (NumberFormatException e) {
					sender.sendMessage(Utils.convertString("&cInvalid integer for level '"+args[2]+"'!"));
					return true;
				}
				if (args.length < 4) {
					if (!(sender instanceof Player executor)) {
						sender.sendMessage(Utils.convertString("&cUsage: /disasters start <disaster> <level> <player|x y z> [world]"));
						return true;
					}
					loc = executor.getLocation();
					player = executor;
				} else if (args.length == 4) {
					Player temp = getOnlinePlayer(args[3]);
					if (temp == null) {
						sender.sendMessage(Utils.convertString("&cCould not find player '"+args[3]+"'!"));
						return true;
					}
					loc = temp.getLocation();
					player = temp;
				} else {
					World world = null;
					if (args.length > 6) {
						world = Bukkit.getWorld(args[6]);
						if (world == null) {
							sender.sendMessage(Utils.convertString("&cCould not find world '"+args[6]+"'!"));
							return true;
						}
					} else if (sender instanceof Player executor) {
						world = executor.getWorld();
					} else {
						sender.sendMessage(Utils.convertString("&cUsage: /disasters start <disaster> <level> <player|x y z> [world]"));
						return true;
					}
					try {
						loc = new Location(world, Double.parseDouble(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]));
					} catch (NumberFormatException e) {
						sender.sendMessage(Utils.convertString("&cInvalid coordinates entered!"));
						return true;
					}
				}
			} else {
				if (!(sender instanceof Player executor)) {
					sender.sendMessage(Utils.convertString("&cUsage: /disasters start <disaster> <level> <player|x y z> [world]"));
					return true;
				}
				loc = executor.getLocation();
				player = executor;
			}
			WorldWrapper wrapper = WorldWrapper.getWorldWrapper(loc.getWorld());
			if (wrapper.disabledDisasters.contains(register)) {
				sender.sendMessage(Utils.convertString(DataUtils.getLanguageString("messages.commands.disabled_disaster")));
				return true;
			}
			Disaster disaster = register.createDisaster(loc, player, level);
			Location adjustment = disaster.findPossiblePosition(loc);
			if (adjustment != null)
				disaster.setLocation(adjustment);
			DisasterStartEvent event = new DisasterStartEvent(disaster, DisasterStartReason.COMMAND);
			Bukkit.getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				sender.sendMessage(Utils.convertString("&cThe disaster was halted from starting by a third-party plugin!"));
				return true;
			}
			disaster.broadcastDisaster();
			disaster.start();
			return true;
		}
		case "stop" -> {
			Class<?> disasterClass = null;
			if (args.length > 1) {
				DisasterRegistry register = DisasterRegistry.getRegistry(args[1]);
				if (register == null) {
					sender.sendMessage(Utils.convertString("&cThere is no such disaster with the name '"+args[1]+"'!"));
					return true;
				}
				disasterClass = register.getRegisteredClass();
			}
			World world = null;
			if (args.length > 2) {
				world = Bukkit.getWorld(args[2]);
				if (world == null) {
					sender.sendMessage(Utils.convertString("&cCould not find world '"+args[2]+"'!"));
					return true;
				}
			}
			final List<Disaster> copyList = new ArrayList<>(Disaster.onGoingDisasters);
			Iterator<Disaster> iterator = copyList.iterator();
			int stopped = 0;
			while (iterator.hasNext()) {
				Disaster temp = iterator.next();
				if (disasterClass != null && !temp.getClass().equals(disasterClass))
					continue;
				if (world != null && !temp.getLocation().getWorld().equals(world))
					continue;
				if (!temp.stop(DisasterStopReason.COMMAND))
					sender.sendMessage(Utils.convertString(Utils.prefix+"&cFailed to stop disaster "+temp.getDisplayName()+" &cat &e"+temp.getLocation().getBlockX()+' '+temp.getLocation().getBlockY()+' '+temp.getLocation().getBlockZ()+" &d("+temp.getLocation().getWorld().getName()+")&c. A third-party plugin prevented this disaster from being stopped!"));
				else
					++stopped;
			}
			sender.sendMessage(Utils.convertString(Utils.prefix+"&bSuccessfully stopped &a"+stopped+" &bdisaster(s)!"));
			return true;
		}
		case "regenerate" -> {
			return regenerationTask(args, sender, true);
		}
		case "fastregenerate" -> {
			return regenerationTask(args, sender, false);
		}
		case "config" -> {
			if (args.length == 1) {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters config <reload|set|enable|disable|setting|list>"));
				return true;
			}
			switch (args[1].toLowerCase()) {
			case "reload" -> {
				plugin.reloadConfig();
				plugin.reload();
				sender.sendMessage(Utils.convertString(Utils.prefix+"&bSuccessfully reloaded all configs!"));
				return true;
			}
			case "set" -> {
				if (args.length < 4) {
					sender.sendMessage(Utils.convertString("&cUsage: /disasters config set <world> <config>"));
					return true;
				}
				File file = new File(plugin.getDataFolder().getAbsolutePath(), "worldConfigs/"+args[3]+".yml");
				if (!file.exists()) {
					sender.sendMessage(Utils.convertString("&cThere is no such disaster world config file '"+args[3]+"!"));
					return true;
				}
				World[] worlds = getWorldSelection(args[2], sender);
				if (worlds == null) {
					sender.sendMessage(Utils.convertString("&cThere is no such world '"+args[2]+"'!"));
					return true;
				}
				if (worlds.length > 1) {
					DataUtils.writeToDataFile(config -> {
						Bukkit.getWorlds().forEach(world -> {
							try {
								config.set("worlds."+world.getUID().toString()+".config", args[3]);
							} catch (Exception e) {
								Utils.sendExceptionLog(e);
							}
						});
					});
					WorldWrapper.init();
					sender.sendMessage(Utils.convertString(Utils.prefix+"&bSuccessfully set the disaster config file for all worlds to &d'"+args[3]+"'&b!"));
					return true;
				} else {
					try {
						DataUtils.writeToDataFile(config -> {
							config.set("worlds."+worlds[0].getUID().toString()+".config", args[3]);
						});
						WorldWrapper.initWorld(worlds[0]);
						sender.sendMessage(Utils.convertString(Utils.prefix+"&bSuccessfully set the disaster config file for world &a'"+worlds[0].getName()+"' &bto &d'"+args[3]+"'&b!"));
					} catch (Exception e) {
						Utils.sendExceptionLog(e);
					}
					return true;
				}
			}
			case "enable" -> {
				if (args.length < 3) {
					sender.sendMessage(Utils.convertString("&cUsage: /disasters config enable <disaster> [world]"));
					return true;
				}
				if (!WorldWrapper.disasterCategories.containsKey(args[2].toUpperCase())) {
					DisasterRegistry enableDisaster = DisasterRegistry.getRegistry(args[2]);
					if (enableDisaster == null) {
						sender.sendMessage(Utils.convertString("&cThere is no such disaster with the name '"+args[2]+"'!"));
						return true;
					}
				}
				World[] worlds = null;
				if (args.length < 4) {
					if (sender instanceof Player player)
						worlds = new World[] { player.getWorld() };
					else {
						sender.sendMessage(Utils.convertString("&cUsage: /disasters config enable <disaster> <world>"));
						return true;
					}
				}
				if (worlds == null) {
					worlds = getWorldSelection(args[3], sender);
					if (worlds == null) {
						sender.sendMessage(Utils.convertString("&cThere is no such world '"+args[3]+"'!"));
						return true;
					}
				}
				if (worlds.length > 1) {
					Bukkit.getWorlds().forEach(world -> {
						WorldWrapper wrapper = WorldWrapper.getWorldWrapper(world);
						List<String> list = DataUtils.getConfigStringList(wrapper.getConfig(), wrapper.getConfigName(), "world.disabled_disasters");
						if (!list.contains(args[2]))
							return;
						list.remove(args[2]);
						wrapper.getConfig().set("world.disabled_disasters", list);
						wrapper.saveAndReload();
					});
					sender.sendMessage(Utils.convertString(Utils.prefix+"&bThe disaster/category &6'"+args[2]+"' &bhas been &a&lenabled &bfor all worlds!"));
					return true;
				} else {
					WorldWrapper wrapper = WorldWrapper.getWorldWrapper(worlds[0]);
					List<String> list = DataUtils.getConfigStringList(wrapper.getConfig(), wrapper.getConfigName(), "world.disabled_disasters");
					if (!list.contains(args[2])) {
						sender.sendMessage(Utils.convertString(Utils.prefix+"&eThe disaster/category &6'"+args[2]+"' &eis already &a&lenabled &ein world config &6'"+wrapper.getConfigName()+"' &efor world &d'"+worlds[0].getName()+"'&e!"));
						return true;
					}
					list.remove(args[2]);
					wrapper.getConfig().set("world.disabled_disasters", list);
					wrapper.saveAndReload();
					sender.sendMessage(Utils.convertString(Utils.prefix+"&bThe disaster/category &6'"+args[2]+"' &bhas been &a&lenabled &bin world config &a'"+wrapper.getConfigName()+"' &bfor world &d'"+worlds[0].getName()+"'&b!"));
					return true;
				}
			}
			case "disable" -> {
				if (args.length < 3) {
					sender.sendMessage(Utils.convertString("&cUsage: /disasters config disable <disaster> [world]"));
					return true;
				}
				if (!WorldWrapper.disasterCategories.containsKey(args[2].toUpperCase())) {
					DisasterRegistry enableDisaster = DisasterRegistry.getRegistry(args[2]);
					if (enableDisaster == null) {
						sender.sendMessage(Utils.convertString("&cThere is no such disaster with the name '"+args[2]+"'!"));
						return true;
					}
				}
				World[] worlds = null;
				if (args.length < 4) {
					if (sender instanceof Player player)
						worlds = new World[] { player.getWorld() };
					else {
						sender.sendMessage(Utils.convertString("&cUsage: /disasters config disable <disaster> <world>"));
						return true;
					}
				}
				if (worlds == null) {
					worlds = getWorldSelection(args[3], sender);
					if (worlds == null) {
						sender.sendMessage(Utils.convertString("&cThere is no such world '"+args[3]+"'!"));
						return true;
					}
				}
				if (worlds.length > 1) {
					Bukkit.getWorlds().forEach(world -> {
						WorldWrapper wrapper = WorldWrapper.getWorldWrapper(world);
						List<String> list = DataUtils.getConfigStringList(wrapper.getConfig(), wrapper.getConfigName(), "world.disabled_disasters");
						if (list.contains(args[2]))
							return;
						list.add(args[2]);
						wrapper.getConfig().set("world.disabled_disasters", list);
						wrapper.saveAndReload();
					});
					sender.sendMessage(Utils.convertString(Utils.prefix+"&bThe disaster/category &6'"+args[2]+"' &bhas been &c&ldisabled &bfor all worlds!"));
					return true;
				} else {
					WorldWrapper wrapper = WorldWrapper.getWorldWrapper(worlds[0]);
					List<String> list = DataUtils.getConfigStringList(wrapper.getConfig(), wrapper.getConfigName(), "world.disabled_disasters");
					if (list.contains(args[2])) {
						sender.sendMessage(Utils.convertString(Utils.prefix+"&eThe disaster/category &6'"+args[2]+"' &eis already &c&ldisabled &ein world config &6'"+wrapper.getConfigName()+"' &efor world &d'"+worlds[0].getName()+"'&e!"));
						return true;
					}
					list.add(args[2]);
					wrapper.getConfig().set("world.disabled_disasters", list);
					wrapper.saveAndReload();
					sender.sendMessage(Utils.convertString(Utils.prefix+"&bThe disaster/category &6'"+args[2]+"' &bhas been &c&ldisabled &bin world config &a'"+wrapper.getConfigName()+"' &bfor world &d'"+worlds[0].getName()+"'&b!"));
					return true;
				}
			}
			case "setting" -> {
				if (args.length < 4) {
					sender.sendMessage(Utils.convertString("&cUsage: /disasters config setting <option> <value> [world]"));
					return true;
				}
				ConfigSettingOption option = configSettings.get(args[2].toLowerCase());
				if (option == null) {
					sender.sendMessage(Utils.convertString("&cThe setting '"+args[2]+"' does not exist!"));
					return true;
				}
				World[] worlds = null;
				if (args.length < 5) {
					if (sender instanceof Player player)
						worlds = new World[] { player.getWorld() };
					else {
						sender.sendMessage(Utils.convertString("&cUsage: /disasters config setting <option> <value> <world>"));
						return true;
					}
				} else {
					worlds = getWorldSelection(args[4], sender);
					if (worlds == null) {
						sender.sendMessage(Utils.convertString("&cThere is no such world '"+args[4]+"'!"));
						return true;
					}
				}
				if (worlds.length > 1) {
					for (World world : Bukkit.getWorlds()) {
						WorldWrapper wrapper = WorldWrapper.getWorldWrapper(world);
						if (!option.function.apply(new ConfigSettingContainer(args[3], sender, wrapper)))
							return true;
						wrapper.getConfig().set(option.configSection + args[2].toLowerCase(), args[3]);
						wrapper.saveAndReload();
					}
					sender.sendMessage(Utils.convertString(Utils.prefix+"&aSuccessfully set the setting &6'"+args[2].toLowerCase()+"' &ato the value &b'"+args[3]+"' &afor all worlds configs!"));
					return true;
				} else {
					WorldWrapper wrapper = WorldWrapper.getWorldWrapper(worlds[0]);
					if (!option.function.apply(new ConfigSettingContainer(args[3], sender, wrapper)))
						return true;
					wrapper.getConfig().set(option.configSection + args[2].toLowerCase(), args[3]);
					wrapper.saveAndReload();
					sender.sendMessage(Utils.convertString(Utils.prefix+"&aSuccessfully set the setting &6'"+args[2].toLowerCase()+"' &ato the value &b'"+args[3]+"' &afor the world config &d'"+wrapper.getConfigName()+"'&a!"));
					return true;
				}
			}
			case "list" -> {
				StringBuilder builder = new StringBuilder(Utils.prefix+"&bAll worlds and their current selected disaster world configs:");
				Bukkit.getWorlds().forEach(world -> {
					builder.append("\n&3- &a"+world.getName()+" &d("+DataUtils.getDataFileString("worlds."+world.getUID().toString()+".config", "&cERROR")+"&d)");
				});
				sender.sendMessage(Utils.convertString(builder.toString()));
				return true;
			}
			default -> {}
			}
			sender.sendMessage(Utils.convertString("&cUsage: /disasters config <reload|set|enable|disable|setting|list>"));
			return true;
		}
		case "blacklist" -> {
			if (args.length < 3) {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters blacklist <add|remove> <player> [world]"));
				return true;
			}
			new BukkitRunnable() {
				@Override
				public void run() {
					OfflinePlayer player = getOnlinePlayer(args[2]);
					if (player == null)
						player = Bukkit.getOfflinePlayer(args[2]);
					UUID uuid = player.getUniqueId();
					new BukkitRunnable() {
						@Override
						public void run() {
							if (uuid == null) {
								sender.sendMessage(Utils.convertString("&cCould not find player '"+args[2]+"'!"));
								return;
							}
							World[] worlds = null;
							if (args.length < 4) {
								if (sender instanceof Player p)
									worlds = new World[] { p.getWorld() };
								else {
									sender.sendMessage(Utils.convertString("&cUsage: /disasters blacklist <add|remove> <player> <world>"));
									return;
								}
							} else {
								worlds = getWorldSelection(args[3], sender);
								if (worlds == null) {
									sender.sendMessage(Utils.convertString("&cThere is no such world '"+args[3]+"'!"));
									return;
								}
							}
							switch (args[1].toLowerCase()) {
							case "add" -> {
								if (worlds.length > 1) {
									DataUtils.writeToDataFile(file -> {
										Bukkit.getWorlds().forEach(world -> {
											WorldWrapper wrapper = WorldWrapper.getWorldWrapper(world);
											List<String> list = DataUtils.getDataFileStringList("worlds."+world.getUID().toString()+".player_blacklist");
											list.add(uuid.toString());
											file.set("worlds."+world.getUID().toString()+".player_blacklist", list);
											if (wrapper.blacklistedPlayers == null)
												wrapper.blacklistedPlayers = new HashSet<>();
											wrapper.blacklistedPlayers.add(uuid);
										});
									});
									sender.sendMessage(Utils.convertString(Utils.prefix+"&bBlacklisted player &d'"+args[2]+"' &bfor all worlds! &7(Disasters will NOT naturally occur on this player)."));
									return;
								}
								WorldWrapper wrapper = WorldWrapper.getWorldWrapper(worlds[0]);
								List<String> list = DataUtils.getDataFileStringList("worlds."+worlds[0].getUID().toString()+".player_blacklist");
								if (list.contains(uuid.toString())) {
									sender.sendMessage(Utils.convertString(Utils.prefix+"&eThe player &d'"+args[2]+"' &eis already blacklisted on world &a'"+worlds[0].getName()+"'&e!"));
									return;
								}
								list.add(uuid.toString());
								UUID worldID = worlds[0].getUID();
								DataUtils.writeToDataFile(file -> file.set("worlds."+worldID.toString()+".player_blacklist", list));
								if (wrapper.blacklistedPlayers == null)
									wrapper.blacklistedPlayers = new HashSet<>();
								wrapper.blacklistedPlayers.add(uuid);
								sender.sendMessage(Utils.convertString(Utils.prefix+"&bBlacklisted player &d'"+args[2]+"' &bfor world &a'"+worlds[0].getName()+"'&b! &7(Disasters will NOT naturally occur on this player)."));
								return;
							}
							case "remove" -> {
								if (worlds.length > 1) {
									DataUtils.writeToDataFile(file -> {
										Bukkit.getWorlds().forEach(world -> {
											WorldWrapper wrapper = WorldWrapper.getWorldWrapper(world);
											List<String> list = DataUtils.getDataFileStringList("worlds."+world.getUID().toString()+".player_blacklist");
											if (list.remove(uuid.toString()))
												file.set("worlds."+world.getUID().toString()+".player_blacklist", list);
											if (wrapper != null) {
												wrapper.blacklistedPlayers.remove(uuid);
												if (wrapper.blacklistedPlayers.isEmpty())
													wrapper.blacklistedPlayers = null;
											}
										});
									});
									sender.sendMessage(Utils.convertString(Utils.prefix+"&bRemoved player &d'"+args[2]+"' &bfrom the blacklist for all worlds! &7(Disasters WILL naturally occur on this player)."));
									return;
								}
								WorldWrapper wrapper = WorldWrapper.getWorldWrapper(worlds[0]);
								List<String> list = DataUtils.getDataFileStringList("worlds."+worlds[0].getUID().toString()+".player_blacklist");
								if (!list.contains(uuid.toString())) {
									sender.sendMessage(Utils.convertString(Utils.prefix+"&eThe player &d'"+args[2]+"' &eis not blacklisted on world &a'"+worlds[0].getName()+"'&e!"));
									return;
								}
								list.remove(uuid.toString());
								UUID worldID = worlds[0].getUID();
								DataUtils.writeToDataFile(file -> file.set("worlds."+worldID.toString()+".player_blacklist", list));
								if (wrapper != null) {
									wrapper.blacklistedPlayers.remove(uuid);
									if (wrapper.blacklistedPlayers.isEmpty())
										wrapper.blacklistedPlayers = null;
								}
								sender.sendMessage(Utils.convertString(Utils.prefix+"&bRemoved player &d'"+args[2]+"' &bfrom the blacklist for world &a'"+worlds[0].getName()+"'&b! &7(Disasters WILL naturally occur on this player)."));
								return;
							}
							default -> {
								sender.sendMessage(Utils.convertString("&cUsage: /disasters config <reload|set|enable|disable|setting|list>"));
								return;
							}
							}
						}
					}.runTask(plugin);
				}
			}.runTaskAsynchronously(plugin);
			return true;
		}
		case "timers" -> {
			if (args.length < 2) {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters timers <listworlds|listplayer> [player]"));
				return true;
			}
			switch (args[1].toLowerCase()) {
			case "listworlds" -> {
				StringBuilder builder = new StringBuilder(Utils.prefix+"&aAll worlds and their global timers listed:");
				Bukkit.getWorlds().forEach(world -> {
					WorldWrapper wrapper = WorldWrapper.getWorldWrapper(world);
					Integer timer = wrapper.targetingMode == 2 ? plugin.selector.worldTimers.get(world.getUID()) : null;
					builder.append("\n&3- &d"+world.getName()+" &7- &f&l" + (timer == null ? "&c&lN/A" : timer));
				});
				sender.sendMessage(Utils.convertString(builder.toString()));
				return true;
			}
			case "listplayer" -> {
				if (args.length < 3) {
					sender.sendMessage(Utils.convertString("&cUsage: /disasters timers listplayer <player>"));
					return true;
				}
				Player target = getOnlinePlayer(args[2]);
				if (target == null) {
					sender.sendMessage(Utils.convertString("&cCould not find player '"+args[2]+"'!"));
					return true;
				}
				StringBuilder builder = new StringBuilder(Utils.prefix+"&aAll world timers for &6"+target.getDisplayName()+" &alisted:");
				Bukkit.getWorlds().forEach(world -> {
					Map<UUID, Integer> worldMap = plugin.selector.playerTimers.get(world.getUID());
					Integer timer = worldMap != null ? worldMap.get(target.getUniqueId()) : null;
					builder.append("\n&3- &d"+world.getName()+" &7- " + (target.getWorld().equals(world) ? "&a&l" : "&f&l") + (timer == null ? "&c&lN/A" : timer + " &7(seconds till disaster)"));
				});
				sender.sendMessage(Utils.convertString(builder.toString()));
				return true;
			}
			default -> {
				sender.sendMessage(Utils.convertString("&cUsage: /disasters timers <listworlds|listplayer> [player]"));
				return true;
			}
			}
		}
		default -> {}
		}
		if (sender instanceof Player && !sender.hasPermission("deadlydisasters.*")) {
			sender.sendMessage(Utils.convertString(DataUtils.getLanguageString("messages.commands.permission_error")));
			return true;
		}
		sender.sendMessage(usage);
		return true;
	}
	private boolean regenerationTask(String[] args, CommandSender sender, boolean force) {
		World[] worlds = null;
		if (args.length > 1) {
			worlds = getWorldSelection(args[1], sender);
			if (worlds == null) {
				sender.sendMessage(Utils.convertString("&cCould not find world '"+args[1]+"'!"));
				return true;
			}
		}
		Class<?> disasterClass = null;
		if (args.length > 2) {
			DisasterRegistry regenRegister = DisasterRegistry.getRegistry(args[2]);
			if (regenRegister == null) {
				sender.sendMessage(Utils.convertString("&cThere is no such disaster with the name '"+args[2]+"'!"));
				return true;
			}
			disasterClass = regenRegister.getRegisteredClass();
		}
		sender.sendMessage(Utils.convertString(Utils.prefix+"&eStarting regeneration task..."));
		final long startTime = System.currentTimeMillis();
		final Set<Block> blocks = new LinkedHashSet<>();
		Iterator<Entry<Disaster, RegeneratingTask>> regenIterator = Disaster.regeneratingDisasters.entrySet().iterator();
		while (regenIterator.hasNext()) {
			Entry<Disaster, RegeneratingTask> entry = regenIterator.next();
			Disaster temp = entry.getKey();
			if (worlds != null && !Stream.of(worlds).anyMatch(w -> w.equals(temp.getLocation().getWorld())))
				continue;
			if (disasterClass != null && !temp.getClass().equals(disasterClass))
				continue;
			entry.getValue().task.cancel();
			blocks.addAll(entry.getValue().blocks);
			blocks.addAll(temp.getModifiedBlocks());
			regenIterator.remove();
		}
		final World[] finalWorlds = worlds;
		new BukkitRunnable() {
			private final Iterator<Block> iterator = blocks.iterator();
			private int exceptions;
			private final int blocksPerTick = force ? regenBlocksPerTick : fastRegenBlocksPerTick;
			
			@Override
			public void run() {
				int tick = 0;
				while (++tick < blocksPerTick && iterator.hasNext())
					try {
						do {
							if (BlockRegenHandler.restoreBlock(iterator.next(), force))
								break;
						} while (iterator.hasNext());
					} catch (Exception e) {
						Utils.sendExceptionLog(e);
						++exceptions;
					}
				if (!iterator.hasNext()) {
//					BlockRegenHandler.printMaps();
					this.cancel();
					final long elapsedTime = System.currentTimeMillis() - startTime;
					final long gameTicks = (elapsedTime * 20) / 1000;
					final long average = blocks.size() / Math.max(gameTicks, 1);
					String completionMessage = Utils.convertString(Utils.prefix+"&aRegenerated all &d"+blocks.size()+" &adamaged blocks!"
							+ (force ? "" : "\n&4&lNOTE: &eFast regeneration task may have left some small bugged spots in the world where physics did not update!")
							+ "\n&3- &7&oTime: " + String.format("%02d", (int) (Math.round(elapsedTime / 1000 / 60)))+":"+String.format("%02d", (int) (Math.round(elapsedTime / 1000 % 60)))+":"+String.format("%02d", (int) (Math.round(elapsedTime / 10 % 100)))
							+ "\n&3- World: " + (finalWorlds == null || finalWorlds.length > 1 ? "&a&lALL" : "&b" + finalWorlds[0].getName())
							+ (args.length > 2 ? "\n&3- Disaster Type: &e" + args[2] : "")
							+ (exceptions > 0 ? "\n&3- &cExceptions: " + exceptions + " &7(These are blocks that failed to regenerate. Check server console for errors!)" : "")
							+ "\n&3- Stability: " + (gameTicks <= 1 || average >= blocksPerTick * 0.7 ? "&a" + average + '/' + blocksPerTick + " per tick (Good)" :
								(average >= blocksPerTick * 0.3 ? "&e" + average + '/' + blocksPerTick + " per tick (Moderate)" :
									"&c" + average + '/' + blocksPerTick + " per tick (Poor, consider lowering the force regen blocks per tick setting in the main config!)")));
					sender.sendMessage(completionMessage);
					if (!(sender instanceof ConsoleCommandSender))
						Main.consoleSender.sendMessage(Utils.convertString(Utils.prefix+"&7&oLogged "+(force ? "" : "fast ")+"regeneration task completion. Details of task:\n") + completionMessage);
				}
			}
		}.runTaskTimer(Main.getInstance(), 0, 1);
		return true;
	}
	private Player getOnlinePlayer(String name) {
		for (Player temp : Bukkit.getServer().getOnlinePlayers())
			if (temp.getName().equalsIgnoreCase(name))
				return temp;
		return null;
	}
	private World[] getWorldSelection(String name, CommandSender sender) {
		switch (name.toLowerCase()) {
		case "all_worlds" -> {
			return Bukkit.getServer().getWorlds().toArray(new World[0]);
		}
		case "this_world" -> {
			if (!(sender instanceof Player player))
				return null;
			return new World[] { player.getWorld() };
		}
		default -> {
			for (World world : Bukkit.getServer().getWorlds())
				if (world.getName().equalsIgnoreCase(name)) {
					return new World[] { world };
				}
			return null;
		}
		}
	}
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		List<String> list = new ArrayList<>();
		String keyword;
		switch (args.length) {
		case 1 -> {
			keyword = args[0].toLowerCase();
			if (sender.hasPermission("deadlydisasters.help"))
				list.add("help");
			if (sender.hasPermission("deadlydisasters.start"))
				list.add("start");
			if (sender.hasPermission("deadlydisasters.stop"))
				list.add("stop");
			if (sender.hasPermission("deadlydisasters.regenerate"))
				list.add("regenerate");
			if (sender.hasPermission("deadlydisasters.fastRegenerate"))
				list.add("fastRegenerate");
			if (sender.hasPermission("deadlydisasters.config"))
				list.add("config");
			if (sender.hasPermission("deadlydisasters.blacklist"))
				list.add("blacklist");
			if (sender.hasPermission("deadlydisasters.timers"))
				list.add("timers");
			list.removeIf(e -> !e.toLowerCase().contains(keyword));
		}
		case 2 -> {
			keyword = args[1].toLowerCase();
			if (args[0].equalsIgnoreCase("help") && sender.hasPermission("deadlydisasters.help")) {
				if (sender.hasPermission("deadlydisasters.start"))
					list.add("start");
				if (sender.hasPermission("deadlydisasters.stop"))
					list.add("stop");
				if (sender.hasPermission("deadlydisasters.regenerate"))
					list.add("regenerate");
				if (sender.hasPermission("deadlydisasters.fastRegenerate"))
					list.add("fastRegenerate");
				if (sender.hasPermission("deadlydisasters.config"))
					list.add("config");
				if (sender.hasPermission("deadlydisasters.blacklist"))
					list.add("blacklist");
				if (sender.hasPermission("deadlydisasters.timers"))
					list.add("timers");
			} else if ((args[0].equalsIgnoreCase("start") && sender.hasPermission("deadlydisasters.start"))
					|| (args[0].equalsIgnoreCase("stop") && sender.hasPermission("deadlydisasters.stop")))
				list.addAll(DisasterRegistry.getRegisteredNames());
			else if ((args[0].equalsIgnoreCase("regenerate") && sender.hasPermission("deadlydisasters.regenerate")) 
					|| args[0].equalsIgnoreCase("fastRegenerate") && sender.hasPermission("deadlydisasters.fastRegenerate"))
				list.addAll(Bukkit.getServer().getWorlds().stream().map(world -> world.getName()).collect(Collectors.toList()));
			else if (args[0].equalsIgnoreCase("config") && sender.hasPermission("deadlydisasters.config"))
				list.addAll(Arrays.asList("reload", "set", "enable", "disable", "setting", "list"));
			else if (args[0].equalsIgnoreCase("blacklist") && sender.hasPermission("deadlydisasters.blacklist"))
				list.addAll(Arrays.asList("add", "remove"));
			else if (args[0].equalsIgnoreCase("timers") && sender.hasPermission("deadlydisasters.timers"))
				list.addAll(Arrays.asList("listworlds", "listplayer"));
			list.removeIf(e -> !e.toLowerCase().contains(keyword));
		}
		case 3 -> {
			keyword = args[2].toLowerCase();
			if (args[0].equalsIgnoreCase("help") && sender.hasPermission("deadlydisasters.help")) {
				if (args[1].equalsIgnoreCase("config") && sender.hasPermission("deadlydisasters.config"))
					list.addAll(Arrays.asList("reload", "set", "enable", "disable", "setting", "list"));
			} else if (args[0].equalsIgnoreCase("start") && sender.hasPermission("deadlydisasters.start"))
				list.addAll(Arrays.asList("1", "2", "3", "4", "5", "6"));
			else if (args[0].equalsIgnoreCase("stop") && sender.hasPermission("deadlydisasters.stop"))
				list.addAll(Bukkit.getServer().getWorlds().stream().map(world -> world.getName()).collect(Collectors.toList()));
			else if ((args[0].equalsIgnoreCase("regenerate") && sender.hasPermission("deadlydisasters.regenerate")) 
					|| args[0].equalsIgnoreCase("fastRegenerate") && sender.hasPermission("deadlydisasters.fastRegenerate"))
				list.addAll(DisasterRegistry.getRegisteredNames());
			else if (args[0].equalsIgnoreCase("config") && sender.hasPermission("deadlydisasters.config")) {
				if (args[1].equalsIgnoreCase("set")) {
					list.addAll(Bukkit.getServer().getWorlds().stream().map(world -> world.getName()).collect(Collectors.toList()));
					list.add("ALL_WORLDS");
					if (sender instanceof Player)
						list.add("THIS_WORLD");
				} else if (args[1].equalsIgnoreCase("setting"))
					list.addAll(configSettings.keySet());
				else if (args[1].equalsIgnoreCase("enable") || args[1].equalsIgnoreCase("disable")) {
					list.addAll(DisasterRegistry.getRegisteredNames());
					list.addAll(WorldWrapper.disasterCategories.keySet());
				}
			} else if (args[0].equalsIgnoreCase("blacklist") && sender.hasPermission("deadlydisasters.blacklist"))
				list.addAll(Bukkit.getServer().getOnlinePlayers().stream().map(player -> player.getName()).collect(Collectors.toList()));
			else if (args[0].equalsIgnoreCase("timers") && sender.hasPermission("deadlydisasters.timers")
					&& args[1].equalsIgnoreCase("listplayer"))
				list.addAll(Bukkit.getServer().getOnlinePlayers().stream().map(player -> player.getName()).collect(Collectors.toList()));
			list.removeIf(e -> !e.toLowerCase().contains(keyword));
		}
		case 4 -> {
			keyword = args[3].toLowerCase();
			if (args[0].equalsIgnoreCase("start") && sender.hasPermission("deadlydisasters.start"))
				list.addAll(Bukkit.getServer().getOnlinePlayers().stream().map(player -> player.getName()).collect(Collectors.toList()));
			if (args[0].equalsIgnoreCase("config") && sender.hasPermission("deadlydisasters.config")) {
				if (args[1].equalsIgnoreCase("set"))
					try {
						File folder = new File(plugin.getDataFolder().getAbsolutePath(), "worldConfigs");
						if (folder.exists())
							for (File file : folder.listFiles())
								list.add(file.getName().split("\\.")[0]);
					} catch (Exception e) {
						Utils.sendExceptionLog(e);
					}
				else if (args[1].equalsIgnoreCase("setting")) {
					ConfigSettingOption option = configSettings.get(args[2].toLowerCase());
					if (option != null && option.values != null)
						list.addAll(option.values);
				} else if (args[1].equalsIgnoreCase("enable") || args[1].equalsIgnoreCase("disable")) {
					list.addAll(Bukkit.getServer().getWorlds().stream().map(world -> world.getName()).collect(Collectors.toList()));
					list.add("ALL_WORLDS");
					if (sender instanceof Player)
						list.add("THIS_WORLD");
				}
			} if (args[0].equalsIgnoreCase("blacklist") && sender.hasPermission("deadlydisasters.blacklist")) {
				list.addAll(Bukkit.getServer().getWorlds().stream().map(world -> world.getName()).collect(Collectors.toList()));
				list.add("ALL_WORLDS");
				if (sender instanceof Player)
					list.add("THIS_WORLD");
			}
			list.removeIf(e -> !e.toLowerCase().contains(keyword));
		}
		case 5 -> {
			keyword = args[4].toLowerCase();
			if (args[0].equalsIgnoreCase("config") && sender.hasPermission("deadlydisasters.config")
					&& args[1].equalsIgnoreCase("setting")) {
				list.addAll(Bukkit.getServer().getWorlds().stream().map(world -> world.getName()).collect(Collectors.toList()));
				list.add("ALL_WORLDS");
				if (sender instanceof Player)
					list.add("THIS_WORLD");
			}
		}
		case 7 -> {
			keyword = args[6].toLowerCase();
			if (args[0].equalsIgnoreCase("start") && sender.hasPermission("deadlydisasters.start"))
				list.addAll(Bukkit.getServer().getWorlds().stream().map(world -> world.getName()).collect(Collectors.toList()));
			list.removeIf(e -> !e.toLowerCase().contains(keyword));
		}
		default -> {}
		}
		return list;
	}
	private class ConfigSettingOption {
		
		private String configSection;
		private Set<String> values;
		private Function<ConfigSettingContainer, Boolean> function;
		
		private ConfigSettingOption(String configSection, Set<String> values, Function<ConfigSettingContainer, Boolean> function) {
			this.configSection = configSection;
			this.values = values;
			this.function = function;
		}
	}
	private class ConfigSettingContainer {
		
		private String value;
		private CommandSender sender;
		private WorldWrapper wrapper;
		
		private ConfigSettingContainer(String value, CommandSender sender, WorldWrapper wrapper) {
			this.value = value;
			this.sender = sender;
			this.wrapper = wrapper;
		}
	}
}
