package com.github.jewishbanana.deadlydisasters.items.enchants;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;

import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class BasicCoating extends com.github.jewishbanana.uiframework.items.UIEnchantment {
	
	public static String REGISTERED_KEY = "dd:basic_coating";
	public static List<Material> applicableTypes = Arrays.asList(Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET, Material.GOLDEN_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET, Material.TURTLE_HELMET);

	public BasicCoating(String registeredName, int id) {
		super(registeredName, id);
	}
	public static void register() {
		com.github.jewishbanana.uiframework.items.UIEnchantment enchant = com.github.jewishbanana.uiframework.items.UIEnchantment.registerEnchant(REGISTERED_KEY, BasicCoating.class);
		
		enchant.setDisplayName(Utils.convertString("&7")+Languages.getString("enchants.basicCoating"));
		enchant.setApplicableTypes(applicableTypes);
	}
}
