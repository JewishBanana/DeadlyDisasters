package com.github.jewishbanana.deadlydisasters.entities.halloweenentities;

import java.util.Iterator;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.AnimatedEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.CustomHead;
import com.github.jewishbanana.deadlydisasters.entities.EntityHandler;
import com.github.jewishbanana.deadlydisasters.handlers.ItemsHandler;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.utils.AnimationHandler;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;
import com.github.jewishbanana.deadlydisasters.utils.AnimationHandler.BodyPart;

public class Scarecrow extends AnimatedEntity {
	
	private int cooldown;
	private Random rand;
	private UUID standUUID;
	private AnimationHandler throwAnimation;
	private boolean throwing;
	private Slime projectile;
	private ArmorStand pumpkin;
	private Location step;

	public Scarecrow(Zombie tempEntity, Main plugin, Random rand) {
		super(tempEntity, plugin);
		this.entityType = CustomEntityType.SCARECROW;
		this.rand = rand;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.setMetadata("dd-scarecrow", new FixedMetadataValue(plugin, "protected"));
		entity.setMetadata("dd-halloweenmobs", plugin.fixedData);
		entity.getEquipment().setHelmetDropChance(0);
		entity.setCanPickupItems(false);
		entity.setSilent(true);
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
		entity.setHealth(entityType.getHealth());
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(entityType.getDamage());
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.5);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40);
		entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true, false));
		entity.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
		
		stand = (ArmorStand) entity.getWorld().spawnEntity(entity.getLocation().clone().add(100,100,0), EntityType.ARMOR_STAND);
		stand = Utils.lockArmorStand(stand, false, true, true);
		stand.setBasePlate(false);
		stand.getEquipment().setItemInMainHand(new ItemStack(Material.JACK_O_LANTERN));
		ItemStack[] armor = {new ItemStack(Material.LEATHER_BOOTS), new ItemStack(Material.LEATHER_LEGGINGS), new ItemStack(Material.LEATHER_CHESTPLATE), CustomHead.SCARECROW.getHead()};
		for (int i=0; i < 3; i++) {
			LeatherArmorMeta meta = (LeatherArmorMeta) armor[i].getItemMeta();
			meta.setColor(Color.fromRGB(105, 68, 31));
			armor[i].setItemMeta(meta);
		}
		stand.getEquipment().setArmorContents(armor);
		stand.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		standUUID = stand.getUniqueId();
		if (!tempEntity.isAdult())
			stand.setSmall(true);
		
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.getString("halloween.scarecrow"));
		
		this.walkAnimation = new AnimationHandler(true, true, true);
		this.walkAnimation.setContinueFrame(1);
		this.walkAnimation.setAnimations(this.walkAnimation.new Animation(
					this.walkAnimation.new AnimationCheckpoint(BodyPart.RIGHT_LEG, 0, 0, 0, -30, 0, 10, 12, false, 0.5, 0.5),
					this.walkAnimation.new AnimationCheckpoint(BodyPart.LEFT_LEG, 0, 0, 0, 30, 0, -10, 12, false, 0.5, 0.5),
					this.walkAnimation.new AnimationCheckpoint(BodyPart.RIGHT_ARM, 0, 0, 0, 20, 0, 10, 12, false, 0.5, 0.5),
					this.walkAnimation.new AnimationCheckpoint(BodyPart.LEFT_ARM, 0, 0, 0, -20, 0, -10, 12, false, 0.5, 0.5),
					this.walkAnimation.new AnimationCheckpoint(BodyPart.HEAD, 0, 0, 0, 0, 0, 30, 12, false, 0.5, 0.5)
				),
				this.walkAnimation.new Animation(
						this.walkAnimation.new AnimationCheckpoint(BodyPart.RIGHT_LEG, -30, 0, 10, 0, 0, 0, 12, false, 0.5, 0),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.LEFT_LEG, 30, 0, -10, 0, 0, 0, 12, false, 0.5, 0),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.RIGHT_ARM, 20, 0, 10, 0, 0, 0, 12, false, 0.5, 0),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.LEFT_ARM, -20, 0, -10, 0, 0, 0, 12, false, 0.5, 0),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.HEAD, 0, 0, 30, 0, 0, 0, 12, false, 0.5, 0)
				),
				this.walkAnimation.new Animation(
						this.walkAnimation.new AnimationCheckpoint(BodyPart.RIGHT_LEG, 0, 0, 0, 30, 0, 10, 12, false, 0, 0.5),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.LEFT_LEG, 0, 0, 0, -30, 0, -10, 12, false, 0, 0.5),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.RIGHT_ARM, 0, 0, 0, -20, 0, 10, 12, false, 0, 0.5),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.LEFT_ARM, 0, 0, 0, 20, 0, -10, 12, false, 0, 0.5),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.HEAD, 0, 0, 0, 0, 0, -30, 12, false, 0, 0.5)
				),
				this.walkAnimation.new Animation(
						this.walkAnimation.new AnimationCheckpoint(BodyPart.RIGHT_LEG, 30, 0, 10, 0, 0, 0, 12, false, 0.5, 0),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.LEFT_LEG, -30, 0, -10, 0, 0, 0, 12, false, 0.5, 0),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.RIGHT_ARM, -20, 0, 10, 0, 0, 0, 12, false, 0.5, 0),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.LEFT_ARM, 20, 0, -10, 0, 0, 0, 12, false, 0.5, 0),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.HEAD, 0, 0, -30, 0, 0, 0, 12, false, 0.5, 0)
				),
				this.walkAnimation.new Animation(
						this.walkAnimation.new AnimationCheckpoint(BodyPart.RIGHT_LEG, 0, 0, 0, -30, 0, 10, 12, false, 0, 0.5),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.LEFT_LEG, 0, 0, 0, 30, 0, -10, 12, false, 0, 0.5),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.RIGHT_ARM, 0, 0, 0, 20, 0, 10, 12, false, 0, 0.5),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.LEFT_ARM, 0, 0, 0, -20, 0, -10, 12, false, 0, 0.5),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.HEAD, 0, 0, 0, 0, 0, 30, 12, false, 0, 0.5)
				)
			);
		this.rotationSpeed = 20f;
		
		this.throwAnimation = new AnimationHandler(true, true, false);
		this.throwAnimation.setAnimations(this.throwAnimation.new Animation(
					this.throwAnimation.new AnimationCheckpoint(BodyPart.RIGHT_LEG, 0, 0, 0, -10, 0, 5, 8, true, 0.5, 0.5),
					this.throwAnimation.new AnimationCheckpoint(BodyPart.LEFT_LEG, 0, 0, 0, 10, 0, -5, 8, true, 0.5, 0.5),
					this.throwAnimation.new AnimationCheckpoint(BodyPart.RIGHT_ARM, 0, 0, 0, -360, 0, 15, 20, false, 0.5, 0.5),
					this.throwAnimation.new AnimationCheckpoint(BodyPart.LEFT_ARM, 0, 0, 0, -10, 0, -5, 8, true, 0.5, 0.5),
					this.throwAnimation.new AnimationCheckpoint(BodyPart.HEAD, 0, 0, 0, 0, 0, 20, 8, true, 0.5, 0.5)
				),
				this.throwAnimation.new Animation(
						this.throwAnimation.new AnimationCheckpoint(BodyPart.RIGHT_LEG, 0, 0, 0, 10, 0, 5, 6, true, 0.5, 0.5),
						this.throwAnimation.new AnimationCheckpoint(BodyPart.LEFT_LEG, 0, 0, 0, -10, 0, -5, 6, true, 0.5, 0.5),
						this.throwAnimation.new AnimationCheckpoint(BodyPart.RIGHT_ARM, 0, 0, 0, -360, 0, 15, 12, false, 0.5, 0.5),
						this.throwAnimation.new AnimationCheckpoint(BodyPart.LEFT_ARM, 0, 0, 0, 10, 0, -5, 6, true, 0.5, 0.5),
						this.throwAnimation.new AnimationCheckpoint(BodyPart.HEAD, 0, 0, 0, 0, 0, -15, 6, true, 0.5, 0.5)
					),
				this.throwAnimation.new Animation(
						this.throwAnimation.new AnimationCheckpoint(BodyPart.RIGHT_LEG, 0, 0, 0, -10, 0, 5, 4, true, 0.5, 0.5),
						this.throwAnimation.new AnimationCheckpoint(BodyPart.LEFT_LEG, 0, 0, 0, 10, 0, -5, 4, true, 0.5, 0.5),
						this.throwAnimation.new AnimationCheckpoint(BodyPart.RIGHT_ARM, 0, 0, 0, -380, 0, 15, 6, false, 0.5, 0.5),
						this.throwAnimation.new AnimationCheckpoint(BodyPart.LEFT_ARM, 0, 0, 0, -10, 0, -5, 4, true, 0.5, 0.5),
						this.throwAnimation.new AnimationCheckpoint(BodyPart.HEAD, 0, 0, 0, 0, 0, 15, 4, true, 0.5, 0.5)
					)
			);
		step = entity.getLocation();
	}
	@Override
	public void tick() {
		if (entity == null || stand == null)
			return;
		super.tick();
		if (entity.getNoDamageTicks() == 20)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_SKELETON_HURT, SoundCategory.HOSTILE, 2f, .5f);
		if (step.distanceSquared(entity.getLocation()) > 6 && entity.isOnGround()) {
			step = entity.getLocation();
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_SKELETON_STEP, SoundCategory.HOSTILE, 1f, .5f);
		}
		Vector dir =  entity.getVelocity().multiply(stand.getLocation().getDirection().setY(1.0));
		entity.setVelocity(dir);
		if (dir.setY(0).lengthSquared() > 0.005)
			this.walkAnimation.setFrameRate(2);
		else
			this.walkAnimation.setFrameRate(1);
		if (throwing) {
			throwAnimation.tick(stand);
			if (throwAnimation.isFinished()) {
				throwing = false;
				if (entity.getTarget() != null && entity.getTarget().getWorld().equals(entity.getWorld()) && entity.getTarget().getLocation().distanceSquared(entity.getLocation()) < 400)
					throwPumpkin(entity.getTarget());
			}
		}
		entity.getWorld().spawnParticle(VersionUtils.getBlockDust(), entity.getLocation().clone().add(0,1.1,0), 1, .2, .4, .2, 1, Material.JACK_O_LANTERN.createBlockData());
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (entity == null || stand == null) {
			clean();
			it.remove();
			return;
		}
		stand = (ArmorStand) plugin.getServer().getEntity(standUUID);
		if (entity.isDead()) {
			if (plugin.mcVersion >= 1.16)
				entity.getWorld().spawnParticle(Particle.SOUL, entity.getLocation().add(0,.5,0), 15, .3, .5, .3, .03);
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_SKELETON_DEATH, SoundCategory.HOSTILE, 2f, .5f);
			if (entity.getKiller() != null && CustomEntityType.dropsEnabled && plugin.random.nextDouble() * 100 < 15.0)
				entity.getWorld().dropItemNaturally(entity.getLocation(), ItemsHandler.spookyPumpkin);
			clean();
			it.remove();
			return;
		}
		if (rand.nextInt(6) == 0)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_SKELETON_AMBIENT, SoundCategory.HOSTILE, 2f, .5f);
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		if (Utils.isTargetInRange(entity, 25, 225, true)) {
			cooldown = 8;
			entity.addPotionEffect(new PotionEffect(VersionUtils.getSlowness(), 60, 5, true, false, false));
			throwAnimation.startAnimation(stand);
			throwing = true;
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 2, .5f);
		}
	}
	@Override
	public void clean() {
		super.clean();
		if (pumpkin != null)
			pumpkin.remove();
		if (projectile != null)
			projectile.remove();
	}
	private void throwPumpkin(LivingEntity target) {
		pumpkin = Utils.lockArmorStand((ArmorStand) entity.getWorld().spawnEntity(entity.getLocation().add(100,100,0), EntityType.ARMOR_STAND), true, false, true);
		pumpkin.getEquipment().setHelmet(new ItemStack(Material.JACK_O_LANTERN));
		pumpkin.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		projectile = entity.getWorld().spawn(entity.getLocation().add(0,100,0), Slime.class, slime -> {
			slime.setSize(0);
			slime.setSilent(true);
			slime.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(100.0);
			slime.setHealth(100.0);
			slime.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(0.0);
			slime.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true, false));
			slime.teleport(entity);
			slime.teleport(slime.getLocation().add(0,1,0));
			slime.setGravity(false);
		});
		Location targetLoc = target.getLocation().add(0,target.getHeight()/2,0);
		Vector vec = Utils.getVectorTowards(projectile.getLocation(), targetLoc);
		vec.setX(vec.getX()/3);
		vec.setZ(vec.getZ()/3);
		vec.setY((targetLoc.distance(entity.getLocation())/13)+((targetLoc.getY()-entity.getLocation().getY())/30));
		stand.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
		attack();
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				if (pumpkin == null || pumpkin.isDead() || projectile == null || projectile.isDead() || entity == null) {
					if (pumpkin != null)
						pumpkin.remove();
					pumpkin = null;
					if (projectile != null)
						projectile.remove();
					if (stand != null && !stand.isDead())
						stand.getEquipment().setItemInMainHand(new ItemStack(Material.JACK_O_LANTERN));
					cancel();
					return;
				}
				projectile.teleport(projectile.getLocation().add(vec));
				pumpkin.teleport(projectile.getLocation().add(0,-2,0));
				double pivot = Math.abs(vec.getX());
				if (Math.abs(vec.getZ()) > pivot)
					pivot = Math.abs(vec.getZ());
				double angle = Math.toDegrees(Math.atan2(Math.abs(vec.getY()), pivot));
				if (vec.getY() >= 0)
					pumpkin.setHeadPose(new EulerAngle(Math.toRadians(360-angle), 0, 0));
				else
					pumpkin.setHeadPose(new EulerAngle(Math.toRadians(360+angle), 0, 0));
				pumpkin.getWorld().spawnParticle(VersionUtils.getBlockCrack(), pumpkin.getLocation().add(0,2,0), 2, .1, .1, .1, 1, Material.JACK_O_LANTERN.createBlockData());
				if (!projectile.getLocation().getBlock().isPassable()) {
					pumpkin.remove();
					projectile.remove();
					if (stand != null && !stand.isDead())
						stand.getEquipment().setItemInMainHand(new ItemStack(Material.JACK_O_LANTERN));
					pumpkin = null;
					projectile.getWorld().createExplosion(projectile.getLocation(), 4f, true, true, entity);
					cancel();
					int[] timer = {20};
					Location explosion = projectile.getLocation();
					new RepeatingTask(plugin, 0, 2) {
						@Override
						public void run() {
							explosion.getWorld().spawnParticle(VersionUtils.getRedstoneDust(), explosion, 30, 2, 2, 2, .001, new DustOptions(Color.ORANGE, 1f));
							explosion.getWorld().spawnParticle(VersionUtils.getRedstoneDust(), explosion, 30, 2, 2, 2, .001, new DustOptions(Color.BLACK, 1f));
							if (--timer[0] <= 0)
								cancel();
						}
					};
					return;
				}
				vec.multiply(0.99);
				vec.setY(vec.getY() - 0.05000000074505806D);
				for (Entity e : projectile.getNearbyEntities(.5, .5, .5))
					if (e instanceof LivingEntity && !(e instanceof ArmorStand) && !e.equals(entity) && !(e instanceof Player && Utils.isPlayerImmune((Player) e))) {
						pumpkin.remove();
						projectile.remove();
						if (stand != null && !stand.isDead())
							stand.getEquipment().setItemInMainHand(new ItemStack(Material.JACK_O_LANTERN));
						pumpkin = null;
						projectile.getWorld().createExplosion(projectile.getLocation(), 4f, true, true, entity);
						cancel();
						int[] timer = {20};
						Location explosion = projectile.getLocation();
						new RepeatingTask(plugin, 0, 2) {
							@Override
							public void run() {
								explosion.getWorld().spawnParticle(VersionUtils.getRedstoneDust(), explosion, 30, 2, 2, 2, .001, new DustOptions(Color.ORANGE, 1f));
								explosion.getWorld().spawnParticle(VersionUtils.getRedstoneDust(), explosion, 30, 2, 2, 2, .001, new DustOptions(Color.BLACK, 1f));
								if (--timer[0] <= 0)
									cancel();
							}
						};
						return;
					}
			}
		};
	}
}
