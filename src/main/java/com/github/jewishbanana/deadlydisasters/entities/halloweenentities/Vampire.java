package com.github.jewishbanana.deadlydisasters.entities.halloweenentities;

import java.util.Iterator;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Zombie;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.handlers.ItemsHandler;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class Vampire extends CustomEntity {
	
	private boolean batForm;
	private Zombie zombie;
	public UUID zombieUUID;
	private double healthBarrier;
	
	private Random rand;
	
	public Vampire(Mob entity, Main plugin) {
		super(entity, plugin);
		this.rand = plugin.random;
		this.entityType = CustomEntityType.VAMPIRE;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		this.healthBarrier = entityType.getHealth() / 3;
		
		entity.setCanPickupItems(false);
		entity.setSilent(true);
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
		entity.setHealth(entityType.getHealth());
		if (entity.getType() != EntityType.BAT) {
			entity.setAI(false);
			entity.setMetadata("dd-vampire", new FixedMetadataValue(plugin, "protected"));
			
			zombie = entity.getWorld().spawn(entity.getLocation(), Zombie.class, false, consumer -> {
				consumer.setCanPickupItems(false);
				consumer.setSilent(true);
				consumer.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
				consumer.setHealth(entityType.getHealth());
				consumer.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(entityType.getDamage());
				consumer.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.3);
				consumer.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40);
				consumer.setMetadata("dd-vampire", new FixedMetadataValue(plugin, "protected"));
				consumer.setMetadata("dd-halloweenmobs", plugin.fixedData);
				consumer.setMetadata("dd-customentity", plugin.fixedData);
				consumer.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 0, true, false));
				if (consumer.getCustomName() == null)
					consumer.setCustomName(Languages.getString("halloween.vampire"));
			});
			this.zombieUUID = zombie.getUniqueId();
		} else
			entity.setMetadata("dd-vampirebat", new FixedMetadataValue(plugin, "protected"));
		entity.setMetadata("dd-halloweenmobs", plugin.fixedData);
		
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.getString("halloween.vampire"));
	}
	@Override
	public void tick() {
		if (entity == null || entity.isDead())
			return;
		if (!batForm) {
			if (zombie == null || zombie.isDead())
				return;
			if (entity.getNoDamageTicks() == 20)
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_EVOKER_HURT, SoundCategory.HOSTILE, 2f, .5f);
			entity.teleport(zombie);
			if (zombie.getHealth() < entity.getHealth()) {
				entity.damage(0.00001);
				entity.setHealth(zombie.getHealth());
			} else if (entity.getHealth() < zombie.getHealth())
				zombie.setHealth(entity.getHealth());
			if (entity.getHealth() <= healthBarrier) {
				batForm = true;
				Bat bat = entity.getWorld().spawn(entity.getLocation(), Bat.class, consumer -> {
					consumer.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
					consumer.setHealth(entity.getHealth());
					consumer.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
					consumer.setMetadata("dd-vampirebat", new FixedMetadataValue(plugin, "protected"));
					if (consumer.getCustomName() == null)
						consumer.setCustomName(Languages.getString("halloween.vampire"));
				});
				entity.getWorld().spawnParticle(VersionUtils.getNormalSmoke(), entity.getLocation(), 15, .7, .7, .7, 0.001);
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 2f, .5f);
				entity.remove();
				zombie.remove();
				this.entity = bat;
				this.entityUUID = bat.getUniqueId();
			}
			return;
		} else if (entity.getHealth() > healthBarrier) {
			batForm = false;
			Mob vampire = entity.getWorld().spawn(entity.getLocation(), Evoker.class, false, consumer -> {
				consumer.setCanPickupItems(false);
				consumer.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
				consumer.setHealth(entity.getHealth());
				consumer.setAI(false);
				consumer.setSilent(true);
				consumer.setMetadata("dd-vampire", new FixedMetadataValue(plugin, "protected"));
				consumer.setMetadata("dd-halloweenmobs", plugin.fixedData);
				consumer.setMetadata("dd-customentity", plugin.fixedData);
				consumer.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
				if (consumer.getCustomName() == null)
					consumer.setCustomName(Languages.getString("halloween.vampire"));
			});
			zombie = entity.getWorld().spawn(entity.getLocation(), Zombie.class, false, consumer -> {
				consumer.setCanPickupItems(false);
				consumer.setSilent(true);
				consumer.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
				consumer.setHealth(entity.getHealth());
				consumer.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(entityType.getDamage());
				consumer.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.3);
				consumer.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40);
				consumer.setMetadata("dd-vampire", new FixedMetadataValue(plugin, "protected"));
				consumer.setMetadata("dd-halloweenmobs", plugin.fixedData);
				consumer.setMetadata("dd-customentity", plugin.fixedData);
				consumer.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 0, true, false));
				if (consumer.getCustomName() == null)
					consumer.setCustomName(Languages.getString("halloween.vampire"));
			});
			this.zombieUUID = zombie.getUniqueId();
			entity.getWorld().spawnParticle(VersionUtils.getNormalSmoke(), entity.getLocation().add(0,1,0), 15, .7, 1, .7, 0.001);
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 2f, .5f);
			entity.remove();
			this.entity = vampire;
			this.entityUUID = vampire.getUniqueId();
		}
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (zombieUUID != null)
			zombie = (Zombie) plugin.getServer().getEntity(zombieUUID);
		if (entity == null) {
			it.remove();
			clean();
			return;
		}
		if (entity.isDead()) {
			if (entity.getKiller() != null && CustomEntityType.dropsEnabled && plugin.random.nextDouble() * 100 < 30.0)
				entity.getWorld().dropItemNaturally(entity.getLocation(), ItemsHandler.vampireFang);
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_EVOKER_DEATH, SoundCategory.HOSTILE, 2f, .5f);
			it.remove();
			clean();
			return;
		}
		if (batForm) {
			entity.setHealth(Math.min(entity.getHealth()+1.0, entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
			entity.getWorld().spawnParticle(Particle.COMPOSTER, entity.getLocation().add(0,0.2,0), 3, .3, .3, .3, 0.001);
		} else if (rand.nextInt(6) == 0)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_EVOKER_AMBIENT, SoundCategory.HOSTILE, 2f, .5f);
	}
	@Override
	public void clean() {
		if (zombie != null)
			zombie.remove();
	}
	@Override
	public void update(FileConfiguration file) {
	}
	public void bite(Entity target) {
		entity.getWorld().spawnParticle(VersionUtils.getBlockDust(), entity.getLocation().clone().add(0,1.1,0).add(Utils.getVectorTowards(entity.getLocation(), target.getLocation()).multiply(.8)), 20, .2, .2, .2, 1, Material.REDSTONE_BLOCK.createBlockData());
		entity.setHealth(Math.min(entity.getHealth()+(entityType.getDamage()/2), entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
		if (zombie != null)
			zombie.setHealth(entity.getHealth());
	}
}
