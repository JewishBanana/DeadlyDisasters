package com.github.jewishbanana.deadlydisasters.events;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public enum Disaster {
	SINKHOLE("&eSinkHole", 2),
	CAVEIN("&7Cave In", 0.2),
	TORNADO("&fTornado", 1),
	GEYSER("&9Water Geyser/&cLava Geyser", 0.5),
	PLAGUE("&0Black Plague", 0),
	ACIDSTORM("&aAcid Storm", 0.5),
	EXTREMEWINDS("&fExtreme Winds", 0.3),
	SOULSTORM("&3Soul Storm", 0),
	BLIZZARD("&9Blizzard", 0.05),
	SANDSTORM("&eSandstorm", 0),
	EARTHQUAKE("&8Earthquake", 6),
	TSUNAMI("&1Tsunami", 0.5),
	METEORSHOWERS("&5Meteor Shower", 3),
	ENDSTORM("&5End Storm", 0),
	SUPERNOVA("&3Supernova", 25),
	HURRICANE("&7Hurricane", 0.3),
	PURGE("&8Purge", 0),
	SOLARSTORM("&eSolar Storm", 1),
	MONSOON("&9Monsoon", 0.7),
	INFESTEDCAVES("&3Infested Caves", 3),
	LANDSLIDE("&eLand Slide/&bAvalanche", 0.2),
	CUSTOM("&fCustom", 5);
	
	private String label,tip,metricsLabel;
	private int maxLevel,delayTicks,minHeight;
	private double frequency,defaultRegenTickRate;
	
	private static final Disaster[] copyOfValues = values();
	 
    private Disaster(String label, double regenTickRate) {
        this.label = label;
        this.metricsLabel = label.substring(2);
        this.defaultRegenTickRate = regenTickRate;
    }
    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
    	this.label = label;
    }
	public int getMaxLevel() {
		return maxLevel;
	}
	public void setMaxLevel(int maxLevel) {
		this.maxLevel = maxLevel;
	}
	public int getDelayTicks() {
		return delayTicks;
	}
	public void setDelayTicks(int delayTicks) {
		this.delayTicks = delayTicks;
	}
	public int getMinHeight() {
		return minHeight;
	}
	public void setMinHeight(int minHeight) {
		this.minHeight = minHeight;
	}
	public double getFrequency() {
		return frequency;
	}
	public void setFrequency(double frequency) {
		this.frequency = frequency;
	}
	public double getDefaultRegenTickRate() {
		return defaultRegenTickRate;
	}
	public void setDefaultRegenTickRate(double defaultRegenTickRate) {
		this.defaultRegenTickRate = defaultRegenTickRate;
	}
	public String getTip() {
		return tip;
	}
	public void setTip(String tip) {
		this.tip = tip;
	}
	public String getMetricsLabel() {
		return metricsLabel;
	}
	public void setMetricsLabel(String metricsLabel) {
		this.metricsLabel = metricsLabel;
	}
	public static void reload(Main plugin) {
		for (Disaster obj : Disaster.values()) {
			if (obj == Disaster.CUSTOM)
				continue;
			if (obj != Disaster.PURGE) {
				if (!Main.isSpigot())
					obj.setLabel(Utils.chat(plugin.getConfig().getString(obj.name().toLowerCase()+".name")));
				else
					obj.setLabel(Utils.translateTextColor(plugin.getConfig().getString(obj.name().toLowerCase()+".name")));
			}
			obj.setMaxLevel(plugin.getConfig().getInt(obj.name().toLowerCase()+".max_level"));
			if (obj != Disaster.INFESTEDCAVES)
				obj.setDelayTicks(plugin.getConfig().getInt(obj.name().toLowerCase()+".start_delay") * 20);
			obj.setFrequency(plugin.getConfig().getDouble(obj.name().toLowerCase()+".frequency"));
			if (plugin.getConfig().contains(obj.name().toLowerCase()+".min_height"))
				obj.setMinHeight(plugin.getConfig().getInt(obj.name().toLowerCase()+".min_height"));
			if (Languages.langFile.contains("tips."+obj.name().toLowerCase()))
				obj.setTip(Utils.chat("&7&o")+Languages.getString("tips."+obj.name().toLowerCase()));
		}
	}
	public static Disaster forName(String name) {
		for (Disaster value : copyOfValues)
			if (value.name().equals(name))
				return value;
		return null;
	}
}
