package deadlydisasters.listeners;

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
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Rotatable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
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
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import deadlydisasters.disasters.AcidStorm;
import deadlydisasters.disasters.BlackPlague;
import deadlydisasters.disasters.Blizzard;
import deadlydisasters.disasters.EndStorm;
import deadlydisasters.disasters.MeteorShower;
import deadlydisasters.disasters.events.DestructionDisaster;
import deadlydisasters.disasters.events.DisasterEvent;
import deadlydisasters.entities.christmasentities.Santa;
import deadlydisasters.entities.easterentities.EasterBunny;
import deadlydisasters.general.ItemsHandler;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;
import deadlydisasters.general.TimerCheck;
import deadlydisasters.general.WorldObject;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public class CoreListener implements Listener {
	
	private Main plugin;
	private Queue<UUID> notified = new ArrayDeque<>();
	private Queue<UUID> warnForKick = new ArrayDeque<>();
	private FileConfiguration dataFile;
	private Random rand;
	private NamespacedKey key;
	private boolean favoredDisaster,dislikedDisaster;
	
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
		
		if (plugin.dataFile.contains("data.favored") && !plugin.dataFile.getString("data.favored").equals("null"))
			favoredDisaster = true;
		if (plugin.dataFile.contains("data.disliked") && !plugin.dataFile.getString("data.disliked").equals("null"))
			dislikedDisaster = true;
	}
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		UUID uuid = e.getPlayer().getUniqueId();
		plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
			@Override
			public void run() {
				if (worldSwap && (nonOpMsg || e.getPlayer().isOp())) {
					WorldObject obj = WorldObject.findWorldObject(e.getPlayer().getWorld());
					String tf = Utils.chat("&c&l"+Languages.langFile.getString("internal.offWord"));
					if (obj.naturalAllowed)
						tf = Utils.chat("&a&l"+Languages.langFile.getString("internal.onWord"));
					e.getPlayer().sendMessage(Utils.chat(worldMessage.replace("%difficulty%", obj.difficulty.getLabel()).replace("%world%", obj.getWorld().getName())
							+ "\n&3- "+Languages.langFile.getString("internal.random_disasters")+": "+tf
							+ "\n&3- "+Languages.langFile.getString("internal.min_timer")+": &6"+(obj.timer)+" &7/ &3"+Languages.langFile.getString("internal.offset")+": &6"+(obj.offset)
							+ "\n&3- "+Languages.langFile.getString("internal.levelWord")+": &a"+(obj.table[0])+"% &2"+(obj.table[1])+"% &b"+(obj.table[2])+"% &e"+(obj.table[3])+"% &c"+(obj.table[4])+"% &4"+(obj.table[5])+"%"));
					e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.3F, 1);
				}
				if (e.getPlayer().hasPermission("deadlydisasters.updatenotify") && dataFile.getBoolean("data.firstStart")) {
					e.getPlayer().sendMessage(Utils.chat(Languages.prefix+Languages.firstStart
							+ "\n&c"+Languages.langFile.getString("internal.allowFlight")));
					if (plugin.mcVersion < 1.16)
						e.getPlayer().sendMessage(Languages.prefix+Utils.chat("&c"+Languages.langFile.getString("internal.olderVersion")));
				}
				if (!notified.contains(uuid) && e.getPlayer().hasPermission("deadlydisasters.updatenotify")) {
					if (plugin.firstAfterUpdate)
						e.getPlayer().sendMessage(Languages.prefix+Languages.joinAfterUpdate);
					if (plugin.updateNotify) {
						String msg = Languages.langFile.getString("internal.playerUpdate");
						e.getPlayer().sendMessage(Utils.chat(Languages.prefix+"&a"+msg.substring(0, msg.indexOf('^'))+plugin.latestVersion+msg.substring(msg.indexOf('^')+1)+"&c"+plugin.getDescription().getVersion()));
					}
					notified.add(uuid);
				}
				if (warnForKick.contains(uuid)) {
					e.getPlayer().sendMessage(Utils.chat(Languages.prefix+"&c"+Languages.langFile.getString("internal.allowFlight")));
					warnForKick.remove(uuid);
				}
				if (catalogNotifyBool && e.getPlayer().isOp() && !catalogNotify.contains(uuid)) {
					e.getPlayer().sendMessage(Languages.prefix+ChatColor.GREEN+Languages.langFile.getString("internal.catalogUpdate")+Utils.chat(" &3(/disasters catalog)"));
					catalogNotify.add(uuid);
				}
				if (e.getPlayer().isOp() && !joinedServer.contains(uuid)) {
					if (!favoredDisaster)
						e.getPlayer().sendMessage(Languages.prefix+ChatColor.AQUA+Languages.langFile.getString("internal.favorDisaster")+Utils.chat(" &3(/disasters favor <disaster>)"));
					if (!dislikedDisaster)
						e.getPlayer().sendMessage(Languages.prefix+ChatColor.AQUA+Languages.langFile.getString("internal.dislikeDisaster")+Utils.chat(" &3(/disasters dislike <disaster>)"));
					joinedServer.add(uuid);
				}
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
			DeathMessages.acidstorms.stream().forEach(i -> {
				if (i.getWorld().equals(p.getWorld()))
					i.clear();
			});
			DeathMessages.extremewinds.stream().forEach(i -> {
				if (i.getWorld().equals(p.getWorld()))
					i.clear();
			});
			DeathMessages.soulstorms.stream().forEach(i -> {
				if (i.getWorld().equals(p.getWorld()))
					i.clear();
			});
			DeathMessages.blizzards.stream().forEach(i -> {
				if (i.getWorld().equals(p.getWorld()))
					i.clear();
			});
			DeathMessages.sandstorms.stream().forEach(i -> {
				if (i.getWorld().equals(p.getWorld()))
					i.clear();
			});
			DeathMessages.meteorshowers.stream().forEach(i -> {
				if (i.getWorld().equals(p.getWorld()))
					i.clear();
			});
			DeathMessages.endstorms.stream().forEach(i -> {
				if (i.getWorld().equals(p.getWorld()))
					i.clear();
			});
			DeathMessages.hurricanes.stream().forEach(i -> {
				if (i.world.equals(p.getWorld()))
					i.clear();
			});
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
		if (Santa.snowGlobeBlocks.contains(event.getBlock()) || EasterBunny.easterBasketBlocks.contains(event.getBlock())) {
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
		if (e.getItemInHand().getItemMeta().getPersistentDataContainer().has(ItemsHandler.brokenSnowGlobeKey, PersistentDataType.BYTE))
			e.setCancelled(true);
		else if (e.getItemInHand().getItemMeta().getPersistentDataContainer().has(ItemsHandler.snowGlobeKey, PersistentDataType.BYTE)) {
			if (!(e.getBlockPlaced().getBlockData() instanceof Rotatable)) {
				e.getPlayer().sendMessage(Utils.chat("&c"+Languages.langFile.getString("internal.placeGlobeError")));
				e.setCancelled(true);
				return;
			}
			Santa.summonSanta(plugin, e.getBlock());
		} else if (e.getItemInHand().getItemMeta().getPersistentDataContainer().has(ItemsHandler.easterBasketKey, PersistentDataType.BYTE)) {
			if (!(e.getBlockPlaced().getBlockData() instanceof Rotatable)) {
				e.getPlayer().sendMessage(Utils.chat("&c"+Languages.langFile.getString("internal.placeGlobeError")));
				e.setCancelled(true);
				return;
			}
			if (!EasterBunny.summonEasterBunny(plugin, e.getBlock())) {
				e.setCancelled(true);
				e.getPlayer().sendMessage(Utils.chat("&cCould not find possible spawn nearby!"));
				return;
			}
		}
	}
	@EventHandler
	public void onPistonExtend(BlockPistonExtendEvent e) {
		for (Block b : e.getBlocks())
			if (Santa.snowGlobeBlocks.contains(b) || EasterBunny.easterBasketBlocks.contains(b)) {
				e.setCancelled(true);
				return;
			}
	}
	@EventHandler
	public void onPistonRetract(BlockPistonRetractEvent e) {
		for (Block b : e.getBlocks())
			if (Santa.snowGlobeBlocks.contains(b) || EasterBunny.easterBasketBlocks.contains(b)) {
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
		}
	}
	public static void addBlockInventory(Entity e, ItemStack[] i) {
		blockInventories.put(e, i);
	}
	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		if (e.getItem() == null || !e.getItem().hasItemMeta())
			return;
		Material type = e.getItem().getType();
		if (type == Material.GHAST_TEAR && ((plugin.customNameSupport && e.getItem().getItemMeta().getDisplayName().equals(ItemsHandler.voidShardName)) || e.getItem().getItemMeta().getPersistentDataContainer().has(ItemsHandler.voidShardKey, PersistentDataType.BYTE))) {
			Location target = e.getPlayer().getEyeLocation().clone().add(e.getPlayer().getLocation().getDirection().multiply(6));
			if (target.getBlock().getType() != Material.AIR)
				return;
			EndStorm storm = new EndStorm(1);
			storm.createCustomRift(target);
			e.getItem().setAmount(e.getItem().getAmount()-1);
		} else if (type == Material.BLAZE_ROD && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) && !mageWandCooldownMap.containsKey(e.getPlayer().getUniqueId()) &&
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
	}
	@EventHandler
	public void bowShoot(EntityShootBowEvent e) {
		if (e.getForce() < 0.8 || !e.getBow().hasItemMeta()) return;
		ItemMeta meta = e.getBow().getItemMeta();
		if (!voidBowCooldownMap.containsKey(e.getEntity().getUniqueId()) && ((plugin.customNameSupport && meta.getDisplayName().equals(ItemsHandler.voidBowName)) || meta.getPersistentDataContainer().has(ItemsHandler.voidBowKey, PersistentDataType.BYTE))) {
			voidBowCooldownMap.put(e.getEntity().getUniqueId(), ItemsHandler.voidBowCooldown);
			Arrow arrow = (Arrow) e.getProjectile();
			arrow.setMetadata("dd-voidarrow", fixdata);
			arrow.setColor(Color.BLACK);
			new RepeatingTask(plugin, 0, 10) {
				@Override
				public void run() {
					if (arrow.isInBlock()) {
						if (!Utils.isZoneProtected(arrow.getLocation()) || plugin.getConfig().getBoolean("customitems.items.void_wrath.allow_in_regions"))
							EndStorm.createUnstableRift(arrow.getLocation().clone(), ItemsHandler.voidBowPortalTicks);
						cancel();
					} else if (arrow.isDead())
						cancel();
				}
			};
		}
	}
	@EventHandler
	public void onConsume(PlayerItemConsumeEvent e) {
		if (e.getItem().getType() != Material.POTION || !BlackPlague.time.containsKey(e.getPlayer().getUniqueId()))
			return;
		if (e.getItem().getItemMeta().getPersistentDataContainer().has(ItemsHandler.plagueCureKey, PersistentDataType.BYTE)) {
			BlackPlague.cureEntity(e.getPlayer(), plugin);
			e.getPlayer().sendMessage(ChatColor.GREEN+Languages.langFile.getString("misc.cureMessage"));
			return;
		}
	}
	@EventHandler
	public void onPotionSplash(PotionSplashEvent e) {
		if (e.getPotion().getItem().getItemMeta().getPersistentDataContainer().has(ItemsHandler.plagueCureKey, PersistentDataType.BYTE))
			for (LivingEntity entity : e.getAffectedEntities())
				if (BlackPlague.time.containsKey(entity.getUniqueId())) {
					BlackPlague.cureEntity(entity, plugin);
					if (entity instanceof Player)
						entity.sendMessage(ChatColor.GREEN+Languages.langFile.getString("misc.cureMessage"));
				}
	}
	@EventHandler
	public void onTradeAquire(VillagerAcquireTradeEvent e) {
		if (e.getEntity() instanceof WanderingTrader)
			return;
		if (((Villager) e.getEntity()).getProfession() == Villager.Profession.CLERIC && rand.nextDouble()*100 < 5.0) {
			MerchantRecipe newRecipe = new MerchantRecipe(ItemsHandler.plagueCure, rand.nextInt(5)+8);
			if (rand.nextInt(5) == 0)
				newRecipe = new MerchantRecipe(ItemsHandler.plagueCureSplash, rand.nextInt(5)+8);
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
			if (Santa.snowGlobeBlocks.contains(b) || EasterBunny.easterBasketBlocks.contains(b))
				e.blockList().remove(b);
	}
	@EventHandler
	public void worldInit(WorldInitEvent e) {
		if (!WorldObject.yamlFile.getKeys(false).contains(e.getWorld().getName())) {
			plugin.createWorldSection(e.getWorld().getName(), WorldObject.yamlFile);
			WorldObject.saveYamlFile(plugin);
		}
		WorldObject.worlds.add(new WorldObject(e.getWorld()));
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
					String tf = Utils.chat("&c&l"+Languages.langFile.getString("internal.offWord"));
					if (obj.naturalAllowed)
						tf = Utils.chat("&a&l"+Languages.langFile.getString("internal.onWord"));
					e.getPlayer().sendMessage(Utils.chat(worldMessage.replace("%difficulty%", obj.difficulty.getLabel()).replace("%world%", e.getTo().getWorld().getName())
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
		worldMessage = Utils.chat(Languages.prefix+plugin.getConfig().getString("messages.misc.world_messages.message"));
	}
}
