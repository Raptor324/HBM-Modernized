package com.hbm_m.client.render.implementations;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.api.fluids.IFluidConnectorBlock;
import com.hbm_m.api.fluids.IFluidConnectorMK2;
import com.hbm_m.api.fluids.IFluidPipeMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.blockentity.machines.BarrelAntimatterBlockEntity;
import com.hbm_m.blockentity.machines.BarrelCorrodedBlockEntity;
import com.hbm_m.blockentity.machines.BarrelIronBlockEntity;
import com.hbm_m.blockentity.machines.BarrelPlasticBlockEntity;
import com.hbm_m.blockentity.machines.BarrelSteelBlockEntity;
import com.hbm_m.blockentity.machines.BarrelTcalloyBlockEntity;
import com.hbm_m.blockentity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.client.render.util.DiamondPronter;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.platform.FluidHooks;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import com.mojang.blaze3d.systems.RenderSystem;
import com.hbm_m.client.render.machine.MachineRenderApi;

/**
 * Port of 1.7.10 {@code RenderFluidBarrel}: connector nozzles on the faces where a neighbor
 * accepts the current fluid, and NFPA diamonds on the 4 side faces -- diamonds only when
 * a fluid with hazard data is set (not NONE/empty). The barrel body (part "Barrel") is rendered
 * by the spec through a VBO; the connector (part "Connector") is drawn immediate from the part's baked quads.
 * <p>
 * 1:1 calibration: the original TESR translated to the block center T(0.5,0.5,0.5), the connector
 * was additionally centered with T(0,-0.5,0); diamonds T(0.4,0.30,-0.24) * scale(1,0.25,0.25),
 * 4x rotY(90). The spec declares a blockTransform T(0.5, 0.0, 0.5), so diamonds need T(0.0, 0.5, 0.0)
 * to reach block vertical center, while connectors rotate directly around Y.
 */
public final class BarrelTankRenderer {

    private static final RandomSource RANDOM = RandomSource.create(42);

    private BarrelTankRenderer() {}

    public static void register() {
        spec("barrel_iron", ModBlockEntities.BARREL_IRON_BE.get(), BarrelIronBlockEntity.class);
        spec("barrel_plastic", ModBlockEntities.BARREL_PLASTIC_BE.get(), BarrelPlasticBlockEntity.class);
        spec("barrel_steel", ModBlockEntities.BARREL_STEEL_BE.get(), BarrelSteelBlockEntity.class);
        spec("barrel_tcalloy", ModBlockEntities.BARREL_TCALLOY_BE.get(), BarrelTcalloyBlockEntity.class);
        spec("barrel_antimatter", ModBlockEntities.BARREL_ANTIMATTER_BE.get(), BarrelAntimatterBlockEntity.class);
        spec("barrel_corroded", ModBlockEntities.BARREL_CORRODED_BE.get(), BarrelCorrodedBlockEntity.class);
    }

    private static <T extends MachineFluidTankBlockEntity> void spec(
            String id, BlockEntityType<T> type, Class<T> cls) {
        MachineRenderers.machine(id, type, cls)
            .blockTransform((be, animator) -> animator.translate(0.5, 0.0, 0.5))
            .part("Barrel")
            .hook(BarrelTankRenderer::renderExtras)
            .chunkRenderTypes(RenderType.cutoutMipped())
            .itemParts("Barrel")
            .register();
    }

    private static void renderExtras(MachineFluidTankBlockEntity be, float partialTick,
                                     PoseStack poseStack, MultiBufferSource buffer,
                                     int packedLight, int packedOverlay, MachineRenderApi api) {
        // Do not draw immediate content during shader shadow pass.
        if (com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isRenderingShadowPass()) {
            return;
        }
        Level level = be.getLevel();
        if (level == null) return;

        Fluid fluid = be.getFluidTank().getTankType();
        boolean hasFluid = fluid != null && fluid != Fluids.EMPTY && fluid != ModFluids.NONE.getSource();

        // Parity with NEXT RenderBarrel.extractRenderState (hasFluid = NTMFluidProperties.get(type) != null) + submit():
        // hazard diamonds are rendered only for fluids with NTM data. forFluid computes a default type for any fluid,
        // so presence of fluid alone is insufficient; check actual diamond data.
        RenderSystem.disableCull();
        try {
            if (hasFluid) {
                FluidType type = FluidType.forFluid(fluid);
                if (type.hasHazardDiamond()) {
                    renderDiamonds(be, poseStack, buffer, type, packedOverlay);
                }
            }
            renderConnectors(be, level, poseStack, buffer, fluid, packedLight, packedOverlay);
        } finally {
            RenderSystem.enableCull();
        }
    }

    /** NFPA diamonds: 1:1 RenderFluidBarrel -- T(center) * [T(0.4,0.30,-0.24) * scale(1,0.25,0.25) * pront * rotY90] x4. */
    private static void renderDiamonds(MachineFluidTankBlockEntity be, PoseStack poseStack,
                                       MultiBufferSource buffer, FluidType type, int packedOverlay) {
        int light = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos());
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5F, 0.0F);
        for (int j = 0; j < 4; j++) {
            poseStack.pushPose();
            poseStack.translate(0.4F, 0.30F, -0.24F);
            poseStack.scale(1.0F, 0.25F, 0.25F);
            DiamondPronter.pront(poseStack, buffer, type.poison, type.flammability, type.reactivity,
                    type.symbol, light, packedOverlay);
            poseStack.popPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        poseStack.popPose();
    }

    /**
     * Nozzles on the faces where a neighboring connector accepts the current fluid.
     * Original: +X rot 0, -X rot 180, -Z rot 90, +Z rot -90; the connector is centered with T(0,-0.5,0)
     * in the block-center frame.
     */
    private static void renderConnectors(MachineFluidTankBlockEntity be, Level level,
                                         PoseStack poseStack, MultiBufferSource buffer, Fluid fluid,
                                         int packedLight, int packedOverlay) {
        BakedModel connector = getConnectorPart(be);
        if (connector == null) return;

        List<net.minecraft.client.renderer.block.model.BakedQuad> quads = collectQuads(connector);
        if (quads.isEmpty()) return;

        VertexConsumer vc = buffer.getBuffer(RenderType.cutout());
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (!neighborAccepts(be, level, dir, fluid)) continue;
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(rotationDegrees(dir)));
            var pose = poseStack.last();
            for (net.minecraft.client.renderer.block.model.BakedQuad quad : quads) {
                RenderHooks.putBulkDataShaded(vc, pose, quad, 1.0F, packedLight, packedOverlay, false);
            }
            poseStack.popPose();
        }
        if (buffer instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(RenderType.cutout());
        }
    }

    private static float rotationDegrees(Direction dir) {
        return switch (dir) {
            case EAST -> 0.0F;
            case WEST -> 180.0F;
            case NORTH -> 90.0F;
            case SOUTH -> -90.0F;
            default -> 0.0F;
        };
    }

    /**
     * Parity with Library.canConnectFluid: the neighbor connector accepts this fluid facing toward us;
     * non-MK2 neighbors fall back to the platform fluid handler on that face (plain fluid-capable blocks).
     */
    private static boolean neighborAccepts(MachineFluidTankBlockEntity be, Level level,
                                           Direction dir, Fluid fluid) {
        BlockPos neighborPos = be.getBlockPos().relative(dir);
        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof IFluidConnectorBlock conBlock) {
            return conBlock.canConnect(fluid != null ? fluid : Fluids.EMPTY, level, neighborPos, dir.getOpposite());
        }
        BlockEntity neighbor = level.getBlockEntity(neighborPos);
        if (neighbor instanceof IFluidPipeMK2 pipe) {
            Fluid pipeFluid = pipe.getFluidType();
            if (pipeFluid == null || pipeFluid == Fluids.EMPTY || pipeFluid == ModFluids.NONE.getSource()) {
                return true;
            }
            if (fluid == null || fluid == Fluids.EMPTY || fluid == ModFluids.NONE.getSource()) {
                return true;
            }
            return VanillaFluidEquivalence.sameSubstance(fluid, pipeFluid);
        }
        if (neighbor instanceof IFluidConnectorMK2 con) {
            if (fluid != null && fluid != Fluids.EMPTY && fluid != ModFluids.NONE.getSource()) {
                return con.canConnect(fluid, dir.getOpposite());
            }
            return con.canConnect(Fluids.EMPTY, dir.getOpposite());
        }
        // Library.canConnectFluid fallback: any neighbor with a fluid handler on this face connects.
        return FluidHooks.getBlockFluidHandler(level, neighborPos, dir.getOpposite()) != null;
    }

    private static BakedModel getConnectorPart(MachineFluidTankBlockEntity be) {
        BlockState defaultState = be.getBlockState().getBlock().defaultBlockState();
        BakedModel raw = Minecraft.getInstance().getBlockRenderer().getBlockModel(defaultState);
        // Unwrap FRAPI/CTM forwarding models first, otherwise instanceof check fails with texture packs like Continuity.
        if (!(com.hbm_m.client.render.AbstractPartBasedRenderer.unwrapFabricForwardingModels(raw)
                instanceof com.hbm_m.client.model.ConfiguredMultipartBakedModel model)) {
            return null;
        }
        return model.getPart("Connector");
    }

    private static List<net.minecraft.client.renderer.block.model.BakedQuad> collectQuads(BakedModel part) {
        List<net.minecraft.client.renderer.block.model.BakedQuad> quads = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            quads.addAll(RenderHooks.getModelQuads(part, null, dir, RANDOM, null));
        }
        quads.addAll(RenderHooks.getModelQuads(part, null, null, RANDOM, null));
        return quads;
    }
}
