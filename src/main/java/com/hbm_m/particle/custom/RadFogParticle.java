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
// Частица радиационного тумана.
// Используется для создания эффекта радиационного тумана в зонах с высокой радиацией.
// Частица медленно увеличивается в размере и плавно исчезает, создавая атмосферный эффект.
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public class RadFogParticle extends TextureSheetParticle {

    // Конструктор частицы
    private final float initialAlpha;

    protected RadFogParticle(ClientLevel level, double x, double y, double z,
                             double xd, double yd, double zd,
                             SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);

        this.quadSize = 3.0F + (this.random.nextFloat() * 2.0F);
        this.friction = 0.96F;
        this.gravity = 0.0F;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.lifetime = 300 + this.random.nextInt(200);
        this.pickSprite(spriteSet);

        this.rCol = 0.85F;
        this.gCol = 0.9F;
        this.bCol = 0.5F;

        // Генерируем начальную альфу в ПРАВИЛЬНОМ диапазоне [0.2, 0.4]
        // и сохраняем ее.
        this.initialAlpha = 0.2F + (this.random.nextFloat() * 0.2F);
        this.alpha = this.initialAlpha;
    }

    @Override
    public void tick() {
        super.tick();
        this.updateLifecycle();
    }

    private void updateLifecycle() {

        // Затухание происходит относительно НАЧАЛЬНОЙ альфы, а не жестко заданного значения.
        this.alpha = this.initialAlpha * (1.0F - ((float)this.age / (float)this.lifetime));
        
        this.quadSize *= 1.005F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        // Этот тип рендера сообщает движку,
        // что частицы прозрачные и их нужно пытаться сортировать.
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        @NotNull
        public Particle createParticle(@NotNull SimpleParticleType particleType, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new RadFogParticle(level, x, y, z, dx, dy, dz, this.sprites);
        }
    }
}
