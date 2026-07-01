# Setup

Add the DeadlyDisasters jar to your plugin's compile classpath. If you use Maven or Gradle, install the jar locally or use the repository/version you publish DeadlyDisasters under.

If you publish through JitPack, the dependency normally follows the GitHub repository and tag.

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.JewishBanana</groupId>
        <artifactId>DeadlyDisasters</artifactId>
        <version>VERSION</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

After adding the jar to your build, add DeadlyDisasters to your `plugin.yml`.

```yaml
# Use depend if your plugin cannot run without DeadlyDisasters.
depend: [DeadlyDisasters]

# Use softdepend if your plugin can run without DeadlyDisasters.
softdepend: [DeadlyDisasters]
```

If you use `softdepend`, check that the plugin is installed before calling DeadlyDisasters classes.

```java
if (Bukkit.getPluginManager().getPlugin("DeadlyDisasters") == null) {
    getLogger().info("DeadlyDisasters was not found. Disaster integration disabled.");
    return;
}
```

DeadlyDisasters currently targets Java 17 and Bukkit API 1.17 or newer.

Useful imports:

```java
import com.github.jewishbanana.deadlydisasters.disasters.Disaster;
import com.github.jewishbanana.deadlydisasters.disasters.WeatherDisaster;
import com.github.jewishbanana.deadlydisasters.disasters.DisasterRegistry;
import com.github.jewishbanana.deadlydisasters.events.DisasterStartEvent;
import com.github.jewishbanana.deadlydisasters.events.DisasterStopEvent;
```

Custom items, enchantments, and custom mobs are no longer documented under the DeadlyDisasters API. Those systems belong to UIFramework and UltimateContent.
