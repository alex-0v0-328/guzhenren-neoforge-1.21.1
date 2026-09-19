package com.unknown.guzhenren.particle;

/**
 * The even-spacing accumulator behind the planted dash trail: how far into this tick's movement
 * segment the next rings drop. Distance-based rather than tick-based because the server-side dodge
 * movement arrives in uneven chunks (Epic Fight locks movement and lets the animation drive it),
 * so a per-tick drop clusters the rings at the start and end of the path (Alex, 2026-09-20);
 * spacing by measured travel keeps the trail uniform across windups, jumps and wall truncations.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */
final class RingTrailSpacing {

    private RingTrailSpacing() {}
    /** Distance between two neighbouring rings on the dash path, in blocks. */
    static final double SPACING = 2.0D;

    /**
     * @param carry     distance travelled since the last drop (always {@code < SPACING})
     * @param segLen    length of this tick's movement segment
     * @param ringsLeft rings the burst may still drop
     * @return where on the segment the rings drop, plus the new carry and remaining ring budget
     */
    static Drops drops(double carry, double segLen, int ringsLeft) {
        double along = SPACING - carry;
        int count = 0;
        while (count < ringsLeft && along + count * SPACING <= segLen) count++;
        double[] offsets = new double[count];
        for (int i = 0; i < count; i++) offsets[i] = along + i * SPACING;
        double newCarry = count == 0 ? carry + segLen : segLen - (along + (count - 1) * SPACING);
        return new Drops(offsets, newCarry, ringsLeft - count);
    }

    /** @param offsets ascending offsets along this tick's segment where rings drop */
    record Drops(double[] offsets, double carry, int ringsLeft) {}
}
