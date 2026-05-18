package com.hbm_m.client.render;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}

/**
 * Shared GPU geometry atlas backing the {@link MdiBatchCoordinator}.
 * <p>
 * Holds:
 * <ul>
 *   <li>One vertex VBO ({@code GL_ARRAY_BUFFER}) containing the concatenated
 *       per-part vertex bytes (pos vec3 / normal vec3 / uv vec2, stride 32).</li>
 *   <li>One index EBO ({@code GL_ELEMENT_ARRAY_BUFFER}) with the concatenated
 *       per-part {@code GL_UNSIGNED_INT} indices. Per-part draw commands use
 *       {@code baseVertex} (added to each element index) and {@code firstIndex}
 *       which per the GL spec is a <b>byte offset</b> into the bound element
 *       array buffer (not an element index). Local mesh indices are stored
 *       verbatim (0..N-1 per part).</li>
 *   <li>One instance VBO with the unsliced 30-float instance layout (loc 3..11
 *       with divisor 1), large enough to hold the sum of all per-renderer
 *       {@code MAX_INSTANCES} budgets for a single frame.</li>
 *   <li>One indirect buffer for the {@link MdiBatchCoordinator} to stream
 *       commands into.</li>
 *   <li>One VAO with all of the above pre-bound.</li>
 * </ul>
 * <p>
 * Geometry registration is <b>lazy and growable</b>: each
 * {@link InstancedStaticPartRenderer} that first becomes MDI-eligible calls
 * {@link #registerGeometryIfAbsent} with a copy of its vertex bytes + index
 * stream. When the existing vertex/index buffers don't have room, we
 * reallocate at double capacity and re-upload all known parts. This is rare
 * (happens only on first-frame growth) and amortised across the session.
 * <p>
 * Sliced renderers ({@code useSlicedLight=true}) are NOT supported here today —
 * they use a different attribute layout and a different shader. Adding a
 * second atlas + a second MDI dispatch later is mechanical, but out of scope
 * for the initial integration.
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public final class MdiGeometryAtlas {

    /** Vertex stride in bytes — pos vec3 + normal vec3 + uv vec2 = 32. */
    private static final int VERTEX_STRIDE_BYTES = 32;

    /** Unsliced instance layout: 30 floats per instance (see {@link InstancedStaticPartRenderer}). */
    private static final int INSTANCE_FLOATS = 30;
    /** Byte offset of {@code InstFadeAlpha} within one instance record. */
    private static final int INSTANCE_FADE_FLOAT_OFFSET = 29;

    private static volatile MdiGeometryAtlas INSTANCE;

    /** Slot record returned to the coordinator. */
    public static final class Slot {
        public final int baseVertex;
        /** Byte offset into the element array buffer (OpenGL {@code DrawElementsIndirectCommand.firstIndex}). */
        public final int firstIndexBytes;
        public final int indexCount;
        Slot(int baseVertex, int firstIndexBytes, int indexCount) {
            this.baseVertex = baseVertex;
            this.firstIndexBytes = firstIndexBytes;
            this.indexCount = indexCount;
        }
    }

    /** Cached per-renderer geometry record (so re-uploads can replay all parts). */
    private static final class GeoRecord {
        final ByteBuffer vertexBytesCopy;
        final IntBuffer indicesCopy;
        /** Снимок размеров на момент регистрации — смена без смены ключа рендерера → перерегистрация. */
        final int registeredVertexBytesLen;
        final int registeredIndexCount;
        Slot slot;
        GeoRecord(ByteBuffer vb, IntBuffer ib, int registeredVertexBytesLen, int registeredIndexCount) {
            this.vertexBytesCopy = vb;
            this.indicesCopy = ib;
            this.registeredVertexBytesLen = registeredVertexBytesLen;
            this.registeredIndexCount = registeredIndexCount;
        }
    }

    private boolean ready = false;
    private int vaoId = 0;
    private int vertexVboId = 0;
    private int indexEboId = 0;
    private int instanceVboId = 0;
    private int indirectBufId = 0;

    // Allocated byte capacities and current high-water marks.
    private long vertexCapBytes = 0;
    private long vertexUsedBytes = 0;
    private long indexCapBytes = 0;
    private long indexUsedBytes = 0;
    private long instanceCapInstances = 0;

    /**
     * Per-renderer geometry cache. {@link LinkedHashMap} preserves registration order so
     * buffer repacks after growth stay aligned with stored {@link Slot}s.
     */
    private final Map<InstancedStaticPartRenderer, GeoRecord> geometryByRenderer = new LinkedHashMap<>();

    private MdiGeometryAtlas() { /* lazy init below */ }

    public static MdiGeometryAtlas getOrCreate() {
        MdiGeometryAtlas inst = INSTANCE;
        if (inst != null) return inst;
        synchronized (MdiGeometryAtlas.class) {
            inst = INSTANCE;
            if (inst != null) return inst;
            inst = new MdiGeometryAtlas();
            try {
                inst.initialise();
            } catch (Throwable t) {
                MainRegistry.LOGGER.error("[HBM-M MDI] MdiGeometryAtlas init failed; MDI path disabled", t);
                inst.ready = false;
            }
            INSTANCE = inst;
            return inst;
        }
    }

    /**
     * Полный сброс атласа при F3+T / disconnect. Только render-thread.
     * <p>
     * Без этого {@link #geometryByRenderer} бессрочно держит старые
     * {@link InstancedStaticPartRenderer} как ключи: после {@code clearCaches()}
     * на них больше нет ссылок из BER, но Map не даёт их собрать GC и накапливает
     * вторую/третью копию той же геометрии для новых инстансов рендерера —
     * отсюда «cog рисуется как base химзавода», дыры в полигонах и полный отказ
     * MDI до перезахода (слоты/порядок repack расходятся с ожиданиями).
     */
    public static void resetForResourceLifecycle() {
        if (!RenderSystem.isOnRenderThread()) {
            MainRegistry.LOGGER.warn("[HBM-M MDI] resetForResourceLifecycle skipped (not on render thread)");
            return;
        }
        synchronized (MdiGeometryAtlas.class) {
            MdiGeometryAtlas inst = INSTANCE;
            if (inst == null) return;
            try {
                inst.destroyInternal();
            } catch (Throwable t) {
                MainRegistry.LOGGER.error("[HBM-M MDI] MdiGeometryAtlas reset failed", t);
            } finally {
                INSTANCE = null;
            }
        }
    }

    private void destroyInternal() {
        // #region agent log
        int ms = geometryByRenderer.size();
        MdiDebugNdjson.log("H2", "MdiGeometryAtlas.destroyInternal", "atlas reset begin",
                "{\"mapSize\":" + ms + ",\"vUsed\":" + vertexUsedBytes + ",\"iUsed\":" + indexUsedBytes + "}");
        // #endregion agent log
        for (GeoRecord rec : geometryByRenderer.values()) {
            try {
                if (rec.vertexBytesCopy != null) {
                    MemoryUtil.memFree(rec.vertexBytesCopy);
                }
                if (rec.indicesCopy != null) {
                    MemoryUtil.memFree(rec.indicesCopy);
                }
            } catch (Throwable t) {
                MainRegistry.LOGGER.warn("[HBM-M MDI] GeoRecord native free: {}", t.toString());
            }
        }
        geometryByRenderer.clear();
        vertexUsedBytes = 0L;
        indexUsedBytes = 0L;

        if (!ready) {
            return;
        }
        GLCapabilitiesGuard guard = GLCapabilitiesGuard.snapshot();
        try {
            if (vaoId != 0) {
                GL30.glDeleteVertexArrays(vaoId);
            }
            if (vertexVboId != 0) {
                GL15.glDeleteBuffers(vertexVboId);
            }
            if (indexEboId != 0) {
                GL15.glDeleteBuffers(indexEboId);
            }
            if (instanceVboId != 0) {
                GL15.glDeleteBuffers(instanceVboId);
            }
            if (indirectBufId != 0) {
                GL15.glDeleteBuffers(indirectBufId);
            }
        } finally {
            guard.restore();
        }
        vaoId = 0;
        vertexVboId = 0;
        indexEboId = 0;
        instanceVboId = 0;
        indirectBufId = 0;
        vertexCapBytes = 0L;
        indexCapBytes = 0L;
        instanceCapInstances = 0L;
        ready = false;
        MainRegistry.LOGGER.info("[HBM-M MDI] MdiGeometryAtlas reset (resource lifecycle)");
    }

    private void initialise() {
        if (!RenderSystem.isOnRenderThread()) {
            MainRegistry.LOGGER.warn("[HBM-M MDI] Atlas init invoked off render thread; deferring");
            return;
        }
        GLCapabilitiesGuard guard = GLCapabilitiesGuard.snapshot();
        try {
            vaoId = GL30.glGenVertexArrays();
            vertexVboId = GL15.glGenBuffers();
            indexEboId = GL15.glGenBuffers();
            instanceVboId = GL15.glGenBuffers();
            indirectBufId = GL15.glGenBuffers();

            if (vaoId == 0 || vertexVboId == 0 || indexEboId == 0 || instanceVboId == 0 || indirectBufId == 0) {
                throw new IllegalStateException("Failed to generate one or more atlas GL objects");
            }

            // Start with a modest 256 KB vertex / 64 KB index allocation; grow on demand.
            vertexCapBytes = 256L * 1024L;
            indexCapBytes = 64L * 1024L;
            instanceCapInstances = 4096L; // grow on demand

            GL30.glBindVertexArray(vaoId);

            // Vertex buffer + per-vertex attribute pointers (pos / normal / uv).
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexVboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexCapBytes, GL15.GL_STATIC_DRAW);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, VERTEX_STRIDE_BYTES, 0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, VERTEX_STRIDE_BYTES, 12);
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, VERTEX_STRIDE_BYTES, 24);

            // Index buffer.
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexEboId);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexCapBytes, GL15.GL_STATIC_DRAW);

            // Instance VBO + per-instance attribute pointers (loc 3..11), all
            // with divisor 1 — must mirror the unsliced layout in
            // InstancedStaticPartRenderer EXACTLY.
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, instanceCapInstances * INSTANCE_FLOATS * 4L, GL15.GL_STREAM_DRAW);
            int stride = INSTANCE_FLOATS * 4;
            // InstPos vec3 @ 0
            GL20.glEnableVertexAttribArray(3);
            GL20.glVertexAttribPointer(3, 3, GL11.GL_FLOAT, false, stride, 0);
            GL33.glVertexAttribDivisor(3, 1);
            // InstRot vec4 @ 12
            GL20.glEnableVertexAttribArray(4);
            GL20.glVertexAttribPointer(4, 4, GL11.GL_FLOAT, false, stride, 12);
            GL33.glVertexAttribDivisor(4, 1);
            // InstBboxMin vec3 @ 28
            GL20.glEnableVertexAttribArray(5);
            GL20.glVertexAttribPointer(5, 3, GL11.GL_FLOAT, false, stride, 28);
            GL33.glVertexAttribDivisor(5, 1);
            // InstBboxSize vec3 @ 40
            GL20.glEnableVertexAttribArray(6);
            GL20.glVertexAttribPointer(6, 3, GL11.GL_FLOAT, false, stride, 40);
            GL33.glVertexAttribDivisor(6, 1);
            // Light vec4 @ 52
            GL20.glEnableVertexAttribArray(7);
            GL20.glVertexAttribPointer(7, 4, GL11.GL_FLOAT, false, stride, 52);
            GL33.glVertexAttribDivisor(7, 1);
            // Light vec4 @ 68
            GL20.glEnableVertexAttribArray(8);
            GL20.glVertexAttribPointer(8, 4, GL11.GL_FLOAT, false, stride, 68);
            GL33.glVertexAttribDivisor(8, 1);
            // Light vec4 @ 84
            GL20.glEnableVertexAttribArray(9);
            GL20.glVertexAttribPointer(9, 4, GL11.GL_FLOAT, false, stride, 84);
            GL33.glVertexAttribDivisor(9, 1);
            // Light vec4 @ 100
            GL20.glEnableVertexAttribArray(10);
            GL20.glVertexAttribPointer(10, 4, GL11.GL_FLOAT, false, stride, 100);
            GL33.glVertexAttribDivisor(10, 1);
            // InstFadeAlpha float @ 116
            GL20.glEnableVertexAttribArray(11);
            GL20.glVertexAttribPointer(11, 1, GL11.GL_FLOAT, false, stride, 116);
            GL33.glVertexAttribDivisor(11, 1);

            GL30.glBindVertexArray(0);
            // Element array binding is part of VAO state — leave it as we set it
            // inside the VAO. The vertex array buffer binding outside the VAO
            // is restored by the guard below.

            ready = true;
            MainRegistry.LOGGER.info("[HBM-M MDI] MdiGeometryAtlas initialised (vao={}, vbo={}, ebo={}, inst={}, indirect={})",
                    vaoId, vertexVboId, indexEboId, instanceVboId, indirectBufId);
        } finally {
            guard.restore();
        }
    }

    public boolean isReady() {
        return ready;
    }

    public int getVaoId() { return vaoId; }
    public int getIndirectBufferId() { return indirectBufId; }
    public int getInstanceFloatsPerInstance() { return INSTANCE_FLOATS; }
    public int getInstanceFadeFloatOffset() { return INSTANCE_FADE_FLOAT_OFFSET; }

    /**
     * Acceptance gate for {@link MdiBatchCoordinator#submit}: only the unsliced
     * 30-float layout is wired into this atlas.
     */
    public boolean acceptsInstanceDataSize(int floatsPerInstance) {
        return ready && floatsPerInstance == INSTANCE_FLOATS;
    }

    /**
     * Текущий {@link Slot} для уже зарегистрированного рендерера после любого
     * {@link #repackGeometryAndRefreshSlots} (рост VBO/EBO). Под {@link MdiBatchCoordinator#dispatch}
     * нужен именно он: {@code submit} копирует смещения в {@code Pending} в
     * порядке {@code flushInstancedBatches}, а между двумя {@code flush}
     * соседних типов машин один {@code registerGeometryIfAbsent} может
     * вызвать repack и пересчитать {@code GeoRecord#slot} у всех записей —
     * старые значения в {@code Pending} тогда указывают в пустоту/чужую
     * геометрию (невидимые двери/сборка, «дырявая» хим установка).
     */
    public synchronized Slot getCurrentSlot(InstancedStaticPartRenderer renderer) {
        if (!ready || renderer == null) return null;
        GeoRecord rec = geometryByRenderer.get(renderer);
        return rec != null ? rec.slot : null;
    }

    /**
     * Удаляет геометрию рендерера из атласа при его {@link InstancedStaticPartRenderer#cleanup()}
     * до полного {@link #resetForResourceLifecycle()}. Иначе в {@link #geometryByRenderer} остаются
     * «зомби»-ключи (cleanup уже освободил retained-буферы, а GeoRecord продолжает участвовать в repack),
     * что ломает порядок/смещения MDI без срабатывания дрейфа слотов в Pending.
     */
    public static void evictRendererIfRegistered(InstancedStaticPartRenderer renderer) {
        if (renderer == null) return;
        MdiGeometryAtlas inst = INSTANCE;
        if (inst == null) return;
        synchronized (inst) {
            if (INSTANCE != inst || !inst.ready) return;
            inst.evictRendererLocked(renderer);
        }
    }

    private void evictRendererLocked(InstancedStaticPartRenderer renderer) {
        GeoRecord rec = geometryByRenderer.remove(renderer);
        if (rec == null) return;
        try {
            if (rec.vertexBytesCopy != null) {
                MemoryUtil.memFree(rec.vertexBytesCopy);
            }
            if (rec.indicesCopy != null) {
                MemoryUtil.memFree(rec.indicesCopy);
            }
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("[HBM-M MDI] evict GeoRecord native free: {}", t.toString());
        }
        // #region agent log
        MdiDebugNdjson.log("H6", "MdiGeometryAtlas.evictRendererLocked", "evicted renderer from atlas",
                "{\"rid\":" + System.identityHashCode(renderer) + ",\"remainingMap\":" + geometryByRenderer.size() + "}");
        // #endregion agent log
        if (geometryByRenderer.isEmpty()) {
            vertexUsedBytes = 0L;
            indexUsedBytes = 0L;
            return;
        }
        repackGeometryAndRefreshSlots();
    }

    /**
     * Lazy-register a renderer's geometry. The first call uploads vertex+index
     * bytes into the atlas; subsequent calls return the cached slot.
     * <p>
     * <b>Atlas growth:</b> if the new geometry doesn't fit in the current
     * VBO/EBO capacity, both buffers are reallocated at next-power-of-two size
     * and ALL known parts are re-uploaded. This is rare (once per part per
     * session); not on the hot path.
     */
    public synchronized Slot registerGeometryIfAbsent(InstancedStaticPartRenderer renderer,
                                                       ByteBuffer vertexBytes,
                                                       IntBuffer indices,
                                                       int indexCount) {
        if (!ready) return null;
        GeoRecord existing = geometryByRenderer.get(renderer);
        if (existing != null && existing.slot != null) {
            if (vertexBytes != null && indices != null && indexCount > 0) {
                int inVertexLen = vertexBytes.remaining();
                if (inVertexLen == existing.registeredVertexBytesLen && indexCount == existing.registeredIndexCount) {
                    return existing.slot;
                }
            }
            evictRendererLocked(renderer);
        }

        if (vertexBytes == null || indices == null || indexCount <= 0) return null;
        long vertexBytesLen = vertexBytes.remaining();
        long indexBytesLen = (long) indexCount * 4L;
        long vertexCount = vertexBytesLen / VERTEX_STRIDE_BYTES;

        // Take a copy so we can replay on growth. We dup() to capture the
        // current position/limit without disturbing the caller's view.
        ByteBuffer vbCopy = MemoryUtil.memAlloc((int) vertexBytesLen);
        ByteBuffer src = vertexBytes.duplicate();
        vbCopy.put(src);
        vbCopy.flip();

        IntBuffer ibCopy = MemoryUtil.memAllocInt(indexCount);
        IntBuffer isrc = indices.duplicate();
        ibCopy.put(isrc);
        ibCopy.flip();

        GeoRecord rec = new GeoRecord(vbCopy, ibCopy, (int) vertexBytesLen, indexCount);
        try {
            // Grow if needed (rare).
            ensureVertexCapacity(vertexUsedBytes + vertexBytesLen);
            ensureIndexCapacity(indexUsedBytes + indexBytesLen);

            int baseVertex = (int) (vertexUsedBytes / VERTEX_STRIDE_BYTES);
            // GL DrawElementsIndirectCommand.firstIndex is a BYTE offset into the EBO, not an element index.
            int firstIndexBytes = (int) indexUsedBytes;

            // Upload at current high-water marks.
            GLCapabilitiesGuard guard = GLCapabilitiesGuard.snapshot();
            try {
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexVboId);
                // LWJGL nio overloads of glBufferSubData consume the buffer
                // (position advances to limit). Upload via a duplicate so the
                // cached copy stays fully readable for any later repack
                // triggered by ensureVertexCapacity / ensureIndexCapacity.
                GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, vertexUsedBytes, vbCopy.duplicate());
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexEboId);
                GL15.glBufferSubData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexUsedBytes, ibCopy.duplicate());
            } finally {
                guard.restore();
            }

            vertexUsedBytes += vertexBytesLen;
            indexUsedBytes += indexBytesLen;

            rec.slot = new Slot(baseVertex, firstIndexBytes, indexCount);
            geometryByRenderer.put(renderer, rec);
            MainRegistry.LOGGER.debug("[HBM-M MDI] Atlas registered renderer {}: verts={}, idx={}, baseVertex={}, firstIndexBytes={}",
                    System.identityHashCode(renderer), vertexCount, indexCount, baseVertex, firstIndexBytes);
            // #region agent log
            MdiDebugNdjson.log("H2", "MdiGeometryAtlas.registerGeometryIfAbsent", "new geo registered",
                    "{\"rid\":" + System.identityHashCode(renderer) + ",\"bv\":" + baseVertex + ",\"fib\":"
                            + firstIndexBytes + ",\"ic\":" + indexCount + ",\"vblen\":" + vertexBytesLen
                            + ",\"mapSize\":" + geometryByRenderer.size() + "}");
            // #endregion agent log
            return rec.slot;
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("[HBM-M MDI] Geometry registration failed", t);
            MemoryUtil.memFree(vbCopy);
            MemoryUtil.memFree(ibCopy);
            return null;
        }
    }

    private void ensureVertexCapacity(long requiredBytes) {
        if (requiredBytes <= vertexCapBytes) return;
        long newCap = vertexCapBytes;
        while (newCap < requiredBytes) newCap *= 2L;
        GLCapabilitiesGuard guard = GLCapabilitiesGuard.snapshot();
        try {
            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexVboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, newCap, GL15.GL_STATIC_DRAW);
            vertexCapBytes = newCap;
        } finally {
            guard.restore();
        }
        // glBufferData wiped the VBO — rebuild layout and refresh every Slot (baseVertex/firstIndexBytes).
        repackGeometryAndRefreshSlots();
    }

    private void ensureIndexCapacity(long requiredBytes) {
        if (requiredBytes <= indexCapBytes) return;
        long newCap = indexCapBytes;
        while (newCap < requiredBytes) newCap *= 2L;
        GLCapabilitiesGuard guard = GLCapabilitiesGuard.snapshot();
        try {
            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexEboId);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, newCap, GL15.GL_STATIC_DRAW);
            indexCapBytes = newCap;
        } finally {
            guard.restore();
        }
        repackGeometryAndRefreshSlots();
    }

    /**
     * Re-uploads all registered parts in {@link #geometryByRenderer} order and recomputes
     * every {@link GeoRecord#slot}. Required after any {@code glBufferData} resize that
     * clears vertex or index storage; also fixes iteration order via {@link LinkedHashMap}
     * so GPU layout always matches {@link Slot} metadata.
     */
    private void repackGeometryAndRefreshSlots() {
        GLCapabilitiesGuard guard = GLCapabilitiesGuard.snapshot();
        int skipped = 0;
        int uploaded = 0;
        try {
            GL30.glBindVertexArray(vaoId);
            long vOff = 0L;
            long iOff = 0L;
            for (GeoRecord rec : geometryByRenderer.values()) {
                if (rec.vertexBytesCopy == null || rec.indicesCopy == null) {
                    skipped++;
                    continue;
                }
                // Always read from position 0..capacity via fresh views so
                // previous glBufferSubData calls (which advance position to
                // limit) don't leave the cached copies "empty" on the next
                // repack. Without this, every record after the first repack
                // would see remaining()==0 and be skipped, leaving its Slot
                // pointing at uninitialised VBO storage post-glBufferData —
                // the root cause of the invisible / duplicated / partial
                // model geometry observed when MDI batching is active.
                ByteBuffer vbView = rec.vertexBytesCopy.duplicate();
                vbView.rewind();
                IntBuffer ibView = rec.indicesCopy.duplicate();
                ibView.rewind();
                int vLen = vbView.remaining();
                int idxCount = ibView.remaining();
                if (vLen <= 0 || idxCount <= 0) {
                    skipped++;
                    continue;
                }
                long idxBytes = (long) idxCount * 4L;
                int baseVertex = (int) (vOff / VERTEX_STRIDE_BYTES);
                int firstIndexBytes = (int) iOff;
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexVboId);
                GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, vOff, vbView);
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexEboId);
                GL15.glBufferSubData(GL15.GL_ELEMENT_ARRAY_BUFFER, iOff, ibView);
                rec.slot = new Slot(baseVertex, firstIndexBytes, idxCount);
                uploaded++;
                vOff += vLen;
                iOff += idxBytes;
            }
            vertexUsedBytes = vOff;
            indexUsedBytes = iOff;
            // #region agent log
            if (skipped > 0) {
                MdiDebugNdjson.log("H3", "MdiGeometryAtlas.repackGeometryAndRefreshSlots",
                        "repack skipped some geo records",
                        "{\"skipped\":" + skipped + ",\"uploaded\":" + uploaded + ",\"mapSize\":"
                                + geometryByRenderer.size() + ",\"vOff\":" + vOff + ",\"iOff\":" + iOff + "}");
            }
            // #endregion agent log
        } finally {
            guard.restore();
        }
    }

    /** Ensure the instance VBO can hold {@code instances} contiguous instance records. */
    public synchronized boolean ensureInstanceCapacity(int instances) {
        if (!ready) return false;
        if (instances <= instanceCapInstances) return true;
        long newCap = instanceCapInstances;
        while (newCap < instances) newCap *= 2L;
        GLCapabilitiesGuard guard = GLCapabilitiesGuard.snapshot();
        try {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, newCap * INSTANCE_FLOATS * 4L, GL15.GL_STREAM_DRAW);
            instanceCapInstances = newCap;
            return true;
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("[HBM-M MDI] Instance VBO grow failed", t);
            return false;
        } finally {
            guard.restore();
        }
    }

    /** Orphan the instance VBO at the start of a frame (driver-friendly streaming). */
    public void orphanInstanceBuffer(int instances) {
        GLCapabilitiesGuard guard = GLCapabilitiesGuard.snapshot();
        try {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, instanceCapInstances * INSTANCE_FLOATS * 4L, GL15.GL_STREAM_DRAW);
        } finally {
            guard.restore();
        }
    }

    /** Upload one window of instance floats at the given instance-float offset. */
    public void uploadInstanceWindow(int floatOffset, FloatBuffer src, int floatCount) {
        GLCapabilitiesGuard guard = GLCapabilitiesGuard.snapshot();
        try {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVboId);
            // FloatBuffer overload of glBufferSubData reads from absolute position
            // 0..remaining(); we want only the first `floatCount` floats.
            FloatBuffer slice = src.duplicate();
            slice.limit(slice.position() + floatCount);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, (long) floatOffset * 4L, slice);
        } finally {
            guard.restore();
        }
    }

    // ---- small GL state snapshot helper ----
    private static final class GLCapabilitiesGuard {
        private final int prevVao;
        private final int prevArrayBuf;
        private final int prevElemBuf;
        private GLCapabilitiesGuard(int vao, int ab, int eb) {
            this.prevVao = vao; this.prevArrayBuf = ab; this.prevElemBuf = eb;
        }
        static GLCapabilitiesGuard snapshot() {
            int v = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            int a = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            int e = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
            return new GLCapabilitiesGuard(v, a, e);
        }
        void restore() {
            try {
                GL30.glBindVertexArray(prevVao);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prevArrayBuf);
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, prevElemBuf);
            } catch (Throwable ignored) {}
        }
        @SuppressWarnings("unused")
        private static void touchGL() { GL.getCapabilities(); }
    }
}
