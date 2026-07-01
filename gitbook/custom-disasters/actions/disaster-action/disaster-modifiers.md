# Disaster Modifiers

This page lists common disaster modifiers from the legacy custom disaster system.

`cfg` means the value defaults to the user's config. `levelBased` means the value is determined internally by the disaster level.

## Sinkhole

```yaml
size: cfg
tickSpeed: cfg
volume: cfg
```

## Earthquake

```yaml
size: cfg
force: cfg
length: levelBased
width: levelBased
tilt: cfg
volume: cfg
```

## Supernova

```yaml
size: cfg
particleChance: cfg
particleType: explosion_huge
flash: true
volume: cfg
materials:
- obsidian
- black_concrete
- fire
```

## Cave-In

```yaml
size: cfg
depth: levelBased
fallSpeed: -0.5
volume: cfg
materials: null
```

## Geyser

```yaml
width: 1
amount: levelBased
damage: cfg
material: water OR lava
tickSpeed: 1
velocity: 3.0
particleType: falling_water OR falling_lava
minReach: 100
maxReach: 256
range: levelBased
volume: cfg
sound: block_fire_extinquish
spawnTicks: 10
```

## Hurricane

```yaml
size: cfg
time: cfg
lightning: cfg
blockForce: cfg
minForce: levelBased
maxForce: levelBased
particleType: 'cloud'
volume: cfg
```

## Purge

```yaml
maxEntities: cfg
showBar: cfg
barTitle: cfg
barColor: 'red'
spawnDistance: cfg
spawnTickSpeed: cfg
despawnSpeed: cfg
endMessage: cfg
volume: cfg
```

## Tornado

```yaml
size: cfg
time: cfg
maxBlocks: cfg
pullForce: levelBased
yVelocity: 0.35
speed: cfg
height: levelBased
particleChance: cfg
width: cfg
particleType: 'cloud'
volume: cfg
materials: null
```

## Tsunami

```yaml
size: cfg
height: levelBased
damage: cfg
tickSpeed: 1
removeLiquid: cfg
ignoreOcean: false
liquidMaterial: water
particleType: water_bubble
minDepth: cfg
volume: cfg
materials:
- sand
- dirt
```

## Acid Storm

```yaml
time: cfg
melt_items: cfg
melt_armor: cfg
damage: cfg
particles: cfg
volume: cfg
broadcastAllowed: false
```

## Blizzard

```yaml
time: cfg
freezeMobs: cfg
leatherProtect: cfg
volume: cfg
broadcastAllowed: false
```

## End Storm

```yaml
time: cfg
maxEntities: cfg
teleportRange: cfg
volume: cfg
broadcastAllowed: false
```

## Extreme Winds

```yaml
time: cfg
force: cfg
particleType: cloud
maxParticles: cfg
blockForce: cfg
volume: cfg
broadcastAllowed: false
```

## Meteor Showers

```yaml
time: cfg
night: cfg
particleType: smoke_large
smokeTime: cfg
spawnRate: 7
maxMeteors: cfg
volume: cfg
broadcastAllowed: false
normalMeteor:
  allowed: true
  material: deepslate OR stone
  generateOres: true
  speed: 1.0
  minSize: 2
  maxSize: 6
explodingMeteor:
  allowed: true
  material: magma_block
  damage: 10.0
  speed: 1.0
  minSize: 2
  maxSize: 6
splittingMeteor:
  allowed: true
  material: calcite OR diorite
  impactSpeed: 1.0
  speed: 1.0
  minSize: 2
  maxSize: 6
```

## Sandstorm

```yaml
time: cfg
customMobs: cfg
wither: cfg
skulls: cfg
volume: cfg
broadcastAllowed: false
```

## Soul Storm

```yaml
time: cfg
customMobs: cfg
volume: cfg
broadcastAllowed: false
```

