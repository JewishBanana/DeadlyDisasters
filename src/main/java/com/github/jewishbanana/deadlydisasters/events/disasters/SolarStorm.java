package com.github.jewishbanana.deadlydisasters.events.disasters;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.solarstormentities.FirePhantom;
import com.github.jewishbanana.deadlydisasters.events.Disaster;
import com.github.jewishbanana.deadlydisasters.events.WeatherDisaster;
import com.github.jewishbanana.deadlydisasters.events.WeatherDisasterEvent;
import com.github.jewishbanana.deadlydisasters.handlers.WorldObject;
import com.github.jewishbanana.deadlydisasters.listeners.CoreListener;
import com.github.jewishbanana.deadlydisasters.listeners.DeathMessages;
import com.github.jewishbanana.deadlydisasters.utils.Metrics;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class SolarStorm extends WeatherDisaster {
	
	private boolean changeTime,spawnMobs = true;
	private long timeMemory;
	private double fireCatch,fireSpawn;
	private float fireballSize;
	private Material fireMaterial = Material.FIRE;
	public int blocksDestroyed;
	
	private Queue<UUID> fallingBlocks = new ArrayDeque<>();
	private Set<UUID> entities = new HashSet<>();
	
	private Map<UUID,UUID> targets = new HashMap<>();
	
	public SolarStorm(int level) {
		super(level);
		time = configFile.getInt("solarstorm.time.level "+this.level) * 20;
		delay = configFile.getInt("solarstorm.start_delay") * 20;
		volume = configFile.getDouble("solarstorm.volume");
		changeTime = configFile.getBoolean("solarstorm.set_sunset");
		fireCatch = configFile.getDouble("solarstorm.fire_catch_chance");
		fireSpawn = configFile.getDouble("solarstorm.fire_spawn_rate");
		fireballSize = (float) configFile.getDouble("solarstorm.fireball_explosion_size");
		
		this.type = Disaster.SOLARSTORM;
	}
	@Override
	public void start(World world, Player p, boolean broadcastAllowed) {
		WeatherDisasterEvent event = new WeatherDisasterEvent(this, world, level, p);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		this.world = world;
		updateWeatherSettings();
		ongoingDisasters.add(this);
		if (broadcastAllowed && (boolean) WorldObject.findWorldObject(world).settings.get("event_broadcast"))
			Utils.broadcastEvent(level, "weather", this.type, world);
		DeathMessages.solarstorms.add(this);
		Random rand = plugin.random;
		int maxFires = (int) (15 * fireSpawn);
		BlockData bd = fireMaterial.createBlockData();
		FixedMetadataValue fixdata = new FixedMetadataValue(plugin, "protected");
		SolarStorm instance = this;
		int[] ticksPerSpawn = {0};
		new RepeatingTask(plugin, delay, 20) {
			@Override
			public void run() {
				Vector vec = new Vector(.6,-2,.6);
				for (UUID uuid : fallingBlocks)
					if (Bukkit.getEntity(uuid) != null)
						Bukkit.getEntity(uuid).setVelocity(vec);
				if (time > 0) {
					time -= 20;
					ticksPerSpawn[0]++;
					if (world.hasStorm())
						world.setStorm(false);
					for (LivingEntity e : world.getLivingEntities()) {
						if (entities.contains(e.getUniqueId()) && ((Mob) e).getTarget() == null && Bukkit.getEntity(targets.get(e.getUniqueId())) != null)
							((Mob) e).setTarget((LivingEntity) Bukkit.getEntity(targets.get(e.getUniqueId())));
						if (Utils.isWeatherDisabled(e.getLocation(), instance))
							continue;
						if (e.getFireTicks() > 0)
							e.setFireTicks(e.getFireTicks()+10);
						if (e.getLocation().getBlockY() > 55 && e.getLocation().getBlock().getLightFromBlocks() >= (byte)11 && rand.nextDouble()*100 < fireCatch)
							e.setFireTicks(100);
						if (e instanceof Player && !Utils.isPlayerImmune((Player) e)) {
							for (int i=0; i < maxFires; i++) {
								FallingBlock fb = world.spawnFallingBlock(new Location(world, e.getLocation().getBlockX()+(rand.nextInt(100)-90), Math.max(e.getLocation().getBlockY()+70, 210), e.getLocation().getBlockZ()+(rand.nextInt(100)-90)), bd);
								fb.setGravity(false);
								fb.setMetadata("dd-fb", fixdata);
								CoreListener.fallingBlocks.put(fb.getUniqueId(), instance);
								fallingBlocks.add(fb.getUniqueId());
							}
							Fireball ball = (Fireball) world.spawnEntity(new Location(world, e.getLocation().getBlockX()+(rand.nextInt(100)-90), Math.max(e.getLocation().getBlockY()+50, 210), e.getLocation().getBlockZ()+(rand.nextInt(100)-90)), EntityType.FIREBALL);
							ball.setDirection(new Vector(.8, -3, .8));
							ball.setYield(fireballSize);
							CoreListener.fireBalls.put(ball, instance);
							if (CustomEntityType.mobsEnabled && spawnMobs && ticksPerSpawn[0] >= 7 && e.getLocation().getBlockY() >= 60 && rand.nextInt(3) == 0) {
								Location spawn = e.getLocation().clone().add(rand.nextInt(40)-20, 20, rand.nextInt(40)-20);
								if (!spawn.getBlock().isPassable())
									spawn = Utils.getBlockAbove(spawn).getLocation();
								Mob entity = (Mob) world.spawnEntity(spawn, EntityType.PHANTOM);
								CustomEntity.handler.addEntity(new FirePhantom(entity, plugin, rand));
								entity.setTarget(e);
								entities.add(entity.getUniqueId());
								targets.put(entity.getUniqueId(), e.getUniqueId());
								entity.setMetadata("dd-solarstormmob", fixdata);
							}
						}
					}
					if (ticksPerSpawn[0] >= 7)
						ticksPerSpawn[0] = 0;
				} else {
					if (!fallingBlocks.isEmpty()) {
						Iterator<UUID> it = fallingBlocks.iterator();
						while (it.hasNext()) {
							Entity entity = Bukkit.getEntity(it.next());
							if (entity == null || entity.isDead())
								it.remove();
						}
						return;
					}
					clear();
					DeathMessages.solarstorms.remove(instance);
					cancel();
					ongoingDisasters.remove(instance);
					Metrics.incrementValue(Metrics.disasterDestroyedMap, type.getMetricsLabel(), blocksDestroyed);
				}
			}
		};
		if (changeTime) {
			timeMemory = world.getTime();
			new RepeatingTask(plugin, delay, 1) {
				@Override
				public void run() {
					long gameTime = world.getTime();
					if (time <= 0) {
						if (gameTime < timeMemory - 300 || gameTime > timeMemory + 300) {
							if (gameTime > timeMemory - 8000 && gameTime < timeMemory + 300) world.setTime(gameTime + 200);
							else world.setTime(gameTime - 200);
							if (DeathMessages.solarstorms.stream().anyMatch(n -> n.getWorld().equals(world)))
								cancel();
							return;
						}
						cancel();
						return;
					}
					if (gameTime < 12700 || gameTime > 13300)
						if (gameTime > 8000 && gameTime < 13300) world.setTime(gameTime + 200);
						else world.setTime(gameTime - 200);
				}
			};
		}
	}
	@Override
	public void clear() {
		time = 0;
		clearEntities();
		Iterator<UUID> it = fallingBlocks.iterator();
		while (it.hasNext()) {
			Entity entity = Bukkit.getEntity(it.next());
			if (entity != null)
				entity.remove();
		}
	}
	public void clearEntities() {
		for (UUID e : entities)
			if (Bukkit.getEntity(e) != null)
				Bukkit.getEntity(e).remove();
	}
	public boolean isChangeTime() {
		return changeTime;
	}
	public void setChangeTime(boolean changeTime) {
		this.changeTime = changeTime;
	}
	public boolean isSpawnMobs() {
		return spawnMobs;
	}
	public void setSpawnMobs(boolean spawnMobs) {
		this.spawnMobs = spawnMobs;
	}
	public double getFireCatch() {
		return fireCatch;
	}
	public void setFireCatch(double fireCatch) {
		this.fireCatch = fireCatch;
	}
	public float getFireballSize() {
		return fireballSize;
	}
	public void setFireballSize(float fireballSize) {
		this.fireballSize = fireballSize;
	}
	public Material getFireMaterial() {
		return fireMaterial;
	}
	public void setFireMaterial(Material fireMaterial) {
		this.fireMaterial = fireMaterial;
	}
	public double getFireSpawn() {
		return fireSpawn;
	}
	public void setFireSpawn(double fireSpawn) {
		this.fireSpawn = fireSpawn;
	}
}
