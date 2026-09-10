package com.hbm_m.particle.nt;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 1:1-Port von {@code ParticleSpark} (1.7.10): der Funke, der beim Schweissen wegspringt.
 *
 * <p>Der Kniff ist die <b>Schleppe</b>: der Funke merkt sich die letzten vier bis sechs
 * Bewegungsschritte und zeichnet daraus eine Linie hinter sich her. Dadurch sieht man nicht einen
 * Punkt fliegen, sondern einen Strich - und weil die Schritte die tatsaechliche Bewegung sind,
 * biegt sich die Schleppe im Flug richtig mit.</p>
 *
 * <p>Er faellt mit halber Schwere und springt vom Boden mit acht Zehnteln zurueck, statt liegen zu
 * bleiben. Im kleinen Modus lebt er nur zwei bis vier Ticks und faellt sofort nach unten - das ist
 * die Fassung fuer Nahaufnahmen.</p>
 */
public class ParticleSparkNT extends ParticleNT {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            com.hbm_m.lib.RefStrings.MODID, "textures/particle/particle_base.png");

    /** Wie breit der Strich gezeichnet wird - im Original {@code glLineWidth(3F)}. */
    private static final float LINE_WIDTH = 0.02F;

    /** Original: {@code steps} - die letzten Bewegungsschritte. */
    private final List<double[]> steps = new ArrayList<>();
    /** Original: {@code thresh = 4 + rand(3)}. */
    private int thresh;

    public ParticleSparkNT(ClientLevel level, double x, double y, double z,
                           double mX, double mY, double mZ) {
        super(level, x, y, z);

        this.xd = mX;
        this.yd = mY;
        this.zd = mZ;

        this.thresh = 4 + random.nextInt(3);
        this.steps.add(new double[] { mX, mY, mZ });
        this.lifetime = 20 + random.nextInt(10);
        this.gravity = 0.5F;
        this.bbWidth = 0.02F;
        this.bbHeight = 0.02F;
        setPos(x, y, z);
    }

    /** 1:1-Port von {@code makeSmall}. */
    public ParticleSparkNT makeSmall(boolean small) {
        if (!small) return this;

        this.bbWidth = 0.01F;
        this.bbHeight = 0.01F;
        this.thresh = 3;
        this.lifetime = 2 + random.nextInt(3);
        this.yd = -Math.abs(this.yd);
        setPos(this.x, this.y, this.z);
        return this;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.dead = true;
            return;
        }

        steps.add(new double[] { this.xd, this.yd, this.zd });
        while (steps.size() > thresh) steps.remove(0);

        this.yd -= 0.04D * this.gravity;
        double lastY = this.yd;

        this.move(this.xd, this.yd, this.zd);

        // Original: der Funke springt zurueck, statt liegen zu bleiben.
        if (this.onGround) {
            this.onGround = false;
            this.yd = -lastY * 0.8D;
        }
    }

    @Override
    public void render(VertexConsumer vc, Camera camera, float partialTicks, PoseStack levelPoseStack) {
        if (steps.size() < 2) return;

        double wx = Mth.lerp(partialTicks, this.xo, this.x);
        double wy = Mth.lerp(partialTicks, this.yo, this.y);
        double wz = Mth.lerp(partialTicks, this.zo, this.z);

        Vec3 off = virtualizedOffset(wx, wy, wz, camera);
        float scale = virtualScale(wx, wy, wz, camera);

        // Die Blickrichtung der Kamera - der Strich wird quer dazu aufgespannt.
        org.joml.Quaternionf camQ = new org.joml.Quaternionf(camera.rotation());
        org.joml.Vector3f right = new org.joml.Vector3f(-1, 0, 0).rotate(camQ);

        int light = getLightColor();
        float half = LINE_WIDTH * scale * 0.5F;

        double px = off.x;
        double py = off.y;
        double pz = off.z;

        for (int i = 1; i < steps.size(); i++) {
            double[] step = steps.get(i);
            double nx = px + step[0] * scale;
            double ny = py + step[1] * scale;
            double nz = pz + step[2] * scale;

            quad(vc, (float) px, (float) py, (float) pz, (float) nx, (float) ny, (float) nz,
                    right, half, light);

            px = nx;
            py = ny;
            pz = nz;
        }
    }

    /** Ein Stueck der Schleppe als schmales, zur Kamera gedrehtes Band. */
    private static void quad(VertexConsumer vc, float ax, float ay, float az,
                             float bx, float by, float bz,
                             org.joml.Vector3f right, float half, int light) {

        float ox = right.x * half;
        float oy = right.y * half;
        float oz = right.z * half;

        vert(vc, ax - ox, ay - oy, az - oz, 0F, 0F, light);
        vert(vc, bx - ox, by - oy, bz - oz, 0F, 1F, light);
        vert(vc, bx + ox, by + oy, bz + oz, 1F, 1F, light);
        vert(vc, ax + ox, ay + oy, az + oz, 1F, 0F, light);
    }

    private static void vert(VertexConsumer vc, float x, float y, float z, float u, float v, int light) {
        //? if < 1.21.1 {
        vc.vertex(x, y, z).color(255, 255, 255, 255).uv(u, v).uv2(light).endVertex();
        //?} else {
        /*vc.addVertex(x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setLight(light);
        *///?}
    }

    @Override
    public RenderType getRenderType() {
        return com.hbm_m.client.ClientRenderHandler.CustomRenderTypes.TOWER_PARTICLES.apply(TEXTURE);
    }
}
