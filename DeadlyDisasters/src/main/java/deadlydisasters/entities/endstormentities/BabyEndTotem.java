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
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.EulerAngle;

import deadlydisasters.disasters.events.DestructionDisaster;
import deadlydisasters.disasters.events.WeatherDisaster;
import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.EntityHandler;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;

public class BabyEndTotem extends CustomEntity {
	
	private ArmorStand[] stands = new ArmorStand[4];
	private int locked=1,timer=3,animTicks;
	private Random rand;
	private boolean animation;
	private String name;

	public BabyEndTotem(Mob entity, FileConfiguration file, Main plugin, Random rand) {
		super(entity, plugin);
		this.entityType = CustomEntityType.BABYENDTOTEM;
		this.rand = rand;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		spawnStands();
		if (plugin.mcVersion >= 1.16)
			entity.getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK).setBaseValue(2);
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(4);
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
		entity.setHealth(20);
		entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true, false));
		entity.setSilent(true);
		entity.setMetadata("dd-endtotem", new FixedMetadataValue(plugin, "protected"));
		if (file.contains("customentities."+entity.getUniqueId()+".name")) {
			entity.setCustomName(file.getString("customentities."+entity.getUniqueId()+".name"));
			stands[3].setCustomName(file.getString("customentities."+entity.getUniqueId()+".name"));
		} else {
			entity.setCustomName(Languages.langFile.getString("entities.endTotem"));
			stands[3].setCustomName(Languages.langFile.getString("entities.endTotem"));
		}
		entity.setCustomNameVisible(false);
		stands[3].setCustomNameVisible(true);
	}
	@Override
	public void tick() {
		if (entity == null)
			return;
		if (entity.getNoDamageTicks() == 20)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_HURT, SoundCategory.HOSTILE, .3f, .5f);
		if (!animation && entity.hasMetadata("dd-animation")) {
			entity.removeMetadata("dd-animation", plugin);
			animation = true;
			animTicks = 15;
		}
		Location loc = entity.getLocation();
		stands[0].setHeadPose(new EulerAngle(Math.toRadians(loc.getPitch()),0,0));
		if (plugin.mcVersion >= 1.16)
			entity.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0,0.5,0), 3, .2, .3, .2, 0.01);
		if (animation) {
			float yaw = stands[0].getLocation().getYaw();
			stands[0].teleport(loc.clone().add(0,0.3,0));
			stands[0].setRotation(yaw+30, 0);
			yaw = stands[1].getLocation().getYaw();
			stands[1].teleport(loc.clone().subtract(0,0.19,0));
			stands[1].setRotation(yaw-30, 0);
			yaw = stands[2].getLocation().getYaw();
			stands[2].teleport(loc.clone().subtract(0,0.68,0));
			stands[2].setRotation(yaw+30, 0);
			stands[3].teleport(loc.clone().add(0,1.15,0));
			entity.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(0,.3,0), 2, .15, .15, .15, 0.1);
			animTicks--;
			if (animTicks <= 0)
				animation = false;
			return;
		}
		Location temp = null;
		if (locked >= 0) temp = stands[locked].getLocation().clone();
		if (((Wolf) entity).isSitting()) {
			stands[0].teleport(loc.clone().add(0,.13,0));
			loc.setYaw(loc.getYaw()+180);
			stands[1].teleport(loc.clone().subtract(0,0.31,0));
			stands[1].setRotation(loc.getYaw()+90, 0);
			stands[2].teleport(loc.clone().subtract(0,0.75,0));
			stands[3].teleport(loc.clone().add(0,.95,0));
		} else {
			stands[0].teleport(loc.clone().add(0,.3,0));
			stands[1].teleport(loc.clone().subtract(0,0.19,0));
			stands[1].setRotation(loc.getYaw()+90, 0);
			stands[2].teleport(loc.clone().subtract(0,0.68,0));
			stands[3].teleport(loc.clone().add(0,1.15,0));
		}
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
		if (stands[0].isDead())
			spawnStands();
		if (entity.isDead()) {
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.HOSTILE, .3f, .7f);
			if (entity.getKiller() != null && plugin.getConfig().getBoolean("customentities.allow_custom_drops")) {
				entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.CHORUS_FRUIT));
				if (rand.nextInt(3) == 0) entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.OBSIDIAN));
			}
			entity.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, entity.getLocation().add(0,1.5,0), 200, .25, .25, .25, 20);
			clean();
			it.remove();
			return;
		}
		entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMITE_STEP, SoundCategory.HOSTILE, .01f, .5f);
		if (timer <= 0) {
			timer = 5;
			locked = -1;
		} else if (timer == 3) {
			locked = rand.nextInt(3);
			if (rand.nextInt(3) == 0) {
				int num = rand.nextInt(4);
				if (num == 0)
					entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT_SHORT, SoundCategory.HOSTILE, .5f, .5f);
				else if (num == 1)
					entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT_SHORT, SoundCategory.HOSTILE, .5f, 2f);
				else if (num == 2)
					entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_CONDUIT_ATTACK_TARGET, SoundCategory.HOSTILE, .15f, .5f);
				else
					entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_CONDUIT_ATTACK_TARGET, SoundCategory.HOSTILE, .15f, 2f);
			}
		}
		timer--;
		if (WeatherDisaster.currentWorlds.contains(entity.getWorld())) {
			animation = true;
			animTicks = 100;
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMITE_STEP, SoundCategory.HOSTILE, .4f, .5f);
		} else if (DestructionDisaster.currentLocations.containsKey(entity.getWorld())) {
			for (Player player : DestructionDisaster.currentLocations.get(entity.getWorld()))
				if (entity.getLocation().distanceSquared(player.getLocation()) < 10000) {
					animation = true;
					animTicks = 100;
					entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMITE_STEP, SoundCategory.HOSTILE, .4f, .5f);
					break;
				}
		} else if (animTicks <= 0)
			animation = false;
	}
	@Override
	public void clean() {
		for (ArmorStand e : stands)
			if (e != null) e.remove();
		update(plugin.dataFile);
	}
	@Override
	public void update(FileConfiguration file) {
		if (name != null)
			file.set("customentities."+entity.getUniqueId()+".name", name);
	}
	private void spawnStands() {
		World world = entity.getWorld();
		Location loc = entity.getLocation().clone().add(150,100,0);
		stands[0] = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
		equipHands(stands[0], Material.END_PORTAL_FRAME, Material.CHORUS_PLANT, Material.CHORUS_FLOWER);
		stands[1] = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
		equipHands(stands[1], Material.PURPUR_PILLAR, Material.CHORUS_PLANT, Material.CHORUS_FLOWER);
		stands[1].setLeftArmPose(new EulerAngle(0.5,0.5,-1.8));
		stands[1].setRotation(90f, 0);
		stands[2] = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
		lockStand(stands[2]);
		stands[2].getEquipment().setHelmet(new ItemStack(Material.OBSIDIAN));
		stands[3] = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
		lockStand(stands[3]);
		stands[0].getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		stands[1].getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		stands[2].getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		stands[3].getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
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
		e.setSmall(true);
		if (plugin.mcVersion >= 1.16) {
			e.addEquipmentLock(EquipmentSlot.CHEST, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			e.addEquipmentLock(EquipmentSlot.FEET, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			e.addEquipmentLock(EquipmentSlot.HAND, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			e.addEquipmentLock(EquipmentSlot.HEAD, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			e.addEquipmentLock(EquipmentSlot.LEGS, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			e.addEquipmentLock(EquipmentSlot.OFF_HAND, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
		}
	}
	public void changeName(String name) {
		this.name = name;
		stands[3].setCustomName(name);
	}
}
