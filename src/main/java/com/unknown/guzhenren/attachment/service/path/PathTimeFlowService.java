package com.unknown.guzhenren.attachment.service.path;

import com.google.common.math.LongMath;
import com.unknown.guzhenren.effect.timed.TimeRateUpEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The only door the player's own clock [自身时间] is hastened through; 宙道 [Time Path] is all that comes
 * to it. {@code rate()} walks {@code getActiveEffects()} for every {@link TimeRateUpEffect}, flooring at 1.
 *
 * <p>⚠ THREE verbs leave this class and a caller uses ONE: {@code waited}, {@code perStep}, {@code
 * steps}; doing arithmetic on {@code rate()} at a call site is how 寿元 once aged BACKWARDS -- only
 * {@code InfoModel} may read {@code rate()}. ⚠ {@code perStep} is what 寿元 goes through, so a
 * hastened life is SPENT FASTER (same verb as essence [真元] and thoughts [念]); {@code waited} floors
 * at one tick. ⚠ A 造诣 grade term has no spec yet; when it lands it goes INSIDE {@code rate()}.
 *
 * @author Alex
 * @version 1.0.0
 * @see TimeRateUpEffect
 * @since 1.0.0
 */

public final class PathTimeFlowService {

    private PathTimeFlowService() {}
    public static final int NORMAL_RATE = 1;
    public static int rate(@NotNull Player player) {
        int rate = 0;
        // TODO(refactor): restore a contributor interface when a second time-flow effect class exists.
        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (instance.getEffect().value() instanceof TimeRateUpEffect effect) {
                rate += effect.timeRate(instance.getAmplifier());
            }
        }
        //   TODO(宙道造诣): a grade term joins HERE, so that no caller has to learn about it.
        return Math.max(NORMAL_RATE, rate);
    }
    //region 自身时间 [his own clock] -- three verbs, because it only ever takes three shapes
    /**
     * A stretch he has to sit through: a press held down, a cooldown, a ritual waited out.
     */
    public static int waited(@NotNull Player p, int ticks) {return waited(rate(p), ticks);}
    /**
     * What he earns or SPENDS in one step -- essence, thought, and the life it costs him.
     */
    public static long perStep(@NotNull Player p, long amount) {return perStep(rate(p), amount);}
    public static double perStep(@NotNull Player p, double amount) {return perStep(rate(p), amount);}
    /**
     * How many times a coupled counter must run this beat, for the two that cannot take a bigger step:
     * 温养 and 炼蛊, whose progress and price would round apart if either were scaled on its own.
     */
    public static int steps(@NotNull Player p) {return rate(p);}
    //endregion

    //region the arithmetic alone -- a rate rather than a player, so it can be asserted without a world
    /**
     * ⚠ Floored at one tick: a short wait divided by a fast clock is zero, which reads as "no wait".
     */
    public static int waited(int rate, int ticks) {return ticks <= 0 ? ticks : Math.max(1, ticks / rate);}
    public static long perStep(int rate, long amount) {return LongMath.saturatedMultiply(amount, rate);}
    public static double perStep(int rate, double amount) {return amount * rate;}
    //endregion
}
