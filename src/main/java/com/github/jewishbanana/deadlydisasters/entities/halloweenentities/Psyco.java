package com.github.jewishbanana.deadlydisasters.entities.halloweenentities;

import java.util.Iterator;
import java.util.Random;
import java.util.stream.Collectors;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.CustomHead;
import com.github.jewishbanana.deadlydisasters.handlers.ItemsHandler;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class Psyco extends CustomEntity {
		
	private Random rand;
	private int cooldown;

	public Psyco(Mob entity, Main plugin, Random rand) {
		super(entity, plugin);
		this.entityType = CustomEntityType.PSYCO;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		this.rand = rand;
		entity.setMetadata("dd-psyco", new FixedMetadataValue(plugin, "protected"));
		entity.setMetadata("dd-halloweenmobs", plugin.fixedData);
		entity.setCanPickupItems(false);
		entity.setSilent(true);
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
		entity.setHealth(entityType.getHealth());
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(entityType.getDamage());
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.3);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40);
		
		entity.getEquipment().setHelmetDropChance(0);
		ItemStack[] armor = {new ItemStack(Material.LEATHER_BOOTS), new ItemStack(Material.LEATHER_LEGGINGS), new ItemStack(Material.LEATHER_CHESTPLATE), CustomHead.STALKER.getHead()};
		for (int i=0; i < 3; i++) {
			LeatherArmorMeta meta = (LeatherArmorMeta) armor[i].getItemMeta();
			meta.setColor(Color.fromRGB(105, 68, 31));
			armor[i].setItemMeta(meta);
		}
		entity.getEquipment().setArmorContents(armor);
		entity.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
		
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.getString("halloween.psyco"));
	}
	@Override
	public void tick() {
		if (entity == null)
			return;
		if (entity.getNoDamageTicks() == 20)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_HUSK_HURT, SoundCategory.HOSTILE, 1f, .5f);
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (entity == null) {
			it.remove();
			return;
		}
		if (entity.isDead()) {
			if (plugin.mcVersion >= 1.16)
				entity.getWorld().spawnParticle(Particle.SOUL, entity.getLocation().add(0,1.5,0), 5, .3, .3, .3, .0001);
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_HUSK_DEATH, SoundCategory.HOSTILE, 1f, .6f);
			if (entity.getKiller() != null && CustomEntityType.dropsEnabled && plugin.random.nextDouble() * 100 < 10.0)
				entity.getWorld().dropItemNaturally(entity.getLocation(), ItemsHandler.candyCorn);
			it.remove();
			return;
		}
		if (rand.nextInt(4) == 0 && Tag.WOODEN_DOORS.getValues().contains(entity.getLocation().add(entity.getLocation().getDirection()).getBlock().getType()) && !Utils.isZoneProtected(entity.getLocation().add(entity.getLocation().getDirection()))) {
			entity.getLocation().add(entity.getLocation().getDirection()).getBlock().breakNaturally();
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 1, 1);
		}
		if (entity.getTarget() == null)
			for (Entity e : entity.getNearbyEntities(30, 30, 30).stream().filter(p -> p instanceof Player && !Utils.isPlayerImmune((Player) p)).sorted((e1, e2) -> (int) e1.getLocation().distanceSquared(e2.getLocation())).collect(Collectors.toList())) {
				entity.setTarget((LivingEntity) e);
				break;
			}
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		if (Utils.isTargetInRange(entity, 25, 1600, false)) {
			float yaw = entity.getTarget().getLocation().getYaw()+180;
			Vector vec = Utils.getVectorTowards(entity.getTarget().getLocation(), entity.getLocation());
			Location check = entity.getTarget().getLocation().setDirection(vec);
			if (yaw > check.getYaw()-45 && yaw < check.getYaw()+45) {
				Location pivot = entity.getTarget().getLocation();
				for (int i=0; i < 5; i ++) {
					check.add(vec);
					Location spawn = Utils.findSmartYSpawn(pivot, check, 2, 5);
					if (spawn == null)
						continue;
					cooldown = 4;
					LivingEntity target = entity.getTarget();
					entity.setTarget(null);
					entity.teleport(spawn);
					plugin.getServer().getScheduler().runTaskLater(plugin, () -> entity.setTarget(target), 1);
					break;
				}
			}
		}
	}
	@Override
	public void clean() {
	}
	@Override
	public void update(FileConfiguration file) {
	}
}

