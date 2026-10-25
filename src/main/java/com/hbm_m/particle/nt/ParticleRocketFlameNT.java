package com.hbm_m.particle.nt;

import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 1:1-Port von {@code ParticleRocketFlame}: zehn Flammenfetzen, die von Orange ueber Dunkel in
 * Rauch uebergehen und sich mit dem Alter aufweiten. Genutzt von missileContrail, exhaust
 * (soyuz/meteor), explosionLarge und den Explosionstruemmern.
 */
public class ParticleRocketFlameNT extends ParticleQuadNT {

    public ParticleRocketFlameNT(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, PARTICLE_BASE);
        this.lifetime = 300 + random.nextInt(50);
        this.particleScale = 1F;
        this.fullBright = true;
    }

    public ParticleRocketFlameNT setScale(float scale) {
        this.particleScale = scale;
        return this;
    }

    public ParticleRocketFlameNT setMaxAge(int maxAge) {
        this.lifetime = maxAge;
        return this;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.age++;
        if (this.age == this.lifetime) this.dead = true;
        this.xd *= 0.91D;
        this.yd *= 0.91D;
        this.zd *= 0.91D;
        this.move(this.xd, this.yd, this.zd);
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float pt, PoseStack pose) {
        VertexConsumer c = buffer();
        Random urandom = new Random(this.seed);
        Vec3 p = camPos(camera, pt);
        Vector3f l = left(camera);
        Vector3f u = up(camera);

        for (int i = 0; i < 10; i++) {
            float add = urandom.nextFloat() * 0.3F;
            float dark = 1 - Math.min(((float) age / (float) (lifetime * 0.25F)), 1);
            float r = 1 * dark + add;
            float g = 0.6F * dark + add;
            float b = add;
            float a = (float) Math.pow(1 - Math.min(((float) age / (float) lifetime), 1), 0.5);

            float spread = (float) Math.pow(((float) age / (float) lifetime) * 4F, 1.5) + 1F;
            spread *= this.particleScale;
            float scale = (urandom.nextFloat() * 0.5F + 0.1F + ((float) age / (float) lifetime) * 2F) * particleScale;
            float pX = (float) (p.x + (urandom.nextGaussian() - 1D) * 0.2F * spread);
            float pY = (float) (p.y + (urandom.nextGaussian() - 1D) * 0.5F * spread);
            float pZ = (float) (p.z + (urandom.nextGaussian() - 1D) * 0.2F * spread);
            quad(c, pX, pY, pZ, l, u, scale, r, g, b, a * 0.75F, 0xF000F0, 0, 0, 1, 1);
        }
    }
}
