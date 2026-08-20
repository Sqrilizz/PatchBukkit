package org.patchbukkit.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitWorker;
import org.jetbrains.annotations.NotNull;

public class PatchBukkitScheduler implements BukkitScheduler {
    private static final long MS_PER_TICK = 50L;
    private final ScheduledThreadPoolExecutor asyncExecutor = executor(Math.max(2, Runtime.getRuntime().availableProcessors()));
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final AtomicLong currentTick = new AtomicLong();
    private final ConcurrentHashMap<Integer, State> tasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Thread> workers = new ConcurrentHashMap<>();
    private volatile Thread primaryThread;

    private static ScheduledThreadPoolExecutor executor(int threads) {
        return new ScheduledThreadPoolExecutor(threads, runnable -> {
            Thread thread = new Thread(runnable, "patchbukkit-async");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void bindPrimaryThread() { primaryThread = Thread.currentThread(); }

    public void tick(long tick) {
        currentTick.set(tick);
        primaryThread = Thread.currentThread();
        tasks.values().stream().filter(state -> state.sync && !state.cancelled.get() && !state.running.get() && state.nextTick <= tick).toList().forEach(state -> runSync(state, tick));
    }

    private BukkitTask submit(Plugin plugin, Runnable runnable, long delay, long period, boolean sync) {
        if (delay < 0) delay = 0;
        if (period == 0 || period < -1) throw new IllegalArgumentException("Period must be positive");
        int id = nextId.getAndIncrement();
        State state = new State(id, plugin, sync, period);
        state.task = runnable;
        tasks.put(id, state);
        if (sync) state.nextTick = currentTick.get() + Math.max(delay, 1);
        else state.future = period < 0 ? asyncExecutor.schedule(() -> runAsync(state, runnable, true), delay * MS_PER_TICK, TimeUnit.MILLISECONDS) : asyncExecutor.scheduleAtFixedRate(() -> runAsync(state, runnable, false), delay * MS_PER_TICK, period * MS_PER_TICK, TimeUnit.MILLISECONDS);
        return new PatchBukkitTask(id, plugin, sync, this);
    }

    private void runSync(State state, long tick) {
        if (state.cancelled.get()) return;
        if (state.period >= 0) state.nextTick = tick + state.period;
        run(state, state.task);
        if (state.period < 0) tasks.remove(state.id, state);
    }

    private void runAsync(State state, Runnable task, boolean once) {
        run(state, task);
        if (once) tasks.remove(state.id, state);
    }

    private void run(State state, Runnable task) {
        if (state.cancelled.get()) return;
        state.running.set(true);
        if (!state.sync) workers.put(state.id, Thread.currentThread());
        try { task.run(); } finally {
            state.running.set(false);
            workers.remove(state.id);
        }
    }

    public boolean isPrimaryThread() { return Thread.currentThread() == primaryThread; }

    public void shutdown() {
        tasks.keySet().forEach(this::cancelTask);
        asyncExecutor.shutdownNow();
    }

    @Override public void cancelTask(int taskId) {
        State state = tasks.remove(taskId);
        if (state != null) {
            state.cancelled.set(true);
            if (state.future != null) state.future.cancel(false);
        }
    }

    @Override public void cancelTasks(@NotNull Plugin plugin) { tasks.values().stream().filter(state -> state.plugin.equals(plugin)).map(state -> state.id).toList().forEach(this::cancelTask); }
    @Override public boolean isCurrentlyRunning(int taskId) { State state = tasks.get(taskId); return state != null && state.running.get(); }
    @Override public boolean isQueued(int taskId) { State state = tasks.get(taskId); return state != null && !state.cancelled.get() && !state.running.get(); }
    @Override public @NotNull List<BukkitWorker> getActiveWorkers() {
        List<BukkitWorker> result = new ArrayList<>();
        workers.forEach((id, thread) -> { State state = tasks.get(id); if (state != null) result.add(new Worker(state, thread)); });
        return result;
    }
    @Override public @NotNull List<BukkitTask> getPendingTasks() { return tasks.values().stream().filter(state -> !state.running.get() && !state.cancelled.get()).<BukkitTask>map(state -> new PatchBukkitTask(state.id, state.plugin, state.sync, this)).toList(); }

    @Override public @NotNull BukkitTask runTask(@NotNull Plugin plugin, @NotNull Runnable task) { return submit(plugin, task, 0, -1, true); }
    @Override public void runTask(@NotNull Plugin plugin, @NotNull Consumer<? super BukkitTask> task) { consumer(plugin, task, 0, -1, true); }
    @Override public @NotNull BukkitTask runTask(@NotNull Plugin plugin, @NotNull BukkitRunnable task) { return runTask(plugin, (Runnable) task); }
    @Override public @NotNull BukkitTask runTaskAsynchronously(@NotNull Plugin plugin, @NotNull Runnable task) { return submit(plugin, task, 0, -1, false); }
    @Override public void runTaskAsynchronously(@NotNull Plugin plugin, @NotNull Consumer<? super BukkitTask> task) { consumer(plugin, task, 0, -1, false); }
    @Override public @NotNull BukkitTask runTaskAsynchronously(@NotNull Plugin plugin, @NotNull BukkitRunnable task) { return runTaskAsynchronously(plugin, (Runnable) task); }
    @Override public @NotNull BukkitTask runTaskLater(@NotNull Plugin plugin, @NotNull Runnable task, long delay) { return submit(plugin, task, delay, -1, true); }
    @Override public void runTaskLater(@NotNull Plugin plugin, @NotNull Consumer<? super BukkitTask> task, long delay) { consumer(plugin, task, delay, -1, true); }
    @Override public @NotNull BukkitTask runTaskLater(@NotNull Plugin plugin, @NotNull BukkitRunnable task, long delay) { return runTaskLater(plugin, (Runnable) task, delay); }
    @Override public @NotNull BukkitTask runTaskLaterAsynchronously(@NotNull Plugin plugin, @NotNull Runnable task, long delay) { return submit(plugin, task, delay, -1, false); }
    @Override public void runTaskLaterAsynchronously(@NotNull Plugin plugin, @NotNull Consumer<? super BukkitTask> task, long delay) { consumer(plugin, task, delay, -1, false); }
    @Override public @NotNull BukkitTask runTaskLaterAsynchronously(@NotNull Plugin plugin, @NotNull BukkitRunnable task, long delay) { return runTaskLaterAsynchronously(plugin, (Runnable) task, delay); }
    @Override public @NotNull BukkitTask runTaskTimer(@NotNull Plugin plugin, @NotNull Runnable task, long delay, long period) { return submit(plugin, task, delay, period, true); }
    @Override public void runTaskTimer(@NotNull Plugin plugin, @NotNull Consumer<? super BukkitTask> task, long delay, long period) { consumer(plugin, task, delay, period, true); }
    @Override public @NotNull BukkitTask runTaskTimer(@NotNull Plugin plugin, @NotNull BukkitRunnable task, long delay, long period) { return runTaskTimer(plugin, (Runnable) task, delay, period); }
    @Override public @NotNull BukkitTask runTaskTimerAsynchronously(@NotNull Plugin plugin, @NotNull Runnable task, long delay, long period) { return submit(plugin, task, delay, period, false); }
    @Override public void runTaskTimerAsynchronously(@NotNull Plugin plugin, @NotNull Consumer<? super BukkitTask> task, long delay, long period) { consumer(plugin, task, delay, period, false); }
    @Override public @NotNull BukkitTask runTaskTimerAsynchronously(@NotNull Plugin plugin, @NotNull BukkitRunnable task, long delay, long period) { return runTaskTimerAsynchronously(plugin, (Runnable) task, delay, period); }
    @Override public int scheduleSyncDelayedTask(@NotNull Plugin plugin, @NotNull Runnable task, long delay) { return runTaskLater(plugin, task, delay).getTaskId(); }
    @Override public int scheduleSyncDelayedTask(@NotNull Plugin plugin, @NotNull BukkitRunnable task, long delay) { return runTaskLater(plugin, task, delay).getTaskId(); }
    @Override public int scheduleSyncDelayedTask(@NotNull Plugin plugin, @NotNull Runnable task) { return runTask(plugin, task).getTaskId(); }
    @Override public int scheduleSyncDelayedTask(@NotNull Plugin plugin, @NotNull BukkitRunnable task) { return runTask(plugin, task).getTaskId(); }
    @Override public int scheduleSyncRepeatingTask(@NotNull Plugin plugin, @NotNull Runnable task, long delay, long period) { return runTaskTimer(plugin, task, delay, period).getTaskId(); }
    @Override public int scheduleSyncRepeatingTask(@NotNull Plugin plugin, @NotNull BukkitRunnable task, long delay, long period) { return runTaskTimer(plugin, task, delay, period).getTaskId(); }
    @Override public int scheduleAsyncDelayedTask(@NotNull Plugin plugin, @NotNull Runnable task, long delay) { return runTaskLaterAsynchronously(plugin, task, delay).getTaskId(); }
    @Override public int scheduleAsyncDelayedTask(@NotNull Plugin plugin, @NotNull Runnable task) { return runTaskAsynchronously(plugin, task).getTaskId(); }
    @Override public int scheduleAsyncRepeatingTask(@NotNull Plugin plugin, @NotNull Runnable task, long delay, long period) { return runTaskTimerAsynchronously(plugin, task, delay, period).getTaskId(); }
    @Override public <T> @NotNull Future<T> callSyncMethod(@NotNull Plugin plugin, @NotNull Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        runTask(plugin, () -> { try { future.complete(task.call()); } catch (Throwable error) { future.completeExceptionally(error); } });
        return future;
    }
    @Override public @NotNull Executor getMainThreadExecutor(@NotNull Plugin plugin) { return runnable -> runTask(plugin, runnable); }

    private void consumer(Plugin plugin, Consumer<? super BukkitTask> consumer, long delay, long period, boolean sync) {
        BukkitTask[] reference = new BukkitTask[1];
        reference[0] = submit(plugin, () -> consumer.accept(reference[0]), delay, period, sync);
    }

    private static final class State {
        private final int id;
        private final Plugin plugin;
        private final boolean sync;
        private final long period;
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicBoolean running = new AtomicBoolean();
        private volatile long nextTick;
        private volatile ScheduledFuture<?> future;
        private volatile Runnable task;
        private State(int id, Plugin plugin, boolean sync, long period) { this.id = id; this.plugin = plugin; this.sync = sync; this.period = period; }
    }

    private record Worker(State state, Thread thread) implements BukkitWorker {
        @Override public int getTaskId() { return state.id; }
        @Override public @NotNull Plugin getOwner() { return state.plugin; }
        @Override public @NotNull Thread getThread() { return thread; }
    }
}
