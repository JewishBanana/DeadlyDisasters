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
    public String getDisplayName() {
        return "Ash Storm";
    }
}
```

## Setting Storm Values

External custom weather disasters should set duration, range, scaling, and particle values directly in `init()` until a proper external config API is added.

```java
@Override
public void init() {
    super.init();

    this.time = 20 * 120;
    this.disasterRange = 120.0;
    this.disasterRangeSquared = disasterRange * disasterRange;
    this.scale = 1.5f;
    this.particleMultiplier = 1.0f;
    this.particleRenderDistance = 15.0f;
}
```

## Useful Methods

```java
isWithinStorm(location);
isWithinStorm(block);
addPlayerToWeather(player);
removePlayerFromWeather(player);
forceDownfallWeather();
```

Override `forceDownfallWeather()` and return `false` if your weather disaster should not force client-side rain.

## Particle Task Structure

`createParticleAsyncTask(...)` accepts one callback for players inside the storm and one callback for players in the smoothing area.

The smoothing callback receives a `Pair<Player, Double>`. The double is the local storm intensity at that player's position, which lets you reduce particles, sounds, or movement at the outer edge.

Only perform safe visual work from the async particle task. Schedule entity changes, block changes, sound playback, and spawning back onto the main thread.
