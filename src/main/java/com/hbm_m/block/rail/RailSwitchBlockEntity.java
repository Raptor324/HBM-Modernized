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
    //? if < 1.21.1 {
    public void load(CompoundTag nbt) {
    //?} else {
    /*public void loadAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
    *///?}
        //? if < 1.21.1 {
        super.load(nbt);
        //?} else {
        /*super.loadAdditional(nbt, registries);
        *///?}
        this.isSwitched = nbt.getBoolean("isSwitched");
    }

    @Override
    //? if < 1.21.1 {
    protected void saveAdditional(CompoundTag nbt) {
    //?} else {
    /*protected void saveAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
    *///?}
        //? if < 1.21.1 {
        super.saveAdditional(nbt);
        //?} else {
        /*super.saveAdditional(nbt, registries);
        *///?}
        nbt.putBoolean("isSwitched", this.isSwitched);
    }

    @Override
    //? if < 1.21.1 {
    public CompoundTag getUpdateTag() {
    //?} else {
    /*public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
    *///?}
        CompoundTag tag = new CompoundTag();
        //? if < 1.21.1 {
        saveAdditional(tag);
        //?} else {
        /*saveAdditional(tag, registries);
        *///?}
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
