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
    public static final int FLUX_BUF = 20;
    public static final int SCREENS  = 6;

    // ─── State (server + client synced) ──────────────────────────────────────

    public RBMKColumnData[] columns    = new RBMKColumnData[AREA];
    public int[]            fluxBuffer = new int[FLUX_BUF];
    /** Console display screen cycle types. */
    public ColumnType[]     screenTypes;
    /** Bottom-left corner of the reactor grid (same Y as console). Null = not configured. */
    public BlockPos reactorOrigin = null;

    private int scanTimer = 0;

    // ─── Constructor ──────────────────────────────────────────────────────────

    public MachineRbmkConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_CONSOLE_BE.get(), pos, state);
        screenTypes = new ColumnType[SCREENS];
        Arrays.fill(screenTypes, ColumnType.FUEL);
    }

    // ─── Tick ─────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, MachineRbmkConsoleBlockEntity be) {
        if (level.isClientSide) return;
        if (++be.scanTimer < 20) return;
        be.scanTimer = 0;
        be.scanReactor(level);
        level.sendBlockUpdated(pos, state, state, 3);
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
                BlockPos cPos  = reactorOrigin.offset(x, 0, z);

                if (level.getBlockEntity(cPos) instanceof RBMKColumnBlockEntity col) {
                    CompoundTag d = col.getNBTForConsole();
                    d.putDouble("heat",    col.heat);
                    d.putDouble("maxHeat", col.maxHeat());
                    d.putInt("lid",        col.getLidState());

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

        setChanged();
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
            case 5 -> { // assign selected columns to screen (for future status panel use)
                setChanged();
            }
        }
    }

    private BlockPos idxToPos(int idx) {
        if (reactorOrigin == null || idx < 0 || idx >= AREA) return null;
        return reactorOrigin.offset(idx % GRID, 0, idx / GRID);
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
