package com.github.jewishbanana.deadlydisasters.entities.monsoonentities;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.Random;

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
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomDropsFactory;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.CustomHead;
import com.github.jewishbanana.deadlydisasters.events.disasters.Monsoon;
import com.github.jewishbanana.deadlydisasters.handlers.ItemsHandler;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class CursedDiver extends CustomEntity {
	
	private int cooldown;
	private Random rand;
	private BlockData bd = Material.OBSIDIAN.createBlockData();

	public CursedDiver() {
	}
	public CursedDiver(Mob tempEntity, Main plugin, Random rand) {
		super(tempEntity, plugin);
		this.entityType = CustomEntityType.CURSEDDIVER;
		this.rand = rand;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.setMetadata("dd-curseddiver", new FixedMetadataValue(plugin, "protected"));
		entity.getEquipment().setHelmet(CustomHead.DIVER.getHead());
		entity.getEquipment().setHelmetDropChance(0);
		entity.setCanPickupItems(false);
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
		entity.setHealth(entityType.getHealth());
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(entityType.getDamage());
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.3);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40);
		entity.getEquipment().setItemInMainHand(ItemsHandler.poseidonsTrident);
		
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.getString("entities.cursedDiver"));
	}
	@Override
	public void tick() {
		if (entity == null)
			return;
		entity.getWorld().spawnParticle(Particle.FALLING_WATER, entity.getLocation().add(0,1,0), 4, .4, .7, .4, 0.0001);
		entity.getWorld().spawnParticle(Particle.FALLING_DUST, entity.getLocation().clone().add(0,1.4,0), 3, .2, .4, .2, 1, bd);
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
			if (entity.getKiller() != null)
				CustomDropsFactory.generateDrops(entity.getLocation(), entityType);
			it.remove();
			return;
		}
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		if (entity.getTarget() == null || entity.getLocation().distanceSquared(entity.getTarget().getLocation()) > 144 || !entity.hasLineOfSight(entity.getTarget()))
			return;
		cooldown = 5;
		entity.addPotionEffect(new PotionEffect(VersionUtils.getSlowness(), 40, 5, true, false, false));
		castWaveSpell(entity.getLocation(), Utils.getVectorTowards(entity.getLocation(), entity.getTarget().getLocation()), 15, rand);
		if (plugin.mcVersion >= 1.16)
			entity.swingMainHand();
		entity.getWorld().spawnParticle(VersionUtils.getDripWater(), entity.getLocation().add(0,1,0), 100, 0.9, 1, 0.9, 1);
		entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_DROWNED_DEATH_WATER, SoundCategory.HOSTILE, 1, .5f);
	}
	@Override
	public void clean() {
	}
	@Override
	public void update(FileConfiguration file) {
	}
	private void castWaveSpell(Location location, Vector dir, int distance, Random rand) {
		World world = location.getWorld();
		int[] timer = {distance};
		Vector angle = new Vector(dir.getZ(), 0, -dir.getX());
		Location spot = location.clone().add(dir.clone().multiply(2)).add(angle.clone().multiply(-2));
		Block[] water = new Block[15];
		Queue<Block> allWaters = new ArrayDeque<>();
		Queue<Block> puddles = new ArrayDeque<>();
		new RepeatingTask(plugin, 0, 2) {
			@Override
			public void run() {
				for (Block b : water)
					if (b != null && b.getType() == Material.WATER) {
						world.spawnParticle(Particle.FALLING_WATER, b.getLocation().add(.5,.5,.5), 30, .5, .5, .5, 0.0001);
						if (rand.nextInt(4) == 0 && b.getRelative(BlockFace.DOWN).getType().isSolid()) {
							puddles.add(b);
							allWaters.remove(b);
							Levelled data = ((Levelled) b.getBlockData());
							data.setLevel(7);
							b.setBlockData(data);
							Monsoon.globalPuddles.add(b);
						} else
							b.setType(Material.AIR);
					}
				if (timer[0] <= 0) {
					for (Block b : allWaters)
						if (b != null)
							b.setType(Material.AIR);
					if (timer[0] <= -10) {
						cancel();
						new RepeatingTask(plugin, 60, 1) {
							@Override
							public void run() {
								if (puddles.isEmpty())
									cancel();
								Block b = puddles.poll();
								if (b != null && b.getType() == Material.WATER)
									b.setType(Material.AIR);
								Monsoon.globalPuddles.remove(b);
							}
						};
					}
					timer[0]--;
					return;
				}
				timer[0]--;
				for (int cycle=0; cycle < 3; cycle++) {
					Location line = spot.clone().add(dir.clone().multiply(cycle)).add(0,cycle,0);
					for (int i=0; i < 5; i++) {
						Block b = line.clone().add(angle.clone().multiply(i)).getBlock();
						if (b.getType() != Material.AIR || Utils.isZoneProtected(b.getLocation()))
							continue;
						allWaters.add(b);
						world.spawnParticle(Particle.BUBBLE_POP, b.getLocation().add(.5,.5,.5), 10, .5, .5, .5, 0.0001);
						b.setType(Material.WATER);
						water[i+(cycle*5)] = b;
						for (Entity e : world.getNearbyEntities(b.getLocation(), 0.5, 0.5, 0.5)) {
							if (e.equals(entity))
								continue;
							e.setVelocity(dir);
							if (e instanceof LivingEntity && !(e instanceof Player && Utils.isPlayerImmune((Player) e)))
								((LivingEntity) e).damage(4);
						}
					}
				}
				spot.add(dir);
				world.playSound(spot, Sound.WEATHER_RAIN, SoundCategory.HOSTILE, 1, 0.75f);
			}
		};
	}
}
