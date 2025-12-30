package com.github.jewishbanana.deadlydisasters.disasters.destructive;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.disasters.Disaster;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class Tornado extends Disaster {
	
	private int minHeight;
	private double size;
	private int time;
	private double speed;
	private double forceMultiplier;
	private int maxEntities;
	
	private double blockPickupRate;
	private double blockPickupRange;
	private Set<Entity> entitiesInList = new HashSet<>();
	private double width;
	private float particleRate;
	
	private Particle particleType = Particle.CLOUD;
	
	public Tornado(Location location, Player player, int level) {
		super(location, player, level);
		
		this.minHeight = getConfigInt("minimum_height");
	}
	public void init() {
		super.init();
		this.size = getConfigDouble("size");
		this.speed = getConfigDouble("speed");
		this.forceMultiplier = getConfigDouble("force_multiplier");
		this.maxEntities = getConfigInt("max_entities.level_"+level);
		switch (level) {
		default:
		case 1:
			disasterRange = 30.0;
			time = 600;
			break;
		case 2:
			disasterRange = 40.0;
			time = 700;
			break;
		case 3:
			disasterRange = 50.0;
			time = 800;
			break;
		case 4:
			disasterRange = 60.0;
			time = 900;
			break;
		case 5:
			disasterRange = 75.0;
			time = 1000;
			break;
		case 6:
			disasterRange = 90.0;
			time = 1200;
			break;
		}
		disasterRange *= size;
		time *= getConfigDouble("life_multiplier");
		
		this.blockPickupRate = 0.15 * getConfigDouble("block_pickup_rate") * level;
		this.blockPickupRange = disasterRange * 1.5 * getConfigDouble("block_pickup_range_multiplier");
		this.width = level * 5 + 5;
		this.particleRate = (float) (0.1 * getConfigDouble("particle_multiplier"));
	}
	public boolean canStart() {
		if (getLocation().getBlockY() < minHeight)
			return false;
		if (!Utils.isAreaFlatGrounded(getLocation()))
			return false;
		return super.canStart();
	}
	public void start() {
		super.start();
		addDeathWatcher("deaths.tornado");
		final World world = getLocation().getWorld();
		final Set<Entity> entities = ConcurrentHashMap.newKeySet();
		final Map<Entity, Location> nearbyEntities = new ConcurrentHashMap<>();
		final AtomicBoolean processEntities = new AtomicBoolean();
		final Set<Entity> interruptEntities = new HashSet<>();
		final Map<Entity, Integer> cooldowns = new HashMap<>();
		scheduleTask(new BukkitRunnable() {
			@Override
			public void run() {
				if (processEntities.get())
					return;
				processEntities.set(true);
				nearbyEntities.clear();
				for (Entity entity : world.getNearbyEntities(location, disasterRange, 193, disasterRange, e -> e.isValid()))
					nearbyEntities.put(entity, entity.getLocation());
				entitiesInList = new HashSet<>(entities);
				entitiesInList.addAll(interruptEntities);
				Iterator<Entry<Entity, Integer>> it = cooldowns.entrySet().iterator();
				while (it.hasNext()) {
					Entry<Entity, Integer> entry = it.next();
					if (entry.getValue() > 0)
						entry.setValue(entry.getValue() - 1);
					else
						it.remove();
				}
				entitiesInList.removeIf(e -> cooldowns.containsKey(e));
				interruptEntities.clear();
				
				final Map<Entity, Location> map = Map.copyOf(nearbyEntities);
				
				scheduleTask(new BukkitRunnable() {
					private final double radiusSquared = disasterRange * disasterRange;
					
					@Override
					public void run() {
						Set<Entity> set = new HashSet<>();
						map.forEach((entity, loc) -> {
							if (set.size() >= maxEntities && !(entity instanceof LivingEntity))
								return;
							if (!Utils.isLocationsWithinDistance(new Location(loc.getWorld(), loc.getX(), location.getY(), loc.getZ()), location, radiusSquared))
								return;
							if (isEntityProtected(entity))
								return;
							if (!loc.clone().add(0, entity.getHeight(), 0).getBlock().isLiquid() && (!(entity instanceof LivingEntity) || EntityUtils.isLocationExposedToOutdoors(loc, 6.0)))
								set.add(entity);
							if (entity instanceof Player p)
								for (int i=0; i < 3; i++)
									if (random.nextFloat() < blockPickupRate)
										for (int j=0; j < 3; j++) {
											Vector towards = Utils.getVectorTowards(loc, location.clone().add(0, random.nextInt(3, 30), 0));
											Block block = BlockUtils.rayTraceForBlock(p.getLocation().add(0, p.getHeight() / 2.0, 0), towards.clone().add(new Vector(random.nextFloat(-.5f, .5f), random.nextFloat(-.1f, .8f), random.nextFloat(-.5f, .5f))), 7.0);
											if (block != null && !Main.isDisablingPlugin) {
												new BukkitRunnable() {
													@Override
													public void run() {
														FallingBlock fb = convertBlockIntoFallingBlock(block);
														if (fb == null)
															return;
														fb.setVelocity(towards.clone().multiply(0.8));
														fb.setHurtEntities(true);
														fb.setDropItem(false);
														entitiesInList.add(fb);
														interruptEntities.add(fb);
													}
												}.runTaskLater(plugin, random.nextInt(10));
												break;
											}
										}
						});
						entities.clear();
						entities.addAll(set);
						processEntities.set(false);
					}
				}.runTaskAsynchronously(plugin));
			}
		}.runTaskTimer(plugin, 0, 1));
		
		scheduleTask(new BukkitRunnable() {
			private double height;
			private final Vector movement = Utils.getRandomizedVector().setY(0).normalize().multiply(0.1 * speed);
			private int heightIteration = 1;
			private final Map<Entity, Integer> heightMap = new HashMap<>();
			
			@Override
			public void run() {
				height = Math.max(200 - location.getY(), 100);
				location.add(movement);
				if (location.getBlock().isPassable())
					location.setY(location.getY() - 0.05);
				else if (!location.getBlock().getRelative(BlockFace.UP).isPassable())
					location.setY(location.getY() + 0.05);
				
//				for (int i=0; i < 10; i++)
//					location.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, location.clone().add(new Vector(0, i, 0)), 1, 0, 0, 0, 0.0001);
				
				final double heightIncrement = width / height;
				final Iterator<Entity> iterator = entitiesInList.iterator();
				while (iterator.hasNext()) {
					Entity entity = iterator.next();
					Location loc = entity.getLocation();
					if (entity instanceof Player player) {
						if (player.isFlying())
							continue;
					}
					final double diff = loc.getY() - location.getY();
					if (diff > height)
						continue;
					final double offset = diff * heightIncrement + 6;
					if (loc.getX() < location.getX() + offset && loc.getX() > location.getX() - offset && loc.getZ() < location.getZ() + offset && loc.getZ() > location.getZ() - offset) {
						Vector vec = new Vector(loc.getX() - location.getX(), 0, loc.getZ() - location.getZ()).rotateAroundY(random.nextFloat(1f, 2f)).normalize().multiply(-1).multiply(diff / width + random.nextFloat(0f, (float) (level))).multiply(random.nextFloat(1f, 2f)).add(new Vector(0, (level * 0.05 + 0.3) * random.nextFloat(0.7f, 1.5f), 0)).multiply((level * 0.04 + 0.15) * forceMultiplier);
						try {
							vec.checkFinite();
						} catch (IllegalArgumentException ex) {
							continue;
						}
						entity.setVelocity(vec);
//						if (entity instanceof Player p)
//							p.sendMessage("in");
						if (random.nextFloat() < particleRate) {
							double particleSpeed = (level * 0.1) - (new Location(world, loc.getX(), location.getY(), loc.getZ()).distance(loc) * (0.07 * forceMultiplier));
							vec.normalize();
							for (int i=0; i < 2; i++)
								world.spawnParticle(particleType, loc.clone().add(random.nextFloat() * 3 - 1.5, random.nextFloat() * 3 - 1.5, random.nextFloat() * 3 - 1.5), 0, vec.getX() * particleSpeed, (random.nextFloat(0, 0.8f) / 1.5) * particleSpeed, vec.getZ() * particleSpeed, 1, null, true);
						}
						if (random.nextInt(level * 10 + 70) == 0) {
							cooldowns.put(entity, random.nextInt(3, 12));
							heightMap.remove(entity);
							iterator.remove();
							continue;
						}
					} else {
						Integer heightOffset = heightMap.get(entity);
						if (heightOffset == null) {
							heightOffset = random.nextInt(1, level * 5 + 10);
							heightMap.put(entity, heightOffset);
						}
						final Location centerTornado = location.clone().add(0, heightOffset, 0);
						if (entity instanceof LivingEntity) {
							Vector vec = Utils.getVectorTowards(centerTornado, loc).rotateAroundY(1.5).multiply(-1).add(Utils.getVectorTowards(loc, centerTornado).multiply(3)).multiply(0.04 * level * forceMultiplier);
							vec.multiply((1.0 - (new Location(world, loc.getX(), location.getY(), loc.getZ()).distance(location) * (1.0 / disasterRange))));
							entity.setVelocity(entity.getVelocity().add(vec));
//							if (entity instanceof Player p)
//								p.sendMessage("out "+entitiesInList.size()+" max "+maxEntities);
						} else {
							Vector vec = Utils.getVectorTowards(centerTornado, loc).rotateAroundY(1.5).multiply(-1).add(Utils.getVectorTowards(loc, centerTornado).multiply(3)).normalize().multiply((0.1 * level + 0.3) * forceMultiplier * random.nextFloat(0.5f, 1.5f));
							vec.multiply((1.0 - (new Location(world, loc.getX(), location.getY(), loc.getZ()).distance(location) * (1.0 / disasterRange))));
							entity.setVelocity(vec);
						}
						if (random.nextInt(level * 10 + 70) == 0) {
							cooldowns.put(entity, random.nextInt(3, 12));
							heightMap.remove(entity);
							iterator.remove();
							continue;
						}
					}
				}
				Location temp = location.clone().add(0, heightIteration, 0);
				Vector right = new Vector(movement.getZ(), 0, -movement.getX()).normalize();
				for (int i=0; i < 4; i++) {
					if (heightIteration > height) {
						heightIteration = 1;
						temp = location.clone().add(0, heightIteration, 0);
					}
					final double length = heightIncrement * heightIteration;
					for (double x = -length; x <= length; x++) {
						Block b = temp.clone().add(right.clone().multiply(x)).getBlock();
//						b.getWorld().spawnParticle(Particle.FLAME, BlockUtils.getCenterOfBlock(b), 1, 0, 0, 0, 0.001);
						if (!b.isPassable()) {
							FallingBlock fb = convertBlockIntoFallingBlock(b);
							if (fb == null)
								return;
							fb.setVelocity(new Vector(0, 0.3, 0));
							entitiesInList.add(fb);
							interruptEntities.add(fb);
						}
					}
					temp.add(0, 1, 0);
					heightIteration++;
				}
				for (int i=0; i < (0.4 * level + 3); i++)
					if (entitiesInList.size() < maxEntities && random.nextFloat() < blockPickupRate) {
						Location pickup = location.clone().add(Utils.getRandomizedVector().setY(0).multiply(random.nextInt(3, (int) blockPickupRange)));
						Block b = pickup.getWorld().getHighestBlockAt(pickup);
						if (b.isPassable())
							for (int j=0; j < 5; j++) {
								b = b.getRelative(BlockFace.DOWN);
								if (b == null || !b.isPassable())
									break;
							}
						if (b == null)
							continue;
						FallingBlock fb = convertBlockIntoFallingBlock(b);
						if (fb == null)
							return;
						fb.setHurtEntities(true);
						fb.setDropItem(false);
//						Location loc = fb.getLocation();
//						fb.setVelocity(Utils.getVectorTowards(loc, centerTornado).multiply(0.04 * level * forceMultiplier).setY(level / 20));
						fb.setVelocity(new Vector(0, 0.3, 0));
						entitiesInList.add(fb);
						interruptEntities.add(fb);
					}
				if (time-- <= 0)
					stop();
			}
		}.runTaskTimer(plugin, 1, 1));
	}
	public void clean() {
		super.clean();
		removeDeathWatcher(200);
	}
	public Function<PlayerDeathEvent, Boolean> getDeathCheck() {
		return event -> {
			if (event.getEntity().getLastDamageCause() == null)
				return false;
			DamageCause cause = event.getEntity().getLastDamageCause().getCause();
			if (cause != DamageCause.FALL && cause != DamageCause.FALLING_BLOCK)
				return false;
			Location loc = event.getEntity().getLocation();
			if (loc.getY() > location.getY() - 40
					|| !Utils.isLocationsWithinDistance(new Location(loc.getWorld(), loc.getX(), location.getY(), loc.getZ()), location, disasterRange * disasterRange))
				return false;
			return true;
		};
	}
	protected String getConfigPath() {
		return "disasters.destructive.tornado";
	}
	public String getDisplayName() {
		return Utils.convertString(DataUtils.getLanguageString(getConfigPath()));
	}
	public Set<Environment> getBannedEnvironments() {
		return Set.of(Environment.NETHER, Environment.THE_END);
	}
}