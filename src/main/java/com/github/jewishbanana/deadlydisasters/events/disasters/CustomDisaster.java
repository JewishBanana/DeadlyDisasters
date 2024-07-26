package com.github.jewishbanana.deadlydisasters.events.disasters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Flying;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.commands.Disasters;
import com.github.jewishbanana.deadlydisasters.events.DestructionDisaster;
import com.github.jewishbanana.deadlydisasters.events.Disaster;
import com.github.jewishbanana.deadlydisasters.events.WeatherDisaster;
import com.github.jewishbanana.deadlydisasters.handlers.Catalog;
import com.github.jewishbanana.deadlydisasters.handlers.ItemsHandler;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.handlers.WorldObject;
import com.github.jewishbanana.deadlydisasters.utils.Metrics;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class CustomDisaster {
	
	public static Map<String, YamlConfiguration> disasterFiles = new HashMap<>();
	
	private Main plugin;
	private YamlConfiguration yaml;
	private int level;
	private Random rand;
	private Location startPos;
	private String nameID;
	
	private int amount = -1;
	
	private Map<Integer, Queue<CustomEvent>> list = new LinkedHashMap<>();
	private Map<String, ItemStack> itemMap = new HashMap<>();
	private Map<String, CustomDisasterMob> entityMap = new HashMap<>();
	
	public CustomDisaster(int level, Main plugin, YamlConfiguration yaml) {
		this.rand = plugin.random;
		this.level = level;
		this.plugin = plugin;
		this.yaml = yaml;
	}
	public static void loadFiles(Main plugin) {
		File folder = new File(plugin.getDataFolder().getAbsolutePath(), "custom disasters");
		folder.mkdirs();
		if (!folder.exists()) {
			Main.consoleSender.sendMessage(Languages.prefix+Utils.convertString("&cCould not create custom disasters directory plugin was denied access?! Please create this folder manually in the plugins directory next to the config file &d'custom disasters'"));
			return;
		}
		disasterFiles.clear();
		new File(plugin.getDataFolder().getAbsolutePath(), "custom disasters").mkdirs();
		for (File f : folder.listFiles())
			loadDisaster(f);
	}
	public static void loadDisaster(File f) {
		try {
			YamlConfiguration temp = new YamlConfiguration();
			temp.load(f);
			if (temp.contains("catalog.id") && Catalog.catalogFileNames.contains(f.getName().substring(0, f.getName().indexOf('.'))))
				Catalog.downloadedDisasters.put(temp.getInt("catalog.id"), f);
			disasterFiles.put(temp.getString("settings.name"), temp);
			Disasters.addDisaster(temp.getString("settings.name"));
		} catch (InvalidConfigurationException e) {
			Main.consoleSender.sendMessage(Languages.prefix+Utils.convertString("&cCould not load &e'"+f.getName()+"' &cinvalid disaster configuration!\n"+e.getMessage()));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void start(Location loc, Player p) {
		this.startPos = loc;
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			public void run() {
				preInit();
				init(WorldObject.findWorldObject(loc.getWorld()).offset);
				updateValues(loc.getWorld(), p);
				
				String path = "core.level "+level+'.';
				int interval = 10;
				if (yaml.contains(path+"interval_seconds"))
					interval = yaml.getInt(path+"interval_seconds");
				int delay = 0;
				if (yaml.contains("settings.start_delay"))
					delay = yaml.getInt("settings.start_delay") * 20;
				if (yaml.getString(path+"operation").equals("random") && yaml.contains(path+"number_of_events"))
					amount = yaml.getInt(path+"number_of_events");
				
				Iterator<Entry<Integer, Queue<CustomEvent>>> it = list.entrySet().iterator();
				new RepeatingTask(plugin, delay, interval * 20) {
					public void run() {
						if (amount == 0) {
							if (yaml.contains("settings.ending_message"))
								broadcastEndingMessage(loc.getWorld());
							cancel();
							return;
						} else if (amount > 0)
							amount--;
						if (it.hasNext())
							it.next().getValue().forEach(e -> e.trigger(rand, plugin));
						else {
							if (yaml.contains("settings.ending_message"))
								broadcastEndingMessage(loc.getWorld());
							cancel();
						}
					}
				};
			}
		});
	}
	public void createTimedStart(int delaySeconds, Vector offset, Player p) {
		Location loc = p.getLocation();
		if (!DestructionDisaster.currentLocations.containsKey(loc.getWorld()))
			DestructionDisaster.currentLocations.put(loc.getWorld(), new ArrayDeque<>());
		DestructionDisaster.currentLocations.get(loc.getWorld()).add(p);
		plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
			public void run() {
				if (!DestructionDisaster.currentLocations.containsKey(loc.getWorld()) || !DestructionDisaster.currentLocations.get(loc.getWorld()).contains(p))
					return;
				DestructionDisaster.currentLocations.get(loc.getWorld()).remove(p);
				if (DestructionDisaster.currentLocations.get(loc.getWorld()).isEmpty())
					DestructionDisaster.currentLocations.remove(loc.getWorld());
				if (!p.isOnline() || !p.getWorld().equals(loc.getWorld()))
					return;
				WorldObject wo = WorldObject.findWorldObject(p.getWorld());
				if (Utils.isPlayerImmune(p) || !wo.naturalAllowed || !wo.allowed.contains(Disaster.CUSTOM))
					return;
				Location temp = p.getLocation().add(offset);
				if ((boolean) wo.settings.get("event_broadcast"))
					broadcastMessage(loc, p);
				if (DestructionDisaster.currentLocations.containsKey(p.getWorld())) {
					for (Entity e : p.getNearbyEntities(wo.maxRadius, wo.maxRadius, wo.maxRadius))
						if (e instanceof Player && DestructionDisaster.currentLocations.get(p.getWorld()).contains(e))
							DestructionDisaster.currentLocations.get(p.getWorld()).remove(e);
					if (DestructionDisaster.currentLocations.get(p.getWorld()).isEmpty())
						DestructionDisaster.currentLocations.remove(p.getWorld());
				}
				plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
					public void run() {
						start(temp, p);
						Metrics.incrementValue(Metrics.disasterOccurredMap, Disaster.CUSTOM.getMetricsLabel());
					}
				}, 0);
			}
		}, delaySeconds * 20);
	}
	public void broadcastEndingMessage(World world) {
		String msg = Utils.convertString(yaml.getString("settings.ending_message"));
		for (Player p : world.getPlayers())
			p.sendMessage(msg);
		Main.consoleSender.sendMessage(Languages.prefix+msg+Utils.convertString(" &a("+world.getName()+")"));
	}
	public void broadcastMessage(Location loc, Player p) {
		if (yaml.contains("settings.start_message")) {
			String str = yaml.getString("settings.start_message");
			char character = Utils.getLevelChar(level);
			str = str.replace("%level%", level+"").replace("%level_char%", "&"+character);
			str = Utils.convertString(str.replace("%disaster%", yaml.getString("settings.title")).replace("%location%", loc.getBlockX()+" "+loc.getBlockY()+" "+loc.getBlockZ()).replace("%player%", p.getName()));
			for (Player all : loc.getWorld().getPlayers())
				all.sendMessage(str);
			Main.consoleSender.sendMessage(Utils.convertString(str+" &a("+loc.getWorld().getName()+")"));
		} else {
			String str = plugin.getConfig().getString("messages.destructive.level "+level);
			str = Utils.convertString(str.replace("%disaster%", yaml.getString("settings.title")).replace("%location%", loc.getBlockX()+" "+loc.getBlockY()+" "+loc.getBlockZ()).replace("%player%", p.getName()));
			for (Player all : loc.getWorld().getPlayers())
				all.sendMessage(str);
			Main.consoleSender.sendMessage(Utils.convertString(str+" &a("+loc.getWorld().getName()+")"));
		}
	}
	public void updateValues(World world, Player p) {
		Iterator<Entry<Integer, Queue<CustomEvent>>> it = list.entrySet().iterator();
		while (it.hasNext())
			it.next().getValue().forEach(k -> k.initValues(world, p, startPos));
	}
	public void preInit() {
		if (yaml.contains("items"))
			for (String itemName : yaml.getConfigurationSection("items").getKeys(false))  {
				String path = "items."+itemName+'.';
				ItemStack item = new ItemStack(Material.valueOf(yaml.getString(path+"type").toUpperCase()));
				ItemMeta meta = item.getItemMeta();
				if (yaml.contains(path+"name"))
					meta.setDisplayName(Utils.convertString(yaml.getString(path+"name")));
				if (yaml.contains(path+"lore")) {
					List<String> lore = new ArrayList<>();
					for (String line : yaml.getStringList(path+"lore"))
						lore.add(Utils.convertString(line));
					meta.setLore(lore);
				}
				if (yaml.contains(path+"enchantments"))
					for (String temp : yaml.getConfigurationSection(path+"enchantments").getKeys(false)) {
						String tempPath = path+"enchantments."+temp+'.';
						meta.addEnchant(Registry.ENCHANTMENT.get(NamespacedKey.minecraft(yaml.getString(tempPath+"enchantment").toLowerCase())), yaml.getInt(tempPath+"level"), true);
					}
				if (yaml.contains(path+"hide_enchants") && yaml.getBoolean(path+"hide_enchants"))
					meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
				item.setItemMeta(meta);
				itemMap.put(itemName, item);
			}
		if (yaml.contains("entities"))
			for (String entityName : yaml.getConfigurationSection("entities").getKeys(false))  {
				String path = "entities."+entityName+'.';
				double health = 0, range = 0, speed = 0, damage = 0, knockback = 0, resistance = 0;
				if (yaml.contains(path+"health"))
					health = yaml.getDouble(path+"health");
				if (yaml.contains(path+"range"))
					range = yaml.getDouble(path+"range");
				if (yaml.contains(path+"speed"))
					speed = yaml.getDouble(path+"speed");
				if (yaml.contains(path+"damage"))
					damage = yaml.getDouble(path+"damage");
				if (yaml.contains(path+"knockback"))
					knockback = yaml.getDouble(path+"knockback");
				if (yaml.contains(path+"resistance"))
					resistance = yaml.getDouble(path+"resistance");
				ItemStack[] armor = {new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR)};
				ItemStack mainHand = new ItemStack(Material.AIR);
				ItemStack offHand = new ItemStack(Material.AIR);
				if (yaml.contains(path+"equipment")) {
					if (yaml.contains(path+"equipment.mainHand"))
						mainHand = getItemByName(yaml.getString(path+"equipment.mainHand"));
					if (yaml.contains(path+"equipment.offHand"))
						offHand = getItemByName(yaml.getString(path+"equipment.offHand"));
					if (yaml.contains(path+"equipment.helmet"))
						armor[3] = getItemByName(yaml.getString(path+"equipment.helmet"));
					if (yaml.contains(path+"equipment.chest"))
						armor[2] = getItemByName(yaml.getString(path+"equipment.chest"));
					if (yaml.contains(path+"equipment.legs"))
						armor[1] = getItemByName(yaml.getString(path+"equipment.legs"));
					if (yaml.contains(path+"equipment.boots"))
						armor[0] = getItemByName(yaml.getString(path+"equipment.boots"));
				}
				float[] dropChances = {0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f};
				if (yaml.contains(path+"dropChances")) {
					if (yaml.contains(path+"dropChances.mainHand"))
						dropChances[4] = (float) yaml.getDouble(path+"dropChances.mainHand");
					if (yaml.contains(path+"dropChances.offHand"))
						dropChances[5] = (float) yaml.getDouble(path+"dropChances.offHand");
					if (yaml.contains(path+"dropChances.helmet"))
						dropChances[0] = (float) yaml.getDouble(path+"dropChances.helmet");
					if (yaml.contains(path+"dropChances.chest"))
						dropChances[1] = (float) yaml.getDouble(path+"dropChances.chest");
					if (yaml.contains(path+"dropChances.legs"))
						dropChances[2] = (float) yaml.getDouble(path+"dropChances.legs");
					if (yaml.contains(path+"dropChances.boots"))
						dropChances[3] = (float) yaml.getDouble(path+"dropChances.boots");
				}
				boolean silent = false;
				if (yaml.contains(path+"silent"))
					silent = yaml.getBoolean(path+"silent");
				boolean pickUp = false;
				if (yaml.contains(path+"pickUpItems"))
					pickUp = yaml.getBoolean(path+"pickUpItems");
				boolean despawn = false;
				if (yaml.contains(path+"despawn"))
					despawn = yaml.getBoolean(path+"despawn");
				String customName = null;
				if (yaml.contains(path+"name"))
					customName = Utils.convertString(yaml.getString(path+"name"));
				CustomDisasterMob mob = new CustomDisasterMob(EntityType.valueOf(yaml.getString(path+"type").toUpperCase()), health, range, speed, damage, knockback, resistance, armor, mainHand, offHand, dropChances,
						silent, customName, pickUp, despawn);
				entityMap.put(entityName, mob);
			}
	}
	public void init(int worldOffset) {
		String initial = "core.level "+level;
		Map<Integer, Queue<CustomEvent>> tempList = new LinkedHashMap<>();
		
		for (String eventSection : yaml.getConfigurationSection(initial+".events").getKeys(false)) {
			Queue<CustomEvent> listOfEvents = new ArrayDeque<>();
			for (String event : yaml.getConfigurationSection(initial+".events."+eventSection).getKeys(false)) {
				String path = initial+".events."+eventSection+'.'+event+'.';
				if (event.contains("disaster")) {
					int disLevel = 0;
					if (yaml.contains(path+"level"))
						disLevel = yaml.getInt(path+"level");
					else
						disLevel = rand.nextInt(6)+1;
					int targets = 0;
					if (yaml.contains(path+"target")) {
						if (yaml.getString(path+"target").equals("all"))
							targets = -1;
						else if (yaml.getString(path+"target").equals("random"))
							targets = yaml.getInt(yaml.getString(path+"targetAmount"));
						else if (yaml.getString(path+"target").equals("startPos"))
							targets = -2;
					}
					int offset = Math.max(worldOffset, 1);
					if (yaml.contains(path+"offset"))
						offset = Math.max(yaml.getInt(path+"offset"), 1);
					String[] firstPoints = {"0", "0", "0"};
					if (yaml.contains(path+"location")) {
						offset = -1;
						firstPoints[0] = yaml.getString(path+"location.x");
						firstPoints[1] = yaml.getString(path+"location.y");
						firstPoints[2] = yaml.getString(path+"location.z");
					}
					boolean broadcastAllowed = false;
					if (yaml.contains(path+"broadcastAllowed"))
						broadcastAllowed = yaml.getBoolean(path+"broadcastAllowed");
					//weathers
					switch (yaml.getString(path+"type")) {
					case "acidstorm":
						AcidStorm acidstorm = new AcidStorm(disLevel);
						if (yaml.contains(path+"time"))
							acidstorm.setTime(yaml.getInt(path+"time") * 20);
						if (yaml.contains(path+"melt_items"))
							acidstorm.setMeltItems(yaml.getBoolean(path+"melt_items"));
						if (yaml.contains(path+"melt_armor"))
							acidstorm.setMeltArmor(yaml.getBoolean(path+"melt_armor"));
						if (yaml.contains(path+"damage"))
							acidstorm.setDamage(yaml.getDouble(path+"damage"));
						if (yaml.contains(path+"volume"))
							acidstorm.setVolume(yaml.getDouble(path+"volume"));
						listOfEvents.add(new CustomEvent("wd", acidstorm, broadcastAllowed));
						continue;
					case "plague":
						BlackPlague plague = new BlackPlague(disLevel);
						listOfEvents.add(new CustomEvent("wd", plague, broadcastAllowed));
						continue;
					case "blizzard":
						Blizzard blizzard = new Blizzard(disLevel);
						if (yaml.contains(path+"time"))
							blizzard.setTime(yaml.getInt(path+"time") * 20);
						if (yaml.contains(path+"freezeMobs"))
							blizzard.setMobsFreeze(yaml.getBoolean(path+"freezeMobs"));
						if (yaml.contains(path+"leatherProtect"))
							blizzard.setLeatherProtect(yaml.getBoolean(path+"leatherProtect"));
						if (yaml.contains(path+"volume"))
							blizzard.setVolume(yaml.getDouble(path+"volume"));
						listOfEvents.add(new CustomEvent("wd", blizzard, broadcastAllowed));
						continue;
					case "endstorm":
						EndStorm endstorm = new EndStorm(disLevel);
						if (yaml.contains(path+"time"))
							endstorm.setTime(yaml.getInt(path+"time") * 20);
						if (yaml.contains(path+"maxEntities"))
							endstorm.setMaxEntities(yaml.getInt(path+"maxEntities"));
						if (yaml.contains(path+"teleportRange"))
							endstorm.setRange(yaml.getInt(path+"teleportRange"));
						if (yaml.contains(path+"volume"))
							endstorm.setVolume(yaml.getDouble(path+"volume"));
						listOfEvents.add(new CustomEvent("wd", endstorm, broadcastAllowed));
						continue;
					case "extremewinds":
						ExtremeWinds winds = new ExtremeWinds(disLevel);
						if (yaml.contains(path+"time"))
							winds.setTime(yaml.getInt(path+"time") * 20);
						if (yaml.contains(path+"force"))
							winds.setTempForce(yaml.getDouble(path+"force"));
						if (yaml.contains(path+"particleType"))
							winds.setParticle(Particle.valueOf(yaml.getString(path+"particleType").toUpperCase()));
						if (yaml.contains(path+"maxParticles"))
							winds.setMaxParticles(yaml.getInt(path+"maxParticles"));
						if (yaml.contains(path+"blockForce"))
							winds.setBreakForce(yaml.getDouble(path+"blockForce"));
						if (yaml.contains(path+"volume"))
							winds.setVolume(yaml.getDouble(path+"volume"));
						listOfEvents.add(new CustomEvent("wd", winds, broadcastAllowed));
						continue;
					case "meteorshower":
						MeteorShower meteor = new MeteorShower(disLevel);
						if (yaml.contains(path+"time"))
							meteor.setTime(yaml.getInt(path+"time") * 20);
						if (yaml.contains(path+"night"))
							meteor.setNight(yaml.getBoolean(path+"night"));
						if (yaml.contains(path+"particleType"))
							meteor.setParticle(Particle.valueOf(yaml.getString(path+"particleType").toUpperCase()));
						if (yaml.contains(path+"smokeTime"))
							meteor.setSmokeTime(yaml.getInt(path+"smokeTime"));
						if (yaml.contains(path+"spawnRate"))
							meteor.setSpawnRate(yaml.getInt(path+"spawnRate"));
						if (yaml.contains(path+"maxMeteors"))
							meteor.setMax(yaml.getInt(path+"maxMeteors"));
						if (yaml.contains(path+"volume"))
							meteor.setVolume(yaml.getDouble(path+"volume"));
						int[] types = {1, 2, 3};
						double[] speeds = {1.0, 1.0, 1.0};
						int[][] sizes = {{2, 6},{2, 6},{2, 6}};
						if (yaml.contains(path+"normalMeteor")) {
							if (yaml.contains(path+"normalMeteor.allowed") && !yaml.getBoolean(path+"normalMeteor.allowed"))
								types[0] = 0;
							if (yaml.contains(path+"normalMeteor.material"))
								meteor.setNormalMaterial(Material.valueOf(yaml.getString(path+"normalMeteor.material").toUpperCase()));
							if (yaml.contains(path+"normalMeteor.generateOres"))
								meteor.setSpawnOres(yaml.getBoolean(path+"normalMeteor.generateOres"));
							if (yaml.contains(path+"normalMeteor.speed"))
								speeds[0] = (yaml.getDouble(path+"normalMeteor.speed"));
							if (yaml.contains(path+"normalMeteor.minSize"))
								sizes[0][0] = yaml.getInt(path+"normalMeteor.minSize");
							if (yaml.contains(path+"normalMeteor.maxSize"))
								sizes[0][1] = yaml.getInt(path+"normalMeteor.maxSize");
						}
						if (yaml.contains(path+"explodingMeteor")) {
							if (yaml.contains(path+"explodingMeteor.allowed") && !yaml.getBoolean(path+"explodingMeteor.allowed"))
								types[1] = 0;
							if (yaml.contains(path+"explodingMeteor.material"))
								meteor.setExplodingMaterial(Material.valueOf(yaml.getString(path+"explodingMeteor.material").toUpperCase()));
							if (yaml.contains(path+"explodingMeteor.damage"))
								meteor.setExplosionDamage(yaml.getDouble(path+"explodingMeteor.damage"));
							if (yaml.contains(path+"explodingMeteor.speed"))
								speeds[1] = (yaml.getDouble(path+"explodingMeteor.speed"));
							if (yaml.contains(path+"explodingMeteor.minSize"))
								sizes[1][0] = yaml.getInt(path+"explodingMeteor.minSize");
							if (yaml.contains(path+"explodingMeteor.maxSize"))
								sizes[1][1] = yaml.getInt(path+"explodingMeteor.maxSize");
						}
						if (yaml.contains(path+"splittingMeteor")) {
							if (yaml.contains(path+"splittingMeteor.allowed") && !yaml.getBoolean(path+"splittingMeteor.allowed"))
								types[0] = 0;
							if (yaml.contains(path+"splittingMeteor.material"))
								meteor.setSplittingMaterial(Material.valueOf(yaml.getString(path+"splittingMeteor.material").toUpperCase()));
							if (yaml.contains(path+"splittingMeteor.impactSpeed"))
								meteor.setSplitImpact(yaml.getDouble(path+"splittingMeteor.impactSpeed"));
							if (yaml.contains(path+"splittingMeteor.speed"))
								speeds[2] = (yaml.getDouble(path+"splittingMeteor.speed"));
							if (yaml.contains(path+"splittingMeteor.minSize"))
								sizes[2][0] = yaml.getInt(path+"splittingMeteor.minSize");
							if (yaml.contains(path+"splittingMeteor.maxSize"))
								sizes[2][1] = yaml.getInt(path+"splittingMeteor.maxSize");
						}
						for (int i=0; i < types.length; i++)
							if (types[i] == 0) {
								types = Utils.removeElement(types, i);
								i--;
							}
						meteor.setAllowedTypes(types);
						meteor.setSpeeds(speeds);
						meteor.setMeteorSizes(sizes);
						listOfEvents.add(new CustomEvent("wd", meteor, broadcastAllowed));
						continue;
					case "sandstorm":
						SandStorm sandstorm = new SandStorm(disLevel);
						if (yaml.contains(path+"time"))
							sandstorm.setTime(yaml.getInt(path+"time") * 20);
						if (yaml.contains(path+"customMobs"))
							sandstorm.setCustom(yaml.getBoolean(path+"customMobs"));
						if (yaml.contains(path+"wither"))
							sandstorm.setWitherActive(yaml.getBoolean(path+"wither"));
						if (yaml.contains(path+"skulls"))
							sandstorm.setMobsDropSkulls(yaml.getBoolean(path+"skulls"));
						if (yaml.contains(path+"volume"))
							sandstorm.setVolume(yaml.getDouble(path+"volume"));
						listOfEvents.add(new CustomEvent("wd", sandstorm, broadcastAllowed));
						continue;
					case "soulstorm":
						SoulStorm soulstorm = new SoulStorm(disLevel);
						if (yaml.contains(path+"time"))
							soulstorm.setTime(yaml.getInt(path+"time") * 20);
						if (yaml.contains(path+"customMobs"))
							soulstorm.setVexSpawn(yaml.getBoolean(path+"customMobs"));
						if (yaml.contains(path+"volume"))
							soulstorm.setVolume(yaml.getDouble(path+"volume"));
						listOfEvents.add(new CustomEvent("wd", soulstorm, broadcastAllowed));
						continue;

					//destructive
					case "sinkhole":
						Sinkhole sinkhole = new Sinkhole(disLevel);
						if (yaml.contains(path+"size"))
							sinkhole.setSize(yaml.getDouble(path+"size"));
						if (yaml.contains(path+"tickSpeed"))
							sinkhole.setSpeed(yaml.getInt(path+"tickSpeed"));
						if (yaml.contains(path+"volume"))
							sinkhole.setVolume(yaml.getDouble(path+"volume"));
						listOfEvents.add(new CustomEvent("dd", sinkhole, targets, offset, firstPoints));
						continue;
					case "earthquake":
						Earthquake earthquake = new Earthquake(disLevel);
						if (yaml.contains(path+"size"))
							earthquake.setSize(yaml.getDouble(path+"size"));
						if (yaml.contains(path+"force"))
							earthquake.setForce(yaml.getDouble(path+"force"));
						if (yaml.contains(path+"length"))
							earthquake.setRadius(yaml.getInt(path+"length"));
						if (yaml.contains(path+"width"))
							earthquake.setWidth(yaml.getInt(path+"width"));
						if (yaml.contains(path+"tilt"))
							earthquake.setTilt(yaml.getDouble(path+"tilt"));
						if (yaml.contains(path+"volume"))
							earthquake.setVolume(yaml.getDouble(path+"volume"));
						listOfEvents.add(new CustomEvent("dd", earthquake, targets, offset, firstPoints));
						continue;
					case "supernova":
						Supernova supernova = new Supernova(disLevel);
						if (yaml.contains(path+"size"))
							supernova.setSizeMultiplier(yaml.getDouble(path+"size"));
						if (yaml.contains(path+"particleChance"))
							supernova.setParticles(yaml.getDouble(path+"particleChance"));
						if (yaml.contains(path+"particleType"))
							supernova.setParticle(Particle.valueOf(yaml.getString(path+"particleType").toUpperCase()));
						if (yaml.contains(path+"flash"))
							supernova.setFlash(yaml.getBoolean(path+"flash"));
						if (yaml.contains(path+"volume"))
							supernova.setVolume(yaml.getDouble(path+"volume"));
						if (yaml.contains(path+"materials")) {
							List<String> strings = yaml.getStringList(path+"materials");
							List<Material> materials = new ArrayList<>();
							strings.forEach(k -> materials.add(Material.valueOf(k.toUpperCase())));
							supernova.setMaterials(materials.toArray(new Material[0]));
						}
						listOfEvents.add(new CustomEvent("dd", supernova, targets, offset, firstPoints));
						continue;
					case "cavein":
						CaveIn cavein = new CaveIn(disLevel);
						if (yaml.contains(path+"size"))
							cavein.setSize(yaml.getDouble(path+"size"));
						if (yaml.contains(path+"depth"))
							cavein.setDepth(yaml.getInt(path+"depth"));
						if (yaml.contains(path+"fallSpeed"))
							cavein.setFallSpeed(yaml.getDouble(path+"fallSpeed"));
						if (yaml.contains(path+"volume"))
							cavein.setVolume(yaml.getDouble(path+"volume"));
						if (yaml.contains(path+"materials")) {
							List<String> strings = yaml.getStringList(path+"materials");
							List<Material> materials = new ArrayList<>();
							strings.forEach(k -> materials.add(Material.valueOf(k.toUpperCase())));
							cavein.setMaterials(materials.toArray(new Material[0]));
						}
						listOfEvents.add(new CustomEvent("dd", cavein, targets, offset, firstPoints));
						continue;
					case "geyser":
						Geyser geyser = new Geyser(disLevel);
						if (yaml.contains(path+"width"))
							geyser.setWidth(yaml.getInt(path+"width") - 1);
						if (yaml.contains(path+"amount"))
							geyser.setMiniGeyserAmount(yaml.getInt(path+"amount"));
						if (yaml.contains(path+"damage"))
							geyser.setDamage(yaml.getDouble(path+"damage"));
						if (yaml.contains(path+"material"))
							geyser.setMaterial(Material.valueOf(yaml.getString(path+"material").toUpperCase()));
						if (yaml.contains(path+"tickSpeed"))
							geyser.setTickSpeed(yaml.getInt(path+"tickSpeed"));
						if (yaml.contains(path+"velocity"))
							geyser.setYVelocity(yaml.getDouble(path+"velocity"));
						if (yaml.contains(path+"particleType"))
							geyser.setParticle(Particle.valueOf(yaml.getString(path+"particleType").toUpperCase()));
						if (yaml.contains(path+"minReach"))
							geyser.setMinReach(yaml.getInt(path+"minReach"));
						if (yaml.contains(path+"maxReach"))
							geyser.setMaxReach(yaml.getInt(path+"maxReach"));
						if (yaml.contains(path+"range"))
							geyser.setRange(yaml.getInt(path+"range"));
						if (yaml.contains(path+"spawnTicks"))
							geyser.setSpawnInterval(yaml.getInt(path+"spawnTicks"));
						if (yaml.contains(path+"volume"))
							geyser.setVolume(yaml.getDouble(path+"volume"));
						if (yaml.contains(path+"sound"))
							geyser.setSound(getSound(yaml.getString(path+"sound").toUpperCase()));
						listOfEvents.add(new CustomEvent("dd", geyser, targets, offset, firstPoints));
						continue;
					case "hurricane":
						Hurricane hurricane = new Hurricane(disLevel);
						if (yaml.contains(path+"size"))
							hurricane.setSize(yaml.getInt(path+"size"));
						if (yaml.contains(path+"time"))
							hurricane.setTime(yaml.getInt(path+"time") * 20);
						if (yaml.contains(path+"lightning"))
							hurricane.setLightning(yaml.getInt(path+"lightning"));
						if (yaml.contains(path+"blockForce"))
							hurricane.setBlockForce(yaml.getDouble(path+"blockForce"));
						if (yaml.contains(path+"minForce"))
							hurricane.setMinForce(yaml.getDouble(path+"minForce"));
						if (yaml.contains(path+"maxForce"))
							hurricane.setMaxForce(yaml.getDouble(path+"maxForce"));
						if (yaml.contains(path+"particleType"))
							hurricane.setParticle(Particle.valueOf(yaml.getString(path+"particleType").toUpperCase()));
						if (yaml.contains(path+"volume"))
							hurricane.setVolume(yaml.getDouble(path+"volume"));
						listOfEvents.add(new CustomEvent("dd", hurricane, targets, offset, firstPoints));
						continue;
					case "purge":
						Purge purge = new Purge(disLevel);
						if (yaml.contains(path+"maxEntities"))
							purge.setMax(yaml.getInt(path+"maxEntities"));
						if (yaml.contains(path+"showBar"))
							purge.setShowBar(yaml.getBoolean(path+"showBar"));
						if (yaml.contains(path+"barTitle"))
							purge.setBarTitle(yaml.getString(path+"barTitle"));
						if (yaml.contains(path+"barColor"))
							purge.setBarColor(BarColor.valueOf(yaml.getString(path+"barColor").toUpperCase()));
						if (yaml.contains(path+"spawnDistance"))
							purge.setSpawnDistance(yaml.getInt(path+"spawnDistance"));
						if (yaml.contains(path+"despawnSpeed"))
							purge.setDespawnSpeed(yaml.getInt(path+"despawnSpeed"));
						if (yaml.contains(path+"spawnTickSpeed"))
							purge.setSpawnSpeed(yaml.getInt(path+"spawnTickSpeed"));
						if (yaml.contains(path+"endMessage"))
							purge.setEndMessage(yaml.getString(path+"endMessage"));
						if (yaml.contains(path+"volume"))
							purge.setVolume(yaml.getDouble(path+"volume"));
						listOfEvents.add(new CustomEvent("dd", purge, targets, offset, firstPoints));
						continue;
					case "tornado":
						Tornado tornado = new Tornado(disLevel);
						if (yaml.contains(path+"size"))
							tornado.setSize(yaml.getInt(path+"size"));
						if (yaml.contains(path+"time"))
							tornado.setTime(yaml.getInt(path+"time") * 20);
						if (yaml.contains(path+"maxBlocks"))
							tornado.setMaxBlocks(yaml.getInt(path+"maxBlocks"));
						if (yaml.contains(path+"pullForce"))
							tornado.setPullForce(yaml.getDouble(path+"pullForce"));
						if (yaml.contains(path+"yVelocity"))
							tornado.setyVelocity(yaml.getDouble(path+"yVelocity"));
						if (yaml.contains(path+"speed"))
							tornado.setSpeed(yaml.getDouble(path+"speed"));
						if (yaml.contains(path+"height"))
							tornado.setHeight(yaml.getInt(path+"height"));
						if (yaml.contains(path+"particleChance"))
							tornado.setParticles(yaml.getDouble(path+"particleChance"));
						if (yaml.contains(path+"width"))
							tornado.setWidth(yaml.getInt(path+"width"));
						if (yaml.contains(path+"particleType"))
							tornado.setParticleType(Particle.valueOf(yaml.getString(path+"particleType").toUpperCase()));
						if (yaml.contains(path+"volume"))
							tornado.setVolume(yaml.getDouble(path+"volume"));
						if (yaml.contains(path+"materials")) {
							List<String> strings = yaml.getStringList(path+"materials");
							List<Material> materials = new ArrayList<>();
							strings.forEach(k -> materials.add(Material.valueOf(k.toUpperCase())));
							tornado.setMaterials(materials.toArray(new Material[0]));
						}
						listOfEvents.add(new CustomEvent("dd", tornado, targets, offset, firstPoints));
						continue;
					case "tsunami":
						Tsunami tsunami = new Tsunami(disLevel);
						if (yaml.contains(path+"size"))
							tsunami.setRadius(yaml.getInt(path+"size"));
						if (yaml.contains(path+"height"))
							tsunami.setHeight(yaml.getInt(path+"height"));
						if (yaml.contains(path+"damage"))
							tsunami.setDamage(yaml.getDouble(path+"damage"));
						if (yaml.contains(path+"tickSpeed"))
							tsunami.setTickSpeed(yaml.getInt(path+"tickSpeed"));
						if (yaml.contains(path+"removeLiquid"))
							tsunami.setRemoveWater(yaml.getBoolean(path+"removeLiquid"));
						if (yaml.contains(path+"ignoreOcean"))
							tsunami.setIgnoreOcean(yaml.getBoolean(path+"ignoreOcean"));
						if (yaml.contains(path+"liquidMaterial"))
							tsunami.setLiquid(Material.valueOf(yaml.getString(path+"liquidMaterial").toUpperCase()));
						if (yaml.contains(path+"particleType"))
							tsunami.setParticleType(Particle.valueOf(yaml.getString(path+"particleType").toUpperCase()));
						if (yaml.contains(path+"minDepth"))
							tsunami.setDepth(yaml.getInt(path+"minDepth"));
						if (yaml.contains(path+"volume"))
							tsunami.setVolume(yaml.getDouble(path+"volume"));
						if (yaml.contains(path+"materials")) {
							List<String> strings = yaml.getStringList(path+"materials");
							List<BlockData> materials = new ArrayList<>();
							strings.forEach(k -> materials.add(Material.valueOf(k.toUpperCase()).createBlockData()));
							tsunami.setMaterials(materials.toArray(new BlockData[0]));
						}
						listOfEvents.add(new CustomEvent("dd", tsunami, targets, offset, firstPoints));
						continue;
					}
				} else if (event.contains("broadcast")) {
					int targets = 0;
					if (yaml.contains(path+"target")) {
						if (yaml.getString(path+"target").equals("all"))
							targets = -1;
						else if (yaml.getString(path+"target").equals("random"))
							targets = yaml.getInt(yaml.getString(path+"targetAmount"));
					}
					listOfEvents.add(new CustomEvent("b", yaml.getString(path+"message"), targets, 0, null));
					continue;
				} else if (event.contains("fill")) {
					int targets = 0;
					if (yaml.contains(path+"target")) {
						if (yaml.getString(path+"target").equals("all"))
							targets = -1;
						else if (yaml.getString(path+"target").equals("random"))
							targets = yaml.getInt(yaml.getString(path+"targetAmount"));
						else if (yaml.getString(path+"target").equals("startPos"))
							targets = -2;
					}
					String[] points = {"0", "0", "0", "0", "0", "0"};
					if (yaml.contains(path+"firstPoint")) {
						points[0] = yaml.getString(path+"firstPoint.x");
						points[1] = yaml.getString(path+"firstPoint.y");
						points[2] = yaml.getString(path+"firstPoint.z");
					}
					if (yaml.contains(path+"secondPoint")) {
						points[3] = yaml.getString(path+"secondPoint.x");
						points[4] = yaml.getString(path+"secondPoint.y");
						points[5] = yaml.getString(path+"secondPoint.z");
					}
					boolean fillSolids = false;
					if (yaml.contains(path+"fillSolids"))
						fillSolids = yaml.getBoolean(path+"fillSolids");
					Set<Material> ignored = new HashSet<>();
					boolean blacklist = true;
					if (yaml.contains(path+"blacklist")) {
						List<String> strings = yaml.getStringList(path+"blacklist");
						List<Material> materials = new ArrayList<>();
						strings.forEach(k -> materials.add(Material.valueOf(k.toUpperCase())));
						ignored.addAll(materials);
					} else if (yaml.contains(path+"whitelist")) {
						blacklist = false;
						List<String> strings = yaml.getStringList(path+"whitelist");
						List<Material> materials = new ArrayList<>();
						strings.forEach(k -> {
							Material mat = Material.valueOf(k.toUpperCase());
							if (!Utils.passStrengthTest(mat))
								materials.add(mat);
						});
						ignored.addAll(materials);
					}
					int partition = 1;
					if (yaml.contains(path+"partition"))
						partition = yaml.getInt(path+"partition");
					Material[] newMats = {Material.AIR};
					if (yaml.contains(path+"materials")) {
						List<String> strings = yaml.getStringList(path+"materials");
						List<Material> materials = new ArrayList<>();
						strings.forEach(k -> materials.add(Material.valueOf(k.toUpperCase())));
						newMats = materials.toArray(new Material[0]);
					}
					listOfEvents.add(new CustomEvent("f", targets, points, blacklist, ignored, fillSolids, partition, newMats));
					continue;
				} else if (event.contains("summon")) {
					CustomDisasterMob mob = entityMap.get(yaml.get(path+"type"));
					int targets = 0;
					if (yaml.contains(path+"target")) {
						if (yaml.getString(path+"target").equals("all"))
							targets = -1;
						else if (yaml.getString(path+"target").equals("random"))
							targets = yaml.getInt(yaml.getString(path+"targetAmount"));
						else if (yaml.getString(path+"target").equals("startPos"))
							targets = -2;
					}
					boolean force = false;
					if (yaml.contains(path+"force"))
						force = yaml.getBoolean(path+"force");
					int offset = 0;
					if (yaml.contains(path+"offset")) {
						offset = yaml.getInt(path+"offset");
						if (offset < 1)
							offset = 1;
					}
					boolean hasTarget = false;
					if (yaml.contains(path+"setTarget"))
						hasTarget = yaml.getBoolean(path+"setTarget");
					String[] points = {"0", "0", "0"};
					if (yaml.contains(path+"location")) {
						points[0] = yaml.getString(path+"location.x");
						points[1] = yaml.getString(path+"location.y");
						points[2] = yaml.getString(path+"location.z");
					}
					listOfEvents.add(new CustomEvent("s", mob, targets, offset, points, force, hasTarget));
					continue;
				} else if (event.contains("particle")) {
					int targets = 0;
					if (yaml.contains(path+"target")) {
						if (yaml.getString(path+"target").equals("all"))
							targets = -1;
						else if (yaml.getString(path+"target").equals("random"))
							targets = yaml.getInt(yaml.getString(path+"targetAmount"));
						else if (yaml.getString(path+"target").equals("startPos"))
							targets = -2;
					}
					Particle particle = Particle.CLOUD;
					if (yaml.contains(path+"type")) {
						String type = yaml.getString(path+"type").toUpperCase();
						if (type.equals("BLOCK_CRACK") || type.equals("BLOCK_DUST") || type.equals("BLOCK_MARKER") || type.equals("FALLING_DUST") || type.equals("REDSTONE") || type.equals("ITEM_CRACK") || type.equals("DUST_COLOR_TRANSITION")) {
							Main.consoleSender.sendMessage(Languages.prefix+Utils.convertString("&eWARNING in '"+nameID+"' file! &f'"+type+"' &eparticle is not supported! Error at &fLevel "+level+" in "+eventSection+"&e! Ignoring this action.."));
							continue;
						}
						particle = Particle.valueOf(type);
					}
					double[] data = {10, 0.1, 0, 0, 0, 1, 1};
					if (yaml.contains(path+"count"))
						data[0] = yaml.getDouble(path+"count");
					if (yaml.contains(path+"speed"))
						data[1] = yaml.getDouble(path+"speed");
					if (yaml.contains(path+"range")) {
						data[2] = yaml.getDouble(path+"range.x");
						data[3] = yaml.getDouble(path+"range.y");
						data[4] = yaml.getDouble(path+"range.z");
					}
					if (yaml.contains(path+"repeat"))
						data[5] = yaml.getInt(path+"repeat");
					if (yaml.contains(path+"intervalTicks"))
						data[6] = yaml.getInt(path+"intervalTicks");
					String[] points = {"0", "0", "0"};
					if (yaml.contains(path+"location")) {
						points[0] = yaml.getString(path+"location.x");
						points[1] = yaml.getString(path+"location.y");
						points[2] = yaml.getString(path+"location.z");
					}
					listOfEvents.add(new CustomEvent("p", targets, particle, data, points));
					continue;
				} else if (event.contains("sound")) {
					int targets = 0;
					if (yaml.contains(path+"target")) {
						if (yaml.getString(path+"target").equals("all"))
							targets = -1;
						else if (yaml.getString(path+"target").equals("random"))
							targets = yaml.getInt(yaml.getString(path+"targetAmount"));
						else if (yaml.getString(path+"target").equals("startPos"))
							targets = -2;
					}
					Sound sound = Sound.AMBIENT_CAVE;
					if (yaml.contains(path+"type"))
						sound = getSound(yaml.getString(path+"type").toUpperCase());
					double[] data = {1, 1, 1, 20};
					if (yaml.contains(path+"volume"))
						data[0] = yaml.getDouble(path+"volume");
					if (yaml.contains(path+"pitch"))
						data[1] = yaml.getDouble(path+"pitch");
					if (yaml.contains(path+"repeat"))
						data[2] = yaml.getInt(path+"repeat");
					if (yaml.contains(path+"intervalTicks"))
						data[3] = yaml.getInt(path+"intervalTicks");
					String[] points = {"0", "0", "0"};
					if (yaml.contains(path+"location")) {
						points[0] = yaml.getString(path+"location.x");
						points[1] = yaml.getString(path+"location.y");
						points[2] = yaml.getString(path+"location.z");
					}
					listOfEvents.add(new CustomEvent("n", targets, sound, data, points));
					continue;
				} else {
					Main.consoleSender.sendMessage(Languages.prefix+Utils.convertString("&eWARNING in '"+nameID+"' file! &f'"+event+"' &eis not reconized as an action at &fLevel "+level+" in "+eventSection+"&e! Ignoring this entry.."));
				}
			}
			if (eventSection.length() < 7) {
				Main.consoleSender.sendMessage(Languages.prefix+Utils.convertString("&eWARNING in '"+nameID+"' file! &f'"+eventSection+" &eis not a valid event format! Ignoring this section.. Format should be as follows"
						+ "\n    events:\n        event 1:\n            <actions>\n        event 2:\n            <actions>..."));
				continue;
			}
			tempList.put(Integer.parseInt(eventSection.substring(6)), listOfEvents);
		}
		if (yaml.contains(initial+".operation") && yaml.getString(initial+".operation").equals("random")) {
			List<Integer> arrayList = new ArrayList<>(tempList.keySet());
		    Collections.shuffle(arrayList);
		    arrayList.forEach(k -> list.put(k, tempList.get(k)));
		} else
			list.putAll(tempList);
	}
	public ItemStack getItemByName(String name) {
		if (itemMap.containsKey(name))
			return itemMap.get(name);
		if (ItemsHandler.allItems.containsKey(name))
			return ItemsHandler.allItems.get(name);
		return new ItemStack(Material.valueOf(name.toUpperCase()));
	}
	public Sound getSound(String name) {
		return Sound.valueOf(name);
	}
	public YamlConfiguration getYaml() {
		return yaml;
	}
	public void setYaml(YamlConfiguration yaml) {
		this.yaml = yaml;
	}
	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
		this.level = level;
	}
	public Location getStartPos() {
		return startPos;
	}
	public void setStartPos(Location startPos) {
		this.startPos = startPos;
	}
	public String getNameID() {
		return nameID;
	}
	public void setNameID(String nameID) {
		this.nameID = nameID;
	}
}
class CustomEvent {
	private Object event;
	private int targets;
	private Player p;
	private World world;
	private int offset;
	private boolean broadcastAllowed;
	private String operation;
	private Vector v1,v2,off1,off2;
	private boolean bool1;
	private boolean bool2;
	private Set<Material> ignored;
	private Material[] newMats;
	private int partition;
	private Location startPos;
	private CustomDisasterMob mob;
	private Particle particle;
	private double[] particleSettings;
	private Sound sound;
	
	public CustomEvent(String operation, Object event, int targets, int offset, String[] points) {
		this.operation = operation;
		this.event = event;
		this.targets = targets;
		if (operation.equals("dd")) {
			this.offset = offset;
			this.v1 = new Vector(0,0,0);
			this.off1 = new Vector(0,0,0);
			if (points[0].charAt(0) == '~')
				this.v1.setX(Double.parseDouble(points[0].substring(1)));
			else {
				this.v1.setX(0.123);
				this.off1.setX(Double.parseDouble(points[0]));
			}
			if (points[1].charAt(0) == '~')
				this.v1.setY(Double.parseDouble(points[1].substring(1)));
			else {
				this.v1.setY(0.123);
				this.off1.setY(Double.parseDouble(points[1]));
			}
			if (points[2].charAt(0) == '~')
				this.v1.setZ(Double.parseDouble(points[2].substring(1)));
			else {
				this.v1.setZ(0.123);
				this.off1.setZ(Double.parseDouble(points[2]));
			}
		}
	}
	public CustomEvent(String operation, Object event, boolean broadcastAllowed) {
		this.operation = operation;
		this.event = event;
		this.broadcastAllowed = broadcastAllowed;
	}
	public CustomEvent(String operation, int targets, String[] points, boolean blacklist, Set<Material> set, boolean fillSolids, int partition, Material[] newMats) {
		this.operation = operation;
		this.targets = targets;
		this.bool1 = blacklist;
		this.bool2 = fillSolids;
		this.ignored = set;
		this.partition = partition;
		this.newMats = newMats;
		this.v1 = new Vector(0,0,0);
		this.v2 = new Vector(0,0,0);
		this.off1 = new Vector(0,0,0);
		this.off2 = new Vector(0,0,0);
		if (points[0].charAt(0) == '~')
			this.v1.setX(Double.parseDouble(points[0].substring(1)));
		else {
			this.v1.setX(0.123);
			this.off1.setX(Double.parseDouble(points[0]));
		}
		if (points[1].charAt(0) == '~')
			this.v1.setY(Double.parseDouble(points[1].substring(1)));
		else {
			this.v1.setY(0.123);
			this.off1.setY(Double.parseDouble(points[1]));
		}
		if (points[2].charAt(0) == '~')
			this.v1.setZ(Double.parseDouble(points[2].substring(1)));
		else {
			this.v1.setZ(0.123);
			this.off1.setZ(Double.parseDouble(points[2]));
		}
		if (points[3].charAt(0) == '~')
			this.v2.setX(Double.parseDouble(points[3].substring(1)));
		else {
			this.v2.setX(0.123);
			this.off2.setX(Double.parseDouble(points[3]));
		}
		if (points[4].charAt(0) == '~')
			this.v2.setY(Double.parseDouble(points[4].substring(1)));
		else {
			this.v2.setY(0.123);
			this.off2.setY(Double.parseDouble(points[4]));
		}
		if (points[5].charAt(0) == '~')
			this.v2.setZ(Double.parseDouble(points[5].substring(1)));
		else {
			this.v2.setZ(0.123);
			this.off2.setZ(Double.parseDouble(points[5]));
		}
	}
	public CustomEvent(String operation, CustomDisasterMob mob, int targets, int offset, String[] points, boolean force, boolean targeting) {
		this.operation = operation;
		this.mob = mob;
		this.targets = targets;
		this.offset = offset;
		this.bool1 = force;
		this.bool2 = targeting;
		this.v1 = new Vector(0,0,0);
		this.off1 = new Vector(0,0,0);
		if (points[0].charAt(0) == '~')
			this.v1.setX(Double.parseDouble(points[0].substring(1)));
		else {
			this.v1.setX(0.123);
			this.off1.setX(Double.parseDouble(points[0]));
		}
		if (points[1].charAt(0) == '~')
			this.v1.setY(Double.parseDouble(points[1].substring(1)));
		else {
			this.v1.setY(0.123);
			this.off1.setY(Double.parseDouble(points[1]));
		}
		if (points[2].charAt(0) == '~')
			this.v1.setZ(Double.parseDouble(points[2].substring(1)));
		else {
			this.v1.setZ(0.123);
			this.off1.setZ(Double.parseDouble(points[2]));
		}
	}
	public CustomEvent(String operation, int targets, Particle particle, double[] data, String[] points) {
		this.operation = operation;
		this.targets = targets;
		this.particle = particle;
		this.particleSettings = data;
		this.v1 = new Vector(0,0,0);
		this.off1 = new Vector(0,0,0);
		if (points[0].charAt(0) == '~')
			this.v1.setX(Double.parseDouble(points[0].substring(1)));
		else {
			this.v1.setX(0.123);
			this.off1.setX(Double.parseDouble(points[0]));
		}
		if (points[1].charAt(0) == '~')
			this.v1.setY(Double.parseDouble(points[1].substring(1)));
		else {
			this.v1.setY(0.123);
			this.off1.setY(Double.parseDouble(points[1]));
		}
		if (points[2].charAt(0) == '~')
			this.v1.setZ(Double.parseDouble(points[2].substring(1)));
		else {
			this.v1.setZ(0.123);
			this.off1.setZ(Double.parseDouble(points[2]));
		}
	}
	public CustomEvent(String operation, int targets, Sound sound, double[] data, String[] points) {
		this.operation = operation;
		this.targets = targets;
		this.sound = sound;
		this.particleSettings = data;
		this.v1 = new Vector(0,0,0);
		this.off1 = new Vector(0,0,0);
		if (points[0].charAt(0) == '~')
			this.v1.setX(Double.parseDouble(points[0].substring(1)));
		else {
			this.v1.setX(0.123);
			this.off1.setX(Double.parseDouble(points[0]));
		}
		if (points[1].charAt(0) == '~')
			this.v1.setY(Double.parseDouble(points[1].substring(1)));
		else {
			this.v1.setY(0.123);
			this.off1.setY(Double.parseDouble(points[1]));
		}
		if (points[2].charAt(0) == '~')
			this.v1.setZ(Double.parseDouble(points[2].substring(1)));
		else {
			this.v1.setZ(0.123);
			this.off1.setZ(Double.parseDouble(points[2]));
		}
	}
	public void trigger(Random rand, Main plugin) {
		if (operation.equals("dd")) {
			if (targets == 0) {
				if (p != null && world.equals(p.getWorld()) && !Utils.isZoneProtected(p.getLocation())) {
					Location loc = p.getLocation().clone();
					if (offset < 0) {
						if (v1.getX() == 0.123) loc.setX(off1.getX()); else loc.setX(loc.getX()+v1.getX());
						if (v1.getY() == 0.123) loc.setY(off1.getY()); else loc.setY(loc.getY()+v1.getY());
						if (v1.getZ() == 0.123) loc.setZ(off1.getZ()); else loc.setZ(loc.getZ()+v1.getZ());
						((DestructionDisaster) event).startAdjustment(loc, p);
					} else
						((DestructionDisaster) event).startAdjustment(loc.add(rand.nextInt(offset*2)-offset,0,rand.nextInt(offset*2)-offset), p);
				}
				return;
			} else if (targets == -1) {
				for (Player all : world.getPlayers())
					if (!Utils.isZoneProtected(all.getLocation())) {
						Location loc = all.getLocation().clone();
						if (offset < 0) {
							if (v1.getX() == 0.123) loc.setX(off1.getX()); else loc.setX(loc.getX()+v1.getX());
							if (v1.getY() == 0.123) loc.setY(off1.getY()); else loc.setY(loc.getY()+v1.getY());
							if (v1.getZ() == 0.123) loc.setZ(off1.getZ()); else loc.setZ(loc.getZ()+v1.getZ());
							((DestructionDisaster) event).startAdjustment(loc, all);
						} else
							((DestructionDisaster) event).startAdjustment(loc.add(rand.nextInt(offset*2)-offset,0,rand.nextInt(offset*2)-offset), all);
					}
				return;
			} else if (targets == -2) {
				Location loc = startPos.clone();
				if (offset < 0) {
					if (v1.getX() == 0.123) loc.setX(off1.getX()); else loc.setX(loc.getX()+v1.getX());
					if (v1.getY() == 0.123) loc.setY(off1.getY()); else loc.setY(loc.getY()+v1.getY());
					if (v1.getZ() == 0.123) loc.setZ(off1.getZ()); else loc.setZ(loc.getZ()+v1.getZ());
					((DestructionDisaster) event).startAdjustment(loc, p);
				} else
					((DestructionDisaster) event).startAdjustment(loc.add(rand.nextInt(offset*2)-offset,0,rand.nextInt(offset*2)-offset), p);
				return;
			} else {
				int limit = world.getPlayers().size();
				if (limit == 0)
					return;
				targets = Math.min(targets, limit);
				Set<Integer> nums = new LinkedHashSet<>();
				while (nums.size() < targets)
					nums.add(rand.nextInt(limit));
				for (Integer num : nums) {
					Player all = world.getPlayers().get(num);
					if (!Utils.isZoneProtected(all.getLocation())) {
						Location loc = all.getLocation().clone();
						if (offset < 0) {
							if (v1.getX() == 0.123) loc.setX(off1.getX()); else loc.setX(loc.getX()+v1.getX());
							if (v1.getY() == 0.123) loc.setY(off1.getY()); else loc.setY(loc.getY()+v1.getY());
							if (v1.getZ() == 0.123) loc.setZ(off1.getZ()); else loc.setZ(loc.getZ()+v1.getZ());
							((DestructionDisaster) event).startAdjustment(loc, all);
						} else
							((DestructionDisaster) event).startAdjustment(loc.add(rand.nextInt(offset*2)-offset,0,rand.nextInt(offset*2)-offset), all);
					}
				}
				return;
			}
		} else if (operation.equals("wd")) {
			((WeatherDisaster) event).start(world, p, broadcastAllowed);
			return;
		} else if (operation.equals("b")) {
			String str = Utils.convertString((String) event);
			if (targets == 0) {
				if (p != null && world.equals(p.getWorld()))
					p.sendMessage(str);
				return;
			} else if (targets == -1) {
				for (Player all : world.getPlayers())
					all.sendMessage(str);
				return;
			} else {
				int limit = world.getPlayers().size();
				if (limit == 0)
					return;
				targets = Math.min(targets, limit);
				Set<Integer> nums = new LinkedHashSet<>();
				while (nums.size() < targets)
					nums.add(rand.nextInt(limit));
				for (Integer num : nums) {
					Player all = world.getPlayers().get(num);
					all.sendMessage(str);
				}
				return;
			}
		} else if (operation.equals("f")) {
			Queue<Block> blocks = new ArrayDeque<>();
			if (targets == 0 && p != null && world.equals(p.getWorld()) && !Utils.isZoneProtected(p.getLocation())) {
				Location p1 = p.getLocation().clone(), p2 = p.getLocation().clone();
				if (v1.getX() == 0.123) p1.setX(off1.getX()); else p1.setX(p1.getX()+v1.getX());
				if (v1.getY() == 0.123) p1.setY(off1.getY()); else p1.setY(p1.getY()+v1.getY());
				if (v1.getZ() == 0.123) p1.setZ(off1.getZ()); else p1.setZ(p1.getZ()+v1.getZ());
				if (v2.getX() == 0.123) p2.setX(off2.getX()); else p2.setX(p2.getX()+v2.getX());
				if (v2.getY() == 0.123) p2.setY(off2.getY()); else p2.setY(p2.getY()+v2.getY());
				if (v2.getZ() == 0.123) p2.setZ(off2.getZ()); else p2.setZ(p2.getZ()+v2.getZ());
				for (int x=p1.getBlockX(); x < p2.getBlockX(); x++)
					for (int y=p1.getBlockY(); y < p2.getBlockY(); y++)
						for (int z=p1.getBlockZ(); z < p2.getBlockZ(); z++)
							blocks.add(new Location(world, x, y, z).getBlock());
			} else if (targets == -1) {
				for (Player all : world.getPlayers())
					if (!Utils.isZoneProtected(all.getLocation())) {
						Location p1 = all.getLocation().clone(), p2 = all.getLocation().clone();
						if (v1.getX() == 0.123) p1.setX(off1.getX()); else p1.setX(p1.getX()+v1.getX());
						if (v1.getY() == 0.123) p1.setY(off1.getY()); else p1.setY(p1.getY()+v1.getY());
						if (v1.getZ() == 0.123) p1.setZ(off1.getZ()); else p1.setZ(p1.getZ()+v1.getZ());
						if (v2.getX() == 0.123) p2.setX(off2.getX()); else p2.setX(p2.getX()+v2.getX());
						if (v2.getY() == 0.123) p2.setY(off2.getY()); else p2.setY(p2.getY()+v2.getY());
						if (v2.getZ() == 0.123) p2.setZ(off2.getZ()); else p2.setZ(p2.getZ()+v2.getZ());
						for (int x=p1.getBlockX(); x < p2.getBlockX(); x++)
							for (int y=p1.getBlockY(); y < p2.getBlockY(); y++)
								for (int z=p1.getBlockZ(); z < p2.getBlockZ(); z++)
									blocks.add(new Location(world, x, y, z).getBlock());
					}
			} else if (targets == -2) {
				Location p1 = startPos.clone(), p2 = startPos.clone();
				if (v1.getX() == 0.123) p1.setX(off1.getX()); else p1.setX(p1.getX()+v1.getX());
				if (v1.getY() == 0.123) p1.setY(off1.getY()); else p1.setY(p1.getY()+v1.getY());
				if (v1.getZ() == 0.123) p1.setZ(off1.getZ()); else p1.setZ(p1.getZ()+v1.getZ());
				if (v2.getX() == 0.123) p2.setX(off2.getX()); else p2.setX(p2.getX()+v2.getX());
				if (v2.getY() == 0.123) p2.setY(off2.getY()); else p2.setY(p2.getY()+v2.getY());
				if (v2.getZ() == 0.123) p2.setZ(off2.getZ()); else p2.setZ(p2.getZ()+v2.getZ());
				for (int x=p1.getBlockX(); x < p2.getBlockX(); x++)
					for (int y=p1.getBlockY(); y < p2.getBlockY(); y++)
						for (int z=p1.getBlockZ(); z < p2.getBlockZ(); z++)
							blocks.add(new Location(world, x, y, z).getBlock());
			} else {
				int limit = world.getPlayers().size();
				if (limit == 0)
					return;
				targets = Math.min(targets, limit);
				Set<Integer> nums = new LinkedHashSet<>();
				while (nums.size() < targets)
					nums.add(rand.nextInt(limit));
				for (Integer num : nums) {
					Player all = world.getPlayers().get(num);
					if (Utils.isZoneProtected(all.getLocation()))
						continue;
					Location p1 = all.getLocation().clone(), p2 = all.getLocation().clone();
					if (v1.getX() == 0.123) p1.setX(off1.getX()); else p1.setX(p1.getX()+v1.getX());
					if (v1.getY() == 0.123) p1.setY(off1.getY()); else p1.setY(p1.getY()+v1.getY());
					if (v1.getZ() == 0.123) p1.setZ(off1.getZ()); else p1.setZ(p1.getZ()+v1.getZ());
					if (v2.getX() == 0.123) p2.setX(off2.getX()); else p2.setX(p2.getX()+v2.getX());
					if (v2.getY() == 0.123) p2.setY(off2.getY()); else p2.setY(p2.getY()+v2.getY());
					if (v2.getZ() == 0.123) p2.setZ(off2.getZ()); else p2.setZ(p2.getZ()+v2.getZ());
					for (int x=p1.getBlockX(); x < p2.getBlockX(); x++)
						for (int y=p1.getBlockY(); y < p2.getBlockY(); y++)
							for (int z=p1.getBlockZ(); z < p2.getBlockZ(); z++)
								blocks.add(new Location(world, x, y, z).getBlock());
				}
			}
			partition = blocks.size() / partition;
			int[] cycle = {0};
			new RepeatingTask(plugin, 0, 1) {
				public void run() {
					Iterator<Block> it = blocks.iterator();
					while (it.hasNext())
						if (cycle[0] >= partition) {
							cycle[0] = 0;
							break;
						} else {
							Block b = it.next();
							if ((bool2 && b.isPassable()) || Utils.isZoneProtected(b.getLocation())) {
								it.remove();
								continue;
							}
							if (bool1) {
								if (!ignored.contains(b.getType()))
									b.setType(newMats[rand.nextInt(newMats.length)]);
							} else if (ignored.contains(b.getType()))
								b.setType(newMats[rand.nextInt(newMats.length)]);
							it.remove();
						}
					if (blocks.isEmpty())
						cancel();
				}
			};
			return;
		} else if (operation.equals("s")) {
			if (targets == 0 && p != null && world.equals(p.getWorld()) && !Utils.isZoneProtected(p.getLocation())) {
				Location loc = p.getLocation().clone();
				if (offset < 0) {
					if (v1.getX() == 0.123) loc.setX(off1.getX()); else loc.setX(loc.getX()+v1.getX());
					if (v1.getY() == 0.123) loc.setY(off1.getY()); else loc.setY(loc.getY()+v1.getY());
					if (v1.getZ() == 0.123) loc.setZ(off1.getZ()); else loc.setZ(loc.getZ()+v1.getZ());
				} else
					loc.add(rand.nextInt(offset*2)-offset,0,rand.nextInt(offset*2)-offset);
				if (!bool1)
					loc = Utils.findApplicableSpawn(loc);
				mob.spawnMob(loc, bool2, p);
				return;
			} else if (targets == -1) {
				for (Player all : world.getPlayers())
					if (!Utils.isZoneProtected(all.getLocation())) {
						Location loc = all.getLocation().clone();
						if (offset < 0) {
							if (v1.getX() == 0.123) loc.setX(off1.getX()); else loc.setX(loc.getX()+v1.getX());
							if (v1.getY() == 0.123) loc.setY(off1.getY()); else loc.setY(loc.getY()+v1.getY());
							if (v1.getZ() == 0.123) loc.setZ(off1.getZ()); else loc.setZ(loc.getZ()+v1.getZ());
						} else
							loc.add(rand.nextInt(offset*2)-offset,0,rand.nextInt(offset*2)-offset);
						if (!bool1)
							loc = Utils.findApplicableSpawn(loc);
						mob.spawnMob(loc, bool2, all);
					}
				return;
			} else if (targets == -2) {
				Location loc = startPos.clone();
				if (offset < 0) {
					if (v1.getX() == 0.123) loc.setX(off1.getX()); else loc.setX(loc.getX()+v1.getX());
					if (v1.getY() == 0.123) loc.setY(off1.getY()); else loc.setY(loc.getY()+v1.getY());
					if (v1.getZ() == 0.123) loc.setZ(off1.getZ()); else loc.setZ(loc.getZ()+v1.getZ());
				} else
					loc.add(rand.nextInt(offset*2)-offset,0,rand.nextInt(offset*2)-offset);
				if (!bool1)
					loc = Utils.findApplicableSpawn(loc);
				mob.spawnMob(loc, bool2, p);
				return;
			} else {
				int limit = world.getPlayers().size();
				if (limit == 0)
					return;
				targets = Math.min(targets, limit);
				Set<Integer> nums = new LinkedHashSet<>();
				while (nums.size() < targets)
					nums.add(rand.nextInt(limit));
				for (Integer num : nums) {
					Player all = world.getPlayers().get(num);
					if (!Utils.isZoneProtected(all.getLocation())) {
						Location loc = all.getLocation().clone();
						if (offset < 0) {
							if (v1.getX() == 0.123) loc.setX(off1.getX()); else loc.setX(loc.getX()+v1.getX());
							if (v1.getY() == 0.123) loc.setY(off1.getY()); else loc.setY(loc.getY()+v1.getY());
							if (v1.getZ() == 0.123) loc.setZ(off1.getZ()); else loc.setZ(loc.getZ()+v1.getZ());
						} else
							loc.add(rand.nextInt(offset*2)-offset,0,rand.nextInt(offset*2)-offset);
						if (!bool1)
							loc = Utils.findApplicableSpawn(loc);
						mob.spawnMob(loc, bool2, all);
					}
				}
				return;
			}
		} else if (operation.equals("p")) {
			Queue<Player> players = new ArrayDeque<>();
			if (targets == 0 && p != null && world.equals(p.getWorld()))
				players.add(p);
			else if (targets == -1)
				players.addAll(world.getPlayers());
			else if (targets == -2) {
				Location loc = startPos.clone();
				if (v1.getX() == 0.123) loc.setX(off1.getX()); else loc.setX(loc.getX()+v1.getX());
				if (v1.getY() == 0.123) loc.setY(off1.getY()); else loc.setY(loc.getY()+v1.getY());
				if (v1.getZ() == 0.123) loc.setZ(off1.getZ()); else loc.setZ(loc.getZ()+v1.getZ());
				new RepeatingTask(plugin, 0, (int) particleSettings[6]) {
					@Override
					public void run() {
						if (particleSettings[5] <= 0) {
							cancel();
							return;
						}
						particleSettings[5]--;
						world.spawnParticle(particle, loc, (int) particleSettings[0], particleSettings[2], particleSettings[3], particleSettings[4], particleSettings[1]);
					}
				};
				return;
			} else {
				int limit = world.getPlayers().size();
				if (limit == 0)
					return;
				targets = Math.min(targets, limit);
				Set<Integer> nums = new LinkedHashSet<>();
				while (nums.size() < targets)
					nums.add(rand.nextInt(limit));
				for (Integer num : nums)
					players.add(world.getPlayers().get(num));
			}
			new RepeatingTask(plugin, 0, (int) particleSettings[6]) {
				@Override
				public void run() {
					if (particleSettings[5] <= 0) {
						cancel();
						return;
					}
					particleSettings[5]--;
					Iterator<Player> it = players.iterator();
					while (it.hasNext()) {
						Player player = it.next();
						if (player == null)
							continue;
						Location loc = player.getLocation().clone();
						if (v1.getX() == 0.123) loc.setX(off1.getX()); else loc.setX(loc.getX()+v1.getX());
						if (v1.getY() == 0.123) loc.setY(off1.getY()); else loc.setY(loc.getY()+v1.getY());
						if (v1.getZ() == 0.123) loc.setZ(off1.getZ()); else loc.setZ(loc.getZ()+v1.getZ());
						world.spawnParticle(particle, loc, (int) particleSettings[0], particleSettings[2], particleSettings[3], particleSettings[4], particleSettings[1]);
					}
				}
			};
			return;
		} else if (operation.equals("n")) {
			Queue<Player> players = new ArrayDeque<>();
			if (targets == 0 && p != null && world.equals(p.getWorld()))
				players.add(p);
			else if (targets == -1)
				players.addAll(world.getPlayers());
			else if (targets == -2) {
				Location loc = startPos.clone();
				if (v1.getX() == 0.123) loc.setX(off1.getX()); else loc.setX(loc.getX()+v1.getX());
				if (v1.getY() == 0.123) loc.setY(off1.getY()); else loc.setY(loc.getY()+v1.getY());
				if (v1.getZ() == 0.123) loc.setZ(off1.getZ()); else loc.setZ(loc.getZ()+v1.getZ());
				new RepeatingTask(plugin, 0, (int) particleSettings[3]) {
					@Override
					public void run() {
						if (particleSettings[2] <= 0) {
							cancel();
							return;
						}
						particleSettings[2]--;
						world.playSound(loc, sound, (float) particleSettings[0], (float) particleSettings[1]);
					}
				};
				return;
			} else {
				int limit = world.getPlayers().size();
				if (limit == 0)
					return;
				targets = Math.min(targets, limit);
				Set<Integer> nums = new LinkedHashSet<>();
				while (nums.size() < targets)
					nums.add(rand.nextInt(limit));
				for (Integer num : nums)
					players.add(world.getPlayers().get(num));
			}
			new RepeatingTask(plugin, 0, (int) particleSettings[3]) {
				@Override
				public void run() {
					if (particleSettings[2] <= 0) {
						cancel();
						return;
					}
					particleSettings[2]--;
					Iterator<Player> it = players.iterator();
					while (it.hasNext()) {
						Player player = it.next();
						if (player == null)
							continue;
						Location loc = player.getLocation().clone();
						if (v1.getX() == 0.123) loc.setX(off1.getX()); else loc.setX(loc.getX()+v1.getX());
						if (v1.getY() == 0.123) loc.setY(off1.getY()); else loc.setY(loc.getY()+v1.getY());
						if (v1.getZ() == 0.123) loc.setZ(off1.getZ()); else loc.setZ(loc.getZ()+v1.getZ());
						world.playSound(loc, sound, (float) particleSettings[0], (float) particleSettings[1]);
					}
				}
			};
			return;
		}
	}
	public void initValues(World world, Player p, Location startPos) {
		this.world = world;
		this.p = p;
		this.startPos = startPos;
	}
}
class CustomDisasterMob {
	private EntityType type;
	private double health;
	private double range;
	private double speed;
	private double damage;
	private double knockback;
	private double resistance;
	private ItemStack[] armor;
	private ItemStack mainHand;
	private ItemStack offHand;
	private float[] dropChances;
	private boolean silent;
	private String name;
	private boolean pickUp;
	private boolean despawn;
	
	public CustomDisasterMob(EntityType type, double health, double range, double speed, double damage, double knockback, double resistance,
			ItemStack[] armor, ItemStack mainHand, ItemStack offHand, float[] dropChances, boolean silent, String name, boolean pickUp, boolean despawn) {
		this.type = type;
		this.health = health;
		this.range = range;
		this.speed = speed;
		this.damage = damage;
		this.knockback = knockback;
		this.resistance = resistance;
		this.armor = armor;
		this.mainHand = mainHand;
		this.offHand = offHand;
		this.dropChances = dropChances;
		this.silent = silent;
		this.name = name;
		this.pickUp = pickUp;
		this.despawn = despawn;
	}
	public void spawnMob(Location loc, boolean hasTarget, LivingEntity target) {
		Entity e = loc.getWorld().spawnEntity(loc, type);
		if (name != null)
			e.setCustomName(name);
		if (e instanceof LivingEntity) {
			LivingEntity le = (LivingEntity) e;
			if (health > 0) {
				le.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
				le.setHealth(health);
			}
			if (range > 0)
				le.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(range);
			if (damage > 0)
				le.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
			if (speed > 0) {
				if (le instanceof Flying)
					le.getAttribute(Attribute.GENERIC_FLYING_SPEED).setBaseValue(speed);
				else
					le.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
			}
			if (knockback > 0 && Main.getInstance().mcVersion >= 1.16)
				le.getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK).setBaseValue(knockback);
			if (resistance > 0)
				le.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(resistance);
			le.setSilent(silent);
			le.setCanPickupItems(pickUp);
			le.setRemoveWhenFarAway(despawn);
			
			EntityEquipment equip = le.getEquipment();
			equip.setItemInMainHand(mainHand);
			equip.setItemInMainHandDropChance(dropChances[4]);
			equip.setItemInOffHand(offHand);
			equip.setItemInOffHandDropChance(dropChances[5]);
			equip.setArmorContents(armor);
			equip.setHelmetDropChance(dropChances[0]);
			equip.setChestplateDropChance(dropChances[1]);
			equip.setLeggingsDropChance(dropChances[2]);
			equip.setBootsDropChance(dropChances[3]);
			
			if (hasTarget && le instanceof Mob && !(target instanceof Player && Utils.isPlayerImmune((Player) target)))
				((Mob) le).setTarget(target);
		}
	}
}