package com.github.jewishbanana.deadlydisasters.events.disasters;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

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
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.events.DestructionDisaster;
import com.github.jewishbanana.deadlydisasters.events.DestructionDisasterEvent;
import com.github.jewishbanana.deadlydisasters.events.Disaster;
import com.github.jewishbanana.deadlydisasters.listeners.DeathMessages;
import com.github.jewishbanana.deadlydisasters.utils.Metrics;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class Tsunami extends DestructionDisaster implements Listener {
	
	private int depth;
	private int current,radius,height;
	private boolean clean,stopPush;
	private double damage;
	private int tickSpeed = 2;
	private boolean removeWater;
	private Particle particleType;
	private boolean ignoreOcean;
	private Material liquid;
	private BlockData[] materials;
	private int blocksDestroyed;
	private Map<Block, Integer> waterBlocks = new HashMap<>();
	private Map<Integer, Queue<Block>> blocksMap = new HashMap<>();
	private Set<Block> cleanMap = new LinkedHashSet<>();
	private Queue<FallingBlock> debris = new ArrayDeque<>();
	
	public Tsunami(int level, World world) {
		super(level, world);
		depth = configFile.getInt("tsunami.minimum_depth");
		damage = configFile.getInt("tsunami.damage");
		radius = (int) (15 * level * configFile.getDouble("tsunami.size"));
		height = level*3;
		removeWater = configFile.getBoolean("tsunami.remove_water");
		volume = configFile.getDouble("tsunami.volume");
		particleType = VersionUtils.getWaterBubble();
		liquid = Material.WATER;
		materials = new BlockData[] {Material.SAND.createBlockData(), Material.DIRT.createBlockData()};
		
		this.type = Disaster.TSUNAMI;
	}
	public void start(Location location, Player p) {
		DestructionDisasterEvent event = new DestructionDisasterEvent(this, location, level, p);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		this.loc = location;
		ongoingDisasters.add(this);
		world = loc.getWorld();
		current = level;
		final Random rand = new Random();
		DeathMessages.tsunamis.add(this);
		Tsunami obj = this;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		int[] tick = {0, 0};
		final int lvlSquared = level * level;
		final Location pushLoc = loc.clone();
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				tick[0]++;
				Queue<Block> list = new ArrayDeque<>();
				blocksMap.put(tick[0], list);
				if (tick[0] < height) {
					BlockVector block = new BlockVector(loc.getX(), loc.getY(), loc.getZ());
					for (int x = -level; x < level; x++)
						for (int z = -level; z < level; z++) {
							Vector position = block.clone().add(new Vector(x, 0, z));
							Block b = world.getBlockAt(position.toLocation(world));
							if (b.getType() != Material.AIR || block.distanceSquared(position) > lvlSquared)
								continue;
							list.add(b);
							waterBlocks.put(b, tick[0]);
						}
					loc.setY(loc.getY()+1);
					return;
				}
				for (int i=tick[0]; i < radius+tick[0]+1; i++)
					blocksMap.put(i, new ArrayDeque<>());
				BlockVector block = new BlockVector(loc.getX(), loc.getY(), loc.getZ());
				for (int x = -radius; x < radius; x++)
					for (int z = -radius; z < radius; z++) {
						Vector position = block.clone().add(new Vector(x, 0, z));
						Block b = world.getBlockAt(position.toLocation(world));
						double pos = block.distance(position);
						if (b.getType() != Material.AIR || pos > radius)
							continue;
						int j = ((int) pos)+tick[0];
						blocksMap.get(j).add(b);
						waterBlocks.put(b, j);
					}
				cancel();
				new RepeatingTask(plugin, 0, tickSpeed) {
					@Override
					public void run() {
						current = tick[1];
						if (tick[1] >= tick[0]+radius) {
							cancel();
							clean = true;
							if (!removeWater) {
								HandlerList.unregisterAll(obj);
								DeathMessages.tsunamis.remove(obj);
								ongoingDisasters.remove(obj);
								Metrics.incrementValue(Metrics.disasterDestroyedMap, type.getMetricsLabel(), blocksDestroyed);
								return;
							}
							new RepeatingTask(plugin, 80, 3) {
								@Override
								public void run() {
									current = tick[1];
									if (tick[1] <= 0) {
										cancel();
										new RepeatingTask(plugin, 0, 1) {
											@Override
											public void run() {
												if (cleanMap.isEmpty()) {
													cancel();
													HandlerList.unregisterAll(obj);
													return;
												}
												Iterator<Block> it = cleanMap.iterator();
												for (int i=0; i < 30; i++) {
													if (!it.hasNext())
														return;
													Block b = it.next();
													if (b.isLiquid() && !Utils.isZoneProtected(b.getLocation()))
														b.setType(Material.AIR);
													it.remove();
												}
											}
										};
										DeathMessages.tsunamis.remove(obj);
										ongoingDisasters.remove(obj);
										Metrics.incrementValue(Metrics.disasterDestroyedMap, type.getMetricsLabel(), blocksDestroyed);
										stopPush = true;
										return;
									}
									for (Block b : blocksMap.get(tick[1]))
										if (b.isLiquid())
											b.setType(Material.AIR);
									tick[1]--;
								}
							};
							return;
						}
						tick[1]++;
						for (Block b : blocksMap.get(tick[1])) {
							if (Utils.passStrengthTest(b.getType()) || Utils.isZoneProtected(b.getLocation()))
								continue;
							b.setType(liquid);
							blocksDestroyed++;
							if (plugin.random.nextInt(tick[1]) == 0)
								world.spawnParticle(particleType, b.getLocation().clone().subtract(0,0.5,0), 5, .7, .5, .7, 0.1);
						}
					}
				};
				final double vol = 2.0 / (radius+31);
				new RepeatingTask(plugin, 20, 10) {
					@Override
					public void run() {
						if (stopPush) {
							cancel();
							return;
						}
						int over = loc.getBlockY(), under = pushLoc.getBlockY()-level;
						for (Entity e : loc.getWorld().getNearbyEntities(pushLoc, tick[1]+25, level*2, tick[1]+25)) {
							Location entityLoc = e.getLocation();
							double dist = entityLoc.distance(pushLoc);
							if (dist <= tick[1] && entityLoc.getBlockY() <= over && entityLoc.getBlockY() >= under) {
								if (!isEntityTypeProtected(e) && entityLoc.getBlock().getType() == liquid) {
									e.setVelocity(new Vector(entityLoc.getX() - loc.getX(), 0.7, entityLoc.getZ() - loc.getZ()).normalize().multiply(0.5));
									if (e instanceof LivingEntity)
										Utils.damageEntity((LivingEntity) e, damage, "dd-tsunamideath", false, DamageCause.DROWNING);
									if (e instanceof Player) {
										((Player) e).spawnParticle(particleType, entityLoc, 30, 1, 1.5, 1, 1);
										Location spawn = new Location(e.getWorld(), entityLoc.getX()+rand.nextInt(8)-4, entityLoc.getY()+rand.nextInt(5)+1, entityLoc.getZ()+rand.nextInt(8)-4);
										if (spawn.getBlock().isLiquid()) {
											FallingBlock fb = e.getWorld().spawnFallingBlock(spawn, materials[rand.nextInt(materials.length)]);
											fb.setVelocity(Utils.getVectorTowards(pushLoc, entityLoc).multiply(0.5));
											fb.setMetadata("dd-fb", new FixedMetadataValue(plugin, "protected"));
											debris.add(fb);
										}
									}
								} else if (e instanceof Player) {
									if (dist < tick[0]-15) {
										Vector dir = Utils.getVectorTowards(pushLoc, entityLoc);
										Location spawn = pushLoc.clone().add(dir.clone().multiply(dist-6.0));
										spawn.setY(entityLoc.getY()+rand.nextInt(5)-1);
										if (spawn.getBlock().isLiquid()) {
											FallingBlock fb = e.getWorld().spawnFallingBlock(spawn.add(new Vector(dir.getZ(), 0, -dir.getX()).multiply(rand.nextInt(8)-4)), materials[rand.nextInt(materials.length)]);
											fb.setVelocity(dir.multiply(0.75).setY(0.2));
											((Player) e).spawnParticle(particleType, spawn, 20, 2, 1.5, 2, 1);
											fb.setMetadata("dd-fb", new FixedMetadataValue(plugin, "protected"));
											debris.add(fb);
										}
									} else {
										Vector dir = Utils.getVectorTowards(pushLoc, entityLoc);
										Location spawn = pushLoc.clone().add(dir.clone().multiply(Math.max(tick[1]-(height*2), 1)));
										spawn.setY(pushLoc.getY()+((loc.getY()-pushLoc.getY())/3.0*2.0));
										int offset = Math.min(Math.max(tick[1]-10, 1), 15);
										for (int i=0; i < 3; i++) {
											spawn.add(new Vector(dir.getZ(), 0, -dir.getX()).multiply(rand.nextInt(offset*2)-offset));
											if (spawn.getBlock().isLiquid()) {
												FallingBlock fb = e.getWorld().spawnFallingBlock(spawn, materials[rand.nextInt(materials.length)]);
												fb.setVelocity(dir.multiply(0.75).setY(0.2));
												((Player) e).spawnParticle(particleType, spawn, 20, 2, 1.5, 2, 1);
												fb.setMetadata("dd-fb", new FixedMetadataValue(plugin, "protected"));
												debris.add(fb);
											}
										}
									}
								}
							} else if (e instanceof Player) {
								Vector dir = Utils.getVectorTowards(pushLoc, entityLoc);
								Location spawn = pushLoc.clone().add(dir.clone().multiply(Math.max(tick[1]-(height*2), 1)));
								spawn.setY(pushLoc.getY()+((loc.getY()-pushLoc.getY())/3.0*2.0));
								int offset = Math.min(Math.max(tick[1]-10, 1), 15);
								spawn.add(new Vector(dir.getZ(), 0, -dir.getX()).multiply(rand.nextInt(offset*2)-offset));
								if (spawn.getBlock().isLiquid()) {
									FallingBlock fb = e.getWorld().spawnFallingBlock(spawn, materials[rand.nextInt(materials.length)]);
									fb.setVelocity(dir.multiply(0.75).setY(0.3));
									((Player) e).spawnParticle(particleType, spawn, 20, 2, 1.5, 2, 1);
									fb.setMetadata("dd-fb", new FixedMetadataValue(plugin, "protected"));
									debris.add(fb);
								}
							}
						}
						for (Player p : loc.getWorld().getPlayers()) {
							double dist = p.getLocation().distance(pushLoc);
							if (dist <= tick[1]+30)
								p.playSound(p.getLocation().add(Utils.getVectorTowards(p.getLocation(), loc).multiply(4.0)), Sound.WEATHER_RAIN_ABOVE, (float) ((vol*(radius-(dist-tick[1])))*volume), 0.5F);
						}
					}
				};
				new RepeatingTask(plugin, 0, 2) {
					@Override
					public void run() {
						if (stopPush) {
							cancel();
							return;
						}
						Iterator<FallingBlock> it = debris.iterator();
						while (it.hasNext()) {
							FallingBlock fb = it.next();
							if (fb == null || fb.isDead()) {
								it.remove();
								continue;
							}
							for (Entity e : world.getNearbyEntities(fb.getLocation().add(.5,.5,.5), .8, .8, .8))
								if (e instanceof LivingEntity && !isEntityTypeProtected(e)) {
									if (e instanceof Player) {
										if (Utils.isPlayerImmune((Player) e))
											continue;
									}
									e.setVelocity(fb.getVelocity().multiply(2.0));
									Utils.damageEntity((LivingEntity) e, damage, "dd-tsunamideath", false, DamageCause.DROWNING);
								}
						}
					}
				};
			}
		};
	}
	@EventHandler
	public void onWaterSpread(BlockFromToEvent e) {
		if (waterBlocks.containsKey(e.getBlock()) || cleanMap.contains(e.getBlock())) {
			if (clean) {
				cleanMap.add(e.getToBlock());
				return;
			}
			int i = waterBlocks.get(e.getBlock());
			blocksMap.get(i).add(e.getToBlock());
			waterBlocks.put(e.getToBlock(), i);
		}
	}
	public Location findAvailabePool(Location location) {
		if (hasXAmountLiquid(depth, location)) {
			for (int i=location.getBlockY(); i > 0; i--) {
				location.setY(i);
				if (location.getBlock().getType() == liquid)
					break;
			}
			if (!Utils.isZoneProtected(location))
				return location.add(0,1,0);
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
						return temp.add(0,1,0);
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
