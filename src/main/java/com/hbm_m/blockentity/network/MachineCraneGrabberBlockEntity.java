package com.hbm_m.blockentity.network;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.block.machines.MachineCraneGrabberBlock;
import com.hbm_m.block.network.IConveyorBelt;
import com.hbm_m.block.network.IEnterableBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.conveyor.MovingConveyorItemEntity;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.filter.ModulePatternMatcher;
import com.hbm_m.inventory.menu.MachineCraneGrabberMenu;
import com.hbm_m.item.industrial.ItemMachineUpgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
//?}

/**
 * Crane Grabber - Port von {@code TileEntityCraneGrabber} (1.7.10 Original). Greift periodisch nach
 * {@link MovingConveyorItemEntity}s, die vor {@link #FACING} auf einem Foerderband liegen, prueft
 * sie gegen den 9-Slot-{@link ModulePatternMatcher} (Whitelist/Blacklist) und setzt sie hinter dem
 * Block (FACING.getOpposite()) auf ein Foerderband oder in ein Inventar um. Kein eigener Puffer -
 * Items werden direkt weitergereicht, exakt wie im Original.
 * <p>
 * SCOPE-Vereinfachung: Das Original nutzt {@code lastGrabbedTick} + individuelle Weite-Skalierung
 * fuer Doppel-/Dreifach-Foerderbaender (die es in diesem Port nicht gibt). Hier: fester periodischer
 * Tick-Check (wie bei Crane Extractor), AABB-Scan direkt vor dem Block ohne Lane-Skalierung.
 */
public class MachineCraneGrabberBlockEntity extends BaseMachineBlockEntity {

    public static final int FILTER_START = 0;
    public static final int FILTER_END = 8;
    public static final int SLOT_UPGRADE_STACK = 9;
    public static final int SLOT_UPGRADE_EJECTOR = 10;
    public static final int INVENTORY_SIZE = 11;

    private final ModulePatternMatcher matcher = new ModulePatternMatcher(9);
    private final UpgradeManager upgradeManager = new UpgradeManager();
    private boolean isWhitelist = false;

    private static final Map<ItemMachineUpgrade.UpgradeType, Integer> UPGRADE_CAPS = new EnumMap<>(ItemMachineUpgrade.UpgradeType.class);
    static {
        UPGRADE_CAPS.put(ItemMachineUpgrade.UpgradeType.STACK, 3);
        UPGRADE_CAPS.put(ItemMachineUpgrade.UpgradeType.EJECTOR, 3);
    }

    public MachineCraneGrabberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_GRABBER_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCraneGrabberBlockEntity be) {
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

        Direction facing = state.getValue(MachineCraneGrabberBlock.FACING);
        BlockPos grabPos = pos.relative(facing);
        BlockPos dropPos = pos.relative(facing.getOpposite());

        AABB scanBox = new AABB(
                grabPos.getX() + 0.1875, grabPos.getY() + 0.1875, grabPos.getZ() + 0.1875,
                grabPos.getX() + 0.8125, grabPos.getY() + 0.8125, grabPos.getZ() + 0.8125);

        List<MovingConveyorItemEntity> items = level.getEntitiesOfClass(MovingConveyorItemEntity.class, scanBox);

        for (MovingConveyorItemEntity item : items) {
            if (!item.isAlive()) continue;
            ItemStack stack = item.getItem();
            boolean match = matchesFilter(stack);
            if ((isWhitelist && !match) || (!isWhitelist && match)) continue;

            if (deposit(level, dropPos, facing.getOpposite(), stack.copy(), amount)) {
                item.discard();
            }
            break;
        }

        setChanged();
    }

    /** Returns true if (some of) the stack was successfully handed off. */
    private boolean deposit(Level level, BlockPos dropPos, Direction dropSide, ItemStack stack, int amount) {
        var dropBlock = level.getBlockState(dropPos).getBlock();

        if (dropBlock instanceof IConveyorBelt belt) {
            var snap = belt.snapNewItem(level, dropPos, new net.minecraft.world.phys.Vec3(
                    dropPos.getX() + 0.5, dropPos.getY() + 0.5, dropPos.getZ() + 0.5));
            MovingConveyorItemEntity moving = MovingConveyorItemEntity.create(level, snap.x, snap.y, snap.z, stack);
            level.addFreshEntity(moving);
            if (dropBlock instanceof IEnterableBlock enterable) {
                enterable.onItemEnter(level, dropPos, moving);
                moving.discard();
            }
            return true;
        }

        //? if forge {
        BlockEntity dropBe = level.getBlockEntity(dropPos);
        if (dropBe != null) {
            IItemHandler handler = dropBe.getCapability(ForgeCapabilities.ITEM_HANDLER, dropSide.getOpposite()).orElse(null);
            if (handler != null) {
                int toAdd = Math.min(stack.getCount(), amount);
                ItemStack toInsert = stack.copy();
                toInsert.setCount(toAdd);
                ItemStack remainder = ItemHandlerHelper.insertItem(handler, toInsert, false);
                return remainder.getCount() < toAdd;
            }
        }
        //?}

        return false;
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

    // ── Filter/mode/toggle API ─────────────────────────────────────────────

    public ModulePatternMatcher getMatcher() { return matcher; }
    public boolean isWhitelist() { return isWhitelist; }
    public void toggleWhitelist() { isWhitelist = !isWhitelist; setChanged(); }

    public void nextMode(int filterSlot) {
        matcher.nextMode(filterSlot);
        setChanged();
    }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("isWhitelist", isWhitelist);
        matcher.writeToNBT(tag);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        isWhitelist = tag.getBoolean("isWhitelist");
        matcher.readFromNBT(tag);
    }

    // ── Slot validation / Menu ─────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot >= FILTER_START && slot <= FILTER_END) return true;
        if (slot == SLOT_UPGRADE_STACK) return stack.getItem() instanceof ItemMachineUpgrade up && up.getUpgradeType() == ItemMachineUpgrade.UpgradeType.STACK;
        if (slot == SLOT_UPGRADE_EJECTOR) return stack.getItem() instanceof ItemMachineUpgrade up && up.getUpgradeType() == ItemMachineUpgrade.UpgradeType.EJECTOR;
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.crane_grabber");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineCraneGrabberMenu.create(id, inventory, this);
    }
}
