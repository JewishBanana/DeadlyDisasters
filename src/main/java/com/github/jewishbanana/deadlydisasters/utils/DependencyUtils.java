package com.github.jewishbanana.deadlydisasters.utils;

import org.bukkit.inventory.ItemStack;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.items.BasicCoatingBook;
import com.github.jewishbanana.deadlydisasters.items.PlagueCure;
import com.github.jewishbanana.deadlydisasters.items.SplashPlagueCure;
import com.github.jewishbanana.deadlydisasters.items.VoidTear;
import com.github.jewishbanana.deadlydisasters.items.enchants.BasicCoating;

public class DependencyUtils {

	private static boolean uif;
	private static UIFHook uifHook;
	private static boolean ultimateContent;
	
	public DependencyUtils(Main plugin) {
		uif = plugin.getServer().getPluginManager().getPlugin("UIFramework") != null && plugin.getServer().getPluginManager().getPlugin("UIFramework").isEnabled();
		ultimateContent = plugin.getServer().getPluginManager().getPlugin("UltimateContent") != null && plugin.getServer().getPluginManager().getPlugin("UltimateContent").isEnabled();
		if (uif) {
			BasicCoating.register();
			uifHook = new UIFHook();
			
			BasicCoatingBook.register();
			VoidTear.register();
			PlagueCure.register();
			SplashPlagueCure.register();
		}
	}
	public static boolean isUIFrameworkEnabled() {
		return uif;
	}
	public static boolean isUltimateContentEnabled() {
		return ultimateContent;
	}
	public static int getBasicCoatingLevel(ItemStack item) {
		return uifHook == null ? 0 : uifHook.basicCoating.getEnchantLevel(item);
	}
	class UIFHook {
		private com.github.jewishbanana.uiframework.items.UIEnchantment basicCoating;
		
		public UIFHook() {
			this.basicCoating = com.github.jewishbanana.uiframework.items.UIEnchantment.getEnchant(BasicCoating.REGISTERED_KEY);
		}
	}
}
