package com.github.jewishbanana.deadlydisasters.disasters;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class DisasterRegistry {
	
	private static final Map<String, DisasterRegistry> register;
	static {
		register = new HashMap<>();
	}
	
	private final String registeredName;
	private Class<? extends Disaster> registeredClass;
	
	private DisasterRegistry(String registeredName, Class<? extends Disaster> registeredClass) {
		this.registeredName = registeredName;
		this.registeredClass = registeredClass;
	}
	public static void registerDisaster(String registeredName, Class<? extends Disaster> registeredClass) {
		String reformatted = registeredName.toLowerCase();
		if (register.containsKey(reformatted))
			throw new IllegalArgumentException("There is already a registered disaster with the name '"+reformatted+"' registered names must be unique!");
		DisasterRegistry registry = new DisasterRegistry(reformatted, registeredClass);
		register.put(reformatted, registry);
	}
	public static DisasterRegistry getRegistry(String registeredName) {
		return register.get(registeredName.toLowerCase());
	}
	public static DisasterRegistry getRegistry(Class<? extends Disaster> disasterClass) {
		for (DisasterRegistry registry : register.values())
			if (registry.registeredClass.equals(disasterClass))
				return registry;
		return null;
	}
	public static Set<String> getRegisteredNames() {
		return register.keySet();
	}
	public static Collection<DisasterRegistry> getRegisteredDisasters() {
		return register.values();
	}
	public Disaster createDisaster(Location location, Player player, int level, boolean initialize) {
		try {
			Disaster disaster = registeredClass.getDeclaredConstructor(Location.class, Player.class, int.class).newInstance(location, player, level);
			if (initialize)
				disaster.init();
			return disaster;
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
		}
		return null;
	}
	public Disaster createDisaster(Location location, Player player, int level) {
		return createDisaster(location, player, level, true);
	}
	public Class<? extends Disaster> getRegisteredClass() {
		return registeredClass;
	}
	public String getRegisteredName() {
		return registeredName;
	}
}
