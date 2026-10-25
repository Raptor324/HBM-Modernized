package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineIndustrialTurbineBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/** 1:1 {@code RenderIndustrialTurbine}: Gehaeuse, Anzeige nach Dampfstufe ({@code 135 - stufe * 90}), drehendes Schwungrad. */
public class IndustrialTurbineRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineIndustrialTurbineBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/machines/industrial_turbine.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/machines/industrial_turbine.png");

    public IndustrialTurbineRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(MachineIndustrialTurbineBlockEntity turbine, float interp, PoseStack ps,
                       MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);

        Direction facing = turbine.getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? turbine.getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        switch (facing) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(0));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            default -> { }
        }

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        MODEL.renderPart("Turbine", ps, vc, light);

        ps.pushPose();
        ps.translate(0, 1.5, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(135 - (ChungusRenderer.steamId(turbine.getSteamTank().getTankType()) - 2) * 90));
        ps.translate(0, -1.5, 0);
        MODEL.renderPart("Gauge", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 1.5, 0);
        ps.mulPose(Axis.ZN.rotationDegrees(turbine.getRotor(interp)));
        ps.translate(0, -1.5, 0);
        MODEL.renderPart("Flywheel", ps, vc, light);
        ps.popPose();

        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(MachineIndustrialTurbineBlockEntity blockEntity) {
        return true;
    }

    @Override public int getViewDistance() { return 256; }
}
