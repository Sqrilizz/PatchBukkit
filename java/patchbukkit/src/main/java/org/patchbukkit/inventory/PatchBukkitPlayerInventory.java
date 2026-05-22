package org.patchbukkit.inventory;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PatchBukkitPlayerInventory extends PatchBukkitInventory implements PlayerInventory {

    private final HumanEntity holder;

    public PatchBukkitPlayerInventory(HumanEntity holder) {
        super(holder.getUniqueId(), 36, "Player Inventory", InventoryType.PLAYER);
        this.holder = holder;
    }

    @Override
    public int getSize() {
        return 36;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public @Nullable ItemStack getItem(int index) {
        return super.getItem(index);
    }

    @Override
    public void setItem(int index, @Nullable ItemStack item) {
        super.setItem(index, item);
    }

    @Override
    public @Nullable HumanEntity getHolder() {
        return holder;
    }

    @Override
    public @Nullable ItemStack getItemInMainHand() {
        return getItem(0);
    }

    @Override
    public void setItemInMainHand(@Nullable ItemStack item) {
        setItem(0, item);
    }

    @Override
    public @Nullable ItemStack getItemInOffHand() {
        return getItem(40);
    }

    @Override
    public void setItemInOffHand(@Nullable ItemStack item) {
        setItem(40, item);
    }

    @Override
    public @Nullable ItemStack getItem(EquipmentSlot slot) {
        return switch (slot) {
            case HAND -> getItemInMainHand();
            case OFF_HAND -> getItemInOffHand();
            case FEET -> getBoots();
            case LEGS -> getLeggings();
            case CHEST -> getChestplate();
            case HEAD -> getHelmet();
            default -> null;
        };
    }

    @Override
    public void setItem(EquipmentSlot slot, @Nullable ItemStack item) {
        switch (slot) {
            case HAND -> setItemInMainHand(item);
            case OFF_HAND -> setItemInOffHand(item);
            case FEET -> setBoots(item);
            case LEGS -> setLeggings(item);
            case CHEST -> setChestplate(item);
            case HEAD -> setHelmet(item);
        }
    }

    @Override
    public @Nullable ItemStack getHelmet() {
        return getItem(39);
    }

    @Override
    public void setHelmet(@Nullable ItemStack helmet) {
        setItem(39, helmet);
    }

    @Override
    public @Nullable ItemStack getChestplate() {
        return getItem(38);
    }

    @Override
    public void setChestplate(@Nullable ItemStack chestplate) {
        setItem(38, chestplate);
    }

    @Override
    public @Nullable ItemStack getLeggings() {
        return getItem(37);
    }

    @Override
    public void setLeggings(@Nullable ItemStack leggings) {
        setItem(37, leggings);
    }

    @Override
    public @Nullable ItemStack getBoots() {
        return getItem(36);
    }

    @Override
    public void setBoots(@Nullable ItemStack boots) {
        setItem(36, boots);
    }

    @Override
    public @NotNull ItemStack[] getArmorContents() {
        return new ItemStack[] {
            getBoots(),
            getLeggings(),
            getChestplate(),
            getHelmet()
        };
    }

    @Override
    public void setArmorContents(@Nullable ItemStack[] items) {
        if (items == null || items.length < 4) return;
        setBoots(items[0]);
        setLeggings(items[1]);
        setChestplate(items[2]);
        setHelmet(items[3]);
    }

    @Override
    public @NotNull ItemStack[] getExtraContents() {
        return new ItemStack[] { getItemInOffHand() };
    }

    @Override
    public void setExtraContents(@Nullable ItemStack[] items) {
        if (items == null || items.length < 1) return;
        setItemInOffHand(items[0]);
    }

    @Override
    public @Nullable ItemStack getItemInHand() {
        return getItemInMainHand();
    }

    @Override
    public void setItemInHand(@Nullable ItemStack stack) {
        setItemInMainHand(stack);
    }

    public int clear(int id, int data) {
        return 0;
    }

    @Override
    public int first(@NotNull ItemStack item) {
        return first(item.getType());
    }

    public int firstPartial(@NotNull Material material) {
        ItemStack[] contents = getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material && item.getAmount() < item.getMaxStackSize()) {
                return i;
            }
        }
        return -1;
    }

    public int firstPartial(@NotNull ItemStack item) {
        return firstPartial(item.getType());
    }

    @Override
    public int firstEmpty() {
        return super.firstEmpty();
    }

    @Override
    public boolean contains(@NotNull Material material) {
        return super.contains(material);
    }

    @Override
    public boolean contains(@Nullable ItemStack item) {
        return super.contains(item);
    }

    @Override
    public boolean contains(@NotNull Material material, int amount) {
        return super.contains(material, amount);
    }

    @Override
    public boolean contains(@Nullable ItemStack item, int amount) {
        return super.contains(item, amount);
    }

    @Override
    public boolean containsAtLeast(@Nullable ItemStack item, int amount) {
        return super.containsAtLeast(item, amount);
    }

    @Override
    public int first(@NotNull Material material) {
        return super.first(material);
    }

    @Override
    public void remove(@NotNull Material material) {
        super.remove(material);
    }

    @Override
    public void remove(@NotNull ItemStack item) {
        super.remove(item);
    }

    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public @NotNull ItemStack[] getStorageContents() {
        ItemStack[] storage = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            storage[i] = getItem(i);
            if (storage[i] == null) storage[i] = new ItemStack(Material.AIR);
        }
        return storage;
    }

    @Override
    public void setStorageContents(@NotNull ItemStack[] items) {
        if (items.length != 36) return;
        for (int i = 0; i < 36; i++) {
            setItem(i, items[i]);
        }
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public void setHeldItemSlot(int slot) {
    }

    @Override
    public int getHeldItemSlot() {
        return 0;
    }

    public void setEquipment(int slot, @Nullable ItemStack item) {
        setItem(slot, item);
    }

    public @Nullable ItemStack getEquipment(int slot) {
        return getItem(slot);
    }

    public EntityEquipment getEquipment(@NotNull HumanEntity entity) {
        return null;
    }
}
