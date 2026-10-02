package com.hbm_m.particle.nt;

import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 1:1-Port von {@code ParticleContrail}: stehende Abgaswolke (exKerosene/exSolid/exHydrogen/
 * exBalefire, ABMContrail). Die Grundfarbe plus 0.2..0.4 Aufhellung pro Fetzen, voll hell.
 */
public class ParticleContrailNT extends ParticleQuadNT {

    private static final ResourceLocation CONTRAIL = tex("contrail");

    public ParticleContrailNT(ClientLevel level, double x, double y, double z) {
        this(level, x, y, z, 0F, 0F, 0F, 1F);
    }

    public ParticleContrailNT(ClientLevel level, double x, double y, double z, float r, float g, float b, float scale) {
        super(level, x, y, z, CONTRAIL);
        this.lifetime = 100 + random.nextInt(40);
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        this.particleScale = scale;
        this.fullBright = true;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.alpha = 1 - ((float) this.age / (float) this.lifetime);
        ++this.age;
        if (this.age == this.lifetime) this.dead = true;
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float pt, PoseStack pose) {
        VertexConsumer c = buffer();
        Random urandom = new Random(this.seed);
        Vec3 p = camPos(camera, pt);
        Vector3f l = left(camera);
        Vector3f u = up(camera);

        for (int i = 0; i < 6; i++) {
            float mod = urandom.nextFloat() * 0.2F + 0.2F;
            float scale = (this.alpha + 0.5F) * this.particleScale;
            float pX = (float) (p.x + urandom.nextGaussian() * 0.5 * this.particleScale);
            float pY = (float) (p.y + urandom.nextGaussian() * 0.5 * this.particleScale);
            float pZ = (float) (p.z + urandom.nextGaussian() * 0.5 * this.particleScale);
            quad(c, pX, pY, pZ, l, u, scale, rCol + mod, gCol + mod, bCol + mod, this.alpha, 0xF000F0, 0, 0, 1, 1);
        }
    }
}
