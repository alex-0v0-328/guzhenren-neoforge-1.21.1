package com.unknown.guzhenren.client.renderer;

import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.entity.WildBoarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renders the wild boar with its cutout model and nearest-neighbor texture. */
public final class WildBoarGeoRenderer extends GeoEntityRenderer<WildBoarEntity> {

    public static final ResourceLocation TEXTURE = Guzhenren.id("textures/entity/wild_boar.png");

    public WildBoarGeoRenderer(EntityRendererProvider.Context context, GeoModel<WildBoarEntity> model) {
        super(context, model);
        this.shadowRadius = 0.55F;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull WildBoarEntity entity) {
        return TEXTURE;
    }

    @Override
    protected float getDeathMaxRotation(@NotNull WildBoarEntity entity) {
        return 0.0F;
    }
}
