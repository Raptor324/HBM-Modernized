package com.hbm_m.client.model.render;

// Рендер для продвинутой сборочной машины.
// Отвечает за анимацию вращающегося кольца и двух манипуляторов.
// Использует модель AdvancedAssemblyMachineBakedModel, которая разбивает модель на части.
import com.hbm_m.client.model.MachineAdvancedAssemblerBakedModel;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.util.LegacyAnimator;
import com.hbm_m.block.MachineAdvancedAssemblerBlock;
import com.hbm_m.block.entity.MachineAdvancedAssemblerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

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
            animator.translate(0, -1.625, -0.9375);
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

        renderRecipeIcon(pBlockEntity, pPartialTick, pPoseStack, pBufferSource, pPackedLight, pPackedOverlay);
    }
    
    /**
     * Рендерит иконку выходного предмета текущего рецепта на основании машины.
     * Адаптировано из оригинальной версии 1.7.10.
     */
    private void renderRecipeIcon(MachineAdvancedAssemblerBlockEntity pBlockEntity, float pPartialTick, 
        PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay) {
        // Проверяем, есть ли выбранный рецепт
        ResourceLocation selectedRecipeId = pBlockEntity.getSelectedRecipeId();
        if (selectedRecipeId == null) return;
        
        // Проверяем дистанцию до игрока (оптимизация из оригинала)
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        double distSq = mc.player.distanceToSqr(
            pBlockEntity.getBlockPos().getX() + 0.5, 
            pBlockEntity.getBlockPos().getY() + 1.0, 
            pBlockEntity.getBlockPos().getZ() + 0.5
        );
        
        if (distSq > 35 * 35) return; // Не рендерим если игрок далеко
        
        // Получаем рецепт и его иконку
        if (pBlockEntity.getLevel() == null) return;
        
        pBlockEntity.getLevel().getRecipeManager().byKey(selectedRecipeId).ifPresent(recipe -> {
            if (!(recipe instanceof AssemblerRecipe assemblerRecipe)) return;
            
            ItemStack iconStack = assemblerRecipe.getResultItem(null);
            if (iconStack.isEmpty()) return;
            
            pPoseStack.pushPose();
            
            // Базовая трансформация (как в оригинале)
            pPoseStack.mulPose(Axis.YP.rotationDegrees(90));
            pPoseStack.translate(0, 1.0625, 0);
            
            // Различная обработка для блоков и предметов (как в оригинале)
            if (iconStack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                BakedModel blockModel = mc.getBlockRenderer().getBlockModel(block.defaultBlockState());
                
                // Проверяем, должен ли блок рендериться в 3D
                if (blockModel.isGui3d()) {
                    // 3D блоки (например, сундуки) - небольшой сдвиг вниз
                    pPoseStack.translate(0, -0.0625, 0);
                } else {
                    // Плоские блоки (например, факелы) - больше вниз и меньше масштаб
                    pPoseStack.translate(0, -0.125, 0);
                    pPoseStack.scale(0.5F, 0.5F, 0.5F);
                }
            } else {
                // Обычные предметы - поворот и смещение
                pPoseStack.mulPose(Axis.XP.rotationDegrees(-90));
                pPoseStack.translate(-1, -0.25, 0);
            }
            
            // Общее масштабирование (как в оригинале)
            pPoseStack.scale(1.0F, 1.0F, 1.0F);
            
            // Рендерим предмет
            ItemRenderer itemRenderer = mc.getItemRenderer();
            itemRenderer.renderStatic(
                iconStack, 
                ItemDisplayContext.FIXED, 
                pPackedLight, 
                pPackedOverlay, 
                pPoseStack, 
                pBufferSource, 
                pBlockEntity.getLevel(), 
                0
            );
            
            pPoseStack.popPose();
        });
    }

    private float[] getInterpolatedAngles(MachineAdvancedAssemblerBlockEntity.AssemblerArm arm, float pPartialTick) {
        float[] angles = new float[4];
        for (int i = 0; i < 4; i++) {
            angles[i] = Mth.lerp(pPartialTick, arm.prevAngles[i], arm.angles[i]);
        }
        return angles;
    }
}
