package deadlydisasters.entities.purgeentities;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.general.Main;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public class TunnellerZombie extends CustomEntity {
	
	private LivingEntity target;
	
	private Queue<Block> blocks = new ArrayDeque<>();

	@SuppressWarnings("deprecation")
	public TunnellerZombie(Zombie entity, LivingEntity target, Main plugin) {
		super(entity, plugin);
		this.entityType = CustomEntityType.TUNNELLER;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_PICKAXE));
		entity.getEquipment().setItemInMainHandDropChance(0.03f);
		entity.getEquipment().setItemInOffHand(new ItemStack(Material.COBBLESTONE));
		entity.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
		
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(30);
		entity.setHealth(30);
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(6);
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.35);
		if (plugin.mcVersion >= 1.16) {
			if (!entity.isAdult())
				entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.2);
		} else if (entity.isBaby())
			entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.2);
	}
	@Override
	public void tick() {
		if (entity == null)
			return;
		if (entity.getVelocity().getY() < -0.5 && entity.getLocation().clone().subtract(0,3,0).getBlock().getType().isSolid() && entity.getLocation().clone().subtract(0,2,0).getBlock().isPassable()) {
			Location temp = entity.getLocation().clone().subtract(0,2,0);
			if (!Utils.isBlockBlacklisted(temp.getBlock().getType()) && !Utils.isZoneProtected(temp)) {
				temp.getBlock().setType(Material.WATER);
				entity.getEquipment().setItemInMainHand(new ItemStack(Material.WATER_BUCKET));
				plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
					public void run() {
						temp.getBlock().setType(Material.AIR);
						entity.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_PICKAXE));
					}
				}, 9);
			}
		}
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (entity == null || entity.isDead()) {
			if (entity != null && entity.getKiller() != null && plugin.getConfig().getBoolean("customentities.allow_custom_drops")) {
				entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.ROTTEN_FLESH, plugin.random.nextInt(4)));
				entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.BONE, plugin.random.nextInt(3)));
				entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.COBBLESTONE, plugin.random.nextInt(10)));
				if (plugin.random.nextInt(100) < 5)
					entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.WATER_BUCKET));
				if (plugin.random.nextInt(100) < 2)
					entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.DIAMOND_PICKAXE));
			}
			it.remove();
			new RepeatingTask(plugin, 100, 20) {
				public void run() {
					if (blocks.isEmpty())
						cancel();
					else {
						Block b = blocks.poll();
						if (b.getType() == Material.COBBLESTONE) {
							b.getWorld().spawnParticle(Particle.BLOCK_CRACK, b.getLocation().clone().add(0.5,0.5,0.5), 7, 0, 0, 0, 0.01, b.getBlockData());
							b.breakNaturally(new ItemStack(Material.AIR));
						}
					}
				}
			};
			return;
		}
		if (target != null && (!entity.getWorld().equals(target.getWorld()) || (target instanceof Player && Utils.isPlayerImmune((Player) target)))) {
			target = null;
			return;
		}
		if (entity.getTarget() == null)
			if (target == null)
				return;
			else
				entity.setTarget(target);
		else if (target == null)
			target = entity.getTarget();
		if (entity.getVelocity().getX() < 0.08 && entity.getVelocity().getZ() < 0.08) {
			Location temp = entity.getLocation().clone().add(0,2,0);
			if (!temp.getBlock().isPassable() && !Utils.isBlockBlacklisted(temp.getBlock().getType()) && !Utils.isZoneProtected(temp)) {
				//cp
				temp.getBlock().breakNaturally();
				if (plugin.mcVersion >= 1.16)
					entity.swingMainHand();
				return;
			}
			Location loc = entity.getLocation();
			Location targetLoc = target.getLocation().clone();
			if (!loc.getWorld().equals(targetLoc.getWorld()))
				return;
			targetLoc.setY(loc.getY());
			if (target.getLocation().getBlockY()-1 > loc.getBlockY() && entity.getVelocity().getY() > -0.5 && (loc.distanceSquared(targetLoc) <= 4 || (entity.getVelocity().getX() <= 0 && entity.getVelocity().getZ() <= 0))) {
				if (!Utils.isBlockBlacklisted(temp.getBlock().getType()) && !Utils.isZoneProtected(temp)) {
					entity.setVelocity(new Vector(0,0.45,0));
					//cp
					loc.getBlock().setType(Material.COBBLESTONE);
					entity.getWorld().playSound(temp, Sound.BLOCK_STONE_PLACE, 1, 1);
					if (plugin.mcVersion >= 1.16)
						entity.swingOffHand();
					blocks.add(loc.getBlock());
					return;
				}
			} else if (entity.getVelocity().getX() <= 0 && entity.getVelocity().getZ() <= 0) {
				if (target.getLocation().getBlockY() < loc.getBlockY()) {
					Location b = loc.clone().subtract(0,1,0);
					if (!Utils.isBlockBlacklisted(b.getBlock().getType()) && !Utils.isZoneProtected(b)) {
						//cp
						b.getBlock().breakNaturally();
						if (plugin.mcVersion >= 1.16)
							entity.swingMainHand();
						return;
					}
				} else {
					temp = entity.getLocation().clone().add(new Vector(targetLoc.getX() - loc.getX(), targetLoc.getY() - loc.getY(), targetLoc.getZ() - loc.getZ()).normalize().multiply(1.25));
					temp.setY(temp.getY()-1);
					if (temp.getBlock().isPassable() && !Utils.isBlockBlacklisted(temp.getBlock().getType()) && !Utils.isZoneProtected(temp)) {
						//cp
						temp.getBlock().setType(Material.COBBLESTONE);
						entity.getWorld().playSound(temp, Sound.BLOCK_STONE_PLACE, 1, 1);
						if (plugin.mcVersion >= 1.16)
							entity.swingOffHand();
						blocks.add(temp.getBlock());
						return;
					}
				}
			}
			targetLoc = target.getLocation();
			temp = entity.getLocation().clone().add(new Vector(targetLoc.getX() - loc.getX(), targetLoc.getY() - loc.getY(), targetLoc.getZ() - loc.getZ()).normalize().multiply(1.25));
			if (temp.getBlock().equals(loc.getBlock()) && !Utils.isBlockBlacklisted(loc.getBlock().getType()) && !Utils.isZoneProtected(loc)) {
				//cp
				temp.getBlock().breakNaturally();
				if (plugin.mcVersion >= 1.16)
					entity.swingMainHand();
				return;
			}
			if (targetLoc.getBlockY()-1 > entity.getLocation().getBlockY())
				temp.setY(entity.getLocation().getY()+3);
			else
				temp.setY(entity.getLocation().getY()+2);
			for (int i=0; i < 3; i++) {
				if (!temp.getBlock().isPassable() && !Utils.isBlockBlacklisted(temp.getBlock().getType()) && !Utils.isZoneProtected(temp)) {
					//cp
					temp.getBlock().breakNaturally();
					if (plugin.mcVersion >= 1.16)
						entity.swingMainHand();
					break;
				}
				temp.setY(temp.getY()-1);
			}
		}
	}
	@Override
	public void clean() {
	}
	@Override
	public void update(FileConfiguration file) {
	}
}
