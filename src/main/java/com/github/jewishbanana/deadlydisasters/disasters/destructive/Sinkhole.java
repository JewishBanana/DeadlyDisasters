package com.github.jewishbanana.deadlydisasters.disasters.destructive;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.jewishbanana.deadlydisasters.disasters.Disaster;
import com.github.jewishbanana.deadlydisasters.events.DisasterStopEvent.DisasterStopReason;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.mojang.datafixers.util.Pair;

public class Sinkhole extends Disaster {
	
	private int minHeight;
	private double size;
	private double speed;
	private int lavaDepth;
	
	private Queue<CollapsingBlock> collapsingList = new ArrayDeque<>();
	private ArrayDeque<Block> modifiedOrder = new ArrayDeque<>();
	private Map<Block, Integer> firstBlockIndex = new HashMap<>();
	private Map<Integer, Block> reinsertOrdering = new TreeMap<>();
	
	public Sinkhole(Location location, Player player, int level) {
		super(location, player, level);
		
		this.minHeight = getConfigInt("minimum_height");
	}
	public void init() {
		super.init();
		this.size = getConfigDouble("size");
		this.speed = getConfigDouble("speed");
		this.lavaDepth = getConfigInt("lava_depth");
		switch (level) {
		default:
		case 1:
			disasterRange = 6;
			break;
		case 2:
			disasterRange = 15;
			break;
		case 3:
			disasterRange = 20;
			break;
		case 4:
			disasterRange = 25;
			break;
		case 5:
			disasterRange = 35;
			break;
		case 6:
			disasterRange = 50;
			break;
		}
		disasterRange *= size;
	}
	public boolean canStart() {
		if (getLocation().getBlockY() < minHeight)
			return false;
		return super.canStart();
	}
	public void start() {
		super.start();
		Set<Block> blocks = new HashSet<>(BlockUtils.getBlocksInCircleRadius(location, disasterRange));
		if (blocks.isEmpty()) {
			stop();
			return;
		}
		addDeathWatcher("deaths.sinkhole");
		final double centerDepth = Math.pow(2.0, level + 2);
		int actualDepth = Math.max((int) (location.getBlockY() - centerDepth), -60) + random.nextInt(level * 2) + (level * 6);
		if (actualDepth < lavaDepth)
			lavaDepth = actualDepth;
		scheduleTask(new BukkitRunnable() {
			private final double noise = 0.3;
			
			@Override
			public void run() {
				try {
					Set<CollapsingBlock> collapsing = new HashSet<>();
//					Set<Block> selected = new HashSet<>();
					for (Block next : blocks) {
						Block block = getHighestExposedBlock(next, level * 3 + random.nextInt(level), temp -> !temp.isPassable() || temp.isLiquid());
						if (block == null)
							continue;
						double distance = BlockUtils.getCenterOfBlock(block).distance(location);
						int depth = (int) (Math.floor(centerDepth * Math.pow(1.0 - (distance / disasterRange), 2.0)) + ((random.nextDouble() * 2 - 1) * noise * (1.0 - distance)));
						if (depth > 0)
							collapsing.add(new CollapsingBlock(block, distance + random.nextDouble(level), (int) (depth + Math.max(block.getY() - location.getY(), 0))));
//						selected.add(block);
					}
//					double radiusSquared = (radius - 1) * (radius - 1);
//					for (Block next : blocks) {
//						if (Utils.getCenterOfBlock(next).distanceSquared(location) > radiusSquared)
//							continue;
//						for (Block adjacent : findAttachedBlocks(next, level + 1, selected)) {
//							double distance = Utils.getCenterOfBlock(adjacent).distance(location);
//							collapsing.add(new CollapsingBlock(adjacent, distance, (int) Math.floor(centerDepth * Math.pow(1.0 - (distance / radius), 2.0))));
//							selected.add(adjacent);
//						}
//					}
					collapsingList = collapsing.stream()
						    .sorted(Comparator
						        .comparingInt((CollapsingBlock cb) -> cb.block.getY())   // sort by Y first
						        .thenComparingDouble(cb -> cb.distance))                 // then by distance
						    .collect(Collectors.toCollection(ArrayDeque::new));
					final double excessSoundRange = level * 7.0;
					final double soundRange = (disasterRange + excessSoundRange) * (disasterRange + excessSoundRange);
					final double distanceSquared = disasterRange * disasterRange;
					location.getWorld().getPlayers().forEach(player -> {
						Location loc = player.getLocation();
						if (!loc.getWorld().equals(location.getWorld()))
							return;
						double distance = loc.distanceSquared(location);
						if (distance > soundRange)
							return;
						if (distance > distanceSquared)
							playSound(player, loc.add(Utils.getVectorTowards(loc, location).multiply(7.0)), Sound.ITEM_TOTEM_USE, (0.33 * level) * (1.0 - ((1.0 / excessSoundRange) * (loc.distance(location) - disasterRange))), 0.5);
						else
							playSound(player, loc.subtract(0, 7, 0), Sound.ITEM_TOTEM_USE, (0.33 * level), 0.5);
					});
					scheduleTask(new BukkitRunnable() {
						private double distance = 1.0;
						private double increment = level / 20.0 * speed;
						private Iterator<CollapsingBlock> iterator = collapsingList.iterator();
						private final int iterationsPerTick = Math.max(collapsingList.size() / 5, 1);
						private int tick;
						
						@Override
						public void run() {
							while (++tick <= iterationsPerTick && iterator.hasNext()) {
								CollapsingBlock entry = iterator.next();
								if (entry.distance > distance)
									continue;
								entry.fall();
								if (entry.depth == 0) {
									Integer index = firstBlockIndex.remove(entry.first);
									if (index != null) {
										Block last = modifiedOrder.getLast();
										if (last.equals(entry.block) && !last.equals(entry.first)) {
											modifiedOrder.removeLast();
											reinsertOrdering.put(index, last);
										}
									}
									iterator.remove();
								}
							}
							if (!iterator.hasNext()) {
								if (collapsingList.isEmpty()) {
									stop();
									return;
								}
								iterator = collapsingList.iterator();
							}
							tick = 0;
							distance += increment;
						}
					}.runTaskTimer(plugin, 0, 1));
					
//					Set<Queue<CollapsingBlock>> collapsing = new HashSet<>();
//					for (Block next : blocks) {
//						Block block = getHighestExposedBlock(next, level * 2 + random.nextInt(level), temp -> !temp.isPassable() || temp.isLiquid());
//						if (block == null)
//							continue;
//						double distance = Utils.getCenterOfBlock(block).distance(location);
//						int depth = (int) (Math.floor(centerDepth * Math.pow(1.0 - (distance / radius), 2.0)) + ((random.nextDouble() * 2 - 1) * noise * (1.0 - distance)));
//						if (depth > 0) {
//							Queue<CollapsingBlock> queue = new ArrayDeque<>();
//							double trueDistance = distance + random.nextDouble(level);
//							for (int i=0; i < level * 4; i++) {
//								if (!block.isPassable() || block.isLiquid())
//									queue.add(new CollapsingBlock(block, trueDistance, depth));
//								block = block.getRelative(BlockFace.UP);
//								if (block == null)
//									break;
//							}
//							collapsing.add(queue);
//						}
//					}
//					scheduleTask(new BukkitRunnable() {
//						private double distance = 1.0;
//						private double increment = level / 20.0 * speed;
//						private Iterator<Queue<CollapsingBlock>> iterator = collapsing.iterator();
//						private final int iterationsPerTick = Math.max(collapsing.size() / 5, 1);
//						private int tick;
//						
//						@Override
//						public void run() {
//							while (++tick <= iterationsPerTick && iterator.hasNext()) {
//								Queue<CollapsingBlock> entry = iterator.next();
//								CollapsingBlock first = entry.peek();
//								if (first.distance > distance)
//									continue;
//								entry.forEach(cb -> cb.fall());
//								if (first.depth == 0) {
//									entry.forEach(cb -> {
//										Integer index = firstBlockIndex.remove(cb.first);
//										if (index != null) {
//											Block last = modifiedOrder.getLast();
//											if (last.equals(cb.block) && !last.equals(cb.first)) {
//												modifiedOrder.removeLast();
//												reinsertOrdering.put(index, last);
//											}
//										}
//									});
//									iterator.remove();
//								}
//							}
//							if (!iterator.hasNext()) {
//								if (collapsing.isEmpty()) {
//									stop();
//									return;
//								}
//								iterator = collapsing.iterator();
//							}
//							tick = 0;
//							distance += increment;
//						}
//					}.runTaskTimer(plugin, 0, 1));
				} catch (Exception e) {
					Utils.sendExceptionLog(e);
				}
			}
		}.runTaskAsynchronously(plugin));
	}
	public void clean() {
		super.clean();
		removeDeathWatcher(300);
	}
	public void regenerateBlocks(DisasterStopReason reason) {
		Set<Block> set = getModifiedBlocks();
		modifiedOrder.forEach(block -> set.remove(block));
		ArrayDeque<Block> rebuilt = new ArrayDeque<>(modifiedOrder.size());
		Iterator<Block> iterator = modifiedOrder.iterator();
		int cursor = 0;
		for (Map.Entry<Integer, Block> entry : reinsertOrdering.entrySet()) {
			while (cursor <= entry.getKey() && iterator.hasNext()) {
				rebuilt.add(iterator.next());
				++cursor;
			}
			rebuilt.add(entry.getValue());
		}
		while (iterator.hasNext())
			rebuilt.add(iterator.next());
		set.addAll(rebuilt);
		if (!collapsingList.isEmpty()) {
			Set<Block> blocks = new LinkedHashSet<>(collapsingList.stream().filter(cb -> cb.first != null && !cb.block.equals(cb.first)).map(cb -> cb.block).collect(Collectors.toSet()));
			blocks.addAll(set);
			set.clear();
			set.addAll(blocks);
		}
		super.regenerateBlocks(reason);
	}
	private class CollapsingBlock {
		
		private Block block;
		private Block first;
		private double distance;
		private int depth;
		
		public CollapsingBlock(Block block, double distance, int depth) {
			this.block = block;
			this.distance = distance;
			this.depth = depth;
		}
		public void fall() {
			Block to = block.getRelative(BlockFace.DOWN);
			if (to == null || to.getY() < -64) {
				depth = 0;
				return;
			}
			if (to.getY() > lavaDepth) {
				if (moveBlock(block, to))
					modifiedOrder.add(to);
				else {
					depth = 0;
					return;
				}
			} else {
				if (placeBlock(block, Material.LAVA))
					modifiedOrder.add(to);
				else {
					depth = 0;
					return;
				}
			}
			if (first == null) {
				first = to;
				firstBlockIndex.put(first, modifiedOrder.size() - 1);
			}
			block = to;
			--depth;
		}
	}
	public Block getHighestExposedBlock(Block start, int maxDistance, Predicate<Block> filter) {
		if (start == null)
			return null;
		Block b = start;
		if (!filter.test(b)) {
			for (int i=0; i < maxDistance; i++) {
				b = b.getRelative(BlockFace.DOWN);
				if (filter.test(b))
					return b;
			}
			return null;
		} else
			for (int i=0; i < maxDistance; i++) {
				b = b.getRelative(BlockFace.UP);
				if (b == null)
					return start.getRelative(BlockFace.UP, i);
				if (!filter.test(b))
					return b.getRelative(BlockFace.DOWN);
			}
		return b;
	}
	public Set<Block> findAttachedBlocks(Block startBlock, int maxStepsAway, Set<Block> selected) {
	    Set<Block> visited = new HashSet<>();
	    Queue<Pair<Block, Integer>> queue = new ArrayDeque<>();
	    visited.add(startBlock);
	    queue.add(Pair.of(startBlock, 0));
	    BlockFace[] faces = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN };
	    while (!queue.isEmpty()) {
	        Pair<Block, Integer> pair = queue.poll();
	        int distance = pair.getSecond();
	        if (distance >= maxStepsAway)
	        	continue;
	        Block current = pair.getFirst();
	        for (BlockFace face : faces) {
	        	if (face == BlockFace.DOWN && current.getY() <= location.getY())
	            	continue;
	            Block adjacent = current.getRelative(face);
	            if (adjacent != null && (!adjacent.isPassable() || adjacent.isLiquid()) && !visited.contains(adjacent) && !selected.contains(adjacent)) {
	                visited.add(adjacent);
	                queue.add(Pair.of(adjacent, distance + 1));
	            }
	        }
	    }
	    return visited;
	}
	public Function<PlayerDeathEvent, Boolean> getDeathCheck() {
		return event -> {
			if (event.getEntity().getLastDamageCause() == null)
				return false;
			DamageCause cause = event.getEntity().getLastDamageCause().getCause();
			if (cause != DamageCause.FALL && cause != DamageCause.LAVA)
				return false;
			Location loc = event.getEntity().getLocation();
			if (loc.getY() > location.getY() - 1
					|| !Utils.isLocationsWithinDistance(new Location(loc.getWorld(), loc.getX(), location.getY(), loc.getZ()), location, disasterRange * disasterRange))
				return false;
			return true;
		};
	}
	protected String getConfigPath() {
		return "disasters.destructive.sinkhole";
	}
	public String getDisplayName() {
		return Utils.convertString(DataUtils.getLanguageString(getConfigPath()));
	}
	public double getRegenTickRate() {
		return level;
	}
	public Set<Environment> getBannedEnvironments() {
		return Set.of(Environment.NETHER, Environment.THE_END);
	}
}