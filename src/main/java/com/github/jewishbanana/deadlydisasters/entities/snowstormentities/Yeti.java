package com.github.jewishbanana.deadlydisasters.entities.snowstormentities;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.BlockVector;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomDropsFactory;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.CustomHead;
import com.github.jewishbanana.deadlydisasters.entities.EntityHandler;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class Yeti extends CustomEntity {
	
	private Random rand;
	private UUID[] standUUID = new UUID[19];
	private ArmorStand[] stands = new ArmorStand[19];
	private int cooldown = 0, jumpTicks;
	private Vector jumpVel;
	private Location past,step;
	private boolean alive = true;

	public Yeti() {
	}
	public Yeti(Mob entity, Main plugin, Random rand) {
		super(entity, plugin);
		this.entityType = CustomEntityType.YETI;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		this.rand = rand;
		
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.getString("entities.yeti"));
		for (int i=0; i < 19; i++)
			spawnStand(i);
		
		if (plugin.mcVersion >= 1.16)
			entity.getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK).setBaseValue(3);
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(entityType.getDamage());
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
		entity.setHealth(entityType.getHealth());
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.28);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40);
		entity.setMetadata("dd-yeti", new FixedMetadataValue(plugin, "protected"));
		entity.setSilent(true);
		entity.setCanPickupItems(false);
		entity.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		step = entity.getLocation();
	}
	@Override
	public void tick() {
		if (entity == null || !alive)
			return;
		for (int i=0; i < stands.length; i++)
			if (stands[i] == null || stands[i].isDead())
				spawnStand(i);
		if (entity.isDead()) {
			entity.remove();
			if (entity.getKiller() != null) {
				CustomDropsFactory.generateDrops(entity.getLocation(), entityType);
				Map<ArmorStand,Vector> vecs = new HashMap<>();
				for (ArmorStand e : stands) {
					vecs.put(e, new Vector((rand.nextDouble()-0.5)/7,-0.001,(rand.nextDouble()-0.5)/7));
					e.setRotation(rand.nextInt(360), 0);
				}
				int[] deathTicks = {(int) entityType.grabCustomSetting("corpse_life_ticks")};
				new RepeatingTask(plugin, 0, 1) {
					@Override
					public void run() {
						deathTicks[0]--;
						if (deathTicks[0] <= 0) {
							cancel();
							clean();
						}
						Iterator<Entry<ArmorStand, Vector>> it = vecs.entrySet().iterator();
						while (it.hasNext()) {
							Entry<ArmorStand, Vector> entry = it.next();
							ArmorStand e = entry.getKey();
							if (e.getLocation().clone().add(0,1.4,0).getBlock().isPassable()) {
								Vector vec = entry.getValue();
								e.teleport(e.getLocation().clone().add(vec));
								entry.setValue(vec.setY(vec.getY()-0.003));
							} else it.remove();
						}
					}
				};
			} else
				clean();
			alive = false;
			return;
		}
		if (entity.getNoDamageTicks() == 20)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_RAVAGER_HURT, SoundCategory.HOSTILE, 1f, .5f);
		Location loc = entity.getLocation();
		stands[0].setHeadPose(new EulerAngle(Math.toRadians(loc.getPitch()), 0, 0));
		if (plugin.mcVersion >= 1.17)
			entity.getWorld().spawnParticle(Particle.SNOWFLAKE, loc.clone().add(0, 1.4, 0), 10, .5, 0.6, .5, 0.04);
		Vector direction = entity.getLocation().getDirection();
		Vector left = new Vector(direction.getZ(), direction.getY(), -direction.getX());
		Vector right = new Vector(-direction.getZ(), direction.getY(), direction.getX());
		
		stands[0].teleport(loc.clone().add(0, 0.7, 0).add(direction.clone().multiply(0.35)));
		stands[1].teleport(loc.clone().add(0, 0.4, 0).add(direction.clone().multiply(0.3)));
		stands[2].teleport(loc.clone().add(0, 0.5, 0).add(left.clone().multiply(0.2)).add(direction.clone().multiply(0.15)));
		stands[3].teleport(loc.clone().add(0, 0.5, 0).add(right.clone().multiply(0.2)).add(direction.clone().multiply(0.15)));
		stands[4].teleport(loc.clone().add(0, 0.1, 0).add(left.clone().multiply(0.3)).add(direction.clone().multiply(0.25)));
		stands[5].teleport(loc.clone().add(0, 0.1, 0).add(right.clone().multiply(0.3)).add(direction.clone().multiply(0.25)));
		stands[6].teleport(loc.clone().add(0, -0.4, 0).add(left.clone().multiply(0.2)).add(direction.clone().multiply(0.15)));
		stands[7].teleport(loc.clone().add(0, -0.4, 0).add(right.clone().multiply(0.2)).add(direction.clone().multiply(0.15)));
		stands[8].teleport(loc.clone().add(0, -0.9, 0).add(left.clone().multiply(0.35)).add(direction.clone().multiply(0.1)));
		stands[9].teleport(loc.clone().add(0, -0.9, 0).add(right.clone().multiply(0.35)).add(direction.clone().multiply(0.1)));
		stands[10].teleport(loc.clone().add(0, -1.4, 0).add(left.clone().multiply(0.35)));
		stands[11].teleport(loc.clone().add(0, -1.4, 0).add(right.clone().multiply(0.35)));
		stands[12].teleport(loc.clone().add(0, 0.1, 0).add(left.clone().multiply(0.3)).add(direction.clone().multiply(-1).multiply(0.25)));
		stands[13].teleport(loc.clone().add(0, 0.1, 0).add(right.clone().multiply(0.3)).add(direction.clone().multiply(-1).multiply(0.25)));
		stands[14].teleport(loc.clone().add(0, -0.4, 0).add(left.clone().multiply(0.2)).add(direction.clone().multiply(-1).multiply(0.15)));
		stands[15].teleport(loc.clone().add(0, -0.4, 0).add(right.clone().multiply(0.2)).add(direction.clone().multiply(-1).multiply(0.15)));
		stands[16].teleport(loc.clone().add(0, 0.15, 0).add(left.clone().multiply(0.6)));
		stands[17].teleport(loc.clone().add(0, 0.15, 0).add(right.clone().multiply(0.6)));
		stands[18].teleport(loc.clone().add(0, 0.6, 0));
		
		if (jumpTicks > 0) {
			jumpTicks--;
			entity.setVelocity(entity.getVelocity().add(jumpVel));
			if (entity.isOnGround()) {
				jumpTicks = 0;
				entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_GLASS_FALL, SoundCategory.HOSTILE, 2f, .5f);
				if (plugin.mcVersion >= 1.17)
					entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.HOSTILE, 2f, 0.6f);
				else
					entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 0.5f, 1f);
			}
		}
		if (step.distanceSquared(entity.getLocation()) > 0.8 && entity.isOnGround()) {
			step = entity.getLocation();
			entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_SNOW_STEP, SoundCategory.HOSTILE, 2f, .5f);
		}
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (alive && entity == null) {
			for (ArmorStand e : stands)
				if (e != null)
					e.remove();
			it.remove();
			return;
		}
		refreshReferences(stands, standUUID);
		if (!alive)
			return;
		
		if (entity.isDead()) {
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_RAVAGER_DEATH, SoundCategory.HOSTILE, 2f, .5f);
			entity.getWorld().spawnParticle(VersionUtils.getSnowShovel(), entity.getLocation().add(0,1.5,0), 40, .3, .4, .3, 0.01);
			it.remove();
			return;
		}
		if (entity.getTarget() == null) {
			for (Entity e : entity.getWorld().getNearbyEntities(entity.getLocation(), 15, 15, 15))
				if (e instanceof Player && !Utils.isPlayerImmune((Player) e)) {
					entity.setTarget((LivingEntity) e);
					entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 2, .5f);
					break;
				}
		} else if (cooldown <= 12 && entity.getTarget().getLocation().getY()-1 > entity.getLocation().getY() && past != null && past.distanceSquared(entity.getLocation()) < 0.8) {
			entity.setVelocity(new Vector(entity.getLocation().getX() - entity.getTarget().getLocation().getX(), 0, entity.getLocation().getZ() - entity.getTarget().getLocation().getZ()).normalize().multiply(-1).setY(0.9));
			jumpVel = new Vector(entity.getLocation().getX() - entity.getTarget().getLocation().getX(), 0, entity.getLocation().getZ() - entity.getTarget().getLocation().getZ()).normalize().multiply(-1).multiply(0.01).setY(0);
			jumpTicks = 20;
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PANDA_BITE, SoundCategory.HOSTILE, 2f, .5f);
		}
		past = entity.getLocation().clone();
		if (rand.nextInt(10) == 0) {
			int num = rand.nextInt(4);
			if (num == 0)
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_RAVAGER_AMBIENT, SoundCategory.HOSTILE, 1, .5f);
			else if (num == 1)
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_RAVAGER_ATTACK, SoundCategory.HOSTILE, 1, .5f);
			else if (num == 2)
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_RAVAGER_CELEBRATE, SoundCategory.HOSTILE, 1, .5f);
			else if (num == 3)
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_RAVAGER_STUNNED, SoundCategory.HOSTILE, 1, .5f);
		}
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		if (entity.getTarget() != null && entity.getTarget().getWorld().equals(entity.getWorld()) && entity.getTarget().getLocation().distanceSquared(entity.getLocation()) <= 49 && entity.isOnGround()) {
			cooldown = 15;
			World world = entity.getWorld();
			entity.addPotionEffect(new PotionEffect(VersionUtils.getSlowness(), 40, 5, true, false));
			LivingEntity target = entity.getTarget();
			ArmorStand tempTarget = (ArmorStand) world.spawnEntity(entity.getLocation().clone().add(entity.getLocation().getDirection().multiply(0.1)), EntityType.ARMOR_STAND);
			tempTarget.setSmall(true);
			entity.setTarget(tempTarget);
			int[] current = {2};
			Location entityHit = entity.getLocation().clone();
			BlockVector block = new BlockVector(entityHit.getX(), entityHit.getY(), entityHit.getZ());
			BlockData bd = Material.PACKED_ICE.createBlockData();
			if (plugin.mcVersion >= 1.16)
				world.playSound(entityHit, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.HOSTILE, 1f, 0.6f);
			world.playSound(entityHit, Sound.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 2, .5f);
			new RepeatingTask(plugin, 10, 5) {
				@Override
				public void run() {
					if (!tempTarget.isDead()) {
						tempTarget.remove();
						if (target != null) entity.setTarget(target);
					}
					current[0]++;
					for (int x = -current[0]; x < current[0]; x++)
						for (int z = -current[0]; z < current[0]; z++) {
							Vector position = block.clone().add(new Vector(x, 0, z));
							Block b = world.getBlockAt(position.toLocation(world));
							if (block.distance(position) >= (current[0] - 1) && block.distance(position) <= current[0]) {
								if (b.isPassable()) {
									for (int i=0; i < 3; i++) {
										b = b.getRelative(BlockFace.DOWN);
										if (!b.isPassable())
											break;
									}
									if (b.isPassable())
										continue;
									else
										b = b.getRelative(BlockFace.UP);
								} else {
									for (int i=0; i < 3; i++) {
										b = b.getRelative(BlockFace.UP);
										if (b.isPassable())
											break;
									}
									if (!b.isPassable())
										continue;
								}
								world.spawnParticle(VersionUtils.getSnowShovel(), b.getLocation().clone().add(0.5,0.5,0.5), 20, .3, .5, .3, 0.001);
								world.spawnParticle(VersionUtils.getBlockCrack(), b.getLocation().clone().add(0.5,0.5,0.5), 3, .3, .5, .3, 0.001, bd);
								if (b.getType() == Material.FIRE) {
									b.setType(Material.AIR);
									world.playSound(b.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1f, 1f);
									world.spawnParticle(VersionUtils.getLargeSmoke(), b.getLocation().clone().add(0.5,0.2,0.5), 5, .3, .5, .3, 0.001);
								}
								if (plugin.mcVersion >= 1.17)
									world.playSound(b.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.HOSTILE, 0.5f, 0.5f);
								else
									world.playSound(b.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 0.5f, 1f);
								for (Entity e : world.getNearbyEntities(b.getLocation().clone().add(0.5,0.5,0.5), 0.5, 1, 0.5)) {
									if (e.equals(entity) || e.equals(tempTarget))
										continue;
									e.setVelocity(new Vector(e.getLocation().getX() - entityHit.getX(), 0, e.getLocation().getZ() - entityHit.getZ()).normalize().multiply(0.2).setY(0.8));
									if (e instanceof LivingEntity && !(e instanceof Player && Utils.isPlayerImmune((Player) e))) {
										((LivingEntity) e).addPotionEffect(new PotionEffect(VersionUtils.getSlowness(), 100, 7, true, false));
										if (plugin.mcVersion >= 1.17)
											e.setFreezeTicks(500);
									}
								}
							}
						}
					if (current[0] >= 10)
						cancel();
				}
			};
		}
	}
	@Override
	public void clean() {
		for (ArmorStand e : stands)
			if (e != null)
				e.remove();
	}
	@Override
	public void update(FileConfiguration file) {
	}
	private void spawnStand(int id) {
		World world = entity.getWorld();
		Location loc = entity.getLocation().add(150,100,0);
		switch (id) {
		case 0:
			stands[id] = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
			equipStand(stands[id], CustomHead.YETI.getHead());
			break;
		case 1:
		case 6:
		case 7:
		case 8:
		case 9:
		case 14:
		case 15:
		case 16:
		case 17:
			stands[id] = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
			equipStand(stands[id], Material.SNOW_BLOCK);
			break;
		case 2:
			stands[id] = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
			equipStand(stands[id], Material.SNOW_BLOCK);
			stands[id].setHeadPose(new EulerAngle(0,0.9,0.4)); //.9 .2
			break;
		case 3:
			stands[id] = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
			equipStand(stands[id], Material.SNOW_BLOCK);
			stands[id].setHeadPose(new EulerAngle(0,0.9,-0.4));
			break;
		case 4:
		case 5:
		case 10:
		case 11:
		case 12:
		case 13:
			stands[id] = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
			equipStand(stands[id], Material.PACKED_ICE);
			break;
		case 18:
			stands[id] = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
			equipStand(stands[id], Material.SNOW_BLOCK);
			stands[id].setHeadPose(new EulerAngle(0.4,0,0));
			break;
		default:
			return;	
		}
		standUUID[id] = stands[id].getUniqueId();
		stands[id].getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
	}
	private void equipStand(ArmorStand e, Material head) {
		lockStand(e);
		e.getEquipment().setHelmet(new ItemStack(head));
	}
	private void equipStand(ArmorStand e, ItemStack head) {
		lockStand(e);
		e.getEquipment().setHelmet(head);
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
