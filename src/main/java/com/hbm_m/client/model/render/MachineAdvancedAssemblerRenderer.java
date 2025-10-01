package com.hbm_m.client.model.render;

// Рендер для продвинутой сборочной машины.
// Отвечает за анимацию вращающегося кольца и двух манипуляторов.
// Использует модель AdvancedAssemblyMachineBakedModel, которая разбивает модель на части.
// TODO: Сейчас не работает
import com.hbm_m.client.model.MachineAdvancedAssemblerBakedModel;
import com.hbm_m.block.MachineAdvancedAssemblerBlock;
import com.hbm_m.block.entity.MachineAdvancedAssemblerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraft.client.renderer.entity.ItemRenderer;

public class MachineAdvancedAssemblerRenderer implements BlockEntityRenderer<MachineAdvancedAssemblerBlockEntity> {

    public MachineAdvancedAssemblerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MachineAdvancedAssemblerBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack,
                    MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay) {

        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        BakedModel originalModel = blockRenderer.getBlockModel(pBlockEntity.getBlockState());

        if (!(originalModel instanceof MachineAdvancedAssemblerBakedModel model)) {
            return;
        }

        // --- ЭТАП 1: Создаём наш LegacyAnimator ---
        LegacyAnimator animator = new LegacyAnimator(pPoseStack, pBufferSource, blockRenderer, pPackedLight, pPackedOverlay);

        animator.push(); // Начало всех трансформаций

        // --- ЭТАП 2: Общая ориентация блока (одной строкой!) ---
        Direction facing = pBlockEntity.getBlockState().getValue(MachineAdvancedAssemblerBlock.FACING);
        animator.setupBlockTransform(facing);

        // --- ЭТАП 3: Рендер частей ---
        animator.renderPart(model.getPart("Base"));

        if (pBlockEntity.frame) {
            animator.renderPart(model.getPart("Frame"));
        }

        animator.push(); // Изолируем вращающиеся части

        float ringSpin = Mth.lerp(pPartialTick, pBlockEntity.prevRingAngle, pBlockEntity.ringAngle);
        float[] arm1Angles = getInterpolatedAngles(pBlockEntity.arms[0], pPartialTick);
        float[] arm2Angles = getInterpolatedAngles(pBlockEntity.arms[1], pPartialTick);

        animator.rotate(ringSpin, 0, 1, 0);
        animator.renderPart(model.getPart("Ring"));

        // === МАНИПУЛЯТОР 1 (КОД ПРАКТИЧЕСКИ 1-в-1 ИЗ 1.7.10) ===
        animator.push();
        {
            animator.translate(0, 1.625, 0.9375);
            animator.rotate(arm1Angles[0], 1, 0, 0);
            animator.translate(0, -1.625, -0.9375); // <--- ВОТ ОНО! Мы можем делать "обратный" translate!
            animator.renderPart(model.getPart("ArmLower1"));

            animator.translate(0, 2.375, 0.9375);
            animator.rotate(arm1Angles[1], 1, 0, 0);
            animator.translate(0, -2.375, -0.9375);
            animator.renderPart(model.getPart("ArmUpper1"));

            animator.translate(0, 2.375, 0.4375);
            animator.rotate(arm1Angles[2], 1, 0, 0);
            animator.translate(0, -2.375, -0.4375);
            animator.renderPart(model.getPart("Head1"));

            animator.translate(0, arm1Angles[3], 0);
            animator.renderPart(model.getPart("Spike1"));
        }
        animator.pop();

        // === МАНИПУЛЯТОР 2 (КОД ПРАКТИЧЕСКИ 1-в-1 ИЗ 1.7.10) ===
        animator.push();
        {
            animator.translate(0, 1.625, -0.9375);
            animator.rotate(-arm2Angles[0], 1, 0, 0);
            animator.translate(0, -1.625, 0.9375);
            animator.renderPart(model.getPart("ArmLower2"));

            animator.translate(0, 2.375, -0.9375);
            animator.rotate(-arm2Angles[1], 1, 0, 0);
            animator.translate(0, -2.375, 0.9375);
            animator.renderPart(model.getPart("ArmUpper2"));

            animator.translate(0, 2.375, -0.4375);
            animator.rotate(-arm2Angles[2], 1, 0, 0);
            animator.translate(0, -2.375, 0.4375);
            animator.renderPart(model.getPart("Head2"));
            
            animator.translate(0, arm2Angles[3], 0);
            animator.renderPart(model.getPart("Spike2"));
        }
        animator.pop();

        animator.pop(); // Конец вращающихся частей
        animator.pop(); // Конец всех трансформаций
    }
    
    /*
    private void renderRecipeIcon(MachineAdvancedAssemblerBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay) {
        ItemStack recipeIcon = ItemStack.EMPTY; // = pBlockEntity.getRecipeIcon();
        if (!recipeIcon.isEmpty()) {
            pPoseStack.pushPose();
            pPoseStack.mulPose(Axis.YP.rotationDegrees(90));
            pPoseStack.translate(0, 1.0625, 0);
            pPoseStack.scale(1.25F, 1.25F, 1.25F);
            ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
            itemRenderer.renderStatic(recipeIcon, ItemDisplayContext.FIXED, pPackedLight, pPackedOverlay, pPoseStack, pBufferSource, pBlockEntity.getLevel(), 0);
            pPoseStack.popPose();
        }
    }
    */

    private float[] getInterpolatedAngles(MachineAdvancedAssemblerBlockEntity.AssemblerArm arm, float pPartialTick) {
        float[] angles = new float[4];
        for (int i = 0; i < 4; i++) {
            angles[i] = Mth.lerp(pPartialTick, arm.prevAngles[i], arm.angles[i]);
        }
        return angles;
    }
}