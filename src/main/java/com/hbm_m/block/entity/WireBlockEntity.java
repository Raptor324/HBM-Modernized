package com.hbm_m.block.entity;

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

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class WireBlockEntity extends BlockEntity {

    private static final ThreadLocal<Set<UUID>> HANDLED_REQUESTS = ThreadLocal.withInitial(HashSet::new);

    private final IEnergyStorage energyProxy = createEnergyProxy();
    private final LazyOptional<IEnergyStorage> lazyProxy = LazyOptional.of(() -> energyProxy);

    public WireBlockEntity(BlockPos pPos, BlockState pState) {
        super(ModBlockEntities.WIRE_BE.get(), pPos, pState);
    }

    public static void startNewTick() {
        HANDLED_REQUESTS.get().clear();
    }

    public int requestEnergy(int maxRequest, boolean simulate, UUID requestId) {
        Level lvl = this.level;
        if (lvl == null) return 0;
        if (!HANDLED_REQUESTS.get().add(requestId)) {
            return 0;
        }

        if (ModClothConfig.get().enableDebugLogging) {
            MainRegistry.LOGGER.debug("[WIRE >>>] requestEnergy id={} pos={} need={} simulate={}", requestId, this.worldPosition, maxRequest, simulate);
        }

        int totalExtracted = 0;
        for (Direction dir : Direction.values()) {
            if (totalExtracted >= maxRequest) break;

            BlockEntity neighbor = lvl.getBlockEntity(worldPosition.relative(dir));
            if (neighbor == null) continue;

            // Игнорируем части ассемблера, чтобы не спрашивать энергию у того, кто ее и так запрашивает.
            if (neighbor instanceof MachineAssemblerPartBlockEntity) {
                continue;
            }

            if (neighbor instanceof WireBlockEntity wireNeighbor) {
                totalExtracted += wireNeighbor.requestEnergy(maxRequest - totalExtracted, simulate, requestId);
            } else {
                LazyOptional<IEnergyStorage> cap = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite());
                if (cap.isPresent()) {
                    IEnergyStorage source = cap.resolve().orElse(null);
                    if (source != null && source.canExtract()) {
                        int extracted = source.extractEnergy(maxRequest - totalExtracted, simulate);
                        if (extracted > 0) {
                            if (ModClothConfig.get().enableDebugLogging) {
                                MainRegistry.LOGGER.debug("[WIRE >>>] Pulled {} FE from {} at {}", extracted, source.getClass().getSimpleName(), neighbor.getBlockPos());
                            }
                            totalExtracted += extracted;
                        }
                    }
                }
            }
        }

        return totalExtracted;
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