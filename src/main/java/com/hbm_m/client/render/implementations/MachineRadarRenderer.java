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
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Радар (малый и крупный) на фабрике {@link MachineRenderers}: Base — статика,
 * Dish — анимированная часть; геометрия обеих зависит от варианта модели
 * (MachineRadarBakedModel.isLargeRadar), поэтому обе объявлены динамическими
 * частями с per-BE ключом кеша (small/large). Вращение тарелки — по
 * prevRotation/rotation при наличии энергии.
 */
public final class MachineRadarRenderer {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    /** Смещение пивота тарелки малого радара (оригинал {@code glTranslated(-0.125, 0, 0)}). */
    private static final float SMALL_DISH_PIVOT_OFFSET_X = -0.125F;

    private static volatile boolean loggedModelResolveFailure;

    public static void register() {
        MachineRenderers.machine("radar", ModBlockEntities.RADAR_BE.get(), MachineRadarBlockEntity.class)
            .dynamicPart("Base", MachineRadarRenderer::baseQuads, MachineRadarRenderer::sizeKey)
            .dynamicPart("Dish", MachineRadarRenderer::animateDish,
                    MachineRadarRenderer::dishQuads, MachineRadarRenderer::sizeKey)
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

    private static boolean animateDish(MachineRadarBlockEntity be, float partialTick,
                                       long gameTime, PoseStack pose) {
        MachineRadarBakedModel model = radarModel(be);
        if (model == null) return false;
        boolean large = model.isLargeRadar();
        boolean powered = be.isActive() || be.getEnergyStored() > 0;
        float angle = powered ? Mth.lerp(partialTick, be.prevRotation, be.rotation) : be.rotation;

        pose.last().pose().mul(new Matrix4f()
                .translate(0.5f, 0f, 0.5f)
                .rotateY(-angle * DEG_TO_RAD)
                .translate(large ? 0f : SMALL_DISH_PIVOT_OFFSET_X, 0f, 0f)
                .translate(-0.5f, 0f, -0.5f));
        return true;
    }

    /** Трансформ блока (вербатим легаси applyFacingRotation). */
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
