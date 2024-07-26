package com.github.jewishbanana.deadlydisasters.entities;

import java.util.Iterator;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataType;

import com.github.jewishbanana.deadlydisasters.Main;

public abstract class CustomEntity {
	
	protected Mob entity;
	protected UUID entityUUID;
	protected String species;
	protected Main plugin;
	protected CustomEntityType entityType;
	
	public static EntityHandler handler;
	
	public CustomEntity(Mob entity, Main plugin) {
		if (entity != null) {
			this.entity = entity;
			this.entityUUID = entity.getUniqueId();
			if (entity.getPersistentDataContainer().has(EntityHandler.removalKey, PersistentDataType.BYTE)) {
				plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
					if (entity != null)
						entity.remove();
				}, 1);
			}
		}
		this.plugin = plugin;
	}
	public abstract void tick();
	public abstract void function(Iterator<CustomEntity> it);
	public abstract void clean();
	public abstract void update(FileConfiguration file);
	
	public LivingEntity getEntity() {
		return entity;
	}
	public UUID getUUID() {
		return entityUUID;
	}
	public String getSpecies() {
		return species;
	}
	public CustomEntityType getType() {
		return entityType;
	}
	public void refreshReferences(Entity[] entities) {
		for (int i=0; i < entities.length; i++)
			if (entities[i] != null)
				entities[i] = plugin.getServer().getEntity(entities[i].getUniqueId());
	}
	public void refreshReferences(Entity[] entities, UUID[] uuids) {
		for (int i=0; i < entities.length; i++)
			entities[i] = plugin.getServer().getEntity(uuids[i]);
	}
}
