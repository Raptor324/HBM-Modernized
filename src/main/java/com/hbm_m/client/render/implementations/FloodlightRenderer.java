package com.hbm_m.client.render.implementations;

import com.hbm_m.block.decorations.FloodlightBlock;
import com.hbm_m.blockentity.decorations.FloodlightBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderFloodlight}: Sockel nach Befestigungsseite gedreht, Leuchtenkopf um die Neigung, Lampen im Betrieb
 * voll hell, aus mit 25 % Farbe.
 */
public class FloodlightRenderer implements com.hbm_m.client.render.HbmBerBounds<FloodlightBlockEntity> {

    public static final SimpleObjModel FLOODLIGHT = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/floodlight.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/floodlight.png");

    public FloodlightRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(FloodlightBlockEntity tile, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0.5D, 0.5D);

        int meta = tile.getBlockState().getValue(FloodlightBlock.META);
        switch (meta) {
            case 0: case 6: ps.mulPose(Axis.XP.rotationDegrees(180)); break;
            case 1: case 7: break;
            case 2: ps.mulPose(Axis.XP.rotationDegrees(90)); ps.mulPose(Axis.ZP.rotationDegrees(180)); break;
            case 3: ps.mulPose(Axis.XP.rotationDegrees(90)); break;
            case 4: ps.mulPose(Axis.XP.rotationDegrees(90)); ps.mulPose(Axis.ZP.rotationDegrees(90)); break;
            case 5: ps.mulPose(Axis.XP.rotationDegrees(90)); ps.mulPose(Axis.ZP.rotationDegrees(270)); break;
            default: break;
        }

        ps.translate(0, -0.5, 0);
        if (meta != 0 && meta != 1) ps.mulPose(Axis.YP.rotationDegrees(90));

        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(TEX));
        FLOODLIGHT.renderPart("Base", ps, vc, light);

        float rotation = tile.rotation;
        if (meta == 0 || meta == 6) rotation -= 90;
        if (meta == 1 || meta == 7) rotation += 90;
        ps.translate(0, 0.5, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(rotation));
        ps.translate(0, -0.5, 0);
        FLOODLIGHT.renderPart("Lights", ps, vc, light);

        if (tile.isOn) {
            FLOODLIGHT.renderPart("Lamps", ps, vc, LightTexture.FULL_BRIGHT);
        } else {
            FLOODLIGHT.renderPartTinted("Lamps", ps, vc, light, 0.25F, 0.25F, 0.25F);
        }

        ps.popPose();
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
