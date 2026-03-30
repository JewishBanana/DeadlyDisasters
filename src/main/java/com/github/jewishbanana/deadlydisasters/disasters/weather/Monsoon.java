package com.github.jewishbanana.deadlydisasters.disasters.weather;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.FluidLevelChangeEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.disasters.MobDisaster;
import com.github.jewishbanana.deadlydisasters.disasters.WeatherDisaster;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.SpawnUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class Monsoon extends WeatherDisaster implements MobDisaster, Listener {
	
	private final static Set<Material> leakBlockTypes;
	private static final List<List<Block>> globalPuddles;
	static {
		Set<Material> tempSet = new HashSet<>();
		tempSet.addAll(Tag.PLANKS.getValues());
		tempSet.addAll(Tag.WOODEN_STAIRS.getValues());
		tempSet.addAll(Tag.WOODEN_SLABS.getValues());
		tempSet.addAll(Tag.WOODEN_TRAPDOORS.getValues());
		tempSet.addAll(Tag.LEAVES.getValues());
		leakBlockTypes = EnumSet.copyOf(tempSet);
		
		globalPuddles = new ArrayList<>();
	}

	private int minHeight;
	private int drownRate;
	private boolean extinguishDroppedItems;
	private float puddleSpawnRate;
	private float puddleDryRate;
	private float blockChangeRate;
	private float mobSpawnRate;
	
	private float particleRate;
	private float waterLeakRate;
	private float soundVolume;
	private List<PotionEffect> effects;
	private Map<Material, Material[]> blockChanges;
	private final Set<Block> puddles = new HashSet<>();
	
	public Monsoon(@NotNull Location location, Player player, int level) {
		super(location, player, level);
		
		this.minHeight = getConfigInt("minimum_height");
	}
	public void init() {
		super.init();
		this.drownRate = (int) (5.0 * getConfigDouble("entity_drown_rate") * scale);
		this.extinguishDroppedItems = getConfigBoolean("extinguish_dropped_items");
		this.puddleSpawnRate = (float) (0.0014 * getConfigDouble("puddle_spawn_rate") * scale);
		this.puddleDryRate = (float) (0.5 * getConfigDouble("puddle_dry_rate") * scale);
		this.waterLeakRate = (float) (0.6 * getConfigDouble("water_leak_multiplier") * (scale / 2.0));
		this.blockChangeRate = (float) (0.0005 * getConfigDouble("block_damage_rate") * scale);
		this.mobSpawnRate = (float) (0.02 * getConfigDouble("mob_spawn_multiplier") * (scale / 1.5));
		
		this.effects = buildPotionEffects("entity_effects");
		this.blockChanges = buildBlockChanges("block_changes");
		
		this.particleRate = (float) (0.3 * particleMultiplier * (scale / 3.0));
		this.soundVolume = (float) (0.2 * scale);
		this.soundTickRate = 30;
	}
	public boolean canStart() {
		if (getLocation().getBlockY() < minHeight)
			return false;
		// Jungle only climate
		final Block block = getLocation().getBlock();
		if (block.getWorld().getTemperature(block.getX(), block.getY(), block.getZ()) < 0.9 || block.getWorld().getHumidity(block.getX(), block.getY(), block.getZ()) < 0.8)
			return false;
		return super.canStart();
	}
	public void start() {
		super.start();
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		location.setY(128);
		final List<LivingEntity> drowningEntities = new ArrayList<>();
		final Set<Entity> protectedEntities = new HashSet<>();
		scheduleTask(new BukkitRunnable() {
			private final float itemExtinguishChance = (float) (0.08 * (scale / 2.0));
			
			@Override
			public void run() {
				for (Player player : playersInMonitorArea) {
					if (!player.isValid() || EntityUtils.isPlayerImmune(player))
						continue;
					if (random.nextFloat() < mobSpawnRate) {
						Location spawn = SpawnUtils.findMonsterSpawnLocation(player.getLocation(), 2, SpawnUtils.MIN_SPAWN_DISTANCE_FROM_PLAYERS, 30);
						if (spawn != null) {
							Mob mob = null;
							switch (random.nextInt(DependencyUtils.isUltimateContentEnabled() ? 2 : 1)) {
							default:
							case 0:
								mob = world.spawn(spawn, Drowned.class);
								break;
							case 1:
								if (random.nextInt(8) == 0)
									mob = com.github.jewishbanana.uiframework.entities.UIEntityManager.spawnEntity(spawn, com.github.jewishbanana.ultimatecontent.entities.waterentities.CursedDiver.class).getCastedEntity();
								else
									mob = world.spawn(spawn, Drowned.class);
								break;
							}
							addEntityToDisasterList(mob, player);
							entitiesInMonitorArea.add(mob);
						}
					}
				}
				drowningEntities.clear();
				for (Entity entity : entitiesInMonitorArea) {
					if (!entity.isValid())
						continue;
					entity.setFireTicks(-20);
					if (protectedEntities.contains(entity))
						continue;
					if (entity instanceof LivingEntity living) {
						if (EntityUtils.isEntityImmunePlayer(entity))
							continue;
						living.addPotionEffects(effects);
						drowningEntities.add(living);
					} else if (extinguishDroppedItems && entity instanceof Item item) {
						ItemStack stack = item.getItemStack();
						switch (stack.getType()) {
						case BLAZE_ROD:
						case BLAZE_POWDER:
						case FIRE_CHARGE:
						case SNOW:
						case SNOW_BLOCK:
						case SNOWBALL:
							if (random.nextFloat() < itemExtinguishChance) {
								stack.setAmount(0);
								final Location loc = entity.getLocation();
								world.spawnParticle(Particle.CLOUD, loc, 3, .2, .2, .2, .001);
								playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, .5f, 1f);
							}
							break;
						case LAVA_BUCKET:
							if (random.nextFloat() < itemExtinguishChance) {
								final Location loc = entity.getLocation();
								entity.remove();
								world.dropItem(loc, new ItemStack(Material.BUCKET));
								world.spawnParticle(Particle.CLOUD, loc, 3, .2, .2, .2, .001);
								playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, .5f, 1f);
							}
							break;
						case POWDER_SNOW_BUCKET:
							if (random.nextFloat() < itemExtinguishChance) {
								final Location loc = entity.getLocation();
								entity.remove();
								world.dropItem(loc, new ItemStack(Material.WATER_BUCKET));
								world.spawnParticle(Particle.CLOUD, loc, 3, .2, .2, .2, .001);
								playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, .5f, 1f);
							}
							break;
						case MAGMA_BLOCK:
							if (random.nextFloat() < itemExtinguishChance) {
								final Location loc = entity.getLocation();
								entity.remove();
								world.dropItem(loc, new ItemStack(Material.NETHERRACK));
								world.spawnParticle(Particle.CLOUD, loc, 3, .2, .2, .2, .001);
								playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, .5f, 1f);
							}
							break;
						case TORCH:
						case SOUL_TORCH:
						case REDSTONE_TORCH:
						case CAMPFIRE:
						case SOUL_CAMPFIRE:
							if (random.nextFloat() < itemExtinguishChance) {
								final Location loc = entity.getLocation();
								entity.remove();
								world.dropItem(loc, new ItemStack(Material.STICK));
								world.spawnParticle(Particle.CLOUD, loc, 3, .2, .2, .2, .001);
								playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, .5f, 1f);
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
		if (drownRate > 0)
			scheduleTask(new BukkitRunnable() {
				private int damageTick;
				
				@Override
				public void run() {
					drowningEntities.forEach(entity -> {
						if (!entity.isValid())
							return;
						if (entity.getRemainingAir() > -10)
							entity.setRemainingAir(entity.getRemainingAir() - drownRate);
						else if (damageTick == 0 && !entity.getEyeLocation().getBlock().isLiquid()) {
							EntityUtils.pureDamageEntity(entity, 1.0, "deaths.monsoon", DamageCause.DROWNING, null, false, false, Sound.ENTITY_PLAYER_HURT_DROWN);
						}
					});
					if (++damageTick == 10)
						damageTick = 0;
				}
			}.runTaskTimer(plugin, 0, 1));
		
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
				if (random.nextFloat() >= particleRate * currentStrength || !isWithinStorm(block))
					continue;
				Block highest = world.getHighestBlockAt(block.getX(), block.getZ());
				if (highest == null || highest.getY() - loc.getBlockY() > 15 || !isBlockInClimate(highest))
					continue;
				if (highest.getY() - loc.getBlockY() <= 10)
					player.spawnParticle(Particle.FALLING_WATER, new Location(world, block.getX() + 0.5, (loc.getY() > highest.getY() ? loc.getY() : highest.getY() + 3) + 8, block.getZ() + 0.5), 1, .5, 6.0, .5, 1);
				if (random.nextFloat() < waterLeakRate * currentStrength) {
					boolean hollow = false;
					for (int i=0; i < 5; i++) {
						if (!leakBlockTypes.contains(highest.getType())) {
							if (!highest.isPassable())
								break;
						} else
							hollow = true;
						highest = highest.getRelative(BlockFace.DOWN);
						if (highest == null)
							break;
						if (hollow && highest.getType() == Material.AIR) {
							player.spawnParticle(VersionUtils.getDripWater(), highest.getX() + 0.5, highest.getY() + 1.0, highest.getZ() + 0.5, 1, .4, 0, .4, 1);
							hollow = false;
						}
					}
				}
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
				final Block highest = world.getHighestBlockAt(block.getX(), block.getZ());
				if (highest == null || highest.getY() - loc.getBlockY() > 10 || !isBlockInClimate(highest))
					continue;
				aboveFlag = true;
				player.spawnParticle(Particle.FALLING_WATER, centerX, (loc.getY() > highest.getY() ? loc.getY() : highest.getY()) + 16, centerZ, 1, .5, 6.0, .5, 1);
				if (distanceSquared <= disasterRangeSquared)
					player.spawnParticle(Particle.FALLING_WATER, centerX, (loc.getY() > highest.getY() ? loc.getY() : highest.getY() + 5) + 8, centerZ, 1, .5, 6.0, .5, 1);
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
					playSound(player, loc.clone().add(Utils.getVectorTowards(loc, location).multiply(8.0).setY(7)), Sound.WEATHER_RAIN, SoundCategory.WEATHER, (float) (((soundVolume / smoothingRangeExcess) * ((smoothingRangeExcess - (fixed.distance(location) - disasterRange - smoothingRange)))) * smoothingIntensity * currentStrength), 1f);
				else
					playSound(player, loc.clone().add(0, 7, 0), soundFlag ? Sound.WEATHER_RAIN : Sound.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, (float) (soundVolume * currentStrength), 1f);
			}
		});
		
		final BlockData puddleData = Material.WATER.createBlockData(data -> ((Levelled) data).setLevel(7));
		getChunksInvolvedSafelyAndThen(() -> {
			scheduleTask(new BukkitRunnable() {
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
							if (Tag.FIRE.isTagged(above.getType()) && !isBlockProtected(above))
								new BukkitRunnable() {
									@Override
									public void run() {
										if (!Tag.FIRE.isTagged(above.getType()))
											return;
										above.setType(Material.AIR);
										world.playSound(BlockUtils.getCenterOfBlock(above), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1f, 1f);
									}
								}.runTask(plugin);
							if (above.getType() == Material.AIR && random.nextFloat() < puddleSpawnRate && above.canPlace(puddleData) && !isBlockProtected(above))
								new BukkitRunnable() {
									@Override
									public void run() {
										if (time <= 0)
											return;
										above.setBlockData(puddleData);
										puddles.add(above);
									}
								}.runTask(plugin);
						}
						if (random.nextFloat() >= blockChangeRate)
							return;
						final Material type = block.getType();
						final Material[] materials = blockChanges.get(type);
						if (materials != null && materials.length != 0) {
							final Material change = materials[random.nextInt(materials.length)];
							new BukkitRunnable() {
								@Override
								public void run() {
									if (block.getType() != type)
										return;
									replaceBlockWithProperties(block, change);
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
		if (!Main.isDisablingPlugin) {
			final List<Block> puddleBlocks = new ArrayList<>(puddles);
			Collections.shuffle(puddleBlocks);
			globalPuddles.add(puddleBlocks);
			new BukkitRunnable() {
				private double regenTicks;
				private final Iterator<Block> iterator = puddleBlocks.iterator();
				
				@Override
				public void run() {
					regenTicks += puddleDryRate;
					while (regenTicks >= 1) {
						regenTicks -= 1;
						if (!iterator.hasNext()) {
							this.cancel();
							globalPuddles.remove(puddleBlocks);
							return;
						}
						Block block = iterator.next();
						if (block.getType() == Material.WATER && block.getBlockData() instanceof Levelled data && data.getLevel() == 7)
							block.setType(Material.AIR);
						iterator.remove();
					}
				}
			}.runTaskTimer(plugin, 200, 1);
		} else
			puddles.forEach(block -> {
				if (block.getType() == Material.WATER && block.getBlockData() instanceof Levelled data && data.getLevel() == 7)
					block.setType(Material.AIR);
			});
	}
	public static void clearAllPuddles() {
		globalPuddles.forEach(list -> {
			list.forEach(block -> {
				if (block.getType() == Material.WATER && block.getBlockData() instanceof Levelled data && data.getLevel() == 7)
					block.setType(Material.AIR);
			});
		});
	}
	public boolean isBlockInClimate(Block block) {
		final double temp = world.getTemperature(block.getX(), block.getY(), block.getZ());
		return temp > 0.15 && temp <= 0.95;
	}
	protected String getConfigPath() {
		return "disasters.weather.monsoon";
	}
	public Set<Environment> getBannedEnvironments() {
		return EnumSet.of(Environment.NETHER, Environment.THE_END);
	}
	
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onFlow(FluidLevelChangeEvent event) {
		if (puddles.contains(event.getBlock()))
			event.setCancelled(true);
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
