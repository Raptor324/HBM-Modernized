package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineBreederBlockEntity;
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
 * Two-slot menu (input/output) matching {@code gui_breeder.png}'s actual layout 1:1 - slot
 * coordinates reverse-engineered from the texture itself (input at 35,35; output at 125,35;
 * flanking a vertical flux-progress bar at 73,19).
 */
public class MachineBreederMenu extends AbstractContainerMenu implements ILongEnergyMenu {

    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;
    private static final int MACHINE_SLOTS = 2;

    private final MachineBreederBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;
    private final Player player;
    private final ModItemStackHandlerContainer machineInventory;

    private long clientEnergy;
    private long clientMaxEnergy;

    public MachineBreederMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, getBlockEntity(inv, extraData));
    }

    public MachineBreederMenu(int id, Inventory inv, MachineBreederBlockEntity blockEntity) {
        this(id, inv, blockEntity, blockEntity.getContainerData());
    }

    public MachineBreederMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.BREEDER_MENU.get(), id);
        this.blockEntity = (MachineBreederBlockEntity) entity;
        this.level = inv.player.level();
        this.data = data;
        this.player = inv.player;

        checkContainerDataCount(data, 2);
        addDataSlots(data);

        this.machineInventory = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        this.addSlot(new Slot(machineInventory, SLOT_INPUT, 35, 35));
        this.addSlot(new Slot(machineInventory, SLOT_OUTPUT, 125, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, 142));
        }
    }

    private static MachineBreederBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf data) {
        BlockEntity be = inv.player.level().getBlockEntity(data.readBlockPos());
        if (be instanceof MachineBreederBlockEntity breeder) {
            return breeder;
        }
        throw new IllegalStateException("BlockEntity is not a Breeder");
    }

    public MachineBreederBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getProgress() {
        return data.get(0);
    }

    public int getMaxProgress() {
        return data.get(1);
    }

    public int getProgressScaled(int scale) {
        int max = getMaxProgress();
        return max == 0 ? 0 : getProgress() * scale / max;
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

        int playerStart = MACHINE_SLOTS;
        int playerEnd = slots.size();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(stack, SLOT_INPUT, SLOT_INPUT + 1, false)) {
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
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.BREEDER.get());
    }
}
