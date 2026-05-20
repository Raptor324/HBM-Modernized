package com.hbm_m.client.render.culling;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}

/**
 * GPU compaction: обнуляет {@code instanceCount} в indirect-командах по bitmask
 * из {@link GpuCullingPipeline} (без readback на CPU).
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public final class GpuMdiCompaction {

    private static final int PARAMS_BYTES = 16;
    private static final int LOCAL_SIZE_X = 64;
    /** Must match {@link com.hbm_m.client.render.MdiBatchCoordinator#INDIRECT_CMD_STRIDE_BYTES}. */
    private static final int INDIRECT_CMD_STRIDE_BYTES = 32;
    private static final int INDIRECT_CMD_STRIDE_WORDS = INDIRECT_CMD_STRIDE_BYTES / 4;

    private static volatile boolean initAttempted;
    private static volatile boolean supported;
    private static int program;
    private static int paramsUbo;
    private static int cullMapSsbo;

    private GpuMdiCompaction() {}

    public static boolean isSupported() {
        return GpuCullingPipeline.isSupported();
    }

    public static synchronized boolean initialize() {
        if (initAttempted) {
            return supported;
        }
        initAttempted = true;
        if (!GpuCullingPipeline.initialize()) {
            return false;
        }
        if (!RenderSystem.isOnRenderThread()) {
            return false;
        }
        try {
            String src = loadShaderSource();
            if (src == null) {
                return false;
            }
            int shader = GL43.glCreateShader(GL43.GL_COMPUTE_SHADER);
            GL20.glShaderSource(shader, src);
            GL20.glCompileShader(shader);
            if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                MainRegistry.LOGGER.warn("[HBM-GpuMdi] compact_mdi compile failed: {}",
                        GL20.glGetShaderInfoLog(shader));
                GL20.glDeleteShader(shader);
                return false;
            }
            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, shader);
            GL20.glLinkProgram(program);
            GL20.glDeleteShader(shader);
            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                MainRegistry.LOGGER.warn("[HBM-GpuMdi] compact_mdi link failed: {}",
                        GL20.glGetProgramInfoLog(program));
                GL20.glDeleteProgram(program);
                program = 0;
                return false;
            }
            paramsUbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, paramsUbo);
            GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, PARAMS_BYTES, GL15.GL_DYNAMIC_DRAW);
            cullMapSsbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
            supported = true;
            MainRegistry.LOGGER.info("[HBM-GpuMdi] GPU-driven MDI indirect compaction enabled");
            return true;
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("[HBM-GpuMdi] init failed", t);
            return false;
        }
    }

    /**
     * @param indirectBufferId {@link org.lwjgl.opengl.GL40#GL_DRAW_INDIRECT_BUFFER}
     * @param cullIndices      per-command cull staging index; {@code -1} = always draw
     */
    public static void compactIndirectCommands(int indirectBufferId, int[] cullIndices, int cmdCount,
                                               int visibilitySsbo, int cullEntryCount) {
        if (!supported || program == 0 || cmdCount <= 0 || indirectBufferId <= 0) {
            return;
        }
        if (visibilitySsbo <= 0 || cullEntryCount <= 0) {
            return;
        }
        try {
            int mapBytes = cmdCount * 4;
            ByteBuffer mapBuf = MemoryUtil.memAlloc(mapBytes).order(ByteOrder.nativeOrder());
            for (int i = 0; i < cmdCount; i++) {
                int idx = i < cullIndices.length ? cullIndices[i] : GpuCullMdiBridge.NO_CULL_INDEX;
                mapBuf.putInt(idx < 0 ? 0xFFFFFFFF : idx);
            }
            mapBuf.flip();
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, cullMapSsbo);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, mapBytes, GL15.GL_STREAM_DRAW);
            GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0L, mapBuf);
            MemoryUtil.memFree(mapBuf);

            ByteBuffer params = MemoryUtil.memAlloc(PARAMS_BYTES).order(ByteOrder.nativeOrder());
            params.putInt(cmdCount);
            params.putInt(cullEntryCount);
            params.putInt(INDIRECT_CMD_STRIDE_WORDS);
            params.putInt(0);
            params.flip();
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, paramsUbo);
            GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0L, params);
            MemoryUtil.memFree(params);

            GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, 0, paramsUbo);
            int prevIndirect = GL11.glGetInteger(org.lwjgl.opengl.GL40.GL_DRAW_INDIRECT_BUFFER_BINDING);
            GL15.glBindBuffer(org.lwjgl.opengl.GL40.GL_DRAW_INDIRECT_BUFFER, 0);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, indirectBufferId);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, visibilitySsbo);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, cullMapSsbo);

            GL20.glUseProgram(program);
            int groups = (cmdCount + LOCAL_SIZE_X - 1) / LOCAL_SIZE_X;
            GL43.glDispatchCompute(groups, 1, 1);
            GL43.glMemoryBarrier(GL43.GL_COMMAND_BARRIER_BIT | GL43.GL_SHADER_STORAGE_BARRIER_BIT);
            GL20.glUseProgram(0);

            GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, 0, 0);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, 0);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, 0);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, 0);
            GL15.glBindBuffer(org.lwjgl.opengl.GL40.GL_DRAW_INDIRECT_BUFFER, prevIndirect);
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("[HBM-GpuMdi] compactIndirectCommands failed", t);
        }
    }

    public static int indirectCommandStrideBytes() {
        return INDIRECT_CMD_STRIDE_BYTES;
    }

    private static String loadShaderSource() {
        try {
            ResourceLocation rl = new ResourceLocation(MainRegistry.MOD_ID, "shaders/compute/compact_mdi_indirect.compute");
            var resOpt = Minecraft.getInstance().getResourceManager().getResource(rl);
            if (resOpt.isPresent()) {
                try (InputStream is = resOpt.get().open()) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (IOException e) {
            MainRegistry.LOGGER.warn("[HBM-GpuMdi] shader load IOException", e);
        }
        return null;
    }
}
