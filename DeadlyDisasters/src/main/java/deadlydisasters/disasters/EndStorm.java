package deadlydisasters.disasters;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import deadlydisasters.disasters.events.WeatherDisaster;
import deadlydisasters.disasters.events.WeatherDisasterEvent;
import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.EntityHandler;
import deadlydisasters.entities.endstormentities.BabyEndTotem;
import deadlydisasters.entities.endstormentities.EndTotem;
import deadlydisasters.entities.endstormentities.EndWorm;
import deadlydisasters.entities.endstormentities.VoidArcher;
import deadlydisasters.entities.endstormentities.VoidGuardian;
import deadlydisasters.entities.endstormentities.VoidStalker;
import deadlydisasters.general.Main;
import deadlydisasters.general.WorldObject;
import deadlydisasters.listeners.DeathMessages;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public class EndStorm extends WeatherDisaster {
	
	private int range,maxEntities;
	private Random rand = new Random();
	private EntityHandler handler;
	
	public Queue<CustomEntity> entities = new ArrayDeque<>();
	public Set<UUID> mobs = new HashSet<>();
	private Map<UUID,UUID> targets = new HashMap<>();

	public EndStorm(int level) {
		super(level);
		this.handler = plugin.handler;
		if (level > 5) level = 5;
		time = plugin.getConfig().getInt("endstorm.time.level "+this.level) * 20;
		delay = plugin.getConfig().getInt("endstorm.start_delay") * 20;
		this.range = plugin.getConfig().getInt("endstorm.max_tp_range");
		this.maxEntities = plugin.getConfig().getInt("endstorm.max_rift_entities");
		volume = plugin.getConfig().getDouble("endstorm.volume");
		
		this.type = Disaster.ENDSTORM;
	}
	public void start(World world, Player p, boolean broadcastAllowed) {
		WeatherDisasterEvent event = new WeatherDisasterEvent(this, world, level, p);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		this.world = world;
		updateWeatherSettings();
		if (broadcastAllowed && (boolean) WorldObject.findWorldObject(world).settings.get("event_broadcast"))
			Utils.broadcastEvent(level, "weather", this.type, world);
		DeathMessages.endstorms.add(this);
		new RepeatingTask(plugin, delay, 20) {
			@Override
			public void run() {
				if (time <= 0) {
					cancel();
					return;
				}
				time -= 20;
				for (LivingEntity e : world.getLivingEntities()) {
					if (mobs.contains(e.getUniqueId()) && ((Mob) e).getTarget() == null && Bukkit.getEntity(targets.get(e.getUniqueId())) != null)
						((Mob) e).setTarget((LivingEntity) Bukkit.getEntity(targets.get(e.getUniqueId())));
					if (e instanceof Enderman || e instanceof Endermite || e instanceof EnderDragon || e instanceof ArmorStand)
						continue;
					if (e instanceof Player && (((Player) e).getGameMode() == GameMode.CREATIVE || ((Player) e).getGameMode() == GameMode.SPECTATOR))
						continue;
					if (Utils.isZoneProtected(e.getLocation())) continue;
					if (rand.nextInt(20) == 1 && !e.hasMetadata("dd-endstormentity") && !(e instanceof ArmorStand)) {
						for (int i=0; i < 3; i++) {
							int x = (int) (e.getLocation().getX()+(rand.nextInt(range*2)-range));
							int z = (int) (e.getLocation().getZ()+(rand.nextInt(range*2)-range));
							Location temp = e.getLocation().clone();
							temp.setX(x);
							temp.setY(world.getHighestBlockYAt(x, z)+1);
							temp.setZ(z);
							if (e.getLocation().distance(temp) > range) continue;
							e.teleport(temp);
							world.playSound(e.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1, 1);
							if (e instanceof Player)
								((Player) e).spawnParticle(Particle.DRAGON_BREATH, e.getLocation().add(0,1.5,0), 30, 3, 1, 3, 3);
							break;
						}
					}
					if (e instanceof Player) {
						if (rand.nextInt(20) == 0) {
							int x = (int) (e.getLocation().getX()+(rand.nextInt(14)-7));
							int z = (int) (e.getLocation().getZ()+(rand.nextInt(14)-7));
							if (e.getLocation().distance(world.getHighestBlockAt(x, z).getLocation()) <= 10)
								createRift(world.getHighestBlockAt(x, z).getLocation().add(0,3,0), (Player) e);
						}
					}
				}
			}
		};
		new RepeatingTask(plugin, delay, 200) {
			@Override
			public void run() {
				if (time <= 0) {
					cancel();
					return;
				}
				for (Player p : world.getPlayers()) {
					if (Utils.isZoneProtected(p.getLocation()) || p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR)
						continue;
					if (plugin.mcVersion >= 1.16) {
						p.stopSound(Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP);
						p.playSound(p.getLocation(), Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP, SoundCategory.AMBIENT, (float) (2*volume), 2);
					}
				}
			}
		};
		new RepeatingTask(plugin, delay, 1) {
			@Override
			public void run() {
				if (time <= 0) {
					clear();
					cancel();
					return;
				}
				for (Player p : world.getPlayers()) {
					if (Utils.isZoneProtected(p.getLocation()) || p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR)
						continue;
					p.spawnParticle(Particle.DRAGON_BREATH, p.getLocation().add(0,1.5,0), 25, 3, 1, 3, 3);
					p.spawnParticle(Particle.SMOKE_LARGE, p.getLocation().add(0,1.5,0), 10, 3, 1, 3, 1.5);
				}
			}
		};
	}
	public void createRift(Location loc, Player player) {
		int[] var = {40, rand.nextInt(4)+1};
		Iterator<CustomEntity> iterator = entities.iterator();
		while (iterator.hasNext()) {
			CustomEntity e = iterator.next();
			if (e.getEntity() == null || e.getEntity().isDead()) {
				e.clean();
				iterator.remove();
			}
		}
		boolean custom = plugin.getConfig().getBoolean("customentities.allow_custom_mobs");
		ItemStack[] armor = {new ItemStack(Material.LEATHER_BOOTS), new ItemStack(Material.LEATHER_LEGGINGS), new ItemStack(Material.LEATHER_CHESTPLATE), new ItemStack(Material.LEATHER_HELMET)};
		if (!custom) {
			for (int i=0; i < 3; i++) {
				LeatherArmorMeta meta = (LeatherArmorMeta) armor[i].getItemMeta();
				meta.setColor(Color.fromBGR(50, 50, 50));
				armor[i].setItemMeta(meta);
			}
		}
		new RepeatingTask(plugin, 0, 5) {
			@Override
			public void run() {
				world.spawnParticle(Particle.PORTAL, loc, 20, .2, .2, .2, 1.5);
				world.spawnParticle(Particle.SQUID_INK, loc.clone().add(0,0.5,0), 30, .25, .25, .25, 0.0001);
				world.playSound(loc, Sound.BLOCK_PORTAL_AMBIENT, SoundCategory.AMBIENT, .7f, 1);
				for (Entity e : world.getNearbyEntities(loc, .5, .5, .5))
					if (e instanceof Player && (((Player) e).getGameMode() == GameMode.SURVIVAL || ((Player) e).getGameMode() == GameMode.ADVENTURE))
						Utils.pureDamageEntity((LivingEntity) e, 1, "dd-unstablerift", true);
				if (var[0] > 0)
					var[0]-=5;
				else {
					if (entities.size() >= maxEntities) {
						cancel();
						return;
					}
					var[0] = 40;
					Mob entity = null;
					CustomEntity ce = null;
					if (!custom) {
						entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
						entity.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
						entity.getEquipment().setItemInOffHand(new ItemStack(Material.SHIELD));
						entity.getEquipment().setArmorContents(armor);
						targets.put(entity.getUniqueId(), player.getUniqueId());
						mobs.add(entity.getUniqueId());
						
						var[1]--;
						if (var[1] <= 0) cancel();
						return;
					}
					int num = rand.nextInt(100);
					if (num < 15) {
						entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.WITHER_SKELETON);
						entity.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
						ce = new EndTotem(entity, plugin, rand);
					} else if (num < 35) {
						entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
						ce = new EndWorm(entity, plugin, rand);
					} else if (num < 45) {
						entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.SKELETON);
						ce = new VoidArcher(entity, plugin, rand);
					} else if (num < 70) {
						entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.PHANTOM);
						ce = new VoidStalker(entity, plugin, rand);
					} else {
						entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
						ce = new VoidGuardian(entity, plugin, rand);
					}
					var[1]--;
					if (var[1] <= 0) cancel();
					if (entity == null)
						return;
					entity.setMetadata("dd-endstormentity", new FixedMetadataValue(plugin, "protected"));
					entities.add(ce);
					handler.addEntity(ce);
					targets.put(entity.getUniqueId(), player.getUniqueId());
					mobs.add(entity.getUniqueId());
				}
			}
		};
	}
	public static void createUnstableRift(Location loc, int ticks) {
		double[] var = {ticks, 1};
		final World world = loc.getWorld();
		final Main plugin = Main.getInstance();
		final Random rand = new Random();
		final boolean removeItems = plugin.getConfig().getBoolean("customitems.items.void_wrath.remove_items");
		new RepeatingTask(plugin, 0, 5) {
			@Override
			public void run() {
				var[0] -= 5;
				if (var[0] <= 0) {
					cancel();
					return;
				}
				var[1] += 0.1;
				world.spawnParticle(Particle.PORTAL, loc.clone().add(0,1.3,0), 20, .2, .2, .2, 1.5);
				world.spawnParticle(Particle.SQUID_INK, loc.add(0,0.2,0).clone().add(0,0.3,0), 30, .25, .25, .25, 0.0001);
				world.playSound(loc, Sound.BLOCK_PORTAL_AMBIENT, SoundCategory.AMBIENT, .7f, 1);
				for (Entity e : world.getNearbyEntities(loc, 4, 4, 4)) {
						if (e instanceof Player && (((Player) e).getGameMode() == GameMode.CREATIVE || ((Player) e).getGameMode() == GameMode.SPECTATOR))
							continue;
						Location temp = e.getLocation();
						e.setVelocity(new Vector(temp.getX() - loc.getX(), temp.getY() - loc.getY(), temp.getZ() - loc.getZ()).normalize().multiply(-1).multiply(0.3));
						if (e instanceof LivingEntity && !e.isDead() && temp.distance(loc) < 1 && !(e instanceof ItemFrame))
							Utils.pureDamageEntity((LivingEntity) e, 1, "dd-unstablerift", true);
						else if (!(e instanceof LivingEntity) && temp.distanceSquared(loc) < 4 && !(e instanceof Item && !removeItems))
							e.remove();
					}
				Block b = world.getBlockAt(loc.getBlockX()+(rand.nextInt(8)-4), (int) (loc.getBlockY()-var[1]), loc.getBlockZ()+(rand.nextInt(8)-4));
				if (b.getType().isBlock() && !Utils.isBlockBlacklisted(b.getType()) && !Utils.isZoneProtected(b.getLocation())) {
					FallingBlock fb = world.spawnFallingBlock(b.getLocation(), b.getBlockData());
					fb.setHurtEntities(true);
					fb.setDropItem(false);
					fb.setMetadata("dd-fb", new FixedMetadataValue(plugin, "protected"));
					if (plugin.CProtect)
						Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
					b.setType(Material.AIR);
					Location temp = b.getLocation();
					fb.setVelocity(new Vector(temp.getX() - loc.getX(), temp.getY() - loc.getY(), temp.getZ() - loc.getZ()).normalize().multiply(-1).multiply(0.3));
				}
			}
		};
	}
	public void createCustomRift(Location loc) {
		int[] var = {60, 7};
		this.world = loc.getWorld();
		boolean custom = plugin.getConfig().getBoolean("customentities.allow_custom_mobs");
		ItemStack[] armor = {new ItemStack(Material.LEATHER_BOOTS), new ItemStack(Material.LEATHER_LEGGINGS), new ItemStack(Material.LEATHER_CHESTPLATE), new ItemStack(Material.LEATHER_HELMET)};
		if (!custom) {
			for (int i=0; i < 3; i++) {
				LeatherArmorMeta meta = (LeatherArmorMeta) armor[i].getItemMeta();
				meta.setColor(Color.fromBGR(50, 50, 50));
				armor[i].setItemMeta(meta);
			}
		}
		new RepeatingTask(plugin, 0, 5) {
			@Override
			public void run() {
				world.spawnParticle(Particle.PORTAL, loc, 20, .2, .2, .2, 1.5);
				world.spawnParticle(Particle.SQUID_INK, loc.clone().add(0,0.5,0), 30, .25, .25, .25, 0.0001);
				world.playSound(loc, Sound.BLOCK_PORTAL_AMBIENT, SoundCategory.AMBIENT, .7f, 1);
				for (Entity e : world.getNearbyEntities(loc, .5, .5, .5))
					if (e instanceof Player && (((Player) e).getGameMode() == GameMode.SURVIVAL || ((Player) e).getGameMode() == GameMode.ADVENTURE))
						Utils.pureDamageEntity((LivingEntity) e, 1, "dd-unstablerift", true);
				if (var[0] > 0)
					var[0]-=5;
				else {
					var[0] = 60;
					Mob entity = null;
					CustomEntity ce = null;
					if (!custom) {
						entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
						entity.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
						entity.getEquipment().setItemInOffHand(new ItemStack(Material.SHIELD));
						entity.getEquipment().setArmorContents(armor);
						
						var[1]--;
						if (var[1] <= 0) cancel();
						return;
					}
					if (var[1] >= 6) {
						entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
						ce = new VoidGuardian(entity, plugin, rand);
					} else if (var[1] >= 4) {
						entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.PHANTOM);
						ce = new VoidStalker(entity, plugin, rand);
					} else if (var[1] == 3) {
						entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
						ce = new EndWorm(entity, plugin, rand);
					} else if (var[1] == 2) {
						entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.WITHER_SKELETON);
						entity.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
						ce = new EndTotem(entity, plugin, rand);
					} else {
						entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.WOLF);
						ce = new BabyEndTotem(entity, plugin.dataFile, plugin, rand);
					}
					var[1]--;
					if (var[1] <= 0) cancel();
					if (entity == null)
						return;
					handler.addEntity(ce);
				}
			}
		};
	}
	@Override
	public void clear() {
		time = 0;
		clearEntities();
		DeathMessages.endstorms.remove(this);
	}
	public void clearEntities() {
		for (CustomEntity e : entities) {
			e.clean();
			if (e.getEntity() != null)
				e.getEntity().remove();
		}
	}
	public int getRange() {
		return range;
	}
	public void setRange(int range) {
		this.range = range;
	}
	public int getMaxEntities() {
		return maxEntities;
	}
	public void setMaxEntities(int maxEntities) {
		this.maxEntities = maxEntities;
	}
}