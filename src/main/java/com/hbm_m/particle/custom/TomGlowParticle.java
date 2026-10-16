package com.hbm_m.particle.custom;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.hbm_m.client.missile.track.MissileTrackWorldRender;
import com.hbm_m.particle.LongRangeParticleRenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Blue glow trailing Gerald's meteor ({@link com.hbm_m.entity.projectile.TomEntity}) - same
 * camera-facing billboard technique as {@link MissileNozzleFlareParticle}, tinted blue-white
 * to match the legacy {@code tom_flame.png} glow instead of an engine's yellow-orange flare.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class TomGlowParticle extends TextureSheetParticle {

    private final float flickerPhase;
    private final float sizePhase;
    private final float baseQuadSize;

    protected TomGlowParticle(ClientLevel level, double x, double y, double z,
                               double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz);
        this.lifetime = 8 + this.random.nextInt(4);
        this.setSprite(sprites.get(0, 1));
        this.flickerPhase = this.random.nextFloat() * ((float) Math.PI * 2.0F);
        this.sizePhase = this.random.nextFloat() * ((float) Math.PI * 2.0F);
        this.baseQuadSize = 0.9F + this.random.nextFloat() * 0.35F;
        this.quadSize = this.baseQuadSize;

        this.rCol = 0.55F + this.random.nextFloat() * 0.15F;
        this.gCol = 0.75F + this.random.nextFloat() * 0.15F;
        this.bCol = 1.0F;
        this.alpha = 0.7F + this.random.nextFloat() * 0.2F;
        this.hasPhysics = false;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        float pulse = 0.5F + 0.5F * Mth.sin(this.age * 1.2F + this.flickerPhase);
        float sizePulse = 0.85F + 0.3F * Mth.sin(this.age * 0.9F + this.sizePhase);
        this.alpha = 0.55F + pulse * 0.35F;
        this.quadSize = this.baseQuadSize * sizePulse;

        this.move(this.xd, this.yd, this.zd);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return LongRangeParticleRenderType.INSTANCE;
    }

    //? if < 1.21.1 {
    @Override
    public boolean shouldCull() {
        return false;
    }
    //?}

    @Override
    public int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 cam = camera.getPosition();
        double wx = Mth.lerp(partialTick, this.xo, this.x);
        double wy = Mth.lerp(partialTick, this.yo, this.y);
        double wz = Mth.lerp(partialTick, this.zo, this.z);
        MissileTrackWorldRender.CameraRelativePose virtual =
                MissileTrackWorldRender.virtualizeWorld(wx, wy, wz, cam);

        float relX = (float) virtual.relX();
        float relY = (float) virtual.relY();
        float relZ = (float) virtual.relZ();
        float scale = this.quadSize * virtual.screenScale();

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
        int light = LightTexture.FULL_BRIGHT;
        int ir = (int) (this.rCol * 255.0F), ig = (int) (this.gCol * 255.0F), ib = (int) (this.bCol * 255.0F), ia = (int) (this.alpha * 255.0F);
        com.hbm_m.platform.RenderHooks.particleVertex(buffer, corners[0].x(), corners[0].y(), corners[0].z(), u1, v1, ir, ig, ib, ia, light);
        com.hbm_m.platform.RenderHooks.particleVertex(buffer, corners[1].x(), corners[1].y(), corners[1].z(), u1, v0, ir, ig, ib, ia, light);
        com.hbm_m.platform.RenderHooks.particleVertex(buffer, corners[2].x(), corners[2].y(), corners[2].z(), u0, v0, ir, ig, ib, ia, light);
        com.hbm_m.platform.RenderHooks.particleVertex(buffer, corners[3].x(), corners[3].y(), corners[3].z(), u0, v1, ir, ig, ib, ia, light);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                        double x, double y, double z,
                                        double dx, double dy, double dz) {
            return new TomGlowParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
