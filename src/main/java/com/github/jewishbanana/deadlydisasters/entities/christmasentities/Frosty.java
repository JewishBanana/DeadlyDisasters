package com.github.jewishbanana.deadlydisasters.entities.christmasentities;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.EntityHandler;
import com.github.jewishbanana.deadlydisasters.handlers.ItemsHandler;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class Frosty extends CustomEntity {
	
	private int iceCooldown = 0, tpCooldown = 10, iceRadius = 10;
	private LivingEntity target;
	private Random rand;

	public Frosty(Snowman entity, Main plugin, Random rand) {
		super(entity, plugin);
		this.rand = rand;
		this.entityType = CustomEntityType.FROSTY;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.setDerp(true);
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(30.0);
		entity.setHealth(30.0);
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.2);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40);
		entity.setCanPickupItems(false);
		entity.setRemoveWhenFarAway(true);
		
		entity.setMetadata("dd-frosty", plugin.fixedData);
		entity.setMetadata("dd-christmasmob", plugin.fixedData);
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.langFile.getString("christmas.frosty"));
		entity.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
	}
	@Override
	public void tick() {
		if (entity == null)
			return;
		entity.getWorld().spawnParticle(VersionUtils.getBlockCrack(), entity.getLocation().add(0,1.1,0), 5, .3, .3, .3, 1, Material.PACKED_ICE.createBlockData());
		entity.getWorld().spawnParticle(Particle.FALLING_DUST, entity.getLocation().add(0,1.2,0), 2, .3, .4, .3, 1, Material.PACKED_ICE.createBlockData());
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (entity == null || entity.isDead()) {
			if (entity != null && entity.getKiller() != null) {
				entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.SNOWBALL, rand.nextInt(6)+2));
				if (rand.nextInt(2) == 0)
					entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.STICK, rand.nextInt(2)+1));
				if (rand.nextInt(100) < 8)
					entity.getWorld().dropItemNaturally(entity.getLocation(), ItemsHandler.brokenSnowGlobe);
			}
			it.remove();
			return;
		}
		if (entity.getTarget() == null)
			for (Entity e : entity.getWorld().getNearbyEntities(entity.getLocation(), 15, 15, 15)) {
				if (e instanceof Player && !Utils.isPlayerImmune((Player) e)) {
					entity.setTarget((LivingEntity) e);
					entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GHAST_DEATH, SoundCategory.HOSTILE, 2, .8f);
					break;
				}
			}
		else if (entity.getTarget().isDead() || (entity.getTarget() instanceof Player && Utils.isPlayerImmune((Player) entity.getTarget())))
			entity.setTarget(null);
		if (iceCooldown > 0)
			iceCooldown--;
		else if (tpCooldown <= 11 && entity.getTarget() != null && entity.getTarget().getLocation().distanceSquared(entity.getLocation()) <= 81) {
			createIceSpell(entity.getLocation(), Utils.getVectorTowards(entity.getLocation(), entity.getTarget().getLocation()), iceRadius);
			iceCooldown = 12;
			return;
		}
		if (tpCooldown > 0)
			tpCooldown--;
		else if (iceCooldown <= 4 && entity.getTarget() != null && entity.isOnGround() && entity.getTarget().getLocation().distanceSquared(entity.getLocation()) >= 225
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
			if (!(b.getState() instanceof InventoryHolder) && !Utils.isBlockImmune(b.getType()) && !Utils.isZoneProtected(b.getLocation()))
				b.setType(Material.PACKED_ICE);
			int[] timer = {0};
			new RepeatingTask(plugin, 0, 1) {
				@Override
				public void run() {
					if (entity == null) {
						if (b != null && b.getType() == Material.PACKED_ICE)
							b.setType(prevMaterial);
						if (spot != null && spot.getBlock().getType() == Material.PACKED_ICE)
							spot.getBlock().setType(spotMaterial);
						cancel();
						return;
					}
					timer[0]++;
					if (timer[0] <= 30) {
						entity.teleport(entity.getLocation().add(0,-0.07,0));
						entity.getWorld().spawnParticle(VersionUtils.getBlockCrack(), b.getLocation().add(.5,1,.5), 7, .3, .2, .3, 1, Material.PACKED_ICE.createBlockData());
						if (timer[0] % 5 == 0)
							entity.getWorld().playSound(b.getLocation(), Sound.BLOCK_GLASS_HIT, SoundCategory.HOSTILE, 1f, .5f);
						return;
					}
					if (timer[0] == 40) {
						if (!(b.getState() instanceof InventoryHolder) && !Utils.isBlockImmune(spot.getBlock().getType()) && !Utils.isZoneProtected(spot.getBlock().getLocation()))
							spot.getBlock().setType(Material.PACKED_ICE);
						entity.teleport(spot.getBlock().getRelative(BlockFace.DOWN).getLocation().add(0.5,0,0.5));
						return;
					}
					if (timer[0] > 40 && timer[0] <= 60) {
						entity.teleport(entity.getLocation().add(0,0.0666,0));
						entity.getWorld().spawnParticle(VersionUtils.getBlockCrack(), spot.clone().add(.5,1,.5), 7, .3, .2, .3, 1, Material.PACKED_ICE.createBlockData());
						if (timer[0] % 5 == 0)
							entity.getWorld().playSound(spot, Sound.BLOCK_GLASS_HIT, SoundCategory.HOSTILE, 1f, .5f);
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
								if (b.getType() == Material.PACKED_ICE)
									b.setType(prevMaterial);
								if (spot.getBlock().getType() == Material.PACKED_ICE)
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
	private void createIceSpell(Location loc, Vector dir, int radius) {
		World world = loc.getWorld();
		Vector angle = new Vector(dir.getZ(), 0, -dir.getX());
		Location spot = loc.clone().add(dir.clone().multiply(-2)).add(angle.clone().multiply(-4));
		Map<Block, BlockState> patches = new HashMap<>();
		int[] timer = {(radius*2)+4, 0};
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				if (timer[1] % 2 == 0 && timer[0] >= 0) {
					if (timer[0] <= 4 || timer[0] >= radius) {
						for (int i=2; i < 6; i++) {
							Block b = Utils.getHighestExposedBlock(spot.clone().add(angle.clone().multiply(i)), 3);
							if (b == null || patches.containsKey(b) || (b.getState() instanceof InventoryHolder) || Utils.isBlockImmune(b.getType()) || Utils.isZoneProtected(b.getLocation()))
								continue;
							patches.put(b, b.getState());
							b.setType(Material.PACKED_ICE);
							world.spawnParticle(VersionUtils.getBlockCrack(), b.getLocation().add(.5,1,.5), 5, .4, .3, .4, 1, Material.PACKED_ICE.createBlockData());
						}
					} else {
						for (int i=0; i < 9; i++) {
							Block b = Utils.getHighestExposedBlock(spot.clone().add(angle.clone().multiply(i)), 3);
							if (b == null || patches.containsKey(b) || (b.getState() instanceof InventoryHolder) || Utils.isBlockImmune(b.getType()) || Utils.isZoneProtected(b.getLocation()))
								continue;
							patches.put(b, b.getState());
							b.setType(Material.PACKED_ICE);
							world.spawnParticle(VersionUtils.getBlockCrack(), b.getLocation().add(.5,1,.5), 5, .4, .3, .4, 1, Material.PACKED_ICE.createBlockData());
						}
					}
					if (plugin.mcVersion >= 1.17)
						world.playSound(spot, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.BLOCKS, 0.6f, .5f);
					spot.add(dir.clone().multiply(0.5));
					timer[0]--;
				}
				for (Entry<Block, BlockState> entry : patches.entrySet())
					if (entry.getKey().getType() == Material.PACKED_ICE) {
						if (rand.nextInt(5) == 0)
							world.spawnParticle(Particle.FALLING_DUST, entry.getKey().getLocation().add(.5,1,.5), 1, .3, .1, .3, 1, Material.PACKED_ICE.createBlockData());
						for (Entity e : world.getNearbyEntities(entry.getKey().getLocation().add(.5,1.5,.5), .5, .5, .5)) {
							if (e.hasMetadata("dd-christmasmob"))
								continue;
							Vector vec = new Vector(e.getVelocity().getX()/8, e.getVelocity().getY(), e.getVelocity().getZ()/8);
							if (vec.getY() > 0)
								vec.setY(vec.getY()/8);
							e.setVelocity(vec);
							if (plugin.mcVersion >= 1.17)
								e.setFreezeTicks(220);
							if (plugin.mcVersion >= 1.17 && e instanceof Player && timer[1] % 5 == 0)
								world.playSound(e.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.BLOCKS, 0.2f, .5f);
						}
					}
				if (timer[1] >= 200) {
					for (Entry<Block, BlockState> entry : patches.entrySet())
						if (entry.getKey().getType() == Material.PACKED_ICE)
							entry.getValue().update(true);
					cancel();
				}
				timer[1]++;
			}
		};
	}
}
