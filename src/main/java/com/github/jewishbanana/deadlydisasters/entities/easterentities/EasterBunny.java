package com.github.jewishbanana.deadlydisasters.entities.easterentities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustTransition;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Rabbit.Type;
import org.bukkit.entity.Slime;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockVector;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.EntityHandler;
import com.github.jewishbanana.deadlydisasters.handlers.ItemsHandler;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.utils.DDSong;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.SongMaker;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class EasterBunny extends CustomEntity {
	
	private int cooldown = 10, leap = 7, eggLay = 15, jumpTicks = 15;
	private Random rand;
	private BossBar bar;
	public SongMaker songMaker;
	public LivingEntity target;
	private int biteTicks;
	
	private ArmorStand[] stands = new ArmorStand[5];
	private Slime[] slimes = new Slime[5];
	private List<KillerChicken> chickens = new ArrayList<>();
	
	public static Set<Block> easterBasketBlocks = new HashSet<>();
	
	public EasterBunny(Rabbit entity, Main plugin, Random rand) {
		super(entity, plugin);
		this.rand = rand;
		this.entityType = CustomEntityType.EASTERBUNNY;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		((Rabbit) entity).setRabbitType(Type.THE_KILLER_BUNNY);
		
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(200.0);
		entity.setHealth(200.0);
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.5);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40);
		
		entity.setMetadata("dd-easterbunny", plugin.fixedData);
		entity.setMetadata("dd-eastermobs", plugin.fixedData);
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.langFile.getString("easter.easterBunny"));
		
		bar = Bukkit.createBossBar(Utils.chat("&c"+Languages.langFile.getString("easter.easterBunny")), BarColor.RED, BarStyle.SOLID, BarFlag.DARKEN_SKY, BarFlag.CREATE_FOG);
	}

	@Override
	public void tick() {
		if (entity == null)
			return;
		bar.setProgress((1.0 / 200.0) * entity.getHealth());
		
		for (int i=0; i < 8; i++) {
			DustTransition dust = new DustTransition(Color.fromRGB(rand.nextInt(125)+25, 255, rand.nextInt(55)+25), Color.fromRGB(25, rand.nextInt(155)+100, 255), rand.nextFloat());
			if (rand.nextInt(2) == 0)
				dust = new DustTransition(Color.fromRGB(rand.nextInt(105)+150, 25, 255), Color.fromRGB(25, rand.nextInt(155)+100, 255), rand.nextFloat()/2f);
			entity.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, entity.getLocation().add(rand.nextDouble()-.5,0.3+(rand.nextDouble()/2-.25),rand.nextDouble()-.5), 1, 0, 0, 0, 0.001, dust);
		}
		if (biteTicks <= 0) {
			if (entity.getTarget() != null && entity.getTarget().getType() != EntityType.ARMOR_STAND && entity.getTarget().getWorld().equals(entity.getWorld())
				&& entity.getTarget().getLocation().distanceSquared(entity.getLocation()) <= 1.25) {
				biteTicks = 8;
				Utils.damageEntity(entity.getTarget(), 20.0, "dd-easterbunnybite", false, entity);
			}
		} else
			biteTicks--;
		if (jumpTicks-- <= 0 && entity.getVelocity().getY() > 0 && entity.getVelocity().getY() < 0.3) {
			jumpTicks = 15;
			entity.setVelocity(entity.getVelocity().multiply(1.5).setY(0.6));
		}
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (entity == null) {
			it.remove();
			clean();
			if (songMaker != null)
				songMaker.stopSong();
			return;
		}
		if (entity.isDead()) {
			if (plugin.getConfig().getBoolean("customentities.allow_custom_drops")) {
				Item item = entity.getWorld().dropItemNaturally(entity.getLocation(), ItemsHandler.goldenEasterEgg);
				item.setInvulnerable(true);
			}
			bar.removeAll();
			it.remove();
			clean();
			if (songMaker != null)
				songMaker.stopSong();
			return;
		}
		if (rand.nextInt(4) == 0)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_RABBIT_ATTACK, SoundCategory.HOSTILE, 1f, 0.5f);
		bar.removeAll();
		if (songMaker != null)
			songMaker.removeAll();
		for (Entity e : entity.getNearbyEntities(40, 40, 40))
			if (e instanceof Player) {
				bar.addPlayer((Player) e);
				if (songMaker != null)
					songMaker.addPlayer((Player) e);
			}
		if (entity.getTarget() == null && target != null && !target.isDead())
			entity.setTarget(target);
		if (eggLay > 0)
			eggLay--;
		else if (entity.getTarget() != null && entity.getTarget().getType() != EntityType.ARMOR_STAND) {
			eggLay = 20;
			new RepeatingTask(plugin, 0, 20) {
				@Override
				public void run() {
					if ((entity.getHealth() >= entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()/2.0 && eggLay <= 17) || eggLay <= 15 || entity == null || entity.isDead()
							|| chickens.size() >= 10) {
						cancel();
						return;
					}
					KillerChicken.createEgg(entity.getLocation(), plugin, target, chickens);
					entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_CHICKEN_EGG, SoundCategory.HOSTILE, 1f, 0f);
				}
			};
		}
		if (leap > 0)
			leap--;
		if (cooldown > 0) {
			if (cooldown >= 5 && leap <= 0 && entity.getTarget() != null && entity.getTarget().getWorld().equals(entity.getWorld())) {
				leap = 12;
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_RABBIT_JUMP, SoundCategory.HOSTILE, 20f, 0f);
				entity.setVelocity(Utils.getVectorTowards(entity.getLocation(), entity.getTarget().getLocation()).multiply(entity.getLocation().distance(entity.getTarget().getLocation())/10).setY(2));
				new RepeatingTask(plugin, 10, 1) {
					@Override
					public void run() {
						if (entity == null || entity.isDead()) {
							cancel();
							return;
						}
						if (entity.isOnGround()) {
							BlockVector block = new BlockVector(entity.getLocation().getBlockX(), entity.getLocation().getBlockY(), entity.getLocation().getBlockZ());
							Set<Material> flowers = new HashSet<>();
							flowers.addAll(Tag.FLOWERS.getValues());
							flowers.addAll(Tag.SMALL_FLOWERS.getValues());
							flowers.addAll(Tag.TALL_FLOWERS.getValues());
							flowers.addAll(Set.of(Material.GRASS, Material.TALL_GRASS));
							entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 2f, 0f);
							World world = entity.getWorld();
							for (int x = -4; x < 4; x++)
								for (int z = -4; z < 4; z++) {
									Vector position = block.clone().add(new Vector(x, 0, z));
									Block b = world.getBlockAt(position.toLocation(world));
									if (block.distance(position) <= 4) {
										if (b.isPassable()) {
											for (int i = 0; i < 3; i++) {
												b = b.getRelative(BlockFace.DOWN);
												if (!b.isPassable())
													break;
											}
											if (b.isPassable())
												continue;
											else
												b = b.getRelative(BlockFace.UP);
										} else {
											for (int i = 0; i < 3; i++) {
												b = b.getRelative(BlockFace.UP);
												if (b.isPassable())
													break;
											}
											if (!b.isPassable())
												continue;
										}
										world.spawnParticle(Particle.BLOCK_CRACK, b.getLocation().clone().add(0.5, 0.5, 0.5), 6, .3, .5, .3, 0.001, Material.COARSE_DIRT.createBlockData());
										if (flowers.contains(b.getType()) && !Utils.isZoneProtected(b.getLocation()) && !Utils.isBlockBlacklisted(b.getType()))
											b.breakNaturally(new ItemStack(Material.AIR));
										for (Entity e : world.getNearbyEntities(b.getLocation().clone().add(0.5, 0.5, 0.5), 0.5, 1, 0.5)) {
											if (e.equals(entity))
												continue;
											e.setVelocity(new Vector(e.getLocation().getX() - entity.getLocation().getX(), 0, e.getLocation().getZ() - entity.getLocation().getZ()).normalize().multiply(0.7).setY(1.5));
											if (e instanceof LivingEntity && !(e instanceof Player && Utils.isPlayerImmune((Player) e))) {
												Utils.damageEntity((LivingEntity) e, 15.0, "dd-easterbunny", false, entity);
											}
										}
									}
								}
							cancel();
						}
					}
				};
			}
			cooldown--;
			return;
		}
		if (entity.getTarget() != null && entity.getTarget().getType() != EntityType.ARMOR_STAND && entity.getTarget().getWorld().equals(entity.getWorld())
				&& entity.getTarget().getLocation().distanceSquared(entity.getLocation()) <= 325 && entity.getTarget().getLocation().distanceSquared(entity.getLocation()) >= 16) {
			cooldown = 10;
			entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 10, true, false));
			int[] index = {0,2};
			if (entity.getHealth() <= entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 2.0)
				index[1] = 4;
			new RepeatingTask(plugin, 0, 8) {
				@Override
				public void run() {
					stands[index[0]] = Utils.lockArmorStand((ArmorStand) entity.getWorld().spawnEntity(entity.getLocation().add(100,100,0), EntityType.ARMOR_STAND), true, false, true);
					stands[index[0]].getEquipment().setItemInMainHand(new ItemStack(Material.CARROT));
					stands[index[0]].setRightArmPose(new EulerAngle(Math.toRadians(270), 0, 0));
					stands[index[0]].getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
					slimes[index[0]] = (Slime) entity.getWorld().spawnEntity(entity.getLocation().add(0,100,0), EntityType.SLIME);
					slimes[index[0]].setSize(0);
					slimes[index[0]].setSilent(true);
					slimes[index[0]].getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(100.0);
					slimes[index[0]].setHealth(100.0);
					slimes[index[0]].getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(0.0);
					slimes[index[0]].addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true, false));
					slimes[index[0]].teleport(entity);
					slimes[index[0]].setVelocity(new Vector(0,1,0));
					
					Firework fire = (Firework) slimes[index[0]].getWorld().spawnEntity(slimes[index[0]].getLocation(), EntityType.FIREWORK);
					FireworkMeta meta = fire.getFireworkMeta();
					meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BURST).withColor(Color.fromRGB(rand.nextInt(125)+25, 255, rand.nextInt(55)+25), Color.fromRGB(25, rand.nextInt(155)+100, 255), Color.fromRGB(rand.nextInt(105)+150, 25, 255)).build());
					fire.setFireworkMeta(meta);
					fire.detonate();
					
					index[0]++;
					if (index[0] > index[1]) {
						cancel();
						return;
					}
				}
			};
			new RepeatingTask(plugin, 0, 1) {
				@Override
				public void run() {
					if (index[0] >= index[1]) {
						int c = 0;
						for (int i=0; i < index[0]; i++)
							if (stands[i] == null)
								c++;
						if (c >= index[1]+1) {
							cancel();
							return;
						}
					}
					for (int i=0; i < index[0]; i++) {
						if (stands[i] == null || stands[i].isDead() || slimes[i] == null || slimes[i].isDead() || entity == null) {
							if (stands[i] != null)
								stands[i].remove();
							if (slimes[i] != null)
								slimes[i].remove();
							stands[i] = null;
							continue;
						}
						if (entity.getTarget() != null && entity.getTarget().getWorld().equals(entity.getWorld()))
							slimes[i].setVelocity(slimes[i].getVelocity().add(Utils.getVectorTowards(slimes[i].getLocation(), entity.getTarget().getLocation().add(0,entity.getTarget().getHeight()/2,0)).multiply(0.1)));
						stands[i].teleport(slimes[i].getLocation().add(0,-1,0));
						Vector vec = slimes[i].getVelocity();
						double pivot = Math.abs(vec.getX());
						if (Math.abs(vec.getZ()) > pivot)
							pivot = Math.abs(vec.getZ());
						double angle = Math.toDegrees(Math.atan2(Math.abs(vec.getY()), pivot));
						if (vec.getY() >= 0)
							stands[i].setRightArmPose(new EulerAngle(Math.toRadians(360-angle), 0, 0));
						else
							stands[i].setRightArmPose(new EulerAngle(Math.toRadians(360+angle), 0, 0));
						for (int p=0; p < 3; p++) {
							DustTransition dust = new DustTransition(Color.fromRGB(plugin.random.nextInt(125)+25, 255, plugin.random.nextInt(55)+25), Color.fromRGB(25, plugin.random.nextInt(155)+100, 255), plugin.random.nextFloat());
							if (plugin.random.nextInt(2) == 0)
								dust = new DustTransition(Color.fromRGB(plugin.random.nextInt(105)+150, 25, 255), Color.fromRGB(25, plugin.random.nextInt(155)+100, 255), plugin.random.nextFloat()/2f);
							slimes[i].getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, slimes[i].getLocation().add(plugin.random.nextDouble()/2.5-.2,plugin.random.nextDouble()/2.5-.2,plugin.random.nextDouble()/2.5-.2), 1, 0, 0, 0, 0.001, dust);
						}
						if (slimes[i].isOnGround()) {
							slimes[i].getWorld().createExplosion(slimes[i].getLocation(), (entity.getHealth() <= entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 2.0 ? 2.5f : 1.5f), true, true, entity);
							stands[i].remove();
							slimes[i].remove();
							stands[i] = null;
							continue;
						}
						for (Entity e : slimes[i].getNearbyEntities(.5, .5, .5))
							if (e instanceof LivingEntity && !e.equals(entity) && !(e instanceof Player && Utils.isPlayerImmune((Player) e))) {
								slimes[i].getWorld().createExplosion(slimes[i].getLocation(), (entity.getHealth() <= entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 2.0 ? 2.5f : 1.5f), true, true, entity);
								stands[i].remove();
								slimes[i].remove();
								stands[i] = null;
								break;
							}
					}
				}
			};
		}
	}
	@Override
	public void clean() {
		for (int i=0; i < 3; i++) {
			if (stands[i] != null)
				stands[i].remove();
			if (slimes[i] != null)
				slimes[i].remove();
		}
		bar.removeAll();
	}
	@Override
	public void update(FileConfiguration file) {
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
	public static boolean summonEasterBunny(Main plugin, Block block) {
		easterBasketBlocks.add(block);
		World world = block.getWorld();
		Location spawn = Utils.findSmartYSpawn(block.getLocation(), Utils.getSpotInSquareRadius(block.getLocation(), 30), 2, 15);
		for (int i=0; i < 30; i++) {
			if (spawn != null && spawn.getBlock().isPassable())
				break;
			spawn = Utils.findSmartYSpawn(block.getLocation(), Utils.getSpotInSquareRadius(block.getLocation(), 30), 2, 15);
		}
		if (spawn == null)
			return false;
		Rabbit bunny = (Rabbit) world.spawnEntity(spawn.clone().add(150, 300, 0), EntityType.RABBIT, false);
		bunny.setRemoveWhenFarAway(false);
		bunny.teleport(spawn);
		EasterBunny bunnyObject = new EasterBunny(bunny, plugin, plugin.random);
		plugin.handler.addEntity(bunnyObject);
		SongMaker music = new SongMaker(DDSong.EASTER_THEME);
		for (Entity e : world.getNearbyEntities(block.getLocation(), 30, 30, 30))
			if (e instanceof Player)
				music.addPlayer((Player) e);
		music.setLoop(true);
		music.setVolume(0.5f);
		music.playSong();
		bunnyObject.songMaker = music;
		ArmorStand target = bunnyObject.spawnTarget(block.getLocation().add(.5,.5,.5));
		bunnyObject.target = target;
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
					DustTransition dust = new DustTransition(Color.fromRGB(plugin.random.nextInt(125)+25, 255, plugin.random.nextInt(55)+25), Color.fromRGB(25, plugin.random.nextInt(155)+100, 255), plugin.random.nextFloat());
					if (plugin.random.nextInt(2) == 0)
						dust = new DustTransition(Color.fromRGB(plugin.random.nextInt(105)+150, 25, 255), Color.fromRGB(25, plugin.random.nextInt(155)+100, 255), plugin.random.nextFloat()/2f);
					world.spawnParticle(Particle.DUST_COLOR_TRANSITION, block.getLocation().add(.5,.3,.5).add(plugin.random.nextDouble()/1.5-.3,plugin.random.nextDouble()/1.5-.3,plugin.random.nextDouble()/1.5-.3), 1, 0, 0, 0, 0.001, dust);
				}
			}
		};
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
			easterBasketBlocks.remove(block);
			block.setType(Material.AIR);
			world.spawnParticle(Particle.BLOCK_CRACK, block.getLocation().add(.5,.3,.5), 30, .2, .2, .2, 0.0001, Material.BLUE_WOOL.createBlockData());
			world.playSound(block.getLocation().add(.5,.3,.5), Sound.BLOCK_WOOL_BREAK, SoundCategory.BLOCKS, 1, 0.5f);
			bunny.setRemoveWhenFarAway(true);
			target.remove();
			end[0] = true;
		}, 300);
		return true;
	}
}
