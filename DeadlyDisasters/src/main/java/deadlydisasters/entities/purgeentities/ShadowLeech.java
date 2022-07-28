package deadlydisasters.entities.purgeentities;

import java.util.Iterator;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Team.Option;
import org.bukkit.scoreboard.Team.OptionStatus;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.CustomHead;
import deadlydisasters.entities.EntityHandler;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;
import deadlydisasters.utils.Utils;

public class ShadowLeech extends CustomEntity {
	
	private ArmorStand stand;
	private int cooldown = 3;
	private Random rand;
	private boolean attached,bloodleech;
	private LivingEntity attachedTo;
	private Vector offsetOfAttach,facingDirection;
	private double damage;
	private Location step;
	private Team team;

	public ShadowLeech(Zombie entity, Main plugin, Random rand) {
		super(entity, plugin);
		this.rand = rand;
		this.entityType = CustomEntityType.SHADOWLEECH;
		this.species = entityType.species;
		
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
		
		if (rand.nextDouble()*100 < 12.5) {
			bloodleech = true;
			damage = 3;
			stand.getEquipment().setHelmet(CustomHead.BLOODYLEECH.getHead());
		} else {
			damage = 2;
			stand.getEquipment().setHelmet(CustomHead.SHADOWLEECH.getHead());
		}
		initZombie();
		step = entity.getLocation();
	}
	@Override
	public void tick() {
		if (entity == null || stand == null)
			return;
		if (entity.getNoDamageTicks() == 20)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_FOX_SCREECH, SoundCategory.HOSTILE, 1f, 1.8f);
		if (entity.isDead()) {
			entity.remove();
			entity.getWorld().spawnParticle(Particle.SMOKE_NORMAL, entity.getLocation().add(0,.25,0), 30, .15, .15, .15, .000001);
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_VEX_DEATH, SoundCategory.HOSTILE, 0.25f, .7f);
			clean();
			return;
		}
		if (bloodleech)
			entity.getWorld().spawnParticle(Particle.BLOCK_CRACK, entity.getLocation(), 1, 0, 0, 0, 1, Material.REDSTONE_BLOCK.createBlockData());
		if (attached) {
			if (attachedTo == null || attachedTo.isDead()) {
				attachedTo = null;
				attached = false;
				entity.setAI(true);
				entity.setGravity(true);
				entity.setTarget(null);
				removeTeam();
				return;
			}
			entity.teleport(attachedTo.getLocation().add(offsetOfAttach).setDirection(facingDirection));
			entity.setVelocity(attachedTo.getVelocity());
		} else if (entity.getTarget() != null) {
			if (cooldown <= 0 && entity.getTarget().getLocation().distanceSquared(entity.getLocation()) <= 6.25) {
				cooldown = 3;
				entity.setVelocity(Utils.getVectorTowards(entity.getLocation(), entity.getTarget().getLocation().add(0,rand.nextDouble()+1,0)));
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ITEM_FRAME_BREAK, SoundCategory.HOSTILE, 1f, .5f);
			}
			Location loc = entity.getTarget().getLocation();
			loc.setY(entity.getLocation().getY());
			if (loc.distanceSquared(entity.getLocation()) < 0.3) {
				attachedTo = entity.getTarget();
				offsetOfAttach = entity.getLocation().toVector().setY(0).subtract(attachedTo.getLocation().toVector().setY(0)).normalize().multiply((entity.getLocation().getY()-attachedTo.getLocation().getY())*0.25).setY(Math.min(entity.getLocation().toVector().subtract(attachedTo.getLocation().toVector()).getY(), attachedTo.getHeight()));
				try {
					offsetOfAttach.checkFinite();
				} catch (IllegalArgumentException e) {
					return;
				}
				attached = true;
				entity.setAI(false);
				entity.setGravity(false);
				Utils.makeEntityFaceLocation(entity, attachedTo.getLocation().add(0,1.5,0));
				facingDirection = entity.getLocation().getDirection();
				Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
				team = sb.registerNewTeam("col-"+entity.getUniqueId());
				team.setOption(Option.COLLISION_RULE, OptionStatus.NEVER);
				char quo = '"';
				Location tempLoc = entity.getLocation();
				Utils.runConsoleCommand("team join col-"+entity.getUniqueId()+" @e[x="+tempLoc.getX()+",y="+tempLoc.getY()+",z="+tempLoc.getZ()+",distance=..0.2,limit=1,name="+quo+entity.getName()+quo+"]", entity.getWorld());
			}
		}
		if (!bloodleech && entity.getNoDamageTicks() == 0 && entity.getLocation().getBlock().getLightFromBlocks() >= (byte) 11) {
			if (attached) {
				attachedTo = null;
				attached = false;
				entity.setAI(true);
				entity.setGravity(true);
				removeTeam();
			}
			if (entity.getTarget() != null)
				entity.setTarget(null);
			Block position = entity.getLocation().getBlock();
			Block towards = entity.getLocation().add(entity.getVelocity().normalize().multiply(-1)).getBlock();
			if (position.getRelative(BlockFace.NORTH).getLightFromBlocks() < towards.getLightFromBlocks())
				towards = position.getRelative(BlockFace.NORTH);
			if (position.getRelative(BlockFace.EAST).getLightFromBlocks() < towards.getLightFromBlocks())
				towards = position.getRelative(BlockFace.EAST);
			if (position.getRelative(BlockFace.SOUTH).getLightFromBlocks() < towards.getLightFromBlocks())
				towards = position.getRelative(BlockFace.SOUTH);
			if (position.getRelative(BlockFace.WEST).getLightFromBlocks() < towards.getLightFromBlocks())
				towards = position.getRelative(BlockFace.WEST);
			Utils.makeEntityFaceLocation(entity, towards.getLocation().add(.5,1,.5));
			entity.setVelocity(Utils.getVectorTowards(entity.getLocation(), towards.getLocation().add(.5,0,.5)).multiply(0.75).setY(0.3));
			entity.damage(2);
			entity.getWorld().spawnParticle(Particle.CLOUD, entity.getLocation().add(0,.4,0), 4, .15, .15, .15, 0.000001);
			entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.HOSTILE, 0.5f, 2f);
		}
		stand.teleport(entity.getLocation().subtract(0,.75,0));
		stand.setHeadPose(new EulerAngle(Math.toRadians(entity.getLocation().getPitch()),0,0));
		if (step.distanceSquared(entity.getLocation()) > 0.8 && entity.isOnGround()) {
			step = entity.getLocation();
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMITE_STEP, SoundCategory.HOSTILE, 0.15f, .8f);
		}
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (entity == null || stand == null) {
			clean();
			it.remove();
			return;
		}
		stand = (ArmorStand) plugin.getServer().getEntity(stand.getUniqueId());
		if (attached) {
			if (attachedTo instanceof Player && Utils.isPlayerImmune((Player) attachedTo)) {
				attachedTo = null;
				entity.setTarget(null);
				removeTeam();
				return;
			}
			if (!bloodleech)
				entity.getWorld().spawnParticle(Particle.BLOCK_CRACK, entity.getLocation(), 3, .1, .1, .1, 1, Material.REDSTONE_BLOCK.createBlockData());
			else
				entity.getWorld().spawnParticle(Particle.BLOCK_CRACK, entity.getLocation(), 8, .1, .1, .1, 1, Material.REDSTONE_BLOCK.createBlockData());
			attachedTo.damage(0.0001);
			if (attachedTo.getHealth()-damage <= 0) {
				if (!bloodleech)
					attachedTo.setMetadata("dd-leechdeath", plugin.fixedData);
				else
					attachedTo.setMetadata("dd-bloodleechdeath", plugin.fixedData);
			}
			attachedTo.setHealth(Math.max(attachedTo.getHealth()-damage, 0));
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMITE_DEATH, SoundCategory.HOSTILE, 0.5f, 0.8f);
		} else {
			if (rand.nextInt(8) == 0)
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMITE_AMBIENT, SoundCategory.HOSTILE, 1, .5f);
			if (cooldown > 0)
				cooldown--;
		}
	}
	@Override
	public void clean() {
		if (stand != null)
			stand.remove();
		removeTeam();
	}
	@Override
	public void update(FileConfiguration file) {
	}
	@SuppressWarnings("deprecation")
	private void initZombie() {
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		entity.setMetadata("dd-shadowleech", new FixedMetadataValue(plugin, "protected"));
		entity.setCanPickupItems(false);
		entity.setSilent(true);
		if (plugin.mcVersion >= 1.16)
			((Zombie) entity).setBaby();
		else
			((Zombie) entity).setBaby(true);
		entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true, false));
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(0);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(30);
		Utils.clearEntityOfItems(entity);
		if (!bloodleech) {
			entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(8);
			entity.setHealth(8);
			entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.15);
			entity.setCustomName(Languages.langFile.getString("entities.shadowLeech"));
		} else {
			entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(16);
			entity.setHealth(16);
			entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.3);
			entity.setCustomName(Languages.langFile.getString("entities.bloodyLeech"));
		}
	}
	private void removeTeam() {
		if (team != null)
			team.unregister();
		team = null;
	}
}
