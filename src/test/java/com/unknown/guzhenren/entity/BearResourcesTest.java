package com.unknown.guzhenren.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.registry.entity.ModEntityTypes;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.junit.jupiter.api.Test;
import software.bernie.geckolib.animation.Animation.LoopType;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BearResourcesTest {

    private static final String ROOT = "/assets/guzhenren/";

    @Test
    void modelAndEveryAnimationBakeWithTheInstalledGeckoLibVersion() throws IOException {
        Model model = KeyFramesAdapter.GEO_GSON.fromJson(
                json("geo/entity/bear.geo.json"), Model.class);
        BakedGeoModel baked = BakedModelFactory.getForNamespace("guzhenren")
                .constructGeoModel(GeometryTree.fromModel(model));
        assertEquals("geometry.bear", baked.properties().identifier());
        assertEquals(20, cubeCount(baked.topLevelBones()));
        assertEquals(22, boneCount(baked.topLevelBones()));

        BakedAnimations animations = KeyFramesAdapter.GEO_GSON.fromJson(
                json("animations/entity/bear.animation.json").getAsJsonObject("animations"),
                BakedAnimations.class);
        assertEquals(Set.of("animation.idle", "animation.walk", "animation.run",
                        "animation.attack_swipe", "animation.attack_rear", "animation.roar",
                        "animation.back_scratch", "animation.sit", "animation.lie_down",
                        "animation.lie", "animation.sleep", "animation.get_up", "animation.roll",
                        "animation.hurt_left", "animation.hurt_right", "animation.death"),
                animations.animations().keySet());
        assertEquals(LoopType.LOOP, animations.getAnimation("animation.idle").loopType());
        assertEquals(LoopType.LOOP, animations.getAnimation("animation.walk").loopType());
        assertEquals(LoopType.LOOP, animations.getAnimation("animation.run").loopType());
        assertEquals(LoopType.LOOP, animations.getAnimation("animation.sit").loopType());
        assertEquals(LoopType.LOOP, animations.getAnimation("animation.lie").loopType());
        assertEquals(LoopType.LOOP, animations.getAnimation("animation.sleep").loopType());
        assertEquals(LoopType.PLAY_ONCE, animations.getAnimation("animation.attack_swipe").loopType());
        assertEquals(LoopType.PLAY_ONCE, animations.getAnimation("animation.attack_rear").loopType());
        assertEquals(LoopType.PLAY_ONCE, animations.getAnimation("animation.roar").loopType());
        assertEquals(LoopType.PLAY_ONCE, animations.getAnimation("animation.back_scratch").loopType());
        assertEquals(LoopType.PLAY_ONCE, animations.getAnimation("animation.get_up").loopType());
        assertEquals(LoopType.PLAY_ONCE, animations.getAnimation("animation.roll").loopType());
        assertEquals(LoopType.PLAY_ONCE, animations.getAnimation("animation.hurt_left").loopType());
        assertEquals(LoopType.PLAY_ONCE, animations.getAnimation("animation.hurt_right").loopType());
        assertEquals(LoopType.HOLD_ON_LAST_FRAME, animations.getAnimation("animation.lie_down").loopType());
        assertEquals(LoopType.HOLD_ON_LAST_FRAME, animations.getAnimation("animation.death").loopType());
        Map<String, Double> expectedLengths = Map.ofEntries(
                Map.entry("animation.idle", 120.0),
                Map.entry("animation.walk", 36.0),
                Map.entry("animation.run", 14.0),
                Map.entry("animation.attack_swipe", 20.0),
                Map.entry("animation.attack_rear", 36.0),
                Map.entry("animation.roar", 52.0),
                Map.entry("animation.back_scratch", 100.0),
                Map.entry("animation.sit", 80.0),
                Map.entry("animation.lie_down", 28.0),
                Map.entry("animation.lie", 120.0),
                Map.entry("animation.sleep", 120.0),
                Map.entry("animation.get_up", 24.0),
                Map.entry("animation.roll", 74.0),
                Map.entry("animation.hurt_left", 11.0),
                Map.entry("animation.hurt_right", 11.0),
                Map.entry("animation.death", 36.0));
        for (var animation : animations.animations().values()) {
            assertEquals(expectedLengths.get(animation.name()), animation.length(), 1.0e-6, animation.name());
            assertTrue(animation.boneAnimations().length > 0, animation.name());
            for (var bone : animation.boneAnimations()) {
                assertTrue(baked.getBone(bone.boneName()).isPresent(),
                        animation.name() + " references missing bone " + bone.boneName());
            }
        }
    }

    @Test
    void entityRegistrationKeepsCreatureCategoryDimensionsAndAttributes() {
        double[] expectedHealth = {54.0, 42.0, 40.0, 40.0};
        BearSpecies[] species = BearSpecies.values();
        EntityType<?>[] types = {ModEntityTypes.BROWN_BEAR.get(), ModEntityTypes.ASIAN_BLACK_BEAR.get(),
                ModEntityTypes.AMERICAN_BLACK_BEAR.get(), ModEntityTypes.ALBINO_BEAR.get()};
        for (int i = 0; i < species.length; i++) {
            EntityType<?> type = types[i];
            assertEquals(Guzhenren.id(species[i].id()), type.builtInRegistryHolder().key().location());
            assertEquals(MobCategory.CREATURE, type.getCategory());
            assertEquals(1.2F, type.getWidth(), 1.0e-6F);
            assertEquals(1.4F, type.getHeight(), 1.0e-6F);

            var attributes = BearEntity.createAttributes(species[i]).build();
            assertEquals(expectedHealth[i], attributes.getBaseValue(Attributes.MAX_HEALTH), 1.0e-6);
            assertEquals(0.25, attributes.getBaseValue(Attributes.MOVEMENT_SPEED), 1.0e-6);
            assertEquals(16.0, attributes.getBaseValue(Attributes.FOLLOW_RANGE), 1.0e-6);
        }
        assertTrue(BearSpecies.ASIAN_BLACK.hostile());
        assertFalse(BearSpecies.BROWN.hostile());
        assertFalse(BearSpecies.AMERICAN_BLACK.hostile());
        assertFalse(BearSpecies.ALBINO.hostile());
    }

    @Test
    void texturesAreTheExpectedNearestNeighborCutoutTextures() throws IOException {
        for (String name : new String[]{"brown_bear", "asian_black_bear", "american_black_bear",
                "albino_bear"}) {
            try (InputStream input = resource("textures/entity/" + name + ".png")) {
                BufferedImage texture = ImageIO.read(input);
                assertNotNull(texture);
                assertEquals(128, texture.getWidth(), name);
                assertEquals(128, texture.getHeight(), name);
                for (int y = 0; y < texture.getHeight(); y++) {
                    for (int x = 0; x < texture.getWidth(); x++) {
                        int alpha = (texture.getRGB(x, y) >>> 24) & 0xff;
                        assertTrue(alpha == 0 || alpha == 0xff,
                                name + " non-binary alpha at " + x + "," + y + ": " + alpha);
                    }
                }
            }
            JsonObject mcmeta = json("textures/entity/" + name + ".png.mcmeta").getAsJsonObject("texture");
            assertFalse(mcmeta.get("blur").getAsBoolean(), name);
            assertFalse(mcmeta.get("clamp").getAsBoolean(), name);
        }
    }

    @Test
    void generatedSpawnTagContainsEveryForestFamilyBiome() throws IOException {
        JsonObject tag = dataJson("tags/worldgen/biome/bear_spawns.json");
        List<String> biomes = new ArrayList<>();
        tag.getAsJsonArray("values").forEach(value -> biomes.add(value.getAsString()));
        assertEquals(List.of("minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest",
                        "minecraft:old_growth_birch_forest", "minecraft:dark_forest", "minecraft:taiga",
                        "minecraft:snowy_taiga", "minecraft:old_growth_pine_taiga",
                        "minecraft:old_growth_spruce_taiga", "minecraft:jungle", "minecraft:sparse_jungle",
                        "minecraft:bamboo_jungle"), biomes);
    }

    @Test
    void generatedSpawnModifiersUseTheSpeciesWeights() throws IOException {
        assertSpawnModifier("spawn_brown_bear", "guzhenren:brown_bear", 4);
        assertSpawnModifier("spawn_asian_black_bear", "guzhenren:asian_black_bear", 4);
        assertSpawnModifier("spawn_american_black_bear", "guzhenren:american_black_bear", 4);
        assertSpawnModifier("spawn_albino_bear", "guzhenren:albino_bear", 1);
    }

    @Test
    void generatedPreyTagListsOnlyVanillaFarmAndSmallAnimals() throws IOException {
        JsonObject tag = dataJson("tags/entity_type/predator_prey.json");
        List<String> values = new ArrayList<>();
        tag.getAsJsonArray("values").forEach(value -> values.add(value.getAsString()));
        assertEquals(List.of("minecraft:pig", "minecraft:cow", "minecraft:sheep", "minecraft:chicken",
                "minecraft:rabbit"), values);
    }

    private static void assertSpawnModifier(String name, String entityId, int weight) throws IOException {
        JsonObject modifier = dataJson("neoforge/biome_modifier/" + name + ".json");
        assertEquals("neoforge:add_spawns", modifier.get("type").getAsString(), name);
        assertEquals("#guzhenren:bear_spawns", modifier.get("biomes").getAsString(), name);
        JsonObject spawner = modifier.getAsJsonObject("spawners");
        assertEquals(entityId, spawner.get("type").getAsString(), name);
        assertEquals(weight, spawner.get("weight").getAsInt(), name);
        assertEquals(1, spawner.get("minCount").getAsInt(), name);
        assertEquals(1, spawner.get("maxCount").getAsInt(), name);
    }

    private static int cubeCount(List<GeoBone> bones) {
        return bones.stream().mapToInt(bone -> bone.getCubes().size() + cubeCount(bone.getChildBones())).sum();
    }

    private static int boneCount(List<GeoBone> bones) {
        return bones.stream().mapToInt(bone -> 1 + boneCount(bone.getChildBones())).sum();
    }

    private static JsonObject json(String path) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(resource(path), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static JsonObject dataJson(String path) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(dataResource(path), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static InputStream resource(String path) {
        InputStream input = BearResourcesTest.class.getResourceAsStream(ROOT + path);
        assertNotNull(input, "Missing packaged resource " + path);
        return input;
    }

    private static InputStream dataResource(String path) {
        InputStream input = BearResourcesTest.class.getResourceAsStream("/data/guzhenren/" + path);
        assertNotNull(input, "Missing packaged data resource " + path);
        return input;
    }
}
