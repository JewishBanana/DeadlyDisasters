package deadlydisasters.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import deadlydisasters.commands.Disasters;
import deadlydisasters.disasters.CustomDisaster;
import deadlydisasters.disasters.Disaster;
import deadlydisasters.disasters.ExtremeWinds;
import deadlydisasters.disasters.Hurricane;
import deadlydisasters.disasters.Sinkhole;
import deadlydisasters.disasters.Tornado;
import deadlydisasters.disasters.events.WeatherDisaster;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;
import deadlydisasters.general.WorldObject;
import deadlydisasters.listeners.CoreListener;
import deadlydisasters.listeners.TownyListener;

public class Utils {
	
	private static Set<String> blacklisted;
	private static Set<Material> mats = new HashSet<>();
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
	private static me.angeschossen.lands.api.integration.LandsIntegration landsclaims;
	
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
					landsclaims = new me.angeschossen.lands.api.integration.LandsIntegration(plugin);
			}
		}, 1);
		
		Sinkhole.treeBlocks.addAll(Arrays.asList(Material.OAK_LOG, Material.OAK_LEAVES, Material.BIRCH_LEAVES, Material.BIRCH_LOG, Material.SPRUCE_LOG, Material.SPRUCE_LEAVES, Material.DARK_OAK_LOG, Material.DARK_OAK_LEAVES,
				Material.ACACIA_LOG, Material.ACACIA_LEAVES));
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
	public static String chat(String s) {
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
		Main.consoleSender.sendMessage(Languages.prefix+str+ChatColor.GREEN+" ("+loc.getWorld().getName()+")");
	}
	public static boolean isWGRegion(Location location) {
		com.sk89q.worldedit.util.Location loc = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location);
		com.sk89q.worldguard.protection.regions.RegionContainer container = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
		com.sk89q.worldguard.protection.regions.RegionQuery query = container.createQuery();
		com.sk89q.worldguard.protection.ApplicableRegionSet set = query.getApplicableRegions(loc);
		return set.size() != 0;
	}
	public static boolean isWorldBlackListed(World w) {
		return blacklisted.contains(w.getName());
	}
	public static boolean isBlockBlacklisted(Material material) {
		return mats.contains(material);
	}
	public static Set<Material> getBlacklistedBlocks() {
		return mats;
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
	public static me.angeschossen.lands.api.integration.LandsIntegration getLandsClaims() {
		return landsclaims;
	}
	public static void reloadVariables() {
		FileConfiguration config = plugin.getConfig();
		blacklisted = new HashSet<String>(config.getStringList("blacklist.worlds"));
		if (blacklisted.contains("exampleWorld"))
			blacklisted.remove("exampleWorld");
		mats.clear();
		for (String str : config.getStringList("blacklist.blocks")) {
			if (Material.getMaterial(str.toUpperCase()) == null) {
				plugin.getLogger().info("[DeadlyDisasters]: There is no such block type '"+str+"' skipping this entry!");
			} else mats.add(Material.getMaterial(str.toUpperCase()));
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
		return (WorldObject.findWorldObject(loc.getWorld()).protectRegions && ((WGuardB && isWGRegion(loc)) || (TownyB && townyapi.getTownBlock(loc) != null)
				|| (GriefB && grief.getClaimAt(loc, true, null) != null) || (LandsB && landsclaims.isClaimed(loc)) || (KingsB && org.kingdoms.constants.land.Land.getLand(loc) != null)));
	}
	public static boolean isWeatherDisabled(Location loc, WeatherDisaster instance) {
		return (instance.RegionWeather && ((WGuardB && isWGRegion(loc)) || (TownyB && townyapi.getTownBlock(loc) != null)
				|| (GriefB && grief.getClaimAt(loc, true, null) != null) || (LandsB && landsclaims.isClaimed(loc)) || (KingsB && org.kingdoms.constants.land.Land.getLand(loc) != null)));
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
		Block b = spawn.getBlock();
		Location loc1 = null, loc2 = null;
		down:
			for (int i = spawn.getBlockY(); i > spawn.getBlockY()-maxDistance; i--) {
				b = b.getRelative(BlockFace.DOWN);
				if (!b.isPassable() && b.getRelative(BlockFace.UP).isPassable() && !b.getRelative(BlockFace.UP).isLiquid()) {
					for (int c = 2; c <= height; c++)
						if (!b.getRelative(BlockFace.UP, c).isPassable())
							continue down;
					loc1 = b.getRelative(BlockFace.UP).getLocation().add(0.5,0,0.5);
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
					loc2 = b.getLocation().add(0.5,0,0.5);
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
	public static int levelOfEnchant(String enchant, ItemStack item) {
		if (!item.hasItemMeta() || !item.getItemMeta().hasLore())
			return 0;
		for (String line : item.getItemMeta().getLore())
			if (line.contains(ChatColor.GRAY+enchant)) {
				if (!line.substring(line.length()-1).equals("I") && !line.substring(line.length()-1).equals("V"))
					return 1;
				switch (line.substring(line.indexOf(ChatColor.GRAY+enchant)+3+enchant.length())) {
				case "I":
					return 1;
				case "II":
					return 2;
				case "III":
					return 3;
				case "IV":
					return 4;
				case "V":
					return 5;
				}
			}
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
	public static void pureDamageEntity(LivingEntity entity, double damage, String meta) {
		if (entity.isDead())
			return;
		entity.damage(0.00001);
		if (entity.getHealth()-damage <= 0 && meta != null)
			entity.setMetadata(meta, plugin.fixedData);
		entity.setHealth(Math.max(entity.getHealth()-damage, 0));
	}
	public static String spigotText(String text, String hex) {
		return (net.md_5.bungee.api.ChatColor.of(hex) + text);
	}
	public static String translateTextColor(String text) {
		if (text == null)
			return null;
		String s = text;
		if (!s.contains("(hex:"))
			return chat(s);
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
		Entity entity = world.spawnEntity(new Location(world, 0, 0, 0), EntityType.MINECART_COMMAND);
		World tempWorld = Bukkit.getWorld("world");
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
	public static void damageEntity(LivingEntity entity, double damage, String meta) {
		double armor = entity.getAttribute(Attribute.GENERIC_ARMOR).getValue();
		double toughness = entity.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).getValue();
		double actualDamage = damage * (1 - Math.min(20, Math.max(armor / 5, armor - damage/(2 + toughness / 4))) / 25);
		Utils.pureDamageEntity(entity, actualDamage, meta);
		Utils.damageArmor(entity, actualDamage);
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
		Main.consoleSender.sendMessage(Utils.chat("&c[DeadlyDisasters]: An error has occurred above this message. Please report the full error to the discord https://discord.gg/MhXFj72VeN"));
	}
	public static void reloadPlugin(Main plugin) {
		CoreListener.reload(plugin);
		Utils.reloadVariables();
		Disaster.reload(plugin);
		Disasters.disasterNames.clear();
		for (Disaster temp : Disaster.values())
			if (temp != Disaster.CUSTOM)
				Disasters.disasterNames.add(temp.name().toLowerCase());
		CustomDisaster.loadFiles(plugin);
		WorldObject.reloadWorlds(plugin);
		if (plugin.seasonsHandler.isActive)
			plugin.seasonsHandler.reload(plugin);
		plugin.enchantHandler.reload();
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
}
