package com.hbm_m.client.render.implementations;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.api.fluids.IFluidConnectorMK2;
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
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import com.mojang.blaze3d.systems.RenderSystem;
import com.hbm_m.client.render.machine.MachineRenderApi;

/**
 * Port of 1.7.10 {@code RenderFluidBarrel}: connector nozzles on the faces where a neighbor
 * accepts the current fluid, and NFPA diamonds on the 4 side faces -- all only when
 * a fluid type is set (not NONE). The barrel body (part "Barrel") is rendered by the spec
 * through a VBO; the connector (part "Connector") is drawn immediate from the part's baked quads.
 * <p>
 * 1:1 calibration: the original TESR translated to the block center T(0.5,0.5,0.5), the connector
 * was additionally centered with T(0,-0.5,0); diamonds T(0.4,0.30,-0.24) * scale(1,0.25,0.25),
 * 4x rotY(90). The spec declares an identity blockTransform, so the hook works in the raw
 * block frame 0..1.
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
            .blockTransform((be, animator) -> {})
            .part("Barrel")
            .hook(BarrelTankRenderer::renderExtras)
            .chunkRenderTypes(RenderType.cutoutMipped())
            .itemParts("Barrel")
            .register();
    }

    private static void renderExtras(MachineFluidTankBlockEntity be, float partialTick,
                                     PoseStack poseStack, MultiBufferSource buffer,
                                     int packedLight, int packedOverlay, MachineRenderApi api) {
        Fluid fluid = be.getFluidTank().getTankType();
        if (fluid == null || fluid == Fluids.EMPTY || fluid == ModFluids.NONE.getSource()) {
            return;
        }
        FluidType type = FluidType.forFluid(fluid);
        Level level = be.getLevel();
        if (level == null) return;

        RenderSystem.disableCull();
        try {
            renderDiamonds(be, poseStack, buffer, type, packedOverlay);
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
        poseStack.translate(0.5F, 0.5F, 0.5F);
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

        // RenderType.translucent() -- as in MachineCrystallizerRenderer.drawCrystallizerFluidBaked:
        // the MachineRenderers pipeline flushes solid/translucent batches; cutoutMipped is not
        // endBatch'ed in the immediate path, so the geometry simply never appears.
        VertexConsumer vc = buffer.getBuffer(RenderType.translucent());
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (!neighborAccepts(be, level, dir, fluid)) continue;
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(rotationDegrees(dir)));
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            var pose = poseStack.last();
            for (net.minecraft.client.renderer.block.model.BakedQuad quad : quads) {
                RenderHooks.putBulkDataShaded(vc, pose, quad, 1.0F, packedLight, packedOverlay, false);
            }
            poseStack.popPose();
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

    /** Parity with Library.canConnectFluid: the neighbor connector accepts this fluid facing toward us. */
    private static boolean neighborAccepts(MachineFluidTankBlockEntity be, Level level,
                                           Direction dir, Fluid fluid) {
        BlockEntity neighbor = level.getBlockEntity(be.getBlockPos().relative(dir));
        if (!(neighbor instanceof IFluidConnectorMK2 con)) return false;
        return con.canConnect(fluid, dir.getOpposite());
    }

    private static BakedModel getConnectorPart(MachineFluidTankBlockEntity be) {
        BakedModel raw = Minecraft.getInstance().getBlockRenderer().getBlockModel(be.getBlockState());
        if (raw instanceof com.hbm_m.client.model.ConfiguredMultipartBakedModel model) {
            return model.getPart("Connector");
        }
        return null;
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
