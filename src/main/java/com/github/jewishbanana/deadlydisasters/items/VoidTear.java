package com.github.jewishbanana.deadlydisasters.items;

import java.util.Arrays;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.endstormentities.BabyEndTotem;
import com.github.jewishbanana.deadlydisasters.entities.endstormentities.EndTotem;
import com.github.jewishbanana.deadlydisasters.entities.endstormentities.EndWorm;
import com.github.jewishbanana.deadlydisasters.entities.endstormentities.VoidGuardian;
import com.github.jewishbanana.deadlydisasters.entities.endstormentities.VoidStalker;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.handlers.WorldObject;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class VoidTear extends com.github.jewishbanana.uiframework.items.GenericItem {
	
	public static String REGISTERED_KEY = "dd:void_tear";
	
	public VoidTear(ItemStack item) {
		super(item);
	}
	public boolean interacted(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_AIR) {
			Location target = event.getPlayer().getEyeLocation().add(event.getPlayer().getLocation().getDirection().multiply(6));
			if (target.getBlock().getType() != Material.AIR)
				return false;
			createRift(target);
			if (item.getAmount() == 1)
				com.github.jewishbanana.uiframework.items.GenericItem.removeBaseItem(item);
			item.setAmount(item.getAmount()-1);
		}
		return true;
	}
	private void createRift(Location loc) {
		int[] var = {60, 7};
		Main plugin = Main.getInstance();
		World world = loc.getWorld();
		boolean custom = (boolean) WorldObject.findWorldObject(loc.getWorld()).settings.get("custom_mob_spawning");
		ItemStack[] armor = {new ItemStack(Material.LEATHER_BOOTS), new ItemStack(Material.LEATHER_LEGGINGS), new ItemStack(Material.LEATHER_CHESTPLATE), new ItemStack(Material.LEATHER_HELMET)};
		if (!custom) {
			for (int i=0; i < 3; i++) {
				LeatherArmorMeta meta = (LeatherArmorMeta) armor[i].getItemMeta();
				meta.setColor(Color.fromBGR(50, 50, 50));
				armor[i].setItemMeta(meta);
			}
		}
		new RepeatingTask(plugin, 0, 5) {
			@Override
			public void run() {
				world.spawnParticle(Particle.PORTAL, loc, 20, .2, .2, .2, 1.5);
				world.spawnParticle(Particle.SQUID_INK, loc.clone().add(0,0.5,0), 30, .25, .25, .25, 0.0001);
				world.playSound(loc, Sound.BLOCK_PORTAL_AMBIENT, SoundCategory.AMBIENT, .7f, 1);
				for (Entity e : world.getNearbyEntities(loc, .5, .5, .5))
					if (e instanceof Player && (((Player) e).getGameMode() == GameMode.SURVIVAL || ((Player) e).getGameMode() == GameMode.ADVENTURE))
						Utils.pureDamageEntity((LivingEntity) e, 1, "dd-unstablerift", true, null);
				if (var[0] > 0)
					var[0]-=5;
				else {
					var[0] = 60;
					Mob entity = null;
					CustomEntity ce = null;
					if (!custom) {
						entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
						entity.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
						entity.getEquipment().setItemInOffHand(new ItemStack(Material.SHIELD));
						entity.getEquipment().setArmorContents(armor);
						
						var[1]--;
						if (var[1] <= 0) cancel();
						return;
					}
					if (var[1] >= 6) {
						entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
						ce = new VoidGuardian(entity, plugin, plugin.random);
					} else if (var[1] >= 4) {
						entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.PHANTOM);
						ce = new VoidStalker(entity, plugin, plugin.random);
					} else if (var[1] == 3) {
						entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
						ce = new EndWorm(entity, plugin, plugin.random);
					} else if (var[1] == 2) {
						entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.WITHER_SKELETON);
						entity.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
						ce = new EndTotem(entity, plugin, plugin.random);
					} else {
						entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.WOLF);
						ce = new BabyEndTotem(entity, plugin, plugin.random);
					}
					var[1]--;
					if (var[1] <= 0) cancel();
					if (entity == null)
						return;
					CustomEntity.handler.addEntity(ce);
				}
			}
		};
	}
	@Override
	public com.github.jewishbanana.uiframework.items.ItemBuilder createItem() {
		getType().setDisplayName(Utils.convertString("&d"+Languages.getString("items.voidTear")));
		getType().setLore(Arrays.asList(Utils.convertString(Languages.getString("items.voidTearLore.line 1")), Utils.convertString(Languages.getString("items.voidTearLore.line 2"))));
		return com.github.jewishbanana.uiframework.items.ItemBuilder.create(getType(), Material.GHAST_TEAR).setHiddenEnchanted(VersionUtils.getUnbreaking()).assembleLore().build();
	}
	public static void register() {
		com.github.jewishbanana.uiframework.items.ItemType.registerItem(REGISTERED_KEY, VoidTear.class);
	}
}
