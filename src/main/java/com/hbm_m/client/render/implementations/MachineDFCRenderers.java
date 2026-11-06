package com.hbm_m.client.render.implementations;

import java.util.Random;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineCoreEmitterBlockEntity;
import com.hbm_m.blockentity.machines.MachineCoreInjectorBlockEntity;
import com.hbm_m.blockentity.machines.MachineCoreReceiverBlockEntity;
import com.hbm_m.blockentity.machines.dfc.DFCCoreBlockEntity;
import com.hbm_m.blockentity.machines.dfc.DFCStabilizerBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.util.BeamPronter;
import com.hbm_m.client.render.util.RenderSparks;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Nucleus renderers for the Dark Fusion Core (DFC) complex:
 * 1. DFC Emitter: 6-way oriented casing with high-intensity laser excitation beam hook.
 * 2. DFC Receiver: 6-way oriented magnetic receptacle casing.
 * 3. DFC Injector: 6-way oriented casing with fluid injection particle beam hook.
 * 4. DFC Stabilizer: 6-way oriented casing with GPU rotating confinement ring and containment beam hook.
 * 5. DFC Core: Standby spark orb, pulsating catalyst-color plasma sphere, and meltdown solar flare hook.
 */
public final class MachineDFCRenderers {

    static final SimpleObjModel SPHERE_UV = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/sphere_uv.obj"));
    static final SimpleObjModel SPHERE_RUV = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/sphere_ruv.obj"));

    public static void orient(BlockEntity be, LegacyAnimator animator) {
        animator.translate(0.5, 0, 0.5);
        animator.rotate(90, 0, 1, 0);
        switch (ObjBerHelper.meta6(be)) {
            case 0 -> {
                animator.translate(0.0D, 0.5D, -0.5D);
                animator.rotate(90, 1, 0, 0);
            }
            case 1 -> {
                animator.translate(0.0D, 0.5D, 0.5D);
                animator.rotate(90, -1, 0, 0);
            }
            case 2 -> animator.rotate(90, 0, 1, 0);
            case 4 -> animator.rotate(180, 0, 1, 0);
            case 3 -> animator.rotate(270, 0, 1, 0);
            case 5 -> animator.rotate(0, 0, 1, 0);
            default -> { }
        }
    }

    public static void register() {
        // ── DFC Emitter ──────────────────────────────────────────────────────
        MachineRenderers.machine("dfc_emitter",
                ModBlockEntities.CORE_EMITTER_BE.get(),
                MachineCoreEmitterBlockEntity.class)
            .blockTransform(MachineDFCRenderers::orient)
            .part("Cube_Cube.001")
            .hook(new MachineRenderHook<MachineCoreEmitterBlockEntity>() {
                @Override
                public void render(MachineCoreEmitterBlockEntity te, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    int range = te.getBeamLength();
                    if (range <= 0) return;

                    ps.pushPose();
                    ps.translate(0, 0.5, 0);
                    long t = te.getLevel() != null ? te.getLevel().getGameTime() : 0L;
                    BeamPronter.prontBeamwithDepth(ps, buf, new Vec3(0, 0, range),
                            BeamPronter.EnumWaveType.SPIRAL, BeamPronter.EnumBeamType.SOLID,
                            0x404000, 0x404000, 0, 1, 0F, 2, 0.0625F, BeamPronter.forkAlpha());
                    BeamPronter.prontBeamwithDepth(ps, buf, new Vec3(0, 0, range),
                            BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID,
                            0x401500, 0x401500, (int) t % 1000, range * 2, 0.125F, 4, 0.0625F, BeamPronter.forkAlpha());
                    BeamPronter.prontBeamwithDepth(ps, buf, new Vec3(0, 0, range),
                            BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID,
                            0x401500, 0x401500, (int) t % 1000 + 1, range * 2, 0.125F, 4, 0.0625F, BeamPronter.forkAlpha());
                    ps.popPose();
                }

                @Override
                public boolean mayDrawContent(MachineCoreEmitterBlockEntity be) {
                    return be.getBeamLength() > 0;
                }
            })
            .itemParts("Cube_Cube.001")
            .register();

        // ── DFC Receiver ─────────────────────────────────────────────────────
        MachineRenderers.machine("dfc_receiver",
                ModBlockEntities.CORE_RECEIVER_BE.get(),
                MachineCoreReceiverBlockEntity.class)
            .blockTransform(MachineDFCRenderers::orient)
            .part("Cube_Cube.001")
            .itemParts("Cube_Cube.001")
            .register();

        // ── DFC Injector ─────────────────────────────────────────────────────
        MachineRenderers.machine("dfc_injector",
                ModBlockEntities.CORE_INJECTOR_BE.get(),
                MachineCoreInjectorBlockEntity.class)
            .blockTransform(MachineDFCRenderers::orient)
            .part("Cube_Cube.001")
            .hook(new MachineRenderHook<MachineCoreInjectorBlockEntity>() {
                @Override
                public void render(MachineCoreInjectorBlockEntity injector, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    int range = injector.getBeam();
                    if (range <= 0) return;

                    ps.pushPose();
                    ps.translate(0, 0.5, 0);
                    long t = injector.getLevel() != null ? injector.getLevel().getGameTime() : 0L;
                    FluidTank t0 = injector.getTank(0), t1 = injector.getTank(1);
                    if (t0 != null && t0.getFill() > 0) {
                        BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, range),
                                BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.LINE,
                                FluidType.forFluid(t0.getTankType()).getColor(), 0x808080,
                                (int) t % 1000, range, 0.0625F, 0, 0, BeamPronter.forkAlpha());
                    }
                    if (t1 != null && t1.getFill() > 0) {
                        BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, range),
                                BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.LINE,
                                FluidType.forFluid(t1.getTankType()).getColor(), 0x808080,
                                (int) t % 1000 + 1, range, 0.0625F, 0, 0, BeamPronter.forkAlpha());
                    }
                    ps.popPose();
                }

                @Override
                public boolean mayDrawContent(MachineCoreInjectorBlockEntity be) {
                    return be.getBeam() > 0;
                }
            })
            .itemParts("Cube_Cube.001")
            .register();

        // ── DFC Stabilizer ───────────────────────────────────────────────────
        MachineRenderers.machine("dfc_stabilizer",
                ModBlockEntities.DFC_STABILIZER_BE.get(),
                DFCStabilizerBlockEntity.class)
            .blockTransform(MachineDFCRenderers::orient)
            .part("Cube_Cube.001")
            .hook(new MachineRenderHook<DFCStabilizerBlockEntity>() {
                @Override
                public void render(DFCStabilizerBlockEntity stabilizer, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    int range = stabilizer.getBeam();
                    if (range <= 0) return;

                    ps.pushPose();
                    ps.translate(0, 0.5, 0);
                    long t = stabilizer.getLevel() != null ? stabilizer.getLevel().getGameTime() : 0L;
                    BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, range),
                            BeamPronter.EnumWaveType.SPIRAL, BeamPronter.EnumBeamType.LINE,
                            0xffa200, 0xffd000, (int) t * -25 % 360, range * 3, 0.125F, 0, 0, BeamPronter.forkAlpha());
                    BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, range),
                            BeamPronter.EnumWaveType.SPIRAL, BeamPronter.EnumBeamType.LINE,
                            0xffa200, 0xffd000, (int) t * -15 % 360 + 180, range * 3, 0.125F, 0, 0, BeamPronter.forkAlpha());
                    BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, range),
                            BeamPronter.EnumWaveType.SPIRAL, BeamPronter.EnumBeamType.LINE,
                            0xffa200, 0xffd000, (int) t * -5 % 360 + 180, range * 3, 0.125F, 0, 0, BeamPronter.forkAlpha());
                    ps.popPose();
                }

                @Override
                public boolean mayDrawContent(DFCStabilizerBlockEntity be) {
                    return be.getBeam() > 0;
                }
            })
            .itemParts("Cube_Cube.001")
            .register();

        // ── DFC Core ─────────────────────────────────────────────────────────
        MachineRenderers.machine("dfc_core",
                ModBlockEntities.DFC_CORE_BE.get(),
                DFCCoreBlockEntity.class)
            .hook(new MachineRenderHook<DFCCoreBlockEntity>() {
                @Override
                public void render(DFCCoreBlockEntity core, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    ps.pushPose();
                    ps.translate(0.5, 0.5, 0.5);
                    if (core.getRenderHeat() == 0) {
                        renderStandby(ps, buf);
                    } else if (core.isRenderMeltdown()) {
                        renderFlare(ps, buf, core);
                    } else {
                        renderOrb(ps, buf, core);
                    }
                    ps.popPose();
                }

                @Override
                public boolean mayDrawContent(DFCCoreBlockEntity be) {
                    return true;
                }
            })
            .register();
    }

    private static void renderStandby(PoseStack ps, MultiBufferSource buf) {
        ps.pushPose();
        ps.scale(0.25F, 0.25F, 0.25F);
        SPHERE_UV.renderAllColor(ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL), 0.5F, 0.5F, 0.5F, 1F);
        ps.scale(1.25F, 1.25F, 1.25F);
        SPHERE_UV.renderAllColor(ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.ADDITIVE_TRIANGLES), 0.1F, 0.1F, 0.1F, 1F);
        ps.popPose();

        if ((System.currentTimeMillis() / 100) % 10 == 0) {
            for (int i = 0; i < 3; i++) {
                RenderSparks.renderSpark(ps, buf, (int) System.currentTimeMillis() / 100 + i * 10000, 0, 0, 0, 1.5F, 5, 10, 0xFFFF00, 0xFFFFFF);
                RenderSparks.renderSpark(ps, buf, (int) System.currentTimeMillis() / 50 + i * 10000, 0, 0, 0, 1.5F, 5, 10, 0xFFFF00, 0xFFFFFF);
            }
        }
    }

    private static void renderOrb(PoseStack ps, MultiBufferSource buf, DFCCoreBlockEntity core) {
        ps.pushPose();
        int color = core.getColor();
        float r = ((color & 0xFF0000) >> 16) / 256F;
        float g = ((color & 0x00FF00) >> 8) / 256F;
        float b = ((color & 0x0000FF)) / 256F;
        float mod = 0.4F;

        FluidTank[] tanks = core.getTanks();
        int tot = tanks[0].getMaxFill() + tanks[1].getMaxFill();
        int fill = tanks[0].getFill() + tanks[1].getFill();

        float scale = 4.5F * fill / Math.max(1, tot) + 0.5F;
        ps.scale(scale, scale, scale);
        ps.scale(0.25F, 0.25F, 0.25F);
        SPHERE_RUV.renderAllColor(ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL), r * mod, g * mod, b * mod, 1F);

        long time = core.getLevel() != null ? core.getLevel().getGameTime() : 0L;
        double ix = (time * 0.1D) % (Math.PI * 2D);
        double t = 0.8F;
        float pulse = (float) ((1D / t) * Math.atan((t * Math.sin(ix)) / (1 - t * Math.cos(ix))));
        pulse = (float) ((pulse + 1D) / 2D);

        VertexConsumer add = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.ADDITIVE_TRIANGLES);
        for (int i = 0; i <= 16; i++) {
            ps.pushPose();
            float s = 1F + 0.25F * i;
            s += (pulse * (20 - i)) * 0.125F;
            ps.scale(s, s, s);
            SPHERE_RUV.renderAllColor(ps, add, r * mod, g * mod, b * mod, 1F);
            ps.popPose();
        }
        ps.popPose();
    }

    private static void renderFlare(PoseStack ps, MultiBufferSource buf, DFCCoreBlockEntity core) {
        int color = core.getColor();
        float r = ((color & 0xFF0000) >> 16) / 255F;
        float g = ((color & 0x00FF00) >> 8) / 255F;
        float b = ((color & 0x0000FF)) / 255F;

        long time = core.getLevel() != null ? core.getLevel().getGameTime() : 0L;
        float f1 = time / 200.0F;
        float f2 = 0.0F;
        Random random = new Random(432L);

        ps.pushPose();
        double ix = (time * 0.2D) % (Math.PI * 2D);
        double t = 0.8F;
        float pulse = (float) ((1D / t) * Math.atan((t * Math.sin(ix)) / (1 - t * Math.cos(ix))));
        pulse = (float) ((pulse + 1D) / 2D);
        float s = 0.875F + pulse * 0.125F;
        ps.scale(s, s, s);

        VertexConsumer vc = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.ADDITIVE_TRIANGLES);
        int count = 150;
        for (int i = 0; i < count; i++) {
            ps.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F));
            ps.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
            ps.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0F));
            ps.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F));
            ps.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
            ps.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0F + f1 * 90.0F));
            float f3 = random.nextFloat() * 2.0F + 5.0F + f2 * 10F;
            float f4 = random.nextFloat() * 1.0F + 1.0F + f2 * 2.0F;
            Matrix4f m = ps.last().pose();

            float[][] rim = {
                    { -0.866F * f4, f3, -0.5F * f4 },
                    { 0.866F * f4, f3, -0.5F * f4 },
                    { 0.0F, f3, 1.0F * f4 },
                    { -0.866F * f4, f3, -0.5F * f4 } };
            for (int k = 0; k < 3; k++) {
                com.hbm_m.platform.RenderHooks.vertexColor(vc, m, 0F, 0F, 0F, r, g, b, 1F);
                com.hbm_m.platform.RenderHooks.vertexColor(vc, m, rim[k][0], rim[k][1], rim[k][2], r, g, b, 0F);
                com.hbm_m.platform.RenderHooks.vertexColor(vc, m, rim[k + 1][0], rim[k + 1][1], rim[k + 1][2], r, g, b, 0F);
            }
            ps.scale(0.999F, 0.999F, 0.999F);
        }
        ps.popPose();
    }

    private MachineDFCRenderers() {}
}
