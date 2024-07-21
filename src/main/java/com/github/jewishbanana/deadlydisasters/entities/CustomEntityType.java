package com.github.jewishbanana.deadlydisasters.entities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.christmasentities.Elf;
import com.github.jewishbanana.deadlydisasters.entities.christmasentities.ElfPet;
import com.github.jewishbanana.deadlydisasters.entities.christmasentities.Frosty;
import com.github.jewishbanana.deadlydisasters.entities.christmasentities.Grinch;
import com.github.jewishbanana.deadlydisasters.entities.christmasentities.Santa;
import com.github.jewishbanana.deadlydisasters.entities.easterentities.EasterBunny;
import com.github.jewishbanana.deadlydisasters.entities.easterentities.KillerChicken;
import com.github.jewishbanana.deadlydisasters.entities.easterentities.RampagingGoat;
import com.github.jewishbanana.deadlydisasters.entities.endstormentities.BabyEndTotem;
import com.github.jewishbanana.deadlydisasters.entities.endstormentities.EndTotem;
import com.github.jewishbanana.deadlydisasters.entities.endstormentities.EndWorm;
import com.github.jewishbanana.deadlydisasters.entities.endstormentities.VoidArcher;
import com.github.jewishbanana.deadlydisasters.entities.endstormentities.VoidGuardian;
import com.github.jewishbanana.deadlydisasters.entities.endstormentities.VoidStalker;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.Ghoul;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.Psyco;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.PumpkinKing;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.Scarecrow;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.Vampire;
import com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities.InfestedCreeper;
import com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities.InfestedDevourer;
import com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities.InfestedEnderman;
import com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities.InfestedHowler;
import com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities.InfestedSkeleton;
import com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities.InfestedSpirit;
import com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities.InfestedTribesman;
import com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities.InfestedWorm;
import com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities.InfestedZombie;
import com.github.jewishbanana.deadlydisasters.entities.monsoonentities.CursedDiver;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.DarkMage;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.PrimedCreeper;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.ShadowLeech;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.SkeletonKnight;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.SwampBeast;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.TunnellerZombie;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.ZombieKnight;
import com.github.jewishbanana.deadlydisasters.entities.sandstormentities.AncientMummy;
import com.github.jewishbanana.deadlydisasters.entities.sandstormentities.AncientSkeleton;
import com.github.jewishbanana.deadlydisasters.entities.snowstormentities.Yeti;
import com.github.jewishbanana.deadlydisasters.entities.solarstormentities.FirePhantom;
import com.github.jewishbanana.deadlydisasters.entities.soulstormentities.LostSoul;
import com.github.jewishbanana.deadlydisasters.entities.soulstormentities.SoulReaper;
import com.github.jewishbanana.deadlydisasters.entities.soulstormentities.TamedLostSoul;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public enum CustomEntityType {
	
	ENDTOTEM("customentities.endstorm_mobs.endtotem", "endtotem", 'd', EndTotem.class),
	BABYENDTOTEM("customentities.pets.baby_endtotem", "babyendtotem", 'd', BabyEndTotem.class),
	ENDWORM("customentities.endstorm_mobs.endworm", "endworm", 'd', EndWorm.class),
	VOIDARCHER("customentities.endstorm_mobs.voidarcher", "voidarcher", 'd', VoidArcher.class),
	VOIDGUARDIAN("customentities.endstorm_mobs.voidguardian", "voidguardian", 'd', VoidGuardian.class),
	VOIDSTALKER("customentities.endstorm_mobs.voidstalker", "voidstalker", 'd', VoidStalker.class),
	DARKMAGE("customentities.purge_mobs.darkmage", "darkmage", '8', DarkMage.class),
	PRIMEDCREEPER("customentities.purge_mobs.primedcreeper", "primedcreeper", '8', PrimedCreeper.class),
	SKELETONKNIGHT("customentities.purge_mobs.skeletonknight", "skeletonknight", '8', SkeletonKnight.class),
	TUNNELLER("customentities.purge_mobs.tunneller", "tunnellerzombie", '8', TunnellerZombie.class),
	SWAMPBEAST("customentities.purge_mobs.swampbeast", "swampbeast", '8', SwampBeast.class),
	ZOMBIEKNIGHT("customentities.purge_mobs.zombieknight", "zombieknight", '8', ZombieKnight.class),
	SHADOWLEECH("customentities.purge_mobs.shadowleech", "shadowleech", '8', ShadowLeech.class),
	ANCIENTMUMMY("customentities.sandstorm_mobs.ancientmummy", "ancientmummy", 'e', AncientMummy.class),
	ANCIENTSKELETON("customentities.sandstorm_mobs.ancientskeleton", "ancientskeleton", 'e', AncientSkeleton.class),
	LOSTSOUL("customentities.soulstorm_mobs.lostsoul", "lostsoul", '3', LostSoul.class),
	TAMEDLOSTSOUL("customentities.pets.tamed_lostsoul", "tamedlostsoul", '3', TamedLostSoul.class),
	SOULREAPER("customentities.soulstorm_mobs.soulreaper", "soulreaper", '3', SoulReaper.class),
	YETI("customentities.snowstorm_mobs.yeti", "yeti", '9', Yeti.class),
	FIREPHANTOM("customentities.solarstorm_mobs.firephantom", "firephantom", 'c', FirePhantom.class),
	CURSEDDIVER("customentities.monsoon_mobs.cursed_diver", "curseddiver", '1', CursedDiver.class),
	INFESTEDSKELETON("customentities.infestedcaves_mobs.infested_skeleton", "infestedskeleton", '3', InfestedSkeleton.class),
	INFESTEDZOMBIE("customentities.infestedcaves_mobs.infested_zombie", "infestedzombie", '3', InfestedZombie.class),
	INFESTEDCREEPER("customentities.infestedcaves_mobs.infested_creeper", "infestedcreeper", '3', InfestedCreeper.class),
	INFESTEDENDERMAN("customentities.infestedcaves_mobs.infested_enderman", "infestedenderman", '3', InfestedEnderman.class),
	INFESTEDSPIRIT("customentities.infestedcaves_mobs.infested_spirit", "infestedspirit", '3', InfestedSpirit.class),
	INFESTEDTRIBESMAN("customentities.infestedcaves_mobs.infested_tribesman", "infestedtribesman", '3', InfestedTribesman.class),
	INFESTEDDEVOURER("customentities.infestedcaves_mobs.infested_devourer", "infesteddevourer", '3', InfestedDevourer.class),
	INFESTEDHOWLER("customentities.infestedcaves_mobs.infested_howler", "infestedhowler", '3', InfestedHowler.class),
	INFESTEDWORM("customentities.infestedcaves_mobs.infested_worm", "infestedworm", '3', InfestedWorm.class),
	CHRISTMASELF("customentities.christmas_mobs.elf", "elf", 'c', Elf.class),
	PETCHRISTMASELF("customentities.pets.pet_elf", "petelf", 'c', ElfPet.class),
	FROSTY("customentities.christmas_mobs.frosty", "frosty", 'c', Frosty.class),
	GRINCH("customentities.christmas_mobs.grinch", "grinch", 'c', Grinch.class),
	SANTA("customentities.christmas_mobs.santa", "santa", 'c', Santa.class),
	RAMPAGINGGOAT("customentities.easter_mobs.rampaging_goat", "rampaginggoat", 'a', RampagingGoat.class),
	EASTERBUNNY("customentities.easter_mobs.easter_bunny", "easterbunny", 'a', EasterBunny.class),
	KILLERCHICKEN("customentities.easter_mobs.killer_chicken", "killerchicken", 'a', KillerChicken.class),
	SCARECROW("customentities.halloween_mobs.scarecrow", "scarecrow", '6', Scarecrow.class),
	GHOUL("customentities.halloween_mobs.ghoul", "ghoul", '6', Ghoul.class),
	VAMPIRE("customentities.halloween_mobs.vampire", "vampire", '6', Vampire.class),
	PSYCO("customentities.halloween_mobs.psyco", "psyco", '6', Psyco.class),
	PUMPKINKING("customentities.halloween_mobs.pumpkin_king", "pumpkinking", '6', PumpkinKing.class);
	
	private double health, damage, spawnRate;
	private boolean spawning;
	public String configPath, species;
	public NamespacedKey nameKey;
	private List<String> dropsList;
	private char colChar;
	private CustomEntity entityClass;
	
	public static Set<String> speciesList = new HashSet<>();
	public static YamlConfiguration yaml;
	public static Set<CustomEntityType> bossTypes = new HashSet<>(Arrays.asList(SANTA, EASTERBUNNY, PUMPKINKING));
	
	private CustomEntityType(String configPath, String species, char colChar, Class<? extends CustomEntity> clazz) {
		this.configPath = configPath;
		this.species = species;
		this.colChar = colChar;
		try {
			this.entityClass = clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void reload(Main plugin) {
		speciesList.clear();
		for (CustomEntityType temp : values()) {
			speciesList.add(temp.species);
			temp.nameKey = new NamespacedKey(plugin, temp.species);
			temp.resetValues();
			if (yaml.contains(temp.configPath+".health"))
				temp.setHealth(yaml.getDouble(temp.configPath+".health"));
			if (yaml.contains(temp.configPath+".damage"))
				temp.setDamage(yaml.getDouble(temp.configPath+".damage"));
			if (yaml.getBoolean("settings.allow_custom_mobs") && yaml.contains(temp.configPath+".spawning"))
				temp.setSpawning(yaml.getBoolean(temp.configPath+".spawning"));
			else
				temp.setSpawning(false);
			if (yaml.contains(temp.configPath+".spawnrate") && temp.spawning)
				temp.setSpawnRate(yaml.getDouble(temp.configPath+".spawnrate") / 100.0);
			else
				temp.setSpawnRate(-1.0);
			if (yaml.getBoolean("settings.allow_custom_drops") && yaml.contains(temp.configPath+".drops"))
				temp.setDropsList(yaml.getStringList(temp.configPath+".drops"));
			else
				temp.setDropsList(new ArrayList<String>());
		}
	}
	public Object grabCustomSetting(String field) {
		return yaml.get(configPath+'.'+field);
	}
	public void resetValues() {
		spawning = false;
		spawnRate = 0;
	}
	public boolean spawnCondition(Location loc) {
		return entityClass.spawnCondition(loc);
	}
	public Entity spawnEntity(Location loc) {
		return entityClass.spawnEntity(loc);
	}
	public static CustomEntityType getCustomEntityType(String species) {
		for (CustomEntityType type : values())
			if (type.species.equals(species))
				return type;
		return null;
	}
	public static void saveDataFile(Main plugin) {
		try {
			yaml.save(new File(plugin.getDataFolder().getAbsolutePath(), "entities.yml"));
		} catch (IOException e) {
			Main.consoleSender.sendMessage(Utils.convertString(Languages.prefix+"&cError #00 Unable to save data file!"));
		}
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
