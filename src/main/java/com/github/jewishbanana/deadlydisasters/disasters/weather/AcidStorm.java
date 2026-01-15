package com.github.jewishbanana.deadlydisasters.disasters.weather;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import com.github.jewishbanana.deadlydisasters.disasters.MobDisaster;
import com.github.jewishbanana.deadlydisasters.disasters.WeatherDisaster;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.SpawnUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class AcidStorm extends WeatherDisaster implements Listener, MobDisaster {
	
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
	private List<PotionEffect> effects;
	private Map<Material, Material[]> blockChanges;
	
	public AcidStorm(@NotNull Location location, Player player, int level) {
		super(location, player, level);
		
		this.minHeight = getConfigInt("minimum_height");
	}
	public void init() {
		super.init();
		this.damage = getConfigDouble("damage_per_storm_tick") * scale;
		this.blockChangeRate = (float) (0.0005 * getConfigDouble("block_damage_rate") * scale);
		this.mobSpawnRate = (float) (0.02 * getConfigDouble("mob_spawn_multiplier") * (scale / 1.5));
		this.poisonCrops = getConfigBoolean("poison_crops");
		this.dissolveEntityArmor = getConfigBoolean("dissolve_entity_armor");
		this.dissolveDroppedItems = getConfigBoolean("dissolve_dropped_items");
		
		this.effects = buildPotionEffects("entity_effects");
		this.blockChanges = buildBlockChanges("block_changes");
		
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
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		location.setY(128);
		final Set<Entity> protectedEntities = new HashSet<>();
		scheduleTask(new BukkitRunnable() {
			private final float itemDissolveChance = (float) (0.04 * (scale / 2.0));
			private final int toolDamage = (int) Math.ceil(damage * 4.0 * (scale / 2.0));
			private final int armorDamage = (int) Math.ceil(damage * 2.0 * (scale / 2.0));
			
			@Override
			public void run() {
				for (Player player : playersInMonitorArea) {
					if (!player.isValid() || EntityUtils.isPlayerImmune(player))
						continue;
					if (random.nextFloat() < mobSpawnRate) {
						Location spawn = SpawnUtils.findMonsterSpawnLocation(player.getLocation(), 1, SpawnUtils.MIN_SPAWN_DISTANCE_FROM_PLAYERS, 30);
						if (spawn != null)
							world.spawn(spawn, Slime.class, slime -> {
								slime.setSize(random.nextInt(3));
								slime.getAttribute(VersionUtils.getMovementSpeedAttribute()).setBaseValue(0.3);
								addEntityToDisasterList(slime, player);
								entitiesInMonitorArea.add(slime);
							});
					}
				}
				for (Entity entity : entitiesInMonitorArea) {
					if (!entity.isValid())
						continue;
					entity.setFireTicks(-20);
					if (protectedEntities.contains(entity))
						continue;
					if (entity instanceof LivingEntity living) {
						if (EntityUtils.isEntityImmunePlayer(entity))
							continue;
						ItemStack[] armor = living.getEquipment().getArmorContents();
						if (armor[3] != null && DependencyUtils.getBasicCoatingLevel(armor[3]) != 0)
							continue;
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
						living.addPotionEffects(effects);
						if (EntityUtils.damageEntity(living, damage * currentStrength, "deaths.acid_storm", DamageCause.POISON) && !living.isSilent())
							playSound(living.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.WEATHER, soundVolume, 2);
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
							if (DependencyUtils.getBasicCoatingLevel(stack) != 0)
								break;
							if (random.nextFloat() < itemDissolveChance) {
								stack.setAmount(0);
								Location loc = entity.getLocation();
								world.spawnParticle(Particle.CLOUD, loc, 3, .2, .2, .2, .001);
								playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, .5, 2);
							}
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
							if (DependencyUtils.getBasicCoatingLevel(stack) != 0)
								break;
							Utils.damageItem(stack, toolDamage);
							if (stack.getAmount() == 0) {
								Location loc = entity.getLocation();
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
							if (DependencyUtils.getBasicCoatingLevel(stack) != 0)
								break;
							Utils.damageItem(stack, armorDamage);
							if (stack.getAmount() == 0) {
								Location loc = entity.getLocation();
								world.spawnParticle(Particle.CLOUD, loc, 3, .2, .2, .2, .001);
								playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, .5, 2);
							}
							break;
						default:
							break;
						}
					}
				}
				updateEntityTargets();
			}
		}.runTaskTimer(plugin, 0, 10));

		createAsyncEntityMonitor(Entity::isValid, 
				(found, entities, players) -> {
					final List<Entity> protectedFound = new ArrayList<>();
					found.forEach((entity, loc) -> {
						if (!isWithinStorm(loc))
							return;
						if (!isBlockInClimate(loc.getBlock()))
							return;
						if (world.getBlockAt(loc.getBlockX(), (int) Math.ceil(loc.getY() + entity.getHeight()), loc.getBlockZ()).getLightFromSky() == 15) {
							entities.add(entity);
							if (isEntityProtected(entity))
								protectedFound.add(entity);
							if (entity instanceof Player p)
								players.add(p);
							return;
						}
						if (entity instanceof Player p && loc.getY() > minHeight - 5)
							players.add(p);
					});
					new BukkitRunnable() {
						@Override
						public void run() {
							protectedEntities.clear();
							protectedEntities.addAll(protectedFound);
						}
					}.runTask(plugin);
				});

		final double trueSmoothingRange = (disasterRange + smoothingRange) * (disasterRange + smoothingRange);
		createParticleAsyncTask(player -> {
			final ThreadLocalRandom random = ThreadLocalRandom.current();
			final Location loc = player.getLocation();
			for (Block block : BlockUtils.getBlocksInCircleRadius(loc, particleRenderDistance)) {
//				Block highest = world.getHighestBlockAt(block.getX(), block.getZ());
//				player.spawnParticle(Particle.FLAME, BlockUtils.getCenterOfBlock(highest).add(0, 1, 0), 1, 0, 0, 0, 0.001);
				if (random.nextFloat() >= particleRate * currentStrength || !isWithinStorm(block))
					continue;
				Block highest = world.getHighestBlockAt(block.getX(), block.getZ());
				if (highest == null || highest.getY() - loc.getBlockY() > 10 || !isBlockInClimate(highest))
					continue;
				player.spawnParticle(Particle.FALLING_SPORE_BLOSSOM, block.getX() + 0.5, (loc.getY() > highest.getY() ? loc.getY() : highest.getY() + 3) + 8, block.getZ() + 0.5, 1, .5, 6.0, .5, 1);
			}
		}, pair -> {
			final ThreadLocalRandom random = ThreadLocalRandom.current();
			final Player player = pair.getFirst();
			final double intensity = pair.getSecond();
			final Location loc = player.getLocation();
			boolean soundFlag = false;
			boolean aboveFlag = false;
			for (Block block : BlockUtils.getBlocksInCircleRadius(loc, particleRenderDistance)) {
				if (random.nextFloat() >= intensity * 2.0 * particleRate * currentStrength)
					continue;
				final double centerX = block.getX() + 0.5;
				final double centerZ = block.getZ() + 0.5;
				final double dx = centerX - location.getX();
				final double dz = centerZ - location.getZ();
				final double distanceSquared = dx * dx + dz * dz;
				if (distanceSquared > trueSmoothingRange)
					continue;
				Block highest = world.getHighestBlockAt(block.getX(), block.getZ());
				if (highest == null || highest.getY() - loc.getBlockY() > 10 || !isBlockInClimate(highest))
					continue;
				aboveFlag = true;
				player.spawnParticle(Particle.FALLING_WATER, centerX, (loc.getY() > highest.getY() ? loc.getY() : highest.getY()) + 16, centerZ, 1, .5, 6.0, .5, 1);
				if (distanceSquared <= disasterRangeSquared)
					player.spawnParticle(Particle.FALLING_SPORE_BLOSSOM, centerX, (loc.getY() > highest.getY() ? loc.getY() : highest.getY() + 3) + 8, centerZ, 1, .5, 6.0, .5, 1);
				if (!soundFlag) {
					if (loc.distanceSquared(BlockUtils.getCenterOfBlock(highest)) <= 25)
						soundFlag = true;
					else if (loc.getY() > highest.getY()) {
						final double bx = highest.getX() + 0.5 - loc.getX();
						final double bz = highest.getZ() + 0.5 - loc.getZ();
						if (bx * bx + bz * bz <= 25)
							soundFlag = true;
					}
				}
			}
			if (aboveFlag && soundTick == 0) {
				final Location fixed = new Location(world, loc.getX(), location.getY(), loc.getZ());
				if (fixed.distanceSquared(location) > trueSmoothingRange)
					playSound(player, loc.clone().add(Utils.getVectorTowards(loc, location).multiply(8.0).setY(7)), Sound.WEATHER_RAIN, SoundCategory.WEATHER, ((soundVolume / smoothingRangeExcess) * ((smoothingRangeExcess - (fixed.distance(location) - disasterRange - smoothingRange)))) * smoothingIntensity * currentStrength, 1);
				else
					playSound(player, loc.add(0, 7, 0), soundFlag ? Sound.WEATHER_RAIN : Sound.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, soundVolume * currentStrength, 1);
			}
		});
		
		getChunksInvolvedSafelyAndThen(() -> {
			scheduleTask(new BukkitRunnable() {
				private final float poisonCropRate = (float) (0.1 * scale);
				
				@Override
				public void run() {
					if (currentStrength < 0.5)
						return;
					final ThreadLocalRandom random = ThreadLocalRandom.current();
					involvedChunks.forEach(chunk -> {
						if (!chunk.isLoaded())
							return;
						final Block block = world.getHighestBlockAt((chunk.getX() << 4) + random.nextInt(16), (chunk.getZ() << 4) + random.nextInt(16));
						if (block == null || !isBlockInClimate(block) || !isWithinStorm(block))
							return;
						final Block above = block.getRelative(BlockFace.UP);
						if (above != null) {
							if (Tag.FIRE.isTagged(above.getType()) && !isBlockProtected(above)) {
								new BukkitRunnable() {
									@Override
									public void run() {
										if (!Tag.FIRE.isTagged(above.getType()))
											return;
										above.setType(Material.AIR);
										world.playSound(BlockUtils.getCenterOfBlock(above), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1f, 1f);
									}
								}.runTask(plugin);
							} else if (poisonCrops && Tag.CROPS.isTagged(above.getType()) && !poisonedCrops.containsKey(above) && random.nextFloat() < poisonCropRate && !isBlockProtected(above))
								poisonedCrops.put(above, random.nextInt(180, 1800));
						}
						if (random.nextFloat() >= blockChangeRate)
							return;
						final Material type = block.getType();
						final Material[] materials = blockChanges.get(type);
						if (materials != null && materials.length != 0) {
							final Material change = materials[random.nextInt(materials.length)];
							final Location top = block.getLocation().add(.5, 1.1, .5);
							new BukkitRunnable() {
								@Override
								public void run() {
									if (block.getType() != type)
										return;
									replaceBlockWithProperties(block, change);
									playSound(top, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, .1, 2);
									world.spawnParticle(Particle.CLOUD, change == Material.AIR ? top.subtract(0, 1, 0) : top, 3, .5, .05, .5, 0.001);
								}
							}.runTask(plugin);
						}
					});
				}
			}.runTaskTimerAsynchronously(plugin, 0, 1));
		});
	}
	public void clean() {
		super.clean();
		HandlerList.unregisterAll(this);
	}
	public boolean isBlockInClimate(Block block) {
		final double temp = block.getTemperature();
		return temp > 0.15 && temp <= 0.95;
	}
	protected String getConfigPath() {
		return "disasters.weather.acid_storm";
	}
	public Set<Environment> getBannedEnvironments() {
		return EnumSet.of(Environment.NETHER, Environment.THE_END);
	}
	
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onEntityCombust(EntityCombustEvent event) {
		final Location loc = event.getEntity() instanceof LivingEntity living ? living.getEyeLocation() : event.getEntity().getLocation();
		if (loc.getWorld().equals(world) && loc.getBlock().getLightFromSky() == 15 && isWithinStorm(loc))
			event.setCancelled(true);
	}
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onFireSpread(BlockSpreadEvent event) {
		if (!Tag.FIRE.isTagged(event.getNewState().getType()))
			return;
		Block block = event.getBlock();
		if (!block.getWorld().equals(world) || block.getLightFromSky() != 15 || !isWithinStorm(block))
			return;
		event.setCancelled(true);
		Block source = event.getSource();
		if (source.getLightFromSky() != 15)
			return;
		Location center = BlockUtils.getCenterOfBlock(source);
		if (!Tag.FIRE.isTagged(source.getType()) || source.getLightFromSky() != 15 || DependencyUtils.isRegionProtected(center))
			return;
		source.setType(Material.AIR);
		world.playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1f, 1f);
	}
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		Block fire = event.getIgnitingBlock();
		if (!fire.getWorld().equals(world) || fire.getLightFromSky() != 15 || !isWithinStorm(fire))
			return;
		event.setCancelled(true);
		Location center = BlockUtils.getCenterOfBlock(fire);
		if (DependencyUtils.isRegionProtected(center))
			return;
		fire.setType(Material.AIR);
		world.playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1f, 1f);
	}
}
