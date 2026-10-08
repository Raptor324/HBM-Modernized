package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineMixerBlockEntity;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.industrial.ItemMachineUpgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ContainerMixer}: Batterie (12,72), fester Stoff (52,72), Fluid-ID (126,72), Upgrades (148,18/36). */
public class MachineMixerMenu extends AbstractContainerMenu {

    private final MachineMixerBlockEntity mixer;
    private final ContainerData data;

    public MachineMixerMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, (MachineMixerBlockEntity) inventory.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(5));
    }

    public static MachineMixerMenu create(int id, Inventory inventory, MachineMixerBlockEntity blockEntity) {
        return new MachineMixerMenu(id, inventory, blockEntity, blockEntity.getContainerData());
    }

    public MachineMixerMenu(int id, Inventory player, MachineMixerBlockEntity mixer, ContainerData data) {
        super(ModMenuTypes.MIXER_MENU.get(), id);
        this.mixer = mixer;
        this.data = data;

        var c = new ModItemStackHandlerContainer(mixer.getInventory(), mixer::setChanged);
        // Battery
        this.addSlot(new Slot(c, 0, 12, 72));
        // Item Input
        this.addSlot(new Slot(c, 1, 52, 72));
        // Fluid ID
        this.addSlot(new Slot(c, 2, 126, 72));
        // Upgrades
        this.addSlot(new Slot(c, 3, 148, 18));
        this.addSlot(new Slot(c, 4, 148, 36));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(player, j + i * 9 + 9, 8 + j * 18, 122 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(player, i, 8 + i * 18, 180));
        }

        addDataSlots(data);
    }

    public MachineMixerBlockEntity getBlockEntity() { return mixer; }

    public int getProgress() { return data.get(0); }
    public int getProcessTime() { return data.get(1); }
    public long getPower() { return data.get(2); }
    public int getRecipeIndex() { return data.get(3); }
    public boolean wasOn() { return data.get(4) != 0; }

    @Override
    public ItemStack quickMoveStack(Player p, int par2) {
        ItemStack var3 = ItemStack.EMPTY;
        Slot var4 = this.slots.get(par2);

        if (var4 != null && var4.hasItem()) {
            ItemStack var5 = var4.getItem();
            var3 = var5.copy();

            if (par2 <= 4) {
                if (!this.moveItemStackTo(var5, 5, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {

                if (com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(var3).isPresent()
                        || com.hbm_m.api.energy.ItemEnergyAccess.getHbmReceiver(var3).isPresent()) {
                    if (!this.moveItemStackTo(var5, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (var3.getItem() instanceof IItemFluidIdentifier) {
                    if (!this.moveItemStackTo(var5, 2, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (var3.getItem() instanceof ItemMachineUpgrade) {
                    if (!this.moveItemStackTo(var5, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(var5, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (var5.isEmpty()) {
                var4.set(ItemStack.EMPTY);
            } else {
                var4.setChanged();
            }
        }

        return var3;
    }

    @Override
    public boolean stillValid(Player player) {
        // w16b: Original isUseableByPlayer (TileEntityMachineBase) = 128 vom Kern, dazu Huelle der Maschine (MultiblockMenuReach)
        return MultiblockMenuReach.stillValidCore(mixer, player, 128.0D);
    }
}
