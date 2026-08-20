package org.patchbukkit.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;
import org.patchbukkit.world.PatchBukkitWorld;

class BridgeUtilsTest {
    @Test
    void convertsUuidWithoutLoss() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, BridgeUtils.convertUuid(BridgeUtils.convertUuid(uuid)));
        assertEquals(uuid, BridgeUtils.convertUuid(
            BridgeUtils.getMostSignificantBits(uuid),
            BridgeUtils.getLeastSignificantBits(uuid)
        ));
    }

    @Test
    void convertsLocationWithoutLoss() {
        UUID worldId = UUID.randomUUID();
        Location source = new Location(PatchBukkitWorld.getOrCreate(worldId), 12.5, -4.25, 88.75, 90.0f, -30.0f);
        Location converted = BridgeUtils.convertLocation(BridgeUtils.convertLocation(source));
        assertEquals(worldId, converted.getWorld().getUID());
        assertEquals(source.getX(), converted.getX());
        assertEquals(source.getY(), converted.getY());
        assertEquals(source.getZ(), converted.getZ());
        assertEquals(source.getYaw(), converted.getYaw());
        assertEquals(source.getPitch(), converted.getPitch());
    }
}
