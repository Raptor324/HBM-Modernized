package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.HeatingOvenBlockEntity;
import com.hbm_m.blockentity.machines.MachineFireboxBaseBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/** 1:1 {@code RenderHeatingOven}: Gehaeuse, Schiebeklappe ({@code door * 0.75 / 135}), Glut voll leuchtend wenn an. */
public class HeatingOvenRenderer implements com.hbm_m.client.render.HbmBerBounds<HeatingOvenBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/machines/heating_oven.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/machines/heating_oven.png");

    public HeatingOvenRenderer(BlockEntityRendererProvider.Context context) { }

    /** Original: Metadaten 3/5/2/4 -> 0/90/180/270 Grad, danach -90. */
    static void rotate(MachineFireboxBaseBlockEntity tile, PoseStack ps) {
        Direction facing = tile.getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? tile.getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        switch (facing) {
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(0));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }
        ps.mulPose(Axis.YP.rotationDegrees(-90));
    }

    @Override
    public void render(HeatingOvenBlockEntity oven, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);
        rotate(oven, ps);

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        MODEL.renderPart("Main", ps, vc, light);

        ps.pushPose();
        float door = oven.prevDoorAngle + (oven.doorAngle - oven.prevDoorAngle) * interp;
        ps.translate(0, 0, door * 0.75D / 135D);
        MODEL.renderPart("Door", ps, vc, light);
        ps.popPose();

        if (oven.wasOn) {
            MODEL.renderPart("InnerBurning", ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), LightTexture.FULL_BRIGHT);
        } else {
            MODEL.renderPart("Inner", ps, vc, light);
        }

        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(HeatingOvenBlockEntity blockEntity) {
        return true;
    }

    @Override public int getViewDistance() { return 256; }
}
