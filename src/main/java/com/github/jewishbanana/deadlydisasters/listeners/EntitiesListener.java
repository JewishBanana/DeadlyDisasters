package com.github.jewishbanana.deadlydisasters.listeners;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.disasters.weather.Blizzard;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;

public class EntitiesListener implements Listener {

	private static NamespacedKey removeKey;
	static {
		removeKey = new NamespacedKey(Main.getInstance(), "ddrk");
	}
	
	public EntitiesListener(Main plugin) {
		plugin.getServer().getWorlds().forEach(world -> world.getEntities().stream().forEach(entity -> {
			PersistentDataContainer container = entity.getPersistentDataContainer();
			if (container.has(removeKey, PersistentDataType.BYTE)) {
				entity.remove();
				return;
			}
			if (container.has(Blizzard.frozenEntityKey, PersistentDataType.BYTE))
				Blizzard.frozenEntities.add((LivingEntity) entity);
		}));
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler
	public void onEntitiesLoad(EntitiesLoadEvent event) {
		event.getEntities().forEach(entity -> {
			PersistentDataContainer container = entity.getPersistentDataContainer();
			if (container.has(removeKey, PersistentDataType.BYTE)) {
				entity.remove();
				return;
			}
			if (container.has(Blizzard.frozenEntityKey, PersistentDataType.BYTE))
				Blizzard.frozenEntities.add((LivingEntity) entity);
		});
	}
	@EventHandler
	public void onEntitiesUnload(EntitiesUnloadEvent event) {
		event.getEntities().forEach(entity -> {
			PersistentDataContainer container = entity.getPersistentDataContainer();
			if (container.has(Blizzard.frozenEntityKey, PersistentDataType.BYTE))
				Blizzard.frozenEntities.remove(entity);
		});
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFallingBlockForm(EntityChangeBlockEvent event) {
		if (event.getEntityType() == EntityType.FALLING_BLOCK && event.getEntity().hasMetadata("dd-fb") && DependencyUtils.isRegionProtected(event.getBlock().getLocation()))
			event.setCancelled(true);
	}
	public static void attachRemoveKey(Entity entity) {
		if (entity != null)
			entity.getPersistentDataContainer().set(removeKey, PersistentDataType.BYTE, (byte) 0);
	}
}
