package com.hbm_m.client.render.implementations;

import org.joml.Matrix4f;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineBigAssTankBlockEntity;
import com.hbm_m.blockentity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.blockentity.machines.OrbusBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.util.BeamPronter;
import com.hbm_m.client.render.util.DiamondPronter;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/**
 * Modern Nucleus renderer implementation for large industrial fluid storage installations:
 * <ul>
 *   <li>Big Ass Tank (Bulk Storage Tank): 16M mB capacity, tilt compensation, NFPA diamond plates,
 *       and side-window fluid level gauges;</li>
 *   <li>Orbus Spherical Tank: Multiblock spherical vessel with levitating fluid orb and energy containment beams.</li>
 * </ul>
 */
public final class MachineTankRenderer {

    private static final SimpleObjModel SPHERE_UV = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/sphere_uv.obj"));

    public static void register() {
        registerBigAssTank();
        registerOrbus();
    }

    private static boolean isNone(Fluid fluid) {
        return fluid == null || fluid == Fluids.EMPTY || fluid == ModFluids.NONE.getSource();
    }

    private static ResourceLocation tankTexture(MachineFluidTankBlockEntity be) {
        ResourceLocation loc = be.getTankTextureLocation();
        return ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), "textures/" + loc.getPath() + ".png");
    }

    private static void registerBigAssTank() {
        MachineRenderers.machine("bigasstank", ModBlockEntities.BIGASSTANK_BE.get(),
                MachineBigAssTankBlockEntity.class)
            .part("Body")
            .part("Tank")
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("Body", "Tank")
            .hook(new MachineRenderHook<MachineBigAssTankBlockEntity>() {
                @Override
                public void render(MachineBigAssTankBlockEntity bat, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    ps.pushPose();
                    if (bat.tilted) {
                        ps.translate(0, -1, 0);
                        ps.mulPose(Axis.ZP.rotationDegrees(10));
                        ps.mulPose(Axis.YP.rotationDegrees(5));
                    }

                    Fluid fluid = bat.getFluidTank().getTankType();
                    if (!isNone(fluid)) {
                        FluidType type = FluidType.forFluid(fluid);
                        ps.pushPose();
                        ps.mulPose(Axis.YP.rotationDegrees(22.5F));
                        for (int j = 0; j < 2; j++) {
                            ps.pushPose();
                            ps.translate(5.5, 2, 0);
                            DiamondPronter.pront(ps, buf, type.poison, type.flammability, type.reactivity, type.symbol, light, OverlayTexture.NO_OVERLAY);
                            ps.popPose();
                            ps.mulPose(Axis.YP.rotationDegrees(180));
                        }
                        ps.popPose();
                    }

                    VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(tankTexture(bat)));
                    Matrix4f m = ps.last().pose();
                    int fill = bat.getFluidTank().getFill(), max = Math.max(1, bat.getFluidTank().getMaxFill());
                    float height = (float) (fill * 1.5D / max);
                    float off = 5.9375F;
                    double speed = 250D;
                    double scaleFactor = 0.5D;
                    long time = bat.getLevel() != null ? bat.getLevel().getGameTime() : 0L;
                    float minU = (float) (-((time % speed + pt) / speed) % 1D);
                    float maxU = (float) (minU + 1 * scaleFactor);
                    float topV = (float) (-height * 2 * scaleFactor);

                    v(vc, m, -off, 1.75F, -0.25F, minU, 0, light);
                    v(vc, m, -off, 1.75F + height, -0.25F, minU, topV, light);
                    v(vc, m, -off, 1.75F + height, 0.25F, maxU, topV, light);
                    v(vc, m, -off, 1.75F, 0.25F, maxU, 0, light);

                    v(vc, m, off, 1.75F, -0.25F, maxU, 0, light);
                    v(vc, m, off, 1.75F + height, -0.25F, maxU, topV, light);
                    v(vc, m, off, 1.75F + height, 0.25F, minU, topV, light);
                    v(vc, m, off, 1.75F, 0.25F, minU, 0, light);

                    ps.popPose();
                }

                @Override
                public boolean mayDrawContent(MachineBigAssTankBlockEntity be) {
                    return true;
                }
            })
            .register();
    }

    private static void v(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float vv, int light) {
        RenderHooks.vertexFull(vc, m, x, y, z, 255, 255, 255, 255, u, vv, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
    }

    private static void registerOrbus() {
        MachineRenderers.machine("orbus", ModBlockEntities.ORBUS_BE.get(),
                OrbusBlockEntity.class)
            .part("Body")
            .part("Base")
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("Body", "Base")
            .hook(new MachineRenderHook<OrbusBlockEntity>() {
                @Override
                public void render(OrbusBlockEntity orbus, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    int fill = orbus.getFluidTank().getFill();
                    if (fill <= 0) return;

                    double scale = (double) fill / (double) Math.max(1, orbus.getFluidTank().getMaxFill());
                    long time = orbus.getLevel() != null ? orbus.getLevel().getGameTime() : 0L;
                    Fluid fluid = orbus.getFluidTank().getTankType();
                    int c = isNone(fluid) ? 0xFFFFFF : FluidType.forFluid(fluid).getColor();

                    ps.pushPose();
                    ps.translate(0, 2.5D + Math.sin(((time + pt) * 0.1D) % (Math.PI * 2D)) * 0.125 * scale, 0);
                    ps.scale((float) scale, (float) scale, (float) scale);
                    SPHERE_UV.renderAllColor(ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL),
                            ((c & 0xff0000) >> 16) / 255F, ((c & 0x00ff00) >> 8) / 255F, (c & 0x0000ff) / 255F, 1F);
                    ps.popPose();

                    ps.pushPose();
                    ps.translate(0, 1, 0);
                    BeamPronter.prontBeam(ps, buf, new Vec3(0, 3, 0), BeamPronter.EnumWaveType.SPIRAL, BeamPronter.EnumBeamType.SOLID, 0x101020, 0x101020, 0, 1, 0F, 6, (float) scale * 0.5F, BeamPronter.forkAlpha());
                    BeamPronter.prontBeam(ps, buf, new Vec3(0, 3, 0), BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID, 0x202060, 0x202060, (int) (time / 2) % 1000, 6, (float) scale, 2, 0.0625F * (float) scale, BeamPronter.forkAlpha());
                    BeamPronter.prontBeam(ps, buf, new Vec3(0, 3, 0), BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID, 0x202060, 0x202060, (int) (time / 4) % 1000, 6, (float) scale, 2, 0.0625F * (float) scale, BeamPronter.forkAlpha());
                    ps.popPose();
                }

                @Override
                public boolean mayDrawContent(OrbusBlockEntity be) {
                    return be.getFluidTank().getFill() > 0;
                }
            })
            .register();
    }

    private MachineTankRenderer() {}
}
