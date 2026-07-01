# API

DeadlyDisasters exposes a Java API for starting disasters, listening for disaster lifecycle events, and registering your own disaster classes.

This section documents the current API structure used by the plugin. The guide is split into focused pages so you can jump directly to the part you need.

## Pages

* [Setup](api/setup.md)
* [Core Classes](api/core-classes.md)
* [Listening For Disasters](api/listening-for-disasters.md)
* [Starting And Stopping Disasters](api/starting-and-stopping.md)
* [Creating Disasters](api/creating-disasters.md)
* [Configuration And Language](api/configuration-and-language.md)
* [Protection And Helper APIs](api/protection-and-helpers.md)
* [Weather Disasters](api/weather-disasters.md)
* [Best Practices](api/best-practices.md)

## Scope

DeadlyDisasters focuses on disaster lifecycle, selection, protection checks, events, and disaster utility methods.

Custom items, enchantments, ability items, and custom mobs are handled by UIFramework and UltimateContent. DeadlyDisasters may hook into those plugins when they are installed, but new item or mob development should use those projects instead.
