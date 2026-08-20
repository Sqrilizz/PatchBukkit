package org.patchbukkit.entity;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.nio.charset.StandardCharsets;

import org.bukkit.BanEntry;
import org.bukkit.Chunk;
import org.bukkit.DyeColor;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.GameMode;
import org.bukkit.Input;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Note;
import org.bukkit.Particle;
import org.bukkit.ServerLinks;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Statistic;
import org.bukkit.WeatherType;
import org.bukkit.WorldBorder;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent.Cause;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;
import org.patchbukkit.bridge.BridgeUtils;
import org.patchbukkit.registry.PatchBukkitSound;

import com.destroystokyo.paper.ClientOption;
import com.destroystokyo.paper.Title;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.common.base.Preconditions;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.entity.LookAnchor;
import io.papermc.paper.entity.PlayerGiveResult;
import io.papermc.paper.math.Position;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.util.TriState;
import net.md_5.bungee.api.chat.BaseComponent;
import patchbukkit.bridge.NativeBridgeFfi;
import patchbukkit.message.SendMessageRequest;
import patchbukkit.abilities.SetAbilitiesRequest;
import patchbukkit.sound.PlayerEntityPlaySoundRequest;
import patchbukkit.sound.PlayerPlaySoundRequest;
import patchbukkit.player.ClearPlayerTitleRequest;
import patchbukkit.player.PlayerTitleMode;
import patchbukkit.player.RemoveResourcePackRequest;
import patchbukkit.player.SendPlayerTitleRequest;
import patchbukkit.player.SendResourcePackRequest;
import patchbukkit.player.SetPlayerTitleTimesRequest;

@SuppressWarnings({ "deprecation", "removal", "unchecked" })
public class PatchBukkitPlayer
    extends PatchBukkitHumanEntity
    implements Player {

    private final Map<UUID, Set<WeakReference<Plugin>>> invertedVisibilityEntities = new HashMap<>();
    private volatile Status resourcePackStatus;

    public PatchBukkitPlayer(UUID uuid, String name) {
        super(uuid, name);
    }

    public void setResourcePackStatus(Status status) {
        resourcePackStatus = status;
    }

    private void sendTitleComponent(Component component, PlayerTitleMode mode) {
        sendTitleJson(GsonComponentSerializer.gson().serialize(component), mode);
    }

    private void sendTitleJson(String componentJson, PlayerTitleMode mode) {
        NativeBridgeFfi.sendPlayerTitle(SendPlayerTitleRequest.newBuilder().setPlayerUuid(BridgeUtils.convertUuid(uuid)).setComponentJson(componentJson).setMode(mode).build());
    }

    private void clearTitle(boolean reset) {
        NativeBridgeFfi.clearPlayerTitle(ClearPlayerTitleRequest.newBuilder().setPlayerUuid(BridgeUtils.convertUuid(uuid)).setReset(reset).build());
    }

    private void sendResourcePack(UUID id, String url, byte[] hash, Component prompt, boolean force) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(url);
        if (hash != null && hash.length != 20) throw new IllegalArgumentException("Resource pack hash must be 20 bytes");
        NativeBridgeFfi.sendResourcePack(SendResourcePackRequest.newBuilder().setPlayerUuid(BridgeUtils.convertUuid(uuid)).setPackUuid(BridgeUtils.convertUuid(id)).setUrl(url).setHash(hash == null ? "" : HexFormat.of().formatHex(hash)).setPromptJson(prompt == null ? "" : GsonComponentSerializer.gson().serialize(prompt)).setForce(force).build());
    }

    @Override
    public void openSign(@NotNull org.bukkit.block.Sign sign) {
    }

    @Override
    public <T> void spawnParticle(@NotNull Particle particle, @NotNull Location location, int count, double offsetX, double offsetY, double offsetZ, double extra, @org.jspecify.annotations.Nullable T data, boolean force) {
    }

    @Override
    public <T> void spawnParticle(@NotNull Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra, @org.jspecify.annotations.Nullable T data) {
    }

    public void unsetFixedPose() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'unsetFixedPose'");
    }

    public void resetFlyingTicks() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'resetFlyingTicks'");
    }

    @Override
    public void sendRawMessage(String message) {
        this.sendRawMessage(null, message);
    }

    @Override
    public void sendRawMessage(UUID sender, String message) {
        var request = SendMessageRequest.newBuilder().setMessage(message != null ? message : "").setUuid(BridgeUtils.convertUuid(this.getUniqueId())).build();
        NativeBridgeFfi.sendMessage(request);
    }

    @Override
    public void sendMessage(String message) {
        this.sendRawMessage(message);
    }

    @Override
    public void sendMessage(String... messages) {
        for (String message : messages) {
            this.sendMessage(message);
        }
    }

    @Override
    public void sendMessage(UUID sender, String message) {
        this.sendRawMessage(sender, message);
    }

    @Override
    public void sendMessage(UUID sender, String... messages) {
        for (String message : messages) {
            this.sendMessage(sender, message);
        }
    }

    @Override
    public Player.Spigot spigot() {
        throw new UnsupportedOperationException("Unimplemented method 'spigot'");
    }

    @Override
    public boolean isConversing() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isConversing'");
    }

    @Override
    public void acceptConversationInput(@NotNull String input) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'acceptConversationInput'");
    }

    @Override
    public boolean beginConversation(@NotNull Conversation conversation) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'beginConversation'");
    }

    @Override
    public void abandonConversation(@NotNull Conversation conversation) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'abandonConversation'");
    }

    @Override
    public void abandonConversation(@NotNull Conversation conversation, @NotNull ConversationAbandonedEvent details) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'abandonConversation'");
    }

    @Override
    public boolean isOnline() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isOnline'");
    }

    @Override
    public boolean isConnected() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isConnected'");
    }

    @Override
    public boolean isBanned() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isBanned'");
    }

    @Override
    public <E extends BanEntry<? super PlayerProfile>> @org.jspecify.annotations.Nullable E ban(
            @org.jspecify.annotations.Nullable String reason, @org.jspecify.annotations.Nullable Date expires,
            @org.jspecify.annotations.Nullable String source) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'ban'");
    }

    @Override
    public <E extends BanEntry<? super PlayerProfile>> @org.jspecify.annotations.Nullable E ban(
            @org.jspecify.annotations.Nullable String reason, @org.jspecify.annotations.Nullable Instant expires,
            @org.jspecify.annotations.Nullable String source) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'ban'");
    }

    @Override
    public <E extends BanEntry<? super PlayerProfile>> @org.jspecify.annotations.Nullable E ban(
            @org.jspecify.annotations.Nullable String reason, @org.jspecify.annotations.Nullable Duration duration,
            @org.jspecify.annotations.Nullable String source) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'ban'");
    }

    @Override
    public boolean isWhitelisted() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isWhitelisted'");
    }

    @Override
    public void setWhitelisted(boolean value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setWhitelisted'");
    }

    @Override
    public @org.jspecify.annotations.Nullable Player getPlayer() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPlayer'");
    }

    @Override
    public long getFirstPlayed() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFirstPlayed'");
    }

    @Override
    public long getLastPlayed() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getLastPlayed'");
    }

    @Override
    public boolean hasPlayedBefore() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'hasPlayedBefore'");
    }

    @Override
    public long getLastLogin() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getLastLogin'");
    }

    @Override
    public long getLastSeen() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getLastSeen'");
    }

    public @org.jspecify.annotations.Nullable Location getRespawnLocation(boolean loadLocationAndValidate) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRespawnLocation'");
    }

    @Override
    public void incrementStatistic(Statistic statistic) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'incrementStatistic'");
    }

    @Override
    public void decrementStatistic(Statistic statistic) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'decrementStatistic'");
    }

    @Override
    public void incrementStatistic(Statistic statistic, int amount) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'incrementStatistic'");
    }

    @Override
    public void decrementStatistic(Statistic statistic, int amount) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'decrementStatistic'");
    }

    @Override
    public void setStatistic(Statistic statistic, int newValue) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStatistic'");
    }

    @Override
    public int getStatistic(Statistic statistic) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStatistic'");
    }

    @Override
    public void incrementStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'incrementStatistic'");
    }

    @Override
    public void decrementStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'decrementStatistic'");
    }

    @Override
    public int getStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStatistic'");
    }

    @Override
    public void incrementStatistic(Statistic statistic, Material material, int amount) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'incrementStatistic'");
    }

    @Override
    public void decrementStatistic(Statistic statistic, Material material, int amount) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'decrementStatistic'");
    }

    @Override
    public void setStatistic(Statistic statistic, Material material, int newValue) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStatistic'");
    }

    @Override
    public void incrementStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'incrementStatistic'");
    }

    @Override
    public void decrementStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'decrementStatistic'");
    }

    @Override
    public int getStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStatistic'");
    }

    @Override
    public void incrementStatistic(Statistic statistic, EntityType entityType, int amount)
            throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'incrementStatistic'");
    }

    @Override
    public void decrementStatistic(Statistic statistic, EntityType entityType, int amount) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'decrementStatistic'");
    }

    @Override
    public void setStatistic(Statistic statistic, EntityType entityType, int newValue) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStatistic'");
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'serialize'");
    }

    @Override
    public void sendPluginMessage(@NotNull Plugin source, @NotNull String channel, byte @NotNull [] message) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendPluginMessage'");
    }

    @Override
    public @NotNull Set<String> getListeningPluginChannels() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getListeningPluginChannels'");
    }

    @Override
    public int getProtocolVersion() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getProtocolVersion'");
    }

    @Override
    public @org.jspecify.annotations.Nullable InetSocketAddress getVirtualHost() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getVirtualHost'");
    }

    @Override
    public @UnmodifiableView Iterable<? extends BossBar> activeBossBars() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'activeBossBars'");
    }

    @Override
    public Component displayName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'displayName'");
    }

    @Override
    public void displayName(@org.jspecify.annotations.Nullable Component displayName) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'displayName'");
    }

    @Override
    public String getDisplayName() {
        return this.getName();
    }

    @Override
    public void setDisplayName(@org.jspecify.annotations.Nullable String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setDisplayName'");
    }

    @Override
    public void playerListName(@org.jspecify.annotations.Nullable Component name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'playerListName'");
    }

    @Override
    public Component playerListName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'playerListName'");
    }

    @Override
    public @org.jspecify.annotations.Nullable Component playerListHeader() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'playerListHeader'");
    }

    @Override
    public @org.jspecify.annotations.Nullable Component playerListFooter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'playerListFooter'");
    }

    @Override
    public String getPlayerListName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPlayerListName'");
    }

    @Override
    public void setPlayerListName(@org.jspecify.annotations.Nullable String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPlayerListName'");
    }

    @Override
    public int getPlayerListOrder() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPlayerListOrder'");
    }

    @Override
    public void setPlayerListOrder(int order) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPlayerListOrder'");
    }

    @Override
    public @org.jspecify.annotations.Nullable String getPlayerListHeader() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPlayerListHeader'");
    }

    @Override
    public @org.jspecify.annotations.Nullable String getPlayerListFooter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPlayerListFooter'");
    }

    @Override
    public void setPlayerListHeader(@org.jspecify.annotations.Nullable String header) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPlayerListHeader'");
    }

    @Override
    public void setPlayerListFooter(@org.jspecify.annotations.Nullable String footer) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPlayerListFooter'");
    }

    @Override
    public void setPlayerListHeaderFooter(@org.jspecify.annotations.Nullable String header,
            @org.jspecify.annotations.Nullable String footer) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPlayerListHeaderFooter'");
    }

    @Override
    public void setCompassTarget(Location loc) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setCompassTarget'");
    }

    @Override
    public Location getCompassTarget() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCompassTarget'");
    }

    @Override
    public @org.jspecify.annotations.Nullable InetSocketAddress getAddress() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAddress'");
    }

    @Override
    public @org.jspecify.annotations.Nullable InetSocketAddress getHAProxyAddress() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getHAProxyAddress'");
    }

    @Override
    public boolean isTransferred() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isTransferred'");
    }

    @Override
    public CompletableFuture<byte @org.jspecify.annotations.Nullable []> retrieveCookie(NamespacedKey key) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'retrieveCookie'");
    }

    @Override
    public void storeCookie(NamespacedKey key, byte[] value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'storeCookie'");
    }

    @Override
    public void transfer(String host, int port) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'transfer'");
    }

    @Override
    public void kickPlayer(@org.jspecify.annotations.Nullable String message) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'kickPlayer'");
    }

    @Override
    public void kick(@org.jspecify.annotations.Nullable Component message, Cause cause) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'kick'");
    }

    @Override
    public <E extends BanEntry<? super PlayerProfile>> @org.jspecify.annotations.Nullable E ban(
            @org.jspecify.annotations.Nullable String reason, @org.jspecify.annotations.Nullable Date expires,
            @org.jspecify.annotations.Nullable String source, boolean kickPlayer) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'ban'");
    }

    @Override
    public <E extends BanEntry<? super PlayerProfile>> @org.jspecify.annotations.Nullable E ban(
            @org.jspecify.annotations.Nullable String reason, @org.jspecify.annotations.Nullable Instant expires,
            @org.jspecify.annotations.Nullable String source, boolean kickPlayer) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'ban'");
    }

    @Override
    public <E extends BanEntry<? super PlayerProfile>> @org.jspecify.annotations.Nullable E ban(
            @org.jspecify.annotations.Nullable String reason, @org.jspecify.annotations.Nullable Duration duration,
            @org.jspecify.annotations.Nullable String source, boolean kickPlayer) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'ban'");
    }

    @Override
    public @org.jspecify.annotations.Nullable BanEntry<InetAddress> banIp(
            @org.jspecify.annotations.Nullable String reason, @org.jspecify.annotations.Nullable Date expires,
            @org.jspecify.annotations.Nullable String source, boolean kickPlayer) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'banIp'");
    }

    @Override
    public @org.jspecify.annotations.Nullable BanEntry<InetAddress> banIp(
            @org.jspecify.annotations.Nullable String reason, @org.jspecify.annotations.Nullable Instant expires,
            @org.jspecify.annotations.Nullable String source, boolean kickPlayer) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'banIp'");
    }

    @Override
    public @org.jspecify.annotations.Nullable BanEntry<InetAddress> banIp(
            @org.jspecify.annotations.Nullable String reason, @org.jspecify.annotations.Nullable Duration duration,
            @org.jspecify.annotations.Nullable String source, boolean kickPlayer) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'banIp'");
    }

    @Override
    public void chat(String msg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'chat'");
    }

    @Override
    public boolean performCommand(String command) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'performCommand'");
    }

    private boolean sprinting = false;

    @Override
    public boolean isSprinting() {
        return this.sprinting;
    }

    @Override
    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    @Override
    public void saveData() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'saveData'");
    }

    @Override
    public void loadData() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'loadData'");
    }

    @Override
    public void setSleepingIgnored(boolean isSleeping) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSleepingIgnored'");
    }

    @Override
    public boolean isSleepingIgnored() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isSleepingIgnored'");
    }

    @Override
    public void setRespawnLocation(@org.jspecify.annotations.Nullable Location location, boolean force) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setRespawnLocation'");
    }

    @Override
    public Collection<EnderPearl> getEnderPearls() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getEnderPearls'");
    }

    @Override
    public Input getCurrentInput() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCurrentInput'");
    }

    @Override
    public void playNote(Location loc, Instrument instrument, Note note) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'playNote'");
    }

    @Override
    public void playSound(Location location, Sound sound, float volume, float pitch) {
        playSound(location, sound, SoundCategory.MASTER, volume, pitch);
    }

    @Override
    public void playSound(Location location, String sound, float volume, float pitch) {
        playSound(location, sound, SoundCategory.MASTER, volume, pitch);
    }

    @Override
    public void playSound(Location location, Sound sound, SoundCategory category, float volume, float pitch) {
        var patchBukkitSound = (PatchBukkitSound) sound;
        this.playSound0(location, patchBukkitSound.getOriginalName(), category, volume, pitch, OptionalLong.empty());
    }

    @Override
    public void playSound(Location location, String sound, SoundCategory category, float volume, float pitch) {
        this.playSound0(location, sound, category, volume, pitch, OptionalLong.empty());
    }

    @Override
    public void playSound(Location location, Sound sound, SoundCategory category, float volume, float pitch,
            long seed) {
        var patchBukkitSound = (PatchBukkitSound) sound;
        this.playSound0(location, patchBukkitSound.getOriginalName(), category, volume, pitch, OptionalLong.of(seed));
    }

    @Override
    public void playSound(Location location, String sound, SoundCategory category, float volume, float pitch,
            long seed) {
        this.playSound0(location, sound, category, volume, pitch, OptionalLong.of(seed));
    }
    
    private void playSound0(Location location, String sound, SoundCategory category, float volume, float pitch, OptionalLong seed) {
        var request = PlayerPlaySoundRequest.newBuilder()
            .setPlayerUuid(BridgeUtils.convertUuid(this.uuid))
            .setLocation(
                patchbukkit.common.Location.newBuilder()
                    .setPosition(
                        patchbukkit.common.Vec3.newBuilder()
                            .setX(location.x())
                            .setY(location.y())
                            .setZ(location.z())
                    ).setWorld(
                        patchbukkit.common.World.newBuilder().setUuid(BridgeUtils.convertUuid(location.getWorld().getUID()))
                    ).setPitch(location.getPitch())
                    .setYaw(location.getYaw())
            ).setSound(
                patchbukkit.sound.Sound.newBuilder()
                    .setCategory(category.name())
                    .setName(sound)
            ).setVolume(volume)
            .setPitch(pitch);

        if (seed.isPresent()) request.setSeed(seed.getAsLong());
        NativeBridgeFfi.playerPlaySound(request.build());
    }

    @Override
    public void playSound(Entity entity, Sound sound, SoundCategory category, float volume, float pitch) {
        var patchBukkitSound = (PatchBukkitSound) sound;
        this.playSound0(entity, patchBukkitSound.getOriginalName(), category, volume, pitch, OptionalLong.empty());
    }

    @Override
    public void playSound(Entity entity, String sound, SoundCategory category, float volume, float pitch) {
        this.playSound0(entity, sound, category, volume, pitch, OptionalLong.empty());
    }

    @Override
    public void playSound(Entity entity, Sound sound, SoundCategory category, float volume, float pitch, long seed) {
        var patchBukkitSound = (PatchBukkitSound) sound;
        this.playSound0(entity, patchBukkitSound.getOriginalName(), category, volume, pitch, OptionalLong.of(seed));
    }

    @Override
    public void playSound(Entity entity, String sound, SoundCategory category, float volume, float pitch, long seed) {
        this.playSound0(entity, sound, category, volume, pitch, OptionalLong.of(seed));
    }

    private void playSound0(Entity entity, String sound, SoundCategory category, float volume, float pitch, OptionalLong seed) {
       var request = PlayerEntityPlaySoundRequest.newBuilder()
           .setPlayerUuid(BridgeUtils.convertUuid(this.uuid))
           .setEntityUuid(BridgeUtils.convertUuid(entity.getUniqueId()))
            .setSound(
                patchbukkit.sound.Sound.newBuilder()
                    .setCategory(category.name())
                    .setName(sound)
            ).setVolume(volume)
            .setPitch(pitch);

        if (seed.isPresent()) request.setSeed(seed.getAsLong());
        NativeBridgeFfi.playerEntityPlaySound(request.build());
    }

    @Override
    public void stopSound(String sound, @org.jspecify.annotations.Nullable SoundCategory category) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'stopSound'");
    }

    @Override
    public void stopSound(SoundCategory category) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'stopSound'");
    }

    @Override
    public void stopAllSounds() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'stopAllSounds'");
    }

    @Override
    public void playEffect(Location loc, Effect effect, int data) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'playEffect'");
    }

    @Override
    public <T> void playEffect(Location loc, Effect effect, @org.jspecify.annotations.Nullable T data) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'playEffect'");
    }

    @Override
    public boolean breakBlock(Block block) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'breakBlock'");
    }

    @Override
    public void sendBlockChange(Location loc, Material material, byte data) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendBlockChange'");
    }

    @Override
    public void sendBlockChange(Location loc, BlockData block) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendBlockChange'");
    }

    @Override
    public void sendBlockChanges(Collection<BlockState> blocks) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendBlockChanges'");
    }

    @Override
    public void sendMultiBlockChange(Map<? extends Position, BlockData> blockChanges) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendMultiBlockChange'");
    }

    @Override
    public void sendBlockDamage(Location loc, float progress, Entity source) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendBlockDamage'");
    }

    @Override
    public void sendBlockDamage(Location loc, float progress, int sourceId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendBlockDamage'");
    }

    @Override
    public void sendEquipmentChange(LivingEntity entity, EquipmentSlot slot,
            @org.jspecify.annotations.Nullable ItemStack item) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendEquipmentChange'");
    }

    @Override
    public void sendEquipmentChange(LivingEntity entity,
            Map<EquipmentSlot, @org.jspecify.annotations.Nullable ItemStack> items) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendEquipmentChange'");
    }

    @Override
    public void sendSignChange(Location loc, @org.jspecify.annotations.Nullable List<? extends Component> lines,
            DyeColor dyeColor, boolean hasGlowingText) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendSignChange'");
    }

    @Override
    public void sendSignChange(Location loc,
            @org.jspecify.annotations.Nullable String @org.jspecify.annotations.Nullable [] lines)
            throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendSignChange'");
    }

    @Override
    public void sendSignChange(Location loc,
            @org.jspecify.annotations.Nullable String @org.jspecify.annotations.Nullable [] lines, DyeColor dyeColor)
            throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendSignChange'");
    }

    @Override
    public void sendSignChange(Location loc,
            @org.jspecify.annotations.Nullable String @org.jspecify.annotations.Nullable [] lines, DyeColor dyeColor,
            boolean hasGlowingText) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendSignChange'");
    }

    @Override
    public void sendBlockUpdate(Location loc, TileState tileState) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendBlockUpdate'");
    }

    @Override
    public void sendPotionEffectChange(LivingEntity entity, PotionEffect effect) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendPotionEffectChange'");
    }

    @Override
    public void sendPotionEffectChangeRemove(LivingEntity entity, PotionEffectType type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendPotionEffectChangeRemove'");
    }

    @Override
    public void sendMap(MapView map) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendMap'");
    }

    @Override
    public void showWinScreen() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'showWinScreen'");
    }

    @Override
    public boolean hasSeenWinScreen() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'hasSeenWinScreen'");
    }

    @Override
    public void setHasSeenWinScreen(boolean hasSeenWinScreen) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setHasSeenWinScreen'");
    }

    @Override
    public void sendActionBar(String message) {
        sendTitleComponent(LegacyComponentSerializer.legacySection().deserialize(message), PlayerTitleMode.PLAYER_TITLE_MODE_ACTION_BAR);
    }

    @Override
    public void sendActionBar(char alternateChar, String message) {
        sendTitleComponent(LegacyComponentSerializer.legacy(alternateChar).deserialize(message), PlayerTitleMode.PLAYER_TITLE_MODE_ACTION_BAR);
    }

    @Override
    public void sendActionBar(BaseComponent... message) {
        sendTitleJson(net.md_5.bungee.chat.ComponentSerializer.toString(message), PlayerTitleMode.PLAYER_TITLE_MODE_ACTION_BAR);
    }

    @Override
    public void setPlayerListHeaderFooter(BaseComponent @org.jspecify.annotations.Nullable [] header,
            BaseComponent @org.jspecify.annotations.Nullable [] footer) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPlayerListHeaderFooter'");
    }

    @Override
    public void setPlayerListHeaderFooter(@org.jspecify.annotations.Nullable BaseComponent header,
            @org.jspecify.annotations.Nullable BaseComponent footer) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPlayerListHeaderFooter'");
    }

    @Override
    public void setTitleTimes(int fadeInTicks, int stayTicks, int fadeOutTicks) {
        NativeBridgeFfi.setPlayerTitleTimes(SetPlayerTitleTimesRequest.newBuilder().setPlayerUuid(BridgeUtils.convertUuid(uuid)).setFadeIn(fadeInTicks).setStay(stayTicks).setFadeOut(fadeOutTicks).build());
    }

    @Override
    public void setSubtitle(BaseComponent[] subtitle) {
        sendTitleJson(net.md_5.bungee.chat.ComponentSerializer.toString(subtitle), PlayerTitleMode.PLAYER_TITLE_MODE_SUBTITLE);
    }

    @Override
    public void setSubtitle(BaseComponent subtitle) {
        setSubtitle(new BaseComponent[] { subtitle });
    }

    @Override
    public void showTitle(@org.jspecify.annotations.Nullable BaseComponent[] title) {
        if (title != null) sendTitleJson(net.md_5.bungee.chat.ComponentSerializer.toString(title), PlayerTitleMode.PLAYER_TITLE_MODE_TITLE);
    }

    @Override
    public void showTitle(@org.jspecify.annotations.Nullable BaseComponent title) {
        if (title != null) showTitle(new BaseComponent[] { title });
    }

    @Override
    public void showTitle(@org.jspecify.annotations.Nullable BaseComponent[] title,
            @org.jspecify.annotations.Nullable BaseComponent[] subtitle, int fadeInTicks, int stayTicks,
            int fadeOutTicks) {
        setTitleTimes(fadeInTicks, stayTicks, fadeOutTicks);
        showTitle(title);
        if (subtitle != null) setSubtitle(subtitle);
    }

    @Override
    public void showTitle(@org.jspecify.annotations.Nullable BaseComponent title,
            @org.jspecify.annotations.Nullable BaseComponent subtitle, int fadeInTicks, int stayTicks,
            int fadeOutTicks) {
        showTitle(title == null ? null : new BaseComponent[] { title }, subtitle == null ? null : new BaseComponent[] { subtitle }, fadeInTicks, stayTicks, fadeOutTicks);
    }

    @Override
    public void sendTitle(Title title) {
        showTitle(title.getTitle(), title.getSubtitle(), title.getFadeIn(), title.getStay(), title.getFadeOut());
    }

    @Override
    public void updateTitle(Title title) {
        sendTitle(title);
    }

    @Override
    public void hideTitle() {
        clearTitle(false);
    }

    @Override
    public void sendHurtAnimation(float yaw) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendHurtAnimation'");
    }

    @Override
    public void sendLinks(ServerLinks links) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendLinks'");
    }

    @Override
    public void addCustomChatCompletions(Collection<String> completions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addCustomChatCompletions'");
    }

    @Override
    public void removeCustomChatCompletions(Collection<String> completions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeCustomChatCompletions'");
    }

    @Override
    public void setCustomChatCompletions(Collection<String> completions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setCustomChatCompletions'");
    }

    @Override
    public void updateInventory() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateInventory'");
    }

    @Override
    public @org.jspecify.annotations.Nullable GameMode getPreviousGameMode() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPreviousGameMode'");
    }

    @Override
    public void setPlayerTime(long time, boolean relative) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPlayerTime'");
    }

    @Override
    public long getPlayerTime() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPlayerTime'");
    }

    @Override
    public long getPlayerTimeOffset() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPlayerTimeOffset'");
    }

    @Override
    public boolean isPlayerTimeRelative() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isPlayerTimeRelative'");
    }

    @Override
    public void resetPlayerTime() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'resetPlayerTime'");
    }

    @Override
    public void setPlayerWeather(WeatherType type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPlayerWeather'");
    }

    @Override
    public @org.jspecify.annotations.Nullable WeatherType getPlayerWeather() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPlayerWeather'");
    }

    @Override
    public void resetPlayerWeather() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'resetPlayerWeather'");
    }

    @Override
    public int getExpCooldown() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getExpCooldown'");
    }

    @Override
    public void setExpCooldown(int ticks) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setExpCooldown'");
    }

    @Override
    public void giveExp(int amount, boolean applyMending) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'giveExp'");
    }

    @Override
    public int applyMending(int amount) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'applyMending'");
    }

    @Override
    public void giveExpLevels(int amount) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'giveExpLevels'");
    }

    @Override
    public float getExp() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getExp'");
    }

    @Override
    public void setExp(float exp) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setExp'");
    }

    @Override
    public int getLevel() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getLevel'");
    }

    @Override
    public void setLevel(int level) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setLevel'");
    }

    @Override
    public int getTotalExperience() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTotalExperience'");
    }

    @Override
    public void setTotalExperience(int exp) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTotalExperience'");
    }

    @Override
    public @Range(from = 0, to = 2147483647) int calculateTotalExperiencePoints() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'calculateTotalExperiencePoints'");
    }

    @Override
    public void setExperienceLevelAndProgress(@Range(from = 0, to = 2147483647) int totalExperience) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setExperienceLevelAndProgress'");
    }

    @Override
    public int getExperiencePointsNeededForNextLevel() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getExperiencePointsNeededForNextLevel'");
    }

    @Override
    public void sendExperienceChange(float progress) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendExperienceChange'");
    }

    @Override
    public void sendExperienceChange(float progress, int level) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendExperienceChange'");
    }

    @Override
    public boolean getAllowFlight() {
        var abilities = NativeBridgeFfi.getAbilities(BridgeUtils.convertUuid(this.uuid));
        return abilities != null && abilities.getAllowFlying();
    }

    @Override
    public void setAllowFlight(boolean flight) {
        var playerUuid = BridgeUtils.convertUuid(this.uuid);
        var abilities = NativeBridgeFfi.getAbilities(playerUuid);
        var builder = (abilities != null ? abilities.toBuilder() : patchbukkit.abilities.Abilities.newBuilder());
        builder.setAllowFlying(flight);
        if (builder.getFlying()) builder.setFlying(false);
        NativeBridgeFfi.setAbilities(SetAbilitiesRequest.newBuilder().setAbilities(builder.build()).setUuid(playerUuid).build());
    }

    @Override
    public void setFlyingFallDamage(TriState flyingFallDamage) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setFlyingFallDamage'");
    }

    @Override
    public TriState hasFlyingFallDamage() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'hasFlyingFallDamage'");
    }

    @Override
    public void hidePlayer(Player player) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'hidePlayer'");
    }

    @Override
    public void showPlayer(Player player) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'showPlayer'");
    }

    @Override
    public boolean canSee(Player player) {
        return this.canSee((org.bukkit.entity.Entity) player);
    }

    @Override
    public boolean canSee(Entity entity) {
        return this.equals(entity) || entity.isVisibleByDefault() ^ this.invertedVisibilityEntities.containsKey(entity.getUniqueId());
    }

    @Override
    public void hideEntity(Plugin plugin, Entity entity) {
        Preconditions.checkNotNull(plugin, "Plugin cannot be null");
        Preconditions.checkNotNull(entity, "Entity cannot be null");
        if (this.equals(entity)) return;

        Set<WeakReference<Plugin>> plugins = invertedVisibilityEntities
            .computeIfAbsent(entity.getUniqueId(), k -> new HashSet<>());
        plugins.add(new WeakReference<>(plugin));
    }

    @Override
    public void showEntity(Plugin plugin, Entity entity) {
        Preconditions.checkNotNull(plugin, "Plugin cannot be null");
        Preconditions.checkNotNull(entity, "Entity cannot be null");
        if (this.equals(entity)) return;

        Set<WeakReference<Plugin>> plugins = invertedVisibilityEntities.get(entity.getUniqueId());
        if (plugins == null) return;

        plugins.removeIf(ref -> {
            Plugin p = ref.get();
            return p == null || p.equals(plugin);
        });

        if (plugins.isEmpty()) {
            invertedVisibilityEntities.remove(entity.getUniqueId());
        }
    }

    @Override
    public boolean isListed(Player other) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isListed'");
    }

    @Override
    public boolean unlistPlayer(Player other) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'unlistPlayer'");
    }

    @Override
    public boolean listPlayer(Player other) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listPlayer'");
    }

    @Override
    public boolean isFlying() {
        var abilities = NativeBridgeFfi.getAbilities(BridgeUtils.convertUuid(this.uuid));
        return abilities != null && abilities.getFlying();
    }

    @Override
    public void setFlying(boolean value) {
        var playerUuid = BridgeUtils.convertUuid(this.getUniqueId());
        var abilities = NativeBridgeFfi.getAbilities(playerUuid);
        var builder = (abilities != null ? abilities.toBuilder() : patchbukkit.abilities.Abilities.newBuilder());
        builder.setFlying(value);
        NativeBridgeFfi.setAbilities(SetAbilitiesRequest.newBuilder().setAbilities(builder.build()).setUuid(playerUuid).build());
    }

    @Override
    public void setFlySpeed(float value) throws IllegalArgumentException {
        if (value < -1.0f || value > 1.0f) {
            throw new IllegalArgumentException("Speed value " + value + " is not between -1.0 and 1.0");
        }
        var playerUuid = BridgeUtils.convertUuid(this.getUniqueId());
        var abilities = NativeBridgeFfi.getAbilities(playerUuid);
        var builder = (abilities != null ? abilities.toBuilder() : patchbukkit.abilities.Abilities.newBuilder());
        builder.setFlySpeed(value);
        NativeBridgeFfi.setAbilities(SetAbilitiesRequest.newBuilder().setAbilities(builder.build()).setUuid(playerUuid).build());
    }

    @Override
    public void setWalkSpeed(float value) throws IllegalArgumentException {
        if (value < -1.0f || value > 1.0f) {
            throw new IllegalArgumentException("Speed value " + value + " is not between -1.0 and 1.0");
        }
        var playerUuid = BridgeUtils.convertUuid(this.getUniqueId());
        var abilities = NativeBridgeFfi.getAbilities(playerUuid);
        var builder = (abilities != null ? abilities.toBuilder() : patchbukkit.abilities.Abilities.newBuilder());
        builder.setWalkSpeed(value);
        NativeBridgeFfi.setAbilities(SetAbilitiesRequest.newBuilder().setAbilities(builder.build()).setUuid(playerUuid).build());
    }

    @Override
    public float getFlySpeed() {
        var abilities = NativeBridgeFfi.getAbilities(BridgeUtils.convertUuid(this.uuid));
        return abilities != null ? abilities.getFlySpeed() : 0.05f;
    }

    @Override
    public float getWalkSpeed() {
        var abilities = NativeBridgeFfi.getAbilities(BridgeUtils.convertUuid(this.uuid));
        return abilities != null ? abilities.getWalkSpeed() : 0.1f;
    }

    @Override
    public void setResourcePack(String url, byte @org.jspecify.annotations.Nullable [] hash,
            @org.jspecify.annotations.Nullable String prompt, boolean force) {
        setResourcePack(UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8)), url, hash, prompt, force);
    }

    @Override
    public void setResourcePack(UUID id, String url, byte @org.jspecify.annotations.Nullable [] hash,
            @org.jspecify.annotations.Nullable String prompt, boolean force) {
        setResourcePack(id, url, hash, prompt == null ? null : LegacyComponentSerializer.legacySection().deserialize(prompt), force);
    }

    @Override
    public void setResourcePack(UUID uuid, String url, byte @org.jspecify.annotations.Nullable [] hash,
            @org.jspecify.annotations.Nullable Component prompt, boolean force) {
        sendResourcePack(uuid, url, hash, prompt, force);
    }

    @Override
    public @org.jspecify.annotations.Nullable Status getResourcePackStatus() {
        return resourcePackStatus;
    }

    @Override
    public void addResourcePack(UUID id, String url, byte @org.jspecify.annotations.Nullable [] hash,
            @org.jspecify.annotations.Nullable String prompt, boolean force) {
        setResourcePack(id, url, hash, prompt, force);
    }

    @Override
    public void removeResourcePack(UUID id) {
        NativeBridgeFfi.removeResourcePack(RemoveResourcePackRequest.newBuilder().setPlayerUuid(BridgeUtils.convertUuid(uuid)).setPackUuid(BridgeUtils.convertUuid(Objects.requireNonNull(id))).build());
    }

    @Override
    public void removeResourcePacks() {
        NativeBridgeFfi.clearResourcePacks(BridgeUtils.convertUuid(uuid));
    }

    @Override
    public Scoreboard getScoreboard() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getScoreboard'");
    }

    @Override
    public void setScoreboard(Scoreboard scoreboard) throws IllegalArgumentException, IllegalStateException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setScoreboard'");
    }

    private WorldBorder customWorldBorder;

    @Override
    public @org.jspecify.annotations.Nullable WorldBorder getWorldBorder() {
        return this.customWorldBorder != null ? this.customWorldBorder : getWorld().getWorldBorder();
    }

    @Override
    public void setWorldBorder(@org.jspecify.annotations.Nullable WorldBorder border) {
        this.customWorldBorder = border;
    }

    @Override
    public void sendHealthUpdate(double health, int foodLevel, float saturation) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendHealthUpdate'");
    }

    @Override
    public void sendHealthUpdate() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendHealthUpdate'");
    }

    @Override
    public boolean isHealthScaled() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isHealthScaled'");
    }

    @Override
    public void setHealthScaled(boolean scale) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setHealthScaled'");
    }

    @Override
    public void setHealthScale(double scale) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setHealthScale'");
    }

    @Override
    public double getHealthScale() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getHealthScale'");
    }

    @Override
    public @org.jspecify.annotations.Nullable Entity getSpectatorTarget() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSpectatorTarget'");
    }

    @Override
    public void setSpectatorTarget(@org.jspecify.annotations.Nullable Entity entity) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSpectatorTarget'");
    }

    @Override
    public void sendTitle(@org.jspecify.annotations.Nullable String title,
            @org.jspecify.annotations.Nullable String subtitle) {
        sendTitle(title, subtitle, 10, 70, 20);
    }

    @Override
    public void sendTitle(@org.jspecify.annotations.Nullable String title,
            @org.jspecify.annotations.Nullable String subtitle, int fadeIn, int stay, int fadeOut) {
        setTitleTimes(fadeIn, stay, fadeOut);
        if (title != null) sendTitleComponent(LegacyComponentSerializer.legacySection().deserialize(title), PlayerTitleMode.PLAYER_TITLE_MODE_TITLE);
        if (subtitle != null) sendTitleComponent(LegacyComponentSerializer.legacySection().deserialize(subtitle), PlayerTitleMode.PLAYER_TITLE_MODE_SUBTITLE);
    }

    @Override
    public void resetTitle() {
        clearTitle(true);
    }

    @Override
    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX,
            double offsetY, double offsetZ, double extra, @org.jspecify.annotations.Nullable T data, boolean force) {
        if (particle == null) return;
        var request = patchbukkit.world.SpawnParticleRequest.newBuilder()
            .setWorldUuid(BridgeUtils.convertUuid(this.getWorld().getUID()))
            .setParticle(particle.name().toLowerCase())
            .setX(x).setY(y).setZ(z)
            .setCount(count)
            .setOffsetX(offsetX).setOffsetY(offsetY).setOffsetZ(offsetZ)
            .setExtra(extra)
            .build();
        NativeBridgeFfi.spawnParticle(request);
    }

    @Override
    public AdvancementProgress getAdvancementProgress(Advancement advancement) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAdvancementProgress'");
    }

    @Override
    public int getClientViewDistance() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getClientViewDistance'");
    }

    @Override
    public Locale locale() {
        String locStr = getLocale();
        if (locStr != null && !locStr.isEmpty()) {
            String[] parts = locStr.split("_");
            if (parts.length >= 2) {
                return new Locale(parts[0], parts[1]);
            } else if (parts.length == 1) {
                return new Locale(parts[0]);
            }
        }
        return Locale.US;
    }

    @Override
    public int getPing() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPing'");
    }

    @Override
    public String getLocale() {
        try {
            var resp = NativeBridgeFfi.getPlayerLocale(BridgeUtils.convertUuid(this.uuid));
            if (resp != null && resp.getLocale() != null && !resp.getLocale().isEmpty()) {
                return resp.getLocale();
            }
        } catch (Throwable ignored) {}
        return "en_us";
    }

    @Override
    public boolean getAffectsSpawning() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAffectsSpawning'");
    }

    @Override
    public void setAffectsSpawning(boolean affects) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setAffectsSpawning'");
    }

    @Override
    public int getViewDistance() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getViewDistance'");
    }

    @Override
    public void setViewDistance(int viewDistance) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setViewDistance'");
    }

    @Override
    public int getSimulationDistance() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSimulationDistance'");
    }

    @Override
    public void setSimulationDistance(int simulationDistance) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSimulationDistance'");
    }

    @Override
    public int getSendViewDistance() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSendViewDistance'");
    }

    @Override
    public void setSendViewDistance(int viewDistance) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSendViewDistance'");
    }

    @Override
    public void updateCommands() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateCommands'");
    }

    @Override
    public void openBook(ItemStack book) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'openBook'");
    }

    public void openVirtualSign(Position block, Side side) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'openVirtualSign'");
    }

    @Override
    public void showDemoScreen() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'showDemoScreen'");
    }

    @Override
    public boolean isAllowingServerListings() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isAllowingServerListings'");
    }

    @Override
    public PlayerProfile getPlayerProfile() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPlayerProfile'");
    }

    @Override
    public void setPlayerProfile(PlayerProfile profile) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPlayerProfile'");
    }

    @Override
    public float getCooldownPeriod() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCooldownPeriod'");
    }

    @Override
    public float getCooledAttackStrength(float adjustTicks) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCooledAttackStrength'");
    }

    @Override
    public void resetCooldown() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'resetCooldown'");
    }

    @Override
    public <T> T getClientOption(ClientOption<T> option) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getClientOption'");
    }

    @Override
    public void sendOpLevel(byte level) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendOpLevel'");
    }

    @Override
    public void addAdditionalChatCompletions(Collection<String> completions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addAdditionalChatCompletions'");
    }

    @Override
    public void removeAdditionalChatCompletions(Collection<String> completions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeAdditionalChatCompletions'");
    }

    @Override
    public @org.jspecify.annotations.Nullable String getClientBrandName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getClientBrandName'");
    }

    @Override
    public void lookAt(Entity entity, LookAnchor playerAnchor, LookAnchor entityAnchor) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'lookAt'");
    }

    @Override
    public void showElderGuardian(boolean silent) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'showElderGuardian'");
    }

    @Override
    public int getWardenWarningCooldown() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getWardenWarningCooldown'");
    }

    @Override
    public void setWardenWarningCooldown(int cooldown) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setWardenWarningCooldown'");
    }

    @Override
    public int getWardenTimeSinceLastWarning() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getWardenTimeSinceLastWarning'");
    }

    @Override
    public void setWardenTimeSinceLastWarning(int time) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setWardenTimeSinceLastWarning'");
    }

    @Override
    public int getWardenWarningLevel() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getWardenWarningLevel'");
    }

    @Override
    public void setWardenWarningLevel(int warningLevel) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setWardenWarningLevel'");
    }

    @Override
    public void increaseWardenWarningLevel() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'increaseWardenWarningLevel'");
    }

    @Override
    public Duration getIdleDuration() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getIdleDuration'");
    }

    @Override
    public void resetIdleDuration() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'resetIdleDuration'");
    }

    @Override
    public @Unmodifiable Set<Long> getSentChunkKeys() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSentChunkKeys'");
    }

    @Override
    public @Unmodifiable Set<Chunk> getSentChunks() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSentChunks'");
    }

    @Override
    public boolean isChunkSent(long chunkKey) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isChunkSent'");
    }

    @Override
    public void sendEntityEffect(EntityEffect effect, Entity target) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendEntityEffect'");
    }

    @Override
    public PlayerGiveResult give(Collection<ItemStack> items, boolean dropIfFull) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'give'");
    }

    @Override
    public int getDeathScreenScore() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDeathScreenScore'");
    }

    @Override
    public void setDeathScreenScore(int score) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setDeathScreenScore'");
    }

    public PlayerGameConnection getConnection() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getConnection'");
    }}
