package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineStrandCasterBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderStrandCaster}: das Geruest, die aus der Form herauswandernde Gussplatte in der Farbe des Materials
 * (je nach Fuellung weiter vorn) und der vollhelle Schmelzspiegel im Giessturm.
 */
public class StrandCasterRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineStrandCasterBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/strand_caster.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/strand_caster.png");
    public static final ResourceLocation LAVA = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/lava_gray.png");

    public StrandCasterRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineStrandCasterBlockEntity caster, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 4/3/5/2 -> 90/180/270/0 Grad
        switch (caster.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }
        ps.translate(0.5D, 0D, 0.5D);
        ps.mulPose(Axis.YP.rotationDegrees(180));

        MODEL.renderPart("caster", ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light);

        if (caster.amount != 0 && caster.getInstalledMold() != null && caster.type != null) {

            float level = (float) (((double) caster.amount / (double) caster.getCapacity()) * 0.675D);
            double offset = ((double) caster.amount / (double) caster.getInstalledMold().getCost()) * 0.375D;

            int color = caster.type.moltenColor;
            float r = (color >> 16 & 0xFF) / 255F;
            float g = (color >> 8 & 0xFF) / 255F;
            float b = (color & 0xFF) / 255F;

            // Original: Clip-Ebene bei z = 0,5; die Platte faehrt aus der Form heraus
            ps.pushPose();
            ps.translate(0, 0, Math.max(-offset + 3.4, 0));
            MODEL.renderPartColor("plate", ps, buf.getBuffer(com.hbm_m.client.ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL), r, g, b, 1F);
            ps.popPose();

            VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(LAVA));
            org.joml.Matrix4f m = ps.last().pose();
            org.joml.Matrix3f n = ps.last().normal();
            float y = 2.3F + level;
            int full = 0xF000F0;
            //? if < 1.21.1 {
            vc.vertex(m, -0.9F, y, -0.999F).color(r, g, b, 1F).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full).normal(n, 0, 1, 0).endVertex();
            vc.vertex(m, -0.9F, y, 0.999F).color(r, g, b, 1F).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full).normal(n, 0, 1, 0).endVertex();
            vc.vertex(m, 0.9F, y, 0.999F).color(r, g, b, 1F).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full).normal(n, 0, 1, 0).endVertex();
            vc.vertex(m, 0.9F, y, -0.999F).color(r, g, b, 1F).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full).normal(n, 0, 1, 0).endVertex();
            //?} else {
            /*com.hbm_m.platform.RenderHooks.normal(vc.addVertex(m, -0.9F, y, -0.999F).setColor(r, g, b, 1F).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full), n, 0, 1, 0);
            com.hbm_m.platform.RenderHooks.normal(vc.addVertex(m, -0.9F, y, 0.999F).setColor(r, g, b, 1F).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full), n, 0, 1, 0);
            com.hbm_m.platform.RenderHooks.normal(vc.addVertex(m, 0.9F, y, 0.999F).setColor(r, g, b, 1F).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full), n, 0, 1, 0);
            com.hbm_m.platform.RenderHooks.normal(vc.addVertex(m, 0.9F, y, -0.999F).setColor(r, g, b, 1F).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full), n, 0, 1, 0);
            *///?}
        }

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineStrandCasterBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
