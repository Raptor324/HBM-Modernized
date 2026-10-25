package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachineTapeDriveBlock;
import com.hbm_m.blockentity.machines.MachineTapeDriveBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
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
 * 1:1 {@code RenderTapeDrive}: Gehaeuse, je belegtem Platz ein Laufwerk und eine leuchtende Lampe (rot: fremd oder
 * defekt, orange: leer, gruen: beschrieben).
 */
public class TapeDriveRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineTapeDriveBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/machines/tape_drive.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/machines/tape_drive.png");

    public TapeDriveRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineTapeDriveBlockEntity drive, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        switch (drive.getBlockState().getValue(MachineTapeDriveBlock.FACING)) {
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            default -> { }
        }

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        MODEL.renderPart("Frame", ps, vc, light);

        for (int i = 0; i < MachineTapeDriveBlockEntity.SLOT_COUNT; i++) {
            if (drive.getTapeState(i) == MachineTapeDriveBlockEntity.SLOT_EMPTY) continue;
            ps.pushPose();
            ps.translate(0, 0.25 - 0.5 * (i / 6), 0.3125 - (i % 6) * 0.125);
            MODEL.renderPart("Drive", ps, vc, light);
            ps.popPose();
        }

        VertexConsumer lights = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL);
        for (int i = 0; i < MachineTapeDriveBlockEntity.SLOT_COUNT; i++) {
            byte tape = drive.getTapeState(i);
            if (tape == MachineTapeDriveBlockEntity.SLOT_EMPTY) continue;

            float r = 1F, g = 0F;
            if (tape == MachineTapeDriveBlockEntity.SLOT_EMPTY_TAPE) g = 0.75F;
            if (tape == MachineTapeDriveBlockEntity.SLOT_FILLED_TAPE) { r = 0F; g = 1F; }

            ps.pushPose();
            ps.translate(0, 0.25 - 0.5 * (i / 6), 0.3125 - (i % 6) * 0.125);
            MODEL.renderPartColor("Light", ps, lights, r, g, 0F, 1F);
            ps.popPose();
        }

        ps.popPose();
    }
}
