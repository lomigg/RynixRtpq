package com.rynix.rtpq.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Paper / Spigot Scheduler Implementation
 * Plugin: RynixRtpq | Author: RinZz
 */
public class PaperScheduler implements SchedulerAdapter {

    @Override
    public void run(Plugin plugin, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runAsync(Plugin plugin, Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runDelayed(Plugin plugin, Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    @Override
    public void runAsyncDelayed(Plugin plugin, Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
    }

    @Override
    public void runAtLocation(Plugin plugin, Location location, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runDelayedAtLocation(Plugin plugin, Location location, Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    @Override
    public void runForEntity(Plugin plugin, Entity entity, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runDelayedForEntity(Plugin plugin, Entity entity, Runnable task, long delayTicks, Runnable retired) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    @Override
    public void runTimer(Plugin plugin, Runnable task, long delay, long period) {
        Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
    }

    @Override
    public void runAsyncTimer(Plugin plugin, Runnable task, long delay, long period) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period);
    }

    @Override
    public void cancelAll(Plugin plugin) {
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
