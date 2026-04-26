package com.hbm_m.particle.custom;


import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public class ExplosionWaveParticle extends TextureSheetParticle {

    private final float initialAlpha;

    protected ExplosionWaveParticle(ClientLevel level, double x, double y, double z,
                                    SpriteSet spriteSet, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        // Трение для замедления
        this.friction = 0.6F;

        // Скорость - дым поднимается вверх
        this.xd = xSpeed + (Math.random() * 2.0D - 1.0D) * 1.5D;
        this.yd = ySpeed + 0.5D + Math.random() * 0.1D; // Поднимается вверх
        this.zd = zSpeed + (Math.random() * 2.0D - 1.0D) * 1.5D;

        // Размер - дым разного размера
        this.quadSize = 1.5F + (this.random.nextFloat() * 2.5F);

        // Время жизни 10 секунд (200 тиков) с вариацией
        this.lifetime = 100 + this.random.nextInt(40);

        this.setSpriteFromAge(spriteSet);

        // Цвет дыма - серый/темный
        this.rCol = 0.3f + this.random.nextFloat() * 0.1f;
        this.gCol = 0.3f + this.random.nextFloat() * 0.1f;
        this.bCol = 0.3f + this.random.nextFloat() * 0.1f;

        // Начальная прозрачность
        this.alpha = 1f;
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
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
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
