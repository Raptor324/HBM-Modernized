package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.handler.rbmk.NeutronNodeWorld;
import com.hbm_m.handler.rbmk.RBMKDials;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public abstract class RBMKColumnBlockEntity extends BlockEntity {

    public double heat        = 20.0;
    public int reasimWater    = 0;
    public int reasimSteam    = 0;
    public static final int MAX_WATER = 16_000;
    public static final int MAX_STEAM = 16_000;
    public int craneIndicator = 0;
    /** 0 = no lid, 1 = concrete lid, 2 = glass lid */
    protected int lidState = 1;

    private static final Direction[] NEIGHBOR_DIRS = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    protected RBMKColumnBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // â"€â"€â"€ Base Tick â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    protected static void baseTick(Level level, BlockPos pos, BlockState state, RBMKColumnBlockEntity be) {
        if (level.isClientSide) return;
        if (be.craneIndicator > 0) be.craneIndicator--;
        be.moveHeat(level);
        if (RBMKDials.getReasimBoilers(level)) be.boilWater(level);
        level.sendBlockUpdated(pos, state, state, 3);
    }

    // â"€â"€â"€ Heat â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    private void moveHeat(Level level) {
        boolean reasim = RBMKDials.getReasimBoilers(level);
        List<RBMKColumnBlockEntity> rec = new ArrayList<>();
        rec.add(this);
        double heatTot = heat;
        int waterTot = reasimWater, steamTot = reasimSteam;

        for (Direction dir : NEIGHBOR_DIRS) {
            BlockPos np = getBlockPos().offset(dir.getStepX(), 0, dir.getStepZ());
            if (level.getBlockEntity(np) instanceof RBMKColumnBlockEntity n) {
                rec.add(n);
                heatTot += n.heat;
                if (reasim) { waterTot += n.reasimWater; steamTot += n.reasimSteam; }
            }
        }

        int members = rec.size();
        if (members > 1) {
            double targetHeat = heatTot / members;
            double step = RBMKDials.getColumnHeatFlow(level);
            int tWater = waterTot / members, rWater = waterTot % members;
            int tSteam  = steamTot  / members, rSteam  = steamTot  % members;
            for (RBMKColumnBlockEntity c : rec) {
                c.heat += (targetHeat - c.heat) * step;
                if (reasim) { c.reasimWater = tWater; c.reasimSteam = tSteam; }
            }
            if (reasim) { reasimWater += rWater; reasimSteam += rSteam; }
            setChanged();
        }
        coolPassively(level, members - 1);
    }

    private void boilWater(Level level) {
        if (heat < 100.0) return;
        double hc = RBMKDials.getBoilerHeatConsumption(level);
        double available = Math.min(Math.min((heat - 100) / hc, reasimWater), MAX_STEAM - reasimSteam);
        int processed = (int) Math.floor(available * RBMKDials.getReaSimBoilerSpeed(level));
        if (processed <= 0) return;
        reasimWater -= processed;
        reasimSteam += processed;
        heat -= processed * hc;
    }

    protected void coolPassively(Level level, int neighbors) {
        double min = RBMKDials.getPassiveCoolingInner(level);
        double max = RBMKDials.getPassiveCooling(level);
        heat -= min + (max - min) * ((4 - Math.min(neighbors, 4)) / 4.0);
        if (heat < 20) heat = 20.0;
    }

    // â"€â"€â"€ Melt â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    public double maxHeat() { return 1500.0; }

    public void onMelt(Level level, int reduce) {
        standardMelt(level, reduce);
        if (lidState == 1) spawnDebris(level, "lid");
    }

    protected void standardMelt(Level level, int reduce) {
        BlockPos base = getBlockPos();
        int h = RBMKDials.getColumnHeight(level);
        reduce = Math.max(1, Math.min(reduce, h));
        if (level.random.nextInt(3) == 0) reduce++;
        for (int i = h; i >= 0; i--)
            level.setBlock(base.above(i),
                i <= h + 1 - reduce ? Blocks.GRAVEL.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
    }

    protected void spawnDebris(Level level, String type) { /* stub */ }

    // â"€â"€â"€ Lid â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    public boolean hasLid()         { return lidState != 0; }
    public int    getLidState()     { return lidState; }
    public boolean isLidRemovable() { return true; }

    public void setLidState(int state) {
        lidState = state;
        if (level != null) {
            RBMKNeutronHandler.RBMKNeutronNode node =
                NeutronNodeWorld.getOrAddWorld(level).getNode(getBlockPos());
            if (node != null) node.hasLid = (state != 0);
        }
        setChanged();
    }

    // â"€â"€â"€ Neutron â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    public RBMKType getRBMKType()  { return RBMKType.OTHER; }
    public boolean  isModerated()  { return false; }

    // â"€â"€â"€ Console â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    public enum ColumnType {
        BLANK, FUEL, CONTROL, MODERATOR, ABSORBER, REFLECTOR, COOLER, BOILER, HEATER, OUTGASSER, STORAGE
    }
    public abstract ColumnType getConsoleType();

    /** Returns NBT data for the RBMK console panel display. Override in subclasses with relevant data. */
    public CompoundTag getNBTForConsole() { return new CompoundTag(); }

    //? if forge {
    /** Expands the render bounding box to cover the full column height so the BESR isn't culled early. */
    @Override
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public AABB getRenderBoundingBox() {
        BlockPos p = getBlockPos();
        return new AABB(p.getX(), p.getY(), p.getZ(),
                        p.getX() + 1, p.getY() + com.hbm_m.handler.rbmk.RBMKDials.COLUMN_HEIGHT + 1, p.getZ() + 1);
    }
    //?}

    /**
     * Returns the texture-name prefix used by the BESR to look up
     * {@code block/rbmk/<prefix>_side} and {@code block/rbmk/<prefix>_top}.
     * Matches the texture names in the block-model JSON. Override where the
     * block entity type doesn't map 1:1 to the ColumnType name.
     */
    public String getRenderTexturePrefix() {
        return switch (getConsoleType()) {
            case FUEL      -> "rbmk_element";
            case BLANK     -> "rbmk_blank";
            case ABSORBER  -> "rbmk_absorber";
            case REFLECTOR -> "rbmk_reflector";
            case COOLER    -> "rbmk_cooler";
            case BOILER    -> "rbmk_boiler";
            case HEATER    -> "rbmk_heater";
            case MODERATOR -> "rbmk_moderator";
            case OUTGASSER -> "rbmk_outgasser";
            case STORAGE   -> "rbmk_storage";
            case CONTROL   -> "rbmk_control";
        };
    }

    // ─── NBT ─────────────────────────────────────────────────────────────────────

    //? if < 1.21.1 {
    protected static CompoundTag safeItemSave(net.minecraft.world.item.ItemStack stack) {
        CompoundTag t = new CompoundTag();
        stack.save(t);
        return t;
    }

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("heat", heat);
        tag.putInt("reasimWater", reasimWater);
        tag.putInt("reasimSteam", reasimSteam);
        tag.putInt("lidState", lidState);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.saveAdditional(tag, registries);
        tag.putDouble("heat", heat);
        tag.putInt("reasimWater", reasimWater);
        tag.putInt("reasimSteam", reasimSteam);
        tag.putInt("lidState", lidState);
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        heat        = tag.getDouble("heat");
        reasimWater = tag.getInt("reasimWater");
        reasimSteam = tag.getInt("reasimSteam");
        lidState    = tag.contains("lidState") ? tag.getInt("lidState") : 1;
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.loadAdditional(tag, registries);
        heat        = tag.getDouble("heat");
        reasimWater = tag.getInt("reasimWater");
        reasimSteam = tag.getInt("reasimSteam");
        lidState    = tag.contains("lidState") ? tag.getInt("lidState") : 1;
    
    }
    *///?}
    //?} else {
    /*protected static CompoundTag safeItemSave(net.minecraft.world.item.ItemStack stack, net.minecraft.core.HolderLookup.Provider registries) {
        return (CompoundTag) stack.save(registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("heat", heat);
        tag.putInt("reasimWater", reasimWater);
        tag.putInt("reasimSteam", reasimSteam);
        tag.putInt("lidState", lidState);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        heat        = tag.getDouble("heat");
        reasimWater = tag.getInt("reasimWater");
        reasimSteam = tag.getInt("reasimSteam");
        lidState    = tag.contains("lidState") ? tag.getInt("lidState") : 1;
    }
    *///?}

    // ─── Sync ─────────────────────────────────────────────────────────────────────

    //? if < 1.21.1 {
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }
    //?} else {
    /*@Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }
    *///?}

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

