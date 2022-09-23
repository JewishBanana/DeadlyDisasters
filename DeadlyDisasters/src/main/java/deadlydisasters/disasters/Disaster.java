package deadlydisasters.disasters;

import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;
import deadlydisasters.utils.Utils;

public enum Disaster {
	SINKHOLE("&eSinkHole"),
	CAVEIN("&7Cave In"),
	TORNADO("&fTornado"),
	GEYSER("&9Water Geyser/&cLava Geyser"),
	PLAGUE("&0Black Plague"),
	ACIDSTORM("&aAcid Storm"),
	EXTREMEWINDS("&fExtreme Winds"),
	SOULSTORM("&3Soul Storm"),
	BLIZZARD("&9Blizzard"),
	SANDSTORM("&eSandstorm"),
	EARTHQUAKE("&8Earthquake"),
	TSUNAMI("&1Tsunami"),
	METEORSHOWERS("&5Meteor Shower"),
	ENDSTORM("&5End Storm"),
	SUPERNOVA("&3Supernova"),
	HURRICANE("&7Hurricane"),
	PURGE("&8Purge"),
	CUSTOM("&fCustom");
	
	private String label,tip,metricsLabel;
	private int maxLevel,delayTicks,minHeight;
	private double frequency;
	
	private static final Disaster[] copyOfValues = values();
	 
    private Disaster(String label) {
        this.label = label;
        this.metricsLabel = label.substring(2);
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
			obj.setDelayTicks(plugin.getConfig().getInt(obj.name().toLowerCase()+".start_delay") * 20);
			obj.setFrequency(plugin.getConfig().getDouble(obj.name().toLowerCase()+".frequency"));
			if (plugin.getConfig().contains(obj.name().toLowerCase()+".min_height"))
				obj.setMinHeight(plugin.getConfig().getInt(obj.name().toLowerCase()+".min_height"));
			if (Languages.langFile.contains("tips."+obj.name().toLowerCase()))
				obj.setTip(Utils.chat("&7&o")+Languages.langFile.getString("tips."+obj.name().toLowerCase()));
		}
	}
	public static Disaster forName(String name) {
		for (Disaster value : copyOfValues)
			if (value.name().equals(name))
				return value;
		return null;
	}
}
