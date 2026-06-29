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

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.github.jewishbanana.deadlydisasters.DeadlyDisasters;
import com.github.jewishbanana.deadlydisasters.disasters.MobDisaster;
import com.github.jewishbanana.deadlydisasters.disasters.WeatherDisaster;
import com.github.jewishbanana.deadlydisasters.listeners.EntitiesListener;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.SpawnUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class SolarStorm extends WeatherDisaster implements MobDisaster, Listener {
	
	private static final float MIN_TEMPERATURE;
	private static final float MAX_HUMIDITY;
	static {
		MIN_TEMPERATURE = 1.8f;
		MAX_HUMIDITY = 0.2f;
	}
	
	private int minHeight;
	private boolean setDawn;
	private float entityIgniteChance;
	private int fireSpawnRate;
	private float fireballExplosionSize;
	private float mobSpawnRate;
	private float blockChangeRate;
	
	private float blockIgniteRate;
	private float particleRate;
	private List<PotionEffect> effects;
	private Map<Material, Material[]> blockChanges;
	private BlockData[] fallingTypes;
	private final List<UUID> fallingBlocks = new ArrayList<>();
	private final Set<UUID> fireballs = new HashSet<>();
	private long currentTime;
	private Vector direction;
	
	public SolarStorm(@NotNull Location location, Player player, int level) {
		super(location, player, level);
		
		this.minHeight = getConfigInt("minimum_height");
	}
	public void init() {
		super.init();
		this.setDawn = getConfigBoolean("set_to_dawn");
		this.entityIgniteChance = (float) (getConfigDouble("entity_ignite_chance") / 100.0 * scale);
		this.blockIgniteRate = (float) (0.005 * getConfigDouble("block_ignite_multiplier") * scale);
		this.fireSpawnRate = (int) (2.5 * getConfigDouble("fire_spawn_multiplier") * scale);
		this.fireballExplosionSize = (float) getConfigDouble("fireball_explosion_size");
		this.mobSpawnRate = (float) (0.02 * getConfigDouble("mob_spawn_multiplier") * (scale / 1.5));
		this.blockChangeRate = (float) (0.0005 * getConfigDouble("block_damage_rate") * scale);
		
		this.effects = buildPotionEffects("entity_effects");
		this.blockChanges = buildBlockChanges("block_changes");
		
		this.fallingTypes = new BlockData[] { Material.FIRE.createBlockData() };
		this.particleRate = (float) (0.1 * particleMultiplier * (scale / 3.0));
		this.soundTickRate = 30;
	}
	public boolean canStart() {
		final Block block = getLocation().getBlock();
		if (block.getY() < minHeight)
			return false;
		// Dry only climate
		if (!isBlockInClimate(block))
			return false;
		return super.canStart();
	}
	public void start() {
		super.start();
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		location.setY(128);
		this.currentTime = world.getTime();
		direction = Utils.getRandomizedVector(1f, 0, 1f);
		scheduleTask(new BukkitRunnable() {
			private final Vector directionInverse = direction.clone().multiply(-40);
			private final Vector fireVelocity = direction.clone().multiply(0.6).setY(-2);
			private final Vector fireballVelocity = direction.clone().multiply(0.8).setY(-3);
			
			@Override
			public void run() {
				for (Player player : playersInMonitorArea) {
					if (!player.isValid() || EntityUtils.isPlayerImmune(player))
						continue;
					if (DependencyUtils.isUltimateContentEnabled() && random.nextFloat() < mobSpawnRate) {
						for (int i=0; i < 5; i++) {
							Location spawn = Utils.findRandomSpotInRadius(player.getLocation(), SpawnUtils.MIN_SPAWN_DISTANCE_FROM_PLAYERS, 32, 1, 3, () -> Utils.getRandomizedVector(1f, 0.25f, 1f));
							if (spawn == null || spawn.getWorld().getNearbyEntities(spawn, SpawnUtils.MIN_SPAWN_DISTANCE_FROM_PLAYERS, SpawnUtils.MIN_SPAWN_DISTANCE_FROM_PLAYERS, SpawnUtils.MIN_SPAWN_DISTANCE_FROM_PLAYERS, e -> e instanceof Player).size() != 0)
								continue;
							spawn.add(0, 20, 0);
							if (Utils.isAreaClear(spawn, 4f)) {
								Mob mob = com.github.jewishbanana.uiframework.entities.UIEntityManager.spawnEntity(spawn, com.github.jewishbanana.ultimatecontent.entities.netherentities.FirePhantom.class).getCastedEntity();
								addEntityToDisasterList(mob, player);
								break;
							}
						}
					}
				}
				for (Entity entity : entitiesInMonitorArea) {
					if (!entity.isValid())
						continue;
					if (entity instanceof LivingEntity alive) {
						if (EntityUtils.isEntityImmunePlayer(entity))
							continue;
						alive.addPotionEffects(effects);
					}
					final Location loc = entity.getLocation();
					if (entity.getFireTicks() > 0)
						entity.setFireTicks(entity.getFireTicks() + 5);
					if (loc.getBlock().getLightFromBlocks() >= 11 && random.nextFloat() < entityIgniteChance)
						entity.setFireTicks(100);
				}
				Iterator<UUID> it = fallingBlocks.iterator();
				while (it.hasNext()) {
					Entity temp = Bukkit.getEntity(it.next());
					if (temp == null || !temp.isValid()) {
						it.remove();
						continue;
					}
					temp.setVelocity(fireVelocity);
				}
				final List<Player> threadSafeCopy = new ArrayList<>(playersInMonitorArea);
				new BukkitRunnable() {
					private final int fireballSpawnChance = (int) (45 / scale);
					
					@Override
					public void run() {
						final Set<Chunk> chunks = new HashSet<>();
						final List<Location> fireSpawns = new ArrayList<>();
						final List<Location> fireballSpawns = new ArrayList<>();
						for (Entity temp : threadSafeCopy) {
							final Location loc = temp.getLocation();
							if (!loc.getWorld().equals(world))
								continue;
							for (int i=0; i < fireSpawnRate; i++) {
								Location spawn = loc.clone().add(directionInverse).add(Utils.getRandomizedVector().setY(0).normalize().multiply(random.nextFloat(60f)));
								spawn.setY(Math.max(loc.getY() + 70, 210));
								if (chunks.contains(world.getChunkAt(spawn)))
									continue;
								if (random.nextInt(fireballSpawnChance) != 0)
									fireSpawns.add(spawn);
								else
									fireballSpawns.add(spawn);
							}
							chunks.addAll(Utils.getChunksInRadius(loc.clone().add(directionInverse), 60f));
						}
						new BukkitRunnable() {
							@Override
							public void run() {
								fireSpawns.forEach(spawn -> {
									FallingBlock fb = createFallingBlock(spawn, fallingTypes[random.nextInt(fallingTypes.length)]);
									fb.setGravity(false);
									fb.setVelocity(fireVelocity);
									fallingBlocks.add(fb.getUniqueId());
								});
								fireballSpawns.forEach(spawn -> {
									world.spawn(spawn, Fireball.class, temp -> {
										temp.setVelocity(fireballVelocity);
										temp.setYield(fireballExplosionSize);
										fireballs.add(temp.getUniqueId());
										EntitiesListener.attachRemoveKey(temp);
									});
								});
							}
						}.runTask(plugin);
					}
				}.runTaskAsynchronously(plugin);
				
				updateEntityTargets();
				
				if (currentTime < 12000)
					currentTime += 500;
				else if (currentTime > 14000)
					currentTime -= 500;
			}
		}.runTaskTimer(plugin, 0, 10));
		
		final Set<UUID> playersInStorm = ConcurrentHashMap.newKeySet();
		final List<UUID> playersIteratedOver = new ArrayList<>();
		final Map<UUID, Integer> timeInStorm = new HashMap<>();
		createAsyncEntityMonitor(Entity::isValid, 
				(found, entities, players) -> {
					playersInStorm.clear();
					playersInStorm.addAll(playersIteratedOver);
					playersIteratedOver.clear();
					found.forEach((entity, loc) -> {
						if (!isWithinStorm(loc))
							return;
						if (isEntityProtected(entity) || !isBlockInClimate(loc.getBlock()))
							return;
						if (DependencyUtils.isUltimateContentEnabled() && entity instanceof Phantom && com.github.jewishbanana.uiframework.entities.UIEntityManager.getEntity(entity) instanceof com.github.jewishbanana.ultimatecontent.entities.netherentities.FirePhantom)
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
		final DustOptions dust = new DustOptions(Color.fromRGB(140, 97, 62), 0.5f);
		createParticleAsyncTask(player -> {
			final ThreadLocalRandom random = ThreadLocalRandom.current();
			final Location loc = player.getLocation();
			Block closest = null;
			double closestDistance = 0;
			final boolean flag = playersInStorm.contains(player.getUniqueId());
			for (Block block : BlockUtils.getBlocksInCircleRadius(loc, particleRenderDistance)) {
				if (random.nextFloat() >= particleRate * currentStrength || !isWithinStorm(block))
					continue;
				final Block highest = world.getHighestBlockAt(block.getX(), block.getZ());
				if (highest == null || highest.getY() - loc.getBlockY() > 10 || !isBlockInClimate(highest))
					continue;
				final double centerX = block.getX() + 0.5;
				final double centerZ = block.getZ() + 0.5;
				final double particleY = (loc.getY() > highest.getY() ? loc.getY() : highest.getY() + 3) + 3;
				player.spawnParticle(VersionUtils.getRedstoneDust(), centerX, particleY, centerZ, 1, .5, 3, .5, 1, dust);
				if (!flag)
					for (int i=0; i < 2; i++)
						player.spawnParticle(VersionUtils.getNormalSmoke(),centerX + random.nextFloat(-.5f, .5f), particleY + random.nextFloat(-5f, 2f), centerZ + random.nextFloat(-.5f, .5f), 0, random.nextFloat(-.2f, .2f), random.nextFloat(-.2f, .2f), random.nextFloat(-.2f, .2f), 0.01);
				final double dx = highest.getX() - loc.getX();
				final double dz = highest.getZ() - loc.getZ();
				final double dist = dx * dx + dz * dz;
				if (closest == null || dist < closestDistance) {
					closest = highest;
					closestDistance = dist;
				}
			}
			if (closest != null) {
				playSound(player, loc.add(0, 3, 0), Sound.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, (float) (0.01 * currentStrength * (flag ? 1.0 : 0.15)), .5f);
				if (soundTick == 0) {
					final Location soundLoc = BlockUtils.getCenterOfBlock(closest);
					playSound(player, soundLoc, Sound.AMBIENT_BASALT_DELTAS_ADDITIONS, SoundCategory.WEATHER, (float) (0.3 * (flag ? 1.0 : 0.15)), .75f);
					playSound(player, soundLoc, Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP, SoundCategory.WEATHER, (float) (0.3 * (flag ? 1.0 : 0.15)), .5f);
				}
			}
			if (setDawn)
				player.setPlayerTime(currentTime, false);
		}, pair -> {
			final ThreadLocalRandom random = ThreadLocalRandom.current();
			final Player player = pair.getFirst();
			final double intensity = pair.getSecond();
			final Location loc = player.getLocation();
			boolean soundFlag = false;
			boolean aboveFlag = false;
			for (Block block : BlockUtils.getBlocksInCircleRadius(loc, particleRenderDistance)) {
				if (random.nextFloat() >= intensity * 2.0 * particleRate * currentStrength)
					continue;
				final double dx = block.getX() + 0.5 - location.getX();
				final double dz = block.getZ() + 0.5 - location.getZ();
				final double distanceSquared = dx * dx + dz * dz;
				if (distanceSquared > trueSmoothingRange)
					continue;
				final Block highest = world.getHighestBlockAt(block.getX(), block.getZ());
				if (highest == null || highest.getY() - loc.getBlockY() > 10 || !isBlockInClimate(highest))
					continue;
				aboveFlag = true;
				final double centerX = block.getX() + 0.5;
				final double centerZ = block.getZ() + 0.5;
				final double particleY = (loc.getY() > highest.getY() ? loc.getY() : highest.getY() + 3) + 5;
				if (distanceSquared <= disasterRangeSquared) {
					player.spawnParticle(VersionUtils.getRedstoneDust(), centerX, particleY, centerZ, 2, .5, 3, .5, 1, dust);
					for (int i=0; i < 2; i++)
						player.spawnParticle(VersionUtils.getNormalSmoke(), centerX + random.nextFloat(-.5f, .5f), particleY + random.nextFloat(-5f, 2f), centerZ + random.nextFloat(-.5f, .5f), 0, random.nextFloat(-.2f, .2f), random.nextFloat(-.2f, .2f), random.nextFloat(-.2f, .2f), 1);
				} else {
					player.spawnParticle(VersionUtils.getRedstoneDust(), centerX, particleY, centerZ, 1, .5, 3, .5, 1, dust);
				}
				if (!soundFlag) {
					if (loc.distanceSquared(BlockUtils.getCenterOfBlock(highest)) <= 25)
						soundFlag = true;
					else if (loc.getY() > highest.getY()) {
						final double bx = highest.getX() + 0.5 - loc.getX();
						final double bz = highest.getZ() + 0.5 - loc.getZ();
						if (bx * bx + bz * bz <= 25)
							soundFlag = true;
					}
				}
			}
			if (aboveFlag && soundTick == 0) {
				final Location fixed = new Location(loc.getWorld(), loc.getX(), location.getY(), loc.getZ());
				if (fixed.distanceSquared(location) > trueSmoothingRange)
					playSound(player, loc.clone().add(Utils.getVectorTowards(loc, location).multiply(8.0).setY(7)), Sound.WEATHER_RAIN, SoundCategory.WEATHER, (float) (((0.02 / smoothingRangeExcess) * ((smoothingRangeExcess - (fixed.distance(location) - disasterRange - smoothingRange)))) * smoothingIntensity * currentStrength), .5f);
				else
					playSound(player, loc.clone().add(0, 7, 0), soundFlag ? Sound.WEATHER_RAIN : Sound.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, (float) (0.01 * currentStrength), .5f);
			}
			if (setDawn) {
				final double actualDistance = new Location(loc.getWorld(), loc.getX(), location.getY(), loc.getZ()).distance(location);
				long diff = (long) (((smoothingRange - (actualDistance - disasterRange)) * ((currentTime - world.getTime()) / smoothingRange)));
				player.setPlayerTime(world.getTime() + diff, false);
			}
		});
		
		getChunksInvolvedSafelyAndThen(() -> {
			scheduleTask(new BukkitRunnable() {
				@Override
				public void run() {
					if (currentStrength < 0.5)
						return;
					final ThreadLocalRandom random = ThreadLocalRandom.current();
					involvedChunks.forEach(chunk -> {
						if (!chunk.isLoaded())
							return;
						final Block block = world.getHighestBlockAt((chunk.getX() << 4) + random.nextInt(16), (chunk.getZ() << 4) + random.nextInt(16));
						if (block == null || !isBlockInClimate(block) || !isWithinStorm(block))
							return;
						if (random.nextFloat() < blockIgniteRate && block.getType().isBurnable()) {
							final Block above = block.getRelative(BlockFace.UP);
							if (above != null && above.getType() == Material.AIR)
								new BukkitRunnable() {
									@Override
									public void run() {
										if (above.getType() != Material.AIR)
											return;
										placeBlock(above, Material.FIRE);
									}
								}.runTask(plugin);
						}
						if (random.nextFloat() >= blockChangeRate)
							return;
						final Material type = block.getType();
						final Material[] materials = blockChanges.get(type);
						if (materials != null && materials.length != 0) {
							final Material change = materials[random.nextInt(materials.length)];
							new BukkitRunnable() {
								@Override
								public void run() {
									if (block.getType() != type)
										return;
									replaceBlockWithProperties(block, change);
								}
							}.runTask(plugin);
						}
					});
				}
			}.runTaskTimerAsynchronously(plugin, 0, 1));
		});
	}
	public void clean() {
		super.clean();
		HandlerList.unregisterAll(this);
		fireballs.forEach(uuid -> {
			Entity entity = Bukkit.getEntity(uuid);
			if (entity != null)
				entity.remove();
		});
		if (!DeadlyDisasters.isDisablingPlugin)
			new BukkitRunnable() {
				private int tick;
				private final Vector fireVelocity = direction.clone().multiply(0.6).setY(-2);
				
				@Override
				public void run() {
					Iterator<UUID> it = fallingBlocks.iterator();
					while (it.hasNext()) {
						Entity temp = Bukkit.getEntity(it.next());
						if (temp == null || !temp.isValid()) {
							it.remove();
							continue;
						}
						temp.setVelocity(fireVelocity);
					}
					if (tick++ == 20 || fallingBlocks.isEmpty()) {
						this.cancel();
						if (!fallingBlocks.isEmpty())
							fallingBlocks.forEach(uuid -> {
								Entity temp = Bukkit.getEntity(uuid);
								if (temp != null)
									temp.remove();
							});
					}
				}
			}.runTaskTimer(plugin, 0, 10);
		weatherPlayers.forEach(uuid -> {
			Player player = Bukkit.getPlayer(uuid);
			if (player == null || !player.isOnline())
				return;
			player.resetPlayerTime();
			player.stopSound(Sound.AMBIENT_BASALT_DELTAS_ADDITIONS);
			player.stopSound(Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP);
		});
	}
	public boolean isBlockInClimate(Block block) {
		return world.getTemperature(block.getX(), block.getY(), block.getZ()) >= MIN_TEMPERATURE && world.getHumidity(block.getX(), block.getY(), block.getZ()) <= MAX_HUMIDITY;
	}
	public void addPlayerToWeather(Player player) {
		super.addPlayerToWeather(player);
		Location soundLoc = player.getLocation().add(0, 5, 0);
		playSound(player, soundLoc, Sound.AMBIENT_BASALT_DELTAS_ADDITIONS, SoundCategory.WEATHER, 0.3f, .75f);
		playSound(player, soundLoc, Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP, SoundCategory.WEATHER, 0.3f, .5f);
	}
	public void removePlayerFromWeather(Player player) {
		super.removePlayerFromWeather(player);
		player.stopSound(Sound.AMBIENT_BASALT_DELTAS_ADDITIONS);
		player.stopSound(Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP);
		if (setDawn)
			player.resetPlayerTime();
	}
	public boolean forceDownfallWeather() {
		return false;
	}
	protected String getConfigPath() {
		return "disasters.weather.solar_storm";
	}
	public Set<Environment> getBannedEnvironments() {
		return EnumSet.of(Environment.NETHER, Environment.THE_END);
	}
	
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onFireballExplode(EntityExplodeEvent event) {
		if (!fireballs.remove(event.getEntity().getUniqueId()))
			return;
		event.blockList().forEach(block -> {
			removeBlock(block);
		});
		event.blockList().clear();
	}
}
