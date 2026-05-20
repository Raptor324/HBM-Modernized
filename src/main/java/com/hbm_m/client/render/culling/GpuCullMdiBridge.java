package com.hbm_m.client.render.culling;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}

/**
 * Связка BE → cull index для GPU-driven MDI: {@link OcclusionCullingHelper#shouldRender}
 * выставляет индекс, {@link com.hbm_m.client.render.MdiBatchCoordinator} читает при submit.
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public final class GpuCullMdiBridge {

    private static final ThreadLocal<Integer> THREAD_CULL_INDEX = new ThreadLocal<>();

    private GpuCullMdiBridge() {}

  /** {@code -1} = нет привязки (MDI рисует команду как есть). */
    public static final int NO_CULL_INDEX = -1;

    public static void setThreadCullIndex(int index) {
        if (index < 0) {
            THREAD_CULL_INDEX.remove();
        } else {
            THREAD_CULL_INDEX.set(index);
        }
    }

    /** Индекс из staging {@link GpuCullingPipeline} для текущего BE. */
    public static int peekThreadCullIndex() {
        Integer v = THREAD_CULL_INDEX.get();
        return v == null ? NO_CULL_INDEX : v;
    }

    public static void clearThreadCullIndex() {
        THREAD_CULL_INDEX.remove();
    }
}
