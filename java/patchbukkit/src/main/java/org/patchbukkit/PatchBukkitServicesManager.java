package org.patchbukkit;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PatchBukkitServicesManager implements ServicesManager {

    private final Map<Class<?>, List<RegisteredServiceProvider<?>>> providers = new ConcurrentHashMap<>();

    @Override
    public <T> void register(@NotNull Class<T> service, @NotNull T provider, @NotNull Plugin plugin, @NotNull ServicePriority priority) {
        providers.compute(service, (key, current) -> {
            List<RegisteredServiceProvider<?>> next = new java.util.ArrayList<>(current == null ? List.of() : current);
            next.add(new RegisteredServiceProvider<>(service, provider, priority, plugin));
            next.sort((a, b) -> b.getPriority().compareTo(a.getPriority()));
            return List.copyOf(next);
        });
    }

    @Override
    public void unregisterAll(@NotNull Plugin plugin) {
        providers.replaceAll((service, current) -> current.stream().filter(provider -> !provider.getPlugin().equals(plugin)).toList());
        providers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    @Override
    public void unregister(@NotNull Object provider) {
        providers.replaceAll((service, current) -> current.stream().filter(registration -> !registration.getProvider().equals(provider)).toList());
        providers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    @Override
    public void unregister(@NotNull Class<?> service, @NotNull Object provider) {
        providers.computeIfPresent(service, (key, current) -> {
            List<RegisteredServiceProvider<?>> next = current.stream().filter(registration -> !registration.getProvider().equals(provider)).toList();
            return next.isEmpty() ? null : next;
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T load(@NotNull Class<T> service) {
        List<RegisteredServiceProvider<?>> list = providers.get(service);
        if (list == null || list.isEmpty()) return null;
        return (T) list.get(0).getProvider();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> RegisteredServiceProvider<T> getRegistration(@NotNull Class<T> service) {
        List<RegisteredServiceProvider<?>> list = providers.get(service);
        if (list == null || list.isEmpty()) return null;
        return (RegisteredServiceProvider<T>) list.get(0);
    }

    @Override
    public @NotNull List<RegisteredServiceProvider<?>> getRegistrations(@NotNull Plugin plugin) {
        List<RegisteredServiceProvider<?>> result = new java.util.ArrayList<>();
        providers.values().forEach(list -> list.stream()
                .filter(r -> r.getPlugin().equals(plugin))
                .forEach(result::add));
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @NotNull Collection<RegisteredServiceProvider<T>> getRegistrations(@NotNull Class<T> service) {
        List<RegisteredServiceProvider<?>> list = providers.get(service);
        if (list == null) return Collections.emptyList();
        List<RegisteredServiceProvider<T>> result = new java.util.ArrayList<>();
        for (RegisteredServiceProvider<?> r : list) result.add((RegisteredServiceProvider<T>) r);
        return result;
    }

    @Override
    public @NotNull Collection<Class<?>> getKnownServices() {
        return Collections.unmodifiableSet(providers.keySet());
    }

    @Override
    public <T> boolean isProvidedFor(@NotNull Class<T> service) {
        List<RegisteredServiceProvider<?>> list = providers.get(service);
        return list != null && !list.isEmpty();
    }
}
