package com.hbm_m.client.render.implementations;

import com.hbm_m.block.network.RedCablePaintableBlock;
import com.hbm_m.blockentity.network.RedCablePaintableBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code BlockCablePaintable} mit {@code RenderBlockMultipass} (2 Durchgaenge):
 * <ul>
 * <li>Durchgang 0: gestrichen = der Anstrichblock (hier mit seinem echten Modell), sonst {@code red_cable_base}.</li>
 * <li>Durchgang 1: ungestrichen immer {@code red_cable_overlay}; gestrichen nur bei Metadate 0
 * ({@link RedCablePaintableBlock#OVERLAY}), bei Metadate 1 noch einmal der Anstrich (also unsichtbar).</li>
 * </ul>
 * Das Overlay ist wie im Original ein alphagetesteter Wuerfel (Cutout) mit Blocklicht.
 */
public class RedCablePaintableRenderer implements com.hbm_m.client.render.HbmBerBounds<RedCablePaintableBlockEntity> {

    private static final ResourceLocation BASE_TEX = rl("hbm_m", "block/red_cable_base");
    private static final ResourceLocation OVERLAY_TEX = rl("hbm_m", "block/red_cable_overlay");

    private static ResourceLocation rl(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    /** Zweiter Durchgang liegt im Original tiefengleich darueber; hier minimal nach aussen gegen Z-Fighting. */
    private static final float EPS = 0.002F;

    public RedCablePaintableRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(RedCablePaintableBlockEntity be, float partialTick, com.mojang.blaze3d.vertex.PoseStack poseStack,
                       MultiBufferSource buffer, int light, int overlay) {
        BlockState camo = be.getCamo();
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        var consumer = buffer.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
        var pose = poseStack.last();

        // Durchgang 0
        if (camo != null) {
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(camo, poseStack, buffer, light, overlay);
        } else {
            cube(consumer, pose, atlas.apply(BASE_TEX), 0F, 1F, light, overlay);
        }

        // Durchgang 1
        BlockState state = be.getBlockState();
        boolean showOverlay = camo == null
                || !state.hasProperty(RedCablePaintableBlock.OVERLAY) || state.getValue(RedCablePaintableBlock.OVERLAY);
        if (showOverlay) {
            cube(consumer, pose, atlas.apply(OVERLAY_TEX), -EPS, 1 + EPS, light, overlay);
        }
    }

    private static void cube(com.mojang.blaze3d.vertex.VertexConsumer c, com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                             TextureAtlasSprite sprite, float a, float b, int light, int overlay) {
        // unten / oben
        face(c, pose, sprite, light, overlay, 0, -1, 0, a, a, b, a, a, a, b, a, a, b, a, b);
        face(c, pose, sprite, light, overlay, 0, 1, 0, a, b, a, a, b, b, b, b, b, b, b, a);
        // Norden / Sueden
        face(c, pose, sprite, light, overlay, 0, 0, -1, b, b, a, b, a, a, a, a, a, a, b, a);
        face(c, pose, sprite, light, overlay, 0, 0, 1, a, b, b, a, a, b, b, a, b, b, b, b);
        // Westen / Osten
        face(c, pose, sprite, light, overlay, -1, 0, 0, a, b, a, a, a, a, a, a, b, a, b, b);
        face(c, pose, sprite, light, overlay, 1, 0, 0, b, b, b, b, a, b, b, a, a, b, b, a);
    }

    /** Ecken gegen den Uhrzeigersinn von aussen gesehen: oben-links, unten-links, unten-rechts, oben-rechts. */
    private static void face(com.mojang.blaze3d.vertex.VertexConsumer c, com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                             TextureAtlasSprite sprite, int light, int overlay, float nx, float ny, float nz,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        vert(c, pose, sprite, x0, y0, z0, 0, 0, light, overlay, nx, ny, nz);
        vert(c, pose, sprite, x1, y1, z1, 0, 1, light, overlay, nx, ny, nz);
        vert(c, pose, sprite, x2, y2, z2, 1, 1, light, overlay, nx, ny, nz);
        vert(c, pose, sprite, x3, y3, z3, 1, 0, light, overlay, nx, ny, nz);
    }

    private static void vert(com.mojang.blaze3d.vertex.VertexConsumer c, com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                             TextureAtlasSprite sprite, float x, float y, float z, float u, float v,
                             int light, int overlay, float nx, float ny, float nz) {
        //? if < 1.21.1 {
        float uu = sprite.getU(u * 16F); // 1.20: Pixel 0-16
        float vv = sprite.getV(v * 16F);
        c.vertex(pose.pose(), x, y, z).color(1F, 1F, 1F, 1F).uv(uu, vv).overlayCoords(overlay).uv2(light)
                .normal(pose.normal(), nx, ny, nz).endVertex();
        //?} else {
        /*float uu = sprite.getU(u);
        float vv = sprite.getV(v);
        c.addVertex(pose, x, y, z).setColor(1F, 1F, 1F, 1F).setUv(uu, vv).setOverlay(overlay).setLight(light)
                .setNormal(pose, nx, ny, nz);
        *///?}
    }
}
