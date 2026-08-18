package com.hbm_m.blockentity.network;

import java.util.EnumMap;
import java.util.Map;

import com.hbm_m.block.machines.MachineCraneExtractorBlock;
import com.hbm_m.block.network.IConveyorBelt;
import com.hbm_m.block.network.IEnterableBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.conveyor.MovingConveyorItemEntity;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.filter.ModulePatternMatcher;
import com.hbm_m.inventory.menu.MachineCraneExtractorMenu;
import com.hbm_m.item.industrial.ItemMachineUpgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
//?}

/**
 * Crane Extractor - Port von {@code TileEntityCraneExtractor} (1.7.10 Original). Zieht periodisch
 * Items aus dem Inventar auf der Eingabeseite, prueft sie gegen einen 9-Slot-{@link ModulePatternMatcher}
 * (Whitelist/Blacklist umschaltbar), und legt passende Items auf das Foerderband der Ausgabeseite -
 * oder in den 9-Slot-Puffer, falls dort kein Foerderband liegt. Stack-/Ejector-Upgrades erhoehen
 * Menge pro Zug bzw. verkuerzen das Intervall, exakt wie im Original (Mk.I/II/III -&gt; 4/16/64 bzw.
 * 10/5/2 Ticks).
 * <p>
 * SCOPE-Vereinfachung: Das Original vertauscht Input-/Output-Seite bewusst ("switcheroo"-Kommentar
 * im Original) relativ zu {@code TileEntityCraneBase}s generischem Input/Output-Seitenpaar. Hier:
 * feste, dokumentierte Konvention - Extraktionsquelle ist {@link #FACING}.getOpposite() (hinter dem
 * Block), Auswurfziel ist {@link #FACING} (vor dem Block) - funktional aequivalent, ohne die
 * Original-Klassenhierarchie (TileEntityCraneBase mit screwdriver-ueberschreibbarem Seitenpaar)
 * nachzubauen.
 */
public class MachineCraneExtractorBlockEntity extends BaseMachineBlockEntity implements IEnterableBlock {

    public static final int FILTER_START = 0;
    public static final int FILTER_END = 8;
    public static final int BUFFER_START = 9;
    public static final int BUFFER_END = 17;
    public static final int SLOT_UPGRADE_STACK = 18;
    public static final int SLOT_UPGRADE_EJECTOR = 19;
    public static final int INVENTORY_SIZE = 20;

    private final ModulePatternMatcher matcher = new ModulePatternMatcher(9);
    private final UpgradeManager upgradeManager = new UpgradeManager();
    private boolean isWhitelist = false;
    private boolean maxEject = false;

    private static final Map<ItemMachineUpgrade.UpgradeType, Integer> UPGRADE_CAPS = new EnumMap<>(ItemMachineUpgrade.UpgradeType.class);
    static {
        UPGRADE_CAPS.put(ItemMachineUpgrade.UpgradeType.STACK, 3);
        UPGRADE_CAPS.put(ItemMachineUpgrade.UpgradeType.EJECTOR, 3);
    }

    public MachineCraneExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_EXTRACTOR_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCraneExtractorBlockEntity be) {
        if (!level.isClientSide) {
            be.serverTick(level, pos, state);
        }
    }

    private void serverTick(Level level, BlockPos pos, BlockState state) {
        upgradeManager.checkSlots(inventory, SLOT_UPGRADE_STACK, SLOT_UPGRADE_EJECTOR, UPGRADE_CAPS);

        int delay = switch (upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.EJECTOR)) {
            case 1 -> 10;
            case 2 -> 5;
            case 3 -> 2;
            default -> 20;
        };

        if (level.getGameTime() % delay != 0) return;
        if (level.hasNeighborSignal(pos)) return;

        int amount = switch (upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.STACK)) {
            case 1 -> 4;
            case 2 -> 16;
            case 3 -> 64;
            default -> 1;
        };

        Direction facing = state.getValue(MachineCraneExtractorBlock.FACING);
        Direction pullSide = facing.getOpposite();
        Direction ejectSide = facing;

        BlockPos sourcePos = pos.relative(pullSide);
        BlockPos ejectPos = pos.relative(ejectSide);

        BlockEntity sourceBe = level.getBlockEntity(sourcePos);
        var ejectBlock = level.getBlockState(ejectPos).getBlock();
        IConveyorBelt belt = ejectBlock instanceof IConveyorBelt ib ? ib : null;

        boolean hasSent = false;

        //? if forge {
        IItemHandler source = sourceBe != null
                ? sourceBe.getCapability(ForgeCapabilities.ITEM_HANDLER, pullSide.getOpposite()).orElse(null)
                : null;

        if (source != null) {
            for (int slot = 0; slot < source.getSlots() && !hasSent; slot++) {
                ItemStack stack = source.getStackInSlot(slot);
                if (stack.isEmpty()) continue;

                int maxTarget = Math.min(amount, stack.getMaxStackSize());
                if (maxEject && stack.getCount() < maxTarget) continue;

                boolean match = matchesFilter(stack);
                if (!((isWhitelist && match) || (!isWhitelist && !match))) continue;

                int toSend = Math.min(amount, stack.getCount());
                ItemStack simulated = source.extractItem(slot, toSend, true);
                if (simulated.isEmpty()) continue;

                ItemStack sendStack = simulated.copy();

                if (belt != null) {
                    source.extractItem(slot, sendStack.getCount(), false);
                    sendItem(level, ejectPos, ejectSide, sendStack);
                    hasSent = true;
                } else {
                    ItemStack remainder = insertIntoBuffer(sendStack.copy());
                    int accepted = sendStack.getCount() - remainder.getCount();
                    if (accepted > 0) {
                        source.extractItem(slot, accepted, false);
                        hasSent = true;
                    }
                }
            }
        }
        //?}

        if (!hasSent && belt != null) {
            for (int slot = BUFFER_START; slot <= BUFFER_END; slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isEmpty()) continue;

                int maxTarget = Math.min(amount, stack.getMaxStackSize());
                if (maxEject && stack.getCount() < maxTarget) continue;

                int toSend = Math.min(amount, stack.getCount());
                ItemStack sendStack = stack.copy();
                sendStack.setCount(toSend);
                stack.shrink(toSend);
                if (stack.isEmpty()) inventory.setStackInSlot(slot, ItemStack.EMPTY);

                sendItem(level, ejectPos, ejectSide, sendStack);
                break;
            }
        }

        setChanged();
    }

    private void sendItem(Level level, BlockPos ejectPos, Direction ejectSide, ItemStack stack) {
        BlockEntity ejectBe = level.getBlockEntity(ejectPos);
        var ejectBlock = level.getBlockState(ejectPos).getBlock();

        if (ejectBlock instanceof IConveyorBelt belt) {
            var snap = belt.snapNewItem(level, ejectPos, new net.minecraft.world.phys.Vec3(
                    ejectPos.getX() + 0.5, ejectPos.getY() + 0.5, ejectPos.getZ() + 0.5));
            MovingConveyorItemEntity moving = MovingConveyorItemEntity.create(level, snap.x, snap.y, snap.z, stack);
            level.addFreshEntity(moving);

            if (ejectBlock instanceof IEnterableBlock enterable) {
                enterable.onItemEnter(level, ejectPos, moving);
                moving.discard();
            }
            return;
        }

        //? if forge {
        if (ejectBe != null) {
            IItemHandler handler = ejectBe.getCapability(ForgeCapabilities.ITEM_HANDLER, ejectSide.getOpposite()).orElse(null);
            if (handler != null) {
                ItemStack remainder = net.minecraftforge.items.ItemHandlerHelper.insertItem(handler, stack, false);
                if (remainder.isEmpty()) return;
                stack = remainder;
            }
        }
        //?}

        ItemEntity drop = new ItemEntity(level, ejectPos.getX() + 0.5, ejectPos.getY() + 0.5, ejectPos.getZ() + 0.5, stack);
        level.addFreshEntity(drop);
    }

    private ItemStack insertIntoBuffer(ItemStack stack) {
        for (int i = BUFFER_START; i <= BUFFER_END && !stack.isEmpty(); i++) {
            ItemStack current = inventory.getStackInSlot(i);
            if (current.isEmpty()) {
                inventory.setStackInSlot(i, stack);
                return ItemStack.EMPTY;
            } else if (com.hbm_m.platform.PlatformHooks.isSameItemSameTags(current, stack)) {
                int space = current.getMaxStackSize() - current.getCount();
                if (space > 0) {
                    int toMove = Math.min(space, stack.getCount());
                    current.grow(toMove);
                    stack.shrink(toMove);
                }
            }
        }
        return stack;
    }

    public boolean matchesFilter(ItemStack stack) {
        for (int i = FILTER_START; i <= FILTER_END; i++) {
            ItemStack filter = inventory.getStackInSlot(i);
            if (!filter.isEmpty() && matcher.isValidForFilter(filter, i, stack)) {
                return true;
            }
        }
        return false;
    }

    // ── IEnterableBlock (not used for input - kept for interface parity with the network) ──

    @Override
    public void onItemEnter(Level level, BlockPos pos, MovingConveyorItemEntity item) {
        // Extractor pulls actively; it does not accept items pushed onto it from a conveyor.
    }

    // ── Filter/mode/toggle API ─────────────────────────────────────────────

    public ModulePatternMatcher getMatcher() { return matcher; }
    public boolean isWhitelist() { return isWhitelist; }
    public void toggleWhitelist() { isWhitelist = !isWhitelist; setChanged(); }
    public boolean isMaxEject() { return maxEject; }
    public void toggleMaxEject() { maxEject = !maxEject; setChanged(); }

    public void nextMode(int filterSlot) {
        matcher.nextMode(filterSlot);
        setChanged();
    }

    // ── NBT ─────────────────────────────────────────────────────────────────

    //? if < 1.21.1 {
    @Override
    public void saveAdditional(CompoundTag tag) {
        tag.putBoolean("isWhitelist", isWhitelist);
        tag.putBoolean("maxEject", maxEject);
        matcher.writeToNBT(tag);
    }
    //?} else {
    /*@Override
    public void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
    tag.putBoolean("isWhitelist", isWhitelist);
    tag.putBoolean("maxEject", maxEject);
    matcher.writeToNBT(tag);
    }
    *///?}

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        isWhitelist = tag.getBoolean("isWhitelist");
        maxEject = tag.getBoolean("maxEject");
        matcher.readFromNBT(tag);
    }

    // ── Slot validation / Menu ─────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot >= FILTER_START && slot <= FILTER_END) return true;
        if (slot >= BUFFER_START && slot <= BUFFER_END) return true;
        if (slot == SLOT_UPGRADE_STACK) return stack.getItem() instanceof ItemMachineUpgrade up && up.getUpgradeType() == ItemMachineUpgrade.UpgradeType.STACK;
        if (slot == SLOT_UPGRADE_EJECTOR) return stack.getItem() instanceof ItemMachineUpgrade up && up.getUpgradeType() == ItemMachineUpgrade.UpgradeType.EJECTOR;
        return false;
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    /** Ueberschreibt: Filter-Slots (0-8) setzen zusaetzlich den Match-Modus, wie im Original. */
    public void setFilterSlot(int index, ItemStack stack) {
        inventory.setStackInSlot(index, stack);
        matcher.initPattern(index, stack);
        setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.crane_extractor");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineCraneExtractorMenu.create(id, inventory, this);
    }
}
