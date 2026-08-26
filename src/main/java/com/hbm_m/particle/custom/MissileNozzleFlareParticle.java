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
 * Engine glare at the missile nozzle — flash + flare textures, yellow tint, flickering brightness.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class MissileNozzleFlareParticle extends TextureSheetParticle {

    /** 0 = flash.png, 1 = nuke_explosion_flare.png */
    public static int currentTextureLayer = 0;
    public static float currentSpawnScale = 1.0F;

    private final float flickerPhase;
    private final float sizePhase;
    private final float baseQuadSize;
    private final float baseR;
    private final float baseG;
    private final float baseB;
    private final float alphaMul;

    protected MissileNozzleFlareParticle(ClientLevel level, double x, double y, double z,
                                       double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz);
        this.lifetime = 6 + this.random.nextInt(4);
        int layer = Mth.clamp(currentTextureLayer, 0, 1);
        // SpriteSet.get(age, lifetime): lifetime must be > 0 (texture count), age selects index
        this.setSprite(sprites.get(layer, 2));
        this.flickerPhase = this.random.nextFloat() * ((float) Math.PI * 2.0F);
        this.sizePhase = this.random.nextFloat() * ((float) Math.PI * 2.0F);
        this.baseQuadSize = (0.28F + this.random.nextFloat() * 0.18F) * currentSpawnScale;
        this.quadSize = this.baseQuadSize;

        boolean flashLayer = layer == 0;
        if (flashLayer) {
            this.baseR = 1.0F;
            this.baseG = 0.9F + this.random.nextFloat() * 0.1F;
            this.baseB = 0.4F + this.random.nextFloat() * 0.12F;
            this.alphaMul = 0.95F;
        } else {
            this.baseR = 1.0F;
            this.baseG = 0.78F + this.random.nextFloat() * 0.12F;
            this.baseB = 0.12F + this.random.nextFloat() * 0.15F;
            this.alphaMul = 0.81F;
        }
        this.rCol = this.baseR;
        this.gCol = this.baseG;
        this.bCol = this.baseB;
        this.alpha = 0.75F + this.random.nextFloat() * 0.2F;
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

        float pulse = 0.42F + 0.58F * Mth.sin(this.age * 1.35F + this.flickerPhase);
        float sizePulse = 0.8F + 0.35F * Mth.sin(this.age * 1.05F + this.sizePhase);
        this.alpha = pulse * this.alphaMul;
        this.quadSize = this.baseQuadSize * sizePulse;

        float brighten = 0.88F + 0.12F * pulse;
        this.rCol = Math.min(1.0F, this.baseR * brighten);
        this.gCol = Math.min(1.0F, this.baseG * brighten);
        this.bCol = Math.min(1.0F, this.baseB * (0.92F + 0.08F * pulse));

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
            // Прокидываем спрайты в NT-порт (частицы спавнятся через ParticleEngineNT).
            com.hbm_m.particle.nt.MissileNozzleFlareNT.setSprites(sprites);
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new MissileNozzleFlareParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
