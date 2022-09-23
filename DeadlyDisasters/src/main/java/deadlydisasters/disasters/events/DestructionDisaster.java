package deadlydisasters.disasters.events;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import deadlydisasters.disasters.Disaster;
import deadlydisasters.general.Main;
import deadlydisasters.general.WorldObject;
import deadlydisasters.utils.Metrics;
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
				if ((boolean) wo.settings.get("event_broadcast") && type != Disaster.GEYSER && type != Disaster.PURGE)
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
