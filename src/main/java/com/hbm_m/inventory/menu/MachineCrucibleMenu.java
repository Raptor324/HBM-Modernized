package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineCrucibleBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Порт {@code ContainerCrucible} (1.7.10): сетка 3×3 плавильного буфера на
 * (107,18) с шагом 18, инвентарь игрока (8,132)/хотбар (8,190). Drag-split
 * отключён (mode 2 игнорируется, чтобы не раскидывать предметы по слотам
 * с лимитом 1). Шкалы прогресса/тепла — через ContainerData; списки расплава
 * GUI читает напрямую из BlockEntity (full-NBT sync).
 */
public class MachineCrucibleMenu extends AbstractContainerMenu {

    /** Слоты плавильного буфера 0..8 (в оригинале TE-слоты 1..9). */
    public static final int MACHINE_SLOTS = MachineCrucibleBlockEntity.SMELT_SLOTS;
    private static final int PLAYER_INV_START = MACHINE_SLOTS;
    private static final int PLAYER_INV_END   = PLAYER_INV_START + 27;
    private static final int HOTBAR_END       = PLAYER_INV_END + 9;

    public static final int IDX_PROGRESS       = 0;
    public static final int IDX_PROCESS_TIME   = 1;
    public static final int IDX_HEAT           = 2;
    public static final int IDX_MAX_HEAT       = 3;
    public static final int IDX_RECIPE_AMOUNT  = 4;
    public static final int IDX_RECIPE_CAP     = 5;
    public static final int IDX_WASTE_AMOUNT   = 6;
    public static final int IDX_WASTE_CAP      = 7;
    private static final int DATA_SLOTS = 8;

    public final MachineCrucibleBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public MachineCrucibleMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(DATA_SLOTS));
    }

    public MachineCrucibleMenu(int containerId, Inventory playerInventory, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.CRUCIBLE_MENU.get(), containerId);

        blockEntity = (entity instanceof MachineCrucibleBlockEntity be) ? be : null;
        this.access = (blockEntity != null)
                ? ContainerLevelAccess.create(playerInventory.player.level(), entity.getBlockPos())
                : ContainerLevelAccess.NULL;
        this.data = data;
        checkContainerDataCount(data, DATA_SLOTS);
        addDataSlots(data);

        if (blockEntity != null) {
            this.machineInventory = new ModItemStackHandlerContainer(blockEntity.getModItemStackHandler(), blockEntity::setChanged);
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    // Оригинал ContainerCrucible: слоты плавильного буфера принимают только 1 предмет.
                    this.addSlot(new Slot(machineInventory, col + row * 3,
                            107 + col * 18, 18 + row * 18) {
                        @Override
                        public int getMaxStackSize() {
                            return 1;
                        }
                    });
                }
            }
        } else {
            this.machineInventory = null;
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        8 + col * 18, 132 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 190));
        }
    }

    private final @org.jetbrains.annotations.Nullable ModItemStackHandlerContainer machineInventory;

    /** Порт slotClick(mode 2) — drag-split запрещён. */
    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (clickType == ClickType.SWAP) return;
        super.clicked(slotId, dragType, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        copy = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, copy);
        } else {
            if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null
                && blockEntity.getLevel() == player.level()
                && player.distanceToSqr(
                        blockEntity.getBlockPos().getX() + 0.5D,
                        blockEntity.getBlockPos().getY() + 0.5D,
                        blockEntity.getBlockPos().getZ() + 0.5D) <= 64.0D
                || stillValid(access, player, com.hbm_m.block.ModBlocks.CRUCIBLE.get());
    }

    // ── Данные шкал ──────────────────────────────────────────────────────

    public int getProgress()    { return data.get(IDX_PROGRESS); }
    public int getProcessTime() { return data.get(IDX_PROCESS_TIME); }
    public int getHeat()        { return data.get(IDX_HEAT); }
    public int getMaxHeat()     { return data.get(IDX_MAX_HEAT); }
    public int getRecipeAmount() { return data.get(IDX_RECIPE_AMOUNT); }
    public int getRecipeCap()    { return data.get(IDX_RECIPE_CAP); }
    public int getWasteAmount()  { return data.get(IDX_WASTE_AMOUNT); }
    public int getWasteCap()     { return data.get(IDX_WASTE_CAP); }

    public int getScaledProgress() {
        int max = getProcessTime();
        return max == 0 ? 0 : getProgress() * 33 / max;
    }

    public int getScaledHeat() {
        int max = getMaxHeat();
        return max == 0 ? 0 : getHeat() * 33 / max;
    }
}
