package com.github.jewishbanana.deadlydisasters.handlers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomDropsFactory;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.events.disasters.CaveIn;
import com.github.jewishbanana.deadlydisasters.utils.AsyncRepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.ChannelDataHolder;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;
import com.github.jewishbanana.uiframework.items.ItemType;

public class AchievementsHandler implements Listener {
	
	private Main plugin;
	private FileConfiguration file;
	public boolean isEnabled, announceToPlayers, broadcastToWorld, notifyPlayers;
	private ItemStack achievementInfo;
	private ItemStack noAchievement;
	private int maxMasteries;
	private String progress;
	private String rewards;
	
	private List<Achievement> achievementsList = new ArrayList<>();
	private Map<String, Achievement> achievementMap = new LinkedHashMap<>();
	private List<Mastery> masteriesList = new ArrayList<>();
	private Map<String, Mastery> masteryMap = new HashMap<>();
	
	private SurvivalChannel survivalChannel;
	private WeatherSurvivalChannel weatherSurvivalChannel;
	
	private Map<Inventory, Map<ItemStack, Achievement>> guiAchievements = new HashMap<>();
	private Map<Inventory, Map<ItemStack, Mastery>> guiMasteries = new HashMap<>();
	
	private Set<Player> twisterPlayers = ConcurrentHashMap.newKeySet();
	
	public AchievementsHandler(Main plugin) {
		this.plugin = plugin;
		if (!new File(plugin.getDataFolder().getAbsolutePath(), "achievements.yml").exists())
			createFile();
		file = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder().getAbsolutePath(), "achievements.yml"));
		if (!file.getBoolean("enabled")) {
			plugin.getLogger().info("Achievements are disabled!");
			return;
		}
		isEnabled = true;
		announceToPlayers = file.getBoolean("messages.announce_to_player");
		broadcastToWorld = file.getBoolean("messages.broadcast_to_world");
		notifyPlayers = file.getBoolean("messages.unclaimed_notify");
		maxMasteries = file.getInt("masteries.max_enabled");
		progress = Languages.getString("words.progress");
		rewards = Languages.getString("words.rewards");
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		achievementInfo = Utils.createItem(Material.NETHER_STAR, 1, "&6&lAchievements", Arrays.asList("&bHover above the items below to see your progress towards certain achievements!"," ",
				"&d&lMasteries &aare earned by progressing through tiers of an achievement, some are upgraded through tier progression. They provide various unique passive effects and abilities. You may have &e"+maxMasteries+" &amasteries active at a time.", " ", "&3Some achievements are locked and can't be progressed on while hidden! Although most can."), false, true);
		noAchievement = Utils.createItem(Material.BLACK_STAINED_GLASS_PANE, 1, "", null, false, true);
		for (String section : file.getConfigurationSection("achievements").getKeys(false))
			for (String type : file.getConfigurationSection("achievements."+section).getKeys(false))
				for (String name : file.getConfigurationSection("achievements."+section+'.'+type).getKeys(false)) {
					ItemStack displayItem = null;
					if (file.contains("display."+section+'.'+type+'.'+name)) {
						if (ItemsHandler.allItems.containsKey(file.getString("display."+section+'.'+type+'.'+name+".item")))
							displayItem = ItemsHandler.allItems.get(file.getString("display."+section+'.'+type+'.'+name+".item")).clone();
						else
							try {
								displayItem = new ItemStack(Material.valueOf(file.getString("display."+section+'.'+type+'.'+name+".item")));
							} catch (IllegalArgumentException e) {
								Main.consoleSender.sendMessage(Languages.prefix+Utils.convertString("&eCould not add item display to achievement &d'"+name+"&d' &eno such item &c"+file.getString("display."+section+'.'+type+'.'+name+".item")+" &eexists!"));
							}
						if (displayItem != null) {
							if (file.contains("display."+section+'.'+type+'.'+name+".amount"))
								displayItem.setAmount(file.getInt("display."+section+'.'+type+'.'+name+".amount"));
							ItemMeta meta = displayItem.getItemMeta();
							if (meta.hasLore()) {
								List<String> lore = meta.getLore();
								lore.addAll(Arrays.asList(" ", Utils.convertString(file.getString("display."+section+'.'+type+'.'+name+".description"))));
								meta.setLore(lore);
							} else {
								List<String> lore = new ArrayList<>(Arrays.asList(" ", Utils.convertString(file.getString("display."+section+'.'+type+'.'+name+".description"))));
								meta.setLore(lore);
							}
							displayItem.setItemMeta(meta);
						}
					}
					Achievement ach = new Achievement("achievements."+section+'.'+type+'.'+name);
					ach.displayItem = displayItem;
					achievementMap.put(section+'.'+type+'.'+name, ach);
					achievementsList.add(ach);
					if (file.contains("masteries."+section+'.'+type+'.'+name) && file.getBoolean("masteries."+section+'.'+type+'.'+name+".enabled")) {
						Mastery mastery = new Mastery("masteries."+section+'.'+type+'.'+name, ach);
						ach.passMastery(mastery);
						masteryMap.put(section+'.'+type+'.'+name, mastery);
						masteriesList.add(mastery);
					}
				}
		survivalChannel = new SurvivalChannel(plugin);
		weatherSurvivalChannel = new WeatherSurvivalChannel(plugin);
		startTwisterTimer();
	}
	public void openGUI(Player player, int page) {
		UUID uuid = player.getUniqueId();
		Inventory inv = Bukkit.createInventory(null, 54, Utils.convertString("&9DeadlyDisasters Achievements"));
		inv.setItem(4, achievementInfo);
		Map<ItemStack, Achievement> itemMap = new HashMap<>();
		Map<ItemStack, Mastery> masteryMap = new HashMap<>();
		for (int i=0; i < 4; i++) {
			if (i+((page-1)*4) >= achievementsList.size())
				break;
			Achievement ach = achievementsList.get(i+((page-1)*4));
			for (int t=0; t < ach.tiers.length; t++) {
				Achievement tier = ach.tiers[t];
				if (t > 0 && tier.hidden && !tier.hasAchieved(uuid) && !ach.tiers[t-1].hasAchieved(uuid)) {
					inv.setItem(i*9+9+t, ach.hiddenItem);
					continue;
				}
				boolean achieved = tier.hasAchieved(uuid);
				List<String> lore = new ArrayList<>();
				lore.add(ach.series);
				lore.add(tier.description);
				lore.add(" ");
				lore.add(Utils.convertString("&b"+progress+": "+tier.getProgress(uuid)+'/'+tier.goal));
				lore.add(" ");
				lore.add(Utils.convertString("&3"+rewards+":"));
				tier.rewards.forEach((k,v) -> {
					if (k.hasItemMeta() && k.getItemMeta().hasDisplayName())
						lore.add(Utils.convertString("&d"+v+" &8x &6"+k.getItemMeta().getDisplayName().toLowerCase()));
					else
						lore.add(Utils.convertString("&d"+v+" &8x &6"+k.getType().toString().toLowerCase().replace('_', ' ')));
				});
				lore.add(Utils.convertString("&2XP: &a"+tier.xp));
				if (achieved && !tier.hasClaimed(uuid)) {
					lore.add(" ");
					lore.add(Utils.convertString("&b&l"+Languages.getString("internal.claimAchievement")));
				}
				ItemStack item = Utils.createItem(tier.item, tier.amount, tier.name, lore, achieved ? !tier.hasClaimed(uuid) : false, true);
				itemMap.put(item, tier);
				inv.setItem(i*9+9+t, item);
			}
			for (int s=ach.tiers.length; s < 9; s++)
				inv.setItem(i*9+9+s, noAchievement);
			if (ach.mastery != null) {
				ItemStack mastItem = ach.mastery.build(uuid);
				masteryMap.put(mastItem, ach.mastery);
				inv.setItem(i*9+17, mastItem);
			} else if (ach.displayItem != null)
				inv.setItem(i*9+17, ach.displayItem);
		}
		if (page > 1)
			inv.setItem(45, Utils.createItem(Material.ARROW, page-1, "&aPage "+(page-1), null, false, true));
		if (page*4 < achievementsList.size())
			inv.setItem(53, Utils.createItem(Material.ARROW, page+1, "&aPage "+(page+1), null, false, true));
		
		guiAchievements.put(inv, itemMap);
		guiMasteries.put(inv, masteryMap);
		player.openInventory(inv);
	}
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		if (notifyPlayers) {
			UUID uuid = e.getPlayer().getUniqueId();
			plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
				for (Achievement ach : achievementsList)
					for (Achievement tier : ach.tiers)
						if (tier.hasAchieved(uuid) && !tier.hasClaimed(uuid)) {
							if (e.getPlayer() != null)
								e.getPlayer().sendMessage(Languages.prefix+ChatColor.GREEN+Utils.convertString(Languages.getString("internal.notifyUnclaimed")));
							return;
						}
			}, 200);
		}
	}
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		if (e.getCurrentItem() == null || !guiAchievements.containsKey(e.getInventory()))
			return;
		e.setCancelled(true);
		if (e.getRawSlot() == 45 || e.getRawSlot() == 53) {
			String name = e.getCurrentItem().getItemMeta().getDisplayName();
			openGUI((Player) e.getWhoClicked(), Integer.parseInt(name.substring(name.indexOf(' ')+1)));
			return;
		} else if (guiAchievements.get(e.getInventory()).containsKey(e.getCurrentItem())) {
			Achievement tier = guiAchievements.get(e.getInventory()).get(e.getCurrentItem());
			if (tier.hasAchieved(e.getWhoClicked().getUniqueId()) && !tier.hasClaimed(e.getWhoClicked().getUniqueId())) {
				tier.claimRewards((Player) e.getWhoClicked());
				((Player) e.getWhoClicked()).playSound(e.getWhoClicked().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
				ItemMeta meta = e.getCurrentItem().getItemMeta();
				List<String> lore = meta.getLore();
				lore.remove(lore.size()-1);
				lore.remove(lore.size()-1);
				meta.setLore(lore);
				meta.removeEnchant(VersionUtils.getUnbreaking());
				e.getCurrentItem().setItemMeta(meta);
				return;
			}
		} else if (guiMasteries.get(e.getInventory()).containsKey(e.getCurrentItem())) {
			Mastery mastery = guiMasteries.get(e.getInventory()).get(e.getCurrentItem());
			UUID uuid = e.getWhoClicked().getUniqueId();
			if (mastery.hasActive(uuid)) {
				mastery.activePlayers.remove(uuid);
				((Player) e.getWhoClicked()).playSound(e.getWhoClicked().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.5f);
				ItemMeta meta = e.getCurrentItem().getItemMeta();
				List<String> lore = meta.getLore();
				lore.remove(lore.size()-1);
				lore.add(Utils.convertString("&a"+Languages.getString("internal.enableMastery")));
				meta.setLore(lore);
				meta.removeEnchant(VersionUtils.getUnbreaking());
				e.getCurrentItem().setItemMeta(meta);
				guiMasteries.get(e.getInventory()).put(e.getCurrentItem(), mastery);
				return;
			} else if (mastery.canUse(uuid)) {
				int i=0;
				for (Mastery m : masteriesList)
					if (m.hasActive(uuid)) {
						i++;
						if (i >= maxMasteries)
							return;
					}
				mastery.activePlayers.add(uuid);
				((Player) e.getWhoClicked()).playSound(e.getWhoClicked().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
				ItemMeta meta = e.getCurrentItem().getItemMeta();
				List<String> lore = meta.getLore();
				lore.remove(lore.size()-1);
				lore.add(Utils.convertString("&b"+Languages.getString("internal.deactivateMastery")));
				meta.setLore(lore);
				meta.addEnchant(VersionUtils.getUnbreaking(), 1, true);
				meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
				e.getCurrentItem().setItemMeta(meta);
				guiMasteries.get(e.getInventory()).put(e.getCurrentItem(), mastery);
				return;
			}
		}
	}
	@EventHandler
	public void onClose(InventoryCloseEvent e) {
		if (guiAchievements.containsKey(e.getInventory())) {
			guiAchievements.remove(e.getInventory());
			guiMasteries.remove(e.getInventory());
		}
	}
	@EventHandler
	public void onDamage(EntityDamageEvent e) {
		if (e.isCancelled() || !(e.getEntity() instanceof Player))
			return;
		if (e.getCause() == DamageCause.FALL && isMasteryActive(e.getEntity().getUniqueId(), "disasters.survival.sinkhole")) {
			Mastery mastery = masteryMap.get("disasters.survival.sinkhole");
			Player player = (Player) e.getEntity();
			e.setDamage(e.getDamage()-((e.getDamage()/100.0)*(mastery.getAbilityPower(player.getUniqueId()))));
			if (e.getFinalDamage() >= player.getHealth() && player.getHealth() >= player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()/100*80 && getMasteryTier(player.getUniqueId(), "disasters.survival.sinkhole") == 3) {
				e.setCancelled(true);
				player.damage(0.0001);
				player.setHealth(0.5D);
				player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.1f, 2f);
			}
			return;
		} else if ((e.getCause() == DamageCause.ENTITY_EXPLOSION || e.getCause() == DamageCause.BLOCK_EXPLOSION) && isMasteryActive(e.getEntity().getUniqueId(), "disasters.survival.supernova")) {
			Mastery mastery = masteryMap.get("disasters.survival.supernova");
			Player player = (Player) e.getEntity();
			e.setDamage(e.getDamage()-((e.getDamage()/100.0)*(mastery.getAbilityPower(player.getUniqueId()))));
			return;
		} else if (e.getCause() == DamageCause.FALLING_BLOCK && isMasteryActive(e.getEntity().getUniqueId(), "disasters.survival.cavein")) {
			Mastery mastery = masteryMap.get("disasters.survival.cavein");
			Player player = (Player) e.getEntity();
			e.setDamage(e.getDamage()-((e.getDamage()/100.0)*(mastery.getAbilityPower(player.getUniqueId()))));
			return;
		}
	}
	@EventHandler
	public void onEntityDamage(EntityDamageByEntityEvent e) {
		if (e.isCancelled() || !(e.getEntity() instanceof LivingEntity))
			return;
		if (e.getDamager() instanceof Player && isMasteryActive(e.getDamager().getUniqueId(), "mobs.slayer.void_mobs") && (e.getEntity().getType() == EntityType.ENDERMAN || e.getEntity().getType() == EntityType.ENDERMITE
				|| e.getEntity().getPersistentDataContainer().has(CustomEntityType.ENDTOTEM.nameKey, PersistentDataType.BYTE) || e.getEntity().getPersistentDataContainer().has(CustomEntityType.BABYENDTOTEM.nameKey, PersistentDataType.BYTE)
				|| e.getEntity().getPersistentDataContainer().has(CustomEntityType.VOIDARCHER.nameKey, PersistentDataType.BYTE) || e.getEntity().getPersistentDataContainer().has(CustomEntityType.VOIDGUARDIAN.nameKey, PersistentDataType.BYTE)
				|| e.getEntity().getPersistentDataContainer().has(CustomEntityType.VOIDSTALKER.nameKey, PersistentDataType.BYTE)))
			e.setDamage(e.getDamage()+((e.getDamage()/100.0)*masteryMap.get("mobs.slayer.void_mobs").getAbilityPower(e.getDamager().getUniqueId())));
		if (e.getFinalDamage() >= ((LivingEntity) e.getEntity()).getHealth() && e.getEntity().getPersistentDataContainer().has(CustomEntityType.ENDTOTEM.nameKey, PersistentDataType.BYTE)
				&& e.getDamager().getPersistentDataContainer().has(CustomEntityType.BABYENDTOTEM.nameKey, PersistentDataType.BYTE) && ((Tameable) e.getDamager()).getOwner() != null)
			awardProgress(((Tameable) e.getDamager()).getOwner().getUniqueId(), "master.series.void_master", 1, 6);
		if (e.getEntity() instanceof Player) {
			if (plugin.random.nextInt(10) == 0 && e.getDamager().getLocation().getBlockY() < 50 && isMasteryActive(e.getEntity().getUniqueId(), "disasters.survival.cavein") && getMasteryTier(e.getEntity().getUniqueId(), "disasters.survival.cavein") == 3)
				plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
					if (e.getEntity().getLocation().distanceSquared(e.getDamager().getLocation()) >= 36 && e.getDamager().getLocation().getBlockY() < 50) {
						Block b = e.getDamager().getLocation().add(0,2,0).getBlock();
						for (int i=0; i < 10; i++) {
							if (!b.isPassable()) {
								CaveIn cavein = new CaveIn(1, b.getWorld());
								cavein.setRadius(plugin.random.nextInt(2)+2);
								cavein.setDepth(plugin.random.nextInt(3)+1);
								cavein.start(b.getLocation(), (Player) e.getEntity());
								break;
							}
							b = b.getRelative(BlockFace.UP);
						}
					}
				}, plugin.random.nextInt(40)+40);
		}
	}
	@EventHandler
	public void onEntityDeath(EntityDeathEvent e) {
		if (e.getEntity().getKiller() == null || Utils.isPlayerImmune(e.getEntity().getKiller()))
			return;
		LivingEntity entity = e.getEntity();
		if (entity.getPersistentDataContainer().has(CustomEntityType.VOIDSTALKER.nameKey, PersistentDataType.BYTE)) {
			achievementMap.get("master.series.void_master").addProgress(entity.getKiller().getUniqueId(), 1, 1);
			achievementMap.get("mobs.slayer.void_mobs").addProgress(entity.getKiller().getUniqueId(), 1);
		} else if (entity.getPersistentDataContainer().has(CustomEntityType.BABYENDTOTEM.nameKey, PersistentDataType.BYTE) || entity.getPersistentDataContainer().has(CustomEntityType.ENDTOTEM.nameKey, PersistentDataType.BYTE)
				|| entity.getPersistentDataContainer().has(CustomEntityType.VOIDARCHER.nameKey, PersistentDataType.BYTE) || entity.getPersistentDataContainer().has(CustomEntityType.VOIDGUARDIAN.nameKey, PersistentDataType.BYTE))
			achievementMap.get("mobs.slayer.void_mobs").addProgress(entity.getKiller().getUniqueId(), 1);
	}
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (isMasteryActive(e.getEntity().getUniqueId(), "disasters.survival.supernova") && getMasteryTier(e.getEntity().getUniqueId(), "disasters.survival.supernova") == 3) {
			Location loc = e.getEntity().getLocation();
			loc.getWorld().playSound(loc, Sound.AMBIENT_NETHER_WASTES_MOOD, 2f, 0.5f);
			BlockVector block = new BlockVector(loc.getX(), loc.getY(), loc.getZ());
			World world = loc.getWorld();
			int[] tick = {0, 0};
			final int size = 10;
			final Material[] materials = new Material[]{Material.OBSIDIAN, Material.BLACK_CONCRETE, Material.FIRE};
			new RepeatingTask(plugin, 0, 1) {
				@Override
				public void run() {
					tick[0]++;
					if (tick[0] < 60) {
						loc.getWorld().spawnParticle(Particle.FLASH, loc, 1, .5, .5, .5, 0.001);
						return;
					}
					if (tick[1] > size) {
						cancel();
						return;
					}
					for (int x = -tick[1]; x < tick[1]; x++)
						for (int y = -tick[1]; y < tick[1]; y++)
							for (int z = -tick[1]; z < tick[1]; z++) {
								Vector position = block.clone().add(new Vector(x, y, z));
								if (!(block.distance(position) >= (tick[1] - 1) && block.distance(position) <= tick[1])) continue;
								Block b = world.getBlockAt(position.toLocation(world));
								if (plugin.random.nextInt(8) == 0)
									world.spawnParticle(VersionUtils.getLargeExplosion(), b.getLocation(), 1, 0, 0, 0, 1, null, true);
								if (b.getType() == Material.AIR || Utils.isBlockImmune(b.getType()) || Utils.isZoneProtected(b.getLocation()))
									continue;
								if (plugin.CProtect)
									Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
								if (tick[1] > size-1 && plugin.random.nextInt(8) == 0) {
									Material mat = materials[plugin.random.nextInt(materials.length)];
									if (plugin.CProtect)
										Utils.getCoreProtect().logPlacement("Deadly-Disasters", b.getLocation(), mat, mat.createBlockData());
									b.setType(mat);
									continue;
								}
								b.setType(Material.AIR);
							}
					tick[1]++;
					if (tick[0] % 10 == 0)
						for (Entity e : world.getNearbyEntities(loc, size+15, size+15, size+15))
							if (e instanceof LivingEntity && !e.isDead()) {
								if (loc.distance(e.getLocation()) < tick[1]) {
									if (Utils.isZoneProtected(e.getLocation()) || (e instanceof Player && Utils.isPlayerImmune((Player) e)))
										continue;
									Utils.pureDamageEntity((LivingEntity) e, 20.0, "dd-supernova", true, null);
								} else if (e instanceof Player) {
									Location temp = e.getLocation().add(Utils.getVectorTowards(e.getLocation(), loc).multiply(4.0));
									float vol = (float) ((2 - (0.0005 * (loc.distance(e.getLocation())) - tick[1]))*0.5);
									if (plugin.mcVersion >= 1.16)
										((Player) e).playSound(temp, Sound.AMBIENT_WARPED_FOREST_MOOD, vol, 0.5F);
									((Player) e).playSound(temp, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, vol, 0.5F);
									((Player) e).playSound(temp, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, vol/30, 0.5F);
								}
							}
				}
			};
		}
	}
	@EventHandler(priority = EventPriority.LOWEST)
	public void onProjectileHit(ProjectileHitEvent e) {
		if (e.isCancelled() || e.getHitEntity() == null || !twisterPlayers.contains(e.getHitEntity()))
			return;
		e.setCancelled(true);
		Vector vec = e.getEntity().getVelocity();
		e.getEntity().setVelocity(new Vector(-vec.getX()/3, vec.getY(), -vec.getZ()/3));
	}
	@EventHandler
	public void onTarget(EntityTargetLivingEntityEvent e) {
		if (e.isCancelled() || !(e.getTarget() instanceof Player))
			return;
		if (plugin.random.nextInt(10) == 0 && e.getEntity().getLocation().getBlockY() < 50 && isMasteryActive(e.getTarget().getUniqueId(), "disasters.survival.cavein") && getMasteryTier(e.getTarget().getUniqueId(), "disasters.survival.cavein") == 3)
			plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
				if (e.getEntity().getLocation().distanceSquared(e.getTarget().getLocation()) >= 36 && e.getEntity().getLocation().getBlockY() < 50) {
					Block b = e.getEntity().getLocation().add(0,2,0).getBlock();
					for (int i=0; i < 10; i++) {
						if (!b.isPassable()) {
							CaveIn cavein = new CaveIn(1, b.getWorld());
							cavein.setRadius(plugin.random.nextInt(2)+2);
							cavein.setDepth(plugin.random.nextInt(3)+1);
							cavein.start(b.getLocation(), (Player) e.getTarget());
							break;
						}
						b = b.getRelative(BlockFace.UP);
					}
				}
			}, plugin.random.nextInt(80));
	}
	public void startTwisterTimer() {
		int[] sound = {0};
		new AsyncRepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				for (Player p : Bukkit.getOnlinePlayers())
					if (twisterPlayers.contains(p)) {
						if (p.isDead() || p.getHealth() > p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()/100*25 || Utils.isPlayerImmune(p)) {
							twisterPlayers.remove(p);
							continue;
						}
						for (int i=0; i < 15; i++) {
							Vector vec = new Vector(0.8+(plugin.random.nextDouble()/2),0,0).rotateAroundY(plugin.random.nextDouble()*6);
							Location loc = p.getLocation().add(vec);
							loc.setY(loc.getY()+plugin.random.nextDouble()*3-0.2);
							p.getWorld().spawnParticle(Particle.CLOUD, loc, 0, vec.getZ()/10, 0, -vec.getX()/10, 1);
							p.getWorld().spawnParticle(Particle.FALLING_WATER, loc, 0, vec.getZ()/10, 0, -vec.getX()/10, 1);
						}
						if (sound[0] == 20) {
							p.getWorld().playSound(p.getLocation().add(0,1,0), Sound.WEATHER_RAIN_ABOVE, 0.125f, 0.5f);
							if (plugin.random.nextInt(2) == 0) {
								p.getWorld().spawnParticle(Particle.FLASH, p.getLocation().add(new Vector(0.8+(plugin.random.nextDouble()/2),0,0).rotateAroundY(plugin.random.nextDouble()*6).setY(2.3)), 1, 0, 0, 0);
								p.getWorld().playSound(p.getLocation().add(0,2.5,0), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.15f, 1f);
							}
						}
					} else if (!Utils.isPlayerImmune(p) && p.getHealth() <= p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()/100*25 && !p.isDead() && isMasteryActive(p.getUniqueId(), "disasters.survival.tornado") && getMasteryTier(p.getUniqueId(), "disasters.survival.tornado") == 3) {
						twisterPlayers.add(p);
						for (int i=0; i < 100; i++) {
							Vector vec = new Vector(0.8+(plugin.random.nextDouble()/2),0,0).rotateAroundY(plugin.random.nextDouble()*6);
							Location loc = p.getLocation().add(vec);
							loc.setY(loc.getY()+plugin.random.nextDouble()*3-0.2);
							p.getWorld().spawnParticle(Particle.CLOUD, loc, 0, vec.getZ()/2, 0, -vec.getX()/2, 1);
						}
						p.getWorld().spawnParticle(Particle.FLASH, p.getLocation().add(new Vector(0.8+(plugin.random.nextDouble()/2),0,0).rotateAroundY(plugin.random.nextDouble()*6).setY(2.3)), 1, 0, 0, 0);
						p.getWorld().playSound(p.getLocation().add(0,2.5,0), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1f);
						plugin.getServer().getScheduler().runTask(plugin, () -> {
							for (Entity e : p.getNearbyEntities(4.0, 4.0, 4.0))
								if (!(e instanceof Player && Utils.isPlayerImmune((Player) e)) && !(e instanceof Tameable && ((Tameable) e).getOwner().equals(p)))
									e.setVelocity(Utils.getVectorTowards(e.getLocation().add(0,0.5,0), p.getLocation()).multiply(-1.0));
						});
					}
				if (sound[0]++ >= 20)
					sound[0] = 0;
			}
		};
	}
	public void awardProgress(UUID uuid, String achievement, int value) {
		if (isEnabled)
			achievementMap.get(achievement).addProgress(uuid, value);
	}
	public void awardProgress(UUID uuid, String achievement, int value, int tierIndex) {
		if (isEnabled)
			achievementMap.get(achievement).addProgress(uuid, value, tierIndex);
	}
	public boolean isMasteryActive(UUID uuid, String mastery) {
		return masteryMap.containsKey(mastery) ? masteryMap.get(mastery).hasActive(uuid) : false;
	}
	public double getMasteryPower(UUID uuid, String mastery) {
		return masteryMap.get(mastery).getAbilityPower(uuid);
	}
	public int getMasteryTier(UUID uuid, String mastery) {
		return masteryMap.get(mastery).getAbilityTier(uuid);
	}
	private void createFile() {
		plugin.getLogger().info("Could not find achievements file in plugin directory! Creating new achievements file...");
		try {
			FileUtils.copyInputStreamToFile(plugin.getResource("files/achievements.yml"), new File(plugin.getDataFolder().getAbsolutePath(), "achievements.yml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void saveData() {
		achievementMap.forEach((k, v) -> v.save());
		masteryMap.forEach((k, v) -> v.save());
	}
	class Achievement {
		private Material item;
		private String name, description, series;
		private String pathName;
		private int goal, amount;
		private boolean hidden, locked;
		private Map<UUID, Integer> progress = new HashMap<>();
		private Set<UUID> claimed = new HashSet<>();
		private Achievement[] tiers;
		private Map<ItemStack, Integer> rewards = new LinkedHashMap<>();
		private int xp;
		private Sound sound;
		private float[] soundSettings = {1.0f, 1.0f};
		private ItemStack hiddenItem;
		private Mastery mastery;
		private ItemStack displayItem;
		
		public Achievement(String pathName) {
			this.pathName = pathName;
			tiers = new Achievement[file.getConfigurationSection(pathName+".tiers").getKeys(false).size()];
			int i = 0;
			for (String tier : file.getConfigurationSection(pathName+".tiers").getKeys(false)) {
				Map<UUID, Integer> tempProgress = new HashMap<>();
				Set<UUID> tempClaimed = new HashSet<>();
				if (plugin.dataFile.contains(pathName+".tiers."+tier+".progress")) {
					for (String key : plugin.dataFile.getConfigurationSection(pathName+".tiers."+tier+".progress").getKeys(false))
						tempProgress.put(UUID.fromString(key), plugin.dataFile.getInt(pathName+".tiers."+tier+".progress."+key));
					if (plugin.dataFile.contains(pathName+".tiers."+tier+".claimed"))
						for (String key : plugin.dataFile.getStringList(pathName+".tiers."+tier+".claimed"))
							tempClaimed.add(UUID.fromString(key));
				}
				tiers[i] = new Achievement(pathName+".tiers."+tier, tempProgress, tempClaimed);
				i++;
			}
			this.series = Utils.convertString(file.getString(pathName+".series"));
			if (file.contains(pathName+".hidden_slots"))
				try {
					this.hiddenItem = Utils.createItem(Material.valueOf(file.getString(pathName+".hidden_slots").toUpperCase()), 1, "&f???", null, false, true);
				} catch (IllegalArgumentException e) {
					Main.consoleSender.sendMessage(Languages.prefix+Utils.convertString("&eCould not add hidden item to achievement &d'"+name+"&d' &eno such item &c"+file.getString(pathName+".hidden_slots")+" &eexists!"));
					this.hiddenItem = Utils.createItem(Material.BLACK_STAINED_GLASS_PANE, 1, "&f???", null, false, true);
				}
			else
				this.hiddenItem = Utils.createItem(Material.BLACK_STAINED_GLASS_PANE, 1, "&f???", null, false, true);
		}
		public Achievement(String pathName, Map<UUID, Integer> progress, Set<UUID> claimed) {
			this.pathName = pathName;
			try {
				this.item = Material.valueOf(file.getString(pathName+".item").toUpperCase());
			} catch (IllegalArgumentException e) {
				Main.consoleSender.sendMessage(Languages.prefix+Utils.convertString("&eCould not set item icon for achievement &d'"+pathName+"' &eno such item named &c'"+file.getString(pathName+".item")+"'"));
				this.item = Material.BARRIER;
			}
			this.name = Utils.convertString(file.getString(pathName+".name"));
			this.amount = file.getInt(pathName+".amount");
			this.description = Utils.convertString(file.getString(pathName+".description"));
			this.goal = file.getInt(pathName+".goal");
			this.hidden = file.getBoolean(pathName+".hidden");
			if (file.contains(pathName+".locked"))
				this.locked = file.getBoolean(pathName+".locked");
			this.progress = progress;
			this.claimed = claimed;
			this.xp = file.getInt(pathName+".xp");
			for (String key : file.getStringList(pathName+".rewards")) {
				String reward = key.replaceAll(" ", "");
				try {
					String itemName = reward.substring(0, reward.indexOf('|'));
					ItemStack item = null;
					if (ItemsHandler.allItems.containsKey(itemName))
						item = ItemsHandler.allItems.get(itemName);
					else if (DependencyUtils.isUIFrameworkEnabled()) {
						String uiName = CustomDropsFactory.changesMap.containsKey(itemName) ? CustomDropsFactory.changesMap.get(itemName) : itemName;
						if (ItemType.getItemType(uiName) != null)
							item = ItemType.getItemType(uiName).getBuilder().getItem();
						else if (DependencyUtils.isUltimateContentEnabled() && ItemType.getItemType(uiName) != null)
							item = ItemType.getItemType(uiName).getBuilder().getItem();
					} else if (CustomDropsFactory.changesMap.containsKey(itemName) || CustomDropsFactory.changesMap.containsValue(itemName)) {
						if (!CustomDropsFactory.warnedItems) {
							Main.consoleSender.sendMessage(Utils.convertString("&e[DeadlyDisasters]: &bYou have custom drop items in your achievements.yml class that are a part of UltimateContent! If you want these custom items then download UltimateContent to your server."));
							CustomDropsFactory.warnedItems = true;
						}
						continue;
					}
					if (item == null)
						item = new ItemStack(Material.valueOf(itemName.toUpperCase()));
					rewards.put(item, Integer.parseInt(reward.substring(reward.indexOf('|')+1)));
				} catch (IllegalArgumentException e) {
					Main.consoleSender.sendMessage(Languages.prefix+Utils.convertString("&eCould not add reward item to achievement &d'"+name+"&d' &eimproper usage on line\n    &c-> "+key));
				}
			}
			if (file.contains(pathName+".sound.type")) {
				try {
					sound = Sound.valueOf(file.getString(pathName+".sound.type").toUpperCase());
					if (file.contains(pathName+".sound.volume"))
						soundSettings[0] = (float) file.getDouble(pathName+".sound.volume");
					if (file.contains(pathName+".sound.pitch"))
						soundSettings[1] = (float) file.getDouble(pathName+".sound.pitch");
				} catch (IllegalArgumentException e) {
					Main.consoleSender.sendMessage(Languages.prefix+Utils.convertString("&eCould not add sound to achievement &d'"+name+"&d' &eno such sound &c"+file.getString(pathName+".sound.type")+" &eexists!"));
				}
			}
		}
		public void save() {
			for (Achievement tier : tiers) {
				for (Map.Entry<UUID, Integer> entry : tier.progress.entrySet())
					plugin.dataFile.set(tier.pathName+".progress."+entry.getKey(), entry.getValue());
				List<String> uuidList = new ArrayList<>();
				tier.claimed.forEach(e -> uuidList.add(e.toString()));
				plugin.dataFile.set(tier.pathName+".claimed", uuidList);
			}
		}
		public void passMastery(Mastery mastery) {
			this.mastery = mastery;
		}
		public void addProgress(UUID uuid, int value) {
			for (int i=0; i < tiers.length; i++) {
				Achievement tier = tiers[i];
				if (tier.hasAchieved(uuid))
					continue;
				if (i > 0 && tier.locked && !tiers[i-1].hasAchieved(uuid))
					return;
				if (!tier.progress.containsKey(uuid))
					tier.progress.put(uuid, value);
				else
					tier.progress.replace(uuid, Math.min(tier.progress.get(uuid) + value, tier.goal));
				if (tier.hasAchieved(uuid)) {
					Player player = (Player) Bukkit.getEntity(uuid);
					if (tier.sound != null)
						player.playSound(player.getLocation(), tier.sound, tier.soundSettings[0], tier.soundSettings[1]);
					if (Utils.isSpigot()) {
						net.md_5.bungee.api.chat.TextComponent local = new net.md_5.bungee.api.chat.TextComponent(Languages.getString("internal.earnAchievementLocal")+' '+ChatColor.getLastColors(tier.name)+'['+tier.name+']');
						local.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text(ChatColor.getLastColors(tier.name)+tier.description)));
						net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent(player.getName()+' '+Languages.getString("internal.earnAchievement")+' '+ChatColor.getLastColors(tier.name)+'['+tier.name+']');
						message.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text(ChatColor.getLastColors(tier.name)+tier.description)));
						if (announceToPlayers)
							player.spigot().sendMessage(local);
						if (broadcastToWorld)
							for (Player p : player.getWorld().getPlayers())
								if (!p.equals(player))
									p.spigot().sendMessage(message);
					} else {
						String local = Languages.getString("internal.earnAchievementLocal")+' '+ChatColor.getLastColors(tier.name)+'['+tier.name+']';
						String message = player.getName()+' '+Languages.getString("internal.earnAchievement")+' '+ChatColor.getLastColors(tier.name)+'['+tier.name+']';
						if (announceToPlayers)
							player.sendMessage(local);
						if (broadcastToWorld)
							for (Player p : player.getWorld().getPlayers())
								if (!p.equals(player))
									p.sendMessage(message);
					}
				}
			}
		}
		public void addProgress(UUID uuid, int value, int tierIndex) {
			if (tierIndex < 0 || tierIndex > tiers.length)
				return;
			Achievement tier = tiers[tierIndex];
			if (tier.hasAchieved(uuid) || (tierIndex > 0 && tier.locked && !tiers[tierIndex-1].hasAchieved(uuid)))
				return;
			if (!tier.progress.containsKey(uuid))
				tier.progress.put(uuid, value);
			else
				tier.progress.replace(uuid, Math.min(tier.progress.get(uuid) + value, tier.goal));
			if (tier.hasAchieved(uuid)) {
				Player player = (Player) Bukkit.getEntity(uuid);
				if (tier.sound != null)
					player.playSound(player.getLocation(), tier.sound, tier.soundSettings[0], tier.soundSettings[1]);
				if (Utils.isSpigot()) {
					net.md_5.bungee.api.chat.TextComponent local = new net.md_5.bungee.api.chat.TextComponent(Languages.getString("internal.earnAchievementLocal")+' '+ChatColor.getLastColors(tier.name)+'['+tier.name+']');
					local.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text(ChatColor.getLastColors(tier.name)+tier.description)));
					net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent(player.getName()+' '+Languages.getString("internal.earnAchievement")+' '+ChatColor.getLastColors(tier.name)+'['+tier.name+']');
					message.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text(ChatColor.getLastColors(tier.name)+tier.description)));
					if (announceToPlayers)
						player.spigot().sendMessage(local);
					if (broadcastToWorld)
						for (Player p : player.getWorld().getPlayers())
							if (!p.equals(player))
								p.spigot().sendMessage(message);
				} else {
					String local = Languages.getString("internal.earnAchievementLocal")+' '+ChatColor.getLastColors(tier.name)+'['+tier.name+']';
					String message = player.getName()+' '+Languages.getString("internal.earnAchievement")+' '+ChatColor.getLastColors(tier.name)+'['+tier.name+']';
					if (announceToPlayers)
						player.sendMessage(local);
					if (broadcastToWorld)
						for (Player p : player.getWorld().getPlayers())
							if (!p.equals(player))
								p.sendMessage(message);
				}
			}
		}
		public void claimRewards(Player player) {
			for (Map.Entry<ItemStack, Integer> entry : rewards.entrySet())
				for (int i=0; i < entry.getValue(); i++)
					if (player.getInventory().firstEmpty() != -1)
						player.getInventory().addItem(entry.getKey());
					else
						player.getWorld().dropItemNaturally(player.getLocation(), entry.getKey());
			player.giveExp(xp);
			claimed.add(player.getUniqueId());
		}
		public int getProgress(UUID uuid) {
			return progress.containsKey(uuid) ? progress.get(uuid) : 0;
		}
		public boolean hasAchieved(UUID uuid) {
			return (progress.containsKey(uuid) && progress.get(uuid) >= goal);
		}
		public boolean hasClaimed(UUID uuid) {
			return claimed.contains(uuid);
		}
		public String getName() {
			return name;
		}
		public String getDescription() {
			return description;
		}
		public String getSeries() {
			return series;
		}
		public int getGoal() {
			return goal;
		}
	}
	class Mastery {
		private Set<UUID> activePlayers = new HashSet<>();
		private Achievement achievement;
		private Material material;
		private String pathName;
		private String name;
		private String description;
		private int requiredTier;
		private int modelData;
		private String abilityDescription;
		private double abilityPower;
		private Mastery[] tiers;
		public Mastery(String pathName, Achievement achievement) {
			this.pathName = pathName;
			this.achievement = achievement;
			tiers = new Mastery[file.getConfigurationSection(pathName+".tiers").getKeys(false).size()];
			int i = 0;
			for (String tier : file.getConfigurationSection(pathName+".tiers").getKeys(false)) {
				tiers[i] = new Mastery(pathName+".tiers."+tier, Integer.parseInt(tier.substring(tier.indexOf(' ')+1)));
				i++;
			}
			if (plugin.dataFile.contains(pathName+".active"))
				for (String key : plugin.dataFile.getStringList(pathName+".active"))
					activePlayers.add(UUID.fromString(key));
		}
		public Mastery(String pathName, int requiredTier) {
			this.pathName = pathName;
			this.requiredTier = requiredTier;
			try {
				this.material = Material.valueOf(file.getString(pathName+".item").toUpperCase());
			} catch (IllegalArgumentException e) {
				Main.consoleSender.sendMessage(Languages.prefix+Utils.convertString("&eCould not add item to mastery &d'"+name+"&d' &eno such item &c"+file.getString(pathName+".item")+" &eexists!"));
				this.material = Material.BARRIER;
			}
			this.name = Utils.convertString(file.getString(pathName+".name"));
			this.description = Utils.convertString(file.getString(pathName+".description"));
			this.abilityDescription = Utils.convertString(file.getString(pathName+".ability_description"));
			this.abilityPower = file.getDouble(pathName+".ability_power");
			this.modelData = file.getInt(pathName+".data");
		}
		public void save() {
			List<String> uuidList = new ArrayList<>();
			activePlayers.forEach(e -> uuidList.add(e.toString()));
			plugin.dataFile.set(pathName+".active", uuidList);
		}
		public boolean hasActive(UUID uuid) {
			return activePlayers.contains(uuid);
		}
		public boolean canUse(UUID uuid) {
			return achievement.tiers[tiers[0].requiredTier-1].hasAchieved(uuid);
		}
		public ItemStack build(UUID uuid) {
			Material item = tiers[0].material;
			String itemName = tiers[0].name;
			String description = tiers[0].description;
			int data = tiers[0].modelData;
			List<String> lore = new ArrayList<>();
			for (Mastery tier : tiers) {
				lore.add(" ");
				if (achievement.tiers[tier.requiredTier-1].hasAchieved(uuid)) {
					item = tier.material;
					itemName = tier.name;
					description = tier.description;
					data = tier.modelData;
					lore.add("&fTier "+Utils.getNumerical(tier.requiredTier)+": "+tier.abilityDescription);
				} else {
					lore.add(Utils.convertString("&8Tier "+Utils.getNumerical(tier.requiredTier)+": "+ChatColor.stripColor(tier.abilityDescription)));
				}
			}
			List<String> newLore = new ArrayList<>();
			newLore.add(description);
			newLore.addAll(lore);
			if (activePlayers.contains(uuid))
				newLore.addAll(Arrays.asList(" ", Utils.convertString("&b"+Languages.getString("internal.deactivateMastery"))));
			else if (achievement.tiers[tiers[0].requiredTier-1].hasAchieved(uuid))
				newLore.addAll(Arrays.asList(" ", Utils.convertString("&a"+Languages.getString("internal.enableMastery"))));
			ItemStack toUse = Utils.createItem(item, 1, itemName, newLore, activePlayers.contains(uuid), true);
			ItemMeta meta = toUse.getItemMeta();
			meta.setCustomModelData(data);
			toUse.setItemMeta(meta);
			return toUse;
		}
		public double getAbilityPower(UUID uuid) {
			double temp = 0;
			for (Mastery tier : tiers)
				if (achievement.tiers[tier.requiredTier-1].hasAchieved(uuid))
					temp = tier.abilityPower;
			return temp;
		}
		public int getAbilityTier(UUID uuid) {
			int temp = 0;
			for (Mastery tier : tiers)
				if (achievement.tiers[tier.requiredTier-1].hasAchieved(uuid))
					temp++;
			return temp;
		}
	}
	class SurvivalChannel {
		private Set<ChannelDataHolder> data = ConcurrentHashMap.newKeySet();
		public SurvivalChannel(Main plugin) {
			plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
				Iterator<ChannelDataHolder> it = data.iterator();
				while (it.hasNext()) {
					Player p = it.next().getPlayer();
					if (p == null || !p.isOnline() || Utils.isPlayerImmune(p))
						it.remove();
				}
			}, 0, 10);
		}
		public void addToTrain(ChannelDataHolder holder) {
			data.add(holder);
		}
		public void removeFromTrain(ChannelDataHolder holder) {
			data.remove(holder);
		}
		public void awardIfPresent(ChannelDataHolder holder, int value, String achievement) {
			if (data.contains(holder) && holder.getPlayer() != null && achievementMap.containsKey(achievement))
				achievementMap.get(achievement).addProgress(holder.getPlayer().getUniqueId(), value);
		}
		public void awardIfPresent(ChannelDataHolder holder, int value, String achievement, int tierIndex) {
			if (data.contains(holder) && holder.getPlayer() != null && achievementMap.containsKey(achievement))
				achievementMap.get(achievement).addProgress(holder.getPlayer().getUniqueId(), value, tierIndex);
		}
	}
	public void addToSurvivalChannel(ChannelDataHolder holder) {
		survivalChannel.addToTrain(holder);
	}
	public void awardToSurvivalChannel(ChannelDataHolder holder, int value, String achievement) {
		survivalChannel.awardIfPresent(holder, value, achievement);
	}
	public void awardToSurvivalChannel(ChannelDataHolder holder, int value, String achievement, int tierIndex) {
		survivalChannel.awardIfPresent(holder, value, achievement, tierIndex);
	}
	public void removeFromSurvivalChannel(ChannelDataHolder holder) {
		survivalChannel.removeFromTrain(holder);
	}
	class WeatherSurvivalChannel {
		private Set<ChannelDataHolder> data = ConcurrentHashMap.newKeySet();
		public WeatherSurvivalChannel(Main plugin) {
			plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
				Iterator<ChannelDataHolder> it = data.iterator();
				while (it.hasNext()) {
					ChannelDataHolder holder = it.next();
					if (holder.getPlayer() == null || !holder.getPlayer().isOnline() || Utils.isPlayerImmune(holder.getPlayer()) || !holder.getPlayer().getWorld().equals(holder.getWorld()))
						it.remove();
				}
			}, 0, 10);
		}
		public void addToTrain(ChannelDataHolder holder) {
			data.add(holder);
		}
		public void removeFromTrain(ChannelDataHolder holder) {
			data.remove(holder);
		}
		public void awardIfPresent(ChannelDataHolder holder, int value, String achievement) {
			if (data.contains(holder) && holder.getPlayer() != null && achievementMap.containsKey(achievement))
				achievementMap.get(achievement).addProgress(holder.getPlayer().getUniqueId(), value);
		}
		public void awardIfPresent(ChannelDataHolder holder, int value, String achievement, int tierIndex) {
			if (data.contains(holder) && holder.getPlayer() != null && achievementMap.containsKey(achievement))
				achievementMap.get(achievement).addProgress(holder.getPlayer().getUniqueId(), value, tierIndex);
		}
	}
	public void addToWeatherSurvivalChannel(ChannelDataHolder holder) {
		weatherSurvivalChannel.addToTrain(holder);
	}
	public void awardToWeatherSurvivalChannel(ChannelDataHolder holder, int value, String achievement) {
		weatherSurvivalChannel.awardIfPresent(holder, value, achievement);
	}
	public void awardToWeatherSurvivalChannel(ChannelDataHolder holder, int value, String achievement, int tierIndex) {
		weatherSurvivalChannel.awardIfPresent(holder, value, achievement, tierIndex);
	}
	public void removeFromWeatherSurvivalChannel(ChannelDataHolder holder) {
		weatherSurvivalChannel.removeFromTrain(holder);
	}
}
