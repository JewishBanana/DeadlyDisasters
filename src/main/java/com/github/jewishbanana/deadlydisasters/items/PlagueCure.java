package com.github.jewishbanana.deadlydisasters.items;

import java.util.Arrays;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.events.disasters.BlackPlague;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

@SuppressWarnings("removal")
public class PlagueCure extends com.github.jewishbanana.uiframework.items.GenericItem {
	
	public static String REGISTERED_KEY = "dd:plague_cure";
	
	public PlagueCure(ItemStack item) {
		super(item);
	}
	public boolean consumeItem(PlayerItemConsumeEvent event) {
		if (BlackPlague.time.containsKey(event.getPlayer().getUniqueId())) {
			BlackPlague.cureEntity(event.getPlayer());
			event.getPlayer().sendMessage(ChatColor.GREEN+Languages.getString("misc.cureMessage"));
		}
		return true;
	}
	@Override
	public com.github.jewishbanana.uiframework.items.ItemBuilder createItem() {
		getType().setDisplayName(Utils.convertString("&f")+Languages.getString("items.plagueCure"));
		getType().setLore(Arrays.asList(Utils.convertString("&e")+Languages.getString("items.plagueCureLore")));
		return com.github.jewishbanana.uiframework.items.ItemBuilder.create(getType(), Material.POTION).accessMeta(e -> {
			PotionMeta meta = (PotionMeta) e;
			meta.setColor(Color.BLACK);
		}).addItemFlags(VersionUtils.getHideEffects()).assembleLore().setCustomModelData(100006).build();
	}
	@SuppressWarnings("deprecation")
	public static void register() {
		com.github.jewishbanana.uiframework.items.ItemType type = com.github.jewishbanana.uiframework.items.ItemType.registerItem(REGISTERED_KEY, PlagueCure.class);
		
		ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(Main.getInstance(), "plague_cure_recipe"), type.getBuilder().getItem());
		recipe.shape(" A ", "ABA", " A ");
		recipe.setIngredient('A', Material.INK_SAC);
		ItemStack waterBottle = new ItemStack(Material.POTION);
		PotionMeta meta = (PotionMeta) waterBottle.getItemMeta();
		meta.setBasePotionData(new PotionData(PotionType.WATER));
		waterBottle.setItemMeta(meta);
		recipe.setIngredient('B', new RecipeChoice.ExactChoice(waterBottle));
		type.registerRecipe(recipe);
		if (Main.getInstance().mcVersion >= 1.17) {
			ShapedRecipe glowRecipe = new ShapedRecipe(new NamespacedKey(Main.getInstance(), "plague_cure_glow_recipe"), type.getBuilder().getItem());
			glowRecipe.shape(" A ", "ABA", " A ");
			glowRecipe.setIngredient('A', Material.GLOW_INK_SAC);
			glowRecipe.setIngredient('B', new RecipeChoice.ExactChoice(waterBottle));
			type.registerRecipe(glowRecipe);
		}
	}
}
