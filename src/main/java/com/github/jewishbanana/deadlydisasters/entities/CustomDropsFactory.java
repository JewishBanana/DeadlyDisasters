package com.github.jewishbanana.deadlydisasters.entities;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.handlers.ItemsHandler;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class CustomDropsFactory {
	
	private static boolean allowDrops;
	private static Map<String,Queue<CustomDrop>> dropMap = new HashMap<>();
	public static Map<String, String> changesMap;
	public static boolean warnedItems;
	static {
		changesMap = Map.of(
				"voidshard", "dd:void_tear",
				"voidswrath", "ui:call_of_the_void",
				"voidsedge", "ui:voids_edge",
				"voidshield", "ui:abyssal_shield",
				"ancientcloth", "ui:ancient_cloth",
				"ancientbone", "ui:ancient_bone",
				"yetifur", "ui:yeti_fur",
				"poseidonstrident", "ui:tritons_fang",
				"goldenegg", "ui:golden_egg");
	}

	public static void generateDrops(Location loc, CustomEntityType type) {
		if (allowDrops)
			for (CustomDrop drop : dropMap.get(type.species))
				drop.generateItems(loc, false);
	}
	public static void generateDrops(Location loc, CustomEntityType type, boolean setInvincible) {
		if (allowDrops)
			for (CustomDrop drop : dropMap.get(type.species))
				drop.generateItems(loc, setInvincible);
	}
	public static void reload(Main plugin) {
		dropMap.clear();
		allowDrops = CustomEntityType.yaml.getBoolean("settings.allow_custom_drops");
		Random rand = new Random();
		warnedItems = false;
		for (CustomEntityType temp : CustomEntityType.values()) {
			dropMap.put(temp.species, new ArrayDeque<CustomDrop>());
			for (String s : temp.getDropsList()) {
				s = s.replaceAll("\\s+", "");
				ItemStack item = null;
				String itemName = s.substring(0, s.indexOf('|'));
				if (ItemsHandler.allItems.containsKey(itemName))
					item = ItemsHandler.allItems.get(itemName);
				else if (DependencyUtils.isUIFrameworkEnabled()) {
					String uiName = changesMap.containsKey(itemName) ? changesMap.get(itemName) : itemName;
					if (com.github.jewishbanana.uiframework.items.ItemType.getItemType(uiName) != null)
						item = com.github.jewishbanana.uiframework.items.ItemType.getItemType(uiName).getBuilder().getItem();
					else if (DependencyUtils.isUltimateContentEnabled() && com.github.jewishbanana.uiframework.items.ItemType.getItemType(uiName) != null)
						item = com.github.jewishbanana.uiframework.items.ItemType.getItemType(uiName).getBuilder().getItem();
				} else if (changesMap.containsKey(itemName) || changesMap.containsValue(itemName)) {
					if (!warnedItems) {
						Main.consoleSender.sendMessage(Utils.convertString("&e[DeadlyDisasters]: &bYou have custom drop items in your entities.yml class that are a part of UltimateContent! If you want these custom items then download UltimateContent to your server."));
						warnedItems = true;
					}
					continue;
				}
				if (item == null && Material.getMaterial(itemName.toUpperCase()) != null)
					item = new ItemStack(Material.getMaterial(itemName.toUpperCase()));
				if (item == null) {
					if (plugin.debug)
						Main.consoleSender.sendMessage(Utils.convertString("&e[DeadlyDisasters]: Item &d'"+s.substring(0, s.indexOf('|'))+"' &edoes not exist in config at &c"+temp.configPath+".drops"));
					continue;
				}
				s = s.substring(s.indexOf('|')+1);
				double chance = 0;
				try {
					chance = Double.parseDouble(s.substring(0, s.indexOf('|'))) / 100;
				} catch (NumberFormatException e) {
					if (plugin.debug)
						Main.consoleSender.sendMessage(Utils.convertString("&e[DeadlyDisasters]: &d'"+s.substring(0, s.indexOf('|'))+"' &eis not a valid double in config at &c"+temp.configPath+".drops"));
					continue;
				}
				s = s.substring(s.indexOf('|')+1);
				int min,max;
				try {
					min = Integer.parseInt(s.substring(0, s.indexOf('-')));
					max = Integer.parseInt(s.substring(s.indexOf('-')+1));
				} catch (NumberFormatException e) {
					if (plugin.debug)
						Main.consoleSender.sendMessage(Utils.convertString("&e[DeadlyDisasters]: &d'"+s+"' &einvalid integer min-max value in config at &c"+temp.configPath+".drops"));
					continue;
				}
				dropMap.get(temp.species).add(new CustomDrop(item, chance, min, max, rand));
			}
		}
		warnedItems = false;
	}
}
class CustomDrop {
	
	private ItemStack item;
	private double chance;
	private int min,max;
	private Random rand;
	
	public CustomDrop(ItemStack item, double chance, int min, int max, Random rand) {
		this.item = item;
		this.chance = chance;
		this.min = min;
		this.max = max;
		this.rand = rand;
	}
	public void generateItems(Location loc, boolean setInvincible) {
		if (min > 0)
			for (int i=0; i < min; i++) {
				Item drop = loc.getWorld().dropItemNaturally(loc, item);
				drop.setInvulnerable(setInvincible);
			}
		for (int i=max-min; i > 0; i--)
			if (rand.nextDouble() < chance) {
				Item drop = loc.getWorld().dropItemNaturally(loc, item);
				drop.setInvulnerable(setInvincible);
			}
	}
}