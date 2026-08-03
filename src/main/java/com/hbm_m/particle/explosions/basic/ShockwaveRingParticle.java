package com.hbm_m.particle.explosions.basic;

import com.hbm_m.client.render.ImmediateVertexWriter;
import com.hbm_m.particle.explosions.AbstractExplosionParticle;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Порт {@code com.hbm.particle.ParticleMukeWave} (1.7.10) — расширяющееся кольцо ударной волны.
 *
 * Поведение 1:1 с оригиналом:
 *  - целевой радиус {@code waveScale} передаётся как xSpeed (25/45/65 для small/standard/large);
 *  - радиус растёт экспоненциально: {@code (1 - e^(-age*0.125)) * waveScale};
 *  - время жизни {@code (int)(25 * waveScale / 45)};
 *  - альфа линейно затухает: {@code 1 - age/lifetime};
 *  - рендер — горизонтальный квад у земли (Y-0.25, от -scale до +scale), как в оригинале.
 *
 * Прежняя реализация игнорировала waveScale (фиксированный quadSize ~1.0, рост 1%/тик) —
 * кольцо было крошечным (~1.3 блока) вместо 25/45/65 блоков.
 */
public class ShockwaveRingParticle extends AbstractExplosionParticle {

    private final float waveScale;

    public ShockwaveRingParticle(ClientLevel level, double x, double y, double z,
                                 SpriteSet sprites, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, sprites);

        // 1.7.10 ParticleMukeWave.setup(scale, maxAge): scale — целевой радиус кольца
        this.waveScale = (float) (xSpeed > 0.0 ? xSpeed : 45.0);
        // 1.7.10 ExplosionCreator: wave.setup(waveScale, (int)(25F * waveScale / 45))
        this.lifetime = Math.max(5, (int) (25.0F * this.waveScale / 45.0F));

        // ФИЗИКА: волна статична по позиции
        this.gravity = 0.0F;
        this.hasPhysics = false;

        // ЦВЕТ: серо-белый (воздушная волна)
        this.rCol = 0.8F + this.random.nextFloat() * 0.2F;
        this.gCol = 0.8F + this.random.nextFloat() * 0.2F;
        this.bCol = 0.9F + this.random.nextFloat() * 0.1F;

        this.alpha = 1.0F;
        this.quadSize = 0.0F;
    }

    @Override
    public void tick() {
        // Не вызываем super.tick() — он двигает частицу по xd/yd/zd и применяет friction;
        // волна не движется, меняем только размер и прозрачность.
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        // 1.7.10 ParticleMukeWave: particleAlpha = 1 - ((age) / maxAge)
        this.alpha = 1.0F - ((float) this.age / (float) this.lifetime);
        // 1.7.10 ParticleMukeWave: scale = (1 - e^(-(age)*0.125)) * waveScale
        this.quadSize = (1.0F - (float) Math.pow(Math.E, -this.age * 0.125)) * this.waveScale;
    }

    /**
     * Горизонтальный квад у земли — точная копия геометрии ParticleMukeWave (Y-0.25,
     * от -scale до +scale по X/Z). UV-маппинг {@link ImmediateVertexWriter#worldQuad}
     * (u1v1, u1v0, u0v0, u0v1) совпадает с оригиналом.
     */
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 cam = camera.getPosition();
        float pX = (float) (Mth.lerp(partialTick, this.xo, this.x) - cam.x());
        float pY = (float) (Mth.lerp(partialTick, this.yo, this.y) - cam.y());
        float pZ = (float) (Mth.lerp(partialTick, this.zo, this.z) - cam.z());
        float scale = this.quadSize;
        float y = pY - 0.25F;
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        ImmediateVertexWriter.worldQuad(buffer,
                pX - scale, y, pZ - scale,
                pX - scale, y, pZ + scale,
                pX + scale, y, pZ + scale,
                pX + scale, y, pZ - scale,
                this.rCol, this.gCol, this.bCol, this.alpha,
                u0, v0, u1, v1);
    }

    public static class Provider extends AbstractExplosionParticle.Provider<ShockwaveRingParticle> {
        public Provider(SpriteSet sprites) {
            super(sprites, ShockwaveRingParticle::new);
        }
    }
}
