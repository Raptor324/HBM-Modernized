//? if forge || neoforge {
package com.hbm_m.client.render.item;

import com.hbm_m.client.ClientRenderHandler;
//? if forge {
import com.hbm_m.client.compat.itemtransformhelper.ItemTransformHelperCompat;
//?}
import com.hbm_m.client.model.MissileBakedModel;
import com.hbm_m.client.render.missile.MissileRenderHelper;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
//? if forge {
import net.minecraftforge.client.model.data.ModelData;
//?} else {
/*import net.neoforged.neoforge.client.model.data.ModelData;
*///?}
import org.joml.Matrix4f;

import java.util.List;
import java.util.Random;

/**
 * 1.7.10 {@code ItemRenderDetonatorLaser} port for {@link com.hbm_m.item.grenades_and_activators.RangeDetonatorItem}.
 * Позиция/поворот/масштаб — из {@code display} в {@code models/item/range_detonator.json}.
 */
public class ItemRenderDetonatorLaser extends BlockEntityWithoutLevelRenderer {

    public static final ItemRenderDetonatorLaser INSTANCE = new ItemRenderDetonatorLaser();

    /** OBJ в block-координатах (~0–2); JSON {@code display.scale} настраивает поверх. */
    private static final float BASE_MESH_SCALE = 0.125F;

    private ItemRenderDetonatorLaser() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BakedModel displayModel = MissileRenderHelper.resolveBakedModel(stack);
        //? if forge {
        MissileBakedModel model = ItemTransformHelperCompat.unwrapMissileDelegate(displayModel);
        if (model == null) {
            return;
        }
        //?}
        //? if neoforge {
        /*MissileBakedModel model = displayModel instanceof MissileBakedModel m ? m : null;
        if (model == null) {
            return;
        }
        *///?}

        if (displayContext == ItemDisplayContext.GUI) {
            Lighting.setupFor3DItems();
        }

        poseStack.pushPose();
        boolean leftHand = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        poseStack.translate(0.5F, 0.5F, 0.5F);
        
        //? if forge {
        ItemTransformHelperCompat.resolveDisplayTransforms(displayModel, model)
                .getTransform(displayContext)
                .apply(leftHand, poseStack);
        //?}
        //? if neoforge {
        /*/// applyTransform у MissileBakedModel — no-op (display применяет сам BEWLR),
        /// поэтому применяем JSON display напрямую, как в фордж-ветке через resolveDisplayTransforms.
        model.getBewlrDisplayTransforms().getTransform(displayContext)
                .apply(leftHand, poseStack);
        *///?}
        poseStack.scale(BASE_MESH_SCALE, BASE_MESH_SCALE, BASE_MESH_SCALE);

        MissileRenderHelper.bindBlockAtlas();

        boolean enableCull = poseStack.last().pose().determinant() >= 0.0F;
        if (enableCull) {
            RenderSystem.enableCull();
        } else {
            RenderSystem.disableCull();
        }

        BakedModel main = model.getPart("Main");
        drawPart(main, poseStack, buffer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F);

        RenderSystem.disableCull();
        int fullBright = LightTexture.FULL_BRIGHT;
        BakedModel lights = model.getPart("Lights");
        drawPart(lights, poseStack, buffer, fullBright, packedOverlay, 1.0F, 0.0F, 0.0F);

        renderSineWave(poseStack, buffer);
        renderDisplayDigits(poseStack, buffer, displayContext);

        RenderSystem.enableCull();
        poseStack.popPose();

        if (displayContext == ItemDisplayContext.GUI) {
            Lighting.setupForFlatItems();
        }
    }

    private static void drawPart(BakedModel part, PoseStack poseStack, MultiBufferSource buffer,
                                  int packedLight, int packedOverlay, float r, float g, float b) {
        if (part == null) {
            return;
        }
        RandomSource random = RandomSource.create(42L);
        List<BakedQuad> quads = part.getQuads(null, null, random, ModelData.EMPTY, RenderType.solid());
        if (quads.isEmpty()) {
            return;
        }
        VertexConsumer consumer = buffer.getBuffer(RenderType.solid());
        PoseStack.Pose pose = poseStack.last();
        for (BakedQuad quad : quads) {
            consumer.putBulkData(pose, quad, r, g, b, 1.0F, packedLight, packedOverlay, false);
        }
    }

    private static void renderSineWave(PoseStack poseStack, MultiBufferSource buffer) {
        poseStack.pushPose();

        float px = 0.0625F;
        poseStack.translate(0.5626F, px * 18.0F, -px * 14.0F);

        VertexConsumer consumer = buffer.getBuffer(ClientRenderHandler.CustomRenderTypes.DETONATOR_LASER_GLOW);
        int sub = 32;
        double width = px * 8.0;
        double len = width / sub;
        double time = System.currentTimeMillis() / -100.0;
        double amplitude = 0.075;
        Matrix4f matrix = poseStack.last().pose();

        for (int i = 0; i < sub; i++) {
            double h0 = Math.sin(i * 0.5 + time) * amplitude;
            double h1 = Math.sin((i + 1) * 0.5 + time) * amplitude;
            com.hbm_m.platform.RenderHooks.vertexColor(consumer, matrix, 0.0F, (float) (-px * 0.25 + h1), (float) (len * (i + 1)), 255, 255, 0, 255);
            com.hbm_m.platform.RenderHooks.vertexColor(consumer, matrix, 0.0F, (float) (px * 0.25 + h1), (float) (len * (i + 1)), 255, 255, 0, 255);
            com.hbm_m.platform.RenderHooks.vertexColor(consumer, matrix, 0.0F, (float) (px * 0.25 + h0), (float) (len * i), 255, 255, 0, 255);
            com.hbm_m.platform.RenderHooks.vertexColor(consumer, matrix, 0.0F, (float) (-px * 0.25 + h0), (float) (len * i), 255, 255, 0, 255);
        }

        poseStack.popPose();
    }

    private static void renderDisplayDigits(PoseStack poseStack, MultiBufferSource buffer,
                                            ItemDisplayContext displayContext) {
        poseStack.pushPose();

        Random rand = new Random(System.currentTimeMillis() / 500);
        Font font = Minecraft.getInstance().font;
        float f3 = 0.01F;
        poseStack.translate(0.5625F, 1.3125F, 0.875F);
        if (displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            poseStack.scale(f3, -f3, -f3);
            poseStack.translate(0.0F, 0.0F, 40.0F);
        } else {
            poseStack.scale(f3, -f3, f3);
        }
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(3.0F, -2.0F, 0.2F);

        Matrix4f matrix = poseStack.last().pose();
        for (int i = 0; i < 3; i++) {
            String s = String.valueOf(rand.nextInt(900000) + 100000);
            font.drawInBatch(s, 0.0F, 0.0F, 0xFF0000, false, matrix, buffer, Font.DisplayMode.NORMAL, 0,
                    LightTexture.FULL_BRIGHT);
            poseStack.translate(0.0F, 12.5F, 0.0F);
            matrix = poseStack.last().pose();
        }

        poseStack.popPose();
    }
}
//?}
