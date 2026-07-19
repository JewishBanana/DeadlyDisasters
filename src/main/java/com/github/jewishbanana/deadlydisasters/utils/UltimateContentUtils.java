package com.github.jewishbanana.deadlydisasters.utils;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.github.jewishbanana.deadlydisasters.DeadlyDisasters;

/**
 * Isolates all optional UltimateContent and UIFramework linkage from vanilla DeadlyDisasters code paths.
 * Callers must first verify that the relevant dependency is enabled through {@link DependencyUtils}.
 */
public final class UltimateContentUtils {
	private static Listener plagueBiteListener;

	private UltimateContentUtils() {
	}

	public static Class<?> getEntityClass(String id) {
		com.github.jewishbanana.uiframework.entities.UIEntityManager manager = com.github.jewishbanana.uiframework.entities.UIEntityManager.getEntityType(id);
		return manager == null ? null : manager.getEntityClass();
	}

	public static Entity spawnEntity(Location location, Class<?> entityClass) {
		com.github.jewishbanana.uiframework.entities.CustomEntity<?> customEntity = spawnCustomEntity(location, entityClass);
		if (customEntity == null || customEntity.getEntity() == null)
			return null;

		Entity entity = customEntity.getEntity();
		com.github.jewishbanana.uiframework.events.CustomEntitySpawnEvent event = new com.github.jewishbanana.uiframework.events.CustomEntitySpawnEvent(
				location, entity, customEntity, CreatureSpawnEvent.SpawnReason.CUSTOM);
		Bukkit.getPluginManager().callEvent(event);
		if (!event.isCancelled())
			return entity;

		com.github.jewishbanana.uiframework.entities.UIEntityManager.removeEntity(entity.getUniqueId());
		if (entity.getVehicle() != null)
			entity.getVehicle().remove();
		entity.remove();
		return null;
	}

	public static Entity spawnBreachMiner(Location location, Class<?> entityClass, Player target, double retentionRange) {
		com.github.jewishbanana.uiframework.entities.CustomEntity<?> customEntity = spawnCustomEntity(location, entityClass);
		if (customEntity == null || customEntity.getEntity() == null)
			return null;
		if (customEntity instanceof com.github.jewishbanana.ultimatecontent.entities.darkentities.UndeadMiner miner)
			miner.setBreachMode(target, retentionRange);
		return customEntity.getEntity();
	}

	public static ItemStack getItem(String id) {
		com.github.jewishbanana.uiframework.items.UIItemType itemType = com.github.jewishbanana.uiframework.items.UIItemType.getItemType(id);
		return itemType == null ? null : itemType.getItem();
	}

	public static void manipulatePlagueAggressionGoals(Mob entity, boolean aggressive, double damage, double speed, boolean preserveVillagerDoors) {
		com.github.jewishbanana.ultimatecontent.utils.EntityUtils.manipulatePlagueAggressionGoals(entity, aggressive, damage, speed, preserveVillagerDoors);
	}

	public static void clearPlagueAggressionGoals(Mob entity) {
		com.github.jewishbanana.ultimatecontent.utils.EntityUtils.clearPlagueAggressionGoals(entity);
	}

	public static void clearAllPlagueAggressionGoals() {
		com.github.jewishbanana.ultimatecontent.utils.EntityUtils.clearAllPlagueAggressionGoals();
	}

	public static float getBodyRotation(Mob entity) {
		return com.github.jewishbanana.ultimatecontent.utils.EntityUtils.getBodyRotation(entity);
	}

	public static void setBodyRotation(Mob entity, float rotation) {
		com.github.jewishbanana.ultimatecontent.utils.EntityUtils.setBodyRotation(entity, rotation);
	}

	public static Entity spawnPlagueRat(Location location) {
		return spawnRegisteredEntity(location, "uc:plague_rat");
	}

	public static boolean beginPlagueRatBurrow(Entity entity) {
		com.github.jewishbanana.uiframework.entities.CustomEntity<?> custom = com.github.jewishbanana.uiframework.entities.UIEntityManager.getEntity(entity);
		return custom instanceof com.github.jewishbanana.ultimatecontent.entities.darkentities.PlagueRat rat && rat.beginBurrowing();
	}

	public static void configurePlagueRat(Entity entity, LivingEntity target, boolean focusedTarget) {
		com.github.jewishbanana.uiframework.entities.CustomEntity<?> custom = com.github.jewishbanana.uiframework.entities.UIEntityManager.getEntity(entity);
		if (custom instanceof com.github.jewishbanana.ultimatecontent.entities.darkentities.PlagueRat rat)
			rat.setInitialTarget(target, focusedTarget);
	}

	public static Entity spawnPlagueBat(Location location) {
		return spawnRegisteredEntity(location, "uc:plague_bat");
	}

	public static void configurePlagueBat(Entity entity, LivingEntity target, boolean focusedTarget) {
		com.github.jewishbanana.uiframework.entities.CustomEntity<?> custom = com.github.jewishbanana.uiframework.entities.UIEntityManager.getEntity(entity);
		if (custom instanceof com.github.jewishbanana.ultimatecontent.entities.darkentities.PlagueBat bat)
			bat.setInitialTarget(target, focusedTarget);
	}

	public static boolean beginPlagueBatDeparture(Entity entity) {
		com.github.jewishbanana.uiframework.entities.CustomEntity<?> custom = com.github.jewishbanana.uiframework.entities.UIEntityManager.getEntity(entity);
		return custom instanceof com.github.jewishbanana.ultimatecontent.entities.darkentities.PlagueBat bat && bat.beginDeparture();
	}

	public static boolean isTamedOrOwned(Entity entity) {
		com.github.jewishbanana.uiframework.entities.CustomEntity<?> custom = com.github.jewishbanana.uiframework.entities.UIEntityManager.getEntity(entity);
		return custom instanceof com.github.jewishbanana.ultimatecontent.entities.TameableEntity tameable && tameable.getOwner() != null;
	}

	@SuppressWarnings("unchecked")
	public static boolean registerPlagueBiteListener(BiConsumer<LivingEntity, LivingEntity> handler) {
		if (plagueBiteListener != null)
			return true;
		Plugin ultimateContent = Bukkit.getPluginManager().getPlugin("UltimateContent");
		if (ultimateContent == null || !ultimateContent.isEnabled())
			return false;
		try {
			Class<?> rawEventClass = Class.forName("com.github.jewishbanana.ultimatecontent.events.PlagueCarrierBiteEvent", true,
					ultimateContent.getClass().getClassLoader());
			if (!Event.class.isAssignableFrom(rawEventClass))
				return false;
			Class<? extends Event> eventClass = (Class<? extends Event>) rawEventClass;
			Method getCarrier = rawEventClass.getMethod("getCarrier");
			Method getVictim = rawEventClass.getMethod("getVictim");
			Listener listener = new Listener() {
			};
			Bukkit.getPluginManager().registerEvent(eventClass, listener, EventPriority.MONITOR, (registered, event) -> {
				try {
					Object carrier = getCarrier.invoke(event);
					Object victim = getVictim.invoke(event);
					if (carrier instanceof LivingEntity && victim instanceof LivingEntity)
						handler.accept((LivingEntity) carrier, (LivingEntity) victim);
				} catch (ReflectiveOperationException ex) {
					throw new IllegalStateException("Could not read an UltimateContent plague bite event", ex);
				}
			}, DeadlyDisasters.getInstance(), true);
			plagueBiteListener = listener;
			return true;
		} catch (ReflectiveOperationException ex) {
			DeadlyDisasters.getInstance().getLogger().warning("UltimateContent plague bite integration could not be enabled: " + ex.getMessage());
			return false;
		}
	}

	private static Entity spawnRegisteredEntity(Location location, String id) {
		Class<?> entityClass = getEntityClass(id);
		if (entityClass == null)
			return null;
		com.github.jewishbanana.uiframework.entities.CustomEntity<?> custom = spawnCustomEntity(location, entityClass);
		return custom == null ? null : custom.getEntity();
	}

	@SuppressWarnings("unchecked")
	private static com.github.jewishbanana.uiframework.entities.CustomEntity<?> spawnCustomEntity(Location location, Class<?> entityClass) {
		if (location == null || entityClass == null)
			return null;
		return com.github.jewishbanana.uiframework.entities.UIEntityManager.spawnEntity(location,
				(Class<? extends com.github.jewishbanana.uiframework.entities.CustomEntity<?>>) entityClass);
	}
}

