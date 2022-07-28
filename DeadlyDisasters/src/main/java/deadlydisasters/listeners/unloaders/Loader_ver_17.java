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
import deadlydisasters.utils.Utils;

public class Loader_ver_17 implements Listener {
	
	private EntityHandler handler;
	private boolean debug;
	
	public Loader_ver_17(Main plugin, EntityHandler handler) {
		this.handler = handler;
		this.debug = plugin.getConfig().getBoolean("general.debug_messages");
		
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
			}
	}
	@EventHandler
	public void onEntitiesUnload(EntitiesUnloadEvent e) {
		for (Entity entity : e.getEntities())
			if (entity.getPersistentDataContainer().has(handler.globalKey, PersistentDataType.BYTE)) {
				CustomEntity customEntity = handler.findEntity((LivingEntity) entity);
				if (customEntity == null) {
					if (debug)
						Main.consoleSender.sendMessage(Utils.chat("&e[DeadlyDisasters]: Failed to unload custom entity of a "+entity.getType()));
					return;
				}
				customEntity.clean();
				handler.removeEntity(customEntity);
			} else if (entity.getPersistentDataContainer().has(EntityHandler.removalKey, PersistentDataType.BYTE))
				entity.remove();
	}
}
