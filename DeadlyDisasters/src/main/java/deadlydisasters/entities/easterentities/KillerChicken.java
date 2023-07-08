package deadlydisasters.entities.easterentities;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustTransition;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.EntityHandler;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;

public class KillerChicken extends CustomEntity {
	
	private Chicken chicken;
	private UUID chickenUUID;
	public List<KillerChicken> chickenList;
	
	@SuppressWarnings("deprecation")
	public KillerChicken(Zombie entity, Main plugin) {
		super(entity, plugin);
		this.entityType = CustomEntityType.KILLERCHICKEN;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
		entity.setHealth(20);
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(8);
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.25);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(30);
		entity.setCanPickupItems(false);
		entity.setSilent(true);
		entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true, false));
		if (plugin.mcVersion >= 1.16)
			((Zombie) entity).setBaby();
		else
			((Zombie) entity).setBaby(true);
		entity.setMetadata("dd-killerchicken", new FixedMetadataValue(plugin, "protected"));
		entity.setMetadata("dd-unburnable", new FixedMetadataValue(plugin, "protected"));
		entity.setMetadata("dd-eastermobs", plugin.fixedData);
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.langFile.getString("easter.killerChicken"));
		
		chicken = (Chicken) entity.getWorld().spawnEntity(entity.getLocation(), EntityType.CHICKEN);
		chicken.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
		chicken.setHealth(20);
		chicken.setMetadata("dd-eastermobs", plugin.fixedData);
		chicken.setMetadata("dd-killerchickenghost", plugin.fixedData);
		chicken.setMetadata("dd-customentity", plugin.fixedData);
		chicken.setCollidable(false);
		chickenUUID = chicken.getUniqueId();
		chicken.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		entity.setRemoveWhenFarAway(true);
	}
	@Override
	public void tick() {
		if (entity == null || chicken == null)
			return;
		chicken.teleport(entity);
		if (entity.getHealth() < chicken.getHealth()) {
			chicken.damage(0.00001);
			chicken.setHealth(entity.getHealth());
		} else if (chicken.getHealth() < entity.getHealth())
			entity.setHealth(chicken.getHealth());
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (entity == null || chicken == null) {
			if (entity != null && entity.getKiller() != null && plugin.getConfig().getBoolean("customentities.allow_custom_drops")) {
				if (plugin.random.nextInt(2) == 0)
					entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.BONE, plugin.random.nextInt(3)));
				entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.FEATHER, plugin.random.nextInt(4)));
			}
			clean();
			it.remove();
			return;
		}
		chicken = (Chicken) plugin.getServer().getEntity(chickenUUID);
	}
	@Override
	public void clean() {
		if (chicken != null)
			chicken.remove();
		if (chickenList != null)
			chickenList.remove(this);
	}
	@Override
	public void update(FileConfiguration file) {
	}
	public static void createEgg(Location loc, Main plugin, LivingEntity initialTarget, List<KillerChicken> list) {
		Item egg = loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.EGG));
		egg.setPickupDelay(100000);
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
			if (egg == null || egg.isDead())
				return;
			loc.getWorld().spawnParticle(Particle.ITEM_CRACK, egg.getLocation(), 5, .3, .3, .3, 0.001, new ItemStack(Material.EGG));
			loc.getWorld().playSound(egg.getLocation(), Sound.ENTITY_TURTLE_EGG_HATCH, SoundCategory.HOSTILE, 1f, 0.7f);
			for (int i=0; i < 8; i++) {
				DustTransition dust = new DustTransition(Color.fromRGB(plugin.random.nextInt(125)+25, 255, plugin.random.nextInt(55)+25), Color.fromRGB(25, plugin.random.nextInt(155)+100, 255), plugin.random.nextFloat());
				if (plugin.random.nextInt(2) == 0)
					dust = new DustTransition(Color.fromRGB(plugin.random.nextInt(105)+150, 25, 255), Color.fromRGB(25, plugin.random.nextInt(155)+100, 255), plugin.random.nextFloat()/2f);
				loc.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, egg.getLocation().add(0,.2,0).add(plugin.random.nextDouble()-.5,plugin.random.nextDouble()-.5,plugin.random.nextDouble()-.5), 1, 0, 0, 0, 0.001, dust);
			}
			Zombie zombie = (Zombie) loc.getWorld().spawnEntity(egg.getLocation(), EntityType.ZOMBIE, false);
			egg.remove();
			KillerChicken kc = new KillerChicken(zombie, plugin);
			kc.chickenList = list;
			plugin.handler.addEntity(kc);
			zombie.setTarget(initialTarget);
		}, 60);
	}
}
