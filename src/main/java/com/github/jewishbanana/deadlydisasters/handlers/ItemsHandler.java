package com.github.jewishbanana.deadlydisasters.handlers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomHead;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

@SuppressWarnings("deprecation")
public class ItemsHandler {
	
	public static Map<String, ItemStack> allItems = new HashMap<>();
	
	public static ItemStack voidshard = new ItemStack(Material.GHAST_TEAR);
	public static String voidShardName;
	public static NamespacedKey voidShardKey;
	
	public static ItemStack voidsedge = new ItemStack(Material.IRON_SWORD);
	public static NamespacedKey voidsEdgeKey;
	
	public static ItemStack voidshield = new ItemStack(Material.SHIELD);
	public static NamespacedKey voidShieldKey;
	
	public static ItemStack voidswrath = new ItemStack(Material.BOW);
	public static String voidBowName;
	public static int voidBowCooldown;
	public static int voidBowPortalTicks;
	public static NamespacedKey voidBowKey;
	
	public static ItemStack ancientblade;
	public static String ancientBladeName;
	public static String ancientCurseName;
	private static NamespacedKey ancientBladeRecipe;
	public static int ancientBladeCooldown;
	public static NamespacedKey ancientBladeKey;
	
	public static ItemStack plagueCure = new ItemStack(Material.POTION);
	public static ItemStack plagueCureSplash = new ItemStack(Material.SPLASH_POTION);
	public static String plagueCureName;
	public static String plagueCureLore;
	private static NamespacedKey plagueCureRecipe;
	private static NamespacedKey plagueCureRecipe2;
	public static NamespacedKey plagueCureKey;
	
	public static ItemStack ancientbone = new ItemStack(Material.BONE);
	public static String ancientBoneLore;
	public static NamespacedKey ancientBoneKey;
	
	public static ItemStack ancientcloth = new ItemStack(Material.PAPER);
	public static String ancientClothLore;
	public static NamespacedKey ancientClothKey;
	
	public static ItemStack mageWand = new ItemStack(Material.BLAZE_ROD);
	public static String mageWandLore;
	public static int mageWandCooldown;
	public static NamespacedKey mageWandKey;
	
	public static ItemStack soulRipper = new ItemStack(Material.IRON_HOE);
	public static String soulRipperLore;
	public static int soulRipperCooldown;
	public static int soulRipperNumberOfSouls;
	public static int soulRipperSoulLifeTicks;
	public static NamespacedKey soulRipperKey;
	
	public static ItemStack yetifur = new ItemStack(Material.WHITE_DYE);
	public static String yetiFurLore;
	public static NamespacedKey yetiFurKey;
	
	public static ItemStack basicBook = new ItemStack(Material.ENCHANTED_BOOK);
	public static String basicBookLore;
	public static double basicBookSpawnrate;
	public static NamespacedKey basicCoatingKey;
	
	public static ItemStack poseidonsTrident = new ItemStack(Material.TRIDENT);
	public static String poseidonsTridentLore;
	public static int poseidonsTridentCooldown;
	public static NamespacedKey poseidonsTridentKey;
	
	public static ItemStack bloodPact;
	public static String bloodPactLore;
	public static int bloodPactCooldown;
	public static double bloodPactHealthTake = 50.0;
	public static double bloodPactDamage = 4.0;
	public static NamespacedKey bloodPactKey;
	
	public static ItemStack bloodIngot;
	public static NamespacedKey bloodIngotKey;
	
	public static ItemStack candyCane;
	public static int candyCaneCooldown;
	public static NamespacedKey candyCaneKey;
	
	public static ItemStack cursedCandyCane;
	public static int cursedCandyCaneCooldown;
	public static NamespacedKey cursedCandyCaneKey;
	
	public static ItemStack ornament;
	public static NamespacedKey ornamentKey;
	
	public static ItemStack brokenSnowGlobe;
	public static NamespacedKey brokenSnowGlobeKey;
	
	public static ItemStack snowGlobe;
	private static NamespacedKey snowGlobeRecipe;
	public static NamespacedKey snowGlobeKey;
	
	public static ItemStack santaHat;
	public static int santaHatCooldown;
	public static NamespacedKey santaHatKey;
	
	public static ItemStack greenEasterEgg;
	public static NamespacedKey greenEasterEggKey;
	
	public static ItemStack blueEasterEgg;
	public static NamespacedKey blueEasterEggKey;
	
	public static ItemStack redEasterEgg;
	public static NamespacedKey redEasterEggKey;
	
	public static ItemStack orangeEasterEgg;
	public static NamespacedKey orangeEasterEggKey;
	
	public static ItemStack purpleEasterEgg;
	public static NamespacedKey purpleEasterEggKey;
	
	public static ItemStack goldenEasterEgg;
	public static NamespacedKey goldenEasterEggKey;
	
	public static ItemStack easterBasket;
	private static NamespacedKey easterBasketRecipe;
	public static NamespacedKey easterBasketKey;
	
	public static NamespacedKey bunnyHopKey;
	
	public static ItemStack cursedFlesh;
	public static NamespacedKey cursedFleshKey;
	
	public static ItemStack vampireFang;
	public static NamespacedKey vampireFangKey;
	
	public static ItemStack candyCorn;
	public static NamespacedKey candyCornKey;
	
	public static ItemStack spookyPumpkin;
	public static NamespacedKey spookyPumpkinKey;
	
	public static ItemStack pumpkinBasket;
	private static NamespacedKey pumpkinBasketRecipe;
	public static NamespacedKey pumpkinBasketKey;
	
	public static ItemStack etherealLantern;
	public static int etherealLanternCooldown;
	public static double etherealLanternChance;
	public static NamespacedKey etherealLanternKey;
	
	public static ItemStack etherealLanternBoss;
	public static NamespacedKey etherealLanternBossKey;
	
	public static ItemStack etherealLanternBoss2;
	public static NamespacedKey etherealLanternBoss2Key;
	
	public static void refreshMetas(Main plugin) {
		allItems.clear();
		String craftables = Utils.chat("&7&o"+Languages.getString("misc.craftable"));
		ItemMeta meta = null;
		
		//voidshard
		try {
			meta = voidshard.getItemMeta();
			meta.addEnchant(Enchantment.DURABILITY, 1, true);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			meta.setDisplayName(ChatColor.LIGHT_PURPLE+Languages.getString("items.voidShard"));
			meta.setLore(Arrays.asList(Languages.getString("items.voidShardLore.line 1"), Utils.chat("&b"+Languages.getString("items.voidShardLore.line 2"))));
			meta.setCustomModelData(100001);
			voidShardKey = new NamespacedKey(plugin, "dd-voidShardKey");
			meta.getPersistentDataContainer().set(voidShardKey, PersistentDataType.BYTE, (byte) 1);
			voidshard.setItemMeta(meta);
			voidShardName = ChatColor.LIGHT_PURPLE+Languages.getString("items.voidShard");
			allItems.put("voidshard", voidshard);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'voidshard' &eplease report this bug to the discord along with the error above.");
		}
		
		//voids edge
		try {
			meta = voidsedge.getItemMeta();
			meta.addEnchant(Enchantment.DAMAGE_ALL, 2, false);
			meta.setDisplayName(ChatColor.LIGHT_PURPLE+Languages.getString("items.voidEdge"));
			meta.setLore(Arrays.asList(Languages.getString("items.voidEdgeLore")));
			meta.setCustomModelData(100002);
			voidsEdgeKey = new NamespacedKey(plugin, "dd-voidsEdgeKey");
			meta.getPersistentDataContainer().set(voidsEdgeKey, PersistentDataType.BYTE, (byte) 1);
			voidsedge.setItemMeta(meta);
			allItems.put("voidsedge", voidsedge);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'voidsedge' &eplease report this bug to the discord along with the error above.");
		}
		
		//void shield
		try {
			meta = voidshield.getItemMeta();
			meta.addEnchant(Enchantment.DURABILITY, 2, false);
			meta.setDisplayName(ChatColor.LIGHT_PURPLE+Languages.getString("items.voidShield"));
			meta.setLore(Arrays.asList(Languages.getString("items.voidShieldLore")));
			meta.setCustomModelData(100003);
			voidShieldKey = new NamespacedKey(plugin, "dd-voidShieldKey");
			meta.getPersistentDataContainer().set(voidShieldKey, PersistentDataType.BYTE, (byte) 1);
			voidshield.setItemMeta(meta);
			allItems.put("voidshield", voidshield);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'voidshield' &eplease report this bug to the discord along with the error above.");
		}
		
		//void wrath
		try {
			meta = voidswrath.getItemMeta();
			meta.addEnchant(Enchantment.ARROW_DAMAGE, 2, false);
			meta.setDisplayName(ChatColor.LIGHT_PURPLE+Languages.getString("items.voidWrath"));
			meta.setLore(Arrays.asList(Languages.getString("items.voidWrathLore")));
			meta.setCustomModelData(100004);
			voidBowKey = new NamespacedKey(plugin, "dd-voidBowKey");
			meta.getPersistentDataContainer().set(voidBowKey, PersistentDataType.BYTE, (byte) 1);
			voidswrath.setItemMeta(meta);
			voidBowName = ChatColor.LIGHT_PURPLE+Languages.getString("items.voidWrath");
			voidBowCooldown = plugin.getConfig().getInt("customitems.items.void_wrath.ability_cooldown");
			voidBowPortalTicks = plugin.getConfig().getInt("customitems.items.void_wrath.portal_ticks");
			allItems.put("voidswrath", voidswrath);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'voidswrath' &eplease report this bug to the discord along with the error above.");
		}
		
		//ancient blade
		try {
			if (plugin.mcVersion >= 1.16)
				ancientblade = new ItemStack(Material.NETHERITE_SWORD);
			else
				ancientblade = new ItemStack(Material.DIAMOND_SWORD);
			meta = ancientblade.getItemMeta();
			meta.setDisplayName(ChatColor.GOLD+Languages.getString("items.ancientBlade"));
			meta.setLore(Arrays.asList(ChatColor.GRAY+Languages.getString("misc.ancientCurse"), " ", ChatColor.YELLOW+Languages.getString("items.ancientBladeLore")));
			meta.addEnchant(Enchantment.DAMAGE_ALL, 2, false);
			meta.setCustomModelData(100005);
			ancientBladeKey = new NamespacedKey(plugin, "dd-ancientBladeKey");
			meta.getPersistentDataContainer().set(ancientBladeKey, PersistentDataType.BYTE, (byte) 1);
			ancientblade.setItemMeta(meta);
			ancientBladeName = ChatColor.LIGHT_PURPLE+Languages.getString("items.ancientBlade");
			ancientCurseName = ChatColor.GRAY+Languages.getString("misc.ancientCurse");
			ancientBladeCooldown = plugin.getConfig().getInt("customitems.items.ancient_blade.ability_cooldown");
			allItems.put("ancientblade", ancientblade);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'ancientblade' &eplease report this bug to the discord along with the error above.");
		}
		
		//plague cure
		try {
			PotionMeta potionMeta = (PotionMeta) plagueCure.getItemMeta();
			potionMeta.setDisplayName(Languages.getString("items.plagueCure"));
			potionMeta.setLore(Arrays.asList(Languages.getString("items.plagueCureLore")));
			potionMeta.setColor(Color.BLACK);
			potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.HEAL, 1, 1, true, false, false), false);
			potionMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
			potionMeta.setBasePotionData(new PotionData(PotionType.AWKWARD));
			plagueCureKey = new NamespacedKey(plugin, "dd-plagueCureKey");
			potionMeta.getPersistentDataContainer().set(plagueCureKey, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100006);
			plagueCure.setItemMeta(potionMeta);
			PotionMeta splashMeta = potionMeta.clone();
			splashMeta.setCustomModelData(100013);
			plagueCureSplash.setItemMeta(splashMeta);
			plagueCureName = Languages.getString("items.plagueCure");
			plagueCureLore = Languages.getString("items.plagueCureLore");
			allItems.put("plaguecure", plagueCure);
			allItems.put("splashplaguecure", plagueCureSplash);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'plaguecure' &eplease report this bug to the discord along with the error above.");
		}
		
		//ancient bone
		try {
			meta = ancientbone.getItemMeta();
			meta.addEnchant(Enchantment.DURABILITY, 1, false);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			meta.setDisplayName(ChatColor.GOLD+Languages.getString("items.ancientBone"));
			meta.setLore(Arrays.asList(ChatColor.YELLOW+Languages.getString("items.ancientBoneLore"), craftables));
			meta.setCustomModelData(100007);
			ancientBoneKey = new NamespacedKey(plugin, "dd-ancientBoneKey");
			meta.getPersistentDataContainer().set(ancientBoneKey, PersistentDataType.BYTE, (byte) 1);
			ancientbone.setItemMeta(meta);
			ancientBoneLore = ChatColor.YELLOW+Languages.getString("items.ancientBoneLore");
			allItems.put("ancientbone", ancientbone);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'ancientbone' &eplease report this bug to the discord along with the error above.");
		}
		
		//ancient cloth
		try {
			meta = ancientcloth.getItemMeta();
			meta.addEnchant(Enchantment.DURABILITY, 1, false);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			meta.setDisplayName(ChatColor.GOLD+Languages.getString("items.ancientCloth"));
			meta.setLore(Arrays.asList(ChatColor.YELLOW+Languages.getString("items.ancientClothLore"), craftables));
			meta.setCustomModelData(100008);
			ancientClothKey = new NamespacedKey(plugin, "dd-ancientClothKey");
			meta.getPersistentDataContainer().set(ancientClothKey, PersistentDataType.BYTE, (byte) 1);
			ancientcloth.setItemMeta(meta);
			ancientClothLore = ChatColor.YELLOW+Languages.getString("items.ancientClothLore");
			allItems.put("ancientcloth", ancientcloth);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'ancientcloth' &eplease report this bug to the discord along with the error above.");
		}
		
		//mage wand
		try {
			meta = mageWand.getItemMeta();
			meta.addEnchant(Enchantment.DURABILITY, 1, false);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			meta.setDisplayName(ChatColor.GRAY+Languages.getString("items.mageWand"));
			meta.setLore(Arrays.asList(ChatColor.YELLOW+Languages.getString("items.mageWandLore")));
			meta.setCustomModelData(100009);
			mageWandKey = new NamespacedKey(plugin, "dd-mageWandKey");
			meta.getPersistentDataContainer().set(mageWandKey, PersistentDataType.BYTE, (byte) 1);
			mageWand.setItemMeta(meta);
			mageWandLore = ChatColor.YELLOW+Languages.getString("items.mageWandLore");
			mageWandCooldown = plugin.getConfig().getInt("customitems.items.dark_mage_wand.ability_cooldown");
			allItems.put("magewand", mageWand);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'magewand' &eplease report this bug to the discord along with the error above.");
		}
		
		//soul ripper
		try {
			meta = soulRipper.getItemMeta();
			meta.addEnchant(Enchantment.DURABILITY, 1, false);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			meta.setDisplayName(ChatColor.GRAY+Languages.getString("items.soulRipper"));
			meta.setLore(Arrays.asList(Languages.getString("items.soulRipperLore")));
			meta.setCustomModelData(100010);
			soulRipperKey = new NamespacedKey(plugin, "dd-soulRipperKey");
			meta.getPersistentDataContainer().set(soulRipperKey, PersistentDataType.BYTE, (byte) 1);
			soulRipper.setItemMeta(meta);
			soulRipperLore = Languages.getString("items.soulRipperLore");
			soulRipperCooldown = plugin.getConfig().getInt("customitems.items.soul_ripper.ability_cooldown");
			soulRipperNumberOfSouls = plugin.getConfig().getInt("customitems.items.soul_ripper.spawned_souls");
			soulRipperSoulLifeTicks = plugin.getConfig().getInt("customitems.items.soul_ripper.souls_life_ticks");
			allItems.put("soulripper", soulRipper);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'soulripper' &eplease report this bug to the discord along with the error above.");
		}

		//yeti fur
		try {
			meta = yetifur.getItemMeta();
			meta.addEnchant(Enchantment.DURABILITY, 1, false);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			meta.setDisplayName(ChatColor.BLUE + Languages.getString("items.yetiFur"));
			meta.setLore(Arrays.asList(ChatColor.YELLOW + Languages.getString("items.yetiFurLore"), craftables));
			meta.setCustomModelData(100011);
			yetiFurKey = new NamespacedKey(plugin, "dd-yetiFurKey");
			meta.getPersistentDataContainer().set(yetiFurKey, PersistentDataType.BYTE, (byte) 1);
			yetifur.setItemMeta(meta);
			yetiFurLore = ChatColor.YELLOW + Languages.getString("items.yetiFurLore");
			allItems.put("yetifur", yetifur);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'yetifur' &eplease report this bug to the discord along with the error above.");
		}
		
		//basic book
		try {
			meta = basicBook.getItemMeta();
			meta.setLore(Arrays.asList(ChatColor.GRAY + Languages.getString("misc.basicCoating")));
			basicCoatingKey = new NamespacedKey(plugin, "dd-basicEnch");
			meta.getPersistentDataContainer().set(basicCoatingKey, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100012);
			basicBook.setItemMeta(meta);
			basicBookLore = ChatColor.GRAY + Languages.getString("misc.basicCoating");
			basicBookSpawnrate = plugin.getConfig().getDouble("customitems.items.basic_coating_book.chest_spawn_rate");
			allItems.put("basicbook", basicBook);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'basicbook' &eplease report this bug to the discord along with the error above.");
		}
		
		//poseidons trident
		try {
			meta = poseidonsTrident.getItemMeta();
			meta.addEnchant(Enchantment.LOYALTY, 3, false);
			meta.addEnchant(Enchantment.IMPALING, 5, false);
			meta.setDisplayName(ChatColor.AQUA+Languages.getString("items.poseidonsTrident"));
			meta.setLore(Arrays.asList(ChatColor.GRAY+Languages.getString("misc.tidalWave")+" I", " ", Languages.getString("items.poseidonsTridentLore")));
			poseidonsTridentKey = new NamespacedKey(plugin, "dd-pTrident");
			meta.getPersistentDataContainer().set(poseidonsTridentKey, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100014);
			poseidonsTrident.setItemMeta(meta);
			poseidonsTridentLore = Languages.getString("items.poseidonsTridentLore");
			poseidonsTridentCooldown = plugin.getConfig().getInt("customitems.items.poseidons_trident.ability_cooldown");
			allItems.put("poseidonstrident", poseidonsTrident);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'poseidonstrident' &eplease report this bug to the discord along with the error above.");
		}
		
		//blood pact
		try {
			if (plugin.mcVersion >= 1.16)
				bloodPact = Utils.createItem(Material.NETHERITE_AXE, 1, ChatColor.DARK_RED+Languages.getString("items.bloodPact"),
						Arrays.asList(ChatColor.RED+Languages.getString("misc.bloodSacrifice")+" I", " ", ChatColor.YELLOW+Languages.getString("items.bloodPactLore")), false, false);
			else
				bloodPact = Utils.createItem(Material.DIAMOND_AXE, 1, ChatColor.DARK_RED+Languages.getString("items.bloodPact"),
						Arrays.asList(ChatColor.RED+Languages.getString("misc.bloodSacrifice")+" I", " ", ChatColor.YELLOW+Languages.getString("items.bloodPactLore")), false, false);
			meta = bloodPact.getItemMeta();
			meta.addEnchant(Enchantment.DAMAGE_ALL, 1, false);
			meta.setCustomModelData(100017);
			bloodPactKey = new NamespacedKey(plugin, "dd-bloodPact");
			meta.getPersistentDataContainer().set(bloodPactKey, PersistentDataType.INTEGER, 0);
			bloodPact.setItemMeta(meta);
			bloodPactCooldown = plugin.getConfig().getInt("customitems.items.blood_pact.ability_cooldown");
			allItems.put("bloodpact", bloodPact);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'bloodpact' &eplease report this bug to the discord along with the error above.");
		}
		
		//blood ingot
		try {
			bloodIngot = Utils.createItem(Material.BRICK, 1, ChatColor.DARK_RED+Languages.getString("items.bloodIngot"), Arrays.asList(ChatColor.YELLOW+Languages.getString("items.bloodIngotLore")), true, false);
			meta = bloodIngot.getItemMeta();
			bloodIngotKey = new NamespacedKey(plugin, "dd-bloodIngot");
			meta.getPersistentDataContainer().set(bloodIngotKey, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100016);
			bloodIngot.setItemMeta(meta);
			allItems.put("bloodingot", bloodIngot);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'bloodingot' &eplease report this bug to the discord along with the error above.");
		}
		
		//candy cane
		try {
			candyCane = Utils.createItem(Material.DIAMOND_SWORD, 1, ChatColor.RED+Languages.getString("items.candyCane"), Arrays.asList(ChatColor.YELLOW+Languages.getString("items.candyCaneLore")), false, false);
			meta = candyCane.getItemMeta();
			candyCaneKey = new NamespacedKey(plugin, "dd-candyCane");
			meta.getPersistentDataContainer().set(candyCaneKey, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100019);
			meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(UUID.randomUUID(), "generic.attackDamage", 9.0, Operation.ADD_NUMBER, EquipmentSlot.HAND));
			meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(UUID.randomUUID(), "generic.attackSpeed", -2.8, Operation.ADD_NUMBER, EquipmentSlot.HAND));
			candyCane.setItemMeta(meta);
			candyCaneCooldown = plugin.getConfig().getInt("customitems.items.candy_cane.ability_cooldown");
			allItems.put("candycane", candyCane);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'candycane' &eplease report this bug to the discord along with the error above.");
		}
		
		//cursed candy cane
		try {
			cursedCandyCane = Utils.createItem(Material.DIAMOND_SWORD, 1, ChatColor.RED+Languages.getString("items.cursedCandyCane"), Arrays.asList(ChatColor.YELLOW+Languages.getString("items.cursedCandyCaneLore")), true, false);
			meta = cursedCandyCane.getItemMeta();
			cursedCandyCaneKey = new NamespacedKey(plugin, "dd-cursedCandyCane");
			meta.getPersistentDataContainer().set(cursedCandyCaneKey, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100020);
			meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(UUID.randomUUID(), "generic.attackDamage", 11.0, Operation.ADD_NUMBER, EquipmentSlot.HAND));
			meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(UUID.randomUUID(), "generic.attackSpeed", -2.8, Operation.ADD_NUMBER, EquipmentSlot.HAND));
			cursedCandyCane.setItemMeta(meta);
			cursedCandyCaneCooldown = plugin.getConfig().getInt("customitems.items.cursed_candy_cane.ability_cooldown");
			allItems.put("cursedcandycane", cursedCandyCane);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'cursedcandycane' &eplease report this bug to the discord along with the error above.");
		}
		
		//ornament
		try {
			ornament = Utils.createItem(Material.GHAST_TEAR, 1, ChatColor.RED+Languages.getString("items.ornament"), Arrays.asList(ChatColor.YELLOW+Languages.getString("items.ornamentLore")), false, false);
			meta = ornament.getItemMeta();
			ornamentKey = new NamespacedKey(plugin, "dd-ornament");
			meta.getPersistentDataContainer().set(ornamentKey, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100018);
			ornament.setItemMeta(meta);
			allItems.put("ornament", ornament);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'ornament' &eplease report this bug to the discord along with the error above.");
		}
		
		//broken snow globe
		try {
			brokenSnowGlobe = Utils.createItem(CustomHead.BROKENSNOWGLOBE.getHead().clone(), 1, ChatColor.RED+Languages.getString("items.brokenSnowGlobe"), Arrays.asList(ChatColor.YELLOW+Languages.getString("items.brokenSnowGlobeLore")), false, false);
			meta = brokenSnowGlobe.getItemMeta();
			brokenSnowGlobeKey = new NamespacedKey(plugin, "dd-brokenSnowGlobe");
			meta.getPersistentDataContainer().set(brokenSnowGlobeKey, PersistentDataType.BYTE, (byte) 1);
			brokenSnowGlobe.setItemMeta(meta);
			allItems.put("brokensnowglobe", brokenSnowGlobe);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'brokensnowglobe' &eplease report this bug to the discord along with the error above.");
		}
		
		//snow globe
		try {
			snowGlobe = Utils.createItem(CustomHead.SNOWGLOBE.getHead().clone(), 1, ChatColor.RED+Languages.getString("items.snowGlobe"), Arrays.asList(ChatColor.YELLOW+Languages.getString("items.snowGlobeLore"), ChatColor.GRAY+"-"+Languages.getString("items.snowGlobeAbility")), false, false);
			meta = snowGlobe.getItemMeta();
			snowGlobeKey = new NamespacedKey(plugin, "dd-snowGlobe");
			meta.getPersistentDataContainer().set(snowGlobeKey, PersistentDataType.BYTE, (byte) 1);
			snowGlobe.setItemMeta(meta);
			allItems.put("snowglobe", snowGlobe);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'snowglobe' &eplease report this bug to the discord along with the error above.");
		}
		
		//santa hat
		try {
			santaHat = Utils.createItem(Material.DIAMOND_HELMET, 1, ChatColor.RED+Languages.getString("items.santaHat"), Arrays.asList(ChatColor.YELLOW+Languages.getString("items.santaHatLore"), ChatColor.GRAY+"-"+Languages.getString("items.santaHatAbility")), false, false);
			meta = santaHat.getItemMeta();
			santaHatKey = new NamespacedKey(plugin, "dd-santaHat");
			meta.getPersistentDataContainer().set(santaHatKey, PersistentDataType.INTEGER, 1);
			meta.setCustomModelData(100021);
			meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(UUID.randomUUID(), "generic.armor", 5.0, Operation.ADD_NUMBER, EquipmentSlot.HEAD));
			santaHat.setItemMeta(meta);
			santaHatCooldown = plugin.getConfig().getInt("customitems.items.santa_hat.elf_revive_rate");
			allItems.put("santahat", santaHat);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'santahat' &eplease report this bug to the discord along with the error above.");
		}
		
		//green egg
		try {
			greenEasterEgg = Utils.createItem(Material.TURTLE_EGG, 1, ChatColor.GREEN+Languages.getString("easter.greenEgg"), Arrays.asList(ChatColor.YELLOW+Languages.getString("easter.greenEggLore")), false, false);
			meta = greenEasterEgg.getItemMeta();
			greenEasterEggKey = new NamespacedKey(plugin, "dd-greenegg");
			meta.getPersistentDataContainer().set(greenEasterEggKey, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100023);
			greenEasterEgg.setItemMeta(meta);
			allItems.put("greenegg", greenEasterEgg);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'greenegg' &eplease report this bug to the discord along with the error above.");
		}
		
		//blue egg
		try {
			blueEasterEgg = Utils.createItem(Material.TURTLE_EGG, 1, ChatColor.BLUE+Languages.getString("easter.blueEgg"), Arrays.asList(ChatColor.YELLOW+Languages.getString("easter.blueEggLore")), false, false);
			meta = blueEasterEgg.getItemMeta();
			blueEasterEggKey = new NamespacedKey(plugin, "dd-blueegg");
			meta.getPersistentDataContainer().set(blueEasterEggKey, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100024);
			blueEasterEgg.setItemMeta(meta);
			allItems.put("blueegg", blueEasterEgg);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'blueegg' &eplease report this bug to the discord along with the error above.");
		}
		
		//red egg
		try {
			redEasterEgg = Utils.createItem(Material.TURTLE_EGG, 1, ChatColor.RED+Languages.getString("easter.redEgg"), Arrays.asList(ChatColor.YELLOW+Languages.getString("easter.redEggLore")), false, false);
			meta = redEasterEgg.getItemMeta();
			redEasterEggKey = new NamespacedKey(plugin, "dd-redegg");
			meta.getPersistentDataContainer().set(redEasterEggKey, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100025);
			redEasterEgg.setItemMeta(meta);
			allItems.put("redegg", redEasterEgg);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'redegg' &eplease report this bug to the discord along with the error above.");
		}
		
		//orange egg
		try {
			orangeEasterEgg = Utils.createItem(Material.TURTLE_EGG, 1, ChatColor.GOLD+Languages.getString("easter.orangeEgg"), Arrays.asList(ChatColor.YELLOW+Languages.getString("easter.orangeEggLore")), false, false);
			meta = orangeEasterEgg.getItemMeta();
			orangeEasterEggKey = new NamespacedKey(plugin, "dd-orangeegg");
			meta.getPersistentDataContainer().set(orangeEasterEggKey, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100026);
			orangeEasterEgg.setItemMeta(meta);
			allItems.put("orangeegg", orangeEasterEgg);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'orangeegg' &eplease report this bug to the discord along with the error above.");
		}
		
		//purple egg
		try {
			purpleEasterEgg = Utils.createItem(Material.TURTLE_EGG, 1, ChatColor.LIGHT_PURPLE+Languages.getString("easter.purpleEgg"), Arrays.asList(ChatColor.YELLOW+Languages.getString("easter.purpleEggLore")), false, false);
			meta = purpleEasterEgg.getItemMeta();
			purpleEasterEggKey = new NamespacedKey(plugin, "dd-purpleegg");
			meta.getPersistentDataContainer().set(purpleEasterEggKey, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100027);
			purpleEasterEgg.setItemMeta(meta);
			allItems.put("purpleegg", purpleEasterEgg);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'purpleegg' &eplease report this bug to the discord along with the error above.");
		}
		
		//golden egg
		try {
			goldenEasterEgg = Utils.createItem(Material.TURTLE_EGG, 1, Utils.chat("&e&l")+Languages.getString("easter.goldenEgg"), Arrays.asList(ChatColor.GREEN+Languages.getString("easter.goldenEggLore")), false, false);
			meta = goldenEasterEgg.getItemMeta();
			goldenEasterEggKey = new NamespacedKey(plugin, "dd-goldenegg");
			meta.getPersistentDataContainer().set(goldenEasterEggKey, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100028);
			goldenEasterEgg.setItemMeta(meta);
			allItems.put("goldenegg", goldenEasterEgg);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'goldenegg' &eplease report this bug to the discord along with the error above.");
		}
		
		//easter basket
		try {
			easterBasket = Utils.createItem(CustomHead.EASTERBASKET.getHead().clone(), 1, ChatColor.AQUA+Languages.getString("easter.basket"), Arrays.asList(ChatColor.YELLOW+Languages.getString("easter.basketLore"), ChatColor.GRAY+"-"+Languages.getString("easter.basketAbility")), false, false);
			meta = easterBasket.getItemMeta();
			easterBasketKey = new NamespacedKey(plugin, "dd-easterBasket");
			meta.getPersistentDataContainer().set(easterBasketKey, PersistentDataType.BYTE, (byte) 1);
			easterBasket.setItemMeta(meta);
			allItems.put("easterbasket", easterBasket);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'easterbasket' &eplease report this bug to the discord along with the error above.");
		}
		
		//bunny hop
		bunnyHopKey = new NamespacedKey(plugin, "dd-bunnyHopEnchant");
		
		//cursed flesh
		try {
			cursedFlesh = Utils.createItem(Material.ROTTEN_FLESH, 1, Utils.chat("&6&l")+Languages.getString("halloween.cursedFlesh"), Arrays.asList(ChatColor.YELLOW+Languages.getString("halloween.cursedFleshLore")), true, false);
			meta = cursedFlesh.getItemMeta();
			cursedFleshKey = new NamespacedKey(plugin, "dd-cursedflesh");
			meta.getPersistentDataContainer().set(cursedFleshKey, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100029);
			cursedFlesh.setItemMeta(meta);
			allItems.put("cursedflesh", cursedFlesh);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'cursedflesh' &eplease report this bug to the discord along with the error above.");
		}
		
		//vampire fang
		try {
			vampireFang = Utils.createItem(Material.GHAST_TEAR, 1, Utils.chat("&6&l")+Languages.getString("halloween.vampireFang"), Arrays.asList(ChatColor.YELLOW+Languages.getString("halloween.vampireFangLore")), false, false);
			meta = vampireFang.getItemMeta();
			vampireFangKey = new NamespacedKey(plugin, "dd-vampirefang");
			meta.getPersistentDataContainer().set(vampireFangKey, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100030);
			vampireFang.setItemMeta(meta);
			allItems.put("vampirefang", vampireFang);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'vampirefang' &eplease report this bug to the discord along with the error above.");
		}
		
		//candy corn
		try {
			candyCorn = Utils.createItem(Material.SUGAR, 1, Utils.chat("&6&l")+Languages.getString("halloween.candyCorn"), Arrays.asList(ChatColor.YELLOW+Languages.getString("halloween.candyCornLore")), false, false);
			meta = candyCorn.getItemMeta();
			candyCornKey = new NamespacedKey(plugin, "dd-candycorn");
			meta.getPersistentDataContainer().set(candyCornKey, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100031);
			candyCorn.setItemMeta(meta);
			allItems.put("candycorn", candyCorn);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'candycorn' &eplease report this bug to the discord along with the error above.");
		}
		
		//spooky pumpkin
		try {
			spookyPumpkin = Utils.createItem(Material.JACK_O_LANTERN, 1, Utils.chat("&6&l")+Languages.getString("halloween.spookyPumpkin"), Arrays.asList(ChatColor.YELLOW+Languages.getString("halloween.spookyPumpkinLore")), false, false);
			meta = spookyPumpkin.getItemMeta();
			spookyPumpkinKey = new NamespacedKey(plugin, "dd-spookypumpkin");
			meta.getPersistentDataContainer().set(spookyPumpkinKey, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100032);
			spookyPumpkin.setItemMeta(meta);
			allItems.put("spookypumpkin", spookyPumpkin);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'spookypumpkin' &eplease report this bug to the discord along with the error above.");
		}
		
		//pumpkin basket
		try {
			pumpkinBasket = Utils.createItem(CustomHead.TRICKORTREATBASKET.getHead().clone(), 1, ChatColor.AQUA+Languages.getString("halloween.pumpkinBasket"), Arrays.asList(ChatColor.YELLOW+Languages.getString("halloween.pumpkinBasketLore"), ChatColor.GRAY+"-"+Languages.getString("halloween.pumpkinBasketAbility")), false, false);
			meta = pumpkinBasket.getItemMeta();
			pumpkinBasketKey = new NamespacedKey(plugin, "dd-pumpkinBasket");
			meta.getPersistentDataContainer().set(pumpkinBasketKey, PersistentDataType.BYTE, (byte) 1);
			pumpkinBasket.setItemMeta(meta);
			allItems.put("pumpkinbasket", pumpkinBasket);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'pumpkinbasket' &eplease report this bug to the discord along with the error above.");
		}
		
		//ethereal lantern
		try {
			etherealLantern = Utils.createItem(Material.SOUL_LANTERN, 1, Utils.chat("&6&l")+Languages.getString("halloween.etherealLantern"), Arrays.asList(ChatColor.YELLOW+Languages.getString("halloween.etherealLanternLore")), true, false);
			meta = etherealLantern.getItemMeta();
			etherealLanternKey = new NamespacedKey(plugin, "dd-ethereallantern");
			meta.getPersistentDataContainer().set(etherealLanternKey, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100033);
			etherealLantern.setItemMeta(meta);
			etherealLanternCooldown = plugin.getConfig().getInt("customitems.items.ethereal_lantern.ability_cooldown");
			etherealLanternChance = plugin.getConfig().getDouble("customitems.items.ethereal_lantern.activate_chance") / 100.0;
			allItems.put("ethereallantern", etherealLantern);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'ethereallantern' &eplease report this bug to the discord along with the error above.");
		}
		
		//ethereal lantern boss
		try {
			etherealLanternBoss = Utils.createItem(Material.SOUL_LANTERN, 1, Utils.chat("&6&l")+Languages.getString("halloween.etherealLantern"), Arrays.asList(ChatColor.YELLOW+Languages.getString("halloween.etherealLanternLore")), false, false);
			meta = etherealLanternBoss.getItemMeta();
			etherealLanternBossKey = new NamespacedKey(plugin, "dd-ethereallanternboss");
			meta.getPersistentDataContainer().set(etherealLanternBossKey, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100034);
			etherealLanternBoss.setItemMeta(meta);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'ethereallanternboss' &eplease report this bug to the discord along with the error above.");
		}
		
		//ethereal lantern boss2
		try {
			etherealLanternBoss2 = Utils.createItem(Material.SOUL_LANTERN, 1, Utils.chat("&6&l")+Languages.getString("halloween.etherealLantern"), Arrays.asList(ChatColor.YELLOW+Languages.getString("halloween.etherealLanternLore")), false, false);
			meta = etherealLanternBoss2.getItemMeta();
			etherealLanternBoss2Key = new NamespacedKey(plugin, "dd-ethereallanternboss2");
			meta.getPersistentDataContainer().set(etherealLanternBoss2Key, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100035);
			etherealLanternBoss2.setItemMeta(meta);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'ethereallanternboss2' &eplease report this bug to the discord along with the error above.");
		}
		
		//disaster bottles
		
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
		
		//snow globe
		if (plugin.getConfig().getBoolean("customitems.recipes.snow_globe")) {
			if (snowGlobeRecipe == null || plugin.getServer().getRecipe(snowGlobeRecipe) == null) {
				snowGlobeRecipe = new NamespacedKey(plugin, "snow_globe");
				ShapelessRecipe sr = new ShapelessRecipe(snowGlobeRecipe, snowGlobe);
				sr.addIngredient(Material.DIAMOND_SWORD);
				sr.addIngredient(Material.PLAYER_HEAD);
				sr.addIngredient(Material.GHAST_TEAR);

				plugin.getServer().addRecipe(sr);
			}
		} else {
			if (snowGlobeRecipe != null && plugin.getServer().getRecipe(snowGlobeRecipe) != null)
				plugin.getServer().removeRecipe(snowGlobeRecipe);
			snowGlobeRecipe = null;
		}
		
		//easter basket
		if (plugin.getConfig().getBoolean("customitems.recipes.easter_basket")) {
			if (easterBasketRecipe == null || plugin.getServer().getRecipe(easterBasketRecipe) == null) {
				easterBasketRecipe = new NamespacedKey(plugin, "easter_basket");
				ShapelessRecipe sr = new ShapelessRecipe(easterBasketRecipe, easterBasket);
				sr.addIngredient(Material.TURTLE_EGG);
				sr.addIngredient(Material.TURTLE_EGG);
				sr.addIngredient(Material.TURTLE_EGG);
				sr.addIngredient(Material.TURTLE_EGG);
				sr.addIngredient(Material.TURTLE_EGG);

				plugin.getServer().addRecipe(sr);
			}
		} else {
			if (easterBasketRecipe != null && plugin.getServer().getRecipe(easterBasketRecipe) != null)
				plugin.getServer().removeRecipe(easterBasketRecipe);
			easterBasketRecipe = null;
		}
		
		//pumpkin basket
		if (plugin.getConfig().getBoolean("customitems.recipes.pumpkin_basket")) {
			if (pumpkinBasketRecipe == null || plugin.getServer().getRecipe(pumpkinBasketRecipe) == null) {
				pumpkinBasketRecipe = new NamespacedKey(plugin, "pumpkin_basket");
				ShapelessRecipe sr = new ShapelessRecipe(pumpkinBasketRecipe, pumpkinBasket);
				sr.addIngredient(Material.ROTTEN_FLESH);
				sr.addIngredient(Material.GHAST_TEAR);
				sr.addIngredient(Material.SUGAR);
				sr.addIngredient(Material.JACK_O_LANTERN);

				plugin.getServer().addRecipe(sr);
			}
		} else {
			if (pumpkinBasketRecipe != null && plugin.getServer().getRecipe(pumpkinBasketRecipe) != null)
				plugin.getServer().removeRecipe(pumpkinBasketRecipe);
			pumpkinBasketRecipe = null;
		}
	}
	public static void reload(Main plugin) {
		refreshMetas(plugin);
		createRecipes(plugin);
	}
}
