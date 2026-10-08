package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineRTGBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.interfaces.IEnergyConnector;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code RenderRTG}: das Gehaeuse ({@code Gen}, 180 Grad gedreht) steht im Blockmodell, hier nur die
 * Anschlussstutzen ({@code Connector}) zu jedem Nachbarn, der von dieser Seite Energie annimmt/abgibt
 * ({@code Library.canConnect}).
 */
public class RTGRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineRTGBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/machines/rtg.obj"));
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/rtg.png");

    public RTGRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineRTGBlockEntity te, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        Level level = te.getLevel();
        if (level == null) return;
        BlockPos p = te.getBlockPos();

        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);
        ps.mulPose(Axis.YP.rotationDegrees(180));
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));

        if (canConnect(level, p.east(), Direction.WEST)) MODEL.renderPart("Connector", ps, vc, light);

        if (canConnect(level, p.west(), Direction.EAST)) {
            ps.mulPose(Axis.YP.rotationDegrees(180));
            MODEL.renderPart("Connector", ps, vc, light);
            ps.mulPose(Axis.YP.rotationDegrees(-180));
        }

        if (canConnect(level, p.north(), Direction.SOUTH)) {
            ps.mulPose(Axis.YP.rotationDegrees(90));
            MODEL.renderPart("Connector", ps, vc, light);
            ps.mulPose(Axis.YP.rotationDegrees(-90));
        }

        if (canConnect(level, p.south(), Direction.NORTH)) {
            ps.mulPose(Axis.YP.rotationDegrees(-90));
            MODEL.renderPart("Connector", ps, vc, light);
            ps.mulPose(Axis.YP.rotationDegrees(90));
        }

        ps.popPose();
    }

    /** {@code Library.canConnect}: Energie-Verbinder, der von dieser Seite aus verbinden will. */
    private static boolean canConnect(Level level, BlockPos pos, Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof IEnergyConnector c && c.canConnectEnergy(side);
    }
}
