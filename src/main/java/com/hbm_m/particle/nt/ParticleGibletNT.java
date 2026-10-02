package com.hbm_m.particle.nt;

import com.hbm_m.client.ClientRenderHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1-Port von {@code ParticleGiblet} ("giblets"): ein wegfliegender Fleisch-/Schleim-/Metallbrocken,
 * der im Flug trudelt und eine Spur aus Blockstaub (Redstone- bzw. Melonenblock) hinterlaesst.
 * Typ 2 (Metall) faellt doppelt so schnell und staubt nicht.
 */
public class ParticleGibletNT extends ParticleQuadNT {

    private final float momentumYaw;
    private final float momentumPitch;
    private final int gibType;

    public ParticleGibletNT(ClientLevel level, double x, double y, double z, double mX, double mY, double mZ, int gibType) {
        super(level, x, y, z, gibType == 2 ? tex("metal") : gibType == 1 ? tex("slime") : tex("meat"));
        this.xd = mX;
        this.yd = mY;
        this.zd = mZ;
        this.lifetime = 140 + random.nextInt(20);
        this.gravity = 2F;
        this.gibType = gibType;
        if (gibType == 2) this.gravity *= 2;
        this.momentumYaw = (float) random.nextGaussian() * 15F;
        this.momentumPitch = (float) random.nextGaussian() * 15F;
        this.particleScale = (random.nextFloat() * 0.5F + 0.5F) * 2.0F;
    }

    @Override
    public void tick() {
        vanillaTick();
        this.prevRotationPitch = this.rotationPitch;
        if (!this.onGround) {
            this.rotationPitch += this.momentumPitch;
            if (gibType == 2) return;
            Particle fx = Minecraft.getInstance().particleEngine.createParticle(
                    new BlockParticleOption(ParticleTypes.BLOCK, gibType == 1 ? Blocks.MELON.defaultBlockState() : Blocks.REDSTONE_BLOCK.defaultBlockState()),
                    x, y, z, 0, 0, 0);
            if (fx != null) {
                fx.setParticleSpeed(0, 0, 0);
                fx.setLifetime(20 + random.nextInt(20));
            }
        }
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float pt, PoseStack pose) {
        Vec3 p = camPos(camera, pt);
        float f10 = this.particleScale * 0.1F;
        // Original: UV (0,0),(0,1),(1,1),(1,0) in Eckreihenfolge.
        quad(buffer(), (float) p.x, (float) p.y, (float) p.z, left(camera), up(camera), f10,
                rCol, gCol, bCol, alpha, light(), 1, 1, 0, 0);
    }

    @Override
    public RenderType getRenderType() {
        return ClientRenderHandler.CustomRenderTypes.ASHES_PARTICLES.apply(texture);
    }

    public ResourceLocation texture() { return texture; }
}
