package deadlydisasters.entities.infestedcavesentities;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.Random;

import org.bukkit.Location;
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
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import deadlydisasters.entities.CustomDropsFactory;
import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.CustomHead;
import deadlydisasters.entities.EntityHandler;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;
import deadlydisasters.utils.Utils;

public class InfestedDevourer extends CustomEntity {
	
	private ArmorStand stand;
	private Random rand;
	private double speed,damage;
	private LivingEntity target;
	private Location targetLoc;
	private int cooldown = 5, ticks = 0, damageTicks = 0;
	private Material material;
	private ArmorStand tempTarget;
	private Vector backVec;
	private boolean isAlpha;
	private ArmorStand targetBlock;
	private Location locationObject;
	
	public static Queue<Location> blockTargets = new ArrayDeque<>();

	@SuppressWarnings("deprecation")
	public InfestedDevourer(Zombie entity, Main plugin, Random rand) {
		super(entity, plugin);
		this.rand = rand;
		this.entityType = CustomEntityType.INFESTEDDEVOURER;
		this.species = entityType.species;
		
		Utils.clearEntityOfItems(entity);
		if (plugin.mcVersion >= 1.16)
			entity.setBaby();
		else
			entity.setBaby(true);
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true, false));
		entity.setCanPickupItems(false);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(25);
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(0);
		entity.setMetadata("dd-infesteddevourer", new FixedMetadataValue(plugin, "protected"));
		entity.setCustomName(Languages.langFile.getString("entities.infestedDevourer"));
		entity.setSilent(true);
		if (rand.nextDouble()*100 < (double) entityType.grabCustomSetting("alpha_spawn_chance")) {
			isAlpha = true;
			entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth()*2);
			entity.setHealth(entityType.getHealth()*1.5);
			damage = entityType.getDamage()*2;
			entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.35);
		} else {
			entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
			entity.setHealth(entityType.getHealth());
			damage = entityType.getDamage();
			entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.25);
		}
		speed = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue();
		spawnHeadStand();
		
		if (plugin.mcVersion >= 1.19)
			material = Material.SCULK;
		else
			material = Material.NETHERRACK;
	}
	@Override
	public void tick() {
		if (entity == null || entity.isDead())
			return;
		if (entity.getNoDamageTicks() == 20)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_FOX_EAT, SoundCategory.HOSTILE, 1, 0.5F);
		if (targetBlock != null && targetBlock.getLocation().getBlock().getType() == Material.AIR) {
			if (entity.getTarget() == targetBlock)
				entity.setTarget(null);
			targetBlock.remove();
			targetBlock = null;
		}
		if (ticks > 0) {
			ticks--;
			if (ticks <= 0) {
				entity.setAI(true);
				plugin.getServer().getScheduler().runTaskLater(plugin, () -> entity.setInvulnerable(false), 10);
				entity.setGravity(true);
				entity.setTarget(target);
				Location toTarget = null;
				Vector toVector = null;
				double distanceSquared = 0;
				for (int i=0; i < 10; i++) {
					Vector vec = new Vector((rand.nextDouble()*2)-1, (rand.nextDouble()*2)-1, (rand.nextDouble()*2)-1).normalize().multiply(0.8);
					Location tempLoc = targetLoc.clone().add(vec);
					for (int c=0; c < 6; c++) {
						if (!tempLoc.getBlock().isPassable()) {
							if (tempLoc.getBlock().getType() == material) {
								double tempDistance = targetLoc.distanceSquared(tempLoc);
								if (toTarget == null || tempDistance < distanceSquared) {
									toTarget = tempLoc;
									toVector = vec;
									distanceSquared = tempDistance;
								}
							}
							break;
						}
						tempLoc.add(vec);
					}
				}
				if (toTarget == null)
					entity.setVelocity(backVec);
				else {
					stand.remove();
					entity.teleport(toTarget.getBlock().getLocation().add(0.5,0.5,0.5));
					spawnHeadStand();
					entity.setVelocity(toVector.clone().multiply(-0.5));
				}
			}
			stand.teleport(entity.getLocation().subtract(0,.6,0));
			if (entity.getTarget() != null) {
				Location newLocation = entity.getLocation().setDirection(entity.getTarget().getLocation().toVector().subtract(entity.getLocation().toVector()));
				stand.setRotation(newLocation.getYaw(), 0);
				stand.setHeadPose(new EulerAngle(Math.toRadians(newLocation.getPitch()),0,0));
			} else
				stand.setHeadPose(new EulerAngle(Math.toRadians(entity.getLocation().getPitch()),0,0));
			return;
		}
		if (damageTicks > 0)
			damageTicks--;
		if (tempTarget != null) {
			entity.setTarget(tempTarget);
			if (entity.getLocation().distanceSquared(tempTarget.getLocation()) <= 3.5) {
				entity.setAI(false);
				entity.setInvulnerable(true);
				entity.setGravity(false);
				backVec = Utils.getVectorTowards(entity.getLocation(), tempTarget.getLocation()).multiply(-0.3);
				entity.teleport(tempTarget.getLocation().subtract(0,0.3,0));
				tempTarget.remove();
				tempTarget = null;
				entity.setTarget(null);
				ticks = 60;
			} else if (tempTarget.getTicksLived() >= 160) {
				tempTarget.remove();
				tempTarget = null;
				entity.setTarget(target);
			}
		} else if (entity.getTarget() != null) {
			if (entity.isOnGround()) {
				entity.setVelocity(Utils.getVectorTowards(entity.getLocation(), entity.getTarget().getLocation()).multiply(speed).setY(0.45));
				if (plugin.mcVersion >= 1.19)
					entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_SCULK_STEP, SoundCategory.HOSTILE, 1, 1);
			}
			if (damageTicks <= 0 && entity.getTarget().getLocation().distanceSquared(entity.getLocation()) <= 1.25) {
				if (entity.getTarget() == targetBlock) {
					if (!Utils.isBlockImmune(targetBlock.getLocation().getBlock().getType())) {
						entity.getWorld().spawnParticle(Particle.BLOCK_CRACK, targetBlock.getLocation(), 10, 0.5, 0.5, 0.5, 0.1, targetBlock.getLocation().getBlock().getBlockData());
						targetBlock.getLocation().getBlock().setType(Material.AIR);
						entity.setVelocity(Utils.getVectorTowards(entity.getLocation(), targetBlock.getLocation()).multiply(0.5));
						entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_FOX_BITE, SoundCategory.HOSTILE, 1, 0.5F);
					}
					blockTargets.remove(locationObject);
					targetBlock.remove();
					targetBlock = null;
					entity.setTarget(null);
					updateTarget();
					damageTicks = 15;
				} else if (entity.hasLineOfSight(entity.getTarget())) {
					Utils.damageEntity(entity.getTarget(), damage, "dd-devourdeath");
					entity.getWorld().spawnParticle(Particle.BLOCK_CRACK, entity.getLocation().add(entity.getLocation().getDirection().multiply(0.3)), 5, .2, .2, .2, 0.1, Material.REDSTONE_BLOCK.createBlockData());
					damageTicks = 15;
					cooldown = 8;
					entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_FOX_BITE, SoundCategory.HOSTILE, 1, 0.5F);
				}
			}
		}
		stand.teleport(entity.getLocation().subtract(0,.6,0));
		if (entity.getTarget() != null) {
			Location newLocation = entity.getLocation().setDirection(entity.getTarget().getLocation().toVector().subtract(entity.getLocation().toVector()));
			stand.setRotation(newLocation.getYaw(), 0);
			stand.setHeadPose(new EulerAngle(Math.toRadians(newLocation.getPitch()),0,0));
		} else
			stand.setHeadPose(new EulerAngle(Math.toRadians(entity.getLocation().getPitch()),0,0));
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (entity == null || stand == null) {
			clean();
			it.remove();
			return;
		}
		if (entity.isDead()) {
			if (plugin.mcVersion >= 1.19)
				entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_BREAK, SoundCategory.HOSTILE, 2, 0.5F);
			if (entity.getKiller() != null)
				CustomDropsFactory.generateDrops(entity.getLocation(), entityType);
			clean();
			it.remove();
			return;
		}
		stand = (ArmorStand) plugin.getServer().getEntity(stand.getUniqueId());
		if (ticks <= 0 && tempTarget == null && entity.getTarget() != null) {
			if (entity.getTarget().equals(target))
				cooldown--;
			else {
				target = entity.getTarget();
				cooldown = 8;
			}
			if (cooldown <= 0) {
				label:
				for (int i=0; i < 10; i++) {
					Vector vec = new Vector((rand.nextDouble()*2)-1, 0, (rand.nextDouble()*2)-1).normalize().setY(((rand.nextDouble()/2)-0.7)/2).multiply(0.8);
					Location tempLoc = entity.getLocation().add(0,1,0).add(vec);
					for (int c=0; c < 6; c++) {
						if (!tempLoc.getBlock().isPassable())
							if (tempLoc.getBlock().getType() == material) {
								tempTarget = spawnTarget(tempLoc.getBlock().getLocation().add(0.5,0.5,0.5));
								targetLoc = target.getLocation().add(0,1,0);
								break label;
							} else
								break;
						tempLoc.add(vec);
					}
				}
				cooldown = 5;
			}
		}
		if (rand.nextInt(8) == 0)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_FOX_AGGRO, SoundCategory.HOSTILE, 1, 0.5F);
	}
	@Override
	public void clean() {
		if (stand != null)
			stand.remove();
		if (tempTarget != null)
			tempTarget.remove();
		if (targetBlock != null)
			targetBlock.remove();
	}
	@Override
	public void update(FileConfiguration file) {
	}
	private ArmorStand spawnTarget(Location loc) {
		ArmorStand newStand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
		if (plugin.mcVersion >= 1.16)
			newStand.setInvisible(true);
		else
			newStand.setVisible(false);
		newStand.setMarker(true);
		newStand.setGravity(false);
		newStand.setSmall(true);
		newStand.setCollidable(false);
		if (plugin.mcVersion >= 1.16) {
			newStand.addEquipmentLock(EquipmentSlot.CHEST, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			newStand.addEquipmentLock(EquipmentSlot.FEET, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			newStand.addEquipmentLock(EquipmentSlot.HAND, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			newStand.addEquipmentLock(EquipmentSlot.HEAD, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			newStand.addEquipmentLock(EquipmentSlot.LEGS, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			newStand.addEquipmentLock(EquipmentSlot.OFF_HAND, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
		}
		newStand.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		return newStand;
	}
	private void spawnHeadStand() {
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
		if (isAlpha)
			stand.getEquipment().setHelmet(CustomHead.INFESTEDDEVOURERALPHA.getHead());
		else
			stand.getEquipment().setHelmet(CustomHead.INFESTEDDEVOURER.getHead());
		stand.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
	}
	public void updateTarget() {
		if (entity == null || entity.isDead() || entity.getTarget() == tempTarget)
			return;
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				Location location = null;
				double distance = 0;
				for (Location loc : blockTargets)
					if (loc.getWorld().equals(entity.getWorld()) && (location == null || location.distanceSquared(entity.getLocation()) < distance)) {
						location = loc;
						distance = location.distanceSquared(entity.getLocation());
					}
				if (location == null || distance > 400)
					return;
				locationObject = location;
				plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
					@Override
					public void run() {
						targetBlock = spawnTarget(locationObject.clone().add(0.5,0.5,0.5));
						entity.setTarget(targetBlock);
					}
				});
			}
		});
	}
}
