package com.hbm_m.particle;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.hbm_m.particle.nt.ParticleNT;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.Util;

import java.util.function.Function;

/**
 * Порт {@code ParticleCoolingTower} из HBM 1.7.10 — клубы дыма из градирни:
 * подъём, ветровой снос, рост масштаба и затухание прозрачности.
 * Текстура — та же particle_base, что и у других дымовых частиц репозитория.
 */
public class ParticleCoolingTower extends ParticleNT {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            com.hbm_m.lib.RefStrings.MODID, "textures/particle/particle_base.png");

    private float baseScale = 1.0F;
    private float maxScale = 1.0F;
    private float lift = 0.3F;
    private float strafe = 0.075F;
    private boolean windDir = true;
    private float alphaMod = 0.25F;

    public ParticleCoolingTower(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        float tint = 0.9F + level.random.nextFloat() * 0.05F;
        this.rCol = this.gCol = this.bCol = tint;
        this.noClip = true;
        this.gravity = 0;
        this.bbWidth = 0.6F;
        this.bbHeight = 0.6F;
    }

    public void setBaseScale(float f) { this.baseScale = f; }
    public void setMaxScale(float f) { this.maxScale = f; }
    public void setLift(float f) { this.lift = f; }
    public void setLife(int i) { this.lifetime = i; }
    public void setStrafe(float f) { this.strafe = f; }
    public void noWind() { this.windDir = false; }
    public void alphaMod(float mod) { this.alphaMod = mod; }
    /** Аналог setRBGColorF из 1.7.10. */
    public void setColor(float r, float g, float b) { this.rCol = r; this.gCol = g; this.bCol = b; }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        float ageScale = (float) this.age / (float) this.lifetime;

        this.alpha = alphaMod - ageScale * alphaMod;
        this.quadSize = (float) (baseScale + Math.pow((maxScale * ageScale - baseScale), 2));

        this.age++;

        if (lift > 0 && this.yd < this.lift) {
            this.yd += 0.01F;
        }
        if (lift < 0 && this.yd > this.lift) {
            this.yd -= 0.01F;
        }

        this.xd += this.random.nextGaussian() * strafe * ageScale;
        this.zd += this.random.nextGaussian() * strafe * ageScale;

        if (windDir) {
            this.xd += 0.02 * ageScale;
            this.zd -= 0.01 * ageScale;
        }

        if (this.age == this.lifetime) {
            this.dead = true;
        }

        this.move(this.xd, this.yd, this.zd);

        this.xd *= 0.925;
        this.yd *= 0.925;
        this.zd *= 0.925;
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks, PoseStack levelPoseStack) {
        Vec3 off = virtualizedOffset(
                Mth.lerp(partialTicks, this.xo, this.x),
                Mth.lerp(partialTicks, this.yo, this.y),
                Mth.lerp(partialTicks, this.zo, this.z),
                camera);
        float pX = (float) off.x;
        float pY = (float) off.y;
        float pZ = (float) off.z;

        float scale = this.quadSize;
        int light = getLightColor();
        int r = Mth.clamp((int) (this.rCol * 255F), 0, 255);
        int g = Mth.clamp((int) (this.gCol * 255F), 0, 255);
        int b = Mth.clamp((int) (this.bCol * 255F), 0, 255);
        int a = Mth.clamp((int) (this.alpha * 255F), 0, 255);

        // Билборд к камере: базис left (наклонён к игроку) / up (мировой Y).
        org.joml.Quaternionf camQ = new org.joml.Quaternionf(camera.rotation());
        org.joml.Vector3f left = new org.joml.Vector3f(-1, 0, 0).rotate(camQ);
        org.joml.Vector3f up = new org.joml.Vector3f(0, 1, 0);

        // Углы квайда как в 1.7.10 renderParticle (fX/fY/fZ, sX/sZ от эффект-рендерера).
        float[][] corners = {
                { -1, -1, 1, 1 }, // (-left-up), uMaxV
                { -1,  1, 1, 0 }, // (-left+up)
                {  1,  1, 0, 0 }, // (+left+up)
                {  1, -1, 0, 1 }, // (+left-up)
        };
        for (float[] cn : corners) {
            float ox = left.x * cn[0] * scale + up.x * cn[1] * scale;
            float oy = left.y * cn[0] * scale + up.y * cn[1] * scale;
            float oz = left.z * cn[0] * scale + up.z * cn[1] * scale;
            float u = cn[2];
            float v = cn[3];
            //? if < 1.21.1 {
            consumer.vertex(pX + ox, pY + oy, pZ + oz)
                    .color(r, g, b, a)
                    .uv(u, v)
                    .uv2(light)
                    .endVertex();
            //?} else {
            /*consumer.addVertex(pX + ox, pY + oy, pZ + oz)
                    .setColor(r, g, b, a)
                    .setUv(u, v)
                    .setLight(light);
            *///?}
        }
    }

    @Override
    public RenderType getRenderType() {
        return TowerRenderTypes.TYPE.apply(TEXTURE);
    }

    /** Дым градирни: полупрозрачный квад с lightmap, как слой 1 в 1.7.10. */
    private static final class TowerRenderTypes extends RenderType {
        private static final Function<ResourceLocation, RenderType> TYPE = Util.memoize(
                texture -> create("hbm_m_cooling_tower", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                        VertexFormat.Mode.QUADS, 1536, false, false,
                        RenderType.CompositeState.builder()
                                .setShaderState(POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                                .setCullState(NO_CULL)
                                .setLightmapState(LIGHTMAP)
                                .setDepthTestState(LEQUAL_DEPTH_TEST)
                                .setWriteMaskState(COLOR_WRITE)
                                .setOutputState(TRANSLUCENT_TARGET)
                                .createCompositeState(false)));

        private TowerRenderTypes(String s, VertexFormat v, VertexFormat.Mode m,
                                 int i, boolean b, boolean b2, Runnable r, Runnable r2) { super(s, v, m, i, b, b2, r, r2); }
    }
}
