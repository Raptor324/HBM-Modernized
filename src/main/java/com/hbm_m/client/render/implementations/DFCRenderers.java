package com.hbm_m.client.render.implementations;

import java.util.Random;

import org.joml.Matrix4f;

import com.hbm_m.blockentity.machines.MachineCoreEmitterBlockEntity;
import com.hbm_m.blockentity.machines.MachineCoreInjectorBlockEntity;
import com.hbm_m.blockentity.machines.dfc.DFCCoreBlockEntity;
import com.hbm_m.blockentity.machines.dfc.DFCStabilizerBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.util.BeamPronter;
import com.hbm_m.client.render.util.RenderSparks;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code RenderCoreComponent} (Emitter, Injektor, Stabilisator) und {@code RenderCore} (DFC-Kern). */
public final class DFCRenderers {

    private DFCRenderers() {}

    static final SimpleObjModel EMITTER = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/reactors/core_emitter.obj"));
    static final SimpleObjModel INJECTOR = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/reactors/core_injector.obj"));
    static final SimpleObjModel RECEIVER = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/reactors/core_receiver.obj"));
    static final ResourceLocation RECEIVER_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/core_receiver.png");
    static final ResourceLocation EMITTER_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/core_emitter.png");
    static final ResourceLocation INJECTOR_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/core_injector.png");
    static final ResourceLocation STABILIZER_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/core_stabilizer.png");

    /** Original-Ausrichtung: erst 90 Grad, dann je Metadate; 0/1 kippen um X. */
    static void orient(PoseStack ps, BlockEntity be) {
        ps.translate(0.5, 0, 0.5);
        ObjBerHelper.rotY(ps, 90);
        switch (ObjBerHelper.meta6(be)) {
            case 0 -> {
                ps.translate(0.0D, 0.5D, -0.5D);
                ps.mulPose(Axis.XP.rotationDegrees(90));
            }
            case 1 -> {
                ps.translate(0.0D, 0.5D, 0.5D);
                ps.mulPose(Axis.XN.rotationDegrees(90));
            }
            case 2 -> ObjBerHelper.rotY(ps, 90);
            case 4 -> ObjBerHelper.rotY(ps, 180);
            case 3 -> ObjBerHelper.rotY(ps, 270);
            case 5 -> ObjBerHelper.rotY(ps, 0);
            default -> { }
        }
    }

    static long time(BlockEntity be) {
        return be.getLevel() != null ? be.getLevel().getGameTime() : 0L;
    }

    public static class Emitter implements com.hbm_m.client.render.HbmBerBounds<MachineCoreEmitterBlockEntity> {
        public Emitter(BlockEntityRendererProvider.Context ctx) { }

        @Override
        public void render(MachineCoreEmitterBlockEntity te, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            ps.pushPose();
            orient(ps, te);
            EMITTER.renderAll(ps, buf.getBuffer(RenderType.entityCutout(EMITTER_TEX)), light);
            ps.translate(0, 0.5, 0);
            int range = te.getBeamLength();
            if (range > 0) {
                long t = time(te);
                BeamPronter.prontBeamwithDepth(ps, buf, new Vec3(0, 0, range), BeamPronter.EnumWaveType.SPIRAL, BeamPronter.EnumBeamType.SOLID, 0x404000, 0x404000, 0, 1, 0F, 2, 0.0625F);
                BeamPronter.prontBeamwithDepth(ps, buf, new Vec3(0, 0, range), BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID, 0x401500, 0x401500, (int) t % 1000, range * 2, 0.125F, 4, 0.0625F);
                BeamPronter.prontBeamwithDepth(ps, buf, new Vec3(0, 0, range), BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID, 0x401500, 0x401500, (int) t % 1000 + 1, range * 2, 0.125F, 4, 0.0625F);
            }
            ps.popPose();
        }

        @Override public boolean shouldRenderOffScreen(MachineCoreEmitterBlockEntity be) { return true; }
    }

    /** Original: Empfaenger nur Gehaeuse, ausgerichtet wie die anderen Kernbauteile (6 Richtungen). */
    public static class Receiver implements com.hbm_m.client.render.HbmBerBounds<com.hbm_m.blockentity.machines.MachineCoreReceiverBlockEntity> {
        public Receiver(BlockEntityRendererProvider.Context ctx) { }

        @Override
        public void render(com.hbm_m.blockentity.machines.MachineCoreReceiverBlockEntity te, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            ps.pushPose();
            orient(ps, te);
            RECEIVER.renderAll(ps, buf.getBuffer(RenderType.entityCutout(RECEIVER_TEX)), light);
            ps.popPose();
        }
    }

    public static class Injector implements com.hbm_m.client.render.HbmBerBounds<MachineCoreInjectorBlockEntity> {
        public Injector(BlockEntityRendererProvider.Context ctx) { }

        @Override
        public void render(MachineCoreInjectorBlockEntity injector, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            ps.pushPose();
            orient(ps, injector);
            INJECTOR.renderAll(ps, buf.getBuffer(RenderType.entityCutout(INJECTOR_TEX)), light);
            ps.translate(0, 0.5, 0);
            int range = injector.getBeam();
            if (range > 0) {
                long t = time(injector);
                FluidTank t0 = injector.getTank(0), t1 = injector.getTank(1);
                if (t0 != null && t0.getFill() > 0)
                    BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, range), BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.LINE,
                            FluidType.forFluid(t0.getTankType()).getColor(), 0x808080, (int) t % 1000, range, 0.0625F, 0, 0);
                if (t1 != null && t1.getFill() > 0)
                    BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, range), BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.LINE,
                            FluidType.forFluid(t1.getTankType()).getColor(), 0x808080, (int) t % 1000 + 1, range, 0.0625F, 0, 0);
            }
            ps.popPose();
        }

        @Override public boolean shouldRenderOffScreen(MachineCoreInjectorBlockEntity be) { return true; }
    }

    public static class Stabilizer implements com.hbm_m.client.render.HbmBerBounds<DFCStabilizerBlockEntity> {
        public Stabilizer(BlockEntityRendererProvider.Context ctx) { }

        @Override
        public void render(DFCStabilizerBlockEntity stabilizer, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            ps.pushPose();
            orient(ps, stabilizer);
            // Original: Injektormodell mit Stabilisatortextur
            INJECTOR.renderAll(ps, buf.getBuffer(RenderType.entityCutout(STABILIZER_TEX)), light);
            ps.translate(0, 0.5, 0);
            int range = stabilizer.getBeam();
            if (range > 0) {
                long t = time(stabilizer);
                BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, range), BeamPronter.EnumWaveType.SPIRAL, BeamPronter.EnumBeamType.LINE, 0xffa200, 0xffd000, (int) t * -25 % 360, range * 3, 0.125F, 0, 0);
                BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, range), BeamPronter.EnumWaveType.SPIRAL, BeamPronter.EnumBeamType.LINE, 0xffa200, 0xffd000, (int) t * -15 % 360 + 180, range * 3, 0.125F, 0, 0);
                BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, range), BeamPronter.EnumWaveType.SPIRAL, BeamPronter.EnumBeamType.LINE, 0xffa200, 0xffd000, (int) t * -5 % 360 + 180, range * 3, 0.125F, 0, 0);
            }
            ps.popPose();
        }

        @Override public boolean shouldRenderOffScreen(DFCStabilizerBlockEntity be) { return true; }
    }

    // ── RenderCore ─────────────────────────────────────────────────────────────────────

    /**
     * DFC-Kern: ohne Hitze die graue Bereitschaftskugel mit gelegentlichen Funken, sonst die pulsierende Kugel in der
     * Katalysatorfarbe (Groesse nach Fuellstand), beim Durchschmelzen der Strahlenkranz.
     */
    public static class Core implements com.hbm_m.client.render.HbmBerBounds<DFCCoreBlockEntity> {

        static final SimpleObjModel SPHERE_UV = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/sphere_uv.obj"));
        static final SimpleObjModel SPHERE_RUV = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/sphere_ruv.obj"));

        public Core(BlockEntityRendererProvider.Context ctx) { }

        @Override
        public void render(DFCCoreBlockEntity core, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
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

        private static void renderStandby(PoseStack ps, MultiBufferSource buf) {
            ps.pushPose();
            ps.scale(0.25F, 0.25F, 0.25F);
            SPHERE_UV.renderAllColor(ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL), 0.5F, 0.5F, 0.5F, 1F);
            // Blend SRC_ALPHA/ONE
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
            pulse += 1D;
            pulse /= 2D;

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
            pulse += 1D;
            pulse /= 2D;
            float s = 0.875F;
            s += pulse * 0.125F;
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
                // Original GL_TRIANGLE_FAN: Mitte + vier Randpunkte -> drei Dreiecke
                float[][] rim = {
                        { -0.866F * f4, f3, -0.5F * f4 },
                        { 0.866F * f4, f3, -0.5F * f4 },
                        { 0.0F, f3, 1.0F * f4 },
                        { -0.866F * f4, f3, -0.5F * f4 } };
                for (int k = 0; k < 3; k++) {
                    vc.vertex(m, 0F, 0F, 0F).color(r, g, b, 1F).endVertex();
                    vc.vertex(m, rim[k][0], rim[k][1], rim[k][2]).color(r, g, b, 0F).endVertex();
                    vc.vertex(m, rim[k + 1][0], rim[k + 1][1], rim[k + 1][2]).color(r, g, b, 0F).endVertex();
                }
                ps.scale(0.999F, 0.999F, 0.999F);
            }
            ps.popPose();
        }

        @Override public boolean shouldRenderOffScreen(DFCCoreBlockEntity be) { return true; }
        @Override public int getViewDistance() { return 256; }
    }
}
