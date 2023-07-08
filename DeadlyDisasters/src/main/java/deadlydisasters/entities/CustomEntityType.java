package deadlydisasters.entities;

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
	INFESTEDWORM("customentities.infestedcaves_mobs.infested_worm", "infestedworm"),
	CHRISTMASELF("customentities.christmas_mobs.elf", "elf"),
	PETCHRISTMASELF("customentities.pets.pet_elf", "petelf"),
	FROSTY("customentities.christmas_mobs.frosty", "frosty"),
	GRINCH("customentities.christmas_mobs.grinch", "grinch"),
	SANTA("customentities.christmas_mobs.santa", "santa"),
	RAMPAGINGGOAT("customentities.easter_mobs.rampaging_goat", "rampaginggoat"),
	EASTERBUNNY("customentities.easter_mobs.easter_bunny", "easterbunny"),
	KILLERCHICKEN("customentities.easter_mobs.killer_chicken", "killerchicken");
	
	public Main plugin;
	public String species;
	public NamespacedKey nameKey;
	
	private CustomEntityType(String configPath, String species) {
		this.species = species;
	}
	
	public static void reload(Main plugin) {
		for (CustomEntityType temp : values()) {
			temp.plugin = plugin;
			temp.nameKey = new NamespacedKey(plugin, temp.species);
		}
	}
}
