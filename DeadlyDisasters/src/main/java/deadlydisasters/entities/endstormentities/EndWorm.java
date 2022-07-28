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
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.EntityHandler;
import deadlydisasters.general.Main;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public class EndWorm extends CustomEntity {
	
	private double health = 30;
	private int cooldown = 0, frame = 0;
	private ArmorStand[] stands = new ArmorStand[10];
	private EvokerFangs fangs;
	private Random rand;
	private boolean anim;
	private Enderman damageEntity;

	@SuppressWarnings("deprecation")
	public EndWorm(Mob entity, Main plugin, Random rand) {
		super(entity, plugin);
		this.entityType = CustomEntityType.ENDWORM;
		this.rand = rand;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		if (plugin.mcVersion >= 1.16)
			((Zombie) entity).setBaby();
		else
			((Zombie) entity).setBaby(true);
		entity.setCanPickupItems(false);
		if (plugin.mcVersion >= 1.16)
			entity.getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK).setBaseValue(0);
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(0);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40);
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.2);
		entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true));
		entity.setSilent(true);
		entity.setInvulnerable(true);
		entity.setCollidable(false);
		new RepeatingTask(plugin, 0, 5) {
			@Override
			public void run() {
				if (entity.isDead())
					cancel();
				entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_CHORUS_FLOWER_DEATH, SoundCategory.HOSTILE, 0.15f, .5f);
			}
		};
		entity.setMetadata("dd-unburnable", new FixedMetadataValue(plugin, "protected"));
		entity.setMetadata("dd-endworm", new FixedMetadataValue(plugin, "protected"));
	}
	@Override
	public void tick() {
		if (entity == null)
			return;
		World world = entity.getWorld();
		if (anim && cooldown == 0) {
			Vector vec = new Vector(0,.32,0);
			BlockData bd = entity.getLocation().clone().subtract(0,1,0).getBlock().getBlockData();
			for (ArmorStand e : stands)
				if (e != null) {
					e.teleport(e.getLocation().add(vec));
					world.spawnParticle(Particle.BLOCK_DUST, e.getLocation().clone().add(0,1.5,0), 5, .2, .2, .2, 0.1, bd);
				}
			if (frame % 2 == 0)
				stands[(int) (frame / 2)] = createStand(entity.getLocation().clone().subtract((rand.nextDouble()-0.5)/4, 2, (rand.nextDouble()-0.5)/4), rand.nextInt(90));
			frame++;
			fangs.remove();
			fangs = (EvokerFangs) world.spawnEntity(stands[0].getLocation().clone().add(0,2,0), EntityType.EVOKER_FANGS);
			fangs.setSilent(true);
			if (stands[9] != null) {
				anim = false;
				cooldown = 8;
				fangs.setMetadata("dd-endworm", new FixedMetadataValue(plugin, "protected"));
				fangs.setSilent(false);
			} else if (damageEntity == null && stands[4] != null) {
				damageEntity = (Enderman) world.spawnEntity(entity.getLocation(), EntityType.ENDERMAN);
				damageEntity.setAI(false);
				damageEntity.setSilent(true);
				damageEntity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true));
				damageEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
				damageEntity.setHealth(health);
				damageEntity.setMetadata("dd-customentity", new FixedMetadataValue(plugin, "protected"));
			}
		} else if (cooldown <= 7 && stands[0] != null) {
			Vector vec = new Vector(0,-.4,0);
			BlockData bd = entity.getLocation().clone().subtract(0,1,0).getBlock().getBlockData();
			for (ArmorStand e : stands)
				if (e != null) {
					e.teleport(e.getLocation().add(vec));
					world.spawnParticle(Particle.BLOCK_DUST, e.getLocation().clone().add(0,1,0), 3, .2, .2, .2, 0.1, bd);
				}
			fangs.remove();
			fangs = (EvokerFangs) world.spawnEntity(stands[0].getLocation().clone().add(0,3.5,0), EntityType.EVOKER_FANGS);
			fangs.setSilent(true);
			frame--;
			if (frame % 2 == 0) {
				stands[(int) (frame / 2)].remove();
				stands[(int) (frame / 2)] = null;
				if (frame == 0) {
					fangs.setMetadata("dd-endworm", new FixedMetadataValue(plugin, "protected"));
					fangs.setSilent(false);
					fangs.setVelocity(new Vector(0,-.1,0));
				}
			}
			if (damageEntity != null && stands[4] == null) {
				if (damageEntity.isDead()) {
					if (plugin.mcVersion >= 1.16) {
						world.spawnParticle(Particle.SOUL, entity.getLocation().add(0,2,0), 15, .3, 1, .3, .03);
						world.playSound(entity.getLocation(), Sound.ENTITY_PIGLIN_BRUTE_DEATH, SoundCategory.HOSTILE, 1, .5f);
					}
					if (damageEntity.getKiller() != null && plugin.getConfig().getBoolean("customentities.allow_custom_drops")) {
						world.dropItemNaturally(entity.getLocation(), new ItemStack(Material.CHORUS_FRUIT));
						if (rand.nextInt(2) == 0) world.dropItemNaturally(entity.getLocation(), new ItemStack(Material.CRYING_OBSIDIAN));
						if (rand.nextInt(5) == 0) world.dropItemNaturally(entity.getLocation(), new ItemStack(Material.DIAMOND));
					}
					clean();
					entity.remove();
				}
				health = damageEntity.getHealth();
				damageEntity.remove();
				damageEntity = null;
			}
		}
		if (fangs != null && fangs.isValid()) {
			Location loc = fangs.getLocation().clone();
			if (cooldown <= 7)
				loc.subtract(0,2.2,0);
			for (Entity e : fangs.getNearbyEntities(0.7, 1.5, 0.7))
				if (e instanceof LivingEntity && !(e instanceof ArmorStand) && !(e instanceof Player && Utils.isPlayerImmune((Player) e)))
					e.setVelocity(new Vector(e.getLocation().getX() - loc.getX(), e.getLocation().getY() - loc.getY(), e.getLocation().getZ() - loc.getZ()).normalize().multiply(-1).multiply(0.3));
		}
		if (damageEntity != null && damageEntity.getNoDamageTicks() == 20) {
			world.spawnParticle(Particle.DAMAGE_INDICATOR, entity.getLocation().add(0,2,0), 20, .3, 2, .3, .25);
			if (plugin.mcVersion >= 1.16)
				world.playSound(entity.getLocation(), Sound.ENTITY_PIGLIN_BRUTE_HURT, SoundCategory.HOSTILE, 1, .5f);
		}
		if (entity.getLocation().clone().subtract(0,1,0).getBlock().getType().isBlock())
			if (!anim)
				world.spawnParticle(Particle.BLOCK_CRACK, entity.getLocation(), 5, .2, .2, .2, 0.1, entity.getLocation().clone().subtract(0,1,0).getBlock().getBlockData());
			else
				world.spawnParticle(Particle.BLOCK_CRACK, entity.getLocation(), 40, .4, .3, .4, 0.1, entity.getLocation().clone().subtract(0,1,0).getBlock().getBlockData());
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
		for (int i=0; i < stands.length; i++)
			if (stands[i] != null)
				stands[i] = (ArmorStand) plugin.getServer().getEntity(stands[i].getUniqueId());
		if (entity.isDead()) {
			clean();
			it.remove();
			return;
		}
		if (cooldown > 0) {
			cooldown--;
			if (cooldown == 2)
				entity.setAI(true);
		}
	}
	@Override
	public void clean() {
		for (ArmorStand stand : stands)
			if (stand != null) stand.remove();
		if (fangs != null)
			fangs.remove();
		if (damageEntity != null)
			damageEntity.remove();
	}
	@Override
	public void update(FileConfiguration file) {
	}
	public void triggerAnimation() {
		if (cooldown == 0 && entity.isOnGround() && entity.hasAI()) {
			entity.setAI(false);
			plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
				@Override
				public void run() {
					anim = true;
					fangs = (EvokerFangs) entity.getWorld().spawnEntity(entity.getLocation().clone().subtract(0,0.5,0), EntityType.EVOKER_FANGS);
					fangs.setSilent(true);
					entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_GRAVEL_BREAK, SoundCategory.HOSTILE, 1f, .5f);
					if (plugin.mcVersion >= 1.16) {
						entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_NETHER_SPROUTS_BREAK, SoundCategory.HOSTILE, 1f, .5f);
						entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_HOGLIN_DEATH, SoundCategory.HOSTILE, 1f, .5f);
					}
				}
			}, 5);
		}
	}
	private ArmorStand createStand(Location loc, float rotation) {
		loc.setYaw(rotation);
		ArmorStand e = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
		if (plugin.mcVersion >= 1.16)
			e.setInvisible(true);
		else
			e.setVisible(false);
		e.setGravity(false);
		e.setMarker(true);
		if (plugin.mcVersion >= 1.16) {
			e.addEquipmentLock(EquipmentSlot.CHEST, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			e.addEquipmentLock(EquipmentSlot.FEET, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			e.addEquipmentLock(EquipmentSlot.HEAD, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			e.addEquipmentLock(EquipmentSlot.LEGS, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
		}
		if (plugin.mcVersion >= 1.16)
			e.getEquipment().setHelmet(new ItemStack(Material.CRYING_OBSIDIAN));
		else
			e.getEquipment().setHelmet(new ItemStack(Material.OBSIDIAN));
		e.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		return e;
	}
}
