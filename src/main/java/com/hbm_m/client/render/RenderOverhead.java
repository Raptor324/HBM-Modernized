package com.hbm_m.client.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Port der Marker aus {@code RenderOverhead} (1.7.10): Drahtgitterkaesten ohne Tiefentest, die der
 * Server ueber den Partikeltyp "marker" setzt (Satelliten-/Radar-/Drohnen-Ziele usw.), mit
 * Beschriftung und - sobald man genau hinschaut - der Entfernung in Metern.
 */
public final class RenderOverhead {

    private RenderOverhead() {}

    public static final Map<BlockPos, Marker> queuedMarkers = new HashMap<>();
    private static final Map<BlockPos, Marker> markers = new HashMap<>();

    public static void renderMarkers(PoseStack pose, Camera camera) {
        markers.putAll(queuedMarkers);
        queuedMarkers.clear();
        if (markers.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Vec3 cam = camera.getPosition();
        double x = cam.x, y = cam.y, z = cam.z;

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = com.hbm_m.platform.RenderHooks.beginTesselator(tess, VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f m = pose.last().pose();

        Iterator<Entry<BlockPos, Marker>> it = markers.entrySet().iterator();
        List<Entry<BlockPos, Marker>> tagList = new ArrayList<>();
        while (it.hasNext()) {
            Entry<BlockPos, Marker> entry = it.next();
            BlockPos pos = entry.getKey();
            Marker mk = entry.getValue();
            int r = (mk.color >> 16) & 0xFF, g = (mk.color >> 8) & 0xFF, b = mk.color & 0xFF;
            float x0 = (float) (pos.getX() + mk.minX - x), y0 = (float) (pos.getY() + mk.minY - y), z0 = (float) (pos.getZ() + mk.minZ - z);
            float x1 = (float) (pos.getX() + mk.maxX - x), y1 = (float) (pos.getY() + mk.maxY - y), z1 = (float) (pos.getZ() + mk.maxZ - z);
            float[][] l = {
                    { x0, y1, z0, x0, y0, z0 }, { x0, y1, z0, x1, y1, z0 }, { x1, y1, z0, x1, y0, z0 }, { x0, y0, z0, x1, y0, z0 },
                    { x1, y0, z0, x1, y0, z1 }, { x1, y1, z1, x1, y1, z0 }, { x1, y1, z1, x1, y0, z1 }, { x0, y1, z0, x0, y1, z1 },
                    { x0, y1, z1, x0, y0, z1 }, { x0, y1, z1, x1, y1, z1 }, { x0, y0, z1, x1, y0, z1 }, { x0, y0, z0, x0, y0, z1 } };
            for (float[] s : l) {
                //? if < 1.21.1 {
                buf.vertex(m, s[0], s[1], s[2]).color(r, g, b, 255).endVertex();
                buf.vertex(m, s[3], s[4], s[5]).color(r, g, b, 255).endVertex();
                //?} else {
                /*buf.addVertex(m, s[0], s[1], s[2]).setColor(r, g, b, 255);
                buf.addVertex(m, s[3], s[4], s[5]).setColor(r, g, b, 255);
                *///?}
            }
            tagList.add(entry);

            if (mk.expire > 0 && System.currentTimeMillis() > mk.expire) {
                it.remove();
            } else if (mk.maxDist > 0) {
                double aX = pos.getX() + (mk.maxX - mk.minX) / 2D;
                double aY = pos.getY() + (mk.maxY - mk.minY) / 2D;
                double aZ = pos.getZ() + (mk.maxZ - mk.minZ) / 2D;
                if (new Vec3(x - aX, y - aY, z - aZ).length() > mk.maxDist) it.remove();
            }
        }
        com.hbm_m.platform.RenderHooks.drawWithShader(buf);
        RenderSystem.enableDepthTest();

        Vec3 look = mc.player.getViewVector(1F);
        MultiBufferSource.BufferSource src = mc.renderBuffers().bufferSource();
        for (Entry<BlockPos, Marker> entry : tagList) {
            BlockPos pos = entry.getKey();
            Marker mk = entry.getValue();
            double aX = pos.getX() + (mk.maxX - mk.minX) / 2D;
            double aY = pos.getY() + (mk.maxY - mk.minY) / 2D;
            double aZ = pos.getZ() + (mk.maxZ - mk.minZ) / 2D;
            Vec3 vec = new Vec3(aX - x, aY - y, aZ - z);
            double len = vec.lengthSqr();
            double sqrt = Math.sqrt(len);
            double mult = Math.min(sqrt, 16D);
            vec = vec.scale(mult / sqrt);
            Vec3 diff = vec.normalize();
            String label = mk.label == null ? "" : mk.label;
            if (Math.abs(look.x - diff.x) + Math.abs(look.y - diff.y) + Math.abs(look.z - diff.z) < 0.15) {
                label += (!label.isEmpty() ? " " : "") + ((int) sqrt) + "m";
            }
            if (!label.isEmpty()) drawTag(pose, camera, src, 1F, len, label, vec.x, vec.y, vec.z, 100, true, mk.color, mk.color);
        }
        src.endBatch();
        RenderSystem.disableBlend();
    }

    /** 1:1-Port von {@code RenderOverhead.drawTag(offset, distsq, ...)}. */
    public static void drawTag(PoseStack pose, Camera camera, MultiBufferSource src, float offset, double distsq, String name,
                               double x, double y, double z, int dist, boolean depthTest, int color, int shadowColor) {
        if (distsq > (double) (dist * dist)) return;
        Font font = Minecraft.getInstance().font;
        float scale = 0.016666668F * 1.6F;
        pose.pushPose();
        pose.translate(x, y + offset, z);
        pose.mulPose(camera.rotation());
        pose.scale(-scale, -scale, scale);
        Matrix4f m = pose.last().pose();
        int heightOffset = name.equals("deadmau5") ? -10 : 0;
        float w = -font.width(name) / 2F;
        int bg = (int) (0.25F * 255.0F) << 24;
        font.drawInBatch(name, w, heightOffset, shadowColor | 0x20000000, false, m, src,
                depthTest ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, bg, 0xF000F0);
        font.drawInBatch(name, w, heightOffset, color | 0xFF000000, false, m, src, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        pose.popPose();
    }

    public static class Marker {
        double minX = 0, minY = 0, minZ = 0;
        double maxX = 1, maxY = 1, maxZ = 1;
        int color;
        String label;
        long expire;
        double maxDist;

        public Marker(int color) { this.color = color; }
        public Marker setExpire(long expire) { this.expire = expire; return this; }
        public Marker setDist(double maxDist) { this.maxDist = maxDist; return this; }
        public Marker withLabel(String label) { this.label = label; return this; }
    }
}
