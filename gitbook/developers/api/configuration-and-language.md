# Configuration And Language

If `getConfigPath()` returns a path, the base class can read config values from the world config.

```java
@Override
protected String getConfigPath() {
    return "disasters.destructive.lightning_burst";
}
```

The base `Disaster#init()` automatically reads these shared settings when they exist.

| Setting | Purpose |
| --- | --- |
| `max_level` | Caps the disaster level. |
| `volume` | Multiplies disaster sounds played through the API helpers. |
| `start_delay` | Delay in seconds before natural starts begin after broadcast. |
| `frequency` | Natural selection chance from `0.0` to `1.0`. |
| `respect_global_mob_blacklist` | Whether to merge global mob blacklist entries. |
| `blacklisted_mob_types` | Entity types ignored by `isEntityProtected(...)`. |

These can be placed directly under your disaster section, or inherited from `disasters.global`.

## Reading Config Values

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

## Language Entries

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

## Broadcasts And Tips

`broadcastDisaster()` uses the broadcast message path returned by `getBroadcastMessageConfigPath()`.

The default destructive disaster path is:

```text
messages.disaster_broadcasts.general.level_<level>
```

Weather disasters override this to:

```text
messages.disaster_broadcasts.weather.level_<level>
```

Tips are read from:

```text
disasters.tips.<disaster_name>
```

