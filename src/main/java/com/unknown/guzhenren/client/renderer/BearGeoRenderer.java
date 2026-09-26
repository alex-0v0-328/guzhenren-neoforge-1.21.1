package com.unknown.guzhenren.client.renderer;

import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.entity.BearEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Shared GeckoLib renderer for the four bear species; geometry and animations come from the shared
 * {@code bear} model, each species' renderer instance injects its fixed texture.
 */
public final class BearGeoRenderer extends GeoEntityRenderer<BearEntity> {

    public static final ResourceLocation BROWN_TEXTURE =
            Guzhenren.id("textures/entity/brown_bear.png");
    public static final ResourceLocation ASIAN_BLACK_TEXTURE =
            Guzhenren.id("textures/entity/asian_black_bear.png");
    public static final ResourceLocation AMERICAN_BLACK_TEXTURE =
            Guzhenren.id("textures/entity/american_black_bear.png");
    public static final ResourceLocation ALBINO_TEXTURE =
            Guzhenren.id("textures/entity/albino_bear.png");

    private final ResourceLocation texture;

    public BearGeoRenderer(EntityRendererProvider.Context context, GeoModel<BearEntity> model,
                           ResourceLocation texture) {
        super(context, model);
        this.shadowRadius = 0.7F;
        this.texture = texture;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull BearEntity entity) {
        return this.texture;
    }

    @Override
    protected float getDeathMaxRotation(@NotNull BearEntity entity) {
        return 0.0F;
    }
}
