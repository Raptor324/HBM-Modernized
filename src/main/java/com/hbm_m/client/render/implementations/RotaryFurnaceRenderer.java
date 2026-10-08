package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineRotaryFurnaceBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderRotaryFurnace}: Ofenkoerper plus Kolben, der waehrend des Betriebs mit
 * {@code sps(anim * 0.75 * 0.125) * 0.5 - 0.5} auf und ab faehrt.
 */
public class RotaryFurnaceRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineRotaryFurnaceBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/rotary_furnace.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/rotary_furnace.png");

    public RotaryFurnaceRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineRotaryFurnaceBlockEntity furnace, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        switch (furnace.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        MODEL.renderPart("Furnace", ps, vc, light);
        ps.pushPose();

        float anim = furnace.lastAnim + (furnace.anim - furnace.lastAnim) * f;

        ps.translate(0, sps((anim * 0.75) * 0.125) * 0.5 - 0.5, 0);
        MODEL.renderPart("Piston", ps, vc, light);
        ps.popPose();

        ps.popPose();
    }

    /** {@code BobMathUtil.sps}. */
    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    @Override public boolean shouldRenderOffScreen(MachineRotaryFurnaceBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
