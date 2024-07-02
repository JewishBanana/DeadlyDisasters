package com.github.jewishbanana.deadlydisasters.utils;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public abstract class AsyncRepeatingTask implements Runnable {

    private BukkitTask taskId;

    public AsyncRepeatingTask(JavaPlugin plugin, int arg1, int arg2) {
        taskId = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this, arg1, arg2);
    }
    public void cancel() {
        taskId.cancel();
    }
}