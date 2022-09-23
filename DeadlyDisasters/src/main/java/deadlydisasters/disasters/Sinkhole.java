package deadlydisasters.disasters;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import deadlydisasters.disasters.events.DestructionDisaster;
import deadlydisasters.disasters.events.DestructionDisasterEvent;
import deadlydisasters.listeners.DeathMessages;
import deadlydisasters.utils.Metrics;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public class Sinkhole extends DestructionDisaster {
	
	private Queue<Places> placements = new ArrayDeque<>();
	
	private Location memory;
	private int tick=0,speed;
	public int maxD,blocksDestroyed;
	private double radius = 0, size;
	public Random rand;
	
	public static Set<Material> treeBlocks = new HashSet<>();
	
	public Sinkhole(int level) {
		super(level);
		this.rand = plugin.random;
		this.maxD = plugin.maxDepth + 5;
		this.speed = plugin.getConfig().getInt("sinkhole.speed");
		this.size = plugin.getConfig().getDouble("sinkhole.size");
		this.volume = plugin.getConfig().getDouble("sinkhole.volume");
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
		this.loc = loc;
		radius *= size;
		final World world = loc.getWorld();
		memory = loc.clone();
		DeathMessages.sinkholes.add(this);
		Sinkhole me = this;
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				BlockVector block = new BlockVector(loc.getX(), loc.getY(), loc.getZ());
				for (int x = -tick; x < tick; x++)
					for (int z = -tick; z < tick; z++) {
						Vector position = block.clone().add(new Vector(x, 0, z));
						if (!(block.distance(position) >= (tick - 1) && block.distance(position) <= tick)) continue;
						Block bl = world.getBlockAt(position.toLocation(world));
						Location place = bl.getLocation();
						int offset = 0;
						if (bl.getType().isSolid() || bl.isLiquid()) {
							for (int c=place.getBlockY(); c < 255; c++) {
								place.setY(c);
								bl = place.getBlock();
								if (!(bl.getType().isSolid()) && !(bl.isLiquid())) {
									place.setY(c-1);
									break;
								}
								if (treeBlocks.contains(bl.getType()))
									offset++;
							}
						} else {
							for (int c=place.getBlockY(); c > maxD; c--) {
								place.setY(c);
								bl = place.getBlock();
								if (bl.getType().isSolid() || bl.isLiquid())
									break;
							}
						}
						int depth = 0;
						if (tick >= radius/4*3) depth = (int) (radius-tick+offset);
						else depth = (int) ((rand.nextInt((int) (radius))+radius-tick)*level);
						if (depth <= 0) {
							tick = (int) radius + 1;
							break;
						}
						placements.add(new Places(place, depth, true, me));
						if (treeBlocks.contains(place.getBlock().getType())) {
							for (int x2=place.getBlockX()-3; x2 < place.getBlockX()+3; x2++)
								for (int z2=place.getBlockZ()-3; z2 < place.getBlockZ()+3; z2++)
									if (!(x2 == place.getBlockX() && z2 == place.getBlockZ()) && treeBlocks.contains(place.getWorld().getHighestBlockAt(x2, z2).getType()))
										placements.add(new Places(new Location(place.getWorld(), x2, place.getWorld().getHighestBlockYAt(x2, z2), z2), depth, false, me));
						}
					}
				if (tick >= radius) {
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
							if (placements.isEmpty()) {
								plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
									@Override
									public void run() {
										DeathMessages.sinkholes.remove(me);
									}
								}, 200L);
								cancel();
								Metrics.incrementValue(Metrics.disasterDestroyedMap, type.getMetricsLabel(), blocksDestroyed);
							}
						}
					};
					cancel();
					return;
				}
				tick++;
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
	private Location loc;
	private boolean force,CP;
	private Sinkhole classInstance;
	private Random rand;
	Places(Location loc, int depth, boolean force, Sinkhole classInstance) {
		this.loc = loc;
		this.depth = depth;
		this.force = force;
		this.classInstance = classInstance;
		this.rand = classInstance.rand;
		this.CP = classInstance.plugin.CProtect;
	}
	void dig(Iterator<Places> it) {
		Block b = loc.getBlock();
		int r = rand.nextInt(3)+1;
		if (r > depth) r = depth;
		for (int i=r; i > 0; i--) {
			loc.setY(loc.getY()-1);
			b = loc.getBlock();
			if (!force && loc.getBlock().getType() == Material.AIR) {
				b = loc.clone().add(0,1,0).getBlock();
				if (!Utils.isBlockBlacklisted(b.getType()) && !Utils.isZoneProtected(b.getLocation())) {
					if (CP) Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
					b.setType(Material.AIR);
					classInstance.blocksDestroyed++;
				}
				it.remove();
				return;
			}
			if (!Utils.isBlockBlacklisted(b.getType()) && !Utils.isZoneProtected(b.getLocation())) {
				if (CP) {
					Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
					Utils.getCoreProtect().logPlacement("Deadly-Disasters", b.getLocation(), new Location(b.getWorld(), b.getX(), b.getY()+1, b.getZ()).getBlock().getType(), new Location(b.getWorld(), b.getX(), b.getY()+1, b.getZ()).getBlock().getBlockData());
					Utils.getCoreProtect().logRemoval("Deadly-Disasters", new Location(b.getWorld(), b.getX(), b.getY()+1, b.getZ()), new Location(b.getWorld(), b.getX(), b.getY()+1, b.getZ()).getBlock().getType(), new Location(b.getWorld(), b.getX(), b.getY()+1, b.getZ()).getBlock().getBlockData());
				}
				Block b2 = loc.clone().add(0, 1, 0).getBlock();
				if (!Utils.isBlockBlacklisted(b2.getType()) && !Utils.isZoneProtected(b2.getLocation())) {
					b.setType(b2.getType());
					if (b2.getState() instanceof InventoryHolder)
						((InventoryHolder) b.getState()).getInventory().setContents(((InventoryHolder) b2.getState()).getInventory().getContents());
					b2.setType(Material.AIR);
					classInstance.blocksDestroyed++;
				}
				//new Location(b.getWorld(), b.getX(), b.getY()+2, b.getZ()).getBlock().setType(Material.AIR);
			} else {
				it.remove();
				return;
			}
		}
		depth-=r;
		if (depth <= 0) {
			if (loc.getBlockY() < (classInstance.maxD + 15)) {
				b.setType(Material.LAVA);
				classInstance.blocksDestroyed++;
			}
			it.remove();
		}
	}
	public Location getLocation() {
		return loc;
	}
}