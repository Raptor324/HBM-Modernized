package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.RefuelerBlock;
import com.hbm_m.blockentity.machines.RefuelerBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderRefueler}: Gehaeuse plus additiver Fluessigkeitsstand, unterhalb Y 0.125 abgeschnitten. */
public class RefuelerRenderer implements BlockEntityRenderer<RefuelerBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/refueler.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/machines/refueler.png");

    public RefuelerRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(RefuelerBlockEntity refueler, float interp, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        switch (refueler.getBlockState().getValue(RefuelerBlock.FACING)) {
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        MODEL.renderPart("Fueler", ps, buffers.getBuffer(RenderType.entityCutout(TEX)), light);

        double fillLevel = refueler.prevFillLevel + (refueler.fillLevel - refueler.prevFillLevel) * interp;
        int color = FluidType.forFluid(refueler.tank.getTankType()).getColor();
        MODEL.renderPartColorClippedY("Fluid", ps, buffers.getBuffer(ClientRenderHandler.CustomRenderTypes.ADDITIVE_TRIANGLES),
                ((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F, 0.75F,
                (float) ((1 - fillLevel) * -0.625), 0.125F);

        ps.popPose();
    }
}
