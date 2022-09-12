package deadlydisasters.disasters.events;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WeatherDisasterEvent extends Event implements Cancellable {
	
	private WeatherDisaster disaster;
	private World world;
	private Player p;
	private int level;
	private boolean isCancelled;
	
	private static final HandlerList handlers = new HandlerList();
	 
	public WeatherDisasterEvent(WeatherDisaster disaster, World world, int level, Player p) {
		this.disaster = disaster;
		this.world = world;
		this.level = level;
	    this.p = p;
	}
	public WeatherDisaster getDisaster() {
		return this.disaster;
	}
	public Player getPlayer() {
	    return this.p;
	}
	public World getWorld() {
	    return this.world;
	}
	public int getLevel() {
		return this.level;
	}
	@Override
	public boolean isCancelled() {
	    return isCancelled;
	}
	@Override
	public void setCancelled(boolean arg0) {
		this.isCancelled = arg0;
	}
	@Override
	public HandlerList getHandlers() {
	    return handlers;
	}
	public static HandlerList getHandlerList() {
	    return handlers;
	}
}
