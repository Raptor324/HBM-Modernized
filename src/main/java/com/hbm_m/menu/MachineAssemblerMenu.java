package com.hbm_m.menu;

// меню для сборочной машины. Имеет слоты для батареи, улучшений, схемы, ввода и вывода.
// Содержит логику для обработки Shift-клика и отображения прогресса сборки и уровня энергии.

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.MachineAssemblerBlockEntity;
import com.hbm_m.item.ItemAssemblyTemplate;
import com.hbm_m.main.MainRegistry;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

public class MachineAssemblerMenu extends AbstractContainerMenu {
    private final MachineAssemblerBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public MachineAssemblerMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(6));
    }

    public MachineAssemblerMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.MACHINE_ASSEMBLER_MENU.get(), pContainerId);
        checkContainerSize(inv, 18);
        this.blockEntity = (MachineAssemblerBlockEntity) entity;
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            // Слот для батареи (0)
            this.addSlot(new SlotItemHandler(handler, 0, 80, 18) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    // Разрешаем класть только предметы, которые могут хранить энергию
                    return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
                }
            });
            // Слоты для улучшений (1, 2, 3) - без ограничений
            this.addSlot(new SlotItemHandler(handler, 1, 152, 18));
            this.addSlot(new SlotItemHandler(handler, 2, 152, 36));
            this.addSlot(new SlotItemHandler(handler, 3, 152, 54));

            // Слот для схемы (4) - НАШЕ ГЛАВНОЕ ИЗМЕНЕНИЕ
            this.addSlot(new SlotItemHandler(handler, 4, 80, 54) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    // Разрешаем класть только предметы, являющиеся шаблонами сборщика
                    return stack.getItem() instanceof ItemAssemblyTemplate;
                }
            });

            // Слот для вывода (5)
            this.addSlot(new SlotItemHandler(handler, 5, 134, 90) {
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
                    this.addSlot(new SlotItemHandler(handler, slotIndex, x, y));
                }
            }
        });

        addDataSlots(data);
    }
    
    public int getEnergy() {
        return this.data.get(2);
    }
    public int getMaxEnergy() {
        return this.data.get(3);
    }

    public boolean isCrafting() {
        return data.get(0) > 0;
    }

    public int getEnergyDelta() {
        return this.data.get(5);
    }


    public int getProgressScaled(int width) {
        int progress = this.data.get(0);
        int maxProgress = this.data.get(1);

        return maxProgress != 0 && progress != 0 ? progress * width / maxProgress : 0;
    }
    
    public int getEnergyScaled(int height) {
        int energy = this.data.get(2);
        int maxEnergy = this.data.get(3);
        
        return maxEnergy != 0 ? energy * height / maxEnergy : 0;
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
    public @NotNull ItemStack quickMoveStack(@Nonnull Player playerIn, int pIndex) {
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
            if (sourceStack.getCapability(ForgeCapabilities.ENERGY).isPresent()) {
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
    public boolean stillValid(@Nonnull Player pPlayer) {
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