package com.github.jewishbanana.deadlydisasters.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.github.jewishbanana.deadlydisasters.DeadlyDisasters;
import com.github.jewishbanana.deadlydisasters.disasters.Disaster;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class DeathMessageHandler implements Listener {

	private static final DeadlyDisasters plugin;
	private static final Set<DeathWatcher> watchers;
	static {
		plugin = DeadlyDisasters.getInstance();
		watchers = new HashSet<>();
	}
	
	private Map<String, String> deathMessages = new HashMap<>();
	
	public DeathMessageHandler(DeadlyDisasters plugin) {
		try {
			for (String s : DataUtils.getLanguageConfig().getConfigurationSection("deaths").getKeys(false))
				deathMessages.put("deaths."+s, Utils.convertString(DataUtils.getLanguageString("deaths."+s)));
		} catch (Exception e) {
			Utils.sendExceptionLog(e);
		}
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		for (DeathWatcher watcher : watchers)
			if (watcher.function.apply(event)) {
				player.setMetadata(watcher.languagePath, plugin.getFixedMetadata());
				break;
			}
		
		for (Map.Entry<String, String> entry : deathMessages.entrySet()) {
			if (!player.hasMetadata(entry.getKey()))
				continue;
			String killer = "Unknown";
			if (player.getLastDamageCause() != null && player.getLastDamageCause() instanceof EntityDamageByEntityEvent damageEvent) {
				if (damageEvent.getDamager() != null) {
					if (damageEvent.getDamager() instanceof Player damager)
						killer = Utils.convertString(damager.getDisplayName());
					else if (damageEvent.getDamager() instanceof Projectile projectile) {
						if (projectile.getShooter() != null && projectile.getShooter() instanceof LivingEntity shooter) {
							if (projectile.getShooter() instanceof Player shootingPlayer)
								killer = Utils.convertString(shootingPlayer.getDisplayName());
							else
								killer = shooter.getCustomName() != null ? Utils.convertString(shooter.getCustomName()) : shooter.getType().getEntityClass().getName();
						} else
							killer = damageEvent.getDamager().getType().getEntityClass().getName();
					} else
						killer = damageEvent.getDamager().getCustomName() != null ? Utils.convertString(damageEvent.getDamager().getCustomName()) : damageEvent.getDamager().getType().getEntityClass().getName();
				}
			}
			event.setDeathMessage(Utils.convertString(entry.getValue().replace("%player%", player.getDisplayName())
					.replace("%killer%", player.getKiller() == null ? killer : player.getKiller().getDisplayName())));
			player.removeMetadata(entry.getKey(), plugin);
			return;
		}
	}
	// (The Purge tags "deaths.purge" on the player itself from its own per-disaster damage handler.)
	public static void createWatcher(Disaster disaster, String languagePath, Function<PlayerDeathEvent, Boolean> function) {
		watchers.add(new DeathWatcher(disaster, languagePath, function));
	}
	public static void removeDeathWatcher(Disaster disaster, int delayTicks) {
		if (DeadlyDisasters.isDisablingPlugin)
			return;
		for (DeathWatcher watcher : watchers)
			if (watcher.disaster.equals(disaster)) {
				plugin.getServer().getScheduler().runTaskLater(plugin, () -> watchers.remove(watcher), delayTicks);
				return;
			}
	}
	private static class DeathWatcher {
		
		private Disaster disaster;
		private String languagePath;
		private Function<PlayerDeathEvent, Boolean> function;
		
		private DeathWatcher(Disaster disaster, String languagePath, Function<PlayerDeathEvent, Boolean> function) {
			this.disaster = disaster;
			this.languagePath = languagePath;
			this.function = function;
		}
	}
}
