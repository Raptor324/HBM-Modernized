package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.LaunchpadLambdaBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.ISatChip;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code ContainerLaunchpadLambda}: Rakete (35,17), Satellit (53,17), Benzin rein/raus (107,80/98), Peroxid
 * rein/raus (125,80/98), Batterie (89,80), Spielerinventar ab y 144.
 */
public class LaunchpadLambdaMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = LaunchpadLambdaBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INV_END = MACHINE_SLOT_COUNT + 36;

    private final LaunchpadLambdaBlockEntity blockEntity;

    public LaunchpadLambdaMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public LaunchpadLambdaMenu(int id, Inventory inventory, LaunchpadLambdaBlockEntity blockEntity) {
        super(ModMenuTypes.LAUNCHPAD_LAMBDA_MENU.get(), id);
        this.blockEntity = blockEntity;

        var handler = blockEntity.getInventory();
        var container = new ModItemStackHandlerContainer(handler, blockEntity::setChanged);

        int[][] pos = { { 35, 17 }, { 53, 17 }, { 107, 80 }, { 107, 98 }, { 125, 80 }, { 125, 98 }, { 89, 80 } };
        for (int i = 0; i < pos.length; i++) {
            final int s = i;
            this.addSlot(new Slot(container, s, pos[i][0], pos[i][1]) {
                @Override public boolean mayPlace(ItemStack stack) { return handler.isItemValid(s, stack); }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 144 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 144 + 58));
        }
    }

    private static LaunchpadLambdaBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof LaunchpadLambdaBlockEntity pad) {
            return pad;
        }
        throw new IllegalStateException("No LaunchpadLambdaBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":launchpad_lambda_menu");
    }

    public LaunchpadLambdaBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        return MultiblockMenuReach.stillValidCore(blockEntity, player, 24.0D * 24.0D);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index < MACHINE_SLOT_COUNT) {
                if (!this.moveItemStackTo(slotStack, PLAYER_INV_START, PLAYER_INV_END, true)) return ItemStack.EMPTY;
            } else {
                boolean battery = com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(slotStack).isPresent();
                if (slotStack.is(ModItems.MISSILE_LAMBDA.get())) {
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) return ItemStack.EMPTY;
                } else if (slotStack.getItem() instanceof ISatChip) {
                    if (!this.moveItemStackTo(slotStack, 1, 2, false)) return ItemStack.EMPTY;
                } else if (battery) {
                    if (!this.moveItemStackTo(slotStack, 6, 7, false)) return ItemStack.EMPTY;
                } else {
                    if (!this.moveItemStackTo(slotStack, 2, 3, false) && !this.moveItemStackTo(slotStack, 4, 5, false)) return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();

            if (slotStack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, slotStack);
        }
        return result;
    }
}
