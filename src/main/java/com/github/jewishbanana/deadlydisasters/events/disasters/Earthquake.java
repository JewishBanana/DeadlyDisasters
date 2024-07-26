package com.github.jewishbanana.deadlydisasters.events.disasters;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.events.DestructionDisaster;
import com.github.jewishbanana.deadlydisasters.events.DestructionDisasterEvent;
import com.github.jewishbanana.deadlydisasters.events.Disaster;
import com.github.jewishbanana.deadlydisasters.handlers.WorldObject;
import com.github.jewishbanana.deadlydisasters.listeners.DeathMessages;
import com.github.jewishbanana.deadlydisasters.utils.Metrics;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class Earthquake extends DestructionDisaster {

	private Set<Place> list = ConcurrentHashMap.newKeySet();
	private Map<BlockVector, Vector> vectors = new HashMap<>();
	public Random rand = new Random();
	
	private Map<Chunk, Set<Place>> masteryBlocks = new ConcurrentHashMap<>();

	private Location mem;
	private int len,wid,radius;
	private World world;
	private double size,tilt,force;
	public int blocksDestroyed;
	public boolean placeLava;

	public Earthquake(int level) {
		super(level);
		size = configFile.getDouble("earthquake.size");
		tilt = configFile.getDouble("earthquake.tilt");
		force = configFile.getDouble("earthquake.force");
		volume = configFile.getDouble("earthquake.volume");
		this.placeLava = configFile.getBoolean("earthquake.place_lava");
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
		ongoingDisasters.add(this);
		double lvl = level;
		len *= size;
		wid *= size;
		radius = len;
		this.p = p;
		this.loc = loc;
		mem = loc.clone();
		world = loc.getWorld();
		Vector angle = new Vector(rand.nextInt(360)-180, 0, rand.nextInt(360)-180).normalize();
		vectors.put(new BlockVector(loc.getX(), loc.getY(), loc.getZ()), angle.clone().multiply(-1));
		loc.subtract(angle);
		vectors.put(new BlockVector(loc.getX(), loc.getY(), loc.getZ()).add(new Vector(angle.getZ(), 0, -angle.getX()).normalize().multiply(wid)).toBlockVector(), angle.clone());
		Earthquake instance = this;
		boolean pushPlayersInRegions = WorldObject.findWorldObject(world).protectRegions;
		DeathMessages.earthquakes.add(this);
		int[] delay = {0, 0};
		Map<Integer, Map<BlockVector, Vector>> pullVectorMap = new HashMap<>();
		final int trueLength = len;
		boolean[] isDead = {false};
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				for (int c=0; c < level; c++) {
					if (len <= 0) {
						for (Entity e : world.getNearbyEntities(loc, trueLength/2, trueLength/3, trueLength/2))
							if (e instanceof Player) ((Player) e).playSound(e.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, (float) ((0.33*lvl)*volume), (float) 0.5);
						new RepeatingTask(plugin, 0, configFile.getInt("earthquake.tick_speed")) {
							@Override
							public void run() {
								Iterator<Place> iterator = list.iterator();
								while (iterator.hasNext())
									iterator.next().dig(iterator);
								for (Map.Entry<BlockVector, Vector> entry : pullVectorMap.get(delay[1]).entrySet()) {
									for (Entity e : world.getNearbyEntities(entry.getKey().subtract(entry.getValue()).toLocation(world), level*10, level*10, level*10)) {
										if (isEntityTypeProtected(e) || (pushPlayersInRegions && Utils.isZoneProtected(e.getLocation())) || (e instanceof Player && ((Player) e).isFlying()))
											continue;
										double yVel = e.getVelocity().getY();
										if (!e.isOnGround() || e.getVelocity().getY() > 3)
											yVel = 0;
										else
											yVel = (rand.nextDouble()/14*lvl)*force;
										Vector vec = new Vector(((rand.nextDouble()-0.5)/5*lvl)*force, yVel, ((rand.nextDouble()-0.5)/5*lvl)*force);
										e.setVelocity(e.getVelocity().add(vec));
									}
								}
								delay[1]++;
								if (delay[1] >= delay[0]) {
									isDead[0] = true;
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
							}
						};
						cancel();
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
							Place place = new Place(b, depth, instance, delay[0]);
							list.add(place);
							if (masteryBlocks.containsKey(b.getChunk()))
								masteryBlocks.get(b.getChunk()).add(place);
							else {
								Set<Place> tempSet = ConcurrentHashMap.newKeySet();
								tempSet.add(place);
								masteryBlocks.put(b.getChunk(), tempSet);
							}
							bv.add(offset);
						}
						if (level > 3 && rand.nextInt(level*10) == 0) {
							Vector tremor = new Vector(offset.getX()+(rand.nextDouble()/2-0.25), 0, offset.getZ()+(rand.nextDouble()/2-0.25)).normalize();
							if (rand.nextInt(2) == 0) tremor.multiply(-1);
							temp.put(entry.getKey().clone().add(offset.clone().multiply(newWid/2)).toBlockVector(), tremor);
						}
						offset.multiply(newWid/2);
						if (pullVectorMap.containsKey(delay[0]))
							pullVectorMap.get(delay[0]).put(bv.clone(), offset.clone());
						else {
							Map<BlockVector, Vector> tempMap = new HashMap<>();
							tempMap.put(bv.clone(), offset.clone().multiply(newWid/2));
							pullVectorMap.put(delay[0], tempMap);
						}
						offset.multiply(tilt);
						entry.getKey().add(offset.multiply(rand.nextInt(3)-1));
					}
					vectors.putAll(temp);
					
					delay[0]++;
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
	class Place {
		private int depth, delay;
		private Location loc;
		private boolean CP;
		private Random rand;
		private Earthquake classInstance;
		Place(Location loc, int depth, Earthquake classInstance, int delay) {
			this.loc = loc;
			this.delay = delay;
			this.depth = depth;
			this.classInstance = classInstance;
			rand = classInstance.rand;
			CP = classInstance.plugin.CProtect;
		}
		void dig(Iterator<Place> it) {
			if (delay > 0) {
				delay--;
				return;
			}
			Block b = loc.getBlock();
			for (int i=(Math.min(rand.nextInt(4)+1, depth)); i > 0; i--) {
				loc.setY(loc.getY()-1);
				b = loc.getBlock();
				if (!Utils.passStrengthTest(b.getType()) && !Utils.isZoneProtected(b.getLocation())) {
					if (CP) {
						Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
						Utils.getCoreProtect().logPlacement("Deadly-Disasters", b.getLocation(), new Location(b.getWorld(), b.getX(), b.getY()+1, b.getZ()).getBlock().getType(), new Location(b.getWorld(), b.getX(), b.getY()+1, b.getZ()).getBlock().getBlockData());
						Utils.getCoreProtect().logRemoval("Deadly-Disasters", new Location(b.getWorld(), b.getX(), b.getY()+1, b.getZ()), new Location(b.getWorld(), b.getX(), b.getY()+1, b.getZ()).getBlock().getType(), new Location(b.getWorld(), b.getX(), b.getY()+1, b.getZ()).getBlock().getBlockData());
					}
					Block b2 = loc.getBlock().getRelative(BlockFace.UP);
					if (!Utils.passStrengthTest(b2.getType()) && !Utils.isZoneProtected(b2.getLocation())) {
						BlockState state = b.getState();
						b.setBlockData(b2.getBlockData());
						if (state instanceof TileState) {
							if (b2.getState() instanceof InventoryHolder)
								((InventoryHolder) state).getInventory().setContents(((InventoryHolder) b2.getState()).getInventory().getContents());
						}
						b2.setType(Material.AIR);
						classInstance.blocksDestroyed++;
					}
				} else {
					it.remove();
					classInstance.masteryBlocks.get(loc.getChunk()).remove(this);
					return;
				}
				depth--;
			}
			if (depth <= 0) {
				if (classInstance.placeLava && b.getWorld().getEnvironment() != Environment.THE_END && loc.getBlockY() < 15) {
					b.setType(Material.LAVA);
					classInstance.blocksDestroyed++;
				}
				classInstance.masteryBlocks.get(loc.getChunk()).remove(this);
				it.remove();
			}
		}
		public Location getLocation() {
			return loc;
		}
	}
}