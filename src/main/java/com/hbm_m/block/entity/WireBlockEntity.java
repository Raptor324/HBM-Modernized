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

import java.util.UUID;

public class WireBlockEntity extends BlockEntity {

    private int recheckTimer = 0;

    private final IEnergyStorage energyProxy = createEnergyProxy();
    private final LazyOptional<IEnergyStorage> lazyProxy = LazyOptional.of(() -> energyProxy);

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
     */
    public int requestEnergy(int maxRequest, boolean simulate) {
        Level lvl = this.level;
        if (lvl == null) return 0;
        // делегируем сложный обход в центральный менеджер (union-find / incremental)
        return com.hbm_m.energy.WireNetworkManager.get().requestEnergy(lvl, this.worldPosition, maxRequest, simulate);
    }
    
    // Принимает энергию от источника и проксирует её дальше по проводам/соседям.
    // Возвращает количество реально принятое целевыми хранилищами.
    public int acceptEnergy(int amount, UUID pushId) {
        Level lvl = this.level;
        if (lvl == null) return 0;
        if (ModClothConfig.get().enableDebugLogging) {
            MainRegistry.LOGGER.debug("[WIRE <<<] acceptEnergy id={} pos={} amount={}", pushId, this.worldPosition, amount);
        }

        // Use a visited set of BlockPos to prevent cycles in the wire graph.
        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        return acceptEnergyInternal(amount, pushId, null, visited);
    }

    // acceptEnergy с указанием позиции источника — чтобы не возвращать энергию обратно к origin
    public int acceptEnergy(int amount, UUID pushId, BlockPos origin) {
        Level lvl = this.level;
        if (lvl == null) return 0;

        if (ModClothConfig.get().enableDebugLogging) {
            MainRegistry.LOGGER.debug("[WIRE <<<] acceptEnergy id={} pos={} amount={} origin={}", pushId, this.worldPosition, amount, origin);
        }

        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        return acceptEnergyInternal(amount, pushId, origin, visited);
    }

    // Internal recursive worker that carries a visited set to avoid infinite loops.
    private int acceptEnergyInternal(int amount, UUID pushId, BlockPos origin, java.util.Set<BlockPos> visited) {
        Level lvl = this.level;
        if (lvl == null) return 0;

        // Mark this position as visited. If already visited, bail out.
        if (!visited.add(this.worldPosition)) return 0;

        int totalAccepted = 0;

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
                LazyOptional<IEnergyStorage> cap = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite());
                if (cap.isPresent()) {
                    IEnergyStorage target = cap.resolve().orElse(null);
                    if (target != null && target.canReceive()) {
                        int accepted = target.receiveEnergy(amount - totalAccepted, false);
                        if (accepted > 0) {
                            if (ModClothConfig.get().enableDebugLogging) {
                                MainRegistry.LOGGER.debug("[WIRE <<<] Delivered {} FE to {} at {}", accepted, target.getClass().getSimpleName(), neighbor.getBlockPos());
                            }
                            totalAccepted += accepted;
                        }
                    }
                }
            }
        }

        return totalAccepted;
    }

    // Удобная перегрузка с указанием origin
    public int acceptEnergy(int amount, BlockPos origin) {
        return acceptEnergy(amount, UUID.randomUUID(), origin);
    }

    // Удобный перегруз: если вызывающий не хочет сам генерировать UUID.
    public int acceptEnergy(int amount) {
        return acceptEnergy(amount, UUID.randomUUID());
    }
    
    private IEnergyStorage createEnergyProxy() {
        return new IEnergyStorage() {
            // Провода сами по себе не хранят и не отдают энергию, они только передают запросы.
            // Поэтому стандартные методы возвращают 0.
            @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
            @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
            @Override public int getEnergyStored() { return 0; }
            @Override public int getMaxEnergyStored() { return 0; }
            @Override public boolean canExtract() { return true; } // Говорим, что можем "извлекать", чтобы к нам могли подключаться.
            @Override public boolean canReceive() { return true; } // Говорим, что можем "принимать".
        };
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyProxy.cast();
        }
        return super.getCapability(cap, side);
    }
    
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyProxy.invalidate();
    }

    // Сохранение и загрузка не нужны, так как у провода нет состояния.
    @Override protected void saveAdditional(@Nonnull CompoundTag pTag) { super.saveAdditional(pTag); }
    @Override public void load(@Nonnull CompoundTag pTag) { super.load(pTag); }
}