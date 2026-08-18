package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.rbmk.RBMKDials;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import com.hbm_m.item.rbmk.RBMKRodItem;
import com.hbm_m.network.RBMKCraneKeyState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 1:1 port of the original's {@code TileEntityCraneConsole}: once linked to a reactor column
 * (via {@link com.hbm_m.item.rbmk.RBMKToolItem}), players standing in its detection zone can
 * fly a physical crane over the reactor grid with the RBMK_CRANE_* keybinds and load/unload
 * fuel rods into whichever column it's hovering over.
 *
 * <p>The original derives its scan/movement directions from the block's placement metadata
 * (a {@code ForgeDirection}); this port has no blockstate facing property (none of the RBMK
 * blocks do), so {@link #facing} is tracked as block-entity-only state, set once when the tool
 * links this console to a target column (see {@link com.hbm_m.item.rbmk.RBMKToolItem#linkCrane}).
 */
public class RBMKCraneConsoleBlockEntity extends RBMKColumnBlockEntity {

    private static final double SPEED = 0.05D;

    public boolean setUpCrane = false;
    public Direction facing = Direction.NORTH;
    public int craneRotationOffset = 0;

    public BlockPos center = BlockPos.ZERO;
    public int spanF, spanB, spanL, spanR, height;

    public double lastTiltFront, lastTiltLeft, tiltFront, tiltLeft;
    public double lastPosFront, lastPosLeft, posFront, posLeft;

    private boolean goesDown = false;
    public double lastProgress = 1D, progress = 1D;

    private ItemStack loadedItem = ItemStack.EMPTY;
    public double loadedHeat, loadedEnrichment;

    public RBMKCraneConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_CRANE_CONSOLE_BE.get(), pos, state);
    }

    // ─── Tick ────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKCraneConsoleBlockEntity be) {
        baseTick(level, pos, state, be);
        be.craneTick(level, pos);
    }

    private void craneTick(Level level, BlockPos pos) {
        if (!setUpCrane) return;

        lastTiltFront = tiltFront;
        lastTiltLeft  = tiltLeft;
        lastPosFront  = posFront;
        lastPosLeft   = posLeft;
        lastProgress  = progress;

        if (level.isClientSide) return;

        RBMKColumnBlockEntity aboveColumn = getColumnAtPos(level);
        if (aboveColumn != null) aboveColumn.craneIndicator = 10;

        if (goesDown) {
            if (progress > 0) {
                progress = Math.max(0, progress - 0.04D);
            } else {
                progress = 0;
                goesDown = false;

                if (aboveColumn instanceof IRBMKLoadable column && canTargetInteract(column)) {
                    if (!loadedItem.isEmpty()) {
                        column.load(loadedItem);
                        loadedItem = ItemStack.EMPTY;
                    } else {
                        loadedItem = column.provideNext().copy();
                        column.unload();
                    }
                    setChanged();
                }
            }
        } else if (progress != 1D) {
            progress = Math.min(1D, progress + 0.04D);
        }

        Direction left = facing.getClockWise();
        double minX = pos.getX() + 0.5 - left.getStepX() * 1.5;
        double maxX = pos.getX() + 0.5 + left.getStepX() * 1.5 + facing.getStepX() * 2;
        double minZ = pos.getZ() + 0.5 - left.getStepZ() * 1.5;
        double maxZ = pos.getZ() + 0.5 + left.getStepZ() * 1.5 + facing.getStepZ() * 2;

        AABB detect = new AABB(Math.min(minX, maxX), pos.getY(), Math.min(minZ, maxZ),
                Math.max(minX, maxX), pos.getY() + 2, Math.max(minZ, maxZ));
        List<Player> players = level.getEntitiesOfClass(Player.class, detect);

        tiltFront = 0;
        tiltLeft = 0;

        if (!players.isEmpty() && !isCraneLoading()) {
            RBMKCraneKeyState.Keys keys = RBMKCraneKeyState.get(players.get(0).getUUID());

            if (keys.up && !keys.down)    { tiltFront = 30;  posFront += SPEED; }
            if (!keys.up && keys.down)    { tiltFront = -30; posFront -= SPEED; }
            if (keys.left && !keys.right) { tiltLeft = 30;   posLeft  += SPEED; }
            if (!keys.left && keys.right) { tiltLeft = -30;  posLeft  -= SPEED; }
            if (keys.load) goesDown = true;
        }

        posFront = Math.max(-spanB, Math.min(spanF, posFront));
        posLeft  = Math.max(-spanR, Math.min(spanL, posLeft));

        if (!loadedItem.isEmpty() && loadedItem.getItem() instanceof RBMKRodItem) {
            loadedHeat       = RBMKRodItem.getHullHeat(loadedItem);
            loadedEnrichment = RBMKRodItem.getEnrichment(loadedItem);
        } else {
            loadedHeat = 0;
            loadedEnrichment = 0;
        }

        setChanged();
    }

    // ─── Crane state ─────────────────────────────────────────────────────────

    public boolean hasItemLoaded()  { return !loadedItem.isEmpty(); }
    public boolean isCraneLoading() { return progress != 1D; }

    public boolean canTargetInteract(IRBMKLoadable column) {
        if (column == null) return false;
        return hasItemLoaded() ? column.canLoad(loadedItem) : column.canUnload();
    }

    public RBMKColumnBlockEntity getColumnAtPos(Level level) {
        Direction left = facing.getClockWise();
        int x = (int) Math.floor(center.getX() - facing.getStepX() * posFront - left.getStepX() * posLeft + 0.5D);
        int y = center.getY() - 1;
        int z = (int) Math.floor(center.getZ() - facing.getStepZ() * posFront - left.getStepZ() * posLeft + 0.5D);
        return level.getBlockEntity(new BlockPos(x, y, z)) instanceof RBMKColumnBlockEntity col ? col : null;
    }

    public IRBMKLoadable getLoadableAtPos(Level level) {
        return getColumnAtPos(level) instanceof IRBMKLoadable l ? l : null;
    }

    /** Called by {@link com.hbm_m.item.rbmk.RBMKToolItem#linkCrane} to point this crane at a reactor column. */
    public void setTarget(Level level, BlockPos columnPos, Direction linkFacing) {
        this.facing = linkFacing;

        int cy = columnPos.getY() + RBMKDials.getColumnHeight(level) + 1;
        this.center = new BlockPos(columnPos.getX(), cy, columnPos.getZ());

        int girderY = cy + 6;
        Direction scanF = facing.getOpposite();
        spanF = findRoomExtent(level, center.getX(), girderY, center.getZ(), scanF, 16);
        Direction scanR = scanF.getClockWise();
        spanR = findRoomExtent(level, center.getX(), girderY, center.getZ(), scanR, 16);
        Direction scanB = scanR.getClockWise();
        spanB = findRoomExtent(level, center.getX(), girderY, center.getZ(), scanB, 16);
        Direction scanL = scanB.getClockWise();
        spanL = findRoomExtent(level, center.getX(), girderY, center.getZ(), scanL, 16);

        height = 7;
        setUpCrane = true;
        setChanged();
    }

    private static int findRoomExtent(Level level, int x, int y, int z, Direction dir, int max) {
        for (int i = 1; i < max; i++) {
            if (!level.isEmptyBlock(new BlockPos(x + dir.getStepX() * i, y, z + dir.getStepZ() * i))) {
                return i - 1;
            }
        }
        return max;
    }

    public void cycleCraneRotation() {
        craneRotationOffset = (craneRotationOffset + 90) % 360;
        setChanged();
    }

    @Override public RBMKType   getRBMKType()    { return RBMKType.OTHER; }
    @Override public ColumnType getConsoleType() { return ColumnType.BLANK; }
    @Override protected boolean participatesInHeatNetwork() { return false; }

    // ─── NBT ─────────────────────────────────────────────────────────────────

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("setUpCrane", setUpCrane);
        tag.putString("facing", facing.getName());
        tag.putInt("craneRotationOffset", craneRotationOffset);
        tag.putInt("centerX", center.getX());
        tag.putInt("centerY", center.getY());
        tag.putInt("centerZ", center.getZ());
        tag.putInt("spanF", spanF);
        tag.putInt("spanB", spanB);
        tag.putInt("spanL", spanL);
        tag.putInt("spanR", spanR);
        tag.putInt("height", height);
        tag.putDouble("posFront", posFront);
        tag.putDouble("posLeft", posLeft);
        tag.putDouble("progress", progress);
        if (!loadedItem.isEmpty()) tag.put("loadedItem", com.hbm_m.platform.PlatformHooks.safeItemSave(loadedItem, null));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        setUpCrane = tag.getBoolean("setUpCrane");
        facing = Direction.byName(tag.getString("facing"));
        if (facing == null) facing = Direction.NORTH;
        craneRotationOffset = tag.getInt("craneRotationOffset");
        center = new BlockPos(tag.getInt("centerX"), tag.getInt("centerY"), tag.getInt("centerZ"));
        spanF = tag.getInt("spanF");
        spanB = tag.getInt("spanB");
        spanL = tag.getInt("spanL");
        spanR = tag.getInt("spanR");
        height = tag.getInt("height");
        posFront = tag.getDouble("posFront");
        posLeft  = tag.getDouble("posLeft");
        progress = tag.contains("progress") ? tag.getDouble("progress") : 1D;
        loadedItem = tag.contains("loadedItem") ? com.hbm_m.platform.PlatformHooks.itemStackOf(tag.getCompound("loadedItem"), null) : ItemStack.EMPTY;
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("setUpCrane", setUpCrane);
        tag.putString("facing", facing.getName());
        tag.putInt("craneRotationOffset", craneRotationOffset);
        tag.putInt("centerX", center.getX());
        tag.putInt("centerY", center.getY());
        tag.putInt("centerZ", center.getZ());
        tag.putInt("spanF", spanF);
        tag.putInt("spanB", spanB);
        tag.putInt("spanL", spanL);
        tag.putInt("spanR", spanR);
        tag.putInt("height", height);
        tag.putDouble("posFront", posFront);
        tag.putDouble("posLeft", posLeft);
        tag.putDouble("progress", progress);
        if (!loadedItem.isEmpty()) tag.put("loadedItem", com.hbm_m.platform.PlatformHooks.safeItemSave(loadedItem, registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        setUpCrane = tag.getBoolean("setUpCrane");
        facing = Direction.byName(tag.getString("facing"));
        if (facing == null) facing = Direction.NORTH;
        craneRotationOffset = tag.getInt("craneRotationOffset");
        center = new BlockPos(tag.getInt("centerX"), tag.getInt("centerY"), tag.getInt("centerZ"));
        spanF = tag.getInt("spanF");
        spanB = tag.getInt("spanB");
        spanL = tag.getInt("spanL");
        spanR = tag.getInt("spanR");
        height = tag.getInt("height");
        posFront = tag.getDouble("posFront");
        posLeft  = tag.getDouble("posLeft");
        progress = tag.contains("progress") ? tag.getDouble("progress") : 1D;
        loadedItem = tag.contains("loadedItem") ? com.hbm_m.platform.PlatformHooks.itemStackOf(tag.getCompound("loadedItem"), registries) : ItemStack.EMPTY;
    }
    *///?}
}
