package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.decorations.LanternBehemothBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderLanternBehemoth}: rostige Laterne; kaputt schief (5 Grad um X, 10 um Z) mit rot pulsierendem
 * Licht, repariert gruen pulsierend (0.5-1.0).
 */
public class LanternBehemothRenderer implements BlockEntityRenderer<LanternBehemothBlockEntity> {

    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/trinkets/lantern_rusty.png");

    public LanternBehemothRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(LanternBehemothBlockEntity lantern, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0, 0.5D);

        if (lantern.isBroken) {
            ps.mulPose(Axis.XP.rotationDegrees(5));
            ps.mulPose(Axis.ZP.rotationDegrees(10));
        }

        LanternRenderer.LANTERN.renderPart("Lantern", ps, buffers.getBuffer(RenderType.entityCutoutNoCull(TEX)), light);

        float r, g;
        if (lantern.isBroken) {
            float mult = (float) (Math.sin(System.currentTimeMillis() / 200D) / 2 + 0.5);
            r = mult; g = 0;
        } else {
            float mult = (float) (Math.sin(System.currentTimeMillis() / 200D) / 2 + 0.5) * 0.5F + 0.5F;
            r = 0; g = mult;
        }
        LanternRenderer.LANTERN.renderPartColor("Light", ps, buffers.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL), r, g, 0F, 1F);

        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(LanternBehemothBlockEntity tile) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
