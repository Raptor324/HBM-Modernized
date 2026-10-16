package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.rbmk.*;
import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity.ColumnType;
import com.hbm_m.inventory.menu.MachineRbmkConsoleMenu;
import com.hbm_m.platform.PlatformHooks;
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

public class MachineRbmkConsoleBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements MenuProvider {

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
    public static final int FLUX_BUF = 20;
    public static final int SCREENS  = 6;

    // ─── State (server + client synced) ──────────────────────────────────────

    public RBMKColumnData[] columns    = new RBMKColumnData[AREA];
    public int[]            fluxBuffer = new int[FLUX_BUF];
    /** Console display screen cycle types. */
    public ColumnType[]     screenTypes;
    /**
     * Per-screen aggregate readout text, recomputed in {@link #scanReactor} - 1:1 in spirit with
     * the original's 5 {@code ScreenType} averages (column temp / rod extraction / fuel
     * depletion / fuel poison / fuel temp), adapted to this port's screen model where each of the
     * 6 screens already picks a {@link ColumnType} to filter on (via control action 3) rather than
     * an explicit column selection: the screen shows the average of whichever stats are relevant
     * to that column type across every column of it currently on the grid.
     */
    public String[] screenText;
    /** Bottom-left corner of the reactor grid (same Y as console). Null = not configured. */
    public BlockPos reactorOrigin = null;

    private int scanTimer = 0;

    // ─── Constructor ──────────────────────────────────────────────────────────

    public MachineRbmkConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_CONSOLE_BE.get(), pos, state);
        screenTypes = new ColumnType[SCREENS];
        Arrays.fill(screenTypes, ColumnType.FUEL);
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

    private void scanReactor(Level level) {
        if (reactorOrigin == null) {
            Arrays.fill(columns, null);
            return;
        }

        int totalFlux = 0;

        for (int z = 0; z < GRID; z++) {
            for (int x = 0; x < GRID; x++) {
                int     idx    = z * GRID + x;
                BlockPos cPos  = reactorOrigin.offset(x - GRID_HALF, 0, z - GRID_HALF);

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

    /** Recomputes {@link #screenText}: averages of every stat relevant to each screen's {@link ColumnType} filter. */
    private void computeScreenText() {
        for (int s = 0; s < SCREENS; s++) {
            ColumnType filter = screenTypes[s];
            int count = 0;
            double heatSum = 0, enrichSum = 0, xenonSum = 0, coreHeatSum = 0, levelSum = 0;

            for (RBMKColumnData col : columns) {
                if (col == null || col.type != filter) continue;
                count++;
                heatSum += col.data.getDouble("heat");
                if (filter == ColumnType.FUEL) {
                    enrichSum   += col.data.getDouble("enrichment") * 100.0;
                    xenonSum    += col.data.getDouble("xenon");
                    coreHeatSum += col.data.getDouble("c_coreHeat");
                } else if (filter == ColumnType.CONTROL) {
                    levelSum += col.data.getDouble("level") * 100.0;
                }
            }

            if (count == 0) {
                screenText[s] = filter.name() + ": --";
                continue;
            }

            String text = switch (filter) {
                case FUEL -> String.format("FUEL  T:%.0f  E:%.0f%%  Xe:%.0f%%  Core:%.0f",
                        heatSum / count, enrichSum / count, xenonSum / count, coreHeatSum / count);
                case CONTROL -> String.format("CTRL  T:%.0f  Lvl:%.0f%%", heatSum / count, levelSum / count);
                default -> String.format("%s  T:%.0f  n=%d", filter.name(), heatSum / count, count);
            };
            screenText[s] = text;
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
            case 3 -> { // cycle screen type
                if (iVal >= 0 && iVal < SCREENS) {
                    ColumnType[] types = ColumnType.values();
                    int cur = screenTypes[iVal].ordinal();
                    screenTypes[iVal] = types[(cur + 1) % types.length];
                    setChanged();
                }
            }
            case 4 -> { // set reactor origin
                if (selected.length >= 3)
                    reactorOrigin = new BlockPos(selected[0], selected[1], selected[2]);
                setChanged();
            }
            case 5 -> {
                // No-op: this port's screens already aggregate "every column of the selected
                // ColumnType" (see computeScreenText/action 3) rather than an explicit
                // player-picked column subset like the original's ScreenType selection, so
                // there's nothing to assign here. Kept as a reserved action id for compatibility
                // with existing client packet senders.
                setChanged();
            }
        }
    }

    private BlockPos idxToPos(int idx) {
        if (reactorOrigin == null || idx < 0 || idx >= AREA) return null;
        return reactorOrigin.offset((idx % GRID) - GRID_HALF, 0, (idx / GRID) - GRID_HALF);
    }

    // ─── MenuProvider ─────────────────────────────────────────────────────────

    @Override public Component getDisplayName() { return Component.translatable("block.hbm_m.rbmk_console"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return MachineRbmkConsoleMenu.create(id, inv, this);
    }

    // ─── NBT ──────────────────────────────────────────────────────────────────

    private void writeExtra(CompoundTag tag) {
        if (reactorOrigin != null) {
            tag.putInt("ox", reactorOrigin.getX());
            tag.putInt("oy", reactorOrigin.getY());
            tag.putInt("oz", reactorOrigin.getZ());
        }
        ListTag screens = new ListTag();
        for (ColumnType st : screenTypes) {
            CompoundTag ct = new CompoundTag();
            ct.putString("t", st.name());
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
        if (tag.contains("ox"))
            reactorOrigin = new BlockPos(tag.getInt("ox"), tag.getInt("oy"), tag.getInt("oz"));
        ListTag screens = tag.getList("screens", 10);
        for (int i = 0; i < Math.min(screens.size(), SCREENS); i++) {
            try { screenTypes[i] = ColumnType.valueOf(screens.getCompound(i).getString("t")); }
            catch (Exception ignored) {}
        }
        fluxBuffer = tag.getIntArray("flux");
        if (fluxBuffer.length != FLUX_BUF) fluxBuffer = new int[FLUX_BUF];

        ListTag texts = tag.getList("screenText", 10);
        for (int i = 0; i < Math.min(texts.size(), SCREENS); i++) {
            screenText[i] = texts.getCompound(i).getString("v");
        }
    }

    // @Override omitted intentionally
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        writeExtra(tag);
        
        ListTag cols = new ListTag();
        for (int i = 0; i < AREA; i++) {
            if (columns[i] != null) {
                CompoundTag c = columns[i].toNBT();
                c.putShort("i", (short) i);
                cols.add(c);
            }
        }
        tag.put("cols", cols);
    }

    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        readExtra(tag);
        
        if (tag.contains("cols")) {
            Arrays.fill(columns, null);
            ListTag cols = tag.getList("cols", 10);
            for (int i = 0; i < cols.size(); i++) {
                CompoundTag c = cols.getCompound(i);
                int idx = c.getShort("i") & 0xFFFF;
                if (idx < AREA) columns[idx] = RBMKColumnData.fromNBT(c);
            }
        }
    }

    @Override
    protected void applyClientUpdate(@NotNull CompoundTag tag) {
        readNbtData(tag, null);
    }

}
