package deadlydisasters.entities.infestedcavesentities;

import java.util.Iterator;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.EulerAngle;

import deadlydisasters.entities.CustomDropsFactory;
import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.CustomHead;
import deadlydisasters.entities.EntityHandler;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;

public class InfestedEnderman extends CustomEntity {
	
	private DustOptions dust = new DustOptions(Color.fromRGB(9, 74, 72), 1);
	private ArmorStand stand;

	public InfestedEnderman(Mob entity, Main plugin) {
		super(entity, plugin);
		this.entityType = CustomEntityType.INFESTEDENDERMAN;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
		entity.setHealth(entityType.getHealth());
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(entityType.getDamage());
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.35);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40);
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
		stand.getEquipment().setHelmet(CustomHead.INFESTEDENDERMAN.getHead());
		stand.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.setMetadata("dd-infestedenderman", plugin.fixedData);
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.getString("entities.infestedEnderman"));
	}
	@Override
	public void tick() {
		if (entity == null || stand == null)
			return;
		entity.getWorld().spawnParticle(Particle.REDSTONE, entity.getLocation().add(0,1.2,0), 7, .25, 0.8, .25, 0.001, dust);
		if (entity.getTarget() == null)
			stand.teleport(entity.getLocation().add(0,0.9,0).add(entity.getLocation().getDirection().multiply(0.1)));
		else
			stand.teleport(entity.getLocation().add(0,1.15,0).add(entity.getLocation().getDirection().multiply(0.2)));
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
}
