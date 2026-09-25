package com.unknown.guzhenren.entity;

import com.google.gson.JsonArray;
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

class WildBoarResourcesTest {

    private static final String ROOT = "/assets/guzhenren/";

    @Test
    void modelAndEveryAnimationBakeWithTheInstalledGeckoLibVersion() throws IOException {
        Model model = KeyFramesAdapter.GEO_GSON.fromJson(
                json("geo/entity/wild_boar.geo.json"), Model.class);
        BakedGeoModel baked = BakedModelFactory.getForNamespace("guzhenren")
                .constructGeoModel(GeometryTree.fromModel(model));
        assertEquals("geometry.wild_boar", baked.properties().identifier());
        assertEquals(35, cubeCount(baked.topLevelBones()));
        assertEquals(25, boneCount(baked.topLevelBones()));

        BakedAnimations animations = KeyFramesAdapter.GEO_GSON.fromJson(
                json("animations/entity/wild_boar.animation.json").getAsJsonObject("animations"),
                BakedAnimations.class);
        assertEquals(Set.of("idle", "walk", "run", "graze", "alert", "attack_charge",
                        "attack_toss", "hurt", "death"), animations.animations().keySet());
        assertEquals(LoopType.LOOP, animations.getAnimation("idle").loopType());
        assertEquals(LoopType.LOOP, animations.getAnimation("walk").loopType());
        assertEquals(LoopType.LOOP, animations.getAnimation("run").loopType());
        assertEquals(LoopType.LOOP, animations.getAnimation("graze").loopType());
        assertEquals(LoopType.PLAY_ONCE, animations.getAnimation("alert").loopType());
        assertEquals(LoopType.PLAY_ONCE, animations.getAnimation("attack_charge").loopType());
        assertEquals(LoopType.PLAY_ONCE, animations.getAnimation("attack_toss").loopType());
        assertEquals(LoopType.PLAY_ONCE, animations.getAnimation("hurt").loopType());
        assertEquals(LoopType.HOLD_ON_LAST_FRAME, animations.getAnimation("death").loopType());
        Map<String, Double> expectedLengths = Map.of(
                "idle", 120.0,
                "walk", 28.0,
                "run", 12.0,
                "graze", 100.0,
                "alert", 16.0,
                "attack_charge", 24.0,
                "attack_toss", 16.0,
                "hurt", 7.0,
                "death", 28.0);
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
        EntityType<WildBoarEntity> type = ModEntityTypes.WILD_BOAR.get();
        assertEquals(Guzhenren.id("wild_boar"), type.builtInRegistryHolder().key().location());
        assertEquals(MobCategory.CREATURE, type.getCategory());
        assertEquals(1.1F, type.getWidth(), 1.0e-6F);
        assertEquals(1.2F, type.getHeight(), 1.0e-6F);

        var attributes = WildBoarEntity.createAttributes().build();
        assertEquals(30.0, attributes.getBaseValue(Attributes.MAX_HEALTH), 1.0e-6);
        assertEquals(0.25, attributes.getBaseValue(Attributes.MOVEMENT_SPEED), 1.0e-6);
    }

    @Test
    void textureIsTheExpectedNearestNeighborCutoutTexture() throws IOException {
        try (InputStream input = resource("textures/entity/wild_boar.png")) {
            BufferedImage texture = ImageIO.read(input);
            assertNotNull(texture);
            assertEquals(128, texture.getWidth());
            assertEquals(128, texture.getHeight());
            for (int y = 0; y < texture.getHeight(); y++) {
                for (int x = 0; x < texture.getWidth(); x++) {
                    int alpha = (texture.getRGB(x, y) >>> 24) & 0xff;
                    assertTrue(alpha == 0 || alpha == 0xff,
                            "Non-binary alpha at " + x + "," + y + ": " + alpha);
                }
            }
        }
    }

    @Test
    void textureMetadataKeepsCutoutSamplingNearest() throws IOException {
        JsonObject texture = json("textures/entity/wild_boar.png.mcmeta").getAsJsonObject("texture");
        assertFalse(texture.get("blur").getAsBoolean());
        assertFalse(texture.get("clamp").getAsBoolean());
    }

    @Test
    void generatedSpawnTagContainsOnlyTheFiveForestBiomes() throws IOException {
        JsonObject tag = dataJson("tags/worldgen/biome/wild_boar_spawns.json");
        List<String> biomes = new ArrayList<>();
        tag.getAsJsonArray("values").forEach(value -> biomes.add(value.getAsString()));
        assertEquals(List.of("minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest",
                        "minecraft:old_growth_birch_forest", "minecraft:dark_forest"), biomes);
    }

    @Test
    void generatedSpawnModifierUsesTheWildBoarWeightAndPackSize() throws IOException {
        JsonObject modifier = dataJson("neoforge/biome_modifier/spawn_wild_boar.json");
        assertEquals("neoforge:add_spawns", modifier.get("type").getAsString());
        assertEquals("#guzhenren:wild_boar_spawns", modifier.get("biomes").getAsString());
        JsonObject spawner = modifier.getAsJsonObject("spawners");
        assertEquals("guzhenren:wild_boar", spawner.get("type").getAsString());
        assertEquals(8, spawner.get("weight").getAsInt());
        assertEquals(1, spawner.get("minCount").getAsInt());
        assertEquals(3, spawner.get("maxCount").getAsInt());
    }

    @Test
    void generatedLootUsesVanillaPorkSmeltingAndLootingFunctions() throws IOException {
        JsonObject loot = dataJson("loot_table/entities/wild_boar.json");
        JsonObject pool = loot.getAsJsonArray("pools").get(0).getAsJsonObject();
        JsonObject entry = pool.getAsJsonArray("entries").get(0).getAsJsonObject();
        assertEquals("minecraft:item", entry.get("type").getAsString());
        assertEquals("minecraft:porkchop", entry.get("name").getAsString());

        Set<String> functions = new java.util.HashSet<>();
        JsonArray functionArray = entry.getAsJsonArray("functions");
        functionArray.forEach(function -> functions.add(function.getAsJsonObject().get("function").getAsString()));
        assertTrue(functions.contains("minecraft:set_count"));
        assertTrue(functions.contains("minecraft:furnace_smelt"));
        assertTrue(functions.contains("minecraft:enchanted_count_increase"));

        JsonObject count = functionArray.asList().stream()
                .map(element -> element.getAsJsonObject())
                .filter(function -> function.get("function").getAsString().equals("minecraft:set_count"))
                .findFirst().orElseThrow();
        JsonObject uniform = count.getAsJsonObject("count");
        assertEquals("minecraft:uniform", uniform.get("type").getAsString());
        assertEquals(1.0, uniform.get("min").getAsDouble(), 1.0e-6);
        assertEquals(3.0, uniform.get("max").getAsDouble(), 1.0e-6);
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
        InputStream input = WildBoarResourcesTest.class.getResourceAsStream(ROOT + path);
        assertNotNull(input, "Missing packaged resource " + path);
        return input;
    }

    private static InputStream dataResource(String path) {
        InputStream input = WildBoarResourcesTest.class.getResourceAsStream("/data/guzhenren/" + path);
        assertNotNull(input, "Missing packaged data resource " + path);
        return input;
    }
}
