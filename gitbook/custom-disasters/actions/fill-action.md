# Fill Action

The fill action fills a cuboid region with a set of blocks.

This example changes ground below the player in a 100-block radius into a mix of gold blocks, netherrack, and cobblestone.

```yaml
fill 1:
  target: 'player'
  partition: 10
  fillSolids: true
  firstPoint:
    x: ~-50
    y: ~-20
    z: ~-50
  secondPoint:
    x: ~50
    y: ~0
    z: ~50
  materials:
  - gold_block
  - netherrack
  - cobblestone
  whitelist:
  - grass
  - dirt
  - stone
```

You can use either a whitelist or blacklist. A whitelist only replaces listed blocks. A blacklist replaces everything except listed blocks.

```yaml
fill 2:
  target: 'all'
  firstPoint:
    x: ~-10
    y: ~-5
    z: ~-10
  secondPoint:
    x: ~10
    y: ~0
    z: ~10
  blacklist:
  - bedrock
  - obsidian
```

