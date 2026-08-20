package org.patchbukkit;

import com.destroystokyo.paper.entity.ai.MobGoals;
import com.google.common.base.Preconditions;

import io.papermc.paper.ban.BanListType;
import io.papermc.paper.configuration.ServerConfiguration;
import io.papermc.paper.datapack.DatapackManager;
import io.papermc.paper.math.Position;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.patchbukkit.world.PatchBukkitWorld;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.BanList;
import org.bukkit.GameMode;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Registry;
import org.bukkit.Server;
import org.bukkit.ServerLinks;
import org.bukkit.ServerTickManager;
import org.bukkit.StructureType;
import org.bukkit.Tag;
import org.bukkit.UnsafeValues;
import org.bukkit.Warning.WarningState;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.advancement.Advancement;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityFactory;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.generator.ChunkGenerator.ChunkData;
import org.bukkit.help.HelpMap;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemCraftResult;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.Recipe;
import org.bukkit.loot.LootTable;
import org.bukkit.map.MapCursor.Type;
import org.bukkit.map.MapView;
import org.bukkit.packs.ResourcePack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.potion.PotionBrewer;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.CachedServerIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patchbukkit.command.PatchBukkitCommandMap;
import org.patchbukkit.command.PatchBukkitConsoleCommandSender;
import org.patchbukkit.events.PatchBukkitEventManager;
import org.patchbukkit.scheduler.PatchBukkitScheduler;
import org.patchbukkit.versioning.Versioning;

import patchbukkit.bridge.NativeBridgeFfi;
import patchbukkit.log.LogLevel;
import patchbukkit.log.SendLogRequest;

@SuppressWarnings({ "deprecation", "removal", "unchecked" })
public class PatchBukkitServer implements Server {
    private static volatile PatchBukkitServer INSTANCE;

    public PatchBukkitServer() {
        INSTANCE = this;
        String name = "PatchBukkit";
        try {
            name = io.papermc.paper.ServerBuildInfo.buildInfo().brandName();
        } catch (Throwable ignored) {}
        this.serverName = name;
    }

    public static PatchBukkitServer getInstance() {
        if (INSTANCE == null) {
            synchronized (PatchBukkitServer.class) {
                if (INSTANCE == null) {
                    new PatchBukkitServer();
                }
            }
        }
        return INSTANCE;
    }

    static {
        try {
            Class<?> sharedConstants = Class.forName("net.minecraft.SharedConstants");
            sharedConstants.getMethod("tryDetectVersion").invoke(null);
            Class<?> bootstrap = Class.forName("net.minecraft.server.Bootstrap");
            bootstrap.getMethod("bootStrap").invoke(null);
        } catch (Throwable ignored) {}
    }

    public static PatchBukkitServer initServer() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            logger.log(Level.SEVERE, "Uncaught exception in thread " + thread.getName(), throwable);
        });
        PatchBukkitServer server = getInstance();
        org.bukkit.Bukkit.setServer(server);
        ((PatchBukkitScheduler) server.scheduler).bindPrimaryThread();
        return server;
    }

    public static void tickScheduler(long tick) {
        ((PatchBukkitScheduler) getInstance().scheduler).tick(tick);
    }

    private final String serverName;
    private final String bukkitVersion = Versioning.getBukkitVersion();
    public SimpleCommandMap commandMap = new PatchBukkitCommandMap(this);
    public BukkitScheduler scheduler = new PatchBukkitScheduler();
    public PatchBukkitPluginManager pluginManager = new PatchBukkitPluginManager(this);
    public ServicesManager servicesManager = new PatchBukkitServicesManager();


    private final Map<UUID, Player> onlinePlayers = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Player> onlinePlayersByName = new java.util.concurrent.ConcurrentHashMap<>();

    private final Set<UUID> operatorUuids = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private final Set<String> operatorNames = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    public boolean isOp(UUID uuid, String name) {
        if (uuid != null && operatorUuids.contains(uuid)) return true;
        if (name != null && operatorNames.contains(name.toLowerCase())) return true;
        return false;
    }

    public void setOperator(UUID uuid, String name, boolean value) {
        if (uuid != null) {
            if (value) operatorUuids.add(uuid); else operatorUuids.remove(uuid);
        }
        if (name != null) {
            if (value) operatorNames.add(name.toLowerCase()); else operatorNames.remove(name.toLowerCase());
        }
    }

    private static final PrintStream ORIGINAL_OUT = System.out;
    private static final PrintStream ORIGINAL_ERR = System.err;
    private static final ThreadLocal<Boolean> IN_LOGGING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private static final Logger logger = Logger.getLogger("Minecraft");

    static {
        configureRootLogger();
    }

    public static final Map<Level, LogLevel> LEVEL_MAP = Map.of(
            Level.SEVERE,  LogLevel.SEVERE,
            Level.WARNING, LogLevel.WARNING,
            Level.INFO,    LogLevel.INFO,
            Level.CONFIG,  LogLevel.CONFIG,
            Level.FINE,    LogLevel.FINE,
            Level.FINER,   LogLevel.FINER,
            Level.FINEST,  LogLevel.FINEST
    );

    private static void configureRootLogger() {
        Logger root = Logger.getLogger("");
        root.setUseParentHandlers(false);
        for (java.util.logging.Handler h : root.getHandlers()) {
            root.removeHandler(h);
        }
        root.setLevel(Level.ALL); // accept everything, let your Rust side filter
        root.addHandler(new java.util.logging.Handler() {
            @Override
            public void publish(java.util.logging.LogRecord record) {
                if (record == null) return;
                if (IN_LOGGING.get()) return;
                IN_LOGGING.set(true);
                try {
                    LogLevel logLevel = LEVEL_MAP.getOrDefault(record.getLevel(), LogLevel.INFO);
                    String message = formatLogRecord(record);
                    String loggerName = record.getLoggerName() != null ? record.getLoggerName() : "";
                    if (loggerName.startsWith("jdk.") || loggerName.startsWith("sun.") || loggerName.startsWith("java.") || loggerName.startsWith("javax.")) {
                        if (record.getLevel().intValue() < Level.WARNING.intValue()) {
                            return;
                        }
                    }
                    try {
                        NativeBridgeFfi.sendLog(
                                SendLogRequest.newBuilder()
                                        .setLevel(logLevel)
                                        .setMessage(message)
                                        .setLoggerName(loggerName)
                                        .build()
                        );
                    } catch (Throwable t) {
                        ORIGINAL_ERR.println("[" + record.getLevel() + "][" + loggerName + "] " + message);
                    }
                } catch (Throwable t) {
                    ORIGINAL_ERR.println("[RootLogger Error] Failed to publish record: " + t.getMessage());
                } finally {
                    IN_LOGGING.set(false);
                }
            }
            @Override public void flush() {}
            @Override public void close() {}
        });

        redirectSystemStreams();
    }

    public static String formatLogRecord(java.util.logging.LogRecord record) {
        String msg = record.getMessage();
        if (msg == null) {
            msg = "";
        } else if (record.getResourceBundle() != null) {
            try {
                msg = record.getResourceBundle().getString(msg);
            } catch (Exception ignored) {}
        }
        Object[] params = record.getParameters();
        if (params != null && params.length > 0 && msg.contains("{0}")) {
            try {
                msg = MessageFormat.format(msg, params);
            } catch (Exception ignored) {}
        }
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            if (!msg.isEmpty()) {
                pw.println(msg);
            }
            record.getThrown().printStackTrace(pw);
            pw.flush();
            msg = sw.toString();
        }
        return msg;
    }

    private static void redirectSystemStreams() {
        try {
            System.setOut(new LoggingPrintStream(ORIGINAL_OUT, Level.INFO, "System.out"));
            System.setErr(new LoggingPrintStream(ORIGINAL_ERR, Level.SEVERE, "System.err"));
        } catch (Throwable t) {
            ORIGINAL_ERR.println("[PatchBukkit] Failed to redirect System streams: " + t.getMessage());
        }
    }

    private static class LoggingPrintStream extends PrintStream {
        private final PrintStream delegate;
        private final Level level;
        private final String loggerName;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        public LoggingPrintStream(PrintStream delegate, Level level, String loggerName) {
            super(delegate, true);
            this.delegate = delegate;
            this.level = level;
            this.loggerName = loggerName;
        }

        @Override
        public void write(int b) {
            delegate.write(b);
            if (b == '\n') {
                flushBuffer();
            } else if (b != '\r') {
                buffer.write(b);
            }
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            delegate.write(buf, off, len);
            for (int i = off; i < off + len; i++) {
                byte b = buf[i];
                if (b == '\n') {
                    flushBuffer();
                } else if (b != '\r') {
                    buffer.write(b);
                }
            }
        }

        private synchronized void flushBuffer() {
            if (buffer.size() == 0) return;
            String line = buffer.toString(StandardCharsets.UTF_8);
            buffer.reset();
            if (line.isEmpty() || IN_LOGGING.get()) return;
            IN_LOGGING.set(true);
            try {
                LogLevel logLevel = LEVEL_MAP.getOrDefault(level, LogLevel.INFO);
                try {
                    NativeBridgeFfi.sendLog(
                            SendLogRequest.newBuilder()
                                    .setLevel(logLevel)
                                    .setMessage(line)
                                    .setLoggerName(loggerName)
                                    .build()
                    );
                } catch (Throwable ignored) {}
            } finally {
                IN_LOGGING.set(false);
            }
        }
    }

    /**
     * Called from Rust when a player joins the Pumpkin server
     */
    public void registerPlayer(Player player) {
        this.onlinePlayers.put(player.getUniqueId(), player);
        this.onlinePlayersByName.put(player.getName().toLowerCase(), player);
    }

    public static void registerPlayer(String uuidStr, String name, boolean isOp) {
        try {
            UUID uuid = UUID.fromString(uuidStr);
            org.patchbukkit.entity.PatchBukkitPlayer player = new org.patchbukkit.entity.PatchBukkitPlayer(uuid, name);
            if (isOp) {
                player.setOp(true);
            }
            if (org.bukkit.Bukkit.getServer() instanceof PatchBukkitServer server) {
                server.registerPlayer(player);
            }
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Failed to register player: " + name, t);
        }
    }

    public PatchBukkitEventManager getEventManager() {
        return this.pluginManager.getEventManager();
    }

    /**
     * Called from Rust when a player leaves
     */
    public void unregisterPlayer(UUID uuid) {
        Player p = this.onlinePlayers.remove(uuid);
        if (p != null) {
            this.onlinePlayersByName.remove(p.getName().toLowerCase());
        }
    }

    public void registerPlugin(@NotNull Plugin plugin) {
        this.pluginManager.registerPlugin(plugin);
    }

    private final Messenger messenger = new org.patchbukkit.messaging.PatchBukkitMessenger();
    private final HelpMap helpMap = new org.patchbukkit.help.PatchBukkitHelpMap();
    private File pluginsFolder = new File("plugins");

    private static void validateChannel(String channel) {
        if (channel == null) throw new IllegalArgumentException("Channel cannot be null");
        if (channel.length() > 64) {
            throw new IllegalArgumentException("Channel '" + channel + "' is invalid");
        }
    }

    @Override
    public void sendPluginMessage(
        @NotNull Plugin source,
        @NotNull String channel,
        byte@NotNull [] message
    ) {
        if (source == null) throw new IllegalArgumentException("Plugin source cannot be null");
        validateChannel(channel);
        if (message == null) throw new IllegalArgumentException("Message cannot be null");

        if (!messenger.isOutgoingChannelRegistered(source, channel)) {
            throw new IllegalArgumentException("Plugin " + source.getDescription().getFullName() + " has not registered outgoing channel '" + channel + "'");
        }

        for (Player player : getOnlinePlayers()) {
            player.sendPluginMessage(source, channel, message);
        }
    }

    @Override
    public @NotNull Set<String> getListeningPluginChannels() {
        Set<String> channels = new java.util.HashSet<>();
        for (Player player : getOnlinePlayers()) {
            channels.addAll(player.getListeningPluginChannels());
        }
        return java.util.Collections.unmodifiableSet(channels);
    }

    @Override
    public @NotNull Iterable<? extends Audience> audiences() {
        return (Collection<? extends Audience>) (Collection<?>) getOnlinePlayers();
    }

    @Override
    public @NotNull File getPluginsFolder() {
        if (!pluginsFolder.exists()) {
            pluginsFolder.mkdirs();
        }
        return this.pluginsFolder;
    }

    @Override
    public @NotNull String getName() {
        return this.serverName;
    }

    @Override
    public @NotNull String getVersion() {
        return "26.2";
    }

    @Override
    public @NotNull String getBukkitVersion() {
        return this.bukkitVersion;
    }

    @Override
    public @NotNull String getMinecraftVersion() {
        String version = this.bukkitVersion;
        if (version != null && version.contains("-")) {
            return version.split("-")[0];
        }
        return version != null && !version.equals("Unknown-Version") ? version : "1.21.4";
    }

    @Override
    public @NotNull Collection<? extends Player> getOnlinePlayers() {
        return Collections.unmodifiableCollection(onlinePlayers.values());
    }

    @Override
    public int getMaxPlayers() {
        return 20;
    }

    @Override
    public void setMaxPlayers(int maxPlayers) {
    }

    @Override
    public int getPort() {
        return 25565;
    }

    @Override
    public int getViewDistance() {
        return 10;
    }

    @Override
    public int getSimulationDistance() {
        return 10;
    }

    @Override
    public @NotNull String getIp() {
        return "127.0.0.1";
    }

    @Override
    public @NotNull String getWorldType() {
        return "DEFAULT";
    }

    @Override
    public boolean getGenerateStructures() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getGenerateStructures'"
        );
    }

    @Override
    public int getMaxWorldSize() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getMaxWorldSize'"
        );
    }

    @Override
    public boolean getAllowEnd() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getAllowEnd'"
        );
    }

    @Override
    public boolean getAllowNether() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getAllowNether'"
        );
    }

    @Override
    public boolean isLoggingIPs() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'isLoggingIPs'"
        );
    }

    @Override
    public @NotNull List<String> getInitialEnabledPacks() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getInitialEnabledPacks'"
        );
    }

    @Override
    public @NotNull List<String> getInitialDisabledPacks() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getInitialDisabledPacks'"
        );
    }

    @Override
    public @NotNull ServerTickManager getServerTickManager() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getServerTickManager'"
        );
    }

    @Override
    public @Nullable ResourcePack getServerResourcePack() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getServerResourcePack'"
        );
    }

    @Override
    public @NotNull String getResourcePack() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getResourcePack'"
        );
    }

    @Override
    public @NotNull String getResourcePackHash() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getResourcePackHash'"
        );
    }

    @Override
    public @NotNull String getResourcePackPrompt() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getResourcePackPrompt'"
        );
    }

    @Override
    public boolean isResourcePackRequired() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'isResourcePackRequired'"
        );
    }

    @Override
    public boolean hasWhitelist() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'hasWhitelist'"
        );
    }

    @Override
    public void setWhitelist(boolean value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'setWhitelist'"
        );
    }

    @Override
    public boolean isWhitelistEnforced() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'isWhitelistEnforced'"
        );
    }

    @Override
    public void setWhitelistEnforced(boolean value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'setWhitelistEnforced'"
        );
    }

    @Override
    public @NotNull Set<OfflinePlayer> getWhitelistedPlayers() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getWhitelistedPlayers'"
        );
    }

    @Override
    public void reloadWhitelist() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'reloadWhitelist'"
        );
    }

    @Override
    public @NotNull String getUpdateFolder() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getUpdateFolder'"
        );
    }

    @Override
    public @NotNull File getUpdateFolderFile() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getUpdateFolderFile'"
        );
    }

    @Override
    public long getConnectionThrottle() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getConnectionThrottle'"
        );
    }

    @Override
    public int getTicksPerSpawns(@NotNull SpawnCategory spawnCategory) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getTicksPerSpawns'"
        );
    }

    @Override
    public @Nullable Player getPlayer(@NotNull String name) {
        return onlinePlayersByName.get(name.toLowerCase());
    }

    @Override
    public @Nullable Player getPlayerExact(@NotNull String name) {
        return onlinePlayersByName.get(name.toLowerCase());
    }

    @Override
    public @NotNull List<Player> matchPlayer(@NotNull String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'matchPlayer'"
        );
    }

   @Override
    public @Nullable Player getPlayer(@NotNull UUID id) {
        return onlinePlayers.get(id);
    }

    @Override
    public @Nullable UUID getPlayerUniqueId(@NotNull String playerName) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getPlayerUniqueId'"
        );
    }

    @Override
    public @NotNull PluginManager getPluginManager() {
        return this.pluginManager;
    }

    @Override
    public @NotNull BukkitScheduler getScheduler() {
        return this.scheduler;
    }

    @Override
    public @NotNull ServicesManager getServicesManager() {
        return this.servicesManager;
    }

    @Override
    public @NotNull List<World> getWorlds() {
        var response = NativeBridgeFfi.getWorlds(patchbukkit.common.EmptyRequest.newBuilder().build());
        if (response == null) return List.of();
        List<World> list = new ArrayList<>();
        for (patchbukkit.common.UUID u : response.getWorldUuidsList()) {
            list.add(PatchBukkitWorld.getOrCreate(u.getValue()));
        }
        return list;
    }

    @Override
    public boolean isTickingWorlds() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'isTickingWorlds'"
        );
    }

    @Override
    public @Nullable World createWorld(@NotNull WorldCreator creator) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createWorld'"
        );
    }

    @Override
    public boolean unloadWorld(@NotNull String name, boolean save) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'unloadWorld'"
        );
    }

    @Override
    public boolean unloadWorld(@NotNull World world, boolean save) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'unloadWorld'"
        );
    }

    public @NotNull World getRespawnWorld() {
        var worlds = getWorlds();
        if (!worlds.isEmpty()) {
            return worlds.get(0);
        }
        throw new UnsupportedOperationException(
            "Unimplemented method 'getRespawnWorld'"
        );
    }

    public void setRespawnWorld(@NotNull World world) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'setRespawnWorld'"
        );
    }

    @Override
    public @Nullable World getWorld(@NotNull String name) {
        for (World world : getWorlds()) {
            if (world.getName().equalsIgnoreCase(name)) {
                return world;
            }
        }
        return null;
    }

    @Override
    public @Nullable World getWorld(@NotNull UUID uid) {
        for (World world : getWorlds()) {
            if (world.getUID().equals(uid)) {
                return world;
            }
        }
        return null;
    }

    @Override
    public @Nullable World getWorld(@NotNull Key worldKey) {
        for (World world : getWorlds()) {
            if (world.getKey().equals(worldKey)) {
                return world;
            }
        }
        return null;
    }

    @Override
    public @NotNull WorldBorder createWorldBorder() {
        return new org.patchbukkit.world.PatchBukkitWorldBorder(null);
    }

    @Override
    public @Nullable MapView getMap(int id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getMap'"
        );
    }

    @Override
    public @NotNull MapView createMap(@NotNull World world) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createMap'"
        );
    }

    @Override
    public @NotNull ItemStack createExplorerMap(
        @NotNull World world,
        @NotNull Location location,
        @NotNull StructureType structureType,
        int radius,
        boolean findUnexplored
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createExplorerMap'"
        );
    }

    @Override
    public @Nullable ItemStack createExplorerMap(
        @NotNull World world,
        @NotNull Location location,
        org.bukkit.generator.structure.@NotNull StructureType structureType,
        @NotNull Type mapIcon,
        int radius,
        boolean findUnexplored
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createExplorerMap'"
        );
    }

    @Override
    public void reload() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'reload'"
        );
    }

    public int getReloadCount() {
        return 0;
    }

    @Override
    public void reloadData() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'reloadData'"
        );
    }

    @Override
    public void updateResources() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'updateResources'"
        );
    }

    @Override
    public void updateRecipes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'updateRecipes'"
        );
    }

    @Override
    public @NotNull Logger getLogger() {
        return this.logger;
    }

    @Override
    public @Nullable PluginCommand getPluginCommand(@NotNull String name) {
        Command cmd = this.commandMap.getCommand(name);
    return (cmd instanceof PluginCommand) ? (PluginCommand) cmd : null;
    }

    @Override
    public void savePlayers() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'savePlayers'"
        );
    }

    @Override
    public boolean dispatchCommand(
        @NotNull CommandSender sender,
        @NotNull String commandLine
    ) throws CommandException {
        if (sender == null) throw new IllegalArgumentException("Sender cannot be null");
        if (commandLine == null) throw new IllegalArgumentException("CommandLine cannot be null");

        return this.commandMap.dispatch(sender, commandLine);
    }

    @Override
    public boolean addRecipe(@Nullable Recipe recipe, boolean resendRecipes) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'addRecipe'"
        );
    }

    @Override
    public @NotNull List<Recipe> getRecipesFor(@NotNull ItemStack result) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getRecipesFor'"
        );
    }

    @Override
    public @Nullable Recipe getRecipe(@NotNull NamespacedKey recipeKey) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getRecipe'"
        );
    }

    @Override
    public @Nullable Recipe getCraftingRecipe(
        @NotNull ItemStack@NotNull [] craftingMatrix,
        @NotNull World world
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getCraftingRecipe'"
        );
    }

    @Override
    public @NotNull ItemCraftResult craftItemResult(
        @NotNull ItemStack@NotNull [] craftingMatrix,
        @NotNull World world,
        @NotNull Player player
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'craftItemResult'"
        );
    }

    @Override
    public @NotNull ItemCraftResult craftItemResult(
        @NotNull ItemStack@NotNull [] craftingMatrix,
        @NotNull World world
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'craftItemResult'"
        );
    }

    @Override
    public @NotNull Iterator<Recipe> recipeIterator() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'recipeIterator'"
        );
    }

    @Override
    public void clearRecipes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'clearRecipes'"
        );
    }

    @Override
    public void resetRecipes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'resetRecipes'"
        );
    }

    @Override
    public boolean removeRecipe(
        @NotNull NamespacedKey key,
        boolean resendRecipes
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'removeRecipe'"
        );
    }

    @Override
    public @NotNull Map<String, String[]> getCommandAliases() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getCommandAliases'"
        );
    }

    @Override
    public int getSpawnRadius() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getSpawnRadius'"
        );
    }

    @Override
    public void setSpawnRadius(int value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'setSpawnRadius'"
        );
    }

    @Override
    public boolean isEnforcingSecureProfiles() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'isEnforcingSecureProfiles'"
        );
    }

    @Override
    public boolean isAcceptingTransfers() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'isAcceptingTransfers'"
        );
    }

    @Override
    public boolean getHideOnlinePlayers() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getHideOnlinePlayers'"
        );
    }

    @Override
    public boolean getOnlineMode() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getOnlineMode'"
        );
    }

    public @NotNull ServerConfiguration getServerConfig() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getServerConfig'"
        );
    }

    @Override
    public boolean getAllowFlight() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getAllowFlight'"
        );
    }

    @Override
    public boolean isHardcore() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'isHardcore'"
        );
    }

    @Override
    public void shutdown() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'shutdown'"
        );
    }

    @Override
    public int broadcast(
        @NotNull Component message,
        @NotNull String permission
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'broadcast'"
        );
    }

    @Override
    public @NotNull OfflinePlayer getOfflinePlayer(@NotNull String name) {
        Player online = getPlayer(name);
        if (online != null) return online;
        return new org.patchbukkit.entity.PatchBukkitOfflinePlayer(null, name);
    }

    @Override
    public @Nullable OfflinePlayer getOfflinePlayerIfCached(@NotNull String name) {
        Player online = getPlayer(name);
        if (online != null) return online;
        return new org.patchbukkit.entity.PatchBukkitOfflinePlayer(null, name);
    }

    @Override
    public @NotNull OfflinePlayer getOfflinePlayer(@NotNull UUID id) {
        Player online = getPlayer(id);
        if (online != null) return online;
        return new org.patchbukkit.entity.PatchBukkitOfflinePlayer(id, null);
    }

    @Override
    public @NotNull PlayerProfile createPlayerProfile(
        @Nullable UUID uniqueId,
        @Nullable String name
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createPlayerProfile'"
        );
    }

    @Override
    public @NotNull PlayerProfile createPlayerProfile(@NotNull UUID uniqueId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createPlayerProfile'"
        );
    }

    @Override
    public @NotNull PlayerProfile createPlayerProfile(@NotNull String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createPlayerProfile'"
        );
    }

    @Override
    public @NotNull Set<String> getIPBans() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getIPBans'"
        );
    }

    @Override
    public void banIP(@NotNull String address) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'banIP'");
    }

    @Override
    public void unbanIP(@NotNull String address) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'unbanIP'"
        );
    }

    @Override
    public void banIP(@NotNull InetAddress address) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'banIP'");
    }

    @Override
    public void unbanIP(@NotNull InetAddress address) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'unbanIP'"
        );
    }

    @Override
    public @NotNull Set<OfflinePlayer> getBannedPlayers() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getBannedPlayers'"
        );
    }

    @Override
    public <T extends BanList<?>> @NotNull T getBanList(
        org.bukkit.BanList.@NotNull Type type
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getBanList'"
        );
    }

    @Override
    public <B extends BanList<E>, E> @NotNull B getBanList(
        @NotNull BanListType<B> type
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getBanList'"
        );
    }

    @Override
    public @NotNull Set<OfflinePlayer> getOperators() {
        Set<OfflinePlayer> ops = new java.util.HashSet<>();
        for (UUID uuid : operatorUuids) {
            ops.add(getOfflinePlayer(uuid));
        }
        for (String name : operatorNames) {
            ops.add(getOfflinePlayer(name));
        }
        return ops;
    }

    @Override
    public @NotNull GameMode getDefaultGameMode() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getDefaultGameMode'"
        );
    }

    @Override
    public void setDefaultGameMode(@NotNull GameMode mode) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'setDefaultGameMode'"
        );
    }

    public boolean forcesDefaultGameMode() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'forcesDefaultGameMode'"
        );
    }

    @Override
    public @NotNull ConsoleCommandSender getConsoleSender() {
        return new PatchBukkitConsoleCommandSender();
    }

    @Override
    public @NotNull CommandSender createCommandSender(
        @NotNull Consumer<? super Component> feedback
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createCommandSender'"
        );
    }

    @Override
    public @NotNull File getWorldContainer() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getWorldContainer'"
        );
    }

    @Override
    public @NotNull OfflinePlayer@NotNull [] getOfflinePlayers() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getOfflinePlayers'"
        );
    }

    @Override
    public @NotNull Messenger getMessenger() {
        return this.messenger;
    }

    @Override
    public @NotNull HelpMap getHelpMap() {
        return this.helpMap;
    }

    @Override
    public @NotNull Inventory createInventory(
        @Nullable InventoryHolder owner,
        @NotNull InventoryType type
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createInventory'"
        );
    }

    @Override
    public @NotNull Inventory createInventory(
        @Nullable InventoryHolder owner,
        @NotNull InventoryType type,
        @NotNull Component title
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createInventory'"
        );
    }

    @Override
    public @NotNull Inventory createInventory(
        @Nullable InventoryHolder owner,
        @NotNull InventoryType type,
        @NotNull String title
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createInventory'"
        );
    }

    @Override
    public @NotNull Inventory createInventory(
        @Nullable InventoryHolder owner,
        int size
    ) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createInventory'"
        );
    }

    @Override
    public @NotNull Inventory createInventory(
        @Nullable InventoryHolder owner,
        int size,
        @NotNull Component title
    ) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createInventory'"
        );
    }

    @Override
    public @NotNull Inventory createInventory(
        @Nullable InventoryHolder owner,
        int size,
        @NotNull String title
    ) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createInventory'"
        );
    }

    @Override
    public @NotNull Merchant createMerchant(@Nullable Component title) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createMerchant'"
        );
    }

    @Override
    public @NotNull Merchant createMerchant(@Nullable String title) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createMerchant'"
        );
    }

    @Override
    public int getMaxChainedNeighborUpdates() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getMaxChainedNeighborUpdates'"
        );
    }

    @Override
    public @NotNull Merchant createMerchant() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createMerchant'"
        );
    }

    @Override
    public int getSpawnLimit(@NotNull SpawnCategory spawnCategory) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getSpawnLimit'"
        );
    }

    @Override
    public boolean isPrimaryThread() {
        return ((PatchBukkitScheduler) scheduler).isPrimaryThread();
    }

    @Override
    public @NotNull Component motd() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'motd'");
    }

    @Override
    public void motd(@NotNull Component motd) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'motd'");
    }

    @Override
    public @Nullable Component shutdownMessage() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'shutdownMessage'"
        );
    }

    @Override
    public @NotNull String getMotd() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getMotd'"
        );
    }

    @Override
    public void setMotd(@NotNull String motd) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'setMotd'"
        );
    }

    @Override
    public @NotNull ServerLinks getServerLinks() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getServerLinks'"
        );
    }

    @Override
    public @Nullable String getShutdownMessage() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getShutdownMessage'"
        );
    }

    @Override
    public @NotNull WarningState getWarningState() {
        return WarningState.DEFAULT;
    }

    @Override
    public @NotNull ItemFactory getItemFactory() {
        return org.patchbukkit.inventory.PatchBukkitItemFactory.INSTANCE;
    }

    @Override
    public @NotNull EntityFactory getEntityFactory() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getEntityFactory'"
        );
    }

    @Override
    public @NotNull ScoreboardManager getScoreboardManager() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getScoreboardManager'"
        );
    }

    @Override
    public @NotNull Criteria getScoreboardCriteria(@NotNull String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getScoreboardCriteria'"
        );
    }

    @Override
    public @Nullable CachedServerIcon getServerIcon() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getServerIcon'"
        );
    }

    @Override
    public @NotNull CachedServerIcon loadServerIcon(@NotNull File file)
        throws IllegalArgumentException, Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'loadServerIcon'"
        );
    }

    @Override
    public @NotNull CachedServerIcon loadServerIcon(
        @NotNull BufferedImage image
    ) throws IllegalArgumentException, Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'loadServerIcon'"
        );
    }

    @Override
    public void setIdleTimeout(int threshold) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'setIdleTimeout'"
        );
    }

    @Override
    public int getIdleTimeout() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getIdleTimeout'"
        );
    }

    @Override
    public int getPauseWhenEmptyTime() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getPauseWhenEmptyTime'"
        );
    }

    @Override
    public void setPauseWhenEmptyTime(int seconds) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'setPauseWhenEmptyTime'"
        );
    }

    @Override
    public @NotNull ChunkData createChunkData(@NotNull World world) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createChunkData'"
        );
    }

    @Override
    public @NotNull BossBar createBossBar(
        @Nullable String title,
        @NotNull BarColor color,
        @NotNull BarStyle style,
        @NotNull BarFlag... flags
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createBossBar'"
        );
    }

    @Override
    public @NotNull KeyedBossBar createBossBar(
        @NotNull NamespacedKey key,
        @Nullable String title,
        @NotNull BarColor color,
        @NotNull BarStyle style,
        @NotNull BarFlag... flags
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createBossBar'"
        );
    }

    @Override
    public @NotNull Iterator<KeyedBossBar> getBossBars() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getBossBars'"
        );
    }

    @Override
    public @Nullable KeyedBossBar getBossBar(@NotNull NamespacedKey key) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getBossBar'"
        );
    }

    @Override
    public boolean removeBossBar(@NotNull NamespacedKey key) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'removeBossBar'"
        );
    }

    @Override
    public @Nullable Entity getEntity(@NotNull UUID uuid) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getEntity'"
        );
    }

    @Override
    public double@NotNull [] getTPS() {
        return new double[] { 20.0, 20.0, 20.0 };
    }

    @Override
    public long@NotNull [] getTickTimes() {
        return new long[100];
    }

    @Override
    public double getAverageTickTime() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getAverageTickTime'"
        );
    }

    @Override
    public @NotNull CommandMap getCommandMap() {
        return commandMap;
    }

    @Override
    public @Nullable Advancement getAdvancement(@NotNull NamespacedKey key) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getAdvancement'"
        );
    }

    @Override
    public @NotNull Iterator<Advancement> advancementIterator() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'advancementIterator'"
        );
    }

    @Override
    public @NotNull BlockData createBlockData(@NotNull Material material) {
        Preconditions.checkArgument(material != null, "Material cannot be null");
        return this.createBlockData(material, (String) null);
    }

    @Override
    public @NotNull BlockData createBlockData(
        @NotNull Material material,
        @Nullable Consumer<? super BlockData> consumer
    ) {
        BlockData data = this.createBlockData(material);

        if (consumer != null) {
            consumer.accept(data);
        }

        return data;
    }

    @Override
    public @NotNull BlockData createBlockData(@NotNull String data) {
        Preconditions.checkArgument(data != null, "data cannot be null");

        return this.createBlockData(null, data);
    }

    @Override
    public @NotNull BlockData createBlockData(
        @Nullable Material material,
        @Nullable String data
    ) throws IllegalArgumentException {
        Preconditions.checkArgument(material != null || data != null, "Must provide one of material or data");
        BlockType type = null;
        if (material != null) {
            type = material.asBlockType();
            if (type == null && material.isBlock()) {
                type = org.patchbukkit.registry.PatchBukkitBlockType.create(material);
            }
            Preconditions.checkArgument(type != null, "Provided material must be a block");
        } else if (data != null) {
            String matName = data.trim();
            int stateIndex = matName.indexOf('[');
            if (stateIndex != -1) {
                matName = matName.substring(0, stateIndex).trim();
            }
            if (matName.startsWith("minecraft:")) {
                matName = matName.substring("minecraft:".length());
            }
            Material matched = null;
            try {
                matched = Material.valueOf(matName.toUpperCase(java.util.Locale.ROOT));
            } catch (Throwable ignored) {}

            if (matched == null || matched.isLegacy()) {
                Material m = Material.matchMaterial(matName, false);
                if (m != null && !m.isLegacy()) {
                    matched = m;
                }
            }

            if (matched == null) {
                matched = Material.matchMaterial(matName);
            }

            if (matched != null && matched.isLegacy()) {
                matched = PatchBukkitLegacy.fromLegacy(matched);
            }

            if (matched != null && (matched.isBlock() || (!matched.isLegacy() && matched.getKey() != null))) {
                material = matched;
                type = matched.asBlockType();
                if (type == null) {
                    type = org.patchbukkit.registry.PatchBukkitBlockType.create(matched);
                }
            } else {
                throw new IllegalArgumentException("Block name " + matName + " was not recognized");
            }
        }

        return PatchBukkitBlockData.newData(material, type, data);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Keyed> @Nullable Tag<T> getTag(
        @NotNull String registry,
        @NotNull NamespacedKey tag,
        @NotNull Class<T> clazz
    ) {
        if (registry == null || tag == null || clazz == null) {
            return null;
        }
        try {
            for (java.lang.reflect.Field field : org.bukkit.Tag.class.getFields()) {
                if (Tag.class.isAssignableFrom(field.getType())) {
                    Tag<?> val = (Tag<?>) field.get(null);
                    if (val != null && val.getKey().equals(tag)) {
                        return (Tag<T>) val;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return new org.patchbukkit.tag.PatchBukkitTag<>(tag);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Keyed> @NotNull Iterable<Tag<T>> getTags(
        @NotNull String registry,
        @NotNull Class<T> clazz
    ) {
        List<Tag<T>> result = new ArrayList<>();
        try {
            for (java.lang.reflect.Field field : org.bukkit.Tag.class.getFields()) {
                if (Tag.class.isAssignableFrom(field.getType())) {
                    Tag<?> val = (Tag<?>) field.get(null);
                    if (val != null) {
                        result.add((Tag<T>) val);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return result;
    }

    @Override
    public @Nullable LootTable getLootTable(@NotNull NamespacedKey key) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getLootTable'"
        );
    }

    @Override
    public @NotNull List<Entity> selectEntities(
        @NotNull CommandSender sender,
        @NotNull String selector
    ) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'selectEntities'"
        );
    }

    @Override
    public @NotNull StructureManager getStructureManager() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getStructureManager'"
        );
    }

    @Override
    public <T extends Keyed> @Nullable Registry<T> getRegistry(
        @NotNull Class<T> tClass
    ) {
        return io.papermc.paper.registry.RegistryAccess.registryAccess().getRegistry(tClass);
    }

    @Override
    public @NotNull UnsafeValues getUnsafe() {
        return PatchBukkitUnsafeValues.INSTANCE;
    }

    @Override
    public @NotNull Spigot spigot() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'spigot'"
        );
    }

    @Override
    public void restart() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'restart'"
        );
    }

    @Override
    public void reloadPermissions() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'reloadPermissions'"
        );
    }

    @Override
    public boolean reloadCommandAliases() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'reloadCommandAliases'"
        );
    }

    @Override
    public boolean suggestPlayerNamesWhenNullTabCompletions() {
        return true;
    }

    @Override
    public @NotNull String getPermissionMessage() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getPermissionMessage'"
        );
    }

    @Override
    public @NotNull Component permissionMessage() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'permissionMessage'"
        );
    }

    @Override
    public com.destroystokyo.paper.profile.@NotNull PlayerProfile createProfile(
        @NotNull UUID uuid
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createProfile'"
        );
    }

    @Override
    public com.destroystokyo.paper.profile.@NotNull PlayerProfile createProfile(
        @NotNull String name
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createProfile'"
        );
    }

    @Override
    public com.destroystokyo.paper.profile.@NotNull PlayerProfile createProfile(
        @Nullable UUID uuid,
        @Nullable String name
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createProfile'"
        );
    }

    @Override
    public com.destroystokyo.paper.profile.@NotNull PlayerProfile createProfileExact(
        @Nullable UUID uuid,
        @Nullable String name
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'createProfileExact'"
        );
    }

    @Override
    public int getCurrentTick() {
        return (int) (System.currentTimeMillis() / 50);
    }

    @Override
    public boolean isStopping() {
        return false;
    }

    @Override
    public @NotNull MobGoals getMobGoals() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getMobGoals'"
        );
    }

    @Override
    public @NotNull DatapackManager getDatapackManager() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getDatapackManager'"
        );
    }

    @Override
    public @NotNull PotionBrewer getPotionBrewer() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getPotionBrewer'"
        );
    }

    @Override
    public @NotNull RegionScheduler getRegionScheduler() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getRegionScheduler'"
        );
    }

    @Override
    public @NotNull AsyncScheduler getAsyncScheduler() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getAsyncScheduler'"
        );
    }

    @Override
    public @NotNull GlobalRegionScheduler getGlobalRegionScheduler() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getGlobalRegionScheduler'"
        );
    }

    @Override
    public boolean isOwnedByCurrentRegion(
        @NotNull World world,
        @NotNull Position position
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'isOwnedByCurrentRegion'"
        );
    }

    @Override
    public boolean isOwnedByCurrentRegion(
        @NotNull World world,
        @NotNull Position position,
        int squareRadiusChunks
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'isOwnedByCurrentRegion'"
        );
    }

    @Override
    public boolean isOwnedByCurrentRegion(@NotNull Location location) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'isOwnedByCurrentRegion'"
        );
    }

    @Override
    public boolean isOwnedByCurrentRegion(
        @NotNull Location location,
        int squareRadiusChunks
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'isOwnedByCurrentRegion'"
        );
    }

    @Override
    public boolean isOwnedByCurrentRegion(
        @NotNull World world,
        int chunkX,
        int chunkZ
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'isOwnedByCurrentRegion'"
        );
    }

    @Override
    public boolean isOwnedByCurrentRegion(
        @NotNull World world,
        int chunkX,
        int chunkZ,
        int squareRadiusChunks
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'isOwnedByCurrentRegion'"
        );
    }

    @Override
    public boolean isOwnedByCurrentRegion(
        @NotNull World world,
        int minChunkX,
        int minChunkZ,
        int maxChunkX,
        int maxChunkZ
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'isOwnedByCurrentRegion'"
        );
    }

    @Override
    public boolean isOwnedByCurrentRegion(@NotNull Entity entity) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'isOwnedByCurrentRegion'"
        );
    }

    @Override
    public boolean isGlobalTickThread() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'isGlobalTickThread'"
        );
    }

    @Override
    public boolean isPaused() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'isPaused'"
        );
    }

    @Override
    public void allowPausing(@NotNull Plugin plugin, boolean value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'allowPausing'"
        );
    }

    public @NotNull Path getLevelDirectory() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getLevelDirectory'");
    }

    @Override
    public int getAmbientSpawnLimit() {
        return 15;
    }

    @Override
    public int getWaterUndergroundCreatureSpawnLimit() {
        return 5;
    }

    @Override
    public int getWaterAmbientSpawnLimit() {
        return 20;
    }

    @Override
    public int getWaterAnimalSpawnLimit() {
        return 5;
    }
}
