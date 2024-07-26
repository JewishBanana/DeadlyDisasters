package com.github.jewishbanana.deadlydisasters.items;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.items.enchants.BasicCoating;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class BasicCoatingBook extends com.github.jewishbanana.uiframework.items.GenericItem {
	
	public static String REGISTERED_KEY = "dd:basic_coating_book";

	public BasicCoatingBook(ItemStack item) {
		super(item);
	}
	@Override
	public com.github.jewishbanana.uiframework.items.ItemBuilder createItem() {
		return com.github.jewishbanana.uiframework.items.ItemBuilder.create(getType(), Material.ENCHANTED_BOOK).build();
	}
	public static void register() {
		com.github.jewishbanana.uiframework.items.ItemType type = com.github.jewishbanana.uiframework.items.ItemType.registerItem(REGISTERED_KEY, BasicCoatingBook.class);
		type.addEnchant(com.github.jewishbanana.uiframework.items.UIEnchantment.getEnchant(BasicCoating.REGISTERED_KEY), 1);
		
		type.registerRecipe(createAnvilBookRecipe(BasicCoating.REGISTERED_KEY, Utils.createIngredients(BasicCoating.applicableTypes)));
	}
	@SuppressWarnings("deprecation")
	private static com.github.jewishbanana.uiframework.utils.AnvilRecipe createAnvilBookRecipe(String enchant, List<ItemStack> ingredients) {
		com.github.jewishbanana.uiframework.items.UIEnchantment type = com.github.jewishbanana.uiframework.items.UIEnchantment.getEnchant(enchant);
		if (type == null)
			return null;
		com.github.jewishbanana.uiframework.utils.AnvilRecipe recipe = new com.github.jewishbanana.uiframework.utils.AnvilRecipe(ingredients, (event) -> {
			ItemStack item = event.getInventory().getItem(0).clone();
			int level = type.getEnchantLevel(item);
			int bookLevel = type.getEnchantLevel(event.getInventory().getItem(1));
			if (level >= type.getMaxLevel() || item.getAmount() != 1 || !type.canBeEnchanted(item) || level > bookLevel)
				return new ItemStack(Material.AIR);
			ItemMeta meta = item.getItemMeta();
			com.github.jewishbanana.uiframework.listeners.ItemListener.attachRecipeMetaFix(meta);
			item.setItemMeta(meta);
			com.github.jewishbanana.uiframework.items.GenericItem base = com.github.jewishbanana.uiframework.items.GenericItem.createItemBase(item);
			if (level > 0)
				type.unloadEnchant(base);
			if (type.addEnchant(base, bookLevel > level ? bookLevel : level+1, true)) {
				type.loadEnchant(base);
				base.getType().getBuilder().assembleLore(item, item.getItemMeta(), base.getType(), base);
				return item;
			}
			return new ItemStack(Material.AIR);
		}, false);
		recipe.setSlot(com.github.jewishbanana.uiframework.utils.AnvilRecipe.AnvilSlot.SECOND);
		return recipe;
	}
	public String getDisplayName() {
		return Utils.convertString("&7")+Languages.getString("items.basicCoatingBook");
	}
	public com.github.jewishbanana.uiframework.items.ItemCategory getItemCategory() {
		return com.github.jewishbanana.uiframework.items.ItemCategory.DefaultCategory.ENCHANTED_BOOKS.getItemCategory();
	}
}
