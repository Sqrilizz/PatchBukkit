package org.patchbukkit.inventory;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patchbukkit.bridge.BridgeUtils;

import patchbukkit.bridge.NativeBridgeFfi;
import patchbukkit.inventory.GetInventoryRequest;
import patchbukkit.inventory.SetInventorySlotRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Predicate;

public class PatchBukkitInventory implements Inventory {

    private final java.util.UUID holderUuid;
    private final int size;
    private final String title;
    private final InventoryType type;

    public PatchBukkitInventory(java.util.UUID holderUuid, int size, String title, InventoryType type) {
        this.holderUuid = holderUuid;
        this.size = size;
        this.title = title;
        this.type = type;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public void setMaxStackSize(int size) {
    }

    @Override
    public @Nullable ItemStack getItem(int index) {
        if (index < 0 || index >= size) return null;
        var request = GetInventoryRequest.newBuilder()
            .setPlayerUuid(BridgeUtils.convertUuid(holderUuid))
            .build();
        var response = NativeBridgeFfi.getInventory(request);
        if (response == null) return null;
        for (var slot : response.getSlotsList()) {
            if (slot.getSlot() == index) {
                var item = slot.getItem();
                Material material = Material.matchMaterial(item.getType());
                if (material == null) material = Material.AIR;
                return new ItemStack(material, item.getAmount());
            }
        }
        return null;
    }

    @Override
    public void setItem(int index, @Nullable ItemStack item) {
        if (index < 0 || index >= size) return;
        var itemProto = patchbukkit.itemstack.ItemStack.newBuilder();
        if (item == null || item.getType() == Material.AIR) {
            itemProto.setType("minecraft:air").setAmount(0);
        } else {
            itemProto.setType(item.getType().getKey().toString()).setAmount(item.getAmount());
        }
        var request = SetInventorySlotRequest.newBuilder()
            .setPlayerUuid(BridgeUtils.convertUuid(holderUuid))
            .setSlot(index)
            .setItem(itemProto.build())
            .build();
        NativeBridgeFfi.setInventorySlot(request);
    }

    @Override
    public @NotNull HashMap<Integer, ItemStack> addItem(@NotNull ItemStack... items) {
        HashMap<Integer, ItemStack> leftover = new HashMap<>();
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            if (item == null || item.getType() == Material.AIR) continue;
            leftover.put(i, item);
        }
        return leftover;
    }

    @Override
    public @NotNull HashMap<Integer, ItemStack> removeItem(@NotNull ItemStack... items) {
        return new HashMap<>();
    }

    @Override
    public @NotNull ItemStack[] getContents() {
        ItemStack[] contents = new ItemStack[size];
        var request = GetInventoryRequest.newBuilder()
            .setPlayerUuid(BridgeUtils.convertUuid(holderUuid))
            .build();
        var response = NativeBridgeFfi.getInventory(request);
        if (response != null) {
            for (var slot : response.getSlotsList()) {
                int index = slot.getSlot();
                if (index >= 0 && index < size) {
                    var item = slot.getItem();
                    Material material = Material.matchMaterial(item.getType());
                    if (material == null) material = Material.AIR;
                    contents[index] = new ItemStack(material, item.getAmount());
                }
            }
        }
        for (int i = 0; i < size; i++) {
            if (contents[i] == null) contents[i] = new ItemStack(Material.AIR);
        }
        return contents;
    }

    @Override
    public void setContents(@NotNull ItemStack[] items) {
        if (items.length != size) return;
        for (int i = 0; i < items.length; i++) {
            setItem(i, items[i]);
        }
    }

    @Override
    public @NotNull ItemStack[] getStorageContents() {
        return getContents();
    }

    @Override
    public void setStorageContents(@NotNull ItemStack[] items) {
        setContents(items);
    }

    @Override
    public boolean contains(@NotNull Material material) {
        return contains(material, 1);
    }

    @Override
    public boolean contains(@Nullable ItemStack item) {
        if (item == null) return false;
        return contains(item.getType(), item.getAmount());
    }

    @Override
    public boolean contains(@NotNull Material material, int amount) {
        int count = 0;
        for (ItemStack item : getContents()) {
            if (item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count >= amount;
    }

    @Override
    public boolean contains(@Nullable ItemStack item, int amount) {
        if (item == null) return false;
        return contains(item.getType(), amount);
    }

    @Override
    public boolean containsAtLeast(@Nullable ItemStack item, int amount) {
        if (item == null) return false;
        return contains(item.getType(), amount);
    }

    @Override
    public @NotNull HashMap<Integer, ? extends ItemStack> all(@NotNull Material material) {
        HashMap<Integer, ItemStack> result = new HashMap<>();
        ItemStack[] contents = getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i].getType() == material) {
                result.put(i, contents[i]);
            }
        }
        return result;
    }

    @Override
    public @NotNull HashMap<Integer, ? extends ItemStack> all(@Nullable ItemStack item) {
        if (item == null) return new HashMap<>();
        return all(item.getType());
    }

    @Override
    public int first(@NotNull Material material) {
        ItemStack[] contents = getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i].getType() == material) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int first(@Nullable ItemStack item) {
        if (item == null) return -1;
        return first(item.getType());
    }

    @Override
    public int firstEmpty() {
        ItemStack[] contents = getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i].getType() == Material.AIR) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack item : getContents()) {
            if (item.getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void remove(@NotNull Material material) {
        ItemStack[] contents = getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i].getType() == material) {
                setItem(i, null);
            }
        }
    }

    @Override
    public void remove(@NotNull ItemStack item) {
        remove(item.getType());
    }

    @Override
    public void clear(int index) {
        setItem(index, null);
    }

    @Override
    public void clear() {
        for (int i = 0; i < size; i++) {
            setItem(i, null);
        }
    }

    @Override
    public int close() {
        return 0;
    }

    @Override
    public @NotNull List<HumanEntity> getViewers() {
        return List.of();
    }

    @Override
    public @NotNull InventoryType getType() {
        return type;
    }

    @Override
    public @Nullable InventoryHolder getHolder() {
        return null;
    }

    @Override
    public @Nullable InventoryHolder getHolder(boolean useSnapshot) {
        return null;
    }

    @Override
    public @NotNull ListIterator<ItemStack> iterator() {
        return List.of(getContents()).listIterator();
    }

    @Override
    public @NotNull ListIterator<ItemStack> iterator(int index) {
        return List.of(getContents()).listIterator(index);
    }

    @Override
    public @Nullable Location getLocation() {
        return null;
    }

    public boolean isEmpty(int slot) {
        ItemStack item = getItem(slot);
        return item == null || item.getType() == Material.AIR;
    }

    public @NotNull ItemStack getItem(NamespacedKey key) {
        return new ItemStack(Material.AIR);
    }

    public void setItem(NamespacedKey key, @Nullable ItemStack item) {
    }

    public @NotNull HashMap<Integer, ItemStack> removeItem(Predicate<ItemStack> filter, int amount, @NotNull ItemStack... items) {
        return new HashMap<>();
    }

    public @NotNull HashMap<Integer, ItemStack> removeItemAnySlot(Predicate<ItemStack> filter, int amount, @NotNull ItemStack... items) {
        return new HashMap<>();
    }

    @Override
    public @NotNull HashMap<Integer, ItemStack> removeItemAnySlot(@NotNull ItemStack... items) {
        return removeItem(items);
    }
}
