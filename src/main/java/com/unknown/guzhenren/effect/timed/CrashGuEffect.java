package com.unknown.guzhenren.effect.timed;

import com.unknown.guzhenren.Ticks;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * The Crash Gu family [横冲蛊 / 直撞蛊 / 横冲直撞蛊]: one class carrying all three dashes --
 * horizontal, vertical, and the charging one that moves on both axes.
 *
 * <p>The class holds the axis flags, the shared duration helper and the dash distance spec; the
 * movement itself is reported by the client through {@link com.unknown.guzhenren.network.payload.DashPayload}.
 * The charging shape grades its icon by rank because it spans ranks four and five.
 *
 * @author Alex
 * @version 1.0.0
 * @see com.unknown.guzhenren.network.payload.DashPayload
 * @since 1.0.0
 */

public final class CrashGuEffect extends MobEffect {

    public static final int HORIZONTAL = 1;
    public static final int VERTICAL = 2;
    /**
     * Multiplier on Epic Fight's dodge coordinate vector: one number shared by the whole Crash Gu
     * family, 4.5 since 2026-09-19. Lives here rather than in the EF bridge so the spec stays
     * readable (and pinnable in pure tests) without Epic Fight on the classpath.
     */
    public static final double DASH_COORD_SCALE = 4.5D;
    private final int axes;
    public CrashGuEffect(MobEffectCategory category, int color, int axes) {
        super(category, color);
        this.axes = axes;
    }
    public static int duration(int seconds) {return seconds * Ticks.SECOND;}
    public boolean horizontal() {return (axes & HORIZONTAL) != 0;}
    public boolean vertical() {return (axes & VERTICAL) != 0;}
}
