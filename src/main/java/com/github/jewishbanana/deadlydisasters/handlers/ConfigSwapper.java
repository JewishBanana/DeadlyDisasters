package com.github.jewishbanana.deadlydisasters.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.utils.ConfigUpdater;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class ConfigSwapper {
	
	private Main plugin;
	public String currentConfig;
	
	public Set<String> configTemplates = new HashSet<>();
	
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
			Main.consoleSender.sendMessage(Utils.convertString("&c[DeadlyDisasters]: File '"+template+"' does not exist in configs folder!"));
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
					Main.consoleSender.sendMessage(Utils.convertString("&c[DeadlyDisasters]: Could not update config template '"+f.getName()+"' please report this bug to the discord!"));
				}
			}
	}
	public FileConfiguration getConfiguration(String config) {
		switch (config) {
		case "DEFAULT":
			return plugin.getConfig();
		case "PERFORMANCE":
			File performanceFile = new File(plugin.getDataFolder().getAbsolutePath(), "pluginData/configs/performanceConfig.yml");
			if (!performanceFile.exists()) {
				performanceFile.getParentFile().mkdirs();
				try {
					performanceFile.createNewFile();
					FileUtils.copyInputStreamToFile(plugin.getResource("files/configs/performanceConfig.yml"), performanceFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return YamlConfiguration.loadConfiguration(performanceFile);
		default:
			File cfg = new File(plugin.getDataFolder().getAbsolutePath(), "pluginData/configs/"+config+".yml");
			if (!cfg.exists())
				return null;
			return YamlConfiguration.loadConfiguration(cfg);
		}
	}
}
