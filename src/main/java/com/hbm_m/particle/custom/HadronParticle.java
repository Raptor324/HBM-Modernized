package com.hbm_m.particle.custom;

import com.hbm_m.particle.AdditiveParticleRenderType;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 1:1-Port von {@code ParticleHadron} (1.7.10): der aufblitzende Ring ueber einem Teilchenaufprall.
 *
 * <p>Er sitzt still an seinem Platz und tut nur zweierlei: er waechst
 * ({@code scale = alter * 0.15}) und verblasst gleichzeitig linear ueber seine zehn Ticks. Das
 * ergibt den kurzen, hellen Stoss, den der ICF-Reaktor und der Lichtbogenschweisser bei jedem
 * Arbeitsschritt ausstossen.</p>
 *
 * <p>Die Textur ist ein voll deckendes Blatt auf schwarzem Grund und funktioniert nur additiv -
 * siehe {@link AdditiveParticleRenderType}.</p>
 */
public class HadronParticle extends TextureSheetParticle {

    /** Original: {@code particleScale} 1.0, halbiert im kleinen Modus. */
    private final float baseScale;

    protected HadronParticle(ClientLevel level, double x, double y, double z, boolean small, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);

        this.baseScale = small ? 0.5F : 1F;
        this.lifetime = small ? 5 : 10;
        this.gravity = 0F;
        this.friction = 1F;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.alpha = 1F;
        this.quadSize = 0F;

        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();

        // Original: alpha = 1 - alter/hoechstalter, scale = alter * 0.15 * grundgroesse.
        float progress = Math.min(1F, this.age / (float) this.lifetime);
        this.alpha = 1F - progress;
        this.quadSize = this.age * 0.15F * baseScale;
    }

    @Override
    public int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT; // tess.setBrightness(240)
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return AdditiveParticleRenderType.INSTANCE;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z, double xd, double yd, double zd) {
            // Der Spawner reicht den kleinen Modus ueber die x-Geschwindigkeit durch.
            return new HadronParticle(level, x, y, z, xd > 0, sprites);
        }
    }
}
