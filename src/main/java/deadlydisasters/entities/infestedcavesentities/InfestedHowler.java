package deadlydisasters.entities.infestedcavesentities;

import java.util.Iterator;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.EulerAngle;

import deadlydisasters.disasters.InfestedCaves;
import deadlydisasters.entities.CustomDropsFactory;
import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.CustomHead;
import deadlydisasters.entities.EntityHandler;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;
import deadlydisasters.listeners.DeathMessages;
import deadlydisasters.utils.Utils;

public class InfestedHowler extends CustomEntity {
	
	private ArmorStand stand,eyes;
	private Random rand;
	private int cooldown = 5, shriek;
	private double animation = -0.6;
	private boolean animActive;
	private BlockData data;

	@SuppressWarnings("deprecation")
	public InfestedHowler(Zombie entity, Main plugin, Random rand) {
		super(entity, plugin);
		this.rand = rand;
		this.entityType = CustomEntityType.INFESTEDHOWLER;
		this.species = entityType.species;
		
		Utils.clearEntityOfItems(entity);
		if (plugin.mcVersion >= 1.16)
			entity.setBaby();
		else
			entity.setBaby(true);
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true, false));
		entity.setCanPickupItems(false);
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
		entity.setHealth(entityType.getHealth());
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(entityType.getDamage());
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.15);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(30);
		entity.setMetadata("dd-infestedhowler", new FixedMetadataValue(plugin, "protected"));
		entity.setCustomName(Languages.langFile.getString("entities.infestedHowler"));
		entity.setSilent(true);
		
		stand = (ArmorStand) entity.getWorld().spawnEntity(entity.getLocation().clone().add(100,100,0), EntityType.ARMOR_STAND);
		if (plugin.mcVersion >= 1.16)
			stand.setInvisible(true);
		else
			stand.setVisible(false);
		stand.setMarker(true);
		stand.setGravity(false);
		if (plugin.mcVersion >= 1.16) {
			stand.addEquipmentLock(EquipmentSlot.CHEST, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.FEET, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.HAND, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.HEAD, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.LEGS, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.OFF_HAND, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
		}
		stand.getEquipment().setHelmet(CustomHead.INFESTEDHOWLER.getHead());
		stand.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		spawnEyes();
		
		if (plugin.mcVersion >= 1.19)
			data = Material.SCULK.createBlockData();
		else
			data = Material.NETHERRACK.createBlockData();
	}
	@Override
	public void tick() {
		if (entity == null || stand == null || eyes == null)
			return;
		if (entity.getNoDamageTicks() == 20 && plugin.mcVersion >= 1.19)
			entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_BREAK, SoundCategory.HOSTILE, 1, .8F);
		entity.getWorld().spawnParticle(Particle.BLOCK_CRACK, entity.getLocation(), 1, .2, .05, .2, 0.1, data);
		if (shriek > 0) {
			if (shriek % 5 == 0 && plugin.mcVersion >= 1.19)
				entity.getWorld().spawnParticle(Particle.SHRIEK, entity.getEyeLocation(), 1, 0, 0, 0, 0.1, 0);
			shriek--;
			if (shriek == 0)
				if (entity.getTarget() != null && !entity.getTarget().isDead()) {
					for (Entity e : entity.getNearbyEntities(25, 25, 25))
						if (e instanceof Mob && ((Mob) e).getTarget() == null)
							((Mob) e).setTarget(entity.getTarget());
					for (InfestedCaves cave : DeathMessages.infestedcaves)
						if (cave.getLocation().getWorld().equals(entity.getWorld()) && cave.getLocation().distanceSquared(entity.getLocation()) <= cave.getSizeSquared()) {
							cave.shriekEvent(entity.getTarget(), (rand.nextDouble()*100 < (double) entityType.grabCustomSetting("warden_notify_chance")) ? true : false);
							cave.closeRoute(entity.getTarget(), 8, Utils.getVectorTowards(entity.getLocation(), entity.getTarget().getLocation().add(0,1,0)));
							return;
						}
					if (plugin.mcVersion >= 1.19 && rand.nextInt(4) == 0)
						spawnWarden(entity.getLocation(), entity.getTarget());
				}
		}
		if (animActive) {
			if (animation < -0.1)
				animation += 0.02;
		} else if (animation > -0.6)
			animation -= 0.02;
		stand.teleport(entity.getLocation().subtract(0,1.4,0));
		eyes.teleport(entity.getLocation().add(0,animation,0));
		if (entity.getTarget() != null) {
			Location newLocation = entity.getLocation().setDirection(entity.getTarget().getEyeLocation().subtract(0,1,0).toVector().subtract(entity.getLocation().toVector()));
			eyes.setRotation(newLocation.getYaw(), 0);
			eyes.setHeadPose(new EulerAngle(Math.toRadians(newLocation.getPitch()),0,0));
		} else
			eyes.setHeadPose(new EulerAngle(Math.toRadians(entity.getLocation().getPitch()),0,0));
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (entity == null || stand == null || eyes == null) {
			clean();
			it.remove();
			return;
		}
		if (entity.isDead()) {
			if (plugin.mcVersion >= 1.19)
				entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.HOSTILE, 1, 2);
			if (entity.getKiller() != null)
				CustomDropsFactory.generateDrops(entity.getLocation(), entityType);
			clean();
			it.remove();
			return;
		}
		stand = (ArmorStand) plugin.getServer().getEntity(stand.getUniqueId());
		eyes = (ArmorStand) plugin.getServer().getEntity(eyes.getUniqueId());
		if (cooldown > 0)
			cooldown--;
		if (entity.getTarget() != null && entity.getLocation().distanceSquared(entity.getTarget().getLocation()) <= 36 && entity.hasLineOfSight(entity.getTarget())) {
			if (!animActive) {
				eyes.getEquipment().setHelmet(CustomHead.INFESTEDHOWLEREYES.getHead());
				animActive = true;
			}
			entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 30, 10, true, false));
			if (cooldown <= 0) {
				cooldown = 10;
				shriek = 40;
				entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 50, 10, true, false));
				if (plugin.mcVersion >= 1.19)
					entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.HOSTILE, 10, .5F);
			}
		} else {
			animActive = false;
			if (animation <= -0.6)
				eyes.getEquipment().setHelmet(new ItemStack(Material.AIR));
		}
	}
	@Override
	public void clean() {
		if (stand != null)
			stand.remove();
		if (eyes != null)
			eyes.remove();
	}
	@Override
	public void update(FileConfiguration file) {
	}
	private void spawnEyes() {
		eyes = (ArmorStand) entity.getWorld().spawnEntity(entity.getLocation().clone().add(100,100,0), EntityType.ARMOR_STAND);
		if (plugin.mcVersion >= 1.16)
			eyes.setInvisible(true);
		else
			eyes.setVisible(false);
		eyes.setMarker(true);
		eyes.setGravity(false);
		eyes.setSmall(true);
		if (plugin.mcVersion >= 1.16) {
			eyes.addEquipmentLock(EquipmentSlot.CHEST, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			eyes.addEquipmentLock(EquipmentSlot.FEET, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			eyes.addEquipmentLock(EquipmentSlot.HAND, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			eyes.addEquipmentLock(EquipmentSlot.HEAD, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			eyes.addEquipmentLock(EquipmentSlot.LEGS, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			eyes.addEquipmentLock(EquipmentSlot.OFF_HAND, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
		}
		eyes.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
	}
	private void spawnWarden(Location location, LivingEntity target) {
		for (int i=1; i < 30; i++) {
			Location spawn = Utils.findSmartYSpawn(location, location.clone().add((rand.nextInt(i)-(i/2))+1, 0, (rand.nextInt(i)-(i/2))+1), 3, 10);
			if (spawn == null || spawn.getBlock().getRelative(BlockFace.DOWN).isPassable() || spawn.getBlock().getRelative(BlockFace.DOWN, 2).isPassable() || spawn.getBlock().getRelative(BlockFace.DOWN, 3).isPassable())
				continue;
			LivingEntity warden = (LivingEntity) location.getWorld().spawnEntity(spawn, EntityType.WARDEN);
			warden.setInvisible(true);
			Utils.mergeEntityData(warden, "{Brain:{memories:{\"minecraft:dig_cooldown\":{ttl:1200L,value:{}},\"minecraft:is_emerging\":{ttl:134L,value:{}}}}}");
			plugin.getServer().getScheduler().runTaskLater(plugin, () -> warden.setInvisible(false), 10);
			((org.bukkit.entity.Warden) warden).setAnger(target, 60);
			break;
		}
	}
}
