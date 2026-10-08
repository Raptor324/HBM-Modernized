package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineChungusBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.inventory.fluid.ModFluids;
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
import net.minecraft.world.level.material.Fluid;

/** 1:1 {@code RenderChungus}: Koerper, Hebel nach Dampfstufe, Laufrad mit dem Rotorwinkel. */
public class ChungusRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineChungusBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/machines/chungus.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/machines/chungus.png");

    public ChungusRenderer(BlockEntityRendererProvider.Context ctx) { }

    /** Original {@code FluidType.ordinal()} = ID: Dampf 2, Heissdampf 3, Superheiss 4, Ultraheiss 5. */
    static int steamId(Fluid f) {
        if (f == ModFluids.HOTSTEAM.getSource()) return 3;
        if (f == ModFluids.SUPERHOTSTEAM.getSource()) return 4;
        if (f == ModFluids.ULTRAHOTSTEAM.getSource()) return 5;
        return 2;
    }

    @Override
    public void render(MachineChungusBlockEntity turbine, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0, 0.5D);

        ps.mulPose(Axis.YP.rotationDegrees(90));

        Direction facing = turbine.getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? turbine.getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        switch (facing) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(0));
            default -> { }
        }

        ps.translate(0, 0, -3);

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        MODEL.renderPart("Body", ps, vc, light);

        ps.pushPose();
        ps.translate(0, 0, 4.5);
        ps.mulPose(Axis.XP.rotationDegrees(15 - (steamId(turbine.getSteamTank().getTankType()) - 2) * 10));
        ps.translate(0, 0, -4.5);
        MODEL.renderPart("Lever", ps, vc, light);
        ps.popPose();

        ps.translate(0, 2.5, 0);
        ps.mulPose(Axis.ZN.rotationDegrees(turbine.getAnim(f)));
        ps.translate(0, -2.5, 0);

        MODEL.renderPart("Blades", ps, vc, light);

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineChungusBlockEntity be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
