package com.github.jewishbanana.deadlydisasters.utils;

import org.bukkit.inventory.ItemStack;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.items.BasicCoatingBook;
import com.github.jewishbanana.deadlydisasters.items.PlagueCure;
import com.github.jewishbanana.deadlydisasters.items.SplashPlagueCure;
import com.github.jewishbanana.deadlydisasters.items.VoidTear;
import com.github.jewishbanana.deadlydisasters.items.enchants.BasicCoating;
import com.github.jewishbanana.uiframework.UIFramework;

public class DependencyUtils {

	private static boolean uif;
	private static UIFHook uifHook;
	private static UCHook ucHook;
	private static boolean ultimateContent;
	
	private String UIFrameworkVersion = "2.2.3";
	
	public DependencyUtils(Main plugin) {
		ultimateContent = plugin.getServer().getPluginManager().isPluginEnabled("UltimateContent");
		if (plugin.getServer().getPluginManager().isPluginEnabled("UIFramework")) {
			if (!UIFramework.isVersionOrAbove(UIFrameworkVersion))
				Main.consoleSender.sendMessage(Utils.convertString(Languages.prefix+"&cERROR Cannot hook into UIFramework because UIFramework is out of date! Please update to at least &a"+UIFrameworkVersion+" &c(Current version installed is &b"+(plugin.getServer().getPluginManager().getPlugin("UIFramework").getDescription().getVersion())+"&c). The only effect this error will have is all custom items related to DeadlyDisasters will be disabled. You can update UIFramework here:&6 https://www.spigotmc.org/resources/uiframework.110768/"));
			else {
				uif = true;
				BasicCoating.register();
				uifHook = new UIFHook();
				if (ultimateContent)
					ucHook = new UCHook();
				else
					Main.consoleSender.sendMessage(Utils.convertString(Languages.prefix+"&aThere is an optional dependency UltimateContent, that adds some really cool custom items to DeadlyDisasters such as custom swords, custom mob drops, custom enchants, and more! Get UltimateContent here:&6 https://www.spigotmc.org/resources/ultimatecontent.118256/"));
				
				BasicCoatingBook.register();
				VoidTear.register();
				PlagueCure.register();
				SplashPlagueCure.register();
			}
		} else
			Main.consoleSender.sendMessage(Utils.convertString(Languages.prefix+"&bThere is an optional dependency UIFramework, that adds some custom items to DeadlyDisasters such as the plague cure potion, basic coating enchant, and more! Get UIFramework here:&6 https://www.spigotmc.org/resources/uiframework.110768/"));
	}
	public static boolean isUIFrameworkEnabled() {
		return uif;
	}
	public static boolean isUltimateContentEnabled() {
		return ultimateContent;
	}
	public static boolean doesItemExist(String item) {
		return uif && com.github.jewishbanana.uiframework.items.ItemType.getItemType(item) != null;
	}
	public static com.github.jewishbanana.uiframework.items.ItemType getItemType(String item) {
		return com.github.jewishbanana.uiframework.items.ItemType.getItemType(item);
	}
	public static int getBasicCoatingLevel(ItemStack item) {
		return uifHook == null ? 0 : uifHook.basicCoating.getEnchantLevel(item);
	}
	public static int getYetisBlessingLevel(ItemStack item) {
		return ucHook == null ? 0 : ucHook.yetisblessing.getEnchantLevel(item);
	}
	private class UIFHook {
		private com.github.jewishbanana.uiframework.items.UIEnchantment basicCoating;
		
		public UIFHook() {
			this.basicCoating = com.github.jewishbanana.uiframework.items.UIEnchantment.getEnchant(BasicCoating.REGISTERED_KEY);
		}
	}
	private class UCHook {
		private com.github.jewishbanana.uiframework.items.UIEnchantment yetisblessing;
		
		public UCHook() {
			this.yetisblessing = com.github.jewishbanana.uiframework.items.UIEnchantment.getEnchant("ui:yetis_blessing");
		}
	}
}
