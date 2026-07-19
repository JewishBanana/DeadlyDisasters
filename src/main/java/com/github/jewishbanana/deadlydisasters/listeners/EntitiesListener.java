package com.github.jewishbanana.deadlydisasters.listeners;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.github.jewishbanana.deadlydisasters.DeadlyDisasters;
import com.github.jewishbanana.deadlydisasters.disasters.mob.BlackPlague;
import com.github.jewishbanana.deadlydisasters.disasters.weather.Blizzard;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;

public class EntitiesListener implements Listener {

	private static NamespacedKey removeKey;
	static {
		removeKey = new NamespacedKey(DeadlyDisasters.getInstance(), "ddrk");
	}
	
	public EntitiesListener(DeadlyDisasters plugin) {
		plugin.getServer().getWorlds().forEach(world -> world.getEntities().forEach(this::loadEntity));
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler
	public void onEntitiesLoad(EntitiesLoadEvent event) {
		event.getEntities().forEach(this::loadEntity);
	}
	@EventHandler
	public void onEntitiesUnload(EntitiesUnloadEvent event) {
		event.getEntities().forEach(entity -> {
			BlackPlague.persistAndUnloadEntity(entity);
			PersistentDataContainer container = entity.getPersistentDataContainer();
			if (container.has(Blizzard.frozenEntityKey, PersistentDataType.BYTE))
				Blizzard.frozenEntities.remove(entity);
		});
	}
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		BlackPlague.restorePersistentInfection(event.getPlayer());
	}
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		BlackPlague.persistAndUnloadEntity(event.getPlayer());
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFallingBlockForm(EntityChangeBlockEvent event) {
		if (event.getEntityType() == EntityType.FALLING_BLOCK && event.getEntity().hasMetadata("dd-fb") && DependencyUtils.isRegionProtected(event.getBlock().getLocation()))
			event.setCancelled(true);
	}
	public static void attachRemoveKey(Entity entity) {
		if (entity != null && entity.getType() != EntityType.PLAYER)
			entity.getPersistentDataContainer().set(removeKey, PersistentDataType.BYTE, (byte) 0);
	}
	public static void detachRemoveKey(Entity entity) {
		if (entity != null)
			entity.getPersistentDataContainer().remove(removeKey);
	}
	private void loadEntity(Entity entity) {
		PersistentDataContainer container = entity.getPersistentDataContainer();
		if (removeEntityIfMarked(entity, container))
			return;
		if (entity instanceof LivingEntity living && container.has(Blizzard.frozenEntityKey, PersistentDataType.BYTE))
			Blizzard.frozenEntities.add(living);
		BlackPlague.restorePersistentInfection(entity);
		BlackPlague.restoreHostileGoal(entity);
	}
	private static boolean removeEntityIfMarked(Entity entity, PersistentDataContainer container) {
		if (!container.has(removeKey, PersistentDataType.BYTE))
			return false;
		if (entity.getType() == EntityType.PLAYER) {
			container.remove(removeKey);
			return true;
		}
		entity.remove();
		return true;
	}
}
