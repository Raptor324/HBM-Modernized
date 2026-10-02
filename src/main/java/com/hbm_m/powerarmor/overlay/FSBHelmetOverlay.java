package com.hbm_m.powerarmor.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

/** Original {@code ArmorFSB.renderHelmetOverlay}: Vollbild-Textur ueber dem HUD (z.B. Asbest-Helm). */
public final class FSBHelmetOverlay {

    private FSBHelmetOverlay() {}

    public static void render(ResourceLocation overlay, int width, int height) {
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.blendFuncSeparate(770, 771, 1, 0);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, overlay);

        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.vertex(0.0D, height, -90.0D).uv(0.0F, 1.0F).endVertex();
        buf.vertex(width, height, -90.0D).uv(1.0F, 1.0F).endVertex();
        buf.vertex(width, 0.0D, -90.0D).uv(1.0F, 0.0F).endVertex();
        buf.vertex(0.0D, 0.0D, -90.0D).uv(0.0F, 0.0F).endVertex();
        Tesselator.getInstance().end();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
