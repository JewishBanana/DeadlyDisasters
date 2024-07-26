package com.github.jewishbanana.deadlydisasters.handlers.specialevents;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Goat;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.easterentities.KillerChicken;
import com.github.jewishbanana.deadlydisasters.entities.easterentities.RampagingGoat;
import com.github.jewishbanana.deadlydisasters.handlers.ItemsHandler;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.listeners.spawners.GlobalSpawner;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class EasterEventHandler extends SpecialEvent implements Listener {
	
	private Main plugin;
	private int year;
	public EggGoal[] eggs = new EggGoal[5];
	private String progress;
	private Set<Inventory> invs = new HashSet<>();
	private boolean notify;
	
	private Set<UUID> modifiedTrades = new HashSet<>();
	private Map<UUID, Set<UUID>> droppedEggs = new HashMap<>();
	
	public EasterEventHandler(Main plugin) {
		this.plugin = plugin;
		
		if (plugin.mcVersion < 1.17 || !plugin.getConfig().getBoolean("general.special_events"))
			return;
		
		LocalDate date = LocalDate.now();
		this.year = date.getYear();
		switch (year) {
		case 2023:
			if (date.getMonth() == Month.APRIL && date.getDayOfMonth() >= 2 && date.getDayOfMonth() <= 16)
				isEnabled = true;
			break;
		case 2024:
			if ((date.getMonth() == Month.MARCH && date.getDayOfMonth() >= 24) || (date.getMonth() == Month.APRIL && date.getDayOfMonth() <= 1))
				isEnabled = true;
			break;
		case 2025:
			if (date.getMonth() == Month.APRIL && date.getDayOfMonth() >= 13 && date.getDayOfMonth() <= 21)
				isEnabled = true;
			break;
		case 2026:
			if ((date.getMonth() == Month.MARCH && date.getDayOfMonth() >= 29) || (date.getMonth() == Month.APRIL && date.getDayOfMonth() <= 6))
				isEnabled = true;
			break;
		default:
			if (date.getMonth() == Month.APRIL && date.getDayOfMonth() >= 1 && date.getDayOfMonth() <= 9)
				isEnabled = true;
			break;
		}
		if (isEnabled) {
			progress = Languages.getString("words.progress");
			notify = plugin.getConfig().getBoolean("messages.event_notifications");
			plugin.getServer().getPluginManager().registerEvents(this, plugin);
			
			for (int i=0; i < 5; i++)
				eggs[i] = new EggGoal(i);
		}
	}
	public void openGUI(Player player) {
		Inventory inv = Bukkit.createInventory(null, 18, Utils.convertString("&9DeadlyDisasters Easter Event"));
		inv.setItem(4, Utils.createItem(Material.NETHER_STAR, 1, Utils.convertString("&6"+Languages.getString("easter.infoItem")), Arrays.asList(Utils.convertString("&a"+Languages.getString("easter.infoItemLore"))), false, true));
		for (int i=0; i < 5; i++) {
			List<String> lore = new ArrayList<>();
			lore.add(eggs[i].description);
			lore.add(" ");
			lore.add(Utils.convertString("&b"+progress+": "+eggs[i].getProgress(player.getUniqueId())+'/'+eggs[i].goal));
			ItemStack item = Utils.createItem(Material.TURTLE_EGG, 1, eggs[i].name, lore, false, true);
			ItemMeta meta = item.getItemMeta();
			meta.setCustomModelData(eggs[i].cmd);
			item.setItemMeta(meta);
			inv.setItem(i+11, item);
		}
		invs.add(inv);
		player.openInventory(inv);
	}
	public void saveData() {
		for (EggGoal egg : eggs)
			egg.save();
	}
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		if (notify)
			plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
				e.getPlayer().sendMessage(Languages.prefix+Utils.convertString("&a"+Languages.getString("easter.eventMessage")));
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
		if (e.getClickedInventory() instanceof MerchantInventory && e.getCurrentItem().hasItemMeta()
				&& e.getCurrentItem().getItemMeta().getPersistentDataContainer().has(ItemsHandler.purpleEasterEggKey, PersistentDataType.BYTE)) {
			if (eggs[4].hasAchieved(e.getWhoClicked().getUniqueId())) {
				e.getWhoClicked().sendMessage(Utils.convertString("&c"+Languages.getString("easter.restrictAction")));
				e.setCancelled(true);
				return;
			}
			eggs[4].addProgress(e.getWhoClicked().getUniqueId(), 1);
		}
	}
	@EventHandler
	public void onClose(InventoryCloseEvent e) {
		invs.remove(e.getInventory());
	}
	@EventHandler
	public void onFish(PlayerFishEvent e) {
		if (e.getCaught() != null && e.getCaught() instanceof Item) {
			if (!eggs[1].hasAchieved(e.getPlayer().getUniqueId()) && plugin.random.nextDouble()*100 < 10.0) {
				ItemStack item = ((Item) e.getCaught()).getItemStack();
				item.setType(Material.TURTLE_EGG);
				item = Utils.createItem(item, 1, ChatColor.BLUE+Languages.getString("easter.blueEgg"), Arrays.asList(ChatColor.YELLOW+Languages.getString("easter.blueEggLore")), false, true);
				ItemMeta meta = item.getItemMeta();
				meta.getPersistentDataContainer().set(ItemsHandler.blueEasterEggKey, PersistentDataType.BYTE, (byte) 1);
				meta.setCustomModelData(100024);
				item.setItemMeta(meta);
				eggs[1].addProgress(e.getPlayer().getUniqueId(), 1);
				return;
			}
		}
	}
	@EventHandler
	public void onTradeAquire(VillagerAcquireTradeEvent e) {
		if (e.getEntity() instanceof WanderingTrader && !modifiedTrades.contains(e.getEntity().getUniqueId())) {
			if (plugin.random.nextDouble()*100 < 20.0) {
				MerchantRecipe newRecipe = new MerchantRecipe(ItemsHandler.purpleEasterEgg, 1);
				newRecipe.addIngredient(new ItemStack(Material.EMERALD, plugin.random.nextInt(5)+9));
				newRecipe.setVillagerExperience(20);
				e.setRecipe(newRecipe);
				modifiedTrades.add(e.getEntity().getUniqueId());
				return;
			}
		}
	}
	@EventHandler
	public void onItemSpawn(ItemSpawnEvent e) {
		if (e.getEntity().getItemStack().getType() == Material.EGG && e.getEntity().getVelocity().getY() == 0.2 && plugin.random.nextDouble()*100 < 5.0) {
			ItemStack item = e.getEntity().getItemStack();
			item.setType(Material.TURTLE_EGG);
			item = Utils.createItem(item, 1, ChatColor.GREEN+Languages.getString("easter.greenEgg"), Arrays.asList(ChatColor.YELLOW+Languages.getString("easter.greenEggLore")), false, true);
			ItemMeta meta = item.getItemMeta();
			meta.getPersistentDataContainer().set(ItemsHandler.greenEasterEggKey, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100023);
			item.setItemMeta(meta);
			droppedEggs.put(e.getEntity().getUniqueId(), new HashSet<>());
			return;
		}
	}
	@EventHandler
	public void onItemPickup(EntityPickupItemEvent e) {
		if (e.getEntity() instanceof Player && droppedEggs.containsKey(e.getItem().getUniqueId())) {
			if (eggs[0].hasAchieved(e.getEntity().getUniqueId())) {
				if (!droppedEggs.get(e.getItem().getUniqueId()).contains(e.getEntity().getUniqueId())) {
					e.getEntity().sendMessage(Utils.convertString("&c"+Languages.getString("easter.restrictAction")));
					droppedEggs.get(e.getItem().getUniqueId()).add(e.getEntity().getUniqueId());
				}
				e.setCancelled(true);
				return;
			}
			eggs[0].addProgress(e.getEntity().getUniqueId(), 1);
		}
	}
	@EventHandler
	public void onLootGen(LootGenerateEvent e) {
		if (e.getInventoryHolder() instanceof Chest && ((Chest) e.getInventoryHolder()).getBlock().getLocation().getBlockY() < 30) {
			if (plugin.random.nextDouble()*100 < 10.0) {
				e.getLoot().add(ItemsHandler.orangeEasterEgg);
				if (e.getEntity() instanceof Player)
					eggs[3].addProgress(e.getEntity().getUniqueId(), 1);
			}
		}
	}
	@EventHandler(priority=EventPriority.LOWEST)
	public void onCreatureSpawn(CreatureSpawnEvent e) {
		if (e.isCancelled())
			return;
		if (e.getLocation().getBlockY() > 150 && plugin.random.nextDouble()*100 < 20.0) {
			Goat goat = (Goat) e.getLocation().getWorld().spawnEntity(e.getLocation(), EntityType.GOAT);
			CustomEntity.handler.addEntity(new RampagingGoat(goat, plugin));
			e.getEntity().remove();
			return;
		}
		if (e.getEntityType() != EntityType.CHICKEN || !(e.getSpawnReason() == SpawnReason.NATURAL || e.getSpawnReason() == SpawnReason.BREEDING || e.getSpawnReason() == SpawnReason.EGG) || GlobalSpawner.noSpawnWorlds.contains(e.getLocation().getWorld())
				|| plugin.random.nextDouble()*100 >= 10.0)
			return;
		Zombie zombie = (Zombie) e.getEntity().getWorld().spawnEntity(e.getLocation(), EntityType.ZOMBIE, false);
		CustomEntity.handler.addEntity(new KillerChicken(zombie, plugin));
		e.getEntity().remove();
	}
	public class EggGoal {
		private Map<UUID, Integer> progress = new HashMap<>();
		private int cmd, goal = 1;
		private String name;
		private String description;
		private String path;
		
		public EggGoal(int index) {
			this.path = "easterevent."+year+".egg"+index;
			switch (index) {
			case 0:
				name = Utils.convertString(ChatColor.GREEN+Languages.getString("easter.greenEgg"));
				description = Utils.convertString(ChatColor.YELLOW+Languages.getString("easter.greenEggGoal"));
				cmd = 100023;
				break;
			case 1:
				name = Utils.convertString(ChatColor.BLUE+Languages.getString("easter.blueEgg"));
				description = Utils.convertString(ChatColor.YELLOW+Languages.getString("easter.blueEggGoal"));
				cmd = 100024;
				break;
			case 2:
				name = Utils.convertString(ChatColor.RED+Languages.getString("easter.redEgg"));
				description = Utils.convertString(ChatColor.YELLOW+Languages.getString("easter.redEggGoal"));
				cmd = 100025;
				break;
			case 3:
				name = Utils.convertString(ChatColor.GOLD+Languages.getString("easter.orangeEgg"));
				description = Utils.convertString(ChatColor.YELLOW+Languages.getString("easter.orangeEggGoal"));
				cmd = 100026;
				break;
			case 4:
				name = Utils.convertString(ChatColor.LIGHT_PURPLE+Languages.getString("easter.purpleEgg"));
				description = Utils.convertString(ChatColor.YELLOW+Languages.getString("easter.purpleEggGoal"));
				cmd = 100027;
				break;
			}
			if (plugin.dataFile.contains(path))
				for (String key : plugin.dataFile.getConfigurationSection(path).getKeys(false))
					progress.put(UUID.fromString(key), plugin.dataFile.getInt(path+'.'+key));
		}
		public void save() {
			for (Map.Entry<UUID, Integer> entry : progress.entrySet())
				plugin.dataFile.set(path+'.'+entry.getKey(), entry.getValue());
		}
		public void addProgress(UUID uuid, int amount) {
			if (hasAchieved(uuid))
				return;
			if (progress.containsKey(uuid))
				progress.replace(uuid, Math.min(goal, progress.get(uuid) + amount));
			else
				progress.put(uuid, Math.min(goal, amount));
			if (hasAchieved(uuid) && Bukkit.getPlayer(uuid) != null)
				Bukkit.getPlayer(uuid).sendMessage(Languages.prefix+Utils.convertString("&a"+Languages.getString("easter.discoverEgg")+" "+name+"!"));
		}
		public int getProgress(UUID uuid) {
			return progress.containsKey(uuid) ? progress.get(uuid) : 0;
		}
		public boolean hasAchieved(UUID uuid) {
			return (progress.containsKey(uuid) && progress.get(uuid) >= goal);
		}
	}
}
