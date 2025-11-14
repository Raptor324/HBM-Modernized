package com.hbm_m.block.entity;

// Этот BE представляет собой блок-провод, который может передавать энергию между машинами.
// Он не хранит энергию, а только проксирует запросы в центральный менеджер проводов,
// который реализует алгоритм union-find для эффективного управления сетью проводов.
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

import com.hbm_m.block.WireBlock;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;

// [ИЗМЕНЕНИЕ] Импортируем твои новые классы
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.energy.ILongEnergyStorage;
import com.hbm_m.energy.LongToForgeWrapper;
import com.hbm_m.energy.ForgeToLongWrapper;

import java.util.UUID;

public class WireBlockEntity extends BlockEntity {

    private int recheckTimer = 0;

    // [ИЗМЕНЕНИЕ] Создаем прокси для ОБЕИХ систем
    private final ILongEnergyStorage longEnergyProxy = createLongProxy();
    private final IEnergyStorage forgeEnergyProxy = createForgeProxy(); // Это будет обертка
    private final LazyOptional<ILongEnergyStorage> lazyLongProxy = LazyOptional.of(() -> longEnergyProxy);
    private final LazyOptional<IEnergyStorage> lazyForgeProxy = LazyOptional.of(() -> forgeEnergyProxy);

    public WireBlockEntity(BlockPos pPos, BlockState pState) {
        super(ModBlockEntities.WIRE_BE.get(), pPos, pState);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            // NEW: register this wire in the union-find manager
            com.hbm_m.energy.WireNetworkManager.get().onWireAdded(level, this.worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide()) {
            // NEW: inform manager about removal so it can rebuild components
            com.hbm_m.energy.WireNetworkManager.get().onWireRemoved(level, this.worldPosition);
        }
    }

    /**
     * Этот метод вызывается из WireBlock, когда рядом появляется "проблемный" сосед.
     */
    public void scheduleRecheck() {
        // Увеличиваем таймер до 20 тиков (~1 секунда) — даём больше времени на синхронизацию TE на клиенте.
        this.recheckTimer = 20;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, WireBlockEntity be) {
        if (be.recheckTimer <= 0) {
            return;
        }

        be.recheckTimer--;

        if (be.recheckTimer == 0) {
            // 1. Получаем текущее состояние блока в мире
            BlockState currentState = level.getBlockState(pos);
            if (!(currentState.getBlock() instanceof WireBlock wireBlock)) {
                return; // На всякий случай
            }

            // 2. Вычисляем, каким состояние ДОЛЖНО БЫТЬ, оглядевшись по сторонам.
            //    Мы, по сути, заново запускаем логику установки блока.
            BlockState correctState = wireBlock.defaultBlockState();
            for (Direction dir : Direction.values()) {
                correctState = correctState.setValue(
                        WireBlock.PROPERTIES_MAP.get(dir),
                        wireBlock.canConnectTo(level, pos, dir)
                );
            }

            // 3. Сравниваем. Если текущее состояние на клиенте неверно...
            if (currentState != correctState) {
                level.setBlock(pos, correctState, 2);
            }
        }
    }

    /**
     * Публичный метод для начала запроса энергии из этой точки сети.
     * [ИЗМЕНЕНИЕ] Используем long
     */
    public long requestEnergy(long maxRequest, boolean simulate) {
        Level lvl = this.level;
        if (lvl == null) return 0L;
        // делегируем сложный обход в центральный менеджер (union-find / incremental)
        return com.hbm_m.energy.WireNetworkManager.get().requestEnergy(lvl, this.worldPosition, maxRequest, simulate);
    }

    // Принимает энергию от источника и проксирует её дальше по проводам/соседям.
    // Возвращает количество реально принятое целевыми хранилищами.
    // [ИЗМЕНЕНИЕ] Используем long
    public long acceptEnergy(long amount, UUID pushId) {
        Level lvl = this.level;
        if (lvl == null) return 0L;
        if (ModClothConfig.get().enableDebugLogging) {
            MainRegistry.LOGGER.debug("[WIRE <<<] acceptEnergy id={} pos={} amount={}", pushId, this.worldPosition, amount);
        }

        // Use a visited set of BlockPos to prevent cycles in the wire graph.
        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        return acceptEnergyInternal(amount, pushId, null, visited);
    }

    // acceptEnergy с указанием позиции источника — чтобы не возвращать энергию обратно к origin
    // [ИЗМЕНЕНИЕ] Используем long
    public long acceptEnergy(long amount, UUID pushId, BlockPos origin) {
        Level lvl = this.level;
        if (lvl == null) return 0L;

        if (ModClothConfig.get().enableDebugLogging) {
            MainRegistry.LOGGER.debug("[WIRE <<<] acceptEnergy id={} pos={} amount={} origin={}", pushId, this.worldPosition, amount, origin);
        }

        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        return acceptEnergyInternal(amount, pushId, origin, visited);
    }

    // Internal recursive worker that carries a visited set to avoid infinite loops.
    // [ИЗМЕНЕНИЕ] Используем long
    private long acceptEnergyInternal(long amount, UUID pushId, BlockPos origin, java.util.Set<BlockPos> visited) {
        Level lvl = this.level;
        if (lvl == null) return 0L;

        // Mark this position as visited. If already visited, bail out.
        if (!visited.add(this.worldPosition)) return 0L;

        long totalAccepted = 0L; // [ИЗМЕНЕНИЕ] long

        for (Direction dir : Direction.values()) {
            if (totalAccepted >= amount) break;
            BlockEntity neighbor = lvl.getBlockEntity(worldPosition.relative(dir));
            if (neighbor == null) continue;

            // Don't return energy back to the original source position
            if (origin != null && neighbor.getBlockPos().equals(origin)) continue;

            if (neighbor instanceof WireBlockEntity wireNeighbor) {
                if (visited.contains(wireNeighbor.worldPosition)) continue;
                totalAccepted += wireNeighbor.acceptEnergyInternal(amount - totalAccepted, pushId, origin, visited);
            } else {
                // [ИЗМЕНЕНИЕ] --- Новая логика поиска Capability ---
                ILongEnergyStorage targetStorage = null;

                // 1. Пытаемся найти нашу ILongEnergyStorage
                LazyOptional<ILongEnergyStorage> longCap = neighbor.getCapability(ModCapabilities.LONG_ENERGY, dir.getOpposite());
                if (longCap.isPresent()) {
                    targetStorage = longCap.resolve().orElse(null);
                } else {
                    // 2. Если не нашли, ищем старую IEnergyStorage
                    LazyOptional<IEnergyStorage> forgeCap = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite());
                    if (forgeCap.isPresent()) {
                        IEnergyStorage intStorage = forgeCap.resolve().orElse(null);
                        // 3. Оборачиваем int-хранилище в long-обертку
                        if (intStorage != null) {
                            targetStorage = new ForgeToLongWrapper(intStorage);
                        }
                    }
                }

                // --- Конец новой логики ---

                if (targetStorage != null && targetStorage.canReceive()) {
                    // [ИЗМЕНЕНИЕ] Используем long
                    long accepted = targetStorage.receiveEnergy(amount - totalAccepted, false);
                    if (accepted > 0) {
                        if (ModClothConfig.get().enableDebugLogging) {
                            MainRegistry.LOGGER.debug("[WIRE <<<] Delivered {} FE to {} at {}", accepted, targetStorage.getClass().getSimpleName(), neighbor.getBlockPos());
                        }
                        totalAccepted += accepted;
                    }
                }
            }
        }

        return totalAccepted;
    }

    // Удобная перегрузка с указанием origin
    // [ИЗМЕНЕНИЕ] Используем long
    public long acceptEnergy(long amount, BlockPos origin) {
        return acceptEnergy(amount, UUID.randomUUID(), origin);
    }

    // Удобный перегруз: если вызывающий не хочет сам генерировать UUID.
    // [ИЗМЕНЕНИЕ] Используем long
    public long acceptEnergy(long amount) {
        return acceptEnergy(amount, UUID.randomUUID());
    }

    // [ИЗМЕНЕНИЕ] Создаем "главный" прокси, реализующий ILongEnergyStorage
    private ILongEnergyStorage createLongProxy() {
        return new ILongEnergyStorage() {
            @Override
            public long receiveEnergy(long maxReceive, boolean simulate) {
                // Машины извне, использующие capability, вызывают этот метод.
                // Мы перенаправляем его в наш 'acceptEnergy'.
                // (Примечание: 'simulate' здесь игнорируется, т.к. 'acceptEnergy' не симулируемый)
                // (Передаем null как origin, т.к. это "внешний" пуш)
                if (simulate) return 0L; // Наша логика accept не поддерживает симуляцию
                return WireBlockEntity.this.acceptEnergy(maxReceive, (BlockPos) null);
            }

            @Override
            public long extractEnergy(long maxExtract, boolean simulate) {
                // Машины извне, "тянущие" энергию, вызывают этот метод.
                // Мы перенаправляем его в 'requestEnergy', который обращается к WireNetworkManager.
                return WireBlockEntity.this.requestEnergy(maxExtract, simulate);
            }

            @Override public long getEnergyStored() { return 0L; }
            @Override public long getMaxEnergyStored() { return 0L; }
            @Override public boolean canExtract() { return true; } // Говорим, что можем "извлекать"
            @Override public boolean canReceive() { return true; } // Говорим, что можем "принимать"
        };
    }

    // [ИЗМЕНЕНИЕ] Создаем прокси для Forge, который просто оборачивает наш long-прокси
    private IEnergyStorage createForgeProxy() {
        return new LongToForgeWrapper(this.longEnergyProxy);
    }

    // [ИЗМЕНЕНИЕ] Предоставляем ОБА capability
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.LONG_ENERGY) {
            return lazyLongProxy.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyForgeProxy.cast();
        }
        return super.getCapability(cap, side);
    }

    // [ИZМЕНЕНИЕ] Инвалидируем ОБА прокси
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyLongProxy.invalidate();
        lazyForgeProxy.invalidate();
    }

    // Сохранение и загрузка не нужны, так как у провода нет состояния.
    @Override protected void saveAdditional(@Nonnull CompoundTag pTag) { super.saveAdditional(pTag); }
    @Override public void load(@Nonnull CompoundTag pTag) { super.load(pTag); }
}