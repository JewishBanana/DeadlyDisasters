package com.github.jewishbanana.deadlydisasters.events.disasters;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.events.DestructionDisaster;
import com.github.jewishbanana.deadlydisasters.events.DestructionDisasterEvent;
import com.github.jewishbanana.deadlydisasters.events.Disaster;
import com.github.jewishbanana.deadlydisasters.listeners.DeathMessages;
import com.github.jewishbanana.deadlydisasters.utils.Metrics;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class Sinkhole extends DestructionDisaster {
	
	private Queue<Places> placements = new ArrayDeque<>();
	
	private Location memory;
	private int speed;
	public int maxD,blocksDestroyed;
	private double radius = 0, size;
	public Random rand;
	public boolean placeLava;
	
	public Map<Block,Integer> liquidBlocks = new HashMap<>();
	
	public static Set<Material> treeBlocks = new HashSet<>();
	
	public Sinkhole(int level, World world) {
		super(level, world);
		this.rand = plugin.random;
		this.maxD = plugin.maxDepth + 5;
		this.speed = configFile.getInt("sinkhole.speed");
		this.size = configFile.getDouble("sinkhole.size");
		this.volume = configFile.getDouble("sinkhole.volume");
		this.placeLava = configFile.getBoolean("sinkhole.place_lava");
		switch (level) {
		default:
		case 1:
			radius = 4;
			break;
		case 2:
			radius = 6;
			break;
		case 3:
			radius = 10;
			break;
		case 4:
			radius = 15;
			break;
		case 5:
			radius = 20;
			break;
		case 6:
			radius = 50;
			break;
		}
		this.type = Disaster.SINKHOLE;
	}
	public void start(final Location loc, Player p) {
		DestructionDisasterEvent event = new DestructionDisasterEvent(this, loc, level, p);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		ongoingDisasters.add(this);
		this.loc = loc;
		radius *= size;
		final World world = loc.getWorld();
		memory = loc.clone();
		DeathMessages.sinkholes.add(this);
		Sinkhole me = this;
		
		Queue<Block> spots = new ArrayDeque<>();
		BlockVector block = new BlockVector(loc.getX(), loc.getY(), loc.getZ());
		for (int x = (int) -radius; x < radius; x++)
			for (int z = (int) -radius; z < radius; z++) {
				Vector position = block.clone().add(new Vector(x, 0, z));
				if (block.distance(position) <= radius)
					spots.add(world.getBlockAt(position.toLocation(world)));
			}
		final int current = Math.min(spots.size()/10, 150);
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				Iterator<Block> it = spots.iterator();
				for (int i=0; i < current; i++) {
					if (!it.hasNext())
						break;
					Block bl = it.next();
					int offset = 0;
					if (!bl.isPassable()) {
						for (int c=0; c < rand.nextInt(15)+5; c++) {
							bl = bl.getRelative(BlockFace.UP);
							if (bl.isPassable()) {
								bl = bl.getRelative(BlockFace.DOWN);
								break;
							}
							if (treeBlocks.contains(bl.getType()))
								offset++;
						}
					} else {
						for (int c=bl.getLocation().getBlockY(); c > maxD; c--) {
							bl = bl.getRelative(BlockFace.DOWN);
							if (!bl.isPassable())
								break;
						}
					}
					Location place = bl.getLocation();
					int depth = 0;
					double dist = place.add(.5,.5,.5).distance(loc);
					if (dist >= radius/4*3)
						depth = (int) (radius-dist+offset);
					else
						depth = (int) ((rand.nextInt((int) (radius))+radius-dist)*level);
					placements.add(new Places(bl, depth, me));
					if (treeBlocks.contains(bl.getType())) {
						for (int x2=place.getBlockX()-3; x2 < place.getBlockX()+3; x2++)
							for (int z2=place.getBlockZ()-3; z2 < place.getBlockZ()+3; z2++)
								if (!(x2 == place.getBlockX() && z2 == place.getBlockZ()) && treeBlocks.contains(place.getWorld().getHighestBlockAt(x2, z2).getType()))
									placements.add(new Places(new Location(place.getWorld(), x2, place.getWorld().getHighestBlockYAt(x2, z2), z2).getBlock(), depth, me));
					}
					it.remove();
				}
				if (!it.hasNext()) {
					placements = new ArrayDeque<>(placements.stream().sorted(Comparator.comparingDouble(e -> e.getBlock().getLocation().distanceSquared(loc))).collect(Collectors.toList()));
					for (Entity all : memory.getWorld().getNearbyEntities(memory, radius+20, 128, radius+20)) {
						if (all instanceof Player && all.getLocation().getBlockY() <= memory.getBlockY()+40) {
							((Player) all).playSound(all.getLocation(), Sound.ITEM_TOTEM_USE, (float) ((0.33*level)*volume), (float) 0.5);
						}
					}
					new RepeatingTask(plugin, 0, speed) {
						@Override
						public void run() {
							Iterator<Places> iterator = placements.iterator();
							while (iterator.hasNext())
								iterator.next().dig(iterator);
							Iterator<Entry<Block, Integer>> it = liquidBlocks.entrySet().iterator();
							while (it.hasNext()) {
								Entry<Block, Integer> entry = it.next();
								if (entry.getKey().isLiquid())
									entry.getKey().setType(Material.AIR);
								entry.setValue(entry.getValue()-1);
								if (entry.getValue() <= 0)
									it.remove();
							}
							if (placements.isEmpty()) {
								plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
									@Override
									public void run() {
										DeathMessages.sinkholes.remove(me);
									}
								}, 200L);
								cancel();
								ongoingDisasters.remove(me);
								Metrics.incrementValue(Metrics.disasterDestroyedMap, type.getMetricsLabel(), blocksDestroyed);
							}
						}
					};
					cancel();
					return;
				}
			}
		};
	}
	public Location findApplicableLocation(Location temp, Player p) {
		temp = Utils.getBlockBelow(temp).getLocation();
		if (temp.getBlockY() < type.getMinHeight())
			return null;
		return temp;
	}
	public void startAdjustment(Location loc, Player p) {
		start(Utils.getBlockBelow(loc).getLocation(), p);
	}
	public int getX() {
		return memory.getBlockX();
	}
	public int getY() {
		return memory.getBlockY();
	}
	public int getZ() {
		return memory.getBlockZ();
	}
	public double getRadius() {
		return radius;
	}
	public void setRadius(double radius) {
		this.radius = radius;
	}
	public void setSpeed(int speed) {
		this.speed = speed;
	}
	public double getSize() {
		return size;
	}
	public void setSize(double size) {
		this.size = size;
	}
}

class Places {
	private int depth;
	private Block b, memory;
	private boolean CP;
	private Sinkhole classInstance;
	private Random rand;
	Places(Block b, int depth, Sinkhole classInstance) {
		this.b = b;
		this.depth = depth;
		this.classInstance = classInstance;
		this.rand = classInstance.rand;
		this.CP = classInstance.plugin.CProtect;
		this.memory = b;
	}
	public void dig(Iterator<Places> it) {
		for (int i=rand.nextInt(3)+1; i > 0; i--) {
			if (!Utils.passStrengthTest(b.getType()) && !Utils.isZoneProtected(b.getLocation())) {
				Block b2 = b.getRelative(BlockFace.DOWN);
				if (!Utils.passStrengthTest(b2.getType()) && !Utils.isZoneProtected(b2.getLocation())) {
					if (CP) {
						Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
						Utils.getCoreProtect().logRemoval("Deadly-Disasters", b2.getLocation(), b2.getType(), b2.getBlockData());
					}
					BlockState state = b.getState();
					b2.setBlockData(b.getBlockData());
					if (state instanceof InventoryHolder)
						((InventoryHolder) b2.getState()).getInventory().setContents(((InventoryHolder) state).getInventory().getContents());
					if (CP)
						Utils.getCoreProtect().logPlacement("Deadly-Disasters", b2.getLocation(), b2.getType(), b2.getBlockData());
					if (b.isLiquid())
						classInstance.liquidBlocks.put(b, 5);
					b.setType(Material.AIR);
					classInstance.blocksDestroyed++;
				} else {
					it.remove();
					return;
				}
			} else {
				if (!b.equals(memory))
				it.remove();
				return;
			}
			depth--;
			b = b.getRelative(BlockFace.DOWN);
			if (depth <= 0) {
				if (classInstance.placeLava && b.getWorld().getEnvironment() != Environment.THE_END && b.getLocation().getBlockY() < (classInstance.maxD + 15)) {
					b.setType(Material.LAVA);
					classInstance.blocksDestroyed++;
				}
				it.remove();
				return;
			}
		}
	}
	public Location getLocation() {
		return b.getLocation();
	}
	public Block getBlock() {
		return b;
	}
}