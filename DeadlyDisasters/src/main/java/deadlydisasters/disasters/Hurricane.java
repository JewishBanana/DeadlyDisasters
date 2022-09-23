package deadlydisasters.disasters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import deadlydisasters.disasters.events.DestructionDisaster;
import deadlydisasters.disasters.events.DestructionDisasterEvent;
import deadlydisasters.disasters.events.DisasterEvent;
import deadlydisasters.listeners.CoreListener;
import deadlydisasters.listeners.DeathMessages;
import deadlydisasters.utils.Metrics;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public class Hurricane extends DestructionDisaster {
	
	public World world;
	private int time,size,lightning;
	private double lvl,blockForce,minForce,maxForce;
	private Particle particle;
	private int blocksDestroyed;
	
	public static Set<Biome> oceans = new HashSet<>();
	
	public Map<FallingBlock,Block> regenPreState = new HashMap<>();
	public Map<Block,Block> regenStates = new HashMap<>();
	public Map<Block,Material[]> fallenBlocks = new LinkedHashMap<>();
	
	public Hurricane(int level) {
		super(level);
		size = (int) (level*60 * plugin.getConfig().getDouble("hurricane.size"));
		time = plugin.getConfig().getInt("hurricane.time") * 20;
		lightning = (int) (((7-level)*10) * plugin.getConfig().getDouble("hurricane.lightning_frequency"));
		volume = (float) (0.33*level * plugin.getConfig().getDouble("hurricane.volume"));
		blockForce = plugin.getConfig().getDouble("hurricane.block_break_force");
		minForce = (double) level / 100.0;
		maxForce = (double) level / 50.0;
		particle = Particle.CLOUD;
		
		this.type = Disaster.HURRICANE;
	}
	@Override
	public void start(Location loc, Player p) {
		DestructionDisasterEvent event = new DestructionDisasterEvent(this, loc, level, p);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		this.loc = loc;
		this.world = loc.getWorld();
		lvl = (double) level;
		DeathMessages.hurricanes.add(this);
		ongoingDisasters.add(this);
		startHurricane();
	}
	public void startHurricane() {
		Random rand = plugin.random;
		Vector vel = new Vector(rand.nextDouble()-0.5, 0, rand.nextDouble()-0.5).normalize().setY((1.1/6.0)*lvl);
		Vector move = vel.clone().multiply((0.2/6)*lvl).setY(0);
		double[] wind = {0, lvl * 0.0001, minForce, maxForce, 0};
		int[] ticker = {0};
		int divided = size/20;
		double speedX = (200/6)*lvl;
		boolean CP = plugin.CProtect;
		Map<Entity,Double> entities = new ConcurrentHashMap<>();
		Map<Entity,Double> storage = new HashMap<>();
		FixedMetadataValue fixdata = new FixedMetadataValue(plugin, "protected");
		
		RepeatingTask windTask = new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				if (time <= 0)
					return;
				loc.add(move.clone().multiply(wind[4]));
				world.spawnParticle(particle, loc, (int) (lvl*10*wind[4]), 1, 1, 1, 0.1);
				wind[0] += wind[1];
				if ((wind[1] > 0 && wind[0] > wind[3]) || (wind[1] < 0 && wind[0] < wind[2]))
					wind[1] *= -1;
				Vector windSpeed = vel.clone().multiply(wind[0]*wind[4]);
				Vector entityWindSpeed = windSpeed.clone().setY(windSpeed.getY()/2).multiply(10);
				Vector offset = windSpeed.clone().multiply(-1).setY(0);
				Iterator<Entry<Entity, Double>> it = entities.entrySet().iterator();
				while (it.hasNext()) {
					Entry<Entity, Double> entry = it.next();
					Entity e = entry.getKey();
					double dist = size - entry.getValue();
					if (e instanceof LivingEntity) {
						if (e instanceof Player) {
							Location pLoc = e.getLocation();
							double reverse = Math.max(entry.getValue() / 2, 15*((6-lvl)/2));
							double speed = Math.min(wind[0]*dist*10, 50);
							Vector tempVec = offset.clone().multiply(speed*10);
							Player p = (Player) e;
							for (int i=0; i < (dist*lvl)*wind[4]; i++)
								p.spawnParticle(particle, (rand.nextDouble()*reverse-(reverse/2))+pLoc.getX()+tempVec.getX(), (rand.nextDouble()*reverse-(reverse/4))+pLoc.getY(), (rand.nextDouble()*reverse-(reverse/2))+pLoc.getZ()+tempVec.getZ(), 0, windSpeed.getX(), 0.001, windSpeed.getZ(), speed);
							if (p.isFlying()) continue;
						}
						e.setVelocity(e.getVelocity().setY(Math.min(e.getVelocity().getY(), 0.3)).add(windSpeed.clone().multiply(dist/speedX)));
					} else
						e.setVelocity(entityWindSpeed);
				}
				for (int i=ticker[0]; i < ticker[0]+divided; i++)
					for (Entity e : world.getNearbyEntities(loc, i, i, i))
						if (!storage.containsKey(e) && e.getLocation().getY() > 60 && !Utils.isZoneProtected(e.getLocation()))
							storage.put(e, loc.distance(e.getLocation()));
				ticker[0] += divided;
				if (ticker[0] >= size) {
					ticker[0] = 0;
					entities.clear();
					entities.putAll(storage);
					storage.clear();
				}
				if (wind[4] < 1)
					wind[4] += 0.0015;
			}
		};
		double minVolume = 2.0/size;
		double roofDist = (size/4)*3;
		BukkitTask[] id = new BukkitTask[1];
		BukkitScheduler scheduler = plugin.getServer().getScheduler();
		Hurricane obj = this;
		id[0] = scheduler.runTaskTimerAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				if (time <= 0) {
					if (!regenPreState.isEmpty()) {
						Iterator<Entry<FallingBlock, Block>> it = regenPreState.entrySet().iterator();
						while (it.hasNext())
							if (it.next().getKey().isDead())
								it.remove();
						return;
					}
					DisasterEvent.ongoingDisasters.remove(obj);
					Metrics.incrementValue(Metrics.disasterDestroyedMap, type.getMetricsLabel(), blocksDestroyed);
					id[0].cancel();
					windTask.cancel();
					DeathMessages.hurricanes.remove(obj);
					scheduler.runTask(plugin, new Runnable() {
						@Override
						public void run() {
							world.setStorm(false);
							world.setThundering(false);
						}
					});
					return;
				}
				if (!world.hasStorm()) {
					scheduler.runTask(plugin, new Runnable() {
						@Override
						public void run() {
							world.setStorm(true);
							world.setThunderDuration(time);
						}
					});
				}
				time -= 10;
				for (Map.Entry<Entity,Double> entry : entities.entrySet())
					if (entry.getKey() instanceof Player) {
						Player p = (Player) entry.getKey();
						Location temp = p.getLocation();
						p.playSound(temp.clone().add(new Vector(temp.getX() - loc.getX(), -2, temp.getZ() - loc.getZ()).normalize().multiply(-3)),
								Sound.WEATHER_RAIN_ABOVE, (float) (((minVolume*(size-entry.getValue()))*volume)*wind[4]), 0.5F);
						if ((size-entry.getValue()) > roofDist) {
							scheduler.runTask(plugin, new Runnable() {
								@Override
								public void run() {
									if (rand.nextInt(lightning) == 0)
										world.spawnEntity(world.getHighestBlockAt(temp.clone().add(rand.nextInt(6)-3, 0, rand.nextInt(6)-3)).getLocation().add(0,1,0), EntityType.LIGHTNING);
									if (wind[0] < blockForce)
										return;
									if (temp.clone().add(0,2,0).getBlock().getType().isSolid()) {
										Block b = temp.clone().add(0,2,0).getBlock();
										if (CP) Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
										FallingBlock fb = world.spawnFallingBlock(b.getLocation().clone().add(0.5,0.5,0.5), b.getBlockData());
										fb.setDropItem(false);
										fb.setHurtEntities(true);
										fb.setMetadata("dd-fb", fixdata);
										entities.put(fb, loc.distance(fb.getLocation()));
										if (b.getState() instanceof InventoryHolder)
											CoreListener.addBlockInventory(fb, ((InventoryHolder) b.getState()).getInventory().getContents());
										b.setType(Material.AIR);
										blocksDestroyed++;
									}
									temp.add(move.clone().multiply(-6)).add(0,2,0);
									int radius = 12-level;
									for (int i=0; i < ((size-entry.getValue())/10)/(7-level); i++) {
										Block b = temp.clone().add(rand.nextInt(radius)-(radius/2), rand.nextInt(radius/2)-(radius/4), rand.nextInt(radius)-(radius/2)).getBlock();
										if (!b.getType().isSolid() || Utils.isBlockBlacklisted(b.getType()) || Utils.isZoneProtected(b.getLocation()))
											continue;
										if (CP) Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
										FallingBlock fb = world.spawnFallingBlock(b.getLocation().clone().add(0.5,0.5,0.5), b.getBlockData());
										fb.setDropItem(false);
										fb.setHurtEntities(true);
										fb.setMetadata("dd-fb", fixdata);
										entities.put(fb, loc.distance(fb.getLocation()));
										if (b.getState() instanceof InventoryHolder)
											CoreListener.addBlockInventory(fb, ((InventoryHolder) b.getState()).getInventory().getContents());
										b.setType(Material.AIR);
										blocksDestroyed++;
									}
								}
							});
						}
					}
			}
		}, 0, 10);
	}
	public Location findApplicableLocation(Location temp, Player p) {
		temp = temp.add(0,7,0);
		if (temp.getBlockY() < type.getMinHeight())
			return null;
		return temp;
	}
	public void startAdjustment(Location loc, Player p) {
		start(Utils.getBlockBelow(loc).getLocation().add(0,7,0), p);
	}
	public void clear() {
		time = 0;
	}
	public int getTime() {
		return time;
	}
	public void setTime(int time) {
		this.time = time;
	}
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	public int getLightning() {
		return lightning;
	}
	public void setLightning(int lightning) {
		this.lightning = lightning;
	}
	public double getBlockForce() {
		return blockForce;
	}
	public void setBlockForce(double blockForce) {
		this.blockForce = blockForce;
	}
	public double getMinForce() {
		return minForce;
	}
	public void setMinForce(double minForce) {
		this.minForce = minForce;
	}
	public double getMaxForce() {
		return maxForce;
	}
	public void setMaxForce(double maxForce) {
		this.maxForce = maxForce;
	}
	public Particle getParticle() {
		return particle;
	}
	public void setParticle(Particle particle) {
		this.particle = particle;
	}
}
