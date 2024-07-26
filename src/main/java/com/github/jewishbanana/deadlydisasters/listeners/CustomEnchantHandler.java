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
import java.util.stream.Stream;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustTransition;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.christmasentities.ElfPet;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.Ghoul;
import com.github.jewishbanana.deadlydisasters.entities.soulstormentities.TamedLostSoul;
import com.github.jewishbanana.deadlydisasters.events.DisasterEvent;
import com.github.jewishbanana.deadlydisasters.handlers.ItemsHandler;
import com.github.jewishbanana.deadlydisasters.listeners.customevents.ArmorEquipEvent;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class CustomEnchantHandler implements Listener {
	
	private Main plugin;
	private Random rand;
	
	private Map<UUID,Integer> soulRipperCooldownMap = new HashMap<UUID,Integer>();
	private Map<UUID,Integer> etheralLanternCooldown = new HashMap<UUID,Integer>();
	
	private int etherealLanternLifeTicks = 300;
	private int etherealLanternGhoulCount = 2;
	
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
					if (pet != null)
						pet.target = (LivingEntity) e.getEntity();
			} else if (santaHatPlayers.containsKey(e.getEntity().getUniqueId()) && santaHatPlayers.get(e.getEntity().getUniqueId()) != null
					&& !Stream.of(santaHatPlayers.get(e.getEntity().getUniqueId())).anyMatch(n -> n != null && n.getEntity().equals(e.getDamager()))) {
				for (ElfPet pet : santaHatPlayers.get(e.getEntity().getUniqueId()))
					if (pet != null)
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
				if (!soulRipperCooldownMap.containsKey(damager.getUniqueId()) && item.getItemMeta().hasLore()
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
			}
		}
		if (CustomEntityType.mobsEnabled && !etheralLanternCooldown.containsKey(damager.getUniqueId()) && rand.nextDouble() < ItemsHandler.etherealLanternChance
				&& damager.getEquipment().getItemInOffHand().hasItemMeta()
				&& damager.getEquipment().getItemInOffHand().getItemMeta().getPersistentDataContainer().has(ItemsHandler.etherealLanternKey, PersistentDataType.BYTE)) {
			etheralLanternCooldown.put(damager.getUniqueId(), ItemsHandler.etherealLanternCooldown);
			spawnGhouls(damager, entity);
		}
	}
	public void reload() {
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
