package com.hbm_m.client.render.implementations;

import org.joml.Vector3f;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.pile.PileControlBlockEntity;
import com.hbm_m.blockentity.machines.pile.PileLoaderBlockEntity;
import com.hbm_m.blockentity.machines.pile.PileVentBlockEntity;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;

/**
 * Nucleus renderers for Chicago Pile Devices:
 * 1. Pile Control: static base with GPU translating control rod (translatingPart, 0..0.75 blocks).
 * 2. Pile Loader: static base with GPU rotating lever (0..90 deg around Z) and translating
 *    fuel carriage slider (translatingPart, 0..0.5 blocks), plus conditional fuel rod.
 * 3. Pile Vent: static base with GPU rotateY continuous blower fan and steam venting hook.
 */
public final class MachinePileRenderers {

    public static void register() {
        // ── Pile Control ─────────────────────────────────────────────────────
        MachineRenderers.machine("pile_control",
                ModBlockEntities.PILE_CONTROL_BE.get(),
                PileControlBlockEntity.class)
            .part("Base")
            // Vertical control rod insertion / extraction translation
            .translatingPart("Rod", new Vector3f(0f, 1f, 0f),
                    (be, pt) -> (float) (be.getExtension() * 0.75))
            .itemParts("Base", "Rod")
            .register();

        // ── Pile Loader ──────────────────────────────────────────────────────
        MachineRenderers.machine("pile_loader",
                ModBlockEntities.PILE_LOADER_BE.get(),
                PileLoaderBlockEntity.class)
            .part("Loader")
            // Loading lever tilting up to 90 degrees around Z at pivot (-0.1875, 0.5, 0)
            .rotatingPart("Lever", new Vector3f(0f, 0f, 1f), new Vector3f(-0.1875f, 0.5f, 0f),
                    (be, pt) -> be.getExtension() * 90.0)
            // Fuel carriage sliding 0.5 blocks along -X into the channel
            .translatingPart("Slider", new Vector3f(-1f, 0f, 0f),
                    (be, pt) -> (float) (be.getExtension() * 0.5))
            // Rod rendered only when a fuel element is loaded, translating along with carriage
            .conditionalPart("Rod", (be, pt, gt, ps) -> {
                if (be.getStack().isEmpty()) return false;
                float dist = (float) (be.getExtension() * -0.5);
                ps.translate(dist, 0.0F, 0.0F);
                return true;
            }, be -> !be.getStack().isEmpty())
            .itemParts("Loader", "Lever", "Slider")
            .register();

        // ── Pile Vent ────────────────────────────────────────────────────────
        MachineRenderers.machine("pile_vent",
                ModBlockEntities.PILE_VENT_BE.get(),
                PileVentBlockEntity.class)
            .part("Pipe")
            // Forced ventilation blower fan rotating about Y axis
            .rotateY("Fan", 0.5f, 0.5f,
                    (be, pt) -> Mth.lerp(pt, be.getLastFan(), be.getFan()))
            .hook(new MachineRenderHook<PileVentBlockEntity>() {
                @Override
                public void render(PileVentBlockEntity be, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    if (be.isActive() && be.getLevel() != null && be.getLevel().random.nextInt(3) == 0) {
                        var pos = be.getBlockPos();
                        be.getLevel().addParticle(ParticleTypes.CLOUD,
                                pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                                0.0, 0.1, 0.0);
                    }
                }

                @Override
                public boolean mayDrawContent(PileVentBlockEntity be) {
                    return be.isActive();
                }
            })
            .itemParts("Pipe", "Fan")
            .register();
    }

    private MachinePileRenderers() {}
}
