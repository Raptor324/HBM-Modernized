package com.hbm_m.block.entity;

import com.hbm_m.multiblock.IMultiblockPart;
import com.hbm_m.multiblock.PartRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UniversalMachinePartBlockEntity extends BlockEntity implements IMultiblockPart {

    private BlockPos controllerPos;
    private PartRole role = PartRole.DEFAULT;

    public UniversalMachinePartBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.UNIVERSAL_MACHINE_PART_BE.get(), pPos, pBlockState);
    }

    @Override
    public void setControllerPos(BlockPos pos) {
        this.controllerPos = pos;
        setChanged();
    }

    @Override
    public void setPartRole(PartRole role) {
        if (this.role != role) {
            this.role = role;
            this.setChanged();
            if (level != null && !level.isClientSide()) {
                // ТОЛЬКО отправляем обновление BlockEntity, БЕЗ updateNeighborsAt
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public BlockPos getControllerPos() {
        return this.controllerPos;
    }

    @Override
    public PartRole getPartRole() {
        return this.role;
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (this.controllerPos == null || this.level == null) {
            return super.getCapability(cap, side);
        }

        // Делегируем ВСЕ capability запросы контроллеру напрямую
        // Контроллер сам решит, что вернуть на основании своей логики
        if (cap == ForgeCapabilities.ENERGY && this.role == PartRole.ENERGY_CONNECTOR) {
            BlockEntity controllerBE = this.level.getBlockEntity(this.controllerPos);
            if (controllerBE != null) {
                return controllerBE.getCapability(cap, side);
            }
        }

        // Для ITEM_HANDLER просто пробрасываем к контроллеру
        // Контроллер MachineAdvancedAssemblerBlockEntity уже возвращает правильный itemHandler
        if (cap == ForgeCapabilities.ITEM_HANDLER && 
            (this.role == PartRole.ITEM_INPUT || this.role == PartRole.ITEM_OUTPUT)) {
            BlockEntity controllerBE = this.level.getBlockEntity(this.controllerPos);
            if (controllerBE != null) {
                return controllerBE.getCapability(cap, side);
            }
        }

        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        if (this.controllerPos != null) {
            pTag.put("ControllerPos", NbtUtils.writeBlockPos(this.controllerPos));
        }
        pTag.putString("PartRole", this.role.name());
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        if (pTag.contains("ControllerPos")) {
            this.controllerPos = NbtUtils.readBlockPos(pTag.getCompound("ControllerPos"));
        }
        if (pTag.contains("PartRole")) {
            try {
                this.role = PartRole.valueOf(pTag.getString("PartRole"));
            } catch (IllegalArgumentException e) {
                this.role = PartRole.DEFAULT;
            }
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        // УДАЛЕНЫ все updateNeighborsAt - они вызывают cascade updates
    }
}
