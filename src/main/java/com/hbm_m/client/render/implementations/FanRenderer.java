package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachineFanBlock;
import com.hbm_m.blockentity.machines.MachineFanBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderFan}: Rahmen nach der Ausrichtung gedreht, Fluegel um die eigene Achse. */
public class FanRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineFanBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/fan.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/fan.png");

    public FanRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineFanBlockEntity fan, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0.5D, 0.5D);

        switch (fan.getBlockState().getValue(MachineFanBlock.FACING)) {
            case DOWN -> ps.mulPose(Axis.XP.rotationDegrees(180));
            case NORTH -> ps.mulPose(Axis.XP.rotationDegrees(-90));
            case WEST -> ps.mulPose(Axis.ZP.rotationDegrees(90));
            case SOUTH -> ps.mulPose(Axis.XP.rotationDegrees(90));
            case EAST -> ps.mulPose(Axis.ZP.rotationDegrees(-90));
            default -> { }
        }

        ps.translate(0D, -0.5D, 0D);

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        MODEL.renderPart("Frame", ps, vc, light);

        float rot = fan.prevSpin + (fan.spin - fan.prevSpin) * interp;
        ps.mulPose(Axis.YP.rotationDegrees(-rot));
        MODEL.renderPart("Blades", ps, vc, light);

        ps.popPose();
    }

    @Override public int getViewDistance() { return 256; }
}
