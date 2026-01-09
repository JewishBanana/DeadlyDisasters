package com.github.jewishbanana.deadlydisasters.disasters.weather;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Husk;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
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
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class Sandstorm extends WeatherDisaster implements MobDisaster {
	
	private static final Set<Material> DESERT_SURFACE;
	private static final Set<Material> BADLANDS_SURFACE;
	private static final float MAX_TEMPERATURE;
	private static final float MAX_HUMIDITY;
	static {
		DESERT_SURFACE = Set.of(Material.SAND, Material.SANDSTONE, Material.CACTUS, Material.SANDSTONE_SLAB, Material.SANDSTONE_STAIRS, Material.SANDSTONE_WALL);
		BADLANDS_SURFACE = Set.of(Material.RED_SAND, Material.RED_SANDSTONE, Material.TERRACOTTA, Material.WHITE_TERRACOTTA, Material.LIGHT_GRAY_TERRACOTTA, Material.BROWN_TERRACOTTA, Material.RED_TERRACOTTA, Material.ORANGE_TERRACOTTA, Material.YELLOW_TERRACOTTA);
		MAX_TEMPERATURE = 1.8f;
		MAX_HUMIDITY = 0.2f;
	}
	
	private int minHeight;
	private double damage;
	private float damageRate;
	private float mobSpawnRate;
	private float skullSpawnRate;
	private int minimumYLevel;
	
	private float particleRate;
	private boolean isBadlands;
	private List<PotionEffect> effects;

	public Sandstorm(@NotNull Location location, Player player, int level) {
		super(location, player, level);
		
		this.minHeight = getConfigInt("minimum_height");
	}
	public void init() {
		super.init();
		this.damage = getConfigDouble("damage") * Math.max(scale / 2.0, 1.0);
		this.damageRate = (float) (0.04 * getConfigDouble("damage_multiplier") * (scale / 2.0));
		this.mobSpawnRate = (float) (0.03 * getConfigDouble("mob_spawn_multiplier") * (scale / 2.0));
		this.skullSpawnRate = (float) (0.00002 * getConfigDouble("skull_spawn_multiplier"));
		this.minimumYLevel = getConfigInt("minimum_entity_Y_level");
		
		this.effects = buildPotionEffects("entity_effects");
		
		this.particleRate = (float) (0.15 * particleMultiplier * (scale / 3.0));
		this.soundTickRate = 60;
		this.weatherEffectsRange = (float) disasterRange;
	}
	public boolean canStart() {
		Location loc = getLocation();
		if (loc.getBlockY() < minHeight)
			return false;
		if (!isBlockInClimate(loc.getBlock()))
			return false;
		Set<Block> blocks = Utils.getRandomSurfaceBlocksInArea(loc, 20, 20);
		if (isAreaDesert(blocks, 13) || isAreaBadlands(blocks, 13))
			return super.canStart();
		return false;
	}
	public void start() {
		super.start();
		if (isAreaBadlands(Utils.getRandomSurfaceBlocksInArea(location, 20, 20), 13))
			isBadlands = true;
		location.setY(128);
		final World world = location.getWorld();
		scheduleTask(new BukkitRunnable() {
			@Override
			public void run() {
				for (Player player : playersInMonitorArea) {
					if (EntityUtils.isPlayerImmune(player))
						continue;
					if (random.nextFloat() < mobSpawnRate) {
						Location spawn = SpawnUtils.findMonsterSpawnLocation(player.getLocation(), 2, SpawnUtils.MIN_SPAWN_DISTANCE_FROM_PLAYERS, 30);
						if (spawn != null) {
							Mob mob = null;
							switch (random.nextInt(DependencyUtils.isUltimateContentEnabled() ? 4 : 2)) {
							default:
							case 0:
								mob = world.spawn(spawn, Husk.class);
								break;
							case 1:
								mob = world.spawn(spawn, Skeleton.class, skeleton -> {
									skeleton.getEquipment().setHelmet(new ItemStack(Material.LEATHER_HELMET));
									plugin.getServer().getScheduler().runTaskLater(plugin, () -> skeleton.getEquipment().setItemInMainHand(new ItemStack(Material.AIR)), 1);
								});
								break;
							case 2:
								mob = com.github.jewishbanana.uiframework.entities.UIEntityManager.spawnEntity(spawn, com.github.jewishbanana.ultimatecontent.entities.desertentities.AncientSkeleton.class).getCastedEntity();
								break;
							case 3:
								mob = com.github.jewishbanana.uiframework.entities.UIEntityManager.spawnEntity(spawn, com.github.jewishbanana.ultimatecontent.entities.desertentities.AncientMummy.class).getCastedEntity();
								break;
							}
							addEntityToDisasterList(mob, player);
						}
					}
				}
				for (Entity entity : entitiesInMonitorArea) {
					if (entity instanceof LivingEntity alive) {
						if (EntityUtils.isEntityImmunePlayer(entity))
							continue;
						alive.addPotionEffects(effects);
						if (random.nextFloat() < damageRate)
							EntityUtils.damageEntity(alive, damage * currentStrength, "deaths.sandstorm", DamageCause.WITHER);
					} else if (entity instanceof Item && random.nextInt(10) == 0)
						entity.setVelocity(entity.getVelocity().add(new Vector(random.nextFloat(-1, 1), random.nextFloat(), random.nextFloat(-1, 1)).multiply(scale / 2.0 * currentStrength)));
				}
				
				updateEntityTargets();
				
				time -= 5;
				if (time <= 0)
					stop();
			}
		}.runTaskTimer(plugin, 0, 5));
		
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
						if (isEntityProtected(entity) || !isBlockInClimate(loc.getBlock()))
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
		final BlockData particleData = isBadlands ? Material.RED_SAND.createBlockData() : Material.SAND.createBlockData();
		final DustOptions dust = isBadlands ? new DustOptions(Color.fromRGB(255, 136, 77), 1) : new DustOptions(Color.fromRGB(255, 254, 196), 1);
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
						|| !isBlockInClimate(highest)
						|| highest.getY() - loc.getBlockY() > 10)
					continue;
				final Location particleLoc = new Location(loc.getWorld(), block.getX() + 0.5, (loc.getY() > highest.getY() ? loc.getY() : highest.getY() + 3) + 3, block.getZ() + 0.5);
				player.spawnParticle(Particle.FALLING_DUST, particleLoc, 1, .5, 1, .5, 1, particleData);
				player.spawnParticle(VersionUtils.getRedstoneDust(), particleLoc, 1, .5, 2, .5, 1, dust);
				if (!flag)
					for (int i=0; i < 2; i++)
						player.spawnParticle(VersionUtils.getNormalSmoke(), particleLoc.getX() + random.nextFloat(-.5f, .5f), particleLoc.getY() + random.nextFloat(-5f, 2f), particleLoc.getZ() + random.nextFloat(-.5f, .5f), 0, random.nextFloat(-.5f, .5f), random.nextFloat(-.5f, .5f), random.nextFloat(-.5f, .5f), 1);
				double dist = highest.getLocation().distanceSquared(loc);
				if (closest == null || dist < closestDistance) {
					closest = highest;
					closestDistance = dist;
				}
			}
			if (closest != null) {
				playSound(player, loc.clone().add(0, 3, 0), Sound.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, 0.02 * currentStrength * (flag ? 1.0 : 0.15), .5);
				if (soundTick == 0) {
					Location soundLoc = BlockUtils.getCenterOfBlock(closest);
					playSound(player, soundLoc, Sound.AMBIENT_BASALT_DELTAS_ADDITIONS, SoundCategory.WEATHER, 1 * (flag ? 1.0 : 0.15), .75);
					playSound(player, soundLoc, Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP, SoundCategory.WEATHER, 1 * (flag ? 1.0 : 0.15), .5);
					if (random.nextInt(10) == 0)
						playSound(player, soundLoc, Sound.ENTITY_EVOKER_PREPARE_WOLOLO, SoundCategory.WEATHER, 1 * (flag ? 1.0 : 0.15), .5);
				}
			}
			if (flag)
				for (int i=0; i < 8; i++)
					player.spawnParticle(VersionUtils.getNormalSmoke(), loc.getX() + random.nextFloat(-2f, 2f), loc.getY() + random.nextFloat(-.5f, 2.5f), loc.getZ() + random.nextFloat(-2f, 2f), 0, random.nextFloat(-3f, 3f), random.nextFloat(-3f, 3f), random.nextFloat(-3f, 3f), 1);
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
						|| !isBlockInClimate(highest)
						|| highest.getY() - loc.getBlockY() > 10)
					continue;
				aboveFlag = true;
				if (actualDistance <= distanceSquared) {
					final Location particleLoc = new Location(loc.getWorld(), block.getX() + 0.5, (loc.getY() > highest.getY() ? loc.getY() : highest.getY() + 3) + 5, block.getZ() + 0.5);
					player.spawnParticle(Particle.FALLING_DUST, particleLoc, 2, .5, 3, .5, 1, particleData);
					player.spawnParticle(VersionUtils.getRedstoneDust(), particleLoc, 2, .5, 3, .5, 1, dust);
					for (int i=0; i < 2; i++)
						player.spawnParticle(VersionUtils.getNormalSmoke(), particleLoc.getX() + random.nextFloat(-.5f, .5f), particleLoc.getY() + random.nextFloat(-5f, 2f), particleLoc.getZ() + random.nextFloat(-.5f, .5f), 0, random.nextFloat(-.5f, .5f), random.nextFloat(-.5f, .5f), random.nextFloat(-.5f, .5f), 1);
				} else {
					final Location particleLoc = new Location(loc.getWorld(), block.getX() + 0.5, (loc.getY() > highest.getY() ? loc.getY() : highest.getY() + 3) + 5, block.getZ() + 0.5);
					player.spawnParticle(Particle.FALLING_DUST, particleLoc, 1, .5, 3, .5, 1, particleData);
					player.spawnParticle(VersionUtils.getRedstoneDust(), particleLoc, 1, .5, 3, .5, 1, dust);
				}
				if (!soundFlag && loc.distanceSquared(BlockUtils.getCenterOfBlock(highest)) <= 25
						|| (loc.getY() > highest.getY() && loc.distanceSquared(new Location(loc.getWorld(), highest.getX() + 0.5, loc.getY(), highest.getZ() + 0.5)) <= 25))
					soundFlag = true;
			}
			if (aboveFlag && soundTick == 0) {
				Location fixed = new Location(loc.getWorld(), loc.getX(), location.getY(), loc.getZ());
				if (fixed.distanceSquared(location) > trueSmoothingRange)
					playSound(player, loc.clone().add(Utils.getVectorTowards(loc, location).multiply(8.0).setY(7)), Sound.WEATHER_RAIN, SoundCategory.WEATHER, ((0.02 / smoothingRangeExcess) * ((smoothingRangeExcess - (fixed.distance(location) - disasterRange - smoothingRange)))) * smoothingIntensity * currentStrength, .5);
				else
					playSound(player, loc.clone().add(0, 7, 0), soundFlag ? Sound.WEATHER_RAIN : Sound.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, 0.02 * currentStrength, .5);
			}
		}, loc -> isBlockInClimate(loc.getBlock()));
		
		if (skullSpawnRate > 0)
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
							if (random.nextFloat() > skullSpawnRate)
								return;
							Block block = BlockUtils.getHighestExposedBlock(location.getWorld().getHighestBlockAt(chunk.getBlock(random.nextInt(16), 0, random.nextInt(16)).getLocation()), 10);
							if (block == null || block.isPassable() 
									|| !isBlockInClimate(block)
									|| new Location(block.getWorld(), block.getX() + 0.5, location.getY(), block.getZ() + 0.5).distanceSquared(location) > distanceSquared)
								return;
							switch (block.getType()) {
							case SAND:
							case SANDSTONE:
							case RED_SAND:
							case RED_SANDSTONE:
							case TERRACOTTA:
							case WHITE_TERRACOTTA:
							case LIGHT_GRAY_TERRACOTTA:
							case YELLOW_TERRACOTTA:
							case BROWN_TERRACOTTA:
							case RED_TERRACOTTA:
							case ORANGE_TERRACOTTA:
								break;
							default:
								return;
							}
							Block above = block.getRelative(BlockFace.UP);
							if (above == null || above.getWorld().getPlayers().stream().anyMatch(p -> Utils.isLocationsWithinDistance(p.getLocation(), BlockUtils.getCenterOfBlock(above), 576)))
								return;
							plugin.getServer().getScheduler().runTask(plugin, () -> {
								if (placeBlock(above, Material.SKELETON_SKULL)) {
									BlockFace facing = null;
									switch (ThreadLocalRandom.current().nextInt(11)) {
									case 0 -> facing = BlockFace.NORTH_NORTH_EAST;
									case 1 -> facing = BlockFace.NORTH_EAST;
									case 2 -> facing = BlockFace.EAST_NORTH_EAST;
									case 3 -> facing = BlockFace.EAST_SOUTH_EAST;
									case 4 -> facing = BlockFace.SOUTH_EAST;
									case 5 -> facing = BlockFace.SOUTH_SOUTH_EAST;
									case 6 -> facing = BlockFace.SOUTH_SOUTH_WEST;
									case 7 -> facing = BlockFace.SOUTH_WEST;
									case 8 -> facing = BlockFace.WEST_SOUTH_WEST;
									case 9 -> facing = BlockFace.NORTH_WEST;
									case 10 -> facing = BlockFace.NORTH_NORTH_WEST;
									}
									Skull skull = (Skull) above.getState();
									Rotatable data = (Rotatable) skull.getBlockData();
									data.setRotation(facing);
									above.setBlockData(data);
								}
							});
						});
					}
				}.runTaskTimerAsynchronously(plugin, 0, 5));
			});
	}
	public void clean() {
		super.clean();
		weatherPlayers.forEach(uuid -> {
			Player player = Bukkit.getPlayer(uuid);
			if (player == null || !player.isOnline())
				return;
			player.stopSound(Sound.AMBIENT_BASALT_DELTAS_ADDITIONS);
			player.stopSound(Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP);
		});
	}
	public boolean isBlockInClimate(Block block) {
		return block.getWorld().getTemperature(block.getX(), block.getY(), block.getZ()) >= MAX_TEMPERATURE && block.getWorld().getHumidity(block.getX(), block.getY(), block.getZ()) <= MAX_HUMIDITY;
	}
	public void addPlayerToWeather(Player player) {
		super.addPlayerToWeather(player);
		Location soundLoc = player.getLocation().add(0, 5, 0);
		playSound(player, soundLoc, Sound.AMBIENT_BASALT_DELTAS_ADDITIONS, SoundCategory.WEATHER, 1, .75);
		playSound(player, soundLoc, Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP, SoundCategory.WEATHER, 1, .5);
	}
	public void removePlayerFromWeather(Player player) {
		super.removePlayerFromWeather(player);
		player.stopSound(Sound.AMBIENT_BASALT_DELTAS_ADDITIONS);
		player.stopSound(Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP);
	}
	protected String getConfigPath() {
		return "disasters.weather.sandstorm";
	}
	public double getRegenTickRate() {
		return 0.01;
	}
	public Set<Environment> getBannedEnvironments() {
		return EnumSet.of(Environment.NETHER, Environment.THE_END);
	}
	
	public static boolean isAreaDesert(Set<Block> blocks, int threshold) {
		int count = 0;
		for (Block block : blocks) {
			if (block.isPassable()) {
				block = block.getRelative(BlockFace.DOWN);
				if (block == null)
					continue;
			}
			if (DESERT_SURFACE.contains(block.getType()))
				count++;
		}
		return count >= threshold;
	}
	public static boolean isAreaBadlands(Set<Block> blocks, int threshold) {
		int count = 0;
		for (Block block : blocks) {
			if (block.isPassable()) {
				block = block.getRelative(BlockFace.DOWN);
				if (block == null)
					continue;
			}
			if (BADLANDS_SURFACE.contains(block.getType()))
				count++;
		}
		return count >= threshold;
	}
}
