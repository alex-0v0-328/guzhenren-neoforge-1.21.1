package com.unknown.guzhenren.network;

import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.attachment.service.aperture.ApertureNourishService;
import com.unknown.guzhenren.attachment.service.aperture.ApertureService;
import com.unknown.guzhenren.compat.EpicFightIntegration;
import com.unknown.guzhenren.item.gu.MortalGuItem;
import com.unknown.guzhenren.menu.ApertureStorageMenu;
import com.unknown.guzhenren.menu.RefinementMenu;
import com.unknown.guzhenren.network.payload.DashPayload;
import com.unknown.guzhenren.network.payload.ImpactApertureWallPayload;
import com.unknown.guzhenren.network.payload.NourishAperturePayload;
import com.unknown.guzhenren.network.payload.OpenApertureStoragePayload;
import com.unknown.guzhenren.network.payload.OpenRefinementPayload;
import com.unknown.guzhenren.network.payload.SetSecondaryPathPayload;
import com.unknown.guzhenren.registry.effect.ModEffects;
import com.unknown.guzhenren.registry.world.ModDimensions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers the client-intent payloads and handles each of them on the server. Every payload in this
 * mod is a client intent -- a B-panel button or movement input that attachment sync cannot carry
 * upstream; none carries player data, and downstream player data always travels as synced state. This
 * class wires the six payloads to their server-side handlers: the two containers, the secondary path,
 * and the three cultivation actions.
 *
 * <p>⚠ This is where a forged payload lands, so a gate that only grays out a button is not a gate:
 * every refusal has to exist here as well as on the screen.
 *
 * @author Alex
 * @version 1.0.0
 * @see com.unknown.guzhenren.client.screen.PlayerInfoScreen
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class ModPayloads {

    private ModPayloads() {}
    private static final String VERSION = "1";
    private static boolean inTyh(ServerPlayer player) {
        return player.level().dimension().equals(ModDimensions.TREASURE_YELLOW_HEAVEN);
    }
    private static final String STORAGE_TITLE = "guzhenren.menu.aperture_storage";
    private static final String REFINEMENT_TITLE = "guzhenren.menu.refinement";
    @SubscribeEvent
    public static void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(OpenApertureStoragePayload.TYPE, OpenApertureStoragePayload.STREAM_CODEC,
                ModPayloads::openStorage);
        registrar.playToServer(SetSecondaryPathPayload.TYPE, SetSecondaryPathPayload.STREAM_CODEC,
                ModPayloads::setSecondaryPath);
        registrar.playToServer(OpenRefinementPayload.TYPE, OpenRefinementPayload.STREAM_CODEC,
                ModPayloads::openRefinement);
        registrar.playToServer(NourishAperturePayload.TYPE, NourishAperturePayload.STREAM_CODEC,
                ModPayloads::nourishAperture);
        registrar.playToServer(ImpactApertureWallPayload.TYPE, ImpactApertureWallPayload.STREAM_CODEC,
                ModPayloads::impactApertureWall);
        registrar.playToServer(DashPayload.TYPE, DashPayload.STREAM_CODEC,
                ModPayloads::dash);
    }
    private static void nourishAperture(NourishAperturePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (inTyh(player)) return;
        if (payload.aperture() < 0 || payload.aperture() >= ApertureService.get(player).count()) return;

        switch (payload.action()) {
            case START -> ApertureNourishService.start(player, payload.aperture());
            case CANCEL -> ApertureNourishService.cancel(player);
        }
    }
    private static void impactApertureWall(ImpactApertureWallPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (inTyh(player)) return;
        ApertureNourishService.impactWall(player);
    }
    private static void dash(DashPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (inTyh(player)) return;

        int vertical = payload.vertical();
        int horizontal = payload.horizontal();
        if (vertical < -1 || vertical > 1 || horizontal < -1 || horizontal > 1
                || (vertical == 0 && horizontal == 0) || !Float.isFinite(payload.yRot())) return;
        if (player.getMainHandItem().getItem() instanceof MortalGuItem) return;
        if (ApertureNourishService.isCultivating(player)) return;
        if (horizontal != 0 && !player.hasEffect(ModEffects.HORIZONTAL_CRASH_GU)
                && !player.hasEffect(ModEffects.CHARGING_CRASH_GU)) return;
        if (vertical != 0 && !player.hasEffect(ModEffects.VERTICAL_CRASH_GU)
                && !player.hasEffect(ModEffects.CHARGING_CRASH_GU)) return;

        EpicFightIntegration.dash(player, vertical, payload.yRot());
    }
    private static void openRefinement(OpenRefinementPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (inTyh(player)) return;
        if (!ApertureService.isAwakened(player)) return;

        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new RefinementMenu(id, inventory),
                Component.translatable(REFINEMENT_TITLE)));
    }
    private static void setSecondaryPath(SetSecondaryPathPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (inTyh(player)) return;

        int aperture = payload.aperture();
        if (aperture < 0 || aperture >= ApertureService.get(player).count()) return;

        ApertureService.setSecondaryPath(player, aperture, payload.path());
    }
    private static void openStorage(OpenApertureStoragePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (inTyh(player)) return;

        int aperture = payload.aperture();
        if (aperture < 0 || aperture >= ApertureService.get(player).count()) return;

        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new ApertureStorageMenu(id, inventory, aperture, 0),
                Component.translatable(STORAGE_TITLE)));
    }
}
