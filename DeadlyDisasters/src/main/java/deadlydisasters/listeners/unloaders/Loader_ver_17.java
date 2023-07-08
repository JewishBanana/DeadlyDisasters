package deadlydisasters.listeners.unloaders;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.persistence.PersistentDataType;

import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.EntityHandler;
import deadlydisasters.general.Main;

public class Loader_ver_17 implements Listener {
	
	private EntityHandler handler;
	
	public Loader_ver_17(Main plugin, EntityHandler handler) {
		this.handler = handler;
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler
	public void onEntitiesLoad(EntitiesLoadEvent e) {
		for (Entity entity : e.getEntities())
			if (entity.isValid() && entity.getPersistentDataContainer().has(handler.globalKey, PersistentDataType.BYTE)) {
				for (CustomEntityType type : CustomEntityType.values())
					if (entity.getPersistentDataContainer().has(type.nameKey, PersistentDataType.BYTE)) {
						handler.addEntityBySpecies(type.species, entity);
						break;
					}
			} else if (entity.getPersistentDataContainer().has(EntityHandler.removalKey, PersistentDataType.BYTE)) {
				for (Entity pass : entity.getPassengers())
					pass.remove();
				entity.remove();
			}
	}
	@EventHandler
	public void onEntitiesUnload(EntitiesUnloadEvent e) {
		for (Entity entity : e.getEntities())
			if (entity.getPersistentDataContainer().has(handler.globalKey, PersistentDataType.BYTE)) {
				CustomEntity customEntity = handler.findEntity((LivingEntity) entity);
				if (customEntity == null) {
					entity.remove();
					return;
				}
				customEntity.clean();
				handler.removeEntity(customEntity);
			}
	}
}
