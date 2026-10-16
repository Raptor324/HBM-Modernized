package com.hbm_m.client.render.implementations;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineRadarBlockEntity;
import com.hbm_m.client.model.MachineRadarBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpec;
import com.hbm_m.main.MainRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Radar (small and large) on the {@link MachineRenderers} factory: Base is
 * static, Dish is animated. Geometry of both depends on the model variant
 * ({@link MachineRadarBakedModel#isLargeRadar()}), so both are declared as
 * dynamic parts with a per-BE cache key (small/large). The dish spins via
 * prevRotation/rotation while powered.
 */
public final class MachineRadarRenderer {

    /** Dish pivot offset for the small radar (original {@code glTranslated(-0.125, 0, 0)}). */
    private static final float SMALL_DISH_PIVOT_OFFSET_X = -0.125F;

    private static volatile boolean loggedModelResolveFailure;

    public static void register() {
        MachineRenderers.machine("radar", ModBlockEntities.RADAR_BE.get(), MachineRadarBlockEntity.class)
            .dynamicPart("Base", MachineRadarRenderer::baseQuads, MachineRadarRenderer::sizeKey)
            // Dish: TRACKED rig (ANIM TRACK FORMAT v2) - one full revolution per
            // spin segment, driven by the segment header (t0/u0/durationTicks).
            // Legacy spin rate: rotation += 5 deg/tick while powered -> 72 ticks
            // per revolution; the track runs 0 -> -360 (legacy applied -angle).
            // Segment classifier (pure in the synced BE state): powered -> t0 = 1,
            // duration 72; unpowered -> duration 0 = FROZEN at u0 = 0 (rest pose).
            // The words are a pure function of the epoch (the segment start),
            // so the epoch-keyed AnimParams word cache in MachineBer hits while
            // the dish spins and the CPU skips rig evaluation entirely.
            // Node layout vs the legacy animator T(0.5,0,0.5)*R(-a)*T(off)*T(-0.5):
            // rotate node about pivot (0.5, 0, 0.5) then translate node (off, 0, 0)
            // compose to the identical matrix (both trailing translates sit after
            // the rotation). The small radar's pivot offset is a STREAMED node
            // (segment-pure: constant per model size, TRACKED_STREAM_BUDGET = 4).
            .trackedChainPart("Dish", "Dish",
                    0f, 0f, 0f, 0f,
                    MachineRadarRenderer::dishQuads, MachineRadarRenderer::sizeKey,
                    MachineRadarRenderer::dishSegmentStart,
                    be -> 0.0,
                    MachineRadarRenderer::dishDuration,
                    MachineSpec.JointNode.rotate(0f, 1f, 0f, 0.5f, 0f, 0.5f,
                            // Never called - the node is tracked (its value comes
                            // from DISH_SPIN_TRACK).
                            (be, pt, gt, out, off) -> true,
                            DISH_SPIN_TRACK),
                    MachineSpec.JointNode.translate(1f, 0f, 0f,
                            (be, pt, gt, out, off) -> {
                                out[off] = isLarge(be) ? 0f : SMALL_DISH_PIVOT_OFFSET_X;
                                return true;
                            }))
            .blockTransform(MachineRadarRenderer::applyBlockTransform)
            .register();
    }

    private MachineRadarRenderer() {}

    private static @Nullable MachineRadarBakedModel radarModel(MachineRadarBlockEntity be) {
        BakedModel raw = Minecraft.getInstance().getBlockRenderer()
                .getBlockModel(be.getBlockState());
        raw = AbstractPartBasedRenderer.unwrapFabricForwardingModels(raw);
        if (raw instanceof MachineRadarBakedModel model) return model;
        if (!loggedModelResolveFailure) {
            loggedModelResolveFailure = true;
            MainRegistry.LOGGER.error(
                    "MachineRadarRenderer: block model is not MachineRadarBakedModel ({}), radar invisible under VBO",
                    raw == null ? "null" : raw.getClass().getName());
        }
        return null;
    }

    private static boolean isLarge(MachineRadarBlockEntity be) {
        MachineRadarBakedModel model = radarModel(be);
        return model != null && model.isLargeRadar();
    }

    private static String sizeKey(MachineRadarBlockEntity be) {
        return isLarge(be) ? "large" : "small";
    }

    private static List<BakedQuad> baseQuads(MachineRadarBlockEntity be) {
        MachineRadarBakedModel model = radarModel(be);
        if (model == null) return List.of();
        BakedModel part = model.getPart(model.getStaticPartName());
        if (part == null) return List.of();
        String cacheKey = "radar:" + sizeKey(be) + "_base";
        return MeshRenderCache.getOrCompilePartGeometry(cacheKey, part).solidQuads();
    }

    private static List<BakedQuad> dishQuads(MachineRadarBlockEntity be) {
        MachineRadarBakedModel model = radarModel(be);
        if (model == null) return List.of();
        BakedModel part = model.getPart("Dish");
        if (part == null) return List.of();
        String cacheKey = "radar:" + sizeKey(be) + "_dish";
        return MeshRenderCache.getOrCompilePartGeometry(cacheKey, part).solidQuads();
    }

    // -- Dish tracked-rig epoch/header (pure in the synced BE state) ------

    /** Legacy spin: {@code rotation += 5F} per powered tick -> 360/5 = 72 ticks per revolution. */
    private static final double DISH_TICKS_PER_REV = 72.0;
    /** Tracked spin node: one full revolution per cycle, opposite the legacy +rotation
     * (the legacy animator applied {@code rotateY(-angle)}). */
    private static final MachineSpec.Track DISH_SPIN_TRACK = MachineSpec.Track.linear(-360f, 0f);

    private static boolean dishPowered(MachineRadarBlockEntity be) {
        return be.isActive() || be.getEnergyStored() > 0;
    }

    /** Segment start tick (level.getGameTime() domain) - the machine epoch. */
    private static long dishSegmentStart(MachineRadarBlockEntity be) {
        return dishPowered(be) ? 1L : 0L;
    }

    /** Segment duration in ticks; a decoded 0 freezes u at u0 (dish parked at rest). */
    private static double dishDuration(MachineRadarBlockEntity be) {
        return dishPowered(be) ? DISH_TICKS_PER_REV : 0.0;
    }

    /** Block transform (verbatim from legacy applyFacingRotation). */
    private static void applyBlockTransform(MachineRadarBlockEntity be, LegacyAnimator animator) {
        BlockState state = be.getBlockState();
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) return;
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        float rot = switch (facing) {
            case SOUTH -> 180F;
            case EAST -> 270F;
            case WEST -> 90F;
            default -> 0F;
        };
        if (rot != 0F) {
            animator.translate(0.5, 0, 0.5);
            animator.rotate(rot, 0, 1, 0);
            animator.translate(-0.5, 0, -0.5);
        }
    }
}
