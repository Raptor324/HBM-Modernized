package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachinePrecAssBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.PrecAssRecipe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code RenderPrecAss}: Modell {@code assembly_machine.obj} mit Praezisionstextur. Sockel, Geruest (nur mit Block
 * darueber), drehender Ring, vier Arme mit Schlagbolzen und das Rezeptsymbol in der Mitte (bis 35 Bloecke).
 */
public class MachinePrecAssRenderer implements BlockEntityRenderer<MachinePrecAssBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/machines/precass.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/precass.png");

    public MachinePrecAssRenderer(BlockEntityRendererProvider.Context ctx) { }

    private static double interp(double prev, double cur, float t) {
        return prev + (cur - prev) * t;
    }

    @Override
    public void render(MachinePrecAssBlockEntity assembler, float interp, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(90));

        switch (assembler.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(TEX));
        MODEL.renderPart("Base", ps, vc, light);
        if (assembler.frame) MODEL.renderPart("Frame", ps, vc, light);

        ps.pushPose();
        double spin = interp(assembler.prevRing, assembler.ring, interp);
        double[] arm = new double[] {
                interp(assembler.prevArmAngles[0], assembler.armAngles[0], interp),
                interp(assembler.prevArmAngles[1], assembler.armAngles[1], interp),
                interp(assembler.prevArmAngles[2], assembler.armAngles[2], interp)
        };

        ps.mulPose(Axis.YP.rotationDegrees((float) spin));
        MODEL.renderPart("Ring", ps, vc, light);
        MODEL.renderPart("Ring2", ps, vc, light);

        for (int i = 0; i < 4; i++) {
            renderArm(ps, vc, light, arm, interp(assembler.prevStrikers[i], assembler.strikers[i], interp));
            ps.mulPose(Axis.YP.rotationDegrees(-90));
        }
        ps.popPose();

        PrecAssRecipe recipe = assembler.getSelectedRecipe();
        var me = Minecraft.getInstance().player;
        if (recipe != null && me != null && me.distanceToSqr(assembler.getBlockPos().getX() + 0.5, assembler.getBlockPos().getY() + 1, assembler.getBlockPos().getZ() + 0.5) < 35 * 35) {
            ps.mulPose(Axis.YP.rotationDegrees(90));
            ps.translate(0, 1.0625, 0);

            ItemStack stack = recipe.getResultItemSafe();
            stack.setCount(1);

            if (stack.getItem() instanceof BlockItem) {
                ps.translate(0, 0.0625 + 0.125, 0);
            } else {
                ps.mulPose(Axis.XN.rotationDegrees(90));
                ps.translate(0, -0.25 + 0.25, 0);
            }
            ps.scale(1.25F * 0.5F, 1.25F * 0.5F, 1.25F * 0.5F);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, ps, buffers, assembler.getLevel(), 0);
        }

        ps.popPose();
    }

    public static void renderArm(PoseStack ps, VertexConsumer vc, int light, double[] arm, double striker) {
        ps.pushPose();
        ps.translate(0, 1.625, 0.9375);
        ps.mulPose(Axis.XP.rotationDegrees((float) arm[0]));
        ps.translate(0, -1.625, -0.9375);
        MODEL.renderPart("ArmLower1", ps, vc, light);

        ps.translate(0, 2.375, 0.9375);
        ps.mulPose(Axis.XP.rotationDegrees((float) arm[1]));
        ps.translate(0, -2.375, -0.9375);
        MODEL.renderPart("ArmUpper1", ps, vc, light);

        ps.translate(0, 2.375, 0.4375);
        ps.mulPose(Axis.XP.rotationDegrees((float) arm[2]));
        ps.translate(0, -2.375, -0.4375);
        MODEL.renderPart("Head1", ps, vc, light);
        ps.translate(0, striker, 0);
        MODEL.renderPart("Spike1", ps, vc, light);
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(MachinePrecAssBlockEntity be) {
        return true;
    }
}
