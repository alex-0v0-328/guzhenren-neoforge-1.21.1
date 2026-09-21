package com.unknown.guzhenren;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.unknown.guzhenren.registry.world.ModDimensions;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModDimensionsTest {

    @Test
    @DisplayName("the anchored-dimension allow-list pins Treasure Yellow Heaven: fixed spawn, rank 8 grotto-heaven")
    void anchoredDimensionsPinTreasureYellowHeaven() {
        assertEquals(1, ModDimensions.ANCHORED_DIMENSIONS.size());
        ModDimensions.AnchoredDimension entry =
                ModDimensions.ANCHORED_DIMENSIONS.get(ModDimensions.TREASURE_YELLOW_HEAVEN);
        assertNotNull(entry);
        assertEquals(new Vec3(0.5, 64.0, 0.5), entry.spawn());
        assertEquals(8, entry.rank());
    }
}
