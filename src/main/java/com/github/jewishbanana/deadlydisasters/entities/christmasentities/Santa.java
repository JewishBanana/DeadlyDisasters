package com.github.jewishbanana.deadlydisasters.entities.christmasentities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Item;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.CustomHead;
import com.github.jewishbanana.deadlydisasters.entities.EntityHandler;
import com.github.jewishbanana.deadlydisasters.handlers.ItemsHandler;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.utils.DDSong;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.SongMaker;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class Santa extends CustomEntity {
	
	private int cooldown = 5;
	private Random rand;
	private Present[] presents = new Present[3];
	private BossBar bar;
	public SongMaker songMaker;
	
	public static Set<Block> snowGlobeBlocks = new HashSet<>();
	
	@SuppressWarnings("deprecation")
	public Santa(Zombie entity, Main plugin, Random rand) {
		super(entity, plugin);
		this.rand = rand;
		this.entityType = CustomEntityType.SANTA;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		if (plugin.mcVersion >= 1.16)
			entity.setAdult();
		else
			entity.setBaby(false);
		
		ItemStack[] armor = {new ItemStack(Material.LEATHER_BOOTS), new ItemStack(Material.LEATHER_LEGGINGS), new ItemStack(Material.LEATHER_CHESTPLATE), CustomHead.SANTA.getHead()};
		for (int i=0; i < 3; i++) {
			LeatherArmorMeta meta = (LeatherArmorMeta) armor[i].getItemMeta();
			meta.setColor(Color.fromBGR(0, 0, 220));
			armor[i].setItemMeta(meta);
		}
		entity.getEquipment().setArmorContents(armor);
		entity.getEquipment().setHelmetDropChance(0);
		entity.getEquipment().setChestplateDropChance(0);
		entity.getEquipment().setLeggingsDropChance(0);
		entity.getEquipment().setBootsDropChance(0);
		entity.setCanPickupItems(false);
		entity.setSilent(true);
		
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(400.0);
		entity.setHealth(400.0);
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(20.0);
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.3);
		
		entity.setMetadata("dd-santa", plugin.fixedData);
		entity.setMetadata("dd-christmasmob", plugin.fixedData);
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.langFile.getString("entities.santa"));
		
		bar = Bukkit.createBossBar(Utils.chat("&c"+Languages.langFile.getString("entities.santa")), BarColor.RED, BarStyle.SOLID, BarFlag.DARKEN_SKY, BarFlag.CREATE_FOG);
	}

	@Override
	public void tick() {
		if (entity == null)
			return;
		bar.setProgress((1.0 / 400.0) * entity.getHealth());
		if (entity.getNoDamageTicks() == 20)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_VILLAGER_HURT, SoundCategory.HOSTILE, 2, 0.6f);
		entity.getWorld().spawnParticle(Particle.FALLING_DUST, entity.getLocation().add(0,1.2,0), 1, .3, .5, .3, 1, Material.SNOW.createBlockData());
		entity.getWorld().spawnParticle(Particle.FALLING_DUST, entity.getLocation().add(0,1.2,0), 1, .3, .5, .3, 1, Material.RED_WOOL.createBlockData());
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
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_VILLAGER_DEATH, SoundCategory.HOSTILE, 2, 0.5f);
			if (plugin.getConfig().getBoolean("customentities.allow_custom_drops")) {
				Item item = entity.getWorld().dropItemNaturally(entity.getLocation(), ItemsHandler.santaHat);
				item.setInvulnerable(true);
			}
			bar.removeAll();
			it.remove();
			clean();
			if (songMaker != null)
				songMaker.stopSong();
			return;
		}
		if (rand.nextInt(3) == 0)
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, SoundCategory.HOSTILE, 2, 0.5f);
		bar.removeAll();
		if (songMaker != null)
			songMaker.removeAll();
		for (Entity e : entity.getNearbyEntities(40, 40, 40))
			if (e instanceof Player) {
				bar.addPlayer((Player) e);
				if (songMaker != null)
					songMaker.addPlayer((Player) e);
			}
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		if (entity.getTarget() != null && entity.getTarget().getWorld().equals(entity.getWorld()) && entity.getTarget().getLocation().distanceSquared(entity.getLocation()) <= 225) {
			cooldown = 15;
			entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 10, true, false));
			entity.swingMainHand();
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SNOW_GOLEM_SHOOT, SoundCategory.HOSTILE, 2, 0.5f);
			clean();
			int lag = 5, count = 0;
			for (Entity e : entity.getNearbyEntities(30, 30, 30))
				if (e.hasMetadata("dd-christmasmob"))
					count++;
			if (count >= 9)
				lag = 2;
			presents[0] = new Present(rand.nextInt(lag), new Vector(0.5, 0.8, 0.0));
			presents[1] = new Present(rand.nextInt(lag), new Vector(-0.5, 0.8, 0.5));
			presents[2] = new Present(rand.nextInt(lag), new Vector(-0.5, 0.8, -0.5));
		}
	}
	@Override
	public void clean() {
		for (int i=0; i < 3; i++) {
			if (presents[i] == null)
				continue;
			if (presents[i].stand != null)
				presents[i].stand.remove();
			if (presents[i].projectile != null)
				presents[i].projectile.remove();
		}
		bar.removeAll();
	}
	@Override
	public void update(FileConfiguration file) {
	}
	class Present {
		private ArmorStand stand;
		private Slime projectile;
		private BlockData trail;
		
		public Present(int type, Vector direction) {
			stand = Utils.lockArmorStand((ArmorStand) entity.getWorld().spawnEntity(entity.getLocation().add(100,100,0), EntityType.ARMOR_STAND), true, false, true);
			stand.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
			switch (type) {
			case 4:
				stand.getEquipment().setHelmet(CustomHead.GGIFT.getHead());
				trail = Material.LIME_WOOL.createBlockData();
				break;
			case 3:
				stand.getEquipment().setHelmet(CustomHead.BGIFT.getHead());
				trail = Material.LIGHT_BLUE_WOOL.createBlockData();
				break;
			case 2:
				stand.getEquipment().setHelmet(CustomHead.YGIFT.getHead());
				trail = Material.YELLOW_WOOL.createBlockData();
				break;
			case 1:
				stand.getEquipment().setHelmet(CustomHead.PGIFT.getHead());
				trail = Material.PURPLE_WOOL.createBlockData();
				break;
			default:
			case 0:
				stand.getEquipment().setHelmet(CustomHead.WGIFT.getHead());
				trail = Material.RED_WOOL.createBlockData();
				break;
			}
			projectile = (Slime) entity.getWorld().spawnEntity(entity.getLocation().add(0,100,0), EntityType.SLIME);
			projectile.setSize(0);
			projectile.setSilent(true);
			projectile.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(100.0);
			projectile.setHealth(100.0);
			projectile.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(0.0);
			projectile.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true, false));
			projectile.teleport(entity);
			projectile.teleport(projectile.getLocation().add(0,1,0));
			projectile.setVelocity(direction);
			new RepeatingTask(plugin, 0, 1) {
				@Override
				public void run() {
					if (stand == null || stand.isDead() || projectile == null || projectile.isDead() || entity == null) {
						if (stand != null)
							stand.remove();
						if (projectile != null)
							projectile.remove();
						cancel();
						return;
					}
					stand.teleport(projectile.getLocation().add(0,-2,0));
					stand.getWorld().spawnParticle(Particle.BLOCK_CRACK, stand.getLocation().add(0,2,0), 2, .1, .1, .1, 1, trail);
					if (projectile.isOnGround()) {
						stand.getWorld().spawnParticle(Particle.BLOCK_CRACK, stand.getLocation().add(0,2,0), 30, .7, .7, .7, 1, trail);
						switch (type) {
						case 4:
							Mob grinch = (Mob) projectile.getWorld().spawnEntity(projectile.getLocation(), EntityType.ZOMBIE);
							plugin.handler.addEntity(new Grinch(grinch, plugin, rand));
							if (entity.getTarget() != null && !entity.getTarget().isDead())
								grinch.setTarget(entity.getTarget());
							break;
						case 3:
							Snowman frosty = (Snowman) projectile.getWorld().spawnEntity(projectile.getLocation(), EntityType.SNOWMAN);
							plugin.handler.addEntity(new Frosty(frosty, plugin, rand));
							if (entity.getTarget() != null && !entity.getTarget().isDead())
								frosty.setTarget(entity.getTarget());
							break;
						case 2:
							for (int i=0; i < 3; i++) {
								Zombie elf = (Zombie) projectile.getWorld().spawnEntity(projectile.getLocation(), EntityType.ZOMBIE);
								plugin.handler.addEntity(new Elf(elf, plugin, rand));
								if (entity.getTarget() != null && !entity.getTarget().isDead())
									elf.setTarget(entity.getTarget());
							}
							break;
						case 1:
							createIceSpell(projectile.getLocation(), 5);
							break;
						default:
						case 0:
							projectile.getWorld().createExplosion(projectile.getLocation(), 3.5f, true, true);
							break;
						}
						stand.remove();
						projectile.remove();
						cancel();
						return;
					}
				}
			};
		}
		private void createIceSpell(Location loc, int radius) {
			World world = loc.getWorld();
			Map<Block,BlockState> patches = new HashMap<>();
			int[] timer = {0, 0};
			new RepeatingTask(plugin, 0, 1) {
				@Override
				public void run() {
					if (timer[1] % 2 == 0 && timer[0] <= radius) {
						BlockVector block = new BlockVector(loc.getX(), loc.getY(), loc.getZ());
						for (int x = -timer[0]; x < timer[0]; x++)
							for (int z = -timer[0]; z < timer[0]; z++) {
								Vector position = block.clone().add(new Vector(x, 0, z));
								double dist = block.distance(position);
								if (dist > timer[0] || dist < timer[0]-1)
									continue;
								Block b = Utils.getHighestExposedBlock(position.toLocation(loc.getWorld()), 3);
								if (b == null || patches.containsKey(b) || Utils.isBlockBlacklisted(b.getType()) || Utils.isZoneProtected(b.getLocation()))
									continue;
								patches.put(b, b.getState());
								b.setType(Material.PACKED_ICE);
								world.spawnParticle(Particle.BLOCK_CRACK, b.getLocation().add(.5,1,.5), 5, .4, .3, .4, 1, Material.PACKED_ICE.createBlockData());
							}
						if (plugin.mcVersion >= 1.17)
							world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.BLOCKS, 0.6f, .5f);
						timer[0]++;
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
	public static void summonSanta(Main plugin, Block block) {
		snowGlobeBlocks.add(block);
		World world = block.getWorld();
		Location spawn = new Location(world, block.getLocation().getBlockX()+(plugin.random.nextInt(16)-8), Math.max(150, block.getLocation().getY()+50), block.getLocation().getBlockZ()-150);
		Boat boat = (Boat) world.spawnEntity(spawn.clone().add(150, 300, 0), EntityType.BOAT);
		boat.teleport(spawn);
		boat.setInvulnerable(true);
		Zombie santa = (Zombie) world.spawnEntity(spawn.clone().add(150, 300, 0), EntityType.ZOMBIE, false);
		santa.setRemoveWhenFarAway(false);
		santa.teleport(spawn.clone().add(0, 0, -9.5));
		Santa santaObject = new Santa(santa, plugin, plugin.random);
		plugin.handler.addEntity(santaObject);
		Location[] deerSpawns = {spawn.clone().add(1, 0, -7), spawn.clone().add(-1, 0, -7), spawn.clone().add(1, 0, -2), spawn.clone().add(-1, 0, -2)};
		Location outside = spawn.clone().add(150, 300, 0);
		Horse[] deer = {(Horse) world.spawnEntity(outside, EntityType.HORSE, false), (Horse) world.spawnEntity(outside, EntityType.HORSE, false),
				(Horse) world.spawnEntity(outside, EntityType.HORSE, false), (Horse) world.spawnEntity(outside, EntityType.HORSE, false)};
		for (int i=0; i < 4; i++) {
			deer[i].teleport(deerSpawns[i]);
			deer[i].setLeashHolder(boat);
			deer[i].setColor(Horse.Color.BROWN);
		}
		boolean[] dismounted = {false};
		SongMaker music = new SongMaker(DDSong.CHRISTMAS_THEME);
		for (Entity e : world.getNearbyEntities(block.getLocation(), 30, 30, 30))
			if (e instanceof Player)
				music.addPlayer((Player) e);
		music.setLoop(true);
		music.setVolume(0.5f);
		music.playSong();
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				world.spawnParticle(Particle.SNOWFLAKE, block.getLocation().add(.5,.3,.5), 1, .2, .2, .2, 0.0001);
				if (!dismounted[0] && boat.getLocation().getZ() >= block.getLocation().getZ()) {
					santa.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 80, 100, true, false));
					dismounted[0] = true;
					santaObject.songMaker = music;
				} else if (boat == null || boat.isDead() || boat.getLocation().getZ() >= block.getLocation().getZ()+150) {
					for (Horse horse : deer)
						if (horse != null)
							horse.remove();
					if (boat != null)
						boat.remove();
					cancel();
					block.setType(Material.AIR);
					world.spawnParticle(Particle.BLOCK_CRACK, block.getLocation().add(.5,.3,.5), 30, .2, .2, .2, 0.0001, Material.SNOW.createBlockData());
					world.playSound(block.getLocation().add(.5,.3,.5), Sound.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1, 0.5f);
					snowGlobeBlocks.remove(block);
					santa.setRemoveWhenFarAway(true);
					return;
				}
				Vector vec = new Vector(0, 0, 1);
				boat.eject();
				for (Horse horse : deer)
					if (horse != null)
						horse.setVelocity(vec);
				boat.setVelocity(vec.clone().multiply(1.1).setY(0.04));
				if (!dismounted[0])
					santa.setVelocity(vec);
			}
		};
	}
}
