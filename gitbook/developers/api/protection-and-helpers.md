# Protection And Helper APIs

Use the helper methods on `Disaster` when your disaster should respect plugin protection settings and shared cleanup behavior.

## Region-Safe Block Changes

Use these methods instead of directly calling `Block#setType(...)` when the change should respect disaster protection settings.

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

