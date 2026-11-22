package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineCyclotronBlockEntity;
import com.hbm_m.blockentity.machines.albion.PABeamlineBlockEntity;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.util.BeamPronter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Nucleus renderers for Particle Accelerators:
 * 1. Cyclotron: body, 4 plug sockets, rotating particle dee joint, and orbiting
 *    Standard Galactic inscription hook ("plures necat crapula quam gladius").
 * 2. PA Beamline: regular pipe vs windowed pipe with glowing glass and continuous
 *    accelerator ion beam hook.
 */
public final class MachineAcceleratorRenderer {

    private static final Style GALACTIC = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath("minecraft", "alt"));
    private static final String INSCRIPTION = "plures necat crapula quam gladius";

    public static void register() {
        // ── Cyclotron ────────────────────────────────────────────────────────
        MachineRenderers.machine("cyclotron",
                ModBlockEntities.CYCLOTRON_BE.get(),
                MachineCyclotronBlockEntity.class)
            .part("Body")
            .conditionalPart("B1", be -> be.getPlug(0))
            .conditionalPart("B2", be -> be.getPlug(1))
            .conditionalPart("B3", be -> be.getPlug(2))
            .conditionalPart("B4", be -> be.getPlug(3))
            .hook(new MachineRenderHook<MachineCyclotronBlockEntity>() {
                @Override
                public void render(MachineCyclotronBlockEntity be, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    boolean plugged = be.getPlug(0) && be.getPlug(1) && be.getPlug(2) && be.getPlug(3);
                    if (plugged) {
                        ps.pushPose();
                        ps.mulPose(Axis.YP.rotationDegrees((float) (System.currentTimeMillis() * 0.025 % 360)));
                        ps.translate(0, 2, 0);
                        ps.mulPose(Axis.XP.rotationDegrees(180));
                        float rot = 0F;
                        Font font = Minecraft.getInstance().font;

                        for (char c : INSCRIPTION.toCharArray()) {
                            ps.pushPose();
                            ps.mulPose(Axis.YP.rotationDegrees(rot));
                            rot -= font.width(String.valueOf(c)) * 2F;
                            ps.translate(2.75, 0, 0);
                            ps.mulPose(Axis.YP.rotationDegrees(-90));
                            float scale = 0.1F;
                            ps.scale(scale, scale, scale);
                            font.drawInBatch(Component.literal(String.valueOf(c)).withStyle(GALACTIC), 0, 0, 0x600060, false,
                                    ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
                            ps.popPose();
                        }
                        ps.popPose();
                    }

                    // Dee ionization glow
                    if (be.getEnergyStored() > 0) {
                        long t = System.currentTimeMillis();
                        ps.pushPose();
                        ps.translate(0, 0.8, 0);
                        BeamPronter.prontBeam(ps, buf, new Vec3(0.3, 0, 0.3),
                                BeamPronter.EnumWaveType.SPIRAL, BeamPronter.EnumBeamType.LINE,
                                0x4080ff, 0x00ffff, (int) (t % 360), 4, 0.05F, 1, 0,
                                BeamPronter.forkAlpha());
                        ps.popPose();
                    }
                }

                @Override
                public boolean mayDrawContent(MachineCyclotronBlockEntity be) {
                    return (be.getPlug(0) && be.getPlug(1) && be.getPlug(2) && be.getPlug(3))
                            || be.getEnergyStored() > 0;
                }
            })
            .itemParts("Body")
            .register();

        // ── PA Beamline ──────────────────────────────────────────────────────
        MachineRenderers.machine("pa_beamline",
                ModBlockEntities.PA_BEAMLINE_BE.get(),
                PABeamlineBlockEntity.class)
            .conditionalPart("Beamline", be -> !be.hasWindow())
            .conditionalPart("BeamlineWindow", be -> be.hasWindow())
            .conditionalPart("BeamlineGlass", be -> be.hasWindow())
            .lightOverride("BeamlineGlass", be -> 0x00F000F0)
            .hook(new MachineRenderHook<PABeamlineBlockEntity>() {
                @Override
                public void render(PABeamlineBlockEntity be, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    ps.pushPose();
                    ps.translate(0, 0.5, 0);
                    long t = System.currentTimeMillis();
                    BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, 1.0),
                            BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.LINE,
                            0x00e1ff, 0xffffff, (int) (t % 1000), 2, 0.04F, 0, 0,
                            BeamPronter.forkAlpha());
                    ps.popPose();
                }

                @Override
                public boolean mayDrawContent(PABeamlineBlockEntity be) {
                    return be.hasWindow();
                }
            })
            .itemParts("Beamline")
            .register();
    }

    private MachineAcceleratorRenderer() {}
}
