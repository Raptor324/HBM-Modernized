package com.hbm_m.block.entity.doors;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт NEXT {@code BlockEntityBlastDoor} (1:1 с 1.7.10 TileEntityBlastDoor).
 *
 * <p>Ключевые отличия от базовой {@link DoorBlockEntity} (паритет NEXT):
 * <ul>
 *   <li>open()/close() только из терминальных состояний (0/1) — середина хода
 *       необратима (у базового класса open() из state 2 / close() из state 3);</li>
 *   <li>редстоун — только rising edge, опрос в тике ({@link #pollRedstone}),
 *       а не по neighborChanged ({@link #checkRedstonePower()} заглушен);</li>
 *   <li>каскад BFS по горизонтальным соседям при переключении и навесе замка;</li>
 *   <li>звуки REACTOR_START (pitch 0.75) на старте хода и REACTOR_STOP
 *       (pitch 1.0) на финише — серверные, volume 0.5, BLOCKS.</li>
 * </ul>
 */
public class BlastDoorBlockEntity extends DoorBlockEntity {

    /** Полное время хода створки в тиках (NEXT TRAVEL_TICKS). */
    public static final int TRAVEL_TICKS = BlastDoorDecl.TRAVEL_TICKS;

    /** Шаг между сегментами створки в тиках (NEXT STEP_TICKS; документирует тайминги —
     *  разбежка сегментов выражена порогами декларации j/5). */
    public static final int STEP_TICKS = BlastDoorDecl.STEP_TICKS;

    /** Rising-edge защёлка редстоуна (NEXT 'redstoned'). */
    private boolean redstoned;

    public BlastDoorBlockEntity(BlockPos pos, BlockState state) {
        // Свой BlockEntity-тип: при загрузке чанка BE восстанавливается фабрикой типа,
        // поэтому класс BlastDoorBlockEntity выживает перезаход только со своим id.
        super(ModBlockEntities.BLAST_DOOR_ENTITY.get(), pos, state, "blast_door");
    }

    // ==================== Состояния (NEXT open/close/finishOpen/finishClose) ====================

    public boolean canOpen() {
        return state == 0;
    }

    public boolean canClose() {
        return state == 1;
    }

    @Override
    public void open() {
        if (state != 0) return;
        setState((byte) 3);
        cue(ModSounds.REACTOR_START.get(), 0.75F);
    }

    @Override
    public void close() {
        if (state != 1) return;
        setState((byte) 2);
        cue(ModSounds.REACTOR_START.get(), 0.75F);
    }

    /**
     * Порт NEXT BlockEntityBlastDoor.tryToggle (1:1): без отказа «запитанная закрытая
     * дверь рукой не открывается» и без сообщения door_locked — канAccess сам делает
     * tryPick (взлом). Запертая дверь без ключа не переключается (player != null).
     */
    public boolean tryToggle(@Nullable Player player) {
        if (player != null && isLocked() && !canAccess(player)) return false;
        if (canOpen()) {
            open();
            spread(true);
            return true;
        } else if (canClose()) {
            close();
            spread(false);
            return true;
        }
        return false;
    }

    /** Звуковой кубик у позиции двери (NEXT cue: volume 0.5, BLOCKS). */
    private void cue(SoundEvent sound, float pitch) {
        if (level == null) return;
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.5F, pitch);
    }

    // ==================== BFS-каскад (NEXT spread, BlockEntityBlastDoor.java:146-165) ====================

    private void spread(boolean opening) {
        if (level == null) return;
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        seen.add(worldPosition);
        queue.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos at = queue.removeFirst();
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos next = at.relative(dir);
                if (!seen.add(next)) continue;
                if (!(level.getBlockEntity(next) instanceof BlastDoorBlockEntity door)) continue;
                if (door.isLocked() && door.getPins() != getPins()) continue;
                if (opening ? !door.canOpen() : !door.canClose()) continue;
                if (opening) door.open();
                else door.close();
                queue.add(next);
            }
        }
    }

    // ==================== Замок: каскад по соседям (NEXT lock, :167-178) ====================

    @Override
    public void lock() {
        super.lock();
        if (level == null) return;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (!(level.getBlockEntity(worldPosition.relative(dir)) instanceof BlastDoorBlockEntity door)) continue;
            if (door.isLocked()) continue;
            door.setPins(getPins());
            door.lock();
            door.setLockMod(getLockMod());
        }
    }

    // ==================== Редстоун: rising edge в тике (NEXT tickServer, :52-62) ====================

    /**
     * NEXT опрашивает редстоун КАЖДЫЙ серверный тик и реагирует только на фронт:
     * сигнал на ядре ИЛИ на верхней ячейке (+6). Запертая дверь не защёлкивается —
     * снятие замка при поданном сигнале срабатывает на следующем тике.
     */
    private void pollRedstone() {
        if (level == null || level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(worldPosition)
                || level.hasNeighborSignal(worldPosition.above(BlastDoorDecl.LEAF_TOP + 1));
        if (!isLocked() && powered) {
            if (!redstoned) tryToggle(null);
            redstoned = true;
        } else {
            redstoned = false;
        }
    }

    /**
     * NEXT не использует neighborChanged для двери — фронт ловится только в тике
     * (pollRedstone). Базовый опрос всех фантомов здесь лишний.
     */
    @Override
    public void checkRedstonePower() {
        // no-op: порт NEXT — rising edge обнаруживается в pollRedstone каждый тик
    }

    /**
     * Серверный тик: редстоун-опрос → базовый ход створки (openTicks++) →
     * финальный REACTOR_STOP на переходах 3→1 (открылась) и 2→0 (закрылась).
     * Порядок как в NEXT tickServer: poll → travel → finish.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, BlastDoorBlockEntity be) {
        byte before = be.state;
        be.pollRedstone();
        DoorBlockEntity.serverTick(level, pos, state, be);
        if (before == 3 && be.state == 1 || before == 2 && be.state == 0) {
            be.cue(ModSounds.REACTOR_STOP.get(), 1.0F);
        }
    }

    // ==================== NBT (ключ 'redstoned' как в NEXT saveAdditional) ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("redstoned", redstoned);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        this.redstoned = tag.getBoolean("redstoned");
    }
}
