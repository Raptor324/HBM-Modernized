package com.hbm_m.particle.nt;

import java.awt.Color;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1-Port von {@code ParticleFoundry} ("foundry"): der fliessende Metallstrahl aus Giessrinnen und
 * Formen. Ein Streifen faellt {@code length} tief, ein zweiter kommt {@code offset} weit aus der
 * Rinne, beide mit der von unten nach oben laufenden {@code lava_gray}-Textur (16 Bilder/1,6 s).
 */
public class ParticleFoundryNT extends ParticleQuadNT {

    public static final ResourceLocation LAVA = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/lava_gray.png");

    private final int color;
    private final Direction dir;
    private final double length;
    private final double base;
    private final double offset;

    public ParticleFoundryNT(ClientLevel level, double x, double y, double z, int color, int direction, double length, double base, double offset) {
        super(level, x, y, z, LAVA);
        this.color = color;
        this.dir = Direction.from3DDataValue(direction);
        this.length = length;
        this.base = base;
        this.offset = offset;
        this.lifetime = 20;
        this.fullBright = true;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) this.dead = true;
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float pt, PoseStack pose) {
        VertexConsumer c = buffer();
        Vec3 p = camPos(camera, pt);
        float pX = (float) p.x, pY = (float) p.y, pZ = (float) p.z;

        // ForgeDirection.getRotation(UP): NORTH->EAST->SOUTH->WEST, UP/DOWN bleiben.
        Direction rot = dir.getAxis().isHorizontal() ? dir.getClockWise() : dir;
        double width = 0.0625 + ((this.age + pt) / this.lifetime) * 0.0625;
        double girth = 0.125 * (1 - ((this.age + pt) / this.lifetime));

        Color col = new Color(this.color).brighter();
        double brightener = 0.7D;
        float r = (int) (255D - (255D - col.getRed()) * brightener) / 255F;
        float g = (int) (255D - (255D - col.getGreen()) * brightener) / 255F;
        float b = (int) (255D - (255D - col.getBlue()) * brightener) / 255F;

        double dirXG = dir.getStepX() * girth;
        double dirZG = dir.getStepZ() * girth;
        double rotXW = rot.getStepX() * width;
        double rotZW = rot.getStepZ() * width;

        double uMin = 0.5 - width;
        double uMax = 0.5 + width;
        double vMin = 0;
        double vMax = length;
        double add = (int) (System.currentTimeMillis() / 100 % 16) / 16D;

        V v = (x, y, z, u, vv) -> vertex(c, (float) (pX + x), (float) (pY + y), (float) (pZ + z), r, g, b, 1F, (float) u, (float) vv, 0xF000F0);

        v.p(rotXW, girth, rotZW, uMax, vMax + add + girth);
        v.p(-rotXW, girth, -rotZW, uMin, vMax + add + girth);
        v.p(-rotXW, -length, -rotZW, uMin, vMin + add);
        v.p(rotXW, -length, rotZW, uMax, vMin + add);

        v.p(dirXG + rotXW, 0, dirZG + rotZW, uMax, vMax + add);
        v.p(dirXG - rotXW, 0, dirZG - rotZW, uMin, vMax + add);
        v.p(dirXG - rotXW, -length, dirZG - rotZW, uMin, vMin + add);
        v.p(dirXG + rotXW, -length, dirZG + rotZW, uMax, vMin + add);

        double wMin = 0;
        double wMax = girth;

        v.p(rotXW, girth, rotZW, wMin, vMax + add + girth);
        v.p(dirXG + rotXW, 0, dirZG + rotZW, wMax, vMax + add);
        v.p(dirXG + rotXW, -length, dirZG + rotZW, wMax, vMin + add);
        v.p(rotXW, -length, rotZW, wMin, vMin + add);

        v.p(-rotXW, girth, -rotZW, wMin, vMax + add + girth);
        v.p(dirXG - rotXW, 0, dirZG - rotZW, wMax, vMax + add);
        v.p(dirXG - rotXW, -length, dirZG - rotZW, wMax, vMin + add);
        v.p(-rotXW, -length, -rotZW, wMin, vMin + add);

        double dirOX = dir.getStepX() * offset;
        double dirOZ = dir.getStepZ() * offset;
        vMax = offset;

        v.p(rotXW, 0, rotZW, uMax, vMax - add);
        v.p(-rotXW, 0, -rotZW, uMin, vMax - add);
        v.p(-rotXW - dirOX, base, -rotZW - dirOZ, uMin, vMin - add);
        v.p(rotXW - dirOX, base, rotZW - dirOZ, uMax, vMin - add);

        v.p(rotXW, girth, rotZW, uMax, vMax - add + 0.25);
        v.p(-rotXW, girth, -rotZW, uMin, vMax - add + 0.25);
        v.p(-rotXW - dirOX, base + girth, -rotZW - dirOZ, uMin, vMin - add + 0.25);
        v.p(rotXW - dirOX, base + girth, rotZW - dirOZ, uMax, vMin - add + 0.25);

        v.p(rotXW, 0, rotZW, wMax, vMax - add + 0.75);
        v.p(rotXW, girth, rotZW, wMin, vMax - add + 0.75);
        v.p(rotXW - dirOX, base + girth, rotZW - dirOZ, wMin, vMin - add + 0.75);
        v.p(rotXW - dirOX, base, rotZW - dirOZ, wMax, vMin - add + 0.75);

        v.p(-rotXW, 0, -rotZW, wMax, vMax - add + 0.75);
        v.p(-rotXW, girth, -rotZW, wMin, vMax - add + 0.75);
        v.p(-rotXW - dirOX, base + girth, -rotZW - dirOZ, wMin, vMin - add + 0.75);
        v.p(-rotXW - dirOX, base, -rotZW - dirOZ, wMax, vMin - add + 0.75);

        vMax = 0.125F;
        v.p(dirXG + rotXW, 0, dirZG + rotZW, uMax, vMin + add + 0.75);
        v.p(dirXG - rotXW, 0, dirZG - rotZW, uMin, vMin + add + 0.75);
        v.p(-rotXW, girth, -rotZW, uMin, vMax + add + 0.75);
        v.p(rotXW, girth, rotZW, uMax, vMax + add + 0.75);
    }

    @FunctionalInterface
    private interface V { void p(double x, double y, double z, double u, double v); }

    @Override
    public RenderType getRenderType() {
        // Original: Blend aus, Tiefe schreibt - ein fester Strahl, keine Wolke.
        return ClientRenderHandler.CustomRenderTypes.ASHES_PARTICLES.apply(texture);
    }
}
