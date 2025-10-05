package com.github.jewishbanana.deadlydisasters.items;

import java.util.stream.Collectors;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

//import com.github.jewishbanana.deadlydisasters.disasters.BlackPlague;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class SplashPlagueCure extends com.github.jewishbanana.uiframework.items.GenericItem {
	
	public static final String REGISTERED_KEY = "dd:splash_plague_cure";
	
	public SplashPlagueCure(ItemStack item) {
		super(item);
	}
	public boolean splashPotion(PotionSplashEvent event) {
//		for (LivingEntity entity : event.getAffectedEntities())
//			if (BlackPlague.time.containsKey(entity.getUniqueId())) {
//				BlackPlague.cureEntity(entity);
//				if (entity instanceof Player)
//					entity.sendMessage(Utils.convertString(DataUtils.getConfigString("messages.disaster_broadcasts.plague.cure_message")));
//			}
		return true;
	}
	@Override
	public com.github.jewishbanana.uiframework.items.ItemBuilder createItem() {
		getType().setDisplayName(Utils.convertString(DataUtils.getLanguageString("items.splash_plague_cure.name")));
		getType().setLore(DataUtils.getLanguageStringList("items.splash_plague_cure.lore").stream().map(line -> Utils.convertString(line)).collect(Collectors.toList()));
		return com.github.jewishbanana.uiframework.items.ItemBuilder.create(getType(), Material.SPLASH_POTION).accessMeta(e -> {
			PotionMeta meta = (PotionMeta) e;
			meta.setColor(Color.BLACK);
		}).addItemFlags(VersionUtils.getHideEffects()).assembleLore().setCustomModelData(100013).build();
	}
	public static void register() {
		com.github.jewishbanana.uiframework.items.UIItemType.registerItem(REGISTERED_KEY, SplashPlagueCure.class);
	}
}
