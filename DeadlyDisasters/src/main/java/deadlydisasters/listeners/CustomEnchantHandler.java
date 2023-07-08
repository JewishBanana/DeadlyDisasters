package deadlydisasters.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
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
import org.bukkit.block.data.BlockData;
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
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import deadlydisasters.disasters.events.DisasterEvent;
import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.christmasentities.ElfPet;
import deadlydisasters.entities.soulstormentities.TamedLostSoul;
import deadlydisasters.general.ItemsHandler;
import deadlydisasters.general.Main;
import deadlydisasters.listeners.customevents.ArmorEquipEvent;
import deadlydisasters.listeners.customevents.ArmorUnequipEvent;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public class CustomEnchantHandler implements Listener {
	
	private Main plugin;
	private Random rand;
	
	private Map<UUID,Integer> ancientBladeCooldownMap = new HashMap<UUID,Integer>();
	private Map<UUID,Integer> soulRipperCooldownMap = new HashMap<UUID,Integer>();
	
	private boolean bunnyHopParticles;
	
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
									plugin.handler.addEntity(new ElfPet((Zombie) p.getWorld().spawnEntity(p.getLocation(), EntityType.ZOMBIE), plugin, rand, p.getUniqueId(), false)),
									plugin.handler.addEntity(new ElfPet((Zombie) p.getWorld().spawnEntity(p.getLocation(), EntityType.ZOMBIE), plugin, rand, p.getUniqueId(), false)),
									plugin.handler.addEntity(new ElfPet((Zombie) p.getWorld().spawnEntity(p.getLocation(), EntityType.ZOMBIE), plugin, rand, p.getUniqueId(), true))
							};
							for (ElfPet elf : elves)
								plugin.handler.addEntity(elf);
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
											elves[i] = plugin.handler.addEntity(new ElfPet((Zombie) p.getWorld().spawnEntity(p.getLocation(), EntityType.ZOMBIE), plugin, rand, p.getUniqueId(), true));
										else
											elves[i] = plugin.handler.addEntity(new ElfPet((Zombie) p.getWorld().spawnEntity(p.getLocation(), EntityType.ZOMBIE), plugin, rand, p.getUniqueId(), false));
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
		if (e.getDamager().hasMetadata("dd-elfarrow") && e.getEntity().hasMetadata("dd-christmasmob")) {
			e.setCancelled(true);
			return;
		} else if (e.getDamager().hasMetadata("dd-petelfarrow")) {
			CustomEntity ce = plugin.handler.findEntity((LivingEntity) ((Arrow) e.getDamager()).getShooter());
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
		if (!item.hasItemMeta())
			return;
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
			int[] spellLife = {20};
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
					tempW.spawnParticle(Particle.FLAME, spell, 10, 1, 1, 1, .05);
					tempW.spawnParticle(Particle.BLOCK_DUST, spell, 10, 1, 1, 1, .1, bd);
					for (Entity entity : spell.getWorld().getNearbyEntities(spell, 1.5, 1.5, 1.5))
						if (entity instanceof LivingEntity && !entity.equals(e.getPlayer())) {
							entity.setFireTicks(80);
							entity.setVelocity(motion.clone().multiply(0.5));
						}
				}
			};
		}
	}
	@EventHandler
	public void onToggleFlight(PlayerToggleFlightEvent e) {
		if (bunnyHopPlayers.contains(e.getPlayer().getUniqueId()) && !hoppingPlayers.contains(e.getPlayer().getUniqueId()) && !Utils.isPlayerImmune(e.getPlayer())) {
			Player p = e.getPlayer();
			e.setCancelled(true);
            p.setAllowFlight(false);
            p.setFlying(false);
            p.setVelocity(e.getPlayer().getLocation().getDirection().multiply(1.25).setY(0.8));
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
	public void reload() {
		bunnyHopParticles = plugin.getConfig().getBoolean("customitems.enchants.bunny_hop.level 1.particles");
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
			plugin.handler.addEntity(new TamedLostSoul(vex, plugin, rand, entity));
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
}
