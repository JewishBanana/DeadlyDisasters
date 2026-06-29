package com.github.jewishbanana.deadlydisasters.disasters.destructive;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.disasters.Disaster;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class LavaGeyser extends Disaster implements Listener {

	private int minHeight;
	private double size;
	private double speed;
	private double damageRadius;
	private int boilingDuration;

	private Material material;
	private Particle particles;

	// Scorched-cone palette (weighted by repetition) + basalt spikes. Volcanic themed.
	private static final Material[] RIM_PALETTE = {Material.BASALT, Material.BLACKSTONE, Material.MAGMA_BLOCK,
			Material.POLISHED_BLACKSTONE, Material.SMOOTH_BASALT, Material.BASALT, Material.BLACKSTONE, Material.MAGMA_BLOCK};
	private static final Material[] SPIKE_PALETTE = {Material.BASALT, Material.POLISHED_BLACKSTONE, Material.OBSIDIAN};
	// Scorched crack lining for the radial surface fractures (volcanic themed).
	private static final Material[] CRACK_FLOOR = {Material.BLACKSTONE, Material.BASALT, Material.MAGMA_BLOCK, Material.OBSIDIAN};
	private static final Set<String> FIRE_IMMUNE_TYPES = Set.of("BLAZE", "MAGMA_CUBE", "STRIDER", "WITHER_SKELETON",
			"ZOMBIFIED_PIGLIN", "ZOMBIE_PIGMAN", "GHAST", "WITHER");
	private static final double DECORATIVE_SPURT_HITBOX_EXPANSION = 2.75;
	private static final double AIMED_SPURT_SEARCH_RADIUS = 3.0;
	private static final double AIMED_SPURT_VERTICAL_SEARCH_RADIUS = 3.0;
	private static final double AIMED_SPURT_HITBOX_EXPANSION = 1.75;
	private static final int NATURAL_LAVA_LAKE_SCAN_HEIGHT = 40;
	private static final double NATURAL_LAVA_LAKE_MIN_LAVA_FRACTION = 0.28;
	private static final double NATURAL_LAVA_LAKE_MIN_SURFACE_FRACTION = 0.16;
	private static final double NATURAL_LAVA_LAKE_MIN_COLUMN_FRACTION = 0.30;
	private static final int MAIN_MIN_PEAK_Y = 85;
	private static final int MAIN_MAX_PEAK_Y = 98;
	private static final int SATELLITE_MIN_PEAK_Y = 74;
	private static final int SATELLITE_MAX_PEAK_Y = 82;
	private static final int DEATH_WATCHER_VERTICAL_PADDING = 4;

	private List<Geyser> geysers = new ArrayList<>();
	private int pendingGeysers; // geysers scheduled but not yet spawned (staggered timing) — keeps the disaster alive until they appear

	public LavaGeyser(Location location, Player player, int level) {
		super(location, player, level);

		this.minHeight = getConfigInt("minimum_height");
	}
	public void init() {
		super.init();
		this.size = getConfigDouble("size");
		this.speed = getConfigDouble("speed");
		this.damageRadius = getConfigDouble("damage_radius");
		int cfgBoil = getConfigInt("boiling_duration");
		this.boilingDuration = cfgBoil > 0 ? cfgBoil : 60; // seconds the leftover pool keeps boiling after the column drops
		switch (level) {
		default:
		case 1:
			disasterRange = 1.5;
			break;
		case 2:
			disasterRange = 2.5;
			break;
		case 3:
			disasterRange = 3.5;
			break;
		case 4:
			disasterRange = 4.5;
			break;
		case 5:
			disasterRange = 5.5;
			break;
		case 6:
			disasterRange = 10.5;
			break;
		}
		disasterRange *= size;

		this.material = Material.LAVA;
		this.particles = Particle.FALLING_LAVA;
	}
	public Location findPossiblePosition(Location initial) {
		if (initial == null)
			return null;
		// Lava geysers live near the Nether floor, with room below for the leftover boiling pool.
		int baseY = Math.max(initial.getWorld().getMinHeight() + 4, minHeight);
		Location temp = new Location(initial.getWorld(), initial.getX(), baseY, initial.getZ());
		int count = 0;
		for (Block b : BlockUtils.getBlocksInSphereRadius(temp, (float) disasterRange))
			if (!b.isPassable())
				count++;
		if (count >= Math.pow(disasterRange, 3.0) * 0.7)
			return temp;
		return null;
	}
	public boolean canStart() {
		if (getPlayer() != null && getPlayer().getLocation().getBlockY() < minHeight)
			return false;
		return super.canStart();
	}
	public void start() {
		super.start();
		addDeathWatcher("deaths.lava_geyser");
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		final int baseY = location.getBlockY();
		final World w = location.getWorld();
		final int mainMaxHeight = peakHeight(baseY, w, MAIN_MIN_PEAK_Y, MAIN_MAX_PEAK_Y);
		int satCount = rollSatelliteCount();
		pendingGeysers = 1 + satCount;
		// The main geyser: full size and height, with a short staggered delay so a satellite can occasionally beat it up.
		spawnGeyserDelayed(BlockUtils.getCenterOfBlock(location.getBlock()), disasterRange, mainMaxHeight, 3.0 * speed, 30 + random.nextInt(30));
		// Satellites: smaller sibling geysers scattered evenly (but non-overlapping) around the main, within (level*7+10)*size
		// blocks — scaled by the config size setting, with extra room at level 6 for its dense 7-10 satellite field.
		double areaRadius = (level * 7 + 10) * size;
		if (level >= 6)
			areaRadius *= 1.25;
		double mainClear = disasterRange + damageRadius + 2.5; // the main's footprint radius (column + damage shell + margin)
		List<double[]> placed = new ArrayList<>(); // {x, z, clearRadius} of everything already placed, to keep geysers apart
		placed.add(new double[] {location.getX(), location.getZ(), mainClear});
		for (int i = 0; i < satCount; i++) {
			// 1/2 .. 3/4 of the main geyser, but a touch smaller at level 6 (0.4 .. 0.6) since its satellite field is dense
			double sizeRatio = (level >= 6 ? 0.4 : 0.5) + random.nextDouble() * (level >= 6 ? 0.2 : 0.25);
			double satSize = disasterRange * sizeRatio;
			double satClear = satSize + damageRadius + 2.5;
			Location satLoc = null;
			for (int attempt = 0; attempt < 24 && satLoc == null; attempt++) {
				// First half of the tries stay in this satellite's even angular sector (keeps them spread out); if those are all
				// blocked, the rest roam the whole circle so it only gets dropped when there's genuinely no room left.
				double ang = attempt < 12 ? (i + random.nextDouble()) / satCount * Math.PI * 2.0 : random.nextDouble() * Math.PI * 2.0;
				double minR = mainClear + satClear + 1.0; // far enough out to clear the main column
				double maxR = Math.max(minR, areaRadius);
				double rr = minR + random.nextDouble() * (maxR - minR);
				double sx = location.getX() + Math.cos(ang) * rr;
				double sz = location.getZ() + Math.sin(ang) * rr;
				boolean clear = true;
				for (double[] p : placed) { // reject if it would overlap/connect with any already-placed geyser
					double dx = sx - p[0], dz = sz - p[1], min = satClear + p[2];
					if (dx * dx + dz * dz < min * min) {
						clear = false;
						break;
					}
				}
				if (clear) {
					satLoc = new Location(w, sx, baseY, sz);
					placed.add(new double[] {sx, sz, satClear});
				}
			}
			if (satLoc == null) { // couldn't find a non-overlapping spot in the area — skip this satellite
				pendingGeysers--;
				continue;
			}
			int satMaxHeight = peakHeight(baseY, w, SATELLITE_MIN_PEAK_Y, SATELLITE_MAX_PEAK_Y);
			// Staggered start so satellites erupt at varied times — some before, some after the main.
			spawnGeyserDelayed(BlockUtils.getCenterOfBlock(satLoc.getBlock()), satSize, satMaxHeight, 3.0 * speed, random.nextInt(0, 180));
		}
	}
	private int peakHeight(int baseY, World world, int minPeakY, int maxPeakY) {
		int peakY = random.nextInt(minPeakY, maxPeakY + 1);
		return Math.min(world.getMaxHeight() - 8, Math.max(baseY + 12, peakY));
	}
	// Satellite count by level: lvl1 main only; then growing bands (2:1-2, 3:2-4, 4:3-5, 5:4-6, 6:7-10).
	private int rollSatelliteCount() {
		switch (level) {
		case 2:
			return 1 + random.nextInt(2); // 1-2
		case 3:
			return 2 + random.nextInt(3); // 2-4
		case 4:
			return 3 + random.nextInt(3); // 3-5
		case 5:
			return 4 + random.nextInt(3); // 4-6
		case 6:
			return 7 + random.nextInt(4); // 7-10
		default:
			return 0; // level 1 (and any fallback): main geyser only
		}
	}
	private void spawnGeyserDelayed(Location loc, double size, int maxHeight, double riseRate, int delay) {
		scheduleTask(new BukkitRunnable() {
			@Override
			public void run() {
				pendingGeysers--;
				createGeyser(loc, size, maxHeight, riseRate);
			}
		}.runTaskLater(plugin, delay));
	}
	public void clean() {
		super.clean();
		HandlerList.unregisterAll(this); // tear down this geyser's spread guard
		removeDeathWatcher(200);
	}
	// The geyser owns its lava entirely: never let it flow/spread outside the tracked shaft and pool.
	@EventHandler(ignoreCancelled = true)
	public void onBlockFromTo(BlockFromToEvent event) {
		if (isWithinAnyGeyser(event.getBlock()) || isWithinAnyGeyser(event.getToBlock()))
			event.setCancelled(true);
	}
	private boolean isWithinAnyGeyser(Block b) {
		int bx = b.getX(), by = b.getY(), bz = b.getZ();
		for (Geyser g : geysers)
			if (g.contains(bx, by, bz))
				return true;
		return false;
	}
	private boolean isTouchingOwnedLava(Entity entity) {
		Location loc = entity.getLocation();
		double height = Math.max(1.0, entity.getHeight());
		for (double dy = -DEATH_WATCHER_VERTICAL_PADDING; dy <= height + DEATH_WATCHER_VERTICAL_PADDING; dy += 0.5) {
			Block block = loc.clone().add(0, dy, 0).getBlock();
			if (block.getType() == material && isWithinAnyGeyser(block))
				return true;
		}
		return false;
	}
	public Function<PlayerDeathEvent, Boolean> getDeathCheck() {
		return event -> {
			if (event.getEntity().getLastDamageCause() == null)
				return false;
			if (event.getEntity().getLastDamageCause().getCause() != DamageCause.LAVA)
				return false;
			return isTouchingOwnedLava(event.getEntity());
		};
	}
	private void createGeyser(Location location, double size, int maxHeight, double riseRate) {
		Geyser geyser = new Geyser(location, size, maxHeight, riseRate);
		geysers.add(geyser);
		geyser.start();
	}
	private void removeGeyser(Geyser geyser) {
		geysers.remove(geyser);
		if (geysers.isEmpty() && pendingGeysers <= 0) // don't end while satellites are still staggered to spawn
			stop();
	}
	protected String getConfigPath() {
		return "disasters.destructive.lava_geyser";
	}
	public Set<Environment> getBannedEnvironments() {
		return EnumSet.of(Environment.NORMAL, Environment.THE_END);
	}
	private enum Phase { BUILDUP, RISING, HOLD, RETREAT, POOL, DISSIPATE }
	// A nearby scaldable entity + an immutable, pre-expanded snapshot of its hitbox, taken on the main thread so the async
	// spurt task can safely test collisions against it.
	private static final class SpurtTarget {
		private final LivingEntity entity;
		private final BoundingBox box;
		private SpurtTarget(LivingEntity entity, BoundingBox box) {
			this.entity = entity;
			this.box = box;
		}
	}
	private class Geyser {

		private final World world;
		private final int cx;
		private final int cz;
		private final int baseY;
		private final double size;
		private final int maxHeight;
		private final double baseRiseRate;
		private final BlockData fluidData = material.createBlockData();

		private final double columnRadiusSq;
		private final double shellRadius;
		private final double shellRadiusSq;
		private final double clearRadius;
		private final List<int[]> disc = new ArrayList<>();
		private final List<int[]> crackVents = new ArrayList<>();
		private final Set<Block> ownedFluidBlocks = new HashSet<>();

		private volatile Phase phase = Phase.BUILDUP; // read by the async decorative-spurt task
		private int phaseTimer;
		private volatile int currentHeight; // read by the async decorative-spurt task
		private double riseAccum;
		private int ticks;
		private volatile boolean isFinished; // read by the async decorative-spurt task
		private boolean crackedSurface;
		private final List<double[]> crackArms = new ArrayList<>();
		private boolean cracking;
		private int crackStep;
		private int crackInner;
		private double crackAccum;
		private int surfaceY;
		private int retreatStartHeight;
		private int naturalLavaLakeSurfaceY = Integer.MIN_VALUE;
		private int poolFloorY;
		private int poolTicks;
		private int dissipateY;     // the water layer currently draining away (top-to-bottom) once the pool is dissipating
		private int dissipateTimer; // ticks until the next layer drains
		private int whirlpoolTimer;
		private int rainTimer;
		private boolean exitsIntoNaturalLavaLake;
		private final List<Block> poolWater = new ArrayList<>();
		private int spurtAimTimer;
		private final java.util.Map<Long, Integer> spikeCells = new java.util.HashMap<>(); // bottom soul-sand spikes: packed (dx,dz) -> top Y
		private final Set<UUID> surged = new HashSet<>(); // entities already blasted by the rising column (so each is hit once)
		private final List<double[]> decoSpurts = new ArrayList<>(); // pooled decorative water spurts {x,y,z,vx,vy,vz,life} — async-thread only
		private volatile List<Player> spurtPlayers = new ArrayList<>(); // snapshot of nearby players (main-thread write, async read) for spurt rendering
		private volatile List<SpurtTarget> spurtTargets = new ArrayList<>(); // snapshot of nearby entities + boxes (main-thread write, async read) for spurt collision

		public Geyser(Location location, double size, int maxHeight, double riseRate) {
			this.world = location.getWorld();
			this.cx = location.getBlockX();
			this.cz = location.getBlockZ();
			this.baseY = location.getBlockY();
			this.size = size;
			this.maxHeight = maxHeight;
			this.baseRiseRate = riseRate;
			this.currentHeight = baseY;

			this.columnRadiusSq = size * size;
			this.shellRadius = size + damageRadius;
			this.shellRadiusSq = shellRadius * shellRadius;
			this.clearRadius = shellRadius + 2.5;
			double clearRadiusSq = clearRadius * clearRadius;

			int r = (int) Math.ceil(clearRadius);
			for (int dx = -r; dx <= r; dx++)
				for (int dz = -r; dz <= r; dz++) {
					int d2 = dx * dx + dz * dz;
					if (d2 <= clearRadiusSq)
						disc.add(new int[] {dx, dz, d2});
				}
		}
		public void start() {
			// The Nether roof makes getHighestBlockYAt unusable here: the geyser needs the local cave layer it actually
			// breaks into, otherwise the shortened Nether column never triggers its surface blast or fractures.
			this.surfaceY = findNetherBreakSurfaceY();
			this.naturalLavaLakeSurfaceY = findNaturalLavaLakeSurfaceY();
			this.exitsIntoNaturalLavaLake = naturalLavaLakeSurfaceY != Integer.MIN_VALUE;
			this.whirlpoolTimer = 20 + random.nextInt(41);
			this.rainTimer = 40 + random.nextInt(40);
			layBoilingFloor();
			formBottomSpikes();
			sculptVent();
			this.phaseTimer = 30 + random.nextInt(20) + level * 3;
			final Geyser reference = this;
			scheduleTask(new BukkitRunnable() {
				@Override
				public void run() {
					try {
						tick(reference);
					} catch (Exception e) {
						Utils.sendExceptionLog(e);
					}
				}
			}.runTaskTimer(plugin, 0, 1));
			scheduleTask(new BukkitRunnable() {
				@Override
				public void run() {
					if (isFinished || phase == Phase.POOL || phase == Phase.DISSIPATE) {
						this.cancel();
						return;
					}
					damageEntities();
				}
			}.runTaskTimer(plugin, 0, 1));
			// Decorative spurts run fully off the main thread (loaded-chunk block reads + particle packets only) so the
			// constant all-around spray is essentially free no matter how dense it gets.
			scheduleTask(new BukkitRunnable() {
				@Override
				public void run() {
					if (isFinished) {
						this.cancel();
						return;
					}
					try {
						runDecorativeSpurtsAsync();
					} catch (Exception e) {
						Utils.sendExceptionLog(e);
					}
				}
			}.runTaskTimerAsynchronously(plugin, 0, 1));
		}
		private int findNetherBreakSurfaceY() {
			int lower = Math.max(world.getMinHeight() + 1, baseY + 4);
			int upper = Math.min(world.getMaxHeight() - 2, maxHeight);
			if (upper <= lower)
				return Math.max(world.getMinHeight() + 1, Math.min(maxHeight, baseY));
			int targetY = maxHeight;
			Player player = getPlayer();
			if (player != null && player.getWorld() == world)
				targetY = Math.max(lower, Math.min(upper, player.getLocation().getBlockY()));
			Integer best = null;
			int bestDistance = Integer.MAX_VALUE;
			boolean seenSolid = false;
			for (int y = lower; y <= upper; y++) {
				boolean solid = isSurfaceSolid(world.getBlockAt(cx, y, cz));
				if (solid) {
					seenSolid = true;
					continue;
				}
				if (!seenSolid || !isOpenForEruption(cx, y, cz) || !isOpenForEruption(cx, y + 1, cz))
					continue;
				int candidate = y - 1;
				if (!isSurfaceSolid(world.getBlockAt(cx, candidate, cz)))
					continue;
				int distance = Math.abs(y - targetY);
				if (best == null || distance < bestDistance || (distance == bestDistance && candidate > best)) {
					best = candidate;
					bestDistance = distance;
				}
			}
			if (best != null)
				return best;
			return Math.max(lower, Math.min(upper, targetY - 1));
		}
		private int localSurfaceTopY(int bx, int bz, int refSurfaceY) {
			int lower = Math.max(world.getMinHeight() + 1, refSurfaceY - 4);
			int upper = Math.min(world.getMaxHeight() - 2, refSurfaceY + 6);
			Integer best = null;
			int bestDistance = Integer.MAX_VALUE;
			for (int y = lower; y <= upper; y++) {
				if (!isSurfaceSolid(world.getBlockAt(bx, y, bz)))
					continue;
				if (!isOpenForEruption(bx, y + 1, bz))
					continue;
				int distance = Math.abs(y - refSurfaceY);
				if (best == null || distance < bestDistance || (distance == bestDistance && y > best)) {
					best = y;
					bestDistance = distance;
				}
			}
			if (best != null)
				return best;
			for (int y = upper; y >= refSurfaceY; y--)
				if (isSurfaceSolid(world.getBlockAt(bx, y, bz)))
					return y;
			return refSurfaceY;
		}
		private boolean isSurfaceSolid(Block block) {
			return !block.isPassable() && !block.isLiquid();
		}
		private boolean isOpenForEruption(int bx, int y, int bz) {
			if (y < world.getMinHeight() || y >= world.getMaxHeight())
				return false;
			Block block = world.getBlockAt(bx, y, bz);
			return block.isPassable() && !block.isLiquid();
		}
		private int findNaturalLavaLakeSurfaceY() {
			int lower = Math.max(world.getMinHeight() + 1, baseY);
			int upper = Math.min(Math.min(maxHeight, baseY + NATURAL_LAVA_LAKE_SCAN_HEIGHT), world.getMaxHeight() - 2);
			if (upper < lower)
				return Integer.MIN_VALUE;
			double scanRadius = Math.max(clearRadius + 3.0, 8.0);
			double scanRadiusSq = scanRadius * scanRadius;
			int r = (int) Math.ceil(scanRadius);
			int bestY = Integer.MIN_VALUE;
			int bestSurfaceCells = 0;
			for (int y = lower; y <= upper; y++) {
				int sampleCells = 0, lavaCells = 0, surfaceCells = 0, columnCells = 0, columnLavaCells = 0;
				for (int dx = -r; dx <= r; dx++)
					for (int dz = -r; dz <= r; dz++) {
						int d2 = dx * dx + dz * dz;
						if (d2 > scanRadiusSq)
							continue;
						sampleCells++;
						Block block = world.getBlockAt(cx + dx, y, cz + dz);
						boolean lava = block.getType() == material;
						if (d2 <= columnRadiusSq) {
							columnCells++;
							if (lava)
								columnLavaCells++;
						}
						if (!lava)
							continue;
						lavaCells++;
						Block above = world.getBlockAt(cx + dx, y + 1, cz + dz);
						if (above.getType() != material && above.isPassable())
							surfaceCells++;
					}
				int minLavaCells = Math.max(24, (int) Math.ceil(sampleCells * NATURAL_LAVA_LAKE_MIN_LAVA_FRACTION));
				int minSurfaceCells = Math.max(12, (int) Math.ceil(sampleCells * NATURAL_LAVA_LAKE_MIN_SURFACE_FRACTION));
				int minColumnLavaCells = Math.max(2, (int) Math.ceil(columnCells * NATURAL_LAVA_LAKE_MIN_COLUMN_FRACTION));
				if (lavaCells < minLavaCells || surfaceCells < minSurfaceCells || columnLavaCells < minColumnLavaCells)
					continue;
				if (surfaceCells > bestSurfaceCells || (surfaceCells == bestSurfaceCells && y > bestY)) {
					bestY = y;
					bestSurfaceCells = surfaceCells;
				}
			}
			return bestY;
		}
		// Lays the disaster's floor one block above bedrock across the whole vent footprint: a churning bed of soul sand
		// (~2/3) and magma (~1/3). With water above it this forms natural upward/downward bubble columns for the whole
		// disaster (the floor is never cleared, so the columns persist through the eruption and the leftover pool alike).
		private void layBoilingFloor() {
			for (int[] cell : disc) {
				Block floor = world.getBlockAt(cx + cell[0], baseY - 1, cz + cell[1]);
				placeBlock(floor, random.nextInt(3) == 0 ? Material.MAGMA_BLOCK : Material.SOUL_SAND);
			}
		}
		// Raises the soul-sand spikes from the START (not just with the final pool) across ~1/3 of the column footprint, so
		// an entity vacuumed to the bottom during the retreat lands on a spiky floor rather than flat ground. buildLevel
		// leaves these cells alone (no water placed over the spike) and the leftover pool reuses them.
		private void formBottomSpikes() {
			for (int[] cell : disc) {
				if (cell[2] > columnRadiusSq || random.nextInt(3) != 0)
					continue;
				int top = baseY + random.nextInt(3); // pokes 0-2 above the floor surface (baseY)
				for (int y = baseY; y <= top; y++)
					placeBlock(world.getBlockAt(cx + cell[0], y, cz + cell[1]), Material.SOUL_SAND);
				spikeCells.put((((long) cell[0]) << 32) | (cell[1] & 0xFFFFFFFFL), top);
			}
		}
		private int spikeTop(int dx, int dz) {
			Integer top = spikeCells.get((((long) dx) << 32) | (dz & 0xFFFFFFFFL));
			return top == null ? Integer.MIN_VALUE : top;
		}
		private void sprinkleSides() {
			spurtPlayers = playersNearColumn(64.0); // refresh the render snapshot for the async decorative spray (cheap, main-thread)
			occasionalTargetedSpurt();              // every few seconds one aimed spurt that actually reaches and scalds a nearby entity
		}
		// The geyser's constant water spray: distinct little streams shooting out from random points ALL AROUND the column
		// circumference, at the surface eruption band and near each nearby player's level (so cave eruptions are visible too),
		// launching in varying directions. Decorative only — they don't damage, they just splash on whatever they hit.
		// This runs entirely on the ASYNC thread: it only does work Bukkit permits off-main — reads loaded-chunk blocks for
		// collision, calls getLocation() on cached player references, and pushes particle packets via Player#spawnParticle.
		// One pooled list is advanced per async-tick (no per-spurt task), so spawning lots of them stays cheap.
		private void runDecorativeSpurtsAsync() {
			Phase ph = phase;
			int ch = currentHeight;
			if (ph == Phase.RISING || ph == Phase.HOLD || ph == Phase.RETREAT) { // keep spraying while the column subsides too
				double surfLow = Math.max(baseY, surfaceY);
				double surfTop = Math.min(ch, surfLow + 18.0);
				List<Player> nearby = spurtPlayers;
				double widthScale = Math.max(0.55, Math.min(1.0, size / disasterRange));
				int spawn = Math.max(1, (int) Math.round((4 + level) * 0.28 * widthScale));
				for (int i = 0; i < spawn; i++) {
					double ang = random.nextDouble() * Math.PI * 2.0; // a random point around the column -> even over time
					double y;
					if (surfTop > surfLow && random.nextBoolean()) { // half from the surface eruption band
						y = surfLow + random.nextDouble() * (surfTop - surfLow);
					} else { // half near a nearby player's level so an eruption in their cave is visible all around the column
						if (!nearby.isEmpty())
							y = Math.max(baseY, Math.min(ch, nearby.get(random.nextInt(nearby.size())).getLocation().getY() - 4.0 + random.nextDouble() * 12.0));
						else if (surfTop > surfLow)
							y = surfLow + random.nextDouble() * (surfTop - surfLow);
						else
							y = Math.max(baseY, ch - random.nextDouble() * 6.0);
					}
					double launchAng = ang + (random.nextDouble() - 0.5) * 0.9; // vary the launch direction off the radial
					double sp = 0.45 + random.nextDouble() * 0.6, up = 0.2 + random.nextDouble() * 0.55;
					decoSpurts.add(new double[] {cx + 0.5 + Math.cos(ang) * (size + 0.3), y, cz + 0.5 + Math.sin(ang) * (size + 0.3),
							Math.cos(launchAng) * sp, up, Math.sin(launchAng) * sp, 14 + random.nextInt(10)});
				}
			}
			if (decoSpurts.size() > 600) // safety cap
				decoSpurts.subList(0, decoSpurts.size() - 600).clear();
			List<SpurtTarget> tgts = spurtTargets;
			java.util.Set<LivingEntity> hitThisTick = null; // dedupe: at most one scald task per entity per pass
			java.util.Iterator<double[]> it = decoSpurts.iterator();
			while (it.hasNext()) {
				double[] s = it.next();
				if (ph == Phase.RETREAT && s[1] > ch) { // as the column drops, spurts only live under its receding peak
					it.remove();
					continue;
				}
				int bx = (int) Math.floor(s[0]), by = (int) Math.floor(s[1]), bz = (int) Math.floor(s[2]);
				if (!world.isChunkLoaded(bx >> 4, bz >> 4)) { // off in an unloaded chunk -> no one to render it for, drop it
					it.remove();
					continue;
				}
				if (by >= world.getMinHeight() && by < world.getMaxHeight()) { // safe block read: chunk is loaded and Y is in bounds
					Block b = world.getBlockAt(bx, by, bz);
					if (!b.isPassable() || b.isLiquid()) { // travels through only passable, non-liquid blocks; else splash, gone
						renderSpurt(Particle.LAVA, s[0], s[1], s[2], 8, 0.2, 0.1, 0.2, 0.1);
						it.remove();
						continue;
					}
				}
				boolean hitEntity = false; // async math against the cached hitboxes; a hit hops to the main thread to verify+scald
				for (SpurtTarget st : tgts) {
					if (st.box.contains(s[0], s[1], s[2])) {
						if (hitThisTick == null)
							hitThisTick = new java.util.HashSet<>();
						if (hitThisTick.add(st.entity))
							scheduleSpurtScald(st.entity, s[0], s[1], s[2]);
						hitEntity = true;
						break;
					}
				}
				if (hitEntity) {
					renderSpurt(Particle.LAVA, s[0], s[1], s[2], 8, 0.2, 0.1, 0.2, 0.1);
					it.remove();
					continue;
				}
				renderSpurt(Particle.LAVA, s[0], s[1], s[2], 2, 0.03, 0.03, 0.03, 0.0);
				renderSpurt(Particle.FALLING_LAVA, s[0], s[1], s[2], 1, 0.02, 0.02, 0.02, 0.0);
				s[0] += s[3];
				s[1] += s[4];
				s[2] += s[5];
				s[4] -= 0.08;  // gravity
				s[3] *= 0.99;  // drag
				s[5] *= 0.99;
				if (--s[6] <= 0)
					it.remove();
			}
		}
		// Async-safe particle dispatch: sends the packet only to snapshot players within render range (getLocation() on a held
		// reference is safe off-main; Player#spawnParticle is just a packet).
		private void renderSpurt(Particle particle, double x, double y, double z, int count, double ox, double oy, double oz, double extra) {
			for (Player p : spurtPlayers) {
				Location pl = p.getLocation();
				double dx = pl.getX() - x, dy = pl.getY() - y, dz = pl.getZ() - z;
				if (dx * dx + dy * dy + dz * dz <= 1024.0) // within 32 blocks
					p.spawnParticle(particle, x, y, z, count, ox, oy, oz, extra);
			}
		}
		private List<Player> playersNearColumn(double r) {
			List<Player> list = new ArrayList<>();
			double rSq = r * r;
			for (Player p : world.getPlayers()) {
				if (p.getWorld() != world)
					continue;
				double dx = p.getLocation().getX() - (cx + 0.5), dz = p.getLocation().getZ() - (cz + 0.5);
				if (dx * dx + dz * dz <= rSq)
					list.add(p);
			}
			return list;
		}
		private boolean canIgnite(LivingEntity entity) {
			return !entity.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)
					&& !EntityUtils.isEntityImmunePlayer(entity)
					&& !FIRE_IMMUNE_TYPES.contains(entity.getType().name());
		}
		private void igniteBySpurt(LivingEntity victim, Location at) {
			if (!canIgnite(victim))
				return;
			int before = Math.max(0, victim.getFireTicks());
			if (before >= 160)
				return;
			victim.setFireTicks(160);
			if (victim.getFireTicks() > before)
				world.playSound(at, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.7f + random.nextFloat() * 0.3f);
		}
		// Main thread, every 5 ticks: snapshot every valid scaldable entity within the spurt range (disasterRange*5 = size*5)
		// together with a pre-expanded copy of its hitbox. The async spurt task tests collisions against these immutable boxes,
		// then bounces a verified hit back to the main thread to actually scald.
		private void refreshSpurtTargets() {
			double reach = size * 5.0; // spurt range
			double topY = Math.max(currentHeight, surfaceY) + 16.0;
			BoundingBox area = new BoundingBox(cx + 0.5 - reach, baseY - 4, cz + 0.5 - reach, cx + 0.5 + reach, topY, cz + 0.5 + reach);
			List<SpurtTarget> list = new ArrayList<>();
			for (Entity e : world.getNearbyEntities(area)) {
				if (!(e instanceof LivingEntity alive) || isEntityProtected(e))
					continue;
				if (!canIgnite(alive))
					continue;
				list.add(new SpurtTarget(alive, alive.getBoundingBox().expand(DECORATIVE_SPURT_HITBOX_EXPANSION)));
			}
			spurtTargets = list;
		}
		// A spurt collided (async) with a cached hitbox. Re-check on the main thread against the entity's CURRENT box in case
		// the async pass was working off stale data, then scald: fire + (off cooldown) damage and the extinguish hiss for everyone.
		private void scheduleSpurtScald(LivingEntity victim, double hx, double hy, double hz) {
			plugin.getServer().getScheduler().runTask(plugin, () -> {
				if (isFinished || victim.isDead() || !victim.isValid())
					return;
				if (!victim.getBoundingBox().expand(DECORATIVE_SPURT_HITBOX_EXPANSION).contains(hx, hy, hz)) // moved out since the async check -> no hit
					return;
				igniteBySpurt(victim, new Location(world, hx, hy, hz));
			});
		}
		// Every few seconds, one terrain-piercing spurt aimed straight at a random nearby entity (within ~13 blocks, any depth)
		// — this is the one that actually reaches and scalds them, distinct from the purely-decorative all-around spray.
		private void occasionalTargetedSpurt() {
			if (--spurtAimTimer > 0)
				return;
			spurtAimTimer = 50 + random.nextInt(50); // ~2.5-5 seconds
			double reach = size + 13;
			BoundingBox box = new BoundingBox(cx + 0.5 - reach, baseY - 2, cz + 0.5 - reach, cx + 0.5 + reach, currentHeight + 8, cz + 0.5 + reach);
			List<LivingEntity> targets = new ArrayList<>();
			for (Entity e : world.getNearbyEntities(box)) {
				if (!(e instanceof LivingEntity alive) || isEntityProtected(e))
					continue;
				if (!canIgnite(alive))
					continue;
				double edx = e.getLocation().getX() - (cx + 0.5), edz = e.getLocation().getZ() - (cz + 0.5);
				double eh = Math.sqrt(edx * edx + edz * edz);
				if (eh >= size + 0.5 && eh <= reach)
					targets.add(alive);
			}
			if (targets.isEmpty())
				return;
			LivingEntity t = targets.get(random.nextInt(targets.size()));
			Location el = t.getLocation();
			double edx = el.getX() - (cx + 0.5), edz = el.getZ() - (cz + 0.5);
			double eh = Math.sqrt(edx * edx + edz * edz);
			double clamped = Math.min(eh, 14.0);
			double dirX = edx / eh, dirZ = edz / eh; // straight at them — this one is meant to connect
			double sp = 0.7 + clamped * 0.06, up = 0.1 + clamped * 0.02;
			double startY = el.getY() + t.getHeight() * 0.5 - 3.0 + random.nextDouble() * 8.0;
			if (phase == Phase.RETREAT) // while subsiding the spurt must leave from under the receding peak
				startY = Math.min(startY, currentHeight - 0.5);
			launchSplashStream(startY, dirX, dirZ, new Vector(dirX * sp, up, dirZ * sp), true);
		}
		// As the rising column engulfs an open space (a cave / air pocket) it blasts any entity caught there clear with a
		// gout of steam — once each (tracked in surged). This is what makes a geyser erupting up through a cave still throw
		// you back and hiss steam at you, not only when it breaks the top surface.
		private void surgeThroughEntities() {
			double radius = shellRadius + 3.0 + level;
			double radiusSq = radius * radius;
			BoundingBox box = new BoundingBox(cx + 0.5 - radius, currentHeight - 2, cz + 0.5 - radius, cx + 0.5 + radius, currentHeight + 3, cz + 0.5 + radius);
			for (Entity e : world.getNearbyEntities(box)) {
				if (isEntityProtected(e) || surged.contains(e.getUniqueId()))
					continue;
				Location el = e.getLocation();
				double dx = el.getX() - (cx + 0.5), dz = el.getZ() - (cz + 0.5);
				if (dx * dx + dz * dz > radiusSq)
					continue;
				if (!el.getBlock().getType().isAir()) // only entities in an air pocket/cave the column is bursting into
					continue;                          // (not buried in rock, and not already standing in the column water)
				surged.add(e.getUniqueId());
				Vector away = new Vector(dx, 0, dz);
				if (away.lengthSquared() < 1.0E-4)
					away = Utils.getRandomizedVector(1, 0, 1);
				e.setVelocity(e.getVelocity().add(away.normalize().multiply(0.6 + level * 0.25).setY(0.5 + level * 0.08)));
				world.spawnParticle(Particle.LARGE_SMOKE, el.clone().add(0, e.getHeight() * 0.5, 0), 30, 0.6, 0.6, 0.6, 0.08);
				world.spawnParticle(Particle.LAVA, el, 15, 0.5, 0.5, 0.5, 0.1);
				if (e instanceof Player bp)
					playSound(bp, el, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.2f, 0.5f);
			}
		}
		// A single arcing stream of boiling water hurled out of the column from startY in direction (dirX,dirZ) with the
		// given velocity: it flies as a self-animating droplet (gravity + drag), trails splash particles, and ends when it
		// lands on the ground / hits a wall (splash burst) or strikes an entity (sets it on fire + a scald hit if off cooldown).
		private void launchSplashStream(double startY, double dirX, double dirZ, Vector vel, boolean pierce) {
			final Location pos = new Location(world, cx + 0.5 + dirX * (size + 0.2), startY, cz + 0.5 + dirZ * (size + 0.2));
			world.playSound(pos, Sound.BLOCK_LAVA_AMBIENT, 0.7f, 1.7f + random.nextFloat() * 0.3f); // the spray hissing out
			scheduleTask(new BukkitRunnable() {
				private int life;
				@Override
				public void run() {
					// Stop spraying once the geyser is gone / pooling. During subsidence it keeps spraying, but only below the
					// receding column peak.
					if (isFinished || !isColumnActive()) {
						cancel();
						return;
					}
					if (phase == Phase.RETREAT && pos.getY() > currentHeight) {
						cancel();
						return;
					}
					boolean openHere = pos.getBlock().isPassable();
					if (openHere) { // don't trail water particles while piercing through solid rock — only in open air/cave
						world.spawnParticle(Particle.LAVA, pos, 3, 0.04, 0.04, 0.04, 0.0);
						world.spawnParticle(Particle.FALLING_LAVA, pos, 2, 0.03, 0.03, 0.03, 0.0);
						// fill the gap back toward last tick's position so it reads as a continuous streak, not dots
						Location mid = pos.clone().subtract(vel.getX() * 0.5, vel.getY() * 0.5, vel.getZ() * 0.5);
						if (mid.getBlock().isPassable())
							world.spawnParticle(Particle.FALLING_LAVA, mid, 1, 0.02, 0.02, 0.02, 0.0);
					}
					// Aimed spurts (pierce) pass through terrain so they can still reach an entity standing in a crevice
					// below the surface; random arcs land on the first wall/ground as before.
					if (!pierce && life > 0 && !openHere) { // landed on ground / hit a wall
						world.spawnParticle(Particle.LAVA, pos, 14, 0.25, 0.1, 0.25, 0.12);
						cancel();
						return;
					}
					for (Entity e : world.getNearbyEntities(pos, AIMED_SPURT_SEARCH_RADIUS, AIMED_SPURT_VERTICAL_SEARCH_RADIUS, AIMED_SPURT_SEARCH_RADIUS)) {
						if (!(e instanceof LivingEntity alive) || isEntityProtected(e))
							continue;
						if (!canIgnite(alive))
							continue;
						// Check the actual hitbox, not the feet location, so a droplet at chest/head height still counts as a hit.
						if (!e.getBoundingBox().expand(AIMED_SPURT_HITBOX_EXPANSION).contains(pos.getX(), pos.getY(), pos.getZ()))
							continue;
						igniteBySpurt(alive, pos);
						world.spawnParticle(Particle.LAVA, pos, 12, 0.2, 0.2, 0.2, 0.1);
						cancel();
						return;
					}
					pos.add(vel);
					vel.setY(vel.getY() - 0.08); // gravity
					vel.multiply(0.99);
					if (++life > 50)
						cancel();
				}
			}.runTaskTimer(plugin, 0, 1));
		}
		private void tick(Geyser reference) {
			if (isFinished) // a finished geyser's per-geyser tick keeps firing until the whole disaster stops — do nothing
				return;
			ticks++;
			if (cracking)
				growCracks();
			if (isColumnActive() && ticks % 5 == 0) // refresh the entity-collision snapshot the async spurt task tests against
				refreshSpurtTargets();
			switch (phase) {
			case BUILDUP:
				buildupEffects();
				if (--phaseTimer <= 0) {
					phase = Phase.RISING;
					eruptionBurst();
				}
				break;
			case RISING:
				riseAccum += currentRiseRate();
				while (riseAccum >= 1.0) {
					riseAccum -= 1.0;
					buildLevel(currentHeight);
					if (++currentHeight >= maxHeight) {
						phase = Phase.HOLD;
						phaseTimer = 300; // sit at full height (fully launching entities) for 15 seconds before subsiding
						riseAccum = 0;
						buildCrown();
						eruptionBurst();
						break;
					}
				}
				// The ground only splits once the eruption column actually breaks the surface — and that emergence is the one
				// moment the geyser blasts nearby entities clear.
				if (!crackedSurface && currentHeight > surfaceY) {
					prepareCracks();
					emergenceBlast();
				}
				spawnPlume(false);
				sprinkleSides(); // also works underground (cave eruptions) via targetedSpurts
				playBoilSounds(true); // boiling sounds carry to underground players too (playColumnSound)
				if (currentHeight < surfaceY - 3)
					surgeThroughEntities(); // blast cave entities the rising column engulfs underground
				if (ticks % 10 == 0)
					playSustainedRoar();
				if (ticks % 5 == 0)
					steamCracks();
				break;
			case HOLD:
				spawnPlume(true);
				sprinkleSides();
				playBoilSounds(true);
				if (ticks % 10 == 0)
					playSustainedRoar();
				if (ticks % 5 == 0)
					steamCracks();
				if (ticks % 26 == 0)
					eruptionBurst();
				if (--phaseTimer <= 0) {
					phase = Phase.RETREAT;
					retreatStartHeight = currentHeight;
					// the async spurt task stops spawning and drains the pool itself once phase leaves RISING/HOLD —
					// don't clear() here (it would race the async iterator)
				}
				break;
			case RETREAT:
				// A real geyser subsides slowly: the water level eases down at first then accelerates, taking ~20s total.
				clearRetreatCap(); // remove last tick's domed cap before the column drops further
				riseAccum += retreatRate();
				int retreatTargetY = exitsIntoNaturalLavaLake ? naturalLavaLakeSurfaceY : baseY;
				while (riseAccum >= 1.0) {
					riseAccum -= 1.0;
					clearLevel(currentHeight);
					if (currentHeight-- <= retreatTargetY) {
						if (exitsIntoNaturalLavaLake)
							finishAtNaturalLavaLake(reference);
						else
							beginBoilingPool();
						return;
					}
				}
				buildRetreatCap(); // re-cap the new top with a rounded dome so it isn't a flat-topped cylinder
				sprinkleSides(); // keep spraying entities as the column drains (spurts capped under the receding peak)
				// While subsiding, drop the earth rumble entirely and just play a rolling rain hiss — but only to players the
				// falling water is still at/above, so it cuts off as the level passes their Y (see playRetreatLavaRush).
				if (--rainTimer <= 0) {
					playRetreatLavaRush();
					rainTimer = 30 + random.nextInt(30);
				}
				if (ticks % 4 == 0) {
					Location front = new Location(world, cx + 0.5, currentHeight, cz + 0.5);
					world.spawnParticle(Particle.LAVA, front, 6, size, 0.5, size, 0.1);
					world.spawnParticle(Particle.LARGE_SMOKE, front, 3, size, 0.5, size, 0.01);
				}
				break;
			case POOL:
				if (++poolTicks >= boilingDuration * 20)
					beginPoolDissipation();
				break;
			case DISSIPATE:
				if (--dissipateTimer <= 0) {
					drainPoolLayer(dissipateY--);
					dissipateTimer = 50 + random.nextInt(20); // a layer every ~3 seconds
					if (poolWater.isEmpty() || dissipateY < poolFloorY) {
						finishPool(reference);
						return;
					}
				}
				break;
			}
		}
		private boolean isColumnActive() {
			return phase == Phase.RISING || phase == Phase.HOLD || phase == Phase.RETREAT;
		}
		// Descent rate over the ~20s subsidence: starts slow and accelerates as the column drains (averages to the full
		// drop over RETREAT_TICKS). The factor 0.4..1.6 across the drop gives the slow-start / speed-up feel.
		private double retreatRate() {
			int span = Math.max(1, retreatStartHeight - baseY);
			double p = (retreatStartHeight - currentHeight) / (double) span; // 0 at the top -> 1 at the bottom
			if (p < 0) p = 0; else if (p > 1) p = 1;
			double avg = span / 350.0; // with the 0.4..1.6 accel curve this integrates to ~20 seconds of subsidence
			return avg * (0.4 + 1.2 * p);
		}
		private double currentRiseRate() {
			// Surging eruption: the column drastically speeds up and stalls instead of a constant climb.
			double surge = 0.55 + (0.65 * Math.abs(Math.sin(ticks * 0.16))) + random.nextDouble() * 0.25;
			return Math.max(0.35, baseRiseRate * surge);
		}
		private void buildLevel(int y) {
			for (int[] cell : disc) {
				if (cell[2] > shellRadiusSq)
					continue;
				Block block = world.getBlockAt(cx + cell[0], y, cz + cell[1]);
				if (cell[2] <= columnRadiusSq) {
					if (y <= spikeTop(cell[0], cell[1]))
						continue; // leave the bottom soul-sand spike poking up; don't drown it in column water
					// Physics OFF everywhere: the column is a tall sealed cylinder (much of it open sky); with vanilla fluid
					// physics every level gushes water outward and cascades across terrain (the old "water everywhere" bug).
					// It also avoids soul-sand bubble columns, which would push entities up — the geyser's launch is driven
					// deliberately by damageEntities instead, so it can be cleanly disabled while the column subsides.
					placeOwnedFluid(block);
				} else if (isNaturalFluid(block))
					continue;
				else if (Math.abs(y - surfaceY) <= 3)
					// Around the surface, fully clear the shell into a clean crater mouth. The deep jagged 1/3 carve left
					// undermined blocks (e.g. floating sandstone) hanging just under the surface inside the hole.
					removeBlock(block, false, false);
				else if (y < surfaceY && !block.isPassable() && random.nextDouble() < 0.14)
					// random magma REPLACING solid hole-wall blocks only — never placed into open air (cave/sky); ~14% (30% fewer than the old 20%)
					placeBlock(block, Material.MAGMA_BLOCK.createBlockData(), false, false);
				else if (random.nextInt(3) == 0)
					removeBlock(block, false, false);
			}
		}
		private int retreatCapHeight() {
			return 2 + level / 2;
		}
		// Removes the rounded water dome sitting on top of the receding column (rebuilt fresh each tick by buildRetreatCap).
		private void clearRetreatCap() {
			int capH = retreatCapHeight();
			for (int dy = 1; dy <= capH; dy++) {
				int y = currentHeight + dy;
				for (int[] cell : disc) {
					if (cell[2] > columnRadiusSq)
						continue;
					Block b = world.getBlockAt(cx + cell[0], y, cz + cell[1]);
					clearOwnedFluid(b);
				}
			}
		}
		// Caps the receding column with a rounded dome (full at its base, tapering to a point) so the top isn't flat.
		private void buildRetreatCap() {
			int capH = retreatCapHeight();
			for (int dy = 1; dy <= capH; dy++) {
				int y = currentHeight + dy;
				double rad = size * Math.cos((Math.PI / 2.0) * (dy / (double) (capH + 1))); // 1->0 over the cap, a dome
				double radSq = rad * rad;
				for (int[] cell : disc) {
					if (cell[2] <= columnRadiusSq && cell[2] <= radSq)
						placeOwnedFluid(world.getBlockAt(cx + cell[0], y, cz + cell[1]));
				}
			}
		}
		private void clearLevel(int y) {
			for (int[] cell : disc) {
				Block block = world.getBlockAt(cx + cell[0], y, cz + cell[1]);
				Material t = block.getType();
				// Clear WATER and any BUBBLE_COLUMN the soul-sand floor propagated up the column (otherwise the receding
				// water "stops" partway down where the column had turned to bubble columns that == material missed).
				if (t == material || t == Material.BUBBLE_COLUMN)
					clearOwnedFluid(block);
			}
		}
		private boolean isNaturalFluid(Block block) {
			return block.getType() == material && !ownedFluidBlocks.contains(block);
		}
		private boolean placeOwnedFluid(Block block) {
			if (block.getType() == material)
				return ownedFluidBlocks.contains(block);
			if (placeBlock(block, fluidData, true, false)) {
				ownedFluidBlocks.add(block);
				return true;
			}
			return false;
		}
		private void clearOwnedFluid(Block block) {
			if (!ownedFluidBlocks.remove(block))
				return;
			Material t = block.getType();
			if (t == material || t == Material.BUBBLE_COLUMN)
				block.setType(Material.AIR, false);
		}
		// Sculpts a jagged geyser cone rim around the vent mouth (the floor itself is the soul-sand/magma bed laid by
		// layBoilingFloor). The rim sits just outside the carved shaft so the rising column never eats it away.
		private void sculptVent() {
			Location floorCenter = new Location(world, cx + 0.5, baseY, cz + 0.5);
			float coneRadius = (float) (shellRadius + 1.0);
			for (Block b : BlockUtils.getBlocksInCircleCircumference(floorCenter, coneRadius)) {
				int height = 1 + random.nextInt(3);
				boolean spike = random.nextInt(7) == 0;
				if (spike)
					height += 2 + random.nextInt(3);
				for (int y = 0; y < height; y++) {
					Material mat = spike ? SPIKE_PALETTE[random.nextInt(SPIKE_PALETTE.length)] : RIM_PALETTE[random.nextInt(RIM_PALETTE.length)];
					placeBlock(world.getBlockAt(b.getX(), baseY + y, b.getZ()), mat);
				}
			}
		}
		// Real geysers fracture the surrounding ground into a rigid radial star of cracks. We split these at the terrain
		// surface the column erupts through (not the deep vent floor, which no one sees). Rather than appearing instantly
		// the arms are precomputed here and then grown outward block-by-block over a few seconds (see growCracks), starting
		// right at the hole rim so the star visibly connects to the vent mouth.
		private void prepareCracks() {
			if (crackedSurface)
				return;
			crackedSurface = true;
			clearCraterMouth(); // level the mouth (incl. sloped terrain) so no undermined blocks float in the hole
			repairSurfaceColumnFluid();
			double inner = Math.max(1.0, size); // begin at the water-column rim so the cracks join the hole, no gap
			crackInner = (int) inner;
			int arms = 5 + level / 2 + random.nextInt(3); // more spokes the stronger the geyser
			double baseAngle = random.nextDouble() * Math.PI * 2.0;
			double step = (Math.PI * 2.0) / arms;
			double reach = shellRadius * (2.2 + level * 0.5); // and they reach much further at higher levels
			for (int a = 0; a < arms; a++) {
				double angle = baseAngle + step * a + (random.nextDouble() - 0.5) * 0.18;
				addCrackArm(angle, inner, (int) (reach * (0.85 + random.nextDouble() * 0.4)) + 4, true);
				// A finer secondary fracture splitting the gap to the next spoke.
				double minor = baseAngle + step * (a + 0.5) + (random.nextDouble() - 0.5) * 0.4;
				addCrackArm(minor, inner, (int) (reach * 0.5 * (0.7 + random.nextDouble() * 0.5)) + 2, false);
			}
			cracking = true;
		}
		private void repairSurfaceColumnFluid() {
			int top = Math.min(currentHeight - 1, surfaceY + 1);
			for (int y = Math.max(baseY, surfaceY - 1); y <= top; y++)
				for (int[] cell : disc) {
					if (cell[2] <= columnRadiusSq && y > spikeTop(cell[0], cell[1]))
						placeOwnedFluid(world.getBlockAt(cx + cell[0], y, cz + cell[1]));
				}
		}
		// Each arm tracks its own walking head {px, pz, wander, dirX, dirZ, length, primary} so it can extend gradually.
		private void addCrackArm(double angle, double inner, int length, boolean primary) {
			double dirX = Math.cos(angle);
			double dirZ = Math.sin(angle);
			crackArms.add(new double[] {cx + 0.5 + dirX * inner, cz + 0.5 + dirZ * inner, angle, dirX, dirZ, length, primary ? 1 : 0});
		}
		// Advances every still-growing arm one block further out per growth step (paced so the full star takes a couple of
		// seconds), carving the fissure as it goes and rumbling the earth around the advancing tips.
		private void growCracks() {
			crackAccum += 0.75; // grow the star outward ~50% faster than before
			boolean carved = false;
			while (crackAccum >= 1.0) {
				crackAccum -= 1.0;
				int d = crackInner + crackStep;
				boolean anyActive = false;
				for (double[] arm : crackArms) {
					int length = (int) arm[5];
					if (d > length)
						continue;
					anyActive = true;
					boolean primary = arm[6] != 0;
					double t = (double) (d - crackInner) / Math.max(1, length - crackInner); // 0 at the vent -> 1 at the tip
					// Zigzag harder as the crack narrows toward the tip so it reads like a jagged, rigid fissure.
					double wanderAmt = (primary ? 0.32 : 0.5) * (0.6 + t);
					arm[2] += (random.nextDouble() - 0.5) * wanderAmt;
					arm[0] += Math.cos(arm[2]);
					arm[1] += Math.sin(arm[2]);
					int bx = (int) Math.floor(arm[0]);
					int bz = (int) Math.floor(arm[1]);
					double dirX = arm[3], dirZ = arm[4];
					int maxHalf = primary ? 2 + level / 2 : 1 + level / 4;  // wider around the hole, scaling with level
					int maxDepth = primary ? 2 + level : 1 + level / 2;     // crack depth scales with level
					int halfWidth = (int) Math.round(maxHalf * (1.0 - t));  // widest at the hole, shrinking to a point
					int depth = Math.max(1, (int) Math.round(maxDepth * (1.0 - t * 0.8)));
					for (int w = -halfWidth; w <= halfWidth; w++) {
						int wx = bx + (int) Math.round(-dirZ * w);
						int wz = bz + (int) Math.round(dirX * w);
						// a wide crack is a V-shaped fissure: shallower toward its edges than along the spine
						int edgeDepth = Math.max(1, depth - Math.abs(w));
						carveCrackColumn(wx, wz, surfaceY, edgeDepth, primary && w == 0);
					}
				}
				crackStep++;
				carved = carved || anyActive;
				if (!anyActive) {
					cracking = false;
					break;
				}
			}
			if (carved)
				crackRumble();
		}
		// An earthy cracking/grinding report emanating from a growing arm tip as the ground tears open.
		private void crackRumble() {
			if (crackArms.isEmpty())
				return;
			double[] arm = crackArms.get(random.nextInt(crackArms.size()));
			Location loc = new Location(world, arm[0], surfaceY, arm[1]);
			playSoundInLargeArea(loc, Sound.BLOCK_GRAVEL_BREAK, 1.1f, 0.55f + random.nextFloat() * 0.15f, soundRange());
			if (random.nextInt(3) == 0)
				playSoundInLargeArea(loc, Sound.BLOCK_STONE_BREAK, 0.8f, 0.45f, soundRange());
		}
		// Levels the crater mouth out to the shell radius down to just under the surface, removing any undermined blocks
		// (e.g. floating sandstone on a slope) that the column's jagged shell-carve leaves hanging inside the hole.
		private void clearCraterMouth() {
			int bottom = surfaceY - 2;
			for (int[] cell : disc) {
				if (cell[2] > shellRadiusSq)
					continue;
				int bx = cx + cell[0], bz = cz + cell[1];
				int top = localSurfaceTopY(bx, bz, surfaceY);
				clearAttachmentsAbove(bx, bz, top); // don't leave grass/flowers floating over the carved mouth
				for (int y = top; y >= bottom; y--) {
					Block b = world.getBlockAt(bx, y, bz);
					if (b.getType().isAir() || b.isLiquid())
						continue;
					removeBlock(b, false, false);
				}
			}
		}
		// Removes the loose plants/snow (non-solid attachments) sitting just above a freshly-carved surface cell so a
		// physics-off carve below doesn't leave them floating in mid-air.
		private void clearAttachmentsAbove(int bx, int bz, int topY) {
			for (int up = 1; up <= 2; up++) {
				Block above = world.getBlockAt(bx, topY + up, bz);
				Material t = above.getType();
				if (!t.isAir() && !t.isSolid() && !above.isLiquid())
					removeBlock(above, false, false);
			}
		}
		// Whether a block coordinate falls inside this geyser's vertical shaft (column/crown/pool footprint) — used by the
		// disaster's ice/spread guard to know which blocks it owns.
		boolean contains(int bx, int by, int bz) {
			if (by < baseY - 4 || by > Math.max(maxHeight + 8, currentHeight + DEATH_WATCHER_VERTICAL_PADDING))
				return false;
			double dx = bx + 0.5 - (cx + 0.5);
			double dz = bz + 0.5 - (cz + 0.5);
			double r = clearRadius + 1.5;
			return dx * dx + dz * dz <= r * r;
		}
		// True if any part of the entity (feet through head) overlaps the geyser's fluid, so it takes damage when only its
		// feet are in the water/lava, not just when its head is submerged.
		private boolean isTouchingMaterial(Entity e) {
			Location base = e.getLocation();
			double height = Math.max(1.0, e.getHeight());
			for (double dy = -0.1; dy <= height + 0.05; dy += 0.5)
				if (base.clone().add(0, dy, 0).getBlock().getType() == material)
					return true;
			return false;
		}
		// Once the column drains to the base, leave a shallow lava pool sitting on the soul-sand/magma floor. The lava itself
		// supplies the burn, ambience, and particles, so this phase only controls the pool lifetime and survival award timing.
		private void beginBoilingPool() {
			currentHeight = baseY;
			phase = Phase.POOL;
			poolTicks = 0;
			sculptBoilingPool();
		}
		private void finishAtNaturalLavaLake(Geyser reference) {
			currentHeight = naturalLavaLakeSurfaceY;
			isFinished = true;
			removeGeyser(reference);
		}
		private void sculptBoilingPool() {
			int poolDepth = 3;               // the deepest pits of the pool are 3 blocks
			poolFloorY = baseY - poolDepth;  // lowest possible pit floor (there is room above bedrock at ~-50)
			double poolRadius = size + 0.5;
			double poolRadiusSq = poolRadius * poolRadius;
			// Surface is at baseY. The exposed spike islands are the SAME soul-sand spikes raised at the start (formBottomSpikes);
			// every other cell becomes a lava pit 1-3 deep on a soul-sand/magma bed. Lava is physics-OFF.
			for (int[] cell : disc) {
				if (cell[2] > poolRadiusSq)
					continue;
				int bx = cx + cell[0], bz = cz + cell[1];
				if (spikeTop(cell[0], cell[1]) != Integer.MIN_VALUE)
					continue; // a pre-formed spike island — leave it standing
				int d = 1 + random.nextInt(poolDepth); // lava pit 1-3 deep, uneven bottom
				int pitFloor = baseY - d;
				placeBlock(world.getBlockAt(bx, pitFloor, bz), random.nextInt(3) == 0 ? Material.MAGMA_BLOCK : Material.SOUL_SAND);
				for (int y = pitFloor + 1; y <= baseY; y++) {
					Block w = world.getBlockAt(bx, y, bz);
					if (placeOwnedFluid(w))
						poolWater.add(w);
				}
			}
		}
		// The molten soundscape for the erupting column: lava pops and ambience without water/rain loops.
		private void playBoilSounds(boolean rain) {
			if (ticks % 10 == 0)
				playColumnSound(Sound.BLOCK_LAVA_POP, 0.5f + random.nextFloat() * 1.5f, 0.5f + random.nextFloat() * 1.0f, soundRange());
			if (--whirlpoolTimer <= 0) {
				playColumnSound(Sound.BLOCK_LAVA_AMBIENT, 1.2f, 0.5f + random.nextFloat() * 1.5f, soundRange());
				whirlpoolTimer = 20 + random.nextInt(41);
			}
			// A low molten rush off the lava column, spaced well apart so the ambience never stacks into a drone.
			if (rain && --rainTimer <= 0) {
				playColumnSound(Sound.BLOCK_LAVA_AMBIENT, 1.1f, 0.7f + random.nextFloat() * 0.15f, soundRange());
				rainTimer = 40 + random.nextInt(40);
			}
		}
		// The subsiding column's rain hiss, played ONLY to players the falling water is still at or above (sourced from the
		// column at the player's own height). As the water level drops past a player's Y it stops playing to them, so a
		// player high up where the geyser already cleared no longer hears it once it's draining away deep below.
		private void playRetreatLavaRush() {
			double lavaTop = currentHeight + retreatCapHeight();
			double range = soundRange();
			float pitch = 0.7f + random.nextFloat() * 0.15f;
			for (Player p : world.getPlayers()) {
				if (p.getWorld() != world)
					continue;
				Location pl = p.getLocation();
				if (pl.getY() > lavaTop + 2) // the water has already drained below this player -> no rain for them
					continue;
				double srcY = Math.min(pl.getY(), lavaTop); // the water still rushing past at their level
				Location src = new Location(world, cx + 0.5, srcY, cz + 0.5);
				double distance = pl.distance(src);
				if (distance > range)
					continue;
				Location at = pl.clone().add(Utils.getVectorTowards(pl, src).multiply(7.0 / range * distance));
				playSound(p, at, Sound.BLOCK_LAVA_AMBIENT, (float) (1.1 - ((1.1 / range) * (distance - range))), pitch);
			}
		}
		// Once the pool has finished boiling it doesn't vanish instantly — it plays its draining sounds, then dissipates one
		// water layer at a time from the top down, dropping a layer every few seconds until it's empty (then finishPool).
		private void beginPoolDissipation() {
			phase = Phase.DISSIPATE;
			dissipateY = baseY;                       // drain from the surface downward
			dissipateTimer = 50 + random.nextInt(20); // first layer drops after ~3 seconds
		}
		// Drains the pool cells at one Y level back to air. Each removeBlock leaves the block's captured original ground intact
		// so regen still restores stone (and clears any bubble columns); the soul-sand floor/spikes stay until regen.
		private void drainPoolLayer(int y) {
			java.util.Iterator<Block> it = poolWater.iterator();
			while (it.hasNext()) {
				Block w = it.next();
				if (w.getY() != y)
					continue;
				ownedFluidBlocks.remove(w);
				removeBlock(w, false, false);
				it.remove();
			}
		}
		private void finishPool(Geyser reference) {
			isFinished = true;
			// Safety: drop any pool water still left so regen restores solid ground without holes.
			for (Block w : new ArrayList<>(poolWater)) { ownedFluidBlocks.remove(w); removeBlock(w, false, false); poolWater.remove(w); }
			// Leave the soul-sand floor + spikes behind; the block-regen system restores everything on its own schedule.
			removeGeyser(reference);
		}
		private void carveCrackColumn(int bx, int bz, int refSurfaceY, int depth, boolean spine) {
			int topY = localSurfaceTopY(bx, bz, refSurfaceY);
			clearAttachmentsAbove(bx, bz, topY); // physics-off carve below would otherwise leave grass/flowers floating
			for (int i = 0; i < depth; i++) {
				Block b = world.getBlockAt(bx, topY - i, bz);
				if (b.getType().isAir() || b.isLiquid())
					continue;
				removeBlock(b, false, false);
			}
			Block floor = world.getBlockAt(bx, topY - depth, bz);
			if (!floor.getType().isAir() && !floor.isLiquid()) {
				int roll = random.nextInt(spine ? 4 : 6); // the deep spine glows hotter than the edges
				if (roll == 0)
					placeBlock(floor, Material.MAGMA_BLOCK); // a steaming-hot crack bottom
				else if (roll <= 2)
					placeBlock(floor, CRACK_FLOOR[random.nextInt(CRACK_FLOOR.length)]);
			}
			// remember a sample of crack mouths so they visibly steam throughout the eruption
			if (spine && crackVents.size() < 48 && random.nextInt(2) == 0)
				crackVents.add(new int[] {bx, topY - depth + 1, bz});
		}
		// Wisps of steam curling up out of the freshly-split cracks for the life of the eruption.
		private void steamCracks() {
			if (crackVents.isEmpty())
				return;
			int emit = Math.min(crackVents.size(), 4 + level);
			for (int i = 0; i < emit; i++) {
				int[] v = crackVents.get(random.nextInt(crackVents.size()));
				Location loc = new Location(world, v[0] + 0.5, v[1] + 0.1, v[2] + 0.5);
				world.spawnParticle(Particle.LARGE_SMOKE, loc, 2, 0.15, 0.05, 0.15, 0.01);
				if (random.nextInt(3) == 0)
					world.spawnParticle(Particle.LAVA, loc, 3, 0.2, 0.1, 0.2, 0.05);
			}
		}
		// Caps the plain flat column top with a frothing, irregular fountain crown that flares into a mushroom head. It
		// stays within the clear radius and currentHeight is advanced to its top so the retreat sweep still cleans it.
		private void buildCrown() {
			int capHeight = 3 + level / 2;
			double capRadius = Math.min(clearRadius - 1.0, size + 1.0 + level * 0.6);
			int r = (int) Math.ceil(capRadius);
			for (int dy = 0; dy < capHeight; dy++) {
				double shape = Math.sin(Math.PI * (dy + 1.0) / (capHeight + 1.0)); // bulge in the middle -> umbrella cap
				double rad = 0.7 + capRadius * shape;
				double radSq = rad * rad;
				double innerSq = Math.max(0.0, (rad - 1.3) * (rad - 1.3));
				int y = maxHeight + dy;
				for (int dx = -r; dx <= r; dx++)
					for (int dz = -r; dz <= r; dz++) {
						double d2 = dx * dx + dz * dz;
						if (d2 > radSq)
							continue;
						if (d2 > innerSq && random.nextInt(3) == 0) // ragged, frothy rim instead of a solid blob
							continue;
						Block block = world.getBlockAt(cx + dx, y, cz + dz);
						placeOwnedFluid(block);
					}
			}
			currentHeight = maxHeight + capHeight - 1;
			crownSpray(true);
		}
		// A fanning fountain of spray arcing up and out from the crown.
		private void crownSpray(boolean violent) {
			double capRadius = size + 1.0 + level * 0.6;
			Location cap = new Location(world, cx + 0.5, currentHeight + 0.5, cz + 0.5);
			int jets = (violent ? 16 : 8) + level * 2;
			for (int i = 0; i < jets; i++) {
				double ang = random.nextDouble() * Math.PI * 2.0;
				double outX = Math.cos(ang);
				double outZ = Math.sin(ang);
				Location from = cap.clone().add(outX * capRadius * 0.4, random.nextDouble() * 1.0, outZ * capRadius * 0.4);
				float speed = 0.35f + random.nextFloat() * 0.5f;
				world.spawnParticle(Particle.LAVA, from, 0, outX * 0.8, 0.5 + random.nextDouble() * 0.5, outZ * 0.8, speed);
				world.spawnParticle(Particle.FALLING_LAVA, from, 0, outX * 0.6, 0.3, outZ * 0.6, speed);
			}
			world.spawnParticle(Particle.LARGE_SMOKE, cap, violent ? 26 : 12, capRadius, 0.6, capRadius, 0.02);
			world.spawnParticle(Particle.FLAME, cap, violent ? 22 : 10, capRadius * 0.7, 0.7, capRadius * 0.7, 0.2);
			world.spawnParticle(particles, cap, violent ? 30 : 14, capRadius, 0.8, capRadius, 0.6);
		}
		private void buildupEffects() {
			Location floor = new Location(world, cx + 0.5, baseY + 0.2, cz + 0.5);
			world.spawnParticle(Particle.LARGE_SMOKE, floor, 4, size, 0.3, size, 0.01);
			world.spawnParticle(Particle.FLAME, floor, 6, size, 0.3, size, 0.05);
			if (ticks % 8 == 0) {
				world.spawnParticle(Particle.LAVA, floor, 10, size * 0.7, 0.2, size * 0.7, 0.2);
				playEarthRumble(0.6f); // pressure building deep underground
			}
		}
		private void eruptionBurst() {
			Location front = new Location(world, cx + 0.5, currentHeight, cz + 0.5);
			world.spawnParticle(Particle.LAVA, front, 40, size * 1.5, 1.0, size * 1.5, 0.4);
			world.spawnParticle(Particle.LARGE_SMOKE, front, 25, size * 1.5, 1.0, size * 1.5, 0.1);
			world.spawnParticle(Particle.FLAME, front, 30, size, 1.0, size, 0.3);
			playEruptionBlast();
		}
		// Fires exactly once, the instant the column bursts through the surface: every nearby entity is thrown clear with a
		// force (and radius) scaled by the disaster level. Afterwards the geyser never pushes entities again.
		private void emergenceBlast() {
			double cxC = cx + 0.5, czC = cz + 0.5;
			double radius = shellRadius + 1.0 + level * 1.8;
			double radiusSq = radius * radius;
			double force = (0.6 + level * 0.25) * 0.8;
			BoundingBox box = new BoundingBox(cxC - radius, surfaceY - 2, czC - radius, cxC + radius, surfaceY + 5, czC + radius);
			for (Entity e : world.getNearbyEntities(box)) {
				if (isEntityProtected(e))
					continue;
				double dx = e.getLocation().getX() - cxC;
				double dz = e.getLocation().getZ() - czC;
				if (dx * dx + dz * dz > radiusSq)
					continue;
				Vector away = new Vector(dx, 0, dz);
				if (away.lengthSquared() < 1.0E-4)
					away = Utils.getRandomizedVector(1, 0, 1);
				e.setVelocity(e.getVelocity().add(away.normalize().multiply(force).setY(0.5 + level * 0.08)));
				// Each blasted player hears the deep fireball-explode concussion right on top of them.
				if (e instanceof Player blasted)
					playSound(blasted, blasted.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.3f, 0.5f);
			}
			Location at = new Location(world, cxC, surfaceY + 0.5, czC);
			world.spawnParticle(Particle.LAVA, at, 80, radius * 0.4, 0.6, radius * 0.4, 0.6);
			// A massive steam burst: a ring of cloud puffs flung outward (and up) in every direction off the surface, so the
			// blast reads as a huge gout of steam hurling everyone clear.
			int puffs = 70 + level * 25;
			for (int i = 0; i < puffs; i++) {
				double ang = random.nextDouble() * Math.PI * 2.0;
				double outX = Math.cos(ang), outZ = Math.sin(ang);
				Location from = at.clone().add(outX * (size + 0.5), random.nextDouble() * 1.2, outZ * (size + 0.5));
				float sp = 0.4f + random.nextFloat() * 0.9f;
				world.spawnParticle(Particle.LARGE_SMOKE, from, 0, outX, 0.25 + random.nextDouble() * 0.5, outZ, sp);
			}
			world.spawnParticle(Particle.LARGE_SMOKE, at, 50, radius * 0.4, 0.6, radius * 0.4, 0.08);
			// The surface eruption boom carries far — radius scales with level (~10 blocks per level, so level 5 ~= 50).
			playSoundInLargeArea(at, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.4f, 0.5f, level * 10.0);
			playEruptionBlast();
		}
		private void spawnPlume(boolean violent) {
			Location front = new Location(world, cx + 0.5, currentHeight, cz + 0.5);
			int count = violent ? 18 : 10;
			world.spawnParticle(particles, front, count, size * 2, 1.5, size * 2, 1);
			world.spawnParticle(Particle.LAVA, front, count, size * 1.5, 1.0, size * 1.5, 0.3);
			world.spawnParticle(Particle.LARGE_SMOKE, front, violent ? 12 : 6, size * 1.5, 1.2, size * 1.5, 0.05);
			if (violent && ticks % 3 == 0)
				world.spawnParticle(Particle.FLAME, front, 20, size, 1.2, size, 0.4);
			if (violent)
				crownSpray(false);
		}
		// Sounds originate at the surface vent (ventLoc), not the deep base, so nearby surface players actually hear them.
		private double soundRange() {
			return Math.max(44.0, disasterRange * 6.0);
		}
		// Plays a sound sourced from the COLUMN at each nearby player's own height (clamped to the column's vertical span), so
		// a player underground / in a cave next to the geyser hears it from the shaft beside them — not from a far-off surface
		// point that may be out of range. Volume dims with distance like playSoundInLargeArea.
		private void playColumnSound(Sound sound, float vol, float pitch, double range) {
			// Span the whole earth column from bedrock up to at least the surface (or higher once the column rises above it),
			// so a player in a cave above the still-buried buildup hears the rumble from the ground beside them toward the vent.
			double bottom = baseY - 1, top = Math.max(surfaceY, currentHeight) + 2;
			for (Player p : world.getPlayers()) {
				if (p.getWorld() != world)
					continue;
				Location pl = p.getLocation();
				double srcY = Math.max(bottom, Math.min(top, pl.getY()));
				Location src = new Location(world, cx + 0.5, srcY, cz + 0.5);
				double distance = pl.distance(src);
				if (distance > range)
					continue;
				Location at = distance < 0.5 ? pl.clone() : pl.clone().add(Utils.getVectorTowards(pl, src).multiply(7.0 / range * distance));
				playSound(p, at, sound, (float) (vol - ((vol / range) * (distance - range))), pitch);
			}
		}
		// The deep "earth erupting" rumble that drives the rise: short, punchy heavy warden footfalls + a rock crack so it
		// can repeat on a tight cadence without the long cavein/dragon sounds overlapping into a drone.
		private void playEarthRumble(float vol) {
			double range = soundRange();
			playColumnSound(Sound.ENTITY_WARDEN_STEP, vol * 1.3f, 0.45f + random.nextFloat() * 0.2f, range);
			if (random.nextBoolean())
				playColumnSound(Sound.BLOCK_DEEPSLATE_BREAK, vol * 0.9f, 0.4f, range);
			else
				playColumnSound(Sound.BLOCK_GRAVEL_BREAK, vol * 0.9f, 0.45f, range);
		}
		// The eruption itself: a heavier earth-shaking footfall plus the burst of water heaving up out of the ground.
		private void playEruptionBlast() {
			double range = soundRange();
			playColumnSound(Sound.ENTITY_WARDEN_STEP, 1.6f, 0.35f, range + 8.0);
			playColumnSound(Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.4f, range);
			playColumnSound(Sound.ITEM_BUCKET_EMPTY_LAVA, 1.5f, 0.5f + random.nextFloat() * 0.1f, range + 10.0);
		}
		// The continuous "earth erupting" rumble while the column is up, on a steady cadence.
		private void playSustainedRoar() {
			playEarthRumble(1.0f);
		}
		private void damageEntities() {
			if (phase == Phase.BUILDUP)
				return;
			if (phase == Phase.POOL || phase == Phase.DISSIPATE)
				return;
			// Include the rounded retreat cap above currentHeight so an entity riding the receding water top is still covered.
			final int topY = currentHeight + (phase == Phase.RETREAT ? retreatCapHeight() : 2);
			BoundingBox box = new BoundingBox(cx + 0.5 - (shellRadius + 3), baseY - 2, cz + 0.5 - (shellRadius + 3),
					cx + 0.5 + (shellRadius + 3), topY + 2, cz + 0.5 + (shellRadius + 3));
			for (Entity e : world.getNearbyEntities(box)) {
				if (isEntityProtected(e))
					continue;
				Location loc = e.getLocation();
				double dx = loc.getX() - (cx + 0.5);
				double dz = loc.getZ() - (cz + 0.5);
				double horiz = dx * dx + dz * dz;
				// The column only affects entities actually inside its water (any body part touching). Nearby bystanders are
				// NOT scalded by proximity anymore — the aimed water spurts (sprinkleSides) deal the burn on a hit instead.
				if (horiz > columnRadiusSq || !isTouchingMaterial(e))
					continue;
				double horizDist = Math.sqrt(horiz);
				// Orbit them on a circle ~1.5 blocks inside the column's outer water edge (radius = size - 1.5), not jammed
				// at the dead center: pull toward that target radius rather than straight in.
				double targetR = Math.max(0.5, size - 1.5);
				double outX = horizDist > 0.1 ? dx / horizDist : 0;
				double outZ = horizDist > 0.1 ? dz / horizDist : 0;
				if (phase != Phase.RETREAT) {
					// Rising/holding: spiral them up around that ring.
					Vector swirl = new Vector(-dz, 0, dx);
					if (swirl.lengthSquared() > 1.0E-4)
						swirl.normalize().multiply(0.3);
					double radial = (targetR - horizDist) * 0.4; // <0 pulls in if outside the ring, >0 pushes out if inside
					e.setVelocity(new Vector(swirl.getX() + outX * radial, 2.7 + random.nextDouble() * 0.6, swirl.getZ() + outZ * radial));
				} else {
					// Subsiding: a draining whirlpool spiralling them down fast on that ring. BUT over the bottom 8 water blocks
					// the pull is greatly weakened so a player has a real chance to scramble out instead of being buried.
					boolean nearBottom = loc.getY() <= baseY + 8;
					if (!nearBottom) {
						// Higher up: full draining suction — override their velocity and spiral them down fast toward the bottom.
						Vector swirl = new Vector(-dz, 0, dx);
						if (swirl.lengthSquared() > 1.0E-4)
							swirl.normalize().multiply(0.5);
						double radial = (targetR - horizDist) * 0.55;
						e.setVelocity(new Vector(swirl.getX() + outX * radial, -1.2, swirl.getZ() + outZ * radial));
					}
					// Over the bottom 8 blocks: apply NO velocity at all (no spiral, no pull) so the entity moves entirely on
					// its own and can freely escape; it still takes the scald damage below while in the water.
					world.spawnParticle(Particle.FALLING_LAVA, loc, 6, 0.3, 0.4, 0.3, 0.0);
					world.spawnParticle(Particle.LAVA, loc, 4, 0.3, 0.2, 0.3, 0.1);
				}
			}
		}
	}
}
