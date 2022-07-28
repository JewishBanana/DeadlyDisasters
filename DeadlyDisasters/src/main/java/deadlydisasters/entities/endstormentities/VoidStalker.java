package deadlydisasters.entities.endstormentities;

import java.util.Iterator;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;

public class VoidStalker extends CustomEntity {
	
	private Random rand;
	
	public VoidStalker(Mob entity, Main plugin, Random rand) {
		super(entity, plugin);
		this.rand = rand;
		this.entityType = CustomEntityType.VOIDSTALKER;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true));
		entity.setMetadata("dd-voidstalker", new FixedMetadataValue(plugin, "protected"));
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(8);
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.langFile.getString("entities.voidStalker"));
		entity.setMetadata("dd-unburnable", new FixedMetadataValue(plugin, "protected"));
	}
	@Override
	public void tick() {
		if (entity == null)
			return;
		entity.getWorld().spawnParticle(Particle.SQUID_INK, entity.getLocation(), 5, .4, .4, .4, 0.01);
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
				entity.getWorld().spawnParticle(Particle.SOUL, entity.getLocation(), 10, .3, .3, .3, .03);
			if (entity.getKiller() != null && plugin.getConfig().getBoolean("customentities.allow_custom_drops"))
				if (rand.nextInt(3) == 0)
					entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.ENDER_EYE));
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
