package com.hbm_m.particle.nt;

import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** 1:1-Port von {@code ParticleSmokePlume} ("launchSmoke"): anschwellende Startrauchwolke. */
public class ParticleSmokePlumeNT extends ParticleQuadNT {

    private static final ResourceLocation CONTRAIL = tex("contrail");

    public ParticleSmokePlumeNT(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, CONTRAIL);
        this.lifetime = 80 + random.nextInt(20);
        this.particleScale = 0.25F;
        this.fullBright = true;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.alpha = 1 - ((float) age / (float) lifetime);
        float prevScale = this.particleScale;
        this.particleScale = 0.25F + ((float) age / (float) lifetime) * 2;
        ++this.age;
        if (this.age == this.lifetime) this.dead = true;

        double bak = Math.sqrt(xd * xd + yd * yd + zd * zd);
        this.move(this.xd, this.yd + (this.particleScale - prevScale), this.zd);
        if (this.verticalCollision) this.yd = bak;

        xd *= 0.925;
        yd *= 0.925;
        zd *= 0.925;
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float pt, PoseStack pose) {
        VertexConsumer c = buffer();
        Random urandom = new Random(this.seed);
        Vec3 p = camPos(camera, pt);
        Vector3f l = left(camera);
        Vector3f u = up(camera);

        for (int i = 0; i < 6; i++) {
            float grey = urandom.nextFloat() * 0.75F + 0.1F;
            float scale = this.particleScale;
            float pX = (float) (p.x + urandom.nextGaussian() * 0.5 * scale);
            float pY = (float) (p.y + urandom.nextGaussian() * 0.5 * scale);
            float pZ = (float) (p.z + urandom.nextGaussian() * 0.5 * scale);
            quad(c, pX, pY, pZ, l, u, scale, grey, grey, grey, this.alpha, 0xF000F0, 0, 0, 1, 1);
        }
    }
}
