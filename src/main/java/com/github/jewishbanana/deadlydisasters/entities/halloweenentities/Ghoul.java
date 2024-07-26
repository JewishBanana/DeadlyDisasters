package com.github.jewishbanana.deadlydisasters.entities.halloweenentities;

import java.util.Iterator;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
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

public class Ghoul extends AnimatedEntity {
	
	private Random rand;
	private UUID standUUID;
	public AnimationHandler grabAnimation;
	private AnimationHandler riseAnimation;
	private boolean walking;
	private Block ground;
	private Location toGround,step;
	private int soundTick;

	@SuppressWarnings("deprecation")
	public Ghoul(Zombie tempEntity, Block block, Main plugin, boolean spawnAnimation) {
		super(tempEntity, plugin);
		this.entityType = CustomEntityType.GHOUL;
		this.rand = plugin.random;
		this.species = entityType.species;
		this.ground = block;
		if (block == null)
			walking = true;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.setMetadata("dd-ghoul", new FixedMetadataValue(plugin, "protected"));
		entity.setMetadata("dd-halloweenmobs", plugin.fixedData);
		entity.setCanPickupItems(false);
		entity.setSilent(true);
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
		entity.setHealth(entityType.getHealth());
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(entityType.getDamage());
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.25);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40);
		entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 0, true, false));
		
		if (plugin.mcVersion >= 1.16)
			tempEntity.setAdult();
		else
			tempEntity.setBaby(false);
		
		stand = (ArmorStand) entity.getWorld().spawnEntity(entity.getLocation().clone().add(100,100,0), EntityType.ARMOR_STAND);
		stand = Utils.lockArmorStand(stand, false, true, true);
		stand.setBasePlate(false);
		ItemStack[] armor = {new ItemStack(Material.LEATHER_BOOTS), new ItemStack(Material.LEATHER_LEGGINGS), new ItemStack(Material.LEATHER_CHESTPLATE), CustomHead.ROTTENZOMBIE.getHead()};
		int green = rand.nextInt(50)+25;
		for (int i=0; i < 3; i++) {
			LeatherArmorMeta meta = (LeatherArmorMeta) armor[i].getItemMeta();
			meta.setColor(Color.fromBGR(green-15, green, 1));
			armor[i].setItemMeta(meta);
		}
		stand.getEquipment().setArmorContents(armor);
		stand.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		standUUID = stand.getUniqueId();
		
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.getString("halloween.ghoul"));
		
		step = entity.getLocation();
		
		this.walkAnimation = new AnimationHandler(true, true, true);
		this.walkAnimation.setContinueFrame(1);
		this.walkAnimation.setAnimations(this.walkAnimation.new Animation(
					this.walkAnimation.new AnimationCheckpoint(BodyPart.RIGHT_LEG, 0, 0, 0, -30, 0, 10, 12, false, 0.5, 0.5),
					this.walkAnimation.new AnimationCheckpoint(BodyPart.LEFT_LEG, 0, 0, 0, 30, 0, -10, 12, false, 0.5, 0.5),
					this.walkAnimation.new AnimationCheckpoint(BodyPart.RIGHT_ARM, 0, 0, 0, 20, 0, 10, 12, false, 0.5, 0.5),
					this.walkAnimation.new AnimationCheckpoint(BodyPart.LEFT_ARM, 0, 0, 0, -20, 0, -10, 12, false, 0.5, 0.5)
				),
				this.walkAnimation.new Animation(
						this.walkAnimation.new AnimationCheckpoint(BodyPart.RIGHT_LEG, -30, 0, 10, 0, 0, 0, 12, false, 0.5, 0),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.LEFT_LEG, 30, 0, -10, 0, 0, 0, 12, false, 0.5, 0),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.RIGHT_ARM, 20, 0, 10, 0, 0, 0, 12, false, 0.5, 0),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.LEFT_ARM, -20, 0, -10, 0, 0, 0, 12, false, 0.5, 0)
				),
				this.walkAnimation.new Animation(
						this.walkAnimation.new AnimationCheckpoint(BodyPart.RIGHT_LEG, 0, 0, 0, 30, 0, 10, 12, false, 0, 0.5),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.LEFT_LEG, 0, 0, 0, -30, 0, -10, 12, false, 0, 0.5),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.RIGHT_ARM, 0, 0, 0, -20, 0, 10, 12, false, 0, 0.5),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.LEFT_ARM, 0, 0, 0, 20, 0, -10, 12, false, 0, 0.5)
				),
				this.walkAnimation.new Animation(
						this.walkAnimation.new AnimationCheckpoint(BodyPart.RIGHT_LEG, 30, 0, 10, 0, 0, 0, 12, false, 0.5, 0),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.LEFT_LEG, -30, 0, -10, 0, 0, 0, 12, false, 0.5, 0),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.RIGHT_ARM, -20, 0, 10, 0, 0, 0, 12, false, 0.5, 0),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.LEFT_ARM, 20, 0, -10, 0, 0, 0, 12, false, 0.5, 0)
				),
				this.walkAnimation.new Animation(
						this.walkAnimation.new AnimationCheckpoint(BodyPart.RIGHT_LEG, 0, 0, 0, -30, 0, 10, 12, false, 0, 0.5),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.LEFT_LEG, 0, 0, 0, 30, 0, -10, 12, false, 0, 0.5),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.RIGHT_ARM, 0, 0, 0, 20, 0, 10, 12, false, 0, 0.5),
						this.walkAnimation.new AnimationCheckpoint(BodyPart.LEFT_ARM, 0, 0, 0, -20, 0, -10, 12, false, 0, 0.5)
				)
			);
		
		this.grabAnimation = new AnimationHandler(true, false, true);
		this.grabAnimation.setAnimations(this.grabAnimation.new Animation(
					this.grabAnimation.new AnimationCheckpoint(BodyPart.RIGHT_ARM, 260, 36, 0, 245, -20, 15, 14, true, 0.5, 0.5),
					this.grabAnimation.new AnimationCheckpoint(BodyPart.LEFT_ARM, 260, 325, 0, 245, 380, -5, 14, true, 1.0, 0.5),
					this.grabAnimation.new AnimationCheckpoint(BodyPart.HEAD, 0, 0, -20, 0, 0, 20, 14, true, 0.5, 0.5)
				)
			);
		this.grabAnimation.go();
		
		this.riseAnimation = new AnimationHandler(true, true, false);
		this.riseAnimation.setAnimations(this.riseAnimation.new Animation(
					this.riseAnimation.new AnimationCheckpoint(BodyPart.RIGHT_ARM, 187, 44, 0, 260, 36, 0, 40, false, 0.5, 0.5),
					this.riseAnimation.new AnimationCheckpoint(BodyPart.LEFT_ARM, 199, 316, 0, 260, 325, 0, 40, false, 0.5, 0.5),
					this.riseAnimation.new AnimationCheckpoint(BodyPart.HEAD, 319, 0, 20, 0, 0, -20, 40, false, 0.5, 0.5)
				)
			);
		if (spawnAnimation) {
			this.riseAnimation.startAnimation(stand);
			this.riseAnimation.tick(stand);
			int[] timer = {40};
			toGround = block.getRelative(BlockFace.DOWN).getLocation().add(.5,.1,.5);
			double diff = 0.8 / 40.0;
			new RepeatingTask(plugin, 0, 1) {
				@Override
				public void run() {
					if (entity == null) {
						cancel();
						return;
					}
					entity.teleport(toGround);
					toGround.add(0,diff,0);
					if (timer[0] % 5 == 0 && plugin.mcVersion >= 1.20)
						entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_SCULK_BREAK, SoundCategory.HOSTILE, 2f, .5f);
					if (--timer[0] <= 0)
						cancel();
				}
			};
		}
	}
	@Override
	public void tick() {
		if (entity == null || stand == null)
			return;
		if (entity.getNoDamageTicks() == 20)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_HURT, SoundCategory.HOSTILE, 1f, .5f);
		if (step.distanceSquared(entity.getLocation()) > 3 && entity.isOnGround()) {
			step = entity.getLocation();
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_STEP, SoundCategory.HOSTILE, 2f, .5f);
		}
		super.tick();
		if (walking) {
			entity.getWorld().spawnParticle(VersionUtils.getBlockDust(), entity.getLocation().clone().add(0,1.1,0), 1, .2, .4, .2, 1, Material.PODZOL.createBlockData());
			return;
		}
		entity.teleport(toGround.setDirection(entity.getEyeLocation().getDirection()));
		stand.teleport(entity.getLocation().subtract(0,.1,0));
		if (!riseAnimation.isFinished()) {
			riseAnimation.tick(stand);
			entity.getWorld().spawnParticle(VersionUtils.getBlockCrack(), ground.getLocation().add(.5, 1.1, .5), 5, .4, .1, .4, 1, ground.getBlockData());
			return;
		}
		if (soundTick++ == 10) {
			soundTick = 0;
			if (plugin.mcVersion >= 1.20)
				entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_SCULK_BREAK, SoundCategory.HOSTILE, 0.1f, .5f);
		}
		grabAnimation.tick(stand);
		if (Utils.isTargetInRange(entity, 0, 4, true))
			entity.getTarget().setVelocity(entity.getTarget().getVelocity().multiply(0.25).add(Utils.getVectorTowards(entity.getTarget().getLocation(), entity.getEyeLocation().subtract(0,.3,0)).multiply(0.05*(entity.getTarget().getLocation().distanceSquared(entity.getEyeLocation())))));
		entity.getWorld().spawnParticle(VersionUtils.getBlockCrack(), ground.getLocation().add(.5, 1.1, .5), 2, .4, .1, .4, 1, ground.getBlockData());
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
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_DEATH, SoundCategory.HOSTILE, 2f, .5f);
			if (entity.getKiller() != null && CustomEntityType.dropsEnabled && plugin.random.nextDouble() * 100 < 15.0)
				entity.getWorld().dropItemNaturally(entity.getLocation(), ItemsHandler.cursedFlesh);
			clean();
			it.remove();
			return;
		}
		if (rand.nextInt(6) == 0)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_AMBIENT, SoundCategory.HOSTILE, 2f, .5f);
		if (!walking && (ground.isPassable() ||
				(entity.getHealth() < entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 100 * 70 && !Utils.isTargetInRange(entity, 0, 2.25, false)))) {
			walking = true;
			entity.setVelocity(new Vector(0,.4,0));
			stand.setRightArmPose(new EulerAngle(0, 0, 0));
			stand.setLeftArmPose(new EulerAngle(0, 0, 0));
		}
	}
	public boolean isWalking() {
		return walking;
	}
	public void setWalking(boolean walking) {
		this.walking = walking;
	}
	@Override
	public void clean() {
		super.clean();
	}
	public void dig() {
		int[] ticks = {0};
		Location[] block = new Location[1];
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				if (entity == null || entity.isDead()) {
					cancel();
					return;
				}
				if (ticks[0] > 0) {
					if (ticks[0]++ == 60) {
						clean();
						cancel();
						entity.remove();
						return;
					}
					entity.teleport(entity.getLocation().subtract(0,0.05,0));
					stand.setHeadPose(new EulerAngle(Math.toRadians(ticks[0]), 0, 0));
					block[0].getWorld().spawnParticle(VersionUtils.getBlockCrack(), block[0], 5, .5, .1, .5, 0.1, block[0].getBlock().getType() == Material.AIR ? Material.DIRT.createBlockData() : block[0].getBlock().getBlockData());
					return;
				}
				entity.setTarget(null);
				if (entity.isOnGround()) {
					entity.setAI(false);
					entity.setGravity(false);
					block[0] = entity.getLocation().getBlock().getLocation().add(.5,0,.5);
					ticks[0]++;
				}
			}
		};
	}
}
