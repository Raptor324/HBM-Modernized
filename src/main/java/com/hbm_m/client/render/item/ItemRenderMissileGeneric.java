//? if forge {
package com.hbm_m.client.render.item;

import com.hbm_m.client.render.missile.MissileRenderHelper;
import com.hbm_m.client.render.missile.MissileRenderRegistry;
import com.hbm_m.client.render.missile.MissileRenderData;
import com.hbm_m.item.missile.MissileItem;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Missile item BEWLR — transforms from NEO {@code ItemRenderMissileGeneric} (1.20.1 OBJ/VBO port),
 * with {@link RenderMissileType} per item matching 1.7.10 {@code ClientProxy} registration.
 * JSON {@code display} is omitted in datagen; {@link com.hbm_m.client.model.MissileBakedModel#applyTransform} is a no-op.
 */
public class ItemRenderMissileGeneric extends BlockEntityWithoutLevelRenderer {

    public static final ItemRenderMissileGeneric INSTANCE = new ItemRenderMissileGeneric();

    /** 1.7.10 {@code ItemRenderMissileGeneric.RenderMissileType}. */
    public enum RenderMissileType {
        TYPE_TIER0,
        TYPE_TIER1,
        TYPE_TIER2,
        TYPE_TIER3,
        TYPE_STEALTH,
        TYPE_ABM,
        TYPE_NUCLEAR,
        TYPE_ROBIN
    }

    private static final float GUI_BLOCK_SCALE = 0.045F;
    private static final float GUI_BLOCK_PIVOT_Y = 11.6F;
    private static final float GUI_BLOCK_PIVOT_Z = -11.6F;

    private ItemRenderMissileGeneric() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    private static GuiLayout guiLayout(RenderMissileType type) {
        float guiScale = 1.0F;
        float guiOffset = 0.0F;
        switch (type) {
            case TYPE_TIER0 -> {
                guiScale = 5.0F;
                guiOffset = 13.5F;
            }
            case TYPE_TIER1 -> {
                guiScale = 3.75F;
                guiOffset = 13.0F;
            }
            case TYPE_TIER2 -> {
                guiScale = 2.75F;
                guiOffset = 12.0F;
            }
            case TYPE_TIER3 -> {
                guiScale = 1.85F;
                guiOffset = 10.0F;
            }
            case TYPE_STEALTH -> {
                guiScale = 2.4F;
                guiOffset = 11.0F;
            }
            case TYPE_ABM -> {
                guiScale = 3.375F;
                guiOffset = 11.5F;
            }
            case TYPE_NUCLEAR -> {
                guiScale = 1.8F;
                guiOffset = 9.0F;
            }
            case TYPE_ROBIN -> {
                guiScale = 1.6F;
                guiOffset = 11.0F;
            }
        }
        return new GuiLayout(guiScale, guiOffset);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!(stack.getItem() instanceof MissileItem)) {
            return;
        }

        MissileRenderData renderData = MissileRenderRegistry.get(stack);
        if (renderData == null) {
            return;
        }

        RenderMissileType type = renderData.renderType();
        float meshScale = renderData.scale();
        GuiLayout gui = guiLayout(type);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);

        switch (displayContext) {
            case FIRST_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.3F, 0.41F, 0.2F);
                poseStack.scale(0.35F, 0.35F, 0.35F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-25.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.translate(0.5F, 0.25F, 0.0F);
                poseStack.scale(0.3F, 0.3F, 0.3F);
            }
            case FIRST_PERSON_LEFT_HAND -> {
                poseStack.translate(-0.3F, 0.41F, 0.2F);
                poseStack.scale(0.35F, 0.35F, 0.35F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-25.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.translate(0.5F, 0.25F, 0.0F);
                poseStack.scale(0.3F, 0.3F, 0.3F);
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            }
            case THIRD_PERSON_RIGHT_HAND, HEAD -> {
                poseStack.translate(0.0F, 0.55F, -0.18F);
                poseStack.scale(0.35F, 0.35F, 0.35F);
                poseStack.translate(0.0F, -0.5F, 0.5F);
                poseStack.scale(0.15F, 0.15F, 0.15F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            case THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.0F, 0.55F, -0.18F);
                poseStack.scale(0.35F, 0.35F, 0.35F);
                poseStack.translate(0.0F, -0.5F, 0.5F);
                poseStack.scale(0.15F, 0.15F, 0.15F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            }
            case GROUND -> {
                poseStack.translate(0.0F, 0.3F, 0.0F);
                poseStack.scale(0.35F, 0.35F, 0.35F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.scale(0.15F, 0.15F, 0.15F);
            }
            case FIXED -> {
                poseStack.translate(0.0F, 0.3F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            case GUI -> {
                Lighting.setupFor3DItems();
                poseStack.scale(GUI_BLOCK_SCALE, GUI_BLOCK_SCALE, GUI_BLOCK_SCALE);
                poseStack.translate(0.0F, GUI_BLOCK_PIVOT_Y, GUI_BLOCK_PIVOT_Z);
                poseStack.scale(gui.scale(), gui.scale(), gui.scale());
                poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees((System.currentTimeMillis() / 15L) % 360));
                poseStack.translate(0.0F, -16.0F + gui.offset(), 0.0F);
            }
            default -> { }
        }

        poseStack.scale(meshScale, meshScale, meshScale);

        RenderSystem.disableCull();
        MissileRenderHelper.drawBakedQuads(poseStack, buffer, packedLight, stack);
        RenderSystem.enableCull();

        if (displayContext == ItemDisplayContext.GUI) {
            Lighting.setupForFlatItems();
        }
        poseStack.popPose();
    }

    private record GuiLayout(float scale, float offset) {
    }
}
//?}
