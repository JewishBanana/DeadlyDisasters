package deadlydisasters.entities;

import java.util.Iterator;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import deadlydisasters.general.Main;

public abstract class CustomEntity {
	
	protected Mob entity;
	protected UUID entityUUID;
	protected String species;
	protected Main plugin;
	protected CustomEntityType entityType;
	
	public CustomEntity(Mob entity, Main plugin) {
		if (entity != null) {
			this.entity = entity;
			this.entityUUID = entity.getUniqueId();
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
