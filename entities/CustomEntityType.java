package deadlydisasters.entities;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.NamespacedKey;

import deadlydisasters.general.Main;

public enum CustomEntityType {
	
	ENDTOTEM("customentities.endstorm_mobs.endtotem", "endtotem", 'd'),
	BABYENDTOTEM("customentities.pets.baby_endtotem", "babyendtotem", 'd'),
	ENDWORM("customentities.endstorm_mobs.endworm", "endworm", 'd'),
	VOIDARCHER("customentities.endstorm_mobs.voidarcher", "voidarcher", 'd'),
	VOIDGUARDIAN("customentities.endstorm_mobs.voidguardian", "voidguardian", 'd'),
	VOIDSTALKER("customentities.endstorm_mobs.voidstalker", "voidstalker", 'd'),
	DARKMAGE("customentities.purge_mobs.darkmage", "darkmage", '8'),
	PRIMEDCREEPER("customentities.purge_mobs.primedcreeper", "primedcreeper", '8'),
	SKELETONKNIGHT("customentities.purge_mobs.skeletonknight", "skeletonknight", '8'),
	TUNNELLER("customentities.purge_mobs.tunneller", "tunnellerzombie", '8'),
	SWAMPBEAST("customentities.purge_mobs.swampbeast", "swampbeast", '8'),
	ZOMBIEKNIGHT("customentities.purge_mobs.zombieknight", "zombieknight", '8'),
	SHADOWLEECH("customentities.purge_mobs.shadowleech", "shadowleech", '8'),
	ANCIENTMUMMY("customentities.sandstorm_mobs.ancientmummy", "ancientmummy", 'e'),
	ANCIENTSKELETON("customentities.sandstorm_mobs.ancientskeleton", "ancientskeleton", 'e'),
	LOSTSOUL("customentities.soulstorm_mobs.lostsoul", "lostsoul", '3'),
	TAMEDLOSTSOUL("customentities.pets.tamed_lostsoul", "tamedlostsoul", '3'),
	SOULREAPER("customentities.soulstorm_mobs.soulreaper", "soulreaper", '3'),
	YETI("customentities.snowstorm_mobs.yeti", "yeti", '9'),
	FIREPHANTOM("customentities.solarstorm_mobs.firephantom", "firephantom", 'c'),
	CURSEDDIVER("customentities.monsoon_mobs.cursed_diver", "curseddiver", '1'),
	INFESTEDSKELETON("customentities.infestedcaves_mobs.infested_skeleton", "infestedskeleton", '3'),
	INFESTEDZOMBIE("customentities.infestedcaves_mobs.infested_zombie", "infestedzombie", '3'),
	INFESTEDCREEPER("customentities.infestedcaves_mobs.infested_creeper", "infestedcreeper", '3'),
	INFESTEDENDERMAN("customentities.infestedcaves_mobs.infested_enderman", "infestedenderman", '3'),
	INFESTEDSPIRIT("customentities.infestedcaves_mobs.infested_spirit", "infestedspirit", '3'),
	INFESTEDTRIBESMAN("customentities.infestedcaves_mobs.infested_tribesman", "infestedtribesman", '3'),
	INFESTEDDEVOURER("customentities.infestedcaves_mobs.infested_devourer", "infesteddevourer", '3'),
	INFESTEDHOWLER("customentities.infestedcaves_mobs.infested_howler", "infestedhowler", '3'),
	INFESTEDWORM("customentities.infestedcaves_mobs.infested_worm", "infestedworm", '3'),
	CHRISTMASELF("customentities.christmas_mobs.elf", "elf", 'c'),
	PETCHRISTMASELF("customentities.pets.pet_elf", "petelf", 'c'),
	FROSTY("customentities.christmas_mobs.frosty", "frosty", 'c'),
	GRINCH("customentities.christmas_mobs.grinch", "grinch", 'c'),
	SANTA("customentities.christmas_mobs.santa", "santa", 'c'),
	RAMPAGINGGOAT("customentities.easter_mobs.rampaging_goat", "rampaginggoat", 'a'),
	EASTERBUNNY("customentities.easter_mobs.easter_bunny", "easterbunny", 'a'),
	KILLERCHICKEN("customentities.easter_mobs.killer_chicken", "killerchicken", 'a');
	
	private double health, damage, spawnRate;
	private boolean spawning;
	public Main plugin;
	public String configPath, species;
	public NamespacedKey nameKey;
	private List<String> dropsList;
	private char colChar;
	
	private CustomEntityType(String configPath, String species, char colChar) {
		this.configPath = configPath;
		this.species = species;
		this.colChar = colChar;
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
			if (plugin.getConfig().contains(temp.configPath+".spawnrate") && temp.spawning)
				temp.setSpawnRate(plugin.getConfig().getDouble(temp.configPath+".spawnrate"));
			else
				temp.setSpawnRate(-1.0);
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
	public char getColChar() {
		return colChar;
	}
	public void setColChar(char colChar) {
		this.colChar = colChar;
	}
}
