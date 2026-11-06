package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineAssemblyFactoryBlockEntity;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.AssemblerRecipe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Modern Nucleus machine renderer for the automated Assembly Factory:
 * - Base chassis and conditional structural frame
 * - Multi-axis robotic arm gantry carriages (Slider 1-4)
 * - Articulated tool heads (hydraulic strikers, circular blades)
 * - Immediate-mode recipe craft icon projection and spark shower hook
 */
public final class MachineFactoryRenderer {

    private static final ResourceLocation SPARKS_TEX =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/assembly_factory_sparks.png");

    public static void register() {
        MachineRenderers.machine("assembly_factory", ModBlockEntities.ASSEMBLY_FACTORY_BE.get(),
                MachineAssemblyFactoryBlockEntity.class)
            .part("Base")
            .conditionalPart("Frame", be -> be.frame)
            // Sliders & articulated robotic tool arm parts
            .part("Slider1", (be, pt, gt, ps) -> animateSlider1(be, pt, ps))
            .part("ArmLower1", (be, pt, gt, ps) -> animateArm1(be, pt, ps, 0))
            .part("ArmUpper1", (be, pt, gt, ps) -> animateArm1(be, pt, ps, 1))
            .part("Head1", (be, pt, gt, ps) -> animateArm1(be, pt, ps, 2))
            .part("Striker1", (be, pt, gt, ps) -> animateArm1(be, pt, ps, 3))

            .part("Slider2", (be, pt, gt, ps) -> animateSlider2(be, pt, ps))
            .part("ArmLower2", (be, pt, gt, ps) -> animateArm2(be, pt, ps, 0))
            .part("ArmUpper2", (be, pt, gt, ps) -> animateArm2(be, pt, ps, 1))
            .part("Head2", (be, pt, gt, ps) -> animateArm2(be, pt, ps, 2))
            .part("Striker2", (be, pt, gt, ps) -> animateArm2(be, pt, ps, 3))
            .part("Blade2", (be, pt, gt, ps) -> animateArm2(be, pt, ps, 4))

            .part("Slider3", (be, pt, gt, ps) -> animateSlider3(be, pt, ps))
            .part("ArmLower3", (be, pt, gt, ps) -> animateArm3(be, pt, ps, 0))
            .part("ArmUpper3", (be, pt, gt, ps) -> animateArm3(be, pt, ps, 1))
            .part("Head3", (be, pt, gt, ps) -> animateArm3(be, pt, ps, 2))
            .part("Striker3", (be, pt, gt, ps) -> animateArm3(be, pt, ps, 3))

            .part("Slider4", (be, pt, gt, ps) -> animateSlider4(be, pt, ps))
            .part("ArmLower4", (be, pt, gt, ps) -> animateArm4(be, pt, ps, 0))
            .part("ArmUpper4", (be, pt, gt, ps) -> animateArm4(be, pt, ps, 1))
            .part("Head4", (be, pt, gt, ps) -> animateArm4(be, pt, ps, 2))
            .part("Striker4", (be, pt, gt, ps) -> animateArm4(be, pt, ps, 3))
            .part("Blade4", (be, pt, gt, ps) -> animateArm4(be, pt, ps, 4))

            .blockTransform(MachineFactoryRenderer::applyFactoryTransform)
            .hook(new MachineRenderHook<MachineAssemblyFactoryBlockEntity>() {
                @Override
                public void render(MachineAssemblyFactoryBlockEntity assemfac, float partialTick, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    renderFactoryOverlays(assemfac, partialTick, ps, buf, light);
                }

                @Override
                public boolean mayDrawContent(MachineAssemblyFactoryBlockEntity assemfac) {
                    return true;
                }
            })
            .itemParts("Base", "Frame")
            .register();
    }

    private static void applyFactoryTransform(MachineAssemblyFactoryBlockEntity assemfac, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        animator.rotate(90, 0, 1, 0);

        Direction facing = assemfac.getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? assemfac.getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        switch (facing) {
            case WEST -> animator.rotate(90, 0, 1, 0);
            case SOUTH -> animator.rotate(180, 0, 1, 0);
            case EAST -> animator.rotate(270, 0, 1, 0);
            default -> {}
        }
    }

    private static void pivot(PoseStack ps, double y, double z, double angle) {
        ps.translate(0.0, y, z);
        ps.mulPose(Axis.XP.rotationDegrees((float) angle));
        ps.translate(0.0, -y, -z);
    }

    private static boolean animateSlider1(MachineAssemblyFactoryBlockEntity be, float pt, PoseStack ps) {
        double slide1 = be.animations[0].getSlider(pt);
        ps.translate(0.5 - slide1, 0, 0);
        return true;
    }

    private static boolean animateArm1(MachineAssemblyFactoryBlockEntity be, float pt, PoseStack ps, int stage) {
        double slide1 = be.animations[0].getSlider(pt);
        double[] arm1 = be.animations[0].striker.getPositions(pt);
        ps.translate(0.5 - slide1, 0, 0);
        pivot(ps, 1.625, -0.9375, -arm1[0]);
        if (stage == 0) return true;
        pivot(ps, 2.375, -0.9375, -arm1[1]);
        if (stage == 1) return true;
        pivot(ps, 2.375, -0.4375, -arm1[2]);
        if (stage == 2) return true;
        ps.translate(0, arm1[3], 0);
        return true;
    }

    private static boolean animateSlider2(MachineAssemblyFactoryBlockEntity be, float pt, PoseStack ps) {
        double slide1 = be.animations[0].getSlider(pt);
        ps.translate(-0.5 + slide1, 0, 0);
        return true;
    }

    private static boolean animateArm2(MachineAssemblyFactoryBlockEntity be, float pt, PoseStack ps, int stage) {
        double slide1 = be.animations[0].getSlider(pt);
        double[] arm2 = be.animations[0].saw.getPositions(pt);
        ps.translate(-0.5 + slide1, 0, 0);
        pivot(ps, 1.625, 0.9375, arm2[0]);
        if (stage == 0) return true;
        pivot(ps, 2.375, 0.9375, arm2[1]);
        if (stage == 1) return true;
        pivot(ps, 2.375, 0.4375, arm2[2]);
        if (stage == 2) return true;
        ps.translate(0, arm2[3], 0);
        if (stage == 3) return true;
        pivot(ps, 1.625, 0.3125, -arm2[4]);
        return true;
    }

    private static boolean animateSlider3(MachineAssemblyFactoryBlockEntity be, float pt, PoseStack ps) {
        double slide2 = be.animations[1].getSlider(pt);
        ps.translate(-0.5 + slide2, 0, 0);
        return true;
    }

    private static boolean animateArm3(MachineAssemblyFactoryBlockEntity be, float pt, PoseStack ps, int stage) {
        double slide2 = be.animations[1].getSlider(pt);
        double[] arm3 = be.animations[1].striker.getPositions(pt);
        ps.translate(-0.5 + slide2, 0, 0);
        pivot(ps, 1.625, 0.9375, arm3[0]);
        if (stage == 0) return true;
        pivot(ps, 2.375, 0.9375, arm3[1]);
        if (stage == 1) return true;
        pivot(ps, 2.375, 0.4375, arm3[2]);
        if (stage == 2) return true;
        ps.translate(0, arm3[3], 0);
        return true;
    }

    private static boolean animateSlider4(MachineAssemblyFactoryBlockEntity be, float pt, PoseStack ps) {
        double slide2 = be.animations[1].getSlider(pt);
        ps.translate(0.5 - slide2, 0, 0);
        return true;
    }

    private static boolean animateArm4(MachineAssemblyFactoryBlockEntity be, float pt, PoseStack ps, int stage) {
        double slide2 = be.animations[1].getSlider(pt);
        double[] arm4 = be.animations[1].saw.getPositions(pt);
        ps.translate(0.5 - slide2, 0, 0);
        pivot(ps, 1.625, -0.9375, -arm4[0]);
        if (stage == 0) return true;
        pivot(ps, 2.375, -0.9375, -arm4[1]);
        if (stage == 1) return true;
        pivot(ps, 2.375, -0.4375, -arm4[2]);
        if (stage == 2) return true;
        ps.translate(0, arm4[3], 0);
        if (stage == 3) return true;
        pivot(ps, 1.625, -0.3125, arm4[4]);
        return true;
    }

    private static void renderFactoryOverlays(MachineAssemblyFactoryBlockEntity assemfac, float partialTick,
                                              PoseStack ps, MultiBufferSource buf, int light) {
        var me = Minecraft.getInstance().player;
        if (me == null || me.distanceToSqr(assemfac.getBlockPos().getX() + 0.5,
                assemfac.getBlockPos().getY() + 1.0, assemfac.getBlockPos().getZ() + 0.5) >= 35.0 * 35.0) {
            return;
        }

        // 1. Recipe icons on 4 modules
        for (int i = 0; i < 4; i++) {
            ps.pushPose();
            ps.translate(1.5 - i, 0, 0);
            ps.mulPose(Axis.YP.rotationDegrees(90));
            ps.translate(0, 1.0625, 0);

            AssemblerRecipe recipe = assemfac.getRecipe(i);
            if (recipe != null) {
                ItemStack stack = recipe.getResultItemSafe().copy();
                if (!stack.isEmpty()) {
                    stack.setCount(1);
                    ps.pushPose();
                    ps.translate(0, 0.25, 0);
                    ps.scale(0.35F, 0.35F, 0.35F);
                    Minecraft.getInstance().getItemRenderer().renderStatic(stack,
                            net.minecraft.world.item.ItemDisplayContext.FIXED, light,
                            OverlayTexture.NO_OVERLAY, ps, buf, assemfac.getLevel(), 0);
                    ps.popPose();
                }
            }
            ps.popPose();
        }

        // 2. Spark fans when saws are active
        double[] arm2 = assemfac.animations[0].saw.getPositions(partialTick);
        double[] arm4 = assemfac.animations[1].saw.getPositions(partialTick);

        VertexConsumer sparks = buf.getBuffer(RenderType.entityTranslucent(SPARKS_TEX));
        double wide = 0.1875D;

        if (arm2[3] <= -0.5D) {
            ps.pushPose();
            double slide1 = assemfac.animations[0].getSlider(partialTick);
            ps.translate(-0.5 + slide1, 0, 0);
            pivot(ps, 1.625, 0.9375, arm2[0]);
            pivot(ps, 2.375, 0.9375, arm2[1]);
            pivot(ps, 2.375, 0.4375, arm2[2]);
            ps.translate(0, arm2[3], 0);
            drawSparkFan(sparks, ps.last().pose(), ps.last().normal(), 1.0625D, 0.3125D, wide, 1);
            ps.popPose();
        }

        if (arm4[3] <= -0.5D) {
            ps.pushPose();
            double slide2 = assemfac.animations[1].getSlider(partialTick);
            ps.translate(0.5 - slide2, 0, 0);
            pivot(ps, 1.625, -0.9375, -arm4[0]);
            pivot(ps, 2.375, -0.9375, -arm4[1]);
            pivot(ps, 2.375, -0.4375, -arm4[2]);
            ps.translate(0, arm4[3], 0);
            drawSparkFan(sparks, ps.last().pose(), ps.last().normal(), 1.0625D, -0.3125D, wide, -1);
            ps.popPose();
        }
    }

    private static void drawSparkFan(VertexConsumer vc, Matrix4f m, Matrix3f n,
                                     double y, double z, double wide, int dir) {
        float f0 = 0.0F, f1 = 1.0F;
        // uv2(240, 240) on 1.20.1 packs to LightTexture.FULL_BRIGHT (0xF000F0)
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, 0.0F, (float) (y - wide), (float) (z - wide * dir), 1F, 1F, 1F, 1F, f1, f1, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, n, 0F, 1F, 0F);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, 0.0F, (float) (y - wide), (float) (z + wide * dir), 1F, 1F, 1F, 1F, f1, f0, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, n, 0F, 1F, 0F);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, 0.0F, (float) (y + wide), (float) (z + wide * dir), 1F, 1F, 1F, 1F, f0, f0, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, n, 0F, 1F, 0F);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, 0.0F, (float) (y + wide), (float) (z - wide * dir), 1F, 1F, 1F, 1F, f0, f1, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, n, 0F, 1F, 0F);
    }

    private MachineFactoryRenderer() {}
}
