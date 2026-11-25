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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Port of markers from {@code RenderOverhead} (1.7.10): wireframe boxes rendered without depth testing,
 * placed by the server via particle type "marker" (satellite, radar, and drone targets, etc.),
 * with text labels and distance in meters when looked at directly.
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
                com.hbm_m.platform.RenderHooks.vertexColor(buf, m, s[0], s[1], s[2], r, g, b, 255);
                com.hbm_m.platform.RenderHooks.vertexColor(buf, m, s[3], s[4], s[5], r, g, b, 255);
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

    private static final TagKey<EntityType<?>> BOSSES_TAG_FORGE =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("forge", "bosses"));
    private static final TagKey<EntityType<?>> BOSSES_TAG_C =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "bosses"));

    public static boolean isBoss(Entity ent) {
        if (ent.getType().is(BOSSES_TAG_FORGE) || ent.getType().is(BOSSES_TAG_C)) {
            return true;
        }
        for (String tag : ent.getTags()) {
            if ("boss".equalsIgnoreCase(tag)) return true;
        }
        if (ent instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                || ent instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                || ent instanceof net.minecraft.world.entity.boss.EnderDragonPart
                || ent instanceof com.hbm_m.entity.mob.EntityHunterChopper
                || ent instanceof com.hbm_m.entity.mob.EntityMaskMan
                || ent instanceof com.hbm_m.entity.mob.EntityQuackos
                || ent instanceof com.hbm_m.entity.mob.EntityUFO
                || ent instanceof com.hbm_m.entity.mob.botprime.EntityBOTPrimeHead
                || ent instanceof com.hbm_m.entity.mob.botprime.EntityBOTPrimeBase) {
            return true;
        }
        if (ent instanceof Enemy && !(ent instanceof Monster
                || ent instanceof net.minecraft.world.entity.monster.Slime
                || ent instanceof net.minecraft.world.entity.monster.Ghast
                || ent instanceof net.minecraft.world.entity.monster.Phantom
                || ent instanceof net.minecraft.world.entity.monster.Shulker
                || ent instanceof net.minecraft.world.entity.monster.hoglin.Hoglin)) {
            return true;
        }
        return false;
    }

    /**
     * 1:1 port of {@code RenderOverhead.renderThermalSight(partialTicks)} from 1.7.10.
     * Renders entity bounding box wireframes with color coding within 64 blocks without depth test.
     */
    public static void renderThermalSight(PoseStack pose, Camera camera, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Player player = mc.player;
        Vec3 cam = camera.getPosition();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = com.hbm_m.platform.RenderHooks.beginTesselator(tess, VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f m = pose.last().pose();

        for (Entity ent : mc.level.entitiesForRendering()) {
            if (ent == null || ent == player) continue;
            if (player.distanceToSqr(ent) > 4096.0D) continue;

            float r, g, b;
            if (isBoss(ent)) {
                r = 1.0F; g = 0.5F; b = 0.0F;
            } else if (ent instanceof Monster || ent instanceof Enemy) {
                r = 1.0F; g = 0.0F; b = 0.0F;
            } else if (ent instanceof Player) {
                r = 1.0F; g = 0.0F; b = 1.0F;
            } else if (ent instanceof LivingEntity) {
                r = 0.0F; g = 1.0F; b = 0.0F;
            } else if (ent instanceof ItemEntity) {
                r = 1.0F; g = 1.0F; b = 0.5F;
            } else if (ent instanceof ExperienceOrb) {
                if (player.tickCount % 10 < 5) {
                    r = 1.0F; g = 1.0F; b = 0.5F;
                } else {
                    r = 0.5F; g = 1.0F; b = 0.5F;
                }
            } else {
                continue;
            }

            if (!ent.isAlive() || (ent instanceof LivingEntity living && living.getHealth() <= 0)) {
                r = 0.0F; g = 0.0F; b = 0.0F;
            }

            double dx = Mth.lerp(partialTicks, ent.xOld, ent.getX()) - ent.getX();
            double dy = Mth.lerp(partialTicks, ent.yOld, ent.getY()) - ent.getY();
            double dz = Mth.lerp(partialTicks, ent.zOld, ent.getZ()) - ent.getZ();
            AABB bb = ent.getBoundingBox().move(dx, dy, dz);

            float x0 = (float) (bb.minX - cam.x);
            float y0 = (float) (bb.minY - cam.y);
            float z0 = (float) (bb.minZ - cam.z);
            float x1 = (float) (bb.maxX - cam.x);
            float y1 = (float) (bb.maxY - cam.y);
            float z1 = (float) (bb.maxZ - cam.z);

            float[][] l = {
                    { x0, y1, z0, x0, y0, z0 }, { x0, y1, z0, x1, y1, z0 }, { x1, y1, z0, x1, y0, z0 }, { x0, y0, z0, x1, y0, z0 },
                    { x1, y0, z0, x1, y0, z1 }, { x1, y1, z1, x1, y1, z0 }, { x1, y1, z1, x1, y0, z1 }, { x0, y1, z0, x0, y1, z1 },
                    { x0, y1, z1, x0, y0, z1 }, { x0, y1, z1, x1, y1, z1 }, { x0, y0, z1, x1, y0, z1 }, { x0, y0, z0, x0, y0, z1 }
            };

            for (float[] s : l) {
                com.hbm_m.platform.RenderHooks.vertexColor(buf, m, s[0], s[1], s[2], r, g, b, 1.0F);
                com.hbm_m.platform.RenderHooks.vertexColor(buf, m, s[3], s[4], s[5], r, g, b, 1.0F);
            }
        }

        com.hbm_m.platform.RenderHooks.drawWithShader(buf);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /** 1:1 port of {@code RenderOverhead.drawTag(offset, distsq, ...)}. */
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
