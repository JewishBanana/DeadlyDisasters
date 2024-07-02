package com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities;

import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.EulerAngle;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomDropsFactory;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.CustomHead;
import com.github.jewishbanana.deadlydisasters.entities.EntityHandler;
import com.github.jewishbanana.deadlydisasters.entities.endstormentities.EndWorm;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class InfestedCreeper extends CustomEntity {
	
	private BlockData data;
	private ArmorStand stand;

	public InfestedCreeper() {
	}
	public InfestedCreeper(Creeper entity, Main plugin) {
		super(entity, plugin);
		this.entityType = CustomEntityType.INFESTEDCREEPER;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
		entity.setHealth(entityType.getHealth());
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(entityType.getDamage());
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.25);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40);
		entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true, false));
		entity.setExplosionRadius(5);
		if (plugin.mcVersion >= 1.16)
			entity.setFuseTicks(15);
		stand = (ArmorStand) entity.getWorld().spawnEntity(entity.getLocation().clone().add(100,100,0), EntityType.ARMOR_STAND);
		if (plugin.mcVersion >= 1.16)
			stand.setInvisible(true);
		else
			stand.setVisible(false);
		stand.setMarker(true);
		stand.setGravity(false);
		if (plugin.mcVersion >= 1.16) {
			stand.addEquipmentLock(EquipmentSlot.CHEST, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.FEET, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.HAND, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.HEAD, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.LEGS, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.OFF_HAND, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
		}
		stand.getEquipment().setHelmet(CustomHead.INFESTEDCREEPER.getHead());
		stand.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.setMetadata("dd-infestedcreeper", plugin.fixedData);
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.getString("entities.infestedCreeper"));
		
		if (plugin.mcVersion >= 1.19)
			data = Material.SCULK.createBlockData();
		else
			data = Material.NETHERRACK.createBlockData();
	}
	@Override
	public void tick() {
		if (entity == null || stand == null)
			return;
		entity.getWorld().spawnParticle(VersionUtils.getBlockCrack(), entity.getLocation().add(0,0.7,0), 10, .25, .4, .25, 0.1, data);
		stand.teleport(entity.getLocation().subtract(0,0.2,0));
		stand.setHeadPose(new EulerAngle(Math.toRadians(entity.getLocation().getPitch()),0,0));
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (entity == null || entity.isDead() || stand == null) {
			if (entity != null && entity.getKiller() != null)
				CustomDropsFactory.generateDrops(entity.getLocation(), entityType);
			clean();
			it.remove();
			return;
		}
		stand = (ArmorStand) plugin.getServer().getEntity(stand.getUniqueId());
	}
	@Override
	public void clean() {
		if (stand != null)
			stand.remove();
	}
	@Override
	public void update(FileConfiguration file) {
	}
	@Override
	public boolean spawnCondition(Location loc) {
		if (loc.getWorld().getEnvironment() == Environment.THE_END)
			return true;
		return false;
	}
	public Entity spawnEntity(Location loc) {
		Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
		handler.addEntity(new EndWorm(entity, Main.getInstance(), Main.getInstance().random));
		return entity;
	}
}
