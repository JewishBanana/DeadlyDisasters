# Sound Action

The sound action plays a sound in the world.

This example plays the anvil land sound near all players every three seconds, five times.

```yaml
sound 1:
  type: 'block_anvil_land'
  target: 'all'
  volume: 0.5
  pitch: 0.5
  repeat: 5
  intervalTicks: 60
  location:
    x: ~5
    y: ~0
    z: ~5
```

For available sound names, see the Spigot `Sound` enum:

https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html

