package com.github.jewishbanana.deadlydisasters.disasters.weather;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
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
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.FluidLevelChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.disasters.WeatherDisaster;
import com.github.jewishbanana.deadlydisasters.listeners.EntitiesListener;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.SpawnUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class Monsoon extends WeatherDisaster implements Listener {
	
	private final static Set<Material> leakBlockTypes;
	static {
		Set<Material> tempSet = new HashSet<>();
		tempSet.addAll(Tag.PLANKS.getValues());
		tempSet.addAll(Tag.WOODEN_STAIRS.getValues());
		tempSet.addAll(Tag.WOODEN_SLABS.getValues());
		tempSet.addAll(Tag.WOODEN_TRAPDOORS.getValues());
		tempSet.addAll(Tag.LEAVES.getValues());
		leakBlockTypes = Set.copyOf(tempSet);
	}

	private int minHeight;
	private int drownRate;
	private float puddleSpawnRate;
	private float puddleDryRate;
	private float blockChangeRate;
	private float mobSpawnRate;
	
	private float particleRate;
	private float waterLeakRate;
	private float soundVolume;
	private Set<PotionEffect> effects;
	private final Map<Material, Material[]> blockChanges = new HashMap<>();
	private final Set<UUID> mobs = new HashSet<>();
	private final Map<UUID, UUID> mobTargets = new HashMap<>();
	private final Set<Block> puddles = new HashSet<>();
	
	public Monsoon(@NotNull Location location, Player player, int level) {
		super(location, player, level);
		
		this.minHeight = getConfigInt("minimum_height");
	}
	public void init() {
		super.init();
		this.drownRate = (int) (5.0 * getConfigDouble("entity_drown_rate") * scale);
		this.puddleSpawnRate = (float) (0.007 * getConfigDouble("puddle_spawn_rate") * scale);
		this.puddleDryRate = (float) (0.5 * getConfigDouble("puddle_dry_rate") * scale);
		this.waterLeakRate = (float) (0.7 * getConfigDouble("water_leak_multiplier") * (scale / 2.0));
		this.blockChangeRate = (float) (0.0025 * getConfigDouble("block_damage_rate") * scale);
		this.mobSpawnRate = (float) (0.02 * getConfigDouble("mob_spawn_multiplier") * (scale / 2.0));
		
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
		
		this.particleRate = (float) (0.3 * particleMultiplier * (scale / 3.0));
		this.soundVolume = (float) (0.2 * scale);
		this.soundTickRate = 30;
	}
	public boolean canStart() {
		if (getLocation().getBlockY() < minHeight)
			return false;
		// Jungle only climate
		final Block block = getLocation().getBlock();
		if (block.getTemperature() < 0.9 || block.getHumidity() < 0.8)
			return false;
		return super.canStart();
	}
	public void start() {
		super.start();
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		location.setY(128);
		final Map<Entity, Location> foundEntities = new ConcurrentHashMap<>();
		final Set<Entity> entitiesInStorm = ConcurrentHashMap.newKeySet();
		final World world = location.getWorld();
		final Set<LivingEntity> drowningEntities = new HashSet<>();
		final AtomicBoolean processEntities = new AtomicBoolean();
		scheduleTask(new BukkitRunnable() {
			@Override
			public void run() {
				processEntities.set(false);
				foundEntities.clear();
				for (Entity entity : world.getNearbyEntities(location, disasterRange, 193, disasterRange, e -> e.isValid()))
					foundEntities.put(entity, entity.getLocation());
				final Set<Entity> currentEntities = Set.copyOf(entitiesInStorm);
				processEntities.set(true);
				drowningEntities.clear();
				for (Entity entity : currentEntities) {
					Location loc = entity.getLocation();
					if (entity instanceof Player player) {
						if (EntityUtils.isPlayerImmune(player))
							continue;
						if (random.nextFloat() < mobSpawnRate) {
							Location spawn = SpawnUtils.findSpawnLocation(loc, 2, SpawnUtils.MIN_SPAWN_DISTANCE_FROM_PLAYERS, 30);
							if (spawn != null) {
								Mob mob = null;
								switch (DependencyUtils.isUltimateContentEnabled() ? random.nextInt(2) : random.nextInt(1)) {
								default:
								case 0:
									mob = world.spawn(spawn, Drowned.class);
									break;
								case 2:
									if (random.nextInt(8) == 0)
										mob = com.github.jewishbanana.uiframework.entities.UIEntityManager.spawnEntity(spawn, com.github.jewishbanana.ultimatecontent.entities.waterentities.CursedDiver.class).getCastedEntity();
									else
										mob = world.spawn(spawn, Drowned.class);
									break;
								}
								mob.setTarget(player);
								mobs.add(mob.getUniqueId());
								mobTargets.put(mob.getUniqueId(), player.getUniqueId());
								EntitiesListener.attachRemoveKey(mob);
							}
						}
					}
					if (entity instanceof LivingEntity alive) {
						if (entity instanceof Drowned mob) {
							UUID target = mobTargets.get(mob.getUniqueId());
							if (target != null && mob.getTarget() == null)
								mob.setTarget(Bukkit.getPlayer(target));
							continue;
						}
						alive.addPotionEffects(effects);
						drowningEntities.add(alive);
					}
				}
				time -= 5;
				if (time <= 0)
					stop();
			}
		}.runTaskTimer(plugin, 0, 5));
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
						else if (damageTick == 0 && !entity.getEyeLocation().getBlock().isLiquid())
							EntityUtils.pureDamageEntity(entity, 1.0, "deaths.monsoon", DamageCause.DROWNING);
					});
					if (++damageTick == 10)
						damageTick = 0;
				}
			}.runTaskTimer(plugin, 1, 1));
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
						|| highest.getY() - loc.getBlockY() > 15)
					continue;
				if (highest.getY() - loc.getBlockY() <= 10)
					player.spawnParticle(Particle.FALLING_WATER, new Location(loc.getWorld(), block.getX() + 0.5, (loc.getY() > highest.getY() ? loc.getY() : highest.getY() + 3) + 8, block.getZ() + 0.5), 1, .5, 6.0, .5, 1);
				if (random.nextFloat() > waterLeakRate * currentStrength)
					continue;
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
						player.spawnParticle(VersionUtils.getDripWater(), highest.getLocation().add(.5, 1.0, .5), 1, .5, 0, .5, 1);
//						player.spawnParticle(Particle.FLAME, highest.getLocation().add(.5, 0.8, .5), 1, 0, 0, 0, 1);
						hollow = false;
					}
				}
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
					player.spawnParticle(Particle.FALLING_WATER, new Location(loc.getWorld(), block.getX() + 0.5, (loc.getY() > highest.getY() ? loc.getY() : highest.getY() + 5) + 8, block.getZ() + 0.5), 1, .5, 6.0, .5, 1);
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
		
		final BlockData puddleData = Material.WATER.createBlockData(data -> ((Levelled) data).setLevel(7));
		getChunksInvolvedSafelyAndThen(() -> {
			scheduleTask(new BukkitRunnable() {
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
						if (random.nextFloat() < puddleSpawnRate && block.getType().isSolid()) {
							Block above = block.getRelative(BlockFace.UP);
							if (above != null && above.getType() == Material.AIR)
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
						if (random.nextFloat() > blockChangeRate)
							return;
						Material[] materials = blockChanges.get(block.getType());
						if (materials != null && materials.length != 0) {
							Material change = materials[random.nextInt(materials.length)];
							new BukkitRunnable() {
								@Override
								public void run() {
									replaceBlockWithProperties(block, change);
								}
							}.runTask(plugin);
						}
					});
				}
			}.runTaskTimerAsynchronously(plugin, 0, 5));
		});
	}
	public void clean() {
		super.clean();
		mobs.forEach(uuid -> {
			Entity entity = Bukkit.getEntity(uuid);
			if (entity != null)
				entity.remove();
		});
		if (!Main.isDisablingPlugin)
			new BukkitRunnable() {
				private double regenTicks;
				private final Iterator<Block> iterator = puddles.iterator();
				
				@Override
				public void run() {
					regenTicks += puddleDryRate;
					while (regenTicks >= 1) {
						regenTicks -= 1;
						if (!iterator.hasNext()) {
							this.cancel();
							return;
						}
						Block block = iterator.next();
						if (block.getType() == Material.WATER && block.getBlockData() instanceof Levelled data && data.getLevel() == 7)
							block.setType(Material.AIR);
					}
				}
			}.runTaskTimer(plugin, 200, 1);
		else
			puddles.forEach(block -> {
				if (block.getType() == Material.WATER && block.getBlockData() instanceof Levelled data && data.getLevel() == 7)
					block.setType(Material.AIR);
			});
		HandlerList.unregisterAll(this);
	}
	public boolean isBlockInClimate(Block block) {
		final double temp = block.getTemperature();
		return temp > 0.15 && temp <= 0.95;
	}
	protected String getConfigPath() {
		return "disasters.weather.monsoon";
	}
	public double getRegenTickRate() {
		return 0.5;
	}
	public Set<Environment> getBannedEnvironments() {
		return Set.of(Environment.NETHER, Environment.THE_END);
	}
	
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onFlow(FluidLevelChangeEvent event) {
		if (puddles.contains(event.getBlock()))
			event.setCancelled(true);
	}
}
