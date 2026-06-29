package com.github.jewishbanana.deadlydisasters.disasters.weather;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Biome;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.FluidLevelChangeEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.github.jewishbanana.deadlydisasters.DeadlyDisasters;
import com.github.jewishbanana.deadlydisasters.disasters.MobDisaster;
import com.github.jewishbanana.deadlydisasters.disasters.WeatherDisaster;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.SpawnUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

/**
 * A large, radius based cyclonic storm. Conditions worsen toward the center through discrete bands (and optionally a calm
 * "eye" with a violent eyewall ring), forcing rain on exposed players, hammering them with gusting/swirling wind that
 * constantly changes speed and direction, ripping their shelters apart (shattering glass, tearing walls into hurtful
 * falling blocks), striking lightning and occasionally sweeping in drowned (sometimes escorting a cursed diver). Players
 * with enough cover are graded as sheltered and are spared the wind and debris; particles only ever render above terrain
 * so they never appear inside. The heavy work (entity exposure ray tracing, particle rendering, shelter-block selection)
 * runs asynchronously; only velocity application, block edits, lightning and entity spawns touch the main thread.
 */
public class Hurricane extends WeatherDisaster implements MobDisaster, Listener {

	// Hurricanes only form in warm, humid climates (especially jungles/swamps), or when an ocean biome is close enough to
	// the activating player. Surface biomes are used so underground cave biomes do not hide a valid jungle or coast above.
	private static final double JUNGLE_MIN_TEMPERATURE = 0.90;
	private static final double JUNGLE_MIN_HUMIDITY = 0.80;
	private static final double JUNGLE_KEY_MIN_TEMPERATURE = 0.80;
	private static final double JUNGLE_KEY_MIN_HUMIDITY = 0.70;
	// Vanilla frozen oceans are 0.0. Every other vanilla ocean variant is 0.5, so temperature cannot separate cold ocean
	// from warm ocean without falling back to biome keys.
	private static final double WARM_OCEAN_MIN_TEMPERATURE = 0.50;
	private static final double WARM_OCEAN_MIN_HUMIDITY = 0.40;
	private static final int OCEAN_DETECTION_RADIUS = 90;
	private static final int OCEAN_DETECTION_STEP = 6;
	private static final int OCEAN_BODY_CHECK_RADIUS = 24;
	private static final int OCEAN_BODY_CHECK_STEP = 6;
	private static final int OCEAN_GENERIC_CANDIDATE_CAP = 3;
	private static final int OCEAN_MIN_WARM_WATER_SAMPLES = 16;
	private static final int OCEAN_KEY_MIN_WARM_WATER_SAMPLES = 10;
	private static final int OCEAN_MIN_SAMPLE_SPAN = 24;
	private static final int OCEAN_KEY_MIN_SAMPLE_SPAN = 18;
	private static final int CURSED_DIVER_ROLL = 24;
	// When a brittle block shatters it tends to take the neighbouring window blocks with it.
	private static final BlockFace[] SHATTER_FACES = { BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };
	// Keep lightning from striking right beside players — strikes must land at least this far (blocks) from any storm player.
	private static final double LIGHTNING_MIN_PLAYER_DISTANCE_SQ = 18.0 * 18.0;
	private static final double LIGHTNING_NEAR_PLAYER_MIN_DISTANCE = 24.0;
	private static final double LIGHTNING_NEAR_PLAYER_MAX_DISTANCE = 80.0;
	private static final double HURRICANE_MOVE_STEP = 0.05;
	private static final double HURRICANE_MIN_CURVE_RATE = 0.0005;
	private static final double HURRICANE_MAX_CURVE_RATE = 0.0012;
	private static final int PUDDLE_SEED_MIN_DISTANCE = 3;
	private static final int PUDDLE_SEED_MAX_DISTANCE = 110;
	private static final int PUDDLE_SEED_NEAR_MAX_DISTANCE = 70;
	private static final BlockFace[] PUDDLE_SPREAD_FACES = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };
	private static final float WOODEN_ENTRY_BLOW_OPEN_CHANCE = 0.90f;
	private static final BlockData WAVE_WATER = Material.WATER.createBlockData();
	// Most concurrent sweeping waves, and the storm strength at/above which they start forming.
	private static final int WAVE_CAP = 4;
	private static final double WAVE_INTENSITY_THRESHOLD = 0.6;
	// A wave runs up at most a 1-block step; anything taller is a wall it cannot climb (so it never washes into bases). It
	// also will not pour down into anything deeper than WAVE_MAX_DROP (pits, shafts, bunkers).
	private static final int WAVE_MAX_CLIMB = 1;
	private static final int WAVE_MAX_DROP = 4;
	// Cosmetic, particle-only "rain ripple" gusts: bands of spray that roll downwind across the ground surface (purely
	// client-side, streamed off-thread to each player). FREQ = band spacing, SPEED = roll rate, THRESHOLD = how thin the
	// leading edge that renders is, CLIMB = how far spray runs up a wall it meets, HEIGHT_BAND = only render on surfaces
	// within this many blocks of the player's level so ripples stay on nearby open ground (never on roofs or indoors).
	private static final double WAVE_RIPPLE_FREQ = 0.50;
	private static final double WAVE_RIPPLE_SPEED = 0.42;
	private static final double WAVE_RIPPLE_THRESHOLD = 0.55;
	private static final int WAVE_RIPPLE_CLIMB = 4;
	private static final int WAVE_RIPPLE_HEIGHT_BAND = 6;

	private int minHeight;
	private int minimumYLevel;

	private float windForce;
	private float swirlForce;
	private float windBreakThreshold;
	private float blockChangeRate;
	private float shatterChance;
	private float shelterSafeThreshold;
	private float lightningRate;
	private float mobSpawnRate;
	private float particleRate;
	private float soundVolume;
	private double damage;
	private int damageInterval;

	private float puddleSeedRate;
	private float puddleGrowthRate;
	private int puddleCap;
	private int puddleSeedLimit;
	private float puddleDryMultiplier;
	private float waveSpawnRate;
	private float splashRate;

	private boolean eyeEnabled;
	private double eyeFraction;

	private double band15Sq;
	private double band50Sq;
	private double band80Sq;
	private double eyeSq;

	private Set<Material> shatterBlocks;
	private List<PotionEffect> effects;

	private volatile double currentForce;
	private Vector windDirection;
	private volatile Vector windSnapshot;
	private Vector stormTravelDirection;
	private int cycloneRotation;
	private int cursedDiversSpawned;

	private final Map<UUID, Float> exposure = new ConcurrentHashMap<>();
	private final Map<UUID, Long> debrisWindDelay = new ConcurrentHashMap<>();
	private final ConcurrentLinkedQueue<ShelterStrike> ripQueue = new ConcurrentLinkedQueue<>();
	private final Set<Block> puddles = ConcurrentHashMap.newKeySet();
	private final List<List<Block>> puddlePools = new ArrayList<>();
	private final Set<Block> waveBlocks = ConcurrentHashMap.newKeySet();
	private final List<Wave> waves = new ArrayList<>();
	private volatile Set<UUID> drownedInRainArea = Set.of();
	private final Set<UUID> aggressiveDrowned = new HashSet<>();

	public Hurricane(@NotNull Location location, Player player, int level) {
		super(location, player, level);

		this.minHeight = getConfigInt("min_height");
	}
	public void init() {
		super.init();
		this.windForce = (float) (getConfigDouble("wind_force") * scale);
		this.swirlForce = (float) (getConfigDouble("swirl_force") * scale);
		this.windBreakThreshold = (float) getConfigDouble("wind_damage_threshold");
		this.blockChangeRate = (float) (0.12 * getConfigDouble("block_damage_rate") * (scale / 2.0));
		this.shatterChance = (float) Utils.clamp(getConfigDouble("glass_shatter_chance"), 0.0, 1.0);
		this.shelterSafeThreshold = (float) Utils.clamp(getConfigDouble("shelter_safe_threshold"), 0.0, 1.0);
		this.minimumYLevel = getConfigInt("minimum_entity_Y_level");
		this.lightningRate = (float) (0.045 * getConfigDouble("lightning_frequency") * scale);
		this.mobSpawnRate = (float) (0.015 * getConfigDouble("mob_spawn_multiplier") * (scale / 1.5));
		this.damage = getConfigDouble("damage");
		this.damageInterval = Math.max(1, (int) (20.0 / Math.max(0.25, getConfigDouble("damage_multiplier"))));

		this.eyeEnabled = getConfigBoolean("eye.enabled");
		this.eyeFraction = Utils.clamp(getConfigDouble("eye.radius_fraction"), 0.0, 0.14);

		this.effects = buildPotionEffects("entity_effects");
		this.shatterBlocks = EnumSet.noneOf(Material.class);
		for (String entry : getConfigStringList("shatter_blocks")) {
			Set<Material> materials = BlockUtils.getMaterials(entry);
			if (materials == null) {
				Utils.sendConsoleMessage("&cERROR the block type or category &d'"+entry+"' &cdoes not exist in the world disaster config &b'"+getWorldLink().getConfigName()+"' &cat the section &c'"+getConfigPath()+".shatter_blocks'&c!");
				continue;
			}
			shatterBlocks.addAll(materials);
		}

		this.particleRate = (float) (0.05 * particleMultiplier * scale);
		this.soundVolume = (float) (0.18 * scale);
		this.soundTickRate = 15;

		// Puddles are deliberately sparser than the monsoon — a few seeds that grow and spread outward rather than a flood.
		final double puddleSpawnMultiplier = Math.max(0.0, getConfigDouble("puddle_spawn_rate"));
		this.puddleSeedRate = (float) Utils.clamp(0.14625 * puddleSpawnMultiplier * scale, 0.0, 1.0);
		this.puddleGrowthRate = (float) ((0.75 + (level * 0.18)) * Math.max(0.0, getConfigDouble("puddle_grow_rate")));
		this.puddleCap = (int) Math.round((90 + (level * 36)) * puddleSpawnMultiplier * scale);
		this.puddleSeedLimit = Math.max(1, (int) Math.round(level * 12.0 * puddleSpawnMultiplier));
		this.puddleDryMultiplier = (float) Math.max(0.05, getConfigDouble("puddle_dry_rate"));
		this.waveSpawnRate = (float) Utils.clamp(0.06 * getConfigDouble("wave_multiplier") * (scale / 2.0), 0.0, 1.0);
		this.splashRate = (float) (0.5 * particleMultiplier * scale);

		final double r = disasterRange;
		this.band15Sq = (r * 0.15) * (r * 0.15);
		this.band50Sq = (r * 0.50) * (r * 0.50);
		this.band80Sq = (r * 0.80) * (r * 0.80);
		this.eyeSq = (r * eyeFraction) * (r * eyeFraction);
	}
	public boolean canStart() {
		final Location loc = getLocation();
		if (loc.getBlockY() < minHeight)
			return false;
		final Player player = getPlayer();
		final Location climateLoc = player != null && player.getWorld().equals(loc.getWorld()) ? player.getLocation() : loc;
		if (!isApplicableArea(climateLoc))
			return false;
		return super.canStart();
	}
	private static boolean isApplicableArea(Location loc) {
		return loc != null && loc.getWorld() != null
				&& (isHurricaneClimate(loc) || hasOceanWithin(loc, OCEAN_DETECTION_RADIUS));
	}
	/**
	 * A hurricane may only form in a warm, humid surface climate or within range of an ocean biome. Surface biome checks
	 * keep underground cave biomes from masking jungles and coastlines.
	 */
	private static boolean isHurricaneClimate(Location loc) {
		final World world = loc.getWorld();
		final int x = loc.getBlockX();
		final int z = loc.getBlockZ();
		final int surfaceY = world.getHighestBlockYAt(x, z);
		final double temperature = world.getTemperature(x, surfaceY, z);
		final double humidity = world.getHumidity(x, surfaceY, z);
		if (temperature >= JUNGLE_MIN_TEMPERATURE && humidity >= JUNGLE_MIN_HUMIDITY)
			return true;
		return isJungleBiome(world.getBiome(x, surfaceY, z))
				&& temperature >= JUNGLE_KEY_MIN_TEMPERATURE && humidity >= JUNGLE_KEY_MIN_HUMIDITY;
	}
	private static boolean hasOceanWithin(Location center, int radius) {
		final World world = center.getWorld();
		final int centerX = center.getBlockX();
		final int centerZ = center.getBlockZ();
		final int radiusSq = radius * radius;
		int[] oceanKeyCandidate = null;
		int oceanKeyDistanceSq = Integer.MAX_VALUE;
		final List<int[]> genericCandidates = new ArrayList<>(OCEAN_GENERIC_CANDIDATE_CAP);
		for (int dx = -radius; dx <= radius; dx += OCEAN_DETECTION_STEP)
			for (int dz = -radius; dz <= radius; dz += OCEAN_DETECTION_STEP) {
				final int distanceSq = dx * dx + dz * dz;
				if (distanceSq > radiusSq)
					continue;
				final int x = centerX + dx;
				final int z = centerZ + dz;
				final Biome biome = getWarmSurfaceWaterBiome(world, x, z);
				if (biome == null)
					continue;
				if (isOceanBiome(biome)) {
					if (distanceSq < oceanKeyDistanceSq) {
						oceanKeyCandidate = new int[] {x, z};
						oceanKeyDistanceSq = distanceSq;
					}
					continue;
				}
				boolean separated = true;
				for (int[] candidate : genericCandidates) {
					final int cdx = x - candidate[0];
					final int cdz = z - candidate[1];
					if (cdx * cdx + cdz * cdz < OCEAN_BODY_CHECK_RADIUS * OCEAN_BODY_CHECK_RADIUS) {
						separated = false;
						if (distanceSq < candidate[2]) {
							candidate[0] = x;
							candidate[1] = z;
							candidate[2] = distanceSq;
						}
						break;
					}
				}
				if (!separated)
					continue;
				if (genericCandidates.size() < OCEAN_GENERIC_CANDIDATE_CAP) {
					genericCandidates.add(new int[] {x, z, distanceSq});
					continue;
				}
				int farthestIndex = 0;
				for (int i = 1; i < genericCandidates.size(); i++)
					if (genericCandidates.get(i)[2] > genericCandidates.get(farthestIndex)[2])
						farthestIndex = i;
				if (distanceSq < genericCandidates.get(farthestIndex)[2])
					genericCandidates.set(farthestIndex, new int[] {x, z, distanceSq});
			}
		if (oceanKeyCandidate != null && isLargeWarmWaterBody(world, oceanKeyCandidate[0], oceanKeyCandidate[1], true))
			return true;
		for (int[] candidate : genericCandidates)
			if (isLargeWarmWaterBody(world, candidate[0], candidate[1], false))
				return true;
		return false;
	}
	@SuppressWarnings("deprecation")
	private static Biome getWarmSurfaceWaterBiome(World world, int x, int z) {
		final int surfaceY = world.getHighestBlockYAt(x, z);
		if (world.getBlockAt(x, surfaceY, z).getType() != Material.WATER)
			return null;
		final Biome biome = world.getBiome(x, surfaceY, z);
		if (biome.getKey().getKey().contains("river"))
			return null;
		final double temperature = world.getTemperature(x, surfaceY, z);
		final double humidity = world.getHumidity(x, surfaceY, z);
		return temperature >= WARM_OCEAN_MIN_TEMPERATURE && humidity >= WARM_OCEAN_MIN_HUMIDITY ? biome : null;
	}
	private static boolean isLargeWarmWaterBody(World world, int centerX, int centerZ, boolean oceanKeyHint) {
		final int radiusSq = OCEAN_BODY_CHECK_RADIUS * OCEAN_BODY_CHECK_RADIUS;
		final int requiredSamples = oceanKeyHint ? OCEAN_KEY_MIN_WARM_WATER_SAMPLES : OCEAN_MIN_WARM_WATER_SAMPLES;
		final int requiredSpan = oceanKeyHint ? OCEAN_KEY_MIN_SAMPLE_SPAN : OCEAN_MIN_SAMPLE_SPAN;
		int samples = 0;
		int minDx = Integer.MAX_VALUE, maxDx = Integer.MIN_VALUE;
		int minDz = Integer.MAX_VALUE, maxDz = Integer.MIN_VALUE;
		for (int dx = -OCEAN_BODY_CHECK_RADIUS; dx <= OCEAN_BODY_CHECK_RADIUS; dx += OCEAN_BODY_CHECK_STEP)
			for (int dz = -OCEAN_BODY_CHECK_RADIUS; dz <= OCEAN_BODY_CHECK_RADIUS; dz += OCEAN_BODY_CHECK_STEP) {
				if (dx * dx + dz * dz > radiusSq || getWarmSurfaceWaterBiome(world, centerX + dx, centerZ + dz) == null)
					continue;
				samples++;
				minDx = Math.min(minDx, dx);
				maxDx = Math.max(maxDx, dx);
				minDz = Math.min(minDz, dz);
				maxDz = Math.max(maxDz, dz);
				if (samples >= requiredSamples && maxDx - minDx >= requiredSpan && maxDz - minDz >= requiredSpan)
					return true;
			}
		return false;
	}
	@SuppressWarnings("deprecation")
	private static boolean isJungleBiome(Biome biome) {
		return biome.getKey().getKey().contains("jungle");
	}
	@SuppressWarnings("deprecation")
	private static boolean isOceanBiome(Biome biome) {
		return biome.getKey().getKey().contains("ocean");
	}
	/**
	 * How violent the storm is at a given squared distance from the center, on a {@code 0.0}-{@code 1.0} scale. Worst in
	 * the inner 15% (or the eyewall ring when an eye is enabled), very bad to 50%, bad to 80%, then a smooth ease to zero
	 * through the outer 20%. With the eye enabled the dead center is calm and ramps up to the eyewall peak.
	 */
	private double zoneIntensity(double distSq) {
		if (distSq >= disasterRangeSquared)
			return 0.0;
		if (eyeEnabled && eyeSq > 0 && distSq <= eyeSq) {
			final double t = Math.sqrt(distSq / eyeSq);
			return 0.15 + (0.85 * t);
		}
		if (distSq <= band15Sq)
			return 1.0;
		if (distSq <= band50Sq)
			return 0.8;
		if (distSq <= band80Sq)
			return 0.55;
		final double inner = disasterRange * 0.80;
		double t = (Math.sqrt(distSq) - inner) / (disasterRange - inner);
		if (t < 0)
			t = 0;
		else if (t > 1)
			t = 1;
		return 0.4 * (1.0 - (t * t * (3 - (2 * t))));
	}
	/**
	 * Keeps visible rain and wind particles continuous across the core/smoothing boundary. The smoothing area rises from
	 * zero at its outer edge to 0.4 at the storm radius, then the outer storm band rises from 0.4 to 0.55.
	 */
	private double particleIntensity(Location loc) {
		final double dx = loc.getX() - location.getX();
		final double dz = loc.getZ() - location.getZ();
		final double distance = Math.sqrt((dx * dx) + (dz * dz));
		final double outerRadius = disasterRange + smoothingRange + smoothingRangeExcess;
		if (distance >= outerRadius)
			return 0.0;
		if (distance >= disasterRange) {
			final double t = Utils.clamp((outerRadius - distance) / (outerRadius - disasterRange), 0.0, 1.0);
			final double smooth = t * t * (3.0 - (2.0 * t));
			return 0.4 * smooth;
		}
		final double outerBandStart = disasterRange * 0.8;
		if (distance >= outerBandStart) {
			final double t = Utils.clamp((disasterRange - distance) / (disasterRange - outerBandStart), 0.0, 1.0);
			final double smooth = t * t * (3.0 - (2.0 * t));
			return 0.4 + (0.15 * smooth);
		}
		return zoneIntensity((dx * dx) + (dz * dz));
	}
	/**
	 * Builds the local horizontal wind vector. Inner bands retain the changing background gust direction, while the outer
	 * half increasingly blends into a rotating inward spiral that draws entities toward the more dangerous storm rings.
	 */
	private Vector getWindVectorAt(Location loc, Vector backgroundDirection, double force) {
		final double dx = location.getX() - loc.getX();
		final double dz = location.getZ() - loc.getZ();
		final double distance = Math.sqrt((dx * dx) + (dz * dz));
		if (distance < 1.0E-4)
			return backgroundDirection.clone().multiply(force);
		final Vector inward = new Vector(dx / distance, 0, dz / distance);
		final Vector tangent = new Vector(-inward.getZ() * cycloneRotation, 0, inward.getX() * cycloneRotation);
		final Vector spiral = inward.clone().multiply(0.94).add(tangent.clone().multiply(0.34)).normalize();
		final double outerBlend = Utils.clamp(((distance / disasterRange) - 0.45) / 0.45, 0.0, 1.0);
		final Vector background = backgroundDirection.clone().multiply(force * (1.0 - outerBlend));
		final Vector inwardGust = spiral.multiply(Math.abs(force) * outerBlend);
		final double localSwirl = swirlForce * (1.0 - (outerBlend * 0.55));
		return background.add(inwardGust).add(tangent.multiply(localSwirl));
	}
	public void start() {
		super.start();
		location.setY(128);
		this.windDirection = Utils.getRandomizedVector().setY(0).normalize();
		this.windSnapshot = windDirection.clone();
		this.stormTravelDirection = Utils.getRandomizedVector().setY(0).normalize();
		this.cycloneRotation = random.nextBoolean() ? 1 : -1;

		// Slowly walk the storm center along a changing curve. Replacing the Location object keeps each center update atomic
		// for the asynchronous weather/render tasks that read it.
		scheduleTask(new BukkitRunnable() {
			private double curveRate = random.nextDouble(HURRICANE_MIN_CURVE_RATE, HURRICANE_MAX_CURVE_RATE) * cycloneRotation;
			private int curveTicks = random.nextInt(80, 161);

			@Override
			public void run() {
				if (--curveTicks <= 0) {
					curveRate = random.nextDouble(HURRICANE_MIN_CURVE_RATE, HURRICANE_MAX_CURVE_RATE)
							* (random.nextBoolean() ? 1 : -1);
					curveTicks = random.nextInt(80, 161);
				}
				stormTravelDirection.rotateAroundY(curveRate).setY(0).normalize();
				final Location next = location.clone().add(stormTravelDirection.clone().multiply(HURRICANE_MOVE_STEP));
				next.setY(128);
				setLocation(next);
			}
		}.runTaskTimer(plugin, 5, 5));

		// Main thread: apply gusting/swirling wind to exposed entities, drain the shelter-block queue, ramp/drift the gust.
		scheduleTask(new BukkitRunnable() {
			private final double baseIncrement = windForce / 50.0;
			private double increment = baseIncrement;
			private boolean increasing = true;
			private double gustPeak = windForce;
			private double gustFloor = 0;
			private int damageTick;

			@Override
			public void run() {
				int drained = 0;
				ShelterStrike strike;
				while (drained < 16 && (strike = ripQueue.poll()) != null) {
					applyShelterStrike(strike);
					drained++;
				}
				final boolean damaging = damage > 0 && (++damageTick % damageInterval == 0);
				for (Entity entity : entitiesInMonitorArea) {
					if (!entity.isValid())
						continue;
					final Long windDelay = debrisWindDelay.get(entity.getUniqueId());
					if (windDelay != null) {
						if (windDelay > System.currentTimeMillis())
							continue;
						debrisWindDelay.remove(entity.getUniqueId(), windDelay);
					}
					// Players in flight (creative flight or gliding) ride above the storm and are not shoved by the wind.
					if (entity instanceof Player p && (p.isFlying() || p.isGliding()))
						continue;
					final Location loc = entity.getLocation();
					final double dx = loc.getX() - location.getX();
					final double dz = loc.getZ() - location.getZ();
					final double intensity = zoneIntensity((dx * dx) + (dz * dz));
					if (intensity <= 0)
						continue;
					float exp = 1f;
					if (entity instanceof Player p) {
						final Float stored = exposure.get(p.getUniqueId());
						exp = stored == null ? 0f : stored;
						if (exp < shelterSafeThreshold)
							continue;
					}
					final Vector vel = getWindVectorAt(loc, windDirection, currentForce).multiply(intensity * exp);
					final Vector current = entity.getVelocity();
					entity.setVelocity(current.setY(Math.min(current.getY(), 0.3)).add(vel.setY(Math.min(vel.getY(), 0.4))));
					if (damaging && entity instanceof LivingEntity living && !EntityUtils.isEntityImmunePlayer(entity) && intensity >= 0.55) {
						if (!effects.isEmpty())
							living.addPotionEffects(effects);
						EntityUtils.pureDamageEntity(living, damage * intensity * currentStrength, "deaths.hurricane", DamageCause.FLY_INTO_WALL);
					}
				}
				if (increasing) {
					currentForce += increment;
					if (currentForce >= gustPeak) {
						currentForce = gustPeak;
						increasing = false;
					}
				} else {
					currentForce -= increment;
					if (currentForce <= gustFloor) {
						currentForce = gustFloor;
						increasing = true;
						// A fresh gust: randomize how hard and how fast it builds, occasionally lull/reverse, and drift the
						// wind direction cyclonically so the storm constantly speeds up, slows down and switches direction.
						gustPeak = windForce * random.nextDouble(0.5, 1.0);
						gustFloor = -windForce * random.nextDouble(0.0, 0.4);
						increment = baseIncrement * random.nextDouble(0.6, 1.8);
						windDirection.rotateAroundY(random.nextDouble(0.3, 1.0) * (random.nextBoolean() ? 1 : -1)).setY(0).normalize();
					}
				}
				windSnapshot = windDirection.clone();
			}
		}.runTaskTimer(plugin, 0, 1));

		// Async: classify entities as sheltered (graded for players) or exposed. Shelter-block tearing is handled by the
		// dedicated async scan task below.
		final Map<UUID, Integer> timeInStorm = new HashMap<>();
		createAsyncEntityMonitor(weatherEffectsRange,
				entity -> entity.isValid() && (entity instanceof Drowned || isWithinStorm(entity.getLocation())),
				(found, entities, players) -> {
					final Map<UUID, Float> newExposure = new HashMap<>();
					final Set<UUID> newDrownedInRainArea = new HashSet<>();
					found.forEach((entity, loc) -> {
						if (entity instanceof Drowned) {
							final double rainDx = loc.getX() - location.getX();
							final double rainDz = loc.getZ() - location.getZ();
							if ((rainDx * rainDx) + (rainDz * rainDz) <= weatherEffectsRange * weatherEffectsRange)
								newDrownedInRainArea.add(entity.getUniqueId());
						}
						if (loc.getY() < minimumYLevel || !isWithinStorm(loc))
							return;
						if (isEntityProtected(entity))
							return;
						if (world.getBlockAt(loc.getBlockX(), loc.getBlockY() + (int) Math.ceil(entity.getHeight()), loc.getBlockZ()).isLiquid())
							return;
						final boolean isPlayer = entity instanceof Player;
						final boolean exposedEnough;
						if (isPlayer) {
							final float exposureScore = Utils.getOutdoorExposureScore(loc, 12f, 12);
							exposedEnough = exposureScore >= shelterSafeThreshold;
							newExposure.put(entity.getUniqueId(), exposureScore);
						} else
							exposedEnough = Utils.isLocationExposedToOutdoorsOptimized(loc, 8f, 6);
						if (exposedEnough) {
							final int time = timeInStorm.compute(entity.getUniqueId(), (key, old) -> Math.min((old != null ? old : 0) + 1, 15));
							if (time > 5) {
								entities.add(entity);
								if (isPlayer) {
									final Player p = (Player) entity;
									players.add(p);
								}
								return;
							}
						} else
							timeInStorm.computeIfPresent(entity.getUniqueId(), (key, old) -> {
								final int next = old - 5;
								return next > 0 ? next : null;
							});
						if (isPlayer)
							players.add((Player) entity);
					});
					exposure.keySet().retainAll(newExposure.keySet());
					exposure.putAll(newExposure);
					drownedInRainArea = Set.copyOf(newDrownedInRainArea);
				});

		// Goal selectors are a server-thread API. The entity snapshot is prepared by the async monitor above, then this
		// low-frequency reconciliation adds or removes only UltimateContent's temporary hurricane goals.
		scheduleTask(new BukkitRunnable() {
			@Override
			public void run() {
				if (!DependencyUtils.isUltimateContentEnabled())
					return;
				final Set<UUID> raining = drownedInRainArea;
				for (UUID uuid : raining) {
					final Entity entity = Bukkit.getEntity(uuid);
					if (entity instanceof Drowned drowned && drowned.isValid() && aggressiveDrowned.add(uuid))
						DependencyUtils.manipulateDrownedGoals(drowned, true);
				}
				for (Iterator<UUID> iterator = aggressiveDrowned.iterator(); iterator.hasNext();) {
					final UUID uuid = iterator.next();
					if (raining.contains(uuid))
						continue;
					final Entity entity = Bukkit.getEntity(uuid);
					if (entity instanceof Drowned drowned)
						DependencyUtils.manipulateDrownedGoals(drowned, false);
					iterator.remove();
				}
			}
		}.runTaskTimer(plugin, 0, 20));

		// Async: scan the shelter around every storm player, including players still safely enclosed. Ring intensity, storm
		// strength and the live gust determine how quickly the outside layer is stripped away.
		scheduleTask(new BukkitRunnable() {
			private double horizontalAngle;

			@Override
			public void run() {
				final double gustStrength = Utils.clamp(Math.abs(currentForce) / Math.max(0.0001, windForce), 0.0, 1.0);
				if (currentStrength <= 0 || Math.abs(currentForce) < windBreakThreshold || ripQueue.size() > 256)
					return;
				final ThreadLocalRandom rng = ThreadLocalRandom.current();
				for (Map.Entry<UUID, Float> entry : exposure.entrySet()) {
					final Player player = Bukkit.getPlayer(entry.getKey());
					if (player == null || !player.isValid())
						continue;
					final Location loc = player.getEyeLocation();
					final double dx = loc.getX() - location.getX();
					final double dz = loc.getZ() - location.getZ();
					final double intensity = zoneIntensity((dx * dx) + (dz * dz));
					if (intensity <= 0)
						continue;
					final double shelterMultiplier = entry.getValue() < shelterSafeThreshold ? 1.2 : 1.0;
					final double pressure = blockChangeRate * intensity * intensity * currentStrength * gustStrength * shelterMultiplier;
					final int rays = Math.max(1, (int) Math.ceil(pressure * 12.0));
					if (rng.nextDouble() < Math.min(0.65, pressure * 2.0)) {
						final Vector roofDirection = new Vector(rng.nextDouble(-0.16, 0.16), 1.0, rng.nextDouble(-0.16, 0.16));
						final int[] roofHit = findOutermostExposedBlock(loc, roofDirection, 12.0);
						if (roofHit != null)
							ripQueue.add(new ShelterStrike(roofHit[0], roofHit[1], roofHit[2], loc.getX(), loc.getY(), loc.getZ()));
					}
					for (int i = 0; i < rays; i++) {
						final Vector dir;
						if (i == 0) {
							// Sweep around the walls at window height instead of relying entirely on random roof-biased rays.
							horizontalAngle += 2.399963229728653;
							dir = new Vector(Math.cos(horizontalAngle), rng.nextDouble(-0.12, 0.18), Math.sin(horizontalAngle));
						} else
							dir = new Vector(rng.nextDouble(-1, 1), rng.nextDouble(-0.2, 0.9), rng.nextDouble(-1, 1));
						if (dir.lengthSquared() < 1.0E-4)
							continue;
						final int[] hit = findOutermostExposedBlock(loc, dir, 14.0);
						if (hit != null)
							ripQueue.add(new ShelterStrike(hit[0], hit[1], hit[2], loc.getX(), loc.getY(), loc.getZ()));
					}
				}
			}
		}.runTaskTimerAsynchronously(plugin, 0, 4));

		// Slow tick: sweep in drowned near exposed players, occasionally escorting a cursed diver.
		scheduleTask(new BukkitRunnable() {
			@Override
			public void run() {
				for (Player player : playersInMonitorArea) {
					if (!player.isValid() || EntityUtils.isPlayerImmune(player))
						continue;
					if (random.nextFloat() >= mobSpawnRate)
						continue;
					final Location spawn = SpawnUtils.findMonsterSpawnLocation(player.getLocation(), 2, SpawnUtils.MIN_SPAWN_DISTANCE_FROM_PLAYERS, 40);
					if (spawn == null)
						continue;
					boolean spawnedDiver = false;
					if (DependencyUtils.isUltimateContentEnabled()
							&& cursedDiversSpawned < Math.max(1, level / 2)
							&& random.nextInt(CURSED_DIVER_ROLL) == 0) {
						final Mob diver = com.github.jewishbanana.uiframework.entities.UIEntityManager.spawnEntity(spawn, com.github.jewishbanana.ultimatecontent.entities.waterentities.CursedDiver.class).getCastedEntity();
						if (diver != null) {
							cursedDiversSpawned++;
							spawnedDiver = true;
							addEntityToDisasterList(diver, player);
							entitiesInMonitorArea.add(diver);
						}
					}
					final double extraDrownedChance = Math.min(0.65, 0.15 + (level * 0.08));
					final int drownedCount = spawnedDiver ? 1 : 1 + (random.nextDouble() < extraDrownedChance ? 1 : 0);
					for (int i = 0; i < drownedCount; i++) {
						final Mob drowned = world.spawn(spawn, Drowned.class);
						addEntityToDisasterList(drowned, player);
						entitiesInMonitorArea.add(drowned);
					}
				}
				updateEntityTargets();
			}
		}.runTaskTimer(plugin, 0, 10));

		// Main thread: lightning, spread across the storm (area-uniform) but kept a wide radius away from players.
		scheduleTask(new BukkitRunnable() {
			@Override
			public void run() {
				if (currentStrength <= 0)
					return;
				final ThreadLocalRandom rng = ThreadLocalRandom.current();
				final int attempts = Math.max(1, Math.round(lightningRate * (float) currentStrength * (level + 2)));
				strike:
				for (int i = 0; i < attempts; i++) {
					final double angle = rng.nextDouble(0, Math.PI * 2);
					final int bx;
					final int bz;
					// Most strikes are anchored near storm players so the increased rate is visible, while the rest remain
					// spread across the whole storm. The exclusion check below still keeps bolts safely away from players.
					if (!playersInMonitorArea.isEmpty() && rng.nextFloat() < 0.6f) {
						final Location anchor = playersInMonitorArea.get(rng.nextInt(playersInMonitorArea.size())).getLocation();
						final double dist = rng.nextDouble(LIGHTNING_NEAR_PLAYER_MIN_DISTANCE, LIGHTNING_NEAR_PLAYER_MAX_DISTANCE);
						bx = anchor.getBlockX() + (int) (Math.cos(angle) * dist);
						bz = anchor.getBlockZ() + (int) (Math.sin(angle) * dist);
					} else {
						// Area-uniform radius (sqrt) so these strikes spread across the whole storm instead of clustering inward.
						final double dist = disasterRange * Math.sqrt(rng.nextDouble());
						bx = location.getBlockX() + (int) (Math.cos(angle) * dist);
						bz = location.getBlockZ() + (int) (Math.sin(angle) * dist);
					}
					final double dx = bx + 0.5 - location.getX();
					final double dz = bz + 0.5 - location.getZ();
					if (rng.nextFloat() >= zoneIntensity((dx * dx) + (dz * dz)))
						continue;
					// Keep strikes a wide radius away from players so they aren't constantly struck right beside them.
					for (Player player : playersInMonitorArea) {
						final Location ploc = player.getLocation();
						final double pdx = bx + 0.5 - ploc.getX();
						final double pdz = bz + 0.5 - ploc.getZ();
						if ((pdx * pdx) + (pdz * pdz) < LIGHTNING_MIN_PLAYER_DISTANCE_SQ)
							continue strike;
					}
					final Block highest = world.getHighestBlockAt(bx, bz);
					world.strikeLightning(highest.getLocation().add(0, 1, 0));
				}
			}
		}.runTaskTimer(plugin, 0, 20));

		// Async particles + rain ambience, scaled by zone intensity, always rendered above terrain (never indoors).
		final double soundIncrement = 1.0 / Math.max(0.0001, windForce);
		final double trueSmoothingRange = (disasterRange + smoothingRange) * (disasterRange + smoothingRange);
		createParticleAsyncTask(player -> {
			final ThreadLocalRandom rng = ThreadLocalRandom.current();
			final Location loc = player.getLocation();
			Vector wind = getWindVectorAt(loc, windSnapshot, currentForce);
			wind = wind.lengthSquared() < 1.0E-4 ? windSnapshot.clone() : wind.normalize();
			final double force = Math.abs(currentForce);
			final double soundLevel = Utils.clamp(soundIncrement * force, 0.0, 1.0);
			final boolean inside = isPlayerExposed(player);
			final double playerIntensity = particleIntensity(loc);
			final double upwindDistance = 2.5 + (playerIntensity * 6.0);
			final double upwindX = wind.getX() * (-upwindDistance + 2.0);
			final double upwindZ = wind.getZ() * (-upwindDistance + 2.0);
			final double particleSpeed = ((force * 15.0) + 0.5) * (0.35 + (playerIntensity * 0.65));
			Block closest = null;
			double closestDistance = 0;
			for (Block block : BlockUtils.getBlocksInCircleRadius(loc, particleRenderDistance)) {
				if (playerIntensity <= 0 || rng.nextFloat() >= particleRate * playerIntensity * currentStrength)
					continue;
				final Block highest = world.getHighestBlockAt(block.getX(), block.getZ());
				if (highest == null || highest.getY() - loc.getBlockY() > 12)
					continue;
				final double particleY = Math.max(highest.getY() + 1.1, loc.getY() + rng.nextDouble(-4.0, 8.0));
				player.spawnParticle(Particle.CLOUD, block.getX() + 0.5 + upwindX + rng.nextFloat(-4f, 4f), particleY, block.getZ() + 0.5 + upwindZ + rng.nextFloat(-4f, 4f), 0, wind.getX(), .001, wind.getZ(), particleSpeed);
				// Hard rain splashing on the ground surface right where the player can see it.
				if (highest.getY() - loc.getBlockY() <= 6 && rng.nextFloat() < splashRate * playerIntensity * currentStrength)
					player.spawnParticle(VersionUtils.getWaterSplash(), highest.getX() + 0.5 + rng.nextFloat(-.5f, .5f), highest.getY() + 1.0, highest.getZ() + 0.5 + rng.nextFloat(-.5f, .5f), 3, .3, .05, .3, 0.02);
				final double bx = highest.getX() - loc.getX();
				final double bz = highest.getZ() - loc.getZ();
				final double distance = (bx * bx) + (bz * bz);
				if (closest == null || distance < closestDistance) {
					closest = highest;
					closestDistance = distance;
				}
			}
			if (inside) {
				final int burst = (int) (particleRate * 110 * playerIntensity * currentStrength);
				for (int i = 0; i < burst; i++)
					player.spawnParticle(Particle.CLOUD, loc.getX() + rng.nextFloat(-10f, 10f), loc.getY() + rng.nextFloat(-4f, 7f), loc.getZ() + rng.nextFloat(-10f, 10f), 0, wind.getX(), .001, wind.getZ(), particleSpeed);
			}
			if (closest != null) {
				// Bright lightning-flash flickers out over the terrain near the player (seen from inside through windows
				// too), and dense little clouds of condensed rain bursting down in random spots — both ramp with the storm.
				if (rng.nextFloat() < 0.05f * currentStrength) {
					final Block spot = randomNearbyOutsideSurface(loc, rng);
					if (spot != null)
						VersionUtils.spawnFlashParticle(player, new Location(world, spot.getX() + 0.5, spot.getY() + 3 + rng.nextFloat(0f, 7f), spot.getZ() + 0.5), 0, 0, 0, 0, 0, Color.WHITE);
				}
				if (rng.nextFloat() < 0.1f * currentStrength) {
					final Block spot = randomNearbyOutsideSurface(loc, rng);
					if (spot != null) {
						player.spawnParticle(VersionUtils.getWaterSplash(), spot.getX() + 0.5, spot.getY() + 1.0, spot.getZ() + 0.5, 25, 1.3, 0.3, 1.3, 0.07);
						player.spawnParticle(Particle.FALLING_WATER, spot.getX() + 0.5, spot.getY() + 4.0, spot.getZ() + 0.5, 18, 1.3, 1.0, 1.3, 0.1);
					}
				}
				// Wind howl and rain ambience that build with the gusts/strength, with thunder rumbling overhead now and then.
				if (soundTick == 0) {
					final float ramp = (float) (soundLevel * currentStrength);
					playSound(player, loc.clone().add(0, 5, 0), inside ? Sound.WEATHER_RAIN : Sound.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, soundVolume * ramp * (inside ? 1.0f : 0.25f), .5f);
					playSound(player, loc.clone().add(0, 6, 0), Sound.ITEM_ELYTRA_FLYING, SoundCategory.WEATHER, soundVolume * ramp * 1.5f * (inside ? 1.0f : 0.4f), 0.55f);
					if (rng.nextFloat() < 0.15f * currentStrength)
						playSound(player, loc.clone().add(rng.nextFloat(-6f, 6f), 18, rng.nextFloat(-6f, 6f)), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, (float) (0.6 * currentStrength), 0.8f + rng.nextFloat(-.15f, .15f));
				}
			}
		}, pair -> {
			final ThreadLocalRandom rng = ThreadLocalRandom.current();
			final Player player = pair.getFirst();
			final Location loc = player.getLocation();
			Vector wind = getWindVectorAt(loc, windSnapshot, currentForce);
			wind = wind.lengthSquared() < 1.0E-4 ? windSnapshot.clone() : wind.normalize();
			final double force = Math.abs(currentForce);
			final double soundLevel = Utils.clamp(soundIncrement * force, 0.0, 1.0);
			final double edgeScale = particleIntensity(loc);
			if (edgeScale <= 0)
				return;
			final double edgeSpeed = ((force * 15.0) + 0.5) * (0.35 + (edgeScale * 0.65));
			final double upwindX = wind.getX() * -0.5;
			final double upwindZ = wind.getZ() * -0.5;
			boolean aboveFlag = false;
			boolean soundFlag = false;
			for (Block block : BlockUtils.getBlocksInCircleRadius(loc, particleRenderDistance)) {
				if (rng.nextFloat() >= particleRate * edgeScale * currentStrength)
					continue;
				final Block highest = world.getHighestBlockAt(block.getX(), block.getZ());
				if (highest == null || highest.getY() - loc.getBlockY() > 10)
					continue;
				aboveFlag = true;
				final double particleY = Math.max(highest.getY() + 1.1, loc.getY() + rng.nextDouble(-4.0, 9.0));
				player.spawnParticle(Particle.CLOUD, block.getX() + 0.5 + upwindX + rng.nextFloat(-4f, 4f), particleY, block.getZ() + 0.5 + upwindZ + rng.nextFloat(-4f, 4f), 0, wind.getX(), .001, wind.getZ(), edgeSpeed);
				if (!soundFlag && loc.distanceSquared(BlockUtils.getCenterOfBlock(highest)) <= 25)
					soundFlag = true;
			}
			if (aboveFlag) {
				final Location fixed = new Location(world, loc.getX(), location.getY(), loc.getZ());
				if (fixed.distanceSquared(location) > trueSmoothingRange)
					playSound(player, loc.clone().add(Utils.getVectorTowards(loc, location).multiply(8.0).setY(7)), Sound.WEATHER_RAIN, SoundCategory.WEATHER, (float) (((soundVolume / smoothingRangeExcess) * (smoothingRangeExcess - (fixed.distance(location) - disasterRange - smoothingRange))) * soundLevel * smoothingIntensity * currentStrength), .5f);
				else
					playSound(player, loc.clone().add(0, 7, 0), soundFlag ? Sound.WEATHER_RAIN : Sound.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, (float) (soundVolume * soundLevel * currentStrength), .5f);
			}
		});

		plugin.getServer().getPluginManager().registerEvents(this, plugin);

		// Seed puddles around players inside the live, moving storm radius. Keeping this bounded and on the main thread makes
		// placement reliable without scanning or force-loading the full hurricane area.
		final BlockData puddleData = Material.WATER.createBlockData(d -> ((Levelled) d).setLevel(7));
		scheduleTask(new BukkitRunnable() {
			private double growthBudget;
			private int growthPoolIndex;

			@Override
			public void run() {
				if (time <= 0 || currentStrength < 0.3)
					return;
				final ThreadLocalRandom rng = ThreadLocalRandom.current();
				growthBudget = Math.min(12.0, growthBudget + puddleGrowthRate);
				final Set<Long> claimedChunks = new HashSet<>();
				for (Player player : world.getPlayers()) {
					if (!player.isOnline() || !isWithinStorm(player.getLocation()))
						continue;
					final Location playerLoc = player.getLocation();
					if (claimedChunks.contains(getChunkKey(playerLoc.getBlockX() >> 4, playerLoc.getBlockZ() >> 4)))
						continue;
					for (int sample = 0; sample < 18; sample++) {
						final Block surface = randomPuddleSeedSurface(playerLoc, rng);
						if (claimedChunks.contains(getChunkKey(surface.getX() >> 4, surface.getZ() >> 4)))
							continue;
						if (!isWithinStorm(surface))
							continue;
						final Block above = surface.getRelative(BlockFace.UP);
						if (Tag.FIRE.isTagged(surface.getType()) && !isBlockProtected(surface)) {
							surface.setType(Material.AIR);
							world.playSound(BlockUtils.getCenterOfBlock(surface), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1f, 1f);
						} else if (Tag.FIRE.isTagged(above.getType()) && !isBlockProtected(above)) {
							above.setType(Material.AIR);
							world.playSound(BlockUtils.getCenterOfBlock(above), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1f, 1f);
						} else if (puddlePools.size() < puddleSeedLimit && puddles.size() < puddleCap
								&& rng.nextFloat() < puddleSeedRate
								&& isValidPuddleSpot(above, puddleData)) {
							above.setBlockData(puddleData, false);
							puddles.add(above);
							final List<Block> pool = new ArrayList<>();
							pool.add(above);
							puddlePools.add(pool);
						}
					}
					claimPuddleChunks(claimedChunks, playerLoc);
				}
				// Most puddle activity is contiguous growth from the rare seeds, not additional random placement.
				int growthSteps = Math.min(12, (int) growthBudget);
				growthBudget -= growthSteps;
				for (int i = 0; i < growthSteps && puddles.size() < puddleCap; i++) {
					growthPoolIndex = growNextPuddle(growthPoolIndex, rng, puddleData);
				}
			}
		}.runTaskTimer(plugin, 0, 20));

		// Main thread: when the storm is intense, mini water waves sweep across the ground toward exposed players/mobs and
		// drag them along (block edits + velocity must run on the main thread). Animated every tick; spawned every 5.
		scheduleTask(new BukkitRunnable() {
			private int spawnTick;

			@Override
			public void run() {
				for (Iterator<Wave> it = waves.iterator(); it.hasNext();)
					if (!tickWave(it.next()))
						it.remove();
				if (++spawnTick < 5)
					return;
				spawnTick = 0;
				if (currentStrength < WAVE_INTENSITY_THRESHOLD || waves.size() >= WAVE_CAP)
					return;
				for (Player player : playersInMonitorArea) {
					if (waves.size() >= WAVE_CAP)
						break;
					if (!player.isValid() || player.isFlying() || player.isGliding() || !isPlayerExposed(player))
						continue;
					final Location ploc = player.getLocation();
					final double dx = ploc.getX() - location.getX();
					final double dz = ploc.getZ() - location.getZ();
					if (zoneIntensity((dx * dx) + (dz * dz)) < 0.5)
						continue;
					if (random.nextFloat() < waveSpawnRate)
						spawnWave(ploc);
				}
			}
		}.runTaskTimer(plugin, 0, 1));

		// Off-main, particle-only rain-ripple gusts: bands of water spray that roll across the ground surface toward every
		// in-storm player (streamed directly to them, so all the heavy surface sampling stays off the main thread).
		scheduleTask(new BukkitRunnable() {
			private double phase;

			@Override
			public void run() {
				if (currentStrength <= 0)
					return;
				// Roll the bands downwind, faster in stronger gusts but gentle overall.
				phase += WAVE_RIPPLE_SPEED * (0.5 + Math.abs(currentForce));
				final ThreadLocalRandom rng = ThreadLocalRandom.current();
				for (Player player : world.getPlayers()) {
					if (!player.isOnline() || !player.getWorld().equals(world))
						continue;
					final Location loc = player.getLocation();
					final double ddx = loc.getX() - location.getX();
					final double ddz = loc.getZ() - location.getZ();
					final double intensity = zoneIntensity((ddx * ddx) + (ddz * ddz));
					if (intensity <= 0)
						continue;
					Vector wind = getWindVectorAt(loc, windSnapshot, currentForce);
					if (wind.lengthSquared() < 1.0E-4)
						continue;
					wind.normalize();
					final double dirX = wind.getX();
					final double dirZ = wind.getZ();
					final double perpX = -dirZ;
					final double perpZ = dirX;
					renderWaveRipples(player, loc, dirX, dirZ, perpX, perpZ, phase, intensity * currentStrength, rng);
				}
			}
		}.runTaskTimerAsynchronously(plugin, 0, 2));
	}
	/**
	 * Renders one player's view of the rolling rain-ripple gusts. Crest bands (a moving {@code sin} field projected along the
	 * wind, with a perpendicular wobble so they curve organically) drop visible particle spray on the ground surface near the
	 * player. Each band only renders on columns whose surface sits within {@link #WAVE_RIPPLE_HEIGHT_BAND} of the player, so
	 * ripples stay on nearby open ground and never appear on rooftops or inside buildings. Where a band rolls into a wall it
	 * cannot cross, the spray instead climbs the wall face. Particles are sent only to {@code player}, so this is async-safe.
	 */
	private void renderWaveRipples(Player player, Location loc, double dirX, double dirZ, double perpX, double perpZ, double phase, double strength, ThreadLocalRandom rng) {
		final Particle rippleParticle = Particle.DOLPHIN;
		final int aheadX = (int) Math.round(dirX);
		final int aheadZ = (int) Math.round(dirZ);
		final float radius = Math.min(particleRenderDistance, 16f);
		for (Block block : BlockUtils.getBlocksInCircleRadius(loc, radius)) {
			final int bx = block.getX();
			final int bz = block.getZ();
			if ((bx & 1) != 0 || (bz & 1) != 0)
				continue;
			final double s = (bx * dirX) + (bz * dirZ);
			final double p = (bx * perpX) + (bz * perpZ);
			final double band = Math.sin((s * WAVE_RIPPLE_FREQ) - phase + (0.6 * Math.sin(p * 0.18)));
			final double renderChance = Utils.clamp(0.35 + (strength * 0.65), 0.0, 1.0);
			if (band < WAVE_RIPPLE_THRESHOLD || rng.nextFloat() >= renderChance || !isWithinStorm(block))
				continue;
			final Block surface = world.getHighestBlockAt(bx, bz);
			final int dh = surface.getY() - loc.getBlockY();
			if (dh > WAVE_RIPPLE_HEIGHT_BAND || dh < -WAVE_RIPPLE_HEIGHT_BAND)
				continue;
			final double sy = surface.getY() + 1.0;
			final int splashCount = 4 + (int) Math.round(strength * 3.0);
			player.spawnParticle(rippleParticle, bx + 0.5 + rng.nextDouble(-0.35, 0.35), sy, bz + 0.5 + rng.nextDouble(-0.35, 0.35), splashCount, 0.35, 0.03, 0.35, 0.07);
			// If the band is rolling into a wall too tall to wash over, let the spray run up the wall face instead of
			// punching through it or popping up on top of whatever is behind it.
			final Block ahead = world.getHighestBlockAt(bx + aheadX, bz + aheadZ);
			final int wall = ahead.getY() - surface.getY();
			if (wall >= 2) {
				final int climb = Math.min(wall, WAVE_RIPPLE_CLIMB);
				final double fx = ahead.getX() + 0.5 - (dirX * 0.5);
				final double fz = ahead.getZ() + 0.5 - (dirZ * 0.5);
				for (int h = 1; h <= climb; h++)
					player.spawnParticle(rippleParticle, fx, surface.getY() + h + 0.4, fz, 2, 0.12, 0.12, 0.12, 0.06);
			}
		}
	}
	public void clean() {
		super.clean();
		HandlerList.unregisterAll(this);
		exposure.clear();
		debrisWindDelay.clear();
		ripQueue.clear();
		for (UUID uuid : aggressiveDrowned) {
			final Entity entity = Bukkit.getEntity(uuid);
			if (entity instanceof Drowned drowned)
				DependencyUtils.manipulateDrownedGoals(drowned, false);
		}
		aggressiveDrowned.clear();
		drownedInRainArea = Set.of();
		// Tear down any in-flight waves immediately.
		waves.clear();
		waveBlocks.forEach(block -> {
			if (block.getType() == Material.WATER)
				block.setType(Material.AIR, false);
		});
		waveBlocks.clear();
		// Dry the puddles up after the storm — or wipe them at once if the plugin is shutting down.
		if (DeadlyDisasters.isDisablingPlugin) {
			puddles.forEach(this::dryPuddle);
			puddles.clear();
			puddlePools.clear();
			return;
		}
		final List<List<Block>> dryingPools = new ArrayList<>(puddlePools.size());
		for (List<Block> pool : puddlePools)
			dryingPools.add(new ArrayList<>(pool));
		puddles.clear();
		puddlePools.clear();
		final int dryingDurationTicks = Math.max(1, (int) Math.round(3600.0 / puddleDryMultiplier));
		new BukkitRunnable() {
			private final int[] indices = new int[dryingPools.size()];
			private final double[] budgets = new double[dryingPools.size()];
			private boolean firstRun = true;

			{
				for (int i = 0; i < dryingPools.size(); i++)
					indices[i] = dryingPools.get(i).size() - 1;
			}

			@Override
			public void run() {
				boolean active = false;
				for (int i = 0; i < dryingPools.size(); i++) {
					if (indices[i] < 0)
						continue;
					if (firstRun)
						budgets[i] = 1.0;
					budgets[i] += dryingPools.get(i).size() / (double) dryingDurationTicks;
					while (budgets[i] >= 1.0 && indices[i] >= 0) {
						budgets[i] -= 1.0;
						dryPuddle(dryingPools.get(i).get(indices[i]--));
					}
					if (indices[i] >= 0)
						active = true;
				}
				firstRun = false;
				if (!active)
					cancel();
			}
		}.runTaskTimer(plugin, 200, 1);
	}
	private void dryPuddle(Block block) {
		if (block.getType() == Material.WATER && block.getBlockData() instanceof Levelled data && data.getLevel() == 7)
			block.setType(Material.AIR, false);
	}
	/** Picks a surface in a wide annulus around the player so rare seed pools do not cluster at their feet. */
	private Block randomPuddleSeedSurface(Location playerLoc, ThreadLocalRandom rng) {
		final double angle = rng.nextDouble(0, Math.PI * 2.0);
		final double minSq = PUDDLE_SEED_MIN_DISTANCE * PUDDLE_SEED_MIN_DISTANCE;
		final int maxDistance = rng.nextFloat() < 0.7f ? PUDDLE_SEED_NEAR_MAX_DISTANCE : PUDDLE_SEED_MAX_DISTANCE;
		final double maxSq = maxDistance * maxDistance;
		final double distance = Math.sqrt(rng.nextDouble(minSq, maxSq));
		final int x = playerLoc.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
		final int z = playerLoc.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
		return world.getHighestBlockAt(x, z);
	}
	private static long getChunkKey(int chunkX, int chunkZ) {
		return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
	}
	/** Reserves the chunks covered by one player's puddle radius so nearby players do not multiply seed attempts. */
	private void claimPuddleChunks(Set<Long> claimedChunks, Location center) {
		final int minChunkX = (center.getBlockX() - PUDDLE_SEED_MAX_DISTANCE) >> 4;
		final int maxChunkX = (center.getBlockX() + PUDDLE_SEED_MAX_DISTANCE) >> 4;
		final int minChunkZ = (center.getBlockZ() - PUDDLE_SEED_MAX_DISTANCE) >> 4;
		final int maxChunkZ = (center.getBlockZ() + PUDDLE_SEED_MAX_DISTANCE) >> 4;
		final double radiusSq = PUDDLE_SEED_MAX_DISTANCE * PUDDLE_SEED_MAX_DISTANCE;
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++)
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				final double nearestX = Utils.clamp(center.getX(), chunkX << 4, (chunkX << 4) + 15);
				final double nearestZ = Utils.clamp(center.getZ(), chunkZ << 4, (chunkZ << 4) + 15);
				final double dx = nearestX - center.getX();
				final double dz = nearestZ - center.getZ();
				if ((dx * dx) + (dz * dz) <= radiusSq)
					claimedChunks.add(getChunkKey(chunkX, chunkZ));
			}
	}
	/** Grows pools in round-robin order and records each block so cleanup can reverse the spread. */
	private int growNextPuddle(int startIndex, ThreadLocalRandom rng, BlockData puddleData) {
		if (puddlePools.isEmpty())
			return 0;
		for (int poolOffset = 0; poolOffset < puddlePools.size(); poolOffset++) {
			final int poolIndex = Math.floorMod(startIndex + poolOffset, puddlePools.size());
			final List<Block> pool = puddlePools.get(poolIndex);
			final int sourceFloor = Math.max(0, pool.size() - 32);
			final int attempts = Math.min(24, Math.max(8, pool.size() * 2));
			for (int i = 0; i < attempts; i++) {
				final Block source = pool.get(rng.nextInt(sourceFloor, pool.size()));
				final Block spot = neighbourPuddleSpot(source, rng, puddleData);
				if (spot == null || puddles.contains(spot))
					continue;
				spot.setBlockData(puddleData, false);
				puddles.add(spot);
				pool.add(spot);
				return (poolIndex + 1) % puddlePools.size();
			}
		}
		return (startIndex + 1) % puddlePools.size();
	}
	/** A puddle spot must be an open-air cell out under the sky, sitting on solid ground, inside the storm and unprotected. */
	private boolean isValidPuddleSpot(Block above, BlockData puddleData) {
		if (above.getType() != Material.AIR || !isWithinStorm(above))
			return false;
		if (above.getY() < world.getHighestBlockYAt(above.getX(), above.getZ()))
			return false;
		if (above.getRelative(BlockFace.DOWN).isPassable())
			return false;
		return above.canPlace(puddleData) && !isBlockProtected(above);
	}
	/** Finds a valid puddle spot on the ground next to an existing puddle (roughly the same height) for it to spread into. */
	private Block neighbourPuddleSpot(Block puddle, ThreadLocalRandom rng, BlockData puddleData) {
		final BlockFace face = PUDDLE_SPREAD_FACES[rng.nextInt(PUDDLE_SPREAD_FACES.length)];
		final Block surface = world.getHighestBlockAt(puddle.getX() + face.getModX(), puddle.getZ() + face.getModZ());
		final Block above = surface.getRelative(BlockFace.UP);
		if (Math.abs(above.getY() - puddle.getY()) > 1)
			return null;
		return isValidPuddleSpot(above, puddleData) ? above : null;
	}
	/** A random outdoor surface within a dozen blocks of (and near the height of) the player, or null if none/sheltered. */
	private Block randomNearbyOutsideSurface(Location loc, ThreadLocalRandom rng) {
		final Block surface = world.getHighestBlockAt(loc.getBlockX() + rng.nextInt(-12, 13), loc.getBlockZ() + rng.nextInt(-12, 13));
		if (!isWithinStorm(surface) || Math.abs(surface.getY() - loc.getBlockY()) > 8)
			return null;
		return surface;
	}
	/** Launches a mini wave that starts a short distance from {@code target} and sweeps toward (and past) it. */
	private void spawnWave(Location target) {
		final ThreadLocalRandom rng = ThreadLocalRandom.current();
		final double angle = rng.nextDouble(0, Math.PI * 2);
		final double awayX = Math.cos(angle);
		final double awayZ = Math.sin(angle);
		final Wave wave = new Wave();
		wave.dirX = -awayX;
		wave.dirZ = -awayZ;
		wave.sideX = -wave.dirZ;
		wave.sideZ = wave.dirX;
		wave.length = rng.nextDouble(10, 15);
		final double startDist = wave.length * rng.nextDouble(0.5, 0.7);
		wave.cx = target.getX() + (awayX * startDist);
		wave.cz = target.getZ() + (awayZ * startDist);
		wave.width = rng.nextInt(4, 7);
		wave.crestMax = rng.nextInt(1, 3);
		wave.speed = 0.5;
		wave.groundY = world.getHighestBlockAt((int) Math.floor(wave.cx), (int) Math.floor(wave.cz)).getY();
		waves.add(wave);
	}
	/**
	 * Advances one mini wave: restores last frame's water, lays the next single-block-thick crest line (arc-shaped, 1-2
	 * high, following the ground), splashes spray and drags caught entities forward. Returns false once it has run its
	 * course. Runs on the main thread.
	 */
	private boolean tickWave(Wave wave) {
		for (Block block : wave.placed)
			if (block.getType() == Material.WATER)
				block.setType(Material.AIR, false);
		waveBlocks.removeAll(wave.placed);
		wave.placed.clear();
		if (wave.traveled >= wave.length)
			return false;
		// Look at the ground directly ahead: if it rises more than the wave can climb it has hit a wall, so the wave stops
		// here rather than washing up and over into a base. Gentle slopes and descents it simply follows.
		final int nextX = (int) Math.floor(wave.cx + (wave.dirX * wave.speed));
		final int nextZ = (int) Math.floor(wave.cz + (wave.dirZ * wave.speed));
		final int nextGroundY = world.getHighestBlockAt(nextX, nextZ).getY();
		if (nextGroundY - wave.groundY > WAVE_MAX_CLIMB)
			return false;
		wave.cx += wave.dirX * wave.speed;
		wave.cz += wave.dirZ * wave.speed;
		wave.traveled += wave.speed;
		wave.groundY = nextGroundY;
		for (int i = 0; i < wave.width; i++) {
			final double off = i - ((wave.width - 1) / 2.0);
			final int bx = (int) Math.floor(wave.cx + (wave.sideX * off));
			final int bz = (int) Math.floor(wave.cz + (wave.sideZ * off));
			final Block surface = world.getHighestBlockAt(bx, bz);
			// Skip columns walled off (too tall to climb) or dropping into a pit/shaft (too deep) so the wave only ever
			// rolls across the open ground at its own level and never over walls or down into bases.
			if (surface.getY() - wave.groundY > WAVE_MAX_CLIMB || wave.groundY - surface.getY() > WAVE_MAX_DROP)
				continue;
			final double t = wave.width <= 1 ? 1.0 : (double) i / (wave.width - 1);
			final int crest = 1 + (int) Math.round((wave.crestMax - 1) * Math.sin(t * Math.PI));
			for (int h = 0; h < crest; h++) {
				final Block cell = surface.getRelative(0, h + 1, 0);
				if (cell.getType() != Material.AIR)
					break;
				cell.setBlockData(WAVE_WATER, false);
				wave.placed.add(cell);
				waveBlocks.add(cell);
			}
			world.spawnParticle(VersionUtils.getWaterSplash(), bx + 0.5, surface.getY() + 1.0 + crest, bz + 0.5, 5, .3, .1, .3, 0.08);
		}
		final double dragRadius = (wave.width / 2.0) + 1.5;
		final double dragRadiusSq = dragRadius * dragRadius;
		for (Entity entity : entitiesInMonitorArea) {
			if (!entity.isValid())
				continue;
			if (entity instanceof Player p && (p.isFlying() || p.isGliding()))
				continue;
			final Location eloc = entity.getLocation();
			if (Math.abs(eloc.getY() - wave.groundY) > 4)
				continue;
			final double ex = eloc.getX() - wave.cx;
			final double ez = eloc.getZ() - wave.cz;
			if ((ex * ex) + (ez * ez) > dragRadiusSq)
				continue;
			entity.setVelocity(entity.getVelocity().add(new Vector(wave.dirX * 0.33, 0.21, wave.dirZ * 0.33)));
		}
		if (wave.soundTick++ % 4 == 0)
			world.playSound(new Location(world, wave.cx, wave.groundY + 1.0, wave.cz), Sound.ENTITY_GENERIC_SPLASH, SoundCategory.WEATHER, 0.65f, 0.65f + random.nextFloat() * 0.25f);
		return true;
	}
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onFluidLevelChange(FluidLevelChangeEvent event) {
		final Block block = event.getBlock();
		if (puddles.contains(block) || waveBlocks.contains(block))
			event.setCancelled(true);
	}
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onFluidFlow(BlockFromToEvent event) {
		if (puddles.contains(event.getBlock()) || waveBlocks.contains(event.getBlock()))
			event.setCancelled(true);
	}
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onEntityCombust(EntityCombustEvent event) {
		final Location loc = event.getEntity() instanceof LivingEntity living ? living.getEyeLocation() : event.getEntity().getLocation();
		if (loc.getWorld().equals(world) && loc.getBlock().getLightFromSky() == 15 && isWithinStorm(loc))
			event.setCancelled(true);
	}
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onFireSpread(BlockSpreadEvent event) {
		if (!Tag.FIRE.isTagged(event.getNewState().getType()))
			return;
		final Block block = event.getBlock();
		if (!block.getWorld().equals(world) || block.getLightFromSky() != 15 || !isWithinStorm(block))
			return;
		event.setCancelled(true);
		final Block source = event.getSource();
		if (!Tag.FIRE.isTagged(source.getType()) || source.getLightFromSky() != 15 || isBlockProtected(source))
			return;
		source.setType(Material.AIR);
		world.playSound(BlockUtils.getCenterOfBlock(source), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1f, 1f);
	}
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		final Block fire = event.getIgnitingBlock();
		if (fire == null || !fire.getWorld().equals(world) || fire.getLightFromSky() != 15 || !isWithinStorm(fire))
			return;
		event.setCancelled(true);
		if (isBlockProtected(fire))
			return;
		fire.setType(Material.AIR);
		world.playSound(BlockUtils.getCenterOfBlock(fire), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1f, 1f);
	}
	private static final class Wave {
		private double cx;
		private double cz;
		private double dirX;
		private double dirZ;
		private double sideX;
		private double sideZ;
		private int width;
		private int crestMax;
		private double speed;
		private double length;
		private double traveled;
		private int groundY;
		private int soundTick;
		private final List<Block> placed = new ArrayList<>();
	}
	private record ShelterStrike(int x, int y, int z, double sourceX, double sourceY, double sourceZ) {}
	/**
	 * Walks a ray outward from {@code origin} and returns the OUTERMOST solid block that still opens onto the outside. It
	 * steps through any inner walls and hollow cavities of a multi-layered shelter and only commits to a wall once the cell
	 * immediately beyond it is exposed to the open sky, so the storm tears the externally-exposed layer first instead of
	 * pointlessly chipping inner walls. Returns {@code null} if the ray never breaks out to the outside (e.g. a fully buried
	 * direction). Block reads run off the main thread (same pattern as the exposure ray casts).
	 */
	private int[] findOutermostExposedBlock(Location origin, Vector direction, double maxDistance) {
		final double length = direction.length();
		if (length < 1.0E-4)
			return null;
		final double stepX = (direction.getX() / length) * 0.5;
		final double stepY = (direction.getY() / length) * 0.5;
		final double stepZ = (direction.getZ() / length) * 0.5;
		final int steps = (int) (maxDistance / 0.5);
		double x = origin.getX();
		double y = origin.getY();
		double z = origin.getZ();
		int lastX = Integer.MIN_VALUE;
		int lastY = Integer.MIN_VALUE;
		int lastZ = Integer.MIN_VALUE;
		Block lastSolid = null;
		for (int i = 0; i < steps; i++) {
			x += stepX;
			y += stepY;
			z += stepZ;
			final int bx = (int) Math.floor(x);
			final int by = (int) Math.floor(y);
			final int bz = (int) Math.floor(z);
			if (bx == lastX && by == lastY && bz == lastZ)
				continue;
			lastX = bx;
			lastY = by;
			lastZ = bz;
			final Block block = world.getBlockAt(bx, by, bz);
			if (!block.isPassable()) {
				lastSolid = block;
				continue;
			}
			// A passable cell beyond a wall: if it is open to the sky, the last wall we crossed is the outermost exposed
			// layer. If it is still enclosed (a hollow gap between walls), keep walking to find the next, more outer wall.
			if (lastSolid != null && by >= world.getHighestBlockYAt(bx, bz))
				return new int[] { lastSolid.getX(), lastSolid.getY(), lastSolid.getZ() };
		}
		return null;
	}
	/** Main-thread: shatter a brittle block (glass etc.) into particles, or tear an ordinary block into a hurtful debris. */
	private void applyShelterStrike(ShelterStrike strike) {
		final Block block = world.getBlockAt(strike.x(), strike.y(), strike.z());
		if (block.isPassable())
			return;
		final Material type = block.getType();
		if (Tag.WOODEN_DOORS.isTagged(type) || Tag.WOODEN_TRAPDOORS.isTagged(type)) {
			final BlockData data = block.getBlockData();
			if (data instanceof Openable openable && openable.isOpen())
				return;
			if (random.nextFloat() < WOODEN_ENTRY_BLOW_OPEN_CHANCE) {
				tryBlowOpenWoodenEntry(block, strike);
				return;
			}
		}
		if (shatterBlocks.contains(type) && random.nextFloat() < shatterChance) {
			final BlockData data = block.getBlockData();
			if (removeBlock(block, false, false)) {
				final Location center = BlockUtils.getCenterOfBlock(block);
				world.playSound(center, Sound.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1f, random.nextFloat(0.8f, 1.2f));
				world.spawnParticle(VersionUtils.getBlockCrack(), center, 30, .25, .25, .25, 0.02, data);
				// A shattering pane tends to take the neighbouring window blocks with it.
				for (BlockFace face : SHATTER_FACES) {
					final Block neighbor = block.getRelative(face);
					if (!shatterBlocks.contains(neighbor.getType()))
						continue;
					final BlockData neighborData = neighbor.getBlockData();
					if (removeBlock(neighbor, false, false))
						world.spawnParticle(VersionUtils.getBlockCrack(), BlockUtils.getCenterOfBlock(neighbor), 15, .25, .25, .25, 0.02, neighborData);
				}
			}
			return;
		}
		final FallingBlock fb = convertBlockIntoFallingBlock(block);
		if (fb != null) {
			fb.setHurtEntities(true);
			fb.setDropItem(false);
			final Location source = new Location(world, strike.sourceX(), strike.sourceY(), strike.sourceZ());
			Vector localWind = getWindVectorAt(source, windDirection, currentForce);
			if (localWind.lengthSquared() < 1.0E-4)
				localWind = windDirection.clone();
			localWind.normalize();
			final Vector wallDirection = new Vector(block.getX() + 0.5 - strike.sourceX(), 0,
					block.getZ() + 0.5 - strike.sourceZ());
			final boolean upwindWall = wallDirection.lengthSquared() > 1.0E-4
					&& wallDirection.normalize().dot(localWind) < -0.2;
			if (upwindWall) {
				final Vector side = new Vector(-localWind.getZ(), 0, localWind.getX())
						.multiply(random.nextBoolean() ? 0.35 : -0.35);
				final double ejectSpeed = Math.max(0.45, Math.abs(currentForce) * 3.5);
				fb.setVelocity(localWind.multiply(-1).add(side).normalize().multiply(ejectSpeed).setY(0.3));
				debrisWindDelay.put(fb.getUniqueId(), System.currentTimeMillis() + 1000L);
			} else
				fb.setVelocity(localWind.multiply(Math.max(0.2, Math.abs(currentForce)) * 6.0).setY(0.25));
			entitiesInMonitorArea.add(fb);
		}
	}
	/**
	 * Opens an attached, closed wooden door or trapdoor only when the side away from the sheltered player is outdoors.
	 */
	private boolean tryBlowOpenWoodenEntry(Block block, ShelterStrike strike) {
		final BlockData rawData = block.getBlockData();
		if (!(rawData instanceof Openable openable) || openable.isOpen() || isBlockProtected(block))
			return false;
		final boolean doorEntry = rawData instanceof Door;
		final Block entryBase;
		if (rawData instanceof Door door) {
			entryBase = door.getHalf() == Half.TOP ? block.getRelative(BlockFace.DOWN) : block;
			if (!entryBase.getRelative(BlockFace.DOWN).getType().isSolid())
				return false;
		} else if (rawData instanceof TrapDoor trapDoor) {
			entryBase = block;
			if (!block.getRelative(trapDoor.getFacing().getOppositeFace()).getType().isSolid())
				return false;
		} else
			return false;
		final BlockFace exteriorFace = getEntryExteriorFace(entryBase, strike, doorEntry);
		final Block outside = entryBase.getRelative(exteriorFace);
		if (!isOutdoorEntrySide(outside)
				&& (!doorEntry || !isOutdoorEntrySide(entryBase.getRelative(BlockFace.UP).getRelative(exteriorFace))))
			return false;
		if (doorEntry) {
			final List<Block> halves = List.of(entryBase, entryBase.getRelative(BlockFace.UP));
			for (Block half : halves)
				if (!(half.getBlockData() instanceof Door) || !Tag.WOODEN_DOORS.isTagged(half.getType())
						|| isBlockProtected(half))
					return false;
			for (Block half : halves) {
				final Door door = (Door) half.getBlockData();
				door.setOpen(true);
				half.setBlockData(door, false);
			}
		} else {
			openable.setOpen(true);
			block.setBlockData(openable, false);
		}
		final Location center = BlockUtils.getCenterOfBlock(entryBase).add(0, doorEntry ? 0.5 : 0, 0);
		world.playSound(center, doorEntry ? Sound.BLOCK_WOODEN_DOOR_OPEN : Sound.BLOCK_WOODEN_TRAPDOOR_OPEN,
				SoundCategory.BLOCKS, 1.25f, random.nextFloat(0.75f, 0.95f));
		world.playSound(center, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.BLOCKS, 1.1f,
				random.nextFloat(0.75f, 0.95f));
		spawnEntryWindBurst(center, exteriorFace, strike);
		return true;
	}
	private BlockFace getEntryExteriorFace(Block entry, ShelterStrike strike, boolean doorEntry) {
		final double dx = entry.getX() + 0.5 - strike.sourceX();
		final double dy = entry.getY() + 0.5 - strike.sourceY();
		final double dz = entry.getZ() + 0.5 - strike.sourceZ();
		if (!doorEntry && Math.abs(dy) > Math.max(Math.abs(dx), Math.abs(dz)))
			return dy > 0 ? BlockFace.UP : BlockFace.DOWN;
		if (Math.abs(dx) > Math.abs(dz))
			return dx > 0 ? BlockFace.EAST : BlockFace.WEST;
		return dz > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
	}
	private boolean isOutdoorEntrySide(Block outside) {
		if (!outside.isPassable())
			return false;
		if (outside.getLightFromSky() >= 14)
			return true;
		return Utils.isLocationExposedToOutdoorsOptimized(BlockUtils.getCenterOfBlock(outside), 8f, 6);
	}
	private void spawnEntryWindBurst(Location center, BlockFace exteriorFace, ShelterStrike strike) {
		final Vector inward = new Vector(strike.sourceX() - center.getX(), strike.sourceY() - center.getY(),
				strike.sourceZ() - center.getZ());
		if (inward.lengthSquared() < 1.0E-4)
			inward.setX(-exteriorFace.getModX()).setY(-exteriorFace.getModY()).setZ(-exteriorFace.getModZ());
		inward.normalize();
		final Location burst = center.clone().add(exteriorFace.getModX() * 0.7, exteriorFace.getModY() * 0.7,
				exteriorFace.getModZ() * 0.7);
		for (int i = 0; i < 14; i++) {
			final Vector direction = inward.clone().add(new Vector(random.nextDouble(-0.12, 0.12),
					random.nextDouble(-0.08, 0.12), random.nextDouble(-0.12, 0.12))).normalize();
			world.spawnParticle(Particle.CLOUD, burst.clone().add(random.nextDouble(-0.3, 0.3),
					random.nextDouble(-0.5, 0.5), random.nextDouble(-0.3, 0.3)), 0,
					direction.getX(), direction.getY(), direction.getZ(), random.nextDouble(0.35, 0.65));
		}
		for (int i = 0; i < 18; i++) {
			final Vector direction = inward.clone().add(new Vector(random.nextDouble(-0.18, 0.18),
					random.nextDouble(-0.12, 0.18), random.nextDouble(-0.18, 0.18))).normalize();
			world.spawnParticle(VersionUtils.getWaterSplash(), burst.clone().add(random.nextDouble(-0.35, 0.35),
					random.nextDouble(-0.5, 0.5), random.nextDouble(-0.35, 0.35)), 0,
					direction.getX(), direction.getY(), direction.getZ(), random.nextDouble(0.45, 0.8));
		}
	}
	private boolean isPlayerExposed(Player player) {
		final Float stored = exposure.get(player.getUniqueId());
		return stored != null && stored >= shelterSafeThreshold;
	}
	public boolean forceDownfallWeather() {
		return true;
	}
	protected String getConfigPath() {
		return "disasters.weather.hurricane";
	}
	public Set<Environment> getBannedEnvironments() {
		return EnumSet.of(Environment.NETHER, Environment.THE_END);
	}
}
