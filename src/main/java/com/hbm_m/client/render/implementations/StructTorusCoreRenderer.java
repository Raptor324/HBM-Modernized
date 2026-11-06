package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.fusion.MachineFusionTorusBlock;
import com.hbm_m.blockentity.machines.fusion.StructTorusCoreBlockEntity;
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
 * 1:1 port of {@code RenderFusionTorusMultiblock} (1.7.10) - blueprint overlay for fusion torus.
 *
 * <p>The torus core renders the complete {@code MachineFusionTorus.layout} as semitransparent
 * mini cubes around itself: each position displays the cube with the texture of the required component.
 * Without this overlay the torus is impractical to construct - five layers of 15x15 cells consisting
 * of three component types, converted into the complete reactor by {@code StructTorusCoreBlockEntity}
 * only when every single position matches.</p>
 *
 * <p>Cube size (5/16 block, centered in cell) and opacity (0.75) match original
 * ({@code SmallBlockPronter.drawSmolBlockAt} renders from {@code 11/32} to {@code 21/32},
 * {@code startDrawing} sets {@code glColor4f(1,1,1,0.75)}).</p>
 */
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
public class StructTorusCoreRenderer implements com.hbm_m.client.render.HbmBerBounds<StructTorusCoreBlockEntity> {

    /** Original: from 11*pixel/2 to 1 - 11*pixel/2, a 5/16 cube centered in the cell. */
    private static final float MIN = 11F / 32F;
    private static final float MAX = 1F - MIN;
    private static final float ALPHA = 0.75F;

    /** Component texture per template value - same mapping as {@code componentForLayoutValue}. */
    private static final String[] TEXTURE_BY_VALUE = {
            null,                              // 0 = Luft
            "block/fusion_component_bscco_welded",
            "block/fusion_component_blanket",
            "block/fusion_component_motor",
    };

    public StructTorusCoreRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(StructTorusCoreBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        Matrix4f m = pose.last().pose();

        for (int y = 0; y < 5; y++) {
            for (int x = -7; x <= 7; x++) {
                for (int z = -7; z <= 7; z++) {

                    int value = MachineFusionTorusBlock.layoutAt(x, y, z);
                    if (value <= 0 || value >= TEXTURE_BY_VALUE.length) continue;

                    String texture = TEXTURE_BY_VALUE[value];
                    if (texture == null) continue;

                    TextureAtlasSprite sprite = RBMKColumnRenderer.sprite(RefStrings.MODID, texture);
                    // entityTranslucentCull instead of RenderType.translucent(): blueprint overlay
                    // renders semitransparently over placed components, and entity layers evaluate
                    // vertex color alpha.
                    VertexConsumer vc = buffer.getBuffer(
                            RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS));

                    cube(vc, m, x, y, z, sprite, packedLight, packedOverlay);
                }
            }
        }
    }

    /** Blueprint dimensions 15x15x5 - prevents culling when the core block entity leaves view. */
    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen(StructTorusCoreBlockEntity be) {
        return true;
    }

    private static void cube(VertexConsumer vc, Matrix4f m, int cx, int cy, int cz,
                             TextureAtlasSprite s, int light, int overlay) {
        float x0 = cx + MIN, x1 = cx + MAX;
        float y0 = cy + MIN, y1 = cy + MAX;
        float z0 = cz + MIN, z1 = cz + MAX;

        quad(vc, m, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0,  0, 0,-1, s, light, overlay); // N
        quad(vc, m, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1,  0, 0, 1, s, light, overlay); // S
        quad(vc, m, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, -1, 0, 0, s, light, overlay); // W
        quad(vc, m, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1,  1, 0, 0, s, light, overlay); // E
        quad(vc, m, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0,  0, 1, 0, s, light, overlay); // Up
        quad(vc, m, x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1,  0,-1, 0, s, light, overlay); // Down
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
