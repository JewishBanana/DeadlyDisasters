# Weather Disasters

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

## Weather Config Fallbacks

Weather config can inherit from `disasters.weather.global_weather` before falling back to `disasters.global`.

Common weather settings include:

| Setting | Purpose |
| --- | --- |
| `time.level_1` through `time.level_6` | Duration in seconds. |
| `range.level_1` through `range.level_6` | Storm radius. |
| `scaling.level_1` through `scaling.level_6` | Level based scaling factor. |
| `particle_multiplier` | Particle intensity multiplier. |
| `particle_render_distance` | Distance around each player for storm particles. |

## Useful Methods

```java
isWithinStorm(location);
isWithinStorm(block);
addPlayerToWeather(player);
removePlayerFromWeather(player);
forceDownfallWeather();
buildPotionEffects("entity_effects");
buildBlockChanges("block_changes");
```

Override `forceDownfallWeather()` and return `false` if your weather disaster should not force client-side rain.

## Particle Task Structure

`createParticleAsyncTask(...)` accepts one callback for players inside the storm and one callback for players in the smoothing area.

The smoothing callback receives a `Pair<Player, Double>`. The double is the local storm intensity at that player's position, which lets you reduce particles, sounds, or movement at the outer edge.

Only perform safe visual work from the async particle task. Schedule entity changes, block changes, sound playback, and spawning back onto the main thread.

