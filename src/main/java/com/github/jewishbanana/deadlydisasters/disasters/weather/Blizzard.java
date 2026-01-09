package com.github.jewishbanana.deadlydisasters.disasters.weather;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Snow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Stray;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.github.jewishbanana.deadlydisasters.disasters.MobDisaster;
import com.github.jewishbanana.deadlydisasters.disasters.WeatherDisaster;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.SpawnUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class Blizzard extends WeatherDisaster implements MobDisaster {
	
	private static final float MAX_TEMPERATURE;
	public static final NamespacedKey frozenEntityKey;
	public static final Set<LivingEntity> frozenEntities;
	static {
		MAX_TEMPERATURE = 0.15f;
		frozenEntityKey = new NamespacedKey(plugin, "dd-fek");
		frozenEntities = ConcurrentHashMap.newKeySet();
		
		new BukkitRunnable() {
			@Override
			public void run() {
				final Iterator<LivingEntity> iterator = frozenEntities.iterator();
				while (iterator.hasNext()) {
					LivingEntity entity = iterator.next();
					if (entity == null || !entity.isValid() || !entity.getLocation().getChunk().isLoaded())
						continue;
					Block block = entity.getLocation().add(0, Math.floor(entity.getHeight()), 0).getBlock();
					if (block == null || block.getType() != Material.ICE) {
						byte value = entity.getPersistentDataContainer().getOrDefault(frozenEntityKey, PersistentDataType.BYTE, (byte) 0b1001);
						entity.setAI((value & 1) == 1);
						entity.setInvulnerable(((value >> 1) & 1) == 1);
						entity.setSilent(((value >> 2) & 1) == 1);
						entity.setRemoveWhenFarAway(((value >> 3) & 1) == 1);
						entity.getPersistentDataContainer().remove(frozenEntityKey);
						iterator.remove();
					}
				}
			}
		}.runTaskTimerAsynchronously(plugin, 0, 20);
	}
	
	private int minHeight;
	private double damage;
	private float damageRate;
	private float mobSpawnRate;
	private boolean freezeEntities;
	private boolean despawnFrozenEntities;
	private boolean leatherArmorReduction;
	private boolean freezeTicks;
	private float raiseSnowLevelMultiplier;
	private int minimumYLevel;
	
	private float particleRate;
	private float soundVolume;
	private List<PotionEffect> effects;

	public Blizzard(@NotNull Location location, Player player, int level) {
		super(location, player, level);
		
		this.minHeight = getConfigInt("minimum_height");
	}
	public void init() {
		super.init();
		this.damage = getConfigDouble("damage") * (scale / 2.0);
		this.damageRate = (float) (0.25 * getConfigDouble("damage_multiplier") * (scale / 2.0));
		this.mobSpawnRate = (float) (0.018 * getConfigDouble("mob_spawn_multiplier") * (scale / 2.0));
		this.freezeEntities = getConfigBoolean("freeze_entities");
		this.despawnFrozenEntities = getConfigBoolean("despawn_frozen_entities");
		this.leatherArmorReduction = getConfigBoolean("leather_armor_reduction");
		this.freezeTicks = getConfigBoolean("give_entities_freeze_ticks");
		this.raiseSnowLevelMultiplier = (float) (0.0325 * getConfigDouble("raise_snow_layer_multiplier") * (scale / 2.0));
		this.minimumYLevel = getConfigInt("minimum_entity_Y_level");
		
		this.effects = buildPotionEffects("entity_effects");
		
		this.particleRate = (float) (0.1 * particleMultiplier * scale);
		this.soundVolume = (float) (0.3 * (scale / 2.0));
		this.soundTickRate = 60;
	}
	public boolean canStart() {
		Location loc = getLocation();
		if (loc.getBlockY() < minHeight)
			return false;
		if (!isBlockInBiome(loc.getBlock()))
			return false;
		return super.canStart();
	}
	public void start() {
		super.start();
		location.setY(128);
		final World world = location.getWorld();
		final Set<LivingEntity> freezingEntities = new HashSet<>();
		scheduleTask(new BukkitRunnable() {
			private final boolean isUCEnabled = DependencyUtils.isUltimateContentEnabled();
			
			@Override
			public void run() {
				for (Player player : playersInMonitorArea) {
					if (EntityUtils.isPlayerImmune(player))
						continue;
					if (random.nextFloat() < mobSpawnRate) {
						Location spawn = SpawnUtils.findMonsterSpawnLocation(player.getLocation(), 2, SpawnUtils.MIN_SPAWN_DISTANCE_FROM_PLAYERS, 30);
						if (spawn != null) {
							Mob mob = null;
							switch (random.nextInt(DependencyUtils.isUltimateContentEnabled() ? 2 : 1)) {
							default:
							case 0:
								mob = world.spawn(spawn, Stray.class, stray -> {
									stray.getEquipment().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
									stray.getEquipment().setHelmetDropChance(0);
									plugin.getServer().getScheduler().runTaskLater(plugin, () -> stray.getEquipment().setItemInMainHand(new ItemStack(Material.AIR)), 1);
								});
								break;
							case 1:
								if (random.nextInt(2) == 0)
									mob = com.github.jewishbanana.uiframework.entities.UIEntityManager.spawnEntity(spawn, com.github.jewishbanana.ultimatecontent.entities.snowentities.Yeti.class).getCastedEntity();
								else
									mob = world.spawn(spawn, Stray.class, stray -> {
										stray.getEquipment().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
										stray.getEquipment().setHelmetDropChance(0);
										plugin.getServer().getScheduler().runTaskLater(plugin, () -> stray.getEquipment().setItemInMainHand(new ItemStack(Material.AIR)), 1);
									});
								break;
							}
							addEntityToDisasterList(mob, player);
						}
					}
				}
				freezingEntities.clear();
				for (Entity entity : entitiesInMonitorArea) {
					if (entity instanceof LivingEntity alive) {
						if (EntityUtils.isEntityImmunePlayer(entity) || 
								(isUCEnabled && entity instanceof IronGolem && com.github.jewishbanana.uiframework.entities.UIEntityManager.getEntity(entity) instanceof com.github.jewishbanana.ultimatecontent.entities.snowentities.Yeti))
							continue;
						alive.addPotionEffects(effects);
						freezingEntities.add(alive);
						if (random.nextFloat() < damageRate) {
							double totalDamage = damage * currentStrength;
							if (leatherArmorReduction) {
								int leatherPieces = 0;
								for (ItemStack item : alive.getEquipment().getArmorContents())
									if (item != null)
										switch (item.getType()) {
										case LEATHER_HELMET:
										case LEATHER_CHESTPLATE:
										case LEATHER_LEGGINGS:
										case LEATHER_BOOTS:
											leatherPieces++;
											break;
										default:
											break;
										}
								if (leatherPieces != 4)
									damageEntity(alive, totalDamage / 4.0 * (4.0 - leatherPieces));
							} else
								damageEntity(alive, damage * currentStrength);
						}
					} else if (entity instanceof Item && random.nextInt(10) == 0)
						entity.setVelocity(entity.getVelocity().add(new Vector(random.nextFloat(-1, 1), random.nextFloat(), random.nextFloat(-1, 1)).multiply(scale / 2.0 * currentStrength)));
				}
				
				updateEntityTargets();
				
				time -= 5;
				if (time <= 0)
					stop();
			}
		}.runTaskTimer(plugin, 0, 5));
		
		if (freezeTicks)
			scheduleTask(new BukkitRunnable() {
				private final int increment = (int) Math.max(level * scale, 3);
				
				@Override
				public void run() {
					freezingEntities.forEach(entity -> {
						if (entity.isValid())
							entity.setFreezeTicks(Math.min(entity.getFreezeTicks() + increment, entity.getMaxFreezeTicks()));
					});
				}
			}.runTaskTimer(plugin, 1, 1));

		final double distanceSquared = disasterRange * disasterRange;
		final Set<UUID> playersInStorm = ConcurrentHashMap.newKeySet();
		final List<UUID> playersIteratedOver = new ArrayList<>();
		final Map<UUID, Integer> timeInStorm = new HashMap<>();
		createAsyncEntityMonitor(Entity::isValid, 
				(found, entities, players) -> {
					playersInStorm.clear();
					playersInStorm.addAll(playersIteratedOver);
					playersIteratedOver.clear();
					found.forEach((entity, loc) -> {
						if (loc.getY() < minimumYLevel || !Utils.isLocationsWithinDistance(new Location(loc.getWorld(), loc.getX(), location.getY(), loc.getZ()), location, distanceSquared))
							return;
						if (isEntityProtected(entity) || !isBlockInBiome(loc.getBlock()))
							return;
						if (entity instanceof Player ? Utils.isLocationExposedToOutdoors(loc) : Utils.isLocationExposedToOutdoorsOptimized(loc, 8f, 6)) {
							int time = timeInStorm.compute(entity.getUniqueId(), (key, oldValue) -> Math.min((oldValue != null ? oldValue : 0) + 1, 20));
							if (time > 10) {
								entities.add(entity);
								if (entity instanceof Player p) {
									players.add(p);
									playersIteratedOver.add(p.getUniqueId());
								}
								return;
							}
						} else
							timeInStorm.computeIfPresent(entity.getUniqueId(), (key, oldValue) -> {
								int newValue = oldValue - 5;
								return newValue > 0 ? newValue : null;
							});
						if (entity instanceof Player p && loc.getY() > minHeight - 5)
							players.add(p);
					});
				});

		final double trueSmoothingRange = (disasterRange + smoothingRange) * (disasterRange + smoothingRange);
		final double internalDistanceSquared = (particleRenderDistance - 1.5) * (particleRenderDistance - 1.5);
		createParticleAsyncTask(player -> {
			final ThreadLocalRandom random = ThreadLocalRandom.current();
			final Location loc = player.getLocation();
			Block closest = null;
			double closestDistance = 0;
			final boolean flag = playersInStorm.contains(player.getUniqueId());
			for (Block block : BlockUtils.getBlocksInCircleRadius(loc, particleRenderDistance)) {
				if (new Location(block.getWorld(), block.getX() + 0.5, location.getY(), block.getZ() + 0.5).distanceSquared(location) > distanceSquared 
						|| random.nextFloat() > particleRate * currentStrength)
					continue;
				Block highest = new Location(block.getWorld(), block.getX(), block.getWorld().getHighestBlockYAt(block.getX(), block.getZ()), block.getZ()).getBlock();
				if (highest == null 
						|| !isBlockInBiome(highest) 
						|| highest.getY() - loc.getBlockY() > 10)
					continue;
				final Location particleLoc = new Location(loc.getWorld(), block.getX() + 0.5, (loc.getY() > highest.getY() ? loc.getY() : highest.getY() + 3) + 3, block.getZ() + 0.5);
				if (!flag)
					player.spawnParticle(Particle.CLOUD, particleLoc, 2, .5, 2.5, .5, .05);
				else if (new Location(particleLoc.getWorld(), particleLoc.getX(), loc.getY(), particleLoc.getZ()).distanceSquared(loc) > internalDistanceSquared)
					player.spawnParticle(Particle.CLOUD, particleLoc, 5, .5, 2.5, .5, .05);
				else
					for (int i=0; i < 2; i++)
						player.spawnParticle(Particle.CLOUD, particleLoc.clone().add(random.nextFloat()-.5, random.nextFloat(3f) + 5, random.nextFloat()-.5), 0, random.nextFloat(-.5f, .5f), random.nextFloat(-1.25f, -.5f), random.nextFloat(-.5f, .5f), 1);
				double dist = highest.getLocation().distanceSquared(loc);
				if (closest == null || dist < closestDistance) {
					closest = highest;
					closestDistance = dist;
				}
			}
			if (closest != null) {
				playSound(player, loc.clone().add(0, 5, 0), Sound.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, soundVolume * currentStrength * (flag ? 1.0 : 0.15), .5);
				if (soundTick == 0) {
//					Location soundLoc = BlockUtils.getCenterOfBlock(closest);
//					playSound(player, soundLoc, Sound.AMBIENT_BASALT_DELTAS_ADDITIONS, SoundCategory.WEATHER, 1, .75);
//					playSound(player, soundLoc, Sound.AMBIENT_BASALT_DELTAS_ADDITIONS, SoundCategory.WEATHER, 1, .75);
//					if (random.nextInt(10) == 0)
//						playSound(player, soundLoc, Sound.ENTITY_EVOKER_PREPARE_WOLOLO, SoundCategory.WEATHER, 1, .5);
				}
			}
		}, pair -> {
			final ThreadLocalRandom random = ThreadLocalRandom.current();
			final Player player = pair.getFirst();
			final double intensity = pair.getSecond();
			final Location loc = player.getLocation();
			boolean soundFlag = false;
			boolean aboveFlag = false;
			for (Block block : BlockUtils.getBlocksInCircleRadius(loc, particleRenderDistance)) {
				final double actualDistance = new Location(block.getWorld(), block.getX() + 0.5, location.getY(), block.getZ() + 0.5).distanceSquared(location);
				if (actualDistance > trueSmoothingRange || random.nextFloat() > intensity * 2.0 * particleRate * currentStrength)
					continue;
				Block highest = new Location(block.getWorld(), block.getX(), block.getWorld().getHighestBlockYAt(block.getX(), block.getZ()), block.getZ()).getBlock();
				if (highest == null 
						|| !isBlockInBiome(highest)
						|| highest.getY() - loc.getBlockY() > 10)
					continue;
				aboveFlag = true;
				final Location particleLoc = new Location(loc.getWorld(), block.getX() + 0.5, (loc.getY() > highest.getY() ? loc.getY() : highest.getY() + 3) + 5, block.getZ() + 0.5);
				if (actualDistance <= distanceSquared) {
					player.spawnParticle(Particle.CLOUD, particleLoc, 1, .5, .7, .5, .05);
				} else {
					player.spawnParticle(Particle.CLOUD, particleLoc.clone().add(random.nextFloat()-.5, random.nextFloat(5f) + 3, random.nextFloat()-.5), 0, random.nextFloat(-.2f, .2f), random.nextFloat(-.5f, -.2f), random.nextFloat(-.2f, .2f), .05);
				}
				if (!soundFlag && loc.distanceSquared(BlockUtils.getCenterOfBlock(highest)) <= 25
						|| (loc.getY() > highest.getY() && loc.distanceSquared(new Location(loc.getWorld(), highest.getX() + 0.5, loc.getY(), highest.getZ() + 0.5)) <= 25))
					soundFlag = true;
			}
			if (aboveFlag && soundTick == 0) {
				Location fixed = new Location(loc.getWorld(), loc.getX(), location.getY(), loc.getZ());
				if (fixed.distanceSquared(location) > trueSmoothingRange)
					playSound(player, loc.clone().add(Utils.getVectorTowards(loc, location).multiply(8.0).setY(7)), Sound.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, ((soundVolume / smoothingRangeExcess) * ((smoothingRangeExcess - (fixed.distance(location) - disasterRange - smoothingRange)))) * smoothingIntensity * currentStrength, .5);
				else
					playSound(player, loc.clone().add(0, 7, 0), Sound.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, soundVolume * currentStrength, .5);
			}
		});
		
		if (raiseSnowLevelMultiplier > 0)
			getChunksInvolvedSafelyAndThen(() -> {
				scheduleTask(new BukkitRunnable() {
					@Override
					public void run() {
						if (currentStrength < 1.0)
							return;
						final ThreadLocalRandom random = ThreadLocalRandom.current();
						involvedChunks.forEach(chunk -> {
							if (!chunk.isLoaded())
								return;
							if (random.nextFloat() > raiseSnowLevelMultiplier)
								return;
							Block block = BlockUtils.getHighestExposedBlock(location.getWorld().getHighestBlockAt(chunk.getBlock(random.nextInt(16), 0, random.nextInt(16)).getLocation()), 10);
							if (block == null 
									|| !isBlockInBiome(block)
									|| new Location(block.getWorld(), block.getX() + 0.5, location.getY(), block.getZ() + 0.5).distanceSquared(location) > distanceSquared)
								return;
							if (block.getType() != Material.SNOW) {
								if (!block.getType().isSolid())
									return;
								block = block.getRelative(BlockFace.UP);
								if (block == null)
									return;
							}
							final Block toPlace = block;
							plugin.getServer().getScheduler().runTask(plugin, () -> {
								if (toPlace.getType() == Material.SNOW) {
									Snow data = (Snow) toPlace.getBlockData();
									if (data.getLayers() < data.getMaximumLayers() - 2) {
										data.setLayers(data.getLayers() + 1);
										placeBlock(toPlace, data, false, true);
									}
								} else if (toPlace.getType() == Material.AIR)
									placeBlock(toPlace, Material.SNOW);
							});
						});
					}
				}.runTaskTimerAsynchronously(plugin, 0, 5));
			});
	}
	private boolean isBlockInBiome(Block block) {
		return DependencyUtils.isRealisticSeasonsEnabled() ? DependencyUtils.isTemperatureUnderOrAtBlizzardThreshold(block.getLocation()) : block.getWorld().getTemperature(block.getX(), block.getY(), block.getZ()) <= MAX_TEMPERATURE;
	}
	private void damageEntity(LivingEntity entity, double damage) {
		if (entity.isInvulnerable())
			return;
		if (freezeEntities && !entity.isDead() && entity.getHealth() <= damage) {
			if (entity instanceof Player p) {
				Skeleton skeleton = entity.getWorld().spawn(entity.getLocation(), Skeleton.class, temp -> {
					temp.setCustomName(p.getDisplayName());
					temp.setCustomNameVisible(false);
					plugin.getServer().getScheduler().runTaskLater(plugin, () -> temp.getEquipment().setItemInMainHand(new ItemStack(Material.AIR)), 1);
				});
				EntityUtils.pureDamageEntity(entity, damage, "deaths.blizzard", DamageCause.FREEZE);
				entity = skeleton;
			}
			byte data = 0;
			data |= entity.hasAI() ? 1 : 0;
			entity.setAI(false);
			data |= entity.isInvulnerable() ? 1 << 1 : 0;
			entity.setInvulnerable(true);
			data |= entity.isSilent() ? 1 << 2 : 0;
			entity.setSilent(true);
			data |= entity.getRemoveWhenFarAway() ? 1 << 3 : 0;
			entity.getPersistentDataContainer().set(frozenEntityKey, PersistentDataType.BYTE, data);
			if (!despawnFrozenEntities)
				entity.setRemoveWhenFarAway(false);
			frozenEntities.add(entity);
			Location loc = entity.getLocation();
			entity.teleport(new Location(loc.getWorld(), loc.getBlockX() + 0.5, loc.getBlockY(), loc.getBlockZ() + 0.5, loc.getYaw(), loc.getPitch()));
			Block block = loc.getBlock();
			for (int i=0; i < (int) Math.ceil(entity.getHeight()); i++) {
				if (block == null)
					break;
				placeBlock(block, Material.ICE);
				block = block.getRelative(BlockFace.UP);
			}
			return;
		}
		EntityUtils.pureDamageEntity(entity, damage, "deaths.blizzard", DamageCause.FREEZE);
	}
	protected String getConfigPath() {
		return "disasters.weather.blizzard";
	}
	public Set<Environment> getBannedEnvironments() {
		return EnumSet.of(Environment.NETHER, Environment.THE_END);
	}
	
}
