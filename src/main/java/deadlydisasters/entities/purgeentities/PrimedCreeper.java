package deadlydisasters.entities.purgeentities;

import java.util.Iterator;

import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataType;

import deadlydisasters.entities.CustomDropsFactory;
import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.general.Main;

public class PrimedCreeper extends CustomEntity {
	
	public PrimedCreeper(Mob entity, Main plugin) {
		super(entity, plugin);
		this.entityType = CustomEntityType.PRIMEDCREEPER;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		((Creeper) this.entity).setMaxFuseTicks(1);
		
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
		entity.setHealth(entityType.getHealth());
		entity.setRemoveWhenFarAway(true);
	}
	@Override
	public void tick() {
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Creeper) plugin.getServer().getEntity(entityUUID);
		if (entity == null) {
			it.remove();
			return;
		}
		if (entity.isDead()) {
			if (entity.getKiller() != null)
				CustomDropsFactory.generateDrops(entity.getLocation(), entityType);
			it.remove();
			return;
		}
		if (entity.getTarget() != null && entity.getWorld().equals(entity.getTarget().getWorld()) && entity.getTarget().getLocation().distanceSquared(entity.getEyeLocation()) <= 16) {
			if (plugin.mcVersion >= 1.16)
				((Creeper) entity).explode();
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
