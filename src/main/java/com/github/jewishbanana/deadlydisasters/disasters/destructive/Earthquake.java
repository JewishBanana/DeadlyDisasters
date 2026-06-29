package com.github.jewishbanana.deadlydisasters.disasters.destructive;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
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
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.disasters.Disaster;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class Earthquake extends Disaster {
	
	private int minHeight;
	private double size;
	private double forceMultiplier;
	private boolean placeLava;
	
	private List<CollapsingBlock> collapsingList = new ArrayList<>();
	private ArrayDeque<Block> modifiedOrder = new ArrayDeque<>();
	private Map<Block, Integer> firstBlockIndex = new HashMap<>();
	private Map<Integer, Block> reinsertOrdering = new TreeMap<>();
	
	private int width;
	
	public Earthquake(Location location, Player player, int level) {
		super(location, player, level);
		
		this.minHeight = getConfigInt("minimum_height");
	}
	public void init() {
		super.init();
		this.size = getConfigDouble("size");
		this.forceMultiplier = getConfigDouble("force_multiplier");
		this.placeLava = getConfigBoolean("place_lava");
		switch (level) {
		default:
		case 1:
			disasterRange = random.nextInt(20, 31);
			this.width = random.nextInt(3, 6);
			break;
		case 2:
			disasterRange = random.nextInt(40, 51);
			this.width = random.nextInt(7, 11);
			break;
		case 3:
			disasterRange = random.nextInt(60, 71);
			this.width = random.nextInt(10, 14);
			break;
		case 4:
			disasterRange = random.nextInt(80, 91);
			this.width = random.nextInt(14, 18);
			break;
		case 5:
			disasterRange = random.nextInt(100, 111);
			this.width = random.nextInt(19, 23);
			break;
		case 6:
			disasterRange = random.nextInt(160, 201);
			this.width = random.nextInt(30, 41);
			break;
		}
		disasterRange *= size;
		width *= size;
	}
	public boolean canStart() {
		if (getLocation().getBlockY() < minHeight)
			return false;
		if (!Utils.isAreaFlatGrounded(getLocation()))
			return false;
		return super.canStart();
	}
	public void start() {
		super.start();
		if (disasterRange < 1) {
			stop();
			return;
		}
		addDeathWatcher("deaths.earthquake");
		final Set<CollapsingBlock> set = new HashSet<>();
		final Set<Block> selected = new HashSet<>();
		final Set<PathContainer> containers = new HashSet<>();
		Vector first = Utils.getRandomizedVector().setY(0).normalize().multiply(0.5);
		containers.add(new PathContainer(first, location.clone(), disasterRange, width));
		containers.add(new PathContainer(first.clone().multiply(-1), location.clone(), disasterRange, width));
		while (true) {
			final Set<PathContainer> tremors = new HashSet<>();
			Iterator<PathContainer> iterator = containers.iterator();
			while (iterator.hasNext()) {
				PathContainer container = iterator.next();
				Vector angle = new Vector(container.vector.getZ(), 0, -container.vector.getX());
				Location offset = container.loc.clone().add(angle.clone().multiply(container.width / 2.0));
				angle.multiply(-1);
				final int falloff = (int) (container.width / 3.0);
				final int totalWidth = (int) Math.ceil(container.width);
				for (int i=0; i < container.width; i++) {
					Block block = offset.getBlock();
					if (!selected.contains(block)) {
						selected.add(block);
//						new Location(block.getWorld(), block.getX(), 120, block.getZ()).getBlock().setType(Material.ORANGE_WOOL);
						block = getHighestExposedBlock(block, 20, temp -> !temp.isPassable() || temp.isLiquid());
						if (block == null)
							continue;
						if (falloff > 0 && (i < falloff || i >= totalWidth-falloff)) {
							Block falloffBlock = block.getRelative(BlockFace.DOWN, (i < falloff ? falloff - i : falloff - (totalWidth - i - 1)) + random.nextInt(2));
							if (block != null) {
								block = falloffBlock;
								if ((i == 0 || i == container.width-1) && random.nextInt(25) < falloff)
									tremors.add(new PathContainer(i == 0 ? angle.clone().multiply(-1) : angle.clone(), offset, container.distance, container.width / 2.0));
							}
						}
						set.add(new CollapsingBlock(block, container.distance, block.getY() + 55 + random.nextInt(5)));
					}
					offset.add(angle);
				}
				container.distance -= 0.5;
				if (container.distance <= 0) {
					iterator.remove();
					continue;
				}
				container.vector.setX(container.vector.getX() + random.nextFloat(-0.15f, 0.15f)).setZ(container.vector.getZ() + random.nextFloat(-0.15f, 0.15f)).normalize().multiply(0.5);
				container.loc.add(container.vector);
				container.width = Math.max(container.width - container.widthDecrement, 1);
			}
			containers.addAll(tremors);
			if (containers.isEmpty())
				break;
		}
		if (set.isEmpty()) {
			stop();
			return;
		}
		collapsingList = set.stream()
				.sorted(Comparator
						.comparingDouble((CollapsingBlock cb) -> -cb.distance)
						.thenComparingInt(cb -> cb.block.getY()))
				.collect(Collectors.toCollection(ArrayList::new));
		
		playSoundInLargeArea(location, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.33f * level, 0.5f, disasterRange, level * 7.0, loc -> loc.subtract(0, 7, 0));
		
		scheduleTask(new BukkitRunnable() {
			private double distance = 1.0;
			private double increment = level / 20.0;
			private Iterator<CollapsingBlock> iterator = collapsingList.iterator();
			private final int iterationsPerTick = Math.max(collapsingList.size() / 5, 1);
			private int tick;
			private final World world = location.getWorld();
			
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
					if (random.nextInt(20) == 0) {
						Location blockLoc = BlockUtils.getCenterOfBlock(entry.block);
						for (Entity entity : world.getNearbyEntities(blockLoc, 10, 15, 10, temp -> temp.isValid() && !isEntityProtected(temp) && !(temp instanceof FallingBlock) && !(temp instanceof Player p && p.isFlying()))) {
							Vector vel = entity.getVelocity();
							if (entity.isOnGround() && vel.getY() < 2)
								vel.setY(vel.getY() + ((random.nextFloat(-1, 1) / 5.0) * (level / 2.0) * forceMultiplier));
							Vector towards = Utils.getVectorTowards(entity.getLocation(), blockLoc);
							vel.setX(vel.getX() + ((towards.getX() / 5.0) * (level / 3.0) * forceMultiplier));
							vel.setZ(vel.getZ() + ((towards.getZ() / 5.0) * (level / 3.0) * forceMultiplier));
							entity.setVelocity(vel);
						}
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
	}
	public void clean() {
		super.clean();
		removeDeathWatcher(300);
	}
	private class PathContainer {
		
		private final Vector vector;
		private final Location loc;
		private double distance;
		private double width;
		private double widthDecrement;
		
		public PathContainer(Vector vector, Location loc, double distance, double width) {
			this.vector = vector;
			this.loc = loc;
			this.distance = distance;
			this.width = width;
			this.widthDecrement = (width / distance) * 0.5;
		}
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
			if (!placeLava || depth > 3) {
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
	    final World world = start.getWorld();
	    final int x = start.getX();
	    final int z = start.getZ();
	    int y = start.getY();
	    if (!filter.test(start)) {
	        for (int i = 0; i < maxDistance; i++) {
	            y--;
	            final Block b = world.getBlockAt(x, y, z);
	            if (filter.test(b))
	                return b;
	        }
	        return null;
	    } else {
	        for (int i = 0; i < maxDistance; i++) {
	            y++;
	            final Block b = world.getBlockAt(x, y, z);
	            if (b == null)
	                return world.getBlockAt(x, start.getY() + i, z);
	            if (!filter.test(b))
	                return world.getBlockAt(x, y - 1, z);
	        }
	    }
	    return world.getBlockAt(x, y, z);
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
		return "disasters.destructive.earthquake";
	}
	public Set<Environment> getBannedEnvironments() {
		return EnumSet.of(Environment.NETHER, Environment.THE_END);
	}
}
