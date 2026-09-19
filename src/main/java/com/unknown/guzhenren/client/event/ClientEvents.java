package com.unknown.guzhenren.client.event;

import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.client.ModKeyMappings;
import com.unknown.guzhenren.client.hud.ChargeHud;
import com.unknown.guzhenren.client.hud.NourishHud;
import com.unknown.guzhenren.client.hud.PlayerStatsHud;
import com.unknown.guzhenren.client.particle.RingParticle;
import com.unknown.guzhenren.client.renderer.BoarGuGeoRenderer;
import com.unknown.guzhenren.client.renderer.RhinocerosBeetleGuGeoRenderer;
import com.unknown.guzhenren.client.renderer.WildBoarGeoRenderer;
import com.unknown.guzhenren.client.screen.ApertureStorageScreen;
import com.unknown.guzhenren.client.screen.PlayerInfoScreen;
import com.unknown.guzhenren.client.screen.RefinementScreen;
import com.unknown.guzhenren.entity.BoarGuEntity;
import com.unknown.guzhenren.entity.RhinocerosBeetleGuEntity;
import com.unknown.guzhenren.entity.WildBoarEntity;
import com.unknown.guzhenren.item.gu.MortalGuItem;
import com.unknown.guzhenren.network.payload.DashPayload;
import com.unknown.guzhenren.registry.effect.ModEffects;
import com.unknown.guzhenren.registry.entity.ModEntityTypes;
import com.unknown.guzhenren.registry.fluid.ModFluids;
import com.unknown.guzhenren.registry.menu.ModMenus;
import com.unknown.guzhenren.registry.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.GeoModel;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/**
 * Every client-side registration this mod makes: HUD layers, key mappings, screens and renderers.
 *
 * <p>Annotated {@code @EventBusSubscriber(Dist.CLIENT)}. Registers three GUI layers
 * ({@link com.unknown.guzhenren.client.hud.PlayerStatsHud},
 * {@link com.unknown.guzhenren.client.hud.ChargeHud},
 * {@link com.unknown.guzhenren.client.hud.NourishHud}), the key mapping for the B panel, the menu
 * screens for the two containers, the shockwave-ring particle provider, fixed-texture renderers sharing each Gu family's GeckoLib model,
 * and the wild boar's cutout GeckoLib model,
 * and the Hope Gu [希望蛊] entity as a
 * {@link net.minecraft.client.renderer.entity.NoopRenderer} (pure particles, no model), plus the
 * Spirit Spring fluid's translucent render layer.
 *
 * @author Alex
 * @version 1.0.0
 * @see com.unknown.guzhenren.client.ModKeyMappings
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {}
    private static final ResourceLocation PLAYER_STATS =
            Guzhenren.id("player_stats");
    private static final ResourceLocation CHARGE =
            Guzhenren.id("charge");
    private static final ResourceLocation NOURISH =
            Guzhenren.id("nourish");
    private static final GeoModel<BoarGuEntity> BOAR_GU_MODEL =
            new DefaultedEntityGeoModel<>(Guzhenren.id("boar_gu"), false);
    private static final GeoModel<RhinocerosBeetleGuEntity> BEETLE_GU_MODEL =
            new DefaultedEntityGeoModel<>(Guzhenren.id("rhinoceros_beetle"), false);
    private static final GeoModel<WildBoarEntity> WILD_BOAR_MODEL =
            new DefaultedEntityGeoModel<>(Guzhenren.id("wild_boar"), false);
    private static final float DASH_YAW_CROSS = 90.0F;
    private static final float DASH_YAW_DIAGONAL = 45.0F;
    private static boolean previousUp;
    private static boolean previousDown;
    private static boolean previousLeft;
    private static boolean previousRight;
    private static boolean previousAlt;
    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, PLAYER_STATS, PlayerStatsHud.INSTANCE);
        event.registerAbove(VanillaGuiLayers.AIR_LEVEL, CHARGE, ChargeHud.INSTANCE);
        event.registerAbove(VanillaGuiLayers.AIR_LEVEL, NOURISH, NourishHud.INSTANCE);
    }
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModFluids.SPIRIT_SPRING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_SPIRIT_SPRING.get(), RenderType.translucent());
        });
    }
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.OPEN_INFO);
    }
    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.APERTURE_STORAGE_MENU.get(), ApertureStorageScreen::new);
        event.register(ModMenus.REFINEMENT_MENU.get(), RefinementScreen::new);
    }
    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SHOCKWAVE_RING.get(), RingParticle::dashTrail);
        event.registerSpriteSet(ModParticles.IMPACT_RING.get(), RingParticle::facingMotion);
    }
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.HOPE_GU_ENTITY.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(),
                context -> new BoarGuGeoRenderer(context, BOAR_GU_MODEL, BoarGuGeoRenderer.WHITE_TEXTURE));
        event.registerEntityRenderer(ModEntityTypes.BLACK_BOAR_GU_ENTITY.get(),
                context -> new BoarGuGeoRenderer(context, BOAR_GU_MODEL, BoarGuGeoRenderer.BLACK_TEXTURE));
        event.registerEntityRenderer(ModEntityTypes.FLOWER_BOAR_GU_ENTITY.get(),
                context -> new BoarGuGeoRenderer(context, BOAR_GU_MODEL, BoarGuGeoRenderer.FLOWER_TEXTURE));
        event.registerEntityRenderer(ModEntityTypes.HORIZONTAL_CRASH_GU_ENTITY.get(),
                context -> new RhinocerosBeetleGuGeoRenderer(context, BEETLE_GU_MODEL,
                        RhinocerosBeetleGuGeoRenderer.LIGHT_TEXTURE));
        event.registerEntityRenderer(ModEntityTypes.VERTICAL_CRASH_GU_ENTITY.get(),
                context -> new RhinocerosBeetleGuGeoRenderer(context, BEETLE_GU_MODEL,
                        RhinocerosBeetleGuGeoRenderer.ORIGINAL_TEXTURE));
        event.registerEntityRenderer(ModEntityTypes.CHARGING_CRASH_GU_4_ENTITY.get(),
                context -> new RhinocerosBeetleGuGeoRenderer(context, BEETLE_GU_MODEL,
                        RhinocerosBeetleGuGeoRenderer.DARK_TEXTURE));
        event.registerEntityRenderer(ModEntityTypes.CHARGING_CRASH_GU_5_ENTITY.get(),
                context -> new RhinocerosBeetleGuGeoRenderer(context, BEETLE_GU_MODEL,
                        RhinocerosBeetleGuGeoRenderer.DARK_TEXTURE));
        event.registerEntityRenderer(ModEntityTypes.WILD_BOAR.get(),
                context -> new WildBoarGeoRenderer(context, WILD_BOAR_MODEL));
    }
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        ItemStack mainHand = minecraft.player.getMainHandItem();
        boolean canDash = canDash(mainHand);
        if (!canDash) {
            EpicFightCapabilities.getLocalPlayerPatchAsOptional(minecraft.player)
                    .filter(yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch::isEpicFightMode)
                    .ifPresent(patch -> patch.toVanillaMode(true));
        }
        while (ModKeyMappings.OPEN_INFO.consumeClick()) {
            if (minecraft.screen == null) minecraft.setScreen(new PlayerInfoScreen());
        }

        boolean up = minecraft.options.keyUp.isDown();
        boolean down = minecraft.options.keyDown.isDown();
        boolean left = minecraft.options.keyLeft.isDown();
        boolean right = minecraft.options.keyRight.isDown();
        boolean pressed = up && !previousUp || down && !previousDown
                || left && !previousLeft || right && !previousRight;
        boolean alt = Screen.hasAltDown();
        if (canDash && minecraft.screen == null && shouldStartDash(alt, previousAlt, pressed)) {
            int vertical = up == down ? 0 : up ? 1 : -1;
            int horizontal = left == right ? 0 : left ? 1 : -1;
            boolean directionHasEffect = canDash(mainHand, vertical, horizontal,
                    minecraft.player.hasEffect(ModEffects.HORIZONTAL_CRASH_GU),
                    minecraft.player.hasEffect(ModEffects.VERTICAL_CRASH_GU),
                    minecraft.player.hasEffect(ModEffects.CHARGING_CRASH_GU));
            if (directionHasEffect) {
                EpicFightCapabilities.getLocalPlayerPatchAsOptional(minecraft.player)
                        .ifPresent(patch -> {
                            patch.toEpicFightMode(true);
                            float cameraYRot = EpicFightCameraAPI.getInstance().getForwardYRot();
                            float yRot = Mth.wrapDegrees(cameraYRot
                                    - (DASH_YAW_CROSS * horizontal * (1 - Math.abs(vertical))
                                    + DASH_YAW_DIAGONAL * vertical * horizontal));
                            PacketDistributor.sendToServer(new DashPayload(vertical, horizontal, yRot));
                            RingParticle.noteLocalDash(minecraft.player.tickCount);
                        });
            }
        }
        previousUp = up;
        previousDown = down;
        previousLeft = left;
        previousRight = right;
        previousAlt = alt;
    }
    public static boolean canDash(ItemStack mainHand) {
        return !(mainHand.getItem() instanceof MortalGuItem);
    }
    public static boolean canDash(ItemStack mainHand, int vertical, int horizontal,
                                  boolean horizontalCrash, boolean verticalCrash, boolean chargingCrash) {
        if (!canDash(mainHand) || vertical == 0 && horizontal == 0) return false;
        return (horizontal == 0 || horizontalCrash || chargingCrash)
                && (vertical == 0 || verticalCrash || chargingCrash);
    }
    public static boolean shouldSendDash(boolean directionHasEffect) {
        return directionHasEffect;
    }
    public static boolean shouldStartDash(boolean alt, boolean previousAlt, boolean directionPressed) {
        return alt && (!previousAlt || directionPressed);
    }
}
