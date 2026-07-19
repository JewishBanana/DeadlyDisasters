package com.github.jewishbanana.deadlydisasters.items;

import java.util.stream.Collectors;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import com.github.jewishbanana.deadlydisasters.DeadlyDisasters;
import com.github.jewishbanana.deadlydisasters.disasters.mob.BlackPlague;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

@SuppressWarnings("removal")
public class PlagueCure extends com.github.jewishbanana.uiframework.items.GenericItem {
	
	public static final String REGISTERED_KEY = "dd:plague_cure";
	
	public PlagueCure(ItemStack item) {
		super(item);
	}
	public boolean consumeItem(PlayerItemConsumeEvent event) {
		if (BlackPlague.isInfected(event.getPlayer()))
			BlackPlague.cureEntity(event.getPlayer());
		return true;
	}
	@Override
	public com.github.jewishbanana.uiframework.items.ItemBuilder createItem() {
		getType().setDisplayName(Utils.convertString(DataUtils.getLanguageString("items.plague_cure.name")));
		getType().setLore(DataUtils.getLanguageStringList("items.plague_cure.lore").stream().map(line -> Utils.convertString(line)).collect(Collectors.toList()));
		return com.github.jewishbanana.uiframework.items.ItemBuilder.create(getType(), Material.POTION).accessMeta(e -> {
			PotionMeta meta = (PotionMeta) e;
			meta.setColor(Color.fromRGB(250, 250, 247));
		}).addItemFlags(VersionUtils.getHideEffects()).assembleLore().setCustomModelData(100006).build();
	}
	@SuppressWarnings("deprecation")
	public static void register() {
		com.github.jewishbanana.uiframework.items.UIItemType type = com.github.jewishbanana.uiframework.items.UIItemType.registerItem(REGISTERED_KEY, PlagueCure.class);
		
		ItemStack awkwardPotion = new ItemStack(Material.POTION);
		PotionMeta meta = (PotionMeta) awkwardPotion.getItemMeta();
		meta.setBasePotionData(new PotionData(PotionType.AWKWARD));
		awkwardPotion.setItemMeta(meta);
		type.registerRecipe(new com.github.jewishbanana.uiframework.utils.BrewingRecipe(
				new NamespacedKey(DeadlyDisasters.getInstance(), "plague_cure_brewing_recipe"),
				new RecipeChoice.ExactChoice(awkwardPotion), new RecipeChoice.MaterialChoice(Material.INK_SAC, Material.GLOW_INK_SAC), type.getBuilder().getItem()));
	}
}
