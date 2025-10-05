package com.github.jewishbanana.deadlydisasters.disasters.weather;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.github.jewishbanana.deadlydisasters.disasters.WeatherDisaster;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class ExtremeWinds extends WeatherDisaster {

	private int minHeight;
	
	private float windForce;
	private float windBreakThreshold;
	private float blockChangeRate;
	private float particleRate;
	private float soundVolume;
	private int minimumYLevel;
	
	private double currentForce;

	public ExtremeWinds(@NotNull Location location, Player player, int level) {
		super(location, player, level);
		
		this.minHeight = getConfigInt("minimum_height");
	}
	public void init() {
		super.init();
		this.windForce = (float) (getConfigDouble("wind_force") * scale);
		this.windBreakThreshold = (float) (getConfigDouble("wind_damage_threshold"));
		this.blockChangeRate = (float) (0.2 * getConfigDouble("block_damage_rate") * (scale / 2.0));
		this.minimumYLevel = getConfigInt("minimum_entity_Y_level");
		
		this.particleRate = (float) (0.03 * particleMultiplier * scale);
		this.soundVolume = (float) (0.1 * scale);
	}
	public boolean canStart() {
		Location loc = getLocation();
		if (loc.getBlockY() < minHeight)
			return false;
		return super.canStart();
	}
	public void start() {
		super.start();
		location.setY(128);
		final Map<Entity, Location> foundEntities = new ConcurrentHashMap<>();
		final Set<Entity> entitiesInStorm = ConcurrentHashMap.newKeySet();
		final World world = location.getWorld();
		final Vector direction = Utils.getRandomizedVector().setY(0).normalize().setY(Math.min(0.2 * scale, 0.6));
		scheduleTask(new BukkitRunnable() {
			private final double increment = windForce / 60.0;
			private boolean increasing = true;
			private final double minNegative = -(windForce / 60.0 * 25.0);
			private final double maxNegative = minNegative * 2.5;
			private double currentNegative;
			
			@Override
			public void run() {
				foundEntities.clear();
				for (Entity entity : world.getNearbyEntities(location, disasterRange, 193, disasterRange, e -> e.isValid()))
					foundEntities.put(entity, entity.getLocation().add(0, entity.getHeight() / 2.0, 0));
				final Set<Entity> currentEntities = Set.copyOf(entitiesInStorm);
				final Vector currentVelocity = direction.clone().multiply(currentForce);
				if (currentForce > 0)
					for (Entity entity : currentEntities) {
						if (entity instanceof Player player) {
							Location loc = player.getLocation();
							if ((EntityUtils.isPlayerImmune(player) && player.isFlying())
									|| loc.clone().add(0, player.getHeight(), 0).getBlock().isLiquid() || !EntityUtils.isLocationExposedToOutdoors(loc.clone().add(0, player.getHeight() / 2.0, 0), 8.0))
								continue;
						}
						entity.setVelocity(entity.getVelocity().add(currentVelocity));
					}
				if (increasing) {
					currentForce += increment;
					if (currentForce >= windForce) {
						currentForce = windForce;
						increasing = false;
						currentNegative = random.nextDouble(maxNegative, minNegative);
					}
				} else {
					currentForce -= increment;
					if (currentForce <= currentNegative) {
						currentForce = currentNegative;
						increasing = true;
					}
				}
				if (time-- <= 0)
					stop();
			}
		}.runTaskTimer(plugin, 0, 1));
		scheduleTask(new BukkitRunnable() {
			private final double radiusSquared = disasterRange * disasterRange;
			private final Set<Entity> set = ConcurrentHashMap.newKeySet();
			private final Vector opposite = direction.clone().multiply(-1).setY(0.1);
			
			@Override
			public void run() {
				set.clear();
				Map.copyOf(foundEntities).forEach((entity, loc) -> {
					if (loc.getY() < minimumYLevel || !Utils.isLocationsWithinDistance(new Location(loc.getWorld(), loc.getX(), location.getY(), loc.getZ()), location, radiusSquared))
						return;
					if (isEntityProtected(entity))
						return;
					if (!loc.clone().add(0, entity.getHeight(), 0).getBlock().isLiquid() && EntityUtils.isLocationExposedToOutdoors(loc, 6.0))
						set.add(entity);
					if (currentForce >= windBreakThreshold && entity instanceof Player p)
						for (int i=0; i < 3; i++)
							if (random.nextFloat() < blockChangeRate)
								for (int j=0; j < 3; j++) {
									Block block = BlockUtils.rayTraceForBlock(p.getLocation().add(0, p.getHeight() / 2.0, 0), opposite.clone().add(new Vector(random.nextFloat(-.5f, .5f), random.nextFloat(-.1f, .8f), random.nextFloat(-.5f, .5f))), 7.0);
									if (block != null) {
										new BukkitRunnable() {
											@Override
											public void run() {
												FallingBlock fb = convertBlockIntoFallingBlock(block);
												if (fb == null)
													return;
												fb.setVelocity(direction.clone().multiply(0.8));
												fb.setHurtEntities(true);
												fb.setDropItem(false);
												entitiesInStorm.add(fb);
											}
										}.runTaskLater(plugin, random.nextInt(10));
										break;
									}
								}
				});
				entitiesInStorm.clear();
				entitiesInStorm.addAll(set);
			}
		}.runTaskTimerAsynchronously(plugin, 1, 10));
		
		final double distanceSquared = disasterRange * disasterRange;
		final double trueSmoothingRange = (disasterRange + smoothingRange) * (disasterRange + smoothingRange);
		final double soundIncrement = 1.0 / windForce;
		createParticleAsyncTask(player -> {
			if (currentForce <= 0)
				return;
			final Location loc = player.getLocation();
			Block closest = null;
			double closestDistance = 0;
			final boolean flag = entitiesInStorm.contains(player);
			final double currentSoundLevel = Utils.clamp(soundIncrement * currentForce, 0.0, 1.0);
			for (Block block : BlockUtils.getBlocksInCircleRadius(loc, particleRenderDistance)) {
				if (new Location(block.getWorld(), block.getX() + 0.5, location.getY(), block.getZ() + 0.5).distanceSquared(location) > distanceSquared 
						|| random.nextFloat() > particleRate * currentStrength * currentSoundLevel)
					continue;
				Block highest = new Location(block.getWorld(), block.getX(), block.getWorld().getHighestBlockYAt(block.getX(), block.getZ()), block.getZ()).getBlock();
				if (highest == null 
						|| highest.getY() - loc.getBlockY() > 10)
					continue;
				final Location particleLoc = new Location(loc.getWorld(), block.getX() + 0.5, (loc.getY() > highest.getY() ? loc.getY() : highest.getY() + 3) + 3, block.getZ() + 0.5);
				player.spawnParticle(Particle.CLOUD, particleLoc.clone().add(random.nextFloat()-.5, random.nextFloat(10f) - 5, random.nextFloat()-.5), 0, direction.getX(), .001, direction.getZ(), currentForce * 15.0);
				double dist = highest.getLocation().distanceSquared(loc);
				if (closest == null || dist < closestDistance) {
					closest = highest;
					closestDistance = dist;
				}
			}
			if (flag)
				for (int i=0; i < particleRate * 100 * currentSoundLevel; i++)
					player.spawnParticle(Particle.CLOUD, loc.clone().add(random.nextFloat(-10f, 10f), random.nextFloat(10f) - 3, random.nextFloat(-10f, 10f)), 0, direction.getX(), .001, direction.getZ(), currentForce * 15.0);
			if (closest != null) {
				playSound(player, loc.clone().add(0, 5, 0), flag ? Sound.WEATHER_RAIN : Sound.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, soundVolume * currentSoundLevel * currentStrength * (flag ? 1.0 : 0.2), .5);
			}
		}, pair -> {
			if (currentForce <= 0)
				return;
			final Player player = pair.getFirst();
			final double intensity = pair.getSecond();
			final Location loc = player.getLocation();
			boolean soundFlag = false;
			boolean aboveFlag = false;
			final double currentSoundLevel = Utils.clamp(soundIncrement * currentForce, 0.0, 1.0);
			for (Block block : BlockUtils.getBlocksInCircleRadius(loc, particleRenderDistance)) {
				final double actualDistance = new Location(block.getWorld(), block.getX() + 0.5, location.getY(), block.getZ() + 0.5).distanceSquared(location);
				if (actualDistance > trueSmoothingRange || random.nextFloat() > intensity * 2.0 * particleRate * currentStrength * currentSoundLevel)
					continue;
				Block highest = new Location(block.getWorld(), block.getX(), block.getWorld().getHighestBlockYAt(block.getX(), block.getZ()), block.getZ()).getBlock();
				if (highest == null 
						|| highest.getY() - loc.getBlockY() > 10)
					continue;
				aboveFlag = true;
				final Location particleLoc = new Location(loc.getWorld(), block.getX() + 0.5, (loc.getY() > highest.getY() ? loc.getY() : highest.getY() + 3) + 5, block.getZ() + 0.5);
				player.spawnParticle(Particle.CLOUD, particleLoc.clone().add(random.nextFloat()-.5, random.nextFloat(6f) - 3, random.nextFloat()-.5), 0, direction.getX(), .001, direction.getZ(), currentForce * 15.0);
				if (!soundFlag && loc.distanceSquared(BlockUtils.getCenterOfBlock(highest)) <= 25
						|| (loc.getY() > highest.getY() && loc.distanceSquared(new Location(loc.getWorld(), highest.getX() + 0.5, loc.getY(), highest.getZ() + 0.5)) <= 25))
					soundFlag = true;
			}
			if (aboveFlag) {
				Location fixed = new Location(loc.getWorld(), loc.getX(), location.getY(), loc.getZ());
				if (fixed.distanceSquared(location) > trueSmoothingRange)
					playSound(player, loc.clone().add(Utils.getVectorTowards(loc, location).multiply(8.0).setY(7)), Sound.WEATHER_RAIN, SoundCategory.WEATHER, ((soundVolume / smoothingRangeExcess) * ((smoothingRangeExcess - (fixed.distance(location) - disasterRange - smoothingRange)))) * currentSoundLevel * smoothingIntensity * currentStrength, .5);
				else
					playSound(player, loc.clone().add(0, 7, 0), Sound.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, soundVolume * currentSoundLevel * currentStrength, .5);
			}
		});
	}
	public void addPlayerToWeather(Player player) {
		weatherPlayers.add(player.getUniqueId());
	}
	public void removePlayerFromWeather(Player player) {
		player.resetPlayerWeather();
	}
	public boolean forceDownfallWeather() {
		return false;
	}
	protected String getConfigPath() {
		return "disasters.weather.extreme_winds";
	}
	public double getRegenTickRate() {
		return 0.1;
	}
	public Set<Environment> getBannedEnvironments() {
		return Set.of(Environment.NETHER, Environment.THE_END);
	}
	public String getBroadcastMessageConfigPath() {
		return "messages.disaster_broadcasts.extreme_winds.level_"+level;
	}
}
