package com.unknown.guzhenren.client.dimension;

import com.unknown.guzhenren.client.ModPalette;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side visual effects for the Treasure Yellow Heaven dimension.
 *
 * <p>Keeps vanilla clouds, sun, moon and the day-night cycle by using the NORMAL sky type,
 * while painting the sky and fog with the dimension's palette yellow.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class TreasureYellowHeavenEffects extends DimensionSpecialEffects {

    private static final int COLOR = ModPalette.TREASURE_YELLOW_HEAVEN;
    private static final float RED = ((COLOR >> 16) & 0xFF) / 255.0F;
    private static final float GREEN = ((COLOR >> 8) & 0xFF) / 255.0F;
    private static final float BLUE = (COLOR & 0xFF) / 255.0F;

    public TreasureYellowHeavenEffects() {
        super(192.0F, false, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return new Vec3(RED, GREEN, BLUE).scale(brightness);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }
}
