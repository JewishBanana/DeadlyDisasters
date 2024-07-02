package com.github.jewishbanana.deadlydisasters.entities.purgeentities;

import java.util.Iterator;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomDropsFactory;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.CustomHead;
import com.github.jewishbanana.deadlydisasters.entities.EntityHandler;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;

public class SkeletonKnight extends CustomEntity {
	
	private SkeletonHorse horse;
	private UUID horseUUID;

	public SkeletonKnight() {
	}
	public SkeletonKnight(Skeleton entity, Main plugin) {
		super(entity, plugin);
		this.entityType = CustomEntityType.SKELETONKNIGHT;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.getEquipment().setHelmet(CustomHead.SKELETONKNIGHT.getHead());
		entity.getEquipment().setHelmetDropChance(0);
		entity.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
		entity.getEquipment().setItemInOffHand(new ItemStack(Material.SHIELD));
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
		entity.setHealth(entityType.getHealth());
		entity.setCanPickupItems(false);
		
		if (!entity.isInsideVehicle()) {
			horse = (SkeletonHorse) entity.getWorld().spawnEntity(entity.getLocation(), EntityType.SKELETON_HORSE);
			horse.setTamed(true);
			horse.addPassenger(entity);
			horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.375);
			horseUUID = horse.getUniqueId();
			horse.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		} else
			if (entity.getVehicle() instanceof SkeletonHorse)
				horseUUID = entity.getVehicle().getUniqueId();
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.getString("entities.skeletonKnight"));
	}
	@Override
	public void tick() {
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Skeleton) plugin.getServer().getEntity(entityUUID);
		if (horseUUID != null)
			horse = (SkeletonHorse) plugin.getServer().getEntity(horseUUID);
		if (entity == null || entity.isDead()) {
			if (entity != null && entity.getKiller() != null)
				CustomDropsFactory.generateDrops(entity.getLocation(), entityType);
			it.remove();
			if (horse != null && !horse.isDead()) {
				horse.setTamed(false);
				plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
					public void run() {
						if (horse != null && !horse.isDead())
							horse.remove();
					}
				}, 600);
			}
			return;
		}
	}
	@Override
	public void clean() {
		if (horse != null && !horse.isDead())
			horse.remove();
	}
	@Override
	public void update(FileConfiguration file) {
	}
}
