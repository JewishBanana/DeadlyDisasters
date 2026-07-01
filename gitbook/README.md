# How To Make A Custom Disaster

Welcome to the unlimited possibilities with custom disasters. Throughout this guide, comments with a `*` in front of them mean that the field is required.

## What is a custom disaster?

Custom disasters are community-made disasters for the DeadlyDisasters plugin. They can combine built-in disasters, broadcasts, particles, sounds, block fills, and summons into a reusable YAML file.

You can share custom disasters with the community, or submit them for review so they can be added to the in-game catalog.

## How to make custom disasters

Go to your server's `plugins/DeadlyDisasters/custom disasters` folder and create a new `.yml` file.

The file is made of a few top-level sections:

```yaml
settings:
  # Basic disaster metadata.

core:
  # Events and actions for each level.

items:
  # Optional custom items for the legacy custom disaster system.

entities:
  # Optional custom entities for the legacy custom disaster system.

catalog:
  # Optional catalog metadata.
```

## How to start a custom disaster

In game, use:

```text
/disasters start <custom disaster> [level] [player]
```

The custom disaster file must be in the custom disasters directory.

## How to test custom disasters

Use:

```text
/disasters reload
```

This reloads custom disasters so you can edit the YAML file, save it, and test the changes without restarting the server.

