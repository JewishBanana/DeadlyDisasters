package com.github.jewishbanana.deadlydisasters.items;

import java.util.Arrays;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import com.github.jewishbanana.deadlydisasters.events.disasters.BlackPlague;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class SplashPlagueCure extends com.github.jewishbanana.uiframework.items.GenericItem {
	
	public static String REGISTERED_KEY = "dd:splash_plague_cure";
	
	public SplashPlagueCure(ItemStack item) {
		super(item);
	}
	public boolean splashPotion(PotionSplashEvent event) {
		for (LivingEntity entity : event.getAffectedEntities())
			if (BlackPlague.time.containsKey(entity.getUniqueId())) {
				BlackPlague.cureEntity(entity);
				if (entity instanceof Player)
					entity.sendMessage(ChatColor.GREEN+Languages.getString("misc.cureMessage"));
			}
		return true;
	}
	@Override
	public com.github.jewishbanana.uiframework.items.ItemBuilder createItem() {
		getType().setDisplayName(Utils.convertString("&f")+Languages.getString("items.splashPlagueCure"));
		getType().setLore(Arrays.asList(Utils.convertString("&e")+Languages.getString("items.plagueCureLore")));
		return com.github.jewishbanana.uiframework.items.ItemBuilder.create(getType(), Material.SPLASH_POTION).accessMeta(e -> {
			PotionMeta meta = (PotionMeta) e;
			meta.setColor(Color.BLACK);
		}).addItemFlags(VersionUtils.getHideEffects()).assembleLore().setCustomModelData(100013).build();
	}
	public static void register() {
		com.github.jewishbanana.uiframework.items.ItemType.registerItem(REGISTERED_KEY, SplashPlagueCure.class);
	}
}
