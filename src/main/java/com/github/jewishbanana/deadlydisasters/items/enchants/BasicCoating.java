package com.github.jewishbanana.deadlydisasters.items.enchants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;

import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class BasicCoating extends com.github.jewishbanana.uiframework.items.UIEnchantment {
	
	public static final String REGISTERED_KEY = "dd:basic_coating";
	public static final List<Material> applicableTypes = new ArrayList<>(Arrays.asList(Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET, Material.GOLDEN_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET, Material.TURTLE_HELMET));
	static {
		Material copperHelmet = Material.getMaterial("COPPER_HELMET");
		if (copperHelmet != null)
			applicableTypes.add(1, copperHelmet);
	}

	public BasicCoating() {
		this.setMaxLevel(1);
	}
	public static void register() {
		com.github.jewishbanana.uiframework.items.UIEnchantment enchant = com.github.jewishbanana.uiframework.items.UIEnchantment.registerEnchant(REGISTERED_KEY, BasicCoating.class);
		
		enchant.setDisplayName(Utils.convertString(DataUtils.getLanguageString("enchants.basic_coating")));
		enchant.setApplicableTypes(applicableTypes);
	}
}
