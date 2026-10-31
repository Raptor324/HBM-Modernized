package com.hbm_m.client.model.nmesh;

import com.hbm_m.client.render.cache.RenderCacheManager;
import com.hbm_m.main.MainRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Loads and caches binary {@link NMeshData} from {@code .nmesh} resources.
 * Automatically remaps {@code .obj} resource locations to {@code .nmesh}.
 */
public final class NMeshLoader {

    private static final ConcurrentHashMap<ResourceLocation, NMeshData> CACHE = new ConcurrentHashMap<>();

    static {
        RenderCacheManager.register(reason -> CACHE.clear());
    }

    private NMeshLoader() {}

    public static void clearCache() {
        CACHE.clear();
    }

    public static ResourceLocation toNMeshLocation(ResourceLocation loc) {
        String path = loc.getPath();
        if (path.endsWith(".obj")) {
            path = path.substring(0, path.length() - 4) + ".nmesh";
        } else if (!path.endsWith(".nmesh")) {
            path = path + ".nmesh";
        }
        return ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), path);
    }

    public static @Nullable NMeshData load(ResourceLocation modelLocation) {
        if (modelLocation == null) return null;
        ResourceLocation nmeshLoc = toNMeshLocation(modelLocation);
        NMeshData cached = CACHE.get(nmeshLoc);
        if (cached != null) {
            return cached;
        }
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        // 1. Primary path: load pre-compiled binary .nmesh
        Resource nmeshRes = rm.getResource(nmeshLoc).orElse(null);
        if (nmeshRes == null && !nmeshLoc.getPath().startsWith("models/")) {
            ResourceLocation prefixed = ResourceLocation.fromNamespaceAndPath(
                    nmeshLoc.getNamespace(), "models/" + nmeshLoc.getPath());
            nmeshRes = rm.getResource(prefixed).orElse(null);
        }
        if (nmeshRes != null) {
            try (InputStream in = nmeshRes.open()) {
                NMeshData data = NMeshData.read(nmeshLoc, in);
                CACHE.put(nmeshLoc, data);
                return data;
            } catch (Exception e) {
                MainRegistry.LOGGER.error("Failed to read binary .nmesh {}", nmeshLoc, e);
            }
        }
        // 2. Fallback: if a resourcepack overrides with a raw .obj at runtime
        ResourceLocation objLoc = modelLocation.getPath().endsWith(".nmesh")
                ? ResourceLocation.fromNamespaceAndPath(modelLocation.getNamespace(),
                        modelLocation.getPath().substring(0, modelLocation.getPath().length() - 6) + ".obj")
                : modelLocation;
        Resource objRes = rm.getResource(objLoc).orElse(null);
        if (objRes == null && !objLoc.getPath().startsWith("models/")) {
            objRes = rm.getResource(ResourceLocation.fromNamespaceAndPath(
                    objLoc.getNamespace(), "models/" + objLoc.getPath())).orElse(null);
        }
        if (objRes != null) {
            try (InputStream in = objRes.open()) {
                NMeshData data = compileObjStream(nmeshLoc, in);
                CACHE.put(nmeshLoc, data);
                return data;
            } catch (Exception e) {
                MainRegistry.LOGGER.error("Failed to parse fallback .obj {}", objLoc, e);
            }
        }
        return null;
    }

    /** Runtime fallback parser for resourcepack-supplied .obj files. */
    public static NMeshData compileObjStream(ResourceLocation loc, InputStream in) throws Exception {
        return compileObjStream(loc, in, mtlRef -> resolveMtlStream(loc, mtlRef));
    }

    /** Runtime fallback parser for resourcepack-supplied .obj files with mockable/custom MTL resolver. */
    public static NMeshData compileObjStream(ResourceLocation loc, InputStream in,
                                             @Nullable Function<String, InputStream> mtlResolver) throws Exception {
        List<float[]> pos = new ArrayList<>();
        List<float[]> uv = new ArrayList<>();
        List<float[]> nrm = new ArrayList<>();
        String curObj = "";
        String curGrp = "";
        String curMat = "default";
        String curMapKd = "";
        float[] curKd = { 1f, 1f, 1f };
        float[] curKa = { 0f, 0f, 0f };
        boolean facesSinceLastO = false;
        Map<String, MtlData> mtlMap = new LinkedHashMap<>();

        record BucketKey(String obj, String grp, String mat, String mapKd,
                         float kdR, float kdG, float kdB, float kaR, float kaG, float kaB) {}
        Map<BucketKey, List<float[]>> buckets = new LinkedHashMap<>();

        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                if (line.startsWith("v ")) {
                    String[] p = line.split("\\s+");
                    pos.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
                } else if (line.startsWith("vt ")) {
                    String[] p = line.split("\\s+");
                    uv.add(new float[]{Float.parseFloat(p[1]), p.length > 2 ? Float.parseFloat(p[2]) : 0f});
                } else if (line.startsWith("vn ")) {
                    String[] p = line.split("\\s+");
                    nrm.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
                } else if (line.startsWith("mtllib ")) {
                    if (mtlResolver != null) {
                        String mtlRef = line.substring(7).trim();
                        loadMtl(mtlRef, mtlResolver, mtlMap);
                    }
                } else if (line.startsWith("o ")) {
                    curObj = line.substring(2).trim();
                    curGrp = "";
                    facesSinceLastO = false;
                } else if (line.startsWith("g ")) {
                    if (facesSinceLastO) {
                        curObj = "";
                        facesSinceLastO = false;
                    }
                    curGrp = line.substring(2).trim();
                } else if (line.startsWith("usemtl ")) {
                    curMat = line.substring(7).trim();
                    if (mtlMap.isEmpty() && mtlResolver != null) {
                        loadMtl("", mtlResolver, mtlMap);
                    }
                    MtlData mtl = mtlMap.get(curMat);
                    if (mtl != null) {
                        curMapKd = mtl.mapKd();
                        curKd = mtl.kd();
                        curKa = mtl.ka();
                    } else {
                        curMapKd = "";
                        curKd = new float[]{ 1f, 1f, 1f };
                        curKa = new float[]{ 0f, 0f, 0f };
                    }
                } else if (line.startsWith("f ")) {
                    facesSinceLastO = true;
                    String[] tokens = line.substring(2).trim().split("\\s+");
                    int n = tokens.length;
                    if (n < 3) continue;
                    float[][] faceVerts = new float[n][8];
                    boolean hasNormals = true;
                    for (int i = 0; i < n; i++) {
                        String[] parts = tokens[i].split("/");
                        int vi = parseObjIndex(parts[0], pos.size());
                        int ti = (parts.length > 1 && !parts[1].isEmpty()) ? parseObjIndex(parts[1], uv.size()) : -1;
                        int ni = (parts.length > 2 && !parts[2].isEmpty()) ? parseObjIndex(parts[2], nrm.size()) : -1;
                        float[] vp = pos.get(vi);
                        float[] vt = (ti >= 0 && ti < uv.size()) ? uv.get(ti) : new float[]{0f, 0f};
                        float[] vn;
                        if (ni >= 0 && ni < nrm.size()) {
                            vn = nrm.get(ni);
                        } else {
                            vn = new float[]{0f, 1f, 0f};
                            hasNormals = false;
                        }
                        faceVerts[i][0] = vp[0];
                        faceVerts[i][1] = vp[1];
                        faceVerts[i][2] = vp[2];
                        faceVerts[i][3] = vn[0];
                        faceVerts[i][4] = vn[1];
                        faceVerts[i][5] = vn[2];
                        faceVerts[i][6] = vt[0];
                        faceVerts[i][7] = vt[1];
                    }
                    if (!hasNormals) {
                        float ax = faceVerts[1][0] - faceVerts[0][0];
                        float ay = faceVerts[1][1] - faceVerts[0][1];
                        float az = faceVerts[1][2] - faceVerts[0][2];
                        float bx = faceVerts[2][0] - faceVerts[0][0];
                        float by = faceVerts[2][1] - faceVerts[0][1];
                        float bz = faceVerts[2][2] - faceVerts[0][2];
                        float nx = ay * bz - az * by;
                        float ny = az * bx - ax * bz;
                        float nz = ax * by - ay * bx;
                        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                        if (len > 1e-6f) { nx /= len; ny /= len; nz /= len; } else { nx = 0f; ny = 1f; nz = 0f; }
                        for (int i = 0; i < n; i++) {
                            faceVerts[i][3] = nx;
                            faceVerts[i][4] = ny;
                            faceVerts[i][5] = nz;
                        }
                    }
                    int[] triIndices = NMeshData.triangulatePolygon(faceVerts);
                    List<float[]> tris = buckets.computeIfAbsent(
                            new BucketKey(curObj, curGrp, curMat, curMapKd,
                                    curKd[0], curKd[1], curKd[2], curKa[0], curKa[1], curKa[2]),
                            k -> new ArrayList<>());
                    for (int idx : triIndices) {
                        tris.add(faceVerts[idx]);
                    }
                }
            }
        }

        List<NMeshData.SubMesh> subMeshes = new ArrayList<>(buckets.size());
        for (var entry : buckets.entrySet()) {
            BucketKey key = entry.getKey();
            List<float[]> triVerts = entry.getValue();
            if (triVerts.isEmpty()) continue;
            subMeshes.add(weldSubMesh(key.obj(), key.grp(), key.mat(), key.mapKd(),
                    key.kdR(), key.kdG(), key.kdB(), key.kaR(), key.kaG(), key.kaB(), triVerts));
        }
        return new NMeshData(loc, subMeshes);
    }

    /** Parsed MTL entry: diffuse map path plus Kd/Ka colors (Forge ObjMaterialLibrary.Material semantics). */
    private record MtlData(String mapKd, float[] kd, float[] ka) {}

    private static void loadMtl(String mtlRef, Function<String, InputStream> mtlResolver, Map<String, MtlData> mtlMap) {
        try (InputStream in = mtlResolver.apply(mtlRef)) {
            if (in == null) return;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                String curMtl = "";
                String mapKd = "";
                float[] kd = { 1f, 1f, 1f };
                float[] ka = { 0f, 0f, 0f };
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    if (line.startsWith("newmtl ")) {
                        if (!curMtl.isEmpty()) {
                            mtlMap.put(curMtl, new MtlData(mapKd, kd, ka));
                        }
                        curMtl = line.substring(7).trim();
                        mapKd = "";
                        kd = new float[]{ 1f, 1f, 1f };
                        ka = new float[]{ 0f, 0f, 0f };
                    } else if (line.startsWith("map_Kd ") && !curMtl.isEmpty()) {
                        mapKd = line.substring(7).trim();
                    } else if (line.startsWith("Kd ") && !curMtl.isEmpty()) {
                        kd = parseMtlColor(line.substring(3).trim());
                    } else if (line.startsWith("Ka ") && !curMtl.isEmpty()) {
                        ka = parseMtlColor(line.substring(3).trim());
                    }
                }
                if (!curMtl.isEmpty()) {
                    mtlMap.put(curMtl, new MtlData(mapKd, kd, ka));
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * MTL color line parsing with Forge {@code ObjModel.parseVector4} semantics:
     * missing components are 0, alpha defaults to 1 (we only carry RGB).
     */
    private static float[] parseMtlColor(String args) {
        String[] p = args.split("\\s+");
        float x = p.length > 0 ? parseFloatOr(p[0], 0f) : 0f;
        float y = p.length > 1 ? parseFloatOr(p[1], 0f) : 0f;
        float z = p.length > 2 ? parseFloatOr(p[2], 0f) : 0f;
        return new float[]{ x, y, z };
    }

    private static float parseFloatOr(String s, float def) {
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static @Nullable InputStream resolveMtlStream(ResourceLocation modelLoc, String mtlRef) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return null;
            ResourceManager rm = mc.getResourceManager();
            if (rm == null) return null;

            if (mtlRef != null && !mtlRef.isEmpty()) {
                if (mtlRef.contains(":")) {
                    ResourceLocation mtlLoc = ResourceLocation.tryParse(mtlRef);
                    if (mtlLoc != null) {
                        var res = rm.getResource(mtlLoc);
                        if (res.isPresent()) return res.get().open();
                    }
                }
                String basePath = modelLoc.getPath();
                int lastSlash = basePath.lastIndexOf('/');
                String dir = lastSlash >= 0 ? basePath.substring(0, lastSlash + 1) : "";
                ResourceLocation relLoc = ResourceLocation.fromNamespaceAndPath(modelLoc.getNamespace(), dir + mtlRef);
                var res = rm.getResource(relLoc);
                if (res.isPresent()) return res.get().open();
            }

            // Fallback to same-basename .mtl
            String path = modelLoc.getPath();
            String baseName = path.endsWith(".obj") ? path.substring(0, path.length() - 4)
                    : path.endsWith(".nmesh") ? path.substring(0, path.length() - 6) : path;
            ResourceLocation fallbackLoc = ResourceLocation.fromNamespaceAndPath(modelLoc.getNamespace(), baseName + ".mtl");
            var fallbackRes = rm.getResource(fallbackLoc);
            if (fallbackRes.isPresent()) return fallbackRes.get().open();
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static int parseObjIndex(String token, int size) {
        int idx = Integer.parseInt(token);
        return idx < 0 ? size + idx : idx - 1;
    }

    private static NMeshData.SubMesh weldSubMesh(String obj, String grp, String mat, String mapKd,
                                                 float kdR, float kdG, float kdB,
                                                 float kaR, float kaG, float kaB,
                                                 List<float[]> triVerts) {
        int rawCount = triVerts.size();
        int tableSize = Integer.highestOneBit(Math.max(16, rawCount * 2) - 1) << 1;
        int mask = tableSize - 1;
        int[] hashTable = new int[tableSize];
        Arrays.fill(hashTable, -1);

        float[] uniqueVerts = new float[rawCount * 8];
        int[] indices = new int[rawCount];
        int uniqueCount = 0;

        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < rawCount; i++) {
            float[] v = triVerts.get(i);
            float x = v[0], y = v[1], z = v[2];
            float nx = v[3], ny = v[4], nz = v[5];
            float nLen = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (nLen > 1e-6f) { nx /= nLen; ny /= nLen; nz /= nLen; }
            float u = v[6], vt = v[7];

            if (x < minX) minX = x; if (x > maxX) maxX = x;
            if (y < minY) minY = y; if (y > maxY) maxY = y;
            if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;

            int w0 = Float.floatToRawIntBits(x);
            int w1 = Float.floatToRawIntBits(y);
            int w2 = Float.floatToRawIntBits(z);
            int w3 = Float.floatToRawIntBits(nx);
            int w4 = Float.floatToRawIntBits(ny);
            int w5 = Float.floatToRawIntBits(nz);
            int w6 = Float.floatToRawIntBits(u);
            int w7 = Float.floatToRawIntBits(vt);

            int h = w0 * 31 + w1;
            h = h * 31 + w2;
            h = h * 31 + w3;
            h = h * 31 + w4;
            h = h * 31 + w5;
            h = h * 31 + w6;
            h = h * 31 + w7;
            h ^= (h >>> 16);

            int slot = h & mask;
            int found = -1;
            while (true) {
                int cand = hashTable[slot];
                if (cand < 0) {
                    found = uniqueCount++;
                    int b = found * 8;
                    uniqueVerts[b]     = x;
                    uniqueVerts[b + 1] = y;
                    uniqueVerts[b + 2] = z;
                    uniqueVerts[b + 3] = nx;
                    uniqueVerts[b + 4] = ny;
                    uniqueVerts[b + 5] = nz;
                    uniqueVerts[b + 6] = u;
                    uniqueVerts[b + 7] = vt;
                    hashTable[slot] = found;
                    break;
                }
                int b = cand * 8;
                if (Float.floatToRawIntBits(uniqueVerts[b]) == w0
                        && Float.floatToRawIntBits(uniqueVerts[b + 1]) == w1
                        && Float.floatToRawIntBits(uniqueVerts[b + 2]) == w2
                        && Float.floatToRawIntBits(uniqueVerts[b + 3]) == w3
                        && Float.floatToRawIntBits(uniqueVerts[b + 4]) == w4
                        && Float.floatToRawIntBits(uniqueVerts[b + 5]) == w5
                        && Float.floatToRawIntBits(uniqueVerts[b + 6]) == w6
                        && Float.floatToRawIntBits(uniqueVerts[b + 7]) == w7) {
                    found = cand;
                    break;
                }
                slot = (slot + 1) & mask;
            }
            indices[i] = found;
        }

        float[] finalVerts = Arrays.copyOf(uniqueVerts, uniqueCount * 8);
        int[] tangents = new int[uniqueCount];
        Arrays.fill(tangents, 0x7F00007F);
        if (!Float.isFinite(minX)) {
            minX = minY = minZ = 0f;
            maxX = maxY = maxZ = 1f;
        }
        return new NMeshData.SubMesh(obj, grp, mat, mapKd,
                kdR, kdG, kdB, kaR, kaG, kaB,
                minX, minY, minZ, maxX, maxY, maxZ,
                uniqueCount, finalVerts, tangents, indices);
    }
}
