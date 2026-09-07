package com.hbm_m.client.render.implementations;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.network.RedCablePaintableBlockEntity;
import com.hbm_m.client.ClientRenderHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Рендер кабеля-камуфляжа (RedCablePaintableBlock):
 * рисует замаскированный блок его настоящей моделью (renderSingleBlock),
 * поверх — полупрозрачный куб red_cable_overlay как в multipass-пассе 1.7.10.
 * Без окраски — красный базовый куб (порт red_cable_base).
 */
public class RedCablePaintableRenderer implements BlockEntityRenderer<RedCablePaintableBlockEntity> {

    private static final ResourceLocation OVERLAY_TEX = rl("hbm_m", "block/red_cable_overlay");

    private static ResourceLocation rl(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
    private static final float EPS = 0.002F;
    private static final float VEIL_ALPHA = 0.35F;

    public RedCablePaintableRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(RedCablePaintableBlockEntity be, float partialTick, com.mojang.blaze3d.vertex.PoseStack poseStack,
                       MultiBufferSource buffer, int light, int overlay) {
        BlockState camo = be.getCamo();
        BlockState shown = camo != null ? camo : ModBlocks.RED_WIRE_COATED.get().defaultBlockState();
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(shown, poseStack, buffer, light, overlay);

        // Подсказка-оверлей (pass 1 оригинала): едва заметная вуаль на всех гранях.
        if (camo == null) return;
        TextureAtlasSprite sprite =
                Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(OVERLAY_TEX);
        var consumer = buffer.getBuffer(ClientRenderHandler.CustomRenderTypes.PYLON_OVERLAY);
        var pose = poseStack.last();
        float a = -EPS, b = 1 + EPS;
        face(consumer, pose, sprite, a, a, a, b, a, a, b, b, a, a, b, a);
        face(consumer, pose, sprite, b, a, b, a, a, b, a, b, b, b, b, a);
        face(consumer, pose, sprite, b, a, a, b, a, b, b, b, b, b, b, a);
        face(consumer, pose, sprite, a, a, b, a, a, a, a, b, a, a, b, a);
        face(consumer, pose, sprite, a, b, a, a, b, b, b, b, b, b, b, a);
        face(consumer, pose, sprite, a, a, a, a, a, b, b, a, b, b, a, a);
    }

    private static void face(com.mojang.blaze3d.vertex.VertexConsumer consumer, com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                             TextureAtlasSprite sprite,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        vert(consumer, pose, sprite, x0, y0, z0, 0, 0);
        vert(consumer, pose, sprite, x1, y1, z1, 0, 1);
        vert(consumer, pose, sprite, x2, y2, z2, 1, 1);
        vert(consumer, pose, sprite, x3, y3, z3, 1, 0);
    }

    private static void vert(com.mojang.blaze3d.vertex.VertexConsumer consumer, com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                             TextureAtlasSprite sprite, float x, float y, float z, float u, float v) {
        float uu = sprite.getU(u);
        float vv = sprite.getV(v);
        //? if < 1.21.1 {
        consumer.vertex(pose.pose(), x, y, z).color(1F, 1F, 1F, VEIL_ALPHA).uv(uu, vv).endVertex();
        //?} else {
        /*consumer.addVertex(pose, x, y, z).setColor(1F, 1F, 1F, VEIL_ALPHA).setUv(uu, vv);
        *///?}
    }
}
