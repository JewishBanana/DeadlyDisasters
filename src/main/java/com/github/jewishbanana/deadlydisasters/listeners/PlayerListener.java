package com.github.jewishbanana.deadlydisasters.listeners;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.WorldWrapper;
import com.github.jewishbanana.deadlydisasters.disasters.mob.Purge;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class PlayerListener implements Listener	{
	
	public static boolean notifyAdminOfUpdate;
	
	private final Main plugin;
	private final Set<UUID> warnForKick = new HashSet<>();
	private final Set<UUID> updateNotified = new HashSet<>();

	public PlayerListener(Main plugin) {
		this.plugin = plugin;
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		new BukkitRunnable() {
			@Override
			public void run() {
				sendWorldForecast(player);
				if (notifyAdminOfUpdate && player.isOp() && !updateNotified.contains(player.getUniqueId())) {
					player.sendMessage(Utils.convertString(Utils.prefix+DataUtils.getLanguageString("messages.internal.update_notify_player")));
					updateNotified.add(player.getUniqueId());
				}
				if (warnForKick.remove(player.getUniqueId()))
					player.sendMessage(Utils.convertString(Utils.prefix+DataUtils.getLanguageString("messages.internal.flight_kick")));
			}
		}.runTaskLater(plugin, 10);
		Purge.checkForPlayerInMap(player);
	}
	@EventHandler
	public void onKick(PlayerKickEvent event) {
		if (event.getReason().equals("Flying is not enabled on this server") && event.getPlayer().isOp())
			warnForKick.add(event.getPlayer().getUniqueId());
	}
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onTeleport(PlayerTeleportEvent event) {
		if (event.getFrom().getWorld().equals(event.getTo().getWorld()))
			return;
		new BukkitRunnable() {
			@Override
			public void run() {
				sendWorldForecast(event.getPlayer());
			}
		}.runTaskLater(plugin, 10);
	}
//	@EventHandler
//	public void onDebug(PlayerInteractEvent event) {
//		Block block = event.getPlayer().getLocation().getBlock();
//		event.getPlayer().sendMessage("temp is "+block.getWorld().getTemperature(block.getX(), block.getY(), block.getZ())+" hum is "+block.getWorld().getHumidity(block.getX(), block.getY(), block.getZ()));
//	}
	private void sendWorldForecast(Player player) {
		if (player == null || !player.isOnline())
			return;
		WorldWrapper wrapper = WorldWrapper.getWorldWrapper(player.getWorld());
		if (wrapper == null || !wrapper.getConfigBoolean("world.forecast.allow_world_messages") || (!wrapper.getConfigBoolean("world.forecast.show_world_messages_to_not_opped") && !player.isOp()))
			return;
		List<String> list = DataUtils.getLanguageStringList("messages.world_messages.world_forecast");
		if (list == null || list.isEmpty())
			return;
		StringBuilder builder = new StringBuilder();
		list.forEach(line -> {
			if (!builder.isEmpty())
				builder.append("\n");
			builder.append(line.replace("%world%", player.getWorld().getName())
					.replace("%targeting%", wrapper.targetingMode == 0 ? "&c&l" + DataUtils.getLanguageString("messages.status.disabled") : wrapper.targetingMode == 1 ? "&e&l" + DataUtils.getLanguageString("messages.status.targetIndividual") : "&a&l" + DataUtils.getLanguageString("messages.status.targetGlobal"))
					.replace("%lowsecond%", ""+wrapper.minimumTime)
					.replace("%highsecond%", ""+wrapper.maximumTime)
					.replace("%lowminute%", ""+(wrapper.minimumTime / 60.0))
					.replace("%highminute%", ""+(wrapper.maximumTime / 60.0))
					.replace("%lowhour%", ""+(wrapper.minimumTime / 60.0 / 60.0))
					.replace("%highhour%", ""+(wrapper.maximumTime / 60.0 / 60.0))
					.replace("%offset%", ""+wrapper.disasterOffset)
					.replace("%level1%", ""+wrapper.probabilityTable[0])
					.replace("%level2%", ""+wrapper.probabilityTable[1])
					.replace("%level3%", ""+wrapper.probabilityTable[2])
					.replace("%level4%", ""+wrapper.probabilityTable[3])
					.replace("%level5%", ""+wrapper.probabilityTable[4])
					.replace("%level6%", ""+wrapper.probabilityTable[5]));
		});
		player.sendMessage(Utils.convertString(builder.toString()));
		if (wrapper.getConfigBoolean("world.forecast.play_pling_sound"))
			player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.3f, 1f);
	}
}
