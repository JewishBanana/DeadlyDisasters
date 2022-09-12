package deadlydisasters.disasters;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import deadlydisasters.disasters.events.DestructionDisaster;
import deadlydisasters.disasters.events.DestructionDisasterEvent;
import deadlydisasters.disasters.events.DisasterEvent;
import deadlydisasters.general.DifficultyLevel;
import deadlydisasters.general.WorldObject;
import deadlydisasters.listeners.CoreListener;
import deadlydisasters.listeners.DeathMessages;
import deadlydisasters.utils.Metrics;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public class CaveIn extends DestructionDisaster {
	
	private Queue<CaveInBlock> placements = new ArrayDeque<>();
	public Iterator<CaveInBlock> iterator;
	
	private int radius,depth,maxOffset,maxBlocks;
	private BlockData[] materials;
	private double fallSpeed = -0.5;
	private double size,damage;
	public int blocksDestroyed;
	
	public Queue<UUID> fallingRoof = new ArrayDeque<>();
	
	public Map<UUID,Block> regenPreState = new HashMap<>();
	public Map<Block,Block> regenStates = new HashMap<>();
	public Map<Block,Material[]> fallenBlocks = new LinkedHashMap<>();
	
	public CaveIn(int level) {
		super(level);
		switch (level) {
		default:
		case 1:
			radius = 9;
			depth = 1;
			maxOffset = 6;
			break;
		case 2:
			radius = 13;
			depth = 2;
			maxOffset = 8;
			break;
		case 3:
			radius = 18;
			depth = 3;
			maxOffset = 10;
			break;
		case 4:
			radius = 23;
			depth = 4;
			maxOffset = 12;
			break;
		case 5:
			radius = 30;
			depth = 5;
			maxOffset = 15;
			break;
		case 6:
			radius = 50;
			depth = 6;
			maxOffset = 25;
			break;
		}
		size = plugin.getConfig().getDouble("cavein.size");
		radius *= size;
		damage = plugin.getConfig().getDouble("cavein.damage");
		maxBlocks = plugin.getConfig().getInt("cavein.max_falling_blocks");
		volume = plugin.getConfig().getDouble("cavein.volume");
		
		this.type = Disaster.CAVEIN;
	}
	public void start(Location loc, Player p) {
		DestructionDisasterEvent event = new DestructionDisasterEvent(this, loc, level, p);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		ongoingDisasters.add(this);
		this.loc = loc;
		DeathMessages.caveins.add(this);
		CaveIn me = this;
		Random rand = plugin.random;
		int[] current = {1, 0};
		BlockVector block = new BlockVector(loc.getX(), loc.getY(), loc.getZ());
		World world = loc.getWorld();
		new RepeatingTask(plugin, 0, 5) {
			@Override
			public void run() {
				if (current[0] <= radius) {
					for (int x = -current[0]; x < current[0]; x++)
						for (int z = -current[0]; z < current[0]; z++) {
							Vector position = block.clone().add(new Vector(x, 0, z));
							Block b = world.getBlockAt(position.toLocation(world));
							if (block.distance(position) >= (current[0] - 1) && block.distance(position) <= current[0]) {
								if (b.getType().isSolid())
									for (int i=0; i < maxOffset; i++) {
										b = b.getRelative(BlockFace.DOWN);
										if (!b.getType().isSolid()) {
											placements.add(new CaveInBlock(b.getRelative(BlockFace.UP).getLocation(), depth, me));
											break;
										}
									}
								else
									for (int i=0; i < maxOffset; i++) {
										b = b.getRelative(BlockFace.UP);
										if (b.getType().isSolid()) {
											placements.add(new CaveInBlock(b.getLocation(), depth, me));
											break;
										}
									}
							}
						}
					current[0]++;
					for (Entity e : loc.getWorld().getNearbyEntities(loc.clone().subtract(0,10,0), current[0]+10, 25, current[0]+10))
						if (e instanceof Player)
							((Player) e).playSound(e.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, (float) ((0.075)*volume), (float) (rand.nextDouble()+0.5));
				}
				Iterator<UUID> roofIt = fallingRoof.iterator();
				while (roofIt.hasNext()) {
					FallingBlock fb = (FallingBlock) Bukkit.getEntity(roofIt.next());
					if (fb == null || fb.isDead()) {
						roofIt.remove();
						continue;
					}
					for (Entity e : world.getNearbyEntities(fb.getLocation().add(.5,.5,.5), .5, .5, .5))
						if (e instanceof LivingEntity && !(e instanceof Player && Utils.isPlayerImmune((Player) e))) {
							LivingEntity entity = (LivingEntity) e;
							entity.damage(0.01);
							e.setLastDamageCause(new EntityDamageEvent(e, DamageCause.CUSTOM, damage));
							if (entity.getHealth()-damage <= 0)
								e.setMetadata("dd-caveincrush", plugin.fixedData);
							entity.setHealth(Math.max(entity.getHealth()-damage, 0));
						}
				}
				iterator = placements.iterator();
				while (iterator.hasNext()) {
					if (fallingRoof.size() >= maxBlocks)
						break;
					BlockData mat = null;
					if (materials != null)
						mat = materials[rand.nextInt(materials.length)];
					iterator.next().fall(mat, fallSpeed);
				}
				if (placements.isEmpty()) {
					current[1] += 5;
					if (current[1] >= 200) {
						for (Map.Entry<UUID, Block> entry : regenPreState.entrySet())
							if (Bukkit.getEntity(entry.getKey()) != null)
								Bukkit.getEntity(entry.getKey()).remove();
						regenPreState.clear();
					}
					if (!regenPreState.isEmpty()) {
						Iterator<Entry<UUID, Block>> it = regenPreState.entrySet().iterator();
						while (it.hasNext()) {
							FallingBlock fb = (FallingBlock) Bukkit.getEntity(it.next().getKey());
							if (fb == null || fb.isDead())
								it.remove();
						}
						return;
					}
					plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
						@Override
						public void run() {
							DeathMessages.caveins.remove(me);
						}
					}, 200L);
					cancel();
					triggerRegen();
					ongoingDisasters.remove(me);
					Metrics.incrementValue(Metrics.disasterDestroyedMap, type.getMetricsLabel(), blocksDestroyed);
				}
			}
		};
		for (Entity e : loc.getWorld().getNearbyEntities(loc.clone().subtract(0,10,0), radius+15, 20, radius+15))
			if (e instanceof Player)
				((Player) e).playSound(e.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, (float) ((0.33*level)*volume), (float) 0.5);
	}
	public Location findApplicableLocation(Location temp, Player p) {
		temp = Utils.getBlockAbove(temp).getLocation();
		if (temp.getBlock().getType() == Material.AIR || (temp.getBlockY() > type.getMinHeight() && temp.getWorld().getEnvironment() != Environment.NETHER))
			return null;
		return temp;
	}
	public void startAdjustment(Location loc, Player p) {
		start(Utils.getBlockAbove(loc).getLocation(), p);
	}
	public void triggerRegen() {
		if (WorldObject.findWorldObject(loc.getWorld()).difficulty != DifficultyLevel.CUSTOM || (int) WorldObject.findWorldObject(loc.getWorld()).settings.get("regenDelay") < 0)
			return;
		DisasterEvent instance = this;
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				reverseList();
				List<Block> blockList = new ArrayList<>(fallenBlocks.keySet());
				Collections.reverse(blockList);
				Map<Block,Material[]> blocks = new LinkedHashMap<>();
				for (Block b : blockList)
					blocks.put(b, fallenBlocks.get(b));
				damagedBlocks.putAll(blocks);
				final double regenRate = type.getRegenTickRate();
				double[] regenTicks = {0};
				RepeatingTask task = new RepeatingTask(plugin, (int) WorldObject.findWorldObject(loc.getWorld()).settings.get("regenDelay") * 20, 1) {
					@Override
					public void run() {
						regenTicks[0] += regenRate;
						while (regenTicks[0] >= 1) {
							regenTicks[0] -= 1;
							if (damagedBlocks.isEmpty()) {
								cancel();
								regeneratingTasks.remove(this);
								return;
							}
							Entry<Block, Material[]> entry = damagedBlocks.entrySet().iterator().next();
							Block b = entry.getKey();
							Material material = entry.getValue()[1];
							if (b.getType() == material || ((b.getType() == Material.WATER || b.getType() == Material.LAVA) && ((Levelled) b.getBlockData()).getLevel() > 0)
									|| b.getType() == Material.SNOW || material == Material.FIRE) {
								if (regenStates.containsKey(b)) {
									if (regenStates.get(b).getType() == entry.getValue()[0]) {
										b.setType(entry.getValue()[0]);
										b.setBlockData(blocksData.get(b));
										blocksData.remove(b);
										if (!b.isPassable())
											for (Entity e : b.getWorld().getNearbyEntities(b.getLocation().clone().add(0.5, 0.5, 0.5), .5, .5, .5))
												if (e.getVelocity().getY() < 0.4)
													e.setVelocity(e.getVelocity().setY(0.4));
										b = regenStates.get(b);
										if (damagedBlocks.containsKey(b)) {
											b.setType(damagedBlocks.get(b)[0]);
											b.setBlockData(blocksData.get(b));
											blocksData.remove(b);
											if (!b.isPassable())
												for (Entity e : b.getWorld().getNearbyEntities(b.getLocation().clone().add(0.5, 0.5, 0.5), .5, .5, .5))
													if (e.getVelocity().getY() < 0.4)
														e.setVelocity(e.getVelocity().setY(0.4));
											damagedBlocks.remove(b);
										}
									}
									damagedBlocks.remove(entry.getKey());
									continue;
								}
								b.setType(entry.getValue()[0]);
								b.setBlockData(blocksData.get(b));
								blocksData.remove(b);
								if (!b.isPassable())
									for (Entity e : b.getWorld().getNearbyEntities(b.getLocation().clone().add(0.5, 0.5, 0.5), .5, .5, .5))
										if (e.getVelocity().getY() < 0.4)
											e.setVelocity(e.getVelocity().setY(0.4));
							}
							damagedBlocks.remove(entry.getKey());
						}
					}
				};
				Map<DisasterEvent, Map<Block,Material[]>> tempMap = new HashMap<>();
				tempMap.put(instance, damagedBlocks);
				regeneratingTasks.put(task, tempMap);
			}
		});
	}
	public int getX() {
		return loc.getBlockX();
	}
	public int getZ() {
		return loc.getBlockZ();
	}
	public int getRadius() {
		return radius;
	}
	public void setRadius(int radius) {
		this.radius = radius;
	}
	public int getDepth() {
		return depth;
	}
	public void setDepth(int depth) {
		this.depth = depth;
	}
	public BlockData[] getMaterials() {
		return materials;
	}
	public void setMaterials(Material[] temp) {
		materials = new BlockData[temp.length];
		for (int i=0; i < temp.length; i++)
			materials[i] = temp[i].createBlockData();
	}
	public double getFallSpeed() {
		return fallSpeed;
	}
	public void setFallSpeed(double fallSpeed) {
		this.fallSpeed = fallSpeed;
	}
	public double getSize() {
		return size;
	}
	public void setSize(double size) {
		this.size = size;
	}
}
class CaveInBlock {
	private Location loc;
	private int depth;
	private CaveIn classInstance;
	private boolean CP;
	CaveInBlock(Location loc, int depth, CaveIn classInstance) {
		this.depth = depth;
		this.loc = loc;
		this.classInstance = classInstance;
		this.CP = classInstance.plugin.CProtect;
	}
	public void fall(BlockData material, double speed) {
		if (depth > 0) {
			Block b = loc.getBlock();
			if (!Utils.passStrengthTest(b.getType()) || !Utils.isZoneProtected(loc)) {
				if (material == null)
					material = b.getBlockData();
				FallingBlock fb = b.getWorld().spawnFallingBlock(loc.clone().add(0.5,0.5,0.5), material);
				if (CP)
					Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
				if (b.getState() instanceof InventoryHolder)
					CoreListener.addBlockInventory(fb, ((InventoryHolder) b.getState()).getInventory().getContents());
				else {
					classInstance.addBlockToList(b, new Material[] {b.getType(), Material.AIR});
					CoreListener.fallingBlocks.put(fb.getUniqueId(), classInstance);
					classInstance.regenPreState.put(fb.getUniqueId(), b);
				}
				b.setType(Material.AIR);
				classInstance.blocksDestroyed++;
				fb.setHurtEntities(false);
				fb.setDropItem(false);
				fb.setVelocity(new Vector(0,speed,0));
				fb.setMetadata("dd-fb", new FixedMetadataValue(classInstance.plugin, "protected"));
				classInstance.fallingRoof.add(fb.getUniqueId());
			} else
				classInstance.iterator.remove();
			depth--;
			loc.setY(loc.getY()+1);
		} else classInstance.iterator.remove();
	}
	public Location getLocation() {
		return loc;
	}
}
