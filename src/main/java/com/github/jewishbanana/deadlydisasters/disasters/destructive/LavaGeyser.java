package com.github.jewishbanana.deadlydisasters.disasters.destructive;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.disasters.Disaster;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class LavaGeyser extends Disaster {
	
	private int minHeight;
	private double size;
	private double speed;
	private double damageRadius;
	
	private Material material;
	private Particle particles;
	private Sound sound;
	private int geyserCount;
	private int minSpawnInterval;
	private int maxSpawnInterval;
	private double spawnRange;
	
	private Queue<Geyser> geysers = new ArrayDeque<>();
	
	public LavaGeyser(Location location, Player player, int level) {
		super(location, player, level);
		
		this.minHeight = getConfigInt("minimum_height");
	}
	public void init() {
		super.init();
		this.size = getConfigDouble("size");
		this.speed = getConfigDouble("speed");
		this.damageRadius = getConfigDouble("damage_radius");
		switch (level) {
		default:
		case 1:
			disasterRange = 1.5;
			break;
		case 2:
			disasterRange = 2.5;
			break;
		case 3:
			disasterRange = 3.5;
			break;
		case 4:
			disasterRange = 4.5;
			break;
		case 5:
			disasterRange = 5.5;
			break;
		case 6:
			disasterRange = 10.5;
			break;
		}
		disasterRange *= size;
		
		this.material = Material.LAVA;
		this.particles = Particle.FALLING_LAVA;
		this.sound = Sound.BLOCK_FIRE_EXTINGUISH;
		this.geyserCount = 1;
		this.minSpawnInterval = 20;
		this.maxSpawnInterval = 40;
		this.spawnRange = 0.0;
	}
	public Location findPossiblePosition(Location initial) {
		if (initial == null)
			return null;
		Location temp = new Location(initial.getWorld(), initial.getX(), random.nextInt(4, 8), initial.getZ());
		int count = 0;
		for (Block b : BlockUtils.getBlocksInSphereRadius(temp, disasterRange))
			if (!b.isPassable())
				count++;
		if (count >= Math.pow(disasterRange, 3.0) * 0.7)
			return temp;
		return null;
	}
	public boolean canStart() {
		if (getPlayer() != null && getPlayer().getLocation().getBlockY() < minHeight)
			return false;
		return super.canStart();
	}
	public void start() {
		super.start();
		scheduleTask(new BukkitRunnable() {
			private int count;
			private int interval;
			
			@Override
			public void run() {
				if (interval-- == 0) {
					createGeyser(BlockUtils.getCenterOfBlock(location.clone().add(Utils.getRandomizedVector(1, 0, 1).multiply(spawnRange)).getBlock()), disasterRange, random.nextInt(80, 110), 3.0 * speed);
					if (++count >= geyserCount) {
						this.cancel();
						return;
					}
					interval = random.nextInt(minSpawnInterval, maxSpawnInterval);
				}
			}
		}.runTaskTimer(plugin, 0, 1));
	}
	private void createGeyser(Location location, double size, int maxHeight, double riseRate) {
		Geyser geyser = new Geyser(location, size, maxHeight, riseRate);
		geysers.add(geyser);
		geyser.start();
	}
	private void removeGeyser(Geyser geyser) {
		geysers.remove(geyser);
		if (geysers.isEmpty())
			stop();
	}
	protected String getConfigPath() {
		return "disasters.destructive.lava_geyser";
	}
	public String getDisplayName() {
		return Utils.convertString(DataUtils.getLanguageString(getConfigPath()));
	}
	public Set<Environment> getBannedEnvironments() {
		return Set.of(Environment.NORMAL, Environment.THE_END);
	}
	private class Geyser {
		
		private Location location;
		private double size;
		private int currentHeight;
		private int maxHeight;
		private double riseRate;
		private boolean isFinished;
		
		public Geyser(Location location, double size, int maxHeight, double riseRate) {
			this.location = location;
			this.size = size;
			this.currentHeight = location.getBlockY();
			this.maxHeight = maxHeight;
			this.riseRate = riseRate;
		}
		public void start() {
			Queue<Location> positions = new ArrayDeque<>(BlockUtils.getBlocksInCircleRadius(location, size + damageRadius).stream().map(b -> BlockUtils.getCenterOfBlock(b)).collect(Collectors.toList()));
			final Geyser reference = this;
			final double innerDistanceFalloff = (size + (size / 3.0)) * (size + (size / 3.0));
			scheduleTask(new BukkitRunnable() {
				private double riseTick;
				private final double innerDistance = size * size;
				private final BlockData materialData = material.createBlockData();
				
				@Override
				public void run() {
					riseTick += riseRate;
					while (riseTick >= 1.0) {
						riseTick -= 1.0;
						if (++currentHeight >= maxHeight) {
							scheduleTask(new BukkitRunnable() {
								@Override
								public void run() {
									riseTick += riseRate;
									while (riseTick >= 1.0) {
										riseTick -= 1.0;
										if (currentHeight-- == location.getBlockY()) {
											removeGeyser(reference);
											this.cancel();
											isFinished = true;
											return;
										}
										for (Location loc : positions) {
											if (new Location(loc.getWorld(), loc.getX(), location.getY(), loc.getZ()).distanceSquared(location) <= innerDistanceFalloff) {
												Block block = loc.getBlock();
												if (block.getType() == material)
													block.setType(Material.AIR);
											}
											loc.subtract(0, 1, 0);
										}
									}
									Location particleLoc = location.clone();
									particleLoc.setY(location.getY() + currentHeight);
									location.getWorld().spawnParticle(particles, particleLoc, 10, size * 2, 1.5, size * 2, 1);
								}
							}.runTaskTimer(plugin, random.nextInt(60, 100), 1));
							this.cancel();
							riseTick = 0;
							return;
						}
						for (Location loc : positions) {
							if (new Location(loc.getWorld(), loc.getX(), location.getY(), loc.getZ()).distanceSquared(location) <= innerDistance) {
								Block block = loc.getBlock();
								placeBlock(block, materialData, true, true);
							} else if (random.nextInt(3) == 0)
								removeBlock(loc.getBlock());
							loc.add(0, 1, 0);
						}
					}
					Location particleLoc = location.clone();
					particleLoc.setY(location.getY() + currentHeight);
					location.getWorld().spawnParticle(particles, particleLoc, 10, size * 2, 1.5, size * 2, 1);
				}
			}.runTaskTimer(plugin, 0, 1));
			scheduleTask(new BukkitRunnable() {
				@Override
				public void run() {
					if (isFinished) {
						this.cancel();
						return;
					}
					for (Entity e : location.getWorld().getNearbyEntities(new BoundingBox(location.getX() - (size * 3), location.getY(), location.getZ() - (size * 3), location.getX() + (size * 3), location.getY() + currentHeight, location.getZ() + (size * 3)))) {
						Location loc = e.getLocation();
						loc.setY(location.getY());
						if (e instanceof Player player)
							playSound(player, player.getLocation().add(Utils.getVectorTowards(loc, location).multiply(5.0)), sound, (0.2 * level) * (1.0 - ((1.0 / (size * 3)) * (loc.distance(location) - size))), 0.5);
						if (loc.distanceSquared(location) > innerDistanceFalloff || loc.getBlock().getType() != material)
							continue;
						e.setVelocity(new Vector(0, 2, 0));
//						if (e instanceof LivingEntity alive && !alive.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE) && !EntityUtils.isEntityImmunePlayer(e))
//							EntityUtils.pureDamageEntity(alive, damage, "deaths.water_geyser", DamageCause.LAVA);
					}
				}
			}.runTaskTimer(plugin, 0, 1));
		}
	}
}