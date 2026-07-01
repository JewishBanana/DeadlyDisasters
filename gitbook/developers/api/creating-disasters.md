# Creating Disasters

Custom disasters are Java classes that extend `Disaster`.

Your class must have this constructor:

```java
public YourDisaster(Location location, Player player, int level) {
    super(location, player, level);
}
```

The registry uses reflection to call that constructor.

## Basic Example

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
    public String getDisplayName() {
        return "Lightning Burst";
    }

    @Override
    public double getFrequency() {
        return 0.35;
    }

    @Override
    public Set<World.Environment> getBannedEnvironments() {
        return EnumSet.of(World.Environment.NETHER, World.Environment.THE_END);
    }
}
```

## Registering The Disaster

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

## Natural Spawning And `softStart`

Registering a disaster makes it available to the registry used by commands and natural selection.

If your custom disaster should only start through your own plugin or through `/disasters start`, override `softStart()` and return `false`.

```java
@Override
public boolean softStart() {
    return false;
}
```

If your custom disaster should be allowed to start naturally, do not override `softStart()` unless you need custom selection behavior.

The default `softStart()` flow:

1. Checks banned environments from `getBannedEnvironments()`.
2. Rolls against `getFrequency()`.
3. Calls `findPossiblePosition(...)`.
4. Calls `canStart()`, which fires `DisasterStartEvent`.
5. Checks RealisticSeasons when that hook is enabled.
6. Calls `init()`.
7. Broadcasts the disaster.
8. Starts the disaster after the configured start delay.

External custom disasters should usually override `getFrequency()` directly instead of relying on config-backed frequency values.

## Lifecycle

A disaster normally follows this lifecycle:

1. Constructor stores the initial location, player, level, and world.
2. `init()` prepares level scaled values.
3. `canStart()` validates start conditions and fires `DisasterStartEvent`.
4. `broadcastDisaster()` sends messages, tips, and start sounds.
5. `start()` begins the disaster and adds it to the active disaster list.
6. `stop(...)` fires `DisasterStopEvent`.
7. `clean()` cancels scheduled tasks and removes disaster owned state.

Always call `super.start()` once when overriding `start()`. Always call `super.clean()` when overriding `clean()`.
