package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineRTGBlockEntity;
import com.hbm_m.interfaces.ILongEnergyMenu;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.packet.PacketSyncEnergy;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Menue des RTG-Generators. Die Steckplatzkoordinaten sind 1:1 aus
 * {@code ContainerMachineRTG} (1.7.10): drei Reihen zu fuenf Pelletplaetzen ab (16, 18),
 * Abstand 18, und das Spielerinventar ab Zeile 106.
 */
public class MachineRTGMenu extends AbstractContainerMenu implements ILongEnergyMenu {

    private static final int MACHINE_SLOTS = MachineRTGBlockEntity.INVENTORY_SIZE;
    private static final int COLUMNS = 5;
    private static final int ROWS = 3;

    private final MachineRTGBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;
    private final Player player;

    private long clientEnergy;
    private long clientMaxEnergy;

    public MachineRTGMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, getBlockEntity(inv, extraData));
    }

    public MachineRTGMenu(int id, Inventory inv, MachineRTGBlockEntity blockEntity) {
        this(id, inv, blockEntity, blockEntity.getContainerData());
    }

    public MachineRTGMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.RTG_MENU.get(), id);
        this.blockEntity = (MachineRTGBlockEntity) entity;
        this.level = inv.player.level();
        this.data = data;
        this.player = inv.player;

        checkContainerDataCount(data, 2);
        addDataSlots(data);

        ModItemStackHandlerContainer machineInventory =
                new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                this.addSlot(new Slot(machineInventory, row * COLUMNS + col, 16 + col * 18, 18 + row * 18));
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 106 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, 164));
        }
    }

    private static MachineRTGBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf data) {
        BlockEntity be = inv.player.level().getBlockEntity(data.readBlockPos());
        if (be instanceof MachineRTGBlockEntity rtg) {
            return rtg;
        }
        throw new IllegalStateException("BlockEntity is not an RTG");
    }

    public MachineRTGBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getHeat() {
        return data.get(0);
    }

    public int getMaxHeat() {
        return data.get(1);
    }

    public int getHeatScaled(int scale) {
        int max = getMaxHeat();
        return max == 0 ? 0 : getHeat() * scale / max;
    }

    public int getPowerScaled(int scale) {
        long max = getMaxEnergyLong();
        return max == 0 ? 0 : (int) (getEnergyLong() * scale / max);
    }

    @Override
    public void setEnergy(long energy, long maxEnergy, long delta) {
        this.clientEnergy = energy;
        this.clientMaxEnergy = maxEnergy;
    }

    @Override
    public long getEnergyStatic() {
        return blockEntity.getEnergyStored();
    }

    @Override
    public long getMaxEnergyStatic() {
        return blockEntity.getMaxEnergyStored();
    }

    @Override
    public long getEnergyDeltaStatic() {
        return 0;
    }

    public long getEnergyLong() {
        if (blockEntity != null && !level.isClientSide) {
            return blockEntity.getEnergyStored();
        }
        return clientEnergy;
    }

    public long getMaxEnergyLong() {
        if (blockEntity != null && !level.isClientSide) {
            return blockEntity.getMaxEnergyStored();
        }
        return clientMaxEnergy;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) {
            ModPacketHandler.sendToPlayer((ServerPlayer) player, ModPacketHandler.SYNC_ENERGY,
                    new PacketSyncEnergy(containerId, blockEntity.getEnergyStored(), blockEntity.getMaxEnergyStored(), 0L));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.MACHINE_RTG.get());
    }
}
