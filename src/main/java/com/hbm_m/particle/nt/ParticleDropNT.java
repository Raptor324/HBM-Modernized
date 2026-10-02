package com.hbm_m.particle.nt;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1-Port der beiden fast identischen Originale {@code ParticleSplash} (Tropfen aus
 * Fluessigkeitsbehaeltern, "splash") und {@code ParticleDeadLeaf} (fallendes totes Laub,
 * "deadleaf"). Beide nehmen den Vanilla-Tick, spiegeln ihre Textur je nach Partikelnummer und
 * unterscheiden sich nur in Schwere, Groesse, Fallbremse und darin, dass der Tropfen am Boden
 * verschwindet, das Blatt aber liegen bleibt.
 */
public class ParticleDropNT extends ParticleQuadNT {

    private final boolean splash;

    public static ParticleDropNT splash(ClientLevel level, double x, double y, double z) {
        return new ParticleDropNT(level, x, y, z, true);
    }

    public static ParticleDropNT leaf(ClientLevel level, double x, double y, double z) {
        return new ParticleDropNT(level, x, y, z, false);
    }

    private ParticleDropNT(ClientLevel level, double x, double y, double z, boolean splash) {
        super(level, x, y, z, splash ? tex("particle_splash") : tex("dead_leaf"));
        this.splash = splash;
        this.rCol = this.gCol = this.bCol = 1F - level.random.nextFloat() * 0.2F;
        this.particleScale = splash ? 0.4F : 0.1F;
        this.lifetime = 200 + level.random.nextInt(50);
        this.gravity = splash ? 0.4F : 0.2F;
        if (splash) this.alpha = 0.5F;
    }

    public void setColor(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }

    @Override
    public void tick() {
        vanillaTick();
        if (!this.onGround) {
            this.xd += random.nextGaussian() * 0.002D;
            this.zd += random.nextGaussian() * 0.002D;
            double cap = splash ? -0.5D : -0.025D;
            if (this.yd < cap) this.yd = cap;
        } else if (splash) {
            this.dead = true;
        }
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float pt, PoseStack pose) {
        Vec3 p = camPos(camera, pt);
        boolean flipU = this.seed % 2 == 0;
        boolean flipV = this.seed % 4 < 2;
        float minU = flipU ? 1 : 0;
        float maxU = flipU ? 0 : 1;
        float minV = flipV ? 1 : 0;
        float maxV = flipV ? 0 : 1;
        quad(buffer(), (float) p.x, (float) p.y, (float) p.z, left(camera), up(camera), particleScale,
                rCol, gCol, bCol, alpha, light(), minU, minV, maxU, maxV);
    }

    /** Textur-Resource fuer Tests/Atlas-Pruefungen. */
    public ResourceLocation texture() {
        return texture;
    }
}
