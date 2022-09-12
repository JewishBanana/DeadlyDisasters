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
	public static double voidShardDroprate;
	
	public static ItemStack voidsedge = new ItemStack(Material.IRON_SWORD);
	public static double voidsedgeDroprate;
	public static ItemStack voidshield = new ItemStack(Material.SHIELD);
	public static double voidshieldDroprate;
	
	public static ItemStack voidswrath = new ItemStack(Material.BOW);
	public static String voidBowName;
	public static double voidBowDroprate;
	public static int voidBowCooldown;
	public static int voidBowPortalTicks;
	
	public static ItemStack ancientblade;
	public static String ancientBladeName;
	public static String ancientCurseName;
	private static NamespacedKey ancientBladeRecipe;
	public static int ancientBladeCooldown;
	
	public static ItemStack plagueCure = new ItemStack(Material.POTION);
	public static ItemStack plagueCureSplash = new ItemStack(Material.SPLASH_POTION);
	public static String plagueCureName;
	public static String plagueCureLore;
	private static NamespacedKey plagueCureRecipe;
	private static NamespacedKey plagueCureRecipe2;
	public static NamespacedKey plagueCureKey;
	
	public static ItemStack ancientbone = new ItemStack(Material.BONE);
	public static String ancientBoneLore;
	public static double ancientBoneDroprate;
	
	public static ItemStack ancientcloth = new ItemStack(Material.PAPER);
	public static String ancientClothLore;
	public static double ancientClothDroprate;
	
	public static ItemStack mageWand = new ItemStack(Material.BLAZE_ROD);
	public static String mageWandLore;
	public static double mageWandDroprate;
	public static int mageWandCooldown;
	
	public static ItemStack soulRipper = new ItemStack(Material.IRON_HOE);
	public static String soulRipperLore;
	public static double soulRipperDroprate;
	public static int soulRipperCooldown;
	public static int soulRipperNumberOfSouls;
	public static int soulRipperSoulLifeTicks;
	
	public static ItemStack yetifur = new ItemStack(Material.WHITE_DYE);
	public static String yetiFurLore;
	public static double yetiFurDroprate;
	
	public static ItemStack basicBook = new ItemStack(Material.ENCHANTED_BOOK);
	public static String basicBookLore;
	public static double basicBookDroprate;
	public static NamespacedKey basicCoatingKey;
	
	public static ItemStack poseidonsTrident = new ItemStack(Material.TRIDENT);
	public static String poseidonsTridentLore;
	public static double poseidonsTridentDroprate;
	public static int poseidonsTridentCooldown;
	public static NamespacedKey poseidonsTridentKey;
	
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
		voidShardDroprate = plugin.getConfig().getDouble("customitems.items.void_shard.droprate");
		allItems.put("voidshard", voidshard);
		
		//voidguards
		meta = voidsedge.getItemMeta();
		meta.addEnchant(Enchantment.DAMAGE_ALL, 2, false);
		meta.setDisplayName(ChatColor.LIGHT_PURPLE+Languages.langFile.getString("items.voidEdge"));
		meta.setLore(Arrays.asList(Languages.langFile.getString("items.voidEdgeLore")));
		voidsedge.setItemMeta(meta);
		voidsedgeDroprate = plugin.getConfig().getDouble("customitems.items.voids_edge.droprate");
		allItems.put("voidsedge", voidsedge);
		
		meta = voidshield.getItemMeta();
		meta.addEnchant(Enchantment.DURABILITY, 2, false);
		meta.setDisplayName(ChatColor.LIGHT_PURPLE+Languages.langFile.getString("items.voidShield"));
		meta.setLore(Arrays.asList(Languages.langFile.getString("items.voidShieldLore")));
		voidshield.setItemMeta(meta);
		voidshieldDroprate = plugin.getConfig().getDouble("customitems.items.void_shield.droprate");
		allItems.put("voidshield", voidshield);
		
		//void wrath
		meta = voidswrath.getItemMeta();
		meta.addEnchant(Enchantment.ARROW_DAMAGE, 2, false);
		meta.setDisplayName(ChatColor.LIGHT_PURPLE+Languages.langFile.getString("items.voidWrath"));
		meta.setLore(Arrays.asList(Languages.langFile.getString("items.voidWrathLore")));
		voidswrath.setItemMeta(meta);
		voidBowName = ChatColor.LIGHT_PURPLE+Languages.langFile.getString("items.voidWrath");
		voidBowDroprate = plugin.getConfig().getDouble("customitems.items.void_wrath.droprate");
		voidBowCooldown = plugin.getConfig().getInt("customitems.items.void_wrath.ability_cooldown");
		voidBowPortalTicks = plugin.getConfig().getInt("customitems.items.void_wrath.portal_ticks");
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
		ancientBladeCooldown = plugin.getConfig().getInt("customitems.items.ancient_blade.ability_cooldown");
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
		ancientBoneDroprate = plugin.getConfig().getDouble("customitems.items.ancient_bone.droprate");
		allItems.put("ancientbone", ancientbone);
		
		//ancient cloth
		meta = ancientcloth.getItemMeta();
		meta.addEnchant(Enchantment.DURABILITY, 1, false);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.setDisplayName(ChatColor.GOLD+Languages.langFile.getString("items.ancientCloth"));
		meta.setLore(Arrays.asList(ChatColor.YELLOW+Languages.langFile.getString("items.ancientClothLore"), craftables));
		ancientcloth.setItemMeta(meta);
		ancientClothLore = ChatColor.YELLOW+Languages.langFile.getString("items.ancientClothLore");
		ancientClothDroprate = plugin.getConfig().getDouble("customitems.items.ancient_cloth.droprate");
		allItems.put("ancientcloth", ancientcloth);
		
		//mage wand
		meta = mageWand.getItemMeta();
		meta.addEnchant(Enchantment.DURABILITY, 1, false);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.setDisplayName(ChatColor.GRAY+Languages.langFile.getString("items.mageWand"));
		meta.setLore(Arrays.asList(ChatColor.YELLOW+Languages.langFile.getString("items.mageWandLore")));
		mageWand.setItemMeta(meta);
		mageWandLore = ChatColor.YELLOW+Languages.langFile.getString("items.mageWandLore");
		mageWandDroprate = plugin.getConfig().getDouble("customitems.items.dark_mage_wand.droprate");
		mageWandCooldown = plugin.getConfig().getInt("customitems.items.dark_mage_wand.ability_cooldown");
		allItems.put("magewand", mageWand);
		
		//soul ripper
		meta = soulRipper.getItemMeta();
		meta.addEnchant(Enchantment.DURABILITY, 1, false);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.setDisplayName(ChatColor.GRAY+Languages.langFile.getString("items.soulRipper"));
		meta.setLore(Arrays.asList(Languages.langFile.getString("items.soulRipperLore")));
		soulRipper.setItemMeta(meta);
		soulRipperLore = Languages.langFile.getString("items.soulRipperLore");
		soulRipperDroprate = plugin.getConfig().getDouble("customitems.items.soul_ripper.droprate");
		soulRipperCooldown = plugin.getConfig().getInt("customitems.items.soul_ripper.ability_cooldown");
		soulRipperNumberOfSouls = plugin.getConfig().getInt("customitems.items.soul_ripper.spawned_souls");
		soulRipperSoulLifeTicks = plugin.getConfig().getInt("customitems.items.soul_ripper.souls_life_ticks");
		allItems.put("soulripper", soulRipper);

		//yeti fur
		meta = yetifur.getItemMeta();
		meta.addEnchant(Enchantment.DURABILITY, 1, false);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.setDisplayName(ChatColor.BLUE + Languages.langFile.getString("items.yetiFur"));
		meta.setLore(Arrays.asList(ChatColor.YELLOW + Languages.langFile.getString("items.yetiFurLore"), craftables));
		yetifur.setItemMeta(meta);
		yetiFurLore = ChatColor.YELLOW + Languages.langFile.getString("items.yetiFurLore");
		yetiFurDroprate = plugin.getConfig().getDouble("customitems.items.yeti_fur.droprate");
		allItems.put("yetifur", yetifur);
		
		//basic book
		meta = basicBook.getItemMeta();
		meta.setLore(Arrays.asList(ChatColor.GRAY + Languages.langFile.getString("misc.basicCoating")));
		basicCoatingKey = new NamespacedKey(plugin, "dd-basicEnch");
		meta.getPersistentDataContainer().set(basicCoatingKey, PersistentDataType.BYTE, (byte) 1);
		basicBook.setItemMeta(meta);
		basicBookLore = ChatColor.GRAY + Languages.langFile.getString("misc.basicCoating");
		basicBookDroprate = plugin.getConfig().getDouble("customitems.items.basic_coating_book.chest_spawn_rate");
		allItems.put("basicbook", basicBook);
		
		//poseidons trident
		meta = poseidonsTrident.getItemMeta();
		meta.addEnchant(Enchantment.LOYALTY, 3, false);
		meta.addEnchant(Enchantment.IMPALING, 5, false);
		meta.setDisplayName(ChatColor.AQUA+Languages.langFile.getString("items.poseidonsTrident"));
		meta.setLore(Arrays.asList(ChatColor.GRAY+Languages.langFile.getString("misc.tidalWave")+" I", " ", Languages.langFile.getString("items.poseidonsTridentLore")));
		poseidonsTridentKey = new NamespacedKey(plugin, "dd-pTrident");
		meta.getPersistentDataContainer().set(poseidonsTridentKey, PersistentDataType.BYTE, (byte) 1);
		poseidonsTrident.setItemMeta(meta);
		poseidonsTridentLore = Languages.langFile.getString("items.poseidonsTridentLore");
		poseidonsTridentDroprate = plugin.getConfig().getDouble("customitems.items.poseidons_trident.droprate");
		poseidonsTridentCooldown = plugin.getConfig().getInt("customitems.items.poseidons_trident.ability_cooldown");
		allItems.put("poseidonstrident", poseidonsTrident);
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
