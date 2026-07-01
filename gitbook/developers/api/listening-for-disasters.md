# Listening For Disasters

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

## Cancelling A Disaster

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

## Start Reasons

`DisasterStartEvent#getReason()` tells you why the disaster is starting.

| Reason | Meaning |
| --- | --- |
| `NATURAL` | Started by the natural disaster timer. |
| `COMMAND` | Started by `/disasters start`. |
| `CUSTOM` | Started by another plugin or custom API logic. |

## Stop Reasons

`DisasterStopEvent#getReason()` tells you why the disaster is stopping.

| Reason | Meaning |
| --- | --- |
| `DISASTER_ENDING` | The disaster finished normally. |
| `COMMAND` | The disaster was stopped by command. |
| `SERVER_CLOSING` | The server is shutting down. |
| `CUSTOM` | Another plugin or custom API logic stopped it. |

`DisasterStartEvent` is pre-cancelled when the start location is blocked by region protection.
