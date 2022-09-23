package deadlydisasters.disasters;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import deadlydisasters.disasters.events.DestructionDisaster;
import deadlydisasters.disasters.events.DestructionDisasterEvent;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;
import deadlydisasters.general.WorldObject;
import deadlydisasters.listeners.DeathMessages;
import deadlydisasters.utils.Metrics;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public class Geyser extends DestructionDisaster {
	
	private boolean finished,overworld,push;
	private Location mem;
	private Geyser me = this;
	private double damageAmount;
	private int amount;
	private int width = 1;
	private Material material;
	private int tickSpeed = 1;
	private Particle particleType;
	private Vector velocity = new Vector(0,3,0);
	private int minReach, maxReach, range;
	private Sound sound;
	private int spawnInterval = 10;
	private int blocksDestroyed;
	
	public Geyser(int level) {
		super(level);
		damageAmount = plugin.getConfig().getDouble("geyser.water_damage");
		volume = plugin.getConfig().getDouble("geyser.volume");
		amount = level;
		material = Material.WATER;
		range = level*2;
		sound = Sound.BLOCK_FIRE_EXTINGUISH;
		
		this.type = Disaster.GEYSER;
	}
	public Geyser(int level, Particle particle, double damage, Vector velocity, int tickSpeed, Material material, int width, int min, int max, double volume, Sound sound) {
		super(level);
		damageAmount = damage;
		this.material = material;
		this.particleType = particle;
		this.velocity = velocity;
		this.tickSpeed = tickSpeed;
		this.width = width;
		this.minReach = min;
		this.maxReach = max;
		this.volume = volume;
		this.sound = sound;
		
		this.type = Disaster.GEYSER;
	}
	public void start(Location loc, Player p) {
		if (loc.getWorld().getEnvironment() == Environment.NETHER && loc.getBlockY() >= 128) return;
		DestructionDisasterEvent event = new DestructionDisasterEvent(this, loc, level, p);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		this.loc = loc;
		double version = plugin.mcVersion;
		if (loc.getWorld().getEnvironment() == Environment.NORMAL) {
			overworld = true;
			if (this.particleType == null) {
				if (version >= 1.14)
					this.particleType = Particle.FALLING_WATER;
				else
					this.particleType = Particle.WATER_SPLASH;
			}
			if (minReach == 0 || maxReach == 0) {
				minReach = 100;
				maxReach = 256;
			}
		} else {
			if (material == Material.WATER)
				material = Material.LAVA;
			if (this.particleType == null) {
				if (version >= 1.14)
					this.particleType = Particle.FALLING_LAVA;
				else
					this.particleType = Particle.FLAME;
			}
			if (velocity.getY() == 3)
				velocity = new Vector(0,1,0);
			if (minReach == 0 || maxReach == 0) {
				minReach = 60;
				maxReach = 120;
			}
		}
		maxReach -= minReach;
		Random rand = plugin.random;
		new RepeatingTask(plugin, 0, spawnInterval) {
			@Override
            public void run() {
				Geyser shoot = new Geyser(1, particleType, damageAmount, velocity, tickSpeed, material, width, minReach, maxReach, volume, sound);
				Location location = new Location(loc.getWorld(), ThreadLocalRandom.current().nextInt(loc.getBlockX()-range, loc.getBlockX()+range+1), loc.getY(), ThreadLocalRandom.current().nextInt(loc.getBlockZ()-range, loc.getBlockZ()+range+1));
				if (overworld) shoot.createWater(location, rand);
				else shoot.createLava(location, rand);
				amount--;
				if (amount <= 0) cancel();
			}
		};
	}
	public void createWater(Location location, Random rand) {
		ongoingDisasters.add(this);
		loc = location.clone();
		mem = location.clone();
		for (int i=(int) (location.getBlockY()); i > plugin.maxDepth+5; i--) {
			loc.setY(i);
			if (loc.getBlock().getType() == Material.WATER) break;
		}
		final int r = rand.nextInt(maxReach)+minReach;
		mem.setY(loc.getY());
		final int[] px = {loc.getBlockX(), loc.getBlockX()+width};
		final int[] pz = {loc.getBlockZ(), loc.getBlockZ()+width};
		if (plugin.RegionProtection) {
			Location test = location.clone();
			for (int x=(int) px[0]-3; x < px[1]+3; x++)
				for (int z=(int) pz[0]-3; z < pz[1]+3; z++) {
					test.setX(x);
					test.setZ(z);
					if (Utils.isZoneProtected(test)) return;
				}
		}
		DeathMessages.geysers.add(this);
		new RepeatingTask(plugin, 0, tickSpeed) {
			@Override
            public void run() {
				for (int times=0; times < 3; times++) {
					if (loc.getBlockY() <= r) {
						loc.setY(loc.getY()+1);
						for (int x=px[0]; x <= px[1]; x++) {
							for (int z=pz[0]; z <= pz[1]; z++) {
								Location block = new Location(loc.getWorld(), x, loc.getY(), z);
								Block b = block.getBlock();
								if (b.getType() != material && !Utils.isBlockBlacklisted(block.getBlock().getType()) && !Utils.isZoneProtected(block)) {
									if (plugin.CProtect) Utils.getCoreProtect().logRemoval("Deadly-Disasters", block, b.getType(), b.getBlockData());
									b.setType(material);
									blocksDestroyed++;
								}
							}
						}
					} else {
						new RepeatingTask(plugin, 40, tickSpeed) {
							@Override
							public void run() {
								if (!push) push = true;
								for (int times=0; times < 3; times++) {
									if (loc.getBlockY() >= mem.getBlockY()) {
										for (int x=px[0]-2; x <= px[1]+2; x++) {
											for (int z=pz[0]-2; z <= pz[1]+2; z++) {
												Block bc = new Location(loc.getWorld(), x, loc.getY(), z).getBlock();
												if (bc.getType() == material) {
													bc.setType(Material.AIR);
													blocksDestroyed++;
												} else if (rand.nextInt(3) == 0 && bc.getType() != Material.AIR && !Utils.isBlockBlacklisted(bc.getType()) && !Utils.isZoneProtected(bc.getLocation())) {
													if (plugin.CProtect) Utils.getCoreProtect().logRemoval("Deadly-Disasters", bc.getLocation(), bc.getType(), bc.getBlockData());
													bc.setType(Material.AIR);
													blocksDestroyed++;
												}
											}
										}
										loc.getWorld().spawnParticle(particleType, loc.getX()-0.5, loc.getY(), loc.getZ()-0.5, 10, 1.5, 0.5, 1.5, 1);
										loc.setY(loc.getY()-1);
									} else {
										DeathMessages.geysers.remove(me);
										finished = true;
										cancel();
										ongoingDisasters.remove(me);
										Metrics.incrementValue(Metrics.disasterDestroyedMap, type.getMetricsLabel(), blocksDestroyed);
										return;
									}
								}
								for (Entity all : loc.getWorld().getNearbyEntities(loc, 20, 256, 20))
									if (all instanceof Player && all.getLocation().getBlockY() <= loc.getBlockY()+20) ((Player) all).playSound(loc, sound, (float) (1*volume), (float) 0.5);
							}
						};
						new RepeatingTask(plugin, 0, 1) {
							@Override
							public void run() {
								if (loc.getBlockY() <= r+20) {
									loc.setY(loc.getBlockY()+1);
									loc.getWorld().spawnParticle(particleType, px[0]+0.5, loc.getY(), pz[0]+0.5, 10, 1.5, 0.5, 1.5, 1);
									return;
								} else {
									loc.setY(loc.getY()-20);
									cancel();
								}
							}
						};
						cancel();
						break;
					}
				}
				for (Entity all : loc.getWorld().getNearbyEntities(loc, 20, loc.getY(), 20))
					if (all instanceof Player && all.getLocation().getBlockY() <= loc.getBlockY()+20) ((Player) all).playSound(loc, sound, (float) (1*volume), (float) 0.5);
			}
		};
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				for (Entity all : loc.getWorld().getNearbyEntities(loc, 2, loc.getY(), 2))
					if (all.getLocation().getBlockX() >= px[0]-1 && all.getLocation().getBlockX() <= px[1]+1 && all.getLocation().getBlockZ() >= pz[0]-1 && all.getLocation().getBlockZ() <= pz[1]+1
							&& all.getLocation().getBlockY() <= loc.getBlockY() && all.getLocation().getBlockY() >= mem.getBlockY()) all.setVelocity(velocity);
				if (push) cancel();
			}
		};
		new RepeatingTask(plugin, 0, 10) {
			@Override
			public void run() {
				for (Entity all : loc.getWorld().getNearbyEntities(loc, 2, loc.getY(), 2))
					if (all instanceof LivingEntity && all.getLocation().getBlockX() >= px[0]-1 && all.getLocation().getBlockX() <= px[1]+1 && all.getLocation().getBlockZ() >= pz[0]-1 && all.getLocation().getBlockZ() <= pz[1]+1
					&& all.getLocation().getBlockY() <= loc.getBlockY() && all.getLocation().getBlockY() >= mem.getBlockY() && !((LivingEntity) all).hasPotionEffect(PotionEffectType.FIRE_RESISTANCE))
						Utils.pureDamageEntity((LivingEntity) all, damageAmount, "dd-geyserdeath", false);
				if (finished) cancel();
			}
		};
	}
	public void createLava(Location location, Random rand) {
		ongoingDisasters.add(this);
		loc = location.clone();
		mem = location.clone();
		for (int i=(int) (location.getBlockY()); i > 5; i--) {
			loc.setY(i);
			if (loc.getBlock().getType() == Material.LAVA) break;
		}
		final int r = rand.nextInt(maxReach)+minReach;
		mem.setY(loc.getY());
		final int[] px = {loc.getBlockX(), loc.getBlockX()+width};
		final int[] pz = {loc.getBlockZ(), loc.getBlockZ()+width};
		if (plugin.RegionProtection) {
			Location test = location.clone();
			for (int x=px[0]-3; x < px[1]+3; x++)
				for (int z=pz[0]-3; z < pz[1]+3; z++) {
					test.setX(x);
					test.setZ(z);
					if (Utils.isZoneProtected(test)) return;
				}
		}
		DeathMessages.geysers.add(this);
		new RepeatingTask(plugin, 0, tickSpeed) {
			@Override
			public void run() {
				if (loc.getBlockY() < r) {
					loc.setY(loc.getY()+1);
					for (int x=px[0]; x <= px[1]; x++) {
						for (int z=pz[0]; z <= pz[1]; z++) {
							Location block = new Location(loc.getWorld(), x, loc.getY(), z);
							Block b = block.getBlock();
							if (b.getType() != material && !Utils.isBlockBlacklisted(b.getType()) && !Utils.isZoneProtected(block)) {
								if (plugin.CProtect) Utils.getCoreProtect().logRemoval("Deadly-Disasters", block, b.getType(), b.getBlockData());
								b.setType(material);
								blocksDestroyed++;
							}
						}
					}
					for (Entity all : loc.getWorld().getNearbyEntities(loc, 2, 128, 2)) {
						if (all.getLocation().getBlockX() >= px[0] && all.getLocation().getBlockX() <= px[1] && all.getLocation().getBlockZ() >= pz[0] && all.getLocation().getBlockZ() <= pz[1]
								&& all.getLocation().getBlockY() <= loc.getBlockY() && all.getLocation().getBlockY() >= mem.getBlockY()) {
							all.setVelocity(new Vector(0,1,0));
						}
					}
				} else {
					new RepeatingTask(plugin, 100, tickSpeed) {
						@Override
						public void run() {
							if (!push) push = true;
							if (loc.getBlockY() >= mem.getBlockY()) {
								for (int x=px[0]-2; x <= px[1]+2; x++) {
									for (int z=pz[0]-2; z <= pz[1]+2; z++) {
										Location block = new Location(loc.getWorld(), x, loc.getY(), z);
										if (block.getBlock().getType() == material) {
											block.getBlock().setType(Material.AIR);
											blocksDestroyed++;
										} else if (rand.nextInt(3) == 0 && block.getBlock().getType() != Material.AIR && !Utils.isBlockBlacklisted(block.getBlock().getType()) && !Utils.isZoneProtected(block)) {
											if (plugin.CProtect) Utils.getCoreProtect().logRemoval("Deadly-Disasters", block, block.getBlock().getType(), block.getBlock().getBlockData());
											block.getBlock().setType(Material.AIR);
											blocksDestroyed++;
										}
									}
								}
								loc.getWorld().spawnParticle(particleType, px[0]+0.5, loc.getY(), pz[0]+0.5, 10, 1.5, 0.5, 1.5, 1);
								loc.setY(loc.getY()-1);
							} else {
								finished = true;
								DeathMessages.geysers.remove(me);
								cancel();
								ongoingDisasters.remove(me);
								Metrics.incrementValue(Metrics.disasterDestroyedMap, type.getMetricsLabel(), blocksDestroyed);
								return;
							}
							for (Entity all : loc.getWorld().getNearbyEntities(loc, 20, 128, 20))
								if (all instanceof Player && all.getLocation().getBlockY() <= loc.getBlockY()+20) ((Player) all).playSound(loc, sound, (float) (1*volume), (float) 0.5);
						}
					};
					new RepeatingTask(plugin, 0, 1) {
						@Override
						public void run() {
							if (loc.getBlockY() <= r+10) {
								loc.setY(loc.getBlockY()+1);
								loc.getWorld().spawnParticle(particleType, loc.getX()-0.5, loc.getY(), loc.getZ()-0.5, 10, 1.5, 0.5, 1.5, 1);
								return;
							} else {
								loc.setY(loc.getY()-10);
								cancel();
							}
						}
					};
					cancel();
				}
				for (Entity all : loc.getWorld().getNearbyEntities(loc, 20, 128, 20))
					if (all instanceof Player && all.getLocation().getBlockY() <= loc.getBlockY()+20) ((Player) all).playSound(loc, sound, (float) (1*volume), (float) 0.5);
			}
		};
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				for (Entity all : loc.getWorld().getNearbyEntities(loc, 2, loc.getY(), 2))
					if (all.getLocation().getBlockX() >= px[0]-1 && all.getLocation().getBlockX() <= px[1]+1 && all.getLocation().getBlockZ() >= pz[0]-1 && all.getLocation().getBlockZ() <= pz[1]+1
							&& all.getLocation().getBlockY() <= loc.getBlockY() && all.getLocation().getBlockY() >= mem.getBlockY()) all.setVelocity(velocity);
				if (push) cancel();
			}
		};
	}
	public Location findApplicableLocation(Location temp, Player p) {
		temp = Utils.getBlockBelow(temp).getLocation();
		if (temp.getBlockY() < type.getMinHeight() || temp.getWorld().getEnvironment() == Environment.THE_END)
			return null;
		broadcastMessage(temp, p);
		return temp;
	}
	public void broadcastMessage(Location temp, Player p) {
		if ((boolean) WorldObject.findWorldObject(temp.getWorld()).settings.get("event_broadcast")) {
			String str = plugin.getConfig().getString("messages.destructive.level "+level);
			str = str.replace("%location%", temp.getBlockX()+" "+temp.getBlockY()+" "+temp.getBlockZ());
			if (p != null) str = str.replace("%player%", p.getName());
			else str = str.replace("%player%", "");
			if (temp.getWorld().getEnvironment() == Environment.NORMAL)
				str = str.replace("%disaster%", type.getLabel().substring(0, type.getLabel().indexOf('/')));
			else
				str = str.replace("%disaster%", type.getLabel().substring(type.getLabel().indexOf('/')+1));
			str = ChatColor.translateAlternateColorCodes('&', str);
			if (plugin.getConfig().getBoolean("messages.disaster_tips"))
				str += "\n"+type.getTip();
			for (Player players : temp.getWorld().getPlayers())
				players.sendMessage(str);
			Main.consoleSender.sendMessage(Languages.prefix+str+ChatColor.GREEN+" ("+temp.getWorld().getName()+")");
		}
	}
	public void startAdjustment(Location loc, Player p) {
		start(loc, p);
	}
	public int getX() {
		return loc.getBlockX();
	}
	public int getZ() {
		return loc.getBlockZ();
	}
	public int getY() {
		return loc.getBlockY();
	}
	public int getMemory() {
		return mem.getBlockY();
	}
	public double getDamage() {
		return damageAmount;
	}
	public void setDamage(double damage) {
		this.damageAmount = damage;
	}
	public int getMiniGeyserAmount() {
		return amount;
	}
	public void setMiniGeyserAmount(int amount) {
		this.amount = amount;
	}
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	public Material getMaterial() {
		return material;
	}
	public void setMaterial(Material material) {
		this.material = material;
	}
	public int getTickSpeed() {
		return tickSpeed;
	}
	public void setTickSpeed(int tickSpeed) {
		this.tickSpeed = tickSpeed;
	}
	public Particle getParticle() {
		return particleType;
	}
	public void setParticle(Particle particle) {
		this.particleType = particle;
	}
	public void setYVelocity(double y) {
		this.velocity = new Vector(0,y,0);
	}
	public int getMinReach() {
		return minReach;
	}
	public void setMinReach(int minReach) {
		this.minReach = minReach;
	}
	public int getMaxReach() {
		return maxReach;
	}
	public void setMaxReach(int maxReach) {
		this.maxReach = maxReach;
	}
	public int getRange() {
		return range;
	}
	public void setRange(int range) {
		this.range = range;
	}
	public Sound getSound() {
		return sound;
	}
	public void setSound(Sound sound) {
		this.sound = sound;
	}
	public int getSpawnInterval() {
		return spawnInterval;
	}
	public void setSpawnInterval(int spawnInterval) {
		this.spawnInterval = spawnInterval;
	}
} 
