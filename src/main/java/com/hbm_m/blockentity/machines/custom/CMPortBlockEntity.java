package com.hbm_m.blockentity.machines.custom;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityProxyCombo().inventory().power().fluid()} fuer {@code cm_port}: die Steuerung traegt sich bei
 * jeder Strukturpruefung als {@code cachedPosition} ein; Inventar-, Fluid- und Stromzugriffe werden an sie
 * weitergereicht. Ohne gueltige Steuerung bleibt der Port stumm.
 */
public class CMPortBlockEntity extends BaseHbmBlockEntity {

    @Nullable
    public BlockPos cachedPosition;

    public CMPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CM_PORT_BE.get(), pos, state);
    }

    public void setCachedPosition(BlockPos pos) {
        if (!pos.equals(cachedPosition)) {
            cachedPosition = pos;
            setChanged();
            //? if neoforge {
            /*invalidateCapabilities(); // NeoForge-Capability-Cache: Anschluss geaendert
            *///?}
        }
    }

    @Nullable
    public CustomMachineBlockEntity getTarget() {
        if (cachedPosition == null || level == null) return null;
        BlockEntity be = level.getBlockEntity(cachedPosition);
        return be instanceof CustomMachineBlockEntity cm ? cm : null;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        if (cachedPosition != null) tag.putIntArray("pos", new int[] { cachedPosition.getX(), cachedPosition.getY(), cachedPosition.getZ() });
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        int[] p = tag.getIntArray("pos");
        cachedPosition = p.length == 3 ? new BlockPos(p[0], p[1], p[2]) : null;
    }

    //? if forge {
    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        CustomMachineBlockEntity target = getTarget();
        if (target != null && target.config != null) {
            if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER
                    || cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER
                    || cap == com.hbm_m.capability.ModCapabilities.HBM_ENERGY_PROVIDER
                    || cap == com.hbm_m.capability.ModCapabilities.HBM_ENERGY_RECEIVER
                    || cap == com.hbm_m.capability.ModCapabilities.HBM_ENERGY_CONNECTOR) {
                return target.getCapability(cap, side);
            }
        }
        return super.getCapability(cap, side);
    }
    //?} elif neoforge {
    /*@Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        CustomMachineBlockEntity target = getTarget();
        if (target != null && target.config != null) {
            if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER
                    || cap == com.hbm_m.platform.HbmCap.FLUID_HANDLER
                    || cap == com.hbm_m.platform.HbmCap.HBM_ENERGY_PROVIDER
                    || cap == com.hbm_m.platform.HbmCap.HBM_ENERGY_RECEIVER
                    || cap == com.hbm_m.platform.HbmCap.HBM_ENERGY_CONNECTOR) {
                return com.hbm_m.platform.HbmCaps.get(target, cap, side);
            }
        }
        return super.getHbmCapability(cap, side);
    }
    *///?}
}
