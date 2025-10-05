package com.github.jewishbanana.deadlydisasters.disasters.weather;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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
	
	private int minHeight;
	private boolean setNight;
	private float meteorSpawnRate;
	private float meteorSizeMultiplier;
	private float meteorSpeedMultiplier;
	private int maxMeteors;
	private int smokeTime;
	
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
		this.meteorSpawnRate = (float) (0.005 * getConfigDouble("meteor_spawn_multiplier") * scale);
		this.meteorSizeMultiplier = (float) (1.0 * getConfigDouble("meteor_size_multiplier") * (scale / 2.0));
		this.meteorSpeedMultiplier = (float) (1.0 * getConfigDouble("meteor_speed_multiplier"));
		this.smokeTime = (int) (getConfigDouble("smoke_time") * 20.0);
		this.maxMeteors = getConfigInt("maximum_meteors");
		
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
		final Map<Entity, Location> foundEntities = new ConcurrentHashMap<>();
		final Set<Entity> entitiesInStorm = ConcurrentHashMap.newKeySet();
		final World world = location.getWorld();
		this.currentTime = world.getTime();
		final AtomicBoolean processEntities = new AtomicBoolean();
		scheduleTask(new BukkitRunnable() {
			private int meteorCount;
			
			@Override
			public void run() {
				processEntities.set(false);
				foundEntities.clear();
				for (Entity entity : world.getNearbyEntities(location, disasterRange, 193, disasterRange, e -> e.isValid() && e instanceof LivingEntity))
					foundEntities.put(entity, entity.getLocation());
				final Set<Entity> currentEntities = Set.copyOf(entitiesInStorm);
				processEntities.set(true);
				if (meteorCount < maxMeteors)
					for (Entity entity : currentEntities) {
						if (entity instanceof Player player) {
							if (EntityUtils.isPlayerImmune(player))
								continue;
						} else if (random.nextInt(4) != 0)
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
					if (isEntityProtected(entity))
						return;
					if (loc.getY() >= minHeight)
						set.add(entity);
				});
				entitiesInStorm.clear();
				entitiesInStorm.addAll(set);
			}
		}.runTaskTimerAsynchronously(plugin, 1, 1));
		
		createParticleAsyncTask(player -> {
			if (!setNight)
				return;
			player.setPlayerTime(currentTime, false);
		}, pair -> {
			if (!setNight)
				return;
			final Player player = pair.getFirst();
			final Location loc = player.getLocation();
			final double actualDistance = new Location(loc.getWorld(), loc.getX(), location.getY(), loc.getZ()).distance(location);
			long diff = (long) (((smoothingRange - (actualDistance - disasterRange)) * ((currentTime - world.getTime()) / smoothingRange)));
			player.setPlayerTime(world.getTime() + diff, false);
		});
	}
	public void clean() {
		super.clean();
		activeMeteors.forEach(meteor -> meteor.clean());
		HandlerList.unregisterAll(this);
		weatherPlayers.forEach(uuid -> {
			Player player = Bukkit.getPlayer(uuid);
			if (player == null || !player.isOnline())
				return;
			player.resetPlayerTime();
		});
	}
	public void addPlayerToWeather(Player player) {
		super.addPlayerToWeather(player);
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
		return "disasters.weather.meteorshower";
	}
	public double getRegenTickRate() {
		return 0.5;
	}
	public Set<Environment> getBannedEnvironments() {
		return Set.of(Environment.NETHER, Environment.THE_END);
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
		protected final float size;
		protected final float sizeSquared;
		protected List<FallingBlock> blocks = new ArrayList<>();
		protected int depth;
		protected Material[] materials;
		
		public boolean markForDead;
		
		public Meteor(Location loc, Vector direction, float size) {
			this.loc = loc;
			this.direction = direction;
			this.size = size;
			this.sizeSquared = size * size;
			
			this.materials = getMaterials();
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
			final double squaredInner = (size - 1.5) * (size - 1.5);
			final Vector vec = new Vector(this.loc.getX(), this.loc.getY(), this.loc.getZ());
			blocks.add(loc.getWorld().spawnFallingBlock(loc, materials[random.nextInt(materials.length)].createBlockData()));
			for (float x = -size; x < size; x++)
				for (float y = -size; y < size; y++)
					for (float z = -size; z < size; z++) {
						final Vector temp = vec.clone().add(new Vector(x, y, z));
						final double distance = vec.distanceSquared(temp);
						if (distance > sizeSquared || distance < squaredInner)
							continue;
						blocks.add(loc.getWorld().spawnFallingBlock(temp.toLocation(loc.getWorld()), materials[random.nextInt(materials.length)].createBlockData()));
					}
			for (FallingBlock fb : blocks) {
				fb.setMetadata("dd-fb", plugin.getFixedMetadata());
				fb.setDropItem(false);
				fb.setGravity(false);
				meteorBlocks.put(fb.getUniqueId(), this);
			}
		}
		public abstract Material[] getMaterials();
		
		public void tick() {
			FallingBlock first = blocks.get(0);
			if (first == null || !first.isValid()) {
				markForDead = true;
				return;
			}
			Location temp = first.getLocation();
			if (random.nextFloat() < particleRate)
				temp.getWorld().spawnParticle(VersionUtils.getLargeSmoke(), temp, (int) (size * 2), size, size, size, 0.001, null, true);
			for (FallingBlock fb : blocks)
				fb.setVelocity(direction);
			for (Entity entity : first.getNearbyEntities(size, size, size))
				if (entity instanceof LivingEntity living 
						&& entity.isValid() 
						&& entity.getLocation().distanceSquared(temp) <= sizeSquared 
						&& !isEntityProtected(entity) 
						&& !EntityUtils.isEntityImmunePlayer(entity) 
						&& BlockUtils.rayTraceForBlock(temp, entity.getLocation().add(0, entity.getHeight(), 0), size) == null)
					EntityUtils.damageEntity(living, 20.0, "deaths.meteor", DamageCause.FALLING_BLOCK);
			if (random.nextInt(7) == 0)
				for (Entity entity : first.getNearbyEntities(size * 5, size * 5, size * 5))
					if (entity instanceof Player player) {
						Location playerLoc = player.getLocation();
						playSound(player, playerLoc.clone().add(Utils.getVectorTowards(playerLoc, temp).multiply(5.0)), Sound.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS, 1.0 / (size * 5.0) * playerLoc.distance(temp), 2);
					}
			if (depth <= 0) {
				markForDead = true;
				stopMeteor();
			}
		}
		
		public abstract void stopMeteor();
		
		public void clean() {
			for (FallingBlock temp : blocks)
				if (temp != null)
					temp.remove();
		}
		public void createSmokeField(Location loc, int amount, float area) {
			scheduleTask(new BukkitRunnable() {
				private int time = smokeTime;
				
				@Override
				public void run() {
					loc.getWorld().spawnParticle(VersionUtils.getLargeSmoke(), loc, amount, area, area, area, .001);
					if (--time <= 0)
						this.cancel();
				}
			}.runTaskTimerAsynchronously(plugin, 0, 1));
		}
	}
	public class NormalMeteor extends Meteor {
		
		public NormalMeteor(Location loc, Vector direction, float size) {
			super(loc, direction, size);
			this.depth = (int) (size * 2);
		}
		public void stopMeteor() {
			for (int i=1; i < blocks.size(); i++) {
				FallingBlock temp = blocks.get(i);
				placeBlock(temp.getLocation().getBlock(), temp.getBlockData().getMaterial());
				temp.remove();
			}
			Location first = blocks.get(0).getLocation();
			blocks.get(0).remove();
			final Vector vec = new Vector(first.getX(), first.getY(), first.getZ());
			final double squaredInner = Math.max(size - 1.5, 1) * Math.max(size - 1.5, 1);
			for (float x = -size; x < size; x++)
				for (float y = -size; y < size; y++)
					for (float z = -size; z < size; z++) {
						final Vector temp = vec.clone().add(new Vector(x, y, z));
						final double distance = vec.distanceSquared(temp);
						if (distance > sizeSquared)
							continue;
						Block b = temp.toLocation(loc.getWorld()).getBlock();
						if (b == null || !b.isPassable())
							continue;
						if (distance < squaredInner) {
							final int roll = random.nextInt(100);
							if (roll > 14)
								placeBlock(b, materials[random.nextInt(materials.length)]);
							else if (roll > 10)
								placeBlock(b, Material.DEEPSLATE_COPPER_ORE);
							else if (roll > 4)
								placeBlock(b, Material.DEEPSLATE_IRON_ORE);
							else if (roll > 0)
								placeBlock(b, Material.DEEPSLATE_GOLD_ORE);
							else
								placeBlock(b, Material.DEEPSLATE_DIAMOND_ORE);
						} else
							placeBlock(b, materials[random.nextInt(materials.length)]);
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
			this.depth = (int) (size * 2);
		}
		public void stopMeteor() {
			Location first = blocks.get(0).getLocation();
			clean();
			final double explosionRadius = size * 3;
			final double perimeterCheck = (explosionRadius - 1) * (explosionRadius - 1);
			for (Block b : BlockUtils.getBlocksInSphereRadius(first, explosionRadius)) {
				if (BlockUtils.getCenterOfBlock(b).distanceSquared(first) < perimeterCheck) {
					removeBlock(b);
					if (random.nextInt(10) == 0)
						b.getWorld().spawnParticle(VersionUtils.getHugeExplosion(), BlockUtils.getCenterOfBlock(b), 1, .5, .5, .5, 1, null, true);
				} else {
					if (random.nextInt(7) == 0)
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
			for (Entity entity : first.getWorld().getNearbyEntities(first, explosionRadius, explosionRadius, explosionRadius, temp -> temp.isValid()))
				if (entity instanceof LivingEntity living 
						&& !EntityUtils.isEntityImmunePlayer(entity) 
						&& entity.getLocation().distanceSquared(first) <= explosionRadiusSquared 
						&& BlockUtils.rayTraceForBlock(first, entity.getLocation().add(0, entity.getHeight(), 0), explosionRadius) == null)
					EntityUtils.damageEntity(living, 40.0 / explosionRadius * (explosionRadius - entity.getLocation().add(0, entity.getHeight(), 0).distance(first)), "deaths.exploding_meteor", DamageCause.BLOCK_EXPLOSION);
			createSmokeField(first.clone().subtract(0, explosionRadius, 0), (int) (size * 5), (float) explosionRadius);
		}
		public Material[] getMaterials() {
			return new Material[] { Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.DEEPSLATE, Material.NETHERRACK };
		}
	}
	public class SplittingMeteor extends Meteor {
		
		public SplittingMeteor(Location loc, Vector direction, float size) {
			super(loc, direction, size);
			this.depth = (int) (size * 2);
		}
		public void stopMeteor() {
			FallingBlock inner = blocks.get(0);
			Location first = inner.getLocation();
			for (FallingBlock fb : blocks) {
				if (fb.equals(inner)) {
					inner.remove();
					continue;
				}
				fb.setGravity(true);
				fb.setVelocity(Utils.getVectorTowards(first, fb.getLocation()).multiply(0.75));
			}
			Queue<FallingBlock> flyingBlocks = new ArrayDeque<>(blocks);
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
					Iterator<FallingBlock> it = flyingBlocks.iterator();
					while (it.hasNext()) {
						FallingBlock fb = it.next();
						if (fb == null || !fb.isValid()) {
							it.remove();
							continue;
						}
						for (Entity entity : fb.getNearbyEntities(.5, .5, .5))
							if (entity instanceof LivingEntity living && entity.isValid() && !EntityUtils.isEntityImmunePlayer(entity))
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
	
	private BlockFace[] faces = new BlockFace[] { BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
	
	@EventHandler(priority = EventPriority.LOWEST)
    public void onFallingBlockForm(EntityChangeBlockEvent event) {
		if (event.getEntity() instanceof FallingBlock fb && meteorBlocks.containsKey(fb.getUniqueId())) {
			event.setCancelled(true);
			Vector vec = fb.getVelocity();
			Block towards = BlockUtils.getCenterOfBlock(event.getBlock()).add(vec.normalize()).getBlock();
			if (towards == null)
				return;
			Meteor meteor = meteorBlocks.remove(fb.getUniqueId());
			meteor.blocks.remove(fb);
			if (meteor.depth-- <= 0)
				return;
			if (!towards.isPassable())
				if (!removeBlock(towards)) {
					meteor.depth = 0;
					return;
				}
			for (BlockFace face : faces) {
				Block temp = towards.getRelative(face);
				if (temp == null || temp.isPassable())
					continue;
				if (!removeBlock(towards)) {
					meteor.depth = 0;
					return;
				}
			}
			FallingBlock replace = fb.getWorld().spawnFallingBlock(fb.getLocation(), fb.getBlockData());
			replace.setMetadata("dd-fb", plugin.getFixedMetadata());
			replace.setDropItem(false);
			replace.setGravity(false);
			meteorBlocks.put(replace.getUniqueId(), meteor);
			meteor.blocks.add(replace);
		}
	}
}
