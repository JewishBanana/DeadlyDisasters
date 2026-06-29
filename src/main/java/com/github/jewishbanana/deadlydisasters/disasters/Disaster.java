package com.github.jewishbanana.deadlydisasters.disasters;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import com.github.jewishbanana.deadlydisasters.DeadlyDisasters;
import com.github.jewishbanana.deadlydisasters.WorldWrapper;
import com.github.jewishbanana.deadlydisasters.events.DisasterStartEvent;
import com.github.jewishbanana.deadlydisasters.events.DisasterStartEvent.DisasterStartReason;
import com.github.jewishbanana.deadlydisasters.events.DisasterStopEvent;
import com.github.jewishbanana.deadlydisasters.events.DisasterStopEvent.DisasterStopReason;
import com.github.jewishbanana.deadlydisasters.listeners.DeathMessageHandler;
import com.github.jewishbanana.deadlydisasters.listeners.EntitiesListener;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

import io.papermc.lib.PaperLib;

public abstract class Disaster {
	
	protected static final DeadlyDisasters plugin;
	protected static final RandomGenerator random;
	public static final List<Disaster> onGoingDisasters;
	static {
		plugin = DeadlyDisasters.getInstance();
		random = RandomGenerator.of("SplittableRandom");
		onGoingDisasters = new ArrayList<>();
	}
	
	private List<BukkitTask> tasks = new ArrayList<>();
	private WorldWrapper worldLink;
	private float volume = 1f;
	private boolean regionsProtected;
	private boolean affectEntitiesInRegions;
	private Set<EntityType> blacklistedEntityTypes;
	private boolean hasEnded;
	private final List<UUID> fallingBlocks = new ArrayList<>();
	
	protected List<Entity> entitiesInMonitorArea;
	protected List<Player> playersInMonitorArea;
	
	protected Location location;
	protected Player player;
	protected int level;
	protected double disasterRange;
	protected int startDelayTicks;
	protected World world;

	public Disaster(@NotNull Location location, @Nullable Player player, int level) {
		this.location = location;
		this.world = location.getWorld();
		this.player = player;
		this.setLevel(level);
		
		this.worldLink = WorldWrapper.getWorldWrapper(this.world);
	}
	public void init() {
		if (getConfigPath() != null) {
			this.volume = (float) getConfigOverrideDouble("volume");
			this.startDelayTicks = (int) (getConfigOverrideDouble("start_delay") * 20.0);
			this.setLevel(Math.min(level, getConfigOverrideInt("max_level")));
			List<String> list = new ArrayList<>(getConfigStringList("blacklisted_mob_types"));
			// Merge the shared global blacklist when respect_global_mob_blacklist is true (default).
			if (getConfigOverrideBoolean("respect_global_mob_blacklist")) {
				for (String entry : worldLink.getConfigStringList("disasters.global.blacklisted_mob_types"))
					if (!list.contains(entry))
						list.add(entry);
			}
			Set<EntityType> types = new HashSet<>();
			list.forEach(type -> {
				try {
					EntityType typeCast = EntityType.valueOf(type.toUpperCase());
					types.add(typeCast);
				} catch (IllegalArgumentException e) {
					Utils.sendConsoleMessage("&cERROR no such entity type named &e'"+type+"' &cin the &d'"+worldLink.getConfigName()+"' &cworld config file at &b'"+getConfigPath()+".blacklisted_mob_types' &clist! This value will be omitted and the entity type will be affected by the disaster.");
				}
			});
			blacklistedEntityTypes = types.isEmpty() ? EnumSet.noneOf(EntityType.class) : EnumSet.copyOf(types);
		}
		this.regionsProtected = worldLink.getConfigBoolean("protection_settings.region_plugins.protect_region_from_damage");
		this.affectEntitiesInRegions = worldLink.getConfigBoolean("protection_settings.region_plugins.allow_disaster_effects_in_regions");
	}
	public Location findPossiblePosition(Location initial) {
		if (initial == null)
			return null;
		Block temp = BlockUtils.getHighestExposedBlock(initial.getBlock(), 255);
		return temp == null ? null : BlockUtils.getCenterOfBlock(temp);
	}
	public boolean canStart(DisasterStartReason reason) {
		DisasterStartEvent event = new DisasterStartEvent(this, reason);
		Bukkit.getPluginManager().callEvent(event);
		return !event.isCancelled() && !DependencyUtils.isDisasterStartBlocked(location);
	}
	public boolean canStart() {
		return canStart(DisasterStartReason.NATURAL);
	}
	public void broadcastDisaster() {
		if (worldLink.getConfigBoolean("world.broadcast_disasters")) {
			String message = Utils.convertString(DataUtils.getLanguageString(getBroadcastMessageConfigPath())
					.replaceAll("%disaster%", getDisplayName())
					.replaceAll("%location%", location.getBlockX()+" "+location.getBlockY()+' '+location.getBlockZ())
					.replaceAll("%player%", player != null ? player.getDisplayName() : "")
					.replaceAll("%level%", level+""));
			location.getWorld().getPlayers().forEach(p -> p.sendMessage(message));
			DeadlyDisasters.consoleSender.sendMessage(message+Utils.convertString(" &d("+location.getWorld().getName()+')'));
		}
		if (worldLink.getConfigBoolean("world.disaster_tips")) {
			String message = getDisasterTip();
			if (message != null) {
				message = Utils.convertString(message);
				for (Player p : location.getWorld().getPlayers())
					p.sendMessage(message);
			}
		}
		worldLink.playDisasterStartSound(location, player, disasterRange);
	}
	public boolean softStart() {
		if (getBannedEnvironments().contains(location.getWorld().getEnvironment())
				|| (getFrequency() != 1f && getFrequency() > random.nextFloat()))
			return false;
		Location temp = findPossiblePosition(location);
		if (temp == null)
			return false;
		setLocation(temp);
		if (!canStart())
			return false;
		if (DependencyUtils.isRealisticSeasonsEnabled() && !DependencyUtils.isDisasterInSeason(getClass(), location.getWorld()))
			return false;
		init();
		broadcastDisaster();
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> start(), startDelayTicks);
		return true;
	}
	public void start() {
		onGoingDisasters.add(this);
//		Bukkit.broadcastMessage("disaster started");
	}
	public boolean stop(DisasterStopReason reason) {
		DisasterStopEvent event = new DisasterStopEvent(this, reason);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled())
			return false;
		hasEnded = true;
		clean();
		onGoingDisasters.remove(this);
//		Bukkit.broadcastMessage("disaster ended");
		return true;
	}
	public boolean stop() {
		return stop(DisasterStopReason.DISASTER_ENDING);
	}
	public void clean() {
		tasks.forEach(task -> task.cancel());
		if (this instanceof MobDisaster cast)
			cast.cleanEntities();
	}
	public boolean removeBlock(Block block, boolean ignoreImmuneOnly, boolean withPhysics, ThreadLocalRandom rng) {
		if ((regionsProtected && isBlockProtected(block)) 
				|| (ignoreImmuneOnly ? BlockUtils.isBlockImmune(block) : BlockUtils.doesBlockResist(block, rng)))
			return false;
		block.setType(Material.AIR, withPhysics);
		return true;
	}
	public boolean removeBlock(Block block, boolean ignoreImmuneOnly, boolean withPhysics) {
		if ((regionsProtected && isBlockProtected(block)) 
				|| (ignoreImmuneOnly ? BlockUtils.isBlockImmune(block) : BlockUtils.doesBlockResist(block)))
			return false;
		block.setType(Material.AIR, withPhysics);
		return true;
	}
	public boolean removeBlock(Block block) {
		return removeBlock(block, false, true);
	}
	public boolean placeBlock(Block block, BlockData data, boolean ignoreImmuneOnly, boolean withPhysics) {
		if ((regionsProtected && (ignoreImmuneOnly ? BlockUtils.isBlockImmune(block) : BlockUtils.doesBlockResist(block))) 
				|| BlockUtils.doesBlockResist(block))
			return false;
		block.setBlockData(data, withPhysics);
		return true;
	}
	public boolean placeBlock(Block block, Material material) {
		return placeBlock(block, material.createBlockData(), false, true);
	}
	public boolean replaceBlockWithProperties(Block block, Material material, boolean withPhysics) {
		BlockData data = material.createBlockData();
		block.getBlockData().copyTo(data);
		return placeBlock(block, data, false, withPhysics);
	}
	public boolean replaceBlockWithProperties(Block block, Material material) {
		return replaceBlockWithProperties(block, material, true);
	}
	public boolean moveBlock(Block from, Block to, boolean withPhysics) {
		if ((regionsProtected && (isBlockProtected(from) || isBlockProtected(to))) 
				|| BlockUtils.doesBlockResist(from) 
				|| BlockUtils.doesBlockResist(to))
			return false;
		BlockState fromState = from.getState();
		to.setBlockData(fromState.getBlockData(), withPhysics);
		if (fromState instanceof InventoryHolder fromHolder)
			((InventoryHolder) to.getState()).getInventory().setContents(fromHolder.getInventory().getContents());
		from.setType(Material.AIR, withPhysics);
	    return true;
	}
	public boolean moveBlock(Block from, Block to) {
		return moveBlock(from, to, true);
	}
	public FallingBlock createFallingBlock(Location location, BlockData data) {
		FallingBlock entity = location.getWorld().spawnFallingBlock(location, data);
		EntityUtils.markFallingBlock(entity);
		EntitiesListener.attachRemoveKey(entity);
		if (entity != null)
			fallingBlocks.add(entity.getUniqueId());
		return entity;
	}
	public FallingBlock convertBlockIntoFallingBlock(Block block) {
		if ((regionsProtected && isBlockProtected(block)) 
				|| BlockUtils.doesBlockResist(block))
			return null;
		BlockState state = block.getState();
		FallingBlock entity = block.getWorld().spawnFallingBlock(BlockUtils.getCenterOfBlock(block), state.getBlockData());
		EntityUtils.markFallingBlock(entity);
		EntitiesListener.attachRemoveKey(entity);
		if (entity != null) {
			fallingBlocks.add(entity.getUniqueId());
		}
		return entity;
	}
	public boolean isBlockProtected(Block block) {
		return DependencyUtils.isRegionProtected(block.getLocation());
	}
	public boolean isEntityProtected(Entity entity) {
		return blacklistedEntityTypes.contains(entity.getType())
				|| (!affectEntitiesInRegions && DependencyUtils.isEntityProtected(entity));
	}
	public void playSound(Location loc, Sound sound, SoundCategory category, float vol, float pitch) {
		loc.getWorld().playSound(loc, sound, category, vol * volume, pitch);
	}
	public void playSound(Location loc, Sound sound, float vol, float pitch) {
		playSound(loc, sound, SoundCategory.MASTER, vol, pitch);
	}
	public void playSound(Player player, Location loc, Sound sound, SoundCategory category, float vol, float pitch) {
		player.playSound(loc, sound, category, vol * volume, pitch);
	}
	public void playSound(Player player, Location loc, Sound sound, float vol, float pitch) {
		playSound(player, loc, sound, SoundCategory.MASTER, vol, pitch);
	}
	public void playSoundInLargeArea(Location loc, Sound sound, float vol, float pitch, double range) {
		final double rangeSquared = range * range;
		final World world = loc.getWorld();
		world.getPlayers().forEach(player -> {
			final Location playerLoc = player.getLocation();
			if (playerLoc.distanceSquared(loc) > rangeSquared)
				return;
			final double distance = playerLoc.distance(loc);
			playSound(player, playerLoc.add(Utils.getVectorTowards(playerLoc, loc).multiply(7.0 / range * distance)), sound, (float) (vol - ((vol / range) * (distance - range))), pitch);
		});
	}
	public void playSoundInLargeArea(Location loc, Sound sound, float vol, float pitch, double innerRange, double outerRange, Function<Location, Location> function) {
		final double innerRangeSquared = innerRange * innerRange;
		final double sumRange = innerRange + outerRange;
		final double sumRangeSquared = sumRange * sumRange;
		final World world = loc.getWorld();
		world.getPlayers().forEach(player -> {
			final Location playerLoc = player.getLocation();
			final double distanceSquared = playerLoc.distanceSquared(loc);
			if (distanceSquared > sumRangeSquared)
				return;
			if (distanceSquared > innerRangeSquared) {
				final double distance = playerLoc.distance(loc);
				playSound(player, playerLoc.add(Utils.getVectorTowards(playerLoc, loc).multiply(7.0 / outerRange * distance)), sound, (float) (vol - ((vol / outerRange) * (distance - innerRange))), pitch);
			} else
				playSound(player, function.apply(playerLoc), sound, vol, pitch);
		});
	}
	public void addDeathWatcher(String languagePath) {
		if (getDeathCheck() == null)
			return;
		DeathMessageHandler.createWatcher(this, languagePath, getDeathCheck());
	}
	public void removeDeathWatcher(int delayTicks) {
		DeathMessageHandler.removeDeathWatcher(this, delayTicks);
	}
	public void getChunksInvolvedSafelyAndThen(Runnable function, boolean generateChunks) {
		final float radiusSquared = (float) (disasterRange * disasterRange);
		final int chunkX = location.getChunk().getX();
		final int chunkZ = location.getChunk().getZ();
		final int chunkRadius = (int) Math.ceil(disasterRange / 16.0);
		final Set<Chunk> chunks = new HashSet<>();
		if (PaperLib.isPaper()) {
			new BukkitRunnable() {
				@Override
				public void run() {
					for (int x = -chunkRadius; x <= chunkRadius; x++)
						for (int z = -chunkRadius; z <= chunkRadius; z++) {
							int currentX = chunkX + x;
							int currentZ = chunkZ + z;
							float deltaX = location.getBlockX() - (Utils.clamp(location.getBlockX(), currentX * 16, (currentX + 1) * 16 - 1));
							float deltaZ = location.getBlockZ() - (Utils.clamp(location.getBlockZ(), currentZ * 16, (currentZ + 1) * 16 - 1));
							if (deltaX * deltaX + deltaZ * deltaZ <= radiusSquared)
								try {
									Chunk chunk = PaperLib.getChunkAtAsync(location.getWorld(), currentX, currentZ, generateChunks).get();
									if (chunk != null)
										chunks.add(chunk);
								} catch (InterruptedException | ExecutionException e) {
									Utils.sendExceptionLog(e);
								}
						}
					function.run();
				}
			}.runTaskAsynchronously(plugin);
		} else {
			for (int x = -chunkRadius; x <= chunkRadius; x++)
				for (int z = -chunkRadius; z <= chunkRadius; z++) {
					int currentX = chunkX + x;
					int currentZ = chunkZ + z;
					float deltaX = location.getBlockX() - (Utils.clamp(location.getBlockX(), currentX * 16, (currentX + 1) * 16 - 1));
					float deltaZ = location.getBlockZ() - (Utils.clamp(location.getBlockZ(), currentZ * 16, (currentZ + 1) * 16 - 1));
					if (deltaX * deltaX + deltaZ * deltaZ <= radiusSquared) {
						Chunk chunk = location.getWorld().getChunkAt(currentX, currentZ, generateChunks);
						if (chunk != null)
							chunks.add(chunk);
					}
				}
			function.run();
		}
	}
	public void getChunksInvolvedSafelyAndThen(Runnable function) {
		getChunksInvolvedSafelyAndThen(function, false);
	}
	@FunctionalInterface
	public interface EntityFilter {
	    void filter(Map<Entity, Location> foundEntities, List<Entity> entities, List<Player> players);
	}
	public void createAsyncEntityMonitor(Predicate<Entity> findConditions, EntityFilter filter) {
		createAsyncEntityMonitor(disasterRange, findConditions, filter);
	}
	public void createAsyncEntityMonitor(double horizontalRange, Predicate<Entity> findConditions, EntityFilter filter) {
		entitiesInMonitorArea = new ArrayList<>();
		playersInMonitorArea = new ArrayList<>();
		recurringAsyncEntityMonitor(horizontalRange, findConditions, filter);
	}
	private void recurringAsyncEntityMonitor(double horizontalRange, Predicate<Entity> findConditions, EntityFilter filter) {
		final World world = location.getWorld();
		final Map<Entity, Location> foundEntities = new HashMap<>();
		for (Entity entity : world.getNearbyEntities(location, horizontalRange, 193, horizontalRange, findConditions))
			foundEntities.put(entity, entity.getLocation());
		final List<Player> players = new ArrayList<>(foundEntities.size());
		new BukkitRunnable() {
			@Override
			public void run() {
				final List<Entity> entities = new ArrayList<>(foundEntities.size());
				filter.filter(foundEntities, entities, players);
				new BukkitRunnable() {
					@Override
					public void run() {
						entitiesInMonitorArea.clear();
						entitiesInMonitorArea.addAll(entities);
						playersInMonitorArea.clear();
						playersInMonitorArea.addAll(players);
						if (!hasEnded())
							recurringAsyncEntityMonitor(horizontalRange, findConditions, filter);
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);
	}

	protected int getConfigInt(String path) {
		return worldLink.getConfigInt(getConfigPath()+'.'+path);
	}
	protected double getConfigDouble(String path) {
		return worldLink.getConfigDouble(getConfigPath()+'.'+path);
	}
	protected boolean getConfigBoolean(String path) {
		return worldLink.getConfigBoolean(getConfigPath()+'.'+path);
	}
	protected String getConfigString(String path) {
		return worldLink.getConfigString(getConfigPath()+'.'+path);
	}
	protected List<String> getConfigStringList(String path) {
		return worldLink.getConfigStringList(getConfigPath()+'.'+path);
	}
	protected List<Map<?, ?>> getConfigMapList(String path) {
		return worldLink.getConfigMapList(getConfigPath()+'.'+path);
	}
	protected ConfigurationSection getConfigSection(String path) {
		return worldLink.getConfigSection(getConfigPath()+'.'+path);
	}
	protected int getConfigOverrideInt(String path) {
		if (worldLink.getConfig().contains(getConfigPath()+'.'+path, true))
			return getConfigInt(path);
		return worldLink.getConfigInt("disasters.global."+path);
	}
	protected boolean getConfigOverrideBoolean(String path) {
		if (worldLink.getConfig().contains(getConfigPath()+'.'+path, true))
			return getConfigBoolean(path);
		return worldLink.getConfigBoolean("disasters.global."+path);
	}
	protected double getConfigOverrideDouble(String path) {
		if (worldLink.getConfig().contains(getConfigPath()+'.'+path, true))
			return getConfigDouble(path);
		return worldLink.getConfigDouble("disasters.global."+path);
	}
	protected List<String> getConfigOverrideStringList(String path) {
		if (worldLink.getConfig().contains(getConfigPath()+'.'+path, true))
			return getConfigStringList(path);
		return worldLink.getConfigStringList("disasters.global."+path);
	}
	public void scheduleTask(BukkitTask task) {
		this.tasks.add(task);
	}
	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
		if (level < 1)
			throw new IllegalArgumentException("Level cannot be less than 1!");
		this.level = level;
	}
	public Location getLocation() {
		return location;
	}
	public void setLocation(@NotNull Location location) {
		this.location = location;
		this.world = location.getWorld();
		this.worldLink = WorldWrapper.getWorldWrapper(this.world);
	}
	public Player getPlayer() {
		return player;
	}
	public void setPlayer(Player player) {
		this.player = player;
	}
	public boolean hasEnded() {
		return hasEnded;
	}
	public WorldWrapper getWorldLink() {
		return worldLink;
	}
	protected String getConfigPath() {
		return null;
	}
	public String getBroadcastMessageConfigPath() {
		return "messages.disaster_broadcasts.general.level_"+level;
	}
	public String getDisplayName() {
		String path = getConfigPath();
		if (path != null)
			return Utils.convertString(DataUtils.getLanguageString(path));
		return this.getClass().getSimpleName();
	}
	public String getDisasterTip() {
		if (getConfigPath() != null) {
			String[] split = getConfigPath().split("\\.");
			return DataUtils.getLanguageString("disasters.tips."+split[2]);
		}
		return null;
	}
	public double getFrequency() {
		return getConfigPath() == null ? 1.0 : getConfigOverrideDouble("frequency");
	}
	public Function<PlayerDeathEvent, Boolean> getDeathCheck() {
		return null;
	}
	public Set<Environment> getBannedEnvironments() {
		return Set.of();
	}
	protected void replaceFallingBlockUUID(UUID oldUUID, UUID newUUID) {
		if (fallingBlocks.remove(oldUUID))
			fallingBlocks.add(newUUID);
	}
	public static void stopAll(DisasterStopReason reason) {
		List<Disaster> disasters = new ArrayList<>(onGoingDisasters);
		disasters.forEach(disaster -> {
			try {
    			disaster.stop(reason);
    		} catch (Exception e) {
    			Utils.sendExceptionLog(e);
    		}
		});
	}
	public static void stopAll() {
		stopAll(DisasterStopReason.CUSTOM);
	}
	public static void cleanUpDisastersEffects() {
	}
}
