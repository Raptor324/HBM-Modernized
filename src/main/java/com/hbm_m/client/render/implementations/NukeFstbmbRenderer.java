package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.bomb.NukeFstbmbBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.render.RenderHelperA;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Dynamischer Teil von {@code RenderNukeFstbmb}: Body und Balefire zeichnet das Blockmodell
 * ({@code block/bomb/nuke_fstbmb_placed}), hier kommen nur der Balefire-Schimmer
 * ({@code renderClassicGlint(..., "Balefire", 0.0F, 0.8F, 0.15F, 5, 2F)}) und die rote Zeitanzeige dazu,
 * beides nur, wenn die Bombe geladen ist. Gezeichnet wird im Rahmen des Blockmodells (dessen Drehung je FACING).
 */
public class NukeFstbmbRenderer implements BlockEntityRenderer<NukeFstbmbBlockEntity> {

    private static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, p); }

    public static final SimpleObjModel MODEL = new SimpleObjModel(rl("models/block/bomb/fstbmb.obj"));
    public static final ResourceLocation TEX = rl("textures/block/bomb/nuke_fstbmb.png");

    public NukeFstbmbRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(NukeFstbmbBlockEntity bf, float f, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        if (!bf.loaded) return;

        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // gleiche Lage wie das Blockmodell (horizontalBlock: north 0, east y90, south y180, west y270)
        Direction facing = bf.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? bf.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
        switch (facing) {
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(-90));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(-180));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(-270));
            default -> { }
        }

        // Balefire noch einmal in den Tiefenpuffer, damit der GL_EQUAL-Schimmer sicher deckungsgleich liegt
        MODEL.renderPartEntity("Balefire", ps, buffers.getBuffer(RenderType.entityCutoutNoCull(TEX)), light, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);

        // RenderMiscEffects.renderClassicGlint(world, f, fstbmb, "Balefire", 0.0F, 0.8F, 0.15F, 5, 2F)
        float offset = (Minecraft.getInstance().player != null ? Minecraft.getInstance().player.tickCount : 0) + f;
        float glintColor = 0.76F;
        GunGL.begin(ps, buffers, light);
        for (int k = 0; k < 2; ++k) {
            float movement = offset * (0.001F + (float) k * 0.003F) * 5F;
            RenderHelperA.renderGlintPart(MODEL, "Balefire", RenderHelperA.glintBF,
                    0.0F * glintColor, 0.8F * glintColor, 0.15F * glintColor, 2F, 30.0F - (float) k * 60.0F, movement);
        }
        GunGL.end();

        Font font = Minecraft.getInstance().font;
        float f3 = 0.04F;
        ps.translate(0.815F, 0.9275F, 0.5F);
        ps.scale(f3, -f3, f3);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        ps.translate(0, 1, 0);
        // glDepthMask(false): Schrift liegt auf der Oberflaeche -> Polygon-Offset statt Z-Kampf
        font.drawInBatch(bf.getMinutes() + ":" + bf.getSeconds(), 0, 0, 0xFFFF0000, false, ps.last().pose(), buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0, light);

        ps.popPose();
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
