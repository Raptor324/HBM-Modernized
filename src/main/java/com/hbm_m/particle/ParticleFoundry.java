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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.function.Function;

import net.minecraft.Util;

/**
 * Порт {@code ParticleFoundry} из HBM 1.7.10 — струя расплавленного металла,
 * льющегося из желоба литейки. Геометрия, цвета и «прокрутка» UV перенесены
 * 1:1; вместо немедленного GL-рендера 1.7.10 вершины пишутся в VertexConsumer
 * рендертайпа {@link #getRenderType()} (движок ParticleEngineNT).
 */
public class ParticleFoundry extends ParticleNT {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            com.hbm_m.lib.RefStrings.MODID, "textures/block/machine/lava_gray.png");

    private final int color;
    private final Direction dir;
    /** Насколько далеко металл выплёскивается вниз от базовой точки. */
    private final double length;
    /** Металл, идущий прямо из крана — выше или рядом с базовой точкой. */
    private final double base;
    /** Насколько далеко уходит назад базовая часть. */
    private final double offset;

    public ParticleFoundry(ClientLevel level, double x, double y, double z, int color, int direction,
                           double length, double base, double offset) {
        super(level, x, y, z);
        this.color = color;
        this.dir = Direction.from3DDataValue(direction);
        this.length = length;
        this.base = base;
        this.offset = offset;
        this.lifetime = 20;
        this.noClip = true;
        this.gravity = 0; // в оригинале частица неподвижна, только стареет
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.dead = true;
        }
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks, PoseStack levelPoseStack) {
        Vec3 cam = camera.getPosition();
        float pX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cam.x);
        float pY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cam.y);
        float pZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cam.z);

        // ForgeDirection.getRotation(UP) — поворот вокруг вертикальной оси.
        Direction rot = this.dir.getClockWise();
        double prog = (this.age + partialTicks) / (double) this.lifetime;
        double width = 0.0625 + prog * 0.0625;
        double girth = 0.125 * (1 - prog);

        // Оригинал: Color(color).brighter(), затем приглушение к белому на 0.7.
        Color c = new Color(this.color).brighter();
        double brightener = 0.7D;
        int r = (int) (255D - (255D - c.getRed()) * brightener);
        int g = (int) (255D - (255D - c.getGreen()) * brightener);
        int b = (int) (255D - (255D - c.getBlue()) * brightener);

        // Оригинал: tess.setBrightness(240) — полная яркость.
        final int light = 240;

        double dirXG = dir.getStepX() * girth;
        double dirZG = dir.getStepZ() * girth;
        double rotXW = rot.getStepX() * width;
        double rotZW = rot.getStepZ() * width;

        double uMin = 0.5 - width;
        double uMax = 0.5 + width;
        double vMin = 0;
        double vMax = length;

        double add = (int) (System.currentTimeMillis() / 100 % 16) / 16D;

        //lower back
        vert(consumer, pX, pY, pZ, rotXW, girth, rotZW, uMax, vMax + add + girth, r, g, b, light);
        vert(consumer, pX, pY, pZ, -rotXW, girth, -rotZW, uMin, vMax + add + girth, r, g, b, light);
        vert(consumer, pX, pY, pZ, -rotXW, -length, -rotZW, uMin, vMin + add, r, g, b, light);
        vert(consumer, pX, pY, pZ, rotXW, -length, rotZW, uMax, vMin + add, r, g, b, light);

        //lower front
        vert(consumer, pX, pY, pZ, dirXG + rotXW, 0, dirZG + rotZW, uMax, vMax + add, r, g, b, light);
        vert(consumer, pX, pY, pZ, dirXG - rotXW, 0, dirZG - rotZW, uMin, vMax + add, r, g, b, light);
        vert(consumer, pX, pY, pZ, dirXG - rotXW, -length, dirZG - rotZW, uMin, vMin + add, r, g, b, light);
        vert(consumer, pX, pY, pZ, dirXG + rotXW, -length, dirZG + rotZW, uMax, vMin + add, r, g, b, light);

        double wMin = 0;
        double wMax = girth;

        //lower left
        vert(consumer, pX, pY, pZ, rotXW, girth, rotZW, wMin, vMax + add + girth, r, g, b, light);
        vert(consumer, pX, pY, pZ, dirXG + rotXW, 0, dirZG + rotZW, wMax, vMax + add, r, g, b, light);
        vert(consumer, pX, pY, pZ, dirXG + rotXW, -length, dirZG + rotZW, wMax, vMin + add, r, g, b, light);
        vert(consumer, pX, pY, pZ, rotXW, -length, rotZW, wMin, vMin + add, r, g, b, light);

        //lower right
        vert(consumer, pX, pY, pZ, -rotXW, girth, -rotZW, wMin, vMax + add + girth, r, g, b, light);
        vert(consumer, pX, pY, pZ, dirXG - rotXW, 0, dirZG - rotZW, wMax, vMax + add, r, g, b, light);
        vert(consumer, pX, pY, pZ, dirXG - rotXW, -length, dirZG - rotZW, wMax, vMin + add, r, g, b, light);
        vert(consumer, pX, pY, pZ, -rotXW, -length, -rotZW, wMin, vMin + add, r, g, b, light);

        double dirOX = dir.getStepX() * offset;
        double dirOZ = dir.getStepZ() * offset;

        vMax = offset;

        //upper back
        vert(consumer, pX, pY, pZ, rotXW, 0, rotZW, uMax, vMax - add, r, g, b, light);
        vert(consumer, pX, pY, pZ, -rotXW, 0, -rotZW, uMin, vMax - add, r, g, b, light);
        vert(consumer, pX, pY, pZ, -rotXW - dirOX, base, -rotZW - dirOZ, uMin, vMin - add, r, g, b, light);
        vert(consumer, pX, pY, pZ, rotXW - dirOX, base, rotZW - dirOZ, uMax, vMin - add, r, g, b, light);

        //upper front
        vert(consumer, pX, pY, pZ, rotXW, girth, rotZW, uMax, vMax - add + 0.25, r, g, b, light);
        vert(consumer, pX, pY, pZ, -rotXW, girth, -rotZW, uMin, vMax - add + 0.25, r, g, b, light);
        vert(consumer, pX, pY, pZ, -rotXW - dirOX, base + girth, -rotZW - dirOZ, uMin, vMin - add + 0.25, r, g, b, light);
        vert(consumer, pX, pY, pZ, rotXW - dirOX, base + girth, rotZW - dirOZ, uMax, vMin - add + 0.25, r, g, b, light);

        //upper left
        vert(consumer, pX, pY, pZ, rotXW, 0, rotZW, wMax, vMax - add + 0.75, r, g, b, light);
        vert(consumer, pX, pY, pZ, rotXW, girth, rotZW, wMin, vMax - add + 0.75, r, g, b, light);
        vert(consumer, pX, pY, pZ, rotXW - dirOX, base + girth, rotZW - dirOZ, wMin, vMin - add + 0.75, r, g, b, light);
        vert(consumer, pX, pY, pZ, rotXW - dirOX, base, rotZW - dirOZ, wMax, vMin - add + 0.75, r, g, b, light);

        //upper right
        vert(consumer, pX, pY, pZ, -rotXW, 0, -rotZW, wMax, vMax - add + 0.75, r, g, b, light);
        vert(consumer, pX, pY, pZ, -rotXW, girth, -rotZW, wMin, vMax - add + 0.75, r, g, b, light);
        vert(consumer, pX, pY, pZ, -rotXW - dirOX, base + girth, -rotZW - dirOZ, wMin, vMin - add + 0.75, r, g, b, light);
        vert(consumer, pX, pY, pZ, -rotXW - dirOX, base, -rotZW - dirOZ, wMax, vMin - add + 0.75, r, g, b, light);

        vMax = 0.125F;

        //bend
        vert(consumer, pX, pY, pZ, dirXG + rotXW, 0, dirZG + rotZW, uMax, vMin + add + 0.75, r, g, b, light);
        vert(consumer, pX, pY, pZ, dirXG - rotXW, 0, dirZG - rotZW, uMin, vMin + add + 0.75, r, g, b, light);
        vert(consumer, pX, pY, pZ, -rotXW, girth, -rotZW, uMin, vMax + add + 0.75, r, g, b, light);
        vert(consumer, pX, pY, pZ, rotXW, girth, rotZW, uMax, vMax + add + 0.75, r, g, b, light);
    }

    /** POSITION_COLOR_TEX_LIGHTMAP: position, color, uv0, uv2. */
    private static void vert(VertexConsumer consumer, float pX, float pY, float pZ,
                             double vx, double vy, double vz, double u, double v,
                             int r, int g, int b, int light) {
        //? if < 1.21.1 {
        consumer.vertex(pX + vx, pY + vy, pZ + vz)
                .color(r, g, b, 255)
                .uv((float) u, (float) v)
                .uv2(light)
                .endVertex();
        //?} else {
        /*consumer.addVertex((float) (pX + vx), (float) (pY + vy), (float) (pZ + vz))
                .setColor(r, g, b, 255)
                .setUv((float) u, (float) v)
                .setLight(light);
        *///?}
    }

    @Override
    public RenderType getRenderType() {
        return FoundryRenderTypes.TYPE.apply(TEXTURE);
    }

    /** Рендертайп струи: как в 1.7.10 — без блендинга, без cull, запись глубины. */
    private static final class FoundryRenderTypes extends RenderType {
        private static final Function<ResourceLocation, RenderType> TYPE = Util.memoize(
                texture -> create("hbm_m_foundry", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                        VertexFormat.Mode.QUADS, 4096, false, false,
                        RenderType.CompositeState.builder()
                                .setShaderState(POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                                .setTransparencyState(NO_TRANSPARENCY)
                                .setCullState(NO_CULL)
                                .setLightmapState(LIGHTMAP)
                                .setDepthTestState(LEQUAL_DEPTH_TEST)
                                .setWriteMaskState(COLOR_DEPTH_WRITE)
                                .createCompositeState(false)));

        private FoundryRenderTypes(String s, VertexFormat v, VertexFormat.Mode m,
                                   int i, boolean b, boolean b2, Runnable r, Runnable r2) { super(s, v, m, i, b, b2, r, r2); }
    }
}
