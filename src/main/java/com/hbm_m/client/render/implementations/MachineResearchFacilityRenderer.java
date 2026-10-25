package com.hbm_m.client.render.implementations;

import java.awt.Color;
import java.util.Random;

import org.joml.Matrix4f;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.ForceFieldBlockEntity;
import com.hbm_m.blockentity.machines.MachineExposureChamberBlockEntity;
import com.hbm_m.blockentity.machines.MachineFelBlockEntity;
import com.hbm_m.blockentity.machines.MachineSuperComputerBlockEntity;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.util.BeamPronter;
import com.hbm_m.client.render.util.BeamPronter.EnumBeamType;
import com.hbm_m.client.render.util.BeamPronter.EnumWaveType;
import com.hbm_m.item.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Nucleus renderers for research installations:
 * 1. Supercomputer: chassis body with fullbright scanning CRT raster lights hook.
 * 2. Exposure Chamber: static chamber, GPU rotating magnets ring, floating core,
 *    and multi-beam high-voltage discharge arc hook.
 * 3. Free Electron Laser (FEL): laser chassis with depth-tested coherent beam hook
 *    modulating wavelength colors and spiral waveforms.
 * 4. Force Field Generator: pedestal base, rotating field head, and dynamic geodesic
 *    wireframe containment sphere hook.
 */
public final class MachineResearchFacilityRenderer {

    public static final ResourceLocation SUPERCOMPUTER_SCAN_TEX =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/machines/supercomputer_scan.png");
    private static final SimpleObjModel SUPERCOMPUTER_MODEL =
            new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/machines/supercomputer.obj"));
    private static final SimpleObjModel EXPOSURE_CHAMBER_MODEL =
            new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/exposure_chamber.obj"));
    private static final ResourceLocation EXPOSURE_CHAMBER_TEX =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/exposure_chamber.png");

    public static void register() {
        // ── Supercomputer ────────────────────────────────────────────────────
        MachineRenderers.machine("supercomputer",
                ModBlockEntities.SUPERCOMPUTER_BE.get(),
                MachineSuperComputerBlockEntity.class)
            .part("Computer")
            .hook(new MachineRenderHook<MachineSuperComputerBlockEntity>() {
                @Override
                public void render(MachineSuperComputerBlockEntity computer, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    float c = computer.didProcess ? 1F : 0F;
                    float scroll = computer.getLevel() != null
                            ? (computer.getLevel().getGameTime() % 20 + pt) / 20F
                            : 0F;
                    VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(SUPERCOMPUTER_SCAN_TEX));
                    SUPERCOMPUTER_MODEL.renderPartShiftedU("Lights", ps, vc,
                            LightTexture.FULL_BRIGHT, c, c, c, -scroll);
                }

                @Override
                public boolean mayDrawContent(MachineSuperComputerBlockEntity computer) {
                    return true;
                }
            })
            .itemParts("Computer")
            .register();

        // ── Exposure Chamber ─────────────────────────────────────────────────
        MachineRenderers.machine("exposure_chamber",
                ModBlockEntities.EXPOSURE_CHAMBER_BE.get(),
                MachineExposureChamberBlockEntity.class)
            .part("Chamber")
            .rotateY("Magnets", 0f, 0f, (be, pt) -> (double) Mth.lerp(pt, be.prevRotation, be.rotation))
            .hook(new MachineRenderHook<MachineExposureChamberBlockEntity>() {
                @Override
                public void render(MachineExposureChamberBlockEntity chamber, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    if (!chamber.isOn) return;

                    double rotation = Mth.lerp(pt, chamber.prevRotation, chamber.rotation);
                    long time = chamber.getLevel() == null ? 0 : chamber.getLevel().getGameTime();

                    // Floating oscillating core
                    ps.pushPose();
                    ps.mulPose(Axis.YP.rotationDegrees((float) (rotation / 2D)));
                    ps.translate(0, Math.sin((time % (Math.PI * 16D) + pt) * 0.125) * 0.0625, 0);
                    VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(EXPOSURE_CHAMBER_TEX));
                    EXPOSURE_CHAMBER_MODEL.renderPart("Core", ps, vc, LightTexture.FULL_BRIGHT);
                    ps.popPose();

                    // High-voltage discharge arcs
                    int duration = 8;
                    Random rand = new Random(time / duration);
                    int chance = 2;
                    int color = time % duration >= duration / 2 ? 0x80d0ff : 0xffffff;
                    rand.nextInt(chance);
                    int start = (int) (System.currentTimeMillis() % 1000) / 50;

                    if (rand.nextInt(chance) == 0) {
                        ps.pushPose();
                        ps.translate(0, 3.675, -7.5);
                        BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, 5), EnumWaveType.RANDOM, EnumBeamType.LINE,
                                color, 0xffffff, start, 15, 0.125F, 1, 0, BeamPronter.forkAlpha());
                        ps.popPose();
                    }
                    if (rand.nextInt(chance) == 0) {
                        ps.pushPose();
                        ps.translate(1.1875, 2.5, -7.5);
                        BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, 5), EnumWaveType.RANDOM, EnumBeamType.LINE,
                                color, 0xffffff, start, 15, 0.125F, 1, 0, BeamPronter.forkAlpha());
                        ps.popPose();
                    }
                    if (rand.nextInt(chance) == 0) {
                        ps.pushPose();
                        ps.translate(-1.1875, 2.5, -7.5);
                        BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, 5), EnumWaveType.RANDOM, EnumBeamType.LINE,
                                color, 0xffffff, start, 15, 0.125F, 1, 0, BeamPronter.forkAlpha());
                        ps.popPose();
                    }

                    // Vertical and spiral containment discharges
                    ps.pushPose();
                    ps.translate(0, 1.75, 0);
                    BeamPronter.prontBeam(ps, buf, new Vec3(0, 1.5, 0), EnumWaveType.RANDOM, EnumBeamType.LINE,
                            0x80d0ff, 0xffffff, start, 10, 0.125F, 1, 0, BeamPronter.forkAlpha());
                    BeamPronter.prontBeam(ps, buf, new Vec3(0, 1.5, 0), EnumWaveType.RANDOM, EnumBeamType.LINE,
                            0x8080ff, 0xffffff, (int) (System.currentTimeMillis() + 5 % 1000) / 50, 10, 0.125F, 1, 0, BeamPronter.forkAlpha());
                    ps.popPose();

                    ps.pushPose();
                    ps.translate(0, 2.5, 0);
                    BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, -1), EnumWaveType.SPIRAL, EnumBeamType.LINE,
                            0xffff80, 0xffffff, (int) (System.currentTimeMillis() % 360), 15, 0.125F, 1, 0, BeamPronter.forkAlpha());
                    BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, -1), EnumWaveType.SPIRAL, EnumBeamType.LINE,
                            0xff8080, 0xffffff, (int) (System.currentTimeMillis() % 360) + 180, 15, 0.125F, 1, 0, BeamPronter.forkAlpha());
                    ps.popPose();
                }

                @Override
                public boolean mayDrawContent(MachineExposureChamberBlockEntity chamber) {
                    return chamber.isOn;
                }
            })
            .itemParts("Chamber", "Magnets", "Core")
            .register();

        // ── FEL (Free Electron Laser) ────────────────────────────────────────
        MachineRenderers.machine("fel",
                ModBlockEntities.FEL_BE.get(),
                MachineFelBlockEntity.class)
            .hook(new MachineRenderHook<MachineFelBlockEntity>() {
                @Override
                public void render(MachineFelBlockEntity fel, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    long time = fel.getLevel() != null ? fel.getLevel().getGameTime() : 0;
                    int color = (fel.mode.renderedBeamColor == 0)
                            ? (Color.HSBtoRGB(time / 50.0F, 0.5F, 0.1F) & 0xFFFFFF)
                            : fel.mode.renderedBeamColor;
                    int length = fel.distance - 3;

                    if (fel.getEnergyStored() > MachineFelBlockEntity.powerReq * Math.pow(2, fel.mode.tier)
                            && fel.isOn && fel.mode != EnumWavelengths.NULL && length > 0) {
                        ps.pushPose();
                        ps.translate(0, 1.5, -1.5);
                        BeamPronter.prontBeamwithDepth(ps, buf, new Vec3(0, 0, -length - 1),
                                EnumWaveType.SPIRAL, EnumBeamType.SOLID, color, color, 0, 1, 0F, 2, 0.0625F,
                                BeamPronter.forkAlpha());
                        BeamPronter.prontBeamwithDepth(ps, buf, new Vec3(0, 0, -length - 1),
                                EnumWaveType.RANDOM, EnumBeamType.SOLID, color, color,
                                (int) (time % 1000 / 2), (length / 2) + 1, 0.0625F, 2, 0.0625F,
                                BeamPronter.forkAlpha());
                        ps.popPose();
                    }
                }

                @Override
                public boolean mayDrawContent(MachineFelBlockEntity fel) {
                    return fel.isOn && fel.mode != EnumWavelengths.NULL && fel.distance > 3;
                }
            })
            .register();

        // ── Force Field Generator ────────────────────────────────────────────
        MachineRenderers.machine("forcefield",
                ModBlockEntities.FORCE_FIELD_BE.get(),
                ForceFieldBlockEntity.class)
            .hook(new MachineRenderHook<ForceFieldBlockEntity>() {
                @Override
                public void render(ForceFieldBlockEntity be, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    boolean fieldUp = be.isFieldOn() && be.getEnergyStored() > 0;
                    if (!fieldUp) return;

                    int segments = (int) (16 + be.getRadius() * 0.125F);
                    drawSphere(ps, buf, segments, segments * 2, be.getRadius(), be.getColor());
                }

                @Override
                public boolean mayDrawContent(ForceFieldBlockEntity be) {
                    return be.isFieldOn() && be.getEnergyStored() > 0;
                }
            })
            .register();
    }

    /**
     * Renders a geodesic wireframe sphere for the force field containment boundary.
     */
    private static void drawSphere(PoseStack ps, MultiBufferSource buffer, int l, int s, float rad, int hex) {
        if (l <= 0 || s <= 0) return;

        VertexConsumer vc = buffer.getBuffer(RenderType.lines());
        Matrix4f m = ps.last().pose();

        float r = ((hex >> 16) & 0xFF) / 255F;
        float g = ((hex >> 8) & 0xFF) / 255F;
        float b = (hex & 0xFF) / 255F;

        float sRotDeg = 360F / s;
        float lRot = (float) Math.PI / l;

        // Longitude lines
        for (int k = 0; k < s; k++) {
            float yaw = (float) Math.toRadians(sRotDeg * (k + 1));
            Vec3 vec = new Vec3(0, rad, 0);

            for (int i = 0; i < l; i++) {
                Vec3 next = rotateX(vec, lRot);
                line(vc, m, rotateY(vec, yaw), rotateY(next, yaw), r, g, b);
                vec = next;
            }
        }

        // Latitude rings
        Vec3 ring = new Vec3(0, rad, 0);
        for (int k = 0; k < l; k++) {
            ring = rotateZ(ring, lRot);
            Vec3 vec = ring;

            for (int i = 0; i < s; i++) {
                Vec3 next = rotateY(vec, (float) Math.PI * 2F / s);
                line(vc, m, vec, next, r, g, b);
                vec = next;
            }
        }
    }

    private static void line(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, float r, float g, float bl) {
        Vec3 dir = b.subtract(a);
        double len = dir.length();
        float nx = len > 1.0E-6D ? (float) (dir.x / len) : 0F;
        float ny = len > 1.0E-6D ? (float) (dir.y / len) : 1F;
        float nz = len > 1.0E-6D ? (float) (dir.z / len) : 0F;

        RenderHooks.vertexColorNormal(vc, m, (float) a.x, (float) a.y, (float) a.z, r, g, bl, 1F, nx, ny, nz);
        RenderHooks.vertexColorNormal(vc, m, (float) b.x, (float) b.y, (float) b.z, r, g, bl, 1F, nx, ny, nz);
    }

    private static Vec3 rotateX(Vec3 v, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        return new Vec3(v.x, v.y * c - v.z * s, v.y * s + v.z * c);
    }

    private static Vec3 rotateY(Vec3 v, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        return new Vec3(v.x * c + v.z * s, v.y, v.z * c - v.x * s);
    }

    private static Vec3 rotateZ(Vec3 v, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        return new Vec3(v.x * c - v.y * s, v.x * s + v.y * c, v.z);
    }

    private MachineResearchFacilityRenderer() {}
}
