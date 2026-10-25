package com.hbm_m.client.render.mob;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.render.legacy.ModelHunterChopper;
import com.hbm_m.entity.mob.EntityHunterChopper;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** 1:1 {@code RenderHunterChopper}: Techne-Modell vierfach, kopfueber, Rotoren laufen nach Systemzeit. */
public class HunterChopperRenderer extends EntityRenderer<EntityHunterChopper> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/chopper.png");
    private final ModelHunterChopper model = new ModelHunterChopper();
    private static final float F = 0.1F;

    public HunterChopperRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(@NotNull EntityHunterChopper entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0, 0.0625F * 32 + 0.0625F * 12, 0);
        pose.scale(4F, 4F, 4F);
        pose.mulPose(Axis.XP.rotationDegrees(180));

        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));

        float time = (System.currentTimeMillis() % 360000L) / (1000F / 60F);
        model.part("RotorBlades").yRot = time * (F * 5);
        model.part("TorsoRotorBlades").zRot = time * (F * 5);
        model.part("TailRotorBlades").zRot = time * (F * 5);

        model.root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(entity, yaw, partialTicks, pose, buffers, light);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityHunterChopper entity) {
        return TEXTURE;
    }
}
