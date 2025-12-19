package com.github.jewishbanana.deadlydisasters.disasters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.listeners.EntitiesListener;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public interface MobDisaster {

	public static final Map<MobDisaster, Set<UUID>> entitiesMap = new HashMap<>();
	public static final Map<UUID, UUID> entitiyTargets = new HashMap<>();
	
	default void addEntityToDisasterList(@NotNull Entity entity) {
		MobDisaster.entitiesMap.computeIfAbsent(this, k -> new HashSet<>()).add(entity.getUniqueId());
		EntitiesListener.attachRemoveKey(entity);
	}
	default void addEntityToDisasterList(@NotNull Entity entity, Entity target) {
		UUID uuid = entity.getUniqueId();
		MobDisaster.entitiesMap.computeIfAbsent(this, k -> new HashSet<>()).add(uuid);
		MobDisaster.entitiyTargets.put(uuid, target.getUniqueId());
		EntitiesListener.attachRemoveKey(entity);
	}
	default Set<UUID> getAllEntities() {
		return entitiesMap.get(this);
	}
	default void updateEntityTargets() {
		Set<UUID> set = entitiesMap.get(this);
		if (set == null)
			return;
		set.forEach(uuid -> {
			Mob entity = (Mob) Bukkit.getEntity(uuid);
			if (entity == null || entity.getTarget() != null)
				return;
			entity.setTarget(Bukkit.getPlayer(entitiyTargets.get(entity.getUniqueId())));
		});
	}
	default void cleanEntities() {
		Set<UUID> set = entitiesMap.get(this);
		if (set == null)
			return;
		if (Main.isDisablingPlugin) {
			set.forEach(uuid -> {
				Entity entity = Bukkit.getEntity(uuid);
				if (entity != null)
					entity.remove();
			});
			return;
		}
		set.forEach(k -> entitiyTargets.remove(k));
		new BukkitRunnable() {
			@Override
			public void run() {
				Iterator<UUID> it = set.iterator();
				while (it.hasNext()) {
					UUID uuid = it.next();
					Entity entity = Bukkit.getEntity(uuid);
					if (entity == null) {
						it.remove();
						continue;
					}
					if (entity instanceof Mob mob && Utils.isNotNullAndCondition(mob.getTarget(), t -> t instanceof Player))
						return;
					Location loc = entity.getLocation();
					if (entity.getWorld().getPlayers().stream().anyMatch(p -> p.getLocation().distanceSquared(loc) <= 400))
						return;
					entity.remove();
					it.remove();
				}
				if (set.isEmpty())
					this.cancel();
			}
		}.runTaskTimer(Disaster.plugin, 0, 20);
	}
	public static void cleanAllEntities() {
		entitiesMap.forEach((disaster, set) -> {
			set.forEach(uuid -> {
				Entity entity = Bukkit.getEntity(uuid);
				if (entity != null)
					entity.remove();
			});
		});
	}
}
