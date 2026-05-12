package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.machines.MachineCrucibleBlockEntity;

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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

public class MachineCrucibleMenu extends AbstractContainerMenu {

    // Slot index boundaries
    /** Machine input slots 0..8 (3×3 grid). */
    public static final int MACHINE_SLOTS = 9;
    /** Player inventory slots 9..35 (3 rows). */
    private static final int PLAYER_INV_START = MACHINE_SLOTS;
    private static final int PLAYER_INV_END   = PLAYER_INV_START + 27;  // exclusive
    /** Hotbar slots 36..44. */
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END   = HOTBAR_START + 9;            // exclusive

    // data[0]=progress, data[1]=processTime, data[2]=heat, data[3]=maxHeat,
    // data[4]=liquidStored, data[5]=liquidCapacity
    private static final int DATA_SLOTS = 6;
    public static final int IDX_PROGRESS      = 0;
    public static final int IDX_PROCESS_TIME  = 1;
    public static final int IDX_HEAT          = 2;
    public static final int IDX_MAX_HEAT      = 3;
    public static final int IDX_LIQUID        = 4;
    public static final int IDX_LIQUID_CAP    = 5;

    public final MachineCrucibleBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    /** Client-side constructor (reads BlockEntity from level via buf position). */
    public MachineCrucibleMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(DATA_SLOTS));
    }

    /** Server-side constructor — BlockEntity must be a MachineCrucibleBlockEntity. */
    public MachineCrucibleMenu(int containerId, Inventory playerInventory, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.CRUCIBLE_MENU.get(), containerId);

        blockEntity = (entity instanceof MachineCrucibleBlockEntity be) ? be : null;
        this.access = (blockEntity != null)
                ? ContainerLevelAccess.create(playerInventory.player.level(), entity.getBlockPos())
                : ContainerLevelAccess.NULL;
        this.data = data;
        checkContainerDataCount(data, DATA_SLOTS);
        addDataSlots(data);

        // --- Machine input slots (3×3 grid, legacy: 107+j*18 / 18+i*18) ---
        if (blockEntity != null) {
            blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 3; col++) {
                        this.addSlot(new SlotItemHandler(handler, col + row * 3,
                                107 + col * 18, 18 + row * 18));
                    }
                }
            });
        }

        // --- Player inventory (3 rows, legacy: y=132) ---
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        8 + col * 18, 132 + row * 18));
            }
        }
        // --- Hotbar (legacy: y=190) ---
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 190));
        }
    }

    // -------------------------------------------------------------------------
    // Click override — block number-key shortcuts (legacy: mode 2 → null)
    // -------------------------------------------------------------------------

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (clickType == ClickType.SWAP) return; // blocks number-key (1–9) hotswap
        super.clicked(slotId, dragType, clickType, player);
    }

    // -------------------------------------------------------------------------
    // Shift-click
    // -------------------------------------------------------------------------

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        copy = stack.copy();

        if (index < MACHINE_SLOTS) {
            // Machine → player
            if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, copy);
        } else {
            // Player / hotbar → machine (slots 0..MACHINE_SLOTS-1)
            if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return copy;
    }

    // -------------------------------------------------------------------------
    // Data accessors (read by GUIMachineCrucible on the client side)
    // -------------------------------------------------------------------------

    public int getProgress()     { return data.get(IDX_PROGRESS); }
    public int getProcessTime()  { return data.get(IDX_PROCESS_TIME); }
    public int getHeat()         { return data.get(IDX_HEAT); }
    public int getMaxHeat()      { return data.get(IDX_MAX_HEAT); }
    public int getLiquidStored() { return data.get(IDX_LIQUID); }
    public int getLiquidCap()    { return data.get(IDX_LIQUID_CAP); }

    /** Scaled progress gauge width (px, max 33). */
    public int getScaledProgress() {
        int pt = getProcessTime();
        return pt > 0 ? getProgress() * 33 / pt : 0;
    }

    /** Scaled heat gauge width (px, max 33). */
    public int getScaledHeat() {
        int mh = getMaxHeat();
        return mh > 0 ? getHeat() * 33 / mh : 0;
    }

    /** Scaled liquid gauge height (px, max 101). */
    public int getScaledLiquidHeight() {
        int cap = getLiquidCap();
        return cap > 0 ? getLiquidStored() * 101 / cap : 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.CRUCIBLE.get());
    }
}
