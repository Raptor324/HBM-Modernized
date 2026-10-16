package com.hbm_m.multiblock;

/**
 * Счётчик «окон сборки/разборки» контрапшенов (Create / Sable / Aeronautics).
 *
 * <p>Пока окно активно, движок сборки физически перемещает блоки HBM:
 * <ul>
 *   <li>Create: {@code Contraption.removeBlocksFromWorld} (setBlock AIR) и
 *       {@code Contraption.addBlocksToWorld} (обратная установка);</li>
 *   <li>Sable/simulated: {@code SubLevelAssemblyHelper.moveBlocks} — общая точка
 *       и для сборки корабля ({@code assembleBlocks}), и для разборки
 *       ({@code SimAssemblyHelper.disassembleSubLevel}).</li>
 * </ul>
 *
 * <p>Внутри окна удаление наших блоков — это ПЕРЕНОС, а не разрушение:
 * движок уже сохранил state+NBT и вернёт их на месте. Любая наша реакция
 * на {@code onRemove} (каскад {@code destroyStructure}, дроп станка лут-таблицей,
 * дроп содержимого инвентаря) в этот момент = дюп. Поэтому:
 * <ul>
 *   <li>{@link com.hbm_m.mixin.LevelChunkSilentRemovalMixin} глушит
 *       {@code BlockState#onRemove} для наших блоков внутри окна;</li>
 *   <li>{@link MultiblockStructureHelper#destroyStructure} и
 *       {@link MultiblockStructureHelper#attemptAutoRepair} выходят рано;</li>
 *   <li>{@code UniversalMachinePartBlock#onRemove} не запускает каскад.</li>
 * </ul>
 *
 * <p>ThreadLocal, т.к. вся сборка выполняется синхронно на server thread.
 * Страховка от утечки глубины (исключение внутри чужого метода между push/pop):
 * окно автоматически истекает через 30 секунд реального времени.
 */
public final class ContraptionAssemblyGuard {

    private ContraptionAssemblyGuard() {}

    /** Максимальная вложенность окон (защита от переполнения счётчика). */
    private static final int MAX_DEPTH = 8;

    /** Страховочное время жизни окна: 30 с. Достаточно даже для гигантских сборок. */
    private static final long WINDOW_TIMEOUT_NANOS = 30_000_000_000L;

    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Long> DEADLINE_NANOS = ThreadLocal.withInitial(() -> 0L);

    /** Открыть окно (вызывается из mixin'ов на HEAD методов движков сборки). */
    public static void push() {
        int d = DEPTH.get();
        if (d == 0) {
            DEADLINE_NANOS.set(System.nanoTime() + WINDOW_TIMEOUT_NANOS);
            // Диагностика дюпа: видно, открывается ли окно вообще и на каком потоке.
            com.hbm_m.main.MainRegistry.LOGGER.info(
                "[HBM] окно сборки ОТКРЫТО (thread {})", Thread.currentThread().getName());
        }
        if (d < MAX_DEPTH) {
            DEPTH.set(d + 1);
        }
    }

    /** Закрыть окно (вызывается из mixin'ов на RETURN методов движков сборки). */
    public static void pop() {
        int d = DEPTH.get();
        if (d == 1) {
            com.hbm_m.main.MainRegistry.LOGGER.info(
                "[HBM] окно сборки ЗАКРЫТО (thread {})", Thread.currentThread().getName());
        }
        DEPTH.set(Math.max(0, d - 1));
    }

    /**
     * @return true, пока идёт перенос блоков движком сборки/разборки.
     * Просроченное окно (страховочный таймаут) считается закрытым.
     */
    public static boolean isMoving() {
        int d = DEPTH.get();
        if (d <= 0) return false;
        if (System.nanoTime() > DEADLINE_NANOS.get()) {
            // Утечка (исключение между push/pop) — сбрасываем, чтобы не
            // заблокировать обычное разрушение машин навсегда.
            DEPTH.set(0);
            return false;
        }
        return true;
    }
}
