package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineChemicalFactoryBlockEntity;
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

/** 1:1 {@code RenderChemicalFactory}: Grundkoerper, Rahmen (wenn oben ein Block sitzt), zwei Luefter, die sich drehen, solange ein Modul arbeitet. */
public class ChemicalFactoryRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineChemicalFactoryBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/machines/chemical_factory.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/machines/chemical_factory.png");

    public ChemicalFactoryRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineChemicalFactoryBlockEntity chemplant, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(90));

        Direction facing = chemplant.getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? chemplant.getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        switch (facing) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(0));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        float anim = chemplant.prevAnim + (chemplant.anim - chemplant.prevAnim) * interp;

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        MODEL.renderPart("Base", ps, vc, light);
        if (chemplant.frame) MODEL.renderPart("Frame", ps, vc, light);

        ps.pushPose();
        ps.translate(1, 0, 0);
        ps.mulPose(Axis.YP.rotationDegrees((float) (-anim * 45 % 360D)));
        ps.translate(-1, 0, 0);
        MODEL.renderPart("Fan1", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(-1, 0, 0);
        ps.mulPose(Axis.YP.rotationDegrees((float) (-anim * 45 % 360D)));
        ps.translate(1, 0, 0);
        MODEL.renderPart("Fan2", ps, vc, light);
        ps.popPose();

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineChemicalFactoryBlockEntity be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
