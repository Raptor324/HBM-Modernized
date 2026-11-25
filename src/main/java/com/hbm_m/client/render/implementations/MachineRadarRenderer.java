package com.hbm_m.client.render.implementations;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineRadarBlockEntity;
import com.hbm_m.client.model.AbstractMultipartBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpec;
import com.hbm_m.main.MainRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class MachineRadarRenderer {

    /** Dish pivot offset for the small radar (original {@code glTranslated(-0.125, 0, 0)}). */
    private static final float SMALL_DISH_PIVOT_OFFSET_X = -0.125F;

    private static volatile boolean loggedModelResolveFailure;

    public static void register() {
        MachineRenderers.machine("radar", ModBlockEntities.RADAR_BE.get(), MachineRadarBlockEntity.class)
            .dynamicPart("Base", MachineRadarRenderer::baseQuads, MachineRadarRenderer::sizeKey)
            .trackedChainPart("Dish", "Dish",
                    0f, 0f, 0f, 0f,
                    MachineRadarRenderer::dishQuads, MachineRadarRenderer::sizeKey,
                    MachineRadarRenderer::dishSegmentStart,
                    be -> 0.0,
                    MachineRadarRenderer::dishDuration,
                    MachineSpec.JointNode.rotate(0f, 1f, 0f, 0.5f, 0f, 0.5f,
                            (be, pt, gt, out, off) -> true,
                            DISH_SPIN_TRACK),
                    MachineSpec.JointNode.translate(1f, 0f, 0f,
                            (be, pt, gt, out, off) -> {
                                out[off] = isLarge(be) ? 0f : SMALL_DISH_PIVOT_OFFSET_X;
                                return true;
                            }))
            .blockTransform(MachineRadarRenderer::applyBlockTransform)
            .itemParts("Base", "Radar", "Dish")
            .register();
    }

    private MachineRadarRenderer() {}

    private static @Nullable AbstractMultipartBakedModel radarModel(MachineRadarBlockEntity be) {
        BakedModel raw = Minecraft.getInstance().getBlockRenderer()
                .getBlockModel(be.getBlockState());
        raw = AbstractPartBasedRenderer.unwrapFabricForwardingModels(raw);
        if (raw instanceof AbstractMultipartBakedModel model) return model;
        if (!loggedModelResolveFailure) {
            loggedModelResolveFailure = true;
            MainRegistry.LOGGER.error(
                    "MachineRadarRenderer: block model is not AbstractMultipartBakedModel ({}), radar invisible under VBO",
                    raw == null ? "null" : raw.getClass().getName());
        }
        return null;
    }

    private static boolean isLarge(MachineRadarBlockEntity be) {
        return be.isLargeRadar();
    }

    private static String sizeKey(MachineRadarBlockEntity be) {
        return isLarge(be) ? "large" : "small";
    }

    private static List<BakedQuad> baseQuads(MachineRadarBlockEntity be) {
        AbstractMultipartBakedModel model = radarModel(be);
        if (model == null) return List.of();
        BakedModel part = model.getPart(isLarge(be) ? "Radar" : "Base");
        if (part == null) return List.of();
        String cacheKey = "radar:" + sizeKey(be) + "_base";
        return MeshRenderCache.getOrCompilePartGeometry(cacheKey, part).solidQuads();
    }

    private static List<BakedQuad> dishQuads(MachineRadarBlockEntity be) {
        AbstractMultipartBakedModel model = radarModel(be);
        if (model == null) return List.of();
        BakedModel part = model.getPart("Dish");
        if (part == null) return List.of();
        String cacheKey = "radar:" + sizeKey(be) + "_dish";
        return MeshRenderCache.getOrCompilePartGeometry(cacheKey, part).solidQuads();
    }

    private static final double DISH_TICKS_PER_REV = 72.0;
    private static final MachineSpec.Track DISH_SPIN_TRACK = MachineSpec.Track.linear(-360f, 0f);

    private static boolean dishPowered(MachineRadarBlockEntity be) {
        return be.isActive() || be.getEnergyStored() > 0;
    }

    private static long dishSegmentStart(MachineRadarBlockEntity be) {
        return dishPowered(be) ? 1L : 0L;
    }

    private static double dishDuration(MachineRadarBlockEntity be) {
        return dishPowered(be) ? DISH_TICKS_PER_REV : 0.0;
    }

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
