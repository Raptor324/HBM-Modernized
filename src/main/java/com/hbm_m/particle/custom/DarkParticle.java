package com.hbm_m.particle.custom;

import javax.annotation.Nonnull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DarkParticle extends TextureSheetParticle {

    protected DarkParticle(ClientLevel pLevel, double pX, double pY, double pZ,
                           SpriteSet pSpriteSet, double pXSpeed, double pYSpeed, double pZSpeed) {
        super(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);

        this.friction = 0.96F; // Трение, чтобы она замедлялась
        // Задаем небольшую случайную начальную скорость для эффекта "плавания"
        this.xd = pXSpeed + (Math.random() * 2.0D - 1.0D) * 0.05D;
        this.yd = pYSpeed + (Math.random() * 2.0D - 1.0D) * 0.05D;
        this.zd = pZSpeed + (Math.random() * 2.0D - 1.0D) * 0.05D;

        // Чтобы частицы были разного размера, как на скриншоте
        this.quadSize *= 0.75F + (this.random.nextFloat() * 0.5F);
        this.lifetime = (int)(8.0D / (Math.random() * 0.8D + 0.2D)) + 20; // Случайное время жизни
        
        this.setSpriteFromAge(pSpriteSet); // Выбираем текстуру

        // Установка цвета (можно не трогать, если текстура уже черная)
        this.rCol = 0.15f;
        this.gCol = 0.15f;
        this.bCol = 0.15f;
    }

    // Этот метод отвечает за то, как частица выглядит при рендере
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    // Фабрика для создания частиц
    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(@Nonnull SimpleParticleType particleType, @Nonnull ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new DarkParticle(level, x, y, z, this.spriteSet, dx, dy, dz);
        }
    }
}