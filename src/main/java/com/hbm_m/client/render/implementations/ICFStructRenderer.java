package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.icf.ICFStructBlock;
import com.hbm_m.blockentity.machines.icf.ICFStructBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

/**
 * 1:1 {@code RenderICFMultiblock}: zeichnet den Bauplan des ICF als durchscheinende Miniwuerfel
 * ({@code SmallBlockPronter.drawSmolBlockAt}, Deckkraft 0.75) um den Aufbaukern, gedreht nach dessen Ausrichtung.
 * Meta 0 = icf_component, 2 = .vessel_welded, 4 = .structure_bolted (alle Seiten gleich).
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class ICFStructRenderer implements com.hbm_m.client.render.HbmBerBounds<ICFStructBlockEntity> {

    private static final float MIN = 11F / 32F;
    private static final float MAX = 1F - MIN;
    private static final float ALPHA = 0.75F;

    private static final String META_0 = "block/icf_component";
    private static final String META_2 = "block/icf_component_vessel_welded";
    private static final String META_4 = "block/icf_component_structure_bolted";

    public ICFStructRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(ICFStructBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light, int overlay) {

        pose.pushPose();
        pose.translate(0.5, 0, 0.5);

        // Original: switch(getBlockMetadata()) 2 -> 270, 4 -> 0, 3 -> 90, 5 -> 180
        Direction dir = be.getBlockState().hasProperty(ICFStructBlock.FACING)
                ? be.getBlockState().getValue(ICFStructBlock.FACING) : Direction.NORTH;
        float rot = switch (dir) {
            case NORTH -> 270F;
            case WEST -> 0F;
            case SOUTH -> 90F;
            case EAST -> 180F;
            default -> 0F;
        };
        pose.mulPose(Axis.YP.rotationDegrees(rot));
        pose.translate(-0.5, 0, -0.5);

        Matrix4f m = pose.last().pose();
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS));

        for (int i = -8; i <= 8; i++) {
            cube(vc, m, META_0, 1, 0, i, light, overlay);
            if (i != 0) cube(vc, m, META_0, 0, 0, i, light, overlay);
            cube(vc, m, META_0, -1, 0, i, light, overlay);
            cube(vc, m, META_2, 0, 3, i, light, overlay);
            String ring = Math.abs(i) <= 2 ? META_2 : META_4;
            for (int j = -1; j <= 1; j++) cube(vc, m, ring, j, 1, i, light, overlay);
            for (int j = -2; j <= 2; j++) cube(vc, m, ring, j, 2, i, light, overlay);
            for (int j = -2; j <= 2; j++) if (j != 0) cube(vc, m, ring, j, 3, i, light, overlay);
            for (int j = -2; j <= 2; j++) cube(vc, m, ring, j, 4, i, light, overlay);
            for (int j = -1; j <= 1; j++) cube(vc, m, ring, j, 5, i, light, overlay);
        }

        pose.popPose();
    }

    @Override public int getViewDistance() { return 256; }
    @Override public boolean shouldRenderOffScreen(ICFStructBlockEntity be) { return true; }

    private static void cube(VertexConsumer vc, Matrix4f m, String tex, int cx, int cy, int cz, int light, int overlay) {
        TextureAtlasSprite s = RBMKColumnRenderer.sprite(RefStrings.MODID, tex);
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
