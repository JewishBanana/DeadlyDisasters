package com.github.jewishbanana.deadlydisasters.events.disasters;

import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
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

public class Supernova extends DestructionDisaster {
	
	private double size,sizeMultiplier,particleMultiplier;
	private int tick = 1;
	private EnderCrystal crystal;
	private Particle particle;
	private Material[] materials;
	private boolean flash,farParticles;
	private int blocksDestroyed;

	public Supernova(int level) {
		super(level);
		switch (level) {
		default:
		case 1:
			size = 35;
			break;
		case 2:
			size = 45;
			break;
		case 3:
			size = 57;
			break;
		case 4:
			size = 70;
			break;
		case 5:
			size = 85;
			break;
		case 6:
			size = 100;
			break;
		}
		volume = configFile.getDouble("supernova.volume");
		particleMultiplier = configFile.getDouble("supernova.particle_multiplier");
		flash = configFile.getBoolean("supernova.flash");
		this.sizeMultiplier = configFile.getDouble("supernova.size");
		this.particle = Particle.EXPLOSION_LARGE;
		materials = new Material[]{Material.OBSIDIAN, Material.BLACK_CONCRETE, Material.FIRE};
		if (!configFile.getBoolean("supernova.place_fire"))
			materials = new Material[]{Material.OBSIDIAN, Material.BLACK_CONCRETE};
		farParticles = configFile.getBoolean("supernova.far_particles");
		this.type = Disaster.SUPERNOVA;
	}
	@Override
	public void start(Location loc, Player p) {
		DestructionDisasterEvent event = new DestructionDisasterEvent(this, loc, level, p);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		this.loc = loc;
		ongoingDisasters.add(this);
		DeathMessages.supernovas.add(this);
		size *= sizeMultiplier;
		World world = loc.getWorld();
		Location top = loc.clone();
		top.setY(320);
		Location crystalLoc = top.clone();
		crystal = (EnderCrystal) world.spawnEntity(crystalLoc, EntityType.ENDER_CRYSTAL);
		crystal.setShowingBottom(false);
		crystal.setBeamTarget(top);
		Vector vec = new Vector(0,-2,0);
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				crystalLoc.add(vec);
				if (vec.getY() < -0.5)
					vec.setY(vec.getY()+0.01);
				crystal.remove();
				crystal = (EnderCrystal) world.spawnEntity(crystalLoc, EntityType.ENDER_CRYSTAL);
				crystal.setShowingBottom(false);
				crystal.setBeamTarget(top);
				world.spawnParticle(Particle.CLOUD, crystalLoc, 10, .5, .5, .5, 0.01, null, true);
				world.spawnParticle(Particle.FLAME, crystalLoc, 10, .5, .5, .5, 0.01, null, true);
				Block b = crystalLoc.clone().subtract(0,1,0).getBlock();
				if (b.getType() != Material.AIR && !Utils.isBlockBlacklisted(b.getType()) && !Utils.isZoneProtected(b.getLocation())) {
					if (plugin.CProtect)
						Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
					b.setType(Material.AIR);
					blocksDestroyed++;
				}
				if (crystalLoc.distance(loc) < 3) {
					crystal.remove();
					cancel();
					explode(world);
				}
				if (flash)
					for (Entity e : world.getNearbyEntities(loc, 140, 100, 140))
						if (e instanceof Player) {
							Location temp = e.getLocation();
							((Player) e).spawnParticle(Particle.FLASH, temp.clone().add(new Vector(temp.getX() - crystalLoc.getX(), temp.getY() - crystalLoc.getY(), temp.getZ() - crystalLoc.getZ()).normalize().multiply(-3)),
									1, 1, 1, 1, 0.001);
						}
			}
		};
		new RepeatingTask(plugin, 0, 10) {
			@Override
			public void run() {
				if (tick > 1)
					cancel();
				if (plugin.mcVersion >= 1.16) {
					for (Entity e : world.getNearbyEntities(loc, 140, 140, 140))
						if (e instanceof Player) {
							Location temp = e.getLocation();
							((Player) e).playSound(temp.clone().add(new Vector(temp.getX() - crystalLoc.getX(), temp.getY() - crystalLoc.getY(), temp.getZ() - crystalLoc.getZ()).normalize().multiply(-4)),
								Sound.AMBIENT_NETHER_WASTES_MOOD, (float) ((2 - (0.002 * crystalLoc.distance(e.getLocation())))*volume), 0.5F);
						}
				}
			}
		};
	}
	private void explode(World world) {
		BlockVector block = new BlockVector(loc.getX(), loc.getY(), loc.getZ());
		final Random rand = new Random();
		FixedMetadataValue fixdata = new FixedMetadataValue(plugin, "protected");
		boolean CP = plugin.CProtect;
		Supernova instance = this;
		
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				if (tick > size) {
					cancel();
					ongoingDisasters.remove(instance);
					DeathMessages.supernovas.remove(instance);
					Metrics.incrementValue(Metrics.disasterDestroyedMap, type.getMetricsLabel(), blocksDestroyed);
					return;
				}
				Set<Block> blocks = new LinkedHashSet<>(tick*tick*tick);
				for (int x = -tick; x < tick; x++)
					for (int y = -tick; y < tick; y++)
						for (int z = -tick; z < tick; z++) {
							Vector position = block.clone().add(new Vector(x, y, z));
							if (!(block.distance(position) >= (tick - 1) && block.distance(position) <= tick)) continue;
							Block b = world.getBlockAt(position.toLocation(world));
							blocks.add(b);
							if (b.getType() == Material.AIR || Utils.isBlockBlacklisted(b.getType()) || Utils.isZoneProtected(b.getLocation()))
								continue;
							if (CP) Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
							if (tick > size-2 && rand.nextInt(8) == 0) {
								Material mat = materials[rand.nextInt(materials.length)];
								if (CP) Utils.getCoreProtect().logPlacement("Deadly-Disasters", b.getLocation(), mat, mat.createBlockData());
								b.setType(mat);
								blocksDestroyed++;
								continue;
							}
							b.setType(Material.AIR);
							blocksDestroyed++;
						}
				tick++;
				plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
					@Override
					public void run() {
						int chance = (int) Math.ceil((1 + (blocks.size()/1000)) * particleMultiplier);
						for (Block b : blocks)
							if (rand.nextInt(chance) == 0)
								world.spawnParticle(particle, b.getLocation(), 1, 0, 0, 0, 1, null, farParticles);
					}
				});
			}
		};
		new RepeatingTask(plugin, 0, 10) {
			@Override
			public void run() {
				if (tick > size) {
					cancel();
					return;
				}
				for (Entity e : world.getNearbyEntities(loc, tick+100, tick+100, tick+100))
					if (e instanceof LivingEntity && !e.isDead()) {
						if (loc.distance(e.getLocation()) < tick) {
							if (Utils.isZoneProtected(e.getLocation()) || (e instanceof Player && Utils.isPlayerImmune((Player) e)))
								continue;
							e.setMetadata("dd-supernova", fixdata);
							((LivingEntity) e).damage(0.01);
							e.setLastDamageCause(new EntityDamageEvent(e, DamageCause.CUSTOM, 20));
							((LivingEntity) e).setHealth(0);
						} else if (e instanceof Player) {
							Location temp = e.getLocation().clone().add(new Vector(e.getLocation().getX() - loc.getX(), e.getLocation().getY() - loc.getY(), e.getLocation().getZ() - loc.getZ()).normalize().multiply(-4));
							float vol = (float) ((2 - (0.0005 * (loc.distance(e.getLocation())) - tick))*volume);
							if (plugin.mcVersion >= 1.16)
								((Player) e).playSound(temp, Sound.AMBIENT_WARPED_FOREST_MOOD, vol, 0.5F);
							((Player) e).playSound(temp, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, vol, 0.5F);
							((Player) e).playSound(temp, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, vol/30, 0.5F);
						}
					}
			}
		};
	}
	public void removeCrystal() {
		if (crystal != null)
			crystal.remove();
	}
	public Location findApplicableLocation(Location temp, Player p) {
		temp = Utils.getBlockBelow(temp).getLocation();
		if (temp.getBlockY() < type.getMinHeight() || temp.getWorld().getEnvironment() == Environment.NETHER)
			return null;
		return temp;
	}
	public void startAdjustment(Location loc, Player p) {
		start(Utils.getBlockBelow(loc).getLocation(), p);
	}
	public double getSizeMultiplier() {
		return sizeMultiplier;
	}
	public void setSizeMultiplier(double sizeMultiplier) {
		this.sizeMultiplier = sizeMultiplier;
	}
	public Particle getParticle() {
		return particle;
	}
	public void setParticle(Particle particle) {
		this.particle = particle;
	}
	public Material[] getMaterials() {
		return materials;
	}
	public void setMaterials(Material[] materials) {
		this.materials = materials;
	}
	public boolean isFlash() {
		return flash;
	}
	public void setFlash(boolean flash) {
		this.flash = flash;
	}
	public double getParticles() {
		return particleMultiplier;
	}
	public void setParticles(double particles) {
		this.particleMultiplier = particles;
	}
}
