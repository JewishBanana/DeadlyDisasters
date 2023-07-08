package deadlydisasters.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

import deadlydisasters.general.ItemsHandler;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;
import deadlydisasters.utils.Utils;

public class CraftingListener implements Listener {
	
	private Main plugin;
	
	public CraftingListener(Main plugin) {
		this.plugin = plugin;
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler
	public void onCraft(PrepareItemCraftEvent e) {
		if (e.getRecipe() == null || !e.getRecipe().getResult().hasItemMeta())
			return;
		ItemStack item = e.getRecipe().getResult();
		if (item.getItemMeta().getPersistentDataContainer().has(ItemsHandler.plagueCureKey, PersistentDataType.BYTE)) {
			PotionMeta meta = (PotionMeta) e.getInventory().getContents()[5].getItemMeta();
			if (meta.getBasePotionData().getType() != PotionType.AWKWARD)
				e.getInventory().setResult(new ItemStack(Material.AIR));
		} else if (plugin.customNameSupport && item.getItemMeta().getDisplayName().contains(ItemsHandler.ancientBladeName) || item.getItemMeta().getPersistentDataContainer().has(ItemsHandler.ancientBladeKey, PersistentDataType.BYTE)) {
			ItemStack[] items = e.getInventory().getContents();
			if (!(items[2].hasItemMeta() && items[2].getItemMeta().hasLore() && items[2].getItemMeta().getLore().get(0).equals(ItemsHandler.ancientBoneLore)
					&& items[4].hasItemMeta() && items[4].getItemMeta().hasLore() && items[4].getItemMeta().getLore().get(0).equals(ItemsHandler.ancientBoneLore)
					&& items[6].hasItemMeta() && items[6].getItemMeta().hasLore() && items[6].getItemMeta().getLore().get(0).equals(ItemsHandler.ancientBoneLore)
					&& items[7].hasItemMeta() && items[7].getItemMeta().hasLore() && items[7].getItemMeta().getLore().get(0).equals(ItemsHandler.ancientClothLore)
					&& items[9].hasItemMeta() && items[9].getItemMeta().hasLore() && items[9].getItemMeta().getLore().get(0).equals(ItemsHandler.ancientClothLore)))
				e.getInventory().setResult(new ItemStack(Material.AIR));
		} else if (item.getItemMeta().getPersistentDataContainer().has(ItemsHandler.snowGlobeKey, PersistentDataType.BYTE)) {
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
				if (it.hasItemMeta())
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
		}
	}
	@EventHandler
	public void onAnvil(PrepareAnvilEvent e) {
		if (e.getInventory().getItem(0) == null || e.getInventory().getItem(1) == null || !e.getInventory().getItem(1).hasItemMeta())
			return;
		ItemStack item = e.getInventory().getItem(0);
		ItemStack secondSlot = e.getInventory().getItem(1);
		if (secondSlot.getType() == Material.TURTLE_EGG && secondSlot.getItemMeta().getPersistentDataContainer().has(ItemsHandler.goldenEasterEggKey, PersistentDataType.BYTE) && secondSlot.getAmount() == 1
				&& plugin.getConfig().getBoolean("customitems.recipes.bunny_hop")) {
			Material[] boots = {Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS, Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS};
			if (plugin.mcVersion >= 1.16)
				boots = new Material[]{Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS, Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS};
			for (Material mat : boots)
				if (item.getType() == mat) {
					ItemStack result = item.clone();
					if (!Utils.upgradeEnchantLevel(result, ChatColor.GRAY+Languages.langFile.getString("easter.bunnyHop"), 1, ItemsHandler.bunnyHopKey)) {
						e.setResult(new ItemStack(Material.AIR));
						return;
					}
					ItemMeta meta = result.getItemMeta();
					if (!meta.hasCustomModelData()) {
						meta.setCustomModelData(mat == Material.IRON_BOOTS ? 100029 : mat == Material.CHAINMAIL_BOOTS ? 100030 : mat == Material.GOLDEN_BOOTS ? 100031 : mat == Material.DIAMOND_BOOTS ? 100032 : 100033);
						result.setItemMeta(meta);
					}
					e.setResult(result);
					return;
				}
		}
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
	public void onClick(InventoryClickEvent e) {
		if (e.getClickedInventory() == null || e.getClickedInventory().getType() != InventoryType.ANVIL || e.getSlot() != 2 || e.getClickedInventory().getItem(2) == null || e.getWhoClicked().getItemOnCursor().getType() != Material.AIR
			|| e.getClickedInventory().getItem(1) == null || !e.getClickedInventory().getItem(1).hasItemMeta() || !e.getClickedInventory().getItem(2).hasItemMeta())
			return;
		ItemStack item = e.getClickedInventory().getItem(1);
		ItemStack result = e.getClickedInventory().getItem(2);
		
		if (item.getItemMeta().getPersistentDataContainer().has(ItemsHandler.goldenEasterEggKey, PersistentDataType.BYTE)) {
			e.getWhoClicked().setItemOnCursor(result);
			((AnvilInventory) e.getClickedInventory()).setItem(2, new ItemStack(Material.AIR));
			for (ItemStack contents : e.getClickedInventory().getContents())
				contents.setAmount(0);
			Block block = e.getClickedInventory().getLocation().getBlock();
			String data = block.getBlockData().getAsString();
			switch (block.getType()) {
			case ANVIL:
				block.getWorld().playSound(block.getLocation(), Sound.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 1, 1);
				if (!Utils.isPlayerImmune((Player) e.getWhoClicked()))
					block.setBlockData(Bukkit.createBlockData(data.replace("anvil", "chipped_anvil")));
				break;
			case CHIPPED_ANVIL:
				block.getWorld().playSound(block.getLocation(), Sound.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 1, 1);
				if (!Utils.isPlayerImmune((Player) e.getWhoClicked()))
					block.setBlockData(Bukkit.createBlockData(data.replace("chipped_anvil", "damaged_anvil")));
				break;
			case DAMAGED_ANVIL:
				if (Utils.isPlayerImmune((Player) e.getWhoClicked()))
					block.getWorld().playSound(block.getLocation(), Sound.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 1, 1);
				else {
					block.getWorld().playSound(block.getLocation(), Sound.BLOCK_ANVIL_DESTROY, SoundCategory.BLOCKS, 1, 1);
					block.breakNaturally(new ItemStack(Material.AIR));
				}
				break;
				default:
			}
		}
	}
	@EventHandler
	public void onBrew(BrewEvent e) {
		if (e.isCancelled())
			return;
		ItemStack[] potions = {e.getContents().getStorageContents()[0], e.getContents().getStorageContents()[1], e.getContents().getStorageContents()[2]};
		for (int i=0; i < 3; i++) {
			ItemStack item = potions[i];
			if (item == null || !item.hasItemMeta() || !item.getItemMeta().getPersistentDataContainer().has(ItemsHandler.plagueCureKey, PersistentDataType.BYTE)) {
				potions[i] = null;
				continue;
			}
			if (e.getContents().getIngredient().getType() == Material.GUNPOWDER) {
				if (item.getType() == Material.POTION)
					e.getResults().set(i, ItemsHandler.plagueCureSplash);
			}
		}
	}
}
