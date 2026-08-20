package org.patchbukkit.events;

import com.google.protobuf.InvalidProtocolBufferException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import patchbukkit.common.UUID;
import patchbukkit.events.BlockBreakEvent;
import patchbukkit.events.BlockDamageEvent;
import patchbukkit.events.BlockPlaceEvent;
import patchbukkit.events.Event;
import patchbukkit.events.FireEventResponse;
import patchbukkit.events.PlayerChatEvent;
import patchbukkit.events.PlayerGameModeChangeEvent;
import patchbukkit.events.PlayerInteractEvent;
import patchbukkit.events.PlayerJoinEvent;
import patchbukkit.events.PlayerInteractEntityEvent;
import patchbukkit.events.PlayerMoveEvent;
import patchbukkit.events.PlayerQuitEvent;
import patchbukkit.events.PlayerResourcePackStatusEvent;
import patchbukkit.events.PlayerToggleFlightEvent;
import patchbukkit.events.PlayerToggleSneakEvent;
import patchbukkit.events.PlayerToggleSprintEvent;
import patchbukkit.events.PluginDisableEvent;
import patchbukkit.events.PluginEnableEvent;
import patchbukkit.events.BlockFadeEvent;
import patchbukkit.events.BlockFormEvent;
import patchbukkit.events.BlockGrowEvent;
import patchbukkit.events.BlockIgniteEvent;
import patchbukkit.events.ServerCommandEvent;
import patchbukkit.events.SignChangeEvent;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PatchBukkitEventFactory {
    private static final Logger LOGGER = Logger.getLogger("PatchBukkit");

    @Nullable
    public static org.bukkit.event.Event createEventFromBytes(byte[] data) {
        try {
            Event event = Event.parseFrom(data);
            return createEvent(event);
        } catch (InvalidProtocolBufferException e) {
            LOGGER.log(Level.SEVERE, "Failed to parse Event", e);
            return null;
        }
    }

    public static byte[] fireEventFromBytes(byte[] data, String pluginName) {
        org.bukkit.event.Event event = createEventFromBytes(data);
        if (event == null) {
            return FireEventResponse.newBuilder().setCancelled(false).build().toByteArray();
        }
        if (Bukkit.getServer() instanceof org.patchbukkit.PatchBukkitServer server) {
            server.getEventManager().fireEvent(event, pluginName);
        }
        return toFireEventResponse(event);
    }

    @Nullable
    public static org.bukkit.event.Event createEvent(@NotNull Event event) {
        try {
            Event.DataCase dataCase = event.getDataCase();

            return switch (dataCase) {
            case PLAYER_JOIN -> {
                PlayerJoinEvent joinEvent = event.getPlayerJoin();
                Player player = getPlayer(joinEvent.getPlayerUuid().getValue());
                if (player == null) yield null;

                Component joinMessage = GsonComponentSerializer.gson().deserialize(joinEvent.getJoinMessage());
                yield new org.bukkit.event.player.PlayerJoinEvent(player, joinMessage);
            }
            case PLAYER_QUIT -> {
                PlayerQuitEvent quitEvent = event.getPlayerQuit();
                Player player = getPlayer(quitEvent.getPlayerUuid().getValue());
                if (player == null) yield null;

                Component quitMessage = quitEvent.getQuitMessage().isEmpty() ? Component.empty() : GsonComponentSerializer.gson().deserialize(quitEvent.getQuitMessage());
                yield new org.bukkit.event.player.PlayerQuitEvent(player, quitMessage);
            }
            case PLAYER_GAMEMODE_CHANGE -> {
                PlayerGameModeChangeEvent gmEvent = event.getPlayerGamemodeChange();
                Player player = getPlayer(gmEvent.getPlayerUuid().getValue());
                if (player == null) yield null;

                org.bukkit.GameMode newGamemode;
                try {
                    newGamemode = org.bukkit.GameMode.valueOf(gmEvent.getNewGamemode().toUpperCase());
                } catch (IllegalArgumentException e) {
                    newGamemode = org.bukkit.GameMode.SURVIVAL;
                }
                yield new org.bukkit.event.player.PlayerGameModeChangeEvent(player, newGamemode);
            }
            case PLAYER_INTERACT -> {
                PlayerInteractEvent interactEvent = event.getPlayerInteract();
                Player player = getPlayer(interactEvent.getPlayerUuid().getValue());
                if (player == null) yield null;

                org.bukkit.event.block.Action action;
                try {
                    action = org.bukkit.event.block.Action.valueOf(interactEvent.getAction());
                } catch (IllegalArgumentException e) {
                    action = org.bukkit.event.block.Action.RIGHT_CLICK_AIR;
                }

                org.bukkit.inventory.EquipmentSlot hand;
                try {
                    hand = org.bukkit.inventory.EquipmentSlot.valueOf(interactEvent.getHand());
                } catch (IllegalArgumentException e) {
                    hand = org.bukkit.inventory.EquipmentSlot.HAND;
                }

                org.bukkit.block.Block clickedBlock = player.getWorld().getBlockAt(
                    interactEvent.getClickedX(),
                    interactEvent.getClickedY(),
                    interactEvent.getClickedZ()
                );

                yield new org.bukkit.event.player.PlayerInteractEvent(
                    player,
                    action,
                    player.getInventory().getItemInMainHand(),
                    clickedBlock,
                    org.bukkit.block.BlockFace.SELF,
                    hand
                );
            }
            case PLUGIN_ENABLE -> {
                PluginEnableEvent enableEvent = event.getPluginEnable();
                org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin(enableEvent.getPluginName());
                if (plugin == null) yield null;
                yield new org.bukkit.event.server.PluginEnableEvent(plugin);
            }
            case PLUGIN_DISABLE -> {
                PluginDisableEvent disableEvent = event.getPluginDisable();
                org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin(disableEvent.getPluginName());
                if (plugin == null) yield null;
                yield new org.bukkit.event.server.PluginDisableEvent(plugin);
            }
            case BLOCK_BREAK -> {
                BlockBreakEvent breakEvent = event.getBlockBreak();
                Player player = breakEvent.hasPlayerUuid() ? getPlayer(breakEvent.getPlayerUuid().getValue()) : null;
                if (player == null) yield null;
                org.bukkit.World world = player.getWorld();
                org.bukkit.block.Block block = world.getBlockAt(breakEvent.getBlockX(), breakEvent.getBlockY(), breakEvent.getBlockZ());
                yield new org.bukkit.event.block.BlockBreakEvent(block, player);
            }
            case BLOCK_PLACE -> {
                BlockPlaceEvent placeEvent = event.getBlockPlace();
                Player player = getPlayer(placeEvent.getPlayerUuid().getValue());
                if (player == null) yield null;
                org.bukkit.block.Block block = player.getWorld().getBlockAt(placeEvent.getBlockX(), placeEvent.getBlockY(), placeEvent.getBlockZ());
                org.bukkit.block.BlockState blockState = block.getState();
                yield new org.bukkit.event.block.BlockPlaceEvent(block, blockState, block, player.getInventory().getItemInMainHand(), player, placeEvent.getCanBuild(), org.bukkit.inventory.EquipmentSlot.HAND);
            }
            case SIGN_CHANGE -> {
                SignChangeEvent signEvent = event.getSignChange();
                Player player = getPlayer(signEvent.getPlayerUuid().getValue());
                if (player == null) yield null;
                org.bukkit.block.Block block = player.getWorld().getBlockAt(
                    signEvent.getBlockX(),
                    signEvent.getBlockY(),
                    signEvent.getBlockZ()
                );
                String[] lines = signEvent.getLinesList().toArray(new String[0]);
                yield new org.bukkit.event.block.SignChangeEvent(block, player, lines);
            }
            case BLOCK_DAMAGE -> {
                BlockDamageEvent damageEvent = event.getBlockDamage();
                Player player = getPlayer(damageEvent.getPlayerUuid().getValue());
                if (player == null) yield null;
                org.bukkit.block.Block block = player.getWorld().getBlockAt(
                    damageEvent.getBlockX(),
                    damageEvent.getBlockY(),
                    damageEvent.getBlockZ()
                );
                yield new org.bukkit.event.block.BlockDamageEvent(
                    player, block, player.getInventory().getItemInMainHand(), damageEvent.getInstaBreak()
                );
            }
            case SERVER_COMMAND -> {
                ServerCommandEvent cmdEvent = event.getServerCommand();
                yield new org.bukkit.event.server.ServerCommandEvent(
                    Bukkit.getConsoleSender(),
                    cmdEvent.getCommand()
                );
            }
            case BLOCK_IGNITE -> {
                BlockIgniteEvent igniteEvent = event.getBlockIgnite();
                Player player = igniteEvent.hasPlayerUuid() ? getPlayer(igniteEvent.getPlayerUuid().getValue()) : null;
                org.bukkit.World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
                if (world == null) yield null;
                org.bukkit.block.Block block = world.getBlockAt(igniteEvent.getBlockX(), igniteEvent.getBlockY(), igniteEvent.getBlockZ());
                org.bukkit.event.block.BlockIgniteEvent.IgniteCause cause = org.bukkit.event.block.BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL;
                yield new org.bukkit.event.block.BlockIgniteEvent(block, cause, player);
            }
            case BLOCK_GROW -> {
                BlockGrowEvent growEvent = event.getBlockGrow();
                org.bukkit.World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
                if (world == null) yield null;
                org.bukkit.block.Block block = world.getBlockAt(growEvent.getBlockX(), growEvent.getBlockY(), growEvent.getBlockZ());
                yield new org.bukkit.event.block.BlockGrowEvent(block, block.getState());
            }
            case BLOCK_FORM -> {
                BlockFormEvent formEvent = event.getBlockForm();
                org.bukkit.World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
                if (world == null) yield null;
                org.bukkit.block.Block block = world.getBlockAt(formEvent.getBlockX(), formEvent.getBlockY(), formEvent.getBlockZ());
                yield new org.bukkit.event.block.BlockFormEvent(block, block.getState());
            }
            case BLOCK_FADE -> {
                BlockFadeEvent fadeEvent = event.getBlockFade();
                org.bukkit.World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
                if (world == null) yield null;
                org.bukkit.block.Block block = world.getBlockAt(fadeEvent.getBlockX(), fadeEvent.getBlockY(), fadeEvent.getBlockZ());
                yield new org.bukkit.event.block.BlockFadeEvent(block, block.getState());
            }
            case PLAYER_TOGGLE_SNEAK -> {
                PlayerToggleSneakEvent sneakEvent = event.getPlayerToggleSneak();
                Player player = getPlayer(sneakEvent.getPlayerUuid().getValue());
                if (player == null) yield null;
                yield new org.bukkit.event.player.PlayerToggleSneakEvent(player, sneakEvent.getIsSneaking());
            }
            case PLAYER_TOGGLE_SPRINT -> {
                PlayerToggleSprintEvent sprintEvent = event.getPlayerToggleSprint();
                Player player = getPlayer(sprintEvent.getPlayerUuid().getValue());
                if (player == null) yield null;
                yield new org.bukkit.event.player.PlayerToggleSprintEvent(player, sprintEvent.getIsSprinting());
            }
            case PLAYER_TOGGLE_FLIGHT -> {
                PlayerToggleFlightEvent flightEvent = event.getPlayerToggleFlight();
                Player player = getPlayer(flightEvent.getPlayerUuid().getValue());
                if (player == null) yield null;
                yield new org.bukkit.event.player.PlayerToggleFlightEvent(player, flightEvent.getIsFlying());
            }
            case PLAYER_MOVE -> {
                PlayerMoveEvent moveEvent = event.getPlayerMove();
                Player player = getPlayer(moveEvent.getPlayerUuid().getValue());
                if (player == null) yield null;
                org.bukkit.Location from = org.patchbukkit.bridge.BridgeUtils.convertLocation(moveEvent.getFrom());
                org.bukkit.Location to = org.patchbukkit.bridge.BridgeUtils.convertLocation(moveEvent.getTo());
                yield new org.bukkit.event.player.PlayerMoveEvent(player, from, to);
            }
            case PLAYER_INTERACT_ENTITY -> {
                PlayerInteractEntityEvent interactEvent = event.getPlayerInteractEntity();
                Player player = getPlayer(interactEvent.getPlayerUuid().getValue());
                if (player == null) yield null;
                java.util.UUID targetUuid = org.patchbukkit.bridge.BridgeUtils.convertUuid(interactEvent.getTargetUuid());
                org.bukkit.entity.Entity target = Bukkit.getPlayer(targetUuid);
                if (target == null) target = Bukkit.getEntity(targetUuid);
                if (target == null) yield null;
                if ("ATTACK".equals(interactEvent.getAction())) {
                    yield new org.bukkit.event.entity.EntityDamageByEntityEvent(
                        player, target, org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK, 1.0
                    );
                } else {
                    yield new org.bukkit.event.player.PlayerInteractEntityEvent(player, target);
                }
            }
            case PLAYER_CHAT -> {
                PlayerChatEvent chatEvent = event.getPlayerChat();
                Player player = getPlayer(chatEvent.getPlayerUuid().getValue());
                if (player == null) yield null;
                yield new org.bukkit.event.player.AsyncPlayerChatEvent(true, player, chatEvent.getMessage(), new java.util.HashSet<>(Bukkit.getOnlinePlayers()));
            }
            case PLAYER_RESOURCE_PACK_STATUS -> {
                PlayerResourcePackStatusEvent packEvent = event.getPlayerResourcePackStatus();
                Player player = getPlayer(packEvent.getPlayerUuid().getValue());
                if (player == null) yield null;
                org.bukkit.event.player.PlayerResourcePackStatusEvent.Status status = org.bukkit.event.player.PlayerResourcePackStatusEvent.Status.valueOf(packEvent.getStatus());
                if (player instanceof org.patchbukkit.entity.PatchBukkitPlayer patchPlayer) patchPlayer.setResourcePackStatus(status);
                yield new org.bukkit.event.player.PlayerResourcePackStatusEvent(player, org.patchbukkit.bridge.BridgeUtils.convertUuid(packEvent.getPackUuid()), status);
            }
            case DATA_NOT_SET -> {
                LOGGER.warning("EventFactory: Received Event with no data");
                yield null;
            }
            default -> null;
        };
        } catch (Throwable t) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Exception in createEvent for " + event.getDataCase() + ": " + t.getMessage(), t);
            t.printStackTrace();
            return null;
        }
    }

    @NotNull
    public static byte[] toFireEventResponse(@NotNull org.bukkit.event.Event event) {
        try {
            boolean cancelled = event instanceof org.bukkit.event.Cancellable c && c.isCancelled();

            FireEventResponse.Builder builder = FireEventResponse.newBuilder()
                .setCancelled(cancelled);

            Event.Builder eventBuilder = Event.newBuilder();

        if (event instanceof org.bukkit.event.player.PlayerJoinEvent joinEvent) {
            String joinMessage = GsonComponentSerializer.gson().serialize(joinEvent.joinMessage());

            eventBuilder.setPlayerJoin(
                PlayerJoinEvent.newBuilder()
                    .setPlayerUuid(UUID.newBuilder()
                        .setValue(joinEvent.getPlayer().getUniqueId().toString())
                        .build())
                    .setJoinMessage(joinMessage)
                    .build()
            );
        } else if (event instanceof org.bukkit.event.player.PlayerQuitEvent quitEvent) {
            String quitMessage = quitEvent.quitMessage() != null ? GsonComponentSerializer.gson().serialize(quitEvent.quitMessage()) : "";

            eventBuilder.setPlayerQuit(
                PlayerQuitEvent.newBuilder()
                    .setPlayerUuid(UUID.newBuilder()
                        .setValue(quitEvent.getPlayer().getUniqueId().toString())
                        .build())
                    .setQuitMessage(quitMessage)
                    .build()
            );
        } else if (event instanceof org.bukkit.event.player.PlayerGameModeChangeEvent gmEvent) {
            eventBuilder.setPlayerGamemodeChange(
                PlayerGameModeChangeEvent.newBuilder()
                    .setPlayerUuid(UUID.newBuilder()
                        .setValue(gmEvent.getPlayer().getUniqueId().toString())
                        .build())
                    .setNewGamemode(gmEvent.getNewGameMode().name())
                    .build()
            );
        } else if (event instanceof org.bukkit.event.player.PlayerInteractEvent interactEvent) {
            var block = interactEvent.getClickedBlock();
            int x = block != null ? block.getX() : 0;
            int y = block != null ? block.getY() : 0;
            int z = block != null ? block.getZ() : 0;

            eventBuilder.setPlayerInteract(
                PlayerInteractEvent.newBuilder()
                    .setPlayerUuid(UUID.newBuilder()
                        .setValue(interactEvent.getPlayer().getUniqueId().toString())
                        .build())
                    .setAction(interactEvent.getAction().name())
                    .setClickedX(x)
                    .setClickedY(y)
                    .setClickedZ(z)
                    .setBlockFace(interactEvent.getBlockFace().name())
                    .setHand(interactEvent.getHand() != null ? interactEvent.getHand().name() : "HAND")
                    .build()
            );
        } else if (event instanceof org.bukkit.event.server.PluginEnableEvent enableEvent) {
            eventBuilder.setPluginEnable(
                PluginEnableEvent.newBuilder()
                    .setPluginName(enableEvent.getPlugin().getName())
                    .build()
            );
        } else if (event instanceof org.bukkit.event.server.PluginDisableEvent disableEvent) {
            eventBuilder.setPluginDisable(
                PluginDisableEvent.newBuilder()
                    .setPluginName(disableEvent.getPlugin().getName())
                    .build()
            );
        } else if (event instanceof org.bukkit.event.block.BlockBreakEvent breakEvent) {
            var b = breakEvent.getBlock();
            var bBuilder = BlockBreakEvent.newBuilder()
                .setBlockX(b.getX())
                .setBlockY(b.getY())
                .setBlockZ(b.getZ())
                .setBlockType(b.getType().name())
                .setExp(breakEvent.getExpToDrop())
                .setDropItems(breakEvent.isDropItems());
            if (breakEvent.getPlayer() != null) {
                bBuilder.setPlayerUuid(UUID.newBuilder().setValue(breakEvent.getPlayer().getUniqueId().toString()).build());
            }
            eventBuilder.setBlockBreak(bBuilder.build());
        } else if (event instanceof org.bukkit.event.block.BlockPlaceEvent placeEvent) {
            var b = placeEvent.getBlockPlaced();
            var against = placeEvent.getBlockAgainst();
            eventBuilder.setBlockPlace(
                BlockPlaceEvent.newBuilder()
                    .setPlayerUuid(UUID.newBuilder().setValue(placeEvent.getPlayer().getUniqueId().toString()).build())
                    .setBlockX(b.getX())
                    .setBlockY(b.getY())
                    .setBlockZ(b.getZ())
                    .setBlockPlacedType(b.getType().name())
                    .setBlockAgainstType(against.getType().name())
                    .setCanBuild(placeEvent.canBuild())
                    .build()
            );
        } else if (event instanceof org.bukkit.event.player.PlayerToggleSneakEvent sneakEvent) {
            eventBuilder.setPlayerToggleSneak(
                PlayerToggleSneakEvent.newBuilder()
                    .setPlayerUuid(UUID.newBuilder().setValue(sneakEvent.getPlayer().getUniqueId().toString()).build())
                    .setIsSneaking(sneakEvent.isSneaking())
                    .build()
            );
        } else if (event instanceof org.bukkit.event.player.PlayerToggleSprintEvent sprintEvent) {
            eventBuilder.setPlayerToggleSprint(
                PlayerToggleSprintEvent.newBuilder()
                    .setPlayerUuid(UUID.newBuilder().setValue(sprintEvent.getPlayer().getUniqueId().toString()).build())
                    .setIsSprinting(sprintEvent.isSprinting())
                    .build()
            );
        } else if (event instanceof org.bukkit.event.player.PlayerToggleFlightEvent flightEvent) {
            eventBuilder.setPlayerToggleFlight(
                PlayerToggleFlightEvent.newBuilder()
                    .setPlayerUuid(UUID.newBuilder().setValue(flightEvent.getPlayer().getUniqueId().toString()).build())
                    .setIsFlying(flightEvent.isFlying())
                    .build()
            );
        } else if (event instanceof org.bukkit.event.player.PlayerMoveEvent moveEvent) {
            eventBuilder.setPlayerMove(
                PlayerMoveEvent.newBuilder()
                    .setPlayerUuid(UUID.newBuilder().setValue(moveEvent.getPlayer().getUniqueId().toString()).build())
                    .setFrom(org.patchbukkit.bridge.BridgeUtils.convertLocation(moveEvent.getFrom()))
                    .setTo(org.patchbukkit.bridge.BridgeUtils.convertLocation(moveEvent.getTo()))
                    .build()
            );
        } else if (event instanceof org.bukkit.event.player.AsyncPlayerChatEvent chatEvent) {
            eventBuilder.setPlayerChat(
                PlayerChatEvent.newBuilder()
                    .setPlayerUuid(UUID.newBuilder().setValue(chatEvent.getPlayer().getUniqueId().toString()).build())
                    .setMessage(chatEvent.getMessage())
                    .setFormat(chatEvent.getFormat())
                    .build()
            );
        } else if (event instanceof org.bukkit.event.player.PlayerResourcePackStatusEvent packEvent) {
            eventBuilder.setPlayerResourcePackStatus(
                PlayerResourcePackStatusEvent.newBuilder()
                    .setPlayerUuid(UUID.newBuilder().setValue(packEvent.getPlayer().getUniqueId().toString()).build())
                    .setPackUuid(UUID.newBuilder().setValue(packEvent.getID().toString()).build())
                    .setStatus(packEvent.getStatus().name())
                    .build()
            );
        }

        builder.setData(eventBuilder.build());

        return builder.build().toByteArray();
        } catch (Throwable t) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Exception serializing event response for " + event.getEventName() + ": " + t.getMessage(), t);
            t.printStackTrace();
            return FireEventResponse.newBuilder().setCancelled(false).build().toByteArray();
        }
    }

    public static boolean isCancellable(@NotNull org.bukkit.event.Event event) {
        return event instanceof org.bukkit.event.Cancellable;
    }

    @Nullable
    private static Player getPlayer(@NotNull String uuidStr) {
        try {
            java.util.UUID uuid = java.util.UUID.fromString(uuidStr);
            Player player = Bukkit.getServer().getPlayer(uuid);
            if (player == null) {
                player = new org.patchbukkit.entity.PatchBukkitPlayer(uuid, "Player");
                if (Bukkit.getServer() instanceof org.patchbukkit.PatchBukkitServer server) {
                    server.registerPlayer(player);
                }
            }
            return player;
        } catch (IllegalArgumentException e) {
            LOGGER.severe("EventFactory: Invalid UUID string: " + uuidStr);
            return null;
        }
    }
}
