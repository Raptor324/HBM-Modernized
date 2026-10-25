package com.hbm_m.client.render.projectile;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.entity.projectile.EntityFallingNuke;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** 1:1 {@code RenderFallingNuke}: Little-Boy-Huelle mit Baukastentextur, nach Meta gedreht, neigt sich im Fall. */
public class FallingNukeRenderer extends EntityRenderer<EntityFallingNuke> {

    private static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/projectiles/lilboy1.obj"));
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/custom_nuke.png");

    public FallingNukeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(@NotNull EntityFallingNuke entity, float yaw, float partialTicks, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();

        switch (entity.getMeta()) {
            case 2 -> { ps.mulPose(Axis.YP.rotationDegrees(90)); ps.translate(-2.0D, 0.0D, 0.0D); }
            case 4 -> { ps.mulPose(Axis.YP.rotationDegrees(180)); ps.translate(-2.0D, 0.0D, 0.0D); }
            case 3 -> { ps.mulPose(Axis.YP.rotationDegrees(270)); ps.translate(-2.0D, 0.0D, 0.0D); }
            case 5 -> ps.translate(-2.0D, 0.0D, 0.0D);
            default -> { }
        }

        float f = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        if (f < -80)
            f = 0;

        ps.mulPose(Axis.ZP.rotationDegrees(f));
        MODEL.renderAll(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light);

        ps.popPose();
        super.render(entity, yaw, partialTicks, ps, buffers, light);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityFallingNuke entity) {
        return TEXTURE;
    }
}
