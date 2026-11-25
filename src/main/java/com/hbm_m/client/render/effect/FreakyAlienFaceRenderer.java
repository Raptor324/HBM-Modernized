package com.hbm_m.client.render.effect;

import com.hbm_m.entity.effect.EntityFreakyAlienFace;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Giant translucent freaky alien face, camera-facing billboard (same technique as {@code ChemicalRenderer}).
 * Fades in over the first second, pulses softly, fades out over the last second.
 */
public class FreakyAlienFaceRenderer extends EntityRenderer<EntityFreakyAlienFace> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/freaky_alien_face.png");
    private static final float SIZE = 20.0F; // half extent -> ~40 blocks wide face

    public FreakyAlienFaceRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0F;
    }

    @Override
    public void render(EntityFreakyAlienFace face, float yaw, float partialTick, PoseStack ps, MultiBufferSource buf, int light) {

        float age = face.tickCount + partialTick;
        float fadeIn = Mth.clamp(age / 20.0F, 0.0F, 1.0F);
        float fadeOut = Mth.clamp((EntityFreakyAlienFace.LIFETIME - age) / 20.0F, 0.0F, 1.0F);
        float pulse = 0.75F + 0.25F * Mth.sin(age * 0.15F);
        int alpha = (int) (230 * fadeIn * fadeOut * pulse);
        if (alpha <= 2) return;

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - this.entityRenderDispatcher.camera.getYRot()));
        ps.mulPose(Axis.XP.rotationDegrees(-this.entityRenderDispatcher.camera.getXRot()));

        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        var m = ps.last().pose();
        var n = ps.last().normal();
        int full = 0xF000F0;

        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, -SIZE, -SIZE, 0, 255, 255, 255, alpha, 1, 1, OverlayTexture.NO_OVERLAY, full, n, 0, 1, 0);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, SIZE, -SIZE, 0, 255, 255, 255, alpha, 0, 1, OverlayTexture.NO_OVERLAY, full, n, 0, 1, 0);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, SIZE, SIZE, 0, 255, 255, 255, alpha, 0, 0, OverlayTexture.NO_OVERLAY, full, n, 0, 1, 0);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, -SIZE, SIZE, 0, 255, 255, 255, alpha, 1, 0, OverlayTexture.NO_OVERLAY, full, n, 0, 1, 0);

        ps.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityFreakyAlienFace entity) {
        return TEXTURE;
    }
}
