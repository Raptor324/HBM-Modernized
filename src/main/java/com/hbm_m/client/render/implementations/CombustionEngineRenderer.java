package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachineCombustionEngineBlock;
import com.hbm_m.blockentity.machines.MachineCombustionEngineBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.inventory.fluid.FluidContainerDefs;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderCombustionEngine}: Motor, Kanister in der Farbe des Treibstoffs, aufschwenkende Wartungsklappe. */
public class CombustionEngineRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineCombustionEngineBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/combustion_engine.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/combustion_engine.png");

    public CombustionEngineRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineCombustionEngineBlockEntity engine, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 3/5/2/4 -> 270/0/90/180 Grad; Port-FACING entspricht dieser Richtung
        switch (engine.getBlockState().getValue(MachineCombustionEngineBlock.FACING)) {
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            default -> { }
        }

        ps.translate(-0.5, 0, 3);

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        MODEL.renderPart("Engine", ps, vc, light);

        FluidContainerDefs.CDCanister canister = FluidContainerDefs.getCanister(engine.tank.getTankType());
        if (canister != null) {
            int color = canister.color();
            float r = ((color & 0xff0000) >> 16) / 256F;
            float g = ((color & 0x00ff00) >> 8) / 256F;
            float b = ((color & 0x0000ff) >> 0) / 256F;
            MODEL.renderPartTinted("Canister", ps, vc, light, r, g, b);
        } else {
            MODEL.renderPart("Canister", ps, vc, light);
        }

        ps.translate(1, 0, -2.6875);
        ps.mulPose(Axis.YN.rotationDegrees(engine.prevDoorAngle + (engine.doorAngle - engine.prevDoorAngle) * interp));
        ps.translate(-1, 0, 2.6875);
        MODEL.renderPart("Hatch", ps, vc, light);

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineCombustionEngineBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
