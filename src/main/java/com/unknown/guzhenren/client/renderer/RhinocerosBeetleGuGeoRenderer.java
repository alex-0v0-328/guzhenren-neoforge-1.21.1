package com.unknown.guzhenren.client.renderer;

import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.entity.RhinocerosBeetleGuEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Shares beetle geometry and animations while each registered type selects its fixed texture. */
public final class RhinocerosBeetleGuGeoRenderer extends GeoEntityRenderer<RhinocerosBeetleGuEntity> {

    public static final ResourceLocation LIGHT_TEXTURE = Guzhenren.id("textures/entity/horizontal_crash_gu.png");
    public static final ResourceLocation ORIGINAL_TEXTURE = Guzhenren.id("textures/entity/vertical_crash_gu.png");
    public static final ResourceLocation DARK_TEXTURE = Guzhenren.id("textures/entity/charging_crash_gu.png");
    private final ResourceLocation texture;
    public RhinocerosBeetleGuGeoRenderer(EntityRendererProvider.Context context,
                                         GeoModel<RhinocerosBeetleGuEntity> model, ResourceLocation texture) {
        super(context, model);
        this.shadowRadius = 0.2F;
        this.texture = texture;
    }
    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull RhinocerosBeetleGuEntity entity) {return texture;}
}
