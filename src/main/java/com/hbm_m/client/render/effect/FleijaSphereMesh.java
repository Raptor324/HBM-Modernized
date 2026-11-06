package com.hbm_m.client.render.effect;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import com.hbm_m.client.model.nmesh.NMeshData;
import com.hbm_m.client.model.nmesh.NMeshLoader;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Cached {@code sphere_new.obj} mesh for the Fleija cloud (no texture, flat color only).
 */
public final class FleijaSphereMesh {

        private static final ResourceLocation SPHERE_OBJ = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/sphere_new.obj");

    private record Tri(float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3) {}

    private static List<Tri> triangles = List.of();

    private FleijaSphereMesh() {
    }

    public static void reload(ResourceManager rm) {
        triangles = loadTriangles(rm);
        MainRegistry.LOGGER.debug("FleijaSphereMesh: {} triangles", triangles.size());
    }

    public static void ensureLoaded() {
        if (!triangles.isEmpty()) {
            return;
        }
        reload(Minecraft.getInstance().getResourceManager());
    }

    public static void renderSphere(PoseStack poseStack, VertexConsumer consumer,
                                      float r, float g, float b, float alpha) {
        ensureLoaded();
        if (triangles.isEmpty()) {
            return;
        }

        Matrix4f matrix = poseStack.last().pose();
        int light = LightTexture.FULL_BRIGHT;
        int overlay = OverlayTexture.NO_OVERLAY;

        for (Tri tri : triangles) {
            emitVertex(consumer, matrix, tri.x1(), tri.y1(), tri.z1(), r, g, b, alpha, light, overlay);
            emitVertex(consumer, matrix, tri.x2(), tri.y2(), tri.z2(), r, g, b, alpha, light, overlay);
            emitVertex(consumer, matrix, tri.x3(), tri.y3(), tri.z3(), r, g, b, alpha, light, overlay);
        }
    }

    private static void emitVertex(VertexConsumer consumer, Matrix4f matrix,
                                   float x, float y, float z,
                                   float r, float g, float b, float a,
                                   int light, int overlay) {
        com.hbm_m.platform.RenderHooks.vertexFull(consumer, matrix, x, y, z, r, g, b, a, 0, 0, overlay, light, 0, 1, 0);
    }

    /**
     * Nmesh-first: binary {@code .nmesh} (or runtime resourcepack .obj via NMeshLoader).
     * UV and normals are ignored - the cloud renders with flat color and fixed normal (0,1,0).
     */
    private static List<Tri> loadTriangles(ResourceManager rm) {
        NMeshData nmesh = NMeshLoader.load(SPHERE_OBJ);
        if (nmesh == null) {
            MainRegistry.LOGGER.warn("Fleija sphere mesh missing: {}", SPHERE_OBJ);
            return List.of();
        }

        List<Tri> tris = new ArrayList<>();
        for (NMeshData.SubMesh sm : nmesh.subMeshes()) {
            float[] verts = sm.vertices;
            int[] idx = sm.indices;
            for (int i = 0; i + 2 < idx.length; i += 3) {
                int b0 = idx[i] * 8, b1 = idx[i + 1] * 8, b2 = idx[i + 2] * 8;
                tris.add(new Tri(
                        verts[b0], verts[b0 + 1], verts[b0 + 2],
                        verts[b1], verts[b1 + 1], verts[b1 + 2],
                        verts[b2], verts[b2 + 1], verts[b2 + 2]));
            }
        }
        return List.copyOf(tris);
    }
}
