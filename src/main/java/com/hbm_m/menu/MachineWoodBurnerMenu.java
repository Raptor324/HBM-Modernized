package com.hbm_m.menu;

import com.hbm_m.block.entity.machine.MachineWoodBurnerBlockEntity;
import com.hbm_m.block.ModBlocks; // ЗАМЕНИ НА СВОЙ КЛАСС
import com.hbm_m.menu.ModMenuTypes; // ЗАМЕНИ НА СВОЙ КЛАСС
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import static net.minecraft.world.inventory.AbstractFurnaceMenu.FUEL_SLOT;

public class MachineWoodBurnerMenu extends AbstractContainerMenu {
    public final MachineWoodBurnerBlockEntity blockEntity;
    private final ContainerData data;

    private static final int PLAYER_INV_START = 0;
    private static final int PLAYER_INV_END = 36; // 9 hotbar + 27 inventory
    private static final int FUEL_SLOT = 36;
    private static final int ASH_SLOT = 37;
    private static final int CHARGE_SLOT = 38;

    public MachineWoodBurnerMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(8));
    }

    public MachineWoodBurnerMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.WOOD_BURNER_MENU.get(), id);
        this.blockEntity = (MachineWoodBurnerBlockEntity) entity;
        this.data = data;

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(h -> {
            this.addSlot(new SlotItemHandler(h, 0, 26, 18) { // Fuel slot
                @Override public boolean mayPlace(ItemStack stack) { return ForgeHooks.getBurnTime(stack, null) > 0; }
            });
            this.addSlot(new SlotItemHandler(h, 1, 26, 54) { // Ash slot
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });

            this.addSlot(new SlotItemHandler(h, 2, 143, 54) { // Charge slot
                @Override public boolean mayPlace(ItemStack stack) {
                    // Разрешаем класть только то, что может принимать FE
                    return stack.getCapability(ForgeCapabilities.ENERGY).map(IEnergyStorage::canReceive).orElse(false);
                }
            });
        });

        addPlayerInventory(inv);
        addPlayerHotbar(inv);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            // === Перемещение ИЗ слотов машины В инвентарь ===
            if (pIndex == FUEL_SLOT || pIndex == ASH_SLOT || pIndex == CHARGE_SLOT) {
                if (!this.moveItemStackTo(slotStack, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(slotStack, itemstack);
            }
            // === Перемещение ИЗ инвентаря В слоты машины ===
            else if (pIndex >= PLAYER_INV_START && pIndex < PLAYER_INV_END) {

                // Пробуем в СЛОТ ТОПЛИВА
                if (ForgeHooks.getBurnTime(slotStack, null) > 0) {
                    if (!this.moveItemStackTo(slotStack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                        // (Если не вышло, продолжаем пробовать другие слоты)
                    } else {
                        // Успешно переместили топливо
                        if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
                        else slot.setChanged();
                        return itemstack;
                    }
                }

                // Пробуем в СЛОТ ЗАРЯДКИ
                if (slotStack.getCapability(ForgeCapabilities.ENERGY).map(IEnergyStorage::canReceive).orElse(false)) {
                    if (!this.moveItemStackTo(slotStack, CHARGE_SLOT, CHARGE_SLOT + 1, false)) {
                        // (Если не вышло, пробуем хотбар/инвентарь)
                    } else {
                        // Успешно переместили батарейку
                        if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
                        else slot.setChanged();
                        return itemstack;
                    }
                }

                // Стандартное перемещение (инвентарь <-> хотбар)
                if (pIndex < 27) { // Из инвентаря в хотбар
                    if (!this.moveItemStackTo(slotStack, 27, 36, false)) {
                        return ItemStack.EMPTY;
                    }
                } else { // Из хотбара в инвентарь
                    if (!this.moveItemStackTo(slotStack, 0, 27, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
            // === Конец ===

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(pPlayer, slotStack);
        }

        return itemstack;
    }

    public long getEnergyLong() { return ((long)this.data.get(1) << 32) | (this.data.get(0) & 0xFFFFFFFFL); }
    public long getMaxEnergyLong() { return ((long)this.data.get(3) << 32) | (this.data.get(2) & 0xFFFFFFFFL); }
    public int getBurnTime() { return this.data.get(4); }
    public int getMaxBurnTime() { return this.data.get(5); }
    public boolean isLit() { return this.data.get(6) != 0; }
    public boolean isEnabled() { return this.data.get(7) != 0; }

    public int getBurnTimeScaled(int scale) { int max = getMaxBurnTime(); return max > 0 ? getBurnTime() * scale / max : 0; }
    public int getEnergyScaled(int scale) { long max = getMaxEnergyLong(); return max > 0 ? (int)((double)getEnergyLong() / max * scale) : 0; }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), pPlayer, ModBlocks.WOOD_BURNER.get());
    }


    private void addPlayerInventory(Inventory i) { for(int y=0; y<3; ++y) for(int x=0; x<9; ++x) this.addSlot(new Slot(i, x+y*9+9, 8+x*18, 104+y*18)); }
    private void addPlayerHotbar(Inventory i) { for(int x=0; x<9; ++x) this.addSlot(new Slot(i, x, 8+x*18, 162)); }
}