package deadlydisasters.entities.sandstormentities;

import java.util.Iterator;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;

import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.CustomHead;
import deadlydisasters.general.ItemsHandler;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;
import deadlydisasters.utils.Utils;

public class AncientMummy extends CustomEntity {
		
	private Random rand;
	private BlockData bd = Material.OBSIDIAN.createBlockData();
	private int cooldown;

	public AncientMummy(Mob entity, Main plugin, Random rand) {
		super(entity, plugin);
		this.entityType = CustomEntityType.ANCIENTMUMMY;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		this.rand = rand;
		entity.setMetadata("dd-ancientmummy", new FixedMetadataValue(plugin, "protected"));
		entity.getEquipment().setHelmet(CustomHead.MUMMY.getHead());
		entity.getEquipment().setHelmetDropChance(0);
		entity.setCanPickupItems(false);
		entity.setSilent(true);
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(40);
		entity.setHealth(40);
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(10);
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.25);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40);
		
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.langFile.getString("entities.ancientMummy"));
	}
	@Override
	public void tick() {
		if (entity == null)
			return;
		if (entity.getNoDamageTicks() == 20)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_HUSK_HURT, SoundCategory.HOSTILE, 1f, .5f);
		entity.getWorld().spawnParticle(Particle.FALLING_DUST, entity.getLocation().clone().add(0,1.6,0), 4, .4, .5, .4, 1, bd);
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
			if (entity.getKiller() != null && plugin.getConfig().getBoolean("customentities.allow_custom_drops")) {
				entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.ROTTEN_FLESH, plugin.random.nextInt(4)));
				if (plugin.random.nextInt(2) == 0)
					entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.BONE, plugin.random.nextInt(3)));
				if (plugin.random.nextDouble() < 0.15)
					entity.getWorld().dropItemNaturally(entity.getLocation(), ItemsHandler.ancientcloth);
			}
			it.remove();
			return;
		}
		if (rand.nextInt(5) == 0 && Tag.WOODEN_DOORS.getValues().contains(entity.getLocation().add(entity.getLocation().getDirection()).getBlock().getType()) && !Utils.isZoneProtected(entity.getLocation().add(entity.getLocation().getDirection()))) {
			entity.getLocation().add(entity.getLocation().getDirection()).getBlock().breakNaturally();
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 1, 1);
		}
		if (rand.nextInt(10) == 0)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_HUSK_AMBIENT, SoundCategory.HOSTILE, 1, .7f);
		if (cooldown > 0) {
			cooldown--;
			if (cooldown == 9)
				entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.25);
			return;
		}
		if (entity.getTarget() == null || entity.getLocation().distanceSquared(entity.getTarget().getLocation()) > 100)
			return;
		cooldown = 12;
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.45);
		entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, SoundCategory.HOSTILE, 1.5f, .5f);
	}
	@Override
	public void clean() {
	}
	@Override
	public void update(FileConfiguration file) {
	}
}
