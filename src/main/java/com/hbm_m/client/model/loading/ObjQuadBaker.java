//? if fabric {
package com.hbm_m.client.model.loading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.math.Transformation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

@Environment(EnvType.CLIENT)
final class ObjQuadBaker {

    private ObjQuadBaker() {}

    static List<BakedQuad> bakeFaceToQuads(ObjModelData.Face face,
                                           TextureAtlasSprite sprite,
                                           boolean flipV,
                                           Transformation transform,
                                           boolean shade) {
        List<ObjModelData.FaceVertex> vs = face.verts();
        if (vs.size() < 3) return List.of();

        if (vs.size() == 3) {
            return List.of(bakeQuad(vs.get(0), vs.get(1), vs.get(2), vs.get(2), sprite, flipV, transform, shade));
        }
        if (vs.size() == 4) {
            return List.of(bakeQuad(vs.get(0), vs.get(1), vs.get(2), vs.get(3), sprite, flipV, transform, shade));
        }

        List<BakedQuad> out = new ArrayList<>(vs.size() - 2);
        for (int i = 1; i + 1 < vs.size(); i++) {
            out.add(bakeQuad(vs.get(0), vs.get(i), vs.get(i + 1), vs.get(i + 1), sprite, flipV, transform, shade));
        }
        return out;
    }

    private static BakedQuad bakeQuad(ObjModelData.FaceVertex a,
                                      ObjModelData.FaceVertex b,
                                      ObjModelData.FaceVertex c,
                                      ObjModelData.FaceVertex d,
                                      TextureAtlasSprite sprite,
                                      boolean flipV,
                                      Transformation transform,
                                      boolean shade) {
        boolean hasTransform = transform != null && !transform.equals(Transformation.identity());

        Matrix4f m = hasTransform ? new Matrix4f(transform.getMatrix()) : new Matrix4f().identity();
        Matrix3f normalM = hasTransform ? new Matrix3f(m).invert().transpose() : null;

        Vector4f[] pos = new Vector4f[4];
        Vector3f[] norm = new Vector3f[4];

        Vector3f faceNormal = computeFaceNormalFallback(a, b, c);

        ObjModelData.FaceVertex[] vs = new ObjModelData.FaceVertex[]{a, b, c, d};
        for (int i = 0; i < 4; i++) {
            ObjModelData.FaceVertex v = vs[i];
            ObjModelData.Vec3 p0 = ObjQuadBakerState.MODEL.pos(v.v());
            Vector4f p = new Vector4f(p0.x(), p0.y(), p0.z(), 1.0f);

            Vector3f n0;
            if (v.vn() > 0) {
                ObjModelData.Vec3 vn = ObjQuadBakerState.MODEL.normal(v.vn());
                n0 = new Vector3f(vn.x(), vn.y(), vn.z());
            } else {
                n0 = new Vector3f(faceNormal);
            }

            if (hasTransform) {
                m.transform(p);
                if (p.w() != 0.0f && p.w() != 1.0f) {
                    p.div(p.w());
                }
                normalM.transform(n0);
                if (n0.lengthSquared() != 0) n0.normalize();
            }

            pos[i] = p;
            norm[i] = n0;
        }

        // Forge uses the first vertex normal for direction determination in makeQuad.
        // This matches: quadBaker.setDirection(Direction.getNearest(normal.x(), normal.y(), normal.z())) at i==0
        Direction dir = Direction.getNearest(norm[0].x(), norm[0].y(), norm[0].z());

        int packed = packNormal(norm[0].x(), norm[0].y(), norm[0].z());
        int[] data = new int[4 * 8];
        putVertex(data, 0, pos[0], uv(a, sprite, flipV), packed);
        putVertex(data, 1, pos[1], uv(b, sprite, flipV), packed);
        putVertex(data, 2, pos[2], uv(c, sprite, flipV), packed);
        putVertex(data, 3, pos[3], uv(d, sprite, flipV), packed);

        return new BakedQuad(data, -1, dir, sprite, shade);
    }

    /**
     * Forge's automatic culling logic from ObjModel.makeQuad():
     * Checks that ALL 4 vertices lie on the face of the unit cube corresponding to the direction,
     * AND that the normal points outward in that direction.
     *
     * Forge uses Mth.equal(a, b) which checks |a - b| < 1e-5f.
     */
    static void addQuadWithCulling(BakedQuad q, Map<Direction, List<BakedQuad>> out, boolean automaticCulling) {
        if (!automaticCulling) {
            out.get(null).add(q);
            return;
        }

        int[] v = q.getVertices();
        float[] px = new float[4], py = new float[4], pz = new float[4];
        for (int i = 0; i < 4; i++) {
            px[i] = Float.intBitsToFloat(v[i * 8]);
            py[i] = Float.intBitsToFloat(v[i * 8 + 1]);
            pz[i] = Float.intBitsToFloat(v[i * 8 + 2]);
        }

        // Extract first vertex normal for direction check (matches Forge using norm[0])
        int packedN = v[7]; // first vertex, offset 7 = normal
        float nx = ((byte)(packedN & 0xFF)) / 127.0f;
        float ny = ((byte)((packedN >> 8) & 0xFF)) / 127.0f;
        float nz = ((byte)((packedN >> 16) & 0xFF)) / 127.0f;

        Direction cull = null;

        // Forge checks: all 4 positions equal to border value AND normal points outward
        if (mthEqual(px[0], 0) && mthEqual(px[1], 0) && mthEqual(px[2], 0) && mthEqual(px[3], 0) && nx < 0) {
            cull = Direction.WEST;
        } else if (mthEqual(px[0], 1) && mthEqual(px[1], 1) && mthEqual(px[2], 1) && mthEqual(px[3], 1) && nx > 0) {
            cull = Direction.EAST;
        } else if (mthEqual(pz[0], 0) && mthEqual(pz[1], 0) && mthEqual(pz[2], 0) && mthEqual(pz[3], 0) && nz < 0) {
            cull = Direction.NORTH;
        } else if (mthEqual(pz[0], 1) && mthEqual(pz[1], 1) && mthEqual(pz[2], 1) && mthEqual(pz[3], 1) && nz > 0) {
            cull = Direction.SOUTH;
        } else if (mthEqual(py[0], 0) && mthEqual(py[1], 0) && mthEqual(py[2], 0) && mthEqual(py[3], 0) && ny < 0) {
            cull = Direction.DOWN;
        } else if (mthEqual(py[0], 1) && mthEqual(py[1], 1) && mthEqual(py[2], 1) && mthEqual(py[3], 1) && ny > 0) {
            cull = Direction.UP;
        }

        if (cull != null) {
            out.get(cull).add(q);
        } else {
            out.get(null).add(q);
        }
    }

    private static boolean mthEqual(float a, float b) {
        return Math.abs(a - b) < 1e-5f;
    }

    private static void putVertex(int[] data, int v, Vector4f pos, float[] uv, int packedNormal) {
        int base = v * 8;
        data[base] = Float.floatToRawIntBits(pos.x());
        data[base + 1] = Float.floatToRawIntBits(pos.y());
        data[base + 2] = Float.floatToRawIntBits(pos.z());
        data[base + 3] = -1; // color
        data[base + 4] = Float.floatToRawIntBits(uv[0]);
        data[base + 5] = Float.floatToRawIntBits(uv[1]);
        data[base + 6] = 0; // lightmap
        data[base + 7] = packedNormal;
    }

    private static float[] uv(ObjModelData.FaceVertex v, TextureAtlasSprite sprite, boolean flipV) {
        if (v.vt() <= 0) {
            return new float[]{sprite.getU0(), sprite.getV0()};
        }
        ObjModelData.Vec2 vt = ObjQuadBakerState.MODEL.uv(v.vt());
        float u = vt.u();
        float vv = vt.v();
        if (flipV) vv = 1.0f - vv;
        return new float[]{sprite.getU((double) (u * 16.0f)), sprite.getV((double) (vv * 16.0f))};
    }

    private static Vector3f computeFaceNormalFallback(ObjModelData.FaceVertex a,
                                                      ObjModelData.FaceVertex b,
                                                      ObjModelData.FaceVertex c) {
        ObjModelData.Vec3 pa0 = ObjQuadBakerState.MODEL.pos(a.v());
        ObjModelData.Vec3 pb0 = ObjQuadBakerState.MODEL.pos(b.v());
        ObjModelData.Vec3 pc0 = ObjQuadBakerState.MODEL.pos(c.v());
        Vector3f pa = new Vector3f(pa0.x(), pa0.y(), pa0.z());
        Vector3f pb = new Vector3f(pb0.x(), pb0.y(), pb0.z());
        Vector3f pc = new Vector3f(pc0.x(), pc0.y(), pc0.z());
        Vector3f abs = new Vector3f(pb).sub(pa);
        Vector3f acs = new Vector3f(pc).sub(pa);
        abs.cross(acs);
        if (abs.lengthSquared() == 0) return new Vector3f(0, 1, 0);
        abs.normalize();
        return abs;
    }

    private static int packNormal(float x, float y, float z) {
        int xb = (byte) Math.round(clamp(x) * 127.0f) & 0xFF;
        int yb = (byte) Math.round(clamp(y) * 127.0f) & 0xFF;
        int zb = (byte) Math.round(clamp(z) * 127.0f) & 0xFF;
        return xb | (yb << 8) | (zb << 16);
    }

    private static float clamp(float v) {
        return Math.max(-1.0f, Math.min(1.0f, v));
    }

    static final class ObjQuadBakerState {
        static ObjModelData MODEL;
    }
}
//?}