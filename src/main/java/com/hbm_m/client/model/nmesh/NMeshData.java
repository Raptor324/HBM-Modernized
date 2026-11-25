package com.hbm_m.client.model.nmesh;

import com.hbm_m.client.model.nmesh.NMeshLoader;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable in-memory representation of a binary {@code .nmesh} file (Version 4, Little-Endian;
 * backward-compatible with Version 3 on read).
 * Contains pre-triangulated, pre-welded sub-meshes with pre-calculated AABBs, MTL
 * diffuse/ambient colors, compact vertex attributes (float32 pos, binary16 UV, snorm8x3 normal),
 * and material bindings.
 */
public final class NMeshData {

    public static final int MAGIC = 0x4E4D5348; // "NMSH"
    public static final int VERSION = 4;

    public static final class SubMesh {
        public final String objectName;
        public final String groupName;
        public final String componentName;
        public final String materialName;
        public final String mapKdTexture;
        /** MTL diffuse color (Kd) of the sub-mesh material; white (1,1,1) when absent. */
        public final float kdR, kdG, kdB;
        /** MTL ambient color (Ka) of the sub-mesh material; black (0,0,0) when absent. */
        public final float kaR, kaG, kaB;
        public final float minX, minY, minZ, maxX, maxY, maxZ;
        public final int vertexCount;
        /** Contiguous 3 floats per vertex: x, y, z. */
        public final float[] positions;
        /** Packed snorm8x3 normals (1 int per vertex: bx | (by << 8) | (bz << 16)). */
        public final int[] packedNormals;
        /** Contiguous 2 floats per vertex: u, v (raw unflipped V). */
        public final float[] uvs;
        /** 0-based triangle indices when vertexCount <= 65535 (unsigned 16-bit). */
        @Nullable
        public final short[] shortIndices;
        /** 0-based triangle indices fallback when vertexCount > 65535. */
        @Nullable
        public final int[] intIndices;

        public SubMesh(String objectName, String groupName, String materialName, String mapKdTexture,
                       float kdR, float kdG, float kdB, float kaR, float kaG, float kaB,
                       float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                       int vertexCount, float[] positions, int[] packedNormals, float[] uvs,
                       @Nullable short[] shortIndices, @Nullable int[] intIndices) {
            this.objectName = objectName != null ? objectName : "";
            this.groupName = groupName != null ? groupName : "";
            if (!this.objectName.isEmpty() && !this.groupName.isEmpty() && !this.objectName.equals(this.groupName)) {
                this.componentName = this.objectName + "/" + this.groupName;
            } else if (!this.objectName.isEmpty()) {
                this.componentName = this.objectName;
            } else if (!this.groupName.isEmpty()) {
                this.componentName = this.groupName;
            } else {
                this.componentName = "default";
            }
            this.materialName = (materialName == null || materialName.isEmpty()) ? "default" : materialName;
            this.mapKdTexture = mapKdTexture != null ? mapKdTexture : "";
            this.kdR = kdR;
            this.kdG = kdG;
            this.kdB = kdB;
            this.kaR = kaR;
            this.kaG = kaG;
            this.kaB = kaB;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.vertexCount = vertexCount;
            this.positions = positions;
            this.packedNormals = packedNormals;
            this.uvs = uvs;
            this.shortIndices = shortIndices;
            this.intIndices = intIndices;
        }

        public int indexCount() {
            return shortIndices != null ? shortIndices.length : (intIndices != null ? intIndices.length : 0);
        }

        public int index(int i) {
            return shortIndices != null ? (shortIndices[i] & 0xFFFF) : intIndices[i];
        }

        public float posX(int v) { return positions[v * 3]; }
        public float posY(int v) { return positions[v * 3 + 1]; }
        public float posZ(int v) { return positions[v * 3 + 2]; }

        public float normX(int v) { return ((byte) (packedNormals[v] & 0xFF)) / 127.0f; }
        public float normY(int v) { return ((byte) ((packedNormals[v] >> 8) & 0xFF)) / 127.0f; }
        public float normZ(int v) { return ((byte) ((packedNormals[v] >> 16) & 0xFF)) / 127.0f; }

        public float u(int v) { return uvs[v * 2]; }
        public float v(int v) { return uvs[v * 2 + 1]; }

        public int packedNormal(int v) { return packedNormals[v]; }

        public static int packNormal(float nx, float ny, float nz) {
            int bx = ((int) Math.round(Math.max(-1f, Math.min(1f, nx)) * 127f)) & 0xFF;
            int by = ((int) Math.round(Math.max(-1f, Math.min(1f, ny)) * 127f)) & 0xFF;
            int bz = ((int) Math.round(Math.max(-1f, Math.min(1f, nz)) * 127f)) & 0xFF;
            return bx | (by << 8) | (bz << 16);
        }

        public boolean matchesPart(String targetPart) {
            if (targetPart == null || targetPart.isEmpty()) return true;
            return targetPart.equals(objectName)
                    || targetPart.equals(groupName)
                    || targetPart.equals(componentName)
                    || componentName.startsWith(targetPart + "/");
        }
    }

    private final ResourceLocation location;
    private final List<SubMesh> subMeshes;
    private final Set<String> rootComponentNames;

    public NMeshData(ResourceLocation location, List<SubMesh> subMeshes) {
        this.location = location;
        this.subMeshes = List.copyOf(subMeshes);
        Set<String> roots = new LinkedHashSet<>();
        boolean anyObject = false;
        for (SubMesh sm : subMeshes) {
            if (!sm.objectName.isEmpty()) {
                anyObject = true;
                break;
            }
        }
        for (SubMesh sm : subMeshes) {
            if (anyObject && !sm.objectName.isEmpty()) {
                roots.add(sm.objectName);
                if (!sm.groupName.isEmpty()) {
                    roots.add(sm.groupName);
                }
            } else if (!sm.groupName.isEmpty()) {
                roots.add(sm.groupName);
            } else {
                roots.add("default");
            }
        }
        this.rootComponentNames = Collections.unmodifiableSet(roots);
    }

    public ResourceLocation location() {
        return location;
    }

    public List<SubMesh> subMeshes() {
        return subMeshes;
    }

    public Set<String> getRootComponentNames() {
        return rootComponentNames;
    }

    public List<SubMesh> getSubMeshesForPart(String partName) {
        List<SubMesh> out = new ArrayList<>();
        for (SubMesh sm : subMeshes) {
            if (sm.matchesPart(partName)) {
                out.add(sm);
            }
        }
        return out;
    }

    public static NMeshData read(ResourceLocation location, InputStream in) throws IOException {
        byte[] bytes = in.readAllBytes();
        try {
            ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            int magic = buf.getInt();
            if (magic != MAGIC) {
                throw new IOException("Invalid .nmesh magic in " + location + ": 0x" + Integer.toHexString(magic));
            }
            int version = buf.getInt();
            if (version != 3 && version != VERSION) {
                throw new IOException("Unsupported .nmesh version " + version + " in " + location);
            }
            int subCount = buf.getInt();
            if (subCount < 0) {
                throw new IOException("Corrupted .nmesh header in " + location + ": negative subCount " + subCount);
            }
            List<SubMesh> list = new ArrayList<>(subCount);
            for (int s = 0; s < subCount; s++) {
                String objName = readString(buf);
                String grpName = readString(buf);
                String matName = readString(buf);
                String mapKd = readString(buf);
                float kdR = buf.getFloat();
                float kdG = buf.getFloat();
                float kdB = buf.getFloat();
                float kaR = buf.getFloat();
                float kaG = buf.getFloat();
                float kaB = buf.getFloat();
                float minX = buf.getFloat();
                float minY = buf.getFloat();
                float minZ = buf.getFloat();
                float maxX = buf.getFloat();
                float maxY = buf.getFloat();
                float maxZ = buf.getFloat();
                int vCount = buf.getInt();
                int iCount = buf.getInt();
                if (vCount < 0 || iCount < 0) {
                    throw new IOException("Corrupted .nmesh submesh in " + location + ": negative count v=" + vCount + ", i=" + iCount);
                }

                float[] positions = new float[vCount * 3];
                int[] packedNormals = new int[vCount];
                float[] uvs = new float[vCount * 2];
                short[] shortIndices = vCount <= 65535 ? new short[iCount] : null;
                int[] intIndices = vCount > 65535 ? new int[iCount] : null;

                if (version == 4) {
                    buf.asFloatBuffer().get(positions);
                    buf.position(buf.position() + vCount * 12);

                    buf.asFloatBuffer().get(uvs);
                    buf.position(buf.position() + vCount * 8);

                    for (int v = 0; v < vCount; v++) {
                        int bx = buf.get() & 0xFF;
                        int by = buf.get() & 0xFF;
                        int bz = buf.get() & 0xFF;
                        packedNormals[v] = bx | (by << 8) | (bz << 16);
                    }

                    int prev = 0;
                    if (vCount <= 256) {
                        for (int i = 0; i < iCount; i++) {
                            int delta = buf.get() & 0xFF;
                            int idx = (prev + delta) & 0xFF;
                            prev = idx;
                            if (idx >= vCount) {
                                throw new IOException("Corrupted .nmesh submesh in " + location
                                        + ": triangle index " + idx + " out of range for " + vCount + " vertices");
                            }
                            shortIndices[i] = (short) idx;
                        }
                    } else if (vCount <= 65535) {
                        ShortBuffer sb = buf.asShortBuffer();
                        for (int i = 0; i < iCount; i++) {
                            int delta = sb.get() & 0xFFFF;
                            int idx = (prev + delta) & 0xFFFF;
                            prev = idx;
                            if (idx >= vCount) {
                                throw new IOException("Corrupted .nmesh submesh in " + location
                                        + ": triangle index " + idx + " out of range for " + vCount + " vertices");
                            }
                            shortIndices[i] = (short) idx;
                        }
                        buf.position(buf.position() + iCount * 2);
                    } else {
                        IntBuffer ib = buf.asIntBuffer();
                        for (int i = 0; i < iCount; i++) {
                            int idx = prev + ib.get();
                            prev = idx;
                            if (idx < 0 || idx >= vCount) {
                                throw new IOException("Corrupted .nmesh submesh in " + location
                                        + ": triangle index " + idx + " out of range for " + vCount + " vertices");
                            }
                            intIndices[i] = idx;
                        }
                        buf.position(buf.position() + iCount * 4);
                    }
                } else {
                    // Legacy Version 3 read support
                    float[] verts = new float[vCount * 8];
                    buf.asFloatBuffer().get(verts);
                    buf.position(buf.position() + vCount * 8 * 4);
                    // Skip tangents (vCount * 4 bytes)
                    buf.position(buf.position() + vCount * 4);

                    for (int v = 0; v < vCount; v++) {
                        int b = v * 8;
                        int p = v * 3;
                        positions[p] = verts[b];
                        positions[p + 1] = verts[b + 1];
                        positions[p + 2] = verts[b + 2];
                        packedNormals[v] = SubMesh.packNormal(verts[b + 3], verts[b + 4], verts[b + 5]);
                        uvs[v * 2] = verts[b + 6];
                        uvs[v * 2 + 1] = verts[b + 7];
                    }

                    if (vCount <= 65535) {
                        buf.asShortBuffer().get(shortIndices);
                        buf.position(buf.position() + iCount * 2);
                        for (int i = 0; i < iCount; i++) {
                            int idx = shortIndices[i] & 0xFFFF;
                            if (idx >= vCount) {
                                throw new IOException("Corrupted .nmesh submesh in " + location
                                        + ": triangle index " + idx + " out of range for " + vCount + " vertices");
                            }
                        }
                    } else {
                        buf.asIntBuffer().get(intIndices);
                        buf.position(buf.position() + iCount * 4);
                        for (int i = 0; i < iCount; i++) {
                            if (intIndices[i] < 0 || intIndices[i] >= vCount) {
                                throw new IOException("Corrupted .nmesh submesh in " + location
                                        + ": triangle index " + intIndices[i] + " out of range for " + vCount + " vertices");
                            }
                        }
                    }
                }
                list.add(new SubMesh(objName, grpName, matName, mapKd,
                        kdR, kdG, kdB, kaR, kaG, kaB,
                        minX, minY, minZ, maxX, maxY, maxZ,
                        vCount, positions, packedNormals, uvs, shortIndices, intIndices));
            }
            return new NMeshData(location, list);
        } catch (BufferUnderflowException | IllegalArgumentException | IndexOutOfBoundsException | NegativeArraySizeException e) {
            throw new IOException("Corrupted or truncated .nmesh binary in " + location + ": " + e.getMessage(), e);
        }
    }

    public void write(OutputStream out) throws IOException {
        out.write(toByteArray());
    }

    public byte[] toByteArray() {
        int totalBytes = 12; // magic (4) + version (4) + subCount (4)
        for (SubMesh sm : subMeshes) {
            byte[] bObj = sm.objectName.getBytes(StandardCharsets.UTF_8);
            byte[] bGrp = sm.groupName.getBytes(StandardCharsets.UTF_8);
            byte[] bMat = sm.materialName.getBytes(StandardCharsets.UTF_8);
            byte[] bTex = sm.mapKdTexture.getBytes(StandardCharsets.UTF_8);
            totalBytes += 2 + bObj.length + 2 + bGrp.length + 2 + bMat.length + 2 + bTex.length;
            totalBytes += 48; // 6 floats (Kd, Ka) + 6 floats (AABB)

            totalBytes += 8;  // vCount, iCount
            totalBytes += sm.vertexCount * 12; // positions
            totalBytes += sm.vertexCount * 8;  // uvs
            totalBytes += sm.vertexCount * 3;  // snorm8x3 normals
            int iCount = sm.indexCount();
            int idxStride = sm.vertexCount <= 256 ? 1 : (sm.vertexCount <= 65535 ? 2 : 4);
            totalBytes += iCount * idxStride;
        }

        ByteBuffer buf = ByteBuffer.allocate(totalBytes).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(MAGIC);
        buf.putInt(VERSION);
        buf.putInt(subMeshes.size());
        for (SubMesh sm : subMeshes) {
            writeString(buf, sm.objectName);
            writeString(buf, sm.groupName);
            writeString(buf, sm.materialName);
            writeString(buf, sm.mapKdTexture);
            buf.putFloat(sm.kdR);
            buf.putFloat(sm.kdG);
            buf.putFloat(sm.kdB);
            buf.putFloat(sm.kaR);
            buf.putFloat(sm.kaG);
            buf.putFloat(sm.kaB);
            buf.putFloat(sm.minX);
            buf.putFloat(sm.minY);
            buf.putFloat(sm.minZ);
            buf.putFloat(sm.maxX);
            buf.putFloat(sm.maxY);
            buf.putFloat(sm.maxZ);
            buf.putInt(sm.vertexCount);
            int iCount = sm.indexCount();
            buf.putInt(iCount);

            FloatBuffer fb = buf.asFloatBuffer();
            fb.put(sm.positions);
            buf.position(buf.position() + sm.vertexCount * 12);

            FloatBuffer uvb = buf.asFloatBuffer();
            uvb.put(sm.uvs);
            buf.position(buf.position() + sm.vertexCount * 8);

            for (int v = 0; v < sm.vertexCount; v++) {
                int n = sm.packedNormals[v];
                buf.put((byte) (n & 0xFF));
                buf.put((byte) ((n >> 8) & 0xFF));
                buf.put((byte) ((n >> 16) & 0xFF));
            }

            int prev = 0;
            if (sm.vertexCount <= 256) {
                for (int i = 0; i < iCount; i++) {
                    int idx = sm.index(i);
                    buf.put((byte) ((idx - prev) & 0xFF));
                    prev = idx;
                }
            } else if (sm.vertexCount <= 65535) {
                ShortBuffer sb = buf.asShortBuffer();
                for (int i = 0; i < iCount; i++) {
                    int idx = sm.index(i);
                    sb.put((short) (idx - prev));
                    prev = idx;
                }
                buf.position(buf.position() + iCount * 2);
            } else {
                IntBuffer ib = buf.asIntBuffer();
                for (int i = 0; i < iCount; i++) {
                    int idx = sm.index(i);
                    ib.put(idx - prev);
                    prev = idx;
                }
                buf.position(buf.position() + iCount * 4);
            }
        }
        return buf.array();
    }

    private static String readString(ByteBuffer buf) {
        int len = buf.getShort() & 0xFFFF;
        byte[] bytes = new byte[len];
        buf.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeString(ByteBuffer buf, String str) {
        if (str == null) str = "";
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        buf.putShort((short) bytes.length);
        buf.put(bytes);
    }

    /**
     * 2D-Projected Ear-Clipping Triangulation.
     * Uses Newell's area-weighted 3D normal, dominant axis projection, signed area winding,
     * and robust fallback for degenerate/collinear geometry.
     * Always emits exactly (n - 2) * 3 triangle indices for any polygon with n >= 3 vertices.
     */
    public static int[] triangulatePolygon(float[][] verts) {
        if (verts == null || verts.length < 3) return new int[0];
        int n = verts.length;
        if (n == 3) return new int[]{0, 1, 2};

        // 1. Newell's Area-Weighted Normal
        double nx = 0, ny = 0, nz = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            float[] vi = verts[i], vj = verts[j];
            nx += (double) (vi[1] - vj[1]) * (vi[2] + vj[2]);
            ny += (double) (vi[2] - vj[2]) * (vi[0] + vj[0]);
            nz += (double) (vi[0] - vj[0]) * (vi[1] + vj[1]);
        }
        double lenSq = nx * nx + ny * ny + nz * nz;
        if (lenSq < 1e-12) {
            // Degenerate 3D normal fallback: naive fan
            int[] fan = new int[(n - 2) * 3];
            int idx = 0;
            for (int i = 1; i + 1 < n; i++) {
                fan[idx++] = 0; fan[idx++] = i; fan[idx++] = i + 1;
            }
            return fan;
        }

        // 2. Dominant Axis Projection
        double absX = Math.abs(nx), absY = Math.abs(ny), absZ = Math.abs(nz);
        double[] u = new double[n], v = new double[n];
        if (absZ >= absX && absZ >= absY) {
            for (int i = 0; i < n; i++) { u[i] = verts[i][0]; v[i] = verts[i][1]; }
        } else if (absY >= absX && absY >= absZ) {
            for (int i = 0; i < n; i++) { u[i] = verts[i][0]; v[i] = verts[i][2]; }
        } else {
            for (int i = 0; i < n; i++) { u[i] = verts[i][1]; v[i] = verts[i][2]; }
        }

        // 3. 2D Signed Area (Shoelace)
        double area2D = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            area2D += (u[i] * v[j] - u[j] * v[i]);
        }
        if (Math.abs(area2D) < 1e-12) {
            int[] fan = new int[(n - 2) * 3];
            int idx = 0;
            for (int i = 1; i + 1 < n; i++) {
                fan[idx++] = 0; fan[idx++] = i; fan[idx++] = i + 1;
            }
            return fan;
        }
        double areaSign = area2D > 0 ? 1.0 : -1.0;

        // 4. Ear-Clipping Execution
        int[] active = new int[n];
        for (int i = 0; i < n; i++) active[i] = i;
        int m = n;
        int[] out = new int[(n - 2) * 3];
        int triCursor = 0;

        while (m > 3) {
            int earIdx = -1;
            for (int k = 0; k < m; k++) {
                int prev = active[(k - 1 + m) % m];
                int curr = active[k];
                int next = active[(k + 1) % m];

                double c = cross2D(u[prev], v[prev], u[curr], v[curr], u[next], v[next]);
                if (c * areaSign <= 1e-7) continue; // Reflex or collinear

                boolean ear = true;
                for (int j = 0; j < m; j++) {
                    int p = active[j];
                    if (p == prev || p == curr || p == next) continue;
                    if (pointInTriangle(u[p], v[p], u[prev], v[prev], u[curr], v[curr], u[next], v[next], areaSign)) {
                        ear = false;
                        break;
                    }
                }
                if (ear) {
                    earIdx = k;
                    break;
                }
            }

            if (earIdx < 0) {
                // Robust fallback: clip the most convex candidate
                double maxCross = -Double.MAX_VALUE;
                int bestK = 0;
                for (int k = 0; k < m; k++) {
                    int prev = active[(k - 1 + m) % m];
                    int curr = active[k];
                    int next = active[(k + 1) % m];
                    double sc = cross2D(u[prev], v[prev], u[curr], v[curr], u[next], v[next]) * areaSign;
                    if (sc > maxCross) {
                        maxCross = sc;
                        bestK = k;
                    }
                }
                earIdx = bestK;
            }

            int prev = active[(earIdx - 1 + m) % m];
            int curr = active[earIdx];
            int next = active[(earIdx + 1) % m];
            out[triCursor++] = prev;
            out[triCursor++] = curr;
            out[triCursor++] = next;

            for (int i = earIdx; i < m - 1; i++) {
                active[i] = active[i + 1];
            }
            m--;
        }

        out[triCursor++] = active[0];
        out[triCursor++] = active[1];
        out[triCursor++] = active[2];
        return out;
    }

    private static double cross2D(double ax, double ay, double bx, double by, double cx, double cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }

    private static boolean pointInTriangle(double px, double py,
                                           double ax, double ay,
                                           double bx, double by,
                                           double cx, double cy,
                                           double areaSign) {
        double s1 = cross2D(ax, ay, bx, by, px, py) * areaSign;
        double s2 = cross2D(bx, by, cx, cy, px, py) * areaSign;
        double s3 = cross2D(cx, cy, ax, ay, px, py) * areaSign;
        return s1 >= -1e-7 && s2 >= -1e-7 && s3 >= -1e-7;
    }
}

