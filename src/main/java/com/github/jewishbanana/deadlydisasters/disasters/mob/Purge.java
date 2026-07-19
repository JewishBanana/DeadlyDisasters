package com.github.jewishbanana.deadlydisasters.disasters.mob;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import java.util.Collections;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Ravager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.github.jewishbanana.deadlydisasters.disasters.Disaster;
import com.github.jewishbanana.deadlydisasters.disasters.MobDisaster;
import com.github.jewishbanana.deadlydisasters.events.DisasterStartEvent.DisasterStartReason;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.UltimateContentUtils;
import com.github.jewishbanana.deadlydisasters.utils.SpawnUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class Purge extends Disaster implements MobDisaster, Listener {

	private static final Set<UUID> targetedPlayers;
	private static final String storedPlayersKey;
	private static final BlockFace[] SIX_FACES;
	private static final int SLOW_TICK_PERIOD = 25; // game ticks between slow sub-ticks (5-tick loop, fires every 5th run)
	private static final int BREACH_STREAK_CAP = 8; // bounds escalation so the horde cap bonus can't run away
	static {
		targetedPlayers = new HashSet<>();
		storedPlayersKey = "purge.stored_players";
		SIX_FACES = new BlockFace[] {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
	}

	private float entitySpawnDistance;
	private int maxHordeSize;
	private int hordeVanquishThreshold;

	private UUID targetUUID;
	private float entitySpawnRate;
	private BossBar bar;
	private List<EntityContainer> entityContainers;

	// Anti-turtle "breach squad" system: when the target seals themselves in (dug-in, bunker, or skybase) the purge
	// sends miner-led squads that tunnel/pillar to them, escalating the longer they hide. See launchBreachSquad.
	private boolean breachEnabled;
	private int breachTriggerTicks;        // slow-ticks the player must be sealed/unreachable before a breach
	private int breachCooldownReset;       // slow-ticks between breaches
	private double breachReachDistanceSq;  // a horde mob this close counts as "reaching" the player
	private int breachScanRadius;
	private int breachScanCap;
	private double breachMinerRetention;   // breach-mode target retention range for miners
	private int breachMaxMiners;           // hard cap on simultaneous breach miners (primary lag guard)
	private int breachMinersPerSquad;
	private int breachEscortSize;
	private int breachFlyerCount;
	private int breachCapBonus;            // added to max horde size per escalation streak
	private double breachSpawnRateMultiplier;
	private int breachMinersMax;
	private int breachEscortMax;
	private int breachFlyerMax;
	private Class<?> breachMinerClass;     // resolved uc:undead_miner class, null if UltimateContent absent
	private final Set<UUID> breachMiners = new HashSet<>(); // this purge's breach miners (faster than entity metadata)

	private int unreachableTicks;          // slow-ticks the player has been turtling (stationary + sealed/underground)
	private int breachCooldownTicks;       // running cooldown counter
	private int breachStreak;              // consecutive breaches while the player stays sealed
	private Location lastBreachLoc;        // player position last slow-tick, to detect movement (running != turtling)
	private Location lastBreachPlayerLoc;  // player position at the previous breach launch (for the burrow cut-off vector)
	// Skybase/burrow detection is a heavy read-only block scan, so it runs off-thread and caches its result here; the main
	// loop reads the cache (one slow-tick stale, which is fine) instead of scanning inline.
	private volatile boolean cachedSkybase;
	private volatile boolean cachedBurrowed;
	private boolean detectionRunning; // main-thread guard so only one detection scan is queued at a time
	private static final double BURROW_LEAD = 14.0;       // how far ahead of the player to aim a burrow cut-off squad
	private static final int CAVE_SEARCH_RADIUS = 6;      // search box (blocks) for a cave pocket near the cut-off aim
	private static final double BURROW_PRUNE_DISTSQ = 40.0 * 40.0; // burrow squads/mobs farther than this are recycled
	private static final double BREACH_MOVE_SQ = 9.0; // >3 blocks moved in a slow-tick counts as relocating, not turtling
	private static final double SWAP_RADIUS_SQ = 20.0 * 20.0; // a non-primary player within this range can steal the horde's focus

	public Purge(@NotNull Location location, Player player, int level) {
		super(location, player, level);
	}
	public void init() {
		super.init();
		this.entitySpawnDistance = (float) getConfigDouble("entity_spawn_distance");
		this.maxHordeSize = getConfigInt("max_horde_size.level_"+level);
		this.hordeVanquishThreshold = getConfigInt("horde_vanquish_threshold.level_"+level);
		
		this.entitySpawnRate = (float) (0.05 * getConfigDouble("entity_spawn_rate") * level);
		if (getConfigBoolean("display_boss_bar"))
			this.bar = Bukkit.createBossBar(Utils.convertString(DataUtils.getLanguageString("disasters.features.purge_boss_bar_title")), BarColor.RED, BarStyle.SOLID, BarFlag.DARKEN_SKY, BarFlag.CREATE_FOG);
		this.entityContainers = new ArrayList<>();
		List<Map<?, ?>> containers = getConfigMapList("entity_spawns");
		if (containers != null)
			containers.forEach(m -> {
				EntityContainer container = EntityContainer.createContainer(m, this);
				if (container == null)
					return;
				entityContainers.add(container);
			});
		this.breachEnabled = getConfigBoolean("breach.enabled");
		if (breachEnabled) {
			this.breachTriggerTicks = secondsToSlowTicks(getConfigInt("breach.trigger_seconds"));
			this.breachCooldownReset = secondsToSlowTicks(getConfigInt("breach.cooldown_seconds"));
			double reach = getConfigDouble("breach.reach_distance");
			this.breachReachDistanceSq = reach * reach;
			this.breachScanRadius = Math.max(2, getConfigInt("breach.scan_radius"));
			this.breachScanCap = Math.max(64, getConfigInt("breach.scan_cap"));
			this.breachMinerRetention = Math.max(8.0, getConfigDouble("breach.miner_retention_range"));
			this.breachMaxMiners = Math.max(0, getConfigInt("breach.max_miners"));
			this.breachMinersPerSquad = Math.max(0, getConfigInt("breach.miners_per_squad"));
			this.breachEscortSize = Math.max(0, getConfigInt("breach.escort_size"));
			this.breachFlyerCount = Math.max(0, getConfigInt("breach.flyer_count"));
			this.breachCapBonus = Math.max(0, getConfigInt("breach.cap_bonus"));
			this.breachSpawnRateMultiplier = Math.max(1.0, getConfigDouble("breach.spawn_rate_multiplier"));
			this.breachMinersMax = Math.max(breachMinersPerSquad, getConfigInt("breach.miners_per_squad_max"));
			this.breachEscortMax = Math.max(breachEscortSize, getConfigInt("breach.escort_size_max"));
			this.breachFlyerMax = Math.max(breachFlyerCount, getConfigInt("breach.flyer_count_max"));
			if (DependencyUtils.isUltimateContentEnabled())
				this.breachMinerClass = UltimateContentUtils.getEntityClass("uc:undead_miner");
		}
	}
	private static int secondsToSlowTicks(int seconds) {
		return Math.max(1, (int) Math.round(seconds * 20.0 / SLOW_TICK_PERIOD));
	}
	public boolean canStart() {
		if (player == null || targetedPlayers.contains(player.getUniqueId()))
			return false;
		int height = getLocation().getBlockY();
		if (height < -32 || height > 100)
			return false;
		return super.canStart();
	}
	public void start() {
		super.start();
		if (player == null || entityContainers.isEmpty()) {
			stop();
			return;
		}
		this.targetUUID = player.getUniqueId();
		targetedPlayers.add(targetUUID);
		// Register this purge's own temporary event handlers (horde focus / friendly-fire / phantom sunlight); torn down
		// in clean() so they only live for the disaster's lifespan, like the other disasters' listeners.
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		playSound(location, Sound.EVENT_RAID_HORN, 100f, .1f);
		if (bar != null)
			bar.addPlayer(player);
		scheduleTask(new BukkitRunnable() {
			private int playerUpdateTick;
			private final double decrement = 1.0 / hordeVanquishThreshold;
			
			@Override
			public void run() {
				final Player player = Bukkit.getPlayer(targetUUID);
				if (player == null || !player.isOnline() || EntityUtils.isPlayerImmune(player) || !player.getWorld().equals(location.getWorld())) {
					if (player == null || !player.isOnline()) {
						Map<String, Integer> map = DataUtils.computeSection(DataUtils.getDataFile(), storedPlayersKey, new HashMap<String, Integer>());
						Integer value = map.get(targetUUID.toString());
						if (value == null || value < getLevel()) {
							map.put(targetUUID.toString(), getLevel());
							DataUtils.writeToDataFile(file -> file.set(storedPlayersKey, map));
						}
					}
					stop();
					return;
				}
				Set<UUID> set = getAllEntities();
				if (set != null) {
					Iterator<UUID> it = set.iterator();
					while (it.hasNext()) {
						Entity entity = Bukkit.getEntity(it.next());
						if (entity == null)
							it.remove();
						else if (entity.isDead()) {
							if (bar != null)
								bar.setProgress(Math.max(bar.getProgress() - decrement, 0));
							it.remove();
							if (--hordeVanquishThreshold == 0) {
								stop();
								return;
							}
						}
					}
				}
				final Location playerLoc = player.getLocation();
				if (playerUpdateTick-- == 0) {
					playerUpdateTick = 4;
					if (bar != null)
						player.getWorld().getPlayers().forEach(p -> {
							if (p.equals(player))
								return;
							if (p.getLocation().distanceSquared(playerLoc) > 900) {
								bar.removePlayer(p);
								return;
							}
							bar.addPlayer(p);
						});
					updatePurgeTargets(player);
						if (breachEnabled) {
							scheduleDetection(player, playerLoc); // async block scan -> caches skybase/burrowed for next tick
							evaluateContainment(player, playerLoc, set);
						}
				}
				final int effectiveMaxHorde = maxHordeSize + (breachEnabled ? breachStreak * breachCapBonus : 0);
				final float effectiveSpawnRate = (breachEnabled && breachStreak > 0) ? (float) (entitySpawnRate * breachSpawnRateMultiplier) : entitySpawnRate;
				if ((set == null || set.size() < effectiveMaxHorde) && random.nextFloat() < effectiveSpawnRate) {
					for (int i=0; i < 10; i++) {
						Location spawn = SpawnUtils.findMonsterSpawnLocation(playerLoc, 2, entitySpawnDistance, entitySpawnDistance + 5f);
						if (spawn == null)
							continue;
						// Half the time, snap the spawn to the player's vertical level so underground/ravine players
						// get mobs in their cave instead of only on the distant surface.
						if (random.nextFloat() < 0.5f) {
							Location smart = SpawnUtils.findSmartYSpawn(playerLoc, spawn, 2, 40);
							if (smart != null)
								spawn = smart;
						}
						if (spawn.getWorld().getNearbyEntities(spawn, Math.min(entitySpawnDistance-1, 15.0), Math.min(entitySpawnDistance-1, 10.0), Math.min(entitySpawnDistance-1, 15.0), e -> e instanceof Player p && !EntityUtils.isPlayerImmune(p)).stream().count() != 0)
							continue;
						EntityContainer container = rollEntity();
						if (container == null)
							continue;
						Entity entity = container.spawnEntity(spawn);
						if (entity == null)
							continue;
						if (!Utils.isAreaClear(spawn, (float) entity.getWidth(), (float) (entity.getHeight() - 0.2))) {
							removeEntityAndVehicle(entity);
							continue;
						}
						if (entity instanceof Mob mob)
							addEntityToDisasterList(mob, player);
						else
							addEntityToDisasterList(entity);
						if (entity instanceof Mob mob)
							mob.setTarget(player);
						break;
					}
				}
			}
		}.runTaskTimer(plugin, 0, 5));
	}
	public void clean() {
		super.clean();
		HandlerList.unregisterAll(this); // tear down this purge's temporary event handlers
		if (targetUUID != null)
			targetedPlayers.remove(targetUUID);
		if (bar != null)
			bar.removeAll();
		if (getWorldLink().getConfigBoolean("world.broadcast_disasters")) {
			Player player = Bukkit.getPlayer(targetUUID);
			if (player != null) {
				String endMessage = Utils.convertString(DataUtils.getLanguageString("messages.disaster_broadcasts.purge.ended"));
				player.sendMessage(endMessage);
				player.getNearbyEntities(30.0, 30.0, 30.0).forEach(e -> {
					if (e instanceof Player)
						e.sendMessage(endMessage);
				});
			}
		}
	}
	private EntityContainer rollEntity() {
		final float roll = random.nextFloat((float) entityContainers.stream().mapToDouble(t -> t.chance).sum());
		float cumulative = 0;
		for (EntityContainer container : entityContainers) {
			cumulative += container.chance;
			if (roll < cumulative)
				return container;
		}
		return null;
	}
	/**
	 * Re-targets the whole horde each slow tick. Every non-miner purge mob locks onto the nearest valid player: the
	 * primary target is always eligible (at any range, so the purge relentlessly chases them), and any OTHER non-immune
	 * player is eligible only when within {@link #SWAP_RADIUS_SQ} and closer than the primary. This means: (a) the horde
	 * never idles on/attacks itself (it always has a player target), (b) when a helper player gets in among the horde the
	 * nearby mobs swap onto and gang up on them ("aid"), and (c) once that helper runs far enough away the mobs revert to
	 * the primary target. Breach miners are skipped - they're driven by their own dig loop toward the sealed player.
	 */
	private void updatePurgeTargets(Player primary) {
		Set<UUID> set = getAllEntities();
		if (set == null)
			return;
		List<Player> others = new ArrayList<>();
		for (Player p : primary.getWorld().getPlayers())
			if (!p.equals(primary) && !EntityUtils.isPlayerImmune(p))
				others.add(p);
		for (UUID id : set) {
			Entity e = Bukkit.getEntity(id);
			if (!(e instanceof Mob mob) || breachMiners.contains(id))
				continue;
			Location loc = mob.getLocation();
			LivingEntity desired = primary;
			double bestSq = primary.getWorld().equals(loc.getWorld()) ? primary.getLocation().distanceSquared(loc) : Double.MAX_VALUE;
			for (Player p : others) {
				if (!p.getWorld().equals(loc.getWorld()))
					continue;
				double d = p.getLocation().distanceSquared(loc);
				if (d <= SWAP_RADIUS_SQ && d < bestSq) {
					bestSq = d;
					desired = p;
				}
			}
			if (!desired.equals(mob.getTarget()))
				mob.setTarget(desired);
		}
	}
	// === Anti-turtle breach system ============================================================================
	/**
	 * Kicks off the skybase/burrow detection off the main thread (it's a heavy read-only block flood-fill). Block reads on
	 * loaded chunks near the player are safe to do async (same approach the InfestedCaves disaster uses); the booleans are
	 * stored back on the main thread for the next slow tick to read. Guarded so only one scan is in flight at a time.
	 */
	private void scheduleDetection(Player player, Location playerLoc) {
		if (detectionRunning)
			return;
		detectionRunning = true;
		final Location pLoc = playerLoc.clone();
		final Location eyeLoc = player.getEyeLocation();
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			boolean skybase = false, burrowed = false;
			try {
				skybase = isSkybase(pLoc);
				boolean sealed = isSealedPocket(eyeLoc);
				World w = pLoc.getWorld();
				boolean underground = pLoc.getBlockY() + 2 < w.getHighestBlockYAt(pLoc.getBlockX(), pLoc.getBlockZ());
				burrowed = !skybase && (sealed || underground);
			} catch (Exception ignored) {
				// a chunk unloaded mid-scan etc. - just leave the previous cached result and try again next tick
			}
			final boolean fSky = skybase, fBur = burrowed;
			plugin.getServer().getScheduler().runTask(plugin, () -> {
				cachedSkybase = fSky;
				cachedBurrowed = fBur;
				detectionRunning = false;
			});
		});
	}
	/**
	 * Runs on the slow sub-tick. Tracks whether the target is being reached, detects sealing/turtling, decays or
	 * grows the escalation streak, keeps breach miners locked on, and launches a breach squad on cooldown.
	 */
	private void evaluateContainment(Player player, Location playerLoc, Set<UUID> set) {
		double nearestSq = Double.MAX_VALUE;
		boolean hordeAlive = false;
		int currentMiners = 0;
		if (set != null)
			for (UUID id : set) {
				Entity e = Bukkit.getEntity(id);
				if (e == null || e.isDead())
					continue;
				hordeAlive = true;
				if (breachMiners.contains(id)) {
					currentMiners++;
					if (e instanceof Mob mob) // keep breach miners locked so they resume digging if they idle
						mob.setTarget(player);
				}
				double d = e.getLocation().distanceSquared(playerLoc);
				if (d < nearestSq)
					nearestSq = d;
			}
		if (breachCooldownTicks > 0)
			breachCooldownTicks--;
		boolean moved = lastBreachLoc != null && lastBreachLoc.getWorld().equals(playerLoc.getWorld()) && lastBreachLoc.distanceSquared(playerLoc) > BREACH_MOVE_SQ;
		lastBreachLoc = playerLoc.clone();
		// Detection (skybase / burrowed) is computed off-thread by scheduleDetection; read its cached result (one slow-tick
		// stale is harmless). burrowed already excludes skybases (a roofed skybase stays a skybase, not a burrow).
		boolean skybase = cachedSkybase;
		boolean burrowed = cachedBurrowed;
		// Flyers (vex/phantom) are only useful in the open or against a skybase; underground they're dead weight, so clear
		// any that are still around once the player burrows in.
		if (burrowed && !skybase)
			removeFlyers(set);
		// The player is "turtled" whenever the surface horde can't simply walk up to them: sealed in a pocket, dug below
		// the surface, OR perched on a skybase/pillar (previously skybases were excluded, so towering up never triggered a
		// breach at all). Open-ground players are left to the normal horde.
		boolean turtled = skybase || burrowed;
		if (!turtled) {
			// Open ground: never breach. Ease the timer/streak when the horde reaches them or they relocate across the
			// surface (the reach-relief lives HERE only - for a turtled player a stray mob touching them must NOT reset
			// the pressure, otherwise a single vex reaching a skybased player throttled the whole assault).
			if ((hordeAlive && nearestSq <= breachReachDistanceSq) || moved) {
				unreachableTicks = 0;
				if (breachStreak > 0)
					breachStreak--;
			} else if (unreachableTicks > 0)
				unreachableTicks--;
			return;
		}
		unreachableTicks++;
		if (breachCooldownTicks > 0 || unreachableTicks < breachTriggerTicks)
			return;
		// Cut-off vector: how the player has moved since the last breach (their tunnelling direction, incl. the Y axis).
		Vector digDir = (lastBreachPlayerLoc != null && lastBreachPlayerLoc.getWorld().equals(playerLoc.getWorld()))
				? playerLoc.toVector().subtract(lastBreachPlayerLoc.toVector()) : null;
		lastBreachPlayerLoc = playerLoc.clone();
		launchBreachSquad(player, playerLoc, currentMiners, set, skybase, burrowed, digDir);
		breachStreak = Math.min(breachStreak + 1, BREACH_STREAK_CAP);
		breachCooldownTicks = breachCooldownReset;
	}
	/** Silently removes any tracked flyers (vex/phantom) - used when the player burrows, where flyers are useless. */
	private void removeFlyers(Set<UUID> set) {
		if (set == null)
			return;
		Iterator<UUID> it = set.iterator();
		while (it.hasNext()) {
			UUID id = it.next();
			Entity e = Bukkit.getEntity(id);
			if (e != null && (e.getType() == EntityType.PHANTOM || e.getType() == EntityType.VEX)) {
				MobDisaster.entitiyTargets.remove(id);
				it.remove();
				removeEntityAndVehicle(e);
			}
		}
	}
	/**
	 * Bounded passable-space flood-fill from the given location. Returns true only for a genuinely enclosed pocket
	 * (no sky exposure, doesn't reach the scan boundary, and stays under the cell cap). Large/open spaces and
	 * sky-exposed spaces return false. Throttled to the slow tick by its caller.
	 */
	private boolean isSealedPocket(Location origin) {
		World world = origin.getWorld();
		Block start = origin.getBlock();
		if (!start.isPassable()) {
			start = start.getRelative(BlockFace.UP);
			if (!start.isPassable())
				return false;
		}
		final int ox = start.getX(), oy = start.getY(), oz = start.getZ();
		final int radius = breachScanRadius;
		Set<Block> visited = new HashSet<>();
		ArrayDeque<Block> queue = new ArrayDeque<>();
		visited.add(start);
		queue.add(start);
		int count = 0;
		while (!queue.isEmpty()) {
			Block b = queue.poll();
			if (b.getY() >= world.getHighestBlockYAt(b.getX(), b.getZ()))
				return false; // sky-exposed -> open
			if (++count > breachScanCap)
				return false; // large space -> not a tight seal
			for (BlockFace face : SIX_FACES) {
				Block n = b.getRelative(face);
				if (!n.isPassable() || visited.contains(n))
					continue;
				if (Math.abs(n.getX() - ox) > radius || Math.abs(n.getY() - oy) > radius || Math.abs(n.getZ() - oz) > radius)
					return false; // space extends past the scan radius -> treat as open
				visited.add(n);
				queue.add(n);
			}
		}
		return true; // fully enclosed within radius and cap, no sky
	}
	/** Spawns a breach squad sized by the escalation streak: miners (capped) + escort, plus flyers for skybases. */
	private void launchBreachSquad(Player player, Location playerLoc, int currentMiners, Set<UUID> set, boolean skybase, boolean burrowed, Vector digDir) {
		int minerCount = Math.max(0, Math.min(Math.min(breachMinersPerSquad + breachStreak, breachMinersMax), breachMaxMiners - currentMiners));
		int escort = Math.min(breachEscortSize + breachStreak, breachEscortMax);
		int flyers = skybase ? Math.min(breachFlyerCount + breachStreak, breachFlyerMax) : 0;
		if (burrowed) {
			// As the player tunnels away, recycle squads/mobs left far behind so a fresh squad can be sent to CUT THEM OFF,
			// and guarantee at least one miner (without a miner the squad can't dig to a sealed player at all).
			pruneFarBreachForces(playerLoc, set);
			currentMiners = countLiveBreachMiners(set);
			int desired = Math.min(breachMinersPerSquad + breachStreak, breachMinersMax);
			minerCount = Math.max(1, Math.min(desired, Math.max(1, breachMaxMiners - currentMiners)));
		}
		// Make room: if adding this squad would blow past the (escalated) horde cap, recycle the farthest stalled mobs -
		// the pile stuck outside the seal / at the base of the pillar - so the miners and flyers can actually spawn instead
		// of the breach silently failing because the cap is full (the player's reported "nothing else happens").
		int wanted = minerCount + escort + flyers;
		int effectiveMaxHorde = maxHordeSize + breachStreak * breachCapBonus;
		int projected = (set == null ? 0 : set.size()) + wanted;
		int evicted = 0;
		if (projected > effectiveMaxHorde)
			evicted = evictForBreach(playerLoc, set, projected - effectiveMaxHorde);
		final int fMinerCount = minerCount, fEscort = escort, fEvicted = evicted;
		if (burrowed) {
			// The cut-off origin (cave pocket / surface column) is a pure read-only block scan - run it off-thread, then
			// spawn the squad back on the main thread.
			plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
				Location origin = findBurrowOrigin(playerLoc, digDir);
				plugin.getServer().getScheduler().runTask(plugin, () -> spawnSquad(origin, player, playerLoc, fMinerCount, fEscort, false, true, fEvicted));
			});
		} else
			spawnSquad(findBreachOrigin(player, playerLoc, skybase), player, playerLoc, fMinerCount, fEscort, skybase, false, fEvicted);
	}
	/** Spawns a resolved breach squad (miners + escort, plus skybase flyers) at {@code origin}. Must run on the main thread. */
	private void spawnSquad(Location origin, Player player, Location playerLoc, int minerCount, int escort, boolean skybase, boolean burrowed, int evicted) {
		if (origin != null) {
			// Spread the miners out: on a skybase, give each its own ground spot at a different angle around the pillar so
			// they build several separate staircases instead of pooling in one place; otherwise cluster them at the origin.
			for (int i = 0; i < minerCount; i++) {
				Location mOrigin = origin;
				if (skybase) {
					double ang = (Math.PI * 2.0 * i / Math.max(1, minerCount)) + random.nextDouble() * 0.6;
					Location spread = skybaseGroundSpot(playerLoc, ang, 6 + random.nextInt(5));
					if (spread != null)
						mOrigin = spread;
				} else
					mOrigin = scatter(origin, 1.5);
				spawnBreachMiner(mOrigin, player);
			}
			for (int i = 0; i < escort; i++) {
				EntityContainer container = rollEntity();
				if (container == null)
					continue;
				Entity e = container.spawnEntity(scatter(origin, 2.0));
				if (e != null)
					registerPurgeMob(e, player);
			}
		}
		// For a skybase the ground escort can barely reach, so the air is a real threat - but full waves piled too many
		// vexes on the player, so send only about a quarter of the computed wave.
		int flyers = skybase ? Math.max(1, Math.min(breachFlyerCount + breachStreak + evicted + 3, breachFlyerMax + 10) / 4) : 0;
		for (int i = 0; i < flyers; i++)
			spawnFlyer(player, playerLoc);
	}
	/** Counts this purge's breach miners that are currently alive (intersect the live tracked set with breachMiners). */
	private int countLiveBreachMiners(Set<UUID> set) {
		if (set == null)
			return 0;
		int c = 0;
		for (UUID id : set)
			if (breachMiners.contains(id)) {
				Entity e = Bukkit.getEntity(id);
				if (e != null && !e.isDead())
					c++;
			}
		return c;
	}
	/**
	 * Recycles this purge's mobs (miners and escort alike) that the tunnelling player has left far behind, so a fresh
	 * cut-off squad can spawn ahead instead. Silent (pulled from the tracked set first -> no vanquish-bar credit).
	 */
	private void pruneFarBreachForces(Location playerLoc, Set<UUID> set) {
		if (set == null)
			return;
		Iterator<UUID> it = set.iterator();
		while (it.hasNext()) {
			UUID id = it.next();
			Entity e = Bukkit.getEntity(id);
			if (e == null || e.isDead())
				continue;
			if (e.getWorld().equals(playerLoc.getWorld()) && e.getLocation().distanceSquared(playerLoc) > BURROW_PRUNE_DISTSQ) {
				it.remove();
				breachMiners.remove(id);
				MobDisaster.entitiyTargets.remove(id);
				removeEntityAndVehicle(e);
			}
		}
	}
	/**
	 * Picks where to drop a burrow cut-off squad: a point {@link #BURROW_LEAD} blocks ahead of the player along their dig
	 * direction (incl. the Y axis), then the nearest reachable spawn to that aim - a cave pocket near it, or the surface
	 * column above it - whichever is closer to the player (less digging). Returns null only if nothing is spawnable.
	 */
	private Location findBurrowOrigin(Location playerLoc, Vector digDir) {
		World world = playerLoc.getWorld();
		Location ahead = playerLoc.clone();
		if (digDir != null && digDir.lengthSquared() > 0.25)
			ahead.add(digDir.clone().normalize().multiply(BURROW_LEAD));
		Location cave = findCavePocket(world, ahead.getBlockX(), ahead.getBlockY(), ahead.getBlockZ(), playerLoc);
		Location surf = surfaceTopSpawn(world, ahead.getBlockX(), ahead.getBlockZ());
		if (cave != null && surf != null)
			return cave.distanceSquared(playerLoc) <= surf.distanceSquared(playerLoc) ? cave : surf;
		if (cave != null)
			return cave;
		if (surf != null)
			return surf;
		return surfaceTopSpawn(world, playerLoc.getBlockX(), playerLoc.getBlockZ());
	}
	/** Nearest standable cave pocket (solid floor, 2 air above) to the aim point within {@link #CAVE_SEARCH_RADIUS}, at least a few blocks off the player. */
	private Location findCavePocket(World world, int cx, int cy, int cz, Location playerLoc) {
		Location best = null;
		double bestSq = Double.MAX_VALUE;
		int r = CAVE_SEARCH_RADIUS;
		for (int dy = -r; dy <= r; dy++) {
			int y = cy + dy;
			if (y <= world.getMinHeight() + 1 || y >= world.getMaxHeight() - 2)
				continue;
			for (int dx = -r; dx <= r; dx++)
				for (int dz = -r; dz <= r; dz++) {
					int x = cx + dx, z = cz + dz;
					Block feet = world.getBlockAt(x, y, z);
					if (!feet.isPassable() || feet.isLiquid())
						continue;
					if (!world.getBlockAt(x, y + 1, z).isPassable())
						continue; // need head room
					Block floor = world.getBlockAt(x, y - 1, z);
					if (floor.isPassable() || floor.isLiquid())
						continue; // need a solid floor to stand/spawn on
					Location loc = new Location(world, x + 0.5, y, z + 0.5);
					if (loc.distanceSquared(playerLoc) < 16)
						continue; // too close (likely the player's own pocket) - the miner needs something to dig
					double d = dx * dx + dy * dy + dz * dz;
					if (d < bestSq) {
						bestSq = d;
						best = loc;
					}
				}
		}
		return best;
	}
	/** A spawn spot on top of the surface at x,z (solid top, 2 air above, not liquid), or null. */
	private Location surfaceTopSpawn(World world, int x, int z) {
		int y = world.getHighestBlockYAt(x, z);
		Block top = world.getBlockAt(x, y, z);
		if (top.isPassable() || top.isLiquid())
			return null;
		if (world.getBlockAt(x, y + 1, z).isPassable() && world.getBlockAt(x, y + 2, z).isPassable())
			return new Location(world, x + 0.5, y + 1, z + 0.5);
		return null;
	}
	/**
	 * Frees up to {@code count} horde-cap slots for a breach squad by removing the least-useful tracked mobs: those that
	 * aren't breach miners or flyers and are farthest from the player (the pile that's stuck outside the seal or milling
	 * at the base of the pillar, unable to reach the target anyway). These are removed <b>silently</b> - pulled from the
	 * tracked set first so the slow loop never sees them die, so the player earns NO vanquish-bar progress for the purge
	 * recycling its own stalled mobs into a fresh breach force.
	 */
	private int evictForBreach(Location playerLoc, Set<UUID> set, int count) {
		if (set == null || count <= 0)
			return 0;
		List<Entity> candidates = new ArrayList<>();
		for (UUID id : set) {
			Entity e = Bukkit.getEntity(id);
			if (e == null || e.isDead() || breachMiners.contains(id))
				continue;
			if (e.getType() == EntityType.PHANTOM || e.getType() == EntityType.VEX)
				continue;
			candidates.add(e);
		}
		// Farthest from the player first - those are the ones that can't reach the sealed/elevated target.
		candidates.sort((a, b) -> Double.compare(b.getLocation().distanceSquared(playerLoc), a.getLocation().distanceSquared(playerLoc)));
		int removed = 0;
		for (Entity e : candidates) {
			if (removed >= count)
				break;
			UUID id = e.getUniqueId();
			set.remove(id);
			MobDisaster.entitiyTargets.remove(id);
			Location loc = e.getLocation();
			loc.getWorld().spawnParticle(VersionUtils.getLargeSmoke(), loc.add(0, e.getHeight() / 2.0, 0), 8, 0.25, 0.4, 0.25, 0.02);
			removeEntityAndVehicle(e);
			removed++;
		}
		return removed;
	}
	/** Picks where a breach squad enters from. Ground breaches start on the surface above the player (miners dig down). */
	private Location findBreachOrigin(Player player, Location playerLoc, boolean skybase) {
		World world = playerLoc.getWorld();
		if (skybase) {
			// Land the squad on the natural ground a short way out from the base of the player's column, so the staircase
			// miner has room to wind up. findMonsterSpawnLocation searches near the player's Y and fails for a high
			// skybase (only air up there), so resolve the ground directly via getHighestBlockYAt at a ring of offsets.
			for (int attempt = 0; attempt < 10; attempt++) {
				Location spot = skybaseGroundSpot(playerLoc, random.nextDouble() * Math.PI * 2, 6 + random.nextInt(5));
				if (spot != null)
					return spot;
			}
			return SpawnUtils.findMonsterSpawnLocation(playerLoc, 2, 6f, 16f);
		}
		int px = playerLoc.getBlockX(), pz = playerLoc.getBlockZ();
		int topY = world.getHighestBlockYAt(px, pz);
		Block surface = world.getBlockAt(px, topY, pz);
		Block above = surface.getRelative(BlockFace.UP);
		if (topY + 1 > playerLoc.getBlockY() && above.isPassable() && !above.isLiquid() && above.getRelative(BlockFace.UP).isPassable())
			return new Location(world, px + 0.5, topY + 1.01, pz + 0.5);
		return SpawnUtils.findMonsterSpawnLocation(playerLoc, 2, 10f, 22f); // fall back to a surface ring
	}
	/** A spawnable spot on the natural ground at the given angle/distance from the player's column (or null if none). */
	private Location skybaseGroundSpot(Location playerLoc, double angle, double dist) {
		World world = playerLoc.getWorld();
		int gx = playerLoc.getBlockX() + (int) Math.round(Math.cos(angle) * dist);
		int gz = playerLoc.getBlockZ() + (int) Math.round(Math.sin(angle) * dist);
		int gy = world.getHighestBlockYAt(gx, gz);
		Location spot = new Location(world, gx + 0.5, gy + 1, gz + 0.5);
		return SpawnUtils.canMonsterSpawn(spot, 2) ? spot : null;
	}
	/** Spawns one UltimateContent undead miner in breach mode. Returns false (no-op) if UltimateContent is absent or spawn fails. */
	private boolean spawnBreachMiner(Location loc, Player player) {
		if (breachMinerClass == null)
			return false;
		Entity e = UltimateContentUtils.spawnBreachMiner(loc, breachMinerClass, player, breachMinerRetention);
		if (e == null)
			return false;
		breachMiners.add(e.getUniqueId());
		registerPurgeMob(e, player);
		return true;
	}
	/**
	 * Spawns a flying assailant (phantom/vex) for an airborne player - out at distance and at the player's altitude (or a
	 * little higher), so it reads as flying in from afar to dive on the skybase rather than popping in beside the player.
	 */
	private void spawnFlyer(Player player, Location playerLoc) {
		World world = playerLoc.getWorld();
		Vector off = Utils.getRandomizedVector(1f, 0f, 1f).multiply(18 + random.nextDouble() * 14);
		Location loc = playerLoc.clone().add(off.getX(), 0, off.getZ());
		loc.setY(playerLoc.getY() + random.nextDouble() * 6);
		if (!loc.getBlock().isPassable()) // don't spawn inside terrain - lift it clear
			loc.add(0, 5, 0);
		Entity e = world.spawnEntity(loc, random.nextBoolean() ? EntityType.PHANTOM : EntityType.VEX);
		registerPurgeMob(e, player);
	}
	/**
	 * Gives a crossbow pillager a 20% chance to become a "firework crossbow" pillager that shoots fireworks instead of
	 * arrows. A repeating task swaps whatever the pillager loads into its crossbow for a firework rocket (after vanilla
	 * finishes charging, preserving its normal fire rate), so each shot launches a firework.
	 */
	private void applyFireworkCrossbow(Pillager pillager) {
		if (pillager == null || random.nextFloat() >= 0.2f)
			return;
		new BukkitRunnable() {
			@Override
			public void run() {
				if (pillager.isDead() || !pillager.isValid()) {
					cancel();
					return;
				}
				ItemStack cb = pillager.getEquipment().getItemInMainHand();
				if (cb == null || cb.getType() != Material.CROSSBOW) {
					cancel();
					return;
				}
				if (cb.getItemMeta() instanceof CrossbowMeta meta && meta.hasChargedProjectiles()
						&& meta.getChargedProjectiles().stream().noneMatch(p -> p.getType() == Material.FIREWORK_ROCKET)) {
					meta.setChargedProjectiles(Collections.singletonList(fireworkProjectile()));
					cb.setItemMeta(meta);
					pillager.getEquipment().setItemInMainHand(cb);
				}
			}
		}.runTaskTimer(plugin, 10, 5);
	}
	/** A modest firework rocket used as the crossbow projectile for firework-crossbow pillagers. */
	private static ItemStack fireworkProjectile() {
		ItemStack fw = new ItemStack(Material.FIREWORK_ROCKET);
		FireworkMeta meta = (FireworkMeta) fw.getItemMeta();
		meta.setPower(1);
		meta.addEffect(FireworkEffect.builder().withColor(Color.RED, Color.ORANGE).with(FireworkEffect.Type.BALL).withTrail().build());
		fw.setItemMeta(meta);
		return fw;
	}
	private static void removeEntityAndVehicle(Entity entity) {
		if (entity == null)
			return;
		Entity vehicle = entity.getVehicle();
		if (vehicle != null)
			vehicle.remove();
		entity.remove();
	}
	/** Targets the spawned mob on the player and tracks it (membership = the disaster entity set, getAllEntities). */
	private void registerPurgeMob(Entity e, Player player) {
		if (e instanceof Mob mob) {
			mob.setTarget(player);
			addEntityToDisasterList(mob, player);
		} else
			addEntityToDisasterList(e);
	}
	/** True if the entity belongs to THIS purge (O(1) set lookup, replacing the old per-mob metadata tag). */
	private boolean isPurgeMob(Entity e) {
		Set<UUID> set = getAllEntities();
		return e != null && set != null && set.contains(e.getUniqueId());
	}
	// === Purge event handlers (registered in start(), torn down in clean(); scoped to this disaster's mobs) ============
	/**
	 * Keeps the horde focused: a purge mob that tries to target another of this purge's mobs (usually creeper-blast /
	 * friendly-fire retaliation, which made the horde fight and kill itself, miners included) is redirected back to its
	 * stored primary target player. Targeting a player (provoked, or a nearby helper) is left alone.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPurgeTarget(EntityTargetLivingEntityEvent event) {
		if (!isPurgeMob(event.getEntity()))
			return;
		LivingEntity target = event.getTarget();
		if (target == null || !isPurgeMob(target))
			return;
		UUID primaryId = MobDisaster.entitiyTargets.get(event.getEntity().getUniqueId());
		Entity primary = primaryId == null ? null : Bukkit.getEntity(primaryId);
		if (primary instanceof LivingEntity living && !primary.isDead())
			event.setTarget(living);
		else
			event.setCancelled(true);
	}
	/** Cancels friendly fire between this purge's mobs (incl. creeper blasts and projectiles) so the horde can't kill itself. */
	@EventHandler(ignoreCancelled = true)
	public void onPurgeFriendlyFire(EntityDamageByEntityEvent event) {
		if (!isPurgeMob(event.getEntity()))
			return;
		Entity damager = event.getDamager();
		if (damager instanceof Projectile proj && proj.getShooter() instanceof Entity shooter)
			damager = shooter;
		if (isPurgeMob(damager))
			event.setCancelled(true);
	}
	/** Keeps this purge's phantoms from burning up in daylight so they can press a surface/skybase target. */
	@EventHandler(ignoreCancelled = true)
	public void onPurgePhantomBurn(EntityCombustEvent event) {
		if (event.getEntity().getType() == EntityType.PHANTOM && isPurgeMob(event.getEntity()))
			event.setCancelled(true);
	}
	/** Flags a player killed by one of this purge's mobs so DeathMessageHandler shows the purge death message. */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPurgeKillPlayer(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof Player victim) || event.getFinalDamage() < victim.getHealth())
			return;
		Entity damager = event.getDamager();
		if (damager instanceof Projectile proj && proj.getShooter() instanceof Entity shooter)
			damager = shooter;
		if (isPurgeMob(damager))
			victim.setMetadata("deaths.purge", plugin.getFixedMetadata());
	}
	@EventHandler(priority = EventPriority.MONITOR)
	public void onTargetDeath(PlayerDeathEvent event) {
		if (targetUUID == null || !event.getEntity().getUniqueId().equals(targetUUID))
			return;
		plugin.getServer().getScheduler().runTask(plugin, () -> {
			if (!hasEnded())
				stop();
		});
	}
	/**
	 * True if the player is perched on a small, elevated platform/pillar (a skybase) rather than on broad natural ground.
	 * Earlier height-ring versions flickered depending on where you stood (e.g. on the central pillar block vs the platform
	 * around it), because a ring sample could land on the build itself. Instead this flood-fills the connected walkable
	 * surface the player is standing on (bounded by {@code radius}/{@code cap}): if that surface is <b>small</b> (doesn't
	 * sprawl past the radius - so it's a built perch, not the ground) AND at least one of its edges drops away >=5 blocks
	 * into air (so it's actually up high), it's a skybase. Standing anywhere on the same platform gives the same answer.
	 */
	private boolean isSkybase(Location playerLoc) {
		World world = playerLoc.getWorld();
		int px = playerLoc.getBlockX(), py = playerLoc.getBlockY(), pz = playerLoc.getBlockZ();
		// Find the surface block the player stands on (solid within 2 below, scanning a 3x3 so edge-crouching still finds it).
		Block surface = null;
		outer:
		for (int dy = 0; dy <= 2; dy++)
			for (int dx = -1; dx <= 1; dx++)
				for (int dz = -1; dz <= 1; dz++) {
					Block b = world.getBlockAt(px + dx, py - 1 - dy, pz + dz);
					if (!b.isPassable()) {
						surface = b;
						break outer;
					}
				}
		if (surface == null)
			return false; // free-falling, not perched
		final int surfY = surface.getY();
		final int sx = surface.getX(), sz = surface.getZ();
		final int radius = 8, cap = 200;
		Set<Long> visited = new HashSet<>();
		ArrayDeque<long[]> queue = new ArrayDeque<>();
		visited.add((((long) sx) << 32) | (sz & 0xffffffffL));
		queue.add(new long[] {sx, sz});
		boolean bounded = true, elevated = false;
		while (!queue.isEmpty()) {
			if (visited.size() > cap) {
				bounded = false;
				break;
			}
			long[] c = queue.poll();
			int cx = (int) c[0], cz = (int) c[1];
			if (Math.abs(cx - sx) > radius || Math.abs(cz - sz) > radius) {
				bounded = false; // surface sprawls -> natural ground, not a perch
				break;
			}
			for (int[] d : new int[][] { {1, 0}, {-1, 0}, {0, 1}, {0, -1} }) {
				int nx = cx + d[0], nz = cz + d[1];
				long key = (((long) nx) << 32) | (nz & 0xffffffffL);
				if (visited.contains(key))
					continue;
				Block atLevel = world.getBlockAt(nx, surfY, nz);
				if (!atLevel.isPassable() && world.getBlockAt(nx, surfY + 1, nz).isPassable()) {
					visited.add(key); // another walkable platform cell
					queue.add(new long[] {nx, nz});
				} else if (atLevel.isPassable() && !elevated) {
					// off the platform at this level: measure the drop here to confirm we're up in the air
					int gap = 0;
					for (int k = 1; k <= 8; k++) {
						if (world.getBlockAt(nx, surfY - k, nz).isPassable())
							gap++;
						else
							break;
					}
					if (gap >= 5)
						elevated = true;
				}
			}
		}
		return bounded && elevated;
	}
	private Location scatter(Location origin, double radius) {
		return origin.clone().add((random.nextDouble() - 0.5) * 2 * radius, 0, (random.nextDouble() - 0.5) * 2 * radius);
	}
	protected String getConfigPath() {
		return "disasters.mob.purge";
	}
	public String getBroadcastMessageConfigPath() {
		return "messages.disaster_broadcasts.purge.started.level_"+level;
	}
	public static void checkForPlayerInMap(Player player) {
		if (player == null)
			return;
		Map<String, Integer> map = DataUtils.computeSection(DataUtils.getDataFile(), storedPlayersKey, new HashMap<String, Integer>());
		Integer value = map.remove(player.getUniqueId().toString());
		if (value == null)
			return;
		DataUtils.writeToDataFile(file -> file.set(storedPlayersKey, map));
		Purge purge = new Purge(player.getLocation(), player, value);
		if (!purge.canStart(DisasterStartReason.CUSTOM))
			return;
		purge.init();
		purge.start();
	}
	
	private class EntityContainer {
		
		private Class<?> entityClass;
		private float chance = 5f;
		private double health;
		private double damage;
		private double speed;
		private boolean randomize = true;
		private ItemStack[] armor;
		private ItemStack mainHand;
		private ItemStack offHand;
		
		private boolean isUCType;
		private boolean chargedCreeper;
		
		@SuppressWarnings("unchecked")
		private Entity spawnEntity(Location location) {
			Entity entity = null;
			if (isUCType) {
				entity = UltimateContentUtils.spawnEntity(location, entityClass);
				if (entity == null)
					return null;
				if (entity instanceof LivingEntity living) {
					if (health != 0) {
						living.getAttribute(VersionUtils.getMaxHealthAttribute()).setBaseValue(health);
						living.setHealth(health);
					}
					if (damage != 0)
						living.getAttribute(VersionUtils.getAttackDamageAttribute()).setBaseValue(damage);
					if (speed != 0)
						living.getAttribute(VersionUtils.getMovementSpeedAttribute()).setBaseValue(speed);
					if (armor != null)
						living.getEquipment().setArmorContents(armor);
					if (mainHand != null)
						plugin.getServer().getScheduler().runTaskLater(plugin, () -> living.getEquipment().setItemInMainHand(mainHand), 1);
					if (offHand != null)
						plugin.getServer().getScheduler().runTaskLater(plugin, () -> living.getEquipment().setItemInOffHand(offHand), 1);
					if (living instanceof Creeper creeper)
						creeper.setPowered(chargedCreeper);
					
					living.getAttribute(VersionUtils.getFollowRangeAttribute()).setBaseValue(50.0);
				}
			} else
				entity = location.getWorld().spawn(location, (Class<? extends Entity>) entityClass, randomize, temp -> {
					if (temp instanceof LivingEntity living) {
						if (health != 0) {
							living.getAttribute(VersionUtils.getMaxHealthAttribute()).setBaseValue(health);
							living.setHealth(health);
						}
						if (damage != 0)
							living.getAttribute(VersionUtils.getAttackDamageAttribute()).setBaseValue(damage);
						if (speed != 0)
							living.getAttribute(VersionUtils.getMovementSpeedAttribute()).setBaseValue(speed);
						if (armor != null)
							living.getEquipment().setArmorContents(armor);
						if (mainHand != null)
							plugin.getServer().getScheduler().runTaskLater(plugin, () -> living.getEquipment().setItemInMainHand(mainHand), 1);
						if (offHand != null)
							plugin.getServer().getScheduler().runTaskLater(plugin, () -> living.getEquipment().setItemInOffHand(offHand), 1);
						if (temp instanceof Creeper creeper)
							creeper.setPowered(chargedCreeper);
						if (temp instanceof Ravager) {
							Pillager rider = temp.getWorld().spawn(location, Pillager.class);
							temp.addPassenger(rider);
							applyFireworkCrossbow(rider);
						}

						living.getAttribute(VersionUtils.getFollowRangeAttribute()).setBaseValue(50.0);
					}
				});
			if (entity instanceof Pillager pil)
				applyFireworkCrossbow(pil);
			return entity;
		}
		private static EntityContainer createContainer(Map<?, ?> map, Purge disaster) {
			Object typeName = map.get("type");
			if (typeName == null || !(typeName instanceof String typeString)) {
				Utils.sendConsoleMessage("&eWARNING there is an entity entry with no specified entity type for the Purge disaster in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"!");
				return null;
			}
			EntityContainer container = disaster.new EntityContainer();
			try {
				EntityType vanillaType = EntityType.valueOf(typeString.toUpperCase());
				container.entityClass = vanillaType.getEntityClass();
			} catch (IllegalArgumentException ex) {
				if (typeString.toLowerCase().startsWith("uc:")) {
					if (DependencyUtils.isUltimateContentEnabled()) {
						Class<?> entityClass = UltimateContentUtils.getEntityClass(typeString.toLowerCase());
						if (entityClass == null) {
							Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry does not exist in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e!");
							Utils.sendConsoleMessage("&eWARNING a Purge entity entry was improperly entered and must be fixed in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e!");
							return null;
						}
						container.entityClass = entityClass;
						container.isUCType = true;
					} else
						return null;
				} else {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry does not exist in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e!");
					Utils.sendConsoleMessage("&eWARNING a Purge entity entry was improperly entered and must be fixed in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e!");
					return null;
				}
			}
			if (map.containsKey("chance"))
				try {
					container.chance = (float) ((double) map.get("chance"));
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'chance' &evalue in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			if (map.containsKey("health"))
				try {
					container.health = (double) map.get("health");
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'health' &evalue in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			if (map.containsKey("damage"))
				try {
					container.damage = (double) map.get("damage");
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'damage' &evalue in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			if (map.containsKey("speed"))
				try {
					container.speed = (double) map.get("speed");
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'speed' &evalue in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			if (map.containsKey("randomize"))
				try {
					container.randomize = (Boolean) map.get("randomize");
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'randomize' &evalue in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			if (map.containsKey("feet"))
				try {
					ItemStack item = createEquipmentItemStack((String) map.get("feet"));
					if (item != null) {
						if (container.armor == null)
							container.armor = new ItemStack[4];
						container.armor[0] = item;
					} else
						Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'feet' &evalue, no such item &b'"+String.valueOf(map.get("feet"))+"' &eexists in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'feet' &evalue, only text values are acceptable! In the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			if (map.containsKey("legs"))
				try {
					ItemStack item = createEquipmentItemStack((String) map.get("legs"));
					if (item != null) {
						if (container.armor == null)
							container.armor = new ItemStack[4];
						container.armor[1] = item;
					} else
						Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'legs' &evalue, no such item &b'"+String.valueOf(map.get("legs"))+"' &eexists in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'legs' &evalue, only text values are acceptable! In the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			if (map.containsKey("chest"))
				try {
					ItemStack item = createEquipmentItemStack((String) map.get("chest"));
					if (item != null) {
						if (container.armor == null)
							container.armor = new ItemStack[4];
						container.armor[2] = item;
					} else
						Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'chest' &evalue, no such item &b'"+String.valueOf(map.get("chest"))+"' &eexists in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'chest' &evalue, only text values are acceptable! In the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			if (map.containsKey("head"))
				try {
					ItemStack item = createEquipmentItemStack((String) map.get("head"));
					if (item != null) {
						if (container.armor == null)
							container.armor = new ItemStack[4];
						container.armor[3] = item;
					} else
						Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'head' &evalue, no such item &b'"+String.valueOf(map.get("head"))+"' &eexists in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'head' &evalue, only text values are acceptable! In the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			if (map.containsKey("main_hand"))
				try {
					ItemStack item = createEquipmentItemStack((String) map.get("main_hand"));
					if (item != null)
						container.mainHand = item;
					else
						Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'main_hand' &evalue, no such item &b'"+String.valueOf(map.get("main_hand"))+"' &eexists in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'main_hand' &evalue, only text values are acceptable! In the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			if (map.containsKey("off_hand"))
				try {
					ItemStack item = createEquipmentItemStack((String) map.get("off_hand"));
					if (item != null)
						container.offHand = item;
					else
						Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'off_hand' &evalue, no such item &b'"+String.valueOf(map.get("off_hand"))+"' &eexists in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'off_hand' &evalue, only text values are acceptable! In the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			if (map.containsKey("charged"))
				try {
					container.chargedCreeper = (Boolean) map.get("charged");
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'charged' &evalue in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			return container;
		}
		private static ItemStack createEquipmentItemStack(String item) {
			if (DependencyUtils.isUltimateContentEnabled()) {
				ItemStack customItem = UltimateContentUtils.getItem(item);
				if (customItem != null)
					return customItem;
			}
			Material material = Material.getMaterial(item.toUpperCase());
			return material == null ? null : new ItemStack(material);
		}
	}
}
