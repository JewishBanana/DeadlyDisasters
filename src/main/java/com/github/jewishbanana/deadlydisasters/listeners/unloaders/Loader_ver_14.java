package com.github.jewishbanana.deadlydisasters.listeners.unloaders;

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

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.EntityHandler;

public class Loader_ver_14 implements Listener {
	
	private Main plugin;
	private EntityHandler handler;
	private Set<Chunk> chunks = new HashSet<>();
	
	public Loader_ver_14(Main plugin, EntityHandler handler) {
		this.plugin = plugin;
		this.handler = handler;
		
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
				} else if (entity.getPersistentDataContainer().has(EntityHandler.removalKey, PersistentDataType.BYTE)) {
					for (Entity pass : entity.getPassengers())
						pass.remove();
					entity.remove();
				}
		}, 40);
	}
	@EventHandler
	public void onEntitiesUnload(ChunkUnloadEvent e) {
		chunks.add(e.getChunk());
		for (Entity entity : e.getChunk().getEntities())
			if (entity.getPersistentDataContainer().has(handler.globalKey, PersistentDataType.BYTE)) {
				CustomEntity customEntity = handler.findEntity((LivingEntity) entity);
				if (customEntity == null)
					return;
				customEntity.clean();
				handler.removeEntity(customEntity);
			}
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> chunks.remove(e.getChunk()), 60);
	}
}
