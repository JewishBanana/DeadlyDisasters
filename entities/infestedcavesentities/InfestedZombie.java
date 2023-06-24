package deadlydisasters.entities.infestedcavesentities;

import java.util.Iterator;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import deadlydisasters.entities.CustomDropsFactory;
import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.CustomHead;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;
import deadlydisasters.utils.Utils;

public class InfestedZombie extends CustomEntity {
	
	private BlockData data;
	private double height;

	public InfestedZombie(Mob entity, Main plugin) {
		super(entity, plugin);
		this.entityType = CustomEntityType.INFESTEDZOMBIE;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		Utils.clearEntityOfItems(entity);
		entity.getEquipment().setHelmet(CustomHead.INFESTEDZOMBIE.getHead());
		entity.getEquipment().setHelmetDropChance(0);
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
		entity.setHealth(entityType.getHealth());
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(entityType.getDamage());
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.25);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40);
		entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true, false));
		entity.setCanPickupItems(false);
		
		entity.setMetadata("dd-infestedzombie", plugin.fixedData);
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.getString("entities.infestedZombie"));
		
		if (plugin.mcVersion >= 1.19)
			data = Material.SCULK.createBlockData();
		else
			data = Material.NETHERRACK.createBlockData();
		this.height = entity.getHeight()/4;
	}
	@Override
	public void tick() {
		if (entity == null)
			return;
		entity.getWorld().spawnParticle(Particle.BLOCK_CRACK, entity.getLocation().add(0,height*1.5,0), 10, .3, height, .3, 0.1, data);
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (entity == null || entity.isDead()) {
			if (entity != null && entity.getKiller() != null)
				CustomDropsFactory.generateDrops(entity.getLocation(), entityType);
			it.remove();
			return;
		}
	}
	@Override
	public void clean() {
	}
	@Override
	public void update(FileConfiguration file) {
	}
}
