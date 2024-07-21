package com.github.jewishbanana.deadlydisasters.entities;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Goat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Zombie;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.christmasentities.Elf;
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
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.DarkMage;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.PrimedCreeper;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.ShadowLeech;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.SkeletonKnight;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.SwampBeast;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.TunnellerZombie;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.ZombieKnight;
import com.github.jewishbanana.deadlydisasters.entities.sandstormentities.AncientMummy;
import com.github.jewishbanana.deadlydisasters.entities.sandstormentities.AncientSkeleton;
import com.github.jewishbanana.deadlydisasters.entities.soulstormentities.LostSoul;
import com.github.jewishbanana.deadlydisasters.entities.soulstormentities.SoulReaper;
import com.github.jewishbanana.deadlydisasters.entities.soulstormentities.TamedLostSoul;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class EntityHandler {
	
	private Main plugin;
	private FileConfiguration file;
	private boolean running;
	private RepeatingTask tick;
	private Random rand;
	public NamespacedKey globalKey;
	
	public static NamespacedKey removalKey;
	
	private Set<CustomEntity> list = ConcurrentHashMap.newKeySet();
	
	public EntityHandler(Main plugin, FileConfiguration file) {
		this.plugin = plugin;
		this.file = file;
		this.rand = plugin.random;
		this.globalKey = new NamespacedKey(plugin, "customentity");
		removalKey = new NamespacedKey(plugin, "removalkey");
		ConfigurationSection section = file.getConfigurationSection("customentities");
		if (section == null)
			section = file.createSection("customentities");
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
			for (World world : Bukkit.getWorlds())
				for (LivingEntity entity : world.getLivingEntities())
					if (entity.getPersistentDataContainer().has(globalKey, PersistentDataType.BYTE))
						addEntityBySpecies(null, entity);
		}, 1);
	}
	public void addEntityBySpecies(String species, Entity e) {
		if (species == null)
			for (CustomEntityType type : CustomEntityType.values())
				if (e.getPersistentDataContainer().has(type.nameKey, PersistentDataType.BYTE)) {
					species = type.species;
					break;
				}
		switch (species) {
		case "babyendtotem":
			addEntity(new BabyEndTotem((Mob) e, file, plugin, rand));
			break;
		case "endtotem":
			addEntity(new EndTotem((Mob) e, plugin, rand));
			break;
		case "endworm":
			addEntity(new EndWorm((Mob) e, plugin, rand));
			break;
		case "voidguardian":
			addEntity(new VoidGuardian((Mob) e, plugin, rand));
			break;
		case "voidarcher":
			addEntity(new VoidArcher((Mob) e, plugin, rand));
			break;
		case "voidstalker":
			addEntity(new VoidStalker((Mob) e, plugin, rand));
			break;
		case "lostsoul":
			addEntity(new LostSoul((Mob) e, plugin, rand));
			break;
		case "ancientskeleton":
			addEntity(new AncientSkeleton((Mob) e, plugin, rand));
			break;
		case "ancientmummy":
			addEntity(new AncientMummy((Mob) e, plugin, rand));
			break;
		case "primedcreeper":
			addEntity(new PrimedCreeper((Mob) e, plugin));
			break;
		case "tunnellerzombie":
			addEntity(new TunnellerZombie((Zombie) e, null, plugin));
			break;
		case "skeletonknight":
			addEntity(new SkeletonKnight((Skeleton) e, plugin));
			break;
		case "darkmage":
			addEntity(new DarkMage((Mob) e, plugin));
			break;
		case "soulreaper":
			addEntity(new SoulReaper((Mob) e, plugin, rand));
			break;
		case "tamedlostsoul":
			addEntity(new TamedLostSoul((Mob) e, plugin, rand, null));
			break;
		case "swampbeast":
			addEntity(new SwampBeast((Mob) e, plugin));
			break;
		case "zombieknight":
			addEntity(new ZombieKnight((Mob) e, plugin));
			break;
		case "shadowleech":
			addEntity(new ShadowLeech((Zombie) e, plugin, rand));
			break;
		case "elf":
			addEntity(new Elf((Zombie) e, plugin, rand));
			break;
		case "frosty":
			addEntity(new Frosty((Snowman) e, plugin, rand));
			break;
		case "grinch":
			addEntity(new Grinch((Mob) e, plugin, rand));
			break;
		case "santa":
			addEntity(new Santa((Zombie) e, plugin, rand));
			break;
		case "rampaginggoat":
			addEntity(new RampagingGoat((Goat) e, plugin));
			break;
		case "easterbunny":
			addEntity(new EasterBunny((Rabbit) e, plugin, rand));
			break;
		case "killerchicken":
			addEntity(new KillerChicken((Zombie) e, plugin));
			break;
		case "scarecrow":
			addEntity(new Scarecrow((Zombie) e, plugin, rand));
			break;
		case "ghoul":
			addEntity(new Ghoul((Zombie) e, null, plugin, false));
			break;
		case "vampire":
			addEntity(new Vampire((Mob) e, plugin));
			break;
		case "psyco":
			addEntity(new Psyco((Mob) e, plugin, rand));
			break;
		case "pumpkinking":
			addEntity(new PumpkinKing((Mob) e, plugin, rand));
			break;
		default:
		}
	}
	public void startTimers() {
		running = true;
		tick = new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				Iterator<CustomEntity> it = list.iterator();
				CustomEntity tempEntity = null;
				while (it.hasNext())
					try {
						tempEntity = it.next();
						tempEntity.tick();
					} catch (Exception e) {
						if (!CustomEntityType.bossTypes.contains(tempEntity.getType())) {
							tempEntity.clean();
							it.remove();
						}
						if (plugin.debug) {
							e.printStackTrace();
							Utils.sendDebugMessage();
						}
					}
			}
		};
		new RepeatingTask(plugin, 0, 20) {
			@Override
			public void run() {
				if (list.isEmpty()) {
					tick.cancel();
					cancel();
					running = false;
					return;
				}
				Iterator<CustomEntity> it = list.iterator();
				CustomEntity tempEntity = null;
				while (it.hasNext())
					try {
						tempEntity = it.next();
						tempEntity.function(it);
					}  catch (Exception e) {
						if (!CustomEntityType.bossTypes.contains(tempEntity.getType())) {
							tempEntity.clean();
							it.remove();
						}
						if (plugin.debug) {
							e.printStackTrace();
							Utils.sendDebugMessage();
						}
					}
			}
		};
	}
	public <T extends CustomEntity> T addEntity(T e) {
		if (findEntity(e.getEntity()) != null) {
			return e;
		}
		e.entity.setMetadata("dd-customentity", new FixedMetadataValue(plugin, "protected"));
		e.entity.getPersistentDataContainer().set(globalKey, PersistentDataType.BYTE, (byte) 0);
		list.add(e);
		if (!running)
			startTimers();
		return e;
	}
	public void addFalseEntity(CustomEntity e) {
		list.add(e);
		if (!running)
			startTimers();
	}
	public void removeEntity(CustomEntity e) {
		list.remove(e);
	}
	public CustomEntity findEntity(LivingEntity e) {
		for (CustomEntity n : list)
			if (n.getUUID() != null && n.getUUID().equals(e.getUniqueId()))
				return n;
		return null;
	}
	public void cleanEntities() {
		list.forEach(e -> e.clean());
	}
	public Set<CustomEntity> getList() {
		return list;
	}
}
