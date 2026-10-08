package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineCombinationOvenBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderFurnaceCombination}: das Ofenmodell, und solange er brennt eine zur Kamera gedrehte, additive
 * Flammenflaeche (2x3 Bloecke, 1,75 ueber dem Sockel) mit dem 14-Bild-Streifen {@code rbmk_fire}.
 */
public class CombinationOvenRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineCombinationOvenBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/combination_oven.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/combination_oven.png");
    public static final ResourceLocation FIRE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/particle/rbmk_fire.png");

    public CombinationOvenRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineCombinationOvenBlockEntity te, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);

        if (te.wasOn && te.getLevel() != null) {
            ps.pushPose();

            int texIndex = (int) (te.getLevel().getGameTime() / 2 % 14);
            float f0 = 1F / 14F;

            // Original: uMin = texIndex % 5 * f0 - bewusst so uebernommen.
            float uMin = texIndex % 5 * f0;
            float uMax = uMin + f0;
            float vMin = 0;
            float vMax = 1;

            ps.translate(0, 1.75, 0);
            ps.mulPose(Axis.YP.rotationDegrees(-Minecraft.getInstance().gameRenderer.getMainCamera().getYRot()));

            float scaleH = 1;
            float scaleV = 3;

            VertexConsumer vc = buf.getBuffer(RenderType.eyes(FIRE));
            org.joml.Matrix4f m = ps.last().pose();
            org.joml.Matrix3f n = ps.last().normal();

            vertex(vc, m, n, -scaleH, 0, uMax, vMax);
            vertex(vc, m, n, -scaleH, scaleV, uMax, vMin);
            vertex(vc, m, n, scaleH, scaleV, uMin, vMin);
            vertex(vc, m, n, scaleH, 0, uMin, vMax);

            // Rueckseite, damit die Flaeche unabhaengig von der Windungsrichtung sichtbar ist.
            vertex(vc, m, n, scaleH, 0, uMin, vMax);
            vertex(vc, m, n, scaleH, scaleV, uMin, vMin);
            vertex(vc, m, n, -scaleH, scaleV, uMax, vMin);
            vertex(vc, m, n, -scaleH, 0, uMax, vMax);

            ps.popPose();
        }

        ps.popPose();
    }

    private static void vertex(VertexConsumer vc, org.joml.Matrix4f m, org.joml.Matrix3f n, float x, float y, float u, float v) {
        vc.vertex(m, x, y, 0).color(1F, 1F, 1F, 1F).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240, 240).normal(n, 0F, 1F, 0F).endVertex();
    }

    @Override public boolean shouldRenderOffScreen(MachineCombinationOvenBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
