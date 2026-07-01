# Starting And Stopping Disasters

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

## Stopping A Specific Disaster Type

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

## Stopping Every Disaster

```java
Disaster.stopAll(DisasterStopEvent.DisasterStopReason.CUSTOM);
```

Only use global stopping when you really mean to stop every active disaster on the server.
