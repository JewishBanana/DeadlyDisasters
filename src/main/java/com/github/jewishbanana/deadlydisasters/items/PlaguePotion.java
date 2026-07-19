package com.github.jewishbanana.deadlydisasters.items;

import java.util.stream.Collectors;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import com.github.jewishbanana.deadlydisasters.disasters.mob.BlackPlague;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class PlaguePotion extends com.github.jewishbanana.uiframework.items.GenericItem {

	public static final String REGISTERED_KEY = "dd:plague_potion";

	public PlaguePotion(ItemStack item) {
		super(item);
	}
	public boolean consumeItem(PlayerItemConsumeEvent event) {
		BlackPlague.infectEntityFromPotion(event.getPlayer());
		return true;
	}
	@Override
	public com.github.jewishbanana.uiframework.items.ItemBuilder createItem() {
		getType().setDisplayName(Utils.convertString(DataUtils.getLanguageString("items.plague_potion.name")));
		getType().setLore(DataUtils.getLanguageStringList("items.plague_potion.lore").stream().map(Utils::convertString).collect(Collectors.toList()));
		return com.github.jewishbanana.uiframework.items.ItemBuilder.create(getType(), Material.POTION).accessMeta(itemMeta ->
				((PotionMeta) itemMeta).setColor(Color.BLACK))
				.addItemFlags(VersionUtils.getHideEffects()).assembleLore().setCustomModelData(100014).build();
	}
	public static void register() {
		com.github.jewishbanana.uiframework.items.UIItemType.registerItem(REGISTERED_KEY, PlaguePotion.class);
	}
}

