package deadlydisasters.general;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

import org.apache.commons.io.FileUtils;

import deadlydisasters.utils.ConfigUpdater;
import deadlydisasters.utils.Utils;

public class ConfigSwapper {
	
	private Main plugin;
	public String currentConfig;
	
	public Queue<String> configTemplates = new ArrayDeque<>();
	
	public ConfigSwapper(Main plugin) {
		this.plugin = plugin;
		
		File folder = new File(plugin.getDataFolder().getAbsolutePath(), "pluginData/configs");
		folder.mkdirs();
		for (File f : folder.listFiles())
			configTemplates.add(f.getName().substring(0, f.getName().indexOf('.')));
		configTemplates.addAll(Arrays.asList("DEFAULT","PERFORMANCE"));
		
		if (plugin.dataFile.contains("data.currentConfig") && configTemplates.contains(plugin.dataFile.getString("data.currentConfig")))
			this.currentConfig = plugin.dataFile.getString("data.currentConfig");
		else
			this.currentConfig = "DEFAULT";
	}
	public void saveConfig(String name) {
		File newConfig = new File(plugin.getDataFolder().getAbsolutePath(), "pluginData/configs/"+name+".yml");
		newConfig.getParentFile().mkdirs();
		try {
			FileUtils.copyFile(new File(plugin.getDataFolder().getAbsolutePath(), "config.yml"), newConfig);
		} catch (IOException e) {
			if (plugin.debug) {
				e.printStackTrace();
				Utils.sendDebugMessage();
			}
			return;
		}
		configTemplates.add(name);
	}
	public boolean swapConfigs(String template) {
		InputStream newConfig = null;
		try {
			switch (template) {
			case "DEFAULT":
				newConfig = plugin.getResource("config.yml");
				break;
			case "PERFORMANCE":
				newConfig = plugin.getResource("files/configs/performanceConfig.yml");
				break;
			default:
				newConfig = new FileInputStream(new File(plugin.getDataFolder().getAbsolutePath(), "pluginData/configs/"+template+".yml"));
				break;
			}
		} catch (FileNotFoundException e) {
			Main.consoleSender.sendMessage(Utils.chat("&c[DeadlyDisasters]: File '"+template+"' does not exist in configs folder!"));
			return false;
		}
		if (currentConfig != null && new File(plugin.getDataFolder().getAbsolutePath(), "pluginData/configs/"+currentConfig+".yml").exists()) {
			File prev = new File(plugin.getDataFolder().getAbsolutePath(), "pluginData/configs/"+currentConfig+".yml");
			try {
				FileUtils.copyFile(new File(plugin.getDataFolder().getAbsolutePath(), "config.yml"), prev);
			} catch (IOException e) {
				if (plugin.debug) {
					e.printStackTrace();
					Utils.sendDebugMessage();
				}
				return false;
			}
		} else {
			File backup = new File(plugin.getDataFolder().getAbsolutePath(), "pluginData/configs/backupConfig.yml");
			try {
				FileUtils.copyFile(new File(plugin.getDataFolder().getAbsolutePath(), "config.yml"), backup);
			} catch (IOException e) {
				if (plugin.debug) {
					e.printStackTrace();
					Utils.sendDebugMessage();
				}
				return false;
			}
		}
		try {
			FileUtils.copyInputStreamToFile(newConfig, new File(plugin.getDataFolder().getAbsolutePath(), "config.yml"));
			plugin.reloadConfig();
			Utils.reloadPlugin(plugin);
		} catch (IOException e) {
			if (plugin.debug) {
				e.printStackTrace();
				Utils.sendDebugMessage();
			}
			return false;
		}
		currentConfig = template;
		plugin.dataFile.set("data.currentConfig", currentConfig);
		return true;
	}
	public boolean deleteConfig(String template) {
		File newConfig = new File(plugin.getDataFolder().getAbsolutePath(), "pluginData/configs/"+template+".yml");
		if (!newConfig.exists())
			return false;
		newConfig.delete();
		if (currentConfig.equals(template))
			currentConfig = null;
		configTemplates.remove(template);
		return true;
	}
	public void updateConfigFolder() {
		File folder = new File(plugin.getDataFolder().getAbsolutePath(), "pluginData/configs");
		folder.mkdirs();
		for (File f : folder.listFiles())
			try {
				ConfigUpdater.update(plugin, Languages.fetchNewConfig(plugin, null), f, Arrays.asList(""));
			} catch (IOException e) {
				if (plugin.debug) {
					e.printStackTrace();
					Main.consoleSender.sendMessage(Utils.chat("&c[DeadlyDisasters]: Could not update config template '"+f.getName()+"' please report this bug to the discord!"));
				}
			}
	}
}
