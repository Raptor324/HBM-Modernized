package com.hbm_m.client.render.implementations;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.LaunchpadSoyuzBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.LaunchpadSoyuzBlockEntity;
import com.hbm_m.client.model.SoyuzRocketBakedModel;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.LoaderHooks;
import com.hbm_m.util.MultipartFacingTransforms;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1:1 {@code RenderLaunchpadSoyuz} on the {@link MachineRenderers} factory: Launchpad static;
 * Strut1-5 slide along +Z (ext 4.5 / 3.0); Carriage+Wheels+Rotor+Mount ride the carriage chain
 * (carriage slide, buffer-stop tilt, rotor flip); the rocket (SoyuzPronter port, per-skin baked
 * model) either rides the mount or - locked during the countdown - stands world-aligned at
 * {@code (0, 4, 0)}. Original model-space offset {@code T(-4, 0, -4)} lives in the block transform.
 * Facing rotation per the original metadata table (see {@link MultipartFacingTransforms#launchpadPoseRotationYDegrees}).
 */
public final class LaunchpadSoyuzRenderer {

    /** Per-skin rocket models (legacy {@code SoyuzPronter.SoyuzSkin}: SOYUZ/LUNA/AUTHENTIC). */
    private static final ResourceLocation[] ROCKET_MODEL_IDS = {
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/deco_soyuz_rocket"),
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/soyuz_rocket_luna"),
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/soyuz_rocket_authentic")
    };

    /** Wheels of the carriage, original double arrays (model-space pivots). */
    private static final double[] WHEELS_FORWARD = new double[] {17D, 19D, 29D, 31D};
    private static final double[] WHEELS_SIDE = new double[] {6.75D, 5.25D, -5.25D, -6.75D};

    public static void register() {
        MachineRenderers.machine("launchpadsoyuz", ModBlockEntities.LAUNCHPAD_SOYUZ_BE.get(),
                LaunchpadSoyuzBlockEntity.class)
            .blockTransform(LaunchpadSoyuzRenderer::applyBlockTransform)
            .part("Launchpad")
            .part("Strut1", LaunchpadSoyuzRenderer::animateStrut1)
            .part("Strut2", LaunchpadSoyuzRenderer::animateStrut2)
            .part("Strut3", LaunchpadSoyuzRenderer::animateStrut3)
            .part("Strut4", LaunchpadSoyuzRenderer::animateStrut4)
            .part("Strut5", LaunchpadSoyuzRenderer::animateStrut5)
            .part("Carriage", LaunchpadSoyuzRenderer::animateCarriage)
            .part("Wheel_1_1", LaunchpadSoyuzRenderer::animateWheel_1_1)
            .part("Wheel_1_2", LaunchpadSoyuzRenderer::animateWheel_1_2)
            .part("Wheel_1_3", LaunchpadSoyuzRenderer::animateWheel_1_3)
            .part("Wheel_1_4", LaunchpadSoyuzRenderer::animateWheel_1_4)
            .part("Wheel_2_1", LaunchpadSoyuzRenderer::animateWheel_2_1)
            .part("Wheel_2_2", LaunchpadSoyuzRenderer::animateWheel_2_2)
            .part("Wheel_2_3", LaunchpadSoyuzRenderer::animateWheel_2_3)
            .part("Wheel_2_4", LaunchpadSoyuzRenderer::animateWheel_2_4)
            .part("Wheel_3_1", LaunchpadSoyuzRenderer::animateWheel_3_1)
            .part("Wheel_3_2", LaunchpadSoyuzRenderer::animateWheel_3_2)
            .part("Wheel_3_3", LaunchpadSoyuzRenderer::animateWheel_3_3)
            .part("Wheel_3_4", LaunchpadSoyuzRenderer::animateWheel_3_4)
            .part("Wheel_4_1", LaunchpadSoyuzRenderer::animateWheel_4_1)
            .part("Wheel_4_2", LaunchpadSoyuzRenderer::animateWheel_4_2)
            .part("Wheel_4_3", LaunchpadSoyuzRenderer::animateWheel_4_3)
            .part("Wheel_4_4", LaunchpadSoyuzRenderer::animateWheel_4_4)
            .part("Rotor", LaunchpadSoyuzRenderer::animateRotor)
            .part("Mount", LaunchpadSoyuzRenderer::animateMount)
            .dynamicPart("Rocket", LaunchpadSoyuzRenderer::animateRocket,
                    LaunchpadSoyuzRenderer::rocketQuads, be -> "rocket" + be.getRocketType())
            .facing(be -> be.getBlockState().getValue(LaunchpadSoyuzBlock.FACING))
            // Original ItemRenderBase: Launchpad + die fuenf Streben
            .itemParts("Launchpad", "Strut1", "Strut2", "Strut3", "Strut4", "Strut5")
            .viewDistance(256) // Original getMaxRenderDistanceSquared = 65536
            .register();
    }

    private LaunchpadSoyuzRenderer() {}

    // ─── Block transform: T(0.5)·R(pad)·T(-4,0,-4) (Original renderTileEntityAt) ──

    private static void applyBlockTransform(LaunchpadSoyuzBlockEntity be, LegacyAnimator animator) {
        Direction facing = facingOf(be);
        animator.translate(0.5, 0.0, 0.5);
        animator.rotate(MultipartFacingTransforms.launchpadPoseRotationYDegrees(facing), 0, 1, 0);
        animator.translate(-4f, 0.0f, -4f);
    }

    private static float facingRotation(LaunchpadSoyuzBlockEntity be) {
        return MultipartFacingTransforms.launchpadPoseRotationYDegrees(facingOf(be));
    }

    private static Direction facingOf(LaunchpadSoyuzBlockEntity be) {
        return be.getBlockState().hasProperty(LaunchpadSoyuzBlock.FACING)
                ? be.getBlockState().getValue(LaunchpadSoyuzBlock.FACING)
                : Direction.NORTH;
    }

    // ─── Struts: slide along +Z, ext 4.5 (Strut5: 3.0) ─────────────────────────

    private static void animateStrutN(LaunchpadSoyuzBlockEntity be, float partialTick,
                                      PoseStack pose, int index, float ext) {
        float strut = Mth.clamp(be.getInterpPos(index, partialTick) * -ext + ext, 0F, ext);
        pose.translate(0, 0, strut);
    }

    private static boolean animateStrut1(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) {
        animateStrutN(be, pt, pose, LaunchpadSoyuzBlockEntity.INDEX_STRUT1, 4.5F);
        return true;
    }

    private static boolean animateStrut2(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) {
        animateStrutN(be, pt, pose, LaunchpadSoyuzBlockEntity.INDEX_STRUT2, 4.5F);
        return true;
    }

    private static boolean animateStrut3(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) {
        animateStrutN(be, pt, pose, LaunchpadSoyuzBlockEntity.INDEX_STRUT3, 4.5F);
        return true;
    }

    private static boolean animateStrut4(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) {
        animateStrutN(be, pt, pose, LaunchpadSoyuzBlockEntity.INDEX_STRUT4, 4.5F);
        return true;
    }

    private static boolean animateStrut5(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) {
        animateStrutN(be, pt, pose, LaunchpadSoyuzBlockEntity.INDEX_STRUT5, 3.0F);
        return true;
    }

    // ─── Carriage chain: slide + tilt (Original GL sequence) ───────────────────

    private static float carriageOffset(LaunchpadSoyuzBlockEntity be, float partialTick) {
        return Mth.clamp(be.getInterpPos(LaunchpadSoyuzBlockEntity.INDEX_CARRIAGE, partialTick) * -19.5F + 19.5F, 0F, 19.5F);
    }

    private static float tiltAngle(LaunchpadSoyuzBlockEntity be, float partialTick) {
        return be.getInterpPos(LaunchpadSoyuzBlockEntity.INDEX_TILT, partialTick);
    }

    private static float rotorAngle(LaunchpadSoyuzBlockEntity be, float partialTick) {
        return Mth.clamp(be.getInterpPos(LaunchpadSoyuzBlockEntity.INDEX_ROTOR, partialTick) * -180F + 180F, 0F, 180F);
    }

    private static float wheelAngle(float carriage) {
        return (float) (carriage * 360D / Math.PI);
    }

    /** Slide + tilt: {@code T(0,0,-c)·T(0,1.5,-32)·R_x(-tilt)·T(0,-1.5,32)}. */
    private static void applyCarriageChain(LaunchpadSoyuzBlockEntity be, float partialTick, PoseStack pose) {
        pose.translate(0, 0, -carriageOffset(be, partialTick));

        pose.translate(0, 1.5, -32);
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-tiltAngle(be, partialTick)));
        pose.translate(0, -1.5, 32);
    }

    private static boolean animateCarriage(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) {
        applyCarriageChain(be, pt, pose);
        return true;
    }

    // ─── Wheels: carriage chain + roll around the vertical axle ────────────────

    private static boolean animateWheel(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose,
                                        int i, int j) {
        applyCarriageChain(be, pt, pose);

        double v0 = WHEELS_FORWARD[i - 1];
        double v1 = WHEELS_SIDE[j - 1];

        pose.translate(v1, 0, -v0);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(wheelAngle(carriageOffset(be, pt)) * (j % 2 == 0 ? -1 : 1)));
        pose.translate(-v1, 0, v0);
        return true;
    }

    private static boolean animateWheel_1_1(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) { return animateWheel(be, pt, t, pose, 1, 1); }
    private static boolean animateWheel_1_2(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) { return animateWheel(be, pt, t, pose, 1, 2); }
    private static boolean animateWheel_1_3(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) { return animateWheel(be, pt, t, pose, 1, 3); }
    private static boolean animateWheel_1_4(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) { return animateWheel(be, pt, t, pose, 1, 4); }
    private static boolean animateWheel_2_1(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) { return animateWheel(be, pt, t, pose, 2, 1); }
    private static boolean animateWheel_2_2(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) { return animateWheel(be, pt, t, pose, 2, 2); }
    private static boolean animateWheel_2_3(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) { return animateWheel(be, pt, t, pose, 2, 3); }
    private static boolean animateWheel_2_4(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) { return animateWheel(be, pt, t, pose, 2, 4); }
    private static boolean animateWheel_3_1(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) { return animateWheel(be, pt, t, pose, 3, 1); }
    private static boolean animateWheel_3_2(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) { return animateWheel(be, pt, t, pose, 3, 2); }
    private static boolean animateWheel_3_3(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) { return animateWheel(be, pt, t, pose, 3, 3); }
    private static boolean animateWheel_3_4(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) { return animateWheel(be, pt, t, pose, 3, 4); }
    private static boolean animateWheel_4_1(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) { return animateWheel(be, pt, t, pose, 4, 1); }
    private static boolean animateWheel_4_2(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) { return animateWheel(be, pt, t, pose, 4, 2); }
    private static boolean animateWheel_4_3(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) { return animateWheel(be, pt, t, pose, 4, 3); }
    private static boolean animateWheel_4_4(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) { return animateWheel(be, pt, t, pose, 4, 4); }

    // ─── Rotor + Mount: carriage chain + flip around the X axis ────────────────

    private static boolean animateRotor(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) {
        applyCarriageChain(be, pt, pose);

        pose.translate(0, 24.5, -18);
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-rotorAngle(be, pt)));
        pose.translate(0, -24.5, 18);
        return true;
    }

    private static boolean animateMount(LaunchpadSoyuzBlockEntity be, float pt, long t, PoseStack pose) {
        applyCarriageChain(be, pt, pose);

        float rotor = rotorAngle(be, pt);
        pose.translate(0, 24.5, -18);
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-rotor));
        pose.translate(0, -24.5, 18);

        pose.translate(0, 24.5, -6);
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(rotor));
        pose.translate(0, -24.5, 6);
        return true;
    }

    // ─── Rocket (SoyuzPronter port via the per-skin baked model) ───────────────

    private static boolean animateRocket(LaunchpadSoyuzBlockEntity be, float partialTick,
                                         long gameTime, PoseStack pose) {
        boolean renderSoyuz = be.getRocketType() >= 0
                && be.soyuzStatus != LaunchpadSoyuzBlockEntity.SoyuzStatus.ABSENT;
        boolean lockSoyuz = be.soyuzStatus == LaunchpadSoyuzBlockEntity.SoyuzStatus.LAUNCHING;
        if (!renderSoyuz) return false;

        if (lockSoyuz) {
            // Original: launch lock - the rocket stands world-aligned at the tower top
            pose.translate(0, 4, 0);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-facingRotation(be)));
            return true;
        }

        // idle: the rocket rides the carriage/rotor/mount assembly
        applyCarriageChain(be, partialTick, pose);

        float rotor = rotorAngle(be, partialTick);
        pose.translate(0, 24.5, -18);
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-rotor));
        pose.translate(0, -24.5, 18);

        pose.translate(0, 24.5, -6);
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(rotor));
        pose.translate(0, -24.5, 6);

        pose.translate(0, 4, 0);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-facingRotation(be)));
        return true;
    }

    private static List<net.minecraft.client.renderer.block.model.BakedQuad> rocketQuads(
            LaunchpadSoyuzBlockEntity be) {
        BakedModel rocketModel = getRocketModel(be.getRocketType());
        if (!(rocketModel instanceof SoyuzRocketBakedModel model)) return List.of();
        BakedModel part = model.getPart(SoyuzRocketBakedModel.ROCKET);
        if (part == null) return List.of();
        return MeshRenderCache.getOrCompilePartGeometry("launchpadsoyuz/rocket/" + be.getRocketType(), part).solidQuads();
    }

    @Nullable
    private static BakedModel getRocketModel(int skin) {
        int index = (skin >= 0 && skin < ROCKET_MODEL_IDS.length) ? skin : 0;
        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = LoaderHooks.getModel(modelManager, ROCKET_MODEL_IDS[index]);
        return (model == null || model == modelManager.getMissingModel()) ? null : model;
    }
}
