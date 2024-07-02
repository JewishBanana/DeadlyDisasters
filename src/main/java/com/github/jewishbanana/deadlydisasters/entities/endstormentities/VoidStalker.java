package com.github.jewishbanana.deadlydisasters.entities.endstormentities;

import java.util.Iterator;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World.Environment;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomDropsFactory;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;

public class VoidStalker extends CustomEntity {
	
	public VoidStalker() {
	}
	public VoidStalker(Mob entity, Main plugin, Random rand) {
		super(entity, plugin);
		this.entityType = CustomEntityType.VOIDSTALKER;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
		entity.setHealth(entityType.getHealth());
		entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true));
		entity.setMetadata("dd-voidstalker", new FixedMetadataValue(plugin, "protected"));
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(entityType.getDamage());
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.getString("entities.voidStalker"));
		entity.setMetadata("dd-unburnable", new FixedMetadataValue(plugin, "protected"));
	}
	@Override
	public void tick() {
		if (entity == null)
			return;
		entity.getWorld().spawnParticle(Particle.SQUID_INK, entity.getLocation(), 5, .4, .4, .4, 0.01);
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (entity == null) {
			it.remove();
			return;
		}
		if (entity.isDead()) {
			if (plugin.mcVersion >= 1.16)
				entity.getWorld().spawnParticle(Particle.SOUL, entity.getLocation(), 10, .3, .3, .3, .03);
			if (entity.getKiller() != null)
				CustomDropsFactory.generateDrops(entity.getLocation(), entityType);
			it.remove();
			return;
		}
	}
	@Override
	public void clean() {
	}
	@Override
	public void update(FileConfiguration file) {
	}
	@Override
	public boolean spawnCondition(Location loc) {
		if (loc.getWorld().getEnvironment() == Environment.THE_END)
			return true;
		return false;
	}
	public Entity spawnEntity(Location loc) {
		Mob entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.PHANTOM);
		handler.addEntity(new VoidStalker(entity, Main.getInstance(), Main.getInstance().random));
		return entity;
	}
}
