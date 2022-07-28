package deadlydisasters.general;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import deadlydisasters.commands.Disasters;
import deadlydisasters.disasters.CustomDisaster;
import deadlydisasters.listeners.CoreListener;
import deadlydisasters.utils.Utils;

public class Catalog implements Listener {
	
	private Main plugin;
	private YamlConfiguration catalogFile;
	
	public Inventory featuredPage;
	private Inventory[] pages;
	private Inventory[] installed;
	
	private Map<Integer, InventoryItem> itemMap = new HashMap<>();
	private Map<Integer, InventoryItem> featuredList = new HashMap<>();
	private Map<Integer, Integer> installedList = new HashMap<>();
	
	public static Map<Integer, File> downloadedDisasters = new HashMap<>();
	
	public static List<String> catalogFileNames = new ArrayList<>();
	
	public static int catalogDownloadTimer;
	
	public Catalog(Main plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		
		if (plugin.dataFile.contains("data.catalogFetchCooldown"))
			catalogDownloadTimer = plugin.dataFile.getInt("data.catalogFetchCooldown");
		initCatalog();
		initInventory();
		CustomDisaster.loadFiles(plugin);
		plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				if (catalogDownloadTimer > 0) {
					catalogDownloadTimer--;
					return;
				}
				pullCatalog();
				initInventory();
				catalogDownloadTimer = 144000;
			}
		}, 0, 20);
	}
	public void pullCatalog() {
		File f = new File(plugin.getDataFolder().getAbsolutePath(), "pluginData/catalog.yml");
		try {
			Utils.copyUrlToFile(new URL("https://docs.google.com/uc?export=download&id=12yxVcLOBpkwiELjWPDAvDn5aUmCCtBfz"), f);
		}  catch (IOException e) {
			Main.consoleSender.sendMessage(Languages.prefix+Utils.chat("&eError could not fetch catalog! You can ignore this error unless the plugin is throwing actual errors.."));
		}
		initCatalog();
	}
	public void initCatalog() {
		File f = new File(plugin.getDataFolder().getAbsolutePath(), "pluginData/catalog.yml");
		try {
			if (!f.exists()) {
				f.getParentFile().mkdirs();
				f.createNewFile();
			}
			catalogFile = YamlConfiguration.loadConfiguration(f);
			if (!catalogFile.contains("version")) {
				FileUtils.copyInputStreamToFile(plugin.getResource("files/catalog.yml"), f);
				catalogFile = YamlConfiguration.loadConfiguration(f);
			}
			catalogFileNames.clear();
			for (String name : catalogFile.getConfigurationSection("disasters").getKeys(false))
				catalogFileNames.add(name);
			if (!(plugin.dataFile.contains("data.catalogVersion") && plugin.dataFile.getInt("data.catalogVersion") >= catalogFile.getInt("version")) && plugin.getConfig().getBoolean("general.catalog_notify")) {
				for (Player p : Bukkit.getOnlinePlayers())
					if (p.isOp())
						p.sendMessage(Languages.prefix+ChatColor.GREEN+Languages.langFile.getString("internal.catalogUpdate")+Utils.chat(" &3(/disasters catalog)"));
				plugin.dataFile.set("data.catalogVersion", catalogFile.getInt("version"));
				plugin.saveDataFile();
				CoreListener.catalogNotifyBool = true;
				CoreListener.catalogNotify.clear();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void initInventory() {
		int id = 0;
		itemMap.clear();
		
		featuredPage = Bukkit.createInventory(null, 54, Utils.chat("&3&lDeadlyDisasters Catalog"));
		
		for (String name : catalogFile.getConfigurationSection("disasters").getKeys(false)) {
			String path = "disasters."+name+'.';
			itemMap.put(id, new InventoryItem(path, catalogFile, plugin));
			id++;
		}
		
		featuredPage.setItem(4, createItem(Material.NETHER_STAR, "&6&lFeatured Disasters", Arrays.asList(Utils.chat("&bFeatured disasters made by the community!"))));
		featuredPage.setItem(47, createItem(Material.CHEST_MINECART, "&b&lInstalled Disasters", Arrays.asList(Utils.chat("&a-View and &cdelete &ainstalled disasters"))));
		featuredPage.setItem(49, createItem(Material.ENDER_CHEST, "&f&l[COMING SOON]", Arrays.asList(Utils.chat("&d&lChallenge Packs"), Utils.chat("&a-Play challenging packs of custom"), Utils.chat("&adisasters made by the community"))));
		featuredPage.setItem(51, createItem(Material.BOOK, "&6&lDisaster Browser", Arrays.asList(Utils.chat("&a-Browse community made custom disasters"), Utils.chat("&a-Install new custom disasters"))));
	
		refreshBrowserItems();
		refreshInstalled();
	}
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		if (e.getCurrentItem() == null)
			return;
		if (e.getInventory().equals(featuredPage)) {
			String name = e.getCurrentItem().getItemMeta().getDisplayName();
			e.setCancelled(true);
			if (name.contains("Disaster Browser")) {
				e.getWhoClicked().openInventory(pages[0]);
				return;
			} else if (name.contains("Installed Disasters")) {
				e.getWhoClicked().openInventory(installed[0]);
				return;
			} else if (name.contains("Featured Disasters") || name.contains("[COMING SOON]")) {
				//here update
				return;
			} else {
				int id = e.getRawSlot();
				if (id >= 10 && id <= 16)
					id -= 10;
				else if (id >= 19 && id <= 25)
					id -= 12;
				else
					id -= 14;
				id = featuredList.get(id).id;
				if (!downloadedDisasters.containsKey(id)) {
					InventoryItem invItem = itemMap.get(id);
					File f = new File(plugin.getDataFolder().getAbsolutePath(), "custom disasters/"+invItem.fileName+".yml");
					downloadItem(invItem, f);
					downloadedDisasters.put(id, f);
					invItem.installItem();
					refreshBrowserItems();
					refreshInstalled();
					e.getWhoClicked().openInventory(featuredPage);
				}
			}
		} else if (Arrays.stream(pages).anyMatch(e.getInventory()::equals)) {
			String name = e.getCurrentItem().getItemMeta().getDisplayName();
			e.setCancelled(true);
			if (name.contains("Page ") && e.getCurrentItem().getType() == Material.ARROW) {
				e.getWhoClicked().openInventory(pages[Integer.parseInt(name.substring(5))-1]);
				return;
			} else if (name.contains("Featured Page")) {
				e.getWhoClicked().openInventory(featuredPage);
				return;
			} else {
				for (int i=0; i < pages.length; i++)
					if (e.getInventory().equals(pages[i])) {
						int id = (i*36)+(e.getRawSlot()-9);
						if (!downloadedDisasters.containsKey(id)) {
							InventoryItem invItem = itemMap.get(id);
							File f = new File(plugin.getDataFolder().getAbsolutePath(), "custom disasters/"+invItem.fileName+".yml");
							downloadItem(invItem, f);
							downloadedDisasters.put(id, f);
							invItem.installItem();
							refreshBrowserItems();
							refreshInstalled();
							e.getWhoClicked().openInventory(pages[i]);
						}
					}
			}
		} else if (Arrays.stream(installed).anyMatch(e.getInventory()::equals)) {
			String name = e.getCurrentItem().getItemMeta().getDisplayName();
			e.setCancelled(true);
			if (name.contains("Featured Page")) {
				e.getWhoClicked().openInventory(featuredPage);
				return;
			} else if (name.contains("Page ") && e.getCurrentItem().getType() == Material.ARROW) {
				e.getWhoClicked().openInventory(installed[Integer.parseInt(name.substring(5))-1]);
				return;
			} else {
				for (int i=0; i < installed.length; i++)
					if (e.getInventory().equals(installed[i])) {
						int id = (i*36)+(e.getRawSlot()-9);
						InventoryItem item = itemMap.get(installedList.get(id));
						item.uninstallItem();
						if (downloadedDisasters.get(item.id).exists())
							downloadedDisasters.get(item.id).delete();
						downloadedDisasters.remove(item.id);
						Disasters.removeDisaster(item.fileName);
						CustomDisaster.disasterFiles.remove(item.fileName);
						refreshBrowserItems();
						refreshInstalled();
						if (i <= installed.length-1)
							e.getWhoClicked().openInventory(installed[i]);
						else
							e.getWhoClicked().openInventory(installed[i-1]);
						return;
					}
			}
		}
	}
	public void downloadItem(InventoryItem invItem, File f) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				try {
					if (!f.exists())
						f.createNewFile();
					Utils.copyUrlToFile(new URL(invItem.url), f);
					CustomDisaster.loadDisaster(f);
					Main.consoleSender.sendMessage(Languages.prefix+Utils.chat("&bDownloaded and installed &e'"+invItem.name+"&e'"));
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
	}
	public void refreshBrowserItems() {
		featuredList.clear();
		List<Integer> featuredItems = catalogFile.getIntegerList("featured");
		for (int i=0; i < Math.min(featuredItems.size(), 7); i++) {
			featuredPage.setItem(10+i, itemMap.get(featuredItems.get(i)).getItem());
			featuredList.put(i, itemMap.get(featuredItems.get(i)));
		}
		if (featuredItems.size() > 7)
			for (int i=7; i < Math.min(featuredItems.size(), 14); i++) {
				featuredPage.setItem(19+(i-7), itemMap.get(featuredItems.get(i)).getItem());
				featuredList.put(i, itemMap.get(featuredItems.get(i)));
			}
		if (featuredItems.size() > 14)
			for (int i=14; i < Math.min(featuredItems.size(), 21); i++) {
				featuredPage.setItem(28+(i-14), itemMap.get(featuredItems.get(i)).getItem());
				featuredList.put(i, itemMap.get(featuredItems.get(i)));
			}
		pages = new Inventory[Math.max((int) Math.ceil((double) itemMap.size() / 36.0), 1)];
		for (int i=0; i < pages.length; i++) {
			pages[i] = Bukkit.createInventory(null, 54, Utils.chat("&9&lDisaster Browser: Page "+(i+1)));
			int spot = 9;
			for (int c=(i*36); c < Math.min(itemMap.size(), (i*36)+36); c++) {
				pages[i].setItem(spot, itemMap.get(c).getItem());
				spot++;
			}
			if (i > 0)
				pages[i].setItem(45, createItem(Material.ARROW, "&aPage "+i, Arrays.asList("")));
			if (pages.length < i+1)
				pages[i].setItem(54, createItem(Material.ARROW, "&aPage "+i+2, Arrays.asList("")));
			pages[i].setItem(4, createItem(Material.NETHER_STAR, "&6&lFeatured Page", Arrays.asList(Utils.chat("&bCLICK to return to the featured page"))));
		}
	}
	public void refreshInstalled() {
		installedList.clear();
		installed = new Inventory[Math.max((int) Math.ceil((double) downloadedDisasters.size() / 36.0), 1)];
		List<Integer> keySet = new ArrayList<>(downloadedDisasters.keySet());
		for (int i=0; i < installed.length; i++) {
			installed[i] = Bukkit.createInventory(null, 54, Utils.chat("&9&lInstalled Page : "+(i+1)));
			int spot = 9;
			for (int c=(i*36); c < Math.min(downloadedDisasters.size(), (i*36)+36); c++) {
				installed[i].setItem(spot, itemMap.get(keySet.get(c)).getInstalledItem());
				installedList.put(c, keySet.get(c));
				spot++;
			}
			if (i > 0)
				installed[i].setItem(45, createItem(Material.ARROW, "&aPage "+i, Arrays.asList("")));
			if (installed.length < i+1)
				installed[i].setItem(54, createItem(Material.ARROW, "&aPage "+i+2, Arrays.asList("")));
			installed[i].setItem(4, createItem(Material.NETHER_STAR, "&6&lFeatured Page", Arrays.asList(Utils.chat("&bCLICK to return to the featured page"))));
		}
	}
	public ItemStack createItem(Material mat, String name, List<String> lore) {
		ItemStack item = new ItemStack(mat);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(Utils.chat(name));
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}
	public static void saveTimer(Main plugin) {
		plugin.dataFile.set("data.catalogFetchCooldown", catalogDownloadTimer);
	}
}
class InventoryItem {
	public ItemStack item;
	public double gameVersion;
	public double pluginVersion;
	public boolean downloadable = true;
	public String name;
	public int id;
	public String url;
	public String fileName;
	
	public InventoryItem(String path, YamlConfiguration yaml, Main plugin) {
		if (yaml.contains(path+"id"))
			id = yaml.getInt(path+"id");
		name = yaml.getString(path+"name");
		url = yaml.getString(path+"link");
		fileName = yaml.getString(path+"file");
		item = new ItemStack(Material.valueOf(yaml.getString(path+"item").toUpperCase()));
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(Utils.chat(yaml.getString(path+"name")+" &8| Version: "+yaml.getString(path+"version")));
		List<String> lore = new ArrayList<>();
		lore.add(Utils.chat(yaml.getString(path+"description")));
		lore.add(Utils.chat("&8Author: "+yaml.getString(path+"author")));
		lore.add(" ");
		if (yaml.getDouble(path+"game_version") <= plugin.mcVersion)
			lore.add(Utils.chat("&7MC Version: &a"+yaml.getDouble(path+"game_version")));
		else {
			lore.add(Utils.chat("&7MC Version: &c"+yaml.getDouble(path+"game_version")));
			downloadable = false;
		}
		if (yaml.getDouble(path+"plugin_version") <= Double.parseDouble(plugin.getDescription().getVersion()))
			lore.add(Utils.chat("&7Plugin Version: &a"+yaml.getDouble(path+"plugin_version")));
		else {
			lore.add(Utils.chat("&7Plugin Version: &c"+yaml.getDouble(path+"plugin_version")));
			downloadable = false;
		}
		if (downloadable) {
			if (Catalog.downloadedDisasters.containsKey(id)) {
				lore.add(Utils.chat("&bInstalled!"));
				meta.addEnchant(Enchantment.DURABILITY, 1, false);
				meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			} else
				lore.add(Utils.chat("&aCLICK to download and install!!"));
		}
		meta.setLore(lore);
		item.setItemMeta(meta);
	}
	public ItemStack getItem() {
		return item;
	}
	public ItemStack getInstalledItem() {
		ItemStack newItem = item.clone();
		ItemMeta meta = newItem.getItemMeta();
		List<String> lore = meta.getLore();
		lore.remove(lore.get(lore.size()-1));
		lore.add(Utils.chat("&cCLICK to delete"));
		meta.setLore(lore);
		newItem.setItemMeta(meta);
		return newItem;
	}
	public void uninstallItem() {
		ItemMeta meta = item.getItemMeta();
		meta.removeEnchant(Enchantment.DURABILITY);
		List<String> lore = meta.getLore();
		lore.remove(lore.get(lore.size()-1));
		lore.add(Utils.chat("&aCLICK to download and install!!"));
		meta.setLore(lore);
		item.setItemMeta(meta);
	}
	public void installItem() {
		ItemMeta meta = item.getItemMeta();
		meta.addEnchant(Enchantment.DURABILITY, 1, false);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		List<String> lore = meta.getLore();
		lore.remove(lore.get(lore.size()-1));
		lore.add(Utils.chat("&bInstalled!"));
		meta.setLore(lore);
		item.setItemMeta(meta);
	}
}
