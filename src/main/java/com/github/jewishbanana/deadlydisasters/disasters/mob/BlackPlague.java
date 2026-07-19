package com.github.jewishbanana.deadlydisasters.disasters.mob;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.github.jewishbanana.deadlydisasters.disasters.Disaster;
import com.github.jewishbanana.deadlydisasters.disasters.MobDisaster;
import com.github.jewishbanana.deadlydisasters.listeners.EntitiesListener;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.UltimateContentUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class BlackPlague extends Disaster implements MobDisaster, Listener {

	private static final String METADATA_KEY = "dd-plague";
	private static final String DEATH_KEY = "deaths.black_plague";
	private static final int PERSISTENCE_VERSION = 1;
	private static final int FLAG_CARRIER = 1;
	private static final int FLAG_PLAGUE_RAT = 1 << 1;
	private static final int FLAG_PLAGUE_BAT = 1 << 2;
	private static final int FLAG_UNDEAD = 1 << 3;
	private static final DustOptions BLACK_DUST = new DustOptions(Color.fromRGB(0, 0, 0), 0.7F);
	private static final Sound SCULK_CATALYST_BLOOM = VersionUtils.getSound("BLOCK_SCULK_CATALYST_BLOOM");
	private static final Sound SNIFFER_SNIFFING = VersionUtils.getSound("ENTITY_SNIFFER_SNIFFING");
	private static final BlockFace[] HORIZONTAL_FACES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
	private static final Map<UUID, Infection> infections = new HashMap<>();
	private static final Set<BlackPlague> activeDisasters = new HashSet<>();
	private static final Map<UUID, BlackPlague> potionOwners = new HashMap<>();
	private static final Map<RestoredOwnerKey, BlackPlague> restoredOwners = new HashMap<>();
	private static final NamespacedKey PLAGUE_PROGRESS_KEY = new NamespacedKey(plugin, "plague_progress");
	private static final NamespacedKey PLAGUE_STATE_KEY = new NamespacedKey(plugin, "plague_state");
	private static final NamespacedKey PLAGUE_TRANSMISSION_KEY = new NamespacedKey(plugin, "plague_transmission");
	private static final NamespacedKey PLAGUE_HOSTILE_KEY = new NamespacedKey(plugin, "plague_hostile");
	private static int infectionTaskId = -1;
	private static boolean plagueBiteListenerRegistered;

	private int maxInfectedMobs;
	private int infectionTicks;
	private int durationTicks;
	private int omenTicks;
	private int outbreakPulseTicks;
	private int nearbyInfectedTarget;
	private int pulseSeedLimit;
	private int ratMinLevel;
	private int maxRatsNearPlayer;
	private int ratPulseSpawnCount;
	private int ratPlayerSwarmMinCount;
	private int ratPlayerSwarmMaxCount;
	private int ratPlayerSwarmIntervalTicks;
	private int ratLifetimeTicks;
	private int batMinLevel;
	private int maxBatsNearPlayer;
	private int batPulseSpawnCount;
	private double batPulseSpawnChance;
	private int batLifetimeTicks;
	private int undeadCarrierTicks;
	private double carrierChance;
	private double ratPlayerTargetChance;
	private double ratDirectSwarmFraction;
	private double batPlayerTargetChance;
	private double batSpawnMinPlayerDistance;
	private double batSpawnMaxPlayerDistance;
	private double batCaveSpawnMultiplier;
	private double infectionTransmissionChance;
	private double proximitySpreadMargin;
	private double outbreakRadius;
	private double verticalPreference;
	private double patientZeroMinPlayerDistance;
	private double ratSpawnMinPlayerDistance;
	private double ratSpawnMaxPlayerDistance;
	private double ratUndergroundMinPlayerDistance;
	private double ratUndergroundMaxPlayerDistance;
	private double naturalRecoveryChance;
	private double corpseBurstRadius;
	private double corpseBurstChance;
	private boolean corpseBurstEnabled;
	private boolean ratSpawningEnabled;
	private boolean batSpawningEnabled;
	private boolean milkCuresPlague;
	private boolean playOmenSound;
	private boolean playerCoughingSounds;
	private boolean villagerCoughingSounds;
	private boolean animalCoughingSounds;
	private boolean curePlagueInRegions;
	private double rabidDamage;
	private int proximitySpreadIntervalTicks;
	private boolean levelSixRampant;
	private boolean seedingComplete;
	private int nextPlayerRatSwarmTicks;

	private final Set<UUID> ownedInfections = new HashSet<>();

	public BlackPlague(@NotNull Location location, Player player, int level) {
		super(location, player, level);
	}
	@Override
	public void init() {
		super.init();
		this.maxInfectedMobs = Math.max(1, getConfigInt("max_infected_mobs.level_"+level));
		this.infectionTicks = Math.max(20, getConfigInt("infection_duration_seconds") * 20);
		this.durationTicks = Math.max(20, getConfigInt("duration_seconds.level_"+level) * 20);
		this.omenTicks = Math.max(0, getConfigInt("omen_seconds") * 20);
		this.outbreakPulseTicks = Math.max(20, getConfigInt("outbreak_pulse_seconds.level_"+level) * 20);
		this.nearbyInfectedTarget = Math.max(1, getConfigInt("nearby_infected_target.level_"+level));
		this.pulseSeedLimit = Math.max(1, getConfigInt("pulse_infections.level_"+level));
		this.ratMinLevel = Math.max(1, getConfigInt("plague_rats.min_level"));
		this.maxRatsNearPlayer = Math.max(0, getConfigInt("plague_rats.max_near_player.level_"+level));
		this.ratPulseSpawnCount = Math.max(1, getConfigInt("plague_rats.pulse_spawn_count.level_"+level));
		this.ratPlayerSwarmMinCount = Math.max(0, getConfigInt("plague_rats.player_swarms.min_count.level_"+level));
		this.ratPlayerSwarmMaxCount = Math.max(ratPlayerSwarmMinCount, getConfigInt("plague_rats.player_swarms.max_count.level_"+level));
		this.ratPlayerSwarmIntervalTicks = Math.max(outbreakPulseTicks, getConfigInt("plague_rats.player_swarms.interval_seconds.level_"+level) * 20);
		this.ratLifetimeTicks = Math.max(20, getConfigInt("plague_rats.lifetime_seconds") * 20);
		this.batMinLevel = Math.max(1, getConfigInt("plague_bats.min_level"));
		this.maxBatsNearPlayer = Math.max(0, getConfigInt("plague_bats.max_near_player.level_"+level));
		this.batPulseSpawnCount = Math.max(0, getConfigInt("plague_bats.pulse_spawn_count.level_"+level));
		this.batPulseSpawnChance = clampChance(getConfigDouble("plague_bats.pulse_spawn_chance"));
		this.batLifetimeTicks = Math.max(20, getConfigInt("plague_bats.lifetime_seconds") * 20);
		this.undeadCarrierTicks = Math.max(20, getConfigInt("undead_carrier_seconds") * 20);
		this.carrierChance = clampChance(getConfigDouble("carrier_chance.level_"+level));
		this.ratPlayerTargetChance = clampChance(getConfigDouble("plague_rats.player_target_chance"));
		this.ratDirectSwarmFraction = clampChance(getConfigDouble("plague_rats.player_swarms.direct_target_percent"));
		this.batPlayerTargetChance = clampChance(getConfigDouble("plague_bats.player_target_chance"));
		this.batSpawnMinPlayerDistance = Math.max(6.0, getConfigDouble("plague_bats.spawn_min_player_distance"));
		this.batSpawnMaxPlayerDistance = Math.max(batSpawnMinPlayerDistance + 6.0, getConfigDouble("plague_bats.spawn_max_player_distance"));
		this.batCaveSpawnMultiplier = Math.max(1.0, getConfigDouble("plague_bats.cave_spawn_multiplier"));
		this.infectionTransmissionChance = clampChance(getConfigDouble("infection_transmission_chance.level_"+level));
		this.proximitySpreadIntervalTicks = Math.max(20, getConfigInt("proximity_spread_interval_seconds") * 20);
		this.proximitySpreadMargin = Math.max(0.0, getConfigDouble("proximity_spread_margin"));
		this.outbreakRadius = Math.max(8.0, getConfigDouble("outbreak_radius"));
		this.disasterRange = outbreakRadius;
		this.verticalPreference = Math.max(4.0, getConfigDouble("vertical_preference"));
		this.patientZeroMinPlayerDistance = Math.max(0.0, getConfigDouble("patient_zero_min_player_distance"));
		double ratMin = getConfigDouble("plague_rats.spawn_min_player_distance");
		double ratMax = getConfigDouble("plague_rats.spawn_max_player_distance");
		double undergroundRatMin = getConfigDouble("plague_rats.underground_spawn_min_player_distance");
		double undergroundRatMax = getConfigDouble("plague_rats.underground_spawn_max_player_distance");
		this.ratSpawnMinPlayerDistance = Math.max(0.0, ratMin);
		this.ratSpawnMaxPlayerDistance = Math.max(ratSpawnMinPlayerDistance + 8.0, ratMax);
		this.ratUndergroundMinPlayerDistance = Math.max(8.0, undergroundRatMin);
		this.ratUndergroundMaxPlayerDistance = Math.max(ratUndergroundMinPlayerDistance + 6.0, undergroundRatMax);
		this.naturalRecoveryChance = clampChance(getConfigDouble("natural_recovery_chance.level_"+level));
		this.corpseBurstEnabled = getConfigBoolean("corpse_burst.enabled");
		this.ratSpawningEnabled = getConfigBoolean("plague_rats.enabled");
		this.batSpawningEnabled = getConfigBoolean("plague_bats.enabled");
		this.corpseBurstRadius = Math.max(0.25, getConfigDouble("corpse_burst.radius"));
		this.corpseBurstChance = clampChance(getConfigDouble("corpse_burst.chance.level_"+level));
		this.milkCuresPlague = getConfigBoolean("milk_cures_plague");
		this.playOmenSound = getConfigBoolean("play_omen_sound");
		this.playerCoughingSounds = getConfigBoolean("player_coughing_sounds");
		this.villagerCoughingSounds = getConfigBoolean("villager_coughing_sounds");
		this.animalCoughingSounds = getConfigBoolean("animal_coughing_sounds");
		this.curePlagueInRegions = getWorldLink().getConfigBoolean("protection_settings.region_plugins.cure_plague_in_regions");
		this.rabidDamage = Math.max(0.0, getConfigDouble("rabid_damage"));
		this.levelSixRampant = level >= 6;
		this.nextPlayerRatSwarmTicks = omenTicks + ratPlayerSwarmIntervalTicks;
		if (DependencyUtils.isUltimateContentEnabled() && !plagueBiteListenerRegistered)
			plagueBiteListenerRegistered = UltimateContentUtils.registerPlagueBiteListener(BlackPlague::onPlagueHelperBite);
	}
	@Override
	public boolean canStart() {
		return location != null && (player != null || findStarterTargets(location, true).size() > 0) && super.canStart();
	}
	@Override
	public void start() {
		super.start();
		activeDisasters.add(this);
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		ensureInfectionTask();
		Location startAnchor = getAnchorLocation();
		if (startAnchor != null && playOmenSound)
			playStartSound(startAnchor);
		scheduleTask(new BukkitRunnable() {
			private int ticks;
			private int pulseTicks = omenTicks;

			@Override
			public void run() {
				ticks += 20;
				Location anchor = getAnchorLocation();
				if (anchor == null) {
					seedingComplete = true;
				} else if (ticks <= omenTicks) {
					playOmen(anchor, ticks);
				} else if (ticks <= durationTicks) {
					pulseTicks += 20;
					if (pulseTicks >= outbreakPulseTicks) {
						pulseTicks = 0;
						outbreakPulse(anchor, ticks);
					}
				} else {
					seedingComplete = true;
				}
				if (seedingComplete) {
					stop();
					return;
				}
			}
		}.runTaskTimer(plugin, 20, 20));
	}
	@Override
	public void clean() {
		super.clean();
		activeDisasters.remove(this);
		if (!hasOwnedInfections())
			HandlerList.unregisterAll(this);
	}
	@Override
	protected String getConfigPath() {
		return "disasters.mob.black_plague";
	}
	@Override
	public String getBroadcastMessageConfigPath() {
		return "messages.disaster_broadcasts.plague.started.level_"+level;
	}
	@Override
	public Function<PlayerDeathEvent, Boolean> getDeathCheck() {
		return event -> event.getEntity().hasMetadata(DEATH_KEY);
	}
	@Override
	public Set<Environment> getBannedEnvironments() {
		return Set.of(Environment.THE_END);
	}
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onContactDamage(EntityDamageByEntityEvent event) {
		Entity damager = event.getDamager();
		if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter)
			damager = shooter;
		if (!(event.getEntity() instanceof LivingEntity victim) || !(damager instanceof LivingEntity attacker))
			return;
		Infection source = infections.get(attacker.getUniqueId());
		if (source == null || source.owner != this || infections.containsKey(victim.getUniqueId()))
			return;
		if (plagueBiteListenerRegistered && (source.plagueRat || source.plagueBat))
			return;
		tryTransmitFromHit(source, victim);
	}
	private static void onPlagueHelperBite(LivingEntity carrier, LivingEntity victim) {
		Infection source = infections.get(carrier.getUniqueId());
		if (source == null || (!source.plagueRat && !source.plagueBat) || infections.containsKey(victim.getUniqueId()))
			return;
		tryTransmitFromHit(source, victim);
	}
	private static void tryTransmitFromHit(Infection source, LivingEntity victim) {
		if (random.nextDouble() >= source.transmissionChance)
			return;
		boolean ignoreCap = victim instanceof Player;
		infectEntity(victim, source.owner, !DependencyUtils.isUltimateContentEnabled() && source.carrier && random.nextDouble() < 0.25, false, false,
				ignoreCap, source.transmissionChance);
	}
	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityDeath(EntityDeathEvent event) {
		LivingEntity entity = event.getEntity();
		Infection infection = infections.get(entity.getUniqueId());
		if (infection == null)
			return;
		if (!infection.plagueRat && !infection.plagueBat && !infection.undeadCarrier
				&& (entity instanceof Villager || entity instanceof WanderingTrader))
			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_FOX_SCREECH, 0.8f, (float) random.nextDouble(0.5, 0.6));
		if (corpseBurstEnabled && !infection.plagueRat && !infection.plagueBat)
			corpseBurst(entity.getLocation(), infection.owner, infection.carrier);
		removeInfection(entity.getUniqueId(), infection);
	}
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		Infection infection = infections.get(player.getUniqueId());
		if (infection == null)
			return;
		removeInfection(player.getUniqueId(), infection);
	}
	@EventHandler(ignoreCancelled = true)
	public void onMilkDrink(PlayerItemConsumeEvent event) {
		if (!milkCuresPlague || event.getItem() == null || event.getItem().getType() != Material.MILK_BUCKET)
			return;
		if (isInfected(event.getPlayer()))
			cureEntity(event.getPlayer());
	}
	@EventHandler(ignoreCancelled = true)
	public void onGameModeChange(PlayerGameModeChangeEvent event) {
		if (event.getNewGameMode() == GameMode.CREATIVE && isInfected(event.getPlayer()))
			cureEntity(event.getPlayer());
	}
	private Location getAnchorLocation() {
		if (player != null && player.isOnline() && !player.isDead() && player.isValid()) {
			location = player.getLocation();
			return location;
		}
		return location != null && location.getWorld() != null ? location : null;
	}
	private void playStartSound(Location anchor) {
		double rangeSquared = 30.0 * 30.0;
		for (Player nearby : anchor.getWorld().getPlayers()) {
			if (nearby.getLocation().distanceSquared(anchor) <= rangeSquared)
				nearby.playSound(anchor, Sound.ENTITY_WITHER_SPAWN, 0.1f, 0.5f);
		}
	}
	private void playOmen(Location anchor, int ticks) {
		double radius = Math.min(outbreakRadius, 8.0 + ticks / 20.0);
		Location loc = anchor.clone().add(random.nextDouble(-radius, radius), random.nextDouble(0.2, 1.2), random.nextDouble(-radius, radius));
		anchor.getWorld().spawnParticle(VersionUtils.getRedstoneDust(), loc, 5, 0.2, 0.15, 0.2, 0.001, BLACK_DUST);
	}
	private void outbreakPulse(Location anchor, int elapsedDisasterTicks) {
		double escalation = getVillageEscalation(anchor);
		if (DependencyUtils.isUltimateContentEnabled()) {
			boolean spawnedHelpers = false;
			if (ratSpawningEnabled && level >= ratMinLevel) {
				spawnedHelpers = true;
				int nearbyRats = getNearbyOwnedInfections(anchor, outbreakRadius, verticalPreference + 12.0, true);
				if (ratPlayerSwarmMaxCount > 0 && elapsedDisasterTicks >= nextPlayerRatSwarmTicks && isValidRatTargetPlayer(player, true)) {
					nextPlayerRatSwarmTicks += ratPlayerSwarmIntervalTicks;
					int swarmCount = random.nextInt(ratPlayerSwarmMinCount, ratPlayerSwarmMaxCount + 1);
					spawnRatSwarm(anchor, this, (int) Math.ceil(swarmCount * escalation), RatTargetMode.PLAYER_SWARM);
				} else {
					int spawnCount = Math.max(1, (int) Math.ceil(ratPulseSpawnCount * escalation));
					spawnRatSwarm(anchor, this, Math.min(spawnCount, Math.max(0, maxRatsNearPlayer - nearbyRats)), RatTargetMode.AREA_TARGETS);
				}
			}
			if (batSpawningEnabled && level >= batMinLevel && batPulseSpawnCount > 0) {
				spawnedHelpers = true;
				boolean caveOutbreak = isUnderground(anchor);
				double spawnMultiplier = caveOutbreak ? batCaveSpawnMultiplier : 1.0;
				if (random.nextDouble() < Math.min(1.0, batPulseSpawnChance * spawnMultiplier)) {
					int nearbyBats = getNearbyOwnedBats(anchor, outbreakRadius, verticalPreference + 20.0);
					int spawnCount = Math.max(1, (int) Math.ceil(batPulseSpawnCount * escalation * spawnMultiplier));
					int localCap = Math.max(1, (int) Math.ceil(maxBatsNearPlayer * spawnMultiplier));
					spawnBatGroup(anchor, this, Math.min(spawnCount, Math.max(0, localCap - nearbyBats)));
				}
			}
			if (spawnedHelpers)
				return;
		}
		int nearbyInfected = getNearbyOwnedInfections(anchor, outbreakRadius, verticalPreference + 8.0, false);
		int target = (int) Math.ceil(nearbyInfectedTarget * escalation);
		int seedLimit = Math.max(1, (int) Math.ceil(pulseSeedLimit * escalation));
		if (nearbyInfected >= target || getGlobalInfectionCount(anchor.getWorld()) >= maxInfectedMobs)
			return;
		int needed = Math.min(seedLimit, target - nearbyInfected);
		for (LivingEntity entity : findStarterTargets(anchor, true)) {
			if (needed <= 0 || getGlobalInfectionCount(anchor.getWorld()) >= maxInfectedMobs)
				break;
			if (infectEntity(entity, this, rollCarrier(entity, escalation), false))
				needed--;
		}
	}
	private int getNearbyOwnedInfections(Location anchor, double radius, double yRadius, boolean ratsOnly) {
		int count = 0;
		double radiusSquared = radius * radius;
		for (UUID uuid : ownedInfections) {
			Infection infection = infections.get(uuid);
			if (infection == null || (ratsOnly && !infection.plagueRat))
				continue;
			Entity entity = Bukkit.getEntity(uuid);
			if (entity == null || !entity.getWorld().equals(anchor.getWorld()))
				continue;
			Location loc = entity.getLocation();
			if (Math.abs(loc.getY() - anchor.getY()) <= yRadius && loc.distanceSquared(anchor) <= radiusSquared)
				count++;
		}
		return count;
	}
	private int getNearbyOwnedBats(Location anchor, double radius, double yRadius) {
		int count = 0;
		double radiusSquared = radius * radius;
		for (UUID uuid : ownedInfections) {
			Infection infection = infections.get(uuid);
			if (infection == null || !infection.plagueBat)
				continue;
			Entity entity = Bukkit.getEntity(uuid);
			if (entity == null || !entity.getWorld().equals(anchor.getWorld()))
				continue;
			Location loc = entity.getLocation();
			if (Math.abs(loc.getY() - anchor.getY()) <= yRadius && loc.distanceSquared(anchor) <= radiusSquared)
				count++;
		}
		return count;
	}
	private List<LivingEntity> findStarterTargets(Location anchor, boolean requireVerticalMatch) {
		List<LivingEntity> targets = new ArrayList<>();
		for (Entity entity : anchor.getWorld().getNearbyEntities(anchor, outbreakRadius, Math.max(verticalPreference, 20.0), outbreakRadius)) {
			if (!(entity instanceof LivingEntity living) || !isValidStarterTarget(living, anchor, requireVerticalMatch))
				continue;
			targets.add(living);
		}
		targets.sort(Comparator.comparingDouble(e -> starterScore(e, anchor)));
		return targets;
	}
	private boolean isValidStarterTarget(LivingEntity entity, Location anchor, boolean requireVerticalMatch) {
		if (entity instanceof Player || !isValidInfectionTarget(entity) || isStarterImmuneTamedMob(entity) || !isSafeStarterDistance(entity))
			return false;
		return !requireVerticalMatch || Math.abs(entity.getLocation().getY() - anchor.getY()) <= verticalPreference;
	}
	private double starterScore(LivingEntity entity, Location anchor) {
		double score = priority(entity) * 1000.0 + entity.getLocation().distanceSquared(anchor);
		double y = Math.abs(entity.getLocation().getY() - anchor.getY());
		score += y * 35.0;
		if (isNearVillageOrFarm(entity.getLocation()))
			score -= 180.0;
		if (player != null && player.isOnline() && player.getWorld().equals(entity.getWorld()) && entity.hasLineOfSight(player))
			score -= 80.0;
		return score;
	}
	private int priority(LivingEntity entity) {
		if (entity instanceof Villager || entity instanceof WanderingTrader)
			return 0;
		if (entity instanceof Animals)
			return 1;
		if (entity.getType() == EntityType.ZOMBIE || entity instanceof Monster)
			return 2;
		return 3;
	}
	private boolean rollCarrier(LivingEntity entity, double escalation) {
		double chance = carrierChance;
		if (entity instanceof Villager || entity instanceof WanderingTrader)
			chance += 0.08;
		if (levelSixRampant)
			chance += 0.08;
		chance *= escalation;
		return random.nextDouble() < Math.min(0.9, chance);
	}
	private double getVillageEscalation(Location anchor) {
		int score = 0;
		for (Entity entity : anchor.getWorld().getNearbyEntities(anchor, 18.0, 10.0, 18.0)) {
			if (entity instanceof Villager || entity instanceof WanderingTrader)
				score += 4;
			else if (entity instanceof Animals)
				score++;
		}
		for (int x = -8; x <= 8; x += 4) {
			for (int z = -8; z <= 8; z += 4) {
				Block block = anchor.getWorld().getHighestBlockAt(anchor.clone().add(x, 0, z));
				Material type = block.getType();
				if (type == Material.BELL || type == Material.HAY_BLOCK || type.name().endsWith("_BED") || type.name().contains("CROP") || type.name().contains("WHEAT"))
					score += 2;
			}
		}
		return score >= 14 ? 1.35 : score >= 7 ? 1.2 : score >= 3 ? 1.1 : 1.0;
	}
	private boolean isNearVillageOrFarm(Location loc) {
		for (Entity entity : loc.getWorld().getNearbyEntities(loc, 10.0, 6.0, 10.0)) {
			if (entity instanceof Villager || entity instanceof WanderingTrader)
				return true;
		}
		for (int x = -5; x <= 5; x += 5) {
			for (int z = -5; z <= 5; z += 5) {
				Material type = loc.getWorld().getHighestBlockAt(loc.clone().add(x, 0, z)).getType();
				if (type == Material.BELL || type == Material.HAY_BLOCK || type.name().endsWith("_BED") || type.name().contains("CROP") || type.name().contains("WHEAT"))
					return true;
			}
		}
		return false;
	}
	private boolean isStarterImmuneTamedMob(LivingEntity entity) {
		if (entity instanceof Tameable tameable && tameable.isTamed())
			return true;
		return DependencyUtils.isUltimateContentEnabled() && UltimateContentUtils.isTamedOrOwned(entity);
	}
	private boolean isSafeStarterDistance(LivingEntity entity) {
		if (patientZeroMinPlayerDistance <= 0)
			return true;
		double minDistanceSquared = patientZeroMinPlayerDistance * patientZeroMinPlayerDistance;
		for (Player player : entity.getWorld().getPlayers()) {
			if (!EntityUtils.isPlayerImmune(player) && player.getLocation().distanceSquared(entity.getLocation()) < minDistanceSquared)
				return false;
		}
		return true;
	}
	private boolean isValidInfectionTarget(LivingEntity entity) {
		if (entity == null || entity.isDead() || !entity.isValid() || entity.isInvulnerable() || infections.containsKey(entity.getUniqueId()))
			return false;
		if (entity instanceof Player player && EntityUtils.isPlayerImmune(player))
			return false;
		return !isEntityProtected(entity);
	}
	private static boolean infectEntity(LivingEntity entity, BlackPlague owner, boolean carrier, boolean plagueRat) {
		return infectEntity(entity, owner, carrier, plagueRat, false, false);
	}
	private static boolean infectEntity(LivingEntity entity, BlackPlague owner, boolean carrier, boolean plagueRat, boolean plagueBat) {
		return infectEntity(entity, owner, carrier, plagueRat, plagueBat, false);
	}
	private static boolean infectEntity(LivingEntity entity, BlackPlague owner, boolean carrier, boolean plagueRat, boolean plagueBat, boolean ignoreCap) {
		return infectEntity(entity, owner, carrier, plagueRat, plagueBat, ignoreCap, owner.infectionTransmissionChance);
	}
	private static boolean infectEntity(LivingEntity entity, BlackPlague owner, boolean carrier, boolean plagueRat, boolean plagueBat, boolean ignoreCap,
			double transmissionChance) {
		if (owner == null || !owner.isValidInfectionTarget(entity) || (!ignoreCap && owner.getGlobalInfectionCount(entity.getWorld()) >= owner.maxInfectedMobs))
			return false;
		boolean undeadCarrier = !plagueRat && !plagueBat && isUndeadCarrier(entity);
		int lifetime = plagueRat ? owner.ratLifetimeTicks : plagueBat ? owner.batLifetimeTicks : undeadCarrier ? owner.undeadCarrierTicks : owner.infectionTicks;
		Infection infection = new Infection(owner, lifetime, carrier || plagueRat || plagueBat || undeadCarrier, plagueRat, plagueBat, undeadCarrier,
				Math.max(0.0, Math.min(1.0, transmissionChance)),
				4000 - random.nextInt(200, 601));
		if (entity instanceof Player)
			infection.coughTicks = 20;
		infections.put(entity.getUniqueId(), infection);
		owner.ownedInfections.add(entity.getUniqueId());
		entity.setMetadata(METADATA_KEY, plugin.getFixedMetadata());
		if (!(entity instanceof Player))
			entity.setRemoveWhenFarAway(false);
		if (entity instanceof Player player)
			player.sendMessage(Utils.convertString(DataUtils.getLanguageString("messages.disaster_broadcasts.plague.plague_catch").replace("%disaster%", owner.getDisplayName())));
		return true;
	}
	public static boolean infectEntityFromPotion(LivingEntity entity) {
		if (entity == null || entity.getWorld() == null)
			return false;
		BlackPlague owner = activeDisasters.stream()
				.filter(plague -> plague.location != null && plague.location.getWorld().equals(entity.getWorld()))
				.min(Comparator.comparingDouble(plague -> plague.location.distanceSquared(entity.getLocation())))
				.orElse(null);
		if (owner == null) {
			UUID worldId = entity.getWorld().getUID();
			owner = potionOwners.get(worldId);
			if (owner == null) {
				owner = new BlackPlague(entity.getLocation(), null, 1);
				owner.init();
				potionOwners.put(worldId, owner);
				plugin.getServer().getPluginManager().registerEvents(owner, plugin);
			}
		}
		ensureInfectionTask();
		return infectEntity(entity, owner, false, false, false, true, 0.5);
	}
	public static boolean isInfected(LivingEntity entity) {
		return entity != null && infections.containsKey(entity.getUniqueId());
	}
	public static boolean restorePersistentInfection(Entity entity) {
		if (!(entity instanceof LivingEntity living) || living.isDead() || !living.isValid() || infections.containsKey(living.getUniqueId()))
			return false;
		PersistentDataContainer container = living.getPersistentDataContainer();
		Integer elapsedTicks = container.get(PLAGUE_PROGRESS_KEY, PersistentDataType.INTEGER);
		int[] state = container.get(PLAGUE_STATE_KEY, PersistentDataType.INTEGER_ARRAY);
		if (elapsedTicks == null || state == null)
			return false;
		if (state.length < 10 || state[0] != PERSISTENCE_VERSION) {
			clearPersistentInfection(living);
			container.remove(PLAGUE_HOSTILE_KEY);
			return false;
		}
		int level = Math.max(1, state[1]);
		BlackPlague owner = getRestoredOwner(living, level);
		if (owner.isEntityTypeBlacklisted(living) || living instanceof Player player && EntityUtils.isPlayerImmune(player)) {
			clearPersistentInfection(living);
			container.remove(PLAGUE_HOSTILE_KEY);
			cleanupInactiveOwner(owner);
			return false;
		}
		int flags = state[4];
		int maxTicks = Math.max(20, state[2]);
		double transmissionChance = container.getOrDefault(PLAGUE_TRANSMISSION_KEY, PersistentDataType.DOUBLE, owner.infectionTransmissionChance);
		Infection infection = new Infection(owner, maxTicks, (flags & FLAG_CARRIER) != 0, (flags & FLAG_PLAGUE_RAT) != 0,
				(flags & FLAG_PLAGUE_BAT) != 0, (flags & FLAG_UNDEAD) != 0, Math.max(0.0, Math.min(1.0, transmissionChance)),
				Math.max(0, state[3]));
		infection.ticksRemaining = Math.max(0, maxTicks - Math.max(0, elapsedTicks));
		infection.phase = Math.max(0, state[5]);
		infection.coughTicks = Math.max(1, state[6]);
		infection.aggressionSoundTicks = Math.max(0, state[8]);
		infection.proximitySpreadTicks = Math.max(1, state[9]);
		if (state[7] >= 0 && state[7] < InfectionBehavior.values().length)
			infection.behavior = InfectionBehavior.values()[state[7]];
		infections.put(living.getUniqueId(), infection);
		owner.ownedInfections.add(living.getUniqueId());
		living.setMetadata(METADATA_KEY, plugin.getFixedMetadata());
		if (!(living instanceof Player))
			living.setRemoveWhenFarAway(false);
		EntitiesListener.detachRemoveKey(living);
		// modifying a mob's AI in the same tick it is loaded leaves it frozen in place until something hits it, so the
		// hold pass keeps the 20-tick infection task (which can land on the load tick) from attaching goals via
		// updateAggression; the actual goal application happens through restoreHostileGoal 20 ticks after the load
		infection.aggressionHoldPasses = 1;
		if (living instanceof Mob && container.has(PLAGUE_HOSTILE_KEY, PersistentDataType.BYTE))
			infection.behavior = InfectionBehavior.HOSTILE;
		clearPersistentInfection(living);
		ensureInfectionTask();
		return true;
	}
	/**
	 * Applies the rabid aggression goal to a loading mob carrying the persisted hostility byte. Called for every loaded
	 * entity alongside {@link #restorePersistentInfection} - runtime pathfinder goals never persist with the entity,
	 * and touching a mob's AI in its load tick freezes it, so the goal is attached through the UltimateContent bridge
	 * 20 ticks after the load. Works for both rabid animals and rabid villagers.
	 */
	public static void restoreHostileGoal(Entity entity) {
		if (!(entity instanceof Mob mob) || !mob.getPersistentDataContainer().has(PLAGUE_HOSTILE_KEY, PersistentDataType.BYTE)
				|| !DependencyUtils.isUltimateContentEnabled())
			return;
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			if (!mob.isValid() || mob.isDead() || !mob.getPersistentDataContainer().has(PLAGUE_HOSTILE_KEY, PersistentDataType.BYTE))
				return;
			double damage = 0.0;
			Infection infection = infections.get(mob.getUniqueId());
			if (infection != null) {
				if (infection.bedridden || infection.aggressive)
					return;
				infection.behavior = InfectionBehavior.HOSTILE;
				infection.aggressive = true;
				infection.aggressionRefreshTicks = 15;
				infection.aggressionSoundTicks = 3;
				damage = infection.owner.rabidDamage;
			}
			UltimateContentUtils.manipulatePlagueAggressionGoals(mob, true, damage, 0.0, true);
		}, 20L);
	}
	public static void persistAndUnloadEntity(Entity entity) {
		if (!(entity instanceof LivingEntity living))
			return;
		Infection infection = infections.remove(living.getUniqueId());
		if (infection == null)
			return;
		writePersistentInfection(living, infection);
		infection.owner.ownedInfections.remove(living.getUniqueId());
		if (living.hasMetadata(METADATA_KEY))
			living.removeMetadata(METADATA_KEY, plugin);
		cleanupRecoveredState(living, infection);
		cleanupInactiveOwner(infection.owner);
	}
	public static void saveAllInfections() {
		for (UUID uuid : new ArrayList<>(infections.keySet())) {
			Entity entity = Bukkit.getEntity(uuid);
			if (entity instanceof LivingEntity)
				persistAndUnloadEntity(entity);
			else {
				Infection infection = infections.remove(uuid);
				if (infection != null)
					infection.owner.ownedInfections.remove(uuid);
			}
		}
		if (infectionTaskId != -1)
			Bukkit.getScheduler().cancelTask(infectionTaskId);
		infectionTaskId = -1;
		infections.clear();
		activeDisasters.clear();
		potionOwners.clear();
		restoredOwners.clear();
		if (DependencyUtils.isUltimateContentEnabled())
			UltimateContentUtils.clearAllPlagueAggressionGoals();
	}
	private static void writePersistentInfection(LivingEntity entity, Infection infection) {
		int flags = (infection.carrier ? FLAG_CARRIER : 0)
				| (infection.plagueRat ? FLAG_PLAGUE_RAT : 0)
				| (infection.plagueBat ? FLAG_PLAGUE_BAT : 0)
				| (infection.undeadCarrier ? FLAG_UNDEAD : 0);
		int elapsedTicks = Math.max(0, infection.maxTicks - infection.ticksRemaining);
		int[] state = {PERSISTENCE_VERSION, infection.owner.getLevel(), infection.maxTicks, infection.terminalCollapseTicks, flags,
				infection.phase, infection.coughTicks, infection.behavior.ordinal(), infection.aggressionSoundTicks, infection.proximitySpreadTicks};
		PersistentDataContainer container = entity.getPersistentDataContainer();
		container.set(PLAGUE_PROGRESS_KEY, PersistentDataType.INTEGER, elapsedTicks);
		container.set(PLAGUE_STATE_KEY, PersistentDataType.INTEGER_ARRAY, state);
		container.set(PLAGUE_TRANSMISSION_KEY, PersistentDataType.DOUBLE, infection.transmissionChance);
		if (!infection.plagueRat && !infection.plagueBat)
			EntitiesListener.detachRemoveKey(entity);
	}
	private static void clearPersistentInfection(LivingEntity entity) {
		PersistentDataContainer container = entity.getPersistentDataContainer();
		container.remove(PLAGUE_PROGRESS_KEY);
		container.remove(PLAGUE_STATE_KEY);
		container.remove(PLAGUE_TRANSMISSION_KEY);
	}
	private static BlackPlague getRestoredOwner(LivingEntity entity, int level) {
		RestoredOwnerKey key = new RestoredOwnerKey(entity.getWorld().getUID(), level);
		return restoredOwners.computeIfAbsent(key, ignored -> {
			BlackPlague owner = new BlackPlague(entity.getLocation(), null, level);
			owner.init();
			plugin.getServer().getPluginManager().registerEvents(owner, plugin);
			return owner;
		});
	}
	public static void cureEntity(LivingEntity entity) {
		if (entity == null)
			return;
		clearPersistentInfection(entity);
		entity.getPersistentDataContainer().remove(PLAGUE_HOSTILE_KEY);
		Infection infection = infections.remove(entity.getUniqueId());
		if (infection == null)
			return;
		if (infection.owner != null) {
			infection.owner.ownedInfections.remove(entity.getUniqueId());
			cleanupInactiveOwner(infection.owner);
		}
		if (entity.hasMetadata(METADATA_KEY))
			entity.removeMetadata(METADATA_KEY, plugin);
		cleanupRecoveredState(entity, infection);
		if (infection.plagueRat && entity.isValid())
			despawnRat(entity);
		else if (infection.plagueBat && entity.isValid())
			despawnBat(entity);
		if (entity instanceof Player player && infection.owner != null)
			player.sendMessage(Utils.convertString(DataUtils.getLanguageString("messages.disaster_broadcasts.plague.cure_message").replace("%disaster%", infection.owner.getDisplayName())));
	}
	public static int cureAllEntities() {
		int count = 0;
		for (UUID uuid : new ArrayList<>(infections.keySet())) {
			Entity entity = Bukkit.getEntity(uuid);
			if (entity instanceof LivingEntity living) {
				cureEntity(living);
				count++;
			} else {
				Infection infection = infections.get(uuid);
				if (infection != null) {
					removeInfection(uuid, infection);
					count++;
				}
			}
		}
		if (DependencyUtils.isUltimateContentEnabled())
			UltimateContentUtils.clearAllPlagueAggressionGoals();
		return count;
	}
	public static void clearAllInfections() {
		for (Map.Entry<UUID, Infection> entry : new ArrayList<>(infections.entrySet())) {
			Entity entity = Bukkit.getEntity(entry.getKey());
			if (entity instanceof LivingEntity living)
				cureEntity(living);
			else
				removeInfection(entry.getKey(), entry.getValue());
		}
		infections.clear();
		activeDisasters.clear();
		potionOwners.clear();
		restoredOwners.clear();
		if (DependencyUtils.isUltimateContentEnabled())
			UltimateContentUtils.clearAllPlagueAggressionGoals();
	}
	private static void removeInfection(UUID uuid, Infection infection) {
		infections.remove(uuid);
		if (infection.owner != null) {
			infection.owner.ownedInfections.remove(uuid);
			cleanupInactiveOwner(infection.owner);
		}
		Entity entity = Bukkit.getEntity(uuid);
		if (entity instanceof LivingEntity living) {
			clearPersistentInfection(living);
			living.getPersistentDataContainer().remove(PLAGUE_HOSTILE_KEY);
			if (living.hasMetadata(METADATA_KEY))
				living.removeMetadata(METADATA_KEY, plugin);
		}
		if (entity instanceof LivingEntity living)
			cleanupRecoveredState(living, infection);
	}
	private static void cleanupInactiveOwner(BlackPlague owner) {
		if (activeDisasters.contains(owner) || owner.hasOwnedInfections())
			return;
		HandlerList.unregisterAll(owner);
		potionOwners.values().removeIf(plague -> plague == owner);
		restoredOwners.values().removeIf(plague -> plague == owner);
	}
	private boolean hasOwnedInfections() {
		for (UUID uuid : ownedInfections) {
			if (infections.containsKey(uuid))
				return true;
		}
		return false;
	}
	private int getGlobalInfectionCount(org.bukkit.World world) {
		int count = 0;
		for (UUID uuid : infections.keySet()) {
			Entity entity = Bukkit.getEntity(uuid);
			if (entity != null && entity.getWorld().equals(world))
				count++;
		}
		return count;
	}
	private void corpseBurst(Location loc, BlackPlague owner, boolean carrier) {
		loc.getWorld().spawnParticle(VersionUtils.getLargeSmoke(), loc.clone().add(0, 0.6, 0), carrier ? 50 : 25, 1.4, 0.8, 1.4, 0.01);
		if (SCULK_CATALYST_BLOOM != null)
			loc.getWorld().playSound(loc, SCULK_CATALYST_BLOOM, 0.75f, 0.6f);
		for (Entity nearby : loc.getWorld().getNearbyEntities(loc, corpseBurstRadius, corpseBurstRadius, corpseBurstRadius)) {
			if (!(nearby instanceof LivingEntity living) || random.nextDouble() >= corpseBurstChance)
				continue;
			infectEntity(living, owner, carrier && random.nextDouble() < 0.35, false);
		}
	}
	private void tickInfection(UUID uuid, Infection infection, LivingEntity entity) {
		if (infection.owner.isEntityTypeBlacklisted(entity)) {
			cureEntity(entity);
			return;
		}
		if (entity instanceof Player player && player.getGameMode() == GameMode.CREATIVE) {
			cureEntity(player);
			return;
		}
		if (infection.owner.curePlagueInRegions && DependencyUtils.isEntityProtected(entity)) {
			cureEntity(entity);
			return;
		}
		int elapsedTicks = infection.maxTicks - infection.ticksRemaining;
		double stage = Math.min(1.0, Math.max(0.0, (double) elapsedTicks / Math.max(1, infection.owner.infectionTicks)));
		if (!infection.plagueRat && !infection.plagueBat && !infection.undeadCarrier && tryNaturalRecovery(entity, infection, stage))
			return;
		updateBedriddenState(entity, infection, elapsedTicks);
		int dust = infection.plagueRat ? 4 : infection.plagueBat ? 3 : infection.undeadCarrier ? 1 : elapsedTicks < 600 ? 1 : elapsedTicks < 1200 ? 3 : elapsedTicks < 2400 ? 5 : elapsedTicks < 3600 ? 7 : 9;
		if (infection.bedridden)
			spawnBedriddenParticles(entity, infection);
		else {
			Location particleLoc = entity.getLocation().add(0, Math.max(0.25, entity.getHeight() * 0.55), 0);
			entity.getWorld().spawnParticle(VersionUtils.getRedstoneDust(), particleLoc, dust, 0.18 + stage * 0.22, entity.getHeight() * 0.18, 0.18 + stage * 0.22, 0.004, BLACK_DUST);
			if (!infection.undeadCarrier && !infection.plagueRat && !infection.plagueBat && elapsedTicks >= 2400)
				entity.getWorld().spawnParticle(VersionUtils.getNormalSmoke(), particleLoc, Math.max(1, (int) (stage * 3)), 0.22, 0.2, 0.22, 0.001);
		}
		applySymptoms(entity, infection, elapsedTicks);
		if (!entity.isValid() || entity.isDead() || infections.get(uuid) != infection)
			return;
		if (shouldCough(entity, infection) && --infection.coughTicks <= 0) {
			int coughDelay = infection.bedridden ? random.nextInt(4, 9) : elapsedTicks >= 2400 ? random.nextInt(6, 13) : random.nextInt(10, 21);
			infection.coughTicks = entity instanceof Player ? (int) Math.ceil(coughDelay * 2.5) : coughDelay;
			playCough(entity, infection);
		}
		spreadFrom(entity, infection);
		updateAggression(entity, infection, elapsedTicks);
		if ((infection.ticksRemaining -= 20) <= 0) {
			if (infection.plagueRat || infection.plagueBat) {
				if (infection.plagueRat)
					despawnRat(entity);
				else
					despawnBat(entity);
				removeInfection(uuid, infection);
				return;
			}
			if (infection.undeadCarrier) {
				cureEntity(entity);
				return;
			}
			infection.ticksRemaining = 0;
		}
	}
	private boolean tryNaturalRecovery(LivingEntity entity, Infection infection, double stage) {
		if (entity instanceof Player)
			return false;
		int phase = Math.min(4, Math.max(0, (int) Math.floor(stage * 4.0)));
		if (phase <= infection.phase)
			return false;
		infection.phase = phase;
		if (random.nextDouble() >= infection.owner.naturalRecoveryChance)
			return false;
		entity.getWorld().spawnParticle(VersionUtils.getNormalSmoke(), entity.getLocation().add(0, Math.max(0.4, entity.getHeight() * 0.6), 0), 8, 0.3, 0.25, 0.3, 0.001);
		entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_VILLAGER_YES, 0.35f, 0.65f);
		cureEntity(entity);
		return true;
	}
	private boolean shouldCough(LivingEntity entity, Infection infection) {
		if (infection.plagueRat || infection.plagueBat || infection.undeadCarrier)
			return false;
		if (entity instanceof Player)
			return infection.owner.playerCoughingSounds;
		if (entity instanceof Villager || entity instanceof WanderingTrader)
			return infection.owner.villagerCoughingSounds;
		return entity instanceof Animals && infection.owner.animalCoughingSounds;
	}
	private void playCough(LivingEntity entity, Infection infection) {
		float volume = infection.bedridden ? 0.95f : entity instanceof Player ? 0.85f : 0.75f;
		if (entity instanceof Player || entity instanceof Villager || entity instanceof WanderingTrader) {
			if (random.nextBoolean())
				entity.getWorld().playSound(entity.getLocation(), SNIFFER_SNIFFING != null ? SNIFFER_SNIFFING : Sound.ENTITY_FOX_SNIFF, volume, 0.5f);
			else
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PANDA_SNEEZE, volume, infection.bedridden ? 0.45f : 0.62f);
			return;
		}
		if (entity instanceof Animals) {
			switch (random.nextInt(3)) {
			case 0 -> entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_FOX_SPIT, volume, (float) random.nextDouble(0.5, 0.6));
			case 1 -> entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_LLAMA_SPIT, volume, 0.5f);
			default -> entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_HORSE_BREATHE, volume, 0.5f);
			}
			return;
		}
		entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PANDA_SNEEZE, volume, 0.62f);
	}
	private void updateBedriddenState(LivingEntity entity, Infection infection, int elapsedTicks) {
		if (!(entity instanceof Villager || entity instanceof WanderingTrader) || infection.plagueRat || infection.plagueBat || infection.undeadCarrier)
			return;
		Mob villagerType = (Mob) entity;
		boolean earlyCollapse = infection.behavior == InfectionBehavior.COLLAPSED && elapsedTicks >= 2400;
		if (!infection.bedridden && (earlyCollapse || elapsedTicks >= infection.terminalCollapseTicks)) {
			infection.bedridden = true;
			infection.previousAI = villagerType.hasAI();
			infection.bedriddenBodyYaw = DependencyUtils.isUltimateContentEnabled()
					? UltimateContentUtils.getBodyRotation(villagerType) : entity.getLocation().getYaw();
			villagerType.setAI(false);
			if (infection.aggressive && DependencyUtils.isUltimateContentEnabled())
				UltimateContentUtils.clearPlagueAggressionGoals(villagerType);
			infection.aggressive = false;
			// a bedridden villager never becomes rabid again, so drop the persisted hostility marker
			villagerType.getPersistentDataContainer().remove(PLAGUE_HOSTILE_KEY);
			if (entity instanceof Villager villager)
				try {
					villager.sleep(villager.getLocation());
				} catch (IllegalStateException ignored) {}
		}
		if (infection.bedridden)
			enforceBedridden(entity, villagerType, infection);
	}
	private void enforceBedridden(LivingEntity entity, Mob villagerType, Infection infection) {
		villagerType.setTarget(null);
		villagerType.setAI(false);
		if (DependencyUtils.isUltimateContentEnabled())
			UltimateContentUtils.setBodyRotation(villagerType, infection.bedriddenBodyYaw);
		Vector velocity = entity.getVelocity();
		entity.setVelocity(new Vector(0.0, Math.min(0.0, velocity.getY()), 0.0));
		forceSleepingPose(entity);
	}
	private static void restoreBedridden(LivingEntity entity, Infection infection) {
		if (!infection.bedridden || !(entity instanceof Mob) || !entity.isValid() || entity.isDead())
			return;
		Mob villagerType = (Mob) entity;
		forceVillagerAwake(entity);
		villagerType.setAI(infection.previousAI);
		entity.setVelocity(new Vector(0.0, 0.18, 0.0));
		entity.getWorld().spawnParticle(VersionUtils.getNormalSmoke(), entity.getLocation().add(0, 0.65, 0), 12, 0.35, 0.25, 0.35, 0.002);
		entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_VILLAGER_YES, 0.45f, 0.9f);
		infection.bedridden = false;
		Bukkit.getScheduler().runTask(plugin, () -> {
			if (entity.isValid() && !entity.isDead() && !infection.bedridden)
				forceVillagerAwake(entity);
		});
	}
	private static void cleanupRecoveredState(LivingEntity entity, Infection infection) {
		restoreBedridden(entity, infection);
		if (entity instanceof Mob mob && DependencyUtils.isUltimateContentEnabled()) {
			UltimateContentUtils.clearPlagueAggressionGoals(mob);
			infection.aggressive = false;
			mob.setTarget(null);
		}
	}
	private void spawnBedriddenParticles(LivingEntity entity, Infection infection) {
		Location orientation = entity.getLocation();
		orientation.setYaw(infection.bedriddenBodyYaw);
		orientation.setPitch(0.0f);
		Vector facing = orientation.getDirection().setY(0);
		if (facing.lengthSquared() < 0.001)
			facing = new Vector(1, 0, 0);
		else
			facing.normalize();
		Vector bodyAxis = new Vector(facing.getZ(), 0, -facing.getX());
		Location origin = entity.getLocation().add(0, 0.18, 0);
		Location bodyCenter = origin.clone().add(bodyAxis.multiply(0.7));
		entity.getWorld().spawnParticle(VersionUtils.getRedstoneDust(), bodyCenter, 16, 0.9, 0.14, 0.9, 0.002, BLACK_DUST);
		entity.getWorld().spawnParticle(VersionUtils.getRedstoneDust(), origin, 5, 0.38, 0.12, 0.38, 0.002, BLACK_DUST);
	}
	private void applySymptoms(LivingEntity entity, Infection infection, int elapsedTicks) {
		if (infection.plagueRat || infection.plagueBat || infection.undeadCarrier)
			return;
		if (elapsedTicks >= 600) {
			entity.addPotionEffect(new PotionEffect(VersionUtils.getSlowness(), 60, elapsedTicks >= 1200 ? 1 : 0, true, false));
			entity.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, elapsedTicks >= 1200 ? 1 : 0, true, false));
		}
		if (entity instanceof Player && elapsedTicks >= 1200)
			entity.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 60, 0, true, false));
		if (elapsedTicks >= 2400)
			entity.addPotionEffect(new PotionEffect(VersionUtils.getSlowDigging(), 60, 1, true, false));
		if (elapsedTicks >= 3600)
			entity.addPotionEffect(new PotionEffect(VersionUtils.getConfusion(), 100, 0, true, false));
		if (elapsedTicks >= 4000) {
			entity.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 39, 0, true, false));
			if (random.nextInt(3) == 0)
				EntityUtils.pureDamageEntity(entity, 2.5, entity instanceof Player ? DEATH_KEY : null,
						EntityDamageEvent.DamageCause.WITHER, null, true, false);
		}
	}
	private void spreadFrom(LivingEntity entity, Infection infection) {
		if ((infection.proximitySpreadTicks -= 20) > 0)
			return;
		infection.proximitySpreadTicks = infection.owner.proximitySpreadIntervalTicks;
		BoundingBox infectiousBox = entity.getBoundingBox().expand(infection.owner.proximitySpreadMargin);
		double scanX = Math.max(2.0, infectiousBox.getWidthX() + 2.0);
		double scanY = Math.max(2.0, infectiousBox.getHeight() + 2.0);
		double scanZ = Math.max(2.0, infectiousBox.getWidthZ() + 2.0);
		for (Entity nearby : entity.getNearbyEntities(scanX, scanY, scanZ)) {
			if (!(nearby instanceof LivingEntity living) || living.equals(entity) || infections.containsKey(living.getUniqueId()))
				continue;
			if (infectiousBox.overlaps(living.getBoundingBox()) && entity.hasLineOfSight(living) && random.nextDouble() < infection.transmissionChance)
				infectEntity(living, infection.owner, !DependencyUtils.isUltimateContentEnabled() && infection.carrier && random.nextDouble() < 0.2, false,
						false, false, infection.transmissionChance);
		}
	}
	private void updateAggression(LivingEntity entity, Infection infection, int elapsedTicks) {
		if (!(entity instanceof Mob mob) || !(entity instanceof Animals || entity instanceof Villager || entity instanceof WanderingTrader))
			return;
		if (infection.plagueRat || infection.plagueBat || infection.undeadCarrier)
			return;
		if (infection.bedridden)
			return;
		if (infection.aggressionHoldPasses > 0) {
			// just restored from a load - never touch the mob's AI on the pass that may share the load tick
			infection.aggressionHoldPasses--;
			return;
		}
		boolean animalRabid = entity instanceof Animals && elapsedTicks >= 1200;
		boolean villagerRabid = (entity instanceof Villager || entity instanceof WanderingTrader) && elapsedTicks >= 1800;
		if ((animalRabid || villagerRabid) && infection.behavior == InfectionBehavior.UNDECIDED) {
			if (animalRabid)
				infection.behavior = random.nextBoolean() ? InfectionBehavior.HOSTILE : InfectionBehavior.NORMAL;
			else {
				int outcome = random.nextInt(3);
				infection.behavior = outcome == 0 ? InfectionBehavior.HOSTILE : outcome == 1 ? InfectionBehavior.COLLAPSED : InfectionBehavior.NORMAL;
			}
		}
		if (infection.behavior == InfectionBehavior.HOSTILE && DependencyUtils.isUltimateContentEnabled()
				&& (!infection.aggressive || --infection.aggressionRefreshTicks <= 0)) {
			boolean firstApplication = !infection.aggressive;
			infection.aggressive = true;
			infection.aggressionRefreshTicks = 15;
			// runtime pathfinder goals never persist with the entity, so re-assert the overlay periodically - this
			// re-attaches it after the mob's chunk reloads or the server restarts (UltimateContent rebuilds the goals
			// when the entity handle changed) and retries if a previous attach attempt failed
			UltimateContentUtils.manipulatePlagueAggressionGoals(mob, true, rabidDamage, 0.0, true);
			if (firstApplication) {
				// persisted independently of the packed infection state so the rabid goal can be re-applied on load;
				// removed only when the mob is cured (or a villager goes bedridden)
				mob.getPersistentDataContainer().set(PLAGUE_HOSTILE_KEY, PersistentDataType.BYTE, (byte) 1);
				infection.aggressionSoundTicks = random.nextInt(2, 5);
				return;
			}
		}
		if (infection.aggressive && mob.getTarget() != null && mob.getTarget().isValid() && !mob.getTarget().isDead()
				&& --infection.aggressionSoundTicks <= 0) {
			infection.aggressionSoundTicks = random.nextInt(4, 9);
			if (entity instanceof Animals)
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_FOX_AGGRO, 0.8f, (float) random.nextDouble(0.5, 0.6));
			else
				entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, (float) random.nextDouble(0.7, 0.8));
		}
		if (!infection.aggressive && infection.behavior != InfectionBehavior.NORMAL && elapsedTicks >= 900 && random.nextInt(6) == 0) {
			Location loc = entity.getLocation();
			entity.setVelocity(Utils.getVectorTowards(loc, loc.clone().add(random.nextDouble(-4.0, 4.0), random.nextDouble(0.2, 1.0), random.nextDouble(-4.0, 4.0))).multiply(0.35));
		}
	}
	private void spawnRatSwarm(Location anchor, BlackPlague owner, int amount, RatTargetMode targetMode) {
		if (amount <= 0)
			return;
		int spawned = 0;
		for (int i = 0; i < amount * 12 && spawned < amount && getGlobalInfectionCount(anchor.getWorld()) < maxInfectedMobs; i++) {
			Location spawn = findRatSpawnLocation(anchor);
			double focusChance = targetMode == RatTargetMode.PLAYER_SWARM ? owner.ratDirectSwarmFraction : owner.ratPlayerTargetChance;
			boolean focusedTarget = random.nextDouble() < focusChance && isValidRatTargetPlayer(owner.player, true);
			LivingEntity target = focusedTarget ? owner.player : findRatSpawnTarget(anchor, false);
			if (target == null)
				target = findRatSpawnTarget(anchor, true);
			if (spawn != null && spawnPlagueRat(spawn, owner, target, focusedTarget))
				spawned++;
		}
	}
	private LivingEntity findRatSpawnTarget(Location anchor, boolean includeTargetPlayer) {
		LivingEntity closest = null;
		double closestDistance = Double.MAX_VALUE;
		for (Entity entity : anchor.getWorld().getNearbyEntities(anchor, Math.max(20.0, ratSpawnMaxPlayerDistance + 6.0), verticalPreference + 16.0, Math.max(20.0, ratSpawnMaxPlayerDistance + 6.0))) {
			if (!(entity instanceof LivingEntity living) || !isValidRatTarget(living, includeTargetPlayer))
				continue;
			double distance = living.getLocation().distanceSquared(anchor);
			if (distance < closestDistance) {
				closest = living;
				closestDistance = distance;
			}
		}
		return closest;
	}
	private boolean isValidRatTarget(LivingEntity entity, boolean includeTargetPlayer) {
		if (entity == null || entity.isDead() || !entity.isValid() || entity.isInvulnerable() || entity.getType() == EntityType.SILVERFISH || entity.getType() == EntityType.BAT || isEntityProtected(entity))
			return false;
		if (entity instanceof Player player)
			return isValidRatTargetPlayer(player, includeTargetPlayer);
		return true;
	}
	private boolean isValidRatTargetPlayer(Player target, boolean includeTargetPlayer) {
		if (target == null || !target.isOnline() || target.isDead() || !target.isValid() || EntityUtils.isPlayerImmune(target))
			return false;
		return includeTargetPlayer || player == null || !target.getUniqueId().equals(player.getUniqueId());
	}
	private Location findRatSpawnLocation(Location anchor) {
		if (isUnderground(anchor)) {
			Location underground = findUndergroundRatSpawnLocation(anchor);
			if (underground != null)
				return underground;
		}
		for (int i = 0; i < 24; i++) {
			double angle = random.nextDouble(0.0, Math.PI * 2.0);
			double distance = random.nextDouble(ratSpawnMinPlayerDistance, ratSpawnMaxPlayerDistance);
			Location sample = anchor.clone().add(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
			Block ground = sample.getWorld().getHighestBlockAt(sample);
			Location spawn = ground.getRelative(BlockFace.UP).getLocation().add(0.5, 0.05, 0.5);
			if (Math.abs(spawn.getY() - anchor.getY()) > Math.max(verticalPreference + 10.0, 24.0) || !isSafeRatSpawn(spawn, ground, ratSpawnMinPlayerDistance, false))
				continue;
			return spawn;
		}
		return null;
	}
	private void spawnBatGroup(Location anchor, BlackPlague owner, int amount) {
		if (amount <= 0)
			return;
		int spawned = 0;
		for (int i = 0; i < amount * 14 && spawned < amount && getGlobalInfectionCount(anchor.getWorld()) < maxInfectedMobs; i++) {
			Location spawn = findBatSpawnLocation(anchor);
			boolean focusedTarget = random.nextDouble() < owner.batPlayerTargetChance && isValidRatTargetPlayer(owner.player, true);
			LivingEntity target = focusedTarget ? owner.player : findRatSpawnTarget(anchor, false);
			if (target == null)
				target = isValidRatTargetPlayer(owner.player, true) ? owner.player : findRatSpawnTarget(anchor, true);
			if (spawn != null && spawnPlagueBat(spawn, owner, target, focusedTarget))
				spawned++;
		}
	}
	private Location findBatSpawnLocation(Location anchor) {
		if (isUnderground(anchor)) {
			for (int i = 0; i < 160; i++) {
				double angle = random.nextDouble(0.0, Math.PI * 2.0);
				double distance = random.nextDouble(batSpawnMinPlayerDistance, batSpawnMaxPlayerDistance);
				Location candidate = anchor.clone().add(Math.cos(angle) * distance, random.nextDouble(-10.0, 11.0), Math.sin(angle) * distance);
				candidate = candidate.getBlock().getLocation().add(0.5, 0.5, 0.5);
				if (isSafeBatSpawn(candidate))
					return candidate;
			}
			return null;
		}
		for (int i = 0; i < 28; i++) {
			double angle = random.nextDouble(0.0, Math.PI * 2.0);
			double distance = random.nextDouble(batSpawnMinPlayerDistance, batSpawnMaxPlayerDistance);
			Location column = anchor.clone().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
			Block ground = column.getWorld().getHighestBlockAt(column);
			Location spawn = ground.getLocation().add(0.5, random.nextDouble(7.0, 14.0), 0.5);
			if (Math.abs(spawn.getY() - anchor.getY()) <= 30.0 && isSafeBatSpawn(spawn))
				return spawn;
		}
		return null;
	}
	private boolean isSafeBatSpawn(Location spawn) {
		return !DependencyUtils.isRegionProtected(spawn) && isSafePlayerDistance(spawn, batSpawnMinPlayerDistance) && isOpenBatVolume(spawn);
	}
	private boolean isOpenBatVolume(Location location) {
		if (location.getY() <= location.getWorld().getMinHeight() + 1 || location.getY() >= location.getWorld().getMaxHeight() - 1)
			return false;
		Block center = location.getBlock();
		return center.isPassable() && isDryBlock(center);
	}
	private Location findUndergroundRatSpawnLocation(Location anchor) {
		for (int i = 0; i < 60; i++) {
			double angle = random.nextDouble(0.0, Math.PI * 2.0);
			double distance = random.nextDouble(ratUndergroundMinPlayerDistance, ratUndergroundMaxPlayerDistance);
			int baseY = anchor.getBlockY() + random.nextInt(-4, 5);
			Location sample = anchor.clone().add(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
			for (int yOffset = 0; yOffset <= 5; yOffset++) {
				int y = baseY + (yOffset % 2 == 0 ? yOffset / 2 : -(yOffset / 2 + 1));
				if (y <= sample.getWorld().getMinHeight() || y >= sample.getWorld().getMaxHeight() - 2)
					continue;
				Block feet = sample.getWorld().getBlockAt(sample.getBlockX(), y, sample.getBlockZ());
				Location spawn = feet.getLocation().add(0.5, 0.05, 0.5);
				if (isSafeRatSpawn(spawn, feet.getRelative(BlockFace.DOWN), ratUndergroundMinPlayerDistance, true))
					return spawn;
			}
		}
		return null;
	}
	private boolean isSafeRatSpawn(Location spawn, Block ground, double minPlayerDistance, boolean requireWall) {
		if (DependencyUtils.isRegionProtected(spawn) || !isSafePlayerDistance(spawn, minPlayerDistance))
			return false;
		Block feet = spawn.getBlock();
		Block head = feet.getRelative(BlockFace.UP);
		return ground.getType().isSolid() && isDryBlock(ground) && isDryBlock(feet) && isDryBlock(head) && isDryBlock(ground.getRelative(BlockFace.DOWN)) && feet.isPassable() && head.isPassable()
				&& isDryRatArea(feet)
				&& (!requireWall || hasSolidWall(feet) && hasRatWalkout(feet));
	}
	private boolean hasSolidWall(Block feet) {
		for (BlockFace face : HORIZONTAL_FACES) {
			if (feet.getRelative(face).getType().isSolid() || feet.getRelative(BlockFace.UP).getRelative(face).getType().isSolid())
				return true;
		}
		return false;
	}
	private boolean hasRatWalkout(Block feet) {
		for (BlockFace face : HORIZONTAL_FACES) {
			Block next = feet.getRelative(face);
			Block nextHead = next.getRelative(BlockFace.UP);
			Block nextGround = next.getRelative(BlockFace.DOWN);
			if (nextGround.getType().isSolid() && isDryBlock(nextGround) && next.isPassable() && nextHead.isPassable() && isDryBlock(next) && isDryBlock(nextHead))
				return true;
		}
		return false;
	}
	private boolean isDryRatArea(Block feet) {
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					if (!isDryBlock(feet.getRelative(x, y, z)))
						return false;
				}
			}
		}
		return true;
	}
	private boolean isDryBlock(Block block) {
		return !block.isLiquid() && !(block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged());
	}
	private boolean isUnderground(Location anchor) {
		Block block = anchor.getBlock();
		int surfaceY = anchor.getWorld().getHighestBlockYAt(anchor);
		return surfaceY - anchor.getBlockY() >= 8 && block.getLightFromSky() <= 3;
	}
	private boolean isSafePlayerDistance(Location loc, double minDistance) {
		if (minDistance <= 0)
			return true;
		double minDistanceSquared = minDistance * minDistance;
		for (Player player : loc.getWorld().getPlayers()) {
			if (!EntityUtils.isPlayerImmune(player) && player.getLocation().distanceSquared(loc) < minDistanceSquared)
				return false;
		}
		return true;
	}
	private boolean spawnPlagueRat(Location spawn, BlackPlague owner, LivingEntity target, boolean focusedTarget) {
		Entity entity = UltimateContentUtils.spawnPlagueRat(spawn);
		if (entity instanceof LivingEntity living && infectEntity(living, owner, true, true)) {
			if (target != null && target.isValid() && !target.isDead() && target.getWorld().equals(entity.getWorld()) && entity instanceof Mob mob) {
				mob.setTarget(target);
				UltimateContentUtils.configurePlagueRat(entity, target, focusedTarget);
			}
			addEntityToDisasterList(entity);
			return true;
		}
		if (entity != null)
			entity.remove();
		return false;
	}
	private boolean spawnPlagueBat(Location spawn, BlackPlague owner, LivingEntity target, boolean focusedTarget) {
		Entity entity = UltimateContentUtils.spawnPlagueBat(spawn);
		if (entity instanceof LivingEntity living && infectEntity(living, owner, true, false, true)) {
			if (target != null && target.isValid() && !target.isDead() && target.getWorld().equals(entity.getWorld()))
				UltimateContentUtils.configurePlagueBat(entity, target, focusedTarget);
			addEntityToDisasterList(entity);
			return true;
		}
		if (entity != null)
			entity.remove();
		return false;
	}
	private static void despawnRat(Entity entity) {
		if (entity == null || !entity.isValid())
			return;
		if (DependencyUtils.isUltimateContentEnabled() && UltimateContentUtils.beginPlagueRatBurrow(entity))
			return;
		entity.getWorld().spawnParticle(VersionUtils.getNormalSmoke(), entity.getLocation().add(0, 0.25, 0), 12, 0.25, 0.14, 0.25, 0.001);
		entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_RABBIT_AMBIENT, 0.3f, 0.55f);
		entity.remove();
	}
	private static void despawnBat(Entity entity) {
		if (entity == null || !entity.isValid())
			return;
		if (DependencyUtils.isUltimateContentEnabled() && UltimateContentUtils.beginPlagueBatDeparture(entity))
			return;
		entity.getWorld().spawnParticle(VersionUtils.getNormalSmoke(), entity.getLocation().add(0, 0.3, 0), 14, 0.3, 0.2, 0.3, 0.001);
		entity.remove();
	}
	private static void ensureInfectionTask() {
		if (infectionTaskId != -1)
			return;
		infectionTaskId = new BukkitRunnable() {
			@Override
			public void run() {
				if (infections.isEmpty() && activeDisasters.isEmpty()) {
					infectionTaskId = -1;
					cancel();
					return;
				}
				for (Map.Entry<UUID, Infection> entry : new ArrayList<>(infections.entrySet())) {
					Entity entity = Bukkit.getEntity(entry.getKey());
					if (!(entity instanceof LivingEntity living) || living.isDead() || !living.isValid()) {
						removeInfection(entry.getKey(), entry.getValue());
						continue;
					}
					entry.getValue().owner.tickInfection(entry.getKey(), entry.getValue(), living);
				}
			}
		}.runTaskTimer(plugin, 20, 20).getTaskId();
	}
	private static double clampChance(double chance) {
		return Math.max(0.0, Math.min(1.0, chance / 100.0));
	}
	private static void forceSleepingPose(LivingEntity entity) {
		forcePose(entity, "SLEEPING");
	}
	private static void forceVillagerAwake(LivingEntity entity) {
		if (entity instanceof Villager villager)
			try {
				villager.wakeup();
			} catch (IllegalStateException ignored) {}
		try {
			Object handle = entity.getClass().getMethod("getHandle").invoke(entity);
			Method stopSleeping = findMethod(handle.getClass(), "stopSleeping");
			if (stopSleeping != null)
				stopSleeping.invoke(handle);
		} catch (Throwable ignored) {}
		forcePose(entity, "STANDING");
	}
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void forcePose(LivingEntity entity, String poseName) {
		try {
			Object handle = entity.getClass().getMethod("getHandle").invoke(entity);
			Class<?> poseClass = Class.forName("net.minecraft.world.entity.Pose");
			Object pose = Enum.valueOf((Class<? extends Enum>) poseClass.asSubclass(Enum.class), poseName);
			Method setPose = findMethod(handle.getClass(), "setPose", poseClass);
			if (setPose != null)
				setPose.invoke(handle, pose);
		} catch (Throwable ignored) {}
	}
	private static Method findMethod(Class<?> type, String name, Class<?>... parameters) {
		Class<?> current = type;
		while (current != null) {
			try {
				Method method = current.getDeclaredMethod(name, parameters);
				method.setAccessible(true);
				return method;
			} catch (NoSuchMethodException ignored) {
				current = current.getSuperclass();
			}
		}
		return null;
	}
	private static boolean isUndeadCarrier(LivingEntity entity) {
		return switch (entity.getType()) {
		case DROWNED, HUSK, PHANTOM, SKELETON, SKELETON_HORSE, STRAY, WITHER, WITHER_SKELETON, ZOGLIN, ZOMBIE, ZOMBIE_HORSE, ZOMBIE_VILLAGER, ZOMBIFIED_PIGLIN -> true;
		default -> false;
		};
	}
	private enum RatTargetMode {
		AREA_TARGETS,
		PLAYER_SWARM
	}
	private enum InfectionBehavior {
		UNDECIDED,
		HOSTILE,
		COLLAPSED,
		NORMAL
	}
	private record RestoredOwnerKey(UUID worldId, int level) {
	}

	private static final class Infection {
		private final BlackPlague owner;
		private final int maxTicks;
		private final boolean carrier;
		private final boolean plagueRat;
		private final boolean plagueBat;
		private final boolean undeadCarrier;
		private final double transmissionChance;
		private final int terminalCollapseTicks;
		private int ticksRemaining;
		private int proximitySpreadTicks;
		private int phase;
		private int coughTicks = 8;
		private int aggressionSoundTicks;
		private boolean aggressive;
		private int aggressionRefreshTicks;
		private int aggressionHoldPasses;
		private InfectionBehavior behavior = InfectionBehavior.UNDECIDED;
		private boolean bedridden;
		private float bedriddenBodyYaw;
		private boolean previousAI = true;

		private Infection(BlackPlague owner, int maxTicks, boolean carrier, boolean plagueRat, boolean plagueBat, boolean undeadCarrier,
				double transmissionChance, int terminalCollapseTicks) {
			this.owner = owner;
			this.maxTicks = maxTicks;
			this.ticksRemaining = maxTicks;
			this.carrier = carrier;
			this.plagueRat = plagueRat;
			this.plagueBat = plagueBat;
			this.undeadCarrier = undeadCarrier;
			this.transmissionChance = transmissionChance;
			this.terminalCollapseTicks = terminalCollapseTicks;
			this.proximitySpreadTicks = owner.proximitySpreadIntervalTicks;
		}
	}
}
