package deadlydisasters.disasters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import deadlydisasters.disasters.events.WeatherDisaster;
import deadlydisasters.disasters.events.WeatherDisasterEvent;
import deadlydisasters.general.Main;
import deadlydisasters.general.WorldObject;
import deadlydisasters.listeners.CoreListener;
import deadlydisasters.listeners.DeathMessages;
import deadlydisasters.utils.Metrics;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public class MeteorShower extends WeatherDisaster {
	
	private MeteorShower me;
	private boolean night;
	private int max,tick_speed,cycle;
	public int smokeTime;
	private BukkitTask id;
	private long timeMemory;
	public double version;
	public FixedMetadataValue fixdata;
	public boolean customized;
	
	private int[] allowedTypes = {1, 2, 3};
	public Material normalMaterial;
	public Material explodingMaterial;
	public Material splittingMaterial;
	public Particle particle;
	public double explosionDamage = 10;
	public double splitImpact = 1.0;
	public double[] speeds = {1.0, 1.0, 1.0};
	public int[][] meteorSizes = {{2, 6},{2, 6},{2, 6}};
	private int spawnRate = 7;
	public int blocksDestroyed;
	
	public boolean spawnOres = true;
	
	public List<Meteor> list = new ArrayList<>();
	public Map<Location, List<Integer>> smoke = new HashMap<>();
		
	public MeteorShower(int level) {
		super(level);
		night = plugin.getConfig().getBoolean("meteorshowers.set_night");
		max = plugin.getConfig().getInt("meteorshowers.max_meteors");
		time = plugin.getConfig().getInt("meteorshowers.time.level "+this.level) * 20;
		delay = plugin.getConfig().getInt("meteorshowers.start_delay") * 20;
		tick_speed = plugin.getConfig().getInt("meteorshowers.tick_speed");
		smokeTime = plugin.getConfig().getInt("meteorshowers.smoke_time") * 20;
		volume = plugin.getConfig().getDouble("meteorshowers.volume");
		cycle = tick_speed;
		me = this;
		version = plugin.mcVersion;
		fixdata = new FixedMetadataValue(plugin, "protected");
		particle = Particle.SMOKE_LARGE;
		if (plugin.mcVersion >= 1.17)
			normalMaterial = Material.DEEPSLATE;
		else
			normalMaterial = Material.STONE;
		explodingMaterial = Material.MAGMA_BLOCK;
		if (plugin.mcVersion >= 1.17)
			splittingMaterial = Material.CALCITE;
		else
			splittingMaterial = Material.DIORITE;
		
		this.type = Disaster.METEORSHOWERS;
	}
	public void start(World world, Player p, boolean broadcastAllowed) {
		WeatherDisasterEvent event = new WeatherDisasterEvent(this, world, level, p);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		this.world = world;
		updateWeatherSettings();
		ongoingDisasters.add(this);
		meteorSizes[0][1] -= meteorSizes[0][0];
		meteorSizes[1][1] -= meteorSizes[1][0];
		meteorSizes[2][1] -= meteorSizes[2][0];
		for (int i=0; i < allowedTypes.length; i++)
			if (allowedTypes[i] == 0) {
				allowedTypes = Utils.removeElement(allowedTypes, i);
				i--;
			}
		if (broadcastAllowed && (boolean) WorldObject.findWorldObject(world).settings.get("event_broadcast"))
			Utils.broadcastEvent(level, "weather", this.type, world);
		DeathMessages.meteorshowers.add(this);
		if (night) timeMemory = world.getTime();
		final Random rand = new Random();
		id = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
			@Override
			public void run() {
				if (time <= 20)
					DeathMessages.meteorshowers.remove(me);
				if (time <= 0) {
					id.cancel();
					DeathMessages.meteorshowers.remove(me);
					for (Meteor e : list) e.removeBlocks();
					list.clear();
					return;
				} else time -= 20;
				if (time > 0 && rand.nextInt(spawnRate) == 0) {
					if (Bukkit.getOnlinePlayers().size() == 0) return;
					for (int i=0; i < max/3; i++) {
						Player temp = (Player) Bukkit.getOnlinePlayers().toArray()[rand.nextInt(Bukkit.getOnlinePlayers().size())];
						if (temp.getGameMode() == GameMode.CREATIVE || temp.getGameMode() == GameMode.SPECTATOR) continue;
						Location place = new Location(world, temp.getLocation().getBlockX()+(rand.nextInt(30)-15), temp.getLocation().getBlockY(), temp.getLocation().getBlockZ()+(rand.nextInt(30)-15));
						Location spawn = new Location(world, temp.getLocation().getBlockX()+(rand.nextInt(100)-50), Math.min(temp.getLocation().getBlockY()+70, 320), temp.getLocation().getBlockZ()+(rand.nextInt(100)-50));
						if (Utils.isZoneProtected(temp.getLocation()) || Utils.isZoneProtected(place))
							continue;
						list.add(new Meteor(spawn, place, allowedTypes[rand.nextInt(allowedTypes.length)], world, me, rand, 0));
						if (i >= Bukkit.getOnlinePlayers().size()-1) break;
					}
				}
			}
		}, delay, 20);
		new RepeatingTask(plugin, delay, 1) {
			@Override
			public void run() {
				long gameTime = world.getTime();
				if (time <= 0 && list.isEmpty()) {
					if (night && (gameTime < timeMemory-300 || gameTime > timeMemory+300)) {
						if (gameTime > timeMemory-8000 && gameTime < timeMemory+300) world.setTime(gameTime + 200);
						else world.setTime(gameTime - 200);
						if (DeathMessages.meteorshowers.stream().anyMatch(n -> n.getWorld().equals(world))) cancel();
						return;
					}
					ongoingDisasters.remove(me);
					cancel();
					Metrics.incrementValue(Metrics.disasterDestroyedMap, type.getMetricsLabel(), blocksDestroyed);
					return;
				}
				for (int i = list.size()/tick_speed*cycle; i > list.size()/tick_speed*(cycle-1); i--) list.get(i-1).tick(rand);
				if (list.size() == 1) list.get(0).tick(rand);
				if (cycle <= 1) cycle = tick_speed;
				else cycle--;
				Iterator<Entry<Location, List<Integer>>> iterator = smoke.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<Location, List<Integer>> entry = iterator.next();
					List<Integer> values = entry.getValue();
					int size = values.get(1);
					world.spawnParticle(particle, entry.getKey(), size*5, size, size, size, 0.001);
					if (values.get(0) <= 0) iterator.remove();
					else values.set(0, values.get(0)-1);
					entry.setValue(values);
				}
				if (night && (gameTime < 19700 || gameTime > 20300))
					if (gameTime > 8000 && gameTime < 20300) world.setTime(gameTime + 200);
					else world.setTime(gameTime - 200);
			}
		};
	}
	@Override
	public void clear() {
		time = 0;
	}
	public static void spawnMeteor(int type, Location spawn, Location target, Main plugin, int size) {
		MeteorShower instance = new MeteorShower(1);
		World world = spawn.getWorld();
		instance.world = world;
		ongoingDisasters.add(instance);
		Random rand = new Random();
		if (size <= 0)
			size = rand.nextInt(instance.meteorSizes[type-1][1])+instance.meteorSizes[type-1][0];
		Meteor meteor = new Meteor(spawn, target, type, world, instance, rand, size);
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				if (!meteor.exists) {
					cancel();
					ongoingDisasters.remove(instance);
					Metrics.incrementValue(Metrics.disasterDestroyedMap, Disaster.METEORSHOWERS.getMetricsLabel(), instance.blocksDestroyed);
					if (type == 1) {
						new RepeatingTask(plugin, 0, 1) {
							@Override
							public void run() {
								Iterator<Entry<Location, List<Integer>>> iterator = instance.smoke.entrySet().iterator();
								while (iterator.hasNext()) {
									Entry<Location, List<Integer>> entry = iterator.next();
									List<Integer> values = entry.getValue();
									int size = values.get(1);
									world.spawnParticle(instance.particle, entry.getKey(), size*5, size, size, size, 0.001);
									if (values.get(0) <= 0) iterator.remove();
									else values.set(0, values.get(0)-1);
									entry.setValue(values);
								}
								if (instance.smoke.isEmpty()) {
									cancel();
								}
							}
						};
					}
					return;
				}
				meteor.tick(rand);
			}
		};
	}
	public boolean isNight() {
		return night;
	}
	public void setNight(boolean night) {
		this.night = night;
	}
	public int getSmokeTime() {
		return smokeTime;
	}
	public void setSmokeTime(int smokeTime) {
		this.smokeTime = smokeTime;
	}
	public int getMax() {
		return max;
	}
	public void setMax(int max) {
		this.max = max;
	}
	public int[] getAllowedTypes() {
		return allowedTypes;
	}
	public void setAllowedTypes(int[] allowedTypes) {
		this.allowedTypes = allowedTypes;
	}
	public Material getNormalMaterial() {
		return normalMaterial;
	}
	public void setNormalMaterial(Material normalMaterial) {
		this.normalMaterial = normalMaterial;
		this.customized = true;
	}
	public Material getExplodingMaterial() {
		return explodingMaterial;
	}
	public void setExplodingMaterial(Material explodingMaterial) {
		this.explodingMaterial = explodingMaterial;
		this.customized = true;
	}
	public Material getSplittingMaterial() {
		return splittingMaterial;
	}
	public void setSplittingMaterial(Material splittingMaterial) {
		this.splittingMaterial = splittingMaterial;
		this.customized = true;
	}
	public Particle getParticle() {
		return particle;
	}
	public void setParticle(Particle particle) {
		this.particle = particle;
	}
	public double getExplosionDamage() {
		return explosionDamage;
	}
	public void setExplosionDamage(double explosionDamage) {
		this.explosionDamage = explosionDamage;
	}
	public double getSplitImpact() {
		return splitImpact;
	}
	public void setSplitImpact(double splitImpact) {
		this.splitImpact = splitImpact;
	}
	public boolean isSpawnOres() {
		return spawnOres;
	}
	public void setSpawnOres(boolean spawnOres) {
		this.spawnOres = spawnOres;
	}
	public double[] getSpeeds() {
		return speeds;
	}
	public void setSpeeds(double[] speeds) {
		this.speeds = speeds;
	}
	public int[][] getMeteorSizes() {
		return meteorSizes;
	}
	public void setMeteorSizes(int[][] meteorSizes) {
		this.meteorSizes = meteorSizes;
	}
	public int getSpawnRate() {
		return spawnRate;
	}
	public void setSpawnRate(int spawnRate) {
		this.spawnRate = spawnRate;
	}
}
class Meteor {
	private int type,size,depth,squared,offset;
	private BlockData bd;
	private FallingBlock[] blocks;
	private Vector vec,off;
	private MeteorShower classInstance;
	private double version,volume,speed;
	private boolean CP;
	private Material material;
	private Particle particle;
	public boolean exists = true;
	
	public Meteor(Location loc, Location dest, int type, World world, MeteorShower classInstance, Random rand, int tempSize) {
		this.type = type;
		this.size = tempSize;
		speed = classInstance.speeds[0];
		if (type == 1) {
			material = classInstance.normalMaterial;
			if (size <= 0)
				size = rand.nextInt(classInstance.meteorSizes[0][1])+classInstance.meteorSizes[0][0];
			this.depth = size * 500;
		} else if (type == 2) {
			material = classInstance.explodingMaterial;
			if (size <= 0)
				size = rand.nextInt(classInstance.meteorSizes[1][1])+classInstance.meteorSizes[1][0];
			this.depth = size * 100;
			speed = classInstance.speeds[1];
		} else {
			material = classInstance.splittingMaterial;
			if (size <= 0)
				size = rand.nextInt(classInstance.meteorSizes[2][1])+classInstance.meteorSizes[2][0];
			this.depth = size * 10;
			speed = classInstance.speeds[2];
		}
		this.classInstance = classInstance;
		this.CP = classInstance.plugin.CProtect;
		this.version = classInstance.plugin.mcVersion;
		this.volume = classInstance.volume;
		this.particle = classInstance.particle;
		if (classInstance.customized)
			bd = material.createBlockData();
		else {
			if (type == 1) {
				if (version >= 1.17) bd = Bukkit.createBlockData(Material.DEEPSLATE);
				else bd = Bukkit.createBlockData(Material.DEAD_BRAIN_CORAL_BLOCK);
			}
			else if (type == 2) {
				bd = Bukkit.createBlockData(material);
			} else {
				if (version >= 1.17 && material == Material.CALCITE) bd = Bukkit.createBlockData(Material.CALCITE);
				else bd = Bukkit.createBlockData(Material.DIORITE);
			}
		}
		this.vec = new Vector(loc.getX() - dest.getX(), loc.getY() - dest.getY(), loc.getZ() - dest.getZ()).normalize().multiply(-1).multiply(speed);
		this.squared = size * size;
		this.offset = (int) ((squared / Math.max(1, size/2)));
		off = new Vector(loc.getX() - dest.getX(), loc.getY() - dest.getY(), loc.getZ() - dest.getZ()).normalize().multiply(-1);
		
		List<FallingBlock> tempBlocks = new ArrayList<>();
		FallingBlock firstBlock = world.spawnFallingBlock(loc.clone().add(.5,0,.5), bd);
		firstBlock.setDropItem(false);
		((Entity) firstBlock).setGravity(false);
		firstBlock.setMetadata("dd-fb", new FixedMetadataValue(classInstance.plugin, "protected"));
		firstBlock.setMetadata("dd-md", new FixedMetadataValue(classInstance.plugin, "protected"));
		tempBlocks.add(firstBlock);
		CoreListener.fallingBlocks.put(firstBlock.getUniqueId(), classInstance);
		
		BlockVector block = new BlockVector(loc.getX(), loc.getY(), loc.getZ());
		for (double x = size; x > -size; x--)
			for (double y = size; y > -size; y--)
				for (double z = size; z > -size; z--) {
					Vector position = block.clone().add(new Vector(x, y, z));
					if (block.distanceSquared(position) > squared || block.distanceSquared(position) < squared-offset) continue;
					FallingBlock fb = world.spawnFallingBlock(position.toLocation(world).add(.5,0,.5), bd);
					fb.setDropItem(false);
					((Entity) fb).setGravity(false);
					fb.setMetadata("dd-fb", new FixedMetadataValue(classInstance.plugin, "protected"));
					fb.setMetadata("dd-md", new FixedMetadataValue(classInstance.plugin, "protected"));
					tempBlocks.add(fb);
					CoreListener.fallingBlocks.put(fb.getUniqueId(), classInstance);
				}
		blocks = tempBlocks.toArray(new FallingBlock[tempBlocks.size()]);
	}
	public void tick(Random rand) {
		if (blocks[0].isDead()) {
			final Meteor instance = this;
			depth = 0;
			Bukkit.getScheduler().runTaskLater(classInstance.plugin, new Runnable() {
				@Override
				public void run() {
					exists = false;
					if (classInstance.list.contains(instance))
						classInstance.list.remove(instance);
				}
			}, 1); //60
		}
		blocks[0].getWorld().spawnParticle(particle, blocks[0].getLocation(), size*2, size/2, size/2, size/2, 0.001, null, true);
		for (int i = 0; i < blocks.length; i++) {
			blocks[i].setVelocity(vec);
			for (int v=1; v < speed+2; v++) {
				Block b = blocks[i].getLocation().clone().add(off.clone().multiply(v)).getBlock();
				if (b.getType() != Material.AIR && !Utils.isBlockBlacklisted(b.getType()) && !Utils.isZoneProtected(b.getLocation())) {
					if (CP) Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
					b.setType(Material.AIR);
					classInstance.blocksDestroyed++;
					depth--;
				}
				b = b.getRelative(BlockFace.DOWN);
				if (b.getType() != Material.AIR && !Utils.isBlockBlacklisted(b.getType()) && !Utils.isZoneProtected(b.getLocation())) {
					if (CP) Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
					b.setType(Material.AIR);
					classInstance.blocksDestroyed++;
					depth--;
				}
			}
		}
		for (Entity e : blocks[0].getWorld().getNearbyEntities(blocks[0].getLocation(), size, size, size))
			if (e instanceof LivingEntity && !e.isDead()) {
				if (e instanceof Player && Utils.isPlayerImmune((Player) e))
					continue;
				if (Utils.rayTraceForSolidBlock(blocks[0].getLocation(), e.getLocation().clone().add(0,.5,0)))
					continue;
				e.setMetadata("dd-meteorcrush", classInstance.fixdata);
				((LivingEntity) e).setHealth(0);
			}
		for (Entity e : blocks[0].getWorld().getNearbyEntities(blocks[0].getLocation(), size*5, size*5, size*5))
			if (e instanceof Player) ((Player) e).playSound(blocks[0].getLocation(), Sound.BLOCK_FIRE_AMBIENT, (float) (1*volume), 2);
		if (depth <= 0) {
			Location loc = blocks[0].getLocation().clone();
			if (type == 1) {
				for (int i = 0; i < blocks.length; i++)
					blocks[i].remove();
				BlockVector block = new BlockVector(loc.getX(), loc.getY(), loc.getZ());
				boolean generateOres = classInstance.spawnOres;
				boolean newer = true;
				if (version < 1.17)
					newer = false;
				for (double x = size; x > -size; x--)
					for (double y = size; y > -size; y--)
						for (double z = size; z > -size; z--) {
							Vector position = block.clone().add(new Vector(x, y, z));
							if (block.distanceSquared(position) > squared) continue;
							Block b = loc.getWorld().getBlockAt(position.toLocation(loc.getWorld()));
							if (Utils.isBlockBlacklisted(b.getType()) || Utils.isZoneProtected(b.getLocation())) continue;
							if (CP) Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
							if (block.distanceSquared(position) > squared-offset) {
								b.setType(bd.getMaterial());
								continue;
							}
							if (!generateOres) {
								b.setType(material);
								continue;
							}
							int table = rand.nextInt(100);
							if (table > 14)
								b.setType(material);
							else if (table > 10)
								if (newer) b.setType(Material.DEEPSLATE_COPPER_ORE);
								else b.setType(Material.COAL_ORE);
							else if (table > 4)
								if (newer) b.setType(Material.DEEPSLATE_IRON_ORE);
								else b.setType(Material.IRON_ORE);
							else if (table > 0)
								if (newer) b.setType(Material.DEEPSLATE_GOLD_ORE);
								else b.setType(Material.GOLD_ORE);
							else
								if (newer) b.setType(Material.DEEPSLATE_DIAMOND_ORE);
								else b.setType(Material.DIAMOND_ORE);
							if (CP) Utils.getCoreProtect().logPlacement("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
						}
				classInstance.smoke.put(loc.clone(), Arrays.asList(classInstance.smokeTime, size/2));
			} else if (type == 2) {
				for (int i = 0; i < blocks.length; i++)
					blocks[i].remove();
				for (Entity e : loc.getWorld().getNearbyEntities(loc, size*6, size*6, size*6))
					if (e instanceof Player) ((Player) e).playSound(e.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, (float) (1*volume), 0.5F);
				int width = size*3, widthSquared = width * width, distOff = widthSquared / (width/2);
				loc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, loc, size/2, size*1.5, size*1.5, size*1.5, 0.01);
				BlockVector block = new BlockVector(loc.getX(), loc.getY(), loc.getZ());
				for (double x = width; x > -width; x--)
					for (double y = width; y > -width; y--)
						for (double z = width; z > -width; z--) {
							Vector position = block.clone().add(new Vector(x, y, z));
							double distance = block.distanceSquared(position);
							if (distance > widthSquared) continue;
							Block b = loc.getWorld().getBlockAt(position.toLocation(loc.getWorld()));
							if (b.getType() == Material.AIR || Utils.isBlockBlacklisted(b.getType()) || Utils.isZoneProtected(b.getLocation())) continue;
							if (CP) Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
							if (distance > widthSquared-distOff && rand.nextInt(3) == 1) {
								if (rand.nextInt(4) == 1) {
									b.setType(material);
									classInstance.blocksDestroyed++;
									if (CP) Utils.getCoreProtect().logPlacement("Deadly-Disasters", b.getLocation(), material, bd);
								}
								continue;
							}
							b.setType(Material.AIR);
							classInstance.blocksDestroyed++;
						}
				double damage = classInstance.explosionDamage;
				for (Entity e : loc.getWorld().getNearbyEntities(loc, width, width, width))
					if (e instanceof LivingEntity && !e.isDead()) {
						if (e instanceof Player && (((Player) e).getGameMode() == GameMode.CREATIVE || ((Player) e).getGameMode() == GameMode.SPECTATOR)) continue;
						LivingEntity entity = (LivingEntity) e;
						if (Utils.rayTraceForSolidBlock(loc, e.getLocation().clone().add(0,.5,0)))
							continue;
						Utils.damageEntity(entity, damage, "dd-meteorcrush", false);
					}
				classInstance.smoke.put(loc.clone().subtract(0,width,0), Arrays.asList(classInstance.smokeTime, width/2));
			} else {
				loc.subtract(.25,size,.25);
				double impact = classInstance.splitImpact;
				for (int i = 0; i < blocks.length; i++) {
					blocks[i].setGravity(true);
					Location current = blocks[i].getLocation();
					blocks[i].setVelocity(new Vector(current.getX() - loc.getX(), current.getY() - loc.getY(), current.getZ() - loc.getZ()).normalize().multiply(0.75).multiply(impact));
					blocks[i].removeMetadata("dd-md", classInstance.plugin);
				}
				loc.getWorld().spawnParticle(Particle.CLOUD, loc, size*6, size, size, size, 0.1);
				for (Entity e : loc.getWorld().getNearbyEntities(loc, size*6, size*6, size*6))
					if (e instanceof Player) ((Player) e).playSound(e.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, (float) (1*volume), 1);
				new RepeatingTask(classInstance.plugin, 0, 5) {
					@Override
					public void run() {
						World world = blocks[0].getWorld();
						for (int i = 0; i < blocks.length; i++) {
							if (blocks[i].isDead()) continue;
							for (Entity e : world.getNearbyEntities(blocks[i].getLocation(), .5, .5, .5))
								if (e instanceof LivingEntity && !e.isDead())
									Utils.pureDamageEntity((LivingEntity) e, 6, "dd-meteorcrush", true);
						}
						depth++;
						if (depth >= 20) cancel();
					}
				};
			}
			exists = false;
			classInstance.list.remove(this);
		}
	}
	public void removeBlocks() {
		for (int i = 0; i < blocks.length; i++) blocks[i].remove();
	}
}
