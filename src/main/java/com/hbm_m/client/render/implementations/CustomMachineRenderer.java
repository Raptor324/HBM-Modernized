package com.hbm_m.client.render.implementations;

import org.joml.Matrix4f;

import com.hbm_m.blockentity.machines.custom.CustomMachineBlockEntity;
import com.hbm_m.config.CustomMachineConfigJSON.MachineConfiguration.ComponentDefinition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;

/**
 * 1:1 {@code RenderCustomMachine}: solange der Bauplan nicht steht, zeigt die Steuerung jedes Bauteil als
 * durchscheinenden Miniwuerfel (SmallBlockPronter); bei mehreren erlaubten Bloecken wechselt die Vorschau sekuendlich.
 */
public class CustomMachineRenderer implements com.hbm_m.client.render.HbmBerBounds<CustomMachineBlockEntity> {

    public CustomMachineRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(CustomMachineBlockEntity custom, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        if (custom.config == null || custom.structureOK) return;

        Matrix4f m = ps.last().pose();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS));
        BlockPos origin = custom.getBlockPos();

        for (ComponentDefinition comp : custom.config.components) {
            if (comp.blocks.isEmpty()) continue;
            BlockPos p = custom.componentPos(comp);
            int index = (int) ((System.currentTimeMillis() / 1000) % comp.blocks.size());
            Block b = comp.blocks.get(index);
            TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer().getBlockModel(b.defaultBlockState()).getParticleIcon();
            StructSoyuzCoreRenderer.cube(vc, m, sprite, p.getX() - origin.getX(), p.getY() - origin.getY(), p.getZ() - origin.getZ(), light, overlay);
        }
    }

    @Override public int getViewDistance() { return 256; }
    @Override public boolean shouldRenderOffScreen(CustomMachineBlockEntity be) { return true; }
}
