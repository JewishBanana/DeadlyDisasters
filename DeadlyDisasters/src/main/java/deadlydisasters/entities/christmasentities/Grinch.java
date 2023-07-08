package deadlydisasters.entities.christmasentities;

import java.util.Iterator;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.CustomHead;
import deadlydisasters.entities.EntityHandler;
import deadlydisasters.general.ItemsHandler;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public class Grinch extends CustomEntity {
	
	private int cooldown = 4;
	private ArmorStand stand;
	private ItemStack weapon;
	private Slime projectile;
	private Random rand;
	private boolean cursed;
	
	public Grinch(Mob entity, Main plugin, Random rand) {
		super(entity, plugin);
		this.rand = rand;
		this.entityType = CustomEntityType.GRINCH;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		ItemStack[] armor = {new ItemStack(Material.LEATHER_BOOTS), new ItemStack(Material.LEATHER_LEGGINGS), new ItemStack(Material.LEATHER_CHESTPLATE), CustomHead.GRINCH.getHead()};
		for (int i=0; i < 3; i++) {
			LeatherArmorMeta meta = (LeatherArmorMeta) armor[i].getItemMeta();
			meta.setColor(Color.fromBGR(28, 94, 4));
			armor[i].setItemMeta(meta);
		}
		entity.getEquipment().setArmorContents(armor);
		entity.getEquipment().setHelmetDropChance(0);
		entity.getEquipment().setChestplateDropChance(0);
		entity.getEquipment().setLeggingsDropChance(0);
		entity.getEquipment().setBootsDropChance(0);
		if (rand.nextInt(10) == 0) {
			weapon = ItemsHandler.cursedCandyCane;
			cursed = true;
		} else
			weapon = ItemsHandler.candyCane;
		entity.getEquipment().setItemInMainHand(weapon);
		entity.getEquipment().setItemInMainHandDropChance(0);
		entity.setCanPickupItems(false);
		entity.setSilent(true);
		entity.setRemoveWhenFarAway(true);
		
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(40.0);
		entity.setHealth(40.0);
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(12.0);
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.35);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(30);
		
		entity.setMetadata("dd-grinch", plugin.fixedData);
		entity.setMetadata("dd-christmasmob", plugin.fixedData);
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.langFile.getString("entities.grinch"));
	}

	@Override
	public void tick() {
		if (entity == null)
			return;
		if (entity.getNoDamageTicks() == 20)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_HURT, SoundCategory.HOSTILE, 1, 0.5f);
		entity.getWorld().spawnParticle(Particle.FALLING_DUST, entity.getLocation().add(0,entity.getHeight()/2,0), 2, .3, .5, .3, 1, Material.LIME_WOOL.createBlockData());
		entity.getWorld().spawnParticle(Particle.FALLING_DUST, entity.getLocation().add(0,entity.getHeight()/2,0), 2, .3, .5, .3, 1, Material.RED_WOOL.createBlockData());
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (entity == null) {
			it.remove();
			clean();
			return;
		}
		if (entity.isDead()) {
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_DEATH, SoundCategory.HOSTILE, 1, 0.5f);
			if (entity.getKiller() != null) {
				if (rand.nextInt(2) == 0)
					entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.BONE, rand.nextInt(2)+1));
				if (rand.nextInt(2) == 0)
					entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.ROTTEN_FLESH, rand.nextInt(2)+1));
				if (rand.nextDouble()*100 < 10.0)
					entity.getWorld().dropItemNaturally(entity.getLocation(), weapon);
			}
			it.remove();
			clean();
			return;
		}
		if (rand.nextInt(6) == 0)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, 1, 0.5f);
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		if (stand == null && entity.getTarget() != null && entity.getTarget().getWorld().equals(entity.getWorld()) && entity.getTarget().getLocation().distanceSquared(entity.getLocation()) >= 30
				&& entity.getTarget().getLocation().distanceSquared(entity.getLocation()) <= 350) {
			cooldown = 4;
			entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 15, 10, true, false));
			stand = Utils.lockArmorStand((ArmorStand) entity.getWorld().spawnEntity(entity.getLocation().add(100,100,0), EntityType.ARMOR_STAND), true, false, true);
			stand.getEquipment().setItemInMainHand(weapon);
			stand.setRightArmPose(new EulerAngle(0, 0, 0));
			stand.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
			projectile = (Slime) entity.getWorld().spawnEntity(entity.getLocation().add(0,100,0), EntityType.SLIME);
			projectile.setSize(0);
			projectile.setSilent(true);
			projectile.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(100.0);
			projectile.setHealth(100.0);
			projectile.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(0.0);
			projectile.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true, false));
			projectile.teleport(entity);
			projectile.teleport(projectile.getLocation().add(0,1,0));
			projectile.setVelocity(Utils.getVectorTowards(entity.getLocation(), entity.getTarget().getLocation()).multiply(1.5).add(new Vector(0,entity.getLocation().distance(entity.getTarget().getLocation())/20,0)));
			entity.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
			entity.swingMainHand();
			new RepeatingTask(plugin, 0, 1) {
				@Override
				public void run() {
					if (stand == null || stand.isDead() || projectile == null || projectile.isDead() || entity == null) {
						clean();
						stand = null;
						entity.getEquipment().setItemInMainHand(weapon);
						cancel();
						return;
					}
					if (cursed && entity.getTarget() != null && entity.getTarget().getWorld().equals(entity.getWorld()))
						projectile.setVelocity(projectile.getVelocity().add(Utils.getVectorTowards(projectile.getLocation(), entity.getTarget().getLocation().add(0,entity.getTarget().getHeight()/2,0)).multiply(0.2)));
					stand.teleport(projectile.getLocation().add(0,-1,0));
					Vector vec = projectile.getVelocity();
					double pivot = Math.abs(vec.getX());
					if (Math.abs(vec.getZ()) > pivot)
						pivot = Math.abs(vec.getZ());
					double angle = Math.toDegrees(Math.atan2(Math.abs(vec.getY()), pivot));
					if (vec.getY() >= 0)
						stand.setRightArmPose(new EulerAngle(Math.toRadians(360-angle), 0, 0));
					else
						stand.setRightArmPose(new EulerAngle(Math.toRadians(360+angle), 0, 0));
					if (cursed)
						stand.getWorld().spawnParticle(Particle.BLOCK_CRACK, stand.getLocation().add(0,1,0), 2, .1, .1, .1, 1, Material.PURPLE_WOOL.createBlockData());
					else
						stand.getWorld().spawnParticle(Particle.BLOCK_CRACK, stand.getLocation().add(0,1,0), 2, .1, .1, .1, 1, Material.RED_WOOL.createBlockData());
					stand.getWorld().spawnParticle(Particle.BLOCK_CRACK, stand.getLocation().add(0,1,0), 2, .1, .1, .1, 1, Material.SNOW.createBlockData());
					if (projectile.isOnGround()) {
						entity.getWorld().spawnParticle(Particle.BLOCK_CRACK, entity.getLocation().add(0,1.2,0), 30, .4, .6, .4, 1, Material.SNOW.createBlockData());
						entity.getWorld().spawnParticle(Particle.BLOCK_DUST, entity.getLocation().add(0,1.2,0), 30, .4, .6, .4, 1, Material.SNOW.createBlockData());
						entity.teleport(projectile);
						stand.remove();
						projectile.remove();
						plugin.getServer().getScheduler().runTaskLater(plugin, () -> entity.getEquipment().setItemInMainHand(weapon), 3);
						stand = null;
						cancel();
						return;
					}
					for (Entity e : projectile.getNearbyEntities(.5, .5, .5))
						if (e instanceof LivingEntity && !e.equals(entity) && !(e instanceof Player && Utils.isPlayerImmune((Player) e))) {
							Utils.pureDamageEntity((LivingEntity) e, 7.0, "dd-candycane", false, entity);
							((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1, true, false));
							if (cursed)
								((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 2, true, false));
							entity.setHealth(Math.min(entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), entity.getHealth()+12.0));
							entity.getWorld().spawnParticle(Particle.BLOCK_CRACK, entity.getLocation().add(0,1.2,0), 30, .4, .6, .4, 1, Material.SNOW.createBlockData());
							entity.getWorld().spawnParticle(Particle.BLOCK_DUST, entity.getLocation().add(0,1.2,0), 30, .4, .6, .4, 1, Material.SNOW.createBlockData());
							entity.teleport(projectile);
							entity.getWorld().spawnParticle(Particle.COMPOSTER, entity.getLocation().add(0,1.2,0), 10, .3, .5, .3, .001);
							stand.remove();
							projectile.remove();
							plugin.getServer().getScheduler().runTaskLater(plugin, () -> entity.getEquipment().setItemInMainHand(weapon), 3);
							stand = null;
							cancel();
							return;
						}
				}
			};
		}
	}
	@Override
	public void clean() {
		if (stand != null)
			stand.remove();
		if (projectile != null)
			projectile.remove();
	}
	@Override
	public void update(FileConfiguration file) {
	}
}
