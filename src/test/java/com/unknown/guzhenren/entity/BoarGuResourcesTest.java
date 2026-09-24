package com.unknown.guzhenren.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoarGuResourcesTest {

    private static final String ROOT = "/assets/guzhenren/";
    @Test
    void ladybugModelAndEveryAnimationBakeWithTheInstalledGeckoLibVersion() throws IOException {
        Model model = KeyFramesAdapter.GEO_GSON.fromJson(json("geo/entity/boar_gu.geo.json"), Model.class);
        BakedGeoModel baked = BakedModelFactory.getForNamespace("guzhenren")
                .constructGeoModel(GeometryTree.fromModel(model));
        assertEquals("geometry.boar_gu", baked.properties().identifier());
        assertEquals(7, boneCount(baked.topLevelBones()));
        assertEquals(8, cubeCount(baked.topLevelBones()));
        for (String bone : List.of("body", "head", "shell_left", "shell_right", "antenna_left", "antenna_right")) {
            assertTrue(baked.getBone(bone).isPresent(), bone);
        }

        BakedAnimations animations = KeyFramesAdapter.GEO_GSON.fromJson(
                json("animations/entity/boar_gu.animation.json").getAsJsonObject("animations"),
                BakedAnimations.class);
        assertEquals(Set.of("animation.idle", "animation.lift", "animation.fly", "animation.land"),
                animations.animations().keySet());
        for (var animation : animations.animations().values()) {
            assertTrue(Double.isFinite(animation.length()) && animation.length() > 0, animation.name());
            assertTrue(animation.boneAnimations().length > 0, animation.name());
            for (var bone : animation.boneAnimations()) {
                assertTrue(baked.getBone(bone.boneName()).isPresent(),
                        animation.name() + " references missing bone " + bone.boneName());
            }
        }
    }
    @Test
    void allThreeEntityTexturesDecodeAtTheModelUvResolution() throws IOException {
        for (String variant : List.of("white_boar_gu", "black_boar_gu", "flower_boar_gu")) {
            try (InputStream input = resource("textures/entity/" + variant + ".png")) {
                var texture = ImageIO.read(input);
                assertNotNull(texture, variant);
                assertEquals(64, texture.getWidth(), variant);
                assertEquals(64, texture.getHeight(), variant);
            }
        }
    }
    private static int boneCount(List<GeoBone> bones) {
        return bones.stream().mapToInt(bone -> 1 + boneCount(bone.getChildBones())).sum();
    }
    private static int cubeCount(List<GeoBone> bones) {
        return bones.stream().mapToInt(bone -> bone.getCubes().size() + cubeCount(bone.getChildBones())).sum();
    }
    private static JsonObject json(String path) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(resource(path), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
    private static InputStream resource(String path) {
        InputStream input = BoarGuResourcesTest.class.getResourceAsStream(ROOT + path);
        assertNotNull(input, "Missing packaged resource " + path);
        return input;
    }
}
