package com.hbm_m.blockentity.network;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.network.IConveyorBelt;
import com.hbm_m.block.network.IEnterableBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.conveyor.MovingConveyorItemEntity;
import com.hbm_m.inventory.filter.ModulePatternMatcher;
import com.hbm_m.inventory.menu.MachineCraneRouterMenu;

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
import net.minecraftforge.items.ItemHandlerHelper;
//?}

/**
 * Crane Router - Port von {@code CraneRouter}/{@code TileEntityCraneRouter} (1.7.10 Original).
 * Passiver Foerderband-Sortierer ohne eigenes FACING: 6 Seiten (DOWN/UP/NORTH/SOUTH/WEST/EAST,
 * gleiche Ordinal-Reihenfolge wie das Original-{@code ForgeDirection}), je mit eigenem 5-Slot-
 * {@link ModulePatternMatcher} und Modus (NONE/WHITELIST/BLACKLIST/WILDCARD). Ankommende Items
 * werden gegen jede aktive Seite geprueft; bei mehreren gueltigen Zielen wird zufaellig gewaehlt;
 * gibt es keine Treffer, greifen WILDCARD-Seiten als Fallback; ansonsten faellt das Item einfach
 * an Ort und Stelle herunter - 1:1 aus dem Original ({@code CraneRouter.getOutputDir}).
 */
public class MachineCraneRouterBlockEntity extends BaseMachineBlockEntity implements IEnterableBlock {

    public static final int SLOTS_PER_SIDE = 5;
    public static final int SIDE_COUNT = 6;
    public static final int INVENTORY_SIZE = SLOTS_PER_SIDE * SIDE_COUNT;

    public static final int MODE_NONE = 0;
    public static final int MODE_WHITELIST = 1;
    public static final int MODE_BLACKLIST = 2;
    public static final int MODE_WILDCARD = 3;

    private final ModulePatternMatcher[] patterns = new ModulePatternMatcher[SIDE_COUNT];
    private final int[] modes = new int[SIDE_COUNT];

    public MachineCraneRouterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_ROUTER_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
        for (int i = 0; i < SIDE_COUNT; i++) patterns[i] = new ModulePatternMatcher(SLOTS_PER_SIDE);
    }

    @Override
    public void onItemEnter(Level level, BlockPos pos, MovingConveyorItemEntity item) {
        if (level.isClientSide) return;

        ItemStack stack = item.getItem().copy();
        Direction dir = getOutputDir(level, stack);

        if (dir == null) {
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            level.addFreshEntity(drop);
        } else {
            sendOnRoute(level, pos, dir, stack);
        }

        item.discard();
        setChanged();
    }

    private Direction getOutputDir(Level level, ItemStack stack) {
        List<Direction> validDirs = new ArrayList<>();

        for (int side = 0; side < SIDE_COUNT; side++) {
            int mode = modes[side];
            if (mode == MODE_NONE || mode == MODE_WILDCARD) continue;

            boolean matches = false;
            for (int slot = 0; slot < SLOTS_PER_SIDE; slot++) {
                ItemStack filter = inventory.getStackInSlot(side * SLOTS_PER_SIDE + slot);
                if (filter.isEmpty()) continue;
                if (patterns[side].isValidForFilter(filter, slot, stack)) {
                    matches = true;
                    break;
                }
            }

            if ((mode == MODE_WHITELIST && matches) || (mode == MODE_BLACKLIST && !matches)) {
                validDirs.add(Direction.values()[side]);
            }
        }

        if (validDirs.isEmpty()) {
            for (int side = 0; side < SIDE_COUNT; side++) {
                if (modes[side] == MODE_WILDCARD) validDirs.add(Direction.values()[side]);
            }
        }

        if (validDirs.isEmpty()) return null;
        return validDirs.get(level.random.nextInt(validDirs.size()));
    }

    private void sendOnRoute(Level level, BlockPos pos, Direction dir, ItemStack stack) {
        BlockPos targetPos = pos.relative(dir);
        var targetBlock = level.getBlockState(targetPos).getBlock();

        if (targetBlock instanceof IConveyorBelt belt) {
            var snap = belt.snapNewItem(level, targetPos, new net.minecraft.world.phys.Vec3(
                    targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5));
            MovingConveyorItemEntity moving = MovingConveyorItemEntity.create(level, snap.x, snap.y, snap.z, stack);
            level.addFreshEntity(moving);
            if (targetBlock instanceof IEnterableBlock enterable) {
                enterable.onItemEnter(level, targetPos, moving);
                moving.discard();
            }
            return;
        }

        //? if forge {
        BlockEntity targetBe = level.getBlockEntity(targetPos);
        if (targetBe != null) {
            IItemHandler handler = targetBe.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).orElse(null);
            if (handler != null) {
                ItemStack remainder = ItemHandlerHelper.insertItem(handler, stack, false);
                if (remainder.isEmpty()) return;
                stack = remainder;
            }
        }
        //?}

        ItemEntity drop = new ItemEntity(level, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, stack);
        level.addFreshEntity(drop);
    }

    // ── Filter/mode API ─────────────────────────────────────────────────────

    public ModulePatternMatcher getMatcher(int side) { return patterns[side]; }
    public int getMode(int side) { return modes[side]; }

    public void nextTargetMode(int side) {
        modes[side] = (modes[side] + 1) % 4;
        setChanged();
    }

    public void nextFilterMode(int index) {
        int side = index / SLOTS_PER_SIDE;
        int slot = index % SLOTS_PER_SIDE;
        patterns[side].nextMode(slot);
        setChanged();
    }

    public void initPattern(int index, ItemStack stack) {
        int side = index / SLOTS_PER_SIDE;
        int slot = index % SLOTS_PER_SIDE;
        patterns[side].initPattern(slot, stack);
    }

    // ── NBT ─────────────────────────────────────────────────────────────────

    //? if < 1.21.1 {
    @Override
    public void saveAdditional(CompoundTag tag) {
        for (int i = 0; i < SIDE_COUNT; i++) {
            CompoundTag patternTag = new CompoundTag();
            patterns[i].writeToNBT(patternTag);
            tag.put("pattern" + i, patternTag);
        }
        tag.putIntArray("modes", modes);
    }
    //?} else {
    /*@Override
    public void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
    for (int i = 0; i < SIDE_COUNT; i++) {
    CompoundTag patternTag = new CompoundTag();
    patterns[i].writeToNBT(patternTag);
    tag.put("pattern" + i, patternTag);
    }
    tag.putIntArray("modes", modes);
    }
    *///?}

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        for (int i = 0; i < SIDE_COUNT; i++) {
            patterns[i].readFromNBT(tag.getCompound("pattern" + i));
        }
        int[] loaded = tag.getIntArray("modes");
        if (loaded.length == SIDE_COUNT) System.arraycopy(loaded, 0, modes, 0, SIDE_COUNT);
    }

    // ── Slot validation / Menu ─────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.crane_router");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineCraneRouterMenu.create(id, inventory, this);
    }
}
