package deadlydisasters.entities;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.NamespacedKey;

import deadlydisasters.general.Main;

public enum CustomEntityType {
	
	ENDTOTEM("customentities.endstorm_mobs.endtotem", "endtotem"),
	BABYENDTOTEM("customentities.pets.baby_endtotem", "babyendtotem"),
	ENDWORM("customentities.endstorm_mobs.endworm", "endworm"),
	VOIDARCHER("customentities.endstorm_mobs.voidarcher", "voidarcher"),
	VOIDGUARDIAN("customentities.endstorm_mobs.voidguardian", "voidguardian"),
	VOIDSTALKER("customentities.endstorm_mobs.voidstalker", "voidstalker"),
	DARKMAGE("customentities.purge_mobs.darkmage", "darkmage"),
	PRIMEDCREEPER("customentities.purge_mobs.primedcreeper", "primedcreeper"),
	SKELETONKNIGHT("customentities.purge_mobs.skeletonknight", "skeletonknight"),
	TUNNELLER("customentities.purge_mobs.tunneller", "tunnellerzombie"),
	SWAMPBEAST("customentities.purge_mobs.swampbeast", "swampbeast"),
	ZOMBIEKNIGHT("customentities.purge_mobs.zombieknight", "zombieknight"),
	SHADOWLEECH("customentities.purge_mobs.shadowleech", "shadowleech"),
	ANCIENTMUMMY("customentities.sandstorm_mobs.ancientmummy", "ancientmummy"),
	ANCIENTSKELETON("customentities.sandstorm_mobs.ancientskeleton", "ancientskeleton"),
	LOSTSOUL("customentities.soulstorm_mobs.lostsoul", "lostsoul"),
	TAMEDLOSTSOUL("customentities.pets.tamed_lostsoul", "tamedlostsoul"),
	SOULREAPER("customentities.soulstorm_mobs.soulreaper", "soulreaper"),
	YETI("customentities.snowstorm_mobs.yeti", "yeti"),
	FIREPHANTOM("customentities.solarstorm_mobs.firephantom", "firephantom"),
	CURSEDDIVER("customentities.monsoon_mobs.cursed_diver", "curseddiver"),
	INFESTEDSKELETON("customentities.infestedcaves_mobs.infested_skeleton", "infestedskeleton"),
	INFESTEDZOMBIE("customentities.infestedcaves_mobs.infested_zombie", "infestedzombie"),
	INFESTEDCREEPER("customentities.infestedcaves_mobs.infested_creeper", "infestedcreeper"),
	INFESTEDENDERMAN("customentities.infestedcaves_mobs.infested_enderman", "infestedenderman"),
	INFESTEDSPIRIT("customentities.infestedcaves_mobs.infested_spirit", "infestedspirit"),
	INFESTEDTRIBESMAN("customentities.infestedcaves_mobs.infested_tribesman", "infestedtribesman"),
	INFESTEDDEVOURER("customentities.infestedcaves_mobs.infested_devourer", "infesteddevourer"),
	INFESTEDHOWLER("customentities.infestedcaves_mobs.infested_howler", "infestedhowler"),
	INFESTEDWORM("customentities.infestedcaves_mobs.infested_worm", "infestedworm");
	
	private double health, damage, spawnRate;
	private boolean spawning;
	public Main plugin;
	public String configPath, species;
	public NamespacedKey nameKey;
	private List<String> dropsList;
	
	private CustomEntityType(String configPath, String species) {
		this.configPath = configPath;
		this.species = species;
	}
	public static void reload(Main plugin) {
		for (CustomEntityType temp : values()) {
			temp.plugin = plugin;
			temp.nameKey = new NamespacedKey(plugin, temp.species);
			temp.resetValues();
			if (plugin.getConfig().contains(temp.configPath+".health"))
				temp.setHealth(plugin.getConfig().getDouble(temp.configPath+".health"));
			if (plugin.getConfig().contains(temp.configPath+".damage"))
				temp.setDamage(plugin.getConfig().getDouble(temp.configPath+".damage"));
			if (plugin.getConfig().getBoolean("customentities.allow_custom_mobs") && plugin.getConfig().contains(temp.configPath+".spawning"))
				temp.setSpawning(plugin.getConfig().getBoolean(temp.configPath+".spawning"));
			else
				temp.setSpawning(false);
			if (plugin.getConfig().contains(temp.configPath+".spawnrate"))
				temp.setSpawnRate(plugin.getConfig().getDouble(temp.configPath+".spawnrate"));
			else
				temp.setSpawnRate(0);
			if (plugin.getConfig().getBoolean("customentities.allow_custom_drops") && plugin.getConfig().contains(temp.configPath+".drops"))
				temp.setDropsList(plugin.getConfig().getStringList(temp.configPath+".drops"));
			else
				temp.setDropsList(new ArrayList<String>());
		}
	}
	public Object grabCustomSetting(String field) {
		return plugin.getConfig().get(configPath+'.'+field);
	}
	public void resetValues() {
		spawning = false;
		spawnRate = 0;
	}
	public double getHealth() {
		return health;
	}
	public void setHealth(double health) {
		this.health = health;
	}
	public double getDamage() {
		return damage;
	}
	public void setDamage(double damage) {
		this.damage = damage;
	}
	public double getSpawnRate() {
		return spawnRate;
	}
	public void setSpawnRate(double spawnRate) {
		this.spawnRate = spawnRate;
	}
	public boolean canSpawn() {
		return spawning;
	}
	public void setSpawning(boolean spawning) {
		this.spawning = spawning;
	}
	public List<String> getDropsList() {
		return dropsList;
	}
	public void setDropsList(List<String> dropsList) {
		this.dropsList = dropsList;
	}
}
