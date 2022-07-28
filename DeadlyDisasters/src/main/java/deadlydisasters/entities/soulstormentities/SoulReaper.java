package deadlydisasters.entities.soulstormentities;

import java.util.Iterator;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Vex;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.CustomHead;
import deadlydisasters.entities.EntityHandler;
import deadlydisasters.general.ItemsHandler;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;

public class SoulReaper extends CustomEntity {
	
	private Random rand;
	public LivingEntity target;
	private Mob vex;
	private UUID vexUUID;
	
	public SoulReaper(Mob entity, Main plugin, Random rand) {
		super(entity, plugin);
		this.entityType = CustomEntityType.SOULREAPER;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		entity.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		
		this.rand = rand;
		vex = (Mob) entity.getWorld().spawnEntity(entity.getLocation(), EntityType.VEX);
		vexUUID = vex.getUniqueId();
		entity.setMetadata("dd-soulreaper", new FixedMetadataValue(plugin, "protected"));
		entity.getEquipment().setHelmet(CustomHead.REAPER.getHead());
		entity.getEquipment().setItemInMainHand(ItemsHandler.soulRipper);
		entity.getEquipment().setItemInMainHandDropChance(0);
		entity.setAI(false);
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(40);
		entity.setHealth(40);
		vex.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(25);
		vex.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.5);
		vex.setSilent(true);
		vex.setInvulnerable(true);
		vex.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true, false));
		vex.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
		
		if (vex.getCustomName() == null)
			vex.setCustomName(Languages.langFile.getString("entities.soulReaper"));
	}
	@Override
	public void tick() {
		if (entity == null || vex == null)
			return;
		entity.teleport(vex.getLocation().subtract(0,.3,0));
		if (plugin.mcVersion >= 1.16)
			vex.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, vex.getLocation(), 1, .2, .2, .2, 0.04);
		vex.getWorld().spawnParticle(Particle.SQUID_INK, vex.getLocation().clone().add(0,0.5,0), 8, .3, .4, .3, 0.015);
		vex.setTarget(target);
		entity.setRotation(vex.getLocation().getYaw(), (float) Math.toRadians(vex.getLocation().getPitch()));
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Skeleton) plugin.getServer().getEntity(entityUUID);
		vex = (Mob) plugin.getServer().getEntity(vexUUID);
		if (entity == null || vex == null) {
			if (vex != null)
				vex.remove();
			it.remove();
			return;
		}
		if (entity.isDead() || vex.isDead()) {
			if (plugin.mcVersion >= 1.16)
				vex.getWorld().spawnParticle(Particle.SOUL, vex.getLocation().add(0,.5,0), 15, .3, .5, .3, .03);
			vex.getWorld().playSound(vex.getLocation(), Sound.ENTITY_CREEPER_DEATH, SoundCategory.HOSTILE, 1.5f, .5f);
			if (entity.getKiller() != null && plugin.getConfig().getBoolean("customentities.allow_custom_drops"))
				if (plugin.random.nextDouble() < 0.1)
					entity.getWorld().dropItemNaturally(entity.getLocation(), ItemsHandler.soulRipper);
			vex.remove();
			clean();
			it.remove();
			return;
		}
		if (rand.nextInt(8) == 0)
			if (rand.nextInt(2) == 0)
				vex.getWorld().playSound(vex.getLocation(), Sound.ENTITY_VEX_AMBIENT, SoundCategory.HOSTILE, 1f, .5f);
			else
				vex.getWorld().playSound(vex.getLocation(), Sound.ENTITY_VEX_CHARGE, SoundCategory.HOSTILE, 1f, .5f);
		if (vex.getTarget() == null)
			for (Entity e : vex.getNearbyEntities(10, 10, 10))
				if (e instanceof Vex) {
					target = (LivingEntity) e;
					vex.setTarget(target);
					break;
				}
	}
	@Override
	public void clean() {
		if (vex != null)
			vex.remove();
	}
	@Override
	public void update(FileConfiguration file) {
	}
}
