package deadlydisasters.entities.solarstormentities;

import java.util.Iterator;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import deadlydisasters.entities.CustomDropsFactory;
import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public class FirePhantom extends CustomEntity {
	
	private Random rand;
	private double igniteChance;

	public FirePhantom(Mob entity, Main plugin, Random rand) {
		super(entity, plugin);
		this.rand = rand;
		this.entityType = CustomEntityType.FIREPHANTOM;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
		entity.setHealth(entityType.getHealth());
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(entityType.getDamage());
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(30);
		entity.setMetadata("dd-firephantom", new FixedMetadataValue(plugin, "protected"));
		entity.setMetadata("dd-unburnable", new FixedMetadataValue(plugin, "protected"));
		entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true, false));
		if (plugin.mcVersion >= 1.17)
			entity.setVisualFire(true);
		igniteChance = (double) entityType.grabCustomSetting("ignite_chance");
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.langFile.getString("entities.firePhantom"));
	}
	@Override
	public void tick() {
		if (entity == null || entity.isDead())
			return;
		Location loc = entity.getLocation().clone();
		World world = loc.getWorld();
		for (int i=0; i < 15; i++) {
			loc.setY(loc.getY()-1.75);
			if (!loc.getBlock().isPassable())
				break;
			world.spawnParticle(Particle.FLAME, loc, 1, .5, 1, .5, 0.3);
			world.playSound(loc, Sound.BLOCK_FIRE_AMBIENT, SoundCategory.HOSTILE, 1, 1);
			for (Entity e : world.getNearbyEntities(loc, 1, 1, 1))
				e.setFireTicks(200);
		}
		loc = entity.getLocation();
		for (int i=0; i < 15; i++) {
			if (!loc.getBlock().isPassable())
				break;
			world.spawnParticle(Particle.FLAME, loc, 1, .5, 1, .5, 0.3);
			world.playSound(loc, Sound.BLOCK_FIRE_AMBIENT, SoundCategory.HOSTILE, 1, 1);
			for (Entity e : world.getNearbyEntities(loc, 1, 1, 1))
				e.setFireTicks(100);
			loc.setY(loc.getY()+1.75);
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
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.HOSTILE, 1f, .5f);
			if (entity.getKiller() != null)
				CustomDropsFactory.generateDrops(entity.getLocation(), entityType);
			entity.getWorld().spawnParticle(Particle.SMOKE_LARGE, entity.getLocation(), 60, .7, .5, .7, 0.0001);
			explode();
			it.remove();
			return;
		}
		Location loc = entity.getLocation().clone();
		for (int i=0; i < 20; i++) {
			loc.setY(loc.getY()-1);
			if (!loc.getBlock().isPassable()) {
				if (rand.nextDouble()*100 < igniteChance) {
					Block b = loc.add(0,1,0).getBlock();
					if (b.getType() == Material.AIR || !Utils.isBlockImmune(b.getType()))
						b.setType(Material.FIRE);
				}
				break;
			}
		}
	}
	@Override
	public void clean() {
	}
	@Override
	public void update(FileConfiguration file) {
	}
	private void explode() {
		World world = entity.getWorld();
		world.spawnParticle(Particle.FLAME, entity.getLocation(), 300, 0, 0, 0, .5);
		Item[] items = new Item[6];
		for (int i=0; i < 6; i++) {
			items[i] = world.dropItemNaturally(entity.getLocation(), new ItemStack(Material.BLAZE_POWDER));
			if (plugin.mcVersion >= 1.16)
				items[i].setVisualFire(true);
			items[i].setPickupDelay(100000);
			items[i].setMetadata("dd-invulnerable", new FixedMetadataValue(plugin, "protected"));
			items[i].setVelocity(items[i].getVelocity().multiply(2));
		}
		int[] time = {(int) entityType.grabCustomSetting("death_flames_life_ticks")};
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				time[0]--;
				if (time[0] <= 0) {
					cancel();
					for (Item item : items)
						if (item != null)
							item.remove();
				}
				for (Item item : items)
					if (item != null)
						for (Entity e : world.getNearbyEntities(item.getLocation(), .3, .3, .3))
							e.setFireTicks(6000);
			}
		};
	}
}
