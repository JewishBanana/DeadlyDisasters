package com.github.jewishbanana.deadlydisasters.disasters.destructive;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.github.jewishbanana.deadlydisasters.disasters.Disaster;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class Tsunami extends Disaster {
	
	private int minHeight;
	private double size;
	private int height;
	
	public Tsunami(Location location, Player player, int level) {
		super(location, player, level);
		
		this.minHeight = getConfigInt("minimum_height");
	}
	public void init() {
		super.init();
		this.size = getConfigDouble("size");
		switch (level) {
		default:
		case 1:
			disasterRange = 6;
			break;
		case 2:
			disasterRange = 15;
			break;
		case 3:
			disasterRange = 20;
			break;
		case 4:
			disasterRange = 25;
			break;
		case 5:
			disasterRange = 35;
			break;
		case 6:
			disasterRange = 50;
			break;
		}
		disasterRange *= size;
		this.height = level + 1;
	}
	public boolean canStart() {
		if (getLocation().getBlockY() < minHeight)
			return false;
		if (!Utils.isAreaFlatGrounded(getLocation()))
			return false;
		return super.canStart();
	}
	public void start() {
		super.start();
		addDeathWatcher("deaths.tsunami");
	}
	public void clean() {
		super.clean();
		removeDeathWatcher(300);
	}
	public Function<PlayerDeathEvent, Boolean> getDeathCheck() {
		return event -> {
			if (event.getEntity().getLastDamageCause() == null)
				return false;
			DamageCause cause = event.getEntity().getLastDamageCause().getCause();
			if (cause != DamageCause.DROWNING)
				return false;
			Location loc = event.getEntity().getLocation();
			if (loc.getY() < location.getY() - level
					|| loc.getY() > location.getY() + height
					|| !Utils.isLocationsWithinDistance(new Location(loc.getWorld(), loc.getX(), location.getY(), loc.getZ()), location, disasterRange * disasterRange))
				return false;
			return true;
		};
	}
	protected String getConfigPath() {
		return "disasters.destructive.tsunami";
	}
	public Set<Environment> getBannedEnvironments() {
		return EnumSet.of(Environment.NETHER, Environment.THE_END);
	}
}
