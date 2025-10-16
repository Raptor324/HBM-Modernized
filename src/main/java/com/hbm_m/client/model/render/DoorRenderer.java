package com.hbm_m.client.model.render;

import com.hbm_m.block.entity.DoorBlockEntity;
import com.hbm_m.client.model.DoorBakedModel;
import com.hbm_m.block.entity.DoorDecl;
import com.hbm_m.util.LegacyAnimator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;

public class DoorRenderer implements BlockEntityRenderer<DoorBlockEntity> {
    
    private final float[] translation = new float[3];
    private final float[] origin = new float[3];
    private final float[] rotation = new float[3];
    
    public DoorRenderer(BlockEntityRendererProvider.Context context) {
    }
    
    @Override
    public void render(DoorBlockEntity be, float partialTick, PoseStack poseStack,
                    MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!be.isController()) return;
        
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        BakedModel originalModel = blockRenderer.getBlockModel(be.getBlockState());
        if (!(originalModel instanceof DoorBakedModel model)) return;
        
        DoorDecl doorDecl = be.getDoorDecl();
        LegacyAnimator animator = new LegacyAnimator(poseStack, bufferSource,
                blockRenderer, packedLight, packedOverlay);
        
        float openTicks = be.getOpenProgress(partialTick) * doorDecl.getOpenTime();
        
        animator.push();
        Direction facing = be.getFacing();
        animator.setupBlockTransform(facing);
        doorDecl.doOffsetTransform(animator);
        
        String[] partNames = model.getPartNames();
        for (String partName : partNames) {
            if (!doorDecl.doesRender(partName, false)) continue;
            
            animator.push();
            doPartTransform(animator, doorDecl, partName, openTicks);
            BakedModel partModel = model.getPart(partName);
            animator.renderPart(partModel);
            animator.pop();
        }
        
        animator.pop();
    }
    
    private void doPartTransform(LegacyAnimator animator, DoorDecl doorDecl,
                                 String partName, float openTicks) {
        doorDecl.getTranslation(partName, openTicks, false, translation);
        doorDecl.getOrigin(partName, origin);
        doorDecl.getRotation(partName, openTicks, rotation);
        
        animator.translate(origin[0], origin[1], origin[2]);
        
        if (rotation[0] != 0) animator.rotate(rotation[0], 1, 0, 0);
        if (rotation[1] != 0) animator.rotate(rotation[1], 0, 1, 0);
        if (rotation[2] != 0) animator.rotate(rotation[2], 0, 0, 1);
        
        animator.translate(
                -origin[0] + translation[0],
                -origin[1] + translation[1],
                -origin[2] + translation[2]
        );
    }
    
    @Override
    public int getViewDistance() {
        return 256;
    }
}
