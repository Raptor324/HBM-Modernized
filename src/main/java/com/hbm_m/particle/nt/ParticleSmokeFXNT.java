package com.hbm_m.particle.nt;

import java.awt.Color;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1-Nachbau von Vanillas 1.7.10-{@code EntitySmokeFX} samt der beiden Ableitungen, die das
 * Original daraus macht:
 * <ul>
 * <li>{@code ParticleGasFlame} ("gasfire") - Flamme, deren Farbe per HSB von Gelb ueber Rot ins
 * Dunkle laeuft, voll hell, ohne Kollision;</li>
 * <li>die per Reflection vergroesserten Rauchwolken von "volcano" und "bnuuy"
 * ({@code smokeParticleScale} ueberschrieben).</li>
 * </ul>
 * Gezeichnet wird mit den Vanilla-Sprites {@code minecraft:generic_0..7} aus dem Partikelatlas -
 * das sind die acht Rauchbilder, die das Original ueber {@code setParticleTextureIndex(7 - ...)}
 * durchlaeuft.
 */
public class ParticleSmokeFXNT extends ParticleQuadNT {

    public float smokeParticleScale;
    private int textureIndex = 7;
    private boolean gasFlame = false;
    private float colorMod = 1.0F;

    public ParticleSmokeFXNT(ClientLevel level, double x, double y, double z, double mX, double mY, double mZ, float scale) {
        super(level, x, y, z, TextureAtlas.LOCATION_PARTICLES);
        // EntityFX(world, x, y, z, 0, 0, 0): Zufallsbewegung, danach * 0.1 + gewuenschte Bewegung.
        double sx = (Math.random() * 2.0 - 1.0) * 0.4;
        double sy = (Math.random() * 2.0 - 1.0) * 0.4;
        double sz = (Math.random() * 2.0 - 1.0) * 0.4;
        double d0 = (Math.random() + Math.random() + 1.0) * 0.15;
        double d1 = Math.sqrt(sx * sx + sy * sy + sz * sz);
        this.xd = sx / d1 * d0 * 0.4 * 0.1 + mX;
        this.yd = (sy / d1 * d0 * 0.4 + 0.1) * 0.1 + mY;
        this.zd = sz / d1 * d0 * 0.4 * 0.1 + mZ;
        this.rCol = this.gCol = this.bCol = (float) (Math.random() * 0.30000001192092896D);
        float particleScale = (random.nextFloat() * 0.5F + 0.5F) * 2.0F;
        particleScale *= 0.75F;
        particleScale *= scale;
        this.smokeParticleScale = particleScale;
        this.lifetime = (int) (8.0D / (Math.random() * 0.8D + 0.2D));
        this.lifetime = (int) ((float) this.lifetime * scale);
        this.noClip = false;
    }

    /** {@code ParticleGasFlame(world, x, y, z, mX, mY, mZ, scale)}. */
    public static ParticleSmokeFXNT gasFlame(ClientLevel level, double x, double y, double z, double mX, double mY, double mZ, float scale) {
        ParticleSmokeFXNT fx = new ParticleSmokeFXNT(level, x, y, z, mX, mY * 1.5, mZ, scale);
        fx.gasFlame = true;
        fx.colorMod = 0.8F + fx.random.nextFloat() * 0.2F;
        fx.noClip = true;
        fx.lifetime = 30 + fx.random.nextInt(13);
        fx.fullBright = true;
        fx.updateColor();
        return fx;
    }

    private void updateColor() {
        float time = (float) this.age / (float) this.lifetime;
        Color color = Color.getHSBColor(Math.max((60 - time * 100) / 360F, 0.0F), 1 - time * 0.25F, 1 - time * 0.5F);
        this.rCol = color.getRed() / 255F * colorMod;
        this.gCol = color.getGreen() / 255F * colorMod;
        this.bCol = color.getBlue() / 255F * colorMod;
    }

    public void setMaxAge(int maxAge) {
        this.lifetime = maxAge;
    }

    @Override
    public void tick() {
        double prevMo = this.yd;
        smokeTick();
        if (gasFlame) {
            updateColor();
            this.yd = prevMo;
            this.xd *= 0.75D;
            this.yd += 0.005D;
            this.zd *= 0.75D;
        }
    }

    private void smokeTick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) this.dead = true;
        this.textureIndex = 7 - this.age * 8 / this.lifetime;
        this.yd += 0.004D;
        this.move(this.xd, this.yd, this.zd);
        if (this.y == this.yo) {
            this.xd *= 1.1D;
            this.zd *= 1.1D;
        }
        this.xd *= 0.9599999785423279D;
        this.yd *= 0.9599999785423279D;
        this.zd *= 0.9599999785423279D;
        if (this.onGround) {
            this.xd *= 0.699999988079071D;
            this.zd *= 0.699999988079071D;
        }
    }

    public static TextureAtlasSprite genericSprite(int index) {
        index = Mth.clamp(index, 0, 7);
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_PARTICLES)
                .apply(ResourceLocation.fromNamespaceAndPath("minecraft", "generic_" + index));
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float pt, PoseStack pose) {
        float f6 = Mth.clamp(((float) this.age + pt) / (float) this.lifetime * 32.0F, 0.0F, 1.0F);
        float size = 0.1F * this.smokeParticleScale * f6;
        TextureAtlasSprite sprite = genericSprite(textureIndex);
        Vec3 p = camPos(camera, pt);
        quad(buffer(), (float) p.x, (float) p.y, (float) p.z, left(camera), up(camera), size,
                rCol, gCol, bCol, alpha, light(), sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1());
    }
}
