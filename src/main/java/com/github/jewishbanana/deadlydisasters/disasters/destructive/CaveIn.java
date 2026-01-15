package com.github.jewishbanana.deadlydisasters.disasters.destructive;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.disasters.Disaster;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class CaveIn extends Disaster {
	
	private int maxHeight;
	private double size;
	private int centerDepth;
	private double damage;
	private double speed;
	private int maxBlocks;
	
	private List<CollapsingBlock> collapsingList = new ArrayList<>();
	private List<FallingBlock> fallingBlocks = new ArrayList<>();
	
	public CaveIn(Location location, Player player, int level) {
		super(location, player, level);
		
		this.maxHeight = getConfigInt("maximum_height");
	}
	public void init() {
		super.init();
		this.size = getConfigDouble("size");
		this.damage = getConfigDouble("damage");
		this.speed = getConfigDouble("speed");
		this.maxBlocks = getConfigInt("max_falling_blocks");
		switch (level) {
		default:
		case 1:
			disasterRange = 14;
			break;
		case 2:
			disasterRange = 25;
			break;
		case 3:
			disasterRange = 34;
			break;
		case 4:
			disasterRange = 45;
			break;
		case 5:
			disasterRange = 60;
			break;
		case 6:
			disasterRange = 90;
			break;
		}
		disasterRange *= size;
		centerDepth = level * 2;
	}
	public Location findPossiblePosition(Location initial) {
		if (initial == null)
			return null;
		Block b = initial.getBlock();
		for (int i=0; i < 20; i++) {
			b = b.getRelative(BlockFace.UP);
			if (!b.isPassable())
				return BlockUtils.getCenterOfBlock(b);
		}
		return null;
	}
	public boolean canStart() {
		if (getLocation().getWorld().getEnvironment() != Environment.NETHER && getLocation().getBlockY() > maxHeight)
			return false;
		if (recursionCheck(getLocation().getBlock(), 0, 150, 0, 10, new HashSet<>()) != 150)
			return false;
		return super.canStart();
	}
	public int recursionCheck(Block block, int count, int targetCount, int distance, int maxDistance, Set<Block> passed) {
		if (count == targetCount || distance == maxDistance || passed.contains(block) || block.isPassable())
			return count;
		passed.add(block);
//		final BlockData data = block.getBlockData();
//		plugin.getServer().getScheduler().runTask(plugin, () -> block.setType(Material.GOLD_BLOCK));
//		plugin.getServer().getScheduler().runTaskLater(plugin, () -> block.setBlockData(data), 200);
		count++;
		for (BlockFace face : Set.of(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)) {
			Block other = block.getRelative(face);
			if (other == null)
				continue;
			count = recursionCheck(other, count, targetCount, distance + 1, maxDistance, passed);
	        if (count >= targetCount)
	            break;
		}
		return count;
	}
	public void start() {
		super.start();
//		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
//			int count = recursionCheck(getLocation().getBlock(), 0, 200, 0, 15, new HashSet<>());
//			plugin.getLogger().info("count is "+count);
//		});
		Set<Block> blocks = new HashSet<>(BlockUtils.getBlocksInCircleRadius(location, (float) disasterRange));
		if (blocks.isEmpty()) {
			stop();
			return;
		}
		scheduleTask(new BukkitRunnable() {
			private final double rangeSq = disasterRange * disasterRange;
			private final double noise = 0.6;
			
			@Override
			public void run() {
				try {
					Set<CollapsingBlock> collapsing = new HashSet<>();
					for (Block next : blocks) {
						Block block = getHighestExposedBlock(next, level * 3 + ThreadLocalRandom.current().nextInt(level), temp -> !temp.isPassable());
						if (block == null)
							continue;
						Location bc = block.getLocation();
						double extraNoise = ThreadLocalRandom.current().nextDouble(level);
						double dx = bc.getX() + 0.5 + extraNoise - location.getX();
						double dz = bc.getZ() + 0.5 + extraNoise - location.getZ();
						double distSq = dx * dx + dz * dz;
						double t = Math.min(distSq / rangeSq, 1.0);
						double falloff = Math.pow(1.0 - t, 2.0);
						double edge = 1.0 - falloff;
						double jitter = -ThreadLocalRandom.current().nextDouble() * noise * edge;
						int depth = (int) Math.max(0, Math.round(centerDepth * falloff + jitter));
						if (depth > 0)
							collapsing.add(new CollapsingBlock(block, distSq, depth));
					}
					collapsingList = collapsing.stream()
						    .sorted(Comparator
						        .comparingDouble((CollapsingBlock cb) -> cb.distance))   // sort by Y first
						    .collect(Collectors.toCollection(ArrayList::new));
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
							playSound(player, loc.add(Utils.getVectorTowards(loc, location).multiply(7.0)), Sound.ENTITY_WITHER_BREAK_BLOCK, (0.0125 * (level * 2)) * (1.0 - ((1.0 / excessSoundRange) * (loc.distance(location) - disasterRange))), 0.5);
						else
							playSound(player, loc.subtract(0, 7, 0), Sound.ENTITY_WITHER_BREAK_BLOCK, (0.0125 * (level * 2)), 0.5);
					});
					scheduleTask(new BukkitRunnable() {
						private double distance = 1.0;
						private double distanceSquared = 1.0;
						private double increment = level / 20.0 * speed;
						private Iterator<CollapsingBlock> iterator = collapsingList.iterator();
						private final int iterationsPerTick = Math.max(collapsingList.size() / 5, 1);
						private int tick;
						private int damageInterval;
						private boolean doneCollapsing;
						private int wrapUpTicks = 100;
						
						@Override
						public void run() {
							Iterator<FallingBlock> it = fallingBlocks.iterator();
							while (it.hasNext()) {
								FallingBlock fb = it.next();
								if (fb == null || !fb.isValid()) {
									it.remove();
									continue;
								}
								if (damageInterval == 10)
									for (Entity e : fb.getNearbyEntities(.5, .5, .5))
										if (e instanceof LivingEntity alive && !isEntityProtected(e) && !EntityUtils.isEntityImmunePlayer(e))
											EntityUtils.damageEntity(alive, damage, "deaths.cavein", DamageCause.FALLING_BLOCK);
							}
							if (++damageInterval == 11)
								damageInterval = 0;
							if (doneCollapsing) {
								if (fallingBlocks.isEmpty())
									stop();
								if (wrapUpTicks-- == 0) {
									fallingBlocks.stream().forEach(e -> {
										if (e != null)
											e.remove();
									});
									stop();
								}
								return;
							}
							while (++tick <= iterationsPerTick && iterator.hasNext()) {
								if (fallingBlocks.size() == maxBlocks)
									break;
								CollapsingBlock entry = iterator.next();
								if (entry.distance > distanceSquared)
									continue;
								FallingBlock fb = entry.fall();
								if (fb != null)
									fallingBlocks.add(fb);
								if (entry.depth == 0)
									iterator.remove();
							}
							if (!iterator.hasNext()) {
								if (collapsingList.isEmpty()) {
									doneCollapsing = true;
									return;
								}
								iterator = collapsingList.iterator();
							}
							tick = 0;
							distance += increment;
							distanceSquared = distance * distance;
						}
					}.runTaskTimer(plugin, 0, 1));
					
				} catch (Exception e) {
					Utils.sendExceptionLog(e);
				}
			}
		}.runTaskAsynchronously(plugin));
	}
	private class CollapsingBlock {
		
		private Block block;
		private double distance;
		private int depth;
		
		public CollapsingBlock(Block block, double distance, int depth) {
			this.block = block;
			this.distance = distance;
			this.depth = depth;
		}
		public FallingBlock fall() {
			if (block == null) {
				depth = 0;
				return null;
			}
			FallingBlock fb = null;
			if (block.getType() != Material.AIR) {
				fb = convertBlockIntoFallingBlock(block);
				if (fb != null) {
					fb.setVelocity(new Vector(0, -.3, 0));
					fb.setDropItem(false);
					fb.setHurtEntities(false);
				} else {
					depth = 0;
					return null;
				}
			}
			--depth;
			block = block.getRelative(BlockFace.UP);
			return fb;
		}
	}
	public Block getHighestExposedBlock(Block start, int maxDistance, Predicate<Block> filter) {
	    if (start == null)
	        return null;
	    final World world = start.getWorld();
	    final int x = start.getX();
	    final int z = start.getZ();
	    int y = start.getY();
	    if (filter.test(start)) {
	        for (int i = 0; i < maxDistance; i++) {
	            y--;
	            final Block b = world.getBlockAt(x, y, z);
	            if (!filter.test(b))
	                return world.getBlockAt(x, y + 1, z);
	        }
	        return null;
	    } else {
	        for (int i = 0; i < maxDistance; i++) {
	            y++;
	            final Block b = world.getBlockAt(x, y, z);
	            if (b == null)
	                return world.getBlockAt(x, start.getY() + i, z);
	            if (filter.test(b))
	                return b;
	        }
	    }
	    return world.getBlockAt(x, y, z);
	}
	protected String getConfigPath() {
		return "disasters.destructive.cavein";
	}
	public String getDisplayName() {
		return Utils.convertString(DataUtils.getLanguageString(getConfigPath()));
	}
	public Set<Environment> getBannedEnvironments() {
		return EnumSet.of(Environment.THE_END);
	}
}