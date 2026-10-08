package com.hbm_m.client.render.implementations;

import org.joml.Matrix4f;

import com.hbm_m.blockentity.machines.MachineAssemblyFactoryBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.RenderHooks;
import com.hbm_m.recipe.AssemblerRecipe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * 1:1 {@code RenderAssemblyFactory}: Grundkoerper, Rahmen (wenn oben ein Block sitzt), zwei Schlitten
 * ({@code TragicYuri}) mit je Stoessel- und Saegearm, unter 35 m die Rezeptsymbole der vier Module und die
 * Funkenfaecher der beiden Saegen, sobald deren Blatt tief genug steht.
 */
public class AssemblyFactoryRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineAssemblyFactoryBlockEntity> {

    private static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, p); }

    public static final SimpleObjModel MODEL = new SimpleObjModel(rl("models/block/machines/assembly_factory.obj"));
    public static final ResourceLocation TEX = rl("textures/block/machine/assembly_factory.png");
    public static final ResourceLocation SPARKS_TEX = rl("textures/block/machine/assembly_factory_sparks.png");

    public AssemblyFactoryRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineAssemblyFactoryBlockEntity assemfac, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(90));

        Direction facing = assemfac.getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? assemfac.getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        // Original: Metadaten 2/4/3/5 -> 0/90/180/270 Grad
        switch (facing) {
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        MODEL.renderPart("Base", ps, vc, light);
        if (assemfac.frame) MODEL.renderPart("Frame", ps, vc, light);

        double slide1 = assemfac.animations[0].getSlider(interp);
        double slide2 = assemfac.animations[1].getSlider(interp);
        double[] arm1 = assemfac.animations[0].striker.getPositions(interp);
        double[] arm2 = assemfac.animations[0].saw.getPositions(interp);
        double[] arm3 = assemfac.animations[1].striker.getPositions(interp);
        double[] arm4 = assemfac.animations[1].saw.getPositions(interp);

        ps.pushPose(); {
            ps.translate(0.5 - slide1, 0, 0);
            MODEL.renderPart("Slider1", ps, vc, light);

            pivot(ps, 1.625, -0.9375, -arm1[0]);
            MODEL.renderPart("ArmLower1", ps, vc, light);

            pivot(ps, 2.375, -0.9375, -arm1[1]);
            MODEL.renderPart("ArmUpper1", ps, vc, light);

            pivot(ps, 2.375, -0.4375, -arm1[2]);
            MODEL.renderPart("Head1", ps, vc, light);
            ps.translate(0, arm1[3], 0);
            MODEL.renderPart("Striker1", ps, vc, light);
        } ps.popPose();

        ps.pushPose(); {
            ps.translate(-0.5 + slide1, 0, 0);
            MODEL.renderPart("Slider2", ps, vc, light);

            pivot(ps, 1.625, 0.9375, arm2[0]);
            MODEL.renderPart("ArmLower2", ps, vc, light);

            pivot(ps, 2.375, 0.9375, arm2[1]);
            MODEL.renderPart("ArmUpper2", ps, vc, light);

            pivot(ps, 2.375, 0.4375, arm2[2]);
            MODEL.renderPart("Head2", ps, vc, light);
            ps.translate(0, arm2[3], 0);
            MODEL.renderPart("Striker2", ps, vc, light);
            pivot(ps, 1.625, 0.3125, -arm2[4]);
            MODEL.renderPart("Blade2", ps, vc, light);
        } ps.popPose();

        ps.pushPose(); {
            ps.translate(-0.5 + slide2, 0, 0);
            MODEL.renderPart("Slider3", ps, vc, light);

            pivot(ps, 1.625, 0.9375, arm3[0]);
            MODEL.renderPart("ArmLower3", ps, vc, light);

            pivot(ps, 2.375, 0.9375, arm3[1]);
            MODEL.renderPart("ArmUpper3", ps, vc, light);

            pivot(ps, 2.375, 0.4375, arm3[2]);
            MODEL.renderPart("Head3", ps, vc, light);
            ps.translate(0, arm3[3], 0);
            MODEL.renderPart("Striker3", ps, vc, light);
        } ps.popPose();

        ps.pushPose(); {
            ps.translate(0.5 - slide2, 0, 0);
            MODEL.renderPart("Slider4", ps, vc, light);

            pivot(ps, 1.625, -0.9375, -arm4[0]);
            MODEL.renderPart("ArmLower4", ps, vc, light);

            pivot(ps, 2.375, -0.9375, -arm4[1]);
            MODEL.renderPart("ArmUpper4", ps, vc, light);

            pivot(ps, 2.375, -0.4375, -arm4[2]);
            MODEL.renderPart("Head4", ps, vc, light);
            ps.translate(0, arm4[3], 0);
            MODEL.renderPart("Striker4", ps, vc, light);
            pivot(ps, 1.625, -0.3125, arm4[4]);
            MODEL.renderPart("Blade4", ps, vc, light);
        } ps.popPose();

        var me = Minecraft.getInstance().player;
        if (me != null && me.distanceToSqr(assemfac.getBlockPos().getX() + 0.5, assemfac.getBlockPos().getY() + 1, assemfac.getBlockPos().getZ() + 0.5) < 35 * 35) {

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
                        renderIcon(assemfac, stack, ps, buf, light);
                    }
                }
                ps.popPose();
            }

            // RenderArcFurnace.fullbright(true), ohne Cull, Blend 770/771
            VertexConsumer sparks = buf.getBuffer(RenderType.entityTranslucent(SPARKS_TEX));
            double wide = 0.1875D;
            double narrow = 0.00D;
            double length = 1.25D;
            double uMin = ((assemfac.getLevel().getGameTime() / 10D + interp)) % 10;
            double uMax = uMin + 1;
            double epsilon = 0.01D;

            // renders two layers of sparks, one with regular UV and one with mirrored +0.5 offset
            // render left and right of the blade with small offset to eliminate z-fighting
            ps.pushPose(); if (arm2[3] <= -0.375D) {
                ps.translate(0.5 + slide1, 1.0625D, -arm2[2] / 45D); // arm angle/45 is a seemingly good enough approximation
                Matrix4f m = ps.last().pose();
                v(sparks, m, -epsilon, -wide, length, uMin + 0.5, 0, 0F);
                v(sparks, m, -epsilon, wide, length, uMin + 0.5, 1, 0F);
                v(sparks, m, -epsilon, narrow, 0, uMax + 0.5, 1, 1F);
                v(sparks, m, -epsilon, -narrow, 0, uMax + 0.5, 0, 1F);

                v(sparks, m, epsilon, -wide, length, uMin, 1, 0F);
                v(sparks, m, epsilon, wide, length, uMin, 0, 0F);
                v(sparks, m, epsilon, narrow, 0, uMax, 0, 1F);
                v(sparks, m, epsilon, -narrow, 0, uMax, 1, 1F);
            } ps.popPose();

            ps.pushPose(); if (arm4[3] <= -0.375D) {
                ps.translate(-0.5 - slide2, 1.0625D, arm4[2] / 45D);
                Matrix4f m = ps.last().pose();
                v(sparks, m, -epsilon, -wide, -length, uMin + 0.5, 0, 0F);
                v(sparks, m, -epsilon, wide, -length, uMin + 0.5, 1, 0F);
                v(sparks, m, -epsilon, narrow, 0, uMax + 0.5, 1, 1F);
                v(sparks, m, -epsilon, -narrow, 0, uMax + 0.5, 0, 1F);

                v(sparks, m, epsilon, -wide, -length, uMin, 1, 0F);
                v(sparks, m, epsilon, wide, -length, uMin, 0, 0F);
                v(sparks, m, epsilon, narrow, 0, uMax, 0, 1F);
                v(sparks, m, epsilon, -narrow, 0, uMax, 1, 1F);
            } ps.popPose();
        }

        ps.popPose();
    }

    /** {@code glTranslated(0, y, z); glRotated(angle, 1, 0, 0); glTranslated(0, -y, -z)}. */
    private static void pivot(PoseStack ps, double y, double z, double angle) {
        ps.translate(0, y, z);
        ps.mulPose(Axis.XP.rotationDegrees((float) angle));
        ps.translate(0, -y, -z);
    }

    private static void v(VertexConsumer vc, Matrix4f m, double x, double y, double z, double u, double v, float alpha) {
        RenderHooks.vertexFull(vc, m, (float) x, (float) y, (float) z, 255, 255, 255, (int) (alpha * 255F),
                (float) u, (float) v, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, 0F, 1F, 0F);
    }

    /**
     * Rezeptsymbol wie im Original ueber {@code RenderItem.renderInFrame}: Bloecke 1.25 * 1.25 * 0.25, flache
     * Gegenstaende 1.25 * 0.5128 gross. {@code ItemDisplayContext.FIXED} zeichnet selbst schon halb so gross und
     * mittig, das wird hier ausgeglichen.
     */
    private static void renderIcon(MachineAssemblyFactoryBlockEntity be, ItemStack stack, PoseStack ps, MultiBufferSource buf, int light) {
        var mc = Minecraft.getInstance();
        boolean block3d = false;
        boolean blockItem = stack.getItem() instanceof BlockItem;
        if (blockItem) {
            block3d = mc.getItemRenderer().getModel(stack, be.getLevel(), null, 0).isGui3d();
            if (block3d) {
                ps.translate(0, -0.0625, 0);
            } else {
                ps.translate(0, -0.125, 0);
                ps.scale(0.5F, 0.5F, 0.5F);
            }
        } else {
            ps.mulPose(Axis.XP.rotationDegrees(-90));
            ps.translate(0, -0.25, 0);
        }

        ps.scale(1.25F, 1.25F, 1.25F);

        // renderInFrame
        if (block3d) {
            ps.scale(1.25F, 1.25F, 1.25F);
            ps.translate(0.0F, 0.05F, 0.0F);
            ps.mulPose(Axis.YP.rotationDegrees(-90.0F));
            ps.scale(0.5F, 0.5F, 0.5F); // 0.25 / 0.5 (FIXED)
        } else {
            ps.scale(0.5128205F, 0.5128205F, 0.5128205F);
            ps.translate(0.0F, -0.05F, 0.0F);
            ps.translate(0.0F, 0.25F, 0.0F); // Symbol lag im Original von -0.25 bis 0.75
            ps.scale(2F, 2F, 2F); // 1 / 0.5 (FIXED)
        }

        mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, ps, buf, be.getLevel(), 0);
    }

    @Override public boolean shouldRenderOffScreen(MachineAssemblyFactoryBlockEntity be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
