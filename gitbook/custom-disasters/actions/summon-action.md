# Summon Action

The summon action spawns a custom mob from the legacy custom disaster entity section.

```yaml
summon 1:
  type: 'customZombie'
  target: 'player'
  offset: 10
  force: false
  setTarget: true
```

`force` controls whether the mob spawns at the exact location even if the block is occupied. If false, the plugin tries to find available ground.

You can also use a relative location.

```yaml
summon 2:
  type: 'scaryMob'
  target: 'startPos'
  location:
    x: ~10
    y: ~0
    z: ~-10
```

