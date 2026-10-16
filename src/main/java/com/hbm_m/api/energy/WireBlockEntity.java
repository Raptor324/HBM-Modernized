package com.hbm_m.api.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.hbm_m.blockentity.ModBlockEntities;
//? if forge {
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.interfaces.IEnergyConnector;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
//?}

/**
 * BlockEntity для провода.
 * Проводник энергосети: создает узел (PowerNode) в UniNodespace, энергии не хранит.
 * Аналог TileEntityCableBaseNT из 1.7.10.
 */
public class WireBlockEntity extends BlockEntity implements PowerConductor {

    //? if forge {
    private final LazyOptional<IEnergyConnector> hbmConnector = LazyOptional.of(() -> this);
    private final LazyOptional<net.minecraftforge.energy.IEnergyStorage> feBridge = LazyOptional.of(this::createFeBridge);
    //?}

    public WireBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRE_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WireBlockEntity entity) {
        if (level.isClientSide) return;

        ServerLevel serverLevel = (ServerLevel) level;
        if (Nodespace.getNode(serverLevel, pos) == null) {
            Nodespace.createNode(serverLevel, entity.createNode(pos));
        }
    }

    private void destroyOwnNode() {
        if (this.level != null && !this.level.isClientSide) {
            Nodespace.destroyNode((ServerLevel) this.level, this.getBlockPos());
        }
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return true;
    }

    /**
     * Мост чужой FE на сеть этого провода: чужие кабели/машины, приложенные к проводу,
     * пьют/льют энергию против опубликованных сетью бюджетов (ForeignEnergyBridge).
     */
    private ForeignEnergyBridge bridge() {
        if (this.level == null || this.level.isClientSide) return null;
        Nodespace.PowerNode node = Nodespace.getNode((ServerLevel) this.level, getBlockPos());
        if (node == null || node.net == null || !node.net.isValid()) return null;
        return node.net.foreignBridge;
    }

    private long feKey() {
        return getBlockPos().asLong();
    }

    private long feTick() {
        return level == null ? 0L : level.getGameTime();
    }

    //? if forge {
    private net.minecraftforge.energy.IEnergyStorage createFeBridge() {
        return new net.minecraftforge.energy.IEnergyStorage() {
            @Override public int receiveEnergy(int maxReceive, boolean simulate) {
                ForeignEnergyBridge b = bridge();
                return b == null ? 0 : (int) b.insertFe(maxReceive, feKey(), feTick());
            }
            @Override public int extractEnergy(int maxExtract, boolean simulate) {
                if (simulate) return 0;
                ForeignEnergyBridge b = bridge();
                return b == null ? 0 : (int) b.extractFe(maxExtract);
            }
            @Override public int getEnergyStored() {
                ForeignEnergyBridge b = bridge();
                long fe = b == null ? 0 : b.amountFe();
                return (int) Math.min(Integer.MAX_VALUE, fe);
            }
            @Override public int getMaxEnergyStored() {
                ForeignEnergyBridge b = bridge();
                long fe = b == null ? 0 : b.capacityFe();
                return (int) Math.min(Integer.MAX_VALUE, fe);
            }
            @Override public boolean canReceive() {
                ForeignEnergyBridge b = bridge();
                return b != null && b.insertableFe() > 0;
            }
            @Override public boolean canExtract() {
                ForeignEnergyBridge b = bridge();
                return b != null && b.extractableFe() > 0;
            }
        };
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.HBM_ENERGY_CONNECTOR) {
            return hbmConnector.cast();
        }
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY) {
            return feBridge.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        hbmConnector.invalidate();
        feBridge.invalidate();
    }

    //?}

    //? if neoforge {
    /*private net.neoforged.neoforge.energy.IEnergyStorage feBridgeInstance;

    // Вызывается из регистрации капабилити (ModCapabilities).
    public net.neoforged.neoforge.energy.IEnergyStorage getFeStorage() {
        if (feBridgeInstance == null) {
            feBridgeInstance = new net.neoforged.neoforge.energy.IEnergyStorage() {
                @Override public int receiveEnergy(int maxReceive, boolean simulate) {
                    ForeignEnergyBridge b = bridge();
                    return b == null ? 0 : (int) b.insertFe(maxReceive, feKey(), feTick());
                }
                @Override public int extractEnergy(int maxExtract, boolean simulate) {
                    if (simulate) return 0;
                    ForeignEnergyBridge b = bridge();
                    return b == null ? 0 : (int) b.extractFe(maxExtract);
                }
                @Override public int getEnergyStored() {
                    ForeignEnergyBridge b = bridge();
                    long fe = b == null ? 0 : b.amountFe();
                    return (int) Math.min(Integer.MAX_VALUE, fe);
                }
                @Override public int getMaxEnergyStored() {
                    ForeignEnergyBridge b = bridge();
                    long fe = b == null ? 0 : b.capacityFe();
                    return (int) Math.min(Integer.MAX_VALUE, fe);
                }
                @Override public boolean canReceive() {
                    ForeignEnergyBridge b = bridge();
                    return b != null && b.insertableFe() > 0;
                }
                @Override public boolean canExtract() {
                    ForeignEnergyBridge b = bridge();
                    return b != null && b.extractableFe() > 0;
                }
            };
        }
        return feBridgeInstance;
    }
    *///?}

    // NeoForge has onChunkUnloaded too (IBlockEntityExtension); keeping it in the forge-only
    // block left the node in Nodespace after an unload on 1.21.1.
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        destroyOwnNode();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        destroyOwnNode();
        //? if forge {
        hbmConnector.invalidate();
        //?}
    }

    @Override
    public void setLevel(Level pLevel) {
        super.setLevel(pLevel);
    }
}
