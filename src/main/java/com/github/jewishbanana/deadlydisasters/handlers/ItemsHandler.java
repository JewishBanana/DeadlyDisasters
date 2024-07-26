package com.github.jewishbanana.deadlydisasters.handlers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomHead;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

@SuppressWarnings("deprecation")
public class ItemsHandler {
	
	public static Map<String, ItemStack> allItems = new HashMap<>();
	
	public static ItemStack mageWand = new ItemStack(Material.BLAZE_ROD);
	public static String mageWandLore;
	public static int mageWandCooldown = 10;
	public static NamespacedKey mageWandKey;
	
	public static ItemStack soulRipper = new ItemStack(Material.IRON_HOE);
	public static String soulRipperLore;
	public static int soulRipperCooldown = 25;
	public static int soulRipperNumberOfSouls = 3;
	public static int soulRipperSoulLifeTicks = 260;
	public static NamespacedKey soulRipperKey;
	
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
	
	public static ItemStack easterBasket;
	private static NamespacedKey easterBasketRecipe;
	public static NamespacedKey easterBasketKey;
	
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
	
	public static Map<NamespacedKey, String> compatibilityMap;
	static {
		Main plugin = Main.getInstance();
		compatibilityMap = new HashMap<>();
		compatibilityMap.put(new NamespacedKey(plugin, "dd-voidShardKey"), "dd:void_tear");
		compatibilityMap.put(new NamespacedKey(plugin, "dd-voidsEdgeKey"), "ui:voids_edge");
		compatibilityMap.put(new NamespacedKey(plugin, "dd-voidShieldKey"), "ui:abyssal_shield");
		compatibilityMap.put(new NamespacedKey(plugin, "dd-voidBowKey"), "ui:call_of_the_void");
		compatibilityMap.put(new NamespacedKey(plugin, "dd-ancientBladeKey"), "ui:ancient_blade");
		compatibilityMap.put(new NamespacedKey(plugin, "dd-plagueCureKey"), "dd:plague_cure");
		compatibilityMap.put(new NamespacedKey(plugin, "dd-ancientBoneKey"), "ui:ancient_bone");
		compatibilityMap.put(new NamespacedKey(plugin, "dd-ancientClothKey"), "ui:ancient_cloth");
		compatibilityMap.put(new NamespacedKey(plugin, "dd-yetiFurKey"), "ui:yeti_fur");
		compatibilityMap.put(new NamespacedKey(plugin, "dd-basicEnch"), "dd:basic_coating_book");
		compatibilityMap.put(new NamespacedKey(plugin, "dd-pTrident"), "ui:tritons_fang");
		compatibilityMap.put(new NamespacedKey(plugin, "dd-goldenegg"), "ui:golden_egg");
	}
	
	public static void refreshMetas(Main plugin) {
		allItems.clear();
		
		//mage wand
		ItemMeta meta = mageWand.getItemMeta();
		meta.addEnchant(VersionUtils.getUnbreaking(), 1, false);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.setDisplayName(ChatColor.GRAY+Languages.langFile.getString("items.mageWand"));
		meta.setLore(Arrays.asList(ChatColor.YELLOW+Languages.langFile.getString("items.mageWandLore")));
		meta.setCustomModelData(100009);
		mageWandKey = new NamespacedKey(plugin, "dd-mageWandKey");
		meta.getPersistentDataContainer().set(mageWandKey, PersistentDataType.BYTE, (byte) 1);
		mageWand.setItemMeta(meta);
		mageWandLore = ChatColor.YELLOW+Languages.langFile.getString("items.mageWandLore");
		allItems.put("magewand", mageWand);
		
		//soul ripper
		meta = soulRipper.getItemMeta();
		meta.addEnchant(VersionUtils.getUnbreaking(), 1, false);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.setDisplayName(ChatColor.GRAY+Languages.langFile.getString("items.soulRipper"));
		meta.setLore(Arrays.asList(Languages.langFile.getString("items.soulRipperLore")));
		meta.setCustomModelData(100010);
		soulRipperKey = new NamespacedKey(plugin, "dd-soulRipperKey");
		meta.getPersistentDataContainer().set(soulRipperKey, PersistentDataType.BYTE, (byte) 1);
		soulRipper.setItemMeta(meta);
		soulRipperLore = Languages.langFile.getString("items.soulRipperLore");
		allItems.put("soulripper", soulRipper);
		
		//candy cane
		candyCane = Utils.createItem(Material.DIAMOND_SWORD, 1, ChatColor.RED+Languages.langFile.getString("christmas.candyCane"), Arrays.asList(ChatColor.YELLOW+Languages.langFile.getString("christmas.candyCaneLore")), false, false);
		meta = candyCane.getItemMeta();
		candyCaneKey = new NamespacedKey(plugin, "dd-candyCane");
		meta.getPersistentDataContainer().set(candyCaneKey, PersistentDataType.BYTE, (byte) 1);
		meta.setCustomModelData(100019);
		meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(UUID.randomUUID(), "generic.attackDamage", 9.0, Operation.ADD_NUMBER, EquipmentSlot.HAND));
		meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(UUID.randomUUID(), "generic.attackSpeed", -2.8, Operation.ADD_NUMBER, EquipmentSlot.HAND));
		candyCane.setItemMeta(meta);
		candyCaneCooldown = plugin.getConfig().getInt("customitems.items.candy_cane.ability_cooldown");
		allItems.put("candycane", candyCane);
		
		//cursed candy cane
		cursedCandyCane = Utils.createItem(Material.DIAMOND_SWORD, 1, ChatColor.RED+Languages.langFile.getString("christmas.cursedCandyCane"), Arrays.asList(ChatColor.YELLOW+Languages.langFile.getString("christmas.cursedCandyCaneLore")), true, false);
		meta = cursedCandyCane.getItemMeta();
		cursedCandyCaneKey = new NamespacedKey(plugin, "dd-cursedCandyCane");
		meta.getPersistentDataContainer().set(cursedCandyCaneKey, PersistentDataType.BYTE, (byte) 1);
		meta.setCustomModelData(100020);
		meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(UUID.randomUUID(), "generic.attackDamage", 11.0, Operation.ADD_NUMBER, EquipmentSlot.HAND));
		meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(UUID.randomUUID(), "generic.attackSpeed", -2.8, Operation.ADD_NUMBER, EquipmentSlot.HAND));
		cursedCandyCane.setItemMeta(meta);
		cursedCandyCaneCooldown = plugin.getConfig().getInt("customitems.items.cursed_candy_cane.ability_cooldown");
		allItems.put("cursedcandycane", cursedCandyCane);
		
		//ornament
		ornament = Utils.createItem(Material.GHAST_TEAR, 1, ChatColor.RED+Languages.langFile.getString("christmas.ornament"), Arrays.asList(ChatColor.YELLOW+Languages.langFile.getString("christmas.ornamentLore")), false, false);
		meta = ornament.getItemMeta();
		ornamentKey = new NamespacedKey(plugin, "dd-ornament");
		meta.getPersistentDataContainer().set(ornamentKey, PersistentDataType.BYTE, (byte) 1);
		meta.setCustomModelData(100018);
		ornament.setItemMeta(meta);
		allItems.put("ornament", ornament);
		
		//broken snow globe
		brokenSnowGlobe = Utils.createItem(CustomHead.BROKENSNOWGLOBE.getHead().clone(), 1, ChatColor.RED+Languages.langFile.getString("christmas.brokenSnowGlobe"), Arrays.asList(ChatColor.YELLOW+Languages.langFile.getString("christmas.brokenSnowGlobeLore")), false, false);
		meta = brokenSnowGlobe.getItemMeta();
		brokenSnowGlobeKey = new NamespacedKey(plugin, "dd-brokenSnowGlobe");
		meta.getPersistentDataContainer().set(brokenSnowGlobeKey, PersistentDataType.BYTE, (byte) 1);
		brokenSnowGlobe.setItemMeta(meta);
		allItems.put("brokensnowglobe", brokenSnowGlobe);
		
		//snow globe
		snowGlobe = Utils.createItem(CustomHead.SNOWGLOBE.getHead().clone(), 1, ChatColor.RED+Languages.langFile.getString("christmas.snowGlobe"), Arrays.asList(ChatColor.YELLOW+Languages.langFile.getString("christmas.snowGlobeLore"), ChatColor.GRAY+"-"+Languages.langFile.getString("christmas.snowGlobeAbility")), false, false);
		meta = snowGlobe.getItemMeta();
		snowGlobeKey = new NamespacedKey(plugin, "dd-snowGlobe");
		meta.getPersistentDataContainer().set(snowGlobeKey, PersistentDataType.BYTE, (byte) 1);
		snowGlobe.setItemMeta(meta);
		allItems.put("snowglobe", snowGlobe);
		
		//santa hat
		santaHat = Utils.createItem(Material.DIAMOND_HELMET, 1, ChatColor.RED+Languages.langFile.getString("christmas.santaHat"), Arrays.asList(ChatColor.YELLOW+Languages.langFile.getString("christmas.santaHatLore"), ChatColor.GRAY+"-"+Languages.langFile.getString("christmas.santaHatAbility")), false, false);
		meta = santaHat.getItemMeta();
		santaHatKey = new NamespacedKey(plugin, "dd-santaHat");
		meta.getPersistentDataContainer().set(santaHatKey, PersistentDataType.INTEGER, 1);
		meta.setCustomModelData(100021);
		meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(UUID.randomUUID(), "generic.armor", 5.0, Operation.ADD_NUMBER, EquipmentSlot.HEAD));
		santaHat.setItemMeta(meta);
		santaHatCooldown = 5;
		allItems.put("santahat", santaHat);
		
		//green egg
		greenEasterEgg = Utils.createItem(Material.TURTLE_EGG, 1, ChatColor.GREEN+Languages.langFile.getString("easter.greenEgg"), Arrays.asList(ChatColor.YELLOW+Languages.langFile.getString("easter.greenEggLore")), false, false);
		meta = greenEasterEgg.getItemMeta();
		greenEasterEggKey = new NamespacedKey(plugin, "dd-greenegg");
		meta.getPersistentDataContainer().set(greenEasterEggKey, PersistentDataType.BYTE, (byte) 1);
		meta.setCustomModelData(100023);
		greenEasterEgg.setItemMeta(meta);
		allItems.put("greenegg", greenEasterEgg);
		
		//blue egg
		blueEasterEgg = Utils.createItem(Material.TURTLE_EGG, 1, ChatColor.BLUE+Languages.langFile.getString("easter.blueEgg"), Arrays.asList(ChatColor.YELLOW+Languages.langFile.getString("easter.blueEggLore")), false, false);
		meta = blueEasterEgg.getItemMeta();
		blueEasterEggKey = new NamespacedKey(plugin, "dd-blueegg");
		meta.getPersistentDataContainer().set(blueEasterEggKey, PersistentDataType.BYTE, (byte) 1);
		meta.setCustomModelData(100024);
		blueEasterEgg.setItemMeta(meta);
		allItems.put("blueegg", blueEasterEgg);
		
		//red egg
		redEasterEgg = Utils.createItem(Material.TURTLE_EGG, 1, ChatColor.RED+Languages.langFile.getString("easter.redEgg"), Arrays.asList(ChatColor.YELLOW+Languages.langFile.getString("easter.redEggLore")), false, false);
		meta = redEasterEgg.getItemMeta();
		redEasterEggKey = new NamespacedKey(plugin, "dd-redegg");
		meta.getPersistentDataContainer().set(redEasterEggKey, PersistentDataType.BYTE, (byte) 1);
		meta.setCustomModelData(100025);
		redEasterEgg.setItemMeta(meta);
		allItems.put("redegg", redEasterEgg);
		
		//orange egg
		orangeEasterEgg = Utils.createItem(Material.TURTLE_EGG, 1, ChatColor.GOLD+Languages.langFile.getString("easter.orangeEgg"), Arrays.asList(ChatColor.YELLOW+Languages.langFile.getString("easter.orangeEggLore")), false, false);
		meta = orangeEasterEgg.getItemMeta();
		orangeEasterEggKey = new NamespacedKey(plugin, "dd-orangeegg");
		meta.getPersistentDataContainer().set(orangeEasterEggKey, PersistentDataType.BYTE, (byte) 1);
		meta.setCustomModelData(100026);
		orangeEasterEgg.setItemMeta(meta);
		allItems.put("orangeegg", orangeEasterEgg);
		
		//purple egg
		purpleEasterEgg = Utils.createItem(Material.TURTLE_EGG, 1, ChatColor.LIGHT_PURPLE+Languages.langFile.getString("easter.purpleEgg"), Arrays.asList(ChatColor.YELLOW+Languages.langFile.getString("easter.purpleEggLore")), false, false);
		meta = purpleEasterEgg.getItemMeta();
		purpleEasterEggKey = new NamespacedKey(plugin, "dd-purpleegg");
		meta.getPersistentDataContainer().set(purpleEasterEggKey, PersistentDataType.BYTE, (byte) 1);
		meta.setCustomModelData(100027);
		purpleEasterEgg.setItemMeta(meta);
		allItems.put("purpleegg", purpleEasterEgg);
		
		
		//easter basket
		easterBasket = Utils.createItem(CustomHead.EASTERBASKET.getHead().clone(), 1, ChatColor.AQUA+Languages.langFile.getString("easter.basket"), Arrays.asList(ChatColor.YELLOW+Languages.langFile.getString("easter.basketLore"), ChatColor.GRAY+"-"+Languages.langFile.getString("easter.basketAbility")), false, false);
		meta = easterBasket.getItemMeta();
		easterBasketKey = new NamespacedKey(plugin, "dd-easterBasket");
		meta.getPersistentDataContainer().set(easterBasketKey, PersistentDataType.BYTE, (byte) 1);
		easterBasket.setItemMeta(meta);
		allItems.put("easterbasket", easterBasket);
		
		//cursed flesh
		try {
			cursedFlesh = Utils.createItem(Material.ROTTEN_FLESH, 1, Utils.convertString("&6&l")+Languages.getString("halloween.cursedFlesh"), Arrays.asList(ChatColor.YELLOW+Languages.getString("halloween.cursedFleshLore")), true, false);
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
			vampireFang = Utils.createItem(Material.GHAST_TEAR, 1, Utils.convertString("&6&l")+Languages.getString("halloween.vampireFang"), Arrays.asList(ChatColor.YELLOW+Languages.getString("halloween.vampireFangLore")), false, false);
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
			candyCorn = Utils.createItem(Material.SUGAR, 1, Utils.convertString("&6&l")+Languages.getString("halloween.candyCorn"), Arrays.asList(ChatColor.YELLOW+Languages.getString("halloween.candyCornLore")), false, false);
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
			spookyPumpkin = Utils.createItem(Material.JACK_O_LANTERN, 1, Utils.convertString("&6&l")+Languages.getString("halloween.spookyPumpkin"), Arrays.asList(ChatColor.YELLOW+Languages.getString("halloween.spookyPumpkinLore")), false, false);
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
			etherealLantern = Utils.createItem(Material.SOUL_LANTERN, 1, Utils.convertString("&6&l")+Languages.getString("halloween.etherealLantern"), Arrays.asList(ChatColor.YELLOW+Languages.getString("halloween.etherealLanternLore")), true, false);
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
			etherealLanternBoss = Utils.createItem(Material.SOUL_LANTERN, 1, Utils.convertString("&6&l")+Languages.getString("halloween.etherealLantern"), Arrays.asList(ChatColor.YELLOW+Languages.getString("halloween.etherealLanternLore")), false, false);
			meta = etherealLanternBoss.getItemMeta();
			etherealLanternBossKey = new NamespacedKey(plugin, "dd-ethereallanternboss");
			meta.getPersistentDataContainer().set(etherealLanternBossKey, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100034);
			etherealLanternBoss.setItemMeta(meta);
			allItems.put("ethereallanternb1", etherealLanternBoss);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'ethereallanternboss' &eplease report this bug to the discord along with the error above.");
		}
		
		//ethereal lantern boss2
		try {
			etherealLanternBoss2 = Utils.createItem(Material.SOUL_LANTERN, 1, Utils.convertString("&6&l")+Languages.getString("halloween.etherealLantern"), Arrays.asList(ChatColor.YELLOW+Languages.getString("halloween.etherealLanternLore")), false, false);
			meta = etherealLanternBoss2.getItemMeta();
			etherealLanternBoss2Key = new NamespacedKey(plugin, "dd-ethereallanternboss2");
			meta.getPersistentDataContainer().set(etherealLanternBoss2Key, PersistentDataType.BYTE, (byte) 1);
			meta.setCustomModelData(100035);
			etherealLanternBoss2.setItemMeta(meta);
			allItems.put("ethereallanternb2", etherealLanternBoss2);
		} catch (Exception e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage("&e[DeadlyDisasters]: Error unable to initialize &d'ethereallanternboss2' &eplease report this bug to the discord along with the error above.");
		}
	}
	public static void createRecipes(Main plugin) {
		if (plugin.mcVersion < 1.16) {
			Main.consoleSender.sendMessage(Languages.prefix+Utils.convertString("&eWARNING old version detected ( < 1.16) All custom crafting recipes are disabled, custom crafting recipe support is only for 1.16+"));
			return;
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
