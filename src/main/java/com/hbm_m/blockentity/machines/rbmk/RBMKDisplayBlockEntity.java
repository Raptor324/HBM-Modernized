package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineRbmkConsoleBlockEntity.RBMKColumnData;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/**
 * 1:1 port of {@code TileEntityRBMKDisplay}: a wall panel showing a 7x7 slice of the reactor,
 * the same mini-map the console draws but smaller and read-only.
 *
 * <p>The scanned area is centered on a target set with the RBMK linking tool (the original's
 * {@code ItemRBMKTool} third target, see {@code setTarget}); until one is set the panel stays
 * blank. Each cell carries the same payload the console collects, so the renderer can tint by
 * heat, color group or crane indicator exactly like {@code RenderRBMKDisplay} does.</p>
 */
public class RBMKDisplayBlockEntity extends RBMKPanelDeviceBlockEntity {

    public static final int GRID = 7;
    public static final int AREA = GRID * GRID;
    private static final int GRID_HALF = GRID / 2;

    /**
     * Which way the scanned grid is turned, 0-3 for 0/90/180/270 degrees. 1:1 with the original's
     * {@code rotation} byte: a screwdriver on the console steps it round, so the same physical
     * reactor can be read from a console standing on any side of it. Without this the scan was
     * locked to one fixed orientation and a console placed on the "wrong" side showed the reactor
     * mirrored.
     */
    public byte rotation = 0;

    /** {@code TileEntityRBMKConsole.rotate()} - screwdriver steps the grid a quarter turn. */
    public void rotate() {
        rotation = (byte) ((rotation + 1) % 4);
        setChanged();
        syncToClient();
    }

    /** Original getXFromIndex: the grid offset after the rotation is applied. */
    protected int rotatedX(int i, int j) {
        return switch (rotation) {
            case 1 -> -j;
            case 2 -> -i;
            case 3 -> j;
            default -> i;
        };
    }

    /** Original getZFromIndex. */
    protected int rotatedZ(int i, int j) {
        return switch (rotation) {
            case 1 -> i;
            case 2 -> -j;
            case 3 -> -i;
            default -> j;
        };
    }


    /** Rescan cadence, matching the console's - the panel is informational, not per-tick critical. */
    private static final int SCAN_INTERVAL = 10;

    public RBMKColumnData[] columns = new RBMKColumnData[AREA];
    public BlockPos target = null;

    private int scanTimer = 0;

    public RBMKDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_DISPLAY_BE.get(), pos, state);
    }

    /** The display has no configurable units of its own; it only mirrors the reactor. */
    @Override public int unitCount() { return 0; }

    /** {@code TileEntityRBMKDisplay.setTarget} - called by the linking tool. */
    public void setTarget(BlockPos pos) {
        this.target = pos;
        setChanged();
        syncToClient();
    }

    @Override
    protected void onPanelTick(Level level, BlockPos pos) {
        if (++scanTimer < SCAN_INTERVAL) return;
        scanTimer = 0;

        if (target == null) {
            Arrays.fill(columns, null);
            syncToClient();
            return;
        }

        for (int z = 0; z < GRID; z++) {
            for (int x = 0; x < GRID; x++) {
                int idx = z * GRID + x;
                int      ri   = x - GRID_HALF, rj = z - GRID_HALF;
                BlockPos cPos = target.offset(rotatedX(ri, rj), 0, rotatedZ(ri, rj));

                if (level.getBlockEntity(cPos) instanceof RBMKColumnBlockEntity col) {
                    CompoundTag d = col.getNBTForConsole();
                    d.putDouble("heat", col.heat);
                    d.putDouble("maxHeat", col.maxHeat());
                    d.putInt("indicator", col.craneIndicator);
                    columns[idx] = new RBMKColumnData(col.getConsoleType(), d);
                } else {
                    columns[idx] = null;
                }
            }
        }
        syncToClient();
    }

    private void saveDisplay(CompoundTag tag) {
        tag.putByte("rotation", rotation);
        if (target != null) {
            tag.putInt("tX", target.getX());
            tag.putInt("tY", target.getY());
            tag.putInt("tZ", target.getZ());
        }
        ListTag list = new ListTag();
        for (int i = 0; i < AREA; i++) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("i", i);
            if (columns[i] != null) entry.put("c", columns[i].toNBT());
            list.add(entry);
        }
        tag.put("cols", list);
    }

    private void loadDisplay(CompoundTag tag) {
        rotation = tag.getByte("rotation");
        target = tag.contains("tX") ? new BlockPos(tag.getInt("tX"), tag.getInt("tY"), tag.getInt("tZ")) : null;
        Arrays.fill(columns, null);
        ListTag list = tag.getList("cols", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int idx = entry.getInt("i");
            if (idx >= 0 && idx < AREA && entry.contains("c"))
                columns[idx] = RBMKColumnData.fromNBT(entry.getCompound("c"));
        }
    }

    @Override
    public void receiveControl(CompoundTag data) {
        receiveSharedControl(data);
        setChanged();
        syncToClient();
    }

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveDisplay(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        loadDisplay(tag);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveDisplay(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadDisplay(tag);
    }
    *///?}
}
