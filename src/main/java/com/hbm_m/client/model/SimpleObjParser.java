package com.hbm_m.client.model; // Поменяй на свой пакет

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import org.joml.Vector2f;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

public class SimpleObjParser {
    public static Map<String, Mesh> load(ResourceLocation location) {
        Map<String, Mesh> meshes = new HashMap<>();
        List<Vector3f> vertices = new ArrayList<>();
        List<Vector2f> uvs = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();

        String currentGroup = "default";
        List<Face> currentFaces = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Minecraft.getInstance().getResourceManager().getResource(location).get().open()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+");

                switch (parts[0]) {
                    case "o":
                    case "g": // Новая группа (часть тела)
                        if (!currentFaces.isEmpty()) {
                            meshes.put(currentGroup, new Mesh(currentFaces));
                            currentFaces = new ArrayList<>();
                        }
                        currentGroup = parts.length > 1 ? parts[1] : "default";
                        break;
                    case "v": // Вершина
                        vertices.add(new Vector3f(
                                Float.parseFloat(parts[1]),
                                Float.parseFloat(parts[2]),
                                Float.parseFloat(parts[3])
                        ));
                        break;
                    case "vt": // Текстурная координата
                        uvs.add(new Vector2f(
                                Float.parseFloat(parts[1]),
                                1.0f - Float.parseFloat(parts[2]) // Minecraft переворачивает V
                        ));
                        break;
                    case "vn": // Нормаль
                        normals.add(new Vector3f(
                                Float.parseFloat(parts[1]),
                                Float.parseFloat(parts[2]),
                                Float.parseFloat(parts[3])
                        ));
                        break;
                    case "f": // Грань
                        // Триангуляция (если квадрат)
                        for (int i = 0; i < parts.length - 3; i++) {
                            currentFaces.add(new Face(
                                    parseVertex(parts[1], vertices, uvs, normals),
                                    parseVertex(parts[2 + i], vertices, uvs, normals),
                                    parseVertex(parts[3 + i], vertices, uvs, normals)
                            ));
                        }
                        break;
                }
            }
            // Добавляем последнюю группу
            if (!currentFaces.isEmpty()) {
                meshes.put(currentGroup, new Mesh(currentFaces));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return meshes;
    }

    private static Vertex parseVertex(String part, List<Vector3f> vs, List<Vector2f> uvs, List<Vector3f> ns) {
        String[] idx = part.split("/");
        int vI = Integer.parseInt(idx[0]) - 1;
        int vtI = idx.length > 1 && !idx[1].isEmpty() ? Integer.parseInt(idx[1]) - 1 : 0;
        int vnI = idx.length > 2 ? Integer.parseInt(idx[2]) - 1 : 0;

        // Защита от отсутствия UV или нормалей
        Vector2f uv = (vtI >= 0 && vtI < uvs.size()) ? uvs.get(vtI) : new Vector2f(0, 0);
        Vector3f norm = (vnI >= 0 && vnI < ns.size()) ? ns.get(vnI) : new Vector3f(0, 1, 0);

        return new Vertex(vs.get(vI), uv, norm);
    }

    public static class Mesh {
        public final List<Face> faces;
        public Mesh(List<Face> faces) { this.faces = faces; }
    }
    public static class Face {
        public final Vertex v1, v2, v3;
        public Face(Vertex v1, Vertex v2, Vertex v3) { this.v1 = v1; this.v2 = v2; this.v3 = v3; }
    }
    public static class Vertex {
        public final Vector3f pos;
        public final Vector2f uv;
        public final Vector3f normal;
        public Vertex(Vector3f p, Vector2f u, Vector3f n) { pos = p; uv = u; normal = n; }
    }
}