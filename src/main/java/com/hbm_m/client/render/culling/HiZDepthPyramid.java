package com.hbm_m.client.render.culling;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.slf4j.Logger;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.logging.LogUtils;

/**
 * Forward-Z Hierarchical-Z (Hi-Z) Depth Pyramid generator.
 * <p>
 * Consumes raw Minecraft scene depth from {@code RenderTarget.getDepthTextureId()}
 * and generates an immutable {@code GL_R32F} mipmapped depth pyramid using
 * a Single Pass Downsampler (SPD) compute pipeline adapted to Forward-Z
 * ($0.0 = \text{near}, 1.0 = \text{far}$, with {@code max()} conservative occluder reduction).
 *
 * @credit CrankShaft / fewizz / Jdb100
 * Based on FidelityFX SPD v2.1 adapted for OpenGL 4.3 depth pyramids in CrankShaft/Flywheel.
 */
@OnlyIn(Dist.CLIENT)
public final class HiZDepthPyramid {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static HiZDepthPyramid INSTANCE;

    private int pyramidTextureId = 0;
    private int placeholderTextureId = 0;
    private int lastWidth = -1;
    private int lastHeight = -1;
    private int mipLevels = 0;

    private int downsampleFirstProgram = 0;
    private int downsampleSecondProgram = 0;
    private int uDepthTexLoc = -1;
    private int uMipLevelsLoc = -1;
    private int uBaseMipLevelLoc = -1;
    private boolean initialized = false;

    private HiZDepthPyramid() {}

    public static synchronized HiZDepthPyramid get() {
        if (INSTANCE == null) {
            INSTANCE = new HiZDepthPyramid();
        }
        return INSTANCE;
    }

    /**
     * Computes the Mip 0 dimension snapped to the lower power of two of the screen dimension.
     */
    public static int mip0Size(int screenSize) {
        int nextPot = screenSize <= 1 ? 1 : Integer.highestOneBit(screenSize - 1) << 1;
        return Math.max(nextPot >> 1, 1);
    }

    /**
     * Computes total mipmap levels down to 1x1.
     */
    public static int getImageMipLevels(int width, int height) {
        int result = 1;
        while (width > 1 && height > 1) {
            result++;
            width >>= 1;
            height >>= 1;
        }
        return result;
    }

    private void ensureShaders() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            downsampleFirstProgram = compileComputeShader("/assets/hbm_m/shaders/cull/downsample_first.comp");
            uDepthTexLoc = GL20.glGetUniformLocation(downsampleFirstProgram, "depth_tex");

            downsampleSecondProgram = compileComputeShader("/assets/hbm_m/shaders/cull/downsample_second.comp");
            uMipLevelsLoc = GL20.glGetUniformLocation(downsampleSecondProgram, "mip_levels");
            uBaseMipLevelLoc = GL20.glGetUniformLocation(downsampleSecondProgram, "base_mip_level");

            LOGGER.info("[HBM-M] HiZDepthPyramid shaders compiled successfully (first={}, second={})",
                    downsampleFirstProgram, downsampleSecondProgram);
        } catch (Throwable t) {
            LOGGER.error("[HBM-M] Failed to compile HiZDepthPyramid compute shaders", t);
            destroy();
        }
    }

    private static String loadShaderSource(String resourcePath) {
        try (InputStream in = HiZDepthPyramid.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Shader resource not found: " + resourcePath);
            }
            String source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (source.contains("#include \"downsample.glsl\"")) {
                String include = loadShaderSource("/assets/hbm_m/shaders/cull/downsample.glsl");
                source = source.replace("#include \"downsample.glsl\"", include);
            }
            return source;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read shader " + resourcePath, e);
        }
    }

    private static int compileComputeShader(String resourcePath) {
        String source = loadShaderSource(resourcePath);
        int shader = GL20.glCreateShader(GL43.GL_COMPUTE_SHADER);
        com.hbm_m.platform.RenderHooks.safeShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader, 4096);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("Failed to compile compute shader " + resourcePath + ": " + log);
        }
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, shader);
        GL20.glLinkProgram(program);
        GL20.glDeleteShader(shader);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program, 4096);
            GL20.glDeleteProgram(program);
            throw new IllegalStateException("Failed to link compute program for " + resourcePath + ": " + log);
        }
        return program;
    }

    private void ensurePyramidTexture(int targetMipLevels, int width, int height) {
        if (pyramidTextureId > 0 && lastWidth == width && lastHeight == height && mipLevels == targetMipLevels) {
            return;
        }

        if (pyramidTextureId > 0) {
            deletePyramidTexture(pyramidTextureId);
            pyramidTextureId = 0;
        }

        lastWidth = width;
        lastHeight = height;
        mipLevels = targetMipLevels;

        pyramidTextureId = GL11.glGenTextures();
        // Texture setup binds go through GlStateManager (cache-coherent): a raw
        // glBindTexture on the current unit would desync the per-unit bind cache.
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(pyramidTextureId);
        GL42.glTexStorage2D(GL11.GL_TEXTURE_2D, mipLevels, GL30.GL_R32F, width, height);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL11.GL_NONE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GlStateManager._bindTexture(0);

        LOGGER.info("[HBM-M] HiZDepthPyramid reallocated (texId={}, size={}x{}, mips={})",
                pyramidTextureId, width, height, mipLevels);
    }

    private int ensurePlaceholder() {
        if (placeholderTextureId <= 0) {
            placeholderTextureId = GL11.glGenTextures();
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            GlStateManager._bindTexture(placeholderTextureId);
            GL42.glTexStorage2D(GL11.GL_TEXTURE_2D, 1, GL30.GL_R32F, 1, 1);
            ByteBuffer buf = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
            buf.putFloat(1.0f); // 1.0 = far plane/sky in Forward-Z
            buf.flip();
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 1, 1, GL11.GL_RED, GL11.GL_FLOAT, buf);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL11.GL_NONE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GlStateManager._bindTexture(0);
        }
        return placeholderTextureId;
    }

    /**
     * Regenerates the Hi-Z depth pyramid from the Minecraft main render target depth buffer.
     *
     * @param sourceDepthTextureId Native OpenGL depth texture ID from RenderTarget
     * @param framebufferWidth Full framebuffer pixel width
     * @param framebufferHeight Full framebuffer pixel height
     */
    public void regenerate(int sourceDepthTextureId, int framebufferWidth, int framebufferHeight) {
        if (sourceDepthTextureId <= 0 || framebufferWidth <= 0 || framebufferHeight <= 0) {
            return;
        }

        ensureShaders();
        if (downsampleFirstProgram <= 0 || downsampleSecondProgram <= 0) {
            return;
        }

        int width = mip0Size(framebufferWidth);
        int height = mip0Size(framebufferHeight);
        int targetMipLevels = getImageMipLevels(width, height);

        ensurePyramidTexture(targetMipLevels, width, height);

        // 1. Wait for preceding framebuffer rasterization writes
        GL42.glMemoryBarrier(GL42.GL_FRAMEBUFFER_BARRIER_BIT);

        // 2. Bind source depth texture to texture unit 10 through GlStateManager:
        // raw binds desync the per-unit cache (the exact recipe for the stale-binding
        // bug CrankShaft fixed in 1.5.2 — silently sampling a dead texture).
        GlStateManager._activeTexture(GL13.GL_TEXTURE10);
        GlStateManager._bindTexture(sourceDepthTextureId);

        // 3. Dispatch first downsample pass (full framebuffer -> Mip 0)
        GL20.glUseProgram(downsampleFirstProgram);
        if (uDepthTexLoc >= 0) {
            GL20.glUniform1i(uDepthTexLoc, 10);
        }

        GL42.glBindImageTexture(1, pyramidTextureId, 0, false, 0, GL15.GL_WRITE_ONLY, GL30.GL_R32F);
        GL43.glDispatchCompute((width * 2 + 63) / 64, (height * 2 + 63) / 64, 1);

        // 4. Dispatch second downsample passes (Mip 0 -> Mip 1..N)
        GL20.glUseProgram(downsampleSecondProgram);
        if (uMipLevelsLoc >= 0) {
            GL30.glUniform1ui(uMipLevelsLoc, targetMipLevels);
        }

        for (int baseMipLevel = 0; baseMipLevel + 1 < targetMipLevels; baseMipLevel += 6) {
            GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

            if (uBaseMipLevelLoc >= 0) {
                GL30.glUniform1ui(uBaseMipLevelLoc, baseMipLevel);
            }

            for (int i = 0; i < Math.min(7, targetMipLevels - baseMipLevel); i++) {
                int access = (i == 0) ? GL15.GL_READ_ONLY : GL15.GL_WRITE_ONLY;
                GL42.glBindImageTexture(i, pyramidTextureId, baseMipLevel + i, false, 0, access, GL30.GL_R32F);
            }

            int mipW = Math.max(1, width >> baseMipLevel);
            int mipH = Math.max(1, height >> baseMipLevel);
            GL43.glDispatchCompute((mipW + 63) / 64, (mipH + 63) / 64, 1);
        }

        // 5. Memory barrier for subsequent texture sampling in compute cull shader
        GL42.glMemoryBarrier(GL42.GL_TEXTURE_FETCH_BARRIER_BIT);

        // Restore OpenGL bindings
        GL20.glUseProgram(0);
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
    }

    /**
     * Deletes a texture with a GlStateManager cache sweep: the pyramid is
     * glGenTextures'd outside GlStateManager accounting, so a raw delete would leave
     * its id cached as "bound" on some unit and a later recreation could be skipped
     * by {@code _bindTexture} (CrankShaft 1.5.2 stale-pyramid bug). {@code _deleteTexture}
     * resets matching cache slots to -1 in both supported versions.
     */
    private static void deletePyramidTexture(int id) {
        if (id > 0) {
            GlStateManager._deleteTexture(id);
        }
    }

    public int getPyramidTextureId() {
        return pyramidTextureId > 0 ? pyramidTextureId : ensurePlaceholder();
    }

    public int getMipLevels() {
        return mipLevels > 0 ? mipLevels : 1;
    }

    public void destroy() {
        if (pyramidTextureId > 0) {
            deletePyramidTexture(pyramidTextureId);
            pyramidTextureId = 0;
        }
        if (placeholderTextureId > 0) {
            deletePyramidTexture(placeholderTextureId);
            placeholderTextureId = 0;
        }
        if (downsampleFirstProgram > 0) {
            GL20.glDeleteProgram(downsampleFirstProgram);
            downsampleFirstProgram = 0;
        }
        if (downsampleSecondProgram > 0) {
            GL20.glDeleteProgram(downsampleSecondProgram);
            downsampleSecondProgram = 0;
        }
        lastWidth = -1;
        lastHeight = -1;
        mipLevels = 0;
        initialized = false;
    }
}
