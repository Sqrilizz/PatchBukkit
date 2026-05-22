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
import patchbukkit.events.Event;
import patchbukkit.events.FireEventResponse;
import patchbukkit.events.PlayerChatEvent;
import patchbukkit.events.PlayerJoinEvent;
import patchbukkit.events.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;

import java.util.logging.Logger;

public class PatchBukkitEventFactory {
    private static final Logger LOGGER = Logger.getLogger("PatchBukkit");

    @Nullable
    public static org.bukkit.event.Event createEventFromBytes(byte[] data) {
        try {
            Event event = Event.parseFrom(data);
            return createEvent(event);
        } catch (InvalidProtocolBufferException e) {
            LOGGER.severe("Failed to parse Event: " + e.getMessage());
            return null;
        }
    }

    @Nullable
    public static org.bukkit.event.Event createEvent(@NotNull Event event) {
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

                Component quitMessage = GsonComponentSerializer.gson().deserialize(quitEvent.getQuitMessage());
                yield new org.bukkit.event.player.PlayerQuitEvent(player, quitMessage);
            }
            case PLAYER_CHAT -> {
                PlayerChatEvent chatEvent = event.getPlayerChat();
                Player player = getPlayer(chatEvent.getPlayerUuid().getValue());
                if (player == null) yield null;

                Set<Player> recipients = new HashSet<>();
                for (UUID recipientUuid : chatEvent.getRecipientsList()) {
                    Player recipient = getPlayer(recipientUuid.getValue());
                    if (recipient != null) {
                        recipients.add(recipient);
                    }
                }

                yield new org.bukkit.event.player.PlayerChatEvent(player, chatEvent.getMessage(), chatEvent.getFormat(), recipients);
            }
            case DATA_NOT_SET -> {
                LOGGER.warning("EventFactory: Received Event with no data");
                yield null;
            }
        };
    }

    @NotNull
    public static byte[] toFireEventResponse(@NotNull org.bukkit.event.Event event) {
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
            String quitMessage = GsonComponentSerializer.gson().serialize(quitEvent.quitMessage());
            eventBuilder.setPlayerQuit(
                PlayerQuitEvent.newBuilder()
                    .setPlayerUuid(UUID.newBuilder()
                        .setValue(quitEvent.getPlayer().getUniqueId().toString())
                        .build())
                    .setQuitMessage(quitMessage)
                    .build()
            );
        } else if (event instanceof org.bukkit.event.player.PlayerChatEvent chatEvent) {
            PlayerChatEvent.Builder chatBuilder = PlayerChatEvent.newBuilder()
                .setPlayerUuid(UUID.newBuilder()
                    .setValue(chatEvent.getPlayer().getUniqueId().toString())
                    .build())
                .setMessage(chatEvent.getMessage())
                .setFormat(chatEvent.getFormat());

            for (Player recipient : chatEvent.getRecipients()) {
                chatBuilder.addRecipients(
                    UUID.newBuilder()
                        .setValue(recipient.getUniqueId().toString())
                        .build()
                );
            }
            eventBuilder.setPlayerChat(chatBuilder.build());
        }

        builder.setData(eventBuilder.build());

        return builder.build().toByteArray();
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
                LOGGER.warning("EventFactory: Player not found for UUID " + uuidStr);
            }
            return player;
        } catch (IllegalArgumentException e) {
            LOGGER.severe("EventFactory: Invalid UUID string: " + uuidStr);
            return null;
        }
    }
}
