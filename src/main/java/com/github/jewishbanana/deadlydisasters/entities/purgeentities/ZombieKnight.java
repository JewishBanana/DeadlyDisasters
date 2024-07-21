package com.github.jewishbanana.deadlydisasters.entities.purgeentities;

import java.util.Iterator;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.ZombieHorse;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.CustomHead;

public class ZombieKnight extends CustomEntity {
	
	private ZombieHorse horse;
	private UUID horseUUID;

	public ZombieKnight(Mob entity, Main plugin) {
		super(entity, plugin);
		this.entityType = CustomEntityType.ZOMBIEKNIGHT;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.getEquipment().setHelmet(CustomHead.ZOMBIEKNIGHT.getHead());
		entity.getEquipment().setHelmetDropChance(0);
		entity.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_SWORD));
		entity.getEquipment().setItemInOffHand(new ItemStack(Material.SHIELD));
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(30);
		entity.setHealth(30);
		
		if (!entity.isInsideVehicle()) {
			horse = (ZombieHorse) entity.getWorld().spawnEntity(entity.getLocation(), EntityType.ZOMBIE_HORSE);
			horse.setTamed(true);
			horse.addPassenger(entity);
			horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.425);
			horseUUID = horse.getUniqueId();
			horse.setRemoveWhenFarAway(true);
		} else
			if (entity.getVehicle() instanceof ZombieHorse)
				horseUUID = entity.getVehicle().getUniqueId();
	}
	@Override
	public void tick() {
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (horseUUID != null)
			horse = (ZombieHorse) plugin.getServer().getEntity(horseUUID);
		if (entity == null || entity.isDead()) {
			if (entity != null && entity.getKiller() != null && plugin.getConfig().getBoolean("customentities.allow_custom_drops")) {
				entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.ROTTEN_FLESH, plugin.random.nextInt(4)));
				if (plugin.random.nextInt(10) == 0)
					entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.STONE_SWORD));
			}
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
