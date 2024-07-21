package com.github.jewishbanana.deadlydisasters.listeners.customevents;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import com.github.jewishbanana.deadlydisasters.listeners.ArmorListener.ArmorSlot;

public class ArmorEquipEvent extends Event {
	
	private Player player;
	private ArmorSlot slot;
	private ItemStack item;

	private static final HandlerList handlers = new HandlerList();
	
	public ArmorEquipEvent(Player player, ArmorSlot slot, ItemStack item) {
		this.player = player;
		this.slot = slot;
		this.item = item;
	}
	public Player getPlayer() {
		return player;
	}
	public void setPlayer(Player player) {
		this.player = player;
	}
	public ArmorSlot getSlot() {
		return slot;
	}
	public void setSlot(ArmorSlot slot) {
		this.slot = slot;
	}
	public ItemStack getItem() {
		return item;
	}
	public void setItem(ItemStack item) {
		this.item = item;
	}
	@Override
	public HandlerList getHandlers() {
	    return handlers;
	}
	public static HandlerList getHandlerList() {
	    return handlers;
	}
}
