package deadlydisasters.disasters.events;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.Future;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import deadlydisasters.disasters.Disaster;
import deadlydisasters.general.DifficultyLevel;
import deadlydisasters.general.Main;
import deadlydisasters.general.WorldObject;
import deadlydisasters.utils.AsyncRepeatingTask;
import deadlydisasters.utils.Metrics;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public abstract class DestructionDisaster extends DisasterEvent {
	protected Location loc;
	protected Player p;
	public Main plugin;
	public double volume;
	
	public static Map<World,Queue<Player>> currentLocations = new HashMap<>();
	
	public DestructionDisaster(int level) {
		this.plugin = Main.getInstance();
		this.level = level;
	}
	public Disaster getType() {
		return type;
	}
	public abstract void start(Location loc, Player p);
	
	public void broadcastMessage(Location temp, Player p) {
		if ((boolean) WorldObject.findWorldObject(temp.getWorld()).settings.get("event_broadcast"))
			Utils.broadcastEvent(level, "destructive", type, temp, p);
	}
	public abstract void startAdjustment(Location loc, Player p);
	public abstract Location findApplicableLocation(Location temp, Player p);
	
	public void createTimedStart(int delaySeconds, Vector offset, Player p) {
		this.loc = p.getLocation();
		if (!currentLocations.containsKey(loc.getWorld()))
			currentLocations.put(loc.getWorld(), new ArrayDeque<>());
		currentLocations.get(loc.getWorld()).add(p);
		inputPlayerToMap(delaySeconds, p);
		plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
			public void run() {
				if (!currentLocations.containsKey(loc.getWorld()) || !currentLocations.get(loc.getWorld()).contains(p))
					return;
				currentLocations.get(loc.getWorld()).remove(p);
				if (currentLocations.get(loc.getWorld()).isEmpty())
					currentLocations.remove(loc.getWorld());
				if (!p.isOnline())
					return;
				WorldObject wo = WorldObject.findWorldObject(p.getWorld());
				if (Utils.isPlayerImmune(p) || !wo.naturalAllowed || !wo.allowed.contains(type))
					return;
				Location temp = findApplicableLocation(p.getLocation().clone().add(offset), p);
				if (temp == null)
					return;
				if ((boolean) wo.settings.get("event_broadcast") && type != Disaster.GEYSER && type != Disaster.PURGE && type != Disaster.INFESTEDCAVES)
					Utils.broadcastEvent(level, "destructive", type, temp, p);
				if (currentLocations.containsKey(p.getWorld())) {
					for (Entity e : p.getNearbyEntities(wo.maxRadius, wo.maxRadius, wo.maxRadius))
						if (e instanceof Player && currentLocations.get(p.getWorld()).contains(e))
							currentLocations.get(p.getWorld()).remove(e);
					if (currentLocations.get(p.getWorld()).isEmpty())
						currentLocations.remove(p.getWorld());
				}
				plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
					public void run() {
						start(temp, p);
						Metrics.incrementValue(Metrics.disasterOccurredMap, type.getMetricsLabel());
					}
				}, type.getDelayTicks());
			}
		}, delaySeconds * 20);
	}
	public void triggerRegen() {
		if (WorldObject.findWorldObject(loc.getWorld()).difficulty != DifficultyLevel.CUSTOM || (int) WorldObject.findWorldObject(loc.getWorld()).settings.get("regenDelay") < 0)
			return;
		DisasterEvent instance = this;
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				reverseList();
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
	public void testRegen() {
		if (WorldObject.findWorldObject(loc.getWorld()).difficulty != DifficultyLevel.CUSTOM || (int) WorldObject.findWorldObject(loc.getWorld()).settings.get("regenDelay") < 0)
			return;
		DisasterEvent instance = this;
		reverseList();
		final double regenRate = type.getRegenTickRate();
		double[] regenTicks = {0};
		AsyncRepeatingTask task = new AsyncRepeatingTask(plugin, (int) WorldObject.findWorldObject(loc.getWorld()).settings.get("regenDelay") * 20, 1) {
			@Override
			public void run() {
				regenTicks[0] += regenRate;
				while (regenTicks[0] >= 1) {
					regenTicks[0] -= 1;
					if (testDamagedBlocks.isEmpty()) {
						cancel();
						testTasks.remove(this);
						return;
					}
					Entry<Block, Object[]> entry = testDamagedBlocks.entrySet().iterator().next();
					Block b = entry.getKey();
					Material material = (Material) entry.getValue()[1];
					Future<Material> futureCall = plugin.getServer().getScheduler().callSyncMethod(plugin, () -> b.getType());
					Material current = null;
					try {
						current = futureCall.get();
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (current == material || ((b.getType() == Material.WATER || b.getType() == Material.LAVA) && ((Levelled) b.getBlockData()).getLevel() > 0)
							|| b.getType() == Material.SNOW || material == Material.FIRE) {
//						BlockRegenHandler.queueBlockRegen(b, );
					}
					testDamagedBlocks.remove(entry.getKey());
				}
			}
		};
		Map<DisasterEvent, Map<Block,Object[]>> tempMap = new HashMap<>();
		tempMap.put(instance, testDamagedBlocks);
		testTasks.put(task, tempMap);
	}
	public double getVolume() {
		return volume;
	}
	public void setVolume(double volume) {
		this.volume = volume;
	}
	public Location getLocation() {
		return loc;
	}
	public void setLocation(Location loc) {
		this.loc = loc;
	}
	public Player getP() {
		return p;
	}
	public void setP(Player p) {
		this.p = p;
	}
}
