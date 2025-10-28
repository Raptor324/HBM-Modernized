package com.hbm_m.client.render;

import com.hbm_m.block.entity.DoorBlockEntity;
import com.hbm_m.client.model.DoorBakedModel;
import com.hbm_m.block.entity.DoorDecl;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DoorRenderer extends AbstractPartBasedRenderer<DoorBlockEntity, DoorBakedModel> {
    
    // ОПТИМИЗАЦИЯ: Переиспользуемые буферы для трансформаций
    private final float[] translation = new float[3];
    private final float[] origin = new float[3];
    private final float[] rotation = new float[3];
    
    public DoorRenderer(BlockEntityRendererProvider.Context context) {}
    
    @Override
    protected DoorBakedModel getModelType(BakedModel rawModel) {
        return rawModel instanceof DoorBakedModel model ? model : null;
    }
    
    @Override
    protected Direction getFacing(DoorBlockEntity blockEntity) {
        return blockEntity.getFacing();
    }
    
    @Override
    protected void renderParts(DoorBlockEntity be, DoorBakedModel model, LegacyAnimator animator,
                              float partialTick, int packedLight, int packedOverlay,
                              PoseStack poseStack, MultiBufferSource bufferSource) {
        
        if (!be.isController()) return;
        
        DoorDecl doorDecl = be.getDoorDecl();
        if (doorDecl == null) return; // Null-check для безопасности
        
        float openTicks = be.getOpenProgress(partialTick) * doorDecl.getOpenTime();
        doorDecl.doOffsetTransform(animator);
        
        // ОПТИМИЗАЦИЯ: Используем кэшированный массив из DoorBakedModel
        String[] partNames = model.getPartNames();
        
        for (String partName : partNames) {
            if (!doorDecl.doesRender(partName, false)) continue;
            
            animator.push();
            doPartTransform(animator, doorDecl, partName, openTicks);
            
            // Используем устаревший метод renderPart для совместимости
            // TODO: Оптимизировать используя GlobalMeshCache.getOrCompile()
            animator.renderPart(model.getPart(partName));
            
            animator.pop();
        }
    }
    
    private void doPartTransform(LegacyAnimator animator, DoorDecl doorDecl,
                                String partName, float openTicks) {
        // ВАЖНО: DoorDecl методы НЕ создают новые массивы, а заполняют переданные
        // (проверено в DoorDecl.java) - это уже оптимально!
        doorDecl.getTranslation(partName, openTicks, false, translation);
        doorDecl.getOrigin(partName, origin);
        doorDecl.getRotation(partName, openTicks, rotation);
        
        animator.translate(origin[0], origin[1], origin[2]);
        
        // Условные проверки оптимальны для CPU branch prediction
        if (rotation[0] != 0) animator.rotate(rotation[0], 1, 0, 0);
        if (rotation[1] != 0) animator.rotate(rotation[1], 0, 1, 0);
        if (rotation[2] != 0) animator.rotate(rotation[2], 0, 0, 1);
        
        animator.translate(-origin[0] + translation[0], -origin[1] + translation[1],
                          -origin[2] + translation[2]);
    }
    
    @Override
    public int getViewDistance() {
        return 128;
    }
}
