package com.unknown.guzhenren.attachment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unknown.guzhenren.attachment.data.aperture.PendingVitalPenalties;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PendingVitalPenaltiesTest {

    @Test
    @DisplayName("losses settle first in first out, per owner, and an owner's queue empties cleanly")
    void firstInFirstOutPerOwner() {
        PendingVitalPenalties ledger = new PendingVitalPenalties();
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        ledger.record(owner, new ItemStack(Items.STICK));
        ledger.record(owner, new ItemStack(Items.BONE));
        ledger.record(other, new ItemStack(Items.APPLE));

        assertEquals(2, ledger.count(owner));
        assertTrue(ledger.poll(owner).is(Items.STICK));
        assertTrue(ledger.poll(owner).is(Items.BONE));
        assertNull(ledger.poll(owner));
        assertEquals(0, ledger.count(owner));
        assertEquals(1, ledger.count(other));
    }
    @Test
    @DisplayName("an empty stack is never recorded and an unknown owner polls nothing")
    void emptyAndUnknown() {
        PendingVitalPenalties ledger = new PendingVitalPenalties();
        UUID owner = UUID.randomUUID();
        ledger.record(owner, ItemStack.EMPTY);

        assertEquals(0, ledger.count(owner));
        assertNull(ledger.poll(owner));
    }
    @Test
    @DisplayName("the recorded stack is a copy, so the caller clearing its slot cannot erase the loss")
    void recordsACopy() {
        PendingVitalPenalties ledger = new PendingVitalPenalties();
        UUID owner = UUID.randomUUID();
        ItemStack stack = new ItemStack(Items.STICK);
        ledger.record(owner, stack);
        stack.setCount(0);

        assertTrue(ledger.poll(owner).is(Items.STICK));
    }
}
