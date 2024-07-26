package com.github.jewishbanana.deadlydisasters.events.disasters;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.events.Disaster;
import com.github.jewishbanana.deadlydisasters.events.WeatherDisaster;
import com.github.jewishbanana.deadlydisasters.events.WeatherDisasterEvent;
import com.github.jewishbanana.deadlydisasters.handlers.SeasonsHandler;
import com.github.jewishbanana.deadlydisasters.handlers.WorldObject;
import com.github.jewishbanana.deadlydisasters.listeners.DeathMessages;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class Blizzard extends WeatherDisaster {
	
	private boolean freeze,leather,seasonsAllowed,despawnEntities;
	private int minTemp,freezeHeight,particleRange,particleYRange;
	private double damage,particleMultiplier;
	private Queue<UUID> entities = new ArrayDeque<>();
	
	private Map<UUID,UUID> targets = new HashMap<>();
	
	private me.casperge.realisticseasons.api.SeasonsAPI seasons;
	
	public Blizzard(int level) {
		super(level);
		freeze = configFile.getBoolean("blizzard.freeze_entities");
		despawnEntities = configFile.getBoolean("blizzard.despawn_frozen_entities");
		leather = configFile.getBoolean("blizzard.leather_armor_protection");
		time = configFile.getInt("blizzard.time.level "+this.level) * 20;
		delay = configFile.getInt("blizzard.start_delay") * 20;
		damage = configFile.getDouble("blizzard.damage");
		freezeHeight = configFile.getInt("blizzard.min_freezing_height");
		if (damage < 0)
			damage = 0;
		seasonsAllowed = plugin.seasonsHandler.isActive;
		if (seasonsAllowed) {
			seasons = SeasonsHandler.getSeasonsAPI();
			minTemp = plugin.seasonsHandler.blizzTemp;
		}
		volume = configFile.getDouble("blizzard.volume");
		particleRange = configFile.getInt("blizzard.particle_max_distance");
		particleYRange = configFile.getInt("blizzard.particle_Y_range");
		particleMultiplier = 1.0 * configFile.getDouble("blizzard.particle_multiplier");
		
		this.type = Disaster.BLIZZARD;
	}
	public void start(World world, Player p, boolean broadcastAllowed) {
		WeatherDisasterEvent event = new WeatherDisasterEvent(this, world, level, p);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		this.world = world;
		updateWeatherSettings();
		ongoingDisasters.add(this);
		if (broadcastAllowed && (boolean) WorldObject.findWorldObject(world).settings.get("event_broadcast"))
			Utils.broadcastEvent(level, "weather", this.type, world);
		DeathMessages.blizzards.add(this);
		Blizzard obj = this;
		int[] cycles = {0};
		Random rand = plugin.random;
		NamespacedKey key = new NamespacedKey(plugin, "dd-frozen-mob");
		new RepeatingTask(plugin, delay, 5) {
			@Override
			public void run() {
				if (time <= 0) {
					cancel();
					DeathMessages.blizzards.remove(obj);
					ongoingDisasters.remove(obj);
					clearEntities();
					return;
				}
				time -= 5;
				for (LivingEntity all : world.getLivingEntities()) {
					if (entities.contains(all.getUniqueId()) && ((Mob) all).getTarget() == null && Bukkit.getEntity(targets.get(all.getUniqueId())) != null)
						((Mob) all).setTarget((LivingEntity) Bukkit.getEntity(targets.get(all.getUniqueId())));
					if (!all.hasMetadata("dd-yeti") && !isEntityTypeProtected(all) && ((!seasonsAllowed && all.getLocation().getBlock().getTemperature() <= 0.15)
							|| (seasonsAllowed && all instanceof Player && seasons.getTemperature((Player) all) <= minTemp))) {
						if (Utils.isWeatherDisabled(all.getLocation(), obj))
							continue;
						Block b = all.getLocation().getBlock();
						if (b.getLightFromBlocks() >= (byte) 13) {
							Location loc = all.getLocation();
							int px = loc.getBlockX(), py = loc.getBlockY(), pz = loc.getBlockZ();
							for (int x = px - 1; x < px + 1; x++) {
								for (int y = py - 1; y < py + 1; y++) {
									for (int z = pz - 1; z < pz + 1; z++) {
										loc.setX(x);
										loc.setY(y);
										loc.setZ(z);
										b = loc.getBlock();
										if (b.getType() == Material.TORCH || b.getType() == Material.WALL_TORCH) {
											if (plugin.CProtect)
												Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(),
														b.getType(), b.getBlockData());
											b.setType(Material.AIR);
											ItemStack item = new ItemStack(Material.STICK, 1);
											world.dropItem(b.getLocation(), item);
										}
									}
								}
							}
						} else {
							all.addPotionEffect(new PotionEffect(VersionUtils.getSlowness(), 20, 2, true, false));
							if (all.getType() == EntityType.STRAY || all.getType() == EntityType.POLAR_BEAR)
								continue;
							double tempDamage = damage;
							if (leather) {
								ItemStack[] armor = all.getEquipment().getArmorContents();
								if (armor[0] != null && armor[0].getType() == Material.LEATHER_BOOTS)
									tempDamage -= damage / 4;
								if (armor[1] != null && armor[1].getType() == Material.LEATHER_LEGGINGS)
									tempDamage -= damage / 4;
								if (armor[2] != null) {
									if (armor[2].getType() == Material.LEATHER_CHESTPLATE)
										tempDamage -= damage / 4;
									int level = DependencyUtils.getYetisBlessingLevel(armor[2]);
									if (level > 0)
										tempDamage -= (damage / 4) * (level + 1);
								}
								if (armor[3] != null && armor[3].getType() == Material.LEATHER_HELMET)
									tempDamage -= damage / 4;
								if (tempDamage <= 0)
									continue;
							}
							if (!all.isInvulnerable()) {
								all.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20, 0, true, false));
								Utils.pureDamageEntity(all, tempDamage, "dd-blizzarddeath", false, null);
							} else
								all.removePotionEffect(PotionEffectType.WITHER);
							if (all.getHealth() <= 10D) {
								if (all instanceof Player)
									Utils.pureDamageEntity(all, tempDamage, "dd-blizzarddeath", false, null);
								else if (freeze && !all.isInvulnerable() && all.getLocation().getBlockY() >= freezeHeight) {
									all.setAI(false);
									all.setInvulnerable(true);
									all.setSilent(true);
									if (all.getRemoveWhenFarAway())
										all.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 0);
									else
										all.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
									if (!despawnEntities)
										all.setRemoveWhenFarAway(false);
									Location tp = new Location(all.getWorld(), all.getLocation().getBlockX()+0.5, all.getLocation().getBlockY(), all.getLocation().getBlockZ()+0.5);
									all.teleport(tp);
									Block eb = all.getLocation().getBlock();
									for (int i = 0; i < all.getHeight(); i++) {
										if (!Utils.passStrengthTest(eb.getType())) {
											if (plugin.CProtect)
												Utils.getCoreProtect().logPlacement("Deadly-Disasters",
														eb.getLocation(), Material.ICE, eb.getBlockData());
											eb.setType(Material.ICE);
										}
										eb = eb.getRelative(BlockFace.UP);
									}
								}
							}
						}
					}
				}
				if (!world.hasStorm())
					world.setStorm(true);
				if (cycles[0] >= 40)
					cycles[0] = 0;
				cycles[0]++;
			}
		};
		BukkitTask[] task = new BukkitTask[1];
		task[0] = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				if (time <= 0) {
					task[0].cancel();
					return;
				}
				for (Player p : world.getPlayers()) {
					if (!p.getWorld().equals(world))
						continue;
					Location closest = null;
					for (int x=-particleRange; x <= particleRange; x++)
						for (int z=-particleRange; z <= particleRange; z++) {
							if (rand.nextDouble() >= particleMultiplier)
								continue;
							Location temp = p.getLocation().add(x,0,z);
							Location b = world.getHighestBlockAt(temp).getLocation();
							if ((!seasonsAllowed && b.getBlock().getTemperature() > 0.15) || Utils.isWeatherDisabled(b, obj))
								continue;
							int diff = b.getBlockY() - temp.getBlockY();
							if (diff > particleYRange)
								continue;
							if (x == particleRange || x == -particleRange || z == particleRange || z == -particleRange) {
								p.spawnParticle(Particle.CLOUD, b.add(0.5,3,0.5), 2, 0.5, 0.7, 0.5, 0.05);
								continue;
							}
							if (diff < 0)
								b.setY(b.getY()+(diff*-1));
							if (closest == null || b.distanceSquared(p.getLocation()) < closest.distanceSquared(p.getLocation()))
								closest = b;
							if (diff > 0)
								p.spawnParticle(Particle.CLOUD, b.add(0.5,3,0.5), 2, 0.5, 0.7, 0.5, 0.05);
							else
								for (int i=0; i < 2; i++)
									p.spawnParticle(Particle.CLOUD, b.add(rand.nextDouble(),3+(rand.nextDouble()*2),rand.nextDouble()), 0, (rand.nextDouble()/2.5)-0.2, -(rand.nextDouble()/0.6), (rand.nextDouble()/2.5)-0.2);
						}
					if (closest != null)
						p.playSound(closest, Sound.WEATHER_RAIN_ABOVE, (float) (0.75*volume), 0.5F);
				}
			}
		}, delay, 1);
	}
	@Override
	public void clear() {
		time = 0;
		clearEntities();
	}
	public void clearEntities() {
		for (UUID e : entities)
			if (Bukkit.getEntity(e) != null)
				Bukkit.getEntity(e).remove();
	}
	public boolean canMobsFreeze() {
		return freeze;
	}
	public void setMobsFreeze(boolean value) {
		this.freeze = value;
	}
	public boolean canLeatherProtect() {
		return leather;
	}
	public void setLeatherProtect(boolean value) {
		this.leather = value;
	}
	public static void refreshFrozen(Main plugin) {
		NamespacedKey key = new NamespacedKey(plugin, "dd-frozen-mob");
		for (World world : Bukkit.getWorlds())
			if (world.getEnvironment() == Environment.NORMAL)
				for (LivingEntity e : world.getLivingEntities())
					if (e.getPersistentDataContainer().has(key, PersistentDataType.BYTE) && e.getLocation().add(0,Math.max(0, e.getHeight()-0.5),0).getBlock().getType() != Material.ICE) {
						e.setInvulnerable(false);
						((LivingEntity) e).setAI(true);
						if (e.getPersistentDataContainer().get(key, PersistentDataType.BYTE) == (byte) 0)
							((LivingEntity) e).setRemoveWhenFarAway(true);
						e.getPersistentDataContainer().remove(key);
						e.setSilent(false);
					}
	}
	public static void breakIce(Block b, Main plugin) {
		NamespacedKey key = new NamespacedKey(plugin, "dd-frozen-mob");
		for (Entity e : b.getWorld().getNearbyEntities(b.getLocation().clone().add(.5,.5,.5), .5, 1.25, .5)) {
			if (!(e instanceof LivingEntity) || !e.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) continue;
			if (e.getHeight() > 1 && e.getLocation().add(0,1,0).getBlock().getType().equals(Material.ICE) && !(e.getLocation().add(0,1,0).getBlock().equals(b))) continue;
			e.setInvulnerable(false);
			((LivingEntity) e).setAI(true);
			if (e.getPersistentDataContainer().get(key, PersistentDataType.BYTE) == (byte) 0)
				((LivingEntity) e).setRemoveWhenFarAway(true);
			e.getPersistentDataContainer().remove(key);
			e.setSilent(false);
			plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
				@Override
				public void run() {
					if (b.getType() == Material.WATER)
						b.setType(Material.AIR);
				}
			}, 1);
			return;
		}
	}
}
