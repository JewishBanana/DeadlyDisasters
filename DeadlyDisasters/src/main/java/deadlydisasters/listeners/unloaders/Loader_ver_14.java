package deadlydisasters.listeners.unloaders;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataType;

import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.EntityHandler;
import deadlydisasters.general.Main;
import deadlydisasters.utils.Utils;

public class Loader_ver_14 implements Listener {
	
	private Main plugin;
	private EntityHandler handler;
	private Set<Chunk> chunks = new HashSet<>();
	private boolean debug;
	
	public Loader_ver_14(Main plugin, EntityHandler handler) {
		this.plugin = plugin;
		this.handler = handler;
		this.debug = plugin.getConfig().getBoolean("general.debug_messages");
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler
	public void onEntitiesLoad(ChunkLoadEvent e) {
		if (chunks.contains(e.getChunk()))
			return;
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
			for (Entity entity : e.getChunk().getEntities())
				if (entity.isValid() && entity.getPersistentDataContainer().has(handler.globalKey, PersistentDataType.BYTE)) {
					for (CustomEntityType type : CustomEntityType.values())
						if (entity.getPersistentDataContainer().has(type.nameKey, PersistentDataType.BYTE)) {
							handler.addEntityBySpecies(type.species, entity);
							break;
						}
				} else if (entity.getPersistentDataContainer().has(EntityHandler.removalKey, PersistentDataType.BYTE))
					entity.remove();
		}, 20);
	}
	@EventHandler
	public void onEntitiesUnload(ChunkUnloadEvent e) {
		chunks.add(e.getChunk());
		for (Entity entity : e.getChunk().getEntities())
			if (entity.getPersistentDataContainer().has(handler.globalKey, PersistentDataType.BYTE)) {
				CustomEntity customEntity = handler.findEntity((LivingEntity) entity);
				if (customEntity == null) {
					if (debug)
						Main.consoleSender.sendMessage(Utils.chat("&e[DeadlyDisasters]: Failed to unload custom entity of a "+entity.getType()));
					return;
				}
				customEntity.clean();
				handler.removeEntity(customEntity);
			}
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> chunks.remove(e.getChunk()), 60);
	}
}
