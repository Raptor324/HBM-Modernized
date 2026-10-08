package com.hbm_m.client.render.implementations;

import org.joml.Matrix4f;

import com.hbm_m.block.machines.SoyuzStructBlock;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * 1:1 {@code RenderSoyuzMultiblock}: Bauplan der Sojus-Startrampe als durchscheinende Miniwuerfel (5/16 Block,
 * Deckkraft 0.75) - Startrampenbloecke fuer Tisch und Plattformen, Beton fuer die Beine, Geruest fuer die Tuerme.
 */
public class StructSoyuzCoreRenderer implements com.hbm_m.client.render.HbmBerBounds<SoyuzStructBlock.StructBE> {

    private static final float MIN = 11F / 32F;
    private static final float MAX = 1F - MIN;
    private static final float ALPHA = 0.75F;

    public StructSoyuzCoreRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(SoyuzStructBlock.StructBE be, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS));

        TextureAtlasSprite launcher = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/struct_launcher");
        TextureAtlasSprite concrete = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/concrete");
        TextureAtlasSprite scaffold = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/struct_scaffold");

        for (int i = -6; i <= 6; i++) for (int j = 3; j <= 4; j++) for (int k = -6; k <= 6; k++) cube(vc, m, launcher, i, j, k, light, overlay);
        for (int i = -1; i <= 1; i++) for (int j = 3; j <= 4; j++) for (int k = -8; k <= -7; k++) cube(vc, m, launcher, i, j, k, light, overlay);
        for (int i = -2; i <= 2; i++) for (int j = 3; j <= 4; j++) for (int k = 7; k <= 9; k++) cube(vc, m, launcher, i, j, k, light, overlay);
        for (int i = -2; i <= 2; i++) for (int k = 5; k <= 9; k++) cube(vc, m, launcher, i, 51, k, light, overlay);
        for (int i = -1; i <= 1; i++) for (int k = -8; k <= -6; k++) cube(vc, m, launcher, i, 38, k, light, overlay);

        for (int i = 3; i <= 6; i++) for (int j = 0; j <= 2; j++) for (int k = 3; k <= 6; k++) cube(vc, m, concrete, i, j, k, light, overlay);
        for (int i = -6; i <= -3; i++) for (int j = 0; j <= 2; j++) for (int k = 3; k <= 6; k++) cube(vc, m, concrete, i, j, k, light, overlay);
        for (int i = -6; i <= -3; i++) for (int j = 0; j <= 2; j++) for (int k = -6; k <= -3; k++) cube(vc, m, concrete, i, j, k, light, overlay);
        for (int i = 3; i <= 6; i++) for (int j = 0; j <= 2; j++) for (int k = -6; k <= -3; k++) cube(vc, m, concrete, i, j, k, light, overlay);
        for (int i = -1; i <= 1; i++) for (int j = 0; j <= 2; j++) for (int k = -8; k <= -6; k++) cube(vc, m, concrete, i, j, k, light, overlay);
        for (int i = -2; i <= 2; i++) for (int j = 0; j <= 2; j++) for (int k = 5; k <= 9; k++) cube(vc, m, concrete, i, j, k, light, overlay);

        for (int i = -1; i <= 1; i++) for (int j = 5; j <= 50; j++) for (int k = 6; k <= 8; k++) cube(vc, m, scaffold, i, j, k, light, overlay);
        for (int j = 5; j <= 37; j++) cube(vc, m, scaffold, 0, j, -7, light, overlay);
    }

    @Override public int getViewDistance() { return 256; }
    @Override public boolean shouldRenderOffScreen(SoyuzStructBlock.StructBE be) { return true; }

    public static void cube(VertexConsumer vc, Matrix4f m, TextureAtlasSprite s, int cx, int cy, int cz, int light, int overlay) {
        float x0 = cx + MIN, x1 = cx + MAX;
        float y0 = cy + MIN, y1 = cy + MAX;
        float z0 = cz + MIN, z1 = cz + MAX;
        quad(vc, m, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0,  0, 0,-1, s, light, overlay);
        quad(vc, m, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1,  0, 0, 1, s, light, overlay);
        quad(vc, m, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, -1, 0, 0, s, light, overlay);
        quad(vc, m, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1,  1, 0, 0, s, light, overlay);
        quad(vc, m, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0,  0, 1, 0, s, light, overlay);
        quad(vc, m, x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1,  0,-1, 0, s, light, overlay);
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float nx, float ny, float nz, TextureAtlasSprite s, int light, int overlay) {
        float u0 = s.getU0(), u1 = s.getU1(), v0 = s.getV0(), v1 = s.getV1();
        int a = (int) (ALPHA * 255);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, x0, y0, z0, 255, 255, 255, a, u0, v1, overlay, light, nx, ny, nz);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, x1, y1, z1, 255, 255, 255, a, u1, v1, overlay, light, nx, ny, nz);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, x2, y2, z2, 255, 255, 255, a, u1, v0, overlay, light, nx, ny, nz);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, x3, y3, z3, 255, 255, 255, a, u0, v0, overlay, light, nx, ny, nz);
    }
}
