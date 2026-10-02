package com.hbm_m.blockentity.network;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.ICopiable;
import com.hbm_m.inventory.filter.ModulePatternMatcher;
import com.hbm_m.inventory.menu.MachineCraneRouterMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityCraneRouter}: je Seite 5 Filter mit eigenem Matcher und ein Modus (aus, Weissliste, Schwarzliste,
 * Wildcard). {@link #sort} verteilt Stapel auf die Richtungen; Index 6 = nicht zuzuordnen.
 */
public class MachineCraneRouterBlockEntity extends BaseMachineBlockEntity
        implements IControlReceiver, ICopiable, CraneBaseBlockEntity.ControlReceiverFilter {

    public static final int SLOTS_PER_SIDE = 5;
    public static final int SIDE_COUNT = 6;
    public static final int INVENTORY_SIZE = SLOTS_PER_SIDE * SIDE_COUNT;

    public static final int MODE_NONE = 0;
    public static final int MODE_WHITELIST = 1;
    public static final int MODE_BLACKLIST = 2;
    public static final int MODE_WILDCARD = 3;

    public ModulePatternMatcher[] patterns = new ModulePatternMatcher[6]; // Original: "why did i make six matchers???"
    public int[] modes = new int[6];

    public MachineCraneRouterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_ROUTER_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
        for (int i = 0; i < patterns.length; i++) patterns[i] = new ModulePatternMatcher(5);
    }

    /** Verteilt Kopien der Stapel auf die Richtungen (Index = 3D-Datenwert, 6 = unbekannt). */
    @SuppressWarnings("unchecked")
    public List<ItemStack>[] sort(ItemStack... stacks) {
        List<ItemStack>[] output = new List[7];
        for (int i = 0; i < 7; i++) output[i] = new ArrayList<>();

        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            Direction dir = getOutputDir(stack.copy());
            output[dir == null ? 6 : dir.get3DDataValue()].add(stack);
        }
        return output;
    }

    public Direction getOutputDir(ItemStack stack) {
        List<Direction> validDirs = new ArrayList<>();

        // Filter aller Seiten pruefen
        for (int side = 0; side < 6; side++) {
            ModulePatternMatcher matcher = patterns[side];
            int mode = modes[side];

            // abgeschaltete und Wildcard-Seiten ueberspringen
            if (mode == MODE_NONE || mode == MODE_WILDCARD) continue;

            boolean matchesFilter = false;

            for (int slot = 0; slot < 5; slot++) {
                ItemStack filter = inventory.getStackInSlot(side * 5 + slot);
                if (filter.isEmpty()) continue;

                // ein Treffer genuegt
                if (matcher.isValidForFilter(filter, slot, stack)) {
                    matchesFilter = true;
                    break;
                }
            }

            if ((mode == MODE_WHITELIST && matchesFilter) || (mode == MODE_BLACKLIST && !matchesFilter)) {
                validDirs.add(Direction.from3DDataValue(side));
            }
        }

        // noch keine Richtung: Wildcard-Seiten
        if (validDirs.isEmpty()) {
            for (int side = 0; side < 6; side++) {
                if (modes[side] == MODE_WILDCARD) validDirs.add(Direction.from3DDataValue(side));
            }
        }

        if (validDirs.isEmpty()) return null;
        return validDirs.get(level.random.nextInt(validDirs.size()));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCraneRouterBlockEntity be) {
        if (!level.isClientSide) be.sendUpdateToClient();
    }

    public ModulePatternMatcher getMatcher(int side) { return patterns[side]; }
    public int getMode(int side) { return modes[side]; }

    @Override
    public void nextMode(int index) {
        int matcher = index / 5;
        int mIndex = index % 5;
        this.patterns[matcher].nextMode(mIndex, inventory.getStackInSlot(index));
        setChanged();
        sendUpdateToClient();
    }

    /** Original {@code initPattern}: {@code initPatternSmart}. */
    public void initPattern(ItemStack stack, int index) {
        int matcher = index / 5;
        int mIndex = index % 5;
        this.patterns[matcher].initPatternSmart(mIndex, stack);
    }

    @Override public int[] getFilterSlots() { return new int[] { 0, INVENTORY_SIZE }; }

    public boolean hasPermission(Player player) {
        return Math.sqrt(player.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())) < 20;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toggle")) {
            int i = data.getInt("toggle");
            modes[i]++;
            if (modes[i] > 3) modes[i] = 0;
        }
        if (data.contains("slot")) setFilterContents(data);
        setChanged();
        sendUpdateToClient();
    }

    // ── Kopierwerkzeug ──────────────────────────────────────────────────────

    @Override
    public CompoundTag getSettings(Level world, BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        ListTag tags = new ListTag();
        int count = 0;
        for (int i = getFilterSlots()[0]; i < getFilterSlots()[1]; i++) {
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
        nbt.putIntArray("modes", modes);
        return nbt;
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level world, Player player, BlockPos pos) {
        ListTag items = nbt.getList("items", 10);
        int listSize = items.size();
        if (listSize > 0 && nbt.contains("modes")) {
            for (int i = 0; i < listSize; i++) {
                CompoundTag slotNBT = items.getCompound(i);
                byte slot = slotNBT.getByte("slot");
                ItemStack loaded = ItemStack.of(slotNBT);
                if (!loaded.isEmpty() && slot > index * 5 && slot < Math.min(index * 5 + 5, 30)) {
                    inventory.setStackInSlot(slot, loaded);
                    nextMode(slot);
                    setChanged();
                }
            }
            modes = nbt.getIntArray("modes");
        } else {
            // Original: IControlReceiverFilter.super.pasteSettings
            int count = 0;
            for (int i = getFilterSlots()[0]; i < getFilterSlots()[1]; i++) {
                if (i < listSize) {
                    CompoundTag slotNBT = items.getCompound(count);
                    byte slot = slotNBT.getByte("slot");
                    ItemStack loaded = ItemStack.of(slotNBT);
                    boolean router = nbt.contains("modes") && slot > index * 5 && slot < index * +5;
                    if (!loaded.isEmpty() && index < listSize && (slot < getFilterSlots()[1] || router)) {
                        inventory.setStackInSlot(slot + getFilterSlots()[0], loaded);
                        nextMode(slot);
                        setChanged();
                    }
                }
                count++;
            }
        }
    }

    @Override
    public String[] infoForDisplay(Level world, BlockPos pos) {
        String[] options = new String[patterns.length];
        for (int i = 0; i < options.length; i++) options[i] = "copytool.pattern" + i;
        return options;
    }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        for (int i = 0; i < patterns.length; i++) {
            CompoundTag compound = new CompoundTag();
            patterns[i].writeToNBT(compound);
            tag.put("pattern" + i, compound);
        }
        tag.putIntArray("modes", modes);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        for (int i = 0; i < patterns.length; i++) patterns[i].readFromNBT(tag.getCompound("pattern" + i));
        int[] loaded = tag.getIntArray("modes");
        this.modes = loaded.length == 6 ? loaded : new int[6];
    }

    /** Original: kein Automatisierungszugriff ({@code TileEntityMachineBase}). */
    @Override protected boolean isItemValidForSlot(int slot, ItemStack stack) { return false; }

    //? if forge {
    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, @org.jetbrains.annotations.Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) return net.minecraftforge.common.util.LazyOptional.empty();
        return super.getCapability(cap, side);
    }

    @Override
    public @org.jetbrains.annotations.Nullable Object getItemHandler(@org.jetbrains.annotations.Nullable Direction side) { return null; }
    //?}

    @Override protected Component getDefaultName() { return Component.translatable("container.craneRouter"); }
    @Override public Component getDisplayName() { return getDefaultName(); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineCraneRouterMenu.create(id, inventory, this);
    }
}
