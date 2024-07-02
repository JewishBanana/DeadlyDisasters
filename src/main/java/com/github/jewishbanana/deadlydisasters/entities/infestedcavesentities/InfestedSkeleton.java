package com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities;

import java.util.Iterator;

import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataType;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomDropsFactory;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.CustomHead;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class InfestedSkeleton extends CustomEntity {

	public InfestedSkeleton() {
	}
	public InfestedSkeleton(Mob entity, Main plugin) {
		super(entity, plugin);
		this.entityType = CustomEntityType.INFESTEDSKELETON;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		Utils.clearEntityOfItems(entity);
		entity.getEquipment().setHelmet(CustomHead.INFESTEDSKELETON.getHead());
		entity.getEquipment().setHelmetDropChance(0);
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
		entity.setHealth(entityType.getHealth());
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(entityType.getDamage());
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.3);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40);
		entity.setCanPickupItems(false);
		
		entity.setMetadata("dd-infestedskeleton", plugin.fixedData);
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.getString("entities.infestedSkeleton"));
	}
	@Override
	public void tick() {
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (entity == null || entity.isDead()) {
			if (entity != null && entity.getKiller() != null)
				CustomDropsFactory.generateDrops(entity.getLocation(), entityType);
			it.remove();
			return;
		}
	}
	@Override
	public void clean() {
	}
	@Override
	public void update(FileConfiguration file) {
	}
}
