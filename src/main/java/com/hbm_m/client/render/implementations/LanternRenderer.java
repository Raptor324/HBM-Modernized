package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.decorations.LanternBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderLantern}: Mast mit Textur, Leuchtkoerper untexturiert voll hell und flackernd (0.9-1.0, gelblich). */
public class LanternRenderer implements BlockEntityRenderer<LanternBlockEntity> {

    public static final SimpleObjModel LANTERN = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/lantern.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/lantern.png");

    public LanternRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(LanternBlockEntity tile, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0, 0.5D);

        LANTERN.renderPart("Lantern", ps, buffers.getBuffer(RenderType.entityCutoutNoCull(TEX)), light);

        float mult = (float) (Math.sin(System.currentTimeMillis() / 200D) / 2 + 0.5) * 0.1F + 0.9F;
        LANTERN.renderPartColor("Light", ps, buffers.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL), 1F * mult, 1F * mult, 0.7F * mult, 1F);

        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(LanternBlockEntity tile) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
