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
 * 1:1-Port von {@code RenderFusionTorusMultiblock} (1.7.10) - der Bauplan des Fusionstorus.
 *
 * <p>Der Toruskern zeichnet das komplette {@code MachineFusionTorus.layout} als durchscheinende
 * Miniwuerfel um sich herum: an jeder Stelle den Wuerfel in der Textur des Bauteils, das dort
 * hingehoert. Ohne diese Anzeige ist der Torus praktisch nicht baubar - es sind fuenf Ebenen mit
 * je 15x15 Zellen aus drei verschiedenen Bauteilen, und {@code StructTorusCoreBlockEntity} setzt
 * sich erst in den fertigen Reaktor um, wenn jede einzelne davon stimmt.</p>
 *
 * <p>Die Wuerfelgroesse (5/16 Block, mittig in der Zelle) und die Deckkraft (0.75) stammen aus dem
 * Original ({@code SmallBlockPronter.drawSmolBlockAt} zeichnet von {@code 11/32} bis {@code 21/32},
 * {@code startDrawing} setzt {@code glColor4f(1,1,1,0.75)}).</p>
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class StructTorusCoreRenderer implements com.hbm_m.client.render.HbmBerBounds<StructTorusCoreBlockEntity> {

    /** Original: von 11*pixel/2 bis 1 - 11*pixel/2, also ein 5/16 grosser Wuerfel in der Zellmitte. */
    private static final float MIN = 11F / 32F;
    private static final float MAX = 1F - MIN;
    private static final float ALPHA = 0.75F;

    /** Bauteiltextur je Musterwert - dieselbe Zuordnung wie {@code componentForLayoutValue}. */
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
                    // entityTranslucentCull statt RenderType.translucent(): der Bauplan soll
                    // durchscheinend ueber den schon gesetzten Bauteilen liegen, und nur die
                    // entity-Layer werten das Alpha der Vertexfarbe ueberhaupt aus.
                    VertexConsumer vc = buffer.getBuffer(
                            RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS));

                    cube(vc, m, x, y, z, sprite, packedLight, packedOverlay);
                }
            }
        }
    }

    /** Der Bauplan ist 15x15x5 gross - ohne das wird er weggeculled, sobald der Kern aus dem Bild faellt. */
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
