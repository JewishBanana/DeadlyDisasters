package deadlydisasters.utils;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public abstract class RepeatingTask implements Runnable {

    private BukkitTask taskId;

    public RepeatingTask(JavaPlugin plugin, int arg1, int arg2) {
        taskId = plugin.getServer().getScheduler().runTaskTimer(plugin, this, arg1, arg2);
    }
    public void cancel() {
        taskId.cancel();
    }
}