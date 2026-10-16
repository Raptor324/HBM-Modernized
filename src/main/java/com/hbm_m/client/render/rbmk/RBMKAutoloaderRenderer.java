package com.hbm_m.client.render.rbmk;

import com.hbm_m.blockentity.machines.rbmk.RBMKAutoloaderBlockEntity;
import com.hbm_m.client.render.implementations.RBMKColumnRenderer;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.List;
import java.util.Map;

/**
 * 1:1 port of {@code RenderRBMKAutoloader}.
 *
 * <p>The autoloader is not a reactor column and must not be drawn as one. It was previously routed
 * through {@link RBMKColumnRenderer}, so it appeared as a four-block-tall blank column and its
 * {@code autoloader.obj} - a static {@code Base} and a {@code Piston} that travels four blocks down
 * into the fuel channel - was never drawn at all.</p>
 *
 * <p>The piston position interpolates between {@code lastPiston} and {@code piston} across the
 * partial tick, exactly as the original does, so the 200-tick travel reads as smooth motion rather
 * than twenty steps a second.</p>
 */
public class RBMKAutoloaderRenderer implements BlockEntityRenderer<RBMKAutoloaderBlockEntity> {

    private static final String MODEL_PATH = "models/rbmk/models/autoloader.obj";

    public RBMKAutoloaderRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(RBMKAutoloaderBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf,
                       int packedLight, int packedOverlay) {

        Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(MODEL_PATH);
        if (obj.isEmpty()) return;

        TextureAtlasSprite sprite = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/rbmk/model_rbmk_autoloader");
        VertexConsumer vc = buf.getBuffer(RenderType.solid());

        ps.pushPose();
        ps.translate(0.5, 0, 0.5);

        List<float[]> base = obj.get("Base");
        if (base != null)
            RBMKColumnRenderer.renderObjGroup(vc, ps.last().pose(), base, sprite, 1f, 1f, 1f, packedLight, packedOverlay);

        // p = 0 leaves the piston parked four blocks up; p = 1 has it fully driven down.
        double p = be.lastPiston + (be.piston - be.lastPiston) * pt;
        ps.translate(0, 4.0 - p * 4.0, 0);

        List<float[]> piston = obj.get("Piston");
        if (piston != null)
            RBMKColumnRenderer.renderObjGroup(vc, ps.last().pose(), piston, sprite, 1f, 1f, 1f, packedLight, packedOverlay);

        ps.popPose();
    }

}
