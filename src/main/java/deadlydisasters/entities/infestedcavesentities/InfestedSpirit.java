package deadlydisasters.entities.infestedcavesentities;

import java.util.Iterator;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.EulerAngle;

import deadlydisasters.entities.CustomDropsFactory;
import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.CustomHead;
import deadlydisasters.entities.EntityHandler;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;

public class InfestedSpirit extends CustomEntity {
	
	private Random rand;
	private ArmorStand stand;
	private DustOptions dust = new DustOptions(Color.fromRGB(9, 74, 72), 1);

	public InfestedSpirit(Mob entity, Main plugin, Random rand) {
		super(entity, plugin);
		this.entityType = CustomEntityType.INFESTEDSPIRIT;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		this.rand = rand;
		entity.setMetadata("dd-infestedspirit", new FixedMetadataValue(plugin, "protected"));
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
		if (rand.nextInt(2) == 0)
			stand.getEquipment().setHelmet(CustomHead.INFESTEDSPIRIT1.getHead());
		else
			stand.getEquipment().setHelmet(CustomHead.INFESTEDSPIRIT2.getHead());
		stand.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
		entity.setHealth(entityType.getHealth());
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(entityType.getDamage());
		entity.setSilent(true);
		entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true, false));
		entity.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
		
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.langFile.getString("entities.infestedSpirit"));
	}
	@Override
	public void tick() {
		if (entity == null || stand == null)
			return;
		stand.teleport(entity.getLocation().subtract(0,.3,0));
		stand.setHeadPose(new EulerAngle(Math.toRadians(entity.getLocation().getPitch()),0,0));
		entity.getWorld().spawnParticle(Particle.REDSTONE, entity.getLocation(), 1, .2, .2, .2, 0.01, dust);
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
		if (entity.isDead()) {
			if (plugin.mcVersion >= 1.16)
				entity.getWorld().spawnParticle(Particle.SOUL, entity.getLocation().add(0,.5,0), 15, .3, .5, .3, .03);
			if (plugin.mcVersion >= 1.19)
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ALLAY_ITEM_GIVEN, SoundCategory.HOSTILE, 1f, .7f);
			if (entity.getKiller() != null)
				CustomDropsFactory.generateDrops(entity.getLocation(), entityType);
			clean();
			it.remove();
			return;
		}
		if (rand.nextInt(8) == 0 && plugin.mcVersion >= 1.19)
			if (rand.nextInt(2) == 0)
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ALLAY_DEATH, SoundCategory.HOSTILE, .8f, .5f);
			else
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.HOSTILE, .8f, .5f);
	}
	@Override
	public void clean() {
		if (stand != null)
			stand.remove();
	}
	@Override
	public void update(FileConfiguration file) {
	}
}
