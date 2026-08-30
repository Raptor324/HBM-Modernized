package com.hbm_m.client.render.implementations;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineArcFurnaceBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineArcFurnaceBlockEntity;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.PlatformHooks;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Дуговая печь на фабрике {@link MachineRenderers}: электроды переключаются между
 * "холодной" и "горячей" injected-моделью по состоянию BE (аналог Hot/Cold-групп
 * оригинального arc_furnace.obj). Статические части (Furnace/Lid/Ring/Cable) живут
 * в обычном чанк-меше блока. Без вращения — чистый state-swap.
 */
public final class MachineArcFurnaceRenderer {

    private static final ResourceLocation COLD_MODEL_ID = id("arc_furnace_electrodes_cold");
    private static final ResourceLocation HOT_MODEL_ID = id("arc_furnace_electrodes_hot");

    public static void register() {
        MachineRenderers.machine("arcfurnace", ModBlockEntities.ARC_FURNACE_BE.get(),
                MachineArcFurnaceBlockEntity.class)
            .dynamicPart("Electrodes", MachineArcFurnaceRenderer::electrodeQuads,
                    be -> be.isActive() ? "hot" : "cold")
            .blockTransform(MachineArcFurnaceRenderer::applyBlockTransform)
            .register();
    }

    private MachineArcFurnaceRenderer() {}

    private static List<net.minecraft.client.renderer.block.model.BakedQuad> electrodeQuads(
            MachineArcFurnaceBlockEntity be) {
        boolean hot = be.isActive();
        BakedModel model = getModel(hot ? HOT_MODEL_ID : COLD_MODEL_ID);
        if (model == null) return List.of();
        String cacheKey = hot ? "arc_furnace/electrodes_hot" : "arc_furnace/electrodes_cold";
        return MeshRenderCache.getOrCompilePartGeometry(cacheKey, model).solidQuads();
    }

    private static void applyBlockTransform(MachineArcFurnaceBlockEntity be, LegacyAnimator animator) {
        Direction facing = be.getBlockState().getValue(MachineArcFurnaceBlock.FACING);
        float facingYRot = switch (facing) {
            case SOUTH -> 180F;
            case WEST -> 270F;
            case EAST -> 90F;
            default -> 0F; // NORTH
        };
        animator.translate(0.5, 0.0, 0.5);
        animator.rotate(facingYRot, 0, 1, 0);
        animator.translate(-0.5, 0.0, -0.5);
    }

    @Nullable
    private static BakedModel getModel(ResourceLocation rid) {
        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = PlatformHooks.getModel(modelManager, rid);
        return (model == null || model == modelManager.getMissingModel()) ? null : model;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/" + path);
    }
}
