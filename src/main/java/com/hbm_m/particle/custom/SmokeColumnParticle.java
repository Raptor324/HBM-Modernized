package com.hbm_m.particle.custom;

import javax.annotation.Nonnull;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SmokeColumnParticle extends TextureSheetParticle {

    private final float initialAlpha;

    protected SmokeColumnParticle(ClientLevel level, double x, double y, double z,
                                  SpriteSet spriteSet, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        // Трение для замедления
        this.friction = 0.92F;

        // Скорость - дым поднимается вверх
        this.xd = xSpeed + (Math.random() * 2.0D - 1.0D) * 0.1D;
        this.yd = ySpeed + 0.05D + Math.random() * 0.05D; // Поднимается вверх
        this.zd = zSpeed + (Math.random() * 2.0D - 1.0D) * 0.1D;

        // Размер - дым разного размера
        this.quadSize = 1.5F + (this.random.nextFloat() * 1.0F);

        // Время жизни 10 секунд (200 тиков) с вариацией
        this.lifetime = 200 + this.random.nextInt(40);

        this.setSpriteFromAge(spriteSet);

        // Цвет дыма - серый/темный
        this.rCol = 0.3f + this.random.nextFloat() * 0.2f;
        this.gCol = 0.3f + this.random.nextFloat() * 0.2f;
        this.bCol = 0.3f + this.random.nextFloat() * 0.2f;

        // Начальная прозрачность
        this.alpha = 0.9f;
        this.initialAlpha = this.alpha;
    }

    @Override
    public void tick() {
        super.tick();

        // Постепенное рассеивание - альфа уменьшается со временем
        float lifeRatio = (float)this.age / (float)this.lifetime;

        // В начале дым более плотный, затем рассеивается
        if (lifeRatio > 0.3F) {
            this.alpha = initialAlpha * (1.0F - lifeRatio);
        }

        // Дым расширяется со временем
        this.quadSize += 0.01F;

        // Небольшое случайное движение для реалистичности
        this.xd += (Math.random() * 2.0D - 1.0D) * 0.001D;
        this.zd += (Math.random() * 2.0D - 1.0D) * 0.001D;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    // Фабрика для создания частиц
    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(@Nonnull SimpleParticleType particleType, @Nonnull ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new SmokeColumnParticle(level, x, y, z, this.spriteSet, dx, dy, dz);
        }
    }
}
