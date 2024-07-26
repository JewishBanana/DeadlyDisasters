package com.github.jewishbanana.deadlydisasters.entities.halloweenentities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Particle.DustTransition;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.CustomHead;
import com.github.jewishbanana.deadlydisasters.entities.EntityHandler;
import com.github.jewishbanana.deadlydisasters.events.disasters.DeathParade;
import com.github.jewishbanana.deadlydisasters.handlers.ItemsHandler;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.utils.AsyncRepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.NBSongs;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.SongPlayer;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

import net.md_5.bungee.api.ChatColor;

public class PumpkinKing extends CustomEntity {
	
	private Random rand;
	private BossBar bar;
	private int cooldown = 3, noTargetTicks;
	private Location lastTargetLoc;
	private Entity lastTarget;
	private int targetHoldTicks;
	private LivingEntity basketTarget;
	private Location step;
	public SongPlayer songPlayer;
	public int damageTicks;
	
	private Slime projectile;
	private ArmorStand pumpkin;
	public DeathParade parade;
	
	private Set<Entity> toRemove = new HashSet<>();
	private Set<UUID> ghouls = new HashSet<>();
	
	public static Set<Block> pumpkinBasketBlocks = new HashSet<>();

	public PumpkinKing(Mob entity, Main plugin, Random rand) {
		super(entity, plugin);
		this.rand = rand;
		this.entityType = CustomEntityType.PUMPKINKING;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.setSilent(true);
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityType.getHealth());
		entity.setHealth(entityType.getHealth());
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.2);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(60);
		
		entity.getEquipment().setHelmetDropChance(0);
		entity.getEquipment().setHelmet(new ItemStack(Material.JACK_O_LANTERN));
		entity.getEquipment().setItemInOffHandDropChance(0);
		entity.getEquipment().setItemInOffHand(ItemsHandler.etherealLanternBoss);
		
		entity.setMetadata("dd-pumpkinking", plugin.fixedData);
		entity.setMetadata("dd-halloweenmobs", plugin.fixedData);
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.getString("halloween.pumpkinKing"));
		
		step = entity.getLocation();
		bar = Bukkit.createBossBar(Utils.convertString("&c"+Languages.getString("halloween.pumpkinKing")), BarColor.RED, BarStyle.SOLID, BarFlag.DARKEN_SKY, BarFlag.CREATE_FOG);
	}
	@Override
	public void tick() {
		if (entity == null)
			return;
		if (entity.getNoDamageTicks() == 20)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOGLIN_HURT, SoundCategory.HOSTILE, 1f, .5f);
		bar.setProgress(Utils.clamp((1.0 / entityType.getHealth()) * entity.getHealth(), 0, 1));
		
		if (damageTicks >= 12) {
			damageTicks = 0;
			for (Entity e : entity.getWorld().getNearbyEntities(entity.getLocation(), 8, 8, 8, p -> (p instanceof Player && !p.isDead() && !Utils.isEntityImmunePlayer(p))))
				spawnSoulBomb(entity.getLocation().add(0,1.4,0), (LivingEntity) e, new Vector(0, 0, 0), true);
		}
		
		for (int i=0; i < 8; i++) {
			DustTransition dust = new DustTransition(Color.BLACK, Color.fromRGB(255, rand.nextInt(40)+80, 0), rand.nextFloat());
			entity.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, entity.getLocation().add(rand.nextDouble()-.5, 1.4+(rand.nextDouble()-.5), rand.nextDouble()-.5), 1, 0, 0, 0, 0.001, dust);
		}
		if (step.distanceSquared(entity.getLocation()) > 3 && entity.isOnGround()) {
			step = entity.getLocation();
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOGLIN_STEP, SoundCategory.HOSTILE, 2f, .5f);
		}
		if (entity.getEquipment().getItemInOffHand().getItemMeta().getPersistentDataContainer().has(ItemsHandler.etherealLanternBossKey, PersistentDataType.BYTE))
			Bukkit.broadcastMessage("1");
		else
			Bukkit.broadcastMessage("2");
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (entity == null) {
			it.remove();
			clean();
			return;
		}
		if (entity.isDead()) {
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.HOSTILE, 1f, .5f);
			if (CustomEntityType.dropsEnabled)
				entity.getWorld().dropItemNaturally(entity.getLocation(), ItemsHandler.etherealLantern);
			bar.removeAll();
			it.remove();
			clean();
			return;
		}
		bar.removeAll();
		if (songPlayer != null)
			songPlayer.clearPlayers();
		for (Entity e : entity.getNearbyEntities(40, 40, 40).stream().filter(p -> p instanceof Player).sorted((e1, e2) -> (int) e1.getLocation().distanceSquared(e2.getLocation())).collect(Collectors.toList())) {
			bar.addPlayer((Player) e);
			if (songPlayer != null)
				songPlayer.addPlayer((Player) e);
			if (entity.getTarget() == null && !Utils.isEntityImmunePlayer(e))
				entity.setTarget((LivingEntity) e);
		}
		
		if (entity.getTarget() == null) {
			if (basketTarget != null && !basketTarget.isDead())
				entity.setTarget(basketTarget);
			if (noTargetTicks < 2) {
				if (noTargetTicks++ == 2)
					entity.getEquipment().setItemInOffHand(ItemsHandler.etherealLanternBoss);
			}
		} else {
			if (entity.getEquipment().getItemInOffHand().getType() != Material.AIR && entity.getEquipment().getItemInOffHand().getItemMeta().getPersistentDataContainer().has(ItemsHandler.etherealLanternBossKey, PersistentDataType.BYTE)) {
				noTargetTicks = 0;
				entity.getEquipment().setItemInOffHand(ItemsHandler.etherealLanternBoss2);
			}
			if (lastTarget == null || !entity.getTarget().equals(lastTarget)) {
				lastTarget = entity.getTarget();
				targetHoldTicks = 0;
			} else {
				if (targetHoldTicks++ == 7) {
					targetHoldTicks = 0;
					throwPumpkin(entity.getTarget(), true);
					cooldown = 9;
				} else if (lastTargetLoc == null || lastTargetLoc.getWorld() != lastTarget.getWorld() || lastTargetLoc.distanceSquared(lastTarget.getLocation()) > 16) {
					targetHoldTicks = 0;
					lastTargetLoc = lastTarget.getLocation();
				}
			}
		}
		if (rand.nextInt(4) == 0)
			switch (rand.nextInt(3)) {
			case 0:
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOGLIN_AMBIENT, SoundCategory.HOSTILE, 2f, .5f);
				break;
			case 1:
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOGLIN_ANGRY, SoundCategory.HOSTILE, 2f, .5f);
				break;
			case 2:
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOGLIN_DEATH, SoundCategory.HOSTILE, 1.5f, .5f);
				break;
			}

		if (damageTicks != 0)
			damageTicks--;
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		if (entity.getTarget() instanceof ArmorStand)
			return;
		List<Integer> choice = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4));
		Collections.shuffle(choice);
		label:
		for (Integer num : choice)
			switch (num) {
			case 0:
				if (Utils.isTargetInRange(entity, 0, 100, true)) {
					Iterator<UUID> iterator = ghouls.iterator();
					while (iterator.hasNext()) {
						Entity e = Bukkit.getEntity(iterator.next());
						if (e == null || e.isDead())
							iterator.remove();
					}
					if (ghouls.size() > 5)
						continue;
					int c = 0;
					Set<Block> prev = new HashSet<>();
					for (int i=0; i < 20; i++) {
						Location spawn = Utils.findSmartYSpawn(entity.getLocation(), Utils.getSpotInSquareRadius(entity.getLocation(), rand.nextInt(6)+3), 2, 5);
						if (spawn == null || prev.contains(spawn.getBlock()))
							continue;
						spawn.subtract(0,1,0);
						prev.add(spawn.getBlock());
						plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
							Zombie zombie = spawn.getWorld().spawn(spawn, Zombie.class, false, consumer -> {
								consumer.setRotation(plugin.random.nextFloat()*360, 0);
							});
							Ghoul ghoul = CustomEntity.handler.addEntity(new Ghoul(zombie, spawn.getBlock(), plugin, true));
							ghouls.add(zombie.getUniqueId());
							plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
								ghoul.setWalking(true);
								ghoul.getEntity().setVelocity(new Vector(0,.4,0));
								((Mob) ghoul.getEntity()).setTarget(entity.getTarget());
								ghoul.grabAnimation.stop();
							}, 60);
						}, rand.nextInt(80));
						if (++c == 5)
							break;
					}
					cooldown = c * 2;
					entity.addPotionEffect(new PotionEffect(VersionUtils.getSlowness(), c * 20, 5, true, false, false));
					entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOGLIN_DEATH, SoundCategory.HOSTILE, 2f, .5f);
					break label;
				}
				break;
			case 1:
				if (Utils.isTargetInRange(entity, 49, 1600, false)) {
					throwPumpkin(entity.getTarget(), false);
					cooldown = 6;
					break label;
				}
				break;
			case 2:
				if (Utils.isTargetInRange(entity, 36, 256, false)) {
					int c = 0;
					Set<Block> prev = new HashSet<>();
					for (int i=0; i < 20; i++) {
						Location spawnLoc = Utils.findSmartYSpawn(entity.getTarget().getLocation(), Utils.getSpotInSquareRadius(entity.getTarget().getLocation(), rand.nextInt(20)+1), 2, 8);
						if (spawnLoc == null)
							continue;
						Block spawn = spawnLoc.getBlock().getRelative(BlockFace.DOWN);
						if (prev.contains(spawn) || prev.stream().anyMatch(e -> e.getLocation().distanceSquared(spawn.getLocation()) < 16))
							continue;
						prev.add(spawn);
						plugin.getServer().getScheduler().runTaskLater(plugin, () -> createBloodCircle(spawn, 80, 40), c * 10);
						if (++c == 5)
							break;
					}
					cooldown = c;
					entity.addPotionEffect(new PotionEffect(VersionUtils.getSlowness(), c  * 5, 5, true, false, false));
					entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOGLIN_ANGRY, SoundCategory.HOSTILE, 2f, .5f);
					break label;
				}
				break;
			case 3:
				if (Utils.isTargetInRange(entity, 64, 1600, true)) {
					for (Entity e : entity.getWorld().getNearbyEntities(entity.getLocation(), 15, 15, 15, p -> (p instanceof Player && !p.isDead() && !Utils.isEntityImmunePlayer(p))))
						spawnSoulBomb(entity.getLocation().add(0,1.4,0), (LivingEntity) e, new Vector(0, 2, 0), false);
					cooldown = 7;
					entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOGLIN_ANGRY, SoundCategory.HOSTILE, 2f, .5f);
					break label;
				}
				break;
			case 4:
				cooldown = 6;
				break label;
			}
	}
	@Override
	public void clean() {
		bar.removeAll();
		if (songPlayer != null)
			songPlayer.setPlaying(false);
		if (pumpkin != null)
			pumpkin.remove();
		if (projectile != null)
			projectile.remove();
		
		for (Entity e : toRemove)
			if (e != null)
				e.remove();
	}
	@Override
	public void update(FileConfiguration file) {
	}
	private void spawnSoulBomb(Location loc, LivingEntity target, Vector vec, boolean knockback) {
		double[] range = {6.0};
		int[] life = {80};
		ArmorStand stand = Utils.lockArmorStand((ArmorStand) loc.getWorld().spawnEntity(loc.clone().add(100,100,0), EntityType.ARMOR_STAND), true, false, true);
		stand.getEquipment().setHelmet(CustomHead.SOULPARTICLE.getHead());
		stand.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		toRemove.add(stand);
		stand.teleport(entity.getLocation());
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				if (--life[0] == 0 || stand == null || stand.isDead() || target == null || target.isDead() || !target.getWorld().equals(stand.getWorld())) {
					cancel();
					if (stand != null)
						stand.remove();
					return;
				}
				range[0] -= 0.05;
				Vector towards = Utils.getVectorTowards(stand.getLocation(), target.getLocation()).multiply(0.2);
				vec.add(towards).normalize().multiply(0.8);
				stand.teleport(stand.getLocation().add(vec).setDirection(towards));
				stand.setHeadPose(new EulerAngle(Math.toRadians(stand.getLocation().getPitch()), 0, 0));
				DustTransition dust = new DustTransition(Color.fromRGB(rand.nextInt(180), 217, 202), Color.BLACK, rand.nextFloat());
				for (int i=0; i < 3; i++)
					stand.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, stand.getEyeLocation().add(rand.nextDouble()/2-.25, 2.2+(rand.nextDouble()/2-.25), rand.nextDouble()/2-.25), 1, 0, 0, 0, 0.001, dust);
				if (life[0] % 5 == 0)
					stand.getWorld().playSound(stand.getLocation(), Sound.ENTITY_GHAST_AMBIENT, SoundCategory.HOSTILE, 1f, .5f);
				if (stand.getLocation().distanceSquared(target.getLocation()) < 1) {
					createSoulBombExplosion(stand.getLocation().add(0,1,0), range[0]);
					if (!Utils.isEntityImmunePlayer(target)) {
						Utils.pureDamageEntity(target, 4.0, "dd-soulbombdeath", false, entity, DamageCause.MAGIC);
						target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, true, false, false));
						if (knockback)
							target.setVelocity(Utils.getVectorTowards(loc, target.getLocation()).setY(0.6));
					}
					stand.getWorld().playSound(stand.getLocation(), Sound.ENTITY_GHAST_HURT, SoundCategory.HOSTILE, 2f, .5f);
					stand.remove();
					cancel();
					return;
				}
			}
		};
	}
	private void createSoulBombExplosion(Location loc, double range) {
		Vector rotation = new Vector(1, 0, 0);
		World world = loc.getWorld();
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			for (int i=0; i < 18; i++) {
				Vector angle = rotation.clone();
				for (int j=0; j < 18; j++) {
					Location dir = loc.clone().add(angle);
					Vector force = Utils.getVectorTowards(loc, dir).multiply(range/30.0);
					world.spawnParticle(Particle.CLOUD, dir, 0, force.getX(), force.getY(), force.getZ());
					angle.rotateAroundZ(Math.toRadians(20.0));
				}
				rotation.rotateAroundY(Math.toRadians(20.0));
			}
		});
		double[] distance = {0};
		new RepeatingTask(plugin, 5, 1) {
			@Override
			public void run() {
				distance[0] += range / 20.0;
				for (Entity e : world.getNearbyEntities(loc, distance[0], distance[0], distance[0], p -> !p.isDead() && !p.hasMetadata("dd-halloweenmobs"))) {
					double dist = e.getLocation().distance(loc);
					if (dist > distance[0])
						continue;
					if (e instanceof LivingEntity && !Utils.isEntityImmunePlayer(e)) {
						if (Utils.rayTraceEntityConeForSolid(e, loc)) {
							e.setVelocity(Utils.getVectorTowards(loc, e.getLocation().add(0,e.getHeight()/2.0,0)).multiply((0.05*range)*(1.0-(dist/range))));
							if (((LivingEntity) e).getNoDamageTicks() == 0)
								Utils.pureDamageEntity((LivingEntity) e, 2.0, "dd-soulbombdeath", false, entity, DamageCause.MAGIC);
							((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, true, false, false));
						}
					} else if (Utils.rayTraceForSolid(loc, e.getLocation()))
						e.setVelocity(Utils.getVectorTowards(loc, e.getLocation().add(0,e.getHeight()/2.0,0)).multiply((0.05*range)*(1.0-(dist/range))));
				}
				if (distance[0] >= range)
					cancel();
			}
		};
	}
	private void throwPumpkin(LivingEntity target, boolean breakBlocks) {
		pumpkin = Utils.lockArmorStand((ArmorStand) entity.getWorld().spawnEntity(entity.getLocation().add(100,100,0), EntityType.ARMOR_STAND), true, false, true);
		pumpkin.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		projectile = entity.getWorld().spawn(entity.getLocation().add(0,100,0), Slime.class, slime -> {
			slime.setSize(0);
			slime.setSilent(true);
			slime.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(100.0);
			slime.setHealth(100.0);
			slime.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(0.0);
			slime.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true, false));
			slime.teleport(entity);
			slime.teleport(slime.getLocation().add(0,1,0));
			slime.setGravity(false);
		});
		Location targetLoc = target.getLocation().add(0,target.getHeight()/2,0);
		Vector vec = Utils.getVectorTowards(projectile.getLocation(), targetLoc);
		vec.setX(vec.getX()/3);
		vec.setZ(vec.getZ()/3);
		vec.setY((targetLoc.distance(entity.getLocation())/13)+((targetLoc.getY()-entity.getLocation().getY())/30));
		entity.getEquipment().setItemInOffHand(new ItemStack(Material.AIR));
		entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true, false));
		if (plugin.mcVersion >= 1.16)
			entity.setInvisible(true);
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				if (pumpkin == null || pumpkin.isDead() || projectile == null || projectile.isDead() || entity == null) {
					if (pumpkin != null)
						pumpkin.remove();
					pumpkin = null;
					if (projectile != null)
						projectile.remove();
					if (entity != null && !entity.isDead()) {
						entity.getEquipment().setItemInOffHand(ItemsHandler.etherealLanternBoss);
						entity.removePotionEffect(PotionEffectType.INVISIBILITY);
						entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOGLIN_ANGRY, SoundCategory.HOSTILE, 2f, .5f);
						if (plugin.mcVersion >= 1.16)
							entity.setInvisible(false);
					}
					cancel();
					return;
				}
				projectile.teleport(projectile.getLocation().add(vec));
				pumpkin.teleport(projectile.getLocation().add(0,-2,0).setDirection(vec));
				entity.teleport(pumpkin.getLocation().add(0,3,0));
				entity.setFallDistance(0);
				double pivot = Math.abs(vec.getX());
				if (Math.abs(vec.getZ()) > pivot)
					pivot = Math.abs(vec.getZ());
				double angle = Math.toDegrees(Math.atan2(Math.abs(vec.getY()), pivot));
				if (vec.getY() >= 0)
					pumpkin.setHeadPose(new EulerAngle(Math.toRadians(360-angle), 0, 0));
				else
					pumpkin.setHeadPose(new EulerAngle(Math.toRadians(360+angle), 0, 0));
				pumpkin.getWorld().spawnParticle(VersionUtils.getBlockCrack(), pumpkin.getLocation().add(0,2,0), 2, .1, .1, .1, 1, Material.JACK_O_LANTERN.createBlockData());
				if (!projectile.getLocation().getBlock().isPassable()) {
					pumpkin.remove();
					projectile.remove();
					if (entity != null && !entity.isDead()) {
						entity.getEquipment().setItemInOffHand(ItemsHandler.etherealLanternBoss);
						entity.removePotionEffect(PotionEffectType.INVISIBILITY);
						entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOGLIN_ANGRY, SoundCategory.HOSTILE, 2f, .5f);
						if (plugin.mcVersion >= 1.16)
							entity.setInvisible(false);
					}
					pumpkin = null;
					projectile.getWorld().createExplosion(projectile.getLocation(), 6f, true, breakBlocks, entity);
					cancel();
					int[] timer = {20};
					Location explosion = projectile.getLocation();
					new RepeatingTask(plugin, 0, 2) {
						@Override
						public void run() {
							explosion.getWorld().spawnParticle(VersionUtils.getRedstoneDust(), explosion, 30, 4, 2, 4, .001, new DustOptions(Color.ORANGE, 1f));
							explosion.getWorld().spawnParticle(VersionUtils.getRedstoneDust(), explosion, 30, 4, 2, 4, .001, new DustOptions(Color.BLACK, 1f));
							if (--timer[0] <= 0)
								cancel();
						}
					};
					return;
				}
				vec.multiply(0.99);
				vec.setY(vec.getY() - 0.05000000074505806D);
				for (Entity e : projectile.getNearbyEntities(.5, .5, .5))
					if (e instanceof LivingEntity && !(e instanceof ArmorStand) && !e.equals(entity) && !(e instanceof Player && Utils.isPlayerImmune((Player) e))) {
						pumpkin.remove();
						projectile.remove();
						if (entity != null && !entity.isDead()) {
							entity.getEquipment().setItemInOffHand(ItemsHandler.etherealLanternBoss);
							entity.removePotionEffect(PotionEffectType.INVISIBILITY);
							entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOGLIN_ANGRY, SoundCategory.HOSTILE, 2f, .5f);
							if (plugin.mcVersion >= 1.16)
								entity.setInvisible(false);
						}
						pumpkin = null;
						projectile.getWorld().createExplosion(projectile.getLocation(), 4f, true, breakBlocks, entity);
						cancel();
						int[] timer = {20};
						Location explosion = projectile.getLocation();
						new RepeatingTask(plugin, 0, 2) {
							@Override
							public void run() {
								explosion.getWorld().spawnParticle(VersionUtils.getRedstoneDust(), explosion, 30, 4, 2, 4, .001, new DustOptions(Color.ORANGE, 1f));
								explosion.getWorld().spawnParticle(VersionUtils.getRedstoneDust(), explosion, 30, 4, 2, 4, .001, new DustOptions(Color.BLACK, 1f));
								if (--timer[0] <= 0)
									cancel();
							}
						};
						return;
					}
			}
		};
	}
	private void createBloodCircle(Block block, int life, int immuneFrames) {
		Location part = block.getLocation().add(.5,1,.5);
		World world = part.getWorld();
		world.playSound(part, Sound.ITEM_FIRECHARGE_USE, SoundCategory.HOSTILE, 1f, .5f);
		int[] timer = {life, immuneFrames};
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				for (int i=0; i < 50; i++) {
					double angle = Math.toRadians(360.0 / 50 * i);
					world.spawnParticle(Particle.FLAME, part.clone().add(Math.cos(angle)*2,0,Math.sin(angle)*2), 1, 0, 0, 0, 0.0001);
				}
				world.spawnParticle(Particle.LAVA, part, 4, .5, .05, .5, 0.1);

				for (int i = 0; i < 5; i++) {
					double angle = Math.toRadians(360.0 / 5 * i);
					double nextAngle = Math.toRadians(360.0 / 5 * (i + 2));
					double x = Math.cos(angle) * 2.5;
					double z = Math.sin(angle) * 2.5;
					double deltaX = (Math.cos(nextAngle) * 2.5) - x;
					double deltaZ = (Math.sin(nextAngle) * 2.5) - z;
					double distance = Math.sqrt((deltaX - x) * (deltaX - x) + (deltaZ - z) * (deltaZ));
					for (double d = 0; d < distance / 6.7; d += .05)
						world.spawnParticle(Particle.FLAME, part.clone().add(x + (deltaX * d), 0, z + (deltaZ * d)), 1, 0, 0, 0, 0.0001);
				}
				if (timer[1] > 0) {
					timer[1]--;
					return;
				}
				for (Entity e : world.getNearbyEntities(part, 1, 2, 1, p -> (p instanceof Player && !Utils.isPlayerImmune((Player) p)))) {
					createBloodWorm(part, (LivingEntity) e);
					cancel();
					return;
				}
				if (timer[0] % 15 == 0)
					world.playSound(part, Sound.BLOCK_FIRE_AMBIENT, SoundCategory.HOSTILE, 2f, .5f);
				if (--timer[0] <= 0) {
					cancel();
					return;
				}
			}
		};
	}
	private void createBloodWorm(Location startPos, LivingEntity target) {
		FallingBlock[] blocks = new FallingBlock[30];
		ArmorStand[] stands = new ArmorStand[16];
		int[] frame = {0, 0, 0, 0};
		double[] damageM = {0};
		Location loc = startPos.clone();
		BlockData bd = Material.REDSTONE_BLOCK.createBlockData();
		World world = loc.getWorld();
		EvokerFangs[] fangs = {(EvokerFangs) world.spawnEntity(target.getLocation().subtract(0,0.5,0), EntityType.EVOKER_FANGS)};
		Vector[] velocities = new Vector[60];
		boolean[] force = {true};
		world.playSound(startPos, Sound.ITEM_FIRECHARGE_USE, SoundCategory.HOSTILE, 1f, .5f);
		for (int i=0; i < 30; i++) {
			velocities[i] = new Vector(0.005, 0, 0.005);
			velocities[i+30] = velocities[i].clone().multiply(10);
		}
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				if (target.isDead())
					force[0] = false;
				for (int i=0; i < 30; i++)
					if (blocks[i] != null) {
						blocks[i].setVelocity(blocks[i].getVelocity().add(velocities[i]));
						if ((velocities[i+30].getX() > 0 && blocks[i].getVelocity().getX() >= velocities[i+30].getX()) || (velocities[i+30].getX() < 0 && blocks[i].getVelocity().getX() <= velocities[i+30].getX())) {
							velocities[i].multiply(-1);
							velocities[i+30].multiply(-1);
						}
					}
				for (int i=0; i < 16; i++)
					if (stands[i] != null) {
						stands[i].teleport(blocks[Math.max(0, i*2-1)].getLocation());
						stands[i].setHeadPose(stands[i].getHeadPose().add(Math.toRadians(rand.nextInt(6)-3), 0, 0));
					}
				if (frame[3]++ == 10) {
					frame[3] = 0;
					if (plugin.mcVersion >= 1.20)
						world.playSound(startPos, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.HOSTILE, 1f, .5f);
					world.playSound(startPos, Sound.BLOCK_FIRE_AMBIENT, SoundCategory.HOSTILE, 1f, .5f);
				}
				if (frame[1] <= 0) {
					for (int i=0; i < 30; i++)
						if (blocks[i] != null)
							blocks[i].setVelocity(blocks[i].getVelocity().setY(.32));
					if (frame[0] % 2 == 0)
						blocks[(int) (frame[0] / 2)] = createBlockForBloodWorm(loc.clone().subtract((rand.nextDouble()-0.5)/4, 0, (rand.nextDouble()-0.5)/4));
					if (frame[0] > 4 && frame[0] % 4 == 0 && rand.nextInt(2) == 0)
						stands[(int) (frame[0] / 4)] = createStandForBloodWorm(blocks[(int) (frame[0] / 2)].getLocation());
					frame[0]++;
					fangs[0].remove();
					fangs[0] = (EvokerFangs) world.spawnEntity(blocks[0].getLocation().clone().add(0,1.5,0), EntityType.EVOKER_FANGS);
					fangs[0].setSilent(true);
					damageM[0] += 100.0/60.0;
					if (blocks[29] != null || !force[0] || target.getLocation().add(0,2,0).getBlock().getType().isSolid()) {
						frame[1] = 1;
						fangs[0].setSilent(false);
						for (FallingBlock e : blocks)
							if (e != null)
								e.setVelocity(e.getVelocity().setY(0));
					}
					if (force[0] && !target.isDead() && !fangs[0].isDead() && !(target instanceof Player && Utils.isPlayerImmune((Player) target))) {
						if (!target.getWorld().equals(world) || target.getLocation().distanceSquared(fangs[0].getLocation()) >= 3) {
							target.teleport(fangs[0].getLocation());
							if (plugin.mcVersion >= 1.20 && rand.nextInt(3) == 0)
								world.playSound(target.getLocation().add(0,4,0), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.HOSTILE, 1f, .5f);
						}
						try {
							target.setVelocity(Utils.getVectorTowards(target.getLocation(), fangs[0].getLocation()).multiply(0.3).setY(fangs[0].getLocation().getY()-target.getLocation().getY()));
						} catch (IllegalArgumentException e) {
							return;
						}
					}
				} else {
					if (frame[0] <= 0) {
						if (frame[0] == -2) {
							fangs[0].remove();
							cancel();
							frame[2] = 1;
							return;
						}
						Location fangLoc = fangs[0].getLocation().subtract(0,.4,0);
						fangs[0].remove();
						fangs[0] = (EvokerFangs) world.spawnEntity(fangLoc, EntityType.EVOKER_FANGS);
						frame[0]--;
					} else if (frame[1] >= 40) {
						for (int i=0; i < 30; i++)
							if (blocks[i] != null)
								blocks[i].setVelocity(blocks[i].getVelocity().setY(-.4));
						if (blocks[0] != null) {
							fangs[0].remove();
							fangs[0] = (EvokerFangs) world.spawnEntity(blocks[0].getLocation().clone().add(0,1.5,0), EntityType.EVOKER_FANGS);
							fangs[0].setSilent(true);
						} else {
							Location fangLoc = fangs[0].getLocation().subtract(0,.4,0);
							fangs[0].remove();
							fangs[0] = (EvokerFangs) world.spawnEntity(fangLoc, EntityType.EVOKER_FANGS);
						}
						frame[0]--;
						if (frame[0] % 2 == 0) {
							if (blocks[(int) (frame[0] / 2)] != null) {
								blocks[(int) (frame[0] / 2)].remove();
								toRemove.remove(blocks[(int) (frame[0] / 2)]);
								blocks[(int) (frame[0] / 2)] = null;
							}
						}
						if (frame[0] % 4 == 0 && stands[(int) (frame[0] / 4)] != null) {
							stands[(int) (frame[0] / 4)].remove();
							toRemove.remove(stands[(int) (frame[0] / 4)]);
							stands[(int) (frame[0] / 4)] = null;
						}
						if (force[0] && !target.isDead() && !fangs[0].isDead() && !(target instanceof Player && Utils.isPlayerImmune((Player) target))) {
							if (!target.getWorld().equals(world) || target.getLocation().distanceSquared(fangs[0].getLocation()) >= 3) {
								target.teleport(fangs[0].getLocation());
								if (plugin.mcVersion >= 1.20 && rand.nextInt(3) == 0)
									world.playSound(target.getLocation().subtract(0,1,0), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.HOSTILE, 1f, .5f);
							}
							target.setVelocity(Utils.getVectorTowards(target.getLocation(), fangs[0].getLocation().subtract(0,2,0)).multiply(0.3).setY(fangs[0].getLocation().getY()-0.5-target.getLocation().getY()));
							target.setFallDistance(0);
						}
					} else if (frame[1] <= 25) {
						fangs[0].remove();
						if (frame[1] != 25) {
							fangs[0] = (EvokerFangs) world.spawnEntity(blocks[0].getLocation().clone().add(0,1.5,0), EntityType.EVOKER_FANGS);
							fangs[0].setSilent(true);
						} else {
							fangs[0] = (EvokerFangs) world.spawnEntity(blocks[0].getLocation().clone().add(0,1.0,0), EntityType.EVOKER_FANGS);
							fangs[0].setMetadata("dd-pumpkinkingbloodfang", plugin.fixedData);
						}
						if (force[0] && !target.isDead() && !fangs[0].isDead() && !(target instanceof Player && Utils.isPlayerImmune((Player) target))) {
							if (!target.getWorld().equals(world) || target.getLocation().distanceSquared(fangs[0].getLocation()) >= 3)
								target.teleport(fangs[0].getLocation());
							target.setVelocity(Utils.getVectorTowards(target.getLocation(), fangs[0].getLocation().subtract(0,2,0)).multiply(0.3).setY(fangs[0].getLocation().getY()-target.getLocation().getY()));
						}
					} else {
						if (force[0] && !target.isDead() && !fangs[0].isDead() && !(target instanceof Player && Utils.isPlayerImmune((Player) target))) {
							if (!target.getWorld().equals(world) || target.getLocation().distanceSquared(fangs[0].getLocation()) >= 3)
								target.teleport(fangs[0].getLocation());
							target.setVelocity(Utils.getVectorTowards(target.getLocation(), fangs[0].getLocation().subtract(0,2,0)).multiply(0.3).setY(fangs[0].getLocation().getY()-target.getLocation().getY()));
						}
					}
					frame[1]++;
				}
				for (FallingBlock e : blocks)
					if (e != null)
						world.spawnParticle(VersionUtils.getBlockDust(), e.getLocation().clone().add(0,1.5,0), 1, .1, .1, .1, 0.1, bd);
			}
		};
		Location part = startPos.clone();
		new AsyncRepeatingTask(plugin, 1, 1) {
			@Override
			public void run() {
				if (frame[2] == 1) {
					cancel();
					return;
				}
				for (int i=0; i < 50; i++) {
					double angle = Math.toRadians(360.0 / 50 * i);
					world.spawnParticle(Particle.FLAME, part.clone().add(Math.cos(angle)*2,0,Math.sin(angle)*2), 1, 0, 0, 0, 0.0001);
				}
				world.spawnParticle(Particle.LAVA, part, 4, .5, .05, .5, 0.1);

				for (int i = 0; i < 5; i++) {
					double angle = Math.toRadians(360.0 / 5 * i);
					double nextAngle = Math.toRadians(360.0 / 5 * (i + 2));
					double x = Math.cos(angle) * 2.5;
					double z = Math.sin(angle) * 2.5;
					double deltaX = (Math.cos(nextAngle) * 2.5) - x;
					double deltaZ = (Math.sin(nextAngle) * 2.5) - z;
					double distance = Math.sqrt((deltaX - x) * (deltaX - x) + (deltaZ - z) * (deltaZ));
					for (double d = 0; d < distance / 6.7; d += .05)
						world.spawnParticle(Particle.FLAME, part.clone().add(x + (deltaX * d), 0, z + (deltaZ * d)), 1, 0, 0, 0, 0.0001);
				}
			}
		};
	}
	private FallingBlock createBlockForBloodWorm(Location loc) {
		FallingBlock fb = loc.getWorld().spawnFallingBlock(loc, Material.CHAIN.createBlockData());
		fb.setGravity(false);
		fb.setDropItem(false);
		fb.setMetadata("dd-fbcancel", plugin.fixedData);
		toRemove.add(fb);
		return fb;
	}
	private ArmorStand createStandForBloodWorm(Location loc) {
		loc.setYaw(rand.nextInt(360));
		ArmorStand e = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
		if (plugin.mcVersion >= 1.16)
			e.setInvisible(true);
		else
			e.setVisible(false);
		e.setGravity(false);
		e.setMarker(true);
		e.setSmall(true);
		if (plugin.mcVersion >= 1.16) {
			e.addEquipmentLock(EquipmentSlot.CHEST, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			e.addEquipmentLock(EquipmentSlot.FEET, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			e.addEquipmentLock(EquipmentSlot.HEAD, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			e.addEquipmentLock(EquipmentSlot.LEGS, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
		}
		e.getEquipment().setHelmet(CustomHead.BLOODWORMEYE.getHead());
		e.setHeadPose(new EulerAngle(Math.toRadians(rand.nextInt(180)), 0, 0));
		e.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		toRemove.add(e);
		return e;
	}
	public ArmorStand spawnTarget(Location loc) {
		ArmorStand newStand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
		if (plugin.mcVersion >= 1.16)
			newStand.setInvisible(true);
		else
			newStand.setVisible(false);
		newStand.setMarker(true);
		newStand.setGravity(false);
		newStand.setSmall(true);
		newStand.setCollidable(false);
		if (plugin.mcVersion >= 1.16) {
			newStand.addEquipmentLock(EquipmentSlot.CHEST, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			newStand.addEquipmentLock(EquipmentSlot.FEET, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			newStand.addEquipmentLock(EquipmentSlot.HAND, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			newStand.addEquipmentLock(EquipmentSlot.HEAD, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			newStand.addEquipmentLock(EquipmentSlot.LEGS, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			newStand.addEquipmentLock(EquipmentSlot.OFF_HAND, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
		}
		newStand.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		return newStand;
	}
	public static boolean summonPumpkinKing(Main plugin, Block block) {
		pumpkinBasketBlocks.add(block);
		World world = block.getWorld();
		Location spawn = Utils.findSmartYSpawn(block.getLocation(), Utils.getSpotInSquareRadius(block.getLocation(), 30), 2, 15);
		for (int i=0; i < 30; i++) {
			if (spawn != null && spawn.getBlock().isPassable())
				break;
			spawn = Utils.findSmartYSpawn(block.getLocation(), Utils.getSpotInSquareRadius(block.getLocation(), 30), 2, 15);
		}
		if (spawn == null)
			return false;
		for (Player p : world.getPlayers())
			p.sendMessage(Utils.convertString(ChatColor.RED+Languages.getString("halloween.deathParade")));
		WitherSkeleton king = (WitherSkeleton) world.spawnEntity(spawn.clone().add(150, 300, 0), EntityType.WITHER_SKELETON, false);
		king.setRemoveWhenFarAway(false);
		king.teleport(spawn);
		PumpkinKing kingObject = new PumpkinKing(king, plugin, plugin.random);
		CustomEntity.handler.addEntity(kingObject);
		if (plugin.noteBlockAPIEnabled) {
			SongPlayer music = new SongPlayer(NBSongs.HALLOWEEN_BOSS);
			for (Entity e : world.getNearbyEntities(block.getLocation(), 30, 30, 30))
				if (e instanceof Player)
					music.addPlayer((Player) e);
			music.setLooping(true);
			music.setVolume((byte) 30);
			music.setPlaying(true);
			kingObject.songPlayer = music;
		}
		ArmorStand target = kingObject.spawnTarget(block.getLocation().add(.5,.5,.5));
		kingObject.basketTarget = target;
		DeathParade parade = new DeathParade(5);
		parade.kingUUID = king.getUniqueId();
		kingObject.parade = parade;
		parade.start(spawn, null);
		boolean[] end = new boolean[1];
		end[0] = false;
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				if (end[0]) {
					cancel();
					return;
				}
				for (int i=0; i < 8; i++) {
					DustTransition dust = new DustTransition(Color.BLACK, Color.fromRGB(255, plugin.random.nextInt(40)+80, 0), plugin.random.nextFloat()/2f);
					world.spawnParticle(Particle.DUST_COLOR_TRANSITION, block.getLocation().add(.5,.3,.5).add(plugin.random.nextDouble()/1.5-.3,plugin.random.nextDouble()/1.5-.3,plugin.random.nextDouble()/1.5-.3), 1, 0, 0, 0, 0.001, dust);
				}
			}
		};
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
			pumpkinBasketBlocks.remove(block);
			block.setType(Material.AIR);
			world.spawnParticle(VersionUtils.getBlockCrack(), block.getLocation().add(.5,.3,.5), 30, .2, .2, .2, 0.0001, Material.ORANGE_WOOL.createBlockData());
			world.playSound(block.getLocation().add(.5,.3,.5), Sound.BLOCK_WOOL_BREAK, SoundCategory.BLOCKS, 1, 0.5f);
			king.setRemoveWhenFarAway(true);
			target.remove();
			end[0] = true;
		}, 300);
		return true;
	}
}
