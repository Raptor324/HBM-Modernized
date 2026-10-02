package com.hbm_m.particle.nt;

import java.awt.Color;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

/**
 * 1:1-Port der drei {@code EntityFXRotating}-Partikel des Originals:
 * <ul>
 * <li>{@code ParticleExplosionSmall} ("explosionSmall") - orangerote, sich drehende Explosionswolke;</li>
 * <li>{@code ParticleFlamethrower} ("flamethrower") - Flammenzunge in fuenf Varianten
 * (Feuer, Balefire, Digamma, Sauerstoff, Schwarz);</li>
 * <li>{@code ParticleBlackPowderSmoke} ("blackPowder") - Pulverdampf.</li>
 * </ul>
 * Das Drehvorzeichen haengt im Original an {@code getEntityId() % 2}; hier an {@link #seed}.
 */
public class ParticleRotatingNT extends ParticleQuadNT {

    public static final int META_FIRE = 0;
    public static final int META_BALEFIRE = 1;
    public static final int META_DIGAMMA = 2;
    public static final int META_OXY = 3;
    public static final int META_BLACK = 4;

    private enum Kind { EXPLOSION_SMALL, FLAMETHROWER, BLACK_POWDER }

    private final Kind kind;
    private float hue;
    private int type;

    private ParticleRotatingNT(ClientLevel level, double x, double y, double z, Kind kind) {
        super(level, x, y, z, PARTICLE_BASE);
        this.kind = kind;
    }

    private float spin() {
        return (this.seed % 2) - 0.5F;
    }

    private void hsb(float hueDeg) {
        Color color = Color.getHSBColor(hueDeg / 255F, 1F, 1F);
        this.rCol = color.getRed() / 255F;
        this.gCol = color.getGreen() / 255F;
        this.bCol = color.getBlue() / 255F;
    }

    public static ParticleRotatingNT explosionSmall(ClientLevel level, double x, double y, double z, float scale, float speedMult) {
        ParticleRotatingNT p = new ParticleRotatingNT(level, x, y, z, Kind.EXPLOSION_SMALL);
        p.lifetime = 25 + p.random.nextInt(10);
        p.particleScale = scale * 0.9F + p.random.nextFloat() * 0.2F;
        p.xd = level.random.nextGaussian() * speedMult;
        p.yd = 0;
        p.zd = level.random.nextGaussian() * speedMult;
        p.gravity = p.random.nextFloat() * -0.01F;
        p.hue = 20F + p.random.nextFloat() * 20F;
        p.hsb(p.hue);
        p.noClip = true;
        return p;
    }

    public static ParticleRotatingNT flamethrower(ClientLevel level, double x, double y, double z, int type) {
        ParticleRotatingNT p = new ParticleRotatingNT(level, x, y, z, Kind.FLAMETHROWER);
        p.lifetime = 20 + p.random.nextInt(10);
        p.particleScale = 0.5F;
        p.type = type;
        p.xd = level.random.nextGaussian() * 0.02;
        p.yd = 0;
        p.zd = level.random.nextGaussian() * 0.02;
        float initialColor = 15F + p.random.nextFloat() * 25F;
        if (type == META_BALEFIRE) initialColor = 65F + p.random.nextFloat() * 35F;
        if (type == META_DIGAMMA) initialColor = 0F - p.random.nextFloat() * 15F;
        p.hsb(initialColor);
        if (type == META_OXY || type == META_BLACK) p.rCol = p.gCol = p.bCol = 1F;
        p.fullBright = true;
        return p;
    }

    public static ParticleRotatingNT blackPowderSmoke(ClientLevel level, double x, double y, double z, float scale) {
        ParticleRotatingNT p = new ParticleRotatingNT(level, x, y, z, Kind.BLACK_POWDER);
        p.lifetime = 30 + p.random.nextInt(15);
        p.particleScale = scale * 0.9F + p.random.nextFloat() * 0.2F;
        p.gravity = 0F;
        p.hue = 20F + p.random.nextFloat() * 20F;
        p.hsb(p.hue);
        p.noClip = true;
        p.xd = p.yd = p.zd = 0;
        return p;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.age++;
        if (this.age >= this.lifetime) this.dead = true;
        this.prevRotationPitch = this.rotationPitch;
        float ageScaled = (float) this.age / (float) this.lifetime;

        switch (kind) {
            case EXPLOSION_SMALL -> {
                this.yd -= gravity;
                this.rotationPitch += (1 - ageScaled) * 5 * spin();
                this.xd *= 0.65D;
                this.zd *= 0.65D;
            }
            case FLAMETHROWER -> {
                this.xd *= 0.91D;
                this.yd *= 0.91D;
                this.zd *= 0.91D;
                this.yd += 0.01D;
                this.rotationPitch += 30 * spin();
            }
            case BLACK_POWDER -> {
                this.yd -= gravity;
                this.rotationPitch += (1 - ageScaled) * 2 * spin();
                this.xd *= 0.65D;
                this.yd *= 0.65D;
                this.zd *= 0.65D;
            }
        }
        this.move(this.xd, this.yd, this.zd);
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float pt, PoseStack pose) {
        VertexConsumer c = buffer();
        switch (kind) {
            case EXPLOSION_SMALL -> {
                double ageScaled = (double) (this.age + pt) / (double) this.lifetime;
                Color color = Color.getHSBColor(hue / 255F, Math.max(1F - (float) ageScaled * 2F, 0),
                        Mth.clamp(1.25F - (float) ageScaled * 2F, hue * 0.01F - 0.1F, 1F));
                float a = (float) Math.pow(1 - Math.min(ageScaled, 1), 0.25);
                double scale = (0.25 + 1 - Math.pow(1 - ageScaled, 4) + (this.age + pt) * 0.02) * this.particleScale;
                quadRotated(c, camera, pt, scale, color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, a * 0.5F);
            }
            case FLAMETHROWER -> {
                double ageScaled = (double) this.age / (double) this.lifetime;
                float r, g, b, a;
                if (type == META_OXY) {
                    a = (float) (1 - ageScaled);
                    float add = (float) ageScaled * 1.25F - 0.25F;
                    r = rCol - add; g = gCol - add * 0.75F; b = bCol;
                } else if (type == META_BLACK) {
                    a = (float) (1 - ageScaled);
                    float add = (float) ageScaled * 2F - 0.25F;
                    r = rCol - add * 0.75F; g = gCol - add; b = bCol - add * 0.5F;
                } else {
                    a = (float) Math.pow(1 - Math.min(ageScaled, 1), 0.5) * 0.5F;
                    float add = 0.75F - (float) ageScaled;
                    r = rCol + add; g = gCol + add; b = bCol + add;
                }
                double scale = (ageScaled * 1.25 + 0.25) * particleScale;
                quadRotated(c, camera, pt, scale, r, g, b, a);
            }
            case BLACK_POWDER -> {
                double ageScaled = (double) (this.age + pt) / (double) this.lifetime;
                Color color = Color.getHSBColor(hue / 255F, Math.max(1F - (float) ageScaled * 4F, 0),
                        Mth.clamp(1.25F - (float) ageScaled * 2F, 0.7F, 1F));
                float a = (float) Math.pow(1 - Math.min(ageScaled, 1), 0.25);
                double scale = (0.25 + ageScaled + (this.age + pt) * 0.025) * this.particleScale;
                quadRotated(c, camera, pt, scale, color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, a * 0.25F);
            }
        }
    }
}
