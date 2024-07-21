package com.github.jewishbanana.deadlydisasters.handlers.specialevents;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.Ghoul;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.Psyco;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.Scarecrow;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.Vampire;
import com.github.jewishbanana.deadlydisasters.handlers.ItemsHandler;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.listeners.spawners.GlobalSpawner;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class ChristmasEventHandler extends SpecialEvent implements Listener {
	
	private Main plugin;
	private Random rand;
	
	private Set<Inventory> invs = new HashSet<>();
	private boolean notify;
	
	public ChristmasEventHandler(Main plugin) {
		this.plugin = plugin;
		this.rand = plugin.random;
		
		if (plugin.mcVersion < 1.16 || !plugin.getConfig().getBoolean("general.special_events"))
			return;
		
		LocalDate date = LocalDate.now();
		if (date.getMonth() == Month.OCTOBER)
			isEnabled = true;
		else if (date.getYear() == 2023 && date.getMonth() == Month.NOVEMBER && date.getDayOfMonth() <= 18)
			isEnabled = true;
		if (isEnabled) {
			notify = plugin.getConfig().getBoolean("messages.event_notifications");
			plugin.getServer().getPluginManager().registerEvents(this, plugin);
		}
	}
	public void openGUI(Player player) {
		Inventory inv = Bukkit.createInventory(null, 27, Utils.chat("&9DeadlyDisasters Halloween Event"));
		ItemStack glass = Utils.createItem(Material.GRAY_STAINED_GLASS_PANE, 1, " ", null, false, true);
		for (int i=0; i < 27; i++)
			inv.setItem(i, glass);
		inv.setItem(4, Utils.createItem(Material.NETHER_STAR, 1, Utils.chat("&6"+Languages.getString("halloween.infoItem")), Arrays.asList(Utils.chat("&a"+Languages.getString("halloween.infoItemLore"))), false, true));
		inv.setItem(10, ItemsHandler.cursedFlesh);
		inv.setItem(11, ItemsHandler.vampireFang);
		inv.setItem(12, ItemsHandler.candyCorn);
		inv.setItem(13, ItemsHandler.spookyPumpkin);
		ItemStack greenGlass = Utils.createItem(Material.LIME_STAINED_GLASS_PANE, 1, " ", null, false, true);
		inv.setItem(14, greenGlass);
		inv.setItem(15, greenGlass);
		inv.setItem(16, ItemsHandler.pumpkinBasket);
		invs.add(inv);
		player.openInventory(inv);
	}
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		if (notify)
			plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
				e.getPlayer().sendMessage(Languages.prefix+Utils.chat("&a"+Languages.getString("halloween.eventMessage")));
			}, 20);
	}
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		if (e.getCurrentItem() == null)
			return;
		if (invs.contains(e.getInventory())) {
			e.setCancelled(true);
			return;
		}
	}
	@EventHandler
	public void onClose(InventoryCloseEvent e) {
		invs.remove(e.getInventory());
	}
	@EventHandler(priority=EventPriority.LOWEST)
	public void onCreatureSpawn(CreatureSpawnEvent e) {
		if (e.isCancelled() || !CustomEntityType.mobsEnabled || e.getSpawnReason() != SpawnReason.NATURAL || !Utils.isEnvironment(e.getLocation().getWorld(), Environment.NORMAL) || GlobalSpawner.noSpawnWorlds.contains(e.getLocation().getWorld()) || !(e.getEntity() instanceof Monster) || !e.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid())
			return;
		Location loc = e.getLocation();
		List<Integer> order = new ArrayList<>(Arrays.asList(1,2,3,4));
		Collections.shuffle(order);
		for (int i : order)
			switch (i) {
			case 1:
				if (rand.nextDouble() * 100 < 2.0) {
					Block block = loc.getBlock().getRelative(BlockFace.DOWN);
					Mob entity = (Mob) loc.getWorld().spawnEntity(block.getRelative(BlockFace.DOWN).getLocation(), EntityType.ZOMBIE, false);
					plugin.handler.addEntity(new Ghoul((Zombie) entity, block, plugin, true));
					e.setCancelled(true);
					break;
				}
				break;
			case 2:
				if (rand.nextDouble() * 100 < 2.0) {
					Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE, false);
					plugin.handler.addEntity(new Scarecrow((Zombie) entity, plugin, rand));
					e.setCancelled(true);
					break;
				}
				break;
			case 3:
				if (rand.nextDouble() * 100 < 1.5) {
					Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.EVOKER, false);
					plugin.handler.addEntity(new Vampire((Mob) entity, plugin));
					e.setCancelled(true);
					break;
				}
				break;
			case 4:
				if (rand.nextDouble() * 100 < 2.0) {
					Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.SKELETON, false);
					plugin.handler.addEntity(new Psyco((Mob) entity, plugin, rand));
					e.setCancelled(true);
					break;
				}
				break;
			}
	}
}
