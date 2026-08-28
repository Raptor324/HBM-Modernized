package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.rbmk.*;
import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity.ColumnType;
import com.hbm_m.inventory.menu.MachineRbmkConsoleMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

public class MachineRbmkConsoleBlockEntity extends BlockEntity implements MenuProvider {

    // ─── Column data ──────────────────────────────────────────────────────────

    public static class RBMKColumnData {
        public final ColumnType  type;
        public final CompoundTag data;

        public RBMKColumnData(ColumnType type, CompoundTag data) {
            this.type = type;
            this.data = data;
        }

        public CompoundTag toNBT() {
            CompoundTag t = new CompoundTag();
            t.putString("t", type.name());
            t.put("d", data);
            return t;
        }

        public static RBMKColumnData fromNBT(CompoundTag t) {
            ColumnType type;
            try { type = ColumnType.valueOf(t.getString("t")); }
            catch (Exception e) { type = ColumnType.BLANK; }
            return new RBMKColumnData(type, t.getCompound("d"));
        }
    }

    // ─── Constants ────────────────────────────────────────────────────────────

    public static final int GRID  = 15;
    public static final int AREA  = GRID * GRID;
    /** CE keeps 60 samples ({@code fluxDisplayBuffer}); the port only kept 20, so the console's
     *  flux graph covered a third of the history it is drawn to show. */
    public static final int FLUX_BUF = 60;
    public static final int SCREENS  = 6;

    // ─── State (server + client synced) ──────────────────────────────────────

    public RBMKColumnData[] columns    = new RBMKColumnData[AREA];
    public int[]            fluxBuffer = new int[FLUX_BUF];
    /**
     * What each of the six screens measures, 1:1 with CE's {@code ScreenType}. The port used to
     * store a {@link ColumnType} here instead and average "every column of that type on the grid",
     * which is not what the console does: in CE a screen picks a <em>statistic</em> and the
     * operator separately picks <em>which columns</em> feed it, so one screen can watch the core
     * temperature of one bank of channels while another watches the extraction of a second bank.
     */
    public enum ScreenType {
        NONE, COL_TEMP, ROD_EXTRACTION, FUEL_DEPLETION, FUEL_POISON, FUEL_TEMP;

        public static final ScreenType[] VALUES = values();
    }

    public ScreenType[] screenTypes;

    /** The column indices each screen averages over - CE's {@code RBMKScreen.columns}. */
    public int[][] screenColumns;

    /** Per-screen readout text, recomputed in {@link #scanReactor}. */
    public String[] screenText;
    /** Bottom-left corner of the reactor grid (same Y as console). Null = not configured. */
    public BlockPos reactorOrigin = null;

    private int scanTimer = 0;

    // ─── Constructor ──────────────────────────────────────────────────────────

    public MachineRbmkConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_CONSOLE_BE.get(), pos, state);
        screenTypes = new ScreenType[SCREENS];
        Arrays.fill(screenTypes, ScreenType.NONE);
        screenColumns = new int[SCREENS][0];
        screenText = new String[SCREENS];
        Arrays.fill(screenText, "");
    }

    // ─── Tick ─────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, MachineRbmkConsoleBlockEntity be) {
        if (level.isClientSide) return;
        if (++be.scanTimer < 20) return;
        be.scanTimer = 0;
        be.scanReactor(level);
        level.sendBlockUpdated(pos, state, state, 3);
    }

    /**
     * Half of {@link #GRID} (integer division) - the scan below is centered on {@link #reactorOrigin}
     * rather than treating it as the grid's corner, so linking works no matter which column of the
     * build the player happened to right-click with the {@code RBMKToolItem} (previously the linked
     * column always ended up pinned to the grid's top-left cell, so anything outside the +X/+Z
     * quadrant from it silently fell off the scanned area and never showed up).
     */
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
        if (level != null && !level.isClientSide) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
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


    private void scanReactor(Level level) {
        if (reactorOrigin == null) {
            Arrays.fill(columns, null);
            return;
        }

        int totalFlux = 0;

        for (int z = 0; z < GRID; z++) {
            for (int x = 0; x < GRID; x++) {
                int     idx    = z * GRID + x;
                int      ri    = x - GRID_HALF, rj = z - GRID_HALF;
                BlockPos cPos  = reactorOrigin.offset(rotatedX(ri, rj), 0, rotatedZ(ri, rj));

                if (level.getBlockEntity(cPos) instanceof RBMKColumnBlockEntity col) {
                    CompoundTag d = col.getNBTForConsole();
                    d.putDouble("heat",      col.heat);
                    d.putDouble("maxHeat",   col.maxHeat());
                    d.putInt("lid",          col.getLidState());
                    // 1:1 with the original's RenderRBMKConsole: a column the crane is currently
                    // hovering flashes yellow on the mini-map, driven by this counter.
                    d.putInt("indicator",    col.craneIndicator);

                    if (col instanceof RBMKRodBlockEntity rod)
                        totalFlux += (int) rod.lastFluxQuantity;

                    columns[idx] = new RBMKColumnData(col.getConsoleType(), d);
                } else {
                    columns[idx] = null;
                }
            }
        }

        // Rotate flux history ring buffer
        System.arraycopy(fluxBuffer, 1, fluxBuffer, 0, FLUX_BUF - 1);
        fluxBuffer[FLUX_BUF - 1] = totalFlux;

        computeScreenText();
        setChanged();
    }

    /**
     * 1:1 with CE's {@code prepareScreenInfo}: each screen averages one statistic over the columns
     * the operator assigned to it. Columns that cannot supply that statistic (a moderator has no
     * enrichment, a fuel channel has no extraction level) are skipped rather than counted as zero.
     */
    private void computeScreenText() {
        for (int s = 0; s < SCREENS; s++) {
            ScreenType type = screenTypes[s];
            if (type == ScreenType.NONE) {
                screenText[s] = "";
                continue;
            }

            double value = 0;
            int count = 0;

            for (int idx : screenColumns[s]) {
                if (idx < 0 || idx >= AREA) continue;
                RBMKColumnData col = columns[idx];
                if (col == null) continue;

                boolean hasFuel = col.type == ColumnType.FUEL;

                switch (type) {
                    case COL_TEMP -> {
                        count++;
                        value += col.data.getDouble("heat");
                    }
                    case FUEL_DEPLETION -> {
                        if (hasFuel && col.data.getDouble("c_maxHeat") > 0) {
                            count++;
                            value += 100.0 - col.data.getDouble("enrichment") * 100.0;
                        }
                    }
                    case FUEL_POISON -> {
                        if (hasFuel && col.data.getDouble("c_maxHeat") > 0) {
                            count++;
                            value += col.data.getDouble("xenon");
                        }
                    }
                    case FUEL_TEMP -> {
                        if (hasFuel && col.data.getDouble("c_maxHeat") > 0) {
                            count++;
                            value += col.data.getDouble("c_heat");
                        }
                    }
                    case ROD_EXTRACTION -> {
                        if (col.type == ColumnType.CONTROL) {
                            count++;
                            value += col.data.getDouble("level") * 100.0;
                        }
                    }
                    default -> { }
                }
            }

            if (count == 0) {
                screenText[s] = "";
                continue;
            }

            String text = ((int) (value / count * 10)) / 10D + "";
            screenText[s] = switch (type) {
                case COL_TEMP, FUEL_TEMP -> text + "\u00B0C";
                case FUEL_DEPLETION, FUEL_POISON, ROD_EXTRACTION -> text + "%";
                default -> text;
            };
        }
    }

    // ─── Control handling (called by packet) ──────────────────────────────────

    public void handleControl(ServerLevel level, int action, double dVal, int iVal, int[] selected) {
        switch (action) {
            case 0 -> { // set control rod level
                for (int idx : selected) {
                    BlockPos cPos = idxToPos(idx);
                    if (cPos != null && level.getBlockEntity(cPos) instanceof RBMKControlBlockEntity rod)
                        rod.setTarget(dVal);
                }
            }
            case 1 -> { // AZ-5: retract ALL control rods immediately
                for (int i = 0; i < AREA; i++) {
                    BlockPos cPos = idxToPos(i);
                    if (cPos != null && level.getBlockEntity(cPos) instanceof RBMKControlBlockEntity rod) {
                        rod.setTarget(0);
                        rod.level = 0;
                    }
                }
            }
            case 2 -> { // assign color group
                for (int idx : selected) {
                    BlockPos cPos = idxToPos(idx);
                    if (cPos != null && level.getBlockEntity(cPos) instanceof RBMKControlBlockEntity rod) {
                        rod.color = (short) iVal;
                        rod.setChanged();
                    }
                }
            }
            case 3 -> { // cycle screen type (CE: the "toggle" key)
                if (iVal >= 0 && iVal < SCREENS) {
                    int next = screenTypes[iVal].ordinal() + 1;
                    screenTypes[iVal] = ScreenType.VALUES[next % ScreenType.VALUES.length];
                    setChanged();
                }
            }
            case 4 -> { // set reactor origin
                if (selected.length >= 3)
                    reactorOrigin = new BlockPos(selected[0], selected[1], selected[2]);
                setChanged();
            }
            case 5 -> { // assign the current grid selection to screen `iVal` (CE: the "id" key)
                if (iVal >= 0 && iVal < SCREENS) {
                    screenColumns[iVal] = selected == null ? new int[0] : selected.clone();
                    setChanged();
                }
            }
            case 6 -> { // cycle the steam compressor on every selected boiler channel
                for (int idx : selected) {
                    BlockPos cPos = idxToPos(idx);
                    if (cPos != null && level.getBlockEntity(cPos)
                            instanceof com.hbm_m.blockentity.machines.rbmk.RBMKBoilerBlockEntity boiler) {
                        boiler.cycleCompressor();
                    }
                }
            }
        }
    }

    private BlockPos idxToPos(int idx) {
        if (reactorOrigin == null || idx < 0 || idx >= AREA) return null;
        int i = (idx % GRID) - GRID_HALF, j = (idx / GRID) - GRID_HALF;
        return reactorOrigin.offset(rotatedX(i, j), 0, rotatedZ(i, j));
    }

    // ─── MenuProvider ─────────────────────────────────────────────────────────

    @Override public Component getDisplayName() { return Component.translatable("block.hbm_m.rbmk_console"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return MachineRbmkConsoleMenu.create(id, inv, this);
    }

    // ─── NBT ──────────────────────────────────────────────────────────────────

    private void writeExtra(CompoundTag tag) {
        tag.putByte("rotation", rotation);
        if (reactorOrigin != null) {
            tag.putInt("ox", reactorOrigin.getX());
            tag.putInt("oy", reactorOrigin.getY());
            tag.putInt("oz", reactorOrigin.getZ());
        }
        ListTag screens = new ListTag();
        for (int i = 0; i < SCREENS; i++) {
            CompoundTag ct = new CompoundTag();
            ct.putString("t", screenTypes[i].name());
            // CE persists each screen's column selection alongside its type (nbt "s<i>").
            ct.putIntArray("s", screenColumns[i]);
            screens.add(ct);
        }
        tag.put("screens", screens);
        tag.putIntArray("flux", fluxBuffer);

        ListTag texts = new ListTag();
        for (String t : screenText) {
            CompoundTag ct = new CompoundTag();
            ct.putString("v", t != null ? t : "");
            texts.add(ct);
        }
        tag.put("screenText", texts);
    }

    private void readExtra(CompoundTag tag) {
        rotation = tag.getByte("rotation");
        if (tag.contains("ox"))
            reactorOrigin = new BlockPos(tag.getInt("ox"), tag.getInt("oy"), tag.getInt("oz"));
        ListTag screens = tag.getList("screens", 10);
        for (int i = 0; i < Math.min(screens.size(), SCREENS); i++) {
            CompoundTag ct = screens.getCompound(i);
            // Older saves stored a ColumnType name here; anything unrecognised falls back to NONE.
            try { screenTypes[i] = ScreenType.valueOf(ct.getString("t")); }
            catch (Exception ignored) { screenTypes[i] = ScreenType.NONE; }
            screenColumns[i] = ct.contains("s") ? ct.getIntArray("s") : new int[0];
        }
        fluxBuffer = tag.getIntArray("flux");
        if (fluxBuffer.length != FLUX_BUF) fluxBuffer = new int[FLUX_BUF];

        ListTag texts = tag.getList("screenText", 10);
        for (int i = 0; i < Math.min(texts.size(), SCREENS); i++) {
            screenText[i] = texts.getCompound(i).getString("v");
        }
    }

    //? if < 1.21.1 {
    // @Override omitted intentionally
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeExtra(tag);
    }

    public void load(CompoundTag tag) {
        super.load(tag);
        readExtra(tag);
    }
    //?} else {
    /*protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeExtra(tag);
    }

    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readExtra(tag);
    }
    *///?}

    // ─── Sync ─────────────────────────────────────────────────────────────────

    private CompoundTag buildUpdateTag(CompoundTag tag) {
        ListTag cols = new ListTag();
        for (int i = 0; i < AREA; i++) {
            if (columns[i] != null) {
                CompoundTag c = columns[i].toNBT();
                c.putShort("i", (short) i);
                cols.add(c);
            }
        }
        tag.put("cols", cols);
        writeExtra(tag);
        return tag;
    }

    private void applyUpdateTag(CompoundTag tag) {
        Arrays.fill(columns, null);
        ListTag cols = tag.getList("cols", 10);
        for (int i = 0; i < cols.size(); i++) {
            CompoundTag c = cols.getCompound(i);
            int idx = c.getShort("i") & 0xFFFF;
            if (idx < AREA) columns[idx] = RBMKColumnData.fromNBT(c);
        }
        readExtra(tag);
    }

    //? if < 1.21.1 {
    // @Override omitted intentionally
    public CompoundTag getUpdateTag() {
        return buildUpdateTag(super.getUpdateTag());
    }

    public void handleUpdateTag(CompoundTag tag) {
        applyUpdateTag(tag);
    }

    // Forge's default IForgeBlockEntity#onDataPacket calls load(tag) instead of handleUpdateTag(tag)
    // for LIVE re-sync packets (handleUpdateTag is only ever invoked for the one-time initial
    // chunk-load sync) - since load()/readExtra() never parsed the "cols" list, every column the
    // console scanned was silently dropped on every periodic sync, permanently leaving the GUI's
    // column grid empty even though the server-side scan was finding columns correctly every tick.
    public void onDataPacket(net.minecraft.network.Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) applyUpdateTag(packet.getTag());
    }
    //?} else {
    /*public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return buildUpdateTag(super.getUpdateTag(registries));
    }

    public void onDataPacket(net.minecraft.network.Connection connection, ClientboundBlockEntityDataPacket packet, net.minecraft.core.HolderLookup.Provider registries) {
        if (packet.getTag() != null) applyUpdateTag(packet.getTag());
    }
    *///?}

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
