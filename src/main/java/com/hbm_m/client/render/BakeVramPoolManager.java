package com.hbm_m.client.render;

import com.hbm_m.client.render.debug.GlDebugMarkers;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Global GPU VRAM multi-draw pass pool manager for Tier-1 compute bake.
 * Manages output SSBOs, element buffer objects (EBOs), indirect command buffers,
 * dynamic pool relayout with prefix sums, idle-tail memory shrinking,
 * LRU cache eviction, and VRAM budget ledger accounting.
 */
public final class BakeVramPoolManager {

    private BakeVramPoolManager() {}

    public static final int INDIRECT_CMD_BYTES = 20; // DrawElementsIndirectCommand
    public static final int CULL_CMD_BINDING = 4;
    public static final long MIN_POOL_BYTES = 64L << 20; // 64 MB initial floor
    /** Hard ceiling for a single OpenGL buffer object (safely below signed 32-bit 2 GiB driver limit). */
    public static final long MAX_SINGLE_BUFFER_BYTES = 1900L << 20; // 1900 MiB
    public static final long SHRINK_IDLE_FRAMES = 300L;
    public static final long SHRINK_MIN_SAVINGS = 8L << 20;
    public static final long LRU_FRAME_LIMIT = 24;
    public static final long ADMISSION_BACKOFF_FRAMES = 32;
    public static final int MIN_EBO_INSTANCES = 1024;
    public static final int CTL_HEADER_WORDS = 16;
    public static final int LOCAL_SIZE = 64;

    public static final int OUT_STRIDE_BYTES = 56;
    public static final int OUT_OFF_POS = 0;
    public static final int OUT_OFF_COLOR = 12;
    public static final int OUT_OFF_UV0 = 16;
    public static final int OUT_OFF_UV1 = 24;
    public static final int OUT_OFF_UV2 = 28;
    public static final int OUT_OFF_NORMAL = 32;
    public static final int OUT_OFF_ENTITY = 36;
    public static final int OUT_OFF_MIDTEX = 40;
    public static final int OUT_OFF_TANGENT = 48;
    public static final int OUT_OFF_MIDBLOCK = 52;

    public static final int SHADOW_STRIDE_BYTES = 24;
    public static final int SH_OFF_POS = 0;
    public static final int SH_OFF_UV0 = 12;
    public static final int SH_OFF_NORMAL = 20;

    public static final PassPool POOL_MAIN = new PassPool(false);
    public static final PassPool POOL_SHADOW = new PassPool(true);

    public static PassPool pool(int mode) {
        return mode == NucleusGpuBaker.MODE_SHADOW ? POOL_SHADOW : POOL_MAIN;
    }

    public static String poolName(PassPool pool) {
        return pool == POOL_SHADOW ? "Shadow" : "Main";
    }

    public static int modeOfPool(PassPool pool) {
        return pool == POOL_SHADOW ? NucleusGpuBaker.MODE_SHADOW : NucleusGpuBaker.MODE_MAIN;
    }

    public static int outStrideBytes(int mode) {
        return mode == NucleusGpuBaker.MODE_SHADOW ? SHADOW_STRIDE_BYTES : OUT_STRIDE_BYTES;
    }

    /** Total VRAM bytes currently held by per-renderer caches and pools. */
    private static long cacheBytes = 0;
    private static boolean cacheOverflowLogged = false;

    public static final List<BakedMesh> BAKED = new ArrayList<>(64);

    public static synchronized long getCacheBytes() {
        return cacheBytes;
    }

    public static synchronized void adjustCacheBytes(long delta) {
        cacheBytes += delta;
    }

    public static synchronized void resetCacheBytes() {
        cacheBytes = 0;
    }

    public static long cacheBudgetBytes() {
        Integer flag = Integer.getInteger("hbm.bakeCacheMB");
        int mb = flag != null ? flag : ModClothConfig.get().nucleusBakeCacheMB;
        if (mb <= 0) {
            return 0L;
        }
        return Math.max(16L, (long) mb) * 1024L * 1024L;
    }

    public static int roundUpInstanceCapacity(int count) {
        int min = Math.max(count, 256);
        if (min <= 1024) {
            return Integer.highestOneBit(min - 1) << 1;
        }
        // Above 1024 instances, round up to multiples of 256 (with 12.5% growth step)
        // instead of doubling (which inflated 5000 instances to 8192 and blew past 2 GiB).
        return (min + (min >> 3) + 255) & ~255;
    }

    public static boolean cacheAllocated(ModeCache mc) {
        return mc.outInstances > 0;
    }

    public static boolean admissionBackoffActive(ModeCache mc) {
        long refused = mc.admissionRefusedFrame;
        return refused >= 0L && frameStamp() - refused < ADMISSION_BACKOFF_FRAMES;
    }

    public static void stampAdmissionRefusal(ModeCache mc) {
        if (!admissionBackoffActive(mc)) {
            mc.admissionRefusedFrame = frameStamp();
        }
    }

    public static boolean poolRelayoutDeferred(int mode) {
        return false;
    }

    public static long frameStamp() {
        return IrisShadowBatchCollector.renderFrame();
    }

    public static int ensurePoolCmdBuffer(PassPool pool, int minSlots) {
        try {
            if (pool.cmdSsbo == -1) {
                pool.cmdSsbo = GL15.glGenBuffers();
                GlDebugMarkers.labelBuffer(pool.cmdSsbo, "HBM_PoolCmd_" + poolName(pool) + "_SSBO");
            }
            if (minSlots > pool.cmdSlots) {
                long newSlots = Math.max(minSlots, Math.max(pool.cmdSlots * 2, 64));
                GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, pool.cmdSsbo);
                GL15.glBufferData(GL40.GL_DRAW_INDIRECT_BUFFER,
                        newSlots * INDIRECT_CMD_BYTES, GL15.GL_DYNAMIC_DRAW);
                GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, 0);
                pool.cmdSlots = newSlots;
                pool.cmdGen++;
                pool.cmdSlotOwners = new ModeCache[(int) newSlots];
            }
            return pool.cmdSsbo;
        } catch (Exception t) {
            MainRegistry.LOGGER.warn(
                    "[HBM-M] NucleusGpuBaker: pool command store allocation failed - "
                            + "flush falls back to the CPU count path this frame", t);
            return -1;
        }
    }

    public static int createOutputVao(int vbo, boolean slim, int[] createdEboOut) {
        int previousVao = GlVaoSafety.currentBinding();
        int vao = GL30.glGenVertexArrays();
        int ebo = -1;
        try {
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            if (slim) {
                GL20.glEnableVertexAttribArray(0);
                GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, SHADOW_STRIDE_BYTES, SH_OFF_POS);
                GL20.glEnableVertexAttribArray(2);
                GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, SHADOW_STRIDE_BYTES, SH_OFF_UV0);
                GL20.glEnableVertexAttribArray(5);
                GL20.glVertexAttribPointer(5, 3, GL11.GL_BYTE, true, SHADOW_STRIDE_BYTES, SH_OFF_NORMAL);
            } else {
                GL20.glEnableVertexAttribArray(0);
                GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, OUT_STRIDE_BYTES, OUT_OFF_POS);
                GL20.glEnableVertexAttribArray(1);
                GL20.glVertexAttribPointer(1, 4, GL11.GL_UNSIGNED_BYTE, true, OUT_STRIDE_BYTES, OUT_OFF_COLOR);
                GL20.glEnableVertexAttribArray(2);
                GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, OUT_STRIDE_BYTES, OUT_OFF_UV0);
                GL20.glEnableVertexAttribArray(3);
                GL30.glVertexAttribIPointer(3, 2, GL11.GL_SHORT, OUT_STRIDE_BYTES, OUT_OFF_UV1);
                GL20.glEnableVertexAttribArray(4);
                GL30.glVertexAttribIPointer(4, 2, GL11.GL_SHORT, OUT_STRIDE_BYTES, OUT_OFF_UV2);
                GL20.glEnableVertexAttribArray(5);
                GL20.glVertexAttribPointer(5, 3, GL11.GL_BYTE, true, OUT_STRIDE_BYTES, OUT_OFF_NORMAL);
            }
            ebo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
            GlVaoSafety.bindVertexArray(previousVao);
            createdEboOut[0] = ebo;
            return vao;
        } catch (Exception t) {
            GlVaoSafety.bindVertexArray(previousVao);
            GL30.glDeleteVertexArrays(vao);
            if (ebo != -1) GL15.glDeleteBuffers(ebo);
            throw t;
        }
    }

    public static int createOutputVaoWithEbo(int vbo, int ebo, boolean slim) {
        int previousVao = GlVaoSafety.currentBinding();
        int[] placeholderEbo = {-1};
        int vao = createOutputVao(vbo, slim, placeholderEbo);
        try {
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
        } catch (Exception t) {
            GlVaoSafety.bindVertexArray(previousVao);
            GL30.glDeleteVertexArrays(vao);
            if (placeholderEbo[0] != -1) {
                GL15.glDeleteBuffers(placeholderEbo[0]);
            }
            throw t;
        }
        GlVaoSafety.bindVertexArray(previousVao);
        int staleEbo = placeholderEbo[0];
        if (staleEbo != -1) GL15.glDeleteBuffers(staleEbo);
        return vao;
    }

    public static boolean eboExpansionFits(BakedMesh baked, int targetCapacity) {
        return (long) baked.idxCount * targetCapacity <= Integer.MAX_VALUE
                && (long) baked.vertCount * targetCapacity <= Integer.MAX_VALUE;
    }

    private static int maxComputeGroupsX = -1;
    private static boolean identityFillRefuseLogged = false;

    public static boolean identityFillGroupsFit(long groups) {
        if (maxComputeGroupsX < 0) {
            int limit = 65535;
            try {
                int queried = GL30.glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT, 0);
                if (queried > 0) {
                    limit = queried;
                }
            } catch (Throwable ignored) {
            }
            maxComputeGroupsX = limit;
        }
        if (groups > maxComputeGroupsX) {
            if (!identityFillRefuseLogged) {
                identityFillRefuseLogged = true;
                MainRegistry.LOGGER.warn(
                        "[HBM-M] NucleusGpuBaker: identity EBO fill needs {} X work-groups, "
                                + "driver limit {} - oversized fills stay on the CPU path",
                        groups, maxComputeGroupsX);
            }
            return false;
        }
        return true;
    }

    public static boolean dispatchIdentityEboFill(BakedMesh baked, int eboSsbo,
                                                   int slotCount, int vertBase, int eboWordBase) {
        if (NucleusGpuBaker.cullProgramId == -1) {
            return false;
        }
        long invocations = (long) baked.idxCount * slotCount;
        long groups = (invocations + LOCAL_SIZE - 1) / LOCAL_SIZE;
        if (!identityFillGroupsFit(groups)) {
            return false;
        }
        GlStateManager._glUseProgram(NucleusGpuBaker.cullProgramId);
        GL20.glUniform1i(NucleusGpuBaker.cuPass, 1);
        GL20.glUniform1i(NucleusGpuBaker.cuIdentity, 1);
        GL20.glUniform1i(NucleusGpuBaker.cuInstCount, slotCount);
        GL20.glUniform1i(NucleusGpuBaker.cuVertCount, baked.vertCount);
        GL20.glUniform1i(NucleusGpuBaker.cuIdxCount, baked.idxCount);
        GL20.glUniform1i(NucleusGpuBaker.cuVertBase, vertBase);
        GL20.glUniform1i(NucleusGpuBaker.cuEboBase, eboWordBase);
        GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT | GL43.GL_BUFFER_UPDATE_BARRIER_BIT);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, baked.baseIdxSsbo);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 3, eboSsbo);
        GL43.glDispatchCompute((int) groups, 1, 1);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, 0);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 3, 0);
        GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT | GL43.GL_ELEMENT_ARRAY_BARRIER_BIT);
        return true;
    }

    public static boolean preallocateCacheBuffers(BakedMesh baked, ModeCache mc, int count, int mode) {
        int cap = roundUpInstanceCapacity(count);
        long recBytes = (long) cap * InstancedStaticPartRenderer.INSTANCE_DATA_SIZE * 4L;
        long recStorage = Math.max(recBytes, 1L << 16);
        long ctlBytes = (long) (CTL_HEADER_WORDS + cap) * 4L;
        long dirtySsboBytes = (long) (1 + cap) * 4L;
        long localNeed = recStorage + ctlBytes + dirtySsboBytes;
        synchronized (BakeVramPoolManager.class) {
            cacheBytes += localNeed;
        }
        try {
            mc.lastUseFrame = frameStamp();
            mc.outInstances = cap;
            mc.eboInstances = cap;
            mc.recordSsbo = GL15.glGenBuffers();
            GlDebugMarkers.labelBuffer(mc.recordSsbo, "HBM_Record_" + baked.debugTag() + "_" + (mode == NucleusGpuBaker.MODE_SHADOW ? "Shadow" : "Main"));
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, mc.recordSsbo);
            mc.recordSize[0] = recStorage;
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, mc.recordSize[0], GL15.GL_DYNAMIC_COPY);
            mc.recordCopy = new float[(int) (recBytes / 4L)];
            mc.ctlSsbo = GL15.glGenBuffers();
            GlDebugMarkers.labelBuffer(mc.ctlSsbo, "HBM_Ctl_" + baked.debugTag() + "_" + (mode == NucleusGpuBaker.MODE_SHADOW ? "Shadow" : "Main"));
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, mc.ctlSsbo);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, ctlBytes, GL15.GL_DYNAMIC_COPY);
            mc.ctlBytes = ctlBytes;
            mc.dirtySsbo = GL15.glGenBuffers();
            GlDebugMarkers.labelBuffer(mc.dirtySsbo, "HBM_Dirty_" + baked.debugTag() + "_" + (mode == NucleusGpuBaker.MODE_SHADOW ? "Shadow" : "Main"));
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, mc.dirtySsbo);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, dirtySsboBytes, GL15.GL_DYNAMIC_COPY);
            mc.dirtyBytes = dirtySsboBytes;
            mc.dirtyIdx = new int[cap];
            mc.dirtyCount = 0;
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            return true;
        } catch (Exception t) {
            MainRegistry.LOGGER.warn(
                    "[HBM-M] NucleusGpuBaker: preallocate cache buffers failed", t);
            if (mc.recordSsbo != -1) GL15.glDeleteBuffers(mc.recordSsbo);
            if (mc.ctlSsbo != -1) GL15.glDeleteBuffers(mc.ctlSsbo);
            if (mc.dirtySsbo != -1) GL15.glDeleteBuffers(mc.dirtySsbo);
            mc.recordSsbo = -1;
            mc.ctlSsbo = -1;
            mc.dirtySsbo = -1;
            mc.outInstances = 0;
            mc.eboInstances = 0;
            synchronized (BakeVramPoolManager.class) {
                cacheBytes -= localNeed;
            }
            return false;
        }
    }

    public static boolean allocateCache(BakedMesh baked, ModeCache mc, int count, int mode) {
        int cap = roundUpInstanceCapacity(count);
        long outBytes = (long) baked.vertCount * cap * outStrideBytes(mode);
        long eboBytes = (long) baked.idxCount * cap * 4L;
        long recBytes = (long) cap * InstancedStaticPartRenderer.INSTANCE_DATA_SIZE * 4L;
        long recStorage = Math.max(recBytes, 1L << 16);
        long ctlBytes = (long) (CTL_HEADER_WORDS + cap) * 4L;
        long dirtySsboBytes = (long) (1 + cap) * 4L;
        long localNeed = recStorage + ctlBytes + dirtySsboBytes;
        long poolNeed = outBytes + eboBytes;
        long totalEstimate = localNeed + poolNeed;
        synchronized (BakeVramPoolManager.class) {
            if (cacheBytes + totalEstimate > cacheBudgetBytes()) {
                evictStaleCaches();
                if (cacheBytes + totalEstimate > cacheBudgetBytes()) {
                    if (!cacheOverflowLogged) {
                        cacheOverflowLogged = true;
                        MainRegistry.LOGGER.warn(
                                "[HBM-M] NucleusGpuBaker: cache budget exceeded (needs {} MB, {} MB in use, budget {} MB) - allowing active mesh allocation to avoid legacy fallback",
                                Math.max(1L, totalEstimate >> 20),
                                cacheBytes >> 20,
                                cacheBudgetBytes() >> 20);
                    }
                }
            }
        }
        if (!preallocateCacheBuffers(baked, mc, count, mode)) {
            return false;
        }
        if (!relayoutPool(mode)) {
            freeModeCache(mc);
            return false;
        }
        return true;
    }

    public static synchronized void evictStaleCaches() {
        long now = frameStamp();
        for (int i = BAKED.size() - 1; i >= 0; i--) {
            BakedMesh bm = BAKED.get(i);
            if (!bm.mesh.isBuilt()) {
                releaseBakedMesh(bm);
                BAKED.remove(i);
                continue;
            }
            releaseCacheIfStale(bm.shadow, now);
            releaseCacheIfStale(bm.main, now);
        }
    }

    public static void releaseCacheIfStale(ModeCache mc, long now) {
        if (cacheAllocated(mc) && now - mc.lastUseFrame > LRU_FRAME_LIMIT) {
            freeModeCache(mc);
        }
    }

    public static void releaseBakedMesh(BakedMesh bm) {
        freeModeCache(bm.shadow);
        freeModeCache(bm.main);
        if (bm.inputSsbo != -1) GL15.glDeleteBuffers(bm.inputSsbo);
        if (bm.baseIdxSsbo != -1) GL15.glDeleteBuffers(bm.baseIdxSsbo);
        bm.inputSsbo = -1;
        bm.baseIdxSsbo = -1;
    }

    public static void freeModeCache(ModeCache mc) {
        synchronized (BakeVramPoolManager.class) {
            cacheBytes -= Math.max(mc.ctlBytes, 0)
                    + Math.max(mc.recordSize[0], 0)
                    + Math.max(mc.dirtyBytes, 0);
        }
        mc.outInstances = 0;
        mc.eboInstances = 0;
        if (mc.recordSsbo != -1) GL15.glDeleteBuffers(mc.recordSsbo);
        if (mc.ctlSsbo != -1) GL15.glDeleteBuffers(mc.ctlSsbo);
        if (mc.dirtySsbo != -1) GL15.glDeleteBuffers(mc.dirtySsbo);
        mc.recordSsbo = -1;
        mc.recordSize[0] = -1;
        mc.ctlSsbo = -1;
        mc.ctlBytes = -1;
        mc.dirtySsbo = -1;
        mc.dirtyBytes = -1;
        mc.dirtyIdx = null;
        mc.dirtyCount = 0;
        mc.filled = -1;
        mc.recordCopy = null;
        mc.recordCount = -1;
        mc.boneKey = -1;
        mc.cullValid = false;
        mc.outputValid = false;
        mc.countsDirty = false;
        mc.stagingSlot = -1;
        mc.frustumStamp = Long.MIN_VALUE;
        mc.admissionRefusedFrame = -1L;
        mc.sourceMutationEpoch = -1L;
    }

    public static boolean ensureOutputCapacity(BakedMesh baked, ModeCache mc, int count, int mode,
                                                boolean relayoutAllowed) {
        boolean grew = false;
        int oldOut = mc.outInstances;
        int oldEbo = mc.eboInstances;
        if (count > mc.outInstances) {
            mc.outInstances = roundUpInstanceCapacity(count);
            grew = true;
        }
        if (count > mc.eboInstances) {
            mc.eboInstances = roundUpInstanceCapacity(count);
            grew = true;
        }
        if (grew) {
            if (!relayoutAllowed) {
                mc.outInstances = oldOut;
                mc.eboInstances = oldEbo;
                return false;
            }
            if (admissionBackoffActive(mc)) {
                mc.outInstances = oldOut;
                mc.eboInstances = oldEbo;
                return false;
            }
            if (!relayoutPool(mode)) {
                evictStaleCaches();
                if (!relayoutPool(mode)) {
                    mc.outInstances = oldOut;
                    mc.eboInstances = oldEbo;
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean expandPoolEbo(BakedMesh baked, ModeCache mc, int count, int mode,
                                         boolean relayoutAllowed) {
        if (mc.eboCapacity >= count && mc.filled == count) {
            return true;
        }
        int targetCapacity = Math.max(count, Math.max(MIN_EBO_INSTANCES,
                mc.eboCapacity + Math.max(16, mc.eboCapacity >> 2)));
        if (!eboExpansionFits(baked, targetCapacity)) {
            return false;
        }
        if (!relayoutAllowed && (count > mc.eboInstances || targetCapacity > mc.eboInstances)) {
            return false;
        }
        if (count > mc.eboInstances) {
            int oldEbo = mc.eboInstances;
            mc.eboInstances = Math.max(count, mc.eboInstances + Math.max(16, mc.eboInstances >> 2));
            if (!relayoutPool(mode)) {
                mc.eboInstances = oldEbo;
                return false;
            }
        }
        PassPool pool = pool(mode);
        if (targetCapacity > mc.eboInstances) {
            int oldEbo = mc.eboInstances;
            mc.eboInstances = targetCapacity;
            if (!relayoutPool(mode)) {
                mc.eboInstances = oldEbo;
                return false;
            }
        }
        if (pool.eboSsbo > 0 && dispatchIdentityEboFill(baked, pool.eboSsbo,
                targetCapacity, baked.vertBase[mode], baked.eboWordBase[mode])) {
            mc.eboCapacity = targetCapacity;
            mc.filled = count;
            return true;
        }
        IntBuffer data = MemoryUtil.memAllocInt((int) ((long) baked.idxCount * targetCapacity));
        try {
            int vertBase = baked.vertBase[mode];
            for (int i = 0; i < targetCapacity; i++) {
                int base = vertBase + i * baked.vertCount;
                for (int k = 0; k < baked.idxCount; k++) {
                    data.put(baked.baseIndices[k] + base);
                }
            }
            data.flip();
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, pool.eboSsbo);
            GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER,
                    (long) baked.eboWordBase[mode] * 4L, data);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            mc.eboCapacity = targetCapacity;
            mc.filled = count;
        } finally {
            MemoryUtil.memFree(data);
        }
        return true;
    }

    public static boolean relayoutPool(int mode) {
        PassPool pool = pool(mode);
        int stride = outStrideBytes(mode);
        // Evict genuinely stale caches FIRST before computing prefix-sum offsets so
        // an eviction never mutates mc.outInstances after newVertBases[] was calculated.
        synchronized (BakeVramPoolManager.class) {
            if (cacheBytes > cacheBudgetBytes()) {
                evictStaleCaches();
            }
        }
        long verts = 0, words = 0;
        long outNeeded = 0, eboNeeded = 0;
        int n = BAKED.size();
        int[] newVertBases = new int[n];
        int[] newEboBases = new int[n];
        long validOutCopyBytes = 0L;
        long validEboCopyBytes = 0L;
        boolean firstShiftEncountered = false;
        for (int i = 0; i < n; i++) {
            BakedMesh bm = BAKED.get(i);
            ModeCache mc = bm.cache(mode);
            newVertBases[i] = (int) verts;
            newEboBases[i] = (int) words;
            verts += (long) bm.vertCount * mc.outInstances;
            words += (long) bm.idxCount * mc.eboInstances;
            outNeeded += (long) bm.vertCount * mc.outInstances * stride;
            eboNeeded += (long) bm.idxCount * mc.eboInstances * 4L;

            if (!firstShiftEncountered) {
                if (bm.vertBase[mode] != newVertBases[i] || bm.eboWordBase[mode] != newEboBases[i]) {
                    firstShiftEncountered = true;
                } else {
                    if (mc.outInstances > 0 && mc.outputValid && mc.recordCount > 0) {
                        validOutCopyBytes = (long) newVertBases[i] * stride + (long) bm.vertCount * mc.recordCount * stride;
                    }
                    if (!NucleusGpuBaker.cullEnabled() && mc.filled > 0) {
                        validEboCopyBytes = (long) newEboBases[i] * 4L + (long) bm.idxCount * mc.filled * 4L;
                    }
                }
            }
        }
        if (verts > Integer.MAX_VALUE || words > Integer.MAX_VALUE
                || outNeeded > MAX_SINGLE_BUFFER_BYTES || eboNeeded > MAX_SINGLE_BUFFER_BYTES) {
            return false;
        }
        if (!ensurePoolStorage(pool, outNeeded, eboNeeded, validOutCopyBytes, validEboCopyBytes)) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            BakedMesh bm = BAKED.get(i);
            ModeCache mc = bm.cache(mode);
            boolean shifted = bm.vertBase[mode] != newVertBases[i]
                    || bm.eboWordBase[mode] != newEboBases[i];
            bm.vertBase[mode] = newVertBases[i];
            bm.eboWordBase[mode] = newEboBases[i];
            if (shifted && mc.outInstances > 0) {
                invalidateBakedOutput(mc);
            }
        }
        pool.lastRelayoutFrame = frameStamp();
        return true;
    }

    public static long poolStoreTarget(long needed) {
        long withHeadroom = needed + (needed >> 3); // 12.5% headroom instead of 50%
        return Math.min(MAX_SINGLE_BUFFER_BYTES, Math.max(withHeadroom, MIN_POOL_BYTES));
    }

    public static void invalidateBakedOutput(ModeCache mc) {
        mc.outputValid = false;
        mc.cullValid = false;
        mc.countsDirty = false;
        mc.stagingSlot = -1;
        mc.filled = -1;
        mc.eboCapacity = 0;
        mc.frustumStamp = Long.MIN_VALUE;
        mc.sourceMutationEpoch = -1L;
    }

    public static boolean ensurePoolStorage(PassPool pool, long outNeeded, long eboNeeded,
                                            long validOutCopyBytes, long validEboCopyBytes) {
        try {
            boolean vaoNeedsRebuild = false;
            if (outNeeded > pool.outputAllocBytes) {
                long target = Math.max(outNeeded, poolStoreTarget(outNeeded));
                if (target > MAX_SINGLE_BUFFER_BYTES) {
                    return false;
                }
                // If no valid bytes need copying (or if the copy would exceed half a gigabyte,
                // risking peak double-allocation OOM), release the old buffer BEFORE allocating
                // the new one and invalidate cached outputs so they re-expand cleanly.
                boolean preserveOld = pool.outputSsbo != -1 && pool.outputAllocBytes > 0
                        && validOutCopyBytes > 0 && (pool.outputAllocBytes + target <= MAX_SINGLE_BUFFER_BYTES);
                if (!preserveOld && pool.outputSsbo != -1) {
                    GL15.glDeleteBuffers(pool.outputSsbo);
                    synchronized (BakeVramPoolManager.class) {
                        cacheBytes -= pool.outputAllocBytes;
                    }
                    pool.outputSsbo = -1;
                    pool.outputAllocBytes = 0;
                    int mode = modeOfPool(pool);
                    for (int i = 0; i < BAKED.size(); i++) {
                        invalidateBakedOutput(BAKED.get(i).cache(mode));
                    }
                }
                while (GL11.glGetError() != GL11.GL_NO_ERROR) { /* drain */ }
                int newSsbo = GL15.glGenBuffers();
                GlDebugMarkers.labelBuffer(newSsbo, "HBM_PoolOut_" + poolName(pool) + "_SSBO");
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, newSsbo);
                GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, target, GL15.GL_DYNAMIC_COPY);
                if (GL11.glGetError() == GL11.GL_OUT_OF_MEMORY) {
                    GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
                    GL15.glDeleteBuffers(newSsbo);
                    MainRegistry.LOGGER.warn("[HBM-M] NucleusGpuBaker: GL_OUT_OF_MEMORY allocating {} MiB pool output SSBO", target >> 20);
                    return false;
                }
                if (preserveOld) {
                    long copyBytes = Math.min(validOutCopyBytes, pool.outputAllocBytes);
                    GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT | GL43.GL_BUFFER_UPDATE_BARRIER_BIT);
                    GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, pool.outputSsbo);
                    GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, newSsbo);
                    GL31.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER, 0L, 0L, copyBytes);
                    GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, 0);
                    GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, 0);
                    GL15.glDeleteBuffers(pool.outputSsbo);
                }
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
                synchronized (BakeVramPoolManager.class) {
                    cacheBytes += target - pool.outputAllocBytes;
                }
                pool.outputSsbo = newSsbo;
                pool.outputAllocBytes = target;
                pool.reallocs++;
                vaoNeedsRebuild = true;
            }
            if (eboNeeded > pool.eboAllocBytes) {
                long target = Math.max(eboNeeded, poolStoreTarget(eboNeeded));
                if (target > MAX_SINGLE_BUFFER_BYTES) {
                    return false;
                }
                boolean preserveOld = !NucleusGpuBaker.cullEnabled() && pool.eboSsbo != -1
                        && pool.eboAllocBytes > 0 && validEboCopyBytes > 0
                        && (pool.eboAllocBytes + target <= MAX_SINGLE_BUFFER_BYTES);
                if (!preserveOld && pool.eboSsbo != -1) {
                    GL15.glDeleteBuffers(pool.eboSsbo);
                    synchronized (BakeVramPoolManager.class) {
                        cacheBytes -= pool.eboAllocBytes;
                    }
                    pool.eboSsbo = -1;
                    pool.eboAllocBytes = 0;
                }
                while (GL11.glGetError() != GL11.GL_NO_ERROR) { /* drain */ }
                int newEbo = GL15.glGenBuffers();
                GlDebugMarkers.labelBuffer(newEbo, "HBM_PoolEbo_" + poolName(pool) + "_EBO");
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, newEbo);
                GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, target, GL15.GL_DYNAMIC_COPY);
                if (GL11.glGetError() == GL11.GL_OUT_OF_MEMORY) {
                    GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
                    GL15.glDeleteBuffers(newEbo);
                    MainRegistry.LOGGER.warn("[HBM-M] NucleusGpuBaker: GL_OUT_OF_MEMORY allocating {} MiB pool EBO", target >> 20);
                    return false;
                }
                if (preserveOld) {
                    long copyBytes = Math.min(validEboCopyBytes, pool.eboAllocBytes);
                    GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT | GL43.GL_ELEMENT_ARRAY_BARRIER_BIT | GL43.GL_BUFFER_UPDATE_BARRIER_BIT);
                    GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, pool.eboSsbo);
                    GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, newEbo);
                    GL31.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER, 0L, 0L, copyBytes);
                    GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, 0);
                    GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, 0);
                    GL15.glDeleteBuffers(pool.eboSsbo);
                }
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
                synchronized (BakeVramPoolManager.class) {
                    cacheBytes += target - pool.eboAllocBytes;
                }
                pool.eboSsbo = newEbo;
                pool.eboAllocBytes = target;
                pool.reallocs++;
                vaoNeedsRebuild = true;
            }
            if (outNeeded == 0 && eboNeeded == 0
                    && (pool.outputAllocBytes > 0 || pool.eboAllocBytes > 0)) {
                if (pool.vao != -1) {
                    GL30.glDeleteVertexArrays(pool.vao);
                    pool.vao = -1;
                }
                if (pool.outputSsbo != -1) {
                    GL15.glDeleteBuffers(pool.outputSsbo);
                    pool.outputSsbo = -1;
                    synchronized (BakeVramPoolManager.class) {
                        cacheBytes -= pool.outputAllocBytes;
                    }
                    pool.outputAllocBytes = 0;
                }
                if (pool.eboSsbo != -1) {
                    GL15.glDeleteBuffers(pool.eboSsbo);
                    pool.eboSsbo = -1;
                    synchronized (BakeVramPoolManager.class) {
                        cacheBytes -= pool.eboAllocBytes;
                    }
                    pool.eboAllocBytes = 0;
                }
            } else if ((pool.vao == -1 || vaoNeedsRebuild) && pool.outputSsbo != -1 && pool.eboSsbo != -1) {
                if (pool.vao != -1) {
                    GL30.glDeleteVertexArrays(pool.vao);
                }
                pool.vao = createOutputVaoWithEbo(pool.outputSsbo, pool.eboSsbo, pool.slim);
                GlDebugMarkers.labelVao(pool.vao, "HBM_Pool_" + poolName(pool) + "_VAO");
            }
            return true;
        } catch (Exception t) {
            MainRegistry.LOGGER.warn(
                    "[HBM-M] NucleusGpuBaker: bake pool allocation failed", t);
            return false;
        }
    }

    public static void maybeShrinkPool(PassPool pool, int mode) {
        long now = frameStamp();
        if (pool.outputSsbo == -1 && pool.eboSsbo == -1) {
            return;
        }
        if (pool.lastRelayoutFrame < 0L || now - pool.lastRelayoutFrame < SHRINK_IDLE_FRAMES) {
            return;
        }
        long outNeeded = 0L, eboNeeded = 0L;
        long stride = outStrideBytes(mode);
        for (int i = 0; i < BAKED.size(); i++) {
            BakedMesh bm = BAKED.get(i);
            ModeCache mc = bm.cache(mode);
            outNeeded += (long) bm.vertCount * mc.outInstances * stride;
            eboNeeded += (long) bm.idxCount * mc.eboInstances * 4L;
        }
        if (outNeeded == 0L && eboNeeded == 0L) {
            ensurePoolStorage(pool, 0L, 0L, 0L, 0L);
            pool.lastRelayoutFrame = now;
            return;
        }
        long outTarget = poolStoreTarget(outNeeded);
        long eboTarget = poolStoreTarget(eboNeeded);
        long outSave = pool.outputAllocBytes - outTarget;
        long eboSave = pool.eboAllocBytes - eboTarget;
        try {
            boolean shrunk = false;
            if (outSave >= SHRINK_MIN_SAVINGS && pool.outputSsbo != -1) {
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, pool.outputSsbo);
                GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, outTarget, GL15.GL_DYNAMIC_COPY);
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
                synchronized (BakeVramPoolManager.class) {
                    cacheBytes -= pool.outputAllocBytes - outTarget;
                }
                pool.outputAllocBytes = outTarget;
                shrunk = true;
            }
            if (eboSave >= SHRINK_MIN_SAVINGS && pool.eboSsbo != -1) {
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, pool.eboSsbo);
                GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, eboTarget, GL15.GL_DYNAMIC_COPY);
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
                synchronized (BakeVramPoolManager.class) {
                    cacheBytes -= pool.eboAllocBytes - eboTarget;
                }
                pool.eboAllocBytes = eboTarget;
                shrunk = true;
            }
            if (shrunk) {
                for (int i = 0; i < BAKED.size(); i++) {
                    invalidateBakedOutput(BAKED.get(i).cache(mode));
                }
                pool.reallocs++;
                pool.shrinks++;
                pool.lastRelayoutFrame = now;
                MainRegistry.LOGGER.info(
                        "[HBM-M] NucleusGpuBaker: {} pool shrunk to {} output / {} EBO bytes (idle release)",
                        mode == NucleusGpuBaker.MODE_SHADOW ? "shadow" : "main", outTarget, eboTarget);
            }
        } catch (Exception t) {
            MainRegistry.LOGGER.warn(
                    "[HBM-M] NucleusGpuBaker: pool shrink failed (sizes unchanged)", t);
        }
    }

    public static void releasePool(PassPool pool) {
        if (pool.vao != -1) {
            GL30.glDeleteVertexArrays(pool.vao);
            pool.vao = -1;
        }
        if (pool.outputSsbo != -1) {
            GL15.glDeleteBuffers(pool.outputSsbo);
            pool.outputSsbo = -1;
            pool.outputAllocBytes = 0;
        }
        if (pool.eboSsbo != -1) {
            GL15.glDeleteBuffers(pool.eboSsbo);
            pool.eboSsbo = -1;
            pool.eboAllocBytes = 0;
        }
        if (pool.cmdSsbo != -1) {
            GL15.glDeleteBuffers(pool.cmdSsbo);
            pool.cmdSsbo = -1;
        }
        pool.cmdSlots = 0;
        pool.cmdGen = 0;
        pool.cmdSlotOwners = new ModeCache[0];
        if (pool.cmdCpuSsbo != -1) {
            GL15.glDeleteBuffers(pool.cmdCpuSsbo);
            pool.cmdCpuSsbo = -1;
        }
        if (pool.paramSsbo != -1) {
            GL15.glDeleteBuffers(pool.paramSsbo);
            pool.paramSsbo = -1;
        }
        if (pool.cmdCpu != null) {
            MemoryUtil.memFree(pool.cmdCpu);
            pool.cmdCpu = null;
        }
    }

    // ── F3 Telemetry ───────────────────────────────────────────────────

    public static long poolMainReallocs() { return POOL_MAIN.reallocs; }
    public static long poolShadowReallocs() { return POOL_SHADOW.reallocs; }
    public static long poolMainShrinks() { return POOL_MAIN.shrinks; }
    public static long poolShadowShrinks() { return POOL_SHADOW.shrinks; }

    public static long poolAllocatedBytes(int mode) {
        synchronized (BakeVramPoolManager.class) {
            return poolAllocatedBytes(pool(mode));
        }
    }

    public static long poolAllocatedBytes(PassPool pool) {
        return pool.outputAllocBytes + pool.eboAllocBytes
                + pool.cmdSlots * INDIRECT_CMD_BYTES
                + (pool.paramSsbo != -1 ? 4L : 0L);
    }

    public static long poolUsedBytes(int mode) {
        synchronized (BakeVramPoolManager.class) {
            long total = 0L;
            for (int i = 0; i < BAKED.size(); i++) {
                BakedMesh bm = BAKED.get(i);
                ModeCache mc = bm.cache(mode);
                total += (long) bm.vertCount * mc.outInstances * outStrideBytes(mode)
                        + (long) bm.idxCount * mc.eboInstances * 4L;
            }
            return total;
        }
    }

    public static long cacheNonPoolAllocatedBytes() {
        synchronized (BakeVramPoolManager.class) {
            long total = 0L;
            for (int i = 0; i < BAKED.size(); i++) {
                BakedMesh bm = BAKED.get(i);
                total += cacheAllocatedBytes(bm.main);
                total += cacheAllocatedBytes(bm.shadow);
            }
            total += NucleusGpuBaker.countsStagingBytes;
            if (NucleusGpuBaker.boneSsbo != -1) total += (long) NucleusGpuBaker.BONE_PALETTE_SIZE * 16L * 4L;
            if (NucleusGpuBaker.bakeParamsUbo != -1) total += 88L;
            return total;
        }
    }

    public static long cacheNonPoolUsedBytes() {
        synchronized (BakeVramPoolManager.class) {
            long total = 0L;
            for (int i = 0; i < BAKED.size(); i++) {
                BakedMesh bm = BAKED.get(i);
                total += cacheUsedBytes(bm.main);
                total += cacheUsedBytes(bm.shadow);
            }
            total += NucleusGpuBaker.countsStagingBytes;
            if (NucleusGpuBaker.boneSsbo != -1) total += (long) NucleusGpuBaker.BONE_PALETTE_SIZE * 16L * 4L;
            if (NucleusGpuBaker.bakeParamsUbo != -1) total += 88L;
            return total;
        }
    }

    public static long allocatedGpuBytes() {
        synchronized (BakeVramPoolManager.class) {
            long total = poolAllocatedBytes(POOL_MAIN) + poolAllocatedBytes(POOL_SHADOW);
            for (int i = 0; i < BAKED.size(); i++) {
                BakedMesh bm = BAKED.get(i);
                total += cacheAllocatedBytes(bm.main);
                total += cacheAllocatedBytes(bm.shadow);
            }
            total += NucleusGpuBaker.countsStagingBytes;
            if (NucleusGpuBaker.boneSsbo != -1) total += (long) NucleusGpuBaker.BONE_PALETTE_SIZE * 16L * 4L;
            if (NucleusGpuBaker.bakeParamsUbo != -1) total += 88L;
            return total;
        }
    }

    public static long usedGpuBytes() {
        synchronized (BakeVramPoolManager.class) {
            long total = 0L;
            for (int mode = NucleusGpuBaker.MODE_SHADOW; mode <= NucleusGpuBaker.MODE_MAIN; mode++) {
                for (int i = 0; i < BAKED.size(); i++) {
                    BakedMesh bm = BAKED.get(i);
                    ModeCache mc = bm.cache(mode);
                    total += (long) bm.vertCount * mc.outInstances * outStrideBytes(mode)
                            + (long) bm.idxCount * mc.eboInstances * 4L;
                }
            }
            for (int i = 0; i < BAKED.size(); i++) {
                BakedMesh bm = BAKED.get(i);
                total += cacheUsedBytes(bm.main);
                total += cacheUsedBytes(bm.shadow);
            }
            total += NucleusGpuBaker.countsStagingBytes;
            if (NucleusGpuBaker.boneSsbo != -1) total += (long) NucleusGpuBaker.BONE_PALETTE_SIZE * 16L * 4L;
            if (NucleusGpuBaker.bakeParamsUbo != -1) total += 88L;
            return total;
        }
    }

    private static long cacheAllocatedBytes(ModeCache mc) {
        return Math.max(mc.recordSize[0], 0L) + Math.max(mc.dirtyBytes, 0L)
                + Math.max(mc.ctlBytes, 0L);
    }

    private static long cacheUsedBytes(ModeCache mc) {
        long records = mc.recordCount >= 0
                ? (long) mc.recordCount * InstancedStaticPartRenderer.INSTANCE_DATA_SIZE * 4L
                : 0L;
        return records + Math.max(mc.dirtyBytes, 0L) + Math.max(mc.ctlBytes, 0L);
    }
}
