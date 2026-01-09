package com.github.jewishbanana.deadlydisasters.disasters.destructive;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.github.jewishbanana.deadlydisasters.disasters.Disaster;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class Supernova extends Disaster {
	
	private int minHeight;
	private double size;
	private double fallSpeed;
	private double particleMultiplier;
	private Material[] debris;
	private boolean destroyItems;
	private boolean farParticles;
	private boolean flashing;
	
	private Map<Block, Float> blocks = new HashMap<>();
	private EnderCrystal crystal;
	private int explosionRange;
	private Particle particle;

	public Supernova(@NotNull Location location, Player player, int level) {
		super(location, player, level);
		
		this.minHeight = getConfigInt("minimum_height");
	}
	public void init() {
		super.init();
		this.size = getConfigDouble("size");
		this.fallSpeed = getConfigDouble("fall_speed_multiplier");
		this.particleMultiplier = getConfigInt("particle_multiplier");
		this.destroyItems = getConfigBoolean("destroy_items");
		this.farParticles = getConfigBoolean("far_particles");
		this.flashing = getConfigBoolean("flashing");
		Set<Material> set = new HashSet<>();
		getConfigStringList("debris").forEach(material -> {
			Set<Material> materials = BlockUtils.getMaterials(material);
			if (materials == null) {
				Utils.sendConsoleMessage("&cERROR the block type or category &d'"+material+"' &cdoes not exist in the world disaster config &b'"+getWorldLink().getConfigName()+"' &cat the section &c'"+getConfigPath()+".debris'&c!");
				return;
			}
			set.addAll(materials);
		});
		this.debris = set.toArray(new Material[0]);
		this.particle = VersionUtils.getLargeExplosion();
				
		switch (level) {
		default:
		case 1:
			disasterRange = 35;
			break;
		case 2:
			disasterRange = 45;
			break;
		case 3:
			disasterRange = 57;
			break;
		case 4:
			disasterRange = 70;
			break;
		case 5:
			disasterRange = 85;
			break;
		case 6:
			disasterRange = 100;
			break;
		}
		disasterRange *= size;
	}
	public Location findPossiblePosition(Location initial) {
		Location temp = super.findPossiblePosition(initial);
		if (temp == null)
			return null;
		temp.setY(temp.getY() + 10);
		return temp;
	}
	public boolean canStart() {
		if (getLocation().getBlockY() < minHeight)
			return false;
		return super.canStart();
	}
	public void start() {
		super.start();
		Location top = location.clone();
		top.setY(320);
		final World world = location.getWorld();
		final Location crystalLoc = top.clone();
		final AtomicBoolean isThreadReady = new AtomicBoolean();
		scheduleTask(new BukkitRunnable() {
			private final Vector vec = new Vector(0, -2.0 * fallSpeed, 0);
			
			@Override
			public void run() {
				crystalLoc.add(vec);
				if (vec.getY() < -0.5)
					vec.multiply(0.995);
				if (crystal != null)
					crystal.remove();
				crystal = world.spawn(crystalLoc, EnderCrystal.class, temp -> {
					temp.setShowingBottom(false);
					temp.setBeamTarget(top);
				});
				world.spawnParticle(Particle.CLOUD, crystalLoc, 10, .5, .5, .5, .01, null, true);
				world.spawnParticle(Particle.FLAME, crystalLoc, 10, .5, .5, .5, .01, null, true);
				Block below = crystalLoc.getBlock().getRelative(BlockFace.DOWN);
				if (below != null && below.getType() != Material.AIR)
					removeBlock(below);
				if (crystalLoc.getY() <= location.getY()) {
					this.cancel();
					scheduleTask(new BukkitRunnable() {
						@Override
						public void run() {
							if (!isThreadReady.get())
								return;
							this.cancel();
							explosionRange = 1;
							scheduleTask(new BukkitRunnable() {
								private final Iterator<Entry<Block, Float>> iterator = blocks.entrySet().iterator();
								private final int maxThrottle = 7000;
								private final ThreadLocalRandom rng = ThreadLocalRandom.current();
								
								@Override
								public void run() {
									final double distanceSquared = explosionRange * explosionRange;
									int tick = 0;
									if (debris.length > 0 && explosionRange > disasterRange - 2)
										while (iterator.hasNext()) {
											if (++tick == maxThrottle)
												return;
											Entry<Block, Float> entry = iterator.next();
											if (entry.getKey().getType() != Material.AIR) {
												if (random.nextInt(8) == 0)
													placeBlock(entry.getKey(), debris[random.nextInt(debris.length)]);
												else
													removeBlock(entry.getKey(), true, false, rng);
											}
											if (entry.getValue() > distanceSquared) {
												explosionRange++;
												return;
											}
										}
									else
										while (iterator.hasNext()) {
											if (++tick == maxThrottle)
												return;
											Entry<Block, Float> entry = iterator.next();
											if (entry.getKey().getType() != Material.AIR)
												removeBlock(entry.getKey(), true, false, rng);
											if (entry.getValue() > distanceSquared) {
												explosionRange++;
												return;
											}
										}
									this.cancel();
									stop();
								}
							}.runTaskTimer(plugin, 0, 1));
							scheduleTask(new BukkitRunnable() {
								@Override
								public void run() {
									final int amount = (int) Math.ceil(explosionRange * 1.5 * particleMultiplier);
									for (int i=0; i < amount; i++)
										world.spawnParticle(particle, location.clone().add(Utils.getRandomizedVector().multiply(explosionRange)), 1, 0, 0, 0, 1, null, farParticles);
								}
							}.runTaskTimerAsynchronously(plugin, 0, 1));
						}
					}.runTaskTimerAsynchronously(plugin, 0, 1));
					scheduleTask(new BukkitRunnable() {
						@Override
						public void run() {
							final double rangeSquared = explosionRange * explosionRange;
							world.getNearbyEntities(location, explosionRange, explosionRange, explosionRange, temp -> temp.isValid() && !EntityUtils.isEntityImmunePlayer(temp)).forEach(entity -> {
								if (entity.getLocation().distanceSquared(location) > rangeSquared
										|| isEntityProtected(entity))
									return;
								if (entity instanceof LivingEntity alive)
									EntityUtils.pureDamageEntity(alive, 1000.0, "deaths.supernova", DamageCause.BLOCK_EXPLOSION, true);
								else {
									if (!destroyItems && entity instanceof Item)
										return;
									entity.remove();
								}
							});
						}
					}.runTaskTimer(plugin, 0, 10));
				}
			}
		}.runTaskTimer(plugin, 0, 1));
		scheduleTask(new BukkitRunnable() {
			private final double soundRange = (disasterRange + 100) * (disasterRange + 100);
			private int soundTick;
			private Color flashColor = Color.WHITE;
			
			@Override
			public void run() {
				if (soundTick-- == 0)
					soundTick = 10;
				world.getPlayers().forEach(player -> {
					Location loc = player.getLocation();
					if (!Utils.isLocationsWithinDistance(loc, crystalLoc, soundRange))
						return;
					final Vector direction = Utils.getVectorTowards(loc, crystalLoc);
					if (soundTick == 10)
						if (explosionRange == 0)
							playSound(player, loc.clone().add(direction.clone().multiply(4.0)), Sound.AMBIENT_NETHER_WASTES_MOOD, 2.0 - (0.002 * loc.distance(crystalLoc)), 0.5);
						else {
							final Location soundLoc = loc.clone().add(direction.clone().multiply(4.0));
							final double vol = 2.0 - (0.0005 * loc.distance(location));
							playSound(player, soundLoc, Sound.AMBIENT_WARPED_FOREST_MOOD, vol, 0.5);
							playSound(player, soundLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, vol, 0.5);
							playSound(player, soundLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, vol / 30, 0.5);
						}
					if (!flashing)
						return;
					Location path = loc.clone();
					for (int i=0; i < 15; i++) {
						if (path.getBlock().getType().isOccluding())
							return;
						path.add(direction);
					}
					VersionUtils.spawnFlashParticle(player, loc.clone().add(direction.clone().multiply(3.0)), 1, 1, 1, 1, .001, flashColor);
				});
			}
		}.runTaskTimerAsynchronously(plugin, 0, 1));
		getChunksInvolvedSafelyAndThen(() -> {
			scheduleTask(new BukkitRunnable() {
				@Override
				public void run() {
					final float centerX = (float) location.getX();
					final float centerY = (float) location.getY();
					final float centerZ = (float) location.getZ();
					final float disasterRangeSquared = (float) (disasterRange * disasterRange);
					final int minX = (int) Math.floor(centerX - disasterRange);
					final int maxX = (int) Math.floor(centerX + disasterRange);
					final int minY = (int) Math.floor(centerY - disasterRange);
					final int maxY = (int) Math.floor(centerY + disasterRange);
					final int minZ = (int) Math.floor(centerZ - disasterRange);
					final int maxZ = (int) Math.floor(centerZ + disasterRange);
					for (int x = minX; x <= maxX; x++) {
					    final float dx = x - centerX;
					    final float dxSquared = dx * dx;
					    for (int y = minY; y <= maxY; y++) {
					        final float dy = y - centerY;
					        final float dySquared = dy * dy;
					        final float dxdySquared = dxSquared + dySquared;
					        for (int z = minZ; z <= maxZ; z++) {
					            final float dz = z - centerZ;
					            final float distanceSquared = dxdySquared + dz * dz;
					            if (distanceSquared <= disasterRangeSquared) {
					                final Block block = world.getBlockAt(x, y, z);
					                if (block != null)
					                    blocks.put(block, distanceSquared);
					            }
					        }
					    }
					}
					blocks = Utils.sortByValue(blocks);
					isThreadReady.set(true);
				}
			}.runTaskAsynchronously(plugin));
		}, true);
	}
	public void clean() {
		super.clean();
		if (crystal != null)
			crystal.remove();
	}
	protected String getConfigPath() {
		return "disasters.destructive.supernova";
	}
	public String getDisplayName() {
		return Utils.convertString(DataUtils.getLanguageString(getConfigPath()));
	}
	public double getRegenTickRate() {
		return level * 2;
	}
	public Set<Environment> getBannedEnvironments() {
		return EnumSet.of(Environment.NETHER);
	}
}
