package com.hbm_m.blockentity.network;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.network.CraneBaseBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.interfaces.ICopiable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityCraneBase}: fuehrt den per Schraubendreher gesetzten Ausgang ({@code outputOverride}) und haelt den
 * Blockzustand ({@link CraneBaseBlock#FACING} = Eingang, {@link CraneBaseBlock#OUTPUT}) damit synchron. Kopierwerkzeug:
 * Eintrag 0 = Filter (falls {@link ControlReceiverFilter}), Eintrag 1 = Ausrichtung.
 */
public abstract class CraneBaseBlockEntity extends BaseMachineBlockEntity implements ICopiable {

    /** Original: Erweiterung des Metadatensystems; {@code null} = {@code ForgeDirection.UNKNOWN}. */
    @Nullable private Direction outputOverride = null;

    protected CraneBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots, 0L, 0L, 0L);
    }

    public Direction getInputSide() {
        BlockState s = getBlockState();
        return s.hasProperty(CraneBaseBlock.FACING) ? s.getValue(CraneBaseBlock.FACING) : Direction.NORTH;
    }

    public Direction getOutputSide() {
        Direction override = getOutputOverride();
        return override != null ? override : getInputSide().getOpposite();
    }

    @Nullable
    public Direction getOutputOverride() {
        return outputOverride;
    }

    public void setOutputOverride(Direction direction) {
        Direction oldSide = getOutputSide();
        if (oldSide == direction) direction = direction.getOpposite();

        outputOverride = direction;

        if (direction == getInputSide())
            setInput(oldSide);
        else
            onBlockChanged();
    }

    public void setInput(Direction direction) {
        outputOverride = getOutputSide(); // aktuellen Ausgang festhalten, falls noch nicht gespeichert

        Direction oldSide = getInputSide();
        if (oldSide == direction) direction = direction.getOpposite();

        boolean needSwapOutput = direction == getOutputSide();
        if (level != null) level.setBlock(worldPosition, getBlockState().setValue(CraneBaseBlock.FACING, direction), needSwapOutput ? 2 | 16 : 3);

        if (needSwapOutput)
            setOutputOverride(oldSide);
        else
            onBlockChanged();
    }

    protected void onBlockChanged() {
        if (level == null) return;
        BlockState s = getBlockState();
        Direction out = getOutputSide();
        if (s.hasProperty(CraneBaseBlock.OUTPUT) && s.getValue(CraneBaseBlock.OUTPUT) != out) {
            level.setBlock(worldPosition, s.setValue(CraneBaseBlock.OUTPUT, out), 3);
        } else {
            level.sendBlockUpdated(worldPosition, s, s, 3);
            level.updateNeighborsAt(worldPosition, s.getBlock());
        }
        setChanged();
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        if (nbt.contains("CraneOutputOverride", 1)) {
            int o = nbt.getByte("CraneOutputOverride");
            outputOverride = o >= 0 && o < 6 ? Direction.from3DDataValue(o) : null;
        }
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putByte("CraneOutputOverride", (byte) (outputOverride == null ? 6 : outputOverride.get3DDataValue()));
    }

    /** Original: {@code getAccessibleSlotsFromSide} - die fuer Automatisierung sichtbaren Plaetze. */
    protected int[] getAccessibleSlots() {
        int[] all = new int[inventory.getSlots()];
        for (int i = 0; i < all.length; i++) all[i] = i;
        return all;
    }

    /** Original: {@code canExtractItem} - Standard von {@code TileEntityMachineBase} ist nein. */
    protected boolean canExtractItem(int slot, ItemStack stack) { return false; }

    //? if forge {
    /** {@code ISidedInventory}-Sicht: nur zugaengliche Plaetze, Entnahme nach {@link #canExtractItem}. */
    private class AutomationView extends CraneInventoryUtil.SlotView {
        private final int[] access;
        AutomationView() { this(getAccessibleSlots()); }
        private AutomationView(int[] access) { super(inventory, access); this.access = access; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!canExtractItem(access[slot], inventory.getStackInSlot(access[slot]))) return ItemStack.EMPTY;
            return super.extractItem(slot, amount, simulate);
        }
    }

    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> automation = net.minecraftforge.common.util.LazyOptional.empty();

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
            if (!automation.isPresent()) automation = net.minecraftforge.common.util.LazyOptional.of(() -> new AutomationView());
            return automation.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public @Nullable Object getItemHandler(@Nullable Direction side) {
        return new AutomationView();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        automation.invalidate();
    }
    //?}

    /** Original: Zugriff aus 20 Bloecken Entfernung. */
    public boolean hasPermission(Player player) {
        return Math.sqrt(player.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())) < 20;
    }

    // ── Kopierwerkzeug ──────────────────────────────────────────────────────

    @Override
    public CompoundTag getSettings(Level world, BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("inputSide", getInputSide().get3DDataValue());
        nbt.putInt("outputSide", getOutputSide().get3DDataValue());

        if (this instanceof ControlReceiverFilter filter) {
            ListTag tags = new ListTag();
            int count = 0;
            for (int i = filter.getFilterSlots()[0]; i < filter.getFilterSlots()[1]; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    CompoundTag slotNBT = new CompoundTag();
                    slotNBT.putByte("slot", (byte) count);
                    stack.save(slotNBT);
                    tags.add(slotNBT);
                }
                count++;
            }
            nbt.put("items", tags);
        }
        return nbt;
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level world, Player player, BlockPos pos) {
        if (index == 1) {
            if (nbt.contains("outputSide")) {
                outputOverride = Direction.from3DDataValue(nbt.getInt("outputSide"));
                onBlockChanged();
            }
            if (nbt.contains("inputSide")) {
                world.setBlock(worldPosition, getBlockState().setValue(CraneBaseBlock.FACING, Direction.from3DDataValue(nbt.getInt("inputSide"))), 3);
                onBlockChanged();
            }
        } else if (this instanceof ControlReceiverFilter filter) {
            ListTag items = nbt.getList("items", 10);
            int listSize = items.size();
            if (listSize > 0) {
                int count = 0;
                for (int i = filter.getFilterSlots()[0]; i < filter.getFilterSlots()[1]; i++) {
                    if (i < listSize) {
                        CompoundTag slotNBT = items.getCompound(count);
                        byte slot = slotNBT.getByte("slot");
                        ItemStack loaded = ItemStack.of(slotNBT);
                        // Original: "router"-Pruefung ist durch "index * + 5" immer falsch
                        boolean router = nbt.contains("modes") && slot > index * 5 && slot < index * +5;
                        if (!loaded.isEmpty() && (slot < filter.getFilterSlots()[1] || router)) {
                            inventory.setStackInSlot(slot + filter.getFilterSlots()[0], loaded);
                            filter.nextMode(slot);
                            setChanged();
                        }
                    }
                    count++;
                }
            }
        }
    }

    @Override
    public String[] infoForDisplay(Level world, BlockPos pos) {
        return new String[] { "copytool.filter", "copytool.orientation" };
    }

    /** 1:1 {@code IControlReceiverFilter}: Filterplaetze per Geisterklick belegen und Modus weiterschalten. */
    public interface ControlReceiverFilter {
        void nextMode(int i);

        /** Anfang (inklusive) und Ende (exklusive) der Filterplaetze. */
        int[] getFilterSlots();

        default void setFilterContents(CompoundTag nbt) {
            BaseMachineBlockEntity tile = (BaseMachineBlockEntity) this;
            int slot = nbt.getInt("slot");
            ItemStack item = ItemStack.of(nbt.getCompound("stack"));
            item.setCount(1);
            tile.getInventory().setStackInSlot(slot, item);
            nextMode(slot);
            tile.setChanged();
        }
    }
}
