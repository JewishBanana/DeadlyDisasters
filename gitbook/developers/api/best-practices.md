# Best Practices

Call `super.init()`, `super.start()`, and `super.clean()` when overriding those methods.

Use `scheduleTask(...)` for disaster tasks so cleanup is automatic.

Use `canStart(...)` before manually starting disasters.

Use block helper methods when region protection and immune blocks should be respected.

Use `isEntityProtected(...)` before moving, damaging, burning, or applying potion effects to entities.

Keep expensive scans and sorting off the main thread.

Schedule Bukkit world edits, entity velocity changes, entity spawning, and sound playback on the main thread unless the Bukkit API call is explicitly safe to use asynchronously.

If a registered disaster is only meant for commands or another plugin, override `softStart()` and return `false`.

Keep custom items, custom enchantments, and custom mobs in UIFramework or UltimateContent. DeadlyDisasters focuses on disaster lifecycle, selection, protection, and events.
