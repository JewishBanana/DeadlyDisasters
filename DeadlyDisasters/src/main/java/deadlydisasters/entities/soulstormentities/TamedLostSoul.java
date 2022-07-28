package deadlydisasters.entities.soulstormentities;

import java.util.Iterator;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.EulerAngle;

import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.CustomHead;
import deadlydisasters.entities.EntityHandler;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;

public class TamedLostSoul extends CustomEntity {
	
	private Random rand;
	private ArmorStand stand;
	private LivingEntity target;

	public TamedLostSoul(Mob entity, Main plugin, Random rand, LivingEntity target) {
		super(entity, plugin);
		this.entityType = CustomEntityType.TAMEDLOSTSOUL;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		this.target = target;
		this.rand = rand;
		entity.setMetadata("dd-tamedlostsoul", new FixedMetadataValue(plugin, "protected"));
		stand = (ArmorStand) entity.getWorld().spawnEntity(entity.getLocation().clone().add(100,100,0), EntityType.ARMOR_STAND);
		if (plugin.mcVersion >= 1.16)
			stand.setInvisible(true);
		else
			stand.setVisible(false);
		stand.setMarker(true);
		stand.setGravity(false);
		stand.setSmall(true);
		if (plugin.mcVersion >= 1.16) {
			stand.addEquipmentLock(EquipmentSlot.CHEST, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.FEET, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.HAND, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.HEAD, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.LEGS, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.OFF_HAND, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
		}
		stand.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(14);
		entity.setHealth(14);
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(5);
		entity.setSilent(true);
		int head = rand.nextInt(4);
		if (head == 0)
			stand.getEquipment().setHelmet(CustomHead.SOUL1.getHead());
		else if (head == 1)
			stand.getEquipment().setHelmet(CustomHead.SOUL2.getHead());
		else if (head == 2)
			stand.getEquipment().setHelmet(CustomHead.SOUL3.getHead());
		else
			stand.getEquipment().setHelmet(CustomHead.SOUL4.getHead());
		entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true, false));
		entity.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
		
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.langFile.getString("entities.lostSoul"));
	}
	@Override
	public void tick() {
		if (entity == null || stand == null)
			return;
		stand.teleport(entity.getLocation().subtract(0,.3,0));
		stand.setHeadPose(new EulerAngle(Math.toRadians(entity.getLocation().getPitch()),0,0));
		if (plugin.mcVersion >= 1.16)
			entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, entity.getLocation(), 1, .2, .2, .2, 0.05);
		entity.getWorld().spawnParticle(Particle.SMOKE_NORMAL, entity.getLocation().clone().add(0,0.4,0), 2, .025, .1, .025, 0.015);
		if (target == null || target.isDead())
			entity.remove();
		else
			entity.setTarget(target);
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (entity == null) {
			if (stand != null)
				stand.remove();
			it.remove();
			return;
		}
		stand = (ArmorStand) plugin.getServer().getEntity(stand.getUniqueId());
		if (entity.isDead()) {
			if (plugin.mcVersion >= 1.16)
				entity.getWorld().spawnParticle(Particle.SOUL, entity.getLocation().add(0,.5,0), 15, .3, .5, .3, .03);
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_VEX_DEATH, SoundCategory.HOSTILE, 1f, .7f);
			clean();
			it.remove();
			return;
		}
		if (rand.nextInt(8) == 0)
			if (rand.nextInt(2) == 0)
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_VEX_AMBIENT, SoundCategory.HOSTILE, 1f, .5f);
			else
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_VEX_CHARGE, SoundCategory.HOSTILE, 1f, .5f);
	}
	@Override
	public void clean() {
		if (stand != null)
			stand.remove();
		if (entity != null)
			entity.remove();
	}
	@Override
	public void update(FileConfiguration file) {
	}
}
