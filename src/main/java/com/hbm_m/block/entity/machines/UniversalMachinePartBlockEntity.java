package com.hbm_m.block.entity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.interfaces.IMultiblockPart;
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

public class UniversalMachinePartBlockEntity extends BlockEntity implements IMultiblockPart {

    private BlockPos controllerPos;
    private PartRole role = PartRole.DEFAULT;
    private java.util.Set<Direction> allowedClimbSides = java.util.EnumSet.noneOf(Direction.class);
    /** Мировые стороны энергоподключения; пусто = не задано (для коннектора - все стороны). */
    private java.util.Set<Direction> allowedEnergySides = java.util.EnumSet.noneOf(Direction.class);

    public UniversalMachinePartBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.UNIVERSAL_MACHINE_PART_BE.get(), pPos, pBlockState);
    }

    @Override
    public synchronized void setControllerPos(BlockPos pos) {
        this.controllerPos = pos;
        setChanged();
    }

    @Override
    public void setPartRole(PartRole role) {
        if (this.role != role) {
            boolean wasEnergy = this.role.canReceiveEnergy() || this.role.canSendEnergy();
            boolean isEnergy = role.canReceiveEnergy() || role.canSendEnergy();
            boolean wasFluid = isFluidConnector(this.role);
            boolean isFluid  = isFluidConnector(role);
            this.role = role;
            this.setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
                // Уведомляем соседей при смене роли, влияющей на соединения (провода, трубы, etc.)
                if (wasEnergy || isEnergy || wasFluid || isFluid) {
                    level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
                }
            }
        }
    }

    private static boolean isFluidConnector(PartRole r) {
        return r == PartRole.FLUID_CONNECTOR || r == PartRole.UNIVERSAL_CONNECTOR;
    }

    @Override
    public BlockPos getControllerPos() {
        return this.controllerPos;
    }

    @Override
    public PartRole getPartRole() {
        return this.role;
    }

    @Override
    public void setAllowedClimbSides(java.util.Set<Direction> sides) {
        this.allowedClimbSides = java.util.EnumSet.copyOf(sides);
        this.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public java.util.Set<Direction> getAllowedClimbSides() {
        return this.allowedClimbSides;
    }

    @Override
    public void setAllowedEnergySides(java.util.Set<Direction> sides) {
        this.allowedEnergySides = java.util.EnumSet.copyOf(sides);
        this.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    public java.util.Set<Direction> getAllowedEnergySides() {
        return this.allowedEnergySides;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // При загрузке мира роль восстанавливается из NBT, минуя setPartRole.
        // Уведомляем соседей, чтобы трубы/провода обновили визуальные соединения.
        if (level != null && !level.isClientSide() &&
                (isFluidConnector(role) || role.canReceiveEnergy() || role.canSendEnergy())) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        var level = this.level;
        if (this.controllerPos == null || level == null) {
            return super.getCapability(cap, side);
        }

        BlockEntity controllerBE = level.getBlockEntity(this.controllerPos);
        if (controllerBE == null) {
            return super.getCapability(cap, side);
        }

        // === ДЕЛЕГИРОВАНИЕ ЭНЕРГИИ ===
        // ENERGY_CONNECTOR и UNIVERSAL_CONNECTOR оба принимают/отдают энергию (PartRole.canReceiveEnergy/canSendEnergy)
        if (this.role.canReceiveEnergy() || this.role.canSendEnergy()) {
            boolean energySideOk = side == null
                    || allowedEnergySides.isEmpty()
                    || allowedEnergySides.contains(side);
            if (!energySideOk) {
                return super.getCapability(cap, side);
            }

            // HBM API (Provider, Receiver, Connector)
            if (cap == ModCapabilities.HBM_ENERGY_PROVIDER ||
                    cap == ModCapabilities.HBM_ENERGY_RECEIVER ||
                    cap == ModCapabilities.HBM_ENERGY_CONNECTOR)
            {
                return controllerBE.getCapability(cap, side);
            }

            // Forge Energy API (как и было)
            if (cap == ForgeCapabilities.ENERGY) {
                return controllerBE.getCapability(cap, side);
            }
        }

        // === ДЕЛЕГИРОВАНИЕ ПРЕДМЕТОВ ===
        if (cap == ForgeCapabilities.ITEM_HANDLER &&
                (this.role == PartRole.ITEM_INPUT || this.role == PartRole.ITEM_OUTPUT))
        {
            // MachineAssemblerBlockEntity вернет специальный proxy-handler
            if (controllerBE instanceof MachineAssemblerBlockEntity assembler) {
                return assembler.getItemHandlerForPart(this.role).cast();
            }

            // Для других машин (если появятся) можно делегировать напрямую
            return controllerBE.getCapability(cap, side);
        }

        // === ДЕЛЕГИРОВАНИЕ ЖИДКОСТЕЙ ===
        // Передаём null как side, чтобы контроллер мог отличить внутреннее делегирование
        // от прямого внешнего подключения трубы к лицу блока контроллера.
        if (cap == ForgeCapabilities.FLUID_HANDLER && this.role == PartRole.FLUID_CONNECTOR) {
            return controllerBE.getCapability(cap, null);
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

        if (!allowedClimbSides.isEmpty()) {
            int mask = 0;
            for (Direction dir : allowedClimbSides) mask |= (1 << dir.get3DDataValue());
            pTag.putInt("ClimbSides", mask);
        }
        if (!allowedEnergySides.isEmpty()) {
            int mask = 0;
            for (Direction dir : allowedEnergySides) mask |= (1 << dir.get3DDataValue());
            pTag.putInt("EnergySides", mask);
        }
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
        if (pTag.contains("ClimbSides")) {
            int mask = pTag.getInt("ClimbSides");
            allowedClimbSides.clear();
            for (Direction dir : Direction.values()) {
                if ((mask & (1 << dir.get3DDataValue())) != 0) allowedClimbSides.add(dir);
            }
        }
        if (pTag.contains("EnergySides")) {
            int mask = pTag.getInt("EnergySides");
            allowedEnergySides.clear();
            for (Direction dir : Direction.values()) {
                if ((mask & (1 << dir.get3DDataValue())) != 0) allowedEnergySides.add(dir);
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
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
            
            // Принудительно обновляем состояние блока на клиенте, чтобы обновилась визуализация/логика
            if (level != null && level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    }
}
