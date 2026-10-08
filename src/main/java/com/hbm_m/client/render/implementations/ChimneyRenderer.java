package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachineChimneyBlock;
import com.hbm_m.blockentity.machines.MachineChimneyBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderChimneyBrick}/{@code RenderChimneyIndustrial}: das statische Schornsteinmodell, um 180 Grad gedreht. */
public class ChimneyRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineChimneyBlockEntity> {

    public static final SimpleObjModel BRICK = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/chimney_brick.obj"));
    public static final ResourceLocation BRICK_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/chimney_brick.png");
    public static final SimpleObjModel INDUSTRIAL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/chimney_industrial.obj"));
    public static final ResourceLocation INDUSTRIAL_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/chimney_industrial.png");

    public ChimneyRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineChimneyBlockEntity te, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);
        ps.mulPose(Axis.YP.rotationDegrees(180));

        boolean industrial = te.getBlockState().getBlock() instanceof MachineChimneyBlock c && c.isIndustrial();
        if (industrial) INDUSTRIAL.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(INDUSTRIAL_TEX)), light);
        else BRICK.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(BRICK_TEX)), light);

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineChimneyBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
