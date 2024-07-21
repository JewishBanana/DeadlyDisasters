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
import org.bukkit.block.data.BlockData;
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
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
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
import com.github.jewishbanana.deadlydisasters.handlers.ItemsHandler;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.listeners.customevents.ArmorEquipEvent;
import com.github.jewishbanana.deadlydisasters.utils.AsyncRepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

import net.md_5.bungee.api.ChatColor;

public class CustomEnchantHandler implements Listener {
	
	private Main plugin;
	private Random rand;
	
	private Map<UUID,Integer> soulRipperCooldownMap = new HashMap<UUID,Integer>();
	private Map<UUID,Integer> bloodPactCooldown = new HashMap<UUID,Integer>();
	private Map<UUID,Integer> etheralLanternCooldown = new HashMap<UUID,Integer>();
	
	private Map<UUID,Object[]> bloodPactFangMap = new HashMap<>();
	
	private int etherealLanternLifeTicks;
	private int etherealLanternGhoulCount;
	
	private String bloodSacrifice;
	
	public static Map<UUID, ElfPet[]> santaHatPlayers = new HashMap<>();
	
	public CustomEnchantHandler(Main plugin) {
		this.plugin = plugin;
		this.rand = plugin.random;
		
		reload();
		
		for (Player p : plugin.getServer().getOnlinePlayers()) {
			ItemStack item = p.getEquipment().getHelmet();
			if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(ItemsHandler.santaHatKey, PersistentDataType.INTEGER))
				santaHatPlayers.put(p.getUniqueId(), null);
		}
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				Iterator<Entry<UUID, Integer>> it = soulRipperCooldownMap.entrySet().iterator();
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
			}
		};
	}
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		ItemStack item = p.getEquipment().getHelmet();
		if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(ItemsHandler.santaHatKey, PersistentDataType.INTEGER))
			santaHatPlayers.put(p.getUniqueId(), null);
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
	}
	@EventHandler
	public void onArmorEquip(ArmorEquipEvent e) {
		if (e.getSlot() == ArmorListener.ArmorSlot.HEAD && e.getItem().hasItemMeta() && e.getItem().getItemMeta().getPersistentDataContainer().has(ItemsHandler.santaHatKey, PersistentDataType.INTEGER))
			santaHatPlayers.put(e.getPlayer().getUniqueId(), null);
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
					damager.getWorld().spawnParticle(VersionUtils.getBlockCrack(), damager.getLocation().add(0,damager.getHeight()/2,0), 50, .2, damager.getHeight()/2, .2, 1, Material.REDSTONE_BLOCK.createBlockData());
					Utils.damageItem(item, 20);
				}
				if (!(e.getEntity() instanceof Player) || !Utils.isPlayerImmune((Player) e.getEntity())) {
					e.getEntity().getWorld().spawnParticle(VersionUtils.getBlockCrack(), e.getEntity().getLocation().add(0,e.getEntity().getHeight()/2,0), 50, .2, e.getEntity().getHeight()/2, .2, 1, Material.REDSTONE_BLOCK.createBlockData());
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
						meta.addEnchant(VersionUtils.getSharpness(), 2, false);
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
	}
	public void reload() {
		bloodSacrifice = Languages.getString("misc.bloodSacrifice");
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
						world.spawnParticle(VersionUtils.getBlockDust(), e.getLocation().clone().add(0,1.5,0), 1, .1, .1, .1, 0.1, bd);
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
