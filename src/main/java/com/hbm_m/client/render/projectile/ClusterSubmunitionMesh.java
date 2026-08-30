package com.hbm_m.client.render.projectile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Кэш части {@code MiniNuke} из {@code fatman.obj} для суббоеприпасов кластерной боеголовки.
 */
public final class ClusterSubmunitionMesh {

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/models/weapons/fatman_submunition.png");

    private static final ResourceLocation FATMAN_OBJ = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "models/weapons/fatman.obj");

    private static final float MAX_EDGE_LENGTH = 6.0F;

    private record Vec3(float x, float y, float z) {}
    private record Vec2(float u, float v) {}
    private record Tri(Vec3 a, Vec3 b, Vec3 c, Vec2 ua, Vec2 ub, Vec2 uc, Vec3 normal) {}

    private static List<Tri> triangles = List.of();

    private ClusterSubmunitionMesh() {
    }

    public static void reload(ResourceManager rm) {
        triangles = loadMiniNukeTriangles(rm);
        MainRegistry.LOGGER.debug("ClusterSubmunitionMesh: {} triangles", triangles.size());
    }

    public static void ensureLoaded() {
        if (!triangles.isEmpty()) {
            return;
        }
        reload(Minecraft.getInstance().getResourceManager());
    }

    public static void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay) {
        ensureLoaded();
        if (triangles.isEmpty()) {
            return;
        }

        PoseStack.Pose pose = poseStack.last();
        int light = packedLight == 0 ? LightTexture.FULL_BRIGHT : packedLight;
        int overlay = packedOverlay == 0 ? OverlayTexture.NO_OVERLAY : packedOverlay;

        for (Tri tri : triangles) {
            emitVertex(consumer, pose, tri.a(), tri.ua(), tri.normal(), light, overlay);
            emitVertex(consumer, pose, tri.b(), tri.ub(), tri.normal(), light, overlay);
            emitVertex(consumer, pose, tri.c(), tri.uc(), tri.normal(), light, overlay);
        }
    }

    private static void emitVertex(VertexConsumer consumer, PoseStack.Pose pose,
                                   Vec3 pos, Vec2 uv, Vec3 normal,
                                   int light, int overlay) {
        //? if < 1.21.1 {
        consumer.vertex(pose.pose(), pos.x(), pos.y(), pos.z())
                .color(255, 255, 255, 255)
                .uv(uv.u(), 1.0F - uv.v())
                .overlayCoords(overlay)
                .uv2(light)
                .normal(pose.normal(), normal.x(), normal.y(), normal.z())
                .endVertex();
        //?} else {
        /*// 1.21.1: vertex->addVertex, color->setColor, uv->setUv, overlayCoords->setOverlay, uv2->setLight,
        // normal(Vector3f,f,f,f) -> setNormal(f,f,f) с ручным умножением на pose.normal() (Matrix3f).
        // pose.normal() в 1.21.1 возвращает Matrix3f (не Vector3f) — transform'им исходный normal.
        org.joml.Matrix3f nm = pose.normal();
        org.joml.Vector3f transformed = nm.transform(new org.joml.Vector3f(
                (float) normal.x(), (float) normal.y(), (float) normal.z()));
        consumer.addVertex(pose.pose(), pos.x(), pos.y(), pos.z())
                .setColor(255, 255, 255, 255)
                .setUv(uv.u(), 1.0F - uv.v())
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(transformed.x, transformed.y, transformed.z);
        *///?}
    }

    private static List<Tri> loadMiniNukeTriangles(ResourceManager rm) {
        Resource resource = rm.getResource(FATMAN_OBJ).orElse(null);
        if (resource == null) {
            MainRegistry.LOGGER.warn("Cluster submunition OBJ missing: {}", FATMAN_OBJ);
            return List.of();
        }

        List<Vec3> positions = new ArrayList<>();
        List<Vec2> uvs = new ArrayList<>();
        List<Vec3> normals = new ArrayList<>();
        List<Tri> tris = new ArrayList<>();
        String currentObject = null;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("o ")) {
                    currentObject = line.substring(2).trim();
                    continue;
                }
                if (line.startsWith("v ")) {
                    String[] p = line.split("\\s+");
                    positions.add(new Vec3(Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])));
                    continue;
                }
                if (line.startsWith("vt ")) {
                    String[] p = line.split("\\s+");
                    uvs.add(new Vec2(Float.parseFloat(p[1]), Float.parseFloat(p[2])));
                    continue;
                }
                if (line.startsWith("vn ")) {
                    String[] p = line.split("\\s+");
                    normals.add(new Vec3(Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])));
                    continue;
                }
                if (!"MiniNuke".equals(currentObject) || !line.startsWith("f ")) {
                    continue;
                }
                if (line.startsWith("f ")) {
                    String[] parts = line.substring(2).trim().split("\\s+");
                    if (parts.length < 3) {
                        continue;
                    }
                    Vec3[] verts = new Vec3[parts.length];
                    Vec2[] tex = new Vec2[parts.length];
                    Vec3[] norms = new Vec3[parts.length];
                    for (int i = 0; i < parts.length; i++) {
                        String[] idx = parts[i].split("/");
                        int vi = parseIndex(idx[0], positions.size());
                        int vti = idx.length > 1 && !idx[1].isEmpty() ? parseIndex(idx[1], uvs.size()) : 0;
                        int vni = idx.length > 2 && !idx[2].isEmpty() ? parseIndex(idx[2], normals.size()) : 0;
                        verts[i] = positions.get(vi - 1);
                        tex[i] = vti > 0 ? uvs.get(vti - 1) : new Vec2(0, 0);
                        norms[i] = vni > 0 ? normals.get(vni - 1) : null;
                    }
                    for (int i = 1; i < parts.length - 1; i++) {
                        Vec3 normal = faceNormal(verts[0], verts[i], verts[i + 1], norms[0], norms[i], norms[i + 1]);
                        if (!isValidTriangle(verts[0], verts[i], verts[i + 1])) {
                            continue;
                        }
                        tris.add(new Tri(verts[0], verts[i], verts[i + 1], tex[0], tex[i], tex[i + 1], normal));
                    }
                }
            }
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to load cluster submunition OBJ {}", FATMAN_OBJ, e);
            return List.of();
        }

        return List.copyOf(tris);
    }

    private static boolean isValidTriangle(Vec3 a, Vec3 b, Vec3 c) {
        return edgeLength(a, b) <= MAX_EDGE_LENGTH
                && edgeLength(b, c) <= MAX_EDGE_LENGTH
                && edgeLength(c, a) <= MAX_EDGE_LENGTH;
    }

    private static float edgeLength(Vec3 a, Vec3 b) {
        float dx = a.x() - b.x();
        float dy = a.y() - b.y();
        float dz = a.z() - b.z();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static int parseIndex(String token, int size) {
        int i = Integer.parseInt(token);
        if (i < 0) {
            return size + i + 1;
        }
        return i;
    }

    private static Vec3 faceNormal(Vec3 a, Vec3 b, Vec3 c, Vec3 na, Vec3 nb, Vec3 nc) {
        if (na != null && nb != null && nc != null) {
            Vector3f n = new Vector3f(na.x() + nb.x() + nc.x(), na.y() + nb.y() + nc.y(), na.z() + nb.z() + nc.z());
            if (n.lengthSquared() > 1.0E-6F) {
                n.normalize();
                return new Vec3(n.x, n.y, n.z);
            }
        }
        Vector3f ab = new Vector3f(b.x() - a.x(), b.y() - a.y(), b.z() - a.z());
        Vector3f ac = new Vector3f(c.x() - a.x(), c.y() - a.y(), c.z() - a.z());
        ab.cross(ac);
        if (ab.lengthSquared() > 1.0E-6F) {
            ab.normalize();
        } else {
            ab.set(0, 1, 0);
        }
        return new Vec3(ab.x, ab.y, ab.z);
    }

    /** Трансформ модели как {@code LegoClient.RENDER_BOMB}. */
    public static void applyModelTransform(PoseStack poseStack) {
        poseStack.scale(0.0625F, 0.0625F, 0.0625F);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90.0F));
        poseStack.translate(0.0D, -1.0D, 1.0D);
    }

    /** Поворот суббоеприпаса как {@code RenderBulletMK4} (yaw − 90°, pitch + 180° по Z). */
    public static void applyEntityRotation(PoseStack poseStack, float yaw, float pitch) {
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(pitch + 180.0F));
    }
}
