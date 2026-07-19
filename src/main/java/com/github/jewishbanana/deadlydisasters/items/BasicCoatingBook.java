package com.github.jewishbanana.deadlydisasters.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

import com.github.jewishbanana.deadlydisasters.DeadlyDisasters;
import com.github.jewishbanana.deadlydisasters.items.enchants.BasicCoating;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class BasicCoatingBook extends com.github.jewishbanana.uiframework.items.GenericItem {
	
	public static final String REGISTERED_KEY = "dd:basic_coating_book";

	public BasicCoatingBook(ItemStack item) {
		super(item);
	}
	@Override
	public com.github.jewishbanana.uiframework.items.ItemBuilder createItem() {
		return com.github.jewishbanana.uiframework.items.ItemBuilder.create(getType(), Material.ENCHANTED_BOOK).build();
	}
	public static void register() {
		com.github.jewishbanana.uiframework.items.UIItemType type = com.github.jewishbanana.uiframework.items.UIItemType.registerItem(REGISTERED_KEY, BasicCoatingBook.class);
		com.github.jewishbanana.uiframework.items.UIEnchantment enchant = com.github.jewishbanana.uiframework.items.UIEnchantment.getEnchant(BasicCoating.REGISTERED_KEY);
		type.addEnchant(enchant, 1);
		com.github.jewishbanana.uiframework.items.GenericItem bookBase = type.createNewInstance(type.getItem());
		if (bookBase == null) {
			DeadlyDisasters.getInstance().getLogger().warning("Could not create the Basic Coating Book preview item; its anvil recipe preview will be unavailable.");
			return;
		}
		bookBase.refreshItemLore();
		com.github.jewishbanana.uiframework.utils.AnvilRecipe.AnvilChoice choice = new com.github.jewishbanana.uiframework.utils.AnvilRecipe.AnvilChoice(
				new RecipeChoice.MaterialChoice(BasicCoating.applicableTypes), new RecipeChoice.ExactChoice(bookBase.getItem()));
		type.registerUsedRecipe(new com.github.jewishbanana.uiframework.utils.AnvilRecipe(
				new NamespacedKey(DeadlyDisasters.getInstance(), "basic_coating_anvil_preview"), choice, inventory -> {
			ItemStack result = inventory.getFirstSlot().clone();
			com.github.jewishbanana.uiframework.items.GenericItem base = com.github.jewishbanana.uiframework.items.GenericItem.createItemBaseNoID(result);
			if (!enchant.addEnchant(base, 1, true, false))
				return new com.github.jewishbanana.uiframework.utils.AnvilRecipe.AnvilResult(new ItemStack(Material.AIR));
			base.refreshItemLore();
			return new com.github.jewishbanana.uiframework.utils.AnvilRecipe.AnvilResult(base.getItem(),
					enchant.getAnvilCost(result, inventory.getSecondSlot(), 1));
		}));
	}
	public String getDisplayName() {
		return Utils.convertString(DataUtils.getLanguageString("items.basic_coating_book.name"));
	}
	public com.github.jewishbanana.uiframework.items.ItemCategory getItemCategory() {
		return com.github.jewishbanana.uiframework.items.ItemCategory.DefaultCategory.ENCHANTED_BOOKS.getItemCategory();
	}
}
