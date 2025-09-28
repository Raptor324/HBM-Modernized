package com.hbm_m.block.entity;

// Этот BE представляет собой универсальную часть для всех мультиблочных структур.
// Он хранит позицию контроллера и свою роль (энергия, предметы и т.д.).
// Он делегирует запросы способностей (capabilities) контроллеру в зависимости от своей роли.
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
    // Храним свою роль локально
    private PartRole role = PartRole.DEFAULT;

    public UniversalMachinePartBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.UNIVERSAL_MACHINE_PART_BE.get(), pPos, pBlockState);
    }

    @Override
    public void setControllerPos(BlockPos pos) {
        this.controllerPos = pos;
        setChanged(); // Отмечаем для сохранения
    }
    
    @Override
    public void setPartRole(PartRole role) {
        // Мы обновляем роль только если она действительно изменилась, чтобы избежать лишних пакетов
        if (this.role != role) {
            this.role = role;
            this.setChanged();
            if (level != null && !level.isClientSide()) {
                // Отправляем полное обновление BlockEntity на клиент
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
                // NEW: уведомляем соседей, чтобы они пересчитали свои формы/состояния (например, провода)
                level.updateNeighborsAt(worldPosition, getBlockState().getBlock());

                // NEW: Явно отправляем блок-обновления соседям — это форсит перерисовку/пересчёт моделей проводов
                for (Direction d : Direction.values()) {
                    BlockPos neigh = this.worldPosition.relative(d);
                    BlockState ns = level.getBlockState(neigh);
                    level.sendBlockUpdated(neigh, ns, ns, 3);
                }
            }
        }
    }

    @Override
    public BlockPos getControllerPos() {
        return this.controllerPos;
    }

    // getter для роли, используется клиентом/проводами
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

        // Проверяем НАШУ СОБСТВЕННУЮ, сохраненную роль. Никаких запросов к контроллеру!
        if (cap == ForgeCapabilities.ENERGY && this.role == PartRole.ENERGY_CONNECTOR) {
            BlockEntity controllerBE = this.level.getBlockEntity(this.controllerPos);
            if (controllerBE != null) {
                // Делегируем запрос напрямую контроллеру. Это надежно.
                return controllerBE.getCapability(cap, side);
            }
        }
        // Аналогично для предметов
        if (cap == ForgeCapabilities.ITEM_HANDLER && (this.role == PartRole.ITEM_INPUT || this.role == PartRole.ITEM_OUTPUT)) {
            if (level.getBlockEntity(controllerPos) instanceof MachineAssemblerBlockEntity assembler) {
                return assembler.getItemHandlerForPart(role).cast();
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
        // Сохраняем и загружаем роль
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

    // Этот пакет отправляется методом level.sendBlockUpdated()
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // Данные, которые будут в пакете
    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    // Этот метод вызывается на КЛИЕНТЕ после получения пакета с данными (getUpdatePacket/getUpdateTag)
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        // Запоминаем, была ли у нас роль до того, как мы обработаем новые данные
        boolean wasDefault = this.role == PartRole.DEFAULT;

        // ВАЖНО: Сначала вызываем метод суперкласса.
        // Он берет NBT из пакета и вызывает наш метод load(), обновляя поля `controllerPos` и `role`.
        super.onDataPacket(net, pkt);

        // Теперь, когда наши поля обновлены, мы выполняем нашу логику.
        if (this.level != null && this.level.isClientSide() && wasDefault && this.role == PartRole.ENERGY_CONNECTOR) {
            // Это заставит провод рядом снова запустить свой updateShape -> canConnectTo.
            this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());

            // NEW: явные обновления соседних блоков на клиенте, чтобы форсить обновление моделей/форм
            for (Direction d : Direction.values()) {
                BlockPos neigh = this.worldPosition.relative(d);
                BlockState ns = this.level.getBlockState(neigh);
                this.level.sendBlockUpdated(neigh, ns, ns, 3);
            }
        }
    }
}