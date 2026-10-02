package com.hbm_m.client.render.implementations;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.block.network.PaintableDuctBlock;
import com.hbm_m.blockentity.network.PaintableDuctBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * 1:1 {@code FluidDuctPaintable} als {@code IBlockMultiPass}: Pass 0 = Anstrich (oder Rohr-Grundtextur),
 * Pass 1 = Markierung auf dem Anstrich (ausser Meta 1) bzw. ohne Anstrich die in Fluessigkeitsfarbe getoente Farbschicht.
 */
public class PaintableDuctRenderer implements BlockEntityRenderer<PaintableDuctBlockEntity> {

    private static final ResourceLocation BASE = ResourceLocation.fromNamespaceAndPath("hbm_m", "block/fluid_duct_paintable");
    private static final ResourceLocation OVERLAY = ResourceLocation.fromNamespaceAndPath("hbm_m", "block/fluid_duct_paintable_overlay");
    private static final ResourceLocation EXHAUST = ResourceLocation.fromNamespaceAndPath("hbm_m", "block/fluid_duct_paintable_block_exhaust");
    private static final ResourceLocation COLOR = ResourceLocation.fromNamespaceAndPath("hbm_m", "block/fluid_duct_paintable_color");

    public PaintableDuctRenderer(BlockEntityRendererProvider.Context ctx) {}

    private static TextureAtlasSprite sprite(ResourceLocation rl) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(rl);
    }

    @Override
    public void render(PaintableDuctBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        BlockState camo = be.getCamo();
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        if (camo != null) {
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(camo, ps, buf, light, overlay);
            if (!be.getBlockState().getValue(PaintableDuctBlock.HIDDEN)) cube(vc, ps, sprite(OVERLAY), 0xFFFFFF, light, overlay, 0.001F);
        } else if (be.getBlockState().getBlock() instanceof PaintableDuctBlock b && b.isExhaust()) {
            cube(vc, ps, sprite(EXHAUST), 0xFFFFFF, light, overlay, 0F);
        } else {
            cube(vc, ps, sprite(BASE), 0xFFFFFF, light, overlay, 0F);
            int col = be.getFluidType() == Fluids.EMPTY ? 0xFFFFFF : HbmFluidRegistry.getTintColor(be.getFluidType()) & 0xFFFFFF;
            cube(vc, ps, sprite(COLOR), col, light, overlay, 0.001F);
        }
    }

    private static void cube(VertexConsumer vc, PoseStack ps, TextureAtlasSprite s, int rgb, int light, int overlay, float e) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        float r = (rgb >> 16 & 255) / 255F, g = (rgb >> 8 & 255) / 255F, b = (rgb & 255) / 255F;
        float a = -e, z = 1 + e;
        // unten, oben, Nord, Sued, West, Ost
        quad(vc, m, n, s, r, g, b, light, overlay, 0, -1, 0, a, a, z, a, a, a, z, a, a, z, a, z);
        quad(vc, m, n, s, r, g, b, light, overlay, 0, 1, 0, a, z, a, a, z, z, z, z, z, z, z, a);
        quad(vc, m, n, s, r, g, b, light, overlay, 0, 0, -1, z, z, a, z, a, a, a, a, a, a, z, a);
        quad(vc, m, n, s, r, g, b, light, overlay, 0, 0, 1, a, z, z, a, a, z, z, a, z, z, z, z);
        quad(vc, m, n, s, r, g, b, light, overlay, -1, 0, 0, a, z, a, a, a, a, a, a, z, a, z, z);
        quad(vc, m, n, s, r, g, b, light, overlay, 1, 0, 0, z, z, z, z, a, z, z, a, a, z, z, a);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n, TextureAtlasSprite s, float r, float g, float b, int light, int overlay,
                             float nx, float ny, float nz, float... p) {
        float[][] uv = { { 0, 0 }, { 0, 1 }, { 1, 1 }, { 1, 0 } };
        for (int i = 0; i < 4; i++) {
            vc.vertex(m, p[i * 3], p[i * 3 + 1], p[i * 3 + 2]).color(r, g, b, 1F).uv(s.getU(uv[i][0] * 16), s.getV(uv[i][1] * 16))
                    .overlayCoords(overlay).uv2(light).normal(n, nx, ny, nz).endVertex();
        }
    }
}
