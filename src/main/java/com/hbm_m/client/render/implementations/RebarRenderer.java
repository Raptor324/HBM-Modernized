package com.hbm_m.client.render.implementations;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.hbm_m.blockentity.RebarBlockEntity;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * 1:1 {@code BlockRebar.renderRebar} (aus {@code onRenderWorldLastEvent}): zeichnet den Betonstand jeder
 * Bewehrung als Quader 0..progress/1000 mit {@code concrete_liquid}, hoechstens {@code RENDER_REBAR_LIMIT}
 * Bloecke pro Bild.
 */
public class RebarRenderer implements com.hbm_m.client.render.HbmBerBounds<RebarBlockEntity> {

    private static final ResourceLocation CONCRETE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/concrete_liquid");

    private static int renderedThisFrame = 0;

    public RebarRenderer(BlockEntityRendererProvider.Context context) { }

    /** Zu Beginn jedes Bildes (AFTER_SKY) aufgerufen. */
    public static void beginFrame() {
        renderedThisFrame = 0;
    }

    @Override
    public void render(RebarBlockEntity rebar, float interp, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (rebar.progress <= 0) return;
        if (renderedThisFrame >= ModClothConfig.get().renderRebarLimit) return;
        renderedThisFrame++;

        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(CONCRETE);
        VertexConsumer buf = buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        float h = Math.min(rebar.progress / 1000F, 1F);

        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();
        // Seiten: wie renderStandardBlock nur der untere Teil der Textur
        //? if < 1.21.1 {
        float vh = sprite.getV(16 - h * 16);
        //?} else {
        /*float vh = sprite.getV(1 - h); // 1.21.1: 0..1 statt 0..16
        *///?}

        // oben
        quad(buf, m, n, light, 0, 1, 0,
                0, h, 0, u0, v0,
                0, h, 1, u0, v1,
                1, h, 1, u1, v1,
                1, h, 0, u1, v0);
        // unten
        quad(buf, m, n, light, 0, -1, 0,
                0, 0, 0, u0, v0,
                1, 0, 0, u1, v0,
                1, 0, 1, u1, v1,
                0, 0, 1, u0, v1);
        // Norden (z = 0)
        quad(buf, m, n, light, 0, 0, -1,
                0, 0, 0, u1, v1,
                0, h, 0, u1, vh,
                1, h, 0, u0, vh,
                1, 0, 0, u0, v1);
        // Sueden (z = 1)
        quad(buf, m, n, light, 0, 0, 1,
                0, 0, 1, u0, v1,
                1, 0, 1, u1, v1,
                1, h, 1, u1, vh,
                0, h, 1, u0, vh);
        // Westen (x = 0)
        quad(buf, m, n, light, -1, 0, 0,
                0, 0, 0, u0, v1,
                0, 0, 1, u1, v1,
                0, h, 1, u1, vh,
                0, h, 0, u0, vh);
        // Osten (x = 1)
        quad(buf, m, n, light, 1, 0, 0,
                1, 0, 0, u1, v1,
                1, h, 0, u1, vh,
                1, h, 1, u0, vh,
                1, 0, 1, u0, v1);
    }

    private static void quad(VertexConsumer buf, Matrix4f m, Matrix3f n, int light, float nx, float ny, float nz,
                             float x0, float y0, float z0, float uA, float vA,
                             float x1, float y1, float z1, float uB, float vB,
                             float x2, float y2, float z2, float uC, float vC,
                             float x3, float y3, float z3, float uD, float vD) {
        vertex(buf, m, n, light, nx, ny, nz, x0, y0, z0, uA, vA);
        vertex(buf, m, n, light, nx, ny, nz, x1, y1, z1, uB, vB);
        vertex(buf, m, n, light, nx, ny, nz, x2, y2, z2, uC, vC);
        vertex(buf, m, n, light, nx, ny, nz, x3, y3, z3, uD, vD);
    }

    private static void vertex(VertexConsumer buf, Matrix4f m, Matrix3f n, int light, float nx, float ny, float nz,
                               float x, float y, float z, float u, float v) {
        //? if < 1.21.1 {
        buf.vertex(m, x, y, z).color(1F, 1F, 1F, 1F).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
        //?} else {
        /*com.hbm_m.platform.RenderHooks.normal(buf.addVertex(m, x, y, z).setColor(1F, 1F, 1F, 1F).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light), n, nx, ny, nz);
        *///?}
    }
}
