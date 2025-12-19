package com.github.jewishbanana.deadlydisasters.disasters.weather;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.github.jewishbanana.deadlydisasters.disasters.WeatherDisaster;
import com.github.jewishbanana.deadlydisasters.listeners.EntitiesListener;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.SpawnUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.Utils.AreaClearing;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class EndStorm extends WeatherDisaster {
	
	private float teleportRate;
	private float teleportRange;
	private float riftSpawnRate;
	private double riftDamageRate;
	private float mobSpawnRate;
	private boolean riftDestroysItems;
	
	private float particleRate;
	private float soundVolume;
	private Set<PotionEffect> effects;
	private final Set<UUID> mobs = new HashSet<>();
	private final Map<UUID, UUID> mobTargets = new HashMap<>();
	private Set<Entity> currentEntities = Set.of();
	private final Map<Location, Integer> activeRifts = new ConcurrentHashMap<>();
	private final Set<Location> rifts = ConcurrentHashMap.newKeySet();

	public EndStorm(@NotNull Location location, Player player, int level) {
		super(location, player, level);
	}
	public void init() {
		super.init();
		this.teleportRate = (float) (0.0325 * getConfigDouble("teleport_rate_multiplier") * (scale / 2.0));
		this.teleportRange = (float) getConfigDouble("max_teleport_range");
		this.riftSpawnRate = (float) (0.0325 * getConfigDouble("rift_spawn_multiplier") * scale);
		this.riftDamageRate = getConfigDouble("rift_damage_rate");
		this.mobSpawnRate = (float) (0.02 * getConfigDouble("mob_spawn_multiplier") * (scale / 2.0));
		this.riftDestroysItems = getConfigBoolean("rift_destroys_items");
		
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
		final World world = location.getWorld();
		scheduleTask(new BukkitRunnable() {
			private final Map<Location, Integer> riftCooldowns = new HashMap<>();
			
			@Override
			public void run() {
				for (Entity entity : currentEntities) {
					if (entity instanceof Player player) {
						if (EntityUtils.isPlayerImmune(player))
							continue;
						if (random.nextFloat() < riftSpawnRate)
							scheduleTask(new BukkitRunnable() {
								private final Location playerLoc = entity.getLocation();
								
								@Override
								public void run() {
									for (int i=0; i < 30; i++) {
										final Location temp = SpawnUtils.findSmartYSpawn(playerLoc, Utils.findRandomSpotInCircle(playerLoc, 3.0, 20.0), 3.0, 15);
										if (temp == null)
											continue;
										temp.add(0, 2.1, 0);
										if (rifts.stream().anyMatch(rift -> rift.distanceSquared(temp) <= 25))
											continue;
										if (!Utils.isAreaClear(temp.getBlock().getRelative(BlockFace.UP), AreaClearing.PLUS_SIGN_3D_FROM_CENTER) || !EntityUtils.isLocationExposedToOutdoors(temp, 12.0))
											continue;
										rifts.add(temp);
										scheduleTask(new BukkitRunnable() {
											private int tick = 60;
											
											@Override
											public void run() {
												if (tick-- <= 0) {
													activeRifts.put(temp, random.nextInt(80, 240));
													this.cancel();
													return;
												}
												temp.getWorld().spawnParticle(Particle.PORTAL, temp, (59 - tick) / 10 * 2, .1, 1.2, .1, .01, null, true);
											}
										}.runTaskTimer(plugin, 0, 1));
										break;
									}
								}
							}.runTaskAsynchronously(plugin));
					}
					if (entity instanceof LivingEntity alive) {
						alive.addPotionEffects(effects);
					} else if (entity instanceof Item && random.nextInt(10) == 0)
						entity.setVelocity(entity.getVelocity().add(new Vector(random.nextFloat(-1, 1), random.nextFloat(), random.nextFloat(-1, 1)).multiply(scale / 2.0 * currentStrength)));
					if (random.nextFloat() < teleportRate) {
						Location entityLoc = entity.getLocation();
						Location spawn = Utils.findRandomSpotInRadius(entityLoc, Math.min(teleportRange - 1, 7.0), teleportRange, 2, 10, () -> Utils.getRandomizedVector(1.0, 0.25, 1.0));
						if (spawn != null) {
							spawn.setDirection(entityLoc.getDirection());
							entity.teleport(spawn);
							world.playSound(spawn, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1, 1);
							if (entity instanceof Player p)
								p.spawnParticle(Particle.DRAGON_BREATH, spawn.add(0, 1.5, 0), 30, 3, 1, 3, 3);
						}
					}
				}
				for (Entry<UUID, UUID> entry : mobTargets.entrySet()) {
					Mob entity = (Mob) Bukkit.getEntity(entry.getKey());
					if (entity == null || entity.isDead() || entity.getTarget() != null)
						continue;
					Entity target = Bukkit.getEntity(entry.getValue());
					if (target == null || target.isDead() || !target.getWorld().equals(entity.getWorld()))
						continue;
					entity.setTarget((LivingEntity) target);
				}
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
					world.spawnParticle(Particle.SQUID_INK, temp.clone().add(0, .5, 0), 30, .25, .25, .25, .0001);
					playSound(temp, Sound.BLOCK_PORTAL_AMBIENT, SoundCategory.AMBIENT, .7, 1);
					for (Entity e : world.getNearbyEntities(temp, 3.0, 3.0, 3.0, t -> !isEntityProtected(t))) {
						Location entityLoc = e.getLocation();
						e.setVelocity(Utils.getVectorTowards(entityLoc, temp).multiply(0.3));
						if (e instanceof LivingEntity alive) {
							if (!e.isDead() && entityLoc.distanceSquared(temp) < 1 && !(e instanceof ItemFrame))
								EntityUtils.pureDamageEntity(alive, riftDamageRate, "deaths.end_storm", DamageCause.VOID);
						} else if (entityLoc.distanceSquared(temp) < 4 && !(e instanceof Item && !riftDestroysItems))
							e.remove();
					}
					if (random.nextInt(4) == 0) {
						Block b = BlockUtils.rayTraceForBlock(temp, Utils.getRandomizedVector(), 4.0, t -> t.getType().isBlock());
						if (b != null) {
							FallingBlock fb = convertBlockIntoFallingBlock(b);
							if (fb != null) {
								fb.setHurtEntities(true);
								fb.setDropItem(false);
								fb.setVelocity(Utils.getVectorTowards(b.getLocation().add(.5, .5, .5), temp).multiply(0.3));
							}
						}
					}
					if (!riftCooldowns.containsKey(temp) && random.nextFloat() < mobSpawnRate) {
						riftCooldowns.put(temp, random.nextInt(20, 40));
						Mob mob = null;
						switch (DependencyUtils.isUltimateContentEnabled() ? random.nextInt(7) : random.nextInt(2)) {
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
							mob.setTarget(player);
							mobs.add(mob.getUniqueId());
							mobTargets.put(mob.getUniqueId(), player.getUniqueId());
							EntitiesListener.attachRemoveKey(mob);
						}
					}
				}
				Iterator<Entry<Location, Integer>> it = riftCooldowns.entrySet().iterator();
				while (it.hasNext()) {
					Entry<Location, Integer> entry = it.next();
					int value = entry.getValue();
					if (value == 0) {
						it.remove();
						continue;
					}
					entry.setValue(value - 1);
				}
				time -= 5;
				if (time <= 0)
					stop();
			}
		}.runTaskTimer(plugin, 0, 5));
		scheduleTask(new BukkitRunnable() {
			final AtomicBoolean processEntities = new AtomicBoolean();
			final Map<Entity, Location> foundEntities = new ConcurrentHashMap<>();
			final Set<Entity> entitiesInStorm = ConcurrentHashMap.newKeySet();

			@Override
			public void run() {
				if (processEntities.get())
					return;
				processEntities.set(true);
				foundEntities.clear();
				for (Entity entity : world.getNearbyEntities(location, disasterRange, 193, disasterRange, e -> e.isValid()))
					foundEntities.put(entity, entity.getLocation().add(0, entity.getHeight() / 2.0, 0));
				currentEntities = Set.copyOf(entitiesInStorm);
				scheduleTask(new BukkitRunnable() {
					private final double radiusSquared = disasterRange * disasterRange;
					
					@Override
					public void run() {
						final Set<Entity> set = new HashSet<>();
						foundEntities.forEach((entity, loc) -> {
							if (!Utils.isLocationsWithinDistance(new Location(loc.getWorld(), loc.getX(), location.getY(), loc.getZ()), location, radiusSquared))
								return;
							if (isEntityProtected(entity))
								return;
							if (EntityUtils.isLocationExposedToOutdoors(loc, 12.0))
								set.add(entity);
						});
						entitiesInStorm.clear();
						entitiesInStorm.addAll(set);
						processEntities.set(false);
					}
				}.runTaskAsynchronously(plugin));
			}
		}.runTaskTimer(plugin, 0, 1));
		
		final double distanceSquared = disasterRange * disasterRange;
		final double trueSmoothingRange = (disasterRange + smoothingRange) * (disasterRange + smoothingRange);
		final double internalDistanceSquared = (particleRenderDistance - 1.5) * (particleRenderDistance - 1.5);
		createParticleAsyncTask(player -> {
			final Location loc = player.getLocation();
			Block closest = null;
			double closestDistance = 0;
			final boolean flag = currentEntities.contains(player);
			for (Block block : BlockUtils.getBlocksInCircleRadius(loc, particleRenderDistance)) {
				if (new Location(block.getWorld(), block.getX() + 0.5, location.getY(), block.getZ() + 0.5).distanceSquared(location) > distanceSquared 
						|| random.nextFloat() > particleRate * currentStrength)
					continue;
				Block highest = new Location(block.getWorld(), block.getX(), block.getWorld().getHighestBlockYAt(block.getX(), block.getZ()), block.getZ()).getBlock();
				if (highest == null 
						|| highest.getY() - loc.getBlockY() > 10)
					continue;
				final Location particleLoc = new Location(loc.getWorld(), block.getX() + 0.5, (loc.getY() > highest.getY() ? loc.getY() : highest.getY() + 3) + 2.5, block.getZ() + 0.5);
				if (!flag) {
					player.spawnParticle(Particle.DRAGON_BREATH, particleLoc, 2, .5, 2.5, .5, .05);
					player.spawnParticle(VersionUtils.getLargeSmoke(), particleLoc, 1, .5, 2.5, .5, .05);
				} else if (new Location(particleLoc.getWorld(), particleLoc.getX(), loc.getY(), particleLoc.getZ()).distanceSquared(loc) > internalDistanceSquared) {
					player.spawnParticle(Particle.DRAGON_BREATH, particleLoc, 5, .5, 2.5, .5, .05);
					player.spawnParticle(VersionUtils.getLargeSmoke(), particleLoc, 2, .5, 2.5, .5, .05);
				} else {
					for (int i=0; i < 2; i++)
						player.spawnParticle(Particle.DRAGON_BREATH, particleLoc.clone().add(random.nextFloat()-.5, random.nextFloat(3f) + 7, random.nextFloat()-.5), 0, random.nextFloat(-.5f, .5f), random.nextFloat(-1.25f, -.5f), random.nextFloat(-.5f, .5f), 1);
					if (random.nextInt(3) == 0)
						player.spawnParticle(VersionUtils.getLargeSmoke(), particleLoc.clone().add(random.nextFloat()-.5, random.nextFloat(3f) + 7, random.nextFloat()-.5), 0, random.nextFloat(-.5f, .5f), random.nextFloat(-1.25f, -.5f), random.nextFloat(-.5f, .5f), 1);
				}
				double dist = highest.getLocation().distanceSquared(loc);
				if (closest == null || dist < closestDistance) {
					closest = highest;
					closestDistance = dist;
				}
			}
			if (closest != null) {
				playSound(player, loc.clone().add(0, 5, 0), Sound.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, soundVolume * currentStrength * (flag ? 1.0 : 0.15), .5);
				if (soundTick == 0) {
					Location soundLoc = BlockUtils.getCenterOfBlock(closest);
					player.stopSound(Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP);
					playSound(player, soundLoc, Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP, SoundCategory.WEATHER, 2, 2);
				}
			}
		}, pair -> {
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
						|| highest.getY() - loc.getBlockY() > 10)
					continue;
				aboveFlag = true;
				final Location particleLoc = new Location(loc.getWorld(), block.getX() + 0.5, (loc.getY() > highest.getY() ? loc.getY() : highest.getY() + 3) + 5, block.getZ() + 0.5);
				if (actualDistance <= distanceSquared) {
					player.spawnParticle(Particle.DRAGON_BREATH, particleLoc, 1, .5, .7, .5, .05);
					player.spawnParticle(VersionUtils.getLargeSmoke(), particleLoc, 1, .5, .7, .5, .05);
				} else {
					player.spawnParticle(Particle.DRAGON_BREATH, particleLoc.clone().add(random.nextFloat()-.5, random.nextFloat(5f) + 3, random.nextFloat()-.5), 0, random.nextFloat(-.2f, .2f), random.nextFloat(-.5f, -.2f), random.nextFloat(-.2f, .2f), .05);
					player.spawnParticle(VersionUtils.getLargeSmoke(), particleLoc.clone().add(random.nextFloat()-.5, random.nextFloat(5f) + 3, random.nextFloat()-.5), 0, random.nextFloat(-.2f, .2f), random.nextFloat(-.5f, -.2f), random.nextFloat(-.2f, .2f), .05);
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
	}
	public void clean() {
		super.clean();
		weatherPlayers.forEach(uuid -> {
			Player player = Bukkit.getPlayer(uuid);
			if (player == null || !player.isOnline())
				return;
			player.stopSound(Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP);
		});
		mobs.forEach(uuid -> {
			Entity entity = Bukkit.getEntity(uuid);
			if (entity != null)
				entity.remove();
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
		return Set.of(Environment.NORMAL, Environment.NETHER);
	}
	
}
