package com.hbm_m.client.render.effect;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
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
 * Кэш {@code sphere_new.obj} для Fleija-облака (без текстуры, только цвет).
 */
public final class FleijaSphereMesh {

    //? if fabric && < 1.21.1 {
    /*private static final ResourceLocation SPHERE_OBJ = new ResourceLocation(RefStrings.MODID, "models/sphere_new.obj");
    *///?} else {
        private static final ResourceLocation SPHERE_OBJ = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/sphere_new.obj");
    //?}

    private record Vec3(float x, float y, float z) {}
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
        consumer.vertex(matrix, x, y, z).color(r, g, b, a).uv(0, 0).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
    }

    private static List<Tri> loadTriangles(ResourceManager rm) {
        ResourceLocation file = new ResourceLocation(SPHERE_OBJ.getNamespace(), SPHERE_OBJ.getPath());
        Resource resource = rm.getResource(file).orElse(null);
        if (resource == null) {
            MainRegistry.LOGGER.warn("Fleija sphere OBJ missing: {}", file);
            return List.of();
        }

        List<Vec3> positions = new ArrayList<>();
        List<Tri> tris = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("v ")) {
                    String[] p = line.split("\\s+");
                    positions.add(new Vec3(Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])));
                    continue;
                }
                if (line.startsWith("f ")) {
                    String[] parts = line.substring(2).trim().split("\\s+");
                    if (parts.length < 3) {
                        continue;
                    }
                    int[] indices = new int[parts.length];
                    for (int i = 0; i < parts.length; i++) {
                        indices[i] = Integer.parseInt(parts[i].split("/")[0]);
                    }
                    for (int i = 1; i < indices.length - 1; i++) {
                        Vec3 a = positions.get(indices[0] - 1);
                        Vec3 b = positions.get(indices[i] - 1);
                        Vec3 c = positions.get(indices[i + 1] - 1);
                        tris.add(new Tri(a.x(), a.y(), a.z(), b.x(), b.y(), b.z(), c.x(), c.y(), c.z()));
                    }
                }
            }
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to load Fleija sphere OBJ {}", file, e);
            return List.of();
        }

        return List.copyOf(tris);
    }
}
