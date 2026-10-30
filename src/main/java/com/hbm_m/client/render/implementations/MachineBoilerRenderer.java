package com.hbm_m.client.render.implementations;

import org.joml.Matrix4f;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineBoilerBlockEntity;
import com.hbm_m.blockentity.machines.MachineSolarBoilerBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;

/**
 * Modern Nucleus renderer implementation for steam generation facilities:
 * <ul>
 *   <li>Industrial Boiler: High-pressure steam vessel with pressure pulsation and failure burst state;</li>
 *   <li>Solar Boiler: Solar thermal central receiver with concentrated heliostat laser/beam hooks.</li>
 * </ul>
 */
public final class MachineBoilerRenderer {

    public static void register() {
        registerIndustrialBoiler();
        registerSolarBoiler();
    }

    private static void registerIndustrialBoiler() {
        // Объект boiler.obj слит в одну группу "Plane" — пульсация давления идёт по всей модели.
        MachineRenderers.machine("boiler", ModBlockEntities.BOILER_BE.get(),
                MachineBoilerBlockEntity.class)
            .part("Plane", (be, pt, gt, ps) -> {
                if (be.tanks != null && be.tanks.length > 1 && be.tanks[1].getFill() > be.tanks[1].getMaxFill() * 0.9) {
                    double sine = Math.sin(System.currentTimeMillis() / 50.0 % (Math.PI * 2)) * 0.01;
                    ps.scale((float) (1.0 - sine), (float) (1.0 + sine), (float) (1.0 - sine));
                }
                return true;
            })
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("Plane")
            .register();
    }

    private static void registerSolarBoiler() {
        MachineRenderers.machine("solar_boiler", ModBlockEntities.SOLAR_BOILER_BE.get(),
                MachineSolarBoilerBlockEntity.class)
            .part("Base")
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("Base")
            .hook(new MachineRenderHook<MachineSolarBoilerBlockEntity>() {
                @Override
                public void render(MachineSolarBoilerBlockEntity boiler, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    if (Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST) return;
                    if (boiler.secondary.isEmpty()) return;

                    VertexConsumer vc = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.RBMK_CHERENKOV);
                    BlockPos bp = boiler.getBlockPos();
                    int beamCount = 0;

                    for (BlockPos co : boiler.secondary) {
                        beamCount++;
                        if (beamCount > com.hbm_m.config.ClientConfig.renderHeliostatBeamLimit) break;

                        int dx = bp.getX() - co.getX();
                        int dy = bp.getY() - co.getY();
                        int dz = bp.getZ() - co.getZ();
                        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                        float min = 0.005F;
                        float max = 0.01F;

                        ps.pushPose();
                        ps.translate(-dx, -dy, -dz);

                        double pitch = Math.toDegrees(-Math.asin((dy + 0.5) / dist)) + 90.0;
                        double yaw = Math.toDegrees(-Math.atan2(dz, dx)) + 180.0;

                        ps.translate(0, 1, 0);
                        ps.mulPose(Axis.YP.rotationDegrees((float) yaw));
                        ps.mulPose(Axis.ZP.rotationDegrees((float) pitch));
                        ps.translate(0, -1, 0);

                        Matrix4f m = ps.last().pose();
                        int aMax = (int) (max * 255.0F);
                        int aMin = (int) (min * 255.0F);
                        float d = (float) dist;

                        v(vc, m, 0.5F, 1.0625F, 0.5F, aMax);
                        v(vc, m, 0.5F, 1.0625F, -0.5F, aMax);
                        v(vc, m, 0.5F, d, -0.5F, aMin);
                        v(vc, m, 0.5F, d, 0.5F, aMin);

                        v(vc, m, -0.5F, 1.0625F, 0.5F, aMax);
                        v(vc, m, -0.5F, 1.0625F, -0.5F, aMax);
                        v(vc, m, -0.5F, d, -0.5F, aMin);
                        v(vc, m, -0.5F, d, 0.5F, aMin);

                        v(vc, m, 0.5F, 1.0625F, 0.5F, aMax);
                        v(vc, m, -0.5F, 1.0625F, 0.5F, aMax);
                        v(vc, m, -0.5F, d, 0.5F, aMin);
                        v(vc, m, 0.5F, d, 0.5F, aMin);

                        v(vc, m, 0.5F, 1.0625F, -0.5F, aMax);
                        v(vc, m, -0.5F, 1.0625F, -0.5F, aMax);
                        v(vc, m, -0.5F, d, -0.5F, aMin);
                        v(vc, m, 0.5F, d, -0.5F, aMin);

                        ps.popPose();
                    }
                }

                @Override
                public boolean mayDrawContent(MachineSolarBoilerBlockEntity be) {
                    return !be.secondary.isEmpty();
                }
            })
            .register();
    }

    private static void v(VertexConsumer vc, Matrix4f m, float x, float y, float z, int a) {
        RenderHooks.vertexColor(vc, m, x, y, z, 255, 255, 255, a);
    }

    private MachineBoilerRenderer() {}
}
