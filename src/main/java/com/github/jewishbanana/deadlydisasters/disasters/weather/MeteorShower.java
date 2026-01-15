package com.github.jewishbanana.deadlydisasters.disasters.weather;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.github.jewishbanana.deadlydisasters.disasters.WeatherDisaster;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class MeteorShower extends WeatherDisaster implements Listener {
	
	private static final BlockFace[] adjacentFaces = new BlockFace[] { BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
	
	private int minHeight;
	private boolean setNight;
	private float meteorSpawnRate;
	private float meteorSizeMultiplier;
	private float meteorSpeedMultiplier;
	private int maxMeteors;
	private int smokeTime;
	private boolean regenerateMeteors;
	
	private float particleRate;
	private final Set<Meteor> activeMeteors = new HashSet<>();
	private final Map<UUID, Meteor> meteorBlocks = new HashMap<>();
	private final List<MeteorFactory> meteorFactory = new ArrayList<>();
	private long currentTime;
	
	public MeteorShower(@NotNull Location location, Player player, int level) {
		super(location, player, level);
		
		this.minHeight = getConfigInt("minimum_height");
	}
	public void init() {
		super.init();
		this.setNight = getConfigBoolean("set_to_night");
		this.meteorSpawnRate = (float) (0.034 * getConfigDouble("meteor_spawn_multiplier") * (scale / 2.0));
		this.meteorSizeMultiplier = (float) (1.0 * getConfigDouble("meteor_size_multiplier") * (scale / 2.0));
		this.meteorSpeedMultiplier = (float) (1.0 * getConfigDouble("meteor_speed_multiplier"));
		this.maxMeteors = getConfigInt("maximum_meteors");
		this.smokeTime = (int) (getConfigDouble("smoke_time") * 20.0);
		this.regenerateMeteors = getConfigBoolean("regenerate_meteors");
		
		final float minMeteorSize = 2 * meteorSizeMultiplier;
		final float maxMeteorSize = 6 * meteorSizeMultiplier;
		this.meteorFactory.add(new MeteorFactory(this, NormalMeteor.class, minMeteorSize, maxMeteorSize));
		this.meteorFactory.add(new MeteorFactory(this, ExplodingMeteor.class, minMeteorSize, maxMeteorSize));
		this.meteorFactory.add(new MeteorFactory(this, SplittingMeteor.class, minMeteorSize, maxMeteorSize));
		
		this.particleRate = (float) (1 * particleMultiplier);
		
		this.smoothingRange = 30f;
	}
	public boolean canStart() {
		if (getLocation().getBlockY() < minHeight)
			return false;
		return super.canStart();
	}
	public void start() {
		super.start();
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		location.setY(128);
		this.currentTime = world.getTime();
		scheduleTask(new BukkitRunnable() {
			@Override
			public void run() {
				if (activeMeteors.size() < maxMeteors)
					for (Entity entity : entitiesInMonitorArea) {
						if (!entity.isValid())
							continue;
						if (entity instanceof Player player) {
							if (EntityUtils.isPlayerImmune(player))
								continue;
						} else if (random.nextInt(100) != 0)
							continue;
						if (random.nextFloat() > meteorSpawnRate)
							continue;
						Location target = entity.getLocation().add(random.nextInt(-15, 15), 0, random.nextInt(-15, 15));
						Location spawn = entity.getLocation().add(random.nextInt(-50, 50), 0, random.nextInt(-50, 50));
						spawn.setY(Math.min(target.getBlockY() + 100, 320));
						activeMeteors.add(meteorFactory.get(random.nextInt(meteorFactory.size())).createMeteor(spawn, Utils.getVectorTowards(spawn, target).multiply(meteorSpeedMultiplier)));
					}
				if (currentTime < 19500)
					currentTime += 500;
				else if (currentTime > 20500)
					currentTime -= 500;
			}
		}.runTaskTimer(plugin, 60, 10));
		
		createAsyncEntityMonitor(e -> e instanceof LivingEntity && e.isValid(), 
				(found, entities, players) -> {
					found.forEach((entity, loc) -> {
						if (!isWithinStorm(loc))
							return;
						if (isEntityProtected(entity))
							return;
						if (loc.getY() >= minHeight)
							entities.add(entity);
					});
				});
		
		createParticleAsyncTask(player -> {
			if (!setNight)
				return;
			player.setPlayerTime(currentTime, false);
		}, pair -> {
			if (!setNight)
				return;
			final Player player = pair.getFirst();
			final Location loc = player.getLocation();
			final double actualDistance = new Location(world, loc.getX(), location.getY(), loc.getZ()).distance(location);
			long diff = (long) (((smoothingRange - (actualDistance - disasterRange)) * ((currentTime - world.getTime()) / smoothingRange)));
			player.setPlayerTime(world.getTime() + diff, false);
		});
	}
	public void clean() {
		super.clean();
		HandlerList.unregisterAll(this);
		activeMeteors.forEach(meteor -> meteor.markForDead = true);
		weatherPlayers.forEach(uuid -> {
			Player player = Bukkit.getPlayer(uuid);
			if (player == null || !player.isOnline())
				return;
			player.resetPlayerTime();
		});
	}
	public void removePlayerFromWeather(Player player) {
		super.removePlayerFromWeather(player);
		if (setNight)
			player.resetPlayerTime();
	}
	public boolean forceDownfallWeather() {
		return false;
	}
	protected String getConfigPath() {
		return "disasters.weather.meteor_shower";
	}
	public Set<Environment> getBannedEnvironments() {
		return EnumSet.of(Environment.NETHER, Environment.THE_END);
	}
	
	public class MeteorFactory {
		
		private final MeteorShower instance;
		private final Class<? extends Meteor> meteorClass;
		private final float minSize;
		private final float maxSize;
		
		public MeteorFactory(MeteorShower instance, Class<? extends Meteor> meteorClass, float minSize, float maxSize) {
			this.instance = instance;
			this.meteorClass = meteorClass;
			this.minSize = minSize;
			this.maxSize = maxSize;
		}
		public Meteor createMeteor(Location loc, Vector direction) {
			try {
				return meteorClass.getDeclaredConstructor(MeteorShower.class, Location.class, Vector.class, float.class).newInstance(instance, loc, direction, random.nextFloat(minSize, maxSize));
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	public abstract class Meteor {
		
		protected Location loc;
		protected Vector direction;
		protected final Vector normalizedSpeed;
		protected final double actualSpeed;
		protected final float size;
		protected final float sizeSquared;
		protected final FallingBlock centerBlock;
		protected Map<UUID, Location> blocks = new HashMap<>();
		protected int depth;
		protected Material[] materials;
		
		public boolean markForDead;
		
		public Meteor(Location loc, Vector direction, float size) {
			this.loc = loc;
			this.direction = direction;
			this.normalizedSpeed = direction.clone().normalize();
			this.actualSpeed = direction.length();
			this.size = size;
			this.sizeSquared = size * size;
			
			this.materials = getMaterials();
			centerBlock = world.spawnFallingBlock(loc, materials[random.nextInt(materials.length)].createBlockData());
			generateMeteor();
			
			final Meteor reference = this;
			new BukkitRunnable() {
				@Override
				public void run() {
					if (markForDead) {
						this.cancel();
						clean();
						activeMeteors.remove(reference);
						return;
					}
					tick();
				}
			}.runTaskTimer(plugin, 0, 1);
		}
		public void generateMeteor() {
			final double squaredInner = (size - 1.1) * (size - 1.1);
			final Vector vec = new Vector(this.loc.getX(), this.loc.getY(), this.loc.getZ());
			blocks.put(centerBlock.getUniqueId(), loc);
			for (float x = -size; x < size; x++)
				for (float y = -size; y < size; y++)
					for (float z = -size; z < size; z++) {
						final Vector temp = vec.clone().add(new Vector(x, y, z));
						final double distance = vec.distanceSquared(temp);
						if (distance > sizeSquared || distance < squaredInner)
							continue;
						Location spawnLoc = temp.toLocation(world);
						blocks.put(world.spawnFallingBlock(spawnLoc, materials[random.nextInt(materials.length)].createBlockData()).getUniqueId(), spawnLoc);
					}
			for (UUID uuid : blocks.keySet()) {
				Entity fb = Bukkit.getEntity(uuid);
				if (fb == null)
					continue;
				fb.setMetadata("dd-fb", plugin.getFixedMetadata());
				((FallingBlock) fb).setDropItem(false);
				fb.setGravity(false);
				meteorBlocks.put(uuid, this);
			}
		}
		public abstract Material[] getMaterials();
		
		public void tick() {
			if (centerBlock == null || !centerBlock.isValid()) {
				markForDead = true;
				stopMeteor();
				return;
			}
			Location center = centerBlock.getLocation();
			if (random.nextFloat() < particleRate)
				center.getWorld().spawnParticle(VersionUtils.getLargeSmoke(), center, (int) (size * 2), size, size, size, 0.001, null, true);
			final Map<UUID, Location> replaceMap = new HashMap<>();
			Iterator<Entry<UUID, Location>> it = blocks.entrySet().iterator();
			while (it.hasNext()) {
				Entry<UUID, Location> entry = it.next();
				Entity temp = Bukkit.getEntity(entry.getKey());
				if (temp == null || !temp.isValid()) {
					meteorBlocks.remove(entry.getKey());
					Location last = entry.getValue();
					it.remove();
					Block towards = BlockUtils.getCenterOfBlock(last.getBlock()).add(direction.clone().normalize()).getBlock();
					if (towards == null)
						continue;
					depth--;
					if (!towards.isPassable())
						if (!removeBlock(towards, true, true))
							continue;
					Block lower = towards.getRelative(BlockFace.DOWN);
					if (lower != null && !lower.isPassable()) {
						if (!removeBlock(towards, true, true))
							continue;
					}
					FallingBlock replace = last.getWorld().spawnFallingBlock(last, temp != null ? ((FallingBlock) temp).getBlockData() : materials[random.nextInt(materials.length)].createBlockData());
					replace.setMetadata("dd-fb", plugin.getFixedMetadata());
					replace.setDropItem(false);
					replace.setGravity(false);
					replace.setVelocity(direction);
					meteorBlocks.put(replace.getUniqueId(), this);
					replaceMap.put(replace.getUniqueId(), last);
					continue;
				}
				temp.setVelocity(direction);
				Location current = temp.getLocation();
				entry.setValue(current.clone());
				for (int i=0; i < actualSpeed+1; i++) {
					current.add(normalizedSpeed);
					Block b = current.getBlock();
					removeAdjacentFaces(b);
				}
			}
			blocks.putAll(replaceMap);
			for (Entity entity : centerBlock.getNearbyEntities(size, size, size)) {
				Location loc = entity.getLocation();
				if (entity instanceof LivingEntity living 
						&& entity.isValid() 
						&& loc.distanceSquared(center) <= sizeSquared 
						&& !isEntityProtected(entity) 
						&& !EntityUtils.isEntityImmunePlayer(entity) 
						&& BlockUtils.rayTraceForBlock(center, loc.add(0, entity.getHeight(), 0), size) == null)
					EntityUtils.damageEntity(living, 20.0, "deaths.meteor", DamageCause.FALLING_BLOCK);
			}
			if (random.nextInt(7) == 0)
				for (Entity entity : centerBlock.getNearbyEntities(size * 5, size * 5, size * 5))
					if (entity instanceof Player player) {
						Location playerLoc = player.getLocation();
						playSound(player, playerLoc.clone().add(Utils.getVectorTowards(playerLoc, center).multiply(5.0)), Sound.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS, 1.0 / (size * 5.0) * playerLoc.distance(center), 2);
					}
			if (depth <= 0) {
				markForDead = true;
				stopMeteor();
			}
		}
		public void removeAdjacentFaces(Block block) {
			if (!block.isPassable() || block.isLiquid()) {
				if (!removeBlock(block, true, true))
					return;
				depth--;
			}
			for (BlockFace face : adjacentFaces) {
				Block area = block.getRelative(face);
				if (!area.isPassable() || block.isLiquid()) {
					if (!removeBlock(area, true, true))
						continue;
					depth--;
					if (random.nextInt(6) == 0)
						removeAdjacentFaces(area);
				}
			}
		}
		
		public abstract void stopMeteor();
		
		public void clean() {
			for (UUID uuid : blocks.keySet()) {
				Entity temp = Bukkit.getEntity(uuid);
				if (temp != null)
					temp.remove();
			}
		}
		public void createSmokeField(Location loc, int amount, float area) {
			scheduleTask(new BukkitRunnable() {
				private int time = smokeTime;
				private final World world = loc.getWorld();
				
				@Override
				public void run() {
					world.spawnParticle(VersionUtils.getLargeSmoke(), loc, amount, area, area, area, .001);
					if (--time <= 0)
						this.cancel();
				}
			}.runTaskTimerAsynchronously(plugin, 0, 1));
		}
	}
	public class NormalMeteor extends Meteor {
		
		public NormalMeteor(Location loc, Vector direction, float size) {
			super(loc, direction, size);
			this.depth = (int) (size * 800);
		}
		public void stopMeteor() {
			for (UUID uuid : blocks.keySet()) {
				Entity temp = Bukkit.getEntity(uuid);
				if (temp == null || temp.equals(centerBlock))
					continue;
				Block tempBlock = temp.getLocation().getBlock();
				if (getModifiedBlocks().contains(tempBlock)) {
					temp.remove();
					continue;
				}
				if (regenerateMeteors)
					placeBlock(tempBlock, ((FallingBlock) temp).getBlockData(), true, true);
				else
					tempBlock.setBlockData(((FallingBlock) temp).getBlockData());
				temp.remove();
			}
			Location first = centerBlock.getLocation();
			centerBlock.remove();
			final Vector vec = new Vector(first.getX(), first.getY(), first.getZ());
			final double squaredInner = Math.max(size - 1.5, 1) * Math.max(size - 1.5, 1);
			for (float x = -size; x < size; x++)
				for (float y = -size; y < size; y++)
					for (float z = -size; z < size; z++) {
						final Vector temp = vec.clone().add(new Vector(x, y, z));
						final double distance = vec.distanceSquared(temp);
						if (distance > sizeSquared)
							continue;
						Block b = temp.toLocation(world).getBlock();
						if (b == null || !b.isPassable())
							continue;
						if (distance < squaredInner) {
							final int roll = random.nextInt(100);
							if (roll > 14) {
								if (regenerateMeteors)
									placeBlock(b, materials[random.nextInt(materials.length)].createBlockData(), true, true);
								else
									b.setBlockData(materials[random.nextInt(materials.length)].createBlockData());
							} else if (roll > 10) {
								if (regenerateMeteors)
									placeBlock(b, Material.DEEPSLATE_COPPER_ORE.createBlockData(), true, true);
								else
									b.setBlockData(Material.DEEPSLATE_COPPER_ORE.createBlockData());
							} else if (roll > 4) {
								if (regenerateMeteors)
									placeBlock(b, Material.DEEPSLATE_IRON_ORE.createBlockData(), true, true);
								else
									b.setBlockData(Material.DEEPSLATE_IRON_ORE.createBlockData());
							} else if (roll > 0) {
								if (regenerateMeteors)
									placeBlock(b, Material.DEEPSLATE_GOLD_ORE.createBlockData(), true, true);
								else
									b.setBlockData(Material.DEEPSLATE_GOLD_ORE.createBlockData());
							} else {
								if (regenerateMeteors)
									placeBlock(b, Material.DEEPSLATE_DIAMOND_ORE.createBlockData(), true, true);
								else
									b.setBlockData(Material.DEEPSLATE_DIAMOND_ORE.createBlockData());
							}
						} else {
							if (regenerateMeteors)
								placeBlock(b, materials[random.nextInt(materials.length)].createBlockData(), true, true);
							else
								b.setBlockData(materials[random.nextInt(materials.length)].createBlockData());
						}
					}
			createSmokeField(first, (int) (size * 5), size + 1);
		}
		public Material[] getMaterials() {
			return new Material[] { Material.DEEPSLATE, Material.DEEPSLATE, Material.STONE };
		}
	}
	public class ExplodingMeteor extends Meteor {
		
		public ExplodingMeteor(Location loc, Vector direction, float size) {
			super(loc, direction, size);
			this.depth = (int) (size * 500);
		}
		public void stopMeteor() {
			Location first = centerBlock.getLocation();
			clean();
			final double explosionRadius = size * 3;
			final double perimeterCheck = (explosionRadius - 1) * (explosionRadius - 1);
			for (Block b : BlockUtils.getBlocksInSphereRadius(first, (float) explosionRadius)) {
				if (BlockUtils.getCenterOfBlock(b).distanceSquared(first) < perimeterCheck) {
					removeBlock(b);
					if (random.nextInt(300) == 0)
						b.getWorld().spawnParticle(VersionUtils.getHugeExplosion(), BlockUtils.getCenterOfBlock(b), 1, .5, .5, .5, 1, null, true);
				} else {
					if (random.nextInt(7) == 0 && !b.isPassable())
						placeBlock(b, materials[random.nextInt(materials.length)]);
					else
						removeBlock(b);
				}
			}
			for (Entity entity : first.getWorld().getNearbyEntities(first, size * 6, size * 6, size * 6))
				if (entity instanceof Player player) {
					Location playerLoc = player.getLocation();
					playSound(player, playerLoc.clone().add(Utils.getVectorTowards(playerLoc, first).multiply(5.0)), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.0 / (size * 6.0) * playerLoc.distance(first), 0.5);
				}
			final double explosionRadiusSquared = explosionRadius * explosionRadius;
			for (Entity entity : first.getWorld().getNearbyEntities(first, explosionRadius, explosionRadius, explosionRadius, temp -> temp.isValid())) {
				Location loc = entity.getLocation();
				if (entity instanceof LivingEntity living 
						&& !EntityUtils.isEntityImmunePlayer(entity) 
						&& loc.distanceSquared(first) <= explosionRadiusSquared 
						&& BlockUtils.rayTraceForBlock(first, loc.add(0, entity.getHeight(), 0), explosionRadius) == null)
					EntityUtils.damageEntity(living, 40.0 / explosionRadius * (explosionRadius - loc.distance(first)), "deaths.exploding_meteor", DamageCause.BLOCK_EXPLOSION);
			}
			createSmokeField(first.clone().subtract(0, explosionRadius, 0), (int) (size * 5), (float) explosionRadius);
		}
		public Material[] getMaterials() {
			return new Material[] { Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.DEEPSLATE, Material.NETHERRACK };
		}
	}
	public class SplittingMeteor extends Meteor {
		
		public SplittingMeteor(Location loc, Vector direction, float size) {
			super(loc, direction, size);
			this.depth = (int) (size * 150);
		}
		public void stopMeteor() {
			Location first = centerBlock.getLocation();
			List<Entity> flyingBlocks = new ArrayList<>();
			for (UUID uuid : blocks.keySet()) {
				Entity temp = Bukkit.getEntity(uuid);
				if (temp == null)
					continue;
				if (temp.equals(centerBlock)) {
					centerBlock.remove();
					continue;
				}
				temp.setGravity(true);
				temp.setVelocity(Utils.getVectorTowards(first, temp.getLocation()).add(new Vector(random.nextFloat(-.05f, .05f), random.nextFloat(-.05f, .05f), random.nextFloat(-.05f, .05f))).multiply(0.75));
				flyingBlocks.add(temp);
			}
			blocks.clear();
			first.getWorld().spawnParticle(Particle.CLOUD, first, (int) (size*6), size, size, size, 0.1, null, true);
			for (Entity entity : first.getWorld().getNearbyEntities(first, size * 6, size * 6, size * 6))
				if (entity instanceof Player player) {
					Location playerLoc = player.getLocation();
					playSound(player, playerLoc.clone().add(Utils.getVectorTowards(playerLoc, first).multiply(5.0)), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.0 / (size * 6.0) * playerLoc.distance(first), 1.0);
				}
			new BukkitRunnable() {
				private int timeout = 80;
				
				@Override
				public void run() {
					Iterator<Entity> it = flyingBlocks.iterator();
					while (it.hasNext()) {
						Entity fb = it.next();
						if (fb == null || !fb.isValid()) {
							it.remove();
							continue;
						}
						for (Entity entity : fb.getNearbyEntities(.5, .5, .5))
							if (entity instanceof LivingEntity living && !EntityUtils.isEntityImmunePlayer(entity))
								EntityUtils.damageEntity(living, 6.0, "deaths.meteor", DamageCause.FALLING_BLOCK);
					}
					if (--timeout == 0)
						this.cancel();
				}
			}.runTaskTimer(plugin, 0, 5);
		}
		public Material[] getMaterials() {
			return new Material[] { Material.CALCITE, Material.CALCITE, Material.DEEPSLATE };
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
    public void onFallingBlockForm(EntityChangeBlockEvent event) {
		if (event.getEntity() instanceof FallingBlock fb && meteorBlocks.containsKey(fb.getUniqueId())) {
			event.setCancelled(true);
			Meteor meteor = meteorBlocks.remove(fb.getUniqueId());
			Block towards = BlockUtils.getCenterOfBlock(event.getBlock()).add(meteor.direction.clone().normalize()).getBlock();
			if (towards == null)
				return;
			meteor.blocks.remove(fb.getUniqueId());
			if (meteor.depth-- <= 0)
				return;
			if (!towards.isPassable())
				if (!removeBlock(towards, true, true)) {
					meteor.depth = 0;
					return;
				}
			for (BlockFace face : adjacentFaces) {
				Block temp = towards.getRelative(face);
				if (temp == null || temp.isPassable())
					continue;
				if (!removeBlock(towards, true, true)) {
					meteor.depth = 0;
					return;
				}
			}
			FallingBlock replace = fb.getWorld().spawnFallingBlock(fb.getLocation(), fb.getBlockData());
			replace.setMetadata("dd-fb", plugin.getFixedMetadata());
			replace.setDropItem(false);
			replace.setGravity(false);
			replace.setVelocity(meteor.direction);
			meteorBlocks.put(replace.getUniqueId(), meteor);
			meteor.blocks.put(replace.getUniqueId(), replace.getLocation());
		}
	}
}
