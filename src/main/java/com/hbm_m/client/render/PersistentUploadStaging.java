package com.hbm_m.client.render;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.Iterator;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL44;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.main.MainRegistry;

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
/**
 * Кольцевой persistently-mapped staging-буфер для аплоадов инстанс-данных.
 * <p>
 * CPU пишет span в замапленную память (memcpy без участия драйвера), затем
 * одна команда {@code glCopyBufferSubData} переносит его в целевой VBO.
 * GPU-GPU копия упорядочена в command stream — с draw, читающим целевой VBO,
 * гонки нет; единственная гонка — CPU-запись против незавершённой копии из
 * staging. Она закрывается кольцом + {@code glFenceSync}: каждый кадр
 * {@link #endFrame()} ставит фенс на записанный диапазон, повторное
 * использование диапазона возможно только после {@code glClientWaitSync}
 * (короткие таймауты; при незасигналенном фенсе {@link #copyRegion}
 * возвращает false — вызывающий деградирует в glBufferSubData).
 * <p>
 * {@link GL44#GL_MAP_COHERENT_BIT}: запись CPU видна GPU-копии, выпущенной
 * ПОСЛЕ неё с того же потока; flush-вызовы не нужны.
 * <p>
 * Выделение на процесс (GL-контекст живёт столько же); F3+T не разрушает
 * контекст, пересоздание не требуется.
 */
public final class PersistentUploadStaging {

    private static final int INITIAL_CAPACITY_BYTES = 4 * 1024 * 1024;

    private static PersistentUploadStaging instance;
    private static boolean resolved = false;

    private int bufferId = 0;
    private long capacityBytes = 0;
    private long baseAddr = 0;
    private long writePtr = 0;
    private boolean pendingAny = false;
    private ArrayDeque<FenceRange> fences = new ArrayDeque<>();

    private static final class FenceRange {
        final long sync;
        final long start;
        final long end;

        FenceRange(long sync, long start, long end) {
            this.sync = sync;
            this.start = start;
            this.end = end;
        }
    }

    private PersistentUploadStaging() {}

    /** Текущий инстанс без ленивого создания (для диагностики; null = staging не используется). */
    public static PersistentUploadStaging peekOrNull() {
        return instance;
    }

    /** null, если GL44 persistent mapping недоступен — вызывающие идут через glBufferSubData. */
    public static PersistentUploadStaging getOrCreate() {
        if (resolved) {
            return instance;
        }
        resolved = true;
        try {
            PersistentUploadStaging s = new PersistentUploadStaging();
            if (s.initialise()) {
                instance = s;
                MainRegistry.LOGGER.info("[HBM-M] PersistentUploadStaging ready ({} KiB ring)", INITIAL_CAPACITY_BYTES / 1024);
            }
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("[HBM-M] PersistentUploadStaging unavailable: {}", t.toString());
            instance = null;
        }
        return instance;
    }

    private boolean initialise() {
        GLCapabilities caps = GL.getCapabilities();
        if (caps.glBufferStorage == 0L || caps.glFenceSync == 0L
                || caps.glMapBufferRange == 0L || caps.glCopyBufferSubData == 0L) {
            return false;
        }
        bufferId = GL15.glGenBuffers();
        capacityBytes = INITIAL_CAPACITY_BYTES;
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bufferId);
        GL44.glBufferStorage(GL15.GL_ARRAY_BUFFER, capacityBytes,
                GL44.GL_MAP_WRITE_BIT | GL44.GL_MAP_PERSISTENT_BIT | GL44.GL_MAP_COHERENT_BIT);
        ByteBuffer mapped = GL30.glMapBufferRange(GL15.GL_ARRAY_BUFFER, 0, capacityBytes,
                GL30.GL_MAP_WRITE_BIT | GL44.GL_MAP_PERSISTENT_BIT | GL44.GL_MAP_COHERENT_BIT);
        if (mapped == null) {
            GL15.glDeleteBuffers(bufferId);
            bufferId = 0;
            return false;
        }
        baseAddr = MemoryUtil.memAddress(mapped);
        writePtr = 0;
        return true;
    }

    /**
     * Копирует {@code floatCount} флоатов из {@code src} (с {@code srcFloatOffset})
     * в целевой буфер по байтовому смещению {@code destByteOffset} через staging.
     * @return false — GPU ещё не освободил кольцо, вызывающий обязан уйти в fallback.
     */
    public boolean copyRegion(FloatBuffer src, int srcFloatOffset, int floatCount, int destVbo, long destByteOffset) {
        long bytes = (long) floatCount * 4L;
        if (bytes <= 0) {
            return true;
        }
        if (bytes > capacityBytes) {
            return false;
        }
        if (writePtr + bytes > capacityBytes) {
            writePtr = 0;
        }
        if (!waitForRange(writePtr, bytes)) {
            return false;
        }
        FloatBuffer view = src.duplicate();
        view.position(srcFloatOffset);
        view.limit(srcFloatOffset + floatCount);
        MemoryUtil.memCopy(MemoryUtil.memAddress(view), baseAddr + writePtr, bytes);

        GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, bufferId);
        GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, destVbo);
        GL31.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER,
                writePtr, destByteOffset, bytes);
        writePtr += bytes;
        pendingAny = true;
        return true;
    }

    /** Ёмкость кольца в байтах (0, если staging недоступен). Для F3-метрики VRAM. */
    public long getCapacityBytes() {
        return capacityBytes;
    }

    /** Фенс на всё записанное с прошлого вызова. В конце каждого кадра (present). */
    public void endFrame() {
        if (!pendingAny) {
            return;
        }
        long sync = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        if (sync != 0L) {
            // Диапазон фенса — от начала текущего «прогона» до writePtr; для
            // ожидания достаточно пересечения с [0, writePtr) цикла кольца.
            fences.add(new FenceRange(sync, 0L, writePtr));
        }
        pendingAny = false;
    }

    private boolean waitForRange(long start, long len) {
        long end = start + len;
        Iterator<FenceRange> it = fences.iterator();
        while (it.hasNext()) {
            FenceRange f = it.next();
            if (f.start >= end || start >= f.end) {
                continue;
            }
            int res = GL32.glClientWaitSync(f.sync, 0, 1_000_000L);
            int tries = 0;
            while (res == GL32.GL_TIMEOUT_EXPIRED && tries < 8) {
                res = GL32.glClientWaitSync(f.sync, 0, 2_000_000L);
                tries++;
            }
            if (res != GL32.GL_ALREADY_SIGNALED && res != GL32.GL_CONDITION_SATISFIED) {
                return false;
            }
            GL32.glDeleteSync(f.sync);
            it.remove();
        }
        return true;
    }
}
