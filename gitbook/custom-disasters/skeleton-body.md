# Skeleton Body

To begin your disaster, include a `settings` skeleton body. This section contains the basic information the plugin needs to register and run the disaster.

Example:

```yaml
# Comments that begin with a '*' are required fields.
settings:
  # * Command name for the disaster. Do not use spaces here.
  name: 'doomsday'

  # * Display name for the disaster. Color codes are supported.
  title: '&4Doomsday'

  # How many seconds after the disaster is announced before it begins.
  start_delay: 10

  # If true, this disaster can naturally occur on players.
  natural: false

  # Minimum Y level the player must be at for the disaster to occur.
  min_height: 0

  # Maximum Y level the player can be at for the disaster to occur.
  max_height: 200

  # Message broadcast when the disaster begins.
  # %level_char% is the level color code.
  start_message: '%level_char%Level: %level% &4Doomsday &6is beginning..'

  # Message broadcast when the disaster ends.
  ending_message: '&eThe clouds begin to clear..'

  # Natural occurrence frequency. Only used when natural is true.
  frequency: 1.0

  # * World environment types where the disaster can occur.
  # Valid values: normal, nether, the_end, custom
  environments:
  - normal

  # * Lowest game version the disaster is allowed to occur in.
  game_version: 1.16

  # * Plugin version this disaster was created for.
  plugin_version: 9.0
```

