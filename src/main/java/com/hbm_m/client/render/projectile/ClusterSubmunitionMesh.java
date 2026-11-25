package com.hbm_m.client.render.projectile;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.client.model.nmesh.NMeshData;
import com.hbm_m.client.model.nmesh.NMeshLoader;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Cached {@code MiniNuke} part of {@code fatman.obj} for the cluster warhead's
 * submunitions.
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
        RenderHooks.vertexFull(consumer, pose,
                pos.x(), pos.y(), pos.z(),
                255, 255, 255, 255,
                uv.u(), 1.0F - uv.v(),
                overlay, light,
                normal.x(), normal.y(), normal.z());
    }

    /**
     * Nmesh-first: binary {@code .nmesh} via {@link NMeshLoader} (raw .obj is
     * resourcepack-fallback inside NMeshLoader). We take only submeshes of the
     * "MiniNuke" object; triangle normal is the average of vertex normals with fallback
     * to cross product, max edge length filter preserved.
     */
    private static List<Tri> loadMiniNukeTriangles(ResourceManager rm) {
        NMeshData nmesh = NMeshLoader.load(FATMAN_OBJ);
        if (nmesh == null) {
            MainRegistry.LOGGER.warn("Cluster submunition mesh missing: {}", FATMAN_OBJ);
            return List.of();
        }

        List<Tri> tris = new ArrayList<>();
        for (NMeshData.SubMesh sm : nmesh.getSubMeshesForPart("MiniNuke")) {
            int iCount = sm.indexCount();
            for (int i = 0; i + 2 < iCount; i += 3) {
                int v0 = sm.index(i), v1 = sm.index(i + 1), v2 = sm.index(i + 2);
                Vec3 a = new Vec3(sm.posX(v0), sm.posY(v0), sm.posZ(v0));
                Vec3 b = new Vec3(sm.posX(v1), sm.posY(v1), sm.posZ(v1));
                Vec3 c = new Vec3(sm.posX(v2), sm.posY(v2), sm.posZ(v2));
                if (!isValidTriangle(a, b, c)) {
                    continue;
                }
                Vec2 ua = new Vec2(sm.u(v0), sm.v(v0));
                Vec2 ub = new Vec2(sm.u(v1), sm.v(v1));
                Vec2 uc = new Vec2(sm.u(v2), sm.v(v2));
                Vec3 na = new Vec3(sm.normX(v0), sm.normY(v0), sm.normZ(v0));
                Vec3 nb = new Vec3(sm.normX(v1), sm.normY(v1), sm.normZ(v1));
                Vec3 nc = new Vec3(sm.normX(v2), sm.normY(v2), sm.normZ(v2));
                tris.add(new Tri(a, b, c, ua, ub, uc, faceNormal(a, b, c, na, nb, nc)));
            }
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

    private static Vec3 faceNormal(Vec3 a, Vec3 b, Vec3 c, Vec3 na, Vec3 nb, Vec3 nc) {
        if (na != null && nb != null && nc != null) {
            float nx = na.x() + nb.x() + nc.x();
            float ny = na.y() + nb.y() + nc.y();
            float nz = na.z() + nb.z() + nc.z();
            float lenSq = nx * nx + ny * ny + nz * nz;
            if (lenSq > 1.0E-6F) {
                float invLen = (float) (1.0 / Math.sqrt(lenSq));
                return new Vec3(nx * invLen, ny * invLen, nz * invLen);
            }
        }
        float abx = b.x() - a.x();
        float aby = b.y() - a.y();
        float abz = b.z() - a.z();
        float acx = c.x() - a.x();
        float acy = c.y() - a.y();
        float acz = c.z() - a.z();
        float cx = aby * acz - abz * acy;
        float cy = abz * acx - abx * acz;
        float cz = abx * acy - aby * acx;
        float lenSq = cx * cx + cy * cy + cz * cz;
        if (lenSq > 1.0E-6F) {
            float invLen = (float) (1.0 / Math.sqrt(lenSq));
            return new Vec3(cx * invLen, cy * invLen, cz * invLen);
        }
        return new Vec3(0.0F, 1.0F, 0.0F);
    }

    /** Model transform, same as {@code LegoClient.RENDER_BOMB}. */
    public static void applyModelTransform(PoseStack poseStack) {
        poseStack.scale(0.0625F, 0.0625F, 0.0625F);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90.0F));
        poseStack.translate(0.0D, -1.0D, 1.0D);
    }

    /** Submunition rotation, same as {@code RenderBulletMK4} (yaw - 90 deg, pitch + 180 deg on Z). */
    public static void applyEntityRotation(PoseStack poseStack, float yaw, float pitch) {
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(pitch + 180.0F));
    }
}
