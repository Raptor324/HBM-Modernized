package com.hbm_m.client.render.implementations;

import com.hbm_m.block.generic.BlockWandStructure;
import com.hbm_m.blockentity.generic.WandStructureBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

/** 1:1 {@code RenderWandStructure}: weisser Drahtrahmen des Speicherbereichs oberhalb des Speicherblocks. */
public class WandStructureRenderer implements com.hbm_m.client.render.HbmBerBounds<WandStructureBlockEntity> {

    public WandStructureRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(WandStructureBlockEntity structure, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        if (!(structure.getBlockState().getBlock() instanceof BlockWandStructure block) || block.load) return;

        AABB box = new AABB(0, 1, 0, structure.sizeX, structure.sizeY + 1, structure.sizeZ);
        LevelRenderer.renderLineBox(ps, buffers.getBuffer(RenderType.lines()), box, 1F, 1F, 1F, 1F);
    }

    @Override
    public boolean shouldRenderOffScreen(WandStructureBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
