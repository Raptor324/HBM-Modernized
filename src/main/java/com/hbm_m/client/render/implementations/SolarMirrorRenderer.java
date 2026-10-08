package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.SolarMirrorBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderMirror} (im Original ein ISBRH): Sockel ("Base") fest, Spiegelflaeche ("Mirror") zum Ziel gedreht,
 * sobald das Ziel ueber dem Spiegel liegt. Original dreht jeden Eckpunkt um (0, 1, 0): erst
 * {@code rotateAroundX(pitch)}, dann {@code rotateAroundY(yaw)}; 1.7.10-{@code rotateAroundX} dreht gegen die
 * Rechte-Hand-Regel, daher hier {@code XP(-pitch)}.
 */
public class SolarMirrorRenderer implements com.hbm_m.client.render.HbmBerBounds<SolarMirrorBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/solar_mirror.obj"));
    /** Original Blockicon {@code hbm:solar_mirror}. */
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/solar_mirror.png");

    public SolarMirrorRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(SolarMirrorBlockEntity mirror, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {

        int dx = mirror.tX - mirror.getBlockPos().getX();
        int dy = mirror.tY - mirror.getBlockPos().getY();
        int dz = mirror.tZ - mirror.getBlockPos().getZ();

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));

        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);
        MODEL.renderPart("Base", ps, vc, light);

        if (mirror.tY <= mirror.getBlockPos().getY()) {
            MODEL.renderPart("Mirror", ps, vc, light);
        } else {
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double pitch = -Math.asin(dy / dist) + Math.PI / 2D;
            double yaw = -Math.atan2(dz, dx) - Math.PI / 2D;

            ps.translate(0D, 1D, 0D);
            ps.mulPose(Axis.YP.rotation((float) yaw));
            ps.mulPose(Axis.XP.rotation((float) -pitch));
            ps.translate(0D, -1D, 0D);
            MODEL.renderPart("Mirror", ps, vc, light);
        }

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(SolarMirrorBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
