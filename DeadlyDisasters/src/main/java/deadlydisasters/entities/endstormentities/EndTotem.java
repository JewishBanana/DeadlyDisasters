package deadlydisasters.entities.endstormentities;

import java.util.Iterator;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
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
import deadlydisasters.entities.EntityHandler;
import deadlydisasters.general.ItemsHandler;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;

public class EndTotem extends CustomEntity {
	
	private ArmorStand[] stands = new ArmorStand[4];
	private int locked=1,timer=3,animTicks;
	private Random rand;
	private boolean animation;

	public EndTotem(Mob entity, Main plugin, Random rand) {
		super(entity, plugin);
		this.entityType = CustomEntityType.ENDTOTEM;
		this.rand = rand;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		World world = entity.getWorld();
		Location loc = entity.getLocation().clone().add(150,100,0);
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.langFile.getString("entities.endTotem"));
		stands[0] = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
		equipHands(stands[0], Material.END_PORTAL_FRAME, Material.CHORUS_PLANT, Material.CHORUS_FLOWER);
		stands[1] = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
		equipHands(stands[1], Material.PURPUR_PILLAR, Material.CHORUS_PLANT, Material.CHORUS_FLOWER);
		stands[2] = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
		equipHands(stands[2], Material.OBSIDIAN, Material.CHORUS_FLOWER, Material.CHORUS_PLANT);
		stands[2].setLeftArmPose(new EulerAngle(0.5,0.5,-1.8));
		stands[2].setRotation(90f, 0);
		stands[3] = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
		lockStand(stands[3]);
		stands[3].getEquipment().setHelmet(new ItemStack(Material.OBSIDIAN));
		stands[0].getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		stands[1].getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		stands[2].getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		stands[3].getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		if (plugin.mcVersion >= 1.16)
			entity.getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK).setBaseValue(5);
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(6);
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(30);
		entity.setHealth(30);
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.32);
		entity.setMetadata("dd-endtotem", new FixedMetadataValue(plugin, "protected"));
		entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true, false));
		entity.setSilent(true);
		entity.setCanPickupItems(false);
	}
	@Override
	public void tick() {
		if (entity == null)
			return;
		if (entity.getNoDamageTicks() == 20)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_HURT, SoundCategory.HOSTILE, 1f, .5f);
		if (!animation && entity.hasMetadata("dd-animation")) {
			entity.removeMetadata("dd-animation", plugin);
			animation = true;
			animTicks = 15;
		}
		Location loc = entity.getLocation();
		stands[0].setHeadPose(new EulerAngle(Math.toRadians(loc.getPitch()),0,0));
		if (plugin.mcVersion >= 1.16)
			entity.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0,1,0), 7, .5, .75, .5, 0.01);
		if (animation) {
			float yaw = stands[0].getLocation().getYaw();
			stands[0].teleport(loc.clone().add(0,1,0));
			stands[0].setRotation(yaw+30, 0);
			yaw = stands[1].getLocation().getYaw();
			stands[1].teleport(loc.clone().add(0,0.2,0));
			stands[1].setRotation(yaw-30, 0);
			yaw = stands[2].getLocation().getYaw();
			stands[2].teleport(loc.clone().subtract(0,0.55,0));
			stands[2].setRotation(yaw+30, 0);
			yaw = stands[3].getLocation().getYaw();
			stands[3].teleport(loc.clone().subtract(0,1.3,0));
			stands[3].setRotation(yaw+30, 0);
			entity.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(0,1,0), 7, .2, .2, .2, 0.2);
			animTicks--;
			if (animTicks <= 0)
				animation = false;
			return;
		}
		Location temp = null;
		if (locked >= 0) temp = stands[locked].getLocation().clone();
		stands[0].teleport(loc.clone().add(0,1,0));
		stands[1].teleport(loc.clone().add(0,0.2,0));
		stands[1].setRotation(loc.getYaw()+90, 0);
		stands[2].teleport(loc.clone().subtract(0,0.55,0));
		stands[3].teleport(loc.clone().subtract(0,1.3,0));
		if (temp != null) stands[locked].setRotation(temp.getYaw()+6, 0);
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (entity == null) {
			for (ArmorStand e : stands)
				if (e != null)
					e.remove();
			it.remove();
			return;
		}
		refreshReferences(stands);
		if (entity.isDead()) {
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.HOSTILE, 1f, .5f);
			if (entity.getKiller() != null && plugin.getConfig().getBoolean("customentities.allow_custom_drops")) {
				entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.CHORUS_FRUIT));
				if (rand.nextInt(2) == 0) entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.OBSIDIAN));
				if (rand.nextInt(7) == 0)
					entity.getWorld().dropItemNaturally(entity.getLocation(), ItemsHandler.voidshard);
			}
			entity.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, entity.getLocation().add(0,2,0), 1000, .25, .25, .25, 20);
			clean();
			it.remove();
			return;
		}
		entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMITE_STEP, SoundCategory.HOSTILE, .8f, .5f);
		if (timer <= 0) {
			timer = 5;
			locked = -1;
		} else if (timer == 3)
			locked = rand.nextInt(3);
		timer--;
	}
	@Override
	public void clean() {
		for (ArmorStand e : stands)
			if (e != null) e.remove();
	}
	@Override
	public void update(FileConfiguration file) {
	}
	private void equipHands(ArmorStand e, Material head, Material main, Material off) {
		lockStand(e);
		e.getEquipment().setHelmet(new ItemStack(head));
		e.getEquipment().setItemInMainHand(new ItemStack(main));
		e.getEquipment().setItemInOffHand(new ItemStack(off));
		e.setRightArmPose(new EulerAngle(0,0.3,1.1));
		e.setLeftArmPose(new EulerAngle(0,0.5,-1.8));
	}
	private void lockStand(ArmorStand e) {
		if (plugin.mcVersion >= 1.16)
			e.setInvisible(true);
		else
			e.setVisible(false);
		e.setGravity(false);
		e.setArms(true);
		e.setMarker(true);
		if (plugin.mcVersion >= 1.16) {
			e.addEquipmentLock(EquipmentSlot.CHEST, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			e.addEquipmentLock(EquipmentSlot.FEET, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			e.addEquipmentLock(EquipmentSlot.HAND, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			e.addEquipmentLock(EquipmentSlot.HEAD, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			e.addEquipmentLock(EquipmentSlot.LEGS, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			e.addEquipmentLock(EquipmentSlot.OFF_HAND, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
		}
	}
}
