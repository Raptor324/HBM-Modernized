package com.hbm_m.particle.nt;

import java.util.Random;

import com.hbm_m.client.ClientRenderHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 1:1-Port von {@code ParticleAmatFlash} ("amat"): der Antimaterie-Blitz - 100 zufaellig gedrehte
 * Strahlenfaecher (Saat 432), additiv, die mit dem Alter wachsen und verblassen. Die Drehungen
 * akkumulieren wie im Original (fuenf {@code glRotatef} pro Strahl ohne Push/Pop).
 */
public class ParticleAmatFlashNT extends ParticleNT {

    private final float particleScale;

    public ParticleAmatFlashNT(ClientLevel level, double x, double y, double z, float scale) {
        super(level, x, y, z);
        this.lifetime = 10;
        this.particleScale = scale;
        this.xd = this.yd = this.zd = 0;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) this.dead = true;
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float pt, PoseStack pose) {
        VertexConsumer c = ParticleEngineNT.buffer().getBuffer(getRenderType());
        Vec3 p = virtualizedOffset(Mth.lerp(pt, xo, x), Mth.lerp(pt, yo, y), Mth.lerp(pt, zo, z), camera);

        Matrix4f m = new Matrix4f().translate((float) p.x, (float) p.y, (float) p.z)
                .scale(0.2F * particleScale);

        double intensity = (double) (this.age + pt) / (double) this.lifetime;
        float inverse = (float) (1.0D - intensity);
        Random random = new Random(432L);
        float scale = 0.5F;

        for (int i = 0; i < 100; i++) {
            m.rotateX((float) Math.toRadians(random.nextFloat() * 360.0F));
            m.rotateY((float) Math.toRadians(random.nextFloat() * 360.0F));
            m.rotateZ((float) Math.toRadians(random.nextFloat() * 360.0F));
            m.rotateX((float) Math.toRadians(random.nextFloat() * 360.0F));
            m.rotateY((float) Math.toRadians(random.nextFloat() * 360.0F));
            float vert1 = (random.nextFloat() * 20.0F + 5.0F + 10.0F) * (float) (intensity * scale);
            float vert2 = (random.nextFloat() * 2.0F + 1.0F + 2.0F) * (float) (intensity * scale);

            // TRIANGLE_FAN aus Mittelpunkt + vier Randpunkten = drei Dreiecke.
            Vector3f o = m.transformPosition(new Vector3f(0, 0, 0));
            Vector3f a = m.transformPosition(new Vector3f(-0.866F * vert2, vert1, -0.5F * vert2));
            Vector3f b = m.transformPosition(new Vector3f(0.866F * vert2, vert1, -0.5F * vert2));
            Vector3f d = m.transformPosition(new Vector3f(0.0F, vert1, 1.0F * vert2));
            tri(c, o, a, b, inverse);
            tri(c, o, b, d, inverse);
            tri(c, o, d, a, inverse);
        }
    }

    private static void tri(VertexConsumer c, Vector3f o, Vector3f a, Vector3f b, float alpha) {
        v(c, o, alpha);
        v(c, a, 0F);
        v(c, b, 0F);
    }

    private static void v(VertexConsumer c, Vector3f p, float alpha) {
        int ai = Mth.clamp((int) (alpha * 255F), 0, 255);
        //? if < 1.21.1 {
        c.vertex(p.x, p.y, p.z).color(255, 255, 255, ai).endVertex();
        //?} else {
        /*c.addVertex(p.x, p.y, p.z).setColor(255, 255, 255, ai);
        *///?}
    }

    @Override
    public RenderType getRenderType() {
        return ClientRenderHandler.CustomRenderTypes.ADDITIVE_TRIANGLES;
    }
}
