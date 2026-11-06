package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineLpw2BlockEntity;
import com.hbm_m.blockentity.machines.MachineMiningLaserBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.UvShiftConsumer;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.util.BeamPronter;
import com.hbm_m.client.render.util.BeamPronter.EnumBeamType;
import com.hbm_m.client.render.util.BeamPronter.EnumWaveType;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Nucleus renderers for laser research and advanced aerospace propulsion installations:
 * 1. LPW-2 Space Engine: external structural frame, wobbling center assembly,
 *    dual counter-rotating turbines, oscillating piston, gimballed thrust bell,
 *    aerodynamic shroud flaps, jittering servers, and scrolling CRT diagnostic error terminal hook.
 * 2. Mining Laser: stationary mounting base, azimuthal revolving turret pivot,
 *    pitch-elevating emitter head, and tri-phase helical red mining laser beam hook.
 */
public final class MachineLaserResearchRenderer {

    private static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/lpw2.obj"));
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/lpw2.png");
    private static final ResourceLocation ERROR_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/lpw2_term_error.png");

    public static void register() {
        // ── LPW-2 Aerospace Propulsion Engine ────────────────────────────────
        MachineRenderers.machine("lpw2",
                ModBlockEntities.LPW2_BE.get(),
                MachineLpw2BlockEntity.class)
            .part("Frame")
            .hook(new MachineRenderHook<MachineLpw2BlockEntity>() {
                @Override
                public void render(MachineLpw2BlockEntity te, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    long time = te.getLevel() == null ? 0 : te.getLevel().getGameTime() % 1000000;
                    double t = te.animationTime(time, pt);

                    double swayTimer = (t / 3D) % (Math.PI * 4);
                    double sway = (Math.sin(swayTimer) + Math.sin(swayTimer * 2) + Math.sin(swayTimer * 4) + 2.23255D) * 0.5;

                    double bellTimer = (t / 5D) % (Math.PI * 4);
                    double h = (Math.sin(bellTimer + Math.PI) + Math.sin(bellTimer * 1.5D)) / 1.90596D;
                    double v = (Math.sin(bellTimer) + Math.sin(bellTimer * 1.5D)) / 1.90596D;

                    double pistonTimer = (t / 5D) % (Math.PI * 2);
                    double piston = sps(pistonTimer);
                    double rotorTimer = (t / 5D) % (Math.PI * 16);
                    double rotor = (sps(rotorTimer) + rotorTimer / 2D - 1) / 25.1327412287D;
                    double turbine = (t % 100) / 100D;

                    VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));

                    renderMainAssembly(ps, vc, light, sway, h, v, piston, rotor, turbine);

                    // Articulated wiring harnesses
                    ps.pushPose();
                    ps.translate(-2.9375, 0, 2.375);
                    ps.mulPose(Axis.YP.rotationDegrees((float) (sway * 10)));
                    ps.translate(2.9375, 0, -2.375);
                    MODEL.renderPart("WireLeft", ps, vc, light);
                    ps.popPose();

                    ps.pushPose();
                    ps.translate(2.9375, 0, 2.375);
                    ps.mulPose(Axis.YP.rotationDegrees((float) (sway * -10)));
                    ps.translate(-2.9375, 0, -2.375);
                    MODEL.renderPart("WireRight", ps, vc, light);
                    ps.popPose();

                    // Shroud covers and suspension linkages
                    double coverTimer = (t / 5D) % (Math.PI * 4);
                    double cover = (Math.sin(coverTimer) + Math.sin(coverTimer * 2) + Math.sin(coverTimer * 4)) * 0.5;

                    ps.pushPose();
                    ps.translate(0, 0, -cover * 0.125);
                    MODEL.renderPart("Cover", ps, vc, light);
                    ps.popPose();

                    ps.pushPose();
                    ps.translate(0, 0, 3.5);
                    ps.scale(1, 1, (float) ((3 + cover * 0.125) / 3));
                    ps.translate(0, 0, -3.5);
                    MODEL.renderPart("SuspensionCoverFront", ps, vc, light);
                    ps.popPose();

                    ps.pushPose();
                    ps.translate(0, 0, -5.5);
                    ps.scale(1, 1, (float) ((1.5 - cover * 0.125) / 1.5));
                    ps.translate(0, 0, 5.5);
                    MODEL.renderPart("SuspensionCoverBack", ps, vc, light);
                    ps.popPose();

                    ps.pushPose();
                    ps.translate(0, 0, -9);
                    ps.scale(1, 1, (float) ((1.25 - sway * 0.125) / 1.25));
                    ps.translate(0, 0, 9);
                    MODEL.renderPart("SuspensionBackOuter", ps, vc, light);
                    ps.popPose();

                    ps.pushPose();
                    ps.translate(0, 0, -9.5);
                    ps.scale(1, 1, (float) ((1.75 - sway * 0.125) / 1.75));
                    ps.translate(0, 0, 9.5);
                    MODEL.renderPart("SuspensionBackCenter", ps, vc, light);
                    ps.popPose();

                    // Oscillating server blade arrays
                    double serverTimer = (t / 2D) % (Math.PI * 4);
                    double sx = (Math.sin(serverTimer + Math.PI) + Math.sin(serverTimer * 1.5D)) / 1.90596D;
                    double sy = (Math.sin(serverTimer) + Math.sin(serverTimer * 1.5D)) / 1.90596D;
                    double serverSway = 0.0625D * 0.25D;

                    ps.pushPose();
                    ps.translate(sx * serverSway, 0, sy * serverSway);
                    MODEL.renderPart("Server1", ps, vc, light);
                    ps.popPose();

                    ps.pushPose();
                    ps.translate(-sy * serverSway, 0, sx * serverSway);
                    MODEL.renderPart("Server2", ps, vc, light);
                    ps.popPose();

                    ps.pushPose();
                    ps.translate(sy * serverSway, 0, -sx * serverSway);
                    MODEL.renderPart("Server3", ps, vc, light);
                    ps.popPose();

                    ps.pushPose();
                    ps.translate(-sx * serverSway, 0, -sy * serverSway);
                    MODEL.renderPart("Server4", ps, vc, light);
                    ps.popPose();

                    // Diagnostic CRT monitor and scrolling error screen
                    double errorTimer = ((time + pt) / 3D);
                    ps.pushPose();
                    ps.translate(sy * serverSway, 0, sx * serverSway);
                    MODEL.renderPart("Monitor", ps, vc, light);

                    float scroll = (float) ((sps(errorTimer) + errorTimer / 2D) % 1);
                    MODEL.renderPart("Screen", ps,
                            new UvShiftConsumer(buf.getBuffer(RenderType.entityCutout(ERROR_TEX)), 0F, scroll),
                            light);
                    ps.popPose();
                }

                @Override
                public boolean mayDrawContent(MachineLpw2BlockEntity te) {
                    return true;
                }
            })
            .itemParts("Frame", "Center", "Engine")
            .register();

        // ── Mining Laser ─────────────────────────────────────────────────────
        MachineRenderers.machine("mining_laser",
                ModBlockEntities.MINING_LASER_BE.get(),
                MachineMiningLaserBlockEntity.class)
            .part("Base")
            .hook(new MachineRenderHook<MachineMiningLaserBlockEntity>() {
                @Override
                public void render(MachineMiningLaserBlockEntity laser, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    double tx = Mth.lerp(pt, laser.lastTargetX, laser.targetX);
                    double ty = Mth.lerp(pt, laser.lastTargetY, laser.targetY);
                    double tz = Mth.lerp(pt, laser.lastTargetZ, laser.targetZ);
                    double vx = tx - laser.getBlockPos().getX();
                    double vy = ty - laser.getBlockPos().getY() + 3;
                    double vz = tz - laser.getBlockPos().getZ();

                    Vec3 nVec = new Vec3(vx, vy, vz).normalize().scale(1.5D);
                    Vec3 vec = new Vec3(vx - nVec.x, vy - nVec.y, vz - nVec.z);

                    double yaw = Math.toDegrees(Math.atan2(vec.x, vec.z));
                    double sqrt = Math.sqrt(vec.x * vec.x + vec.z * vec.z);
                    double pitch = Math.toDegrees(Math.atan2(vec.y, sqrt));

                    // Azimuth pivot
                    ps.pushPose();
                    ps.mulPose(Axis.YP.rotationDegrees((float) yaw));
                    MiningLaserRenderer.MODEL.renderPart("Pivot", ps,
                            buf.getBuffer(RenderType.entityCutoutNoCull(MiningLaserRenderer.PIVOT_TEX)), light);
                    ps.popPose();

                    // Pitch emitter
                    ps.pushPose();
                    ps.mulPose(Axis.YP.rotationDegrees((float) yaw));
                    ps.translate(0, -1, 0);
                    ps.mulPose(Axis.XN.rotationDegrees((float) (pitch + 90)));
                    ps.translate(0, 1, 0);
                    MiningLaserRenderer.MODEL.renderPart("Laser", ps,
                            buf.getBuffer(RenderType.entityCutoutNoCull(MiningLaserRenderer.LASER_TEX)), light);
                    ps.popPose();

                    // Tri-spiral concentrated mining beam
                    if (laser.beam && laser.getLevel() != null) {
                        double length = vec.length();
                        ps.pushPose();
                        ps.translate(nVec.x, nVec.y - 1, nVec.z);
                        int range = (int) Math.ceil(length * 0.5);
                        int beamTime = (int) laser.getLevel().getGameTime() * -25 % 360;

                        BeamPronter.prontBeam(ps, buf, vec, EnumWaveType.SPIRAL, EnumBeamType.SOLID,
                                0xa00000, 0xa00000, beamTime, range * 2, 0.075F, 3, 0.025F, BeamPronter.forkAlpha());
                        BeamPronter.prontBeam(ps, buf, vec, EnumWaveType.SPIRAL, EnumBeamType.SOLID,
                                0xa00000, 0xa00000, beamTime + 120, range * 2, 0.075F, 3, 0.025F);
                        BeamPronter.prontBeam(ps, buf, vec, EnumWaveType.SPIRAL, EnumBeamType.SOLID,
                                0xa00000, 0xa00000, beamTime + 240, range * 2, 0.075F, 3, 0.025F, BeamPronter.forkAlpha());
                        ps.popPose();
                    }
                }

                @Override
                public boolean mayDrawContent(MachineMiningLaserBlockEntity laser) {
                    return true;
                }
            })
            .itemParts("Base", "Pivot", "Laser")
            .register();
    }

    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    private static void part(String name, PoseStack ps, VertexConsumer vc, int light) {
        MODEL.renderPart(name, ps, vc, light);
    }

    private static void renderMainAssembly(PoseStack ps, VertexConsumer vc, int light, double sway, double h, double v, double piston, double rotor, double turbine) {
        ps.pushPose();
        ps.translate(0, 0, -sway * 0.125);
        part("Center", ps, vc, light);

        ps.pushPose();
        ps.translate(0, 3.5, 0);

        ps.pushPose();
        ps.mulPose(Axis.ZN.rotationDegrees((float) (rotor * 360)));
        ps.translate(0, -3.5, 0);
        part("Rotor", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.mulPose(Axis.ZP.rotationDegrees((float) (turbine * 360)));
        ps.translate(0, -3.5, 0);
        part("TurbineFront", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.mulPose(Axis.ZN.rotationDegrees((float) (turbine * 360)));
        ps.translate(0, -3.5, 0);
        part("TurbineBack", ps, vc, light);
        ps.popPose();

        ps.popPose();

        ps.pushPose();
        ps.translate(0, 0, piston * 0.375D + 0.375D);
        part("Piston", ps, vc, light);
        ps.popPose();

        renderBell(ps, vc, light, h, v);
        ps.popPose();

        renderShroud(ps, vc, light, h, v);
    }

    private static void renderBell(PoseStack ps, VertexConsumer vc, int light, double h, double v) {
        ps.pushPose();
        ps.translate(0, 3.5, 2.75);
        double magnitude = 2D;
        ps.mulPose(Axis.YP.rotationDegrees((float) (v * magnitude)));
        ps.mulPose(Axis.XP.rotationDegrees((float) (h * magnitude)));
        ps.translate(0, -3.5, -2.75);
        part("Engine", ps, vc, light);
        ps.popPose();
    }

    private static void renderShroud(PoseStack ps, VertexConsumer vc, int light, double h, double v) {
        double magnitude = 0.125D;
        double rotation = 5D;
        double offset = 10D;

        ps.pushPose();
        ps.translate(0, -h * magnitude, 0);
        part("ShroudH", ps, vc, light);

        ps.pushPose();
        ps.translate(0, 3.5 + 2.75, 4.375);
        ps.mulPose(Axis.XP.rotationDegrees((float) (-h * rotation + offset)));
        ps.translate(0, -(3.5 + 2.75), -4.375);
        part("FlapTop", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 3.5 - 2.75, 4.375);
        ps.mulPose(Axis.XP.rotationDegrees((float) (-h * rotation - offset)));
        ps.translate(0, -(3.5 - 2.75), -4.375);
        part("FlapBottom", ps, vc, light);
        ps.popPose();
        ps.popPose();

        ps.pushPose();
        ps.translate(v * magnitude, 0, 0);
        part("ShroudV", ps, vc, light);

        ps.pushPose();
        ps.translate(-2.75, 3.5, 4.375);
        ps.mulPose(Axis.YP.rotationDegrees((float) (-v * rotation + offset)));
        ps.translate(2.75, -3.5, -4.375);
        part("FlapLeft", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(2.75, 3.5, 4.375);
        ps.mulPose(Axis.YP.rotationDegrees((float) (-v * rotation - offset)));
        ps.translate(-2.75, -3.5, -4.375);
        part("FlapRight", ps, vc, light);
        ps.popPose();
        ps.popPose();
    }

    private MachineLaserResearchRenderer() {}
}
