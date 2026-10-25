package com.hbm_m.particle.nt;

import com.hbm_m.client.ClientRenderHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1-Port von {@code ParticleDebug} ("network" - kleine Energie-/Fluid-Symbole, die durch Leitungen
 * wandern) und {@code ParticleDebugLine} ("debugline"/"debugdrone" - eine Linie von pos nach
 * pos+motion, ohne Tiefentest, deren Helligkeit ueber 60 Ticks abfaellt).
 */
public class ParticleDebugNT extends ParticleQuadNT {

    private final boolean line;
    private final int color;
    private final double lx, ly, lz;

    /** {@code ParticleDebug} Typ 0 (Strom). */
    public static ParticleDebugNT power(ClientLevel level, double x, double y, double z, double mX, double mY, double mZ) {
        return new ParticleDebugNT(level, x, y, z, mX, mY, mZ, tex("debug_power"), false, 0xFFFFFF);
    }

    /** {@code ParticleDebug} Typ 1 (Fluid, eingefaerbt). */
    public static ParticleDebugNT fluid(ClientLevel level, double x, double y, double z, double mX, double mY, double mZ, int color) {
        ParticleDebugNT fx = new ParticleDebugNT(level, x, y, z, mX, mY, mZ, tex("debug_fluid"), false, color);
        fx.rCol = ((color & 0xff0000) >> 16) / 255F;
        fx.gCol = ((color & 0x00ff00) >> 8) / 255F;
        fx.bCol = (color & 0x0000ff) / 255F;
        return fx;
    }

    /** {@code ParticleDebugLine}. */
    public static ParticleDebugNT line(ClientLevel level, double x, double y, double z, double lx, double ly, double lz, int color) {
        return new ParticleDebugNT(level, x, y, z, lx, ly, lz, PARTICLE_BASE, true, color);
    }

    private ParticleDebugNT(ClientLevel level, double x, double y, double z, double mX, double mY, double mZ,
                            net.minecraft.resources.ResourceLocation tex, boolean line, int color) {
        super(level, x, y, z, tex);
        this.line = line;
        this.color = color;
        this.lx = mX;
        this.ly = mY;
        this.lz = mZ;
        this.xd = mX;
        this.yd = mY;
        this.zd = mZ;
        this.lifetime = line ? 60 : 10;
        this.noClip = true;
        this.fullBright = true;
    }

    @Override
    public void tick() {
        if (line) {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            if (this.age++ >= this.lifetime) this.dead = true;
            return;
        }
        vanillaTick();
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float pt, PoseStack pose) {
        Vec3 p = camPos(camera, pt);
        if (line) {
            VertexConsumer c = buffer();
            float bright = Mth.clamp(1F - (this.age + pt) / this.lifetime, 0F, 1F);
            int r = (int) (((color >> 16) & 0xFF) * bright);
            int g = (int) (((color >> 8) & 0xFF) * bright);
            int b = (int) ((color & 0xFF) * bright);
            //? if < 1.21.1 {
            c.vertex((float) p.x, (float) p.y, (float) p.z).color(r, g, b, 255).endVertex();
            c.vertex((float) (p.x + lx), (float) (p.y + ly), (float) (p.z + lz)).color(r, g, b, 255).endVertex();
            //?} else {
            /*c.addVertex((float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, 255);
            c.addVertex((float) (p.x + lx), (float) (p.y + ly), (float) (p.z + lz)).setColor(r, g, b, 255);
            *///?}
            return;
        }
        quad(buffer(), (float) p.x, (float) p.y, (float) p.z, left(camera), up(camera), 0.05F,
                rCol, gCol, bCol, alpha, 0xF000F0, 0, 0, 1, 1);
    }

    @Override
    public RenderType getRenderType() {
        return line ? ClientRenderHandler.CustomRenderTypes.DEBUG_LINES_NO_DEPTH : super.getRenderType();
    }
}
