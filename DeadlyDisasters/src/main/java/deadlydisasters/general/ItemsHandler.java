package deadlydisasters.general;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import deadlydisasters.utils.Utils;

public class ItemsHandler {
	
	public static Map<String, ItemStack> allItems = new HashMap<>();
	
	public static ItemStack voidshard = new ItemStack(Material.GHAST_TEAR);
	public static String voidShardName;
	
	public static ItemStack voidsedge = new ItemStack(Material.IRON_SWORD);
	public static double voidsedgeDroprate;
	public static ItemStack voidshield = new ItemStack(Material.SHIELD);
	
	public static ItemStack voidswrath = new ItemStack(Material.BOW);
	public static String voidBowName;
	public static int voidBowCooldown = 10;
	public static int voidBowPortalTicks = 80;
	
	public static ItemStack ancientblade;
	public static String ancientBladeName;
	public static String ancientCurseName;
	private static NamespacedKey ancientBladeRecipe;
	public static int ancientBladeCooldown = 8;
	
	public static ItemStack plagueCure = new ItemStack(Material.POTION);
	public static ItemStack plagueCureSplash = new ItemStack(Material.SPLASH_POTION);
	public static String plagueCureName;
	public static String plagueCureLore;
	private static NamespacedKey plagueCureRecipe;
	private static NamespacedKey plagueCureRecipe2;
	public static NamespacedKey plagueCureKey;
	
	public static ItemStack ancientbone = new ItemStack(Material.BONE);
	public static String ancientBoneLore;
	
	public static ItemStack ancientcloth = new ItemStack(Material.PAPER);
	public static String ancientClothLore;
	
	public static ItemStack mageWand = new ItemStack(Material.BLAZE_ROD);
	public static String mageWandLore;
	public static int mageWandCooldown = 10;
	
	public static ItemStack soulRipper = new ItemStack(Material.IRON_HOE);
	public static String soulRipperLore;
	public static int soulRipperCooldown = 25;
	public static int soulRipperNumberOfSouls = 3;
	public static int soulRipperSoulLifeTicks = 260;
	
	public static void refreshMetas(Main plugin) {
		allItems.clear();
		String craftables = Utils.chat("&7&o"+Languages.langFile.getString("misc.craftable"));
		
		//voidshard
		ItemMeta meta = voidshard.getItemMeta();
		meta.addEnchant(Enchantment.DURABILITY, 1, true);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.setDisplayName(ChatColor.LIGHT_PURPLE+Languages.langFile.getString("items.voidShard"));
		meta.setLore(Arrays.asList(Languages.langFile.getString("items.voidShardLore.line 1"), Utils.chat("&b"+Languages.langFile.getString("items.voidShardLore.line 2"))));
		voidshard.setItemMeta(meta);
		voidShardName = ChatColor.LIGHT_PURPLE+Languages.langFile.getString("items.voidShard");
		allItems.put("voidshard", voidshard);
		
		//voidguards
		meta = voidsedge.getItemMeta();
		meta.addEnchant(Enchantment.DAMAGE_ALL, 2, false);
		meta.setDisplayName(ChatColor.LIGHT_PURPLE+Languages.langFile.getString("items.voidEdge"));
		meta.setLore(Arrays.asList(Languages.langFile.getString("items.voidEdgeLore")));
		voidsedge.setItemMeta(meta);
		allItems.put("voidsedge", voidsedge);
		
		meta = voidshield.getItemMeta();
		meta.addEnchant(Enchantment.DURABILITY, 2, false);
		meta.setDisplayName(ChatColor.LIGHT_PURPLE+Languages.langFile.getString("items.voidShield"));
		meta.setLore(Arrays.asList(Languages.langFile.getString("items.voidShieldLore")));
		voidshield.setItemMeta(meta);
		allItems.put("voidshield", voidshield);
		
		//void wrath
		meta = voidswrath.getItemMeta();
		meta.addEnchant(Enchantment.ARROW_DAMAGE, 2, false);
		meta.setDisplayName(ChatColor.LIGHT_PURPLE+Languages.langFile.getString("items.voidWrath"));
		meta.setLore(Arrays.asList(Languages.langFile.getString("items.voidWrathLore")));
		voidswrath.setItemMeta(meta);
		voidBowName = ChatColor.LIGHT_PURPLE+Languages.langFile.getString("items.voidWrath");
		allItems.put("voidswrath", voidswrath);
		
		//ancient blade
		if (plugin.mcVersion >= 1.16)
			ancientblade = new ItemStack(Material.NETHERITE_SWORD);
		else
			ancientblade = new ItemStack(Material.DIAMOND_SWORD);
		meta = ancientblade.getItemMeta();
		meta.setDisplayName(ChatColor.GOLD+Languages.langFile.getString("items.ancientBlade"));
		meta.setLore(Arrays.asList(ChatColor.GRAY+Languages.langFile.getString("misc.ancientCurse"), " ", ChatColor.YELLOW+Languages.langFile.getString("items.ancientBladeLore")));
		meta.addEnchant(Enchantment.DAMAGE_ALL, 2, false);
		ancientblade.setItemMeta(meta);
		ancientBladeName = ChatColor.LIGHT_PURPLE+Languages.langFile.getString("items.ancientBlade");
		ancientCurseName = ChatColor.GRAY+Languages.langFile.getString("misc.ancientCurse");
		allItems.put("ancientblade", ancientblade);
		
		//plague cure
		PotionMeta potionMeta = (PotionMeta) plagueCure.getItemMeta();
		potionMeta.setDisplayName(Languages.langFile.getString("items.plagueCure"));
		potionMeta.setLore(Arrays.asList(Languages.langFile.getString("items.plagueCureLore")));
		potionMeta.setColor(Color.BLACK);
		potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.HEAL, 1, 1, true, false, false), false);
		potionMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		potionMeta.setBasePotionData(new PotionData(PotionType.AWKWARD));
		plagueCureKey = new NamespacedKey(plugin, "dd-plagueCureKey");
		potionMeta.getPersistentDataContainer().set(plagueCureKey, PersistentDataType.BYTE, (byte) 1);
		plagueCure.setItemMeta(potionMeta);
		plagueCureSplash.setItemMeta(potionMeta);
		plagueCureName = Languages.langFile.getString("items.plagueCure");
		plagueCureLore = Languages.langFile.getString("items.plagueCureLore");
		allItems.put("plaguecure", plagueCure);
		
		//ancient bone
		meta = ancientbone.getItemMeta();
		meta.addEnchant(Enchantment.DURABILITY, 1, false);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.setDisplayName(ChatColor.GOLD+Languages.langFile.getString("items.ancientBone"));
		meta.setLore(Arrays.asList(ChatColor.YELLOW+Languages.langFile.getString("items.ancientBoneLore"), craftables));
		ancientbone.setItemMeta(meta);
		ancientBoneLore = ChatColor.YELLOW+Languages.langFile.getString("items.ancientBoneLore");
		allItems.put("ancientbone", ancientbone);
		
		//ancient cloth
		meta = ancientcloth.getItemMeta();
		meta.addEnchant(Enchantment.DURABILITY, 1, false);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.setDisplayName(ChatColor.GOLD+Languages.langFile.getString("items.ancientCloth"));
		meta.setLore(Arrays.asList(ChatColor.YELLOW+Languages.langFile.getString("items.ancientClothLore"), craftables));
		ancientcloth.setItemMeta(meta);
		ancientClothLore = ChatColor.YELLOW+Languages.langFile.getString("items.ancientClothLore");
		allItems.put("ancientcloth", ancientcloth);
		
		//mage wand
		meta = mageWand.getItemMeta();
		meta.addEnchant(Enchantment.DURABILITY, 1, false);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.setDisplayName(ChatColor.GRAY+Languages.langFile.getString("items.mageWand"));
		meta.setLore(Arrays.asList(ChatColor.YELLOW+Languages.langFile.getString("items.mageWandLore")));
		mageWand.setItemMeta(meta);
		mageWandLore = ChatColor.YELLOW+Languages.langFile.getString("items.mageWandLore");
		allItems.put("magewand", mageWand);
		
		//soul ripper
		meta = soulRipper.getItemMeta();
		meta.addEnchant(Enchantment.DURABILITY, 1, false);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.setDisplayName(ChatColor.GRAY+Languages.langFile.getString("items.soulRipper"));
		meta.setLore(Arrays.asList(Languages.langFile.getString("items.soulRipperLore")));
		soulRipper.setItemMeta(meta);
		soulRipperLore = Languages.langFile.getString("items.soulRipperLore");
		allItems.put("soulripper", soulRipper);
	}
	public static void createRecipes(Main plugin) {
		if (plugin.mcVersion < 1.16) {
			Main.consoleSender.sendMessage(Languages.prefix+Utils.chat("&eWARNING old version detected ( < 1.16) All custom crafting recipes are disabled, custom crafting recipe support is only for 1.16+"));
			return;
		}
		// plague cure
		if (plugin.getConfig().getBoolean("customitems.recipes.plague_cure")) {
			if (plagueCureRecipe == null || plugin.getServer().getRecipe(plagueCureRecipe) == null) {
				plagueCureRecipe = new NamespacedKey(plugin, "plague_cure");
				ShapedRecipe sr = new ShapedRecipe(plagueCureRecipe, plagueCure);
				sr.shape(" A ", "ABA", " A ");
				sr.setIngredient('A', Material.INK_SAC);
				sr.setIngredient('B', Material.POTION);

				plugin.getServer().addRecipe(sr);
			}
			if (plugin.mcVersion >= 1.17 && (plagueCureRecipe2 == null || plugin.getServer().getRecipe(plagueCureRecipe2) == null)) {
				plagueCureRecipe2 = new NamespacedKey(plugin, "plague_cure2");
				ShapedRecipe sr2 = new ShapedRecipe(plagueCureRecipe2, plagueCure);
				sr2.shape(" A ","ABA"," A ");
				sr2.setIngredient('A', Material.GLOW_INK_SAC);
				sr2.setIngredient('B', Material.POTION);
				
				plugin.getServer().addRecipe(sr2);
			}
		} else {
			if (plagueCureRecipe != null && plugin.getServer().getRecipe(plagueCureRecipe) != null)
				plugin.getServer().removeRecipe(plagueCureRecipe);
			plagueCureRecipe = null;
			if (plagueCureRecipe2 != null && plugin.getServer().getRecipe(plagueCureRecipe2) != null)
				plugin.getServer().removeRecipe(plagueCureRecipe2);
			plagueCureRecipe2 = null;
		}
		
		//ancient blade
		if (plugin.getConfig().getBoolean("customitems.recipes.ancient_blade")) {
			if (ancientBladeRecipe == null || plugin.getServer().getRecipe(ancientBladeRecipe) == null) {
				ancientBladeRecipe = new NamespacedKey(plugin, "ancient_blade");
				ShapedRecipe sr = new ShapedRecipe(ancientBladeRecipe, ancientblade);
				sr.shape(" A ", "ABA", "CDC");
				sr.setIngredient('A', Material.BONE);
				sr.setIngredient('B', Material.NETHER_STAR);
				sr.setIngredient('C', Material.PAPER);
				sr.setIngredient('D', Material.NETHERITE_SWORD);

				plugin.getServer().addRecipe(sr);
			}
		} else {
			if (ancientBladeRecipe != null && plugin.getServer().getRecipe(ancientBladeRecipe) != null)
				plugin.getServer().removeRecipe(ancientBladeRecipe);
			ancientBladeRecipe = null;
		}
	}
	public static void reload(Main plugin) {
		refreshMetas(plugin);
		createRecipes(plugin);
	}
}
