//? if fabric {
package com.hbm_m.client.model.loading;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.main.MainRegistry;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

@Environment(EnvType.CLIENT)
public final class ObjModelData {

    public record Vec3(float x, float y, float z) {}
    public record Vec2(float u, float v) {}

    public record FaceVertex(int v, int vt, int vn) {}

    public record Face(List<FaceVertex> verts, String objectName, String materialName) {}

    private final List<Vec3> positions;
    private final List<Vec2> uvs;
    private final List<Vec3> normals;
    private final List<Face> faces;
    private final Map<String, String> materialToTexture; // map_Kd raw string (can be "#key" or "ns:path")
    private final float minX, minY, minZ, maxX, maxY, maxZ;

    private ObjModelData(List<Vec3> positions,
                         List<Vec2> uvs,
                         List<Vec3> normals,
                         List<Face> faces,
                         Map<String, String> materialToTexture,
                         float minX, float minY, float minZ,
                         float maxX, float maxY, float maxZ) {
        this.positions = positions;
        this.uvs = uvs;
        this.normals = normals;
        this.faces = faces;
        this.materialToTexture = materialToTexture;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public List<Face> faces() {
        return faces;
    }

    public Vec3 pos(int idx1) {
        return positions.get(idx1 - 1);
    }

    public Vec2 uv(int idx1) {
        return uvs.get(idx1 - 1);
    }

    public Vec3 normal(int idx1) {
        return normals.get(idx1 - 1);
    }

    public String materialTexture(String materialName) {
        return materialToTexture.get(materialName);
    }

    /**
     * Heuristic: if model vertices include negative coordinates, it's usually authored around origin (e.g. [-0.5..0.5]).
     * Such models should rotate around origin, and then be translated into block space by JSON "transform".
     */
    public boolean prefersOriginPivot() {
        return minX < 0.0f || minY < 0.0f || minZ < 0.0f;
    }

    public static ObjModelData load(ResourceManager rm, ResourceLocation objLocation) {
        // Forge expects location like "modid:models/block/foo.obj"
        ResourceLocation file = new ResourceLocation(objLocation.getNamespace(), objLocation.getPath());
        Resource objRes = rm.getResource(file).orElseThrow(() -> new IllegalArgumentException("OBJ not found: " + file));

        List<Vec3> positions = new ArrayList<>();
        List<Vec2> uvs = new ArrayList<>();
        List<Vec3> normals = new ArrayList<>();
        List<Face> faces = new ArrayList<>();

        String currentObject = null;
        String currentMtl = null;
        String mtllib = null;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(objRes.open(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                if (line.startsWith("o ")) {
                    currentObject = line.substring(2).trim();
                    continue;
                }
                if (line.startsWith("g ")) {
                    if (currentObject == null || currentObject.isBlank()) {
                        currentObject = line.substring(2).trim();
                    }
                    continue;
                }
                if (line.startsWith("usemtl ")) {
                    currentMtl = line.substring("usemtl ".length()).trim();
                    continue;
                }
                if (line.startsWith("mtllib ")) {
                    mtllib = line.substring("mtllib ".length()).trim();
                    continue;
                }
                if (line.startsWith("v ")) {
                    String[] p = split(line, 4);
                    positions.add(new Vec3(parseF(p[1]), parseF(p[2]), parseF(p[3])));
                    continue;
                }
                if (line.startsWith("vt ")) {
                    String[] p = split(line, 3);
                    uvs.add(new Vec2(parseF(p[1]), parseF(p[2])));
                    continue;
                }
                if (line.startsWith("vn ")) {
                    String[] p = split(line, 4);
                    normals.add(new Vec3(parseF(p[1]), parseF(p[2]), parseF(p[3])));
                    continue;
                }
                if (line.startsWith("f ")) {
                    String[] parts = line.substring(2).trim().split("\\s+");
                    if (parts.length < 3) continue;
                    List<FaceVertex> verts = new ArrayList<>(parts.length);
                    for (String part : parts) {
                        verts.add(parseFaceVertex(part, positions.size(), uvs.size(), normals.size()));
                    }
                    faces.add(new Face(verts, currentObject, currentMtl));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read OBJ: " + file, e);
        }

        Map<String, String> materialToTex = mtllib != null ? MtlData.load(rm, objLocation, mtllib) : Map.of();
        MainRegistry.LOGGER.debug("Loaded OBJ {} (v={}, vt={}, vn={}, faces={}, mtllib={})",
                objLocation, positions.size(), uvs.size(), normals.size(), faces.size(), mtllib);

        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        for (Vec3 v : positions) {
            minX = Math.min(minX, v.x());
            minY = Math.min(minY, v.y());
            minZ = Math.min(minZ, v.z());
            maxX = Math.max(maxX, v.x());
            maxY = Math.max(maxY, v.y());
            maxZ = Math.max(maxZ, v.z());
        }

        return new ObjModelData(positions, uvs, normals, faces, materialToTex, minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static FaceVertex parseFaceVertex(String token, int vCount, int vtCount, int vnCount) {
        String[] p = token.split("/");
        int v = parseIndex(p[0], vCount);
        int vt = (p.length > 1 && !p[1].isEmpty()) ? parseIndex(p[1], vtCount) : 0;
        int vn = (p.length > 2 && !p[2].isEmpty()) ? parseIndex(p[2], vnCount) : 0;
        return new FaceVertex(v, vt, vn);
    }

    private static int parseIndex(String s, int size) {
        int i = Integer.parseInt(s);
        if (i < 0) {
            return size + i + 1; // -1 = last
        }
        return i;
    }

    private static float parseF(String s) {
        return Float.parseFloat(s);
    }

    private static String[] split(String line, int expected) {
        String[] out = line.split("\\s+");
        if (out.length < expected) {
            throw new IllegalArgumentException("Malformed OBJ line: " + line);
        }
        return out;
    }
}
//?}

