package com.github.jewishbanana.deadlydisasters.entities;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.NamespacedKey;

import com.github.jewishbanana.deadlydisasters.Main;

public enum CustomEntityType {
	
	ENDTOTEM("customentities.endstorm_mobs.endtotem", "endtotem", 'd', 30, 6),
	BABYENDTOTEM("customentities.pets.baby_endtotem", "babyendtotem", 'd', 20, 4),
	ENDWORM("customentities.endstorm_mobs.endworm", "endworm", 'd', 30, 6),
	VOIDARCHER("customentities.endstorm_mobs.voidarcher", "voidarcher", 'd', 20, 5),
	VOIDGUARDIAN("customentities.endstorm_mobs.voidguardian", "voidguardian", 'd', 40, 10),
	VOIDSTALKER("customentities.endstorm_mobs.voidstalker", "voidstalker", 'd', 20, 8),
	DARKMAGE("customentities.purge_mobs.darkmage", "darkmage", '8', 20, 6),
	PRIMEDCREEPER("customentities.purge_mobs.primedcreeper", "primedcreeper", '8', 20, 0),
	SKELETONKNIGHT("customentities.purge_mobs.skeletonknight", "skeletonknight", '8', 20, 6),
	TUNNELLER("customentities.purge_mobs.tunneller", "tunnellerzombie", '8', 30, 6),
	SWAMPBEAST("customentities.purge_mobs.swampbeast", "swampbeast", '8', 40, 10),
	ZOMBIEKNIGHT("customentities.purge_mobs.zombieknight", "zombieknight", '8', 30, 7),
	SHADOWLEECH("customentities.purge_mobs.shadowleech", "shadowleech", '8', 8, 2),
	ANCIENTMUMMY("customentities.sandstorm_mobs.ancientmummy", "ancientmummy", 'e', 40, 10),
	ANCIENTSKELETON("customentities.sandstorm_mobs.ancientskeleton", "ancientskeleton", 'e', 30, 7),
	LOSTSOUL("customentities.soulstorm_mobs.lostsoul", "lostsoul", '3', 14, 5),
	TAMEDLOSTSOUL("customentities.pets.tamed_lostsoul", "tamedlostsoul", '3', 14, 5),
	SOULREAPER("customentities.soulstorm_mobs.soulreaper", "soulreaper", '3', 40, 25),
	FIREPHANTOM("customentities.solarstorm_mobs.firephantom", "firephantom", 'c', 20.0, 8.0),
	CHRISTMASELF("customentities.christmas_mobs.elf", "elf", 'c', 12, 4),
	PETCHRISTMASELF("customentities.pets.pet_elf", "petelf", 'c', 12, 4),
	FROSTY("customentities.christmas_mobs.frosty", "frosty", 'c', 30, 8),
	GRINCH("customentities.christmas_mobs.grinch", "grinch", 'c', 40, 12),
	SANTA("customentities.christmas_mobs.santa", "santa", 'c', 400, 20),
	RAMPAGINGGOAT("customentities.easter_mobs.rampaging_goat", "rampaginggoat", 'a', 60, 20),
	EASTERBUNNY("customentities.easter_mobs.easter_bunny", "easterbunny", 'a', 200, 20),
	KILLERCHICKEN("customentities.easter_mobs.killer_chicken", "killerchicken", 'a', 20, 8),
	SCARECROW("customentities.halloween_mobs.scarecrow", "scarecrow", '6', 30, 8),
	GHOUL("customentities.halloween_mobs.ghoul", "ghoul", '6', 30, 8),
	VAMPIRE("customentities.halloween_mobs.vampire", "vampire", '6', 40, 12),
	PSYCO("customentities.halloween_mobs.psyco", "psyco", '6', 40, 12),
	PUMPKINKING("customentities.halloween_mobs.pumpkin_king", "pumpkinking", '6', 500, 20);
	
	private double health, damage;
	public String species;
	public NamespacedKey nameKey;
	private char colChar;
	
	public static Set<String> speciesList = new HashSet<>();
	public static boolean mobsEnabled,dropsEnabled;
	public static Set<CustomEntityType> bossTypes = new HashSet<>(Arrays.asList(SANTA, EASTERBUNNY, PUMPKINKING));
	
	private CustomEntityType(String configPath, String species, char colChar, double health, double damage) {
		this.species = species;
		this.colChar = colChar;
		this.health = health;
		this.damage = damage;
	}
	
	public static void reload(Main plugin) {
		speciesList.clear();
		for (CustomEntityType temp : values()) {
			speciesList.add(temp.species);
			temp.nameKey = new NamespacedKey(plugin, temp.species);
		}
		mobsEnabled = plugin.getConfig().getBoolean("customentities.allow_custom_mobs");
		dropsEnabled = plugin.getConfig().getBoolean("customentities.allow_custom_drops");
	}
	public static CustomEntityType getCustomEntityType(String species) {
		for (CustomEntityType type : values())
			if (type.species.equals(species))
				return type;
		return null;
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
	public char getColChar() {
		return colChar;
	}
	public void setColChar(char colChar) {
		this.colChar = colChar;
	}
}
