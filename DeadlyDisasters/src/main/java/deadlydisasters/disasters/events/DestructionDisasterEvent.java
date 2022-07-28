package deadlydisasters.disasters.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class DestructionDisasterEvent extends Event implements Cancellable {
	
	private DestructionDisaster disaster;
	private Location loc;
	private Player p;
	private int level;
	private boolean isCancelled;
	
	private static final HandlerList handlers = new HandlerList();
	 
	public DestructionDisasterEvent(DestructionDisaster disaster, Location loc, int level, Player p) {
		this.disaster = disaster;
		this.loc = loc;
		this.level = level;
	    this.p = p;
	}
	public DestructionDisaster getDisaster() {
		return this.disaster;
	}
	public Player getPlayer() {
	    return this.p;
	}
	public Location getLocation() {
	    return this.loc;
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
