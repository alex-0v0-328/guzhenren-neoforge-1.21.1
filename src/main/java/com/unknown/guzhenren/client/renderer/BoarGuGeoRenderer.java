package com.unknown.guzhenren.client.renderer;

import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.entity.BoarGuEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Shared GeckoLib [GeckoLib] renderer for the white, black and flower Boar Gu [豕蛊] entities.
 *
 * <p>Geometry and animations come from the shared {@link GeoModel} baked from
 * {@code assets/guzhenren/geo/entity/boar_gu.geo.json}; each entity injects its own fixed
 * texture ({@code WHITE_TEXTURE}, {@code BLACK_TEXTURE} or {@code FLOWER_TEXTURE}). The ladybug
 * geometry is life-sized (about 0.19 by 0.28 blocks); {@code MODEL_SCALE} draws it at the width of the
 * 0.4-block hitbox without touching the exported geometry.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class BoarGuGeoRenderer extends GeoEntityRenderer<BoarGuEntity> {

    public static final ResourceLocation WHITE_TEXTURE =
            Guzhenren.id("textures/entity/white_boar_gu.png");
    public static final ResourceLocation BLACK_TEXTURE =
            Guzhenren.id("textures/entity/black_boar_gu.png");
    public static final ResourceLocation FLOWER_TEXTURE =
            Guzhenren.id("textures/entity/flower_boar_gu.png");
    private static final float MODEL_SCALE = 2.0F;
    private final ResourceLocation texture;
    public BoarGuGeoRenderer(EntityRendererProvider.Context context, GeoModel<BoarGuEntity> model,
                             ResourceLocation texture) {
        super(context, model);
        withScale(MODEL_SCALE);
        this.shadowRadius = 0.2F;
        this.texture = texture;
    }
    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull BoarGuEntity entity) {
        return texture;
    }
}
