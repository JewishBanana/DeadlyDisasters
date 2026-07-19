package com.github.jewishbanana.deadlydisasters.items;

import java.util.stream.Collectors;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import org.bukkit.entity.LivingEntity;

import com.github.jewishbanana.deadlydisasters.disasters.mob.BlackPlague;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

@SuppressWarnings({"deprecation", "removal"})
public class SplashPlagueCure extends com.github.jewishbanana.uiframework.items.GenericItem {
	
	public static final String REGISTERED_KEY = "dd:splash_plague_cure";
	
	public SplashPlagueCure(ItemStack item) {
		super(item);
	}
	public boolean splashPotion(PotionSplashEvent event) {
		for (LivingEntity entity : event.getAffectedEntities())
			if (BlackPlague.isInfected(entity))
				BlackPlague.cureEntity(entity);
		return true;
	}
	@Override
	public com.github.jewishbanana.uiframework.items.ItemBuilder createItem() {
		getType().setDisplayName(Utils.convertString(DataUtils.getLanguageString("items.splash_plague_cure.name")));
		getType().setLore(DataUtils.getLanguageStringList("items.splash_plague_cure.lore").stream().map(line -> Utils.convertString(line)).collect(Collectors.toList()));
		return com.github.jewishbanana.uiframework.items.ItemBuilder.create(getType(), Material.SPLASH_POTION).accessMeta(e -> {
			PotionMeta meta = (PotionMeta) e;
			meta.setColor(Color.fromRGB(250, 250, 247));
		}).addItemFlags(VersionUtils.getHideEffects()).assembleLore().setCustomModelData(100013).build();
	}
	public static void register() {
		com.github.jewishbanana.uiframework.items.UIItemType type = com.github.jewishbanana.uiframework.items.UIItemType.registerItem(REGISTERED_KEY, SplashPlagueCure.class);
		com.github.jewishbanana.uiframework.items.UIItemType cureType = com.github.jewishbanana.uiframework.items.UIItemType.getItemType(PlagueCure.REGISTERED_KEY);
		if (cureType == null)
			throw new IllegalStateException("Plague Cure must be registered before Splash Plague Cure");
		type.registerRecipe(new com.github.jewishbanana.uiframework.utils.BrewingRecipe(
				new NamespacedKey(com.github.jewishbanana.deadlydisasters.DeadlyDisasters.getInstance(), "splash_plague_cure_recipe"),
				new RecipeChoice.ExactChoice(cureType.getItem()), new RecipeChoice.MaterialChoice(Material.GUNPOWDER), type.getItem()));
		ItemStack awkwardSplashPotion = new ItemStack(Material.SPLASH_POTION);
		PotionMeta meta = (PotionMeta) awkwardSplashPotion.getItemMeta();
		meta.setBasePotionData(new PotionData(PotionType.AWKWARD));
		awkwardSplashPotion.setItemMeta(meta);
		type.registerRecipe(new com.github.jewishbanana.uiframework.utils.BrewingRecipe(
				new NamespacedKey(com.github.jewishbanana.deadlydisasters.DeadlyDisasters.getInstance(), "splash_plague_cure_direct_recipe"),
				new RecipeChoice.ExactChoice(awkwardSplashPotion), new RecipeChoice.MaterialChoice(Material.INK_SAC, Material.GLOW_INK_SAC), type.getItem()));
	}
}
