//? if forge {
package com.hbm_m.client.render.item;

import com.hbm_m.client.render.missile.MissileRenderData;
import com.hbm_m.client.render.missile.MissileRenderRegistry;
import com.hbm_m.item.missile.MissileItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class MissileItemRenderer extends BlockEntityWithoutLevelRenderer {

    public static final MissileItemRenderer INSTANCE = new MissileItemRenderer();

    private MissileItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!(stack.getItem() instanceof MissileItem missile)) {
            return;
        }

        MissileRenderData renderer = MissileRenderRegistry.get(stack);
        if (renderer == null) {
            return;
        }

        float guiScale = 1.0F;
        float guiOffset = 0.0F;
        switch (missile.tier) {
            case TIER0 -> {
                guiScale = 5.0F;
                guiOffset = 13.5F;
            }
            case TIER1 -> {
                guiScale = 3.75F;
                guiOffset = 13.0F;
            }
            case TIER2 -> {
                guiScale = 2.75F;
                guiOffset = 12.0F;
            }
            case TIER3 -> {
                guiScale = 1.85F;
                guiOffset = 10.0F;
            }
            case TIER4 -> {
                guiScale = 1.8F;
                guiOffset = 9.0F;
            }
        }
        if (missile.formFactor == MissileItem.MissileFormFactor.STEALTH) {
            guiScale = 2.4F;
            guiOffset = 11.0F;
        } else if (missile.formFactor == MissileItem.MissileFormFactor.ABM) {
            guiScale = 2.25F;
            guiOffset = 7.0F;
        }

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);

        switch (displayContext) {
            case FIRST_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.3F, 0.41F, 0.2F);
                poseStack.scale(0.35F, 0.35F, 0.35F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-25.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                float s = 0.3F;
                poseStack.translate(0.5F, 0.25F, 0.0F);
                poseStack.scale(s, s, s);
            }
            case FIRST_PERSON_LEFT_HAND -> {
                poseStack.translate(-0.3F, 0.41F, 0.2F);
                poseStack.scale(0.35F, 0.35F, 0.35F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-25.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                float s = 0.3F;
                poseStack.translate(0.5F, 0.25F, 0.0F);
                poseStack.scale(s, s, s);
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            }
            case THIRD_PERSON_RIGHT_HAND, HEAD -> {
                poseStack.translate(0.0F, 0.55F, -0.18F);
                poseStack.scale(0.35F, 0.35F, 0.35F);
                float s = 0.15F;
                poseStack.translate(0.0F, -0.5F, 0.5F);
                poseStack.scale(s, s, s);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            case THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.0F, 0.55F, -0.18F);
                poseStack.scale(0.35F, 0.35F, 0.35F);
                float s = 0.15F;
                poseStack.translate(0.0F, -0.5F, 0.5F);
                poseStack.scale(s, s, s);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            }
            case GROUND -> {
                poseStack.translate(0.0F, 0.3F, 0.0F);
                poseStack.scale(0.35F, 0.35F, 0.35F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                float s = 0.15F;
                poseStack.scale(s, s, s);
            }
            case FIXED -> {
                poseStack.translate(0.0F, 0.3F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            case GUI -> {
                poseStack.scale(0.045F, 0.045F, 0.045F);
                poseStack.translate(0.0F, 11.6F, -11.6F);
                poseStack.scale(guiScale, guiScale, guiScale);
                poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees((System.currentTimeMillis() / 15L) % 360));
                poseStack.translate(0.0F, -16.0F + guiOffset, 0.0F);
            }
            default -> { }
        }

        RenderSystem.disableCull();
        renderer.render(poseStack, packedLight, BlockPos.ZERO);
        RenderSystem.enableCull();

        poseStack.popPose();
    }
}
//?}
