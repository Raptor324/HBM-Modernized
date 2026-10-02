package com.hbm_m.client.render.effect;

import org.joml.Matrix4f;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.entity.logic.EntityDeathBlast;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code RenderDeathBlast} und {@code RenderOrbitalLaser}: zwei achtseitige Strahlen (250 Bloecke hoch, aussen
 * gruen bzw. rot, innen magenta bzw. weiss; der Drehschritt ist wie im Original {@code rotateAroundY(45)} im Bogenmass).
 * Der Todesstrahl zeigt zusaetzlich die schrumpfende gruene Kugel ({@code Sphere.obj}) mit acht Leuchthuellen.
 */
public class OrbitalStrikeRenderer<T extends Entity> extends EntityRenderer<T> {

    private static final SimpleObjModel SPHERE = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/sphere.obj"));

    private final float outerR, outerG, outerB, innerR, innerG, innerB;

    public OrbitalStrikeRenderer(EntityRendererProvider.Context ctx, float outerR, float outerG, float outerB, float innerR, float innerG, float innerB) {
        super(ctx);
        this.outerR = outerR; this.outerG = outerG; this.outerB = outerB;
        this.innerR = innerR; this.innerG = innerG; this.innerB = innerB;
    }

    public static <E extends Entity> OrbitalStrikeRenderer<E> deathBlast(EntityRendererProvider.Context ctx) {
        return new OrbitalStrikeRenderer<>(ctx, 0F, 1F, 0F, 1F, 0F, 1F);
    }

    public static <E extends Entity> OrbitalStrikeRenderer<E> orbitalLaser(EntityRendererProvider.Context ctx) {
        return new OrbitalStrikeRenderer<>(ctx, 1F, 0F, 0F, 1F, 1F, 1F);
    }

    @Override
    public void render(T entity, float yaw, float interp, PoseStack pose, MultiBufferSource buffers, int light) {
        VertexConsumer beam = buffers.getBuffer(ClientRenderHandler.CustomRenderTypes.ORBITAL_BEAM);
        Matrix4f m = pose.last().pose();
        Vec3 vector = new Vec3(0.5D, 0, 0);

        for (int i = 0; i < 8; i++) {
            Vec3 next = vector.yRot(45);
            quad(beam, m, vector, next, 1F, outerR, outerG, outerB);
            vector = next;
        }

        for (int i = 0; i < 8; i++) {
            Vec3 next = vector.yRot(45);
            quad(beam, m, vector, next, 0.5F, innerR, innerG, innerB);
            vector = next;
        }

        if (entity instanceof EntityDeathBlast) renderOrb(entity, interp, pose, buffers);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, float f, float r, float g, float bl) {
        vc.vertex(m, (float) a.x * f, 250F, (float) a.z * f).color(r, g, bl, 1F).endVertex();
        vc.vertex(m, (float) a.x * f, 0F, (float) a.z * f).color(r, g, bl, 1F).endVertex();
        vc.vertex(m, (float) b.x * f, 0F, (float) b.z * f).color(r, g, bl, 1F).endVertex();
        vc.vertex(m, (float) b.x * f, 250F, (float) b.z * f).color(r, g, bl, 1F).endVertex();
    }

    private void renderOrb(T entity, float interp, PoseStack pose, MultiBufferSource buffers) {
        pose.pushPose();

        double scale = 10 - 10D * (((double) entity.tickCount + interp) / ((double) EntityDeathBlast.maxAge));
        double alpha = (((double) entity.tickCount + interp) / ((double) EntityDeathBlast.maxAge));
        if (scale < 0) scale = 0;

        pose.scale((float) scale, (float) scale, (float) scale);
        SPHERE.renderAllColor(pose, buffers.getBuffer(ClientRenderHandler.CustomRenderTypes.ORBITAL_ORB), 0.05F, 1.0F, 0.05F, (float) alpha);

        pose.scale(1.25F, 1.25F, 1.25F);
        VertexConsumer glow = buffers.getBuffer(ClientRenderHandler.CustomRenderTypes.ORBITAL_ORB_GLOW);
        for (int i = 0; i < 8; i++) {
            SPHERE.renderAllColor(pose, glow, 0F, 1F, 0F, (float) (alpha * 0.125));
            pose.scale(1.05F, 1.05F, 1.05F);
        }

        pose.popPose();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
