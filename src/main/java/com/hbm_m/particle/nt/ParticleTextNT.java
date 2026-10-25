package com.hbm_m.particle.nt;

import com.hbm_m.client.ClientRenderHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 1:1-Port von {@code ParticleLetter} ("fireworks" - ein wachsender, verblassender Buchstabe) und
 * {@code ParticleText} ("debug" - ein langsam aufsteigender Text mit Schatten). Beide drehen sich
 * wie im Original nach Blickwinkel des Spielers, nicht nach der Kamera.
 */
public class ParticleTextNT extends ParticleNT {

    private final int color;
    private final String text;
    private final boolean letter;
    public float particleScale = 1F;

    /** {@code ParticleLetter}. */
    public static ParticleTextNT letter(ClientLevel level, double x, double y, double z, int color, char c) {
        ParticleTextNT fx = new ParticleTextNT(level, x, y, z, color, String.valueOf(c), true);
        fx.lifetime = 30;
        return fx;
    }

    /** {@code ParticleText}. */
    public static ParticleTextNT text(ClientLevel level, double x, double y, double z, int color, String text) {
        ParticleTextNT fx = new ParticleTextNT(level, x, y, z, color, text, false);
        fx.lifetime = 100;
        fx.yd = 0.01D;
        fx.noClip = true;
        return fx;
    }

    private ParticleTextNT(ClientLevel level, double x, double y, double z, int color, String text, boolean letter) {
        super(level, x, y, z);
        this.color = color;
        this.text = text;
        this.letter = letter;
        this.xd = this.zd = 0;
        this.yd = 0;
        this.particleScale = (random.nextFloat() * 0.5F + 0.5F) * 2.0F;
    }

    /** Original {@code multipleParticleScaleBy}. */
    public ParticleTextNT scaleBy(float f) {
        this.particleScale *= f;
        return this;
    }

    @Override
    public void tick() {
        if (letter) {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            if (this.age++ >= this.lifetime) this.dead = true;
            return;
        }
        super.tick();
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float pt, PoseStack pose) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        Font font = mc.font;
        Vec3 p = virtualizedOffset(Mth.lerp(pt, xo, x), Mth.lerp(pt, yo, y), Mth.lerp(pt, zo, z), camera);

        Matrix4f m = new Matrix4f().translate((float) p.x, (float) p.y, (float) p.z)
                .rotateY((float) Math.toRadians(-player.getYRot()))
                .rotateX((float) Math.toRadians(player.getXRot()))
                .scale(-1.0F, -1.0F, 1.0F);

        if (letter) {
            float time = (this.age + pt) * 4F / this.lifetime;
            float scale = (float) (1 - (1D / Math.pow(Math.E, time)));
            float a = 1 - ((this.age + pt) / (float) this.lifetime);
            if (a < 0) a = 0;
            int alpha = Mth.clamp((int) (a * 255), 10, 255);
            int col = (color & 0xFFFFFF) + (alpha << 24);
            m.scale(scale);
            font.drawInBatch(text, -(int) (font.width(text) * 0.5F), -(int) (font.lineHeight * 0.5F), col, false, m,
                    ParticleEngineNT.buffer(), Font.DisplayMode.NORMAL, 0, 0xF000F0);
        } else {
            m.scale(particleScale * 0.01F);
            font.drawInBatch(text, -(int) (font.width(text) * 0.5F), -(int) (font.lineHeight * 0.5F), color | 0xFF000000, true, m,
                    ParticleEngineNT.buffer(), Font.DisplayMode.NORMAL, 0, 0xF000F0);
        }
    }

    @Override
    public RenderType getRenderType() {
        return ClientRenderHandler.CustomRenderTypes.TOWER_PARTICLES.apply(ParticleQuadNT.PARTICLE_BASE);
    }
}
