package com.hbm_m.particle.nt;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Gemeinsame Basis fuer die 1:1-Ports der klassischen {@code EntityFX}-Partikel des Originals, die
 * nur aus Billboard-Quads bestehen.
 *
 * <p>Im Original bekommt {@code renderParticle} die fuenf Kamera-Werte {@code rotX, rotXZ, rotZ,
 * rotYZ, rotXY}; die Ecke {@code (pos - rotX*s - rotYZ*s, y - rotXZ*s, z - rotZ*s - rotXY*s)} ist
 * dabei genau {@code pos - links*s - oben*s}. {@link #quad} bildet diese vier Ecken samt ihrer
 * UV-Zuordnung ab ({@code maxU/maxV, maxU/minV, minU/minV, minU/maxV}).</p>
 *
 * <p>{@code getFXLayer() == 1} bedeutet im Original "Blockatlas + Helligkeit am Ort",
 * {@code setBrightness(240)} bzw. {@code 15728880} "voll hell". Beides steuert {@link #fullBright}.</p>
 *
 * <p>Die Entity-ID, die das Original als Zufallssaat fuer die Wolkenform nimmt
 * ({@code new Random(getEntityId())}), ersetzt {@link #seed}: pro Partikel fest, pro Partikel
 * verschieden.</p>
 */
public abstract class ParticleQuadNT extends ParticleNT {

    public static final ResourceLocation PARTICLE_BASE = tex("particle_base");

    private static long nextSeed = 0;

    protected final ResourceLocation texture;
    /** Ersatz fuer {@code getEntityId()} des Originals. */
    protected final int seed;
    protected boolean fullBright = false;
    /** Original {@code particleScale}. */
    public float particleScale = 1F;
    /** Original {@code rotationPitch}/{@code prevRotationPitch} bei den drehenden Partikeln. */
    protected float rotationPitch, prevRotationPitch;

    protected ParticleQuadNT(ClientLevel level, double x, double y, double z, ResourceLocation texture) {
        super(level, x, y, z);
        this.texture = texture;
        this.seed = (int) (nextSeed++ & 0x7FFFFFFF);
    }

    public static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/particle/" + name + ".png");
    }

    /** Kamera-Links multipliziert mit dem Fallback-Skalierungsfaktor. */
    protected static Vector3f left(Camera camera) {
        return new Vector3f(camera.getLeftVector());
    }

    protected static Vector3f up(Camera camera) {
        return new Vector3f(camera.getUpVector());
    }

    protected double ix(float pt) { return Mth.lerp(pt, this.xo, this.x); }
    protected double iy(float pt) { return Mth.lerp(pt, this.yo, this.y); }
    protected double iz(float pt) { return Mth.lerp(pt, this.zo, this.z); }

    /** Kamerabezogene Position des Partikels, samt Fallback-Virtualisierung. */
    protected Vec3 camPos(Camera camera, float pt) {
        return virtualizedOffset(ix(pt), iy(pt), iz(pt), camera);
    }

    protected int light() {
        return fullBright ? 0xF000F0 : getLightColor();
    }

    /** Billboard in Originalreihenfolge; {@code (cx, cy, cz)} kamerabezogen. */
    protected void quad(VertexConsumer c, float cx, float cy, float cz, Vector3f l, Vector3f u, float s,
                        float r, float g, float b, float a, int light,
                        float u0, float v0, float u1, float v1) {
        vertex(c, cx - l.x * s - u.x * s, cy - l.y * s - u.y * s, cz - l.z * s - u.z * s, r, g, b, a, u1, v1, light);
        vertex(c, cx - l.x * s + u.x * s, cy - l.y * s + u.y * s, cz - l.z * s + u.z * s, r, g, b, a, u1, v0, light);
        vertex(c, cx + l.x * s + u.x * s, cy + l.y * s + u.y * s, cz + l.z * s + u.z * s, r, g, b, a, u0, v0, light);
        vertex(c, cx + l.x * s - u.x * s, cy + l.y * s - u.y * s, cz + l.z * s - u.z * s, r, g, b, a, u0, v1, light);
    }

    /** Ein Vertex im Format {@code POSITION_COLOR_TEX_LIGHTMAP}. */
    public static void vertex(VertexConsumer c, float x, float y, float z, float r, float g, float b, float a,
                              float u, float v, int light) {
        int ri = Mth.clamp((int) (r * 255F), 0, 255);
        int gi = Mth.clamp((int) (g * 255F), 0, 255);
        int bi = Mth.clamp((int) (b * 255F), 0, 255);
        int ai = Mth.clamp((int) (a * 255F), 0, 255);
        //? if < 1.21.1 {
        c.vertex(x, y, z).color(ri, gi, bi, ai).uv(u, v).uv2(light).endVertex();
        //?} else {
        /*c.addVertex(x, y, z).setColor(ri, gi, bi, ai).setUv(u, v).setLight(light);
        *///?}
    }

    /**
     * 1:1-Port von {@code EntityFXRotating.renderParticleRotated}: das Billboard wird um seine
     * eigene Normale um {@code rotationPitch} Grad gedreht (Rodrigues ohne den Parallelanteil,
     * genau wie im Original).
     */
    protected void quadRotated(VertexConsumer c, Camera camera, float pt, double scale,
                               float r, float g, float b, float a) {
        Vec3 p = camPos(camera, pt);
        Vector3f l = left(camera);
        Vector3f u = up(camera);
        float rotation = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * pt;

        double[][] v = {
                { -l.x * scale - u.x * scale, -l.y * scale - u.y * scale, -l.z * scale - u.z * scale },
                { -l.x * scale + u.x * scale, -l.y * scale + u.y * scale, -l.z * scale + u.z * scale },
                { l.x * scale + u.x * scale, l.y * scale + u.y * scale, l.z * scale + u.z * scale },
                { l.x * scale - u.x * scale, l.y * scale - u.y * scale, l.z * scale - u.z * scale } };

        double nX = ((v[1][1] - v[0][1]) * (v[2][2] - v[0][2])) - ((v[1][2] - v[0][2]) * (v[2][1] - v[0][1]));
        double nY = ((v[1][2] - v[0][2]) * (v[2][0] - v[0][0])) - ((v[1][0] - v[0][0]) * (v[2][2] - v[0][2]));
        double nZ = ((v[1][0] - v[0][0]) * (v[2][1] - v[0][1])) - ((v[1][1] - v[0][1]) * (v[2][0] - v[0][0]));
        double len = Math.sqrt(nX * nX + nY * nY + nZ * nZ);
        if (len < 1.0E-4D) { nX = 0; nY = 0; nZ = 0; } else { nX /= len; nY /= len; nZ /= len; }

        double cosTh = Math.cos(rotation * Math.PI / 180D);
        double sinTh = Math.sin(rotation * Math.PI / 180D);
        float[][] uv = { { 1, 1 }, { 1, 0 }, { 0, 0 }, { 0, 1 } };
        int light = light();

        for (int i = 0; i < 4; i++) {
            double x1 = v[i][0], y1 = v[i][1], z1 = v[i][2];
            double x0 = x1 * cosTh + (nY * z1 - nZ * y1) * sinTh;
            double y0 = y1 * cosTh + (nZ * x1 - nX * z1) * sinTh;
            double z0 = z1 * cosTh + (nX * y1 - nY * x1) * sinTh;
            vertex(c, (float) (p.x + x0), (float) (p.y + y0), (float) (p.z + z0), r, g, b, a, uv[i][0], uv[i][1], light);
        }
    }

    protected VertexConsumer buffer() {
        return ParticleEngineNT.buffer().getBuffer(getRenderType());
    }

    @Override
    public RenderType getRenderType() {
        return ClientRenderHandler.CustomRenderTypes.TOWER_PARTICLES.apply(texture);
    }

    @Override
    public abstract void render(VertexConsumer consumer, Camera camera, float partialTicks, PoseStack levelPoseStack);

    /** Vanilla {@code EntityFX.onUpdate}, wie ihn die meisten Unterklassen per {@code super} rufen. */
    protected void vanillaTick() {
        super.tick();
    }
}
