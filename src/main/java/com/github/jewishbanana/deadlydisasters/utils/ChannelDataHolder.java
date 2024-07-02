package com.github.jewishbanana.deadlydisasters.utils;

import org.bukkit.World;
import org.bukkit.entity.Player;

public class ChannelDataHolder {
	
	private Player player;
	private World world;
	
	public ChannelDataHolder(Player player) {
		this.player = player;
	}
	public Player getPlayer() {
		return player;
	}
	public void setPlayer(Player player) {
		this.player = player;
	}
	public World getWorld() {
		return world;
	}
	public void setWorld(World world) {
		this.world = world;
	}
}
