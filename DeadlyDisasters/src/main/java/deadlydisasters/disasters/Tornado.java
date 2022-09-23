package deadlydisasters.disasters;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import deadlydisasters.disasters.events.DestructionDisaster;
import deadlydisasters.disasters.events.DestructionDisasterEvent;
import deadlydisasters.listeners.CoreListener;
import deadlydisasters.listeners.DeathMessages;
import deadlydisasters.utils.Metrics;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public class Tornado extends DestructionDisaster {
	
	private double speed,size,pullForce,height,particles,yVelocity = 0.35;
	private int max_blocks,time,width,pickupRange,particleCount;
	private int blocksDestroyed;
	private World world;
	private boolean CP;
	private Particle particleType;
	private BlockData[] materials;
	
	public static Set<Material> bannedBlocks = new HashSet<>();
	
	public Set<Block> pickedUp = new HashSet<>();
	public Map<UUID, Integer> cooldownEntities = new ConcurrentHashMap<>();
	public Map<UUID, Integer> holdEntities = new ConcurrentHashMap<>();
	public Map<UUID, Double> velocityMap = new ConcurrentHashMap<>();
		
	public Tornado(int level) {
		super(level);
		switch (level) {
		default:
		case 1:
			time = 600;
			size = 30;
			break;
		case 2:
			time = 700;
			size = 40;
			break;
		case 3:
			time = 800;
			size = 50;
			break;
		case 4:
			time = 900;
			size = 60;
			break;
		case 5:
			time = 1000;
			size = 75;
			break;
		case 6:
			time = 1200;
			size = 90;
			break;
		}
		size = size * plugin.getConfig().getDouble("tornado.size");
		time = (int) (time * plugin.getConfig().getDouble("tornado.time_multiplier"));
		max_blocks = plugin.getConfig().getInt("tornado.max_entities.level "+level);
		speed = 3D * plugin.getConfig().getDouble("tornado.speed");
		particles = 1.0 * plugin.getConfig().getDouble("tornado.particleAmount");
		volume = (float) (0.33*level * plugin.getConfig().getDouble("tornado.volume"));
		CP = plugin.CProtect;
		particleType = Particle.CLOUD;
		pullForce = -(0.02*level);
		height = size+(level*7);
		width = plugin.getConfig().getInt("tornado.width");
		pickupRange = (int) (size * plugin.getConfig().getDouble("tornado.block_pickup_range"));
		particleCount = 10 - level;
		
		this.type = Disaster.TORNADO;
	}
	public void start(Location loc, Player p) {
		DestructionDisasterEvent event = new DestructionDisasterEvent(this, loc, level, p);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		ongoingDisasters.add(this);
		DeathMessages.tornados.add(this);
		this.p = p;
		this.loc = loc.clone().subtract(0,level*2+(6-level),0);
		world = loc.getWorld();
		spawnTornado();
	}
	public void spawnTornado() {
		Random rand = plugin.random;
		Vector vec = new Vector(rand.nextDouble()-0.5, 0, rand.nextDouble()-0.5).normalize().multiply(0.05);
		Vector velocity = new Vector(0, ((double) (level)) / 20, 0);
		Vector entityVel = velocity.clone().multiply(((double) (level))/5);
		FixedMetadataValue fixdata = new FixedMetadataValue(plugin, "protected");
		int cooldownTicks = 180 - (level*15);
		int holdTicks = 80 + (level*40);
		double growth = 1.0 / size;
		
		Set<UUID> entities = ConcurrentHashMap.newKeySet();
		
		for (Entity e : world.getNearbyEntities(loc.clone().add(0,size-5,0), size, size, size))
			if (e instanceof LivingEntity)
				cooldownEntities.put(e.getUniqueId(), rand.nextInt(cooldownTicks));
		BukkitTask[] task = new BukkitTask[1];
		task[0] = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				if (time <= 0) {
					task[0].cancel();
					return;
				}
				Iterator<Entry<UUID, Integer>> it = cooldownEntities.entrySet().iterator();
				while (it.hasNext()) {
					Entry<UUID, Integer> entry = it.next();
					if (entry.getValue() <= 0)
						it.remove();
					else
						entry.setValue(entry.getValue()-1);
				}
				it = holdEntities.entrySet().iterator();
				while (it.hasNext()) {
					Entry<UUID, Integer> entry = it.next();
					if (entry.getValue() <= 0) {
						cooldownEntities.putIfAbsent(entry.getKey(), rand.nextInt(cooldownTicks)+(cooldownTicks/2));
						it.remove();
					} else
						entry.setValue(entry.getValue()-1);
				}
			}
		}, 0, 1);
		Tornado instance = this;
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				if (time <= 0) {
					cancel();
					plugin.getServer().getScheduler().runTaskLater(plugin, () -> DeathMessages.tornados.remove(instance), 100);
					return;
				}
				time--;
				loc.add(vec);
				Iterator<UUID> it = entities.iterator();
				Set<UUID> tempList = new HashSet<>();
				while (it.hasNext()) {
					Entity e = Bukkit.getEntity(it.next());
					if (e == null)
						continue;
					double diff = e.getLocation().getY() - loc.getY();
					if (diff > height) {
						it.remove();
						continue;
					}
					Location temp = e.getLocation();
					if ((!(e instanceof LivingEntity) && temp.getX() < loc.getX()+diff && temp.getX() > loc.getX()-diff && temp.getZ() < loc.getZ()+diff && temp.getZ() > loc.getZ()-diff)
							|| (e instanceof LivingEntity && temp.getX() < loc.getX()+(diff/3) && temp.getX() > loc.getX()-(diff/3) && temp.getZ() < loc.getZ()+(diff/3) && temp.getZ() > loc.getZ()-(diff/3))) {
						e.setVelocity(new Vector((temp.getX() - loc.getX()), 0, (temp.getZ() - loc.getZ())).rotateAroundY(1.5).normalize().multiply(diff/width).multiply(speed+(rand.nextDouble()*2)).add(new Vector(0,velocityMap.get(e.getUniqueId()),0)));
						if (e instanceof Player) {
							((Player) e).spawnParticle(particleType, temp.add(0,1,0), 30, 1, 1, 1, 0.3);
							Location area = e.getLocation().clone().add(e.getVelocity());
							if (!pickedUp.contains(area.getBlock()) && area.getBlock().getType().isSolid() && !Utils.isBlockBlacklisted(area.getBlock().getType()) && !Utils.isZoneProtected(area))
								tempList.add(addBlock(area.getBlock(), fixdata, pullForce, rand));
							area.add(0,1,0);
							if (!pickedUp.contains(area.getBlock()) && area.getBlock().getType().isSolid() && !Utils.isBlockBlacklisted(area.getBlock().getType()) && !Utils.isZoneProtected(area))
								tempList.add(addBlock(area.getBlock(), fixdata, pullForce, rand));
						}
						if (rand.nextDouble() < particles)
							world.spawnParticle(particleType, temp, particleCount, 1, 1, 1, 0.3, null, true);
					} else {
						if (e instanceof LivingEntity) {
							if (e.getVelocity().getY() > 0.4)
								e.setVelocity(e.getVelocity().add(new Vector(temp.getX() - loc.getX(), 0, temp.getZ() - loc.getZ()).normalize().multiply(pullForce).multiply((size+1-temp.distance(loc))*growth)));
							else
								e.setVelocity(e.getVelocity().add(new Vector(temp.getX() - loc.getX(), 0, temp.getZ() - loc.getZ()).normalize().multiply(pullForce).add(entityVel).multiply((size+1-temp.distance(loc))*growth)));
							Location area = e.getLocation().clone().add(e.getVelocity().clone().setY(0.3));
							if (!pickedUp.contains(area.getBlock()) && area.getBlock().getType().isSolid() && !Utils.isBlockBlacklisted(area.getBlock().getType()) && !Utils.isZoneProtected(area))
								tempList.add(addBlock(area.getBlock(), fixdata, pullForce, rand));
							area.add(0,1,0);
							if (!pickedUp.contains(area.getBlock()) && area.getBlock().getType().isSolid() && !Utils.isBlockBlacklisted(area.getBlock().getType()) && !Utils.isZoneProtected(area))
								tempList.add(addBlock(area.getBlock(), fixdata, pullForce, rand));
						} else
							e.setVelocity(new Vector(temp.getX() - loc.getX(), 0, temp.getZ() - loc.getZ()).normalize().multiply(pullForce*3).add(velocity));
					}
				}
				entities.addAll(tempList);
				if (entities.size() < max_blocks) {
					for (int c=0; c < level*4; c++)  {
						Location spot = loc.clone().add(rand.nextInt((int) (pickupRange*2))-pickupRange, rand.nextInt((int) (size/2))+(size/3), rand.nextInt((int) (pickupRange*2))-pickupRange);
						for (int i=1; i < 40; i++) {
							Block b = spot.getBlock();
							if (!pickedUp.contains(b) && b.getType().isSolid() && !bannedBlocks.contains(b.getType()) && !Utils.isBlockBlacklisted(b.getType()) && !Utils.isZoneProtected(spot)) {
								entities.add(addBlock(b, fixdata, pullForce, rand));
								break;
							}
							spot.setY(spot.getY()-i);
						}
					}
				}
			}
		};
		new RepeatingTask(plugin, 0, 10) {
			@Override
			public void run() {
				if (time <= 0 || Utils.isZoneProtected(loc)) {
					time = 0;
					ongoingDisasters.remove(instance);
					cancel();
					Metrics.incrementValue(Metrics.disasterDestroyedMap, type.getMetricsLabel(), blocksDestroyed);
					return;
				}
				entities.clear();
				
				Location forward = loc.clone().add(vec.clone().multiply(2));
				if (forward.clone().add(0,level*2+(6-level)+1,0).getBlock().getType().isSolid())
					loc.setY(loc.getY()+1);
				else if (forward.clone().add(0,level*2+(6-level)-1,0).getBlock().getType() == Material.AIR)
					loc.setY(loc.getY()-1);
				forward = loc.clone().add(vec);
				for (int y=(int) size; y >= 0; y--)
					for (int x=-y/5; x < y/5; x++) {
						Block b = forward.clone().add(new Vector(vec.getZ(), 0, -vec.getX()).normalize().multiply(x).add(new Vector(0,y,0))).getBlock();
						if (!pickedUp.contains(b) && b.getType().isSolid() && !bannedBlocks.contains(b.getType()) && !Utils.isBlockBlacklisted(b.getType()) && !Utils.isZoneProtected(b.getLocation()))
							addBlock(b, fixdata, pullForce, rand);
					}
				for (Entity e : world.getNearbyEntities(loc.clone().add(0,size-5,0), size, size, size)) {
					if (!(entities.size() > max_blocks && !(e instanceof LivingEntity)) && !cooldownEntities.containsKey(e.getUniqueId()) && !(e instanceof Player && ((Player) e).isFlying())) {
						entities.add(e.getUniqueId());
						holdEntities.putIfAbsent(e.getUniqueId(), holdTicks);
						velocityMap.putIfAbsent(e.getUniqueId(), rand.nextDouble()/2);
					}
					if (e instanceof Player) {
						Location temp = e.getLocation();
						((Player) e).playSound(temp.clone().add(new Vector(temp.getX() - loc.getX(), -3, temp.getZ() - loc.getZ()).normalize().multiply(-3)),
								Sound.WEATHER_RAIN_ABOVE, (float) ((2 - (0.02 * loc.distance(temp.subtract(0,temp.getY()-loc.getY(),0))))*volume), 0.5F);
					}
				}
				plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
					@Override
					public void run() {
						Iterator<Entry<UUID, Double>> iterator = velocityMap.entrySet().iterator();
						while (iterator.hasNext())
							if (!entities.contains(iterator.next().getKey()))
								iterator.remove();
					}
				});
			}
		};
	}
	private UUID addBlock(Block b, FixedMetadataValue fixdata, double force, Random rand) {
		if (CP) Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
		BlockData mat = null;
		if (materials == null)
			mat = b.getBlockData();
		else
			mat = materials[rand.nextInt(materials.length)];
		FallingBlock fb = world.spawnFallingBlock(b.getLocation().clone().add(0.5,0.5,0.5), mat);
		fb.setVelocity(new Vector(fb.getLocation().getX() - loc.getX(), -0.5, fb.getLocation().getZ() - loc.getZ()).normalize().multiply(force));
		fb.setDropItem(false);
		fb.setHurtEntities(true);
		fb.setMetadata("dd-fb", fixdata);
		if (b.getState() instanceof InventoryHolder)
			CoreListener.addBlockInventory(fb, ((InventoryHolder) b.getState()).getInventory().getContents());
		b.setType(Material.AIR);
		blocksDestroyed++;
		velocityMap.put(fb.getUniqueId(), rand.nextDouble()/2);
		pickedUp.add(b);
		return fb.getUniqueId();
	}
	public Location findApplicableLocation(Location temp, Player p) {
		temp = Utils.getBlockBelow(temp).getLocation();
		if (temp.getBlockY() < type.getMinHeight())
			return null;
		return temp;
	}
	public void startAdjustment(Location loc, Player p) {
		start(Utils.getBlockBelow(loc).getLocation(), p);
	}
	public boolean isEntityInvolved(UUID uuid) {
		return (cooldownEntities.containsKey(uuid) || holdEntities.containsKey(uuid));
	}
	public double getSpeed() {
		return speed;
	}
	public void setSpeed(double speed) {
		this.speed = speed;
	}
	public int getMaxBlocks() {
		return max_blocks;
	}
	public void setMaxBlocks(int max_blocks) {
		this.max_blocks = max_blocks;
	}
	public int getTime() {
		return time;
	}
	public void setTime(int time) {
		this.time = time;
	}
	public double getSize() {
		return size;
	}
	public void setSize(double size) {
		this.size = size;
	}
	public double getPullForce() {
		return pullForce;
	}
	public void setPullForce(double pullForce) {
		this.pullForce = pullForce;
	}
	public double getHeight() {
		return height;
	}
	public void setHeight(double height) {
		this.height = height;
	}
	public Particle getParticleType() {
		return particleType;
	}
	public void setParticleType(Particle particleType) {
		this.particleType = particleType;
	}
	public BlockData[] getMaterials() {
		return materials;
	}
	public void setMaterials(Material[] temp) {
		materials = new BlockData[temp.length];
		for (int i=0; i < temp.length; i++)
			materials[i] = temp[i].createBlockData();
	}
	public double getParticles() {
		return particles;
	}
	public void setParticles(double particles) {
		this.particles = particles;
	}
	public double getyVelocity() {
		return yVelocity;
	}
	public void setyVelocity(double yVelocity) {
		this.yVelocity = yVelocity;
	}
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
}
