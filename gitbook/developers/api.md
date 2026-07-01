# API

DeadlyDisasters exposes a Java API for starting disasters, listening for disaster lifecycle events, and registering your own disaster classes. This page covers the current API used by the plugin.

Custom items, enchantments, and custom mobs are now handled by UIFramework and UltimateContent. DeadlyDisasters still hooks into those plugins when they are installed, but new item or mob development should use those APIs instead.

## Adding DeadlyDisasters to Your Plugin

Add the DeadlyDisasters jar to your plugin's compile classpath. If you use Maven or Gradle, install the jar locally or use the repository/version you publish DeadlyDisasters under. If you publish through JitPack, the dependency normally follows the GitHub repository and tag.

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.JewishBanana</groupId>
        <artifactId>DeadlyDisasters</artifactId>
        <version>VERSION</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

After adding it to your build, add DeadlyDisasters to your `plugin.yml` as either a required dependency or a soft dependency.

```yaml
# Use depend if your plugin cannot run without DeadlyDisasters.
depend: [DeadlyDisasters]

# Use softdepend if your plugin can run without DeadlyDisasters.
softdepend: [DeadlyDisasters]
```

If you use `softdepend`, always check that the plugin is installed before calling DeadlyDisasters classes.

```java
if (Bukkit.getPluginManager().getPlugin("DeadlyDisasters") == null) {
    getLogger().info("DeadlyDisasters was not found. Disaster integration disabled.");
    return;
}
```

DeadlyDisasters currently targets Java 17 and Bukkit API 1.17 or newer.

## Main API Classes

Most integrations use these classes:

| Class | Purpose |
| --- | --- |
| `Disaster` | Base class for all disasters. Handles lifecycle, config helpers, block protection checks, death watchers, task cleanup, and shared utilities. |
| `WeatherDisaster` | Base class for large storm style disasters. Adds storm range, duration, fake weather, smoothing area handling, particle task support, and chunk tracking. |
| `DisasterRegistry` | Registry used to look up, create, and register disaster classes by name. |
| `DisasterStartEvent` | Bukkit event fired before a disaster starts. This event can be cancelled. |
| `DisasterStopEvent` | Bukkit event fired before a disaster stops. This event can be cancelled. |

Useful packages:

```java
import com.github.jewishbanana.deadlydisasters.disasters.Disaster;
import com.github.jewishbanana.deadlydisasters.disasters.WeatherDisaster;
import com.github.jewishbanana.deadlydisasters.disasters.DisasterRegistry;
import com.github.jewishbanana.deadlydisasters.events.DisasterStartEvent;
import com.github.jewishbanana.deadlydisasters.events.DisasterStopEvent;
```

## Listening for Disasters

DeadlyDisasters uses normal Bukkit events. Register a listener in your plugin and listen for `DisasterStartEvent` or `DisasterStopEvent`.

```java
public final class DisasterListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onDisasterStart(DisasterStartEvent event) {
        Disaster disaster = event.getDisaster();

        Bukkit.getLogger().info(disaster.getDisplayName()
                + " started at "
                + event.getLocation().getBlockX() + ", "
                + event.getLocation().getBlockY() + ", "
                + event.getLocation().getBlockZ());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDisasterStop(DisasterStopEvent event) {
        Bukkit.getLogger().info(event.getDisaster().getDisplayName() + " stopped.");
    }
}
```

Register the listener from your plugin:

```java
@Override
public void onEnable() {
    Bukkit.getPluginManager().registerEvents(new DisasterListener(), this);
}
```

### Cancelling a Disaster

`DisasterStartEvent` is cancellable. Cancelling it prevents the disaster from starting.

```java
@EventHandler
public void onDisasterStart(DisasterStartEvent event) {
    if (event.getLevel() >= 5 && event.getReason() == DisasterStartEvent.DisasterStartReason.NATURAL) {
        event.setCancelled(true);
    }
}
```

`DisasterStopEvent` is also cancellable. Only cancel stop events when you have a clear reason, because cancelled stop events can leave an active disaster running.

### Start Reasons

`DisasterStartEvent#getReason()` tells you why the disaster is starting.

| Reason | Meaning |
| --- | --- |
| `NATURAL` | Started by the natural disaster timer. |
| `COMMAND` | Started by `/disasters start`. |
| `CUSTOM` | Started by another plugin or custom API logic. |

Stop events use `DisasterStopReason`.

| Reason | Meaning |
| --- | --- |
| `DISASTER_ENDING` | The disaster finished normally. |
| `COMMAND` | The disaster was stopped by command. |
| `SERVER_CLOSING` | The server is shutting down. |
| `CUSTOM` | Another plugin or custom API logic stopped it. |

## Starting an Existing Disaster

Use `DisasterRegistry` to look up a disaster by its registered name, then create and start it.

```java
public boolean startTornado(Player player) {
    DisasterRegistry registry = DisasterRegistry.getRegistry("tornado");
    if (registry == null)
        return false;

    Location location = player.getLocation();
    int level = 3;

    Disaster disaster = registry.createDisaster(location, player, level);
    if (disaster == null)
        return false;

    Location adjusted = disaster.findPossiblePosition(location);
    if (adjusted != null)
        disaster.setLocation(adjusted);

    if (!disaster.canStart(DisasterStartEvent.DisasterStartReason.CUSTOM))
        return false;

    disaster.broadcastDisaster();
    disaster.start();
    return true;
}
```

Always call `canStart(...)` before starting a disaster yourself. It fires `DisasterStartEvent` and respects region protection hooks such as WorldGuard's `allow-disasters` flag.

## Stopping Disasters

Every active disaster is stored in `Disaster.onGoingDisasters`.

```java
public void stopAllTornadoes() {
    for (Disaster disaster : new ArrayList<>(Disaster.onGoingDisasters)) {
        if (disaster.getClass().getSimpleName().equalsIgnoreCase("Tornado")) {
            disaster.stop(DisasterStopEvent.DisasterStopReason.CUSTOM);
        }
    }
}
```

To stop every active disaster:

```java
Disaster.stopAll(DisasterStopEvent.DisasterStopReason.CUSTOM);
```

## Registered Disaster Names

The built-in free version registers these disaster names:

```text
sinkhole
earthquake
tornado
cavein
water_geyser
lava_geyser
supernova
tsunami
acid_storm
sandstorm
blizzard
extreme_winds
soul_storm
meteor_shower
end_storm
solar_storm
hurricane
purge
```

You can also get the current registered names at runtime:

```java
Set<String> names = DisasterRegistry.getRegisteredNames();
```

## Creating a Custom Disaster

Custom disasters are Java classes that extend `Disaster`.

Your class must have this constructor:

```java
public YourDisaster(Location location, Player player, int level) {
    super(location, player, level);
}
```

The registry uses reflection to call that constructor.

### Basic Disaster Example

```java
public final class LightningBurst extends Disaster {

    public LightningBurst(Location location, Player player, int level) {
        super(location, player, level);
    }

    @Override
    public void init() {
        super.init();
        this.disasterRange = 8 + (level * 4);
    }

    @Override
    public boolean canStart() {
        return location.getWorld().getEnvironment() == World.Environment.NORMAL && super.canStart();
    }

    @Override
    public void start() {
        super.start();

        addDeathWatcher("deaths.lightning_burst");

        scheduleTask(new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                if (ticks++ >= 100) {
                    stop();
                    return;
                }

                Location strike = location.clone().add(
                        ThreadLocalRandom.current().nextDouble(-disasterRange, disasterRange),
                        0,
                        ThreadLocalRandom.current().nextDouble(-disasterRange, disasterRange));

                strike.getWorld().strikeLightning(strike);
            }
        }.runTaskTimer(DeadlyDisasters.getInstance(), 0L, 10L));
    }

    @Override
    public void clean() {
        removeDeathWatcher(0);
        super.clean();
    }

    @Override
    public Function<PlayerDeathEvent, Boolean> getDeathCheck() {
        return event -> event.getEntity().getLocation().distanceSquared(location) <= disasterRange * disasterRange
                && event.getEntity().getLastDamageCause() != null
                && event.getEntity().getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.LIGHTNING;
    }

    @Override
    protected String getConfigPath() {
        return "disasters.destructive.lightning_burst";
    }

    @Override
    public Set<World.Environment> getBannedEnvironments() {
        return EnumSet.of(World.Environment.NETHER, World.Environment.THE_END);
    }
}
```

### Registering the Disaster

Register your disaster during your plugin startup.

```java
@Override
public void onEnable() {
    if (Bukkit.getPluginManager().getPlugin("DeadlyDisasters") == null)
        return;

    DisasterRegistry.registerDisaster("lightning_burst", LightningBurst.class);
}
```

Registry names must be unique. Use lowercase names with underscores.

## Natural Spawning and `softStart`

Registering a disaster makes it available to the registry used by commands and natural selection.

If your custom disaster should only start through your own plugin or through `/disasters start`, override `softStart()` and return `false`.

```java
@Override
public boolean softStart() {
    return false;
}
```

If your custom disaster should be allowed to start naturally, do not override `softStart()` unless you need custom selection behavior. The default `softStart()` flow:

1. Checks banned environments from `getBannedEnvironments()`.
2. Rolls against `getFrequency()`.
3. Calls `findPossiblePosition(...)`.
4. Calls `canStart()`, which fires `DisasterStartEvent`.
5. Checks RealisticSeasons when that hook is enabled.
6. Calls `init()`.
7. Broadcasts the disaster.
8. Starts the disaster after the configured start delay.

## Configuration Paths

If `getConfigPath()` returns a path, the base class can read config values from the world config.

```java
@Override
protected String getConfigPath() {
    return "disasters.destructive.lightning_burst";
}
```

The base `Disaster#init()` automatically reads these shared settings when they exist:

| Setting | Purpose |
| --- | --- |
| `max_level` | Caps the disaster level. |
| `volume` | Multiplies disaster sounds played through the API helpers. |
| `start_delay` | Delay in seconds before natural starts begin after broadcast. |
| `frequency` | Natural selection chance from `0.0` to `1.0`. |
| `respect_global_mob_blacklist` | Whether to merge global mob blacklist entries. |
| `blacklisted_mob_types` | Entity types ignored by `isEntityProtected(...)`. |

These can be placed directly under your disaster section, or inherited from `disasters.global`.

Use the config helper methods inside your disaster:

```java
int minHeight = getConfigInt("minimum_height");
double damage = getConfigDouble("damage");
boolean placeFire = getConfigBoolean("place_fire");
List<String> entries = getConfigStringList("extra_blocks");
```

Use the override helpers when a setting should fall back to global defaults:

```java
double frequency = getConfigOverrideDouble("frequency");
int maxLevel = getConfigOverrideInt("max_level");
```

### Language Entries

`getDisplayName()` uses the same path returned by `getConfigPath()`, but in `language.yml`.

For the example above, add:

```yaml
disasters:
  destructive:
    lightning_burst: "&eLightning Burst"
  tips:
    lightning_burst: "&7Tip: Get indoors before the storm breaks."

deaths:
  lightning_burst: "%player% was struck down by a lightning burst"
```

If you call `addDeathWatcher("deaths.lightning_burst")`, the death message must exist in `language.yml`.

## Disaster Lifecycle

A disaster normally follows this lifecycle:

1. Constructor stores the initial location, player, level, world, and world config wrapper.
2. `init()` reads config and prepares level scaled values.
3. `canStart()` validates start conditions and fires `DisasterStartEvent`.
4. `broadcastDisaster()` sends messages, tips, and start sounds.
5. `start()` begins the disaster and adds it to the active disaster list.
6. `stop(...)` fires `DisasterStopEvent`.
7. `clean()` cancels scheduled tasks and removes disaster owned state.

Always call `super.start()` once when overriding `start()`. Always call `super.clean()` when overriding `clean()`.

## Task Cleanup

Use `scheduleTask(...)` for every repeating or delayed task owned by your disaster. The base class cancels these tasks in `clean()`.

```java
scheduleTask(new BukkitRunnable() {
    @Override
    public void run() {
        // Disaster tick logic.
    }
}.runTaskTimer(DeadlyDisasters.getInstance(), 0L, 1L));
```

Do not leave Bukkit tasks running after the disaster stops.

## Region and Protection Safe Block Changes

Use the block helper methods on `Disaster` instead of directly calling `Block#setType(...)` when the change should respect disaster protection settings.

```java
removeBlock(block);
placeBlock(block, Material.FIRE);
moveBlock(fromBlock, toBlock);
convertBlockIntoFallingBlock(block);
createFallingBlock(location, blockData);
```

These helpers respect immune blocks and region protection checks where applicable.

WorldGuard integration adds two region flags:

| Flag | Purpose |
| --- | --- |
| `allow-disasters` | Controls whether disasters can start in the region. Default behavior blocks disaster starts in protected regions. |
| `allow-disaster-damage` | Controls whether disasters can damage blocks in the region. Default behavior protects region blocks. |

`DisasterStartEvent` is pre-cancelled when the start location is blocked by region protection.

## Entity Protection

Use `isEntityProtected(entity)` before applying disaster effects to an entity.

```java
for (Entity entity : location.getWorld().getNearbyEntities(location, disasterRange, 64, disasterRange)) {
    if (isEntityProtected(entity))
        continue;

    entity.setVelocity(entity.getVelocity().add(new Vector(0, 1, 0)));
}
```

This respects the disaster's mob blacklist and region settings.

## Death Watchers

Death watchers let your disaster replace a player's death message when the death was caused by your disaster.

1. Override `getDeathCheck()`.
2. Call `addDeathWatcher(...)` when the dangerous phase begins.
3. Call `removeDeathWatcher(...)` when that danger ends.

```java
@Override
public Function<PlayerDeathEvent, Boolean> getDeathCheck() {
    return event -> event.getEntity().getLocation().distanceSquared(location) <= disasterRange * disasterRange
            && event.getEntity().getLastDamageCause() != null
            && event.getEntity().getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.FALL;
}
```

```java
addDeathWatcher("deaths.my_disaster");
removeDeathWatcher(160);
```

The delay in `removeDeathWatcher(...)` is useful for disasters that launch players and kill them a few seconds later with fall damage.

## Async Entity Monitoring

`createAsyncEntityMonitor(...)` is useful for disasters that need a frequently refreshed list of nearby entities without doing all filtering on the main thread.

```java
createAsyncEntityMonitor(
        entity -> entity instanceof LivingEntity && !entity.isDead(),
        (foundEntities, entities, players) -> {
            for (Map.Entry<Entity, Location> entry : foundEntities.entrySet()) {
                Entity entity = entry.getKey();

                if (isEntityProtected(entity))
                    continue;

                entities.add(entity);
                if (entity instanceof Player player)
                    players.add(player);
            }
        });
```

The final lists are available in `entitiesInMonitorArea` and `playersInMonitorArea`.

Do not edit blocks or entities from asynchronous code. If your async filter calculates something that requires a Bukkit world change, schedule that change back on the main thread.

## Chunk Loading Helpers

Use `getChunksInvolvedSafelyAndThen(...)` when your disaster needs chunks near its center.

```java
getChunksInvolvedSafelyAndThen(() -> {
    Bukkit.getScheduler().runTask(DeadlyDisasters.getInstance(), () -> {
        // Main thread block or entity work here.
    });
}, false);
```

The callback can run asynchronously on Paper. Keep heavy calculations off the main thread, but schedule world edits back to the main thread.

## Creating Weather Disasters

Weather disasters should extend `WeatherDisaster`. This base class handles duration, range, scaling, fake rain, smoothing around the edge of the storm, and storm cleanup.

```java
public final class AshStorm extends WeatherDisaster {

    public AshStorm(Location location, Player player, int level) {
        super(location, player, level);
    }

    @Override
    public void start() {
        super.start();

        createParticleAsyncTask(
                player -> {
                    player.spawnParticle(Particle.ASH, player.getLocation().add(0, 2, 0), 4, 8, 2, 8, 0.01);
                },
                pair -> {
                    Player player = pair.getFirst();
                    double strength = pair.getSecond();

                    player.spawnParticle(Particle.ASH, player.getLocation().add(0, 2, 0),
                            Math.max(1, (int) (strength * 8)), 8, 2, 8, 0.01);
                });
    }

    @Override
    protected String getConfigPath() {
        return "disasters.weather.ash_storm";
    }
}
```

Weather config can inherit from `disasters.weather.global_weather` before falling back to `disasters.global`.

Common weather settings include:

| Setting | Purpose |
| --- | --- |
| `time.level_1` through `time.level_6` | Duration in seconds. |
| `range.level_1` through `range.level_6` | Storm radius. |
| `scaling.level_1` through `scaling.level_6` | Level based scaling factor. |
| `particle_multiplier` | Particle intensity multiplier. |
| `particle_render_distance` | Distance around each player for storm particles. |

Useful `WeatherDisaster` methods:

```java
isWithinStorm(location);
isWithinStorm(block);
addPlayerToWeather(player);
removePlayerFromWeather(player);
forceDownfallWeather();
buildPotionEffects("entity_effects");
buildBlockChanges("block_changes");
```

Override `forceDownfallWeather()` and return `false` if your weather disaster should not force client side rain.

## Best Practices

Call `super.init()`, `super.start()`, and `super.clean()` when overriding those methods.

Use `scheduleTask(...)` for disaster tasks so cleanup is automatic.

Use `canStart(...)` before manually starting disasters.

Use block helper methods when region protection and immune blocks should be respected.

Use `isEntityProtected(...)` before moving, damaging, burning, or applying potion effects to entities.

Keep expensive scans and sorting off the main thread.

Schedule Bukkit world edits, entity velocity changes, entity spawning, and sound playback on the main thread unless the Bukkit API call is explicitly safe to use asynchronously.

If a registered disaster is only meant for commands or another plugin, override `softStart()` and return `false`.

Keep custom items, custom enchantments, and custom mobs in UIFramework or UltimateContent. DeadlyDisasters focuses on disaster lifecycle, selection, protection, and events.
