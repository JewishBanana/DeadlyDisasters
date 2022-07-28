package deadlydisasters.entities.purgeentities;

import java.util.Iterator;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.CustomHead;
import deadlydisasters.general.Main;

public class SkeletonKnight extends CustomEntity {
	
	private SkeletonHorse horse;
	private UUID horseUUID;

	public SkeletonKnight(Skeleton entity, Main plugin) {
		super(entity, plugin);
		this.entityType = CustomEntityType.SKELETONKNIGHT;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.getEquipment().setHelmet(CustomHead.SKELETONKNIGHT.getHead());
		entity.getEquipment().setHelmetDropChance(0);
		entity.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
		entity.getEquipment().setItemInOffHand(new ItemStack(Material.SHIELD));
		
		if (!entity.isInsideVehicle()) {
			horse = (SkeletonHorse) entity.getWorld().spawnEntity(entity.getLocation(), EntityType.SKELETON_HORSE);
			horse.setTamed(true);
			horse.addPassenger(entity);
			horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.375);
			horseUUID = horse.getUniqueId();
			horse.setRemoveWhenFarAway(true);
		} else
			if (entity.getVehicle() instanceof SkeletonHorse)
				horseUUID = entity.getVehicle().getUniqueId();
	}
	@Override
	public void tick() {
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Skeleton) plugin.getServer().getEntity(entityUUID);
		if (horseUUID != null)
			horse = (SkeletonHorse) plugin.getServer().getEntity(horseUUID);
		if (entity == null || entity.isDead()) {
			if (entity != null && entity.getKiller() != null && plugin.getConfig().getBoolean("customentities.allow_custom_drops")) {
				entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.BONE, plugin.random.nextInt(4)));
				if (plugin.random.nextInt(10) == 0)
					entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.IRON_SWORD));
			}
			it.remove();
			if (horse != null && !horse.isDead()) {
				horse.setTamed(false);
				plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
					public void run() {
						if (horse != null && !horse.isDead())
							horse.remove();
					}
				}, 600);
			}
			return;
		}
	}
	@Override
	public void clean() {
		if (horse != null && !horse.isDead())
			horse.remove();
	}
	@Override
	public void update(FileConfiguration file) {
	}
}
