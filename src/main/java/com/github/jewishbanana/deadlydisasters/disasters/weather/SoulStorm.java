package com.github.jewishbanana.deadlydisasters.disasters.weather;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.github.jewishbanana.deadlydisasters.disasters.MobDisaster;
import com.github.jewishbanana.deadlydisasters.disasters.WeatherDisaster;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.SpawnUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class SoulStorm extends WeatherDisaster implements MobDisaster {
	
	private float mobSpawnRate;
	
	private float particleRate;
	
	private List<PotionEffect> effects;

	public SoulStorm(@NotNull Location location, Player player, int level) {
		super(location, player, level);
	}
	public void init() {
		super.init();
		this.mobSpawnRate = (float) (0.04 * getConfigDouble("mob_spawn_multiplier") * (scale / 2.0));
		
		this.effects = buildPotionEffects("entity_effects");
		
		this.particleRate = (float) (0.8 * particleMultiplier * (scale / 3.0));
		this.soundTickRate = 80;
	}
	public void start() {
		super.start();
		location.setY(128);
		final World world = location.getWorld();
		scheduleTask(new BukkitRunnable() {
			@Override
			public void run() {
				for (Entity entity : entitiesInMonitorArea) {
					if (entity instanceof Player player) {
						if (EntityUtils.isPlayerImmune(player))
							continue;
						if (random.nextFloat() < mobSpawnRate) {
							Location spawn = SpawnUtils.findMonsterSpawnLocationNoCollision(entity.getLocation(), 1, SpawnUtils.MIN_SPAWN_DISTANCE_FROM_PLAYERS, 30);
							if (spawn != null) {
								Mob mob = null;
								switch (DependencyUtils.isUltimateContentEnabled() ? 1 : 0) {
								default:
								case 0:
									mob = world.spawn(spawn, Vex.class);
									break;
								case 1:
									if (random.nextInt(20) == 0)
										mob = com.github.jewishbanana.uiframework.entities.UIEntityManager.spawnEntity(spawn, com.github.jewishbanana.ultimatecontent.entities.netherentities.SoulReaper.class).getCastedEntity();
									else
										mob = com.github.jewishbanana.uiframework.entities.UIEntityManager.spawnEntity(spawn, com.github.jewishbanana.ultimatecontent.entities.netherentities.LostSoul.class).getCastedEntity();
									break;
								}
								addEntityToDisasterList(mob, player);
							}
						}
					}
					if (entity instanceof LivingEntity alive)
						alive.addPotionEffects(effects);
					else if (entity instanceof Item && random.nextInt(10) == 0)
						entity.setVelocity(entity.getVelocity().add(new Vector(random.nextFloat(-1, 1), random.nextFloat(), random.nextFloat(-1, 1)).multiply(scale / 2.0 * currentStrength)));
				}
				
				updateEntityTargets();
				
				time -= 5;
				if (time <= 0)
					stop();
			}
		}.runTaskTimer(plugin, 0, 5));
		
		final double distanceSquared = disasterRange * disasterRange;
		createAsyncEntityMonitor(Entity::isValid, 
				(found, entities, players) -> {
					found.forEach((entity, loc) -> {
						if (!Utils.isLocationsWithinDistance(new Location(loc.getWorld(), loc.getX(), location.getY(), loc.getZ()), location, distanceSquared))
							return;
						if (isEntityProtected(entity))
							return;
						entities.add(entity);
					});
				});
		
		final double trueSmoothingRange = (disasterRange + smoothingRange) * (disasterRange + smoothingRange);
		createParticleAsyncTask(player -> {
			final ThreadLocalRandom random = ThreadLocalRandom.current();
			final Location loc = player.getLocation();
			if (new Location(loc.getWorld(), loc.getX(), location.getY(), loc.getZ()).distanceSquared(location) > distanceSquared 
					|| random.nextFloat() > particleRate * currentStrength)
				return;
			final Location particleLoc = loc.clone().add(0, 1, 0);
			player.spawnParticle(Particle.ASH, particleLoc, 10, 5.0, 3.0, 5.0, 1.0);
			player.spawnParticle(Particle.WHITE_ASH, particleLoc, 10, 5.0, 3.0, 5.0, 1.0);
			player.spawnParticle(Particle.WARPED_SPORE, particleLoc, 10, 5.0, 3.0, 5.0, 1.0);
			player.spawnParticle(Particle.SOUL, particleLoc, 1, 5.0, 3.0, 5.0, 1.0);
//			playSound(player, particleLoc, Sound.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, 0.017 * currentStrength, .5);
			if (soundTick == 0) {
				playSound(player, particleLoc, Sound.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, SoundCategory.WEATHER, 1, .5);
				playSound(player, particleLoc, Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP, SoundCategory.WEATHER, 1, .5);
				if (random.nextInt(10) == 0)
					playSound(player, particleLoc, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, SoundCategory.WEATHER, 1, .75);
			}
		}, pair -> {
			final ThreadLocalRandom random = ThreadLocalRandom.current();
			final Player player = pair.getFirst();
			final double intensity = pair.getSecond();
			final Location loc = player.getLocation();
			for (Block block : BlockUtils.getBlocksInCircleRadius(loc, particleRenderDistance)) {
				final double actualDistance = new Location(block.getWorld(), block.getX() + 0.5, location.getY(), block.getZ() + 0.5).distanceSquared(location);
				if (actualDistance > trueSmoothingRange || random.nextFloat() > intensity * 2.0 * particleRate * currentStrength)
					continue;
				final Location particleLoc = new Location(loc.getWorld(), block.getX() + 0.5, loc.getY() + 2.0, block.getZ() + 0.5);
				if (actualDistance <= distanceSquared) {
					player.spawnParticle(Particle.ASH, particleLoc, 2, 5.0, 3.0, 5.0, 1.0);
					player.spawnParticle(Particle.WHITE_ASH, particleLoc, 2, 5.0, 3.0, 5.0, 1.0);
					player.spawnParticle(Particle.WARPED_SPORE, particleLoc, 2, 5.0, 3.0, 5.0, 1.0);
					player.spawnParticle(Particle.SOUL, particleLoc, 1, 5.0, 3.0, 5.0, 1.0);
				} else {
					player.spawnParticle(Particle.ASH, particleLoc, 2, 5.0, 3.0, 5.0, 1.0);
					player.spawnParticle(Particle.WHITE_ASH, particleLoc, 2, 5.0, 3.0, 5.0, 1.0);
					player.spawnParticle(Particle.WARPED_SPORE, particleLoc, 2, 5.0, 3.0, 5.0, 1.0);
				}
			}
			if (soundTick == 0) {
				Location fixed = new Location(loc.getWorld(), loc.getX(), location.getY(), loc.getZ());
				if (fixed.distanceSquared(location) > trueSmoothingRange)
					playSound(player, loc.clone().add(Utils.getVectorTowards(loc, location).multiply(8.0).setY(7)), Sound.WEATHER_RAIN, SoundCategory.WEATHER, ((0.017 / smoothingRangeExcess) * ((smoothingRangeExcess - (fixed.distance(location) - disasterRange - smoothingRange)))) * smoothingIntensity * currentStrength, .5);
				else
					playSound(player, loc.clone().add(0, 3, 0), Sound.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, 0.017 * currentStrength, .5);
			}
		});
	}
	public void clean() {
		super.clean();
		weatherPlayers.forEach(uuid -> {
			Player player = Bukkit.getPlayer(uuid);
			if (player == null || !player.isOnline())
				return;
			player.stopSound(Sound.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS);
			player.stopSound(Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP);
			player.stopSound(Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD);
		});
	}
	public void addPlayerToWeather(Player player) {
		super.addPlayerToWeather(player);
		Location soundLoc = player.getLocation().add(0, 5, 0);
		playSound(player, soundLoc, Sound.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, SoundCategory.WEATHER, 1, .5);
		playSound(player, soundLoc, Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP, SoundCategory.WEATHER, 1, .5);
	}
	public void removePlayerFromWeather(Player player) {
		player.stopSound(Sound.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS);
		player.stopSound(Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP);
		player.stopSound(Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD);
	}
	public boolean forceDownfallWeather() {
		return false;
	}
	protected String getConfigPath() {
		return "disasters.weather.soul_storm";
	}
	public Set<Environment> getBannedEnvironments() {
		return EnumSet.of(Environment.NORMAL, Environment.THE_END);
	}
}
