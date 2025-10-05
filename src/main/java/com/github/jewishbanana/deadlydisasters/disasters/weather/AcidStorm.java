package com.github.jewishbanana.deadlydisasters.disasters.weather;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import com.github.jewishbanana.deadlydisasters.disasters.WeatherDisaster;
import com.github.jewishbanana.deadlydisasters.listeners.EntitiesListener;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.SpawnUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class AcidStorm extends WeatherDisaster {
	
	public static final Map<Block, Integer> poisonedCrops;
	static {
		poisonedCrops = new ConcurrentHashMap<>();
		
		new BukkitRunnable() {
			@Override
			public void run() {
				Iterator<Entry<Block, Integer>> iterator = poisonedCrops.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<Block, Integer> entry = iterator.next();
					int value = entry.getValue() - 1;
					if (value < 0)
						value = 0;
					entry.setValue(value);
					Block block = entry.getKey();
					if (!block.getChunk().isLoaded()) {
						block.getChunk().load();
						continue;
					}
					if (!Tag.CROPS.isTagged(block.getType()) || !(block.getBlockData() instanceof Ageable ageable)) {
						iterator.remove();
						continue;
					}
					if (value == 0) {
						plugin.getServer().getScheduler().runTask(plugin, () -> {
							block.getWorld().spawnParticle(Particle.CLOUD, block.getLocation().add(.5, .2, .5), 3, .2, .2, .2, 0.0001);
							block.getWorld().playSound(block.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.1f, 2);
							block.setType(Material.AIR);
						});
						iterator.remove();
						continue;
					}
					Location loc = BlockUtils.getCenterOfBlock(block);
					for (Player player : block.getWorld().getPlayers()) {
						if (player.getLocation().distanceSquared(loc) > 49)
							continue;
						switch (ageable.getAge()) {
						default:
						case 0:
							block.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, loc.subtract(0, 0.55, 0), 4, .225, .05, .225, 1);
							break;
						case 1:
							block.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, loc.subtract(0, 0.45, 0), 4, .225, .05, .225, 1);
							break;
						case 2:
							block.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, loc.subtract(0, 0.2, 0), 4, .24, .05, .24, 1);
							break;
						case 3:
							block.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, loc, 4, .24, .05, .24, 1);
							break;
						case 4:
						case 5:
							block.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, loc.add(0, 0.2, 0), 4, .24, .05, .24, 1);
							break;
						}
						break;
					}
				}
			}
		}.runTaskTimerAsynchronously(plugin, 0, 20);
	}

	private int minHeight;
	private double damage;
	private float blockChangeRate;
	private float mobSpawnRate;
	private boolean poisonCrops;
	private boolean dissolveEntityArmor;
	private boolean dissolveDroppedItems;
	
	private float particleRate;
	private float soundVolume;
	private Set<PotionEffect> effects;
	private final Map<Material, Material[]> blockChanges = new HashMap<>();
	private final Set<UUID> slimes = new HashSet<>();
	private final Map<UUID, UUID> slimeTargets = new HashMap<>();
	
	public AcidStorm(@NotNull Location location, Player player, int level) {
		super(location, player, level);
		
		this.minHeight = getConfigInt("minimum_height");
	}
	public void init() {
		super.init();
		this.damage = getConfigDouble("damage_per_storm_tick") * scale;
		this.blockChangeRate = (float) (0.0025 * getConfigDouble("block_damage_rate") * scale);
		this.mobSpawnRate = (float) (0.02 * getConfigDouble("mob_spawn_multiplier") * (scale / 2.0));
		this.poisonCrops = getConfigBoolean("poison_crops");
		this.dissolveEntityArmor = getConfigBoolean("dissolve_entity_armor");
		this.dissolveDroppedItems = getConfigBoolean("dissolve_dropped_items");
		
		this.effects = buildPotionEffects("entity_effects");
		ConfigurationSection section = getConfigSection("block_changes");
		if (section != null)
			for (String material : section.getKeys(false)) {
				Set<Material> materials = BlockUtils.getMaterials(material);
				if (materials == null) {
					Utils.sendConsoleMessage("&cERROR the block type or category &d'"+material+"' &cdoes not exist in the world disaster config &b'"+getWorldLink().getConfigName()+"' &cat the section &c'"+getConfigPath()+".block_changes'&c!");
					continue;
				}
				String toMaterial = DataUtils.getConfigString(getWorldLink().getConfig(), getWorldLink().getConfigName(), section.getCurrentPath()+'.'+material, null);
				if (toMaterial == null)
					continue;
				Set<Material> toSet = BlockUtils.getMaterials(toMaterial);
				if (toSet == null) {
					Utils.sendConsoleMessage("&cERROR the block type or category &d'"+toMaterial+"' &cdoes not exist in the world disaster config &b'"+getWorldLink().getConfigName()+"' &cat the section &c'"+getConfigPath()+".block_changes."+material+"'&c!");
					continue;
				}
				materials.forEach(type -> blockChanges.put(type, toSet.toArray(Material[]::new)));
			}
		
		this.particleRate = (float) (0.025 * particleMultiplier * (scale / 3.0));
		this.soundVolume = (float) (0.2 * scale);
		this.soundTickRate = 30;
	}
	public boolean canStart() {
		if (getLocation().getBlockY() < minHeight)
			return false;
		final double temperature = getLocation().getBlock().getTemperature();
		if (temperature <= 0.15 || temperature > 0.95)
			return false;
		return super.canStart();
	}
	public void start() {
		super.start();
		location.setY(128);
		final Map<Entity, Location> foundEntities = new ConcurrentHashMap<>();
		final Set<Entity> entitiesInStorm = ConcurrentHashMap.newKeySet();
		final World world = location.getWorld();
		final AtomicBoolean processEntities = new AtomicBoolean();
		scheduleTask(new BukkitRunnable() {
			private final float itemDissolveChance = (float) (0.02 * (scale / 2.0));
			private final int toolDamage = (int) Math.ceil(damage * 2.0 * (scale / 2.0));
			private final int armorDamage = (int) Math.ceil(damage * (scale / 2.0));
			
			@Override
			public void run() {
				processEntities.set(false);
				foundEntities.clear();
				for (Entity entity : world.getNearbyEntities(location, disasterRange, 193, disasterRange, e -> e.isValid()))
					foundEntities.put(entity, entity.getLocation());
				final Set<Entity> currentEntities = Set.copyOf(entitiesInStorm);
				processEntities.set(true);
				for (Entity entity : currentEntities) {
					Location loc = entity.getLocation();
					if (entity instanceof Player player) {
						if (EntityUtils.isPlayerImmune(player))
							continue;
						if (random.nextFloat() < mobSpawnRate) {
							Location spawn = SpawnUtils.findSpawnLocation(loc, 1, SpawnUtils.MIN_SPAWN_DISTANCE_FROM_PLAYERS, 30);
							if (spawn != null)
								world.spawn(spawn, Slime.class, slime -> {
									slime.setSize(random.nextInt(3));
									slime.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.3);
									slime.setTarget(player);
									slimes.add(slime.getUniqueId());
									slimeTargets.put(slime.getUniqueId(), player.getUniqueId());
									EntitiesListener.attachRemoveKey(slime);
								});
						}
					}
					if (entity instanceof LivingEntity alive) {
						if (entity instanceof Slime slime) {
							UUID target = slimeTargets.get(slime.getUniqueId());
							if (target != null && slime.getTarget() == null)
								slime.setTarget(Bukkit.getPlayer(target));
							continue;
						}
						ItemStack[] armor = alive.getEquipment().getArmorContents();
						if (armor[3] != null && DependencyUtils.getBasicCoatingLevel(armor[3]) != 0)
							continue;
						if (entity instanceof Player player)
							playSound(player, loc, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.WEATHER, soundVolume, 2);
						if (dissolveEntityArmor)
							for (ItemStack item : armor)
								if (item != null)
									switch (item.getType()) {
									case IRON_HELMET:
									case GOLDEN_HELMET:
									case CHAINMAIL_HELMET:
									case IRON_CHESTPLATE:
									case GOLDEN_CHESTPLATE:
									case CHAINMAIL_CHESTPLATE:
									case IRON_LEGGINGS:
									case GOLDEN_LEGGINGS:
									case CHAINMAIL_LEGGINGS:
									case IRON_BOOTS:
									case GOLDEN_BOOTS:
									case CHAINMAIL_BOOTS:
										Utils.damageItem(item, armorDamage);
										break;
									default:
										break;
									}
						alive.addPotionEffects(effects);
						EntityUtils.damageEntity(alive, damage * currentStrength, "deaths.acid_storm", DamageCause.POISON);
					} else if (dissolveDroppedItems && entity instanceof Item item) {
						ItemStack stack = item.getItemStack();
						switch (stack.getType()) {
						case IRON_INGOT:
						case IRON_BLOCK:
						case IRON_HORSE_ARMOR:
						case IRON_NUGGET:
						case IRON_DOOR:
						case IRON_TRAPDOOR:
						case IRON_BARS:
						case GOLD_INGOT:
						case GOLD_BLOCK:
						case GOLDEN_HORSE_ARMOR:
						case GOLD_NUGGET:
						case GOLDEN_CARROT:
						case GOLDEN_APPLE:
							if (random.nextFloat() > itemDissolveChance)
								break;
							stack.setAmount(0);
							world.spawnParticle(Particle.CLOUD, loc, 3, .2, .2, .2, .001);
							playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, .5, 2);
							break;
						case IRON_SWORD:
						case IRON_AXE:
						case IRON_PICKAXE:
						case IRON_SHOVEL:
						case IRON_HOE:
						case GOLDEN_SWORD:
						case GOLDEN_AXE:
						case GOLDEN_PICKAXE:
						case GOLDEN_SHOVEL:
						case GOLDEN_HOE:
							Utils.damageItem(stack, toolDamage);
							if (stack.getAmount() == 0) {
								world.dropItem(loc, new ItemStack(Material.STICK));
								world.spawnParticle(Particle.CLOUD, loc, 3, .2, .2, .2, .001);
								playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, .5, 2);
							}
							break;
						case IRON_HELMET:
						case IRON_CHESTPLATE:
						case IRON_LEGGINGS:
						case IRON_BOOTS:
						case GOLDEN_HELMET:
						case GOLDEN_CHESTPLATE:
						case GOLDEN_LEGGINGS:
						case GOLDEN_BOOTS:
							Utils.damageItem(stack, armorDamage);
							if (stack.getAmount() == 0) {
								world.spawnParticle(Particle.CLOUD, loc, 3, .2, .2, .2, .001);
								playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, .5, 2);
							}
							break;
						default:
							break;
						}
					}
				}
				time -= 5;
				if (time <= 0)
					stop();
			}
		}.runTaskTimer(plugin, 0, 5));
		scheduleTask(new BukkitRunnable() {
			private final double radiusSquared = disasterRange * disasterRange;
			
			@Override
			public void run() {
				if (!processEntities.get())
					return;
				Map<Entity, Location> map = Map.copyOf(foundEntities);
				Set<Entity> set = new HashSet<>();
				map.forEach((entity, loc) -> {
					if (!Utils.isLocationsWithinDistance(new Location(loc.getWorld(), loc.getX(), location.getY(), loc.getZ()), location, radiusSquared))
						return;
					if (isEntityProtected(entity) || !isBlockInClimate(loc.getBlock()))
						return;
					if (world.getHighestBlockYAt(loc) <= loc.getY() + entity.getHeight())
						set.add(entity);
				});
				entitiesInStorm.clear();
				entitiesInStorm.addAll(set);
			}
		}.runTaskTimerAsynchronously(plugin, 1, 1));
		
		final double distanceSquared = disasterRange * disasterRange;
		final double trueSmoothingRange = (disasterRange + smoothingRange) * (disasterRange + smoothingRange);
		createParticleAsyncTask(player -> {
			final Location loc = player.getLocation();
			for (Block block : BlockUtils.getBlocksInCircleRadius(loc, particleRenderDistance)) {
				if (new Location(block.getWorld(), block.getX() + 0.5, location.getY(), block.getZ() + 0.5).distanceSquared(location) > distanceSquared 
						|| random.nextFloat() > particleRate * currentStrength)
					continue;
				Block highest = new Location(block.getWorld(), block.getX(), block.getWorld().getHighestBlockYAt(block.getX(), block.getZ()), block.getZ()).getBlock();
				if (highest == null 
						|| !isBlockInClimate(highest)
						|| highest.getY() - loc.getBlockY() > 10)
					continue;
				player.spawnParticle(Particle.FALLING_SPORE_BLOSSOM, new Location(loc.getWorld(), block.getX() + 0.5, (loc.getY() > highest.getY() ? loc.getY() : highest.getY() + 3) + 8, block.getZ() + 0.5), 1, .5, 6.0, .5, 1);
			}
		}, pair -> {
			final Player player = pair.getFirst();
			final double intensity = pair.getSecond();
			final Location loc = player.getLocation();
			boolean soundFlag = false;
			boolean aboveFlag = false;
			for (Block block : BlockUtils.getBlocksInCircleRadius(loc, particleRenderDistance)) {
				final double actualDistance = new Location(block.getWorld(), block.getX() + 0.5, location.getY(), block.getZ() + 0.5).distanceSquared(location);
				if (actualDistance > trueSmoothingRange || random.nextFloat() > intensity * 2.0 * particleRate * currentStrength)
					continue;
				Block highest = new Location(block.getWorld(), block.getX(), block.getWorld().getHighestBlockYAt(block.getX(), block.getZ()), block.getZ()).getBlock();
				if (highest == null 
						|| !isBlockInClimate(highest)
						|| highest.getY() - loc.getBlockY() > 10)
					continue;
				aboveFlag = true;
				player.spawnParticle(Particle.FALLING_WATER, new Location(loc.getWorld(), block.getX() + 0.5, (loc.getY() > highest.getY() ? loc.getY() : highest.getY()) + 16, block.getZ() + 0.5), 1, .5, 6.0, .5, 1);
				if (actualDistance <= distanceSquared)
					player.spawnParticle(Particle.FALLING_SPORE_BLOSSOM, new Location(loc.getWorld(), block.getX() + 0.5, (loc.getY() > highest.getY() ? loc.getY() : highest.getY() + 3) + 8, block.getZ() + 0.5), 1, .5, 6.0, .5, 1);
				if (!soundFlag && loc.distanceSquared(BlockUtils.getCenterOfBlock(highest)) <= 25
						|| (loc.getY() > highest.getY() && loc.distanceSquared(new Location(loc.getWorld(), highest.getX() + 0.5, loc.getY(), highest.getZ() + 0.5)) <= 25))
					soundFlag = true;
			}
			if (aboveFlag && soundTick == 0) {
				Location fixed = new Location(loc.getWorld(), loc.getX(), location.getY(), loc.getZ());
				if (fixed.distanceSquared(location) > trueSmoothingRange)
					playSound(player, loc.clone().add(Utils.getVectorTowards(loc, location).multiply(8.0).setY(7)), Sound.WEATHER_RAIN, SoundCategory.WEATHER, ((soundVolume / smoothingRangeExcess) * ((smoothingRangeExcess - (fixed.distance(location) - disasterRange - smoothingRange)))) * smoothingIntensity * currentStrength, 1);
				else
					playSound(player, loc.clone().add(0, 7, 0), soundFlag ? Sound.WEATHER_RAIN : Sound.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, soundVolume * currentStrength, 1);
			}
		});
		
		getChunksInvolvedSafelyAndThen(() -> {
			scheduleTask(new BukkitRunnable() {
				private final float poisonCropRate = (float) (0.5 * scale);
				
				@Override
				public void run() {
					if (currentStrength < 0.5)
						return;
					involvedChunks.forEach(chunk -> {
						if (!chunk.isLoaded())
							return;
						Block block = BlockUtils.getHighestExposedBlock(location.getWorld().getHighestBlockAt(chunk.getBlock(random.nextInt(16), 0, random.nextInt(16)).getLocation()), 10);
						if (block == null 
								|| !isBlockInClimate(block)
								|| new Location(block.getWorld(), block.getX() + 0.5, location.getY(), block.getZ() + 0.5).distanceSquared(location) > distanceSquared)
							return;
						if (poisonCrops && random.nextFloat() < poisonCropRate) {
							Block crop = block.getRelative(BlockFace.UP);
							if (crop != null && Tag.CROPS.isTagged(crop.getType()) && !poisonedCrops.containsKey(crop))
								poisonedCrops.put(crop, random.nextInt(180, 1800));
						}
						if (random.nextFloat() > blockChangeRate)
							return;
						Material[] materials = blockChanges.get(block.getType());
						if (materials != null && materials.length != 0) {
							Material change = materials[random.nextInt(materials.length)];
							Location top = block.getLocation().add(.5, 1.1, .5);
							plugin.getServer().getScheduler().runTask(plugin, () -> {
								replaceBlockWithProperties(block, change);
								playSound(top, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, .1, 2);
								location.getWorld().spawnParticle(Particle.CLOUD, change == Material.AIR ? top.subtract(0, 1, 0) : top, 3, .5, .05, .5, 0.001);
							});
						}
					});
				}
			}.runTaskTimerAsynchronously(plugin, 0, 5));
		});
	}
	public void clean() {
		super.clean();
		slimes.forEach(uuid -> {
			Entity entity = Bukkit.getEntity(uuid);
			if (entity != null)
				entity.remove();
		});
	}
	public boolean isBlockInClimate(Block block) {
		final double temp = block.getTemperature();
		return temp > 0.15 && temp <= 0.95;
	}
	protected String getConfigPath() {
		return "disasters.weather.acid_storm";
	}
	public double getRegenTickRate() {
		return 0.5;
	}
	public Set<Environment> getBannedEnvironments() {
		return Set.of(Environment.NETHER, Environment.THE_END);
	}
}
