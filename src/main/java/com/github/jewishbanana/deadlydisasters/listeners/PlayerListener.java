package com.github.jewishbanana.deadlydisasters.listeners;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
	
	public static String adminUpdateMessage;
	
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
				if (adminUpdateMessage != null && player.isOp() && !updateNotified.contains(player.getUniqueId())) {
					player.sendMessage(adminUpdateMessage);
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
					.replace("%lowsecond%", formatForecastNumber(wrapper.minimumTime))
					.replace("%highsecond%", formatForecastNumber(wrapper.maximumTime))
					.replace("%lowminute%", formatForecastNumber(wrapper.minimumTime / 60.0))
					.replace("%highminute%", formatForecastNumber(wrapper.maximumTime / 60.0))
					.replace("%lowhour%", formatForecastNumber(wrapper.minimumTime / 60.0 / 60.0))
					.replace("%highhour%", formatForecastNumber(wrapper.maximumTime / 60.0 / 60.0))
					.replace("%offset%", formatForecastNumber(wrapper.disasterOffset))
					.replace("%preset%", wrapper.getPresetDisplayName())
					.replace("%level1%", formatForecastNumber(getLevelProbability(wrapper, 0)))
					.replace("%level2%", formatForecastNumber(getLevelProbability(wrapper, 1)))
					.replace("%level3%", formatForecastNumber(getLevelProbability(wrapper, 2)))
					.replace("%level4%", formatForecastNumber(getLevelProbability(wrapper, 3)))
					.replace("%level5%", formatForecastNumber(getLevelProbability(wrapper, 4)))
					.replace("%level6%", formatForecastNumber(getLevelProbability(wrapper, 5))));
		});
		player.sendMessage(Utils.convertString(builder.toString()));
		if (wrapper.getConfigBoolean("world.forecast.play_pling_sound"))
			player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.3f, 1f);
	}
	private static float getLevelProbability(WorldWrapper wrapper, int index) {
		if (wrapper.probabilityTable == null || wrapper.probabilityTable.length <= index)
			return 0f;
		return wrapper.probabilityTable[index];
	}
	private static String formatForecastNumber(double value) {
		return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}
}
