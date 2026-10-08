package com.hbm_m.client.render.projectile;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.render.legacy.ModelChopperMine;
import com.hbm_m.entity.projectile.EntityChopperMine;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderChopperMine}: 8er-Wuerfel, 1,5-fach, kopfueber. */
public class ChopperMineRenderer extends EntityRenderer<EntityChopperMine> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/chopper_bomb.png");
    private final ModelChopperMine model = new ModelChopperMine();

    public ChopperMineRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(@NotNull EntityChopperMine entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.scale(1.5F, 1.5F, 1.5F);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        model.root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(entity, yaw, partialTicks, pose, buffers, light);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityChopperMine entity) {
        return TEXTURE;
    }
}
