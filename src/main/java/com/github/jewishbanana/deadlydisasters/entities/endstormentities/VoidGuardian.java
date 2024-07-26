package com.github.jewishbanana.deadlydisasters.entities.endstormentities;

import java.util.Iterator;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World.Environment;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomDropsFactory;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.CustomHead;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class VoidGuardian extends CustomEntity {
	
	private boolean rage;
	private Random rand;
	private ItemStack[] armor = {new ItemStack(Material.LEATHER_BOOTS), new ItemStack(Material.LEATHER_LEGGINGS), new ItemStack(Material.LEATHER_CHESTPLATE), CustomHead.VOIDGUARD.getHead()};

	public VoidGuardian() {
	}
	public VoidGuardian(Mob entity, Main plugin, Random rand) {
		super(entity, plugin);
		this.entityType = CustomEntityType.VOIDGUARDIAN;
		this.rand = rand;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
		entity.setHealth(entityType.getHealth());
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40);
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.25);
		changeColor(50, 50, 50, CustomHead.VOIDGUARD.getHead());
		entity.getEquipment().setItemInMainHand(DependencyUtils.doesItemExist("ui:voids_edge") ? DependencyUtils.getItemType("ui:voids_edge").getItem() : new ItemStack(Material.IRON_SWORD));
		entity.getEquipment().setItemInOffHand(DependencyUtils.doesItemExist("ui:abyssal_shield") ? DependencyUtils.getItemType("ui:abyssal_shield").getItem() : new ItemStack(Material.SHIELD));
		entity.setMetadata("dd-voidguardian", new FixedMetadataValue(plugin, "protected"));
		EntityEquipment equip = entity.getEquipment();
		equip.setHelmetDropChance(0);
		equip.setChestplateDropChance(0);
		equip.setLeggingsDropChance(0);
		equip.setBootsDropChance(0);
		equip.setItemInMainHandDropChance(0);
		equip.setItemInOffHandDropChance(0);
		entity.setCanPickupItems(false);
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.getString("entities.voidGuard"));
		entity.setSilent(true);
	}
	@Override
	public void tick() {
		if (entity == null)
			return;
		entity.getWorld().spawnParticle(Particle.DRAGON_BREATH, entity.getLocation().add(0,.5,0), 3, .25, .5, .25, .015);
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (entity == null) {
			it.remove();
			return;
		}
		if (entity.isDead()) {
			if (plugin.mcVersion >= 1.16)
				entity.getWorld().spawnParticle(Particle.SOUL, entity.getLocation().add(0,.5,0), 15, .3, .5, .3, .03);
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_DROWNED_DEATH, SoundCategory.HOSTILE, .8f, .5f);
			if (entity.getKiller() != null)
				CustomDropsFactory.generateDrops(entity.getLocation(), entityType);
			it.remove();
			return;
		}
		if (entity.getHealth() < entityType.getHealth()-1.0) {
			entity.setHealth(Math.min(entity.getHealth()+1.0, entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
			entity.getWorld().spawnParticle(Particle.COMPOSTER, entity.getLocation().add(0,.5,0), 5, .3, .4, .3, .001);
		}
		if (!rage && entity.getHealth() <= entityType.getHealth()/2.0) {
			changeColor(76,48,255, CustomHead.VOIDGUARDRAGE.getHead());
			entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.4);
			rage = true;
		} else if (rage && entity.getHealth() > entityType.getHealth()/2.0) {
			changeColor(50, 50, 50, CustomHead.VOIDGUARD.getHead());
			entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.25);
			rage = false;
			if (entity.getTarget() instanceof Player && !Utils.isPlayerImmune((Player) entity.getTarget()))
				plugin.achievementsHandler.awardProgress(entity.getTarget().getUniqueId(), "master.series.void_master", 1, 2);
		}
		if (rage)
			entity.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, entity.getLocation().add(0,.5,0), 10, .3, .3, .3, .25);
		if (rand.nextInt(8) == 0)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_DROWNED_AMBIENT, SoundCategory.HOSTILE, .8f, .5f);
	}
	@Override
	public void clean() {
	}
	@Override
	public void update(FileConfiguration file) {
	}
	private void changeColor(int r, int g, int b, ItemStack head) {
		for (int i=0; i < 3; i++) {
			LeatherArmorMeta meta = (LeatherArmorMeta) armor[i].getItemMeta();
			meta.setColor(Color.fromBGR(r,g,b));
			armor[i].setItemMeta(meta);
		}
		armor[3] = head;
		entity.getEquipment().setArmorContents(armor);
	}
	@Override
	public boolean spawnCondition(Location loc) {
		if (loc.getWorld().getEnvironment() == Environment.THE_END)
			return true;
		return false;
	}
	public Entity spawnEntity(Location loc) {
		Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
		handler.addEntity(new VoidGuardian(entity, Main.getInstance(), Main.getInstance().random));
		return entity;
	}
}
