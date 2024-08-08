package com.github.jewishbanana.deadlydisasters.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.handlers.ItemsHandler;
import com.github.jewishbanana.deadlydisasters.items.PlagueCure;
import com.github.jewishbanana.deadlydisasters.items.SplashPlagueCure;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class CraftingListener implements Listener {
	
	public CraftingListener(Main plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler
	public void onCraft(PrepareItemCraftEvent e) {
		if (e.getRecipe() == null || !e.getRecipe().getResult().hasItemMeta())
			return;
		ItemStack item = e.getRecipe().getResult();
		if (item.getItemMeta().getPersistentDataContainer().has(ItemsHandler.snowGlobeKey, PersistentDataType.BYTE)) {
			ItemStack[] items = e.getInventory().getContents();
			for (ItemStack it : items) {
				switch (it.getType()) {
				case PLAYER_HEAD:
					if (!it.getItemMeta().getPersistentDataContainer().has(ItemsHandler.brokenSnowGlobeKey, PersistentDataType.BYTE) && !it.getItemMeta().getPersistentDataContainer().has(ItemsHandler.snowGlobeKey, PersistentDataType.BYTE))
						e.getInventory().setResult(new ItemStack(Material.AIR));
					break;
				case DIAMOND_SWORD:
					if (!it.getItemMeta().getPersistentDataContainer().has(ItemsHandler.candyCaneKey, PersistentDataType.BYTE))
						e.getInventory().setResult(new ItemStack(Material.AIR));
					break;
				case GHAST_TEAR:
					if (!it.getItemMeta().getPersistentDataContainer().has(ItemsHandler.ornamentKey, PersistentDataType.BYTE))
						e.getInventory().setResult(new ItemStack(Material.AIR));
					break;
				default:
					break;
				}
			}
		} else if (item.getItemMeta().getPersistentDataContainer().has(ItemsHandler.easterBasketKey, PersistentDataType.BYTE)) {
			ItemStack[] items = e.getInventory().getContents();
			boolean[] eggs = new boolean[5];
			for (int i=0; i < 5; i++)
				eggs[i] = false;
			for (ItemStack it : items) {
				if (!it.hasItemMeta())
					continue;
				PersistentDataContainer data = it.getItemMeta().getPersistentDataContainer();
				if (data.has(ItemsHandler.greenEasterEggKey, PersistentDataType.BYTE))
					eggs[0] = true;
				else if (data.has(ItemsHandler.blueEasterEggKey, PersistentDataType.BYTE))
					eggs[1] = true;
				else if (data.has(ItemsHandler.redEasterEggKey, PersistentDataType.BYTE))
					eggs[2] = true;
				else if (data.has(ItemsHandler.orangeEasterEggKey, PersistentDataType.BYTE))
					eggs[3] = true;
				else if (data.has(ItemsHandler.purpleEasterEggKey, PersistentDataType.BYTE))
					eggs[4] = true;
			}
			for (boolean val : eggs)
				if (!val) {
					e.getInventory().setResult(new ItemStack(Material.AIR));
					return;
				}
		} else if (item.getItemMeta().getPersistentDataContainer().has(ItemsHandler.pumpkinBasketKey, PersistentDataType.BYTE)) {
			ItemStack[] items = e.getInventory().getContents();
			boolean[] values = new boolean[4];
			for (int i=0; i < 4; i++)
				values[i] = false;
			for (ItemStack it : items) {
				if (!it.hasItemMeta())
					continue;
				PersistentDataContainer data = it.getItemMeta().getPersistentDataContainer();
				if (data.has(ItemsHandler.cursedFleshKey, PersistentDataType.BYTE))
					values[0] = true;
				else if (data.has(ItemsHandler.vampireFangKey, PersistentDataType.BYTE))
					values[1] = true;
				else if (data.has(ItemsHandler.candyCornKey, PersistentDataType.BYTE))
					values[2] = true;
				else if (data.has(ItemsHandler.spookyPumpkinKey, PersistentDataType.BYTE))
					values[3] = true;
			}
			for (boolean val : values)
				if (!val) {
					e.getInventory().setResult(new ItemStack(Material.AIR));
					return;
				}
		}
	}
	@SuppressWarnings("removal")
	@EventHandler
	public void onAnvil(PrepareAnvilEvent e) {
		if (e.getInventory().getItem(0) == null || e.getInventory().getItem(1) == null || !e.getInventory().getItem(1).hasItemMeta())
			return;
		ItemStack item = e.getInventory().getItem(0);
		ItemStack secondSlot = e.getInventory().getItem(1);
		if (item.hasItemMeta()) {
			if (item.getItemMeta().getPersistentDataContainer().has(ItemsHandler.santaHatKey, PersistentDataType.INTEGER)) {
				if (secondSlot.getItemMeta().getPersistentDataContainer().has(ItemsHandler.ornamentKey, PersistentDataType.BYTE)) {
					ItemStack result = item.clone();
					Utils.repairItem(result, secondSlot.getAmount()*100);
					e.setResult(result);
					e.getInventory().setRepairCost(secondSlot.getAmount()*5);
					return;
				}
				e.setResult(new ItemStack(Material.AIR));
				return;
			}
		}
	}
	@EventHandler
	public void onBrew(BrewEvent e) {
		if (e.isCancelled() || !DependencyUtils.isUIFrameworkEnabled())
			return;
		ItemStack[] potions = {e.getContents().getStorageContents()[0], e.getContents().getStorageContents()[1], e.getContents().getStorageContents()[2]};
		for (int i=0; i < 3; i++) {
			ItemStack item = potions[i];
			com.github.jewishbanana.uiframework.items.GenericItem base = com.github.jewishbanana.uiframework.items.GenericItem.getItemBaseNoID(item);
			if (base != null && base.getClass().equals(PlagueCure.class)) {
				if (e.getContents().getIngredient().getType() == Material.GUNPOWDER)
					e.getResults().set(i, com.github.jewishbanana.uiframework.items.ItemType.getItemType(SplashPlagueCure.REGISTERED_KEY).getBuilder().getItem());
			}
		}
	}
}
