package org.patchbukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.Test;

class PatchBukkitServicesManagerTest {
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
    void retainsConcurrentRegistrationsAndRemovesEmptyServices() throws InterruptedException {
        PatchBukkitServicesManager manager = new PatchBukkitServicesManager();
        CountDownLatch ready = new CountDownLatch(20);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(20);
        for (int i = 0; i < 20; i++) {
            Thread.ofVirtual().start(() -> {
                ready.countDown();
                try {
                    start.await();
                    manager.register(Runnable.class, () -> {}, PLUGIN, ServicePriority.Normal);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        assertTrue(ready.await(3, TimeUnit.SECONDS));
        start.countDown();
        assertTrue(done.await(3, TimeUnit.SECONDS));
        assertEquals(20, manager.getRegistrations(Runnable.class).size());
        manager.unregisterAll(PLUGIN);
        assertFalse(manager.isProvidedFor(Runnable.class));
        assertFalse(manager.getKnownServices().contains(Runnable.class));
    }
}
