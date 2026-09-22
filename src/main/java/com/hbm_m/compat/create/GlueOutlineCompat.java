package com.hbm_m.compat.create;

//? if forge || neoforge {
import java.lang.reflect.Method;
import java.util.Set;

import net.minecraft.core.BlockPos;

/**
 * Рефлективная обёртка над {@code Outliner.showCluster} (catnip/Create).
 *
 * <p>Нужна потому, что Create подключён как {@code :slim} с
 * {@code isTransitive = false} — catnip в compile-класспасе отсутствует, а прямая
 * ссылка на Outliner не скомпилируется. Метод вызывается только когда
 * Create/simulated реально присутствуют (из миксинов их систем выделения),
 * поэтому рефлексия безопасна и дёшева (Method кешируется).
 */
public final class GlueOutlineCompat {

    private GlueOutlineCompat() {}

    /** Слот аутлайнера для расширенного кластера мультиблока при выделении клеем. */
    public static final Object HBM_CLUSTER_SLOT = new Object();

    private static volatile boolean resolved;
    private static Method getInstanceMethod;
    private static Method showClusterMethod;
    private static Object outlinerInstance;

    /**
     * Рисует набор позиций как кластер (как родное выделение клея Create).
     * No-op без Create/catnip или при ошибке рефлексии.
     *
     * @param slot      ключ слота аутлайнера (одинаковый между кадрами для обновления)
     * @param positions позиции блоков кластера
     */
    /**
     * Client: outlines every HBM multiblock that has a part or controller inside {@code box}
     * (the glue selection box). Shared by the honey glue and super glue handlers.
     */
    public static void showMultiblockClusterIn(net.minecraft.world.level.Level level, net.minecraft.world.phys.AABB box) {
        Set<BlockPos> seeds = null;
        int scanned = 0;
        for (BlockPos pos : BlockPos.betweenClosed(
                BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ).offset(1, 1, 1))) {
            if (++scanned > 16384) break;
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
            // IMultiblockPart lives on the block entity, IMultiblockController on the block.
            if (be instanceof com.hbm_m.interfaces.IMultiblockPart
                    || level.getBlockState(pos).getBlock() instanceof com.hbm_m.interfaces.IMultiblockController) {
                if (seeds == null) seeds = new java.util.HashSet<>();
                seeds.add(pos.immutable());
            }
        }
        if (seeds == null) {
            return;
        }
        showCluster(HBM_CLUSTER_SLOT, MultiblockExpander.expandToFullMultiblock(level, seeds));
    }

    public static void showCluster(Object slot, Set<BlockPos> positions) {
        if (!resolve()) return;
        try {
            showClusterMethod.invoke(outlinerInstance, slot, positions);
        } catch (Throwable ignored) {
        }
    }

    private static boolean resolve() {
        if (resolved) return showClusterMethod != null;
        synchronized (GlueOutlineCompat.class) {
            if (resolved) return showClusterMethod != null;
            try {
                Class<?> outlinerClass = findOutlinerClass();
                if (outlinerClass == null) return false;
                getInstanceMethod = outlinerClass.getMethod("getInstance");
                outlinerInstance = getInstanceMethod.invoke(null);
                for (Method m : outlinerClass.getMethods()) {
                    if (!m.getName().equals("showCluster")) continue;
                    Class<?>[] p = m.getParameterTypes();
                    // showCluster(Object slot, Set/Iterable<BlockPos> blocks)
                    if (p.length == 2 && p[0] == Object.class && p[1] == Set.class
                            || p.length == 2 && p[0] == Object.class && p[1] == Iterable.class) {
                        m.setAccessible(true);
                        showClusterMethod = m;
                        break;
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                resolved = true;
            }
            return showClusterMethod != null;
        }
    }

    /** Catnip (Create 6.x) либо старый foundation-пакет. */
    private static Class<?> findOutlinerClass() {
        try {
            return Class.forName("net.createmod.catnip.outliner.Outliner");
        } catch (ClassNotFoundException ignored) {
        }
        try {
            return Class.forName("com.simibubi.create.foundation.outliner.Outliner");
        } catch (ClassNotFoundException ignored) {
        }
        return null;
    }
}
//?}
