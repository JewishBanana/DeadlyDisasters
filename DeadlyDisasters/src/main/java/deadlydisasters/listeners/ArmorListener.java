package deadlydisasters.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import deadlydisasters.general.Main;
import deadlydisasters.listeners.customevents.ArmorEquipEvent;
import deadlydisasters.listeners.customevents.ArmorUnequipEvent;

public class ArmorListener implements Listener {
	
	private Main plugin;
	
	public static enum ArmorSlot {
		HEAD,
		CHEST,
		LEGS,
		FEET;
	}
	
	public ArmorListener(Main plugin) {
		this.plugin = plugin;
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		ItemStack[] oldArmor = {e.getWhoClicked().getEquipment().getBoots(), e.getWhoClicked().getEquipment().getLeggings(), e.getWhoClicked().getEquipment().getChestplate(), e.getWhoClicked().getEquipment().getHelmet()};
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
			ItemStack[] newArmor = {e.getWhoClicked().getEquipment().getBoots(), e.getWhoClicked().getEquipment().getLeggings(), e.getWhoClicked().getEquipment().getChestplate(), e.getWhoClicked().getEquipment().getHelmet()};
			for (int i=0; i < 4; i++) {
				if ((oldArmor[i] == null && newArmor[i] != null) || (oldArmor[i] != null && newArmor[i] != null && !oldArmor[i].equals(newArmor[i])))
					Bukkit.getPluginManager().callEvent(new ArmorEquipEvent((Player) e.getWhoClicked(), i == 0 ? ArmorSlot.FEET : i == 1 ? ArmorSlot.LEGS : i == 2 ? ArmorSlot.CHEST : ArmorSlot.HEAD, newArmor[i]));
				if ((oldArmor[i] != null && newArmor[i] == null) || (oldArmor[i] != null && newArmor[i] != null && !oldArmor[i].equals(newArmor[i])))
					Bukkit.getPluginManager().callEvent(new ArmorUnequipEvent((Player) e.getWhoClicked(), i == 0 ? ArmorSlot.FEET : i == 1 ? ArmorSlot.LEGS : i == 2 ? ArmorSlot.CHEST : ArmorSlot.HEAD, oldArmor[i]));
			}
		}, 1);
	}
	@EventHandler
	public void onDrag(InventoryDragEvent e) {
		ItemStack[] oldArmor = {e.getWhoClicked().getEquipment().getBoots(), e.getWhoClicked().getEquipment().getLeggings(), e.getWhoClicked().getEquipment().getChestplate(), e.getWhoClicked().getEquipment().getHelmet()};
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
			ItemStack[] newArmor = {e.getWhoClicked().getEquipment().getBoots(), e.getWhoClicked().getEquipment().getLeggings(), e.getWhoClicked().getEquipment().getChestplate(), e.getWhoClicked().getEquipment().getHelmet()};
			for (int i=0; i < 4; i++) {
				if ((oldArmor[i] == null && newArmor[i] != null) || (oldArmor[i] != null && newArmor[i] != null && !oldArmor[i].equals(newArmor[i])))
					Bukkit.getPluginManager().callEvent(new ArmorEquipEvent((Player) e.getWhoClicked(), i == 0 ? ArmorSlot.FEET : i == 1 ? ArmorSlot.LEGS : i == 2 ? ArmorSlot.CHEST : ArmorSlot.HEAD, newArmor[i]));
				if ((oldArmor[i] != null && newArmor[i] == null) || (oldArmor[i] != null && newArmor[i] != null && !oldArmor[i].equals(newArmor[i])))
					Bukkit.getPluginManager().callEvent(new ArmorUnequipEvent((Player) e.getWhoClicked(), i == 0 ? ArmorSlot.FEET : i == 1 ? ArmorSlot.LEGS : i == 2 ? ArmorSlot.CHEST : ArmorSlot.HEAD, oldArmor[i]));
			}
		}, 1);
	}
	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		ItemStack[] oldArmor = {e.getPlayer().getEquipment().getBoots(), e.getPlayer().getEquipment().getLeggings(), e.getPlayer().getEquipment().getChestplate(), e.getPlayer().getEquipment().getHelmet()};
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
			ItemStack[] newArmor = {e.getPlayer().getEquipment().getBoots(), e.getPlayer().getEquipment().getLeggings(), e.getPlayer().getEquipment().getChestplate(), e.getPlayer().getEquipment().getHelmet()};
			for (int i=0; i < 4; i++) {
				if ((oldArmor[i] == null && newArmor[i] != null) || (oldArmor[i] != null && newArmor[i] != null && !oldArmor[i].equals(newArmor[i])))
					Bukkit.getPluginManager().callEvent(new ArmorEquipEvent(e.getPlayer(), i == 0 ? ArmorSlot.FEET : i == 1 ? ArmorSlot.LEGS : i == 2 ? ArmorSlot.CHEST : ArmorSlot.HEAD, newArmor[i]));
				if ((oldArmor[i] != null && newArmor[i] == null) || (oldArmor[i] != null && newArmor[i] != null && !oldArmor[i].equals(newArmor[i])))
					Bukkit.getPluginManager().callEvent(new ArmorUnequipEvent(e.getPlayer(), i == 0 ? ArmorSlot.FEET : i == 1 ? ArmorSlot.LEGS : i == 2 ? ArmorSlot.CHEST : ArmorSlot.HEAD, oldArmor[i]));
			}
		}, 1);
	}
	@EventHandler
	public void onDispense(BlockDispenseArmorEvent e) {
		if (!(e.getTargetEntity() instanceof Player))
			return;
		ItemStack[] oldArmor = {e.getTargetEntity().getEquipment().getBoots(), e.getTargetEntity().getEquipment().getLeggings(), e.getTargetEntity().getEquipment().getChestplate(), e.getTargetEntity().getEquipment().getHelmet()};
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
			ItemStack[] newArmor = {e.getTargetEntity().getEquipment().getBoots(), e.getTargetEntity().getEquipment().getLeggings(), e.getTargetEntity().getEquipment().getChestplate(), e.getTargetEntity().getEquipment().getHelmet()};
			for (int i=0; i < 4; i++) {
				if ((oldArmor[i] == null && newArmor[i] != null) || (oldArmor[i] != null && newArmor[i] != null && !oldArmor[i].equals(newArmor[i])))
					Bukkit.getPluginManager().callEvent(new ArmorEquipEvent((Player) e.getTargetEntity(), i == 0 ? ArmorSlot.FEET : i == 1 ? ArmorSlot.LEGS : i == 2 ? ArmorSlot.CHEST : ArmorSlot.HEAD, newArmor[i]));
				if ((oldArmor[i] != null && newArmor[i] == null) || (oldArmor[i] != null && newArmor[i] != null && !oldArmor[i].equals(newArmor[i])))
					Bukkit.getPluginManager().callEvent(new ArmorUnequipEvent((Player) e.getTargetEntity(), i == 0 ? ArmorSlot.FEET : i == 1 ? ArmorSlot.LEGS : i == 2 ? ArmorSlot.CHEST : ArmorSlot.HEAD, oldArmor[i]));
			}
		}, 1);
	}
}
