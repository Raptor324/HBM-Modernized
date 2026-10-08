package com.hbm_m.client.render.implementations;

import com.hbm_m.block.generic.LogicBlockInvis;
import com.hbm_m.blockentity.generic.LogicBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gegenstueck zu {@code LogicBlock.getIcon(IBlockAccess, ...)}: ist eine Tarnung gesetzt, wird deren Modell gezeichnet,
 * sonst das eigene Modell des Logikblocks. Der unsichtbare Logikblock zeichnet nichts.
 */
public class LogicBlockRenderer implements BlockEntityRenderer<LogicBlockEntity> {

    public LogicBlockRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(LogicBlockEntity logic, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        BlockState own = logic.getBlockState();
        if (own.getBlock() instanceof LogicBlockInvis) return;

        BlockState state = logic.disguise != null ? logic.disguise : own;
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();

        if (logic.getLevel() != null) light = LevelRenderer.getLightColor(logic.getLevel(), logic.getBlockPos());

        dispatcher.getModelRenderer().renderModel(ps.last(), buffers.getBuffer(ItemBlockRenderTypes.getRenderType(state, false)), state,
                dispatcher.getBlockModel(state), 1F, 1F, 1F, light, overlay);
    }
}
