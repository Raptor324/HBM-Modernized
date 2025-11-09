package com.hbm_m.menu;

import com.hbm_m.block.entity.MachineWoodBurnerBlockEntity;
import com.hbm_m.block.ModBlocks; // ЗАМЕНИ НА СВОЙ КЛАСС
import com.hbm_m.menu.ModMenuTypes; // ЗАМЕНИ НА СВОЙ КЛАСС
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

public class MachineWoodBurnerMenu extends AbstractContainerMenu {
    public final MachineWoodBurnerBlockEntity blockEntity;
    private final ContainerData data;

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
        });

        addPlayerInventory(inv);
        addPlayerHotbar(inv);
        addDataSlots(data);
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

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        // ... Твоя логика shift-клика, если нужна ...
        return ItemStack.EMPTY;
    }

    private void addPlayerInventory(Inventory i) { for(int y=0; y<3; ++y) for(int x=0; x<9; ++x) this.addSlot(new Slot(i, x+y*9+9, 8+x*18, 104+y*18)); }
    private void addPlayerHotbar(Inventory i) { for(int x=0; x<9; ++x) this.addSlot(new Slot(i, x, 8+x*18, 162)); }
}