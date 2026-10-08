package com.hbm_m.block.rail;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code RailStandardSwitch.TileEntityRailSwitch}: Weichenstellung, ohne Tick, per Beschreibungspaket synchronisiert. */
public class RailSwitchBlockEntity extends BlockEntity {

    public boolean isSwitched = false;

    public RailSwitchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RAIL_SWITCH.get(), pos, state);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.isSwitched = nbt.getBoolean("isSwitched");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putBoolean("isSwitched", this.isSwitched);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
