package com.hbm_m.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.machine.MachineBatteryBlockEntity;
import com.hbm_m.block.machine.MachineBatteryBlock;
import com.hbm_m.util.LongDataPacker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.SlotItemHandler;

import java.util.Optional;

public class MachineBatteryMenu extends AbstractContainerMenu {
    public final MachineBatteryBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    private static final int PLAYER_INVENTORY_START = 0;
    private static final int PLAYER_INVENTORY_END = 36;
    private static final int TE_INPUT_SLOT = 36;
    private static final int TE_OUTPUT_SLOT = 37;

    // Серверный конструктор
    public MachineBatteryMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.MACHINE_BATTERY_MENU.get(), pContainerId);
        checkContainerSize(inv, 2);

        if (!(entity instanceof MachineBatteryBlockEntity)) {
            throw new IllegalArgumentException("Wrong BlockEntity type!");
        }

        blockEntity = (MachineBatteryBlockEntity) entity;
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            this.addSlot(new SlotItemHandler(handler, 0, 26, 17));  // INPUT
            this.addSlot(new SlotItemHandler(handler, 1, 26, 53));  // OUTPUT
        });

        addDataSlots(data);
    }

    // Клиентский конструктор
    public MachineBatteryMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv,
                inv.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(8));
    }

    // --- Геттеры ---
    public long getEnergy() {
        return LongDataPacker.unpack(this.data.get(0), this.data.get(1));
    }

    public long getMaxEnergy() {
        return LongDataPacker.unpack(this.data.get(2), this.data.get(3));
    }

    public int getEnergyDelta() {
        return this.data.get(4);
    }

    public int getModeOnNoSignal() {
        return this.data.get(5);
    }

    public int getModeOnSignal() {
        return this.data.get(6);
    }

    public int getPriorityOrdinal() {
        return this.data.get(7);
    }

    // --- Shift-Click ---
    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Из инвентаря игрока в машину
        if (index >= PLAYER_INVENTORY_START && index < PLAYER_INVENTORY_END) {
            Optional<IEnergyStorage> energyCapability = sourceStack.getCapability(ForgeCapabilities.ENERGY).resolve();

            if (energyCapability.isPresent()) {
                IEnergyStorage itemEnergy = energyCapability.get();
                boolean moved = false;

                // Если предмет может ОТДАВАТЬ энергию -> INPUT
                if (itemEnergy.canExtract()) {
                    if (moveItemStackTo(sourceStack, TE_INPUT_SLOT, TE_INPUT_SLOT + 1, false)) {
                        moved = true;
                    }
                }

                // Если предмет может ПРИНИМАТЬ энергию -> OUTPUT
                if (!moved && itemEnergy.canReceive()) {
                    if (moveItemStackTo(sourceStack, TE_OUTPUT_SLOT, TE_OUTPUT_SLOT + 1, false)) {
                        moved = true;
                    }
                }

                if (!moved) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            // Из машины в инвентарь игрока
        } else if (index == TE_INPUT_SLOT || index == TE_OUTPUT_SLOT) {
            if (!moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        // Создаем доступ к уровню
        return ContainerLevelAccess.create(level, blockEntity.getBlockPos()).evaluate((level, pos) -> {
            // Получаем блок, на который смотрит игрок
            Block block = level.getBlockState(pos).getBlock();

            // ПРОВЕРКА: Является ли этот блок батарейкой (любой: обычной, литиевой и т.д.)
            if (!(block instanceof MachineBatteryBlock)) {
                return false;
            }

            // Стандартная проверка дистанции (64 блока)
            return pPlayer.distanceToSqr((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D) <= 64.0D;
        }, true);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}