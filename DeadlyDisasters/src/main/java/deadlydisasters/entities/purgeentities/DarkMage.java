package deadlydisasters.entities.purgeentities;

import java.util.Iterator;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.CustomHead;
import deadlydisasters.general.ItemsHandler;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;

public class DarkMage extends CustomEntity {
	
	private int cooldown = 0;
	private double speed;
	private FixedMetadataValue fixdata = new FixedMetadataValue(plugin, "protected");
		
	private ShulkerBullet[] bullets = new ShulkerBullet[13];
	
	public LivingEntity reboundTarget;

	public DarkMage(Mob entity, Main plugin) {
		super(entity, plugin);
		this.entityType = CustomEntityType.DARKMAGE;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		ItemStack[] armor = {new ItemStack(Material.LEATHER_BOOTS), new ItemStack(Material.LEATHER_LEGGINGS), new ItemStack(Material.LEATHER_CHESTPLATE), CustomHead.MAGE.getHead()};
		for (int i=0; i < 3; i++) {
			LeatherArmorMeta meta = (LeatherArmorMeta) armor[i].getItemMeta();
			meta.setColor(Color.fromBGR(40, 40, 40));
			armor[i].setItemMeta(meta);
		}
		entity.getEquipment().setArmorContents(armor);
		entity.getEquipment().setHelmetDropChance(0);
		entity.getEquipment().setItemInMainHand(ItemsHandler.mageWand);
		entity.getEquipment().setItemInMainHandDropChance(0);
		
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.3);
		
		entity.setMetadata("dd-darkmage", fixdata);
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.langFile.getString("entities.darkMage"));
	}
	@Override
	public void tick() {
		if (entity == null)
			return;
		Location loc = entity.getLocation();
		if (entity.getNoDamageTicks() == 20 && reboundTarget != null) {
			Location temp = reboundTarget.getLocation();
			Vector vec = new Vector(temp.getX() - loc.getX(), temp.getY() - loc.getY(), temp.getZ() - loc.getZ()).normalize();
			bullets[12] = (ShulkerBullet) entity.getWorld().spawnEntity(loc.clone().add(0,1,0), EntityType.SHULKER_BULLET);
			bullets[12].setShooter(entity);
			bullets[12].setVelocity(vec);
			bullets[12].setMetadata("dd-magebullet", fixdata);
			reboundTarget = null;
		}
		if (cooldown <= 5)
			return;
		if (cooldown > 6) {
			speed += 0.09;
			double yVel = entity.getLocation().getY()+1;
			for (int i=0; i < 6; i++) {
				Location temp = bullets[i].getLocation();
				temp.setY(yVel+0.3);
				bullets[i].teleport(temp);
				bullets[i].setVelocity(new Vector(temp.getX() - loc.getX(), 0, temp.getZ() - loc.getZ()).rotateAroundY(1.2).normalize().multiply(speed).setY(0.04));
			}
			for (int i=6; i < 12; i++) {
				Location temp = bullets[i].getLocation();
				temp.setY(yVel-0.3);
				bullets[i].teleport(temp);
				bullets[i].setVelocity(new Vector(temp.getX() - loc.getX(), 0, temp.getZ() - loc.getZ()).rotateAroundY(-1.2).normalize().multiply(speed).setY(0.04));
			}
		} else if (entity.getTarget() != null) {
			double yValue = (entity.getTarget().getLocation().getY()+1 - entity.getLocation().getY()+1) / 10;
			for (int i=0; i < 12; i++) {
				Location temp = bullets[i].getLocation();
				bullets[i].setVelocity(new Vector(temp.getX() - loc.getX(), 0, temp.getZ() - loc.getZ()).normalize().setY(yValue));
			}
			entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.3);
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
			if (entity.getKiller() != null && plugin.getConfig().getBoolean("customentities.allow_custom_drops")) {
				entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.ROTTEN_FLESH, plugin.random.nextInt(4)));
				if (plugin.random.nextInt(2) == 0)
					entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.BONE, plugin.random.nextInt(3)));
				if (plugin.random.nextDouble() < 0.03)
					entity.getWorld().dropItemNaturally(entity.getLocation(), ItemsHandler.mageWand);
			}
			it.remove();
			return;
		}
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		if (entity.getTarget() != null && entity.getLocation().distanceSquared(entity.getTarget().getLocation()) <= 100) {
			cooldown = 7;
			entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0);
			speed = 0.05;
			World world = entity.getWorld();
			for (int i=0; i < 6; i++) {
				bullets[i] = (ShulkerBullet) world.spawnEntity(entity.getLocation().clone().add(new Vector(1, 1.3, 0).rotateAroundY(i).normalize()), EntityType.SHULKER_BULLET);
				bullets[i].setShooter(entity);
				bullets[i].setMetadata("dd-magebullet", fixdata);
			}
			for (int i=6; i < 12; i++) {
				bullets[i] = (ShulkerBullet) world.spawnEntity(entity.getLocation().clone().add(new Vector(1, 0.7, 0).rotateAroundY(i+0.5).normalize()), EntityType.SHULKER_BULLET);
				bullets[i].setShooter(entity);
				bullets[i].setMetadata("dd-magebullet", fixdata);
			}
			if (plugin.mcVersion >= 1.16)
				entity.swingMainHand();
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 0.75f, 0.5f);
		}
	}
	@Override
	public void clean() {
	}
	@Override
	public void update(FileConfiguration file) {
	}
}
