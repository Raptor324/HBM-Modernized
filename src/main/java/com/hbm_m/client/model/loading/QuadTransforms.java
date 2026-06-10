//? if fabric {
/*package com.hbm_m.client.model.loading;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;

@Environment(EnvType.CLIENT)
final class QuadTransforms {

    private QuadTransforms() {}

    static BakedQuad transform(BakedQuad quad, Matrix4f m) {
        int[] verts = quad.getVertices();
        if (verts.length < 4 * 8) return quad;
        int stride = verts.length / 4;
        int normalOffset = stride - 1;
        int[] out = verts.clone();

        Matrix4f normalMatrix = new Matrix4f(m).invert().transpose();
        Vector4f pos = new Vector4f();
        Vector4f norm = new Vector4f();

        for (int v = 0; v < 4; v++) {
            int i = v * stride;
            float x = Float.intBitsToFloat(out[i]);
            float y = Float.intBitsToFloat(out[i + 1]);
            float z = Float.intBitsToFloat(out[i + 2]);

            pos.set(x, y, z, 1.0f);
            m.transform(pos); // Fix: matrix.transform(vector)

            if (pos.w() != 0.0f && pos.w() != 1.0f) {
                pos.div(pos.w());
            }

            out[i] = Float.floatToRawIntBits(pos.x());
            out[i + 1] = Float.floatToRawIntBits(pos.y());
            out[i + 2] = Float.floatToRawIntBits(pos.z());

            if (normalOffset < stride) {
                int packed = out[i + normalOffset];
                byte nxb = (byte) (packed & 0xFF);
                byte nyb = (byte) ((packed >> 8) & 0xFF);
                byte nzb = (byte) ((packed >> 16) & 0xFF);

                norm.set(nxb / 127f, nyb / 127f, nzb / 127f, 0.0f);
                normalMatrix.transform(norm); // Fix: normalMatrix.transform(vector)
                if (norm.lengthSquared() != 0) norm.normalize();

                int xb = (byte) Math.round(norm.x() * 127f) & 0xFF;
                int yb = (byte) Math.round(norm.y() * 127f) & 0xFF;
                int zb = (byte) Math.round(norm.z() * 127f) & 0xFF;
                out[i + normalOffset] = xb | (yb << 8) | (zb << 16);
            }
        }

        Direction dir = quad.getDirection();
        return new BakedQuad(out, quad.getTintIndex(), dir, quad.getSprite(), quad.isShade());
    }
}
*///?}