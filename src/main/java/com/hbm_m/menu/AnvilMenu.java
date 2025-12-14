package com.hbm_m.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.custom.machines.AnvilBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class AnvilMenu extends AbstractContainerMenu {
    
    public final AnvilBlockEntity blockEntity;
    private final Level level;
    private final ContainerLevelAccess access;

    // Конструктор для клиента
    public AnvilMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, extraData.readBlockPos());
    }

    // Конструктор для клиента с BlockPos
    private AnvilMenu(int containerId, Inventory inv, BlockPos pos) {
        this(containerId, inv, inv.player.level().getBlockEntity(pos));
    }

    // Основной конструктор
    public AnvilMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.ANVIL_MENU.get(), containerId);
        
        if (entity instanceof AnvilBlockEntity anvilEntity) {
            blockEntity = anvilEntity;
        } else {
            blockEntity = new AnvilBlockEntity(BlockPos.ZERO, ModBlocks.ANVIL_IRON.get().defaultBlockState());
        }

        this.level = inv.player.level();
        this.access = ContainerLevelAccess.create(level, blockEntity.getBlockPos());

        // Добавляем инвентарь игрока ПЕРЕД слотами наковальни
        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        IItemHandler itemHandler = blockEntity.getItemHandler();

        // Слот A (вход 1) - индекс 36
        this.addSlot(new SmithingInputSlot(itemHandler, 0, 17, 27));

        // Слот B (вход 2) - индекс 37
        this.addSlot(new SmithingInputSlot(itemHandler, 1, 53, 27));

        // Слот C (выход) - индекс 38, только для извлечения
        this.addSlot(new SmithingOutputSlot(itemHandler, 2, 89, 27));
    }

    public void tryCraft(Player player, boolean craftAll) {
        blockEntity.craft(player, craftAll);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index == 38) {
            // Выходной слот - переносим в инвентарь
            if (!this.moveItemStackTo(sourceStack, 0, 36, true)) {
                return ItemStack.EMPTY;
            }
            sourceSlot.onQuickCraft(sourceStack, copyOfSourceStack);
        } else if (index >= 36 && index <= 37) {
            // Входные слоты - переносим в инвентарь
            if (!this.moveItemStackTo(sourceStack, 0, 36, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Инвентарь игрока - пытаемся переместить во входные слоты
            if (!this.moveItemStackTo(sourceStack, 36, 38, false)) {
                if (index < 27) {
                    if (!this.moveItemStackTo(sourceStack, 27, 36, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 36) {
                    if (!this.moveItemStackTo(sourceStack, 0, 27, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.setByPlayer(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        if (sourceStack.getCount() == copyOfSourceStack.getCount()) {
            return ItemStack.EMPTY;
        }

        sourceSlot.onTake(player, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, blockEntity.getBlockState().getBlock());
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> {
            // Выполняем только на сервере
            if (!level.isClientSide) {
                ItemStackHandler handler = blockEntity.getItemHandler();
                
                // Проходим только по ВХОДНЫМ слотам (0 и 1)
                // Слот 2 (выход) пропускаем, чтобы не дюпать результат
                for (int i = 0; i < 2; i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        // Создаем копию, чтобы безопасно работать
                        ItemStack toReturn = stack.copy();
                        
                        // Очищаем слот в машине
                        handler.setStackInSlot(i, ItemStack.EMPTY);
                        
                        // Пытаемся положить игроку в инвентарь
                        if (!player.getInventory().add(toReturn)) {
                            // Если не влезло полностью - кидаем под ноги игроку
                            player.drop(toReturn, false);
                        } else {
                            // Если влезло частично (add изменяет stack, уменьшая count),
                            // то остаток (если есть) тоже кидаем игроку
                            if (!toReturn.isEmpty()) {
                                player.drop(toReturn, false);
                            }
                        }
                    }
                }
                
                // Очищаем слот выхода, чтобы он не висел "фантомом" до следующего открытия
                // Это важно, если у BlockEntity логика завязана на наличие предметов
                handler.setStackInSlot(2, ItemStack.EMPTY);
            }
        });
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18 + 56));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142 + 56));
        }
    }

    /**
     * Входной слот для ковки - при изменении обновляет рецепт
     */
    private class SmithingInputSlot extends SlotItemHandler {
        public SmithingInputSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            blockEntity.updateCrafting();
        }

        @Override
        public void set(@NotNull ItemStack stack) {
            super.set(stack);
            blockEntity.updateCrafting();
        }

        @Override
        public @NotNull ItemStack remove(int amount) {
            ItemStack result = super.remove(amount);
            blockEntity.updateCrafting();
            return result;
        }
    }

    /**
     * Выходной слот - только для извлечения, при взятии расходует материалы
     */
    private class SmithingOutputSlot extends SlotItemHandler {

        public SmithingOutputSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
            super.onTake(player, stack);
            
            if (!level.isClientSide) {
                blockEntity.handleCombineOutputTaken(player, stack);
            }
        }

        @Override
        public void setChanged() {
            super.setChanged();
            blockEntity.updateCrafting();
        }
    }
}
