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
 * A leaked window is force-closed at the end of the server tick (see #endServerTick):
 * a foreign mixin can cancel the engine method at HEAD, and then our pop never runs.
 */
public final class ContraptionAssemblyGuard {

    private ContraptionAssemblyGuard() {}

    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    /**
     * Открыть окно (вызывается из mixin'ов на HEAD методов движков сборки).
     *
     * <p>The depth is not capped. It used to stop incrementing past a limit while pop() always
     * decremented, so nesting deeper than the cap closed the window while the outermost engine was
     * still moving blocks - which is exactly the dupe this guard exists to prevent.</p>
     */
    public static void push() {
        int d = DEPTH.get();
        if (d == 0) {
            com.hbm_m.main.MainRegistry.LOGGER.debug(
                "[HBM] contraption move window opened (thread {})", Thread.currentThread().getName());
        }
        DEPTH.set(d + 1);
    }

    /**
     * Force the window shut at the end of a server tick.
     *
     * <p>The RETURN half of a push/pop pair is not reliable: another mod's mixin can cancel the
     * engine method at HEAD (this modpack has four mods injecting into SubLevelAssemblyHelper), and
     * then our pop is never reached. Measured in game: seven windows opened over a session, none
     * closed - every window stayed open for the whole 30 s failsafe, and breaking or placing an HBM
     * machine in that time did nothing (a part broke without its structure, a placed machine built
     * no structure at all).
     *
     * <p>Every engine block move runs synchronously inside one server tick, so a window that
     * outlives its tick is by definition a leak - closing it here bounds the damage to that tick.
     */
    public static void endServerTick() {
        int d = DEPTH.get();
        if (d > 0) {
            DEPTH.set(0);
            com.hbm_m.main.MainRegistry.LOGGER.debug(
                "[HBM] contraption move window leaked (depth {}), forced shut at tick end", d);
        }
    }

    /** Закрыть окно (вызывается из mixin'ов на RETURN методов движков сборки). */
    public static void pop() {
        int d = DEPTH.get();
        if (d == 1) {
            com.hbm_m.main.MainRegistry.LOGGER.debug(
                "[HBM] contraption move window closed (thread {})", Thread.currentThread().getName());
        }
        DEPTH.set(Math.max(0, d - 1));
    }

    /**
     * @return true, пока идёт перенос блоков движком сборки/разборки.
     */
    public static boolean isMoving() {
        return DEPTH.get() > 0;
    }
}
