package com.hbm_m.particle;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.particle.nt.ParticleNT;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.Util;

import java.awt.Color;
import java.util.function.Function;

/**
 * Порт {@code ParticleGasFlame} (1.7.10): факельное пламя газовой факела.
 * Дымовой квайд с цветом, уходящим по HSB от жёлтого к красному за время жизни,
 * полный яр свет, noClip, подъём и затухание.
 */
public class ParticleGasFlame extends ParticleNT {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            com.hbm_m.lib.RefStrings.MODID, "textures/particle/particle_base.png");

    private final float colorMod;

    public ParticleGasFlame(ClientLevel level, double x, double y, double z, double mx, double my, double mz, float scale) {
        super(level, x, y, z);
        this.xd = mx;
        this.yd = my * 1.5D;
        this.zd = mz;
        this.quadSize = scale;
        this.colorMod = 0.8F + this.random.nextFloat() * 0.2F;
        this.noClip = true;
        this.gravity = 0;
        this.lifetime = 30 + this.random.nextInt(13);
        updateColor();
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        // Оригинал: движение, трение по XZ и подъём; гравитации нет (motionY сохранялся).
        this.xd *= 0.75D;
        this.zd *= 0.75D;
        this.yd += 0.005D;

        this.age++;
        if (this.age >= this.lifetime) {
            this.dead = true;
        }

        this.move(this.xd, this.yd, this.zd);
        updateColor();
    }

    /** HSB-цвет: жёлтый (60°) → красный, светлота падает со временем (1:1 с оригиналом). */
    private void updateColor() {
        float time = (float) this.age / (float) this.lifetime;
        Color color = Color.getHSBColor(Math.max((60 - time * 100) / 360F, 0.0F), 1 - time * 0.25F, 1 - time * 0.5F);
        this.rCol = color.getRed() / 255F * colorMod;
        this.gCol = color.getGreen() / 255F * colorMod;
        this.bCol = color.getBlue() / 255F * colorMod;
        this.alpha = 1.0F - time;
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
        int light = 15728880; // Оригинал getBrightnessForRender: полный яр свет
        int r = Mth.clamp((int) (this.rCol * 255F), 0, 255);
        int g = Mth.clamp((int) (this.gCol * 255F), 0, 255);
        int b = Mth.clamp((int) (this.bCol * 255F), 0, 255);
        int a = Mth.clamp((int) (this.alpha * 255F), 0, 255);

        org.joml.Quaternionf camQ = new org.joml.Quaternionf(camera.rotation());
        org.joml.Vector3f left = new org.joml.Vector3f(-1, 0, 0).rotate(camQ);
        org.joml.Vector3f up = new org.joml.Vector3f(0, 1, 0);

        float[][] corners = {
                { -1, -1, 1, 1 },
                { -1,  1, 1, 0 },
                {  1,  1, 0, 0 },
                {  1, -1, 0, 1 },
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
        return GasFireRenderTypes.TYPE.apply(TEXTURE);
    }

    /** Пламя: полупрозрачный квад с полным ярким светом. */
    private static final class GasFireRenderTypes extends RenderType {
        private static final Function<ResourceLocation, RenderType> TYPE = Util.memoize(
                texture -> create("hbm_m_gas_fire", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                        VertexFormat.Mode.QUADS, 1536, false, false,
                        RenderType.CompositeState.builder()
                                .setShaderState(POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                                .setCullState(NO_CULL)
                                .createCompositeState(false)));

        private GasFireRenderTypes(String s, VertexFormat v, VertexFormat.Mode m,
                                   int i, boolean b, boolean b2, Runnable r, Runnable r2) { super(s, v, m, i, b, b2, r, r2); }
    }
}
