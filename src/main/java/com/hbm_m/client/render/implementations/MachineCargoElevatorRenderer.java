package com.hbm_m.client.render.implementations;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.CargoElevatorBlockEntity;
import com.hbm_m.client.model.ConfiguredMultipartBakedModel;
import com.hbm_m.client.model.ModelHelper;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Cargo elevator on the {@link MachineRenderers} factory (replaces the legacy raw BER
 * {@code CargoElevatorRenderer} + the unused {@code CargoElevatorVboRenderer}).
 * <p>
 * Geometry ({@code models/block/cargo_elevator.obj}) is centered on the core cell center
 * (X/Z in -1.5..1.5, Y in -0.25..1.0), so the block transform is a plain
 * {@code T(0.5,0,0.5)} — the shaft has no facing.
 * <p>
 * Parts (1:1 with the original {@code RenderCargoElevator}):
 * <ul>
 *   <li>{@code Base} — static floor slab;</li>
 *   <li>{@code Guides} — one floor's guide posts baked once, repeated {@code height + 1}
 *       times as a dynamic part keyed on {@code height};</li>
 *   <li>{@code Platform} — GPU translating joint on Y by the interpolated {@code extension};</li>
 *   <li>{@code Piston} — dynamic part: {@code floor(ext) + 1} piston segments stacked below
 *       the platform (integer offsets), with a CPU animator carrying the fractional part
 *       {@code ext - floor(ext)} so the stack stays glued to the platform between floors.</li>
 * </ul>
 */
public final class MachineCargoElevatorRenderer {

    private static final RandomSource RANDOM = RandomSource.create(42);

    private MachineCargoElevatorRenderer() {}

    public static void register() {
        MachineRenderers.machine("cargo_elevator", ModBlockEntities.CARGO_ELEVATOR_BE.get(),
                CargoElevatorBlockEntity.class)
            .dynamicPart("Base", MachineCargoElevatorRenderer::baseAnimator,
                    MachineCargoElevatorRenderer::baseQuads,
                    be -> "b" + be.renderPlatform)
            .dynamicPart("Guides", MachineCargoElevatorRenderer::guidesQuads,
                    be -> "h" + be.height)
            .dynamicPart("Platform", MachineCargoElevatorRenderer::platformAnimator,
                    MachineCargoElevatorRenderer::platformQuads,
                    be -> "p" + be.renderPlatform)
            .dynamicPart("Piston", MachineCargoElevatorRenderer::pistonFraction,
                    MachineCargoElevatorRenderer::pistonQuads,
                    be -> "p" + pistonCount(be) + be.renderPlatform)
            .blockTransform((be, animator) -> animator.translate(0.5f, 0.0f, 0.5f))
            .animationEpoch(MachineCargoElevatorRenderer::epoch)
            .chunkRenderTypes(RenderType.cutoutMipped())
            .itemParts("Base", "Guides", "Piston", "Platform")
            .viewDistance(96)
            .register();
    }

    /**
     * Epoch must mix prev AND curr of every animator input (extension drives the Platform
     * joint and the Piston fraction; height keys the Guides geometry). Idle convergence:
     * when the platform rests, {@code extension == prevExtension} — stable.
     */
    private static long epoch(CargoElevatorBlockEntity be) {
        if (be.extension != be.prevExtension) {
            return Long.MIN_VALUE;
        }
        long a = Double.doubleToLongBits(be.extension);
        return a * 31L + be.height;
    }

    /**
     * 1:1 {@code if(elevator.renderPlatform)} in {@code RenderCargoElevator}: the moving assembly
     * (Base/Platform/Piston) is skipped until the server has ticked once, fixing the short
     * flickering platform that instantly despawns.
     */
    private static boolean baseAnimator(CargoElevatorBlockEntity be, float pt, long gameTime, PoseStack pose) {
        return be.renderPlatform;
    }

    private static List<BakedQuad> baseQuads(CargoElevatorBlockEntity be) {
        return be.renderPlatform ? bakedPartQuads(be, "Base") : List.of();
    }

    private static boolean platformAnimator(CargoElevatorBlockEntity be, float pt, long gameTime, PoseStack pose) {
        if (!be.renderPlatform) return false;
        pose.translate(0f, (float) be.getRenderExtension(pt), 0f);
        return true;
    }

    private static List<BakedQuad> platformQuads(CargoElevatorBlockEntity be) {
        return be.renderPlatform ? bakedPartQuads(be, "Platform") : List.of();
    }

    /** CPU translation by the full extension — moves the piston stack with the platform. */
    private static boolean pistonFraction(CargoElevatorBlockEntity be, float pt, long gameTime, PoseStack pose) {
        if (!be.renderPlatform) return false;
        double ext = be.getRenderExtension(pt);
        pose.translate(0f, (float) ext, 0f);
        return true;
    }

    /** One floor's guide posts, repeated for every floor of the shaft (0..height). */
    private static List<BakedQuad> guidesQuads(CargoElevatorBlockEntity be) {
        List<BakedQuad> one = bakedPartQuads(be, "Guides");
        if (one.isEmpty()) return one;
        List<BakedQuad> out = new ArrayList<>(one.size() * (be.height + 1));
        for (int i = 0; i <= be.height; i++) {
            out.addAll(ModelHelper.translateQuads(one, 0f, i, 0f));
        }
        return out;
    }

    /**
     * Exact 1:1 parity with 1.7.10 {@code for(int i = 0; i < extension + 1; i++)}:
     * For extension == 0, 1 segment fills the base. For any extension > 0, an additional
     * segment extends down into the foundation so no holes appear at the base while the
     * platform is rising between floors.
     */
    private static int pistonCount(CargoElevatorBlockEntity be) {
        double maxExt = Math.max(be.extension, be.prevExtension);
        if (maxExt < 1.0e-5) {
            return 1;
        }
        return (int) Math.ceil(maxExt) + 1;
    }

    private static List<BakedQuad> pistonQuads(CargoElevatorBlockEntity be) {
        List<BakedQuad> one = be.renderPlatform ? bakedPartQuads(be, "Piston") : List.of();
        if (one.isEmpty()) return one;
        int count = pistonCount(be);
        List<BakedQuad> out = new ArrayList<>(one.size() * count);
        for (int i = 0; i < count; i++) {
            out.addAll(ModelHelper.translateQuads(one, 0f, -i, 0f));
        }
        return out;
    }

    private static List<BakedQuad> bakedPartQuads(CargoElevatorBlockEntity be, String partName) {
        BakedModel raw = Minecraft.getInstance().getBlockRenderer().getBlockModel(be.getBlockState());
        if (!(raw instanceof ConfiguredMultipartBakedModel model)) return List.of();
        BakedModel part = model.getPart(partName);
        if (part == null) return List.of();
        List<BakedQuad> quads = new ArrayList<>();
        quads.addAll(com.hbm_m.platform.RenderHooks.getModelQuads(part, null, null, RANDOM, null));
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            quads.addAll(com.hbm_m.platform.RenderHooks.getModelQuads(part, null, dir, RANDOM, null));
        }
        return quads;
    }
}
