package deadlydisasters.disasters;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
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

public class Earthquake extends DestructionDisaster {

	private Queue<Place> list = new ArrayDeque<>();
	private Map<BlockVector, Vector> vectors = new HashMap<>();
	public Random rand = new Random();

	private Location mem;
	private int len,wid,radius;
	private World world;
	private double size,tilt,force;
	public int blocksDestroyed;

	public Earthquake(int level) {
		super(level);
		size = plugin.getConfig().getDouble("earthquake.size");
		tilt = plugin.getConfig().getDouble("earthquake.tilt");
		force = plugin.getConfig().getDouble("earthquake.force");
		volume = plugin.getConfig().getDouble("earthquake.volume");
		switch (level) {
		default:
		case 1:
			len = (int) (rand.nextInt(11)+10);
			wid = (int) (rand.nextInt(3)+3);
			break;
		case 2:
			len = (int) (rand.nextInt(11)+20);
			wid = (int) (rand.nextInt(4)+5);
			break;
		case 3:
			len = (int) (rand.nextInt(11)+40);
			wid = (int) (rand.nextInt(4)+8);
			break;
		case 4:
			len = (int) (rand.nextInt(21)+60);
			wid = (int) (rand.nextInt(5)+10);
			break;
		case 5:
			len = (int) (rand.nextInt(21)+80);
			wid = (int) (rand.nextInt(6)+15);
			break;
		case 6:
			len = (int) (rand.nextInt(11)+140);
			wid = (int) (rand.nextInt(6)+20);
			break;
		}
		radius = len;
		
		this.type = Disaster.EARTHQUAKE;
	}
	public void start(Location loc, Player p) {
		DestructionDisasterEvent event = new DestructionDisasterEvent(this, loc, level, p);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		double lvl = level;
		len *= size;
		wid *= size;
		this.p = p;
		this.loc = loc;
		mem = loc.clone();
		world = loc.getWorld();
		Vector angle = new Vector(rand.nextInt(360)-180, 0, rand.nextInt(360)-180).normalize();
		vectors.put(new BlockVector(loc.getX(), loc.getY(), loc.getZ()), angle.clone().multiply(-1));
		loc.subtract(angle);
		vectors.put(new BlockVector(loc.getX(), loc.getY(), loc.getZ()).add(new Vector(angle.getZ(), 0, -angle.getX()).normalize().multiply(wid)).toBlockVector(), angle.clone());
		for (Entity e : world.getNearbyEntities(loc, len/2, len/3, len/2))
			if (e instanceof Player) ((Player) e).playSound(e.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, (float) ((0.33*lvl)*volume), (float) 0.5);
		Earthquake instance = this;
		DeathMessages.earthquakes.add(this);
		new RepeatingTask(plugin, 0, plugin.getConfig().getInt("earthquake.tick_speed")) {
			@Override
			public void run() {
				if (len <= 0) {
					cancel();
					ongoingDisasters.remove(instance);
					Metrics.incrementValue(Metrics.disasterDestroyedMap, type.getMetricsLabel(), blocksDestroyed);
					plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
						@Override
						public void run() {
							DeathMessages.earthquakes.remove(instance);
						}
					}, 200L);
					return;
				}
				len--;
				int newWid = wid*2;
				Map<BlockVector, Vector> temp = new HashMap<>();
				for (Entry<BlockVector, Vector> entry : vectors.entrySet()) {
					entry.getKey().add(entry.getValue());
					BlockVector bv = entry.getKey().clone();
					Vector offset = new Vector(-entry.getValue().getZ(), 0, entry.getValue().getX()).normalize().multiply(0.5);
					for (int i = 1; i < newWid; i++) {
						Location b = bv.toLocation(world);
						if (b.getBlock().getType().isSolid())
							for (int d = 0; d < 15; d++) {
								b.setY(b.getY()+1);
								if (!b.getBlock().getType().isSolid()) {
									b.setY(b.getY()-1);
									break;
								}
							}
						else
							for (int d = 0; d < 15; d++) {
								b.setY(b.getY()-1);
								if (b.getBlock().getType().isSolid())
									break;
							}
						int depth = loc.getBlockY()-5;
						if (i < level*2 && i < newWid/3) {
							b.subtract(0,5-i,0);
							depth = (depth/newWid*i);
						} else if (i > newWid-level && i > newWid/3*2) {
							b.subtract(0,5-(newWid-i),0);
							depth = (depth/newWid*(newWid-i));
						}
						list.add(new Place(b, depth, instance));
						bv.add(offset);
					}
					if (level > 3 && rand.nextInt(level*10) == 0) {
						Vector tremor = new Vector(offset.getX()+(rand.nextDouble()/2-0.25), 0, offset.getZ()+(rand.nextDouble()/2-0.25)).normalize();
						if (rand.nextInt(2) == 0) tremor.multiply(-1);
						temp.put(entry.getKey().clone().add(offset.clone().multiply(newWid/2)).toBlockVector(), tremor);
					}
					for (Entity e : world.getNearbyEntities(bv.subtract(offset.multiply(newWid/2)).toLocation(world), level*10, level*10, level*10)) {
						if (!(e instanceof LivingEntity) || (e instanceof Player && ((Player) e).isFlying())) continue;
						double yVel = e.getVelocity().getY();
						if (!e.isOnGround() || e.getVelocity().getY() > 3)
							yVel = 0;
						else
							yVel = (rand.nextDouble()/14*lvl)*force;
						e.setVelocity(e.getVelocity().add(new Vector(((rand.nextDouble()-0.5)/5*lvl)*force, yVel, ((rand.nextDouble()-0.5)/5*lvl)*force)));
					}
					offset.multiply(tilt);
					entry.getKey().add(offset.multiply(rand.nextInt(3)-1));
				}
				vectors.putAll(temp);
				
				Iterator<Place> iterator = list.iterator();
				while (iterator.hasNext())
					iterator.next().dig(iterator);
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
		return mem.getBlockX();
	}
	public int getY() {
		return mem.getBlockY();
	}
	public int getZ() {
		return mem.getBlockZ();
	}
	public int getRadius() {
		return radius;
	}
	public void setRadius(int radius) {
		this.len = radius;
		this.radius = radius;
	}
	public int getWidth() {
		return wid;
	}
	public void setWidth(int width) {
		this.wid = width;
	}
	public double getTilt() {
		return tilt;
	}
	public void setTilt(double tilt) {
		this.tilt = tilt;
	}
	public double getSize() {
		return size;
	}
	public void setSize(double size) {
		this.size = size;
	}
	public double getForce() {
		return force;
	}
	public void setForce(double force) {
		this.force = force;
	}
}
class Place {
	private int depth;
	private Location loc;
	private boolean CP;
	private Random rand;
	private Earthquake classInstance;
	Place(Location loc, int depth, Earthquake classInstance) {
		this.loc = loc;
		this.depth = depth;
		this.classInstance = classInstance;
		rand = classInstance.rand;
		CP = classInstance.plugin.CProtect;
	}
	void dig(Iterator<Place> it) {
		Block b = loc.getBlock();
		for (int i=(Math.min(rand.nextInt(4)+1, depth)); i > 0; i--) {
			depth--;
			loc.setY(loc.getY()-1);
			b = loc.getBlock();
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
		if (depth <= 0) {
			if (loc.getBlockY() < 15) {
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