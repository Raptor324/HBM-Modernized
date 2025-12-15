package com.hbm_m.menu;

import com.hbm_m.api.energy.ILongEnergyMenu;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.block.entity.custom.machines.MachineWoodBurnerBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

public class MachineWoodBurnerMenu extends AbstractContainerMenu implements ILongEnergyMenu {
    
    public final MachineWoodBurnerBlockEntity blockEntity;
    private final ContainerData data;
    private final Player player;

    // Клиентские поля для энергии
    private long clientEnergy;
    private long clientMaxEnergy;

    private static final int PLAYER_INV_START = 0;
    private static final int PLAYER_INV_END = 36;
    private static final int FUEL_SLOT = 36;
    private static final int ASH_SLOT = 37;
    private static final int CHARGE_SLOT = 38;

    // Клиентский конструктор
    public MachineWoodBurnerMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        // Ожидаем 4 int-значения (BurnTime, MaxBurnTime, IsBurning, Enabled)
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(4));
    }

    // Серверный конструктор
    public MachineWoodBurnerMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.WOOD_BURNER_MENU.get(), id);

        checkContainerDataCount(data, 4); // Проверяем размер данных

        this.blockEntity = (MachineWoodBurnerBlockEntity) entity;
        this.data = data;
        this.player = inv.player; // Сохраняем игрока

        // Используем inventory из базового класса через capability
        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(h -> {
            this.addSlot(new SlotItemHandler(h, 0, 26, 18) { // Fuel slot
                @Override public boolean mayPlace(ItemStack stack) { return ForgeHooks.getBurnTime(stack, null) > 0; }
            });
            this.addSlot(new SlotItemHandler(h, 1, 26, 54) { // Ash slot
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });

            this.addSlot(new SlotItemHandler(h, 2, 143, 54) { // Charge slot
                @Override public boolean mayPlace(ItemStack stack) {
                    return stack.getCapability(ForgeCapabilities.ENERGY).map(IEnergyStorage::canReceive).orElse(false) ||
                           stack.getCapability(com.hbm_m.capability.ModCapabilities.HBM_ENERGY_RECEIVER).isPresent();
                }
            });
        });

        addPlayerInventory(inv);
        addPlayerHotbar(inv);
        addDataSlots(data);
    }

    // --- Реализация ILongEnergyMenu ---

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

    // --- Геттеры для данных (индексы смещены) ---
    // Было 4, 5, 6, 7 -> Стало 0, 1, 2, 3

    public int getBurnTime() { return this.data.get(0); }
    public int getMaxBurnTime() { return this.data.get(1); }
    public boolean isLit() { return this.data.get(2) != 0; }
    public boolean isEnabled() { return this.data.get(3) != 0; }

    public int getBurnTimeScaled(int scale) { int max = getMaxBurnTime(); return max > 0 ? getBurnTime() * scale / max : 0; }
    public int getEnergyScaled(int scale) { long max = getMaxEnergyLong(); return max > 0 ? (int)((double)getEnergyLong() / max * scale) : 0; }

    // --- Синхронизация ---

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) {
            ModPacketHandler.INSTANCE.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> (net.minecraft.server.level.ServerPlayer) this.player),
                    new com.hbm_m.network.packet.PacketSyncEnergy(
                            this.containerId,
                            blockEntity.getEnergyStored(),
                            blockEntity.getMaxEnergyStored(),
                            blockEntity.getEnergyDelta()
                    )
            );
        }
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            if (pIndex == FUEL_SLOT || pIndex == ASH_SLOT || pIndex == CHARGE_SLOT) {
                if (!this.moveItemStackTo(slotStack, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(slotStack, itemstack);
            }
            else if (pIndex >= PLAYER_INV_START && pIndex < PLAYER_INV_END) {
                if (ForgeHooks.getBurnTime(slotStack, null) > 0) {
                    if (!this.moveItemStackTo(slotStack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                        // continue
                    } else {
                        if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
                        else slot.setChanged();
                        return itemstack;
                    }
                }
                if (slotStack.getCapability(ForgeCapabilities.ENERGY).map(IEnergyStorage::canReceive).orElse(false)) {
                    if (!this.moveItemStackTo(slotStack, CHARGE_SLOT, CHARGE_SLOT + 1, false)) {
                        // continue
                    } else {
                        if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
                        else slot.setChanged();
                        return itemstack;
                    }
                }
                if (pIndex < 27) {
                    if (!this.moveItemStackTo(slotStack, 27, 36, false)) return ItemStack.EMPTY;
                } else {
                    if (!this.moveItemStackTo(slotStack, 0, 27, false)) return ItemStack.EMPTY;
                }
            }

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

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), pPlayer, ModBlocks.WOOD_BURNER.get());
    }

    private void addPlayerInventory(Inventory i) { for(int y=0; y<3; ++y) for(int x=0; x<9; ++x) this.addSlot(new Slot(i, x+y*9+9, 8+x*18, 104+y*18)); }
    private void addPlayerHotbar(Inventory i) { for(int x=0; x<9; ++x) this.addSlot(new Slot(i, x, 8+x*18, 162)); }
}