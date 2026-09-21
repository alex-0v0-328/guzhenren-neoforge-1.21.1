package com.unknown.guzhenren.event.dimension;

import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.attachment.service.dimension.DimensionTravelService;
import com.unknown.guzhenren.registry.world.ModDimensions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Server-authority guard for the Treasure Yellow Heaven [宝黄天] anchored dimension. While inside,
 * a player may only move, fly, chat, and use the exit command; every other interaction is cancelled
 * here. Void damage is also cancelled so the rescue handler below can return the player to spawn.
 *
 * <p>All checks are server-side. The few events that also fire on the client are guarded with
 * {@code !level.isClientSide()} so action is taken exactly once per interaction.
 *
 * @author Alex
 * @version 1.0.0
 * @see com.unknown.guzhenren.attachment.service.dimension.DimensionTravelService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class TreasureYellowHeavenGuardEvents {

    private TreasureYellowHeavenGuardEvents() {}

    private static boolean inTyh(Entity entity) {
        return entity.level().dimension().equals(ModDimensions.TREASURE_YELLOW_HEAVEN);
    }

    private static boolean inTyh(Player player) {
        return player.level().dimension().equals(ModDimensions.TREASURE_YELLOW_HEAVEN);
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (inTyh(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof Player player && inTyh(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (inTyh(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) return;
        if (inTyh(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (inTyh(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getLevel().isClientSide()) return;
        if (inTyh(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (inTyh(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (!inTyh(event.getPlayer())) return;

        event.setCanceled(true);
        event.getPlayer().getInventory().placeItemBackInInventory(event.getEntity().getItem());
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        if (event.getSource().getEntity() instanceof Player attacker && inTyh(attacker)) {
            event.setCanceled(true);
            return;
        }

        if (event.getEntity() instanceof Player victim && inTyh(victim)
                && event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!inTyh(player)) return;

        DimensionTravelService.ensureFlight(player);
        DimensionTravelService.rescueIfBelowVoid(player, ModDimensions.TREASURE_YELLOW_HEAVEN_SPAWN);
    }
}
