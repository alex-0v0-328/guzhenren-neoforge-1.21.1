package com.unknown.guzhenren.client.particle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pins the ring art the {@link RingParticle} scale math derives from: each frame's world size is
 * its canvas width over {@link RingGeometry#LARGEST_CANVAS}, so a redraw that changes a canvas
 * size (or a sprite list that drops a frame) must go red here and be adjudicated, not silently
 * rescale the effect.
 */
class RingTexturesTest {

    private static final String ROOT = "/assets/guzhenren/";
    private static final List<Integer> CANVAS_SIZES = List.of(3, 8, 13, 18, 23);

    @Test
    @DisplayName("all five ring textures exist, square, at their name's canvas size")
    void ringTexturesMatchTheirNames() throws IOException {
        for (int size : CANVAS_SIZES) {
            BufferedImage image = ImageIO.read(resource("textures/particle/ring_" + size + "x" + size + ".png"));
            assertEquals(size, image.getWidth(), "ring_" + size + "x" + size + " width");
            assertEquals(size, image.getHeight(), "ring_" + size + "x" + size + " height");
        }
    }
    @Test
    @DisplayName("the dash ring lists the sprites smallest-to-largest, five frames")
    void shockwaveRingPlaysSmallToLarge() throws IOException {
        assertEquals(List.of("guzhenren:ring_3x3", "guzhenren:ring_8x8", "guzhenren:ring_13x13",
                "guzhenren:ring_18x18", "guzhenren:ring_23x23"), spriteList("shockwave_ring"));
    }
    @Test
    @DisplayName("the impact ring lists the sprites smallest-to-largest, five frames")
    void impactRingPlaysSmallToLarge() throws IOException {
        assertEquals(List.of("guzhenren:ring_3x3", "guzhenren:ring_8x8", "guzhenren:ring_13x13",
                "guzhenren:ring_18x18", "guzhenren:ring_23x23"), spriteList("impact_ring"));
    }

    private static List<String> spriteList(String particle) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(
                resource("particles/" + particle + ".json"), StandardCharsets.UTF_8)) {
            JsonArray textures = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("textures");
            assertEquals(RingParticle.FRAME_COUNT, textures.size(), particle + " frame count");
            return textures.asList().stream().map(element -> element.getAsString()).toList();
        }
    }

    private static InputStream resource(String path) {
        InputStream input = RingTexturesTest.class.getResourceAsStream(ROOT + path);
        assertNotNull(input, "Missing packaged resource " + path);
        return input;
    }
}
