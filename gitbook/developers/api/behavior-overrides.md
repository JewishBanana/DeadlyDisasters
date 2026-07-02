# Behavior Overrides

External custom disasters should configure behavior in code for now. The built-in disasters have internal config support, but external disaster config sections are not a clean public API yet.

For third-party disasters, customize behavior by overriding methods directly.

## `getFrequency()`

Controls how often the disaster passes the natural selection frequency roll.

`1.0` means the disaster always passes the frequency roll when selected. `0.0` means it never naturally starts.

```java
@Override
public double getFrequency() {
    return 0.35;
}
```

This only matters when the disaster is allowed to start naturally. If your disaster should only be started by your own plugin or by command, override `softStart()` and return `false`.

## `getBannedEnvironments()`

Controls which world environments the disaster cannot start in.

```java
@Override
public Set<World.Environment> getBannedEnvironments() {
    return EnumSet.of(World.Environment.NETHER, World.Environment.THE_END);
}
```

The base `softStart()` checks this before the disaster starts naturally.

## `findPossiblePosition(...)`

Lets a disaster adjust or reject the original start location.

Return a new location to move the start position. Return `null` if no valid position was found.

```java
@Override
public Location findPossiblePosition(Location initial) {
    if (initial == null)
        return null;

    Block ground = initial.getWorld().getHighestBlockAt(initial);
    return ground.getLocation().add(0.5, 1.0, 0.5);
}
```

This is useful for disasters that must start on the surface, underground, near water, or at a specific Y range. By default the initial location will be the player or world position the disaster was called to start at. For example, if a player is on a shore and the Tsunami disaster calls this method, it will check nearby chunks for an ocean surface and returns a random spot on the ocean surface or null if there is none.

## `canStart()`

Use `canStart()` for custom validation that should happen before the disaster starts.

```java
@Override
public boolean canStart() {
    if (location.getBlockY() < 50)
        return false;

    return super.canStart();
}
```

Call `super.canStart()` so `DisasterStartEvent` is fired and region protection checks are respected.

## `softStart()`

`softStart()` is used by natural disaster selection. Override it if your disaster should not be naturally selected.

```java
@Override
public boolean softStart() {
    return false;
}
```

If you do allow natural selection, the default `softStart()` flow checks the banned environments, rolls `getFrequency()`, calls `findPossiblePosition(...)`, calls `canStart()`, broadcasts, and starts the disaster after the configured delay.

## Display And Broadcast Text

For external custom disasters, prefer overriding display methods directly.

```java
@Override
public String getDisplayName() {
    return "Lightning Burst";
}

@Override
public String getDisasterTip() {
    return "&7Tip: Get indoors before the storm breaks.";
}
```

`broadcastDisaster()` uses `getDisplayName()` and `getDisasterTip()` when it sends disaster messages.

## `getDeathCheck()`

Override `getDeathCheck()` when your disaster needs a custom death message.

```java
@Override
public Function<PlayerDeathEvent, Boolean> getDeathCheck() {
    return event -> event.getEntity().getLocation().distanceSquared(location) <= disasterRange * disasterRange
            && event.getEntity().getLastDamageCause() != null
            && event.getEntity().getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.LIGHTNING;
}
```

Then call `addDeathWatcher(...)` when the dangerous phase begins and `removeDeathWatcher(...)` when it ends.

```java
addDeathWatcher("deaths.lightning_burst");
removeDeathWatcher(160);
```

The language path must exist in `language.yml`. If it does not, the plugin cannot resolve the custom death message.
