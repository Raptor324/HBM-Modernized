package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachineArcFurnaceBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineArcFurnaceBlockEntity;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.machine.MachineRenderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Direction;

/**
 * Arc furnace on the {@link MachineRenderers} factory — port of {@code RenderArcFurnace} (1.7.10):
 * the Lid lifts by {@code 2 * lift} with a small sine shake while progressing, the Cables swing
 * ±30° around their y = 5.5 pivots while progressing, the Rings/Electrodes/Cables are gated
 * per electrode slot (FRESH/USED fullbright/DEPLETED fullbright/NONE) and the molten contents
 * switch between ContentsCold (no liquids, material inside) and ContentsHot (fullbright, rising
 * with the liquid amount). Facing table 1:1 with the original TESR: NORTH 90 / SOUTH 270 /
 * WEST 180 / EAST 0 degrees.
 */
public final class MachineArcFurnaceRenderer {

    public static void register() {
        MachineRenderers.machine("arcfurnace", ModBlockEntities.ARC_FURNACE_BE.get(),
                MachineArcFurnaceBlockEntity.class)
            // Original fullbright(true)-Bloecke: ContentsHot und gebrauchte/verbrauchte Elektroden.
            .lightOverride("ContentsHot", be -> LightTexture.FULL_BRIGHT)
            .lightOverride("Electrode1Hot", be -> LightTexture.FULL_BRIGHT)
            .lightOverride("Electrode2Hot", be -> LightTexture.FULL_BRIGHT)
            .lightOverride("Electrode3Hot", be -> LightTexture.FULL_BRIGHT)
            .lightOverride("Electrode1Short", be -> LightTexture.FULL_BRIGHT)
            .lightOverride("Electrode2Short", be -> LightTexture.FULL_BRIGHT)
            .lightOverride("Electrode3Short", be -> LightTexture.FULL_BRIGHT)
            .part("Furnace")
            .staticPart("Lid", MachineArcFurnaceRenderer::animateLid)
            .staticPart("ContentsHot", MachineArcFurnaceRenderer::animateContentsHot)
            .staticPart("ContentsCold", MachineArcFurnaceRenderer::animateContentsCold)
            // RingN haengt an Elektrodenplatz N (original: nur wenn electrodes[N] != NONE).
            .conditionalPart("Ring1", slotNotEmpty(0))
            .conditionalPart("Ring2", slotNotEmpty(1))
            .conditionalPart("Ring3", slotNotEmpty(2))
            // ElectrodeN nur im FRESH-Zustand (original: == ELECTRODE_FRESH).
            .conditionalPart("Electrode1", slotState(0, MachineArcFurnaceBlockEntity.ELECTRODE_FRESH))
            .conditionalPart("Electrode2", slotState(1, MachineArcFurnaceBlockEntity.ELECTRODE_FRESH))
            .conditionalPart("Electrode3", slotState(2, MachineArcFurnaceBlockEntity.ELECTRODE_FRESH))
            // Hot/Short-Varianten (fullbright ueber lightOverride oben).
            .conditionalPart("Electrode1Hot", slotState(0, MachineArcFurnaceBlockEntity.ELECTRODE_USED))
            .conditionalPart("Electrode2Hot", slotState(1, MachineArcFurnaceBlockEntity.ELECTRODE_USED))
            .conditionalPart("Electrode3Hot", slotState(2, MachineArcFurnaceBlockEntity.ELECTRODE_USED))
            .conditionalPart("Electrode1Short", slotState(0, MachineArcFurnaceBlockEntity.ELECTRODE_DEPLETED))
            .conditionalPart("Electrode2Short", slotState(1, MachineArcFurnaceBlockEntity.ELECTRODE_DEPLETED))
            .conditionalPart("Electrode3Short", slotState(2, MachineArcFurnaceBlockEntity.ELECTRODE_DEPLETED))
            // Cables schwingen ±30° um y = 5.5 (z-Offsets 0.5 / 0 / -0.5 wie im Original).
            .conditionalPart("Cable1", cableAnimator(0.5), slotNotEmpty(0))
            .conditionalPart("Cable2", cableAnimator(0.0), slotNotEmpty(1))
            .conditionalPart("Cable3", cableAnimator(-0.5), slotNotEmpty(2))
            // Original ItemRenderBase: Furnace/Lid/Rings/Electrode1-3/Cables, keine Contents/Hot/Short.
            .itemParts("Furnace", "Lid", "Ring1", "Ring2", "Ring3",
                    "Electrode1", "Electrode2", "Electrode3",
                    "Cable1", "Cable2", "Cable3")
            .blockTransform(MachineArcFurnaceRenderer::applyBlockTransform)
            .register();
    }

    private MachineArcFurnaceRenderer() {}

    // ── Block transform / facing ─────────────────────────────────────────────

    /**
     * 1:1 facing table of {@code RenderArcFurnace} (metadata - 10 = ForgeDirection):
     * NORTH(2) -&gt; 90, SOUTH(3) -&gt; 270, WEST(4) -&gt; 180, EAST(5) -&gt; 0 degrees.
     * Der Pour-Offset im TE nutzt dieselbe Facing-Richtung wie das Modellfront.
     */
    private static void applyBlockTransform(MachineArcFurnaceBlockEntity be, LegacyAnimator animator) {
        Direction facing = be.getBlockState().getValue(MachineArcFurnaceBlock.FACING);
        float facingYRot = switch (facing) {
            case NORTH -> 90F;
            case SOUTH -> 270F;
            case WEST -> 180F;
            default -> 0F; // EAST
        };
        animator.translate(0.5, 0.0, 0.5);
        animator.rotate(facingYRot, 0, 1, 0);
        animator.translate(-0.5, 0.0, -0.5);
    }

    // ── Animators (1:1 RenderArcFurnace.renderTileEntityAt) ──────────────────

    /** Lid: T(0, 2 * lift, 0) plus Schuetteln sin(totalTime) * 0.005 auf z, solange geschmolzen wird. */
    private static boolean animateLid(MachineArcFurnaceBlockEntity be, float partialTick,
                                      long gameTime, PoseStack pose) {
        float lift = be.prevLid + (be.lid - be.prevLid) * partialTick;
        pose.translate(0, 2 * lift, 0);
        if (be.isProgressing) pose.translate(0, 0, Math.sin(gameTime + partialTick) * 0.005);
        return true;
    }

    /** ContentsHot: nur bei Schmelze im Kessel, Anhebung mit der Fuellmenge (-1.75 .. 0). */
    private static boolean animateContentsHot(MachineArcFurnaceBlockEntity be, float partialTick,
                                              long gameTime, PoseStack pose) {
        if (be.liquids.isEmpty()) return false;
        pose.translate(0, -1.75 + MachineArcFurnaceBlockEntity.getStackAmount(be.liquids) * 1.75
                / MachineArcFurnaceBlockEntity.maxLiquid, 0);
        return true;
    }

    /** ContentsCold: nur bei leerem Kessel mit Material darin (original: hasMaterial). */
    private static boolean animateContentsCold(MachineArcFurnaceBlockEntity be, float partialTick,
                                               long gameTime, PoseStack pose) {
        return be.liquids.isEmpty() && be.hasMaterial;
    }

    /** Cable: R_x(sin(totalTime / 2) * 30) um das Pivot (0, 5.5, zOff), solange geschmolzen wird. */
    private static boolean animateCable(MachineArcFurnaceBlockEntity be, float partialTick,
                                        long gameTime, PoseStack pose, double zOff) {
        pose.translate(0, 5.5, zOff);
        if (be.isProgressing) pose.mulPose(Axis.XP.rotationDegrees((float) (Math.sin((gameTime + partialTick) / 2) * 30)));
        pose.translate(0, -5.5, -zOff);
        return true;
    }

    private static com.hbm_m.client.render.machine.PartAnimator<MachineArcFurnaceBlockEntity> cableAnimator(double zOff) {
        return (be, partialTick, gameTime, pose) -> animateCable(be, partialTick, gameTime, pose, zOff);
    }

    private static java.util.function.Predicate<MachineArcFurnaceBlockEntity> slotNotEmpty(final int slot) {
        return be -> be.electrodes[slot] != MachineArcFurnaceBlockEntity.ELECTRODE_NONE;
    }

    private static java.util.function.Predicate<MachineArcFurnaceBlockEntity> slotState(final int slot, final byte state) {
        return be -> be.electrodes[slot] == state;
    }
}
