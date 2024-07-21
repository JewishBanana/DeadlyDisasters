package com.github.jewishbanana.deadlydisasters.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.events.Disaster;
import com.github.jewishbanana.deadlydisasters.utils.ConfigUpdater;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class Languages {
	
	public enum langFileLink {
		CHINESELANG("https://docs.google.com/uc?export=download&id=1Sw9qmw-lT2L6dazy25aVyBolsrasuV4D"),
		CHINESECONFIG("https://docs.google.com/uc?export=download&id=14CcSJ636YbJl3Wr-KhnNX1bZFf1zNvp6"),
		CHINESETRADLANG(""),
		CHINESETRADCONFIG(""),
		RUSSIANLANG("https://docs.google.com/uc?export=download&id=1V-SSOqGJZSNA4bUv0Wi-dJzVuqDFNB1D"),
		RUSSIANCONFIG("https://docs.google.com/uc?export=download&id=1W55tEu5cCUW1JIcOsPWzPmSps1Ra7cSu"),
		CZECHLANG("https://docs.google.com/uc?export=download&id=1qQZo-cnUJO-Ikdi1-fIQwtBnMz0Ex-ii"),
		CZECHCONFIG("https://docs.google.com/uc?export=download&id=1QJlGncTIyjLcDDOUAhKgMJ9lGGEZKHuo"),
		FRENCHLANG("https://docs.google.com/uc?export=download&id=1RMkZ9lzBpHNsDblMXOneIeQrCZfIZLK9"),
		FRENCHCONFIG("https://docs.google.com/uc?export=download&id=1fp-mLVZ5TKvICsNrzmC4rVEXXAb6gdt_");
		
		private URL source;
		private langFileLink(String link) {
			try {
				this.source = new URL(link);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		public URL getLink() {
			return source;
		}
	}
	
	private static int langID;
	
	public static YamlConfiguration langFile;
	public static YamlConfiguration defaultLang;
	private static InputStream cfgFile;
	
	public static String prefix = Utils.chat("&6&l[DeadlyDisasters]: ");
	public static String firstStart;
	
	public static String joinAfterUpdate = Utils.chat("&bUpdate log for &4&lV10.4 \\n&3- Small bug fixes\\n- Limited time easter event till 16th\\n- Custom world support");
	
	public static String getString(String path) {
		return langFile.contains(path) ? langFile.getString(path) : defaultLang.getString(path);
	}
	public static void updateLang(int tempID, Main plugin, Player sender) {
		langID = tempID;
		if (langID == 0) {
			if (sender != null)
				sender.sendMessage(Utils.chat("&bInstalling language files..."));
			langFile = YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource("lang/langEnglish.yml")));
			cfgFile = plugin.getResource("config.yml");
			if (sender != null)
				sender.sendMessage(Utils.chat("&aSuccessfully installed!"));
		} else if (langID == 1) {
			//chinese simp
			try {
				fetchNewConfig(plugin, sender);
				if (sender != null)
					sender.sendMessage(Utils.chat("&bInstalling language files..."));
				langFile = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder().getAbsolutePath()+File.separator+"languages", "langChinesePRO.yml"));
				cfgFile = FileUtils.openInputStream(new File(plugin.getDataFolder().getAbsolutePath()+File.separator+"languages", "langChineseCfgPRO.yml"));
				if (sender != null)
					sender.sendMessage(Utils.chat("&aSuccessfully installed!"));
			} catch (IOException e) {
				e.printStackTrace();
				Main.consoleSender.sendMessage(Utils.chat(Languages.prefix+"&cUnable to update language! Please report the full error above to the discord!"));
			}
		} else if (langID == 2) {
			//russian
			try {
				fetchNewConfig(plugin, sender);
				if (sender != null)
					sender.sendMessage(Utils.chat("&bInstalling language files..."));
				langFile = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder().getAbsolutePath()+File.separator+"languages", "langRussian.yml"));
				cfgFile = FileUtils.openInputStream(new File(plugin.getDataFolder().getAbsolutePath()+File.separator+"languages", "langRussianCfg.yml"));
				if (sender != null)
					sender.sendMessage(Utils.chat("&aSuccessfully installed!"));
			} catch (IOException e) {
				e.printStackTrace();
				Main.consoleSender.sendMessage(Utils.chat(Languages.prefix+"&cUnable to update language! Please report the full error above to the discord!"));
			}
		} else if (langID == 3) {
			//czech
			try {
				fetchNewConfig(plugin, sender);
				if (sender != null)
					sender.sendMessage(Utils.chat("&bInstalling language files..."));
				langFile = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder().getAbsolutePath()+File.separator+"languages", "langCzech.yml"));
				cfgFile = FileUtils.openInputStream(new File(plugin.getDataFolder().getAbsolutePath()+File.separator+"languages", "langCzechCfg.yml"));
				if (sender != null)
					sender.sendMessage(Utils.chat("&aSuccessfully installed!"));
			} catch (IOException e) {
				e.printStackTrace();
				Main.consoleSender.sendMessage(Utils.chat(Languages.prefix+"&cUnable to update language! Please report the full error above to the discord!"));
			}
		} else if (langID == 4) {
			//french
			try {
				fetchNewConfig(plugin, sender);
				if (sender != null)
					sender.sendMessage(Utils.chat("&bInstalling language files..."));
				langFile = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder().getAbsolutePath()+File.separator+"languages", "langFrench.yml"));
				cfgFile = FileUtils.openInputStream(new File(plugin.getDataFolder().getAbsolutePath()+File.separator+"languages", "langFrenchCfg.yml"));
				if (sender != null)
					sender.sendMessage(Utils.chat("&aSuccessfully installed!"));
			} catch (IOException e) {
				e.printStackTrace();
				Main.consoleSender.sendMessage(Utils.chat(Languages.prefix+"&cUnable to update language! Please report the full error above to the discord!"));
			}
		}
		
		prefix = Utils.chat("&6&l["+ langFile.getString("misc.prefix") +"]: ");
		firstStart = Utils.chat("&b"+langFile.getString("internal.firstStart.line 1")
				+ "\n&a/disasters disable randomdisasters"
				+ "\n&b"+langFile.getString("internal.firstStart.line 2")+" &a/disasters difficulty <world> <difficulty>"
				+ "\n&b"+langFile.getString("internal.firstStart.line 3")
				+ "\n&b"+langFile.getString("internal.firstStart.line 4")+" &e/disasters help");
		
		DifficultyLevel.reloadNames();
		ItemsHandler.refreshMetas(plugin);
	}
	public static void changeConfigLang(Main plugin) {
		plugin.reloadConfig();
		Map<String, Object> tempValues = new HashMap<>();
		
		for (Map.Entry<String, Object> entry : plugin.getConfig().getValues(true).entrySet())
			if (entry.getValue() instanceof String && !entry.getValue().toString().contains("MemorySection[path=")) {
				tempValues.put(entry.getKey()+"_delete", "This should not be here!");
				entry.setValue(null);
				tempValues.put(entry.getKey(), entry.getValue());
			}
		for (Map.Entry<String, Object> entry : tempValues.entrySet())
			plugin.getConfig().set(entry.getKey(), entry.getValue());
		plugin.saveConfig();
		try {
			ConfigUpdater.update(plugin, cfgFile, new File(plugin.getDataFolder().getAbsolutePath(), "config.yml"), Arrays.asList(""));
			plugin.reloadConfig();
			Disaster.reload(plugin);
		} catch (IOException e) {
			e.printStackTrace();
			Main.consoleSender.sendMessage(Utils.chat(Languages.prefix+"&cUnable to change config language! Please report the full error above to the discord."));
		}
	}
	public static InputStream fetchNewConfig(Main plugin, Player sender) throws IOException {
		if (langID == 0)
			return plugin.getResource("config.yml");
		else if (langID == 1) {
			File tempFile = new File(plugin.getDataFolder().getAbsolutePath()+File.separator+"languages", "langChinesePRO.yml");
			tempFile.getParentFile().mkdirs();
			YamlConfiguration yaml = YamlConfiguration.loadConfiguration(tempFile);
			File cfg = new File(plugin.getDataFolder().getAbsolutePath()+File.separator+"languages", "langChineseCfgPRO.yml");
			if (!cfg.exists() || !yaml.contains("version") || yaml.getDouble("version") < Double.parseDouble(plugin.getDescription().getVersion()))
				try {
					plugin.getLogger().info("Downloading config translation file...");
					if (sender != null)
						sender.sendMessage(Utils.chat("&eDownloading files..."));
					tempFile.createNewFile();
					//Utils.copyUrlToFile(langFileLink.CHINESELANG.getLink(), tempFile);
					FileUtils.copyInputStreamToFile(plugin.getResource("lang/langChinesePRO.yml"), tempFile);
					
					if (!cfg.exists())
						cfg.createNewFile();
					//Utils.copyUrlToFile(langFileLink.CHINESECONFIG.getLink(), cfg);
					FileUtils.copyInputStreamToFile(plugin.getResource("lang/langChineseCfgPRO.yml"), cfg);
					plugin.getLogger().info("Download Successful!");
				} catch (IOException e) {
					e.printStackTrace();
				}
			return new FileInputStream(cfg);
		} else if (langID == 2) {
			File tempFile = new File(plugin.getDataFolder().getAbsolutePath()+File.separator+"languages", "langRussian.yml");
			tempFile.getParentFile().mkdirs();
			YamlConfiguration yaml = YamlConfiguration.loadConfiguration(tempFile);
			File cfg = new File(plugin.getDataFolder().getAbsolutePath()+File.separator+"languages", "langRussianCfg.yml");
			if (!cfg.exists() || !yaml.contains("version") || yaml.getDouble("version") < Double.parseDouble(plugin.getDescription().getVersion()))
				try {
					plugin.getLogger().info("Downloading config translation file...");
					if (sender != null)
						sender.sendMessage(Utils.chat("&eDownloading files..."));
					tempFile.createNewFile();
					Utils.copyUrlToFile(langFileLink.RUSSIANLANG.getLink(), tempFile);
					
					if (!cfg.exists())
						cfg.createNewFile();
					Utils.copyUrlToFile(langFileLink.RUSSIANCONFIG.getLink(), cfg);
					plugin.getLogger().info("Download Successful!");
				} catch (IOException e) {
					e.printStackTrace();
				}
			return new FileInputStream(cfg);
		} else if (langID == 3) {
			File tempFile = new File(plugin.getDataFolder().getAbsolutePath()+File.separator+"languages", "langCzech.yml");
			tempFile.getParentFile().mkdirs();
			YamlConfiguration yaml = YamlConfiguration.loadConfiguration(tempFile);
			File cfg = new File(plugin.getDataFolder().getAbsolutePath()+File.separator+"languages", "langCzechCfg.yml");
			if (!cfg.exists() || !yaml.contains("version") || yaml.getDouble("version") < Double.parseDouble(plugin.getDescription().getVersion()))
				try {
					plugin.getLogger().info("Downloading config translation file...");
					if (sender != null)
						sender.sendMessage(Utils.chat("&eDownloading files..."));
					tempFile.createNewFile();
					Utils.copyUrlToFile(langFileLink.CZECHLANG.getLink(), tempFile);
					
					if (!cfg.exists())
						cfg.createNewFile();
					Utils.copyUrlToFile(langFileLink.CZECHCONFIG.getLink(), cfg);
					plugin.getLogger().info("Download Successful!");
				} catch (IOException e) {
					e.printStackTrace();
				}
			return new FileInputStream(cfg);
		} else if (langID == 4) {
			File tempFile = new File(plugin.getDataFolder().getAbsolutePath()+File.separator+"languages", "langFrench.yml");
			tempFile.getParentFile().mkdirs();
			YamlConfiguration yaml = YamlConfiguration.loadConfiguration(tempFile);
			File cfg = new File(plugin.getDataFolder().getAbsolutePath()+File.separator+"languages", "langFrenchCfg.yml");
			if (!cfg.exists() || !yaml.contains("version") || yaml.getDouble("version") < Double.parseDouble(plugin.getDescription().getVersion()))
				try {
					plugin.getLogger().info("Downloading config translation file...");
					if (sender != null)
						sender.sendMessage(Utils.chat("&eDownloading files..."));
					tempFile.createNewFile();
					Utils.copyUrlToFile(langFileLink.FRENCHLANG.getLink(), tempFile);
					
					if (!cfg.exists())
						cfg.createNewFile();
					Utils.copyUrlToFile(langFileLink.FRENCHCONFIG.getLink(), cfg);
					plugin.getLogger().info("Download Successful!");
				} catch (IOException e) {
					e.printStackTrace();
				}
			return new FileInputStream(cfg);
		}
		return null;
	}
}
