package com.hbm_m.client.render.implementations;

import java.util.List;
import java.util.Map;

import com.hbm_m.blockentity.machines.MachineRTGBlockEntity;
import com.hbm_m.interfaces.IEnergyConnector;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1-Port von {@code RenderRTG} (1.7.10): der Generator ({@code Gen}) steckt im Blockmodell, hier
 * kommt zu jeder Seite mit einem Stromanschluss ein Kabelstutzen ({@code Connector}) dazu.
 */
public class RtgRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineRTGBlockEntity> {

    public RtgRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(MachineRTGBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null) return;
        Map<String, List<float[]>> obj = ObjMachines.obj("rtg");
        if (obj.isEmpty()) return;
        TextureAtlasSprite sprite = ObjMachines.sprite("rtg");
        VertexConsumer vc = buffer.getBuffer(RenderType.cutout());

        pose.pushPose();
        ObjMachines.begin(pose, be.getBlockState(), 180F);
        // Original: +X as modelled, -X turned 180, -Z turned 90, +Z turned -90.
        connector(vc, pose, obj, sprite, level, be, Direction.EAST, 0F, packedLight, packedOverlay);
        connector(vc, pose, obj, sprite, level, be, Direction.WEST, 180F, packedLight, packedOverlay);
        connector(vc, pose, obj, sprite, level, be, Direction.NORTH, 90F, packedLight, packedOverlay);
        connector(vc, pose, obj, sprite, level, be, Direction.SOUTH, -90F, packedLight, packedOverlay);
        pose.popPose();
    }

    private static void connector(VertexConsumer vc, PoseStack pose, Map<String, List<float[]>> obj,
                                  TextureAtlasSprite sprite, Level level, BlockEntity be, Direction side,
                                  float rotation, int light, int overlay) {
        BlockEntity neighbour = level.getBlockEntity(be.getBlockPos().relative(side));
        if (!(neighbour instanceof IEnergyConnector connector) || !connector.canConnectEnergy(side.getOpposite())) return;
        pose.pushPose();
        if (rotation != 0F) pose.mulPose(Axis.YP.rotationDegrees(rotation));
        ObjMachines.group(vc, pose, obj, "Connector", sprite, light, overlay);
        pose.popPose();
    }
}
