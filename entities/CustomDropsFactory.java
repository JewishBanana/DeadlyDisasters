package deadlydisasters.entities;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import deadlydisasters.general.ItemsHandler;
import deadlydisasters.general.Main;
import deadlydisasters.utils.Utils;

public class CustomDropsFactory {
	
	private static boolean allowDrops;
	private static Map<String,Queue<CustomDrop>> dropMap = new HashMap<>();

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
		allowDrops = plugin.getConfig().getBoolean("customentities.allow_custom_drops");
		Random rand = new Random();
		for (CustomEntityType temp : CustomEntityType.values()) {
			dropMap.put(temp.species, new ArrayDeque<CustomDrop>());
			for (String s : temp.getDropsList()) {
				s = s.replaceAll("\\s+", "");
				ItemStack item = null;
				if (ItemsHandler.allItems.containsKey(s.substring(0, s.indexOf('|'))))
					item = ItemsHandler.allItems.get(s.substring(0, s.indexOf('|')));
				else if (Material.getMaterial(s.substring(0, s.indexOf('|')).toUpperCase()) != null)
					item = new ItemStack(Material.getMaterial(s.substring(0, s.indexOf('|')).toUpperCase()));
				else {
					if (plugin.debug)
						Main.consoleSender.sendMessage(Utils.chat("&e[DeadlyDisasters]: Item &d'"+s.substring(0, s.indexOf('|'))+"' &edoes not exist in config at &c"+temp.configPath+".drops"));
					continue;
				}
				s = s.substring(s.indexOf('|')+1);
				double chance = 0;
				try {
					chance = Double.parseDouble(s.substring(0, s.indexOf('|'))) / 100;
				} catch (NumberFormatException e) {
					if (plugin.debug)
						Main.consoleSender.sendMessage(Utils.chat("&e[DeadlyDisasters]: &d'"+s.substring(0, s.indexOf('|'))+"' &eis not a valid double in config at &c"+temp.configPath+".drops"));
					continue;
				}
				s = s.substring(s.indexOf('|')+1);
				int min,max;
				try {
					min = Integer.parseInt(s.substring(0, s.indexOf('-')));
					max = Integer.parseInt(s.substring(s.indexOf('-')+1));
				} catch (NumberFormatException e) {
					if (plugin.debug)
						Main.consoleSender.sendMessage(Utils.chat("&e[DeadlyDisasters]: &d'"+s+"' &einvalid integer min-max value in config at &c"+temp.configPath+".drops"));
					continue;
				}
				dropMap.get(temp.species).add(new CustomDrop(item, chance, min, max, rand));
			}
		}
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