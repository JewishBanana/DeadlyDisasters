package com.github.jewishbanana.deadlydisasters.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import com.github.jewishbanana.deadlydisasters.disasters.Disaster;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;

public class DisasterStartEvent extends Event implements Cancellable {
	
	public enum DisasterStartReason {
		COMMAND,
		NATURAL,
		CUSTOM
	}

	private static final HandlerList handlers = new HandlerList();
	
	private Disaster disaster;
	private DisasterStartReason reason;
	private boolean isCancelled;
	
	public DisasterStartEvent(@NotNull Disaster disaster, @NotNull DisasterStartReason reason) {
		this.disaster = disaster;
		this.reason = reason;
		this.isCancelled = DependencyUtils.isDisasterStartBlocked(disaster.getLocation());
	}
	public Disaster getDisaster() {
		return disaster;
	}
	public Player getPlayer() {
	    return disaster.getPlayer();
	}
	public Location getLocation() {
	    return disaster.getLocation();
	}
	public int getLevel() {
		return disaster.getLevel();
	}
	public DisasterStartReason getReason() {
		return reason;
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
