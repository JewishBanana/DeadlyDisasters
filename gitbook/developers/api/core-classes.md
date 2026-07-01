# Core Classes

Most integrations use these classes.

| Class | Purpose |
| --- | --- |
| `Disaster` | Base class for all disasters. Handles lifecycle, behavior overrides, block protection checks, death watchers, task cleanup, and shared utilities. |
| `WeatherDisaster` | Base class for large storm style disasters. Adds storm range, duration, fake weather, smoothing area handling, particle task support, and chunk tracking. |
| `DisasterRegistry` | Registry used to look up, create, and register disaster classes by name. |
| `DisasterStartEvent` | Bukkit event fired before a disaster starts. This event can be cancelled. |
| `DisasterStopEvent` | Bukkit event fired before a disaster stops. This event can be cancelled. |

## Disaster

`Disaster` is the base class for destructive, mob, and custom disaster implementations.

Important responsibilities:

* Tracks the active disaster in `Disaster.onGoingDisasters`.
* Stores the start location, world, target player, and level.
* Provides overridable methods such as `getFrequency()`, `getBannedEnvironments()`, and `getDeathCheck()`.
* Provides region-safe block helper methods such as `removeBlock(...)`, `placeBlock(...)`, and `convertBlockIntoFallingBlock(...)`.
* Provides death watcher helpers for custom death messages.
* Cancels scheduled tasks during cleanup when they are registered with `scheduleTask(...)`.

## WeatherDisaster

`WeatherDisaster` extends `Disaster` and should be used for storm-like disasters that affect a large radius over time.

It adds:

* Level based duration, range, and scaling.
* Client-side weather control for affected players.
* Smoothing behavior at the outer edge of storms.
* Particle task helpers.
* Storm chunk tracking.
* Shared weather behavior for storms that affect players across a wide radius.

## DisasterRegistry

`DisasterRegistry` maps names to disaster classes.

```java
DisasterRegistry.registerDisaster("lightning_burst", LightningBurst.class);

DisasterRegistry registry = DisasterRegistry.getRegistry("tornado");
Set<String> names = DisasterRegistry.getRegisteredNames();
```

Registry names must be unique. Use lowercase names with underscores.

## Built-In Disaster Names

The free version registers these names:

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
