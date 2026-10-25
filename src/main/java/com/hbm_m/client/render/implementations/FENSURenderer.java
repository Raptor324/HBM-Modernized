package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineFENSUBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderFENSU}: Sockel, um die X-Achse (Hoehe 2,5) drehende Scheibe ({@code rotation/prevRotation}) und
 * die mitdrehenden, vollhellen Lichter ohne Flaechenausblendung.
 */
public class FENSURenderer implements com.hbm_m.client.render.HbmBerBounds<MachineFENSUBlockEntity> {

    private static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, p); }

    public static final SimpleObjModel MODEL = new SimpleObjModel(rl("models/block/machines/fensu.obj"));
    public static final ResourceLocation TEX = rl("textures/block/machine/fensu.png");

    public FENSURenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineFENSUBlockEntity fensu, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 2/4/3/5 -> 90/180/270/0 Grad
        switch (fensu.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        MODEL.renderPart("Base", ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);

        float rot = fensu.prevRotation + (fensu.rotation - fensu.prevRotation) * f;

        ps.translate(0, 2.5, 0);
        ps.mulPose(Axis.XP.rotationDegrees(rot));
        ps.translate(0, -2.5, 0);
        MODEL.renderPart("Disc", ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);

        // GL_LIGHTING/GL_CULL_FACE aus, Lichtkarte 240/240
        MODEL.renderPart("Lights", ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), LightTexture.FULL_BRIGHT);

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineFENSUBlockEntity be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
