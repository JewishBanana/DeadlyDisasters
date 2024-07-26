package com.github.jewishbanana.deadlydisasters.listeners;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Rotatable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.christmasentities.Santa;
import com.github.jewishbanana.deadlydisasters.entities.easterentities.EasterBunny;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.PumpkinKing;
import com.github.jewishbanana.deadlydisasters.events.DestructionDisaster;
import com.github.jewishbanana.deadlydisasters.events.DisasterEvent;
import com.github.jewishbanana.deadlydisasters.events.disasters.AcidStorm;
import com.github.jewishbanana.deadlydisasters.events.disasters.Blizzard;
import com.github.jewishbanana.deadlydisasters.events.disasters.EndStorm;
import com.github.jewishbanana.deadlydisasters.events.disasters.ExtremeWinds;
import com.github.jewishbanana.deadlydisasters.events.disasters.Hurricane;
import com.github.jewishbanana.deadlydisasters.events.disasters.MeteorShower;
import com.github.jewishbanana.deadlydisasters.events.disasters.SandStorm;
import com.github.jewishbanana.deadlydisasters.events.disasters.SolarStorm;
import com.github.jewishbanana.deadlydisasters.events.disasters.SoulStorm;
import com.github.jewishbanana.deadlydisasters.handlers.ItemsHandler;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.handlers.TimerCheck;
import com.github.jewishbanana.deadlydisasters.handlers.WorldObject;
import com.github.jewishbanana.deadlydisasters.items.BasicCoatingBook;
import com.github.jewishbanana.deadlydisasters.items.PlagueCure;
import com.github.jewishbanana.deadlydisasters.items.SplashPlagueCure;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class CoreListener implements Listener {
	
	private Main plugin;
	private Queue<UUID> notified = new ArrayDeque<>();
	private Queue<UUID> warnForKick = new ArrayDeque<>();
	private FileConfiguration dataFile;
	private Random rand;
	private NamespacedKey key;
//	private boolean favoredDisaster,dislikedDisaster;
	
	private static boolean worldSwap;
	private static boolean nonOpMsg;
	public static String worldMessage;
	
	private static Map<Entity, ItemStack[]> blockInventories = new HashMap<>();
	
	private TimerCheck tc;
	
	private FixedMetadataValue fixdata;
	
	private Map<UUID,Integer> mageWandCooldownMap = new HashMap<UUID,Integer>();
	private Map<UUID,Integer> voidBowCooldownMap = new HashMap<UUID,Integer>();
	
	public static Set<UUID> joinedServer = new HashSet<>();
	public static Set<UUID> catalogNotify = new HashSet<>();
	public static boolean catalogNotifyBool;
	
	public static Map<UUID,DisasterEvent> fallingBlocks = new HashMap<>();
	public static Map<Fireball, DisasterEvent> fireBalls = new HashMap<>();
	
	public CoreListener(Main plugin, TimerCheck tc, FileConfiguration dataFile, Random rand) {
		this.plugin = plugin;
		this.tc = tc;
		this.dataFile = dataFile;
		this.rand = rand;
		this.key = new NamespacedKey(plugin, "dd-frozen-mob");
		
		this.fixdata = new FixedMetadataValue(plugin, "protected");
		
		reload(plugin);
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		
		plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				Iterator<Entry<UUID, Integer>> it = mageWandCooldownMap.entrySet().iterator();
				while (it.hasNext()) {
					Entry<UUID, Integer> entry = it.next();
					entry.setValue(entry.getValue() - 1);
					if (entry.getValue() <= 0)
						it.remove();
				}
				it = voidBowCooldownMap.entrySet().iterator();
				while (it.hasNext()) {
					Entry<UUID, Integer> entry = it.next();
					entry.setValue(entry.getValue() - 1);
					if (entry.getValue() <= 0)
						it.remove();
				}
			}
		}, 0, 20);
		
//		if (plugin.dataFile.contains("data.favored") && !plugin.dataFile.getString("data.favored").equals("null"))
//			favoredDisaster = true;
//		if (plugin.dataFile.contains("data.disliked") && !plugin.dataFile.getString("data.disliked").equals("null"))
//			dislikedDisaster = true;
	}
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		UUID uuid = e.getPlayer().getUniqueId();
		plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
			@Override
			public void run() {
				if (worldSwap && (nonOpMsg || e.getPlayer().isOp())) {
					WorldObject obj = WorldObject.findWorldObject(e.getPlayer().getWorld());
					String tf = Utils.convertString("&c&l"+Languages.langFile.getString("internal.offWord"));
					if (obj.naturalAllowed)
						tf = Utils.convertString("&a&l"+Languages.langFile.getString("internal.onWord"));
					e.getPlayer().sendMessage(Utils.convertString(worldMessage.replace("%difficulty%", obj.difficulty.getLabel()).replace("%world%", obj.getWorld().getName())
							+ "\n&3- "+Languages.langFile.getString("internal.random_disasters")+": "+tf
							+ "\n&3- "+Languages.langFile.getString("internal.min_timer")+": &6"+(obj.timer)+" &7/ &3"+Languages.langFile.getString("internal.offset")+": &6"+(obj.offset)
							+ "\n&3- "+Languages.langFile.getString("internal.levelWord")+": &a"+(obj.table[0])+"% &2"+(obj.table[1])+"% &b"+(obj.table[2])+"% &e"+(obj.table[3])+"% &c"+(obj.table[4])+"% &4"+(obj.table[5])+"%"));
					e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.3F, 1);
				}
				if (e.getPlayer().hasPermission("deadlydisasters.updatenotify") && dataFile.getBoolean("data.firstStart")) {
					e.getPlayer().sendMessage(Utils.convertString(Languages.prefix+Languages.firstStart
							+ "\n&c"+Languages.langFile.getString("internal.allowFlight")));
					if (plugin.mcVersion < 1.16)
						e.getPlayer().sendMessage(Languages.prefix+Utils.convertString("&c"+Languages.langFile.getString("internal.olderVersion")));
				}
				if (!notified.contains(uuid) && e.getPlayer().hasPermission("deadlydisasters.updatenotify")) {
					if (plugin.firstAfterUpdate)
						e.getPlayer().sendMessage(Languages.prefix+Languages.joinAfterUpdate);
					if (plugin.updateNotify) {
						String msg = Languages.langFile.getString("internal.playerUpdate").replace("${plugin.page}", Main.pluginSpigotPage);
						e.getPlayer().sendMessage(Utils.convertString(Languages.prefix+"&a"+msg.substring(0, msg.indexOf('^'))+plugin.latestVersion+msg.substring(msg.indexOf('^')+1)+" &c"+plugin.getDescription().getVersion()));
					}
					notified.add(uuid);
				}
				if (warnForKick.contains(uuid)) {
					e.getPlayer().sendMessage(Utils.convertString(Languages.prefix+"&c"+Languages.langFile.getString("internal.allowFlight")));
					warnForKick.remove(uuid);
				}
				if (catalogNotifyBool && e.getPlayer().isOp() && !catalogNotify.contains(uuid)) {
					e.getPlayer().sendMessage(Languages.prefix+ChatColor.GREEN+Languages.langFile.getString("internal.catalogUpdate")+Utils.convertString(" &3(/disasters catalog)"));
					catalogNotify.add(uuid);
				}
//				if (e.getPlayer().isOp() && !joinedServer.contains(uuid)) {
//					if (!favoredDisaster)
//						e.getPlayer().sendMessage(Languages.prefix+ChatColor.AQUA+Languages.langFile.getString("internal.favorDisaster")+Utils.convertString(" &3(/disasters favor <disaster>)"));
//					if (!dislikedDisaster)
//						e.getPlayer().sendMessage(Languages.prefix+ChatColor.AQUA+Languages.langFile.getString("internal.dislikeDisaster")+Utils.convertString(" &3(/disasters dislike <disaster>)"));
//					joinedServer.add(uuid);
//				}
			}
		}, 10);
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				for (UUID worldUUID : tc.timer.keySet()) {
					WorldObject obj = WorldObject.findWorldObject(Bukkit.getWorld(worldUUID));
					if (obj == null)
						continue;
					if (dataFile.contains("timers."+worldUUID+"."+uuid))
						tc.timer.get(worldUUID).put(uuid, dataFile.getInt("timers."+worldUUID+"."+uuid));
					else
						tc.timer.get(worldUUID).put(uuid, rand.nextInt(obj.timer/2)+obj.timer);
				}
			}
		});
	}
	@EventHandler
	public void onLeave(PlayerQuitEvent e) {
		UUID uuid = e.getPlayer().getUniqueId();
		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				for (UUID worldUUID : tc.timer.keySet()) {
					dataFile.set("timers."+worldUUID+"."+uuid, tc.timer.get(worldUUID).get(uuid));
					tc.timer.get(worldUUID).remove(uuid);
				}
				plugin.saveDataFile();
				World world = e.getPlayer().getWorld();
				if (DestructionDisaster.currentLocations.containsKey(world) && DestructionDisaster.currentLocations.get(world).contains(e.getPlayer())) {
					DestructionDisaster.currentLocations.get(world).remove(e.getPlayer());
					if (DestructionDisaster.currentLocations.get(world).isEmpty())
						DestructionDisaster.currentLocations.remove(world);
				}
			}
		});
	}
	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e) {
		if (e.getMessage().equalsIgnoreCase("/weather clear")) {
			if (!e.getPlayer().hasPermission("deadlydisasters.clearweather")) return;
			Player p = e.getPlayer();
			Iterator<AcidStorm> acidStormIterator = DeathMessages.acidstorms.iterator();
			while (acidStormIterator.hasNext()) {
				AcidStorm storm = acidStormIterator.next();
				if (storm.getWorld().equals(p.getWorld())) {
					storm.clear();
					acidStormIterator.remove();
				}
			}
			Iterator<ExtremeWinds> extremeWindsIterator = DeathMessages.extremewinds.iterator();
			while (extremeWindsIterator.hasNext()) {
				ExtremeWinds storm = extremeWindsIterator.next();
				if (storm.getWorld().equals(p.getWorld())) {
					storm.clear();
					extremeWindsIterator.remove();
				}
			}
			Iterator<SoulStorm> soulstormIterator = DeathMessages.soulstorms.iterator();
			while (soulstormIterator.hasNext()) {
				SoulStorm storm = soulstormIterator.next();
				if (storm.getWorld().equals(p.getWorld())) {
					storm.clear();
					soulstormIterator.remove();
				}
			}
			Iterator<Blizzard> blizzardIterator = DeathMessages.blizzards.iterator();
			while (blizzardIterator.hasNext()) {
				Blizzard storm = blizzardIterator.next();
				if (storm.getWorld().equals(p.getWorld())) {
					storm.clear();
					blizzardIterator.remove();
				}
			}
			Iterator<SandStorm> sandstormIterator = DeathMessages.sandstorms.iterator();
			while (sandstormIterator.hasNext()) {
				SandStorm storm = sandstormIterator.next();
				if (storm.getWorld().equals(p.getWorld())) {
					storm.clear();
					sandstormIterator.remove();
				}
			}
			Iterator<MeteorShower> meteorshowerIterator = DeathMessages.meteorshowers.iterator();
			while (meteorshowerIterator.hasNext()) {
				MeteorShower storm = meteorshowerIterator.next();
				if (storm.getWorld().equals(p.getWorld())) {
					storm.clear();
					meteorshowerIterator.remove();
				}
			}
			Iterator<EndStorm> endstormIterator = DeathMessages.endstorms.iterator();
			while (endstormIterator.hasNext()) {
				EndStorm storm = endstormIterator.next();
				if (storm.getWorld().equals(p.getWorld())) {
					storm.clear();
					endstormIterator.remove();
				}
			}
			Iterator<Hurricane> hurricaneIterator = DeathMessages.hurricanes.iterator();
			while (hurricaneIterator.hasNext()) {
				Hurricane storm = hurricaneIterator.next();
				if (storm.world.equals(p.getWorld())) {
					storm.clear();
					hurricaneIterator.remove();
				}
			}
			Iterator<SolarStorm> solarstormIterator = DeathMessages.solarstorms.iterator();
			while (solarstormIterator.hasNext()) {
				SolarStorm storm = solarstormIterator.next();
				if (storm.world.equals(p.getWorld())) {
					storm.clear();
					solarstormIterator.remove();
				}
			}
		}
	}
	@EventHandler
	public void onBlockMelt(BlockFadeEvent event) {
		if (event.getBlock().getType().equals(Material.ICE)) {
			for (Entity e : event.getBlock().getWorld().getNearbyEntities(event.getBlock().getLocation().clone().add(.5,.5,.5), .5, 1.25, .5)) {
				if (!(e instanceof LivingEntity) || !e.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) continue;
				if (e.getHeight() > 1 && e.getLocation().add(0,1,0).getBlock().getType().equals(Material.ICE) && !(e.getLocation().add(0,1,0).getBlock().equals(event.getBlock()))) continue;
				e.setInvulnerable(false);
				((LivingEntity) e).setAI(true);
				if (e.getPersistentDataContainer().get(key, PersistentDataType.BYTE) == (byte) 0)
					((LivingEntity) e).setRemoveWhenFarAway(true);
				e.getPersistentDataContainer().remove(key);
				e.setSilent(false);
				return;
			}
		}
	}
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (Santa.snowGlobeBlocks.contains(event.getBlock()) || EasterBunny.easterBasketBlocks.contains(event.getBlock()) || PumpkinKing.pumpkinBasketBlocks.contains(event.getBlock())) {
			event.setCancelled(true);
			return;
		}
		if (event.getBlock().getType() == Material.ICE)
			Blizzard.breakIce(event.getBlock(), plugin);
		else if (AcidStorm.poisonedCrops.contains(event.getBlock())) {
			AcidStorm.poisonedCrops.remove(event.getBlock());
			event.setDropItems(false);
			event.setExpToDrop(0);
		}
	}
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent e) {
		if (!e.getItemInHand().hasItemMeta())
			return;
		if (e.getItemInHand().getItemMeta().getPersistentDataContainer().has(ItemsHandler.brokenSnowGlobeKey, PersistentDataType.BYTE)
				|| e.getItemInHand().getItemMeta().getPersistentDataContainer().has(ItemsHandler.spookyPumpkinKey, PersistentDataType.BYTE)
				|| e.getItemInHand().getItemMeta().getPersistentDataContainer().has(ItemsHandler.etherealLanternKey, PersistentDataType.BYTE))
			e.setCancelled(true);
		else if (e.getItemInHand().getItemMeta().getPersistentDataContainer().has(ItemsHandler.snowGlobeKey, PersistentDataType.BYTE)) {
			if (!(e.getBlockPlaced().getBlockData() instanceof Rotatable)) {
				e.getPlayer().sendMessage(Utils.convertString("&c"+Languages.langFile.getString("internal.placeGlobeError")));
				e.setCancelled(true);
				return;
			}
			Santa.summonSanta(plugin, e.getBlock());
		} else if (e.getItemInHand().getItemMeta().getPersistentDataContainer().has(ItemsHandler.easterBasketKey, PersistentDataType.BYTE)) {
			if (!(e.getBlockPlaced().getBlockData() instanceof Rotatable)) {
				e.getPlayer().sendMessage(Utils.convertString("&c"+Languages.langFile.getString("internal.placeGlobeError")));
				e.setCancelled(true);
				return;
			}
			if (!EasterBunny.summonEasterBunny(plugin, e.getBlock())) {
				e.setCancelled(true);
				e.getPlayer().sendMessage(Utils.convertString("&cCould not find possible spawn nearby!"));
				return;
			}
		} else if (e.getItemInHand().getItemMeta().getPersistentDataContainer().has(ItemsHandler.pumpkinBasketKey, PersistentDataType.BYTE)) {
			if (!(e.getBlockPlaced().getBlockData() instanceof Rotatable)) {
				e.getPlayer().sendMessage(Utils.convertString("&c"+Languages.getString("internal.placeGlobeError")));
				e.setCancelled(true);
				return;
			}
			if (!PumpkinKing.summonPumpkinKing(plugin, e.getBlock())) {
				e.setCancelled(true);
				e.getPlayer().sendMessage(Utils.convertString("&cCould not find possible spawn nearby!"));
				return;
			}
		}
	}
	@EventHandler
	public void onPistonExtend(BlockPistonExtendEvent e) {
		for (Block b : e.getBlocks())
			if (Santa.snowGlobeBlocks.contains(b) || EasterBunny.easterBasketBlocks.contains(b) || PumpkinKing.pumpkinBasketBlocks.contains(b)) {
				e.setCancelled(true);
				return;
			}
	}
	@EventHandler
	public void onPistonRetract(BlockPistonRetractEvent e) {
		for (Block b : e.getBlocks())
			if (Santa.snowGlobeBlocks.contains(b) || EasterBunny.easterBasketBlocks.contains(b) || PumpkinKing.pumpkinBasketBlocks.contains(b)) {
				e.setCancelled(true);
				return;
			}
	}
	@EventHandler
	public void onBlockForm(EntityChangeBlockEvent event) {
		if (!(event.getEntity() instanceof FallingBlock)) return;
		FallingBlock fb = (FallingBlock) event.getEntity();
		Block block = event.getBlock();
		if (fb.hasMetadata("dd-fb")) {
			if (Utils.isZoneProtected(fb.getLocation())) {
				event.setCancelled(true);
				return;
			}
			if (plugin.CProtect)
				Utils.getCoreProtect().logPlacement("Deadly-Disasters", block.getLocation(), event.getTo(), event.getBlockData());
			if (blockInventories.containsKey(fb)) {
				plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
					public void run() {
						((InventoryHolder) block.getState()).getInventory().setContents(blockInventories.get(fb));
						blockInventories.remove(fb);
					}
				}, 1);
			}
			if (fallingBlocks.containsKey(fb.getUniqueId())) {
				DisasterEvent disaster = fallingBlocks.get(fb.getUniqueId());
				if (disaster instanceof MeteorShower) {
					if (fb.hasMetadata("dd-md")) {
						event.setCancelled(true);
						fb.remove();
					}
				}
				fallingBlocks.remove(fb.getUniqueId());
			}
		} else if (fb.hasMetadata("dd-fbcancel"))
			event.setCancelled(true);
	}
	public static void addBlockInventory(Entity e, ItemStack[] i) {
		blockInventories.put(e, i);
	}
	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		if (e.getItem() == null || !e.getItem().hasItemMeta())
			return;
		if (e.getItem().getType() == Material.BLAZE_ROD && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) && !mageWandCooldownMap.containsKey(e.getPlayer().getUniqueId()) &&
				e.getItem().getItemMeta().hasLore() && ((plugin.customNameSupport && e.getItem().getItemMeta().getLore().get(0).equals(ItemsHandler.mageWandLore)) || e.getItem().getItemMeta().getPersistentDataContainer().has(ItemsHandler.mageWandKey, PersistentDataType.BYTE))) {
			mageWandCooldownMap.put(e.getPlayer().getUniqueId(), ItemsHandler.mageWandCooldown);
			ShulkerBullet[] bullets = new ShulkerBullet[12];
			double[] speed = {0.05, 0};
			Player p = e.getPlayer();
			World world = p.getWorld();
			for (int i=0; i < 6; i++) {
				bullets[i] = (ShulkerBullet) world.spawnEntity(p.getLocation().clone().add(new Vector(1, 1.6, 0).rotateAroundY(i).normalize()), EntityType.SHULKER_BULLET);
				bullets[i].setShooter(p);
				bullets[i].setMetadata("dd-magebullet", fixdata);
			}
			for (int i=6; i < 12; i++) {
				bullets[i] = (ShulkerBullet) world.spawnEntity(p.getLocation().clone().add(new Vector(1, 1, 0).rotateAroundY(i+0.5).normalize()), EntityType.SHULKER_BULLET);
				bullets[i].setShooter(p);
				bullets[i].setMetadata("dd-magebullet", fixdata);
			}
			p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 0.75f, 0.5f);
			new RepeatingTask(plugin, 0, 1) {
				public void run() {
					speed[0] += 0.09;
					double yVel = p.getLocation().getY()+1;
					Location loc = p.getLocation();
					for (int i=0; i < 6; i++) {
						Location temp = bullets[i].getLocation();
						temp.setY(yVel+0.3);
						bullets[i].teleport(temp);
						bullets[i].setVelocity(new Vector(temp.getX() - loc.getX(), 0, temp.getZ() - loc.getZ()).rotateAroundY(1.2).normalize().multiply(speed[0]).setY(0.04));
					}
					for (int i=6; i < 12; i++) {
						Location temp = bullets[i].getLocation();
						temp.setY(yVel-0.3);
						bullets[i].teleport(temp);
						bullets[i].setVelocity(new Vector(temp.getX() - loc.getX(), 0, temp.getZ() - loc.getZ()).rotateAroundY(-1.2).normalize().multiply(speed[0]).setY(0.04));
					}
					speed[1]++;
					if (speed[1] > 20) {
						for (int i=0; i < 12; i++) {
							Location temp = bullets[i].getLocation();
							bullets[i].setVelocity(new Vector(temp.getX() - loc.getX(), 0, temp.getZ() - loc.getZ()).normalize().setY(0.05));
						}
						cancel();
					}
				}
			};
		}
		if (DependencyUtils.isUIFrameworkEnabled())
			for (NamespacedKey key : e.getItem().getItemMeta().getPersistentDataContainer().getKeys())
				if (ItemsHandler.compatibilityMap.containsKey(key)) {
					com.github.jewishbanana.uiframework.items.ItemType type = com.github.jewishbanana.uiframework.items.ItemType.getItemType(ItemsHandler.compatibilityMap.get(key));
					if (type != null)
						e.getPlayer().getInventory().setItem(e.getHand(), type.getBuilder().getItem());
					break;
				}
	}
	@EventHandler
	public void onTradeAquire(VillagerAcquireTradeEvent e) {
		if (!DependencyUtils.isUIFrameworkEnabled() || e.getEntity() instanceof WanderingTrader)
			return;
		if (((Villager) e.getEntity()).getProfession() == Villager.Profession.LIBRARIAN && ((Villager) e.getEntity()).getVillagerLevel() >= 3 && rand.nextDouble()*100 < plugin.getConfig().getDouble("customitems.items.basic_coating_book.librarian_trade_chance")) {
			MerchantRecipe newRecipe = new MerchantRecipe(com.github.jewishbanana.uiframework.items.ItemType.getItemType(BasicCoatingBook.REGISTERED_KEY).getBuilder().getItem(), rand.nextInt(3)+1);
			newRecipe.addIngredient(new ItemStack(Material.EMERALD, rand.nextInt(60)+5));
			newRecipe.addIngredient(new ItemStack(Material.BOOK, 1));
			newRecipe.setVillagerExperience(20);
			e.setRecipe(newRecipe);
			return;
		} else if (((Villager) e.getEntity()).getProfession() == Villager.Profession.CLERIC && rand.nextDouble()*100 < plugin.getConfig().getDouble("customitems.items.plague_cure.cleric_trade_chance")) {
			MerchantRecipe newRecipe = new MerchantRecipe(com.github.jewishbanana.uiframework.items.ItemType.getItemType(PlagueCure.REGISTERED_KEY).getBuilder().getItem(), rand.nextInt(5)+8);
			if (rand.nextInt(5) == 0)
				newRecipe = new MerchantRecipe(com.github.jewishbanana.uiframework.items.ItemType.getItemType(SplashPlagueCure.REGISTERED_KEY).getBuilder().getItem(), rand.nextInt(5)+8);
			newRecipe.addIngredient(new ItemStack(Material.EMERALD, rand.nextInt(6)+3));
			if (rand.nextInt(3) == 0)
				newRecipe.addIngredient(new ItemStack(Material.INK_SAC, rand.nextInt(3)+1));
			newRecipe.setVillagerExperience(10);
			e.setRecipe(newRecipe);
			return;
		}
	}
	@EventHandler
	public void onPortal(EntityPortalEnterEvent e) {
		if (e.getEntity().hasMetadata("dd-endstormentity"))
			e.getEntity().remove();
	}
	@EventHandler
	public void onBurn(EntityCombustEvent e) {
		if (e.getEntity().hasMetadata("dd-unburnable"))
			e.setCancelled(true);
	}
	@EventHandler
	public void onExplode(EntityExplodeEvent e) {
		for (Block b : e.blockList())
			if (Santa.snowGlobeBlocks.contains(b) || EasterBunny.easterBasketBlocks.contains(b) || PumpkinKing.pumpkinBasketBlocks.contains(b))
				e.blockList().remove(b);
		if (!fireBalls.containsKey(e.getEntity()))
			return;
		for (Block b : e.blockList()) {
			if (Utils.isZoneProtected(b.getLocation()) || Utils.passStrengthTest(b.getType()))
				continue;
			if (plugin.CProtect)
				Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
			b.setType(Material.AIR);
		}
		e.blockList().clear();
	}
	@EventHandler
	public void worldInit(WorldInitEvent e) {
		if (!WorldObject.yamlFile.getKeys(false).contains(e.getWorld().getName())) {
			plugin.createWorldSection(e.getWorld().getName(), WorldObject.yamlFile);
			WorldObject.saveYamlFile(plugin);
		}
		WorldObject.worlds.add(new WorldObject(e.getWorld(), plugin));
		if (!plugin.dataFile.contains("timers."+e.getWorld().getUID()))
			plugin.dataFile.createSection("timers."+e.getWorld().getUID());
		plugin.saveDataFile();
		if (!tc.timer.containsKey(e.getWorld().getUID()))
			tc.timer.put(e.getWorld().getUID(), new HashMap<UUID,Integer>());
	}
	@EventHandler
	public void onTeleport(PlayerTeleportEvent e) {
		if (e.getFrom().getWorld().equals(e.getTo().getWorld())) return;
		WorldObject obj = WorldObject.findWorldObject(e.getTo().getWorld());
		if (worldSwap && (nonOpMsg || e.getPlayer().isOp()))
			plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
				@Override
				public void run() {
					String tf = Utils.convertString("&c&l"+Languages.langFile.getString("internal.offWord"));
					if (obj.naturalAllowed)
						tf = Utils.convertString("&a&l"+Languages.langFile.getString("internal.onWord"));
					e.getPlayer().sendMessage(Utils.convertString(worldMessage.replace("%difficulty%", obj.difficulty.getLabel()).replace("%world%", e.getTo().getWorld().getName())
							+ "\n&3- "+Languages.langFile.getString("internal.random_disasters")+": "+tf
							+ "\n&3- "+Languages.langFile.getString("internal.min_timer")+": &6"+(obj.timer)+" &7/ &3"+Languages.langFile.getString("internal.offset")+": &6"+(obj.offset)
							+ "\n&3- "+Languages.langFile.getString("internal.levelWord")+": &a"+(obj.table[0])+"% &2"+(obj.table[1])+"% &b"+(obj.table[2])+"% &e"+(obj.table[3])+"% &c"+(obj.table[4])+"% &4"+(obj.table[5])+"%"));
					e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.3F, 1);
				}
			}, 10);
	}
	@EventHandler
	public void onKick(PlayerKickEvent e) {
		if (e.getReason().equals("Flying is not enabled on this server") && e.getPlayer().isOp())
			warnForKick.add(e.getPlayer().getUniqueId());
	}
	public static void reload(Main plugin) {
		worldSwap = plugin.getConfig().getBoolean("messages.misc.world_messages.allow_world_messages");
		nonOpMsg = plugin.getConfig().getBoolean("messages.misc.world_messages.show_world_messages_to_not_opped");
		worldMessage = Utils.convertString(Languages.prefix+plugin.getConfig().getString("messages.misc.world_messages.message"));
	}
}
