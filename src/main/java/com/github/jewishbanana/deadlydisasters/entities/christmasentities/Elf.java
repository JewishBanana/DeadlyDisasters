package com.github.jewishbanana.deadlydisasters.entities.christmasentities;

import java.util.Iterator;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.CustomHead;
import com.github.jewishbanana.deadlydisasters.handlers.ItemsHandler;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class Elf extends CustomEntity {
	
	private boolean archer,noStack;
	private int cooldown = 2, maxStack = 0;
	private Random rand;

	@SuppressWarnings("deprecation")
	public Elf(Zombie entity, Main plugin, Random rand) {
		super(entity, plugin);
		this.rand = rand;
		this.entityType = CustomEntityType.CHRISTMASELF;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		Utils.clearEntityOfItems(entity);
		ItemStack head = null;
		if (rand.nextInt(2) != 0)
			head = CustomHead.CHRISTMASELF1.getHead();
		else
			head = CustomHead.CHRISTMASELF2.getHead();
		ItemStack[] armor = {new ItemStack(Material.LEATHER_BOOTS), new ItemStack(Material.LEATHER_LEGGINGS), new ItemStack(Material.LEATHER_CHESTPLATE), head};
		int red = rand.nextInt(10)+30, green = rand.nextInt(100)+80;
		for (int i=0; i < 3; i++) {
			LeatherArmorMeta meta = (LeatherArmorMeta) armor[i].getItemMeta();
			meta.setColor(Color.fromBGR(0, green, red));
			armor[i].setItemMeta(meta);
		}
		entity.getEquipment().setArmorContents(armor);
		entity.getEquipment().setHelmetDropChance(0);
		entity.getEquipment().setChestplateDropChance(0);
		entity.getEquipment().setLeggingsDropChance(0);
		entity.getEquipment().setBootsDropChance(0);
		entity.setCanPickupItems(false);
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(12.0);
		entity.setHealth(12.0);
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(4.0);
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.25);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(25);
		if (plugin.mcVersion >= 1.16)
			entity.setBaby();
		else
			entity.setBaby(true);
		entity.setSilent(true);
		entity.setRemoveWhenFarAway(true);
		
		entity.setMetadata("dd-elf", plugin.fixedData);
		entity.setMetadata("dd-christmasmob", plugin.fixedData);
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.langFile.getString("christmas.christmasElf"));
		
		if (rand.nextDouble()*100 < 35.0) {
			entity.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
			archer = true;
			entity.setMetadata("dd-elfarcher", plugin.fixedData);
			if (rand.nextInt(4) == 0)
				noStack = true;
			else if (rand.nextInt(3) == 0)
				maxStack = 1;
		} else {
			int r = rand.nextInt(3);
			if (r == 1)
				entity.getEquipment().setItemInMainHand(new ItemStack(Material.WOODEN_SWORD));
			else if (r == 2)
				entity.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_SWORD));
		}
	}
	@Override
	public void tick() {
		if (entity == null)
			return;
		if (rand.nextInt(4) == 0) {
			entity.getWorld().spawnParticle(Particle.FALLING_DUST, entity.getLocation().add(0,.5,0), 1, .15, .3, .15, 1, Material.LIME_WOOL.createBlockData());
			entity.getWorld().spawnParticle(Particle.FALLING_DUST, entity.getLocation().add(0,.5,0), 1, .15, .3, .15, 1, Material.RED_WOOL.createBlockData());
		}
		if (entity.getNoDamageTicks() == 20)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_VILLAGER_HURT, SoundCategory.HOSTILE, 1, 2);
		if (archer && cooldown <= 0 && entity.getTarget() != null && entity.getTarget().getLocation().distanceSquared(entity.getLocation()) <= 81 && entity.hasLineOfSight(entity.getTarget())) {
			cooldown = 3;
			entity.addPotionEffect(new PotionEffect(VersionUtils.getSlowness(), 20, 10, true, false));
			Arrow arrow = (Arrow) entity.getWorld().spawnEntity(entity.getLocation().add(entity.getLocation().getDirection().multiply(0.1).setY(0.7)), EntityType.ARROW);
			arrow.addCustomEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1), false);
			arrow.setShooter(entity);
			Location target = entity.getTarget().getLocation().add(0,entity.getTarget().getHeight()/2,0);
			Vector vec = Utils.getVectorTowards(arrow.getLocation(), target);
			vec.setX(vec.getX()/3);
			vec.setZ(vec.getZ()/3);
			vec.setY((target.distance(entity.getLocation())/13)+((target.getY()-entity.getLocation().getY())/30));
			arrow.setVelocity(vec);
			arrow.setMetadata("dd-elfarrow", plugin.fixedData);
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
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_VILLAGER_DEATH, SoundCategory.HOSTILE, 1, 2);
			if (entity.getKiller() != null) {
				if (rand.nextInt(3) == 0)
					entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.BONE, rand.nextInt(2)+1));
				if (rand.nextInt(3) == 0)
					entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.ROTTEN_FLESH, rand.nextInt(2)+1));
				if (rand.nextInt(100) < 5)
					entity.getWorld().dropItemNaturally(entity.getLocation(), ItemsHandler.ornament);
			}
			it.remove();
			return;
		}
		if (cooldown > 0)
			cooldown--;
		if (archer) {
			if (!noStack && !entity.isInsideVehicle())
				for (Entity e : entity.getNearbyEntities(1, 1, 1))
					if (!e.isInsideVehicle() && e.getPassengers().size() <= maxStack && e.hasMetadata("dd-elf") && !e.hasMetadata("dd-elfarcher"))
						if (e.getPassengers().isEmpty())
							e.addPassenger(entity);
						else if (e.getPassengers().get(0).isEmpty())
							e.getPassengers().get(0).addPassenger(entity);
			if (entity.getTarget() != null && entity.getTarget().getLocation().distanceSquared(entity.getLocation()) <= 81 && entity.hasLineOfSight(entity.getTarget()))
				entity.addPotionEffect(new PotionEffect(VersionUtils.getSlowness(), 30, 10, true, false));
		}
		if (rand.nextInt(6) == 0)
			if (rand.nextInt(4) == 0)
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_VILLAGER_YES, SoundCategory.HOSTILE, 1, 2);
			else
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, SoundCategory.HOSTILE, 1, 2);
	}
	@Override
	public void clean() {
	}
	@Override
	public void update(FileConfiguration file) {
	}
}
