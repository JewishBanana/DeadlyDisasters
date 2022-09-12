package deadlydisasters.general;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import deadlydisasters.disasters.Disaster;
import deadlydisasters.utils.ConfigUpdater;
import deadlydisasters.utils.Utils;

public class SeasonsHandler {
	
	public boolean isActive;
	public int blizzTemp;
	
	private static me.casperge.realisticseasons.api.SeasonsAPI SeasonsAPI;
	private Main plugin;
	
	private FileConfiguration seasonFile;
	
	public Map<Disaster, Set<me.casperge.realisticseasons.season.Season>> seasonMap = new HashMap<>();
	
	public SeasonsHandler(Main plugin) {
		if (plugin.getServer().getPluginManager().getPlugin("RealisticSeasons") == null)
			return;
		this.plugin = plugin;
		plugin.getLogger().info("Successfully hooked into RealisticSeasons");
		if (!new File(plugin.getDataFolder().getAbsolutePath(), "seasons.yml").exists())
			createFile();
		seasonFile = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder().getAbsolutePath(), "seasons.yml"));
		if (!seasonFile.getBoolean("general.enabled")) {
			plugin.getLogger().info("RealisticSeasons is detected but this feature is disabled in the seasons.yml file!");
			return;
		}
		plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
			public void run() {
				isActive = true;
				SeasonsAPI = me.casperge.realisticseasons.api.SeasonsAPI.getInstance();
				readMapValues();
				blizzTemp = seasonFile.getInt("general.min_blizzard_temperature");
			}
		}, 1);
	}
	private void createFile() {
		plugin.getLogger().info("Could not find seasons file in plugin directory! Creating new seasons file...");
		try {
			FileUtils.copyInputStreamToFile(plugin.getResource("files/seasons.yml"), new File(plugin.getDataFolder().getAbsolutePath(), "seasons.yml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void updateSeasonsFile() {
		try {
			ConfigUpdater.update(plugin, plugin.getResource("files/seasons.yml"), new File(plugin.getDataFolder().getAbsolutePath(), "seasons.yml"), Arrays.asList(""));
			saveSeasonsFile(plugin);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void readMapValues() {
		core:
		for (Disaster temp : Disaster.values()) {
			if (temp == Disaster.SOULSTORM || temp == Disaster.ENDSTORM || temp == Disaster.CUSTOM)
				continue;
			List<String> list = seasonFile.getStringList("disasters."+temp.name());
			Set<me.casperge.realisticseasons.season.Season> set = new HashSet<>();
			if (list.contains("ALL"))
				set.addAll(Arrays.asList(me.casperge.realisticseasons.season.Season.SPRING, me.casperge.realisticseasons.season.Season.SUMMER, me.casperge.realisticseasons.season.Season.FALL, me.casperge.realisticseasons.season.Season.WINTER));
			else
				for (String s : list) {
					if (!(s.equals("SPRING") || s.equals("SUMMER") || s.equals("FALL") || s.equals("WINTER"))) {
						Main.consoleSender.sendMessage(Utils.chat(Languages.prefix+"&cSeason &6'"+s+"' &cdoes not exist! Something won't work right until you fix this line in seasons.yml:\n"
								+ "disasters\n  -> "+temp.name()+": "+Arrays.toString(list.toArray())));
						set.clear();
						set.addAll(Arrays.asList(me.casperge.realisticseasons.season.Season.SPRING, me.casperge.realisticseasons.season.Season.SUMMER, me.casperge.realisticseasons.season.Season.FALL, me.casperge.realisticseasons.season.Season.WINTER));
						seasonMap.put(temp, set);
						continue core;
					}
					set.add(me.casperge.realisticseasons.season.Season.valueOf(s));
				}
			seasonMap.put(temp, set);
		}
	}
	public void saveSeasonsFile(Main plugin) {
		try {
			seasonFile.save(new File(plugin.getDataFolder().getAbsolutePath(), "seasons.yml"));
		} catch (IOException e) {
			Main.consoleSender.sendMessage(Utils.chat("&c[DeadlyDisasters]: Error unable to save seasons file!"));
		}
	}
	public void reload(Main plugin) {
		seasonFile = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder().getAbsolutePath(), "seasons.yml"));
		seasonMap.clear();
		readMapValues();
	}
	public static me.casperge.realisticseasons.api.SeasonsAPI getSeasonsAPI() {
		return SeasonsAPI;
	}
}
