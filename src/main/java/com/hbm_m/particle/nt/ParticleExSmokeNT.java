package com.hbm_m.particle.nt;

import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** 1:1-Port von {@code ParticleExSmoke}: sechs graue Rauchfetzen, die schnell abbremsen. */
public class ParticleExSmokeNT extends ParticleQuadNT {

    public ParticleExSmokeNT(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, PARTICLE_BASE);
        this.lifetime = 100 + random.nextInt(40);
    }

    public void setMaxAge(int maxAge) {
        this.lifetime = maxAge;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.alpha = 1 - ((float) this.age / (float) this.lifetime);
        ++this.age;
        if (this.age == this.lifetime) this.dead = true;
        this.xd *= 0.7599999785423279D;
        this.yd *= 0.7599999785423279D;
        this.zd *= 0.7599999785423279D;
        this.move(this.xd, this.yd, this.zd);
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float pt, PoseStack pose) {
        VertexConsumer c = buffer();
        Random urandom = new Random(this.seed);
        Vec3 p = camPos(camera, pt);
        Vector3f l = left(camera);
        Vector3f u = up(camera);
        int light = light();

        for (int i = 0; i < 6; i++) {
            float grey = urandom.nextFloat() * 0.25F + 0.25F;
            float scale = urandom.nextFloat() + 0.5F;
            float pX = (float) (p.x + (urandom.nextGaussian() - 1D) * 0.75F);
            float pY = (float) (p.y + (urandom.nextGaussian() - 1D) * 0.75F);
            float pZ = (float) (p.z + (urandom.nextGaussian() - 1D) * 0.75F);
            quad(c, pX, pY, pZ, l, u, scale, grey, grey, grey, this.alpha, light, 0, 0, 1, 1);
        }
    }
}
