package deadlydisasters.entities.endstormentities;

import java.util.Iterator;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.CustomHead;
import deadlydisasters.general.ItemsHandler;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;

public class VoidArcher extends CustomEntity {
	
	private double health = 20;
	private Random rand;
	private ItemStack[] armor = {new ItemStack(Material.LEATHER_BOOTS), new ItemStack(Material.LEATHER_LEGGINGS), new ItemStack(Material.LEATHER_CHESTPLATE), CustomHead.VOIDARCHER.getHead()};

	public VoidArcher(Mob entity, Main plugin, Random rand) {
		super(entity, plugin);
		this.entityType = CustomEntityType.VOIDARCHER;
		this.rand = rand;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
		entity.setHealth(health);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40);
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.05);
		for (int i=0; i < 3; i++) {
			LeatherArmorMeta meta = (LeatherArmorMeta) armor[i].getItemMeta();
			meta.setColor(Color.fromRGB(50, 50, 50));
			armor[i].setItemMeta(meta);
		}
		entity.getEquipment().setArmorContents(armor);
		entity.getEquipment().setItemInMainHand(ItemsHandler.voidswrath);
		EntityEquipment equip = entity.getEquipment();
		equip.setHelmetDropChance(0);
		equip.setChestplateDropChance(0);
		equip.setLeggingsDropChance(0);
		equip.setBootsDropChance(0);
		equip.setItemInMainHandDropChance(0);
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.langFile.getString("entities.voidArcher"));
		entity.setSilent(true);
	}
	@Override
	public void tick() {
		if (entity == null)
			return;
		entity.getWorld().spawnParticle(Particle.PORTAL, entity.getLocation().add(0,.75,0), 7, .1, .1, .1, .8);
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
				entity.getWorld().spawnParticle(Particle.SOUL, entity.getLocation().add(0,.5,0), 15, .3, .5, .3, .03);
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_DONKEY_DEATH, SoundCategory.HOSTILE, .3f, .5f);
			if (entity.getKiller() != null && plugin.getConfig().getBoolean("customentities.allow_custom_drops")) {
				entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.BONE, rand.nextInt(4)));
				if (rand.nextInt(2) == 0) entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.ARROW));
				if (rand.nextDouble() < 0.075)
					entity.getWorld().dropItemNaturally(entity.getLocation(), ItemsHandler.voidswrath);
			}
			it.remove();
			return;
		}
		if (entity.getHealth() < health) {
			health = entity.getHealth();
			for (int i=0; i < 3; i++) {
				int x = (int) (entity.getLocation().getX()+(rand.nextInt(50)-25));
				int z = (int) (entity.getLocation().getZ()+(rand.nextInt(50)-25));
				if (entity.getLocation().distance(entity.getWorld().getHighestBlockAt(x, z).getLocation()) > 25) continue;
				for (Entity e : entity.getNearbyEntities(6, 6, 6))
					if (e instanceof Player)
						((Player) e).playSound(e.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
				entity.teleport(entity.getWorld().getHighestBlockAt(x, z).getLocation().add(0,1,0));
				break;
			}
		}
		if (rand.nextInt(6) == 0)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, SoundCategory.HOSTILE, 1f, .8f);
	}
	@Override
	public void clean() {
	}
	@Override
	public void update(FileConfiguration file) {
	}
}
