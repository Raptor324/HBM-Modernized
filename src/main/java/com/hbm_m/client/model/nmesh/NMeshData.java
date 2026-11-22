package com.hbm_m.client.model.nmesh;

import net.minecraft.resources.ResourceLocation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable in-memory representation of a binary {@code .nmesh} file (Version 3, Little-Endian).
 * Contains pre-triangulated, pre-welded sub-meshes with pre-calculated AABBs, tangents, MTL
 * diffuse/ambient colors, and material bindings.
 */
public final class NMeshData {

    public static final int MAGIC = 0x4E4D5348; // "NMSH"
    public static final int VERSION = 3;

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
        /** Contiguous 8 floats per vertex: x, y, z, nx, ny, nz, u, v (raw unflipped V). */
        public final float[] vertices;
        /** Precomputed packed tangents (1 int per vertex: 4 signed bytes tx, ty, tz, tw). */
        public final int[] tangents;
        /** 0-based triangle indices (length is a multiple of 3). */
        public final int[] indices;

        public SubMesh(String objectName, String groupName, String materialName, String mapKdTexture,
                       float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                       int vertexCount, float[] vertices, int[] tangents, int[] indices) {
            this(objectName, groupName, materialName, mapKdTexture,
                    1f, 1f, 1f, 0f, 0f, 0f,
                    minX, minY, minZ, maxX, maxY, maxZ,
                    vertexCount, vertices, tangents, indices);
        }

        public SubMesh(String objectName, String groupName, String materialName, String mapKdTexture,
                       float kdR, float kdG, float kdB, float kaR, float kaG, float kaB,
                       float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                       int vertexCount, float[] vertices, int[] tangents, int[] indices) {
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
            this.vertices = vertices;
            this.tangents = tangents;
            this.indices = indices;
        }

        public SubMesh(String objectName, String groupName, String materialName,
                       float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                       int vertexCount, float[] vertices, int[] tangents, int[] indices) {
            this(objectName, groupName, materialName, "", minX, minY, minZ, maxX, maxY, maxZ,
                    vertexCount, vertices, tangents, indices);
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
            if (version != VERSION) {
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

                float[] verts = new float[vCount * 8];
                buf.asFloatBuffer().get(verts);
                buf.position(buf.position() + vCount * 8 * 4);

                int[] tangents = new int[vCount];
                buf.asIntBuffer().get(tangents);
                buf.position(buf.position() + vCount * 4);

                int[] indices = new int[iCount];
                if (vCount <= 65535) {
                    short[] sIndices = new short[iCount];
                    buf.asShortBuffer().get(sIndices);
                    buf.position(buf.position() + iCount * 2);
                    for (int i = 0; i < iCount; i++) {
                        indices[i] = sIndices[i] & 0xFFFF;
                    }
                } else {
                    buf.asIntBuffer().get(indices);
                    buf.position(buf.position() + iCount * 4);
                }
                for (int i = 0; i < iCount; i++) {
                    if (indices[i] < 0 || indices[i] >= vCount) {
                        throw new IOException("Corrupted .nmesh submesh in " + location
                                + ": triangle index " + indices[i] + " out of range for " + vCount + " vertices");
                    }
                }
                list.add(new SubMesh(objName, grpName, matName, mapKd,
                        kdR, kdG, kdB, kaR, kaG, kaB,
                        minX, minY, minZ, maxX, maxY, maxZ,
                        vCount, verts, tangents, indices));
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
            totalBytes += sm.vertexCount * 8 * 4; // verts
            totalBytes += sm.vertexCount * 4;     // tangents
            totalBytes += sm.indices.length * (sm.vertexCount <= 65535 ? 2 : 4);
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
            buf.putInt(sm.indices.length);

            buf.asFloatBuffer().put(sm.vertices);
            buf.position(buf.position() + sm.vertexCount * 8 * 4);

            buf.asIntBuffer().put(sm.tangents);
            buf.position(buf.position() + sm.vertexCount * 4);

            if (sm.vertexCount <= 65535) {
                ShortBuffer sb = buf.asShortBuffer();
                for (int idx : sm.indices) {
                    sb.put((short) idx);
                }
                buf.position(buf.position() + sm.indices.length * 2);
            } else {
                buf.asIntBuffer().put(sm.indices);
                buf.position(buf.position() + sm.indices.length * 4);
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

