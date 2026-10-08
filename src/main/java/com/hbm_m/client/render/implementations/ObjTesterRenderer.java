package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.generic.ObjTesterBlockEntity;
import com.hbm_m.client.render.util.HorsePronter;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RendererObjTester.renderTileEntityAt}: Pony "Sunburst" mit Horn, Culling aus. */
public class ObjTesterRenderer implements BlockEntityRenderer<ObjTesterBlockEntity> {

    private static final ResourceLocation EXTRA = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/horse/sunburst.png");

    public ObjTesterRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(ObjTesterBlockEntity te, float partialTick, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        HorsePronter.reset();
        HorsePronter.enableHorn();
        HorsePronter.pront(ps, buf.getBuffer(RenderType.entityCutoutNoCull(EXTRA)), light);
        ps.popPose();
    }

    /** Original: INFINITE_EXTENT_AABB, Sichtweite 256. */
    @Override public boolean shouldRenderOffScreen(ObjTesterBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
