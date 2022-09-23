package deadlydisasters.disasters;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import deadlydisasters.disasters.events.WeatherDisaster;
import deadlydisasters.disasters.events.WeatherDisasterEvent;
import deadlydisasters.general.Main;
import deadlydisasters.general.WorldObject;
import deadlydisasters.listeners.DeathMessages;
import deadlydisasters.utils.Metrics;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public class AcidStorm extends WeatherDisaster {
	
	private int particleRange,particleYRange,blockDamageRange;
	private boolean meltItems,meltArmor,poisonCrops;
	private double damage,slimeRate,blockChangeRate,particleMultiplier;
	private int blocksDestroyed;
	
	private Queue<UUID> slimes = new ArrayDeque<>();
	private Map<PotionEffectType, Integer> potionEffects =  new HashMap<>();
	private Map<Material, Material> blockChanges =  new HashMap<>();
	private Map<UUID,UUID> targets = new HashMap<>();
	
	public static Set<Material> CROPS = new HashSet<>(Arrays.asList(Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.PUMPKIN_STEM, Material.ATTACHED_PUMPKIN_STEM, Material.MELON_STEM, Material.ATTACHED_MELON_STEM, Material.NETHER_WART));
	public static Set<Block> poisonedCrops = ConcurrentHashMap.newKeySet();
	public static BukkitTask cropsMonitor;

	public AcidStorm(int level) {
		super(level);
		meltItems = plugin.getConfig().getBoolean("acidstorm.melt_dropped_items");
		meltArmor = plugin.getConfig().getBoolean("acidstorm.melt_armor");
		time = plugin.getConfig().getInt("acidstorm.time.level "+this.level) * 20;
		delay = plugin.getConfig().getInt("acidstorm.start_delay") * 20;
		damage = plugin.getConfig().getDouble("acidstorm.damage");
		particleRange = plugin.getConfig().getInt("acidstorm.particle_max_distance");
		particleYRange = plugin.getConfig().getInt("acidstorm.particle_Y_range");
		particleMultiplier = 0.25 * plugin.getConfig().getDouble("acidstorm.particle_multiplier");
		blockDamageRange = plugin.getConfig().getInt("acidstorm.block_damage_range");
		blockChangeRate = 0.03 * plugin.getConfig().getDouble("acidstorm.block_change_rate");
		slimeRate = 0.075 * plugin.getConfig().getDouble("acidstorm.slime_spawn_rate");
		poisonCrops = plugin.getConfig().getBoolean("acidstorm.poison_crops");
		volume = plugin.getConfig().getDouble("acidstorm.volume");
		for (String effect : plugin.getConfig().getConfigurationSection("acidstorm.effects").getKeys(false))
			if (PotionEffectType.getByName(effect) != null)
				potionEffects.putIfAbsent(PotionEffectType.getByName(effect), plugin.getConfig().getInt("acidstorm.effects."+effect));
		for (String material : plugin.getConfig().getConfigurationSection("acidstorm.block_changes").getKeys(false))
			if (Material.getMaterial(material.toUpperCase()) != null) {
				if (Material.getMaterial(plugin.getConfig().getString("acidstorm.block_changes."+material).toUpperCase()) != null)
					blockChanges.putIfAbsent(Material.getMaterial(material.toUpperCase()), Material.getMaterial(plugin.getConfig().getString("acidstorm.block_changes."+material).toUpperCase()));
				else if (plugin.getConfig().getString("acidstorm.block_changes."+material).toLowerCase().equals("air"))
					blockChanges.putIfAbsent(Material.getMaterial(material), Material.AIR);
				else
					Main.consoleSender.sendMessage(Utils.chat("&e[DeadlyDisasters]: Could not find material &c'"+plugin.getConfig().getString("acidstorm.block_changes."+material)+"' &eon line &d'"+material+" : "+plugin.getConfig().getString("acidstorm.block_changes."+material)+"' &ein acidstorm block changes section in the config!"));
			} else
				Main.consoleSender.sendMessage(Utils.chat("&e[DeadlyDisasters]: Could not find material &c'"+material+"' &eon line &d'"+material+" : "+plugin.getConfig().getString("acidstorm.block_changes."+material)+"' &ein acidstorm block changes section in the config!"));
		
		this.type = Disaster.ACIDSTORM;
	}
	public void start(World world, Player p, boolean broadcastAllowed) {
		WeatherDisasterEvent event = new WeatherDisasterEvent(this, world, level, p);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		this.world = world;
		updateWeatherSettings();
		ongoingDisasters.add(this);
		if (broadcastAllowed && (boolean) WorldObject.findWorldObject(world).settings.get("event_broadcast"))
			Utils.broadcastEvent(level, "weather", this.type, world);
		DeathMessages.acidstorms.add(this);
		AcidStorm instance = this;
		Random rand = new Random();
		int[] ticks = {0};
		new RepeatingTask(plugin, delay, 5) {
			@Override
			public void run() {
				if (world.hasStorm()) {
					ticks[0]++;
					if (ticks[0] >= 4) {
						ticks[0] = 0;
						for (Player p : world.getPlayers())
							if (!Utils.isPlayerImmune(p) && rand.nextDouble() < slimeRate) {
								Location spawn = world.getHighestBlockAt(Utils.getSpotInSquareRadius(p.getLocation(), 18)).getRelative(BlockFace.UP).getLocation();
								if (spawn.getBlockY() - p.getLocation().getBlockY() <= 25) {
									Slime entity = (Slime) world.spawnEntity(spawn, EntityType.SLIME);
									entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.3);
									entity.setTarget((LivingEntity) p);
									slimes.add(entity.getUniqueId());
									targets.put(entity.getUniqueId(), p.getUniqueId());
								}
							}
					}
					for (Entity all : world.getEntities()) {
						if (slimes.contains(all.getUniqueId()) && ((Mob) all).getTarget() == null && Bukkit.getEntity(targets.get(all.getUniqueId())) != null)
							((Mob) all).setTarget((LivingEntity) Bukkit.getEntity(targets.get(all.getUniqueId())));
						Location temp = all.getLocation();
						if (all.getWorld().getHighestBlockYAt(temp) > temp.getBlockY()+1) continue;
						if (temp.getBlock().getTemperature() <= 0.15 || temp.getBlock().getTemperature() > 0.95) continue;
						if (Utils.isWeatherDisabled(temp, instance)) continue;
						if (all instanceof LivingEntity) {
							if (all instanceof Slime || all.isDead())
								continue;
							LivingEntity e = (LivingEntity) all;
							if (all instanceof Player) {
								if (Utils.isPlayerImmune((Player) all))
									continue;
								((Player) all).playSound(all.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, (float) (0.75*volume), 2F);
							}
							for (Map.Entry<PotionEffectType, Integer> entry : potionEffects.entrySet())
								e.addPotionEffect(new PotionEffect(entry.getKey(), entry.getValue(), 1, true, false, false));
							Utils.pureDamageEntity(e, damage, "dd-acidstormdeath", false);
							if (!meltArmor) continue;
							ItemStack helmet = e.getEquipment().getHelmet(),chest = e.getEquipment().getChestplate(),boots = e.getEquipment().getBoots(),pants = e.getEquipment().getLeggings();
							if (helmet != null && (helmet.getType() == Material.IRON_HELMET || helmet.getType() == Material.GOLDEN_HELMET || helmet.getType() == Material.CHAINMAIL_HELMET)) {
								ItemMeta meta = helmet.getItemMeta();
								((Damageable) meta).setDamage(((Damageable) meta).getDamage()+1);
								if (((Damageable) meta).getDamage() >= helmet.getType().getMaxDurability()) helmet.setAmount(0);
								else helmet.setItemMeta(meta);
								e.getEquipment().setHelmet(helmet);
							}
							if (chest != null && (chest.getType() == Material.IRON_CHESTPLATE || chest.getType() == Material.GOLDEN_CHESTPLATE || chest.getType() == Material.CHAINMAIL_CHESTPLATE)) {
								ItemMeta meta = chest.getItemMeta();
								((Damageable) meta).setDamage(((Damageable) meta).getDamage()+1);
								if (((Damageable) meta).getDamage() >= chest.getType().getMaxDurability()) chest.setAmount(0);
								else chest.setItemMeta(meta);
								e.getEquipment().setChestplate(chest);
							}
							if (pants != null && (pants.getType() == Material.IRON_LEGGINGS || pants.getType() == Material.GOLDEN_LEGGINGS || pants.getType() == Material.CHAINMAIL_LEGGINGS)) {
								ItemMeta meta = pants.getItemMeta();
								((Damageable) meta).setDamage(((Damageable) meta).getDamage()+1);
								if (((Damageable) meta).getDamage() >= pants.getType().getMaxDurability()) pants.setAmount(0);
								else pants.setItemMeta(meta);
								e.getEquipment().setLeggings(pants);
							}
							if (boots != null && (boots.getType() == Material.IRON_BOOTS || boots.getType() == Material.GOLDEN_BOOTS || boots.getType() == Material.CHAINMAIL_BOOTS)) {
								ItemMeta meta = boots.getItemMeta();
								((Damageable) meta).setDamage(((Damageable) meta).getDamage()+1);
								if (((Damageable) meta).getDamage() >= boots.getType().getMaxDurability()) boots.setAmount(0);
								else boots.setItemMeta(meta);
								e.getEquipment().setBoots(boots);
							}
						} else if (meltItems && all.getType().equals(EntityType.DROPPED_ITEM)) {
							ItemStack item = ((Item) all).getItemStack();
							if (item.getType() == Material.IRON_INGOT || item.getType() == Material.IRON_BLOCK || item.getType() == Material.IRON_HORSE_ARMOR || item.getType() == Material.IRON_NUGGET || item.getType() == Material.IRON_DOOR || item.getType() == Material.IRON_TRAPDOOR
									|| item.getType() == Material.GOLD_INGOT || item.getType() == Material.GOLD_BLOCK || item.getType() == Material.GOLDEN_HORSE_ARMOR || item.getType() == Material.GOLD_NUGGET || item.getType() == Material.GOLDEN_CARROT || item.getType() == Material.GOLDEN_APPLE) {
								item.setAmount(0);
							} else if (item.getType() == Material.IRON_SWORD || item.getType() == Material.IRON_AXE || item.getType() == Material.IRON_PICKAXE || item.getType() == Material.IRON_SHOVEL || item.getType() == Material.IRON_HOE
									|| item.getType() == Material.GOLDEN_SWORD || item.getType() == Material.GOLDEN_AXE || item.getType() == Material.GOLDEN_PICKAXE || item.getType() == Material.GOLDEN_SHOVEL || item.getType() == Material.GOLDEN_HOE) {
								ItemMeta meta = item.getItemMeta();
								((Damageable) meta).setDamage(((Damageable) meta).getDamage()+2);
								if (((Damageable) meta).getDamage() >= item.getType().getMaxDurability()) {
									world.dropItem(all.getLocation(), new ItemStack(Material.STICK));
									item.setAmount(0);
								}
								else item.setItemMeta(meta);
							} else if (item.getType() == Material.IRON_HELMET || item.getType() == Material.IRON_CHESTPLATE || item.getType() == Material.IRON_LEGGINGS
									|| item.getType() == Material.IRON_BOOTS || item.getType() == Material.GOLDEN_HELMET || item.getType() == Material.GOLDEN_CHESTPLATE || item.getType() == Material.GOLDEN_LEGGINGS
									|| item.getType() == Material.GOLDEN_BOOTS) {
								ItemMeta meta = item.getItemMeta();
								((Damageable) meta).setDamage(((Damageable) meta).getDamage()+1);
								if (((Damageable) meta).getDamage() >= item.getType().getMaxDurability()) item.setAmount(0);
								else item.setItemMeta(meta);
							}
						}
					}
					time -= 5;
					if (time <= 0)
						world.setStorm(false);
				} else if (time > 0) {
					world.setStorm(true);
				} else {
					DeathMessages.acidstorms.remove(instance);
					ongoingDisasters.remove(instance);
					clearEntities();
					cancel();
					Metrics.incrementValue(Metrics.disasterDestroyedMap, type.getMetricsLabel(), blocksDestroyed);
				}
			}
		};
		BukkitTask[] task = new BukkitTask[2];
		BlockData particleData = Material.LIME_WOOL.createBlockData();
		task[0] = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				if (time <= 0) {
					task[0].cancel();
					return;
				}
				for (Player p : world.getPlayers()) {
					if (!p.getWorld().equals(world))
						continue;
					for (int x=-particleRange; x <= particleRange; x++)
						for (int z=-particleRange; z <= particleRange; z++) {
							if (rand.nextDouble() >= particleMultiplier)
								continue;
							Location temp = p.getLocation().add(x,0,z);
							Location b = world.getHighestBlockAt(temp).getLocation();
							if (b.getBlock().getTemperature() <= 0.15 || b.getBlock().getTemperature() > 0.95 || Utils.isWeatherDisabled(b, instance))
								continue;
							int diff = b.getBlockY() - temp.getBlockY();
							if (diff > particleYRange)
								continue;
							if (diff < 0)
								b.setY(b.getY()+(diff*-1));
							if (plugin.mcVersion >= 1.17)
								p.spawnParticle(Particle.FALLING_SPORE_BLOSSOM, b.add(0.5,5,0.5), 1, .5, 1.5, .5, 1);
							else
								p.spawnParticle(Particle.FALLING_DUST, b.add(0.5,5,0.5), 1, .5, 1.5, .5, 1, particleData);
						}
				}
			}
		}, delay, 1);
		task[1] = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				if (time <= 0) {
					task[1].cancel();
					return;
				}
				Map<Block,Material> changes = new HashMap<>();
				for (Player p : world.getPlayers()) {
					for (int x=-blockDamageRange; x < blockDamageRange; x++)
						for (int z=-blockDamageRange; z < blockDamageRange; z++) {
							if (rand.nextDouble() >= blockChangeRate)
								continue;
							Block b = world.getHighestBlockAt(p.getLocation().add(x,0,z));
							if (poisonCrops && CROPS.contains(b.getRelative(BlockFace.UP).getType()) && !poisonedCrops.contains(b.getRelative(BlockFace.UP))) {
								poisonedCrops.add(b.getRelative(BlockFace.UP));
								if (cropsMonitor == null)
									startCropsMonitor(plugin);
							}
							if (!blockChanges.containsKey(b.getType()) || Utils.isZoneProtected(b.getLocation()) || Utils.isBlockBlacklisted(b.getType()) || Utils.isWeatherDisabled(b.getLocation(), instance))
								continue;
							Material material = blockChanges.get(b.getType());
							if (plugin.CProtect) {
								Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
								Utils.getCoreProtect().logPlacement("Deadly-Disasters", b.getLocation(), material, material.createBlockData());
							}
							changes.put(b, material);
							world.playSound(b.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, (float) (0.1*volume), 2);
						}
				}
				plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
					@Override
					public void run() {
						for (Map.Entry<Block, Material> entry : changes.entrySet())
							entry.getKey().setType(entry.getValue());
						blocksDestroyed += changes.size();
					}
				});
			}
		}, delay, 40);
	}
	public static void startCropsMonitor(Main plugin) {
		BlockData particleData = Material.LIME_WOOL.createBlockData();
		Random rand = plugin.random;
		cropsMonitor = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				Iterator<Block> it = poisonedCrops.iterator();
				while (it.hasNext()) {
					Block b = it.next();
					for (Player p : b.getWorld().getPlayers())
						if (p.getLocation().distanceSquared(b.getLocation()) <= 2500) {
							if (!CROPS.contains(b.getType()) || !(b.getBlockData() instanceof Ageable))
								it.remove();
							int age = ((Ageable) b.getBlockData()).getAge();
							if (age >= ((Ageable) b.getBlockData()).getMaximumAge() && rand.nextInt(50) == 0) {
								plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
									@Override
									public void run() {
										b.getWorld().spawnParticle(Particle.CLOUD, b.getLocation().add(.5,.2,.5), 10, .2, .2, .2, 0.0001);
										b.getWorld().playSound(b.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.1f, 2);
										b.setType(Material.AIR);
									}
								});
								it.remove();
								break;
							}
							if (p.getLocation().distanceSquared(b.getLocation()) <= 100)
								switch (age) {
								default:
								case 0:
									if (plugin.mcVersion >= 1.17)
										b.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, b.getLocation().add(0.5,-0.05,0.5), 4, 0.225, 0.05, 0.225, 1);
									else
										b.getWorld().spawnParticle(Particle.FALLING_DUST, b.getLocation().add(0.5,-0.05,0.5), 3, 0.225, 0.05, 0.225, 0.0001, particleData);
									break;
								case 1:
									if (plugin.mcVersion >= 1.17)
										b.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, b.getLocation().add(0.5,0.05,0.5), 4, 0.225, 0.05, 0.225, 1);
									else
										b.getWorld().spawnParticle(Particle.FALLING_DUST, b.getLocation().add(0.5,0.05,0.5), 3, 0.225, 0.05, 0.225, 0.0001, particleData);
									break;
								case 2:
									if (plugin.mcVersion >= 1.17)
										b.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, b.getLocation().add(0.5,0.3,0.5), 4, 0.24, 0.05, 0.24, 1);
									else
										b.getWorld().spawnParticle(Particle.FALLING_DUST, b.getLocation().add(0.5,0.4,0.5), 3, 0.225, 0.05, 0.225, 0.0001, particleData);
									break;
								case 3:
									if (plugin.mcVersion >= 1.17)
										b.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, b.getLocation().add(0.5,0.5,0.5), 4, 0.24, 0.05, 0.24, 1);
									else
										b.getWorld().spawnParticle(Particle.FALLING_DUST, b.getLocation().add(0.5,0.5,0.5), 3, 0.225, 0.05, 0.225, 0.0001, particleData);
									break;
								case 4:
								case 5:
									if (plugin.mcVersion >= 1.17)
										b.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, b.getLocation().add(0.5,0.7,0.5), 4, 0.24, 0.05, 0.24, 1);
									else
										b.getWorld().spawnParticle(Particle.FALLING_DUST, b.getLocation().add(0.5,0.7,0.5), 3, 0.225, 0.05, 0.225, 0.0001, particleData);
									break;
								}
							break;
						}
				}
				if (poisonedCrops.isEmpty()) {
					cropsMonitor.cancel();
					cropsMonitor = null;
				}
			}
		}, 0, 10);
	}
	@Override
	public void clear() {
		time = 0;
	}
	public void clearEntities() {
		for (UUID e : slimes)
			if (Bukkit.getEntity(e) != null)
				Bukkit.getEntity(e).remove();
	}
	public boolean isMeltItems() {
		return meltItems;
	}
	public void setMeltItems(boolean meltItems) {
		this.meltItems = meltItems;
	}
	public boolean isMeltArmor() {
		return meltArmor;
	}
	public void setMeltArmor(boolean meltArmor) {
		this.meltArmor = meltArmor;
	}
	public double getDamage() {
		return damage;
	}
	public void setDamage(double damage) {
		this.damage = damage;
	}
}
