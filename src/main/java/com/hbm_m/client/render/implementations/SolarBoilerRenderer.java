package com.hbm_m.client.render.implementations;

import org.joml.Matrix4f;

import com.hbm_m.block.machines.MachineSolarBoilerBlock;
import com.hbm_m.blockentity.machines.MachineSolarBoilerBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderSolarBoiler}: Kesselmodell ("Base") nach Blickrichtung gedreht, bei schoener Grafik pro aktivem
 * Spiegel ein additiver Lichtstrahl (Alpha 0,01 am Kessel, 0,005 am Spiegel), hoechstens
 * {@code ClientConfig.renderHeliostatBeamLimit} Stueck.
 */
public class SolarBoilerRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineSolarBoilerBlockEntity> {

    // Original ClientConfig.RENDER_HELIOSTAT_BEAM_LIMIT: com.hbm_m.config.ClientConfig.renderHeliostatBeamLimit (client.json)

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/solar_boiler.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/solar_boiler.png");

    public SolarBoilerRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineSolarBoilerBlockEntity boiler, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {

        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(90));

        // Original: Metadaten 2/4/3/5 -> 0/90/180/270 Grad; Port-FACING entspricht dieser Richtung
        switch (boiler.getBlockState().getValue(MachineSolarBoilerBlock.FACING)) {
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        MODEL.renderPart("Base", ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);
        ps.popPose();

        if (Minecraft.getInstance().options.graphicsMode().get() != GraphicsStatus.FAST) {

            VertexConsumer vc = buf.getBuffer(com.hbm_m.client.ClientRenderHandler.CustomRenderTypes.RBMK_CHERENKOV);
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

                double pitch = Math.toDegrees(-Math.asin((dy + 0.5) / dist)) + 90;
                double yaw = Math.toDegrees(-Math.atan2(dz, dx)) + 180;

                ps.translate(0, 1, 0);
                ps.mulPose(Axis.YP.rotationDegrees((float) yaw));
                ps.mulPose(Axis.ZP.rotationDegrees((float) pitch));
                ps.translate(0, -1, 0);

                Matrix4f m = ps.last().pose();
                // Tessellator.setColorRGBA_F schneidet auf Bytes ab
                int aMax = (int) (max * 255F);
                int aMin = (int) (min * 255F);
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

        ps.popPose();
    }

    private static void v(VertexConsumer vc, Matrix4f m, float x, float y, float z, int a) {
        RenderHooks.vertexColor(vc, m, x, y, z, 255, 255, 255, a);
    }

    @Override public boolean shouldRenderOffScreen(MachineSolarBoilerBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
