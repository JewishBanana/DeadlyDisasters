package deadlydisasters.entities.sandstormentities;

import java.util.Iterator;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Skeleton;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.CustomHead;
import deadlydisasters.general.ItemsHandler;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;
import deadlydisasters.utils.Utils;

public class AncientSkeleton extends CustomEntity {
	
	private int cooldown,timer,spellLife;
	private Skeleton[] mobs = new Skeleton[3];
	private LivingEntity lockedTarget;
	private Location spell;
	private Vector motion;
	private BlockData bd = Material.SAND.createBlockData();
	private Random rand;

	public AncientSkeleton(Mob entity, Main plugin, Random rand) {
		super(entity, plugin);
		this.entityType = CustomEntityType.ANCIENTSKELETON;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		this.rand = rand;
		entity.setMetadata("dd-ancientskeleton", new FixedMetadataValue(plugin, "protected"));
		entity.getEquipment().setHelmet(CustomHead.ANCIENTSKELETON.getHead());
		entity.getEquipment().setHelmetDropChance(0);
		entity.setCanPickupItems(false);
		entity.setSilent(true);
		entity.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(30);
		entity.setHealth(30);
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(7);
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.3);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40);
		
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.langFile.getString("entities.ancientSkeleton"));
	}
	@Override
	public void tick() {
		if (entity == null)
			return;
		if (entity.getNoDamageTicks() == 20)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SKELETON_HURT, SoundCategory.HOSTILE, 1f, .5f);
		entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation().clone().add(0,1.3,0), 1, .25, .45, .25, .01);
		if (timer > 0)
			for (Skeleton skel : mobs) {
				skel.teleport(skel.getLocation().clone().add(0,0.035,0));
				skel.getWorld().spawnParticle(Particle.BLOCK_DUST, skel.getLocation(), 5, .5, .5, .5, .01, bd);
			}
		if (spellLife > 0) {
			spellLife--;
			spell.add(motion);
			spell.getWorld().spawnParticle(Particle.FLAME, spell, 10, 1, 1, 1, .05);
			spell.getWorld().spawnParticle(Particle.BLOCK_DUST, spell, 7, 1, 1, 1, .1, bd);
			for (Entity e : spell.getWorld().getNearbyEntities(spell, 1.5, 1.5, 1.5))
				if (e instanceof LivingEntity) {
					e.setFireTicks(80);
					e.setVelocity(motion.clone().multiply(0.5));
				}
		}
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (entity == null) {
			it.remove();
			return;
		}
		if (entity.isDead()) {
			entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation().add(0,1,0), 15, .3, .5, .3, .2);
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SKELETON_DEATH, SoundCategory.HOSTILE, 1f, .5f);
			if (entity.getKiller() != null && plugin.getConfig().getBoolean("customentities.allow_custom_drops")) {
				entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.BONE, plugin.random.nextInt(4)));
				if (plugin.random.nextDouble() < 0.1)
					entity.getWorld().dropItemNaturally(entity.getLocation(), ItemsHandler.ancientbone);
			}
			clean();
			it.remove();
			return;
		}
		if (rand.nextInt(8) == 0)
			if (rand.nextInt(2) == 0)
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SKELETON_AMBIENT, SoundCategory.HOSTILE, 1, .5f);
			else
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SKELETON_STEP, SoundCategory.HOSTILE, 1, .5f);
		if (timer > 0) {
			timer--;
			if (timer == 2)
				for (Skeleton skel : mobs)
					skel.setInvulnerable(false);
			else
				if (timer == 0) {
					for (Skeleton skel : mobs) {
						skel.setAI(true);
						skel.setGravity(true);
						if (lockedTarget != null)
							skel.setTarget(lockedTarget);
					}
					entity.setAI(true);
					if (lockedTarget != null)
						entity.setTarget(lockedTarget);
				}
		}
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		if (entity.getTarget() == null || entity.getLocation().distanceSquared(entity.getTarget().getLocation()) > 49)
			return;
		if (entity.isOnGround() && (mobs[0] == null || (mobs[0].isDead() && mobs[1].isDead() && mobs[2].isDead()))) {
			timer = 3;
			cooldown = 17;
			lockedTarget = entity.getTarget();
			entity.setAI(false);
			entity.setVelocity(new Vector(0,0,0));
			Location target = entity.getLocation().clone().add(entity.getLocation().getDirection().multiply(2).setY(entity.getLocation().getBlockY()));
			Vector direction = entity.getLocation().getDirection().setY(0);
			mobs[0] = attemptSpawn(target);
			mobs[1] = attemptSpawn(entity.getLocation().clone().add(direction.clone().setX(-direction.getZ()).setZ(direction.getX()).multiply(1.5)));
			mobs[2] = attemptSpawn(entity.getLocation().clone().add(direction.clone().setX(direction.getZ()).setZ(-direction.getX()).multiply(1.5)));
			if (plugin.mcVersion >= 1.17)
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.HOSTILE, 1f, .5f);
		} else {
			cooldown = 10;
			spellLife = 20;
			motion = entity.getLocation().getDirection().clone().multiply(0.7);
			spell = entity.getEyeLocation().clone().add(motion.clone().multiply(4));
			if (plugin.mcVersion >= 1.16)
				entity.swingMainHand();
			entity.getWorld().playSound(spell, Sound.ITEM_FIRECHARGE_USE, SoundCategory.HOSTILE, 1f, .6f);
		}
	}
	@Override
	public void clean() {
		for (Skeleton skel : mobs)
			if (skel != null)
				skel.remove();
	}
	@Override
	public void update(FileConfiguration file) {
		clean();
	}
	private Skeleton spawnSkeleton(Location location) {
		Skeleton skel = (Skeleton) location.getWorld().spawnEntity(location, EntityType.SKELETON);
		skel.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
		skel.setAI(false);
		skel.setInvulnerable(true);
		skel.setGravity(false);
		skel.setMetadata("dd-unburnable", new FixedMetadataValue(plugin, "protected"));
		skel.setMetadata("dd-ancientminion", new FixedMetadataValue(plugin, "protected"));
		Location temp = entity.getLocation();
		skel.setRotation(temp.getYaw(), temp.getPitch());
		return skel;
	}
	private Skeleton attemptSpawn(Location location) {
		Block b = location.getBlock();
		if (b.isPassable())
			b = Utils.getBlockBelow(location);
		else
			b = Utils.getBlockAbove(location);
		return spawnSkeleton(b.getRelative(BlockFace.DOWN).getLocation());
	}
}
