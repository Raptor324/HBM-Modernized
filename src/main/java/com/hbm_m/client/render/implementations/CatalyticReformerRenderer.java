package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineCatalyticReformerBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.util.HorsePronter;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.Polaroid;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code RenderCatalyticReformer}: Anlage (Original-Modell und -Ausrichtung), bei Polaroid 11 dreht sich obendrauf
 * das Pony mit Horn, Fluegeln und Zigarette.
 */
public class CatalyticReformerRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineCatalyticReformerBlockEntity> {

    static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/machines/catalytic_reformer.obj"));
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/machines/catalytic_reformer.png");
    static final ResourceLocation EXTRA = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/horse/dyx.png");

    public CatalyticReformerRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineCatalyticReformerBlockEntity be, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        switch (ObjBerHelper.meta(be)) {
            case 2 -> ObjBerHelper.rotY(ps, 90);
            case 4 -> ObjBerHelper.rotY(ps, 180);
            case 3 -> ObjBerHelper.rotY(ps, 270);
            case 5 -> ObjBerHelper.rotY(ps, 0);
        }

        MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light);

        if (Polaroid.id() == 11) {
            // rapidly spinning dicks
            ps.translate(-1.125, 1.375, 1);
            float s = 0.125F;
            ps.scale(s, s, s);
            ps.mulPose(Axis.YN.rotationDegrees((float) (System.currentTimeMillis() / 5D % 360D)));
            ps.translate(0, 0.1, -0.5);

            HorsePronter.reset();
            double r = 60;
            HorsePronter.pose(HorsePronter.id_body, 0, -r, 0);
            HorsePronter.pose(HorsePronter.id_tail, 0, 45, 90);
            HorsePronter.pose(HorsePronter.id_lbl, 0, -90 + r, 35);
            HorsePronter.pose(HorsePronter.id_rbl, 0, -90 + r, -35);
            HorsePronter.pose(HorsePronter.id_lfl, 0, r - 10, 5);
            HorsePronter.pose(HorsePronter.id_rfl, 0, r - 10, -5);
            HorsePronter.pose(HorsePronter.id_head, 0, r, 0);
            HorsePronter.enableHorn();
            HorsePronter.enableWings();
            HorsePronter.pront(ps, buf.getBuffer(RenderType.entityCutoutNoCull(EXTRA)), light);

            double scale = 0.25;
            ps.translate(0.02, 1.13, -0.42);
            ps.scale((float) scale, (float) scale, (float) scale);
            ps.mulPose(Axis.YN.rotationDegrees(90));
            ps.mulPose(Axis.ZN.rotationDegrees(60));
            TrinketRenderer.renderItem2D(ps, buf, new ItemStack(ModItems.CIGARETTE.get()), light);
        }

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineCatalyticReformerBlockEntity be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
