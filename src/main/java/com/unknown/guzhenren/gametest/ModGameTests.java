package com.unknown.guzhenren.gametest;

import com.mojang.authlib.GameProfile;
import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.Ticks;
import com.unknown.guzhenren.attachment.PlayerDataService;
import com.unknown.guzhenren.attachment.data.aperture.ApertureData;
import com.unknown.guzhenren.attachment.data.aperture.ApertureNourishData;
import com.unknown.guzhenren.attachment.data.aperture.ApertureStorage;
import com.unknown.guzhenren.attachment.data.aperture.PendingVitalPenalties;
import com.unknown.guzhenren.attachment.service.aperture.ApertureNourishService;
import com.unknown.guzhenren.attachment.service.aperture.AperturePressureExplosionTask;
import com.unknown.guzhenren.attachment.service.aperture.ApertureService;
import com.unknown.guzhenren.attachment.service.aperture.ApertureStorageService;
import com.unknown.guzhenren.attachment.service.body.BodyService;
import com.unknown.guzhenren.attachment.service.mind.MindService;
import com.unknown.guzhenren.attachment.service.path.PathQiService;
import com.unknown.guzhenren.attachment.service.path.PathService;
import com.unknown.guzhenren.attachment.service.soul.SoulService;
import com.unknown.guzhenren.block.SpiritSpringBlock;
import com.unknown.guzhenren.custom.enums.aperture.Rank;
import com.unknown.guzhenren.custom.enums.body.ExtremePhysique;
import com.unknown.guzhenren.custom.enums.body.Physique;
import com.unknown.guzhenren.custom.enums.path.GuPath;
import com.unknown.guzhenren.custom.enums.wisdom.WisdomType;
import com.unknown.guzhenren.display.InfoModel;
import com.unknown.guzhenren.entity.BoarGuEntity;
import com.unknown.guzhenren.entity.FlyingGuEntity;
import com.unknown.guzhenren.entity.HopeGuEntity;
import com.unknown.guzhenren.item.GuItem;
import com.unknown.guzhenren.item.gu.RefinedGuState;
import com.unknown.guzhenren.item.gu.TendedGuItem;
import com.unknown.guzhenren.menu.ApertureStorageMenu;
import com.unknown.guzhenren.registry.attachment.ModAttachments;
import com.unknown.guzhenren.registry.block.ModBlocks;
import com.unknown.guzhenren.registry.entity.ModEntityTypes;
import com.unknown.guzhenren.registry.fluid.ModFluids;
import com.unknown.guzhenren.registry.item.ModDataComponents;
import com.unknown.guzhenren.registry.item.ModItems;
import com.unknown.guzhenren.world.SpiritSpringFeature;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.player.SetTargetEvent;
import yesman.epicfight.api.event.types.player.SkillConsumeEvent;
import yesman.epicfight.registry.entries.EpicFightSkills;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/**
 * Runtime behavior tests executed inside a real server tick loop via {@code runGameTestServer}.
 * Every method pins one load-bearing mechanic that plain unit tests cannot reach: the Gu hunger
 * day clock, the frame-spread pressure crater, and the entity seek window. Scenes use the
 * committed all-air structure {@code empty9x9x9}; bigger scenes need a bigger committed template.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ModGameTests {

    private static final BlockPos CENTER = new BlockPos(4, 1, 4);
    private static final String HUNGRY_KEY = "guzhenren.item.gu.hungry";
    private static final String STARVED_KEY = "guzhenren.item.gu.starved";
    private ModGameTests() {}

    @GameTest(template = "empty9x9x9", timeoutTicks = 200)
    public static void spiritSpringFlowsBetweenSourcesWithoutMintingNewOnes(GameTestHelper helper) {
        BlockPos middle = CENTER;
        // A closed trough -- stone everywhere at spring height except the three trough cells -- so
        // the gap is the only place the fluid can go; water skips flat cells when a ledge is nearer.
        for (int x = -3; x <= 3; x++) {
            for (int z = -2; z <= 2; z++) {
                helper.setBlock(middle.offset(x, -1, z), Blocks.STONE);
                boolean trough = z == 0 && x >= -1 && x <= 1;
                if (!trough) helper.setBlock(middle.offset(x, 0, z), Blocks.STONE);
            }
        }
        helper.setBlock(middle.offset(-1, 0, 0), ModBlocks.SPIRIT_SPRING.get());
        helper.setBlock(middle.offset(1, 0, 0), ModBlocks.SPIRIT_SPRING.get());
        helper.succeedWhen(() -> helper.assertBlockState(middle,
                state -> state.getFluidState().getType() == ModFluids.FLOWING_SPIRIT_SPRING.get()
                        && !state.getFluidState().isSource(),
                () -> "the gap must fill with the flowing arm, never a new source, but is "
                        + helper.getLevel().getBlockState(helper.absolutePos(middle))));
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 600)
    public static void spiritSpringProducesPrimevalStonesUpToTheCap(GameTestHelper helper) {
        BlockPos spring = CENTER;
        helper.setBlock(spring.below(), Blocks.STONE);
        helper.setBlock(spring, ModBlocks.SPIRIT_SPRING.get());
        ServerPlayer player = survivalMock(helper, null, true);
        BlockPos absoluteSpring = helper.absolutePos(spring);
        player.setPos(absoluteSpring.getX() + 0.5, absoluteSpring.getY() + 1.0, absoluteSpring.getZ() + 0.5);
        helper.succeedWhen(() -> {
            int stones = SpiritSpringBlock.nearbyStones(helper.getLevel(), helper.absolutePos(spring));
            helper.assertTrue(stones >= SpiritSpringBlock.STONES_PER_PRODUCTION, "first batch of stones spawned");
            helper.assertTrue(stones <= SpiritSpringBlock.NEARBY_STONES_CAP, "the cap pauses further batches");
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void spiritSpringStructurePlacesPerSpec(GameTestHelper helper) {
        // Ground level y=1: dirt inside the 7x7 (the original terrain x cells must keep), stone
        // subsurface at y=0. The structure center column is (4,1,4); layer one lands at y=0.
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 7; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.DIRT);
        }
        // Vegetation anchored to the original dirt: above a slab cell, above a full-block cell,
        // two high above a rim cell, and above an x cell (that one must survive).
        helper.setBlock(new BlockPos(2, 2, 4), Blocks.SHORT_GRASS);
        helper.setBlock(new BlockPos(1, 2, 3), Blocks.SHORT_GRASS);
        helper.setBlock(new BlockPos(3, 3, 1), Blocks.SHORT_GRASS);
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.SHORT_GRASS);
        SpiritSpringFeature.placeStructure(helper.getLevel(), helper.absolutePos(CENTER));
        helper.assertBlockState(CENTER.below(), state -> state.is(Blocks.CALCITE),
                () -> "layer-one center must be calcite");
        helper.assertBlockState(new BlockPos(3, 0, 1), state -> state.is(Blocks.MOSSY_COBBLESTONE),
                () -> "layer-one (t) must be mossy cobblestone");
        helper.assertBlockState(new BlockPos(1, 0, 1), state -> state.is(Blocks.STONE),
                () -> "layer-one (x) must keep the original terrain");
        helper.assertBlockState(CENTER, state -> state.is(Blocks.CALCITE),
                () -> "layer-two center must be the calcite pillar");
        helper.assertBlockState(new BlockPos(1, 1, 3), state -> state.is(Blocks.COBBLESTONE),
                () -> "layer-two (y) must be cobblestone");
        helper.assertBlockState(new BlockPos(1, 1, 1), state -> state.is(Blocks.DIRT),
                () -> "layer-two outer (x) must keep the original terrain");
        helper.assertBlockState(new BlockPos(6, 1, 1), state -> state.is(Blocks.DIRT),
                () -> "layer-two outer (x) must keep the original terrain");
        helper.assertBlockState(new BlockPos(3, 1, 3), state -> state.isAir(),
                () -> "the basin well around the pillar must be cleared to air");
        helper.assertBlockState(new BlockPos(4, 1, 3), state -> state.isAir(),
                () -> "the basin well around the pillar must be cleared to air");
        helper.assertBlockState(new BlockPos(2, 2, 4), state -> state.isAir(),
                () -> "grass above a slab cell must be cleared");
        helper.assertBlockState(new BlockPos(1, 2, 3), state -> state.isAir(),
                () -> "grass above a full-block cell must be cleared");
        helper.assertBlockState(new BlockPos(3, 3, 1), state -> state.isAir(),
                () -> "grass two cells above ground must be cleared");
        helper.assertBlockState(new BlockPos(1, 2, 1), state -> state.is(Blocks.SHORT_GRASS),
                () -> "grass above an x cell must survive");
        helper.assertBlockState(new BlockPos(3, 1, 2),
                state -> state.is(Blocks.MOSSY_COBBLESTONE_SLAB)
                        && state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM,
                () -> "layer-two (t1) must be a bottom mossy cobblestone slab");
        helper.assertBlockState(new BlockPos(6, 1, 5),
                state -> state.is(Blocks.COBBLESTONE_SLAB)
                        && state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM,
                () -> "layer-two (y1) must be a bottom cobblestone slab");
        helper.assertBlockState(CENTER.above(),
                state -> state.is(ModBlocks.SPIRIT_SPRING.get()) && state.getFluidState().isSource(),
                () -> "the third layer must be the spirit spring source");
        helper.succeedWhen(() -> {
            helper.assertBlockState(new BlockPos(3, 1, 3),
                    state -> state.getFluidState().getType() == ModFluids.FLOWING_SPIRIT_SPRING.get()
                            && !state.getFluidState().isSource(),
                    () -> "the streams must fill the calcite basin, but is "
                            + helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(3, 1, 3))));
            helper.assertBlockState(new BlockPos(0, 1, 4), state -> state.getFluidState().isEmpty(),
                    () -> "the rim must hold the water, nothing flows outside, but is "
                            + helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(0, 1, 4))));
            helper.assertBlockState(new BlockPos(1, 2, 4), state -> state.getFluidState().isEmpty(),
                    () -> "no sheet may spill over the rim at spring height, but is "
                            + helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(1, 2, 4))));
        });
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void bodyAndDistilledEssenceAdditionsDoNotWrap(GameTestHelper helper) {
        ServerPlayer player = storagePlayer(helper);
        BodyService.addAge(player, Long.MAX_VALUE);
        helper.assertValueEqual(BodyService.get(player).ageParts(), Long.MAX_VALUE, "age addition saturates");
        BodyService.addLifespan(player, Long.MAX_VALUE);
        helper.assertValueEqual(BodyService.get(player).lifespanParts(), Long.MAX_VALUE, "lifespan addition saturates");
        BodyService.setLifespan(player, Long.MIN_VALUE);
        BodyService.addLifespan(player, Long.MAX_VALUE);
        helper.assertValueEqual(BodyService.get(player).lifespanParts(), Long.MAX_VALUE,
                "year conversion must not saturate before adding signed lifespan");
        var aperture = ApertureService.aperture(player).withDistilling(true).withDistilledEssence(1L);
        ApertureService.set(player, ApertureData.PRIMARY, aperture);
        com.unknown.guzhenren.attachment.service.aperture.ApertureEssenceService.addDistilled(player, Long.MAX_VALUE);
        helper.assertTrue(ApertureService.aperture(player).distilledEssence() > 0L,
                "positive distilled essence addition emptied the pool");
        helper.succeed();
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void deathQiRefundPreservesLargeFraction(GameTestHelper helper) {
        ServerPlayer player = storagePlayer(helper);
        player.setData(ModAttachments.BODY, BodyService.get(player).withLifespanParts(0L)
                .withDeathQiLifespanLost(30_000_000_000_000L));
        BodyService.refundDeathQiDebt(player, 3, 4);
        helper.assertValueEqual(BodyService.get(player).lifespanParts(), 3_240_000_000_000_000_000L,
                "refund multiplication must retain the exact three-quarter result");
        helper.assertValueEqual(BodyService.get(player).deathQiLifespanLost(), 0L, "refund clears the debt");
        player.setData(ModAttachments.BODY, BodyService.get(player).withLifespanParts(Long.MIN_VALUE)
                .withDeathQiLifespanLost(Long.MAX_VALUE));
        BodyService.refundDeathQiDebt(player, 3, 4);
        helper.assertValueEqual(BodyService.get(player).lifespanParts(), Long.MAX_VALUE,
                "refund must saturate only after adding signed lifespan");
        helper.succeed();
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void soulAddCannotWrapIntoDeath(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, null, true);
        SoulService.addMax(player, Long.MAX_VALUE);
        helper.assertValueEqual(SoulService.get(player).maxSoul(), Long.MAX_VALUE, "soul cap saturates");
        SoulService.addCurrent(player, Long.MAX_VALUE);
        helper.assertValueEqual(SoulService.get(player).currentSoul(), Long.MAX_VALUE, "soul current saturates");
        SoulService.addCurrent(player, Long.MIN_VALUE);
        helper.assertValueEqual(SoulService.get(player).currentSoul(), 0L, "negative delta clamps to zero");
        player.getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4),
                "gzr soul max sub -9223372036854775808");
        helper.assertValueEqual(SoulService.get(player).maxSoul(), Long.MAX_VALUE, "subtraction cannot negate MIN");
        helper.succeed();
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void mindAddAndRegenerationSaturate(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, null, true);
        var thoughts = com.unknown.guzhenren.custom.enums.wisdom.WisdomType.THOUGHTS;
        MindService.addMax(player, thoughts, Long.MAX_VALUE);
        helper.assertValueEqual(MindService.max(player, thoughts), Long.MAX_VALUE, "mind cap saturates");
        MindService.setCurrent(player, thoughts, Long.MAX_VALUE - 1L);
        MindService.setBrilliance(player, com.unknown.guzhenren.custom.enums.wisdom.Brilliance.OUTSTANDING);
        MindService.regenStep(player);
        helper.assertValueEqual(MindService.current(player, thoughts), Long.MAX_VALUE, "regen saturates");
        MindService.addCurrent(player, thoughts, 1L);
        helper.assertValueEqual(MindService.current(player, thoughts), Long.MAX_VALUE, "mind current saturates");
        helper.succeed();
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void pathResourceAdditionSaturates(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, null, true);
        var tag = com.unknown.guzhenren.custom.enums.path.MarkTag.NATURAL;
        var qi = com.unknown.guzhenren.custom.enums.qi.QiKind.HUMAN;
        PathService.setMark(player, GuPath.STRENGTH, tag, 1L);
        PathService.addMark(player, GuPath.STRENGTH, tag, Long.MAX_VALUE);
        helper.assertValueEqual(PathService.mark(player, GuPath.STRENGTH, tag), Long.MAX_VALUE, "marks saturate");
        PathQiService.set(player, qi, 1L);
        PathQiService.add(player, qi, Long.MAX_VALUE);
        helper.assertValueEqual(PathQiService.current(player, qi), Long.MAX_VALUE, "qi saturates");
        var strength = com.unknown.guzhenren.custom.enums.qi.QiKind.STRENGTH;
        PathQiService.set(player, strength, 640L);
        helper.assertTrue(player.hasEffect(com.unknown.guzhenren.registry.effect.ModEffects.STRENGTH_QI),
                "held strength qi projects its effect");
        player.setData(ModAttachments.QI, PathQiService.get(player).with(strength,
                new com.unknown.guzhenren.attachment.data.path.PathQiEntry(640L, 0L)));
        PathQiService.syncEffects(player);
        helper.assertTrue(!player.hasEffect(com.unknown.guzhenren.registry.effect.ModEffects.STRENGTH_QI),
                "expired hold removes the effect even before all qi decays");
        helper.succeed();
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void sceneSupportsBlockPlacement(GameTestHelper helper) {
        helper.setBlock(CENTER, Blocks.DIRT);
        helper.succeedWhen(() -> helper.assertBlockState(CENTER, state -> state.is(Blocks.DIRT), () -> "dirt missing"));
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void tendedGuHungerClock(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer player = survivalMock(helper, inbox, false);
        ItemStack gu = new ItemStack(ModItems.WHITE_BOAR_GU.get());
        gu.set(ModDataComponents.REFINED_GU_STATE.get(), new RefinedGuState(true, 0, 0, 3, 0));
        player.getInventory().setItem(0, gu);

        TendedGuItem.tickCarried(player, 0L);
        helper.assertValueEqual(TendedGuItem.state(gu).hunger(), 3, "hunger without a day boundary");
        helper.assertTrue(messages(inbox, HUNGRY_KEY).isEmpty(), "hungry broadcast without a day boundary");

        TendedGuItem.tickCarried(player, 1L);
        helper.assertValueEqual(TendedGuItem.state(gu).hunger(), 2, "hunger after one day");
        helper.assertTrue(messages(inbox, HUNGRY_KEY).isEmpty(), "hungry broadcast while above the threshold");

        TendedGuItem.tickCarried(player, 1L);
        helper.assertValueEqual(TendedGuItem.state(gu).hunger(), 1, "hunger at the hungry threshold");
        helper.assertValueEqual(messages(inbox, HUNGRY_KEY).size(), 1, "hungry broadcast count");

        TendedGuItem.tickCarried(player, 0L);
        helper.assertValueEqual(messages(inbox, HUNGRY_KEY).size(), 1, "hungry broadcast latched to once per day");

        TendedGuItem.tickCarried(player, 1L);
        helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "starved Gu was removed from the inventory");
        helper.assertValueEqual(TendedGuItem.state(gu).hunger(), 0, "hunger after starving");
        helper.assertValueEqual(messages(inbox, STARVED_KEY).size(), 1, "starved broadcast count");
        helper.succeed();
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 200)
    public static void pressureExplosionSpreadsOverTicks(GameTestHelper helper) {
        for (int x = 2; x <= 6; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = 2; z <= 6; z++) helper.setBlock(new BlockPos(x, y, z), Blocks.DIRT);
            }
        }
        Vec3 center = helper.absoluteVec(new Vec3(4.5D, 1.5D, 4.5D));
        AperturePressureExplosionTask.start(helper.getLevel(), center.x, center.y, center.z, 2, ExtremePhysique.NONE);

        helper.succeedWhen(() -> {
            for (int x = 2; x <= 6; x++) {
                for (int y = 0; y <= 2; y++) {
                    for (int z = 2; z <= 6; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        boolean inner = x >= 3 && x <= 5 && z >= 3 && z <= 5;
                        helper.assertBlockState(pos, state -> inner == state.isAir(),
                                () -> inner ? "crater block survived" : "rim block was cleared");
                    }
                }
            }
            helper.assertTrue(helper.getEntities(EntityType.ITEM).isEmpty(), "crater dropped item entities");
        });
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 200)
    public static void hopeGuSeeksUnawakenedPlayer(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, null, true);
        Vec3 spot = helper.absoluteVec(new Vec3(4.5D, 1.5D, 0.5D));
        player.moveTo(spot.x, spot.y, spot.z, 0.0F, 0.0F);
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);

        HopeGuEntity gu = helper.spawn(ModEntityTypes.HOPE_GU_ENTITY.get(), CENTER);
        Vec3 start = gu.position();
        Vec3 towardPlayer = player.position().subtract(start);
        double startDistance = towardPlayer.length();
        helper.assertTrue(startDistance > FlyingGuEntity.HOVER_RANGE, "player started inside hover range");
        helper.assertTrue(startDistance <= FlyingGuEntity.DETECT_RANGE, "player started outside detect range");

        helper.succeedWhen(() -> {
            helper.assertTrue(gu.isAlive(), "hope gu vanished");
            Vec3 travel = gu.position().subtract(start);
            helper.assertTrue(travel.dot(towardPlayer) > 1.0D, "hope gu did not fly toward the player");
            helper.assertTrue(gu.distanceTo(player) < startDistance - 1.0D, "hope gu distance did not shrink");
        });
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 1000)
    public static void boarGuVariantsWanderWithoutSeekingPlayers(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, null, false);
        List<BoarGuEntity> variants = List.of(
                helper.spawn(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(), new BlockPos(2, 1, 2)),
                helper.spawn(ModEntityTypes.BLACK_BOAR_GU_ENTITY.get(), new BlockPos(4, 1, 2)),
                helper.spawn(ModEntityTypes.FLOWER_BOAR_GU_ENTITY.get(), new BlockPos(6, 1, 2)));
        List<Vec3> starts = variants.stream().map(BoarGuEntity::position).toList();
        for (BoarGuEntity variant : variants) {
            variant.getRandom().setSeed(42L);
            variant.setYRot(0.0F);
            helper.assertTrue(!variant.seeks(player), "boar gu seeks player");
        }
        boolean[] horizontal = new boolean[variants.size()];
        boolean[] vertical = new boolean[variants.size()];
        helper.succeedWhen(() -> {
            for (int i = 0; i < variants.size(); i++) {
                Vec3 travel = variants.get(i).position().subtract(starts.get(i));
                horizontal[i] |= Math.sqrt(travel.x * travel.x + travel.z * travel.z) > 0.05D;
                vertical[i] |= Math.abs(travel.y) > 0.05D;
            }
            for (int i = 0; i < variants.size(); i++) {
                helper.assertTrue(horizontal[i], "boar gu did not wander horizontally: " + i);
                helper.assertTrue(vertical[i], "boar gu did not wander vertically: " + i);
            }
        });
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 200)
    public static void boarGuFleesNearbyPlayer(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, null, true);
        player.moveTo(helper.absoluteVec(new Vec3(4.5D, 1.5D, 1.5D)));
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);

        BoarGuEntity gu = helper.spawn(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(), CENTER);
        helper.assertTrue(gu.distanceTo(player) <= BoarGuEntity.FLEE_RANGE, "player started outside flee range");

        helper.succeedWhen(() -> helper.assertTrue(gu.distanceTo(player) > BoarGuEntity.FLEE_RANGE,
                "boar gu stayed within flee range of the player"));
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void boarGuVariantsCatchAsMatchingItems(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, null, true);
        catchBoar(helper, player, ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(), ModItems.WHITE_BOAR_GU.get());
        catchBoar(helper, player, ModEntityTypes.BLACK_BOAR_GU_ENTITY.get(), ModItems.BLACK_BOAR_GU.get());
        catchBoar(helper, player, ModEntityTypes.FLOWER_BOAR_GU_ENTITY.get(), ModItems.FLOWER_BOAR_GU.get());
        helper.succeed();
    }
    private static void catchBoar(GameTestHelper helper, ServerPlayer player, EntityType<BoarGuEntity> type,
                                  Item item) {
        BoarGuEntity gu = helper.spawn(type, CENTER);
        helper.assertTrue(gu.interact(player, InteractionHand.MAIN_HAND).consumesAction(),
                "boar gu interaction was not consumed");
        helper.assertValueEqual(player.getInventory().countItem(item), 1, "matching gu item missing");
        helper.assertTrue(!gu.isAlive(), "caught boar gu was not discarded");
        helper.assertTrue(helper.getEntities(EntityType.ITEM).isEmpty(), "caught boar gu dropped an item entity");
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void secondOnlyThenFirstApertureFlow(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, null, true);

        ApertureService.openSecondary(player, Rank.THREE);
        helper.assertValueEqual(ApertureService.get(player).count(), 1, "second-only aperture count");
        helper.assertTrue(ApertureService.aperture(player, 0).second(), "lone aperture must be flagged second");
        helper.assertTrue(!ApertureService.isAwakened(player), "second-only holder must not read awakened");
        helper.assertTrue(ApertureService.hasAperture(player), "second-only holder must read hasAperture");

        ItemStack vital = new ItemStack(ModItems.WHITE_BOAR_GU.get());
        ApertureStorageService.setVital(player, 0, vital);
        ApertureStorageService.set(player, 0, List.of(new ItemStack(ModItems.WHITE_BOAR_GU.get())));
        ApertureNourishService.start(player, 0);
        helper.assertTrue(ApertureNourishService.isCultivating(player), "nourish started on the lone second");

        ApertureService.awaken(player, 80);
        ApertureData data = ApertureService.get(player);
        helper.assertValueEqual(data.count(), 2, "count after Hope Gu inserts the first aperture");
        helper.assertValueEqual(data.firstIndex(), 0, "first aperture takes position 0");
        helper.assertValueEqual(data.secondIndex(), 1, "second aperture slides to position 1");
        helper.assertTrue(data.get(0).rank() == Rank.ONE, "first aperture rank");
        helper.assertValueEqual(data.get(1).rank(), Rank.THREE, "second aperture kept its rank");
        helper.assertTrue(GuItem.boundAperture(ApertureStorageService.vital(player, 1)) == 1,
                "vital gu binding followed the slide");
        helper.assertTrue(ApertureStorageService.items(player, 0).isEmpty(), "old store moved off position 0");
        helper.assertValueEqual(ApertureStorageService.items(player, 1).size(), 1, "store followed the slide");
        helper.assertTrue(ApertureNourishService.isCultivating(player)
                && ApertureNourishService.targetIndex(player) == 1, "nourish target followed the slide");

        ApertureService.openSecondary(player, Rank.FIVE);
        ApertureData upgraded = ApertureService.get(player);
        helper.assertValueEqual(upgraded.count(), 2, "upgrade keeps two apertures");
        helper.assertValueEqual(upgraded.secondIndex(), 1, "upgrade overwrites in place");
        helper.assertValueEqual(upgraded.get(1).rank(), Rank.FIVE, "upgrade lands the gu rank");
        helper.assertValueEqual(upgraded.get(1).primaryPath(), GuPath.STRENGTH, "upgrade keeps the bound path");
        helper.succeed();
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void storageMousePlacementKeepsOverflow(GameTestHelper helper) {
        ServerPlayer player = storagePlayer(helper);
        ApertureStorageMenu menu = new ApertureStorageMenu(1, player.getInventory(), ApertureData.PRIMARY, 0);
        menu.setCarried(new ItemStack(ModItems.SECOND_APERTURE_GU_5.get(), 64));

        menu.clicked(0, 0, ClickType.PICKUP, player);

        helper.assertValueEqual(storedCount(player, ApertureData.PRIMARY), 8, "mouse placement accepted count");
        helper.assertValueEqual(menu.getCarried().getCount(), 56, "mouse placement kept overflow");
        helper.succeed();
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void storageShiftMoveKeepsOverflow(GameTestHelper helper) {
        ServerPlayer player = storagePlayer(helper);
        player.getInventory().setItem(0, new ItemStack(ModItems.SECOND_APERTURE_GU_5.get(), 64));
        ApertureStorageMenu menu = new ApertureStorageMenu(1, player.getInventory(), ApertureData.PRIMARY, 0);

        menu.clicked(ApertureStorageMenu.PAGE_SIZE + 27, 0, ClickType.QUICK_MOVE, player);

        helper.assertValueEqual(storedCount(player, ApertureData.PRIMARY), 8, "shift move accepted count");
        helper.assertValueEqual(player.getInventory().getItem(0).getCount(), 56, "shift move kept overflow");
        helper.succeed();
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void storageSyncedLoadLimitsSlot(GameTestHelper helper) {
        ServerPlayer player = storagePlayer(helper);
        ApertureStorageMenu menu = new ApertureStorageMenu(1, player.getInventory(), ApertureData.PRIMARY, 0);
        menu.setData(2, 224);
        ItemStack incoming = new ItemStack(ModItems.SECOND_APERTURE_GU_5.get(), 64);

        helper.assertValueEqual(menu.getSlot(0).getMaxStackSize(incoming), 1, "synced load slot limit");
        helper.succeed();
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void storageLegacyOverloadCanBeReducedBySwap(GameTestHelper helper) {
        ServerPlayer player = storagePlayer(helper);
        player.setData(ModAttachments.APERTURE_STORAGE, ApertureStorage.DEFAULT.with(ApertureData.PRIMARY, List.of(
                new ItemStack(ModItems.SECOND_APERTURE_GU_5.get(), 9),
                new ItemStack(ModItems.SECOND_APERTURE_GU_5.get()))));
        ApertureStorageMenu menu = new ApertureStorageMenu(1, player.getInventory(), ApertureData.PRIMARY, 0);
        menu.setCarried(new ItemStack(ModItems.SECOND_APERTURE_GU_1.get()));

        menu.clicked(1, 0, ClickType.PICKUP, player);

        helper.assertTrue(ApertureStorageService.items(player, ApertureData.PRIMARY).get(1)
                .is(ModItems.SECOND_APERTURE_GU_1.get()), "legacy overload replacement was blocked");
        helper.assertValueEqual(ApertureStorageService.load(player, ApertureData.PRIMARY), 290,
                "legacy overload reduced load");
        helper.assertTrue(menu.getCarried().is(ModItems.SECOND_APERTURE_GU_5.get()),
                "legacy overload kept replaced gu on cursor");
        helper.succeed();
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void healthFollowsFirstApertureOnly(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, null, true);

        helper.assertValueEqual(player.getMaxHealth(), 20.0F, "mortal max health");
        ApertureService.openSecondary(player, Rank.THREE);
        helper.assertValueEqual(player.getMaxHealth(), 20.0F, "lone second aperture keeps mortal health");
        ApertureService.awaken(player, 80);
        helper.assertValueEqual(player.getMaxHealth(), 20.0F, "rank one first aperture keeps 20");
        ApertureService.setRank(player, ApertureData.PRIMARY, Rank.THREE);
        helper.assertValueEqual(player.getMaxHealth(), 60.0F, "first aperture rank three lifts to 60");
        ApertureService.openSecondary(player, Rank.FIVE);
        helper.assertValueEqual(player.getMaxHealth(), 60.0F, "second aperture never touches health");
        helper.succeed();
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void infoModelAlwaysEmitsApertureTitleRows(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, null, true);

        helper.assertValueEqual(titles(player), List.of(new InfoModel.ApertureIndex(1, 0)),
                "mortal keeps the clickable first-aperture title row");
        ApertureService.awaken(player, 80);
        helper.assertValueEqual(titles(player), List.of(new InfoModel.ApertureIndex(1, 0)),
                "first-only holder keeps the clickable title row");
        ApertureService.openSecondary(player, Rank.THREE);
        helper.assertValueEqual(titles(player), List.of(new InfoModel.ApertureIndex(1, 0),
                new InfoModel.ApertureIndex(2, 1)), "two apertures keep both clickable title rows");
        helper.succeed();
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void emptyApertureCancelsStaleNourishing(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, null, true);
        player.setData(ModAttachments.NOURISH, ApertureNourishData.DEFAULT.withCultivating(true));
        helper.assertValueEqual(ApertureNourishService.targetIndex(player), ApertureData.PRIMARY,
                "empty aperture has a safe UI target");
        ApertureNourishService.tickNourish(player);
        helper.assertTrue(!ApertureNourishService.isCultivating(player), "stale session canceled without apertures");
        helper.succeed();
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void nourishmentRejectsInvalidAndRepeatedStarts(GameTestHelper helper) {
        ServerPlayer player = storagePlayer(helper);
        for (int index : new int[]{-1, Integer.MIN_VALUE, 1, Integer.MAX_VALUE}) {
            helper.assertTrue(!ApertureNourishService.canNourish(player, index), "invalid target must be rejected");
            ApertureNourishService.start(player, index);
            helper.assertTrue(!ApertureNourishService.isCultivating(player), "invalid start changed session");
        }
        ApertureNourishService.start(player, 0);
        ApertureNourishData session = ApertureNourishService.get(player).withStarvedSinceTick(0L);
        player.setData(ModAttachments.NOURISH, session);
        ApertureNourishService.start(player, 0);
        helper.assertValueEqual(ApertureNourishService.get(player), session, "repeated start reset starvation anchor");
        helper.succeed();
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void deathCloneKeepsDataAndCopiesStorage(GameTestHelper helper) {
        ServerPlayer from = storagePlayer(helper);
        PlayerDataService.onBirth(from);
        BodyService.setExtremePhysique(from, ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL);
        BodyService.addPhysique(from, Physique.ZOMBIE);
        ApertureStorageService.set(from, 0, List.of(new ItemStack(ModItems.SECOND_APERTURE_GU_1.get(), 2)));
        ServerPlayer to = survivalMock(helper, null, true);
        PlayerDataService.onClone(from, to, true, true);
        PlayerDataService.onRespawn(to);
        helper.assertValueEqual(ApertureService.get(to), ApertureService.get(from),
                "death with keepInventory lost apertures");
        helper.assertValueEqual(to.getData(ModAttachments.MIND).brilliance(),
                from.getData(ModAttachments.MIND).brilliance(),
                "retained death rerolled brilliance");
        helper.assertTrue(to.getData(ModAttachments.BORN), "clone lost birth latch");
        helper.assertTrue(BodyService.isExtreme(to) && !BodyService.isUndead(to),
                "revival lost extreme or kept undead");
        ApertureStorageService.items(to, 0).getFirst().shrink(1);
        helper.assertValueEqual(ApertureStorageService.items(from, 0).getFirst().getCount(), 2,
                "cloned storage shares mutable stacks with original");
        helper.succeed();
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void deathResetClearsExtremeAndNourishment(GameTestHelper helper) {
        ServerPlayer from = storagePlayer(helper);
        BodyService.setExtremePhysique(from, ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL);
        ApertureNourishService.start(from, 0);
        ServerPlayer to = survivalMock(helper, null, true);
        PlayerDataService.onClone(from, to, true, false);
        PlayerDataService.onRespawn(to);
        helper.assertTrue(!ApertureService.hasAperture(to), "reset retained apertures");
        helper.assertTrue(!BodyService.isExtreme(to), "reset retained extreme physique");
        helper.assertTrue(!ApertureNourishService.isCultivating(to), "reset retained nourishment");
        helper.assertTrue(to.getData(ModAttachments.BORN), "reset did not initialize newborn");
        helper.succeed();
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void nonDeathCloneIgnoresKeepInventory(GameTestHelper helper) {
        ServerPlayer from = storagePlayer(helper);
        ApertureService.openSecondary(from, Rank.THREE);
        PlayerDataService.onBirth(from);
        ServerPlayer to = survivalMock(helper, null, true);
        PlayerDataService.onClone(from, to, false, false);
        helper.assertValueEqual(ApertureService.get(to), ApertureService.get(from), "dimension clone lost apertures");
        helper.assertValueEqual(to.getData(ModAttachments.MIND), from.getData(ModAttachments.MIND),
                "dimension clone lost mind");
        helper.assertTrue(to.getData(ModAttachments.BORN), "dimension clone lost birth latch");
        helper.succeed();
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void epicFightDoesNotTargetWildGu(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, null, true);
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        helper.assertTrue(patch != null, "Epic Fight player patch missing");
        SetTargetEvent hope = new SetTargetEvent(patch, helper.spawn(ModEntityTypes.HOPE_GU_ENTITY.get(), CENTER));
        EpicFightEventHooks.Player.SET_TARGET.post(hope);
        helper.assertTrue(hope.isCanceled(), "Hope Gu target was not canceled");
        SetTargetEvent boar = new SetTargetEvent(patch,
                helper.spawn(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(), CENTER));
        EpicFightEventHooks.Player.SET_TARGET.post(boar);
        helper.assertTrue(boar.isCanceled(), "boar Gu target was not canceled");
        SetTargetEvent pig = new SetTargetEvent(patch, helper.spawn(EntityType.PIG, CENTER));
        EpicFightEventHooks.Player.SET_TARGET.post(pig);
        helper.assertTrue(!pig.isCanceled(), "ordinary mob target was canceled");
        helper.succeed();
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void epicFightOnlyWaivesUndeadStamina(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, null, true);
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        helper.assertTrue(patch != null, "Epic Fight player patch missing");
        helper.assertValueEqual(skillCost(patch, Skill.Resource.STAMINA), 5.0F, "living stamina cost changed");
        BodyService.addPhysique(player, Physique.HALF_ZOMBIE);
        helper.assertValueEqual(skillCost(patch, Skill.Resource.STAMINA), 0.0F, "half-zombie still pays stamina");
        BodyService.addPhysique(player, Physique.ZOMBIE);
        helper.assertValueEqual(skillCost(patch, Skill.Resource.STAMINA), 0.0F, "zombie still pays stamina");
        helper.assertValueEqual(skillCost(patch, Skill.Resource.HEALTH), 5.0F, "non-stamina cost was waived");
        helper.succeed();
    }
    private static float skillCost(ServerPlayerPatch patch, Skill.Resource resource) {
        SkillConsumeEvent event = new SkillConsumeEvent(patch, EpicFightSkills.STEP.get(), resource, 5.0F,
                new CompoundTag());
        EpicFightEventHooks.Player.CONSUME_SKILL.post(event);
        return event.getAmount();
    }
    private static List<InfoModel.ApertureIndex> titles(ServerPlayer player) {
        return InfoModel.aperture(player).stream().map(InfoModel.Row::entry)
                .filter(InfoModel.ApertureIndex.class::isInstance)
                .map(InfoModel.ApertureIndex.class::cast)
                .toList();
    }
    private static int storedCount(ServerPlayer player, int aperture) {
        return ApertureStorageService.items(player, aperture).stream().mapToInt(ItemStack::getCount).sum();
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void offlineVitalGuDeathIsRecordedAndSurvivesSave(GameTestHelper helper) {
        ServerPlayer holder = survivalMock(helper, null, true);
        UUID offlineOwner = UUID.randomUUID();
        PendingVitalPenalties ledger = PendingVitalPenalties.get(helper.getLevel().getServer());

        TendedGuItem.starved(holder, vitalOwnedBy(offlineOwner, ApertureData.PRIMARY));
        helper.assertValueEqual(ledger.count(offlineOwner), 1, "offline owner's vital loss recorded");

        RegistryAccess registries = helper.getLevel().registryAccess();
        PendingVitalPenalties reloaded = PendingVitalPenalties.load(
                ledger.save(new CompoundTag(), registries), registries);
        ItemStack restored = reloaded.poll(offlineOwner);
        helper.assertTrue(restored != null && restored.is(ModItems.WHITE_BOAR_GU.get())
                && offlineOwner.equals(GuItem.owner(restored)), "pending loss survives a save/load round trip");
        ledger.poll(offlineOwner);
        helper.assertValueEqual(ledger.count(offlineOwner), 0, "ledger drained");
        helper.succeed();
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void offlineVitalPenaltyWaitsOutSpawnInvulnerability(GameTestHelper helper) {
        ServerPlayer owner = survivalMock(helper, null, true);
        ApertureService.awaken(owner, 80);
        PendingVitalPenalties ledger = PendingVitalPenalties.get(helper.getLevel().getServer());
        ledger.record(owner.getUUID(), vitalOwnedBy(owner.getUUID(), ApertureData.PRIMARY));

        PlayerDataService.settleOfflineVitalLoss(owner);
        serverTicks(owner, PlayerDataService.OFFLINE_VITAL_SETTLE_AFTER_TICKS);
        helper.assertValueEqual(owner.tickCount, PlayerDataService.OFFLINE_VITAL_SETTLE_AFTER_TICKS, "tick count");
        helper.assertValueEqual(ledger.count(owner.getUUID()), 1,
                "nothing settles at login or on the heartbeats inside spawn invulnerability");
        ledger.poll(owner.getUUID());
        helper.succeed();
    }
    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void offlineVitalPenaltiesSettleOnePerHeartbeat(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer owner = survivalMock(helper, inbox, true);
        ApertureService.awaken(owner, 80);
        serverTicks(owner, PlayerDataService.OFFLINE_VITAL_SETTLE_AFTER_TICKS);
        SoulService.setMax(owner, 1_000L);
        SoulService.setCurrent(owner, 1_000L);
        for (WisdomType type : WisdomType.values()) {
            MindService.setMax(owner, type, 1_000_000L);
            MindService.setCurrent(owner, type, 1_000_000L);
        }
        ApertureService.setPrimaryPath(owner, ApertureData.PRIMARY, GuPath.TIME);
        PendingVitalPenalties ledger = PendingVitalPenalties.get(helper.getLevel().getServer());
        ledger.record(owner.getUUID(), vitalOwnedBy(owner.getUUID(), ApertureData.PRIMARY));
        ledger.record(owner.getUUID(), vitalOwnedBy(owner.getUUID(), ApertureData.PRIMARY));
        float healthBefore = owner.getHealth();

        serverTicks(owner, Ticks.SECOND);
        helper.assertValueEqual(owner.tickCount, 80, "first heartbeat past spawn invulnerability");
        helper.assertValueEqual(ledger.count(owner.getUUID()), 1, "one loss settles on the first heartbeat");
        helper.assertValueEqual(SoulService.get(owner).currentSoul(), 500L, "first settle halves the soul");
        for (WisdomType type : WisdomType.values()) {
            helper.assertTrue(MindService.current(owner, type) < 1_000_000L, "first settle halves " + type);
        }
        helper.assertTrue(owner.getHealth() < healthBefore * 0.5F, "first settle's hurt landed");
        helper.assertTrue(ApertureService.aperture(owner, ApertureData.PRIMARY).primaryPath() == null,
                "first settle clears the bound aperture's primary path");
        helper.assertValueEqual(messages(inbox, "guzhenren.item.gu.vital_lost").size(), 1, "one loss message");
        float healthAfterFirst = owner.getHealth();

        serverTicks(owner, Ticks.SECOND);
        helper.assertValueEqual(ledger.count(owner.getUUID()), 0, "second loss settles on the next heartbeat");
        helper.assertValueEqual(SoulService.get(owner).currentSoul(), 250L, "second settle halves again");
        helper.assertTrue(owner.getHealth() < healthAfterFirst, "second hurt cleared the hurt cooldown");
        helper.assertValueEqual(messages(inbox, "guzhenren.item.gu.vital_lost").size(), 2, "two loss messages");
        helper.succeed();
    }
    // One real server tick, as ServerLevel.tickNonPassenger and the connection run it: the level bumps
    // tickCount and calls tick() (spawn invulnerability counts down there); doTick() runs Player.tick, which
    // fires PlayerTickEvent -- the heartbeat. A test body runs inside one game tick, so it replays them here.
    private static void serverTicks(ServerPlayer player, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            player.tickCount++;
            player.tick();
            player.doTick();
        }
    }
    private static ItemStack vitalOwnedBy(UUID owner, int aperture) {
        ItemStack vital = new ItemStack(ModItems.WHITE_BOAR_GU.get());
        vital.set(ModDataComponents.VITAL_OWNER.get(), owner);
        vital.set(ModDataComponents.VITAL_APERTURE.get(), aperture);
        return vital;
    }
    private static ServerPlayer storagePlayer(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, null, true);
        ApertureService.awaken(player, 80);
        return player;
    }
    private static List<Component> messages(List<Component> inbox, String key) {
        List<Component> hits = new ArrayList<>();
        for (Component message : inbox) {
            if (message.getContents() instanceof TranslatableContents content && content.getKey().equals(key)) {
                hits.add(message);
            }
        }
        return hits;
    }
    private static ServerPlayer survivalMock(GameTestHelper helper, @Nullable List<Component> inbox, boolean connect) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), "gzr-gametest-mock"), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {return false;}
            @Override
            public boolean isCreative() {return false;}
            @Override
            public void sendSystemMessage(@NotNull Component message) {if (inbox != null) inbox.add(message);}
        };
        if (!connect) {
            player.getAbilities().instabuild = false;
            return player;
        }
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
