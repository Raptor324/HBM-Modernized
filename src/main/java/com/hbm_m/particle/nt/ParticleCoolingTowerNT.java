package com.hbm_m.particle.nt;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1-Port von {@code ParticleCoolingTower} (1.7.10) - die Dampfschwade, die aus Kuehltuermen,
 * Lueftungsschaechten, Gasfackeln und Startrampen aufsteigt.
 *
 * <p>Der Kniff des Originals steckt in einer einzigen Zeile:
 * {@code scale = base + (max * age/life - base)^2}. Die Schwade beginnt klein, wird beim
 * Aufsteigen erst langsam, dann immer schneller breiter und verblasst dabei gleichmaessig - das
 * ergibt die typische, sich nach oben oeffnende Wolke statt einer Reihe gleich grosser Puffs.</p>
 *
 * <p>Dazu ein leichter Zufallsversatz je Tick ({@code strafe}), der mit dem Alter zunimmt, und -
 * sofern nicht mit {@code noWind} abgeschaltet - eine feste Drift nach Osten und Norden, damit
 * mehrere Schwaden nicht als Saeule uebereinanderstehen.</p>
 */
public class ParticleCoolingTowerNT extends ParticleNT {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            com.hbm_m.lib.RefStrings.MODID, "textures/particle/particle_base.png");

    private float baseScale = 1.0F;
    private float maxScale = 1.0F;
    private float lift = 0.3F;
    private float strafe = 0.075F;
    private boolean windDir = true;
    private float alphaMod = 0.25F;

    /** Die Groesse dieses Ticks - im Original {@code particleScale}. */
    private float scale = 1.0F;
    private float lastScale = 1.0F;
    private float lastAlpha = 0F;

    public ParticleCoolingTowerNT(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        // Original: 0.9 + rand * 0.05 - ein Hauch Unregelmaessigkeit im Weiss.
        float grey = 0.9F + level.random.nextFloat() * 0.05F;
        this.rCol = this.gCol = this.bCol = grey;
        this.noClip = true;
        this.lifetime = 20;
        this.alpha = 0F;
    }

    public void setBaseScale(float f) { this.baseScale = f; }
    public void setMaxScale(float f)  { this.maxScale = f; }
    public void setLift(float f)      { this.lift = f; }
    public void setLife(int i)        { this.lifetime = Math.max(1, i); }
    public void setStrafe(float f)    { this.strafe = f; }
    public void noWind()              { this.windDir = false; }
    public void alphaMod(float mod)   { this.alphaMod = mod; }

    public void setColor(int rgb) {
        this.rCol = ((rgb >> 16) & 0xFF) / 255F;
        this.gCol = ((rgb >> 8) & 0xFF) / 255F;
        this.bCol = (rgb & 0xFF) / 255F;
    }

    /** 1:1-Port von {@code onUpdate}. */
    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.lastScale = this.scale;
        this.lastAlpha = this.alpha;

        float ageScale = (float) this.age / (float) this.lifetime;

        this.alpha = alphaMod - ageScale * alphaMod;
        this.scale = baseScale + (float) Math.pow(maxScale * ageScale - baseScale, 2);

        this.age++;

        if (lift > 0 && this.yd < this.lift) this.yd += 0.01F;
        if (lift < 0 && this.yd > this.lift) this.yd -= 0.01F;

        this.xd += random.nextGaussian() * strafe * ageScale;
        this.zd += random.nextGaussian() * strafe * ageScale;

        if (windDir) {
            this.xd += 0.02D * ageScale;
            this.zd -= 0.01D * ageScale;
        }

        if (this.age >= this.lifetime) {
            this.dead = true;
            return;
        }

        this.move(this.xd, this.yd, this.zd);

        this.xd *= 0.925D;
        this.yd *= 0.925D;
        this.zd *= 0.925D;
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks, PoseStack levelPoseStack) {
        double wx = Mth.lerp(partialTicks, this.xo, this.x);
        double wy = Mth.lerp(partialTicks, this.yo, this.y);
        double wz = Mth.lerp(partialTicks, this.zo, this.z);

        Vec3 off = virtualizedOffset(wx, wy, wz, camera);
        float pX = (float) off.x;
        float pY = (float) off.y;
        float pZ = (float) off.z;

        float s = Mth.lerp(partialTicks, this.lastScale, this.scale) * virtualScale(wx, wy, wz, camera);
        float a = Mth.lerp(partialTicks, this.lastAlpha, this.alpha);

        int light = getLightColor();
        int cr = Mth.clamp((int) (this.rCol * 255F), 0, 255);
        int cg = Mth.clamp((int) (this.gCol * 255F), 0, 255);
        int cb = Mth.clamp((int) (this.bCol * 255F), 0, 255);
        int ca = Mth.clamp((int) (a * 255F), 0, 255);
        if (ca <= 0) return;

        org.joml.Quaternionf camQ = new org.joml.Quaternionf(camera.rotation());
        org.joml.Vector3f left = new org.joml.Vector3f(-1, 0, 0).rotate(camQ);
        org.joml.Vector3f up = new org.joml.Vector3f(0, 1, 0).rotate(camQ);

        // Original: die vier Ecken (-f-s), (-f+s), (+f+s), (+f-s) mal Groesse.
        float[][] corners = { { 1, 1 }, { -1, 1 }, { -1, -1 }, { 1, -1 } };

        for (float[] corner : corners) {
            float ox = left.x * corner[0] * s + up.x * corner[1] * s;
            float oy = left.y * corner[0] * s + up.y * corner[1] * s;
            float oz = left.z * corner[0] * s + up.z * corner[1] * s;

            //? if < 1.21.1 {
            consumer.vertex(pX + ox, pY + oy, pZ + oz)
                    .color(cr, cg, cb, ca)
                    .uv(corner[0] * 0.5F + 0.5F, corner[1] * 0.5F + 0.5F)
                    .uv2(light)
                    .endVertex();
            //?} else {
            /*consumer.addVertex(pX + ox, pY + oy, pZ + oz)
                    .setColor(cr, cg, cb, ca)
                    .setUv(corner[0] * 0.5F + 0.5F, corner[1] * 0.5F + 0.5F)
                    .setLight(light);
            *///?}
        }
    }

    @Override
    public RenderType getRenderType() {
        return com.hbm_m.client.ClientRenderHandler.CustomRenderTypes.TOWER_PARTICLES.apply(TEXTURE);
    }
}
