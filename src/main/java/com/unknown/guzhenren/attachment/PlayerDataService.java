package com.unknown.guzhenren.attachment;

import com.unknown.guzhenren.attachment.data.aperture.Aperture;
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
import com.unknown.guzhenren.attachment.service.aperture.ApertureEssenceService;
import com.unknown.guzhenren.attachment.service.aperture.ApertureService;
import com.unknown.guzhenren.attachment.service.body.BodyAttackService;
import com.unknown.guzhenren.attachment.service.body.BodyHealthService;
import com.unknown.guzhenren.attachment.service.body.BodyService;
import com.unknown.guzhenren.attachment.service.mind.MindService;
import com.unknown.guzhenren.attachment.service.path.PathQiService;
import com.unknown.guzhenren.attachment.service.soul.SoulService;
import com.unknown.guzhenren.compat.EpicFightIntegration;
import com.unknown.guzhenren.custom.enums.body.ExtremePhysique;
import com.unknown.guzhenren.custom.enums.qi.QiKind;
import com.unknown.guzhenren.custom.enums.wisdom.WisdomType;
import com.unknown.guzhenren.registry.attachment.ModAttachments;
import com.unknown.guzhenren.registry.damage.ModDamageTypes;
import com.unknown.guzhenren.registry.item.ModDataComponents;
import com.unknown.guzhenren.registry.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The one cross-domain lifecycle service: birth, sleep, death, clone, respawn, and a full reset. It is
 * the single place that decides what a clone inherits; every domain service re-runs on join/clone/reset.
 *
 * <p>⚠ The {@code Player} (not {@code ServerPlayer}) signature on {@code copy}/{@code onBirth}/{@code
 * resetAll} is the one carve-out from read-{@code Player}/write-{@code ServerPlayer}: during {@code
 * PlayerEvent.Clone} the fresh entity is typed {@code Player}; never widen a domain service. ⚠ {@code
 * copy} must carry {@code BORN} or the next login re-rolls brilliance. ⚠ A new death needs an {@code
 * onRespawn} line; its un-fire returns BARE values (soul 1, mind 0) the lethal check never fires on.
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureService
 * @see BodyService
 * @since 1.0.0
 */

public final class PlayerDataService {

    private static final String VITAL_LOST = "guzhenren.item.gu.vital_lost";
    private PlayerDataService() {}
    public static void onJoin(@NotNull ServerPlayer player) {
        if (!player.getData(ModAttachments.BORN)) onBirth(player);
        migratePhysique(player);
        ApertureService.syncTalentMarks(player);
        BodyHealthService.refresh(player);
        BodyAttackService.refresh(player);
        EpicFightIntegration.refresh(player);
    }
    private static void migratePhysique(@NotNull ServerPlayer player) {
        Aperture aperture = ApertureService.aperture(player);
        ExtremePhysique legacy = aperture.legacyExtremePhysique();
        if (!BodyService.isExtreme(player) && legacy != null && legacy != ExtremePhysique.NONE) {
            BodyService.setExtremePhysique(player, legacy);
        } else if (!BodyService.isExtreme(player) && aperture.baseEssence() == Aperture.MAX_BASE) {
            ApertureService.set(player, ApertureData.PRIMARY, aperture.withBaseEssence(Aperture.MAX_BASE - 1)
                    .withPressure(0));
        }
        aperture = ApertureService.aperture(player);
        if (aperture.legacyExtremePhysique() != null) {
            ApertureService.set(player, ApertureData.PRIMARY, aperture.clearLegacyExtremePhysique());
        }
    }
    public static void onBirth(@NotNull Player player) {
        player.setData(ModAttachments.MIND, MindData.newborn());
        player.setData(ModAttachments.BORN, true);
    }
    public static void onSleepComplete(@NotNull ServerPlayer player) {
        SoulService.refill(player);
        ApertureEssenceService.refill(player);
        MindService.onSleepComplete(player);
    }
    public static void onClone(@NotNull Player from, @NotNull Player to, boolean wasDeath, boolean keepInventory) {
        if (wasDeath) {
            if (!keepInventory) {
                dropHumanApertures(from);
                resetAll(to);
            } else {
                copy(from, to);
                to.setData(ModAttachments.DIMENSION_RETURN, DimensionReturnData.DEFAULT);
            }
        } else {
            copy(from, to);
        }
        if (to instanceof ServerPlayer server) {
            BodyHealthService.refresh(server);
            BodyAttackService.refresh(server);
            EpicFightIntegration.refresh(server);
        }
    }
    /**
     * A death that wipes the apertures shakes one Human Aperture [人窍] loose per aperture, each at its
     * own rank, at the corpse. keepInventory deaths keep the apertures and drop nothing.
     */
    @SuppressWarnings("resource")
    private static void dropHumanApertures(@NotNull Player from) {
        ApertureData data = from.getData(ModAttachments.APERTURE);
        for (int i = 0; i < data.count(); i++) {
            Item drop = ModItems.humanAperture(data.get(i).rank());
            if (drop == null) continue;
            from.level().addFreshEntity(new ItemEntity(from.level(), from.getX(), from.getY(), from.getZ(),
                    new ItemStack(drop)));
        }
    }
    public static void onRespawn(@NotNull ServerPlayer player) {
        BodyService.revive(player);
        if (ApertureService.pressureFull(player)) ApertureService.setPressure(player, ApertureService.PRIMARY, 0);
        if (BodyService.get(player).isExhausted()) {
            BodyService.setLifespan(player, BodyData.DEFAULT_LIFESPAN);
        }
        if (SoulService.get(player).isCollapsed()) {
            SoulService.revive(player);
        }
        if (MindService.get(player).isOverflowing()) {
            MindService.empty(player);
        }
        BodyService.clearDeathQiDebt(player);
        PathQiService.set(player, QiKind.DEATH, 0L);
    }
    public static void onVitalGuLost(@NotNull ServerPlayer owner, @NotNull ItemStack stack) {
        owner.sendSystemMessage(Component.translatable(VITAL_LOST, stack.getHoverName()));

        SoulService.setCurrent(owner, SoulService.get(owner).currentSoul() / 2L);
        for (WisdomType type : WisdomType.values()) {
            MindService.setCurrent(owner, type, MindService.current(owner, type) / 2L);
        }
        owner.hurt(ModDamageTypes.source(owner, ModDamageTypes.VITAL_GU_LOST), owner.getHealth() * 0.8F);

        int bound = stack.getOrDefault(ModDataComponents.VITAL_APERTURE.get(), ApertureData.PRIMARY);
        ApertureService.setPrimaryPath(owner, bound, null);
    }
    private static void copy(@NotNull Player from, @NotNull Player to) {
        to.setData(ModAttachments.APERTURE, from.getData(ModAttachments.APERTURE));
        to.setData(ModAttachments.APERTURE_STORAGE, from.getData(ModAttachments.APERTURE_STORAGE).copy());
        to.setData(ModAttachments.BODY, from.getData(ModAttachments.BODY));
        to.setData(ModAttachments.SOUL, from.getData(ModAttachments.SOUL));
        to.setData(ModAttachments.PATH, from.getData(ModAttachments.PATH));
        to.setData(ModAttachments.QI, from.getData(ModAttachments.QI));
        to.setData(ModAttachments.STRENGTH, from.getData(ModAttachments.STRENGTH));
        to.setData(ModAttachments.MIND, from.getData(ModAttachments.MIND));
        to.setData(ModAttachments.NOURISH, from.getData(ModAttachments.NOURISH));
        to.setData(ModAttachments.DIMENSION_RETURN, from.getData(ModAttachments.DIMENSION_RETURN));
        to.setData(ModAttachments.BORN, from.getData(ModAttachments.BORN));
    }
    public static void resetAll(@NotNull Player player) {
        player.setData(ModAttachments.APERTURE, ApertureData.DEFAULT);
        player.setData(ModAttachments.APERTURE_STORAGE, ApertureStorage.DEFAULT);
        player.setData(ModAttachments.SOUL, SoulData.DEFAULT);
        player.setData(ModAttachments.PATH, PathData.DEFAULT);
        player.setData(ModAttachments.QI, PathQiData.DEFAULT);
        player.setData(ModAttachments.STRENGTH, PathStrengthData.DEFAULT);
        player.setData(ModAttachments.ESSENCE_CARRY, new float[ApertureData.MAX_APERTURES]);
        player.setData(ModAttachments.NOURISH, ApertureNourishData.DEFAULT);
        player.setData(ModAttachments.DIMENSION_RETURN, DimensionReturnData.DEFAULT);
        onBirth(player);

        player.setData(ModAttachments.BODY,
                BodyData.DEFAULT.withLastDayIndex(BodyService.get(player).lastDayIndex()));

        if (player instanceof ServerPlayer server) {
            BodyHealthService.refresh(server);
            BodyAttackService.refresh(server);
            EpicFightIntegration.refresh(server);
        }
    }
}
