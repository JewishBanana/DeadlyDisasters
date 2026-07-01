# Custom Entities

This page documents the legacy custom disaster entity section.

For new Java plugin development, custom mobs are handled by UltimateContent. This legacy YAML system is still useful for old custom disaster files that use summon actions.

```yaml
settings:
  # Settings info

core:
  # Core info

items:
  # Optional custom items

entities:
  customZombie:
    type: zombie
    name: '&cCustom Zombie'
    equipment:
      mainHand: coolSword
      offHand: strongerShield
      helmet: iron_helmet
      chest: iron_chestplate
      legs: iron_leggings
      boots: iron_boots
    dropChances:
      mainHand: 1.0
      offHand: 0.5
      helmet: 0.1
      chest: 0.1
      legs: 0.1
      boots: 0.1
    health: 30.0
    speed: 0.3
    range: 50.0
    knockback: 1.0
    damage: 6.0
    resistance: 3.0

  scaryMob:
    type: skeleton
    name: '&cCustom'
    equipment:
      mainHand: bow
      helmet: chainmail_helmet
      chest: chainmail_chestplate
      legs: chainmail_leggings
      boots: chainmail_boots
    dropChances:
      mainHand: 1.0
      helmet: 0.1
      chest: 0.1
      legs: 0.1
      boots: 0.1
    health: 15.0
    speed: 0.3
    range: 50.0
    knockback: 1.0
    damage: 6.0
    resistance: 3.0
```

