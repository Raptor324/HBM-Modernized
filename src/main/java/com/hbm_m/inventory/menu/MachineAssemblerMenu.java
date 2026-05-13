package com.hbm_m.inventory.menu;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.machines.MachineAssemblerBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.interfaces.ILongEnergyMenu;
import com.hbm_m.item.industrial.ItemAssemblyTemplate;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.api.energy.ItemEnergyAccess;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if fabric {
import team.reborn.energy.api.EnergyStorage;
//?}

@SuppressWarnings("UnstableApiUsage")
public class MachineAssemblerMenu extends AbstractContainerMenu implements ILongEnergyMenu {
    private final MachineAssemblerBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;
    private long clientEnergy;
    private long clientMaxEnergy;
    private final Player player;

    public MachineAssemblerMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(3));
    }

    public MachineAssemblerMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.MACHINE_ASSEMBLER_MENU.get(), pContainerId);
        checkContainerSize(inv, 18);
        this.player = inv.player;
        this.blockEntity = (MachineAssemblerBlockEntity) entity;
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        var handler = this.blockEntity.getInventory();
        var container = new ModItemStackHandlerContainer(handler, this.blockEntity::setChanged);
            // Слот для батареи (0)
            this.addSlot(new Slot(container, 0, 80, 18) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    if (ItemEnergyAccess.getHbmReceiver(stack).isPresent()) return true;
                    //? if fabric {
                    return EnergyStorage.ITEM.find(stack, null) != null;
                    //?} else {
                    /*return false;
                    *///?}
                }
            });
            // Слоты для улучшений (1, 2, 3) - без ограничений
            this.addSlot(new Slot(container, 1, 152, 18));
            this.addSlot(new Slot(container, 2, 152, 36));
            this.addSlot(new Slot(container, 3, 152, 54));

            // Слот для схемы (4)
            this.addSlot(new Slot(container, 4, 80, 54) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    // Разрешаем класть только предметы, являющиеся шаблонами сборщика
                    return stack.getItem() instanceof ItemAssemblyTemplate;
                }
            });

            // Слот для вывода (5)
            this.addSlot(new Slot(container, 5, 134, 90) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    // Запрещаем игроку класть что-либо в выходной слот
                    return false;
                }
            });

            // Слоты для ввода (6-17) - без ограничений
            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 2; col++) {
                    int slotIndex = 6 + (row * 2) + col;
                    int x = 8 + col * 18;
                    int y = 18 + row * 18;
                    this.addSlot(new Slot(container, slotIndex, x, y));
                }
            }

        addDataSlots(data);
    }

    @Override
    public void setEnergy(long energy, long maxEnergy, long delta) {
        this.clientEnergy = energy;
        this.clientMaxEnergy = maxEnergy;
    }

    @Override
    public long getEnergyStatic() {
        if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) {
            return blockEntity.getEnergyStored();
        }
        return clientEnergy;
    }

    @Override
    public long getMaxEnergyStatic() {
        if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) {
            return blockEntity.getMaxEnergyStored();
        }
        return clientMaxEnergy;
    }

    public long getEnergyLong() {
        return getEnergyStatic();
    }

    public long getMaxEnergyLong() {
        return getMaxEnergyStatic();
    }

    @Override
    public long getEnergyDeltaStatic() {
        if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) {
            return blockEntity.getEnergyDelta();
        }
        return 0;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) {
            ModPacketHandler.sendToPlayer((net.minecraft.server.level.ServerPlayer) this.player, ModPacketHandler.SYNC_ENERGY,
                new com.hbm_m.network.packet.PacketSyncEnergy(
                    this.containerId,
                    blockEntity.getEnergyStored(),
                    blockEntity.getMaxEnergyStored(),
                    blockEntity.getEnergyDelta()
                ));
        }
    }

    public boolean isCrafting() {
        // Состояние крафта сместилось на индекс 6
        return data.get(2) > 0;
    }


    public int getProgress() {
        // Прогресс в индексе 0
        return this.data.get(0);
    }

    public int getMaxProgress() {
        // Макс. прогресс в индексе 1
        return this.data.get(1);
    }

    public MachineAssemblerBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    public int getProgressScaled(int width) {
        int progress = this.data.get(0);
        int maxProgress = this.data.get(1);

        return maxProgress != 0 && progress != 0 ? progress * width / maxProgress : 0;
    }

    public int getEnergyScaled(int height) {
        // --- ИЗМЕНЕНИЕ: Используем long-геттеры ---
        long energy = getEnergyLong();
        long maxEnergy = getMaxEnergyLong();

        return maxEnergy != 0 ? (int)(energy * (long)height / maxEnergy) : 0; // (добавил long-каст к height для безопасности)
        // ---
    }

    public int getScaledProgress() {
        int progress = this.data.get(0);
        int maxProgress = this.data.get(1);
        int progressArrowSize = 24; // Ширина стрелки прогресса в пикселях

        return maxProgress != 0 && progress != 0 ? progress * progressArrowSize / maxProgress : 0;
    }

    // Логика Shift-Click, адаптированная из старого кода
    private static final int VANILLA_SLOT_COUNT = 36;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = 36;
    private static final int TE_INVENTORY_SLOT_COUNT = 18;

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player playerIn, int pIndex) {
        Slot sourceSlot = this.slots.get(pIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (pIndex < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // Перемещение из инвентаря игрока в машину
            // Try prioritized placement from player inventory into TE:
            // 1) energy items -> slot 0
            // 2) assembly templates -> slot 4
            // 3) other items -> input slots (6..17)
            boolean moved = false;

            // 1) Energy-capable items -> energy slot (index TE_INVENTORY_FIRST_SLOT_INDEX + 0)
            if (ItemEnergyAccess.getHbmReceiver(sourceStack).isPresent()
                    //? if fabric {
                    || EnergyStorage.ITEM.find(sourceStack, null) != null
                    //?}
            ) {
                moved = this.moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX + 0, TE_INVENTORY_FIRST_SLOT_INDEX + 1, false);
            }

            // 2) Assembly templates -> template slot (index TE_INVENTORY_FIRST_SLOT_INDEX + 4)
            if (!moved && sourceStack.getItem() instanceof ItemAssemblyTemplate) {
                moved = this.moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX + 4, TE_INVENTORY_FIRST_SLOT_INDEX + 5, false);
            }

            // 3) Any other items -> input slots (indices TE_INVENTORY_FIRST_SLOT_INDEX + 6 .. TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT)
            if (!moved) {
                moved = this.moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX + 6, TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT, false);
            }

            // If none of the prioritized moves succeeded, as a fallback try the full TE range
            if (!moved) {
                if (!this.moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // Перемещение из машины в инвентарь игрока
            if (!this.moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            MainRegistry.LOGGER.debug("Invalid slotIndex:" + pIndex);
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(@NotNull Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                pPlayer, ModBlocks.MACHINE_ASSEMBLER.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                // Координаты Y взяты из старого кода: 84 + i * 18 + 56 = 140 + i*18
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 140 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            // Координаты Y: 142 + 56 = 198
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 198));
        }
    }
}