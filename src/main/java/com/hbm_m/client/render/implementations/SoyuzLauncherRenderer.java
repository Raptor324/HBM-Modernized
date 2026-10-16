package com.hbm_m.client.render.implementations;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.decorations.SoyuzLauncherBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.SoyuzLauncherBlockEntity;
import com.hbm_m.client.model.SoyuzLauncherBakedModel;
import com.hbm_m.client.model.SoyuzRocketBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpec;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.PlatformHooks;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Soyuz launch pad on the {@link MachineRenderers} factory: Table/TowerBase/
 * SupportBase/Legs are static; Tower/Support are the animated clamp arms
 * (rotate 45 degrees at launch); the rocket preview is a dynamic part from an
 * injected model (visible only while hasRocket).
 */
public final class SoyuzLauncherRenderer {

    private static final ResourceLocation ROCKET_MODEL_ID =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/deco_soyuz_rocket");

    /** Ticks over which the arms fully open/close, matching legacy {@code timer = 20}. */
    private static final int ARM_ANIM_TICKS = 20;
    private static final double ARM_OPEN_DEGREES = 45.0D;

    /**
     * Arm opening stroke as a CYCLE hermite track: the reciprocating stroke
     * spline sweeps 0 -> ARM_OPEN_DEGREES over u 0..0.5 and back over 0.5..1
     * (mirrors the press head track pattern). Only the first half of the cycle
     * is ridden while the countdown opens the arms; frozen segments pin u at
     * 0 (clamped onto the rocket) or 0.5 (fully open after launch).
     */
    private static final MachineSpec.Track ARM_TRACK =
            MachineSpec.Track.hermite(new float[] {0f, 1f},
                    new float[] {0f, (float) ARM_OPEN_DEGREES}, true);

    public static void register() {
        MachineRenderers.machine("soyuzlauncher", ModBlockEntities.SOYUZ_LAUNCHER_BE.get(),
                SoyuzLauncherBlockEntity.class)
            .part(SoyuzLauncherBakedModel.TABLE)
            .part(SoyuzLauncherBakedModel.TOWER_BASE)
            .part(SoyuzLauncherBakedModel.SUPPORT_BASE)
            .part(SoyuzLauncherBakedModel.LEGS)
            // Clamp arms: TRACKED rigs (ANIM TRACK FORMAT v2). Legacy:
            // rot = 0 (hasRocket) / 45 (launched), ramping 0 -> 45 over the last
            // 20 countdown ticks. The tracked segment classifier below maps
            // that onto the ARM_TRACK hermite stroke: opening segment
            // (starting && countdown < 20) -> t0 = now - elapsed,
            // u0 = 0, duration 2*20 (u reaches 0.5 = full open after 20 ticks);
            // frozen states -> duration 0, u0 = 0 (clamped) or 0.5 (open).
            // Words are a pure function of the epoch (the segment start tick;
            // starting/countdown are synced via the menu data slots), so the
            // epoch-keyed AnimParams word cache in MachineBer hits while a
            // segment runs. The hermite stroke eases the ramp (legacy was
            // linear) - accepted shape deviation of the track format.
            .trackedChainPart(SoyuzLauncherBakedModel.TOWER,
                    SoyuzLauncherRenderer::armSegmentStart,
                    SoyuzLauncherRenderer::armU0,
                    SoyuzLauncherRenderer::armDuration,
                    MachineSpec.JointNode.rotate(1f, 0f, 0f, 0f, 5.5f, 5.5f,
                            // Never called - the node is tracked (ARM_TRACK).
                            (be, pt, gt, out, off) -> true,
                            ARM_TRACK))
            .trackedChainPart(SoyuzLauncherBakedModel.SUPPORT,
                    SoyuzLauncherRenderer::armSegmentStart,
                    SoyuzLauncherRenderer::armU0,
                    SoyuzLauncherRenderer::armDuration,
                    MachineSpec.JointNode.rotate(-1f, 0f, 0f, 0f, 5.5f, -6.5f,
                            // Never called - the node is tracked (ARM_TRACK).
                            (be, pt, gt, out, off) -> true,
                            ARM_TRACK))
            .dynamicPart("Rocket", SoyuzLauncherRenderer::animateRocket,
                    SoyuzLauncherRenderer::rocketQuads, be -> "rocket")
            .facing(be -> be.getBlockState().getValue(SoyuzLauncherBlock.FACING))
            .register();
    }

    private SoyuzLauncherRenderer() {}

    // -- Arm tracked-rig epoch/header (pure in the synced BE state) -------

    /** Whether the arms are mid-opening (the moving segment of the cycle). */
    private static boolean armOpening(SoyuzLauncherBlockEntity be) {
        return be.isStarting() && be.getCountdown() < ARM_ANIM_TICKS;
    }

    /** Segment start tick (level.getGameTime() domain) - the machine epoch.
     * t0 = now - elapsed, elapsed = the opening ticks already applied to the synced countdown. */
    private static long armSegmentStart(SoyuzLauncherBlockEntity be) {
        if (armOpening(be)) {
            long now = be.getLevel() != null ? be.getLevel().getGameTime() : 0L;
            return now - (ARM_ANIM_TICKS - be.getCountdown());
        }
        return 0L;
    }

    /** Curve position at t0: 0 while opening/clamped, 0.5 = frozen fully open. */
    private static double armU0(SoyuzLauncherBlockEntity be) {
        if (armOpening(be)) return 0.0;
        return be.hasRocket() ? 0.0 : 0.5;
    }

    /** Segment duration in ticks; a decoded 0 freezes u at u0 (clamped/open). */
    private static double armDuration(SoyuzLauncherBlockEntity be) {
        return armOpening(be) ? 2.0 * ARM_ANIM_TICKS : 0.0;
    }

    private static boolean animateRocket(SoyuzLauncherBlockEntity be, float partialTick,
                                         long gameTime, PoseStack pose) {
        if (!be.hasRocket()) return false;
        pose.translate(0.0, 5.0, 0.0);
        return true;
    }

    private static List<net.minecraft.client.renderer.block.model.BakedQuad> rocketQuads(
            SoyuzLauncherBlockEntity be) {
        BakedModel rocketModel = getRocketModel();
        if (!(rocketModel instanceof SoyuzRocketBakedModel model)) return List.of();
        BakedModel part = model.getPart(SoyuzRocketBakedModel.ROCKET);
        if (part == null) return List.of();
        return MeshRenderCache.getOrCompilePartGeometry("soyuzlauncher/rocket", part).solidQuads();
    }

    @Nullable
    private static BakedModel getRocketModel() {
        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = PlatformHooks.getModel(modelManager, ROCKET_MODEL_ID);
        return (model == null || model == modelManager.getMissingModel()) ? null : model;
    }
}
