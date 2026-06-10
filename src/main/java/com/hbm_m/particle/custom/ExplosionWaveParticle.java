package com.hbm_m.particle.custom;


import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.render.ImmediateVertexWriter;
import com.hbm_m.particle.FullBrightParticleRenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ExplosionWaveParticle extends TextureSheetParticle {

    private final float initialAlpha;

    protected ExplosionWaveParticle(ClientLevel level, double x, double y, double z,
                                    SpriteSet spriteSet, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        this.friction = 0.6F;

        this.xd = xSpeed + (Math.random() * 2.0D - 1.0D) * 1.5D;
        this.yd = ySpeed + 0.5D + Math.random() * 0.1D;
        this.zd = zSpeed + (Math.random() * 2.0D - 1.0D) * 1.5D;

        this.quadSize = 1.5F + (this.random.nextFloat() * 2.5F);
        this.lifetime = 100 + this.random.nextInt(40);

        this.setSpriteFromAge(spriteSet);

        this.rCol = 0.3f + this.random.nextFloat() * 0.1f;
        this.gCol = 0.3f + this.random.nextFloat() * 0.1f;
        this.bCol = 0.3f + this.random.nextFloat() * 0.1f;

        this.alpha = 1f;
        this.initialAlpha = this.alpha;
    }

    @Override
    public void tick() {
        super.tick();

        float lifeRatio = (float) this.age / (float) this.lifetime;

        if (lifeRatio > 0.3F) {
            this.alpha = initialAlpha * (1.0F - lifeRatio);
        }

        this.quadSize += 0.01F;

        this.xd += (Math.random() * 2.0D - 1.0D) * 0.001D;
        this.zd += (Math.random() * 2.0D - 1.0D) * 0.001D;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return FullBrightParticleRenderType.INSTANCE;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 cam = camera.getPosition();
        float relX = (float) (Mth.lerp(partialTick, this.xo, this.x) - cam.x());
        float relY = (float) (Mth.lerp(partialTick, this.yo, this.y) - cam.y());
        float relZ = (float) (Mth.lerp(partialTick, this.zo, this.z) - cam.z());
        float scale = this.quadSize;
        Quaternionf rotation = camera.rotation();

        Vector3f[] corners = new Vector3f[]{
                new Vector3f(-scale, -scale, 0.0F),
                new Vector3f(-scale, scale, 0.0F),
                new Vector3f(scale, scale, 0.0F),
                new Vector3f(scale, -scale, 0.0F)
        };
        for (Vector3f corner : corners) {
            corner.rotate(rotation);
            corner.add(relX, relY, relZ);
        }

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        ImmediateVertexWriter.worldQuad(buffer,
                corners[0].x(), corners[0].y(), corners[0].z(),
                corners[1].x(), corners[1].y(), corners[1].z(),
                corners[2].x(), corners[2].y(), corners[2].z(),
                corners[3].x(), corners[3].y(), corners[3].z(),
                this.rCol, this.gCol, this.bCol, this.alpha,
                u0, v0, u1, v1);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType particleType, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new ExplosionWaveParticle(level, x, y, z, this.spriteSet, dx, dy, dz);
        }
    }
}
