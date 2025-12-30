package com.github.jewishbanana.deadlydisasters.disasters;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.random.RandomGenerator;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.WorldWrapper;
import com.github.jewishbanana.deadlydisasters.events.DisasterStartEvent;
import com.github.jewishbanana.deadlydisasters.events.DisasterStartEvent.DisasterStartReason;
import com.github.jewishbanana.deadlydisasters.events.DisasterStopEvent;
import com.github.jewishbanana.deadlydisasters.events.DisasterStopEvent.DisasterStopReason;
import com.github.jewishbanana.deadlydisasters.listeners.BlockRegenHandler;
import com.github.jewishbanana.deadlydisasters.listeners.DeathMessageHandler;
import com.github.jewishbanana.deadlydisasters.utils.BlockUtils;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

import io.papermc.lib.PaperLib;

public abstract class Disaster {
	
	protected static final Main plugin;
	protected static final RandomGenerator random;
	public static final Queue<Disaster> onGoingDisasters;
	public static final Map<Disaster, RegeneratingTask> regeneratingDisasters;
	static {
		plugin = Main.getInstance();
		random = RandomGenerator.of("SplittableRandom");
		onGoingDisasters = new ArrayDeque<>();
		regeneratingDisasters = new HashMap<>();
	}
	
	private Queue<BukkitTask> tasks = new ArrayDeque<>();
	private WorldWrapper worldLink;
	private List<Block> modifiedBlocks = new ArrayList<>();
	private float volume = 1f;
	private boolean regionsProtected;
	private boolean affectEntitiesInRegions;
	private Set<EntityType> blacklistedEntityTypes;
	
	protected Location location;
	protected Player player;
	protected int level;
	protected double disasterRange;
	protected int startDelayTicks;

	public Disaster(@NotNull Location location, @Nullable Player player, int level) {
		this.location = location;
		this.player = player;
		this.setLevel(level);
		
		this.worldLink = WorldWrapper.getWorldWrapper(location.getWorld());
	}
	public void init() {
		if (getConfigPath() != null) {
			this.volume = (float) getConfigOverrideDouble("volume");
			this.startDelayTicks = (int) (getConfigOverrideDouble("start_delay") * 20.0);
			this.setLevel(Math.min(level, getConfigOverrideInt("max_level")));
			List<String> list = getConfigOverrideStringList("blacklisted_mob_types");
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
		return !event.isCancelled();
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
			Main.consoleSender.sendMessage(message+Utils.convertString(" &d("+location.getWorld().getName()+')'));
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
	public void start() {
		onGoingDisasters.add(this);
//		Bukkit.broadcastMessage("disaster started");
	}
	public boolean stop(DisasterStopReason reason) {
		DisasterStopEvent event = new DisasterStopEvent(this, reason);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled())
			return false;
		clean();
		onGoingDisasters.remove(this);
		if (reason != DisasterStopReason.SERVER_CLOSING)
			regenerateBlocks(reason);
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
	public boolean removeBlock(Block block, boolean ignoreImmuneOnly, boolean withPhysics) {
		if ((regionsProtected && isBlockProtected(block)) 
				|| (ignoreImmuneOnly ? BlockUtils.isBlockImmune(block) : BlockUtils.testBlockResistance(block)))
			return false;
		if (BlockRegenHandler.removeBlock(block, this, withPhysics) != null);
			modifiedBlocks.add(block);
		return true;
	}
	public boolean removeBlock(Block block) {
		return removeBlock(block, false, true);
	}
	public boolean placeBlock(Block block, BlockData data, boolean ignoreImmuneOnly, boolean withPhysics) {
		if ((regionsProtected && (ignoreImmuneOnly ? BlockUtils.isBlockImmune(block) : BlockUtils.testBlockResistance(block))) 
				|| BlockUtils.testBlockResistance(block))
			return false;
		if (BlockRegenHandler.placeBlock(block, data, this, withPhysics) != null)
			modifiedBlocks.add(block);
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
				|| BlockUtils.testBlockResistance(from) 
				|| BlockUtils.testBlockResistance(to))
			return false;
		BlockRegenHandler.moveBlock(from, to, this, withPhysics);
		if (reverseRegenerationOrder()) {
			modifiedBlocks.add(from);
		    modifiedBlocks.add(to);
		} else {
			modifiedBlocks.add(to);
		    modifiedBlocks.add(from);
		}
	    return true;
	}
	public boolean moveBlock(Block from, Block to) {
		return moveBlock(from, to, true);
	}
	public FallingBlock convertBlockIntoFallingBlock(Block block) {
		if ((regionsProtected && isBlockProtected(block)) 
				|| BlockUtils.testBlockResistance(block))
			return null;
		FallingBlock entity = BlockRegenHandler.convertBlockIntoFallingBlock(block, this);
		if (entity != null)
			modifiedBlocks.add(block);
		return entity;
	}
	public boolean isBlockProtected(Block block) {
		return DependencyUtils.isRegionProtected(block.getLocation());
	}
	public boolean isEntityProtected(Entity entity) {
		return blacklistedEntityTypes.contains(entity.getType())
				|| (!affectEntitiesInRegions && DependencyUtils.isEntityProtected(entity));
	}
	public void regenerateBlocks(DisasterStopReason reason) {
		if (modifiedBlocks.isEmpty())
			return;
		if (!worldLink.getConfigBoolean("regeneration.enabled")) {
			modifiedBlocks.clear();
			return;
		}
		List<Block> copiedSet = new ArrayList<>(modifiedBlocks);
		if (reverseRegenerationOrder()) {
			List<Block> list = new ArrayList<>(copiedSet);
			Collections.reverse(list);
			copiedSet = new ArrayList<>(list);
		}
		if (reason != DisasterStopReason.SERVER_CLOSING) {
//			BlockRegenHandler.printMaps();
			final double regenRate = getRegenTickRate() * (getConfigPath() == null ? 1.0 : getConfigOverrideDouble("regen_rate"));
			if (regenRate > 0)
				regeneratingDisasters.put(this, this.new RegeneratingTask(this, copiedSet, regenRate, (int) (worldLink.getConfigDouble("regeneration.regeneration_delay") * 20)));
			modifiedBlocks.clear();
		} else
			modifiedBlocks = copiedSet;
	}
	public void regenerateBlocks() {
		regenerateBlocks(DisasterStopReason.CUSTOM);
	}
	public class RegeneratingTask {
		
		public BukkitTask task;
		public List<Block> blocks;
		
		public RegeneratingTask(Disaster disaster, List<Block> blocks, double regenRate, int startDelay) {
			this.blocks = blocks;
			this.task = new BukkitRunnable() {
				private double regenTicks;
				private Iterator<Block> iterator = blocks.iterator();
				
				@Override
				public void run() {
					regenTicks += regenRate;
					while (regenTicks >= 1 && iterator.hasNext()) {
						regenTicks -= 1;
						try {
							do {
								Block block = iterator.next();
								iterator.remove();
								if (BlockRegenHandler.restoreBlock(block, true))
									break;
							} while (iterator.hasNext());
						} catch (Exception e) {
							Utils.sendExceptionLog(e);
						}
					}
					if (!iterator.hasNext()) {
						if (modifiedBlocks.isEmpty()) {
							this.cancel();
							regeneratingDisasters.remove(disaster);
//							BlockRegenHandler.printMaps();
							return;
						}
						blocks.addAll(modifiedBlocks);
						modifiedBlocks.clear();
						iterator = blocks.iterator();
					}
				}
			}.runTaskTimer(plugin, startDelay, 1);
		}
		public RegeneratingTask(Disaster disaster, List<Block> blocks, int startDelay) {
			this(disaster, blocks, getRegenTickRate() * (getConfigPath() == null ? 1.0 : getConfigOverrideDouble("regen_rate")), startDelay);
		}
	}
	public void playSound(Location loc, Sound sound, SoundCategory category, double vol, double pitch) {
		loc.getWorld().playSound(loc, sound, category, (float) (vol * volume), (float) pitch);
	}
	public void playSound(Location loc, Sound sound, double vol, double pitch) {
		playSound(loc, sound, SoundCategory.MASTER, vol, pitch);
	}
	public void playSound(Player player, Location loc, Sound sound, SoundCategory category, double vol, double pitch) {
		player.playSound(loc, sound, category, (float) (vol * volume), (float) pitch);
	}
	public void playSound(Player player, Location loc, Sound sound, double vol, double pitch) {
		playSound(player, loc, sound, SoundCategory.MASTER, vol, pitch);
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
		final double radiusSquared = disasterRange * disasterRange;
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
							double deltaX = location.getBlockX() - (Utils.clamp(location.getBlockX(), currentX * 16, (currentX + 1) * 16 - 1));
							double deltaZ = location.getBlockZ() - (Utils.clamp(location.getBlockZ(), currentZ * 16, (currentZ + 1) * 16 - 1));
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
					double deltaX = location.getBlockX() - (Utils.clamp(location.getBlockX(), currentX * 16, (currentX + 1) * 16 - 1));
					double deltaZ = location.getBlockZ() - (Utils.clamp(location.getBlockZ(), currentZ * 16, (currentZ + 1) * 16 - 1));
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
	public void setLocation(Location location) {
		this.location = location;
		this.worldLink = WorldWrapper.getWorldWrapper(location.getWorld());
	}
	public Player getPlayer() {
		return player;
	}
	public void setPlayer(Player player) {
		this.player = player;
	}
	public WorldWrapper getWorldLink() {
		return worldLink;
	}
	protected String getConfigPath() {
		return null;
	}
	public double getRegenTickRate() {
		return level * 0.05;
	}
	public boolean reverseRegenerationOrder() {
		return true;
	}
	public String getBroadcastMessageConfigPath() {
		return "messages.disaster_broadcasts.general.level_"+level;
	}
	public String getDisplayName() {
		return this.getClass().getSimpleName();
	}
	public String getDisasterTip() {
		if (getConfigPath() != null) {
			String[] split = getConfigPath().split("\\.");
			return DataUtils.getLanguageString("disasters.tips."+split[2]);
		}
		return null;
	}
	public List<Block> getModifiedBlocks() {
		return modifiedBlocks;
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
	public static void stopAll(DisasterStopReason reason) {
		Queue<Disaster> disasters = new ArrayDeque<>(onGoingDisasters);
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
}
