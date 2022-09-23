package deadlydisasters.disasters;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import deadlydisasters.disasters.events.DestructionDisaster;
import deadlydisasters.disasters.events.DestructionDisasterEvent;
import deadlydisasters.listeners.CoreListener;
import deadlydisasters.listeners.DeathMessages;
import deadlydisasters.utils.Metrics;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public class Tsunami extends DestructionDisaster {
	
	private int depth;
	private int current,radius,height;
	private World world;
	private boolean push = true;
	private double damage;
	private int tickSpeed = 2;
	private boolean removeWater;
	private Particle particleType;
	private boolean ignoreOcean;
	private Material liquid;
	private BlockData[] materials;
	private int blocksDestroyed;
	
	public Queue<FallingBlock> fallingBlocks = new ArrayDeque<>();
	
	public Tsunami(int level) {
		super(level);
		depth = plugin.getConfig().getInt("tsunami.minimum_depth");
		damage = plugin.getConfig().getInt("tsunami.damage");
		radius = (int) (15 * level * plugin.getConfig().getDouble("tsunami.size"));
		height = level*3;
		removeWater = plugin.getConfig().getBoolean("tsunami.remove_water");
		volume = plugin.getConfig().getDouble("tsunami.volume");
		particleType = Particle.WATER_BUBBLE;
		liquid = Material.WATER;
		materials = new BlockData[] {Material.SAND.createBlockData(), Material.DIRT.createBlockData()};
		
		this.type = Disaster.TSUNAMI;
	}
	public void start(Location loc, Player p) {
		DestructionDisasterEvent event = new DestructionDisasterEvent(this, loc, level, p);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		this.loc = loc;
		ongoingDisasters.add(this);
		height += loc.getBlockY();
		world = loc.getWorld();
		current = level;
		final Random rand = new Random();
		DeathMessages.tsunamis.add(this);
		Tsunami obj = this;
		new RepeatingTask(plugin, 0, 3) {
			@Override
			public void run() {
				if (loc.getBlockY() < height) {
					loc.setY(loc.getBlockY()+1);
					BlockVector block = new BlockVector(loc.getX(), loc.getY(), loc.getZ());
					for (int x = -level; x < level; x++)
						for (int z = -level; z < level; z++) {
							Vector position = block.clone().add(new Vector(x, 0, z));
							Block b = world.getBlockAt(position.toLocation(world));
							if (block.distance(position) <= level && !b.getType().isSolid() && !Utils.isBlockBlacklisted(b.getType())) {
								if (Utils.isZoneProtected(b.getLocation())) continue;
								if (plugin.CProtect) Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
								b.setType(liquid);
								blocksDestroyed++;
							}
						}
				} else {
					BlockVector block = new BlockVector(loc.getX(), loc.getY(), loc.getZ());
					new RepeatingTask(plugin, 0, tickSpeed) {
						@Override
						public void run() {
							if (current >= radius) {
								if (removeWater) {
									cancel();
									current = 0;
									BlockVector block = new BlockVector(loc.getX(), loc.getY(), loc.getZ());
									new RepeatingTask(plugin, 100, 3) {
										@Override
										public void run() {
											if (current <= height) {
												BlockVector temp = new BlockVector(loc.getX(), loc.getY(), loc.getZ());
												for (int x = -level-3; x < level+3; x++)
													for (int z = -level-3; z < level+3; z++) {
														Vector position = temp.clone().add(new Vector(x, -current, z));
														Block b = world.getBlockAt(position.toLocation(world));
														if (Utils.isZoneProtected(b.getLocation())) continue;
														if (temp.distance(position) <= level+3 && b.getType() == liquid)
															b.setType(Material.AIR);
													}
											} else if (current > radius) {
												push = false;
												if (!fallingBlocks.isEmpty()) {
													Iterator<FallingBlock> it = fallingBlocks.iterator();
													while (it.hasNext())
														if (it.next().isDead())
															it.remove();
													return;
												}
												DeathMessages.tsunamis.remove(obj);
												ongoingDisasters.remove(obj);
												cancel();
												Metrics.incrementValue(Metrics.disasterDestroyedMap, type.getMetricsLabel(), blocksDestroyed);
												return;
											}
											current++;
											for (int x = -current; x < current; x++)
												for (int z = -current; z < current; z++) {
													Vector position = block.clone().add(new Vector(x, 0, z));
													Block b = world.getBlockAt(position.toLocation(world));
													if (Utils.isZoneProtected(b.getLocation())) continue;
													if (block.distance(position) >= (current - 1) && block.distance(position) <= current && b.getType() == liquid)
														b.setType(Material.AIR);
												}
										}
									};
								} else {
									if (!fallingBlocks.isEmpty()) {
										Iterator<FallingBlock> it = fallingBlocks.iterator();
										while (it.hasNext())
											if (it.next().isDead())
												it.remove();
										return;
									}
									DeathMessages.tsunamis.remove(obj);
									ongoingDisasters.remove(obj);
									cancel();
									Metrics.incrementValue(Metrics.disasterDestroyedMap, type.getMetricsLabel(), blocksDestroyed);
								}
								return;
							}
							current++;
							for (int x = -current; x < current; x++)
								for (int z = -current; z < current; z++) {
									Vector position = block.clone().add(new Vector(x, 0, z));
									Block b = world.getBlockAt(position.toLocation(world));
									if (block.distance(position) >= (current - 1) && block.distance(position) <= current && !Utils.isBlockBlacklisted(b.getType()) && !b.getType().isSolid()) {
										if (Utils.isZoneProtected(b.getLocation())) continue;
										if (plugin.CProtect) Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
										b.setType(liquid);
										world.spawnParticle(particleType, b.getLocation().clone().subtract(0,0.5,0), 5, .7, .5, .7, 0.1);
									}
								}
						}
					};
					cancel();
				}
				new RepeatingTask(plugin, 20, 10) {
					@Override
					public void run() {
						for (Entity e : loc.getWorld().getNearbyEntities(loc.clone().subtract(0, 15, 0), current, 15, current)) {
							if (Utils.isZoneProtected(e.getLocation())) continue;
							if (e.getLocation().getBlock().getType() == liquid) {
								e.setVelocity(new Vector(e.getLocation().getX() - loc.getX(), 0.7, e.getLocation().getZ() - loc.getZ()).normalize().multiply(0.5));
								if (e instanceof LivingEntity) ((LivingEntity) e).damage(damage);
								if (e instanceof Player) {
									if (rand.nextInt(2) != 1) continue;
									((Player) e).spawnParticle(particleType, e.getLocation(), 70, 1, 1.5, 1, 1);
									FallingBlock fb = e.getWorld().spawnFallingBlock(new Location(e.getWorld(), e.getLocation().getX()+rand.nextInt(8)-4, e.getLocation().getY()+rand.nextInt(5)+1, e.getLocation().getZ()+rand.nextInt(8)-4), materials[rand.nextInt(materials.length)]);
									fb.setMetadata("dd-fb", new FixedMetadataValue(plugin, "protected"));
									fallingBlocks.add(fb);
									CoreListener.fallingBlocks.put(fb.getUniqueId(), obj);
								}
							}
						}
						if (!push) cancel();
					}
				};
				new RepeatingTask(plugin, 0, 1) {
					@Override
					public void run() {
						for (Entity e : loc.getWorld().getNearbyEntities(loc.clone().subtract(0, 5, 0), current+20, 20, current+20))
							if (e instanceof Player) ((Player) e).playSound(e.getLocation(), Sound.WEATHER_RAIN_ABOVE, (float) (1*volume), 0.5F);
						if (!push) cancel();
					}
				};
			}
		};
	}
	public Location findAvailabePool(Location location) {
		if (hasXAmountLiquid(depth, location)) {
			for (int i=location.getBlockY(); i > 0; i--) {
				location.setY(i);
				if (location.getBlock().getType() == liquid) break;
			}
			if (!Utils.isZoneProtected(location))
				return location;
		}
		for (int x=-2; x < 3; x++)
			for (int z=-2; z < 3; z++) {
				Location temp = location.getWorld().getChunkAt(location.getChunk().getX()+x, location.getChunk().getZ()+z).getBlock(5, location.getBlockY(), 5).getLocation();
				if (hasXAmountLiquid(depth, temp)) {
					for (int i=temp.getBlockY(); i > 0; i--) {
						temp.setY(i);
						if (temp.getBlock().getType() == liquid) {
							temp.setY(temp.getY()+1);
							break;
						}
					}
					if (!Utils.isZoneProtected(location))
						return temp;
				}
			}
		return null;
	}
	private boolean hasXAmountLiquid(int x, Location location) {
		int amount = 0;
		Block b = location.getBlock();
		for (int i=location.getBlockY(); i > 0; i--) {
			if (b.getType() == liquid)
				amount++;
			if (amount >= x)
				return true;
			b = b.getRelative(BlockFace.DOWN);
		}
		return false;
	}
	public Location findApplicableLocation(Location temp, Player p) {
		temp = findAvailabePool(temp.clone());
		if (temp == null || temp.getBlockY() < type.getMinHeight())
			return null;
		return temp;
	}
	public void startAdjustment(Location loc, Player p) {
		Location temp = findAvailabePool(loc.clone());
		if (temp != null) {
			start(temp, p);
			return;
		}
		if (ignoreOcean)
			start(loc, p);
	}
	public Location getLocation() {
		return loc;
	}
	public int getCurrent() {
		return current;
	}
	public int getRadius() {
		return radius;
	}
	public void setRadius(int radius) {
		this.radius = radius;
	}
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}
	public double getDamage() {
		return damage;
	}
	public void setDamage(double damage) {
		this.damage = damage;
	}
	public int getTickSpeed() {
		return tickSpeed;
	}
	public void setTickSpeed(int tickSpeed) {
		this.tickSpeed = tickSpeed;
	}
	public boolean isRemoveWater() {
		return removeWater;
	}
	public void setRemoveWater(boolean removeWater) {
		this.removeWater = removeWater;
	}
	public Particle getParticleType() {
		return particleType;
	}
	public void setParticleType(Particle particleType) {
		this.particleType = particleType;
	}
	public boolean isIgnoreOcean() {
		return ignoreOcean;
	}
	public void setIgnoreOcean(boolean ignoreOcean) {
		this.ignoreOcean = ignoreOcean;
	}
	public Material getLiquid() {
		return liquid;
	}
	public void setLiquid(Material liquid) {
		this.liquid = liquid;
	}
	public BlockData[] getMaterials() {
		return materials;
	}
	public void setMaterials(BlockData[] materials) {
		this.materials = materials;
	}
	public int getDepth() {
		return depth;
	}
	public void setDepth(int depth) {
		this.depth = depth;
	}
}
