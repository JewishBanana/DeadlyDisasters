# Particle Action

The particle action spawns particles for visual detail.

```yaml
particle 1:
  type: 'flame'
  target: 'startPos'
  count: 10
  speed: 0.2
  repeat: 5
  intervalTicks: 10
  range:
    x: 5
    y: 2
    z: 3
  location:
    x: ~5
    y: ~0
    z: ~5
```

For available particle names, see the Spigot `Particle` enum:

https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Particle.html

