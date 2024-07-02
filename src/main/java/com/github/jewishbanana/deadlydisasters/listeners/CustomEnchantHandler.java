package com.github.jewishbanana.deadlydisasters.listeners;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustTransition;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Trident;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockVector;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.CustomHead;
import com.github.jewishbanana.deadlydisasters.entities.EntityHandler;
import com.github.jewishbanana.deadlydisasters.entities.christmasentities.ElfPet;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.Ghoul;
import com.github.jewishbanana.deadlydisasters.entities.soulstormentities.TamedLostSoul;
import com.github.jewishbanana.deadlydisasters.events.DisasterEvent;
import com.github.jewishbanana.deadlydisasters.events.disasters.Monsoon;
import com.github.jewishbanana.deadlydisasters.handlers.ItemsHandler;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.listeners.customevents.ArmorEquipEvent;
import com.github.jewishbanana.deadlydisasters.listeners.customevents.ArmorUnequipEvent;
import com.github.jewishbanana.deadlydisasters.utils.AsyncRepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

import net.md_5.bungee.api.ChatColor;

public class CustomEnchantHandler implements Listener {
	
	private Main plugin;
	private Random rand;
	
	private Map<UUID,Integer> ancientBladeCooldownMap = new HashMap<UUID,Integer>();
	private Map<UUID,Integer> soulRipperCooldownMap = new HashMap<UUID,Integer>();
	private Map<UUID,Integer> poseidonsTridentCooldown = new HashMap<UUID,Integer>();
	private Map<UUID,Integer> bloodPactCooldown = new HashMap<UUID,Integer>();
	private Map<UUID,Integer> etheralLanternCooldown = new HashMap<UUID,Integer>();
//	private Map<UUID,Vector> playerMoveMap = new ConcurrentHashMap<UUID,Vector>();
//	private Map<UUID,Location> playerMoveLocationMap = new ConcurrentHashMap<UUID,Location>();
//	private Map<UUID,Integer> playerMoveCooldown = new HashMap<UUID,Integer>();
	
	private Map<UUID,Object[]> bloodPactFangMap = new HashMap<>();
	
	private int ancientCurseFireTicks;
	private int ancientCurseLifeTicks;
	private int ancientCurseParticleCount;
	private int[] yetisBlessingRange;
	private int[] yetisBlessingChance;
	private int[] poseidonsTridentRange;
	private double bunnyHopMultiplier;
	private boolean bunnyHopParticles;
	private int etherealLanternLifeTicks;
	private int etherealLanternGhoulCount;
	
	private String yetisBlessing;
	private String bloodSacrifice;
	
	public static Map<UUID, ElfPet[]> santaHatPlayers = new HashMap<>();
	private Set<UUID> bunnyHopPlayers = new HashSet<>();
	private Set<UUID> hoppingPlayers = new HashSet<>();
	
	public CustomEnchantHandler(Main plugin) {
		this.plugin = plugin;
		this.rand = plugin.random;
		
		reload();
		
		for (Player p : plugin.getServer().getOnlinePlayers()) {
			ItemStack item = p.getEquipment().getHelmet();
			if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(ItemsHandler.santaHatKey, PersistentDataType.INTEGER))
				santaHatPlayers.put(p.getUniqueId(), null);
			item = p.getEquipment().getBoots();
			if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(ItemsHandler.bunnyHopKey, PersistentDataType.BYTE))
				bunnyHopPlayers.add(p.getUniqueId());
		}
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				Iterator<Entry<UUID, Integer>> it = ancientBladeCooldownMap.entrySet().iterator();
				while (it.hasNext()) {
					Entry<UUID, Integer> entry = it.next();
					entry.setValue(entry.getValue() - 1);
					if (entry.getValue() <= 0)
						it.remove();
				}
				it = soulRipperCooldownMap.entrySet().iterator();
				while (it.hasNext()) {
					Entry<UUID, Integer> entry = it.next();
					entry.setValue(entry.getValue() - 1);
					if (entry.getValue() <= 0)
						it.remove();
				}
				it = poseidonsTridentCooldown.entrySet().iterator();
				while (it.hasNext()) {
					Entry<UUID, Integer> entry = it.next();
					entry.setValue(entry.getValue() - 1);
					if (entry.getValue() <= 0)
						it.remove();
				}
				it = bloodPactCooldown.entrySet().iterator();
				while (it.hasNext()) {
					Entry<UUID, Integer> entry = it.next();
					entry.setValue(entry.getValue() - 1);
					if (entry.getValue() <= 0)
						it.remove();
				}
				it = etheralLanternCooldown.entrySet().iterator();
				while (it.hasNext()) {
					Entry<UUID, Integer> entry = it.next();
					entry.setValue(entry.getValue() - 1);
					if (entry.getValue() <= 0)
						it.remove();
				}
//				it = playerMoveCooldown.entrySet().iterator();
//				while (it.hasNext()) {
//					Entry<UUID, Integer> entry = it.next();
//					entry.setValue(entry.getValue() - 1);
//					if (entry.getValue() <= 0)
//						it.remove();
//				}
				Iterator<Entry<UUID, Map<DisasterEvent, Integer>>> iterator = DisasterEvent.countdownMap.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<UUID, Map<DisasterEvent, Integer>> entry = iterator.next();
					Iterator<Entry<DisasterEvent, Integer>> internal = entry.getValue().entrySet().iterator();
					while (internal.hasNext()) {
						Entry<DisasterEvent, Integer> map = internal.next();
						map.setValue(map.getValue() - 1);
						if (map.getValue() <= 0) {
							internal.remove();
							if (entry.getValue().isEmpty())
								iterator.remove();
						}
					}
				}
			}
		}, 0, 20);
		new RepeatingTask(plugin, 0, 20) {
			@Override
			public void run() {
				Iterator<Entry<UUID, ElfPet[]>> santaHatIterator = santaHatPlayers.entrySet().iterator();
				while (santaHatIterator.hasNext()) {
					Entry<UUID, ElfPet[]> entry = santaHatIterator.next();
					Player p = plugin.getServer().getPlayer(entry.getKey());
					if (p == null || !p.isOnline()) {
						if (entry.getValue() != null)
							for (ElfPet pet : entry.getValue())
								if (pet != null && pet.getEntity() != null)
									pet.getEntity().remove();
						santaHatIterator.remove();
						continue;
					}
					ItemStack item = p.getEquipment().getHelmet();
					if (item == null || !item.hasItemMeta() || !item.getItemMeta().getPersistentDataContainer().has(ItemsHandler.santaHatKey, PersistentDataType.INTEGER)) {
						if (entry.getValue() != null)
							for (ElfPet pet : entry.getValue())
								if (pet != null && pet.getEntity() != null)
									pet.getEntity().remove();
						santaHatIterator.remove();
						continue;
					}
					ItemMeta meta = item.getItemMeta();
					int amount = meta.getPersistentDataContainer().get(ItemsHandler.santaHatKey, PersistentDataType.INTEGER);
					if (entry.getValue() == null) {
						if (amount <= 0)
							amount = ItemsHandler.santaHatCooldown;
						else if (amount == 1) {
							meta.getPersistentDataContainer().set(ItemsHandler.santaHatKey, PersistentDataType.INTEGER, 0);
							item.setItemMeta(meta);
							ElfPet[] elves = {
									CustomEntity.handler.addEntity(new ElfPet((Zombie) p.getWorld().spawnEntity(p.getLocation(), EntityType.ZOMBIE), plugin, rand, p.getUniqueId(), false)),
									CustomEntity.handler.addEntity(new ElfPet((Zombie) p.getWorld().spawnEntity(p.getLocation(), EntityType.ZOMBIE), plugin, rand, p.getUniqueId(), false)),
									CustomEntity.handler.addEntity(new ElfPet((Zombie) p.getWorld().spawnEntity(p.getLocation(), EntityType.ZOMBIE), plugin, rand, p.getUniqueId(), true))
							};
							for (ElfPet elf : elves)
								CustomEntity.handler.addEntity(elf);
							entry.setValue(elves);
							continue;
						}
						meta.getPersistentDataContainer().set(ItemsHandler.santaHatKey, PersistentDataType.INTEGER, amount-1);
						item.setItemMeta(meta);
						continue;
					}
					for (ElfPet pet : entry.getValue()) {
						if (pet.getEntity() == null || pet.getEntity().isDead()) {
							if (amount <= 0)
								amount = ItemsHandler.santaHatCooldown;
							else if (amount == 1) {
								ElfPet[] elves = entry.getValue();
								for (int i=0; i < 3; i++) {
									if (elves[i].getEntity() == null || elves[i].getEntity().isDead()) {
										if (i == 2)
											elves[i] = CustomEntity.handler.addEntity(new ElfPet((Zombie) p.getWorld().spawnEntity(p.getLocation(), EntityType.ZOMBIE), plugin, rand, p.getUniqueId(), true));
										else
											elves[i] = CustomEntity.handler.addEntity(new ElfPet((Zombie) p.getWorld().spawnEntity(p.getLocation(), EntityType.ZOMBIE), plugin, rand, p.getUniqueId(), false));
									}
								}
							}
							meta.getPersistentDataContainer().set(ItemsHandler.santaHatKey, PersistentDataType.INTEGER, amount-1);
							item.setItemMeta(meta);
							break;
						}
					}
				}
				Iterator<UUID> it = bunnyHopPlayers.iterator();
				while (it.hasNext()) {
					Player p = Bukkit.getPlayer(it.next());
					if (p == null || !p.isOnline()) {
						it.remove();
						continue;
					}
					ItemStack item = p.getEquipment().getBoots();
					if (item == null || !item.hasItemMeta() || !item.getItemMeta().getPersistentDataContainer().has(ItemsHandler.bunnyHopKey, PersistentDataType.BYTE)) {
						if (!Utils.isPlayerImmune(p))
							p.setAllowFlight(false);
						it.remove();
						continue;
					}
					if (!hoppingPlayers.contains(p.getUniqueId()))
						p.setAllowFlight(true);
				}
			}
		};
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				if (!bunnyHopParticles)
					return;
				for (UUID uuid : bunnyHopPlayers) {
					Player p = (Player) Bukkit.getEntity(uuid);
					if (p != null && !p.isDead())
						for (int i=0; i < 2; i++) {
	            			DustTransition dust = new DustTransition(Color.fromRGB(rand.nextInt(125)+25, 255, rand.nextInt(55)+25), Color.fromRGB(25, rand.nextInt(155)+100, 255), rand.nextFloat());
	            			if (rand.nextInt(2) == 0)
	            				dust = new DustTransition(Color.fromRGB(rand.nextInt(105)+150, 25, 255), Color.fromRGB(25, rand.nextInt(155)+100, 255), rand.nextFloat());
	            			p.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, p.getLocation().add(rand.nextDouble()/1.2-.4,0.1+(rand.nextDouble()/3-.15),rand.nextDouble()/1.2-.4), 1, 0, 0, 0, 0.001, dust);
	            		}
				}
			}
		};
	}
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		ItemStack item = p.getEquipment().getHelmet();
		if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(ItemsHandler.santaHatKey, PersistentDataType.INTEGER))
			santaHatPlayers.put(p.getUniqueId(), null);
		item = p.getEquipment().getBoots();
		if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(ItemsHandler.bunnyHopKey, PersistentDataType.BYTE))
			bunnyHopPlayers.add(p.getUniqueId());
	}
	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent e) {
		UUID uuid = e.getPlayer().getUniqueId();
		if (santaHatPlayers.containsKey(uuid)) {
			if (santaHatPlayers.get(uuid) != null)
				for (ElfPet pet : santaHatPlayers.get(uuid))
					if (pet != null && pet.getEntity() != null)
						pet.getEntity().remove();
			santaHatPlayers.remove(uuid);
		}
		bunnyHopPlayers.remove(uuid);
		hoppingPlayers.remove(uuid);
	}
	@EventHandler
	public void onArmorEquip(ArmorEquipEvent e) {
		if (e.getSlot() == ArmorListener.ArmorSlot.HEAD && e.getItem().hasItemMeta() && e.getItem().getItemMeta().getPersistentDataContainer().has(ItemsHandler.santaHatKey, PersistentDataType.INTEGER))
			santaHatPlayers.put(e.getPlayer().getUniqueId(), null);
		if (e.getSlot() == ArmorListener.ArmorSlot.FEET && e.getItem().hasItemMeta() && e.getItem().getItemMeta().getPersistentDataContainer().has(ItemsHandler.bunnyHopKey, PersistentDataType.BYTE))
			bunnyHopPlayers.add(e.getPlayer().getUniqueId());
	}
	@EventHandler
	public void onArmorUnequip(ArmorUnequipEvent e) {
		if (e.getSlot() == ArmorListener.ArmorSlot.FEET && e.getItem().hasItemMeta() && e.getItem().getItemMeta().getPersistentDataContainer().has(ItemsHandler.bunnyHopKey, PersistentDataType.BYTE)) {
			bunnyHopPlayers.remove(e.getPlayer().getUniqueId());
			hoppingPlayers.remove(e.getPlayer().getUniqueId());
			if (!Utils.isPlayerImmune(e.getPlayer()))
				e.getPlayer().setAllowFlight(false);
		}
	}
	@EventHandler
	public void onAttack(EntityDamageByEntityEvent e) {
		if (e.isCancelled())
			return;
		if (bloodPactFangMap.containsKey(e.getDamager().getUniqueId())) {
			e.setDamage(0);
			LivingEntity damager = (LivingEntity) Bukkit.getEntity((UUID) bloodPactFangMap.get(e.getDamager().getUniqueId())[0]);
			if (damager != null && !damager.isDead()) {
				ItemStack item = (ItemStack) bloodPactFangMap.get(e.getDamager().getUniqueId())[2];
				int enchantLevel = Utils.levelOfEnchant(ChatColor.RED+bloodSacrifice, item);
				double damage = (((damager.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()/100.0)*plugin.getConfig().getDouble("customitems.enchants.blood_sacrifice.level "+enchantLevel+".lifeTake"))/100.0)*(double) (bloodPactFangMap.get(e.getDamager().getUniqueId())[1]);
				if (!(damager instanceof Player) || !Utils.isPlayerImmune((Player) damager)) {
					if (damage >= damager.getHealth())
						damage = damager.getHealth();
					Utils.pureDamageEntity(damager, damage, "dd-bloodsacrificesuicide", enchantLevel >= 3, null);
					damager.getWorld().spawnParticle(Particle.BLOCK_CRACK, damager.getLocation().add(0,damager.getHeight()/2,0), 50, .2, damager.getHeight()/2, .2, 1, Material.REDSTONE_BLOCK.createBlockData());
					Utils.damageItem(item, 20);
				}
				if (!(e.getEntity() instanceof Player) || !Utils.isPlayerImmune((Player) e.getEntity())) {
					e.getEntity().getWorld().spawnParticle(Particle.BLOCK_CRACK, e.getEntity().getLocation().add(0,e.getEntity().getHeight()/2,0), 50, .2, e.getEntity().getHeight()/2, .2, 1, Material.REDSTONE_BLOCK.createBlockData());
					Utils.damageEntity((LivingEntity) e.getEntity(), damage*plugin.getConfig().getDouble("customitems.enchants.blood_sacrifice.level "+enchantLevel+".damage"), "dd-bloodsacrifice", enchantLevel >= 3);
				}
				if (e.getEntity().isDead() && enchantLevel < 3) {
					int amount = item.getItemMeta().getPersistentDataContainer().get(ItemsHandler.bloodPactKey, PersistentDataType.INTEGER)+1;
					if (amount == 25) {
						Utils.upgradeEnchantLevel(item, ChatColor.RED+bloodSacrifice, 3);
						ItemMeta meta = item.getItemMeta();
						meta.setCustomModelData(100018);
						item.setItemMeta(meta);
					} else if (amount == 50) {
						ItemMeta meta = item.getItemMeta();
						meta.setDisplayName(ChatColor.DARK_RED+Languages.getString("items.awakenedBloodPact"));
						meta.setLore(Utils.chopLore(Arrays.asList(ChatColor.RED+Languages.getString("misc.bloodSacrifice")+" III", " ", ChatColor.YELLOW+Languages.getString("items.awakenedBloodPactLore"))));
						meta.addEnchant(Enchantment.DAMAGE_ALL, 2, false);
						meta.setCustomModelData(100019);
						item.setItemMeta(meta);
					}
					ItemMeta meta = item.getItemMeta();
					meta.getPersistentDataContainer().set(ItemsHandler.bloodPactKey, PersistentDataType.INTEGER, amount);
					item.setItemMeta(meta);
				}
			}
			bloodPactFangMap.remove(e.getDamager().getUniqueId());
			return;
		}
		if (e.getDamager().hasMetadata("dd-elfarrow") && e.getEntity().hasMetadata("dd-christmasmob")) {
			e.setCancelled(true);
			return;
		} else if (e.getDamager().hasMetadata("dd-petelfarrow")) {
			CustomEntity ce = CustomEntity.handler.findEntity((LivingEntity) ((Arrow) e.getDamager()).getShooter());
			if (ce != null) {
				UUID uuid = ((ElfPet) ce).owner;
				if (uuid.equals(e.getEntity().getUniqueId()) || (santaHatPlayers.containsKey(uuid) && Stream.of(santaHatPlayers.get(uuid)).anyMatch(n -> n != null && n.getEntity().getUniqueId().equals(e.getEntity().getUniqueId())))) {
					e.setCancelled(true);
					return;
				}
			}
		}
		if (e.getDamager().hasMetadata("dd-easterbunny") && e.getEntity().hasMetadata("dd-eastermobs")) {
			e.setCancelled(true);
			return;
		}
		Entity dmr = e.getDamager();
		if (dmr instanceof Projectile && ((Projectile) dmr).getShooter() instanceof LivingEntity)
			dmr = (Entity) ((Projectile) dmr).getShooter();
		if (dmr instanceof LivingEntity && e.getEntity() instanceof LivingEntity && !e.getEntity().equals(dmr)) {
			if (santaHatPlayers.containsKey(dmr.getUniqueId()) && santaHatPlayers.get(dmr.getUniqueId()) != null && !(e.getEntity() instanceof Tameable && ((Tameable) e.getEntity()).getOwner().equals((Player) dmr))
					&& !Stream.of(santaHatPlayers.get(dmr.getUniqueId())).anyMatch(n -> n.getEntity() != null && n.getEntity().getUniqueId().equals(e.getEntity().getUniqueId()))) {
				for (ElfPet pet : santaHatPlayers.get(dmr.getUniqueId()))
					pet.target = (LivingEntity) e.getEntity();
			} else if (santaHatPlayers.containsKey(e.getEntity().getUniqueId()) && santaHatPlayers.get(e.getEntity().getUniqueId()) != null
					&& !Stream.of(santaHatPlayers.get(e.getEntity().getUniqueId())).anyMatch(n -> n != null && n.getEntity().equals(e.getDamager()))) {
				for (ElfPet pet : santaHatPlayers.get(e.getEntity().getUniqueId()))
					pet.target = (LivingEntity) dmr;
			}
		}
		if (!(e.getEntity() instanceof LivingEntity) || !(e.getDamager() instanceof LivingEntity))
			return;
		LivingEntity entity = (LivingEntity) e.getEntity();
		LivingEntity damager = (LivingEntity) e.getDamager();
		ItemStack item = damager.getEquipment().getItemInMainHand();
		if (item.hasItemMeta()) {
			if (item.getType() == Material.IRON_HOE) {
				if (CustomEntityType.TAMEDLOSTSOUL.canSpawn() && !soulRipperCooldownMap.containsKey(damager.getUniqueId()) && item.getItemMeta().hasLore()
						&& ((plugin.customNameSupport && item.getItemMeta().getLore().get(0).equals(ItemsHandler.soulRipperLore)) || item.getItemMeta().getPersistentDataContainer().has(ItemsHandler.soulRipperKey, PersistentDataType.BYTE))) {
					if (!(damager instanceof Player) || !Utils.isPlayerImmune((Player) damager))
						soulRipperCooldownMap.put(damager.getUniqueId(), ItemsHandler.soulRipperCooldown);
					spawnSouls(damager.getLocation(), entity);
					if (damager instanceof Player && !Utils.isPlayerImmune((Player) damager)) {
						ItemMeta meta = item.getItemMeta();
						((Damageable) meta).setDamage(((Damageable) meta).getDamage() + 10);
						if (((Damageable) meta).getDamage() >= item.getType().getMaxDurability())
							item.setAmount(0);
						else
							item.setItemMeta(meta);
					}
				}
			} else if (!bloodPactCooldown.containsKey(damager.getUniqueId()) && item.getItemMeta().getPersistentDataContainer().has(ItemsHandler.bloodPactKey, PersistentDataType.INTEGER)
					&& entity.getHealth() > e.getFinalDamage() && Utils.getBlockBelow(entity.getLocation()).getLocation().distanceSquared(entity.getLocation()) <= 900) {
				if (!(damager instanceof Player) || !Utils.isPlayerImmune((Player) damager))
					bloodPactCooldown.put(damager.getUniqueId(), ItemsHandler.bloodPactCooldown);
				createBloodWorm(Utils.getBlockBelow(entity.getLocation()).getLocation().add(.5,.5,.5), entity, item, damager);
			}
		}
		if (CustomEntityType.GHOUL.canSpawn() && !etheralLanternCooldown.containsKey(damager.getUniqueId()) && rand.nextDouble() < ItemsHandler.etherealLanternChance
				&& damager.getEquipment().getItemInOffHand().hasItemMeta()
				&& damager.getEquipment().getItemInOffHand().getItemMeta().getPersistentDataContainer().has(ItemsHandler.etherealLanternKey, PersistentDataType.BYTE)) {
			etheralLanternCooldown.put(damager.getUniqueId(), ItemsHandler.etherealLanternCooldown);
			spawnGhouls(damager, entity);
		}
		if (e.getCause() == DamageCause.ENTITY_ATTACK && entity.getEquipment().getChestplate() != null && Utils.levelOfEnchant(yetisBlessing, entity.getEquipment().getChestplate()) > 0) {
			int level = Utils.levelOfEnchant(yetisBlessing, entity.getEquipment().getChestplate());
			double[] data = {yetisBlessingChance[0], yetisBlessingRange[0]};
			if (level == 2) {
				data[0] = yetisBlessingChance[1];
				data[1] = yetisBlessingRange[1];
			} else if (level == 3) {
				data[0] = yetisBlessingChance[2];
				data[1] = yetisBlessingRange[2];
			}
			if (rand.nextInt(100) < data[0])
				yetiRoar(entity, level, data);
		}
	}
	@EventHandler
	public void onDamage(EntityDamageEvent e) {
		if (e.isCancelled())
			return;
		if (e.getCause() == DamageCause.FALL && bunnyHopPlayers.contains(e.getEntity().getUniqueId())) {
			if (e.getEntity().getFallDistance() < 15)
				e.setCancelled(true);
			else
				e.setDamage(e.getFinalDamage()/2.5);
			hoppingPlayers.remove(e.getEntity().getUniqueId());
			((Player) e.getEntity()).setAllowFlight(true);
			return;
		}
	}
	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		if (e.getItem() == null || !e.getItem().hasItemMeta())
			return;
		if ((e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) && !ancientBladeCooldownMap.containsKey(e.getPlayer().getUniqueId())
				&& e.getItem().getItemMeta().hasLore() && ((plugin.customNameSupport && e.getItem().getItemMeta().getLore().get(0).equals(ItemsHandler.ancientCurseName)) || e.getItem().getItemMeta().getPersistentDataContainer().has(ItemsHandler.ancientBladeKey, PersistentDataType.BYTE))) {
			if (e.getPlayer() instanceof Player && !Utils.isPlayerImmune(e.getPlayer()))
				ancientBladeCooldownMap.put(e.getPlayer().getUniqueId(), ItemsHandler.ancientBladeCooldown);
			int[] spellLife = {ancientCurseLifeTicks};
			Vector motion = e.getPlayer().getEyeLocation().getDirection().clone();
			Location spell = e.getPlayer().getEyeLocation().clone().add(motion.clone().multiply(2));
			e.getPlayer().getWorld().playSound(spell, Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1f, .6f);
			World tempW = spell.getWorld();
			BlockData bd = Material.SAND.createBlockData();
			new RepeatingTask(plugin, 0, 1) {
				@Override
				public void run() {
					if (spellLife[0] <= 0) {
						cancel();
						return;
					}
					spellLife[0]--;
					spell.add(motion);
					tempW.spawnParticle(Particle.FLAME, spell, ancientCurseParticleCount, 1, 1, 1, .05);
					tempW.spawnParticle(Particle.BLOCK_DUST, spell, ancientCurseParticleCount, 1, 1, 1, .1, bd);
					for (Entity entity : spell.getWorld().getNearbyEntities(spell, 1.5, 1.5, 1.5))
						if (entity instanceof LivingEntity && !entity.equals(e.getPlayer())) {
							entity.setFireTicks(ancientCurseFireTicks);
							entity.setVelocity(motion.clone().multiply(0.5));
						}
				}
			};
		}
	}
	@EventHandler
	public void onShoot(ProjectileLaunchEvent e) {
		if (e.isCancelled())
			return;
		if (e.getEntityType() == EntityType.TRIDENT && e.getEntity().getShooter() instanceof LivingEntity && ((Trident) e.getEntity()).getItem().getItemMeta().getPersistentDataContainer().has(ItemsHandler.poseidonsTridentKey, PersistentDataType.BYTE)
				&& !poseidonsTridentCooldown.containsKey(((LivingEntity) e.getEntity().getShooter()).getUniqueId())) {
			LivingEntity entity = (LivingEntity) e.getEntity().getShooter();
			if (entity instanceof Player && !Utils.isPlayerImmune((Player) entity))
				poseidonsTridentCooldown.put(entity.getUniqueId(), ItemsHandler.poseidonsTridentCooldown);
			castWaveSpell(entity.getLocation(), e.getEntity().getVelocity().normalize(), poseidonsTridentRange[0], rand, entity);
		}
	}
	@EventHandler
	public void onToggleFlight(PlayerToggleFlightEvent e) {
		if (bunnyHopPlayers.contains(e.getPlayer().getUniqueId()) && !hoppingPlayers.contains(e.getPlayer().getUniqueId()) && !Utils.isPlayerImmune(e.getPlayer())) {
			Player p = e.getPlayer();
			e.setCancelled(true);
            p.setAllowFlight(false);
            p.setFlying(false);
            p.setVelocity(e.getPlayer().getLocation().getDirection().multiply(1.25).setY(0.8).multiply(bunnyHopMultiplier));
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_RABBIT_JUMP, SoundCategory.PLAYERS, 20f, 0.8f);
            p.setFallDistance(10f);
            hoppingPlayers.add(p.getUniqueId());
            new RepeatingTask(plugin, 5, 1) {
				@SuppressWarnings("deprecation")
				@Override
            	public void run() {
            		if (p == null || !p.isOnline() || p.isDead() || Utils.isPlayerImmune(p) || !hoppingPlayers.contains(p.getUniqueId()) || p.isOnGround()) {
            			cancel();
            			hoppingPlayers.remove(p.getUniqueId());
            			return;
            		}
				}
            };
            return;
		}
	}
//	@EventHandler
//	public void onPlayerMove(PlayerMoveEvent e) {
//		UUID uuid = e.getPlayer().getUniqueId();
//		if (e.getPlayer().isFlying() || playerMoveCooldown.containsKey(uuid) || e.getFrom().distanceSquared(e.getTo()) <= 0.0225)
//			return;
//		e.getPlayer().sendMessage(""+e.getFrom().distance(e.getTo()));
//		if (playerMoveMap.containsKey(uuid)) {
//			Vector vec = Utils.getVectorTowards(e.getFrom(), e.getTo());
//			Vector history = playerMoveMap.get(uuid);
////			e.getPlayer().sendMessage(Utils.chat("&bTried "+(history.getX()*history.getZ()) / (vec.getX()*vec.getZ())));
//			if ((history.getX()*history.getZ()) / (vec.getX()*vec.getZ()) <= 1.0 && playerMoveLocationMap.get(uuid).distanceSquared(e.getTo()) <= 0.25) {
//				e.getPlayer().setVelocity(vec.multiply(2).setY(0.3));
//				playerMoveCooldown.put(uuid, 2);
//				e.getPlayer().sendMessage(Utils.chat("&aDodged"));
//			}
//			playerMoveMap.remove(uuid);
//			playerMoveLocationMap.remove(uuid);
//		} else {
//			playerMoveMap.put(uuid, Utils.getVectorTowards(e.getFrom(), e.getTo()));
//			playerMoveLocationMap.put(uuid, e.getFrom());
//			plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
//				@Override
//				public void run() {
//					playerMoveMap.remove(uuid);
//					playerMoveLocationMap.remove(uuid);
//				}
//			}, 2);
//		}
//	}
	public void reload() {
		ancientCurseFireTicks = plugin.getConfig().getInt("customitems.enchants.ancient_curse.fire_ticks");
		ancientCurseLifeTicks = plugin.getConfig().getInt("customitems.enchants.ancient_curse.spell_life_ticks");
		ancientCurseParticleCount = plugin.getConfig().getInt("customitems.enchants.ancient_curse.particle_count");
		yetisBlessing = Languages.getString("misc.yetiBlessing");
		yetisBlessingRange = new int[] {plugin.getConfig().getInt("customitems.enchants.yetis_blessing.level 1.range"), plugin.getConfig().getInt("customitems.enchants.yetis_blessing.level 2.range"), plugin.getConfig().getInt("customitems.enchants.yetis_blessing.level 3.range")};
		yetisBlessingChance = new int[] {plugin.getConfig().getInt("customitems.enchants.yetis_blessing.level 1.chance")-1, plugin.getConfig().getInt("customitems.enchants.yetis_blessing.level 2.chance")-1, plugin.getConfig().getInt("customitems.enchants.yetis_blessing.level 3.chance")-1};
		poseidonsTridentRange = new int[] {plugin.getConfig().getInt("customitems.enchants.tidal_wave.level 1.range")};
		bloodSacrifice = Languages.getString("misc.bloodSacrifice");
		bunnyHopMultiplier = plugin.getConfig().getDouble("customitems.enchants.bunny_hop.level 1.jump_multiplier");
		bunnyHopParticles = plugin.getConfig().getBoolean("customitems.enchants.bunny_hop.level 1.particles");
		etherealLanternLifeTicks = plugin.getConfig().getInt("customitems.items.ethereal_lantern.ghoul_life_ticks");
		etherealLanternGhoulCount = plugin.getConfig().getInt("customitems.items.ethereal_lantern.amount_of_ghouls");
	}
	private void spawnSouls(Location loc, LivingEntity entity)  {
		LivingEntity[] souls = new LivingEntity[ItemsHandler.soulRipperNumberOfSouls];
		for (int i=0; i < ItemsHandler.soulRipperNumberOfSouls; i++) {
			Location temp = loc.clone().add(rand.nextInt(10)-5,0,rand.nextInt(10)-5);
			if (temp.getBlock().isPassable())
				temp = Utils.getBlockBelow(temp).getLocation().clone().add(0.5,0.5,0.5);
			else
				temp = Utils.getBlockAbove(temp).getLocation().clone().add(0.5,0.5,0.5);
			Mob vex = (Mob) loc.getWorld().spawnEntity(temp, EntityType.VEX);
			CustomEntity.handler.addEntity(new TamedLostSoul(vex, plugin, rand, entity));
			souls[i] = vex;
			temp.getWorld().spawnParticle(Particle.SQUID_INK, temp.clone().add(0,0.75,0), 20, .4, .4, .4, 0.0001);
			temp.getWorld().playSound(temp, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, SoundCategory.PLAYERS, 1, 0.8f);
			temp.getWorld().playSound(temp, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.3f, 1.5f);
		}
		plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
			@Override
			public void run() {
				for (LivingEntity soul : souls)
					if (soul != null)
						soul.remove();
			}
		}, ItemsHandler.soulRipperSoulLifeTicks);
	}
	private void yetiRoar(LivingEntity entity, int level, double[] data) {
		Location entityHit = entity.getLocation().clone();
		World world = entityHit.getWorld();
		BlockVector block = new BlockVector(entityHit.getX(), entityHit.getY(), entityHit.getZ());
		BlockData bd = Material.PACKED_ICE.createBlockData();
		if (plugin.mcVersion >= 1.16)
			world.playSound(entityHit, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.HOSTILE, (1f/3f)*((float) level), 0.6f);
		world.playSound(entityHit, Sound.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, (2f/3f)*((float) level), .5f);
		int[] time = {1};
		new RepeatingTask(plugin, 0, 5) {
		@Override
		public void run() {
			time[0]++;
				for (int x = -time[0]; x < time[0]; x++)
					for (int z = -time[0]; z < time[0]; z++) {
						Vector position = block.clone().add(new Vector(x, 0, z));
						Block b = world.getBlockAt(position.toLocation(world));
						if (block.distance(position) >= (time[0] - 1) && block.distance(position) <= time[0]) {
							if (b.isPassable()) {
								for (int i = 0; i < 3; i++) {
									b = b.getRelative(BlockFace.DOWN);
									if (!b.isPassable())
										break;
								}
								if (b.isPassable())
									continue;
								else
									b = b.getRelative(BlockFace.UP);
							} else {
								for (int i = 0; i < 3; i++) {
									b = b.getRelative(BlockFace.UP);
									if (b.isPassable())
										break;
								}
								if (!b.isPassable())
									continue;
							}
							world.spawnParticle(Particle.SNOW_SHOVEL, b.getLocation().clone().add(0.5, 0.5, 0.5), 20, .3, .5, .3, 0.001);
							world.spawnParticle(Particle.BLOCK_CRACK, b.getLocation().clone().add(0.5, 0.5, 0.5), 3, .3, .5, .3, 0.001, bd);
							if (b.getType() == Material.FIRE) {
								b.setType(Material.AIR);
								world.playSound(b.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1f, 1f);
								world.spawnParticle(Particle.SMOKE_LARGE, b.getLocation().clone().add(0.5, 0.2, 0.5), 5, .3, .5, .3, 0.001, bd);
							}
							if (plugin.mcVersion >= 1.17)
								world.playSound(b.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.HOSTILE, 0.5f, 0.5f);
							else
								world.playSound(b.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 0.5f, 1f);
							for (Entity e : world.getNearbyEntities(b.getLocation().clone().add(0.5, 0.5, 0.5), 0.5, 1, 0.5)) {
								if (e.equals(entity))
									continue;
								e.setVelocity(new Vector(e.getLocation().getX() - entityHit.getX(), 0, e.getLocation().getZ() - entityHit.getZ()).normalize().multiply(0.2).setY(0.8));
								if (e instanceof LivingEntity && !(e instanceof Player && Utils.isPlayerImmune((Player) e))) {
									((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 7, true, false));
									if (plugin.mcVersion >= 1.17)
										e.setFreezeTicks(500);
								}
							}
						}
					}
				if (time[0] >= data[1])
					cancel();
			}
		};
	}
	private void castWaveSpell(Location location, Vector dir, int distance, Random rand, LivingEntity shooter) {
		World world = location.getWorld();
		int[] timer = {distance};
		Vector angle = new Vector(dir.getZ(), 0, -dir.getX());
		Location spot = location.clone().add(dir.clone().multiply(2)).add(angle.clone().multiply(-2));
		Block[] water = new Block[15];
		Queue<Block> allWaters = new ArrayDeque<>();
		Queue<Block> puddles = new ArrayDeque<>();
		new RepeatingTask(plugin, 0, 2) {
			@Override
			public void run() {
				for (Block b : water)
					if (b != null && b.getType() == Material.WATER) {
						world.spawnParticle(Particle.FALLING_WATER, b.getLocation().add(.5,.5,.5), 30, .5, .5, .5, 0.0001);
						if (rand.nextInt(4) == 0 && b.getRelative(BlockFace.DOWN).getType().isSolid()) {
							puddles.add(b);
							allWaters.remove(b);
							Levelled data = ((Levelled) b.getBlockData());
							data.setLevel(7);
							b.setBlockData(data);
							Monsoon.globalPuddles.add(b);
						} else
							b.setType(Material.AIR);
					}
				if (timer[0] <= 0) {
					for (Block b : allWaters)
						if (b != null)
							b.setType(Material.AIR);
					if (timer[0] <= -10) {
						cancel();
						new RepeatingTask(plugin, 60, 1) {
							@Override
							public void run() {
								if (puddles.isEmpty())
									cancel();
								Block b = puddles.poll();
								if (b != null && b.getType() == Material.WATER)
									b.setType(Material.AIR);
								Monsoon.globalPuddles.remove(b);
							}
						};
					}
					timer[0]--;
					return;
				}
				timer[0]--;
				for (int cycle=0; cycle < 3; cycle++) {
					Location line = spot.clone().add(dir.clone().multiply(cycle)).add(0,cycle,0);
					for (int i=0; i < 5; i++) {
						Block b = line.clone().add(angle.clone().multiply(i)).getBlock();
						if (b.getType() != Material.AIR || Utils.isZoneProtected(b.getLocation()))
							continue;
						allWaters.add(b);
						world.spawnParticle(Particle.BUBBLE_POP, b.getLocation().add(.5,.5,.5), 10, .5, .5, .5, 0.0001);
						b.setType(Material.WATER);
						water[i+(cycle*5)] = b;
						for (Entity e : world.getNearbyEntities(b.getLocation(), 0.5, 0.5, 0.5)) {
							if (e.equals(shooter))
								continue;
							e.setVelocity(dir);
							if (e instanceof LivingEntity && !(e instanceof Player && Utils.isPlayerImmune((Player) e)))
								((LivingEntity) e).damage(4);
						}
					}
				}
				spot.add(dir);
				world.playSound(spot, Sound.WEATHER_RAIN, SoundCategory.HOSTILE, 1, 0.75f);
			}
		};
	}
	public void createBloodWorm(Location startPos, LivingEntity target, ItemStack item, LivingEntity damager) {
		FallingBlock[] blocks = new FallingBlock[30];
		ArmorStand[] stands = new ArmorStand[16];
		int[] frame = {0, 0, 0};
		double[] damageM = {0};
		Location loc = startPos.clone();
		BlockData bd = Material.REDSTONE_BLOCK.createBlockData();
		World world = loc.getWorld();
		EvokerFangs[] fangs = {(EvokerFangs) world.spawnEntity(target.getLocation().subtract(0,0.5,0), EntityType.EVOKER_FANGS)};
		Vector[] velocities = new Vector[60];
		boolean[] force = {true};
		for (int i=0; i < 30; i++) {
			velocities[i] = new Vector(0.005, 0, 0.005);
			velocities[i+30] = velocities[i].clone().multiply(10);
		}
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				if (target.isDead())
					force[0] = false;
				for (int i=0; i < 30; i++)
					if (blocks[i] != null) {
						blocks[i].setVelocity(blocks[i].getVelocity().add(velocities[i]));
						if ((velocities[i+30].getX() > 0 && blocks[i].getVelocity().getX() >= velocities[i+30].getX()) || (velocities[i+30].getX() < 0 && blocks[i].getVelocity().getX() <= velocities[i+30].getX())) {
							velocities[i].multiply(-1);
							velocities[i+30].multiply(-1);
						}
					}
				for (int i=0; i < 16; i++)
					if (stands[i] != null) {
						stands[i].teleport(blocks[Math.max(0, i*2-1)].getLocation());
						stands[i].setHeadPose(stands[i].getHeadPose().add(Math.toRadians(rand.nextInt(6)-3), 0, 0));
					}
				if (frame[1] <= 0) {
					for (int i=0; i < 30; i++)
						if (blocks[i] != null)
							blocks[i].setVelocity(blocks[i].getVelocity().setY(.32));
					if (frame[0] % 2 == 0)
						blocks[(int) (frame[0] / 2)] = createBlockForBloodWorm(loc.clone().subtract((rand.nextDouble()-0.5)/4, 0, (rand.nextDouble()-0.5)/4));
					if (frame[0] > 4 && frame[0] % 4 == 0 && rand.nextInt(2) == 0)
						stands[(int) (frame[0] / 4)] = createStandForBloodWorm(blocks[(int) (frame[0] / 2)].getLocation());
					frame[0]++;
					fangs[0].remove();
					fangs[0] = (EvokerFangs) world.spawnEntity(blocks[0].getLocation().clone().add(0,1.5,0), EntityType.EVOKER_FANGS);
					fangs[0].setSilent(true);
					damageM[0] += 100.0/60.0;
					if (blocks[29] != null || !force[0] || damager.isDead() || target.getLocation().add(0,2,0).getBlock().getType().isSolid()) {
						frame[1] = 1;
						fangs[0].setSilent(false);
						for (FallingBlock e : blocks)
							if (e != null)
								e.setVelocity(e.getVelocity().setY(0));
						if (damager.isDead())
							frame[1] = 40;
					}
					if (force[0] && !target.isDead() && !fangs[0].isDead() && !(target instanceof Player && Utils.isPlayerImmune((Player) target))) {
						if (!target.getWorld().equals(world) || target.getLocation().distanceSquared(fangs[0].getLocation()) >= 3)
							target.teleport(fangs[0].getLocation());
						target.setVelocity(Utils.getVectorTowards(target.getLocation(), fangs[0].getLocation()).multiply(0.3).setY(fangs[0].getLocation().getY()-target.getLocation().getY()));
					}
				} else {
					if (frame[0] <= 0) {
						if (frame[0] == -2) {
							fangs[0].remove();
							cancel();
							frame[2] = 1;
							return;
						}
						Location fangLoc = fangs[0].getLocation().subtract(0,.4,0);
						fangs[0].remove();
						fangs[0] = (EvokerFangs) world.spawnEntity(fangLoc, EntityType.EVOKER_FANGS);
						frame[0]--;
					} else if (frame[1] >= 40) {
						for (int i=0; i < 30; i++)
							if (blocks[i] != null)
								blocks[i].setVelocity(blocks[i].getVelocity().setY(-.4));
						if (blocks[0] != null) {
							fangs[0].remove();
							fangs[0] = (EvokerFangs) world.spawnEntity(blocks[0].getLocation().clone().add(0,1.5,0), EntityType.EVOKER_FANGS);
							fangs[0].setSilent(true);
						} else {
							Location fangLoc = fangs[0].getLocation().subtract(0,.4,0);
							fangs[0].remove();
							fangs[0] = (EvokerFangs) world.spawnEntity(fangLoc, EntityType.EVOKER_FANGS);
						}
						frame[0]--;
						if (frame[0] % 2 == 0) {
							if (blocks[(int) (frame[0] / 2)] != null) {
								blocks[(int) (frame[0] / 2)].remove();
								blocks[(int) (frame[0] / 2)] = null;
							}
						}
						if (frame[0] % 4 == 0 && stands[(int) (frame[0] / 4)] != null) {
							stands[(int) (frame[0] / 4)].remove();
							stands[(int) (frame[0] / 4)] = null;
						}
						if (force[0] && !target.isDead() && !fangs[0].isDead() && !(target instanceof Player && Utils.isPlayerImmune((Player) target))) {
							if (!target.getWorld().equals(world) || target.getLocation().distanceSquared(fangs[0].getLocation()) >= 3)
								target.teleport(fangs[0].getLocation());
							target.setVelocity(Utils.getVectorTowards(target.getLocation(), fangs[0].getLocation().subtract(0,2,0)).multiply(0.3).setY(fangs[0].getLocation().getY()-0.5-target.getLocation().getY()));
							target.setFallDistance(0);
						}
					} else if (frame[1] <= 25) {
						fangs[0].remove();
						if (frame[1] != 25) {
							fangs[0] = (EvokerFangs) world.spawnEntity(blocks[0].getLocation().clone().add(0,1.5,0), EntityType.EVOKER_FANGS);
							fangs[0].setSilent(true);
						} else {
							fangs[0] = (EvokerFangs) world.spawnEntity(blocks[0].getLocation().clone().add(0,1.0,0), EntityType.EVOKER_FANGS);
							bloodPactFangMap.put(fangs[0].getUniqueId(), new Object[] {damager.getUniqueId(), damageM[0], item});
						}
						if (force[0] && !target.isDead() && !fangs[0].isDead() && !(target instanceof Player && Utils.isPlayerImmune((Player) target))) {
							if (!target.getWorld().equals(world) || target.getLocation().distanceSquared(fangs[0].getLocation()) >= 3)
								target.teleport(fangs[0].getLocation());
							target.setVelocity(Utils.getVectorTowards(target.getLocation(), fangs[0].getLocation().subtract(0,2,0)).multiply(0.3).setY(fangs[0].getLocation().getY()-target.getLocation().getY()));
						}
					} else {
						if (force[0] && !target.isDead() && !fangs[0].isDead() && !(target instanceof Player && Utils.isPlayerImmune((Player) target))) {
							if (!target.getWorld().equals(world) || target.getLocation().distanceSquared(fangs[0].getLocation()) >= 3)
								target.teleport(fangs[0].getLocation());
							target.setVelocity(Utils.getVectorTowards(target.getLocation(), fangs[0].getLocation().subtract(0,2,0)).multiply(0.3).setY(fangs[0].getLocation().getY()-target.getLocation().getY()));
						}
					}
					frame[1]++;
				}
				for (FallingBlock e : blocks)
					if (e != null)
						world.spawnParticle(Particle.BLOCK_DUST, e.getLocation().clone().add(0,1.5,0), 1, .1, .1, .1, 0.1, bd);
			}
		};
		Location part = startPos.clone().add(0,.5,0);
		new AsyncRepeatingTask(plugin, 1, 1) {
			@Override
			public void run() {
				if (frame[2] == 1) {
					cancel();
					return;
				}
				for (int i=0; i < 50; i++) {
					double angle = Math.toRadians(360.0 / 50 * i);
					world.spawnParticle(Particle.FLAME, part.clone().add(Math.cos(angle)*2,0,Math.sin(angle)*2), 1, 0, 0, 0, 0.0001);
				}
				world.spawnParticle(Particle.LAVA, part, 4, .5, .05, .5, 0.1);

				for (int i = 0; i < 5; i++) {
					double angle = Math.toRadians(360.0 / 5 * i);
					double nextAngle = Math.toRadians(360.0 / 5 * (i + 2));
					double x = Math.cos(angle) * 2.5;
					double z = Math.sin(angle) * 2.5;
					double deltaX = (Math.cos(nextAngle) * 2.5) - x;
					double deltaZ = (Math.sin(nextAngle) * 2.5) - z;
					double distance = Math.sqrt((deltaX - x) * (deltaX - x) + (deltaZ - z) * (deltaZ));
					for (double d = 0; d < distance / 6.7; d += .05)
						world.spawnParticle(Particle.FLAME, part.clone().add(x + (deltaX * d), 0, z + (deltaZ * d)), 1, 0, 0, 0, 0.0001);
				}
			}
		};
	}
	private FallingBlock createBlockForBloodWorm(Location loc) {
		FallingBlock fb = loc.getWorld().spawnFallingBlock(loc, Material.CHAIN.createBlockData());
		fb.setGravity(false);
		fb.setDropItem(false);
		fb.setMetadata("dd-fbcancel", plugin.fixedData);
		return fb;
	}
	private ArmorStand createStandForBloodWorm(Location loc) {
		loc.setYaw(rand.nextInt(360));
		ArmorStand e = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
		if (plugin.mcVersion >= 1.16)
			e.setInvisible(true);
		else
			e.setVisible(false);
		e.setGravity(false);
		e.setMarker(true);
		e.setSmall(true);
		if (plugin.mcVersion >= 1.16) {
			e.addEquipmentLock(EquipmentSlot.CHEST, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			e.addEquipmentLock(EquipmentSlot.FEET, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			e.addEquipmentLock(EquipmentSlot.HEAD, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			e.addEquipmentLock(EquipmentSlot.LEGS, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
		}
		e.getEquipment().setHelmet(CustomHead.BLOODWORMEYE.getHead());
		e.setHeadPose(new EulerAngle(Math.toRadians(rand.nextInt(180)), 0, 0));
		e.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		return e;
	}
	private void spawnGhouls(LivingEntity spawner, LivingEntity target) {
		int c = 0;
		Set<Block> prev = new HashSet<>();
		Queue<Ghoul> ghouls = new ArrayDeque<>();
		for (int i=0; i < 20; i++) {
			Location spawn = Utils.findSmartYSpawn(target.getLocation(), Utils.getSpotInSquareRadius(target.getLocation(), rand.nextInt(etherealLanternGhoulCount * 2)+3), 2, 5);
			if (spawn == null || prev.contains(spawn.getBlock()))
				continue;
			spawn.subtract(0,1,0);
			prev.add(spawn.getBlock());
			plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
				Zombie zombie = spawn.getWorld().spawn(spawn, Zombie.class, false, consumer -> {
					consumer.setRotation(plugin.random.nextFloat()*360, 0);
				});
				Ghoul ghoul = CustomEntity.handler.addEntity(new Ghoul(zombie, spawn.getBlock(), plugin, true));
				ghouls.add(ghoul);
				plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
					ghoul.setWalking(true);
					ghoul.getEntity().setVelocity(new Vector(0,.4,0));
					ghoul.grabAnimation.stop();
				}, 60);
			}, rand.nextInt(80));
			if (++c >= etherealLanternGhoulCount)
				break;
		}
		if (c > 0) {
			int[] life = {etherealLanternLifeTicks};
			boolean particles = spawner instanceof Player;
			DustTransition dust = new DustTransition(Color.BLUE, Color.BLACK, 0.5f);
			new RepeatingTask(plugin, 60, 1) {
				@Override
				public void run() {
					Iterator<Ghoul> it = ghouls.iterator();
					if (life[0]-- <= 0) {
						while (it.hasNext()) {
							Ghoul e = it.next();
							if (e.getEntity() != null && !e.getEntity().isDead())
								e.dig();
						}
						cancel();
						return;
					}
					while (it.hasNext()) {
						Ghoul e = it.next();
						if (e.getEntity() == null || e.getEntity().isDead()) {
							it.remove();
							return;
						}
						if (target == null || target.isDead()) {
							e.dig();
							it.remove();
							return;
						}
						((Mob) e.getEntity()).setTarget(target);
						if (particles && spawner != null)
							((Player) spawner).spawnParticle(Particle.DUST_COLOR_TRANSITION, e.getEntity().getLocation().add(0,1,0), 4, .4, .6, .4, 0.001, dust);
					}
					if (ghouls.isEmpty())
						cancel();
				}
			};
		}
	}
}
