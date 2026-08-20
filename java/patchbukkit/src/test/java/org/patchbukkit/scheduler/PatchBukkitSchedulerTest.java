package org.patchbukkit.scheduler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;

class PatchBukkitSchedulerTest {
    private static final Plugin PLUGIN = plugin();

    private static Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[] { Plugin.class }, (proxy, method, args) -> switch (method.getName()) {
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "test";
            default -> null;
        });
    }

    @Test
    void completesAndRemovesSingleTask() throws InterruptedException {
        PatchBukkitScheduler scheduler = new PatchBukkitScheduler();
        CountDownLatch latch = new CountDownLatch(1);
        BukkitTask task = scheduler.runTaskAsynchronously(PLUGIN, latch::countDown);
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertFalse(scheduler.getPendingTasks().stream().anyMatch(pending -> pending.getTaskId() == task.getTaskId()));
        assertTrue(task.isCancelled());
        scheduler.shutdown();
    }

    @Test
    void runsSyncTaskOnTickThread() {
        PatchBukkitScheduler scheduler = new PatchBukkitScheduler();
        AtomicBoolean primary = new AtomicBoolean();
        scheduler.runTask(PLUGIN, () -> {
            primary.set(scheduler.isPrimaryThread());
        });
        scheduler.tick(1);
        assertTrue(primary.get());
        scheduler.shutdown();
    }

    @Test
    void schedulesSyncTasksByServerTick() {
        PatchBukkitScheduler scheduler = new PatchBukkitScheduler();
        AtomicInteger runs = new AtomicInteger();
        scheduler.runTaskLater(PLUGIN, runs::incrementAndGet, 2);
        scheduler.runTaskTimer(PLUGIN, runs::incrementAndGet, 1, 2);
        scheduler.tick(1);
        assertTrue(runs.get() == 1);
        scheduler.tick(2);
        assertTrue(runs.get() == 2);
        scheduler.tick(3);
        assertTrue(runs.get() == 3);
        scheduler.shutdown();
    }

    @Test
    void cancelsDelayedTask() throws InterruptedException {
        PatchBukkitScheduler scheduler = new PatchBukkitScheduler();
        AtomicBoolean ran = new AtomicBoolean();
        BukkitTask task = scheduler.runTaskLaterAsynchronously(PLUGIN, () -> ran.set(true), 2);
        task.cancel();
        Thread.sleep(150);
        assertFalse(ran.get());
        assertTrue(task.isCancelled());
        scheduler.shutdown();
    }
}
