package com.hbm_m.client.render.implementations;

import com.hbm_m.block.decorations.VendingMachineBlock;
import com.hbm_m.blockentity.decorations.VendingMachineBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code RenderVendingMachine}: Teil "Soda" bzw. "Obamna" (Snacks), nur wenn der Oberteil steht, ohne Culling. */
public class VendingMachineRenderer implements com.hbm_m.client.render.HbmBerBounds<VendingMachineBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/machines/vending_machine.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/machines/vending_machine.png");

    public VendingMachineRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(VendingMachineBlockEntity tile, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = tile.getBlockState();
        if (!(state.getBlock() instanceof VendingMachineBlock block) || tile.getLevel() == null) return;
        if (tile.getLevel().getBlockState(tile.getBlockPos().above()).isAir()) return;

        ps.pushPose();
        ps.translate(0.5D, 0.0D, 0.5D);
        switch (state.getValue(VendingMachineBlock.FACING)) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }
        MODEL.renderPart(block.snacks ? "Obamna" : "Soda", ps, buffers.getBuffer(RenderType.entityCutoutNoCull(TEX)), light);
        ps.popPose();
    }
}
