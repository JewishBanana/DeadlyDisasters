# Core Body

The `core` section defines what happens during the disaster and how each level behaves.

You can place the core anywhere in the YAML file, but it is usually placed under `settings`. A custom disaster can define up to six levels. You do not need to define every level if your disaster only supports a smaller range.

Example:

```yaml
core:
  level 1:
    # operation can be 'ordered' or 'random'.
    operation: 'ordered'

    # Seconds between events.
    interval_seconds: 10

    events:
      event 1:
        disaster 1:
          type: 'acidstorm'
          level: 2
          time: 60
          damage: 2.0
          particles: false
          broadcastAllowed: true

        broadcast 1:
          message: '&cYou feel a cold chill down your spine..'
          target: 'all'

      event 2:
        disaster 1:
          type: 'sinkhole'
          size: 0.5
          target: 'player'
          offset: 5

        disaster 2:
          type: 'earthquake'
          level: 3
          force: 0.5
          target: 'player'
          offset: 10

      # Empty events can be used as pauses.
      event 3:

      event 4:
        disaster 1:
          type: 'supernova'
          level: 1
          target: 'player'

  level 2:
    operation: 'random'

    # Optional for random operation. Limits how many events are selected.
    number_of_events: 3

    interval_seconds: 9

    events:
      event 1:
        disaster 1:
          type: 'acidstorm'
          level: 2
          time: 60
          damage: 2.0
          particles: true

      event 2:
        disaster 1:
          type: 'sinkhole'
          size: 0.6
          target: 'all'
          offset: 5

        disaster 2:
          type: 'earthquake'
          level: 3
          force: 1.2
          target: 'random'
          targetAmount: 3
          offset: 10
```

Events are groups of actions. When an event runs, every action inside that event runs.

