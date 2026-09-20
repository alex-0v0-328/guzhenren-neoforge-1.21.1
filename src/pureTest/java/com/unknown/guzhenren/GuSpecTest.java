package com.unknown.guzhenren;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.unknown.guzhenren.custom.enums.aperture.Rank;
import com.unknown.guzhenren.custom.enums.path.GuPath;
import com.unknown.guzhenren.item.gu.GuSpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GuSpecTest {

    private static GuSpec channelling() {
        return GuSpec.of(Rank.ONE, GuPath.STRENGTH)
                .refine(1_000)
                .channel(3_600)
                .hungerBar(18, 3)
                .essencePerHunger(100);
    }
    @Test
    @DisplayName("a real 灌注 registration passes its own check")
    void aRealChannelPasses() {
        assertDoesNotThrow(() -> channelling().validate("white_boar_gu"));
    }
    @Test
    @DisplayName("an 即时 Gu is not asked for hunger rates")
    void instantGuIsNotChecked() {
        assertDoesNotThrow(() -> GuSpec.of(Rank.TWO, GuPath.TRANSFORMATION)
                .refine(16_000).costPerUse(200)
                .hungerBar(4, 2).hungerPerUse(2)
                .validate("roaming_zombie_gu"));
    }
    @Test
    @DisplayName("a round that does not divide by the hunger rate is refused")
    void roundMustDivideByHungerRate() {
        GuSpec bad = channelling().essencePerHunger(700);
        assertThrows(IllegalStateException.class, () -> bad.validate("white_boar_gu"));
    }
    @Test
    @DisplayName("a channel that costs no hunger is refused")
    void aChannelMustPaySomething() {
        assertThrows(IllegalStateException.class, () -> GuSpec.of(Rank.ONE, GuPath.STRENGTH)
                .channel(3_600).validate("no_hunger"));
    }
    @Test
    @DisplayName("negative prices are refused on either shape")
    void negativePricesAreRefused() {
        assertThrows(IllegalStateException.class,
                () -> GuSpec.of(Rank.ONE, GuPath.STRENGTH).refine(-1).validate("negative_refine"));
        assertThrows(IllegalStateException.class,
                () -> GuSpec.of(Rank.ONE, GuPath.STRENGTH).costPerUse(-1).validate("negative_use"));
    }
    @Test
    @DisplayName("effect and item cooldowns can be configured independently")
    void cooldownsAreIndependent() {
        GuSpec spec = GuSpec.of(Rank.THREE, GuPath.STRENGTH).cooldown(600, 20);
        assertEquals(600, spec.effectCooldownTicks());
        assertEquals(20, spec.itemCooldownTicks());
    }
}
