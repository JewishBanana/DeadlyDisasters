package com.github.jewishbanana.deadlydisasters.disasters.destructive;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.disasters.Disaster;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class Tsunami extends Disaster implements Listener {

	private static final int[] ADJACENT_LANE_OFFSETS = { -1, 1 };
	private static final int CLIMB_DESCENT_DELAY_TICKS = 60;
	private static final int CLIMB_DESCENT_STEP_TICKS = 8;
	private static final double CLIMB_RADIUS_TOLERANCE = 0.35;

	private int minHeight;
	private int minWaterDepth;
	private double size;
	private double damage;
	private int maxFallingBlocks;
	private double debrisMultiplier;
	private int height;
	private int peak;
	private int maxOffset;
	private double waveSpeed;
	private int waterPersistTicks;
	private double impactDebrisChance;
	private int occlusionBins;
	private Particle particleType;
	private int debrisTick;

	private double[] radii;
	private boolean[] caughtUp;
	private double[] maxRadii;
	private boolean[] stopped;

	private final Set<Block> placedWater = new HashSet<>();
	private final Set<Block> waveWaterFootprint = new HashSet<>();
	private final Set<Block> temporaryAirWaveWater = new HashSet<>();
	private final Set<Block> climbedWater = new HashSet<>();
	private final List<Block> climbedWaterOrder = new ArrayList<>();
	private final Set<Block> verticalWakeWater = new HashSet<>();
	private final Set<Block> fastFadeWater = new HashSet<>();
	private final Set<Block> flowingFinals = new HashSet<>();
	private final Map<Block, Set<Block>> flowingFinalChildren = new HashMap<>();
	private final Map<Block, Integer> fadeQueue = new LinkedHashMap<>();
	private final Map<Block, Set<Block>> waterloggedChildren = new HashMap<>();
	private final Map<Block, Set<Block>> waterloggedSources = new HashMap<>();
	private final Map<Integer, Map<Integer, BlockedLane>> blockedWaveLanes = new HashMap<>();
	private final Map<Long, ClimbLane> climbingWaveLanes = new HashMap<>();
	private final Set<Long> repairedLanesThisTick = new HashSet<>();
	private final Deque<BackflowCell> backflowQueue = new ArrayDeque<>();
	private final List<UUID> debrisEntities = new ArrayList<>();
	private double dissolveBudget;
	private int floodRepairBudget;
	private boolean climbCleanupActive;

	public Tsunami(Location location, Player player, int level) {
		super(location, player, level);
		this.minHeight = getConfigInt("minimum_height");
		this.minWaterDepth = getConfigInt("minimum_water_depth");
	}
	public void init() {
		super.init();
		this.size = getConfigDouble("size");
		this.damage = 0.8 * level * getConfigDouble("damage_multiplier");
		this.maxFallingBlocks = getConfigInt("max_falling_blocks");
		this.debrisMultiplier = Math.max(0, getConfigDouble("debris_multiplier"));
		this.waterPersistTicks = Math.max(1, (int) (level * getConfigDouble("water_persist_seconds_per_level") * 20));
		switch (level) {
		default:
		case 1:
			disasterRange = 20;
			height = 3;
			break;
		case 2:
			disasterRange = 32;
			height = 6;
			break;
		case 3:
			disasterRange = 45;
			height = 8;
			break;
		case 4:
			disasterRange = 58;
			height = 12;
			break;
		case 5:
			disasterRange = 80;
			height = 16;
			break;
		case 6:
			disasterRange = 110;
			height = 23;
			break;
		}
		disasterRange *= size;
		this.peak = Math.max(1, (height + 1) / 2);
		this.maxOffset = Math.max(peak - 1, height - peak);
		this.waveSpeed = 0.06 + (level * 0.04);
		this.impactDebrisChance = 0.1 * level;
		this.occlusionBins = Math.max(64, (int) Math.ceil(disasterRange * Math.PI * 2.0 * 1.25));
		this.particleType = Particle.FALLING_WATER;
	}
	public Location findPossiblePosition(Location initial) {
		if (initial == null)
			return null;
		Block b = initial.getBlock();
		if (b.getType() == Material.WATER) {
			for (int i = 0; i < 255; i++) {
				Block above = b.getRelative(BlockFace.UP);
				if (above.getType() != Material.WATER)
					return BlockUtils.getCenterOfBlock(b);
				b = above;
			}
		} else {
			for (int i = 0; i < 255; i++) {
				b = b.getRelative(BlockFace.DOWN);
				if (b.getType() == Material.WATER) {
					while (b.getRelative(BlockFace.UP).getType() == Material.WATER)
						b = b.getRelative(BlockFace.UP);
					return BlockUtils.getCenterOfBlock(b);
				}
			}
		}
		return null;
	}
	public boolean canStart() {
		Location loc = getLocation();
		if (loc.getBlockY() < minHeight)
			return false;
		Block surface = loc.getBlock();
		if (surface.getType() != Material.WATER)
			return false;
		Block probe = surface;
		int depth = 0;
		for (int i = 0; i < minWaterDepth + 5; i++) {
			if (probe.getType() != Material.WATER)
				break;
			depth++;
			probe = probe.getRelative(BlockFace.DOWN);
		}
		if (depth < minWaterDepth)
			return false;
		int waterCount = 0;
		int total = 0;
		World world = loc.getWorld();
		int sx = loc.getBlockX();
		int sy = loc.getBlockY();
		int sz = loc.getBlockZ();
		for (int dx = -8; dx <= 8; dx += 2)
			for (int dz = -8; dz <= 8; dz += 2) {
				total++;
				if (world.getBlockAt(sx + dx, sy, sz + dz).getType() == Material.WATER)
					waterCount++;
			}
		if (waterCount < total * 0.6)
			return false;
		return super.canStart();
	}
	public void start() {
		super.start();
		addDeathWatcher("deaths.tsunami");
		plugin.getServer().getPluginManager().registerEvents(this, plugin);

		radii = new double[height + 1];
		caughtUp = new boolean[height + 1];
		maxRadii = new double[height + 1];
		stopped = new boolean[height + 1];
		for (int h = 1; h <= height; h++) {
			radii[h] = -1;
			caughtUp[h] = false;
			maxRadii[h] = Double.MAX_VALUE;
			stopped[h] = false;
		}

		final boolean useMiniWave = level >= 4;
		final int slowRingCount = Math.max(1, height / 3);
		final int slowRingDelay = 25;
		final double miniWaveDistance = disasterRange / 3.0;
		final int miniWaveDuration = (int) Math.ceil(miniWaveDistance / waveSpeed);
		final int miniWaveWidth = 3;
		final int slowBuildupStart = useMiniWave ? Math.max(20, miniWaveDuration / 3) : 0;
		final int mainEruptionDelay = slowBuildupStart + slowRingCount * slowRingDelay + 30;
		final int mainEruptionInterval = 4;
		final double catchUpSpeed = waveSpeed * 2.5;
		final int rampDownStep = 4;
		final int cleanupHeightDelay = 40;
		final double cleanupSpeed = waveSpeed * 2.0;

		scheduleTask(new BukkitRunnable() {
			private int totalTicks;
			private double miniRadius = -1;
			private boolean miniActive;
			private Set<Block> miniBlocks = new HashSet<>();
			private boolean mainErupted;
			private int mainEruptionTick;
			private int nextEruptionHeight;
			private boolean rampDown;
			private boolean climbPreCleanup;
			private int climbPreCleanupTicks;
			private boolean cleanup;
			private int activeCleanupHeight;
			private int cleanupTickCounter;
			private double[] cleanupRadii;
			private Map<Integer, Set<Block>> previousRings = new HashMap<>();

			@Override
			public void run() {
				try {
					processFadeQueue();
					processClimbDescents();
					++totalTicks;

					tickMiniWave();
					activateSlowRings();
					maybeStartMainEruption();

					if (cleanup) {
						tickCleanup();
						if (isCleanupSweepComplete())
							clearRemainingTrackedWater();
						if (placedWater.isEmpty() && fadeQueue.isEmpty()) {
							this.cancel();
							stop();
						}
						return;
					}

					if (!rampDown)
						advanceHeights();
					else
						advanceRampDown();

					repairedLanesThisTick.clear();
					floodRepairBudget = Math.max(1280, height * 256);
					Map<Integer, Set<Block>> newRings = new HashMap<>();
					for (int h = 1; h <= height; h++) {
						if (radii[h] < 0)
							continue;
						newRings.put(h, computeRing(radii[h], h - 1));
					}
					Set<Block> currentWaveSupport = applyRingDiff(previousRings, newRings);
					currentWaveSupport.addAll(placedWater);
					processBackflowQueue(currentWaveSupport);

					if (rampDown) {
						for (int h = 1; h <= height; h++) {
							if (!stopped[h])
								continue;
							Set<Block> ring = newRings.get(h);
							if (ring == null)
								continue;
							for (Block b : ring) {
								if (placedWater.contains(b) && !flowingFinals.contains(b)) {
									convertToFlowing(b);
									flowingFinals.add(b);
									queueFade(b);
								}
							}
						}
					}

					previousRings = newRings;

					if (mainErupted) {
						pushAndDamage();
						spawnDebris();
					}
					if (totalTicks % 12 == 0) {
						if (radii[height] >= 0)
							playFullWaveSound();
						double r = miniActive ? miniRadius : -1;
						for (int h = 1; h <= slowRingCount; h++)
							if (radii[h] > r && !caughtUp[h])
								r = radii[h];
						if (r > 0)
							playSmallRingSound(r);
					}
					pruneDeadDebris();

					if (rampDown && !climbPreCleanup)
						beginClimbedWaterPreCleanup();
					if (climbPreCleanup)
						tickClimbedWaterPreCleanup();
					if (rampDown && allHeightsStopped()) {
						if (isClimbedWaterPreCleanupComplete())
							beginCleanup();
					}
				} catch (Exception e) {
					Utils.sendExceptionLog(e);
				}
			}

			private void tickMiniWave() {
				if (useMiniWave && totalTicks == 1) {
					miniActive = true;
					miniRadius = 0;
				}
				if (!miniActive)
					return;
				miniRadius += waveSpeed;
				if (miniRadius >= miniWaveDistance) {
					for (Block b : miniBlocks)
						queueFade(b);
					miniBlocks.clear();
					miniActive = false;
					return;
				}
				Set<Block> newMini = computeWidthRing(miniRadius, 0, miniWaveWidth);
				Set<Block> toRemove = new HashSet<>(miniBlocks);
				toRemove.removeAll(newMini);
				for (Block b : toRemove)
					queueFade(b);
				Set<Block> supportBlocks = new HashSet<>(miniBlocks);
				supportBlocks.addAll(placedWater);
				supportBlocks.removeAll(fadeQueue.keySet());
				Set<Block> nextMini = new HashSet<>();
				for (Block b : newMini) {
					if (miniBlocks.contains(b)) {
						if (isPlacedWaveWater(b) || isWaveBlockType(b.getType()))
							nextMini.add(b);
						continue;
					}
					WavePlacement placement = placeRingWaterSource(b, 1, supportBlocks, null);
					if (placement.result == WaveBlockResult.FLOODED || placement.result == WaveBlockResult.OPEN)
						nextMini.add(placement.block == null ? b : placement.block);
				}
				miniBlocks = nextMini;
			}

			private void activateSlowRings() {
				for (int h = 1; h <= slowRingCount; h++) {
					int activationTick = slowBuildupStart + (h - 1) * slowRingDelay;
					if (totalTicks >= activationTick && radii[h] < 0) {
						radii[h] = 0;
						caughtUp[h] = false;
					}
				}
			}

			private void maybeStartMainEruption() {
				if (!mainErupted) {
					if (totalTicks < mainEruptionDelay)
						return;
					mainErupted = true;
					mainEruptionTick = totalTicks;
					nextEruptionHeight = slowRingCount + 1;
				}
				if (nextEruptionHeight > height)
					return;
				int elapsed = totalTicks - mainEruptionTick;
				int activatedCount = elapsed / mainEruptionInterval + 1;
				int targetHeight = Math.min(height, slowRingCount + activatedCount);
				while (nextEruptionHeight <= targetHeight) {
					if (radii[nextEruptionHeight] < 0) {
						radii[nextEruptionHeight] = 0;
						caughtUp[nextEruptionHeight] = false;
					}
					nextEruptionHeight++;
				}
			}

			private void advanceHeights() {
				if (mainErupted && radii[height] >= 0) {
					if (!caughtUp[height]) {
						double leaderTarget = computeLeaderTarget();
						radii[height] += catchUpSpeed;
						if (leaderTarget > 0 && radii[height] >= leaderTarget) {
							radii[height] = leaderTarget;
							caughtUp[height] = true;
						}
					} else {
						radii[height] += waveSpeed;
					}
					double leaderRadius = radii[height];
					double wavefrontPos = leaderRadius - (height - peak);
					for (int h = slowRingCount + 1; h < height; h++) {
						if (radii[h] < 0)
							continue;
						if (caughtUp[h]) {
							radii[h] = wavefrontPos + Math.abs(h - peak);
						} else {
							int offset = Math.abs(h - peak);
							double target = wavefrontPos + offset;
							radii[h] += catchUpSpeed;
							if (target > 0 && radii[h] >= target) {
								radii[h] = target;
								caughtUp[h] = true;
							}
						}
					}
					for (int h = 1; h <= slowRingCount; h++) {
						if (radii[h] < 0)
							continue;
						double target = wavefrontPos + Math.abs(h - peak);
						if (caughtUp[h]) {
							radii[h] = target;
						} else if (target > 0 && target >= radii[h]) {
							radii[h] = target;
							caughtUp[h] = true;
						} else {
							radii[h] += waveSpeed;
						}
					}
				} else if (mainErupted) {
					for (int h = slowRingCount + 1; h < height; h++) {
						if (radii[h] >= 0)
							radii[h] += catchUpSpeed;
					}
					for (int h = 1; h <= slowRingCount; h++) {
						if (radii[h] >= 0)
							radii[h] += waveSpeed;
					}
				} else {
					for (int h = 1; h <= slowRingCount; h++) {
						if (radii[h] >= 0)
							radii[h] += waveSpeed;
					}
				}
				if (mainErupted && caughtUp[height] && radii[height] >= disasterRange) {
					rampDown = true;
					for (int h = 1; h <= height; h++) {
						if (radii[h] < 0) {
							stopped[h] = true;
						} else {
							maxRadii[h] = disasterRange + rampDownStep * (height - h + 3);
							if (radii[h] >= maxRadii[h])
								stopped[h] = true;
						}
					}
				}
			}

			private double computeLeaderTarget() {
				double target = -1;
				for (int h = 1; h <= slowRingCount; h++) {
					if (radii[h] < 0)
						continue;
					double t = radii[h] - Math.abs(h - peak) + (height - peak);
					if (t > target)
						target = t;
				}
				return target;
			}

			private void advanceRampDown() {
				for (int h = 1; h <= height; h++) {
					if (radii[h] < 0 || stopped[h])
						continue;
					radii[h] += waveSpeed;
					if (radii[h] >= maxRadii[h]) {
						radii[h] = maxRadii[h];
						stopped[h] = true;
					}
				}
			}

			private boolean allHeightsStopped() {
				for (int h = 1; h <= height; h++)
					if (radii[h] >= 0 && !stopped[h])
						return false;
				return true;
			}

			private void beginClimbedWaterPreCleanup() {
				climbPreCleanup = true;
				climbCleanupActive = true;
				climbPreCleanupTicks = 0;
				climbingWaveLanes.clear();
				for (Block block : new ArrayList<>(climbedWater))
					queueFastFade(block, 1);
			}

			private void tickClimbedWaterPreCleanup() {
				climbPreCleanupTicks++;
				pruneStaleClimbedWater();
				int remaining = climbedWater.size();
				if (remaining <= 0)
					return;
				int targetTicks = 140;
				int budget = Math.max(1, (int) Math.ceil(remaining / (double) targetTicks));
				while (budget-- > 0 && !climbedWaterOrder.isEmpty()) {
					Block block = climbedWaterOrder.remove(climbedWaterOrder.size() - 1);
					if (!climbedWater.contains(block))
						continue;
					dissolveBlock(block);
				}
				if (climbedWaterOrder.isEmpty() && !climbedWater.isEmpty())
					for (Block block : new ArrayList<>(climbedWater))
						dissolveBlock(block);
				if (climbPreCleanupTicks >= 200)
					clearAllClimbedWater();
			}

			private boolean isClimbedWaterPreCleanupComplete() {
				pruneStaleClimbedWater();
				return climbedWater.isEmpty();
			}

			private void beginCleanup() {
				cleanup = true;
				climbPreCleanup = false;
				climbCleanupActive = false;
				climbingWaveLanes.clear();
				activeCleanupHeight = height;
				cleanupTickCounter = 0;
				cleanupRadii = new double[height + 1];
				for (int h = 0; h <= height; h++)
					cleanupRadii[h] = -1;
				cleanupRadii[activeCleanupHeight] = 0;
			}

			private void tickCleanup() {
				cleanupTickCounter++;
				if (cleanupTickCounter % cleanupHeightDelay == 0 && activeCleanupHeight > 1) {
					activeCleanupHeight--;
					cleanupRadii[activeCleanupHeight] = 0;
				}
				for (int h = 1; h <= height; h++) {
					if (cleanupRadii[h] < 0)
						continue;
					cleanupRadii[h] += cleanupSpeed;
					int targetY = location.getBlockY() + h - 1;
					double rSq = cleanupRadii[h] * cleanupRadii[h];
					Set<Block> toClear = new HashSet<>();
					for (Block b : placedWater) {
						if (b.getY() != targetY)
							continue;
						double dx = b.getX() + 0.5 - location.getX();
						double dz = b.getZ() + 0.5 - location.getZ();
						if (dx * dx + dz * dz <= rSq)
							toClear.add(b);
					}
					for (Block b : toClear)
						dissolveBlock(b);
				}
			}

			private boolean isCleanupSweepComplete() {
				if (cleanupRadii == null || activeCleanupHeight > 1 || cleanupRadii[1] < 0)
					return false;
				double targetRadius = maxRadii != null && maxRadii.length > 1 && maxRadii[1] < Double.MAX_VALUE
					? maxRadii[1] + 3.0
					: disasterRange + maxOffset + 8.0;
				return cleanupRadii[1] >= targetRadius;
			}

			private void clearRemainingTrackedWater() {
				for (Block block : new ArrayList<>(placedWater))
					dissolveBlock(block);
				fadeQueue.clear();
				clearLeftoverWaveWaterFootprint();
			}
		}.runTaskTimer(plugin, 0, 1));
	}
	public void clean() {
		super.clean();
		climbCleanupActive = false;
		fadeQueue.clear();
		blockedWaveLanes.clear();
		climbingWaveLanes.clear();
		climbedWater.clear();
		climbedWaterOrder.clear();
		verticalWakeWater.clear();
		fastFadeWater.clear();
		repairedLanesThisTick.clear();
		backflowQueue.clear();
		for (Block b : new ArrayList<>(placedWater))
			clearWaterBlock(b);
		clearLeftoverWaveWaterFootprint();
		temporaryAirWaveWater.clear();
		flowingFinals.clear();
		flowingFinalChildren.clear();
		clearTrackedWaterloggedBlocks();
		HandlerList.unregisterAll(this);
		for (UUID uuid : debrisEntities) {
			Entity e = Bukkit.getEntity(uuid);
			if (e != null)
				e.remove();
		}
		debrisEntities.clear();
		removeDeathWatcher(300);
	}
	private Set<Block> computeRing(double radius, int yOffset) {
		Set<Block> ring = new HashSet<>();
		World world = location.getWorld();
		int cx = location.getBlockX();
		int cy = location.getBlockY() + yOffset;
		int cz = location.getBlockZ();
		if (radius < 0.5) {
			ring.add(world.getBlockAt(cx, cy, cz));
			return ring;
		}
		int max = (int) Math.ceil(radius + 1.5);
		for (int dx = -max; dx <= max; dx++)
			for (int dz = -max; dz <= max; dz++) {
				double dist = Math.sqrt(dx * dx + dz * dz);
				double effective = radius + ringNoise(dx, dz, yOffset);
				if (Math.abs(dist - effective) <= 0.5)
					ring.add(world.getBlockAt(cx + dx, cy, cz + dz));
			}
		return ring;
	}
	private Set<Block> computeWidthRing(double radius, int yOffset, int width) {
		Set<Block> ring = new HashSet<>();
		World world = location.getWorld();
		int cx = location.getBlockX();
		int cy = location.getBlockY() + yOffset;
		int cz = location.getBlockZ();
		double inner = Math.max(0, radius - width);
		double outer = radius;
		int max = (int) Math.ceil(outer + 1.5);
		for (int dx = -max; dx <= max; dx++)
			for (int dz = -max; dz <= max; dz++) {
				double dist = Math.sqrt(dx * dx + dz * dz);
				double noise = ringNoise(dx, dz, yOffset);
				if (dist >= inner + noise && dist <= outer + noise)
					ring.add(world.getBlockAt(cx + dx, cy, cz + dz));
			}
		return ring;
	}
	private double ringNoise(int dx, int dz, int yOffset) {
		return Math.sin(dx * 0.55 + dz * 1.3 + yOffset * 0.27) * 0.45
				+ Math.sin(dx * 1.7 - dz * 0.4 + yOffset * 0.19) * 0.25;
	}
	private Set<Block> applyRingDiff(Map<Integer, Set<Block>> oldRings, Map<Integer, Set<Block>> newRings) {
		Set<Block> oldUnion = getRingUnion(oldRings);
		Set<Block> supportBlocks = new HashSet<>(oldUnion);
		supportBlocks.addAll(placedWater);
		supportBlocks.removeAll(fadeQueue.keySet());
		Map<Integer, Set<Block>> repairedRingCells = new HashMap<>();
		Map<Integer, Set<Block>> activeRingCells = new HashMap<>();
		for (Map.Entry<Integer, Set<Block>> entry : newRings.entrySet()) {
			int h = entry.getKey();
			Iterator<Block> ringIt = entry.getValue().iterator();
			while (ringIt.hasNext()) {
				Block b = ringIt.next();
				if (oldUnion.contains(b)) {
					cancelFade(b);
					continue;
				}
				WavePlacement placement = placeRingWaterSource(b, h, supportBlocks, repairedRingCells);
				if (placement.result == WaveBlockResult.BLOCKED || placement.result == WaveBlockResult.DEFERRED) {
					ringIt.remove();
					continue;
				}
				Block activeBlock = placement.block == null ? b : placement.block;
				if (!activeBlock.equals(b)) {
					ringIt.remove();
					activeRingCells.computeIfAbsent(h, k -> new HashSet<>()).add(activeBlock);
				}
				cancelFade(b);
				cancelFade(activeBlock);
			}
		}
		for (Map.Entry<Integer, Set<Block>> entry : repairedRingCells.entrySet())
			newRings.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).addAll(entry.getValue());
		for (Map.Entry<Integer, Set<Block>> entry : activeRingCells.entrySet())
			newRings.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).addAll(entry.getValue());
		processBlockedLaneWraps(supportBlocks, repairedRingCells);
		for (Map.Entry<Integer, Set<Block>> entry : repairedRingCells.entrySet())
			newRings.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).addAll(entry.getValue());
		fillVerticalWakeGaps(supportBlocks);
		Set<Block> newUnion = getRingUnion(newRings);
		for (Block b : oldUnion)
			if (!newUnion.contains(b)) {
				if (shouldFastFadeDetachedWater(b))
					queueFastFade(b, 8);
				else
					queueFade(b);
			}
		clearInactiveClimbedWater(newUnion);
		return newUnion;
	}
	private void clearInactiveClimbedWater(Set<Block> activeBlocks) {
		List<Block> toClear = new ArrayList<>();
		for (Block block : climbedWater) {
			if (activeBlocks.contains(block) && placedWater.contains(block) && !fadeQueue.containsKey(block))
				continue;
			toClear.add(block);
		}
		for (Block block : toClear)
			dissolveBlock(block);
	}
	private void clearAllClimbedWater() {
		for (Block block : new ArrayList<>(climbedWater))
			dissolveBlock(block);
	}
	private void pruneStaleClimbedWater() {
		Iterator<Block> it = climbedWater.iterator();
		while (it.hasNext()) {
			Block block = it.next();
			if (block != null && placedWater.contains(block) && isWaveBlockType(block.getType()))
				continue;
			it.remove();
		}
		climbedWaterOrder.removeIf(block -> !climbedWater.contains(block));
	}
	private void trackClimbedWater(Block block) {
		if (block == null)
			return;
		if (climbCleanupActive)
			return;
		if (climbedWater.add(block))
			climbedWaterOrder.add(block);
	}
	private void cancelFade(Block block) {
		if (block == null)
			return;
		fadeQueue.remove(block);
		fastFadeWater.remove(block);
		verticalWakeWater.remove(block);
	}
	private boolean shouldFastFadeDetachedWater(Block block) {
		return block != null && (block.getY() >= location.getBlockY() + height - 1 || verticalWakeWater.contains(block));
	}
	private Set<Block> getRingUnion(Map<Integer, Set<Block>> rings) {
		Set<Block> union = new HashSet<>();
		for (Set<Block> ring : rings.values())
			union.addAll(ring);
		return union;
	}
	private void fillVerticalWakeGaps(Set<Block> supportBlocks) {
		if (radii == null || radii[height] < 0)
			return;
		double dropRadius = radii[height] - getVerticalWakeLagDistance();
		if (dropRadius <= 0)
			return;
		int budget = Math.max(96, height * 24);
		Set<Block> sources = computeRing(dropRadius, height - 1);
		int minY = location.getBlockY();
		for (Block source : sources) {
			if (budget <= 0)
				return;
			if (!placedWater.contains(source) || !isWaveBlockType(source.getType()))
				continue;
			supportBlocks.add(source);
			Block next = source.getRelative(BlockFace.DOWN);
			while (budget > 0 && next.getY() >= minY) {
				if (isWaveBlockType(next.getType())) {
					supportBlocks.add(next);
					next = next.getRelative(BlockFace.DOWN);
					continue;
				}
				if (!canWaveOccupyWithoutBreakingSolid(next))
					break;
				WaveBlockResult result = placeWaterSource(next);
				if (result == WaveBlockResult.BLOCKED)
					break;
				supportBlocks.add(next);
				verticalWakeWater.add(next);
				queueFastFade(next, 8);
				budget--;
				next = next.getRelative(BlockFace.DOWN);
			}
		}
	}
	private double getVerticalWakeLagDistance() {
		return Math.max(1.0, maxOffset);
	}
	private WavePlacement placeRingWaterSource(Block block, int h, Set<Block> supportBlocks, Map<Integer, Set<Block>> repairedRingCells) {
		int lane = getLane(block);
		Map<Integer, BlockedLane> blocked = blockedWaveLanes.get(h);
		BlockedLane blockedLane = blocked == null ? null : blocked.get(lane);
		double radius = getHorizontalDistance(block);
		if (blockedLane != null && radius >= blockedLane.startRadius - 0.5) {
			blockedLane.lastRadius = Math.max(blockedLane.lastRadius, radius);
			WavePlacement resumed = tryResumeBlockedLane(block, h, lane, radius, blockedLane, supportBlocks, repairedRingCells);
			if (resumed.result != WaveBlockResult.DEFERRED)
				return resumed;
			return new WavePlacement(WaveBlockResult.DEFERRED, block);
		}
		Block target = getClimbTarget(block, h, lane, radius);
		if (!hasWaveSupport(target, h, radius, supportBlocks))
			return new WavePlacement(WaveBlockResult.DEFERRED, target);
		if (!hasClimbTerrainSupport(target, h))
			return new WavePlacement(WaveBlockResult.DEFERRED, target);
		WaveBlockResult result = placeWaterSource(target);
		if (result == WaveBlockResult.BLOCKED) {
			WavePlacement climbed = tryClimbWaveBlock(block, h, lane, radius, supportBlocks);
			if (climbed.result != WaveBlockResult.BLOCKED)
				return climbed;
			markLaneBlocked(h, lane, radius);
			return climbed;
		}
		updateClimbAfterForwardMove(h, lane, radius, target);
		supportBlocks.add(target);
		if (!repairedLanesThisTick.contains(getLaneKey(h, lane)))
			tryRepairAdjacentBlockedLanes(h, lane, radius, target, supportBlocks, repairedRingCells);
		return new WavePlacement(result, target);
	}
	private WavePlacement tryResumeBlockedLane(Block block, int h, int lane, double radius, BlockedLane blockedLane,
			Set<Block> supportBlocks, Map<Integer, Set<Block>> repairedRingCells) {
		Block target = getClimbTarget(block, h, lane, radius);
		if (!hasWaveSupport(target, h, radius, supportBlocks) || !hasClimbTerrainSupport(target, h))
			return new WavePlacement(WaveBlockResult.DEFERRED, target);
		WaveBlockResult result = placeWaterSource(target);
		if (result == WaveBlockResult.BLOCKED)
			return new WavePlacement(WaveBlockResult.DEFERRED, target);
		Map<Integer, BlockedLane> lanes = blockedWaveLanes.get(h);
		if (lanes != null) {
			lanes.remove(blockedLane.lane);
			if (lanes.isEmpty())
				blockedWaveLanes.remove(h);
		}
		updateClimbAfterForwardMove(h, lane, radius, target);
		supportBlocks.add(target);
		repairedLanesThisTick.add(getLaneKey(h, lane));
		if (repairedRingCells != null)
			repairedRingCells.computeIfAbsent(h, k -> new HashSet<>()).add(target);
		enqueueBackflow(h, lane, radius - 1.0, blockedLane.startRadius);
		return new WavePlacement(result, target);
	}
	private Block getClimbTarget(Block block, int h, int lane, double radius) {
		ClimbLane climb = climbingWaveLanes.get(getLaneKey(h, lane));
		if (climb == null || climb.offset <= 0)
			return block;
		if (radius + CLIMB_RADIUS_TOLERANCE < climb.startRadius)
			return block;
		if (!climb.descendingInPlace && radius >= climb.descentStartRadius) {
			Block lower = block.getRelative(0, climb.offset - 1, 0);
			if (canWaveOccupyWithoutBreakingSolid(lower))
				return lower;
		}
		return block.getRelative(0, climb.offset, 0);
	}
	private WavePlacement tryClimbWaveBlock(Block blockedBlock, int h, int lane, double radius, Set<Block> supportBlocks) {
		if (climbCleanupActive)
			return new WavePlacement(WaveBlockResult.BLOCKED, blockedBlock);
		long key = getLaneKey(h, lane);
		ClimbLane climb = climbingWaveLanes.computeIfAbsent(key, k -> new ClimbLane());
		if (climb.offset > 0 && radius + CLIMB_RADIUS_TOLERANCE < climb.startRadius)
			return new WavePlacement(WaveBlockResult.BLOCKED, blockedBlock);
		if (climb.usedClimb >= level) {
			beginClimbDescent(climb);
			return new WavePlacement(WaveBlockResult.BLOCKED, climb.lastActiveBlock == null ? blockedBlock : climb.lastActiveBlock);
		}
		if (climb.offset <= 0)
			climb.startRadius = radius;
		climb.offset++;
		climb.usedClimb++;
		climb.descentStartRadius = radius + getClimbDescentDelayRadius();
		climb.descendingInPlace = false;
		climb.descentTicks = 0;
		Block stepBlock = getClimbStepBlock(blockedBlock, climb.offset);
		WaveBlockResult result = placeWaterSource(stepBlock);
		if (result == WaveBlockResult.BLOCKED) {
			beginClimbDescent(climb);
			WaveBlockResult fallback = climb.lastActiveBlock == null ? WaveBlockResult.DEFERRED : WaveBlockResult.OPEN;
			return new WavePlacement(fallback, climb.lastActiveBlock == null ? blockedBlock : climb.lastActiveBlock);
		}
		climb.lastActiveBlock = stepBlock;
		trackClimbedWater(stepBlock);
		supportBlocks.add(stepBlock);
		return new WavePlacement(result, stepBlock);
	}
	private void updateClimbAfterForwardMove(int h, int lane, double radius, Block target) {
		ClimbLane climb = climbingWaveLanes.get(getLaneKey(h, lane));
		if (climb == null)
			return;
		if (climb.offset > 0 && radius + CLIMB_RADIUS_TOLERANCE < climb.startRadius)
			return;
		climb.descendingInPlace = false;
		climb.descentTicks = 0;
		if (climb.offset > 0 && target.getY() < location.getBlockY() + h - 1 + climb.offset) {
			climb.offset--;
			climb.descentStartRadius = radius + 1.0;
		}
		climb.lastActiveBlock = target;
		if (climb.offset > 0)
			trackClimbedWater(target);
	}
	private Block getClimbStepBlock(Block blockedBlock, int offset) {
		return blockedBlock.getRelative(0, offset, 0);
	}
	private boolean canWaveOccupyWithoutBreakingSolid(Block block) {
		return block.getType() == Material.AIR || isWaveBlockType(block.getType()) || block.isPassable();
	}
	private boolean hasClimbTerrainSupport(Block block, int h) {
		int baseY = location.getBlockY() + h - 1;
		if (block.getY() <= baseY)
			return true;
		Block below = block.getRelative(BlockFace.DOWN);
		return placedWater.contains(below) || !below.getType().isAir();
	}
	private double getClimbDescentDelayRadius() {
		return Math.max(3.0, waveSpeed * CLIMB_DESCENT_DELAY_TICKS);
	}
	private void beginClimbDescent(ClimbLane climb) {
		if (climb == null || climb.offset <= 0 || climb.descendingInPlace)
			return;
		climb.descendingInPlace = true;
		climb.descentTicks = CLIMB_DESCENT_DELAY_TICKS;
	}
	private boolean hasWaveSupport(Block block, int h, double radius, Set<Block> supportBlocks) {
		if (block == null)
			return false;
		if (radius <= 1.5)
			return true;
		if (isWaveSupportBlock(block, supportBlocks)
				|| isWaveSupportBlock(block.getRelative(BlockFace.UP), supportBlocks)
				|| isWaveSupportBlock(block.getRelative(BlockFace.DOWN), supportBlocks))
			return true;
		double dx = block.getX() + 0.5 - location.getX();
		double dz = block.getZ() + 0.5 - location.getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);
		if (dist > 0.01) {
			Block inward = block.getRelative(-(int) Math.round(dx / dist), 0, -(int) Math.round(dz / dist));
			if (isWaveSupportBlock(inward, supportBlocks))
				return true;
		}
		for (BlockFace face : new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST })
			if (isWaveSupportBlock(block.getRelative(face), supportBlocks))
				return true;
		for (int x : new int[] { -1, 1 })
			for (int z : new int[] { -1, 1 })
				if (isOpenDiagonalWaveSupport(block, x, z, supportBlocks))
					return true;
		return false;
	}
	private boolean isOpenDiagonalWaveSupport(Block block, int x, int z, Set<Block> supportBlocks) {
		Block diagonal = block.getRelative(x, 0, z);
		if (!isWaveSupportBlock(diagonal, supportBlocks))
			return false;
		Block sideX = block.getRelative(x, 0, 0);
		Block sideZ = block.getRelative(0, 0, z);
		return canWaveOccupyWithoutBreakingSolid(sideX)
				|| canWaveOccupyWithoutBreakingSolid(sideZ)
				|| isWaveSupportBlock(sideX, supportBlocks)
				|| isWaveSupportBlock(sideZ, supportBlocks);
	}
	private boolean isWaveSupportBlock(Block block, Set<Block> supportBlocks) {
		return block != null && supportBlocks.contains(block) && isWaveBlockType(block.getType());
	}
	private boolean isPlacedWaveWater(Block block) {
		return block != null && placedWater.contains(block) && isWaveBlockType(block.getType());
	}
	private WaveBlockResult placeWaterSource(Block block) {
		if (block == null)
			return WaveBlockResult.BLOCKED;
		boolean wasAir = block.getType() == Material.AIR;
		WaveBlockResult prepared = prepareWaveBlock(block);
		if (prepared != WaveBlockResult.FLOODED)
			return prepared;
		if (placeWaveWater(block, Material.WATER.createBlockData(), wasAir))
			placedWater.add(block);
		else
			return WaveBlockResult.BLOCKED;
		return WaveBlockResult.FLOODED;
	}
	private WaveBlockResult prepareWaveBlock(Block block) {
		if (block.getType() == Material.AIR)
			return WaveBlockResult.FLOODED;
		if (isWaveBlockType(block.getType()))
			return WaveBlockResult.OPEN;
		if (block.isPassable())
			return removeBlock(block, false, false) ? WaveBlockResult.FLOODED : WaveBlockResult.BLOCKED;
		if (debrisEntities.size() >= maxFallingBlocks || random.nextDouble() >= impactDebrisChance)
			return WaveBlockResult.BLOCKED;
		FallingBlock debris = convertBlockIntoFallingBlock(block);
		if (debris == null)
			return WaveBlockResult.BLOCKED;
		double dx = block.getX() + 0.5 - location.getX();
		double dz = block.getZ() + 0.5 - location.getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);
		Vector waveDir = (dist > 0.01)
			? new Vector(dx / dist, 0, dz / dist)
			: Utils.getRandomizedVector(1f, 0f, 1f).normalize();
		configureDebris(debris, getTurbulentWaveVelocity(waveDir, Math.max(0.28, waveSpeed * 1.4), 0.14, 0.08, -0.12, 0.28));
		return block.getType() == Material.AIR ? WaveBlockResult.FLOODED : WaveBlockResult.BLOCKED;
	}
	private void markLaneBlocked(int h, int lane, double radius) {
		Map<Integer, BlockedLane> lanes = blockedWaveLanes.computeIfAbsent(h, k -> new HashMap<>());
		int spread = getOcclusionLaneSpread(radius);
		for (int offset = -spread; offset <= spread; offset++) {
			int blockedLane = normalizeLane(lane + offset);
			BlockedLane blocked = lanes.get(blockedLane);
			if (blocked == null) {
				lanes.put(blockedLane, new BlockedLane(blockedLane, radius));
				continue;
			}
			blocked.lastRadius = Math.max(blocked.lastRadius, radius);
		}
	}
	private int getOcclusionLaneSpread(double radius) {
		double circumferencePerLane = (Math.PI * 2.0 * Math.max(1.0, radius)) / occlusionBins;
		return Math.max(1, Math.min(8, (int) Math.ceil(1.75 / Math.max(0.1, circumferencePerLane))));
	}
	private void processBlockedLaneWraps(Set<Block> supportBlocks, Map<Integer, Set<Block>> repairedRingCells) {
		if (blockedWaveLanes.isEmpty() || floodRepairBudget <= 0)
			return;
		int laneChecks = Math.max(128, height * 16);
		for (Map.Entry<Integer, Map<Integer, BlockedLane>> heightEntry : new ArrayList<>(blockedWaveLanes.entrySet())) {
			int h = heightEntry.getKey();
			if (h < 1 || h > height || radii == null || radii[h] < 0)
				continue;
			Map<Integer, BlockedLane> lanes = heightEntry.getValue();
			if (lanes == null || lanes.isEmpty())
				continue;
			for (BlockedLane blocked : new ArrayList<>(lanes.values())) {
				if (floodRepairBudget <= 0 || laneChecks-- <= 0 || !lanes.containsKey(blocked.lane))
					break;
				double repairRadius = Math.max(blocked.startRadius, radii[h]);
				if (repairRadius < blocked.startRadius + 0.5)
					continue;
				Block target = getBlockAtLane(h, blocked.lane, repairRadius);
				Block source = findNearbyWaveSupport(target, supportBlocks);
				if (source == null)
					continue;
				int before = floodRepairBudget;
				tryFloodAroundBlockedLane(h, blocked, repairRadius, source, supportBlocks, repairedRingCells, lanes);
				if (before == floodRepairBudget)
					blocked.lastRadius = Math.max(blocked.lastRadius, repairRadius);
				if (lanes.isEmpty()) {
					blockedWaveLanes.remove(h);
					break;
				}
			}
		}
	}
	private Block findNearbyWaveSupport(Block target, Set<Block> supportBlocks) {
		int search = Math.max(6, Math.min(14, level + 8));
		Block best = null;
		double bestDistance = Double.MAX_VALUE;
		for (int dx = -search; dx <= search; dx++)
			for (int dz = -search; dz <= search; dz++) {
				Block candidate = target.getRelative(dx, 0, dz);
				if (!isWaveSupportBlock(candidate, supportBlocks))
					continue;
				double dist = Math.abs(dx) + Math.abs(dz);
				if (dist < bestDistance) {
					bestDistance = dist;
					best = candidate;
				}
			}
		return best;
	}
	private void tryRepairAdjacentBlockedLanes(int h, int lane, double radius, Block sourceBlock, Set<Block> supportBlocks, Map<Integer, Set<Block>> repairedRingCells) {
		Map<Integer, BlockedLane> lanes = blockedWaveLanes.get(h);
		if (lanes == null || lanes.isEmpty())
			return;
		int maxRepairSteps = getFloodFillSteps();
		int maxSkippedSolidLanes = Math.max(8, level * 3);
		for (int dir : ADJACENT_LANE_OFFSETS) {
			int currentLane = lane;
			int skippedSolidLanes = 0;
			for (int step = 0; step < maxRepairSteps; step++) {
				int repairLane = normalizeLane(currentLane + dir);
				BlockedLane blocked = lanes.get(repairLane);
				if (blocked == null || radius < blocked.startRadius + 0.5)
					break;
				double repairRadius = Math.max(blocked.startRadius, radius);
				Block target = getBlockAtLane(h, blocked.lane, repairRadius);
				if (!hasWaveSupport(target, h, repairRadius, supportBlocks)) {
					if (tryFloodAroundBlockedLane(h, blocked, repairRadius, sourceBlock, supportBlocks, repairedRingCells, lanes) > 0) {
						currentLane = blocked.lane;
						continue;
					}
					blocked.lastRadius = Math.max(blocked.lastRadius, repairRadius);
					break;
				}
				WaveBlockResult result = placeWaterSource(target);
				if (result == WaveBlockResult.BLOCKED) {
					if (tryFloodAroundBlockedLane(h, blocked, repairRadius, sourceBlock, supportBlocks, repairedRingCells, lanes) > 0) {
						currentLane = blocked.lane;
						skippedSolidLanes = 0;
						continue;
					}
					blocked.lastRadius = Math.max(blocked.lastRadius, repairRadius);
					currentLane = blocked.lane;
					if (++skippedSolidLanes > maxSkippedSolidLanes)
						break;
					continue;
				}
				skippedSolidLanes = 0;
				lanes.remove(blocked.lane);
				if (lanes.isEmpty())
					blockedWaveLanes.remove(h);
				repairedLanesThisTick.add(getLaneKey(h, blocked.lane));
				supportBlocks.add(target);
				if (repairedRingCells != null)
					repairedRingCells.computeIfAbsent(h, k -> new HashSet<>()).add(target);
				enqueueBackflow(h, blocked.lane, repairRadius - 1.0, blocked.startRadius);
				currentLane = blocked.lane;
				if (lanes.isEmpty())
					break;
			}
		}
	}
	private int tryFloodAroundBlockedLane(int h, BlockedLane blocked, double radius, Block sourceBlock,
			Set<Block> supportBlocks, Map<Integer, Set<Block>> repairedRingCells, Map<Integer, BlockedLane> lanes) {
		if (sourceBlock == null || floodRepairBudget <= 0 || !isWaveSupportBlock(sourceBlock, supportBlocks))
			return 0;
		double corridor = Math.max(12.0, Math.min(44.0, level * 8.0 + getFloodFillSteps() * 1.25));
		double minRadius = Math.max(0.0, radius - corridor);
		double maxRadius = radius + 1.5;
		int targetY = sourceBlock.getY();
		int repaired = 0;
		int maxNodes = Math.min(floodRepairBudget, Math.max(384, getFloodFillSteps() * 48));
		Deque<Block> queue = new ArrayDeque<>();
		Set<Block> visited = new HashSet<>();
		queue.add(sourceBlock);
		visited.add(sourceBlock);
		while (!queue.isEmpty() && visited.size() <= maxNodes && floodRepairBudget > 0) {
			Block current = queue.pollFirst();
			for (BlockFace face : new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST }) {
				Block next = current.getRelative(face);
				if (next.getY() != targetY || !visited.add(next))
					continue;
				floodRepairBudget--;
				double nextRadius = getHorizontalDistance(next);
				if (nextRadius < minRadius || nextRadius > maxRadius)
					continue;
				if (!canWaveOccupyWithoutBreakingSolid(next)
						|| !hasClimbTerrainSupport(next, h)
						|| !hasWaveSupport(next, h, nextRadius, supportBlocks))
					continue;
				WaveBlockResult result = placeWaterSource(next);
				if (result == WaveBlockResult.BLOCKED)
					continue;
				supportBlocks.add(next);
				if (repairedRingCells != null) {
					int heightIndex = getWaveHeight(next);
					if (heightIndex >= 1 && heightIndex <= height)
						repairedRingCells.computeIfAbsent(heightIndex, k -> new HashSet<>()).add(next);
				}
				queue.addLast(next);
				int repairedLane = getLane(next);
				BlockedLane repairedBlock = lanes.remove(repairedLane);
				if (repairedBlock != null && nextRadius >= repairedBlock.startRadius - 0.5) {
					repairedLanesThisTick.add(getLaneKey(h, repairedLane));
					if (repairedRingCells != null)
						repairedRingCells.computeIfAbsent(h, k -> new HashSet<>()).add(next);
					enqueueBackflow(h, repairedLane, nextRadius - 1.0, repairedBlock.startRadius);
					repaired++;
					if (lanes.isEmpty()) {
						blockedWaveLanes.remove(h);
						return repaired;
					}
				}
			}
		}
		blocked.lastRadius = Math.max(blocked.lastRadius, radius);
		return repaired;
	}
	private int getFloodFillSteps() {
		return Math.max(8, Math.min(36, (int) Math.ceil(waveSpeed * 84.0)));
	}
	private void enqueueBackflow(int h, int lane, double radius, double minRadius) {
		if (radius <= minRadius + 0.5 || backflowQueue.size() >= 4096)
			return;
		backflowQueue.addLast(new BackflowCell(h, lane, radius, minRadius));
	}
	private void processBackflowQueue(Set<Block> supportBlocks) {
		int queueBudget = Math.min(backflowQueue.size(), Math.max(192, height * 32));
		int stepBudget = Math.max(384, height * 96);
		int maxSteps = getFloodFillSteps();
		for (int i = 0; i < queueBudget && stepBudget > 0; i++) {
			BackflowCell cell = backflowQueue.pollFirst();
			if (cell == null || cell.radius <= cell.minRadius + 0.5)
				continue;
			double nextRadius = cell.radius;
			boolean requeue = true;
			for (int step = 0; step < maxSteps && stepBudget > 0; step++) {
				if (nextRadius <= cell.minRadius + 0.5) {
					requeue = false;
					break;
				}
				Block target = getBlockAtLane(cell.height, cell.lane, nextRadius);
				if (!hasWaveSupport(target, cell.height, nextRadius, supportBlocks)) {
					break;
				}
				WaveBlockResult result = placeWaterSource(target);
				if (result == WaveBlockResult.BLOCKED) {
					requeue = false;
					break;
				}
				if (result == WaveBlockResult.FLOODED)
					queueFade(target);
				supportBlocks.add(target);
				nextRadius -= 1.0;
				stepBudget--;
			}
			if (requeue && nextRadius > cell.minRadius + 0.5)
				enqueueBackflow(cell.height, cell.lane, nextRadius, cell.minRadius);
		}
	}
	private int getLane(Block block) {
		double angle = Math.atan2(block.getZ() + 0.5 - location.getZ(), block.getX() + 0.5 - location.getX());
		if (angle < 0)
			angle += Math.PI * 2.0;
		return normalizeLane((int) Math.floor(angle / (Math.PI * 2.0) * occlusionBins));
	}
	private int normalizeLane(int lane) {
		int normalized = lane % occlusionBins;
		return normalized < 0 ? normalized + occlusionBins : normalized;
	}
	private long getLaneKey(int h, int lane) {
		return (((long) h) << 32) ^ normalizeLane(lane);
	}
	private double getHorizontalDistance(Block block) {
		double dx = block.getX() + 0.5 - location.getX();
		double dz = block.getZ() + 0.5 - location.getZ();
		return Math.sqrt(dx * dx + dz * dz);
	}
	private int getWaveHeight(Block block) {
		return block.getY() - location.getBlockY() + 1;
	}
	private Block getBlockAtLane(int h, int lane, double radius) {
		double angle = (normalizeLane(lane) + 0.5) / occlusionBins * Math.PI * 2.0;
		int x = location.getBlockX() + (int) Math.round(Math.cos(angle) * radius);
		int z = location.getBlockZ() + (int) Math.round(Math.sin(angle) * radius);
		int y = location.getBlockY() + h - 1;
		return location.getWorld().getBlockAt(x, y, z);
	}
	private void convertToFlowing(Block block) {
		if (block == null || block.getType() != Material.WATER)
			return;
		BlockData data = Material.WATER.createBlockData();
		if (data instanceof Levelled levelled) {
			levelled.setLevel(1);
			placeWaveWater(block, levelled, false);
		}
	}
	private boolean placeWaveWater(Block block, BlockData data) {
		return placeWaveWater(block, data, block.getType() == Material.AIR);
	}
	private boolean placeWaveWater(Block block, BlockData data, boolean temporaryAirWater) {
		if (!placeBlock(block, data, false, false))
			return false;
		if (data.getMaterial() == Material.WATER) {
			waveWaterFootprint.add(block);
			if (temporaryAirWater)
				temporaryAirWaveWater.add(block);
		}
		trackPotentialWaterloggedNeighbors(block);
		return true;
	}
	private void processClimbDescents() {
		if (climbCleanupActive)
			return;
		for (ClimbLane climb : climbingWaveLanes.values()) {
			if (!climb.descendingInPlace || climb.offset <= 0)
				continue;
			if (climb.lastActiveBlock == null || fadeQueue.containsKey(climb.lastActiveBlock) || !placedWater.contains(climb.lastActiveBlock)) {
				climb.descendingInPlace = false;
				climb.lastActiveBlock = null;
				continue;
			}
			if (climb.descentTicks > 0) {
				climb.descentTicks--;
				continue;
			}
			if (climb.lastActiveBlock != null)
				queueFade(climb.lastActiveBlock);
			climb.offset--;
			if (climb.offset <= 0) {
				climb.descendingInPlace = false;
				climb.lastActiveBlock = null;
				continue;
			}
			if (climb.lastActiveBlock != null) {
				Block lower = climb.lastActiveBlock.getRelative(BlockFace.DOWN);
				if (canWaveOccupyWithoutBreakingSolid(lower) && placeWaterSource(lower) != WaveBlockResult.BLOCKED) {
					climb.lastActiveBlock = lower;
					trackClimbedWater(lower);
				}
			}
			climb.descentTicks = CLIMB_DESCENT_STEP_TICKS;
		}
	}
	private void queueFade(Block block) {
		queueFade(block, waterPersistTicks);
	}
	private void queueFastFade(Block block, int ticks) {
		if (block == null || !placedWater.contains(block))
			return;
		fastFadeWater.add(block);
		queueFade(block, ticks);
	}
	private void queueFade(Block block, int ticks) {
		if (block == null || !placedWater.contains(block))
			return;
		int delay = Math.max(1, ticks);
		fadeQueue.merge(block, delay, Math::min);
	}
	private void processFadeQueue() {
		if (fadeQueue.isEmpty()) {
			dissolveBudget = 0;
			verticalWakeWater.removeIf(block -> !placedWater.contains(block));
			fastFadeWater.removeIf(block -> !placedWater.contains(block));
			return;
		}
		List<Block> due = new ArrayList<>();
		Iterator<Map.Entry<Block, Integer>> it = fadeQueue.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Block, Integer> entry = it.next();
			int remaining = entry.getValue() - 1;
			if (remaining <= 0) {
				entry.setValue(0);
				due.add(entry.getKey());
			} else {
				entry.setValue(remaining);
			}
		}
		if (due.isEmpty())
			return;
		int wakeBudget = Math.max(160, height * 32);
		Iterator<Block> dueIt = due.iterator();
		while (dueIt.hasNext() && wakeBudget > 0) {
			Block b = dueIt.next();
			if (!verticalWakeWater.contains(b) && !fastFadeWater.contains(b))
				continue;
			fadeQueue.remove(b);
			clearWaterBlock(b);
			dueIt.remove();
			wakeBudget--;
		}
		if (due.isEmpty())
			return;
		dissolveBudget += 1.0 / 3.0;
		int toDissolve = (int) dissolveBudget;
		if (toDissolve > due.size())
			toDissolve = due.size();
		dissolveBudget -= toDissolve;
		for (int i = 0; i < toDissolve; i++) {
			Block b = due.get(i);
			fadeQueue.remove(b);
			clearWaterBlock(b);
		}
	}
	private void clearWaterBlock(Block block) {
		if (block == null)
			return;
		climbedWater.remove(block);
		verticalWakeWater.remove(block);
		fastFadeWater.remove(block);
		boolean wasFinal = flowingFinals.remove(block);
		clearWaterloggedChildren(block);
		Set<Block> children = flowingFinalChildren.remove(block);
		if (placedWater.remove(block))
			clearTrackedWaveBlock(block);
		if (children != null) {
			for (Block child : children) {
				flowingFinals.remove(child);
				flowingFinalChildren.remove(child);
				verticalWakeWater.remove(child);
				fastFadeWater.remove(child);
				clearWaterloggedChildren(child);
				if (placedWater.remove(child))
					clearTrackedWaveBlock(child);
			}
		}
		if (wasFinal) {
			for (BlockFace face : new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.DOWN }) {
				Block adj = block.getRelative(face);
				if (adj.getType() != Material.WATER)
					continue;
				BlockData data = adj.getBlockData();
				if (data instanceof Levelled lev && lev.getLevel() > 0) {
					flowingFinals.remove(adj);
					flowingFinalChildren.remove(adj);
					if (placedWater.remove(adj)) {
						clearWaterloggedChildren(adj);
						clearTrackedWaveBlock(adj);
					}
				}
			}
		}
	}
	private void clearTrackedWaveBlock(Block block) {
		Material type = block.getType();
		if (isWaveBlockType(type) || isKelpBlock(type))
			clearWaveWater(block);
	}
	private void clearLeftoverWaveWaterFootprint() {
		if (waveWaterFootprint.isEmpty())
			return;
		for (Block block : new ArrayList<>(waveWaterFootprint)) {
			if (block == null)
				continue;
			if (block.getType() == Material.WATER)
				clearWaveWater(block);
		}
		waveWaterFootprint.clear();
	}
	private void trackWaterloggedBlock(Block source, Block waterlogged) {
		waterloggedChildren.computeIfAbsent(source, k -> new HashSet<>()).add(waterlogged);
		waterloggedSources.computeIfAbsent(waterlogged, k -> new HashSet<>()).add(source);
	}
	private void trackPotentialWaterloggedNeighbors(Block source) {
		for (BlockFace face : new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN }) {
			Block adj = source.getRelative(face);
			BlockData data = adj.getBlockData();
			if (data instanceof Waterlogged waterlogged && !waterlogged.isWaterlogged())
				trackWaterloggedBlock(source, adj);
		}
	}
	private void clearWaterloggedChildren(Block source) {
		Set<Block> children = waterloggedChildren.remove(source);
		if (children == null)
			return;
		for (Block child : children) {
			Set<Block> sources = waterloggedSources.get(child);
			if (sources != null) {
				sources.remove(source);
				if (!sources.isEmpty())
					continue;
				waterloggedSources.remove(child);
			}
			clearWaterloggedState(child);
		}
	}
	private void clearTrackedWaterloggedBlocks() {
		for (Block block : new ArrayList<>(waterloggedSources.keySet()))
			clearWaterloggedState(block);
		waterloggedChildren.clear();
		waterloggedSources.clear();
	}
	private void clearWaterloggedState(Block block) {
		BlockData data = block.getBlockData();
		if (!(data instanceof Waterlogged waterlogged) || !waterlogged.isWaterlogged())
			return;
		waterlogged.setWaterlogged(false);
		block.setBlockData(waterlogged, false);
	}
	private void clearWaveWater(Block block) {
		waveWaterFootprint.remove(block);
		removeBlock(block, false, false);
	}
	private boolean isWaveBlockType(Material material) {
		return material == Material.WATER
				|| material == Material.ICE
				|| material == Material.FROSTED_ICE
				|| material == Material.PACKED_ICE
				|| material == Material.BLUE_ICE;
	}
	private boolean isKelpBlock(Material material) {
		return material == Material.KELP || material == Material.KELP_PLANT;
	}
	private void dissolveBlock(Block block) {
		if (block == null)
			return;
		fadeQueue.remove(block);
		clearWaterBlock(block);
	}
	private void pushAndDamage() {
		double topRadius = radii[height];
		if (topRadius < 0)
			return;
		double scanR = topRadius + 1;
		World world = location.getWorld();
		Location centerY = location.clone().add(0, height / 2.0, 0);
		for (Entity e : world.getNearbyEntities(centerY, scanR, height + 4, scanR)) {
			if (isEntityProtected(e) || EntityUtils.isEntityImmunePlayer(e))
				continue;
			Location eLoc = e.getLocation();
			double dx = eLoc.getX() - location.getX();
			double dz = eLoc.getZ() - location.getZ();
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (dist > topRadius || !isInWater(eLoc))
				continue;
			Vector waveDir = (dist > 0.01)
				? new Vector(dx / dist, 0, dz / dist)
				: Utils.getRandomizedVector(1f, 0f, 1f).normalize();
			Vector currentVel = e.getVelocity();
			Vector velocity = getTurbulentWaveVelocity(waveDir, Math.max(0.42, waveSpeed * 2.05), 0.18, 0.10, -0.16, 0.34);
			currentVel.setX(velocity.getX());
			currentVel.setY(Math.max(-0.18, Math.min(0.42, currentVel.getY() * 0.35 + velocity.getY())));
			currentVel.setZ(velocity.getZ());
			e.setVelocity(currentVel);
			if (e instanceof LivingEntity alive && alive.getNoDamageTicks() == 0)
				if (EntityUtils.damageEntity(alive, damage, "deaths.tsunami", DamageCause.DROWNING) && !alive.isDead())
					alive.setNoDamageTicks(10);
			if (e instanceof Player p)
				p.spawnParticle(particleType, eLoc, 20, 1, 1.5, 1, 0.5);
		}
	}
	private boolean isInWater(Location loc) {
		Block feet = loc.getBlock();
		if (feet.getType() == Material.WATER)
			return true;
		Block above = feet.getRelative(BlockFace.UP);
		return above.getType() == Material.WATER;
	}
	private void spawnDebris() {
		if (debrisEntities.size() >= maxFallingBlocks || radii[height] < 0)
			return;
		World world = location.getWorld();
		debrisTick++;
		int attemptsPerPlayer = debrisTick % 2 == 0 ? 2 : 1;
		double spawnChance = level * 0.03 * debrisMultiplier;
		for (Player p : world.getPlayers()) {
			if (isEntityProtected(p) || EntityUtils.isEntityImmunePlayer(p))
				continue;
			Location pLoc = p.getLocation();
			if (isCaughtInTopWave(pLoc)) {
				if (random.nextFloat() >= spawnChance)
					continue;
				for (int i = 0; i < attemptsPerPlayer; i++)
					spawnDebrisAt(findDebrisSpawnNearPlayer(pLoc));
			} else if (isNearOutsideTopWave(pLoc) && random.nextFloat() < spawnChance * 0.75) {
				spawnDebrisAt(findDebrisSpawnOnWaveArc(pLoc));
			}
		}
	}
	private boolean spawnDebrisAt(Location spawnLoc) {
		if (spawnLoc == null || debrisEntities.size() >= maxFallingBlocks)
			return false;
		double dx = spawnLoc.getX() - location.getX();
		double dz = spawnLoc.getZ() - location.getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);
		if (dist > radii[height] || spawnLoc.getBlock().getType() != Material.WATER)
			return false;
		Vector waveDir = (dist > 0.01)
			? new Vector(dx / dist, 0, dz / dist)
			: Utils.getRandomizedVector(1f, 0f, 1f).normalize();
		Block ground = findGroundBlockBelow(spawnLoc.getBlock());
		if (ground == null)
			return false;
		FallingBlock fb = convertBlockIntoFallingBlock(ground);
		if (fb == null)
			return false;
		fb.teleport(spawnLoc);
		configureDebris(fb, getTurbulentWaveVelocity(waveDir, Math.max(0.38, waveSpeed * 1.95), 0.22, 0.16, -0.18, 0.38));
		return true;
	}
	private Block findGroundBlockBelow(Block waterBlock) {
		World world = waterBlock.getWorld();
		for (int y = waterBlock.getY() - 1; y >= world.getMinHeight(); y--) {
			Block block = world.getBlockAt(waterBlock.getX(), y, waterBlock.getZ());
			Material type = block.getType();
			if (type == Material.WATER || type == Material.AIR || block.isPassable())
				continue;
			return block;
		}
		return null;
	}
	private void configureDebris(FallingBlock debris, Vector velocity) {
		debris.setHurtEntities(true);
		debris.setDropItem(false);
		debris.setVelocity(velocity);
		debrisEntities.add(debris.getUniqueId());
	}
	private boolean isCaughtInTopWave(Location loc) {
		double topRadius = radii[height];
		if (topRadius < 0 || !isInWater(loc))
			return false;
		double dx = loc.getX() - location.getX();
		double dz = loc.getZ() - location.getZ();
		return dx * dx + dz * dz <= topRadius * topRadius;
	}
	private boolean isNearOutsideTopWave(Location loc) {
		double topRadius = radii[height];
		if (topRadius < 0)
			return false;
		double dx = loc.getX() - location.getX();
		double dz = loc.getZ() - location.getZ();
		double distSq = dx * dx + dz * dz;
		double outerRadius = topRadius + 35.0;
		return distSq > topRadius * topRadius && distSq <= outerRadius * outerRadius;
	}
	private Location findDebrisSpawnNearPlayer(Location playerLoc) {
		World world = playerLoc.getWorld();
		for (int i = 0; i < 10; i++) {
			double radius = Math.sqrt(random.nextDouble()) * 5.0;
			double angle = random.nextDouble() * Math.PI * 2.0;
			double x = playerLoc.getX() + Math.cos(angle) * radius;
			double z = playerLoc.getZ() + Math.sin(angle) * radius;
			double y = playerLoc.getY() + random.nextDouble(-1.0, 2.0);
			Location loc = new Location(world, x, y, z);
			Block water = loc.getBlock();
			if (water.getType() == Material.WATER)
				return water.getLocation().add(0.5, 0.15, 0.5);
		}
		return null;
	}
	private Location findDebrisSpawnOnWaveArc(Location playerLoc) {
		World world = playerLoc.getWorld();
		double topRadius = radii[height];
		if (topRadius < 0)
			return null;
		double playerAngle = Math.atan2(playerLoc.getZ() - location.getZ(), playerLoc.getX() - location.getX());
		double arcHalf = Math.toRadians(70.0 / 2.0);
		double minRadiusDepth = 5.0;
		double maxRadiusDepth = Math.min(15.0, topRadius);
		if (maxRadiusDepth < minRadiusDepth)
			return null;
		for (int i = 0; i < 12; i++) {
			double angle = playerAngle + random.nextDouble(-arcHalf, arcHalf);
			double radiusDepth = maxRadiusDepth <= minRadiusDepth ? minRadiusDepth : random.nextDouble(minRadiusDepth, maxRadiusDepth);
			double spawnRadius = Math.max(0.0, topRadius - radiusDepth);
			double x = location.getX() + Math.cos(angle) * spawnRadius;
			double z = location.getZ() + Math.sin(angle) * spawnRadius;
			double y = location.getY() + random.nextInt(Math.max(1, height));
			Block water = new Location(world, x, y, z).getBlock();
			if (water.getType() == Material.WATER)
				return water.getLocation().add(0.5, 0.15, 0.5);
		}
		return null;
	}
	private Vector getTurbulentWaveVelocity(Vector waveDir, double forwardSpeed, double sideNoise, double forwardNoise, double minY, double maxY) {
		Vector sideDir = new Vector(-waveDir.getZ(), 0, waveDir.getX());
		double speed = forwardSpeed + random.nextDouble(-forwardNoise, forwardNoise);
		return waveDir.clone().multiply(speed)
			.add(sideDir.multiply(random.nextDouble(-sideNoise, sideNoise)))
			.setY(random.nextDouble(minY, maxY));
	}
	private void pruneDeadDebris() {
		double cap = Math.max(0.5, waveSpeed * 2.25);
		double capSq = cap * cap;
		Iterator<UUID> it = debrisEntities.iterator();
		while (it.hasNext()) {
			Entity e = Bukkit.getEntity(it.next());
			if (e == null || e.isDead() || !e.isValid()) {
				it.remove();
				continue;
			}
			Vector v = e.getVelocity();
			Location eLoc = e.getLocation();
			double dx = eLoc.getX() - location.getX();
			double dz = eLoc.getZ() - location.getZ();
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (dist > radii[height] || !isInWater(eLoc)) {
				e.setVelocity(new Vector(0, Math.min(v.getY(), 0.0), 0));
				continue;
			}
			Vector waveDir = (dist > 0.01)
				? new Vector(dx / dist, 0, dz / dist)
				: Utils.getRandomizedVector(1f, 0f, 1f).normalize();
			v.multiply(0.35).add(getTurbulentWaveVelocity(waveDir, Math.max(0.30, waveSpeed * 1.35), 0.18, 0.12, -0.26, 0.32));
			double lenSq = v.lengthSquared();
			if (lenSq > capSq) {
				double scale = cap / Math.sqrt(lenSq);
				v.multiply(scale);
			}
			e.setVelocity(v);
		}
	}
	private void playFullWaveSound() {
		if (radii[height] < 0)
			return;
		double leaderRadius = radii[height];
		playSoundInLargeArea(location, Sound.WEATHER_RAIN_ABOVE,
			(float) Math.min(2.0, 0.4 * level), 0.5f,
			leaderRadius, 10.0 * level,
			loc -> loc.add(Utils.getVectorTowards(loc, location).multiply(4.0)));
	}
	private void playSmallRingSound(double radius) {
		if (radius <= 0)
			return;
		playSoundInLargeArea(location, Sound.WEATHER_RAIN_ABOVE,
			0.2f, 0.7f,
			radius, 15.0,
			loc -> loc.add(Utils.getVectorTowards(loc, location).multiply(2.0)));
	}
	private enum WaveBlockResult {
		FLOODED,
		OPEN,
		DEFERRED,
		BLOCKED
	}
	private static class WavePlacement {
		private final WaveBlockResult result;
		private final Block block;

		private WavePlacement(WaveBlockResult result, Block block) {
			this.result = result;
			this.block = block;
		}
	}
	private static class BlockedLane {
		private final int lane;
		private final double startRadius;
		private double lastRadius;

		private BlockedLane(int lane, double startRadius) {
			this.lane = lane;
			this.startRadius = startRadius;
			this.lastRadius = startRadius;
		}
	}
	private static class ClimbLane {
		private int offset;
		private int usedClimb;
		private double startRadius;
		private double descentStartRadius;
		private Block lastActiveBlock;
		private int descentTicks;
		private boolean descendingInPlace;
	}
	private static class BackflowCell {
		private final int height;
		private final int lane;
		private final double minRadius;
		private final double radius;

		private BackflowCell(int height, int lane, double radius, double minRadius) {
			this.height = height;
			this.lane = lane;
			this.radius = radius;
			this.minRadius = minRadius;
		}
	}
	private boolean isWithinActiveWaveArea(Block block) {
		if (block == null || radii == null || block.getWorld() != location.getWorld())
			return false;
		if (placedWater.contains(block)
				|| placedWater.contains(block.getRelative(BlockFace.UP))
				|| placedWater.contains(block.getRelative(BlockFace.DOWN)))
			return true;
		int y = block.getY();
		if (y < location.getBlockY() - 2 || y > location.getBlockY() + height + 1)
			return false;
		double activeRadius = -1;
		for (int h = 1; h <= height; h++)
			if (radii[h] >= 0)
				activeRadius = Math.max(activeRadius, radii[h] + Math.abs(h - peak) + 2.0);
		if (activeRadius < 0)
			return false;
		double dx = block.getX() + 0.5 - location.getX();
		double dz = block.getZ() + 0.5 - location.getZ();
		return dx * dx + dz * dz <= activeRadius * activeRadius;
	}
	private void restoreTrackedWaveWater(Block block) {
		if (block == null || !placedWater.contains(block) || isWaveBlockType(block.getType()))
			return;
		if (isKelpBlock(block.getType()) || block.getType() == Material.AIR)
			placeWaveWater(block, Material.WATER.createBlockData());
	}
	@EventHandler(ignoreCancelled = true)
	public void onKelpGrow(BlockGrowEvent event) {
		if (!isKelpBlock(event.getBlock().getType()) && !isKelpBlock(event.getNewState().getType()))
			return;
		Block block = event.getBlock();
		if (!isWithinActiveWaveArea(block))
			return;
		event.setCancelled(true);
		restoreTrackedWaveWater(block);
		restoreTrackedWaveWater(block.getRelative(BlockFace.UP));
		restoreTrackedWaveWater(block.getRelative(BlockFace.DOWN));
	}
	@EventHandler
	public void onWaterFlow(BlockFromToEvent event) {
		Block from = event.getBlock();
		if (!placedWater.contains(from))
			return;
		if (flowingFinals.contains(from)) {
			Block to = event.getToBlock();
			BlockData toData = to.getBlockData();
			if (toData instanceof Waterlogged waterlogged && !waterlogged.isWaterlogged()) {
				trackWaterloggedBlock(from, to);
				return;
			}
			if (to.getType() == Material.AIR) {
				event.setCancelled(true);
				BlockData data = Material.WATER.createBlockData();
				if (data instanceof Levelled levelled)
					levelled.setLevel(1);
				if (placeWaveWater(to, data)) {
					placedWater.add(to);
					flowingFinalChildren.computeIfAbsent(from, k -> new HashSet<>()).add(to);
				}
			}
			return;
		}
		event.setCancelled(true);
	}
	public Function<PlayerDeathEvent, Boolean> getDeathCheck() {
		return event -> {
			if (event.getEntity().getLastDamageCause() == null)
				return false;
			DamageCause cause = event.getEntity().getLastDamageCause().getCause();
			if (cause != DamageCause.DROWNING)
				return false;
			Location loc = event.getEntity().getLocation();
			double horizontalRangeSq = (disasterRange + maxOffset + 2) * (disasterRange + maxOffset + 2);
			if (loc.getY() < location.getY() - 2
					|| loc.getY() > location.getY() + height + 2
					|| !Utils.isLocationsWithinDistance(new Location(loc.getWorld(), loc.getX(), location.getY(), loc.getZ()), location, horizontalRangeSq))
				return false;
			return true;
		};
	}
	protected String getConfigPath() {
		return "disasters.destructive.tsunami";
	}
	public Set<Environment> getBannedEnvironments() {
		return EnumSet.of(Environment.NETHER, Environment.THE_END);
	}
}
