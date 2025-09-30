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
            System.err.println("ОШИБКА РЕНДЕРА: Модель блока НЕ является MachineAdvancedAssemblerBakedModel! Тип модели: " + originalModel.getClass().getName());
            return;
        }
        
        // Диагностика, если нужна
        if (pBlockEntity.isCrafting()) {
            if(pBlockEntity.getLevel().getGameTime() % 20 == 0) {
                System.out.println("[РЕНДЕРЕР] Рендеринг в состоянии крафта. Угол кольца: " + pBlockEntity.ringAngle);
            }
        }

        pPoseStack.pushPose();

        // --- ЭТАП 1: УСТАНОВКА ОБЩЕЙ ОРИЕНТАЦИИ БЛОКА ---
        // 1. Перемещаемся в ЦЕНТР блока, чтобы он стал точкой вращения.
        pPoseStack.translate(0.5, 0.0, 0.5);

        // 2. Поворачиваем всю систему координат в соответствии с направлением блока.
        Direction facing = pBlockEntity.getBlockState().getValue(MachineAdvancedAssemblerBlock.FACING);
        float yRot = -facing.toYRot(); 
        pPoseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        
        // 3. Возвращаемся обратно. Теперь начало координат (0,0,0) снова в углу блока,
        // но вся система координат повернута. Это ключевой шаг.
        pPoseStack.translate(-0.5, 0.0, -0.5);
        
        // --- ЭТАП 2: РЕНДЕР СТАТИЧНЫХ ЧАСТЕЙ ---
        // Рендерим базу. Она будет нарисована в уже повернутой системе координат и встанет на свое место.
        renderPart(model, "Base", pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, blockRenderer);
        
        // --- ЭТАП 3: РЕНДЕР АНИМИРОВАННЫХ ЧАСТЕЙ ---
        pPoseStack.pushPose(); // Изолируем трансформации анимации

        // Чтобы вращать анимированные части вокруг центра, нам снова нужно:
        // 1. Переместиться в центр (в уже повернутой системе).
        pPoseStack.translate(0.5, 0.0, 0.5);
        
        // 2. Применить вращение анимации (кольца).
        float ringSpin = Mth.lerp(pPartialTick, pBlockEntity.prevRingAngle, pBlockEntity.ringAngle);
        pPoseStack.mulPose(Axis.YP.rotationDegrees(ringSpin));
        
        // 3. Вернуться из центра.
        pPoseStack.translate(-0.5, 0.0, -0.5);

        // Теперь рендерим анимированные части. Они унаследовали основной поворот блока
        // и получили дополнительное вращение от анимации, вращаясь при этом вокруг центра.
        renderPart(model, "Ring", pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, blockRenderer);
        renderArm1(pBlockEntity.arms[0], pPartialTick, model, pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, blockRenderer);
        renderArm2(pBlockEntity.arms[1], pPartialTick, model, pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, blockRenderer);
        
        pPoseStack.popPose(); // Завершаем изоляцию трансформаций анимации
        // --- Конец анимации ---

        pPoseStack.popPose(); // Завершаем основную трансформацию
    }

    private void renderPart(MachineAdvancedAssemblerBakedModel model, String name, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay, BlockRenderDispatcher blockRenderer) {
        BakedModel part = model.getPart(name);
        if (part != null) {
            RenderType rt = RenderType.cutout();
            
            blockRenderer.getModelRenderer().renderModel(
                poseStack.last(),
                bufferSource.getBuffer(rt),
                null, part, 1.0f, 1.0f, 1.0f, light, overlay, ModelData.EMPTY, null
            );
        }
    }

    private void renderArm1(MachineAdvancedAssemblerBlockEntity.AssemblerArm arm, float pPartialTick, MachineAdvancedAssemblerBakedModel model, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay, BlockRenderDispatcher blockRenderer) {
        pPoseStack.pushPose();
        float[] angles = getInterpolatedAngles(arm, pPartialTick);

        pPoseStack.translate(0, 1.625, 0.9375);
        pPoseStack.mulPose(Axis.XP.rotationDegrees(angles[0]));
        pPoseStack.translate(0, -1.625, -0.9375);
        renderPart(model, "ArmLower1", pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, blockRenderer);

        pPoseStack.translate(0, 2.375, 0.9375);
        pPoseStack.mulPose(Axis.XP.rotationDegrees(angles[1]));
        pPoseStack.translate(0, -2.375, -0.9375);
        renderPart(model, "ArmUpper1", pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, blockRenderer);
        
        pPoseStack.translate(0, 2.375, 0.4375);
        pPoseStack.mulPose(Axis.XP.rotationDegrees(angles[2]));
        pPoseStack.translate(0, -2.375, -0.4375);
        renderPart(model, "Head1", pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, blockRenderer);

        pPoseStack.translate(0, angles[3], 0);
        renderPart(model, "Spike1", pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, blockRenderer);

        pPoseStack.popPose();
    }

    private void renderArm2(MachineAdvancedAssemblerBlockEntity.AssemblerArm arm, float pPartialTick, MachineAdvancedAssemblerBakedModel model, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay, BlockRenderDispatcher blockRenderer) {
        pPoseStack.pushPose();
        float[] angles = getInterpolatedAngles(arm, pPartialTick);

        pPoseStack.translate(0, 1.625, -0.9375);
        pPoseStack.mulPose(Axis.XP.rotationDegrees(-angles[0]));
        pPoseStack.translate(0, -1.625, 0.9375);
        renderPart(model, "ArmLower2", pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, blockRenderer);

        pPoseStack.translate(0, 2.375, -0.9375);
        pPoseStack.mulPose(Axis.XP.rotationDegrees(-angles[1]));
        pPoseStack.translate(0, -2.375, 0.9375);
        renderPart(model, "ArmUpper2", pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, blockRenderer);

        pPoseStack.translate(0, 2.375, -0.4375);
        pPoseStack.mulPose(Axis.XP.rotationDegrees(-angles[2]));
        pPoseStack.translate(0, -2.375, 0.4375);
        renderPart(model, "Head2", pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, blockRenderer);

        pPoseStack.translate(0, angles[3], 0);
        renderPart(model, "Spike2", pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, blockRenderer);

        pPoseStack.popPose();
    }
    
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

    private float[] getInterpolatedAngles(MachineAdvancedAssemblerBlockEntity.AssemblerArm arm, float pPartialTick) {
        float[] angles = new float[4];
        for (int i = 0; i < 4; i++) {
            angles[i] = Mth.lerp(pPartialTick, arm.prevAngles[i], arm.angles[i]);
        }
        return angles;
    }
}