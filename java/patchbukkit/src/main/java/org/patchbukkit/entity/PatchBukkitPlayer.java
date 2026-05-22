package org.patchbukkit.entity;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;

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
import net.kyori.adventure.util.TriState;
import net.md_5.bungee.api.chat.BaseComponent;
import patchbukkit.bridge.NativeBridgeFfi;
import patchbukkit.bridge.GetPlayerAddressResponse;
import patchbukkit.message.SendMessageRequest;
import patchbukkit.abilities.SetAbilitiesRequest;
import patchbukkit.sound.PlayerEntityPlaySoundRequest;
import patchbukkit.sound.PlayerPlaySoundRequest;

@SuppressWarnings({ "deprecation", "removal" })
public class PatchBukkitPlayer
    extends PatchBukkitHumanEntity
    implements Player {

    private final Map<UUID, Set<WeakReference<Plugin>>> invertedVisibilityEntities = new HashMap<>();

   public PatchBukkitPlayer(UUID uuid, String name) {
        super(uuid, name);
    }

    @Override
    public void sendRawMessage(String message) {
        this.sendRawMessage(null, message);
    }

    @Override
    public void sendRawMessage(UUID sender, String message) {
        if (sender == null) {
            sender = this.getUniqueId();
        }

        var request = SendMessageRequest.newBuilder().setMessage(message).setUuid(BridgeUtils.convertUuid(sender)).build();
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
        return new Player.Spigot();
    }

    @Override
    public boolean isConversing() {
        return false;
    }

    @Override
    public void acceptConversationInput(@NotNull String input) {
    }

    @Override
    public boolean beginConversation(@NotNull Conversation conversation) {
        return false;
    }

    @Override
    public void abandonConversation(@NotNull Conversation conversation) {
    }

    @Override
    public void abandonConversation(@NotNull Conversation conversation, @NotNull ConversationAbandonedEvent details) {
    }

    @Override
    public boolean isOnline() {
        return Bukkit.getServer().getPlayer(this.uuid) != null;
    }

    @Override
    public boolean isConnected() {
        return isOnline();
    }

    @Override
    public boolean isBanned() {
        return false;
    }

    @Override
    public <E extends BanEntry<? super PlayerProfile>> @org.jspecify.annotations.Nullable E ban(
            @org.jspecify.annotations.Nullable String reason, @org.jspecify.annotations.Nullable Date expires,
            @org.jspecify.annotations.Nullable String source) {
        return null;
    }

    @Override
    public <E extends BanEntry<? super PlayerProfile>> @org.jspecify.annotations.Nullable E ban(
            @org.jspecify.annotations.Nullable String reason, @org.jspecify.annotations.Nullable Instant expires,
            @org.jspecify.annotations.Nullable String source) {
        return null;
    }

    @Override
    public <E extends BanEntry<? super PlayerProfile>> @org.jspecify.annotations.Nullable E ban(
            @org.jspecify.annotations.Nullable String reason, @org.jspecify.annotations.Nullable Duration duration,
            @org.jspecify.annotations.Nullable String source) {
        return null;
    }

    @Override
    public boolean isWhitelisted() {
        return true;
    }

    @Override
    public void setWhitelisted(boolean value) {
    }

    @Override
    public @org.jspecify.annotations.Nullable Player getPlayer() {
        return this;
    }

    @Override
    public long getFirstPlayed() {
        return System.currentTimeMillis();
    }

    @Override
    public long getLastPlayed() {
        return System.currentTimeMillis();
    }

    @Override
    public boolean hasPlayedBefore() {
        return true;
    }

    @Override
    public long getLastLogin() {
        return System.currentTimeMillis();
    }

    @Override
    public long getLastSeen() {
        return System.currentTimeMillis();
    }

    @Override
    public @org.jspecify.annotations.Nullable Location getRespawnLocation(boolean loadLocationAndValidate) {
        return null;
    }

    @Override
    public void incrementStatistic(Statistic statistic) throws IllegalArgumentException {
    }

    @Override
    public void decrementStatistic(Statistic statistic) throws IllegalArgumentException {
    }

    @Override
    public void incrementStatistic(Statistic statistic, int amount) throws IllegalArgumentException {
    }

    @Override
    public void decrementStatistic(Statistic statistic, int amount) throws IllegalArgumentException {
    }

    @Override
    public void setStatistic(Statistic statistic, int newValue) throws IllegalArgumentException {
    }

    @Override
    public int getStatistic(Statistic statistic) throws IllegalArgumentException {
        return 0;
    }

    @Override
    public void incrementStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
    }

    @Override
    public void decrementStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
    }

    @Override
    public int getStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
        return 0;
    }

    @Override
    public void incrementStatistic(Statistic statistic, Material material, int amount) throws IllegalArgumentException {
    }

    @Override
    public void decrementStatistic(Statistic statistic, Material material, int amount) throws IllegalArgumentException {
    }

    @Override
    public void setStatistic(Statistic statistic, Material material, int newValue) throws IllegalArgumentException {
    }

    @Override
    public void incrementStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
    }

    @Override
    public void decrementStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
    }

    @Override
    public int getStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
        return 0;
    }

    @Override
    public void incrementStatistic(Statistic statistic, EntityType entityType, int amount)
            throws IllegalArgumentException {
    }

    @Override
    public void decrementStatistic(Statistic statistic, EntityType entityType, int amount) {
    }

    @Override
    public void setStatistic(Statistic statistic, EntityType entityType, int newValue) {
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return Map.of("name", getName());
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
        return 775;
    }

    @Override
    public @org.jspecify.annotations.Nullable InetSocketAddress getVirtualHost() {
        return null;
    }

    @Override
    public @UnmodifiableView Iterable<? extends BossBar> activeBossBars() {
        return Collections.emptyList();
    }

    @Override
    public Component displayName() {
        return Component.text(this.getName());
    }

    @Override
    public void displayName(@org.jspecify.annotations.Nullable Component displayName) {
    }

    @Override
    public String getDisplayName() {
        return this.getName();
    }

    @Override
    public void setDisplayName(@org.jspecify.annotations.Nullable String name) {
    }

    @Override
    public void playerListName(@org.jspecify.annotations.Nullable Component name) {
    }

    @Override
    public Component playerListName() {
        return Component.text(this.getName());
    }

    @Override
    public @org.jspecify.annotations.Nullable Component playerListHeader() {
        return null;
    }

    @Override
    public @org.jspecify.annotations.Nullable Component playerListFooter() {
        return null;
    }

    @Override
    public String getPlayerListName() {
        return this.getName();
    }

    @Override
    public void setPlayerListName(@org.jspecify.annotations.Nullable String name) {
    }

    @Override
    public int getPlayerListOrder() {
        return 0;
    }

    @Override
    public void setPlayerListOrder(int order) {
    }

    @Override
    public @org.jspecify.annotations.Nullable String getPlayerListHeader() {
        return null;
    }

    @Override
    public @org.jspecify.annotations.Nullable String getPlayerListFooter() {
        return null;
    }

    @Override
    public void setPlayerListHeader(@org.jspecify.annotations.Nullable String header) {
    }

    @Override
    public void setPlayerListFooter(@org.jspecify.annotations.Nullable String footer) {
    }

    @Override
    public void setPlayerListHeaderFooter(@org.jspecify.annotations.Nullable String header,
            @org.jspecify.annotations.Nullable String footer) {
    }

    @Override
    public void setCompassTarget(Location loc) {
    }

    @Override
    public Location getCompassTarget() {
        return getLocation();
    }

    @Override
    public @org.jspecify.annotations.Nullable InetSocketAddress getAddress() {
        try {
            GetPlayerAddressResponse resp = NativeBridgeFfi.getPlayerAddress(BridgeUtils.convertUuid(this.uuid));
            if (resp == null || resp.getHost().isEmpty()) return null;
            return new InetSocketAddress(InetAddress.getByName(resp.getHost()), resp.getPort());
        } catch (UnknownHostException e) {
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public @org.jspecify.annotations.Nullable InetSocketAddress getHAProxyAddress() {
        return null;
    }

    @Override
    public boolean isTransferred() {
        return false;
    }

    @Override
    public CompletableFuture<byte @org.jspecify.annotations.Nullable []> retrieveCookie(NamespacedKey key) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void storeCookie(NamespacedKey key, byte[] value) {
    }

    @Override
    public void transfer(String host, int port) {
    }

    @Override
    public void kickPlayer(@org.jspecify.annotations.Nullable String message) {
    }

    @Override
    public void kick(@org.jspecify.annotations.Nullable Component message, Cause cause) {
    }

    @Override
    public <E extends BanEntry<? super PlayerProfile>> @org.jspecify.annotations.Nullable E ban(
            @org.jspecify.annotations.Nullable String reason, @org.jspecify.annotations.Nullable Date expires,
            @org.jspecify.annotations.Nullable String source, boolean kickPlayer) {
        return null;
    }

    @Override
    public <E extends BanEntry<? super PlayerProfile>> @org.jspecify.annotations.Nullable E ban(
            @org.jspecify.annotations.Nullable String reason, @org.jspecify.annotations.Nullable Instant expires,
            @org.jspecify.annotations.Nullable String source, boolean kickPlayer) {
        return null;
    }

    @Override
    public <E extends BanEntry<? super PlayerProfile>> @org.jspecify.annotations.Nullable E ban(
            @org.jspecify.annotations.Nullable String reason, @org.jspecify.annotations.Nullable Duration duration,
            @org.jspecify.annotations.Nullable String source, boolean kickPlayer) {
        return null;
    }

    @Override
    public @org.jspecify.annotations.Nullable BanEntry<InetAddress> banIp(
            @org.jspecify.annotations.Nullable String reason, @org.jspecify.annotations.Nullable Date expires,
            @org.jspecify.annotations.Nullable String source, boolean kickPlayer) {
        return null;
    }

    @Override
    public @org.jspecify.annotations.Nullable BanEntry<InetAddress> banIp(
            @org.jspecify.annotations.Nullable String reason, @org.jspecify.annotations.Nullable Instant expires,
            @org.jspecify.annotations.Nullable String source, boolean kickPlayer) {
        return null;
    }

    @Override
    public @org.jspecify.annotations.Nullable BanEntry<InetAddress> banIp(
            @org.jspecify.annotations.Nullable String reason, @org.jspecify.annotations.Nullable Duration duration,
            @org.jspecify.annotations.Nullable String source, boolean kickPlayer) {
        return null;
    }

    @Override
    public void chat(String msg) {
    }

    @Override
    public boolean performCommand(String command) {
        return Bukkit.dispatchCommand(this, command);
    }

    @Override
    public boolean isSprinting() {
        return false;
    }

    @Override
    public void setSprinting(boolean sprinting) {
    }

    @Override
    public void saveData() {
    }

    @Override
    public void loadData() {
    }

    @Override
    public void setSleepingIgnored(boolean isSleeping) {
    }

    @Override
    public boolean isSleepingIgnored() {
        return false;
    }

    @Override
    public void setRespawnLocation(@org.jspecify.annotations.Nullable Location location, boolean force) {
    }

    @Override
    public Collection<EnderPearl> getEnderPearls() {
        return Collections.emptyList();
    }

    @Override
    public Input getCurrentInput() {
        return null;
    }

    @Override
    public void playNote(Location loc, Instrument instrument, Note note) {
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
    }

    @Override
    public void stopSound(SoundCategory category) {
    }

    @Override
    public void stopAllSounds() {
    }

    @Override
    public void playEffect(Location loc, Effect effect, int data) {
    }

    @Override
    public <T> void playEffect(Location loc, Effect effect, @org.jspecify.annotations.Nullable T data) {
    }

    @Override
    public boolean breakBlock(Block block) {
        return false;
    }

    @Override
    public void sendBlockChange(Location loc, Material material, byte data) {
    }

    @Override
    public void sendBlockChange(Location loc, BlockData block) {
    }

    @Override
    public void sendBlockChanges(Collection<BlockState> blocks) {
    }

    @Override
    public void sendMultiBlockChange(Map<? extends Position, BlockData> blockChanges) {
    }

    @Override
    public void sendBlockDamage(Location loc, float progress, Entity source) {
    }

    @Override
    public void sendBlockDamage(Location loc, float progress, int sourceId) {
    }

    @Override
    public void sendEquipmentChange(LivingEntity entity, EquipmentSlot slot,
            @org.jspecify.annotations.Nullable ItemStack item) {
    }

    @Override
    public void sendEquipmentChange(LivingEntity entity,
            Map<EquipmentSlot, @org.jspecify.annotations.Nullable ItemStack> items) {
    }

    @Override
    public void sendSignChange(Location loc, @org.jspecify.annotations.Nullable List<? extends Component> lines,
            DyeColor dyeColor, boolean hasGlowingText) throws IllegalArgumentException {
    }

    @Override
    public void sendSignChange(Location loc,
            @org.jspecify.annotations.Nullable String @org.jspecify.annotations.Nullable [] lines)
            throws IllegalArgumentException {
    }

    @Override
    public void sendSignChange(Location loc,
            @org.jspecify.annotations.Nullable String @org.jspecify.annotations.Nullable [] lines, DyeColor dyeColor)
            throws IllegalArgumentException {
    }

    @Override
    public void sendSignChange(Location loc,
            @org.jspecify.annotations.Nullable String @org.jspecify.annotations.Nullable [] lines, DyeColor dyeColor,
            boolean hasGlowingText) throws IllegalArgumentException {
    }

    @Override
    public void sendBlockUpdate(Location loc, TileState tileState) throws IllegalArgumentException {
    }

    @Override
    public void sendPotionEffectChange(LivingEntity entity, PotionEffect effect) {
    }

    @Override
    public void sendPotionEffectChangeRemove(LivingEntity entity, PotionEffectType type) {
    }

    @Override
    public void sendMap(MapView map) {
    }

    @Override
    public void showWinScreen() {
    }

    @Override
    public boolean hasSeenWinScreen() {
        return false;
    }

    @Override
    public void setHasSeenWinScreen(boolean hasSeenWinScreen) {
    }

    @Override
    public void sendActionBar(String message) {
    }

    @Override
    public void sendActionBar(char alternateChar, String message) {
    }

    @Override
    public void sendActionBar(BaseComponent... message) {
    }

    @Override
    public void setPlayerListHeaderFooter(BaseComponent @org.jspecify.annotations.Nullable [] header,
            BaseComponent @org.jspecify.annotations.Nullable [] footer) {
    }

    @Override
    public void setPlayerListHeaderFooter(@org.jspecify.annotations.Nullable BaseComponent header,
            @org.jspecify.annotations.Nullable BaseComponent footer) {
    }

    @Override
    public void setTitleTimes(int fadeInTicks, int stayTicks, int fadeOutTicks) {
    }

    @Override
    public void setSubtitle(BaseComponent[] subtitle) {
    }

    @Override
    public void setSubtitle(BaseComponent subtitle) {
    }

    @Override
    public void showTitle(@org.jspecify.annotations.Nullable BaseComponent[] title) {
    }

    @Override
    public void showTitle(@org.jspecify.annotations.Nullable BaseComponent title) {
    }

    @Override
    public void showTitle(@org.jspecify.annotations.Nullable BaseComponent[] title,
            @org.jspecify.annotations.Nullable BaseComponent[] subtitle, int fadeInTicks, int stayTicks,
            int fadeOutTicks) {
    }

    @Override
    public void showTitle(@org.jspecify.annotations.Nullable BaseComponent title,
            @org.jspecify.annotations.Nullable BaseComponent subtitle, int fadeInTicks, int stayTicks,
            int fadeOutTicks) {
    }

    @Override
    public void sendTitle(Title title) {
    }

    @Override
    public void updateTitle(Title title) {
    }

    @Override
    public void hideTitle() {
    }

    @Override
    public void sendHurtAnimation(float yaw) {
    }

    @Override
    public void sendLinks(ServerLinks links) {
    }

    @Override
    public void addCustomChatCompletions(Collection<String> completions) {
    }

    @Override
    public void removeCustomChatCompletions(Collection<String> completions) {
    }

    @Override
    public void setCustomChatCompletions(Collection<String> completions) {
    }

    @Override
    public void updateInventory() {
    }

    @Override
    public @org.jspecify.annotations.Nullable GameMode getPreviousGameMode() {
        return null;
    }

    @Override
    public void setPlayerTime(long time, boolean relative) {
    }

    @Override
    public long getPlayerTime() {
        return 0L;
    }

    @Override
    public long getPlayerTimeOffset() {
        return 0L;
    }

    @Override
    public boolean isPlayerTimeRelative() {
        return true;
    }

    @Override
    public void resetPlayerTime() {
    }

    @Override
    public void setPlayerWeather(WeatherType type) {
    }

    @Override
    public @org.jspecify.annotations.Nullable WeatherType getPlayerWeather() {
        return null;
    }

    @Override
    public void resetPlayerWeather() {
    }

    @Override
    public int getExpCooldown() {
        return 0;
    }

    @Override
    public void setExpCooldown(int ticks) {
    }

    @Override
    public void giveExp(int amount, boolean applyMending) {
    }

    @Override
    public int applyMending(int amount) {
        return 0;
    }

    @Override
    public void giveExpLevels(int amount) {
    }

    @Override
    public float getExp() {
        return 0f;
    }

    @Override
    public void setExp(float exp) {
    }

    @Override
    public int getLevel() {
        return 0;
    }

    @Override
    public void setLevel(int level) {
    }

    @Override
    public int getTotalExperience() {
        return 0;
    }

    @Override
    public void setTotalExperience(int exp) {
    }

    @Override
    public @Range(from = 0, to = 2147483647) int calculateTotalExperiencePoints() {
        return 0;
    }

    @Override
    public void setExperienceLevelAndProgress(@Range(from = 0, to = 2147483647) int totalExperience) {
    }

    @Override
    public int getExperiencePointsNeededForNextLevel() {
        return 7;
    }

    @Override
    public void sendExperienceChange(float progress) {
    }

    @Override
    public void sendExperienceChange(float progress, int level) {
    }

    @Override
    public boolean getAllowFlight() {
        return NativeBridgeFfi.getAbilities(BridgeUtils.convertUuid(this.uuid)).getAllowFlying();
    }

    @Override
    public void setAllowFlight(boolean flight) {
        var abilities = NativeBridgeFfi.getAbilities(BridgeUtils.convertUuid(this.uuid)).toBuilder();
        abilities.setAllowFlying(flight);
        if (abilities.getFlying()) abilities.setFlying(false);
        NativeBridgeFfi.setAbilities(SetAbilitiesRequest.newBuilder().setAbilities(abilities).setUuid(BridgeUtils.convertUuid(this.uuid)).build());
    }

    @Override
    public void setFlyingFallDamage(TriState flyingFallDamage) {
    }

    @Override
    public TriState hasFlyingFallDamage() {
        return TriState.NOT_SET;
    }

    @Override
    public void hidePlayer(Player player) {
    }

    @Override
    public void showPlayer(Player player) {
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
        return true;
    }

    @Override
    public boolean unlistPlayer(Player other) {
        return false;
    }

    @Override
    public boolean listPlayer(Player other) {
        return true;
    }

    @Override
    public boolean isFlying() {
        return NativeBridgeFfi.getAbilities(BridgeUtils.convertUuid(this.uuid)).getFlying();
    }

    @Override
    public void setFlying(boolean value) {
        var playerUuid = BridgeUtils.convertUuid(this.getUniqueId());
        var abilities = NativeBridgeFfi.getAbilities(playerUuid).toBuilder().setFlying(value).build();
        NativeBridgeFfi.setAbilities(SetAbilitiesRequest.newBuilder().setAbilities(abilities).setUuid(playerUuid).build());
    }

    @Override
    public void setFlySpeed(float value) throws IllegalArgumentException {
    }

    @Override
    public void setWalkSpeed(float value) throws IllegalArgumentException {
    }

    @Override
    public float getFlySpeed() {
        return 0.1f;
    }

    @Override
    public float getWalkSpeed() {
        return 0.2f;
    }

    @Override
    public void setResourcePack(String url, byte @org.jspecify.annotations.Nullable [] hash,
            @org.jspecify.annotations.Nullable String prompt, boolean force) {
    }

    @Override
    public void setResourcePack(UUID id, String url, byte @org.jspecify.annotations.Nullable [] hash,
            @org.jspecify.annotations.Nullable String prompt, boolean force) {
    }

    @Override
    public void setResourcePack(UUID uuid, String url, byte @org.jspecify.annotations.Nullable [] hash,
            @org.jspecify.annotations.Nullable Component prompt, boolean force) {
    }

    @Override
    public @org.jspecify.annotations.Nullable Status getResourcePackStatus() {
        return null;
    }

    @Override
    public void addResourcePack(UUID id, String url, byte @org.jspecify.annotations.Nullable [] hash,
            @org.jspecify.annotations.Nullable String prompt, boolean force) {
    }

    @Override
    public void removeResourcePack(UUID id) {
    }

    @Override
    public void removeResourcePacks() {
    }

    @Override
    public Scoreboard getScoreboard() {
        return Bukkit.getScoreboardManager().getNewScoreboard();
    }

    @Override
    public void setScoreboard(Scoreboard scoreboard) throws IllegalArgumentException, IllegalStateException {
    }

    @Override
    public @org.jspecify.annotations.Nullable WorldBorder getWorldBorder() {
        return null;
    }

    @Override
    public void setWorldBorder(@org.jspecify.annotations.Nullable WorldBorder border) {
    }

    @Override
    public void sendHealthUpdate(double health, int foodLevel, float saturation) {
    }

    @Override
    public void sendHealthUpdate() {
    }

    @Override
    public boolean isHealthScaled() {
        return false;
    }

    @Override
    public void setHealthScaled(boolean scale) {
    }

    @Override
    public void setHealthScale(double scale) throws IllegalArgumentException {
    }

    @Override
    public double getHealthScale() {
        return 20.0;
    }

    @Override
    public @org.jspecify.annotations.Nullable Entity getSpectatorTarget() {
        return null;
    }

    @Override
    public void setSpectatorTarget(@org.jspecify.annotations.Nullable Entity entity) {
    }

    @Override
    public void sendTitle(@org.jspecify.annotations.Nullable String title,
            @org.jspecify.annotations.Nullable String subtitle) {
    }

    @Override
    public void sendTitle(@org.jspecify.annotations.Nullable String title,
            @org.jspecify.annotations.Nullable String subtitle, int fadeIn, int stay, int fadeOut) {
    }

    @Override
    public void resetTitle() {
    }

    @Override
    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX,
            double offsetY, double offsetZ, double extra, @org.jspecify.annotations.Nullable T data, boolean force) {
    }

    @Override
    public AdvancementProgress getAdvancementProgress(Advancement advancement) {
        return null;
    }

    @Override
    public int getClientViewDistance() {
        return 10;
    }

    @Override
    public Locale locale() {
        return Locale.ENGLISH;
    }

    @Override
    public int getPing() {
        return 0;
    }

    @Override
    public String getLocale() {
        return "en_US";
    }

    @Override
    public boolean getAffectsSpawning() {
        return true;
    }

    @Override
    public void setAffectsSpawning(boolean affects) {
    }

    @Override
    public int getViewDistance() {
        return 10;
    }

    @Override
    public void setViewDistance(int viewDistance) {
    }

    @Override
    public int getSimulationDistance() {
        return 10;
    }

    @Override
    public void setSimulationDistance(int simulationDistance) {
    }

    @Override
    public int getSendViewDistance() {
        return 10;
    }

    @Override
    public void setSendViewDistance(int viewDistance) {
    }

    @Override
    public void updateCommands() {
    }

    @Override
    public void openBook(ItemStack book) {
    }

    @Override
    public void openVirtualSign(Position block, Side side) {
    }

    @Override
    public void showDemoScreen() {
    }

    @Override
    public boolean isAllowingServerListings() {
        return true;
    }

    @Override
    public PlayerProfile getPlayerProfile() {
        return Bukkit.createProfile(this.uuid, this.getName());
    }

    @Override
    public void setPlayerProfile(PlayerProfile profile) {
    }

    @Override
    public float getCooldownPeriod() {
        return 0f;
    }

    @Override
    public float getCooledAttackStrength(float adjustTicks) {
        return 1f;
    }

    @Override
    public void resetCooldown() {
    }

    @Override
    public <T> T getClientOption(ClientOption<T> option) {
        return null;
    }

    @Override
    public void sendOpLevel(byte level) {
    }

    @Override
    public void addAdditionalChatCompletions(Collection<String> completions) {
    }

    @Override
    public void removeAdditionalChatCompletions(Collection<String> completions) {
    }

    @Override
    public @org.jspecify.annotations.Nullable String getClientBrandName() {
        return null;
    }

    @Override
    public void lookAt(Entity entity, LookAnchor playerAnchor, LookAnchor entityAnchor) {
    }

    @Override
    public void showElderGuardian(boolean silent) {
    }

    @Override
    public int getWardenWarningCooldown() {
        return 0;
    }

    @Override
    public void setWardenWarningCooldown(int cooldown) {
    }

    @Override
    public int getWardenTimeSinceLastWarning() {
        return 0;
    }

    @Override
    public void setWardenTimeSinceLastWarning(int time) {
    }

    @Override
    public int getWardenWarningLevel() {
        return 0;
    }

    @Override
    public void setWardenWarningLevel(int warningLevel) {
    }

    @Override
    public void increaseWardenWarningLevel() {
    }

    @Override
    public Duration getIdleDuration() {
        return Duration.ZERO;
    }

    @Override
    public void resetIdleDuration() {
    }

    @Override
    public @Unmodifiable Set<Long> getSentChunkKeys() {
        return Set.of();
    }

    @Override
    public @Unmodifiable Set<Chunk> getSentChunks() {
        return Set.of();
    }

    @Override
    public boolean isChunkSent(long chunkKey) {
        return false;
    }

    @Override
    public void sendEntityEffect(EntityEffect effect, Entity target) {
    }

    @Override
    public PlayerGiveResult give(Collection<ItemStack> items, boolean dropIfFull) {
        return null;
    }

    @Override
    public int getDeathScreenScore() {
        return 0;
    }

    @Override
    public void setDeathScreenScore(int score) {
    }

    @Override
    public PlayerGameConnection getConnection() {
        return null;
    }}
