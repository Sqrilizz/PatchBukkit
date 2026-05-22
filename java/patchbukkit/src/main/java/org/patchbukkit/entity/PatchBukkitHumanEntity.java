package org.patchbukkit.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityCategory;
import org.bukkit.entity.Firework;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.inventory.InventoryCloseEvent.Reason;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.InventoryView.Property;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.checkerframework.checker.index.qual.NonNegative;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import com.destroystokyo.paper.block.TargetBlockInfo;
import com.destroystokyo.paper.block.TargetBlockInfo.FluidMode;
import com.destroystokyo.paper.entity.TargetEntityInfo;

import io.papermc.paper.world.damagesource.CombatTracker;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.util.TriState;

@SuppressWarnings({ "deprecation", "removal" })
public class PatchBukkitHumanEntity
    extends PatchBukkitLivingEntity
    implements HumanEntity {
    private boolean op;
    protected final PermissibleBase perm = new PermissibleBase(this);

    public PatchBukkitHumanEntity(UUID uuid,
        String name) {
        super(uuid, name);
    }

    @Override
    public boolean isOp() {
        return this.op;
    }

    @Override
    public void setOp(boolean value) {
        this.op = value;
        this.perm.recalculatePermissions();
    }

    @Override
    public boolean isPermissionSet(String name) {
        return this.perm.isPermissionSet(name);
    }

    @Override
    public boolean isPermissionSet(Permission perm) {
        return this.perm.isPermissionSet(perm);
    }

    @Override
    public boolean hasPermission(String name) {
        return this.perm.hasPermission(name);
    }

    @Override
    public boolean hasPermission(Permission perm) {
        return this.perm.hasPermission(perm);
    }

    @Override
    public double getEyeHeight() {
        return 1.62;
    }

    @Override
    public double getEyeHeight(boolean ignorePose) {
        return 1.62;
    }

    @Override
    public @NotNull Location getEyeLocation() {
        return getLocation().add(0, 1.62, 0);
    }

    @Override
    public @NotNull List<Block> getLineOfSight(@Nullable Set<Material> transparent, int maxDistance) {
        return Collections.emptyList();
    }

    @Override
    public @NotNull Block getTargetBlock(@Nullable Set<Material> transparent, int maxDistance) {
        return null;
    }

    @Override
    public @Nullable Block getTargetBlock(int maxDistance, @NotNull FluidMode fluidMode) {
        return null;
    }

    @Override
    public @Nullable BlockFace getTargetBlockFace(int maxDistance, @NotNull FluidMode fluidMode) {
        return null;
    }

    @Override
    public @Nullable BlockFace getTargetBlockFace(int maxDistance, @NotNull FluidCollisionMode fluidMode) {
        return null;
    }

    @Override
    public @Nullable TargetBlockInfo getTargetBlockInfo(int maxDistance, @NotNull FluidMode fluidMode) {
        return null;
    }

    @Override
    public @Nullable Entity getTargetEntity(int maxDistance, boolean ignoreBlocks) {
        return null;
    }

    @Override
    public @Nullable TargetEntityInfo getTargetEntityInfo(int maxDistance, boolean ignoreBlocks) {
        return null;
    }

    @Override
    public @Nullable RayTraceResult rayTraceEntities(int maxDistance, boolean ignoreBlocks) {
        return null;
    }

    @Override
    public @NotNull List<Block> getLastTwoTargetBlocks(@Nullable Set<Material> transparent, int maxDistance) {
        return Collections.emptyList();
    }

    @Override
    public @Nullable Block getTargetBlockExact(int maxDistance, @NotNull FluidCollisionMode fluidCollisionMode) {
        return null;
    }

    @Override
    public @Nullable RayTraceResult rayTraceBlocks(double maxDistance, @NotNull FluidCollisionMode fluidCollisionMode) {
        return null;
    }

    @Override
    public int getRemainingAir() {
        return 300;
    }

    @Override
    public void setRemainingAir(int ticks) {
    }

    @Override
    public int getMaximumAir() {
        return 300;
    }

    @Override
    public void setMaximumAir(int ticks) {
    }

    @Override
    public @Nullable ItemStack getItemInUse() {
        return null;
    }

    @Override
    public int getItemInUseTicks() {
        return 0;
    }

    @Override
    public void setItemInUseTicks(int ticks) {
    }

    @Override
    public @NonNegative int getArrowCooldown() {
        return 0;
    }

    @Override
    public void setArrowCooldown(@NonNegative int ticks) {
    }

    @Override
    public @NonNegative int getArrowsInBody() {
        return 0;
    }

    @Override
    public void setArrowsInBody(@NonNegative int count, boolean fireEvent) {
    }

    @Override
    public @NonNegative int getBeeStingerCooldown() {
        return 0;
    }

    @Override
    public void setBeeStingerCooldown(@NonNegative int ticks) {
    }

    @Override
    public @NonNegative int getBeeStingersInBody() {
        return 0;
    }

    @Override
    public void setBeeStingersInBody(@NonNegative int count) {
    }

    @Override
    public int getMaximumNoDamageTicks() {
        return 20;
    }

    @Override
    public void setMaximumNoDamageTicks(int ticks) {
    }

    @Override
    public double getLastDamage() {
        return 0.0;
    }

    @Override
    public void setLastDamage(double damage) {
    }

    @Override
    public int getNoDamageTicks() {
        return 0;
    }

    @Override
    public void setNoDamageTicks(int ticks) {
    }

    @Override
    public int getNoActionTicks() {
        return 0;
    }

    @Override
    public void setNoActionTicks(int ticks) {
    }

    @Override
    public @Nullable Player getKiller() {
        return null;
    }

    @Override
    public void setKiller(@Nullable Player killer) {
    }

    @Override
    public boolean addPotionEffect(@NotNull PotionEffect effect, boolean force) {
        return false;
    }

    @Override
    public boolean addPotionEffects(@NotNull Collection<PotionEffect> effects) {
        return false;
    }

    @Override
    public boolean hasPotionEffect(@NotNull PotionEffectType type) {
        return false;
    }

    @Override
    public @Nullable PotionEffect getPotionEffect(@NotNull PotionEffectType type) {
        return null;
    }

    @Override
    public void removePotionEffect(@NotNull PotionEffectType type) {
    }

    @Override
    public @NotNull Collection<PotionEffect> getActivePotionEffects() {
        return Collections.emptyList();
    }

    @Override
    public boolean clearActivePotionEffects() {
        return false;
    }

    @Override
    public boolean hasLineOfSight(@NotNull Entity other) {
        return true;
    }

    @Override
    public boolean hasLineOfSight(@NotNull Location location) {
        return true;
    }

    @Override
    public boolean getRemoveWhenFarAway() {
        return false;
    }

    @Override
    public void setRemoveWhenFarAway(boolean remove) {
    }

    @Override
    public void setCanPickupItems(boolean pickup) {
    }

    @Override
    public boolean getCanPickupItems() {
        return true;
    }

    @Override
    public boolean isLeashed() {
        return false;
    }

    @Override
    public @NotNull Entity getLeashHolder() throws IllegalStateException {
        throw new IllegalStateException("Not leashed");
    }

    @Override
    public boolean setLeashHolder(@Nullable Entity holder) {
        return false;
    }

    @Override
    public boolean isGliding() {
        return false;
    }

    @Override
    public void setGliding(boolean gliding) {
    }

    @Override
    public boolean isSwimming() {
        return false;
    }

    @Override
    public void setSwimming(boolean swimming) {
    }

    @Override
    public boolean isRiptiding() {
        return false;
    }

    @Override
    public void setRiptiding(boolean riptiding) {
    }

    @Override
    public boolean isSleeping() {
        return false;
    }

    @Override
    public boolean isClimbing() {
        return false;
    }

    @Override
    public void setAI(boolean ai) {
    }

    @Override
    public boolean hasAI() {
        return true;
    }

    @Override
    public void attack(@NotNull Entity target) {
    }

    @Override
    public void swingMainHand() {
    }

    @Override
    public void swingOffHand() {
    }

    @Override
    public void playHurtAnimation(float yaw) {
    }

    @Override
    public void setCollidable(boolean collidable) {
    }

    @Override
    public boolean isCollidable() {
        return true;
    }

    @Override
    public @NotNull Set<UUID> getCollidableExemptions() {
        return Collections.emptySet();
    }

    @Override
    public <T> @Nullable T getMemory(@NotNull MemoryKey<T> memoryKey) {
        return null;
    }

    @Override
    public <T> void setMemory(@NotNull MemoryKey<T> memoryKey, @Nullable T memoryValue) {
    }

    @Override
    public @Nullable Sound getHurtSound() {
        return null;
    }

    @Override
    public @Nullable Sound getDeathSound() {
        return null;
    }

    @Override
    public @NotNull Sound getFallDamageSound(int fallHeight) {
        return null;
    }

    @Override
    public @NotNull Sound getFallDamageSoundSmall() {
        return null;
    }

    @Override
    public @NotNull Sound getFallDamageSoundBig() {
        return null;
    }

    @Override
    public @NotNull Sound getDrinkingSound(@NotNull ItemStack itemStack) {
        return null;
    }

    @Override
    public @NotNull Sound getEatingSound(@NotNull ItemStack itemStack) {
        return null;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return false;
    }

    @Override
    public @NotNull EntityCategory getCategory() {
        return EntityCategory.NONE;
    }

    @Override
    public float getSidewaysMovement() {
        return 0f;
    }

    @Override
    public float getUpwardsMovement() {
        return 0f;
    }

    @Override
    public float getForwardsMovement() {
        return 0f;
    }

    @Override
    public void startUsingItem(@NotNull EquipmentSlot hand) {
    }

    @Override
    public void completeUsingActiveItem() {
    }

    @Override
    public @NotNull ItemStack getActiveItem() {
        return new ItemStack(Material.AIR);
    }

    @Override
    public void clearActiveItem() {
    }

    @Override
    public int getActiveItemRemainingTime() {
        return 0;
    }

    @Override
    public void setActiveItemRemainingTime(@Range(from = 0, to = 2147483647) int ticks) {
    }

    @Override
    public boolean hasActiveItem() {
        return false;
    }

    @Override
    public int getActiveItemUsedTime() {
        return 0;
    }

    @Override
    public @NotNull EquipmentSlot getActiveItemHand() {
        return EquipmentSlot.HAND;
    }

    @Override
    public boolean isJumping() {
        return false;
    }

    @Override
    public void setJumping(boolean jumping) {
    }

    @Override
    public void playPickupItemAnimation(@NotNull Item item, int quantity) {
    }

    @Override
    public float getHurtDirection() {
        return 0f;
    }

    @Override
    public void knockback(double strength, double directionX, double directionZ) {
    }

    @Override
    public void broadcastSlotBreak(@NotNull EquipmentSlot slot) {
    }

    @Override
    public void broadcastSlotBreak(@NotNull EquipmentSlot slot, @NotNull Collection<Player> players) {
    }

    @Override
    public @NotNull ItemStack damageItemStack(@NotNull ItemStack stack, int amount) {
        return stack;
    }

    @Override
    public void damageItemStack(@NotNull EquipmentSlot slot, int amount) {
    }

    @Override
    public float getBodyYaw() {
        return getLocation().getYaw();
    }

    @Override
    public void setBodyYaw(float bodyYaw) {
    }

    @Override
    public boolean canUseEquipmentSlot(@NotNull EquipmentSlot slot) {
        return true;
    }

    @Override
    public @NotNull CombatTracker getCombatTracker() {
        return null;
    }

    @Override
    public void setWaypointStyle(@Nullable Key key) {
    }

    @Override
    public void setWaypointColor(@Nullable Color color) {
    }

    @Override
    public @NotNull Key getWaypointStyle() {
        return Key.key("minecraft", "air");
    }

    @Override
    public @Nullable Color getWaypointColor() {
        return null;
    }

    @Override
    public @Nullable AttributeInstance getAttribute(@NotNull Attribute attribute) {
        return null;
    }

    @Override
    public void registerAttribute(@NotNull Attribute attribute) {
    }

    @Override
    public void damage(double amount) {
    }

    @Override
    public void damage(double amount, @Nullable Entity source) {
    }

    @Override
    public void damage(double amount, @NotNull DamageSource damageSource) {
    }

    @Override
    public double getHealth() {
        return 20.0;
    }

    @Override
    public void setHealth(double health) {
    }

    @Override
    public void heal(double amount, @NotNull RegainReason reason) {
    }

    @Override
    public double getAbsorptionAmount() {
        return 0.0;
    }

    @Override
    public void setAbsorptionAmount(double amount) {
    }

    @Override
    public double getMaxHealth() {
        return 20.0;
    }

    @Override
    public void setMaxHealth(double health) {
    }

    @Override
    public void resetMaxHealth() {
    }

    @Override
    public <T extends Projectile> @NotNull T launchProjectile(@NotNull Class<? extends T> projectile,
            @Nullable Vector velocity, @Nullable Consumer<? super T> function) {
        return null;
    }

    @Override
    public TriState getFrictionState() {
        return TriState.NOT_SET;
    }

    @Override
    public void setFrictionState(TriState state) {
    }

    @Override
    public EntityEquipment getEquipment() {
        return null;
    }

    @Override
    public PlayerInventory getInventory() {
        return new org.patchbukkit.inventory.PatchBukkitPlayerInventory(this);
    }

    @Override
    public Inventory getEnderChest() {
        return null;
    }

    @Override
    public MainHand getMainHand() {
        return MainHand.RIGHT;
    }

    @Override
    public boolean setWindowProperty(Property prop, int value) {
        return false;
    }

    @Override
    public int getEnchantmentSeed() {
        return 0;
    }

    @Override
    public void setEnchantmentSeed(int seed) {
    }

    @Override
    public InventoryView getOpenInventory() {
        return null;
    }

    @Override
    public @org.jspecify.annotations.Nullable InventoryView openInventory(Inventory inventory) {
        return null;
    }

    @Override
    public @org.jspecify.annotations.Nullable InventoryView openWorkbench(
            @org.jspecify.annotations.Nullable Location location, boolean force) {
        return null;
    }

    @Override
    public @org.jspecify.annotations.Nullable InventoryView openEnchanting(
            @org.jspecify.annotations.Nullable Location location, boolean force) {
        return null;
    }

    @Override
    public void openInventory(InventoryView inventory) {
    }

    @Override
    public @org.jspecify.annotations.Nullable InventoryView openMerchant(Merchant merchant, boolean force) {
        return null;
    }

    @Override
    public @org.jspecify.annotations.Nullable InventoryView openAnvil(
            @org.jspecify.annotations.Nullable Location location, boolean force) {
        return null;
    }

    @Override
    public @org.jspecify.annotations.Nullable InventoryView openCartographyTable(
            @org.jspecify.annotations.Nullable Location location, boolean force) {
        return null;
    }

    @Override
    public @org.jspecify.annotations.Nullable InventoryView openGrindstone(
            @org.jspecify.annotations.Nullable Location location, boolean force) {
        return null;
    }

    @Override
    public @org.jspecify.annotations.Nullable InventoryView openLoom(
            @org.jspecify.annotations.Nullable Location location, boolean force) {
        return null;
    }

    @Override
    public @org.jspecify.annotations.Nullable InventoryView openSmithingTable(
            @org.jspecify.annotations.Nullable Location location, boolean force) {
        return null;
    }

    @Override
    public @org.jspecify.annotations.Nullable InventoryView openStonecutter(
            @org.jspecify.annotations.Nullable Location location, boolean force) {
        return null;
    }

    @Override
    public void closeInventory(Reason reason) {
    }

    @Override
    public ItemStack getItemInHand() {
        return getInventory().getItemInMainHand();
    }

    @Override
    public void setItemInHand(@org.jspecify.annotations.Nullable ItemStack item) {
        getInventory().setItemInMainHand(item);
    }

    @Override
    public ItemStack getItemOnCursor() {
        return null;
    }

    @Override
    public void setItemOnCursor(@org.jspecify.annotations.Nullable ItemStack item) {
    }

    @Override
    public boolean hasCooldown(Material material) {
        return false;
    }

    @Override
    public int getCooldown(Material material) {
        return 0;
    }

    @Override
    public void setHurtDirection(float hurtDirection) {
    }

    @Override
    public boolean isDeeplySleeping() {
        return false;
    }

    @Override
    public boolean hasCooldown(ItemStack item) {
        return false;
    }

    @Override
    public int getCooldown(ItemStack item) {
        return 0;
    }

    @Override
    public void setCooldown(ItemStack item, int ticks) {
    }

    @Override
    public int getCooldown(Key key) {
        return 0;
    }

    @Override
    public void setCooldown(Key key, int ticks) {
    }

    @Override
    public int getSleepTicks() {
        return 0;
    }

    @Override
    public @org.jspecify.annotations.Nullable Location getPotentialRespawnLocation() {
        return null;
    }

    @Override
    public @org.jspecify.annotations.Nullable FishHook getFishHook() {
        return null;
    }

    @Override
    public boolean sleep(Location location, boolean force) {
        return false;
    }

    @Override
    public void wakeup(boolean setSpawnLocation) {
    }

    @Override
    public void startRiptideAttack(int duration, float attackStrength,
            @org.jspecify.annotations.Nullable ItemStack attackItem) {
    }

    @Override
    public Location getBedLocation() {
        return null;
    }

    @Override
    public GameMode getGameMode() {
        return GameMode.SURVIVAL;
    }

    @Override
    public void setGameMode(GameMode mode) {
    }

    @Override
    public boolean isBlocking() {
        return false;
    }

    @Override
    public boolean isHandRaised() {
        return false;
    }

    @Override
    public int getExpToLevel() {
        return 0;
    }

    @Override
    public @org.jspecify.annotations.Nullable Entity releaseLeftShoulderEntity() {
        return null;
    }

    @Override
    public @org.jspecify.annotations.Nullable Entity releaseRightShoulderEntity() {
        return null;
    }

    @Override
    public float getAttackCooldown() {
        return 1f;
    }

    @Override
    public int discoverRecipes(Collection<NamespacedKey> recipes) {
        return 0;
    }

    @Override
    public int undiscoverRecipes(Collection<NamespacedKey> recipes) {
        return 0;
    }

    @Override
    public boolean hasDiscoveredRecipe(NamespacedKey recipe) {
        return false;
    }

    @Override
    public Set<NamespacedKey> getDiscoveredRecipes() {
        return Collections.emptySet();
    }

    @Override
    public @org.jspecify.annotations.Nullable Entity getShoulderEntityLeft() {
        return null;
    }

    @Override
    public void setShoulderEntityLeft(@org.jspecify.annotations.Nullable Entity entity) {
    }

    @Override
    public @org.jspecify.annotations.Nullable Entity getShoulderEntityRight() {
        return null;
    }

    @Override
    public void setShoulderEntityRight(@org.jspecify.annotations.Nullable Entity entity) {
    }

    @Override
    public void openSign(Sign sign, Side side) {
    }

    @Override
    public boolean dropItem(boolean dropAll) {
        return false;
    }

    @Override
    public @org.jspecify.annotations.Nullable Item dropItem(int slot, int amount, boolean throwRandomly,
            @org.jspecify.annotations.Nullable Consumer<Item> entityOperation) {
        return null;
    }

    @Override
    public @org.jspecify.annotations.Nullable Item dropItem(EquipmentSlot slot, int amount, boolean throwRandomly,
            @org.jspecify.annotations.Nullable Consumer<Item> entityOperation) {
        return null;
    }

    @Override
    public @org.jspecify.annotations.Nullable Item dropItem(ItemStack itemStack, boolean throwRandomly,
            @org.jspecify.annotations.Nullable Consumer<Item> entityOperation) {
        return null;
    }

    @Override
    public float getExhaustion() {
        return 0f;
    }

    @Override
    public void setExhaustion(float value) {
    }

    @Override
    public float getSaturation() {
        return 5f;
    }

    @Override
    public void setSaturation(float value) {
    }

    @Override
    public int getFoodLevel() {
        return 20;
    }

    @Override
    public void setFoodLevel(int value) {
    }

    @Override
    public int getSaturatedRegenRate() {
        return 10;
    }

    @Override
    public void setSaturatedRegenRate(int ticks) {
    }

    @Override
    public int getUnsaturatedRegenRate() {
        return 80;
    }

    @Override
    public void setUnsaturatedRegenRate(int ticks) {
    }

    @Override
    public int getStarvationRate() {
        return 80;
    }

    @Override
    public void setStarvationRate(int ticks) {
    }

    @Override
    public @org.jspecify.annotations.Nullable Location getLastDeathLocation() {
        return null;
    }

    @Override
    public void setLastDeathLocation(@org.jspecify.annotations.Nullable Location location) {
    }

    @Override
    public @org.jspecify.annotations.Nullable Firework fireworkBoost(ItemStack fireworkItemStack) {
        return null;
    }}
