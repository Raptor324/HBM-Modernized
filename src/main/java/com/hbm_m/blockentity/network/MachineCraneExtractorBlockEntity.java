package com.hbm_m.blockentity.network;

import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.block.network.IConveyorBelt;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.filter.ModulePatternMatcher;
import com.hbm_m.inventory.menu.MachineCraneExtractorMenu;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.items.IItemHandler;
//?}

/**
 * 1:1 {@code TileEntityCraneExtractor}: 9 Filter, 9 Puffer, Stapel- und Auswurf-Upgrade. Zieht aus dem Inventar am Ausgang
 * (Filter nach Weiss-/Schwarzliste) und legt auf das Band am Eingang; ohne Band landet es im Puffer. Hat es nichts gezogen,
 * leert es den Puffer auf das Band (ohne Filter).
 */
public class MachineCraneExtractorBlockEntity extends CraneBaseBlockEntity implements IControlReceiver, CraneBaseBlockEntity.ControlReceiverFilter {

    public static final int FILTER_START = 0;
    public static final int FILTER_END = 8;
    public static final int BUFFER_START = 9;
    public static final int BUFFER_END = 17;
    public static final int SLOT_UPGRADE_STACK = 18;
    public static final int SLOT_UPGRADE_EJECTOR = 19;
    public static final int INVENTORY_SIZE = 20;

    public boolean isWhitelist = false;
    public boolean maxEject = false;
    public final ModulePatternMatcher matcher = new ModulePatternMatcher(9);

    public MachineCraneExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_EXTRACTOR_BE.get(), pos, state, INVENTORY_SIZE);
    }

    /** Stufe eines Upgrades des gegebenen Typs, 0 = keins. */
    public static int upgradeTier(ItemStack stack, ItemMachineUpgrade.UpgradeType type) {
        return stack.getItem() instanceof ItemMachineUpgrade up && up.getUpgradeType() == type ? up.getTier() : 0;
    }

    public static int delayFor(ItemStack ejector) {
        return switch (upgradeTier(ejector, ItemMachineUpgrade.UpgradeType.EJECTOR)) {
            case 1 -> 10;
            case 2 -> 5;
            case 3 -> 2;
            default -> 20;
        };
    }

    public static int amountFor(ItemStack stackUpgrade) {
        return switch (upgradeTier(stackUpgrade, ItemMachineUpgrade.UpgradeType.STACK)) {
            case 1 -> 4;
            case 2 -> 16;
            case 3 -> 64;
            default -> 1;
        };
    }

    @Override
    protected ModItemStackHandler createInventoryHandler(int size) {
        return new ModItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) { setChanged(); }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) { return isItemValidForSlot(slot, stack); }

            /** Original {@code setInventorySlotContents}: Einstecken eines Upgrades spielt den Steckerklang. */
            @Override
            public void setStackInSlot(int slot, ItemStack stack) {
                super.setStackInSlot(slot, stack);
                if (!stack.isEmpty() && level != null && ((upgradeTier(stack, ItemMachineUpgrade.UpgradeType.EJECTOR) > 0 && slot == SLOT_UPGRADE_EJECTOR)
                        || (upgradeTier(stack, ItemMachineUpgrade.UpgradeType.STACK) > 0 && slot == SLOT_UPGRADE_STACK))) {
                    level.playSound(null, worldPosition, com.hbm_m.sound.ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCraneExtractorBlockEntity be) {
        be.serverTick(level, pos);
    }

    private void serverTick(Level level, BlockPos pos) {
        int delay = delayFor(inventory.getStackInSlot(SLOT_UPGRADE_EJECTOR));

        if (level.getGameTime() % delay == 0 && !level.hasNeighborSignal(pos)) {
            int amount = amountFor(inventory.getStackInSlot(SLOT_UPGRADE_STACK));

            Direction inputSide = getOutputSide(); // Achtung, vertauscht!
            Direction outputSide = getInputSide();
            IConveyorBelt belt = CraneInventoryUtil.beltAt(level, pos.relative(outputSide));

            boolean hasSent = false;

            //? if forge {
            IItemHandler inv = CraneInventoryUtil.inventoryAt(level, pos.relative(inputSide), inputSide.getOpposite());

            /* aus einem angeschlossenen Inventar senden */
            if (inv != null) {
                for (int index = 0; index < inv.getSlots(); index++) {
                    ItemStack stack = inv.getStackInSlot(index);

                    if (!stack.isEmpty() && !inv.extractItem(index, 1, true).isEmpty()) {

                        int maxTarget = Math.min(amount, stack.getMaxStackSize());
                        if (this.maxEject && stack.getCount() < maxTarget) continue;
                        boolean match = this.matchesFilter(stack);

                        if ((isWhitelist && match) || (!isWhitelist && !match)) {
                            stack = stack.copy();
                            int toSend = Math.min(amount, stack.getCount());

                            if (belt != null) {
                                ItemStack taken = inv.extractItem(index, toSend, false);
                                CraneInventoryUtil.sendItemAndEnter(level, pos, outputSide, belt, taken);
                            } else {
                                stack.setCount(toSend);
                                ItemStack remaining = tryAddToBuffer(stack);
                                inv.extractItem(index, toSend - remaining.getCount(), false);
                            }
                            hasSent = true;
                            break;
                        }
                    }
                }
            }
            //?}

            /* hat nichts gesendet: Puffer ohne Filter auf das Band */
            if (!hasSent && belt != null) {
                for (int i = BUFFER_START; i <= BUFFER_END; i++) {
                    ItemStack stack = inventory.getStackInSlot(i);

                    if (!stack.isEmpty()) {
                        stack = stack.copy();
                        int toSend = Math.min(amount, stack.getCount());

                        int maxTarget = Math.min(amount, stack.getMaxStackSize());
                        if (this.maxEject && stack.getCount() < maxTarget) continue;

                        inventory.extractItem(i, toSend, false);
                        stack.setCount(toSend);
                        CraneInventoryUtil.sendItemAndEnter(level, pos, outputSide, belt, stack);
                        break;
                    }
                }
            }
        }

        sendUpdateToClient();
    }

    /** Original: {@code InventoryUtil.tryAddItemToInventory(slots, 9, 17, stack)}. */
    private ItemStack tryAddToBuffer(ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int i = BUFFER_START; i <= BUFFER_END && !rest.isEmpty(); i++) {
            ItemStack cur = inventory.getStackInSlot(i);
            if (!cur.isEmpty() && ItemStack.isSameItemSameTags(cur, rest) && cur.getCount() < cur.getMaxStackSize()) {
                int move = Math.min(cur.getMaxStackSize() - cur.getCount(), rest.getCount());
                cur.grow(move);
                rest.shrink(move);
            }
        }
        for (int i = BUFFER_START; i <= BUFFER_END && !rest.isEmpty(); i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                inventory.setStackInSlot(i, rest.copy());
                rest.setCount(0);
            }
        }
        setChanged();
        return rest;
    }

    public boolean matchesFilter(ItemStack stack) {
        for (int i = 0; i < 9; i++) {
            ItemStack filter = inventory.getStackInSlot(i);
            if (!filter.isEmpty() && this.matcher.isValidForFilter(filter, i, stack)) return true;
        }
        return false;
    }

    @Override
    public void nextMode(int i) {
        this.matcher.nextMode(i, inventory.getStackInSlot(i));
        setChanged();
        sendUpdateToClient();
    }

    @Override public int[] getFilterSlots() { return new int[] { 0, 9 }; }

    public ModulePatternMatcher getMatcher() { return matcher; }
    public boolean isWhitelist() { return isWhitelist; }
    public boolean isMaxEject() { return maxEject; }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("whitelist")) this.isWhitelist = !this.isWhitelist;
        if (data.contains("maxEject")) this.maxEject = !this.maxEject;
        if (data.contains("slot")) setFilterContents(data);
        setChanged();
        sendUpdateToClient();
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("isWhitelist", isWhitelist);
        tag.putBoolean("maxEject", maxEject);
        matcher.writeToNBT(tag);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        isWhitelist = tag.getBoolean("isWhitelist");
        maxEject = tag.getBoolean("maxEject");
        matcher.readFromNBT(tag);
    }

    /** Original: {@code isItemValidForSlot} - nur Puffer (9-17) fuer Automatisierung. */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot > 8 && slot < 18;
    }

    @Override protected boolean canExtractItem(int slot, ItemStack stack) { return slot > 8 && slot < 18; }

    @Override protected int[] getAccessibleSlots() { return new int[] { 9, 10, 11, 12, 13, 14, 15, 16, 17 }; }

    @Override protected Component getDefaultName() { return Component.translatable("container.craneExtractor"); }
    @Override public Component getDisplayName() { return getDefaultName(); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineCraneExtractorMenu.create(id, inventory, this);
    }
}
