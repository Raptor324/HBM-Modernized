package com.hbm_m.client.render.implementations;

import java.util.List;
import java.util.Map;

import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared bits of the renderers that draw the moving parts of a 1.7.10 OBJ machine whose static
 * part sits in the block model ({@code models/block/machines/<name>.json}).
 *
 * <p>The block model uses {@code <name>_static.obj} with the NORTH facing baked in; the renderer
 * reads the untouched {@code <name>.obj}, so the original's fixed pre-rotation (the
 * {@code glRotatef} before its facing switch) is applied here and the original's local pivots
 * and axes can be copied verbatim.
 */
final class ObjMachines {

    private ObjMachines() {}

    static Map<String, List<float[]>> obj(String name) {
        return RBMKColumnRenderer.getObj("models/block/machines/" + name + ".obj");
    }

    static TextureAtlasSprite sprite(String texture) {
        return RBMKColumnRenderer.sprite(RefStrings.MODID, "block/machine/" + texture);
    }

    /** Block centre, blockstate facing, then the original's fixed pre-rotation about Y. */
    static void begin(PoseStack pose, BlockState state, float preRotation) {
        pose.translate(0.5D, 0D, 0.5D);
        FusionTorusRenderer.applyFacing(state, pose);
        if (preRotation != 0F) pose.mulPose(Axis.YP.rotationDegrees(preRotation));
    }

    static void group(VertexConsumer vc, PoseStack pose, Map<String, List<float[]>> obj, String group,
                      TextureAtlasSprite sprite, int light, int overlay) {
        group(vc, pose, obj, group, sprite, 1F, 1F, 1F, light, overlay);
    }

    static void group(VertexConsumer vc, PoseStack pose, Map<String, List<float[]>> obj, String group,
                      TextureAtlasSprite sprite, float r, float g, float b, int light, int overlay) {
        List<float[]> tris = obj.get(group);
        if (tris == null) return;
        RBMKColumnRenderer.renderObjGroup(vc, pose.last().pose(), tris, sprite, r, g, b, light, overlay);
    }
}
