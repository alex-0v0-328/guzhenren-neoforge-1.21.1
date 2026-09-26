package com.unknown.guzhenren.client.renderer;

import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.entity.TigerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Shared GeckoLib renderer for the orange and white tigers; geometry and animations come from the
 * shared {@code tiger} model, each coat's renderer instance injects its fixed texture.
 */
public final class TigerGeoRenderer extends GeoEntityRenderer<TigerEntity> {

    public static final ResourceLocation ORANGE_TEXTURE =
            Guzhenren.id("textures/entity/tiger.png");
    public static final ResourceLocation WHITE_TEXTURE =
            Guzhenren.id("textures/entity/white_tiger.png");

    private final ResourceLocation texture;

    public TigerGeoRenderer(EntityRendererProvider.Context context, GeoModel<TigerEntity> model,
                            ResourceLocation texture) {
        super(context, model);
        this.shadowRadius = 0.6F;
        this.texture = texture;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull TigerEntity entity) {
        return this.texture;
    }

    @Override
    protected float getDeathMaxRotation(@NotNull TigerEntity entity) {
        return 0.0F;
    }
}
