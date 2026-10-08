package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineStirlingBlockEntity;
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
 * 1:1 {@code RenderStirling}: Sockel, grosses Zahnrad (nur mit {@code hasCog}) dreht mit {@code -rot} um Z,
 * kleines Zahnrad mit {@code rot * 2 + 3} um X, Kolben pendelt mit {@code sin(rot * PI / 90) * 0.25 + 0.125}.
 * Textur je Bauart ({@code getGeatMeta}: 0 normal, 1 Stahl, 2 kreativ).
 */
public class StirlingRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineStirlingBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/machines/stirling.obj"));
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/stirling.png");
    private static final ResourceLocation TEX_STEEL = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/stirling_steel.png");
    private static final ResourceLocation TEX_CREATIVE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/stirling_creative.png");

    public StirlingRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineStirlingBlockEntity stirling, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);
        switch (ObjBerHelper.meta(stirling)) {
            case 3 -> ObjBerHelper.rotY(ps, 0);
            case 5 -> ObjBerHelper.rotY(ps, 90);
            case 2 -> ObjBerHelper.rotY(ps, 180);
            case 4 -> ObjBerHelper.rotY(ps, 270);
        }

        float rot = stirling.lastSpin + (stirling.spin - stirling.lastSpin) * interp;
        int type = stirling.getGeatMeta();
        ResourceLocation tex = type == 0 ? TEX : type == 2 ? TEX_CREATIVE : TEX_STEEL;
        renderCommon(ps, buf.getBuffer(RenderType.entityCutout(tex)), light, rot, stirling.hasCog());

        ps.popPose();
    }

    public static void renderCommon(PoseStack ps, VertexConsumer vc, int light, float rot, boolean hasCog) {
        MODEL.renderPart("Base", ps, vc, light);

        if (hasCog) {
            ps.pushPose();
            ps.translate(0, 1.375, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(-rot));
            ps.translate(0, -1.375, 0);
            MODEL.renderPart("Cog", ps, vc, light);
            ps.popPose();
        }

        ps.pushPose();
        ps.translate(0, 1.375, 0.25);
        ps.mulPose(Axis.XP.rotationDegrees(rot * 2 + 3));
        ps.translate(0, -1.375, -0.25);
        MODEL.renderPart("CogSmall", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(Math.sin(rot * Math.PI / 90D) * 0.25 + 0.125, 0, 0);
        MODEL.renderPart("Piston", ps, vc, light);
        ps.popPose();
    }

    @Override public int getViewDistance() { return 256; }
}
