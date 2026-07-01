# Actions

Actions are things that happen inside events. An event can have any number of actions.

Example:

```yaml
event 1:
  disaster 1:
    type: 'acidstorm'
    level: 5
    broadcastAllowed: false

  disaster 2:
    type: 'earthquake'
    level: 6
    target: 'random'
    targetAmount: 4
    offset: 1

  broadcast 1:
    message: '&cYou feel a cold chill down your spine..'
    target: 'all'

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

Available action types:

* Disaster actions
* Broadcast actions
* Fill actions
* Summon actions
* Particle actions
* Sound actions

