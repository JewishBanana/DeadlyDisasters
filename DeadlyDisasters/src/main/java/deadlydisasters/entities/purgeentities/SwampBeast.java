package deadlydisasters.entities.purgeentities;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.CustomHead;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public class SwampBeast extends CustomEntity {
	
	private int mudCooldown = 0, tpCooldown = 10, mudRadius;
	private Material mud;
	private LivingEntity target;

	public SwampBeast(Mob entity, Main plugin) {
		super(entity, plugin);
		this.entityType = CustomEntityType.SWAMPBEAST;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.getEquipment().setHelmet(CustomHead.SWAMPMONSTER.getHead());
		entity.getEquipment().setHelmetDropChance(0);
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(40);
		entity.setHealth(40);
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(10);
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.2);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40);
		
		entity.setMetadata("dd-swampbeast", plugin.fixedData);
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.langFile.getString("entities.swampBeast"));
		mudRadius = 8;
		
		if (plugin.mcVersion >= 1.19)
			mud = Material.MUD;
		else
			mud = Material.SOUL_SAND;
	}
	@Override
	public void tick() {
		if (entity == null)
			return;
		entity.getWorld().spawnParticle(Particle.BLOCK_CRACK, entity.getLocation().add(0,1.1,0), 10, .3, .3, .3, 1, mud.createBlockData());
		entity.getWorld().spawnParticle(Particle.FALLING_DUST, entity.getLocation().add(0,1.2,0), 7, .3, .3, .3, 1, Material.SOUL_SAND.createBlockData());
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (entity == null || entity.isDead()) {
			if (entity != null && entity.getKiller() != null && plugin.getConfig().getBoolean("customentities.allow_custom_drops")) {
				entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.ROTTEN_FLESH, plugin.random.nextInt(4)));
				entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.BONE, plugin.random.nextInt(3)));
				entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.DIRT, plugin.random.nextInt(5)));
			}
			it.remove();
			return;
		}
		if (mudCooldown > 0)
			mudCooldown--;
		else if (tpCooldown <= 11 && entity.getTarget() != null && entity.getTarget().getLocation().distanceSquared(entity.getLocation()) <= 36) {
			createMudSpell(entity.getLocation(), Utils.getVectorTowards(entity.getLocation(), entity.getTarget().getLocation()), mudRadius);
			if (plugin.mcVersion >= 1.16)
				entity.swingMainHand();
			mudCooldown = 12;
			return;
		}
		if (tpCooldown > 0)
			tpCooldown--;
		else if (mudCooldown <= 4 && entity.getTarget() != null && entity.isOnGround() && entity.getTarget().getLocation().distanceSquared(entity.getLocation()) >= 100
				&& !entity.getLocation().getBlock().getRelative(BlockFace.DOWN).isPassable() && !entity.getLocation().getBlock().getRelative(BlockFace.DOWN, 2).isPassable() && !entity.getLocation().getBlock().getRelative(BlockFace.DOWN, 3).isPassable()) {
			Location test = null;
			for (int i=0; i < 10; i++) {
				test = Utils.findSmartYSpawn(entity.getTarget().getLocation(), Utils.getSpotInSquareRadius(entity.getTarget().getLocation(), 3), 2, 4);
				if (test != null && !test.getBlock().getRelative(BlockFace.DOWN).isPassable() && !test.getBlock().getRelative(BlockFace.DOWN, 2).isPassable() && !test.getBlock().getRelative(BlockFace.DOWN, 3).isPassable())
					break;
			}
			if (test == null)
				return;
			final Location spot = test.getBlock().getRelative(BlockFace.DOWN).getLocation();
			final Material spotMaterial = spot.getBlock().getType();
			target = entity.getTarget();
			tpCooldown = 15;
			entity.setAI(false);
			entity.setGravity(false);
			float yaw = entity.getLocation().getYaw();
			entity.teleport(entity.getLocation().getBlock().getLocation().add(0.5,0,0.5));
			entity.setRotation(yaw, 70);
			Block b = entity.getLocation().getBlock().getRelative(BlockFace.DOWN);
			final Material prevMaterial = b.getType();
			if (!(b.getState() instanceof InventoryHolder) && !Utils.isBlockBlacklisted(b.getType()) && !Utils.isZoneProtected(b.getLocation()))
				b.setType(mud);
			int[] timer = {0};
			new RepeatingTask(plugin, 0, 1) {
				@Override
				public void run() {
					if (entity == null) {
						if (b != null && b.getType() == mud)
							b.setType(prevMaterial);
						if (spot != null && spot.getBlock().getType() == mud)
							spot.getBlock().setType(spotMaterial);
						cancel();
						return;
					}
					timer[0]++;
					if (timer[0] <= 30) {
						entity.teleport(entity.getLocation().add(0,-0.07,0));
						entity.getWorld().spawnParticle(Particle.BLOCK_CRACK, b.getLocation().add(.5,1,.5), 7, .3, .2, .3, 1, mud.createBlockData());
						if (timer[0] % 5 == 0 && plugin.mcVersion >= 1.19)
							entity.getWorld().playSound(b.getLocation(), Sound.BLOCK_MUD_HIT, SoundCategory.HOSTILE, 1f, .5f);
						return;
					}
					if (timer[0] == 40) {
						if (!(b.getState() instanceof InventoryHolder) && !Utils.isBlockBlacklisted(spot.getBlock().getType()) && !Utils.isZoneProtected(spot.getBlock().getLocation()))
							spot.getBlock().setType(mud);
						entity.teleport(spot.getBlock().getRelative(BlockFace.DOWN).getLocation().add(0.5,0,0.5));
						return;
					}
					if (timer[0] > 40 && timer[0] <= 60) {
						entity.teleport(entity.getLocation().add(0,0.0666,0));
						entity.getWorld().spawnParticle(Particle.BLOCK_CRACK, spot.clone().add(.5,1,.5), 7, .3, .2, .3, 1, mud.createBlockData());
						if (timer[0] % 5 == 0 && plugin.mcVersion >= 1.19)
							entity.getWorld().playSound(spot, Sound.BLOCK_MUD_HIT, SoundCategory.HOSTILE, 1f, .5f);
						if (target != null && target.getWorld().equals(entity.getWorld()))
							Utils.makeEntityFaceLocation(entity, target.getLocation());
						return;
					}
					if (timer[0] > 60) {
						entity.setAI(true);
						entity.setGravity(true);
						entity.setTarget(target);
						cancel();
						plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
							public void run() {
								if (b.getType() == mud)
									b.setType(prevMaterial);
								if (spot.getBlock().getType() == mud)
									spot.getBlock().setType(spotMaterial);
							};
						}, 200);
					}
				}
			};
			return;
		}
	}
	@Override
	public void clean() {
	}
	@Override
	public void update(FileConfiguration file) {
	}
	private void createMudSpell(Location loc, Vector dir, int radius) {
		World world = loc.getWorld();
		Vector angle = new Vector(dir.getZ(), 0, -dir.getX());
		Location spot = loc.clone().add(dir.clone().multiply(-1)).add(angle.clone().multiply(-2));
		Map<Block,Material> patches = new HashMap<>();
		int[] timer = {(radius*2)+2, 0};
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				if (timer[1] % 3 == 0 && timer[0] >= 0) {
					if (timer[0] <= 1 || timer[0] == radius+2) {
						for (int i=1; i < 4; i++) {
							Block b = Utils.getHighestExposedBlock(spot.clone().add(angle.clone().multiply(i)), 3);
							if (b == null || patches.containsKey(b) || (b.getState() instanceof InventoryHolder) || Utils.isBlockBlacklisted(b.getType()) || Utils.isZoneProtected(b.getLocation()))
								continue;
							patches.put(b, b.getType());
							b.setType(mud);
							world.spawnParticle(Particle.BLOCK_CRACK, b.getLocation().add(.5,1,.5), 5, .4, .3, .4, 1, mud.createBlockData());
						}
					} else {
						for (int i=0; i < 5; i++) {
							Block b = Utils.getHighestExposedBlock(spot.clone().add(angle.clone().multiply(i)), 3);
							if (b == null || patches.containsKey(b) || (b.getState() instanceof InventoryHolder) || Utils.isBlockBlacklisted(b.getType()) || Utils.isZoneProtected(b.getLocation()))
								continue;
							patches.put(b, b.getType());
							b.setType(mud);
							world.spawnParticle(Particle.BLOCK_CRACK, b.getLocation().add(.5,1,.5), 5, .4, .3, .4, 1, mud.createBlockData());
						}
					}
					if (plugin.mcVersion >= 1.19)
						world.playSound(spot, Sound.BLOCK_MUD_BREAK, SoundCategory.BLOCKS, 1, .5f);
					spot.add(dir.clone().multiply(0.5));
					timer[0]--;
				}
				for (Entry<Block, Material> entry : patches.entrySet())
					if (entry.getKey().getType() == mud) {
						world.spawnParticle(Particle.FALLING_DUST, entry.getKey().getLocation().add(.5,1,.5), 1, .3, .1, .3, 1, mud.createBlockData());
						for (Entity e : world.getNearbyEntities(entry.getKey().getLocation().add(.5,1.5,.5), .5, .5, .5)) {
							if (e.hasMetadata("dd-swampbeast"))
								continue;
							Vector vec = new Vector(e.getVelocity().getX()/8, e.getVelocity().getY(), e.getVelocity().getZ()/8);
							if (vec.getY() > 0)
								vec.setY(vec.getY()/8);
							e.setVelocity(vec);
							if (e instanceof Player && timer[1] % 5 == 0 && plugin.mcVersion >= 1.19)
								world.playSound(e.getLocation(), Sound.BLOCK_MUD_PLACE, SoundCategory.BLOCKS, 0.3f, .5f);
						}
					}
				if (timer[1] >= 200) {
					for (Entry<Block, Material> entry : patches.entrySet())
						if (entry.getKey().getType() == mud)
							entry.getKey().setType(entry.getValue());
					cancel();
				}
				timer[1]++;
			}
		};
	}
}
