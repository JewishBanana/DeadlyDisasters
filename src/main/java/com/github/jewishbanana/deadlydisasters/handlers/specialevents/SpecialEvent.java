package com.github.jewishbanana.deadlydisasters.handlers.specialevents;

import org.bukkit.entity.Player;

import com.github.jewishbanana.deadlydisasters.Main;

public class SpecialEvent {

	public boolean isEnabled;
	
	public static SpecialEvent checkForEvent(Main plugin) {
		SpecialEvent event = new EasterEventHandler(plugin);
		if (event.isEnabled)
			return event;
		event = new HalloweenEventHandler(plugin);
		if (event.isEnabled)
			return event;
		return new SpecialEvent();
	}
	public void openGUI(Player player) {
	}
	public void saveData() {
	}
}
