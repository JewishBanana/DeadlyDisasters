package com.github.jewishbanana.deadlydisasters.events.disasters;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.soulstormentities.LostSoul;
import com.github.jewishbanana.deadlydisasters.entities.soulstormentities.SoulReaper;
import com.github.jewishbanana.deadlydisasters.events.Disaster;
import com.github.jewishbanana.deadlydisasters.events.WeatherDisaster;
import com.github.jewishbanana.deadlydisasters.events.WeatherDisasterEvent;
import com.github.jewishbanana.deadlydisasters.handlers.WorldObject;
import com.github.jewishbanana.deadlydisasters.listeners.DeathMessages;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class SoulStorm extends WeatherDisaster {
	
	private SoulStorm obj;
	private boolean spawnVex;
	private double version;
	
	private Queue<UUID> souls = new ArrayDeque<>();
	
	public SoulStorm(int level, World world) {
		super(level, world);
		obj = this;
		spawnVex = configFile.getBoolean("soulstorm.spawn_souls");
		time = configFile.getInt("soulstorm.time.level "+this.level) * 20;
		delay = configFile.getInt("soulstorm.start_delay") * 20;
		volume = configFile.getDouble("soulstorm.volume");
		version = plugin.mcVersion;
		this.type = Disaster.SOULSTORM;
	}
	public void start(World world, Player p, boolean broadcastAllowed) {
		WeatherDisasterEvent event = new WeatherDisasterEvent(this, world, level, p);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		this.world = world;
		updateWeatherSettings();
		if (broadcastAllowed && (boolean) WorldObject.findWorldObject(world).settings.get("event_broadcast"))
			Utils.broadcastEvent(level, "weather", this.type, world);
		DeathMessages.soulstorms.add(this);
		final Random rand = new Random();
		DustOptions dust = new DustOptions(Color.fromRGB(170,228,255), 1);
		new RepeatingTask(plugin, delay, 5) {
			@Override
			public void run() {
				if (time > 0) {
					for (Player p : world.getPlayers()) {
						Location loc = p.getLocation();
						if (p.getGameMode().equals(GameMode.CREATIVE) || p.getGameMode().equals(GameMode.SPECTATOR)) continue;
						if (Utils.isWeatherDisabled(loc, obj)) continue;
						p.removePotionEffect(PotionEffectType.BLINDNESS);
						p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1, true));
						if (version >= 1.16) {
							p.spawnParticle(Particle.ASH, (double)loc.getX(), (double)loc.getY()+1, (double)loc.getZ(), 30, 1, 1, 1, 1);
							p.spawnParticle(Particle.WHITE_ASH, (double)loc.getX(), (double)loc.getY()+1, (double)loc.getZ(), 30, 1, 1, 1, 1);
							p.spawnParticle(Particle.WARPED_SPORE, (double)loc.getX(), (double)loc.getY()+1, (double)loc.getZ(), 30, 1, 1, 1, 1);
							p.spawnParticle(Particle.SOUL, (double)loc.getX(), (double)loc.getY()+1, (double)loc.getZ(), 2, 2, 1, 2, 0.001);
							if (rand.nextInt(8) == 0) p.spawnParticle(Particle.FLASH, (double)loc.getX(), (double)loc.getY()+1, (double)loc.getZ(), 1, 1, 1, 1, 1);
						} else {
							p.spawnParticle(Particle.SQUID_INK, (double)loc.getX(), (double)loc.getY()+2, (double)loc.getZ(), 5, 2, 1, 2, 0.1);
							p.spawnParticle(VersionUtils.getRedstoneDust(), (double)loc.getX(), (double)loc.getY()+1, (double)loc.getZ(), 30, 2, 1, 2, 0.1, dust);
						}
						p.playSound(new Location(p.getWorld(), p.getLocation().getX(), p.getLocation().getY()+3, p.getLocation().getZ()), Sound.WEATHER_RAIN_ABOVE, (float) (0.017*volume), 0.5F);
					}
					time-=5;
				} else {
					DeathMessages.soulstorms.remove(obj);
					if (version >= 1.16)
						world.getPlayers().stream().forEach(p -> p.stopSound(Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP));
					cancel();
				}
			}
		};
		boolean custom = plugin.getConfig().getBoolean("customentities.allow_custom_mobs");
		new RepeatingTask(plugin, delay + 100, 80) {
			@Override
			public void run() {
				if (time > 0) {
					for (Player p : world.getPlayers()) {
						if (version >= 1.16) {
							p.playSound(new Location(p.getWorld(), p.getLocation().getX(), p.getLocation().getY()+3, p.getLocation().getZ()), Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP, (float) (1*volume), 0.5F);
							p.playSound(new Location(p.getWorld(), p.getLocation().getX(), p.getLocation().getY()+3, p.getLocation().getZ()), Sound.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, (float) (1*volume), 0.5F);
						}
						if (rand.nextInt(4) != 0) continue;
						if (p.getGameMode().equals(GameMode.CREATIVE) || p.getGameMode().equals(GameMode.SPECTATOR)) continue;
						if (Utils.isWeatherDisabled(p.getLocation(), obj)) continue;
						if (version >= 1.16)
							p.playSound(new Location(p.getWorld(), p.getLocation().getX(), p.getLocation().getY()+3, p.getLocation().getZ()), Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, (float) (1*volume), 0.75F);
						if (!spawnVex) continue;
						Location loc = p.getLocation();
						breakthis:
						for (int x=loc.getBlockX()-2; x < loc.getBlockX()+2; x++) {
							for (int z=loc.getBlockZ()-2; z < loc.getBlockZ()+2; z++) {
								Location place = p.getLocation();
								place.setX(x);
								place.setY(loc.getBlockY()+4);
								place.setZ(z);
								Block b = place.getBlock();
								if (b.getType() == Material.AIR) {
									Vex vex = (Vex) world.spawnEntity(place, EntityType.VEX);
									if (custom) {
										if (rand.nextInt(100) < 3 && CustomEntityType.mobsEnabled) {
											vex.remove();
											Mob reaper = (Mob) world.spawnEntity(place, EntityType.SKELETON);
											CustomEntity.handler.addEntity(new SoulReaper(reaper, plugin, rand));
											souls.add(reaper.getUniqueId());
											break breakthis;
										} else if (CustomEntityType.mobsEnabled)
											CustomEntity.handler.addEntity(new LostSoul(vex, plugin, rand));
									}
									vex.setTarget(p);
									souls.add(vex.getUniqueId());
									break breakthis;
								}
							}
						}
					}
				} else {
					clearEntities();
					cancel();
				}
			}
		};
	}
	@Override
	public void clear() {
		time = 0;
		clearEntities();
	}
	public void clearEntities() {
		for (UUID e : souls)
			if (Bukkit.getEntity(e) != null)
				Bukkit.getEntity(e).remove();
	}
	public boolean canVexSpawn() {
		return spawnVex;
	}
	public void setVexSpawn(boolean value) {
		this.spawnVex = value;
	}
}
