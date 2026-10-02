package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.WatzBlocks;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

/**
 * 1:1 {@code RenderWatzMultiblock}: zeichnet den Bauplan des Watz als durchscheinende Miniwuerfel
 * ({@code SmallBlockPronter.drawSmolBlockAt}, 5/16 Block gross, Deckkraft 0.75) um das Kernbauteil. Saeulenbloecke
 * (Reaktionskammer, Superkuehler) zeigen oben/unten ihre Stirntextur wie {@code BlockPillar}.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class StructWatzCoreRenderer implements com.hbm_m.client.render.HbmBerBounds<WatzBlocks.StructCoreBlockEntity> {

    private static final float MIN = 11F / 32F;
    private static final float MAX = 1F - MIN;
    private static final float ALPHA = 0.75F;

    private static final String[] COOLER = { "block/watz_cooler_side", "block/watz_cooler_top" };
    private static final String[] ELEMENT = { "block/watz_element_side", "block/watz_element_top" };
    private static final String[] END = { "block/watz_end_bolted", "block/watz_end_bolted" };

    public StructWatzCoreRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(WatzBlocks.StructCoreBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light, int overlay) {

        Matrix4f m = pose.last().pose();
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS));

        cube(vc, m, COOLER, 0, 1, 0, light, overlay);
        cube(vc, m, COOLER, 0, 2, 0, light, overlay);

        for (int i = 0; i < 3; i++) {
            cube(vc, m, ELEMENT, 1, i, 0, light, overlay);
            cube(vc, m, ELEMENT, 2, i, 0, light, overlay);
            cube(vc, m, ELEMENT, 0, i, 1, light, overlay);
            cube(vc, m, ELEMENT, 0, i, 2, light, overlay);
            cube(vc, m, ELEMENT, -1, i, 0, light, overlay);
            cube(vc, m, ELEMENT, -2, i, 0, light, overlay);
            cube(vc, m, ELEMENT, 0, i, -1, light, overlay);
            cube(vc, m, ELEMENT, 0, i, -2, light, overlay);
            cube(vc, m, ELEMENT, 1, i, 1, light, overlay);
            cube(vc, m, ELEMENT, 1, i, -1, light, overlay);
            cube(vc, m, ELEMENT, -1, i, 1, light, overlay);
            cube(vc, m, ELEMENT, -1, i, -1, light, overlay);
            cube(vc, m, COOLER, 2, i, 1, light, overlay);
            cube(vc, m, COOLER, 2, i, -1, light, overlay);
            cube(vc, m, COOLER, 1, i, 2, light, overlay);
            cube(vc, m, COOLER, -1, i, 2, light, overlay);
            cube(vc, m, COOLER, -2, i, 1, light, overlay);
            cube(vc, m, COOLER, -2, i, -1, light, overlay);
            cube(vc, m, COOLER, 1, i, -2, light, overlay);
            cube(vc, m, COOLER, -1, i, -2, light, overlay);
            for (int j = -1; j < 2; j++) {
                cube(vc, m, END, 3, i, j, light, overlay);
                cube(vc, m, END, j, i, 3, light, overlay);
                cube(vc, m, END, -3, i, j, light, overlay);
                cube(vc, m, END, j, i, -3, light, overlay);
            }
            cube(vc, m, END, 2, i, 2, light, overlay);
            cube(vc, m, END, 2, i, -2, light, overlay);
            cube(vc, m, END, -2, i, 2, light, overlay);
            cube(vc, m, END, -2, i, -2, light, overlay);
        }
    }

    @Override public int getViewDistance() { return 256; }
    @Override public boolean shouldRenderOffScreen(WatzBlocks.StructCoreBlockEntity be) { return true; }

    private static void cube(VertexConsumer vc, Matrix4f m, String[] tex, int cx, int cy, int cz, int light, int overlay) {
        TextureAtlasSprite side = RBMKColumnRenderer.sprite(RefStrings.MODID, tex[0]);
        TextureAtlasSprite top = RBMKColumnRenderer.sprite(RefStrings.MODID, tex[1]);
        float x0 = cx + MIN, x1 = cx + MAX;
        float y0 = cy + MIN, y1 = cy + MAX;
        float z0 = cz + MIN, z1 = cz + MAX;

        quad(vc, m, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0,  0, 0,-1, side, light, overlay);
        quad(vc, m, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1,  0, 0, 1, side, light, overlay);
        quad(vc, m, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, -1, 0, 0, side, light, overlay);
        quad(vc, m, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1,  1, 0, 0, side, light, overlay);
        quad(vc, m, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0,  0, 1, 0, top, light, overlay);
        quad(vc, m, x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1,  0,-1, 0, top, light, overlay);
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float nx, float ny, float nz,
                             TextureAtlasSprite s, int light, int overlay) {
        float u0 = s.getU0(), u1 = s.getU1(), v0 = s.getV0(), v1 = s.getV1();
        int a = (int) (ALPHA * 255);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, x0, y0, z0, 255, 255, 255, a, u0, v1, overlay, light, nx, ny, nz);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, x1, y1, z1, 255, 255, 255, a, u1, v1, overlay, light, nx, ny, nz);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, x2, y2, z2, 255, 255, 255, a, u1, v0, overlay, light, nx, ny, nz);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, x3, y3, z3, 255, 255, 255, a, u0, v0, overlay, light, nx, ny, nz);
    }
}
