package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.main.MainRegistry;

/**
 * Global registry of parametric joint specifications (GPU animation,
 * attrib 15 AnimParams.w = joint index). Joint data are PART constants
 * (kind, axis, pivot), shared by all instances of a variant - therefore they
 * live not in the instance record (which only holds per-frame parameters)
 * but in a single RGBA32F texture that the vertex shader reads via texelFetch
 * ({@code #version 330} forbids dynamic UBO indexing; a texture does not).
 * <p>
 * Layout: 2 texels per joint, texel x = idx*2 + {0,1}:
 * <pre>
 *   A = (kind, axisX, axisY, axisZ)  - kind: 0 = rotation around axis (degrees),
 *                                      1 = translation along axis (blocks)
 *   B = (pivotX, pivotY, pivotZ, -)  - rotation pivot in part coordinates
 * </pre>
 * Registration is idempotent (dedup by value bits); upload happens only when a
 * new joint appears (once per part variant at build). The texture is
 * process-global - recreated only on render re-init.
 */
@OnlyIn(Dist.CLIENT)
public final class NucleusJointSpecs {

    public static final int KIND_ROTATE = 0;
    public static final int KIND_TRANSLATE = 1;
    /**
     * Sampler unit for uJointSpecs. Deliberately NOT the "4th declared sampler"
     * index: ShaderInstance.apply() maps samplers by declaration INDEX and drops
     * entries missing from the GLSL (Sampler1 is absent from block_lit.fsh -
     * see the registration warn), shifting every later index - the specs ended
     * up on the lightmap's unit. Binding is explicit (see {@link #applyBinding}),
     * so any unit outside the vanilla 0..2 works; 4 keeps clear of everything.
     */
    public static final int SAMPLER_UNIT = 4;

    private static final int FLOATS_PER_TEXEL = 4;
    private static final int TEXELS_PER_JOINT = 2;

    private static int texId = 0;
    private static int widthTexels = 0;
    private static int jointCount = 0;
    private static FloatBuffer cpu;
    private static boolean dirty = false;

    private NucleusJointSpecs() {}

    /**
     * Registers a joint (idempotently) and returns its index. Call on the render
     * thread (texture upload). The axis is normalized.
     */
    public static synchronized int register(int kind, float axisX, float axisY, float axisZ,
                                            float pivotX, float pivotY, float pivotZ) {
        float len = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
        if (len < 1.0e-6f) {
            len = 1f;
        }
        final float ax = axisX / len, ay = axisY / len, az = axisZ / len;

        // Dedup by raw bits - the index stays stable across frames.
        for (int i = 0; i < jointCount; i++) {
            int base = i * TEXELS_PER_JOINT * FLOATS_PER_TEXEL;
            if (cpu.get(base) == Float.intBitsToFloat(kind)
                    && Float.floatToRawIntBits(cpu.get(base + 1)) == Float.floatToRawIntBits(ax)
                    && Float.floatToRawIntBits(cpu.get(base + 2)) == Float.floatToRawIntBits(ay)
                    && Float.floatToRawIntBits(cpu.get(base + 3)) == Float.floatToRawIntBits(az)
                    && Float.floatToRawIntBits(cpu.get(base + 4)) == Float.floatToRawIntBits(pivotX)
                    && Float.floatToRawIntBits(cpu.get(base + 5)) == Float.floatToRawIntBits(pivotY)
                    && Float.floatToRawIntBits(cpu.get(base + 6)) == Float.floatToRawIntBits(pivotZ)) {
                return i;
            }
        }

        if (cpu == null) {
            cpu = MemoryUtil.memAllocFloat(64 * TEXELS_PER_JOINT * FLOATS_PER_TEXEL);
        }
        long need = (long) (jointCount + 1) * TEXELS_PER_JOINT * FLOATS_PER_TEXEL;
        if ((long) cpu.capacity() < need) {
            FloatBuffer bigger = MemoryUtil.memAllocFloat(cpu.capacity() * 2);
            MemoryUtil.memCopy(MemoryUtil.memAddress(cpu), MemoryUtil.memAddress(bigger),
                    (long) jointCount * TEXELS_PER_JOINT * FLOATS_PER_TEXEL * 4L);
            MemoryUtil.memFree(cpu);
            cpu = bigger;
        }
        int base = jointCount * TEXELS_PER_JOINT * FLOATS_PER_TEXEL;
        cpu.put(base, Float.intBitsToFloat(kind));
        cpu.put(base + 1, ax);
        cpu.put(base + 2, ay);
        cpu.put(base + 3, az);
        cpu.put(base + 4, pivotX);
        cpu.put(base + 5, pivotY);
        cpu.put(base + 6, pivotZ);
        cpu.put(base + 7, 0f);
        jointCount++;
        dirty = true;
        uploadIfDirty();
        return jointCount - 1;
    }

    /** GL texture of specs; 0 means no joints registered. */
    public static synchronized int getTextureId() {
        uploadIfDirty();
        return texId;
    }

    public static synchronized int getJointCount() {
        return jointCount;
    }

    /**
     * Binds the specs texture to unit {@link #SAMPLER_UNIT} (or a dummy when there
     * are no joints - the active sampler must be complete).
     * Call after {@code shader.apply()} of the instanced shader.
     */
    public static void bindSampler() {
        int tex = getTextureId();
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + SAMPLER_UNIT);
        if (tex != 0) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        } else {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    /**
     * Deterministic sampler binding, independent of ShaderInstance's
     * declaration-index mapping (which shifts when a declared sampler is absent
     * from the GLSL - Sampler1 case): sets the uJointSpecs uniform to
     * {@link #SAMPLER_UNIT} and binds the specs texture to that unit. Call while
     * the instanced program is active, after {@code shader.apply()}.
     */
    public static void applyBinding(int programId) {
        int loc = GL20.glGetUniformLocation(programId, "uJointSpecs");
        if (loc < 0) {
            return;
        }
        GL20.glUniform1i(loc, SAMPLER_UNIT);
        int tex = getTextureId();
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + SAMPLER_UNIT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    private static void uploadIfDirty() {
        if (!dirty) {
            return;
        }
        dirty = false;
        try {
            if (texId == 0) {
                texId = GL11.glGenTextures();
            }
            int needed = jointCount * TEXELS_PER_JOINT;
            if (widthTexels < needed) {
                widthTexels = Math.max(needed, 64);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL32.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL32.GL_CLAMP_TO_EDGE);
                GL30.glBindVertexArray(0); // reset for DSA-less drivers - not critical
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, widthTexels, 1, 0,
                        GL11.GL_RGBA, GL11.GL_FLOAT,
                        (FloatBuffer) cpu.limit(widthTexels * FLOATS_PER_TEXEL).position(0));
                ((FloatBuffer) cpu).clear();
            } else {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, needed, 1,
                        GL11.GL_RGBA, GL11.GL_FLOAT, (FloatBuffer) cpu.limit(needed * FLOATS_PER_TEXEL).position(0));
                ((FloatBuffer) cpu).clear();
            }
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        } catch (Throwable t) {
            // The texture is an optimization: on failure the parametric joints simply
            // will not work (w >= 0 but the fetch returns garbage) - disable joints until fixed.
            MainRegistry.LOGGER.warn("[HBM-M] NucleusJointSpecs upload failed: {}", t.toString());
            texId = 0;
            dirty = false;
        }
    }
}
