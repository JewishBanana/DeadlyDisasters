package com.github.jewishbanana.deadlydisasters.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

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
		type.addEnchant(com.github.jewishbanana.uiframework.items.UIEnchantment.getEnchant(BasicCoating.REGISTERED_KEY), 1);
	}
	public String getDisplayName() {
		return Utils.convertString(DataUtils.getLanguageString("items.basic_coating_book.name"));
	}
	public com.github.jewishbanana.uiframework.items.ItemCategory getItemCategory() {
		return com.github.jewishbanana.uiframework.items.ItemCategory.DefaultCategory.ENCHANTED_BOOKS.getItemCategory();
	}
}
