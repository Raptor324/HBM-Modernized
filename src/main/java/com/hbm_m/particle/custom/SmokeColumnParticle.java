package com.hbm_m.particle.custom;

import com.hbm_m.block.machines.LaunchPadBlock;
import com.hbm_m.block.machines.LaunchPadRustedBlock;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.block.Block;

public class SmokeColumnParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    protected SmokeColumnParticle(ClientLevel level, double x, double y, double z,
                                  double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz);
        this.sprites = sprites;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.lifetime = 80 + this.random.nextInt(20);
        this.quadSize = 0.25F;
        float gray = 0.1F + this.random.nextFloat() * 0.75F;
        this.rCol = this.gCol = this.bCol = gray;
        this.alpha = 1.0F;
        this.pickSprite(sprites);
    }

    private void updatePadCollision() {
        Block block = this.level.getBlockState(BlockPos.containing(this.x, this.y, this.z)).getBlock();
        boolean onLaunchPad = block instanceof LaunchPadBlock || block instanceof LaunchPadRustedBlock;
        this.hasPhysics = !onLaunchPad;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.updatePadCollision();

        this.alpha = 1.0F - ((float) this.age / (float) this.lifetime);
        float prevScale = this.quadSize;
        this.quadSize = 0.25F + ((float) this.age / (float) this.lifetime) * 2.0F;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.move(this.xd, this.yd + (this.quadSize - prevScale), this.zd);

        this.xd *= 0.91D;
        this.yd *= 0.91D;
        this.zd *= 0.91D;

        if (!this.removed) {
            this.setSpriteFromAge(this.sprites);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
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
            return new SmokeColumnParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
