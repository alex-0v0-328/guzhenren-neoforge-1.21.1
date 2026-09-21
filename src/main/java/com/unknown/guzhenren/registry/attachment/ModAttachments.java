package com.unknown.guzhenren.registry.attachment;

import com.mojang.serialization.Codec;
import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.attachment.data.aperture.ApertureData;
import com.unknown.guzhenren.attachment.data.aperture.ApertureNourishData;
import com.unknown.guzhenren.attachment.data.aperture.ApertureStorage;
import com.unknown.guzhenren.attachment.data.body.BodyData;
import com.unknown.guzhenren.attachment.data.dimension.DimensionReturnData;
import com.unknown.guzhenren.attachment.data.mind.MindData;
import com.unknown.guzhenren.attachment.data.path.PathData;
import com.unknown.guzhenren.attachment.data.path.PathQiData;
import com.unknown.guzhenren.attachment.data.path.PathStrengthData;
import com.unknown.guzhenren.attachment.data.soul.SoulData;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Every data attachment this mod puts on a player.
 *
 * <p>DeferredRegister holder: the nine record attachments (immutable, written only through their
 * service) plus the two scratch fields ({@code ESSENCE_CARRY}, {@code BORN}).
 * Synced ones use {@code OWNER_ONLY}; the storage and scratch fields are sync-less.
 *
 * <p>⚠ The data and service layers are split into five domain packages (aperture/body/soul/path/mind);
 * {@code qi} and {@code strength} are subdomains living in the path package, and class names carry
 * their package prefix. Never give an attachment the bare domain word --
 * {@code qi}/{@code soul}/{@code strength} are also {@code GuPath} names.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModAttachments {

    private ModAttachments() {}
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Guzhenren.MOD_ID);
    private static final BiPredicate<IAttachmentHolder, ServerPlayer> OWNER_ONLY =
            (holder, viewer) -> holder == viewer;

    //region Aperture [空窍]
    public static final Supplier<AttachmentType<ApertureData>> APERTURE = ATTACHMENT_TYPES.register(
            "aperture_data", () -> AttachmentType.builder(() -> ApertureData.DEFAULT)
                    .serialize(ApertureData.CODEC)
                    .sync(OWNER_ONLY, ApertureData.STREAM_CODEC)
                    .build());
    public static final Supplier<AttachmentType<float[]>> ESSENCE_CARRY = ATTACHMENT_TYPES.register(
            "essence_carry", () -> AttachmentType.builder(
                    () -> new float[ApertureData.MAX_APERTURES]).build());
    public static final Supplier<AttachmentType<ApertureStorage>> APERTURE_STORAGE = ATTACHMENT_TYPES.register(
            "aperture_storage", () -> AttachmentType.builder(() -> ApertureStorage.DEFAULT)
                    .serialize(ApertureStorage.CODEC)
                    .build());
    public static final Supplier<AttachmentType<ApertureNourishData>> NOURISH = ATTACHMENT_TYPES.register(
            "nourish_data", () -> AttachmentType.builder(() -> ApertureNourishData.DEFAULT)
                    .serialize(ApertureNourishData.CODEC)
                    .sync(OWNER_ONLY, ApertureNourishData.STREAM_CODEC)
                    .build());
    //endregion

    //region Body [肉身]
    public static final Supplier<AttachmentType<BodyData>> BODY = ATTACHMENT_TYPES.register(
            "body_data", () -> AttachmentType.builder(() -> BodyData.DEFAULT)
                    .serialize(BodyData.CODEC)
                    .sync(OWNER_ONLY, BodyData.STREAM_CODEC)
                    .build());
    public static final Supplier<AttachmentType<SoulData>> SOUL = ATTACHMENT_TYPES.register(
            "soul_data", () -> AttachmentType.builder(() -> SoulData.DEFAULT)
                    .serialize(SoulData.CODEC)
                    .sync(OWNER_ONLY, SoulData.STREAM_CODEC)
                    .build());
    public static final Supplier<AttachmentType<PathData>> PATH = ATTACHMENT_TYPES.register(
            "path_data", () -> AttachmentType.builder(() -> PathData.DEFAULT)
                    .serialize(PathData.CODEC)
                    .sync(OWNER_ONLY, PathData.STREAM_CODEC)
                    .build());
    public static final Supplier<AttachmentType<PathStrengthData>> STRENGTH = ATTACHMENT_TYPES.register(
            "strength_data", () -> AttachmentType.builder(() -> PathStrengthData.DEFAULT)
                    .serialize(PathStrengthData.CODEC)
                    .sync(OWNER_ONLY, PathStrengthData.STREAM_CODEC)
                    .build());
    public static final Supplier<AttachmentType<PathQiData>> QI = ATTACHMENT_TYPES.register(
            "qi_data", () -> AttachmentType.builder(() -> PathQiData.DEFAULT)
                    .serialize(PathQiData.CODEC)
                    .sync(OWNER_ONLY, PathQiData.STREAM_CODEC)
                    .build());
    //endregion

    //region Mind [脑海]
    public static final Supplier<AttachmentType<MindData>> MIND = ATTACHMENT_TYPES.register(
            "mind_data", () -> AttachmentType.builder(() -> MindData.DEFAULT)
                    .serialize(MindData.CODEC)
                    .sync(OWNER_ONLY, MindData.STREAM_CODEC)
                    .build());
    //endregion

    //region Dimension travel
    public static final Supplier<AttachmentType<DimensionReturnData>> DIMENSION_RETURN = ATTACHMENT_TYPES.register(
            "dimension_return", () -> AttachmentType.builder(() -> DimensionReturnData.DEFAULT)
                    .serialize(DimensionReturnData.CODEC)
                    .build());
    //endregion

    public static final Supplier<AttachmentType<Boolean>> BORN = ATTACHMENT_TYPES.register(
            "born_flag", () -> AttachmentType.builder(() -> Boolean.FALSE)
                    .serialize(Codec.BOOL)
                    .build());
    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
