package com.unknown.guzhenren.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.unknown.guzhenren.Guzhenren;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The reusable shockwave ring [激波环] for speed- and force-feel feedback: Alex's five hand-drawn
 * rings played in sprite-list order, one particle per drop. Both trails go through
 * {@code RingConeEmitter} and bloom small-to-large where they were planted: the dash trail rides
 * {@code shockwave_ring} along the dash path (hidden from the dashing player's own first-person
 * view -- the trail is for third-person and bystanders, Alex 2026-09-20), the punch trail rides
 * {@code impact_ring} along the punch ray from the strike point. The particles json's sprite
 * order IS the playback order, and each frame's world size derives from its own canvas width
 * ({@link RingGeometry#scaleForWidth(int)}), so growth direction lives in exactly one place.
 * The quad size lerps toward the next frame every tick, so the bloom reads smooth instead of
 * stepping (the 3px↔8px jump alone is ×2.6).
 *
 * <p>{@link Orientation#GROUND} lays the quad flat in the world XZ plane (rotating XZ corners by
 * the camera quaternion was the 2026-09-19 bug -- it pitched the quad with the view and sank its
 * leading edge under the terrain). {@link Orientation#FACING_MOTION} builds the ring plane
 * perpendicular to the spawn velocity, tilting a few degrees toward the camera only when that
 * plane would be perfectly edge-on (a sideways dash seen from the front -- Alex's 2026-09-19 spec:
 * keep the ring perpendicular to the motion, opening toward the camera by
 * {@link RingGeometry#MIN_OPENING_DEGREES} when it runs parallel to the screen). New
 * callers register a type in {@code ModParticles}, list the rings in the datagen provider, and map
 * the type to {@link #ground(SpriteSet)}, {@link #facingMotion(SpriteSet)} or
 * {@link #dashTrail(SpriteSet)}.
 *
 * <p>It renders through {@link #ADDITIVE_GLOW} -- additive, two-sided, full bright -- because the
 * one-pixel stroke in the art all but disappears on an alpha-blended sheet (2026-09-19 runClient
 * feedback).
 *
 * @author Alex
 * @version 1.0.0
 * @see com.unknown.guzhenren.registry.particle.ModParticles
 * @see RingGeometry
 * @since 1.0.0
 */
public final class RingParticle extends TextureSheetParticle {

    /** How the ring quad is oriented in the world; per particle type, chosen by the provider. */
    public enum Orientation {GROUND, FACING_MOTION}
    /**
     * Additive glow sheet: the stroke art is pure white, so SRC_ALPHA/ONE makes it emit instead of
     * blend, and masked depth writes keep the ground-lying quad from z-fighting. 1.21.1 has no
     * {@code end()} hook on {@link ParticleRenderType}, so nothing here may leak to the next type:
     * every vanilla type re-sets blend and depth mask in its own {@code begin}, culling is never
     * touched (the quad is emitted two-sided instead of disabling cull), and the atlas filter is
     * left at the vanilla default.
     */
    public static final ParticleRenderType ADDITIVE_GLOW = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }
        @Override
        public String toString() {return Guzhenren.MOD_ID + ":additive_glow";}
    };
    /**
     * One tick per hand-drawn ring. Pinned by the L2 ring-texture test against the datagen'd
     * sprite lists, so a redraw that adds or drops frames goes red instead of silently clipping.
     */
    static final int FRAME_COUNT = 5;
    /**
     * Extra ticks a ring holds its final frame before despawning. Without it a ring dies the very
     * tick it reaches full size and the completed bloom never reads. Both planted trails linger.
     */
    private static final int RING_LINGER_TICKS = 3;
    /**
     * How long the local player's own dash cone stays hidden in first person, in ticks: the burst
     * length plus one ring life including its linger (Alex, 2026-09-20: the dash trail is for
     * third-person and bystanders). The remaining-window comparison must bound both ends -- a
     * world change resets the client player's tickCount and would otherwise pin the hide on for
     * ages.
     */
    private static final int DASH_SELF_HIDE_TICKS = 20;
    private static int localDashHiddenUntilTick = Integer.MIN_VALUE;
    private final SpriteSet sprites;
    private final Orientation orientation;
    /** True for the dash trail type: skipped by the dasher's own first-person camera. */
    private final boolean dashTrail;
    /** The spawn velocity, kept as the FACING_MOTION ring normal; GROUND ignores it. */
    private final Vec3 spawnDirection;
    /** Next frame's half-span, the lerp target of {@link #getQuadSize(float)}. */
    private float nextQuadSize;

    private RingParticle(ClientLevel level, double x, double y, double z,
                         double xSpeed, double ySpeed, double zSpeed,
                         SpriteSet sprites, Orientation orientation, boolean dashTrail,
                         int lingerTicks) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.orientation = orientation;
        this.dashTrail = dashTrail;
        this.spawnDirection = new Vec3(xSpeed, ySpeed, zSpeed);
        this.lifetime = FRAME_COUNT + lingerTicks;
        this.hasPhysics = false;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        applyFrame(0);
    }

    public static ParticleProvider<SimpleParticleType> ground(SpriteSet sprites) {
        return (type, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
                new RingParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites,
                        Orientation.GROUND, false, 0);
    }

    public static ParticleProvider<SimpleParticleType> facingMotion(SpriteSet sprites) {
        return (type, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
                new RingParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites,
                        Orientation.FACING_MOTION, false, RING_LINGER_TICKS);
    }

    /**
     * The dash-trail provider: the same facing-motion ring, but hidden from the dashing player's
     * own first-person camera for {@link #DASH_SELF_HIDE_TICKS} after the dash fires.
     */
    public static ParticleProvider<SimpleParticleType> dashTrail(SpriteSet sprites) {
        return (type, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
                new RingParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites,
                        Orientation.FACING_MOTION, true, RING_LINGER_TICKS);
    }

    /** Called when the local player fires a dash: their own cone stays first-person-hidden. */
    public static void noteLocalDash(int tickCount) {
        localDashHiddenUntilTick = tickCount + DASH_SELF_HIDE_TICKS;
    }

    /**
     * Vanilla pacing: the frame advances with the post-increment {@code age} -- the constructor
     * plants frame 0 for the spawn render, and the guard reads the age after the increment so the
     * last frame (index {@code FRAME_COUNT - 1}) is applied exactly once and never overstepped
     * (mixing the two sides was the off-by-one that crashed the first dash). Once the ring lingers
     * past its last frame, the clamped index simply holds the final sprite.
     */
    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }
        applyFrame(Math.min(this.age, FRAME_COUNT - 1));
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
    }

    private void applyFrame(int frame) {
        // The denominator is FRAME_COUNT - 1, NOT lifetime - 1: SpriteSet.get(i, j) maps to sprite
        // i * (n-1) / j, and only the frame count lands exactly on frames 0..n-1 once lingerTicks
        // decouples the lifetime from it.
        this.setSprite(this.sprites.get(frame, FRAME_COUNT - 1));
        this.quadSize = RingGeometry.scaleForWidth(this.sprite.contents().width());
        this.nextQuadSize = frame + 1 < FRAME_COUNT
                ? RingGeometry.scaleForWidth(
                        this.sprites.get(frame + 1, FRAME_COUNT - 1).contents().width())
                : this.quadSize;
    }

    /** Smooth growth: the quad lerps toward the next frame's size over the current tick. */
    @Override
    public float getQuadSize(float partialTick) {
        return Mth.lerp(partialTick, this.quadSize, this.nextQuadSize);
    }

    /**
     * The quad is emitted as two proper quads with opposite winding (8 vertices) so it shows from
     * above and below without touching the global cull state, which no vanilla {@code begin}
     * restores. QUADS groups vertices four-by-four -- a six-vertex triangle style would land as
     * degenerate quads -- and corner UVs stay pinned to their corners on the reversed side.
     */
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        if (this.dashTrail && ownDashHiddenNow()) return;
        float size = this.getQuadSize(partialTick);
        Vector3f look = camera.getLookVector();
        Vector3f[] corners = this.orientation == Orientation.FACING_MOTION
                ? RingGeometry.facing(this.spawnDirection,
                        new Vec3(look.x(), look.y(), look.z()), size)
                : RingGeometry.ground(size);
        Vec3 cameraPosition = camera.getPosition();
        float dx = (float) (Mth.lerp(partialTick, this.xo, this.x) - cameraPosition.x());
        float dy = (float) (Mth.lerp(partialTick, this.yo, this.y) - cameraPosition.y());
        float dz = (float) (Mth.lerp(partialTick, this.zo, this.z) - cameraPosition.z());
        for (Vector3f corner : corners) {
            corner.add(dx, dy, dz);
        }
        emitQuad(buffer, corners[0], this.getU0(), this.getV0(), corners[1], this.getU0(), this.getV1(),
                corners[2], this.getU1(), this.getV1(), corners[3], this.getU1(), this.getV0());
        emitQuad(buffer, corners[0], this.getU0(), this.getV0(), corners[3], this.getU1(), this.getV0(),
                corners[2], this.getU1(), this.getV1(), corners[1], this.getU0(), this.getV1());
    }

    private void emitQuad(VertexConsumer buffer, Vector3f a, float au, float av,
                          Vector3f b, float bu, float bv, Vector3f c, float cu, float cv,
                          Vector3f d, float du, float dv) {
        emitVertex(buffer, a, au, av);
        emitVertex(buffer, b, bu, bv);
        emitVertex(buffer, c, cu, cv);
        emitVertex(buffer, d, du, dv);
    }

    private static boolean ownDashHiddenNow() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !minecraft.options.getCameraType().isFirstPerson()) return false;
        int remaining = localDashHiddenUntilTick - minecraft.player.tickCount;
        return remaining > 0 && remaining <= DASH_SELF_HIDE_TICKS;
    }

    private void emitVertex(VertexConsumer buffer, Vector3f corner, float u, float v) {
        buffer.addVertex(corner.x(), corner.y(), corner.z())
                .setUv(u, v)
                .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
                .setLight(LightTexture.FULL_BRIGHT);
    }

    @Override
    public ParticleRenderType getRenderType() {return ADDITIVE_GLOW;}
}
