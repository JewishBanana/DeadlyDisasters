# Disaster Action

Disaster actions start a built-in disaster at a target location. They cannot start another custom disaster.

Each disaster supports its own optional modifiers. If a modifier is not specified, the plugin uses the default value from the server's config or the disaster's internal level value.

```yaml
disaster 1:
  # * Type of disaster to start.
  type: 'sinkhole'

  # Disaster level. If omitted, a random level is used.
  level: 3

  # Size modifier.
  size: 0.5

  # Target location. Valid values: player, random, all, startPos.
  target: 'player'

  # Random radius from the target.
  offset: 5
```

You can also define exact or relative coordinates. A `~` makes the coordinate relative to the target.

```yaml
disaster 2:
  type: 'earthquake'
  force: 0.5
  target: 'random'
  targetAmount: 5
  location:
    x: ~10
    y: 60
    z: ~-10
```

This starts earthquakes 10 blocks away from five random players at Y level 60.

## Disaster Modifiers

Modifiers are optional disaster-specific fields.

```yaml
disaster 1:
  type: 'supernova'
  level: 2
  size: 3.5
  particleType: falling_water
  flash: false
  volume: 0.5
  materials:
  - water
  - dirt
  - sand
```

```yaml
disaster 2:
  type: 'meteorshower'
  level: 4
  time: 30
  night: false
  spawnRate: 5
  particleType: water_bubble
  maxMeteors: 10
  normalMeteor:
    material: obsidian
    generateOres: false
    minSize: 4
    maxSize: 10
    speed: 0.8
  explodingMeteor:
    material: lava
    damage: 5
    minSize: 2
    maxSize: 5
    speed: 2.5
  splittingMeteor:
    material: gravel
    impactSpeed: 3
    minSize: 3
    maxSize: 6
    speed: 2
```

