package com.github.jewishbanana.deadlydisasters.disasters.weather;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.BlockInventoryHolder;
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
import com.github.jewishbanana.deadlydisasters.utils.Utils.AreaClearing;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class EndStorm extends WeatherDisaster implements MobDisaster {
	
	private float teleportRate;
	private float teleportRange;
	private float riftSpawnRate;
	private double riftDamageRate;
	private float mobSpawnRate;
	private boolean riftDestroysItems;
	private boolean riftDestroyItemContainers;
	
	private float particleRate;
	private float soundVolume;
	private List<PotionEffect> effects;
	private final Map<Location, Integer> activeRifts = new ConcurrentHashMap<>();
	private final Set<Location> rifts = ConcurrentHashMap.newKeySet();

	public EndStorm(@NotNull Location location, Player player, int level) {
		super(location, player, level);
	}
	public void init() {
		super.init();
		this.teleportRate = (float) (0.065 * getConfigDouble("teleport_rate_multiplier") * (scale / 2.0));
		this.teleportRange = (float) getConfigDouble("max_teleport_range");
		this.riftSpawnRate = (float) (0.025 * getConfigDouble("rift_spawn_multiplier") * (scale / 1.5));
		this.riftDamageRate = getConfigDouble("rift_damage_rate");
		this.mobSpawnRate = (float) (1.0 / 12.0 * getConfigDouble("mob_spawn_multiplier"));
		this.riftDestroysItems = getConfigBoolean("rift_destroys_items");
		this.riftDestroyItemContainers = getConfigBoolean("rift_destroy_item_containers");
		
		this.effects = buildPotionEffects("entity_effects");
		
		this.particleRate = (float) (0.035 * particleMultiplier * scale);
		this.soundVolume = (float) (0.075 * (scale / 2.0));
		this.soundTickRate = 60;
	}
	public boolean canStart() {
		return super.canStart();
	}
	public void start() {
		super.start();
		location.setY(128);
		scheduleTask(new BukkitRunnable() {
			@Override
			public void run() {
				for (Player player : playersInMonitorArea) {
					if (!player.isValid() || EntityUtils.isPlayerImmune(player))
						continue;
					if (random.nextFloat() < riftSpawnRate)
						scheduleTask(new BukkitRunnable() {
							private final Location playerLoc = player.getLocation();
							
							@Override
							public void run() {
								for (int i=0; i < 30; i++) {
									final Location temp = SpawnUtils.findSmartYSpawn(playerLoc, Utils.findRandomSpotInCircle(playerLoc, 3f, 20f), 3.0, 15);
									if (temp == null)
										continue;
									temp.add(0, 2.0, 0);
									if (rifts.stream().anyMatch(rift -> rift.distanceSquared(temp) <= 25))
										continue;
									if (!Utils.isAreaClear(temp.getBlock().getRelative(BlockFace.UP), AreaClearing.PLUS_SIGN_3D_FROM_CENTER) || !Utils.isLocationExposedToOutdoors(temp))
										continue;
									rifts.add(temp);
									scheduleTask(new BukkitRunnable() {
										private int tick = 80;
										
										@Override
										public void run() {
											if (tick-- <= 0) {
												activeRifts.put(temp, ThreadLocalRandom.current().nextInt(16, 48));
												this.cancel();
												return;
											}
											world.spawnParticle(Particle.PORTAL, temp, (int) ((79 - tick) / 2.5), .4, .5, .4, .01, null, true);
										}
									}.runTaskTimer(plugin, 0, 1));
									break;
								}
							}
						}.runTaskAsynchronously(plugin));
				}
				for (Entity entity : entitiesInMonitorArea) {
					if (!entity.isValid())
						continue;
					if (EntityUtils.isEntityImmunePlayer(entity))
						continue;
					if (entity instanceof LivingEntity living)
						living.addPotionEffects(effects);
					else if (entity instanceof Item && random.nextInt(8 - level) == 0)
						entity.setVelocity(entity.getVelocity().add(new Vector(random.nextFloat(-1, 1), random.nextFloat(), random.nextFloat(-1, 1)).multiply(scale / 4.0 * currentStrength)));
					if (random.nextFloat() < teleportRate) {
						Location entityLoc = entity.getLocation();
						Location spawn = Utils.findRandomSpotInRadius(entityLoc, Math.min(teleportRange - 1, 7f), teleportRange, 2, 10, () -> Utils.getRandomizedVector(1f, 0.25f, 1f));
						if (spawn != null) {
							spawn.setDirection(entityLoc.getDirection());
							entity.teleport(spawn);
							world.playSound(spawn, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1, 1);
							if (entity instanceof Player p)
								VersionUtils.spawnDragonBreathParticle(p, spawn.add(0, 1.5, 0), 30, 3, 1, 3, 3, 1f);
						}
					}
				}
			}
		}.runTaskTimer(plugin, 0, 10));
		scheduleTask(new BukkitRunnable() {
			private final Map<Location, Integer> riftCooldowns = new HashMap<>();
			
			@Override
			public void run() {
				final Iterator<Entry<Location, Integer>> iterator = activeRifts.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<Location, Integer> entry = iterator.next();
					int value = entry.getValue();
					if (value == 0) {
						iterator.remove();
						rifts.remove(entry.getKey());
						continue;
					}
					entry.setValue(value - 1);
					Location temp = entry.getKey();
					world.spawnParticle(Particle.PORTAL, temp, 20, .2, .2, .2, 1.5);
					world.spawnParticle(Particle.SQUID_INK, temp.getX(), temp.getY() + 0.5, temp.getZ(), 30, .25, .25, .25, .0001);
					if (value % 6 == 0)
						playSound(temp, Sound.BLOCK_PORTAL_AMBIENT, SoundCategory.AMBIENT, .7f, 1.5f);
					for (Entity e : world.getNearbyEntities(temp, 3.0, 3.0, 3.0, t -> !isEntityProtected(t))) {
						Location entityLoc = e.getLocation();
						e.setVelocity(Utils.getVectorTowards(entityLoc, temp).multiply(0.3));
						if (e instanceof LivingEntity living) {
							if (!e.isDead() && entityLoc.distanceSquared(temp) < 1)
								EntityUtils.pureDamageEntity(living, riftDamageRate, "deaths.end_storm", DamageCause.VOID);
						} else if (entityLoc.distanceSquared(temp) < 4 && !(!riftDestroysItems && e instanceof Item))
							e.remove();
					}
					if (random.nextInt(4) == 0)
						for (int i=0; i < 3; i++) {
							Block b = BlockUtils.rayTraceForBlock(temp, Utils.getRandomizedVector(), 4.0, t -> !t.isPassable());
							if (b == null)
								continue;
							if (!riftDestroyItemContainers && b.getState() instanceof BlockInventoryHolder)
								continue;
							FallingBlock fb = convertBlockIntoFallingBlock(b);
							if (fb == null)
								continue;
							fb.setHurtEntities(true);
							fb.setDropItem(false);
							fb.setVelocity(Utils.getVectorTowards(b.getLocation().add(.5, .5, .5), temp).multiply(0.3));
							break;
						}
					if (!riftCooldowns.containsKey(temp) && random.nextFloat() < mobSpawnRate) {
						riftCooldowns.put(temp, random.nextInt(4, 8));
						Mob mob = null;
						switch (random.nextInt(DependencyUtils.isUltimateContentEnabled() ? 7 : 2)) {
						default:
						case 0:
							mob = world.spawn(temp, Endermite.class);
							break;
						case 1:
							mob = world.spawn(temp, Enderman.class);
							break;
						case 2:
							mob = com.github.jewishbanana.uiframework.entities.UIEntityManager.spawnEntity(temp, com.github.jewishbanana.ultimatecontent.entities.endentities.EndTotem.class).getCastedEntity();
							break;
						case 3:
							mob = com.github.jewishbanana.uiframework.entities.UIEntityManager.spawnEntity(temp, com.github.jewishbanana.ultimatecontent.entities.endentities.VoidArcher.class).getCastedEntity();
							break;
						case 4:
							mob = com.github.jewishbanana.uiframework.entities.UIEntityManager.spawnEntity(temp, com.github.jewishbanana.ultimatecontent.entities.endentities.VoidGuardian.class).getCastedEntity();
							break;
						case 5:
							mob = com.github.jewishbanana.uiframework.entities.UIEntityManager.spawnEntity(temp, com.github.jewishbanana.ultimatecontent.entities.endentities.VoidStalker.class).getCastedEntity();
							break;
						case 6:
							mob = com.github.jewishbanana.uiframework.entities.UIEntityManager.spawnEntity(temp, com.github.jewishbanana.ultimatecontent.entities.endentities.VoidWorm.class).getCastedEntity();
							break;
						}
						if (mob != null) {
							if (player != null && !EntityUtils.isPlayerImmune(player))
								mob.setTarget(player);
							addEntityToDisasterList(mob);
						}
					}
				}
				riftCooldowns.entrySet().removeIf(entry -> {
					int value = entry.getValue() - 1;
					if (value < 0)
						return true;
					entry.setValue(value);
					return false;
				});
			}
		}.runTaskTimer(plugin, 0, 5));
		
		final Set<UUID> playersInStorm = ConcurrentHashMap.newKeySet();
		final List<UUID> playersIteratedOver = new ArrayList<>();
		final Map<UUID, Integer> timeInStorm = new HashMap<>();
		createAsyncEntityMonitor(Entity::isValid, 
				(found, entities, players) -> {
					playersInStorm.clear();
					playersInStorm.addAll(playersIteratedOver);
					playersIteratedOver.clear();
					found.forEach((entity, loc) -> {
						if (isEntityProtected(entity) || !isWithinStorm(loc))
							return;
						if (DependencyUtils.isUltimateContentEnabled() 
								&& com.github.jewishbanana.uiframework.entities.UIEntityManager.getEntity(entity) instanceof com.github.jewishbanana.ultimatecontent.entities.BaseEntity base 
								&& base.getEntityType().category == com.github.jewishbanana.ultimatecontent.entities.CustomEntityType.Category.END_ENTITIES)
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
						if (entity instanceof Player p)
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
				if (random.nextFloat() >= particleRate * currentStrength || !isWithinStorm(block))
					continue;
				final Block highest = world.getHighestBlockAt(block.getX(), block.getZ());
				if (highest == null || highest.getY() - loc.getBlockY() > 10)
					continue;
				final double centerX = block.getX() + 0.5;
				final double centerZ = block.getZ() + 0.5;
				final double particleY = (loc.getY() > highest.getY() ? loc.getY() : highest.getY() + 3) + 2.5;
				if (!flag) {
					VersionUtils.spawnDragonBreathParticle(player, centerX, particleY, centerZ, 2, .5, 2.5, .5, .05, 1f);
					player.spawnParticle(VersionUtils.getLargeSmoke(), centerX, particleY, centerZ, 1, .5, 2.5, .5, .05);
				} else {
					final double dx = centerX - loc.getX();
					final double dz = centerZ - loc.getZ();
					if (dx * dx + dz * dz > internalDistanceSquared) {
						VersionUtils.spawnDragonBreathParticle(player, centerX, particleY, centerZ, 5, .5, 2.5, .5, .05, 1f);
						player.spawnParticle(VersionUtils.getLargeSmoke(), centerX, particleY, centerZ, 2, .5, 2.5, .5, .05);
					} else {
						for (int i=0; i < 2; i++)
							VersionUtils.spawnDragonBreathParticle(player, centerX + random.nextFloat(-.5f, .5f), particleY + random.nextFloat(7f, 10f), centerZ + random.nextFloat(-.5f, .5f), 0, random.nextFloat(-.5f, .5f), random.nextFloat(-1.25f, -.5f), random.nextFloat(-.5f, .5f), 1, 1f);
						if (random.nextInt(3) == 0)
							player.spawnParticle(VersionUtils.getLargeSmoke(), centerX + random.nextFloat(-.5f, .5f), particleY + random.nextFloat(7f, 10f), centerZ + random.nextFloat(-.5f, .5f), 0, random.nextFloat(-.5f, .5f), random.nextFloat(-1.25f, -.5f), random.nextFloat(-.5f, .5f), 1);
					}
				}
				final double dx = highest.getX() - loc.getX();
				final double dz = highest.getZ() - loc.getZ();
				final double dist = dx * dx + dz * dz;
				if (closest == null || dist < closestDistance) {
					closest = highest;
					closestDistance = dist;
				}
			}
			if (closest != null) {
				playSound(player, loc.add(0, 5, 0), Sound.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, (float) (soundVolume * currentStrength * (flag ? 1.0 : 0.15)), .5f);
				if (soundTick == 0) {
					player.stopSound(Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP);
					playSound(player, BlockUtils.getCenterOfBlock(closest), Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP, SoundCategory.WEATHER, 2, 2);
				}
			}
		}, pair -> {
			final ThreadLocalRandom random = ThreadLocalRandom.current();
			final Player player = pair.getFirst();
			final double intensity = pair.getSecond();
			final Location loc = player.getLocation();
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
				if (highest == null || highest.getY() - loc.getBlockY() > 10)
					continue;
				aboveFlag = true;
				final double centerX = block.getX() + 0.5;
				final double centerZ = block.getZ() + 0.5;
				final double particleY = (loc.getY() > highest.getY() ? loc.getY() : highest.getY() + 3) + 5;
				if (distanceSquared <= disasterRangeSquared) {
					VersionUtils.spawnDragonBreathParticle(player, centerX, particleY, centerZ, 1, .5, .7, .5, .05, 1f);
					player.spawnParticle(VersionUtils.getLargeSmoke(), centerX, particleY, centerZ, 1, .5, .7, .5, .05);
				} else {
					VersionUtils.spawnDragonBreathParticle(player, centerX + random.nextFloat(-.5f, .5f), particleY + random.nextFloat(3f, 8f), centerZ + random.nextFloat(-.5f, .5f), 0, random.nextFloat(-.2f, .2f), random.nextFloat(-.5f, -.2f), random.nextFloat(-.2f, .2f), .05, 1f);
					player.spawnParticle(VersionUtils.getLargeSmoke(), centerX + random.nextFloat(-.5f, .5f), particleY + random.nextFloat(3f, 8f), centerZ + random.nextFloat(-.5f, .5f), 0, random.nextFloat(-.2f, .2f), random.nextFloat(-.5f, -.2f), random.nextFloat(-.2f, .2f), .05);
				}
			}
			if (aboveFlag && soundTick == 0) {
				final Location fixed = new Location(world, loc.getX(), location.getY(), loc.getZ());
				if (fixed.distanceSquared(location) > trueSmoothingRange)
					playSound(player, loc.clone().add(Utils.getVectorTowards(loc, location).multiply(8.0).setY(7)), Sound.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, (float) (((soundVolume / smoothingRangeExcess) * ((smoothingRangeExcess - (fixed.distance(location) - disasterRange - smoothingRange)))) * smoothingIntensity * currentStrength), .5f);
				else
					playSound(player, loc.add(0, 7, 0), Sound.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, (float) (soundVolume * currentStrength), .5f);
			}
		});
	}
	public void clean() {
		super.clean();
		weatherPlayers.forEach(uuid -> {
			Player player = Bukkit.getPlayer(uuid);
			if (player == null || !player.isOnline())
				return;
			player.stopSound(Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP);
		});
	}
	public boolean isEntityProtected(Entity entity) {
		return super.isEntityProtected(entity) || 
				(DependencyUtils.isUltimateContentEnabled() && Utils.isNotNullAndCondition(com.github.jewishbanana.uiframework.entities.UIEntityManager.getEntity(entity), e -> e instanceof com.github.jewishbanana.ultimatecontent.entities.BaseEntity<?> base && base.getEntityType().category == com.github.jewishbanana.ultimatecontent.entities.CustomEntityType.Category.END_ENTITIES));
	}
	protected String getConfigPath() {
		return "disasters.weather.end_storm";
	}
	public double getRegenTickRate() {
		return 0.01;
	}
	public Set<Environment> getBannedEnvironments() {
		return EnumSet.of(Environment.NORMAL, Environment.NETHER);
	}
}
