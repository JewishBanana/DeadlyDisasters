# Custom Items

This page documents the legacy custom disaster item section.

For new Java plugin development, custom items, enchantments, and abilities are handled by UIFramework and UltimateContent.

Within a custom disaster YAML file, legacy custom items can be referenced by custom entities.

```yaml
settings:
  # Settings info

core:
  # Core info

items:
  coolSword:
    name: '&6Cool Sword'
    type: iron_sword
    hide_enchants: false
    lore:
    - '&eThis is line 1 of lore'
    - '&bThis is line 2 of lore'
    enchantments:
      enchant 1:
        enchantment: sharpness
        level: 2
      enchant 2:
        enchantment: unbreaking
        level: 3

  strongerShield:
    name: '&4Stronger Shield'
    type: shield
    hide_enchants: true
    lore:
    - '&eThis shield is a much more durable shield!'
    - '&b+3 Durability'
    enchantments:
      enchant 1:
        enchantment: unbreaking
        level: 3
```

This creates `coolSword` and `strongerShield`, which can be referenced by name in the custom entities section.

