package com.hbm_m.client.render.rbmk;

import com.hbm_m.block.machines.rbmk.RBMKMiniPanelBlock;
import com.hbm_m.blockentity.machines.rbmk.RBMKPanelDeviceBlockEntity;
import com.hbm_m.client.render.implementations.RBMKColumnRenderer;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.Map;

/**
 * Shared plumbing for the RBMK control-room panel renderers, ported from the eight
 * {@code RenderRBMK*} tile entity special renderers in the original.
 *
 * <p>Every one of them opens the same way - translate to the block's horizontal center, then
 * rotate by the panel's facing - and then places its units at local {@code x = 0.25}, which after
 * the rotation lands exactly on the panel's recessed face (see {@link RBMKMiniPanelBlock} for the
 * facing/bounds mapping). The original's metadata-to-angle switch (2→90°, 4→180°, 3→270°, 5→0°)
 * is reproduced by {@link #facingAngle(Direction)}.</p>
 */
public abstract class RBMKPanelRenderer<T extends RBMKPanelDeviceBlockEntity>
        implements BlockEntityRenderer<T> {

    /** Original: {@code RenderArcFurnace.fullbright(true)} - lightmap pinned to maximum. */
    protected static final int FULLBRIGHT = LightTexture.FULL_BRIGHT;

    /** Panel body/device OBJ models all live under this folder in the port. */
    private static final String MODEL_ROOT = "models/rbmk/models/";

    /**
     * The original's per-metadata rotation, expressed against the modern FACING property:
     * NORTH(2)→90°, WEST(4)→180°, SOUTH(3)→270°, EAST(5)→0°.
     */
    protected static float facingAngle(Direction facing) {
        return switch (facing) {
            case NORTH -> 90f;
            case WEST  -> 180f;
            case SOUTH -> 270f;
            default    -> 0f;
        };
    }

    protected static Direction facingOf(RBMKPanelDeviceBlockEntity be) {
        var state = be.getBlockState();
        return state.hasProperty(RBMKMiniPanelBlock.FACING)
                ? state.getValue(RBMKMiniPanelBlock.FACING)
                : Direction.NORTH;
    }

    protected static Map<String, List<float[]>> model(String name) {
        return RBMKColumnRenderer.getObj(MODEL_ROOT + name + ".obj");
    }

    protected static TextureAtlasSprite panelSprite(String name) {
        return RBMKColumnRenderer.sprite(RefStrings.MODID, "block/rbmk/panel/" + name);
    }

    /** Draws one named OBJ group with a flat tint, matching the original's {@code renderPart}. */
    protected static void part(PoseStack ps, MultiBufferSource buf, Map<String, List<float[]>> obj,
                                String group, TextureAtlasSprite sprite,
                                float r, float g, float b, int light, int overlay) {
        List<float[]> mesh = obj.get(group);
        if (mesh == null) return;
        VertexConsumer vc = buf.getBuffer(RenderType.solid());
        RBMKColumnRenderer.renderObjGroup(vc, ps.last().pose(), mesh, sprite, r, g, b, light, overlay);
    }

    /** Draws every group of a model, matching the original's {@code renderAll}. */
    protected static void all(PoseStack ps, MultiBufferSource buf, Map<String, List<float[]>> obj,
                               TextureAtlasSprite sprite, float r, float g, float b, int light, int overlay) {
        VertexConsumer vc = buf.getBuffer(RenderType.solid());
        for (List<float[]> mesh : obj.values())
            RBMKColumnRenderer.renderObjGroup(vc, ps.last().pose(), mesh, sprite, r, g, b, light, overlay);
    }

    protected static Font font() {
        return Minecraft.getInstance().font;
    }

    /**
     * A single quad in the panel's face plane (constant local X) with an explicit UV sub-rect on
     * the given sprite. The original drew these straight onto a bound texture with raw 0-1 UVs;
     * here the same fractions are remapped into the sprite's slot in the block atlas.
     */
    protected static void uvQuad(PoseStack ps, MultiBufferSource buf, TextureAtlasSprite sprite,
                                  float x, float y0, float y1, float z0, float z1,
                                  float u0, float u1, float v0, float v1,
                                  float r, float g, float b, int light, int overlay) {
        VertexConsumer vc = buf.getBuffer(RenderType.solid());
        var m = ps.last().pose();
        float au0 = sprite.getU0() + u0 * (sprite.getU1() - sprite.getU0());
        float au1 = sprite.getU0() + u1 * (sprite.getU1() - sprite.getU0());
        float av0 = sprite.getV0() + v0 * (sprite.getV1() - sprite.getV0());
        float av1 = sprite.getV0() + v1 * (sprite.getV1() - sprite.getV0());
        vc.vertex(m, x, y0, z0).color(r, g, b, 1f).uv(au0, av1).overlayCoords(overlay).uv2(light).normal(1, 0, 0).endVertex();
        vc.vertex(m, x, y1, z0).color(r, g, b, 1f).uv(au0, av0).overlayCoords(overlay).uv2(light).normal(1, 0, 0).endVertex();
        vc.vertex(m, x, y1, z1).color(r, g, b, 1f).uv(au1, av0).overlayCoords(overlay).uv2(light).normal(1, 0, 0).endVertex();
        vc.vertex(m, x, y0, z1).color(r, g, b, 1f).uv(au1, av1).overlayCoords(overlay).uv2(light).normal(1, 0, 0).endVertex();
    }

    /** One line segment, replacing the original's {@code GL_LINES} tessellator batch. */
    protected static void line(PoseStack ps, MultiBufferSource buf,
                                float x0, float y0, float z0, float x1, float y1, float z1,
                                float r, float g, float b) {
        VertexConsumer vc = buf.getBuffer(RenderType.lines());
        var pose = ps.last();
        vc.vertex(pose.pose(), x0, y0, z0).color(r, g, b, 1f).normal(pose.normal(), 1, 0, 0).endVertex();
        vc.vertex(pose.pose(), x1, y1, z1).color(r, g, b, 1f).normal(pose.normal(), 1, 0, 0).endVertex();
    }

    /**
     * The label-drawing block every panel repeats verbatim: scale down, face outwards along the
     * panel normal, and draw the string centered. The original does
     * {@code glScalef(f3,-f3,f3); glRotatef(90,0,1,0); font.drawString(label, -width/2, -height/2, color)}
     * - the Y flip is what turns Minecraft's downward-growing text space the right way up in world
     * space, and is reproduced here by the negative Y scale.
     */
    protected static void label(PoseStack ps, MultiBufferSource buf, String text,
                                 float maxWidth, int color, int light) {
        if (text == null || text.isEmpty()) return;
        Font font = font();
        int width = font.width(text);
        float scale = Math.min(0.0125f, maxWidth / Math.max(width, 1));

        ps.pushPose();
        ps.scale(scale, -scale, scale);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        font.drawInBatch(text, -width / 2f, -font.lineHeight / 2f, color, false,
                ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, light);
        ps.popPose();
    }

    /** Left-aligned variant used by the gauge scale ticks and the graph's bound labels. */
    protected static void labelRaw(PoseStack ps, MultiBufferSource buf, String text,
                                    float scale, float x, int color, int light) {
        if (text == null || text.isEmpty()) return;
        Font font = font();
        ps.pushPose();
        ps.scale(scale, -scale, scale);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        font.drawInBatch(text, x, -font.lineHeight / 2f, color, false,
                ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, light);
        ps.popPose();
    }

    protected static float red(int rgb)   { return ((rgb >> 16) & 0xFF) / 255f; }
    protected static float green(int rgb) { return ((rgb >>  8) & 0xFF) / 255f; }
    protected static float blue(int rgb)  { return ( rgb        & 0xFF) / 255f; }

    /** Opens the shared transform: block center, then the facing rotation. */
    protected void begin(T be, PoseStack ps) {
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(facingAngle(facingOf(be))));
    }
}
