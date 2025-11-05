package com.hbm_m.particle.explosions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class MushroomSmokeParticle extends TextureSheetParticle {
    private final float initialY;

    protected MushroomSmokeParticle(ClientLevel level, double x, double y, double z,
                                    double xSpeed, double ySpeed, double zSpeed,
                                    float size, boolean isStem) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        this.initialY = (float) y;
        this.lifetime = 80; // 4 секунды
        this.gravity = isStem ? -0.02F : 0.01F; // Стебель поднимается быстрее
        this.hasPhysics = false;

        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

        this.quadSize = size;

        // Тёмно-серый дым
        float brightness = 0.2F + this.random.nextFloat() * 0.2F;
        this.rCol = brightness;
        this.gCol = brightness;
        this.bCol = brightness;
        this.alpha = 0.8F;
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

        // Движение
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;

        // Расширение облака
        this.quadSize += 0.15F;

        // Затухание в конце
        float fadeStart = this.lifetime * 0.6F;
        if (this.age > fadeStart) {
            float fadeProgress = (this.age - fadeStart) / (this.lifetime - fadeStart);
            this.alpha = 0.8F * (1.0F - fadeProgress);
        }

        // Турбулентность
        this.xd *= 0.96;
        this.zd *= 0.96;
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
                                       double xSpeed, double ySpeed, double zSpeed) {
            boolean isStem = ySpeed > 0.5; // Определяем стебель по скорости
            float size = isStem ? 8.0F : 12.0F;

            MushroomSmokeParticle particle = new MushroomSmokeParticle(
                    level, x, y, z, xSpeed, ySpeed, zSpeed, size, isStem
            );
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}