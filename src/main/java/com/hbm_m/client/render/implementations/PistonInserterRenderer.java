package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.PistonInserterBlock;
import com.hbm_m.blockentity.machines.PistonInserterBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code RenderPistonInserter}: Rahmen, ausfahrender Kolben und der eingelegte Gegenstand auf der Kolbenplatte. */
public class PistonInserterRenderer implements com.hbm_m.client.render.HbmBerBounds<PistonInserterBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/piston_inserter.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/machines/piston_inserter.png");

    public PistonInserterRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(PistonInserterBlockEntity piston, float interp, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);

        switch (piston.getBlockState().getValue(PistonInserterBlock.FACING)) {
            case DOWN -> ps.mulPose(Axis.XP.rotationDegrees(180));
            case UP -> { }
            case NORTH -> { ps.mulPose(Axis.XP.rotationDegrees(-90)); ps.mulPose(Axis.YP.rotationDegrees(180)); }
            case WEST -> { ps.mulPose(Axis.ZP.rotationDegrees(90)); ps.mulPose(Axis.YP.rotationDegrees(-90)); }
            case SOUTH -> ps.mulPose(Axis.XP.rotationDegrees(90));
            case EAST -> { ps.mulPose(Axis.ZP.rotationDegrees(-90)); ps.mulPose(Axis.YP.rotationDegrees(90)); }
        }

        ps.translate(0D, -0.5, 0D);

        MODEL.renderPart("Frame", ps, buffers.getBuffer(RenderType.entityCutout(TEX)), light);

        double e = (piston.lastExtend + (piston.renderExtend - piston.lastExtend) * interp) / (double) PistonInserterBlockEntity.maxExtend;
        ps.translate(0, e * 0.9375D, 0);
        MODEL.renderPart("Piston", ps, buffers.getBuffer(RenderType.entityCutout(TEX)), light);

        if (!piston.slot.isEmpty()) {
            ItemStack stack = piston.slot.copyWithCount(1);

            if (stack.getItem() instanceof BlockItem) {
                ps.translate(0.0D, 1.125D, 0.0D);
            } else {
                ps.translate(0.0D, 1.0625D, 0.1D);
                ps.mulPose(Axis.XN.rotationDegrees(90));
            }

            // Original: EntityItem im Bilderrahmen-Modus (halbe Groesse)
            ps.scale(0.5F, 0.5F, 0.5F);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, ps, buffers, piston.getLevel(), 0);
        }

        ps.popPose();
    }
}
