package deadlydisasters.disasters;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import deadlydisasters.disasters.events.WeatherDisaster;
import deadlydisasters.disasters.events.WeatherDisasterEvent;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.monsoonentities.CursedDiver;
import deadlydisasters.general.Main;
import deadlydisasters.general.WorldObject;
import deadlydisasters.listeners.DeathMessages;
import deadlydisasters.utils.Metrics;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public class Monsoon extends WeatherDisaster {
	
	private int particleRange,particleYRange,airDecrease,blockDamageRange;
	private double puddleRate,blockChangeRate,leakRate,drownedRate,particleMultiplier;
	private int blocksDestroyed;
	
	private Queue<Block> puddles = new ArrayDeque<>();
	private Set<UUID> drowned = new HashSet<>();
	private Map<UUID,UUID> targets = new HashMap<>();
	
//	private Map<PotionEffectType, Integer> potionEffects =  new HashMap<>();
	private Map<Material, Material> blockChanges =  new HashMap<>();
	
	public static Queue<Block> globalPuddles = new ArrayDeque<>();

	public Monsoon(int level) {
		super(level);
		time = plugin.getConfig().getInt("monsoon.time.level "+this.level) * 20;
		delay = plugin.getConfig().getInt("monsoon.start_delay") * 20;
		volume = plugin.getConfig().getDouble("monsoon.volume");
		particleRange = plugin.getConfig().getInt("monsoon.particle_max_distance");
		particleYRange = plugin.getConfig().getInt("monsoon.particle_Y_range");
		particleMultiplier = 1.0 * plugin.getConfig().getInt("monsoon.particle_multiplier");
		puddleRate = 0.075 * plugin.getConfig().getDouble("monsoon.puddle_spawn_rate");
		airDecrease = (int) (7 * plugin.getConfig().getDouble("monsoon.entity_drowning_rate"));
		blockDamageRange = plugin.getConfig().getInt("monsoon.block_damage_range");
		blockChangeRate = 0.01 * plugin.getConfig().getDouble("monsoon.block_change_rate");
		leakRate = 0.1 * plugin.getConfig().getDouble("monsoon.water_leak_particles");
		drownedRate = 0.05 * plugin.getConfig().getDouble("monsoon.drowned_spawn_rate");
//		for (String effect : plugin.getConfig().getConfigurationSection("monsoon.effects").getKeys(false))
//			if (PotionEffectType.getByName(effect) != null)
//				potionEffects.putIfAbsent(PotionEffectType.getByName(effect), plugin.getConfig().getInt("monsoon.effects."+effect));
		for (String material : plugin.getConfig().getConfigurationSection("monsoon.block_changes").getKeys(false))
			if (Material.getMaterial(material.toUpperCase()) != null) {
				if (Material.getMaterial(plugin.getConfig().getString("monsoon.block_changes."+material).toUpperCase()) != null)
					blockChanges.putIfAbsent(Material.getMaterial(material.toUpperCase()), Material.getMaterial(plugin.getConfig().getString("monsoon.block_changes."+material).toUpperCase()));
				else if (plugin.getConfig().getString("monsoon.block_changes."+material).toLowerCase().equals("air"))
					blockChanges.putIfAbsent(Material.getMaterial(material), Material.AIR);
				else
					Main.consoleSender.sendMessage(Utils.chat("&e[DeadlyDisasters]: Could not find material &c'"+plugin.getConfig().getString("monsoon.block_changes."+material)+"' &eon line &d'"+material+" : "+plugin.getConfig().getString("monsoon.block_changes."+material)+"' &ein monsoon block changes section in the config!"));
			} else
				Main.consoleSender.sendMessage(Utils.chat("&e[DeadlyDisasters]: Could not find material &c'"+material+"' &eon line &d'"+material+" : "+plugin.getConfig().getString("monsoon.block_changes."+material)+"' &ein monsoon block changes section in the config!"));
		
		this.type = Disaster.MONSOON;
	}
	@Override
	public void start(World world, Player p, boolean broadcastAllowed) {
		WeatherDisasterEvent event = new WeatherDisasterEvent(this, world, level, p);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		this.world = world;
		updateWeatherSettings();
		ongoingDisasters.add(this);
		if (broadcastAllowed && (boolean) WorldObject.findWorldObject(world).settings.get("event_broadcast"))
			Utils.broadcastEvent(level, "weather", this.type, world);
		DeathMessages.monsoons.add(this);
		Monsoon instance = this;
		Random rand = plugin.random;
		Queue<LivingEntity> outsideEntities = new ArrayDeque<>();
		new RepeatingTask(plugin, delay, 20) {
			@Override
			public void run() {
				if (time <= 0) {
					clear();
					cancel();
					world.setStorm(false);
					ongoingDisasters.remove(instance);
					triggerRegen();
					Metrics.incrementValue(Metrics.disasterDestroyedMap, type.getMetricsLabel(), blocksDestroyed);
					return;
				}
				time -= 20;
				if (!world.hasStorm()) {
					world.setStorm(true);
					world.setThunderDuration(time);
					world.setThundering(true);
				}
				outsideEntities.clear();
				for (Entity all : world.getEntities()) {
					if (drowned.contains(all.getUniqueId()) && ((Mob) all).getTarget() == null && Bukkit.getEntity(targets.get(all.getUniqueId())) != null)
						((Mob) all).setTarget((LivingEntity) Bukkit.getEntity(targets.get(all.getUniqueId())));
					Location temp = all.getLocation();
					if (temp.getBlock().getTemperature() <= 0.15 || temp.getBlock().getTemperature() > 0.95) continue;
					if (Utils.isWeatherDisabled(temp, instance)) continue;
					if (all instanceof LivingEntity) {
						if (all instanceof Player) {
							if (Utils.isPlayerImmune((Player) all)) continue;
							if (rand.nextDouble() < drownedRate) {
								Location spawn = world.getHighestBlockAt(Utils.getSpotInSquareRadius(temp, 20)).getRelative(BlockFace.UP).getLocation();
								if (Math.abs(spawn.getBlockY() - temp.getBlockY()) <= 15) {
									Drowned entity = (Drowned) world.spawnEntity(spawn, EntityType.DROWNED);
									if (rand.nextInt(10) == 0 && CustomEntityType.CURSEDDIVER.canSpawn())
										plugin.handler.addEntity(new CursedDiver(entity, plugin, rand));
									entity.setTarget((LivingEntity) all);
									drowned.add(entity.getUniqueId());
									targets.put(entity.getUniqueId(), all.getUniqueId());
									entity.setMetadata("dd-monsoonmob", plugin.fixedData);
								}
							}
						}
						if (all instanceof Drowned || all.getWorld().getHighestBlockYAt(temp) > temp.getBlockY()+1)
							continue;
						LivingEntity entity = (LivingEntity) all;
						outsideEntities.add(entity);
						entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 30, 2, true, false, false));
						if (rand.nextInt(3) == 0 && entity.getLocation().getBlock().getType() == Material.WATER)
							entity.setVelocity(new Vector((rand.nextDouble()-0.5), 0.15, (rand.nextDouble()-0.5)));
					}
				}
			}
		};
		new RepeatingTask(plugin, delay, 1) {
			@Override
			public void run() {
				if (time <= 0) {
					cancel();
					return;
				}
				for (LivingEntity e : outsideEntities)
					if (e.getRemainingAir() > -10)
						e.setRemainingAir(e.getRemainingAir()-airDecrease);
					else if (!e.isDead() && e.getLocation().add(0,e.getHeight(),0).getBlock().getType() != Material.WATER) {
						e.damage(0.001);
						e.setLastDamageCause(new EntityDamageEvent(e, DamageCause.CUSTOM, 0.5));
						if (e.getHealth()-0.5 <= 0)
							e.setMetadata("dd-monsoondrown", plugin.fixedData);
						e.setHealth(Math.max(e.getHealth()-0.5, 0));
					}
			}
		};
		Map<Location,Integer> blockDrips = new ConcurrentHashMap<>();
		BukkitTask[] task = new BukkitTask[2];
		task[0] = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				if (time <= 0) {
					task[0].cancel();
					return;
				}
				Queue<Block> tempPuddles = new ArrayDeque<>();
				for (Player p : world.getPlayers()) {
					if (rand.nextDouble() < puddleRate) {
						Location puddle = p.getLocation().add(rand.nextInt(blockDamageRange)-(blockDamageRange/2),0,rand.nextInt(blockDamageRange)-(blockDamageRange/2));
						puddle.setY(world.getHighestBlockYAt(puddle)+1);
						if (puddle.getBlockY() < 256 && puddle.getBlock().getType() == Material.AIR && !globalPuddles.contains(puddle.getBlock()) && !Utils.isZoneProtected(puddle) && !Utils.isWeatherDisabled(puddle, instance)
								&& puddle.getBlock().getTemperature() > 0.15 && puddle.getBlock().getTemperature() <= 0.95) {
							if (plugin.CProtect)
								Utils.getCoreProtect().logPlacement("Deadly-Disasters", puddle, Material.WATER, puddle.getBlock().getBlockData());
							tempPuddles.add(puddle.getBlock());
							puddles.add(puddle.getBlock());
							globalPuddles.add(puddle.getBlock());
						}
					}
					for (int x=-particleRange; x <= particleRange; x++)
						for (int z=-particleRange; z <= particleRange; z++) {
							if (rand.nextDouble() >= particleMultiplier)
								continue;
							Location temp = p.getLocation().add(x,0,z);
							Location b = world.getHighestBlockAt(temp).getLocation();
							if (b.getBlock().getTemperature() <= 0.15 || b.getBlock().getTemperature() > 0.95 || Utils.isWeatherDisabled(b, instance))
								continue;
							int diff = b.getBlockY() - temp.getBlockY();
							if (diff > particleYRange)
								continue;
							if (diff < 0)
								b.setY(b.getY()+(diff*-1));
							p.spawnParticle(Particle.FALLING_WATER, b.add(0.5,7,0.5), 3, .5, 2, .5, 1);
						}
				}
				plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
					@Override
					public void run() {
						for (Block b : tempPuddles) {
							b.setType(Material.WATER);
							Levelled data = ((Levelled) b.getBlockData());
							data.setLevel(7);
							b.setBlockData(data);
						}
					}
				});
				for (Map.Entry<Location,Integer> entry : blockDrips.entrySet()) {
					if (rand.nextDouble() < leakRate)
						world.spawnParticle(Particle.DRIP_WATER, entry.getKey(), 1, 0.2, 0, 0.2, 1);
					entry.setValue(entry.getValue()-1);
					if (entry.getValue() <= 0)
						blockDrips.remove(entry.getKey());
				}
			}
		}, delay, 1);
		Set<Material> PLANKS = new HashSet<>(Arrays.asList(Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS, Material.DARK_OAK_PLANKS, Material.JUNGLE_PLANKS, Material.ACACIA_PLANKS,
				Material.OAK_SLAB, Material.SPRUCE_SLAB, Material.BIRCH_SLAB, Material.DARK_OAK_SLAB, Material.JUNGLE_SLAB, Material.ACACIA_SLAB,
				Material.OAK_STAIRS, Material.SPRUCE_STAIRS, Material.BIRCH_STAIRS, Material.DARK_OAK_STAIRS, Material.JUNGLE_STAIRS, Material.ACACIA_STAIRS));
		if (plugin.mcVersion >= 1.19)
			PLANKS.addAll(Arrays.asList(Material.MANGROVE_PLANKS, Material.MANGROVE_SLAB, Material.MANGROVE_STAIRS));
		task[1] = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				if (time <= 0) {
					task[1].cancel();
					return;
				}
				Map<Block,Material> changes = new HashMap<>();
				for (Player p : world.getPlayers()) {
					for (int x=-blockDamageRange; x < blockDamageRange; x++)
						for (int z=-blockDamageRange; z < blockDamageRange; z++) {
							Block b = world.getHighestBlockAt(p.getLocation().add(x,0,z));
							if (b.getTemperature() <= 0.15 || b.getTemperature() > 0.95) continue;
							if (PLANKS.contains(b.getType()) || Tag.LEAVES.getValues().contains(b.getType())) {
								if (b.getRelative(BlockFace.DOWN).isPassable())
									blockDrips.put(b.getLocation().clone().add(0.5,0,0.5), 40);
								continue;
							}
							if (rand.nextDouble() >= blockChangeRate || !blockChanges.containsKey(b.getType()) || Utils.isZoneProtected(b.getLocation()) || Utils.passStrengthTest(b.getType()) || Utils.isWeatherDisabled(b.getLocation(), instance))
								continue;
							Material material = blockChanges.get(b.getType());
							if (plugin.CProtect) {
								Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
								Utils.getCoreProtect().logPlacement("Deadly-Disasters", b.getLocation(), material, material.createBlockData());
							}
							addBlockToList(b, new Material[] {b.getType(), material});
							changes.put(b, material);
						}
				}
				plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
					@Override
					public void run() {
						for (Map.Entry<Block, Material> entry : changes.entrySet())
							entry.getKey().setType(entry.getValue());
						blocksDestroyed += changes.size();
					}
				});
			}
		}, delay, 40);
	}
	@Override
	public void clear() {
		time = 0;
		DeathMessages.monsoons.remove(this);
		clearEntities();
		
		double regenRate = type.getDefaultRegenTickRate() * plugin.getConfig().getDouble("monsoon.puddle_dry_rate");
		double[] regenTicks = {0};
		new RepeatingTask(plugin, 200, 1) {
			@Override
			public void run() {
				regenTicks[0] += regenRate;
				while (regenTicks[0] >= 1) {
					regenTicks[0] -= 1;
					if (puddles.isEmpty()) {
						cancel();
						return;
					}
					Block b = puddles.poll();
					globalPuddles.remove(b);
					if (b.getType() == Material.WATER) {
						if (plugin.CProtect)
							Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
						b.setType(Material.AIR);
					}
				}
			}
		};
	}
	public void clearEntities() {
		for (UUID e : drowned)
			if (Bukkit.getEntity(e) != null)
				Bukkit.getEntity(e).remove();
	}
}
