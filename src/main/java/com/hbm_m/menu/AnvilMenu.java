package com.hbm_m.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.AnvilBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.recipe.AnvilRecipe;
import com.hbm_m.recipe.AnvilRecipeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class AnvilMenu extends AbstractContainerMenu {

    public final AnvilBlockEntity blockEntity;
    private final Level level;

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
        
        if (entity instanceof AnvilBlockEntity) {
            blockEntity = (AnvilBlockEntity) entity;
        } else {
            // Создаём временную заглушку для клиента если entity == null
            blockEntity = new AnvilBlockEntity(BlockPos.ZERO, ModBlocks.ANVIL_BLOCK.get().defaultBlockState());
        }
        
        this.level = inv.player.level();
        
        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        // Слот A (вход 1)
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 0, 27, 17));
        
        // Слот B (вход 2)
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 1, 76, 17));
        
        // Слот C (выход) - только для извлечения
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 2, 134, 17) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });
    }

    // Метод для крафта
    public boolean tryCraft(Player player, boolean craftMax) {
        ItemStack slotA = blockEntity.getItemHandler().getStackInSlot(0);
        ItemStack slotB = blockEntity.getItemHandler().getStackInSlot(1);
        
        Optional<AnvilRecipe> recipeOpt = AnvilRecipeManager.findRecipe(level, slotA, slotB);
        
        if (recipeOpt.isEmpty()) {
            return false;
        }
        
        AnvilRecipe recipe = recipeOpt.get();
        int craftCount = craftMax ? 64 : 1;
        
        // Проверяем, сколько раз можем скрафтить
        int maxCrafts = Math.min(
            slotA.getCount() / recipe.getInputA().getCount(),
            slotB.getCount() / recipe.getInputB().getCount()
        );
        
        // Проверяем наличие ресурсов в инвентаре игрока
        for (ItemStack required : recipe.getRequiredItems()) {
            int available = countItemInInventory(player, required);
            maxCrafts = Math.min(maxCrafts, available / required.getCount());
        }
        
        craftCount = Math.min(craftCount, maxCrafts);
        if (craftCount <= 0) {
            return false;
        }
        
        // Забираем ресурсы из инвентаря
        for (ItemStack required : recipe.getRequiredItems()) {
            removeItemFromInventory(player, required, required.getCount() * craftCount);
        }
        
        // Забираем из слотов
        slotA.shrink(recipe.getInputA().getCount() * craftCount);
        slotB.shrink(recipe.getInputB().getCount() * craftCount);
        
        ItemStack result = recipe.getResultItem(RegistryAccess.EMPTY).copy();
        result.setCount(result.getCount() * craftCount);
        
        if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
        
        return true;
    }

    private int countItemInInventory(Player player, ItemStack stack) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(invStack, stack)) {
                count += invStack.getCount();
            }
        }
        return count;
    }

    private void removeItemFromInventory(Player player, ItemStack stack, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(invStack, stack)) {
                int toRemove = Math.min(remaining, invStack.getCount());
                invStack.shrink(toRemove);
                remaining -= toRemove;
            }
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index < 36) {
            // Из инвентаря в наковальню
            if (!this.moveItemStackTo(sourceStack, 36, 38, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < 39) {
            // Из наковальни в инвентарь
            if (!this.moveItemStackTo(sourceStack, 0, 36, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(player, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.ANVIL_BLOCK.get());
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
