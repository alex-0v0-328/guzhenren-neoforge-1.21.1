package com.unknown.guzhenren.gametest;

import com.mojang.authlib.GameProfile;
import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.attachment.PlayerDataService;
import com.unknown.guzhenren.attachment.data.dimension.DimensionReturnData;
import com.unknown.guzhenren.attachment.service.dimension.DimensionTravelService;
import com.unknown.guzhenren.event.dimension.TreasureYellowHeavenGuardEvents;
import com.unknown.guzhenren.registry.attachment.ModAttachments;
import com.unknown.guzhenren.registry.world.ModDimensions;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runtime coverage for the Treasure Yellow Heaven [宝黄天] anchored dimension.
 *
 * <p>IMPORTANT FEASIBILITY NOTE: GameTestServer builds its world from the {@code minecraft:flat}
 * world preset ({@code net.minecraft.gametest.framework.GameTestServer}, lines 97-109). That preset
 * only selects overworld, the_nether and the_end; custom datapack dimensions are registered in the
 * datapack registries but never instantiated as a {@code ServerLevel}. Therefore the originally
 * proposed ServerPlayer enter/exit/void-rescue/guard tests that require a live TYH level are not
 * runnable inside {@code runGameTestServer}. This class keeps the tests that are genuinely feasible
 * in that environment: datapack registration, service branching, record lifecycle, and sanity checks
 * that the guard handlers do not cancel for players outside TYH.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TreasureYellowHeavenGameTests {

    private static final BlockPos CENTER = new BlockPos(4, 1, 4);
    private TreasureYellowHeavenGameTests() {}

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void dimensionDatapackIsRegistered(GameTestHelper helper) {
        var registryAccess = helper.getLevel().registryAccess();
        var dimensionTypes = registryAccess.registryOrThrow(Registries.DIMENSION_TYPE);
        var biomes = registryAccess.registryOrThrow(Registries.BIOME);

        helper.assertTrue(dimensionTypes.containsKey(ModDimensions.TREASURE_YELLOW_HEAVEN_TYPE),
                "dimension type missing");
        helper.assertTrue(biomes.containsKey(ModDimensions.TREASURE_YELLOW_HEAVEN_BIOME),
                "biome missing");

        var type = dimensionTypes.get(ModDimensions.TREASURE_YELLOW_HEAVEN_TYPE);
        helper.assertTrue(type != null, "dimension type value missing");
        helper.assertValueEqual(type.minY(), 0, "TYH min_y");
        helper.assertValueEqual(type.height(), 256, "TYH height");
        helper.assertValueEqual(type.logicalHeight(), 256, "TYH logical_height");
        helper.assertTrue(type.hasSkyLight(), "TYH must have sky light");
        helper.assertTrue(!type.natural(), "TYH must not be natural");

        var biome = biomes.get(ModDimensions.TREASURE_YELLOW_HEAVEN_BIOME);
        helper.assertTrue(biome != null, "biome value missing");
        helper.assertTrue(!biome.hasPrecipitation(), "TYH biome must not have precipitation");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void isInsideFalseForOverworldPlayer(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper);
        helper.assertTrue(!DimensionTravelService.isInside(player, ModDimensions.TREASURE_YELLOW_HEAVEN),
                "overworld player reported as inside TYH");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void enterReturnsFalseWhenTargetLevelMissing(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper);
        player.moveTo(helper.absoluteVec(new Vec3(4.5D, 2.5D, 4.5D)));

        boolean entered = DimensionTravelService.enter(player, ModDimensions.TREASURE_YELLOW_HEAVEN,
                ModDimensions.TREASURE_YELLOW_HEAVEN_SPAWN);

        helper.assertTrue(!entered, "enter must return false when TYH ServerLevel is absent");
        helper.assertTrue(!player.getData(ModAttachments.DIMENSION_RETURN).isPresent(),
                "return record must stay empty when enter fails");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void exitReturnsFalseAndClearsEmptyRecord(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper);
        player.moveTo(helper.absoluteVec(new Vec3(4.5D, 2.5D, 4.5D)));
        helper.assertTrue(!player.getData(ModAttachments.DIMENSION_RETURN).isPresent(),
                "fresh player already had a return record");

        boolean used = DimensionTravelService.exit(player);

        helper.assertTrue(!used, "exit must report fallback when no record exists");
        helper.assertTrue(!player.getData(ModAttachments.DIMENSION_RETURN).isPresent(),
                "empty record must stay empty");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void ensureFlightGrantsMayfly(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper);
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;

        DimensionTravelService.ensureFlight(player);

        helper.assertTrue(player.getAbilities().mayfly, "ensureFlight did not grant mayfly");
        helper.assertTrue(!player.getAbilities().flying, "ensureFlight must not force flying");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void rescueIfBelowVoidTeleportsToSpawn(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper);
        Vec3 spawn = helper.absoluteVec(new Vec3(4.5D, 2.5D, 4.5D));
        player.moveTo(spawn.x, spawn.y, spawn.z, 0.0F, 0.0F);
        player.moveTo(spawn.x, helper.getLevel().getMinBuildHeight() - 5.0, spawn.z);
        helper.assertTrue(player.getY() < helper.getLevel().getMinBuildHeight(), "player not below void");

        boolean rescued = DimensionTravelService.rescueIfBelowVoid(player, spawn);

        helper.assertTrue(rescued, "rescue must return true");
        helper.assertValueEqual(player.getX(), spawn.x(), "rescue x");
        helper.assertValueEqual(player.getY(), spawn.y(), "rescue y");
        helper.assertValueEqual(player.getZ(), spawn.z(), "rescue z");
        helper.assertValueEqual(player.fallDistance, 0.0F, "rescue did not reset fall distance");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void clearRemovesReturnRecord(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper);
        player.setData(ModAttachments.DIMENSION_RETURN, DimensionReturnData.of(
                helper.getLevel().dimension(), 1.0, 2.0, 3.0, 0.0F, 0.0F, false, false));

        DimensionTravelService.clear(player);

        helper.assertTrue(!player.getData(ModAttachments.DIMENSION_RETURN).isPresent(),
                "clear did not remove record");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void resetAllClearsReturnRecord(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper);
        player.setData(ModAttachments.DIMENSION_RETURN, DimensionReturnData.of(
                helper.getLevel().dimension(), 1.0, 2.0, 3.0, 0.0F, 0.0F, false, false));

        PlayerDataService.resetAll(player);

        helper.assertTrue(!player.getData(ModAttachments.DIMENSION_RETURN).isPresent(),
                "resetAll did not clear record");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void deathCloneClearsReturnRecord(GameTestHelper helper) {
        ServerPlayer from = survivalMock(helper);
        from.setData(ModAttachments.DIMENSION_RETURN, DimensionReturnData.of(
                helper.getLevel().dimension(), 1.0, 2.0, 3.0, 0.0F, 0.0F, false, false));
        ServerPlayer to = survivalMock(helper);

        PlayerDataService.onClone(from, to, true, true);

        helper.assertTrue(!to.getData(ModAttachments.DIMENSION_RETURN).isPresent(),
                "death clone did not clear record");
        from.discard();
        to.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void guardHandlersIgnoreOverworldPlayer(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper);
        player.moveTo(helper.absoluteVec(new Vec3(4.5D, 2.5D, 4.5D)));
        var pig = helper.spawn(EntityType.PIG, CENTER);

        BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(helper.getLevel(), CENTER,
                Blocks.STONE.defaultBlockState(), player);
        TreasureYellowHeavenGuardEvents.onBreakBlock(breakEvent);
        helper.assertTrue(!breakEvent.isCanceled(), "break canceled outside TYH");

        BlockSnapshot snapshot = BlockSnapshot.create(helper.getLevel().dimension(), helper.getLevel(), CENTER.above());
        BlockEvent.EntityPlaceEvent placeEvent = new BlockEvent.EntityPlaceEvent(snapshot,
                Blocks.AIR.defaultBlockState(), player);
        TreasureYellowHeavenGuardEvents.onPlaceBlock(placeEvent);
        helper.assertTrue(!placeEvent.isCanceled(), "place canceled outside TYH");

        BlockHitResult hit = new BlockHitResult(Vec3.ZERO, net.minecraft.core.Direction.UP, CENTER, false);
        PlayerInteractEvent.RightClickBlock rightClickBlock = new PlayerInteractEvent.RightClickBlock(
                player, InteractionHand.MAIN_HAND, CENTER, hit);
        TreasureYellowHeavenGuardEvents.onRightClickBlock(rightClickBlock);
        helper.assertTrue(!rightClickBlock.isCanceled(), "right-click block canceled outside TYH");

        PlayerInteractEvent.RightClickItem rightClickItem = new PlayerInteractEvent.RightClickItem(
                player, InteractionHand.MAIN_HAND);
        TreasureYellowHeavenGuardEvents.onRightClickItem(rightClickItem);
        helper.assertTrue(!rightClickItem.isCanceled(), "right-click item canceled outside TYH");

        PlayerInteractEvent.EntityInteract entityInteract = new PlayerInteractEvent.EntityInteract(
                player, InteractionHand.MAIN_HAND, pig);
        TreasureYellowHeavenGuardEvents.onEntityInteract(entityInteract);
        helper.assertTrue(!entityInteract.isCanceled(), "entity interact canceled outside TYH");

        AttackEntityEvent attack = new AttackEntityEvent(player, pig);
        TreasureYellowHeavenGuardEvents.onAttackEntity(attack);
        helper.assertTrue(!attack.isCanceled(), "attack canceled outside TYH");

        player.getInventory().clearContent();
        ItemStack stack = new ItemStack(Items.DIAMOND, 3);
        ItemEntity tossed = new ItemEntity(helper.getLevel(), player.getX(), player.getY(), player.getZ(), stack);
        ItemTossEvent toss = new ItemTossEvent(tossed, player);
        TreasureYellowHeavenGuardEvents.onItemToss(toss);
        helper.assertTrue(!toss.isCanceled(), "item toss canceled outside TYH");
        helper.assertValueEqual(player.getInventory().countItem(Items.DIAMOND), 0,
                "item stack restored outside TYH");

        pig.discard();
        player.discard();
        helper.succeed();
    }

    private static ServerPlayer survivalMock(GameTestHelper helper) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), "gzr-tyh-gametest"), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {return false;}
            @Override
            public boolean isCreative() {return false;}
            @Override
            public void sendSystemMessage(@NotNull Component message) {}
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player, cookie) {
            @Override
            public void send(@NotNull Packet<?> packet) {}
            @Override
            public void send(@NotNull Packet<?> packet, @Nullable PacketSendListener listener) {}
        };
        helper.getLevel().addNewPlayer(player);
        player.getAbilities().instabuild = false;
        return player;
    }
}
