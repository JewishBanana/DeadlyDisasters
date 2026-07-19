package com.github.jewishbanana.deadlydisasters.items;

import java.util.stream.Collectors;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.PotionMeta;

import com.github.jewishbanana.deadlydisasters.disasters.mob.BlackPlague;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class SplashPlaguePotion extends com.github.jewishbanana.uiframework.items.GenericItem {

	public static final String REGISTERED_KEY = "dd:splash_plague_potion";

	public SplashPlaguePotion(ItemStack item) {
		super(item);
	}
	public boolean splashPotion(PotionSplashEvent event) {
		for (LivingEntity entity : event.getAffectedEntities())
			BlackPlague.infectEntityFromPotion(entity);
		return true;
	}
	@Override
	public com.github.jewishbanana.uiframework.items.ItemBuilder createItem() {
		getType().setDisplayName(Utils.convertString(DataUtils.getLanguageString("items.splash_plague_potion.name")));
		getType().setLore(DataUtils.getLanguageStringList("items.splash_plague_potion.lore").stream().map(Utils::convertString).collect(Collectors.toList()));
		return com.github.jewishbanana.uiframework.items.ItemBuilder.create(getType(), Material.SPLASH_POTION).accessMeta(itemMeta ->
				((PotionMeta) itemMeta).setColor(Color.BLACK))
				.addItemFlags(VersionUtils.getHideEffects()).assembleLore().setCustomModelData(100015).build();
	}
	public static void register() {
		com.github.jewishbanana.uiframework.items.UIItemType type = com.github.jewishbanana.uiframework.items.UIItemType.registerItem(REGISTERED_KEY, SplashPlaguePotion.class);
		com.github.jewishbanana.uiframework.items.UIItemType potionType = com.github.jewishbanana.uiframework.items.UIItemType.getItemType(PlaguePotion.REGISTERED_KEY);
		if (potionType == null)
			throw new IllegalStateException("Plague Potion must be registered before Splash Plague Potion");
		type.registerRecipe(new com.github.jewishbanana.uiframework.utils.BrewingRecipe(
				new NamespacedKey(com.github.jewishbanana.deadlydisasters.DeadlyDisasters.getInstance(), "splash_plague_potion_recipe"),
				new RecipeChoice.ExactChoice(potionType.getItem()), new RecipeChoice.MaterialChoice(Material.GUNPOWDER), type.getItem()));
	}
}

