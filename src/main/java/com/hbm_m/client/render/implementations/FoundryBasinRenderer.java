package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.IRenderFoundry;
import com.hbm_m.blockentity.machines.MachineFoundryCastingBaseBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code RenderFoundry} fuer Giessbecken und flache Formen: die eingesetzte Form und das fertige Gussstueck liegen
 * flach im Becken (Bloecke als halbdurchsichtige Oberflaeche), darueber der vollhelle Schmelzspiegel in der Farbe des
 * Materials mit einer additiven Glanzschicht.
 */
public class FoundryBasinRenderer<T extends MachineFoundryCastingBaseBlockEntity & IRenderFoundry> implements com.hbm_m.client.render.HbmBerBounds<T> {

    public static final ResourceLocation LAVA = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/lava_gray.png");

    public FoundryBasinRenderer(BlockEntityRendererProvider.Context ctx) { }

    private void drawItem(T tile, ItemStack stack, double height, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        ps.translate(0.5D, height, 0.5D);
        ps.mulPose(Axis.XP.rotationDegrees(90));
        ps.scale(0.75F, 0.75F, 0.01F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.GUI, light, OverlayTexture.NO_OVERLAY, ps, buf, tile.getLevel(), 0);
        ps.popPose();
    }

    private void drawBlock(ItemStack stack, IRenderFoundry foundry, PoseStack ps, MultiBufferSource buf, int light) {
        BlockItem item = (BlockItem) stack.getItem();
        TextureAtlasSprite icon = Minecraft.getInstance().getBlockRenderer().getBlockModel(item.getBlock().defaultBlockState()).getParticleIcon();
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        org.joml.Matrix4f m = ps.last().pose();
        org.joml.Matrix3f n = ps.last().normal();
        float h = (float) foundry.outHeight();
        float x0 = (float) foundry.minX(), x1 = (float) foundry.maxX(), z0 = (float) foundry.minZ(), z1 = (float) foundry.maxZ();
        vertex(vc, m, n, x0, h, z0, 1F, 1F, 1F, 0.3F, icon.getU0(), icon.getV1(), light);
        vertex(vc, m, n, x0, h, z1, 1F, 1F, 1F, 0.3F, icon.getU1(), icon.getV1(), light);
        vertex(vc, m, n, x1, h, z1, 1F, 1F, 1F, 0.3F, icon.getU1(), icon.getV0(), light);
        vertex(vc, m, n, x1, h, z0, 1F, 1F, 1F, 0.3F, icon.getU0(), icon.getV0(), light);
    }

    @Override
    public void render(T tile, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        IRenderFoundry foundry = tile;

        ItemStack mold = tile.slots[0];
        if (!mold.isEmpty()) {
            drawItem(tile, mold, foundry.moldHeight(), ps, buf, light);
        }

        ItemStack out = tile.slots[1];
        if (!out.isEmpty()) {
            if (out.getItem() instanceof BlockItem) {
                drawBlock(out, foundry, ps, buf, light);
            } else {
                drawItem(tile, out, foundry.outHeight(), ps, buf, light);
            }
        }

        if (foundry.shouldRender() && foundry.getMat() != null) {
            int hex = foundry.getMat().moltenColor;
            float r = (hex >> 16 & 255) / 255F, g = (hex >> 8 & 255) / 255F, b = (hex & 255) / 255F;
            float y = (float) foundry.getMoltenLevel();
            float x0 = (float) foundry.minX(), x1 = (float) foundry.maxX(), z0 = (float) foundry.minZ(), z1 = (float) foundry.maxZ();
            int full = 0xF000F0;

            VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(LAVA));
            org.joml.Matrix4f m = ps.last().pose();
            org.joml.Matrix3f n = ps.last().normal();
            vertex(vc, m, n, x0, y, z0, r, g, b, 1F, z0, x1, full);
            vertex(vc, m, n, x0, y, z1, r, g, b, 1F, z1, x1, full);
            vertex(vc, m, n, x1, y, z1, r, g, b, 1F, z1, x0, full);
            vertex(vc, m, n, x1, y, z0, r, g, b, 1F, z0, x0, full);

            VertexConsumer glow = buf.getBuffer(RenderType.eyes(LAVA));
            vertex(glow, m, n, x0, y, z0, 1F, 1F, 1F, 0.3F, z0, x1, full);
            vertex(glow, m, n, x0, y, z1, 1F, 1F, 1F, 0.3F, z1, x1, full);
            vertex(glow, m, n, x1, y, z1, 1F, 1F, 1F, 0.3F, z1, x0, full);
            vertex(glow, m, n, x1, y, z0, 1F, 1F, 1F, 0.3F, z0, x0, full);
        }
    }

    private static void vertex(VertexConsumer vc, org.joml.Matrix4f m, org.joml.Matrix3f n, float x, float y, float z,
                               float r, float g, float b, float a, float u, float v, int light) {
        vc.vertex(m, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 1, 0).endVertex();
    }
}
