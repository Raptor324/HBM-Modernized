package com.hbm_m.client.model;

import org.jetbrains.annotations.Nullable;

/**
 * Thread-local bridge that passes block entity render attachment data from the
 * chunk rebuild call site ({@code ModelBlockRenderer.tesselateBlock}) to
 * {@code BakedModel.getQuads()} which has no {@code BlockPos} parameter.
 * <p>
 * On Forge this is not needed because {@code getQuads} receives
 * {@code ModelData} directly. On Fabric + Sodium, {@code FabricBakedModel}
 * is not supported without Indium, so we use this thread-local approach
 * instead.
 */
public final class FabricRenderDataBridge {

    private static final ThreadLocal<Object> CURRENT = new ThreadLocal<>();

    private FabricRenderDataBridge() {}

    public static void set(@Nullable Object data) {
        CURRENT.set(data);
    }

    @Nullable
    public static Object get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
